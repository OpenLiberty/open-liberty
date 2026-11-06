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

import static io.openliberty.mcp.internal.fat.suite.McpTelemetryServerSuite.serverLifecycle;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.skyscreamer.jsonassert.JSONCompareMode.LENIENT;
import static org.skyscreamer.jsonassert.JSONCompareMode.STRICT;

import java.time.Duration;
import java.util.List;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.JSONAssert;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.utils.FATServletClient;
import componenttest.topology.utils.HttpRequest;
import io.openliberty.mcp.internal.fat.observability.telemetry.PullExporterAutoConfigurationCustomizerProvider;
import io.openliberty.mcp.internal.fat.utils.McpClient;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;

@RunWith(FATRunner.class)
public class TelemetryOperationsTest extends FATServletClient {

    // Server is managed by McpTelemetryServerSuite — do NOT add @Server or copy the field here.
    private final static String APP_NAME = "telemetryTest";

    @Rule
    public McpClient client = McpClient.builder(serverLifecycle.getServer(), "/" + APP_NAME).build();

    private static final String BASIC_TOOL_REQUEST = """
                      {
                      "jsonrpc": "2.0",
                      "id": 2,
                      "method": "tools/call",
                      "params": {
                        "name": "basicTool",
                        "arguments": {}
                      }
                    }
                    """;

    private static final String ADVANCED_TOOL_REQUEST = """
                      {
                      "jsonrpc": "2.0",
                      "id": 2,
                      "method": "tools/call",
                      "params": {
                        "name": "advancedTool",
                        "arguments": {}
                      }
                    }
                    """;

    private static final String WAITING_TOOL_REQUEST = """
                    {
                        "jsonrpc": "2.0",
                        "id": 3,
                        "method": "tools/call",
                        "params": {
                            "name": "waitingTool",
                            "arguments": {
                                "waitMs": %d
                            }
                        }
                    }
                    """;

    private static final String ERROR_RESPONSE = """
                    {
                        "result": {
                            "isError": true
                        }
                    }
                    """;

    @BeforeClass
    public static void setup() throws Exception {
        serverLifecycle.getServer().setMarkToEndOfLog();
        WebArchive war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                                   .addPackage(PullExporterAutoConfigurationCustomizerProvider.class.getPackage())
                                   .addAsResource(new StringAsset("otel.sdk.disabled=false"),
                                                  "META-INF/microprofile-config.properties")
                                   .addAsServiceProvider(AutoConfigurationCustomizerProvider.class,
                                                         PullExporterAutoConfigurationCustomizerProvider.class);
        serverLifecycle.deployDropinApp(war);
    }

    @AfterClass
    public static void teardown() throws Exception {
        serverLifecycle.getServer().addIgnoredErrors(List.of("CWMCM0010E")); // Expected: Tool threw non-business exception
        serverLifecycle.undeployDropinApp(APP_NAME);
    }

    private static final String TOOLS_LIST_REQUEST = """
                    {
                      "jsonrpc": "2.0",
                      "id": 3,
                      "method": "tools/list",
                      "params": {}
                    }
                    """;

    private static final String PING_REQUEST = """
                    {
                      "jsonrpc": "2.0",
                      "id": 4,
                      "method": "ping",
                      "params": {}
                    }
                    """;

    @Test
    public void testInitializeAndInitializedMetrics() throws Exception {
        captureMetrics();

        // Create two new clients to cause the lifecycle methods to be called
        // Create two new clients to cause the lifecycle methods to be called
        try (McpClient clientA = McpClient.builder(serverLifecycle.getServer(), APP_NAME).build()) {
            clientA.initialize();
        }
        try (McpClient clientB = McpClient.builder(serverLifecycle.getServer(), APP_NAME).build()) {
            clientB.initialize();
        }

        FATServletClient.runTest(serverLifecycle.getServer(), APP_NAME + "/McpOperationMetricServlet", "testInitializeAndInitializedMetrics");
    }

    @Test
    public void testToolsListMetrics() throws Exception {
        captureMetrics();
        String response = client.callMCP(TOOLS_LIST_REQUEST);
        assertTrue("Tools list should succeed", response.contains("\"result\""));

        FATServletClient.runTest(serverLifecycle.getServer(), APP_NAME + "/McpOperationMetricServlet", "testToolsListMetrics");
    }

    @Test
    public void testPingMetrics() throws Exception {
        // Call ping
        String response = client.callMCP(PING_REQUEST);
        assertTrue("Ping should succeed", response.contains("\"result\""));

        FATServletClient.runTest(serverLifecycle.getServer(), APP_NAME + "/McpOperationMetricServlet", "testPingMetrics");
    }

    @Test
    public void testToolCallMetrics() throws Exception {
        captureMetrics();
        String response = client.callMCP(BASIC_TOOL_REQUEST);
        String expectedResponseString = """
                        {"id":2,"jsonrpc":"2.0","result":{"content":[{"type":"text","text": "Hello from this basic tool"}], "isError": false}}
                        """;
        JSONAssert.assertEquals(expectedResponseString, response, true);

        client.callMCP(ADVANCED_TOOL_REQUEST);
        client.callMCP(ADVANCED_TOOL_REQUEST);

        // Run servlet tests to see if metrics are collected correctly
        FATServletClient.runTest(serverLifecycle.getServer(), APP_NAME + "/McpOperationMetricServlet", "testToolCallMetrics");
    }

    @Test
    public void testToolCallDuration() throws Exception {
        int waitMs = 300;
        int toleranceMs = 20; // Accounts for OS timer resolution and scheduling jitter

        long startTime = System.nanoTime();
        String response = client.callMCP(WAITING_TOOL_REQUEST.formatted(waitMs));
        Duration clientCallDuration = Duration.ofNanos(System.nanoTime() - startTime);
        String expectedResponseString = """
                        {"id":3,"jsonrpc":"2.0","result":{"content":[{"type":"text","text": "OK"}], "isError": false}}
                        """;
        JSONAssert.assertEquals(expectedResponseString, response, STRICT);

        Duration metricDuration = getDurationMetric("waitingTool");

        assertThat(metricDuration, greaterThanOrEqualTo(Duration.ofMillis(waitMs - toleranceMs)));
        assertThat(metricDuration, lessThanOrEqualTo(clientCallDuration));
    }

    @Test
    public void testCancelRequestSuccessMetrics() throws Exception {
        // Send a normal cancel notification - should succeed
        String cancelRequest = """
                        {
                          "jsonrpc": "2.0",
                          "method": "notifications/cancelled",
                          "params": {
                            "requestId": "999",
                            "reason": "User requested cancellation"
                          }
                        }
                        """;

        client.callMCPNotification(cancelRequest);

        // Run servlet test to verify successful cancel metrics
        FATServletClient.runTest(serverLifecycle.getServer(), APP_NAME + "/McpOperationMetricServlet", "testCancelRequestSuccessMetrics");
    }

    @Test
    public void testCancelRequestErrorMetrics() throws Exception {
        String cancelRequestWithError = """
                        {
                          "jsonrpc": "2.0",
                          "method": "notifications/cancelled",
                          "params": {
                            "requestId": "999",
                            "reason": "This should cause auth an error"
                          }
                        }
                        """;

        // Send the notification with an invalid session ID — status is not asserted since
        // the server may return 404 or similar, but the error metric should still be recorded.
        client.callMCPRaw(cancelRequestWithError).sessionId("fakeSessionId").anyStatus().run();

        FATServletClient.runTest(serverLifecycle.getServer(), APP_NAME + "/McpOperationMetricServlet", "testCancelRequestErrorMetrics");
    }

    @Test
    public void testBusinessErrorToolMetrics() throws Exception {
        captureMetrics();
        String businessErrorRequest = """
                        {
                          "jsonrpc": "2.0",
                          "id": 5,
                          "method": "tools/call",
                          "params": {
                            "name": "businessErrorTool",
                            "arguments": {
                              "input": "bad-value"
                            }
                          }
                        }
                        """;

        String response = client.callMCP(businessErrorRequest);

        // Verify the response is an error
        JSONAssert.assertEquals(ERROR_RESPONSE, response, LENIENT);

        // Run servlet test to verify error metrics are recorded correctly
        FATServletClient.runTest(serverLifecycle.getServer(), APP_NAME + "/McpOperationMetricServlet", "testBusinessErrorToolMetrics");
    }

    @Test
    public void testNonBusinessErrorToolMetrics() throws Exception {
        captureMetrics();
        String nonBusinessErrorRequest = """
                        {
                          "jsonrpc": "2.0",
                          "id": 6,
                          "method": "tools/call",
                          "params": {
                            "name": "nonBusinessErrorTool",
                            "arguments": {
                              "input": "trigger-error"
                            }
                          }
                        }
                        """;

        String response = client.callMCP(nonBusinessErrorRequest);

        // Verify the response is an error
        JSONAssert.assertEquals(ERROR_RESPONSE, response, LENIENT);

        // Run servlet test to verify error metrics are recorded correctly
        FATServletClient.runTest(serverLifecycle.getServer(), APP_NAME + "/McpOperationMetricServlet", "testNonBusinessErrorToolMetrics");
    }

    @Test
    public void testAsyncBusinessErrorToolMetrics() throws Exception {
        captureMetrics();
        String asyncBusinessErrorRequest = """
                        {
                          "jsonrpc": "2.0",
                          "id": 7,
                          "method": "tools/call",
                          "params": {
                            "name": "asyncBusinessErrorTool",
                            "arguments": {
                              "input": "bad-value"
                            }
                          }
                        }
                        """;

        String response = client.callMCP(asyncBusinessErrorRequest);

        // Verify the response is an error
        JSONAssert.assertEquals(ERROR_RESPONSE, response, LENIENT);

        // Run servlet test to verify async error metrics are recorded correctly
        FATServletClient.runTest(serverLifecycle.getServer(), APP_NAME + "/McpOperationMetricServlet", "testAsyncBusinessErrorToolMetrics");
    }

    @Test
    public void testAsyncNonBusinessErrorToolMetrics() throws Exception {
        captureMetrics();
        String asyncNonBusinessErrorRequest = """
                        {
                          "jsonrpc": "2.0",
                          "id": 8,
                          "method": "tools/call",
                          "params": {
                            "name": "asyncNonBusinessErrorTool",
                            "arguments": {
                              "input": "trigger-error"
                            }
                          }
                        }
                        """;

        String response = client.callMCP(asyncNonBusinessErrorRequest);

        // Verify the response is an error
        JSONAssert.assertEquals(ERROR_RESPONSE, response, LENIENT);

        // Run servlet test to verify async error metrics are recorded correctly
        FATServletClient.runTest(serverLifecycle.getServer(), APP_NAME + "/McpOperationMetricServlet", "testAsyncNonBusinessErrorToolMetrics");
    }

    @Test
    public void testAsyncFailedStageToolMetrics() throws Exception {
        captureMetrics();
        String asyncFailedStageRequest = """
                        {
                          "jsonrpc": "2.0",
                          "id": 9,
                          "method": "tools/call",
                          "params": {
                            "name": "asyncFailedStageTool",
                            "arguments": {
                              "input": "trigger-error"
                            }
                          }
                        }
                        """;

        String response = client.callMCP(asyncFailedStageRequest);

        // Verify the response is an error
        JSONAssert.assertEquals(ERROR_RESPONSE, response, LENIENT);

        // Run servlet test to verify async failed stage error metrics are recorded correctly
        FATServletClient.runTest(serverLifecycle.getServer(), APP_NAME + "/McpOperationMetricServlet", "testAsyncFailedStageToolMetrics");
    }

    @Test
    public void testParseErrorMetrics() throws Exception {
        captureMetrics();
        client.callMCP("this is not json");
        FATServletClient.runTest(serverLifecycle.getServer(), APP_NAME + "/McpOperationMetricServlet", "testParseErrorMetrics");
    }

    @Test
    public void testInvalidRequestMetrics() throws Exception {
        captureMetrics();
        client.callMCP("""
                        {
                          "jsonrpc": "1.0",
                          "id": 99,
                          "method": "tools/list"
                        }
                        """);
        FATServletClient.runTest(serverLifecycle.getServer(), APP_NAME + "/McpOperationMetricServlet", "testInvalidRequestMetrics");
    }

    @Test
    public void testMethodNotFoundMetrics() throws Exception {
        captureMetrics();
        client.callMCP("""
                        {
                          "jsonrpc": "2.0",
                          "id": 100,
                          "method": "unknown/method",
                          "params": {}
                        }
                        """);
        FATServletClient.runTest(serverLifecycle.getServer(), APP_NAME + "/McpOperationMetricServlet", "testMethodNotFoundMetrics");
    }

    /**
     * Call McpMetricDurationServlet and get the recorded total duration for executions of a tool
     */
    private Duration getDurationMetric(String toolName) throws Exception {
        HttpRequest req = new HttpRequest(serverLifecycle.getServer(), "/", APP_NAME, "/McpMetricDuration", "?toolName=", toolName);
        String response = req.run(String.class);
        double durationSeconds = Double.valueOf(response);
        return Duration.ofNanos((long) (durationSeconds * 1_000_000_000D));
    }

    /**
     * Capture the current state of the MCP Operation metrics, for later comparison
     */
    private void captureMetrics() throws Exception {
        runTest(serverLifecycle.getServer(), APP_NAME + "/McpOperationMetricServlet", "captureOperationDurationMetrics");
    }

}
