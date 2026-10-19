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

import static io.openliberty.mcp.internal.fat.suite.McpMainServerSuite.serverLifecycle;
import static io.openliberty.mcp.internal.fat.utils.TestConstants.ACCEPT;
import static io.openliberty.mcp.internal.fat.utils.TestConstants.MCP_PROTOCOL_VERSION;
import static io.openliberty.mcp.internal.fat.utils.TestConstants.MCP_SESSION_ID;
import static io.openliberty.mcp.internal.fat.utils.TestConstants.VALUE_ACCEPT_DEFAULT;
import static io.openliberty.mcp.internal.fat.utils.TestConstants.VALUE_MCP_PROTOCOL_VERSION;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.regex.Pattern;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.utils.FATServletClient;
import componenttest.topology.utils.HttpRequest;
import io.openliberty.mcp.internal.fat.tool.basicToolApp.BasicTools;
import io.openliberty.mcp.internal.fat.utils.McpClient;

@RunWith(FATRunner.class)
public class ToolCallEventTraceTest extends FATServletClient {

    // Server is managed by McpMainServerSuite — do NOT add @Server or copy the field here.
    private static final String APP_NAME = "toolCallEventTraceTest";

    @Rule
    public McpClient client = McpClient.builder(serverLifecycle.getServer(), "/" + APP_NAME).build();
    
    @BeforeClass
    public static void setup() throws Exception {
        serverLifecycle.getServer().setMarkToEndOfLog();
        WebArchive war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                                   .addPackage(BasicTools.class.getPackage());
        serverLifecycle.deployDropinApp(war);
    }

    @AfterClass
    public static void teardown() throws Exception {
        serverLifecycle.getServer().addIgnoredErrors(List.of("CWMCM0010E")); // The - tool method threw an unexpected exception.
        serverLifecycle.undeployDropinApp(APP_NAME);
    }

    @Test
    public void testEventTraceForSyncToolCall() throws Exception {
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
        List<String> expectedEventTraceMsgs = List.of("A tool call request has arrived", "The tool method 'textContentTool' is about to be called",
                                                      "The tool method 'textContentTool' returned: 'Echo: hello'");

        for (String traceMsg : expectedEventTraceMsgs) {
            assertTrue("Expected event trace not found: " + traceMsg, !serverLifecycle.getServer().findStringsInLogsAndTrace(Pattern.quote(traceMsg)).isEmpty());
        }

    }

    @Test
    public void testEventTraceForSyncToolCallWithExceptionThrown() throws Exception {
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
        client.callMCP(request);
        String traceMsg = "The tool method 'echo' returned the following error to the user: 'An internal server error occurred while running the tool.'";
        assertTrue("Expected event trace not found: " + traceMsg, !serverLifecycle.getServer().findStringsInLogsAndTrace(Pattern.quote(traceMsg)).isEmpty());
    }

    @Test
    public void testEventTraceForSyncToolCallWithJsonRpcError() throws Exception {
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
        client.callMCP(request);
        String traceMsg = "The following error was returned to the user: 'JSONRPCException {code=-32603, message='Internal error', data=[Could not call privateEcho]}'";
        assertTrue("Expected event trace not found: " + traceMsg, !serverLifecycle.getServer().findStringsInLogsAndTrace(Pattern.quote(traceMsg)).isEmpty());
    }

    @Test
    public void testEventTraceForSyncToolCallWithToolCallError() throws Exception {
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

        client.callMCP(request);
        String traceMsg = "The tool method 'echo' returned the following error to the user: 'The method expected the following arguments but did not receive them: [input].'";
        assertTrue("Expected event trace not found: " + traceMsg, !serverLifecycle.getServer().findStringsInLogsAndTrace(Pattern.quote(traceMsg)).isEmpty());

    }

    @Test
    public void testEventTraceForSyncToolCallWithHttpResponseException() throws Exception {
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

        new HttpRequest(serverLifecycle.getServer(), "/" + APP_NAME + "/mcp")
                                                              .requestProp(ACCEPT, VALUE_ACCEPT_DEFAULT)
                                                              .requestProp(MCP_PROTOCOL_VERSION, VALUE_MCP_PROTOCOL_VERSION)
                                                              .requestProp(MCP_SESSION_ID, sessionId)
                                                              .jsonBody(request)
                                                              .method("POST")
                                                              .expectCode(404)
                                                              .run(String.class);

        String traceMsg = "The following error was returned to the user: 'HTTP 404 - Invalid or Expired Session Id'";
        assertTrue("Expected event trace not found: " + traceMsg, !serverLifecycle.getServer().findStringsInLogsAndTrace(Pattern.quote(traceMsg)).isEmpty());
    }

}
