/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.org.jboss.resteasy.common.component;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.servlet.annotation.HandlesTypes;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.Path;
import javax.ws.rs.core.Application;
import javax.ws.rs.ext.Provider;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;

import org.jboss.resteasy.plugins.server.servlet.HttpServlet30Dispatcher;
import org.jboss.resteasy.plugins.server.servlet.ResteasyContextParameters;
import org.jboss.resteasy.plugins.servlet.ResteasyServletInitializer;

@Trivial
@HandlesTypes({Application.class, Path.class, Provider.class})
public class RESTfulServletContainerInitializer extends ResteasyServletInitializer implements ServletContainerInitializer {
    private final static TraceComponent tc = Tr.register(RESTfulServletContainerInitializer.class, "JAXRS");

    private final static String RESTEASY_MAPPING_PREFIX = "resteasy.servlet.mapping.prefix";
    private final static String IBM_REST_SERVLET_NAME = "com.ibm.websphere.jaxrs.server.IBMRestServlet";
    private final static String RESTEASY_DISPATCHER_NAME = "org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher";
    private final static String RESTEASY_DISPATCHER_30_NAME = "org.jboss.resteasy.plugins.server.servlet.HttpServlet30Dispatcher";


    public void onStartup(Set<Class<?>> classes, ServletContext servletContext) throws ServletException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "onStartup ", new Object[] {classes, servletContext});
        }
        addMappingParam(servletContext);
        if (classes == null) {
            return;
        }
        //super.onStartup(classes, ctx);
        Set<Class<?>> appClasses = new HashSet<Class<?>>();
        Set<Class<?>> providers = new HashSet<Class<?>>();
        Set<Class<?>> resources = new HashSet<Class<?>>();

        for (Class<?> clazz : classes) {
            if (clazz.isInterface()) {
                continue;
            }
            if (clazz.isAnnotationPresent(Path.class)) {
                resources.add(clazz);
            }
            if (clazz.isAnnotationPresent(Provider.class)) {
                providers.add(clazz);
            }
            if (Application.class.isAssignableFrom(clazz)){
                appClasses.add(clazz);
            }
        }
        if (appClasses.size() == 0 && resources.size() == 0) {
            return;
        }

        if (appClasses.size() == 0) {
            return;
        }

        for (Class<?> app : appClasses) {
            register(app, providers, resources, servletContext);
        }
    }


    @Override
    protected void register(Class<?> applicationClass, Set<Class<?>> providers, Set<Class<?>> resources, ServletContext servletContext) {
        ApplicationPath path = applicationClass.getAnnotation(ApplicationPath.class);
        ServletRegistration.Dynamic reg;
        String mapping;
        String prefix;
        if (path == null) {
            if (servletContext.getServletRegistration(applicationClass.getName()) == null) {
                // Application subclass has no @ApplicationPath and no declared mappings to use
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "register - no @ApplicationPath and no servlet mapping for "+ applicationClass.getName());
                }
                return;
            }
            reg = servletContext.addServlet(applicationClass.getName(), HttpServlet30Dispatcher.class);
            Collection<String> mappings = reg.getMappings();
            if (mappings == null || mappings.isEmpty()) {
                // no declared mappings
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "register - no @ApplicationPath; servlet declared, but no mappings for "+ applicationClass.getName());
                }
                return;
            }
            mapping = mappings.iterator().next();
            prefix = mapping;
            while (!prefix.equals("/") && (prefix.endsWith("/") || prefix.endsWith("*"))) {
                prefix = prefix.substring(0, prefix.length() - 1);
            }
        } else {
            mapping = path.value();
            if (!mapping.startsWith("/")) mapping = "/" + mapping;
            prefix = mapping;
            if (!prefix.equals("/") && prefix.endsWith("/")) prefix = prefix.substring(0, prefix.length() - 1);
            if (!mapping.endsWith("/*")) {
                if (mapping.endsWith("/")) mapping += "*";
                else mapping += "/*";
            }
            reg = servletContext.addServlet(applicationClass.getName(), HttpServlet30Dispatcher.class);
            reg.setLoadOnStartup(1);
            reg.setAsyncSupported(true);
            reg.addMapping(mapping);
        }

        reg.setInitParameter("javax.ws.rs.Application", applicationClass.getName());
        // resteasy.servlet.mapping.prefix
        reg.setInitParameter("resteasy.servlet.mapping.prefix", prefix);
        
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "register - mapping app " + applicationClass.getName() + " to " + mapping);
        }

        if (resources.size() > 0) {
            StringBuilder builder = new StringBuilder();
            boolean first = true;
            for (Class<?> resource : resources) {
                if (first) {
                    first = false;
                } else {
                    builder.append(",");
                }

                builder.append(resource.getName());
            }
            reg.setInitParameter(ResteasyContextParameters.RESTEASY_SCANNED_RESOURCES, builder.toString());
        }
        if (providers.size() > 0) {
            StringBuilder builder = new StringBuilder();
            boolean first = true;
            for (Class<?> provider : providers) {
                if (first) {
                    first = false;
                } else {
                    builder.append(",");
                }
                builder.append(provider.getName());
            }
            reg.setInitParameter(ResteasyContextParameters.RESTEASY_SCANNED_PROVIDERS, builder.toString());
        }
    }

    /**
     * RESTEasy requires a resteasy.servlet.mapping.prefix parameter to be set if the 
     * web.xml declares a servlet to handle JAX-RS requests AND if that servlet is mapped
     * to paths other than "/*". For example, if the IBMRestServlet is mapped to "/rest/*"
     * then the following code would also be needed in the web.xml:
     * <pre>
         &lt;context-param&gt;
           &lt;param-name&gt;resteasy.servlet.mapping.prefix&lt;/param-name&gt;
           &lt;param-value&gt;/rest&lt;/param-value&gt;
         &lt;/context-param&gt;
     * </pre>
     * 
     * This method adds that programmatically.
     */
    private void addMappingParam(ServletContext ctx) {
        if (ctx.getInitParameter(RESTEASY_MAPPING_PREFIX) != null) {
            // user has already set this for the entire web app - use their settings - we're done!
            return;
        }

        boolean mapped = false;
        Map<String, ? extends ServletRegistration> servletRegistrations = ctx.getServletRegistrations();
        for(Map.Entry<String, ? extends ServletRegistration> entry : servletRegistrations.entrySet()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "addMappingParam servletRegistrations " + entry.getKey() + " = " + entry.getValue());
            }

            ServletRegistration reg = entry.getValue();
            String servletClassName = reg.getClassName();
            if (IBM_REST_SERVLET_NAME.equals(servletClassName) ||
                RESTEASY_DISPATCHER_NAME.equals(servletClassName) ||
                RESTEASY_DISPATCHER_30_NAME.equals(servletClassName)) {
                if (mapped) {
                    Tr.warning(tc, "MULTIPLE_REST_SERVLETS_CWWKW1300W", ctx.getServletContextName());
                }
                Collection<String> mappings = reg.getMappings();
                if (mappings.size() < 1) {
                    // no mappings, assume "/*" but keep processing in case another servlet exists
                    continue;
                }
                if (mappings.size() > 1) {
                    Tr.warning(tc, "MULTIPLE_REST_SERVLET_MAPPINGS_CWWKW1301W", ctx.getServletContextName());
                }
                String mapping = mappings.iterator().next();
                while (mapping != null && mapping.length() > 0 && (mapping.endsWith("*") || mapping.endsWith("/"))) {
                    mapping = mapping.substring(0, mapping.length() - 1);
                }
                if (mapping != null && mapping.length() > 0) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "addMappingParam using mapping: " + mapping);
                    }
                    reg.setInitParameter(RESTEASY_MAPPING_PREFIX, mapping);
                    mapped = true;
                }
            }
        }
    }
}
