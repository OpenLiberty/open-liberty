/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package cdi.dynamic;

import java.io.PrintWriter;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cdi.servlets.CDIDynamicFilter;
import cdi.servlets.CDIDynamicListener;
import cdi.servlets.CDIDynamicServlet;

/**
 * CDI Testing: Servlet Container Dynamic registration
 */
public class CDIDynamicInitializer implements ServletContainerInitializer {

    // Initializer API
    @Override
    public void onStartup(Set<Class<?>> arg0, ServletContext servletContext) throws ServletException {
        registerServlet(servletContext);
        registerFilter(servletContext);
        registerListener(servletContext);
    }

    //

    public static final String SERVLET_DYNAMIC_NAME = "CDIDynamicServlet";
    public static final String SERVLET_DYNAMIC_MAPPING = "/CDIDynamicServlet";

    public static final String FILTER_DYNAMIC_NAME = "CDIDynamicFilter";
    public static final boolean REGISTER_FILTER_AFTER_DECLARED = true;
    public static final EnumSet<DispatcherType> DISPATCHER_REQUEST = null;

    private void registerServlet(ServletContext servletContext) {
        ServletRegistration servletRegistration = servletContext.addServlet(SERVLET_DYNAMIC_NAME, CDIDynamicServlet.class);
        servletRegistration.addMapping(SERVLET_DYNAMIC_MAPPING);
    }

    private void registerFilter(ServletContext servletContext) {
        FilterRegistration filterRegistration = servletContext.addFilter(FILTER_DYNAMIC_NAME, CDIDynamicFilter.class);
        filterRegistration.addMappingForUrlPatterns(DISPATCHER_REQUEST, REGISTER_FILTER_AFTER_DECLARED, SERVLET_DYNAMIC_MAPPING);
    }

    private void registerListener(ServletContext servletContext) {
        servletContext.addListener(CDIDynamicListener.class);
    }

    //

    public static void verifyRegistration(HttpServletRequest servletRequest, HttpServletResponse servletResponse, PrintWriter responseWriter) {
        verifyServletRegistration(servletRequest, servletResponse, responseWriter);
        verifyFilterRegistration(servletRequest, servletResponse, responseWriter);
        verifyListenerRegistration(servletRequest, servletResponse, responseWriter);
    }

    private static void verifyServletRegistration(HttpServletRequest servletRequest, HttpServletResponse servletResponse, PrintWriter responseWriter) {
        String responsePrefix = "CDIServletRegistrar.verifyServletRegistration";
        responseWriter.println(responsePrefix + ":" + "Entry");

        ServletContext servletContext = servletRequest.getServletContext();

        ServletRegistration servletRegistration = servletContext.getServletRegistration(SERVLET_DYNAMIC_NAME);

        responseWriter.println(responsePrefix + ":" + "Servlet Registration [ " + servletRegistration + " ]");

        if (servletRegistration == null) {
            responseWriter.println(responsePrefix + ":" + "FAILED");

        } else {
            Collection<String> servletMappings = servletRegistration.getMappings();
            responseWriter.println(responsePrefix + ":" + "Servlet Mappings [ " + servletMappings.size() + " ]");

            if (servletMappings.isEmpty()) {
                responseWriter.println(responsePrefix + ":" + "FAILED (Empty)");

            } else {
                String selectedMapping = null;
                for (String servletMapping : servletMappings) {
                    selectedMapping = servletMapping;
                    responseWriter.println(responsePrefix + ":" + "  Mapping [ " + servletMapping + " ]");
                }

                if (servletMappings.size() != 1) {
                    responseWriter.println(responsePrefix + ":" + "FAILED (Size != 1)");

                } else {
                    if ((selectedMapping == null) || !selectedMapping.equals(SERVLET_DYNAMIC_MAPPING)) {
                        responseWriter.println(responsePrefix + ":" + "FAILED (Unexpected mapping)");
                    }
                }
            }
        }

        responseWriter.println(responsePrefix + ":" + "Exit");
    }

    private static void verifyFilterRegistration(HttpServletRequest servletRequest, HttpServletResponse servletResponse, PrintWriter responseWriter) {
        String responsePrefix = "CDIServletRegistrar.verifyFilterRegistration";
        responseWriter.println(responsePrefix + ":" + "Entry");

        ServletContext servletContext = servletRequest.getServletContext();

        FilterRegistration filterRegistration = servletContext.getFilterRegistration(FILTER_DYNAMIC_NAME);
        if (filterRegistration == null) {
            responseWriter.println(responsePrefix + ":" + "FAILED (Empty)");

        } else {
            Collection<String> urlPatterns = filterRegistration.getUrlPatternMappings();
            responseWriter.println(responsePrefix + ":" + "Servlet Mappings [ " + urlPatterns.size() + " ]");

            if (urlPatterns.isEmpty()) {
                responseWriter.println(responsePrefix + ":" + "FAILED (Empty)");

            } else {
                String selectedPattern = null;
                for (String urlPattern : urlPatterns) {
                    selectedPattern = urlPattern;
                    responseWriter.println(responsePrefix + ":" + "  URL pattern [ " + urlPattern + " ]");
                }

                if (urlPatterns.size() != 1) {
                    responseWriter.println(responsePrefix + ":" + "FAILED (Size != 1)");

                } else {
                    if ((selectedPattern == null) || !selectedPattern.equals(SERVLET_DYNAMIC_MAPPING)) {
                        responseWriter.println(responsePrefix + ":" + "FAILED (Unexpected pattern)");
                    }
                }
            }
        }

        responseWriter.println(responsePrefix + ":" + "Exit");
    }

    // TODO: Don't know how to verify the listener registration.
    private static void verifyListenerRegistration(HttpServletRequest servletRequest, HttpServletResponse servletResponse, PrintWriter responseWriter) {
        String responsePrefix = "CDIServletRegistrar.verifyListenerRegistration";
        responseWriter.println(responsePrefix + ":" + "Entry");

        // ServletContext servletContext = servletRequest.getServletContext();
        responseWriter.println(responsePrefix + ":" + "NoOp");

        responseWriter.println(responsePrefix + ":" + "Exit");
    }
}
