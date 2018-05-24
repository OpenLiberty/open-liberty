/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.session.store.cache;

import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.cache.Cache;
import javax.cache.Cache.Entry;
import javax.cache.CacheException;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.CompleteConfiguration;
import javax.cache.configuration.OptionalFeature;
import javax.cache.management.CacheMXBean;
import javax.cache.management.CacheStatisticsMXBean;
import javax.cache.spi.CachingProvider;
import javax.management.JMX;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.servlet.ServletContext;
import javax.transaction.UserTransaction;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.serialization.SerializationService;
import com.ibm.ws.session.MemoryStoreHelper;
import com.ibm.ws.session.SessionManagerConfig;
import com.ibm.ws.session.SessionStoreService;
import com.ibm.ws.session.utils.SessionLoader;
import com.ibm.wsspi.library.Library;
import com.ibm.wsspi.logging.Introspector;
import com.ibm.wsspi.session.IStore;

/**
 * Constructs CacheStore instances.
 */
public class CacheStoreService implements Introspector, SessionStoreService {
    
    private static final TraceComponent tc = Tr.register(CacheStoreService.class);
    
    Map<String, Object> configurationProperties;
    private static final String BASE_PREFIX = "properties";
    private static final int BASE_PREFIX_LENGTH = BASE_PREFIX.length();
    private static final int TOTAL_PREFIX_LENGTH = BASE_PREFIX_LENGTH + 3; //3 is the length of .0.

    volatile CacheManager cacheManager; // requires lazy activation
    volatile CachingProvider cachingProvider; // requires lazy activation

    private volatile boolean completedPassivation = true;

    private Library library;

    final AtomicReference<ServiceReference<?>> monitorRef = new AtomicReference<ServiceReference<?>>();

    SerializationService serializationService;

    /**
     * Indicates whether or not the caching provider supports store by reference.
     */
    boolean supportsStoreByReference;

    /**
     * Trace identifier for the cache manager
     */
    volatile String tcCacheManager; // requires lazy activation

    /**
     * Trace identifier for the caching provider.
     */
    private volatile String tcCachingProvider; // requires lazy activation

    volatile UserTransaction userTransaction;

    /**
     * Declarative Services method to activate this component.
     * Best practice: this should be a protected method, not public or private
     *
     * @param context for this component instance
     * @param props service properties
     */
    protected void activate(ComponentContext context, Map<String, Object> props) {
        configurationProperties = new HashMap<String, Object>(props);

        Object scheduleInvalidationFirstHour = configurationProperties.get("scheduleInvalidationFirstHour");
        Object scheduleInvalidationSecondHour = configurationProperties.get("scheduleInvalidationSecondHour");
        Object writeContents = configurationProperties.get("writeContents");
        Object writeFrequency = configurationProperties.get("writeFrequency");

        // httpSessionCache writeContents accepts ONLY_SET_ATTRIBUTES in place of ONLY_UPDATED_ATTRIBUTES to better reflect the behavior provided
        if (writeContents == null || "ONLY_SET_ATTRIBUTES".equals(writeContents))
            configurationProperties.put("writeContents", "ONLY_UPDATED_ATTRIBUTES");

        // default/disallow advanced properties from httpSessionDatabase
        configurationProperties.put("noAffinitySwitchBack", "TIME_BASED_WRITE".equals(writeFrequency));
        configurationProperties.put("onlyCheckInCacheDuringPreInvoke", false);
        configurationProperties.put("optimizeCacheIdIncrements", true);
        configurationProperties.put("scheduleInvalidation", scheduleInvalidationFirstHour != null || scheduleInvalidationSecondHour != null);
        configurationProperties.put("sessionPersistenceMode", "JCACHE");
        configurationProperties.put("useInvalidatedId", false);
        configurationProperties.put("useMultiRowSchema", true);
    }

    /**
     * Performs deferred activation/initialization.
     */
    synchronized void activateLazily() {
        if (cacheManager != null)
            return; // lazy initialization has already completed

        final boolean trace = TraceComponent.isAnyTracingEnabled();
        
        Properties vendorProperties = new Properties();
        
        String uriValue = (String) configurationProperties.get("uri");
        final URI uri;
        if (uriValue != null)
            try {
                uri = new URI(uriValue);
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException(Tr.formatMessage(tc, "INCORRECT_URI_SYNTAX", e), e);
            }
        else
            uri = null;
        
        for (Map.Entry<String, Object> entry : configurationProperties.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            //properties start with properties.0.
            if (key.length () > TOTAL_PREFIX_LENGTH && key.charAt(BASE_PREFIX_LENGTH) == '.' && key.startsWith(BASE_PREFIX)) {
                key = key.substring(TOTAL_PREFIX_LENGTH);
                if (!key.equals("config.referenceType")) 
                    vendorProperties.setProperty(key, (String) value);
            }
        }

        // load JCache provider from configured library, which is either specified as a libraryRef or via a bell
        final ClassLoader cl = library.getClassLoader();

        try {
            AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
                ClassLoader loader = new CachingProviderClassLoader(cl);

                if (trace && tc.isDebugEnabled())
                    CacheHashMap.tcInvoke("Caching", "getCachingProvider", loader);

                cachingProvider = Caching.getCachingProvider(loader);

                tcCachingProvider = "CachingProvider" + Integer.toHexString(System.identityHashCode(cachingProvider));

                if (trace && tc.isDebugEnabled()) {
                    CacheHashMap.tcReturn("Caching", "getCachingProvider", tcCachingProvider, cachingProvider);
                    Tr.debug(this, tc, "caching provider class is " + cachingProvider.getClass().getName());
                    CacheHashMap.tcInvoke(tcCachingProvider, "getCacheManager", uri, null, vendorProperties);
                }

                cacheManager = cachingProvider.getCacheManager(uri, null, vendorProperties);

                return null;
            });

            tcCacheManager = "CacheManager" + Integer.toHexString(System.identityHashCode(cacheManager));

            if (trace && tc.isDebugEnabled()) {
                CacheHashMap.tcReturn(tcCachingProvider, "getCacheManager", tcCacheManager, cacheManager);
                CacheHashMap.tcInvoke(tcCachingProvider, "isSupported", "STORE_BY_REFERENCE");
            }

            supportsStoreByReference = cachingProvider.isSupported(OptionalFeature.STORE_BY_REFERENCE);

            if (trace && tc.isDebugEnabled())
                CacheHashMap.tcReturn(tcCachingProvider, "isSupported", supportsStoreByReference);
        } catch (CacheException x) {
            if (library.getFiles().isEmpty()) {
                Tr.error(tc, "ERROR_CONFIG_EMPTY_LIBRARY", library.id(), Tr.formatMessage(tc, "SESSION_CACHE_CONFIG_MESSAGE", RuntimeUpdateListenerImpl.sampleConfig));
            }
            throw x;
        } catch (Error | RuntimeException x) {
            // deactivate will not be invoked if activate fails, so ensure CachingProvider is closed on error paths
            if (cachingProvider != null) {
                CacheHashMap.tcInvoke(tcCachingProvider, "close");
                cachingProvider.close();
                CacheHashMap.tcReturn(tcCachingProvider, "close");
            }
            throw x;
        }
    }

    /**
     * Configures management and statistics on the specified cache according to enablement by the monitor config element.
     * Precondition: invoking code must run within a doPrivileged block.
     * 
     * @param cacheName name of the cache
     */
    @Trivial // disable autotrace because tracing of the JCache operations will include all of the useful information
    void configureMonitoring(String cacheName) {
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        boolean enable = monitorRef.get() != null;

        if (trace && tc.isDebugEnabled())
            CacheHashMap.tcInvoke(tcCacheManager, "enableManagement", cacheName, enable);
        cacheManager.enableManagement(cacheName, enable);
        if (trace && tc.isDebugEnabled()) {
            CacheHashMap.tcReturn(tcCacheManager, "enableManagement");
            CacheHashMap.tcInvoke(tcCacheManager, "enableStatistics", cacheName, enable);
        }
        cacheManager.enableStatistics(cacheName, enable);
        if (trace && tc.isDebugEnabled())
            CacheHashMap.tcReturn(tcCacheManager, "enableStatistics");
    }

    @Override
    public IStore createStore(SessionManagerConfig smc, String smid, ServletContext sc, MemoryStoreHelper storeHelper, ClassLoader classLoader, boolean applicationSessionStore) {
        IStore store = new CacheStore(smc, smid, sc, storeHelper, applicationSessionStore, this);
        store.setLoader(new SessionLoader(serializationService, classLoader, applicationSessionStore));
        setCompletedPassivation(false);
        return store;
    }

    /**
     * Declarative Services method to deactivate this component.
     * Best practice: this should be a protected method, not public or private
     *
     * @param context for this component instance
     */
    protected void deactivate(ComponentContext context) {
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        // Block the progress of deactivate so that session manager is able to access the cache until it finishes stopping applications.
        // The approach of blocking is copied from DatabaseStoreService as a temporary workaround. It would be nice to have a better solution here.
        final long MAX_WAIT = TimeUnit.SECONDS.toNanos(10);
        for (long start = System.nanoTime(); !completedPassivation && System.nanoTime() - start < MAX_WAIT; )
            try {
                TimeUnit.MILLISECONDS.sleep(100); // sleep 1/10th of a second
            } catch (InterruptedException e) {
            }

        if (cachingProvider != null) {
            if (trace && tc.isDebugEnabled())
                CacheHashMap.tcInvoke(tcCachingProvider, "close");

            AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
                cachingProvider.close();
                return null;
            });

            if (trace && tc.isDebugEnabled())
                CacheHashMap.tcReturn(tcCachingProvider, "close");

            cachingProvider = null;
            cacheManager = null;
        }
    }

    @Override
    public Map<String, Object> getConfiguration() {
        return configurationProperties;
    }

    /**
     * @see com.ibm.wsspi.logging.Introspector#getIntrospectorDescription()
     */
    @Override
    public String getIntrospectorDescription() {
        return "JCache provider diagnostics for HTTP Sessions";
    }

    /**
     * @see com.ibm.wsspi.logging.Introspector#getIntrospectorName()
     */
    @Override
    public String getIntrospectorName() {
        return "SessionCacheIntrospector";
    }

    /**
     * @see com.ibm.wsspi.logging.Introspector#introspect(java.io.PrintWriter)
     */
    @Override
    public void introspect(PrintWriter out) throws Exception {
        final String INDENT = "  ";

        out.print("CachingProvider implementation: ");
        out.println(cachingProvider == null ? null : cachingProvider.getClass().getName());

        out.print("Supports store by reference? ");
        out.println(cachingProvider == null ? null : cachingProvider.isSupported(OptionalFeature.STORE_BY_REFERENCE));

        out.println("Caching provider default properties:");
        if (cachingProvider != null) {
            Properties props = cachingProvider.getDefaultProperties();
            if (props != null)
                props.entrySet().forEach(prop -> out.println(INDENT + prop.getKey() + ": " + prop.getValue()));
        }

        out.print("Caching provider default class loader: ");
        out.println(cachingProvider == null ? null : cachingProvider.getDefaultClassLoader());

        out.println();
        out.print("CacheManager class loader: ");
        out.println(cacheManager == null ? null : cacheManager.getClassLoader());

        out.print("Cache manager URI: ");
        out.println(cacheManager == null ? null : cacheManager.getURI());

        out.print("Cache manager is closed? ");
        out.println(cacheManager == null ? null : cacheManager.isClosed());

        out.println("Cache manager properties:");
        if (cacheManager != null) {
            Properties props = cacheManager.getProperties();
            if (props != null)
                props.entrySet().forEach(prop -> out.println(INDENT + prop.getKey() + ": " + prop.getValue()));
        }

        out.print("Cache manager: ");
        out.println(cacheManager);

        out.println();
        out.println("Cache names:");
        if (cacheManager != null)
            AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
                TreeSet<String> cacheNames = new TreeSet<String>();
                cacheManager.getCacheNames().forEach(cacheNames::add);

                for (String cacheName : cacheNames)
                     out.println(INDENT + cacheName);

                // detailed information per cache
                for (String cacheName : cacheNames) {
                    out.println();
                    boolean isMetaCache = cacheName.startsWith("com.ibm.ws.session.meta.");
                    boolean isAttrCache = cacheName.startsWith("com.ibm.ws.session.attr.");
                    out.println("Cache " + cacheName + ":");
                    Cache<?, ?> cache = isMetaCache ? cacheManager.getCache(cacheName, String.class, ArrayList.class)
                                      : isAttrCache ? cacheManager.getCache(cacheName, String.class, byte[].class)
                                      : cacheManager.getCache(cacheName);
                    if (cache != null) {
                        boolean closed = cache.isClosed();
                        out.println(INDENT + "closed? " + closed);
                        if (!closed) {
                            try {
                                @SuppressWarnings("unchecked")
                                CompleteConfiguration<?, ?> config = cache.getConfiguration(CompleteConfiguration.class);
                                out.println(INDENT + "configuration " + config);

                                MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
                                ObjectName objectName = new ObjectName("javax.cache:type=CacheConfiguration,Cache=" + cacheName + ",*");
                                Set<ObjectName> objectNames = mbs.queryNames(objectName, null); 
                                if (!objectNames.isEmpty()) {
                                    CacheMXBean cacheMXBean = JMX.newMBeanProxy(mbs, objectNames.iterator().next(), CacheMXBean.class);
                                    out.println(INDENT + "is management enabled? " + cacheMXBean.isManagementEnabled());
                                    out.println(INDENT + "is statistics enabled? " + cacheMXBean.isStatisticsEnabled());
                                    out.println(INDENT + "is store by value? " + cacheMXBean.isStoreByValue());
                                    out.println(INDENT + "is read through? " + cacheMXBean.isReadThrough());
                                    out.println(INDENT + "is write through? " + cacheMXBean.isWriteThrough());
                                }

                                objectName = new ObjectName("javax.cache:type=CacheStatistics,Cache=" + cacheName + ",*");
                                objectNames = mbs.queryNames(objectName, null); 
                                if (!objectNames.isEmpty()) {
                                    CacheStatisticsMXBean statsMXBean = JMX.newMBeanProxy(mbs, objectNames.iterator().next(), CacheStatisticsMXBean.class);
                                    out.println(INDENT + "average get time:    " + (statsMXBean.getAverageGetTime() / 1000.0) + "ms");
                                    out.println(INDENT + "average put time:    " + (statsMXBean.getAveragePutTime() / 1000.0) + "ms");
                                    out.println(INDENT + "average remove time: " + (statsMXBean.getAverageRemoveTime() / 1000.0) + "ms");
                                    out.println(INDENT + "cache evictions: " + statsMXBean.getCacheEvictions());
                                    out.println(INDENT + "cache gets:      " + statsMXBean.getCacheGets());
                                    out.println(INDENT + "cache puts:      " + statsMXBean.getCachePuts());
                                    out.println(INDENT + "cache removals:  " + statsMXBean.getCacheRemovals());
                                    out.println(INDENT + "cache hits:      " + statsMXBean.getCacheHits());
                                    out.println(INDENT + "cache misses:    " + statsMXBean.getCacheMisses());
                                    out.println(INDENT + "cache hit percentage:  " + statsMXBean.getCacheHitPercentage() + '%');
                                    out.println(INDENT + "cache miss percentage: " + statsMXBean.getCacheMissPercentage() + '%');
                                }
                            } catch (IllegalArgumentException x) {
                                // Ignore - type not supported by JCache provider
                            } catch (MalformedObjectNameException x) {
                                // Internal error on diagnostics path, allow to continue after auto-logging FFDC
                            }
                            if (isAttrCache) {
                                out.println(INDENT + "First 100 entries:");
                                int i = 0;
                                for (Iterator<?> it = cache.iterator(); i++ < 50 && it.hasNext(); )
                                    try {
                                        Entry<?, ?> entry = (Entry<?, ?>) it.next();
                                        if (entry != null) {
                                            // deserialization of the value might require the application's class loader, which is not available during introspection
                                            byte[] bytes = (byte[]) entry.getValue();
                                            out.println(INDENT + INDENT + "session attribute " + entry.getKey() + ": " + (bytes == null ? null : ("byte[" + bytes.length + "]")));
                                        }
                                    } catch (NoSuchElementException x) {
                                        // ignore - some JCache providers might raise this instead of returning null when modified during iterator
                                    }
                            } else if (isMetaCache) {
                                out.println(INDENT + "First 50 entries:");
                                int i = 0;
                                for (Iterator<?> it = cache.iterator(); i++ < 50 && it.hasNext(); )
                                    try {
                                        Entry<?, ?> entry = (Entry<?, ?>) it.next();
                                        if (entry != null) {
                                            out.println(INDENT + INDENT + "session " + entry.getKey() + ": " + new SessionInfo((ArrayList<?>) entry.getValue()));
                                        }
                                    } catch (NoSuchElementException x) {
                                        // ignore - some JCache providers might raise this instead of returning null when modified during iterator
                                    }
                            }
                        }
                    }
                }

                return null;
            });
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public void setCompletedPassivation(boolean isInProcessOfStopping) {
        completedPassivation = isInProcessOfStopping;
    }

    protected void setLibrary(Library library) {
        this.library = library;
    }

    protected void setMonitor(ServiceReference<?> ref) {
        monitorRef.set(ref);
        if (cacheManager != null)
            AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
                for (String cacheName : cacheManager.getCacheNames())
                    configureMonitoring(cacheName);
                return null;
            });
    }

    protected void setSerializationService(SerializationService serializationService) {
        this.serializationService = serializationService;
    }

    protected void setUserTransaction(UserTransaction userTransaction) {
        this.userTransaction = userTransaction;
    }

    protected void unsetLibrary(Library library) {
        this.library = null;
    }

    protected void unsetMonitor(ServiceReference<?> ref) {
        if (monitorRef.compareAndSet(ref, null) && cacheManager != null)
            AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
                for (String cacheName : cacheManager.getCacheNames())
                    configureMonitoring(cacheName);
                return null;
            });
    }

    protected void unsetSerializationService(SerializationService serializationService) {
        this.serializationService = null;
    }

    protected void unsetUserTransaction(UserTransaction userTransaction) {
        this.userTransaction = null;
    }
}
