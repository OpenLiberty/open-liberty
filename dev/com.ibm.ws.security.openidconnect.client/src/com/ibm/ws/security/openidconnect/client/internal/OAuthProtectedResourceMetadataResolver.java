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

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.jwt.JwtBuilder;
import com.ibm.websphere.security.jwt.JwtToken;
import com.ibm.ws.security.openidconnect.client.OAuthProtectedResourceMetadataService;
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
@Component(configurationPolicy = ConfigurationPolicy.IGNORE, service = OAuthProtectedResourceMetadataService.class)
public class OAuthProtectedResourceMetadataResolver implements OAuthProtectedResourceMetadataService {

    private static final TraceComponent tc = Tr.register(OAuthProtectedResourceMetadataResolver.class);

    private static final String WELL_KNOWN_PREFIX = "/.well-known/oauth-protected-resource";

    @Reference
    private OidcClientImpl oidcClientImpl;

    /**
     * Test hook: allows unit tests to inject a mock {@link OidcClientImpl} without DS.
     */
    void setOidcClient(OidcClientImpl client) {
        this.oidcClientImpl = client;
    }

    /**
     * Returns the OAuth 2.0 protected resource metadata JSON for the given request and
     * protected resource path, or {@code null} if no OIDC client configuration matches.
     *
     * @param request
     *            the incoming metadata endpoint HTTP request
     * @param protectedResourcePath
     *            normalized protected resource path, e.g. {@code /myApp/protected}
     * @param absoluteResourceUrl
     *            absolute protected resource URL to include in the metadata document,
     *            e.g. {@code https://localhost:9443/myApp/protected}
     * @return serialized JSON metadata document, or {@code null} if no match
     */
    @Override
    public String resolveMetadataJson(HttpServletRequest request, String protectedResourcePath, String absoluteResourceUrl) {
        OidcClientImpl client = oidcClientImpl;
        if (client == null) {
            return null;
        }

        // Adapt the metadata request to look like a direct request to the protected resource
        // so that the standard OIDC provider-selection flow can evaluate auth filters against
        // the configured URL patterns.
        HttpServletRequest resourceRequest = new ProtectedResourceRequestWrapper(request, protectedResourcePath);

        String providerId = client.getOidcProvider(resourceRequest);

        if (providerId == null) {
            return null;
        }

        OidcClientConfig config = client.getOidcClientConfig(request, providerId); // OidcClientImpl

        if (config == null) {
            return null;
        }

        if (!config.getServeProtectedResourceMetadata()) {
            return null;
        }

        return createMetadataJson(config, absoluteResourceUrl, request);
    }

    /**
     * Creates the OAuth 2.0 protected resource metadata JSON document.
     * Package-scoped for unit testing.
     *
     * @param config
     *            matching OIDC client configuration
     * @param absoluteResourceUrl
     *            absolute protected resource URL
     * @param request
     *            the incoming HTTP request (used to derive {@code jwks_uri})
     * @return serialized JSON metadata document
     */
    // package visible for unit testing
    String createMetadataJson(OidcClientConfig config, String absoluteResourceUrl, HttpServletRequest request) {
        JSONObject metadata = new JSONObject();

        metadata.put("resource", absoluteResourceUrl);

        String authorizationServer = getAuthorizationServer(config);
        if (authorizationServer != null) {
            JSONArray authorizationServers = new JSONArray();
            for (String issuer : authorizationServer.split(",")) {
                String trimmedIssuer = issuer.trim();
                if (!trimmedIssuer.isEmpty()) {
                    authorizationServers.add(trimmedIssuer);
                }
            }
            if (!authorizationServers.isEmpty()) {
                metadata.put("authorization_servers", authorizationServers);
            }
        }

        List<String> advertisedScopes = config.getProtectedResourceMetadataAdvertisedScopes();
        if (advertisedScopes != null && !advertisedScopes.isEmpty()) {
            JSONArray scopesSupported = new JSONArray();
            scopesSupported.addAll(advertisedScopes);
            metadata.put("scopes_supported", scopesSupported);
        }

        String jwtBuilderId = config.getProtectedResourceMetadataJwtBuilderId();
        if (jwtBuilderId != null && !jwtBuilderId.trim().isEmpty()) {
            String signedJwt = createSignedMetadata(jwtBuilderId, metadata);
            if (signedJwt != null) {
                metadata.put("signed_metadata", signedJwt);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Resolved OAuth protected resource metadata for resource [" + absoluteResourceUrl + "] using client [" + config.getId() + "]");
        }

        return metadata.toString();
    }

    /**
     * Builds a compact JWS (signed JWT) whose payload mirrors the assembled protected
     * resource metadata claims, as required by RFC 9728 §4.
     *
     * @param jwtBuilderId
     *            the JWT builder configuration ID to use for signing
     * @param metadata
     *            the protected resource metadata, which should be complete other than the {@code signed_metadata} entry
     * @return compact JWS string, or {@code null} if signing fails or {@code jwtBuilderRef} is blank
     */
    // package visible for unit testing
    String createSignedMetadata(String jwtBuilderId, JSONObject metadata) {
        if (jwtBuilderId == null || jwtBuilderId.trim().isEmpty()) {
            return null;
        }
        try {
            JwtBuilder builder = JwtBuilder.create(jwtBuilderId);
            // Copy all entries from the metadata into the JWT as claims
            @SuppressWarnings("unchecked")
            Map<String, Object> metadataAsMap = metadata;
            builder.claim(metadataAsMap);

            JwtToken jwtToken = builder.buildJwt();
            return jwtToken.compact();
        } catch (Exception e) {
            Tr.warning(tc, "PRMD_SIGNED_METADATA_BUILD_FAILURE", jwtBuilderId, e);
            return null;
        }
    }

    /**
     * Returns the authorization server identifier from the OIDC client configuration.
     * Prefers the issuer identifier.
     *
     * @param config
     *            matching OIDC client configuration
     * @return authorization server URL, or {@code null} if none is configured
     */
    // package visible for unit testing
    String getAuthorizationServer(OidcClientConfig config) {
        String issuer = config.getIssuerIdentifier();
        if (issuer != null && !issuer.trim().isEmpty()) {
            return issuer;
        }
        String validationEndpoint = config.getValidationEndpointUrl();
        if (validationEndpoint != null) {
            int lastSlashIndex = validationEndpoint.lastIndexOf("/");
            return validationEndpoint.substring(0, lastSlashIndex);
        }
        return null;
    }

    /**
     * Wraps an incoming metadata endpoint request so that it appears to target the protected
     * resource directly. This strips the {@code /.well-known/oauth-protected-resource} prefix
     * from the URI and URL so that {@link OidcClient#getOidcProvider} can match the request
     * against configured auth filters as if it were a real request to the protected resource.
     */
    // package visible for unit testing
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
