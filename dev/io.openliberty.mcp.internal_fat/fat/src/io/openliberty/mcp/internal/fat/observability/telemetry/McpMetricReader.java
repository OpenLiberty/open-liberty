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

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.metrics.data.HistogramPointData;
import io.opentelemetry.sdk.metrics.data.MetricData;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Simplified access to MCP Telemetry metrics
 */
@ApplicationScoped
public class McpMetricReader {

    InMemoryMetricReader reader = PullExporterAutoConfigurationCustomizerProvider.exporter;

    // Helper method to get cancel operation with specific status
    public HistogramPointData getCancelOperationPoint(String status, String errorType) {
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
    public Optional<HistogramPointData> findCancelOperationPoint(String status) {
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

    public HistogramPointData getOperationPoint(String methodName) {
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

    public HistogramPointData getToolCallPoint(String toolName) {
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

    public Optional<MetricData> getMetricData(String metricName) {
        return reader.getMcpMetricData()
                     .stream()
                     .filter(metric -> metricName.equals(metric.getName()))
                     .findFirst();
    }

    public static String getStringAttribute(Attributes attributes, String key) {
        return attributes.get(AttributeKey.stringKey(key));
    }

}
