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

@WebServlet("/McpMetricServlet")
public class McpMetricServlet extends FATServlet {
    private static final long serialVersionUID = 1L;

    InMemoryMetricReader reader = PullExporterAutoConfigurationCustomizerProvider.exporter;

    @Test
    public void testToolCallMetrics() {
        HistogramPointData point = getAggregatedToolsCallPoint();

        // Aggregated tools/call point currently includes
        assertEquals("Expected aggregated tools/call count to be 3", 3, point.getCount());

        Attributes attributes = point.getAttributes();
        assertInvariantToolCallAttributes(attributes);
        assertSuccessAttributes(attributes);
        assertTimingAttributes(point);
        assertProtocolAttributes(attributes);
    }

    private HistogramPointData getAggregatedToolsCallPoint() {
        Optional<MetricData> metric = getMetricData("mcp.server.operation.duration");
        assertTrue("mcp.server.operation.duration metric not found", metric.isPresent());

        List<HistogramPointData> toolCallPoints = metric.get()
                                                        .getHistogramData()
                                                        .getPoints()
                                                        .stream()
                                                        .filter(point -> "tools/call".equals(
                                                                                             getStringAttribute(point.getAttributes(), "mcp.method.name")))
                                                        .toList();

        assertEquals("Expected exactly one aggregated tools/call point", 1, toolCallPoints.size());
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