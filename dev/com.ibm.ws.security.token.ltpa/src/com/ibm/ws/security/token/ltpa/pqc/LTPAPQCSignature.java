/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.security.token.ltpa.pqc;

import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;

/**
 * ML-DSA (Module-Lattice-Based Digital Signature Algorithm) operations for LTPA v3 tokens.
 * 
 * This class provides post-quantum digital signature capabilities using ML-DSA
 * (FIPS 204), the NIST-standardized lattice-based signature algorithm.
 * 
 * ML-DSA provides:
 * - Quantum-resistant digital signatures
 * - Three security levels (128, 192, 256-bit quantum security)
 * - Deterministic signatures (no random nonce required)
 * - Fast verification (important for token validation)
 * 
 * Implementation Strategy:
 * - Uses reflection to access Java 26+ ML-DSA APIs
 * - Compiles with Java 17 (no direct Java 26 dependencies)
 * - Runtime detection of ML-DSA support
 * - Graceful fallback if ML-DSA unavailable
 * 
 * Thread Safety: All methods are thread-safe
 * 
 * @since Liberty 26.0.0.1
 */
public class LTPAPQCSignature {
    private static final TraceComponent tc = Tr.register(LTPAPQCSignature.class);

    // ML-DSA availability flag (set at class load time)
    private static final boolean ML_DSA_AVAILABLE;

    static {
        boolean available = false;
        try {
            // Check if ML-DSA KeyPairGenerator is available (Java 26+)
            // Note: MLDSAParameterSpec class doesn't exist in Java 26 build 35,
            // but the ML-DSA algorithms are available via KeyPairGenerator
            KeyPairGenerator.getInstance("ML-DSA");
            available = true;
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "ML-DSA support detected (Java 26+)");
            }
        } catch (NoSuchAlgorithmException e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "ML-DSA not available: " + e.getMessage());
            }
        }
        ML_DSA_AVAILABLE = available;
    }

    /**
     * Check if ML-DSA is supported on this JVM.
     * 
     * @return true if ML-DSA is available (Java 26+)
     */
    public static boolean isMLDSASupported() {
        return ML_DSA_AVAILABLE;
    }

    /**
     * Generate ML-DSA key pair.
     * 
     * @param algorithm ML-DSA algorithm type
     * @return key pair containing ML-DSA public and private keys
     * @throws NoSuchAlgorithmException if ML-DSA is not available
     * @throws InvalidKeySpecException if key generation fails
     */
    public static KeyPair generateMLDSAKeyPair(MLDSAAlgorithmType algorithm) 
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        
        if (!ML_DSA_AVAILABLE) {
            throw new NoSuchAlgorithmException("ML-DSA not available (requires Java 26+)");
        }

        try {
            // Get ML-DSA key pair generator
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("ML-DSA");
            
            // Create parameter spec using reflection
            Class<?> paramSpecClass = Class.forName("java.security.spec.MLDSAParameterSpec");
            Object paramSpec = null;
            
            // Get the appropriate parameter spec constant
            switch (algorithm) {
                case ML_DSA_44:
                    paramSpec = paramSpecClass.getField("ML_DSA_44").get(null);
                    break;
                case ML_DSA_65:
                    paramSpec = paramSpecClass.getField("ML_DSA_65").get(null);
                    break;
                case ML_DSA_87:
                    paramSpec = paramSpecClass.getField("ML_DSA_87").get(null);
                    break;
                default:
                    throw new InvalidKeySpecException("Unknown ML-DSA algorithm: " + algorithm);
            }
            
            // Initialize key generator with parameter spec
            keyGen.initialize((java.security.spec.AlgorithmParameterSpec) paramSpec);
            
            // Generate key pair
            KeyPair keyPair = keyGen.generateKeyPair();
            
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Generated ML-DSA key pair: " + algorithm.getAlgorithmName() +
                        ", public=" + keyPair.getPublic().getEncoded().length + " bytes" +
                        ", private=" + keyPair.getPrivate().getEncoded().length + " bytes");
            }
            
            return keyPair;
            
        } catch (Exception e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "ML-DSA key generation failed", e);
            }
            throw new InvalidKeySpecException("Failed to generate ML-DSA key pair: " + e.getMessage(), e);
        }
    }

    /**
     * Sign data using ML-DSA private key.
     * 
     * @param data data to sign
     * @param privateKey ML-DSA private key
     * @param algorithm ML-DSA algorithm type
     * @return ML-DSA signature bytes
     * @throws SignatureException if signing fails
     * @throws InvalidKeyException if key is invalid
     */
    public static byte[] sign(@Sensitive byte[] data, 
                             @Sensitive PrivateKey privateKey,
                             MLDSAAlgorithmType algorithm) 
            throws SignatureException, InvalidKeyException {
        
        if (!ML_DSA_AVAILABLE) {
            throw new SignatureException("ML-DSA not available (requires Java 26+)");
        }

        if (data == null || data.length == 0) {
            throw new SignatureException("Data to sign cannot be null or empty");
        }

        if (privateKey == null) {
            throw new InvalidKeyException("Private key cannot be null");
        }

        try {
            // Create ML-DSA signature instance
            Signature signature = Signature.getInstance("ML-DSA");
            
            // Initialize for signing
            signature.initSign(privateKey);
            
            // Update with data
            signature.update(data);
            
            // Generate signature
            byte[] signatureBytes = signature.sign();
            
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Generated ML-DSA signature: " + algorithm.getAlgorithmName() +
                        ", data=" + data.length + " bytes" +
                        ", signature=" + signatureBytes.length + " bytes");
            }
            
            return signatureBytes;
            
        } catch (NoSuchAlgorithmException e) {
            throw new SignatureException("ML-DSA algorithm not available", e);
        }
    }

    /**
     * Verify ML-DSA signature.
     * 
     * @param data original data that was signed
     * @param signatureBytes ML-DSA signature to verify
     * @param publicKey ML-DSA public key
     * @param algorithm ML-DSA algorithm type
     * @return true if signature is valid
     * @throws SignatureException if verification fails
     * @throws InvalidKeyException if key is invalid
     */
    public static boolean verify(byte[] data,
                                byte[] signatureBytes,
                                PublicKey publicKey,
                                MLDSAAlgorithmType algorithm)
            throws SignatureException, InvalidKeyException {
        
        if (!ML_DSA_AVAILABLE) {
            throw new SignatureException("ML-DSA not available (requires Java 26+)");
        }

        if (data == null || data.length == 0) {
            throw new SignatureException("Data to verify cannot be null or empty");
        }

        if (signatureBytes == null || signatureBytes.length == 0) {
            throw new SignatureException("Signature cannot be null or empty");
        }

        if (publicKey == null) {
            throw new InvalidKeyException("Public key cannot be null");
        }

        try {
            // Create ML-DSA signature instance
            Signature signature = Signature.getInstance("ML-DSA");
            
            // Initialize for verification
            signature.initVerify(publicKey);
            
            // Update with data
            signature.update(data);
            
            // Verify signature
            boolean valid = signature.verify(signatureBytes);
            
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Verified ML-DSA signature: " + algorithm.getAlgorithmName() +
                        ", valid=" + valid);
            }
            
            return valid;
            
        } catch (NoSuchAlgorithmException e) {
            throw new SignatureException("ML-DSA algorithm not available", e);
        }
    }

    /**
     * Reconstruct ML-DSA private key from raw bytes.
     * 
     * @param keyBytes private key bytes (PKCS#8 encoded)
     * @param algorithm ML-DSA algorithm type
     * @return ML-DSA private key
     * @throws InvalidKeySpecException if key reconstruction fails
     */
    public static PrivateKey reconstructPrivateKey(@Sensitive byte[] keyBytes,
                                                   MLDSAAlgorithmType algorithm)
            throws InvalidKeySpecException {
        
        if (!ML_DSA_AVAILABLE) {
            throw new InvalidKeySpecException("ML-DSA not available (requires Java 26+)");
        }

        if (keyBytes == null || keyBytes.length == 0) {
            throw new InvalidKeySpecException("Key bytes cannot be null or empty");
        }

        try {
            KeyFactory keyFactory = KeyFactory.getInstance("ML-DSA");
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
            PrivateKey privateKey = keyFactory.generatePrivate(keySpec);
            
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Reconstructed ML-DSA private key: " + algorithm.getAlgorithmName());
            }
            
            return privateKey;
            
        } catch (NoSuchAlgorithmException e) {
            throw new InvalidKeySpecException("ML-DSA algorithm not available", e);
        }
    }

    /**
     * Reconstruct ML-DSA public key from raw bytes.
     * 
     * @param keyBytes public key bytes (X.509 encoded)
     * @param algorithm ML-DSA algorithm type
     * @return ML-DSA public key
     * @throws InvalidKeySpecException if key reconstruction fails
     */
    public static PublicKey reconstructPublicKey(byte[] keyBytes,
                                                MLDSAAlgorithmType algorithm)
            throws InvalidKeySpecException {
        
        if (!ML_DSA_AVAILABLE) {
            throw new InvalidKeySpecException("ML-DSA not available (requires Java 26+)");
        }

        if (keyBytes == null || keyBytes.length == 0) {
            throw new InvalidKeySpecException("Key bytes cannot be null or empty");
        }

        try {
            KeyFactory keyFactory = KeyFactory.getInstance("ML-DSA");
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
            PublicKey publicKey = keyFactory.generatePublic(keySpec);
            
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Reconstructed ML-DSA public key: " + algorithm.getAlgorithmName());
            }
            
            return publicKey;
            
        } catch (NoSuchAlgorithmException e) {
            throw new InvalidKeySpecException("ML-DSA algorithm not available", e);
        }
    }

    /**
     * Validate ML-DSA key sizes match expected values.
     * 
     * @param publicKeyBytes public key bytes
     * @param privateKeyBytes private key bytes
     * @param algorithm ML-DSA algorithm type
     * @return true if key sizes are valid
     */
    public static boolean validateKeySizes(byte[] publicKeyBytes,
                                          @Sensitive byte[] privateKeyBytes,
                                          MLDSAAlgorithmType algorithm) {
        
        if (publicKeyBytes == null || privateKeyBytes == null) {
            return false;
        }

        // Note: Encoded keys include ASN.1 overhead, so we check minimum sizes
        int minPublicSize = algorithm.getPublicKeySize();
        int minPrivateSize = algorithm.getPrivateKeySize();

        boolean valid = publicKeyBytes.length >= minPublicSize &&
                       privateKeyBytes.length >= minPrivateSize;

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "ML-DSA key size validation: " + algorithm.getAlgorithmName() +
                    ", public=" + publicKeyBytes.length + " (min " + minPublicSize + ")" +
                    ", private=" + privateKeyBytes.length + " (min " + minPrivateSize + ")" +
                    ", valid=" + valid);
        }

        return valid;
    }

    /**
     * Get ML-DSA provider information.
     * 
     * @return provider name and version, or null if ML-DSA not available
     */
    public static String getProviderInfo() {
        if (!ML_DSA_AVAILABLE) {
            return null;
        }

        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("ML-DSA");
            return keyGen.getProvider().getName() + " " + keyGen.getProvider().getVersion();
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }
}

// Made with Bob
