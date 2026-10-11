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

import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * Immutable data class representing a Post-Quantum Cryptography (PQC) key pair.
 * 
 * <p>This class encapsulates a PQC public/private key pair along with metadata about
 * the algorithm used to generate the keys. It provides a type-safe wrapper around
 * standard Java {@link PublicKey} and {@link PrivateKey} objects with additional
 * PQC-specific context.</p>
 * 
 * <h3>Key Characteristics:</h3>
 * <ul>
 *   <li><b>Immutable:</b> Once created, the key pair cannot be modified</li>
 *   <li><b>Thread-Safe:</b> Can be safely shared across multiple threads</li>
 *   <li><b>Algorithm-Aware:</b> Includes the PQC algorithm used for key generation</li>
 *   <li><b>Validation:</b> Ensures keys are non-null at construction time</li>
 * </ul>
 * 
 * <h3>Usage Example:</h3>
 * <pre>
 * // Generate a PQC key pair
 * PQCProvider provider = PQCProviderFactory.getProvider();
 * PQCKeyPair keyPair = provider.generateKeyPair(PQCAlgorithm.ML_DSA_65);
 * 
 * // Access keys
 * PublicKey publicKey = keyPair.getPublicKey();
 * PrivateKey privateKey = keyPair.getPrivateKey();
 * PQCAlgorithm algorithm = keyPair.getAlgorithm();
 * 
 * // Use for signing
 * byte[] signature = provider.sign(data, keyPair.getPrivateKey(), keyPair.getAlgorithm());
 * </pre>
 * 
 * <h3>Security Considerations:</h3>
 * <ul>
 *   <li><b>Private Key Protection:</b> The private key should be protected and never
 *       transmitted over the network or logged</li>
 *   <li><b>Key Storage:</b> Private keys should be stored securely (e.g., in LTPA key files
 *       with appropriate file permissions)</li>
 *   <li><b>Key Lifecycle:</b> Keys should be rotated periodically according to security policy</li>
 *   <li><b>Memory Clearing:</b> Consider clearing sensitive key material from memory when no
 *       longer needed (though Java's garbage collector makes this challenging)</li>
 * </ul>
 * 
 * @see PQCProvider#generateKeyPair(PQCAlgorithm)
 * @see PQCAlgorithm
 * @since 1.0
 */
public final class PQCKeyPair {
    
    private final PublicKey publicKey;
    private final PrivateKey privateKey;
    private final PQCAlgorithm algorithm;
    
    /**
     * Constructs a new PQCKeyPair with the specified keys and algorithm.
     * 
     * <p>This constructor validates that both keys are non-null and stores them
     * along with the algorithm metadata. The keys are stored as-is without copying,
     * so the caller should not modify the key objects after passing them to this
     * constructor.</p>
     * 
     * <h4>Validation:</h4>
     * <ul>
     *   <li>Public key must not be null</li>
     *   <li>Private key must not be null</li>
     *   <li>Algorithm must not be null</li>
     * </ul>
     * 
     * <h4>Usage Example:</h4>
     * <pre>
     * KeyPairGenerator kpg = KeyPairGenerator.getInstance("ML-DSA-65");
     * java.security.KeyPair javaKeyPair = kpg.generateKeyPair();
     * 
     * PQCKeyPair pqcKeyPair = new PQCKeyPair(
     *     javaKeyPair.getPublic(),
     *     javaKeyPair.getPrivate(),
     *     PQCAlgorithm.ML_DSA_65
     * );
     * </pre>
     * 
     * @param publicKey the PQC public key (must not be null)
     * @param privateKey the PQC private key (must not be null)
     * @param algorithm the PQC algorithm used to generate the keys (must not be null)
     * @throws IllegalArgumentException if any parameter is null
     */
    public PQCKeyPair(PublicKey publicKey, PrivateKey privateKey, PQCAlgorithm algorithm) {
        if (publicKey == null) {
            throw new IllegalArgumentException("Public key cannot be null");
        }
        if (privateKey == null) {
            throw new IllegalArgumentException("Private key cannot be null");
        }
        if (algorithm == null) {
            throw new IllegalArgumentException("Algorithm cannot be null");
        }
        
        this.publicKey = publicKey;
        this.privateKey = privateKey;
        this.algorithm = algorithm;
    }
    
    /**
     * Returns the public key from this key pair.
     * 
     * <p>The public key is used for signature verification and can be safely shared
     * or transmitted. In PQC-LTPA tokens, the public key is typically included in
     * the token itself or distributed through a separate key distribution mechanism.</p>
     * 
     * <h4>Public Key Usage:</h4>
     * <ul>
     *   <li>Signature verification</li>
     *   <li>Token validation</li>
     *   <li>Key distribution to relying parties</li>
     *   <li>Storage in LTPA key files for validation</li>
     * </ul>
     * 
     * @return the public key (never null)
     */
    public PublicKey getPublicKey() {
        return publicKey;
    }
    
    /**
     * Returns the private key from this key pair.
     * 
     * <p><b>Security Warning:</b> The private key is sensitive cryptographic material
     * that must be protected. It should:</p>
     * <ul>
     *   <li>Never be transmitted over the network</li>
     *   <li>Never be logged or written to unsecured storage</li>
     *   <li>Be stored with appropriate file permissions (e.g., 600 on Unix)</li>
     *   <li>Be encrypted when stored (e.g., in LTPA key files)</li>
     *   <li>Be accessible only to authorized processes</li>
     * </ul>
     * 
     * <h4>Private Key Usage:</h4>
     * <ul>
     *   <li>Signature generation (token creation)</li>
     *   <li>Secure storage in LTPA key files</li>
     *   <li>Key rotation operations</li>
     * </ul>
     * 
     * @return the private key (never null)
     */
    public PrivateKey getPrivateKey() {
        return privateKey;
    }
    
    /**
     * Returns the PQC algorithm used to generate this key pair.
     * 
     * <p>The algorithm metadata is essential for:</p>
     * <ul>
     *   <li>Selecting the correct signature algorithm</li>
     *   <li>Determining key sizes and signature sizes</li>
     *   <li>Validating algorithm compatibility</li>
     *   <li>FIPS compliance checking</li>
     * </ul>
     * 
     * @return the PQC algorithm (never null)
     */
    public PQCAlgorithm getAlgorithm() {
        return algorithm;
    }
    
    /**
     * Returns a string representation of this key pair.
     * 
     * <p>The string representation includes:</p>
     * <ul>
     *   <li>The PQC algorithm name</li>
     *   <li>The public key algorithm and format</li>
     *   <li>The private key algorithm (but not the key material)</li>
     * </ul>
     * 
     * <p><b>Security Note:</b> This method does not include any sensitive key material
     * in the output, making it safe for logging and debugging.</p>
     * 
     * <h4>Example Output:</h4>
     * <pre>
     * PQCKeyPair[algorithm=ML-DSA-65, publicKey=ML-DSA-65 Public Key (X.509), privateKey=ML-DSA-65 Private Key]
     * </pre>
     * 
     * @return a string representation of this key pair
     */
    @Override
    public String toString() {
        return String.format("PQCKeyPair[algorithm=%s, publicKey=%s Public Key (%s), privateKey=%s Private Key]",
                           algorithm.getNistName(),
                           publicKey.getAlgorithm(),
                           publicKey.getFormat(),
                           privateKey.getAlgorithm());
    }
    
    /**
     * Compares this key pair to another object for equality.
     * 
     * <p>Two PQCKeyPair objects are considered equal if and only if:</p>
     * <ul>
     *   <li>They have the same algorithm</li>
     *   <li>Their public keys are equal (based on encoded form)</li>
     *   <li>Their private keys are equal (based on encoded form)</li>
     * </ul>
     * 
     * <p><b>Note:</b> This method compares the encoded forms of the keys, which may
     * be expensive for large PQC keys. Use with caution in performance-critical code.</p>
     * 
     * @param obj the object to compare with
     * @return true if the objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        
        PQCKeyPair other = (PQCKeyPair) obj;
        
        // Compare algorithm
        if (algorithm != other.algorithm) {
            return false;
        }
        
        // Compare public keys by encoded form
        if (!java.util.Arrays.equals(publicKey.getEncoded(), other.publicKey.getEncoded())) {
            return false;
        }
        
        // Compare private keys by encoded form
        if (!java.util.Arrays.equals(privateKey.getEncoded(), other.privateKey.getEncoded())) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Returns a hash code for this key pair.
     * 
     * <p>The hash code is computed based on:</p>
     * <ul>
     *   <li>The algorithm</li>
     *   <li>The encoded form of the public key</li>
     *   <li>The encoded form of the private key</li>
     * </ul>
     * 
     * <p><b>Note:</b> Computing the hash code requires encoding the keys, which may
     * be expensive for large PQC keys. Consider caching the hash code if it will be
     * used frequently.</p>
     * 
     * @return a hash code value for this key pair
     */
    @Override
    public int hashCode() {
        int result = algorithm.hashCode();
        result = 31 * result + java.util.Arrays.hashCode(publicKey.getEncoded());
        result = 31 * result + java.util.Arrays.hashCode(privateKey.getEncoded());
        return result;
    }
}

// Made with Bob
