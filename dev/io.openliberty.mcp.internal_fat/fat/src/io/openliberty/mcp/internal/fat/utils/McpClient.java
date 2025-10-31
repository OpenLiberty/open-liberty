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

    private static final String ACCEPT_HEADER = "application/json, text/event-stream";
    public static final String APPLICATION_JSON = "application/json";
    private static final String MCP_PROTOCOL_HEADER = "MCP-Protocol-Version";
    private static final String MCP_PROTOCOL_VERSION = "2025-06-18";

    private String sessionId;
    private LibertyServer server;
    private String path;

    /**
     * @param server the {@link LibertyServer} instance used to send requests
     * @param path the base endpoint path for MCP. The full request path will be {@code path + "/mcp"}.
     */
    public McpClient(LibertyServer server, String path) {
        super();
        this.server = server;
        this.path = path;
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
                            "protocolVersion": "2025-06-18",
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

        HttpRequest httpRequest = new HttpRequest(server, path + "/mcp").requestProp("Accept", ACCEPT_HEADER)
                                                                        .requestProp(MCP_PROTOCOL_HEADER, MCP_PROTOCOL_VERSION)
                                                                        .jsonBody(request)
                                                                        .method("POST");
        String response = httpRequest.run(String.class);

        String expectedResponse = """
                        {
                          "jsonrpc": "2.0",
                          "id": "1",
                          "result": {
                            "protocolVersion": "2025-06-18",
                          }
                        }
                        """;
        JSONAssert.assertEquals(expectedResponse, response, JSONCompareMode.LENIENT);

        sessionId = httpRequest.getResponseHeader("Mcp-Session-Id");
        assertNotNull(sessionId);

        String contentType = httpRequest.getResponseHeader("Content-Type");
        assertThat(contentType, containsString(McpClient.APPLICATION_JSON));

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
        try {
            new HttpRequest(server, path + "/mcp").requestProp("Mcp-Session-Id", sessionId)
                                                  .method("DELETE")
                                                  .run(String.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String getSessionId() {
        return this.sessionId;
    }

    /**
     * Call MCP server endpoint with a given JSON-RPC request body and return the response as a string.
     * The request includes required headers: Accept, MCP-Protocol-Version, and Mcp-Session-Id.
     * This method expects a successful response (200 OK) with a response body.
     */
    public String callMCP(String jsonRequestBody) throws Exception {
        return new HttpRequest(server, path + "/mcp")
                                                     .requestProp("Accept", ACCEPT_HEADER)
                                                     .requestProp(MCP_PROTOCOL_HEADER, MCP_PROTOCOL_VERSION)
                                                     .requestProp("Mcp-Session-Id", sessionId)
                                                     .jsonBody(jsonRequestBody)
                                                     .method("POST")
                                                     .run(String.class);
    }

    /**
     * Call MCP server with a custom endpoint, and an expected response code
     */
    public String callMCPCustomized(String jsonRequestBody, String appendPath, int expectedCode) throws Exception {
        return new HttpRequest(server, path + appendPath)
                                                         .requestProp("Accept", ACCEPT_HEADER)
                                                         .requestProp(MCP_PROTOCOL_HEADER, MCP_PROTOCOL_VERSION)
                                                         .requestProp("Mcp-Session-Id", sessionId)
                                                         .jsonBody(jsonRequestBody)
                                                         .method("POST")
                                                         .expectCode(expectedCode)
                                                         .run(String.class);
    }

    /**
     * Call MCP server notification endpoint, and provide a 202 expected response code. No response body is returned
     * If a response body is returned, or a response code that is not 202, and exception is thrown
     */
    public void callMCPNotification(LibertyServer server,
                                    String path,
                                    String jsonRequestBody)
                    throws Exception {

        String response = new HttpRequest(server, path + "/mcp")
                                                                .requestProp("Accept", ACCEPT_HEADER)
                                                                .requestProp(MCP_PROTOCOL_HEADER, MCP_PROTOCOL_VERSION)
                                                                .requestProp("Mcp-Session-Id", sessionId)
                                                                .jsonBody(jsonRequestBody)
                                                                .method("POST")
                                                                .expectCode(202)
                                                                .run(String.class);

        assertNull("Notification request received a response", response);
    }

}
