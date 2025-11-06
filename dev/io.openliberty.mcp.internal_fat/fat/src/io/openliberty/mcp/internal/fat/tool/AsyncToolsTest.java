/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.tool;

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

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.mcp.internal.fat.tool.asyncToolApp.AsyncTools;
import io.openliberty.mcp.internal.fat.utils.AwaitToolServlet;
import io.openliberty.mcp.internal.fat.utils.McpClient;

@RunWith(FATRunner.class)
public class AsyncToolsTest extends FATServletClient {
    private static final String EXPECTED_ERROR = "Method call caused runtime exception. This is expected if the input was 'throw error'";

    @Server("mcp-server-async")
    public static LibertyServer server;

    @Rule
    public McpClient client = new McpClient(server, "/asyncToolsTest");

    @BeforeClass
    public static void setup() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "asyncToolsTest.war")
                                   .addPackage(AsyncTools.class.getPackage())
                                   .addPackage(AwaitToolServlet.class.getPackage());

        ShrinkHelper.exportDropinAppToServer(server, war, SERVER_ONLY);

        server.startServer();
        assertNotNull(server.waitForStringInLog("MCP server endpoint: .*/mcp$"));
    }

    @AfterClass
    public static void teardown() throws Exception {
        server.stopServer(EXPECTED_ERROR);
    }

    @Test
    public void testCompletionStageThatCompletesImmediately() throws Exception {
        String request = """
                          {
                            "jsonrpc": "2.0",
                            "id": "3",
                            "method": "tools/call",
                            "params": {
                              "name": "asyncEcho",
                              "arguments": {
                                "input": "Async Hello"
                              }
                            }
                          }
                        """;

        String response = client.callMCP(request);

        String expectedResponseString = """
                        {"id":"3","jsonrpc":"2.0","result":{"content":[{"type":"text","text":"Async Hello: (async)"}],"isError":false}}
                        """;

        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Test
    public void testCompletionStageThatCompletesLater() throws Exception {
        String request = """
                          {
                            "jsonrpc": "2.0",
                            "id": "3",
                            "method": "tools/call",
                            "params": {
                              "name": "asyncDelayedEcho",
                              "arguments": {
                                "input": "Async Hello"
                              }
                            }
                          }
                        """;

        String response = client.callMCP(request);

        String expectedResponseString = """
                        {"id":"3","jsonrpc":"2.0","result":{"content":[{"type":"text","text":"Async Hello: (async)"}],"isError":false}}
                        """;

        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Test
    public void testCompletionStageThatFailsImmediately() throws Exception {
        String request = """
                          {
                            "jsonrpc": "2.0",
                            "id": "2",
                            "method": "tools/call",
                            "params": {
                              "name": "asyncEcho",
                              "arguments": {
                                "input": "throw error"
                              }
                            }
                          }
                        """;

        String response = client.callMCP(request);

        String expectedResponseString = """
                        {"id":"2","jsonrpc":"2.0","result":{"content":[{"type":"text","text":"CWMCM0011E: An internal server error occurred while running the tool."}], "isError": true}}
                        """;

        JSONAssert.assertEquals(expectedResponseString, response, true);
        assertNotNull(server.waitForStringInLogUsingMark("Method call caused runtime exception", server.getDefaultLogFile()));
    }

    @Test
    public void testCompletionStageThatFailsLater() throws Exception {
        String request = """
                          {
                            "jsonrpc": "2.0",
                            "id": "2",
                            "method": "tools/call",
                            "params": {
                              "name": "asyncDelayedEcho",
                              "arguments": {
                                "input": "throw error"
                              }
                            }
                          }
                        """;

        String response = client.callMCP(request);

        String expectedResponseString = """
                        {"id":"2","jsonrpc":"2.0","result":{"content":[{"type":"text","text":"CWMCM0011E: An internal server error occurred while running the tool."}], "isError": true}}
                        """;

        JSONAssert.assertEquals(expectedResponseString, response, true);
        assertNotNull(server.waitForStringInLogUsingMark("Method call caused runtime exception", server.getDefaultLogFile()));
    }

    @Test
    public void testAsyncEchoWithInvalidParamsException() throws Exception {
        String request = """
                          {
                          "jsonrpc": "2.0",
                          "id": "2",
                          "method": "tools/call",
                          "params": {
                            "name": "asyncEcho"
                          }
                        }
                        """;

        String response = client.callMCP(request);
        String expectedResponseString = """
                        {"error":{"code":-32602,
                        "data":[
                            "The request does not have any arguments in parameters."
                            ],
                        "message":"Invalid params"},
                        "id":"2",
                        "jsonrpc":"2.0"}
                        """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Test
    public void testAsyncToolReturningListOfObjects() throws Exception {
        String request = """
                          {
                          "jsonrpc": "2.0",
                          "id": "2",
                          "method": "tools/call",
                          "params": {
                            "name": "asyncListObjectTool",
                            "arguments": {}
                          }
                        }
                        """;

        String response = client.callMCP(request);

        String expectedResponseString = """
                        {
                          "id":"2",
                          "jsonrpc":"2.0",
                          "result": {
                            "content": [
                              {
                                "type":"text",
                                "text":"[{\\\"country\\\":\\\"France\\\",\\\"isCapital\\\":true,\\\"name\\\":\\\"Paris\\\",\\\"population\\\":8000},{\\\"country\\\":\\\"England\\\",\\\"isCapital\\\":false,\\\"name\\\":\\\"Manchester\\\",\\\"population\\\":15000}]"
                              }
                            ],
                            "structuredContent": [
                              {
                                "country": "France",
                                "isCapital": true,
                                "name": "Paris",
                                "population": 8000
                              },
                              {
                                "country": "England",
                                "isCapital": false,
                                "name": "Manchester",
                                "population": 15000
                              }
                            ],
                            "isError": false
                          }
                        }
                        """;

        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Test
    public void testAsyncToolReturningSingleObject() throws Exception {
        String request = """
                          {
                          "jsonrpc": "2.0",
                          "id": "2",
                          "method": "tools/call",
                          "params": {
                            "name": "asyncObjectTool",
                            "arguments": {
                              "name": "Manchester"
                            }
                          }
                        }
                        """;

        String response = client.callMCP(request);
        String expectedResponseString = """
                        {
                          "id":"2",
                          "jsonrpc":"2.0",
                          "result": {
                            "content": [
                              {
                                "type":"text",
                                "text":"{\\\"country\\\":\\\"England\\\",\\\"isCapital\\\":false,\\\"name\\\":\\\"Manchester\\\",\\\"population\\\":8000}"
                              }
                            ],
                            "structuredContent": {
                              "country": "England",
                              "isCapital": false,
                              "name": "Manchester",
                              "population": 8000
                            },
                            "isError": false
                          }
                        }
                        """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Test(expected = Exception.class)
    public void testCompletionStageThatNeverCompletes() throws Exception {
        String request = """
                          {
                            "jsonrpc": "2.0",
                            "id": "2",
                            "method": "tools/call",
                            "params": {
                              "name": "asyncToolThatNeverCompletes",
                              "arguments": {
                                "input": "Hello"
                              }
                            }
                          }
                        """;
        client.callMCP(request);
    }
}
