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
import static componenttest.custom.junit.runner.Mode.TestMode.FULL;
import static componenttest.rules.repeater.EERepeatActions.EE10;
import static componenttest.rules.repeater.EERepeatActions.EE11;
import static io.openliberty.mcp.internal.fat.utils.JsonRpcErrorCode.INTERNAL_ERROR;
import static io.openliberty.mcp.internal.fat.utils.JsonRpcErrorCode.INVALID_PARAMS;
import static io.openliberty.mcp.internal.fat.utils.JsonRpcErrorCode.INVALID_REQUEST;
import static io.openliberty.mcp.internal.fat.utils.JsonRpcErrorCode.METHOD_NOT_FOUND;
import static io.openliberty.mcp.internal.fat.utils.JsonRpcErrorCode.PARSE_ERROR;
import static io.openliberty.mcp.internal.fat.utils.JsonRpcResponseMatcher.jsonRpcResponse;
import static io.openliberty.mcp.internal.fat.utils.TestConstants.ACCEPT;
import static io.openliberty.mcp.internal.fat.utils.TestConstants.MCP_PROTOCOL_VERSION;
import static io.openliberty.mcp.internal.fat.utils.TestConstants.MCP_SESSION_ID;
import static io.openliberty.mcp.internal.fat.utils.TestConstants.VALUE_ACCEPT_DEFAULT;
import static io.openliberty.mcp.internal.fat.utils.TestConstants.VALUE_MCP_PROTOCOL_VERSION;
import static io.openliberty.mcp.internal.fat.utils.ToolResponseMatcher.toolResponse;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
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
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.custom.junit.runner.TestModeFilter;
import componenttest.rules.repeater.EERepeatActions;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import componenttest.topology.utils.HttpRequest;
import io.openliberty.mcp.internal.fat.tool.basicToolApp.BasicTools;
import io.openliberty.mcp.internal.fat.tool.metaKeyApp.MetaKeyValidationTestServlet;
import io.openliberty.mcp.internal.fat.tool.progressApp.ProgressTools;
import io.openliberty.mcp.internal.fat.utils.McpClient;
import io.openliberty.mcp.internal.fat.utils.TestConstants;
import io.openliberty.mcp.internal.fat.utils.ToolResponse;

/**
 *
 */
@RunWith(FATRunner.class)
public class ToolTest extends FATServletClient {

    @TestServlet(contextRoot = "metaKeyTest", servlet = MetaKeyValidationTestServlet.class)
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
    public McpClient client = McpClient.builder(server, "/toolTest").build();

    @Rule
    public McpClient progressClient = McpClient.builder(server, "/progressTest").build();

    @BeforeClass
    public static void setup() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "toolTest.war")
                                   .addPackage(BasicTools.class.getPackage())
                                   .addClass(TestConstants.class);

        WebArchive progressWar = ShrinkWrap.create(WebArchive.class, "progressTest.war")
                                           .addPackage(ProgressTools.class.getPackage());

        WebArchive metaKeyWar = ShrinkWrap.create(WebArchive.class, "metaKeyTest.war")
                                          .addPackage(MetaKeyValidationTestServlet.class.getPackage());

        ShrinkHelper.exportDropinAppToServer(server, war, SERVER_ONLY);
        ShrinkHelper.exportDropinAppToServer(server, progressWar, SERVER_ONLY);
        ShrinkHelper.exportDropinAppToServer(server, metaKeyWar, SERVER_ONLY);

        server.startServer();

        assertNotNull(server.waitForStringInLog("MCP server endpoint: .*/mcp$")); // regex matches string that ends with /mcp e.g. "MCP server endpoint: http://macbookpro.home:8010/toolTest/mcp"

    }

    @AfterClass
    public static void teardown() throws Exception {
        server.stopServer(
                          "CWMCM0010E", //The JSON-RPC request is not valid JSON.
                          "CWMCM0012E", // The requested JSON-RPC method is not found.
                          "CWMCM0010E", //  Tool method threw an unexpected exception
                          "CWMCM0022E" // Invalid Cursor provided
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

        assertThat(client.call(request),
                   jsonRpcResponse().withErrorCode(INVALID_REQUEST)
                                    .withErrorMessage("Invalid request")
                                    .withErrorDataItems("The jsonrpc field must be present. Only JSONRPC 2.0 is currently supported.",
                                                        "The method field is empty.",
                                                        "The id type is not an acceptable type. The id must be a string or integer."));
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

        assertThat(client.call(request),
                   jsonRpcResponse().withErrorCode(PARSE_ERROR)
                                    .withErrorMessage("Parse error")
                                    .withErrorDataItems("Invalid token=CURLYCLOSE at (line no=1, column no=3, offset=2). Expected tokens are: [CURLYOPEN, SQUAREOPEN, STRING, NUMBER, TRUE, FALSE, NULL]"));
    }

    @Test
    public void testEchoWithInvalidParamsException() throws Exception {
        assertThat(client.callTool("echo"),
                   toolResponse().withError("The method expected the following arguments but did not receive them: [input]."));
    }

    @Test
    public void testEchoWithInvalidParamsArgumentMismatchException() throws Exception {
        assertThat(client.callTool("echo", Map.of("other", "Hello")),
                   toolResponse().withError("The following arguments were passed but were not found in the method: [other]. The method expected the following arguments but did not receive them: [input]."));
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

        assertThat(client.call(request),
                   jsonRpcResponse().withErrorCode(METHOD_NOT_FOUND)
                                    .withErrorMessage("Method not found")
                                    .withErrorDataItems("call/tools not found"));
    }

    @Test
    public void testToolReturnsListOfContent() throws Exception {
        assertThat(client.callTool("textContentTool", Map.of("input", "hello")),
                   toolResponse().withContent(new JSONObject("""
                                   { "type": "text", "text": "Echo: hello" }
                                   """)));
    }

    @Test
    public void testToolReturnsListOfContentWithAnnotations() throws Exception {
        assertThat(client.callTool("textContentToolWithContentAnnotation", Map.of("input", "hello")),
                   toolResponse().withContent(new JSONObject("""
                                   {
                                     "annotations": {
                                       "audience": ["assistant"],
                                       "lastModified": "2025-08-26T08:40:00Z",
                                       "priority": 0.5
                                     },
                                     "text": "Echo: hello",
                                     "type": "text"
                                   }
                                   """)));
    }

    @Test
    public void testToolReturnsImageContentList() throws Exception {
        assertThat(client.callTool("imageContentTool", Map.of("imageData", "aGkJ")),
                   toolResponse().withContent(new JSONObject("""
                                   {
                                     "data": "aGkJ",
                                     "mimeType": "image/png",
                                     "type": "image"
                                   }
                                   """)));
    }

    @Test
    public void testToolReturnsImageContentListWithAnnotations() throws Exception {
        assertThat(client.callTool("imageContentToolWithContentAnnotation", Map.of("imageData", "aGkK")),
                   toolResponse().withContent(new JSONObject("""
                                   {
                                     "annotations": {
                                       "audience": ["user"],
                                       "lastModified": "2025-08-26T08:40:00Z",
                                       "priority": 0.8
                                     },
                                     "data": "aGkK",
                                     "mimeType": "image/png",
                                     "type": "image"
                                   }
                                   """)));
    }

    @Test
    public void testToolReturnsAudioContentList() throws Exception {
        assertThat(client.callTool("audioContentTool", Map.of("audioData", "aGkK")),
                   toolResponse().withContent(new JSONObject("""
                                   {
                                     "data": "aGkK",
                                     "mimeType": "audio/mpeg",
                                     "type": "audio"
                                   }
                                   """)));
    }

    @Test
    public void testToolReturnsAudioContentListWithAnnotations() throws Exception {
        assertThat(client.callTool("audioContentToolWithContentAnnotation", Map.of("audioData", "aGkK")),
                   toolResponse().withContent(new JSONObject("""
                                   {
                                     "annotations": {
                                       "audience": ["assistant"],
                                       "lastModified": "2025-08-26T08:40:00Z",
                                       "priority": 0.3
                                     },
                                     "data": "aGkK",
                                     "mimeType": "audio/mpeg",
                                     "type": "audio"
                                   }
                                   """)));
    }

    @Test
    public void testMixedContentTool() throws Exception {
        // Note: base64 encoding of literal strings "base64-encoded-image" and "base64-encoded-audio"
        assertThat(client.callTool("mixedContentTool", Map.of("input", "Hello")),
                   toolResponse().withContent(new JSONObject("""
                                   { "text": "Echo: Hello", "type": "text" }
                                   """),
                                              new JSONObject("""
                                                              { "data": "YmFzZTY0LWVuY29kZWQtaW1hZ2U=", "mimeType": "image/png", "type": "image" }
                                                              """),
                                              new JSONObject("""
                                                              { "data": "YmFzZTY0LWVuY29kZWQtYXVkaW8=", "mimeType": "audio/mpeg", "type": "audio" }
                                                              """)));
    }

    @Test
    public void testMixedContentListTool() throws Exception {
        assertThat(client.callTool("mixedContentListTool", Map.of("input", "Hello")),
                   toolResponse().withContent(new JSONObject("""
                                   { "text": "Echo: Hello", "type": "text" }
                                   """),
                                              new JSONObject("""
                                                              { "data": "%s", "mimeType": "image/png", "type": "image" }
                                                              """.formatted(TestConstants.TEST_IMAGE_DATA_64)),
                                              new JSONObject("""
                                                              { "data": "%s", "mimeType": "audio/mpeg", "type": "audio" }
                                                              """.formatted(TestConstants.TEST_AUDIO_DATA_64))));
    }

    @Test
    public void testToolCallWithoutNonRequiredStringArg() throws Exception {
        assertThat(client.callTool("testToolArgStringNotRequired", Map.of()),
                   toolResponse().withTextContent("null"));
    }

    @Test
    public void testToolCallWithoutNonRequiredIntArg() throws Exception {
        assertThat(client.callTool("testToolArgIntNotRequired", Map.of()),
                   toolResponse().withTextContent("0"));
    }

    @Test
    public void testToolCallWithoutNonRequiredArrayArg() throws Exception {
        assertThat(client.callTool("testToolArgArrayNotRequired", Map.of()),
                   toolResponse().withTextContent("null"));
    }

    @Test
    public void testToolCallWithoutNonRequiredObjectArg() throws Exception {
        assertThat(client.callTool("testToolArgObjectNotRequired", Map.of()),
                   toolResponse().withTextContent("null"));
    }

    @Test
    public void testToolCallWithTwoToolArgsWithoutNonRequiredArg() throws Exception {
        assertThat(client.callTool("testMultipleToolArgsOneNotRequired", Map.of("planet", "Earth")),
                   toolResponse().withTextContent("Planet Earth was created in the year 0"));
    }

    @Test
    public void testToolList() throws Exception {

        String response = client.listAllTools();
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

    @Test
    public void testToolListInvalidCursor() throws Exception {
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
        assertThat(client.call(request),
                   jsonRpcResponse().withErrorCode(INVALID_PARAMS)
                                    .withErrorMessage("Invalid params")
                                    .withErrorData("CWMCM0022E: The \"optional-cursor-value\" cursor value is invalid."));
    }

    @Test
    public void testEchoMethodCallError() throws Exception {
        server.setMarkToEndOfLog();

        assertThat(client.callTool("echo", Map.of("input", "throw error")),
                   toolResponse().withError("An internal server error occurred while running the tool."));
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

        assertThat(client.call(request),
                   jsonRpcResponse().withErrorCode(INTERNAL_ERROR)
                                    .withErrorMessage("Internal error")
                                    .withErrorDataItems("Could not call privateEcho"));
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

        assertThat(client.call(request),
                   jsonRpcResponse().withErrorCode(INVALID_PARAMS)
                                    .withErrorMessage("Invalid params")
                                    .withErrorDataItems("Method privateEchoMissing not found"));
    }

    @Test
    public void testAddWithIntegerInputArgs() throws Exception {
        assertThat(client.callTool("add", Map.of("num1", 100, "num2", 200)),
                   toolResponse().withTextContent("300"));
    }

    @Test
    public void testToggleWithBooleanArgs() throws Exception {
        assertThat(client.callTool("toggle", Map.of("value", true)),
                   toolResponse().withTextContent("false"));
    }

    @Test
    public void testJSONCharacter() throws Exception {
        assertThat(client.callTool("testJSONCharacter", Map.of("c", "c")),
                   toolResponse().withTextContent("c"));
    }

    @Test
    public void testJSONcharacter() throws Exception {
        assertThat(client.callTool("testJSONcharacter", Map.of("c", "c")),
                   toolResponse().withTextContent("c"));
    }

    @Test
    public void testJSONlong() throws Exception {
        assertThat(client.callTool("testJSONlong", Map.of("num1", 2)),
                   toolResponse().withTextContent("2"));
    }

    @Test
    public void testJSONdouble() throws Exception {
        assertThat(client.callTool("testJSONdouble", Map.of("num1", 2.2)),
                   toolResponse().withTextContent("2.2"));
    }

    @Test
    public void testJSONbyte() throws Exception {
        assertThat(client.callTool("testJSONbyte", Map.of("num1", 2)),
                   toolResponse().withTextContent("2"));
    }

    @Test
    public void testJSONfloat() throws Exception {
        assertThat(client.callTool("testJSONfloat", Map.of("num1", 2.5)),
                   toolResponse().withTextContent("2.5"));
    }

    @Test
    public void testJSONshort() throws Exception {
        assertThat(client.callTool("testJSONshort", Map.of("num1", 2)),
                   toolResponse().withTextContent("2"));
    }

    @Test
    public void testJSONLong() throws Exception {
        assertThat(client.callTool("testJSONLong", Map.of("num1", 2)),
                   toolResponse().withTextContent("2"));
    }

    @Test
    public void testJSONDouble() throws Exception {
        assertThat(client.callTool("testJSONDouble", Map.of("num1", 2.5)),
                   toolResponse().withTextContent("2.5"));
    }

    @Test
    public void testJSONByte() throws Exception {
        assertThat(client.callTool("testJSONByte", Map.of("num1", 2)),
                   toolResponse().withTextContent("2"));
    }

    @Test
    public void testJSONFloat() throws Exception {
        assertThat(client.callTool("testJSONFloat", Map.of("num1", 2.5)),
                   toolResponse().withTextContent("2.5"));
    }

    @Test
    public void testJSONShort() throws Exception {
        assertThat(client.callTool("testJSONShort", Map.of("num1", 2)),
                   toolResponse().withTextContent("2"));
    }

    @Test
    public void testJSONInteger() throws Exception {
        assertThat(client.callTool("testJSONInteger", Map.of("num1", 2)),
                   toolResponse().withTextContent("2"));
    }

    @Test
    public void testJSONBoolean() throws Exception {
        assertThat(client.callTool("testJSONBoolean", Map.of("b", true)),
                   toolResponse().withTextContent("true"));
    }

    @Test
    public void testReturningObject() throws Exception {
        // the object within the text field is expected to have the fields in lexicographical order after converting the object to JSON
        assertThat(client.callTool("testObjectResponse", Map.of("name", "Manchester")),
                   toolResponse().withTextContent("{\"country\":\"England\",\"isCapital\":false,\"name\":\"Manchester\",\"population\":8000}")
                                 .withStructuredContent(new JSONObject("""
                                                 {
                                                   "country": "England",
                                                   "isCapital": false,
                                                   "name": "Manchester",
                                                   "population": 8000
                                                 }
                                                 """)));
    }

    @Test
    public void testStringNotReturnedAsStructuredContent() throws Exception {
        assertThat(client.callTool("testStringStructuredContentResponse", Map.of()),
                   toolResponse().withTextContent("Hello World")
                                 .withNoStructuredContent());
    }

    @Test
    public void testStaticInnerTool() throws Exception {
        assertThat(client.callTool("staticInnerTool", Map.of("input", "World")),
                   toolResponse().withTextContent("Hello World"));
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
        JSONObject arguments = new JSONObject("""
                        {
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
                        """);
        assertThat(callToolWithJson("checkPerson", arguments, null),
                   toolResponse().withTextContent("true"));
    }

    /** The {@code arguments} shared by the {@code addPersonToList*} tool calls. */
    private static final String ADD_PERSON_ARGUMENTS = """
                    {
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
                    """;

    /** The {@code structuredContent} returned by the {@code addPersonToList*} tools for {@link #ADD_PERSON_ARGUMENTS}. */
    private static final String PERSON_LIST_STRUCTURED_CONTENT = """
                    {
                        "returnList": [
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
                        ]
                    }
                    """;

    /**
     * The text content returned by the {@code addPersonToList*} tools for {@link #ADD_PERSON_ARGUMENTS}: the
     * structured content serialised to JSON, with its fields in the order the server writes them.
     */
    private static final String PERSON_LIST_TEXT = """
                    {"returnList":[{"address":{"number":2,"postcode":"so21 2rt","street":{"streetName":"Poles Ln","roadType":"n/a"}},"company":{"address":{"number":100,"postcode":"so21 2er","street":{"streetName":"Hursley Park Rd","roadType":"Private Property"}},"name":"IBM","shareholderRegistry":{"1":{"address":{"number":100,"postcode":"so21 2er","street":{"streetName":"Hursley Park Rd","roadType":"Private Property"}},"fullname":"Shareholder 1"}}},"fullname":"John Smith"},{"address":{"number":100,"postcode":"so21 2er","street":{"streetName":"Hursley Park Rd","roadType":"Private Property"}},"fullname":"Shareholder 1"},{"address":{"number":2,"postcode":"so21 2rt","street":{"streetName":"Poles Ln","roadType":"n/a"}},"company":{"address":{"number":100,"postcode":"so21 2er","street":{"streetName":"Hursley Park Rd","roadType":"Private Property"}},"name":"IBM"},"fullname":"John Smith"}]}""";

    /** The request {@code _meta} sent to, and the result {@code _meta} returned by, the {@code addPersonToListToolResponse*} tools. */
    private static final String PERSON_META = """
                    {
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
                    """;

    /**
     * Calls a tool whose arguments, and optionally request {@code _meta}, are given as JSON, for the
     * tests whose arguments are too large to build from a {@link Map}.
     */
    private ToolResponse callToolWithJson(String toolName, JSONObject arguments, JSONObject meta) throws Exception {
        JSONObject params = new JSONObject().put("name", toolName).put("arguments", arguments);
        if (meta != null) {
            params.put("_meta", meta);
        }
        return client.callTool(client.request("tools/call", params));
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
        assertThat(callToolWithJson("addPersonToList", new JSONObject(ADD_PERSON_ARGUMENTS), null),
                   toolResponse().withTextContent(PERSON_LIST_TEXT)
                                 .withStructuredContent(new JSONObject(PERSON_LIST_STRUCTURED_CONTENT)));
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
        assertThat(callToolWithJson("addPersonToListToolResponse", new JSONObject(ADD_PERSON_ARGUMENTS), null),
                   toolResponse().withTextContent(PERSON_LIST_TEXT)
                                 .withStructuredContent(new JSONObject(PERSON_LIST_STRUCTURED_CONTENT))
                                 .withCustomMeta(new JSONObject(PERSON_META)));
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
        assertThat(callToolWithJson("addPersonToListToolResponseWithMetaRequest", new JSONObject(ADD_PERSON_ARGUMENTS), new JSONObject(PERSON_META)),
                   toolResponse().withTextContent(PERSON_LIST_TEXT)
                                 .withStructuredContent(new JSONObject(PERSON_LIST_STRUCTURED_CONTENT))
                                 .withCustomMeta(new JSONObject(PERSON_META)));
    }

    @Test
    public void simpleMetaRequest() throws Exception {
        assertThat(client.callTool("simpleMetaRequest", Map.of("name", "IBMUser"),
                                   Map.of("api.ibmtest.org/location", "Hursley", "timestamp", 1762860699)),
                   toolResponse().withTextContent("Hello IBMUser you have called this tool from Hursley at timestamp 1762860699"));
    }

    @Test
    public void noArgRequest() throws Exception {
        assertThat(client.callTool("noArgsRequest", null,
                                   Map.of("api.ibmtest.org/location", "Hursley", "timestamp", 1762860699)),
                   toolResponse().withTextContent("You have called this tool from Hursley at timestamp 1762860699"));
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
        client.callTool("textContentTool", Map.of("input", "hello"));

        assertNotNull(server.waitForStringInTrace(Pattern.quote(redactedSessionId)));
        assertNull(server.waitForStringInTrace(sessionId, TestConstants.NEGATIVE_TIMEOUT_MS)); // negative check to confirm full session Id not found in trace
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
        assertThat(client.callTool("get-user-jp", Map.of("userid", "ユーザー1")),
                   toolResponse().withTextContent("ID: ユーザー1, Name: 仮名, role: user"));
    }

    @Test
    public void testStructuredContentResponseWithNonLatinCharacters() throws Exception {
        assertThat(client.callTool("testNonLatinStringStructuredContent", Map.of()),
                   toolResponse().withTextContent("{\"country\":\"日本\",\"isCapital\":true,\"name\":\"東京\",\"population\":14000000}")
                                 .withStructuredContent(new JSONObject("""
                                                 {
                                                   "country": "日本",
                                                   "isCapital": true,
                                                   "name": "東京",
                                                   "population": 14000000
                                                 }
                                                 """)));
    }

    @Test
    public void testMcpRequest() throws Exception {
        assertThat(client.callTool("checkMcpRequest"),
                   toolResponse().withTextContent("OK"));
    }

    @Test
    public void testMcpRequestHasSessionId() throws Exception {
        String reverseSessionId = new StringBuilder(client.getSessionId()).reverse().toString();
        assertThat(client.callTool("readSessionIdReversed"),
                   toolResponse().withTextContent(reverseSessionId));
    }

    @Test
    public void testResourceLinkTool() throws Exception {
        assertThat(client.callTool("resourceLinkTool", Map.of("name", "readme", "uri", "file:///readme.md")),
                   toolResponse().withContent(new JSONObject("""
                                   {
                                     "type": "resource_link",
                                     "name": "readme",
                                     "uri": "file:///readme.md",
                                     "title": "Resource: readme",
                                     "mimeType": "text/plain"
                                   }
                                   """)));
    }

    @Test
    public void testResourceLinkToolWithAnnotation() throws Exception {
        assertThat(client.callTool("resourceLinkToolWithAnnotation", Map.of("name", "readme", "uri", "file:///readme.md")),
                   toolResponse().withContent(new JSONObject("""
                                   {
                                     "type": "resource_link",
                                     "name": "readme",
                                     "uri": "file:///readme.md",
                                     "annotations": {
                                       "audience": ["user"],
                                       "lastModified": "2025-08-26T08:40:00Z",
                                       "priority": null
                                     }
                                   }
                                   """)));
    }

    @Test
    public void testEmbeddedTextResourceTool() throws Exception {
        assertThat(client.callTool("embeddedTextResourceTool", Map.of("text", "Hello world", "uri", "file:///readme.md")),
                   toolResponse().withContent(new JSONObject("""
                                   {
                                     "type": "resource",
                                     "resource": {
                                       "uri": "file:///readme.md",
                                       "text": "Hello world",
                                       "mimeType": "text/plain"
                                     }
                                   }
                                   """)));
    }

    @Test
    public void testEmbeddedBlobResourceTool() throws Exception {
        String imageData64 = TestConstants.TEST_IMAGE_DATA_64;
        assertThat(client.callTool("embeddedBlobResourceTool", Map.of("imageData", imageData64, "uri", "file:///image.png")),
                   toolResponse().withContent(new JSONObject("""
                                   {
                                     "type": "resource",
                                     "resource": {
                                       "uri": "file:///image.png",
                                       "blob": "%s",
                                       "mimeType": "image/png"
                                     }
                                   }
                                   """.formatted(imageData64))));
    }

    /**
     * Verifies that a {@code @Tool} method with a {@link org.mcpjava.server.progress.Progress}
     * parameter is invoked successfully and that the no-op {@code ProgressImpl} reports no
     * progress token (i.e. {@code progress.token().isPresent() == false}).
     */
    @Test
    public void testProgressInjectedWithNoToken() throws Exception {
        // The tool returns "progressTokenPresent=false,input=hello" when no progress
        // token is present, confirming the no-op ProgressImpl was injected.
        assertThat(progressClient.callTool("progressTool", Map.of("input", "hello")),
                   toolResponse().withTextContent("progressTokenPresent=false,input=hello"));
    }

    /**
     * Verifies that {@code progressTool} can be listed via {@code tools/list},
     * confirming that the tool with a {@link org.mcpjava.server.progress.Progress}
     * parameter is registered correctly without deployment errors.
     */
    @Test
    public void testProgressToolIsListed() throws Exception {
        String request = """
                        {
                          "jsonrpc": "2.0",
                          "id": "2",
                          "method": "tools/list",
                          "params": {}
                        }
                        """;

        String response = progressClient.callMCP(request);

        // Verify the tool was registered (name is present in the tools list)
        String expectedFragment = """
                        {
                          "result": {
                            "tools": [
                              {
                                "name": "progressTool"
                              }
                            ]
                          }
                        }
                        """;

        JSONAssert.assertEquals(expectedFragment, response, false);
    }

    @Test
    public void testUnannotatedToolArgs() throws Exception {
        assertThat(client.callTool("unannotatedArgTool", Map.of("name", "fred", "count", 3)),
                   toolResponse().withTextContent("hello hello hello fred of FAT Test Client"));
    }

    @Test
    public void testNoParamNameTool() throws Exception {
        assertThat(client.callTool("echoNoParamName", Map.of("input", "test1")),
                   toolResponse().withTextContent("test1"));
    }

    @Test
    public void testOutputSchemaFromOnToolResponseToolCall() throws Exception {
        String request = """
                        {
                          "jsonrpc": "2.0",
                          "id": 2,
                          "method": "tools/call",
                          "params": {
                            "name": "testOutputSchemaFromOnToolResponse",
                            "arguments": { "name": "Paris" }
                          }
                        }
                        """;
        String response = client.callMCP(request);
        String expected = """
                        {
                          "id": 2,
                          "jsonrpc": "2.0",
                          "result": {
                            "isError": false,
                            "structuredContent": {
                              "name": "Paris",
                              "country": "England",
                              "population": 8000,
                              "isCapital": false
                            }
                          }
                        }
                        """;
        JSONAssert.assertEquals(expected, response, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    public void testOutputSchemaFromOverridesReturnTypeToolCall() throws Exception {
        // Returns String - schema is City (from outputSchemaFrom) but response is plain text
        assertThat(client.callTool("testOutputSchemaFromOverridesReturnType", Map.of("name", "Lyon")),
                   toolResponse().withTextContent("Lyon")
                                 .withNoStructuredContent());
    }

    @Test
    public void testSchemaAnnotationTakesPrecedenceOverOutputSchemaFromToolCall() throws Exception {
        String request = """
                        {
                          "jsonrpc": "2.0",
                          "id": 2,
                          "method": "tools/call",
                          "params": {
                            "name": "testSchemaAnnotationTakesPrecedenceOverOutputSchemaFrom",
                            "arguments": { "name": "Paris" }
                          }
                        }
                        """;
        String response = client.callMCP(request);
        // @Schema wins over outputSchemaFrom=City — structuredContent has cityName, not City fields
        String expected = """
                        {
                          "id": 2,
                          "jsonrpc": "2.0",
                          "result": {
                            "isError": false,
                            "structuredContent": {
                              "cityName": "Paris"
                            }
                          }
                        }
                        """;
        JSONAssert.assertEquals(expected, response, JSONCompareMode.NON_EXTENSIBLE);
    }

}