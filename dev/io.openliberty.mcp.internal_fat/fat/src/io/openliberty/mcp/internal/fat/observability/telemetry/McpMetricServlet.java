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
import java.util.stream.Collectors;

import org.junit.Test;

import componenttest.app.FATServlet;
import io.opentelemetry.api.common.AttributeKey;
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

    private List<HistogramPointData> getToolMetricAttributes(MetricData metricData, String toolName) {
        return metricData.getHistogramData()
                         .getPoints()
                         .stream()
                         .filter(point -> toolName.equals(point.getAttributes().get(AttributeKey.stringKey("gen_ai.tool.name"))))
                         .toList();
    }

    private List<HistogramPointData> getSessionMetricAttributes(MetricData metricData, String toolName) {
        return metricData.getHistogramData()
                         .getPoints()
                         .stream()
                         .toList();
    }

    @Test
    public void testSessionDurationMetrics() {
        Optional<MetricData> getMetricAttributes = getMetricData("mcp.server.session.duration");
        assertTrue("mcp.server.session.duration metric not found", getMetricAttributes.isPresent());

        System.out.println(getMetricAttributes.get());

        List<HistogramPointData> sessionMetrics = getMetricAttributes.get()
                                                                     .getHistogramData()
                                                                     .getPoints()
                                                                     .stream()
                                                                     .toList();

        // Test that at least one session was recorded
        assertTrue("No session metrics recorded", sessionMetrics.size() >= 1);

        HistogramPointData sessionPoint = sessionMetrics.get(0);

        // Test session count is correct (at least 1 session)
        assertTrue("Session count should be at least 1", sessionPoint.getCount() >= 1);

        // Test that duration is reasonable (greater than 0)
        assertTrue("Session duration should be greater than 0", sessionPoint.getSum() > 0);

        Attributes sessionAttributes = sessionPoint.getAttributes();

        // Test correct values for MCP session histogram attributes
        assertEquals("2.0", sessionAttributes.get(AttributeKey.stringKey("jsonrpc.protocol.version")));
        assertEquals("V_2025_11_25", sessionAttributes.get(AttributeKey.stringKey("mcp.protocol.version")));
        assertEquals("HTTP", sessionAttributes.get(AttributeKey.stringKey("network.protocol.name")));
        assertEquals("1.1", sessionAttributes.get(AttributeKey.stringKey("network.protocol.version")));
        assertEquals("tcp", sessionAttributes.get(AttributeKey.stringKey("network.transport")));
    }

    @Test
    public void testSessionTimeoutMetrics() {
        // Get session duration metrics
        Optional<MetricData> metricData = getMetricData("mcp.server.session.duration");
        assertTrue("mcp.server.session.duration metric not found", metricData.isPresent());

        System.out.println(metricData.get());

        // Get all session metric points
        List<HistogramPointData> allSessions = metricData.get()
                                                         .getHistogramData()
                                                         .getPoints()
                                                         .stream()
                                                         .collect(Collectors.toList());

        // Filter for timeout sessions (those with error.type="timeout")
        List<HistogramPointData> timeoutSessions = allSessions.stream()
                                                              .filter(point -> {
                                                                  String errorType = point.getAttributes().get(AttributeKey.stringKey("error.type"));
                                                                  return "timeout".equals(errorType);
                                                              })
                                                              .collect(Collectors.toList());

        // Verify we have at least one timeout session
        assertTrue("Expected at least 1 timeout session metric, found: " + timeoutSessions.size(),
                   timeoutSessions.size() >= 1);

        HistogramPointData timeoutPoint = timeoutSessions.get(0);

        // Verify the timeout session was counted
        assertTrue("Timeout session count should be at least 1, was: " + timeoutPoint.getCount(),
                   timeoutPoint.getCount() >= 1);

        // Verify duration was recorded (should be ~1 second)
        assertTrue("Timeout session duration should be > 0, was: " + timeoutPoint.getSum(),
                   timeoutPoint.getSum() > 0);

        // Verify it has the correct attributes
        io.opentelemetry.api.common.Attributes attrs = timeoutPoint.getAttributes();
        assertEquals("timeout", attrs.get(AttributeKey.stringKey("error.type")));
        assertEquals("2.0", attrs.get(AttributeKey.stringKey("jsonrpc.protocol.version")));
        assertEquals("V_2025_11_25", attrs.get(AttributeKey.stringKey("mcp.protocol.version")));
        assertEquals("HTTP", attrs.get(AttributeKey.stringKey("network.protocol.name")));
        assertEquals("1.1", attrs.get(AttributeKey.stringKey("network.protocol.version")));
        assertEquals("tcp", attrs.get(AttributeKey.stringKey("network.transport")));
    }

}
