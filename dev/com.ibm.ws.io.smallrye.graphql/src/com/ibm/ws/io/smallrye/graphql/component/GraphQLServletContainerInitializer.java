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
package com.ibm.ws.io.smallrye.graphql.component;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.container.service.app.deploy.ModuleInfo;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.webcontainer.webapp.WebApp;
import com.ibm.wsspi.adaptable.module.NonPersistentCache;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;

import graphql.schema.GraphQLSchema;

import io.smallrye.graphql.bootstrap.SmallRyeGraphQLBootstrap;
import io.smallrye.graphql.bootstrap.index.IndexInitializer;
import io.smallrye.graphql.bootstrap.type.SchemaTypeCreateException;
import io.smallrye.graphql.execution.ExecutionService;
import io.smallrye.graphql.execution.GraphQLConfig;
import io.smallrye.graphql.servlet.SmallRyeGraphQLExecutionServlet;
import io.smallrye.graphql.servlet.SmallRyeGraphQLSchemaServlet;

import org.osgi.service.component.annotations.Component;
import org.jboss.jandex.IndexView;

@Component(property = { "service.vendor=IBM" })
public class GraphQLServletContainerInitializer implements ServletContainerInitializer {
    private static final TraceComponent tc = Tr.register(GraphQLServletContainerInitializer.class);

    @FFDCIgnore({SchemaTypeCreateException.class})
    public void onStartup(Set<Class<?>> classes, ServletContext ctx) throws ServletException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.entry(tc, "onStartup", new Object[] {classes, ctx, ctx.getServletContextName(), ctx.getContextPath()});
        }

        URL webinfClassesUrl = null;
        try {
            String realPath = ctx.getRealPath("/WEB-INF/classes");
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "realPath: " + realPath);
            }
            if (realPath != null) {
                webinfClassesUrl = Paths.get(realPath).toUri().toURL();
            }
        } catch (MalformedURLException ex) {
            throw new ServletException("Unable to find classes in webapp, " + ctx.getServletContextName(), ex);
        }
        if (webinfClassesUrl == null && ctx instanceof WebApp) {
            WebApp wapp = WebApp.class.cast(ctx);
            try {
                NonPersistentCache overlayCache = wapp.getModuleContainer().adapt(NonPersistentCache.class);
                ModuleInfo moduleInfo = (ModuleInfo) overlayCache.getFromCache(ModuleInfo.class);
                String containerPhysicalPath = moduleInfo.getContainer().getPhysicalPath();
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "moduleInfo.getContainer().getPhysicalPath() == " + containerPhysicalPath);
                }
                
                if (containerPhysicalPath == null) {
                    // this can occur for "system" apps
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Cannot find app path, will not process for GraphQL APIs");
                    }
                    return;
                }
                Path containerPath = Paths.get(containerPhysicalPath);
                Path webinfClassesPath = containerPath.resolve("WEB-INF").resolve("classes");
                if (Files.exists(webinfClassesPath)) {
                    webinfClassesUrl = webinfClassesPath.toUri().toURL(); // most likely expanded/loose app
                } else {
                    webinfClassesUrl = containerPath.toUri().toURL(); // most likely a JAR/WAR
                }
                
                
            } catch (Exception ex) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Failed to find WEB-INF/classes from container in moduleInfo", ex);
                }
            }
        }

        IndexInitializer indexInitializer = new IndexInitializer();
        IndexView index = indexInitializer.createIndex(webinfClassesUrl);
        GraphQLSchema schema = null;
        try {
            schema = SmallRyeGraphQLBootstrap.bootstrap(index);
        } catch (SchemaTypeCreateException ex) {
            Tr.error(tc, "ERROR_GENERATING_SCHEMA_CWMGQ0001E", ctx.getServletContextName());
            throw new ServletException(ex);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "SmallRye GraphQL initialized");
        }

        ctx.setAttribute(SmallRyeGraphQLSchemaServlet.SCHEMA_PROP, schema);
        GraphQLConfig config = new GraphQLConfig();
        config.setPrintDataFetcherException(true);
        config.setDefaultErrorMessage("Server Error");
        config.setBlackList(Arrays.asList("**empty**"));
        config.setWhiteList(Arrays.asList("**empty**"));
        ExecutionService executionService = new ExecutionService(config, schema);
        executionService.init();
        
        String path = "/" + ConfigFacade.getOptionalValue("mp.graphql.contextpath", String.class)
                                        .filter(s -> {return s.replaceAll("/", "").length() > 0;})
                                        .orElse("graphql");
        SmallRyeGraphQLExecutionServlet execServlet = new SmallRyeGraphQLExecutionServlet(executionService, config);
        ServletRegistration.Dynamic execServletReg = ctx.addServlet("SmallRyeGraphQLExecutionServlet", execServlet);
        execServletReg.addMapping(path + "/*");
        ServletRegistration.Dynamic schemaServletReg = ctx.addServlet("SmallRyeGraphQLSchemaServlet", new SmallRyeGraphQLSchemaServlet());
        schemaServletReg.addMapping(path + "/schema.graphql");

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.exit(tc, "onStartup");
        }
    }
}
