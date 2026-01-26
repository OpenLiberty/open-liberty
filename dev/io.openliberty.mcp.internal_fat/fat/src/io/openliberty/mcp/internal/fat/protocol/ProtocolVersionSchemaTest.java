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

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;
import static io.openliberty.mcp.internal.fat.utils.TestConstants.ACCEPT;
import static io.openliberty.mcp.internal.fat.utils.TestConstants.MCP_PROTOCOL_VERSION;
import static io.openliberty.mcp.internal.fat.utils.TestConstants.MCP_SESSION_ID;
import static io.openliberty.mcp.internal.fat.utils.TestConstants.VALUE_ACCEPT_DEFAULT;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.json.JSONObject;
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
import componenttest.topology.utils.HttpRequest;
import io.openliberty.mcp.internal.fat.tool.protocolVersionApp.ProtocolVersionTestTools;
import io.openliberty.mcp.internal.fat.utils.McpClient;

@RunWith(FATRunner.class)
public class ProtocolVersionSchemaTest {

    @Rule
    public McpClient client = new McpClient(server, "/protocolVersionSchemaTest");

    @Server("mcp-server")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "protocolVersionSchemaTest.war").addPackage(ProtocolVersionTestTools.class.getPackage());

        ShrinkHelper.exportDropinAppToServer(server, war, SERVER_ONLY);

        server.startServer();
    }

    @AfterClass
    public static void teardown() throws Exception {
        server.stopServer();
    }

    @Test
    public void testOlderProtocolVersionsExcludeOutputSchema() throws Exception {
        String request = """
                        {
                          "jsonrpc": "2.0",
                          "id": "2",
                          "method": "tools/call",
                          "params": {
                            "name": "testListObjectResponse",
                            "arguments": {}
                          }
                        }
                        """;

        String response = new HttpRequest(server, "/protocolVersionSchemaTest/mcp").requestProp(ACCEPT, VALUE_ACCEPT_DEFAULT)
                                                                                   .requestProp(MCP_PROTOCOL_VERSION, "2025-03-26")
                                                                                   .requestProp(MCP_SESSION_ID, client.getSessionId())
                                                                                   .jsonBody(request)
                                                                                   .method("POST")
                                                                                   .expectCode(200)
                                                                                   .run(String.class);

        String expectedResponseString = """
                        {
                          "id":"2",
                          "jsonrpc":"2.0",
                          "result": {
                            "content": [
                              {
                                "type":"text",
                                "text":"{\\\"country\\\":\\\"France\\\",\\\"isCapital\\\":true,\\\"name\\\":\\\"Paris\\\",\\\"population\\\":8000}"
                              }
                            ],
                            "isError": false
                          }
                        }
                        """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Test
    public void testNewerProtocolVersionsIncludeOutputSchema() throws Exception {
        String request = """
                        {
                          "jsonrpc": "2.0",
                          "id": "2",
                          "method": "tools/call",
                          "params": {
                            "name": "testListObjectResponse",
                            "arguments": {}
                          }
                        }
                        """;

        String response = new HttpRequest(server, "/protocolVersionSchemaTest/mcp").requestProp(ACCEPT, VALUE_ACCEPT_DEFAULT)
                                                                                   .requestProp(MCP_PROTOCOL_VERSION, "2025-11-25")
                                                                                   .requestProp(MCP_SESSION_ID, client.getSessionId())
                                                                                   .jsonBody(request)
                                                                                   .method("POST")
                                                                                   .expectCode(200)
                                                                                   .run(String.class);

        String expectedResponseString = """
                        {
                          "id":"2",
                          "jsonrpc":"2.0",
                          "result": {
                            "content": [
                              {
                                "type":"text",
                                "text":"{\\\"country\\\":\\\"France\\\",\\\"isCapital\\\":true,\\\"name\\\":\\\"Paris\\\",\\\"population\\\":8000}"
                              }
                            ],
                            "structuredContent":
                              {
                                "country": "France",
                                "isCapital": true,
                                "name": "Paris",
                                "population": 8000
                              },
                            "isError": false
                          }
                        }
                        """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Test
    public void testNewerProtocolVersionIncludeOutputSchemaInToolDescription() throws Exception {
        String request = """
                        {
                          "jsonrpc": "2.0",
                          "id": 1,
                          "method": "tools/list"
                        }
                        """;

        String response = new HttpRequest(server, "/protocolVersionSchemaTest/mcp").requestProp(ACCEPT, VALUE_ACCEPT_DEFAULT)
                                                                                   .requestProp(MCP_PROTOCOL_VERSION, "2025-11-25")
                                                                                   .requestProp(MCP_SESSION_ID, client.getSessionId())
                                                                                   .jsonBody(request)
                                                                                   .method("POST")
                                                                                   .expectCode(200)
                                                                                   .run(String.class);

        JSONObject jsonResponse = new JSONObject(response);
        String expectedString = """
                        {
                            "id": 1,
                            "jsonrpc": "2.0",
                            "result": {
                                "tools": [
                                    {
                                        "description": "A tool to return a list of cities",
                                        "inputSchema": {
                                            "type": "object",
                                            "properties": {},
                                            "required": []
                                        },
                                        "name": "testListObjectResponse",
                                        "outputSchema": {
                                            "type": "object",
                                            "properties": {
                                                "name": {
                                                    "type": "string"
                                                },
                                                "country": {
                                                    "type": "string"
                                                },
                                                "population": {
                                                    "type": "integer"
                                                },
                                                "isCapital": {
                                                    "type": "boolean"
                                                }
                                            },
                                            "required": [
                                                "name",
                                                "country",
                                                "population",
                                                "isCapital"
                                            ]
                                        },
                                        "title": "City List"
                                    },
                                    {
                                        "inputSchema": {
                                            "type": "object",
                                            "properties": {},
                                            "required": []
                                        },
                                        "name": "testToolResponseNoContent"
                                    }
                                ]
                            }
                        }
                        """;
        JSONAssert.assertEquals(expectedString, jsonResponse.toString(), JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    public void testOlderProtocolVersionExcludeOutputSchemaInToolDescription() throws Exception {
        String request = """
                        {
                          "jsonrpc": "2.0",
                          "id": 1,
                          "method": "tools/list"
                        }
                        """;

        String response = new HttpRequest(server, "/protocolVersionSchemaTest/mcp").requestProp(ACCEPT, VALUE_ACCEPT_DEFAULT)
                                                                                   .requestProp(MCP_PROTOCOL_VERSION, "2025-03-26")
                                                                                   .requestProp(MCP_SESSION_ID, client.getSessionId())
                                                                                   .jsonBody(request)
                                                                                   .method("POST")
                                                                                   .expectCode(200)
                                                                                   .run(String.class);

        JSONObject jsonResponse = new JSONObject(response);
        String expectedString = """
                         {
                          "id": 1,
                          "jsonrpc": "2.0",
                          "result": {
                            "tools": [
                              {
                                "description": "A tool to return a list of cities",
                                "inputSchema": {
                                  "type": "object",
                                  "properties": {},
                                  "required": []
                                },
                                "name": "testListObjectResponse",
                                "title": "City List"
                              },
                              {
                                "inputSchema": {
                                  "type": "object",
                                  "properties": {},
                                  "required": []
                                },
                                "name": "testToolResponseNoContent"
                              }
                            ]
                          }
                        }
                        """;
        JSONAssert.assertEquals(expectedString, jsonResponse.toString(), JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    public void testOlderProtocolVersionWithStructuredContentButNoTextContent() throws Exception {
        String request = """
                        {
                          "jsonrpc": "2.0",
                          "id": "2",
                          "method": "tools/call",
                          "params": {
                            "name": "testToolResponseNoContent",
                            "arguments": {}
                          }
                        }
                        """;

        String response = new HttpRequest(server, "/protocolVersionSchemaTest/mcp").requestProp(ACCEPT, VALUE_ACCEPT_DEFAULT)
                                                                                   .requestProp(MCP_PROTOCOL_VERSION, "2025-03-26")
                                                                                   .requestProp(MCP_SESSION_ID, client.getSessionId())
                                                                                   .jsonBody(request)
                                                                                   .method("POST")
                                                                                   .expectCode(200)
                                                                                   .run(String.class);

        JSONObject jsonResponse = new JSONObject(response);
        String expectedString = """
                         {
                          "id":"2",
                          "jsonrpc":"2.0",
                          "result": {
                            "content": [
                              {
                                "type":"text",
                                "text":"{\\"content\\":\\"Hello World\\"}"
                              }
                            ],
                            "isError": false
                          }
                        }
                        """;
        JSONAssert.assertEquals(expectedString, jsonResponse.toString(), JSONCompareMode.NON_EXTENSIBLE);
    }

}
