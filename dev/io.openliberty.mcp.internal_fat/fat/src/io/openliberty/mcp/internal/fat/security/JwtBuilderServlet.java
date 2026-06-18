/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.security;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import com.ibm.websphere.security.jwt.InvalidClaimException;
import com.ibm.websphere.security.jwt.JwtBuilder;
import com.ibm.websphere.security.jwt.JwtToken;

import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObjectBuilder;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Servlet for demonstrating JWT Builder functionality and RFC 9728
 * Protected Resource Metadata with signed_metadata support.
 *
 * Uses {@link MockOidcClientConfig} to simulate the configuration that
 * would come from an openidConnectClient element in server.xml. This allows
 * testing metadata generation with different configurations without requiring
 * the actual OIDC runtime.
 *
 * Pass {@code ?mock=unsigned-metadata} to exercise the unsigned (no jwtBuilderRef) path;
 * any other value or no parameter selects the signed path.
 */
@WebServlet("/jwtBuilder")
public class JwtBuilderServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final String RESOURCE_IDENTIFIER = "https://api.toyshop.example.com/shop/v1";
    private static final String AS_ISSUER = "https://localhost:9445/oidc/endpoint/SampleProvider";
    private static final String TEST_SCOPES = "toys_browse,toys_search,cart_read,cart_write";
    private static final List<String> BEARER_METHODS = List.of("header");

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String jsonResponse = generateProtectedMetadataResponse(request);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().print(jsonResponse);
    }

    /**
     * Generate RFC 9728 Protected Resource Metadata from the selected config.
     * The unsigned metadata is built first as the shared model; JWT claims are
     * derived from that same object. If signing fails, metadata is returned
     * without signed_metadata.
     */
    private String generateProtectedMetadataResponse(HttpServletRequest request) {
        MockOidcClientConfig oidcConfig = configFrom(request.getParameter("mock"));
        MockProtectedResourceMetadataConfig prmdConfig = oidcConfig.protectedResourceMetadata();
        ProtectedResourceMetadata unsigned = new ProtectedResourceMetadata(
                                                                           RESOURCE_IDENTIFIER,
                                                                           List.of(oidcConfig.issuerIdentifier()),
                                                                           prmdConfig.jwksUri(request),
                                                                           prmdConfig.advertisedScopesAsList(),
                                                                           BEARER_METHODS,
                                                                           null);

        // Derive JWT claims from the unsigned metadata object, then produce final response.
        ProtectedResourceMetadata signed = unsigned.withSignedMetadata(generateJWT(unsigned, oidcConfig));
        return signed.toJson();
    }

    private static MockOidcClientConfig configFrom(String param) {
        return switch (param == null ? "" : param) {
            case "unsigned-metadata" -> new MockOidcClientConfig(AS_ISSUER, new MockProtectedResourceMetadataConfig(TEST_SCOPES, null));
            case "signed-metadata"   -> new MockOidcClientConfig(AS_ISSUER, new MockProtectedResourceMetadataConfig(TEST_SCOPES, "testJwtBuilder"));
            default                  -> new MockOidcClientConfig(AS_ISSUER, new MockProtectedResourceMetadataConfig(TEST_SCOPES, "testJwtBuilder"));
        };
    }

    /**
     * Generates the signed metadata JWT whose claims mirror the unsigned metadata.
     * Returns null if the configured jwtBuilderRef is blank or generation fails — caller omits signed_metadata.
     */
    private String generateJWT(ProtectedResourceMetadata metadata, MockOidcClientConfig oidcConfig) {
        String builderId = oidcConfig.protectedResourceMetadata().jwtBuilderRef();
        if (builderId == null || builderId.isBlank()) {
            return null;
        }
        try {
            JwtBuilder builder = JwtBuilder.create(builderId);
            builder.claim("resource", metadata.resource());
            builder.claim("iss", metadata.resource());
            builder.claim("authorization_servers", metadata.authorizationServers().toArray(new String[0]));
            addListClaim(builder, "scopes_supported", metadata.scopesSupported());
            addListClaim(builder, "bearer_methods_supported", metadata.bearerMethodsSupported());
            JwtToken token = builder.buildJwt();
            return token.compact();
        } catch (Exception e) {
            System.err.println("Failed to generate signed metadata JWT with builder '"
                               + builderId + "': " + e.getMessage());
            return null;
        }
    }

    private static void addListClaim(JwtBuilder builder, String claimName, List<String> values) throws InvalidClaimException {
        if (values != null && !values.isEmpty()) {
            builder.claim(claimName, values.toArray(new String[0]));
        }
    }

    // ==================== Configuration Records ====================

    /**
     * Mock configuration representing an openidConnectClient element in server.xml.
     * Models the attributes relevant to protected resource metadata generation.
     */
    record MockOidcClientConfig(String issuerIdentifier,
                                MockProtectedResourceMetadataConfig protectedResourceMetadata) {}

    /**
     * Mock configuration representing a protectedResourceMetadata sub-element:
     *
     * <pre>{@code
     * <protectedResourceMetadata
     *     advertisedScopes="scope1,scope2,..."
     *     jwtBuilderRef="builderConfigId"/>
     * }</pre>
     */
    record MockProtectedResourceMetadataConfig(String advertisedScopes,
                                               String jwtBuilderRef) {

        /** Parses the comma-separated advertisedScopes into a List. Returns null if blank. */
        List<String> advertisedScopesAsList() {
            if (advertisedScopes == null || advertisedScopes.isBlank()) {
                return null;
            }
            return Arrays.asList(advertisedScopes.split(","));
        }

        /**
         * Derives the JWK endpoint URI from the jwtBuilderRef and current request.
         * Returns null when jwtBuilderRef is blank — jwks_uri will be omitted from metadata.
         */
        String jwksUri(HttpServletRequest request) {
            if (jwtBuilderRef == null || jwtBuilderRef.isBlank()) {
                return null;
            }
            StringBuilder uri = new StringBuilder().append(request.getScheme())
                                                   .append("://")
                                                   .append(request.getServerName());

            uri.append(':').append(request.getServerPort());
            uri.append("/jwt/ibm/api/")
               .append(jwtBuilderRef)
               .append("/jwk");
            return uri.toString();
        }
    }

    // ==================== Output Record ====================

    /**
     * Represents the RFC 9728 Protected Resource Metadata JSON response.
     * Optional fields are omitted from the JSON output when null/empty.
     */
    private record ProtectedResourceMetadata(
                                             String resource,
                                             List<String> authorizationServers,
                                             String jwksUri,
                                             List<String> scopesSupported,
                                             List<String> bearerMethodsSupported,
                                             String signedMetadata) {

        ProtectedResourceMetadata {
            if (resource == null || resource.isEmpty()) {
                throw new IllegalArgumentException("resource is required");
            }
        }

        /** Returns a copy of this record with the given signedMetadata value. */
        ProtectedResourceMetadata withSignedMetadata(String signedMetadata) {
            return new ProtectedResourceMetadata(resource, authorizationServers, jwksUri,
                                                 scopesSupported, bearerMethodsSupported, signedMetadata);
        }

        public String toJson() {
            JsonObjectBuilder builder = Json.createObjectBuilder();
            builder.add("resource", resource);
            addIfPresent(builder, "authorization_servers", authorizationServers);
            addIfPresent(builder, "jwks_uri", jwksUri);
            addIfPresent(builder, "scopes_supported", scopesSupported);
            addIfPresent(builder, "bearer_methods_supported", bearerMethodsSupported);
            addIfPresent(builder, "signed_metadata", signedMetadata);
            return builder.build().toString();
        }

        private static void addIfPresent(JsonObjectBuilder builder, String key, String value) {
            if (value != null && !value.isEmpty()) {
                builder.add(key, value);
            }
        }

        private static void addIfPresent(JsonObjectBuilder builder, String key, List<String> values) {
            if (values != null && !values.isEmpty()) {
                JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
                for (String item : values) {
                    arrayBuilder.add(item);
                }
                builder.add(key, arrayBuilder);
            }
        }
    }
}
