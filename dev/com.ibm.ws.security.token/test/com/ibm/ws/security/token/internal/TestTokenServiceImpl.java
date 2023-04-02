/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package com.ibm.ws.security.token.internal;

import java.util.Map;

import com.ibm.websphere.security.auth.InvalidTokenException;
import com.ibm.websphere.security.auth.TokenCreationFailedException;
import com.ibm.websphere.security.auth.TokenExpiredException;
import com.ibm.ws.security.token.TokenService;
import com.ibm.wsspi.security.ltpa.Token;

/**
 * The TokenService is used to create a token. Generally this
 * will be an LTPAToken2. For simplicity, I am testing
 * the LTPAToken2 serialization separately, so here we
 * simply return the 'fake' token.
 */
public class TestTokenServiceImpl implements TokenService {

    /**
     * Not called.
     */
    @Override
    public Token createToken(Map<String, Object> tokenData) throws TokenCreationFailedException {
        return null;
    }

    /**
     * Return the fake token.
     */
    @Override
    public Token recreateTokenFromBytes(byte[] tokenBytes) throws InvalidTokenException, TokenExpiredException {
        return new TestToken();
    }

    /**
     * Not called.
     */
    @Override
    public Token recreateTokenFromBytes(byte[] tokenBytes, String... removeAttributes) throws InvalidTokenException, TokenExpiredException {
        return null;
    }

}
