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

import com.ibm.json.java.JSONObject;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.webcontainer.security.AuthResult;
import com.ibm.ws.webcontainer.security.ProviderAuthenticationResult;

import io.openliberty.security.oidcclientcore.authentication.JakartaOidcAuthorizationRequest;
import io.openliberty.security.oidcclientcore.client.LogoutConfig;
import io.openliberty.security.oidcclientcore.client.OidcClientConfig;
import io.openliberty.security.oidcclientcore.client.OidcProviderMetadata;
import io.openliberty.security.oidcclientcore.exceptions.OidcDiscoveryException;
import io.openliberty.security.oidcclientcore.http.EndpointRequest;

public class LogoutHandler extends EndpointRequest {
    HttpServletRequest req;
    HttpServletResponse resp;
    OidcClientConfig oidcClientConfig;
    LogoutConfig logoutConfig;

    private OidcProviderMetadata providerMetadata = null;
    private String clientId = null;
    private String idTokenString = null;

    //Move to OidcDiscoveryConstants
    public static final String METADATA_KEY_ENDSESSION_ENDPOINT = "end_session_endpoint";

    ProviderAuthenticationResult authResult = new ProviderAuthenticationResult(AuthResult.SUCCESS, 200);

    public LogoutHandler(HttpServletRequest req, HttpServletResponse resp, OidcClientConfig oidcClientConfig, LogoutConfig logoutConfig, String idTokenString) {
        this.req = req;
        this.resp = resp;
        this.oidcClientConfig = oidcClientConfig;
        this.logoutConfig = logoutConfig;
        this.idTokenString = idTokenString;
        if (oidcClientConfig != null) {
            this.providerMetadata = oidcClientConfig.getProviderMetadata();
            clientId = oidcClientConfig.getClientId();
        }
    }

    private static final TraceComponent tc = Tr.register(LogoutHandler.class);

    public ProviderAuthenticationResult logout() throws ServletException, OidcDiscoveryException {

        LocalLogoutStrategy localLogout = new LocalLogoutStrategy(req);
        localLogout.logout();

        String endSessionEndPoint = getEndSessionEndpoint();
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

    String getEndSessionEndpoint() throws OidcDiscoveryException {
        // Provider metadata overrides properties discovered via providerUri
        //TODO: move to some util class
        String endSessionEndpoint = getEndSessionEndpointFromProviderMetadata();
        if (endSessionEndpoint == null) {
            endSessionEndpoint = getEndSessionEndpointFromDiscoveryMetadata();
        }
        return endSessionEndpoint;

    }

    String getEndSessionEndpointFromProviderMetadata() {
        if (providerMetadata != null) {
            String endSessionEndpoint = providerMetadata.getEndSessionEndpoint();
            if (endSessionEndpoint != null && !endSessionEndpoint.isEmpty()) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "End Session endpoint found in the provider metadata: [" + endSessionEndpoint + "]");
                }
                return endSessionEndpoint;
            }
        }
        return null;
    }

    String getEndSessionEndpointFromDiscoveryMetadata() throws OidcDiscoveryException {
        String tokenEndpoint = null;
        JSONObject discoveryData = getProviderDiscoveryMetadata(oidcClientConfig);
        if (discoveryData != null) {
            tokenEndpoint = (String) discoveryData.get(METADATA_KEY_ENDSESSION_ENDPOINT);
        }
        if (tokenEndpoint == null) {
            String nlsMessage = Tr.formatMessage(tc, "DISCOVERY_METADATA_MISSING_VALUE", METADATA_KEY_ENDSESSION_ENDPOINT);
            throw new OidcDiscoveryException(clientId, oidcClientConfig.getProviderURI(), nlsMessage);
        }
        return tokenEndpoint;
    }

}
