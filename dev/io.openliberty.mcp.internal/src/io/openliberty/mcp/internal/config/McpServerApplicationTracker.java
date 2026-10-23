/*******************************************************************************
 * Copyright (c) 2025, 2026 IBM Corporation and others.
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
import java.util.concurrent.TimeUnit;

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
 * Tracks applications and their mcp configurations defined in the server.xml
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
     * Map from mcp PID to its corresponding McpConfigurationComponent. When an application arrives referencing mcp PIDs,
     * we are able to look up the actual configComponent object with this map.
     * Must hold lock on {@code this} to access.
     */
    private final Map<String, McpConfigurationComponent> mcpPidToMcpConfigComponent = new HashMap<>();

    /**
     * A record of the application name and the list of mcpPids that application references. Keeps these 2 pieces of
     * per-app data co-located and impossible to get out of sync
     */
    private record AppEntry(String appName, List<String> mcpPids) {};

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
            mcpPidToMcpConfigComponent.clear();
            appPidToAppEntry.clear();
            appNameToMcpConfigProps.clear();
        }
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE,
               policy = ReferencePolicy.DYNAMIC,
               policyOption = ReferencePolicyOption.GREEDY)
    protected void addMcpConfiguration(McpConfigurationComponent config) {
        synchronized (this) {
            mcpPidToMcpConfigComponent.put(config.getServicePid(), config);
            updateAppsUsingMcpConfig(config.getServicePid());
            notifyAll();
        }
    }

    protected void updatedMcpConfiguration(McpConfigurationComponent config) {
        synchronized (this) {
            updateAppsUsingMcpConfig(config.getServicePid());
            notifyAll();
        }
    }

    protected void removeMcpConfiguration(McpConfigurationComponent config) {
        String mcpPid = config.getServicePid();
        synchronized (this) {
            mcpPidToMcpConfigComponent.remove(mcpPid);
            updateAppsUsingMcpConfig(mcpPid);
            notifyAll();
        }
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE,
               policy = ReferencePolicy.DYNAMIC,
               policyOption = ReferencePolicyOption.GREEDY,
               service = Application.class)
    protected void addApp(Map<String, Object> appProps) {
        String appPid = (String) appProps.get("service.pid");
        String appName = (String) appProps.get("name");
        String[] mcpPids = (String[]) appProps.get("mcp");

        if (isTracingAndDebugEnabled) {
            Tr.debug(this, tc, "Application added. Name=" + appName + ", PID=" + appPid +
                               ", mcpPIDs=" + Arrays.toString(mcpPids));
        }

        if (appPid == null && appName == null) {
            return; //We don't ever expect these to be null
        }
        synchronized (this) {
            List<String> mcpPidList = (mcpPids != null && mcpPids.length > 0) ? Arrays.asList(mcpPids) : Collections.emptyList();
            appPidToAppEntry.put(appPid, new AppEntry(appName, mcpPidList));
            if (!mcpPidList.isEmpty()) {
                updateAppMcpConfigProps(appPid);
            }
        }
    }

    protected void updatedApp(Map<String, Object> appProps) {
        String appPid = (String) appProps.get("service.pid");
        String appName = (String) appProps.get("name");
        String[] mcpPids = (String[]) appProps.get("mcp");

        if (isTracingAndDebugEnabled) {
            Tr.debug(this, tc, "Application updated. Name=" + appName + ", PID=" + appPid +
                               ", mcpPIDs=" + Arrays.toString(mcpPids));
        }

        if (appPid == null && appName == null) {
            return; //We don't ever expect these to be null
        }
        synchronized (this) {
            AppEntry oldEntry = appPidToAppEntry.get(appPid);
            if (oldEntry != null && !oldEntry.appName().equals(appName)) {
                appNameToMcpConfigProps.remove(oldEntry.appName());
            }
            List<String> mcpPidList = (mcpPids != null && mcpPids.length > 0) ? Arrays.asList(mcpPids) : Collections.emptyList();
            appPidToAppEntry.put(appPid, new AppEntry(appName, mcpPidList));
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
     * Given an mcp PID that was just added or updated, scan all `appEntries` to find applications
     * that reference it and trigger a config rebuild for each, via `updateAppMcpConfigProps()`
     *
     */
    private void updateAppsUsingMcpConfig(String mcpPid) {
        for (Map.Entry<String, AppEntry> appEntry : appPidToAppEntry.entrySet()) {
            if (appEntry.getValue().mcpPids.contains(mcpPid)) {
                updateAppMcpConfigProps(appEntry.getKey());
            }
        }
    }

    /**
     * Given an application's PID, looks up its AppEntry, and build a complete list of resolved
     * {@link McpServerConfigProps} by streaming the {@code mcpPids}, map each to its {@link McpConfigurationComponent},
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

        List<McpServerConfigProps> newPropsList = appEntry.mcpPids().stream()
                                                          .map(mcpPid -> mcpPidToMcpConfigComponent.get(mcpPid))
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
     * Waits up to {@code timeoutMs} milliseconds for a non-default {@link McpServerConfigProps} to become available
     * for the given application and module, then returns it.
     * <p>
     * Use this in preference to {@link #getConfigForModule} when the caller runs during application startup and
     * the {@link McpConfigurationComponent} may not yet have been activated by DS (for example when the
     * {@code mcp-1.0} feature bundle is loading concurrently with the application's servlet initializer).
     *
     * @param appName    application name
     * @param moduleName module name (may include .war suffix for EAR deployments)
     * @param timeoutMs  maximum time to wait in milliseconds
     * @return the resolved {@link McpServerConfigProps}, or {@link McpServerConfigProps#DEFAULT_CONFIG} if the timeout
     *         expires before config becomes available
     */
    public McpServerConfigProps waitForConfigForModule(String appName, String moduleName, long timeoutMs) {
        if (appName == null) {
            return DEFAULT_CONFIG;
        }

        // Only wait if the app is known to reference at least one mcp PID.
        // If it has no mcp configuration at all, return DEFAULT_CONFIG immediately.
        boolean appHasMcpPids;
        synchronized (this) {
            appHasMcpPids = appPidToAppEntry.values().stream()
                                            .anyMatch(e -> e.appName().equals(appName) && !e.mcpPids().isEmpty());
        }
        if (!appHasMcpPids) {
            return DEFAULT_CONFIG;
        }

        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMs);
        McpServerConfigProps result = getConfigForModule(appName, moduleName);

        while (result == DEFAULT_CONFIG) {
            long remainingNs = deadline - System.nanoTime();
            if (remainingNs <= 0) {
                break;
            }
            synchronized (this) {
                // Re-check under lock before waiting to avoid a missed-notify race
                result = getConfigForModule(appName, moduleName);
                if (result != DEFAULT_CONFIG) {
                    break;
                }
                try {
                    TimeUnit.NANOSECONDS.timedWait(this, Math.min(remainingNs, TimeUnit.MILLISECONDS.toNanos(50)));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            result = getConfigForModule(appName, moduleName);
        }

        if (isTracingAndDebugEnabled) {
            Tr.debug(this, tc, "waitForConfigForModule appName=" + appName + " module=" + moduleName +
                               " result=" + result + (result == DEFAULT_CONFIG ? " (timed out or no config)" : ""));
        }
        return result;
    }

    /**
     * Iterates through `appNameToMcpConfigProps` map to get the registered `McpServerConfigProps` for a particular
     * appName, and a module name if specified, or a wildcard if one is available and a specific one can not
     * be found
     *
     * @param appName The name of the application defined in the server.xml under `<application name="appName" location="app.war">`
     * @param moduleName The name of the module from runtime (may include .war suffix for EAR deployments)
     * @return the McpServerConfigProps for the given application. If no {@code <mcp>} property was set in the server.xml
     * or if a configuration for particular module could not be found, returns {@link McpServerConfigProps.DEFAULT_CONFIG}
     */
    public McpServerConfigProps getConfigForModule(String appName, String moduleName) {
        List<McpServerConfigProps> configProps = appNameToMcpConfigProps.get(appName);

        if (configProps == null || configProps.isEmpty()) {
            return DEFAULT_CONFIG;
        }

        String normalizedModuleName = normalizeModuleName(moduleName);
        McpServerConfigProps result = null;

        for (McpServerConfigProps configProp : configProps) {
            String configuredModuleName = normalizeModuleName(configProp.moduleName());

            if (normalizedModuleName != null && normalizedModuleName.equals(configuredModuleName)) {
                return configProp;
            }

            // no module name specified - use default for all modules
            if (configProp.moduleName() == null) {
                result = configProp;
            }
        }

        return result != null ? result : DEFAULT_CONFIG;
    }

    /**
     * Normalizes module names before comparing server.xml configuration with runtime metadata.
     *
     * For WAR modules inside an EAR, Liberty runtime metadata can report the module using the archive
     * name, for example `war1.war`, while the configured mcp moduleName is `war1`.
     */
    private String normalizeModuleName(String moduleName) {
        if (moduleName != null && moduleName.endsWith(".war")) {
            return moduleName.substring(0, moduleName.length() - ".war".length());
        }

        return moduleName;
    }

}
