# PQC LTPA Unit Tests

This directory contains comprehensive unit tests for the Post-Quantum Cryptography (PQC) LTPA implementation.

## Test Files

### 1. PQCKeyGeneratorTest.java
**Purpose**: Unit tests for the PQCKeyGenerator class

**Test Methods**:
- `testGetAvailablePQCProviders()` - Verifies detection of available PQC providers
- `testGetFirstAvailableProvider()` - Tests provider selection logic
- `testGetRecommendedAlgorithm()` - Validates algorithm recommendation based on security level
- `testGenerateMLDSAKeyPair_MLDSA44()` - Tests ML-DSA-44 key generation (128-bit security)
- `testGenerateMLDSAKeyPair_MLDSA65()` - Tests ML-DSA-65 key generation (192-bit security)
- `testGenerateMLDSAKeyPair_MLDSA87()` - Tests ML-DSA-87 key generation (256-bit security)
- `testGenerateMLDSAKeyPair_NullProvider()` - Tests null provider handling

**Coverage**: Key generation, provider detection, algorithm selection

### 2. PQCSignatureHelperTest.java
**Purpose**: Unit tests for the PQCSignatureHelper class

**Test Methods**:
- `testSignAndVerifyMLDSA()` - Tests signature creation and verification
- `testVerifyMLDSA_TamperedData()` - Verifies rejection of tampered data
- `testVerifyMLDSA_TamperedSignature()` - Verifies rejection of tampered signatures
- `testSignMLDSA_NullData()` - Tests null data handling
- `testSignMLDSA_NullPrivateKey()` - Tests null private key handling
- `testVerifyMLDSA_NullData()` - Tests null data handling in verification
- `testVerifyMLDSA_NullSignature()` - Tests null signature handling
- `testVerifyMLDSA_NullPublicKey()` - Tests null public key handling
- `testGetMLDSASignatureSize()` - Validates signature size calculations
- `testSignAndVerifyHybrid()` - Tests hybrid mode (RSA + ML-DSA) signatures
- `testVerifyHybrid_TamperedData()` - Tests hybrid mode with tampered data

**Coverage**: Signature operations, error handling, hybrid mode

### 3. PQCConstantsTest.java
**Purpose**: Unit tests for the PQCConstants class

**Test Methods**:
- `testCryptoModeConstants()` - Validates crypto mode constants
- `testAlgorithmConstants()` - Validates ML-DSA algorithm constants
- `testKeyImportConstants()` - Validates key import property names
- `testProviderConstants()` - Validates PQC provider names
- `testDefaultValues()` - Validates default configuration values
- `testSecurityLevelConstants()` - Validates security level mappings
- `testSignatureSizeConstants()` - Validates signature size constants
- `testKeyPropertyConstants()` - Validates LTPA key property names
- `testConfigPropertyConstants()` - Validates configuration property names
- `testAlgorithmOIDConstants()` - Validates algorithm OID constants

**Coverage**: Constants validation, configuration defaults

### 4. PQCKeyGenerationTest.java (Standalone Test Runner)
**Purpose**: Standalone test runner for manual verification

**Features**:
- Can be run directly with `java` command
- Provides detailed console output
- Tests provider detection, key generation, and signature operations
- Useful for debugging and manual testing

**Usage**:
```bash
java -cp <classpath> com.ibm.ws.security.token.ltpa.pqc.PQCKeyGenerationTest
```

## Running Unit Tests

### Using Gradle
```bash
# Run all unit tests in the project
./gradlew com.ibm.ws.security.token.ltpa:test

# Run only PQC unit tests
./gradlew com.ibm.ws.security.token.ltpa:test --tests "com.ibm.ws.security.token.ltpa.pqc.*"

# Run a specific test class
./gradlew com.ibm.ws.security.token.ltpa:test --tests "com.ibm.ws.security.token.ltpa.pqc.PQCKeyGeneratorTest"

# Run a specific test method
./gradlew com.ibm.ws.security.token.ltpa:test --tests "com.ibm.ws.security.token.ltpa.pqc.PQCKeyGeneratorTest.testGenerateMLDSAKeyPair_MLDSA65"
```

### Using Maven (if applicable)
```bash
mvn test -Dtest=PQCKeyGeneratorTest
mvn test -Dtest=PQCSignatureHelperTest
mvn test -Dtest=PQCConstantsTest
```

## Test Coverage Summary

| Component | Test Class | Test Methods | Coverage |
|-----------|------------|--------------|----------|
| PQCKeyGenerator | PQCKeyGeneratorTest | 7 | Key generation, provider detection |
| PQCSignatureHelper | PQCSignatureHelperTest | 11 | Signature ops, error handling |
| PQCConstants | PQCConstantsTest | 10 | Constants validation |
| **Total** | **3 classes** | **28 methods** | **Comprehensive** |

## Test Dependencies

These tests require:
- JUnit 4.x
- A PQC provider (OpenJCEPlus, IBMJCEPlus, or IBMJCECCA)
- Java 17+ (for build)
- Java 21+ (for PQC provider support)

## Notes

1. **Provider Availability**: Some tests will be skipped if no PQC provider is available
2. **Performance**: Key generation tests may take several seconds due to cryptographic operations
3. **Security Levels**: Tests cover all three ML-DSA security levels (128, 192, 256-bit)
4. **Error Handling**: Comprehensive null parameter and error condition testing
5. **Hybrid Mode**: Tests include both PQC-only and hybrid (RSA + ML-DSA) modes

## Test Execution Order

Tests are independent and can run in any order. However, for debugging:
1. Run `PQCConstantsTest` first (fastest, validates configuration)
2. Run `PQCKeyGeneratorTest` second (validates key generation)
3. Run `PQCSignatureHelperTest` last (validates signature operations)

## Continuous Integration

These unit tests should be included in CI/CD pipelines:
- Run on every commit
- Run before merging pull requests
- Include in nightly builds
- Monitor test execution time (should be < 30 seconds total)

## Troubleshooting

### Tests Skipped
If tests are skipped with "no PQC provider available":
- Ensure Java 21+ is being used
- Verify PQC provider is in classpath
- Check provider installation

### Tests Fail
If tests fail:
1. Check Java version (must be 17+ for build, 21+ for PQC)
2. Verify JAVA_21_HOME environment variable is set
3. Check provider availability with standalone test runner
4. Review test output for specific error messages

## Related Documentation

- [PQC Implementation Guide](../../../../../src/com/ibm/ws/security/token/ltpa/pqc/README.md)
- [FAT Tests](../../../../../com.ibm.ws.security.token.ltpa.pqc_fat/README.md)
- [NIST FIPS 204 - ML-DSA Standard](https://csrc.nist.gov/pubs/fips/204/final)
