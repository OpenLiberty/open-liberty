/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.client.internal;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.openidconnect.clients.common.OidcClientConfig;
import com.ibm.ws.webcontainer.security.openidconnect.OidcClient;

/**
 * Gets the OAuth 2.0 protected resource metadata for a given protected resource path.
 * <p>
 * This component owns the runtime and configuration integration for the protected resource
 * metadata endpoint. The well-known web bundle delegates request handling to this service
 * so that the endpoint remains part of the {@code openidConnectClient} feature while still
 * using a dedicated web context path.
 * </p>
 */
@Component(name = "com.ibm.ws.security.openidconnect.client.internal.OAuthProtectedResourceMetadataResolver", configurationPolicy = ConfigurationPolicy.IGNORE, service = OAuthProtectedResourceMetadataResolver.class, property = { "service.vendor=IBM" })
public class OAuthProtectedResourceMetadataResolver {

    private static final TraceComponent tc = Tr.register(OAuthProtectedResourceMetadataResolver.class);

    private static final String WELL_KNOWN_PREFIX = "/.well-known/oauth-protected-resource";

    private volatile OidcClientImpl oidcClient;

    @Reference(service = OidcClient.class)
    protected void setOidcClient(OidcClient oidcClient) {
        this.oidcClient = (OidcClientImpl) oidcClient;
    }

    protected void unsetOidcClient(OidcClient oidcClient) {
        if (this.oidcClient == oidcClient) {
            this.oidcClient = null;
        }
    }

    /**
     * Returns the OAuth 2.0 protected resource metadata JSON for the given request and
     * protected resource path, or {@code null} if no OIDC client configuration matches.
     *
     * @param request               the incoming metadata endpoint HTTP request
     * @param protectedResourcePath normalized protected resource path, e.g. {@code /myApp/protected}
     * @param absoluteResourceUrl   absolute protected resource URL to include in the metadata document,
     *                                  e.g. {@code https://localhost:9443/myApp/protected}
     * @return serialized JSON metadata document, or {@code null} if no match
     */
    public String resolveMetadataJson(HttpServletRequest request, String protectedResourcePath, String absoluteResourceUrl) {
        OidcClientImpl client = oidcClient;
        if (client == null) {
            return null;
        }

        // Adapt the metadata request to look like a direct request to the protected resource
        // so that the auth filter can match it against the configured URL patterns.
        // We use getOidcProviderByAuthFilter rather than getOidcProvider to avoid the
        // IExtendedRequest cast that happens in the provider-hint code path.
        HttpServletRequest resourceRequest = new ProtectedResourceRequestWrapper(request, protectedResourcePath);

        String providerId = client.getOidcProviderByAuthFilter(resourceRequest);
        if (providerId == null) {
            return null;
        }

        OidcClientConfig config = client.getOidcClientConfig(request, providerId);
        if (config == null) {
            return null;
        }

        return createMetadataJson(config, absoluteResourceUrl);
    }

    /**
     * Creates the OAuth 2.0 protected resource metadata JSON document.
     *
     * @param config             matching OIDC client configuration
     * @param absoluteResourceUrl absolute protected resource URL
     * @return serialized JSON metadata document
     */
    String createMetadataJson(OidcClientConfig config, String absoluteResourceUrl) {
        JSONObject metadata = new JSONObject();

        metadata.put("resource", absoluteResourceUrl);

        String authorizationServer = getAuthorizationServer(config);
        if (authorizationServer != null) {
            JSONArray authorizationServers = new JSONArray();
            authorizationServers.add(authorizationServer);
            metadata.put("authorization_servers", authorizationServers);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Resolved OAuth protected resource metadata for resource [" + absoluteResourceUrl + "] using client [" + config.getId() + "]");
        }

        return metadata.toString();
    }

    /**
     * Returns the authorization server identifier from the OIDC client configuration.
     * Prefers the issuer identifier.
     *
     * @param config matching OIDC client configuration
     * @return authorization server URL, or {@code null} if none is configured
     */
    String getAuthorizationServer(OidcClientConfig config) {
        String issuer = config.getIssuerIdentifier();
        if (issuer != null && !issuer.trim().isEmpty()) {
            return issuer;
        }
        return null;
    }

    /**
     * Wraps an incoming metadata endpoint request so that it appears to target the protected
     * resource directly. This strips the {@code /.well-known/oauth-protected-resource} prefix
     * from the URI and URL so that {@link OidcClient#getOidcProvider} can match the request
     * against configured auth filters as if it were a real request to the protected resource.
     */
    static class ProtectedResourceRequestWrapper extends HttpServletRequestWrapper {

        private final String protectedResourcePath;

        ProtectedResourceRequestWrapper(HttpServletRequest request, String protectedResourcePath) {
            super(request);
            this.protectedResourcePath = protectedResourcePath;
        }

        @Override
        public String getRequestURI() {
            return getContextPath() + protectedResourcePath;
        }

        @Override
        public StringBuffer getRequestURL() {
            // Replace everything from the well-known prefix onwards with the protected resource path
            String originalUrl = super.getRequestURL().toString();
            int wellKnownIndex = originalUrl.indexOf(WELL_KNOWN_PREFIX);
            if (wellKnownIndex >= 0) {
                return new StringBuffer(originalUrl.substring(0, wellKnownIndex) + protectedResourcePath);
            }
            return new StringBuffer(originalUrl);
        }

        @Override
        public String getServletPath() {
            return protectedResourcePath;
        }

        @Override
        public String getContextPath() {
            return "";
        }

        @Override
        public String getPathInfo() {
            return null;
        }
    }
}
