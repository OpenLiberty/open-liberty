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
import componenttest.topology.utils.FATServletClient;
import io.openliberty.mcp.internal.fat.tool.basicToolApp.BasicTools;
import io.openliberty.mcp.internal.fat.utils.McpClient;

/**
 *
 */
@RunWith(FATRunner.class)
public class ToolTest extends FATServletClient {

    @Server("mcp-server")
    public static LibertyServer server;

    @Rule
    public McpClient client = new McpClient(server, "/toolTest");

    @BeforeClass
    public static void setup() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "toolTest.war").addPackage(BasicTools.class.getPackage());

        ShrinkHelper.exportDropinAppToServer(server, war, SERVER_ONLY);

        server.startServer();

        assertNotNull(server.waitForStringInLog("MCP server endpoint: .*/mcp$")); // regex matches string that ends with /mcp e.g. "MCP server endpoint: http://macbookpro.home:8010/toolTest/mcp"
    }

    @AfterClass
    public static void teardown() throws Exception {
        server.stopServer();
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
                            "jsonrpc field must be present. Only JSONRPC 2.0 is currently supported",
                            "method must be present and not empty",
                            "id must be a string or number"
                            ],
                        "message":"Invalid request"},
                        "id":null,
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
                        "id":null,
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
                            "Missing arguments in params"
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
                            "args [other] passed but not found in method",
                            "args [input] were expected by the method"
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

        String expectedString = """
                        {
                            "result": {
                                "tools": [
                                   {
                                        "inputSchema": {
                                            "type": "object",
                                            "properties": {
                                                "input": {
                                                    "type": "string"
                                                }
                                            },
                                            "required": [
                                                "input"
                                            ]
                                        },
                                        "name": "ignoredEcho"
                                    },
                                    {
                                        "inputSchema": {
                                            "type": "object",
                                            "properties": {
                                                "input": {
                                                    "type": "string"
                                                }
                                            },
                                            "required": [
                                                "input"
                                            ]
                                        },
                                        "name": ""
                                    },
                                    {
                                        "inputSchema": {
                                            "type": "object",
                                            "properties": {
                                                "num1": {
                                                    "description": "first number",
                                                    "type": "integer"
                                                },
                                                "num2": {
                                                    "description": "second number",
                                                    "type": "integer"
                                                }
                                            },
                                            "required": [
                                                "num1",
                                                "num2"
                                            ]
                                        },
                                        "name": "add",
                                        "description": "Returns the sum of the two inputs",
                                        "title": "Addition calculator"
                                    },
                                    {
                                        "inputSchema": {
                                            "type": "object",
                                            "properties": {
                                                "@arg1!><": {
                                                    "description": "specialCharactersInToolArgName",
                                                    "type": "string"
                                                },
                                                "@arg2={}": {
                                                    "description": "specialCharactersInToolArgName",
                                                    "type": "string"
                                                }
                                            },
                                            "required": [
                                                "@arg1!><",
                                                "@arg2={}"
                                            ]
                                        },
                                        "name": "specialCharactersInToolArgName",
                                    },
                                    {
                                        "inputSchema": {
                                            "type": "object",
                                            "properties": {
                                                "@arg1'()": {
                                                    "description": "specialCharactersInToolArgName",
                                                    "type": "string"
                                                },
                                                "@arg2.%:": {
                                                    "description": "specialCharactersInToolArgName",
                                                    "type": "string"
                                                }
                                            },
                                            "required": [
                                                "@arg1'()",
                                                "@arg2.%:"
                                            ]
                                        },
                                        "name": "specialCharactersInToolArgNameVariant2",
                                    },
                                    {
                                        "inputSchema": {
                                            "type": "object",
                                            "properties": {
                                                "package": {
                                                    "description": "reservedNamesInToolArgName",
                                                    "type": "string"
                                                },
                                                "int": {
                                                    "description": "reservedNamesInToolArgName",
                                                    "type": "string"
                                                }
                                            },
                                            "required": [
                                                "package",
                                                "int"
                                            ]
                                        },
                                        "name": "reservedNamesInToolArgName",
                                    },
                                    {
                                        "inputSchema": {
                                            "type": "object",
                                            "properties": {
                                                "class": {
                                                    "description": "reservedNamesInToolArgName",
                                                    "type": "string"
                                                },
                                                "void": {
                                                    "description": "reservedNamesInToolArgName",
                                                    "type": "string"
                                                }
                                            },
                                            "required": [
                                                "class",
                                                "void"
                                            ]
                                        },
                                        "name": "reservedNamesInToolArgNameVariant",
                                    },
                                    {
                                        "inputSchema": {
                                            "type": "object",
                                            "properties": {
                                                "input": {
                                                    "description": "input to echo",
                                                    "type": "string"
                                                }
                                            },
                                            "required": [
                                                "input"
                                            ]
                                        },
                                        "name": "privateEcho",
                                        "description": "Returns the input unchanged",
                                        "title": "Echoes the input"
                                    },
                                    {
                                        "inputSchema": {
                                            "type": "object",
                                            "properties": {
                                                "arg1": {
                                                    "description": "reservedWordsInToolName",
                                                    "type": "string"
                                                }
                                            },
                                            "required": [
                                                "arg1"
                                            ]
                                        },
                                        "name": "package"
                                    },
                                    {
                                        "inputSchema": {
                                            "type": "object",
                                            "properties": {
                                                "arg1": {
                                                    "description": "specialCharactersInToolName",
                                                    "type": "string"
                                                }
                                            },
                                            "required": [
                                                "arg1"
                                            ]
                                        },
                                        "name": "specialCharactersInToolName@!><={}'().%:"
                                    },
                                    {
                                        "inputSchema": {
                                            "type": "object",
                                            "properties": {
                                                "num1": {
                                                    "type": "integer"
                                                },
                                                "num2": {
                                                    "type": "integer"
                                                }
                                            },
                                            "required": [
                                                "num1",
                                                "num2"
                                            ]
                                        },
                                        "name": "subtract",
                                        "description": "Minus number 2 from number 1",
                                        "title": "Subtraction calculator"
                                    },
                                    {
                                        "inputSchema": {
                                            "type": "object",
                                            "properties": {
                                                "input": {
                                                    "description": "input to echo",
                                                    "type": "string"
                                                }
                                            },
                                            "required": [
                                                "input"
                                            ]
                                        },
                                        "name": "echo",
                                        "description": "Returns the input unchanged",
                                        "title": "Echoes the input"
                                    },
                                    {
                                        "inputSchema": {
                                            "type": "object",
                                            "properties": {
                                                "num1": {
                                                    "description": "long",
                                                    "type": "number"
                                                }
                                            },
                                            "required": [
                                                "num1"
                                            ]
                                        },
                                        "name": "testJSONlong",
                                        "description": "testJSONlong",
                                        "title": "testJSONlong"
                                    },
                                    {
                                        "inputSchema": {
                                            "type": "object",
                                            "properties": {
                                                "num1": {
                                                    "description": "double",
                                                    "type": "number"
                                                }
                                            },
                                            "required": [
                                                "num1"
                                            ]
                                        },
                                        "name": "testJSONdouble",
                                        "description": "testJSONdouble",
                                        "title": "testJSONdouble"
                                    },
                                    {
                                        "inputSchema": {
                                            "type": "object",
                                            "properties": {
                                                "num1": {
                                                    "description": "byte",
                                                    "type": "number"
                                                }
                                            },
                                            "required": [
                                                "num1"
                                            ]
                                        },
                                        "name": "testJSONbyte",
                                        "description": "testJSONbyte",
                                        "title": "testJSONbyte"
                                    },
                                    {
                                        "inputSchema": {
                                            "type": "object",
                                            "properties": {
                                                "num1": {
                                                    "description": "float",
                                                    "type": "number"
                                                }
                                            },
                                            "required": [
                                                "num1"
                                            ]
                                        },
                                        "name": "testJSONfloat",
                                        "description": "testJSONfloat",
                                        "title": "testJSONfloat"
                                    },
                                    {
                                        "inputSchema": {
                                            "type": "object",
                                            "properties": {
                                                "num1": {
                                                    "description": "short",
                                                    "type": "number"
                                                }
                                            },
                                            "required": [
                                                "num1"
                                            ]
                                        },
                                        "name": "testJSONshort",
                                        "description": "testJSONshort",
                                        "title": "testJSONshort"
                                    },
                                    {
                                        "inputSchema": {
                                            "type": "object",
                                            "properties": {
                                                "num1": {
                                                    "description": "Long",
                                                    "type": "number"
                                                }
                                            },
                                            "required": [
                                                "num1"
                                            ]
                                        },
                                        "name": "testJSONLong",
                                        "description": "testJSONLong",
                                        "title": "testJSONLong"
                                    },
                                    {
                                        "inputSchema": {
                                            "type": "object",
                                            "properties": {
                                                "num1": {
                                                    "description": "Double",
                                                    "type": "number"
                                                }
                                            },
                                            "required": [
                                                "num1"
                                            ]
                                        },
                                        "name": "testJSONDouble",
                                        "description": "testJSONDouble",
                                        "title": "testJSONDouble"
                                    },
                                    {
                                        "inputSchema": {
                                            "type": "object",
                                            "properties": {
                                                "num1": {
                                                    "description": "Byte",
                                                    "type": "number"
                                                }
                                            },
                                            "required": [
                                                "num1"
                                            ]
                                        },
                                        "name": "testJSONByte",
                                        "description": "testJSONByte",
                                        "title": "testJSONByte"
                                    },
                                    {
                                        "inputSchema": {
                                            "type": "object",
                                            "properties": {
                                                "num1": {
                                                    "description": "Float",
                                                    "type": "number"
                                                }
                                            },
                                            "required": [
                                                "num1"
                                            ]
                                        },
                                        "name": "testJSONFloat",
                                        "description": "testJSONFloat",
                                        "title": "testJSONFloat"
                                    },
                                    {
                                        "inputSchema": {
                                            "type": "object",
                                            "properties": {
                                                "num1": {
                                                    "description": "Short",
                                                    "type": "number"
                                                }
                                            },
                                            "required": [
                                                "num1"
                                            ]
                                        },
                                         "name": "testJSONShort",
                                        "description": "testJSONShort",
                                        "title": "testJSONShort"
                                    },
                                    {
                                        "inputSchema": {
                                            "type": "object",
                                            "properties": {
                                                "num1": {
                                                    "description": "Integer",
                                                    "type": "integer"
                                                }
                                            },
                                            "required": [
                                                "num1"
                                            ]
                                        },
                                        "name": "testJSONInteger",
                                        "description": "testJSONInteger",
                                        "title": "testJSONInteger"
                                    },
                                    {
                                        "inputSchema": {
                                            "type": "object",
                                            "properties": {
                                                "c": {
                                                    "description": "Character",
                                                    "type": "string"
                                                }
                                            },
                                            "required": [
                                                "c"
                                            ]
                                        },
                                        "name": "testJSONCharacter",
                                        "description": "testJSONCharacter",
                                        "title": "testJSONCharacter"
                                    },
                                    {
                                        "inputSchema": {
                                            "type": "object",
                                            "properties": {
                                                "c": {
                                                    "description": "char",
                                                    "type": "string"
                                                }
                                            },
                                            "required": [
                                                "c"
                                            ]
                                        },
                                        "name": "testJSONcharacter",
                                        "description": "testJSONcharacter",
                                        "title": "testJSONcharacter"
                                    },
                                    {
                                        "inputSchema": {
                                            "type": "object",
                                            "properties": {
                                                "b": {
                                                    "description": "Boolean",
                                                    "type": "boolean"
                                                }
                                            },
                                            "required": [
                                                "b"
                                            ]
                                        },
                                        "name": "testJSONBoolean",
                                        "description": "testJSONBoolean",
                                        "title": "testJSONBoolean"
                                    },
                                    {
                                      "inputSchema": {
                                        "type": "object",
                                        "properties": {
                                          "value": {
                                            "description": "boolean value",
                                            "type": "boolean"
                                          }
                                        },
                                        "required": [
                                          "value"
                                        ]
                                      },
                                      "name": "toggle",
                                      "description": "toggles the boolean input",
                                      "title": "Boolean toggle"
                                    },
                                    {
                                      "inputSchema": {
                                        "type": "object",
                                        "properties": {
                                          "input": {
                                            "description": "input string",
                                            "type": "string"
                                          }
                                        },
                                        "required": [
                                          "input"
                                        ]
                                      },
                                      "annotations": {
                                        "readOnlyHint": true
                                      },
                                      "name": "readOnlyTool",
                                      "title": "Read Only Tool",
                                      "description": "A tool that is read-only"
                                    },
                                    {
                                      "inputSchema": {
                                        "type": "object",
                                        "properties": {
                                          "input": {
                                            "description": "input string",
                                            "type": "string"
                                          }
                                        },
                                        "required": [
                                          "input"
                                        ]
                                      },
                                      "annotations": {
                                        "openWorldHint": false,
                                        "title": "Destructive Tool"
                                      },
                                      "name": "destructiveTool",
                                      "title": "Destructive Tool",
                                      "description": "A tool that performs a destructive operation"
                                    },
                                    {
                                      "inputSchema": {
                                        "type": "object",
                                        "properties": {
                                          "input": {
                                            "description": "input string",
                                            "type": "string"
                                          }
                                        },
                                        "required": [
                                          "input"
                                        ]
                                      },
                                      "annotations": {
                                        "title": "Open to World Tool"
                                      },
                                      "name": "openWorldTool",
                                      "title": "Open to World Tool",
                                      "description": "A tool in an open world context"
                                    },
                                    {
                                      "inputSchema": {
                                        "type": "object",
                                        "properties": {
                                          "input": {
                                            "description": "input string",
                                            "type": "string"
                                          }
                                        },
                                        "required": [
                                          "input"
                                        ]
                                      },
                                      "annotations": {
                                        "idempotentHint": true,
                                        "title": "Idempotent Tool"
                                      },
                                      "name": "idempotentTool",
                                      "title": "Idempotent Tool",
                                      "description": "A tool with idempotent context"
                                    },
                                    {
                                      "inputSchema": {
                                        "type": "object",
                                        "properties": {
                                          "input": {
                                            "description": "input string",
                                            "type": "string"
                                          }
                                        },
                                        "required": [
                                          "input"
                                        ]
                                      },
                                      "name": "missingTitle",
                                      "description": "A tool that does not have a title"
                                    },
                                    {
                                      "inputSchema": {
                                        "type": "object",
                                        "properties": {
                                          "input": {
                                            "description": "input to echo",
                                            "type": "string"
                                          }
                                        },
                                        "required": [
                                          "input"
                                        ]
                                      },
                                      "name": "mixedContentTool",
                                      "description": "Returns Text, Audio or Image Content",
                                      "title": "Mixed Content Tool"
                                    },
                                    {
                                      "inputSchema": {
                                        "type": "object",
                                        "properties": {
                                          "input": {
                                            "description": "input to echo",
                                            "type": "string"
                                          }
                                        },
                                        "required": [
                                          "input"
                                        ]
                                      },
                                      "name": "mixedContentListTool",
                                      "description": "Returns Text, Audio or Image Content List",
                                      "title": "Mixed Content List Tool"
                                    },
                                    {
                                      "inputSchema": {
                                        "type": "object",
                                        "properties": {
                                          "input": {
                                            "description": "input string to echo back as content",
                                            "type": "string"
                                          }
                                        },
                                        "required": [
                                          "input"
                                        ]
                                      },
                                      "name": "textContentTool",
                                      "description": "Returns text content object",
                                      "title": "Text Content Tool"
                                    },
                                    {
                                      "inputSchema": {
                                        "type": "object",
                                        "properties": {
                                          "imageData": {
                                            "description": "Base64-encoded image",
                                            "type": "string"
                                          }
                                        },
                                        "required": [
                                          "imageData"
                                        ]
                                      },
                                      "name": "imageContentTool",
                                      "description": "Returns image content object",
                                      "title": "Image Content Tool"
                                    },
                                    {
                                      "inputSchema": {
                                        "type": "object",
                                        "properties": {
                                          "audioData": {
                                            "description": "Base64-encoded audio",
                                            "type": "string"
                                          }
                                        },
                                        "required": [
                                          "audioData"
                                        ]
                                      },
                                      "name": "audioContentTool",
                                      "description": "Returns audio content object",
                                      "title": "Audio Content Tool"
                                    },
                                    {
                                        "inputSchema": {
                                            "type": "object",
                                            "properties": {},
                                            "required": []
                                        },
                                        "name": "testListObjectResponse",
                                        "description": "A tool to return a list of cities",
                                        "title": "City List"
                                    },
                                    {
                                        "inputSchema": {
                                            "type": "object",
                                            "properties": {},
                                            "required": []
                                        },
                                        "name": "testListStringResponse",
                                        "description": "A tool to return a list of strings",
                                        "title": "String List"
                                    },
                                    {
                                        "inputSchema": {
                                            "type": "object",
                                            "properties": {},
                                            "required": []
                                        },
                                        "name": "testArrayResponse",
                                        "description": "A tool to return an array of ints",
                                        "title": "Array of ints"
                                    },
                                    {
                                        "inputSchema": {
                                            "type": "object",
                                            "properties": {
                                                "name": {
                                                    "description": "name of your city",
                                                    "type": "string"
                                                }
                                            },
                                            "required": [
                                                "name"
                                            ]
                                        },
                                        "name": "testObjectResponse",
                                        "description": "A tool to return a city object you've named",
                                        "title": "Create a city"
                                    },
                                    {
                                        "inputSchema": {
                                            "type": "object",
                                            "properties": {},
                                            "required": []
                                        },
                                        "name": "testStringStructuredContentResponse",
                                        "description": "A tool to return a string with structuredContent set. The tool should ignore this and not return a structuredContent field when the response is string.",
                                        "title": "Structured Content String Response"
                                    },
                                    {
                                        "inputSchema": {
                                            "type": "object",
                                            "properties": {
                                                "value": {
                                                    "description": "boolean value",
                                                    "type": "boolean"
                                                }
                                            },
                                            "required": []
                                        },

                                        "name": "testToolArgIsNotRequired",
                                        "description": "ToolArgNotRequired",
                                        "title": "ToolArgNotRequired"
                                     },
                                    {
                                        "inputSchema": {
                                            "type": "object",
                                            "properties": {
                                                "input": {
                                                    "type": "string"
                                                }
                                            },
                                            "required": [
                                                "input"
                                            ]
                                        },

                                        "name": "staticInnerTool",
                                        "description": "Defined in static inner class",
                                        "title": "Static Inner Tool"
                                      },
                                ]
                            },
                            "id": 1,
                            "jsonrpc": "2.0"
                        }
                         """;

        // Lenient mode test (false boolean in 3rd parameter
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
                        {"id":2,"jsonrpc":"2.0","result":{"content":[{"type":"text","text":"Internal server error"}], "isError": true}}
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
}
