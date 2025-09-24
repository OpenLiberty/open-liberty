/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal;

import static org.osgi.service.component.annotations.ConfigurationPolicy.IGNORE;

import java.util.Set;

import org.osgi.service.component.annotations.Component;

import jakarta.servlet.ServletContainerInitializer;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRegistration.Dynamic;

/**
 *
 */
@Component(configurationPolicy = IGNORE)
public class McpServletInitializer implements ServletContainerInitializer {

    @Override
    public void onStartup(Set<Class<?>> c, ServletContext context) throws ServletException {
        if (ToolRegistry.get().hasTools()) {
            Dynamic reg = context.addServlet("io.openliberty.mcp.servlet", McpServlet.class);
            reg.addMapping("/mcp"); // TODO: make configurable
        }
    }

}
