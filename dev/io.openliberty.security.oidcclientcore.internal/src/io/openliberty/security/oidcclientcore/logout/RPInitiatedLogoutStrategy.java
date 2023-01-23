/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.security.oidcclientcore.logout;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import javax.servlet.http.HttpServletResponse;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.webcontainer.security.AuthResult;
import com.ibm.ws.webcontainer.security.ProviderAuthenticationResult;

import io.openliberty.security.oidcclientcore.JakartaOIDCConstants;
import io.openliberty.security.oidcclientcore.client.LogoutConfig;
import io.openliberty.security.oidcclientcore.client.OidcClientConfig;
import io.openliberty.security.oidcclientcore.http.OidcClientHttpUtil;

public class RPInitiatedLogoutStrategy {

    private final OidcClientConfig oidcClientConfig;
    private LogoutConfig logoutConfig;
    private final String endSessionEndPoint;
    private final String idTokenString;

    OidcClientHttpUtil oidcClientHttpUtil = OidcClientHttpUtil.getInstance();

    public RPInitiatedLogoutStrategy(OidcClientConfig oidcClientConfig, String endSessionEndPoint, String idTokenString) {
        this.oidcClientConfig = oidcClientConfig;
        if (oidcClientConfig != null) {
            logoutConfig = oidcClientConfig.getLogoutConfig();
        }
        this.endSessionEndPoint = endSessionEndPoint;
        this.idTokenString = idTokenString;
    }

    public ProviderAuthenticationResult logout() {
        String endSessionUrl = buildEndSessionUrl();
        return new ProviderAuthenticationResult(AuthResult.REDIRECT_TO_PROVIDER, HttpServletResponse.SC_OK, null, null, null, endSessionUrl);

    }

    String buildEndSessionUrl() {
        String queryString = "";
        if (oidcClientConfig != null) {
            queryString = appendParameter(queryString, "client_id", oidcClientConfig.getClientId());
        }
        if (logoutConfig != null) {
            queryString = appendParameter(queryString, JakartaOIDCConstants.POST_LOGOUT_REDIRECT_URI, logoutConfig.getRedirectURI());
        }
        // Will look at including the ID token hint later
//        queryString = appendParameter(queryString, JakartaOIDCConstants.ID_TOKEN_HINT, idTokenString);

        String endSessionUrlWithQueryParams = endSessionEndPoint;
        if (!queryString.isEmpty()) {
            if (!endSessionEndPoint.contains("?")) {
                endSessionUrlWithQueryParams += "?";
            } else {
                endSessionUrlWithQueryParams += "&";
            }
        }
        endSessionUrlWithQueryParams += queryString;
        return endSessionUrlWithQueryParams;
    }

    @FFDCIgnore(UnsupportedEncodingException.class)
    String appendParameter(String queryString, String parameterName, String parameterValue) {
        if (parameterValue != null && !parameterValue.isEmpty()) {
            if (queryString == null) {
                queryString = "";
            }
            if (!queryString.isEmpty()) {
                queryString += "&";
            }
            try {
                queryString += parameterName + "=" + URLEncoder.encode(parameterValue, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                // Do nothing - UTF-8 should be supported.
            }
        }
        return queryString;
    }

}
