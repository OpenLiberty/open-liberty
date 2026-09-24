# JaCoCo Code Coverage Integration for Open Liberty

This guide explains how to use the integrated JaCoCo code coverage support in the Open Liberty project.

## Table of Contents

1. [Overview](#overview)
2. [Prerequisites](#prerequisites)
3. [Quick Start](#quick-start)
4. [Running Tests with Coverage](#running-tests-with-coverage)
5. [Generating Coverage Reports](#generating-coverage-reports)
6. [Gradle Tasks](#gradle-tasks)
7. [Coverage Report Formats](#coverage-report-formats)
8. [CI/CD Integration](#cicd-integration)
9. [Troubleshooting](#troubleshooting)
10. [Advanced Configuration](#advanced-configuration)

---

## Overview

JaCoCo (Java Code Coverage) has been integrated into the Open Liberty build system to provide comprehensive code coverage analysis for unit tests. This integration allows developers to:

- Measure code coverage for individual modules
- Generate aggregate coverage reports across all modules
- Export coverage data in multiple formats (HTML, XML, CSV)
- Integrate coverage metrics into CI/CD pipelines
- Track coverage trends over time

### What's Included

- **JaCoCo Gradle Plugin**: Configured for all subprojects
- **Test Execution Scripts**: Convenient shell scripts for running tests with coverage
- **Report Generation**: Automated HTML, XML, and CSV report generation
- **Aggregate Reports**: Combined coverage metrics across all modules

---

## Prerequisites

Before using the coverage tools, ensure you have:

1. **Java 17 or higher** (Java 21 recommended)
   ```bash
   java -version
   ```

2. **Gradle** (via gradlew wrapper)
   ```bash
   ./gradlew --version
   ```

3. **Initialized CNF workspace**
   ```bash
   ./gradlew cnf:initialize
   ```

---

## Quick Start

### Run All Tests with Coverage

```bash
# Run all tests and generate aggregate coverage report
./run-tests-with-coverage.sh -a -o

# This will:
# - Execute all unit tests
# - Collect coverage data
# - Generate an aggregate HTML report
# - Open the report in your browser
```

### Run Tests for a Specific Module

```bash
# Run tests for a specific module
./run-tests-with-coverage.sh -m com.ibm.ws.anno -o

# Replace 'com.ibm.ws.anno' with your module name
```

---

## Running Tests with Coverage

The `run-tests-with-coverage.sh` script provides a convenient way to execute tests with coverage enabled.

### Script Options

```bash
./run-tests-with-coverage.sh [options]

Options:
  -m, --module <name>     Run tests for a specific module only
  -c, --clean             Clean build before running tests
  -o, --open              Open HTML coverage report after generation
  -a, --aggregate         Generate aggregate coverage report for all modules
  -s, --sequential        Run tests sequentially (disable parallel execution)
  -h, --help              Display help message
```

### Usage Examples

#### Example 1: Run All Tests with Aggregate Report

```bash
./run-tests-with-coverage.sh -a -o
```

This will:
- Run all unit tests in parallel
- Generate an aggregate coverage report
- Open the HTML report in your default browser

#### Example 2: Run Tests for Specific Module

```bash
./run-tests-with-coverage.sh -m com.ibm.ws.microprofile.rest.client_fat -o
```

This will:
- Run tests only for the specified module
- Generate a coverage report for that module
- Open the HTML report

#### Example 3: Clean Build with Coverage

```bash
./run-tests-with-coverage.sh -c -a
```

This will:
- Clean the build directory
- Run all tests
- Generate aggregate coverage report

#### Example 4: Sequential Test Execution

```bash
./run-tests-with-coverage.sh -a -s
```

Useful when:
- Debugging test failures
- Running on systems with limited resources
- Avoiding parallel execution issues

---

## Generating Coverage Reports

The `generate-coverage-report.sh` script generates coverage reports from existing test execution data.

### Script Options

```bash
./generate-coverage-report.sh [options]

Options:
  -m, --module <name>     Generate report for a specific module
  -a, --aggregate         Generate aggregate report for all modules
  -f, --format <type>     Report format: html, xml, csv, or all (default: html)
  -o, --open              Open HTML report after generation
  -d, --output-dir <dir>  Custom output directory for reports
  -h, --help              Display help message
```

### Usage Examples

#### Example 1: Generate Aggregate HTML Report

```bash
./generate-coverage-report.sh -a -o
```

#### Example 2: Generate All Report Formats

```bash
./generate-coverage-report.sh -a -f all
```

This generates:
- HTML report (human-readable)
- XML report (for CI/CD tools)
- CSV report (for data analysis)

#### Example 3: Generate XML Report for CI/CD

```bash
./generate-coverage-report.sh -a -f xml
```

#### Example 4: Module-Specific Report

```bash
./generate-coverage-report.sh -m com.ibm.ws.anno -o
```

---

## Gradle Tasks

### Individual Module Tasks

```bash
# Run tests with coverage for a specific module
./gradlew :com.ibm.ws.anno:test

# Generate coverage report for a specific module
./gradlew :com.ibm.ws.anno:jacocoTestReport

# Verify coverage thresholds (if configured)
./gradlew :com.ibm.ws.anno:jacocoTestCoverageVerification
```

### Aggregate Tasks

```bash
# Run all tests with coverage
./gradlew test

# Generate aggregate coverage report
./gradlew jacocoRootReport

# Run tests and generate aggregate report
./gradlew testWithCoverage

# Clean all coverage data
./gradlew cleanCoverage
```

### Task Dependencies

The following task dependencies are automatically configured:

- `test` → `jacocoTestReport` (coverage report generated after tests)
- `testWithCoverage` → `jacocoRootReport` (aggregate report after all tests)

---

## Coverage Report Formats

### HTML Reports

**Location**: `build/reports/jacoco/aggregate/html/index.html`

**Features**:
- Interactive web interface
- Drill-down from package to class to method level
- Color-coded coverage indicators
- Source code view with coverage highlighting

**Best for**: Manual review and analysis

### XML Reports

**Location**: `build/reports/jacoco/aggregate/jacocoAggregateReport.xml`

**Features**:
- Machine-readable format
- Compatible with CI/CD tools
- Supports SonarQube, Jenkins, etc.

**Best for**: CI/CD integration and automated analysis

### CSV Reports

**Location**: `build/reports/jacoco/aggregate/jacocoAggregateReport.csv`

**Features**:
- Tabular data format
- Easy to import into spreadsheets
- Suitable for trend analysis

**Best for**: Data analysis and reporting

---

## CI/CD Integration

### Jenkins Integration

```groovy
pipeline {
    stages {
        stage('Test with Coverage') {
            steps {
                sh './run-tests-with-coverage.sh -a'
            }
        }
        stage('Publish Coverage') {
            steps {
                jacoco(
                    execPattern: '**/build/jacoco/*.exec',
                    classPattern: '**/build/classes',
                    sourcePattern: '**/src/main/java',
                    exclusionPattern: '**/*Test*.class'
                )
            }
        }
    }
}
```

### GitHub Actions Integration

```yaml
name: Test Coverage

on: [push, pull_request]

jobs:
  coverage:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
      
      - name: Run tests with coverage
        run: ./run-tests-with-coverage.sh -a
      
      - name: Upload coverage to Codecov
        uses: codecov/codecov-action@v3
        with:
          files: ./build/reports/jacoco/aggregate/jacocoAggregateReport.xml
```

### SonarQube Integration

```bash
# Generate coverage report
./run-tests-with-coverage.sh -a

# Run SonarQube analysis
./gradlew sonarqube \
  -Dsonar.coverage.jacoco.xmlReportPaths=build/reports/jacoco/aggregate/jacocoAggregateReport.xml
```

---

## Troubleshooting

### Issue: No Coverage Data Found

**Symptom**: Report generation fails with "No coverage data found"

**Solution**:
```bash
# Run tests first to generate coverage data
./run-tests-with-coverage.sh -a

# Then generate report
./generate-coverage-report.sh -a -o
```

### Issue: Tests Fail During Coverage Run

**Symptom**: Some tests fail when coverage is enabled

**Solution**:
```bash
# Run tests sequentially to isolate issues
./run-tests-with-coverage.sh -a -s

# Or run specific module
./run-tests-with-coverage.sh -m <module-name>
```

### Issue: Out of Memory Errors

**Symptom**: Build fails with OutOfMemoryError

**Solution**:
1. Increase heap size in `gradle.properties`:
   ```properties
   org.gradle.jvmargs=-Xmx8g -XX:+HeapDumpOnOutOfMemoryError
   ```

2. Run tests sequentially:
   ```bash
   ./run-tests-with-coverage.sh -a -s
   ```

### Issue: Coverage Report Shows 0% Coverage

**Symptom**: Report generated but shows no coverage

**Possible Causes**:
1. Tests didn't run successfully
2. Coverage data file is corrupted
3. Source/class paths are incorrect

**Solution**:
```bash
# Clean and rebuild
./run-tests-with-coverage.sh -c -a

# Verify test execution
./gradlew test --info
```

---

## Advanced Configuration

### Customizing Coverage Thresholds

Edit `wlp-gradle/jacoco.gradle` to set minimum coverage requirements:

```groovy
jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                counter = 'LINE'
                value = 'COVEREDRATIO'
                minimum = 0.80  // 80% line coverage
            }
            limit {
                counter = 'BRANCH'
                value = 'COVEREDRATIO'
                minimum = 0.70  // 70% branch coverage
            }
        }
    }
}
```

### Excluding Classes from Coverage

Add exclusions in `wlp-gradle/jacoco.gradle`:

```groovy
jacocoTestReport {
    afterEvaluate {
        classDirectories.setFrom(files(classDirectories.files.collect {
            fileTree(dir: it, exclude: [
                '**/generated/**',
                '**/*Test*.class',
                '**/*IT.class',
                '**/test/**'
            ])
        }))
    }
}
```

### Module-Specific Configuration

Create a `jacoco.gradle` in your module directory:

```groovy
// com.ibm.ws.mymodule/jacoco.gradle
jacoco {
    toolVersion = "0.8.11"
}

jacocoTestReport {
    reports {
        xml.enabled = true
        html.enabled = true
    }
}
```

Then apply it in your module's `build.gradle`:

```groovy
apply from: 'jacoco.gradle'
```

---

## Report Locations

### Individual Module Reports

```
<module-name>/build/reports/jacoco/test/
├── html/
│   └── index.html          # HTML report
├── jacocoTestReport.xml    # XML report
└── jacocoTestReport.csv    # CSV report
```

### Aggregate Reports

```
build/reports/jacoco/aggregate/
├── html/
│   └── index.html                  # Aggregate HTML report
├── jacocoAggregateReport.xml       # Aggregate XML report
└── jacocoAggregateReport.csv       # Aggregate CSV report
```

### Coverage Data Files

```
<module-name>/build/jacoco/
└── test.exec                       # Binary coverage data
```

---

## Best Practices

1. **Run Coverage Regularly**: Integrate coverage checks into your development workflow
2. **Set Realistic Thresholds**: Start with achievable coverage goals and increase gradually
3. **Focus on Critical Code**: Prioritize coverage for business logic and critical paths
4. **Review Coverage Reports**: Don't just aim for high percentages; review what's actually tested
5. **Exclude Generated Code**: Don't include auto-generated code in coverage metrics
6. **Use Aggregate Reports**: For overall project health, use aggregate reports
7. **Track Trends**: Monitor coverage trends over time, not just absolute values

---

## Additional Resources

- [JaCoCo Official Documentation](https://www.jacoco.org/jacoco/trunk/doc/)
- [Gradle JaCoCo Plugin](https://docs.gradle.org/current/userguide/jacoco_plugin.html)
- [Open Liberty Testing Guide](https://openliberty.io/docs/)

---

## Support

For issues or questions:
1. Check the [Troubleshooting](#troubleshooting) section
2. Review existing test configurations in similar modules
3. Consult the Open Liberty development team

---

**Last Updated**: 2026-04-16  
**JaCoCo Version**: 0.8.11  
**Gradle Version**: As specified in gradle-wrapper.properties