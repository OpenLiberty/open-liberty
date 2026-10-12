# PQC Unit Tests for LTPAKeyInfoManager

## Overview
Added comprehensive unit tests for Post-Quantum Cryptography (PQC) functionality in `LTPAKeyInfoManager.java`.

## New Test File
**File**: `LTPAKeyInfoManagerTest_PQC.java`
**Location**: `com.ibm.ws.security.token.ltpa/test/com/ibm/ws/security/token/ltpa/`
**Test Count**: 5 test methods

## Test Coverage

### 1. testLoadPQCKeys()
**Purpose**: Verify that LTPA key files with PQC ML-DSA keys are properly loaded and cached.

**What it tests**:
- Loading classical LTPA keys (secret, private, public)
- Loading PQC ML-DSA keys (private, public)
- Both key types are successfully cached

**Expected behavior**: All keys (classical + PQC) should be non-null after loading.

---

### 2. testGetMLDSAKeysNotPresent()
**Purpose**: Verify correct behavior when PQC keys are not present in an LTPA key file.

**What it tests**:
- Loading a classical LTPA key file without PQC keys
- Attempting to retrieve ML-DSA keys that don't exist

**Expected behavior**: 
- Classical keys should load successfully
- ML-DSA key getters should return `null` (not throw exceptions)

---

### 3. testGetMLDSAKeysForNonExistentFile()
**Purpose**: Verify correct behavior when retrieving PQC keys for a file that was never loaded.

**What it tests**:
- Calling `getMLDSAPrivateKey()` and `getMLDSAPublicKey()` for non-existent file

**Expected behavior**: Both methods should return `null` (graceful handling).

---

### 4. testPQCKeyCaching()
**Purpose**: Verify that PQC keys are properly cached and reused.

**What it tests**:
- Loading PQC keys from file
- Retrieving keys multiple times
- Verifying same cached instances are returned

**Expected behavior**: 
- Keys should be cached after first load
- Subsequent retrievals should return same object instances (not reload from file)

---

### 5. testLoadHybridModeKeys()
**Purpose**: Verify hybrid mode operation with both classical and PQC keys.

**What it tests**:
- Loading LTPA key file configured for hybrid mode
- Presence of all classical keys (RSA-based)
- Presence of all PQC keys (ML-DSA-based)

**Expected behavior**: All key types should be present and accessible.

---

## Code Changes Tested

These tests cover the PQC functionality added to `LTPAKeyInfoManager.java`:

1. **Lines 86-88**: PQC key cache identifiers
   ```java
   private static final String MLDSA_PRIVATEKEY = "mldsaprivatekey";
   private static final String MLDSA_PUBLICKEY = "mldsapublickey";
   ```

2. **Lines 270-280**: Loading ML-DSA keys from properties file
   ```java
   String mldsaPrivateKeyStr = props.getProperty(LTPAKeyFileUtility.KEYIMPORT_MLDSA_PRIVATEKEY);
   String mldsaPublicKeyStr = props.getProperty(LTPAKeyFileUtility.KEYIMPORT_MLDSA_PUBLICKEY);
   ```

3. **Lines 318-348**: Decoding and caching ML-DSA keys
   ```java
   byte[] mldsaPrivateKey = Base64Coder.base64DecodeString(mldsaPrivateKeyStr);
   this.keyCache.put(keyImportFile + MLDSA_PRIVATEKEY, mldsaPrivateKey);
   ```

4. **Lines 604-616**: New getter methods
   ```java
   public byte[] getMLDSAPrivateKey(String keyImportFile)
   public byte[] getMLDSAPublicKey(String keyImportFile)
   ```

---

## Test Data Requirements

The tests expect the following test LTPA key files to exist:

1. **security.token.ltpa.keys.pqc.txt**
   - Contains classical LTPA keys + PQC ML-DSA keys
   - Used by: `testLoadPQCKeys()`, `testPQCKeyCaching()`

2. **security.token.ltpa.keys.hybrid.txt**
   - Contains both classical and PQC keys for hybrid mode
   - Used by: `testLoadHybridModeKeys()`

3. **security.token.ltpa.keys.correct.txt** (already exists)
   - Contains only classical LTPA keys (no PQC)
   - Used by: `testGetMLDSAKeysNotPresent()`

**Note**: Test key files need to be created in the test resources directory:
`com.ibm.ws.security.token.ltpa/test-resources/security/`

---

## Running the Tests

### Compile and run all unit tests:
```bash
source ~/.bash_profile && ./gradlew com.ibm.ws.security.token.ltpa:test
```

### Run only PQC tests:
```bash
source ~/.bash_profile && ./gradlew com.ibm.ws.security.token.ltpa:test --tests LTPAKeyInfoManagerTest_PQC
```

### Run a specific test method:
```bash
source ~/.bash_profile && ./gradlew com.ibm.ws.security.token.ltpa:test --tests LTPAKeyInfoManagerTest_PQC.testLoadPQCKeys
```

---

## Integration with Existing Tests

The new `LTPAKeyInfoManagerTest_PQC` class complements the existing `LTPAKeyInfoManagerTest` class:

- **LTPAKeyInfoManagerTest**: Tests classical LTPA functionality
- **LTPAKeyInfoManagerTest_PQC**: Tests PQC-specific functionality

Both test classes use the same test infrastructure (`UTLocationHelper`, `SharedOutputManager`).

---

## Next Steps

1. **Create test LTPA key files** with PQC keys for the tests to use
2. **Run unit tests** to verify PQC key loading works correctly
3. **Fix any issues** discovered during testing
4. **Update FAT tests** once unit tests pass

---

## Related Files

- **Implementation**: `com.ibm.ws.security.token.ltpa/src/com/ibm/ws/security/token/ltpa/LTPAKeyInfoManager.java`
- **Existing Tests**: `com.ibm.ws.security.token.ltpa/test/com/ibm/ws/security/token/ltpa/LTPAKeyInfoManagerTest.java`
- **New Tests**: `com.ibm.ws.security.token.ltpa/test/com/ibm/ws/security/token/ltpa/LTPAKeyInfoManagerTest_PQC.java`
- **FAT Tests**: `com.ibm.ws.security.token.ltpa.pqc_fat/fat/src/com/ibm/ws/security/token/ltpa/pqc/fat/PQCLTPATests.java`
