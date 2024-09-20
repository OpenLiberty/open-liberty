/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
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
package com.ibm.ws.microprofile.graphql.authorization.component;

import java.util.EnumSet;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import javax.servlet.ServletContext;

import org.osgi.service.component.annotations.Component;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.io.smallrye.graphql.component.GraphQLSecurityInitializer;
import com.ibm.ws.io.smallrye.graphql.component.GraphQLServletContainerInitializer;

@Component(property = { "service.vendor=IBM" })
public class GraphQLAuthorizationInitializer implements GraphQLSecurityInitializer {
    private static final TraceComponent tc = Tr.register(GraphQLAuthorizationInitializer.class);
    private static final String AUTH_FILTER_NAME = "AuthorizationFilter";

    public void onStartup(ServletContext ctx) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "onStartup", new Object[] {ctx, ctx.getServletContextName(), ctx.getContextPath()});
        }

        // add servlet filter for ExecutionServlet
        FilterRegistration.Dynamic filterReg = ctx.addFilter(AUTH_FILTER_NAME, AuthorizationFilter.getInstance());
        filterReg.addMappingForServletNames(EnumSet.of(DispatcherType.REQUEST), true, 
                GraphQLServletContainerInitializer.EXECUTION_SERVLET_NAME);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "onStartup");
        }
    }
}
