/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.wim.env;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.HashMap;

import org.junit.Test;

import com.ibm.ws.security.wim.FactoryManager;

public class CacheTest {

    // private static final SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    /**
     * Using the test rule will drive capture/restore and will dump on error..
     * Notice this is not a static variable, though it is being assigned a value we
     * allocated statically. -- the normal-variable-ness is for before/after processing
     */
    // @Rule
    // public TestRule managerRule = outputMgr;

    @Test
    public void testCreate() {
        assertNotNull("Created", FactoryManager.getCacheUtil());
    }

    @Test
    public void testInitialize() {
        ICacheUtil cache = FactoryManager.getCacheUtil();
        cache = cache.initialize(1, 3, 2000);

        int size = cache.size();

        assertEquals("Invalid initial size", 0, size);
    }

    @Test
    public void testMaxsize() {
        ICacheUtil cache = FactoryManager.getCacheUtil();
        cache = cache.initialize(1, 3, 2000);

        cache.put("a", "a");
        cache.put("b", "b");
        cache.put("c", "c");
        cache.put("d", "d");
        cache.put("e", "e");

        int size = cache.size();

        assertEquals("Invalid max size", 1, size);
    }

    @Test
    public void testInvalidate() {
        ICacheUtil cache = FactoryManager.getCacheUtil();
        cache = cache.initialize(1, 3, 2000);

        cache.put("a", "a");

        int size = cache.size();

        assertEquals("Invalid initial size", 1, size);

        cache.invalidate("a");

        size = cache.size();

        assertEquals("Invalid final size", 0, size);
    }

    @Test
    public void testInvalidateCache() {
        ICacheUtil cache = FactoryManager.getCacheUtil();
        cache = cache.initialize(1, 3, 2000);

        cache.put("a", "a");
        cache.put("b", "b");
        cache.put("c", "c");

        int size = cache.size();

        assertEquals("Invalid initial size", 3, size);

        cache.invalidate();

        size = cache.size();

        assertEquals("Invalid final size", 0, size);
    }

    @Test
    public void testRetrival() {
        ICacheUtil cache = FactoryManager.getCacheUtil();
        cache = cache.initialize(1, 3, 2000);

        cache.put("a", "a");

        assertEquals("Invalid return", "a", cache.get("a"));
    }

    @Test
    public void testContainsKey() {
        ICacheUtil cache = FactoryManager.getCacheUtil();
        cache = cache.initialize(1, 3, 1000);

        cache.put("a", "a");
        assertEquals("The containsKey returned incorrect answer", true, cache.containsKey("a"));
    }

    @Test
    public void testPutAll() {
        ICacheUtil cache = FactoryManager.getCacheUtil();
        cache = cache.initialize(1, 3, 1000);

        HashMap<String, String> data = new HashMap<String, String>();
        data.put("a", "a");
        data.put("b", "b");

        cache.putAll(data);

        assertEquals("Incorrect cache size", 2, cache.size());
    }

    @Test
    public void testEviction() {
        ICacheUtil cache = FactoryManager.getCacheUtil();
        cache = cache.initialize(1, 3, 500);

        cache.put("a", "a");

        int size = cache.size();

        assertEquals("Invalid initial size", 1, size);

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        size = cache.size();

        if (size != 0) {
            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            size = cache.size();
        }

        assertEquals("Invalid Final size", 0, size);
    }
}
