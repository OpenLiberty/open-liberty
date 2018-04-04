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

import java.net.URI;
import java.net.URISyntaxException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.OptionalFeature;
import javax.cache.spi.CachingProvider;
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
import com.ibm.wsspi.session.IStore;

/**
 * Constructs CacheStore instances.
 */
public class CacheStoreService implements SessionStoreService {
    
    private static final TraceComponent tc = Tr.register(CacheStoreService.class);
    
    Map<String, Object> configurationProperties;
    private static final String BASE_PREFIX = "properties";
    private static final int BASE_PREFIX_LENGTH = BASE_PREFIX.length();
    private static final int TOTAL_PREFIX_LENGTH = BASE_PREFIX_LENGTH + 3; //3 is the length of .0.

    CacheManager cacheManager;
    CachingProvider cachingProvider;

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
    String tcCacheManager;

    /**
     * Trace identifier for the caching provider.
     */
    private String tcCachingProvider;

    volatile UserTransaction userTransaction;

    /**
     * Declarative Services method to activate this component.
     * Best practice: this should be a protected method, not public or private
     *
     * @param context for this component instance
     * @param props service properties
     */
    protected void activate(ComponentContext context, Map<String, Object> props) {
        final boolean trace = TraceComponent.isAnyTracingEnabled();

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
        // TODO decide whether or not to externalize useInvalidatedId
        configurationProperties.put("useMultiRowSchema", true);
        
        Properties vendorProperties = new Properties();
        
        String uriValue = (String) props.get("uri");
        URI uri = null;
        if(uriValue != null)
            try {
                uri = new URI(uriValue);
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException(Tr.formatMessage(tc, "INCORRECT_URI_SYNTAX", e), e);
            }
        
        for (Map.Entry<String, Object> entry : props.entrySet()) {
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

        ClassLoader loader = AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
            @Trivial
            public ClassLoader run() {
                return new CachingProviderClassLoader(cl);
            }
        });

        if (trace && tc.isDebugEnabled())
            CacheHashMap.tcInvoke("Caching", "getCachingProvider", loader);

        cachingProvider = Caching.getCachingProvider(loader);

        try {
            tcCachingProvider = "CachingProvider" + Integer.toHexString(System.identityHashCode(cachingProvider));

            if (trace && tc.isDebugEnabled()) {
                CacheHashMap.tcReturn("Caching", "getCachingProvider", tcCachingProvider, cachingProvider);
                Tr.debug(this, tc, "caching provider class is " + cachingProvider.getClass().getName());
                CacheHashMap.tcInvoke(tcCachingProvider, "getCacheManager", uri, null, vendorProperties);
            }

            cacheManager = cachingProvider.getCacheManager(uri, null, vendorProperties);

            tcCacheManager = "CacheManager" + Integer.toHexString(System.identityHashCode(cacheManager));

            if (trace && tc.isDebugEnabled()) {
                CacheHashMap.tcReturn(tcCachingProvider, "getCacheManager", tcCacheManager, cacheManager);
                CacheHashMap.tcInvoke(tcCachingProvider, "isSupported", "STORE_BY_REFERENCE");
            }

            supportsStoreByReference = cachingProvider.isSupported(OptionalFeature.STORE_BY_REFERENCE);

            if (trace && tc.isDebugEnabled())
                CacheHashMap.tcReturn(tcCachingProvider, "isSupported", supportsStoreByReference);
        } catch (Error | RuntimeException x) {
            // deactivate will not be invoked if activate fails, so ensure CachingProvider is closed on error paths
            CacheHashMap.tcInvoke(tcCachingProvider, "close");
            cachingProvider.close();
            CacheHashMap.tcReturn(tcCachingProvider, "close");
            throw x;
        }
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

        if (trace && tc.isDebugEnabled())
            CacheHashMap.tcInvoke(tcCachingProvider, "close");

        cachingProvider.close();

        if (trace && tc.isDebugEnabled())
            CacheHashMap.tcReturn(tcCachingProvider, "close");
    }

    @Override
    public Map<String, Object> getConfiguration() {
        return configurationProperties;
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
        if (cacheManager != null) {
            final boolean trace = TraceComponent.isAnyTracingEnabled();
            for (String cacheName : cacheManager.getCacheNames()) {
                if (trace && tc.isDebugEnabled())
                    CacheHashMap.tcInvoke(tcCacheManager, "enableManagement", cacheName, true);
                cacheManager.enableManagement(cacheName, true);
                if (trace && tc.isDebugEnabled()) {
                    CacheHashMap.tcReturn(tcCacheManager, "enableManagement");
                    CacheHashMap.tcInvoke(tcCacheManager, "enableStatistics", cacheName, true);
                }
                cacheManager.enableStatistics(cacheName, true);
                if (trace && tc.isDebugEnabled())
                    CacheHashMap.tcReturn(tcCacheManager, "enableStatistics");
            }
        }
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
        if (monitorRef.compareAndSet(ref, null) && cacheManager != null) {
            final boolean trace = TraceComponent.isAnyTracingEnabled();
            for (String cacheName : cacheManager.getCacheNames()) {
                if (trace && tc.isDebugEnabled())
                    CacheHashMap.tcInvoke(tcCacheManager, "enableManagement", cacheName, false);
                cacheManager.enableManagement(cacheName, false);
                if (trace && tc.isDebugEnabled()) {
                    CacheHashMap.tcReturn(tcCacheManager, "enableManagement");
                    CacheHashMap.tcInvoke(tcCacheManager, "enableStatistics", cacheName, false);
                }
                cacheManager.enableStatistics(cacheName, false);
                if (trace && tc.isDebugEnabled())
                    CacheHashMap.tcReturn(tcCacheManager, "enableStatistics");
            }
        }
    }

    protected void unsetSerializationService(SerializationService serializationService) {
        this.serializationService = null;
    }

    protected void unsetUserTransaction(UserTransaction userTransaction) {
        this.userTransaction = null;
    }
}
