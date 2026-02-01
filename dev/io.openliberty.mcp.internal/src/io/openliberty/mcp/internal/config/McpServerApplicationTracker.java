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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

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
     * Map from mcpServer PID to its corresponding McpConfigurationComponent
     * Must hold lock on {@code this} to access.
     */
    private final Map<String, McpConfigurationComponent> mcpServerPidToMcpConfig = new HashMap<>();

    /**
     * A record of the application name and the list of mcpServerPids that application references
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
            mcpServerPidToMcpConfig.clear();
            appPidToAppEntry.clear();
            appNameToMcpConfigProps.clear();
        }
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE,
               policy = ReferencePolicy.DYNAMIC,
               policyOption = ReferencePolicyOption.GREEDY)
    protected void addMcpConfiguration(McpConfigurationComponent config) {
        synchronized (this) {
            mcpServerPidToMcpConfig.put(config.getServicePid(), config);
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
            mcpServerPidToMcpConfig.remove(mcpServerPid);
            for (AppEntry appEntry : appPidToAppEntry.values()) {
                if (appEntry.mcpServerPids.contains(mcpServerPid)) {
                    removeMcpConfigPropsFromApp(appEntry.appName, mcpServerPid);
                }
            }
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
            if (oldEntry != null) {
                for (String mcpServerPid : oldEntry.mcpServerPids) {
                    removeMcpConfigPropsFromApp(oldEntry.appName, mcpServerPid);
                }
            }
            List<String> mcpServerPidList = (mcpServerPids != null && mcpServerPids.length > 0) ? Arrays.asList(mcpServerPids) : Collections.emptyList();
            appPidToAppEntry.put(appPid, new AppEntry(appName, mcpServerPidList));
            if (!mcpServerPidList.isEmpty()) {
                updateAppMcpConfigProps(appPid);
            }
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
                for (String mcpServerPid : appEntry.mcpServerPids) {
                    removeMcpConfigPropsFromApp(appEntry.appName, mcpServerPid);
                }
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
     * Given an application's PID, looks up its AppEntry, iterates the appEntry's mcpServerPids, looks up
     * each `McpConfigurationComponent`from `mcpServerPidToMcpConfig` map, extracts the `McpServerConfigProps`.
     * We then build a new list of `McpServerConfigProps` by copying all existing entries except ones
     * with a matching `servicePid` (to remove duplicates) then append the new entry. We then add the new
     * immutable list to the map `appNameToMcpConfigProps`
     *
     */
    private void updateAppMcpConfigProps(String appPid) {

        AppEntry appEntry = appPidToAppEntry.get(appPid);
        if (appEntry == null) {
            return;
        }
        String appName = appEntry.appName();
        List<String> mcpServerPidList = appEntry.mcpServerPids();

        for (String mcpServerPid : mcpServerPidList) {
            McpConfigurationComponent mcpConfig = mcpServerPidToMcpConfig.get(mcpServerPid);
            if (mcpConfig != null) {
                McpServerConfigProps props = mcpConfig.getConfigProps();
                if (props != null) {
                    String servicePid = props.servicePid();
                    List<McpServerConfigProps> currentPropsList = appNameToMcpConfigProps.getOrDefault(appName, Collections.emptyList());
                    List<McpServerConfigProps> newPropsList = currentPropsList.stream()
                                                                              .filter(prop -> !servicePid.equals(prop.servicePid()))
                                                                              .collect(Collectors.toCollection(ArrayList::new));
                    newPropsList.add(props);
                    appNameToMcpConfigProps.put(appName, Collections.unmodifiableList(newPropsList));
                }
            }
        }
    }

    /**
     * Given an application's name and the config's mcpServerPid, we can get the current list of `McpServerConfigProps`
     * from `appNameToMcpConfigProps`. We then filter out the ones we want to remove and if the resulting list is empty,
     * remove the application's entry from `appNameToMcpConfigProps` entirely, otherwise create an immutable
     * list and publish it.
     */
    private void removeMcpConfigPropsFromApp(String appName, String servicePid) {
        List<McpServerConfigProps> currentPropsList = appNameToMcpConfigProps.get(appName);
        if (currentPropsList == null) {
            return;
        }
        List<McpServerConfigProps> newPropsList = currentPropsList.stream()
                                                                  .filter(prop -> !servicePid.equals(prop.servicePid()))
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
