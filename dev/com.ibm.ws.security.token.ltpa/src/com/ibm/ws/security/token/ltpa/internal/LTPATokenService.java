/*******************************************************************************
 * Copyright (c) 2011, 2026 IBM Corporation and others.
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

import java.util.Map;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.security.auth.InvalidTokenException;
import com.ibm.websphere.security.auth.TokenCreationFailedException;
import com.ibm.websphere.security.auth.TokenExpiredException;
import com.ibm.ws.security.token.TokenService;
import com.ibm.ws.security.token.ltpa.LTPAConfiguration;
import com.ibm.wsspi.security.ltpa.Token;
import com.ibm.wsspi.security.ltpa.TokenFactory;

/**
 *
 */
@Component(name = "com.ibm.ws.security.token.ltpa.LTPATokenService", service = TokenService.class, configurationPolicy = ConfigurationPolicy.IGNORE,
           property = { "service.vendor=IBM", "tokenType=Ltpa2" })
public class LTPATokenService implements TokenService {

    private final LTPAConfiguration ltpaConfig;

    @Activate
    public LTPATokenService(@Reference LTPAConfiguration ltpaConfig) {
        this.ltpaConfig = ltpaConfig;
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
    }

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
