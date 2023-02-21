/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.security.oidcclientcore.client;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jose4j.jwt.JwtClaims;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.common.jwk.impl.JWKSet;
import com.ibm.ws.webcontainer.security.AuthResult;
import com.ibm.ws.webcontainer.security.ProviderAuthenticationResult;

import io.openliberty.security.oidcclientcore.authentication.AbstractFlow;
import io.openliberty.security.oidcclientcore.authentication.Flow;
import io.openliberty.security.oidcclientcore.exceptions.AuthenticationResponseException;
import io.openliberty.security.oidcclientcore.exceptions.TokenRequestException;
import io.openliberty.security.oidcclientcore.exceptions.UnsupportedResponseTypeException;
import io.openliberty.security.oidcclientcore.logout.LogoutHandler;
import io.openliberty.security.oidcclientcore.token.TokenRefresher;
import io.openliberty.security.oidcclientcore.token.TokenResponse;
import io.openliberty.security.oidcclientcore.token.TokenResponseValidator;
import io.openliberty.security.oidcclientcore.token.TokenValidationException;

public class Client {

    public static final TraceComponent tc = Tr.register(Client.class);

    private final OidcClientConfig oidcClientConfig;
    private final LogoutConfig logoutConfig;

    private static JWKSet jwkSet = null;

    public Client(OidcClientConfig oidcClientConfig) {
        this.oidcClientConfig = oidcClientConfig;
        logoutConfig = oidcClientConfig.getLogoutConfig();
    }

    public OidcClientConfig getOidcClientConfig() {
        return oidcClientConfig;
    }

    public ProviderAuthenticationResult startFlow(HttpServletRequest request, HttpServletResponse response) throws UnsupportedResponseTypeException {
        Flow flow = AbstractFlow.getInstance(oidcClientConfig);
        return flow.startFlow(request, response);
    }

    public ProviderAuthenticationResult continueFlow(HttpServletRequest request,
                                                     HttpServletResponse response) throws UnsupportedResponseTypeException, AuthenticationResponseException, TokenRequestException {
        Flow flow = AbstractFlow.getInstance(oidcClientConfig);
        return flow.continueFlow(request, response);
    }

    public JwtClaims validate(TokenResponse tokenResponse, HttpServletRequest request, HttpServletResponse response) throws TokenValidationException {
        TokenResponseValidator tokenResponseValidator = new TokenResponseValidator(this.oidcClientConfig, request, response);
        return tokenResponseValidator.validate(tokenResponse);
    }

    public static JWKSet getJwkSet() {
        if (jwkSet == null) {
            jwkSet = new JWKSet();
        }
        return jwkSet;
    }

    public ProviderAuthenticationResult processExpiredToken(HttpServletRequest request, HttpServletResponse response,
                                                            boolean isAccessTokenExpired,
                                                            boolean isIdTokenExpired,
                                                            String idTokenString,
                                                            String refreshTokenString) {
        TokenRefresher tokenRefresher = new TokenRefresher(request, oidcClientConfig, isAccessTokenExpired, isIdTokenExpired, refreshTokenString);

        if (tokenRefresher.isTokenExpired()) {
            if (oidcClientConfig.isTokenAutoRefresh()) {
                // when there is no previously stored refresh_token field of the Token Response, a logout should be initiated
                if (refreshTokenString == null) {
                    return logout(request, response, idTokenString);
                }
                ProviderAuthenticationResult providerAuthResult = tokenRefresher.refreshToken();
                if (AuthResult.SUCCESS.equals(providerAuthResult.getStatus())) {
                    return providerAuthResult;
                }
                // When the call is not successful, ... a logout should be initiated.
                return logout(request, response, idTokenString);
            } else {

                if ((logoutConfig.isAccessTokenExpiry() && tokenRefresher.isAccessTokenExpired()) ||
                    (logoutConfig.isIdentityTokenExpiry() && tokenRefresher.isIdTokenExpired())) {
                    return logout(request, response, idTokenString);
                }
            }
        }

        // The token expiration is ignored when none of the above conditions hold
        // TODO: Return AuthResult.CONTINUE instead so that caller does not need to attempt to update context unnecessarily.
        return new ProviderAuthenticationResult(AuthResult.SUCCESS, HttpServletResponse.SC_OK);
    }

    public ProviderAuthenticationResult logout(HttpServletRequest request, HttpServletResponse response, String idTokenString) {
        LogoutHandler logoutHandler = new LogoutHandler(request, response, oidcClientConfig, logoutConfig, idTokenString);
        try {
            return logoutHandler.logout();
        } catch (ServletException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Logout failed with a ServletException exception on " + idTokenString, e);
            }
            return new ProviderAuthenticationResult(AuthResult.FAILURE, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

}
