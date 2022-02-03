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
package com.ibm.ws.security.token.internal;

import java.util.Iterator;
import java.util.Map;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.auth.InvalidTokenException;
import com.ibm.websphere.security.auth.TokenCreationFailedException;
import com.ibm.websphere.security.auth.TokenExpiredException;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.token.TokenManager;
import com.ibm.ws.security.token.TokenService;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap;
import com.ibm.wsspi.security.ltpa.Token;
import com.ibm.wsspi.security.token.SingleSignonToken;

/**
 * The TokenManager class creates tokens as specified by the token type and recreates a token from the token bytes.
 */
public class TokenManagerImpl implements TokenManager {
    private static final TraceComponent tc = Tr.register(TokenManagerImpl.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    static final String CFG_KEY_SSO_TOKEN_TYPE = "ssoTokenType";
    static final String KEY_TOKEN_SERVICE = "tokenService";
    static final String KEY_TOKEN_TYPE = "tokenType";
    private final ConcurrentServiceReferenceMap<String, TokenService> services = new ConcurrentServiceReferenceMap<String, TokenService>(KEY_TOKEN_SERVICE);
    private volatile String ssoTokenType;

    protected void setTokenService(ServiceReference<TokenService> tokenServiceReference) {
        String tokenType = (String) tokenServiceReference.getProperty(KEY_TOKEN_TYPE);
        services.putReference(tokenType, tokenServiceReference);
    }

    protected void unsetTokenService(ServiceReference<TokenService> tokenServiceReference) {
        String tokenType = (String) tokenServiceReference.getProperty(KEY_TOKEN_TYPE);
        services.removeReference(tokenType, tokenServiceReference);
    }

    protected void activate(ComponentContext componentContext, Map<String, Object> props) {
        services.activate(componentContext);
        ssoTokenType = (String) props.get(CFG_KEY_SSO_TOKEN_TYPE);
    }

    protected void modified(Map<String, Object> props) {
        ssoTokenType = (String) props.get(CFG_KEY_SSO_TOKEN_TYPE);
    }

    protected void deactivate(ComponentContext componentContext) {
        services.deactivate(componentContext);
    }

    /** {@inheritDoc} */
    @Override
    public Token createToken(String tokenType, Map<String, Object> tokenData) throws TokenCreationFailedException {
        try {
            TokenService tokenService = getTokenServiceForType(tokenType);
            return tokenService.createToken(tokenData);
        } catch (IllegalArgumentException e) {
            throw new TokenCreationFailedException(e.getMessage(), e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public SingleSignonToken createSSOToken(Map<String, Object> tokenData) throws TokenCreationFailedException {
        try {
            TokenService tokenService = getTokenServiceForType(ssoTokenType);
            SingleSignonTokenImpl ssoToken = new SingleSignonTokenImpl(tokenService);
            Token ssoLtpaToken = tokenService.createToken(tokenData);
            ssoToken.initializeToken(ssoLtpaToken);
            return ssoToken;
        } catch (IllegalArgumentException e) {
            throw new TokenCreationFailedException(e.getMessage(), e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public SingleSignonToken createSSOToken(Token token) throws TokenCreationFailedException {
        try {
            TokenService tokenService = getTokenServiceForType(ssoTokenType);
            SingleSignonTokenImpl ssoToken = new SingleSignonTokenImpl(tokenService);
            ssoToken.initializeToken(token);
            return ssoToken;
        } catch (IllegalArgumentException e) {
            throw new TokenCreationFailedException(e.getMessage(), e);
        }
    }

    /** {@inheritDoc} */
    @Override
    @FFDCIgnore(InvalidTokenException.class)
    public Token recreateTokenFromBytes(byte[] tokenBytes, String... removeAttributes) throws InvalidTokenException, TokenExpiredException {
        Token token = null;

        Iterator<TokenService> availableServices = services.getServices();
        while (availableServices.hasNext()) {
            TokenService tokenService = availableServices.next();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(
                         tc,
                         "Trying to recreate token using token service "
                             + tokenService
                             + ". This will fail if the token was not created by this service and may fail if the configuration of the service which created the token has changed.");
            }
            try {
                if (removeAttributes == null) {
                    token = tokenService.recreateTokenFromBytes(tokenBytes);
                } else {
                    token = tokenService.recreateTokenFromBytes(tokenBytes, removeAttributes);
                }
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Successfully recreated token using token service " + tokenService + ".");
                }
                break;
            } catch (InvalidTokenException e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "The token service " + tokenService + " failed to recreate the token.", e);
                }
            }
        }

        if (token == null) {
            Tr.info(tc, "TOKEN_SERVICE_INVALID_TOKEN_INFO");
            String translatedMessage = TraceNLS.getStringFromBundle(this.getClass(),
                                                                    TraceConstants.MESSAGE_BUNDLE,
                                                                    "TOKEN_SERVICE_INVALID_TOKEN_INFO",
                                                                    "CWWKS4001I: The security token cannot be validated.");
            throw new InvalidTokenException(translatedMessage);
        }

        return token;
    }

    /** {@inheritDoc} */
    @Override
    public Token recreateTokenFromBytes(String tokenType, byte[] tokenBytes) throws InvalidTokenException, TokenExpiredException {
        try {
            TokenService tokenService = getTokenServiceForType(tokenType);
            return tokenService.recreateTokenFromBytes(tokenBytes);
        } catch (IllegalArgumentException e) {
            Tr.info(tc, "TOKEN_SERVICE_INVALID_TOKEN_INFO");
            String translatedMessage = TraceNLS.getStringFromBundle(this.getClass(),
                                                                    TraceConstants.MESSAGE_BUNDLE,
                                                                    "TOKEN_SERVICE_INVALID_TOKEN_INFO",
                                                                    "CWWKS4001I: The security token cannot be validated.");
            throw new InvalidTokenException(translatedMessage, e);
        }
    }

    /**
     * Get the TokenService object which provides for the specified tokenType.
     *
     * @param tokenType
     * @return TokenService which handles the specified tokenType. Will not return {@code null}.
     * @throws IllegalArgumentException if there is no available service for the specified tokenType
     */
    private TokenService getTokenServiceForType(String tokenType) {
        TokenService service = services.getService(tokenType);
        if (service != null) {
            return service;
        } else {
            Tr.error(tc, "TOKEN_SERVICE_CONFIG_ERROR_NO_SUCH_SERVICE_TYPE", tokenType);
            String formattedMessage = TraceNLS.getFormattedMessage(
                                                                   this.getClass(),
                                                                   TraceConstants.MESSAGE_BUNDLE,
                                                                   "TOKEN_SERVICE_CONFIG_ERROR_NO_SUCH_SERVICE_TYPE",
                                                                   new Object[] { tokenType },
                                                                   "CWWKS4000E: A configuration error has occurred. The requested TokenService instance of type {0} could not be found.");
            throw new IllegalArgumentException(formattedMessage);
        }
    }
}
