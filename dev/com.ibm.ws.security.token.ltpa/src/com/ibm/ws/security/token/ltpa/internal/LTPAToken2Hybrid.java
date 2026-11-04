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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.PublicKey;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.security.auth.InvalidTokenException;
import com.ibm.websphere.security.auth.TokenExpiredException;
import com.ibm.ws.common.encoder.Base64Coder;
import com.ibm.ws.crypto.ltpakeyutil.LTPAPrivateKey;
import com.ibm.ws.crypto.ltpakeyutil.LTPAPublicKey;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.token.ltpa.internal.pqc.PQCAlgorithm;
import com.ibm.ws.security.token.ltpa.internal.pqc.PQCException;
import com.ibm.ws.security.token.ltpa.internal.pqc.PQCProvider;
import com.ibm.ws.security.token.ltpa.internal.pqc.PQCProviderFactory;

/**
 * Represents a hybrid LTPA token that combines classical RSA signatures with Post-Quantum
 * Cryptography (PQC) signatures for quantum-resistant authentication.
 * 
 * <p>This class extends {@link LTPAToken2} to add PQC signature support while maintaining
 * full backward compatibility with classical LTPA tokens. Hybrid tokens contain both RSA-2048
 * and ML-DSA (Module-Lattice-Based Digital Signature Algorithm) signatures, providing protection
 * against both classical and quantum attacks.</p>
 * 
 * <h3>Token Format:</h3>
 * <pre>
 * LTPAToken2Hybrid = Base64(Payload || RSA_Signature || PQC_Signature)
 * 
 * Components:
 * - Payload: Enhanced with PQC metadata (pqcEnabled, pqcAlgorithm, pqcVersion)
 * - RSA_Signature: 256-byte RSA-SHA256 signature (backward compatibility)
 * - PQC_Signature: ~3293-byte ML-DSA-65 signature (quantum resistance)
 * </pre>
 * 
 * <h3>Signature Chaining:</h3>
 * <p>The hybrid token uses signature chaining to cryptographically bind both signatures:</p>
 * <ol>
 *   <li>RSA signature signs the payload (existing behavior)</li>
 *   <li>PQC signature signs (payload + RSA signature) for cryptographic binding</li>
 *   <li>Both signatures must validate for hybrid tokens</li>
 * </ol>
 * 
 * <h3>Backward Compatibility:</h3>
 * <ul>
 *   <li><b>Classical servers:</b> Validate RSA signature only, ignore PQC signature</li>
 *   <li><b>Hybrid servers:</b> Validate both RSA and PQC signatures</li>
 *   <li><b>Token version:</b> Field distinguishes classical (0x00) vs hybrid (0x01)</li>
 * </ul>
 * 
 * <h3>Usage Example:</h3>
 * <pre>
 * // Create hybrid token
 * LTPAToken2Hybrid token = new LTPAToken2Hybrid(
 *     "user@example.com",
 *     120, // 120 minutes expiration
 *     sharedKey,
 *     rsaPrivateKey,
 *     rsaPublicKey,
 *     pqcPrivateKey,
 *     pqcPublicKey,
 *     PQCAlgorithm.ML_DSA_65
 * );
 * 
 * // Get token bytes for transmission
 * byte[] tokenBytes = token.getBytes();
 * 
 * // Validate hybrid token
 * LTPAToken2Hybrid validatedToken = new LTPAToken2Hybrid(
 *     tokenBytes,
 *     sharedKey,
 *     rsaPrivateKey,
 *     rsaPublicKey,
 *     pqcPrivateKey,
 *     pqcPublicKey,
 *     expDiffAllowed
 * );
 * boolean valid = validatedToken.isValid(); // Validates both signatures
 * </pre>
 * 
 * <h3>Security Considerations:</h3>
 * <ul>
 *   <li><b>Quantum Resistance:</b> ML-DSA provides NIST Level 3 security (equivalent to AES-192)</li>
 *   <li><b>Hybrid Security:</b> Token remains secure even if one signature algorithm is broken</li>
 *   <li><b>FIPS Compliance:</b> ML-DSA is FIPS 140-3 approved when using validated providers</li>
 *   <li><b>Token Size:</b> Hybrid tokens are ~6.3x larger than classical tokens (~3862 bytes)</li>
 * </ul>
 * 
 * @see LTPAToken2
 * @see PQCProvider
 * @see PQCAlgorithm
 * @since 1.0
 */
public class LTPAToken2Hybrid extends LTPAToken2 {
    
    private static final TraceComponent tc = Tr.register(LTPAToken2Hybrid.class);
    
    /**
     * Serial version UID for serialization compatibility.
     */
    private static final long serialVersionUID = 1L;
    
    /**
     * Token version for hybrid tokens (0x01).
     * Classical tokens use version 0x00.
     */
    private static final byte HYBRID_TOKEN_VERSION = 0x01;
    
    /**
     * PQC format version (1).
     * Allows for future format changes while maintaining compatibility.
     */
    private static final int PQC_FORMAT_VERSION = 1;
    
    /**
     * Length prefix size for variable-length fields (4 bytes).
     */
    private static final int LENGTH_PREFIX_SIZE = 4;
    
    // ========================================================================
    // PQC-Specific Fields
    // ========================================================================
    
    /**
     * PQC signature bytes.
     * Size varies by algorithm: ~3293 bytes for ML-DSA-65, ~4595 bytes for ML-DSA-87.
     */
    private byte[] pqcSignature;
    
    /**
     * PQC private key for signature generation.
     */
    private final PrivateKey pqcPrivateKey;
    
    /**
     * PQC public key for signature verification.
     */
    private final PublicKey pqcPublicKey;
    
    /**
     * PQC algorithm used for signature generation and verification.
     * Default: ML-DSA-65 (NIST Security Level 3).
     */
    private final PQCAlgorithm pqcAlgorithm;
    
    /**
     * PQC provider for cryptographic operations.
     * Automatically selected based on Java version and FIPS mode.
     */
    private final PQCProvider pqcProvider;
    
    /**
     * Flag indicating whether PQC validation is enabled.
     * When false, only RSA signature is validated (backward compatibility mode).
     */
    private final boolean pqcValidationEnabled;
    
    // ========================================================================
    // Constructors
    // ========================================================================
    
    /**
     * Constructs a hybrid LTPA token from encrypted token bytes (validation constructor).
     * 
     * <p>This constructor is used to validate an existing hybrid token. It decrypts the token,
     * parses the payload and signatures, and validates both RSA and PQC signatures.</p>
     * 
     * <h4>Validation Process:</h4>
     * <ol>
     *   <li>Decrypt token bytes using shared key</li>
     *   <li>Parse payload, RSA signature, and PQC signature</li>
     *   <li>Validate token expiration</li>
     *   <li>Validate RSA signature (always required)</li>
     *   <li>Validate PQC signature (if PQC validation enabled)</li>
     * </ol>
     * 
     * @param tokenBytes encrypted token bytes (Base64-decoded)
     * @param sharedKey LTPA shared key for encryption/decryption
     * @param rsaPrivateKey RSA private key (not used for validation, but required by parent)
     * @param rsaPublicKey RSA public key for signature verification
     * @param pqcPrivateKey PQC private key (not used for validation)
     * @param pqcPublicKey PQC public key for signature verification
     * @param expDiffAllowed maximum allowed difference between expiration fields (milliseconds)
     * @throws InvalidTokenException if token validation fails
     * @throws TokenExpiredException if token has expired
     */
    public LTPAToken2Hybrid(byte[] tokenBytes, @Sensitive byte[] sharedKey,
                           LTPAPrivateKey rsaPrivateKey, LTPAPublicKey rsaPublicKey,
                           PrivateKey pqcPrivateKey, PublicKey pqcPublicKey,
                           long expDiffAllowed) throws InvalidTokenException, TokenExpiredException {
        this(tokenBytes, sharedKey, rsaPrivateKey, rsaPublicKey, pqcPrivateKey, pqcPublicKey,
             expDiffAllowed, PQCAlgorithm.ML_DSA_65, true);
    }
    
    /**
     * Constructs a hybrid LTPA token from encrypted token bytes with PQC validation control.
     * 
     * <p>This constructor allows disabling PQC validation for backward compatibility scenarios
     * where classical servers need to validate hybrid tokens using only RSA signatures.</p>
     * 
     * @param tokenBytes encrypted token bytes (Base64-decoded)
     * @param sharedKey LTPA shared key for encryption/decryption
     * @param rsaPrivateKey RSA private key (not used for validation, but required by parent)
     * @param rsaPublicKey RSA public key for signature verification
     * @param pqcPrivateKey PQC private key (not used for validation)
     * @param pqcPublicKey PQC public key for signature verification
     * @param expDiffAllowed maximum allowed difference between expiration fields (milliseconds)
     * @param pqcAlgorithm PQC algorithm to use for validation
     * @param pqcValidationEnabled whether to validate PQC signature
     * @throws InvalidTokenException if token validation fails
     * @throws TokenExpiredException if token has expired
     */
    public LTPAToken2Hybrid(byte[] tokenBytes, @Sensitive byte[] sharedKey,
                           LTPAPrivateKey rsaPrivateKey, LTPAPublicKey rsaPublicKey,
                           PrivateKey pqcPrivateKey, PublicKey pqcPublicKey,
                           long expDiffAllowed, PQCAlgorithm pqcAlgorithm,
                           boolean pqcValidationEnabled) throws InvalidTokenException, TokenExpiredException {
        super(tokenBytes, sharedKey, rsaPrivateKey, rsaPublicKey, expDiffAllowed);
        
        if (pqcPrivateKey == null) {
            throw new IllegalArgumentException("PQC private key cannot be null");
        }
        if (pqcPublicKey == null) {
            throw new IllegalArgumentException("PQC public key cannot be null");
        }
        if (pqcAlgorithm == null) {
            throw new IllegalArgumentException("PQC algorithm cannot be null");
        }
        
        this.pqcPrivateKey = pqcPrivateKey;
        this.pqcPublicKey = pqcPublicKey;
        this.pqcAlgorithm = pqcAlgorithm;
        this.pqcValidationEnabled = pqcValidationEnabled;
        
        try {
            this.pqcProvider = PQCProviderFactory.getProvider();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Using PQC provider: " + pqcProvider.getProviderName());
            }
        } catch (PQCException e) {
            throw new InvalidTokenException("Failed to initialize PQC provider: " + e.getMessage(), e);
        }
        
        // Decrypt and parse hybrid token (overrides parent's decrypt)
        decryptHybrid();
        
        // Validate hybrid token (both RSA and PQC signatures)
        if (pqcValidationEnabled) {
            isValid();
        }
    }
    
    /**
     * Constructs a new hybrid LTPA token for token generation.
     * 
     * <p>This constructor is used to create a new hybrid token with both RSA and PQC signatures.
     * The token is not encrypted until {@link #getBytes()} is called.</p>
     * 
     * @param accessID unique user identifier
     * @param expirationInMinutes expiration limit in minutes
     * @param sharedKey LTPA shared key for encryption
     * @param rsaPrivateKey RSA private key for signature generation
     * @param rsaPublicKey RSA public key for signature verification
     * @param pqcPrivateKey PQC private key for signature generation
     * @param pqcPublicKey PQC public key for signature verification
     * @param pqcAlgorithm PQC algorithm to use (ML-DSA-65 or ML-DSA-87)
     */
    public LTPAToken2Hybrid(String accessID, long expirationInMinutes, @Sensitive byte[] sharedKey,
                           LTPAPrivateKey rsaPrivateKey, LTPAPublicKey rsaPublicKey,
                           PrivateKey pqcPrivateKey, PublicKey pqcPublicKey,
                           PQCAlgorithm pqcAlgorithm) {
        super(accessID, expirationInMinutes, sharedKey, rsaPrivateKey, rsaPublicKey);
        
        if (pqcPrivateKey == null) {
            throw new IllegalArgumentException("PQC private key cannot be null");
        }
        if (pqcPublicKey == null) {
            throw new IllegalArgumentException("PQC public key cannot be null");
        }
        if (pqcAlgorithm == null) {
            throw new IllegalArgumentException("PQC algorithm cannot be null");
        }
        
        this.pqcPrivateKey = pqcPrivateKey;
        this.pqcPublicKey = pqcPublicKey;
        this.pqcAlgorithm = pqcAlgorithm;
        this.pqcValidationEnabled = true;
        this.pqcSignature = null;
        
        try {
            this.pqcProvider = PQCProviderFactory.getProvider();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Using PQC provider: " + pqcProvider.getProviderName());
            }
        } catch (PQCException e) {
            throw new IllegalStateException("Failed to initialize PQC provider: " + e.getMessage(), e);
        }
        
        // Add PQC metadata to token payload
        addPQCMetadata();
    }
    
    /**
     * Constructs a hybrid LTPA token for cloning.
     * 
     * @param expirationInMinutes expiration limit in minutes
     * @param sharedKey LTPA shared key
     * @param rsaPrivateKey RSA private key
     * @param rsaPublicKey RSA public key
     * @param pqcPrivateKey PQC private key
     * @param pqcPublicKey PQC public key
     * @param pqcAlgorithm PQC algorithm
     * @param userdata user data to clone
     */
    protected LTPAToken2Hybrid(long expirationInMinutes, @Sensitive byte[] sharedKey,
                              LTPAPrivateKey rsaPrivateKey, LTPAPublicKey rsaPublicKey,
                              PrivateKey pqcPrivateKey, PublicKey pqcPublicKey,
                              PQCAlgorithm pqcAlgorithm, UserData userdata) {
        super(expirationInMinutes, sharedKey, rsaPrivateKey, rsaPublicKey, userdata);
        
        this.pqcPrivateKey = pqcPrivateKey;
        this.pqcPublicKey = pqcPublicKey;
        this.pqcAlgorithm = pqcAlgorithm;
        this.pqcValidationEnabled = true;
        this.pqcSignature = null;
        
        try {
            this.pqcProvider = PQCProviderFactory.getProvider();
        } catch (PQCException e) {
            throw new IllegalStateException("Failed to initialize PQC provider: " + e.getMessage(), e);
        }
    }
    
    // ========================================================================
    // PQC Signature Operations
    // ========================================================================
    
    /**
     * Generates a PQC signature over the payload and RSA signature (signature chaining).
     * 
     * <p>The PQC signature signs the concatenation of:</p>
     * <ol>
     *   <li>Token payload (user data)</li>
     *   <li>RSA signature</li>
     * </ol>
     * 
     * <p>This signature chaining ensures that any tampering with the RSA signature
     * will invalidate the PQC signature, providing cryptographic binding between
     * the two signatures.</p>
     * 
     * @throws PQCException if PQC signature generation fails
     */
    private void signPQC() throws PQCException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(this, tc, "signPQC", pqcAlgorithm);
        }
        
        try {
            // Get payload bytes
            String dataStr = super.getUserData().toString();
            byte[] payloadBytes = Base64Coder.getBytes(dataStr);
            
            // Get RSA signature (must be generated first)
            byte[] rsaSignature = super.signature;
            if (rsaSignature == null) {
                throw new PQCException("RSA signature must be generated before PQC signature");
            }
            
            // Concatenate payload and RSA signature for PQC signing (signature chaining)
            byte[] dataToSign = concatenateBytes(payloadBytes, rsaSignature);
            
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Signing " + dataToSign.length + " bytes with " + pqcAlgorithm);
            }
            
            // Generate PQC signature
            this.pqcSignature = pqcProvider.sign(dataToSign, pqcPrivateKey, pqcAlgorithm);
            
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Generated PQC signature: " + pqcSignature.length + " bytes");
            }
            
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(this, tc, "Error generating PQC signature: " + e);
            }
            throw new PQCException("Failed to generate PQC signature", e);
        } finally {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.exit(this, tc, "signPQC");
            }
        }
    }
    
    /**
     * Verifies the PQC signature.
     * 
     * <p>Validates that the PQC signature is valid for the concatenation of
     * payload and RSA signature.</p>
     * 
     * @return true if PQC signature is valid, false otherwise
     * @throws PQCException if PQC signature verification fails
     */
    private boolean verifyPQC() throws PQCException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(this, tc, "verifyPQC", pqcAlgorithm);
        }
        
        try {
            if (pqcSignature == null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "PQC signature is null");
                }
                return false;
            }
            
            // Get payload bytes
            String dataStr = super.getUserData().toString();
            byte[] payloadBytes = Base64Coder.getBytes(dataStr);
            
            // Get RSA signature
            byte[] rsaSignature = super.signature;
            if (rsaSignature == null) {
                throw new PQCException("RSA signature is null during PQC verification");
            }
            
            // Concatenate payload and RSA signature (same as signing)
            byte[] dataToVerify = concatenateBytes(payloadBytes, rsaSignature);
            
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Verifying PQC signature: " + pqcSignature.length + " bytes");
            }
            
            // Verify PQC signature
            boolean valid = pqcProvider.verify(dataToVerify, pqcSignature, pqcPublicKey, pqcAlgorithm);
            
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "PQC signature valid: " + valid);
            }
            
            return valid;
            
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(this, tc, "Error verifying PQC signature: " + e);
            }
            throw new PQCException("Failed to verify PQC signature", e);
        } finally {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.exit(this, tc, "verifyPQC");
            }
        }
    }
    
    // ========================================================================
    // Hybrid Token Encryption/Decryption
    // ========================================================================
    
    /**
     * Encrypts the hybrid token with both RSA and PQC signatures.
     * 
     * <p>Token structure after encryption:</p>
     * <pre>
     * Encrypted(Payload % Expiration % RSA_Sig % PQC_Sig_Length % PQC_Sig)
     * </pre>
     * 
     * @throws Exception if encryption fails
     */
    private void encryptHybrid() throws Exception {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(this, tc, "encryptHybrid");
        }
        
        try {
            // Generate RSA signature first (parent class behavior)
            super.sign();
            
            // Generate PQC signature (signs payload + RSA signature)
            signPQC();
            
            // Build hybrid token structure
            String signStr = Base64Coder.toString(Base64Coder.base64Encode(super.signature));
            String ud = super.getUserData().toString();
            
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(this, tc, "encryptHybrid: userData " + ud);
            }
            
            byte[] accessID = Base64Coder.getBytes(ud);
            StringBuilder sb = new StringBuilder(DELIM);
            sb.append(getExpiration()).append(DELIM).append(signStr);
            byte[] timeAndSign = getSimpleBytes(sb.toString());
            
            // Add PQC signature with length prefix
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            baos.write(accessID);
            baos.write(timeAndSign);
            
            // Write PQC signature length (4 bytes)
            DataOutputStream dos = new DataOutputStream(baos);
            dos.writeInt(pqcSignature.length);
            dos.write(pqcSignature);
            dos.flush();
            
            byte[] toBeEnc = baos.toByteArray();
            
            // Encrypt the entire structure
            super.encryptedBytes = com.ibm.ws.crypto.ltpakeyutil.LTPAKeyUtil.encrypt(
                toBeEnc, super.sharedKey, super.cipher
            );
            
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(this, tc, "Encrypted hybrid token: " + super.encryptedBytes.length + " bytes");
            }
            
        } catch (PQCException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(this, tc, "Error encrypting hybrid token: " + e);
            }
            throw new Exception("Failed to encrypt hybrid token", e);
        } finally {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.exit(this, tc, "encryptHybrid");
            }
        }
    }
    
    /**
     * Decrypts the hybrid token and extracts both RSA and PQC signatures.
     * 
     * @throws InvalidTokenException if decryption or parsing fails
     */
    @FFDCIgnore({ Exception.class })
    private void decryptHybrid() throws InvalidTokenException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(this, tc, "decryptHybrid");
        }
        
        try {
            // Decrypt token bytes
            byte[] tokenData = com.ibm.ws.crypto.ltpakeyutil.LTPAKeyUtil.decrypt(
                super.encryptedBytes.clone(), super.sharedKey, super.cipher
            );
            
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Decrypted token data: " + tokenData.length + " bytes");
            }
            
            // Parse token structure
            String UTF8TokenString = toUTF8String(tokenData);
            String[] userFields = LTPATokenizer.parseToken(UTF8TokenString);
            java.util.Map<String, java.util.ArrayList<String>> attribs = LTPATokenizer.parseUserData(userFields[0]);
            super.userData = new UserData(attribs);
            
            String tokenString = toSimpleString(tokenData);
            String[] fields = LTPATokenizer.parseToken(tokenString);
            
            // Extract expiration
            String[] expirationArray = super.userData.getAttributes(
                com.ibm.wsspi.security.token.AttributeNameConstants.WSTOKEN_EXPIRATION
            );
            if (expirationArray != null && expirationArray[expirationArray.length - 1] != null) {
                super.expirationInMilliseconds = Long.parseLong(expirationArray[expirationArray.length - 1]);
            } else {
                super.expirationInMilliseconds = Long.parseLong(fields[1]);
            }
            
            // Extract RSA signature (always the last field before PQC signature)
            byte[] rsaSignature = Base64Coder.base64Decode(Base64Coder.getBytes(fields[fields.length - 1]));
            super.setSignature(rsaSignature);
            
            // Extract PQC signature (at the end of tokenData)
            // Find where PQC signature starts (after RSA signature)
            int pqcSigStart = findPQCSignatureStart(tokenData);
            if (pqcSigStart > 0) {
                DataInputStream dis = new DataInputStream(
                    new ByteArrayInputStream(tokenData, pqcSigStart, tokenData.length - pqcSigStart)
                );
                int pqcSigLength = dis.readInt();
                this.pqcSignature = new byte[pqcSigLength];
                dis.readFully(this.pqcSignature);
                
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "Extracted PQC signature: " + pqcSignature.length + " bytes");
                }
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "No PQC signature found in token");
                }
                this.pqcSignature = null;
            }
            
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(this, tc, "Error decrypting hybrid token: " + e);
            }
            throw new InvalidTokenException("Failed to decrypt hybrid token: " + e.getMessage(), e);
        } finally {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.exit(this, tc, "decryptHybrid");
            }
        }
    }
    
    // ========================================================================
    // Overridden Methods
    // ========================================================================
    
    /**
     * Validates the hybrid token by checking both RSA and PQC signatures.
     * 
     * <p>Validation process:</p>
     * <ol>
     *   <li>Validate token expiration</li>
     *   <li>Validate RSA signature (always required)</li>
     *   <li>Validate PQC signature (if PQC validation enabled)</li>
     * </ol>
     * 
     * @return true if token is valid
     * @throws InvalidTokenException if validation fails
     * @throws TokenExpiredException if token has expired
     */
    @Override
    @FFDCIgnore(Exception.class)
    public boolean isValid() throws InvalidTokenException, TokenExpiredException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(this, tc, "isValid");
        }
        
        try {
            // Validate expiration
            validateExpiration();
            
            // Validate RSA signature (parent class)
            boolean rsaValid = super.verify();
            if (!rsaValid) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "RSA signature validation failed");
                }
                throw new InvalidTokenException("RSA signature validation failed");
            }
            
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "RSA signature valid");
            }
            
            // Validate PQC signature (if enabled)
            if (pqcValidationEnabled) {
                boolean pqcValid = verifyPQC();
                if (!pqcValid) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(this, tc, "PQC signature validation failed");
                    }
                    throw new InvalidTokenException("PQC signature validation failed");
                }
                
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "PQC signature valid");
                }
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "PQC validation disabled, skipping PQC signature check");
                }
            }
            
            return true;
            
        } catch (PQCException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(this, tc, "PQC validation error: " + e);
            }
            throw new InvalidTokenException("PQC validation failed: " + e.getMessage(), e);
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(this, tc, "Token validation error: " + e);
            }
            throw new InvalidTokenException("Token validation failed: " + e.getMessage(), e);
        } finally {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.exit(this, tc, "isValid");
            }
        }
    }
    
    /**
     * Returns the encrypted hybrid token bytes.
     * 
     * <p>If the token has not been encrypted yet, this method generates both
     * RSA and PQC signatures and encrypts the token.</p>
     * 
     * @return encrypted hybrid token bytes
     * @throws InvalidTokenException if encryption fails
     * @throws TokenExpiredException if token has expired
     */
    @Override
    @FFDCIgnore(Exception.class)
    public byte[] getBytes() throws InvalidTokenException, TokenExpiredException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(this, tc, "getBytes");
        }
        
        try {
            if (super.encryptedBytes == null) {
                encryptHybrid();
            }
            return super.encryptedBytes.clone();
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(this, tc, "Error getting token bytes: " + e);
            }
            throw new InvalidTokenException("Failed to get token bytes: " + e.getMessage(), e);
        } finally {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.exit(this, tc, "getBytes");
            }
        }
    }
    
    /**
     * Creates a deep copy of this hybrid LTPA token.
     * 
     * @return a new copy of this token
     */
    @Override
    public Object clone() {
        UserData userdata = (UserData) super.userData.clone();
        return new LTPAToken2Hybrid(getExpiration(), super.sharedKey, super.privateKey,
                                   super.publicKey, pqcPrivateKey, pqcPublicKey,
                                   pqcAlgorithm, userdata);
    }
    
    // ========================================================================
    // Helper Methods
    // ========================================================================
    
    /**
     * Adds PQC metadata to the token payload.
     * 
     * <p>Metadata includes:</p>
     * <ul>
     *   <li>pqcEnabled: true</li>
     *   <li>pqcAlgorithm: algorithm name (e.g., "ML-DSA-65")</li>
     *   <li>pqcVersion: PQC format version (1)</li>
     * </ul>
     */
    private void addPQCMetadata() {
        super.addAttribute("pqcEnabled", "true");
        super.addAttribute("pqcAlgorithm", pqcAlgorithm.getNistName());
        super.addAttribute("pqcVersion", String.valueOf(PQC_FORMAT_VERSION));
        super.addAttribute("tokenVersion", String.valueOf(HYBRID_TOKEN_VERSION));
    }
    
    /**
     * Concatenates two byte arrays.
     * 
     * @param a first byte array
     * @param b second byte array
     * @return concatenated byte array
     */
    private byte[] concatenateBytes(byte[] a, byte[] b) {
        byte[] result = new byte[a.length + b.length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }
    
    /**
     * Finds the start position of the PQC signature in the decrypted token data.
     * 
     * <p>The PQC signature is located after the RSA signature and is prefixed
     * with a 4-byte length field.</p>
     * 
     * @param tokenData decrypted token data
     * @return start position of PQC signature, or -1 if not found
     */
    private int findPQCSignatureStart(byte[] tokenData) {
        // The PQC signature starts after the last DELIM and RSA signature
        // We need to find the position after the Base64-encoded RSA signature
        String tokenString = toSimpleString(tokenData);
        int lastDelimPos = tokenString.lastIndexOf(DELIM);
        if (lastDelimPos < 0) {
            return -1;
        }
        
        // Find the end of the Base64-encoded RSA signature
        // Base64 characters: A-Z, a-z, 0-9, +, /, =
        int pos = lastDelimPos + 1;
        while (pos < tokenString.length()) {
            char c = tokenString.charAt(pos);
            if (!isBase64Char(c)) {
                break;
            }
            pos++;
        }
        
        return pos;
    }
    
    /**
     * Checks if a character is a valid Base64 character.
     * 
     * @param c character to check
     * @return true if character is valid Base64
     */
    private boolean isBase64Char(char c) {
        return (c >= 'A' && c <= 'Z') ||
               (c >= 'a' && c <= 'z') ||
               (c >= '0' && c <= '9') ||
               c == '+' || c == '/' || c == '=';
    }
    
    /**
     * Converts byte array to UTF-8 string.
     * 
     * @param b byte array
     * @return UTF-8 string
     */
    private static String toUTF8String(byte[] b) {
        return new String(b, StandardCharsets.UTF_8);
    }
    
    /**
     * Converts byte array to simple string (8-bit characters).
     * 
     * @param b byte array
     * @return simple string
     */
    private static String toSimpleString(byte[] b) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0, len = b.length; i < len; i++) {
            sb.append((char) (b[i] & 0xff));
        }
        return sb.toString();
    }
    
    /**
     * Converts string to byte array (8-bit characters).
     * 
     * @param str string
     * @return byte array
     */
    private static byte[] getSimpleBytes(String str) {
        StringBuilder sb = new StringBuilder(str);
        byte[] b = new byte[sb.length()];
        for (int i = 0, len = sb.length(); i < len; i++) {
            b[i] = (byte) sb.charAt(i);
        }
        return b;
    }
    
    // ========================================================================
    // Getters
    // ========================================================================
    
    /**
     * Gets the PQC algorithm used for this token.
     * 
     * @return PQC algorithm
     */
    public PQCAlgorithm getPqcAlgorithm() {
        return pqcAlgorithm;
    }
    
    /**
     * Gets the PQC signature bytes.
     * 
     * @return PQC signature bytes (defensive copy), or null if not yet generated
     */
    public byte[] getPqcSignature() {
        return pqcSignature != null ? pqcSignature.clone() : null;
    }
    
    /**
     * Checks if PQC validation is enabled for this token.
     * 
     * @return true if PQC validation is enabled
     */
    public boolean isPqcValidationEnabled() {
        return pqcValidationEnabled;
    }
    
    /**
     * Gets the PQC provider name.
     * 
     * @return PQC provider name
     */
    public String getPqcProviderName() {
        return pqcProvider.getProviderName();
    }
}

// Made with Bob