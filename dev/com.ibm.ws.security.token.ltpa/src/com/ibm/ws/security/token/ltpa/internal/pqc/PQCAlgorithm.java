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
package com.ibm.ws.security.token.ltpa.internal.pqc;

/**
 * Enumeration of supported Post-Quantum Cryptography (PQC) algorithms for LTPA tokens.
 * 
 * <p>This enum defines the NIST-standardized ML-DSA (Module-Lattice-Based Digital Signature Algorithm)
 * variants supported by Liberty for PQC-LTPA token signatures. These algorithms are defined in
 * NIST FIPS 204 and provide quantum-resistant digital signatures.</p>
 * 
 * <h3>Algorithm Selection Guidelines:</h3>
 * <ul>
 *   <li><b>ML-DSA-65</b>: Recommended for most use cases. Provides NIST security level 3 
 *       (equivalent to AES-192) with balanced performance and signature size.</li>
 *   <li><b>ML-DSA-87</b>: For high-security environments requiring NIST security level 5 
 *       (equivalent to AES-256). Larger signatures and slower performance.</li>
 * </ul>
 * 
 * <h3>FIPS 140-3 Compliance:</h3>
 * <p>Both algorithms are FIPS 140-3 approved when used with a validated cryptographic module
 * (e.g., OpenJCEPlusFIPS on IBM Semeru Runtime with FIPS mode enabled).</p>
 * 
 * @see <a href="https://csrc.nist.gov/pubs/fips/204/final">NIST FIPS 204: ML-DSA Standard</a>
 * @since 1.0
 */
public enum PQCAlgorithm {
    
    /**
     * ML-DSA-65 (formerly Dilithium3) - NIST Security Level 3.
     * 
     * <p><b>Security Level:</b> NIST Level 3 (equivalent to AES-192, SHA-384)<br>
     * <b>Public Key Size:</b> 1,952 bytes<br>
     * <b>Private Key Size:</b> 4,000 bytes<br>
     * <b>Signature Size:</b> 3,293 bytes<br>
     * <b>Performance:</b> Balanced - suitable for most production environments</p>
     * 
     * <p>This is the <b>recommended default</b> for PQC-LTPA tokens. It provides strong
     * quantum resistance with reasonable performance characteristics and signature sizes.</p>
     * 
     * <h4>Use Cases:</h4>
     * <ul>
     *   <li>Standard enterprise SSO deployments</li>
     *   <li>Web applications with moderate security requirements</li>
     *   <li>Environments where signature size is a consideration</li>
     * </ul>
     */
    ML_DSA_65(
        "ML-DSA-65",           // NIST standard name
        "Dilithium3",          // BouncyCastle legacy name
        3,                     // NIST security level
        1952,                  // Public key size in bytes
        4000,                  // Private key size in bytes
        3293                   // Signature size in bytes
    ),
    
    /**
     * ML-DSA-87 (formerly Dilithium5) - NIST Security Level 5.
     * 
     * <p><b>Security Level:</b> NIST Level 5 (equivalent to AES-256, SHA-512)<br>
     * <b>Public Key Size:</b> 2,592 bytes<br>
     * <b>Private Key Size:</b> 4,864 bytes<br>
     * <b>Signature Size:</b> 4,595 bytes<br>
     * <b>Performance:</b> Slower than ML-DSA-65, larger signatures</p>
     * 
     * <p>This algorithm provides the <b>highest security level</b> for quantum resistance,
     * suitable for high-security environments with stringent security requirements.</p>
     * 
     * <h4>Use Cases:</h4>
     * <ul>
     *   <li>High-security government or financial systems</li>
     *   <li>Long-term data protection (10+ years)</li>
     *   <li>Environments where maximum security is prioritized over performance</li>
     *   <li>Compliance requirements mandating NIST Level 5</li>
     * </ul>
     */
    ML_DSA_87(
        "ML-DSA-87",           // NIST standard name
        "Dilithium5",          // BouncyCastle legacy name
        5,                     // NIST security level
        2592,                  // Public key size in bytes
        4864,                  // Private key size in bytes
        4595                   // Signature size in bytes
    );
    
    private final String nistName;
    private final String bouncyCastleName;
    private final int securityLevel;
    private final int publicKeySize;
    private final int privateKeySize;
    private final int signatureSize;
    
    /**
     * Constructs a PQCAlgorithm enum constant.
     * 
     * @param nistName NIST standard algorithm name (e.g., "ML-DSA-65")
     * @param bouncyCastleName BouncyCastle provider algorithm name (e.g., "Dilithium3")
     * @param securityLevel NIST security level (3 or 5)
     * @param publicKeySize Public key size in bytes
     * @param privateKeySize Private key size in bytes
     * @param signatureSize Signature size in bytes
     */
    private PQCAlgorithm(String nistName, String bouncyCastleName, int securityLevel,
                         int publicKeySize, int privateKeySize, int signatureSize) {
        this.nistName = nistName;
        this.bouncyCastleName = bouncyCastleName;
        this.securityLevel = securityLevel;
        this.publicKeySize = publicKeySize;
        this.privateKeySize = privateKeySize;
        this.signatureSize = signatureSize;
    }
    
    /**
     * Gets the NIST standard algorithm name.
     * 
     * <p>This is the official name defined in NIST FIPS 204 and used by JEP 497
     * (Java 21+) native PQC support.</p>
     * 
     * @return NIST algorithm name (e.g., "ML-DSA-65", "ML-DSA-87")
     */
    public String getNistName() {
        return nistName;
    }
    
    /**
     * Gets the BouncyCastle provider algorithm name.
     * 
     * <p>This is the legacy name used by BouncyCastle PQC provider for Java 8-17
     * compatibility. BouncyCastle uses pre-standardization names (Dilithium3, Dilithium5)
     * that map to the NIST standard names.</p>
     * 
     * @return BouncyCastle algorithm name (e.g., "Dilithium3", "Dilithium5")
     */
    public String getBouncyCastleName() {
        return bouncyCastleName;
    }
    
    /**
     * Gets the NIST security level.
     * 
     * <p>NIST defines five security levels for post-quantum algorithms:</p>
     * <ul>
     *   <li><b>Level 1:</b> Equivalent to AES-128 (not used for ML-DSA)</li>
     *   <li><b>Level 2:</b> Equivalent to SHA-256 (not used for ML-DSA)</li>
     *   <li><b>Level 3:</b> Equivalent to AES-192 (ML-DSA-65)</li>
     *   <li><b>Level 4:</b> Equivalent to SHA-384 (not used for ML-DSA)</li>
     *   <li><b>Level 5:</b> Equivalent to AES-256 (ML-DSA-87)</li>
     * </ul>
     * 
     * @return NIST security level (3 or 5)
     */
    public int getSecurityLevel() {
        return securityLevel;
    }
    
    /**
     * Gets the public key size in bytes.
     * 
     * <p>This is the encoded size of the public key when serialized for storage
     * or transmission. Public keys are included in LTPA tokens for signature verification.</p>
     * 
     * @return public key size in bytes
     */
    public int getPublicKeySize() {
        return publicKeySize;
    }
    
    /**
     * Gets the private key size in bytes.
     * 
     * <p>This is the encoded size of the private key when serialized for storage.
     * Private keys are stored securely in LTPA key files and never transmitted.</p>
     * 
     * @return private key size in bytes
     */
    public int getPrivateKeySize() {
        return privateKeySize;
    }
    
    /**
     * Gets the signature size in bytes.
     * 
     * <p>This is the size of the digital signature produced by this algorithm.
     * PQC signatures are significantly larger than classical signatures (e.g., RSA-2048
     * produces 256-byte signatures, while ML-DSA-65 produces 3,293-byte signatures).</p>
     * 
     * <p><b>Impact on LTPA Tokens:</b> Larger signatures increase token size, which may
     * affect HTTP cookie size limits and network bandwidth. Consider using HTTP headers
     * or session storage for very large tokens.</p>
     * 
     * @return signature size in bytes
     */
    public int getSignatureSize() {
        return signatureSize;
    }
    
    /**
     * Parses an algorithm name string to the corresponding PQCAlgorithm enum.
     * 
     * <p>This method accepts both NIST standard names and BouncyCastle legacy names
     * for backward compatibility and provider abstraction.</p>
     * 
     * <h4>Supported Names:</h4>
     * <ul>
     *   <li>"ML-DSA-65" or "Dilithium3" → {@link #ML_DSA_65}</li>
     *   <li>"ML-DSA-87" or "Dilithium5" → {@link #ML_DSA_87}</li>
     * </ul>
     * 
     * @param algorithmName algorithm name (case-sensitive)
     * @return corresponding PQCAlgorithm enum constant
     * @throws IllegalArgumentException if the algorithm name is not recognized
     */
    public static PQCAlgorithm fromString(String algorithmName) {
        if (algorithmName == null) {
            throw new IllegalArgumentException("Algorithm name cannot be null");
        }
        
        for (PQCAlgorithm algorithm : values()) {
            if (algorithm.nistName.equals(algorithmName) || 
                algorithm.bouncyCastleName.equals(algorithmName)) {
                return algorithm;
            }
        }
        
        throw new IllegalArgumentException("Unsupported PQC algorithm: " + algorithmName + 
                                         ". Supported algorithms: ML-DSA-65, ML-DSA-87, Dilithium3, Dilithium5");
    }
    
    /**
     * Returns the NIST standard algorithm name.
     * 
     * @return NIST algorithm name
     */
    @Override
    public String toString() {
        return nistName;
    }
}

// Made with Bob
