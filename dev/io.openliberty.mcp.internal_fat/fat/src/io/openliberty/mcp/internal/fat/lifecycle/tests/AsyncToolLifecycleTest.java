/*******************************************************************************
 * Copyright (c) 2025, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.lifecycle.tests;

import static io.openliberty.mcp.internal.fat.suite.McpAsyncServerSuite.serverLifecycle;
import static io.openliberty.mcp.internal.fat.utils.ToolResponseMatcher.toolResponse;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.custom.junit.runner.FATRunner;
import io.openliberty.mcp.internal.fat.tool.asyncToolApp.AsyncLifecycleTools;
import io.openliberty.mcp.internal.fat.utils.McpClient;
import io.openliberty.mcp.internal.fat.utils.ToolResponse;
import io.openliberty.mcp.internal.fat.utils.ToolStatus;
import io.openliberty.mcp.internal.fat.utils.ToolStatusClient;

@SuppressWarnings("unchecked")
@RunWith(FATRunner.class)
public class AsyncToolLifecycleTest {

    // Server is managed by McpAsyncServerSuite — do NOT add @Server here.
    // Do NOT copy McpAsyncServerSuite.server into a static field here:
    // the suite's @BeforeClass runs after test-class static initializers,
    // so a static copy would capture null.

    private static final String APP_NAME = "asyncToolLifecycleTest";

    @Rule
    public ToolStatusClient toolStatus = new ToolStatusClient(serverLifecycle.getServer(), "/" + APP_NAME);

    @Rule
    public McpClient client = McpClient.builder(serverLifecycle.getServer(), "/" + APP_NAME).build();

    @BeforeClass
    public static void setup() throws Exception {
        serverLifecycle.getServer().addIgnoredErrors(List.of("Method call caused runtime exception. This is expected if the input was 'throw error'"));
        serverLifecycle.getServer().setMarkToEndOfLog();

        WebArchive war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                                   .addPackage(AsyncLifecycleTools.class.getPackage())
                                   .addPackage(ToolStatus.class.getPackage());

        serverLifecycle.deployDropinApp(war);

        assertNotNull(serverLifecycle.getServer().waitForStringInLogUsingMark("MCP server endpoint: .*/mcp$"));
    }

    @AfterClass
    public static void teardown() throws Exception {
        serverLifecycle.undeployDropinApp(APP_NAME);
    }

    @Test
    public void testAsyncDependentBeanLifecycleForCompleteCompletionStage() throws Exception {
        serverLifecycle.getServer().setMarkToEndOfLog();
        ToolResponse response = client.callTool("asyncLifecycleCompleteCompletionStage", Map.of("input", "Hello, World"));

        assertThat(response, toolResponse().withTextContent("Hello, World: (async)").withNoStructuredContent());

        assertNotNull(serverLifecycle.getServer().waitForStringInLogUsingMark("\\[LIFECYCLE] @PreDestroy AsyncLifecycleTools"));

        // Fetch all lifecycle-related log messages
        List<String> lifecycleMessages = serverLifecycle.getServer().findStringsInLogsUsingMark(".*\\[(LIFECYCLE|LOGGED)].*", serverLifecycle.getServer().getDefaultLogFile());
        assertFalse("No [LIFECYCLE] lines found in logs since mark", lifecycleMessages.isEmpty());

        assertThat("Unexpected lifecycle sequence:\n" + String.join("\n", lifecycleMessages),
                   lifecycleMessages,
                   contains(containsString("@PostConstruct AsyncLifecycleTools"),
                            containsString("[LOGGED] AsyncLifecycleTools.asyncLifecycleCompleteCompletionStage Tool logged"),
                            containsString("@PreDestroy AsyncLifecycleTools")));
    }

    @Test
    public void testAsyncDependentBeanLifecycleForAsyncStage() throws Exception {
        serverLifecycle.getServer().setMarkToEndOfLog();
        ToolResponse response = client.callTool("asyncLifecycleAsyncStage", Map.of("input", "Hello, World"));

        assertThat(response, toolResponse().withTextContent("Hello, World: (async)").withNoStructuredContent());

        assertNotNull(serverLifecycle.getServer().waitForStringInLogUsingMark("\\[LIFECYCLE] @PreDestroy AsyncLifecycleTools"));

        // Fetch all lifecycle-related log messages
        List<String> lifecycleMessages = serverLifecycle.getServer().findStringsInLogsUsingMark(".*\\[(LIFECYCLE|LOGGED)].*", serverLifecycle.getServer().getDefaultLogFile());
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
        serverLifecycle.getServer().setMarkToEndOfLog();
        ToolResponse response = client.callTool("asyncLifecycleCompleteCompletionStage", Map.of("input", "throw error"));

        assertThat(response, toolResponse().withError("An internal server error occurred while running the tool."));

        assertNotNull(serverLifecycle.getServer().waitForStringInLogUsingMark("\\[LIFECYCLE] @PreDestroy AsyncLifecycleTools"));

        List<String> lifecycleMessages = serverLifecycle.getServer().findStringsInLogsUsingMark(".*\\[(LIFECYCLE|LOGGED)].*", serverLifecycle.getServer().getDefaultLogFile());
        assertFalse("No [LIFECYCLE] lines found in logs since mark", lifecycleMessages.isEmpty());

        assertThat("Unexpected lifecycle sequence:\n" + String.join("\n", lifecycleMessages),
                   lifecycleMessages,
                   contains(containsString("@PostConstruct AsyncLifecycleTools"),
                            containsString("[LOGGED] AsyncLifecycleTools.asyncLifecycleCompleteCompletionStage Tool logged"),
                            containsString("[LOGGED] AsyncLifecycleTools.asyncLifecycleCompleteCompletionStage Tool throwing error"),
                            containsString("@PreDestroy AsyncLifecycleTools")));

        // asyncLifecycleAsyncStage - exception thrown synchronously before returning a stage;
        // @PreDestroy must still fire (CDI-dependent lifecycle must be honoured on the error path)
        serverLifecycle.getServer().setMarkToEndOfLog();
        ToolResponse response2 = client.callTool("asyncLifecycleAsyncStage", Map.of("input", "throw error"));

        assertThat(response2, toolResponse().withError("An internal server error occurred while running the tool."));

        assertNotNull(serverLifecycle.getServer().waitForStringInLogUsingMark("\\[LIFECYCLE] @PreDestroy AsyncLifecycleTools"));

        List<String> lifecycleMessages2 = serverLifecycle.getServer().findStringsInLogsUsingMark(".*\\[(LIFECYCLE|LOGGED)].*", serverLifecycle.getServer().getDefaultLogFile());
        assertFalse("No [LIFECYCLE] lines found in logs since mark", lifecycleMessages2.isEmpty());

        assertThat("Unexpected lifecycle sequence:\n" + String.join("\n", lifecycleMessages2),
                   lifecycleMessages2,
                   contains(containsString("@PostConstruct AsyncLifecycleTools"),
                            containsString("[LOGGED] AsyncLifecycleTools.asyncLifecycleAsyncStage Tool logged"),
                            containsString("[LOGGED] AsyncLifecycleTools.asyncLifecycleAsyncStage Tool throwing error"),
                            containsString("@PreDestroy AsyncLifecycleTools")));
    }

    // Negative Tests

    /**
     * Verifies that an async {@code @Dependent}-scoped bean is not reused across successive tool calls —
     * each invocation must produce its own {@code @PostConstruct}/{@code @PreDestroy} pair,
     * in the correct order.
     */
    @Test
    public void testAsyncDependentBeanIsNotReusedAcrossSuccessiveCalls() throws Exception {
        serverLifecycle.getServer().setMarkToEndOfLog();

        client.callTool("asyncLifecycleCompleteCompletionStage", Map.of("input", "First"));
        client.callTool("asyncLifecycleCompleteCompletionStage", Map.of("input", "Second"));

        // Wait for both @PreDestroy events - each call must destroy its own instance
        long startTime = System.nanoTime();
        boolean found = false;
        while (System.nanoTime() - startTime < Duration.ofSeconds(30).toNanos()) {
            List<String> predestroyMessages = serverLifecycle.getServer().findStringsInLogsUsingMark("\\[LIFECYCLE] @PreDestroy AsyncLifecycleTools",
                                                                                                     serverLifecycle.getServer().getDefaultLogFile());

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
                   contains(containsString("@PostConstruct AsyncLifecycleTools"),
                            containsString("[LOGGED] AsyncLifecycleTools.asyncLifecycleCompleteCompletionStage Tool logged"),
                            containsString("@PreDestroy AsyncLifecycleTools"),
                            containsString("@PostConstruct AsyncLifecycleTools"),
                            containsString("[LOGGED] AsyncLifecycleTools.asyncLifecycleCompleteCompletionStage Tool logged"),
                            containsString("@PreDestroy AsyncLifecycleTools")));
    }
}