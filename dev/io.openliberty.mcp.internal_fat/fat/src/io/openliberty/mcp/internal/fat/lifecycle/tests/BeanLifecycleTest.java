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

import static io.openliberty.mcp.internal.fat.suite.McpMainServerSuite.serverLifecycle;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.time.Duration;
import java.util.List;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.JSONAssert;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.FATRunner;
import io.openliberty.mcp.internal.fat.lifecycle.tools.ClassTool;
import io.openliberty.mcp.internal.fat.utils.McpClient;

/**
 *
 */
@SuppressWarnings("unused")
@RunWith(FATRunner.class)
public class BeanLifecycleTest {

    // Server is managed by McpMainServerSuite — do NOT add @Server or copy the field here.
    private static final String APP_NAME = "beanLifecycleTest";

    @Rule
    public McpClient client = McpClient.builder(serverLifecycle.getServer(), "/" + APP_NAME).build();


    public static void info(Class<?> clazz, String method, String message) {
        Log.info(clazz, method, message);
    }

    @BeforeClass
    public static void setup() throws Exception {
        serverLifecycle.getServer().setMarkToEndOfLog();
        WebArchive war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                                   .addPackage(ClassTool.class.getPackage());
        serverLifecycle.deployDropinApp(war);
    }

    @AfterClass
    public static void teardown() throws Exception {
        serverLifecycle.undeployDropinApp(APP_NAME);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testDependentBeanLifecycle() throws Exception {
        serverLifecycle.getServer().setMarkToEndOfLog();
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

        assertNotNull(serverLifecycle.getServer().waitForStringInLogUsingMark("\\[LIFECYCLE] @PreDestroy ClassTool"));
        // Fetch all lifecycle-related log messages
        List<String> lifecycleMessages = serverLifecycle.getServer().findStringsInLogsUsingMark(".*\\[(LIFECYCLE|LOGGED)].*", serverLifecycle.getServer().getDefaultLogFile());
        assertFalse("No [LIFECYCLE] lines found in logs since mark", lifecycleMessages.isEmpty());

        // Print lifecycle messages before asserting
        Log.info(getClass(), "testDependentBeanLifecycle", "Lifecycle messages since mark:");
        lifecycleMessages.forEach(msg -> Log.info(getClass(), "testDependentBeanLifecycle", msg));

        assertThat("Unexpected lifecycle sequence:\n" + String.join("\n", lifecycleMessages),
                   lifecycleMessages, contains(containsString("@PostConstruct ClassTool"), containsString("[LOGGED] Class Tool logged"), containsString("@PreDestroy ClassTool")));

    }

    // Negative Tests

    /**
     * Verifies that a {@code @Dependent}-scoped bean is not reused across successive tool calls —
     * each invocation must produce its own {@code @PostConstruct}/{@code @PreDestroy} pair,
     * in the correct order.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testDependentBeanIsNotReusedAcrossSuccessiveCalls() throws Exception {
        serverLifecycle.getServer().setMarkToEndOfLog();

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

        // Wait for both @PreDestroy events - each call must destroy its own instance
        long startTime = System.nanoTime();
        boolean found = false;
        while (System.nanoTime() - startTime < Duration.ofSeconds(30).toNanos()) {
            List<String> predestroyMessages = serverLifecycle.getServer().findStringsInLogsUsingMark("\\[LIFECYCLE] @PreDestroy ClassTool", serverLifecycle.getServer().getDefaultLogFile());

            if (predestroyMessages.size() >= 2) {
                found = true;
                break;
            }

            Thread.sleep(100);
        }
        if (!found) {
            fail("Not found two PreDestroy messages");
        }

        List<String> lifecycleMessages = serverLifecycle.getServer().findStringsInLogsUsingMark(".*\\[(LIFECYCLE|LOGGED)].*", serverLifecycle.getServer().getDefaultLogFile());
        assertFalse("No [LIFECYCLE] lines found in logs since mark", lifecycleMessages.isEmpty());

        assertThat("Unexpected lifecycle sequence:\n" + String.join("\n", lifecycleMessages),
                   lifecycleMessages,
                   contains(containsString("@PostConstruct ClassTool"),
                            containsString("[LOGGED] Class Tool logged"),
                            containsString("@PreDestroy ClassTool"),
                            containsString("@PostConstruct ClassTool"),
                            containsString("[LOGGED] Class Tool logged"),
                            containsString("@PreDestroy ClassTool")));
    }
}