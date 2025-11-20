/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal;

import static org.osgi.service.component.annotations.ConfigurationPolicy.IGNORE;

import java.util.Set;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.http.VirtualHost;

import jakarta.servlet.FilterRegistration;
import jakarta.servlet.ServletContainerInitializer;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRegistration.Dynamic;

/**
 *
 */
@Component(configurationPolicy = IGNORE)
public class McpServletInitializer implements ServletContainerInitializer {

    private static final TraceComponent tc = Tr.register(McpServletInitializer.class);

    @Reference
    private volatile VirtualHost virtualHost;
    private String mcpEndpoint = "/mcp"; // TODO: make configurable

    @Override
    public void onStartup(Set<Class<?>> c, ServletContext context) throws ServletException {
        if (ToolRegistry.get().hasTools()) {
            Dynamic reg = context.addServlet("io.openliberty.mcp.servlet", McpServlet.class);
            reg.addMapping(mcpEndpoint);
            reg.setAsyncSupported(true);

            FilterRegistration.Dynamic filterReg = context.addFilter("io.openliberty.mcp.servlet.filter", McpForwardFilter.class);
            filterReg.addMappingForUrlPatterns(null, false, "/mcp/");
            filterReg.setAsyncSupported(true);

            String fullMcpUrl = virtualHost.getUrlString(context.getContextPath() + mcpEndpoint, false);
            Tr.info(tc, "MCP server endpoint: " + fullMcpUrl);
        }
    }

}
