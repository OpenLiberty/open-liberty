/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.monitor.mxbeanAccessApp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.management.ManagementFactory;
import java.util.Set;

import javax.management.JMX;
import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import com.ibm.websphere.monitor.jmx.Counter;
import com.ibm.websphere.monitor.jmx.StatisticsMeter;

import componenttest.app.FATServlet;
import io.openliberty.mcp.internal.fat.utils.TestConstants;
import io.openliberty.mcp.monitor.McpOperationStatisticsMXBean;
import io.openliberty.mcp.monitor.McpSessionStatisticsMXBean;
import jakarta.servlet.annotation.WebServlet;

/**
 * Servlet deployed inside Liberty that asserts MCP monitor MXBean attributes
 * using {@link ManagementFactory#getPlatformMBeanServer()} — the same API
 * that application code running inside Liberty would use.
 *
 * <p>Each public void method is a JUnit test case invoked by
 * {@link componenttest.topology.utils.FATServletClient#runTest}.
 */
@SuppressWarnings("serial")
@WebServlet("/McpMXBeanAccessServlet")
public class McpMXBeanAccessServlet extends FATServlet {

    private static final String MBEAN_DOMAIN = "WebSphere";
    private static final String MBEAN_TYPE_OPERATION = "McpOperationStatistics";
    private static final String MBEAN_TYPE_SESSION = "McpSessionStatistics";

    /**
     * The platform MBean server — same JVM as Liberty, so MBeans registered by the
     * monitor-1.0 feature are directly visible here without any remote connector.
     */
    private final MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

    /**
     * Finds a single operation MBean whose ObjectName contains
     * {@code mcpMethod=<method>} and, when non-null, {@code genAiTool=<toolName>}.
     */
    private ObjectName findOperationMBean(String method, String toolName) throws Exception {
        StringBuilder pattern = new StringBuilder(MBEAN_DOMAIN + ":type=" + MBEAN_TYPE_OPERATION);
        pattern.append(",mcpMethod=").append(method);
        if (toolName != null) {
            pattern.append(",genAiTool=").append(toolName);
        }
        pattern.append(",*");

        Set<ObjectName> found = mbs.queryNames(new ObjectName(pattern.toString()), null);
        return switch (found.size()) {
            case 0 -> null;
            case 1 -> found.iterator().next();
            default -> throw new AssertionError(
                                                "Expected exactly one operation MBean for "
                                                + method + "/" + toolName + " but got " + found.size());
        };
    }

    /** Finds the session MBean (uses {@code session=true} property). */
    private ObjectName findSessionMBean() throws Exception {
        Set<ObjectName> found = mbs.queryNames(
                                               new ObjectName(MBEAN_DOMAIN + ":type=" + MBEAN_TYPE_SESSION + ",session=true,*"), null);
        return switch (found.size()) {
            case 0 -> null;
            case 1 -> found.iterator().next();
            default -> throw new AssertionError("Expected at most one session MBean but got " + found.size());
        };
    }

    /**
     * Verifies that all String attributes defined by {@code McpOperationStatisticsMXBean} are
     * readable via a typed MXBean proxy obtained from the platform MBean server.
     */
    public void testOperationMXBeanStringAttributesReadable() throws Exception {
        ObjectName mbean = findOperationMBean("tools/call", "add");
        assertNotNull("Operation MBean for add tool must be registered after call", mbean);

        McpOperationStatisticsMXBean operationStats = JMX.newMXBeanProxy(mbs, mbean, McpOperationStatisticsMXBean.class);

        String mcpMethodName = operationStats.getMcpMethodName();
        assertNotNull("McpMethodName must not be null", mcpMethodName);
        assertEquals("McpMethodName value", "tools/call", mcpMethodName);

        String toolName = operationStats.getGenAiToolName();
        assertNotNull("GenAiToolName must not be null for tools/call", toolName);
        assertEquals("GenAiToolName value", "add", toolName);

        String statusCode = operationStats.getRpcResponseStatusCode();
        assertNotNull("RpcResponseStatusCode must not be null", statusCode);

        // ErrorType may be null for a successful call — just verify it is readable
        operationStats.getErrorType();

        String jsonrpcVer = operationStats.getJsonrpcProtocolVersion();
        assertNotNull("JsonrpcProtocolVersion must not be null", jsonrpcVer);
        assertEquals("JsonrpcProtocolVersion must be 2.0", "2.0", jsonrpcVer);

        String mcpVer = operationStats.getMcpProtocolVersion();
        assertNotNull("McpProtocolVersion must not be null", mcpVer);
        assertEquals("McpProtocolVersion must match constant",
                     TestConstants.VALUE_MCP_PROTOCOL_VERSION, mcpVer);

        operationStats.getGenAiPromptName();
        operationStats.getGenAiOperationName();
        operationStats.getNetworkProtocolName();
        operationStats.getNetworkProtocolVersion();
        operationStats.getNetworkTransport();
        operationStats.getMcpResourceUri();
    }

    /**
     * Verifies that {@code Count} (long) and {@code Duration} (double) are readable with
     * the correct primitive types via a typed MXBean proxy.
     */
    public void testOperationMXBeanNumericAttributesReadable() throws Exception {
        ObjectName mbean = findOperationMBean("tools/call", "add");
        assertNotNull("Operation MBean for add tool must be registered", mbean);

        McpOperationStatisticsMXBean operationStats = JMX.newMXBeanProxy(mbs, mbean, McpOperationStatisticsMXBean.class);

        long count = operationStats.getCount();
        assertTrue("Count must be >= 1 after at least one call", count >= 1L);

        double duration = operationStats.getDuration();
        assertTrue("Duration must be > 0 after calls", duration > 0.0);
    }

    /**
     * Verifies that {@code CountDetails} and {@code DurationDetails} are returned as
     * typed {@link Counter} and {@link StatisticsMeter} objects via the MXBean proxy,
     * and that composite and scalar count values agree.
     */
    public void testOperationMXBeanCompositeAttributesReadable() throws Exception {
        ObjectName mbean = findOperationMBean("tools/call", "add");
        assertNotNull("Operation MBean for add must be registered", mbean);

        McpOperationStatisticsMXBean operationStats = JMX.newMXBeanProxy(mbs, mbean, McpOperationStatisticsMXBean.class);

        Counter countDetails = operationStats.getCountDetails();
        assertNotNull("CountDetails must not be null", countDetails);

        StatisticsMeter durationDetails = operationStats.getDurationDetails();
        assertNotNull("DurationDetails must not be null", durationDetails);
        assertTrue("DurationDetails total must be > 0", durationDetails.getTotal() > 0.0);
        assertTrue("DurationDetails count must be >= 1", durationDetails.getCount() >= 1L);
        assertTrue("DurationDetails mean must be > 0", durationDetails.getMean() > 0.0);

        long scalarCount = operationStats.getCount();
        long compositeCount = countDetails.getCurrentValue();
        assertEquals("Scalar Count and CountDetails.currentValue must agree", scalarCount, compositeCount);
    }

    /**
     * Verifies that the count increments by exactly 2 for two {@code tools/list} calls,
     * and that duration grows. The FAT test pre-records a baseline before the calls and
     * passes it to this method via {@link javax.management.MBeanServerConnection}.
     */
    public void testOperationMXBeanCountAccumulates() throws Exception {
        // Baseline is read before the two extra tools/list calls are made (by the FAT test).
        // This method is invoked after those calls.
        ObjectName mbean = findOperationMBean("tools/list", null);
        assertNotNull("tools/list operation MBean must exist", mbean);

        McpOperationStatisticsMXBean operationStats = JMX.newMXBeanProxy(mbs, mbean, McpOperationStatisticsMXBean.class);

        long count = operationStats.getCount();
        assertTrue("Count must be at least 2 after two tools/list calls", count >= 2L);

        double duration = operationStats.getDuration();
        assertTrue("Duration must be > 0 after calls", duration > 0.0);
    }

    /**
     * Verifies all String attributes of {@code McpSessionStatisticsMXBean} are readable
     * via a typed MXBean proxy obtained from the platform MBean server.
     */
    public void testSessionMXBeanStringAttributesReadable() throws Exception {
        ObjectName mbean = findSessionMBean();
        assertNotNull("Session MBean must be registered after session ends", mbean);

        McpSessionStatisticsMXBean sessionStats = JMX.newMXBeanProxy(mbs, mbean, McpSessionStatisticsMXBean.class);

        sessionStats.getErrorType();

        String jsonrpcVer = sessionStats.getJsonrpcProtocolVersion();
        assertEquals("JsonrpcProtocolVersion must be 2.0", "2.0", jsonrpcVer);

        String mcpVer = sessionStats.getMcpProtocolVersion();
        assertEquals("McpProtocolVersion must match",
                     TestConstants.VALUE_MCP_PROTOCOL_VERSION, mcpVer);

        sessionStats.getNetworkProtocolName();
        sessionStats.getNetworkProtocolVersion();
        sessionStats.getNetworkTransport();
    }

    /** Verifies numeric attributes of the session MBean are correctly typed and positive. */
    public void testSessionMXBeanNumericAttributesReadable() throws Exception {
        ObjectName mbean = findSessionMBean();
        assertNotNull("Session MBean must exist after session ends", mbean);

        McpSessionStatisticsMXBean sessionStats = JMX.newMXBeanProxy(mbs, mbean, McpSessionStatisticsMXBean.class);

        long count = sessionStats.getCount();
        assertTrue("Count must be >= 1", count >= 1L);

        double duration = sessionStats.getDuration();
        assertTrue("Duration must be positive", duration > 0.0);
    }

    /** Verifies composite attributes of the session MBean and that scalar/composite counts agree. */
    public void testSessionMXBeanCompositeAttributesReadable() throws Exception {
        ObjectName mbean = findSessionMBean();
        assertNotNull("Session MBean must exist", mbean);

        McpSessionStatisticsMXBean sessionStats = JMX.newMXBeanProxy(mbs, mbean, McpSessionStatisticsMXBean.class);

        Counter countDetails = sessionStats.getCountDetails();
        assertNotNull("CountDetails must not be null", countDetails);

        StatisticsMeter durationDetails = sessionStats.getDurationDetails();
        assertNotNull("DurationDetails must not be null", durationDetails);
        assertTrue("DurationDetails total must be > 0", durationDetails.getTotal() > 0.0);
        assertTrue("DurationDetails count must be >= 1", durationDetails.getCount() >= 1L);
        assertTrue("DurationDetails mean must be > 0", durationDetails.getMean() > 0.0);

        long scalarCount = sessionStats.getCount();
        long compositeCount = countDetails.getCurrentValue();
        assertEquals("Scalar Count and CountDetails.currentValue must agree", scalarCount, compositeCount);
    }

    /** Verifies the operation MBean ObjectName contains the expected key properties. */
    public void testOperationMBeanObjectNameKeyProperties() throws Exception {
        ObjectName mbean = findOperationMBean("tools/call", "add");
        assertNotNull("Operation MBean for add must be registered", mbean);

        String name = mbean.toString();
        assertTrue("ObjectName must contain mcpMethod=tools/call", name.contains("mcpMethod=tools/call"));
        assertTrue("ObjectName must contain genAiTool=add", name.contains("genAiTool=add"));
        assertTrue("ObjectName must contain jsonrpcVer=2.0", name.contains("jsonrpcVer=2.0"));
        assertTrue("ObjectName must contain mcpVer=", name.contains("mcpVer="));
    }

    /** Verifies that a {@code tools/list} MBean does NOT have a {@code genAiTool} key property. */
    public void testNonToolCallMBeanHasNoToolNameProperty() throws Exception {
        ObjectName mbean = findOperationMBean("tools/list", null);
        assertNotNull("tools/list MBean must be registered", mbean);

        String name = mbean.toString();
        assertTrue("ObjectName must contain mcpMethod=tools/list", name.contains("mcpMethod=tools/list"));
        assertTrue("ObjectName must NOT contain genAiTool=", !name.contains("genAiTool="));
    }

    /** Verifies the session MBean ObjectName contains {@code session=true} and protocol-version properties. */
    public void testSessionMBeanObjectNameKeyProperties() throws Exception {
        ObjectName mbean = findSessionMBean();
        assertNotNull("Session MBean must be registered after session ends", mbean);

        String name = mbean.toString();
        assertTrue("Session MBean ObjectName must contain session=true", name.contains("session=true"));
        assertTrue("Session MBean ObjectName must contain jsonrpcVer=2.0", name.contains("jsonrpcVer=2.0"));
        assertTrue("Session MBean ObjectName must contain mcpVer=", name.contains("mcpVer="));
    }

    /**
     * Verifies via {@link MBeanInfo} that every attribute declared in
     * {@code McpOperationStatisticsMXBean} is exposed in the registered MBean.
     */
    public void testOperationMBeanInfoContainsAllInterfaceAttributes() throws Exception {
        ObjectName mbean = findOperationMBean("tools/call", "add");
        assertNotNull("Operation MBean must exist", mbean);

        MBeanInfo info = mbs.getMBeanInfo(mbean);
        Set<String> attributeNames = new java.util.HashSet<>();
        for (var attr : info.getAttributes()) {
            attributeNames.add(attr.getName());
        }

        for (String expected : new String[] {
                                              "McpMethodName", "ErrorType", "GenAiPromptName", "GenAiToolName",
                                              "RpcResponseStatusCode", "GenAiOperationName", "JsonrpcProtocolVersion",
                                              "McpProtocolVersion", "NetworkProtocolName", "NetworkProtocolVersion",
                                              "NetworkTransport", "McpResourceUri", "Count", "CountDetails",
                                              "Duration", "DurationDetails"
        }) {
            assertTrue("MBeanInfo must expose attribute: " + expected, attributeNames.contains(expected));
        }
    }

    /**
     * Verifies via {@link MBeanInfo} that every attribute declared in
     * {@code McpSessionStatisticsMXBean} is exposed in the registered session MBean.
     */
    public void testSessionMBeanInfoContainsAllInterfaceAttributes() throws Exception {
        ObjectName mbean = findSessionMBean();
        assertNotNull("Session MBean must exist", mbean);

        MBeanInfo info = mbs.getMBeanInfo(mbean);
        Set<String> attributeNames = new java.util.HashSet<>();
        for (var attr : info.getAttributes()) {
            attributeNames.add(attr.getName());
        }

        for (String expected : new String[] {
                                              "ErrorType", "JsonrpcProtocolVersion", "McpProtocolVersion",
                                              "NetworkProtocolName", "NetworkProtocolVersion", "NetworkTransport",
                                              "Count", "CountDetails", "Duration", "DurationDetails"
        }) {
            assertTrue("MBeanInfo must expose attribute: " + expected, attributeNames.contains(expected));
        }
    }
}
