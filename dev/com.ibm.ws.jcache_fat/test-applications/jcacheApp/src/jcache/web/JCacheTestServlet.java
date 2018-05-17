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
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/JCacheTestServlet")
public class JCacheTestServlet extends FATServlet {
	
	//TODO: Disabling all tests until the hazelcast config for the bucket is fixed and Annotations are addressed.
	@Test
	public void mustHaveTest() throws Exception {
		assertTrue(true);
	}
	
    /**
     * Basic test to confirm we can get a cache, insert a value, and retrieve it.
     */
    //@Test
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
    
    //@Test
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
    //@Test
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
    //@Test
    public void testAnnotations() throws Exception {
    		ZombiePod zp = new ZombiePod();
    		String name = zp.getZombie(2);
    		int zid = zp.addZombie("Nathan");
    		zp.emptyPod();
    		
    		CachingProvider provider = Caching.getCachingProvider();
        URI hazelcastXML = new URI("../hazelcast/hazelcast-localhost-only.xml");
        CacheManager manager = provider.getCacheManager(hazelcastXML, Caching.getDefaultClassLoader());
        //Cache zom = manager.getCache("zombies");
        //System.out.println(zom.iterator().hasNext());
    
            
    		assertEquals("Should have received cached name",name,zp.getZombie(2));		
    		
    		//TODO remove cache
    }
}
