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

        List<HistogramPointData> toolCallPoints = getMetricAttributes.get()
                                                                     .getHistogramData()
                                                                     .getPoints()
                                                                     .stream()
                                                                     .filter(point -> "tools/call".equals(
                                                                                                          point.getAttributes().get(
                                                                                                                                    AttributeKey.stringKey("mcp.method.name"))))
                                                                     .toList();

        assertEquals("Expected one aggregated tools/call point", 1, toolCallPoints.size());

        HistogramPointData point = toolCallPoints.get(0);

        assertEquals(3, point.getCount());

        Attributes attributes = point.getAttributes();
        assertEquals("2.0", attributes.get(AttributeKey.stringKey("jsonrpc.protocol.version")));
        assertEquals("tools/call", attributes.get(AttributeKey.stringKey("mcp.method.name")));
        assertEquals("V_2025_11_25", attributes.get(AttributeKey.stringKey("mcp.protocol.version")));
        assertEquals("HTTP", attributes.get(AttributeKey.stringKey("network.protocol.name")));
        assertEquals("1.1", attributes.get(AttributeKey.stringKey("network.protocol.version")));
        assertEquals("tcp", attributes.get(AttributeKey.stringKey("network.transport")));
        assertEquals("ok", attributes.get(AttributeKey.stringKey("rpc.response.status_code")));
    }

    @Test
    public void testAdvancedToolCallMetrics() {
        Optional<MetricData> getMetricAttributes = getMetricData("mcp.server.operation.duration");
        assertTrue("mcp.server.operation.duration metric not found", getMetricAttributes.isPresent());

        List<HistogramPointData> toolCallPoints = getMetricAttributes.get()
                                                                     .getHistogramData()
                                                                     .getPoints()
                                                                     .stream()
                                                                     .filter(point -> "tools/call".equals(
                                                                                                          point.getAttributes().get(
                                                                                                                                    AttributeKey.stringKey("mcp.method.name"))))
                                                                     .toList();

        assertEquals("Expected one aggregated tools/call point", 1, toolCallPoints.size());

        HistogramPointData point = toolCallPoints.get(0);

        assertEquals(3, point.getCount());

        Attributes attributes = point.getAttributes();
        assertEquals("2.0", attributes.get(AttributeKey.stringKey("jsonrpc.protocol.version")));
        assertEquals("tools/call", attributes.get(AttributeKey.stringKey("mcp.method.name")));
        assertEquals("V_2025_11_25", attributes.get(AttributeKey.stringKey("mcp.protocol.version")));
        assertEquals("HTTP", attributes.get(AttributeKey.stringKey("network.protocol.name")));
        assertEquals("1.1", attributes.get(AttributeKey.stringKey("network.protocol.version")));
        assertEquals("tcp", attributes.get(AttributeKey.stringKey("network.transport")));
        assertEquals("ok", attributes.get(AttributeKey.stringKey("rpc.response.status_code")));
    }

    private Optional<MetricData> getMetricData(String metricName) {
        return reader.getMcpMetricData()
                     .stream()
                     .filter(metric -> metric.getName().equals(metricName))
                     .findFirst();
    }

}
