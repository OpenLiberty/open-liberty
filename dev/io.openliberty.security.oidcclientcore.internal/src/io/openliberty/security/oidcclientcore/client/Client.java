/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.oidcclientcore.client;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.ws.webcontainer.security.AuthResult;
import com.ibm.ws.webcontainer.security.ProviderAuthenticationResult;

import io.openliberty.security.oidcclientcore.authentication.AbstractFlow;
import io.openliberty.security.oidcclientcore.authentication.Flow;
import io.openliberty.security.oidcclientcore.exceptions.AuthenticationResponseException;
import io.openliberty.security.oidcclientcore.exceptions.TokenRequestException;
import io.openliberty.security.oidcclientcore.token.TokenRefresher;
import io.openliberty.security.oidcclientcore.token.TokenResponse;
import io.openliberty.security.oidcclientcore.token.TokenResponseValidator;
import io.openliberty.security.oidcclientcore.token.TokenValidationException;

public class Client {

    private final OidcClientConfig oidcClientConfig;

    public Client(OidcClientConfig oidcClientConfig) {
        this.oidcClientConfig = oidcClientConfig;
    }

    public OidcClientConfig getOidcClientConfig() {
        return oidcClientConfig;
    }

    public ProviderAuthenticationResult startFlow(HttpServletRequest request, HttpServletResponse response) {
        Flow flow = AbstractFlow.getInstance(oidcClientConfig);
        return flow.startFlow(request, response);
    }

    public ProviderAuthenticationResult continueFlow(HttpServletRequest request, HttpServletResponse response) throws AuthenticationResponseException, TokenRequestException {
        Flow flow = AbstractFlow.getInstance(oidcClientConfig);
        return flow.continueFlow(request, response);
    }

    public void validate(TokenResponse tokenResponse) throws TokenValidationException {
        TokenResponseValidator tokenResponseValidator = new TokenResponseValidator(this.oidcClientConfig);
        tokenResponseValidator.validate(tokenResponse);
    }

    public ProviderAuthenticationResult processExpiredToken(HttpServletRequest request, HttpServletResponse response) {
        //TODO: pass in OpenIdContextImpl to TokenRefresher
        TokenRefresher tokenRefresher = new TokenRefresher(request, oidcClientConfig);
        LogoutConfig logoutConfig = oidcClientConfig.getLogoutConfig();

        if (tokenRefresher.isTokenExpired()) {
            if (oidcClientConfig.isTokenAutoRefresh()) {
                // when there is no previously stored refresh_token field of the Token Response, a logout should be initiated
                if (tokenRefresher.getRefreshToken() == null) {
                    return logout(request, response, logoutConfig);
                }
                ProviderAuthenticationResult providerAuthResult = tokenRefresher.refreshToken();
                if (AuthResult.SUCCESS.equals(providerAuthResult.getStatus())) {
                    return providerAuthResult;
                }
                // When the call is not successful, ... a logout should be initiated.
                return logout(request, response, logoutConfig);
            } else {
                if ((logoutConfig.isAccessTokenExpiry() && tokenRefresher.isAccessTokenExpired()) ||
                    (logoutConfig.isIdentityTokenExpiry() && tokenRefresher.isIdTokenExpired())) {
                    return logout(request, response, logoutConfig);
                }
            }
        }
        // The token expiration is ignored when none of the above conditions hold
        // TODO: is this the correct auth result return here?
        return new ProviderAuthenticationResult(AuthResult.SUCCESS, HttpServletResponse.SC_OK);
    }

    public ProviderAuthenticationResult logout(HttpServletRequest request, HttpServletResponse response,
                                               LogoutConfig logoutConfig) {
        // TODO
        return null;
    }

}
