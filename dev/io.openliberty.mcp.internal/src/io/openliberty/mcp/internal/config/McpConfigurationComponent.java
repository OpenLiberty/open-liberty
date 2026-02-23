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

import java.util.Map;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

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

        this.config = new McpServerConfigProps(stateless, moduleName, path, servicePid);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "McpConfigurationComponent activated: servicePid=" + servicePid);
        }
    }

    public McpServerConfigProps getConfigProps() {
        return config;
    }

    public String getServicePid() {
        return servicePid;
    }

}
