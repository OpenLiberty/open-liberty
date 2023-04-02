/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
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
package io.openliberty.security.oidcclientcore.logout;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.webcontainer.security.AuthResult;
import com.ibm.ws.webcontainer.security.ProviderAuthenticationResult;

import io.openliberty.security.oidcclientcore.authentication.JakartaOidcAuthorizationRequest;
import io.openliberty.security.oidcclientcore.client.LogoutConfig;
import io.openliberty.security.oidcclientcore.client.OidcClientConfig;
import io.openliberty.security.oidcclientcore.config.MetadataUtils;
import io.openliberty.security.oidcclientcore.exceptions.OidcClientConfigurationException;
import io.openliberty.security.oidcclientcore.exceptions.OidcDiscoveryException;

public class LogoutHandler {

    public static final TraceComponent tc = Tr.register(LogoutHandler.class);

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

    public ProviderAuthenticationResult logout() throws ServletException {

        LocalLogoutStrategy localLogout = new LocalLogoutStrategy(req);
        localLogout.logout();

        return logoutWithoutLocalLogout();
    }

    @FFDCIgnore(OidcClientConfigurationException.class)
    public ProviderAuthenticationResult logoutWithoutLocalLogout() throws ServletException {
        String endSessionEndPoint = null;

        try {
            endSessionEndPoint = MetadataUtils.getEndSessionEndpoint(oidcClientConfig);
        } catch (OidcDiscoveryException | OidcClientConfigurationException e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "The {0} OpenID Connect client failed to find a configured end session endpoint. {1}", oidcClientConfig.getClientId(), e.toString());
            }
        }

        String redirectUrl = logoutConfig.getRedirectURI();

        if (logoutConfig.isNotifyProvider() && endSessionEndPoint != null) {
            RPInitiatedLogoutStrategy rpInitiatedLogoutStrategy = new RPInitiatedLogoutStrategy(oidcClientConfig, endSessionEndPoint, idTokenString);
            return rpInitiatedLogoutStrategy.logout();
        } else if (!logoutConfig.isNotifyProvider() && redirectUrl != null && !redirectUrl.isEmpty()) {
            CustomLogoutStrategy customLogoutStrategy = new CustomLogoutStrategy(redirectUrl);
            return customLogoutStrategy.logout();
        } else {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Redirect to the OpenID Connect Provider Authentication endpoint for re-authentication");
            }
            JakartaOidcAuthorizationRequest oidcAuthorizationRequest = new JakartaOidcAuthorizationRequest(req, resp, oidcClientConfig);
            return oidcAuthorizationRequest.sendRequest();
        }
    }

}
