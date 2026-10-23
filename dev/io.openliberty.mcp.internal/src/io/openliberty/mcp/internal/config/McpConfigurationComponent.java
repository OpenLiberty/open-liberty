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

import java.time.Duration;
import java.util.Map;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.openliberty.mcp.internal.responses.McpInitializeResult.ServerInfo;

/**
 * Configuration for the MCP Server feature.
 * This component stores its configuration and publishes itself as a service.
 * The McpServerApplicationTracker compares this with Application services to
 * determine which application this configuration belongs to.
 */
@Component(service = McpConfigurationComponent.class,
           configurationPid = "mcp",
           configurationPolicy = org.osgi.service.component.annotations.ConfigurationPolicy.REQUIRE)
public class McpConfigurationComponent {

    private static final TraceComponent tc = Tr.register(McpConfigurationComponent.class);

    private volatile McpServerConfigProps config;
    private volatile String servicePid;

    @Activate
    protected void activate(Map<String, Object> properties) {
        processConfig(properties);
    }

    @Modified
    protected void modified(Map<String, Object> properties) {
        processConfig(properties);
    }

    @Deactivate
    protected void deactivate() {
        this.servicePid = null;
        this.config = null;
    }

    private void processConfig(Map<String, Object> properties) {
        this.servicePid = (String) properties.get("service.pid");
        String moduleName = (String) properties.get("moduleName");
        String path = (String) properties.get("path");
        // Properties for duration and boolean fields may arrive in three forms depending
        // on when Config Admin delivers the configuration relative to metatype processing:
        //   (a) already converted: Long (for durations) or Boolean
        //   (b) raw string from XML: e.g. "5s", "600s", "true" — when a transient
        //       pre-metatype config entry is activated before type conversion runs
        //   (c) absent (null): when the mcp-1.0 bundle loads after the config was first
        //       evaluated and the entry carries no defaults yet
        // All three cases are handled below; defaults match the metatype declarations.
        boolean stateless = toBoolean(properties.get("stateless"), false);
        long asyncTimeout = toDurationMs(properties.get("asyncTimeout"), McpServerConfigProps.DEFAULT_ASYNC_TIMEOUT_MS);
        Duration sessionTimeout = Duration.ofSeconds(toDurationS(properties.get("sessionTimeout"), 600L));

        // Parse flattened serverInfo properties (ibm:flat="true" in metatype.xml)
        ServerInfo serverInfo = parseServerInfo(properties);

        this.config = new McpServerConfigProps(stateless, moduleName, path, servicePid, sessionTimeout, serverInfo, asyncTimeout);
    }

    /**
     * Converts a config property value to a boolean.
     * Handles Long (already converted by metatype, though unusual for Boolean),
     * Boolean (normal metatype-converted case), String ("true"/"false" from
     * a pre-metatype config entry), or null (absent — use defaultValue).
     */
    private static boolean toBoolean(Object value, boolean defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return Boolean.parseBoolean(value.toString());
    }

    /**
     * Converts a config property value to a duration in milliseconds.
     * The metatype declares {@code ibm:type="duration(ms)"} which Liberty converts
     * to a Long. A pre-metatype config entry delivers the raw XML string instead
     * (e.g. "5s", "30s", "500ms"). Null means the property is absent; use defaultMs.
     */
    private static long toDurationMs(Object value, long defaultMs) {
        if (value == null) {
            return defaultMs;
        }
        if (value instanceof Long) {
            return (Long) value;
        }
        return parseLibertyDurationToMs(value.toString(), defaultMs);
    }

    /**
     * Converts a config property value to a duration in seconds.
     * The metatype declares {@code ibm:type="duration(s)"} which Liberty converts
     * to a Long. A pre-metatype config entry delivers the raw XML string instead
     * (e.g. "600s", "10m"). Null means the property is absent; use defaultS.
     */
    private static long toDurationS(Object value, long defaultS) {
        if (value == null) {
            return defaultS;
        }
        if (value instanceof Long) {
            return (Long) value;
        }
        // Convert to ms first, then to seconds
        return parseLibertyDurationToMs(value.toString(), defaultS * 1000L) / 1000L;
    }

    /**
     * Parses a Liberty duration string (e.g. "5s", "500ms", "2m", "1h") to milliseconds.
     * Returns {@code defaultMs} if the string is null, empty, or not parseable.
     * Liberty duration strings use suffix units: ms, s, m, h, d.
     */
    private static long parseLibertyDurationToMs(String value, long defaultMs) {
        if (value == null || value.isEmpty()) {
            return defaultMs;
        }
        try {
            if (value.endsWith("ms")) {
                return Long.parseLong(value.substring(0, value.length() - 2).trim());
            } else if (value.endsWith("s")) {
                return Long.parseLong(value.substring(0, value.length() - 1).trim()) * 1000L;
            } else if (value.endsWith("m")) {
                return Long.parseLong(value.substring(0, value.length() - 1).trim()) * 60_000L;
            } else if (value.endsWith("h")) {
                return Long.parseLong(value.substring(0, value.length() - 1).trim()) * 3_600_000L;
            } else if (value.endsWith("d")) {
                return Long.parseLong(value.substring(0, value.length() - 1).trim()) * 86_400_000L;
            } else {
                // Plain number with no unit — treat as milliseconds
                return Long.parseLong(value.trim());
            }
        } catch (NumberFormatException e) {
            return defaultMs;
        }
    }

    /**
     * Parse the server info configuration from flattened properties.
     * With ibm:flat="true", nested properties arrive as "info.0.name", "info.0.version", etc...
     *
     * @param properties the configuration properties
     * @return the ServerInfo object with configured or default values
     */
    private ServerInfo parseServerInfo(Map<String, Object> properties) {
        String name = (String) properties.get("info.0.name");
        String title = (String) properties.get("info.0.title");
        String version = (String) properties.get("info.0.version");
        String description = (String) properties.get("info.0.description");

        return new ServerInfo(name, title, version, description);
    }

    public McpServerConfigProps getConfigProps() {
        return config;
    }

    public String getServicePid() {
        return servicePid;
    }

}