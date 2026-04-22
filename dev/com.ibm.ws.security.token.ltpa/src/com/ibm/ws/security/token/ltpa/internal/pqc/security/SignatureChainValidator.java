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

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.security.token.ltpa.internal.TraceConstants;
import com.ibm.ws.security.token.ltpa.internal.pqc.PQCAlgorithm;
import com.ibm.ws.security.token.ltpa.internal.pqc.PQCException;
import com.ibm.ws.security.token.ltpa.internal.pqc.PQCProvider;

/**
 * Validates signature chains in hybrid LTPA tokens to detect tampering.
 * 
 * <p>This validator ensures the integrity of the signature chain:
 * <pre>
 * TokenPayload → RSA_Sign() → RSA_Signature
 *                                    ↓
 *              (Payload || RSA_Sig) → ML-DSA_Sign() → PQC_Signature
 * </pre>
 * 
 * <h3>Signature Chain Security</h3>
 * <p>The signature chain provides multiple layers of protection:
 * <ul>
 *   <li><b>RSA Signature</b>: Protects token payload, provides backward compatibility</li>
 *   <li><b>PQC Signature</b>: Protects payload + RSA signature, provides quantum resistance</li>
 *   <li><b>Chaining</b>: PQC signature cryptographically binds to RSA signature</li>
 *   <li><b>Tampering Detection</b>: Any modification invalidates both signatures</li>
 * </ul>
 * 
 * <h3>Validation Process</h3>
 * <ol>
 *   <li>Verify RSA signature over token payload</li>
 *   <li>Verify PQC signature over (payload || RSA_signature)</li>
 *   <li>Both signatures must be valid for token to be accepted</li>
 *   <li>Any tampering with payload or RSA signature invalidates PQC signature</li>
 * </ol>
 * 
 * <h3>Attack Resistance</h3>
 * <p>This design prevents several attack vectors:
 * <ul>
 *   <li><b>Payload Tampering</b>: Invalidates RSA signature</li>
 *   <li><b>RSA Signature Replacement</b>: Invalidates PQC signature</li>
 *   <li><b>Signature Stripping</b>: Detected by version validator</li>
 *   <li><b>Quantum Attacks</b>: PQC signature remains secure</li>
 * </ul>
 * 
 * <h3>Usage Example</h3>
 * <pre>
 * SignatureChainValidator validator = new SignatureChainValidator(pqcProvider);
 * 
 * // Validate hybrid token
 * boolean valid = validator.validateHybridToken(
 *     payload, rsaSignature, pqcSignature,
 *     rsaPublicKey, pqcPublicKey, pqcAlgorithm
 * );
 * 
 * if (!valid) {
 *     throw new SecurityException("Token signature validation failed");
 * }
 * </pre>
 * 
 * @see TokenVersionValidator
 * @see <a href="https://github.com/open-liberty/open-liberty/blob/integration/dev/com.ibm.ws.security.token.ltpa/pqc-ltpa-implementation/05-security-considerations/attack-resistance.md">Attack Resistance Documentation</a>
 */
public class SignatureChainValidator {
    
    private static final TraceComponent tc = Tr.register(SignatureChainValidator.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);
    
    private final PQCProvider pqcProvider;
    private final boolean strictValidation;
    
    /**
     * Creates a new SignatureChainValidator with the specified PQC provider.
     * 
     * @param pqcProvider the PQC provider for signature verification
     * @throws IllegalArgumentException if pqcProvider is null
     */
    public SignatureChainValidator(PQCProvider pqcProvider) {
        this(pqcProvider, true);
    }
    
    /**
     * Creates a new SignatureChainValidator with specified settings.
     * 
     * @param pqcProvider the PQC provider for signature verification
     * @param strictValidation if true, all validation failures are fatal
     * @throws IllegalArgumentException if pqcProvider is null
     */
    public SignatureChainValidator(PQCProvider pqcProvider, boolean strictValidation) {
        if (pqcProvider == null) {
            throw new IllegalArgumentException("PQC provider cannot be null");
        }
        
        this.pqcProvider = pqcProvider;
        this.strictValidation = strictValidation;
        
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "SignatureChainValidator initialized: provider=" + pqcProvider.getProviderName() +
                        ", strictValidation=" + strictValidation);
        }
    }
    
    /**
     * Validates a classical token (RSA signature only).
     * 
     * @param payload the token payload bytes
     * @param rsaSignature the RSA signature bytes
     * @param rsaPublicKey the RSA public key for verification
     * @return true if signature is valid, false otherwise
     */
    public boolean validateClassicalToken(@Sensitive byte[] payload, 
                                         @Sensitive byte[] rsaSignature,
                                         PublicKey rsaPublicKey) {
        if (tc.isEntryEnabled()) {
            Tr.entry(tc, "validateClassicalToken");
        }
        
        try {
            // Validate inputs
            if (payload == null || payload.length == 0) {
                Tr.error(tc, "CWWKS4220E: Token payload is null or empty");
                return false;
            }
            
            if (rsaSignature == null || rsaSignature.length == 0) {
                Tr.error(tc, "CWWKS4221E: RSA signature is null or empty");
                return false;
            }
            
            if (rsaPublicKey == null) {
                Tr.error(tc, "CWWKS4222E: RSA public key is null");
                return false;
            }
            
            // Verify RSA signature
            boolean valid = verifyRsaSignature(payload, rsaSignature, rsaPublicKey);
            
            if (!valid) {
                Tr.error(tc, "CWWKS4223E: RSA signature verification failed");
                Tr.audit(tc, "SECURITY: Classical token signature validation failed");
            }
            
            return valid;
            
        } finally {
            if (tc.isEntryEnabled()) {
                Tr.exit(tc, "validateClassicalToken");
            }
        }
    }
    
    /**
     * Validates a hybrid token (RSA + PQC signatures).
     * 
     * <p>Validation steps:
     * <ol>
     *   <li>Verify RSA signature over payload</li>
     *   <li>Construct chained input: payload || RSA_signature</li>
     *   <li>Verify PQC signature over chained input</li>
     *   <li>Both signatures must be valid</li>
     * </ol>
     * 
     * @param payload the token payload bytes
     * @param rsaSignature the RSA signature bytes
     * @param pqcSignature the PQC signature bytes
     * @param rsaPublicKey the RSA public key for verification
     * @param pqcPublicKey the PQC public key for verification
     * @param pqcAlgorithm the PQC algorithm used
     * @return true if both signatures are valid, false otherwise
     */
    public boolean validateHybridToken(@Sensitive byte[] payload,
                                      @Sensitive byte[] rsaSignature,
                                      @Sensitive byte[] pqcSignature,
                                      PublicKey rsaPublicKey,
                                      PublicKey pqcPublicKey,
                                      PQCAlgorithm pqcAlgorithm) {
        if (tc.isEntryEnabled()) {
            Tr.entry(tc, "validateHybridToken", pqcAlgorithm);
        }
        
        try {
            // Validate inputs
            if (payload == null || payload.length == 0) {
                Tr.error(tc, "CWWKS4220E: Token payload is null or empty");
                return false;
            }
            
            if (rsaSignature == null || rsaSignature.length == 0) {
                Tr.error(tc, "CWWKS4221E: RSA signature is null or empty");
                return false;
            }
            
            if (pqcSignature == null || pqcSignature.length == 0) {
                Tr.error(tc, "CWWKS4224E: PQC signature is null or empty");
                return false;
            }
            
            if (rsaPublicKey == null) {
                Tr.error(tc, "CWWKS4222E: RSA public key is null");
                return false;
            }
            
            if (pqcPublicKey == null) {
                Tr.error(tc, "CWWKS4225E: PQC public key is null");
                return false;
            }
            
            if (pqcAlgorithm == null) {
                Tr.error(tc, "CWWKS4226E: PQC algorithm is null");
                return false;
            }
            
            // Step 1: Verify RSA signature over payload
            boolean rsaValid = verifyRsaSignature(payload, rsaSignature, rsaPublicKey);
            
            if (!rsaValid) {
                Tr.error(tc, "CWWKS4223E: RSA signature verification failed");
                Tr.audit(tc, "SECURITY: Hybrid token RSA signature validation failed");
                return false;
            }
            
            // Step 2: Verify PQC signature over (payload || RSA_signature)
            boolean pqcValid = verifyPqcSignatureChain(payload, rsaSignature, pqcSignature, 
                                                       pqcPublicKey, pqcAlgorithm);
            
            if (!pqcValid) {
                Tr.error(tc, "CWWKS4227E: PQC signature verification failed");
                Tr.audit(tc, "SECURITY: Hybrid token PQC signature validation failed");
                return false;
            }
            
            // Both signatures valid
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Hybrid token signature chain validated successfully");
            }
            
            return true;
            
        } finally {
            if (tc.isEntryEnabled()) {
                Tr.exit(tc, "validateHybridToken");
            }
        }
    }
    
    /**
     * Verifies an RSA signature.
     * 
     * @param data the data that was signed
     * @param signature the signature to verify
     * @param publicKey the RSA public key
     * @return true if signature is valid, false otherwise
     */
    private boolean verifyRsaSignature(@Sensitive byte[] data, 
                                       @Sensitive byte[] signature,
                                       PublicKey publicKey) {
        try {
            // Use Java's standard RSA signature verification
            java.security.Signature rsaSig = java.security.Signature.getInstance("SHA256withRSA");
            rsaSig.initVerify(publicKey);
            rsaSig.update(data);
            return rsaSig.verify(signature);
            
        } catch (Exception e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "RSA signature verification exception", e);
            }
            Tr.error(tc, "CWWKS4228E: RSA signature verification error: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Verifies a PQC signature over the chained input (payload || RSA_signature).
     * 
     * @param payload the token payload
     * @param rsaSignature the RSA signature
     * @param pqcSignature the PQC signature to verify
     * @param pqcPublicKey the PQC public key
     * @param pqcAlgorithm the PQC algorithm
     * @return true if PQC signature is valid, false otherwise
     */
    private boolean verifyPqcSignatureChain(@Sensitive byte[] payload,
                                           @Sensitive byte[] rsaSignature,
                                           @Sensitive byte[] pqcSignature,
                                           PublicKey pqcPublicKey,
                                           PQCAlgorithm pqcAlgorithm) {
        byte[] chainedInput = null;
        
        try {
            // Construct chained input: payload || RSA_signature
            chainedInput = new byte[payload.length + rsaSignature.length];
            System.arraycopy(payload, 0, chainedInput, 0, payload.length);
            System.arraycopy(rsaSignature, 0, chainedInput, payload.length, rsaSignature.length);
            
            // Verify PQC signature using provider
            boolean valid = pqcProvider.verify(chainedInput, pqcSignature, pqcPublicKey, pqcAlgorithm);
            
            return valid;
            
        } catch (PQCException e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "PQC signature verification exception", e);
            }
            Tr.error(tc, "CWWKS4229E: PQC signature verification error: " + e.getMessage());
            return false;
            
        } finally {
            // Clear sensitive chained input from memory
            if (chainedInput != null) {
                Arrays.fill(chainedInput, (byte) 0);
            }
        }
    }
    
    /**
     * Generates an RSA signature for a token payload.
     * 
     * <p>This method is used during token generation.
     * 
     * @param payload the token payload to sign
     * @param privateKey the RSA private key
     * @return the RSA signature bytes
     * @throws SignatureException if signature generation fails
     */
    public byte[] generateRsaSignature(@Sensitive byte[] payload, PrivateKey privateKey) 
            throws SignatureException {
        if (tc.isEntryEnabled()) {
            Tr.entry(tc, "generateRsaSignature");
        }
        
        try {
            if (payload == null || payload.length == 0) {
                throw new SignatureException("Payload is null or empty");
            }
            
            if (privateKey == null) {
                throw new SignatureException("Private key is null");
            }
            
            // Generate RSA signature
            java.security.Signature rsaSig = java.security.Signature.getInstance("SHA256withRSA");
            rsaSig.initSign(privateKey);
            rsaSig.update(payload);
            byte[] signature = rsaSig.sign();
            
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "RSA signature generated: " + signature.length + " bytes");
            }
            
            return signature;
            
        } catch (Exception e) {
            Tr.error(tc, "CWWKS4230E: RSA signature generation error: " + e.getMessage());
            throw new SignatureException("Failed to generate RSA signature", e);
            
        } finally {
            if (tc.isEntryEnabled()) {
                Tr.exit(tc, "generateRsaSignature");
            }
        }
    }
    
    /**
     * Generates a PQC signature for a hybrid token.
     * 
     * <p>This method signs the chained input (payload || RSA_signature).
     * 
     * @param payload the token payload
     * @param rsaSignature the RSA signature
     * @param privateKey the PQC private key
     * @param pqcAlgorithm the PQC algorithm to use
     * @return the PQC signature bytes
     * @throws SignatureException if signature generation fails
     */
    public byte[] generatePqcSignatureChain(@Sensitive byte[] payload,
                                           @Sensitive byte[] rsaSignature,
                                           PrivateKey privateKey,
                                           PQCAlgorithm pqcAlgorithm) 
            throws SignatureException {
        if (tc.isEntryEnabled()) {
            Tr.entry(tc, "generatePqcSignatureChain", pqcAlgorithm);
        }
        
        byte[] chainedInput = null;
        
        try {
            if (payload == null || payload.length == 0) {
                throw new SignatureException("Payload is null or empty");
            }
            
            if (rsaSignature == null || rsaSignature.length == 0) {
                throw new SignatureException("RSA signature is null or empty");
            }
            
            if (privateKey == null) {
                throw new SignatureException("Private key is null");
            }
            
            if (pqcAlgorithm == null) {
                throw new SignatureException("PQC algorithm is null");
            }
            
            // Construct chained input: payload || RSA_signature
            chainedInput = new byte[payload.length + rsaSignature.length];
            System.arraycopy(payload, 0, chainedInput, 0, payload.length);
            System.arraycopy(rsaSignature, 0, chainedInput, payload.length, rsaSignature.length);
            
            // Generate PQC signature using provider
            byte[] pqcSignature = pqcProvider.sign(chainedInput, privateKey, pqcAlgorithm);
            
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "PQC signature generated: " + pqcSignature.length + " bytes");
            }
            
            return pqcSignature;
            
        } catch (PQCException e) {
            Tr.error(tc, "CWWKS4231E: PQC signature generation error: " + e.getMessage());
            throw new SignatureException("Failed to generate PQC signature", e);
            
        } finally {
            // Clear sensitive chained input from memory
            if (chainedInput != null) {
                Arrays.fill(chainedInput, (byte) 0);
            }
            
            if (tc.isEntryEnabled()) {
                Tr.exit(tc, "generatePqcSignatureChain");
            }
        }
    }
    
    /**
     * Exception thrown when signature generation or validation fails.
     */
    public static class SignatureException extends Exception {
        private static final long serialVersionUID = 1L;
        
        public SignatureException(String message) {
            super(message);
        }
        
        public SignatureException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

// Made with Bob
