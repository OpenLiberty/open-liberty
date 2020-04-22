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
import java.util.Collections;
import java.util.List;
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

import io.smallrye.graphql.bootstrap.Bootstrap;
import io.smallrye.graphql.execution.ExecutionService;
import io.smallrye.graphql.schema.SchemaBuilder;
import io.smallrye.graphql.schema.model.Schema;
import io.smallrye.graphql.servlet.ExecutionServlet;
import io.smallrye.graphql.servlet.GraphQLConfig;
import io.smallrye.graphql.servlet.IndexInitializer;
import io.smallrye.graphql.servlet.SchemaServlet;

import org.eclipse.microprofile.graphql.ConfigKey;
import org.osgi.service.component.annotations.Component;
import org.jboss.jandex.IndexView;

@Component(property = { "service.vendor=IBM" })
public class GraphQLServletContainerInitializer implements ServletContainerInitializer {
    private static final TraceComponent tc = Tr.register(GraphQLServletContainerInitializer.class);

    @FFDCIgnore({Throwable.class})
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

        
        GraphQLSchema graphQLSchema = null;
        try {
            IndexInitializer indexInitializer = new IndexInitializer();
            IndexView index = indexInitializer.createIndex(webinfClassesUrl);
            Schema schema = SchemaBuilder.build(index);
            graphQLSchema = Bootstrap.bootstrap(schema);
        } catch (Throwable t) {
            Tr.error(tc, "ERROR_GENERATING_SCHEMA_CWMGQ0001E", ctx.getServletContextName());
            throw new ServletException(t);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "SmallRye GraphQL initialized");
        }

        ctx.setAttribute(SchemaServlet.SCHEMA_PROP, graphQLSchema);

        GraphQLConfig config = new GraphQLConfig() {
            public String getDefaultErrorMessage() {
                return ConfigFacade.getOptionalValue(ConfigKey.DEFAULT_ERROR_MESSAGE, String.class)
                                   .orElse("Server Error");
            }

            public boolean isPrintDataFetcherException() {
                return ConfigFacade.getOptionalValue("mp.graphql.printDataFetcherException", boolean.class)
                                   .orElse(false);
            }

            public List<String> getBlackList() {
                return ConfigFacade.getOptionalValue(ConfigKey.EXCEPTION_BLACK_LIST, List.class)
                                   .orElse(Collections.EMPTY_LIST);
            }

            public List<String> getWhiteList() {
                return ConfigFacade.getOptionalValue(ConfigKey.EXCEPTION_WHITE_LIST, List.class)
                                   .orElse(Collections.EMPTY_LIST);
            }

            public boolean isAllowGet() {
                return ConfigFacade.getOptionalValue("mp.graphql.allowGet", boolean.class)
                                   .orElse(false);
            }

            public boolean isMetricsEnabled() {
                return ConfigFacade.getOptionalValue("smallrye.graphql.metrics.enabled", boolean.class)
                                   .orElse(false);
            }
        };
        //config.setPrintDataFetcherException(true);
        //config.setDefaultErrorMessage("Server Error");
        //config.setBlackList(Arrays.asList("**empty**"));
        //config.setWhiteList(Arrays.asList("**empty**"));
        ExecutionService executionService = new ExecutionService(config, graphQLSchema);
        //executionService.init();
        
        String path = "/" + ConfigFacade.getOptionalValue("mp.graphql.contextpath", String.class)
                                        .filter(s -> {return s.replaceAll("/", "").length() > 0;})
                                        .orElse("graphql");
        ExecutionServlet execServlet = new ExecutionServlet(executionService, config);
        ServletRegistration.Dynamic execServletReg = ctx.addServlet("ExecutionServlet", execServlet);
        execServletReg.addMapping(path + "/*");
        ServletRegistration.Dynamic schemaServletReg = ctx.addServlet("SchemaServlet", new SchemaServlet());
        schemaServletReg.addMapping(path + "/schema.graphql");

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.exit(tc, "onStartup");
        }
    }
}
