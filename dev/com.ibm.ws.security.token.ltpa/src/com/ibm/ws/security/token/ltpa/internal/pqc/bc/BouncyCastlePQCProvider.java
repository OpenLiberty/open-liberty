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
package com.ibm.ws.security.token.ltpa.internal.pqc.bc;

import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.pqc.jcajce.provider.BouncyCastlePQCProvider;
import org.bouncycastle.pqc.jcajce.spec.DilithiumParameterSpec;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.token.ltpa.internal.pqc.PQCAlgorithm;
import com.ibm.ws.security.token.ltpa.internal.pqc.PQCException;
import com.ibm.ws.security.token.ltpa.internal.pqc.PQCKeyPair;
import com.ibm.ws.security.token.ltpa.internal.pqc.PQCProvider;

/**
 * BouncyCastle-based implementation of PQCProvider for Java 8-17 support.
 * 
 * <p>This provider uses the BouncyCastle PQC library (bcpqc-jdk18on) to provide
 * Post-Quantum Cryptography support for LTPA tokens on Java 8, 11, 17, and 21.
 * It implements NIST FIPS 204 ML-DSA (Module-Lattice-Based Digital Signature Algorithm)
 * using BouncyCastle's Dilithium implementation.</p>
 * 
 * <h3>Algorithm Name Mapping:</h3>
 * <p>This provider maps NIST standard names to BouncyCastle algorithm names:</p>
 * <ul>
 *   <li><b>ML-DSA-65</b> (NIST FIPS 204) → <b>Dilithium3</b> (BouncyCastle)</li>
 *   <li><b>ML-DSA-87</b> (NIST FIPS 204) → <b>Dilithium5</b> (BouncyCastle)</li>
 * </ul>
 * 
 * <h3>Provider Registration:</h3>
 * <p>This implementation automatically registers both BouncyCastle providers:</p>
 * <ul>
 *   <li><b>BouncyCastleProvider</b> - Core cryptographic provider</li>
 *   <li><b>BouncyCastlePQCProvider</b> - Post-Quantum cryptographic provider</li>
 * </ul>
 * 
 * <h3>Java Version Compatibility:</h3>
 * <ul>
 *   <li><b>Java 8</b> - Fully supported (primary target)</li>
 *   <li><b>Java 11</b> - Fully supported</li>
 *   <li><b>Java 17</b> - Fully supported</li>
 *   <li><b>Java 21</b> - Supported (fallback when JEP 497 preview not enabled)</li>
 * </ul>
 * 
 * <h3>FIPS Compliance:</h3>
 * <p><b>WARNING:</b> BouncyCastle PQC is NOT FIPS 140-3 validated. This provider
 * should not be used in FIPS-required environments. Use FIPSPQCProvider instead
 * when FIPS mode is enabled.</p>
 * 
 * <h3>Performance Characteristics:</h3>
 * <table border="1">
 *   <tr>
 *     <th>Operation</th>
 *     <th>ML-DSA-65 (Dilithium3)</th>
 *     <th>ML-DSA-87 (Dilithium5)</th>
 *   </tr>
 *   <tr>
 *     <td>Key Generation</td>
 *     <td>10-50ms</td>
 *     <td>15-75ms</td>
 *   </tr>
 *   <tr>
 *     <td>Signature Generation</td>
 *     <td>1-5ms</td>
 *     <td>2-8ms</td>
 *   </tr>
 *   <tr>
 *     <td>Signature Verification</td>
 *     <td>0.5-2ms</td>
 *     <td>1-3ms</td>
 *   </tr>
 *   <tr>
 *     <td>Public Key Size</td>
 *     <td>1,952 bytes</td>
 *     <td>2,592 bytes</td>
 *   </tr>
 *   <tr>
 *     <td>Private Key Size</td>
 *     <td>4,000 bytes</td>
 *     <td>4,864 bytes</td>
 *   </tr>
 *   <tr>
 *     <td>Signature Size</td>
 *     <td>3,293 bytes</td>
 *     <td>4,595 bytes</td>
 *   </tr>
 * </table>
 * 
 * <h3>Thread Safety:</h3>
 * <p>This implementation is thread-safe. Multiple threads can safely call provider
 * methods concurrently. Internal state is either immutable or properly synchronized.</p>
 * 
 * <h3>Dependencies:</h3>
 * <pre>
 * org.bouncycastle:bcprov-jdk18on:1.78
 * org.bouncycastle:bcpqc-jdk18on:1.78
 * </pre>
 * 
 * @see PQCProvider
 * @see PQCAlgorithm
 * @see PQCKeyPair
 * @since 1.0
 */
public class BouncyCastlePQCProvider implements PQCProvider {
    
    private static final TraceComponent tc = Tr.register(BouncyCastlePQCProvider.class);
    
    // ========================================================================
    // Provider Constants
    // ========================================================================
    
    /** Provider name for BouncyCastle PQC */
    private static final String PROVIDER_NAME = "BCPQC";
    
    /** Provider version (BouncyCastle 1.78) */
    private static final String PROVIDER_VERSION = "1.78";
    
    /** BouncyCastle core provider name */
    private static final String BC_PROVIDER_NAME = "BC";
    
    /** BouncyCastle PQC provider name */
    private static final String BCPQC_PROVIDER_NAME = "BCPQC";
    
    // ========================================================================
    // Algorithm Name Mapping (NIST → BouncyCastle)
    // ========================================================================
    
    /**
     * Maps NIST FIPS 204 algorithm names to BouncyCastle Dilithium names.
     * 
     * <p>NIST standardized ML-DSA (Module-Lattice-Based Digital Signature Algorithm)
     * in FIPS 204, but BouncyCastle uses the original CRYSTALS-Dilithium names.
     * This mapping ensures compatibility between NIST standards and BouncyCastle
     * implementation.</p>
     * 
     * <p>Mapping details:</p>
     * <ul>
     *   <li>ML-DSA-65 → Dilithium3 (NIST Security Level 3, 192-bit security)</li>
     *   <li>ML-DSA-87 → Dilithium5 (NIST Security Level 5, 256-bit security)</li>
     * </ul>
     */
    private static final String DILITHIUM3 = "Dilithium3"; // ML-DSA-65
    private static final String DILITHIUM5 = "Dilithium5"; // ML-DSA-87
    
    // ========================================================================
    // Supported Algorithms
    // ========================================================================
    
    /**
     * Set of PQC algorithms supported by this provider.
     * 
     * <p>Currently supports:</p>
     * <ul>
     *   <li>ML-DSA-65 (Dilithium3) - NIST Security Level 3</li>
     *   <li>ML-DSA-87 (Dilithium5) - NIST Security Level 5</li>
     * </ul>
     */
    private static final Set<PQCAlgorithm> SUPPORTED_ALGORITHMS = 
        Collections.unmodifiableSet(EnumSet.of(
            PQCAlgorithm.ML_DSA_65,
            PQCAlgorithm.ML_DSA_87
        ));
    
    // ========================================================================
    // Provider Availability
    // ========================================================================
    
    /** Flag indicating if BouncyCastle providers are available and registered */
    private final boolean available;
    
    // ========================================================================
    // Constructor
    // ========================================================================
    
    /**
     * Constructs a new BouncyCastlePQCProvider and registers BouncyCastle providers.
     * 
     * <p>This constructor attempts to register both BouncyCastle providers with the
     * Java Cryptography Architecture (JCA). If registration fails, the provider will
     * be marked as unavailable.</p>
     * 
     * <p>Provider registration is idempotent - if the providers are already registered,
     * this operation has no effect.</p>
     * 
     * <h4>Registration Process:</h4>
     * <ol>
     *   <li>Check if BouncyCastleProvider is already registered</li>
     *   <li>Register BouncyCastleProvider if not present</li>
     *   <li>Check if BouncyCastlePQCProvider is already registered</li>
     *   <li>Register BouncyCastlePQCProvider if not present</li>
     *   <li>Verify provider availability by checking for Dilithium3 algorithm</li>
     * </ol>
     */
    public BouncyCastlePQCProvider() {
        boolean isAvailable = false;
        
        try {
            // Register BouncyCastle core provider if not already registered
            if (Security.getProvider(BC_PROVIDER_NAME) == null) {
                Security.addProvider(new BouncyCastleProvider());
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Registered BouncyCastleProvider");
                }
            }
            
            // Register BouncyCastle PQC provider if not already registered
            if (Security.getProvider(BCPQC_PROVIDER_NAME) == null) {
                Security.addProvider(new BouncyCastlePQCProvider());
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Registered BouncyCastlePQCProvider");
                }
            }
            
            // Verify provider availability by checking for Dilithium3 algorithm
            // This ensures the provider is properly registered and functional
            KeyPairGenerator.getInstance(DILITHIUM3, BCPQC_PROVIDER_NAME);
            isAvailable = true;
            
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "BouncyCastlePQCProvider initialized successfully");
            }
            
        } catch (NoSuchAlgorithmException e) {
            Tr.error(tc, "CWWKS4200E: BouncyCastle PQC provider not available: algorithm not found", e);
        } catch (NoSuchProviderException e) {
            Tr.error(tc, "CWWKS4200E: BouncyCastle PQC provider not available: provider not found", e);
        } catch (Exception e) {
            Tr.error(tc, "CWWKS4200E: BouncyCastle PQC provider initialization failed", e);
        }
        
        this.available = isAvailable;
    }
    
    // ========================================================================
    // Provider Information
    // ========================================================================
    
    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }
    
    @Override
    public String getProviderVersion() {
        return PROVIDER_VERSION;
    }
    
    @Override
    public boolean isAvailable() {
        return available;
    }
    
    // ========================================================================
    // Algorithm Support
    // ========================================================================
    
    @Override
    public boolean supportsAlgorithm(PQCAlgorithm algorithm) {
        if (algorithm == null) {
            throw new IllegalArgumentException("Algorithm cannot be null");
        }
        return SUPPORTED_ALGORITHMS.contains(algorithm);
    }
    
    @Override
    public Set<PQCAlgorithm> getSupportedAlgorithms() {
        return SUPPORTED_ALGORITHMS;
    }
    
    // ========================================================================
    // Algorithm Name Mapping
    // ========================================================================
    
    /**
     * Maps a NIST FIPS 204 algorithm to its BouncyCastle Dilithium equivalent.
     * 
     * <p>This method performs the critical translation between NIST standard names
     * and BouncyCastle implementation names. The mapping is based on security levels:</p>
     * 
     * <table border="1">
     *   <tr>
     *     <th>NIST Name</th>
     *     <th>BouncyCastle Name</th>
     *     <th>Security Level</th>
     *     <th>Quantum Security</th>
     *   </tr>
     *   <tr>
     *     <td>ML-DSA-65</td>
     *     <td>Dilithium3</td>
     *     <td>NIST Level 3</td>
     *     <td>192-bit</td>
     *   </tr>
     *   <tr>
     *     <td>ML-DSA-87</td>
     *     <td>Dilithium5</td>
     *     <td>NIST Level 5</td>
     *     <td>256-bit</td>
     *   </tr>
     * </table>
     * 
     * @param algorithm the NIST FIPS 204 algorithm
     * @return the corresponding BouncyCastle Dilithium algorithm name
     * @throws IllegalArgumentException if algorithm is null or not supported
     */
    private String mapAlgorithmName(PQCAlgorithm algorithm) {
        if (algorithm == null) {
            throw new IllegalArgumentException("Algorithm cannot be null");
        }
        
        switch (algorithm) {
            case ML_DSA_65:
                return DILITHIUM3;
            case ML_DSA_87:
                return DILITHIUM5;
            default:
                throw new IllegalArgumentException("Unsupported algorithm: " + algorithm);
        }
    }
    
    /**
     * Gets the DilithiumParameterSpec for the specified algorithm.
     * 
     * <p>BouncyCastle requires a DilithiumParameterSpec to configure key generation.
     * This method returns the appropriate parameter spec based on the algorithm.</p>
     * 
     * @param algorithm the PQC algorithm
     * @return the DilithiumParameterSpec for the algorithm
     * @throws IllegalArgumentException if algorithm is null or not supported
     */
    private DilithiumParameterSpec getParameterSpec(PQCAlgorithm algorithm) {
        if (algorithm == null) {
            throw new IllegalArgumentException("Algorithm cannot be null");
        }
        
        switch (algorithm) {
            case ML_DSA_65:
                return DilithiumParameterSpec.dilithium3;
            case ML_DSA_87:
                return DilithiumParameterSpec.dilithium5;
            default:
                throw new IllegalArgumentException("Unsupported algorithm: " + algorithm);
        }
    }
    
    // ========================================================================
    // Key Operations
    // ========================================================================
    
    @Override
    public PQCKeyPair generateKeyPair(PQCAlgorithm algorithm) throws PQCException {
        if (algorithm == null) {
            throw new IllegalArgumentException("Algorithm cannot be null");
        }
        
        if (!available) {
            throw new PQCException("BouncyCastle PQC provider is not available");
        }
        
        if (!supportsAlgorithm(algorithm)) {
            throw new IllegalArgumentException("Unsupported algorithm: " + algorithm);
        }
        
        final String bcAlgorithm = mapAlgorithmName(algorithm);
        
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Generating key pair for algorithm: " + algorithm + " (BC: " + bcAlgorithm + ")");
        }
        
        try {
            // Get KeyPairGenerator for the BouncyCastle algorithm
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance(bcAlgorithm, BCPQC_PROVIDER_NAME);
            
            // Initialize with appropriate parameter spec
            // Note: BouncyCastle's Dilithium implementation uses parameter specs
            // to configure key generation parameters
            DilithiumParameterSpec paramSpec = getParameterSpec(algorithm);
            keyGen.initialize(paramSpec);
            
            // Generate the key pair
            // Performance note: This is a computationally intensive operation
            // ML-DSA-65: ~10-50ms, ML-DSA-87: ~15-75ms depending on hardware
            KeyPair keyPair = keyGen.generateKeyPair();
            
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Successfully generated key pair for algorithm: " + algorithm);
            }
            
            return new PQCKeyPair(keyPair.getPublic(), keyPair.getPrivate(), algorithm);
            
        } catch (NoSuchAlgorithmException e) {
            String msg = "Algorithm not available: " + bcAlgorithm;
            Tr.error(tc, "CWWKS4201E: " + msg, e);
            throw new PQCException(msg, e);
        } catch (NoSuchProviderException e) {
            String msg = "BouncyCastle PQC provider not available";
            Tr.error(tc, "CWWKS4201E: " + msg, e);
            throw new PQCException(msg, e);
        } catch (Exception e) {
            String msg = "Failed to generate key pair for algorithm: " + algorithm;
            Tr.error(tc, "CWWKS4201E: " + msg, e);
            throw new PQCException(msg, e);
        }
    }
    
    @Override
    public byte[] encodePublicKey(PublicKey publicKey) throws PQCException {
        if (publicKey == null) {
            throw new IllegalArgumentException("Public key cannot be null");
        }
        
        if (!available) {
            throw new PQCException("BouncyCastle PQC provider is not available");
        }
        
        try {
            // Use X.509 encoding (SubjectPublicKeyInfo structure)
            // This is the standard encoding format for public keys
            byte[] encoded = publicKey.getEncoded();
            
            if (encoded == null) {
                throw new PQCException("Failed to encode public key: getEncoded() returned null");
            }
            
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Encoded public key, size: " + encoded.length + " bytes");
            }
            
            return encoded;
            
        } catch (Exception e) {
            String msg = "Failed to encode public key";
            Tr.error(tc, "CWWKS4202E: " + msg, e);
            throw new PQCException(msg, e);
        }
    }
    
    @Override
    public byte[] encodePrivateKey(PrivateKey privateKey) throws PQCException {
        if (privateKey == null) {
            throw new IllegalArgumentException("Private key cannot be null");
        }
        
        if (!available) {
            throw new PQCException("BouncyCastle PQC provider is not available");
        }
        
        try {
            // Use PKCS#8 encoding (PrivateKeyInfo structure)
            // This is the standard encoding format for private keys
            byte[] encoded = privateKey.getEncoded();
            
            if (encoded == null) {
                throw new PQCException("Failed to encode private key: getEncoded() returned null");
            }
            
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Encoded private key, size: " + encoded.length + " bytes");
            }
            
            return encoded;
            
        } catch (Exception e) {
            String msg = "Failed to encode private key";
            Tr.error(tc, "CWWKS4202E: " + msg, e);
            throw new PQCException(msg, e);
        }
    }
    
    @Override
    public PublicKey decodePublicKey(byte[] encoded, PQCAlgorithm algorithm) throws PQCException {
        if (encoded == null) {
            throw new IllegalArgumentException("Encoded key cannot be null");
        }
        if (algorithm == null) {
            throw new IllegalArgumentException("Algorithm cannot be null");
        }
        
        if (!available) {
            throw new PQCException("BouncyCastle PQC provider is not available");
        }
        
        if (!supportsAlgorithm(algorithm)) {
            throw new IllegalArgumentException("Unsupported algorithm: " + algorithm);
        }
        
        final String bcAlgorithm = mapAlgorithmName(algorithm);
        
        try {
            // Get KeyFactory for the BouncyCastle algorithm
            KeyFactory keyFactory = KeyFactory.getInstance(bcAlgorithm, BCPQC_PROVIDER_NAME);
            
            // Decode using X.509 format (SubjectPublicKeyInfo)
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encoded);
            PublicKey publicKey = keyFactory.generatePublic(keySpec);
            
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Successfully decoded public key for algorithm: " + algorithm);
            }
            
            return publicKey;
            
        } catch (NoSuchAlgorithmException e) {
            String msg = "Algorithm not available: " + bcAlgorithm;
            Tr.error(tc, "CWWKS4203E: " + msg, e);
            throw new PQCException(msg, e);
        } catch (NoSuchProviderException e) {
            String msg = "BouncyCastle PQC provider not available";
            Tr.error(tc, "CWWKS4203E: " + msg, e);
            throw new PQCException(msg, e);
        } catch (InvalidKeySpecException e) {
            String msg = "Invalid key encoding for algorithm: " + algorithm;
            Tr.error(tc, "CWWKS4203E: " + msg, e);
            throw new PQCException(msg, e);
        } catch (Exception e) {
            String msg = "Failed to decode public key for algorithm: " + algorithm;
            Tr.error(tc, "CWWKS4203E: " + msg, e);
            throw new PQCException(msg, e);
        }
    }
    
    @Override
    public PrivateKey decodePrivateKey(byte[] encoded, PQCAlgorithm algorithm) throws PQCException {
        if (encoded == null) {
            throw new IllegalArgumentException("Encoded key cannot be null");
        }
        if (algorithm == null) {
            throw new IllegalArgumentException("Algorithm cannot be null");
        }
        
        if (!available) {
            throw new PQCException("BouncyCastle PQC provider is not available");
        }
        
        if (!supportsAlgorithm(algorithm)) {
            throw new IllegalArgumentException("Unsupported algorithm: " + algorithm);
        }
        
        final String bcAlgorithm = mapAlgorithmName(algorithm);
        
        try {
            // Get KeyFactory for the BouncyCastle algorithm
            KeyFactory keyFactory = KeyFactory.getInstance(bcAlgorithm, BCPQC_PROVIDER_NAME);
            
            // Decode using PKCS#8 format (PrivateKeyInfo)
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
            PrivateKey privateKey = keyFactory.generatePrivate(keySpec);
            
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Successfully decoded private key for algorithm: " + algorithm);
            }
            
            return privateKey;
            
        } catch (NoSuchAlgorithmException e) {
            String msg = "Algorithm not available: " + bcAlgorithm;
            Tr.error(tc, "CWWKS4204E: " + msg, e);
            throw new PQCException(msg, e);
        } catch (NoSuchProviderException e) {
            String msg = "BouncyCastle PQC provider not available";
            Tr.error(tc, "CWWKS4204E: " + msg, e);
            throw new PQCException(msg, e);
        } catch (InvalidKeySpecException e) {
            String msg = "Invalid key encoding for algorithm: " + algorithm;
            Tr.error(tc, "CWWKS4204E: " + msg, e);
            throw new PQCException(msg, e);
        } catch (Exception e) {
            String msg = "Failed to decode private key for algorithm: " + algorithm;
            Tr.error(tc, "CWWKS4204E: " + msg, e);
            throw new PQCException(msg, e);
        }
    }
    
    // ========================================================================
    // Signature Operations
    // ========================================================================
    
    @Override
    public byte[] sign(byte[] data, PrivateKey privateKey, PQCAlgorithm algorithm) throws PQCException {
        if (data == null) {
            throw new IllegalArgumentException("Data cannot be null");
        }
        if (privateKey == null) {
            throw new IllegalArgumentException("Private key cannot be null");
        }
        if (algorithm == null) {
            throw new IllegalArgumentException("Algorithm cannot be null");
        }
        
        if (!available) {
            throw new PQCException("BouncyCastle PQC provider is not available");
        }
        
        if (!supportsAlgorithm(algorithm)) {
            throw new IllegalArgumentException("Unsupported algorithm: " + algorithm);
        }
        
        final String bcAlgorithm = mapAlgorithmName(algorithm);
        
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Signing data with algorithm: " + algorithm + " (BC: " + bcAlgorithm + "), data size: " + data.length + " bytes");
        }
        
        try {
            // Get Signature instance for the BouncyCastle algorithm
            Signature signature = Signature.getInstance(bcAlgorithm, BCPQC_PROVIDER_NAME);
            
            // Initialize for signing with the private key
            signature.initSign(privateKey);
            
            // Update with the data to be signed
            signature.update(data);
            
            // Generate the signature
            // Performance note: This is relatively fast compared to key generation
            // ML-DSA-65: ~1-5ms, ML-DSA-87: ~2-8ms depending on hardware
            byte[] signatureBytes = signature.sign();
            
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Successfully signed data, signature size: " + signatureBytes.length + " bytes");
            }
            
            return signatureBytes;
            
        } catch (NoSuchAlgorithmException e) {
            String msg = "Algorithm not available: " + bcAlgorithm;
            Tr.error(tc, "CWWKS4205E: " + msg, e);
            throw new PQCException(msg, e);
        } catch (NoSuchProviderException e) {
            String msg = "BouncyCastle PQC provider not available";
            Tr.error(tc, "CWWKS4205E: " + msg, e);
            throw new PQCException(msg, e);
        } catch (InvalidKeyException e) {
            String msg = "Invalid private key for algorithm: " + algorithm;
            Tr.error(tc, "CWWKS4205E: " + msg, e);
            throw new PQCException(msg, e);
        } catch (SignatureException e) {
            String msg = "Failed to generate signature";
            Tr.error(tc, "CWWKS4205E: " + msg, e);
            throw new PQCException(msg, e);
        } catch (Exception e) {
            String msg = "Failed to sign data with algorithm: " + algorithm;
            Tr.error(tc, "CWWKS4205E: " + msg, e);
            throw new PQCException(msg, e);
        }
    }
    
    @Override
    public boolean verify(byte[] data, byte[] signature, PublicKey publicKey, PQCAlgorithm algorithm) throws PQCException {
        if (data == null) {
            throw new IllegalArgumentException("Data cannot be null");
        }
        if (signature == null) {
            throw new IllegalArgumentException("Signature cannot be null");
        }
        if (publicKey == null) {
            throw new IllegalArgumentException("Public key cannot be null");
        }
        if (algorithm == null) {
            throw new IllegalArgumentException("Algorithm cannot be null");
        }
        
        if (!available) {
            throw new PQCException("BouncyCastle PQC provider is not available");
        }
        
        if (!supportsAlgorithm(algorithm)) {
            throw new IllegalArgumentException("Unsupported algorithm: " + algorithm);
        }
        
        final String bcAlgorithm = mapAlgorithmName(algorithm);
        
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Verifying signature with algorithm: " + algorithm + " (BC: " + bcAlgorithm + "), data size: " + data.length + " bytes, signature size: " + signature.length + " bytes");
        }
        
        try {
            // Get Signature instance for the BouncyCastle algorithm
            Signature sig = Signature.getInstance(bcAlgorithm, BCPQC_PROVIDER_NAME);
            
            // Initialize for verification with the public key
            sig.initVerify(publicKey);
            
            // Update with the data that was signed
            sig.update(data);
            
            // Verify the signature
            // Performance note: Verification is faster than signing
            // ML-DSA-65: ~0.5-2ms, ML-DSA-87: ~1-3ms depending on hardware
            boolean valid = sig.verify(signature);
            
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Signature verification result: " + (valid ? "VALID" : "INVALID"));
            }
            
            return valid;
            
        } catch (NoSuchAlgorithmException e) {
            String msg = "Algorithm not available: " + bcAlgorithm;
            Tr.error(tc, "CWWKS4206E: " + msg, e);
            throw new PQCException(msg, e);
        } catch (NoSuchProviderException e) {
            String msg = "BouncyCastle PQC provider not available";
            Tr.error(tc, "CWWKS4206E: " + msg, e);
            throw new PQCException(msg, e);
        } catch (InvalidKeyException e) {
            String msg = "Invalid public key for algorithm: " + algorithm;
            Tr.error(tc, "CWWKS4206E: " + msg, e);
            throw new PQCException(msg, e);
        } catch (SignatureException e) {
            // SignatureException during verify typically means invalid signature
            // Return false instead of throwing exception
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Signature verification failed: " + e.getMessage());
            }
            return false;
        } catch (Exception e) {
            String msg = "Failed to verify signature with algorithm: " + algorithm;
            Tr.error(tc, "CWWKS4206E: " + msg, e);
            throw new PQCException(msg, e);
        }
    }
    
    // ========================================================================
    // Performance Characteristics
    // ========================================================================
    
    @Override
    public int getSignatureSize(PQCAlgorithm algorithm) {
        if (algorithm == null) {
            throw new IllegalArgumentException("Algorithm cannot be null");
        }
        
        switch (algorithm) {
            case ML_DSA_65:
                return 3293; // Dilithium3 signature size
            case ML_DSA_87:
                return 4595; // Dilithium5 signature size
            default:
                throw new IllegalArgumentException("Unsupported algorithm: " + algorithm);
        }
    }
    
    @Override
    public int getPublicKeySize(PQCAlgorithm algorithm) {
        if (algorithm == null) {
            throw new IllegalArgumentException("Algorithm cannot be null");
        }
        
        switch (algorithm) {
            case ML_DSA_65:
                return 1952; // Dilithium3 public key size
            case ML_DSA_87:
                return 2592; // Dilithium5 public key size
            default:
                throw new IllegalArgumentException("Unsupported algorithm: " + algorithm);
        }
    }
    
    @Override
    public int getPrivateKeySize(PQCAlgorithm algorithm) {
        if (algorithm == null) {
            throw new IllegalArgumentException("Algorithm cannot be null");
        }
        
        switch (algorithm) {
            case ML_DSA_65:
                return 4000; // Dilithium3 private key size
            case ML_DSA_87:
                return 4864; // Dilithium5 private key size
            default:
                throw new IllegalArgumentException("Unsupported algorithm: " + algorithm);
        }
    }
    
    // ========================================================================
    // FIPS Compliance
    // ========================================================================
    
    @Override
    public boolean isFIPSCompliant() {
        // BouncyCastle PQC is NOT FIPS 140-3 validated
        // This provider should not be used in FIPS-required environments
        return false;
    }
}

// Made with Bob
