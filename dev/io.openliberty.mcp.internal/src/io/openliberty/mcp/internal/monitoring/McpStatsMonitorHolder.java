/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.monitoring;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * Holder for the McpStatsMonitor instance created by the monitoring framework.
 * This allows the io.openliberty.mcp.internal bundle to access the monitor
 * without creating a cyclic dependency on io.openliberty.mcp.internal.monitor.
 */
public final class McpStatsMonitorHolder {

    private static final TraceComponent tc = Tr.register(McpStatsMonitorHolder.class);
    private static volatile McpStatsMonitor monitor;

    private McpStatsMonitorHolder() {}

    public static void set(McpStatsMonitor mcpStatsMonitor) {
        monitor = mcpStatsMonitor;
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "McpStatsMonitor set in holder: " + (mcpStatsMonitor != null ? mcpStatsMonitor.getClass().getName() : "null"));
        }
    }

    public static McpStatsMonitor get() {
        if (monitor == null && TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "McpStatsMonitor requested but holder is null - monitoring framework may not have instantiated the monitor yet");
        }
        return monitor;
    }

    public static void clear() {
        monitor = null;
        // Also clear the singleton instance in McpStatsMonitorImpl for testing
        try {
            Class<?> implClass = Class.forName("io.openliberty.mcp.internal.monitor.McpStatsMonitorImpl");
            java.lang.reflect.Method clearInstance = implClass.getDeclaredMethod("clearInstance");
            clearInstance.setAccessible(true);
            clearInstance.invoke(null);
        } catch (Exception e) {
            // Ignore - monitor bundle may not be loaded
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "McpStatsMonitor cleared from holder");
        }
    }
}
