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

import java.util.Map;
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
 * Application state listener responsible for cleaning up MCP monitoring resources
 * (MBeans, metrics, statistics) when applications are unloaded.
 *
 * <p>Without this cleanup, memory leaks would occur as internal data structures
 * continue to hold references to unloaded applications.
 *
 * <p><b>Lifecycle integration:</b>
 * {@link #applicationStopped(ApplicationInfo)} is called by the Liberty application
 * lifecycle framework when an application is unloaded. It delegates to
 * {@link McpStatsMonitor#removeStatsForApp(String)} via {@link McpStatsMonitorHolder}
 * to remove all MBeans and metrics associated with that application. Cleanup always
 * occurs regardless of the {@code monitor-1.0} filter configuration.
 *
 * <p><b>Configuration policy ({@code OPTIONAL}):</b>
 * This component binds to the {@code com.ibm.ws.monitor.internal.MonitoringFrameworkExtender}
 * configuration PID; the same PID as the {@code <monitor>} element in {@code server.xml}.
 * {@code ConfigurationPolicy.OPTIONAL} ensures the component activates whether or not a
 * {@code <monitor>} element is present. When one is present its {@code filter} attribute
 * is read by {@link #activate} and {@link #modified} to set {@link #isMcpEnabled}, which
 * gates stat collection in {@link McpStatsMonitorImpl}.
 * {@code ConfigurationPolicy.REQUIRE} must not be used here: it would prevent activation
 * when no {@code <monitor>} element exists, leaving MBeans registered after app unload.
 *
 * @see McpStatsMonitor
 * @see ApplicationStateListener
 */
@Component(service = { ApplicationStateListener.class },
           configurationPid = "com.ibm.ws.monitor.internal.MonitoringFrameworkExtender",
           configurationPolicy = ConfigurationPolicy.OPTIONAL,
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
     * <li>MBeans registered for operation and session statistics</li>
     * <li>Entries in the appNameToStat tracking map</li>
     * <li>MicroProfile Metrics and Telemetry data</li>
     * </ul>
     *
     * <p>The cleanup is performed via {@link McpStatsMonitor#removeStatsForApp(String)},
     * accessed through {@link McpStatsMonitorHolder}.
     *
     * @param appInfo Information about the application that was stopped
     */
    @Override
    public void applicationStopped(ApplicationInfo appInfo) {
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
    }

    /**
     * Called when the component configuration is modified.
     * Re-evaluates the monitor filter to update {@link #isMcpEnabled}.
     *
     * @param context The component context
     * @param properties The updated configuration properties
     */
    @Modified
    protected void modified(ComponentContext context, Map<String, Object> properties) {
        resolveMonitorFilter(properties);
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
