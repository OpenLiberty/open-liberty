/*******************************************************************************
 * Copyright (c) 2025, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.protocol;

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;
import static io.openliberty.mcp.internal.fat.utils.TestConstants.ACCEPT;
import static io.openliberty.mcp.internal.fat.utils.TestConstants.MCP_PROTOCOL_VERSION;
import static io.openliberty.mcp.internal.fat.utils.TestConstants.VALUE_ACCEPT_DEFAULT;
import static io.openliberty.mcp.internal.fat.utils.TestConstants.VALUE_APPLICATION_JSON;
import static io.openliberty.mcp.internal.fat.utils.TestConstants.VALUE_MCP_PROTOCOL_VERSION;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeThat;

import java.util.function.Function;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
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

    @Rule
    public McpClient client = new McpClient(server, "/httpTest");

    @Test
    public void testGetRequestWithoutAcceptHeaderReturns405() throws Exception {
        HttpRequest request = new HttpRequest(server, "/httpTest/mcp")
                                                                      .method("GET")
                                                                      .expectCode(405);

        String response = request.run(String.class);

        assertNotNull("Expected response body for 405 error", response);
        assertEquals("GET requests are not supported.", response);
    }

    @Test
    public void testGetRequestWithTextEventStreamReturns405() throws Exception {
        HttpRequest request = new HttpRequest(server, ENDPOINT)
                                                               .requestProp(ACCEPT, "text/event-stream")
                                                               .method("GET")
                                                               .expectCode(405);

        String response = request.run(String.class);

        assertNotNull("Expected response body for 405 error", response);
        assertEquals("GET requests are not supported.", response);
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
                                                                   .requestProp(MCP_PROTOCOL_VERSION, VALUE_MCP_PROTOCOL_VERSION)
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

        HttpRequest JsonRequest = new HttpRequest(server, ENDPOINT)
                                                                   .requestProp(ACCEPT, VALUE_APPLICATION_JSON)
                                                                   .requestProp(MCP_PROTOCOL_VERSION, VALUE_MCP_PROTOCOL_VERSION)
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
                                                           .requestProp(ACCEPT, VALUE_ACCEPT_DEFAULT)
                                                           .requestProp(MCP_PROTOCOL_VERSION, VALUE_MCP_PROTOCOL_VERSION)
                                                           .jsonBody(request)
                                                           .method("POST")
                                                           .expectCode(400)
                                                           .run(String.class);

        assertThat("Expected 'Missing Mcp-Session-Id' in response body", response, containsString("Missing Mcp-Session-Id"));
    }

    @Test
    public void testPingWithoutSessionId() throws Exception {
        callPing(200, req -> req);
    }

    @Test
    public void testInvalidNotificationReturns202() throws Exception {
        // A JSON-RPC notification (no "id" field) with an unknown method triggers a
        // METHOD_NOT_FOUND JSONRPCException. Per the JSON-RPC spec, notifications must
        // not receive any response — the server should return 202 with no body.
        String notification = """
                        {
                          "jsonrpc": "2.0",
                          "method": "notifications/unknownMethod"
                        }
                        """;

        client.callMCPNotification(notification);
    }

    @Test
    public void testInvalidLocalhostOriginHeaderReturns403() throws Exception {
        assumeThat(server.getHostname(), equalTo("localhost")); // Test is not valid if the server is not local
        server.setMarkToEndOfLog();
        callPing(403, req -> req.requestProp("Origin", "http://evil.example.com"));
        callPing(403, req -> req.requestProp("Origin", "something odd"));
        assertNotNull(server.waitForStringInLogUsingMark("CWMCM0044I: A local MCP request was rejected due to an invalid Origin header."));
    }

    @Test
    public void testValidLocalhostOriginHeaderReturns200() throws Exception {
        assumeThat(server.getHostname(), equalTo("localhost")); // Test is not valid if the server is not local
        callPing(200, req -> req); // No extra headers
        callPing(200, req -> req.requestProp("Origin", "http://localhost"));
        callPing(200, req -> req.requestProp("Origin", "http://127.0.0.1"));
        callPing(200, req -> req.requestProp("Origin", "http://[::1]"));
    }

    void callPing(int expectedResponseCode, Function<HttpRequest, HttpRequest> requestCustomizer) throws Exception {
        String request = """
                        {
                          "jsonrpc": "2.0",
                          "id": 1,
                          "method": "ping"
                        }
                        """;

        HttpRequest httpRequest = new HttpRequest(server, ENDPOINT).requestProp(ACCEPT, VALUE_ACCEPT_DEFAULT)
                                                                   .requestProp(MCP_PROTOCOL_VERSION, VALUE_MCP_PROTOCOL_VERSION)
                                                                   .jsonBody(request)
                                                                   .method("POST")
                                                                   .expectCode(expectedResponseCode);
        httpRequest = requestCustomizer.apply(httpRequest);
        String response = httpRequest.run(String.class);

        if (expectedResponseCode == 200) {
            // Also validate ping response
            assertTrue("Expected 'result' field in ping response", response.contains("\"result\""));

            String contentType = httpRequest.getResponseHeader("Content-Type");
            assertThat(contentType, containsString(VALUE_APPLICATION_JSON));
        }
    }
}
