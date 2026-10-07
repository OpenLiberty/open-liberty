/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.token.ltpa.pqc;

/**
 * Enumeration of supported ML-KEM (Module-Lattice-Based Key Encapsulation Mechanism) 
 * algorithm types as defined in NIST FIPS 203.
 * 
 * ML-KEM provides quantum-resistant key encapsulation for secure key exchange.
 */
public enum MLKEMAlgorithmType {
    
    /**
     * ML-KEM-512: NIST Security Level 1 (equivalent to AES-128)
     * - Public Key Size: ~800 bytes
     * - Ciphertext Size: ~768 bytes
     * - Security: 128-bit quantum security
     * - Use Case: Lower security requirements, faster performance
     */
    ML_KEM_512("ML-KEM-512", 512, 800, 768, 1),
    
    /**
     * ML-KEM-768: NIST Security Level 3 (equivalent to AES-192)
     * - Public Key Size: ~1184 bytes
     * - Ciphertext Size: ~1088 bytes
     * - Security: 192-bit quantum security
     * - Use Case: Recommended for most applications (default)
     */
    ML_KEM_768("ML-KEM-768", 768, 1184, 1088, 3),
    
    /**
     * ML-KEM-1024: NIST Security Level 5 (equivalent to AES-256)
     * - Public Key Size: ~1568 bytes
     * - Ciphertext Size: ~1568 bytes
     * - Security: 256-bit quantum security
     * - Use Case: Highest security requirements, slower performance
     */
    ML_KEM_1024("ML-KEM-1024", 1024, 1568, 1568, 5);
    
    private final String algorithmName;
    private final int parameterSize;
    private final int publicKeySize;
    private final int ciphertextSize;
    private final int nistSecurityLevel;
    
    /**
     * Constructor for ML-KEM algorithm type.
     * 
     * @param algorithmName The standard algorithm name (e.g., "ML-KEM-768")
     * @param parameterSize The parameter size (512, 768, or 1024)
     * @param publicKeySize The approximate public key size in bytes
     * @param ciphertextSize The approximate ciphertext (encapsulation) size in bytes
     * @param nistSecurityLevel The NIST security level (1, 3, or 5)
     */
    private MLKEMAlgorithmType(String algorithmName, int parameterSize, int publicKeySize, 
                               int ciphertextSize, int nistSecurityLevel) {
        this.algorithmName = algorithmName;
        this.parameterSize = parameterSize;
        this.publicKeySize = publicKeySize;
        this.ciphertextSize = ciphertextSize;
        this.nistSecurityLevel = nistSecurityLevel;
    }
    
    /**
     * Get the standard algorithm name for use with Java Cryptography Architecture.
     * 
     * @return The algorithm name (e.g., "ML-KEM-768")
     */
    public String getAlgorithmName() {
        return algorithmName;
    }
    
    /**
     * Get the parameter size.
     * 
     * @return The parameter size (512, 768, or 1024)
     */
    public int getParameterSize() {
        return parameterSize;
    }
    
    /**
     * Get the approximate public key size in bytes.
     * 
     * @return The public key size
     */
    public int getPublicKeySize() {
        return publicKeySize;
    }
    
    /**
     * Get the approximate ciphertext (encapsulation) size in bytes.
     * 
     * @return The ciphertext size
     */
    public int getCiphertextSize() {
        return ciphertextSize;
    }
    
    /**
     * Get the NIST security level.
     *
     * @return The NIST security level (1, 3, or 5)
     */
    public int getNistSecurityLevel() {
        return nistSecurityLevel;
    }
    
    /**
     * Get the security level (alias for getNistSecurityLevel).
     *
     * @return The NIST security level (1, 3, or 5)
     */
    public int getSecurityLevel() {
        return nistSecurityLevel;
    }
    
    /**
     * Get the approximate private key size in bytes.
     * ML-KEM private keys are approximately 2x the public key size.
     *
     * @return The private key size
     */
    public int getPrivateKeySize() {
        // ML-KEM private keys contain both public and private components
        // Approximate sizes: ML-KEM-512: ~1632, ML-KEM-768: ~2400, ML-KEM-1024: ~3168
        return publicKeySize * 2;
    }
    
    /**
     * Get the ML-KEM algorithm type from a string representation.
     * 
     * @param value The string value (e.g., "ML-KEM-768", "ML_KEM_768", or "768")
     * @return The corresponding MLKEMAlgorithmType
     * @throws IllegalArgumentException if the value is not recognized
     */
    public static MLKEMAlgorithmType fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return ML_KEM_768; // Default
        }
        
        String normalized = value.trim().toUpperCase().replace("-", "_");
        
        // Try exact enum name match
        try {
            return valueOf(normalized);
        } catch (IllegalArgumentException e) {
            // Continue to other matching strategies
        }
        
        // Try parameter size match
        if (normalized.contains("512")) {
            return ML_KEM_512;
        } else if (normalized.contains("768")) {
            return ML_KEM_768;
        } else if (normalized.contains("1024")) {
            return ML_KEM_1024;
        }
        
        throw new IllegalArgumentException("Unknown ML-KEM algorithm type: " + value + 
                                         ". Valid values are: ML-KEM-512, ML-KEM-768, ML-KEM-1024");
    }
    
    /**
     * Get the default ML-KEM algorithm type (ML-KEM-512).
     *
     * @return The default algorithm type
     */
    public static MLKEMAlgorithmType getDefault() {
        return ML_KEM_512;
    }
    
    @Override
    public String toString() {
        return algorithmName;
    }
}

// Made with Bob
