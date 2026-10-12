# BugPredict AI — Software Defect Scanner & Automated Bug Repair

BugPredict AI is an enterprise-grade automated software defect prediction and code repair engine that uses multi-language AST parsing, static analysis rules, and machine learning risk heuristics.

---

## 🚀 Key Features
- **Multi-Language AST Analysis**: Java, Python, JavaScript, TypeScript, C++, C#, Go, SQL.
- **Automated Bug Patching**: Instant auto-remediation for NullPointerExceptions, empty exceptions, loose equality vulnerabilities, and resource leaks.
- **Zero-Bug Output**: Automatically compiles and packages clean, zero-defect ZIP distributions.
- **Local & Privacy-Preserving**: No third-party API keys required; 100% on-premise analysis.

---

## 🛠️ System Requirements
- **Java**: JDK 17 or JDK 21+
- **Build System**: Apache Maven 3.9+ (or included Maven Wrapper `./mvnw`)
- **Memory**: 2 GB RAM minimum

---

## 📦 Installation & Setup

1. **Clone the repository:**
   ```bash
   git clone https://github.com/Lakshmikala43/Bug_Prediction.git
   cd Bug_Prediction
   ```

2. **Install dependencies and verify build:**
   ```bash
   ./mvnw clean compile
   ```

---

## 🧪 Running Tests & Coverage

To run the unit test suite and verify parser evaluation logic:
```bash
./mvnw test
```

Test results and reports are generated in `target/surefire-reports/`.

---

## 🚀 Running the Application

To start the BugPredict AI server locally:
```bash
./mvnw spring-boot:run
```

Once started, the web application is accessible at:
```
http://localhost:8080
```

---

## 📁 Project Architecture & Components

- **`src/main/java/com/bugpredict/controller/`**: REST APIs for scan execution, uploads, and reports.
- **`src/main/java/com/bugpredict/service/parser/`**: AST analyzers and defect remediation rules.
- **`src/main/java/com/bugpredict/service/prediction/`**: Defect risk scoring and evaluation engine.
- **`src/main/resources/static/`**: Client interface for project scanning and defect remediation.
- **`src/test/`**: Unit test suite for AST parser metrics and defect risk logic.

---

## 🔒 License & Ownership
Proprietary — All rights reserved. Not licensed under MIT, Apache, or GPL.
