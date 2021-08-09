/*******************************************************************************
 * Copyright (c) 2015, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.jaspi;

import java.util.EnumSet;
import java.util.Set;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

/**
 * This class adds a servlet filter for each web app that starts when the
 * jaspic-1.1 feature is enabled. The filter is required to enable the
 * servlet request/response wrapper function in the JSR 196 spec.
 * The filter is added ahead of the application's declared filters.
 * 
 * The web container becomes aware of this ServletContainerInitializer simply
 * by being an active OSGI service that implements ServletContainerInitializer.
 */

@Component(service = { ServletContainerInitializer.class },
           name = "com.ibm.ws.security.jaspi.servlet.container.initializer",
           configurationPolicy = ConfigurationPolicy.IGNORE,
           immediate = true,
           property = { "service.vendor=IBM" })
public class JaspiServletContainerInitializer implements ServletContainerInitializer {

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.ServletContainerInitializer#onStartup(java.util.Set, javax.servlet.ServletContext)
     */
    @Override
    public void onStartup(Set<Class<?>> arg0, ServletContext servletContext) throws ServletException {
        FilterRegistration.Dynamic dynamic =
                        servletContext.addFilter("com.ibm.ws.security.jaspi.servlet.filter", JaspiServletFilter.class);
        dynamic.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), false, "/*");
        dynamic.setAsyncSupported(true);
    }

    /**
     * Declarative services method to activate this component.
     */
    public void activate(ComponentContext context) {}

    /**
     * Declarative services method to deactivate this component.
     */
    public void deactivate(ComponentContext context) {}
}
