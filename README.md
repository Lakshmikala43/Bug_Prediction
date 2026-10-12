# BugPredict AI — Predict Bugs Before Deployment

BugPredict AI is an enterprise-grade, Java-based software defect prediction and code-risk intelligence platform. It analyzes Java source code via AST parsing (McCabe Cyclomatic Complexity, Nesting Depth, Method Length, Class Count), correlates with historical defect datasets, and delivers explainable risk scores with testing and remediation priorities.

---

## 🌟 Key Features

1. **AST-Driven Java Source Scanner**: Recursive discovery and AST parsing with JavaParser, measuring SLOC, McCabe Cyclomatic Complexity, Max Nesting, and Monolithic Methods.
2. **Explainable Risk Prediction Engine**: Transparent scoring (0–100) evaluating complexity, nesting depth, code churn, and defect histories.
3. **Defect History Ingestion**: CSV ingestion of historical defect datasets to align known buggy modules with active code scans.
4. **Remediation Task Management**: Transition defect risks into actionable engineering tasks (`OPEN` -> `IN_PROGRESS` -> `RESOLVED`).
5. **Report & Export System**: Instant CSV report generation for CI/CD pipelines and risk governance audits.
6. **Zero External API Dependencies**: Runs completely locally on Windows/Linux without cloud tokens or paid external AI services.
7. **Warm Human-Centered Design System**: Refined aesthetic featuring ivory backgrounds, charcoal typography, terracotta accents, and sage status badges.

---

## 🚀 Quickstart on Windows

### Prerequisites
- **Java 21+** (JDK 24 compatible)
- **PowerShell**

### Running the Application
From the project root:
```powershell
.\scripts\run.ps1
```
The application will boot on `http://localhost:8080`.

### Measuring Production LOC
```powershell
.\scripts\measure-loc.ps1
```

---

## 🧪 Running Tests
```powershell
.\mvnw.cmd test
```

---

## 📊 Complete User Workflow
1. **Create Project**: Provide project name and absolute Java source code directory.
2. **Run Scan**: JavaParser traverses source files, constructs AST trees, and records metrics.
3. **Inspect Predictions**: Review explainable risk categories (Low, Medium, High, Critical) and metric factors.
4. **Create Remediation**: Directly convert findings into remediation tasks.
5. **Export Report**: Download CSV scan audit reports.
