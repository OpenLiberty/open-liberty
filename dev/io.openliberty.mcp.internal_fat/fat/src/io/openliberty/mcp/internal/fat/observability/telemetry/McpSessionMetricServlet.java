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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;

import java.util.List;
import java.util.Optional;

import org.junit.Test;

import componenttest.app.FATServlet;
import io.openliberty.mcp.internal.fat.utils.TestConstants;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.metrics.data.HistogramPointData;
import io.opentelemetry.sdk.metrics.data.MetricData;
import jakarta.servlet.annotation.WebServlet;

@WebServlet("/McpSessionMetricServlet")
public class McpSessionMetricServlet extends FATServlet {
    private static final long serialVersionUID = 1L;

    InMemoryMetricReader reader = PullExporterAutoConfigurationCustomizerProvider.exporter;

    @Test
    public void testSessionDurationMetrics() {
        HistogramPointData point = getSuccessfulSessionPoint();

        assertEquals("Expected exactly 1 session", 1, point.getCount());

        Attributes attributes = point.getAttributes();
        assertInvariantSessionAttributes(attributes);
        assertSuccessfulSessionAttributes(attributes);
        assertTimingAttributes(point);
    }

    @Test
    public void testSessionTimeoutMetrics() {
        HistogramPointData point = getTimeoutSessionPoint();

        assertEquals("Expected exactly 1 timeout session", 1, point.getCount());

        Attributes attributes = point.getAttributes();
        assertInvariantSessionAttributes(attributes);
        assertTimeoutSessionAttributes(attributes);
        assertTimingAttributes(point);
    }

    private Optional<MetricData> getMetricData(String metricName) {
        return reader.getMcpMetricData()
                     .stream()
                     .filter(metric -> metricName.equals(metric.getName()))
                     .findFirst();
    }

    private HistogramPointData getTimeoutSessionPoint() {
        Optional<MetricData> metric = getMetricData("mcp.server.session.duration");
        assertTrue("mcp.server.session.duration metric not found", metric.isPresent());

        List<HistogramPointData> timeoutSessions = metric.get()
                                                         .getHistogramData()
                                                         .getPoints()
                                                         .stream()
                                                         .filter(point -> "timeout".equals(getStringAttribute(point.getAttributes(), "error.type")))
                                                         .toList();

        assertEquals("Expected exactly one timeout session point", 1, timeoutSessions.size());
        return timeoutSessions.get(0);
    }

    private void assertTimeoutSessionAttributes(Attributes attrs) {
        assertEquals("Expected error.type to be 'timeout'",
                     "timeout",
                     getStringAttribute(attrs, "error.type"));
    }

    private HistogramPointData getSuccessfulSessionPoint() {
        Optional<MetricData> metric = getMetricData("mcp.server.session.duration");
        assertTrue("mcp.server.session.duration metric not found", metric.isPresent());

        List<HistogramPointData> successfulSessions = metric.get()
                                                            .getHistogramData()
                                                            .getPoints()
                                                            .stream()
                                                            .filter(point -> getStringAttribute(point.getAttributes(), "error.type") == null)
                                                            .toList();

        assertEquals("Expected exactly one successful session point", 1, successfulSessions.size());
        return successfulSessions.get(0);
    }

    private void assertInvariantSessionAttributes(Attributes attrs) {
        assertEquals("2.0", getStringAttribute(attrs, "jsonrpc.protocol.version"));
        assertEquals(TestConstants.VALUE_MCP_PROTOCOL_VERSION, getStringAttribute(attrs, "mcp.protocol.version"));
        assertEquals("HTTP", getStringAttribute(attrs, "network.protocol.name"));
        assertEquals("1.1", getStringAttribute(attrs, "network.protocol.version"));
        assertEquals("tcp", getStringAttribute(attrs, "network.transport"));
    }

    private void assertSuccessfulSessionAttributes(Attributes attrs) {
        assertNull("Did not expect error.type for successful session end",
                   getStringAttribute(attrs, "error.type"));
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
