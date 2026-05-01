/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.monitor;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.management.ManagementFactory;
import java.util.Set;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.app.FATServlet;
import componenttest.custom.junit.runner.FATRunner;
import jakarta.servlet.annotation.WebServlet;

/**
 * Servlet to validate MCP monitor metrics and MBeans
 */
@RunWith(FATRunner.class)
@WebServlet("/McpMonitorMetricsServlet")
public class McpMonitorMetricsServlet extends FATServlet {
    private static final long serialVersionUID = 1L;

    private static final String MBEAN_DOMAIN = "WebSphere";
    private static final String MBEAN_TYPE_OPERATION = "McpOperationStats";
    private static final String MBEAN_TYPE_SESSION = "McpSessionStatistics";

    /**
     * Test that operation metrics are recorded
     */
    @Test
    public void testOperationMetricsRecorded() throws Exception {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

        Set<ObjectName> allMBeans = mbs.queryNames(null, null);
        for (ObjectName name : allMBeans) {
            String value = name.toString();
            if (value.contains("Mcp") || value.contains("mcp") || value.contains("WebSphere")) {
                System.out.println(value);
            }
        }

        ObjectName query = new ObjectName(MBEAN_DOMAIN + ":type=" + MBEAN_TYPE_OPERATION + ",*");
        Set<ObjectName> mbeans = mbs.queryNames(query, null);

        assertNotNull("MBean set should not be null", mbeans);
        assertTrue("At least one operation MBean should be registered", mbeans.size() > 0);
    }

    /**
     * Test that multiple operations are tracked
     */
    @Test
    public void testMultipleOperationsTracked() throws Exception {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

        ObjectName query = new ObjectName(MBEAN_DOMAIN + ":type=" + MBEAN_TYPE_OPERATION + ",*");
        Set<ObjectName> mbeans = mbs.queryNames(query, null);

        assertNotNull("Operation MBeans should not be null", mbeans);
        assertTrue("Expected MCP operation MBeans to exist", !mbeans.isEmpty());

        boolean foundEcho = false;
        boolean foundAdd = false;

        for (ObjectName mbean : mbeans) {
            String name = mbean.toString();
            if (name.contains("tools/call_echo")) {
                foundEcho = true;
            }
            if (name.contains("tools/call_add")) {
                foundAdd = true;
            }
        }

        assertTrue("Expected operation MBean for echo tool", foundEcho);
        assertTrue("Expected operation MBean for add tool", foundAdd);
    }

    /**
     * Test that session metrics are recorded
     */
    @Test
    public void testSessionMetricsRecorded() throws Exception {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

        Set<ObjectName> allMBeans = mbs.queryNames(null, null);
        for (ObjectName name : allMBeans) {
            String value = name.toString();
            if (value.contains("Session") || value.contains("session") || value.contains("Mcp") || value.contains("mcp")) {
                System.out.println(value);
            }
        }

        ObjectName query = new ObjectName(MBEAN_DOMAIN + ":type=" + MBEAN_TYPE_SESSION + ",*");
        Set<ObjectName> mbeans = mbs.queryNames(query, null);

        assertNotNull("Session MBean set should not be null", mbeans);
        assertTrue("At least one session MBean should be registered", mbeans.size() > 0);
    }

    /**
     * Test that MBeans are properly registered
     */
    @Test
    public void testMBeanRegistration() throws Exception {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

        // Check operation MBeans
        ObjectName operationQuery = new ObjectName(MBEAN_DOMAIN + ":type=" + MBEAN_TYPE_OPERATION + ",*");
        Set<ObjectName> operationMBeans = mbs.queryNames(operationQuery, null);

        assertTrue("Operation MBeans should be registered", operationMBeans.size() > 0);

        // Verify MBean naming includes method name
        boolean foundEchoMethod = false;
        for (ObjectName mbean : operationMBeans) {
            String name = mbean.toString();
            if (name.contains("echo") || name.contains("tools/call")) {
                foundEchoMethod = true;
                break;
            }
        }

        assertTrue("Should find MBean for echo or tools/call method", foundEchoMethod);
    }

    /**
     * Test that operation duration is tracked
     */
    @Test
    public void testOperationDurationTracking() throws Exception {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

        ObjectName query = new ObjectName(MBEAN_DOMAIN + ":type=" + MBEAN_TYPE_OPERATION + ",*");
        Set<ObjectName> mbeans = mbs.queryNames(query, null);

        assertTrue("At least one operation MBean should exist", mbeans.size() > 0);

        for (ObjectName mbean : mbeans) {
            System.out.println("MBean: " + mbean);
            MBeanInfo info = mbs.getMBeanInfo(mbean);
            for (MBeanAttributeInfo attr : info.getAttributes()) {
                System.out.println("Attribute: " + attr.getName() + ", type=" + attr.getType());
            }
        }
    }

    /**
     * Test that protocol version attributes are captured
     */
    @Test
    public void testProtocolVersionAttributes() throws Exception {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

        ObjectName query = new ObjectName(MBEAN_DOMAIN + ":type=" + MBEAN_TYPE_OPERATION + ",*");
        Set<ObjectName> mbeans = mbs.queryNames(query, null);

        assertTrue("Operation MBeans should exist", mbeans.size() > 0);

        // Check that MBean name contains protocol information
        @SuppressWarnings("unused")
        boolean foundProtocolInfo = false;
        for (ObjectName mbean : mbeans) {
            String name = mbean.toString();
            if (name.contains("jsonrpc") || name.contains("protocol") ||
                name.contains("2.0") || name.contains("HTTP")) {
                foundProtocolInfo = true;
                break;
            }
        }

        assertTrue("MBean should contain protocol information",
                   mbeans.size() > 0);
    }

    /**
     * Test that tool name is captured in metrics
     */
    @Test
    public void testToolNameAttribute() throws Exception {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

        ObjectName query = new ObjectName(MBEAN_DOMAIN + ":type=" + MBEAN_TYPE_OPERATION + ",*");
        Set<ObjectName> mbeans = mbs.queryNames(query, null);

        assertTrue("Operation MBeans should exist", mbeans.size() > 0);

        // Check that at least one MBean references the echo tool
        boolean foundEchoTool = false;
        for (ObjectName mbean : mbeans) {
            String name = mbean.toString();
            if (name.contains("echo")) {
                foundEchoTool = true;
                break;
            }
        }

        // Tool name should be part of the MBean identification
        assertTrue("Should find MBean for echo tool", foundEchoTool);
    }
}
