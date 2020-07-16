/*******************************************************************************
 * Copyright (c) 2016, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package web;

import java.util.EnumSet;
import java.util.Set;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

/**
 * ServletContainerInitializer to install a request filter dynamically.
 */
public class ServletContainerInitializerImpl implements ServletContainerInitializer {

    @Override
    public void onStartup(Set<Class<?>> arg0, ServletContext servletContext) throws ServletException {
        FilterRegistration.Dynamic dynamic = servletContext.addFilter("sciProgrammaticFilter", SCIProgrammaticRequestFilter.class);
        dynamic.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), false, "/*");
    }

}
