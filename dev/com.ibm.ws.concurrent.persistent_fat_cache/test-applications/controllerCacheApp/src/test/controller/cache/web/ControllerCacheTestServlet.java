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
import java.util.Enumeration;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.MutableConfiguration;
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
}
