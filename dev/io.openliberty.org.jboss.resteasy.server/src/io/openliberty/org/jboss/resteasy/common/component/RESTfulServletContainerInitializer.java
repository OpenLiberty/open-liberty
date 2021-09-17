/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
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

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.jboss.resteasy.plugins.server.servlet.HttpServlet30Dispatcher;
import org.jboss.resteasy.plugins.server.servlet.ResteasyContextParameters;
import org.jboss.resteasy.plugins.servlet.ResteasyServletInitializer;
import org.jboss.resteasy.util.Encode;

@Trivial
@HandlesTypes({Application.class, Path.class, Provider.class})
public class RESTfulServletContainerInitializer extends ResteasyServletInitializer implements ServletContainerInitializer {
    private final static TraceComponent tc = Tr.register(RESTfulServletContainerInitializer.class, "JAXRS");

    private final static String RESTEASY_MAPPING_PREFIX = "resteasy.servlet.mapping.prefix";
    private final static String IBM_REST_SERVLET_NAME = "com.ibm.websphere.jaxrs.server.IBMRestServlet";
    private final static String RESTEASY_DISPATCHER_NAME = "org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher";
    private final static String RESTEASY_DISPATCHER_30_NAME = "org.jboss.resteasy.plugins.server.servlet.HttpServlet30Dispatcher";
    private final static String APPLICATION = "javax.ws.rs.Application";
    private final static String EE8_APP_CLASS_NAME = transformBack("javax.ws.rs.core.Application");


    public void onStartup(Set<Class<?>> classes, ServletContext servletContext) throws ServletException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "onStartup ", new Object[] {classes, servletContext});
        }
        if (classes == null) {
            return;
        }

        // auto-discovered application classes, providers, and resources
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
                if (!getServletsForApplication(clazz, servletContext, false).isEmpty() || clazz.isAnnotationPresent(ApplicationPath.class)) {
                    appClasses.add(clazz);
                } else {
                    Tr.warning(tc, "UNMAPPED_APPLICATION_CWWKW1302W", new Object[] {servletContext.getServletContextName(), clazz.getName()});
                }
            }
        }
        if (appClasses.size() == 0 && resources.size() == 0) {
            return;
        }

        if (appClasses.size() == 0) {
            appClasses.add(Application.class);
        }

        addMappingParam(servletContext, appClasses);
        for (Class<?> app : appClasses) {
            register(app, providers, resources, servletContext);
        }
    }

    private Set<ServletRegistration> getServletsForApplication(Class<?> applicationClass, ServletContext servletContext, boolean includeAppClass) {
        Set<ServletRegistration> set = new HashSet<>();
        ServletRegistration reg = servletContext.getServletRegistration(applicationClass.getName());
        if (reg != null) {
            set.add(reg);
        } else if (includeAppClass && Application.class.equals(applicationClass)) {
            // try EE8 class name in case the app's web.xml hasn't been properly transformed
            reg = servletContext.getServletRegistration(EE8_APP_CLASS_NAME);
            if (reg != null) {
                set.add(reg);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "App {0} is using older (<=EE8) style app class name with javax package name", servletContext.getServletContextName());
                }
            }
        }
        for (ServletRegistration sr : servletContext.getServletRegistrations().values()) {
            String appClassName = sr.getInitParameter(APPLICATION);
            if (applicationClass.getName().equals(appClassName)) {
                set.add(sr);
            }
        }
        return set;
    }

    @Override
    protected void register(Class<?> applicationClass, Set<Class<?>> providers, Set<Class<?>> resources, ServletContext servletContext) {
        
        Set<ServletRegistration> servletsForApp = getServletsForApplication(applicationClass, servletContext, true);
        // ignore @ApplicationPath if application is already mapped in web.xml
        if (!servletsForApp.isEmpty()) {
            for (ServletRegistration servletReg : servletsForApp) {
                String servletClassName = servletReg.getClassName();
                if (servletClassName == null) {
                    ServletRegistration.Dynamic dynReg = servletContext.addServlet(servletReg.getName(), HttpServlet30Dispatcher.class);
                    dynReg.setAsyncSupported(true);
                    dynReg.setInitParameter(APPLICATION, applicationClass.getName());
                }
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "register - mapping app " + applicationClass.getName() + " via web.xml servlet, " + servletReg.getName());
                }
                registerResourcesAndProviders(servletReg, providers, resources);
            }
            return;
        }

        ApplicationPath path = applicationClass.getAnnotation(ApplicationPath.class);
        if (path == null) {
            // Application subclass has no @ApplicationPath and no declared mappings to use
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "register - no @ApplicationPath and no servlet mapping for "+ applicationClass.getName());
            }
            return;
        }
        ServletRegistration.Dynamic reg;
        String mapping = Encode.decode(path.value());
        String prefix;

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

        reg.setInitParameter(APPLICATION, applicationClass.getName());
        // resteasy.servlet.mapping.prefix
        reg.setInitParameter(RESTEASY_MAPPING_PREFIX, prefix);
        
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "register - mapping app " + applicationClass.getName() + " to " + mapping);
        }

        registerResourcesAndProviders(reg, providers, resources);
    }

    private void registerResourcesAndProviders(ServletRegistration reg, Set<Class<?>> providers, Set<Class<?>> resources) {
        reg.setInitParameter("resteasy.proxy.implement.all.interfaces", "true");
        String unwrappedExceptions = reg.getInitParameter("resteasy.unwrapped.exceptions");
        if (unwrappedExceptions == null) {
            reg.setInitParameter("resteasy.unwrapped.exceptions", "jakarta.ejb.EJBException");
        } else if (!unwrappedExceptions.contains("jakarta.ejb.EJBException")){
            reg.setInitParameter("resteasy.unwrapped.exceptions", unwrappedExceptions + ",jakarta.ejb.EJBException");
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
     * web.xml declares a servlet to handle RESTful WS requests AND if that servlet is mapped
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
    private void addMappingParam(ServletContext ctx, Set<Class<?>> appClasses) {
        boolean globallyMapped = false;
        if (ctx.getInitParameter(RESTEASY_MAPPING_PREFIX) != null) {
            // user has already set this for the entire web app - use their settings
            globallyMapped = true;
        }

        Set<String> mappedServletNames = new HashSet<>();
        Map<String, ? extends ServletRegistration> servletRegistrationMap = ctx.getServletRegistrations();
        for(Map.Entry<String, ? extends ServletRegistration> entry : servletRegistrationMap.entrySet()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "addMappingParam servletRegistrations " + entry.getKey() + " = " + entry.getValue());
            }

            ServletRegistration reg = entry.getValue();
            String servletName = reg.getName();
            String servletClassName = reg.getClassName();
            if (IBM_REST_SERVLET_NAME.equals(servletClassName) ||
                RESTEASY_DISPATCHER_NAME.equals(servletClassName) ||
                RESTEASY_DISPATCHER_30_NAME.equals(servletClassName) ||
                Application.class.getName().equals(servletName) ||
                EE8_APP_CLASS_NAME.equals(servletName) ||
                appClasses.stream().anyMatch(c -> c.getName().equals(servletName))) {

                if (mappedServletNames.contains(servletName)) {
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
                if (mapping != null) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "addMappingParam using mapping: " + mapping);
                    }
                    if (!globallyMapped) {
                        reg.setInitParameter(RESTEASY_MAPPING_PREFIX, mapping);
                    }
                    mappedServletNames.add(servletName);
                }
            }
        }
    }

    private static String transformBack(String original) {
        return original.replaceAll("jakarta", "javax");
    }
}