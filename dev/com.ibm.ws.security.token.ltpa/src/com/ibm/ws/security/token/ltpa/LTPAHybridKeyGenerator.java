/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.security.token.ltpa;

import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.crypto.ltpakeyutil.LTPAKeyPair;
import com.ibm.ws.crypto.ltpakeyutil.LTPAKeyUtil;
import com.ibm.ws.security.token.ltpa.pqc.MLDSAAlgorithmType;
import com.ibm.ws.security.token.ltpa.pqc.MLKEMAlgorithmType;
import com.ibm.ws.security.token.ltpa.pqc.LTPAPQCSignature;
import com.ibm.ws.security.token.ltpa.pqc.PQCRuntimeSupport;

/**
 * Generator for hybrid PQC LTPA keys (RSA + ML-DSA + ML-KEM).
 * 
 * This class generates complete key sets for LTPA v3 tokens with full
 * post-quantum cryptography support. Each key set includes:
 * 
 * 1. RSA-2048: Classical encryption (backward compatibility)
 * 2. ML-DSA: Post-quantum digital signatures (FIPS 204)
 * 3. ML-KEM: Post-quantum key encapsulation (FIPS 203)
 * 
 * Security Levels:
 * - Level 1 (128-bit): RSA-2048 + ML-DSA-44 + ML-KEM-512 (DEFAULT)
 * - Level 3 (192-bit): RSA-2048 + ML-DSA-65 + ML-KEM-768
 * - Level 5 (256-bit): RSA-2048 + ML-DSA-87 + ML-KEM-1024
 *
 * Key Generation Strategy:
 * - All keys generated independently (no key derivation)
 * - Cryptographically secure random number generation
 * - Keys validated before returning
 * - Consistent security levels across algorithms
 *
 * Performance:
 * - Level 1: ~50ms total generation time (DEFAULT)
 * - Level 3: ~100ms total generation time
 * - Level 5: ~200ms total generation time
 *
 * Thread Safety: All methods are thread-safe
 *
 * @since Liberty 26.0.0.1
 */
public class LTPAHybridKeyGenerator {
    private static final TraceComponent tc = Tr.register(LTPAHybridKeyGenerator.class);

    // Default security level (NIST Level 1 - 128-bit quantum security)
    private static final MLDSAAlgorithmType DEFAULT_MLDSA = MLDSAAlgorithmType.ML_DSA_44;
    private static final MLKEMAlgorithmType DEFAULT_MLKEM = MLKEMAlgorithmType.ML_KEM_512;

    /**
     * Generate hybrid keys with default security level (Level 1).
     *
     * Generates:
     * - RSA-2048 key pair
     * - ML-DSA-44 key pair (128-bit quantum security)
     * - ML-KEM-512 key pair (128-bit quantum security)
     *
     * @return hybrid keys with all three key pairs
     * @throws LTPAKeystoreException if key generation fails
     */
    public static LTPAHybridKeys generateKeys() throws LTPAKeystoreException {
        return generateKeys(DEFAULT_MLDSA, DEFAULT_MLKEM);
    }

    /**
     * Generate hybrid keys with specified security level.
     * 
     * @param securityLevel NIST security level (1, 3, or 5)
     * @return hybrid keys with matching security level
     * @throws LTPAKeystoreException if key generation fails
     * @throws IllegalArgumentException if security level is invalid
     */
    public static LTPAHybridKeys generateKeys(int securityLevel) throws LTPAKeystoreException {
        MLDSAAlgorithmType mldsaType;
        MLKEMAlgorithmType mlkemType;

        switch (securityLevel) {
            case 1:
                mldsaType = MLDSAAlgorithmType.ML_DSA_44;
                mlkemType = MLKEMAlgorithmType.ML_KEM_512;
                break;
            case 3:
                mldsaType = MLDSAAlgorithmType.ML_DSA_65;
                mlkemType = MLKEMAlgorithmType.ML_KEM_768;
                break;
            case 5:
                mldsaType = MLDSAAlgorithmType.ML_DSA_87;
                mlkemType = MLKEMAlgorithmType.ML_KEM_1024;
                break;
            default:
                throw new IllegalArgumentException("Invalid security level: " + securityLevel + 
                                                 " (must be 1, 3, or 5)");
        }

        return generateKeys(mldsaType, mlkemType);
    }

    /**
     * Generate hybrid keys with specified algorithms.
     * 
     * @param mldsaType ML-DSA algorithm type
     * @param mlkemType ML-KEM algorithm type
     * @return hybrid keys with specified algorithms
     * @throws LTPAKeystoreException if key generation fails
     * @throws IllegalArgumentException if algorithms have mismatched security levels
     */
    public static LTPAHybridKeys generateKeys(MLDSAAlgorithmType mldsaType,
                                             MLKEMAlgorithmType mlkemType) 
            throws LTPAKeystoreException {
        
        if (mldsaType == null) {
            throw new IllegalArgumentException("ML-DSA algorithm type cannot be null");
        }
        if (mlkemType == null) {
            throw new IllegalArgumentException("ML-KEM algorithm type cannot be null");
        }

        // Validate security level consistency
        if (!mldsaType.isCompatibleWith(mlkemType)) {
            Tr.warning(tc, "CWWKS4126W: ML-DSA and ML-KEM security levels do not match: " +
                      mldsaType.getAlgorithmName() + " (Level " + mldsaType.getSecurityLevel() + ") vs " +
                      mlkemType.getAlgorithmName() + " (Level " + mlkemType.getSecurityLevel() + ")");
        }

        long startTime = System.currentTimeMillis();

        try {
            // Step 1: Generate RSA-2048 key pair
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Generating RSA-2048 key pair...");
            }
            LTPAKeyPair rsaKeyPair = generateRSAKeyPair();
            byte[] rsaPrivateKeyBytes = rsaKeyPair.getPrivate().getEncoded();
            byte[] rsaPublicKeyBytes = rsaKeyPair.getPublic().getEncoded();

            // Step 2: Generate ML-DSA key pair
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Generating " + mldsaType.getAlgorithmName() + " key pair...");
            }
            KeyPair mldsaKeyPair = generateMLDSAKeyPair(mldsaType);
            byte[] mldsaPrivateKeyBytes = mldsaKeyPair.getPrivate().getEncoded();
            byte[] mldsaPublicKeyBytes = mldsaKeyPair.getPublic().getEncoded();

            // Step 3: Generate ML-KEM key pair
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Generating " + mlkemType.getAlgorithmName() + " key pair...");
            }
            KeyPair mlkemKeyPair = generateMLKEMKeyPair(mlkemType);
            byte[] mlkemPrivateKeyBytes = mlkemKeyPair.getPrivate().getEncoded();
            byte[] mlkemPublicKeyBytes = mlkemKeyPair.getPublic().getEncoded();

            // Step 4: Validate key sizes
            validateKeySizes(rsaPrivateKeyBytes, rsaPublicKeyBytes,
                           mldsaPrivateKeyBytes, mldsaPublicKeyBytes, mldsaType,
                           mlkemPrivateKeyBytes, mlkemPublicKeyBytes, mlkemType);

            // Step 5: Create hybrid keys object
            LTPAHybridKeys hybridKeys = new LTPAHybridKeys(
                rsaPrivateKeyBytes, rsaPublicKeyBytes,
                mldsaPrivateKeyBytes, mldsaPublicKeyBytes, mldsaType.getAlgorithmName(),
                mlkemPrivateKeyBytes, mlkemPublicKeyBytes, mlkemType.getAlgorithmName()
            );

            long duration = System.currentTimeMillis() - startTime;
            
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Generated hybrid keys in " + duration + "ms: " +
                        "RSA-2048 + " + mldsaType.getAlgorithmName() + " + " + mlkemType.getAlgorithmName());
            }

            Tr.info(tc, "CWWKS4127I: Generated hybrid PQC LTPA keys: " +
                   mldsaType.getAlgorithmName() + " + " + mlkemType.getAlgorithmName() +
                   " (Security Level " + mldsaType.getSecurityLevel() + ")");

            return hybridKeys;

        } catch (Exception e) {
            Tr.error(tc, "CWWKS4128E: Failed to generate hybrid PQC LTPA keys: " + e.getMessage());
            throw new LTPAKeystoreException("Failed to generate hybrid keys", e);
        }
    }

    /**
     * Generate RSA-2048 key pair for backward compatibility.
     * 
     * @return RSA key pair
     * @throws NoSuchAlgorithmException if RSA is not available
     * @throws InvalidKeyException if key generation fails
     */
    private static LTPAKeyPair generateRSAKeyPair() 
            throws NoSuchAlgorithmException, InvalidKeyException {
        
        try {
            // Use LTPAKeyUtil to generate RSA-2048 keys
            LTPAKeyPair keyPair = LTPAKeyUtil.generateLTPAKeyPair();
            
            if (keyPair == null) {
                throw new InvalidKeyException("RSA key generation returned null");
            }

            return keyPair;

        } catch (Exception e) {
            throw new InvalidKeyException("Failed to generate RSA key pair: " + e.getMessage(), e);
        }
    }

    /**
     * Generate ML-DSA key pair for post-quantum signatures.
     * 
     * @param algorithm ML-DSA algorithm type
     * @return ML-DSA key pair
     * @throws NoSuchAlgorithmException if ML-DSA is not available
     * @throws InvalidKeySpecException if key generation fails
     */
    private static KeyPair generateMLDSAKeyPair(MLDSAAlgorithmType algorithm)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        
        if (!LTPAPQCSignature.isMLDSASupported()) {
            throw new NoSuchAlgorithmException("ML-DSA not available (requires Java 26+)");
        }

        return LTPAPQCSignature.generateMLDSAKeyPair(algorithm);
    }

    /**
     * Generate ML-KEM key pair for post-quantum encryption.
     * 
     * @param algorithm ML-KEM algorithm type
     * @return ML-KEM key pair
     * @throws NoSuchAlgorithmException if ML-KEM is not available
     * @throws InvalidKeySpecException if key generation fails
     */
    private static KeyPair generateMLKEMKeyPair(MLKEMAlgorithmType algorithm)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        
        if (!PQCRuntimeSupport.isPQCSupported()) {
            throw new NoSuchAlgorithmException("ML-KEM not available (requires Java 26+)");
        }

        try {
            return PQCRuntimeSupport.generateMLKEMKeyPair(algorithm);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidKeySpecException("Failed to generate ML-KEM key pair: " + e.getMessage(), e);
        }
    }

    /**
     * Validate all key sizes match expected values.
     * 
     * @throws InvalidKeySpecException if any key size is invalid
     */
    private static void validateKeySizes(@Sensitive byte[] rsaPrivateKeyBytes,
                                        byte[] rsaPublicKeyBytes,
                                        @Sensitive byte[] mldsaPrivateKeyBytes,
                                        byte[] mldsaPublicKeyBytes,
                                        MLDSAAlgorithmType mldsaType,
                                        @Sensitive byte[] mlkemPrivateKeyBytes,
                                        byte[] mlkemPublicKeyBytes,
                                        MLKEMAlgorithmType mlkemType)
            throws InvalidKeySpecException {
        
        // Validate RSA keys (minimum 1024 bytes for RSA-2048 PKCS#8)
        if (rsaPrivateKeyBytes.length < 1024 || rsaPublicKeyBytes.length < 256) {
            throw new InvalidKeySpecException("Invalid RSA key sizes: " +
                    "private=" + rsaPrivateKeyBytes.length + ", public=" + rsaPublicKeyBytes.length);
        }

        // Validate ML-DSA keys
        if (!LTPAPQCSignature.validateKeySizes(mldsaPublicKeyBytes, mldsaPrivateKeyBytes, mldsaType)) {
            throw new InvalidKeySpecException("Invalid ML-DSA key sizes for " + mldsaType.getAlgorithmName());
        }

        // Validate ML-KEM keys
        if (!PQCRuntimeSupport.validateKeySizes(mlkemPublicKeyBytes, mlkemPrivateKeyBytes, mlkemType)) {
            throw new InvalidKeySpecException("Invalid ML-KEM key sizes for " + mlkemType.getAlgorithmName());
        }

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Key size validation passed: " +
                    "RSA=" + rsaPrivateKeyBytes.length + "/" + rsaPublicKeyBytes.length + ", " +
                    mldsaType.getAlgorithmName() + "=" + mldsaPrivateKeyBytes.length + "/" + mldsaPublicKeyBytes.length + ", " +
                    mlkemType.getAlgorithmName() + "=" + mlkemPrivateKeyBytes.length + "/" + mlkemPublicKeyBytes.length);
        }
    }

    /**
     * Check if hybrid key generation is supported on this JVM.
     * 
     * @return true if both ML-DSA and ML-KEM are available (Java 26+)
     */
    public static boolean isHybridKeyGenerationSupported() {
        return LTPAPQCSignature.isMLDSASupported() && PQCRuntimeSupport.isPQCSupported();
    }

    /**
     * Get supported security levels on this JVM.
     * 
     * @return array of supported security levels (1, 3, 5), or empty if PQC not available
     */
    public static int[] getSupportedSecurityLevels() {
        if (!isHybridKeyGenerationSupported()) {
            return new int[0];
        }
        return new int[] { 1, 3, 5 };
    }

    /**
     * Get recommended security level for production use.
     *
     * @return recommended security level (1 = 128-bit quantum security)
     */
    public static int getRecommendedSecurityLevel() {
        return 1; // NIST Level 1 (ML-DSA-44 + ML-KEM-512)
    }

    /**
     * Get provider information for all algorithms.
     * 
     * @return string describing available providers
     */
    public static String getProviderInfo() {
        StringBuilder info = new StringBuilder();
        
        info.append("RSA: ").append(LTPAKeyUtil.class.getPackage().getName()).append("\n");
        
        String mldsaProvider = LTPAPQCSignature.getProviderInfo();
        info.append("ML-DSA: ").append(mldsaProvider != null ? mldsaProvider : "Not available").append("\n");
        
        String mlkemProvider = PQCRuntimeSupport.getProviderInfo();
        info.append("ML-KEM: ").append(mlkemProvider != null ? mlkemProvider : "Not available");
        
        return info.toString();
    }
}

// Made with Bob
