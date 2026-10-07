# PQC Test LTPA Key Files

This directory contains test LTPA key files for Post-Quantum Cryptography (PQC) unit testing.

## Test Key Files

### 1. security.token.ltpa.keys.pqc.txt
**Purpose**: Test PQC mode with ML-DSA keys

**Contents**:
- Classical LTPA keys (3DES secret key, RSA private/public keys)
- PQC ML-DSA keys (private and public)
- PQC Algorithm: ML-DSA-65
- Crypto Mode: pqc
- Realm: PQCTestRealm

**Used by**:
- `LTPAKeyInfoManagerTest_PQC.testLoadPQCKeys()`
- `LTPAKeyInfoManagerTest_PQC.testPQCKeyCaching()`

**Properties**:
```
com.ibm.websphere.ltpa.PQCAlgorithm=ML-DSA-65
com.ibm.websphere.ltpa.CryptoMode=pqc
com.ibm.websphere.ltpa.MLDSAPrivateKey=<base64-encoded-key>
com.ibm.websphere.ltpa.MLDSAPublicKey=<base64-encoded-key>
```

---

### 2. security.token.ltpa.keys.hybrid.txt
**Purpose**: Test hybrid mode with both classical and PQC keys

**Contents**:
- Classical LTPA keys (3DES secret key, RSA private/public keys)
- PQC ML-DSA keys (private and public)
- PQC Algorithm: ML-DSA-65
- Crypto Mode: hybrid
- Realm: HybridTestRealm

**Used by**:
- `LTPAKeyInfoManagerTest_PQC.testLoadHybridModeKeys()`

**Properties**:
```
com.ibm.websphere.ltpa.PQCAlgorithm=ML-DSA-65
com.ibm.websphere.ltpa.CryptoMode=hybrid
com.ibm.websphere.ltpa.MLDSAPrivateKey=<base64-encoded-key>
com.ibm.websphere.ltpa.MLDSAPublicKey=<base64-encoded-key>
```

---

## Key Properties Reference

### Standard LTPA Properties (present in all files)
- `com.ibm.websphere.ltpa.version` - LTPA version (1.0 = non-FIPS, 2.0 = FIPS)
- `com.ibm.websphere.ltpa.3DESKey` - Encrypted 3DES secret key (base64)
- `com.ibm.websphere.ltpa.PrivateKey` - Encrypted RSA private key (base64)
- `com.ibm.websphere.ltpa.PublicKey` - RSA public key (base64)
- `com.ibm.websphere.ltpa.Realm` - Security realm name

### PQC-Specific Properties (new)
- `com.ibm.websphere.ltpa.PQCAlgorithm` - PQC algorithm (ML-DSA-44, ML-DSA-65, ML-DSA-87)
- `com.ibm.websphere.ltpa.CryptoMode` - Crypto mode (classical, pqc, hybrid)
- `com.ibm.websphere.ltpa.MLDSAPrivateKey` - ML-DSA private key (base64)
- `com.ibm.websphere.ltpa.MLDSAPublicKey` - ML-DSA public key (base64)

---

## Important Notes

### Test Keys Only
**WARNING**: The PQC keys in these files are **placeholder values for testing purposes only**. They are NOT real cryptographic keys and should NEVER be used in production.

The base64-encoded values are test strings like:
- "testMLDSAPostQuantumPrivateKeyForUnitTestingPurposesOnly..."
- "hybridMLDSAPostQuantumPrivateKeyForUnitTestingPurposesOnly..."

### Key Password
All test key files use the standard test password: **"WebAS"**

This is defined in the test class as:
```java
private static final byte[] KEYPASSWORD_CORRECT = "WebAS".getBytes();
```

### File Format
LTPA key files follow Java Properties file format:
- Lines starting with `#` are comments
- Properties use `key=value` format
- Special characters are escaped with backslash (e.g., `\:` for colon)
- Long values can span multiple lines (continuation with backslash)

---

## Creating Real PQC Keys

To create real PQC LTPA keys for actual use (not testing), you would need to:

1. Generate real ML-DSA key pairs using a proper PQC library
2. Encrypt the private keys using the LTPA key password
3. Base64-encode all keys
4. Create the properties file with proper metadata

**Example workflow** (pseudocode):
```java
// Generate ML-DSA key pair
KeyPair mldsaKeyPair = PQCKeyGenerator.generateMLDSAKeyPair("ML-DSA-65");

// Encrypt private key
byte[] encryptedPrivateKey = encryptKey(mldsaKeyPair.getPrivate(), keyPassword);

// Base64 encode
String mldsaPrivateKeyBase64 = Base64.encode(encryptedPrivateKey);
String mldsaPublicKeyBase64 = Base64.encode(mldsaKeyPair.getPublic().getEncoded());

// Write to properties file
properties.setProperty("com.ibm.websphere.ltpa.MLDSAPrivateKey", mldsaPrivateKeyBase64);
properties.setProperty("com.ibm.websphere.ltpa.MLDSAPublicKey", mldsaPublicKeyBase64);
```

---

## Related Files

- **Test Class**: `com.ibm.ws.security.token.ltpa/test/com/ibm/ws/security/token/ltpa/LTPAKeyInfoManagerTest_PQC.java`
- **Implementation**: `com.ibm.ws.security.token.ltpa/src/com/ibm/ws/security/token/ltpa/LTPAKeyInfoManager.java`
- **Constants**: `com.ibm.ws.crypto.ltpakeyutil/src/com/ibm/ws/crypto/ltpakeyutil/LTPAKeyFileUtility.java`

---

## Running Tests

To run the PQC unit tests that use these key files:

```bash
# Run all PQC tests
source ~/.bash_profile && ./gradlew com.ibm.ws.security.token.ltpa:test --tests LTPAKeyInfoManagerTest_PQC

# Run specific test
source ~/.bash_profile && ./gradlew com.ibm.ws.security.token.ltpa:test --tests LTPAKeyInfoManagerTest_PQC.testLoadPQCKeys
```

---

## Troubleshooting

### Test fails with "File not found"
- Verify the test key files exist in the correct directory
- Check file permissions (should be readable)
- Verify the file path in the test matches the actual location

### Test fails with "Key decoding error"
- Verify the base64-encoded keys are valid
- Check for line breaks or whitespace in the base64 strings
- Ensure the properties file format is correct (no extra spaces, proper escaping)

### Test fails with "Null key returned"
- Verify the property names match exactly (case-sensitive)
- Check that the key properties are present in the file
- Ensure the file is being loaded correctly by LTPAKeyInfoManager

---

Last Updated: April 24, 2026
