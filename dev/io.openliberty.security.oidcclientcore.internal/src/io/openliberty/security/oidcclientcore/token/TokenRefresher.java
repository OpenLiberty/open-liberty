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
package io.openliberty.security.oidcclientcore.token;

//import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.webcontainer.security.AuthResult;
import com.ibm.ws.webcontainer.security.ProviderAuthenticationResult;

//import io.openliberty.security.jakartasec.identitystore.OpenIdContextImpl;
import io.openliberty.security.oidcclientcore.client.OidcClientConfig;
import io.openliberty.security.oidcclientcore.exceptions.TokenRequestException;
//import jakarta.security.enterprise.identitystore.openid.RefreshToken;

public class TokenRefresher {

    private static final TraceComponent tc = Tr.register(TokenRefresher.class);

    private HttpServletRequest request = null;
    private OidcClientConfig oidcClientConfig = null;

    //private OpenIdContextImpl openIdContextImpl = null;

    private Boolean accessTokenExpired = null;
    private Boolean idTokenExpired = null;

    public TokenRefresher(HttpServletRequest req, OidcClientConfig clientConfig) {
        //this(req, clientConfig, null);
        request = req;
        oidcClientConfig = clientConfig;
    }

    //public TokenRefresher(HttpServletRequest req, OidcClientConfig clientConfig, OpenIdContextImpl oidcContextImpl) {
    //    request = req;
    //    oidcClientConfig = clientConfig;
    //    openIdContextImpl = oidcContextImpl;
    //}

    public boolean isTokenExpired() {
        return isAccessTokenExpired() || isIdTokenExpired();
    }

    public boolean isAccessTokenExpired() {
        if (accessTokenExpired == null)
            accessTokenExpired = false; //openIdContextImpl.getAccessToken().isExpired();
        return accessTokenExpired;
    }

    public boolean isIdTokenExpired() {
        if (idTokenExpired == null)
            idTokenExpired = false; //openIdContextImpl.getIdentityToken().isExpired();
        return idTokenExpired;
    }

    public ProviderAuthenticationResult refreshToken() {
        JakartaOidcTokenRequest tokenRequest = new JakartaOidcTokenRequest(oidcClientConfig, request);
        ProviderAuthenticationResult authResult;
        try {
            authResult = tokenRequest.sendTokenRefreshRequest(getRefreshToken());
        } catch (TokenRequestException e) {
            authResult = new ProviderAuthenticationResult(AuthResult.FAILURE, HttpServletResponse.SC_UNAUTHORIZED);
        }
        return authResult;
    }

    public String getRefreshToken() {
//        Optional<RefreshToken> optionalRefreshToken = openIdContextImpl.getRefreshToken();
//        if (optionalRefreshToken.isPresent()) {
//            RefreshToken refreshToken = optionalRefreshToken.get();
//            if (refreshToken != null) {
//                return refreshToken.getToken();
//            }
//        }
        return null;
    }

}
