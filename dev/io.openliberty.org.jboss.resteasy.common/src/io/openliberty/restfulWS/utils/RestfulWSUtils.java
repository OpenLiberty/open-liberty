/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.restfulWS.utils;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.security.AccessController;
import java.security.PrivilegedAction;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.PATCH;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.jboss.resteasy.core.ResourceMethodRegistry;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

import io.openliberty.restfulWS.introspector.RESTfulEndpointLoggingIntrospector;

/**
 * A collection of restfulWS-3.0 utility methods.
 */
public class RestfulWSUtils {

    private static final TraceComponent tc = Tr.register(RestfulWSUtils.class);

    /*
     * Register the servlet's restfulWS endpoints with the ResteasyEndpointIntrospector
     * and print them to messages.log if trace is enabled
     */
    @SuppressWarnings("rawtypes")
    @FFDCIgnore(NoClassDefFoundError.class)
    public static void logEndpoints(ServletConfig servletConfig) {

        RESTfulEndpointLoggingIntrospector introspector = getRESTfulEndpointLoggingIntrospector();

        if (introspector != null) {

            ServletContext sc = servletConfig.getServletContext();
            String appname = sc.getServletContextName();
            String contextRoot = sc.getContextPath();
            String servletName = servletConfig.getServletName();
            String servletMapping = servletConfig.getInitParameter("resteasy.servlet.mapping.prefix");
            String resourceList = servletConfig.getInitParameter("resteasy.scanned.resources");
            String[] resources = null;
            if (resourceList != null) {
                resources = resourceList.split(",");
            }
            StringBuffer sb = new StringBuffer();
            sb.append("Endpoints for Application: ").append(appname).append(" - ").append(servletName).append("\n");
            
            if (resources != null) {
                for (String clazz : resources) {

                    // load the class
                    Class resource = loadClass(clazz, sb);
                    if (resource != null) {

                        sb.append(resource.getName()).append("\n");

                        // check if the class is annotated with @Path, if so, get the value
                        String classPath = getAnnotationValue(resource, Path.class, String.class);

                        // get the @Consumes MediaTypes
                        String[] classConsumes = getAnnotationValue(resource, Consumes.class, String[].class);

                        // get the @Produces mediatypes
                        String[] classProduces = getAnnotationValue(resource, Produces.class, String[].class);

                        // get all methods on the class
                        Method[] methods = getMethods(resource);
                        for (Method m : methods) {

                            // check each method for an HTTP verb annotation
                            String httpMethod = getHttpMethod(m);

                            // ignore methods without verbs
                            if (httpMethod != null) {

                                // check if the method also has a @Path annotation
                                String methodPath = getAnnotationValue(m, Path.class, String.class);

                                // get the @Consumes MediaTypes
                                String[] consumes = getAnnotationValue(m, Consumes.class, String[].class);

                                // get the @Produces mediatypes
                                String[] produces = getAnnotationValue(m, Produces.class, String[].class);

                                // build the endpoint string
                                sb.append(httpMethod).append("\t");

                                if (contextRoot.length() > 0 && !contextRoot.startsWith("/")) {
                                    sb.append("/");
                                }
                                sb.append(contextRoot);

                                if (servletMapping.length() > 0 && !servletMapping.startsWith("/")) {
                                    sb.append("/");
                                }
                                sb.append(servletMapping);

                                if (classPath != null) {
                                    if (classPath.length() > 0 && !classPath.startsWith("/")) {
                                        sb.append("/");
                                    }
                                    sb.append(classPath);
                                }

                                if (methodPath != null) {
                                    if (methodPath.length() > 0 && !methodPath.startsWith("/")) {
                                        sb.append("/");
                                    }
                                    sb.append(methodPath);
                                }
                                sb.append(" - ");

                                // method signature
                                sb.append(resource.getName()).append(".").append(m.getName()).append("(");

                                int i = 0;
                                for (Parameter p : m.getParameters()) {
                                    if (i > 0) {
                                        sb.append(", ");
                                    }
                                    sb.append(p.getType().getName());
                                    i++;
                                }
                                sb.append(") return=").append(m.getReturnType().getName()).append(" ");

                                // @Consumes
                                i = 0;
                                sb.append(" consumes={");
                                appendMediaTypes(classConsumes, sb, i);
                                appendMediaTypes(consumes, sb, i);
                                sb.append("} ");

                                // @Produces
                                i = 0;
                                sb.append("produces={");
                                appendMediaTypes(classProduces, sb, i);
                                appendMediaTypes(produces, sb, i);
                                sb.append("}\n");
                            }
                        }
                        sb.append("\n");
                    }
                }
            }

            introspector.addEndpoints(servletConfig, sb);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.info(tc, sb.toString());
            }
        } else if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "unable to find LibertyResteasyEndpointLoggingIntrospector");
        }
    }

    private static RESTfulEndpointLoggingIntrospector getRESTfulEndpointLoggingIntrospector() {
        return AccessController.doPrivileged(new PrivilegedAction<RESTfulEndpointLoggingIntrospector>(){

            @Override
            public RESTfulEndpointLoggingIntrospector run() {
                try {
                Bundle b = FrameworkUtil.getBundle(ResourceMethodRegistry.class);
                if(b != null) {
                    BundleContext bc = b.getBundleContext();
                    ServiceReference<RESTfulEndpointLoggingIntrospector> sr = bc.getServiceReference(RESTfulEndpointLoggingIntrospector.class);
                    return (RESTfulEndpointLoggingIntrospector)bc.getService(sr);
                }
                } catch (NoClassDefFoundError ncdfe) {
                    // ignore - return null
                }
                return null;
            }});
    }

    private static Class<?> loadClass(String clazz, StringBuffer sb) {
        return AccessController.doPrivileged(new PrivilegedAction<Class<?>>() {
            public Class<?> run() {
                try {
                    return Thread.currentThread().getContextClassLoader().loadClass(clazz);
                } catch (ClassNotFoundException e) {
                    sb.append("ERROR WHILE INTROSPECTING - ").append(e.getMessage());
                }
                return null;
            }
        });
    }

    private static Method[] getMethods(Class<?> clazz) {
         return AccessController.doPrivileged(new PrivilegedAction<Method[]>() {
            public Method[] run() {
                return clazz.getDeclaredMethods();
            }
        });
    }

    private static String getHttpMethod(Method m) {
        String httpMethod = null;
        for (Annotation annotation : m.getAnnotations()) {
            if (annotation.annotationType().equals(DELETE.class)) {
                httpMethod = "DELETE";
            }
            if (annotation.annotationType().equals(GET.class)) {
                httpMethod = "GET";
            }
            if (annotation.annotationType().equals(HEAD.class)) {
                httpMethod = "HEAD";
            }
            if (annotation.annotationType().equals(OPTIONS.class)) {
                httpMethod = "OPTIONS";
            }
            if (annotation.annotationType().equals(PATCH.class)) {
                httpMethod = "PATCH";
            }
            if (annotation.annotationType().equals(POST.class)) {
                httpMethod = "POST";
            }
            if (annotation.annotationType().equals(PUT.class)) {
                httpMethod = "PUT";
            }
                
        }
        return httpMethod;
    }

    public static <T> T getAnnotationValue(AnnotatedElement element, Class<? extends Annotation> annoType, Class<T> valueType) {
        T value = null;
        if (element.isAnnotationPresent(annoType)) {
            Method valueMethod;
            try {
                valueMethod = annoType.getMethod("value");
                Annotation anno = element.getAnnotation(annoType);
                return valueType.cast(valueMethod.invoke(anno));
            } catch (Exception e) {
                // ignore, return null
            }
        }
        return value;
    }

    private static void appendMediaTypes(String[] list, StringBuffer sb, int i) {
        if (list != null && list.length > 0) {
            for (String c : list) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append("\"").append(c).append("\"");
                i++;
            }
        }
    }
}
