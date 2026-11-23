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
import static io.openliberty.mcp.internal.fat.suite.McpAsyncAuthServerSuite.serverLifecycle;
import static io.openliberty.mcp.internal.fat.utils.ToolResponseMatcher.toolResponse;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.mcp.internal.fat.tool.asyncToolApp.AsyncTools;
import io.openliberty.mcp.internal.fat.utils.McpClient;
import io.openliberty.mcp.internal.fat.utils.McpRequest;
import io.openliberty.mcp.internal.fat.utils.TestConstants;
import io.openliberty.mcp.internal.fat.utils.ToolResponse;
import io.openliberty.mcp.internal.fat.utils.ToolStatus;
import io.openliberty.mcp.internal.fat.utils.ToolStatusClient;

@RunWith(FATRunner.class)
public class AsyncToolCancellationTest extends FATServletClient {

    // Server is managed by McpAsyncAuthServerSuite — do NOT add @Server or copy the field here.
    private static ExecutorService executor;
    private static final String EXPECTED_ERROR = "OperationCancelledException";

    @Rule
    public McpClient client = McpClient.builder(serverLifecycle.getServer(), "/asyncToolCancellationTest").basicAuth("BobTheAdmin", "testpassword").build();

    @Rule
    public ToolStatusClient toolStatus = new ToolStatusClient(serverLifecycle.getServer(), "/asyncToolCancellationTest");

    @BeforeClass
    public static void setup() throws Exception {
        serverLifecycle.getServer().addIgnoredErrors(List.of(EXPECTED_ERROR));
        WebArchive war = ShrinkWrap.create(WebArchive.class, "asyncToolCancellationTest.war")
                                   .addPackage(AsyncTools.class.getPackage())
                                   .addPackage(ToolStatus.class.getPackage());

        serverLifecycle.getServer().setMarkToEndOfLog();
        ShrinkHelper.exportDropinAppToServer(serverLifecycle.getServer(), war, SERVER_ONLY);
        assertNotNull(serverLifecycle.getServer().waitForStringInLogUsingMark("MCP server endpoint: .*/mcp$"));

        executor = Executors.newSingleThreadExecutor();
    }

    @AfterClass
    public static void teardown() throws Exception {
        serverLifecycle.undeployDropinApp("asyncToolCancellationTest");
        executor.shutdown();
    }

    /** Builds the {@code notifications/cancelled} notification for a request that is in flight. */
    private static String cancellationNotification(McpRequest request) {
        return """
                        {
                          "jsonrpc": "2.0",
                          "method": "notifications/cancelled",
                          "params": {
                            "requestId": "%s",
                            "reason": "no longer needed"
                          }
                        }
                        """.formatted(request.getId());
    }

    @Test
    public void testCancellationToolAsync() throws Exception {
        final String latchName = "testCancellationToolAsync";

        McpRequest request = client.toolCallRequest("asyncCancellationTool", Map.of("latchName", latchName));

        Callable<ToolResponse> threadCallingTool = () -> {
            try {
                return client.callTool(request);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };

        Future<ToolResponse> future = executor.submit(threadCallingTool);

        toolStatus.awaitStarted(latchName);
        client.callMCPNotification(cancellationNotification(request));

        ToolResponse response = future.get(TestConstants.POSITIVE_TIMEOUT_MS, TimeUnit.MILLISECONDS);

        assertThat(response, toolResponse().withError("Operation was cancelled"));
    }

    @Test
    public void testOperationCancelledExceptionNotLoggedAsError() throws Exception {
        final String latchName = "testOperationCancelledExceptionNotLoggedAsError";

        McpRequest request = client.toolCallRequest("asyncCancellationTool", Map.of("latchName", latchName));

        Callable<ToolResponse> threadCallingTool = () -> {
            try {
                return client.callTool(request);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };

        Future<ToolResponse> future = executor.submit(threadCallingTool);

        toolStatus.awaitStarted(latchName);
        client.callMCPNotification(cancellationNotification(request));

        ToolResponse response = future.get(TestConstants.POSITIVE_TIMEOUT_MS, TimeUnit.MILLISECONDS);

        // Verify the response indicates cancellation
        assertThat(response, toolResponse().withError("Operation was cancelled"));

        // Verify that the internal server error message (CWMCM0010E) was NOT logged
        // This error message is only logged for non-business exceptions, and OperationCancelledException
        // should be handled specially without logging
        String errorLog = serverLifecycle.getServer().waitForStringInLog("CWMCM0010E.*asyncCancellationTool", TestConstants.NEGATIVE_TIMEOUT_MS);
        if (errorLog != null) {
            throw new AssertionError("OperationCancelledException should not be logged as an error. Found: " + errorLog);
        }
    }
}