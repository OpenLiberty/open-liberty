/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.lifecycle.tests;

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.List;

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
import io.openliberty.mcp.internal.fat.tool.asyncToolApp.AsyncLifecycleTools;
import io.openliberty.mcp.internal.fat.utils.McpClient;
import io.openliberty.mcp.internal.fat.utils.ToolStatus;
import io.openliberty.mcp.internal.fat.utils.ToolStatusClient;

@SuppressWarnings("unchecked")
@RunWith(FATRunner.class)
public class AsyncToolLifecycleTest {
    private static final String EXPECTED_ERROR = "Method call caused runtime exception. This is expected if the input was 'throw error'";

    @Server("mcp-server-async")
    public static LibertyServer server;

    @Rule
    public ToolStatusClient toolStatus = new ToolStatusClient(server, "/asyncToolLifecycleTest");

    @Rule
    public McpClient client = new McpClient(server, "/asyncToolLifecycleTest");

    @BeforeClass
    public static void setup() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "asyncToolLifecycleTest.war")
                                   .addPackage(AsyncLifecycleTools.class.getPackage())
                                   .addPackage(ToolStatus.class.getPackage());

        ShrinkHelper.exportDropinAppToServer(server, war, SERVER_ONLY);

        server.startServer();
    }

    @AfterClass
    public static void teardown() throws Exception {
        server.stopServer(EXPECTED_ERROR);
    }

    @Test
    public void testAsyncDependentBeanLifecycleForCompleteCompletionStage() throws Exception {
        server.setMarkToEndOfLog();
        String request = """
                        {
                          "jsonrpc": "2.0",
                          "id": "2",
                          "method": "tools/call",
                          "params": {
                            "beanClass": "io.openliberty.mcp.internal_fat.tool.asyncToolApp.AsyncLifecycleTools",
                            "name": "asyncLifecycleCompleteCompletionStage",
                            "arguments": {
                              "input": "Hello, World"
                            }
                          }
                        }
                        """;

        String response = client.callMCP(request);

        String expectedResponseString = """
                        {
                          "id": "2",
                          "jsonrpc": "2.0",
                          "result": {
                            "content": [
                              { "type": "text", "text": "Hello, World: (async)" }
                            ],
                            "isError": false
                          }
                        }
                        """;

        JSONAssert.assertEquals(expectedResponseString, response, true);

        assertNotNull(server.waitForStringInLogUsingMark("\\[LIFECYCLE] @PreDestroy AsyncLifecycleTools"));

        // Fetch all lifecycle-related log messages
        List<String> lifecycleMessages = server.findStringsInLogsUsingMark(".*\\[(LIFECYCLE|LOGGED)].*", server.getDefaultLogFile());
        assertFalse("No [LIFECYCLE] lines found in logs since mark", lifecycleMessages.isEmpty());

        assertThat("Unexpected lifecycle sequence:\n" + String.join("\n", lifecycleMessages),
                   lifecycleMessages,
                   contains(containsString("@PostConstruct AsyncLifecycleTools"),
                            containsString("[LOGGED] AsyncLifecycleTools.asyncLifecycleCompleteCompletionStage Tool logged"),
                            containsString("@PreDestroy AsyncLifecycleTools")));
    }

    @Test
    public void testAsyncDependentBeanLifecycleForAsyncStage() throws Exception {
        server.setMarkToEndOfLog();
        String request = """
                        {
                          "jsonrpc": "2.0",
                          "id": "2",
                          "method": "tools/call",
                          "params": {
                            "beanClass": "io.openliberty.mcp.internal_fat.tool.asyncToolApp.AsyncLifecycleTools",
                            "name": "asyncLifecycleAsyncStage",
                            "arguments": {
                              "input": "Hello, World"
                            }
                          }
                        }
                        """;

        String response = client.callMCP(request);

        String expectedResponseString = """
                        {
                          "id": "2",
                          "jsonrpc": "2.0",
                          "result": {
                            "content": [
                              { "type": "text", "text": "Hello, World: (async)" }
                            ],
                            "isError": false
                          }
                        }
                        """;

        JSONAssert.assertEquals(expectedResponseString, response, true);

        assertNotNull(server.waitForStringInLogUsingMark("\\[LIFECYCLE] @PreDestroy AsyncLifecycleTools"));

        // Fetch all lifecycle-related log messages
        List<String> lifecycleMessages = server.findStringsInLogsUsingMark(".*\\[(LIFECYCLE|LOGGED)].*", server.getDefaultLogFile());
        assertFalse("No [LIFECYCLE] lines found in logs since mark", lifecycleMessages.isEmpty());

        assertThat("Unexpected lifecycle sequence:\n" + String.join("\n", lifecycleMessages),
                   lifecycleMessages,
                   contains(containsString("@PostConstruct AsyncLifecycleTools"),
                            containsString("[LOGGED] AsyncLifecycleTools.asyncLifecycleAsyncStage Tool logged"),
                            containsString("[LOGGED] AsyncLifecycleTools.asyncLifecycleAsyncStage Tool running process async"),
                            containsString("@PreDestroy AsyncLifecycleTools")));
    }

    @Test
    public void testAsyncDependentBeanLifecycleWhenToolThrowsException() throws Exception {
        server.setMarkToEndOfLog();
        String request = """
                        {
                          "jsonrpc": "2.0",
                          "id": "2",
                          "method": "tools/call",
                          "params": {
                            "beanClass": "io.openliberty.mcp.internal_fat.tool.asyncToolApp.AsyncLifecycleTools",
                            "name": "asyncLifecycleCompleteCompletionStage",
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

        assertNotNull(server.waitForStringInLogUsingMark("\\[LIFECYCLE] @PreDestroy AsyncLifecycleTools"));

        List<String> lifecycleMessages = server.findStringsInLogsUsingMark(".*\\[(LIFECYCLE|LOGGED)].*", server.getDefaultLogFile());
        assertFalse("No [LIFECYCLE] lines found in logs since mark", lifecycleMessages.isEmpty());

        assertThat("Unexpected lifecycle sequence:\n" + String.join("\n", lifecycleMessages),
                   lifecycleMessages,
                   contains(containsString("@PostConstruct AsyncLifecycleTools"),
                            containsString("[LOGGED] AsyncLifecycleTools.asyncLifecycleCompleteCompletionStage Tool logged"),
                            containsString("[LOGGED] AsyncLifecycleTools.asyncLifecycleCompleteCompletionStage Tool throwing error"),
                            containsString("@PreDestroy AsyncLifecycleTools")));
    }

    // Negative Tests

    /**
     * Negative test: when {@code asyncLifecycleAsyncStage} is called with the
     * {@code "throw error"} input it throws a {@link RuntimeException} synchronously
     * (before returning a stage). The {@code @PreDestroy} callback must still fire;
     * the CDI {@code Dependent} lifecycle must be honoured on the exception path.
     */
    @Test
    public void testAsyncDependentBeanLifecycleWhenAsyncStageThrowsException() throws Exception {
        server.setMarkToEndOfLog();
        String request = """
                        {
                          "jsonrpc": "2.0",
                          "id": "neg-1",
                          "method": "tools/call",
                          "params": {
                            "beanClass": "io.openliberty.mcp.internal_fat.tool.asyncToolApp.AsyncLifecycleTools",
                            "name": "asyncLifecycleAsyncStage",
                            "arguments": {
                              "input": "throw error"
                            }
                          }
                        }
                        """;

        String response = client.callMCP(request);

        // The server must return an error response, not crash
        String expectedResponseString = """
                        {"id":"neg-1","jsonrpc":"2.0","result":{"content":[{"type":"text","text":"An internal server error occurred while running the tool."}], "isError": true}}
                        """;
        JSONAssert.assertEquals(expectedResponseString, response, true);

        // @PreDestroy must fire even though the tool threw synchronously
        assertNotNull("@PreDestroy must fire after asyncLifecycleAsyncStage throws an exception",
                      server.waitForStringInLogUsingMark("\\[LIFECYCLE] @PreDestroy AsyncLifecycleTools"));

        List<String> lifecycleMessages = server.findStringsInLogsUsingMark(".*\\[(LIFECYCLE|LOGGED)].*", server.getDefaultLogFile());
        assertFalse("Lifecycle log must not be empty", lifecycleMessages.isEmpty());

        assertThat("Unexpected lifecycle sequence on error path:\n" + String.join("\n", lifecycleMessages),
                   lifecycleMessages,
                   contains(containsString("@PostConstruct AsyncLifecycleTools"),
                            containsString("[LOGGED] AsyncLifecycleTools.asyncLifecycleAsyncStage Tool logged"),
                            containsString("[LOGGED] AsyncLifecycleTools.asyncLifecycleAsyncStage Tool throwing error"),
                            containsString("@PreDestroy AsyncLifecycleTools")));
    }

    /**
     * Negative test: two successive calls to the same async tool must each create and
     * destroy a distinct {@code Dependent}-scoped bean instance;instance reuse across
     * invocations is not permitted by the CDI {@code @Dependent} scope.
     */
    @Test
    public void testAsyncDependentBeanIsNotReusedAcrossSuccessiveCalls() throws Exception {
        server.setMarkToEndOfLog();

        String request = """
                        {
                          "jsonrpc": "2.0",
                          "id": "neg-2",
                          "method": "tools/call",
                          "params": {
                            "beanClass": "io.openliberty.mcp.internal_fat.tool.asyncToolApp.AsyncLifecycleTools",
                            "name": "asyncLifecycleCompleteCompletionStage",
                            "arguments": {
                              "input": "First"
                            }
                          }
                        }
                        """;
        client.callMCP(request);

        String request2 = """
                        {
                          "jsonrpc": "2.0",
                          "id": "neg-3",
                          "method": "tools/call",
                          "params": {
                            "beanClass": "io.openliberty.mcp.internal_fat.tool.asyncToolApp.AsyncLifecycleTools",
                            "name": "asyncLifecycleCompleteCompletionStage",
                            "arguments": {
                              "input": "Second"
                            }
                          }
                        }
                        """;
        client.callMCP(request2);

        // Wait for at least one @PreDestroy to confirm both calls ran
        assertNotNull("@PreDestroy must fire after async tool calls",
                      server.waitForStringInLogUsingMark("\\[LIFECYCLE] @PreDestroy AsyncLifecycleTools"));

        List<String> postConstructEvents = server.findStringsInLogsUsingMark(
                                                                             ".*\\[LIFECYCLE] @PostConstruct AsyncLifecycleTools.*", server.getDefaultLogFile());
        List<String> preDestroyEvents = server.findStringsInLogsUsingMark(
                                                                          ".*\\[LIFECYCLE] @PreDestroy AsyncLifecycleTools.*", server.getDefaultLogFile());

        assertTrue("Two separate @PostConstruct events must fire (Dependent scope, no instance reuse)",
                   postConstructEvents.size() >= 2);
        assertTrue("Two separate @PreDestroy events must fire",
                   preDestroyEvents.size() >= 2);
    }
}
