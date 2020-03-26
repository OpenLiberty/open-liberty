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
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;

import com.ibm.ws.container.service.app.deploy.ModuleInfo;
import com.ibm.ws.webcontainer.webapp.WebApp;
import com.ibm.wsspi.adaptable.module.NonPersistentCache;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;

import graphql.schema.GraphQLSchema;

import io.smallrye.graphql.bootstrap.SmallRyeGraphQLBootstrap;
import io.smallrye.graphql.bootstrap.index.IndexInitializer;
import io.smallrye.graphql.execution.ExecutionService;
import io.smallrye.graphql.execution.GraphQLConfig;
import io.smallrye.graphql.servlet.SmallRyeGraphQLExecutionServlet;
import io.smallrye.graphql.servlet.SmallRyeGraphQLSchemaServlet;

import org.osgi.service.component.annotations.Component;
import org.jboss.jandex.IndexView;

@Component(property = { "service.vendor=IBM" })
public class GraphQLServletContainerInitializer implements ServletContainerInitializer {
    private static final String c = GraphQLServletContainerInitializer.class.getName();
    private static final Logger LOG = Logger.getLogger(c);

    public void onStartup(Set<Class<?>> classes, ServletContext ctx) throws ServletException {
        if (LOG.isLoggable(Level.FINER)) {
            LOG.entering(c, "onStartup", new Object[] {classes, ctx});
            LOG.finest("servletContextName: " + ctx.getServletContextName() + " contextPath: " + ctx.getContextPath());
        }

        //TODO: change to debug
        
        
        URL webinfClassesUrl = null;
        try {
            String realPath = ctx.getRealPath("/WEB-INF/classes");
            if (LOG.isLoggable(Level.FINER)) {
                LOG.finest("realPath: " + realPath);
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
                if (LOG.isLoggable(Level.FINEST)) {
                    LOG.finest("moduleInfo.getContainer().getPhysicalPath() == " + containerPhysicalPath);
                }
                
                if (containerPhysicalPath == null) {
                    // this can occur for "system" apps
                    if (LOG.isLoggable(Level.FINEST)) {
                        LOG.finest("Cannot find app path, will not process for GraphQL APIs");
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
                if (LOG.isLoggable(Level.FINEST)) {
                    LOG.log(Level.FINEST, "Failed to find WEB-INF/classes from container in moduleInfo", ex);
                }
            }
        }

        SmallRyeGraphQLBootstrap bootstrap = new SmallRyeGraphQLBootstrap();
        IndexInitializer indexInitializer = new IndexInitializer();
        GraphQLSchema schema;

        IndexView index = indexInitializer.createIndex(webinfClassesUrl);
        schema = bootstrap.bootstrap(index);
        if (LOG.isLoggable(Level.FINEST)) {
            LOG.finest("SmallRye GraphQL initialized");
        }

        ctx.setAttribute(SmallRyeGraphQLSchemaServlet.SCHEMA_PROP, schema);
        GraphQLConfig config = new GraphQLConfig();
        config.setPrintDataFetcherException(true);
        config.setDefaultErrorMessage("Server Error");
        config.setBlackList(Arrays.asList("**empty**"));
        config.setWhiteList(Arrays.asList("**empty**"));
        ExecutionService executionService = new ExecutionService(config, bootstrap);
        executionService.init();
        SmallRyeGraphQLExecutionServlet execServlet = new SmallRyeGraphQLExecutionServlet(executionService, config);
        ServletRegistration.Dynamic execServletReg = ctx.addServlet("SmallRyeGraphQLExecutionServlet", execServlet);
        execServletReg.addMapping("/graphql/*");
        ServletRegistration.Dynamic schemaServletReg = ctx.addServlet("SmallRyeGraphQLSchemaServlet", new SmallRyeGraphQLSchemaServlet());
        schemaServletReg.addMapping("/graphql/schema.graphql");

        if (LOG.isLoggable(Level.FINER)) {
            LOG.exiting(c, "onStartup");
        }
    }
}
