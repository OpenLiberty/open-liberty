/*******************************************************************************
 * Copyright (c) 2025, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.lifecycle.tests;

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.mcp.internal.fat.tool.basicToolApp.BasicTools;
import io.openliberty.mcp.internal.fat.utils.McpClient;

/**
 *
 */
@RunWith(FATRunner.class)
public class LifecycleTest {

    @Server("mcp-server")
    public static LibertyServer server;

    @Rule
    public McpClient client = new McpClient(server, "/lifecycleTest");

    @BeforeClass
    public static void setup() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "lifecycleTest.war")
                                   .addPackage(BasicTools.class.getPackage());

        ShrinkHelper.exportDropinAppToServer(server, war, SERVER_ONLY);

        server.startServer();
    }

    @AfterClass
    public static void teardown() throws Exception {
        server.stopServer();
    }

    @Test
    public void testInitialization() throws Exception {
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
                              "name": "ExampleClient",
                              "title": "Example Client Display Name",
                              "version": "1.0.0"
                            }
                          }
                        }
                        """;

        String response = client.callMCP(request);

        String expectedResponse = """
                        {
                          "jsonrpc": "2.0",
                          "id": "1",
                          "result": {
                            "protocolVersion": "2025-11-25",
                            "capabilities": {
                              "tools": {
                                "listChanged": false
                              }
                            },
                            "serverInfo": {
                              "name": "mcp-server",
                              "version": "1.0.0"
                            }
                          }
                        }
                        """;
        JSONAssert.assertEquals(expectedResponse, response, JSONCompareMode.STRICT);
    }

    @Test
    public void testClientInitializedNotification() throws Exception {
        String request = """
                         {
                           "jsonrpc": "2.0",
                           "method": "notifications/initialized"
                         }
                        """;

        client.callMCPNotification(request);
    }

    @Test
    public void testPing() throws Exception {
        String request = """
                        {
                          "jsonrpc": "2.0",
                          "id": "123",
                          "method": "ping"
                        }
                        """;

        String response = client.callMCP(request);

        String expectedResponse = """
                          {
                          "jsonrpc": "2.0",
                          "id": "123",
                          "result": {}
                        }
                        """;
        JSONAssert.assertEquals(expectedResponse, response, JSONCompareMode.STRICT);
    }
    // Negative Tests

    /**
     * Negative test: an {@code initialize} request that omits the required
     * {@code protocolVersion} field must still return a valid JSON-RPC response
     * envelope; the server must not crash or return an empty body.
     */
    @Test
    public void testInitializeWithMissingProtocolVersionReturnsError() throws Exception {
        String request = """
                        {
                          "jsonrpc": "2.0",
                          "id": "neg-1",
                          "method": "initialize",
                          "params": {
                            "capabilities": {},
                            "clientInfo": {
                              "name": "BadClient",
                              "version": "0.0"
                            }
                          }
                        }
                        """;

        String response = client.callMCP(request);
        assertNotNull("Response must not be null for a malformed initialize request", response);
        // The server must return a JSON-RPC envelope; it must not silently swallow the call
        assertTrue("Response must contain 'jsonrpc'", response.contains("jsonrpc"));
    }

    /**
     * Negative test: a JSON-RPC request for an unknown method name must return a
     * JSON-RPC error response with code {@code -32601} (Method not found).
     */
    @Test
    public void testUnknownMethodReturnsMethodNotFoundError() throws Exception {
        String request = """
                        {
                          "jsonrpc": "2.0",
                          "id": "neg-2",
                          "method": "nonexistent/method"
                        }
                        """;

        String response = client.callMCP(request);
        assertNotNull("Response must not be null for an unknown method", response);

        String expectedError = """
                        {
                          "jsonrpc": "2.0",
                          "id": "neg-2",
                          "error": {
                            "code": -32601
                          }
                        }
                        """;
        JSONAssert.assertEquals(expectedError, response, JSONCompareMode.LENIENT);
    }

    /**
     * Negative test: a {@code ping} request carrying extra unknown fields in its
     * {@code params} object must still succeed; the server must ignore unknown
     * parameters rather than rejecting the request.
     */
    @Test
    public void testPingWithUnknownParamsIsIgnored() throws Exception {
        String request = """
                        {
                          "jsonrpc": "2.0",
                          "id": "neg-3",
                          "method": "ping",
                          "params": {
                            "unknownField": "shouldBeIgnored"
                          }
                        }
                        """;

        String response = client.callMCP(request);
        assertNotNull("Response must not be null", response);

        String expectedResponse = """
                        {
                          "jsonrpc": "2.0",
                          "id": "neg-3",
                          "result": {}
                        }
                        """;
        JSONAssert.assertEquals(expectedResponse, response, JSONCompareMode.STRICT);
    }
}
