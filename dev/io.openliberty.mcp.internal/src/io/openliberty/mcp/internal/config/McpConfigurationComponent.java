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

import java.io.IOException;
import java.util.Dictionary;
import java.util.Map;

import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.openliberty.mcp.internal.responses.McpInitializeResult.ServerInfo;

/**
 * Configuration for the MCP Server feature.
 * This component stores it's configuration and publishes itself as a service.
 * The McpServerApplicationTracker compares this with Application services to
 * determine which application this configuration belongs to.
 *
 */

@Component(service = McpConfigurationComponent.class,
           configurationPid = "io.openliberty.mcp.internal.config.McpConfigurationComponent",
           configurationPolicy = org.osgi.service.component.annotations.ConfigurationPolicy.REQUIRE)
public class McpConfigurationComponent {

    private static final TraceComponent tc = Tr.register(McpConfigurationComponent.class);

    private volatile McpServerConfigProps config;
    private volatile String servicePid;

    @Reference
    protected ConfigurationAdmin configAdmin;

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
        Object statelessObj = properties.get("stateless");
        boolean stateless = statelessObj != null ? Boolean.parseBoolean(String.valueOf(statelessObj)) : false;

        // Retrieve the info sub-element
        ServerInfo serverInfo = parseServerInfo(properties);

        this.config = new McpServerConfigProps(stateless, moduleName, path, servicePid, serverInfo);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "McpConfigurationComponent activated: servicePid=" + servicePid + ", ServerInfo=" + serverInfo);
        }
    }

    /**
     * Parse the server info configuration from the properties.
     *
     * @param properties the configuration properties
     * @return the ServerInfo object with configured or default values
     */
    private ServerInfo parseServerInfo(Map<String, Object> properties) {
        String infoPid = (String) properties.get("info");
        if (infoPid == null) {
            // No info element configured, return defaults from metatype.xml
            return McpServerConfigProps.DEFAULT_SERVER_INFO;
        }

        Configuration infoConfig;
        try {
            infoConfig = configAdmin.getConfiguration(infoPid, null);
        } catch (IOException e) {
            // Error accessing persistent config storage
            // Shouldn't happen in liberty but we'll get an FFDC if it does
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "IOException accessing configuration for infoPid: " + infoPid, e);
            }
            // Return defaults on error
            return McpServerConfigProps.DEFAULT_SERVER_INFO;
        }

        Dictionary<String, Object> infoProperties = infoConfig.getProperties();
        if (infoProperties == null) {
            // Javadoc indicates this is possible during startup
            // We don't expect this case, but check it to ensure we don't NPE
            // Return defaults
            return McpServerConfigProps.DEFAULT_SERVER_INFO;
        }

        String name = (String) infoProperties.get("name");
        String title = (String) infoProperties.get("title");
        String version = (String) infoProperties.get("version");
        String description = (String) infoProperties.get("description");

        return new ServerInfo(name, title, version, description);
    }

    public McpServerConfigProps getConfigProps() {
        return config;
    }

    public String getServicePid() {
        return servicePid;
    }

}