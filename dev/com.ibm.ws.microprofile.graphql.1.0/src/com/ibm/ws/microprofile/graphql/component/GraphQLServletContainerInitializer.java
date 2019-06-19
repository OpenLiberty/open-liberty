/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.graphql.component;

import java.util.Set;

import javax.annotation.security.DeclareRoles;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.spi.BeanManager;
import javax.servlet.FilterRegistration;
import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.cdi.CDIService;

import graphql.schema.GraphQLSchema;
import graphql.schema.idl.SchemaPrinter;
import graphql.servlet.SimpleGraphQLHttpServlet;


/**
 * This class is intended to create the GraphQL servlet instance.
 */
@Component(property = { "service.vendor=IBM" })
@ApplicationScoped
public class GraphQLServletContainerInitializer implements ServletContainerInitializer {
    private final static TraceComponent tc = Tr.register(GraphQLServletContainerInitializer.class);
    private final static String PATH_PROPERTY = "org.eclipse.microprofile.graphql.contextpath";

    private CDIService cdiService;

    private String path = ConfigFacade.getOptionalValue(PATH_PROPERTY, String.class)
                                      .filter(s -> {return s.replaceAll("/", "").length() > 0;})
                                      .orElse("graphql");

    private String servletname = "GraphQLServlet";

    @Override
    public void onStartup(Set<Class<?>> classes, ServletContext sc) throws ServletException {
        BeanManager beanManager = cdiService.getCurrentBeanManager();
        GraphQLSchema schema = GraphQLExtension.createSchema(beanManager);

        if (schema != null) {
            GraphQLExtension.forEachComponentInApp(cls -> {
                DeclareRoles dRoles = cls.getAnnotation(DeclareRoles.class);
                if (dRoles != null) {
                    String[] roles = dRoles.value();
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Roles declared from " + cls + ": {0}", roles);
                    }
                    sc.declareRoles(dRoles.value());
                }
            });

            SimpleGraphQLHttpServlet graphQLServlet = SimpleGraphQLHttpServlet.newBuilder(schema).build();
            ServletRegistration.Dynamic endpointservlet = sc.addServlet(servletname, graphQLServlet);
            endpointservlet.addMapping("/" + path + "/*");
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "GraphQL servlet registered at /" + path);
            }
            
            //TODO: ensure that we document that @DeclareRoles, @DenyAll, @RolesAllowed or @PermitAll is required on at
            // least one @GraphQLApi class in order to enable security checks
            FilterRegistration.Dynamic authFilter = sc.addFilter("AuthFilter", new AuthFilter());
            authFilter.addMappingForServletNames(null, true, servletname);

            GraphQLSchemaServlet graphQLSchemaServlet = new GraphQLSchemaServlet(schema);
            ServletRegistration.Dynamic schemaservlet = sc.addServlet(servletname + "Schema", graphQLSchemaServlet);
            String schemaPath = "/" + path + "/schema.graphql";
            schemaservlet.addMapping(schemaPath);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "GraphQL schema servlet registered at /" + path + "/schema.graphql");
                if (tc.isDebugEnabled()) {
                    String schemaText = "Schema at: " + sc.getRealPath(schemaPath) + System.lineSeparator()
                        + new SchemaPrinter().print(schema);
                    Tr.debug(tc, schemaText);
                }
            }
        }
    }

    @Reference
    protected void setCdiService(CDIService cdiService) {
        this.cdiService = cdiService;
    }

    protected void unsetCdiService(CDIService cdiService) {
        this.cdiService = null;
    }
}
