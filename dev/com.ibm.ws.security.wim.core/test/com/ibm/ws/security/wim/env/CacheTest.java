/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;

import org.junit.Test;

import com.ibm.ws.security.wim.FactoryManager;
import com.ibm.ws.security.wim.env.was.Cache;
import com.ibm.ws.security.wim.env.was.Cache.CacheEntry;

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
        cache.put("a", "a");
        size = cache.size();
        assertEquals("Invalid size", 1, size);

        cache = cache.initialize("name", 3, false);

        size = cache.size();
        assertEquals("Invalid initial size", 0, size);
        cache.put("a", "a");
        size = cache.size();
        assertEquals("Invalid size", 1, size);

        cache = cache.initialize("name", 3, false, 0);

        size = cache.size();
        assertEquals("Invalid initial size", 0, size);
        cache.put("a", "a");
        size = cache.size();
        assertEquals("Invalid size", 1, size);
    }

    @Test
    public void testMaxSize() {
        ICacheUtil cache = FactoryManager.getCacheUtil();
        cache = cache.initialize(1, 3, 2000);

        cache.put("a", "a");
        cache.put("b", "b");
        cache.put("c", "c");
        cache.put("d", "d");
        cache.put("e", "e");

        assertFalse("Entry should have been evicted.", cache.containsKey("a"));
        assertFalse("Entry should have been evicted.", cache.containsKey("b"));
        assertFalse("Entry should have been evicted.", cache.containsKey("c"));
        assertTrue("Entry is missing or corrupted.", cache.containsKey("d"));
        assertTrue("Entry is missing or corrupted.", cache.containsKey("e"));
        sleep(500);
        int size = cache.size();

        assertEquals("Invalid size", 2, size);
    }

    @Test
    public void testInvalidate() {
        ICacheUtil cache = FactoryManager.getCacheUtil();
        cache = cache.initialize(1, 3, 2000);

        cache.put("a", "a");
        cache.put("b", "b");

        int size = cache.size();

        assertNotNull("Entry is missing or corrupted.", cache.get("a"));
        assertNotNull("Entry is missing or corrupted.", cache.get("b"));
        assertEquals("Invalid initial size", 2, size);

        cache.invalidate("a");

        size = cache.size();

        assertNull("Entry should have been invalidated.", cache.get("a"));
        assertNotNull("Entry should have been created after access.", cache.get("b"));
        assertEquals("Invalid final size", 1, size);
    }

    @Test
    public void testInvalidateCache() {
        ICacheUtil cache = FactoryManager.getCacheUtil();
        cache = cache.initialize(1, 3, 2000);

        cache.put("a", "a");
        cache.put("b", "b");
        cache.put("c", "c");

        int size = cache.size();

        assertNotNull("Entry is missing or corrupted.", cache.get("a"));
        assertNotNull("Entry is missing or corrupted.", cache.get("b"));
        assertNotNull("Entry is missing or corrupted.", cache.get("c"));
        assertEquals("Invalid initial size", 3, size);

        cache.invalidate();

        size = cache.size();

        assertNull("Entry should have been invalidated.", cache.get("a"));
        assertNull("Entry should have been invalidated.", cache.get("b"));
        assertNull("Entry should have been invalidated.", cache.get("c"));
        assertEquals("Invalid final size", 0, size);
    }

    @Test
    public void testRetrieval() {
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
    public void testContainsKey_IncludeDiskCache() {
        ICacheUtil cache = FactoryManager.getCacheUtil();
        cache = cache.initialize(1, 3, 1000);

        cache.put("a", "a");

        assertEquals("The containsKey returned incorrect answer", true, cache.containsKey("a", true));
        assertEquals("The containsKey returned incorrect answer", true, cache.containsKey("a", false));
    }

    @Test
    public void testPutAll() {
        ICacheUtil cache = FactoryManager.getCacheUtil();
        cache = cache.initialize(1, 3, 1000);

        HashMap<String, String> data = new HashMap<String, String>();
        data.put("a", "a");
        data.put("b", "b");

        cache.putAll(data);

        int size = cache.size();

        assertNotNull("Entry is missing or corrupted.", cache.get("a"));
        assertNotNull("Entry is missing or corrupted.", cache.get("b"));
        assertEquals("Incorrect cache size", 2, size);
    }

    @Test
    public void testEviction() {
        ICacheUtil cache = FactoryManager.getCacheUtil();
        cache = cache.initialize(1, 3, 500);

        cache.put("a", "a");

        int size = cache.size();
        assertNotNull("Entry is missing or corrupted.", cache.get("a"));
        assertEquals("Invalid initial size", 1, size);

        sleep(600);
        size = cache.size();
        assertNull("Entry should have been evicted.", cache.get("a"));
        assertEquals("Invalid Final size", 0, size);
    }

    @Test
    public void testTimeout() throws Exception {
        ICacheUtil cache = FactoryManager.getCacheUtil();
        cache = cache.initialize(1, 3, 500);

        cache.put("a", "a");

        sleep(200);
        assertNotNull("Entry is missing or corrupted.", cache.get("a"));
        // total 400ms
        sleep(200);
        assertNotNull("Entry is missing or corrupted.", cache.get("a"));
        // total 600ms
        sleep(200);
        assertNull("Entry should have been evicted.", cache.get("a"));

        // Attempt two
        cache = cache.initialize(1, 3, 1500);

        cache.put("a", "a");

        sleep(200);
        assertNotNull("Entry is missing or corrupted.", cache.get("a"));
        sleep(200);
        assertNotNull("Entry is missing or corrupted.", cache.get("a"));
        sleep(200);
        assertNotNull("Entry is missing or corrupted.", cache.get("a"));
        sleep(200);
        assertNotNull("Entry is missing or corrupted.", cache.get("a"));
        sleep(200);
        assertNotNull("Entry is missing or corrupted.", cache.get("a"));
        sleep(200);
        assertNotNull("Entry is missing or corrupted.", cache.get("a"));
        sleep(200);
        assertNotNull("Entry is missing or corrupted.", cache.get("a"));
        sleep(200);
        assertNull("Entry should have been evicted.", cache.get("a"));
    }

    @Test
    public void testStopEviction() {
        ICacheUtil cache = FactoryManager.getCacheUtil();
        cache = cache.initialize(1, 3, 300);

        cache.put("a", "a");

        sleep(200);
        assertNotNull("Entry is missing or corrupted.", cache.get("a"));

        sleep(200);
        assertNull("Entry should be evicted.", cache.get("a"));

        cache.put("a", "a");
        cache.stopEvictionTask();
        sleep(500);
        assertNotNull("Entry is missing or corrupted.", cache.get("a"));
    }

    @Test
    public void testContainsValue() {
        ICacheUtil cache = FactoryManager.getCacheUtil();
        cache = cache.initialize(1, 5, 3000);

        cache.put("a", "a");
        cache.put("b", "b");

        int size = cache.size();
        assertEquals("Invalid size", 2, size);
        assertEquals("Wrong value", "a", cache.get("a"));
        assertEquals("Wrong value", "b", cache.get("b"));

        assertTrue("Value missing or corrupted.", cache.containsValue("a"));
        assertTrue("Value missing or corrupted.", cache.containsValue("b"));

        cache.clear();
        assertFalse("Cache should be cleared.", cache.containsValue("a"));
        assertFalse("Cache should be cleared.", cache.containsValue("b"));

    }

    @Test
    public void testEntrySet() {
        ICacheUtil cache = FactoryManager.getCacheUtil();
        cache = cache.initialize(1, 5, 3000);

        cache.put("a", "a");
        cache.put("b", "b");
        cache.put("c", "c");

        Set<Entry<String, Object>> set = cache.entrySet();

        Iterator<Entry<String, Object>> it = set.iterator();

        Entry<String, Object> entry = it.next();
        assertEquals("Key missing or corrupted.", "a", entry.getKey());
        assertEquals("Value missing or corrupted.", new Cache.CacheEntry("a"), entry.getValue());

        entry = it.next();
        assertEquals("Key missing or corrupted.", "b", entry.getKey());
        assertEquals("Value missing or corrupted.", new Cache.CacheEntry("b"), entry.getValue());

        entry = it.next();
        assertEquals("Key missing or corrupted.", "c", entry.getKey());
        assertEquals("Value missing or corrupted.", new Cache.CacheEntry("c"), entry.getValue());
    }

    @Test
    public void testGetNotSharedInt() {
        ICacheUtil cache = FactoryManager.getCacheUtil();
        cache = cache.initialize(1, 5, 3000);
        // default - not implemented
        assertEquals("Method implementation changed. Expected a default value.", 0, cache.getNotSharedInt());

    }

    @Test
    public void testGetSharedPushInt() {
        ICacheUtil cache = FactoryManager.getCacheUtil();
        cache = cache.initialize(1, 5, 3000);
        // default - not implemented
        assertEquals("Method implementation changed. Expected a default value.", 0, cache.getSharedPushInt());

    }

    @Test
    public void testGetSharedPushPullInt() {
        ICacheUtil cache = FactoryManager.getCacheUtil();
        cache = cache.initialize(1, 5, 3000);
        // default - not implemented
        assertEquals("Method implementation changed. Expected a default value.", 0, cache.getSharedPushPullInt());

    }

    @Test
    public void testGetSharingPolicyInt() {
        ICacheUtil cache = FactoryManager.getCacheUtil();
        cache = cache.initialize(1, 5, 3000);
        // default - not implemented
        assertEquals("Method implementation changed. Expected a default value.", 0, cache.getSharingPolicyInt("notUsed"));

    }

    @Test
    public void testIsCacheAvailable() {
        ICacheUtil cache = FactoryManager.getCacheUtil();
        cache = cache.initialize(1, 5, 3000);
        // default - not implemented
        assertTrue("Method implementation changed. Expected a default value.", cache.isCacheAvailable());

    }

    @Test
    public void testIsEmpty() {
        ICacheUtil cache = FactoryManager.getCacheUtil();
        cache = cache.initialize(1, 5, 3000);

        assertTrue("Cache should be empty.", cache.isEmpty());
        cache.put("a", "a");
        assertFalse("Cache should not be empty.", cache.isEmpty());
    }

    @Test
    public void testIsCacheInitialized() {
        ICacheUtil cache = FactoryManager.getCacheUtil();
        assertFalse("Cache should not be initialized.", cache.isCacheInitialized());

        cache = cache.initialize(1, 5, 3000);
        assertTrue("Cache should be initialized.", cache.isCacheInitialized());

    }

    @Test
    public void testKeySet() {
        ICacheUtil cache = FactoryManager.getCacheUtil();
        cache = cache.initialize(1, 5, 3000);

        cache.put("a", "a");
        cache.put("b", "b");
        cache.put("c", "c");

        cache.keySet();

        Set<String> set = cache.keySet();

        Iterator<String> it = set.iterator();

        String entry = it.next();
        assertTrue("Value missing or corrupted.", entry.equals("a"));

        entry = it.next();
        assertTrue("Value missing or corrupted.", entry.equals("b"));

        entry = it.next();
        assertTrue("Value missing or corrupted.", entry.equals("c"));
    }

    @Test
    public void testRemove() {
        ICacheUtil cache = FactoryManager.getCacheUtil();
        cache = cache.initialize(1, 5, 3000);

        cache.put("a", "a");
        assertFalse("Cache should not be empty.", cache.isEmpty());
        cache.remove("a");
        assertTrue("Cache should be empty.", cache.isEmpty());

    }

    @Test
    public void testPut() {
        ICacheUtil cache = FactoryManager.getCacheUtil();
        cache = cache.initialize(1, 5, 3000);

        Object obj = new Object();
        Object val = new Object();

        cache.put(obj, val, 0, 0, 0, null);
        assertFalse("Cache should not be empty.", cache.isEmpty());
        cache.remove(obj);
        assertTrue("Cache should be empty.", cache.isEmpty());

    }

    @Test
    public void testPut_Multi() {
        ICacheUtil cache = FactoryManager.getCacheUtil();
        cache = cache.initialize(1, 5, 1500);

        cache.put("a", "a");
        sleep(600);

        assertEquals("Invalid entry returned.", "a", cache.put("a", "a"));

        int size = cache.size();

        assertEquals("Too many entries in cache", 1, size);

        Object obj = cache.remove("a");

        assertNotNull("An entry should be returned", obj);
        assertTrue("Returned entry should be a CacheEntry", obj instanceof CacheEntry);

        size = cache.size();

        assertEquals("Too many entries in cache", 0, size);
        assertNull("An entry should not be returned", cache.remove("a"));

    }

    @Test
    public void testPut_AfterTimeOut() {
        ICacheUtil cache = FactoryManager.getCacheUtil();
        cache = cache.initialize(1, 5, 500);

        cache.put("a", "a");
        sleep(600);

        int size = cache.size();

        assertEquals("Invalid number of entries in cache", 0, size);
        assertNull("Entry should have timed out.", cache.remove("a"));
        assertNull("Entry should have timed out.", cache.put("a", "a"));

        size = cache.size();

        assertEquals("Invalid number of entries in cache", 1, size);

        Object obj = cache.remove("a");

        assertNotNull("An entry should be returned", obj);
        assertTrue("Returned entry should be a CacheEntry", obj instanceof CacheEntry);

        size = cache.size();

        assertEquals("Invalid number of entries in cache", 0, size);
        assertNull("An entry should not be returned", cache.remove("a"));

    }

    @Test
    public void testSetTTL() {
        ICacheUtil cache = FactoryManager.getCacheUtil();
        cache = cache.initialize(1, 5, 500);

        assertEquals("Invalid timeout value.", 500, Cache.getDefaultTimeout());
        cache.put("a", "a");
        sleep(600);

        int size = cache.size();

        assertEquals("Invalid number of entries in cache", 0, size);
        assertNull("Entry should have timed out.", cache.remove("a"));

        cache.setTimeToLive(2000);
        assertEquals("Invalid timeout value.", 2000, Cache.getDefaultTimeout());
        cache.put("a", "a");
        sleep(2100);

        size = cache.size();

        assertEquals("Invalid number of entries in cache", 0, size);
        assertNull("Entry should have timed out.", cache.remove("a"));

        Cache.setDefaultTimeout(1000);
        assertEquals("Invalid timeout value.", 1000, Cache.getDefaultTimeout());
        cache.put("a", "a");
        sleep(1100);

        size = cache.size();

        assertEquals("Invalid number of entries in cache", 0, size);
        assertNull("Entry should have timed out.", cache.remove("a"));

    }

    @Test
    public void testClear() {
        ICacheUtil cache = FactoryManager.getCacheUtil();
        cache = cache.initialize(1, 5, 3000);

        cache.put("a", "a");
        cache.put("b", "b");
        cache.put("c", "c");

        assertEquals("Invalid cache size.", 3, cache.size());
        assertFalse("Invalid cache size.", cache.isEmpty());

        cache.clear();

        assertEquals("Cache should be empty.", 0, cache.size());
        assertTrue("Cache should be empty.", cache.isEmpty());

    }

    @Test
    public void testGet() {
        ICacheUtil cache = FactoryManager.getCacheUtil();
        cache = cache.initialize(1, 5, 3000);

        assertNull("Entry should not have been present.", cache.get("a"));
        assertTrue("Key and null value should have been created.", cache.containsKey("a"));

        cache.put("a", "a");

        assertTrue("Value missing or corrupted.", cache.get("a").equals("a"));

    }

    @Test
    public void testInsert() {
        ICacheUtil cache = FactoryManager.getCacheUtil();
        cache = cache.initialize(1, 5, 600);

        ((Cache) cache).insert("a", "a");

        assertTrue("Entry missing or corrupted.", "a".equals(cache.get("a")));
        assertEquals("Invalid size.", 1, cache.size());
        sleep(700);
        assertEquals("Invalid size.", 0, cache.size());
        assertNull("Entry should not be present.", cache.get("a"));
        assertEquals("Invalid size.", 1, cache.size());

        assertNull("No return value should be provided", ((Cache) cache).insert("a", "a"));
        assertNull("No return value should be provided", ((Cache) cache).insert("b", "b"));
        assertEquals("Invalid size.", 2, cache.size());

        assertTrue("Entry missing or corrupted.", "a".equals(cache.get("a")));
        assertTrue("Entry missing or corrupted.", "b".equals(cache.get("b")));
        assertEquals("Invalid size.", 2, cache.size());

        //first table
        assertTrue("Old entry's value should have been returned.", "a".equals(((Cache) cache).insert("a", "b")));

        //second table
        sleep(250);
        assertTrue("Old entry's value should have been returned.", "b".equals(((Cache) cache).insert("a", "c")));
        sleep(700);
        assertNull("Entry should not be present", cache.get("a"));
    }

    /**
     * Check that when we do an update, the entry still evicts at the insert time.
     * We don't want the timeout to reset (last access time vs creation time).
     */
    @Test
    public void testInsertWithUpdate() {
        ICacheUtil cache = FactoryManager.getCacheUtil();
        cache = cache.initialize(1, 5, 600);

        ((Cache) cache).insert("a", "a");

        assertEquals("Entry missing or corrupted.", "a", cache.get("a"));
        assertEquals("Invalid size.", 1, cache.size());

        sleep(100);

        ((Cache) cache).update("a", "b");
        assertEquals("Entry missing or corrupted.", "b", cache.get("a"));
        assertEquals("Invalid size.", 1, cache.size());

        // even with an update, the entry should be evicted
        sleep(600);

        assertEquals("Invalid size.", 0, cache.size());
        assertNull("Entry should not be present.", cache.get("a"));
        assertEquals("Invalid size.", 1, cache.size());

        assertNull("No return value should be provided", ((Cache) cache).insert("a", "a"));
        assertNull("No return value should be provided", ((Cache) cache).insert("b", "b"));
        assertEquals("Invalid size.", 2, cache.size());

    }

    @Test
    public void testInsert_ThirdTable() {
        ICacheUtil cache = FactoryManager.getCacheUtil();
        cache = cache.initialize(1, 5, 3000);

        ((Cache) cache).insert("a", "a");

        sleep(2500);
        assertTrue("Old entry's value should have been returned.", "a".equals(((Cache) cache).insert("a", "d")));
    }

    @Test
    public void testValues() {
        ICacheUtil cache = FactoryManager.getCacheUtil();
        cache = cache.initialize(1, 5, 3000);

        ((Cache) cache).insert("a", "a");
        cache.put("b", "b");
        cache.put("c", "c");

        Collection<Object> c = cache.values();

        assertFalse("List should not be empty.", c.isEmpty());
        assertEquals("Invalid list size.", 3, c.size());

        Iterator<Object> it = c.iterator();

        CacheEntry ce = (CacheEntry) it.next();
        assertTrue("Value missing or corrupted.", ce.value.equals("a"));

        ce = (CacheEntry) it.next();
        assertTrue("Value missing or corrupted.", ce.value.equals("b"));

        ce = (CacheEntry) it.next();
        assertTrue("Value missing or corrupted.", ce.value.equals("c"));

        try {
            it.next();
            // Should not reach
            fail();
        } catch (NoSuchElementException e) {
            // Expected
        }

    }

    // move to some util location
    public static void sleep(long millis) {
        long end = System.currentTimeMillis() + millis;
        long millisToSleep;
        while (end > System.currentTimeMillis()) {
            millisToSleep = end - System.currentTimeMillis();

            try {
                Thread.sleep(millisToSleep);
            } catch (InterruptedException e) {
                // IGNORE
            }
        }
    }

}
