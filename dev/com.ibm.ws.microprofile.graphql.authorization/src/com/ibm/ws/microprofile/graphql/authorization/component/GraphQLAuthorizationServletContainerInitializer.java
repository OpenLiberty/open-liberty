/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.graphql.authorization.component;

import java.util.EnumSet;
import java.util.Set;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.io.smallrye.graphql.component.GraphQLServletContainerInitializer;

import org.osgi.service.component.annotations.Component;

@Component(property = { "service.vendor=IBM" })
public class GraphQLAuthorizationServletContainerInitializer implements ServletContainerInitializer {
    private static final TraceComponent tc = Tr.register(GraphQLAuthorizationServletContainerInitializer.class);
    private static final String AUTH_FILTER_NAME = "AuthorizationFilter";

    public void onStartup(Set<Class<?>> classes, ServletContext ctx) throws ServletException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.entry(tc, "onStartup", new Object[] {classes, ctx, ctx.getServletContextName(), ctx.getContextPath()});
        }

        // add servlet filter for ExecutionServlet
        FilterRegistration.Dynamic filterReg = ctx.addFilter(AUTH_FILTER_NAME, AuthorizationFilter.getInstance());
        filterReg.addMappingForServletNames(EnumSet.of(DispatcherType.REQUEST), true, 
                GraphQLServletContainerInitializer.EXECUTION_SERVLET_NAME);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.exit(tc, "onStartup");
        }
    }
}
