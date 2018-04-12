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
package jcache.web;

import static org.junit.Assert.assertEquals;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.AccessedExpiryPolicy;
import javax.cache.expiry.Duration;
import javax.cache.spi.CachingProvider;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/JCacheTestServlet")
public class JCacheTestServlet extends FATServlet {

    /**
     * Basic test to confirm we can get a cache, insert a value, and retrieve it.
     */
    @Test
    public void basicJCacheTest() throws Exception {
        CachingProvider provider = Caching.getCachingProvider();
        CacheManager manager = provider.getCacheManager();

        MutableConfiguration<String, Integer> config = new MutableConfiguration<String, Integer>()
                        .setTypes(String.class, Integer.class)
                        .setExpiryPolicyFactory(AccessedExpiryPolicy.factoryOf(Duration.ONE_HOUR));

        Cache<String, Integer> cache = manager.createCache("basicCache", config);
        cache.put("zip", 55902);
        assertEquals("Cache get should have been 55902", cache.get("zip"), (Integer) 55902);
    }
    
    @Test
    public void test2() throws Exception {
    	
    }
    
    
    
}
