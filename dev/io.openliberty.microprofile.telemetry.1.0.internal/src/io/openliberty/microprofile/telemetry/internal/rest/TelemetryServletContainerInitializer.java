/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.telemetry.internal.rest;

import static org.osgi.service.component.annotations.ConfigurationPolicy.IGNORE;

import java.util.Set;

import org.osgi.service.component.annotations.Component;

import jakarta.servlet.FilterRegistration;
import jakarta.servlet.ServletContainerInitializer;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;

/**
 * Registers our ServletRequestListener
 */
@Component(configurationPolicy = IGNORE)
public class TelemetryServletContainerInitializer implements ServletContainerInitializer {

    /** {@inheritDoc} */
    @Override
    public void onStartup(Set<Class<?>> c, ServletContext sc) throws ServletException {
        sc.addListener(TelemetryServletRequestListener.class);
        FilterRegistration.Dynamic filterRegistration = sc.addFilter("io.openliberty.microprofile.telemetry.internal.rest.TelemetryServletFilter", TelemetryServletFilter.class);
        filterRegistration.addMappingForUrlPatterns(null, true, "/*");
        filterRegistration.setAsyncSupported(true);
    }

}
