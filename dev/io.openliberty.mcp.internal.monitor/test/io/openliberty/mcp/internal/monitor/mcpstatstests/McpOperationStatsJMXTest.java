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
import java.util.Optional;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.openliberty.mcp.internal.monitor.McpOperationStatistics;
import io.openliberty.mcp.internal.monitoring.internal.McpOperationStatAttributes;

/**
 * Test class for McpOperationStats JMX bean functionality.
 * Verify that JMX beans can be registered and expose correct attributes.
 */
public class McpOperationStatsJMXTest {

    private McpOperationStatAttributes testAttributes;
    private McpOperationStatistics mcpStats;
    private MBeanServer mbs;
    private ObjectName objectName;

    @Before
    public void setUp() throws Exception {
        // Create test attributes
        testAttributes = McpOperationStatAttributes.builder()
                .withMcpMethodName("tools/call")
                .withGenAiToolName(Optional.of("testTool"))
                .withRpcResponseStatusCode(Optional.of("ok"))
                .withJsonrpcProtocolVersion(Optional.of("2.0"))
                .withMcpProtocolVersion(Optional.of("V_2025_11_25"))
                .withNetworkProtocolName(Optional.of("HTTP"))
                .withNetworkProtocolVersion(Optional.of("1.1"))
                .withNetworkTransport(Optional.of("tcp"))
                .build();

        mcpStats = new McpOperationStatistics(testAttributes);
        mbs = ManagementFactory.getPlatformMBeanServer();
        objectName = new ObjectName("io.openliberty.mcp.test:type=McpOperationStatistics,name=test");
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
        mbs.registerMBean(mcpStats, objectName);

        // Verify it's registered
        Set<ObjectName> names = mbs.queryNames(objectName, null);
        assertEquals("MBean should be registered", 1, names.size());
    }

    @Test
    public void testReadAttributesThroughJMX() throws Exception {
        mbs.registerMBean(mcpStats, objectName);

        // Read attributes through JMX
        String methodName = (String) mbs.getAttribute(objectName, "McpMethodName");
        assertEquals("Should read method name through JMX", "tools/call", methodName);

        String toolName = (String) mbs.getAttribute(objectName, "GenAiToolName");
        assertEquals("Should read tool name through JMX", "testTool", toolName);

        String statusCode = (String) mbs.getAttribute(objectName, "RpcResponseStatusCode");
        assertEquals("Should read status code through JMX", "ok", statusCode);

        Long count = (Long) mbs.getAttribute(objectName, "Count");
        assertEquals("Should read count through JMX", Long.valueOf(0), count);

        Double duration = (Double) mbs.getAttribute(objectName, "Duration");
        assertEquals("Should read duration through JMX", 0.0, duration, 0.001);
    }

    @Test
    public void testUpdateMetricsThroughJMX() throws Exception {
        mbs.registerMBean(mcpStats, objectName);

        // Update metrics
        mcpStats.incrementToolCallCountBy(5);
        mcpStats.addToolTimeStat(1_000_000_000L); // 1 second in nanos

        // Read updated values through JMX
        Long count = (Long) mbs.getAttribute(objectName, "Count");
        assertEquals("Count should be updated", Long.valueOf(5), count);

        Double duration = (Double) mbs.getAttribute(objectName, "Duration");
        assertEquals("Duration should be updated", 1_000_000_000.0, duration, 1.0);
    }

    @Test
    public void testGetAllAttributes() throws Exception {
        mbs.registerMBean(mcpStats, objectName);

        // Verify all attributes are accessible
        assertNotNull(mbs.getAttribute(objectName, "McpMethodName"));
        assertNotNull(mbs.getAttribute(objectName, "GenAiToolName"));
        assertNotNull(mbs.getAttribute(objectName, "RpcResponseStatusCode"));
        assertNotNull(mbs.getAttribute(objectName, "JsonrpcProtocolVersion"));
        assertNotNull(mbs.getAttribute(objectName, "NetworkProtocolName"));
        assertNotNull(mbs.getAttribute(objectName, "NetworkProtocolVersion"));
        assertNotNull(mbs.getAttribute(objectName, "NetworkTransport"));
        assertNotNull(mbs.getAttribute(objectName, "Count"));
        assertNotNull(mbs.getAttribute(objectName, "Duration"));
    }

    @Test
    public void testNullOptionalAttributes() throws Exception {
        // Create stats with minimal attributes
        McpOperationStatAttributes minimalAttrs = McpOperationStatAttributes.builder()
                .withMcpMethodName("initialize")
                .build();
        McpOperationStatistics minimalStats = new McpOperationStatistics(minimalAttrs);

        ObjectName minimalName = new ObjectName("io.openliberty.mcp.test:type=McpOperationStatistics,name=minimal");
        mbs.registerMBean(minimalStats, minimalName);

        try {
            // Verify required attribute
            String methodName = (String) mbs.getAttribute(minimalName, "McpMethodName");
            assertEquals("Method name should be set", "initialize", methodName);

            // Verify optional attributes can be null
            String errorType = (String) mbs.getAttribute(minimalName, "ErrorType");
            assertNull("Error type should be null", errorType);

            String toolName = (String) mbs.getAttribute(minimalName, "GenAiToolName");
            assertNull("Tool name should be null", toolName);
        } finally {
            mbs.unregisterMBean(minimalName);
        }
    }
}
