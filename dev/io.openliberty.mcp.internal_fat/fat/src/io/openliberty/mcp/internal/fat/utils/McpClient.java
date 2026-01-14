/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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

    private boolean sessionDeleted = false;
    private String sessionId;
    private LibertyServer server;
    private String path;
    private StateMode mode = StateMode.STATEFUL;

    public static enum StateMode {
        // STATEFUL - Uses sessions and session IDs to maintain state across requests
        STATEFUL,
        // STATELESS - Each request is independent with no session information e.g. authentication will be required for each request
        STATELESS
    }

    /**
     * @param server the {@link LibertyServer} instance used to send requests
     * @param path the base endpoint path for MCP. The full request path will be {@code path + "/mcp"}.
     */
    public McpClient(LibertyServer server, String path) {
        super();
        this.server = server;
        this.path = path;
    }

    public McpClient(LibertyServer server, String path, StateMode mode) {
        super();
        this.server = server;
        this.path = path;
        this.mode = mode;
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

        HttpRequest httpRequest = new HttpRequest(server, path + "/mcp")
                                                                        .requestProp(ACCEPT, VALUE_ACCEPT_DEFAULT)
                                                                        .requestProp(MCP_PROTOCOL_VERSION, VALUE_MCP_PROTOCOL_VERSION)
                                                                        .jsonBody(request)
                                                                        .method("POST");
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

        if (mode.equals(StateMode.STATEFUL)) {
            sessionId = httpRequest.getResponseHeader(MCP_SESSION_ID);
            assertNotNull(sessionId);
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

        callMCPNotification(server, path, notification);
    }

    @Override
    protected void after() {
        if (mode.equals(StateMode.STATEFUL)) {
            if (sessionDeleted) {
                return;
            }
            try {
                new HttpRequest(server, path + "/mcp").requestProp(MCP_SESSION_ID, sessionId)
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
                new HttpRequest(server, path + "/mcp")
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
        final HttpRequest request = new HttpRequest(server, path + "/mcp");
        return setupAndRunRequest(request, jsonRequestBody);
    }

    public String callMCPwithBasicAuth(String jsonRequestBody, String user, String password) throws Exception {
        final HttpRequest request = new HttpRequest(server, path + "/mcp").basicAuth(user, password);
        return setupAndRunRequest(request, jsonRequestBody);
    }

    public String callMCPAuthorisationErrorExpected(String jsonRequestBody) throws Exception {
        final HttpRequest request = new HttpRequest(server, path + "/mcp").expectCode(403);
        return setupAndRunRequest(request, jsonRequestBody);
    }

    public String callMCPwithBasicAuth_AuthorisationErrorExpected(String jsonRequestBody, String user, String password) throws Exception {
        final HttpRequest request = new HttpRequest(server, path + "/mcp").expectCode(403)
                                                                          .basicAuth(user, password);
        return setupAndRunRequest(request, jsonRequestBody);
    }

    /**
     * Call MCP server with a custom endpoint, and an expected response code
     */
    public String callMCPCustomized(String jsonRequestBody, String appendPath, int expectedCode) throws Exception {
        final HttpRequest request = new HttpRequest(server, path + appendPath).expectCode(expectedCode);
        return setupAndRunRequest(request, jsonRequestBody);
    }

    /**
     * Call MCP server notification endpoint, and provide a 202 expected response code. No response body is returned
     * If a response body is returned, or a response code that is not 202, and exception is thrown
     */
    public void callMCPNotification(LibertyServer server,
                                    String path,
                                    String jsonRequestBody)
                    throws Exception {

        final HttpRequest request = new HttpRequest(server, path + "/mcp").expectCode(202);
        String response = setupAndRunRequest(request, jsonRequestBody);
        assertNull("Notification request received a response", response);
    }
}
