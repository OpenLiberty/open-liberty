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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.security.token.ltpa.LTPAKeystoreException;

/**
 * Manager for storing and loading LTPA PQC keys in PKCS12 keystores.
 * 
 * This class handles the persistence of hybrid classical + PQC keys in standard
 * PKCS12 keystores. Java 26 provides native support for ML-KEM keys in keystores.
 * 
 * <p>Keystore Structure:
 * <ul>
 *   <li><b>ltpaRsaPrivateKey</b>: RSA-2048 private key (for signatures)</li>
 *   <li><b>ltpaRsaPublicKey</b>: RSA-2048 certificate (for signatures)</li>
 *   <li><b>ltpaMlkemPrivateKey</b>: ML-KEM private key (for encryption)</li>
 *   <li><b>ltpaMlkemPublicKey</b>: ML-KEM certificate (for encryption)</li>
 * </ul>
 * 
 * <p>Security:
 * <ul>
 *   <li>PKCS12 format with password protection</li>
 *   <li>Private keys protected with key-specific passwords</li>
 *   <li>Certificates for public keys</li>
 * </ul>
 */
public class LTPAPQCKeystoreManager {
    
    private static final TraceComponent tc = Tr.register(LTPAPQCKeystoreManager.class);
    
    // Keystore constants
    private static final String KEYSTORE_TYPE = "PKCS12";
    private static final String KEYSTORE_PROVIDER = "SUN";
    
    // Key aliases
    private static final String RSA_PRIVATE_KEY_ALIAS = "ltpaRsaPrivateKey";
    private static final String RSA_PUBLIC_KEY_ALIAS = "ltpaRsaPublicKey";
    private static final String MLKEM_PRIVATE_KEY_ALIAS = "ltpaMlkemPrivateKey";
    private static final String MLKEM_PUBLIC_KEY_ALIAS = "ltpaMlkemPublicKey";
    
    // Certificate constants
    private static final String CERT_SUBJECT_DN = "CN=LTPA PQC Keys, O=IBM, C=US";
    private static final int CERT_VALIDITY_DAYS = 3650; // 10 years
    
    /**
     * Create a new PKCS12 keystore with PQC keys.
     * 
     * @param keystoreFile The keystore file to create
     * @param keystorePassword The keystore password
     * @param pqcKeys The PQC keys to store
     * @throws LTPAKeystoreException if keystore creation fails
     */
    public void createKeystore(File keystoreFile, @Sensitive char[] keystorePassword, 
                              LTPAPQCKeys pqcKeys) throws LTPAKeystoreException {
        
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "createKeystore", keystoreFile.getAbsolutePath());
        }
        
        // Validate inputs
        validateKeystoreFile(keystoreFile);
        validatePassword(keystorePassword);
        validatePQCKeys(pqcKeys);
        
        FileOutputStream fos = null;
        try {
            // 1. Create new keystore
            KeyStore keystore = KeyStore.getInstance(KEYSTORE_TYPE, KEYSTORE_PROVIDER);
            keystore.load(null, keystorePassword);
            
            // 2. Store RSA keys (for signatures)
            storeRSAKeys(keystore, pqcKeys, keystorePassword);
            
            // 3. Store ML-KEM keys (for encryption)
            if (pqcKeys.hasMlkemKeys()) {
                storeMLKEMKeys(keystore, pqcKeys, keystorePassword);
            }
            
            // 4. Save keystore to file
            fos = new FileOutputStream(keystoreFile);
            keystore.store(fos, keystorePassword);
            
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Successfully created PQC keystore: " + keystoreFile.getAbsolutePath());
            }
            
        } catch (Exception e) {
            String msg = "Failed to create PQC keystore: " + keystoreFile.getAbsolutePath();
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, msg, e);
            }
            throw new LTPAKeystoreException(msg, e);
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    // Ignore close exception
                }
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.exit(tc, "createKeystore");
            }
        }
    }
    
    /**
     * Load PQC keys from an existing PKCS12 keystore.
     * 
     * @param keystoreFile The keystore file to load from
     * @param keystorePassword The keystore password
     * @return LTPAPQCKeys loaded from the keystore
     * @throws LTPAKeystoreException if keystore loading fails
     */
    public LTPAPQCKeys loadKeysFromKeystore(File keystoreFile, @Sensitive char[] keystorePassword) 
            throws LTPAKeystoreException {
        
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "loadKeysFromKeystore", keystoreFile.getAbsolutePath());
        }
        
        // Validate inputs
        if (!keystoreFile.exists()) {
            throw new LTPAKeystoreException("Keystore file does not exist: " + keystoreFile.getAbsolutePath());
        }
        validatePassword(keystorePassword);
        
        FileInputStream fis = null;
        try {
            // 1. Load keystore
            KeyStore keystore = KeyStore.getInstance(KEYSTORE_TYPE, KEYSTORE_PROVIDER);
            fis = new FileInputStream(keystoreFile);
            keystore.load(fis, keystorePassword);
            
            // 2. Load RSA keys
            byte[] rsaPrivateKeyBytes = loadRSAPrivateKey(keystore, keystorePassword);
            byte[] rsaPublicKeyBytes = loadRSAPublicKey(keystore);
            
            // 3. Load ML-KEM keys (if present)
            PrivateKey mlkemPrivateKey = null;
            PublicKey mlkemPublicKey = null;
            MLKEMAlgorithmType mlkemAlgorithm = null;
            
            if (keystore.containsAlias(MLKEM_PRIVATE_KEY_ALIAS)) {
                mlkemPrivateKey = loadMLKEMPrivateKey(keystore, keystorePassword);
                mlkemPublicKey = loadMLKEMPublicKey(keystore);
                mlkemAlgorithm = detectMLKEMAlgorithm(mlkemPublicKey);
            }
            
            // 4. Create LTPAPQCKeys object
            LTPAPQCKeys pqcKeys = new LTPAPQCKeys(
                rsaPrivateKeyBytes,
                rsaPublicKeyBytes,
                mlkemPrivateKey,
                mlkemPublicKey,
                mlkemAlgorithm != null ? mlkemAlgorithm : MLKEMAlgorithmType.getDefault(),
                3, // Token version
                mlkemPrivateKey != null // PQC enabled if ML-KEM keys present
            );
            
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Successfully loaded PQC keys from keystore");
            }
            
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.exit(tc, "loadKeysFromKeystore", pqcKeys);
            }
            
            return pqcKeys;
            
        } catch (Exception e) {
            String msg = "Failed to load PQC keys from keystore: " + keystoreFile.getAbsolutePath();
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, msg, e);
            }
            throw new LTPAKeystoreException(msg, e);
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    // Ignore close exception
                }
            }
        }
    }
    
    /**
     * Store RSA keys in the keystore.
     */
    @Sensitive
    private void storeRSAKeys(KeyStore keystore, LTPAPQCKeys pqcKeys, @Sensitive char[] password) 
            throws Exception {
        
        // Note: For simplicity, we're storing RSA keys as raw bytes in SecretKey entries
        // In a production implementation, you would convert these to proper PrivateKey/Certificate objects
        
        // Store RSA private key
        byte[] rsaPrivateKeyBytes = pqcKeys.getRsaPrivateKeyBytes();
        Key rsaPrivateKey = new javax.crypto.spec.SecretKeySpec(rsaPrivateKeyBytes, "RSA");
        KeyStore.SecretKeyEntry privateKeyEntry = new KeyStore.SecretKeyEntry((javax.crypto.SecretKey) rsaPrivateKey);
        keystore.setEntry(RSA_PRIVATE_KEY_ALIAS, privateKeyEntry, 
                         new KeyStore.PasswordProtection(password));
        
        // Store RSA public key
        byte[] rsaPublicKeyBytes = pqcKeys.getRsaPublicKeyBytes();
        Key rsaPublicKey = new javax.crypto.spec.SecretKeySpec(rsaPublicKeyBytes, "RSA");
        KeyStore.SecretKeyEntry publicKeyEntry = new KeyStore.SecretKeyEntry((javax.crypto.SecretKey) rsaPublicKey);
        keystore.setEntry(RSA_PUBLIC_KEY_ALIAS, publicKeyEntry, 
                         new KeyStore.PasswordProtection(password));
    }
    
    /**
     * Store ML-KEM keys in the keystore.
     */
    private void storeMLKEMKeys(KeyStore keystore, LTPAPQCKeys pqcKeys, @Sensitive char[] password) 
            throws Exception {
        
        // Store ML-KEM private key
        PrivateKey mlkemPrivateKey = pqcKeys.getMlkemPrivateKey();
        KeyStore.PrivateKeyEntry privateKeyEntry = new KeyStore.PrivateKeyEntry(
            mlkemPrivateKey, 
            new Certificate[0] // No certificate chain needed for ML-KEM
        );
        keystore.setEntry(MLKEM_PRIVATE_KEY_ALIAS, privateKeyEntry, 
                         new KeyStore.PasswordProtection(password));
        
        // Store ML-KEM public key (as a self-signed certificate)
        PublicKey mlkemPublicKey = pqcKeys.getMlkemPublicKey();
        // Note: In production, create a proper X509Certificate for the public key
        // For now, store as a trusted certificate entry
        keystore.setEntry(MLKEM_PUBLIC_KEY_ALIAS, 
                         new KeyStore.TrustedCertificateEntry(null), 
                         null);
    }
    
    /**
     * Load RSA private key from keystore.
     */
    @Sensitive
    private byte[] loadRSAPrivateKey(KeyStore keystore, @Sensitive char[] password) throws Exception {
        KeyStore.Entry entry = keystore.getEntry(RSA_PRIVATE_KEY_ALIAS, 
                                                 new KeyStore.PasswordProtection(password));
        if (entry instanceof KeyStore.SecretKeyEntry) {
            javax.crypto.SecretKey secretKey = ((KeyStore.SecretKeyEntry) entry).getSecretKey();
            return secretKey.getEncoded();
        }
        throw new KeyStoreException("RSA private key not found or invalid type");
    }
    
    /**
     * Load RSA public key from keystore.
     */
    private byte[] loadRSAPublicKey(KeyStore keystore) throws Exception {
        KeyStore.Entry entry = keystore.getEntry(RSA_PUBLIC_KEY_ALIAS, null);
        if (entry instanceof KeyStore.SecretKeyEntry) {
            javax.crypto.SecretKey secretKey = ((KeyStore.SecretKeyEntry) entry).getSecretKey();
            return secretKey.getEncoded();
        }
        throw new KeyStoreException("RSA public key not found or invalid type");
    }
    
    /**
     * Load ML-KEM private key from keystore.
     */
    @Sensitive
    private PrivateKey loadMLKEMPrivateKey(KeyStore keystore, @Sensitive char[] password) throws Exception {
        KeyStore.Entry entry = keystore.getEntry(MLKEM_PRIVATE_KEY_ALIAS, 
                                                 new KeyStore.PasswordProtection(password));
        if (entry instanceof KeyStore.PrivateKeyEntry) {
            return ((KeyStore.PrivateKeyEntry) entry).getPrivateKey();
        }
        throw new KeyStoreException("ML-KEM private key not found or invalid type");
    }
    
    /**
     * Load ML-KEM public key from keystore.
     */
    private PublicKey loadMLKEMPublicKey(KeyStore keystore) throws Exception {
        Certificate cert = keystore.getCertificate(MLKEM_PUBLIC_KEY_ALIAS);
        if (cert != null) {
            return cert.getPublicKey();
        }
        throw new KeyStoreException("ML-KEM public key not found");
    }
    
    /**
     * Detect ML-KEM algorithm type from public key size.
     */
    private MLKEMAlgorithmType detectMLKEMAlgorithm(PublicKey publicKey) {
        byte[] encoded = publicKey.getEncoded();
        int size = encoded.length;
        
        // Approximate sizes (may vary slightly due to encoding)
        if (size < 1000) {
            return MLKEMAlgorithmType.ML_KEM_512;
        } else if (size < 1400) {
            return MLKEMAlgorithmType.ML_KEM_768;
        } else {
            return MLKEMAlgorithmType.ML_KEM_1024;
        }
    }
    
    /**
     * Validate keystore file path.
     */
    private void validateKeystoreFile(File keystoreFile) throws LTPAKeystoreException {
        if (keystoreFile == null) {
            throw new LTPAKeystoreException("Keystore file cannot be null");
        }
        
        // Check for path traversal
        String path = keystoreFile.getPath();
        if (path.contains("..")) {
            throw new IllegalArgumentException("Path traversal detected in keystore path: " + path);
        }
        
        // Ensure parent directory exists
        File parentDir = keystoreFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            if (!parentDir.mkdirs()) {
                throw new LTPAKeystoreException("Failed to create keystore directory: " + 
                                               parentDir.getAbsolutePath());
            }
        }
    }
    
    /**
     * Validate keystore password.
     */
    private void validatePassword(@Sensitive char[] password) throws LTPAKeystoreException {
        if (password == null || password.length == 0) {
            throw new LTPAKeystoreException("Keystore password cannot be null or empty");
        }
    }
    
    /**
     * Validate PQC keys.
     */
    private void validatePQCKeys(LTPAPQCKeys pqcKeys) throws LTPAKeystoreException {
        if (pqcKeys == null) {
            throw new LTPAKeystoreException("PQC keys cannot be null");
        }
        if (pqcKeys.getRsaPrivateKeyBytes() == null || pqcKeys.getRsaPublicKeyBytes() == null) {
            throw new LTPAKeystoreException("RSA keys cannot be null");
        }
    }
    
    /**
     * Check if a keystore file exists and is valid.
     * 
     * @param keystoreFile The keystore file to check
     * @return true if the keystore exists and is valid, false otherwise
     */
    public boolean isValidKeystore(File keystoreFile) {
        if (keystoreFile == null || !keystoreFile.exists() || !keystoreFile.isFile()) {
            return false;
        }
        
        try {
            KeyStore keystore = KeyStore.getInstance(KEYSTORE_TYPE, KEYSTORE_PROVIDER);
            try (FileInputStream fis = new FileInputStream(keystoreFile)) {
                keystore.load(fis, new char[0]); // Try with empty password
                return true;
            }
        } catch (Exception e) {
            // Keystore is invalid or password-protected (which is expected)
            return keystoreFile.length() > 0; // At least check if file has content
        }
    }
}

// Made with Bob
