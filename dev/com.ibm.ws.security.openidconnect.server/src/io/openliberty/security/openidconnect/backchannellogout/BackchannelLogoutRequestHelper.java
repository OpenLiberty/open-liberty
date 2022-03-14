/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.openidconnect.backchannellogout;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.oauth20.plugins.OidcBaseClient;
import com.ibm.ws.webcontainer.security.openidconnect.OidcServerConfig;

public class BackchannelLogoutRequestHelper {

    private static TraceComponent tc = Tr.register(BackchannelLogoutRequestHelper.class);

    private final HttpServletRequest request;
    private final OidcServerConfig oidcServerConfig;

    public BackchannelLogoutRequestHelper(HttpServletRequest request, OidcServerConfig oidcServerConfig) {
        this.request = request;
        this.oidcServerConfig = oidcServerConfig;
    }

    /**
     * Uses the provided ID token string to build logout tokens and sends back-channel logout requests to all of the necessary
     * RPs. If the ID token contains multiple audiences, logout tokens are created for each client audience. Logout tokens are
     * also created for all RPs that the OP is aware of having active or recently valid sessions.
     */
    public void sendBackchannelLogoutRequests(String idTokenString) {
        if (idTokenString == null || idTokenString.isEmpty()) {
            return;
        }
        Map<OidcBaseClient, List<String>> logoutTokens = null;
        try {
            LogoutTokenBuilder tokenBuilder = new LogoutTokenBuilder(request, oidcServerConfig);
            logoutTokens = tokenBuilder.buildLogoutTokensFromIdTokenString(idTokenString);
        } catch (LogoutTokenBuilderException e) {
            Tr.error(tc, "OIDC_SERVER_BACKCHANNEL_LOGOUT_REQUEST_ERROR", new Object[] { oidcServerConfig.getProviderId(), e.getMessage() });
            return;
        }
        // TODO
    }

}
