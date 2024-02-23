/*******************************************************************************
 * Copyright (c) 2021, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.jcache.internal;

import static org.osgi.service.component.annotations.ConfigurationPolicy.REQUIRE;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

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
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.serialization.SerializationService;

import io.openliberty.checkpoint.spi.CheckpointPhase;
import io.openliberty.jcache.CacheManagerService;
import io.openliberty.jcache.CacheService;
import io.openliberty.jcache.DeserializationException;
import io.openliberty.jcache.SerializationException;

/**
 * Service that configures an individual {@link Cache} instance for JCache
 * caching.
 */
@Component(immediate = true, configurationPolicy = REQUIRE, configurationPid = "io.openliberty.jcache.cache",
           property = { "service.vendor=IBM" })
public class CacheServiceImpl implements CacheService {

    private static final TraceComponent tc = Tr.register(CacheServiceImpl.class);
    private static final String KEY_CACHE_NAME = "name";
    private static final String KEY_ID = "id";

    private CacheManagerService cacheManagerService = null;
    private SerializationService serializationService = null;
    private ScheduledExecutorService scheduledExecutorService = null;
    private ScheduledFuture<?> getCacheFuture = null;

    private String cacheName = null;
    private Cache<Object, Object> cache = null;
    private Object syncObject = new Object();
    private String id;

    /** Collection of classes that have had error messages emitted for NotSerializableExceptions. */
    private static final Set<String> NOTSERIALIZABLE_CLASSES_LOGGED = new HashSet<String>();

    /** An object that the CachingProviderService, CacheManagerService and CacheService should sync on before closing. */
    private Object closeSyncObject = null;

    @Activate
    public void activate(Map<String, Object> configProps) {
        /*
         * Retrieve the id and cache name from the configuration properties.
         */
        this.id = (String) configProps.get(KEY_ID);
        this.cacheName = (String) configProps.get(KEY_CACHE_NAME);

        /*
         * Schedule a task to initialize the cache in the background. This will
         * alleviate delays on the first request to the cache.
         */
        CheckpointPhase.onRestore(() -> {
            getCacheFuture = scheduledExecutorService.schedule(new Runnable() {
                @Override
                public void run() {
                    getCache();
                }
            }, 0, TimeUnit.SECONDS);
        });
    }

    @Deactivate
    @FFDCIgnore({ Exception.class })
    public void deactivate() {
        /*
         * Close the cache.
         */
        if (cache != null && !cache.isClosed()) {
            try {
                synchronized (closeSyncObject) {
                    if (!cache.isClosed()) {
                        cache.close();
                    }
                }
            } catch (Exception e) {
                Tr.warning(tc, "CWLJC0012_CLOSE_CACHE_ERR", cacheName, e);
            }
        }

        /*
         * Null out instance fields.
         */
        cache = null;
        getCacheFuture = null;
        closeSyncObject = null;
        NOTSERIALIZABLE_CLASSES_LOGGED.clear();
    }

    @Override
    @Sensitive
    public Object deserialize(@Sensitive byte[] bytes) {
        if (bytes == null)
            return null;

        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            ObjectInputStream ois = serializationService.createObjectInputStream(bais,
                                                                                 cacheManagerService.getCachingProviderService().getUnifiedClassLoader());
            return ois.readObject();
        } catch (ClassNotFoundException | IOException e) {
            String msg = Tr.formatMessage(tc, "CWLJC0008_DESERIALIZE_ERR", cacheName, e);
            throw new DeserializationException(msg, e);
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
            throwSerializationException(e);
            return null; // Not reachable.
        }
    }

    @Override
    public Cache<Object, Object> getCache() {
        if (this.cache == null) {
            /*
             * We will need to get the cache from the provider at this point.
             */
            synchronized (syncObject) {
                /*
                 * Possibly set while we were waiting on the sync block.
                 */
                if (this.cache == null) {

                    this.cache = AccessController.doPrivileged((PrivilegedAction<Cache<Object, Object>>) () -> {

                        Cache<Object, Object> tCache = null;

                        /*
                         * Configuration updates can occur while this task is either queued to run or while running.
                         * If this occurs, the CachingProviderService could be unregistered. Make sure it is still
                         * registered, if not, no-op this task.
                         */
                        if (cacheManagerService == null) {
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                Tr.debug(tc, "It appears that perhaps the CacheManagerService was stopped after this task was started." +
                                             " Perhaps a configuration change was processed?");
                            }
                            return null;
                        }

                        /*
                         * Search for an existing cache.
                         */
                        CacheManager cacheManager = null;
                        long loadTimeMs = 0l;
                        try {
                            cacheManager = cacheManagerService.getCacheManager();

                            /*
                             * Configuration updates can occur while this task is either queued to run or while running.
                             * If this occurs, the CachingProviderService could have been unregistered from the
                             * CacheManagerService causing the CacheManager to be null here. Make sure it is still
                             * registered, if not, no-op this task.
                             */
                            if (cacheManager == null) {
                                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                    Tr.debug(tc, "It appears that the CacheManagerService was unable to get a CacheManager instance." +
                                                 " Perhaps a configuration change was processed?");
                                }
                                return null;
                            }

                            /*
                             * The JCache specification says that any cache created outside of the JCache
                             * APIs should have no types for the key or value. Some providers seem to respect
                             * that and others don't. If we provide the types in the getCache call, we can
                             * expect some providers to throw an exception, so don't.
                             */
                            loadTimeMs = System.currentTimeMillis();
                            Cache<Object, Object> jCache = cacheManager.getCache(cacheName);
                            loadTimeMs = System.currentTimeMillis() - loadTimeMs;

                            if (jCache != null) {
                                tCache = new CacheProxy(jCache, this);
                            }
                        } catch (Throwable e) {
                            /*
                             * If we failed and couldn't retrieve an existing cache, log an error and try to
                             * create one.
                             */
                            Tr.warning(tc, "CWLJC0011_GET_CACHE_ERR", cacheName, e);
                        }

                        /*
                         * Did we find a cache? If not, create it.
                         */
                        if (tCache == null) {
                            /*
                             * Update the cache configuration.
                             */
                            MutableConfiguration<Object, Object> config = new MutableConfiguration<Object, Object>();
                            config.setTypes(Object.class, Object.class);

                            /*
                             * Finally, create the JCache instance.
                             */
                            loadTimeMs = System.currentTimeMillis();
                            tCache = new CacheProxy(cacheManager.createCache(cacheName, config), this);
                            loadTimeMs = System.currentTimeMillis() - loadTimeMs;

                            if (TraceComponent.isAnyTracingEnabled() && tc.isInfoEnabled()) {
                                Tr.info(tc, "CWLJC0001_CACHE_CREATED", cacheName, loadTimeMs,
                                        cacheManagerService.getCachingProviderService().getCachingProvider().getClass().getName());
                            }
                        } else {
                            if (TraceComponent.isAnyTracingEnabled() && tc.isInfoEnabled()) {
                                Tr.info(tc, "CWLJC0002_CACHE_FOUND", cacheName, loadTimeMs,
                                        cacheManagerService.getCachingProviderService().getCachingProvider().getClass().getName());
                            }
                        }

                        return tCache;
                    });
                }
            }
        }
        return this.cache;
    }

    @Override
    public String getCacheName() {
        return cacheName;
    }

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    public void setCacheManagerService(CacheManagerService service) {
        this.cacheManagerService = service;
        this.closeSyncObject = ((CacheManagerServiceImpl) service).getCloseSyncObject();
    }

    public void unsetCacheManagerService(CacheManagerService service) {
        /*
         * Wait for the getCacheFuture to complete if in progress.
         */
        waitForBackgroundTask();

        /*
         * Close the cache.
         */
        if (this.cache != null) {
            cache.close();
        }

        /*
         * Null out any instance fields derived from the CacheManager service.
         */
        this.cache = null;
        this.getCacheFuture = null;
        this.cacheManagerService = null;
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
        return super.toString() + "{id=" + id + ", cacheName=" + cacheName + "}";
    }

    /**
     * This method throws a {@link SerializationException} always. It will print out trace when applicable.
     *
     * @param cause The cause of the Exception.
     */
    private void throwSerializationException(Exception cause) {
        String msg = Tr.formatMessage(tc, "CWLJC0009_SERIALIZE_ERR", cacheName, cause);

        /*
         * If there is a java.io.NotSerializableException, let's only print out a message once for each class.
         */
        if (cause instanceof NotSerializableException) {
            String className = cause.getMessage(); // The message is the class name.
            if (!NOTSERIALIZABLE_CLASSES_LOGGED.contains(className)) {
                NOTSERIALIZABLE_CLASSES_LOGGED.add(className);
                if (TraceComponent.isAnyTracingEnabled() && tc.isErrorEnabled()) {
                    Tr.error(tc, msg);
                }
            } else if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                /*
                 * If debug is enabled, emit the message even if we emitted the error message for
                 * this class already.
                 */
                Tr.debug(tc, msg);
            }
        }
        throw new SerializationException(msg, cause);
    }

    /**
     * Wait for the {@link #getCacheFuture} task to finish.
     */
    private void waitForBackgroundTask() {
        if (this.getCacheFuture != null && !this.getCacheFuture.isDone()) {
            boolean shouldTrace = TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled();
            try {
                if (shouldTrace) {
                    Tr.debug(tc, "Started waiting for background task to finish.");
                }
                this.getCacheFuture.get(60, TimeUnit.SECONDS);
                if (shouldTrace) {
                    Tr.debug(tc, "Finished waiting for background task to finish.");
                }
            } catch (Exception e) {
                if (shouldTrace) {
                    Tr.debug(tc, "Caught the following exception while waiting for background task to finish: " + e);
                }
            }
        }
    }
}
