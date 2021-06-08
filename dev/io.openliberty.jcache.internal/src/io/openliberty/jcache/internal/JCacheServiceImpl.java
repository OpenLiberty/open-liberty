/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.jcache.internal;

import static org.osgi.service.component.annotations.ConfigurationPolicy.REQUIRE;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.configuration.MutableConfiguration;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.kernel.productinfo.ProductInfo;
import com.ibm.ws.serialization.SerializationService;

import io.openliberty.jcache.JCacheManagerService;
import io.openliberty.jcache.JCacheService;

/**
 * Service that configures an individual {@link Cache} instance for JCache
 * caching.
 */
@Component(immediate = true, configurationPolicy = REQUIRE, configurationPid = "io.openliberty.jcache.cache",
           property = { "service.vendor=IBM" })
public class JCacheServiceImpl implements JCacheService {

    private static final TraceComponent tc = Tr.register(JCacheServiceImpl.class);
    private static final String KEY_CACHE_NAME = "name";
    private static final String KEY_ID = "id";

    private JCacheManagerService jCacheManagerService = null;
    private SerializationService serializationService = null;
    private ScheduledExecutorService scheduledExecutorService = null;

    private String cacheName = null;
    private Cache<Object, Object> cache = null;
    private Object syncObject = new Object();
    private String id;

    /** Flag tells us if the message for a call to a beta method has been issued */
    private static boolean issuedBetaMessage = false;

    @Activate
    public void activate(Map<String, Object> configProps) {
        /*
         * Don't run if not in beta.
         */
        betaFenceCheck();

        /*
         * Retrieve the id and cache name from the configuration properties.
         */
        this.id = (String) configProps.get(KEY_ID);
        this.cacheName = (String) configProps.get(KEY_CACHE_NAME);

        /*
         * Schedule a task to initialize the cache in the background. This will
         * alleviate delays on the first request to the cache.
         */
        scheduledExecutorService.execute(new Runnable() {
            @Override
            public void run() {
                getCache();
            }
        });
    }

    @Deactivate
    public void deactivate() {
        if (cache != null) {
            cache.close();
        }
        cache = null;
    }

    /**
     * Prevent beta functionality from being used when not running the beta edition.
     *
     * @throws UnsupportedOperationException if we are not running the beta edition.
     */
    private void betaFenceCheck() throws UnsupportedOperationException {
        /*
         * Not running beta edition, throw exception
         */
        if (!ProductInfo.getBetaEdition()) {
            throw new UnsupportedOperationException("The jCache feature is beta and is not available.");
        } else {
            /*
             * Running beta exception, issue message if we haven't already issued one for
             * this class.
             */
            if (!issuedBetaMessage) {
                Tr.info(tc, "BETA: A beta method has been invoked for the class " + this.getClass().getName()
                            + " for the first time.");
                issuedBetaMessage = !issuedBetaMessage;
            }
        }
    }

    @Override
    @Sensitive
    public Object deserialize(@Sensitive byte[] bytes) {
        if (bytes == null)
            return null;

        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            ObjectInputStream ois = serializationService.createObjectInputStream(bais,
                                                                                 jCacheManagerService.getJCachingProviderService().getUnifiedClassLoader());
            return ois.readObject();
        } catch (ClassNotFoundException e) {
            Tr.error(tc, "CWLJC0008_DESERIALIZE_ERR", cacheName, e);
            return null;
        } catch (IOException e) {
            Tr.error(tc, "CWLJC0008_DESERIALIZE_ERR", cacheName, e);
            return null;
        }
    }

    @Override
    @Sensitive
    public byte[] serialize(@Sensitive Object o) {
        if (o == null)
            return null;

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = serializationService.createObjectOutputStream(baos);
            oos.writeObject(o);
            return baos.toByteArray();
        } catch (IOException e) {
            Tr.error(tc, "CWLJC0009_SERIALIZE_ERR", cacheName, e);
            return null;
        }
    }

    @Override
    public Cache<Object, Object> getCache() {
        if (this.cache != null) {
            return this.cache;
        }

        return AccessController.doPrivileged((PrivilegedAction<Cache<Object, Object>>) () -> {

            /*
             * We will need to get the cache from the provider at this point.
             */
            synchronized (syncObject) {
                if (cache == null) {
                    long loadTimeMs;

                    /*
                     * Search for an existing cache.
                     */
                    try {
                        CacheManager cacheManager = jCacheManagerService.getCacheManager();
                        loadTimeMs = System.currentTimeMillis();
                        Cache<Object, Object> jCache = cacheManager.getCache(cacheName, Object.class, Object.class);
                        loadTimeMs = System.currentTimeMillis() - loadTimeMs;

                        if (jCache != null) {
                            cache = new JCacheProxy(jCache, this);
                        }
                    } catch (Throwable e) {
                        // Have seen classcastexception if hazelcast key / value types don't match in the configuration.
                        Tr.error(tc, "CWLJC0011_GET_CACHE_ERR", cacheName, e);
                        throw e;
                    }

                    /*
                     * Did we find a cache? If not, create it.
                     */
                    if (cache == null) {
                        /*
                         * Update the cache configuration.
                         */
                        MutableConfiguration<Object, Object> config = new MutableConfiguration<Object, Object>();
                        config.setTypes(Object.class, Object.class);

                        /*
                         * Finally, create the JCache instance.
                         */
                        CacheManager cacheManager = jCacheManagerService.getCacheManager();
                        loadTimeMs = System.currentTimeMillis();
                        cache = new JCacheProxy(cacheManager.createCache(cacheName, config), this);
                        loadTimeMs = System.currentTimeMillis() - loadTimeMs;

                        if (TraceComponent.isAnyTracingEnabled() && tc.isInfoEnabled()) {
                            Tr.info(tc, "CWLJC0001_CACHE_CREATED", cacheName, loadTimeMs);
                        }
                    } else {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isInfoEnabled()) {
                            Tr.info(tc, "CWLJC0002_CACHE_FOUND", cacheName, loadTimeMs);
                        }
                    }

                    /*
                     * Output trace to mark the caching provider class in use for this cache.
                     */
                    if (TraceComponent.isAnyTracingEnabled() && tc.isInfoEnabled()) {
                        Tr.info(tc, "CWLJC0003_USING_PROVIDER", cacheName, jCacheManagerService.getJCachingProviderService()
                                        .getCachingProvider()
                                        .getClass()
                                        .getName());
                    }
                }

                return this.cache;
            }
        });
    }

    @Override
    public String getCacheName() {
        return cacheName;
    }

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    public void setJCachingProviderService(JCacheManagerService service) {
        this.jCacheManagerService = service;
    }

    public void unsetJCachingProviderService(JCacheManagerService service) {
        this.jCacheManagerService = null;
    }

    @Reference(name = "scheduledExecutorService", service = ScheduledExecutorService.class, target = "(deferrable=false)")
    public void setScheduledExecutorService(ScheduledExecutorService scheduledExecutorService) {
        this.scheduledExecutorService = scheduledExecutorService;
    }

    public void unsetScheduledExecutorService(ServiceReference<ScheduledExecutorService> scheduledExecutorService) {
        this.scheduledExecutorService = null;
    }

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    public void setSerializationService(SerializationService serializationService) {
        this.serializationService = serializationService;
    }

    public void unsetSerializationService(SerializationService serializationService) {
        this.serializationService = null;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{id=" + id + ", cacheName=" + cacheName + "}";
    }
}
