/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.security.audit.pqc;

import java.lang.reflect.Method;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.AlgorithmParameterSpec;
import javax.crypto.SecretKey;

import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;

/**
 * Runtime support for Post-Quantum Cryptography (PQC) algorithms for Audit.
 * 
 * This class provides a compatibility layer that:
 * - Compiles with Java 17 (using reflection)
 * - Runs with Java 26+ (using native ML-KEM support via JEP 478)
 * - Gracefully degrades on older Java versions
 * 
 * The PQC features are only available when running on Java 26 or later.
 */
public class AuditPQCRuntimeSupport {
    
    private static final TraceComponent tc = Tr.register(AuditPQCRuntimeSupport.class, null, "com.ibm.ejs.resources.security");
    
    private static final boolean IS_JAVA_26_OR_LATER;
    private static final Class<?> KEM_CLASS;
    
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
     * @return true if running on Java 26+ with ML-KEM support
     */
    public static boolean isPQCSupported() {
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
}

// Made with Bob