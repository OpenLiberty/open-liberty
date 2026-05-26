/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.observability;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.management.ManagementFactory;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.junit.Test;

import componenttest.app.FATServlet;
import jakarta.servlet.annotation.WebServlet;

/**
 * Servlet that checks MBean registration status.
 * Uses FATServlet pattern so tests can be invoked via FATServletClient.runTest()
 */
@WebServlet("/MBeanCheckerServlet")
public class MBeanCheckerServlet extends FATServlet {
    private static final long serialVersionUID = 1L;

    /**
     * Test that verifies MCP operation MBeans are registered.
     * Called after tool invocation to confirm MBeans exist.
     */
    @Test
    public void testOperationMBeansRegistered() throws Exception {
        boolean registered = checkMBeanRegistered("WebSphere:type=McpOperationStatistics,*");
        assertTrue("MCP operation MBeans should be registered after tool invocation", registered);
    }

    /**
     * Test that verifies MCP operation MBeans are NOT registered.
     * Called after application unload to confirm cleanup occurred.
     */
    @Test
    public void testOperationMBeansNotRegistered() throws Exception {
        boolean registered = checkMBeanRegistered("WebSphere:type=McpOperationStatistics,*");
        assertFalse("MCP operation MBeans should be removed after app unload", registered);
    }

    /**
     * Test that verifies MCP session MBeans are NOT registered.
     * Called after application unload to confirm cleanup occurred.
     */
    @Test
    public void testSessionMBeansNotRegistered() throws Exception {
        boolean registered = checkMBeanRegistered("WebSphere:type=McpSessionStatistics,*");
        assertFalse("MCP session MBeans should be removed after app unload", registered);
    }

    /**
     * Helper method to check if MBeans matching the pattern are registered.
     * Uses doPrivileged to handle Java 2 Security restrictions.
     */
    private boolean checkMBeanRegistered(String objectNamePattern) throws Exception {
        return AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
            @Override
            public Boolean run() {
                try {
                    MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
                    ObjectName pattern = new ObjectName(objectNamePattern);
                    
                    // Wait up to 5 seconds for MBeans to appear/disappear
                    for (int i = 0; i < 5; i++) {
                        Set<ObjectName> mbeans = mbs.queryNames(pattern, null);
                        if (!mbeans.isEmpty()) {
                            return true;
                        }
                        Thread.sleep(1000);
                    }
                    return false;
                } catch (Exception e) {
                    throw new RuntimeException("Error checking MBean registration", e);
                }
            }
        });
    }
}

// Made with Bob
