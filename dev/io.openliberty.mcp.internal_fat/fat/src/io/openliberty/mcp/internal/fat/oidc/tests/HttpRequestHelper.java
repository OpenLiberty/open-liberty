/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.oidc.tests;

import static org.junit.Assert.assertEquals;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONObject;

/**
 * Shared test helpers used by both the HTTP and HTTPS test scenarios in
 * {@link AuthorizationFlowTests}.
 *
 * <p>This class contains stateless utility methods that are independent of whether
 * the MCP endpoint is being called over HTTP or HTTPS:
 * <ul>
 * <li>{@link #toolCallRequest(String)} — builds the JSON-RPC tools/call body</li>
 * <li>{@link #extractResourceMetadataUrl(String)} — parses the {@code resource_metadata} URL
 * from a {@code WWW-Authenticate} header</li>
 * <li>{@link #fetchJson(String, KeycloakContainer)} — GETs a JSON document over HTTP or HTTPS
 * (Keycloak URLs only)</li>
 * <li>{@link #fetchAccessToken(String, String, String, KeycloakContainer)} — obtains a Bearer
 * token from Keycloak via ROPC</li>
 * </ul>
 *
 * <p>HTTPS-specific helpers that require a Liberty-trusting {@link HttpClient}
 * (i.e. {@code postMcpHttps}, {@code fetchJsonHttps}, {@code discoverTokenEndpointHttps})
 * live in {@link HttpsRequestHelper}.
 */
public final class HttpRequestHelper {

    // MCP protocol header names and values
    public static final String HEADER_ACCEPT = "Accept";
    public static final String HEADER_MCP_VERSION = "MCP-Protocol-Version";
    public static final String HEADER_CONTENT_TYPE = "Content-Type";
    public static final String VALUE_ACCEPT = "application/json, text/event-stream";
    public static final String VALUE_MCP_VERSION = "2025-11-25";
    public static final String VALUE_JSON = "application/json";

    public static final String AS_METADATA_SUFFIX = "/.well-known/openid-configuration";

    /**
     * Builds a JSON-RPC {@code tools/call} request body for the given tool name
     *
     * @param toolName the name of the MCP tool to call
     * @return the JSON-RPC request body as a string
     */
    public static String toolCallRequest(String toolName) {
        return String.format("""
                        {
                          "jsonrpc": "2.0",
                          "id": 1,
                          "method": "tools/call",
                          "params": {
                            "name": "%s",
                            "arguments": {}
                          }
                        }
                        """, toolName);
    }

    /**
     * Parses the {@code resource_metadata="<url>"} parameter out of a
     * {@code WWW-Authenticate} header value.
     *
     * @param wwwAuthenticate the value of the {@code WWW-Authenticate} response header
     * @return the resource_metadata URL, or {@code null} if not found
     */
    public static String extractResourceMetadataUrl(String wwwAuthenticate) {
        if (wwwAuthenticate == null) {
            return null;
        }
        Matcher matcher = Pattern.compile("resource_metadata=\"([^\"]+)\"").matcher(wwwAuthenticate);
        return matcher.find() ? matcher.group(1) : null;
    }

    /**
     * GETs a JSON document. Routes {@code https://} URLs through the Keycloak-trusting
     * {@link HttpClient} (sufficient for Keycloak discovery URLs) and {@code http://} URLs
     * through a plain client.
     *
     * <p><b>Note:</b> for {@code https://} Liberty URLs (e.g. the {@code resource_metadata}
     * endpoint) use {@link HttpsRequestHelper#fetchJsonHttps} instead, which also
     * trusts the Liberty certificate.
     *
     * @param url the URL to fetch
     * @param keycloakContainer the Keycloak container whose HTTP client to use for https:// URLs
     * @return the parsed JSON response body
     * @throws Exception if the request fails or the response status is not 200
     */
    public static JSONObject fetchJson(String url, KeycloakContainer keycloakContainer) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                                         .uri(URI.create(url))
                                         .header(HEADER_ACCEPT, VALUE_JSON)
                                         .GET()
                                         .build();
        HttpClient client = url.startsWith("https://") ? keycloakContainer.getHttpClient() : HttpClient.newHttpClient();
        HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            System.out.println("[KeycloakContainerHelper] fetchJson(" + url + ") -> HTTP "
                               + response.statusCode() + "\nBody: " + response.body());
        }
        assertEquals("Expected HTTP 200 from " + url, 200, response.statusCode());
        return new JSONObject(response.body());
    }

    /**
     * Performs an OAuth 2.0 Resource Owner Password Credentials grant against
     * {@code tokenEndpoint} and returns the access token string.
     *
     * <p>Always uses the Keycloak-trusting {@link HttpClient} because the token
     * endpoint is always an {@code https://} Keycloak URL.
     *
     * @param tokenEndpoint the full URL of the token endpoint
     * @param username the resource owner username
     * @param password the resource owner password
     * @param keycloakContainer the Keycloak container whose HTTP client to use
     * @return the access token string
     * @throws Exception if the token request fails
     */
    public static String fetchAccessToken(String tokenEndpoint, String username, String password,
                                          KeycloakContainer keycloakContainer)
                    throws Exception {
        String formData = String.join("&",
                                      "client_id=" + encode(KeycloakContainer.PUBLIC_CLIENT_ID),
                                      "username=" + encode(username),
                                      "password=" + encode(password),
                                      "grant_type=password");

        HttpRequest request = HttpRequest.newBuilder()
                                         .uri(URI.create(tokenEndpoint))
                                         .header(HEADER_CONTENT_TYPE, "application/x-www-form-urlencoded")
                                         .POST(HttpRequest.BodyPublishers.ofString(formData))
                                         .build();

        HttpResponse<String> response = keycloakContainer.getHttpClient().send(request, BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("Token request failed. Status: " + response.statusCode()
                                       + "\nBody: " + response.body());
        }

        Matcher matcher = Pattern.compile("\"access_token\"\\s*:\\s*\"([^\"]+)\"").matcher(response.body());
        if (!matcher.find()) {
            throw new RuntimeException("No access_token in token response: " + response.body());
        }
        return matcher.group(1);
    }

    // Private helpers

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
