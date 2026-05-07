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

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.openliberty.mcp.internal.monitoring.internal.McpOperationStatAttributes;
import io.openliberty.mcp.internal.monitoring.internal.McpSessionStatAttributes;

@Component(configurationPolicy = ConfigurationPolicy.IGNORE, immediate = true, service = MetricsManager.class)
public class MetricsManager {

	private static final TraceComponent tc = Tr.register(MetricsManager.class);

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
    private volatile List<McpMetricAdapter> mcpMetricRuntimes;



    /**
     * Updates MCP operation duration metrics across all registered metric adapters.
     *
     * @param mcpStatsAttribute the MCP operation attributes
     * @param duration the operation duration
     */
	public void updateMcpOperationDurationMetrics(McpOperationStatAttributes mcpStatsAttribute , Duration duration) {
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
	        Tr.debug(tc, "Forwarding metrics to " + mcpMetricRuntimes.size() + " adapters");
	    }

        for (McpMetricAdapter adapter : mcpMetricRuntimes) {
        	if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
	            Tr.debug(tc, "Adapter: " + adapter.getClass().getName());
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
	public void updateMcpSessionDurationMetrics(McpSessionStatAttributes mcpStatsAttribute , Duration duration) {
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
	        Tr.debug(tc, "Forwarding metrics to " + mcpMetricRuntimes.size() + " adapters");
	    }

        for (McpMetricAdapter adapter : mcpMetricRuntimes) {
        	if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
	            Tr.debug(tc, "Adapter: " + adapter.getClass().getName());
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
  if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
         Tr.debug(tc, "Removing metrics for application: " + appName + " across " + mcpMetricRuntimes.size() + " adapters");
     }

        for (McpMetricAdapter adapter : mcpMetricRuntimes) {
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
             Tr.debug(tc, "Cleaning up metrics in adapter: " + adapter.getClass().getName());
         }
            adapter.removeMetricsForApp(appName);
        }
 }

}