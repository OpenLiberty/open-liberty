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
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;

import org.jboss.resteasy.plugins.servlet.ResteasyServletInitializer;

@Trivial
public class RESTfulServletContainerInitializer extends ResteasyServletInitializer implements ServletContainerInitializer {
    private final static TraceComponent tc = Tr.register(RESTfulServletContainerInitializer.class, "JAXRS");

    private final static String RESTEASY_MAPPING_PREFIX = "resteasy.servlet.mapping.prefix";
    private final static String IBM_REST_SERVLET_NAME = "com.ibm.websphere.jaxrs.server.IBMRestServlet";
    private final static String RESTEASY_DISPATCHER_NAME = "org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher";
    private final static String RESTEASY_DISPATCHER_30_NAME = "org.jboss.resteasy.plugins.server.servlet.HttpServlet30Dispatcher";
    

    public void onStartup(Set<Class<?>> classes, ServletContext ctx) throws ServletException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "onStartup ", new Object[] {classes, ctx});
        }
        addMappingParam(ctx);
        super.onStartup(classes, ctx);
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
            // user has already set this?  use their settings - we're done!
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
                    ctx.setInitParameter(RESTEASY_MAPPING_PREFIX, mapping);
                    mapped = true;
                }
            }
        }
    }
}
