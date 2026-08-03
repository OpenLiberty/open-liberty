/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.security.token.ltpa.pqc;

import java.lang.reflect.Method;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.AlgorithmParameterSpec;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;

/**
 * Runtime support for Post-Quantum Cryptography (PQC) algorithms.
 * 
 * This class provides a compatibility layer that:
 * - Compiles with Java 17 (using reflection)
 * - Runs with Java 26+ (using native ML-KEM support via JEP 478)
 * - Gracefully degrades on older Java versions
 * 
 * The PQC features are only available when running on Java 26 or later.
 */
public class PQCRuntimeSupport {
    
    private static final TraceComponent tc = Tr.register(PQCRuntimeSupport.class);
    
    private static final boolean IS_JAVA_26_OR_LATER;
    private static final Class<?> KEM_CLASS;
    private static final String PQC_ENABLED_PROPERTY = "com.ibm.ws.security.pqc.enabled";
    
    static {
        boolean java26Available = false;
        Class<?> kemClass = null;
        
        try {
            // Check if Java 26 ML-KEM support is available
            // JEP 478: Key Encapsulation Mechanism API
            kemClass = Class.forName("javax.crypto.KEM");
            
            // Verify ML-KEM algorithm is supported
            KeyPairGenerator.getInstance("ML-KEM");
            
            java26Available = true;
            
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Java 26+ ML-KEM support detected");
            }
        } catch (ClassNotFoundException e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Java 26+ ML-KEM support not available (KEM class not found): " + e.getMessage());
            }
        } catch (NoSuchAlgorithmException e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Java 26+ ML-KEM support not available (ML-KEM algorithm not supported): " + e.getMessage());
            }
        }
        
        IS_JAVA_26_OR_LATER = java26Available;
        KEM_CLASS = kemClass;
    }
    
    /**
     * Check if PQC (ML-KEM) support is available at runtime.
     *
     * This method checks the system property 'com.ibm.ws.security.pqc.enabled'
     * which can be set via JVM options (e.g., -Dcom.ibm.ws.security.pqc.enabled=true).
     *
     * Behavior:
     * - If property is set to "true": Returns true only if Java 26+ with ML-KEM is available
     * - If property is set to "false": Returns false (PQC disabled)
     * - If property is not set: Auto-detect based on Java version (default behavior)
     *
     * @return true if PQC support is enabled and available
     */
    public static boolean isPQCSupported() {
        String pqcEnabled = System.getProperty(PQC_ENABLED_PROPERTY);
        
        if (pqcEnabled != null) {
            boolean enabled = Boolean.parseBoolean(pqcEnabled);
            
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "PQC mode explicitly set via system property: " +
                        PQC_ENABLED_PROPERTY + "=" + pqcEnabled);
            }
            
            // If explicitly enabled, still require Java 26+ runtime support
            if (enabled) {
                if (!IS_JAVA_26_OR_LATER) {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "PQC enabled via property but Java 26+ not available. " +
                                "Current Java version: " + getJavaVersion());
                    }
                    return false;
                }
                return true;
            }
            
            // Explicitly disabled
            return false;
        }
        
        // Default: auto-detect based on Java version
        if (tc.isDebugEnabled() && IS_JAVA_26_OR_LATER) {
            Tr.debug(tc, "PQC auto-detected as available (Java 26+)");
        }
        
        return IS_JAVA_26_OR_LATER;
    }
    
    /**
     * Check if both ML-KEM and ML-DSA support are available at runtime.
     * This is an alias for isPQCSupported() for backward compatibility.
     *
     * @return true if running on Java 26+ with PQC support
     */
    public static boolean isPQCAvailable() {
        return isPQCSupported();
    }
    
    /**
     * Check if ML-KEM support is available at runtime.
     *
     * @return true if running on Java 26+ with ML-KEM support
     */
    public static boolean isMLKEMAvailable() {
        return IS_JAVA_26_OR_LATER;
    }
    
    /**
     * Check if ML-DSA support is available at runtime.
     *
     * @return true if running on Java 26+ with ML-DSA support
     */
    public static boolean isMLDSAAvailable() {
        return IS_JAVA_26_OR_LATER;
    }
    
    /**
     * Get the Java version string for diagnostic purposes.
     * 
     * @return Java version (e.g., "17.0.12", "26.0.0")
     */
    public static String getJavaVersion() {
        return System.getProperty("java.version");
    }
    
    /**
     * Get ML-KEM parameter spec for the specified algorithm.
     *
     * @param algorithm ML-KEM algorithm (ML-KEM-512, ML-KEM-768, ML-KEM-1024)
     * @return AlgorithmParameterSpec for ML-KEM
     * @throws UnsupportedOperationException if Java 26+ is not available
     * @throws IllegalArgumentException if algorithm is invalid
     */
    public static AlgorithmParameterSpec getMLKEMParameterSpec(String algorithm) {
        if (!IS_JAVA_26_OR_LATER) {
            throw new UnsupportedOperationException(
                "ML-KEM requires Java 26 or later. Current version: " + getJavaVersion());
        }
        
        try {
            // Access MLKEMParameterSpec constants via reflection
            // MLKEMParameterSpec.ml_kem_512, ml_kem_768, ml_kem_1024
            Class<?> paramSpecClass = Class.forName("java.security.spec.MLKEMParameterSpec");
            String fieldName = algorithm.toLowerCase().replace("-", "_");
            Object paramSpec = paramSpecClass.getField(fieldName).get(null);
            return (AlgorithmParameterSpec) paramSpec;
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid ML-KEM algorithm: " + algorithm, e);
        }
    }
    
    /**
     * Generate ML-KEM key pair.
     *
     * @param algorithm ML-KEM algorithm (ML-KEM-512, ML-KEM-768, ML-KEM-1024)
     * @return KeyPair containing ML-KEM public and private keys
     * @throws Exception if key generation fails
     */
    public static KeyPair generateMLKEMKeyPair(String algorithm) throws Exception {
        if (!IS_JAVA_26_OR_LATER) {
            throw new UnsupportedOperationException(
                "ML-KEM requires Java 26 or later. Current version: " + getJavaVersion());
        }
        
        AlgorithmParameterSpec paramSpec = getMLKEMParameterSpec(algorithm);
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("ML-KEM");
        kpg.initialize(paramSpec);
        return kpg.generateKeyPair();
    }
    
    /**
     * Generate ML-KEM key pair using MLKEMAlgorithmType enum.
     *
     * @param algorithmType ML-KEM algorithm type
     * @return KeyPair containing ML-KEM public and private keys
     * @throws Exception if key generation fails
     */
    public static KeyPair generateMLKEMKeyPair(MLKEMAlgorithmType algorithmType) throws Exception {
        return generateMLKEMKeyPair(algorithmType.getAlgorithmName());
    }
    
    /**
     * Perform ML-KEM encapsulation to generate shared secret.
     * 
     * @param publicKey ML-KEM public key
     * @return Object containing shared secret and encapsulation (SecretKeyWithEncapsulation)
     * @throws Exception if encapsulation fails
     */
    public static Object encapsulate(PublicKey publicKey) throws Exception {
        if (!IS_JAVA_26_OR_LATER) {
            throw new UnsupportedOperationException(
                "ML-KEM requires Java 26 or later. Current version: " + getJavaVersion());
        }
        
        // Use reflection to call KEM.getInstance("ML-KEM").newEncapsulator(publicKey).encapsulate()
        Class<?> kemClass = Class.forName("javax.crypto.KEM");
        Method getInstance = kemClass.getMethod("getInstance", String.class);
        Object kem = getInstance.invoke(null, "ML-KEM");
        
        Method newEncapsulator = kemClass.getMethod("newEncapsulator", PublicKey.class);
        Object encapsulator = newEncapsulator.invoke(kem, publicKey);
        
        Method encapsulate = encapsulator.getClass().getMethod("encapsulate");
        return encapsulate.invoke(encapsulator);
    }
    
    /**
     * Perform ML-KEM decapsulation to recover shared secret.
     * 
     * @param privateKey ML-KEM private key
     * @param encapsulation Encapsulation bytes from encapsulate()
     * @return SecretKey containing the shared secret
     * @throws Exception if decapsulation fails
     */
    public static SecretKey decapsulate(PrivateKey privateKey, byte[] encapsulation) throws Exception {
        if (!IS_JAVA_26_OR_LATER) {
            throw new UnsupportedOperationException(
                "ML-KEM requires Java 26 or later. Current version: " + getJavaVersion());
        }
        
        // Use reflection to call KEM.getInstance("ML-KEM").newDecapsulator(privateKey).decapsulate(encapsulation)
        Class<?> kemClass = Class.forName("javax.crypto.KEM");
        Method getInstance = kemClass.getMethod("getInstance", String.class);
        Object kem = getInstance.invoke(null, "ML-KEM");
        
        Method newDecapsulator = kemClass.getMethod("newDecapsulator", PrivateKey.class);
        Object decapsulator = newDecapsulator.invoke(kem, privateKey);
        
        Method decapsulate = decapsulator.getClass().getMethod("decapsulate", byte[].class);
        return (SecretKey) decapsulate.invoke(decapsulator, encapsulation);
    }
    
    /**
     * Extract shared secret from SecretKeyWithEncapsulation.
     * 
     * @param secretKeyWithEncap Object returned from encapsulate()
     * @return SecretKey containing the shared secret
     * @throws Exception if extraction fails
     */
    public static SecretKey extractSharedSecret(Object secretKeyWithEncap) throws Exception {
        if (!IS_JAVA_26_OR_LATER) {
            throw new UnsupportedOperationException(
                "ML-KEM requires Java 26 or later. Current version: " + getJavaVersion());
        }
        
        // KEM.Encapsulated is a record with key() and encapsulation() methods
        // Use reflection to call key() method to get the SecretKey
        Method keyMethod = secretKeyWithEncap.getClass().getMethod("key");
        return (SecretKey) keyMethod.invoke(secretKeyWithEncap);
    }
    
    /**
     * Extract encapsulation bytes from SecretKeyWithEncapsulation.
     * 
     * @param secretKeyWithEncap Object returned from encapsulate()
     * @return Encapsulation bytes
     * @throws Exception if extraction fails
     */
    public static byte[] extractEncapsulation(Object secretKeyWithEncap) throws Exception {
        if (!IS_JAVA_26_OR_LATER) {
            throw new UnsupportedOperationException(
                "ML-KEM requires Java 26 or later. Current version: " + getJavaVersion());
        }
        
        // Use reflection to call secretKeyWithEncap.encapsulation()
        Method encapsulation = secretKeyWithEncap.getClass().getMethod("encapsulation");
        return (byte[]) encapsulation.invoke(secretKeyWithEncap);
    }
    
    /**
     * Validate ML-KEM key sizes match expected values.
     *
     * @param publicKeyBytes public key bytes
     * @param privateKeyBytes private key bytes
     * @param algorithm ML-KEM algorithm type
     * @return true if key sizes are valid
     */
    public static boolean validateKeySizes(byte[] publicKeyBytes,
                                          @Sensitive byte[] privateKeyBytes,
                                          MLKEMAlgorithmType algorithm) {
        
        if (publicKeyBytes == null || privateKeyBytes == null) {
            return false;
        }

        // Note: Encoded keys include ASN.1 overhead, so we check minimum sizes
        int minPublicSize = algorithm.getPublicKeySize();
        int minPrivateSize = algorithm.getPrivateKeySize();

        boolean valid = publicKeyBytes.length >= minPublicSize &&
                       privateKeyBytes.length >= minPrivateSize;

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "ML-KEM key size validation: " + algorithm.getAlgorithmName() +
                    ", public=" + publicKeyBytes.length + " (min " + minPublicSize + ")" +
                    ", private=" + privateKeyBytes.length + " (min " + minPrivateSize + ")" +
                    ", valid=" + valid);
        }

        return valid;
    }
    
    /**
     * Get ML-KEM provider information.
     *
     * @return provider name and version, or null if ML-KEM not available
     */
    public static String getProviderInfo() {
        if (!IS_JAVA_26_OR_LATER) {
            return null;
        }

        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("ML-KEM");
            return kpg.getProvider().getName() + " " + kpg.getProvider().getVersion();
        } catch (Exception e) {
            return null;
        }
    }
}

// Made with Bob
