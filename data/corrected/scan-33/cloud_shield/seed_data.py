from database import SessionLocal, engine, Base
import models, random, json

Base.metadata.create_all(bind=engine)
db = SessionLocal()

print("Seeding Cloud Shield Database...")

# Clear existing
for tbl in reversed(Base.metadata.sorted_tables):
    try:
        engine.execute(tbl.delete())
    except:
        db.execute(tbl.delete())
db.commit()

# Roles
roles = {
    "Administrator": models.Role(name="Administrator", description="Full system access"),
    "Security Analyst": models.Role(name="Security Analyst", description="Monitor posture, manage alerts"),
    "Cloud Engineer": models.Role(name="Cloud Engineer", description="Manage resources, apply fixes"),
    "Auditor": models.Role(name="Auditor", description="Read-only compliance views")
}
for r in roles.values(): db.add(r)
db.commit()

# Users
users = [
    models.User(email="admin@cloudshield.local", password_hash="admin123", role_id=roles["Administrator"].id),
    models.User(email="analyst@cloudshield.local", password_hash="admin123", role_id=roles["Security Analyst"].id),
    models.User(email="engineer@cloudshield.local", password_hash="admin123", role_id=roles["Cloud Engineer"].id)
]
db.add_all(users)
db.commit()

# Environments
envs = [
    models.CloudEnvironment(name="Production", description="Live production systems"),
    models.CloudEnvironment(name="Development", description="Dev & Test environments"),
    models.CloudEnvironment(name="College Cloud", description="Academic sandbox"),
    models.CloudEnvironment(name="Corporate Cloud", description="Internal IT infrastructure")
]
db.add_all(envs)
db.commit()

# Policies
policies = [
    models.SecurityPolicy(name="S3 Public Access Block", description="Storage must not be publicly accessible.", severity="Critical", remediation_guidance="1. Go to Storage settings.\n2. Disable public ACLs.\n3. Re-scan."),
    models.SecurityPolicy(name="Database Encryption", description="Sensitive data must be encrypted.", severity="High", remediation_guidance="1. Enable KMS encryption on DB instance."),
    models.SecurityPolicy(name="MFA Required", description="MFA must be enabled for privileged users.", severity="Critical", remediation_guidance="1. Enforce MFA in IAM policy."),
    models.SecurityPolicy(name="Unrestricted Inbound Traffic", description="Firewall rules must not allow 0.0.0.0/0.", severity="High", remediation_guidance="1. Restrict SG inbound rules to known IPs."),
    models.SecurityPolicy(name="Unused IAM Account", description="Inactive accounts must be disabled.", severity="Medium", remediation_guidance="1. Review login history and disable account.")
]
db.add_all(policies)
db.commit()

# Resources
types = ["Virtual Machine", "Storage Bucket", "Database", "Firewall", "IAM Role", "Kubernetes Cluster"]
resources = []
for i in range(1, 251):
    res = models.CloudResource(
        resource_id=f"RES-{10000 + i}",
        name=f"sim-resource-{i}",
        resource_type=random.choice(types),
        environment_id=random.choice(envs).id,
        owner=random.choice(["Alice", "Bob", "Charlie", "System"]),
        status="Active",
        configuration=json.dumps({"public_access": random.choice([True, False]), "encrypted": random.choice([True, False])})
    )
    db.add(res)
    resources.append(res)
db.commit()

# Findings & Alerts are no longer pre-generated.
# They will only appear when a user explicitly runs a scan on a resource.

# Audit logs are populated only by user actions now

print("Cloud Shield Database Seeded Successfully.")