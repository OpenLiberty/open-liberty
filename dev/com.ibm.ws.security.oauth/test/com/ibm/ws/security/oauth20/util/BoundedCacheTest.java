/*******************************************************************************
 * Copyright (c) 2013, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth20.util;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Iterator;
import java.util.Map;

import org.junit.Test;

/**
 * This class tests the authorization table for a feature web bundle.
 */
public class BoundedCacheTest {

    /**
     * Test method for com.ibm.ws.security.oauth20.util.BoundedCache.put()
     * 
     */
    @Test
    public void put() {
        final int size = 5;
        BoundedCache<String, String> cache = new BoundedCache<String, String>(size);
        for (int i = 0; i < 100; i++) {
            //System.out.println("i = " + i);
            cache.put(String.valueOf(i), String.valueOf(i));
            if (i >= 5) {
                Iterator<Map.Entry<String, String>> it = cache.entrySet().iterator();
                if (it.hasNext()) {
                    Map.Entry<String, String> entry = it.next();
                    //System.out.println("Oldest entry = " + entry.getKey());
                    assertTrue("Oldest entry not correct",
                               entry.getKey().equals(String.valueOf(i - size + 1)));
                }
                else {
                    fail("BoundedCache interator is empty");
                }
            }
        }
    }

    /**
     * Test method for com.ibm.ws.security.oauth20.util.BoundedCache.putAll()
     * 
     */
    @Test
    public void putAll() {
        final int size = 5;
        final int sizeLarge = 100;
        BoundedCache<String, String> cache = new BoundedCache<String, String>(size);
        BoundedCache<String, String> cacheLarge = new BoundedCache<String, String>(sizeLarge);
        for (int i = 0; i < sizeLarge; i++) {
            cacheLarge.put(String.valueOf(i), String.valueOf(i));
        }
        Iterator<Map.Entry<String, String>> it = cacheLarge.entrySet().iterator();
        if (it.hasNext()) {
            Map.Entry<String, String> entry = it.next();
            //System.out.println("Oldest entry large = " + entry.getKey());
            assertTrue("Oldest entry not correct",
                       entry.getKey().equals(String.valueOf(0)));
        }
        cache.putAll(cacheLarge);
        it = cache.entrySet().iterator();
        if (it.hasNext()) {
            Map.Entry<String, String> entry = it.next();
            //System.out.println("Oldest entry = " + entry.getKey());
            assertTrue("Oldest entry not correct",
                       entry.getKey().equals(String.valueOf(sizeLarge - size)));
        }
    }
}
