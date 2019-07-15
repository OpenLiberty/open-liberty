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

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.spi.CachingProvider;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/ControllerCacheTestServlet")
public class ControllerCacheTestServlet extends FATServlet {
    private Cache<String, String> cache;
    private CacheManager cacheManager;
    private CachingProvider cacheProvider;

    @Override
    public void destroy() {
        AccessController.doPrivileged((PrivilegedAction<?>) () -> {
            cache.close();
            cacheManager.close();
            cacheProvider.close();
            return null;
        });
    }

    @Override
    public void init(ServletConfig servletConfig) throws ServletException {
        cache = AccessController.doPrivileged((PrivilegedAction<Cache<String, String>>) () -> {
            MutableConfiguration<String, String> cacheConfig = new MutableConfiguration<>();
            cacheConfig.setTypes(String.class, String.class);
            cacheProvider = Caching.getCachingProvider();
            cacheManager = cacheProvider.getCacheManager();
            return cacheManager.createCache("ControllerCacheTest", cacheConfig);
        });
    }

    /**
     * A basic that adds and removes a cache entry to demonstrate that the JCache provider is set up correctly and working.
     */
    @Test
    public void testBasicAddAndRemove() {
        cache.put("testBasicAddAndRemove", "value1");
        assertEquals("value1", cache.get("testBasicAddAndRemove"));
        assertTrue(cache.remove("testBasicAddAndRemove"));
    }
}
