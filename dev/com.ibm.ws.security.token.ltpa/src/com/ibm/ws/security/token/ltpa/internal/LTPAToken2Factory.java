/*******************************************************************************
 * Copyright (c) 2004, 2026 IBM Corporation and others.
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
import java.io.DataInputStream;
import java.io.IOException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.security.auth.InvalidTokenException;
import com.ibm.websphere.security.auth.TokenCreationFailedException;
import com.ibm.websphere.security.auth.TokenExpiredException;
import com.ibm.ws.crypto.ltpakeyutil.LTPAKeyUtil;
import com.ibm.ws.crypto.ltpakeyutil.LTPAPrivateKey;
import com.ibm.ws.crypto.ltpakeyutil.LTPAPublicKey;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.token.ltpa.LTPAValidationKeysInfo;
import com.ibm.ws.security.token.ltpa.internal.pqc.PQCAlgorithm;
import com.ibm.ws.security.token.ltpa.internal.pqc.PQCException;
import com.ibm.ws.security.token.ltpa.internal.pqc.PQCProviderFactory;
import com.ibm.wsspi.security.ltpa.Token;
import com.ibm.wsspi.security.ltpa.TokenFactory;

/**
 * Factory for creating and validating LTPA tokens with support for both classical
 * and hybrid (PQC-enhanced) tokens.
 *
 * <p>This factory automatically routes token creation and validation based on:</p>
 * <ul>
 *   <li><b>Token Creation:</b> Configuration determines whether to create classical or hybrid tokens</li>
 *   <li><b>Token Validation:</b> Token version byte determines which validator to use</li>
 * </ul>
 *
 * <h3>Token Version Detection:</h3>
 * <p>After decrypting the token bytes, the first byte of the payload indicates the token version:</p>
 * <ul>
 *   <li><b>Version 0x00:</b> Classical LTPAToken2 (RSA signature only)</li>
 *   <li><b>Version 0x01:</b> Hybrid LTPAToken2 (RSA + PQC signatures)</li>
 * </ul>
 *
 * <h3>Configuration-Based Token Creation:</h3>
 * <p>Token creation type is determined by configuration parameters:</p>
 * <ul>
 *   <li><b>pqcEnabled=true:</b> Create hybrid tokens with PQC signatures</li>
 *   <li><b>pqcEnabled=false:</b> Create classical tokens (default)</li>
 *   <li><b>pqcAlgorithm:</b> ML-DSA-65 (default) or ML-DSA-87</li>
 * </ul>
 *
 * <h3>Backward Compatibility:</h3>
 * <ul>
 *   <li>Classical tokens always validate successfully on all servers</li>
 *   <li>Hybrid tokens validate on both classical and hybrid servers</li>
 *   <li>Classical servers ignore PQC signature in hybrid tokens</li>
 *   <li>No breaking changes to existing TokenFactory API</li>
 * </ul>
 *
 * @see LTPAToken2
 * @see LTPAToken2Hybrid
 * @since 1.0
 */
public class LTPAToken2Factory implements TokenFactory {
    private static final TraceComponent tc = Tr.register(LTPAToken2Factory.class);
    
    /**
     * Token version for classical LTPA tokens (0x00).
     */
    private static final byte CLASSICAL_TOKEN_VERSION = 0x00;
    
    /**
     * Token version for hybrid LTPA tokens (0x01).
     */
    private static final byte HYBRID_TOKEN_VERSION = 0x01;
    
    // ========================================================================
    // Classical LTPA Configuration Fields
    // ========================================================================
    
    private long expirationInMinutes;
    private byte[] primarySharedKey;
    private LTPAPublicKey primaryPublicKey;
    private LTPAPrivateKey primaryPrivateKey;
    private CopyOnWriteArrayList<LTPAValidationKeysInfo> validationKeys;
    private long expDiffAllowed;
    
    // ========================================================================
    // PQC Configuration Fields
    // ========================================================================
    
    /**
     * Flag indicating whether PQC is enabled for token creation.
     * When true, hybrid tokens are created; when false, classical tokens are created.
     */
    private boolean pqcEnabled = false;
    
    /**
     * PQC algorithm to use for hybrid token creation.
     * Default: ML-DSA-65 (NIST Security Level 3).
     */
    private PQCAlgorithm pqcAlgorithm = PQCAlgorithm.ML_DSA_65;
    
    /**
     * PQC private key for hybrid token signature generation.
     */
    private PrivateKey pqcPrivateKey;
    
    /**
     * PQC public key for hybrid token signature verification.
     */
    private PublicKey pqcPublicKey;

    // ========================================================================
    // Initialization
    // ========================================================================
    
    /**
     * Initializes the token factory with configuration parameters.
     *
     * <p>This method extracts both classical LTPA configuration and PQC configuration
     * from the provided map. PQC configuration is optional; if not provided, the factory
     * operates in classical mode only.</p>
     *
     * @param tokenFactoryMap configuration map containing LTPA and PQC parameters
     */
    @SuppressWarnings("unchecked")
    @Override
    public void initialize(@Sensitive Map tokenFactoryMap) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "initialize");
        }
        
        // Initialize classical LTPA configuration
        expirationInMinutes = (Long) tokenFactoryMap.get(LTPAConstants.EXPIRATION);
        primarySharedKey = (byte[]) tokenFactoryMap.get(LTPAConstants.PRIMARY_SECRET_KEY);
        primaryPublicKey = (LTPAPublicKey) tokenFactoryMap.get(LTPAConstants.PRIMARY_PUBLIC_KEY);
        primaryPrivateKey = (LTPAPrivateKey) tokenFactoryMap.get(LTPAConstants.PRIMARY_PRIVATE_KEY);
        expDiffAllowed = (Long) tokenFactoryMap.get(LTPAConfigurationImpl.KEY_EXP_DIFF_ALLOWED);
        validationKeys = (CopyOnWriteArrayList<LTPAValidationKeysInfo>) tokenFactoryMap.get(LTPAConstants.VALIDATION_KEYS);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Number of validationKeys: " + validationKeys.size());
        }
        
        // Initialize PQC configuration (optional)
        initializePQCConfiguration(tokenFactoryMap);
        
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "initialize", "pqcEnabled=" + pqcEnabled);
        }
    }
    
    /**
     * Initializes PQC-specific configuration from the token factory map.
     *
     * <p>This method is called during factory initialization to extract PQC configuration.
     * If PQC configuration is not present or incomplete, PQC mode is disabled and the
     * factory operates in classical mode only.</p>
     *
     * @param tokenFactoryMap configuration map
     */
    @FFDCIgnore(Exception.class)
    private void initializePQCConfiguration(@Sensitive Map tokenFactoryMap) {
        try {
            // Check if PQC is enabled in configuration
            Boolean pqcEnabledConfig = (Boolean) tokenFactoryMap.get(LTPAConstants.PQC_ENABLED);
            if (pqcEnabledConfig != null && pqcEnabledConfig) {
                // Extract PQC keys
                pqcPrivateKey = (PrivateKey) tokenFactoryMap.get(LTPAConstants.PQC_PRIVATE_KEY);
                pqcPublicKey = (PublicKey) tokenFactoryMap.get(LTPAConstants.PQC_PUBLIC_KEY);
                
                // Extract PQC algorithm (default to ML-DSA-65)
                String pqcAlgorithmName = (String) tokenFactoryMap.get(LTPAConstants.PQC_ALGORITHM);
                if (pqcAlgorithmName != null) {
                    try {
                        pqcAlgorithm = PQCAlgorithm.fromString(pqcAlgorithmName);
                    } catch (IllegalArgumentException e) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Invalid PQC algorithm: " + pqcAlgorithmName + ", using default ML-DSA-65");
                        }
                        pqcAlgorithm = PQCAlgorithm.ML_DSA_65;
                    }
                }
                
                // Validate PQC configuration
                if (pqcPrivateKey != null && pqcPublicKey != null) {
                    // Verify PQC provider is available
                    try {
                        PQCProviderFactory.getProvider();
                        pqcEnabled = true;
                        
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "PQC enabled with algorithm: " + pqcAlgorithm);
                        }
                    } catch (PQCException e) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "PQC provider not available, disabling PQC: " + e.getMessage());
                        }
                        pqcEnabled = false;
                    }
                } else {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "PQC keys not configured, disabling PQC");
                    }
                    pqcEnabled = false;
                }
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "PQC not enabled in configuration");
                }
                pqcEnabled = false;
            }
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Error initializing PQC configuration, disabling PQC: " + e.getMessage());
            }
            pqcEnabled = false;
        }
    }
    
    // ========================================================================
    // Token Creation
    // ========================================================================
    
    /**
     * Creates a new LTPA token based on configuration.
     *
     * <p>Token type is determined by PQC configuration:</p>
     * <ul>
     *   <li><b>PQC Enabled:</b> Creates hybrid token with RSA + PQC signatures</li>
     *   <li><b>PQC Disabled:</b> Creates classical token with RSA signature only</li>
     * </ul>
     *
     * @param tokenData map containing user unique ID
     * @return new LTPA token (classical or hybrid)
     * @throws TokenCreationFailedException if token creation fails
     */
    @Override
    public Token createToken(Map tokenData) throws TokenCreationFailedException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "createToken", "pqcEnabled=" + pqcEnabled);
        }
        
        try {
            String userUniqueId = getUniqueId(tokenData);
            
            Token token;
            if (isPQCEnabled()) {
                // Create hybrid token with PQC signature
                token = createHybridToken(userUniqueId);
                
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Created hybrid LTPA token with " + pqcAlgorithm);
                }
            } else {
                // Create classical token
                token = new LTPAToken2(userUniqueId, expirationInMinutes, primarySharedKey,
                                      primaryPrivateKey, primaryPublicKey);
                
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Created classical LTPA token");
                }
            }
            
            return token;
            
        } finally {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.exit(tc, "createToken");
            }
        }
    }
    
    /**
     * Creates a hybrid LTPA token with both RSA and PQC signatures.
     *
     * @param userUniqueId unique user identifier
     * @return new hybrid LTPA token
     * @throws TokenCreationFailedException if hybrid token creation fails
     */
    private Token createHybridToken(String userUniqueId) throws TokenCreationFailedException {
        try {
            return new LTPAToken2Hybrid(userUniqueId, expirationInMinutes, primarySharedKey,
                                       primaryPrivateKey, primaryPublicKey,
                                       pqcPrivateKey, pqcPublicKey, pqcAlgorithm);
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Failed to create hybrid token: " + e.getMessage());
            }
            
            // Fall back to classical token if hybrid creation fails
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Falling back to classical token creation");
            }
            
            return new LTPAToken2(userUniqueId, expirationInMinutes, primarySharedKey,
                                 primaryPrivateKey, primaryPublicKey);
        }
    }

    // ========================================================================
    // Token Validation
    // ========================================================================
    
    /**
     * Validates token bytes using automatic version detection.
     *
     * <p>This method delegates to {@link #validateTokenBytes(byte[], String...)}
     * with no attributes to remove.</p>
     *
     * @param tokenBytes encrypted token bytes
     * @return validated token (classical or hybrid)
     * @throws InvalidTokenException if token validation fails
     * @throws TokenExpiredException if token has expired
     */
    @Override
    public Token validateTokenBytes(byte[] tokenBytes) throws InvalidTokenException, TokenExpiredException {
        return validateTokenBytes(tokenBytes, (String[]) null);
    }

    /**
     * Validates token bytes with automatic version detection and attribute removal.
     *
     * <p>This method implements the core validation logic with automatic routing:</p>
     * <ol>
     *   <li>Decrypt token bytes to access payload</li>
     *   <li>Detect token version from payload</li>
     *   <li>Route to appropriate validator (classical or hybrid)</li>
     *   <li>Try validation keys if primary key fails</li>
     * </ol>
     *
     * @param tokenBytes encrypted token bytes
     * @param removeAttributes attributes to remove from token after validation
     * @return validated token (classical or hybrid)
     * @throws InvalidTokenException if token validation fails
     * @throws TokenExpiredException if token has expired
     */
    @FFDCIgnore(Exception.class)
    @Override
    public Token validateTokenBytes(byte[] tokenBytes, String... removeAttributes) throws InvalidTokenException, TokenExpiredException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "validateTokenBytes");
        }
        
        Token validatedToken = null;

        // Validate with primary keys
        if (primarySharedKey != null && primaryPrivateKey != null && primaryPublicKey != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "validateTokenBytes with primary keys");
            }

            try {
                // Detect token version and route to appropriate validator
                byte tokenVersion = detectTokenVersion(tokenBytes, primarySharedKey);
                
                if (tokenVersion == HYBRID_TOKEN_VERSION && isPQCEnabled()) {
                    // Validate as hybrid token
                    validatedToken = validateHybridToken(tokenBytes, primarySharedKey,
                                                        primaryPrivateKey, primaryPublicKey,
                                                        pqcPrivateKey, pqcPublicKey,
                                                        expDiffAllowed, removeAttributes);
                } else {
                    // Validate as classical token (version 0x00 or PQC not enabled)
                    validatedToken = new LTPAToken2(tokenBytes, primarySharedKey, primaryPrivateKey,
                                                   primaryPublicKey, expDiffAllowed, removeAttributes);
                }
                
                if (validatedToken != null) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "validateTokenBytes with primary keys (success)");
                    }
                    return validatedToken;
                }
            } catch (Exception e) {
                // If the token is expired, don't try validation keys
                if (e instanceof com.ibm.websphere.security.auth.TokenExpiredException) {
                    if (tc.isEntryEnabled())
                        Tr.exit(tc, "validateTokenBytes (expired)");
                    throw (com.ibm.websphere.security.auth.TokenExpiredException) e;
                }
                // Invalid token exceptions should continue to check other keys below
            }
        }

        // Try validation keys (secondary keys)
        if (validationKeys != null && !validationKeys.isEmpty()) {
            Exception lastException = null;

            Iterator<LTPAValidationKeysInfo> validationKeysIterator = validationKeys.iterator();

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "go through " + validationKeys.size() + " validationKeys");
            }
            
            while (validationKeysIterator.hasNext()) {
                LTPAValidationKeysInfo ltpaKeyInfo = validationKeysIterator.next();
                byte[] sharedKeyForValidation = ltpaKeyInfo.getSecretKey();
                LTPAPrivateKey ltpaPrivateKeyForValidation = ltpaKeyInfo.getLTPAPrivateKey();
                LTPAPublicKey ltpaPublicKeyForValidation = ltpaKeyInfo.getLTPAPublicKey();
                
                if (ltpaKeyInfo.isValidUntilDateExpired()) {
                    validationKeys.remove(ltpaKeyInfo);
                } else {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "validateTokenBytes with validationKeys: " + ltpaKeyInfo);
                    }
                    
                    if (sharedKeyForValidation != null && ltpaPrivateKeyForValidation != null && ltpaPublicKeyForValidation != null) {
                        try {
                            // Detect token version for validation key
                            byte tokenVersion = detectTokenVersion(tokenBytes, sharedKeyForValidation);
                            
                            if (tokenVersion == HYBRID_TOKEN_VERSION && isPQCEnabled()) {
                                // Validate as hybrid token
                                validatedToken = validateHybridToken(tokenBytes, sharedKeyForValidation,
                                                                    ltpaPrivateKeyForValidation, ltpaPublicKeyForValidation,
                                                                    pqcPrivateKey, pqcPublicKey,
                                                                    expDiffAllowed, removeAttributes);
                            } else {
                                // Validate as classical token
                                validatedToken = new LTPAToken2(tokenBytes, sharedKeyForValidation,
                                                               ltpaPrivateKeyForValidation, ltpaPublicKeyForValidation,
                                                               expDiffAllowed, removeAttributes);
                            }
                            
                            if (validatedToken != null) {
                                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                    Tr.debug(tc, "validateTokenBytes with validationKeys (success)");
                                }
                                return validatedToken;
                            }
                        } catch (Exception e) {
                            if (e instanceof com.ibm.websphere.security.auth.TokenExpiredException) {
                                if (tc.isEntryEnabled())
                                    Tr.exit(tc, "validateTokenBytes (expired)");
                                throw (com.ibm.websphere.security.auth.TokenExpiredException) e;
                            }

                            lastException = e;
                            // no ffdc needed.
                            Tr.debug(tc, "Exception validating LTPAToken using validation keys.", new Object[] { e.getMessage() });
                        }
                    }
                }
            }

            if (lastException != null && lastException instanceof com.ibm.websphere.security.auth.InvalidTokenException) {
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "validateTokenBytes (invalid token)");
                throw (com.ibm.websphere.security.auth.InvalidTokenException) lastException;
            } else if (lastException != null) {
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "validateTokenBytes (" + lastException.getClass().getName() + ")");
                throw new com.ibm.websphere.security.auth.InvalidTokenException(lastException.getMessage(), lastException);
            } else {
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "validateTokenBytes (unknown error)");
                throw new com.ibm.websphere.security.auth.InvalidTokenException("Error validating LTPA token.");
            }
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "validateTokenBytes (no keys)");
        throw new com.ibm.websphere.security.auth.InvalidTokenException("Token factory not properly initialized.");
    }
    
    // ========================================================================
    // Version Detection & Routing
    // ========================================================================
    
    /**
     * Detects the token version from encrypted token bytes.
     *
     * <p>This method decrypts the token bytes and examines the first byte of the payload
     * to determine the token version:</p>
     * <ul>
     *   <li><b>0x00:</b> Classical LTPAToken2</li>
     *   <li><b>0x01:</b> Hybrid LTPAToken2</li>
     * </ul>
     *
     * <p>If version detection fails, the method assumes classical token format (0x00)
     * for backward compatibility.</p>
     *
     * @param tokenBytes encrypted token bytes
     * @param sharedKey shared key for decryption
     * @return token version byte (0x00 or 0x01)
     */
    @FFDCIgnore(Exception.class)
    private byte detectTokenVersion(byte[] tokenBytes, @Sensitive byte[] sharedKey) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "detectTokenVersion");
        }
        
        try {
            // Decrypt token to access payload
            byte[] decryptedBytes = LTPAKeyUtil.decrypt(tokenBytes.clone(), sharedKey, "AES/CBC/PKCS5Padding");
            
            if (decryptedBytes != null && decryptedBytes.length > 0) {
                // Check if this looks like a hybrid token by examining structure
                // Hybrid tokens have a specific format with version byte
                try (ByteArrayInputStream bais = new ByteArrayInputStream(decryptedBytes);
                     DataInputStream dis = new DataInputStream(bais)) {
                    
                    // Try to read version byte
                    byte version = dis.readByte();
                    
                    if (version == HYBRID_TOKEN_VERSION) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Detected hybrid token (version 0x01)");
                        }
                        return HYBRID_TOKEN_VERSION;
                    }
                } catch (IOException e) {
                    // Not a hybrid token format, assume classical
                }
            }
            
            // Default to classical token
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Detected classical token (version 0x00)");
            }
            return CLASSICAL_TOKEN_VERSION;
            
        } catch (Exception e) {
            // If detection fails, assume classical token for backward compatibility
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Version detection failed, assuming classical token: " + e.getMessage());
            }
            return CLASSICAL_TOKEN_VERSION;
        } finally {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.exit(tc, "detectTokenVersion");
            }
        }
    }
    
    /**
     * Validates a hybrid LTPA token.
     *
     * @param tokenBytes encrypted token bytes
     * @param sharedKey shared key for decryption
     * @param rsaPrivateKey RSA private key
     * @param rsaPublicKey RSA public key
     * @param pqcPrivateKey PQC private key
     * @param pqcPublicKey PQC public key
     * @param expDiffAllowed expiration difference allowed
     * @param removeAttributes attributes to remove
     * @return validated hybrid token
     * @throws InvalidTokenException if validation fails
     * @throws TokenExpiredException if token has expired
     */
    private Token validateHybridToken(byte[] tokenBytes, @Sensitive byte[] sharedKey,
                                     LTPAPrivateKey rsaPrivateKey, LTPAPublicKey rsaPublicKey,
                                     PrivateKey pqcPrivateKey, PublicKey pqcPublicKey,
                                     long expDiffAllowed, String... removeAttributes)
            throws InvalidTokenException, TokenExpiredException {
        
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Validating hybrid token");
        }
        
        return new LTPAToken2Hybrid(tokenBytes, sharedKey, rsaPrivateKey, rsaPublicKey,
                                   pqcPrivateKey, pqcPublicKey, expDiffAllowed);
    }
    
    // ========================================================================
    // Helper Methods
    // ========================================================================
    
    /**
     * Extracts the unique user ID from token data.
     *
     * @param tokenData token data map
     * @return unique user ID
     * @throws TokenCreationFailedException if unique ID is missing or empty
     */
    private String getUniqueId(Map tokenData) throws TokenCreationFailedException {
        String userUniqueId = (String) tokenData.get(LTPAConstants.UNIQUE_ID);
        if ((userUniqueId == null) || (userUniqueId.length() == 0)) {
            Tr.error(tc, "LTPA_TOKEN_SERVICE_INVALID_UNIQUE_ID");
            String formattedMessage = Tr.formatMessage(tc, "LTPA_TOKEN_SERVICE_INVALID_UNIQUE_ID");
            throw new TokenCreationFailedException(formattedMessage);
        }
        return userUniqueId;
    }
    
    /**
     * Checks if PQC is enabled and properly configured.
     *
     * @return true if PQC is enabled and keys are available
     */
    private boolean isPQCEnabled() {
        return pqcEnabled && pqcPrivateKey != null && pqcPublicKey != null;
    }

}
