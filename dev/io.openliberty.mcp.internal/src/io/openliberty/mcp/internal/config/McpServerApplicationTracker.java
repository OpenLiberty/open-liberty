/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.config;

import static io.openliberty.mcp.internal.config.McpServerConfigProps.DEFAULT_CONFIG;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.application.Application;

/**
 * Tracks applications and their mcpServer configurations defined in the server.xml
 * The component compares Application service with `McpConfigurationComponent` services to determine
 * which mcp configuration belongs to which application
 * See {@link io.openliberty.microprofile.config.internal.serverxml.AppPropertiesTrackingComponent}
 *
 */
@Component(service = McpServerApplicationTracker.class,
           configurationPolicy = ConfigurationPolicy.IGNORE,
           immediate = true)
public class McpServerApplicationTracker {
    private static final TraceComponent tc = Tr.register(McpServerApplicationTracker.class);
    private boolean isTracingAndDebugEnabled = TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled();

    /**
     * Map from mcpServer PID to its corresponding McpConfigurationComponent. When an application arrives referencing mcpServer PIDs,
     * we are able to look up the actual configComponent object with this map.
     * Must hold lock on {@code this} to access.
     */
    private final Map<String, McpConfigurationComponent> mcpServerPidToMcpConfigComponent = new HashMap<>();

    /**
     * A record of the application name and the list of mcpServerPids that application references. Keeps these 2 pieces of
     * per-app data co-located and impossible to get out of sync
     */
    private record AppEntry(String appName, List<String> mcpServerPids) {};

    /**
     * Map from an application PID to its AppEntry record
     */
    private final Map<String, AppEntry> appPidToAppEntry = new HashMap<>();

    /**
     * Map from application name to list of MCP configsProps.
     * Must hold lock on {@code this} to <b>write</b>, but not to read
     */
    private final Map<String, List<McpServerConfigProps>> appNameToMcpConfigProps = new ConcurrentHashMap<>();

    @Activate
    protected void activate() {
        if (isTracingAndDebugEnabled) {
            Tr.debug(this, tc, "McpServerApplicationTracker activated");
        }
    }

    @Deactivate
    protected void deactivate() {
        synchronized (this) {
            mcpServerPidToMcpConfigComponent.clear();
            appPidToAppEntry.clear();
            appNameToMcpConfigProps.clear();
        }
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE,
               policy = ReferencePolicy.DYNAMIC,
               policyOption = ReferencePolicyOption.GREEDY)
    protected void addMcpConfiguration(McpConfigurationComponent config) {
        synchronized (this) {
            mcpServerPidToMcpConfigComponent.put(config.getServicePid(), config);
            updateAppsUsingMcpConfig(config.getServicePid());
        }
    }

    protected void updatedMcpConfiguration(McpConfigurationComponent config) {
        synchronized (this) {
            updateAppsUsingMcpConfig(config.getServicePid());
        }
    }

    protected void removeMcpConfiguration(McpConfigurationComponent config) {
        String mcpServerPid = config.getServicePid();
        synchronized (this) {
            mcpServerPidToMcpConfigComponent.remove(mcpServerPid);
            updateAppsUsingMcpConfig(mcpServerPid);
        }
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE,
               policy = ReferencePolicy.DYNAMIC,
               policyOption = ReferencePolicyOption.GREEDY,
               service = Application.class)
    protected void addApp(Map<String, Object> appProps) {
        String appPid = (String) appProps.get("service.pid");
        String appName = (String) appProps.get("name");
        String[] mcpServerPids = (String[]) appProps.get("mcpServer");

        if (isTracingAndDebugEnabled) {
            Tr.debug(this, tc, "Application added. Name=" + appName + ", PID=" + appPid +
                               ", mcpServerPIDs=" + Arrays.toString(mcpServerPids));
        }

        if (appPid == null && appName == null) {
            return; //We don't ever expect these to be null
        }
        synchronized (this) {
            List<String> mcpServerPidList = (mcpServerPids != null && mcpServerPids.length > 0) ? Arrays.asList(mcpServerPids) : Collections.emptyList();
            appPidToAppEntry.put(appPid, new AppEntry(appName, mcpServerPidList));
            if (!mcpServerPidList.isEmpty()) {
                updateAppMcpConfigProps(appPid);
            }
        }
    }

    protected void updatedApp(Map<String, Object> appProps) {
        String appPid = (String) appProps.get("service.pid");
        String appName = (String) appProps.get("name");
        String[] mcpServerPids = (String[]) appProps.get("mcpServer");

        if (isTracingAndDebugEnabled) {
            Tr.debug(this, tc, "Application updated. Name=" + appName + ", PID=" + appPid +
                               ", mcpServerPIDs=" + Arrays.toString(mcpServerPids));
        }

        if (appPid == null && appName == null) {
            return; //We don't ever expect these to be null
        }
        synchronized (this) {
            AppEntry oldEntry = appPidToAppEntry.get(appPid);
            if (oldEntry != null && !oldEntry.appName().equals(appName)) {
                appNameToMcpConfigProps.remove(oldEntry.appName());
            }
            List<String> mcpServerPidList = (mcpServerPids != null && mcpServerPids.length > 0) ? Arrays.asList(mcpServerPids) : Collections.emptyList();
            appPidToAppEntry.put(appPid, new AppEntry(appName, mcpServerPidList));
            updateAppMcpConfigProps(appPid);
        }
    }

    protected void removeApp(Map<String, Object> appProps) {
        String appPid = (String) appProps.get("service.pid");

        if (isTracingAndDebugEnabled) {
            Tr.debug(this, tc, "Application removed. PID=" + appPid);
        }

        if (appPid == null) {
            return; //We don't ever expect these to be null
        }
        synchronized (this) {
            AppEntry appEntry = appPidToAppEntry.remove(appPid);
            if (appEntry != null) {
                appNameToMcpConfigProps.remove(appEntry.appName());
            }
        }
    }

    /**
     * Given an mcpServer PID that was just added or updated, scan all `appEntries` to find applications
     * that reference it and trigger a config rebuild for each, via `updateAppMcpConfigProps()`
     *
     */
    private void updateAppsUsingMcpConfig(String mcpServerPid) {
        for (Map.Entry<String, AppEntry> appEntry : appPidToAppEntry.entrySet()) {
            if (appEntry.getValue().mcpServerPids.contains(mcpServerPid)) {
                updateAppMcpConfigProps(appEntry.getKey());
            }
        }
    }

    /**
     * Given an application's PID, looks up its AppEntry, and build a complete list of resolved
     * {@link McpServerConfigProps} by streaming the {@link mcpServerPids}, map each to its {@link McpConfigurationComponent},
     * filter out nulls, then map to {@link McpServerConfigProps}, filter out nulls and collect the resulting
     * list.
     * If the resulting list is empty the appEntry is removed from {@link appNameToMcpConfigProps}, otherwise the new
     * list replaces the previous entry
     *
     * Must hold lock on {@code this} to access.
     */
    private void updateAppMcpConfigProps(String appPid) {

        AppEntry appEntry = appPidToAppEntry.get(appPid);
        if (appEntry == null) {
            return;
        }
        String appName = appEntry.appName();

        List<McpServerConfigProps> newPropsList = appEntry.mcpServerPids().stream()
                                                          .map(mcpServerPid -> mcpServerPidToMcpConfigComponent.get(mcpServerPid))
                                                          .filter(Objects::nonNull)
                                                          .map(mcpConfigComponent -> mcpConfigComponent.getConfigProps())
                                                          .filter(Objects::nonNull)
                                                          .distinct()
                                                          .toList();

        if (newPropsList.isEmpty()) {
            appNameToMcpConfigProps.remove(appName);
        } else {
            appNameToMcpConfigProps.put(appName, newPropsList);
        }
    }

    /**
     * Iterates through `appNameToMcpConfigProps` map to get the registered `McpServerConfigProps` for a particular
     * appName, and a module name if specified, or a wildcard if one is available and a specific one can not
     * be found
     *
     * @param appName The name of the application defined in the server.xml under `<application name="appName" location="app.war">`
     * @param moduleName The name of the module defined in the server.xml under `<mcpServer moduleName="myModule" path="/mcp"`
     * @return the McpServerConfigProps for the given application. If no `<mcpServer>` property was set in the server.xml
     * or if a configuration for particular module could not be found, returns {@link McpServerConfigProps.DEFAULT_CONFIG}
     */
    public McpServerConfigProps getConfigForModule(String appName, String moduleName) {
        List<McpServerConfigProps> configProps = appNameToMcpConfigProps.get(appName);
        if (configProps == null || configProps.isEmpty()) {
            return DEFAULT_CONFIG;
        }
        McpServerConfigProps result = null;
        for (McpServerConfigProps configProp : configProps) {
            //Exact module name match
            if (moduleName != null && moduleName.equals(configProp.moduleName())) {
                result = configProp;
                break;
            }
            //no module name specified - use default for all modules
            if (configProp.moduleName() == null) {
                result = configProp;
            }
        }
        return result != null ? result : DEFAULT_CONFIG;
    }

}
