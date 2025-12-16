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
import static componenttest.custom.junit.runner.Mode.TestMode.FULL;
import static componenttest.rules.repeater.EERepeatActions.EE10;
import static componenttest.rules.repeater.EERepeatActions.EE11;
import static io.openliberty.mcp.internal.fat.utils.TestConstants.ACCEPT;
import static io.openliberty.mcp.internal.fat.utils.TestConstants.MCP_PROTOCOL_VERSION;
import static io.openliberty.mcp.internal.fat.utils.TestConstants.MCP_SESSION_ID;
import static io.openliberty.mcp.internal.fat.utils.TestConstants.VALUE_ACCEPT_DEFAULT;
import static io.openliberty.mcp.internal.fat.utils.TestConstants.VALUE_MCP_PROTOCOL_VERSION;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runners.model.Statement;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.custom.junit.runner.TestModeFilter;
import componenttest.rules.repeater.EERepeatActions;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import componenttest.topology.utils.HttpRequest;
import io.openliberty.mcp.internal.fat.tool.basicToolApp.BasicTools;
import io.openliberty.mcp.internal.fat.utils.McpClient;

/**
 *
 */
@RunWith(FATRunner.class)
public class ToolTest extends FATServletClient {

    @Server("mcp-server")
    public static LibertyServer server;

    @ClassRule
    public static TestRule repeatInLiteOnly;

    static {
        if (TestModeFilter.FRAMEWORK_TEST_MODE == TestMode.LITE) {
            // Whole bucket is repeated in FULL mode
            // We want to add a repeat for just this test when running in LITE mode
            repeatInLiteOnly = EERepeatActions.repeat("mcp-server", TestMode.LITE, /* skipTransformation */ true, EE10, EE11);
        } else {
            // In full mode, return a no-op
            repeatInLiteOnly = new TestRule() {
                @Override
                public Statement apply(Statement statement, Description desc) {
                    return statement;
                }
            };
        }
    }

    @Rule
    public McpClient client = new McpClient(server, "/toolTest");

    @BeforeClass
    public static void setup() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "toolTest.war")
                                   .addPackage(BasicTools.class.getPackage());

        ShrinkHelper.exportDropinAppToServer(server, war, SERVER_ONLY);

        server.startServer();

        assertNotNull(server.waitForStringInLog("MCP server endpoint: .*/mcp$")); // regex matches string that ends with /mcp e.g. "MCP server endpoint: http://macbookpro.home:8010/toolTest/mcp"

    }

    @AfterClass
    public static void teardown() throws Exception {
        server.stopServer(
                          "CWMCM0010E", //The JSON-RPC request is not valid JSON.
                          "CWMCM0011E", // The JSON-RPC request was invalid.
                          "CWMCM0012E", // The requested JSON-RPC method is not found.
                          "CWMCM0013E", // JSON-RPC PC request contained invalid parameters.
                          "CWMCM0014E", // An Internal Server Error occurred whilst processing the JSON-RPC request.
                          "CWMCM0010E", //  Tool method threw an unexpected exception
                          "CWMCM0011E" // An internal server error occurred
        );
    }

    @Test
    public void postJsonRpc() throws Exception {
        String request = """
                          {
                            "jsonrpc": "2.0",
                            "id": "2",
                            "method": "tools/call",
                            "params": {
                              "name": "echo",
                              "arguments": {
                                "input": "Hello"
                              }
                            }
                          }
                        """;
        String response = client.callMCP(request);

        JSONObject jsonResponse = new JSONObject(response);
        // Lenient mode tests
        JSONAssert.assertEquals("{ \"jsonrpc\": \"2.0\", \"id\": \"2\"}", response, false);
        JSONAssert.assertEquals("{\"result\":{\"content\":[{\"type\":\"text\",\"text\":\"Hello\"}]}}", jsonResponse, false);

        // Strict Mode tests
        String expectedResponseString = """
                        {"id":\"2\","jsonrpc":"2.0","result":{"content":[{"type":"text","text":"Hello"}], "isError": false}}
                        """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Test
    public void testEchoRequestIdInjectionWithStringId() throws Exception {
        String request = """
                        {
                          "jsonrpc": "2.0",
                          "id": "my-custom-id-42",
                          "method": "tools/call",
                          "params": {
                            "name": "echoRequestId",
                            "arguments": {
                              "input": "hello-world"
                            }
                          }
                        }
                        """;

        String response = client.callMCP(request);
        String expectedResponseString = """
                        {
                          "id": "my-custom-id-42",
                          "jsonrpc": "2.0",
                          "result": {
                            "content": [
                              {
                                "type": "text",
                                "text": "my-custom-id-42: hello-world"
                              }
                            ],
                            "isError": false
                          }
                        }
                        """;

        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Test
    public void testEchoWithNumberIdType() throws Exception {
        String request = """
                          {
                          "jsonrpc": "2.0",
                          "id": 2,
                          "method": "tools/call",
                          "params": {
                            "name": "echo",
                            "arguments": {
                              "input": "Hello"
                            }
                          }
                        }
                        """;

        String response = client.callMCP(request);
        JSONObject jsonResponse = new JSONObject(response);
        // Lenient mode tests
        JSONAssert.assertEquals("{ \"jsonrpc\": \"2.0\", \"id\": 2}", response, false);
        JSONAssert.assertEquals("{\"result\":{\"content\":[{\"type\":\"text\",\"text\":\"Hello\"}]}}", jsonResponse, false);

        // Strict Mode tests
        String expectedResponseString = """
                        {"id":2,"jsonrpc":"2.0","result":{"content":[{"type":"text","text":"Hello"}], "isError": false}}
                        """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Test
    public void testEchoWithStringIdType() throws Exception {
        String request = """
                          {
                          "jsonrpc": "2.0",
                          "id": "2",
                          "method": "tools/call",
                          "params": {
                            "name": "echo",
                            "arguments": {
                              "input": "Hello"
                            }
                          }
                        }
                        """;

        String response = client.callMCP(request);
        JSONObject jsonResponse = new JSONObject(response);

        // Lenient mode tests
        JSONAssert.assertEquals("{ \"jsonrpc\": \"2.0\", \"id\": \"2\"}", response, false);
        JSONAssert.assertEquals("{\"result\":{\"content\":[{\"type\":\"text\",\"text\":\"Hello\"}]}}", jsonResponse, false);

        // Strict Mode tests
        String expectedResponseString = """
                        {"id":\"2\","jsonrpc":"2.0","result":{"content":[{"type":"text","text":"Hello"}], "isError": false}}
                        """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Test
    public void testEchoWithInvalidRequestException() throws Exception {
        String request = """
                          {
                          "jsonrpc": "1.0",
                          "id": false
                        }
                        """;

        String response = client.callMCP(request);
        String expectedResponseString = """
                        {"error":{"code":-32600,
                        "data":[
                            "The jsonrpc field must be present. Only JSONRPC 2.0 is currently supported.",
                            "The method field is empty.",
                            "The id type is not an acceptable type. The id must be a string or integer."
                            ],
                        "message":"Invalid request"},
                        "id": null,
                        "jsonrpc":"2.0"}
                        """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Test
    public void testEchoWithParseErrorException() throws Exception {
        String request = """
                          }
                          "jsonrpc": "1.0",
                          "id": false,
                          {
                        }
                        """;

        String response = client.callMCP(request);
        String expectedResponseString = """
                        {"error":{"code":-32700,
                        "message":"Parse error",
                        "data":["Invalid token=CURLYCLOSE at (line no=1, column no=3, offset=2). Expected tokens are: [CURLYOPEN, SQUAREOPEN, STRING, NUMBER, TRUE, FALSE, NULL]"]},
                        "id": null,
                        "jsonrpc":"2.0"}
                        """;
        JSONAssert.assertEquals(expectedResponseString, response, false);
    }

    @Test
    public void testEchoWithInvalidParamsException() throws Exception {
        String request = """
                          {
                          "jsonrpc": "2.0",
                          "id": "2",
                          "method": "tools/call",
                          "params": {
                            "name": "echo"
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
    public void testEchoWithInvalidParamsArgumentMismatchException() throws Exception {
        String request = """
                          {
                          "jsonrpc": "2.0",
                          "id": "2",
                          "method": "tools/call",
                          "params": {
                            "name": "echo",
                            "arguments": {
                                "other": "Hello"
                              }
                          }
                        }
                        """;

        String response = client.callMCP(request);
        String expectedResponseString = """
                        {"error":{"code":-32602,
                        "data":[
                            "The following arguments were passed but were not found in the method: [other].",
                            "The following arguments were expected by the method but were not provided: [input]."
                            ],
                        "message": "Invalid params"},
                        "id":"2",
                        "jsonrpc":"2.0"}
                        """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Test
    public void testEchoWithMethodNotFoundException() throws Exception {
        String request = """
                          {
                          "jsonrpc": "2.0",
                          "id": "2",
                          "method": "call/tools",
                          "params": {
                            "name": "echo",
                            "arguments": {
                              "input": "Hello"
                            }
                          }
                        }
                        """;

        String response = client.callMCP(request);
        String expectedResponseString = """
                        {"error":{"code":-32601,
                        "data":[
                            "call/tools not found"
                            ],
                        "message":"Method not found"},
                        "id":"2",
                        "jsonrpc":"2.0"}
                        """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Test
    public void testToolReturnsListOfContent() throws Exception {
        String request = """
                        {
                          "jsonrpc": "2.0",
                          "id": 1,
                          "method": "tools/call",
                          "params": {
                            "name": "textContentTool",
                            "arguments": {
                              "input": "hello"
                            }
                          }
                        }
                        """;
        String response = client.callMCP(request);

        String expectedResponseString = """
                        {
                          "id": 1,
                          "jsonrpc": "2.0",
                          "result": {
                            "content": [
                              { "type": "text", "text": "Echo: hello" }
                            ],
                            "isError": false
                          }
                        }
                        """;

        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Test
    public void testToolReturnsListOfContentWithAnnotations() throws Exception {
        String request = """
                        {
                          "jsonrpc": "2.0",
                          "id": 1,
                          "method": "tools/call",
                          "params": {
                            "name": "textContentToolWithContentAnnotation",
                            "arguments": {
                              "input": "hello"
                            }
                          }
                        }
                        """;
        String response = client.callMCP(request);

        String expectedResponseString = """
                        {
                          "id": 1,
                          "jsonrpc": "2.0",
                          "result": {
                            "content": [
                              {
                                "annotations": {
                                  "audience": "assistant",
                                  "lastModified": "2025-08-26T08:40:00Z",
                                  "priority": 0.5
                                },
                                "text": "Echo: hello",
                                "type": "text"
                              }
                            ],
                            "isError": false
                          }
                        }
                        """;

        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Test
    public void testToolReturnsImageContentList() throws Exception {
        String request = """
                        {
                          "jsonrpc": "2.0",
                          "id": 1,
                          "method": "tools/call",
                          "params": {
                            "name": "imageContentTool",
                            "arguments": {
                              "imageData": "base64-encoded-image"
                            }
                          }
                        }
                        """;

        String response = client.callMCP(request);

        String expectedResponseString = """
                        {
                            "id": 1,
                            "jsonrpc": "2.0",
                            "result": {
                              "content": [
                                {
                                  "data": "base64-encoded-image",
                                  "mimeType": "image/png",
                                  "type": "image"
                                }
                              ],
                              "isError": false
                            }
                         }
                         """;

        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Test
    public void testToolReturnsImageContentListWithAnnotations() throws Exception {
        String request = """
                        {
                          "jsonrpc": "2.0",
                          "id": 1,
                          "method": "tools/call",
                          "params": {
                            "name": "imageContentToolWithContentAnnotation",
                            "arguments": {
                              "imageData": "base64-encoded-image"
                            }
                          }
                        }
                        """;

        String response = client.callMCP(request);

        String expectedResponseString = """
                        {
                            "id": 1,
                            "jsonrpc": "2.0",
                            "result": {
                              "content": [
                                {
                                  "annotations": {
                                    "audience": "user",
                                    "lastModified": "2025-08-26T08:40:00Z",
                                    "priority": 0.8
                                  },
                                  "data": "base64-encoded-image",
                                  "mimeType": "image/png",
                                  "type": "image"
                                }
                              ],
                              "isError": false
                            }
                         }
                         """;

        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Test
    public void testToolReturnsAudioContentList() throws Exception {
        String request = """
                        {
                          "jsonrpc": "2.0",
                          "id": 1,
                          "method": "tools/call",
                          "params": {
                            "name": "audioContentTool",
                            "arguments": {
                              "audioData": "base64-encoded-audio"
                            }
                          }
                        }
                        """;
        String response = client.callMCP(request);

        String expectedResponseString = """
                        {
                           "id": 1,
                           "jsonrpc": "2.0",
                           "result": {
                             "content": [
                               {
                                 "data": "base64-encoded-audio",
                                 "mimeType": "audio/mpeg",
                                 "type": "audio"
                               }
                             ],
                             "isError": false
                           }
                         }
                         """;

        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Test
    public void testToolReturnsAudioContentListWithAnnotations() throws Exception {
        String request = """
                        {
                          "jsonrpc": "2.0",
                          "id": 1,
                          "method": "tools/call",
                          "params": {
                            "name": "audioContentToolWithContentAnnotation",
                            "arguments": {
                              "audioData": "base64-encoded-audio"
                            }
                          }
                        }
                        """;
        String response = client.callMCP(request);

        String expectedResponseString = """
                                                {
                          "id": 1,
                          "jsonrpc": "2.0",
                          "result": {
                            "content": [
                              {
                                "annotations": {
                                  "audience": "assistant",
                                  "lastModified": "2025-08-26T08:40:00Z",
                                  "priority": 0.3
                                },
                                "data": "base64-encoded-audio",
                                "mimeType": "audio/mpeg",
                                "type": "audio"
                              }
                            ],
                            "isError": false
                          }
                        }
                        """;

        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Test
    public void testMixedContentTool() throws Exception {
        String request = """
                        {
                          "jsonrpc": "2.0",
                          "id": 1,
                          "method": "tools/call",
                          "params": {
                            "name": "mixedContentTool",
                            "arguments": {
                              "input": "Hello"
                            }
                          }
                        }
                        """;
        String response = client.callMCP(request);

        String expectedResponseString = """
                        {
                          "id": 1,
                          "jsonrpc": "2.0",
                          "result": {
                            "content": [
                              { "text": "Echo: Hello", "type": "text" },
                              { "data": "base64-encoded-image", "mimeType": "image/png", "type": "image" },
                              { "data": "base64-encoded-audio", "mimeType": "audio/mpeg", "type": "audio" }
                            ],
                            "isError": false
                          }
                        }
                         """;

        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Test
    public void testMixedContentListTool() throws Exception {
        String request = """
                        {
                          "jsonrpc": "2.0",
                          "id": 1,
                          "method": "tools/call",
                          "params": {
                            "name": "mixedContentListTool",
                            "arguments": {
                              "input": "Hello"
                            }
                          }
                        }
                        """;
        String response = client.callMCP(request);
        String expectedResponseString = """
                        {
                          "id": 1,
                          "jsonrpc": "2.0",
                          "result": {
                            "content": [
                              { "text": "Echo: Hello", "type": "text" },
                              { "data": "base64-encoded-image", "mimeType": "image/png", "type": "image" },
                              { "data": "base64-encoded-audio", "mimeType": "audio/mpeg", "type": "audio" }
                            ],
                            "isError": false
                          }
                        }
                         """;

        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Test
    public void testToolCallWithoutNonRequiredStringArg() throws Exception {
        String request = """
                          {
                          "jsonrpc": "2.0",
                          "id": 2,
                          "method": "tools/call",
                          "params": {
                            "name": "testToolArgStringNotRequired",
                            "arguments": {}
                          }
                        }
                        """;

        String response = client.callMCP(request);
        String expectedResponseString = """
                        {"id":2,"jsonrpc":"2.0","result":{"content":[{"type":"text","text": "null"}], "isError": false}}
                        """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Test
    public void testToolCallWithoutNonRequiredIntArg() throws Exception {
        String request = """
                          {
                          "jsonrpc": "2.0",
                          "id": 2,
                          "method": "tools/call",
                          "params": {
                            "name": "testToolArgIntNotRequired",
                            "arguments": {}
                          }
                        }
                        """;

        String response = client.callMCP(request);
        String expectedResponseString = """
                        {"id":2,"jsonrpc":"2.0","result":{"content":[{"type":"text","text": "0"}], "isError": false}}
                        """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Test
    public void testToolCallWithoutNonRequiredArrayArg() throws Exception {
        String request = """
                          {
                          "jsonrpc": "2.0",
                          "id": 2,
                          "method": "tools/call",
                          "params": {
                            "name": "testToolArgArrayNotRequired",
                            "arguments": {}
                          }
                        }
                        """;

        String response = client.callMCP(request);
        String expectedResponseString = """
                        {"id":2,"jsonrpc":"2.0","result":{"content":[{"type":"text","text": "null"}], "isError": false}}
                        """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Test
    public void testToolCallWithoutNonRequiredObjectArg() throws Exception {
        String request = """
                          {
                          "jsonrpc": "2.0",
                          "id": 2,
                          "method": "tools/call",
                          "params": {
                            "name": "testToolArgObjectNotRequired",
                            "arguments": {}
                          }
                        }
                        """;

        String response = client.callMCP(request);
        String expectedResponseString = """
                        {"id":2,"jsonrpc":"2.0","result":{"content":[{"type":"text","text": "null"}], "isError": false}}
                        """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Test
    public void testToolCallWithTwoToolArgsWithoutNonRequiredArg() throws Exception {
        String request = """
                          {
                          "jsonrpc": "2.0",
                          "id": 2,
                          "method": "tools/call",
                          "params": {
                            "name": "testMultipleToolArgsOneNotRequired",
                            "arguments": {
                              "planet": "Earth"
                            }
                          }
                        }
                        """;

        String response = client.callMCP(request);
        String expectedResponseString = """
                        {"id":2,"jsonrpc":"2.0","result":{"content":[{"type":"text","text": "Planet Earth was created in the year 0"}], "isError": false}}
                        """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Test
    public void testToolCallWithToolArgStringDefaultValue() throws Exception {
        String request = """
                          {
                          "jsonrpc": "2.0",
                          "id": 2,
                          "method": "tools/call",
                          "params": {
                            "name": "testToolArgStringDefaultValue",
                            "arguments": {}
                          }
                        }
                        """;

        String response = client.callMCP(request);
        String expectedResponseString = """
                        {"id":2,"jsonrpc":"2.0","result":{"content":[{"type":"text","text": "Jupiter"}], "isError": false}}
                        """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Test
    public void testToolCallWithToolArgIntDefaultValue() throws Exception {
        String request = """
                          {
                          "jsonrpc": "2.0",
                          "id": 2,
                          "method": "tools/call",
                          "params": {
                            "name": "testToolArgIntDefaultValue",
                            "arguments": {}
                          }
                        }
                        """;

        String response = client.callMCP(request);
        String expectedResponseString = """
                        {"id":2,"jsonrpc":"2.0","result":{"content":[{"type":"text","text": "2025"}], "isError": false}}
                        """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Test
    public void testToolCallWithTwoToolArgsWithOneDefaultValue() throws Exception {
        String request = """
                          {
                          "jsonrpc": "2.0",
                          "id": 2,
                          "method": "tools/call",
                          "params": {
                            "name": "testMultipleToolArgsOneDefaultValue",
                            "arguments": {
                              "year": "2000"
                            }
                          }
                        }
                        """;

        String response = client.callMCP(request);
        String expectedResponseString = """
                        {"id":2,"jsonrpc":"2.0","result":{"content":[{"type":"text","text": "Planet Jupiter was created in the year 2000"}], "isError": false}}
                        """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Test
    public void testToolList() throws Exception {
        String request = """
                        {
                          "jsonrpc": "2.0",
                          "id": 1,
                          "method": "tools/list",
                          "params": {
                            "cursor": "optional-cursor-value"
                          }
                        }
                        """;

        String response = client.callMCP(request);
        JSONObject jsonResponse = new JSONObject(response);
        String expectedString = "";
        try (InputStream inputStream = this.getClass().getResourceAsStream("expected-tools-list-response.json")) {
            if (inputStream == null) {
                throw new FileNotFoundException("Resource not found: expected-tools-list-response.json");
            }
            expectedString = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
        JSONAssert.assertEquals(expectedString, jsonResponse.toString(), JSONCompareMode.NON_EXTENSIBLE);
    }

    /**
     *
     */

    @Test
    public void testEchoMethodCallError() throws Exception {
        server.setMarkToEndOfLog();

        String request = """
                          {
                          "jsonrpc": "2.0",
                          "id": 2,
                          "method": "tools/call",
                          "params": {
                            "name": "echo",
                            "arguments": {
                              "input": "throw error"
                            }
                          }
                        }
                        """;

        String response = client.callMCP(request);

        String expectedResponseString = """
                        {"id":2,"jsonrpc":"2.0","result":{"content":[{"type":"text","text":"CWMCM0011E: An internal server error occurred while running the tool."}], "isError": true}}
                        """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
        assertNotNull(server.waitForStringInLogUsingMark("Method call caused runtime exception", server.getDefaultLogFile()));
    }

    @Test
    public void testprivateMethodAccessError() throws Exception {
        String request = """
                          {
                          "jsonrpc": "2.0",
                          "id": 2,
                          "method": "tools/call",
                          "params": {
                            "name": "privateEcho",
                            "arguments": {
                              "input": "throw error"
                            }
                          }
                        }
                        """;

        String response = client.callMCP(request);
        JSONObject jsonResponse = new JSONObject(response);

        String expectedResponseString = """
                        {"error":{"code":-32603,
                        "data":[
                            "Could not call privateEcho"
                            ],
                        "message":"Internal error"},
                        "id":2,
                        "jsonrpc":"2.0"}
                        """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Test
    public void testToolNotFoundError() throws Exception {
        String request = """
                          {
                          "jsonrpc": "2.0",
                          "id": 2,
                          "method": "tools/call",
                          "params": {
                            "name": "privateEchoMissing",
                            "arguments": {
                              "input": "Hello",
                              "repeat": 4
                            }
                          }
                        }
                        """;

        String response = client.callMCP(request);
        String expectedResponseString = """
                        {
                            "id": 2,
                            "jsonrpc": "2.0",
                            "error": {
                                "code": -32602,
                                "data": [
                                    "Method privateEchoMissing not found"
                                ],
                                "message": "Invalid params"
                            }
                        }
                        """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Test
    public void testAddWithIntegerInputArgs() throws Exception {
        String request = """
                          {
                          "jsonrpc": "2.0",
                          "id": "2",
                          "method": "tools/call",
                          "params": {
                            "name": "add",
                            "arguments": {
                              "num1": 100,
                              "num2": 200
                            }
                          }
                        }
                        """;

        String response = client.callMCP(request);
        JSONObject jsonResponse = new JSONObject(response);

        // Lenient mode tests
        JSONAssert.assertEquals("{\"result\":{\"content\":[{\"type\":\"text\",\"text\": \"300\"}]}}", jsonResponse, false);

        // Strict Mode tests
        String expectedResponseString = """
                        {"id":\"2\","jsonrpc":"2.0","result":{"content":[{"type":"text","text": "300"}], "isError": false}}
                        """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Test
    public void testToggleWithBooleanArgs() throws Exception {
        String request = """
                          {
                          "jsonrpc": "2.0",
                          "id": "2",
                          "method": "tools/call",
                          "params": {
                            "name": "toggle",
                            "arguments": {
                              "value": true
                            }
                          }
                        }
                        """;

        String response = client.callMCP(request);

        String expectedResponseString = """
                        {"id":"2","jsonrpc":"2.0","result":{"content":[{"type":"text","text": "false"}], "isError": false}}
                        """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Test
    public void testJSONCharacter() throws Exception {
        String request = """
                          {
                          "jsonrpc": "2.0",
                          "id": "2",
                          "method": "tools/call",
                          "params": {
                            "name": "testJSONCharacter",
                            "arguments": {
                              "c": "c"
                            }
                          }
                        }
                        """;

        String response = client.callMCP(request);
        String expectedResponseString = """
                        {"id":"2","jsonrpc":"2.0","result":{"content":[{"type":"text","text":"c"}], "isError": false}}
                        """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Test
    public void testJSONcharacter() throws Exception {
        String request = """
                          {
                          "jsonrpc": "2.0",
                          "id": "2",
                          "method": "tools/call",
                          "params": {
                            "name": "testJSONcharacter",
                            "arguments": {
                              "c": "c"
                            }
                          }
                        }
                        """;

        String response = client.callMCP(request);
        String expectedResponseString = """
                        {"id":"2","jsonrpc":"2.0","result":{"content":[{"type":"text","text":"c"}], "isError": false}}
                        """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Test
    public void testJSONlong() throws Exception {
        String request = """
                          {
                          "jsonrpc": "2.0",
                          "id": "2",
                          "method": "tools/call",
                          "params": {
                            "name": "testJSONlong",
                            "arguments": {
                              "num1": 2
                            }
                          }
                        }
                        """;

        String response = client.callMCP(request);
        String expectedResponseString = """
                        {"id":"2","jsonrpc":"2.0","result":{"content":[{"type":"text","text":"2"}],"isError":false}}
                        """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Test
    public void testJSONdouble() throws Exception {
        String request = """
                          {
                          "jsonrpc": "2.0",
                          "id": "2",
                          "method": "tools/call",
                          "params": {
                            "name": "testJSONdouble",
                            "arguments": {
                              "num1": 2.2
                            }
                          }
                        }
                        """;

        String response = client.callMCP(request);
        String expectedResponseString = """
                        {"id":"2","jsonrpc":"2.0","result":{"content":[{"type":"text","text":"2.2"}], "isError": false}}
                        """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Test
    public void testJSONbyte() throws Exception {
        String request = """
                          {
                          "jsonrpc": "2.0",
                          "id": "2",
                          "method": "tools/call",
                          "params": {
                            "name": "testJSONbyte",
                            "arguments": {
                              "num1": 2
                            }
                          }
                        }
                        """;

        String response = client.callMCP(request);
        String expectedResponseString = """
                        {"id":"2","jsonrpc":"2.0","result":{"content":[{"type":"text","text":"2"}], "isError": false}}
                        """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Test
    public void testJSONfloat() throws Exception {
        String request = """
                          {
                          "jsonrpc": "2.0",
                          "id": "2",
                          "method": "tools/call",
                          "params": {
                            "name": "testJSONfloat",
                            "arguments": {
                              "num1": 2.5
                            }
                          }
                        }
                        """;

        String response = client.callMCP(request);
        String expectedResponseString = """
                        {"id":"2","jsonrpc":"2.0","result":{"content":[{"type":"text","text":"2.5"}], "isError": false}}
                        """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Test
    public void testJSONshort() throws Exception {
        String request = """
                          {
                          "jsonrpc": "2.0",
                          "id": "2",
                          "method": "tools/call",
                          "params": {
                            "name": "testJSONshort",
                            "arguments": {
                              "num1": 2
                            }
                          }
                        }
                        """;

        String response = client.callMCP(request);
        String expectedResponseString = """
                        {"id":"2","jsonrpc":"2.0","result":{"content":[{"type":"text","text":"2"}], "isError": false}}
                        """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Test
    public void testJSONLong() throws Exception {
        String request = """
                          {
                          "jsonrpc": "2.0",
                          "id": "2",
                          "method": "tools/call",
                          "params": {
                            "name": "testJSONLong",
                            "arguments": {
                              "num1": 2
                            }
                          }
                        }
                        """;

        String response = client.callMCP(request);
        String expectedResponseString = """
                        {"id":"2","jsonrpc":"2.0","result":{"content":[{"type":"text","text":"2"}], "isError": false}}
                        """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Test
    public void testJSONDouble() throws Exception {
        String request = """
                          {
                          "jsonrpc": "2.0",
                          "id": "2",
                          "method": "tools/call",
                          "params": {
                            "name": "testJSONDouble",
                            "arguments": {
                              "num1": 2.5
                            }
                          }
                        }
                        """;

        String response = client.callMCP(request);
        String expectedResponseString = """
                        {"id":"2","jsonrpc":"2.0","result":{"content":[{"type":"text","text":"2.5"}], "isError": false}}
                        """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Test
    public void testJSONByte() throws Exception {
        String request = """
                          {
                          "jsonrpc": "2.0",
                          "id": "2",
                          "method": "tools/call",
                          "params": {
                            "name": "testJSONByte",
                            "arguments": {
                              "num1": 2
                            }
                          }
                        }
                        """;

        String response = client.callMCP(request);
        String expectedResponseString = """
                        {"id":"2","jsonrpc":"2.0","result":{"content":[{"type":"text","text":"2"}], "isError": false}}
                        """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Test
    public void testJSONFloat() throws Exception {
        String request = """
                          {
                          "jsonrpc": "2.0",
                          "id": "2",
                          "method": "tools/call",
                          "params": {
                            "name": "testJSONFloat",
                            "arguments": {
                              "num1": 2.5
                            }
                          }
                        }
                        """;

        String response = client.callMCP(request);
        String expectedResponseString = """
                        {"id":"2","jsonrpc":"2.0","result":{"content":[{"type":"text","text":"2.5"}], "isError": false}}
                        """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Test
    public void testJSONShort() throws Exception {
        String request = """
                          {
                          "jsonrpc": "2.0",
                          "id": "2",
                          "method": "tools/call",
                          "params": {
                            "name": "testJSONShort",
                            "arguments": {
                              "num1": 2
                            }
                          }
                        }
                        """;

        String response = client.callMCP(request);
        String expectedResponseString = """
                        {"id":"2","jsonrpc":"2.0","result":{"content":[{"type":"text","text":"2"}], "isError": false}}
                        """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Test
    public void testJSONInteger() throws Exception {
        String request = """
                          {
                          "jsonrpc": "2.0",
                          "id": "2",
                          "method": "tools/call",
                          "params": {
                            "name": "testJSONInteger",
                            "arguments": {
                              "num1": 2
                            }
                          }
                        }
                        """;

        String response = client.callMCP(request);
        String expectedResponseString = """
                        {"id":"2","jsonrpc":"2.0","result":{"content":[{"type":"text","text":"2"}], "isError": false}}
                        """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Test
    public void testJSONBoolean() throws Exception {
        String request = """
                          {
                          "jsonrpc": "2.0",
                          "id": "2",
                          "method": "tools/call",
                          "params": {
                            "name": "testJSONBoolean",
                            "arguments": {
                              "b": true
                            }
                          }
                        }
                        """;

        String response = client.callMCP(request);
        String expectedResponseString = """
                        {"id":"2","jsonrpc":"2.0","result":{"content":[{"type":"text","text":"true"}], "isError": false}}
                        """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Test
    public void testReturningObject() throws Exception {
        String request = """
                          {
                          "jsonrpc": "2.0",
                          "id": "2",
                          "method": "tools/call",
                          "params": {
                            "name": "testObjectResponse",
                            "arguments": {
                              "name": "Manchester"
                            }
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

    @Test
    public void testReturningArray() throws Exception {
        String request = """
                          {
                          "jsonrpc": "2.0",
                          "id": "2",
                          "method": "tools/call",
                          "params": {
                            "name": "testArrayResponse",
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
                                "text":"[1,2,3,4,5]"
                              }
                            ],
                            "structuredContent": [1,2,3,4,5],
                            "isError": false
                          }
                        }
                        """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Test
    public void testReturningStringList() throws Exception {
        String request = """
                          {
                          "jsonrpc": "2.0",
                          "id": "2",
                          "method": "tools/call",
                          "params": {
                            "name": "testListStringResponse",
                            "arguments": {}
                          }
                        }
                        """;

        String response = client.callMCP(request);
        // 3 backslashes, as it should look like \" in the response. So we need extra backslashes to escape the \ and to escape the "
        String expectedResponseString = """
                        {
                          "id":"2",
                          "jsonrpc":"2.0",
                          "result": {
                            "content": [
                              {
                                "type":"text",
                                "text":"[\\\"red\\\",\\\"blue\\\",\\\"yellow\\\"]"
                              }
                            ],
                            "structuredContent": ["red","blue","yellow"],
                            "isError": false
                          }
                        }
                        """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Test
    public void testReturningListOfObjects() throws Exception {
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

        String response = client.callMCP(request);
        // the object within the text field is expected to have the fields in lexicographical order after converting the object to JSON
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
    public void testStringNotReturnedAsStructuredContent() throws Exception {
        String request = """
                          {
                          "jsonrpc": "2.0",
                          "id": "2",
                          "method": "tools/call",
                          "params": {
                            "name": "testStringStructuredContentResponse",
                            "arguments": {}
                          }
                        }
                        """;

        String response = client.callMCP(request);
        String expectedResponseString = """
                        {"id":"2","jsonrpc":"2.0","result":{"content":[{"type":"text","text":"Hello World"}], "isError": false}}
                        """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Test
    public void testStaticInnerTool() throws Exception {
        String request = """
                        {
                          "id": "2",
                          "jsonrpc": "2.0",
                          "method": "tools/call",
                          "params": {
                            "name": "staticInnerTool",
                            "arguments": {
                              "input": "World"
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
                                                        "text":"Hello World"
                                                      }
                                                    ],
                                                    "isError": false
                                                  }
                                                }
                        """;

        JSONAssert.assertEquals(expectedResponseString, response, false);
    }

    public void testSpecialCharactersInToolName() throws Exception {
        String request = """
                          {
                          "jsonrpc": "2.0",
                          "id": "2",
                          "method": "tools/call",
                          "params": {
                            "name": "specialCharactersInToolName@!><={}'().%:",
                            "arguments": {
                              "arg1": "Hello"
                            }
                          }
                        }
                        """;

        String response = client.callMCP(request);
        JSONObject jsonResponse = new JSONObject(response);

        // Lenient mode tests
        JSONAssert.assertEquals("{ \"jsonrpc\": \"2.0\", \"id\": \"2\"}", response, false);
        JSONAssert.assertEquals("{\"result\":{\"content\":[{\"type\":\"text\",\"text\":\"Hello\"}]}}", jsonResponse, false);

        // Strict Mode tests
        String expectedResponseString = """
                        {"id":\"2\","jsonrpc":"2.0","result":{"content":[{"type":"text","text":"Hello"}], "isError": false}}
                        """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    public void testSpecialCharactersInToolArgName() throws Exception {
        String request = """
                          {
                          "jsonrpc": "2.0",
                          "id": "2",
                          "method": "tools/call",
                          "params": {
                            "name": "specialCharactersInToolArgName",
                            "arguments": {
                              "@arg1!><": "Hello",
                              "@arg2={}": "Hello2"
                            }
                          }
                        }
                        """;

        String response = client.callMCP(request);
        JSONObject jsonResponse = new JSONObject(response);

        // Lenient mode tests
        JSONAssert.assertEquals("{ \"jsonrpc\": \"2.0\", \"id\": \"2\"}", response, false);
        JSONAssert.assertEquals("{\"result\":{\"content\":[{\"type\":\"text\",\"text\":\"Hello\"}]}}", jsonResponse, false);

        // Strict Mode tests
        String expectedResponseString = """
                        {"id":\"2\","jsonrpc":"2.0","result":{"content":[{"type":"text","text":"Hello"}], "isError": false}}
                        """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    public void testSpecialCharactersInToolArgNameVariant2() throws Exception {
        String request = """
                          {
                          "jsonrpc": "2.0",
                          "id": "2",
                          "method": "tools/call",
                          "params": {
                            "name": "specialCharactersInToolArgNameVariant2",
                            "arguments": {
                              "@arg1!><": "Hello",
                              "@arg2={}": "Hello2"
                            }
                          }
                        }
                        """;

        String response = client.callMCP(request);
        JSONObject jsonResponse = new JSONObject(response);

        // Lenient mode tests
        JSONAssert.assertEquals("{ \"jsonrpc\": \"2.0\", \"id\": \"2\"}", response, false);
        JSONAssert.assertEquals("{\"result\":{\"content\":[{\"type\":\"text\",\"text\":\"Hello\"}]}}", jsonResponse, false);

        // Strict Mode tests
        String expectedResponseString = """
                        {"id":\"2\","jsonrpc":"2.0","result":{"content":[{"type":"text","text":"Hello"}], "isError": false}}
                        """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    public void testReservedWordInToolName() throws Exception {
        String request = """
                          {
                          "jsonrpc": "2.0",
                          "id": "2",
                          "method": "tools/call",
                          "params": {
                            "name": "package",
                            "arguments": {
                              "arg1": "Hello"
                            }
                          }
                        }
                        """;

        String response = client.callMCP(request);
        JSONObject jsonResponse = new JSONObject(response);

        // Lenient mode tests
        JSONAssert.assertEquals("{ \"jsonrpc\": \"2.0\", \"id\": \"2\"}", response, false);
        JSONAssert.assertEquals("{\"result\":{\"content\":[{\"type\":\"text\",\"text\":\"Hello\"}]}}", jsonResponse, false);

        // Strict Mode tests
        String expectedResponseString = """
                        {"id":\"2\","jsonrpc":"2.0","result":{"content":[{"type":"text","text":"Hello"}], "isError": false}}
                        """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    public void testReservedNamesInToolArgName() throws Exception {
        String request = """
                          {
                          "jsonrpc": "2.0",
                          "id": "2",
                          "method": "tools/call",
                          "params": {
                            "name": "package",
                            "arguments": {
                              "package": "Hello",
                              "int": "Hello2"
                            }
                          }
                        }
                        """;

        String response = client.callMCP(request);
        JSONObject jsonResponse = new JSONObject(response);

        // Lenient mode tests
        JSONAssert.assertEquals("{ \"jsonrpc\": \"2.0\", \"id\": \"2\"}", response, false);
        JSONAssert.assertEquals("{\"result\":{\"content\":[{\"type\":\"text\",\"text\":\"Hello\"}]}}", jsonResponse, false);

        // Strict Mode tests
        String expectedResponseString = """
                        {"id":\"2\","jsonrpc":"2.0","result":{"content":[{"type":"text","text":"Hello"}], "isError": false}}
                        """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    public void testReservedNamesInToolArgNameVariant() throws Exception {
        String request = """
                          {
                          "jsonrpc": "2.0",
                          "id": "2",
                          "method": "tools/call",
                          "params": {
                            "name": "package",
                            "arguments": {
                              "class": "Hello",
                              "void": "Hello2"
                            }
                          }
                        }
                        """;

        String response = client.callMCP(request);
        JSONObject jsonResponse = new JSONObject(response);

        // Lenient mode tests
        JSONAssert.assertEquals("{ \"jsonrpc\": \"2.0\", \"id\": \"2\"}", response, false);
        JSONAssert.assertEquals("{\"result\":{\"content\":[{\"type\":\"text\",\"text\":\"Hello\"}]}}", jsonResponse, false);

        // Strict Mode tests
        String expectedResponseString = """
                        {"id":\"2\","jsonrpc":"2.0","result":{"content":[{"type":"text","text":"Hello"}], "isError": false}}
                        """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Test
    public void testCheckPersonCall() throws Exception {
//        Based on the following context
//        Address companyAddress = new Address(100, new Street("Hursley Park Rd", "Private Property"), "so21 2er", "inside hursley park");
//        Person companyPerson = new Person("Shareholder 1", companyAddress, null);
//        List<Person> companyList = new ArrayList<>();
//        companyList.add(companyPerson);
//        Map<String, Person> companyMap = new HashMap<>();
//        companyMap.put("1", companyPerson);
//        Company company = new Company("IBM", companyAddress, companyList, companyMap);
//        Address personAddress = new Address(002, new Street("Poles Ln", "n/a"), "so21 2rt", "near hursley park");
//        Person person = new Person("John Smith", personAddress, company);

        String request = """
                          {
                          "jsonrpc": "2.0",
                          "id": 2,
                          "method": "tools/call",
                          "params": {
                            "name": "checkPerson",
                            "arguments": {
                                            "person":{
                                            "address": {
                                                "number": 2,
                                                "postcode": "so21 2rt",
                                                "street": {
                                                    "streetName": "Poles Ln",
                                                    "roadType": "n/a"
                                                }
                                            },
                                            "company": {
                                                "address": {
                                                    "number": 100,
                                                    "postcode": "so21 2er",
                                                    "street": {
                                                        "streetName": "Hursley Park Rd",
                                                        "roadType": "Private Property"
                                                    }
                                                },
                                                "name": "IBM",
                                                "shareholder": [
                                                    {
                                                        "address": {
                                                            "number": 100,
                                                            "postcode": "so21 2er",
                                                            "street": {
                                                                "streetName": "Hursley Park Rd",
                                                                "roadType": "Private Property"
                                                            }
                                                        },
                                                        "fullname": "Shareholder 1"
                                                    }
                                                ],
                                                "shareholderRegistry": {
                                                    "1": {
                                                        "address": {
                                                            "number": 100,
                                                            "postcode": "so21 2er",
                                                            "street": {
                                                                "streetName": "Hursley Park Rd",
                                                                "roadType": "Private Property"
                                                            }
                                                        },
                                                        "fullname": "Shareholder 1"
                                                    }
                                                }
                                            },
                                            "fullname": "John Smith"
                                        },
                                        "company": {
                                                "address": {
                                                    "number": 100,
                                                    "postcode": "so21 2er",
                                                    "street": {
                                                        "streetName": "Hursley Park Rd",
                                                        "roadType": "Private Property"
                                                    }
                                                },
                                                "name": "IBM",
                                                "shareholder": [
                                                    {
                                                        "address": {
                                                            "number": 100,
                                                            "postcode": "so21 2er",
                                                            "street": {
                                                                "streetName": "Hursley Park Rd",
                                                                "roadType": "Private Property"
                                                            }
                                                        },
                                                        "fullname": "Shareholder 1"
                                                    }
                                                ],
                                                "shareholderRegistry": {
                                                    "1": {
                                                        "address": {
                                                            "number": 100,
                                                            "postcode": "so21 2er",
                                                            "street": {
                                                                "streetName": "Hursley Park Rd",
                                                                "roadType": "Private Property"
                                                            }
                                                        },
                                                        "fullname": "Shareholder 1"
                                                    }
                                                }
                                            }
                                    }
                          }
                        }
                        """;

        String response = client.callMCP(request);
        JSONObject jsonResponse = new JSONObject(response);
        // Strict Mode tests
        String expectedResponseString = """
                        {"result":{"isError":false,"content":[{"text":"true","type":"text"}]},"id":2,"jsonrpc":"2.0"}
                        """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Test
    public void testAddPersonToListWithOptionalField() throws Exception {
//        Based on the following context
//        Address companyAddress = new Address(100, new Street("Hursley Park Rd", "Private Property"), "so21 2er", "inside hursley park");
//        Person companyPerson = new Person("Shareholder 1", companyAddress, null);
//        List<Person> companyList = new ArrayList<>();
//        companyList.add(companyPerson);
//        Map<String, Person> companyMap = new HashMap<>();
//        companyMap.put("1", companyPerson);
//        Company company = new Company("IBM", companyAddress, companyList, companyMap);
//        Address personAddress = new Address(002, new Street("Poles Ln", "n/a"), "so21 2rt", "near hursley park");
//        Person person = new Person("John Smith", personAddress, company);

        String request = """
                          {
                          "jsonrpc": "2.0",
                          "id": 2,
                          "method": "tools/call",
                          "params": {
                            "name": "addPersonToList",
                            "arguments": {
                                            "person":{
                                            "address": {
                                                "number": 2,
                                                "postcode": "so21 2rt",
                                                "street": {
                                                    "streetName": "Poles Ln",
                                                    "roadType": "n/a"
                                                }
                                            },
                                            "company": {
                                                "address": {
                                                    "number": 100,
                                                    "postcode": "so21 2er",
                                                    "street": {
                                                        "streetName": "Hursley Park Rd",
                                                        "roadType": "Private Property"
                                                    }
                                                },
                                                "name": "IBM",
                                                "shareholder": [
                                                    {
                                                        "address": {
                                                            "number": 100,
                                                            "postcode": "so21 2er",
                                                            "street": {
                                                                "streetName": "Hursley Park Rd",
                                                                "roadType": "Private Property"
                                                            }
                                                        },
                                                        "fullname": "Shareholder 1"
                                                    }
                                                ]
                                            },
                                            "fullname": "John Smith"
                                        },
                                        "employeeList": [
                                                    {
                                                        "address": {
                                                            "number": 2,
                                                            "postcode": "so21 2rt",
                                                            "street": {
                                                                "streetName": "Poles Ln",
                                                                "roadType": "n/a"
                                                            }
                                                        },
                                                        "company": {
                                                            "address": {
                                                                "number": 100,
                                                                "postcode": "so21 2er",
                                                                "street": {
                                                                    "streetName": "Hursley Park Rd",
                                                                    "roadType": "Private Property"
                                                                }
                                                            },
                                                            "name": "IBM",
                                                            "shareholder": [
                                                                {
                                                                    "address": {
                                                                        "number": 100,
                                                                        "postcode": "so21 2er",
                                                                        "street": {
                                                                            "streetName": "Hursley Park Rd",
                                                                            "roadType": "Private Property"
                                                                        }
                                                                    },
                                                                    "fullname": "Shareholder 1"
                                                                }
                                                            ],
                                                            "shareholderRegistry": {
                                                                "1": {
                                                                    "address": {
                                                                        "number": 100,
                                                                        "postcode": "so21 2er",
                                                                        "street": {
                                                                            "streetName": "Hursley Park Rd",
                                                                            "roadType": "Private Property"
                                                                        }
                                                                    },
                                                                    "fullname": "Shareholder 1"
                                                                }
                                                            }
                                                        },
                                                        "fullname": "John Smith"
                                                    },
                                                    {
                                                        "address": {
                                                            "number": 100,
                                                            "postcode": "so21 2er",
                                                            "street": {
                                                                "streetName": "Hursley Park Rd",
                                                                "roadType": "Private Property"
                                                            }
                                                        },
                                                        "fullname": "Shareholder 1"
                                                    }
                                                ]
                                    }
                          }
                        }
                        """;

        String response = client.callMCP(request);
        JSONObject jsonResponse = new JSONObject(response);
        // Strict Mode tests
        String expectedResponseString = """
                                                                {
                            "result": {
                                "isError": false,
                                "structuredContent": [
                                    {
                                        "address": {
                                            "number": 2,
                                            "street": {
                                                "streetName": "Poles Ln",
                                                "roadType": "n/a"
                                            },
                                            "postcode": "so21 2rt"
                                        },
                                        "company": {
                                            "address": {
                                                "number": 100,
                                                "street": {
                                                    "streetName": "Hursley Park Rd",
                                                    "roadType": "Private Property"
                                                },
                                                "postcode": "so21 2er"
                                            },
                                            "shareholderRegistry": {
                                                "1": {
                                                    "address": {
                                                        "number": 100,
                                                        "street": {
                                                            "streetName": "Hursley Park Rd",
                                                            "roadType": "Private Property"
                                                        },
                                                        "postcode": "so21 2er"
                                                    },
                                                    "fullname": "Shareholder 1"
                                                }
                                            },
                                            "name": "IBM"
                                        },
                                        "fullname": "John Smith"
                                    },
                                    {
                                        "address": {
                                            "number": 100,
                                            "street": {
                                                "streetName": "Hursley Park Rd",
                                                "roadType": "Private Property"
                                            },
                                            "postcode": "so21 2er"
                                        },
                                        "fullname": "Shareholder 1"
                                    },
                                    {
                                        "address": {
                                            "number": 2,
                                            "street": {
                                                "streetName": "Poles Ln",
                                                "roadType": "n/a"
                                            },
                                            "postcode": "so21 2rt"
                                        },
                                        "company": {
                                            "address": {
                                                "number": 100,
                                                "street": {
                                                    "streetName": "Hursley Park Rd",
                                                    "roadType": "Private Property"
                                                },
                                                "postcode": "so21 2er"
                                            },
                                            "name": "IBM"
                                        },
                                        "fullname": "John Smith"
                                    }
                                ],
                                "content": [
                                    {
                                        "text": "[{\\\"address\\\":{\\\"number\\\":2,\\\"postcode\\\":\\\"so21 2rt\\\",\\\"street\\\":{\\\"streetName\\\":\\\"Poles Ln\\\",\\\"roadType\\\":\\\"n/a\\\"}},\\\"company\\\":{\\\"address\\\":{\\\"number\\\":100,\\\"postcode\\\":\\\"so21 2er\\\",\\\"street\\\":{\\\"streetName\\\":\\\"Hursley Park Rd\\\",\\\"roadType\\\":\\\"Private Property\\\"}},\\\"name\\\":\\\"IBM\\\",\\\"shareholderRegistry\\\":{\\\"1\\\":{\\\"address\\\":{\\\"number\\\":100,\\\"postcode\\\":\\\"so21 2er\\\",\\\"street\\\":{\\\"streetName\\\":\\\"Hursley Park Rd\\\",\\\"roadType\\\":\\\"Private Property\\\"}},\\\"fullname\\\":\\\"Shareholder 1\\\"}}},\\\"fullname\\\":\\\"John Smith\\\"},{\\\"address\\\":{\\\"number\\\":100,\\\"postcode\\\":\\\"so21 2er\\\",\\\"street\\\":{\\\"streetName\\\":\\\"Hursley Park Rd\\\",\\\"roadType\\\":\\\"Private Property\\\"}},\\\"fullname\\\":\\\"Shareholder 1\\\"},{\\\"address\\\":{\\\"number\\\":2,\\\"postcode\\\":\\\"so21 2rt\\\",\\\"street\\\":{\\\"streetName\\\":\\\"Poles Ln\\\",\\\"roadType\\\":\\\"n/a\\\"}},\\\"company\\\":{\\\"address\\\":{\\\"number\\\":100,\\\"postcode\\\":\\\"so21 2er\\\",\\\"street\\\":{\\\"streetName\\\":\\\"Hursley Park Rd\\\",\\\"roadType\\\":\\\"Private Property\\\"}},\\\"name\\\":\\\"IBM\\\"},\\\"fullname\\\":\\\"John Smith\\\"}]",
                                        "type": "text"
                                    }
                                ]
                            },
                            "id": 2,
                            "jsonrpc": "2.0"
                        }
                                                                                                """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Test
    public void testAddPersonToListToolResponse() throws Exception {
//        Based on the following context
//        Address companyAddress = new Address(100, new Street("Hursley Park Rd", "Private Property"), "so21 2er", "inside hursley park");
//        Person companyPerson = new Person("Shareholder 1", companyAddress, null);
//        List<Person> companyList = new ArrayList<>();
//        companyList.add(companyPerson);
//        Map<String, Person> companyMap = new HashMap<>();
//        companyMap.put("1", companyPerson);
//        Company company = new Company("IBM", companyAddress, companyList, companyMap);
//        Address personAddress = new Address(002, new Street("Poles Ln", "n/a"), "so21 2rt", "near hursley park");
//        Person person = new Person("John Smith", personAddress, company);

        String request = """
                          {
                          "jsonrpc": "2.0",
                          "id": 2,
                          "method": "tools/call",
                          "params": {
                            "name": "addPersonToListToolResponse",
                            "arguments": {
                                            "person":{
                                            "address": {
                                                "number": 2,
                                                "postcode": "so21 2rt",
                                                "street": {
                                                    "streetName": "Poles Ln",
                                                    "roadType": "n/a"
                                                }
                                            },
                                            "company": {
                                                "address": {
                                                    "number": 100,
                                                    "postcode": "so21 2er",
                                                    "street": {
                                                        "streetName": "Hursley Park Rd",
                                                        "roadType": "Private Property"
                                                    }
                                                },
                                                "name": "IBM",
                                                "shareholder": [
                                                    {
                                                        "address": {
                                                            "number": 100,
                                                            "postcode": "so21 2er",
                                                            "street": {
                                                                "streetName": "Hursley Park Rd",
                                                                "roadType": "Private Property"
                                                            }
                                                        },
                                                        "fullname": "Shareholder 1"
                                                    }
                                                ]
                                            },
                                            "fullname": "John Smith"
                                        },
                                        "employeeList": [
                                                    {
                                                        "address": {
                                                            "number": 2,
                                                            "postcode": "so21 2rt",
                                                            "street": {
                                                                "streetName": "Poles Ln",
                                                                "roadType": "n/a"
                                                            }
                                                        },
                                                        "company": {
                                                            "address": {
                                                                "number": 100,
                                                                "postcode": "so21 2er",
                                                                "street": {
                                                                    "streetName": "Hursley Park Rd",
                                                                    "roadType": "Private Property"
                                                                }
                                                            },
                                                            "name": "IBM",
                                                            "shareholder": [
                                                                {
                                                                    "address": {
                                                                        "number": 100,
                                                                        "postcode": "so21 2er",
                                                                        "street": {
                                                                            "streetName": "Hursley Park Rd",
                                                                            "roadType": "Private Property"
                                                                        }
                                                                    },
                                                                    "fullname": "Shareholder 1"
                                                                }
                                                            ],
                                                            "shareholderRegistry": {
                                                                "1": {
                                                                    "address": {
                                                                        "number": 100,
                                                                        "postcode": "so21 2er",
                                                                        "street": {
                                                                            "streetName": "Hursley Park Rd",
                                                                            "roadType": "Private Property"
                                                                        }
                                                                    },
                                                                    "fullname": "Shareholder 1"
                                                                }
                                                            }
                                                        },
                                                        "fullname": "John Smith"
                                                    },
                                                    {
                                                        "address": {
                                                            "number": 100,
                                                            "postcode": "so21 2er",
                                                            "street": {
                                                                "streetName": "Hursley Park Rd",
                                                                "roadType": "Private Property"
                                                            }
                                                        },
                                                        "fullname": "Shareholder 1"
                                                    }
                                                ]
                                    }
                          }
                        }
                        """;

        String response = client.callMCP(request);
        JSONObject jsonResponse = new JSONObject(response);
        // Strict Mode tests
        String expectedResponseString = """
                                                                {
                            "result": {
                                "isError": false,
                                "structuredContent": [
                                    {
                                        "address": {
                                            "number": 2,
                                            "street": {
                                                "streetName": "Poles Ln",
                                                "roadType": "n/a"
                                            },
                                            "postcode": "so21 2rt"
                                        },
                                        "company": {
                                            "address": {
                                                "number": 100,
                                                "street": {
                                                    "streetName": "Hursley Park Rd",
                                                    "roadType": "Private Property"
                                                },
                                                "postcode": "so21 2er"
                                            },
                                            "shareholderRegistry": {
                                                "1": {
                                                    "address": {
                                                        "number": 100,
                                                        "street": {
                                                            "streetName": "Hursley Park Rd",
                                                            "roadType": "Private Property"
                                                        },
                                                        "postcode": "so21 2er"
                                                    },
                                                    "fullname": "Shareholder 1"
                                                }
                                            },
                                            "name": "IBM"
                                        },
                                        "fullname": "John Smith"
                                    },
                                    {
                                        "address": {
                                            "number": 100,
                                            "street": {
                                                "streetName": "Hursley Park Rd",
                                                "roadType": "Private Property"
                                            },
                                            "postcode": "so21 2er"
                                        },
                                        "fullname": "Shareholder 1"
                                    },
                                    {
                                        "address": {
                                            "number": 2,
                                            "street": {
                                                "streetName": "Poles Ln",
                                                "roadType": "n/a"
                                            },
                                            "postcode": "so21 2rt"
                                        },
                                        "company": {
                                            "address": {
                                                "number": 100,
                                                "street": {
                                                    "streetName": "Hursley Park Rd",
                                                    "roadType": "Private Property"
                                                },
                                                "postcode": "so21 2er"
                                            },
                                            "name": "IBM"
                                        },
                                        "fullname": "John Smith"
                                    }
                                ],
                                "content": [
                                    {
                                        "text": "[{\\\"address\\\":{\\\"number\\\":2,\\\"postcode\\\":\\\"so21 2rt\\\",\\\"street\\\":{\\\"streetName\\\":\\\"Poles Ln\\\",\\\"roadType\\\":\\\"n/a\\\"}},\\\"company\\\":{\\\"address\\\":{\\\"number\\\":100,\\\"postcode\\\":\\\"so21 2er\\\",\\\"street\\\":{\\\"streetName\\\":\\\"Hursley Park Rd\\\",\\\"roadType\\\":\\\"Private Property\\\"}},\\\"name\\\":\\\"IBM\\\",\\\"shareholderRegistry\\\":{\\\"1\\\":{\\\"address\\\":{\\\"number\\\":100,\\\"postcode\\\":\\\"so21 2er\\\",\\\"street\\\":{\\\"streetName\\\":\\\"Hursley Park Rd\\\",\\\"roadType\\\":\\\"Private Property\\\"}},\\\"fullname\\\":\\\"Shareholder 1\\\"}}},\\\"fullname\\\":\\\"John Smith\\\"},{\\\"address\\\":{\\\"number\\\":100,\\\"postcode\\\":\\\"so21 2er\\\",\\\"street\\\":{\\\"streetName\\\":\\\"Hursley Park Rd\\\",\\\"roadType\\\":\\\"Private Property\\\"}},\\\"fullname\\\":\\\"Shareholder 1\\\"},{\\\"address\\\":{\\\"number\\\":2,\\\"postcode\\\":\\\"so21 2rt\\\",\\\"street\\\":{\\\"streetName\\\":\\\"Poles Ln\\\",\\\"roadType\\\":\\\"n/a\\\"}},\\\"company\\\":{\\\"address\\\":{\\\"number\\\":100,\\\"postcode\\\":\\\"so21 2er\\\",\\\"street\\\":{\\\"streetName\\\":\\\"Hursley Park Rd\\\",\\\"roadType\\\":\\\"Private Property\\\"}},\\\"name\\\":\\\"IBM\\\"},\\\"fullname\\\":\\\"John Smith\\\"}]",
                                        "type": "text"
                                    }
                                ],
                                "_meta":{
                                        "api.ibmtest.org/location": "Hursley",
                                        "api.libertytest.org/person": {
                                            "address": {
                                                "number": 2,
                                                "postcode": "so21 2rt",
                                                "street": {
                                                    "streetName": "Poles Ln",
                                                    "roadType": "n/a"
                                                }
                                            },
                                            "company": {
                                                "address": {
                                                    "number": 100,
                                                    "postcode": "so21 2er",
                                                    "street": {
                                                        "streetName": "Hursley Park Rd",
                                                        "roadType": "Private Property"
                                                    }
                                                },
                                                "name": "IBM"
                                            },
                                            "fullname": "John Smith"
                                        },
                                        "timestamp": 1762860699
                                    }
                            },
                            "id": 2,
                            "jsonrpc": "2.0"
                        }
                                                                                                """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Test
    public void testAddPersonToListToolResponseWithMetaRequest() throws Exception {
//        Based on the following context
//        Address companyAddress = new Address(100, new Street("Hursley Park Rd", "Private Property"), "so21 2er", "inside hursley park");
//        Person companyPerson = new Person("Shareholder 1", companyAddress, null);
//        List<Person> companyList = new ArrayList<>();
//        companyList.add(companyPerson);
//        Map<String, Person> companyMap = new HashMap<>();
//        companyMap.put("1", companyPerson);
//        Company company = new Company("IBM", companyAddress, companyList, companyMap);
//        Address personAddress = new Address(002, new Street("Poles Ln", "n/a"), "so21 2rt", "near hursley park");
//        Person person = new Person("John Smith", personAddress, company);

        String request = """
                          {
                          "jsonrpc": "2.0",
                          "id": 2,
                          "method": "tools/call",
                          "params": {
                            "_meta":{
                                        "api.ibmtest.org/location": "Hursley",
                                        "api.libertytest.org/person": {
                                            "address": {
                                                "number": 2,
                                                "postcode": "so21 2rt",
                                                "street": {
                                                    "streetName": "Poles Ln",
                                                    "roadType": "n/a"
                                                }
                                            },
                                            "company": {
                                                "address": {
                                                    "number": 100,
                                                    "postcode": "so21 2er",
                                                    "street": {
                                                        "streetName": "Hursley Park Rd",
                                                        "roadType": "Private Property"
                                                    }
                                                },
                                                "name": "IBM"
                                            },
                                            "fullname": "John Smith"
                                        },
                                        "timestamp": 1762860699
                                    },
                            "name": "addPersonToListToolResponseWithMetaRequest",
                            "arguments": {
                                            "person":{
                                            "address": {
                                                "number": 2,
                                                "postcode": "so21 2rt",
                                                "street": {
                                                    "streetName": "Poles Ln",
                                                    "roadType": "n/a"
                                                }
                                            },
                                            "company": {
                                                "address": {
                                                    "number": 100,
                                                    "postcode": "so21 2er",
                                                    "street": {
                                                        "streetName": "Hursley Park Rd",
                                                        "roadType": "Private Property"
                                                    }
                                                },
                                                "name": "IBM",
                                                "shareholder": [
                                                    {
                                                        "address": {
                                                            "number": 100,
                                                            "postcode": "so21 2er",
                                                            "street": {
                                                                "streetName": "Hursley Park Rd",
                                                                "roadType": "Private Property"
                                                            }
                                                        },
                                                        "fullname": "Shareholder 1"
                                                    }
                                                ]
                                            },
                                            "fullname": "John Smith"
                                        },
                                        "employeeList": [
                                                    {
                                                        "address": {
                                                            "number": 2,
                                                            "postcode": "so21 2rt",
                                                            "street": {
                                                                "streetName": "Poles Ln",
                                                                "roadType": "n/a"
                                                            }
                                                        },
                                                        "company": {
                                                            "address": {
                                                                "number": 100,
                                                                "postcode": "so21 2er",
                                                                "street": {
                                                                    "streetName": "Hursley Park Rd",
                                                                    "roadType": "Private Property"
                                                                }
                                                            },
                                                            "name": "IBM",
                                                            "shareholder": [
                                                                {
                                                                    "address": {
                                                                        "number": 100,
                                                                        "postcode": "so21 2er",
                                                                        "street": {
                                                                            "streetName": "Hursley Park Rd",
                                                                            "roadType": "Private Property"
                                                                        }
                                                                    },
                                                                    "fullname": "Shareholder 1"
                                                                }
                                                            ],
                                                            "shareholderRegistry": {
                                                                "1": {
                                                                    "address": {
                                                                        "number": 100,
                                                                        "postcode": "so21 2er",
                                                                        "street": {
                                                                            "streetName": "Hursley Park Rd",
                                                                            "roadType": "Private Property"
                                                                        }
                                                                    },
                                                                    "fullname": "Shareholder 1"
                                                                }
                                                            }
                                                        },
                                                        "fullname": "John Smith"
                                                    },
                                                    {
                                                        "address": {
                                                            "number": 100,
                                                            "postcode": "so21 2er",
                                                            "street": {
                                                                "streetName": "Hursley Park Rd",
                                                                "roadType": "Private Property"
                                                            }
                                                        },
                                                        "fullname": "Shareholder 1"
                                                    }
                                                ]
                                    }
                          }
                        }
                        """;

        String response = client.callMCP(request);
        JSONObject jsonResponse = new JSONObject(response);
        // Strict Mode tests
        String expectedResponseString = """
                                                                {
                            "result": {
                                "isError": false,
                                "structuredContent": [
                                    {
                                        "address": {
                                            "number": 2,
                                            "street": {
                                                "streetName": "Poles Ln",
                                                "roadType": "n/a"
                                            },
                                            "postcode": "so21 2rt"
                                        },
                                        "company": {
                                            "address": {
                                                "number": 100,
                                                "street": {
                                                    "streetName": "Hursley Park Rd",
                                                    "roadType": "Private Property"
                                                },
                                                "postcode": "so21 2er"
                                            },
                                            "shareholderRegistry": {
                                                "1": {
                                                    "address": {
                                                        "number": 100,
                                                        "street": {
                                                            "streetName": "Hursley Park Rd",
                                                            "roadType": "Private Property"
                                                        },
                                                        "postcode": "so21 2er"
                                                    },
                                                    "fullname": "Shareholder 1"
                                                }
                                            },
                                            "name": "IBM"
                                        },
                                        "fullname": "John Smith"
                                    },
                                    {
                                        "address": {
                                            "number": 100,
                                            "street": {
                                                "streetName": "Hursley Park Rd",
                                                "roadType": "Private Property"
                                            },
                                            "postcode": "so21 2er"
                                        },
                                        "fullname": "Shareholder 1"
                                    },
                                    {
                                        "address": {
                                            "number": 2,
                                            "street": {
                                                "streetName": "Poles Ln",
                                                "roadType": "n/a"
                                            },
                                            "postcode": "so21 2rt"
                                        },
                                        "company": {
                                            "address": {
                                                "number": 100,
                                                "street": {
                                                    "streetName": "Hursley Park Rd",
                                                    "roadType": "Private Property"
                                                },
                                                "postcode": "so21 2er"
                                            },
                                            "name": "IBM"
                                        },
                                        "fullname": "John Smith"
                                    }
                                ],
                                "content": [
                                    {
                                        "text": "[{\\\"address\\\":{\\\"number\\\":2,\\\"postcode\\\":\\\"so21 2rt\\\",\\\"street\\\":{\\\"streetName\\\":\\\"Poles Ln\\\",\\\"roadType\\\":\\\"n/a\\\"}},\\\"company\\\":{\\\"address\\\":{\\\"number\\\":100,\\\"postcode\\\":\\\"so21 2er\\\",\\\"street\\\":{\\\"streetName\\\":\\\"Hursley Park Rd\\\",\\\"roadType\\\":\\\"Private Property\\\"}},\\\"name\\\":\\\"IBM\\\",\\\"shareholderRegistry\\\":{\\\"1\\\":{\\\"address\\\":{\\\"number\\\":100,\\\"postcode\\\":\\\"so21 2er\\\",\\\"street\\\":{\\\"streetName\\\":\\\"Hursley Park Rd\\\",\\\"roadType\\\":\\\"Private Property\\\"}},\\\"fullname\\\":\\\"Shareholder 1\\\"}}},\\\"fullname\\\":\\\"John Smith\\\"},{\\\"address\\\":{\\\"number\\\":100,\\\"postcode\\\":\\\"so21 2er\\\",\\\"street\\\":{\\\"streetName\\\":\\\"Hursley Park Rd\\\",\\\"roadType\\\":\\\"Private Property\\\"}},\\\"fullname\\\":\\\"Shareholder 1\\\"},{\\\"address\\\":{\\\"number\\\":2,\\\"postcode\\\":\\\"so21 2rt\\\",\\\"street\\\":{\\\"streetName\\\":\\\"Poles Ln\\\",\\\"roadType\\\":\\\"n/a\\\"}},\\\"company\\\":{\\\"address\\\":{\\\"number\\\":100,\\\"postcode\\\":\\\"so21 2er\\\",\\\"street\\\":{\\\"streetName\\\":\\\"Hursley Park Rd\\\",\\\"roadType\\\":\\\"Private Property\\\"}},\\\"name\\\":\\\"IBM\\\"},\\\"fullname\\\":\\\"John Smith\\\"}]",
                                        "type": "text"
                                    }
                                ],
                                "_meta":{
                                        "api.ibmtest.org/location": "Hursley",
                                        "api.libertytest.org/person": {
                                            "address": {
                                                "number": 2,
                                                "postcode": "so21 2rt",
                                                "street": {
                                                    "streetName": "Poles Ln",
                                                    "roadType": "n/a"
                                                }
                                            },
                                            "company": {
                                                "address": {
                                                    "number": 100,
                                                    "postcode": "so21 2er",
                                                    "street": {
                                                        "streetName": "Hursley Park Rd",
                                                        "roadType": "Private Property"
                                                    }
                                                },
                                                "name": "IBM"
                                            },
                                            "fullname": "John Smith"
                                        },
                                        "timestamp": 1762860699
                                    }
                            },
                            "id": 2,
                            "jsonrpc": "2.0"
                        }
                                                                                                """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Test
    public void simpleMetaRequest() throws Exception {
        String request = """
                          {
                          "jsonrpc": "2.0",
                          "id": 2,
                          "method": "tools/call",
                          "params": {
                            "_meta":{
                                        "api.ibmtest.org/location": "Hursley",
                                        "timestamp": 1762860699
                                    },
                            "name": "simpleMetaRequest",
                            "arguments": {
                                            "name": "IBMUser"
                                    }
                          }
                        }
                        """;

        String response = client.callMCP(request);
        JSONObject jsonResponse = new JSONObject(response);
        // Strict Mode tests
        String expectedResponseString = """
                                                                {
                            "result": {
                                "isError": false,
                                "content": [
                                    {
                                        "text": "Hello IBMUser you have called this tool from Hursley at timestamp 1762860699",
                                        "type": "text"
                                    }
                                ],
                            },
                            "id": 2,
                            "jsonrpc": "2.0"
                        }
                                                                                                """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Test
    public void noArgRequest() throws Exception {
        String request = """
                          {
                          "jsonrpc": "2.0",
                          "id": 2,
                          "method": "tools/call",
                          "params": {
                            "_meta":{
                                        "api.ibmtest.org/location": "Hursley",
                                        "timestamp": 1762860699
                                    },
                            "name": "noArgsRequest"
                          }
                        }
                        """;

        String response = client.callMCP(request);
        JSONObject jsonResponse = new JSONObject(response);
        // Strict Mode tests
        String expectedResponseString = """
                                                                {
                            "result": {
                                "isError": false,
                                "content": [
                                    {
                                        "text": "You have called this tool from Hursley at timestamp 1762860699",
                                        "type": "text"
                                    }
                                ],
                            },
                            "id": 2,
                            "jsonrpc": "2.0"
                        }
                                                                                                """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Test
    public void testReusingRequestIdAfterCompletionSucceeds() throws Exception {

        String requestTemplate = """
                        {
                          "jsonrpc": "2.0",
                          "id": "2",
                          "method": "tools/call",
                          "params": {
                            "name": "echo",
                            "arguments": {
                              "input": "Hello"
                            }
                          }
                        }
                        """;

        String expectedResponseString = """
                        {"id":\"2\","jsonrpc":"2.0","result":{"content":[{"type":"text","text":"Hello"}], "isError": false}}
                        """;

        // First request call
        String response = client.callMCP(requestTemplate);
        JSONAssert.assertEquals(expectedResponseString, response, true);

        // Second request - same ID
        String duplicateResponse = client.callMCP(requestTemplate);

        JSONAssert.assertEquals(expectedResponseString, duplicateResponse, true);
    }

    @Test
    @Mode(FULL)
    public void testSessionIdNotTraced() throws Exception {
        String sessionId = client.getSessionId();
        int visibleSessionIdLength = 6;
        String redactedSessionId = sessionId.substring(0, visibleSessionIdLength) + "*".repeat(sessionId.length() - visibleSessionIdLength);

        assertNotNull("Expected session ID from MCP initialization", sessionId);
        String request = """
                        {
                          "jsonrpc": "2.0",
                          "id": 1,
                          "method": "tools/call",
                          "params": {
                            "name": "textContentTool",
                            "arguments": {
                              "input": "hello"
                            }
                          }
                        }
                        """;
        client.callMCP(request);

        assertNotNull(server.waitForStringInTrace(Pattern.quote(redactedSessionId)));
        assertNull(server.waitForStringInTrace(sessionId, 3000)); // wait 3 seconds to confirm full session Id not found in trace
    }

    @Test
    public void testDeleteSessionRemovesSession() throws Exception {
        String sessionId = client.getSessionId();
        assertNotNull("Expected session ID from MCP initialization", sessionId);

        client.deleteSession();

        String request = """
                        {
                          "jsonrpc": "2.0",
                          "id": "1",
                          "method": "tools/call",
                          "params": {
                            "name": "echo",
                            "arguments": {
                              "input": "hello"
                            }
                          }
                        }
                        """;

        String response = new HttpRequest(server, "/toolTest/mcp")
                                                                  .requestProp(ACCEPT, VALUE_ACCEPT_DEFAULT)
                                                                  .requestProp(MCP_PROTOCOL_VERSION, VALUE_MCP_PROTOCOL_VERSION)
                                                                  .requestProp(MCP_SESSION_ID, sessionId)
                                                                  .jsonBody(request)
                                                                  .method("POST")
                                                                  .expectCode(404)
                                                                  .run(String.class);

        assertTrue(response.contains("Invalid or Expired Session Id"));
    }

    @Test
    public void testNonLatinCharacters() throws Exception {
        String request = """
                        {
                          "jsonrpc": "2.0",
                          "id": "1",
                          "method": "tools/call",
                          "params": {
                            "name": "get-user-jp",
                            "arguments": {
                              "userid": "ユーザー1"
                            }
                          }
                        }
                        """;

        String response = client.callMCP(request);
        String expected = """
                          {
                            "jsonrpc": "2.0",
                            "id": "1",
                            "result": {
                                "isError": false,
                                "content": [
                                    {
                                        "text": "ID: ユーザー1, Name: 仮名, role: user",
                                        "type": "text"
                                    }
                                ],
                            }
                        }
                        """;

        JSONAssert.assertEquals(expected, response, JSONCompareMode.STRICT);
    }

}