/*******************************************************************************
 * Copyright (c) 2014, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth20.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import test.common.SharedOutputManager;

/**
 *
 */
public class BoundedConsentCacheTest {
    private static final String CLIENT_ID = "client_id";
    private static final String REDIRECT_URI = "redirect_uri";
    private static final String SCOPE = "scope";
    private static final String RESOURCE_ID = "resource_id";
    private static final int KEY_LIFETIME_SECONDS = 60;

    private final ConsentCacheKey KEY_1 = new ConsentCacheKey(CLIENT_ID + "1", REDIRECT_URI, SCOPE, RESOURCE_ID, KEY_LIFETIME_SECONDS);
    private final ConsentCacheKey KEY_2 = new ConsentCacheKey(CLIENT_ID + "2", REDIRECT_URI, SCOPE, RESOURCE_ID, KEY_LIFETIME_SECONDS);
    private final ConsentCacheKey KEY_3 = new ConsentCacheKey(CLIENT_ID + "3", REDIRECT_URI, SCOPE, RESOURCE_ID, KEY_LIFETIME_SECONDS);
    private final ConsentCacheKey KEY_4 = new ConsentCacheKey(CLIENT_ID + "4", REDIRECT_URI, SCOPE, RESOURCE_ID, KEY_LIFETIME_SECONDS);
    private final ConsentCacheKey KEY_1_UPDATE = new ConsentCacheKey(CLIENT_ID + "1", REDIRECT_URI, SCOPE, RESOURCE_ID, KEY_LIFETIME_SECONDS + 1000);

    private static SharedOutputManager outputMgr;

    /**
     * Capture stdout/stderr output to the manager.
     * 
     * @throws Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.captureStreams();
    }

    /**
     * Final teardown work when class is exiting.
     * 
     * @throws Exception
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        // Make stdout and stderr "normal"
        outputMgr.restoreStreams();
    }

    /**
     * Individual teardown after each test.
     * 
     * @throws Exception
     */
    @After
    public void tearDown() throws Exception {
        // Clear the output generated after each method invocation
        outputMgr.resetStreams();
    }

    @Test
    public void testConstructor() {
        BoundedConsentCache cache = new BoundedConsentCache(10);
        assertTrue("Cache was not initialized properly.", cache.getCapacity() == 10);
        cache = new BoundedConsentCache(0);
        assertTrue("Cache with capacity 0 was not initialized properly.", cache.getCapacity() == 0);
        cache = new BoundedConsentCache(-1);
        assertTrue("Cache initialized with negative capacity was not initialized properly.", cache.getCapacity() == 0);
    }

    @Test
    public void testGetCapacity() {
        BoundedConsentCache cache = new BoundedConsentCache(10);
        assertTrue("Cache capacity does not match.", cache.getCapacity() == 10);
    }

    @Test
    public void testUpdateCapacityWithEmptyCache() {
        BoundedConsentCache cache = new BoundedConsentCache(10);
        assertEquals("Cache was not initialized properly.", 10, cache.getCapacity());
        cache.updateCapacity(20);
        assertEquals("Cache capacity was not successfully increased.", 20, cache.getCapacity());
        cache.updateCapacity(5);
        assertEquals("Cache capacity was not successfully decreased.", 5, cache.getCapacity());
        cache.updateCapacity(0);
        assertEquals("Cache capacity was not successfully set to zero.", 0, cache.getCapacity());
        cache.updateCapacity(-1);
        assertEquals("Negative cache capacity was improperly set.", 0, cache.getCapacity());
    }

    @Test
    public void testUpdateCapacityWithEntries() {
        BoundedConsentCache cache = new BoundedConsentCache(10);
        cache.put(KEY_1);
        cache.put(KEY_2);
        cache.put(KEY_3);

        // Decrease capacity so capacity > size
        cache.updateCapacity(5);
        assertEquals("Cache capacity was not decreased properly.", 5, cache.getCapacity());
        assertEquals("Cache size has changed.", 3, cache.size());
        assertTrue("Cache does not contain all previous entries, despite having enough space with the updated capacity.",
                   cache.contains(KEY_1) && cache.contains(KEY_2) && cache.contains(KEY_3));

        // Updating to the same capacity should have no effect
        cache.updateCapacity(5);
        assertEquals("Cache capacity was not updated properly.", 5, cache.getCapacity());
        assertEquals("Cache size has changed.", 3, cache.size());
        assertTrue("Cache does not contain all entries, despite having enough space with the updated capacity.",
                   cache.contains(KEY_1) && cache.contains(KEY_2) && cache.contains(KEY_3));

        // Decrease capacity so capacity < size
        cache.put(KEY_4);
        cache.updateCapacity(2);
        assertEquals("Cache capacity was not decreased properly.", 2, cache.getCapacity());
        assertEquals("Cache size does not reflect new capacity update.", 2, cache.size());
        assertTrue("Cache does not contain the latest entries after capacity decrease.", cache.contains(KEY_3) && cache.contains(KEY_4));
        assertFalse("Oldest cache entries were not removed after capacity decrease.", cache.contains(KEY_1) || cache.contains(KEY_2));

        // Increase capacity
        cache.updateCapacity(5);
        assertEquals("Cache capacity was not increased properly.", 5, cache.getCapacity());
        assertEquals("Cache size has changed.", 2, cache.size());
        assertTrue("Cache does not contain all previous entries, despite having enough space with the updated capacity.",
                   cache.contains(KEY_3) && cache.contains(KEY_4));

        // Decrease capacity so capacity == size
        cache.updateCapacity(2);
        assertEquals("Cache capacity was not decreased properly.", 2, cache.getCapacity());
        assertEquals("Cache size does not reflect new capacity update.", 2, cache.size());
        assertTrue("Cache does not contain all previous entries, despite having enough space with the updated capacity.",
                   cache.contains(KEY_3) && cache.contains(KEY_4));

        // Decrease capacity to 0
        cache.updateCapacity(0);
        assertEquals("Cache capacity was not decreased properly.", 0, cache.getCapacity());
        assertEquals("Cache size does not reflect new capacity update.", 0, cache.size());

        // Negative capacity
        cache.updateCapacity(5);
        cache.put(KEY_1);
        cache.put(KEY_2);
        cache.put(KEY_3);
        cache.updateCapacity(-1);
        assertEquals("Cache capacity was not decreased properly.", 0, cache.getCapacity());
        assertEquals("Cache size does not reflect new capacity update.", 0, cache.size());
    }

    @Test
    public void testSize() {
        BoundedConsentCache cache = new BoundedConsentCache(0);
        assertEquals("Unexpected size for cache initialized with capacity 0 and no entries.", 0, cache.size());
        cache = new BoundedConsentCache(-1);
        assertEquals("Unexpected size for cache initialized with negative capacity and no entries.", 0, cache.size());
        cache = new BoundedConsentCache(2);
        assertEquals("Unexpected size for cache initialized with capacity 2 and no entries.", 0, cache.size());
        cache.put(KEY_1);
        assertEquals("Unexpected size for cache with single entry.", 1, cache.size());
        cache.put(KEY_2);
        assertEquals("Unexpected size for full cache.", 2, cache.size());
        cache.put(KEY_3);
        assertEquals("Unexpected size for full cache after adding new entry.", 2, cache.size());
        cache.remove(KEY_2);
        assertEquals("Unexpected size after removing entry.", 1, cache.size());
        cache.remove(KEY_3);
        assertEquals("Unexpected size after removing last entry.", 0, cache.size());
        cache.remove(KEY_3);
        assertEquals("Unexpected size after trying to remove a previous entry from empty cache.", 0, cache.size());
    }

    @Test
    public void testGet() {
        BoundedConsentCache cache = new BoundedConsentCache(10);
        assertNull("Empty cache should return null for any attempts to get an entry.", cache.get(KEY_1));

        cache.put(KEY_1);
        ConsentCacheKey keyGotten = cache.get(KEY_1);
        assertTrue("Incorrect cache key was returned.", KEY_1.equals(keyGotten));

        assertTrue("Cache key gotten should equal another cache key with a different lifetime.", KEY_1_UPDATE.equals(keyGotten));
        keyGotten = cache.get(KEY_1_UPDATE);
        assertTrue("Key with different lifetime should return an existing cache entry that is otherwise equivalent.", keyGotten.equals(KEY_1));

        keyGotten = cache.get(KEY_2);
        assertNull("Getting a key not in the cache should return null.", keyGotten);

        cache.put(KEY_2);
        cache.put(KEY_3);
        assertTrue("Incorrect cache key was returned from partially-filled cache.", KEY_2.equals(cache.get(KEY_2)));
    }

    @Test
    public void testPut() {
        BoundedConsentCache cache = new BoundedConsentCache(10);
        cache.put(KEY_1);
        assertTrue("Entry was not successfully added to empty cache having sufficient space.", cache.contains(KEY_1));
        cache.put(KEY_2);
        assertTrue("Entry was not successfully added to cache having sufficient space and at least one previous entry.", cache.contains(KEY_2));
        assertTrue("Previous cache entry was not found.", cache.contains(KEY_1));

        cache = new BoundedConsentCache(2);
        cache.put(KEY_1);
        cache.put(KEY_2);
        cache.put(KEY_3);
        assertTrue("Newest cache entry into the full cache was not found.", cache.contains(KEY_3));
        assertFalse("Oldest cache entry should have been removed once cache capacity was exceeded.", cache.contains(KEY_1));
        cache.put(KEY_4);
        assertTrue("Previous cache entry within capacity was not found.", cache.contains(KEY_3));
        assertTrue("Newest cache entry into the full cache was not found.", cache.contains(KEY_4));
        assertFalse("Oldest cache entries should have been removed once cache capacity was exceeded.", cache.contains(KEY_1) || cache.contains(KEY_2));
        cache.put(KEY_1);
        cache.put(KEY_1);
        cache.put(KEY_1);
        assertTrue("Cache does not contain key after multiple calls to put it in.", cache.contains(KEY_1));
        assertEquals("Unexpected cache size; a cache entry cannot have an identical entry within the cache.", 2, cache.size());
        assertTrue("Multiple calls to put the same cache entry into cache should not have removed the previous entry.", cache.contains(KEY_4));
        cache.put(KEY_1_UPDATE);
        assertTrue("Key with updated lifetime was not added successfully.", cache.contains(KEY_1_UPDATE));
        assertTrue("Key with updated lifetime should be equal to the original key.", cache.contains(KEY_1));
        assertTrue("Updating an existing key should not remove other entries.", cache.contains(KEY_4));

        cache = new BoundedConsentCache(0);
        cache.put(KEY_1);
        assertFalse("Cache initialized with a capacity of 0 should not allow any entries to be added.", cache.contains(KEY_1));
        assertEquals("No entries should be present in cache with capacity set to 0.", 0, cache.size());

        cache = new BoundedConsentCache(-10);
        cache.put(KEY_1);
        assertFalse("Cache initialized with a negative capacity should not allow any entries to be added.", cache.contains(KEY_1));
    }

    @Test
    public void testRemove() {
        BoundedConsentCache cache = new BoundedConsentCache(0);
        assertFalse("Removing from an empty cache should return false.", cache.remove(KEY_1));

        cache = new BoundedConsentCache(10);
        cache.put(KEY_1);
        assertTrue("Removing single entry did not succeed.", cache.remove(KEY_1));
        assertFalse("Entry still contained in the cache.", cache.contains(KEY_1));
        assertEquals("Cache size was not updated properly after single entry removal.", 0, cache.size());

        cache = new BoundedConsentCache(2);
        cache.put(KEY_1);
        cache.put(KEY_2);
        assertTrue("Removing old entry did not succeed.", cache.remove(KEY_1));
        assertFalse("Old entry still contained in the cache.", cache.contains(KEY_1));
        assertEquals("Cache size was not updated properly after entry removal.", 1, cache.size());
        cache.put(KEY_3);
        assertTrue("Removing last entry did not succeed.", cache.remove(KEY_3));
        assertFalse("Latest entry still contained in the cache despite removal.", cache.contains(KEY_3));

        cache = new BoundedConsentCache(2);
        cache.put(KEY_1);
        assertTrue("Key with updated lifetime did not remove existing entry.", cache.remove(KEY_1_UPDATE));
        assertFalse("Removed entry still contained in the cache despite removal.", cache.contains(KEY_1));
    }

}
