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
import java.util.Arrays;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.kernel.productinfo.ProductInfo;

import io.openliberty.mcp.internal.McpProtocolVersion;
import io.openliberty.mcp.internal.responses.McpInitializeResult.ServerInfo;

/**
 * Configuration for the MCP Server feature.
 * This component stores its configuration and publishes itself as a service.
 * The McpServerApplicationTracker compares this with Application services to
 * determine which application this configuration belongs to.
 */
@Component(service = McpConfigurationComponent.class,
           configurationPid = "io.openliberty.mcp.internal.config.McpConfigurationComponent",
           configurationPolicy = org.osgi.service.component.annotations.ConfigurationPolicy.REQUIRE)
public class McpConfigurationComponent {

    private static final TraceComponent tc = Tr.register(McpConfigurationComponent.class);

    private volatile McpServerConfigProps config;
    private volatile String servicePid;

    private static final String CFG_MINIMUM_PROTOCOL_VERSION = "minimumProtocolVersion";
    private static final String CFG_MAXIMUM_PROTOCOL_VERSION = "maximumProtocolVersion";

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
        boolean stateless = (boolean) properties.get("stateless");
        long asyncTimeout = (long) properties.get("asyncTimeout");
        Duration sessionTimeout = Duration.ofSeconds((Long) properties.get("sessionTimeout"));

        // Parse flattened serverInfo properties (ibm:flat="true" in metatype.xml)
        ServerInfo serverInfo = parseServerInfo(properties);

        // minimumProtocolVersion and maximumProtocolVersion are beta. Because the metatype provides
        // defaults, the properties are always present, so they are only read when running a beta runtime.
        // Otherwise the defaults are used (minimum 2025-03-26, maximum 2025-11-25).
        McpProtocolVersion minimumProtocolVersion = null;
        McpProtocolVersion maximumProtocolVersion = null;
        if (ProductInfo.getBetaEdition()) {
            minimumProtocolVersion = parseProtocolVersion(properties, CFG_MINIMUM_PROTOCOL_VERSION);
            maximumProtocolVersion = parseProtocolVersion(properties, CFG_MAXIMUM_PROTOCOL_VERSION);

            McpProtocolVersion effectiveMinimum = minimumProtocolVersion != null ? minimumProtocolVersion : McpProtocolVersion.defaultMinimum();
            McpProtocolVersion effectiveMaximum = maximumProtocolVersion != null ? maximumProtocolVersion : McpProtocolVersion.defaultMaximum();
            if (!effectiveMinimum.isBetween(McpProtocolVersion.oldest(), effectiveMaximum)) {
                Tr.error(tc, "CWMCM0046E.protocol.version.range.invalid", effectiveMinimum.getVersion(), effectiveMaximum.getVersion());
                minimumProtocolVersion = null;
                maximumProtocolVersion = null;
            }
        }

        this.config = new McpServerConfigProps(stateless, moduleName, path, servicePid, sessionTimeout, serverInfo, asyncTimeout,
                                               minimumProtocolVersion, maximumProtocolVersion);
    }

    /**
     * Parse a protocol version attribute, which is either "latest" or a supported protocol version.
     * Logs an error and returns null if the value is not valid. The caller then uses the default for that attribute.
     *
     * @param properties the configuration properties
     * @param attributeName the name of the attribute to read
     * @return the protocol version, or null if the attribute is not set or not valid
     */
    @FFDCIgnore(NoSuchElementException.class)
    private McpProtocolVersion parseProtocolVersion(Map<String, Object> properties, String attributeName) {
        String value = (String) properties.get(attributeName);
        if (value == null) {
            return null;
        }
        try {
            return McpProtocolVersion.parseConfigValue(value.trim());
        } catch (NoSuchElementException e) {
            String supportedValues = McpProtocolVersion.LATEST + ", "
                                     + Arrays.stream(McpProtocolVersion.values())
                                             .map(McpProtocolVersion::getVersion)
                                             .collect(Collectors.joining(", "));
            Tr.error(tc, "CWMCM0045E.protocol.version.invalid", attributeName, value, supportedValues);
            return null;
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