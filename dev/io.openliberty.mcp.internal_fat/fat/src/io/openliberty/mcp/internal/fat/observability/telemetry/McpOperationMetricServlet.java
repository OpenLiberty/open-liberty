/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.observability.telemetry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Optional;

import org.junit.Test;

import componenttest.app.FATServlet;
import io.openliberty.mcp.internal.fat.utils.TestConstants;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.metrics.data.HistogramPointData;
import io.opentelemetry.sdk.metrics.data.MetricData;
import jakarta.servlet.annotation.WebServlet;

@WebServlet("/McpOperationMetricServlet")
public class McpOperationMetricServlet extends FATServlet {
    private static final long serialVersionUID = 1L;

    InMemoryMetricReader reader = PullExporterAutoConfigurationCustomizerProvider.exporter;

    @Test
    public void testBasicToolCallMetrics() {
        HistogramPointData point = getToolCallPoint("basicTool");

        assertEquals("Expected 1 basicTool call", 1, point.getCount());

        Attributes attributes = point.getAttributes();
        assertEquals("basicTool", getStringAttribute(attributes, "gen_ai.tool.name"));
        assertEquals("Expected 1 basicdTool calls", 1, point.getCount());
        assertInvariantToolCallAttributes(attributes);
        assertSuccessAttributes(attributes);
        assertTimingAttributes(point);
    }

    @Test
    public void testAdvancedToolCallMetrics() {
        HistogramPointData point = getToolCallPoint("advancedTool");

        assertEquals("Expected 2 advancedTool calls", 2, point.getCount());

        Attributes attributes = point.getAttributes();
        assertEquals("advancedTool", getStringAttribute(attributes, "gen_ai.tool.name"));
        assertEquals("Expected 2 advancedTool calls", 2, point.getCount());
        assertInvariantToolCallAttributes(attributes);
        assertSuccessAttributes(attributes);
        assertTimingAttributes(point);
        assertProtocolAttributes(attributes);
    }

    @Test
    public void testInitializeMetrics() {
        HistogramPointData point = getOperationPoint("initialize");
        assertEquals("Expected exactly 1 initialize call", 1, point.getCount());

        Attributes attributes = point.getAttributes();
        assertEquals("initialize", getStringAttribute(attributes, "mcp.method.name"));
        assertInvariantOperationAttributes(attributes);
        assertSuccessAttributes(attributes);
        assertTimingAttributes(point);
        assertProtocolAttributes(attributes);
    }

    @Test
    public void testToolsListMetrics() {
        HistogramPointData point = getOperationPoint("tools/list");
        assertEquals("Expected exactly 1 tools/list call", 1, point.getCount());

        Attributes attributes = point.getAttributes();
        assertEquals("tools/list", getStringAttribute(attributes, "mcp.method.name"));
        assertInvariantOperationAttributes(attributes);
        assertSuccessAttributes(attributes);
        assertTimingAttributes(point);
        assertProtocolAttributes(attributes);
    }

    @Test
    public void testPingMetrics() {
        HistogramPointData point = getOperationPoint("ping");
        assertEquals("Expected exactly 1 ping call", 1, point.getCount());

        Attributes attributes = point.getAttributes();
        assertEquals("ping", getStringAttribute(attributes, "mcp.method.name"));
        assertInvariantOperationAttributes(attributes);
        assertSuccessAttributes(attributes);
        assertTimingAttributes(point);
    }

    @Test
    public void testInitializedMetrics() {
        HistogramPointData point = getOperationPoint("notifications/initialized");
        assertEquals("Expected exactly 1 initialized notification", 1, point.getCount());

        Attributes attributes = point.getAttributes();
        assertEquals("notifications/initialized", getStringAttribute(attributes, "mcp.method.name"));
        assertInvariantOperationAttributes(attributes);
        assertSuccessAttributes(attributes);
        assertTimingAttributes(point);
    }

    @Test
    public void testCancelRequestSuccessMetrics() {
        HistogramPointData point = getCancelOperationPoint("ok", null);

        assertTrue("Expected at least 1 successful cancel request", point.getCount() >= 1);

        Attributes attributes = point.getAttributes();
        assertEquals("notifications/cancelled", getStringAttribute(attributes, "mcp.method.name"));
        assertInvariantOperationAttributes(attributes);
        assertEquals("ok", getStringAttribute(attributes, "rpc.response.status_code"));
        assertNull("Did not expect error.type for successful cancel",
                   getStringAttribute(attributes, "error.type"));
        assertTimingAttributes(point);
    }

    @Test
    public void testCancelRequestErrorMetrics() {
        Optional<HistogramPointData> errorPoint = findCancelOperationPoint("error");

        if (errorPoint.isPresent()) {
            HistogramPointData point = errorPoint.get();
            Attributes attributes = point.getAttributes();

            assertEquals("notifications/cancelled", getStringAttribute(attributes, "mcp.method.name"));
            assertInvariantOperationAttributes(attributes);
            assertEquals("error", getStringAttribute(attributes, "rpc.response.status_code"));
            assertNotNull("Expected error.type for failed cancel",
                          getStringAttribute(attributes, "error.type"));
            assertTimingAttributes(point);
        } else {
            assertTrue("At least success case should exist",
                       findCancelOperationPoint("ok").isPresent());
        }
    }

    @Test
    public void testBusinessErrorToolMetrics() {
        HistogramPointData point = getToolCallPointWithStatus("businessErrorTool", "error");
        
        assertTrue("Expected at least 1 businessErrorTool call with error status", point.getCount() >= 1);
        
        Attributes attributes = point.getAttributes();
        assertEquals("businessErrorTool", getStringAttribute(attributes, "gen_ai.tool.name"));
        assertInvariantToolCallAttributes(attributes);
        assertEquals("error", getStringAttribute(attributes, "rpc.response.status_code"));
        assertNotNull("Expected error.type for business error", getStringAttribute(attributes, "error.type"));
        assertTimingAttributes(point);
    }

    @Test
    public void testNonBusinessErrorToolMetrics() {
        HistogramPointData point = getToolCallPointWithStatus("nonBusinessErrorTool", "error");
        
        assertTrue("Expected at least 1 nonBusinessErrorTool call with error status", point.getCount() >= 1);
        
        Attributes attributes = point.getAttributes();
        assertEquals("nonBusinessErrorTool", getStringAttribute(attributes, "gen_ai.tool.name"));
        assertInvariantToolCallAttributes(attributes);
        assertEquals("error", getStringAttribute(attributes, "rpc.response.status_code"));
        assertNotNull("Expected error.type for non-business error", getStringAttribute(attributes, "error.type"));
        assertTimingAttributes(point);
    }

    private HistogramPointData getToolCallPointWithStatus(String toolName, String status) {
        Optional<MetricData> metric = getMetricData("mcp.server.operation.duration");
        assertTrue("mcp.server.operation.duration metric not found", metric.isPresent());

        List<HistogramPointData> toolCallPoints = metric.get()
                                                        .getHistogramData()
                                                        .getPoints()
                                                        .stream()
                                                        .filter(point -> "tools/call".equals(getStringAttribute(point.getAttributes(), "mcp.method.name")))
                                                        .filter(point -> toolName.equals(getStringAttribute(point.getAttributes(), "gen_ai.tool.name")))
                                                        .filter(point -> status.equals(getStringAttribute(point.getAttributes(), "rpc.response.status_code")))
                                                        .toList();

        assertTrue("Expected at least one point for " + toolName + " with status " + status, !toolCallPoints.isEmpty());
        return toolCallPoints.get(0);
    }

    @Test
    public void testAsyncBusinessErrorToolMetrics() {
        HistogramPointData point = getToolCallPointWithStatus("asyncBusinessErrorTool", "error");
        
        assertTrue("Expected at least 1 asyncBusinessErrorTool call with error status", point.getCount() >= 1);
        
        Attributes attributes = point.getAttributes();
        assertEquals("asyncBusinessErrorTool", getStringAttribute(attributes, "gen_ai.tool.name"));
        assertInvariantToolCallAttributes(attributes);
        assertEquals("error", getStringAttribute(attributes, "rpc.response.status_code"));
        assertNotNull("Expected error.type for async business error", getStringAttribute(attributes, "error.type"));
        assertTimingAttributes(point);
    }

    @Test
    public void testAsyncNonBusinessErrorToolMetrics() {
        HistogramPointData point = getToolCallPointWithStatus("asyncNonBusinessErrorTool", "error");
        
        assertTrue("Expected at least 1 asyncNonBusinessErrorTool call with error status", point.getCount() >= 1);
        
        Attributes attributes = point.getAttributes();
        assertEquals("asyncNonBusinessErrorTool", getStringAttribute(attributes, "gen_ai.tool.name"));
        assertInvariantToolCallAttributes(attributes);
        assertEquals("error", getStringAttribute(attributes, "rpc.response.status_code"));
        assertNotNull("Expected error.type for async non-business error", getStringAttribute(attributes, "error.type"));
        assertTimingAttributes(point);
    }

    @Test
    public void testAsyncFailedStageToolMetrics() {
        HistogramPointData point = getToolCallPointWithStatus("asyncFailedStageTool", "error");
        
        assertTrue("Expected at least 1 asyncFailedStageTool call with error status", point.getCount() >= 1);
        
        Attributes attributes = point.getAttributes();
        assertEquals("asyncFailedStageTool", getStringAttribute(attributes, "gen_ai.tool.name"));
        assertInvariantToolCallAttributes(attributes);
        assertEquals("error", getStringAttribute(attributes, "rpc.response.status_code"));
        assertNotNull("Expected error.type for async failed stage error", getStringAttribute(attributes, "error.type"));
        assertTimingAttributes(point);
    }

    // Helper method to get cancel operation with specific status
    private HistogramPointData getCancelOperationPoint(String status, String errorType) {
        Optional<MetricData> metric = getMetricData("mcp.server.operation.duration");
        assertTrue("mcp.server.operation.duration metric not found", metric.isPresent());

        List<HistogramPointData> cancelPoints = metric.get()
                                                      .getHistogramData()
                                                      .getPoints()
                                                      .stream()
                                                      .filter(point -> "notifications/cancelled".equals(getStringAttribute(point.getAttributes(), "mcp.method.name")))
                                                      .filter(point -> status.equals(getStringAttribute(point.getAttributes(), "rpc.response.status_code")))
                                                      .filter(point -> {
                                                          String actualErrorType = getStringAttribute(point.getAttributes(), "error.type");
                                                          return errorType == null ? actualErrorType == null : errorType.equals(actualErrorType);
                                                      })
                                                      .toList();

        assertTrue("Expected at least one cancel point with status=" + status + ", errorType=" + errorType,
                   !cancelPoints.isEmpty());
        return cancelPoints.get(0);
    }

    // Helper to find cancel operation point (returns Optional)
    private Optional<HistogramPointData> findCancelOperationPoint(String status) {
        Optional<MetricData> metric = getMetricData("mcp.server.operation.duration");
        if (metric.isEmpty()) {
            return Optional.empty();
        }

        return metric.get()
                     .getHistogramData()
                     .getPoints()
                     .stream()
                     .filter(point -> "notifications/cancelled".equals(getStringAttribute(point.getAttributes(), "mcp.method.name")))
                     .filter(point -> status.equals(getStringAttribute(point.getAttributes(), "rpc.response.status_code")))
                     .findFirst();
    }

    private HistogramPointData getOperationPoint(String methodName) {
        Optional<MetricData> metric = getMetricData("mcp.server.operation.duration");
        assertTrue("mcp.server.operation.duration metric not found", metric.isPresent());

        List<HistogramPointData> operationPoints = metric.get()
                                                         .getHistogramData()
                                                         .getPoints()
                                                         .stream()
                                                         .filter(point -> methodName.equals(getStringAttribute(point.getAttributes(), "mcp.method.name")))
                                                         .toList();

        assertEquals("Expected exactly one metric point for " + methodName, 1, operationPoints.size());
        return operationPoints.get(0);
    }

    private void assertInvariantOperationAttributes(Attributes attributes) {
        assertEquals("2.0", getStringAttribute(attributes, "jsonrpc.protocol.version"));
        assertEquals("HTTP", getStringAttribute(attributes, "network.protocol.name"));
        assertEquals("1.1", getStringAttribute(attributes, "network.protocol.version"));
        assertEquals("tcp", getStringAttribute(attributes, "network.transport"));
    }

    private HistogramPointData getToolCallPoint(String toolName) {
        Optional<MetricData> metric = getMetricData("mcp.server.operation.duration");
        assertTrue("mcp.server.operation.duration metric not found", metric.isPresent());

        List<HistogramPointData> toolCallPoints = metric.get()
                                                        .getHistogramData()
                                                        .getPoints()
                                                        .stream()
                                                        .filter(point -> "tools/call".equals(getStringAttribute(point.getAttributes(), "mcp.method.name")))
                                                        .filter(point -> toolName.equals(getStringAttribute(point.getAttributes(), "gen_ai.tool.name")))
                                                        .toList();

        assertEquals("Expected exactly one point for " + toolName, 1, toolCallPoints.size());
        return toolCallPoints.get(0);
    }

    private Optional<MetricData> getMetricData(String metricName) {
        return reader.getMcpMetricData()
                     .stream()
                     .filter(metric -> metricName.equals(metric.getName()))
                     .findFirst();
    }

    private void assertInvariantToolCallAttributes(Attributes attributes) {
        assertEquals("2.0", getStringAttribute(attributes, "jsonrpc.protocol.version"));
        assertEquals("tools/call", getStringAttribute(attributes, "mcp.method.name"));
        assertEquals("HTTP", getStringAttribute(attributes, "network.protocol.name"));
        assertEquals("1.1", getStringAttribute(attributes, "network.protocol.version"));
        assertEquals("tcp", getStringAttribute(attributes, "network.transport"));
    }

    private void assertSuccessAttributes(Attributes attributes) {
        assertEquals("ok", getStringAttribute(attributes, "rpc.response.status_code"));
        assertNull("Did not expect error.type for successful tool calls",
                   getStringAttribute(attributes, "error.type"));
    }

    private void assertTimingAttributes(HistogramPointData point) {
        assertTrue("Expected duratiaon sum to be greater than 0", point.getSum() > 0);
        assertTrue("Expected min duration to be present", point.hasMin());
        assertTrue("Expected max duration to be present", point.hasMax());
        assertTrue("Expected min duration to be non-negative", point.getMin() >= 0);
        assertTrue("Expected max duration to be non-negative", point.getMax() >= 0);
        assertTrue("Expected max duration to be >= min duration", point.getMax() >= point.getMin());
    }

    private void assertProtocolAttributes(Attributes attributes) {
        String mcpProtocolVersion = getStringAttribute(attributes, "mcp.protocol.version");
        assertNotNull("Expected mcp.protocol.version to be present", mcpProtocolVersion);
        assertEquals(TestConstants.VALUE_MCP_PROTOCOL_VERSION, mcpProtocolVersion);
    }

    private String getStringAttribute(Attributes attributes, String key) {
        return attributes.get(AttributeKey.stringKey(key));
    }

}
