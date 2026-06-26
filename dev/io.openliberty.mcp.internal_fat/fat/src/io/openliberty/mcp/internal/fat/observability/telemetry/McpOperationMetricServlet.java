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

import static io.openliberty.mcp.internal.fat.observability.telemetry.McpMetricReader.getStringAttribute;
import static io.openliberty.mcp.internal.fat.observability.telemetry.MetricComparator.countIncreasedBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import componenttest.app.FATServlet;
import io.openliberty.mcp.internal.fat.utils.TestConstants;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.metrics.data.HistogramPointData;
import io.opentelemetry.sdk.metrics.data.MetricData;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;

@WebServlet("/McpOperationMetricServlet")
@ApplicationScoped
public class McpOperationMetricServlet extends FATServlet {
    private static final long serialVersionUID = 1L;

    @Inject
    private McpMetricReader reader;

    private MetricData capturedMetricData;

    public void testToolCallMetrics() {
        HistogramPointData basicToolPoint = reader.getToolCallPoint("basicTool");

        Attributes basicToolAttributes = basicToolPoint.getAttributes();
        assertEquals("basicTool", getStringAttribute(basicToolAttributes, "gen_ai.tool.name"));
        assertInvariantToolCallAttributes(basicToolAttributes);
        assertSuccessAttributes(basicToolAttributes);
        assertTimingAttributes(basicToolPoint);

        HistogramPointData advancedToolPoint = reader.getToolCallPoint("advancedTool");

        Attributes advancedToolAttributes = advancedToolPoint.getAttributes();
        assertEquals("advancedTool", getStringAttribute(advancedToolAttributes, "gen_ai.tool.name"));
        assertInvariantToolCallAttributes(advancedToolAttributes);
        assertSuccessAttributes(advancedToolAttributes);
        assertTimingAttributes(advancedToolPoint);
        assertProtocolAttributes(advancedToolAttributes);

        MetricComparator.compareOperationDuration(reader)
                        .expectChange(toolCallWithStatus("basicTool", null), countIncreasedBy(1))
                        .expectChange(toolCallWithStatus("advancedTool", null), countIncreasedBy(2))
                        .runCompareAgainst(capturedMetricData);
    }

    public void testInitializeAndInitializedMetrics() {
        HistogramPointData initializePoint = reader.getOperationPoint("initialize");

        Attributes initializeAttributes = initializePoint.getAttributes();
        assertEquals("initialize", getStringAttribute(initializeAttributes, "mcp.method.name"));
        assertInvariantOperationAttributes(initializeAttributes);
        assertSuccessAttributes(initializeAttributes);
        assertTimingAttributes(initializePoint);
        assertProtocolAttributes(initializeAttributes);

        HistogramPointData initializedPoint = reader.getOperationPoint("notifications/initialized");

        Attributes initializedAttributes = initializedPoint.getAttributes();
        assertEquals("notifications/initialized", getStringAttribute(initializedAttributes, "mcp.method.name"));
        assertInvariantOperationAttributes(initializedAttributes);
        assertSuccessAttributes(initializedAttributes);
        assertTimingAttributes(initializedPoint);
        assertProtocolAttributes(initializeAttributes);

        MetricComparator.compareOperationDuration(reader)
                        .expectChange(method("initialize"), countIncreasedBy(2))
                        .expectChange(method("notifications/initialized"), countIncreasedBy(2))
                        .runCompareAgainst(capturedMetricData);
    }

    public void testToolsListMetrics() {
        HistogramPointData point = reader.getOperationPoint("tools/list");

        Attributes attributes = point.getAttributes();
        assertEquals("tools/list", getStringAttribute(attributes, "mcp.method.name"));
        assertInvariantOperationAttributes(attributes);
        assertSuccessAttributes(attributes);
        assertTimingAttributes(point);
        assertProtocolAttributes(attributes);

        MetricComparator.compareOperationDuration(reader)
                        .expectChange(method("tools/list"), countIncreasedBy(1))
                        .runCompareAgainst(capturedMetricData);
    }

    public void testPingMetrics() {
        HistogramPointData point = reader.getOperationPoint("ping");
        assertEquals("Expected exactly 1 ping call", 1, point.getCount());

        Attributes attributes = point.getAttributes();
        assertEquals("ping", getStringAttribute(attributes, "mcp.method.name"));
        assertInvariantOperationAttributes(attributes);
        assertSuccessAttributes(attributes);
        assertTimingAttributes(point);
    }

    public void testInitializedMetrics() {
        HistogramPointData initializedPoint = reader.getOperationPoint("notifications/initialized");
        assertEquals("Expected exactly 1 initialized notification", 1, initializedPoint.getCount());

        Attributes initializedAttributes = initializedPoint.getAttributes();
        assertEquals("notifications/initialized", getStringAttribute(initializedAttributes, "mcp.method.name"));
        assertInvariantOperationAttributes(initializedAttributes);
        assertSuccessAttributes(initializedAttributes);
        assertTimingAttributes(initializedPoint);
    }

    public void testCancelRequestSuccessMetrics() {
        HistogramPointData point = reader.getCancelOperationPoint("ok", null);

        assertTrue("Expected at least 1 successful cancel request", point.getCount() >= 1);

        Attributes attributes = point.getAttributes();
        assertEquals("notifications/cancelled", getStringAttribute(attributes, "mcp.method.name"));
        assertInvariantOperationAttributes(attributes);
        assertEquals("ok", getStringAttribute(attributes, "rpc.response.status_code"));
        assertNull("Did not expect error.type for successful cancel",
                   getStringAttribute(attributes, "error.type"));
        assertTimingAttributes(point);
    }

    public void testCancelRequestErrorMetrics() {
        Optional<HistogramPointData> errorPoint = reader.findCancelOperationPoint("error");

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
                       reader.findCancelOperationPoint("ok").isPresent());
        }
    }

    public void testBusinessErrorToolMetrics() {
        HistogramPointData point = getToolCallPointWithStatus("businessErrorTool", "error");

        assertTrue("Expected at least 1 businessErrorTool call with error status", point.getCount() >= 1);

        Attributes attributes = point.getAttributes();
        assertEquals("businessErrorTool", getStringAttribute(attributes, "gen_ai.tool.name"));
        assertInvariantToolCallAttributes(attributes);
        assertEquals("error", getStringAttribute(attributes, "rpc.response.status_code"));
        assertNotNull("Expected error.type for business error", getStringAttribute(attributes, "error.type"));
        assertTimingAttributes(point);

        MetricComparator.compareOperationDuration(reader)
                        .expectChange(toolCallWithStatus("businessErrorTool", "error"),
                                      (pOld, pNew) -> assertEquals("Count was not incremented", pOld.getCount() + 1, pNew.getCount()))
                        .runCompareAgainst(capturedMetricData);
    }

    public void testNonBusinessErrorToolMetrics() {
        HistogramPointData point = getToolCallPointWithStatus("nonBusinessErrorTool", "error");

        assertTrue("Expected at least 1 nonBusinessErrorTool call with error status", point.getCount() >= 1);

        Attributes attributes = point.getAttributes();
        assertEquals("nonBusinessErrorTool", getStringAttribute(attributes, "gen_ai.tool.name"));
        assertInvariantToolCallAttributes(attributes);
        assertEquals("error", getStringAttribute(attributes, "rpc.response.status_code"));
        assertNotNull("Expected error.type for non-business error", getStringAttribute(attributes, "error.type"));
        assertTimingAttributes(point);

        MetricComparator.compareOperationDuration(reader)
                        .expectChange(toolCallWithStatus("nonBusinessErrorTool", "error"), countIncreasedBy(1))
                        .runCompareAgainst(capturedMetricData);
    }

    public void testAsyncBusinessErrorToolMetrics() {
        HistogramPointData point = getToolCallPointWithStatus("asyncBusinessErrorTool", "error");

        assertTrue("Expected at least 1 asyncBusinessErrorTool call with error status", point.getCount() >= 1);

        Attributes attributes = point.getAttributes();
        assertEquals("asyncBusinessErrorTool", getStringAttribute(attributes, "gen_ai.tool.name"));
        assertInvariantToolCallAttributes(attributes);
        assertEquals("error", getStringAttribute(attributes, "rpc.response.status_code"));
        assertNotNull("Expected error.type for async business error", getStringAttribute(attributes, "error.type"));
        assertTimingAttributes(point);

        MetricComparator.compareOperationDuration(reader)
                        .expectChange(toolCallWithStatus("asyncBusinessErrorTool", "error"), countIncreasedBy(1))
                        .runCompareAgainst(capturedMetricData);
    }

    public void testAsyncNonBusinessErrorToolMetrics() {
        HistogramPointData point = getToolCallPointWithStatus("asyncNonBusinessErrorTool", "error");

        assertTrue("Expected at least 1 asyncNonBusinessErrorTool call with error status", point.getCount() >= 1);

        Attributes attributes = point.getAttributes();
        assertEquals("asyncNonBusinessErrorTool", getStringAttribute(attributes, "gen_ai.tool.name"));
        assertInvariantToolCallAttributes(attributes);
        assertEquals("error", getStringAttribute(attributes, "rpc.response.status_code"));
        assertNotNull("Expected error.type for async non-business error", getStringAttribute(attributes, "error.type"));
        assertTimingAttributes(point);

        MetricComparator.compareOperationDuration(reader)
                        .expectChange(toolCallWithStatus("asyncNonBusinessErrorTool", "error"), countIncreasedBy(1))
                        .runCompareAgainst(capturedMetricData);
    }

    public void testAsyncFailedStageToolMetrics() {
        HistogramPointData point = getToolCallPointWithStatus("asyncFailedStageTool", "error");

        assertTrue("Expected at least 1 asyncFailedStageTool call with error status", point.getCount() >= 1);

        Attributes attributes = point.getAttributes();
        assertEquals("asyncFailedStageTool", getStringAttribute(attributes, "gen_ai.tool.name"));
        assertInvariantToolCallAttributes(attributes);
        assertEquals("error", getStringAttribute(attributes, "rpc.response.status_code"));
        assertNotNull("Expected error.type for async failed stage error", getStringAttribute(attributes, "error.type"));
        assertTimingAttributes(point);

        MetricComparator.compareOperationDuration(reader)
                        .expectChange(toolCallWithStatus("asyncFailedStageTool", "error"), countIncreasedBy(1))
                        .runCompareAgainst(capturedMetricData);
    }

    public void captureOperationDurationMetrics() {
        capturedMetricData = reader.getMetricData("mcp.server.operation.duration").get();
    }

    private static void assertInvariantOperationAttributes(Attributes attributes) {
        assertEquals("2.0", getStringAttribute(attributes, "jsonrpc.protocol.version"));
        assertEquals("HTTP", getStringAttribute(attributes, "network.protocol.name"));
        assertEquals("1.1", getStringAttribute(attributes, "network.protocol.version"));
        assertEquals("tcp", getStringAttribute(attributes, "network.transport"));
    }

    private static void assertInvariantToolCallAttributes(Attributes attributes) {
        assertEquals("2.0", getStringAttribute(attributes, "jsonrpc.protocol.version"));
        assertEquals("tools/call", getStringAttribute(attributes, "mcp.method.name"));
        assertEquals("HTTP", getStringAttribute(attributes, "network.protocol.name"));
        assertEquals("1.1", getStringAttribute(attributes, "network.protocol.version"));
        assertEquals("tcp", getStringAttribute(attributes, "network.transport"));
    }

    private static void assertSuccessAttributes(Attributes attributes) {
        assertEquals("ok", getStringAttribute(attributes, "rpc.response.status_code"));
        assertNull("Did not expect error.type for successful tool calls",
                   getStringAttribute(attributes, "error.type"));
    }

    private static void assertTimingAttributes(HistogramPointData point) {
        assertTrue("Expected duratiaon sum to be greater than 0", point.getSum() > 0);
        assertTrue("Expected min duration to be present", point.hasMin());
        assertTrue("Expected max duration to be present", point.hasMax());
        assertTrue("Expected min duration to be non-negative", point.getMin() >= 0);
        assertTrue("Expected max duration to be non-negative", point.getMax() >= 0);
        assertTrue("Expected max duration to be >= min duration", point.getMax() >= point.getMin());
    }

    private static void assertProtocolAttributes(Attributes attributes) {
        String mcpProtocolVersion = getStringAttribute(attributes, "mcp.protocol.version");
        assertNotNull("Expected mcp.protocol.version to be present", mcpProtocolVersion);
        assertEquals(TestConstants.VALUE_MCP_PROTOCOL_VERSION, mcpProtocolVersion);
    }

    private HistogramPointData getToolCallPointWithStatus(String toolName, String status) {
        Optional<MetricData> metric = reader.getMetricData("mcp.server.operation.duration");
        assertTrue("mcp.server.operation.duration metric not found", metric.isPresent());

        List<HistogramPointData> toolCallPoints = metric.get()
                                                        .getHistogramData()
                                                        .getPoints()
                                                        .stream()
                                                        .filter(toolCallWithStatus(toolName, status))
                                                        .toList();

        assertTrue("Expected at least one point for " + toolName + " with status " + status, !toolCallPoints.isEmpty());
        return toolCallPoints.get(0);
    }

    private static Predicate<HistogramPointData> toolCallWithStatus(String toolName, String status) {
        return point -> "tools/call".equals(getStringAttribute(point.getAttributes(), "mcp.method.name"))
                        && toolName.equals(getStringAttribute(point.getAttributes(), "gen_ai.tool.name"))
                        && (status == null || status.equals(getStringAttribute(point.getAttributes(), "rpc.response.status_code")));
    }

    private static Predicate<HistogramPointData> method(String methodName) {
        return point -> methodName.equals(getStringAttribute(point.getAttributes(), "mcp.method.name"));
    }

}
