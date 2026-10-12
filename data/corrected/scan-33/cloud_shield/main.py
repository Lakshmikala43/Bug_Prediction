from fastapi import FastAPI, Request, Form, Depends, HTTPException, status
from fastapi.responses import HTMLResponse, RedirectResponse
from fastapi.staticfiles import StaticFiles
from fastapi.templating import Jinja2Templates
from sqlalchemy.orm import Session
import database, models, json, random, datetime

app = FastAPI(title="Cloud Shield")

app.mount("/static", StaticFiles(directory="static"), name="static")
templates = Jinja2Templates(directory="templates")

database.Base.metadata.create_all(bind=database.engine)

def get_db():
    db = database.SessionLocal()
    try:
        yield db
    finally:
        db.close()

# Context processor for all templates
def render(request, template_name, context, db: Session):
    # Simulated auth context
    user_email = request.cookies.get("session_user", None)
    role = "Viewer"
    if user_email:
        u = db.query(models.User).filter(models.User.email == user_email).first()
        if u and u.role:
            role = u.role.name
    context.update({"request": request, "user_email": user_email, "user_role": role})
    return templates.TemplateResponse(template_name, context)

def log_audit(db, email, action, resource, result="Success"):
    log = models.AuditLog(user_email=email, action=action, resource=resource, result=result)
    db.add(log)
    db.commit()

@app.get("/", response_class=HTMLResponse)
def home(request: Request, db: Session = Depends(get_db)):
    return render(request, "home.html", {}, db)

@app.get("/login", response_class=HTMLResponse)
def login_page(request: Request, db: Session = Depends(get_db)):
    return render(request, "login.html", {}, db)

@app.post("/login")
def login(email: str = Form(...), password: str = Form(...), db: Session = Depends(get_db)):
    user = db.query(models.User).filter(models.User.email == email).first()
    if not user or password != "admin123":
        return RedirectResponse(url="/login?error=Invalid+Credentials", status_code=status.HTTP_303_SEE_OTHER)
    
    log_audit(db, email, "User Login", "Auth System")
    response = RedirectResponse(url="/dashboard", status_code=status.HTTP_303_SEE_OTHER)
    response.set_cookie(key="session_user", value=email)
    return response

@app.get("/logout")
def logout(request: Request, db: Session = Depends(get_db)):
    email = request.cookies.get("session_user", "Unknown")
    log_audit(db, email, "User Logout", "Auth System")
    response = RedirectResponse(url="/", status_code=status.HTTP_303_SEE_OTHER)
    response.delete_cookie("session_user")
    return response

@app.get("/dashboard", response_class=HTMLResponse)
def dashboard(request: Request, db: Session = Depends(get_db)):
    email = request.cookies.get("session_user")
    if not email: return RedirectResponse("/login")
    
    total_res = db.query(models.CloudResource).count()
    total_findings = db.query(models.Finding).filter(models.Finding.status == "Open").count()
    critical = db.query(models.Finding).filter(models.Finding.status == "Open", models.Finding.severity == "Critical").count()
    high = db.query(models.Finding).filter(models.Finding.status == "Open", models.Finding.severity == "High").count()
    
    posture_score = max(0, 100 - (critical * 5) - (high * 2))
    
    recent_alerts = db.query(models.Alert).order_by(models.Alert.id.desc()).limit(6).all()
    
    return render(request, "dashboard.html", {
        "score": posture_score,
        "resources_count": total_res,
        "findings_count": total_findings,
        "critical": critical,
        "high": high,
        "recent_alerts": recent_alerts
    }, db)

@app.get("/resources", response_class=HTMLResponse)
def resources(request: Request, db: Session = Depends(get_db)):
    items = db.query(models.CloudResource).order_by(models.CloudResource.id.desc()).all()
    envs = db.query(models.CloudEnvironment).all()
    return render(request, "resources.html", {"resources": items, "environments": envs}, db)

@app.post("/api/resources")
def add_resource(
    name: str = Form(...),
    resource_type: str = Form(...),
    environment_id: int = Form(...),
    owner: str = Form(...),
    request: Request = None,
    db: Session = Depends(get_db)
):
    email = request.cookies.get("session_user", "System")
    resource_id = f"RES-{random.randint(100000, 999999)}"
    
    res = models.CloudResource(
        resource_id=resource_id,
        name=name,
        resource_type=resource_type,
        environment_id=environment_id,
        owner=owner,
        status="Active",
        configuration=json.dumps({"public_access": False, "encrypted": True, "manual_creation": True})
    )
    db.add(res)
    log_audit(db, email, "Created Simulated Resource", resource_id, result="Success")
    db.commit()
    
    return RedirectResponse(url="/resources", status_code=status.HTTP_303_SEE_OTHER)

@app.get("/findings", response_class=HTMLResponse)
def findings(request: Request, db: Session = Depends(get_db)):
    items = db.query(models.Finding).all()
    return render(request, "findings.html", {"findings": items}, db)

@app.get("/policies", response_class=HTMLResponse)
def policies(request: Request, db: Session = Depends(get_db)):
    items = db.query(models.SecurityPolicy).all()
    return render(request, "policies.html", {"policies": items}, db)

@app.get("/audit", response_class=HTMLResponse)
def audit(request: Request, db: Session = Depends(get_db)):
    items = db.query(models.AuditLog).order_by(models.AuditLog.id.desc()).limit(50).all()
    return render(request, "audit.html", {"logs": items}, db)

# SIMULATED ASSESSMENT ENGINE ROUTE
@app.post("/api/scan/{resource_id}")
def scan_resource(resource_id: int, request: Request, db: Session = Depends(get_db)):
    email = request.cookies.get("session_user", "System")
    res = db.query(models.CloudResource).filter(models.CloudResource.id == resource_id).first()
    if not res:
        return {"status": "error", "message": "Resource not found"}
        
    log_audit(db, email, "Initiated Security Scan", f"Resource {res.resource_id}")
    
    existing_finding = db.query(models.Finding).filter(models.Finding.resource_id == res.id, models.Finding.status != "Resolved").first()
    
    if not existing_finding:
        pol = db.query(models.SecurityPolicy).order_by(models.SecurityPolicy.id).first()
        f = models.Finding(resource_id=res.id, policy_id=pol.id, description=f"Detected misconfiguration: {pol.name}", severity=pol.severity, risk_score=random.randint(60, 100), status="Open")
        db.add(f)
        log_audit(db, "System", "Detected Vulnerability", f"Resource {res.resource_id}", result="Finding Created")
    else:
        log_audit(db, "System", "Scan Complete", f"Resource {res.resource_id}", result="Vulnerabilities persist")
    
    db.commit()
    return RedirectResponse(url="/findings", status_code=status.HTTP_303_SEE_OTHER)

@app.get("/remediation", response_class=HTMLResponse)
def remediation(request: Request, db: Session = Depends(get_db)):
    # Only show findings currently being investigated
    items = db.query(models.Finding).filter(models.Finding.status == "Investigating").all()
    return render(request, "remediation.html", {"findings": items}, db)

@app.post("/api/findings/{finding_id}/status")
def update_finding_status(finding_id: int, request: Request, new_status: str = Form(...), db: Session = Depends(get_db)):
    email = request.cookies.get("session_user", "System")
    finding = db.query(models.Finding).filter(models.Finding.id == finding_id).first()
    
    if finding:
        old_status = finding.status
        finding.status = new_status
        log_audit(db, email, f"Updated Finding Status", f"FIN-{finding_id}", result=f"{old_status} -> {new_status}")
        db.commit()
    
    # Redirect dynamically based on where they should go next
    if new_status == "Investigating":
        return RedirectResponse(url="/remediation", status_code=status.HTTP_303_SEE_OTHER)
    elif new_status == "Resolved":
        return RedirectResponse(url="/remediation", status_code=status.HTTP_303_SEE_OTHER)
    else:
        return RedirectResponse(url="/findings", status_code=status.HTTP_303_SEE_OTHER)