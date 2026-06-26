/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.monitor.mcpstatstests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.lang.management.ManagementFactory;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.openliberty.mcp.internal.monitor.McpSessionStatistics;
import io.openliberty.mcp.internal.monitor.metrics.McpSessionStatAttributes;

/**
 * Test class for McpSessionStats JMX bean functionality.
 * Verifies that JMX beans can be registered and expose correct session attributes.
 */
public class McpSessionStatsJMXTest {

    private McpSessionStatAttributes testAttributes;
    private McpSessionStatistics mcpSessionStats;
    private MBeanServer mbs;
    private ObjectName objectName;

    @Before
    public void setUp() throws Exception {
        // Create test attributes for session
        testAttributes = McpSessionStatAttributes.builder()
                                                 .withErrorType("connection_error")
                                                 .withJsonrpcProtocolVersion("2.0")
                                                 .withMcpProtocolVersion("V_2025_11_25")
                                                 .withNetworkProtocolName("HTTP")
                                                 .withNetworkProtocolVersion("1.1")
                                                 .withNetworkTransport("tcp")
                                                 .build().get();

        mcpSessionStats = new McpSessionStatistics(testAttributes);
        mbs = ManagementFactory.getPlatformMBeanServer();
        objectName = new ObjectName("io.openliberty.mcp.test:type=McpSessionStatistics,name=test");
    }

    @After
    public void tearDown() throws Exception {
        if (mbs != null && objectName != null && mbs.isRegistered(objectName)) {
            mbs.unregisterMBean(objectName);
        }
    }

    @Test
    public void testJMXBeanRegistration() throws Exception {
        // Register the MBean
        mbs.registerMBean(mcpSessionStats, objectName);

        // Verify it's registered
        Set<ObjectName> names = mbs.queryNames(objectName, null);
        assertEquals("MBean should be registered", 1, names.size());
    }

    @Test
    public void testReadSessionAttributesThroughJMX() throws Exception {
        mbs.registerMBean(mcpSessionStats, objectName);

        // Read session attributes through JMX
        String errorType = (String) mbs.getAttribute(objectName, "ErrorType");
        assertEquals("Should read error type through JMX", "connection_error", errorType);

        String jsonrpcVersion = (String) mbs.getAttribute(objectName, "JsonrpcProtocolVersion");
        assertEquals("Should read JSON-RPC version through JMX", "2.0", jsonrpcVersion);

        String mcpVersion = (String) mbs.getAttribute(objectName, "McpProtocolVersion");
        assertEquals("Should read MCP version through JMX", "V_2025_11_25", mcpVersion);

        String protocolName = (String) mbs.getAttribute(objectName, "NetworkProtocolName");
        assertEquals("Should read protocol name through JMX", "HTTP", protocolName);

        String protocolVersion = (String) mbs.getAttribute(objectName, "NetworkProtocolVersion");
        assertEquals("Should read protocol version through JMX", "1.1", protocolVersion);

        String transport = (String) mbs.getAttribute(objectName, "NetworkTransport");
        assertEquals("Should read transport through JMX", "tcp", transport);

        Long count = (Long) mbs.getAttribute(objectName, "Count");
        assertEquals("Should read count through JMX", Long.valueOf(0), count);

        var countData = (CompositeData) mbs.getAttribute(objectName, "CountDetails");
        assertEquals("Count should be updated", 0L, countData.get("currentValue"));

        Double duration = (Double) mbs.getAttribute(objectName, "Duration");
        assertEquals("Should read duration through JMX", 0.0, duration, 0.001);

        var durationData = (CompositeData) mbs.getAttribute(objectName, "DurationDetails");
        assertEquals("Duration should be updated", 0.0, (Double) durationData.get("mean"), 1.0);
        assertEquals("DurationDetails count should be updated", Long.valueOf(0), durationData.get("count"));
    }

    @Test
    public void testUpdateSessionMetricsThroughJMX() throws Exception {
        mbs.registerMBean(mcpSessionStats, objectName);

        // Update session metrics
        mcpSessionStats.incrementSessionCountBy(3);
        mcpSessionStats.addSessionDurationStat(5_000_000_000L); // 5 seconds in nanos

        // Read updated values through JMX
        Long count = (Long) mbs.getAttribute(objectName, "Count");
        assertEquals("Session count should be updated", Long.valueOf(3), count);

        var countData = (CompositeData) mbs.getAttribute(objectName, "CountDetails");
        assertEquals("Count should be updated", 3L, countData.get("currentValue"));

        Double duration = (Double) mbs.getAttribute(objectName, "Duration");
        assertEquals("Session duration should be updated", 5_000_000_000.0, duration, 1.0);

        var durationData = (CompositeData) mbs.getAttribute(objectName, "DurationDetails");
        assertEquals("Duration should be updated", 5_000_000_000.0, (Double) durationData.get("mean"), 1.0);
        assertEquals("DurationDetails count should be updated", Long.valueOf(1), durationData.get("count"));
    }

    @Test
    public void testGetAllSessionAttributes() throws Exception {
        mbs.registerMBean(mcpSessionStats, objectName);

        // Verify all session attributes are accessible
        assertNotNull(mbs.getAttribute(objectName, "ErrorType"));
        assertNotNull(mbs.getAttribute(objectName, "JsonrpcProtocolVersion"));
        assertNotNull(mbs.getAttribute(objectName, "McpProtocolVersion"));
        assertNotNull(mbs.getAttribute(objectName, "NetworkProtocolName"));
        assertNotNull(mbs.getAttribute(objectName, "NetworkProtocolVersion"));
        assertNotNull(mbs.getAttribute(objectName, "NetworkTransport"));
        assertNotNull(mbs.getAttribute(objectName, "Count"));
        assertNotNull(mbs.getAttribute(objectName, "CountDetails"));
        assertNotNull(mbs.getAttribute(objectName, "Duration"));
        assertNotNull(mbs.getAttribute(objectName, "DurationDetails"));
    }

    @Test
    public void testNullOptionalSessionAttributes() throws Exception {
        // Create session stats with minimal attributes
        McpSessionStatAttributes minimalAttrs = McpSessionStatAttributes.builder()
                                                                        .build().get();
        McpSessionStatistics minimalStats = new McpSessionStatistics(minimalAttrs);

        ObjectName minimalName = new ObjectName("io.openliberty.mcp.test:type=McpSessionStatistics,name=minimal");
        mbs.registerMBean(minimalStats, minimalName);

        try {
            // Verify optional attributes can be null
            String errorType = (String) mbs.getAttribute(minimalName, "ErrorType");
            assertNull("Error type should be null", errorType);

            String jsonrpcVersion = (String) mbs.getAttribute(minimalName, "JsonrpcProtocolVersion");
            assertNull("JSON-RPC version should be null", jsonrpcVersion);

            // Count and duration should still be accessible
            Long count = (Long) mbs.getAttribute(minimalName, "Count");
            assertEquals("Count should be 0", Long.valueOf(0), count);

            Double duration = (Double) mbs.getAttribute(minimalName, "Duration");
            assertEquals("Duration should be 0.0", 0.0, duration, 0.001);
        } finally {
            mbs.unregisterMBean(minimalName);
        }
    }

    @Test
    public void testMultipleSessionUpdates() throws Exception {
        mbs.registerMBean(mcpSessionStats, objectName);

        // Add multiple session duration measurements
        mcpSessionStats.addSessionDurationStat(1_000_000_000L); // 1 second
        mcpSessionStats.addSessionDurationStat(2_000_000_000L); // 2 seconds
        mcpSessionStats.addSessionDurationStat(3_000_000_000L); // 3 seconds

        // Total should be 6 seconds
        Double duration = (Double) mbs.getAttribute(objectName, "Duration");
        assertEquals("Total duration should be sum of all sessions",
                     6_000_000_000.0, duration, 1.0);

        var durationData = (CompositeData) mbs.getAttribute(objectName, "DurationDetails");
        assertEquals("Duration should be updated", 2_000_000_000.0, (Double) durationData.get("mean"), 1.0);
        assertEquals("DurationDetails count should be updated", Long.valueOf(3), durationData.get("count"));
        assertEquals("DurationDetails total should be updated", 6_000_000_000.0, durationData.get("total"));
    }
}
