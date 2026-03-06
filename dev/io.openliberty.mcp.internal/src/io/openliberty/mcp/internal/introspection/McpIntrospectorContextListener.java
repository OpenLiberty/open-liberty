/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.introspection;

import com.ibm.ws.kernel.service.util.ServiceCaller;

import io.openliberty.mcp.internal.ToolRegistry;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;

public class McpIntrospectorContextListener implements ServletContextListener {
    private static final ServiceCaller<McpIntrospector> introspector = new ServiceCaller<>(McpIntrospectorContextListener.class, McpIntrospector.class);
    private ToolRegistry registry;

    /**
     * @param registry
     */
    public McpIntrospectorContextListener(ToolRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {

        ServletContext context = sce.getServletContext();
        String appName = extractAppName(context);

        introspector.call(introspector -> {
            introspector.register(appName, registry);
        });
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        ServletContext context = sce.getServletContext();
        String appName = extractAppName(context);

        introspector.call(introspector -> {
            introspector.unregister(appName);
        });;
    }

    private String extractAppName(ServletContext context) {
        String contextPath = context.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && !"/".equals(contextPath)) {
            return contextPath.startsWith("/") ? contextPath.substring(1) : contextPath;
        }
        return "unknown-app";
    }
}
