/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.kernel.service.util.ServiceCaller;

import io.openliberty.mcp.internal.config.McpServerConfigProps;
import io.openliberty.mcp.internal.introspection.McpIntrospector;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;

/**
 * Deferred MCP endpoint registration listener.
 *
 * <p>Registered from {@link McpServletInitializer#onStartup} via
 * {@link jakarta.servlet.ServletContext#addListener} when no {@code @Tool}-annotated CDI beans
 * were found at servlet-container-initializer time. Because {@link jakarta.servlet.ServletContainerInitializer}
 * listeners are registered before Weld's programmatic listener
 * ({@code Weld50InitialListener}), this listener's {@code contextInitialized} fires
 * <em>after</em> {@code Weld50InitialListener.contextInitialized} completes — which means
 * it fires after all user {@code @Startup} observers (including programmatic
 * {@link io.openliberty.mcp.tools.ToolManager} registrations).
 *
 * <p>At that point {@code addServlet}/{@code addFilter} are still legal because the
 * servlet context is still in its initialisation phase, and {@link ToolRegistry#hasTools()}
 * returns {@code true} if the user registered any tools programmatically.
 *
 * <p>Note: Liberty's webcontainer blocks {@code addListener} from any "programmatically-added"
 * listener's {@code contextInitialized}, even when the listener was originally registered from
 * a {@link jakarta.servlet.ServletContainerInitializer}. Therefore this class directly calls
 * {@link McpIntrospector} instead of adding a {@code McpIntrospectorContextListener}, and
 * handles introspector cleanup in its own {@link #contextDestroyed}.
 */
class McpDeferredEndpointListener implements ServletContextListener {

    private static final TraceComponent tc = Tr.register(McpDeferredEndpointListener.class);
    private static final ServiceCaller<McpIntrospector> introspector = new ServiceCaller<>(McpDeferredEndpointListener.class, McpIntrospector.class);

    private final ToolRegistry registry;
    private final McpServletInitializer initializer;
    private final McpServerConfigProps configProps;
    private final McpCdiExtension extension;

    /** Set to true when this listener registers the MCP endpoint, so contextDestroyed can unregister it. */
    private volatile boolean registered = false;

    McpDeferredEndpointListener(ToolRegistry registry,
                                McpServletInitializer initializer,
                                McpServerConfigProps configProps,
                                McpCdiExtension extension) {
        this.registry = registry;
        this.initializer = initializer;
        this.configProps = configProps;
        this.extension = extension;
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        // All user @Startup observers have now completed. Check whether any tools
        // were registered programmatically (e.g. via ToolManager).
        if (registry.hasTools()) {
            ServletContext context = sce.getServletContext();
            String contextPath = context.getContextPath();
            if (extension.markRegistered(contextPath)) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "Deferred MCP endpoint registration for context path: " + contextPath);
                }
                // Register introspector directly here instead of adding a listener,
                // because Liberty forbids addListener() from a programmatically-added
                // listener's contextInitialized even if this listener was added from a SCI.
                introspector.call(intr -> intr.register(appNameFrom(contextPath), registry));
                registered = true;
                initializer.registerEndpoint(context, configProps);
            }
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        if (registered) {
            String contextPath = sce.getServletContext().getContextPath();
            introspector.call(intr -> intr.unregister(appNameFrom(contextPath)));
        }
    }

    private static String appNameFrom(String contextPath) {
        if (contextPath == null || contextPath.isEmpty() || "/".equals(contextPath)) {
            return "/";
        }
        return contextPath.startsWith("/") ? contextPath.substring(1) : contextPath;
    }
}
