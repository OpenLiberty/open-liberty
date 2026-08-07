/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.clients.common;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.jwt.JwtBuilder;
import com.ibm.websphere.security.jwt.JwtToken;
import com.ibm.ws.webcontainer.security.openidconnect.OidcClient;

/**
 * Base implementation for {@link OAuthProtectedResourceMetadataService}, generic across the object containing the actual config,
 * allowing it to work with both openidConnectClient and socialLogin features, which mostly hold the same information but in
 * different classes.
 *
 * @param CONFIG
 *            the config class
 */
public abstract class OAuthProtectedResourceMetadataResolverBase<CONFIG> implements OAuthProtectedResourceMetadataService {

    private static final TraceComponent tc = Tr.register(OAuthProtectedResourceMetadataResolverBase.class);

    protected static final String WELL_KNOWN_PREFIX = "/.well-known/oauth-protected-resource";

    /**
     * Returns the advertised scopes from the config
     */
    protected abstract List<String> getAdvertisedScopes(CONFIG config);

    /**
     * @param config
     * @return
     */
    protected abstract String getJwtBuilderId(CONFIG config);

    /**
     * @param config
     * @return
     */
    protected abstract String getConfigId(CONFIG config);

    /**
     * Returns the authorization server identifier from the OIDC client configuration.
     * Prefers the issuer identifier.
     *
     * @param config
     *            matching OIDC client configuration
     * @return authorization server URL, or {@code null} if none is configured
     */
    protected abstract String getAuthorizationServer(CONFIG config);

    /**
     * {@inheritDoc}
     * <p>
     * Subclasses should implement this method so that it finds the relevant CONFIG for the request (if there is one) and calls
     * {@link #createMetadataJson(Object, String, HttpServletRequest)} and returns the result.
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
    public abstract String resolveMetadataJson(HttpServletRequest request, String protectedResourcePath, String absoluteResourceUrl);

    /**
     * Wraps an incoming metadata endpoint request so that it appears to target the protected
     * resource directly. This strips the {@code /.well-known/oauth-protected-resource} prefix
     * from the URI and URL so that {@link OidcClient#getOidcProvider} can match the request
     * against configured auth filters as if it were a real request to the protected resource.
     */
    // package visible for unit testing
    protected static class ProtectedResourceRequestWrapper extends HttpServletRequestWrapper {

        private final String protectedResourcePath;

        public ProtectedResourceRequestWrapper(HttpServletRequest request, String protectedResourcePath) {
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

    /**
     * Creates the OAuth 2.0 protected resource metadata JSON document.
     *
     * @param config
     *            matching OIDC client configuration
     * @param absoluteResourceUrl
     *            absolute protected resource URL
     * @return serialized JSON metadata document
     */
    protected String createMetadataJson(CONFIG config, String absoluteResourceUrl) {
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

        List<String> advertisedScopes = getAdvertisedScopes(config);
        if (advertisedScopes != null && !advertisedScopes.isEmpty()) {
            JSONArray scopesSupported = new JSONArray();
            scopesSupported.addAll(advertisedScopes);
            metadata.put("scopes_supported", scopesSupported);
        }

        String jwtBuilderId = getJwtBuilderId(config);
        if (jwtBuilderId != null && !jwtBuilderId.trim().isEmpty()) {
            String signedJwt = createSignedMetadata(jwtBuilderId, metadata);
            if (signedJwt != null) {
                metadata.put("signed_metadata", signedJwt);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Resolved OAuth protected resource metadata for resource [" + absoluteResourceUrl + "] using client [" + getConfigId(config) + "]");
        }

        return metadata.toString();
    }

    /**
     * Builds a compact JWS (signed JWT) whose payload mirrors the assembled protected
     * resource metadata claims, as required by RFC 9728 §4.
     *
     * @param jwtBuilderRef
     *            the JWT builder configuration ID to use for signing
     * @param metadata
     *            the protected resource metadata, which should be complete other than the {@code signed_metadata} entry
     * @return compact JWS string, or {@code null} if signing fails or {@code jwtBuilderRef} is blank
     */
    protected String createSignedMetadata(String jwtBuilderRef, JSONObject metadata) {
        if (jwtBuilderRef == null || jwtBuilderRef.trim().isEmpty()) {
            return null;
        }
        try {
            JwtBuilder builder = JwtBuilder.create(jwtBuilderRef);
            // Copy all entries from the metadata into the JWT as claims
            @SuppressWarnings("unchecked")
            Map<String, Object> metadataAsMap = metadata;
            builder.claim(metadataAsMap);

            JwtToken jwtToken = builder.buildJwt();
            return jwtToken.compact();
        } catch (Exception e) {
            Tr.warning(tc, "PRMD_SIGNED_METADATA_BUILD_FAILURE", jwtBuilderRef, e);
            return null;
        }
    }

    /**
     *
     */
    public OAuthProtectedResourceMetadataResolverBase() {
        super();
    }

}