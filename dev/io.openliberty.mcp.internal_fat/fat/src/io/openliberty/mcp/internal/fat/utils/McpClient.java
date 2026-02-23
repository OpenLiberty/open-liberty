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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.rules.ExternalResource;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpRequest;

/**
 * A client for testing the MCP server, which takes care of setting up a session, passing
 * through the correct headers on each request and deleting the session after the test.
 * <p>
 * This class must be initialized as a JUnit test rule in a field in the test class:
 *
 * <pre>{@code
 * @Rule
 * public McpClient client = new McpClient(server, "/[whatever your root file is named]");
 * }</pre>
 *
 * <p>
 * Then within your test method, you can use the client to make requests:
 *
 * <pre>{@code
 * String jsonResponse = client.callMCP(jsonRequest);
 * }</pre>
 */
public class McpClient extends ExternalResource {

    private final LibertyServer server;
    private final String contextRoot;
    private final StateMode mode;
    private final String username;
    private final String password;

    private static final String DEFAULT_MCP_PATH = "/mcp";
    private final String path;
    private boolean sessionDeleted = false;
    private String sessionId;

    public static enum StateMode {
        // STATEFUL - Uses sessions and session IDs to maintain state across requests
        STATEFUL,
        // STATELESS - Each request is independent with no session information e.g. authentication will be required for each request
        STATELESS
    }

    public McpClient(LibertyServer server, String contextRoot) {
        this(server, contextRoot, DEFAULT_MCP_PATH, StateMode.STATEFUL, null, null);
    }

    /**
     * @param server the {@link LibertyServer} instance used to send requests
     * @param contextRoot the contextRoot for MCP app.
     * @param mode whether to expect the server to be in stateful or stateless mode
     */
    public McpClient(LibertyServer server, String contextRoot, StateMode mode) {
        this(server, contextRoot, DEFAULT_MCP_PATH, mode, null, null);
    }

    public McpClient(LibertyServer server, String contextRoot, String path) {
        this(server, contextRoot, path, StateMode.STATEFUL, null, null);
    }

    /**
     * @param server the {@link LibertyServer} instance used to send requests
     * @param contextRoot the contextRoot for MCP app.
     * @param path The full request endpoint path e.g {@code path + "/mcp"}.
     */
    public McpClient(LibertyServer server, String contextRoot, String path, StateMode mode) {
        this(server, contextRoot, path, mode, null, null);
    }

    /**
     * Use this constructor if you have the StateMode, username and password
     */
    public McpClient(LibertyServer server, String contextRoot, StateMode mode, String username, String password) {
        this(server, contextRoot, DEFAULT_MCP_PATH, mode, username, password);
    }

    /**
     * @param server the {@link LibertyServer} instance used to send requests
     * @param path the base endpoint path for MCP. The full request path will be {@code path + "/mcp"}.
     * @param mode whether to expect the server to be in stateful or stateless mode
     * @param username for basic auth
     * @param password for basic auth
     */
    public McpClient(LibertyServer server, String contextRoot, String path, StateMode mode, String username, String password) {
        super();
        this.server = server;
        this.contextRoot = contextRoot.startsWith("/") ? contextRoot : "/" + contextRoot;
        this.path = path.startsWith("/") ? path : "/" + path;
        this.mode = mode;
        this.username = username;
        this.password = password;
    }

    private String getMcpPath() {
        return contextRoot + path;
    }

    /** {@inheritDoc} */
    @Override
    protected void before() throws Throwable {
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
                              "name": "FAT Test Client",
                              "title": "FAT Test Client",
                              "version": "1.0.0"
                            }
                          }
                        }
                        """;

        HttpRequest httpRequest = new HttpRequest(server, getMcpPath())
                                                                       .requestProp(ACCEPT, VALUE_ACCEPT_DEFAULT)
                                                                       .requestProp(MCP_PROTOCOL_VERSION, VALUE_MCP_PROTOCOL_VERSION)
                                                                       .jsonBody(request)
                                                                       .method("POST");
        if (username != null && password != null) {
            httpRequest.basicAuth(username, password);
        }
        String response = httpRequest.run(String.class);

        String expectedResponse = """
                        {
                          "jsonrpc": "2.0",
                          "id": "1",
                          "result": {
                            "protocolVersion": "2025-11-25",
                          }
                        }
                        """;
        JSONAssert.assertEquals(expectedResponse, response, JSONCompareMode.LENIENT);

        sessionId = httpRequest.getResponseHeader(MCP_SESSION_ID);
        switch (mode) {
            case STATEFUL -> assertNotNull(sessionId);
            case STATELESS -> assertNull(sessionId);
        }

        String contentType = httpRequest.getResponseHeader("Content-Type");
        assertThat(contentType, containsString(VALUE_APPLICATION_JSON));

        // Notify the server that initialization was successful
        String notification = """
                         {
                           "jsonrpc": "2.0",
                           "method": "notifications/initialized"
                         }
                        """;

        callMCPNotification(notification);
    }

    @Override
    protected void after() {
        if (mode.equals(StateMode.STATEFUL)) {
            if (sessionDeleted) {
                return;
            }
            try {
                new HttpRequest(server, getMcpPath()).requestProp(MCP_SESSION_ID, sessionId)
                                                     .method("DELETE")
                                                     .run(String.class);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public String getSessionId() {
        return this.sessionId;
    }

    public void deleteSession() {
        if (mode.equals(StateMode.STATEFUL)) {
            try {
                new HttpRequest(server, getMcpPath())
                                                     .requestProp(MCP_SESSION_ID, sessionId)
                                                     .method("DELETE")
                                                     .run(String.class);

                this.sessionDeleted = true;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     *
     * Sets up and runs a HTTP request
     * Only requests a sessionId if Stateful mode is enabled
     *
     * @param request
     * @param jsonRequestBody
     * @return
     */
    private String setupAndRunRequest(final HttpRequest request, String jsonRequestBody) throws Exception {
        request.requestProp(ACCEPT, VALUE_ACCEPT_DEFAULT)
               .requestProp(MCP_PROTOCOL_VERSION, VALUE_MCP_PROTOCOL_VERSION)
               .jsonBody(jsonRequestBody)
               .method("POST");

        if (mode.equals(StateMode.STATEFUL)) {
            if (sessionId == null) {
                throw new IllegalStateException("In stateful mode but don't have a sessionId, did you forget to use @Rule?");
            }
            request.requestProp(MCP_SESSION_ID, sessionId);
        }
        return request.run(String.class);
    }

    /**
     * Call MCP server endpoint with a given JSON-RPC request body and return the response as a string.
     * The request includes required headers: Accept, MCP-Protocol-Version, and Mcp-Session-Id.
     * This method expects a successful response (200 OK) with a response body.
     */
    public String callMCP(String jsonRequestBody) throws Exception {
        final HttpRequest request = new HttpRequest(server, getMcpPath());
        return setupAndRunRequest(request, jsonRequestBody);
    }

    public String callMCPwithBasicAuth(String jsonRequestBody, String user, String password) throws Exception {
        final HttpRequest request = new HttpRequest(server, getMcpPath()).basicAuth(user, password);
        return setupAndRunRequest(request, jsonRequestBody);
    }

    public String callMCPAuthorisationErrorExpected(String jsonRequestBody) throws Exception {
        final HttpRequest request = new HttpRequest(server, getMcpPath()).expectCode(403);
        return setupAndRunRequest(request, jsonRequestBody);
    }

    public String callMCPwithBasicAuth_AuthorisationErrorExpected(String jsonRequestBody, String user, String password) throws Exception {
        final HttpRequest request = new HttpRequest(server, getMcpPath()).expectCode(403)
                                                                         .basicAuth(user, password);
        return setupAndRunRequest(request, jsonRequestBody);
    }

    /**
     * Call MCP server with a custom endpoint, and an expected response code
     */
    public String callMCPCustomized(String jsonRequestBody, String appendPath, int expectedCode) throws Exception {
        final HttpRequest request = new HttpRequest(server, contextRoot + appendPath).expectCode(expectedCode);
        return setupAndRunRequest(request, jsonRequestBody);
    }

    /**
     * Call MCP server notification endpoint, and provide a 202 expected response code. No response body is returned
     * If a response body is returned, or a response code that is not 202, and exception is thrown
     */
    public void callMCPNotification(String jsonRequestBody) throws Exception {
        final HttpRequest request = new HttpRequest(server, getMcpPath()).expectCode(202);
        String response = setupAndRunRequest(request, jsonRequestBody);
        assertNull("Notification request received a response", response);
    }

    public String callMCPNotificationWithBasicAuth(String jsonRequestBody, String user, String password) throws Exception {
        final HttpRequest request = new HttpRequest(server, getMcpPath()).expectCode(202).basicAuth(user, password);
        String response = setupAndRunRequest(request, jsonRequestBody);
        assertNull("Notification request received a response", response);
        return response;
    }

    public String callMCPNotificationWithBasicAuthForbiddenErrorExpected(String jsonRequestBody, String user, String password)
                    throws Exception {
        final HttpRequest request = new HttpRequest(server, getMcpPath()).expectCode(403).basicAuth(user, password);
        String response = setupAndRunRequest(request, jsonRequestBody);
        return response;
    }

    /**
     * Sends an MCP request with Basic Authentication credentials and asserts
     * that the server responds with the expected HTTP status code.
     *
     * This is used for negative security tests where we want to verify that:
     *
     * - 401 Unauthorized is returned when authentication fails (wrong credentials)
     * - 403 Forbidden is returned when authentication succeeds but authorization fails (wrong role)
     */
    public void callMCPExpectingStatus(String jsonRequestBody,
                                       String user,
                                       String password,
                                       int expectedCode)
                    throws Exception {
        final HttpRequest request = new HttpRequest(server, getMcpPath()).expectCode(expectedCode).basicAuth(user, password);
        setupAndRunRequest(request, jsonRequestBody);
    }

    /**
     * Sends an MCP request without any authentication credentials and asserts
     * that the server responds with the expected HTTP status code.
     *
     * This is mainly used to verify that unauthenticated requests are rejected
     * correctly, for example:
     *
     * - 401 Unauthorized when a tool requires authentication
     * - 403 Forbidden when a tool is explicitly denied
     */
    public void callMCPExpectingStatus(String jsonRequestBody,
                                       int expectedCode)
                    throws Exception {
        final HttpRequest request = new HttpRequest(server, getMcpPath()).expectCode(expectedCode);
        setupAndRunRequest(request, jsonRequestBody);
    }

    /**
     * Returns the list of all tools. Takes the multiple paginated responses and combines them into a single
     * tools list response.
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
                assertTrue(!cursor.isEmpty());
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

}
