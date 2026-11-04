/*******************************************************************************
 * Copyright (c) 2004, 2025 IBM Corporation and others.
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

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.security.auth.InvalidTokenException;
import com.ibm.websphere.security.auth.TokenCreationFailedException;
import com.ibm.websphere.security.auth.TokenExpiredException;
import com.ibm.ws.crypto.ltpakeyutil.LTPAPrivateKey;
import com.ibm.ws.crypto.ltpakeyutil.LTPAPublicKey;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.token.ltpa.LTPAConfiguration;
import com.ibm.ws.security.token.ltpa.LTPAKeyInfoManager;
import com.ibm.ws.security.token.ltpa.LTPAValidationKeysInfo;
import com.ibm.wsspi.security.ltpa.Token;
import com.ibm.wsspi.security.ltpa.TokenFactory;

import com.ibm.ws.security.token.ltpa.pqc.PQCConstants;
import com.ibm.ws.security.token.ltpa.pqc.PQCSignatureHelper;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

public class LTPAToken2Factory implements TokenFactory {
    private static final TraceComponent tc = Tr.register(LTPAToken2Factory.class);
    private long expirationInMinutes;
    private byte[] primarySharedKey;
    private LTPAPublicKey primaryPublicKey;
    private LTPAPrivateKey primaryPrivateKey;
    private CopyOnWriteArrayList<LTPAValidationKeysInfo> validationKeys;
    private long expDiffAllowed;
  
    // PQC: Configuration and key management (Issue #35556 - Task 2.7)
    private LTPAConfiguration ltpaConfig;
    private LTPAKeyInfoManager keyInfoMgr;
    private String primaryKeyFile;

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public void initialize(@Sensitive Map tokenFactoryMap) {
        expirationInMinutes = (Long) tokenFactoryMap.get(LTPAConstants.EXPIRATION);
        primarySharedKey = (byte[]) tokenFactoryMap.get(LTPAConstants.PRIMARY_SECRET_KEY);
        primaryPublicKey = (LTPAPublicKey) tokenFactoryMap.get(LTPAConstants.PRIMARY_PUBLIC_KEY);
        primaryPrivateKey = (LTPAPrivateKey) tokenFactoryMap.get(LTPAConstants.PRIMARY_PRIVATE_KEY);
        expDiffAllowed = (Long) tokenFactoryMap.get(LTPAConfigurationImpl.KEY_EXP_DIFF_ALLOWED);
        validationKeys = (CopyOnWriteArrayList<LTPAValidationKeysInfo>) tokenFactoryMap.get(LTPAConstants.VALIDATION_KEYS);

        // PQC: Get configuration and key manager (Issue #35556 - Task 2.7)
        ltpaConfig = (LTPAConfiguration) tokenFactoryMap.get("ltpaConfiguration");
        keyInfoMgr = (LTPAKeyInfoManager) tokenFactoryMap.get("keyInfoManager");
        primaryKeyFile = (String) tokenFactoryMap.get("primaryKeyFile");

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Number of validationKeys: " + validationKeys.size());
        }
    }

        /**
         * Load ML-DSA keys from key info manager.
         *
         * @param keyInfoMgr The key info manager
         * @param keyFile The key file name
         * @param provider The crypto provider
         * @return Array containing [PrivateKey, PublicKey] or null if not available
         */
        private Object[] loadMLDSAKeys(LTPAKeyInfoManager keyInfoMgr, String keyFile, String provider) {
                try {
                        byte[] privateKeyBytes = keyInfoMgr.getMLDSAPrivateKey(keyFile);
                        byte[] publicKeyBytes = keyInfoMgr.getMLDSAPublicKey(keyFile);

                        if (privateKeyBytes == null || publicKeyBytes == null) {
                                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                        Tr.debug(tc, "ML-DSA keys not found in key file");
                                }
                                return null;
                        }

                        KeyFactory keyFactory = KeyFactory.getInstance("ML-DSA"); // Use default SUN provider

                        PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
                        PrivateKey privateKey = keyFactory.generatePrivate(privateKeySpec);

                        X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyBytes);
                        PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);

                        return new Object[] { privateKey, publicKey };

                } catch (Exception e) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                Tr.debug(tc, "Error loading ML-DSA keys: " + e.getMessage());
                        }
                        return null;
                }
        }      
    /** {@inheritDoc} */
    @Override
    public Token createToken(Map tokenData) throws TokenCreationFailedException {
        String userUniqueId = getUniqueId(tokenData);
        
        // PQC: Check if PQC is enabled (Issue #35556 - Task 2.7)
        String cryptoMode = ltpaConfig != null ? ltpaConfig.getCryptoMode() : PQCConstants.CRYPTO_MODE_CLASSICAL;
        
        if (PQCConstants.CRYPTO_MODE_PQC.equals(cryptoMode) || 
            PQCConstants.CRYPTO_MODE_HYBRID.equals(cryptoMode)) {
            
            // Try to load ML-DSA keys
            Object[] mldsaKeys = loadMLDSAKeys(keyInfoMgr, primaryKeyFile, null);
            
            if (mldsaKeys != null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Creating PQC token with crypto mode: " + cryptoMode);
                }
                return new LTPAToken2(userUniqueId, expirationInMinutes,
                                    primarySharedKey, primaryPrivateKey, primaryPublicKey,
                                    (PrivateKey) mldsaKeys[0], (PublicKey) mldsaKeys[1], cryptoMode);
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isWarningEnabled()) {
                    Tr.warning(tc, "ML-DSA keys not available, falling back to classical mode");
                }
            }
        }
        
        // Classical mode (default)
        return new LTPAToken2(userUniqueId, expirationInMinutes,
                            primarySharedKey, primaryPrivateKey, primaryPublicKey);

    }

    private String getUniqueId(Map tokenData) throws TokenCreationFailedException {
        String userUniqueId = (String) tokenData.get(LTPAConstants.UNIQUE_ID);
        if ((userUniqueId == null) || (userUniqueId.length() == 0)) {
            Tr.error(tc, "LTPA_TOKEN_SERVICE_INVALID_UNIQUE_ID");
            String formattedMessage = Tr.formatMessage(tc, "LTPA_TOKEN_SERVICE_INVALID_UNIQUE_ID");
            throw new TokenCreationFailedException(formattedMessage);
        }
        return userUniqueId;
    }

    /** {@inheritDoc} */
    @Override
    public Token validateTokenBytes(byte[] tokenBytes) throws InvalidTokenException, TokenExpiredException {
    // TODO: PQC #35556 - Task 2.7: Add PQC token validation
    // After loading RSA keys:
    // 1. Detect token version from token bytes (classical vs PQC vs hybrid)
    // 2. If PQC or hybrid, load ML-DSA public keys using loadMLDSAKeys()
    // 3. Pass to LTPAToken2 for verification
    // 4. LTPAToken2.isValid() will use PQCSignatureHelper for verification
    //
    // Note: Hybrid mode requires BOTH RSA and ML-DSA signatures to be valid
        return validateTokenBytes(tokenBytes, (String[]) null);
    }

    /** {@inheritDoc} */
    @FFDCIgnore(Exception.class)
    @Override
    public Token validateTokenBytes(byte[] tokenBytes, String... removeAttributes) throws InvalidTokenException, TokenExpiredException {
        Token validatedToken = null;
  
        // PQC: Try to load ML-DSA keys if PQC is configured (Issue #35556 - Task 2.7)
        String cryptoMode = ltpaConfig != null ? ltpaConfig.getCryptoMode() : PQCConstants.CRYPTO_MODE_CLASSICAL;
        Object[] mldsaKeys = null;
        
        if (PQCConstants.CRYPTO_MODE_PQC.equals(cryptoMode) || 
            PQCConstants.CRYPTO_MODE_HYBRID.equals(cryptoMode)) {
            mldsaKeys = loadMLDSAKeys(keyInfoMgr, primaryKeyFile, null);
            if (mldsaKeys == null && TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "ML-DSA keys not available for validation, will try classical mode");
            }
        }

        // primary key for create and validation
        if (primarySharedKey != null && primaryPrivateKey != null && primaryPublicKey != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "validateTokenBytes with primary keys");
            }

            try {
                if (mldsaKeys != null) {
                    validatedToken = new LTPAToken2(tokenBytes, primarySharedKey, primaryPrivateKey, primaryPublicKey,
                                                   (PrivateKey) mldsaKeys[0], (PublicKey) mldsaKeys[1], cryptoMode, expDiffAllowed, removeAttributes);
                } else {
                    validatedToken = new LTPAToken2(tokenBytes, primarySharedKey, primaryPrivateKey, primaryPublicKey, expDiffAllowed, removeAttributes);
                }
                if (validatedToken != null) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "validateTokenBytes with primary keys (success)");
                    }
                    return validatedToken;
                }
            } catch (Exception e) {
                //If the token is expired then we do not want to continue processing validation keys below
                if (e instanceof com.ibm.websphere.security.auth.TokenExpiredException) {
                    if (tc.isEntryEnabled())
                        Tr.exit(tc, "validateTokenBytes (expired)");
                    throw (com.ibm.websphere.security.auth.TokenExpiredException) e;
                }
                //invalidToken exceptions should continue to check other keys below
            }
        }

        // validation keys (secondary keys)
        if (validationKeys != null && !validationKeys.isEmpty()) {
            Exception lastException = null;

            Iterator<LTPAValidationKeysInfo> validationKeysIterator = validationKeys.iterator();

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "go through " + validationKeys.size() + " validationKeys");
            }
            while (validationKeysIterator.hasNext()) { // go through all validation keys until successfully validated the token
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
                            if (mldsaKeys != null) {
                                validatedToken = new LTPAToken2(tokenBytes, sharedKeyForValidation, ltpaPrivateKeyForValidation, ltpaPublicKeyForValidation,
                                                               (PrivateKey) mldsaKeys[0], (PublicKey) mldsaKeys[1], cryptoMode, expDiffAllowed, removeAttributes);
                            } else {
                                validatedToken = new LTPAToken2(tokenBytes, sharedKeyForValidation, ltpaPrivateKeyForValidation, ltpaPublicKeyForValidation, expDiffAllowed, removeAttributes);
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

}
