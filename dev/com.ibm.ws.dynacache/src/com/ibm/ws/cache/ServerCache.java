/*******************************************************************************
 * Copyright (c) 1997, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cache;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.osgi.service.component.ComponentContext;
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
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.cache.intf.CommandCache;
import com.ibm.ws.cache.intf.DCache;
import com.ibm.ws.cache.intf.JSPCache;
import com.ibm.ws.cache.stat.CachePerfFactory;
import com.ibm.wsspi.cache.CacheFeatureSupport;
import com.ibm.wsspi.cache.CacheProvider;
import com.ibm.wsspi.cache.CoreCache;

/**
 * This class creates and holds all(servlet and/or object) cache instances created.
 */

@Component(service = ServerCache.class, immediate = true, configurationPolicy = ConfigurationPolicy.IGNORE, property = "service.vendor=IBM")
public class ServerCache {

    private static TraceComponent tc = Tr.register(ServerCache.class, "WebSphere Dynamic Cache", "com.ibm.ws.cache.resources.dynacache");

    public static volatile boolean coreCacheEnabled = false;
    public static volatile boolean servletCacheEnabled = false;
    public static volatile boolean objectCacheEnabled = false;

    public static CacheUnit cacheUnit = new CacheUnitImpl(new CacheConfig());
    public static DCache cache = null;
    public static CommandCache commandCache = null;
    public static JSPCache jspCache = null;
    private static int sharingPolicy = EntryInfo.NOT_SHARED;
    private static int pushFrequency = 0;

    private static CacheService cacheService = null;

    private final static AtomicReference<CachePerfFactory> cachePerfFactoryRef = new AtomicReference<CachePerfFactory>();

    private static Map<String, DCache> cacheInstances = new ConcurrentHashMap<String, DCache>();

    public static int getSharingPolicy() {
        return sharingPolicy;
    }

    public static void setSharingPolicy(int i) {
        sharingPolicy = i;
    }

    public static int getPushFrequency() {
        return pushFrequency;
    }

    public static void setPushFrequency(int i) {
        pushFrequency = i;
    }

    @Trivial
    public static Map getCacheInstances() {
        return cacheInstances;
    }

    /**
     * Declarative Services method to activate this component. Best practice: this should be a protected method, not
     * public or private
     * 
     * @param context
     *            context for this component
     */
    @Activate
    protected void activate(ComponentContext context) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
            Tr.event(tc, "activate", context);
    }

    /**
     * Declarative Services method to deactivate this component. Best practice: this should be a protected method, not
     * public or private
     * 
     * @param context
     *            context for this component
     */
    @Deactivate
    protected void deactivate(ComponentContext context) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
            Tr.event(tc, "deactivate", context);
    }

    /**
     * Get a named Jsp cache.
     * 
     * @param cacheName
     *            The name of the cache instance or null for base cache.
     * @return The cache object or null if the named cache has not been defined.
     */
    public static JSPCache getJspCache(String cacheName) {
        final String methodName = "getJspCache()";
        JSPCache cacheOut = null;
        if (servletCacheEnabled == false) {
            // DYNA1059W=DYNA1059W: WebSphere Dynamic Cache instance named {0} cannot be used because of Dynamic Servlet
            // cache service has not be started.
            Tr.error(tc, "DYNA1059W", new Object[] { cacheName });
        } else {
            if (cacheName != null) {
                try {
                    cacheOut = cacheUnit.getJSPCache(cacheName);
                } catch (Exception e) {
                    // DYNA1003E=DYNA1003E: WebSphere Dynamic Cache instance named {0} can not be initialized because of
                    // error {1}.
                    Tr.error(tc, "DYNA1003E", new Object[] { cacheName, e });
                }
            } else {
                cacheOut = ServerCache.jspCache;
            }
        }
        if (cacheOut == null && TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, methodName + " could not find JSPCache for cacheName=" + cacheName);
        }
        return cacheOut;
    }

    /**
     * Get a named Command cache.
     * 
     * @param cacheName
     *            The name of the cache instance or null for base cache.
     * @return The cache object or null if the named cache has not been defined.
     */
    public static CommandCache getCommandCache(String cacheName) {
        final String methodName = "getCommandCache()";
        CommandCache cacheOut = null;
        if (servletCacheEnabled == false) {
            // DYNA1059W=DYNA1059W: WebSphere Dynamic Cache instance named {0} cannot be used because of Dynamic Servlet
            // cache service has not be started.
            Tr.error(tc, "DYNA1059W", new Object[] { cacheName });
        } else {
            if (cacheName != null) {
                try {
                    cacheOut = cacheUnit.getCommandCache(cacheName);
                } catch (Exception e) {
                    // DYNA1003E=DYNA1003E: WebSphere Dynamic Cache instance named {0} can not be initialized because of
                    // error {1}.
                    Tr.error(tc, "DYNA1003E", new Object[] { cacheName, e });
                }
            } else {
                cacheOut = ServerCache.commandCache;
            }
        }
        if (cacheOut == null && TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, methodName + " could not find CommandCache for cacheName=" + cacheName);
        }
        return cacheOut;
    }

    /**
     * Get a named cache. Caller must check for null return.
     * 
     * @param cacheName
     *            The name of the cache instance or null for base cache.
     * @return The cache object or null if the named cache has not yet been created.
     */
    @Trivial
    public static DCache getCache(String cacheName) {

        final String methodName = "getCache()";
        DCache cacheOut = ServerCache.cache;

        if (cacheName != null) {
            cacheName = normalizeCacheName(cacheName, null);
        }

        if (cacheName != null && !cacheName.equalsIgnoreCase(DCacheBase.DEFAULT_CACHE_NAME)) {
            cacheOut = cacheInstances.get(cacheName);
        }

        if (cacheOut == null && TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, methodName + " could not find cache for cacheName=" + cacheName);
        }

        return cacheOut;
    }

    /**
     * Get a configured named cache.
     * 
     * @param cache
     *            name
     * @return The cache object.
     */
    public static DCache getConfiguredCache(String cacheName) {
        final String methodName = "getConfiguredCache()";
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, methodName + " input cacheName=" + cacheName);
        }
        DCache cacheOut = null;
        if (coreCacheEnabled == false) {
            // DYNA1003E=DYNA1003E: WebSphere Dynamic Cache instance named {0} can not be initialized because of error
            // {1}.
            Tr.error(tc, "DYNA1003E", new Object[] { cacheName, "Core Cache Service has not been started." });
        } else {
            cacheOut = getCache(cacheName);
            if (cacheOut == null) {

                CacheConfig config = getCacheService().getCacheInstanceConfig(cacheName);
                if (config == null) {
                    // DYNA1004E=DYNA1004E: WebSphere Dynamic Cache instance named {0} can not be initialized because it
                    // is not configured.
                    // DYNA1004E.explanation=This message indicates the named WebSphere Dynamic Cache instance can not
                    // be initialized. The named instance is not avaliable.
                    // DYNA1004E.useraction=Use the WebSphere Administrative Console to configure a cache instance
                    // resource named {0}.
                    Tr.error(tc, "DYNA1004E", new Object[] { cacheName });
                    cacheOut = null;
                } else {
                    cacheOut = createCache(config.cacheName, config);
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, methodName + " output cacheName=" + (cacheOut != null ? cacheOut.getCacheName() : "null"));
        }
        return cacheOut;
    }

    public synchronized static DCache createCache(String cacheName, CacheConfig cacheConfig) {
        final String methodName = "createCache()";

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, methodName + " cacheName=" + cacheName + " config=" + cacheConfig + " enableReplication="
                         + cacheConfig.enableCacheReplication + " provider: " + cacheConfig.cacheProviderName);
        }

        String tempCacheName = normalizeCacheName(cacheName, cacheConfig);
        cacheConfig.determineCacheProvider();

        DCache cacheOut = null;
        String errorString = "";
        boolean error = false;

        if (coreCacheEnabled == false) {
            // DYNA1003E=DYNA1003E: WebSphere Dynamic Cache instance named {0} can not be initialized because of error
            // {1}.
            Tr.error(tc, "DYNA1003E", new Object[] { cacheName, "Core Cache Service has not been started." });

        } else {

            cacheOut = getCache(tempCacheName);

            if (cacheOut == null) {
                String cacheProviderName = CacheConfig.CACHE_PROVIDER_DYNACACHE;
                CacheProvider cacheProvider = null;
                if (!cacheConfig.isDefaultCacheProvider()) {
                    cacheProviderName = cacheConfig.cacheProviderName;
                    cacheProvider = CacheProviderLoaderImpl.getInstance().getCacheProvider(cacheConfig.cacheProviderName);
                    if (cacheProvider != null) {
                        // when firing event listeners we have to use cachename non prefixed
                        cacheConfig.cacheName = tempCacheName;
                        CoreCache coreCache = cacheProvider.createCache(cacheConfig);
                        CacheFeatureSupport featureSupport = cacheProvider.getCacheFeatureSupport();
                        if (coreCache == null || featureSupport == null) {
                            error = true;
                            errorString = "ENGLISH ONLY MESSAGE: coreCache == null || featureSupport == null....Check FFDC logs for Exceptions";
                        } else {
                            cacheUnit.startServices(false); // start all services except TimeLimitDaemon
                            cacheOut = new CacheProviderWrapper(cacheConfig, featureSupport, coreCache);
                            cacheInstances.put(tempCacheName, cacheOut);
                            coreCache.start();
                            if (tempCacheName.equals(DCacheBase.DEFAULT_CACHE_NAME)) {
                                ServerCache.cache = cacheOut;
                            }
                            cacheConfig.cache = cacheOut;
                            cacheConfig.defaultProvider = false;
                            // DYNA1001I=DYNA1001I: WebSphere Dynamic Cache instance named {0} initialized successfully.
                            Tr.info(tc, "DYNA1001I", new Object[] { tempCacheName });
                            // DYNA1071I=DYNA1071I: The cache provider \"{0}\" is being used.
                            Tr.info(tc, "DYNA1071I", new Object[] { cacheProviderName });
                        }
                    } else {
                        error = true;
                        errorString = "ENGLISH ONLY MESSAGE: cacheProvider is null. Check for the cache provider libraries ";
                    }
                }

                if (error) {
                    // DYNA1066E=DYNA1066E: Unable to initialize the cache provider \"{0}\". The Dynamic cache will be
                    // used as default cache provider to create cache instance \"{1}\".
                    Tr.error(tc, "DYNA1066E", new Object[] { cacheConfig.cacheProviderName, cacheConfig.cacheName });
                    Tr.error(tc, errorString);
                    cacheConfig.resetProvider(tempCacheName);
                }

                if (cacheOut == null) {
                    cacheProviderName = CacheConfig.CACHE_PROVIDER_DYNACACHE;

                    // start services if it is not started yet.
                    cacheUnit.startServices(true); // start all services including the time limit daemon
                    cacheOut = new Cache(tempCacheName, cacheConfig);
                    cacheConfig.cache = cacheOut;
                    cacheOut.setBatchUpdateDaemon(cacheUnit.getBatchUpdateDaemon());

                    CachePerfFactory factory = cachePerfFactoryRef.get();
                    cacheOut.setCachePerf(factory);

                    NotificationService ns = null;
                    RemoteServices rs = cacheUnit.getRemoteService();
                    rs.setNotificationService(new NullNotificationService());

                    cacheOut.setRemoteServices(rs);
                    cacheOut.setTimeLimitDaemon(cacheUnit.getTimeLimitDaemon());
                    cacheOut.setInvalidationAuditDaemon(cacheUnit.getInvalidationAuditDaemon());

                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(tc, "Cache settings are: " + " cacheSize=" + cacheConfig.cacheSize + " cachePriority=" + cacheConfig.cachePriority);

                    if (cacheConfig.enableCacheReplication && ns != null) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            Tr.debug(tc, "Cache Replication is enabled: " + " replicationDomain=" + cacheConfig.replicationDomain
                                         + " replicationType=" + cacheConfig.replicationType + " defaultShareType=" + cacheConfig.defaultShareType);
                        ns.setCacheStatisticsListener(cacheOut.getCacheStatisticsListener());
                    } else {
                        Tr.debug(tc, "Cache Replication is not enabled");
                    }
                    cacheInstances.put(tempCacheName, cacheOut);
                    cacheOut.start();
                    if (tempCacheName.equals(DCacheBase.DEFAULT_CACHE_NAME)) {
                        ServerCache.cache = cacheOut;
                    }

                    // DYNA1001I=DYNA1001I: WebSphere Dynamic Cache instance named {0} initialized successfully.
                    Tr.info(tc, "DYNA1001I", new Object[] { tempCacheName });
                    // DYNA1071I=DYNA1071I: The cache provider \"{0}\" is being used.
                    Tr.info(tc, "DYNA1071I", new Object[] { cacheProviderName });
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, methodName + " normalized name=" + tempCacheName + " cache=" + cacheOut);
        }
        return cacheOut;
    }

    /**
     * Since the cache instances are stored with key as cacheName, it has to change default cache JNDI name to internal
     * cache name.
     */
    @Trivial
    public static String normalizeCacheName(String cacheName, CacheConfig cacheConfig) {

        String tempCacheName = cacheName;

        if (cacheName.equalsIgnoreCase(DCacheBase.DEFAULT_BASE_JNDI_NAME)) {
            tempCacheName = DCacheBase.DEFAULT_CACHE_NAME;

        } else if (cacheName.equalsIgnoreCase(DCacheBase.DEFAULT_DMAP_JNDI_NAME)) {
            tempCacheName = DCacheBase.DEFAULT_DISTRIBUTED_MAP_NAME;
        }

        // Make sure the input cacheName matched with cacheConfig's cacheName.
        // If not match, set to the input cacheName for cacheConfig
        if (null != cacheConfig && !tempCacheName.equals(cacheConfig.cacheName)) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "normalized cacheName=" + cacheName + " does not match with cacheConfig cacheName=" + cacheConfig.cacheName);
            }
            cacheConfig.cacheName = tempCacheName;
        }

        return tempCacheName;
    }

    // For CacheMonitor
    public static ArrayList getServletCacheInstanceNames() {
        ArrayList list = null;
        if (ServerCache.servletCacheEnabled == false) {
            list = new ArrayList();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "CacheMonitor: Servlet Cache Service has not been started.");

        } else {
            list = getCacheService().getServletCacheInstanceNames();
        }
        return list;
    }

    // ---------------------------------------------------------
    // For Perf Advisor
    // ---------------------------------------------------------
    public static CacheInstanceInfo[] getCacheInstanceInfo() {
        CacheInstanceInfo[] info = null;
        if (coreCacheEnabled == false) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Core Cache Service has not been started.");
        } else {
            info = getCacheService().getCacheInstanceInfo();
        }
        return info;
    }

    public static int getActiveCacheInstanceCount() {
        return cacheInstances.size();
    }

    public static ArrayList getObjectCacheInstanceNames() {
        ArrayList list = null;
        CacheService cs = getCacheService();
        if (cs != null) {
            list = cs.getObjectCacheInstanceNames();
        }
        return list;
    }

    // Use this dependency to control service ordering, but get real CacheService reference earlier via static method below
    @Reference(service = CacheService.class, cardinality = ReferenceCardinality.MANDATORY, policy = ReferencePolicy.STATIC, target = "(id=baseCache)")
    protected void setCacheService(CacheService cs) {}

    public static void setCacheServiceEarly(CacheService cs) {
        cacheService = cs;
    }

    protected static void unsetCacheService(CacheService cs) {
        cacheService = null;
    }

    public final static CacheService getCacheService() {
        if (cacheService == null) {
            // Error trying to get cache service before it has activated
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "*** Cache Service has not been started - null being returned (may cause NPEs).");
        }
        return cacheService;
    }

    @Reference(service = CachePerfFactory.class, cardinality = ReferenceCardinality.OPTIONAL, policyOption = ReferencePolicyOption.GREEDY, policy = ReferencePolicy.DYNAMIC)
    protected void setCachePerfFactory(CachePerfFactory perfFactory) {
        cachePerfFactoryRef.set(perfFactory);
        for (DCache dcache : cacheInstances.values()) {
            dcache.setCachePerf(perfFactory);
        }
    }

    protected void unsetCachePerfFactory(CachePerfFactory perfFactory) {
        cachePerfFactoryRef.set(null);
        for (DCache dcache : cacheInstances.values()) {
            dcache.setCachePerf(null);
        }
    }
}