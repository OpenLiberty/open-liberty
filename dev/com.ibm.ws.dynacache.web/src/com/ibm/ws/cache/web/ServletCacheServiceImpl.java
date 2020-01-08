/*******************************************************************************
 * Copyright (c) 1997, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cache.web;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRegistration;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.servlet.cache.ESIInvalidatorServlet;
import com.ibm.ws.cache.ServerCache;
import com.ibm.ws.cache.intf.ServletCacheUnit;
import com.ibm.ws.cache.servlet.CacheProxyResponseFactory;
import com.ibm.ws.cache.servlet.ESIProcessor;
import com.ibm.ws.cache.servlet.FragmentComposerFactory;
import com.ibm.ws.cache.web.config.ConfigManager;
import com.ibm.ws.exception.RuntimeError;
import com.ibm.ws.exception.RuntimeWarning;
import com.ibm.wsspi.webcontainer.facade.ServletContextFacade;
import com.ibm.wsspi.webcontainer.servlet.IServletContext;
import com.ibm.wsspi.webcontainer.webapp.WebAppConfig;

/**
 * This class holds the Servlet Cache Service to support servlet caching on top of Core Cache Service.
 */
@Component(name = "com.ibm.ws.cache.servlet", service = { ServletContainerInitializer.class, ServletCacheServiceImpl.class }, immediate = true, configurationPolicy = ConfigurationPolicy.IGNORE, property = "service.vendor=IBM")
public class ServletCacheServiceImpl implements ServletContainerInitializer, ServletContextListener {

    private static TraceComponent tc = Tr.register(ServletCacheServiceImpl.class, "WebSphere Dynamic Cache", "com.ibm.ws.cache.resources.dynacache");
    private static ServletCacheUnit servletCacheUnit = new ServletCacheUnitImpl();
    static {
        ServerCache.cacheUnit.setServletCacheUnit(servletCacheUnit);
    }
    final private AtomicReference<ServerCache> serverCacheReference = new AtomicReference<ServerCache>(null);
    final private AtomicReference<ConfigManager> configManagerRef = new AtomicReference<ConfigManager>(null);
    
    public CacheProxyResponseFactory cacheProxyResponseFactory = null;
    public FragmentComposerFactory fragmentComposerFactory = null;


    // --------------------------------------------------------------
    // Start the ServletCacheService - called by the runtime
    // --------------------------------------------------------------
    @Activate
    protected void start() throws RuntimeWarning, RuntimeError {

        final String methodName = "start()";

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, methodName + " coreCacheEnabled=" + ServerCache.coreCacheEnabled);

        try {
            if (ServerCache.coreCacheEnabled) {

                ServerCache.servletCacheEnabled = true;
                servletCacheUnit.createBaseCache();
                instance = this;

                // ----------------------------------------
                // DYNA1055I=DYNA1055I: WebSphere Dynamic Cache (servlet cache) initialized successfully.
                // DYNA1055I.explanation=This message indicates that the WebSphere Dynamic Cache (servlet cache) was
                // successfully initialized.
                // DYNA1055I.useraction=None.
                // ----------------------------------------
                Tr.info(tc, "DYNA1055I");
            }
        } catch (IllegalStateException ex) {
            ServerCache.servletCacheEnabled = false;
            com.ibm.ws.ffdc.FFDCFilter.processException(ex, "com.ibm.ws.cache.ServletCacheServiceImpl.start", "222", this);
            throw new RuntimeError("Servlet Cache Service was not initialized sucessful. Exception: " + ex.getMessage());
        } catch (Throwable ex) {
            ServerCache.servletCacheEnabled = false;
            com.ibm.ws.ffdc.FFDCFilter.processException(ex, "com.ibm.ws.cache.ServletCacheServiceImpl.start", "227", this);
            throw new RuntimeError("Servlet Cache Service was not initialized sucessful. Exception: " + ex.getMessage());
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, methodName + " cacheEnabled=" + ServerCache.servletCacheEnabled);
    }

    // --------------------------------------------------------------
    // Stop the ServletCacheService - called by the runtime
    // --------------------------------------------------------------
    @Deactivate
    protected void stop() {
        final String methodName = "stop()";
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, methodName);

        ESIProcessor[] esiProcessors = ESIProcessor.getRunning();
        for (int i = 0; i < esiProcessors.length; i++) {
            esiProcessors[i].markDead();
        }
        servletCacheUnit.purgeState();
        ServerCache.servletCacheEnabled = false;
        instance = null;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, methodName);
    }
    
    static private ServletCacheServiceImpl instance;

    static public ServletCacheServiceImpl getInstance() {
    	return instance;
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        final String methodName = "contextInitialized";
        if (tc.isEntryEnabled())
            Tr.entry(tc, methodName, new Object[] { sce.getServletContext().getContextPath() });

        String webModuleName = null;
        try {
            String contextRoot = sce.getServletContext().getContextPath();
            if (!contextRoot.startsWith("/"))
                contextRoot = "/" + contextRoot;

            HashMap<String, Object> appContext = new HashMap<String, Object>();
            appContext.put("servlet", contextRoot);
            appContext.put("static", contextRoot);
            appContext.put("webservice", contextRoot);
            appContext.put("portlet", contextRoot);

            // Retrieve all the Servlet Mappings for this webapp
            WebAppConfig wConfig = getWebModuleConfiguration(sce);
            appContext.put("webAppConfiguration", wConfig);
            webModuleName = wConfig.getModuleName();
            getConfigManager().loadConfig(sce.getServletContext(), webModuleName, appContext);

        } catch (Exception ex) {
            com.ibm.ws.ffdc.FFDCFilter.processException(ex, "com.ibm.ws.cache.ServletCacheServiceImpl", "169", this);
        }
        if (tc.isEntryEnabled())
            Tr.exit(tc, methodName, webModuleName);
    }

    private WebAppConfig getWebModuleConfiguration(ServletContextEvent sce) {
        ServletContext context = sce.getServletContext();
        while (context instanceof ServletContextFacade) {
            context = ((ServletContextFacade) context).getIServletContext();
        }
        IServletContext iContext = (IServletContext) context;
        WebAppConfig wConfig = iContext.getWebAppConfig();
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "webAppConfiguration", wConfig);
        }
        return wConfig;
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        final String methodName = "contextDestroyed";
        if (tc.isEntryEnabled())
            Tr.entry(tc, methodName, new Object[] { sce.getServletContext().getContextPath() });
        try {
            ConfigManager configManager = getConfigManager();
            if (null != configManager) {
                configManager.deleteAppNameCacheEntries(getWebModuleConfiguration(sce).getModuleName());
            }
        } catch (Exception ex) {
            com.ibm.ws.ffdc.FFDCFilter.processException(ex, "com.ibm.ws.cache.ServletCacheServiceImpl", "184", this);
        }
        if (tc.isEntryEnabled())
            Tr.exit(tc, methodName);
    }

    @Override
    public void onStartup(java.util.Set<java.lang.Class<?>> c, ServletContext ctx) {

        if (tc.isEntryEnabled())
            Tr.entry(tc, "onStartup");

        if (tc.isDebugEnabled())
            Tr.debug(tc, "created listener to add to servletContext.  listener is: " + this);

        ctx.addListener(this);

        // Register the ESIInvalidatorServlet
        ServletRegistration.Dynamic dynamic = ctx.addServlet("ESIInvalidatorServlet", ESIInvalidatorServlet.class);
        dynamic.addMapping("/esiInvalidator");

        if (tc.isEntryEnabled())
            Tr.exit(tc, "onStartup");
    }

    public ServerCache getServerCache() {
        return serverCacheReference.get();
    }

    @Reference(service = ServerCache.class)
    protected void setServerCache(ServerCache sc) {
        serverCacheReference.set(sc);
    }

    protected void unsetServerCache(ServerCache serverCache) {
        serverCacheReference.compareAndSet(serverCache, null);
    }

    public ConfigManager getConfigManager() {
        return configManagerRef.get();
    }

    @Reference(service = ConfigManager.class)
    protected void setConfigManager(ConfigManager cm) {
        configManagerRef.set(cm);
    }

    protected void unsetConfigManager(ConfigManager configManager) {
        configManagerRef.compareAndSet(configManager, null);
    }
    
    @Reference(cardinality=ReferenceCardinality.MANDATORY, policy=ReferencePolicy.DYNAMIC, policyOption=ReferencePolicyOption.GREEDY)
    protected void setCacheProxyResponseFactory(CacheProxyResponseFactory factory) {
 	   if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, " setCacheProxyResponseFactory: " + factory);
       cacheProxyResponseFactory = factory;
    }
    
    protected void unsetCacheProxyResponseFactory(CacheProxyResponseFactory factory) {
    }
       
    @Reference(cardinality=ReferenceCardinality.MANDATORY, policy=ReferencePolicy.DYNAMIC, policyOption=ReferencePolicyOption.GREEDY)
    protected void setFragmentComposerFactory(FragmentComposerFactory factory) {
     if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, " setFragmentComposerFactory: " + factory);
        fragmentComposerFactory = factory;
    }
    
    protected void unsetFragmentComposerFactory(FragmentComposerFactory factory) {
    }
}
