/*******************************************************************************
 * Copyright (c) 2025, 2026 IBM Corporation and others.
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
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.kernel.service.util.ServiceCaller;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;
import com.ibm.wsspi.http.VirtualHost;

import io.openliberty.mcp.internal.config.McpServerApplicationTracker;
import io.openliberty.mcp.internal.config.McpServerConfigProps;
import io.openliberty.mcp.internal.introspection.McpIntrospectorContextListener;
import jakarta.enterprise.inject.spi.CDI;
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
    private static final ServiceCaller<McpServerApplicationTracker> mcpAppTrackerService = new ServiceCaller<>(McpServletInitializer.class, McpServerApplicationTracker.class);
    public static final String STATELESS_INIT_PARAM = "io.openliberty.mcp.internal.config.stateless";

    @Reference
    private volatile VirtualHost virtualHost;

    @Override
    @FFDCIgnore(IllegalStateException.class)
    public void onStartup(Set<Class<?>> c, ServletContext context) throws ServletException, IllegalStateException {
        try {
            McpCdiExtension extension = CDI.current().select(McpCdiExtension.class).get();
            ToolRegistry registry = extension.getCurrentToolRegistry();

            ComponentMetaData componentMetaData = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
            String appName = getApplicationName(componentMetaData);
            String moduleName = getModuleName(componentMetaData);

            McpServerConfigProps configProps = resolveConfigProps(appName, moduleName);

            // Fast path: tools already present from @Tool CDI scanning — register immediately.
            if (registry.hasTools()) {
                // markRegistered returns true only on first call for this context path,
                // preventing the deferred listener from double-registering.
                if (extension.markRegistered(context.getContextPath())) {
                    context.addListener(new McpIntrospectorContextListener(registry));
                    registerEndpoint(context, configProps);
                }
            } else {
                // Deferred path: no @Tool beans found yet. Add a ServletContextListener that
                // fires after Weld50InitialListener.contextInitialized (and therefore after all
                // user @Startup observers) because listener ordering matches registration order
                // and this listener is registered from a ServletContainerInitializer.
                context.addListener(new McpDeferredEndpointListener(registry, this, configProps, extension));
            }
        } catch (IllegalStateException e) {
            String inactiveCdiMsg = "The MCP server endpoint for the application {0} is unavailable due to CDI being inactive. Verify that any MCP annotations are placed on methods of CDI beans that have an appropriate scope annotation (for example, @ApplicationScoped).";
            Tr.event(tc, inactiveCdiMsg, context.getServletContextName()); // called if CDI.current() has an issue
        }
    }

    /**
     * Registers the MCP servlet and forward filter on the given servlet context.
     * Package-private so {@link McpCdiExtension} can call it from the deferred Startup observer.
     *
     * @param context the servlet context for the web module
     * @param configProps the resolved MCP configuration for this module
     */
    void registerEndpoint(ServletContext context, McpServerConfigProps configProps) {

        String path = configProps.path() != null ? configProps.path() : McpServerConfigProps.FALLBACK_PATH;

        if (!path.startsWith("/")) {
            path = "/" + path;
        }

        Dynamic reg = context.addServlet("io.openliberty.mcp.servlet", McpServlet.class);
        reg.addMapping(path);
        reg.setAsyncSupported(true);
        reg.setInitParameter(STATELESS_INIT_PARAM, String.valueOf(configProps.stateless()));

        FilterRegistration.Dynamic filterReg = context.addFilter("io.openliberty.mcp.servlet.filter", new McpForwardFilter(path));
        filterReg.addMappingForUrlPatterns(null, false, path + "/");
        filterReg.setAsyncSupported(true);

        String fullMcpUrl = virtualHost.getUrlString(context.getContextPath() + path, false);
        Tr.info(tc, "CWMCM0008I.mcp.endpoint.url", fullMcpUrl);
    }

    private McpServerConfigProps resolveConfigProps(String appName, String moduleName) {
        if (appName != null) {
            McpServerConfigProps configProps = mcpAppTrackerService.run(tracker -> tracker.getConfigForModule(appName, moduleName)).get();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Looking up MCP config for application: " + appName + ", module: " + moduleName +
                                   ", found: " + configProps);
            }
            return configProps;
        }
        return McpServerConfigProps.DEFAULT_CONFIG;
    }

    private String getApplicationName(ComponentMetaData componentMetaData) {
        if (componentMetaData != null) {
            return componentMetaData.getJ2EEName().getApplication();
        }
        return null;
    }

    private String getModuleName(ComponentMetaData componentMetaData) {
        if (componentMetaData != null) {
            String moduleName = componentMetaData.getJ2EEName().getModule();

            // Enhanced debug logging to investigate when .war suffix is present
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                String appName = componentMetaData.getJ2EEName().getApplication();
                boolean hasWarSuffix = moduleName != null && moduleName.endsWith(".war");
                Tr.debug(this, tc, "Module name retrieved: '" + moduleName +
                                   "', hasWarSuffix=" + hasWarSuffix +
                                   ", application='" + appName + "'");
            }

            return moduleName;
        }
        return null;
    }

}
