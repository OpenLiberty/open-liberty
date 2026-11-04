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
import java.util.Set;

/**
 * Provider abstraction for Post-Quantum Cryptography (PQC) operations in LTPA token processing.
 * 
 * <p>This interface defines the contract for PQC cryptographic providers that support quantum-resistant
 * digital signatures for LTPA tokens. Implementations of this interface abstract the underlying
 * cryptographic provider (BouncyCastle, JEP 497, OpenJCEPlusFIPS) and provide a consistent API
 * across different Java versions and FIPS modes.</p>
 * 
 * <h3>Provider Implementations:</h3>
 * <ul>
 *   <li><b>BouncyCastlePQCProvider</b> - For Java 8-17 using BouncyCastle PQC library</li>
 *   <li><b>JEP497PreviewProvider</b> - For Java 21 using JEP 497 preview features</li>
 *   <li><b>NativePQCProvider</b> - For Java 24+ using finalized JEP 497</li>
 *   <li><b>FIPSPQCProvider</b> - For FIPS 140-3 mode using OpenJCEPlusFIPS</li>
 * </ul>
 * 
 * <h3>Provider Selection:</h3>
 * <p>The appropriate provider is automatically selected based on:</p>
 * <ol>
 *   <li>FIPS mode status (highest priority)</li>
 *   <li>Java version (8, 11, 17, 21, 24+)</li>
 *   <li>Available cryptographic libraries</li>
 * </ol>
 * 
 * <h3>Usage Example:</h3>
 * <pre>
 * // Get provider (automatically selected based on environment)
 * PQCProvider provider = PQCProviderFactory.getProvider();
 * 
 * // Check provider capabilities
 * if (!provider.isAvailable()) {
 *     throw new PQCException("PQC provider not available");
 * }
 * 
 * // Generate key pair
 * PQCKeyPair keyPair = provider.generateKeyPair(PQCAlgorithm.ML_DSA_65);
 * 
 * // Sign data
 * byte[] data = "token payload".getBytes();
 * byte[] signature = provider.sign(data, keyPair.getPrivateKey(), PQCAlgorithm.ML_DSA_65);
 * 
 * // Verify signature
 * boolean valid = provider.verify(data, signature, keyPair.getPublicKey(), PQCAlgorithm.ML_DSA_65);
 * </pre>
 * 
 * <h3>Thread Safety:</h3>
 * <p>Implementations of this interface must be thread-safe. Multiple threads may call provider
 * methods concurrently for token generation and validation operations.</p>
 * 
 * <h3>FIPS Compliance:</h3>
 * <p>When FIPS 140-3 mode is enabled, the provider must use FIPS-validated cryptographic modules.
 * Use {@link #isFIPSCompliant()} to verify FIPS compliance before performing cryptographic operations
 * in FIPS-required environments.</p>
 * 
 * @see PQCAlgorithm
 * @see PQCKeyPair
 * @see PQCException
 * @since 1.0
 */
public interface PQCProvider {
    
    // ========================================================================
    // Provider Information
    // ========================================================================
    
    /**
     * Returns the name of this PQC provider.
     * 
     * <p>The provider name identifies the underlying cryptographic provider being used:</p>
     * <ul>
     *   <li><b>"BCPQC"</b> - BouncyCastle PQC provider (Java 8-17)</li>
     *   <li><b>"SunJCE"</b> - Sun JCE provider with JEP 497 (Java 21+)</li>
     *   <li><b>"OpenJCEPlusFIPS"</b> - IBM FIPS provider (FIPS mode)</li>
     * </ul>
     * 
     * <p>The provider name is useful for:</p>
     * <ul>
     *   <li>Logging and diagnostics</li>
     *   <li>Provider-specific configuration</li>
     *   <li>Troubleshooting cryptographic issues</li>
     * </ul>
     * 
     * @return the provider name (never null)
     */
    String getProviderName();
    
    /**
     * Returns the version of this PQC provider.
     * 
     * <p>The version string format is provider-specific but typically follows semantic
     * versioning (e.g., "1.78" for BouncyCastle, "21.0.1" for JEP 497).</p>
     * 
     * <p>Version information is useful for:</p>
     * <ul>
     *   <li>Compatibility checking</li>
     *   <li>Bug tracking and support</li>
     *   <li>Feature availability determination</li>
     * </ul>
     * 
     * @return the provider version string (never null)
     */
    String getProviderVersion();
    
    /**
     * Checks if this PQC provider is available and ready for use.
     * 
     * <p>A provider is considered available if:</p>
     * <ul>
     *   <li>All required cryptographic libraries are loaded</li>
     *   <li>The provider is properly registered with the JCA</li>
     *   <li>At least one PQC algorithm is supported</li>
     *   <li>The provider can perform basic cryptographic operations</li>
     * </ul>
     * 
     * <p>This method should be called before attempting to use the provider for
     * cryptographic operations. If the provider is not available, consider:</p>
     * <ul>
     *   <li>Falling back to classical LTPA tokens</li>
     *   <li>Logging an error for administrator attention</li>
     *   <li>Checking for missing dependencies or configuration issues</li>
     * </ul>
     * 
     * @return true if the provider is available and ready for use, false otherwise
     */
    boolean isAvailable();
    
    // ========================================================================
    // Algorithm Support
    // ========================================================================
    
    /**
     * Checks if the specified PQC algorithm is supported by this provider.
     * 
     * <p>Algorithm support may vary based on:</p>
     * <ul>
     *   <li>Provider implementation (BouncyCastle vs JEP 497)</li>
     *   <li>Java version (some algorithms require Java 21+)</li>
     *   <li>FIPS mode (FIPS providers may restrict algorithms)</li>
     *   <li>Provider configuration</li>
     * </ul>
     * 
     * <p>Always check algorithm support before attempting to use an algorithm,
     * especially when supporting multiple Java versions or FIPS mode.</p>
     * 
     * @param algorithm the PQC algorithm to check
     * @return true if the algorithm is supported, false otherwise
     * @throws IllegalArgumentException if algorithm is null
     */
    boolean supportsAlgorithm(PQCAlgorithm algorithm);
    
    /**
     * Returns the set of PQC algorithms supported by this provider.
     * 
     * <p>The returned set contains all algorithms that can be used with this provider
     * for key generation, signing, and verification operations. The set is immutable
     * and reflects the current provider configuration.</p>
     * 
     * <p>Use this method to:</p>
     * <ul>
     *   <li>Display available algorithms to administrators</li>
     *   <li>Validate configuration settings</li>
     *   <li>Select an appropriate algorithm based on availability</li>
     * </ul>
     * 
     * @return an immutable set of supported algorithms (never null, may be empty)
     */
    Set<PQCAlgorithm> getSupportedAlgorithms();
    
    // ========================================================================
    // Key Operations
    // ========================================================================
    
    /**
     * Generates a new PQC key pair for the specified algorithm.
     * 
     * <p>This method generates a cryptographically secure key pair suitable for
     * PQC-LTPA token signatures. The generated keys are:</p>
     * <ul>
     *   <li>Cryptographically random</li>
     *   <li>Suitable for long-term use (subject to key rotation policy)</li>
     *   <li>Compliant with NIST FIPS 204 specifications</li>
     *   <li>FIPS-validated if the provider is FIPS-compliant</li>
     * </ul>
     * 
     * <h4>Key Generation Performance:</h4>
     * <p>PQC key generation is computationally intensive compared to classical algorithms.
     * Typical generation times:</p>
     * <ul>
     *   <li>ML-DSA-65: 10-50ms depending on provider and hardware</li>
     *   <li>ML-DSA-87: 15-75ms depending on provider and hardware</li>
     * </ul>
     * 
     * <h4>Key Storage:</h4>
     * <p>Generated keys should be stored securely in LTPA key files with:</p>
     * <ul>
     *   <li>Encryption (using AES-256 or stronger)</li>
     *   <li>Appropriate file permissions (e.g., 600 on Unix)</li>
     *   <li>Backup and recovery procedures</li>
     * </ul>
     * 
     * @param algorithm the PQC algorithm to use for key generation
     * @return a new PQC key pair (never null)
     * @throws PQCException if key generation fails
     * @throws IllegalArgumentException if algorithm is null or not supported
     */
    PQCKeyPair generateKeyPair(PQCAlgorithm algorithm) throws PQCException;
    
    /**
     * Encodes a PQC public key to a byte array for storage or transmission.
     * 
     * <p>The encoding format is provider-specific but typically follows X.509
     * SubjectPublicKeyInfo structure. The encoded key can be:</p>
     * <ul>
     *   <li>Stored in LTPA key files</li>
     *   <li>Included in LTPA tokens</li>
     *   <li>Transmitted to relying parties</li>
     *   <li>Decoded using {@link #decodePublicKey(byte[], PQCAlgorithm)}</li>
     * </ul>
     * 
     * <h4>Encoded Key Size:</h4>
     * <ul>
     *   <li>ML-DSA-65: ~1,952 bytes</li>
     *   <li>ML-DSA-87: ~2,592 bytes</li>
     * </ul>
     * 
     * @param publicKey the public key to encode
     * @return the encoded public key bytes (never null)
     * @throws PQCException if encoding fails
     * @throws IllegalArgumentException if publicKey is null
     */
    byte[] encodePublicKey(PublicKey publicKey) throws PQCException;
    
    /**
     * Encodes a PQC private key to a byte array for secure storage.
     * 
     * <p>The encoding format is provider-specific but typically follows PKCS#8
     * PrivateKeyInfo structure. The encoded key should be:</p>
     * <ul>
     *   <li>Encrypted before storage (using AES-256 or stronger)</li>
     *   <li>Protected with appropriate file permissions</li>
     *   <li>Never transmitted over the network</li>
     *   <li>Decoded using {@link #decodePrivateKey(byte[], PQCAlgorithm)}</li>
     * </ul>
     * 
     * <h4>Encoded Key Size:</h4>
     * <ul>
     *   <li>ML-DSA-65: ~4,000 bytes</li>
     *   <li>ML-DSA-87: ~4,864 bytes</li>
     * </ul>
     * 
     * <p><b>Security Warning:</b> The encoded private key contains sensitive cryptographic
     * material. Handle with extreme care and clear from memory when no longer needed.</p>
     * 
     * @param privateKey the private key to encode
     * @return the encoded private key bytes (never null)
     * @throws PQCException if encoding fails
     * @throws IllegalArgumentException if privateKey is null
     */
    byte[] encodePrivateKey(PrivateKey privateKey) throws PQCException;
    
    /**
     * Decodes a PQC public key from a byte array.
     * 
     * <p>This method reconstructs a public key from its encoded form (typically X.509
     * SubjectPublicKeyInfo). The encoded bytes must have been produced by
     * {@link #encodePublicKey(PublicKey)} or a compatible encoding method.</p>
     * 
     * <p>Use this method to:</p>
     * <ul>
     *   <li>Load public keys from LTPA key files</li>
     *   <li>Extract public keys from LTPA tokens</li>
     *   <li>Reconstruct keys received from relying parties</li>
     * </ul>
     * 
     * @param encoded the encoded public key bytes
     * @param algorithm the PQC algorithm of the key
     * @return the decoded public key (never null)
     * @throws PQCException if decoding fails or the encoded data is invalid
     * @throws IllegalArgumentException if encoded is null or algorithm is null
     */
    PublicKey decodePublicKey(byte[] encoded, PQCAlgorithm algorithm) throws PQCException;
    
    /**
     * Decodes a PQC private key from a byte array.
     * 
     * <p>This method reconstructs a private key from its encoded form (typically PKCS#8
     * PrivateKeyInfo). The encoded bytes must have been produced by
     * {@link #encodePrivateKey(PrivateKey)} or a compatible encoding method.</p>
     * 
     * <p>Use this method to:</p>
     * <ul>
     *   <li>Load private keys from LTPA key files</li>
     *   <li>Reconstruct keys after decryption</li>
     * </ul>
     * 
     * <p><b>Security Warning:</b> The decoded private key is sensitive cryptographic
     * material. Protect it appropriately and clear from memory when no longer needed.</p>
     * 
     * @param encoded the encoded private key bytes
     * @param algorithm the PQC algorithm of the key
     * @return the decoded private key (never null)
     * @throws PQCException if decoding fails or the encoded data is invalid
     * @throws IllegalArgumentException if encoded is null or algorithm is null
     */
    PrivateKey decodePrivateKey(byte[] encoded, PQCAlgorithm algorithm) throws PQCException;
    
    // ========================================================================
    // Signature Operations
    // ========================================================================
    
    /**
     * Signs data using a PQC private key.
     * 
     * <p>This method generates a quantum-resistant digital signature over the provided
     * data using the specified private key and algorithm. The signature can be verified
     * using {@link #verify(byte[], byte[], PublicKey, PQCAlgorithm)}.</p>
     * 
     * <h4>Signature Properties:</h4>
     * <ul>
     *   <li><b>Deterministic:</b> Same data and key produce the same signature (for ML-DSA)</li>
     *   <li><b>Quantum-Resistant:</b> Secure against quantum computer attacks</li>
     *   <li><b>FIPS-Compliant:</b> When using FIPS-validated provider</li>
     * </ul>
     * 
     * <h4>Signature Size:</h4>
     * <ul>
     *   <li>ML-DSA-65: 3,293 bytes</li>
     *   <li>ML-DSA-87: 4,595 bytes</li>
     * </ul>
     * 
     * <h4>Performance:</h4>
     * <p>Signature generation is computationally intensive. Typical times:</p>
     * <ul>
     *   <li>ML-DSA-65: 1-5ms depending on provider and hardware</li>
     *   <li>ML-DSA-87: 2-8ms depending on provider and hardware</li>
     * </ul>
     * 
     * @param data the data to sign (must not be null)
     * @param privateKey the PQC private key to use for signing
     * @param algorithm the PQC algorithm to use
     * @return the digital signature bytes (never null)
     * @throws PQCException if signing fails
     * @throws IllegalArgumentException if any parameter is null
     */
    byte[] sign(byte[] data, PrivateKey privateKey, PQCAlgorithm algorithm) throws PQCException;
    
    /**
     * Verifies a PQC signature.
     * 
     * <p>This method verifies that the provided signature was generated by the private key
     * corresponding to the given public key, over the specified data. It returns true if
     * the signature is valid, false otherwise.</p>
     * 
     * <h4>Verification Properties:</h4>
     * <ul>
     *   <li><b>Fast:</b> Verification is faster than signature generation</li>
     *   <li><b>Deterministic:</b> Same inputs always produce the same result</li>
     *   <li><b>Secure:</b> Computationally infeasible to forge valid signatures</li>
     * </ul>
     * 
     * <h4>Performance:</h4>
     * <p>Signature verification is faster than generation. Typical times:</p>
     * <ul>
     *   <li>ML-DSA-65: 0.5-2ms depending on provider and hardware</li>
     *   <li>ML-DSA-87: 1-3ms depending on provider and hardware</li>
     * </ul>
     * 
     * <h4>Error Handling:</h4>
     * <p>This method returns false for invalid signatures rather than throwing an exception.
     * Exceptions are thrown only for operational errors (e.g., invalid key format, provider
     * unavailable).</p>
     * 
     * @param data the original data that was signed
     * @param signature the signature bytes to verify
     * @param publicKey the PQC public key to use for verification
     * @param algorithm the PQC algorithm to use
     * @return true if the signature is valid, false otherwise
     * @throws PQCException if verification fails due to an operational error
     * @throws IllegalArgumentException if any parameter is null
     */
    boolean verify(byte[] data, byte[] signature, PublicKey publicKey, PQCAlgorithm algorithm) throws PQCException;
    
    // ========================================================================
    // Performance Characteristics
    // ========================================================================
    
    /**
     * Returns the signature size in bytes for the specified algorithm.
     * 
     * <p>This method returns the exact size of signatures produced by the algorithm,
     * which is useful for:</p>
     * <ul>
     *   <li>Allocating buffers for signature storage</li>
     *   <li>Estimating LTPA token sizes</li>
     *   <li>Validating signature data</li>
     *   <li>Network bandwidth planning</li>
     * </ul>
     * 
     * <h4>Signature Sizes:</h4>
     * <ul>
     *   <li>ML-DSA-65: 3,293 bytes</li>
     *   <li>ML-DSA-87: 4,595 bytes</li>
     * </ul>
     * 
     * @param algorithm the PQC algorithm
     * @return the signature size in bytes
     * @throws IllegalArgumentException if algorithm is null or not supported
     */
    int getSignatureSize(PQCAlgorithm algorithm);
    
    /**
     * Returns the public key size in bytes for the specified algorithm.
     * 
     * <p>This method returns the encoded size of public keys, which is useful for:</p>
     * <ul>
     *   <li>Allocating buffers for key storage</li>
     *   <li>Estimating LTPA token sizes (if public key is included)</li>
     *   <li>Network bandwidth planning</li>
     * </ul>
     * 
     * <h4>Public Key Sizes:</h4>
     * <ul>
     *   <li>ML-DSA-65: 1,952 bytes</li>
     *   <li>ML-DSA-87: 2,592 bytes</li>
     * </ul>
     * 
     * @param algorithm the PQC algorithm
     * @return the public key size in bytes
     * @throws IllegalArgumentException if algorithm is null or not supported
     */
    int getPublicKeySize(PQCAlgorithm algorithm);
    
    /**
     * Returns the private key size in bytes for the specified algorithm.
     * 
     * <p>This method returns the encoded size of private keys, which is useful for:</p>
     * <ul>
     *   <li>Allocating buffers for key storage</li>
     *   <li>Estimating LTPA key file sizes</li>
     *   <li>Storage capacity planning</li>
     * </ul>
     * 
     * <h4>Private Key Sizes:</h4>
     * <ul>
     *   <li>ML-DSA-65: 4,000 bytes</li>
     *   <li>ML-DSA-87: 4,864 bytes</li>
     * </ul>
     * 
     * @param algorithm the PQC algorithm
     * @return the private key size in bytes
     * @throws IllegalArgumentException if algorithm is null or not supported
     */
    int getPrivateKeySize(PQCAlgorithm algorithm);
    
    // ========================================================================
    // FIPS Compliance
    // ========================================================================
    
    /**
     * Checks if this provider is FIPS 140-3 compliant.
     * 
     * <p>A provider is FIPS-compliant if it uses a FIPS-validated cryptographic module
     * for all cryptographic operations. FIPS compliance is required in certain government
     * and regulated environments.</p>
     * 
     * <h4>FIPS-Compliant Providers:</h4>
     * <ul>
     *   <li><b>OpenJCEPlusFIPS</b> - IBM FIPS provider (FIPS mode)</li>
     *   <li><b>JEP 497 (Java 24+)</b> - Native Java PQC with FIPS support</li>
     * </ul>
     * 
     * <h4>Non-FIPS Providers:</h4>
     * <ul>
     *   <li><b>BouncyCastle PQC</b> - Not FIPS-validated</li>
     *   <li><b>JEP 497 (Java 21)</b> - Preview, partial FIPS support</li>
     * </ul>
     * 
     * <p>When FIPS mode is required, check this method before performing cryptographic
     * operations. If the provider is not FIPS-compliant, consider:</p>
     * <ul>
     *   <li>Switching to a FIPS-compliant provider</li>
     *   <li>Disabling PQC-LTPA in favor of classical LTPA with FIPS algorithms</li>
     *   <li>Logging a warning for administrator attention</li>
     * </ul>
     * 
     * @return true if the provider is FIPS 140-3 compliant, false otherwise
     */
    boolean isFIPSCompliant();
}

// Made with Bob
