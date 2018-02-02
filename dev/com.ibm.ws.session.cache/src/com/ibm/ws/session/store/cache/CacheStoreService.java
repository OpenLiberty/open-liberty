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

import java.util.Map;

import javax.cache.Cache;
import javax.cache.CacheException;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.CompleteConfiguration;
import javax.cache.configuration.Configuration;
import javax.cache.configuration.MutableConfiguration;
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

import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.serialization.SerializationService;
import com.ibm.ws.session.MemoryStoreHelper;
import com.ibm.ws.session.SessionManagerConfig;
import com.ibm.ws.session.SessionStoreService;
import com.ibm.ws.session.store.cache.serializable.SessionData;
import com.ibm.ws.session.store.cache.serializable.SessionKey;
import com.ibm.ws.session.utils.SessionLoader;
import com.ibm.wsspi.library.Library;
import com.ibm.wsspi.session.IStore;

/**
 * Constructs CacheStore instances.
 */
@Component(name = "com.ibm.ws.session.cache", configurationPolicy = ConfigurationPolicy.OPTIONAL, service = { SessionStoreService.class })
public class CacheStoreService implements SessionStoreService {
    private Map<String, Object> configurationProperties;

    Cache<SessionKey, SessionData> cache;
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

        // load JCache provider from configured library, which is either specified as a libraryRef or via a bell
        CachingProvider provider = Caching.getCachingProvider(library.getClassLoader());
        cacheManager = provider.getCacheManager(null, new CacheClassLoader(), null); // TODO When class loader is specified, it isn't being used for deserialization. Why?
        cache = cacheManager.getCache("com.ibm.ws.session.cache");
        if (cache == null) {
            Configuration<SessionKey, SessionData> config = new MutableConfiguration<SessionKey, SessionData>().setTypes(SessionKey.class, SessionData.class);
            try {
                cache = cacheManager.createCache("com.ibm.ws.session.cache", config);
            } catch (CacheException x) {
                cache = cacheManager.getCache("com.ibm.ws.session.cache");
                if (cache == null)
                    throw x;
            }
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
    @Deactivate
    protected void deactivate(ComponentContext context) {
        cacheManager.close();
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
