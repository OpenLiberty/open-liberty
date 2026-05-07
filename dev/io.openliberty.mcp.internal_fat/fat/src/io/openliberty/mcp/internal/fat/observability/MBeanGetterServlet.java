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

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Servlet to check MBean registration from within the Liberty server JVM.
 * This is necessary because MBeans registered via @PublishedMetric only exist
 * in the server's JVM, not in the FAT test client's JVM.
 */
@WebServlet("/MBeanGetterServlet")
public class MBeanGetterServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    
    private static final MBeanServer mbeanServer = AccessController.doPrivileged(
        (PrivilegedAction<MBeanServer>) () -> ManagementFactory.getPlatformMBeanServer()
    );

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        String objectNamePattern = request.getParameter("objectname");
        if (objectNamePattern == null || objectNamePattern.isEmpty()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing 'objectname' parameter");
            return;
        }

        response.setContentType("text/plain");
        PrintWriter out = response.getWriter();

        try {
            // Retry logic: check for MBean registration with retries
            boolean isRegistered = false;
            int maxRetries = 30; // 30 seconds total
            int retryCount = 0;
            
            while (!isRegistered && retryCount < maxRetries) {
                Set<ObjectName> mbeans = mbeanServer.queryNames(new ObjectName(objectNamePattern), null);
                isRegistered = !mbeans.isEmpty();
                
                if (!isRegistered) {
                    TimeUnit.SECONDS.sleep(1);
                    retryCount++;
                }
            }
            
            if (isRegistered) {
                Set<ObjectName> mbeans = mbeanServer.queryNames(new ObjectName(objectNamePattern), null);
                out.println("true");
                out.println("Found " + mbeans.size() + " MBean(s):");
                for (ObjectName name : mbeans) {
                    out.println("  " + name);
                }
                out.println("Waited " + retryCount + " second(s) for MBeans to appear");
            } else {
                out.println("false");
                out.println("No MBeans found matching pattern: " + objectNamePattern);
                out.println("Waited " + retryCount + " second(s)");
            }
            
        } catch (Exception e) {
            out.println("false");
            out.println("Error checking MBean: " + e.getMessage());
            e.printStackTrace(out);
        }
    }
}

// Made with Bob
