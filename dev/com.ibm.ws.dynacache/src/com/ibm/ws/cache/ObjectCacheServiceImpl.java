/*******************************************************************************
 * Copyright (c) 1997, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cache;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicReference;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import com.ibm.websphere.cache.DistributedObjectCache;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.cache.intf.DCache;
import com.ibm.ws.cache.intf.ObjectCacheUnit;
import com.ibm.ws.exception.RuntimeError;
import com.ibm.ws.exception.RuntimeWarning;
import com.ibm.wsspi.cache.DistributedObjectCacheFactory;
import com.ibm.wsspi.kernel.service.location.VariableRegistry;

/**
 * This class holds the Object Cache Service to support object caching on top of Core Cache Service.
 */

//@Component(service = ServletContainerInitializer.class, immediate = true, configurationPolicy = ConfigurationPolicy.IGNORE, property = "service.vendor=IBM")
public class ObjectCacheServiceImpl implements ServletContextListener, ServletContainerInitializer {

    private static TraceComponent tc = Tr.register(ObjectCacheServiceImpl.class, "WebSphere Dynamic Cache", "com.ibm.ws.cache.resources.dynacache");
    private static ObjectCacheUnit objectCacheUnit = new ObjectCacheUnitImpl();
    static {
        ServerCache.cacheUnit.setObjectCacheUnit(objectCacheUnit);
    }

    private final AtomicReference<ServerCache> serverCacheReference = new AtomicReference<ServerCache>(null);
    private final AtomicReference<VariableRegistry> variableRegistryRef = new AtomicReference<VariableRegistry>(null);

    public static final String CACHE_INSTANCES_PROPERTIES = "cacheinstances.properties";
    public static final String DISTRIBUTED_MAP_PROPERTIES = "distributedmap.properties";
    public static final String WEB_INF_CACHE_INSTANCES_PROPERTIES = "WEB-INF/cacheinstances.properties";
    public static final String WEB_INF_DISTRIBUTED_MAP_PROPERTIES = "WEB-INF/distributedmap.properties";

    private final Set<URL> processedUrls = new CopyOnWriteArraySet<URL>();

    // --------------------------------------------------------------
    // Start the ObjectCacheService - called by the runtime
    // --------------------------------------------------------------
//    @Activate
    protected void start() throws RuntimeWarning, RuntimeError {
        final String methodName = "start()";

        CacheService cacheService = getCacheService();
        if (null == cacheService)
            throw new RuntimeError("Core Cache Service is NOT available");
        CacheConfig cacheConfigTemplate = cacheService.getCacheConfig();

        try {
            // ----------------------------------------------
            // Wrap passed config object for our use - Cache.DEFAULT_DISTRIBUTED_MAP_NAME ( default )
            // ----------------------------------------------
            ServerCache.objectCacheEnabled = true;

            // ----------------------------------------
            // load CacheConfig from all distributedmap.properties and cacheinstances.properties files
            // ----------------------------------------
            initializeConfigPropertiesFiles(getClass().getClassLoader(), DISTRIBUTED_MAP_PROPERTIES);
            initializeConfigPropertiesFiles(getClass().getClassLoader(), CACHE_INSTANCES_PROPERTIES);

            // ----------------------------------------
            // Create object cache instance if createCacheAtServerStartup in the CacheConfig is true
            // ----------------------------------------
            ArrayList instanceList = cacheService.getObjectCacheInstanceNames();
            Iterator it = instanceList.iterator();
            while (it.hasNext()) {
                String instanceName = (String) it.next();
                CacheConfig cacheConfig = cacheService.getCacheInstanceConfig(instanceName);
                if (cacheConfig != null && cacheConfig.isCreateCacheAtServerStartup()) {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "Create object cache instance \"" + instanceName + "\" during startup because the cache is configured to.");
                    }
                    ServerCache.createCache(cacheConfig.getCacheName(), cacheConfig);
                }
            }

            // ----------------------------------------
            // DYNA1056I=DYNA1056I: WebSphere Dynamic Cache (object cache) initialized successfully.
            // DYNA1056I.explanation=This message indicates that the WebSphere Dynamic Cache (object cache) was
            // successfully initialized.
            // DYNA1056I.useraction=None.
            // ----------------------------------------
            Tr.info(tc, "DYNA1056I");

        } catch (IllegalStateException ex) {
            com.ibm.ws.ffdc.FFDCFilter.processException(ex, "com.ibm.ws.cache.ObjectCacheServiceImpl.start", "232", this);
            Tr.error(tc, "dynacache.cacheInitFailed", new Object[] { cacheConfigTemplate.getServerServerName(), ex.getMessage() });
            throw new RuntimeError("Object Cache Service was not initialized sucessful. Exception: " + ex.getMessage());
        } catch (Throwable ex) {
            com.ibm.ws.ffdc.FFDCFilter.processException(ex, "com.ibm.ws.cache.ObjectCacheServiceImpl.start", "237", this);
            Tr.error(tc, "dynacache.cacheInitFailed", new Object[] { cacheConfigTemplate.getServerServerName(), ex.getMessage() });
            throw new RuntimeError("Object Cache Service was not initialized sucessful. Exception: " + ex.getMessage());
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, methodName + " objectCacheEnabled=" + ServerCache.objectCacheEnabled);
    }

    // --------------------------------------------------------------
    // Stop the ObjectCacheService - called by the runtime
    // --------------------------------------------------------------
//    @Deactivate
    protected void stop() {

        final String methodName = "stop()";
        if (tc.isEntryEnabled())
            Tr.entry(tc, methodName);

        ServerCache.objectCacheEnabled = false;
        CacheService cacheService = getCacheService();
        if (null == cacheService)
            return;
        unInitializeConfigPropertiesFiles(getClass().getClassLoader(), DISTRIBUTED_MAP_PROPERTIES);
        unInitializeConfigPropertiesFiles(getClass().getClassLoader(), CACHE_INSTANCES_PROPERTIES);

        Collection<DistributedObjectCache> distributedMaps = DistributedObjectCacheFactory.distributedMaps.values();
        for (DistributedObjectCache distributedObjectCache : distributedMaps) {
            DCache cache = ((DistributedObjectCacheAdapter) distributedObjectCache).getCache();
            ServerCache.getCacheInstances().remove(cache);
            cache.stop();
        }
        DistributedObjectCacheFactory.distributedMaps.clear();

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

        if (tc.isEntryEnabled())
            Tr.exit(tc, "onStartup");
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        final String methodName = "contextInitialized";
        if (tc.isEntryEnabled())
            Tr.entry(tc, methodName, new Object[] { sce.getServletContext().getContextPath() });
        try {
            CacheService cacheService = getCacheService();
            if (null == cacheService)
                return;
            initializeConfigPropertiesFiles(sce.getServletContext().getClassLoader(), DISTRIBUTED_MAP_PROPERTIES);
            initializeConfigPropertiesFiles(sce.getServletContext().getClassLoader(), CACHE_INSTANCES_PROPERTIES);
            initializeConfigPropertiesFiles(sce.getServletContext().getClassLoader(), WEB_INF_DISTRIBUTED_MAP_PROPERTIES);
            initializeConfigPropertiesFiles(sce.getServletContext().getClassLoader(), WEB_INF_CACHE_INSTANCES_PROPERTIES);
        } catch (Exception ex) {
            com.ibm.ws.ffdc.FFDCFilter.processException(ex, "com.ibm.ws.cache.ObjectCacheServiceImpl.contextInitialized(ServletContextEvent)", "169",
                                                        this);
        }
        if (tc.isEntryEnabled())
            Tr.exit(tc, methodName);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        final String methodName = "contextDestroyed";
        if (tc.isEntryEnabled())
            Tr.entry(tc, methodName, new Object[] { sce.getServletContext().getContextPath() });
        try {
            CacheService cacheService = getCacheService();
            if (null == cacheService)
                return;
            unInitializeConfigPropertiesFiles(sce.getServletContext().getClassLoader(), DISTRIBUTED_MAP_PROPERTIES);
            unInitializeConfigPropertiesFiles(sce.getServletContext().getClassLoader(), CACHE_INSTANCES_PROPERTIES);
            unInitializeConfigPropertiesFiles(sce.getServletContext().getClassLoader(), WEB_INF_DISTRIBUTED_MAP_PROPERTIES);
            unInitializeConfigPropertiesFiles(sce.getServletContext().getClassLoader(), WEB_INF_CACHE_INSTANCES_PROPERTIES);

        } catch (Exception ex) {
            com.ibm.ws.ffdc.FFDCFilter.processException(ex, "com.ibm.ws.cache.ObjectCacheServiceImpl.contextDestroyed(ServletContextEvent)", "184",
                                                        this);
        }
        if (tc.isEntryEnabled())
            Tr.exit(tc, methodName);
    }

//    @Reference(service = ServerCache.class)
    protected void setServerCache(ServerCache serverCache) {
        serverCacheReference.set(serverCache);
    }

    protected void unsetServerCache(ServerCache serverCache) {
        serverCacheReference.compareAndSet(serverCache, null);
    }

    protected CacheService getCacheService() {
        ServerCache sc = serverCacheReference.get();
        if (sc != null) {
            return ServerCache.getCacheService();
        }
        return null;
    }

//    @Reference(service = VariableRegistry.class)
    protected void setVariableRegistry(VariableRegistry vr) {
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "setVariableRegistry ", vr);
        }
        variableRegistryRef.set(vr);
    }

    protected void unsetVariableRegistry(VariableRegistry vr) {
        variableRegistryRef.compareAndSet(vr, null);
    }

    // --------------------------------------------------------------
    // Called during start() and stateChanged().
    // Reads all the config properties files and
    // creates and binds the maps.
    // --------------------------------------------------------------
    private void initializeConfigPropertiesFiles(ClassLoader cl, String fileName) {
        final String methodName = "initializeConfigPropertiesFiles()";

        if (tc.isEntryEnabled())
            Tr.entry(tc, methodName + " classLoader=" + cl + " fileName=" + fileName);
        try {
            Enumeration<URL> resources = cl.getResources(fileName);
            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                if (!processedUrls.contains(url))
                    processConfigUrl(url);
            }
        } catch (Exception ex) {
        }
        if (tc.isEntryEnabled())
            Tr.exit(tc, methodName);
    }

    private void unInitializeConfigPropertiesFiles(ClassLoader classLoader, String fileName) {
        try {
            Enumeration<URL> resources = classLoader.getResources(fileName);
            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                processedUrls.remove(url);
            }
        } catch (Exception ex) {
        }
    }

    // --------------------------------------------------------------
    // Create cache instances and associated DistributedObjectCache wrapper from a properties file.
    // --------------------------------------------------------------
    private void processConfigUrl(URL url) throws Exception {
        final String methodName = "processConfigUrl()";
        if (tc.isEntryEnabled())
            Tr.entry(tc, methodName + " configUrl=" + url);

        processedUrls.add(url);
        Properties props = new Properties();
        InputStream is = url.openStream();
        try {
            props.load(is);
        } finally {
            is.close();
        }

        int i = 0;
        VariableRegistry vr = variableRegistryRef.get();
        boolean found = false;
        do {
            // Look for instance name ( JNDI name == cacheName )
            String instancekey = "cache.instance." + i;
            String instanceName = props.getProperty(instancekey);

            // Remove leading forward slash
            if (instanceName != null && instanceName.charAt(0) == '/')
                instanceName = instanceName.substring(1);

            // Do we have an instance name?
            if (instanceName != null) {
                instanceName = instanceName.trim();
                if (tc.isDebugEnabled())
                    Tr.debug(tc, methodName + " Adding new cache instance. instanceName=" + instanceName);
                found = true;

                // Build a vMap converted properties object
                Iterator<Object> it = props.keySet().iterator();
                Properties ccProps = new Properties();
                ccProps.put(CacheConfig.CACHE_NAME, instanceName);
                while (it.hasNext()) {
                    String s = (String) it.next();
                    if (s.startsWith(instancekey + ".")) {
                        String value = vr.resolveString(props.getProperty(s)).trim();
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, methodName + " Adding property: " + s + " value:" + value);
                        ccProps.put("com.ibm.ws.cache.CacheConfig" + s.substring(instancekey.length()), value);
                        // for external cache provider access
                        String unmodifiedKey = s.substring(instancekey.length() + 1);
                        if (tc.isDebugEnabled()) {
                            Tr.debug(tc, methodName + " Adding property: " + unmodifiedKey + " value:" + value);
                        }
                        ccProps.put(unmodifiedKey, value);
                    }
                }
                if (tc.isDebugEnabled())
                    Tr.debug(tc, methodName + " Final props=" + ccProps);

                // ------------------------------------------------------------
                // Bind to JNDI if the cache & the config do not exist
                // ------------------------------------------------------------
                DCache c = ServerCache.getCache(instanceName);

                CacheService cs = getCacheService();
                CacheConfig cacheConfig = cs.getCacheInstanceConfig(instanceName);
                if (c == null && cacheConfig == null) {
                    cacheConfig = cs.addCacheInstanceConfig(ccProps);
                    if (cacheConfig.isCreateCacheAtServerStartup()) {
                        if (tc.isDebugEnabled()) {
                            Tr.debug(tc, "Create cache instance \"" + instanceName + "\" during startup because the cache is configured to.");
                        }
                        ServerCache.createCache(cacheConfig.getCacheName(), cacheConfig);
                    }
                } else {
                    // In this case, the instanceName already exists, so output
                    // a
                    // warning message for the user
                    // DYNA1057W=DYNA1057W: Cache instance \"{0}\" defined in
                    // the \"{1}\" is not added because a cache with this name
                    // already exists.
                    // DYNA1057W.explanation=The cache instance is not added
                    // because a cache with this name already exists.
                    // DYNA1057W.useraction=Ensure that the cache instance does
                    // not define more than once in the properties file.
                    Tr.warning(tc, "DYNA1057W", new Object[] { instanceName, url.toString() });
                }
            } else {
                found = false;
            }
            i++;
        } while (found);

        if (tc.isEntryEnabled())
            Tr.exit(tc, methodName + " configUrl=" + url);
    }
}