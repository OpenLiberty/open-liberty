/*******************************************************************************
 * Copyright (c) 2004, 2011, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.token.ltpa.internal;

import java.util.Map;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.security.auth.InvalidTokenException;
import com.ibm.websphere.security.auth.TokenCreationFailedException;
import com.ibm.websphere.security.auth.TokenExpiredException;
import com.ibm.ws.crypto.ltpakeyutil.LTPAPrivateKey;
import com.ibm.ws.crypto.ltpakeyutil.LTPAPublicKey;
import com.ibm.wsspi.security.ltpa.Token;
import com.ibm.wsspi.security.ltpa.TokenFactory;

public class LTPAToken2Factory implements TokenFactory {
    private static final TraceComponent tc = Tr.register(LTPAToken2Factory.class);
    private long expirationInMinutes;
    private byte[] sharedKey;
    private LTPAPublicKey publicKey;
    private LTPAPrivateKey privateKey;

    /** {@inheritDoc} */
    @Override
    public void initialize(@Sensitive Map tokenFactoryMap) {
        expirationInMinutes = (Long) tokenFactoryMap.get(LTPAConstants.EXPIRATION);
        sharedKey = (byte[]) tokenFactoryMap.get(LTPAConstants.SECRET_KEY);
        publicKey = (LTPAPublicKey) tokenFactoryMap.get(LTPAConstants.PUBLIC_KEY);
        privateKey = (LTPAPrivateKey) tokenFactoryMap.get(LTPAConstants.PRIVATE_KEY);
    }

    /** {@inheritDoc} */
    @Override
    public Token createToken(Map tokenData) throws TokenCreationFailedException {
        String userUniqueId = getUniqueId(tokenData);
        return new LTPAToken2(userUniqueId, expirationInMinutes, sharedKey, privateKey, publicKey);
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
        return new LTPAToken2(tokenBytes, sharedKey, privateKey, publicKey);
    }

    /** {@inheritDoc} */
    @Override
    public Token validateTokenBytes(byte[] tokenBytes, String... removeAttributes) throws InvalidTokenException, TokenExpiredException {
        return new LTPAToken2(tokenBytes, sharedKey, privateKey, publicKey, removeAttributes);
    }

}
