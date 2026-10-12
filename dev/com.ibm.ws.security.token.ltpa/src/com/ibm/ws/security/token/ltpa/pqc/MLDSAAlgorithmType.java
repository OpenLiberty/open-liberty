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

/**
 * ML-DSA (Module-Lattice-Based Digital Signature Algorithm) parameter sets.
 * 
 * ML-DSA is the NIST-standardized post-quantum digital signature algorithm
 * based on the CRYSTALS-Dilithium submission. It provides quantum-resistant
 * signatures for LTPA v3 tokens.
 * 
 * Standard: FIPS 204 (Module-Lattice-Based Digital Signature Standard)
 * Provider: SunJCE (Java 26+)
 * 
 * Security Levels (NIST):
 * - ML-DSA-44: Level 1 (128-bit quantum security, equivalent to AES-128)
 * - ML-DSA-65: Level 3 (192-bit quantum security, equivalent to AES-192)
 * - ML-DSA-87: Level 5 (256-bit quantum security, equivalent to AES-256)
 * 
 * Key Sizes:
 * - ML-DSA-44: Public 1312 bytes, Private 2560 bytes, Signature 2420 bytes
 * - ML-DSA-65: Public 1952 bytes, Private 4032 bytes, Signature 3309 bytes
 * - ML-DSA-87: Public 2592 bytes, Private 4896 bytes, Signature 4627 bytes
 * 
 * Performance Characteristics:
 * - ML-DSA-44: Fastest, smallest signatures (recommended for most use cases)
 * - ML-DSA-65: Balanced security/performance (recommended for high security)
 * - ML-DSA-87: Highest security, largest signatures (recommended for maximum security)
 * 
 * Recommended Pairings with ML-KEM:
 * - ML-DSA-44 + ML-KEM-512 (NIST Level 1) - DEFAULT
 * - ML-DSA-65 + ML-KEM-768 (NIST Level 3)
 * - ML-DSA-87 + ML-KEM-1024 (NIST Level 5)
 * 
 * @since Liberty 26.0.0.1
 */
public enum MLDSAAlgorithmType {
    
    /**
     * ML-DSA-44 (NIST Security Level 1)
     * 
     * Quantum Security: 128-bit (equivalent to AES-128)
     * Classical Security: ~143-bit
     * 
     * Key Sizes:
     * - Public Key: 1312 bytes
     * - Private Key: 2560 bytes
     * - Signature: 2420 bytes
     * 
     * Performance: Fastest signing and verification
     * Use Case: Standard security applications, high-throughput systems
     * 
     * Recommended for: Most LTPA deployments requiring quantum resistance
     */
    ML_DSA_44("ML-DSA-44", 1, 1312, 2560, 2420),
    
    /**
     * ML-DSA-65 (NIST Security Level 3) - DEFAULT
     * 
     * Quantum Security: 192-bit (equivalent to AES-192)
     * Classical Security: ~207-bit
     * 
     * Key Sizes:
     * - Public Key: 1952 bytes
     * - Private Key: 4032 bytes
     * - Signature: 3309 bytes
     * 
     * Performance: Balanced security and performance
     * Use Case: High-security applications, financial systems
     * 
     * Recommended for: Enterprise LTPA deployments with elevated security requirements
     */
    ML_DSA_65("ML-DSA-65", 3, 1952, 4032, 3309),
    
    /**
     * ML-DSA-87 (NIST Security Level 5)
     * 
     * Quantum Security: 256-bit (equivalent to AES-256)
     * Classical Security: ~272-bit
     * 
     * Key Sizes:
     * - Public Key: 2592 bytes
     * - Private Key: 4896 bytes
     * - Signature: 4627 bytes
     * 
     * Performance: Slower signing and verification, largest signatures
     * Use Case: Maximum security applications, government/defense systems
     * 
     * Recommended for: Critical infrastructure, classified systems
     */
    ML_DSA_87("ML-DSA-87", 5, 2592, 4896, 4627);

    private final String algorithmName;
    private final int securityLevel;
    private final int publicKeySize;
    private final int privateKeySize;
    private final int signatureSize;

    /**
     * Constructor for ML-DSA algorithm types.
     * 
     * @param algorithmName FIPS 204 algorithm name
     * @param securityLevel NIST security level (1, 3, or 5)
     * @param publicKeySize public key size in bytes
     * @param privateKeySize private key size in bytes
     * @param signatureSize signature size in bytes
     */
    MLDSAAlgorithmType(String algorithmName, int securityLevel, 
                       int publicKeySize, int privateKeySize, int signatureSize) {
        this.algorithmName = algorithmName;
        this.securityLevel = securityLevel;
        this.publicKeySize = publicKeySize;
        this.privateKeySize = privateKeySize;
        this.signatureSize = signatureSize;
    }

    /**
     * Get the FIPS 204 algorithm name.
     * 
     * @return algorithm name (e.g., "ML-DSA-65")
     */
    public String getAlgorithmName() {
        return algorithmName;
    }

    /**
     * Get the NIST security level.
     * 
     * @return security level (1, 3, or 5)
     */
    public int getSecurityLevel() {
        return securityLevel;
    }

    /**
     * Get the public key size in bytes.
     * 
     * @return public key size
     */
    public int getPublicKeySize() {
        return publicKeySize;
    }

    /**
     * Get the private key size in bytes.
     * 
     * @return private key size
     */
    public int getPrivateKeySize() {
        return privateKeySize;
    }

    /**
     * Get the signature size in bytes.
     * 
     * @return signature size
     */
    public int getSignatureSize() {
        return signatureSize;
    }

    /**
     * Get the recommended ML-KEM algorithm for this ML-DSA level.
     * 
     * @return corresponding ML-KEM algorithm type
     */
    public MLKEMAlgorithmType getRecommendedMLKEM() {
        switch (this) {
            case ML_DSA_44:
                return MLKEMAlgorithmType.ML_KEM_512;
            case ML_DSA_65:
                return MLKEMAlgorithmType.ML_KEM_768;
            case ML_DSA_87:
                return MLKEMAlgorithmType.ML_KEM_1024;
            default:
                return MLKEMAlgorithmType.ML_KEM_768; // Default to Level 3
        }
    }

    /**
     * Get the default ML-DSA algorithm (Level 1).
     *
     * @return ML-DSA-44 (recommended for optimal performance and smaller tokens)
     */
    public static MLDSAAlgorithmType getDefault() {
        return ML_DSA_44;
    }

    /**
     * Parse algorithm name to enum value.
     *
     * @param algorithmName algorithm name (e.g., "ML-DSA-65")
     * @return corresponding enum value
     * @throws IllegalArgumentException if algorithm name is invalid
     */
    public static MLDSAAlgorithmType fromString(String algorithmName) {
        if (algorithmName == null || algorithmName.isEmpty()) {
            throw new IllegalArgumentException("Algorithm name cannot be null or empty");
        }

        for (MLDSAAlgorithmType type : values()) {
            if (type.algorithmName.equalsIgnoreCase(algorithmName)) {
                return type;
            }
        }

        throw new IllegalArgumentException("Unknown ML-DSA algorithm: " + algorithmName);
    }

    /**
     * Parse algorithm name to enum value (alias for fromString).
     *
     * @param algorithmName algorithm name (e.g., "ML-DSA-65")
     * @return corresponding enum value
     * @throws IllegalArgumentException if algorithm name is invalid
     */
    public static MLDSAAlgorithmType fromAlgorithmName(String algorithmName) {
        return fromString(algorithmName);
    }

    /**
     * Check if this algorithm is compatible with a given ML-KEM algorithm.
     * 
     * @param mlkemType ML-KEM algorithm type
     * @return true if security levels match
     */
    public boolean isCompatibleWith(MLKEMAlgorithmType mlkemType) {
        if (mlkemType == null) {
            return false;
        }
        return this.securityLevel == mlkemType.getSecurityLevel();
    }

    /**
     * Get quantum security strength in bits.
     * 
     * @return quantum security bits (128, 192, or 256)
     */
    public int getQuantumSecurityBits() {
        switch (this) {
            case ML_DSA_44:
                return 128;
            case ML_DSA_65:
                return 192;
            case ML_DSA_87:
                return 256;
            default:
                return 0;
        }
    }

    /**
     * Get NIST security level (alias for getSecurityLevel).
     * 
     * @return NIST security level (1, 3, or 5)
     */
    public int getNistSecurityLevel() {
        return securityLevel;
    }

    /**
     * Get ML-DSA algorithm by NIST security level.
     * 
     * @param level NIST security level (1, 3, or 5)
     * @return corresponding ML-DSA algorithm type
     * @throws IllegalArgumentException if security level is invalid
     */
    public static MLDSAAlgorithmType fromSecurityLevel(int level) {
        for (MLDSAAlgorithmType type : values()) {
            if (type.securityLevel == level) {
                return type;
            }
        }
        throw new IllegalArgumentException("No ML-DSA algorithm for security level: " + level);
    }

    /**
     * Get classical security strength in bits (approximate).
     * 
     * @return classical security bits
     */
    public int getClassicalSecurityBits() {
        switch (this) {
            case ML_DSA_44:
                return 143;
            case ML_DSA_65:
                return 207;
            case ML_DSA_87:
                return 272;
            default:
                return 0;
        }
    }

    @Override
    public String toString() {
        return algorithmName + " (Level " + securityLevel + 
               ", " + getQuantumSecurityBits() + "-bit quantum security)";
    }
}

// Made with Bob
