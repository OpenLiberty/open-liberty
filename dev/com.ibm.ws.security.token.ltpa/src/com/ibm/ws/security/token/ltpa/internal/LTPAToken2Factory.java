/*******************************************************************************
 * Copyright (c) 2004, 2023 IBM Corporation and others.
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
import java.util.List;
import java.util.Map;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.security.auth.InvalidTokenException;
import com.ibm.websphere.security.auth.TokenCreationFailedException;
import com.ibm.websphere.security.auth.TokenExpiredException;
import com.ibm.ws.crypto.ltpakeyutil.LTPAPrivateKey;
import com.ibm.ws.crypto.ltpakeyutil.LTPAPublicKey;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.token.ltpa.LTPAValidationKeysInfo;
import com.ibm.wsspi.security.ltpa.Token;
import com.ibm.wsspi.security.ltpa.TokenFactory;

public class LTPAToken2Factory implements TokenFactory {
    private static final TraceComponent tc = Tr.register(LTPAToken2Factory.class);
    private long expirationInMinutes;
    private byte[] primarySharedKey;
    private LTPAPublicKey primaryPublicKey;
    private LTPAPrivateKey primaryPrivateKey;
    private List<LTPAValidationKeysInfo> validationKeys;
    private long expDiffAllowed;

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public void initialize(@Sensitive Map tokenFactoryMap) {
        expirationInMinutes = (Long) tokenFactoryMap.get(LTPAConstants.EXPIRATION);
        primarySharedKey = (byte[]) tokenFactoryMap.get(LTPAConstants.PRIMARY_SECRET_KEY);
        primaryPublicKey = (LTPAPublicKey) tokenFactoryMap.get(LTPAConstants.PRIMARY_PUBLIC_KEY);
        primaryPrivateKey = (LTPAPrivateKey) tokenFactoryMap.get(LTPAConstants.PRIMARY_PRIVATE_KEY);
        expDiffAllowed = (Long) tokenFactoryMap.get(LTPAConfigurationImpl.KEY_EXP_DIFF_ALLOWED);
        validationKeys = (List<LTPAValidationKeysInfo>) tokenFactoryMap.get(LTPAConstants.VALIDATION_KEYS);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Number of validationKeys: " + validationKeys.size());
        }
    }

    /** {@inheritDoc} */
    @Override
    public Token createToken(Map tokenData) throws TokenCreationFailedException {
        String userUniqueId = getUniqueId(tokenData);
        return new LTPAToken2(userUniqueId, expirationInMinutes, primarySharedKey, primaryPrivateKey, primaryPublicKey);
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
                validatedToken = new LTPAToken2(tokenBytes, primarySharedKey, primaryPrivateKey, primaryPublicKey, expDiffAllowed, removeAttributes);
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
        if (validationKeys != null) {
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
                    validationKeysIterator.remove();
                } else {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "validateTokenBytes with validationKeys: " + ltpaKeyInfo);
                    }
                    if (sharedKeyForValidation != null && ltpaPrivateKeyForValidation != null && ltpaPublicKeyForValidation != null) {
                        try {
                            validatedToken = new LTPAToken2(tokenBytes, sharedKeyForValidation, ltpaPrivateKeyForValidation, ltpaPublicKeyForValidation, expDiffAllowed, removeAttributes);
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
