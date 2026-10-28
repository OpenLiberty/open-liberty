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

import static io.openliberty.mcp.internal.fat.suite.McpAsyncServerSuite.serverLifecycle;
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
import io.openliberty.mcp.internal.fat.tool.asyncToolApp.AsyncTools;
import io.openliberty.mcp.internal.fat.utils.McpClient;
import io.openliberty.mcp.internal.fat.utils.ToolStatus;

@RunWith(FATRunner.class)
public class AsyncToolCallEventTraceTest extends FATServletClient {

    private static final String EXPECTED_ERROR = "Method call caused runtime exception. This is expected if the input was 'throw error'";
    private static final String APP_NAME = "asyncToolCallEventTraceTest";

    // Server is managed by McpAsyncServerSuite — do NOT add @Server or copy the field here.

    @Rule
    public McpClient client = McpClient.builder(serverLifecycle.getServer(), "/" + APP_NAME).build();


    @BeforeClass
    public static void setup() throws Exception {
        serverLifecycle.getServer().addIgnoredErrors(List.of(EXPECTED_ERROR));
        serverLifecycle.getServer().setMarkToEndOfLog();
        WebArchive war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                                   .addPackage(AsyncTools.class.getPackage())
                                   .addPackage(ToolStatus.class.getPackage());

        serverLifecycle.deployDropinApp(war);
        assertNotNull(serverLifecycle.getServer().waitForStringInLogUsingMark("MCP server endpoint: .*/mcp$"));
    }

    @AfterClass
    public static void teardown() throws Exception {
        serverLifecycle.undeployDropinApp(APP_NAME);
    }

    @Test
    public void testEventTraceForAsyncToolCall() throws Exception {
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
        client.callMCP(request);
        List<String> expectedEventTraceMsgs = List.of("A tool call request has arrived", "The tool method 'asyncEcho' is about to be called",
                                                      "The tool method 'asyncEcho' returned: 'Async Hello: (async)'");

        for (String traceMsg : expectedEventTraceMsgs) {
            assertTrue("Expected event trace not found: " + traceMsg, !serverLifecycle.getServer().findStringsInLogsAndTrace(Pattern.quote(traceMsg)).isEmpty());
        }

    }

    @Test
    public void testEventTraceForSyncToolCallWithExceptionThrown() throws Exception {
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
        client.callMCP(request);
        String traceMsg = "The tool method 'asyncEcho' returned the following error to the user: 'An internal server error occurred while running the tool.'";
        assertTrue("Expected event trace not found: " + traceMsg, !serverLifecycle.getServer().findStringsInLogsAndTrace(Pattern.quote(traceMsg)).isEmpty());
    }

    @Test
    public void testEventTraceForAsyncToolCallWithToolCallError() throws Exception {
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

        client.callMCP(request);
        String traceMsg = "The tool method 'asyncEcho' returned the following error to the user: 'The method expected the following arguments but did not receive them: [input].'";
        assertTrue("Expected event trace not found: " + traceMsg, !serverLifecycle.getServer().findStringsInLogsAndTrace(Pattern.quote(traceMsg)).isEmpty());

    }

}
