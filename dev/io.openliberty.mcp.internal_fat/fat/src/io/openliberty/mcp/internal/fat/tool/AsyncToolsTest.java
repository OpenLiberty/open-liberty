/*******************************************************************************
 * Copyright (c) 2025, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.tool;

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONParser;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.mcp.internal.fat.tool.asyncToolApp.AsyncTools;
import io.openliberty.mcp.internal.fat.utils.McpClient;
import io.openliberty.mcp.internal.fat.utils.ToolStatus;
import io.openliberty.mcp.internal.fat.utils.ToolStatusClient;

@RunWith(FATRunner.class)
public class AsyncToolsTest extends FATServletClient {

    private static final String EXPECTED_ERROR = "Method call caused runtime exception. This is expected if the input was 'throw error'";
    @Server("mcp-server-async")
    public static LibertyServer server;

    @Rule
    public McpClient client = new McpClient(server, "/asyncToolsTest");

    @Rule
    public ToolStatusClient toolStatus = new ToolStatusClient(server, "/asyncToolsTest");

    @BeforeClass
    public static void setup() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "asyncToolsTest.war")
                                   .addPackage(AsyncTools.class.getPackage())
                                   .addPackage(ToolStatus.class.getPackage());

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
                        {"id":"2","jsonrpc":"2.0","result":{"content":[{"type":"text","text":"An internal server error occurred while running the tool."}], "isError": true}}
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
                        {"id":"2","jsonrpc":"2.0","result":{"content":[{"type":"text","text":"An internal server error occurred while running the tool."}], "isError": true}}
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
                        {
                          "jsonrpc": "2.0",
                          "id": "2",
                          "result": {
                            "isError": true,
                            "content": [
                              {
                                "type": "text",
                                "text": "The method expected the following arguments but did not receive them: [input]."
                              }
                            ]
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
    @Mode(TestMode.FULL)
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

    @Test
    public void testContentEncoderThatReturnsCompletionStage() throws Exception {
        String request = """
                          {
                          "jsonrpc": "2.0",
                          "id": "2",
                          "method": "tools/call",
                          "params": {
                            "name": "testContentEncoderCompletionStage",
                            "arguments": {}
                          }
                        }
                        """;

        String response = client.callMCP(request);
        // the object within the text field is expected to have the fields in lexicographical order after converting the object to JSON
        // 3 backslashes, as it should look like \" in the response. So we need extra backslashes to escape the \ and to escape the "
        String expectedResponseString = """
                        {
                          "id":"2",
                          "jsonrpc":"2.0",
                          "result": {
                            "content": [
                              {
                                "type":"text",
                                "text":"{\\\"age\\\":32,\\\"fistName\\\":\\\"Jon\\\",\\\"lastName\\\":\\\"Encoded by PersonContentEncoder\\\"}"
                              }
                            ],
                            "isError": false
                          }
                        }
                        """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Test
    public void testContentEncoderCompletionStageWithListEncoding() throws Exception {
        String request = """
                          {
                          "jsonrpc": "2.0",
                          "id": "2",
                          "method": "tools/call",
                          "params": {
                            "name": "testContentEncoderEncodingACompletionStageContainingAList",
                            "arguments": {}
                          }
                        }
                        """;

        String response = client.callMCP(request);
        // the object within the text field is expected to have the fields in lexicographical order after converting the object to JSON
        // 3 backslashes, as it should look like \" in the response. So we need extra backslashes to escape the \ and to escape the "
        String expectedResponseString = """
                        {
                          "id":"2",
                          "jsonrpc":"2.0",
                          "result": {
                            "content": [
                              {
                                "type":"text",
                                "text":"{\\\"age\\\":32,\\\"fistName\\\":\\\"Jon\\\",\\\"lastName\\\":\\\"Encoded by PersonContentEncoder\\\"}"
                              },
                              {
                                "type":"text",
                                "text":"{\\\"age\\\":22,\\\"fistName\\\":\\\"Jane\\\",\\\"lastName\\\":\\\"Encoded by PersonContentEncoder\\\"}"
                              }
                            ],
                            "isError": false
                          }
                        }
                        """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Test
    public void testAsyncObjectToolHasExpectedOutputSchema() throws Exception {
        String response = client.callMCP("""
                            {
                              "jsonrpc": "2.0",
                              "id": 1,
                              "method": "tools/list"
                            }
                        """);

        JSONObject root = (JSONObject) JSONParser.parseJSON(response);
        JSONArray tools = root.getJSONObject("result").getJSONArray("tools");

        JSONObject asyncObjectTool = null;

        for (int i = 0; i < tools.length(); i++) {
            JSONObject tool = tools.getJSONObject(i);
            if ("asyncObjectTool".equals(tool.getString("name"))) {
                asyncObjectTool = tool;
                break;
            }
        }

        assertNotNull("Tool 'asyncObjectTool' should be present in tool list", asyncObjectTool);

        String actualToolJson = asyncObjectTool.toString();

        String expectedToolJson = """
                                                    {
                          "name": "asyncObjectTool",
                          "title": "Async asyncObjectTool",
                          "description": "A tool to return an object of cities asynchronously",
                          "inputSchema": {
                            "type": "object",
                            "properties": {
                              "name": {
                                "type": "string",
                                "description": "name of your city"
                              }
                            },
                            "required": ["name"]
                          },
                          "outputSchema": {
                            "type": "object",
                            "properties": {
                              "country": {"type": "string"},
                              "isCapital": {"type": "boolean"},
                              "name": {"type": "string"},
                              "population": {"type": "integer"}
                            },
                            "required": ["name", "country", "population", "isCapital"]
                          }
                        }
                         """;

        JSONAssert.assertEquals(expectedToolJson, actualToolJson, true);
    }

    @Test
    public void testAsyncToolsWithoutStructuredContentHaveNoOutputSchema() throws Exception {
        String response = client.callMCP("""
                            {
                              "jsonrpc": "2.0",
                              "id": 1,
                              "method": "tools/list"
                            }
                        """);

        JSONObject root = (JSONObject) JSONParser.parseJSON(response);
        JSONArray tools = root.getJSONObject("result").getJSONArray("tools");

        List<String> toolsThatShouldNotHaveOutputSchema = List.of(
                                                                  "asyncStringTool",
                                                                  "asyncContentTool",
                                                                  "asyncListContentTool",
                                                                  "asyncObjectToolWithoutStructuredContent");

        for (int i = 0; i < tools.length(); i++) {
            JSONObject tool = tools.getJSONObject(i);
            String name = tool.getString("name");

            if (toolsThatShouldNotHaveOutputSchema.contains(name)) {
                assertFalse("Tool '" + name + "' should NOT have an output schema", tool.has("outputSchema"));
            }
        }
    }

    @Test
    public void testAsyncToolsWithBasicOutputTypesShouldNotHaveOutputSchema() throws Exception {
        String response = client.callMCP("""
                            {
                              "jsonrpc": "2.0",
                              "id": 1,
                              "method": "tools/list"
                            }
                        """);

        JSONObject root = (JSONObject) JSONParser.parseJSON(response);
        JSONArray tools = root.getJSONObject("result").getJSONArray("tools");

        // Tools that SHOULD NOT have an output schema
        List<String> toolsWithoutSchema = List.of(
                                                  "asyncEcho",
                                                  "asyncDelayedEcho",
                                                  "asyncCancellationTool",
                                                  "asyncToolThatNeverCompletes");

        for (int i = 0; i < tools.length(); i++) {
            JSONObject tool = tools.getJSONObject(i);
            String name = tool.getString("name");

            if (toolsWithoutSchema.contains(name)) {
                assertFalse(
                            "Tool '" + name + "' should NOT have an output schema",
                            tool.has("outputSchema"));
            }
        }
    }

}
