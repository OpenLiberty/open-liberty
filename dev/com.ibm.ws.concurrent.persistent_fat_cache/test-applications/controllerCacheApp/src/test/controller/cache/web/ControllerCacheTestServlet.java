/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.controller.cache.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.Factory;
import javax.cache.configuration.FactoryBuilder;
import javax.cache.configuration.MutableCacheEntryListenerConfiguration;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.Duration;
import javax.cache.expiry.ModifiedExpiryPolicy;
import javax.cache.spi.CachingProvider;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/ControllerCacheTestServlet")
public class ControllerCacheTestServlet extends FATServlet {
    private static final long TIMEOUT_NS = TimeUnit.MINUTES.toNanos(2);

    static final Map<String, String> created = new ConcurrentHashMap<String, String>();
    static final List<String> expired = new ArrayList<String>();

    private Cache<String, String> cache;
    private CacheManager cacheManager;
    private CachingProvider cacheProvider;

    private ExecutorService testThreads = Executors.newFixedThreadPool(5);

    @Override
    public void destroy() {
        testThreads.shutdownNow();
        AccessController.doPrivileged((PrivilegedAction<?>) () -> {
            cache.close();
            cacheManager.close();
            cacheProvider.close();
            return null;
        });
    }

    /**
     * Confirm that entries exist in the cache, as specified in query parameters of the form,
     * key<KeyName>=<ExpectedValue>
     */
    public void getEntries(HttpServletRequest req, HttpServletResponse res) {
        for (Enumeration<String> en = req.getParameterNames(); en.hasMoreElements();) {
            String paramName = en.nextElement();
            if (paramName.startsWith("key")) {
                String expected = req.getParameter(paramName);
                paramName = paramName.substring(3);
                String found = cache.get(paramName);
                System.out.println("Getting " + paramName + ", found: " + found);
                assertEquals("Unexpected value of " + paramName, expected, found);
            }
        }
    }

    @Override
    public void init(ServletConfig servletConfig) throws ServletException {
        cache = AccessController.doPrivileged((PrivilegedAction<Cache<String, String>>) () -> {
            MutableConfiguration<String, String> cacheConfig = new MutableConfiguration<>();
            cacheConfig.setTypes(String.class, String.class);
            cacheConfig.setExpiryPolicyFactory(FactoryBuilder.factoryOf(new ModifiedExpiryPolicy(new Duration(TimeUnit.SECONDS, 5))));

            Factory<TestCacheEntryCreatedOrExpiredListener> listenerFactory = FactoryBuilder.factoryOf(TestCacheEntryCreatedOrExpiredListener.class);
            MutableCacheEntryListenerConfiguration<String, String> listenerConfig = new MutableCacheEntryListenerConfiguration<String, String>( //
                            listenerFactory, //
                            null, //
                            true, // old value required
                            false);
            cacheConfig.addCacheEntryListenerConfiguration(listenerConfig);

            cacheProvider = Caching.getCachingProvider();
            cacheManager = cacheProvider.getCacheManager();
            Cache<String, String> c = cacheManager.getCache("ControllerCacheTest", String.class, String.class);
            if (c == null)
                return cacheManager.createCache("ControllerCacheTest", cacheConfig);
            else
                return c;
        });
    }

    /**
     * Add or replace cache entries, as specified in query parameters of the form,
     * key<KeyName>=<NewValue>
     */
    public void putEntries(HttpServletRequest req, HttpServletResponse res) {
        for (Enumeration<String> en = req.getParameterNames(); en.hasMoreElements();) {
            String paramName = en.nextElement();
            if (paramName.startsWith("key")) {
                String value = req.getParameter(paramName);
                paramName = paramName.substring(3);
                System.out.println("Putting " + paramName);
                cache.put(paramName, value);
            }
        }
    }

    /**
     * A basic that adds and removes a cache entry to demonstrate that the JCache provider is set up correctly and working.
     */
    public void testBasicAddAndRemove() {
        cache.put("testBasicAddAndRemove", "value1");
        assertEquals("value1", cache.get("testBasicAddAndRemove"));
        assertTrue(cache.remove("testBasicAddAndRemove"));
    }

    /**
     * Wait (if necessary) for notifications that the specified cache entries have been created.
     */
    public void waitForCreatedNotifications(HttpServletRequest req, HttpServletResponse res) throws InterruptedException {
        long start = System.nanoTime();
        for (Enumeration<String> en = req.getParameterNames(); en.hasMoreElements();) {
            String paramName = en.nextElement();
            if (paramName.startsWith("key")) {
                paramName = paramName.substring(3);
                System.out.println("Waiting for created notification: " + paramName);
                while (created.get(paramName) == null && System.nanoTime() - start < TIMEOUT_NS)
                    Thread.sleep(200);
                String val = created.get(paramName);
                if (val == null)
                    throw new RuntimeException("Did not find " + paramName + " in " + created);
                else
                    System.out.println("FOUND " + paramName);
            }
        }
    }

    /**
     * Wait (if necessary) for notifications that the specified cache entries have expired.
     */
    public void waitForExpiredNotifications(HttpServletRequest req, HttpServletResponse res) throws InterruptedException {
        // The following is a workaround for Hazelcast behavior of delaying the onExpired notification
        // until the user attempts to access the cache entry and finds it missing.
        // For this workaround, a background thread repeatedly accesses the cache entries so that Hazelcast can be
        // prompted to send the onExpire event once enough time elapses.
        ConcurrentLinkedQueue<String> hazelcastExpiryWorkaroundKeys = new ConcurrentLinkedQueue<String>();
        Future<?> hazelcastExpiryWorkaroundThread = testThreads.submit(() -> {
            try {
                for (; !cache.isClosed(); Thread.sleep(1000))
                    for (String key : hazelcastExpiryWorkaroundKeys)
                        if (!cache.isClosed())
                            cache.get(key);
            } catch (InterruptedException x) {
                System.out.println("Hazelcast expiry workaround thread is done.");
            }
        });
        // TODO if not Hazelcast, then skip the above workaround

        try {
            long start = System.nanoTime();
            for (Enumeration<String> en = req.getParameterNames(); en.hasMoreElements();) {
                String paramName = en.nextElement();
                if (paramName.startsWith("key")) {
                    String key = paramName.substring(3);
                    hazelcastExpiryWorkaroundKeys.add(key);
                    String search = key + ':' + req.getParameter(paramName);
                    System.out.println("Waiting for expired notification: " + search);
                    while (!expired.contains(search) && System.nanoTime() - start < TIMEOUT_NS)
                        Thread.sleep(200);
                    if (!expired.contains(search))
                        throw new RuntimeException("Did not find " + search + " in " + expired);
                    else
                        System.out.println("FOUND " + search);
                }
            }
        } finally {
            if (hazelcastExpiryWorkaroundThread != null)
                hazelcastExpiryWorkaroundThread.cancel(true);
        }
    }
}
