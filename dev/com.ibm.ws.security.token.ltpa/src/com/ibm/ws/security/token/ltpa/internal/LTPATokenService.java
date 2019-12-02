/*******************************************************************************
 * Copyright (c) 2011, 2019 IBM Corporation and others.
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

import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.security.auth.InvalidTokenException;
import com.ibm.websphere.security.auth.TokenCreationFailedException;
import com.ibm.websphere.security.auth.TokenExpiredException;
import com.ibm.ws.security.token.TokenService;
import com.ibm.wsspi.security.ltpa.Token;
import com.ibm.wsspi.security.ltpa.TokenFactory;

/**
 *
 */
public class LTPATokenService implements TokenService {
    private volatile LTPAConfiguration ltpaConfig;

    protected void setLtpaConfig(LTPAConfiguration ltpaConfig) {
        this.ltpaConfig = ltpaConfig;
    }

    protected void unsetLtpaConfig(LTPAConfiguration ltpaConfig) {
        if (this.ltpaConfig == ltpaConfig) {
            ltpaConfig = null;
        }
    }

    protected void activate(ComponentContext context) {}

    protected void deactivate(ComponentContext context) {}

    /**
     * {@inheritDoc}
     *
     * @throws TokenCreationFailedException
     */
    @Override
    public Token createToken(Map<String, Object> tokenData) throws TokenCreationFailedException {
        TokenFactory tokenFactory = ltpaConfig.getTokenFactory();
        return tokenFactory.createToken(tokenData);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Token recreateTokenFromBytes(byte[] tokenBytes) throws InvalidTokenException, TokenExpiredException {
        TokenFactory tokenFactory = ltpaConfig.getTokenFactory();
        Token token = tokenFactory.validateTokenBytes(tokenBytes);
        validateRecreatedToken(token);
        return token;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Token recreateTokenFromBytes(byte[] tokenBytes, String... removeAttributes) throws InvalidTokenException, TokenExpiredException {
        TokenFactory tokenFactory = ltpaConfig.getTokenFactory();
        Token token = tokenFactory.validateTokenBytes(tokenBytes, removeAttributes);
        return token;
    }

    private void validateRecreatedToken(Token token) throws InvalidTokenException, TokenExpiredException {
        if (token != null && token.isValid()) {
            return;
        }
    }
}
