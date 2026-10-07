# Phase 2 Hybrid PQC LTPA Unit Tests

This document describes the comprehensive unit tests for Phase 2 of the PQC LTPA implementation, which adds ML-DSA (digital signature) support to create a complete hybrid cryptographic system: RSA-2048 + ML-DSA + ML-KEM.

## Overview

Phase 2 introduces hybrid key generation and ML-DSA signature operations, completing the quantum-resistant LTPA token infrastructure. The tests validate:

1. **Hybrid key generation** (RSA + ML-DSA + ML-KEM)
2. **ML-DSA signature operations** (sign/verify)
3. **Algorithm type validation** (ML-DSA-44, ML-DSA-65, ML-DSA-87)
4. **Hybrid key container** (immutability, defensive copying, security)

## Test Files

### 1. LTPAHybridKeyGeneratorTest.java (280 lines)
**Purpose**: Tests the LTPAHybridKeyGenerator class that generates complete hybrid key sets.

**Test Coverage**:
- Default security level key generation (Level 3)
- All three security levels (Level 1, 3, 5)
- Specific algorithm combinations
- Invalid security levels and incompatible algorithms
- Null parameter handling
- Multiple key generation (uniqueness)
- Key size verification for all levels
- Algorithm consistency validation

**Key Test Methods**:
- `testGenerateKeys_DefaultSecurityLevel()` - Validates default Level 3 generation
- `testGenerateKeys_Level1Security()` - Tests 128-bit quantum security (ML-DSA-44 + ML-KEM-512)
- `testGenerateKeys_Level3Security()` - Tests 192-bit quantum security (ML-DSA-65 + ML-KEM-768)
- `testGenerateKeys_Level5Security()` - Tests 256-bit quantum security (ML-DSA-87 + ML-KEM-1024)
- `testGenerateKeys_WithSpecificAlgorithms()` - Tests explicit algorithm selection
- `testGenerateKeys_InvalidSecurityLevel()` - Validates error handling
- `testGenerateKeys_IncompatibleAlgorithms()` - Tests security level mismatch detection
- `testGenerateKeys_MultipleInvocations()` - Verifies key uniqueness
- `testGenerateKeys_AllSecurityLevels()` - Validates key size progression
- `testGenerateKeys_VerifyRSAKeySize()` - Confirms RSA-2048 key sizes
- `testGenerateKeys_VerifyAlgorithmConsistency()` - Tests all valid combinations

**Security Validations**:
- RSA-2048 key generation
- ML-DSA key sizes match NIST FIPS 204 specifications
- ML-KEM key sizes match NIST FIPS 203 specifications
- Security level consistency across all three key types
- Proper error handling for invalid configurations

### 2. MLDSAAlgorithmTypeTest.java (230 lines)
**Purpose**: Tests the MLDSAAlgorithmType enum for ML-DSA algorithm variants.

**Test Coverage**:
- Algorithm properties (name, security level, key sizes, signature sizes)
- Recommended ML-KEM algorithm mapping
- Compatibility checking with ML-KEM algorithms
- Security level mapping (1, 3, 5)
- Algorithm name parsing
- Enum operations (values, valueOf, toString)

**Key Test Methods**:
- `testML_DSA_44_Properties()` - Validates ML-DSA-44 (Level 1) properties
- `testML_DSA_65_Properties()` - Validates ML-DSA-65 (Level 3) properties
- `testML_DSA_87_Properties()` - Validates ML-DSA-87 (Level 5) properties
- `testGetRecommendedMLKEM()` - Tests ML-KEM recommendation logic
- `testIsCompatibleWith_MatchingLevels()` - Validates compatible combinations
- `testIsCompatibleWith_MismatchedLevels()` - Tests incompatible combinations
- `testFromSecurityLevel()` - Tests security level to algorithm mapping
- `testFromAlgorithmName()` - Tests algorithm name parsing
- `testKeySizeProgression()` - Validates key sizes increase with security level
- `testSignatureSizeProgression()` - Validates signature sizes increase with security level

**NIST FIPS 204 Compliance**:
- ML-DSA-44: 1312-byte public key, 2560-byte private key, 2420-byte signature
- ML-DSA-65: 1952-byte public key, 4032-byte private key, 3309-byte signature
- ML-DSA-87: 2592-byte public key, 4896-byte private key, 4627-byte signature

### 3. LTPAPQCSignatureTest.java (430 lines)
**Purpose**: Tests the LTPAPQCSignature class for ML-DSA signature operations.

**Test Coverage**:
- ML-DSA key pair generation for all three variants
- Signature generation and verification
- Tampered data detection
- Tampered signature detection
- Wrong public key detection
- Key reconstruction from byte arrays
- Variable data size handling
- Multiple signatures with same key
- Null parameter handling

**Key Test Methods**:
- `testGenerateMLDSAKeyPair_ML_DSA_44()` - Tests ML-DSA-44 key generation
- `testGenerateMLDSAKeyPair_ML_DSA_65()` - Tests ML-DSA-65 key generation
- `testGenerateMLDSAKeyPair_ML_DSA_87()` - Tests ML-DSA-87 key generation
- `testSignAndVerify_ML_DSA_65()` - Tests basic sign/verify operations
- `testSignAndVerify_AllAlgorithms()` - Tests all three ML-DSA variants
- `testVerify_TamperedData()` - Validates tampered data rejection
- `testVerify_TamperedSignature()` - Validates tampered signature rejection
- `testVerify_WrongPublicKey()` - Tests wrong key detection
- `testReconstructPrivateKey()` - Tests private key reconstruction
- `testReconstructPublicKey()` - Tests public key reconstruction
- `testSignWithDifferentDataSizes()` - Tests variable-length data
- `testMultipleSignaturesWithSameKey()` - Validates signature uniqueness

**Security Validations**:
- Signature sizes match NIST FIPS 204 specifications
- Tampered data/signatures are properly rejected
- Wrong keys are detected
- Signature sizes are constant regardless of data size
- Multiple signatures with same key are unique

### 4. LTPAHybridKeysTest.java (450 lines)
**Purpose**: Tests the LTPAHybridKeys immutable container class.

**Test Coverage**:
- Valid key construction
- Null parameter validation (all 8 parameters)
- Incompatible algorithm detection
- Invalid key size detection
- Security level calculation
- Defensive copying (input and output)
- Key clearing (secure memory wiping)
- All three security levels
- toString() implementation

**Key Test Methods**:
- `testConstructor_ValidKeys()` - Tests valid key set construction
- `testConstructor_Null*()` - Tests null parameter handling (8 tests)
- `testConstructor_IncompatibleAlgorithms()` - Tests security level mismatch
- `testConstructor_InvalidMldsaPrivateKeySize()` - Tests ML-DSA key size validation
- `testConstructor_InvalidMldsaPublicKeySize()` - Tests ML-DSA key size validation
- `testConstructor_InvalidMlkemPublicKeySize()` - Tests ML-KEM key size validation
- `testGetSecurityLevel()` - Tests security level calculation
- `testDefensiveCopying()` - Validates input defensive copying
- `testGettersReturnDefensiveCopies()` - Validates output defensive copying
- `testClear()` - Tests secure memory wiping
- `testAllSecurityLevels()` - Tests all three security levels
- `testToString()` - Tests string representation

**Security Features Tested**:
- Immutability (defensive copying on input and output)
- Null safety (all parameters validated)
- Key size validation (NIST FIPS 203/204 compliance)
- Security level consistency (ML-DSA and ML-KEM must match)
- Secure memory wiping (clear() method)

## Running the Tests

### Run All Phase 2 Tests
```bash
cd /Users/utle/libertyGit/open-liberty/dev

# Set Java environment
export JAVA_HOME=/Users/utle/Java/semeru/jdk-17.0.12+7/Contents/Home
export JAVA_21_HOME=/Users/utle/java/OpenJDK/jdk-21.0.4+7/Contents/Home
export JAVA_26_HOME=/Users/utle/java/OpenJDK/jdk-26+35/Contents/Home

# Run all Phase 2 tests
./gradlew com.ibm.ws.security.token.ltpa:test --tests "com.ibm.ws.security.token.ltpa.pqc.LTPAHybridKeyGeneratorTest"
./gradlew com.ibm.ws.security.token.ltpa:test --tests "com.ibm.ws.security.token.ltpa.pqc.MLDSAAlgorithmTypeTest"
./gradlew com.ibm.ws.security.token.ltpa:test --tests "com.ibm.ws.security.token.ltpa.pqc.LTPAPQCSignatureTest"
./gradlew com.ibm.ws.security.token.ltpa:test --tests "com.ibm.ws.security.token.ltpa.pqc.LTPAHybridKeysTest"
```

### Run Individual Test Classes
```bash
# Test hybrid key generation
./gradlew com.ibm.ws.security.token.ltpa:test --tests "com.ibm.ws.security.token.ltpa.pqc.LTPAHybridKeyGeneratorTest"

# Test ML-DSA algorithm types
./gradlew com.ibm.ws.security.token.ltpa:test --tests "com.ibm.ws.security.token.ltpa.pqc.MLDSAAlgorithmTypeTest"

# Test ML-DSA signature operations
./gradlew com.ibm.ws.security.token.ltpa:test --tests "com.ibm.ws.security.token.ltpa.pqc.LTPAPQCSignatureTest"

# Test hybrid key container
./gradlew com.ibm.ws.security.token.ltpa:test --tests "com.ibm.ws.security.token.ltpa.pqc.LTPAHybridKeysTest"
```

### Run Specific Test Methods
```bash
# Test default security level generation
./gradlew com.ibm.ws.security.token.ltpa:test --tests "com.ibm.ws.security.token.ltpa.pqc.LTPAHybridKeyGeneratorTest.testGenerateKeys_DefaultSecurityLevel"

# Test ML-DSA-65 properties
./gradlew com.ibm.ws.security.token.ltpa:test --tests "com.ibm.ws.security.token.ltpa.pqc.MLDSAAlgorithmTypeTest.testML_DSA_65_Properties"

# Test sign and verify
./gradlew com.ibm.ws.security.token.ltpa:test --tests "com.ibm.ws.security.token.ltpa.pqc.LTPAPQCSignatureTest.testSignAndVerify_ML_DSA_65"

# Test defensive copying
./gradlew com.ibm.ws.security.token.ltpa:test --tests "com.ibm.ws.security.token.ltpa.pqc.LTPAHybridKeysTest.testDefensiveCopying"
```

## Test Statistics

| Test Class | Test Methods | Lines of Code | Coverage |
|------------|--------------|---------------|----------|
| LTPAHybridKeyGeneratorTest | 15 | 280 | Hybrid key generation |
| MLDSAAlgorithmTypeTest | 20 | 230 | ML-DSA algorithm types |
| LTPAPQCSignatureTest | 22 | 430 | ML-DSA signature ops |
| LTPAHybridKeysTest | 23 | 450 | Hybrid key container |
| **Total** | **80** | **1,390** | **Comprehensive** |

## Test Dependencies

### Required for Compilation (Java 17+)
- JUnit 4.x
- Open Liberty test framework
- Standard Java cryptography APIs

### Required for Execution (Java 26+)
- Java 26+ with ML-KEM/ML-DSA support (JEP 478)
- SunJCE provider with PQC algorithms
- NIST FIPS 203 (ML-KEM) implementation
- NIST FIPS 204 (ML-DSA) implementation

### Test Behavior
- **Java 17-25**: Tests compile successfully but skip PQC-dependent tests with warning messages
- **Java 26+**: All tests execute with full PQC functionality

## Expected Test Results

### With Java 26+ (Full PQC Support)
```
LTPAHybridKeyGeneratorTest: 15/15 passed
MLDSAAlgorithmTypeTest: 20/20 passed
LTPAPQCSignatureTest: 22/22 passed
LTPAHybridKeysTest: 23/23 passed
Total: 80/80 passed (100%)
```

### With Java 17-25 (No PQC Support)
```
LTPAHybridKeyGeneratorTest: 5/15 passed (10 skipped - PQC not available)
MLDSAAlgorithmTypeTest: 20/20 passed (enum tests don't require PQC)
LTPAPQCSignatureTest: 5/22 passed (17 skipped - PQC not available)
LTPAHybridKeysTest: 10/23 passed (13 skipped - PQC not available)
Total: 40/80 passed, 40 skipped (50% execution rate)
```

## Integration with Phase 1 Tests

Phase 2 tests complement the existing Phase 1 tests:

### Phase 1 Tests (Existing)
- PQCKeyGeneratorTest.java - ML-KEM key generation
- PQCSignatureHelperTest.java - Basic signature operations
- PQCConstantsTest.java - Constants validation

### Phase 2 Tests (New)
- LTPAHybridKeyGeneratorTest.java - Complete hybrid key generation
- MLDSAAlgorithmTypeTest.java - ML-DSA algorithm types
- LTPAPQCSignatureTest.java - Advanced ML-DSA operations
- LTPAHybridKeysTest.java - Hybrid key container

### Combined Coverage
- **Phase 1**: ML-KEM encryption, basic signatures, constants
- **Phase 2**: Hybrid keys (RSA + ML-DSA + ML-KEM), advanced signatures, immutable containers
- **Total**: Complete hybrid PQC LTPA infrastructure

## Next Steps

After Phase 2 unit tests pass:

1. **Phase 3**: Integration tests
   - LTPAToken3 creation and validation
   - End-to-end token lifecycle
   - Backward compatibility with v2 tokens

2. **Phase 4**: FAT (Functional Acceptance Tests)
   - Server-side token generation
   - Token validation across servers
   - Configuration testing

3. **Phase 5**: Performance benchmarking
   - Compare v2 vs v3 token performance
   - Measure key generation overhead
   - Analyze signature/verification times

## Troubleshooting

### Tests Skipped
If tests are skipped with "PQC runtime not available":
- Verify Java 26+ is being used: `java -version`
- Check JAVA_26_HOME environment variable
- Ensure SunJCE provider supports ML-KEM/ML-DSA

### Compilation Errors
If tests fail to compile:
- Verify Java 17+ for compilation
- Check all imports are correct
- Ensure Phase 2 implementation classes exist

### Test Failures
If tests fail:
1. Check Java version (must be 26+ for PQC tests)
2. Verify NIST FIPS 203/204 compliance
3. Review test output for specific error messages
4. Check key sizes match specifications

## Related Documentation

- [Phase 1 Unit Tests](README_UNIT_TESTS.md)
- [PQC Implementation Guide](../../../../../src/com/ibm/ws/security/token/ltpa/pqc/README.md)
- [NIST FIPS 203 - ML-KEM Standard](https://csrc.nist.gov/pubs/fips/203/final)
- [NIST FIPS 204 - ML-DSA Standard](https://csrc.nist.gov/pubs/fips/204/final)
- [JEP 478: Key Encapsulation Mechanism API](https://openjdk.org/jeps/478)