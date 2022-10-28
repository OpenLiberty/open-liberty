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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.ws.webcontainer.security.AuthResult;
import com.ibm.ws.webcontainer.security.ProviderAuthenticationResult;

import io.openliberty.security.oidcclientcore.authentication.JakartaOidcAuthorizationRequest;
import io.openliberty.security.oidcclientcore.client.LogoutConfig;
import io.openliberty.security.oidcclientcore.client.OidcClientConfig;
import io.openliberty.security.oidcclientcore.config.MetadataUtils;
import io.openliberty.security.oidcclientcore.exceptions.OidcClientConfigurationException;
import io.openliberty.security.oidcclientcore.exceptions.OidcDiscoveryException;
import io.openliberty.security.oidcclientcore.http.EndpointRequest;

public class LogoutHandler extends EndpointRequest {
    HttpServletRequest req;
    HttpServletResponse resp;
    OidcClientConfig oidcClientConfig;
    LogoutConfig logoutConfig;

    private String idTokenString = null;

    ProviderAuthenticationResult authResult = new ProviderAuthenticationResult(AuthResult.SUCCESS, 200);

    public LogoutHandler(HttpServletRequest req, HttpServletResponse resp, OidcClientConfig oidcClientConfig, LogoutConfig logoutConfig, String idTokenString) {
        this.req = req;
        this.resp = resp;
        this.oidcClientConfig = oidcClientConfig;
        this.logoutConfig = logoutConfig;
        this.idTokenString = idTokenString;
    }

    public ProviderAuthenticationResult logout() throws ServletException, OidcDiscoveryException, OidcClientConfigurationException {

        LocalLogoutStrategy localLogout = new LocalLogoutStrategy(req);
        localLogout.logout();

        String endSessionEndPoint = MetadataUtils.getEndSessionEndpoint(oidcClientConfig);
        String redirectUrl = logoutConfig.getRedirectURI();

        if (logoutConfig.isNotifyProvider() && endSessionEndPoint != null) {
            RPInitiatedLogoutStrategy rpInitiatedLogoutStrategy = new RPInitiatedLogoutStrategy(oidcClientConfig, endSessionEndPoint, idTokenString);
            return rpInitiatedLogoutStrategy.logout();
        } else if (!logoutConfig.isNotifyProvider() && redirectUrl != null) {
            CustomLogoutStrategy customLogoutStrategy = new CustomLogoutStrategy(redirectUrl);
            return customLogoutStrategy.logout();
        } else {
            JakartaOidcAuthorizationRequest oidcAuthorizationRequest = new JakartaOidcAuthorizationRequest(req, resp, oidcClientConfig);
            return oidcAuthorizationRequest.sendRequest();
        }
    }

}
