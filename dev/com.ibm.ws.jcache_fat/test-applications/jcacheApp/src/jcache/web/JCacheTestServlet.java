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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URI;
import java.util.Iterator;

import javax.cache.Cache;
import javax.cache.Cache.Entry;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.AccessedExpiryPolicy;
import javax.cache.expiry.Duration;
import javax.cache.spi.CachingProvider;
import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/JCacheTestServlet")
public class JCacheTestServlet extends FATServlet {
    @Inject
    ZombiePod zp;
	
	public void mustHaveTest() throws Exception {
		assertTrue(true);
	}
	
    /**
     * Basic test to confirm we can get a cache, insert a value, and retrieve it.
     */
    public void basicJCacheTest() throws Exception {
        CachingProvider provider = Caching.getCachingProvider();
        URI hazelcastXML = new URI("../hazelcast/hazelcast-localhost-only.xml");
        CacheManager manager = provider.getCacheManager(hazelcastXML, Caching.getDefaultClassLoader());

        MutableConfiguration<String, Integer> config = new MutableConfiguration<String, Integer>()
                        .setTypes(String.class, Integer.class)
                        .setExpiryPolicyFactory(AccessedExpiryPolicy.factoryOf(Duration.ONE_HOUR));

        Cache<String, Integer> cache = manager.createCache("basicCache", config);
        cache.put("zip", 55902);
        
        assertEquals("Cache get should have been 55902", cache.get("zip"), (Integer) 55902);
        
        cache.close();    
        manager.destroyCache("basicCache");  
        try {
    			cache.put("zip2", 55901);
    			fail("Should have thrown IllegalStateException when invoking destroyed cache");
        } catch (IllegalStateException e) {
    			//Expected
        }
        
        provider.close(hazelcastXML, Caching.getDefaultClassLoader());
    }
    
    public void testCloseAndReopen() throws Exception {
        CachingProvider provider = Caching.getCachingProvider();
        URI hazelcastXML = new URI("../hazelcast/hazelcast-localhost-only.xml");
        CacheManager manager = provider.getCacheManager(hazelcastXML, Caching.getDefaultClassLoader());

        MutableConfiguration<String, Integer> config = new MutableConfiguration<String, Integer>()
                        .setTypes(String.class, Integer.class)
                        .setExpiryPolicyFactory(AccessedExpiryPolicy.factoryOf(Duration.ONE_HOUR));

        Cache<String, Integer> cache = manager.createCache("basicCache", config);
        cache.put("zip", 55902);
        cache.close();
        try {
        		cache.put("zip2", 55901);
        		fail("Should have thrown IllegalStateException when invoking closed cache");
        } catch (IllegalStateException e) {
    			//Expected
        }
        Cache<String, Integer> cache2 = manager.getCache("basicCache", String.class, Integer.class);
        cache2.put("zip2", 55901);
        assertEquals("Cache get should have been 55901",cache.get("zip2"), (Integer) 55901);
        
        manager.destroyCache("basicCache");
        provider.close(hazelcastXML, Caching.getDefaultClassLoader());
    }
    
    /**
     * Test that we can use an entry processor with a cache.
     * @throws Exception
     */
    public void testEntryProcessor() throws Exception {
        CachingProvider provider = Caching.getCachingProvider();
        URI hazelcastXML = new URI("../hazelcast/hazelcast-localhost-only.xml");
        CacheManager manager = provider.getCacheManager(hazelcastXML, Caching.getDefaultClassLoader());

        
        MutableConfiguration<Integer, String> config = new MutableConfiguration<Integer, String>()
                .setTypes(Integer.class, String.class)
                .setExpiryPolicyFactory(AccessedExpiryPolicy.factoryOf(Duration.ONE_HOUR));
        
        Cache<Integer, String> cache = manager.createCache("basicCache", config);
        
        cache.put(0, "Blizzard");
        cache.put(1, "ZombieApocalypse");
        cache.put(2, "Wendigo");
        
        ZombieProcessor zp = new ZombieProcessor();
        
        int count = 0;
        
        Iterator<Entry<Integer, String>> it = cache.iterator();
        
        while(it.hasNext()) {
        		Entry<Integer, String> e = it.next();
        		count += cache.invoke(e.getKey(), zp) ? 1 : 0;
        }

        assertEquals("Count did not match", 2, count);
        
        manager.destroyCache("basicCache");
        provider.close(hazelcastXML, Caching.getDefaultClassLoader());
    }
    
    /**
     * Test that we can use annotations
     */
    public void testAnnotations() throws Exception {
        zp.emptyPod();

        zp.addOrReplaceZombie(1, "Alex");
        assertEquals("Andy", zp.getZombie(2, "Andy"));
        //TODO assertEquals("Andy", zp.getZombie(2, "should-ignore-this-value"));
        //TODO assertEquals("Alex", zp.getZombie(1, "should-ignore-this-value"));

        zp.removeZombie(1);
        assertEquals("Jim", zp.getZombie(1, "Jim"));

        zp.addOrReplaceZombie(2, "Joe");
        // TODO assertEquals("Joe", zp.getZombie(2, "should-ignore-this-value"));

        zp.emptyPod();
        assertEquals("Mark", zp.getZombie(1, "Mark"));
        assertEquals("Nathan", zp.getZombie(2, "Nathan"));
    }
}
