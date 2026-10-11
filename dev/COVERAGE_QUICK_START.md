# JaCoCo Coverage - Quick Start Guide

## 🚀 Quick Commands

### Run All Tests with Coverage
```bash
./run-tests-with-coverage.sh -a -o
```
Runs all tests, generates aggregate report, and opens it in your browser.

### Run Tests for Specific Module
```bash
./run-tests-with-coverage.sh -m <module-name> -o
```
Example:
```bash
./run-tests-with-coverage.sh -m com.ibm.ws.anno -o
```

### Generate Report from Existing Data
```bash
./generate-coverage-report.sh -a -o
```

---

## 📊 Common Use Cases

### 1. Testing Your Changes
```bash
# Clean build and run tests with coverage
./run-tests-with-coverage.sh -c -m <your-module> -o
```

### 2. Full Project Coverage
```bash
# Run all tests and get aggregate coverage
./run-tests-with-coverage.sh -a -o
```

### 3. CI/CD Integration
```bash
# Generate XML report for CI tools
./run-tests-with-coverage.sh -a
./generate-coverage-report.sh -a -f xml
```

---

## 📁 Report Locations

### Individual Module
```
<module-name>/build/reports/jacoco/test/html/index.html
```

### Aggregate (All Modules)
```
build/reports/jacoco/aggregate/html/index.html
```

---

## 🛠️ Gradle Tasks

```bash
# Run tests with coverage for specific module
./gradlew :module-name:test :module-name:jacocoTestReport

# Generate aggregate report
./gradlew jacocoRootReport

# Run all tests with coverage
./gradlew testWithCoverage

# Clean coverage data
./gradlew cleanCoverage
```

---

## 📖 Full Documentation

For detailed information, see [JACOCO_COVERAGE_GUIDE.md](./JACOCO_COVERAGE_GUIDE.md)

---

## 🔧 Troubleshooting

### No coverage data found?
Run tests first:
```bash
./run-tests-with-coverage.sh -a
```

### Tests failing?
Try sequential execution:
```bash
./run-tests-with-coverage.sh -a -s
```

### Need help?
```bash
./run-tests-with-coverage.sh --help
./generate-coverage-report.sh --help