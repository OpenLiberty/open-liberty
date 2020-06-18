/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.openidconnect.web;

import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.oauth.core.internal.oauth20.OAuth20Constants;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.oauth20.web.OAuth20Request.EndpointType;
import com.ibm.ws.security.openidconnect.server.internal.OidcServerConfigImpl;
import com.ibm.ws.security.openidconnect.web.OidcEndpointServices;
import com.ibm.ws.security.openidconnect.web.OidcRequest;

import io.openliberty.security.oauth20.web.OAuthSupportedHttpMethodHandler;
import io.openliberty.security.openidconnect.server.config.OidcEndpointSettings;
import io.openliberty.security.openidconnect.server.config.SpecificOidcEndpointSettings;

@SuppressWarnings("restriction")
public class OidcSupportedHttpMethodHandler extends OAuthSupportedHttpMethodHandler {

    private static TraceComponent tc = Tr.register(OidcSupportedHttpMethodHandler.class);

    protected OidcRequest oidcRequest = null;
    protected String oidcProviderName = null;
    protected OidcEndpointServices endpointServices = null;
    protected OidcServerConfigImpl oidcConfig = null;

    public OidcSupportedHttpMethodHandler(HttpServletRequest request, HttpServletResponse response, OidcEndpointServices endpointServices) {
        super(request, response);
        this.endpointServices = endpointServices;
        oidcRequest = getOidcRequestAttribute();
        if (oidcRequest != null) {
            oidcProviderName = oidcRequest.getProviderName();
            oidcConfig = getOidcProviderConfig();
            if (oidcConfig != null) {
                oauth20ProviderName = oidcConfig.getOauthProviderName();
                oauthProvider = getOAuth20Provider();
            }
        }
    }

    @Override
    protected EndpointType getEndpointType() {
        if (oidcRequest != null) {
            return oidcRequest.getType();
        }
        return super.getEndpointType();
    }

    @Override
    protected Set<HttpMethod> getDefaultSupportedMethodsForEndpoint(EndpointType endpointType) {
        Set<HttpMethod> supportedMethods = new HashSet<HttpMethod>();
        supportedMethods.add(HttpMethod.OPTIONS);

        if (endpointType == EndpointType.discovery
            || endpointType == EndpointType.userinfo
            || endpointType == EndpointType.end_session
            || endpointType == EndpointType.check_session_iframe
            || endpointType == EndpointType.jwk) {
            // All of the OIDC endpoints support the same set of HTTP methods
            supportedMethods.add(HttpMethod.GET);
            supportedMethods.add(HttpMethod.HEAD);
            supportedMethods.add(HttpMethod.POST);
        } else {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Received a request for an unknown OIDC endpoint: [" + endpointType + "]. Checking if it's an OAuth endpoint...");
            }
            return super.getDefaultSupportedMethodsForEndpoint(endpointType);
        }
        return supportedMethods;
    }

    @Override
    protected Set<HttpMethod> getConfiguredSupportedMethodsForEndpoint(EndpointType endpoint) {
        OidcEndpointSettings endpointConfigSettings = getConfiguredOidcEndpointSettings();
        if (endpointConfigSettings == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Did not find any OIDC endpoint settings for provider. Checking for OAuth endpoint settings...");
            }
            return super.getConfiguredSupportedMethodsForEndpoint(endpoint);
        }
        SpecificOidcEndpointSettings specificEndpointSettings = endpointConfigSettings.getSpecificOidcEndpointSettings(endpoint);
        if (specificEndpointSettings == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Did not find any specific OIDC endpoint settings for endpoint [" + endpoint + "]. Checking if it's an OAuth endpoint...");
            }
            return super.getConfiguredSupportedMethodsForEndpoint(endpoint);
        }
        return specificEndpointSettings.getSupportedHttpMethods();
    }

    OidcEndpointSettings getConfiguredOidcEndpointSettings() {
        if (oidcConfig == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Did not find an OIDC provider matching the name [" + oidcProviderName + "]");
            }
            return null;
        }
        return oidcConfig.getOidcEndpointSettings();
    }

    @FFDCIgnore(Exception.class)
    OidcServerConfigImpl getOidcProviderConfig() {
        if (oidcProviderName == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Cannot look up configured endpoint settings because OIDC provider name is not known");
            }
            return null;
        }
        if (endpointServices == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Did not find OIDC endpoint services object to use to get configured OIDC endpoint settings");
            }
            return null;
        }
        try {
            return (OidcServerConfigImpl) endpointServices.getOidcServerConfig(response, oidcProviderName);
        } catch (Exception e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Caught an exception attempting to get OIDC server configuration for provider [" + oidcProviderName + "]: " + e);
            }
        }
        return null;
    }

    OidcRequest getOidcRequestAttribute() {
        OidcRequest oidcRequest = (OidcRequest) request.getAttribute(OAuth20Constants.OIDC_REQUEST_OBJECT_ATTR_NAME);
        if (oidcRequest == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Failed to find OidcRequest information from the inbound request");
            }
            return null;
        }
        return oidcRequest;
    }

}