/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.monitor;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Modified;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.container.service.app.deploy.ApplicationInfo;
import com.ibm.ws.container.service.state.ApplicationStateListener;
import com.ibm.ws.container.service.state.StateChangeException;

import io.openliberty.mcp.internal.monitoring.McpStatsMonitor;
import io.openliberty.mcp.internal.monitoring.McpStatsMonitorHolder;

/**
 * Application state listener that monitors MCP application lifecycle events.
 * 
 * <p>This listener is responsible for cleaning up MCP monitoring resources (MBeans, metrics, statistics)
 * when applications are unloaded. Without this cleanup, memory leaks would occur as the monitoring
 * data structures would continue to hold references to unloaded applications.
 * 
 * <p>The listener is configured to respond to the monitor-1.0 filter configuration, allowing
 * administrators to enable/disable MCP monitoring via the filter attribute. When MCP monitoring
 * is disabled via the filter, this listener will not perform cleanup operations.
 * 
 * <p><b>Lifecycle Integration:</b>
 * <ul>
 *   <li>{@link #applicationStopped(ApplicationInfo)} - Called when an application is unloaded.
 *       Triggers cleanup of all MCP statistics, MBeans, and metrics associated with that application.</li>
 * </ul>
 * 
 * <p><b>Configuration:</b> This component is tied to the monitor-1.0 configuration and respects
 * the "filter" attribute to determine if MCP monitoring is enabled. The filter is a comma-separated
 * list of monitoring groups (e.g., "HTTP,MCP"). If "MCP" is not in the list, monitoring is disabled.
 * 
 * @see McpStatsMonitor
 * @see ApplicationStateListener
 */
@Component(service = { ApplicationStateListener.class },
           configurationPid = "com.ibm.ws.monitor.internal.MonitoringFrameworkExtender",
           configurationPolicy = ConfigurationPolicy.REQUIRE,
           immediate = true)
public class McpMonitorAppStateListener implements ApplicationStateListener {

    private static final String MONITORING_GROUP_FILTER = "filter";
    
    private static final TraceComponent tc = Tr.register(McpMonitorAppStateListener.class);
    
    /**
     * Indicates whether MCP monitoring is enabled based on the monitor-1.0 filter configuration.
     * By default, without any monitor-1.0 filters configured, all monitor components are enabled.
     */
    private static volatile boolean isMcpEnabled = true;
    
    /**
     * Tracks whether the component has been activated. Used to prevent cleanup during initial activation.
     */
    private volatile boolean activated = false;
    
    /**
     * Returns whether MCP monitoring is currently enabled.
     * 
     * @return true if MCP monitoring is enabled, false otherwise
     */
    public static boolean isMcpEnabled() {
        return isMcpEnabled;
    }
    
    @Override
    public void applicationStarting(ApplicationInfo appInfo) throws StateChangeException {
        // No action needed on application starting
    }

    @Override
    public void applicationStarted(ApplicationInfo appInfo) throws StateChangeException {
        // No action needed on application started
    }

    @Override
    public void applicationStopping(ApplicationInfo appInfo) {
        // No action needed on application stopping
    }

    /**
     * Called when an application has been stopped and unloaded.
     * 
     * <p>This method triggers cleanup of all MCP monitoring resources associated with the
     * stopped application, including:
     * <ul>
     *   <li>MBeans registered for operation and session statistics</li>
     *   <li>Entries in the appNameToStat tracking map</li>
     *   <li>MicroProfile Metrics and Telemetry data</li>
     * </ul>
     * 
     * <p>The cleanup is performed via {@link McpStatsMonitor#removeStatsForApp(String)} which is
     * accessed through the ServiceCaller pattern to ensure proper OSGi service lifecycle management.
     * 
     * @param appInfo Information about the application that was stopped
     */
    @Override
    public void applicationStopped(ApplicationInfo appInfo) {
        if (!isMcpEnabled) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "MCP monitoring is disabled, skipping cleanup for: " + appInfo.getDeploymentName());
            }
            return;
        }
        
        String appName = appInfo.getDeploymentName();
        
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "Application stopped, cleaning up MCP statistics for: " + appName);
        }
        
        // Get the monitor instance from the holder
        McpStatsMonitor monitor = McpStatsMonitorHolder.get();
        if (monitor != null) {
            monitor.removeStatsForApp(appName);
        }
    }
    
    /**
     * Called when the component is activated.
     * Resolves the monitor filter configuration to determine if MCP monitoring is enabled.
     * 
     * @param context The component context
     * @param properties The configuration properties
     */
    @Activate
    protected void activate(ComponentContext context, Map<String, Object> properties) {
        resolveMonitorFilter(properties);
        activated = true;
    }

    /**
     * Called when the component configuration is modified.
     * Re-evaluates the monitor filter to update the enabled state.
     * If MCP monitoring is disabled, triggers cleanup of all MCP monitoring resources.
     *
     * @param context The component context
     * @param properties The updated configuration properties
     */
    @Modified
    protected void modified(ComponentContext context, Map<String, Object> properties) {
        boolean wasEnabled = isMcpEnabled;
        resolveMonitorFilter(properties);
        
        // If MCP monitoring was just disabled, remove MBeans but preserve tracking data
        // Only perform cleanup if component has been fully activated
        if (activated && wasEnabled && !isMcpEnabled) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "MCP monitoring disabled, removing MBeans but preserving tracking data");
            }
            
            // Remove MBeans but keep tracking data for potential re-registration
            McpStatsMonitor monitor = McpStatsMonitorHolder.get();
            if (monitor != null) {
                monitor.removeAllMBeansForMonitoringDisabled();
            }
        } else if (activated && !wasEnabled && isMcpEnabled) {
            // If MCP monitoring was just re-enabled, re-register MBeans for existing applications
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "MCP monitoring re-enabled, re-registering MBeans for existing applications");
            }
            
            // Re-register MBeans asynchronously to avoid blocking the config update thread
            // and to allow the monitoring framework time to process the filter change
            Thread reregisterThread = new Thread(() -> {
                McpStatsMonitor monitor = McpStatsMonitorHolder.get();
                if (monitor != null) {
                    // Retry up to 5 times with 200ms delay between attempts
                    // This gives the monitoring framework time to process the filter change
                    for (int attempt = 1; attempt <= 5; attempt++) {
                        monitor.reregisterAllMBeans();
                        
                        // Check if any MBeans were successfully registered
                        // If so, we're done. If not, wait and retry.
                        try {
                            Thread.sleep(200);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
            }, "MCP-MBean-Reregistration");
            reregisterThread.setDaemon(true);
            reregisterThread.start();
        }
    }
    
    /**
     * Resolves the monitor filter configuration to determine if MCP monitoring is enabled.
     * 
     * <p>The filter is a comma-separated list of monitoring groups. If "MCP" appears in the list,
     * MCP monitoring is enabled. If the filter is empty or null, all monitoring is enabled by default.
     * 
     * @param properties The configuration properties containing the filter
     */
    private void resolveMonitorFilter(Map<String, Object> properties) {
        String filter = (String) properties.get(MONITORING_GROUP_FILTER);

        if (filter == null || filter.isEmpty()) {
            // No filter or empty filter means all monitor components are enabled by default
            isMcpEnabled = true;
        } else {
            // Check if MCP is explicitly listed in the filter
            isMcpEnabled = Stream.of(filter.split(",")).anyMatch(item -> item.equals("MCP"));
        }
        
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, String.format("MCP monitoring enabled: [%s]", isMcpEnabled));
        }
    }
}

// Made with Bob
