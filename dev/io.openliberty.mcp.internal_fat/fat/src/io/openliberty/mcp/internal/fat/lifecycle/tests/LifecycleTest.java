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
     * Verifies that an {@code initialize} request whose body omits {@code protocolVersion}
     * is tolerated: the server falls back to its preferred version ({@code 2025-11-25}) and
     * returns a normal, successful initialize result rather than an error.
     */
    @Test
    public void testInitializeWithMissingProtocolVersionFallsBackToServerDefault() throws Exception {
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
        // Missing protocolVersion in the body is tolerated - the server falls back to
        // its preferred version (2025-11-25) and returns a normal initialize result
        String expectedResponse = """
                        {
                          "jsonrpc": "2.0",
                          "id": "neg-1",
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

    /**
     * Verifies that a JSON-RPC request for an unrecognised method returns an error response
     * with code {@code -32601} (Method not found), the standard message, and the method name
     * in the {@code data} field.
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
                            "code": -32601,
                            "message": "Method not found",
                            "data": ["nonexistent/method not found"]
                          }
                        }
                        """;
        JSONAssert.assertEquals(expectedError, response, JSONCompareMode.STRICT);
    }

    /**
     * Verifies that a {@code ping} request with unrecognised fields in {@code params}
     * still succeeds — unknown parameters must be silently ignored.
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
