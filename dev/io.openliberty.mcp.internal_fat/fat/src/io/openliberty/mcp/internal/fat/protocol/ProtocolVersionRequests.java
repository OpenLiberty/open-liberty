/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.protocol;

import static io.openliberty.mcp.internal.fat.utils.TestConstants.ACCEPT;
import static io.openliberty.mcp.internal.fat.utils.TestConstants.MCP_PROTOCOL_VERSION;
import static io.openliberty.mcp.internal.fat.utils.TestConstants.MCP_SESSION_ID;
import static io.openliberty.mcp.internal.fat.utils.TestConstants.VALUE_ACCEPT_DEFAULT;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;

import org.json.JSONObject;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpRequest;

/**
 * Helpers for tests which send requests using a chosen {@code MCP-Protocol-Version} header to an
 * MCP application.
 * <p>
 * {@link io.openliberty.mcp.internal.fat.utils.McpClient} can override the header on a POST with
 * {@code callMCPRaw(...).protocolVersion(...)}, but it cannot omit the header, send a DELETE with a chosen
 * version, read the {@code Mcp-Session-Id} response header, or initialize with a version other than 2025-11-25.
 * These helpers use {@link HttpRequest} directly to cover those cases.
 * <p>
 * Stateless applications need no session. For stateful applications use {@link #initialize} to obtain a session
 * and the {@code ...InSession} methods to send requests in it.
 */
public final class ProtocolVersionRequests {

    public static final String V_2025_03_26 = "2025-03-26";
    public static final String V_2025_06_18 = "2025-06-18";
    public static final String V_2025_11_25 = "2025-11-25";
    public static final String V_2026_07_28 = "2026-07-28";

    /** Start of the message sent to the client when the protocol version is not accepted */
    public static final String UNSUPPORTED_MESSAGE = "An unsupported MCP-Protocol-Version header was provided.";

    private static final String ECHO_REQUEST = """
                    {
                      "jsonrpc": "2.0",
                      "id": 1,
                      "method": "tools/call",
                      "params": {
                        "name": "echo",
                        "arguments": {
                          "input": "Hello"
                        }
                      }
                    }
                    """;

    private static final String INITIALIZE_REQUEST = """
                    {
                      "jsonrpc": "2.0",
                      "id": 1,
                      "method": "initialize",
                      "params": {
                        "protocolVersion": "%s",
                        "clientInfo": {
                          "name": "test-client",
                          "version": "0.1"
                        },
                        "capabilities": {}
                      }
                    }
                    """;

    /**
     * The interesting parts of a successful initialize response.
     *
     * @param protocolVersion the protocolVersion in the result
     * @param sessionId the Mcp-Session-Id response header, or {@code null} if the application is stateless
     */
    public record InitializeResponse(String protocolVersion, String sessionId) {}

    private ProtocolVersionRequests() {
        // utility class
    }

    /**
     * Sends a tools/call request for the echo tool and expects it to be accepted.
     *
     * @param protocolVersion the value for the MCP-Protocol-Version header, or {@code null} to omit the header
     */
    public static void assertEchoAccepted(LibertyServer server, String appName, String protocolVersion) throws Exception {
        assertEchoAcceptedInSession(server, appName, protocolVersion, null);
    }

    /**
     * Sends a tools/call request for the echo tool in a session and expects it to be accepted.
     *
     * @param protocolVersion the value for the MCP-Protocol-Version header, or {@code null} to omit the header
     * @param sessionId the value for the Mcp-Session-Id header, or {@code null} to omit the header
     */
    public static void assertEchoAcceptedInSession(LibertyServer server, String appName, String protocolVersion, String sessionId) throws Exception {
        String response = post(server, appName, protocolVersion, sessionId, ECHO_REQUEST, 200);
        assertThat("Expected the echo tool to run with protocol version " + protocolVersion,
                   response, containsString("Hello"));
    }

    /**
     * Sends a tools/call request for the echo tool and expects it to be rejected with HTTP 400.
     *
     * @param protocolVersion the value for the MCP-Protocol-Version header, or {@code null} to omit the header
     * @param expectedSupportedValues the exact list the server should report as supported, e.g. "2025-06-18"
     */
    public static void assertEchoRejected(LibertyServer server, String appName, String protocolVersion, String expectedSupportedValues) throws Exception {
        assertEchoRejectedInSession(server, appName, protocolVersion, null, expectedSupportedValues);
    }

    /**
     * Sends a tools/call request for the echo tool in a session and expects it to be rejected with HTTP 400.
     *
     * @param protocolVersion the value for the MCP-Protocol-Version header, or {@code null} to omit the header
     * @param sessionId the value for the Mcp-Session-Id header, or {@code null} to omit the header
     * @param expectedSupportedValues the exact list the server should report as supported, e.g. "2025-06-18"
     */
    public static void assertEchoRejectedInSession(LibertyServer server, String appName, String protocolVersion, String sessionId,
                                                   String expectedSupportedValues)
                    throws Exception {
        String response = post(server, appName, protocolVersion, sessionId, ECHO_REQUEST, 400);
        assertRejectedResponse(protocolVersion, response, expectedSupportedValues);
    }

    /**
     * Sends an initialize request with no MCP-Protocol-Version header (as allowed for initialize) and returns the
     * protocol version the server selected.
     *
     * @param clientProtocolVersion the protocol version requested by the client in the initialize params
     * @return the protocolVersion in the result
     */
    public static String initializeAndGetProtocolVersion(LibertyServer server, String appName, String clientProtocolVersion) throws Exception {
        return initialize(server, appName, null, clientProtocolVersion).protocolVersion();
    }

    /**
     * Sends an initialize request and expects it to be accepted.
     *
     * @param headerVersion the value for the MCP-Protocol-Version header, or {@code null} to omit the header
     * @param clientProtocolVersion the protocol version requested by the client in the initialize params
     * @return the protocol version selected by the server and the session ID, if there is one
     */
    public static InitializeResponse initialize(LibertyServer server, String appName, String headerVersion, String clientProtocolVersion) throws Exception {
        HttpRequest request = newRequest(server, appName, headerVersion, null, String.format(INITIALIZE_REQUEST, clientProtocolVersion), 200);
        String response = request.run(String.class);
        String selectedVersion = new JSONObject(response).getJSONObject("result").getString("protocolVersion");
        return new InitializeResponse(selectedVersion, request.getResponseHeader(MCP_SESSION_ID));
    }

    /**
     * Sends an initialize request and expects it to be rejected with HTTP 400 because of its MCP-Protocol-Version header.
     *
     * @param headerVersion the value for the MCP-Protocol-Version header
     * @param clientProtocolVersion the protocol version requested by the client in the initialize params
     * @param expectedSupportedValues the exact list the server should report as supported, e.g. "2025-06-18"
     */
    public static void assertInitializeRejected(LibertyServer server, String appName, String headerVersion, String clientProtocolVersion,
                                                String expectedSupportedValues)
                    throws Exception {
        String response = newRequest(server, appName, headerVersion, null, String.format(INITIALIZE_REQUEST, clientProtocolVersion), 400).run(String.class);
        assertRejectedResponse(headerVersion, response, expectedSupportedValues);
    }

    /**
     * Sends a DELETE request for a session and expects the given HTTP status.
     *
     * @param protocolVersion the value for the MCP-Protocol-Version header, or {@code null} to omit the header
     * @param sessionId the value for the Mcp-Session-Id header, or {@code null} to omit the header
     * @return the response body
     */
    public static String delete(LibertyServer server, String appName, String protocolVersion, String sessionId, int expectedCode) throws Exception {
        HttpRequest request = new HttpRequest(server, "/" + appName + "/mcp")
                                                                             .requestProp(ACCEPT, VALUE_ACCEPT_DEFAULT)
                                                                             .method("DELETE")
                                                                             .expectCode(expectedCode);
        if (protocolVersion != null) {
            request = request.requestProp(MCP_PROTOCOL_VERSION, protocolVersion);
        }
        if (sessionId != null) {
            request = request.requestProp(MCP_SESSION_ID, sessionId);
        }
        return request.run(String.class);
    }

    /**
     * Sends a DELETE request for a session and expects it to be rejected with HTTP 400 because of its
     * MCP-Protocol-Version header.
     *
     * @param expectedSupportedValues the exact list the server should report as supported, e.g. "2025-06-18"
     */
    public static void assertDeleteRejected(LibertyServer server, String appName, String protocolVersion, String sessionId,
                                            String expectedSupportedValues)
                    throws Exception {
        assertRejectedResponse(protocolVersion, delete(server, appName, protocolVersion, sessionId, 400), expectedSupportedValues);
    }

    private static void assertRejectedResponse(String protocolVersion, String response, String expectedSupportedValues) {
        assertThat("Expected error message about unsupported protocol version " + protocolVersion,
                   response, containsString(UNSUPPORTED_MESSAGE));
        assertThat("Expected error message to list the supported versions",
                   response, containsString("Supported values: " + expectedSupportedValues));
    }

    private static String post(LibertyServer server, String appName, String protocolVersion, String sessionId, String body, int expectedCode) throws Exception {
        return newRequest(server, appName, protocolVersion, sessionId, body, expectedCode).run(String.class);
    }

    private static HttpRequest newRequest(LibertyServer server, String appName, String protocolVersion, String sessionId, String body,
                                          int expectedCode)
                    throws Exception {
        HttpRequest request = new HttpRequest(server, "/" + appName + "/mcp")
                                                                             .requestProp(ACCEPT, VALUE_ACCEPT_DEFAULT)
                                                                             .jsonBody(body)
                                                                             .method("POST")
                                                                             .expectCode(expectedCode);
        if (protocolVersion != null) {
            request = request.requestProp(MCP_PROTOCOL_VERSION, protocolVersion);
        }
        if (sessionId != null) {
            request = request.requestProp(MCP_SESSION_ID, sessionId);
        }
        return request;
    }
}