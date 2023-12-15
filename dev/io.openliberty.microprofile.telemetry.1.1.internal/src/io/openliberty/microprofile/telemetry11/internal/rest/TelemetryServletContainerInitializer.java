/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.telemetry11.internal.rest;

import static org.osgi.service.component.annotations.ConfigurationPolicy.IGNORE;

import java.util.Set;

import org.osgi.service.component.annotations.Component;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.openliberty.microprofile.telemetry.internal.common.rest.TelemetryServletRequestListener;
import javax.servlet.FilterRegistration;
import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

/**
 * Registers our ServletRequestListener
 */
@Component(configurationPolicy = IGNORE)
public class TelemetryServletContainerInitializer implements ServletContainerInitializer {

    private static final TraceComponent tc = Tr.register(TelemetryServletContainerInitializer.class);

    /** {@inheritDoc} */
    @Override
    public void onStartup(Set<Class<?>> c, ServletContext sc) throws ServletException {
        sc.addListener(TelemetryServletRequestListener.class);

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Enable TelemetryServletFilter");
        }
        FilterRegistration.Dynamic filterRegistration = sc.addFilter("io.openliberty.microprofile.telemetry11.internal.rest.TelemetryServletFilter",
                                                                     TelemetryServletFilter.class);
        filterRegistration.addMappingForUrlPatterns(null, true, "/*");
        filterRegistration.setAsyncSupported(true);

    }

}
