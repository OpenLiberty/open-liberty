/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.util.cache;

import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class BackgroundLruEvictionStrategyTest
{
    @Test
    @Ignore
    public void testBackgroundLruEvictionStrategy() throws Exception
    {
        int size = 15;

        int bigSize = 30;

        int softLimit = 15;

        Object[] objectList = new Object[bigSize];

        Cache testCache = new Cache("TestCache", 9, false);

        final ScheduledExecutorService scheduledExecutorService =
                        Executors.newScheduledThreadPool(1); // F73234

        BackgroundLruEvictionStrategy ev =
                        new BackgroundLruEvictionStrategy(testCache, 9, 1000, scheduledExecutorService, scheduledExecutorService); // F73234
        testCache.setEvictionStrategy(ev);
        testCache.setCachePreferredMaxSize(softLimit);

        java.text.SimpleDateFormat format =
                        new java.text.SimpleDateFormat("HH:mm:ss.SSSS");

        //
        // Fill the cache to the soft limit with some stuff
        //

        for (int i = 0; i < size; ++i) {
            String key = Integer.toString(i);
            String object = format.format(new Date());
            System.out.println("Inserting (" + key + ", " + object + ")");
            testCache.insert(key, object);
            objectList[i] = object;
            testCache.unpin(key);
        }

        System.out.println("---- There are " + testCache.getSize() +
                           " elements in the cache");

        Assert.assertEquals("The size of cache is wrong. ", size, testCache.getSize());

        //
        // Check to see if we can find everything we inserted
        //

        for (int i = size - 1; i >= 0; --i) {
            String key = Integer.toString(i);
            String object = (String) testCache.find(key);
            System.out.println("Found (" + key + ", " + object + ")");
            Assert.assertSame("The object found isn't correct. ", objectList[i], object);
            testCache.unpin(key);
        }

        System.out.println("---- There are still " + testCache.getSize() +
                           " elements in the cache");

        Assert.assertEquals("The size of cache is wrong. ", size, testCache.getSize());

        //
        // Look for stuff we haven't inserted yet
        //

        for (int i = bigSize - 1; i >= size; --i) {
            String key = Integer.toString(i);
            String object = (String) testCache.find(key);
            System.out.println("Found (" + key + ", " + object + ")");
            Assert.assertNull("Found an object that shouldn't be inserted yet. ", object);
        }

        System.out.println("---- There are still " + testCache.getSize() +
                           " elements in the cache");

        Assert.assertEquals("The size of cache is wrong. ", size, testCache.getSize());

        //
        // Stuff more into the cache to exceed the hard limit
        //

        for (int i = size; i < bigSize; ++i) {
            String key = Integer.toString(i);
            String object = format.format(new Date());
            System.out.println("Inserting (" + key + ", " + object + ")");
            testCache.insert(key, object);
            testCache.unpin(key);
        }

        System.out.println("---- There are now " + testCache.getSize() +
                           " elements in the cache");

        Assert.assertEquals("The size of cache is wrong. ", bigSize, testCache.getSize());

        //
        // Wait and see if LRU evicts any objects
        //

        ev.start();

        int iterations = 0;
        do {
            // We earlier specified the cache's soft limit (which is the only limit we use) to be 15 objects. 
            // We filled the cache with 30 objects. The backgroundLru strategy of evicting gets rid of the
            // minimum amount needed for the limit to not be reached meaning that we need to get rid of 16 objects. 
            // However, we also specified the discard threshold of an object to be 60 which means that an objects have to 
            // age 60 sweeps before it is evicted. In order to evict faster, as the number of sweeps increase, 
            // we decrease the discard threshold. It takes 31 sweeps to decrease the threshold to 30 which means that all 
            // objects have aged enough to be evicted and we can get rid of the 16 objects we need to evict. 
            // Note that we have configured the sweep interval to happen every second (1000 ms). Since it takes 31 sweeps
            // to check and evict the appropriate objects which happen every second totaling 31 iterations + 1 for 
            // the final check (32 seconds). To accommodate for hardware problems, we'll double that amount 
            // and make sure the loop won't get stuck beyond that. 
            Assert.assertTrue("Maximum iterations exceeded.", ++iterations < 64);
            System.out.println("Sleeping...");
            Thread.sleep(1000);
            System.out.println(">>>> There are now " + testCache.getSize() +
                               " elements in the cache");
        } while (testCache.getSize() > softLimit);

        //
        // Let's see what we've got left...
        //

        int emptySpaces = 0;
        for (int i = 0; i < bigSize; ++i) {
            String key = Integer.toString(i);
            String object = (String) testCache.find(key);
            System.out.println("Found (" + key + ", " + object + ")");
            if (object == null) {
                emptySpaces++;
            }
        }

        Assert.assertEquals("The cache size is inconsistent. ", testCache.getSize(), bigSize - emptySpaces);

        System.out.println("---- There are " + testCache.getSize() +
                           " elements in the cache");
    }

    @Test
    @Ignore
    public void testSetPreferredMaxSize() throws Exception
    {
        int bigSize = 30;

        int smallSize = 15;

        Cache testCache = new Cache("TestCache", bigSize, false);

        final ScheduledExecutorService scheduledExecutorService =
                        Executors.newScheduledThreadPool(1);

        BackgroundLruEvictionStrategy ev =
                        new BackgroundLruEvictionStrategy(testCache, bigSize, 3000, scheduledExecutorService, scheduledExecutorService);
        testCache.setEvictionStrategy(ev);

        // Initially the preferred max size is the size specified at the creation of the cache
        Assert.assertEquals(bigSize, ev.getPreferredMaxSize());

        // Set the preferred size to be something else
        testCache.setCachePreferredMaxSize(smallSize);

        // Preferred size is still original size - this gets updated every time the eviction 
        // strategy wakes up.
        Assert.assertEquals(bigSize, ev.getPreferredMaxSize());

        ev.start();
        Thread.sleep(3500);

        // We slept past the sweep interval time, and now see the update
        Assert.assertEquals(smallSize, ev.getPreferredMaxSize());
    }

    @Test
    @Ignore
    public void testSetSweepInterval() throws Exception
    {
        int bigSize = 30;

        int smallSize = 15;

        long initSweepInterval = 3000;
        long longSweepInterval = 15000;

        Cache testCache = new Cache("TestCache", bigSize, false);

        final ScheduledExecutorService scheduledExecutorService =
                        Executors.newScheduledThreadPool(1);

        // Set the initial sweep interval to be small
        BackgroundLruEvictionStrategy ev =
                        new BackgroundLruEvictionStrategy(testCache, bigSize, initSweepInterval, scheduledExecutorService, scheduledExecutorService);
        testCache.setEvictionStrategy(ev);

        // Change the preferred cache size 
        testCache.setCachePreferredMaxSize(smallSize);

        ev.start();
        Thread.sleep(3500);

        // After a sleep of 3.5 seconds, the eviction strategy should have woken up
        // and thus re-set the preferred cache size
        Assert.assertEquals(smallSize, ev.getPreferredMaxSize());

        // Set new values to be updated next time around
        testCache.setSweepInterval(longSweepInterval);
        testCache.setCachePreferredMaxSize(bigSize);

        Thread.sleep(3500);

        // Old sweep interval was still in effect, so observe that the cache size
        // was indeed modified
        Assert.assertEquals(bigSize, ev.getPreferredMaxSize());

        //Change the cache size again and see when it is updated
        testCache.setCachePreferredMaxSize(smallSize);

        Thread.sleep(5000);

        // After a sleep of 5s, the eviction strategy has not woken up yet,
        // so the cache size should still be at the bigSize
        Assert.assertEquals(bigSize, ev.getPreferredMaxSize());

        Thread.sleep(12000);

        // Sleep past the sweep point, and observe that the cache size did indeed
        // update after this prolonged sweep interval
        Assert.assertEquals(smallSize, ev.getPreferredMaxSize());
    }
}
