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
package io.openliberty.security.oauth20.web;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.oauth.core.internal.oauth20.OAuth20Constants;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.oauth20.ProvidersService;
import com.ibm.ws.security.oauth20.api.OAuth20Provider;
import com.ibm.ws.security.oauth20.web.EndpointUtils;
import com.ibm.ws.security.oauth20.web.OAuth20Request;
import com.ibm.ws.security.oauth20.web.OAuth20Request.EndpointType;

import io.openliberty.security.common.http.SupportedHttpMethodHandler;
import io.openliberty.security.oauth20.internal.config.OAuthEndpointSettings;
import io.openliberty.security.oauth20.internal.config.SpecificOAuthEndpointSettings;

@SuppressWarnings("restriction")
public class OAuthSupportedHttpMethodHandler extends SupportedHttpMethodHandler {

    private static TraceComponent tc = Tr.register(OAuthSupportedHttpMethodHandler.class);

    private OAuth20Request oauth20Request = null;

    EndpointUtils endpointUtils = new EndpointUtils();

    public OAuthSupportedHttpMethodHandler(HttpServletRequest request, HttpServletResponse response) {
        super(request, response);
        oauth20Request = getOAuth20RequestAttribute();
    }

    @Override
    @FFDCIgnore(IOException.class)
    public boolean isValidHttpMethodForRequest(HttpMethod requestMethod) {
        try {
            EndpointType endpointType = getEndpointType();
            if (endpointType == null) {
                return false;
            }
            Set<HttpMethod> supportedMethods = getSupportedMethodsForEndpoint(endpointType);
            if (supportedMethods != null && supportedMethods.contains(requestMethod)) {
                return true;
            }
        } catch (IOException e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Caught an exception determing whether the HTTP method is supported for endpoint: " + e);
            }
            return false;
        }
        return false;
    }

    @Override
    public void sendHttpOptionsResponse() throws IOException {
        EndpointType endpointType = getEndpointType();
        if (endpointType == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Failed to find a known endpoint type from the inbound request");
            }
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        Set<HttpMethod> supportedMethods = getSupportedMethodsForEndpoint(endpointType);
        setAllowHeaderAndSendResponse(supportedMethods);
    }

    EndpointType getEndpointType() throws IOException {
        if (oauth20Request == null) {
            return null;
        }
        return oauth20Request.getType();
    }

    Set<HttpMethod> getSupportedMethodsForEndpoint(EndpointType endpointType) {
        Set<HttpMethod> supportedMethods = getDefaultSupportedMethodsForEndpoint(endpointType);
        if (supportedMethods == null || supportedMethods.isEmpty()) {
            // If there are no HTTP methods supported by default, and configuring supported HTTP methods only further limits that set,
            // there's no point in doing any further work here.
            return supportedMethods;
        }
        Set<HttpMethod> configuredSupportedMethods = getConfiguredSupportedMethodsForEndpoint(endpointType);
        return getAdjustedSupportedMethodsForEndpoint(supportedMethods, configuredSupportedMethods);
    }

    Set<HttpMethod> getDefaultSupportedMethodsForEndpoint(EndpointType endpointType) {
        Set<HttpMethod> supportedMethods = new HashSet<HttpMethod>();
        supportedMethods.add(HttpMethod.OPTIONS);

        if (endpointType == EndpointType.authorize) {
            supportedMethods.add(HttpMethod.GET);
            supportedMethods.add(HttpMethod.HEAD);
            supportedMethods.add(HttpMethod.POST);
        } else if (endpointType == EndpointType.introspect) {
            supportedMethods.add(HttpMethod.GET);
            supportedMethods.add(HttpMethod.HEAD);
            supportedMethods.add(HttpMethod.POST);
        } else if (endpointType == EndpointType.revoke) {
            supportedMethods.add(HttpMethod.POST);
        } else if (endpointType == EndpointType.token) {
            supportedMethods.add(HttpMethod.POST);
        } else if (endpointType == EndpointType.coverage_map) {
            supportedMethods.add(HttpMethod.GET);
            supportedMethods.add(HttpMethod.HEAD);
        } else if (endpointType == EndpointType.registration) {
            supportedMethods.add(HttpMethod.GET);
            supportedMethods.add(HttpMethod.HEAD);
            supportedMethods.add(HttpMethod.POST);
            supportedMethods.add(HttpMethod.DELETE);
            supportedMethods.add(HttpMethod.PUT);
        } else if (endpointType == EndpointType.logout) {
            supportedMethods.add(HttpMethod.GET);
            supportedMethods.add(HttpMethod.HEAD);
            supportedMethods.add(HttpMethod.POST);
            supportedMethods.add(HttpMethod.DELETE);
            supportedMethods.add(HttpMethod.PUT);
        } else if (endpointType == EndpointType.app_password) {
            supportedMethods.add(HttpMethod.GET);
            supportedMethods.add(HttpMethod.HEAD);
            supportedMethods.add(HttpMethod.POST);
            supportedMethods.add(HttpMethod.DELETE);
        } else if (endpointType == EndpointType.app_token) {
            supportedMethods.add(HttpMethod.GET);
            supportedMethods.add(HttpMethod.HEAD);
            supportedMethods.add(HttpMethod.POST);
            supportedMethods.add(HttpMethod.DELETE);
        } else if (endpointType == EndpointType.clientManagement) {
            supportedMethods.add(HttpMethod.GET);
            supportedMethods.add(HttpMethod.HEAD);
        } else if (endpointType == EndpointType.personalTokenManagement) {
            supportedMethods.add(HttpMethod.GET);
            supportedMethods.add(HttpMethod.HEAD);
        } else if (endpointType == EndpointType.usersTokenManagement) {
            supportedMethods.add(HttpMethod.GET);
            supportedMethods.add(HttpMethod.HEAD);
        } else if (endpointType == EndpointType.clientMetatype) {
            supportedMethods.add(HttpMethod.GET);
            supportedMethods.add(HttpMethod.HEAD);
        } else {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Received a request for an unknown OAuth endpoint: [" + endpointType + "]");
            }
            return null;
        }
        return supportedMethods;
    }

    Set<HttpMethod> getConfiguredSupportedMethodsForEndpoint(EndpointType endpoint) {
        OAuthEndpointSettings endpointConfigSettings = getConfiguredOAuthEndpointSettings();
        if (endpointConfigSettings == null) {
            return null;
        }
        SpecificOAuthEndpointSettings specificEndpointSettings = endpointConfigSettings.getSpecificOAuthEndpointSettings(endpoint);
        if (specificEndpointSettings == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Did not find any specific OAuth endpoint settings for endpoint [" + endpoint + "]");
            }
            return null;
        }
        return specificEndpointSettings.getSupportedHttpMethods();
    }

    OAuthEndpointSettings getConfiguredOAuthEndpointSettings() {
        if (oauth20Request == null) {
            return null;
        }
        final String providerName = oauth20Request.getProviderName();
        OAuth20Provider provider = getOAuth20ProviderByName(providerName);
        if (provider == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Did not find an OAuth provider matching the name [{0}]", providerName);
            }
            return null;
        }
        return provider.getOAuthEndpointSettings();
    }

    OAuth20Provider getOAuth20ProviderByName(String providerName) {
        return ProvidersService.getOAuth20Provider(providerName);
    }

    Set<HttpMethod> getAdjustedSupportedMethodsForEndpoint(Set<HttpMethod> defaultSupportedMethods, Set<HttpMethod> configuredSupportedMethods) {
        if (defaultSupportedMethods == null || defaultSupportedMethods.isEmpty()) {
            // If there are no HTTP methods supported by default, and configuring supported HTTP methods only further limits that set,
            // there's no point in doing any further work here.
            return defaultSupportedMethods;
        }
        Set<HttpMethod> adjustedSupportedMethods = new HashSet<HttpMethod>(defaultSupportedMethods);
        if (configuredSupportedMethods != null) {
            adjustedSupportedMethods.retainAll(configuredSupportedMethods);
        }
        if (!adjustedSupportedMethods.contains(HttpMethod.OPTIONS)) {
            // The HTTP OPTIONS method is always supported
            adjustedSupportedMethods.add(HttpMethod.OPTIONS);
        }
        return adjustedSupportedMethods;
    }

    OAuth20Request getOAuth20RequestAttribute() {
        OAuth20Request oauth20Request = (OAuth20Request) request.getAttribute(OAuth20Constants.OAUTH_REQUEST_OBJECT_ATTR_NAME);
        if (oauth20Request == null) {
            oauth20Request = (OAuth20Request) request.getAttribute(OAuth20Constants.OIDC_REQUEST_OBJECT_ATTR_NAME);
            if (oauth20Request == null) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Failed to find OAuth20Request information from the inbound request");
                }
                return null;
            }
        }
        return oauth20Request;
    }

}