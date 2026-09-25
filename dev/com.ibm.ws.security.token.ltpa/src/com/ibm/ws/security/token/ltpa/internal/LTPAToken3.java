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
package com.ibm.ws.security.token.ltpa.internal;

import java.io.Serializable;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.security.auth.InvalidTokenException;
import com.ibm.websphere.security.auth.TokenExpiredException;
import com.ibm.ws.common.encoder.Base64Coder;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.token.ltpa.LTPAHybridKeys;
import com.ibm.ws.security.token.ltpa.pqc.LTPAPQCCrypto;
import com.ibm.ws.security.token.ltpa.pqc.LTPAPQCSignature;
import com.ibm.ws.security.token.ltpa.pqc.MLDSAAlgorithmType;
import com.ibm.ws.security.token.ltpa.pqc.MLKEMAlgorithmType;
import com.ibm.wsspi.security.ltpa.Token;
import com.ibm.wsspi.security.token.AttributeNameConstants;

/**
 * Represents an LTPA Token Version 3 with Post-Quantum Cryptography (PQC) support.
 *
 * This token uses a hybrid cryptographic approach combining three cryptographic systems:
 * - RSA-2048 for digital signatures (classical security)
 * - ML-DSA for quantum-resistant digital signatures (NIST FIPS 204)
 * - ML-KEM for quantum-resistant key encapsulation (NIST FIPS 203)
 * - AES-256-GCM for authenticated encryption of token data
 *
 * Token Format (Base64-encoded):
 * [version:1][userData][expiration:8][rsaSignature:256][mldsaSignature:variable][mlkemEncapsulation:variable][iv:12][encryptedData:variable][authTag:16]
 *
 * Security Properties:
 * - Provides quantum-resistant security via ML-DSA + ML-KEM (NIST Level 1/3/5)
 * - Maintains classical security via RSA-2048 signatures
 * - Defense-in-depth: Both RSA and ML-DSA signatures must verify
 * - Forward secrecy through ephemeral ML-KEM key encapsulation
 * - Authenticated encryption prevents tampering
 *
 * @since Liberty 26.0.0.1
 */
public class LTPAToken3 implements Token, Serializable {

    private static final TraceComponent tc = Tr.register(LTPAToken3.class);
    private static final long serialVersionUID = 3L;
    
    // Token format constants
    private static final short VERSION = 3;
    private static final String DELIM = "%";
    private static final int VERSION_SIZE = 1;
    private static final int EXPIRATION_SIZE = 8;
    private static final int RSA_SIGNATURE_SIZE = 256; // RSA-2048 signature
    private static final int MLDSA_SIGNATURE_SIZE_OFFSET = 2; // 2 bytes for signature size
    
    // Token components
    private byte[] encryptedBytes = null;
    private byte[] rsaSignature = null;
    private byte[] mldsaSignature = null;
    private UserData userData;
    private long expirationInMilliseconds;
    
    // Cryptographic keys
    private final LTPAHybridKeys hybridKeys;
    
    /**
     * Constructor for validating an existing LTPA3 token.
     *
     * @param tokenBytes The Base64-encoded byte representation of the LTPA3 token
     * @param hybridKeys The hybrid keys (RSA + ML-DSA + ML-KEM) for validation
     * @throws InvalidTokenException if the token is malformed or invalid
     */
    public LTPAToken3(byte[] tokenBytes, LTPAHybridKeys hybridKeys) throws InvalidTokenException {
        checkTokenBytes(tokenBytes);
        this.encryptedBytes = tokenBytes.clone();
        this.hybridKeys = hybridKeys;
        this.rsaSignature = null;
        this.mldsaSignature = null;
        this.expirationInMilliseconds = 0;
        decrypt();
    }
    
    /**
     * Constructor for validating an existing LTPA3 token with attribute removal.
     *
     * @param tokenBytes The Base64-encoded byte representation of the LTPA3 token
     * @param hybridKeys The hybrid keys (RSA + ML-DSA + ML-KEM) for validation
     * @param attributes The list of attributes to remove from the token
     * @throws InvalidTokenException if the token is malformed or invalid
     * @throws TokenExpiredException if the token has expired
     */
    public LTPAToken3(byte[] tokenBytes, LTPAHybridKeys hybridKeys, String... attributes)
            throws InvalidTokenException, TokenExpiredException {
        checkTokenBytes(tokenBytes);
        this.encryptedBytes = tokenBytes.clone();
        this.hybridKeys = hybridKeys;
        this.rsaSignature = null;
        this.mldsaSignature = null;
        this.expirationInMilliseconds = 0;
        decrypt();
        isValid();
        if (attributes != null) {
            // Reset signatures and encrypted bytes, then remove attributes
            this.rsaSignature = null;
            this.mldsaSignature = null;
            this.encryptedBytes = null;
            userData.removeAttributes(attributes);
        }
    }
    
    /**
     * Constructor for creating a new LTPA3 token.
     *
     * @param accessID The unique user identifier
     * @param expirationInMinutes Expiration limit of the token in minutes
     * @param hybridKeys The hybrid keys (RSA + ML-DSA + ML-KEM) for signing and encryption
     */
    protected LTPAToken3(String accessID, long expirationInMinutes, LTPAHybridKeys hybridKeys) {
        this.hybridKeys = hybridKeys;
        this.userData = new UserData(accessID);
        this.rsaSignature = null;
        this.mldsaSignature = null;
        this.encryptedBytes = null;
        setExpiration(expirationInMinutes);
    }
    
    /**
     * Constructor for cloning an LTPA3 token.
     *
     * @param expirationInMinutes Expiration limit of the token in minutes
     * @param hybridKeys The hybrid keys (RSA + ML-DSA + ML-KEM)
     * @param userdata The UserData to clone
     */
    protected LTPAToken3(long expirationInMinutes, LTPAHybridKeys hybridKeys, UserData userdata) {
        this.hybridKeys = hybridKeys;
        this.userData = userdata;
        this.rsaSignature = null;
        this.mldsaSignature = null;
        this.encryptedBytes = null;
        setExpiration(expirationInMinutes);
    }
    
    /**
     * Encrypts the token data using ML-KEM key encapsulation and AES-256-GCM.
     *
     * Token Structure:
     * 1. Version byte (1 byte)
     * 2. RSA signature (256 bytes)
     * 3. ML-DSA signature size (2 bytes)
     * 4. ML-DSA signature (variable length)
     * 5. ML-KEM encapsulation + IV + encrypted data + auth tag
     *
     * @throws Exception if encryption fails
     */
    @FFDCIgnore(Exception.class)
    private void encrypt() throws Exception {
        try {
            // Prepare user data with expiration
            String ud = userData.toString();
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(this, tc, "encrypt: userData=" + ud);
            }
            
            byte[] userDataBytes = ud.getBytes(StandardCharsets.UTF_8);
            
            // Check if ML-KEM keys are available for encryption
            boolean hasMLKEMKeys = (hybridKeys.getMlkemPublicKeyBytes() != null &&
                                   hybridKeys.getMlkemAlgorithm() != null);
            
            if (hasMLKEMKeys) {
                // Build plaintext: userData + expiration
                ByteBuffer plaintext = ByteBuffer.allocate(userDataBytes.length + EXPIRATION_SIZE);
                plaintext.put(userDataBytes);
                plaintext.putLong(expirationInMilliseconds);
                byte[] plaintextBytes = plaintext.array();
                
                // Reconstruct ML-KEM public key from bytes
                MLKEMAlgorithmType mlkemAlgo = MLKEMAlgorithmType.fromString(hybridKeys.getMlkemAlgorithm());
                PublicKey mlkemPublicKey = reconstructMLKEMPublicKey(
                    hybridKeys.getMlkemPublicKeyBytes(),
                    mlkemAlgo
                );
                
                // Encrypt using ML-KEM + AES-256-GCM
                byte[] encryptedData = LTPAPQCCrypto.encryptToken(
                    plaintextBytes,
                    mlkemPublicKey,
                    mlkemAlgo
                );
                
                // Build final token: version + rsaSignature + mldsaSignatureSize + mldsaSignature + encryptedData
                int mldsaSignatureSize = mldsaSignature.length;
                ByteBuffer tokenBuffer = ByteBuffer.allocate(
                    VERSION_SIZE + RSA_SIGNATURE_SIZE + MLDSA_SIGNATURE_SIZE_OFFSET + mldsaSignatureSize + encryptedData.length
                );
                tokenBuffer.put((byte) VERSION);
                tokenBuffer.put(rsaSignature);
                tokenBuffer.putShort((short) mldsaSignatureSize);
                tokenBuffer.put(mldsaSignature);
                tokenBuffer.put(encryptedData);
                
                encryptedBytes = tokenBuffer.array();
                
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(this, tc, "Token encrypted with ML-KEM successfully, size=" + encryptedBytes.length);
                }
            } else {
                // ML-KEM keys not available - create unencrypted token with signatures only
                // Format: version + rsaSignature + mldsaSignatureSize + mldsaSignature + userData + expiration
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "ML-KEM keys not available, creating token without encryption");
                }
                
                ByteBuffer plaintext = ByteBuffer.allocate(userDataBytes.length + EXPIRATION_SIZE);
                plaintext.put(userDataBytes);
                plaintext.putLong(expirationInMilliseconds);
                byte[] plaintextBytes = plaintext.array();
                
                int mldsaSignatureSize = mldsaSignature.length;
                ByteBuffer tokenBuffer = ByteBuffer.allocate(
                    VERSION_SIZE + RSA_SIGNATURE_SIZE + MLDSA_SIGNATURE_SIZE_OFFSET + mldsaSignatureSize + plaintextBytes.length
                );
                tokenBuffer.put((byte) VERSION);
                tokenBuffer.put(rsaSignature);
                tokenBuffer.putShort((short) mldsaSignatureSize);
                tokenBuffer.put(mldsaSignature);
                tokenBuffer.put(plaintextBytes);
                
                encryptedBytes = tokenBuffer.array();
                
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(this, tc, "Token created without encryption (ML-KEM not available), size=" + encryptedBytes.length);
                }
            }
            
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(this, tc, "Error encrypting token", e);
            }
            throw e;
        }
    }
    
    /**
     * Decrypts the encrypted token bytes.
     *
     * @throws InvalidTokenException if decryption fails or token is malformed
     */
    @FFDCIgnore(Exception.class)
    private void decrypt() throws InvalidTokenException {
        try {
            ByteBuffer buffer = ByteBuffer.wrap(encryptedBytes);
            
            // Read version
            byte version = buffer.get();
            if (version != VERSION) {
                throw new InvalidTokenException("Invalid token version: " + version);
            }
            
            // Read RSA signature
            byte[] rsaSig = new byte[RSA_SIGNATURE_SIZE];
            buffer.get(rsaSig);
            this.rsaSignature = rsaSig;
            
            // Read ML-DSA signature size
            short mldsaSigSize = buffer.getShort();
            if (mldsaSigSize < 0 || mldsaSigSize > 10000) { // Sanity check
                throw new InvalidTokenException("Invalid ML-DSA signature size: " + mldsaSigSize);
            }
            
            // Read ML-DSA signature
            byte[] mldsaSig = new byte[mldsaSigSize];
            buffer.get(mldsaSig);
            this.mldsaSignature = mldsaSig;
            
            // Read encrypted data (ML-KEM encapsulation + IV + ciphertext + tag)
            byte[] encryptedData = new byte[buffer.remaining()];
            buffer.get(encryptedData);
            
            // Reconstruct ML-KEM private key from bytes
            MLKEMAlgorithmType mlkemAlgo = MLKEMAlgorithmType.fromString(hybridKeys.getMlkemAlgorithm());
            PrivateKey mlkemPrivateKey = reconstructMLKEMPrivateKey(
                hybridKeys.getMlkemPrivateKeyBytes(),
                mlkemAlgo
            );
            
            // Decrypt using ML-KEM + AES-256-GCM
            byte[] decryptedData = LTPAPQCCrypto.decryptToken(
                encryptedData,
                mlkemPrivateKey,
                mlkemAlgo
            );
            
            // Parse decrypted data
            ByteBuffer plaintext = ByteBuffer.wrap(decryptedData);
            
            // Extract user data (everything except last 8 bytes)
            byte[] userDataBytes = new byte[decryptedData.length - EXPIRATION_SIZE];
            plaintext.get(userDataBytes);
            
            // Extract expiration
            expirationInMilliseconds = plaintext.getLong();
            
            // Parse user data
            String userDataStr = new String(userDataBytes, StandardCharsets.UTF_8);
            String[] userFields = LTPATokenizer.parseToken(userDataStr);
            Map<String, ArrayList<String>> attribs = LTPATokenizer.parseUserData(userFields[0]);
            userData = new UserData(attribs);
            
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(this, tc, "Token decrypted successfully");
            }
            
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(this, tc, "Error decrypting token", e);
            }
            throw new InvalidTokenException("Token decryption failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Signs the token data using both RSA-2048 and ML-DSA.
     *
     * @throws Exception if signing fails
     */
    @FFDCIgnore(Exception.class)
    private void sign() throws Exception {
        try {
            // Prepare data to sign: userData + expiration
            String dataStr = userData.toString();
            byte[] userDataBytes = dataStr.getBytes(StandardCharsets.UTF_8);
            
            ByteBuffer signData = ByteBuffer.allocate(userDataBytes.length + EXPIRATION_SIZE);
            signData.put(userDataBytes);
            signData.putLong(expirationInMilliseconds);
            byte[] dataToSign = signData.array();
            
            // Get LTPAPrivateKey and extract raw key components
            com.ibm.ws.crypto.ltpakeyutil.LTPAPrivateKey ltpaPrivKey = hybridKeys.getRsaPrivateKey();
            byte[][] rawKey = com.ibm.ws.crypto.ltpakeyutil.LTPAKeyUtil.getRawKey(ltpaPrivKey);
            
            // Reconstruct standard RSA private key from raw components
            // rawKey[1] = private exponent (d)
            // rawKey[2] = public exponent (e)
            // rawKey[3] = prime P
            // rawKey[4] = prime Q
            BigInteger privateExponent = new BigInteger(1, rawKey[1]);
            BigInteger publicExponent = new BigInteger(1, rawKey[2]);
            BigInteger primeP = new BigInteger(1, rawKey[3]);
            BigInteger primeQ = new BigInteger(1, rawKey[4]);
            BigInteger modulus = primeP.multiply(primeQ);
            
            // Calculate CRT parameters
            BigInteger primeExponentP = privateExponent.mod(primeP.subtract(BigInteger.ONE));
            BigInteger primeExponentQ = privateExponent.mod(primeQ.subtract(BigInteger.ONE));
            BigInteger crtCoefficient = primeQ.modInverse(primeP);
            
            RSAPrivateCrtKeySpec rsaKeySpec = new RSAPrivateCrtKeySpec(
                modulus, publicExponent, privateExponent,
                primeP, primeQ, primeExponentP, primeExponentQ, crtCoefficient
            );
            
            KeyFactory rsaKeyFactory = KeyFactory.getInstance("RSA");
            PrivateKey rsaPrivateKey = rsaKeyFactory.generatePrivate(rsaKeySpec);
            
            // Sign with RSA-2048
            Signature rsaSigner = Signature.getInstance("SHA256withRSA");
            rsaSigner.initSign(rsaPrivateKey);
            rsaSigner.update(dataToSign);
            rsaSignature = rsaSigner.sign();
            
            // Reconstruct ML-DSA private key from bytes
            MLDSAAlgorithmType mldsaAlgo = MLDSAAlgorithmType.fromString(hybridKeys.getMldsaAlgorithm());
            PrivateKey mldsaPrivateKey = LTPAPQCSignature.reconstructPrivateKey(
                hybridKeys.getMldsaPrivateKeyBytes(),
                mldsaAlgo
            );
            
            // Sign with ML-DSA
            mldsaSignature = LTPAPQCSignature.sign(
                dataToSign,
                mldsaPrivateKey,
                mldsaAlgo
            );
            
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(this, tc, "Token signed successfully with RSA and ML-DSA");
            }
            
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(this, tc, "Error signing token", e);
            }
            throw e;
        }
    }
    
    /**
     * Verifies both RSA and ML-DSA signatures of the token.
     * Both signatures must be valid for the token to be considered valid.
     *
     * @return true if both signatures are valid
     * @throws Exception if verification fails
     */
    @FFDCIgnore(Exception.class)
    private boolean verify() throws Exception {
        try {
            // Prepare data to verify: userData + expiration
            String dataStr = userData.toString();
            byte[] userDataBytes = dataStr.getBytes(StandardCharsets.UTF_8);
            
            ByteBuffer verifyData = ByteBuffer.allocate(userDataBytes.length + EXPIRATION_SIZE);
            verifyData.put(userDataBytes);
            verifyData.putLong(expirationInMilliseconds);
            byte[] dataToVerify = verifyData.array();
            
            // Verify RSA signature
            Signature rsaVerifier = Signature.getInstance("SHA256withRSA");
            rsaVerifier.initVerify(hybridKeys.getRsaPublicKey());
            rsaVerifier.update(dataToVerify);
            boolean rsaValid = rsaVerifier.verify(rsaSignature);
            
            if (!rsaValid) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(this, tc, "RSA signature verification failed");
                }
                return false;
            }
            
            // Reconstruct ML-DSA public key from bytes
            MLDSAAlgorithmType mldsaAlgo = MLDSAAlgorithmType.fromString(hybridKeys.getMldsaAlgorithm());
            PublicKey mldsaPublicKey = LTPAPQCSignature.reconstructPublicKey(
                hybridKeys.getMldsaPublicKeyBytes(),
                mldsaAlgo
            );
            
            // Verify ML-DSA signature
            boolean mldsaValid = LTPAPQCSignature.verify(
                dataToVerify,
                mldsaSignature,
                mldsaPublicKey,
                mldsaAlgo
            );
            
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(this, tc, "Signature verification - RSA: " + rsaValid + ", ML-DSA: " + mldsaValid);
            }
            
            return rsaValid && mldsaValid;
            
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(this, tc, "Error verifying signatures", e);
            }
            throw e;
        }
    }
    
    /** {@inheritDoc} */
    @Override
    @FFDCIgnore(Exception.class)
    public boolean isValid() throws InvalidTokenException, TokenExpiredException {
        validateExpiration();
        
        boolean verified = false;
        try {
            verified = verify();
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Signature verification failed", e);
            }
            throw new InvalidTokenException("Token validation failed: " + e.getMessage(), e);
        }
        
        if (!verified) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Invalid signature");
            }
            throw new InvalidTokenException("Token validation failed: invalid signature");
        }
        
        return true;
    }
    
    /**
     * Validates that the token has not expired.
     * 
     * @throws TokenExpiredException if the token has expired
     */
    public void validateExpiration() throws TokenExpiredException {
        Date currentTime = new Date();
        Date expirationTime = new Date(expirationInMilliseconds);
        
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "Current time=" + currentTime + ", expiration=" + expirationTime);
        }
        
        if (currentTime.after(expirationTime)) {
            String msg = "Token expired: current=" + currentTime + ", expiration=" + expirationTime;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, msg);
            }
            throw new TokenExpiredException(expirationInMilliseconds, msg);
        }
    }
    
    /** {@inheritDoc} */
    @Override
    @FFDCIgnore(Exception.class)
    public byte[] getBytes() throws InvalidTokenException, TokenExpiredException {
        if (encryptedBytes == null) {
            try {
                sign();
                encrypt();
            } catch (Exception e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "Error generating token bytes", e);
                }
                throw new InvalidTokenException("Failed to generate token: " + e.getMessage(), e);
            }
        }
        return encryptedBytes.clone();
    }
    
    /** {@inheritDoc} */
    @Override
    public long getExpiration() {
        return expirationInMilliseconds;
    }
    
    /** {@inheritDoc} */
    @Override
    public short getVersion() {
        return VERSION;
    }
    
    /** {@inheritDoc} */
    @Override
    public String[] addAttribute(String name, String value) {
        rsaSignature = null;
        mldsaSignature = null;
        encryptedBytes = null;
        return userData.addAttribute(name, value);
    }
    
    /** {@inheritDoc} */
    @Override
    public String[] getAttributes(String name) {
        return userData.getAttributes(name);
    }
    
    /** {@inheritDoc} */
    @Override
    public Enumeration<String> getAttributeNames() {
        return userData.getAttributeNames();
    }
    
    /** {@inheritDoc} */
    @Override
    public String toString() {
        return encryptedBytes == null ? "NULL" : Base64Coder.base64EncodeToString(encryptedBytes);
    }
    
    /**
     * Creates a deep copy of this LTPA3 token.
     *
     * @return A new copy of the token
     */
    @Override
    public Object clone() {
        UserData clonedUserData = (UserData) userData.clone();
        return new LTPAToken3(expirationInMilliseconds, hybridKeys, clonedUserData);
    }
    
    /**
     * Validates that token bytes are not null or empty.
     * 
     * @param tokenBytes The token bytes to validate
     * @throws IllegalArgumentException if token bytes are invalid
     */
    private static void checkTokenBytes(byte[] tokenBytes) {
        if (tokenBytes == null || tokenBytes.length == 0) {
            throw new IllegalArgumentException("Token bytes cannot be null or empty");
        }
    }
    
    /**
     * Sets the expiration time for this token.
     * 
     * @param expirationInMinutes The expiration time in minutes from now
     */
    private void setExpiration(long expirationInMinutes) {
        expirationInMilliseconds = System.currentTimeMillis() + (expirationInMinutes * 60 * 1000);
        rsaSignature = null;
        mldsaSignature = null;
        encryptedBytes = null;
        
        if (userData != null) {
            userData.addAttribute(AttributeNameConstants.WSTOKEN_EXPIRATION,
                                Long.toString(expirationInMilliseconds));
        }
    }
    
    /**
     * Reconstruct ML-KEM public key from byte array.
     *
     * @param keyBytes The public key bytes
     * @param algorithm The ML-KEM algorithm type
     * @return The reconstructed public key
     * @throws Exception if reconstruction fails
     */
    private PublicKey reconstructMLKEMPublicKey(byte[] keyBytes, MLKEMAlgorithmType algorithm) throws Exception {
        try {
            java.security.KeyFactory keyFactory = java.security.KeyFactory.getInstance("ML-KEM");
            java.security.spec.X509EncodedKeySpec keySpec = new java.security.spec.X509EncodedKeySpec(keyBytes);
            return keyFactory.generatePublic(keySpec);
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Failed to reconstruct ML-KEM public key", e);
            }
            throw e;
        }
    }
    
    /**
     * Reconstruct ML-KEM private key from byte array.
     *
     * @param keyBytes The private key bytes
     * @param algorithm The ML-KEM algorithm type
     * @return The reconstructed private key
     * @throws Exception if reconstruction fails
     */
    private PrivateKey reconstructMLKEMPrivateKey(byte[] keyBytes, MLKEMAlgorithmType algorithm) throws Exception {
        try {
            java.security.KeyFactory keyFactory = java.security.KeyFactory.getInstance("ML-KEM");
            java.security.spec.PKCS8EncodedKeySpec keySpec = new java.security.spec.PKCS8EncodedKeySpec(keyBytes);
            return keyFactory.generatePrivate(keySpec);
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Failed to reconstruct ML-KEM private key", e);
            }
            throw e;
        }
    }
    
    /**
     * Get token as multiple cookies if splitting is required.
     *
     * For large LTPA3 tokens (>3KB), this method splits the token across multiple
     * cookies to stay within browser cookie size limits (4KB per cookie).
     *
     * @return Map of cookie names to Base64-encoded cookie values
     * @throws InvalidTokenException if token generation fails
     * @throws TokenExpiredException if token has expired
     */
    public Map<String, String> getCookies() throws InvalidTokenException, TokenExpiredException {
        byte[] tokenBytes = getBytes();
        
        if (LTPACookieSplitter.requiresSplitting(tokenBytes)) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Token requires splitting, size=" + tokenBytes.length);
            }
            return LTPACookieSplitter.splitToken(tokenBytes, "LtpaToken3");
        } else {
            // Single cookie (backward compatible)
            Map<String, String> cookies = new HashMap<>();
            cookies.put("LtpaToken3", Base64Coder.base64EncodeToString(tokenBytes));
            
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Token fits in single cookie, size=" + tokenBytes.length);
            }
            
            return cookies;
        }
    }
    
    /**
     * Create an LTPA3 token from cookie fragments.
     *
     * This method handles both single-cookie tokens (backward compatible) and
     * multi-cookie fragmented tokens. It automatically detects which format is
     * being used and reassembles the token accordingly.
     *
     * @param cookies Map of cookie names to Base64-encoded values
     * @param hybridKeys The hybrid keys for token validation
     * @return The reconstructed LTPA3 token
     * @throws InvalidTokenException if cookies are missing, invalid, or corrupted
     */
    public static LTPAToken3 fromCookies(Map<String, String> cookies, LTPAHybridKeys hybridKeys)
            throws InvalidTokenException {
        
        if (cookies == null || cookies.isEmpty()) {
            throw new InvalidTokenException("No cookies provided");
        }
        
        String mainCookie = cookies.get("LtpaToken3");
        if (mainCookie == null) {
            throw new InvalidTokenException("LtpaToken3 cookie not found");
        }
        
        byte[] tokenBytes;
        
        // Check if this is a fragmented token
        if (LTPACookieSplitter.isFragmented(cookies, "LtpaToken3")) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Reassembling fragmented token from " + cookies.size() + " cookies");
            }
            tokenBytes = LTPACookieSplitter.reassembleToken(cookies, "LtpaToken3");
        } else {
            // Single cookie (backward compatible)
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Processing single-cookie token");
            }
            try {
                tokenBytes = Base64Coder.base64DecodeString(mainCookie);
            } catch (Exception e) {
                throw new InvalidTokenException("Failed to decode token: " + e.getMessage());
            }
        }
        
        return new LTPAToken3(tokenBytes, hybridKeys);
    }
}

// Made with Bob
