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
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Optional;

import org.junit.Test;

import componenttest.app.FATServlet;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.metrics.data.HistogramPointData;
import io.opentelemetry.sdk.metrics.data.MetricData;
import jakarta.servlet.annotation.WebServlet;

@WebServlet("/McpMetricServlet")
public class McpMetricServlet extends FATServlet {
    private static final long serialVersionUID = 1L;

    InMemoryMetricReader reader = PullExporterAutoConfigurationCustomizerProvider.exporter;

    @Test
    public void testBasicToolCallMetrics() {
        Optional<MetricData> getMetricAttributes = getMetricData("mcp.server.operation.duration");
        assertTrue("mcp.server.operation.duration metric not found", getMetricAttributes.isPresent());

        List<HistogramPointData> basicToolAttributesList = getToolMetricAttributes(getMetricAttributes.get(), "basicTool");
        assertTrue("No attributes with the tool name 'basicTool'", basicToolAttributesList.size() == 1);

        // Test tool call count is correct
        assertEquals(1, basicToolAttributesList.get(0).getCount());

        Attributes basicToolAttributes = basicToolAttributesList.get(0).getAttributes();
        // Test correct values for MCP histogram attributes
        assertEquals("basicTool", basicToolAttributes.get(AttributeKey.stringKey("gen_ai.tool.name")));
        assertEquals("2.0", basicToolAttributes.get(AttributeKey.stringKey("jsonrpc.protocol.version")));
        assertEquals("tools/call", basicToolAttributes.get(AttributeKey.stringKey("mcp.method.name")));
        assertEquals("V_2025_11_25", basicToolAttributes.get(AttributeKey.stringKey("mcp.protocol.version")));
        assertEquals("HTTP", basicToolAttributes.get(AttributeKey.stringKey("network.protocol.name")));
        assertEquals("1.1", basicToolAttributes.get(AttributeKey.stringKey("network.protocol.version")));
        assertEquals("tcp", basicToolAttributes.get(AttributeKey.stringKey("network.transport")));

    }

    @Test
    public void testAdvancedToolCallMetrics() {
        Optional<MetricData> getMetricAttributes = getMetricData("mcp.server.operation.duration");
        assertTrue("mcp.server.operation.duration metric not found", getMetricAttributes.isPresent());

        List<HistogramPointData> advancedToolAttributesList = getToolMetricAttributes(getMetricAttributes.get(), "advancedTool");
        assertTrue("No attributes with the tool name 'advancedTool'", advancedToolAttributesList.size() == 1);

        // Test tool call count is correct
        assertEquals(2, advancedToolAttributesList.get(0).getCount());

        Attributes advancedToolAttributes = advancedToolAttributesList.get(0).getAttributes();
        // Test correct values for MCP histogram attributes
        assertEquals("advancedTool", advancedToolAttributes.get(AttributeKey.stringKey("gen_ai.tool.name")));
        assertEquals("2.0", advancedToolAttributes.get(AttributeKey.stringKey("jsonrpc.protocol.version")));
        assertEquals("tools/call", advancedToolAttributes.get(AttributeKey.stringKey("mcp.method.name")));
        assertEquals("V_2025_11_25", advancedToolAttributes.get(AttributeKey.stringKey("mcp.protocol.version")));
        assertEquals("HTTP", advancedToolAttributes.get(AttributeKey.stringKey("network.protocol.name")));
        assertEquals("1.1", advancedToolAttributes.get(AttributeKey.stringKey("network.protocol.version")));
        assertEquals("tcp", advancedToolAttributes.get(AttributeKey.stringKey("network.transport")));

    }

    private Optional<MetricData> getMetricData(String metricName) {
        return reader.getMcpMetricData()
                     .stream()
                     .filter(metric -> metric.getName().equals(metricName))
                     .findFirst();
    }

    private List<HistogramPointData> getToolMetricAttributes(MetricData metricData, String toolName) {
        return metricData.getHistogramData()
                         .getPoints()
                         .stream()
                         .filter(point -> toolName.equals(point.getAttributes().get(AttributeKey.stringKey("gen_ai.tool.name"))))
                         .toList();
    }

}
