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
package io.openliberty.security.oidcclientcore.logout;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.ws.webcontainer.security.AuthResult;
import com.ibm.ws.webcontainer.security.ProviderAuthenticationResult;

import io.openliberty.security.oidcclientcore.client.LogoutConfig;
import io.openliberty.security.oidcclientcore.client.OidcClientConfig;
import io.openliberty.security.oidcclientcore.http.OidcClientHttpUtil;

public class RPInitiatedLogoutStrategy {

    private final HttpServletRequest req;
    private final OidcClientConfig oidcClientConfig;
    private LogoutConfig logoutConfig;
    private final String endSessionEndPoint;
    private final String idTokenString;

    //TODO: Move these constants to the common place or user OIDCConstants.java
    public static final String ID_TOKEN_HINT = "id_token_hint";
    public static final String CLIENT_ID = "post_logout_redirect_uri";
    public static final String POST_LOGOUT_REDIRECT_URI = "post_logout_redirect_uri";

    OidcClientHttpUtil oidcClientHttpUtil = OidcClientHttpUtil.getInstance();

    public RPInitiatedLogoutStrategy(HttpServletRequest req, OidcClientConfig oidcClientConfig, String endSessionEndPoint, String idTokenString) {
        this.req = req;
        this.oidcClientConfig = oidcClientConfig;
        this.endSessionEndPoint = endSessionEndPoint;
        this.idTokenString = idTokenString;
    }

    public ProviderAuthenticationResult logout() {
        String clientId = null;
        String redirectURI = null;
        if (oidcClientConfig != null) {
            clientId = oidcClientConfig.getClientId();
        }
        if (logoutConfig != null) {
            redirectURI = logoutConfig.getRedirectURI();
        }

        if (idTokenString != null) {
            req.setAttribute(ID_TOKEN_HINT, idTokenString);
        }
        req.setAttribute(CLIENT_ID, clientId);
        if (redirectURI != null && !redirectURI.isEmpty()) {
            req.setAttribute(POST_LOGOUT_REDIRECT_URI, redirectURI);
        }

        return new ProviderAuthenticationResult(AuthResult.REDIRECT_TO_PROVIDER, HttpServletResponse.SC_OK, null, null, null, endSessionEndPoint);

    }

}
