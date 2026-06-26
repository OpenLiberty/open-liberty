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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;

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
import componenttest.topology.utils.FATServletClient;
import componenttest.topology.utils.HttpRequest;
import io.openliberty.mcp.internal.fat.observability.mpmetrics.McpMpMetricBean;
import io.openliberty.mcp.internal.fat.observability.mpmetrics.MpMetricsParser;
import io.openliberty.mcp.internal.fat.observability.mpmetrics.MpMetricsParser.MetricEntry;
import io.openliberty.mcp.internal.fat.utils.McpClient;
import io.openliberty.mcp.internal.fat.utils.TestConstants;

@RunWith(FATRunner.class)
public class MpMetricsOperationsTest extends FATServletClient {

    private final static String APP_NAME = "mpMetricsTest";

    @Server("mcp-server-mpmetric")
    public static LibertyServer server;

    @Rule
    public McpClient client = new McpClient(server, "/" + APP_NAME);

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

    @BeforeClass
    public static void setup() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                                   .addPackage(McpMpMetricBean.class.getPackage());
        ShrinkHelper.exportDropinAppToServer(server, war, SERVER_ONLY);
        server.startServer();
    }

    @AfterClass
    public static void teardown() throws Exception {
        server.stopServer("CWWKS9113E");
    }

    @Test
    public void mcpMetricHistogramTest() throws Exception {
        String response = client.callMCP(BASIC_TOOL_REQUEST);
        String expectedResponseString = """
                        {"id":2,"jsonrpc":"2.0","result":{"content":[{"type":"text","text": "Hello from this basic tool"}], "isError": false}}
                        """;
        JSONAssert.assertEquals(expectedResponseString, response, true);

        client.callMCP(ADVANCED_TOOL_REQUEST);
        client.callMCP(ADVANCED_TOOL_REQUEST);

        List<MetricEntry> mcpDurationMetrics = getMcpOperationMpMetrics();
        assertInvariantToolCallAttributes(mcpDurationMetrics);
        assertToolCallCount(mcpDurationMetrics);
        assertSuccessAttributes(mcpDurationMetrics);
        assertTimingAttributes(mcpDurationMetrics);
        assertProtocolAttributes(mcpDurationMetrics);
    }

    private List<MetricEntry> getMcpOperationMpMetrics() throws Exception {
        String mpMetricsOutput = getMpMetricsOutput();
        List<MetricEntry> parsedMetrics = MpMetricsParser.parse(mpMetricsOutput).getMetrics();
        List<MetricEntry> mcpDurationMetrics = MpMetricsParser.filterMetricsByNameContains(parsedMetrics, "mcp_server_operation_duration_nanoseconds");
        assertTrue("No mpMetrics with the name 'mcp_server_operation_duration_nanoseconds' found", mcpDurationMetrics.size() > 1);
        return mcpDurationMetrics;
    }

    private void assertInvariantToolCallAttributes(List<MetricEntry> mcpMetrics) {
        Map<String, String> expectedMcpMpMetricsTags = Map.of(
                                                              "mcp_method_name", "tools/call",
                                                              "jsonrpc_protocol_version", "2.0",
                                                              "network_protocol_name", "HTTP",
                                                              "network_protocol_version", "1.1",
                                                              "network_transport", "tcp");

        List<MetricEntry> metricsWithExpectedTags = MpMetricsParser.getMetricsByTags(mcpMetrics, expectedMcpMpMetricsTags);
        assertTrue(metricsWithExpectedTags.size() > 1);
    }

    private void assertToolCallCount(List<MetricEntry> mcpMetrics) {
        List<MetricEntry> toolCallCountMetrics = MpMetricsParser.filterMetricsByName(mcpMetrics, "mcp_server_operation_duration_nanoseconds_count");
        assertTrue("Could not find count metrics for the tools basicTool and advancedTool", toolCallCountMetrics.size() > 2);

        List<MetricEntry> basicToolCallCountMetrics = MpMetricsParser.getMetricsByTags(toolCallCountMetrics, Map.of("gen_ai_tool_name", "basicTool"));
        List<MetricEntry> advancedToolCallCountMetrics = MpMetricsParser.getMetricsByTags(toolCallCountMetrics, Map.of("gen_ai_tool_name", "advancedTool"));
        assertEquals("There should only exist one count metric for the tool basicTool", 1, basicToolCallCountMetrics.size());
        assertEquals("There should only exist one count metric for the tool advancedTool", 1, advancedToolCallCountMetrics.size());

        // Compares the actual count and expected count of the tool calls with a delta of 0 (delta - the maximum difference allowed between expected and actual values)
        assertEquals(1, basicToolCallCountMetrics.get(0).getValue(), 0);
        assertEquals(2, advancedToolCallCountMetrics.get(0).getValue(), 0);

    }

    private void assertSuccessAttributes(List<MetricEntry> mcpMetrics) {
        List<MetricEntry> metricsWithSuccessResponse = MpMetricsParser.getMetricsByTags(mcpMetrics,
                                                                                        Map.of("rpc_response_status_code", "ok"));
        assertTrue("Expected all MCP mpMetrics to have a successful response status", metricsWithSuccessResponse.size() == mcpMetrics.size());

        List<MetricEntry> metricsWithErrorResponse = MpMetricsParser.getMetricsByTags(mcpMetrics,
                                                                                      Map.of("error_type", ""));
        assertEquals("Expect error_type tags to be empty for all metrics as there should be no errors", mcpMetrics.size(), metricsWithErrorResponse.size());
    }

    private void assertTimingAttributes(List<MetricEntry> mcpMetrics) {
        List<MetricEntry> basicToolCallCountMetrics = MpMetricsParser.getMetricsByTags(mcpMetrics, Map.of("gen_ai_tool_name", "basicTool"));
        List<MetricEntry> advancedToolCallCountMetrics = MpMetricsParser.getMetricsByTags(mcpMetrics, Map.of("gen_ai_tool_name", "advancedTool"));

        List<MetricEntry> basicToolMaxMetric = MpMetricsParser.filterMetricsByName(basicToolCallCountMetrics, "mcp_server_operation_duration_nanoseconds_max");
        List<MetricEntry> advancedToolMaxMetric = MpMetricsParser.filterMetricsByName(advancedToolCallCountMetrics, "mcp_server_operation_duration_nanoseconds_max");

        assertEquals("Expected one max duration mpMetric to be present for basic tool", 1, basicToolMaxMetric.size());
        assertEquals("Expected one max duration mpMetric to be present for advanced tool", 1, advancedToolMaxMetric.size());

        double basicToolMaxDuration = basicToolMaxMetric.get(0).getValue();
        double advancedToolMaxDuration = advancedToolMaxMetric.get(0).getValue();

        // Lower bound: 100 microseconds (100,000 nanoseconds)
        double minExpectedNanos = 100_000; // 100μs

        // Upper bound: 5 seconds (5,000,000,000 nanoseconds)
        double maxExpectedNanos = 5_000_000_000.0; // 5s

        assertTrue("Expected the basic tool call max duration to be more than " + minExpectedNanos, basicToolMaxDuration > minExpectedNanos);
        assertTrue("Expected the basic tool call max duration to be less than " + maxExpectedNanos, basicToolMaxDuration < maxExpectedNanos);
        assertTrue("Expected the advanced tool call max duration to be more than " + minExpectedNanos, advancedToolMaxDuration > minExpectedNanos);
        assertTrue("Expected the advanced tool call max duration to be less than " + maxExpectedNanos, advancedToolMaxDuration < maxExpectedNanos);
    }

    private void assertProtocolAttributes(List<MetricEntry> mcpMetrics) {
        String expectedMcpProtocolVersion = TestConstants.VALUE_MCP_PROTOCOL_VERSION;
        List<MetricEntry> metricsWithExpectedMcpProtocolVersion = MpMetricsParser.getMetricsByTags(mcpMetrics,
                                                                                                   Map.of("mcp_protocol_version", expectedMcpProtocolVersion));
        assertTrue("Expected all MCP mpMetrics to have the MCP Protocol Version " + expectedMcpProtocolVersion, metricsWithExpectedMcpProtocolVersion.size() == mcpMetrics.size());
    }

    private String getMpMetricsOutput() throws Exception {
        final HttpRequest request = new HttpRequest(server, "/metrics");
        request.method("GET");
        request.requestProp("Accept", "text/plain");
        return request.run(String.class);
    }

}
