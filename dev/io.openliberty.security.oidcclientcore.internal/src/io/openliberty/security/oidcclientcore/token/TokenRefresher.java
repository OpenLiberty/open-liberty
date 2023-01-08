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
package io.openliberty.security.oidcclientcore.token;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.ws.webcontainer.security.AuthResult;
import com.ibm.ws.webcontainer.security.ProviderAuthenticationResult;

import io.openliberty.security.oidcclientcore.client.OidcClientConfig;
import io.openliberty.security.oidcclientcore.exceptions.TokenRequestException;

public class TokenRefresher {

    private HttpServletRequest request = null;
    private OidcClientConfig oidcClientConfig = null;

    private Boolean accessTokenExpired = null;
    private Boolean idTokenExpired = null;
    private String refreshTokenString = null;

    public TokenRefresher(HttpServletRequest request, OidcClientConfig oidcClientConfig,
                          boolean accessTokenExpired,
                          boolean idTokenExpired,
                          String refreshTokenString) {
        this.request = request;
        this.oidcClientConfig = oidcClientConfig;
        this.accessTokenExpired = accessTokenExpired;
        this.idTokenExpired = idTokenExpired;
        this.refreshTokenString = refreshTokenString;
    }

    public boolean isTokenExpired() {
        return isAccessTokenExpired() || isIdTokenExpired();
    }

    public boolean isAccessTokenExpired() {
        return accessTokenExpired;
    }

    public boolean isIdTokenExpired() {
        return idTokenExpired;
    }

    public ProviderAuthenticationResult refreshToken() {
        JakartaOidcTokenRequest tokenRequest = new JakartaOidcTokenRequest(oidcClientConfig, request);
        ProviderAuthenticationResult authResult;
        try {
            authResult = tokenRequest.sendTokenRefreshRequest(refreshTokenString);
        } catch (TokenRequestException e) {
            authResult = new ProviderAuthenticationResult(AuthResult.FAILURE, HttpServletResponse.SC_UNAUTHORIZED);
        }
        return authResult;
    }

}
