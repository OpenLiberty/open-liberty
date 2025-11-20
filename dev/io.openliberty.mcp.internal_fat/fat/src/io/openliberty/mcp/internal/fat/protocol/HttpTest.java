/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.protocol;

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpRequest;
import io.openliberty.mcp.internal.fat.tool.basicToolApp.BasicTools;
import io.openliberty.mcp.internal.fat.utils.McpClient;

/**
 *
 */
@RunWith(FATRunner.class)
public class HttpTest {

    private static final String ACCEPT_HEADER = "application/json, text/event-stream";
    private static final String MCP_PROTOCOL_HEADER = "MCP-Protocol-Version";
    private static final String MCP_PROTOCOL_VERSION = "2025-06-18";

    @Server("mcp-server")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "httpTest.war").addPackage(BasicTools.class.getPackage());

        ShrinkHelper.exportDropinAppToServer(server, war, SERVER_ONLY);

        server.startServer();
    }

    @AfterClass
    public static void teardown() throws Exception {
        server.stopServer();
    }

    private static final String ENDPOINT = "/httpTest/mcp";

    @Test
    public void testGetRequestWithoutAcceptHeaderReturns405() throws Exception {
        HttpRequest request = new HttpRequest(server, "/httpTest/mcp")
                                                                      .method("GET")
                                                                      .expectCode(405);

        String response = request.run(String.class);

        assertNotNull("Expected response body for 405 error", response);
        assertEquals("CWMCM0009I: GET requests are not supported.", response);
    }

    @Test
    public void testGetRequestWithTextEventStreamReturns405() throws Exception {
        HttpRequest request = new HttpRequest(server, ENDPOINT)
                                                               .requestProp("Accept", "text/event-stream")
                                                               .method("GET")
                                                               .expectCode(405);

        String response = request.run(String.class);

        assertNotNull("Expected response body for 405 error", response);
        assertEquals("CWMCM0009I: GET requests are not supported.", response);
    }

    @Test
    public void testMissingAcceptHeader() throws Exception {
        String request = """
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

        HttpRequest JsonRequest = new HttpRequest(server, ENDPOINT)
                                                                   .requestProp(MCP_PROTOCOL_VERSION, MCP_PROTOCOL_HEADER)
                                                                   .jsonBody(request)
                                                                   .method("POST")
                                                                   .expectCode(406);

        String response = JsonRequest.run(String.class);
        assertNull("Expected no response body for 406 Not Acceptable", response);
    }

    @Test
    public void testIncorrectAcceptHeader() throws Exception {
        String request = """
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

        HttpRequest JsonRequest = new HttpRequest(server, ENDPOINT).requestProp("Accept", "application/json")
                                                                   .requestProp(MCP_PROTOCOL_VERSION, MCP_PROTOCOL_HEADER)
                                                                   .jsonBody(request).method("POST").expectCode(406);

        String response = JsonRequest.run(String.class);
        assertNull("Expected no response body for 406 Not Acceptable due to incorrect Accept header", response);
    }

    @Test
    public void testCallWithoutSessionId() throws Exception {
        String request = """
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

        String response = new HttpRequest(server, ENDPOINT)
                                                           .requestProp("Accept", ACCEPT_HEADER)
                                                           .requestProp(MCP_PROTOCOL_HEADER, MCP_PROTOCOL_VERSION)
                                                           .jsonBody(request)
                                                           .method("POST")
                                                           .expectCode(400)
                                                           .run(String.class);

        assertThat("Expected 'Missing Mcp-Session-Id' in response body", response, containsString("Missing Mcp-Session-Id"));
    }

    @Test
    public void testPingWithoutSessionId() throws Exception {
        String request = """
                        {
                          "jsonrpc": "2.0",
                          "id": 1,
                          "method": "ping"
                        }
                        """;

        HttpRequest httpRequest = new HttpRequest(server, ENDPOINT)
                                                                   .requestProp("Accept", ACCEPT_HEADER)
                                                                   .requestProp(MCP_PROTOCOL_HEADER, MCP_PROTOCOL_VERSION)
                                                                   .jsonBody(request)
                                                                   .method("POST")
                                                                   .expectCode(200);
        String response = httpRequest.run(String.class);

        assertTrue("Expected 'result' field in ping response", response.contains("\"result\""));

        String contentType = httpRequest.getResponseHeader("Content-Type");
        assertThat(contentType, containsString(McpClient.APPLICATION_JSON));
    }
}