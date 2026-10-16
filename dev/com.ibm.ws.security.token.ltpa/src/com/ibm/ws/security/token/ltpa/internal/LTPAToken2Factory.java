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
import com.ibm.ws.kernel.productinfo.ProductInfo;
import com.ibm.ws.security.token.ltpa.LTPAValidationKeysInfo;
import com.ibm.wsspi.security.ltpa.Token;
import com.ibm.wsspi.security.ltpa.TokenFactory;

public class LTPAToken2Factory implements TokenFactory {
    private static final TraceComponent tc = Tr.register(LTPAToken2Factory.class);
    private long expirationInMinutes;
    private long refreshThresholdInMinutes;
    private long inactivityTimeoutInMinutes;
    private boolean dynamicExpirationValidation;
    private byte[] primarySharedKey;
    private LTPAPublicKey primaryPublicKey;
    private LTPAPrivateKey primaryPrivateKey;
    private CopyOnWriteArrayList<LTPAValidationKeysInfo> validationKeys;
    private long expDiffAllowed;

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public void initialize(@Sensitive Map tokenFactoryMap) {
        expirationInMinutes = (Long) tokenFactoryMap.get(LTPAConstants.EXPIRATION);
        refreshThresholdInMinutes = (long) tokenFactoryMap.get(LTPAConstants.REFRESH_THRESHOLD);
        inactivityTimeoutInMinutes = (Long) tokenFactoryMap.get(LTPAConstants.INACTIVITY_TIMEOUT);
        Boolean dynExpVal = (Boolean) tokenFactoryMap.get(LTPAConstants.DYNAMIC_EXPIRATION_VALIDATION);
        dynamicExpirationValidation = (dynExpVal != null) ? dynExpVal : false;
        primarySharedKey = (byte[]) tokenFactoryMap.get(LTPAConstants.PRIMARY_SECRET_KEY);
        primaryPublicKey = (LTPAPublicKey) tokenFactoryMap.get(LTPAConstants.PRIMARY_PUBLIC_KEY);
        primaryPrivateKey = (LTPAPrivateKey) tokenFactoryMap.get(LTPAConstants.PRIMARY_PRIVATE_KEY);
        expDiffAllowed = (Long) tokenFactoryMap.get(LTPAConfigurationImpl.KEY_EXP_DIFF_ALLOWED);
        validationKeys = (CopyOnWriteArrayList<LTPAValidationKeysInfo>) tokenFactoryMap.get(LTPAConstants.VALIDATION_KEYS);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Number of validationKeys: " + validationKeys.size());
        }
    }

    /** {@inheritDoc} */
    @Override
    public Token createToken(Map tokenData) throws TokenCreationFailedException {
        String userUniqueId = getUniqueId(tokenData);
        return new LTPAToken2(userUniqueId, expirationInMinutes, refreshThresholdInMinutes, inactivityTimeoutInMinutes, dynamicExpirationValidation, primarySharedKey, primaryPrivateKey, primaryPublicKey);
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
        return validateTokenBytes(tokenBytes, (String[]) null);
    }

    /** {@inheritDoc} */
    @FFDCIgnore(Exception.class)
    @Override
    public Token validateTokenBytes(byte[] tokenBytes, String... removeAttributes) throws InvalidTokenException, TokenExpiredException {
        Token validatedToken = null;

        // primary key for create and validation
        if (primarySharedKey != null && primaryPrivateKey != null && primaryPublicKey != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "validateTokenBytes with primary keys");
            }

            try {

                // Start timing
                long startTime = 0;
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    startTime = System.nanoTime();
                }

                Token returnToken = null;

                validatedToken = new LTPAToken2(tokenBytes, primarySharedKey, primaryPrivateKey, primaryPublicKey, expDiffAllowed, expirationInMinutes, refreshThresholdInMinutes, inactivityTimeoutInMinutes, dynamicExpirationValidation, removeAttributes);
                if (validatedToken != null) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "validateTokenBytes with primary keys (success)");
                    }
                    // Beta guard: Token refresh is only available in beta edition
                    if (ProductInfo.getBetaEdition() && validatedToken.shouldRefreshToken()) {
                        returnToken = (Token) validatedToken.clone();
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            debugMeasureTime(startTime, true);
                        }
                    } else {
                        returnToken = validatedToken;
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            debugMeasureTime(startTime, false);
                        }
                    }
                }

                return returnToken;

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
                            validatedToken = new LTPAToken2(tokenBytes, sharedKeyForValidation, ltpaPrivateKeyForValidation, ltpaPublicKeyForValidation, expDiffAllowed, expirationInMinutes, refreshThresholdInMinutes, inactivityTimeoutInMinutes, dynamicExpirationValidation, removeAttributes);
                            if (validatedToken != null) {
                                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                    Tr.debug(tc, "validateTokenBytes with validationKeys (success)");
                                }

                                // Beta guard: Token refresh is only available in beta edition
                                if (ProductInfo.getBetaEdition() && validatedToken.shouldRefreshToken())
                                    return (Token) validatedToken.clone();
                                else
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

    /**
     * @param startTime
     */
    private void debugMeasureTime(long startTime, boolean clone) {
        long endTime = System.nanoTime();

        String msg = null;
        if (clone) {
            msg = "validateTokenBytes() and clone took ";
        } else {
            msg = "validateTokenBytes() took ";
        }

        // Calculate duration in milliseconds
        long durationMs = (endTime - startTime) / 1_000_000;
        // Or in seconds (with decimals)
        double durationSeconds = (endTime - startTime) / 1_000_000_000.0;
        Tr.debug(tc, msg + "milliseconds: " + durationMs + " seconds: " + durationSeconds);
    }

}
