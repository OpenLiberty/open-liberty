/*******************************************************************************
 * Copyright (c) 2024, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.telemetry.user.feature;

import static org.osgi.service.component.annotations.ConfigurationPolicy.IGNORE;

import java.util.Set;

import javax.servlet.FilterRegistration;
import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.osgi.service.component.annotations.Component;

/**
 * Registers our ServletRequestListener
 */
@Component(configurationPolicy = IGNORE)
public class TelemetryServletContainerInitializer implements ServletContainerInitializer {

    /** {@inheritDoc} */
    @Override
    public void onStartup(Set<Class<?>> c, ServletContext sc) throws ServletException {
        System.out.println("UserFeatureTest: Enable UserFeatureServletFilter");
        FilterRegistration.Dynamic filterRegistration = sc.addFilter("io.openliberty.telemetry.user.feature.UserFeatureServletFilter",
                                                                     UserFeatureServletFilter.class);
        filterRegistration.addMappingForUrlPatterns(null, true, "/userfeature");
        filterRegistration.setAsyncSupported(true);
    }

}
