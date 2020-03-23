/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.server.internal;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.ibm.ws.security.oauth20.util.OIDCConstants;
import com.ibm.ws.security.oauth20.web.OAuth20Request.EndpointType;

/**
 * Class to helps manage access to the OIDC Discovery configuration metadata
 * per provider.
 */
public class OidcDiscoveryProviderConfig {

    private static final Map<String, String> endpointMap = new HashMap<String, String>();
    private HttpServletRequest request;
    private String issuerId;

    //Disallow default construction
    @SuppressWarnings("unused")
    private OidcDiscoveryProviderConfig() {
    }

    public OidcDiscoveryProviderConfig(String providerId, HttpServletRequest request) {
        this.request = request;
        this.issuerId = getCalculatedIssuerId(providerId);
    }

    public String getIssuerId() {
        return this.issuerId;
    }

    /**
     * Calculates and returns the calculated endpoint.
     *
     * @param endpointProp is a qualified endpoint property key
     * @return Endpoint for a given provider. Returns calculated value.
     */
    public String getEndpoint(String endpointProp) {
        return getCalculatedEndpoint(OidcDiscoveryProviderConfig.endpointMap.get(endpointProp));
    }

    private String getCalculatedIssuerId(String providerId) {
        String fullServletPath = HttpUtils.getFullCtxServletPath(this.request);

        return (new StringBuffer()).append(fullServletPath).append((fullServletPath.endsWith("/") ? "" : "/")).append(providerId).toString();
    }

    private String getCalculatedEndpoint(String endpoint) {
        return (new StringBuffer()).append(this.issuerId).append((this.issuerId.endsWith("/") ? "" : "/")).append(endpoint).toString();
    }

    static {
        endpointMap.put(OIDCConstants.KEY_OIDC_AUTHORIZATION_EP_QUAL, EndpointType.authorize.name());
        endpointMap.put(OIDCConstants.KEY_OIDC_INTROSPECTION_EP_QUAL, EndpointType.introspect.name());
        endpointMap.put(OIDCConstants.KEY_OIDC_TOKEN_EP_QUAL, EndpointType.token.name());
        endpointMap.put(OIDCConstants.KEY_OIDC_JWKS_URI_QAL, EndpointType.jwk.name());
        endpointMap.put(OIDCConstants.KEY_OIDC_USERINFO_EP_QUAL, EndpointType.userinfo.name());
        endpointMap.put(OIDCConstants.KEY_OIDC_REGISTRATION_EP_QUAL, EndpointType.registration.name());
        endpointMap.put(OIDCConstants.KEY_OIDC_CHECK_SESSION_IFRAME_QUAL, EndpointType.check_session_iframe.name());
        endpointMap.put(OIDCConstants.KEY_OIDC_END_SESSION_EP_QUAL, EndpointType.end_session.name());
        endpointMap.put(OIDCConstants.KEY_OIDC_COVERAGE_MAP_EP_QUAL, EndpointType.coverage_map.name());
        endpointMap.put(OIDCConstants.KEY_OIDC_PROXY_EP_QUAL, EndpointType.proxy.name());
        endpointMap.put(OIDCConstants.KEY_OIDC_REVOKE_EP_QUAL, EndpointType.revoke.name());
        endpointMap.put(OIDCConstants.KEY_OIDC_APP_PASSWORDS_EP_QUAL, EndpointType.app_password_effective_name.replace('_', '-'));
        endpointMap.put(OIDCConstants.KEY_OIDC_APP_TOKENS_EP_QUAL, EndpointType.app_token_effective_name.replace('_', '-'));
        endpointMap.put(OIDCConstants.KEY_OIDC_PERSONAL_TOKEN_MGMT_EP_QUAL, EndpointType.personalTokenManagement.name());
        endpointMap.put(OIDCConstants.KEY_OIDC_USERS_TOKEN_MGMT_EP_QUAL, EndpointType.usersTokenManagement.name());
        endpointMap.put(OIDCConstants.KEY_OIDC_CLIENT_MGMT_EP_QUAL, EndpointType.clientManagement.name());
    }
}
