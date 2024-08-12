/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.http.monitor;

import static org.osgi.service.component.annotations.ConfigurationPolicy.IGNORE;

import java.util.Set;

import org.osgi.service.component.annotations.Component;

import com.ibm.ws.kernel.productinfo.ProductInfo;

import javax.servlet.FilterRegistration;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

/**
 *
 */
@Component(configurationPolicy = IGNORE)
public class ServletContainerInitializer implements javax.servlet.ServletContainerInitializer {

	// happens when app id deployed
	@Override
	public void onStartup(Set<Class<?>> c, ServletContext sc) throws ServletException {
		/*
		 * Just prevent Servlet filter registration. This bundle starts from an
		 * auto-feature. User's are not explicitly enabling this so lets not throw an
		 * exception. We'll quietly get out of the way.
		 */
		if (!ProductInfo.getBetaEdition()) {
			return;
		}

		FilterRegistration.Dynamic filterRegistration = sc.addFilter("io.openliberty.http.monitor.ServletFilter",
				ServletFilter.class);
		filterRegistration.addMappingForUrlPatterns(null, true, "/*");
		filterRegistration.setAsyncSupported(true);

	}
}
