/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.monitor.metrics;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

@Component(configurationPolicy = ConfigurationPolicy.IGNORE, service = MetricsManager.class)
public class MetricsManager {

    private static final TraceComponent tc = Tr.register(MetricsManager.class);
    
    // Use CopyOnWriteArrayList for thread-safe dynamic service tracking
    private final List<McpMetricAdapter> mcpMetricAdapters = new CopyOnWriteArrayList<>();
    
    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void addMcpMetricAdapter(McpMetricAdapter adapter) {
        mcpMetricAdapters.add(adapter);
    }
    
    protected void removeMcpMetricAdapter(McpMetricAdapter adapter) {
        mcpMetricAdapters.remove(adapter);
    }

    /**
     * Updates MCP operation duration metrics across all registered metric adapters.
     *
     * @param mcpStatsAttribute the MCP operation attributes
     * @param duration the operation duration
     */
    public void updateMcpOperationDurationMetrics(McpOperationStatAttributes mcpStatsAttribute, Duration duration) {
        for (McpMetricAdapter adapter : mcpMetricAdapters) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Forwarding operation metrics to adapter: " + adapter.getClass().getName());
            }
            adapter.updateMcpOperationMetrics(mcpStatsAttribute, duration);
        }
    }

    /**
     * Updates MCP session duration metrics across all registered metric adapters.
     *
     * @param mcpStatsAttribute the MCP session attributes
     * @param duration the session duration
     */
    public void updateMcpSessionDurationMetrics(McpSessionStatAttributes mcpStatsAttribute, Duration duration) {
        for (McpMetricAdapter adapter : mcpMetricAdapters) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Forwarding session metrics to adapter: " + adapter.getClass().getName());
            }
            adapter.updateMcpSessionMetrics(mcpStatsAttribute, duration);
        }
    }

    /**
     * Removes all metrics associated with the specified application.
     * This method is called when an application is unloaded to clean up metrics and prevent memory leaks.
     *
     * @param appName The name of the application being unloaded
     */
    public void removeMetricsForApp(String appName) {
        for (McpMetricAdapter adapter : mcpMetricAdapters) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Cleaning up metrics for app " + appName + " in adapter: " + adapter.getClass().getName());
            }
            adapter.removeMetricsForApp(appName);
        }
    }

}