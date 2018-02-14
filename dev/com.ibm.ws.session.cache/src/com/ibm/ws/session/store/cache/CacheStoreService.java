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

import java.util.ArrayList;
import java.util.Map;

import javax.cache.Cache;
import javax.cache.CacheException;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.Configuration;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.EternalExpiryPolicy;
import javax.cache.spi.CachingProvider;
import javax.servlet.ServletContext;
import javax.transaction.UserTransaction;

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
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
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
@Component(name = "com.ibm.ws.session.cache", configurationPolicy = ConfigurationPolicy.OPTIONAL, service = { SessionStoreService.class })
public class CacheStoreService implements SessionStoreService {
    
    private static final TraceComponent tc = Tr.register(CacheStoreService.class);
    
    private Map<String, Object> configurationProperties;

    /**
     * For single-cache path, the whole session is store as an entry in this cache.
     * For multi-cache path, separate caches are created per application to store the session properties each as their own cache entry,
     * and this cache only contains information about the session rather than its contents.
     */
    @SuppressWarnings("rawtypes")
    Cache<String, ArrayList> cache;

    CacheManager cacheManager;

    private volatile boolean completedPassivation = true;

    @Reference(policyOption = ReferencePolicyOption.GREEDY, target = "(id=unbound)")
    protected Library library;

    @Reference
    protected SerializationService serializationService;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
    protected volatile UserTransaction userTransaction;

    /**
     * Declarative Services method to activate this component.
     * Best practice: this should be a protected method, not public or private
     *
     * @param context for this component instance
     * @param props service properties
     */
    @Activate
    @FFDCIgnore(CacheException.class)
    protected void activate(ComponentContext context, Map<String, Object> props) {
        configurationProperties = props;

        // Use different cache names depending on whether session property values are all stored in a single entry
        // within this main cache vs as separate entries in other caches.
        // The use of different cache names prevents servers from colliding on the same cache when they have different
        // options selected for how to store session properties.
        String cacheName = "true".equals(configurationProperties.get("useMultiRowSchema"))
                                        ? "com.ibm.ws.session.info"   // no session property values are kept in this cache
                                        : "com.ibm.ws.session.cache"; // all session property values are stored in a single entry per session

        // load JCache provider from configured library, which is either specified as a libraryRef or via a bell
        CachingProvider provider = Caching.getCachingProvider(library.getClassLoader());
        cacheManager = provider.getCacheManager(null, null, null);
        cache = cacheManager.getCache(cacheName, String.class, ArrayList.class);
        if (cache == null) {
            @SuppressWarnings("rawtypes")
            Configuration<String, ArrayList> config = new MutableConfiguration<String, ArrayList>().setTypes(String.class, ArrayList.class);
            try {
                cache = cacheManager.createCache(cacheName, config);

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "Created a new session info cache", cache);
            } catch (CacheException x) {
                cache = cacheManager.getCache(cacheName, String.class, ArrayList.class);
                if (cache == null)
                    throw x;
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "Using session info cache", cache);
    }

    @Override
    public IStore createStore(SessionManagerConfig smc, String smid, ServletContext sc, MemoryStoreHelper storeHelper, ClassLoader classLoader, boolean applicationSessionStore) {
        if ("true".equals(configurationProperties.get("useMultiRowSchema")))
            smc.setUsingMultirow(true); // TODO temporary code for experimenting with cache entry per session property
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
    @Deactivate
    protected void deactivate(ComponentContext context) {
        cacheManager.close();
    }

    /**
     * Obtains the session cache for the specified application.
     * For multi-cache path, each session property is a separate entry in this cache.
     * 
     * @param appName the application name.
     * @return the cache.
     */
    Cache<String, byte[]> getCache(String appName) {
        // TODO replace / and : characters (per spec for cache names) and ensure the name is still unique.
        String cacheName = "com.ibm.ws.session.app." + appName;

        // Because byte[] does instance-based .equals, it will not be possible to use Cache.replace operations, but we are okay with that.
        Cache<String, byte[]> cache = cacheManager.getCache(cacheName, String.class, byte[].class);
        if (cache == null) {
            Configuration<String, byte[]> config = new MutableConfiguration<String, byte[]>()
                            .setTypes(String.class, byte[].class)
                            .setExpiryPolicyFactory(EternalExpiryPolicy.factoryOf());
            try {
                cache = cacheManager.createCache(cacheName, config);

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "Created a new session property cache");
            } catch (CacheException x) {
                cache = cacheManager.getCache(cacheName, String.class, byte[].class);
                if (cache == null)
                    throw x;
            }
        }
        return cache;
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
}
