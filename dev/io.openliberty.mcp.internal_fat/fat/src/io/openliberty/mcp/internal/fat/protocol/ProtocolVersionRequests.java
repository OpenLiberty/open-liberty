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
import static io.openliberty.mcp.internal.fat.utils.TestConstants.VALUE_ACCEPT_DEFAULT;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;

import org.json.JSONObject;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpRequest;

/**
 * Helpers for tests which send requests using a chosen {@code MCP-Protocol-Version} header to a
 * stateless MCP application.
 * <p>
 * {@link io.openliberty.mcp.internal.fat.utils.McpClient} always sends the 2025-11-25 header, so it cannot be
 * used to test a protocol version range which excludes that version. These helpers use {@link HttpRequest}
 * directly, like {@link ProtocolVersionTest}. The applications must be stateless so no session is needed.
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

    private ProtocolVersionRequests() {
        // utility class
    }

    /**
     * Sends a tools/call request for the echo tool and expects it to be accepted.
     *
     * @param protocolVersion the value for the MCP-Protocol-Version header, or {@code null} to omit the header
     */
    public static void assertEchoAccepted(LibertyServer server, String appName, String protocolVersion) throws Exception {
        String response = post(server, appName, protocolVersion, ECHO_REQUEST, 200);
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
        String response = post(server, appName, protocolVersion, ECHO_REQUEST, 400);
        assertThat("Expected error message about unsupported protocol version " + protocolVersion,
                   response, containsString(UNSUPPORTED_MESSAGE));
        assertThat("Expected error message to list the supported versions",
                   response, containsString("Supported values: " + expectedSupportedValues));
    }

    /**
     * Sends an initialize request with no MCP-Protocol-Version header (as allowed for initialize) and returns the
     * protocol version the server selected.
     *
     * @param clientProtocolVersion the protocol version requested by the client in the initialize params
     * @return the protocolVersion in the result
     */
    public static String initializeAndGetProtocolVersion(LibertyServer server, String appName, String clientProtocolVersion) throws Exception {
        String response = post(server, appName, null, String.format(INITIALIZE_REQUEST, clientProtocolVersion), 200);
        return new JSONObject(response).getJSONObject("result").getString("protocolVersion");
    }

    private static String post(LibertyServer server, String appName, String protocolVersion, String body, int expectedCode) throws Exception {
        HttpRequest request = new HttpRequest(server, "/" + appName + "/mcp")
                                                                              .requestProp(ACCEPT, VALUE_ACCEPT_DEFAULT)
                                                                              .jsonBody(body)
                                                                              .method("POST")
                                                                              .expectCode(expectedCode);
        if (protocolVersion != null) {
            request = request.requestProp(MCP_PROTOCOL_VERSION, protocolVersion);
        }
        return request.run(String.class);
    }
}
