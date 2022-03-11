/*******************************************************************************
 * Copyright (c) 2021, 2022 IBM Corporation and others.
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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ScheduledExecutorService;

import javax.cache.CacheManager;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.kernel.productinfo.ProductInfo;

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
    private URI configuredUri = null;
    private CacheManager cacheManager = null;

    private CachingProviderService cachingProviderService = null;
    private ScheduledExecutorService scheduledExecutorService = null;

    private Object syncObject = new Object();
    private String id = null;

    private CacheConfigUtil cacheConfigUtil = null;

    /** Flag tells us if the message for a call to a beta method has been issued. */
    private static boolean issuedBetaMessage = false;

    /**
     * Activate this OSGi component.
     *
     * @param config The configuration to use when activating this component.
     */
    @Activate
    public void activate(Map<String, Object> config) {
        /*
         * Don't run if not in beta.
         */
        betaFenceCheck();

        id = (String) config.get(KEY_ID);

        /*
         * Get the URI.
         */
        String uriValue = (String) config.get(KEY_URI);
        if (uriValue != null)
            try {
                this.configuredUri = new URI(uriValue);
            } catch (URISyntaxException e) {
                /*
                 * Catch incorrectly formatted httpSessionCache uri values from server.xml file
                 */
                throw new IllegalArgumentException(Tr.formatMessage(tc, "CWLJC0007_URI_INVALID_SYNTAX", e), e);
            }
        else {
            this.configuredUri = null;
        }

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
        scheduledExecutorService.execute(new Runnable() {
            @Override
            public void run() {
                getCacheManager();
            }
        });
    }

    /**
     * Deactivate this OSGi component.
     */
    @Deactivate
    public void deactivate() {
        /*
         * Close and clear the CacheManager instance.
         */
        if (cacheManager != null) {
            cacheManager.close();
        }
        cacheManager = null;
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
            throw new UnsupportedOperationException("The cachingProvider feature is beta and is not available.");
        } else {
            /*
             * Running beta exception, issue message if we haven't already issued one for
             * this class
             */
            if (!issuedBetaMessage) {
                Tr.info(tc, "BETA: A beta method has been invoked for the class " + this.getClass().getName()
                            + " for the first time.");
                issuedBetaMessage = !issuedBetaMessage;
            }
        }
    }

    @Override
    public CacheManager getCacheManager() {
        if (cacheManager == null) {
            synchronized (syncObject) {
                if (cacheManager == null) {
                    try {
                        /*
                         * Perform some custom configuration updates for the CacheManager.
                         */
                        cacheConfigUtil = new CacheConfigUtil();
                        URI uri = cacheConfigUtil.preConfigureCacheManager(configuredUri,
                                                                           cachingProviderService.getCachingProvider(), properties);

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
                        cacheManager = cachingProviderService.getCachingProvider()
                                        .getCacheManager(uri, null,
                                                         properties);
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
    }

    /**
     * Unset the {@link CachingProviderService} for this {@link CacheManagerService}.
     *
     * @param service The {@link CachingProviderService}.
     */
    public void unsetCachingProviderService(CachingProviderService service) {
        this.cachingProviderService = null;
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
        return super.toString() + "{id=" + id + ", configuredUri=" + configuredUri + ", cacheManager=" + cacheManager + "}";
    }
}
