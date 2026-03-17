/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.tool;

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;
import static io.openliberty.mcp.internal.fat.tool.toolManagerApp.ToolEditorServlet.DYNAMIC_REPEATER;
import static java.util.function.Function.identity;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.skyscreamer.jsonassert.JSONCompareMode.STRICT;

import java.io.StringReader;
import java.util.Map;
import java.util.stream.Collectors;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

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
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.mcp.internal.fat.tool.toolManagerApp.ToolEditorClient;
import io.openliberty.mcp.internal.fat.tool.toolManagerApp.ToolEditorServlet;
import io.openliberty.mcp.internal.fat.tool.toolManagerApp.ToolManagerStartupTestBean;
import io.openliberty.mcp.internal.fat.tool.toolManagerApp.ToolManagerTestServlet;
import io.openliberty.mcp.internal.fat.utils.McpClient;

/**
 * Test use of the ToolManager bean
 */
@RunWith(FATRunner.class)
public class ToolManagerTest extends FATServletClient {

    private static final String APP_NAME = "ToolManagerTest";

    @TestServlet(contextRoot = APP_NAME, servlet = ToolManagerTestServlet.class)
    @Server("mcp-server")
    public static LibertyServer server;

    @Rule
    public McpClient client = new McpClient(server, APP_NAME);

    @BeforeClass
    public static void setup() throws Exception {

        WebArchive ToolManagerTest = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                                               .addPackage(ToolManagerTestServlet.class.getPackage());

        ShrinkHelper.exportDropinAppToServer(server, ToolManagerTest, SERVER_ONLY);

        server.startServer();
    }

    @AfterClass
    public static void teardown() throws Exception {
        server.stopServer("CWMCM0010E"); // The - tool method threw an unexpected exception.
    }

    /**
     * Test that tools can be added and removed dynamically
     * <p>
     * The tool is added and removed via {@link ToolEditorServlet} and {@link ToolEditorClient}
     *
     * @throws Exception on error
     */
    @Test
    public void testDynamicToolUpdate() throws Exception {
        ToolEditorClient toolEditorClient = new ToolEditorClient(server, APP_NAME);
        try {
            // List all tools, test our dynamic tool is not registered
            assertThat(getTools(), not(hasKey(DYNAMIC_REPEATER)));

            // Add our test tool
            toolEditorClient.addDynamicRepeaterTool(5);

            // Check that it's now returned by list tools
            assertThat(getTools(), hasKey(DYNAMIC_REPEATER));
            // Validate the descriptor
            String description = getTools().get(DYNAMIC_REPEATER).getString("description");
            assertEquals("repeat string 5 times", description);

            // Test calling the tool
            String request = """
                            {
                              "jsonrpc": "2.0",
                              "id": 1,
                              "method": "tools/call",
                              "params": {
                                "name": "dynamicRepeater",
                                "arguments": {
                                  "inputString": "bork"
                                }
                              }
                            }
                            """;
            String expectedResponse = """
                            {
                              "jsonrpc": "2.0",
                              "id": 1,
                              "result": {
                                "content": [
                                  {
                                    "type": "text",
                                    "text": "borkborkborkborkbork"
                                  }
                                ],
                                "isError": false
                              }
                            }
                            """;
            JSONAssert.assertEquals(expectedResponse, client.callMCP(request), STRICT);

            // Remove the tool
            toolEditorClient.removeDynamicRepeaterTool();

            // Check that it's now gone from the tool list
            assertThat(getTools(), not(hasKey(DYNAMIC_REPEATER)));
        } finally {
            // Ensure we remove the tool if the test fails to not interfere with other tests
            toolEditorClient.removeDynamicRepeaterTool();
        }
    }

    /**
     * Test the tool descriptors returned from tools/list for tools registered with the API
     * <p>
     * Tools for this test are created in {@link ToolManagerStartupTestBean#createToolListTools}
     *
     * @throws Exception on error
     */
    @Test
    public void testToolList() throws Exception {

        var tools = getTools();

        // Covers: name, title, description, arguments (several types, required/not, default)
        //         annotations, generated schema
        JsonObject toolWithArgs = tools.get("tool-with-args");
        JSONAssert.assertEquals("""
                        {
                            "name": "tool-with-args",
                            "title": "Tool With Args",
                            "description": "Test tool with arguments",
                            "inputSchema": {
                                "type": "object",
                                "properties": {
                                    "stringArg": {
                                        "type": "string",
                                        "description": "string argument"
                                    },
                                    "intArg": {
                                        "type": "integer",
                                        "description": "integer argument"
                                    },
                                    "pojoArg": {
                                        "type": "object",
                                        "description": "POJO argument",
                                        "properties": {
                                            "foo": {
                                                "type": "string"
                                            },
                                            "bar": {
                                                "type": "integer"
                                            }
                                        },
                                        "required": ["foo", "bar"]
                                    }
                                },
                                "required": ["intArg"]
                            },
                            "outputSchema": {
                                "type": "object",
                                "properties": {
                                    "baz": {
                                        "type": "string"
                                    },
                                    "qux": {
                                        "type": "array",
                                        "items": {
                                            "type": "boolean"
                                        }
                                    }
                                },
                                "required": ["baz", "qux"]
                            },
                            "annotations": {
                                "title": "Anno Title",
                                "readOnlyHint": true,
                                "destructiveHint": false,
                                "openWorldHint": false
                            }
                        }
                        """,
                                toolWithArgs.toString(),
                                STRICT);

        // Covers: supplied schema
        JsonObject toolWithManualSchema = tools.get("tool-with-manual-schema");
        JSONAssert.assertEquals("""
                        {
                            "name": "tool-with-manual-schema",
                            "inputSchema": {
                                "type": "object",
                                "properties": {
                                    "foo": {
                                        "type": "string"
                                    },
                                    "bar": {
                                        "type": "integer"
                                    },
                                },
                                "required": ["foo"]
                            },
                            "outputSchema": {
                                "type": "object",
                                "properties": {
                                    "baz": {
                                        "type": "string"
                                    },
                                    "qux": {
                                        "type": "array",
                                        "items": {
                                            "type": "boolean"
                                        }
                                    }
                                },
                                "required": ["baz", "qux"]
                            }
                        }
                        """,
                                toolWithManualSchema.toString(),
                                STRICT);
    }

    /**
     * Test calling a synchronous tool, registered via the API. Test success, business error and non-business error responses.
     * <p>
     * Tool defined in {@link ToolManagerStartupTestBean#createSyncTestTool}
     *
     * @throws Exception on error
     */
    @Test
    public void testCallSyncTool() throws Exception {
        String successRequest = """
                        {
                          "jsonrpc": "2.0",
                          "id": 3,
                          "method": "tools/call",
                          "params": {
                            "name": "sync-test-tool",
                            "arguments": {
                              "action": "success"
                            }
                          }
                        }
                        """;
        String expectedSuccessResponse = """
                        {
                          "jsonrpc": "2.0",
                          "id": 3,
                          "result": {
                            "content": [{
                              "type": "text",
                              "text": "OK"
                            }],
                            "isError": false
                          }
                        }
                        """;

        String successResponse = client.callMCP(successRequest);
        JSONAssert.assertEquals(expectedSuccessResponse, successResponse, STRICT);

        String errorRequest = """
                        {
                          "jsonrpc": "2.0",
                          "id": 4,
                          "method": "tools/call",
                          "params": {
                            "name": "sync-test-tool",
                            "arguments": {
                              "action": "error"
                            }
                          }
                        }
                        """;
        String expectedErrorResponse = """
                        {
                          "jsonrpc": "2.0",
                          "id": 4,
                          "result": {
                            "content": [{
                              "type": "text",
                              "text": "Error"
                            }],
                            "isError": true
                          }
                        }
                        """;

        String errorResponse = client.callMCP(errorRequest);
        JSONAssert.assertEquals(expectedErrorResponse, errorResponse, STRICT);

        String exceptionRequest = """
                        {
                          "jsonrpc": "2.0",
                          "id": 5,
                          "method": "tools/call",
                          "params": {
                            "name": "sync-test-tool",
                            "arguments": {
                              "action": "exception"
                            }
                          }
                        }
                        """;
        String exceptionErrorResponse = """
                        {
                          "jsonrpc": "2.0",
                          "id": 5,
                          "result": {
                            "content": [{
                              "type": "text",
                              "text": "An internal server error occurred while running the tool."
                            }],
                            "isError": true
                          }
                        }
                        """;

        server.setMarkToEndOfLog();
        String exceptionResponse = client.callMCP(exceptionRequest);
        JSONAssert.assertEquals(exceptionErrorResponse, exceptionResponse, STRICT);
        assertNotNull(server.waitForStringInLogUsingMark("CWMCM0010E: The sync-test-tool tool method threw an unexpected exception. The exception is .*Test Exception"));
    }

    /**
     * Test calling an asynchronous tool, registered via the API. Test success, business error (thrown as an exception and returned asynchronously),
     * and non-business error (thrown and returned asynchronously).
     * <p>
     * Tool defined in {@link ToolManagerStartupTestBean#createSyncTestTool}
     *
     * @throws Exception on error
     */
    @Test
    public void testCallAsyncTool() throws Exception {
        String successRequest = """
                        {
                          "jsonrpc": "2.0",
                          "id": 3,
                          "method": "tools/call",
                          "params": {
                            "name": "async-test-tool",
                            "arguments": {
                              "action": "success"
                            }
                          }
                        }
                        """;
        String expectedSuccessResponse = """
                        {
                          "jsonrpc": "2.0",
                          "id": 3,
                          "result": {
                            "content": [{
                              "type": "text",
                              "text": "OK"
                            }],
                            "isError": false
                          }
                        }
                        """;

        String successResponse = client.callMCP(successRequest);
        JSONAssert.assertEquals(expectedSuccessResponse, successResponse, STRICT);

        String errorRequest = """
                        {
                          "jsonrpc": "2.0",
                          "id": 4,
                          "method": "tools/call",
                          "params": {
                            "name": "async-test-tool",
                            "arguments": {
                              "action": "error"
                            }
                          }
                        }
                        """;
        String expectedErrorResponse = """
                        {
                          "jsonrpc": "2.0",
                          "id": 4,
                          "result": {
                            "content": [{
                              "type": "text",
                              "text": "Error"
                            }],
                            "isError": true
                          }
                        }
                        """;

        String errorResponse = client.callMCP(errorRequest);
        JSONAssert.assertEquals(expectedErrorResponse, errorResponse, STRICT);

        String asyncErrorRequest = """
                        {
                          "jsonrpc": "2.0",
                          "id": 4,
                          "method": "tools/call",
                          "params": {
                            "name": "async-test-tool",
                            "arguments": {
                              "action": "async-error"
                            }
                          }
                        }
                        """;
        String expectedAsyncErrorResponse = """
                        {
                          "jsonrpc": "2.0",
                          "id": 4,
                          "result": {
                            "content": [{
                              "type": "text",
                              "text": "Error"
                            }],
                            "isError": true
                          }
                        }
                        """;

        String asyncErrorResponse = client.callMCP(asyncErrorRequest);
        JSONAssert.assertEquals(expectedAsyncErrorResponse, asyncErrorResponse, STRICT);

        String exceptionRequest = """
                        {
                          "jsonrpc": "2.0",
                          "id": 5,
                          "method": "tools/call",
                          "params": {
                            "name": "async-test-tool",
                            "arguments": {
                              "action": "exception"
                            }
                          }
                        }
                        """;
        String expectedExceptionResponse = """
                        {
                          "jsonrpc": "2.0",
                          "id": 5,
                          "result": {
                            "content": [{
                              "type": "text",
                              "text": "An internal server error occurred while running the tool."
                            }],
                            "isError": true
                          }
                        }
                        """;

        server.setMarkToEndOfLog();
        String exceptionResponse = client.callMCP(exceptionRequest);
        JSONAssert.assertEquals(expectedExceptionResponse, exceptionResponse, STRICT);
        assertNotNull(server.waitForStringInLogUsingMark("CWMCM0010E: The async-test-tool tool method threw an unexpected exception. The exception is .*Test Exception"));

        String asyncExceptionRequest = """
                        {
                          "jsonrpc": "2.0",
                          "id": 5,
                          "method": "tools/call",
                          "params": {
                            "name": "async-test-tool",
                            "arguments": {
                              "action": "async-exception"
                            }
                          }
                        }
                        """;
        String expectedAsyncExceptionResponse = """
                        {
                          "jsonrpc": "2.0",
                          "id": 5,
                          "result": {
                            "content": [{
                              "type": "text",
                              "text": "An internal server error occurred while running the tool."
                            }],
                            "isError": true
                          }
                        }
                        """;

        server.setMarkToEndOfLog();
        String asyncExceptionResponse = client.callMCP(asyncExceptionRequest);
        JSONAssert.assertEquals(expectedAsyncExceptionResponse, asyncExceptionResponse, STRICT);
        assertNotNull(server.waitForStringInLogUsingMark("CWMCM0010E: The async-test-tool tool method threw an unexpected exception. The exception is .*Test Async Exception"));
    }

    /**
     * Calls tools/list and returns all the tools from the server as a map.
     *
     * @return a map from tool name to the JSON descriptor for the tool
     * @throws Exception on error
     */
    private Map<String, JsonObject> getTools() throws Exception {
        JsonObject response;
        try (JsonReader parser = Json.createReader(new StringReader(client.listAllTools()))) {
            response = parser.readObject();
        }

        return response.getJsonObject("result")
                       .getJsonArray("tools")
                       .stream()
                       .map(JsonObject.class::cast)
                       .collect(Collectors.toMap(o -> o.getString("name"), identity()));
    }
}
