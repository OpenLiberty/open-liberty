/*******************************************************************************
 * Copyright (c) 2025, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.utils;

import static io.openliberty.mcp.internal.fat.utils.TestConstants.ACCEPT;
import static io.openliberty.mcp.internal.fat.utils.TestConstants.MCP_PROTOCOL_VERSION;
import static io.openliberty.mcp.internal.fat.utils.TestConstants.MCP_SESSION_ID;
import static io.openliberty.mcp.internal.fat.utils.TestConstants.VALUE_ACCEPT_DEFAULT;
import static io.openliberty.mcp.internal.fat.utils.TestConstants.VALUE_APPLICATION_JSON;
import static io.openliberty.mcp.internal.fat.utils.TestConstants.VALUE_MCP_PROTOCOL_VERSION;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import javax.net.ssl.SSLContext;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.rules.ExternalResource;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import componenttest.topology.impl.LibertyServer;

/**
 * A client for testing the MCP server. Handles session setup, standard header
 * propagation, and session teardown.
 *
 * <h2>Creating a client</h2>
 * Use the {@link Builder} to construct an instance:
 *
 * <pre>{@code
 * // As a JUnit @Rule (session opened before each test, closed after):
 * @Rule
 * public McpClient client = McpClient.builder(server, "/myApp").build();
 *
 * // Manually managed (e.g. when you need multiple sessions):
 * try (McpClient client = McpClient.builder(server, "/myApp").build()) {
 *     client.initialize();
 *     client.callMCP(jsonRequest);
 * }
 * }</pre>
 *
 * <h2>Making requests</h2>
 * <ul>
 *   <li>{@link #callMCP(String)} — standard call; applies all client-level auth and session
 *       headers, expects HTTP 200 with a response body.</li>
 *   <li>{@link #callMCPRaw(String, RequestOptions)} — full control over expected status code,
 *       per-call auth override, and custom endpoint path. Use for negative/exceptional test
 *       cases.</li>
 *   <li>{@link #callMCPNotification(String)} — sends a notification, expects HTTP 202 with
 *       no response body.</li>
 *   <li>{@link #listAllTools()} — convenience method that follows pagination to return all tools.</li>
 * </ul>
 *
 * <h2>HTTPS</h2>
 * Pass an {@link SSLContext} via {@link Builder#sslContext(SSLContext)} to make all requests
 * over TLS. When set, the client uses the secure port and the supplied context to trust
 * self-signed certificates.
 */
public class McpClient extends ExternalResource implements AutoCloseable {

    private static final Class<?> c = McpClient.class;

    private final LibertyServer server;
    private final String contextRoot;
    private final StateMode mode;
    private final String username;
    private final String password;
    private final String bearerToken;

    /** Non-null when this client speaks HTTPS. */
    private final SSLContext sslContext;
    private final java.net.http.HttpClient httpClient;

    /**
     * Optional fallback HTTP client used for {@link #getDiscoveryHttpClient()} when this
     * client has no {@link SSLContext}. Allows plain-HTTP MCP clients to follow discovery
     * URLs that point to HTTPS endpoints (e.g. Keycloak) without switching the MCP
     * transport to HTTPS.
     */
    private java.net.http.HttpClient discoveryHttpClient;

    private static final String DEFAULT_MCP_PATH = "/mcp";
    private final String path;
    private boolean sessionDeleted = false;
    private String sessionId;

    public static enum StateMode {
        /** Uses sessions and session IDs to maintain state across requests. */
        STATEFUL,
        /** Each request is independent with no session information. */
        STATELESS
    }

    // -------------------------------------------------------------------------
    // Builder
    // -------------------------------------------------------------------------

    /**
     * Returns a new {@link Builder} for the given server and context root.
     *
     * @param server      the Liberty server to target
     * @param contextRoot the context root of the MCP application (leading {@code /} optional)
     */
    public static Builder builder(LibertyServer server, String contextRoot) {
        return new Builder(server, contextRoot);
    }

    /**
     * Fluent builder for {@link McpClient}.
     *
     * <p>Only {@code server} and {@code contextRoot} are required. All other options have
     * sensible defaults: stateful mode, no authentication, plain HTTP, default MCP path.
     */
    public static final class Builder {
        private final LibertyServer server;
        private final String contextRoot;
        private String path = DEFAULT_MCP_PATH;
        private StateMode mode = StateMode.STATEFUL;
        private String username;
        private String password;
        private String bearerToken;
        private SSLContext sslContext;

        private Builder(LibertyServer server, String contextRoot) {
            this.server = server;
            this.contextRoot = contextRoot;
        }

        /** Sets the MCP endpoint path (default: {@code /mcp}). */
        public Builder path(String path) {
            this.path = path;
            return this;
        }

        /** Sets whether the server is in stateful or stateless mode (default: {@link StateMode#STATEFUL}). */
        public Builder mode(StateMode mode) {
            this.mode = mode;
            return this;
        }

        /** Configures Basic Authentication credentials applied to every request. */
        public Builder basicAuth(String username, String password) {
            this.username = username;
            this.password = password;
            return this;
        }

        /** Configures a Bearer token applied to every request. */
        public Builder bearerToken(String token) {
            this.bearerToken = token;
            return this;
        }

        /**
         * Configures TLS. When set, all MCP requests and discovery requests are made over HTTPS
         * using the supplied context to trust self-signed certificates.
         */
        public Builder sslContext(SSLContext sslContext) {
            this.sslContext = sslContext;
            return this;
        }

        /** Builds and returns a new {@link McpClient}. */
        public McpClient build() {
            return new McpClient(this);
        }
    }

    private McpClient(Builder b) {
        super();
        this.server = b.server;
        this.contextRoot = b.contextRoot.startsWith("/") ? b.contextRoot : "/" + b.contextRoot;
        this.path = b.path.startsWith("/") ? b.path : "/" + b.path;
        this.mode = b.mode;
        this.username = b.username;
        this.password = b.password;
        this.bearerToken = b.bearerToken;
        this.sslContext = b.sslContext;
        this.httpClient = sslContext == null
                          ? java.net.http.HttpClient.newBuilder().version(java.net.http.HttpClient.Version.HTTP_1_1).build()
                          : java.net.http.HttpClient.newBuilder().version(java.net.http.HttpClient.Version.HTTP_1_1).sslContext(sslContext).build();
    }

    // -------------------------------------------------------------------------
    // URL helpers
    // -------------------------------------------------------------------------

    private String getMcpPath() {
        return contextRoot + path;
    }

    private String getMcpUrl() {
        if (sslContext != null) {
            return "https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + getMcpPath();
        }
        return "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + getMcpPath();
    }

    /** Returns the {@link LibertyServer} this client targets. */
    public LibertyServer getServer() {
        return server;
    }

    /** Returns the context root of the MCP application (always starts with {@code /}). */
    public String getContextRoot() {
        return contextRoot;
    }

    /** Returns {@code true} when this client makes MCP requests over HTTPS. */
    public boolean isSecure() {
        return sslContext != null;
    }

    /**
     * Returns the {@link SSLContext} used by this client, or {@code null} if plain HTTP.
     * Useful when creating a new authenticated client that should share the same TLS config.
     */
    public SSLContext getSslContext() {
        return sslContext;
    }

    /**
     * Sets a fallback {@link java.net.http.HttpClient} used for discovery requests when this
     * client is plain HTTP but the discovery chain includes HTTPS endpoints (e.g. Keycloak).
     *
     * @param client the HTTP client to use for discovery requests
     * @return {@code this} for fluent chaining
     */
    public McpClient withDiscoveryClient(java.net.http.HttpClient client) {
        this.discoveryHttpClient = client;
        return this;
    }

    /**
     * Returns the appropriate {@link java.net.http.HttpClient} for discovery requests.
     * Priority: discovery client > this client's HTTP client.
     */
    public java.net.http.HttpClient getDiscoveryHttpClient() {
        return discoveryHttpClient != null ? discoveryHttpClient : httpClient;
    }

    // -------------------------------------------------------------------------
    // Lifecycle (ExternalResource / AutoCloseable)
    // -------------------------------------------------------------------------

    /**
     * Sends the MCP {@code initialize} request and stores the resulting session ID.
     * Called automatically when used as a JUnit {@code @Rule}; call manually when
     * managing the lifecycle yourself.
     *
     * @throws Exception if initialization fails
     */
    public void initialize() throws Exception {
        try {
            before();
        } catch (Throwable t) {
            if (t instanceof Exception) {
                throw (Exception) t;
            }
            throw new Exception("Initialization failed", t);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void before() throws Throwable {
        // Clear any stale session ID so it is not sent on the initialize request.
        sessionId = null;

        String request = """
                        {
                          "jsonrpc": "2.0",
                          "id": "1",
                          "method": "initialize",
                          "params": {
                            "protocolVersion": "2025-11-25",
                            "capabilities": {
                              "roots": {
                                "listChanged": true
                              },
                              "sampling": {},
                              "elicitation": {}
                            },
                            "clientInfo": {
                              "name": "fat-test-client",
                              "title": "FAT Test Client",
                              "version": "1.0.0"
                            }
                          }
                        }
                        """;

        HttpResponse<String> response = post(getMcpUrl(), request, null);

        if (response.statusCode() != 200) {
            throw new Exception("initialize failed with HTTP " + response.statusCode() + ": " + response.body());
        }

        String expectedResponse = """
                        {
                          "jsonrpc": "2.0",
                          "id": "1",
                          "result": {
                            "protocolVersion": "2025-11-25",
                          }
                        }
                        """;
        JSONAssert.assertEquals(expectedResponse, response.body(), JSONCompareMode.LENIENT);

        sessionId = response.headers().firstValue(MCP_SESSION_ID).orElse(null);
        switch (mode) {
            case STATEFUL -> assertNotNull(sessionId);
            case STATELESS -> assertNull(sessionId);
        }

        String contentType = response.headers().firstValue("Content-Type").orElse(null);
        assertThat(contentType, containsString(VALUE_APPLICATION_JSON));

        // Notify the server that initialization was successful
        callMCPNotification("""
                        {
                          "jsonrpc": "2.0",
                          "method": "notifications/initialized"
                        }
                        """);
    }

    /** {@inheritDoc} */
    @Override
    protected void after() {
        if (mode.equals(StateMode.STATEFUL) && !sessionDeleted && sessionId != null) {
            try {
                deleteSession();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Implements {@link AutoCloseable}: deletes the MCP session if stateful and not already
     * deleted, identical to the JUnit {@code @Rule} teardown.
     */
    @Override
    public void close() {
        after();
    }

    public String getSessionId() {
        return this.sessionId;
    }

    /**
     * Marks the session as already deleted so that the automatic teardown does not attempt
     * to delete it again. Use this when the session has been invalidated externally (e.g.
     * by a server config change or explicit {@link #deleteSession()} call).
     */
    public void markSessionDeleted() {
        this.sessionDeleted = true;
    }

    /** Sends a DELETE request for the current session and marks it as deleted. */
    public void deleteSession() throws Exception {
        deleteSession(sessionId);
    }

    /**
     * Sends a DELETE request for the given session ID and marks this client's session as
     * deleted. Useful when testing cross-session deletion.
     */
    public void deleteSession(String mcpSessionId) throws Exception {
        if (mode.equals(StateMode.STATEFUL)) {
            java.net.http.HttpRequest deleteRequest = java.net.http.HttpRequest.newBuilder()
                            .uri(URI.create(getMcpUrl()))
                            .header(MCP_SESSION_ID, mcpSessionId)
                            .DELETE()
                            .build();
            httpClient.send(deleteRequest, BodyHandlers.ofString());
            this.sessionDeleted = true;
        }
    }

    // -------------------------------------------------------------------------
    // Core HTTP plumbing
    // -------------------------------------------------------------------------

    /**
     * Sends a POST to {@code url} with the standard MCP headers, optional session ID,
     * and client-level auth credentials (Basic or Bearer). Does not validate the status code.
     */
    private HttpResponse<String> post(String url, String jsonBody, String sessionIdOverride) throws Exception {
        java.net.http.HttpRequest.Builder builder = java.net.http.HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header(ACCEPT, VALUE_ACCEPT_DEFAULT)
                        .header(MCP_PROTOCOL_VERSION, VALUE_MCP_PROTOCOL_VERSION)
                        .header("Content-Type", VALUE_APPLICATION_JSON)
                        .POST(java.net.http.HttpRequest.BodyPublishers.ofString(jsonBody));

        String sid = sessionIdOverride != null ? sessionIdOverride : sessionId;
        if (mode == StateMode.STATEFUL && sid != null) {
            builder.header(MCP_SESSION_ID, sid);
        }

        if (bearerToken != null) {
            builder.header("Authorization", "Bearer " + bearerToken);
        } else if (username != null && password != null) {
            String encoded = Base64.getEncoder().encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
            builder.header("Authorization", "Basic " + encoded);
        }

        return httpClient.send(builder.build(), BodyHandlers.ofString());
    }

    // -------------------------------------------------------------------------
    // Public call methods
    // -------------------------------------------------------------------------

    /**
     * Sends a standard MCP POST request. Applies all client-level auth and session headers.
     * Expects HTTP 200 with a response body.
     *
     * @param jsonRequestBody the JSON-RPC request body
     * @return the response body as a string
     * @throws Exception if the request fails or the response status is not 200
     */
    public String callMCP(String jsonRequestBody) throws Exception {
        if (mode == StateMode.STATEFUL && sessionId == null) {
            throw new IllegalStateException("In stateful mode but don't have a sessionId, did you forget to use @Rule or call initialize()?");
        }
        HttpResponse<String> response = post(getMcpUrl(), jsonRequestBody, null);
        if (response.statusCode() != 200) {
            throw new Exception("Unexpected HTTP status expected:<200> but was:<" + response.statusCode() + ">\nBody: " + response.body());
        }
        return response.body();
    }

    /**
     * Sends an MCP notification. Expects HTTP 202 with no response body.
     *
     * @param jsonRequestBody the JSON-RPC notification body (no {@code id} field)
     * @throws Exception if the request fails or the response is not 202
     */
    public void callMCPNotification(String jsonRequestBody) throws Exception {
        HttpResponse<String> response = post(getMcpUrl(), jsonRequestBody, null);
        if (response.statusCode() != 202) {
            throw new Exception("Unexpected HTTP status for notification expected:<202> but was:<" + response.statusCode() + ">\nBody: " + response.body());
        }
        String body = response.body();
        if (body != null && !body.isEmpty()) {
            throw new Exception("Notification request received a response body: " + body);
        }
    }

    /**
     * Returns the list of all tools, merging paginated responses into a single tools list.
     */
    public String listAllTools() throws Exception {
        JSONArray allTools = new JSONArray();
        String cursor = null;
        String lastResponse = null;
        int requestId = 1;

        do {
            String request;
            if (cursor == null) {
                request = String.format("""
                                {
                                   "jsonrpc": "2.0",
                                   "id": %d,
                                   "method": "tools/list"
                                 }
                                """, requestId++);
            } else {
                request = String.format("""
                                {
                                   "jsonrpc": "2.0",
                                   "id": %d,
                                   "method": "tools/list",
                                   "params": {
                                     "cursor": "%s"
                                   }
                                 }
                                """, requestId++, cursor);
            }

            lastResponse = callMCP(request);

            JSONObject jsonResponse = new JSONObject(lastResponse);
            JSONObject result = jsonResponse.getJSONObject("result");

            JSONArray tools = result.getJSONArray("tools");
            for (int i = 0; i < tools.length(); i++) {
                allTools.put(tools.get(i));
            }

            cursor = result.optString("nextCursor", null);
            if (cursor != null) {
                org.junit.Assert.assertTrue(!cursor.isEmpty());
            }
        } while (cursor != null);

        JSONObject combinedResult = new JSONObject();
        combinedResult.put("tools", allTools);

        JSONObject combinedResponse = new JSONObject();
        combinedResponse.put("jsonrpc", "2.0");
        combinedResponse.put("id", 1);
        combinedResponse.put("result", combinedResult);

        return combinedResponse.toString();
    }

    // -------------------------------------------------------------------------
    // Raw / low-level call method
    // -------------------------------------------------------------------------

    /**
     * Sends an MCP POST request with full caller control over the expected HTTP status code,
     * per-call authentication, and endpoint path. Use this for negative tests and other cases
     * that do not fit the standard happy path.
     *
     * <p>Example — expect a 401 with no credentials:
     * <pre>{@code
     * McpDetailedAuthResponse r = client.callMCPRaw(body,
     *     RequestOptions.builder().expectedStatus(401).build());
     * }</pre>
     *
     * <p>Example — one-off call with different credentials:
     * <pre>{@code
     * try (McpClient adminClient = McpClient.builder(server, contextRoot)
     *                                       .mode(StateMode.STATEFUL)
     *                                       .basicAuth("admin", "secret")
     *                                       .build()) {
     *     adminClient.initialize();
     *     String response = adminClient.callMCP(body);
     * }
     * }</pre>
     *
     * @param jsonRequestBody the JSON-RPC request body
     * @param options         options controlling the expected status, auth override, and path
     * @return a {@link McpDetailedAuthResponse} with status code, headers, and body
     * @throws Exception if the request fails unexpectedly
     */
    public McpDetailedAuthResponse callMCPRaw(String jsonRequestBody, RequestOptions options) throws Exception {
        String url = options.appendPath != null
                     ? (sslContext != null
                        ? "https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort()
                        : "http://" + server.getHostname() + ":" + server.getHttpDefaultPort())
                       + contextRoot + options.appendPath
                     : getMcpUrl();

        // Build a temporary request, applying per-call auth override if present
        java.net.http.HttpRequest.Builder builder = java.net.http.HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header(ACCEPT, VALUE_ACCEPT_DEFAULT)
                        .header(MCP_PROTOCOL_VERSION, VALUE_MCP_PROTOCOL_VERSION)
                        .header("Content-Type", VALUE_APPLICATION_JSON)
                        .POST(java.net.http.HttpRequest.BodyPublishers.ofString(jsonRequestBody));

        // sessionIdOverride takes precedence over the client's session ID
        if (options.sessionIdOverride != null) {
            builder.header(MCP_SESSION_ID, options.sessionIdOverride);
        } else if (mode == StateMode.STATEFUL && sessionId != null) {
            builder.header(MCP_SESSION_ID, sessionId);
        }

        // Per-call basicAuth override takes precedence over client-level credentials.
        if (options.basicAuthUsername != null && options.basicAuthPassword != null) {
            String encoded = Base64.getEncoder()
                            .encodeToString((options.basicAuthUsername + ":" + options.basicAuthPassword).getBytes(StandardCharsets.UTF_8));
            builder.header("Authorization", "Basic " + encoded);
        } else if (bearerToken != null) {
            builder.header("Authorization", "Bearer " + bearerToken);
        } else if (username != null && password != null) {
            String encoded = Base64.getEncoder()
                            .encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
            builder.header("Authorization", "Basic " + encoded);
        }

        HttpResponse<String> response = httpClient.send(builder.build(), BodyHandlers.ofString());

        if (!options.anyStatus && response.statusCode() != options.expectedStatus) {
            throw new Exception("[McpClient] callMCPRaw(" + url + ") -> HTTP "
                                + response.statusCode() + " (expected " + options.expectedStatus + ")\nBody: " + response.body());
        }

        return new McpDetailedAuthResponse(
                        response.statusCode(),
                        response.headers().firstValue("WWW-Authenticate").orElse(null),
                        response.headers().firstValue("Content-Type").orElse(null),
                        response.body());
    }

    // -------------------------------------------------------------------------
    // RequestOptions
    // -------------------------------------------------------------------------

    /**
     * Options for {@link McpClient#callMCPRaw(String, RequestOptions)}.
     * Build with {@link RequestOptions#builder()}.
     */
    public static final class RequestOptions {
        final int expectedStatus;
        final boolean anyStatus;
        final String appendPath;
        final String sessionIdOverride;
        final String basicAuthUsername;
        final String basicAuthPassword;

        private RequestOptions(OptionsBuilder b) {
            this.expectedStatus = b.expectedStatus;
            this.anyStatus = b.anyStatus;
            this.appendPath = b.appendPath;
            this.sessionIdOverride = b.sessionIdOverride;
            this.basicAuthUsername = b.basicAuthUsername;
            this.basicAuthPassword = b.basicAuthPassword;
        }

        public static OptionsBuilder builder() {
            return new OptionsBuilder();
        }

        public static final class OptionsBuilder {
            private int expectedStatus = 200;
            private boolean anyStatus = false;
            private String appendPath;
            private String sessionIdOverride;
            private String basicAuthUsername;
            private String basicAuthPassword;

            /** Expected HTTP status code (default: 200). */
            public OptionsBuilder expectedStatus(int code) {
                this.expectedStatus = code;
                return this;
            }

            /** Accept any HTTP status code — disables the status assertion entirely. */
            public OptionsBuilder anyStatus() {
                this.anyStatus = true;
                return this;
            }

            /**
             * Append this path to the context root instead of using the client's configured
             * MCP path. Useful for testing a different endpoint within the same application.
             */
            public OptionsBuilder appendPath(String path) {
                this.appendPath = path;
                return this;
            }

            /** Override the session ID header for this single call only. */
            public OptionsBuilder sessionId(String sessionId) {
                this.sessionIdOverride = sessionId;
                return this;
            }

            /**
             * Override the Basic Auth credentials for this single call only.
             * Use when the call must be made on an existing session but with different credentials,
             * e.g. testing that a cross-user request is rejected.
             */
            public OptionsBuilder basicAuth(String username, String password) {
                this.basicAuthUsername = username;
                this.basicAuthPassword = password;
                return this;
            }

            public RequestOptions build() {
                return new RequestOptions(this);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Response record
    // -------------------------------------------------------------------------

    /**
     * Detailed response data returned by {@link #callMCPRaw(String, RequestOptions)}.
     */
    public record McpDetailedAuthResponse(int statusCode, String wwwAuthenticate, String contentType, String body) {}

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
