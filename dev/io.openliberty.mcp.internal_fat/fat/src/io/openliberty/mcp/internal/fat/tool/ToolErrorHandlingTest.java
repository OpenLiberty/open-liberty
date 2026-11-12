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

import static io.openliberty.mcp.internal.fat.suite.McpMainServerSuite.serverLifecycle;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;

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

import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.mcp.internal.fat.tool.businessExceptionApp.ToolErrorHandlingTools;
import io.openliberty.mcp.internal.fat.utils.McpClient;
import io.openliberty.mcp.internal.fat.utils.ToolResponse;
import io.openliberty.mcp.internal.fat.utils.ToolResponseMatcher;

/**
 *
 */
@RunWith(FATRunner.class)
public class ToolErrorHandlingTest extends FATServletClient {

    // Server is managed by McpMainServerSuite — do NOT add @Server or copy the field here.
    private static final String APP_NAME = "toolErrorHandlingTest";
    private static final String ENDPOINT = "/" + APP_NAME;

    @Rule
    public McpClient client = McpClient.builder(serverLifecycle.getServer(), ENDPOINT).build();

    @BeforeClass
    public static void setup() throws Exception {
        serverLifecycle.getServer().setMarkToEndOfLog();
        WebArchive war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war").addPackage(ToolErrorHandlingTools.class.getPackage());
        serverLifecycle.deployDropinApp(war);
    }

    @AfterClass
    public static void teardown() throws Exception {
        serverLifecycle.getServer().addIgnoredErrors(List.of("CWMCM0010E")); // Tool method threw an unexpected exception
        serverLifecycle.undeployDropinApp(APP_NAME);
    }

    @Test
    public void testToolThrowsToolCallException() throws Exception {
        ToolResponse response = client.callTool("businessErrorTool", Map.of("input", "bad-value"));
        assertThat(response, ToolResponseMatcher.withErrorText("Invalid business input: bad-value"));
    }

    @Test
    public void testToolThrowsExceptionWithoutWrapBusiness() throws Exception {
        ToolResponse response = client.callTool("unwrappedExceptionTool", Map.of("input", "trigger"));
        assertThat(response, ToolResponseMatcher.withErrorText("An internal server error occurred while running the tool."));
    }

    @Test
    public void testToolThrowsExceptionWithWrapNoArgs() throws Exception {
        ToolResponse response = client.callTool("wrappedNoArgsTool", Map.of("input", "trigger"));
        assertThat(response, ToolResponseMatcher.withErrorText("Wrapped error for input: trigger"));
    }

    @Test
    public void testToolThrowsListedWrappedException() throws Exception {
        ToolResponse response = client.callTool("listedWrappedExceptionTool", Map.of("input", "bad-value"));
        assertThat(response, ToolResponseMatcher.withErrorText("Invalid business input: bad-value"));
    }

    @Test
    public void testToolThrowsExceptionWrappedBySuperclass() throws Exception {
        ToolResponse response = client.callTool("superclassWrappedExceptionTool", Map.of("input", "test arg"));
        assertThat(response, ToolResponseMatcher.withErrorText("Invalid input for superclass: test arg"));
    }

    @Test
    public void testToolThrowsUnwrappedException() throws Exception {
        ToolResponse response = client.callTool("excludeExceptionTool", Map.of("input", "fail-now"));
        assertThat(response, ToolResponseMatcher.withErrorText("An internal server error occurred while running the tool."));
    }

    @Test
    public void testCheckedExceptionTool() throws Exception {
        ToolResponse response = client.callTool("checkedExceptionTool", Map.of("input", "abc"));
        assertThat(response, ToolResponseMatcher.withErrorText("Checked error for: abc"));
    }

    @Test
    public void testCheckedExceptionTool_Unwrapped() throws Exception {
        ToolResponse response = client.callTool("unwrappedCheckedExceptionTool", Map.of("input", "abc"));
        Log.info(getClass(), "unwrappedCheckedExceptionTool", "Raw MCP response: " + response);

        assertThat(response, ToolResponseMatcher.withErrorText("An internal server error occurred while running the tool."));

        assertNotNull(serverLifecycle.getServer().waitForStringInLogUsingMark("Unwrapped checked error for: abc", serverLifecycle.getServer().getDefaultLogFile()));
    }

    @Test
    public void testUnexpectedArgumentSupplied() throws Exception {
        ToolResponse response = client.callTool("inputValidationTool", Map.of("input", "Hi", "count", 2, "extra", "unexpected"));
        assertThat(response, ToolResponseMatcher.withErrorText("The following arguments were passed but were not found in the method: [extra]."));
    }

    @Test
    public void testMissingRequiredArgument() throws Exception {
        ToolResponse response = client.callTool("inputValidationTool", Map.of("count", 2));
        assertThat(response, ToolResponseMatcher.withErrorText("The method expected the following arguments but did not receive them: [input]."));
    }

    @Test
    public void testArgumentTypeMismatch() throws Exception {
        ToolResponse response = client.callTool("inputValidationTool", Map.of("input", "hello", "count", "oops"));
        assertThat(response, ToolResponseMatcher.withErrorText("The count argument cannot be converted to the java.lang.Integer type."));
    }

    @Test
    public void testToolList() throws Exception {
        String request = """
                        {
                          "jsonrpc": "2.0",
                          "id": 1,
                          "method": "tools/list"
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
                                                       "description": "Handles business tool error",
                                                       "type": "string"
                                                   }
                                               },
                                               "required": [
                                                   "input"
                                               ]
                                           },
                                           "name": "businessErrorTool",
                                           "description": "This tool throws error on business tool",
                                           "title": "Business Error Handler"
                                       },
                                       {
                                           "inputSchema": {
                                               "type": "object",
                                               "properties": {
                                                   "input": {
                                                       "description": "Throws raw exception",
                                                       "type": "string"
                                                   }
                                               },
                                               "required": [
                                                   "input"
                                               ]
                                           },
                                           "name": "unwrappedExceptionTool",
                                           "description": "Throws unwrapped RuntimeException",
                                           "title": "Unwrapped Exception Tool"
                                       },
                                       {
                                           "inputSchema": {
                                               "type": "object",
                                               "properties": {
                                                   "input": {
                                                       "description": "Error Triggers",
                                                       "type": "string"
                                                   }
                                               },
                                               "required": [
                                                   "input"
                                               ]
                                           },
                                           "name": "wrappedNoArgsTool",
                                           "description": "Throws raw exception with @WrapBusinessException but no types listed",
                                           "title": "Tool with no Args"
                                       },
                                       {
                                           "inputSchema": {
                                               "type": "object",
                                               "properties": {
                                                   "input": {
                                                       "description": "Invalid input",
                                                       "type": "string"
                                                   }
                                               },
                                               "required": [
                                                   "input"
                                               ]
                                           },
                                           "name": "listedWrappedExceptionTool",
                                           "description": "Tool throws exception listed in WrapBusinessException",
                                           "title": "Listed Wrapped Exception"
                                       },
                                       {
                                           "inputSchema": {
                                               "type": "object",
                                               "properties": {
                                                   "input": {
                                                       "description": "This is input",
                                                       "type": "string"
                                                   }
                                               },
                                               "required": [
                                                   "input"
                                               ]
                                           },
                                           "name": "superclassWrappedExceptionTool",
                                           "description": "Tool throws subclass exception",
                                           "title": "Superclass Listed"
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
                                           "name": "excludeExceptionTool",
                                           "description": "Throws a business exception not listed in WrapBusinessException",
                                           "title": "Excludes exception Tool"
                                       },
                                       {
                                           "inputSchema": {
                                               "type": "object",
                                               "properties": {
                                                   "input": {
                                                       "description": "Checked exception Triggers",
                                                       "type": "string"
                                                   }
                                               },
                                               "required": [
                                                   "input"
                                               ]
                                           },
                                           "name": "checkedExceptionTool",
                                           "description": "Throws a checked exception",
                                           "title": "Checked Exception"
                                       },
                                       {
                                           "inputSchema": {
                                               "type": "object",
                                               "properties": {
                                                   "input": {
                                                       "description": "Triggers unwrapped checked",
                                                       "type": "string"
                                                   }
                                               },
                                               "required": [
                                                   "input"
                                               ]
                                           },
                                           "name": "unwrappedCheckedExceptionTool",
                                           "description": "Throws IOException but not listed",
                                           "title": "Unwrapped Checked"
                                       },
                                       {
                                          "inputSchema": {
                                            "type": "object",
                                            "properties": {
                                              "input": {
                                                "type": "string"
                                              },
                                              "count": {
                                                "type": "integer"
                                              }
                                            },
                                            "required": [
                                              "input"
                                            ]
                                          },
                                          "name": "inputValidationTool",
                                          "title": "Input Validation Tool"
                                        },
                                        {
                                          "inputSchema": {
                                            "type": "object",
                                            "properties": {
                                              "input": {
                                                "description": "input",
                                                "type": "string"
                                              }
                                            },
                                            "required": [
                                              "input"
                                            ]
                                          },
                                          "name": "unauthorizedTool",
                                          "description": "Tool that throws ToolCallUnauthorizedException",
                                          "title": "Unauthorized Tool"
                                        }
                                     ]
                                    },
                                    "id":1,
                                    "jsonrpc":"2.0"
                                 }
                                 """;

        // Lenient mode test (false boolean in 3rd parameter
        JSONAssert.assertEquals(expectedString, jsonResponse.toString(), JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    public void testToolThrowsToolCallUnauthorizedException() throws Exception {
        String request = """
                        {
                          "jsonrpc": "2.0",
                          "id": 1,
                          "method": "tools/call",
                          "params": {
                            "name": "unauthorizedTool",
                            "arguments": {
                              "input": "bad-value"
                            }
                          }
                        }
                        """;

        McpClient.McpDetailedAuthResponse response = client.callMCPRaw(request).anyStatus().run();
        Log.info(ToolErrorHandlingTest.class, "testToolThrowsToolCallUnauthorizedException", response.toString());

        assertEquals(403, response.statusCode());
        assertTrue("Content-Type must be text/plain", response.contentType().contains("text/plain"));
        assertTrue("Response body must contain the exception message",
                   response.body().contains("Not authorized to call this tool for input: bad-value"));
        assertFalse("Response must not be a JSON-RPC envelope", response.body().contains("jsonrpc"));
    }
}
