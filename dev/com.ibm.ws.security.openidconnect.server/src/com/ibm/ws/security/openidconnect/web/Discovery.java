/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.web;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.oauth20.util.OIDCConstants;
import com.ibm.ws.security.openidconnect.server.internal.HttpUtils;
import com.ibm.ws.security.openidconnect.server.internal.OidcDiscoveryProviderConfig;
import com.ibm.ws.security.openidconnect.server.plugins.OIDCWASDiscoveryModel;
import com.ibm.ws.webcontainer.security.openidconnect.OidcServerConfig;

public class Discovery {
    private static TraceComponent tc = Tr.register(Discovery.class);

    public void processRequest(OidcServerConfig provider, HttpServletRequest request, HttpServletResponse response) throws IOException {
        OIDCWASDiscoveryModel discoveryObj = new OIDCWASDiscoveryModel();

        OidcDiscoveryProviderConfig discoverConfig = new OidcDiscoveryProviderConfig(provider.getProviderId(), request);

        // IssuerIdentifier from config takes precedence, if null or empty take the generated string
        String issuerFromConfig = provider.getIssuerIdentifier();
        discoveryObj.setIssuer((issuerFromConfig == null || issuerFromConfig.isEmpty()) ? discoverConfig.getIssuerId() : issuerFromConfig);

        discoveryObj.setAuthorizationEndpoint(discoverConfig.getEndpoint(OIDCConstants.KEY_OIDC_AUTHORIZATION_EP_QUAL));

        discoveryObj.setTokenEndpoint(discoverConfig.getEndpoint(OIDCConstants.KEY_OIDC_TOKEN_EP_QUAL));

        discoveryObj.setJwks_uri(discoverConfig.getEndpoint(OIDCConstants.KEY_OIDC_JWKS_URI_QAL));

        discoveryObj.setResponseTypesSupported(provider.getResponseTypesSupported());

        discoveryObj.setSubjectTypesSupported(provider.getSubjectTypesSupported());

        discoveryObj.setIdTokenSigningAlgValuesSupported(new String[] { provider.getIdTokenSigningAlgValuesSupported() });

        discoveryObj.setUserinfoEndpoint(discoverConfig.getEndpoint(OIDCConstants.KEY_OIDC_USERINFO_EP_QUAL));

        discoveryObj.setRegistrationEndpoint(discoverConfig.getEndpoint(OIDCConstants.KEY_OIDC_REGISTRATION_EP_QUAL));

        discoveryObj.setScopesSupported(provider.getScopesSupported());

        discoveryObj.setClaimsSupported(provider.getClaimsSupported());

        discoveryObj.setResponseModesSupported(provider.getResponseModesSupported());

        discoveryObj.setGrantTypesSupported(provider.getGrantTypesSupported());

        discoveryObj.setTokenEndpointAuthMethodsSupported(provider.getTokenEndpointAuthMethodsSupported());

        discoveryObj.setDisplayValuesSupported(provider.getDisplayValuesSupported());

        discoveryObj.setClaimTypesSupported(provider.getClaimTypesSupported());

        discoveryObj.setClaimsParameterSupported(provider.isClaimsParameterSupported());

        discoveryObj.setRequestParameterSupported(provider.isRequestParameterSupported());

        discoveryObj.setRequestUriParameterSupported(provider.isRequestUriParameterSupported());

        discoveryObj.setRequireRequestUriRegistration(provider.isRequireRequestUriRegistration());

        discoveryObj.setCheckSessionIframe(discoverConfig.getEndpoint(OIDCConstants.KEY_OIDC_CHECK_SESSION_IFRAME_QUAL));

        discoveryObj.setEndSessionEndpoint(discoverConfig.getEndpoint(OIDCConstants.KEY_OIDC_END_SESSION_EP_QUAL));

        discoveryObj.setIntrospectionEndpoint(discoverConfig.getEndpoint(OIDCConstants.KEY_OIDC_INTROSPECTION_EP_QUAL));

        discoveryObj.setCoverageMapEndpoint(discoverConfig.getEndpoint(OIDCConstants.KEY_OIDC_COVERAGE_MAP_EP_QUAL));

        discoveryObj.setBackingIdpUriPrefix(provider.getBackingIdpUriPrefix());

        discoveryObj.setProxyEndpoint(provider.getAuthProxyEndpointUrl());

        discoveryObj.setRevocationEndpoint(discoverConfig.getEndpoint(OIDCConstants.KEY_OIDC_REVOKE_EP_QUAL));

        discoveryObj.setAppPasswordsEndpoint(discoverConfig.getEndpoint(OIDCConstants.KEY_OIDC_APP_PASSWORDS_EP_QUAL));

        discoveryObj.setAppTokensEndpoint(discoverConfig.getEndpoint(OIDCConstants.KEY_OIDC_APP_TOKENS_EP_QUAL));

        discoveryObj.setPersonalTokenMgmtEndpoint(discoverConfig.getEndpoint(OIDCConstants.KEY_OIDC_PERSONAL_TOKEN_MGMT_EP_QUAL));

        discoveryObj.setUsersTokenMgmtEndpoint(discoverConfig.getEndpoint(OIDCConstants.KEY_OIDC_USERS_TOKEN_MGMT_EP_QUAL));

        discoveryObj.setClientMgmtEndpoint(discoverConfig.getEndpoint(OIDCConstants.KEY_OIDC_CLIENT_MGMT_EP_QUAL));

        discoveryObj.setPkceCodeChallengeMethodsSupported(OIDCConstants.OIDC_DISC_PKCE_CODE_CHALLENGE_METHODS_SUPPORTED);

        String discoverJSONString = discoveryObj.toJSONString();

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, (new StringBuffer()).append("Discover response for provider ").append(provider.getProviderId()).append(" :").append(discoverJSONString).toString());
        }

        //Set Headers and Status
        response.setContentType(HttpUtils.CT_APPLICATION_JSON);
        response.setStatus(HttpServletResponse.SC_OK);
        String cacheCtrHdr = HttpUtils.constructCacheControlHeaderWithMaxAge(true, "3600");//Specify Cache-Control Public @ 1 Hr (3600 Sec)
        response.setHeader(HttpUtils.CACHE_CONTROL, cacheCtrHdr);

        //Deserialize Discovery document into JSON string
        response.getWriter().print(discoverJSONString);
        response.flushBuffer();
    }
}
