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

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.cache.CacheManager;
import javax.cache.spi.CachingProvider;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

import io.openliberty.checkpoint.spi.CheckpointPhase;
import io.openliberty.jcache.CacheManagerService;
import io.openliberty.jcache.utils.CacheConfigUtil;

/**
 * Service that configures a {@link CacheManager} to use with JCache caching.
 */
@Component(immediate = true, configurationPolicy = REQUIRE, configurationPid = "io.openliberty.jcache.cachemanager",
           property = { "service.vendor=IBM" })
public class CacheManagerServiceImpl implements CacheManagerService {
    private static final TraceComponent tc = Tr.register(CacheManagerServiceImpl.class);

    private static final String KEY_URI = "uri";
    private static final String KEY_ID = "id";

    private static final String BASE_PREFIX = "properties";
    private static final int BASE_PREFIX_LENGTH = BASE_PREFIX.length();
    private static final int TOTAL_PREFIX_LENGTH = BASE_PREFIX_LENGTH + 3; // 3 is the length of .0.

    private Properties properties = null;
    private String uriValue = null;
    private CacheManager cacheManager = null;

    private CachingProviderService cachingProviderService = null;
    private ScheduledExecutorService scheduledExecutorService = null;
    private ScheduledFuture<?> getCacheManagerFuture = null;

    private Object syncObject = new Object();
    private String id = null;

    private CacheConfigUtil cacheConfigUtil = null;

    /** An object that the CachingProviderService, CacheManagerService and CacheService should sync on before closing. */
    private Object closeSyncObject = null;

    /**
     * Activate this OSGi component.
     *
     * @param config The configuration to use when activating this component.
     */
    @Activate
    public void activate(Map<String, Object> config) {
        id = (String) config.get(KEY_ID);

        /*
         * Get the URI.
         */
        this.uriValue = (String) config.get(KEY_URI);

        /*
         * Get the configured vendor properties.
         */
        this.properties = new Properties();
        for (Map.Entry<String, Object> entry : config.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            /*
             * properties start with properties.0.
             */
            if (key.length() > TOTAL_PREFIX_LENGTH && key.charAt(BASE_PREFIX_LENGTH) == '.'
                && key.startsWith(BASE_PREFIX)) {
                key = key.substring(TOTAL_PREFIX_LENGTH);
                if (!key.equals("config.referenceType"))
                    this.properties.setProperty(key, (String) value);
            }
        }

        /*
         * Schedule a task to initialize the CacheManager in the background. This will
         * alleviate delays on the first request to any caches that use this
         * CacheManager.
         */
        CheckpointPhase.onRestore(() -> {
            getCacheManagerFuture = scheduledExecutorService.schedule(new Runnable() {
                @Override
                public void run() {
                    getCacheManager();
                }
            }, 0, TimeUnit.SECONDS);
        });
    }

    /**
     * Deactivate this OSGi component.
     */
    @Deactivate
    @FFDCIgnore({ Exception.class })
    public void deactivate() {
        /*
         * Close and clear the CacheManager instance.
         */
        if (cacheManager != null && !cacheManager.isClosed()) {
            try {
                synchronized (closeSyncObject) {
                    if (!cacheManager.isClosed()) {
                        cacheManager.close();
                    }
                }
            } catch (Exception e) {
                Tr.warning(tc, "CWLJC0013_CLOSE_CACHEMGR_ERR", ((id == null) ? "" : id), e);
            }
        }

        /*
         * Null out any instance fields.
         */
        cacheManager = null;
        getCacheManagerFuture = null;
        closeSyncObject = null;
    }

    @Override
    public CacheManager getCacheManager() {
        if (cacheManager == null) {
            synchronized (syncObject) {
                if (cacheManager == null) {
                    try {

                        /*
                         * Configuration updates can occur while this task is either queue to run or while running.
                         * If this occurs, the CachingProviderService could be unregistered. Make sure it is still
                         * registered, if not, no-op this task.
                         */
                        if (cachingProviderService == null) {
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                Tr.debug(tc, "It appears that perhaps the CachingProviderService was unset after this task was started." +
                                             " Perhaps a configuration change was processed?");
                            }
                            return null;
                        }
                        CachingProvider cachingProvider = cachingProviderService.getCachingProvider();

                        /*
                         * Perform some custom configuration updates for the CacheManager.
                         */
                        cacheConfigUtil = new CacheConfigUtil();
                        URI uri = cacheConfigUtil.preConfigureCacheManager(uriValue, cachingProvider, properties);

                        /*
                         * Get the CacheManager instance. We don't provide the ClassLoader to the
                         * getCacheManager call b/c it should use the default ClassLoader for the
                         * provider.
                         *
                         * When the ClassLoader was provided here, its caused differences in behavior in
                         * comparison to how the legacy HttpSessionCache implementation worked. It
                         * didn't really effect the product so much, as the cache wasn't scoped under
                         * the default ClassLoader and some of the HttpSessionCache test servlets that
                         * used JCache to verify values in the cache couldn't find the cache.
                         *
                         * In the future, if we need to provide the ClassLoader to the getCacheManager
                         * method, call replace null with
                         * cachingProviderService.getUnifiedClassLoader() and update the tests that
                         * will now fail. Note: This might also cause issues running mixed levels since
                         * the cache scope will now be different based on the new ClassLoader.
                         */
                        long loadTimeMs = System.currentTimeMillis();
                        cacheManager = cachingProvider.getCacheManager(uri, null, properties);
                        loadTimeMs = System.currentTimeMillis() - loadTimeMs;

                        if (TraceComponent.isAnyTracingEnabled() && tc.isInfoEnabled()) {
                            Tr.info(tc, "CWLJC0005_MANAGER_LOADED", id, loadTimeMs);
                        }
                    } catch (NullPointerException e) {
                        /*
                         * Catch incorrectly formatted httpSessionCache uri values from server.xml file
                         */
                        throw new IllegalArgumentException(Tr.formatMessage(tc, "CWLJC0007_URI_INVALID_SYNTAX", e), e);
                    } catch (IOException e) {
                        throw new IllegalArgumentException(Tr.formatMessage(tc, "CWLJC0010_MANAGER_CONFIG_ERR", e), e);
                    }
                }
            }
        }

        return cacheManager;
    }

    @Override
    public CachingProviderService getCachingProviderService() {
        return cachingProviderService;
    }

    /**
     * Set the {@link CachingProviderService} for this {@link CacheManagerService}.
     *
     * @param service The {@link CachingProviderService}.
     */
    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    public void setCachingProviderService(CachingProviderService service) {
        this.cachingProviderService = service;
        this.closeSyncObject = service.getCloseSyncObject();
    }

    /**
     * Unset the {@link CachingProviderService} for this {@link CacheManagerService}.
     *
     * @param service The {@link CachingProviderService}.
     */
    public void unsetCachingProviderService(CachingProviderService service) {
        /*
         * Wait for the getCacheManagerFuture to complete if in progress.
         */
        waitForBackgroundTask();

        /*
         * Close the CacheManager.
         */
        if (this.cacheManager != null) {
            this.cacheManager.close();
        }

        /*
         * Null out any of the instance fields derived from the CachingProviderService.
         */
        this.getCacheManagerFuture = null;
        this.cachingProviderService = null;
        this.cacheManager = null;
    }

    /**
     * Set the {@link ScheduledExecutorService} for this {@link CacheManagerService}.
     *
     * @param scheduledExecutorService The {@link ScheduledExecutorService}.
     */
    @Reference(name = "scheduledExecutorService", service = ScheduledExecutorService.class, target = "(deferrable=false)")
    public void setScheduledExecutorService(ScheduledExecutorService scheduledExecutorService) {
        this.scheduledExecutorService = scheduledExecutorService;
    }

    /**
     * Unset the {@link ScheduledExecutorService} for this {@link CacheManagerService}.
     *
     * @param scheduledExecutorService The {@link ScheduledExecutorService}.
     */
    public void unsetScheduledExecutorService(ServiceReference<ScheduledExecutorService> scheduledExecutorService) {
        this.scheduledExecutorService = null;
    }

    @Override
    public String toString() {
        return super.toString() + "{id=" + id + ", uriValue=" + uriValue + ", cacheManager=" + cacheManager + "}";
    }

    /**
     * Wait for the {@link #getCacheManagerFuture} task to finish.
     */
    private void waitForBackgroundTask() {
        if (this.getCacheManagerFuture != null && !this.getCacheManagerFuture.isDone()) {
            boolean shouldTrace = TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled();
            try {
                if (shouldTrace) {
                    Tr.debug(tc, "Started waiting for background task to finish.");
                }
                this.getCacheManagerFuture.get(60, TimeUnit.SECONDS);
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

    /**
     * Get the synch object to be used when closing the CachingProvider, CacheManager or the Cache itself.
     *
     * @return The sync object.
     */
    Object getCloseSyncObject() {
        return closeSyncObject;
    }
}
