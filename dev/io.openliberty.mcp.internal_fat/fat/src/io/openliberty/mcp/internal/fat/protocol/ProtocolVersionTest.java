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
import static io.openliberty.mcp.internal.fat.utils.TestConstants.MCP_SESSION_ID;
import static io.openliberty.mcp.internal.fat.utils.TestConstants.VALUE_ACCEPT_DEFAULT;
import static io.openliberty.mcp.internal.fat.utils.TestConstants.VALUE_APPLICATION_JSON;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.JSONAssert;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpRequest;
import io.openliberty.mcp.internal.fat.tool.basicToolApp.BasicTools;
import io.openliberty.mcp.internal.fat.utils.McpClient;
import io.openliberty.mcp.internal.fat.utils.TestConstants;

/**
 *
 */
@RunWith(FATRunner.class)
public class ProtocolVersionTest {

    @Rule
    public McpClient client = new McpClient(server, "/protocolVersionTest");

    @Server("mcp-server")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "protocolVersionTest.war").addPackage(BasicTools.class.getPackage());

        ShrinkHelper.exportDropinAppToServer(server, war, SERVER_ONLY);

        server.startServer();
    }

    @AfterClass
    public static void teardown() throws Exception {
        server.stopServer();
    }

    @Test
    public void testMissingMcpProtocolVersionHeader() throws Exception {
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

        HttpRequest httpRequest = new HttpRequest(server, "/protocolVersionTest/mcp")
                                                                                     .requestProp(ACCEPT, VALUE_ACCEPT_DEFAULT)
                                                                                     .requestProp(MCP_SESSION_ID, client.getSessionId())
                                                                                     .jsonBody(request)
                                                                                     .method("POST")
                                                                                     .expectCode(200);
        String response = httpRequest.run(String.class);

        String expectedResponse = """
                        {
                          "id": 1,
                          "jsonrpc": "2.0",
                          "result": {
                            "content": [
                              {
                                "type": "text",
                                "text": "Hello"
                              }
                            ],
                            "isError": false
                          }
                        }""";

        JSONAssert.assertEquals(expectedResponse, response, true);

        String contentType = httpRequest.getResponseHeader("Content-Type");
        assertThat(contentType, containsString(VALUE_APPLICATION_JSON));
    }

    @Test
    public void testInitializeDoesNotNeedMcpProtocolVersionHeader() throws Exception {
        String request = """
                        {
                          "jsonrpc": "2.0",
                          "id": 1,
                          "method": "initialize",
                         "params": {
                            "protocolVersion": "2025-11-25",
                            "clientInfo": {
                              "name": "test-client",
                              "version": "0.1"
                            },
                            "rootUri": "file:/test/root"
                          }
                        }
                        """;

        HttpRequest httpRequest = new HttpRequest(server, "/protocolVersionTest/mcp")
                                                                                     .requestProp(ACCEPT, VALUE_ACCEPT_DEFAULT)
                                                                                     .jsonBody(request)
                                                                                     .method("POST")
                                                                                     .expectCode(200);
        String response = httpRequest.run(String.class);

        assertTrue("Expected response to contain result", response.contains("\"result\""));
        assertTrue("Expected protocolVersion field in response", response.contains("\"protocolVersion\""));
        assertTrue("Expected serverInfo field in response", response.contains("\"serverInfo\""));

        String contentType = httpRequest.getResponseHeader("Content-Type");
        assertThat(contentType, containsString(TestConstants.VALUE_APPLICATION_JSON));
    }

    @Test
    public void testRejectsUnsupportedProtocolVersion() throws Exception {
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

        String response = new HttpRequest(server, "/protocolVersionTest/mcp")
                                                                             .requestProp(ACCEPT, VALUE_ACCEPT_DEFAULT)
                                                                             .requestProp(MCP_PROTOCOL_VERSION, "2022-02-02")
                                                                             .requestProp(MCP_SESSION_ID, client.getSessionId())
                                                                             .jsonBody(request)
                                                                             .method("POST")
                                                                             .expectCode(400)
                                                                             .run(String.class);

        assertThat("Expected error message about invalid protocol version",
                   response, containsString("An unsupported MCP-Protocol-Version header was provided."));
        assertThat("Expected error message to contain expected version",
                   response, containsString("Supported values: 2025-11-25, 2025-06-18, 2025-03-26"));
    }

}
