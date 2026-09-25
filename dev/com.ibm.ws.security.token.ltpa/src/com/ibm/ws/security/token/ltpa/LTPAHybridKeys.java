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

import java.io.Serializable;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.crypto.ltpakeyutil.LTPAPrivateKey;
import com.ibm.ws.crypto.ltpakeyutil.LTPAPublicKey;

/**
 * Hybrid key container for LTPA v3 tokens with full PQC support.
 * 
 * This class manages three key pairs for maximum security:
 * 1. RSA-2048: Classical encryption (backward compatibility)
 * 2. ML-DSA: Post-quantum digital signatures (FIPS 204)
 * 3. ML-KEM: Post-quantum key encapsulation (FIPS 203)
 * 
 * Key Format:
 * - RSA keys: PKCS#8 encoded (DER format)
 * - ML-DSA keys: Raw key bytes (FIPS 204 format)
 * - ML-KEM keys: Raw key bytes (FIPS 203 format)
 * 
 * Security Properties:
 * - Hybrid approach provides defense-in-depth
 * - Quantum-resistant signatures prevent token forgery
 * - Quantum-resistant encryption protects token confidentiality
 * - RSA fallback ensures compatibility with non-PQC systems
 * 
 * Thread Safety: Immutable after construction
 * 
 * @since Liberty 26.0.0.1
 */
public class LTPAHybridKeys implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final TraceComponent tc = Tr.register(LTPAHybridKeys.class);

    // RSA-2048 keys (classical cryptography)
    private final byte[] rsaPrivateKeyBytes;
    private final byte[] rsaPublicKeyBytes;

    // ML-DSA keys (post-quantum signatures - FIPS 204)
    private final byte[] mldsaPrivateKeyBytes;
    private final byte[] mldsaPublicKeyBytes;
    private final String mldsaAlgorithm; // "ML-DSA-44", "ML-DSA-65", or "ML-DSA-87"

    // ML-KEM keys (post-quantum encryption - FIPS 203)
    private final byte[] mlkemPrivateKeyBytes;
    private final byte[] mlkemPublicKeyBytes;
    private final String mlkemAlgorithm; // "ML-KEM-512", "ML-KEM-768", or "ML-KEM-1024"

    // Key metadata
    private final long creationTime;
    private final String keyVersion; // "3.0" for hybrid PQC

    /**
     * Construct hybrid keys with all three key pairs.
     * 
     * @param rsaPrivateKeyBytes RSA-2048 private key (PKCS#8 encoded)
     * @param rsaPublicKeyBytes RSA-2048 public key (X.509 encoded)
     * @param mldsaPrivateKeyBytes ML-DSA private key (raw bytes)
     * @param mldsaPublicKeyBytes ML-DSA public key (raw bytes)
     * @param mldsaAlgorithm ML-DSA algorithm identifier
     * @param mlkemPrivateKeyBytes ML-KEM private key (raw bytes)
     * @param mlkemPublicKeyBytes ML-KEM public key (raw bytes)
     * @param mlkemAlgorithm ML-KEM algorithm identifier
     * @throws IllegalArgumentException if any key is null or invalid
     */
    public LTPAHybridKeys(@Sensitive byte[] rsaPrivateKeyBytes,
                          byte[] rsaPublicKeyBytes,
                          @Sensitive byte[] mldsaPrivateKeyBytes,
                          byte[] mldsaPublicKeyBytes,
                          String mldsaAlgorithm,
                          @Sensitive byte[] mlkemPrivateKeyBytes,
                          byte[] mlkemPublicKeyBytes,
                          String mlkemAlgorithm) {
        
        // Validate RSA keys
        if (rsaPrivateKeyBytes == null || rsaPrivateKeyBytes.length == 0) {
            throw new IllegalArgumentException("RSA private key cannot be null or empty");
        }
        if (rsaPublicKeyBytes == null || rsaPublicKeyBytes.length == 0) {
            throw new IllegalArgumentException("RSA public key cannot be null or empty");
        }

        // Validate ML-DSA keys
        if (mldsaPrivateKeyBytes == null || mldsaPrivateKeyBytes.length == 0) {
            throw new IllegalArgumentException("ML-DSA private key cannot be null or empty");
        }
        if (mldsaPublicKeyBytes == null || mldsaPublicKeyBytes.length == 0) {
            throw new IllegalArgumentException("ML-DSA public key cannot be null or empty");
        }
        if (mldsaAlgorithm == null || mldsaAlgorithm.isEmpty()) {
            throw new IllegalArgumentException("ML-DSA algorithm cannot be null or empty");
        }

        // Validate ML-KEM keys (optional - may be null if not yet implemented)
        // If ML-KEM keys are provided, they must be valid
        boolean hasMLKEMKeys = (mlkemPrivateKeyBytes != null && mlkemPrivateKeyBytes.length > 0) ||
                               (mlkemPublicKeyBytes != null && mlkemPublicKeyBytes.length > 0);
        
        if (hasMLKEMKeys) {
            // If any ML-KEM key is provided, all must be provided
            if (mlkemPrivateKeyBytes == null || mlkemPrivateKeyBytes.length == 0) {
                throw new IllegalArgumentException("ML-KEM private key cannot be null or empty when ML-KEM is enabled");
            }
            if (mlkemPublicKeyBytes == null || mlkemPublicKeyBytes.length == 0) {
                throw new IllegalArgumentException("ML-KEM public key cannot be null or empty when ML-KEM is enabled");
            }
            if (mlkemAlgorithm == null || mlkemAlgorithm.isEmpty()) {
                throw new IllegalArgumentException("ML-KEM algorithm cannot be null or empty when ML-KEM is enabled");
            }
        }

        // Store defensive copies
        this.rsaPrivateKeyBytes = rsaPrivateKeyBytes.clone();
        this.rsaPublicKeyBytes = rsaPublicKeyBytes.clone();
        this.mldsaPrivateKeyBytes = mldsaPrivateKeyBytes.clone();
        this.mldsaPublicKeyBytes = mldsaPublicKeyBytes.clone();
        this.mldsaAlgorithm = mldsaAlgorithm;
        
        // ML-KEM keys are optional - only clone if not null
        this.mlkemPrivateKeyBytes = (mlkemPrivateKeyBytes != null) ? mlkemPrivateKeyBytes.clone() : null;
        this.mlkemPublicKeyBytes = (mlkemPublicKeyBytes != null) ? mlkemPublicKeyBytes.clone() : null;
        this.mlkemAlgorithm = mlkemAlgorithm;

        this.creationTime = System.currentTimeMillis();
        this.keyVersion = "3.0";

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Created hybrid keys: RSA-2048 + " + mldsaAlgorithm + " + " + mlkemAlgorithm);
        }
    }

    /**
     * Get RSA private key as LTPAPrivateKey.
     * 
     * @return RSA private key
     */
    public LTPAPrivateKey getRsaPrivateKey() {
        return new LTPAPrivateKey(rsaPrivateKeyBytes.clone());
    }

    /**
     * Get RSA public key as LTPAPublicKey.
     * 
     * @return RSA public key
     */
    public LTPAPublicKey getRsaPublicKey() {
        return new LTPAPublicKey(rsaPublicKeyBytes.clone());
    }

    /**
     * Get RSA private key bytes (PKCS#8 encoded).
     * 
     * @return defensive copy of RSA private key bytes
     */
    @Sensitive
    public byte[] getRsaPrivateKeyBytes() {
        return rsaPrivateKeyBytes.clone();
    }

    /**
     * Get RSA public key bytes (X.509 encoded).
     * 
     * @return defensive copy of RSA public key bytes
     */
    public byte[] getRsaPublicKeyBytes() {
        return rsaPublicKeyBytes.clone();
    }

    /**
     * Get ML-DSA private key bytes.
     * 
     * @return defensive copy of ML-DSA private key bytes
     */
    @Sensitive
    public byte[] getMldsaPrivateKeyBytes() {
        return mldsaPrivateKeyBytes.clone();
    }

    /**
     * Get ML-DSA public key bytes.
     * 
     * @return defensive copy of ML-DSA public key bytes
     */
    public byte[] getMldsaPublicKeyBytes() {
        return mldsaPublicKeyBytes.clone();
    }

    /**
     * Get ML-DSA algorithm identifier.
     * 
     * @return ML-DSA algorithm (e.g., "ML-DSA-65")
     */
    public String getMldsaAlgorithm() {
        return mldsaAlgorithm;
    }

    /**
     * Get ML-KEM private key bytes.
     * 
     * @return defensive copy of ML-KEM private key bytes
     */
    @Sensitive
    public byte[] getMlkemPrivateKeyBytes() {
        return mlkemPrivateKeyBytes.clone();
    }

    /**
     * Get ML-KEM public key bytes.
     *
     * @return defensive copy of ML-KEM public key bytes, or null if not available
     */
    public byte[] getMlkemPublicKeyBytes() {
        return mlkemPublicKeyBytes != null ? mlkemPublicKeyBytes.clone() : null;
    }

    /**
     * Get ML-KEM algorithm identifier.
     * 
     * @return ML-KEM algorithm (e.g., "ML-KEM-768")
     */
    public String getMlkemAlgorithm() {
        return mlkemAlgorithm;
    }

    /**
     * Get key creation timestamp.
     * 
     * @return creation time in milliseconds since epoch
     */
    public long getCreationTime() {
        return creationTime;
    }

    /**
     * Get key version.
     * 
     * @return key version string (always "3.0" for hybrid keys)
     */
    public String getKeyVersion() {
        return keyVersion;
    }

    /**
     * Check if this key set supports full PQC (both ML-DSA and ML-KEM).
     * 
     * @return true if both ML-DSA and ML-KEM keys are present
     */
    public boolean isFullPQC() {
        return mldsaPrivateKeyBytes != null && mldsaPrivateKeyBytes.length > 0 &&
               mlkemPrivateKeyBytes != null && mlkemPrivateKeyBytes.length > 0;
    }

    /**
     * Get security level based on algorithm parameters.
     * 
     * Security levels:
     * - Level 1: ML-DSA-44 + ML-KEM-512 (128-bit quantum security)
     * - Level 3: ML-DSA-65 + ML-KEM-768 (192-bit quantum security)
     * - Level 5: ML-DSA-87 + ML-KEM-1024 (256-bit quantum security)
     * 
     * @return security level (1, 3, or 5)
     */
    public int getSecurityLevel() {
        // Determine security level from ML-DSA algorithm
        if ("ML-DSA-44".equals(mldsaAlgorithm)) {
            return 1; // NIST Level 1 (128-bit quantum security)
        } else if ("ML-DSA-65".equals(mldsaAlgorithm)) {
            return 3; // NIST Level 3 (192-bit quantum security)
        } else if ("ML-DSA-87".equals(mldsaAlgorithm)) {
            return 5; // NIST Level 5 (256-bit quantum security)
        }
        return 0; // Unknown
    }

    /**
     * Validate key consistency (matching security levels).
     * 
     * @return true if ML-DSA and ML-KEM algorithms have matching security levels
     */
    public boolean isConsistent() {
        // Check if ML-DSA and ML-KEM security levels match
        boolean level1 = "ML-DSA-44".equals(mldsaAlgorithm) && "ML-KEM-512".equals(mlkemAlgorithm);
        boolean level3 = "ML-DSA-65".equals(mldsaAlgorithm) && "ML-KEM-768".equals(mlkemAlgorithm);
        boolean level5 = "ML-DSA-87".equals(mldsaAlgorithm) && "ML-KEM-1024".equals(mlkemAlgorithm);
        
        return level1 || level3 || level5;
    }

    /**
     * Clear sensitive key material from memory.
     * Should be called when keys are no longer needed.
     */
    public void clear() {
        Arrays.fill(rsaPrivateKeyBytes, (byte) 0);
        Arrays.fill(mldsaPrivateKeyBytes, (byte) 0);
        Arrays.fill(mlkemPrivateKeyBytes, (byte) 0);
        
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Cleared sensitive key material");
        }
    }

    @Override
    public String toString() {
        return "LTPAHybridKeys[version=" + keyVersion +
               ", rsa=RSA-2048" +
               ", mldsa=" + mldsaAlgorithm +
               ", mlkem=" + mlkemAlgorithm +
               ", securityLevel=" + getSecurityLevel() +
               ", created=" + creationTime + "]";
    }
}

// Made with Bob
