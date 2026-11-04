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

import java.nio.ByteBuffer;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;

/**
 * Cryptographic operations for LTPA Post-Quantum Cryptography (PQC) using ML-KEM.
 * 
 * This class provides quantum-resistant encryption and decryption for LTPA tokens using:
 * <ul>
 *   <li><b>ML-KEM</b>: Key Encapsulation Mechanism for quantum-resistant key exchange</li>
 *   <li><b>AES-256-GCM</b>: Authenticated encryption for token data</li>
 *   <li><b>HKDF</b>: Key derivation for additional security</li>
 * </ul>
 * 
 * <p>Encryption Process:
 * <ol>
 *   <li>Generate ephemeral shared secret using ML-KEM encapsulation</li>
 *   <li>Derive AES-256 key from shared secret using HKDF</li>
 *   <li>Encrypt token data with AES-256-GCM</li>
 *   <li>Return: encapsulation || IV || ciphertext || auth-tag</li>
 * </ol>
 * 
 * <p>Decryption Process:
 * <ol>
 *   <li>Extract encapsulation, IV, and ciphertext from token</li>
 *   <li>Decapsulate shared secret using ML-KEM private key</li>
 *   <li>Derive AES-256 key from shared secret</li>
 *   <li>Decrypt and authenticate ciphertext with AES-256-GCM</li>
 * </ol>
 */
public class LTPAPQCCrypto {
    
    private static final TraceComponent tc = Tr.register(LTPAPQCCrypto.class);
    
    // Algorithm constants
    private static final String ML_KEM_ALGORITHM = "ML-KEM";
    private static final String SUNJCE_PROVIDER = "SunJCE";
    private static final String AES_GCM_CIPHER = "AES/GCM/NoPadding";
    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final String AES_ALGORITHM = "AES";
    
    // Cryptographic parameters
    private static final int GCM_IV_LENGTH = 12;  // 96 bits (recommended for GCM)
    private static final int GCM_TAG_LENGTH = 128; // 128 bits (16 bytes)
    private static final int AES_KEY_LENGTH = 32;  // 256 bits (32 bytes)
    
    // HKDF info string for key derivation
    private static final byte[] HKDF_INFO = "LTPA-PQC-v3".getBytes();
    
    /**
     * Encrypt token data using ML-KEM key encapsulation and AES-256-GCM.
     * 
     * @param tokenData The token data to encrypt
     * @param recipientPublicKey The recipient's ML-KEM public key
     * @param mlkemAlgorithm The ML-KEM algorithm type
     * @return Encrypted token: encapsulation || IV || ciphertext
     * @throws Exception if encryption fails
     */
    @Sensitive
    public static byte[] encryptToken(@Sensitive byte[] tokenData, 
                                     PublicKey recipientPublicKey,
                                     MLKEMAlgorithmType mlkemAlgorithm) throws Exception {
        
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "encryptToken", "dataLength=" + tokenData.length + ", algorithm=" + mlkemAlgorithm);
        }
        
        long startTime = System.currentTimeMillis();
        
        try {
            // 1. Generate shared secret using ML-KEM encapsulation
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Performing ML-KEM encapsulation");
            }
            
            // Use PQCRuntimeSupport for Java 26 ML-KEM operations
            Object secretKeyWithEncap = PQCRuntimeSupport.encapsulate(recipientPublicKey);
            byte[] encapsulation = PQCRuntimeSupport.extractEncapsulation(secretKeyWithEncap);
            SecretKey sharedSecret = PQCRuntimeSupport.extractSharedSecret(secretKeyWithEncap);
            
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Encapsulation size: " + encapsulation.length + " bytes");
            }
            
            // 2. Derive AES-256 key using HKDF
            SecretKey aesKey = deriveAESKey(sharedSecret);
            
            // 3. Generate random IV for GCM
            byte[] iv = generateIV();
            
            // 4. Encrypt token data with AES-256-GCM
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Encrypting with AES-256-GCM");
            }
            
            Cipher cipher = Cipher.getInstance(AES_GCM_CIPHER);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, aesKey, gcmSpec);
            byte[] ciphertext = cipher.doFinal(tokenData);
            
            // 5. Combine: encapsulation || IV || ciphertext (includes auth tag)
            byte[] encryptedToken = concatenate(encapsulation, iv, ciphertext);
            
            long elapsedTime = System.currentTimeMillis() - startTime;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Encryption completed in " + elapsedTime + "ms, output size: " + 
                        encryptedToken.length + " bytes");
            }
            
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.exit(tc, "encryptToken");
            }
            
            return encryptedToken;
            
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Error encrypting token", e);
            }
            throw e;
        }
    }
    
    /**
     * Decrypt token data using ML-KEM key decapsulation and AES-256-GCM.
     * 
     * @param encryptedToken The encrypted token (encapsulation || IV || ciphertext)
     * @param recipientPrivateKey The recipient's ML-KEM private key
     * @param mlkemAlgorithm The ML-KEM algorithm type
     * @return Decrypted token data
     * @throws Exception if decryption fails
     */
    @Sensitive
    public static byte[] decryptToken(@Sensitive byte[] encryptedToken,
                                     @Sensitive PrivateKey recipientPrivateKey,
                                     MLKEMAlgorithmType mlkemAlgorithm) throws Exception {
        
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "decryptToken", "tokenLength=" + encryptedToken.length + ", algorithm=" + mlkemAlgorithm);
        }
        
        long startTime = System.currentTimeMillis();
        
        try {
            // 1. Extract components from encrypted token
            int encapsulationSize = mlkemAlgorithm.getCiphertextSize();
            
            if (encryptedToken.length < encapsulationSize + GCM_IV_LENGTH) {
                throw new IllegalArgumentException("Encrypted token too short: " + encryptedToken.length + 
                                                 " bytes (expected at least " + 
                                                 (encapsulationSize + GCM_IV_LENGTH) + " bytes)");
            }
            
            byte[] encapsulation = Arrays.copyOfRange(encryptedToken, 0, encapsulationSize);
            byte[] iv = Arrays.copyOfRange(encryptedToken, encapsulationSize, 
                                          encapsulationSize + GCM_IV_LENGTH);
            byte[] ciphertext = Arrays.copyOfRange(encryptedToken, encapsulationSize + GCM_IV_LENGTH,
                                                   encryptedToken.length);
            
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Extracted: encapsulation=" + encapsulation.length + 
                        " bytes, IV=" + iv.length + " bytes, ciphertext=" + ciphertext.length + " bytes");
            }
            
            // 2. Decapsulate shared secret using ML-KEM
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Performing ML-KEM decapsulation");
            }
            
            // Use PQCRuntimeSupport for Java 26 ML-KEM operations
            SecretKey sharedSecret = PQCRuntimeSupport.decapsulate(recipientPrivateKey, encapsulation);
            
            // 3. Derive AES-256 key
            SecretKey aesKey = deriveAESKey(sharedSecret);
            
            // 4. Decrypt with AES-256-GCM
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Decrypting with AES-256-GCM");
            }
            
            Cipher cipher = Cipher.getInstance(AES_GCM_CIPHER);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, aesKey, gcmSpec);
            byte[] tokenData = cipher.doFinal(ciphertext);
            
            long elapsedTime = System.currentTimeMillis() - startTime;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Decryption completed in " + elapsedTime + "ms, output size: " + 
                        tokenData.length + " bytes");
            }
            
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.exit(tc, "decryptToken");
            }
            
            return tokenData;
            
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Error decrypting token", e);
            }
            throw e;
        }
    }
    
    /**
     * Derive an AES-256 key from the ML-KEM shared secret using HKDF.
     * 
     * This provides additional security by deriving a fresh key from the shared secret
     * rather than using it directly.
     * 
     * @param sharedSecret The shared secret from ML-KEM
     * @return AES-256 key
     * @throws Exception if key derivation fails
     */
    @Sensitive
    private static SecretKey deriveAESKey(@Sensitive SecretKey sharedSecret) throws Exception {
        // Use HKDF-Expand (simplified version using HMAC-SHA256)
        Mac hmac = Mac.getInstance(HMAC_SHA256);
        hmac.init(sharedSecret);
        hmac.update(HKDF_INFO);
        byte[] derivedKey = hmac.doFinal();
        
        // Take first 32 bytes for AES-256
        byte[] aesKeyBytes = Arrays.copyOf(derivedKey, AES_KEY_LENGTH);
        
        return new SecretKeySpec(aesKeyBytes, AES_ALGORITHM);
    }
    
    /**
     * Generate a cryptographically secure random IV for GCM mode.
     * 
     * @return Random IV (12 bytes)
     */
    private static byte[] generateIV() {
        byte[] iv = new byte[GCM_IV_LENGTH];
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(iv);
        return iv;
    }
    
    /**
     * Concatenate multiple byte arrays into a single array.
     * 
     * @param arrays The arrays to concatenate
     * @return Concatenated array
     */
    private static byte[] concatenate(byte[]... arrays) {
        int totalLength = 0;
        for (byte[] array : arrays) {
            totalLength += array.length;
        }
        
        ByteBuffer buffer = ByteBuffer.allocate(totalLength);
        for (byte[] array : arrays) {
            buffer.put(array);
        }
        
        return buffer.array();
    }
    
    /**
     * Get the expected encrypted token size for a given plaintext size.
     * 
     * @param plaintextSize The size of the plaintext data
     * @param mlkemAlgorithm The ML-KEM algorithm type
     * @return Expected encrypted token size
     */
    public static int getEncryptedTokenSize(int plaintextSize, MLKEMAlgorithmType mlkemAlgorithm) {
        int encapsulationSize = mlkemAlgorithm.getCiphertextSize();
        int gcmOverhead = GCM_TAG_LENGTH / 8; // Convert bits to bytes (16 bytes)
        return encapsulationSize + GCM_IV_LENGTH + plaintextSize + gcmOverhead;
    }
    
    /**
     * Validate that the encrypted token has the minimum required size.
     * 
     * @param encryptedToken The encrypted token
     * @param mlkemAlgorithm The ML-KEM algorithm type
     * @return true if valid, false otherwise
     */
    public static boolean isValidEncryptedTokenSize(byte[] encryptedToken, MLKEMAlgorithmType mlkemAlgorithm) {
        if (encryptedToken == null) {
            return false;
        }
        int minSize = mlkemAlgorithm.getCiphertextSize() + GCM_IV_LENGTH + (GCM_TAG_LENGTH / 8);
        return encryptedToken.length >= minSize;
    }
}

// Made with Bob
