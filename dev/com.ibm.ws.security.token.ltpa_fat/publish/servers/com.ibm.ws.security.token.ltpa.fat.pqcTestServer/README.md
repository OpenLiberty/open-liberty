# LTPA Hybrid PQC Test Server

This test server is configured to test Hybrid Post-Quantum Cryptography (PQC) support in LTPA tokens.

## Overview

This server demonstrates the use of LTPA Token Version 3 with **triple-layer hybrid cryptography**:
- **RSA-2048** for classical digital signatures (backward compatibility)
- **ML-DSA-65** for quantum-resistant digital signatures (NIST FIPS 204)
- **ML-KEM-768** for quantum-resistant key encapsulation (NIST FIPS 203)

This defense-in-depth approach ensures security even if one cryptographic system is compromised.

## Prerequisites

### Java 26 Required for Full Hybrid PQC Support
To test the full hybrid PQC functionality, you must run this server with **Java 26 or later**, which includes:
- **ML-KEM (FIPS 203)** support via JEP 478 - Quantum-resistant key encapsulation
- **ML-DSA (FIPS 204)** support via JEP 478 - Quantum-resistant digital signatures
- Native post-quantum cryptography implementations

### Fallback Behavior with Java 17
If you run with Java 17, the server will:
- Compile successfully (uses reflection-based compatibility layer)
- Detect that Java 26 is not available at runtime
- Fall back to RSA-only mode (LTPA Token Version 2)
- Log warnings about ML-DSA and ML-KEM unavailability

## Configuration

### LTPA Configuration (server.xml)
```xml
<ltpa
    keysFileName="${server.output.dir}/resources/security/ltpa.keys"
    keysPassword="{xor}Lz4sLCgwLTs="
    expiration="30m"
    tokenVersion="3"
    hybridPqcEnabled="true"
    hybridKeystoreFile="${server.output.dir}/resources/security/ltpa-hybrid.p12"
    hybridKeystorePassword="{xor}Lz4sLCgwLTs="
    mldsaAlgorithm="ML-DSA-65"
    mlkemAlgorithm="ML-KEM-768"/>
```

### Key Configuration Parameters

| Parameter | Value | Description |
|-----------|-------|-------------|
| `tokenVersion` | `3` | Enables LTPA Token Version 3 (Hybrid PQC) |
| `hybridPqcEnabled` | `true` | Activates Hybrid Post-Quantum Cryptography |
| `mldsaAlgorithm` | `ML-DSA-65` | Quantum-resistant signatures (44/65/87) |
| `mlkemAlgorithm` | `ML-KEM-768` | Quantum-resistant key encapsulation (512/768/1024) |
| `hybridKeystoreFile` | Path to PKCS12 | Stores RSA + ML-DSA + ML-KEM keys |

### Algorithm Security Levels

**ML-DSA (Digital Signatures):**
- `ML-DSA-44`: NIST Level 2 (128-bit quantum security)
- `ML-DSA-65`: NIST Level 3 (192-bit quantum security) ⭐ **Recommended**
- `ML-DSA-87`: NIST Level 5 (256-bit quantum security)

**ML-KEM (Key Encapsulation):**
- `ML-KEM-512`: NIST Level 1 (128-bit quantum security)
- `ML-KEM-768`: NIST Level 3 (192-bit quantum security) ⭐ **Recommended**
- `ML-KEM-1024`: NIST Level 5 (256-bit quantum security)

### Passwords
- LTPA Keys Password: `ltpapwd` (XOR encoded: `{xor}Lz4sLCgwLTs=`)
- Hybrid Keystore Password: `ltpapwd` (XOR encoded: `{xor}Lz4sLCgwLTs=`)

## Running the Server

### With Java 26 (Full PQC Support)
```bash
export JAVA_HOME=/path/to/java-26
cd /Users/utle/libertyGit/open-liberty/dev
./gradlew :com.ibm.ws.security.token.ltpa_fat:buildandrun
```

### With Java 17 (RSA Fallback)
```bash
export JAVA_HOME=/path/to/java-17
cd /Users/utle/libertyGit/open-liberty/dev
./gradlew :com.ibm.ws.security.token.ltpa_fat:buildandrun
```

## Testing PQC Functionality

### 1. Check Server Logs
Look for these messages in `logs/messages.log`:
```
CWWKS4105I: LTPA configuration is ready after X seconds.
CWWKS4104A: LTPA keys created in X seconds. LTPA key file: ltpa.keys
```

With Java 26, you should also see:
```
[INFO] PQC Runtime Support: Java 26+ detected
[INFO] ML-DSA support available: true
[INFO] ML-KEM support available: true
[INFO] Hybrid keystore created: ltpa-hybrid.p12
[INFO] Generated RSA-2048 + ML-DSA-65 + ML-KEM-768 hybrid keys
```

### 2. Verify Hybrid Keys Generated
Check that the hybrid keystore was created:
```bash
ls -la wlp/usr/servers/com.ibm.ws.security.token.ltpa.fat.pqcTestServer/resources/security/
```

You should see:
- `ltpa.keys` (traditional RSA keys for backward compatibility)
- `ltpa-hybrid.p12` (Hybrid keystore with RSA + ML-DSA + ML-KEM keys)

### 3. Test Token Creation
Access a protected resource to trigger LTPA token creation:
```bash
curl -u testuser:testpwd http://localhost:9080/protected-resource
```

Check the `Set-Cookie` header for the LTPA token.

### 4. Inspect Trace Logs
Enable detailed tracing in `server.xml`:
```xml
<logging 
    traceSpecification="*=info:com.ibm.ws.security.token.ltpa*=all:com.ibm.ws.security.token.ltpa.pqc*=all"/>
```

Look for hybrid PQC-specific trace in `logs/trace.log`:
```
[PQCRuntimeSupport] Checking Java version for PQC support...
[PQCRuntimeSupport] Java 26 detected, ML-DSA and ML-KEM available
[LTPAHybridKeyGenerator] Generating RSA-2048 key pair...
[LTPAHybridKeyGenerator] Generating ML-DSA-65 key pair...
[LTPAHybridKeyGenerator] Generating ML-KEM-768 key pair...
[LTPAToken3] Creating token with hybrid signatures (RSA + ML-DSA)
[LTPAToken3] Encrypting token with ML-KEM + AES-GCM...
[LTPAToken3] Verifying dual signatures: RSA and ML-DSA
```

## Troubleshooting

### Issue: "ML-DSA not available" or "ML-KEM not available" warning
**Cause:** Running with Java 17 or earlier
**Solution:** Use Java 26 or later for full hybrid PQC support

### Issue: Hybrid keystore not created
**Cause:** Insufficient permissions or invalid path
**Solution:** Check server has write access to `${server.output.dir}/resources/security/`

### Issue: Token validation fails
**Cause:** Clock skew, expired tokens, or signature verification failure
**Solution:**
- Check system time synchronization
- Adjust `expiration` setting
- Verify both RSA and ML-DSA signatures are valid
- Check trace logs for specific signature verification errors

### Issue: "LTPA_TOKEN3_FACTORY_NO_HYBRID_KEYS" error
**Cause:** Hybrid keys not initialized in factory
**Solution:** Ensure `hybridPqcEnabled="true"` and hybrid keystore file exists

## Security Notes

1. **Passwords:** Change default passwords in production
2. **Keystore Protection:** Secure the hybrid keystore file with appropriate file permissions (chmod 600)
3. **Key Rotation:** Implement regular key rotation for all three key types (RSA, ML-DSA, ML-KEM)
4. **Algorithm Selection:**
   - **ML-DSA-65 + ML-KEM-768** (recommended): NIST Level 3, good balance of security and performance
   - **ML-DSA-44 + ML-KEM-512**: Faster, lower security margin (NIST Level 1-2)
   - **ML-DSA-87 + ML-KEM-1024**: Slower, higher security margin (NIST Level 5)
5. **Defense-in-Depth:** Both RSA and ML-DSA signatures must be valid for token acceptance
6. **Quantum Readiness:** This implementation is quantum-safe and future-proof

## References

- [NIST FIPS 203: ML-KEM (Module-Lattice-Based Key-Encapsulation Mechanism)](https://csrc.nist.gov/pubs/fips/203/final)
- [NIST FIPS 204: ML-DSA (Module-Lattice-Based Digital Signature Algorithm)](https://csrc.nist.gov/pubs/fips/204/final)
- [JEP 478: Key Encapsulation Mechanism API](https://openjdk.org/jeps/478)
- [Open Liberty LTPA Documentation](https://openliberty.io/docs/latest/reference/config/ltpa.html)
- [NIST Post-Quantum Cryptography Standardization](https://csrc.nist.gov/projects/post-quantum-cryptography)

## Test Users

| Username | Password | Role |
|----------|----------|------|
| testuser | testpwd  | User |
| user1    | user1pwd | User |
| user2    | user2pwd | User |

---
**Created with IBM Bob** - PQC LTPA Implementation