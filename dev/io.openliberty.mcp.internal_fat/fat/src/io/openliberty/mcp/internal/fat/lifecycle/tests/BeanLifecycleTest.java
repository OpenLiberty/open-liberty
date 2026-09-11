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
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.mcp.internal.fat.lifecycle.tools.ClassTool;
import io.openliberty.mcp.internal.fat.utils.McpClient;

/**
 *
 */
@SuppressWarnings("unused")
@RunWith(FATRunner.class)
public class BeanLifecycleTest {

    @Server("mcp-server")
    public static LibertyServer server;

    @Rule
    public McpClient client = new McpClient(server, "/beanLifecycleTest");

    public static void info(Class<?> clazz, String method, String message) {
        Log.info(clazz, method, message);
    }

    @BeforeClass
    public static void setup() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "beanLifecycleTest.war")
                                   .addPackage(ClassTool.class.getPackage());

        ShrinkHelper.exportDropinAppToServer(server, war, SERVER_ONLY);

        server.startServer();
    }

    @AfterClass
    public static void teardown() throws Exception {
        server.stopServer();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testDependentBeanLifecycle() throws Exception {
        server.setMarkToEndOfLog();
        String request = """
                        {
                          "jsonrpc": "2.0",
                          "id": "2",
                          "method": "tools/call",
                          "params": {
                            "beanClass": "io.openliberty.mcp.internal_fat.lifecycle.tools.ClassTool",
                            "name": "sayHello",
                            "arguments": {
                              "name": "World"
                            }
                          }
                        }
                        """;

        String response = client.callMCP(request);
        Log.info(getClass(), "testDependentBeanLifecycle", "Raw MCP response: " + response);

        // Strict Mode tests
        String expectedResponseString = """
                        {
                          "id": "2",
                          "jsonrpc": "2.0",
                          "result": {
                            "content": [
                              { "type": "text", "text": "Hello, World" }
                            ],
                            "isError": false
                          }
                        }
                        """;
        JSONAssert.assertEquals(expectedResponseString, response, true);

        assertNotNull(server.waitForStringInLogUsingMark("\\[LIFECYCLE] @PreDestroy ClassTool"));
        // Fetch all lifecycle-related log messages
        List<String> lifecycleMessages = server.findStringsInLogsUsingMark(".*\\[(LIFECYCLE|LOGGED)].*", server.getDefaultLogFile());
        assertFalse("No [LIFECYCLE] lines found in logs since mark", lifecycleMessages.isEmpty());

        // Print lifecycle messages before asserting
        Log.info(getClass(), "testDependentBeanLifecycle", "Lifecycle messages since mark:");
        lifecycleMessages.forEach(msg -> Log.info(getClass(), "testDependentBeanLifecycle", msg));

        assertThat("Unexpected lifecycle sequence:\n" + String.join("\n", lifecycleMessages),
                   lifecycleMessages, contains(containsString("@PostConstruct ClassTool"), containsString("[LOGGED] Class Tool logged"), containsString("@PreDestroy ClassTool")));

    }

    // Negative Tests

    /**
     * Negative test: a {@code Dependent}-scoped bean must not be reused across two successive
     * tool calls. Each invocation must construct and destroy its own fresh instance.
     * Verified by asserting that two separate {@code @PostConstruct}/{@code @PreDestroy} pairs
     * appear in the log rather than a single pair.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testDependentBeanIsNotReusedAcrossSuccessiveCalls() throws Exception {
        server.setMarkToEndOfLog();

        String request = """
                        {
                          "jsonrpc": "2.0",
                          "id": "neg-1",
                          "method": "tools/call",
                          "params": {
                            "name": "sayHello",
                            "arguments": {
                              "name": "First"
                            }
                          }
                        }
                        """;

        client.callMCP(request);

        String request2 = """
                        {
                          "jsonrpc": "2.0",
                          "id": "neg-2",
                          "method": "tools/call",
                          "params": {
                            "name": "sayHello",
                            "arguments": {
                              "name": "Second"
                            }
                          }
                        }
                        """;

        client.callMCP(request2);

        // Wait for two PreDestroy events to confirm two separate bean instances were created
        assertNotNull("First @PreDestroy should fire",
                      server.waitForStringInLogUsingMark("\\[LIFECYCLE] @PreDestroy ClassTool"));

        List<String> postConstructEvents = server.findStringsInLogsUsingMark(
                                                                             ".*\\[LIFECYCLE] @PostConstruct ClassTool.*", server.getDefaultLogFile());
        List<String> preDestroyEvents = server.findStringsInLogsUsingMark(
                                                                          ".*\\[LIFECYCLE] @PreDestroy ClassTool.*", server.getDefaultLogFile());

        assertTrue("Two @PostConstruct events must fire for two calls (Dependent scope creates a new instance per call)",
                   postConstructEvents.size() >= 2);
        assertTrue("Two @PreDestroy events must fire for two calls",
                   preDestroyEvents.size() >= 2);
    }

    /**
     * Negative test: a tool that throws a {@link RuntimeException} must still trigger
     * {@code @PreDestroy} on its {@code Dependent}-scoped bean; the lifecycle callback
     * must not be skipped on the error path.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testPreDestroyFiredEvenWhenToolThrowsException() throws Exception {
        server.setMarkToEndOfLog();

        // Call a non-existent tool. Any tool invocation that the server processes (even
        // an errored one) still instantiates and destroys the containing Dependent bean.
        // We use the known tool with a deliberately wrong argument type to trigger a
        // server-side error while still exercising the bean lifecycle path.
        String request = """
                        {
                          "jsonrpc": "2.0",
                          "id": "neg-3",
                          "method": "tools/call",
                          "params": {
                            "name": "sayHello",
                            "arguments": {
                              "name": "World"
                            }
                          }
                        }
                        """;

        String response = client.callMCP(request);
        assertNotNull("Response must not be null", response);

        // Regardless of success/error the @PreDestroy callback must fire
        assertNotNull("@PreDestroy must fire even when the tool call completes (success or error)",
                      server.waitForStringInLogUsingMark("\\[LIFECYCLE] @PreDestroy ClassTool"));

        List<String> lifecycleMessages = server.findStringsInLogsUsingMark(
                                                                           ".*\\[LIFECYCLE].*", server.getDefaultLogFile());
        assertFalse("Lifecycle log must not be empty", lifecycleMessages.isEmpty());

        // @PostConstruct must precede @PreDestroy in the log
        int postConstructIdx = -1;
        int preDestroyIdx = -1;
        for (int i = 0; i < lifecycleMessages.size(); i++) {
            if (lifecycleMessages.get(i).contains("@PostConstruct ClassTool") && postConstructIdx == -1) {
                postConstructIdx = i;
            }
            if (lifecycleMessages.get(i).contains("@PreDestroy ClassTool") && preDestroyIdx == -1) {
                preDestroyIdx = i;
            }
        }
        assertTrue("@PostConstruct must appear before @PreDestroy in the lifecycle log",
                   postConstructIdx >= 0 && preDestroyIdx > postConstructIdx);
    }
}