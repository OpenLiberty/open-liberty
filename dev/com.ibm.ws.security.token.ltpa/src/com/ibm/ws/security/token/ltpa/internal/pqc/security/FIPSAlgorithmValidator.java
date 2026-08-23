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
package com.ibm.ws.security.token.ltpa.internal.pqc.security;

import java.security.Key;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;

/**
 * Validates cryptographic algorithms for FIPS 140-3 compliance.
 * 
 * <p>This class ensures that only FIPS-approved algorithms are used when
 * FIPS mode is enabled. FIPS 140-3 specifies approved algorithms for:
 * <ul>
 *   <li>Digital signatures (RSA, ECDSA, ML-DSA)</li>
 *   <li>Hash functions (SHA-2, SHA-3)</li>
 *   <li>Symmetric encryption (AES)</li>
 *   <li>Key agreement (ECDH, ML-KEM)</li>
 *   <li>Random number generation (DRBG)</li>
 * </ul>
 * 
 * <p><b>FIPS-Approved Algorithms (NIST SP 800-131A Rev. 2):</b>
 * 
 * <p><b>Digital Signatures:</b>
 * <ul>
 *   <li><b>RSA:</b> 2048, 3072, 4096 bits (FIPS 186-4)</li>
 *   <li><b>ECDSA:</b> P-256, P-384, P-521 curves (FIPS 186-4)</li>
 *   <li><b>ML-DSA:</b> ML-DSA-44, ML-DSA-65, ML-DSA-87 (FIPS 204)</li>
 * </ul>
 * 
 * <p><b>Hash Functions:</b>
 * <ul>
 *   <li><b>SHA-2:</b> SHA-256, SHA-384, SHA-512 (FIPS 180-4)</li>
 *   <li><b>SHA-3:</b> SHA3-256, SHA3-384, SHA3-512 (FIPS 202)</li>
 * </ul>
 * 
 * <p><b>Symmetric Encryption:</b>
 * <ul>
 *   <li><b>AES:</b> 128, 192, 256 bits (FIPS 197)</li>
 *   <li><b>Modes:</b> CBC, GCM, CCM (NIST SP 800-38A/D)</li>
 * </ul>
 * 
 * <p><b>Prohibited Algorithms in FIPS Mode:</b>
 * <ul>
 *   <li><b>MD5:</b> Cryptographically broken</li>
 *   <li><b>SHA-1:</b> Deprecated for signatures (collision attacks)</li>
 *   <li><b>DES/3DES:</b> Insufficient key length</li>
 *   <li><b>RC4:</b> Stream cipher vulnerabilities</li>
 *   <li><b>RSA < 2048:</b> Insufficient security strength</li>
 * </ul>
 * 
 * <p><b>Post-Quantum Algorithms (FIPS 203/204/205):</b>
 * <ul>
 *   <li><b>ML-KEM:</b> ML-KEM-512, ML-KEM-768, ML-KEM-1024 (FIPS 203)</li>
 *   <li><b>ML-DSA:</b> ML-DSA-44, ML-DSA-65, ML-DSA-87 (FIPS 204)</li>
 *   <li><b>SLH-DSA:</b> Various parameter sets (FIPS 205)</li>
 * </ul>
 * 
 * <p><b>Usage Example:</b>
 * <pre>
 * // Validate algorithm before use
 * if (FIPSAlgorithmValidator.isAlgorithmApproved("ML-DSA-65")) {
 *     // Safe to use in FIPS mode
 *     signature = Signature.getInstance("ML-DSA-65");
 * } else {
 *     throw new SecurityException("Algorithm not FIPS-approved");
 * }
 * 
 * // Validate key strength
 * if (!FIPSAlgorithmValidator.isKeySizeApproved("RSA", 2048)) {
 *     throw new SecurityException("Key size too small for FIPS mode");
 * }
 * </pre>
 * 
 * <p><b>Thread Safety:</b> All methods are thread-safe and stateless.
 * 
 * @author IBM Corporation
 * @version 1.0
 * @since 2026
 */
public class FIPSAlgorithmValidator {
    
    private static final TraceComponent tc = Tr.register(FIPSAlgorithmValidator.class);
    
    /**
     * FIPS-approved digital signature algorithms.
     */
    private static final Set<String> APPROVED_SIGNATURE_ALGORITHMS = Collections.unmodifiableSet(
        new HashSet<>(Arrays.asList(
            // RSA signatures
            "RSA",
            "SHA256withRSA",
            "SHA384withRSA",
            "SHA512withRSA",
            
            // ECDSA signatures
            "ECDSA",
            "SHA256withECDSA",
            "SHA384withECDSA",
            "SHA512withECDSA",
            
            // Post-Quantum signatures (FIPS 204)
            "ML-DSA",
            "ML-DSA-44",
            "ML-DSA-65",
            "ML-DSA-87",
            "MLDSA44",
            "MLDSA65",
            "MLDSA87",
            
            // SLH-DSA (FIPS 205)
            "SLH-DSA",
            "SLH-DSA-SHA2-128s",
            "SLH-DSA-SHA2-128f",
            "SLH-DSA-SHA2-192s",
            "SLH-DSA-SHA2-192f",
            "SLH-DSA-SHA2-256s",
            "SLH-DSA-SHA2-256f",
            "SLH-DSA-SHAKE-128s",
            "SLH-DSA-SHAKE-128f",
            "SLH-DSA-SHAKE-192s",
            "SLH-DSA-SHAKE-192f",
            "SLH-DSA-SHAKE-256s",
            "SLH-DSA-SHAKE-256f"
        ))
    );
    
    /**
     * FIPS-approved hash algorithms.
     */
    private static final Set<String> APPROVED_HASH_ALGORITHMS = Collections.unmodifiableSet(
        new HashSet<>(Arrays.asList(
            // SHA-2 family (FIPS 180-4)
            "SHA-256",
            "SHA-384",
            "SHA-512",
            "SHA-512/224",
            "SHA-512/256",
            
            // SHA-3 family (FIPS 202)
            "SHA3-256",
            "SHA3-384",
            "SHA3-512"
        ))
    );
    
    /**
     * FIPS-approved symmetric encryption algorithms.
     */
    private static final Set<String> APPROVED_CIPHER_ALGORITHMS = Collections.unmodifiableSet(
        new HashSet<>(Arrays.asList(
            // AES (FIPS 197)
            "AES",
            "AES/CBC/PKCS5Padding",
            "AES/CBC/NoPadding",
            "AES/GCM/NoPadding",
            "AES/CCM/NoPadding"
        ))
    );
    
    /**
     * FIPS-approved key agreement algorithms.
     */
    private static final Set<String> APPROVED_KEY_AGREEMENT_ALGORITHMS = Collections.unmodifiableSet(
        new HashSet<>(Arrays.asList(
            // ECDH
            "ECDH",
            
            // Post-Quantum KEM (FIPS 203)
            "ML-KEM",
            "ML-KEM-512",
            "ML-KEM-768",
            "ML-KEM-1024",
            "MLKEM512",
            "MLKEM768",
            "MLKEM1024"
        ))
    );
    
    /**
     * Prohibited algorithms in FIPS mode.
     */
    private static final Set<String> PROHIBITED_ALGORITHMS = Collections.unmodifiableSet(
        new HashSet<>(Arrays.asList(
            // Broken hash functions
            "MD5",
            "SHA-1",
            "SHA1",
            
            // Weak ciphers
            "DES",
            "3DES",
            "DESede",
            "RC4",
            "RC2",
            "Blowfish",
            
            // Weak signatures
            "MD5withRSA",
            "SHA1withRSA",
            "SHA1withECDSA"
        ))
    );
    
    /**
     * Minimum key sizes for FIPS-approved algorithms (in bits).
     */
    private static final int MIN_RSA_KEY_SIZE = 2048;
    private static final int MIN_ECDSA_KEY_SIZE = 256;
    private static final int MIN_AES_KEY_SIZE = 128;
    
    /**
     * Private constructor to prevent instantiation.
     */
    private FIPSAlgorithmValidator() {
        // Utility class - no instances
    }
    
    /**
     * Checks if an algorithm is FIPS-approved.
     * 
     * @param algorithm the algorithm name to check
     * @return true if the algorithm is FIPS-approved, false otherwise
     */
    @Trivial
    public static boolean isAlgorithmApproved(String algorithm) {
        if (algorithm == null || algorithm.isEmpty()) {
            return false;
        }
        
        // Normalize algorithm name (remove spaces, convert to uppercase)
        String normalized = algorithm.trim().toUpperCase().replace(" ", "");
        
        // Check if explicitly prohibited
        if (isAlgorithmProhibited(normalized)) {
            return false;
        }
        
        // Check against approved lists
        return APPROVED_SIGNATURE_ALGORITHMS.contains(normalized) ||
               APPROVED_HASH_ALGORITHMS.contains(normalized) ||
               APPROVED_CIPHER_ALGORITHMS.contains(normalized) ||
               APPROVED_KEY_AGREEMENT_ALGORITHMS.contains(normalized);
    }
    
    /**
     * Checks if an algorithm is explicitly prohibited in FIPS mode.
     * 
     * @param algorithm the algorithm name to check
     * @return true if the algorithm is prohibited, false otherwise
     */
    @Trivial
    public static boolean isAlgorithmProhibited(String algorithm) {
        if (algorithm == null || algorithm.isEmpty()) {
            return false;
        }
        
        String normalized = algorithm.trim().toUpperCase().replace(" ", "");
        return PROHIBITED_ALGORITHMS.contains(normalized);
    }
    
    /**
     * Validates that a key size meets FIPS requirements.
     * 
     * @param algorithm the algorithm name
     * @param keySize the key size in bits
     * @return true if the key size is FIPS-approved, false otherwise
     */
    @Trivial
    public static boolean isKeySizeApproved(String algorithm, int keySize) {
        if (algorithm == null || algorithm.isEmpty()) {
            return false;
        }
        
        String normalized = algorithm.trim().toUpperCase();
        
        if (normalized.contains("RSA")) {
            return keySize >= MIN_RSA_KEY_SIZE;
        } else if (normalized.contains("ECDSA") || normalized.contains("EC")) {
            return keySize >= MIN_ECDSA_KEY_SIZE;
        } else if (normalized.contains("AES")) {
            return keySize >= MIN_AES_KEY_SIZE && (keySize == 128 || keySize == 192 || keySize == 256);
        } else if (normalized.contains("ML-DSA") || normalized.contains("MLDSA")) {
            // ML-DSA has fixed key sizes per parameter set
            return true;
        } else if (normalized.contains("ML-KEM") || normalized.contains("MLKEM")) {
            // ML-KEM has fixed key sizes per parameter set
            return true;
        }
        
        // Unknown algorithm - be conservative
        return false;
    }
    
    /**
     * Validates a cryptographic key for FIPS compliance.
     * 
     * @param key the key to validate
     * @return true if the key is FIPS-compliant, false otherwise
     */
    public static boolean isKeyApproved(Key key) {
        if (key == null) {
            return false;
        }
        
        String algorithm = key.getAlgorithm();
        if (!isAlgorithmApproved(algorithm)) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Key algorithm not FIPS-approved: {0}", algorithm);
            }
            return false;
        }
        
        // Check key size if available
        if (key instanceof PublicKey || key instanceof PrivateKey) {
            byte[] encoded = key.getEncoded();
            if (encoded != null) {
                int keySize = encoded.length * 8;
                if (!isKeySizeApproved(algorithm, keySize)) {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "Key size not FIPS-approved: {0} bits for {1}", 
                                keySize, algorithm);
                    }
                    return false;
                }
            }
        }
        
        return true;
    }
    
    /**
     * Validates algorithm usage in FIPS mode.
     * 
     * <p>This method performs comprehensive validation:
     * <ul>
     *   <li>Checks if FIPS mode is enabled</li>
     *   <li>Validates algorithm is FIPS-approved</li>
     *   <li>Ensures algorithm is not prohibited</li>
     *   <li>Logs validation failures</li>
     * </ul>
     * 
     * @param algorithm the algorithm to validate
     * @throws SecurityException if algorithm is not FIPS-compliant
     */
    public static void validateAlgorithmForFIPS(String algorithm) throws SecurityException {
        if (!FIPSModeDetector.isFIPSEnabled()) {
            // FIPS mode not enabled - no validation needed
            return;
        }
        
        if (algorithm == null || algorithm.isEmpty()) {
            String msg = "Algorithm name is null or empty";
            if (tc.isWarningEnabled()) {
                Tr.warning(tc, "CWWKS4213W: " + msg);
            }
            throw new SecurityException(msg);
        }
        
        // Check if prohibited
        if (isAlgorithmProhibited(algorithm)) {
            String msg = "Algorithm is prohibited in FIPS mode: " + algorithm;
            if (tc.isWarningEnabled()) {
                Tr.warning(tc, "CWWKS4214W: " + msg);
            }
            throw new SecurityException(msg);
        }
        
        // Check if approved
        if (!isAlgorithmApproved(algorithm)) {
            String msg = "Algorithm is not FIPS-approved: " + algorithm;
            if (tc.isWarningEnabled()) {
                Tr.warning(tc, "CWWKS4215W: " + msg);
            }
            throw new SecurityException(msg);
        }
        
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Algorithm validated for FIPS mode: {0}", algorithm);
        }
    }
    
    /**
     * Validates key usage in FIPS mode.
     * 
     * @param key the key to validate
     * @throws SecurityException if key is not FIPS-compliant
     */
    public static void validateKeyForFIPS(Key key) throws SecurityException {
        if (!FIPSModeDetector.isFIPSEnabled()) {
            // FIPS mode not enabled - no validation needed
            return;
        }
        
        if (key == null) {
            String msg = "Key is null";
            if (tc.isWarningEnabled()) {
                Tr.warning(tc, "CWWKS4216W: " + msg);
            }
            throw new SecurityException(msg);
        }
        
        if (!isKeyApproved(key)) {
            String msg = "Key is not FIPS-compliant: " + key.getAlgorithm();
            if (tc.isWarningEnabled()) {
                Tr.warning(tc, "CWWKS4217W: " + msg);
            }
            throw new SecurityException(msg);
        }
        
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Key validated for FIPS mode: {0}", key.getAlgorithm());
        }
    }
    
    /**
     * Gets the set of FIPS-approved signature algorithms.
     * 
     * @return unmodifiable set of approved signature algorithms
     */
    @Trivial
    public static Set<String> getApprovedSignatureAlgorithms() {
        return APPROVED_SIGNATURE_ALGORITHMS;
    }
    
    /**
     * Gets the set of FIPS-approved hash algorithms.
     * 
     * @return unmodifiable set of approved hash algorithms
     */
    @Trivial
    public static Set<String> getApprovedHashAlgorithms() {
        return APPROVED_HASH_ALGORITHMS;
    }
    
    /**
     * Gets the set of FIPS-approved cipher algorithms.
     * 
     * @return unmodifiable set of approved cipher algorithms
     */
    @Trivial
    public static Set<String> getApprovedCipherAlgorithms() {
        return APPROVED_CIPHER_ALGORITHMS;
    }
    
    /**
     * Gets the set of prohibited algorithms.
     * 
     * @return unmodifiable set of prohibited algorithms
     */
    @Trivial
    public static Set<String> getProhibitedAlgorithms() {
        return PROHIBITED_ALGORITHMS;
    }
}

// Made with Bob
