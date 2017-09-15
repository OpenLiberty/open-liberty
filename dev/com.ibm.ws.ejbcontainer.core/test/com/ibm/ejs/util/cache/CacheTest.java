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

import java.util.concurrent.CountDownLatch;

import junit.framework.Assert;
import junit.framework.AssertionFailedError;

import org.junit.Ignore;
import org.junit.Test;

public class CacheTest
{
    private static void fail(Throwable t)
    {
        if (t != null)
        {
            if (t instanceof Error)
            {
                throw (Error) t;
            }
            if (t instanceof RuntimeException)
            {
                throw (RuntimeException) t;
            }

            AssertionFailedError err = new AssertionFailedError();
            err.initCause(t);
            throw err;
        }
    }

    @Test
    @Ignore
    public void testInsertAndRemove()
                    throws Exception
    {
        final int NUM_LOOPS = 16;
        final int NUM_THREADS = 16;
        final int NUM_BUCKETS = 16;
        final int NUM_THREAD_LOOPS = 1024;
        final int NUM_VALUES = 200;

        for (int loop = 0; loop < NUM_LOOPS; loop++)
        {
            Cache cache = new Cache("cache", NUM_BUCKETS, false);
            CountDownLatch runningLatch = new CountDownLatch(NUM_THREADS);
            CountDownLatch testLatch = new CountDownLatch(1);

            TestInsertAndRemoveRunnable[] runnables = new TestInsertAndRemoveRunnable[NUM_THREADS];
            Thread[] threads = new Thread[NUM_THREADS];
            for (int i = 0; i < NUM_THREADS; i++)
            {
                runnables[i] = new TestInsertAndRemoveRunnable(cache, runningLatch, testLatch, NUM_THREAD_LOOPS, NUM_VALUES, i * NUM_VALUES);
                threads[i] = new Thread(runnables[i]);
                threads[i].start();
            }

            runningLatch.await();
            testLatch.countDown();

            for (int i = 0; i < NUM_THREADS; i++)
            {
                threads[i].join();
            }

            for (int i = 0; i < NUM_THREADS; i++)
            {
                fail(runnables[i].ivResult);
            }
        }
    }

    private static class TestInsertAndRemoveRunnable
                    implements Runnable
    {
        private final Cache ivCache;
        private final CountDownLatch ivRunningLatch;
        private final CountDownLatch ivTestLatch;
        private final int ivNumIters;
        private final int ivNumValues;
        private final int ivValueOffset;
        Throwable ivResult;

        TestInsertAndRemoveRunnable(Cache cache, CountDownLatch runningLatch, CountDownLatch testLatch, int numIters, int numValues, int valueOffset)
        {
            ivCache = cache;
            ivRunningLatch = runningLatch;
            ivTestLatch = testLatch;
            ivNumIters = numIters;
            ivNumValues = numValues;
            ivValueOffset = valueOffset;
        }

        @Override
        public void run()
        {
            try
            {
                ivRunningLatch.countDown();
                ivTestLatch.await();

                for (int iter = 0; iter < ivNumIters; iter++)
                {
                    for (int value = ivValueOffset; value < ivValueOffset + ivNumValues; value++)
                    {
                        Assert.assertNull(ivCache.find(value));
                        ivCache.insertUnpinned(value, value);
                    }

                    for (int value = ivValueOffset; value < ivValueOffset + ivNumValues; value++)
                    {
                        Assert.assertEquals(value, ivCache.findDontPinNAdjustPinCount(value, 0));
                        Assert.assertEquals(value, ivCache.remove(value, false));
                    }

                    for (int value = ivValueOffset; value < ivValueOffset + ivNumValues; value++)
                    {
                        Assert.assertNull(ivCache.find(value));
                    }
                }
            } catch (Throwable t)
            {
                ivResult = t;
            }
        }
    }

    @Test
    @Ignore
    public void testLazyBucketCreation()
                    throws Exception
    {
        final int NUM_LOOPS = 16;
        final int NUM_BUCKETS = 16;
        final int NUM_THREADS_PER_BUCKET = 16;

        for (int loop = 0; loop < NUM_LOOPS; loop++)
        {
            Cache cache = new Cache("cache", NUM_BUCKETS, false);
            int numRunnables = NUM_BUCKETS * NUM_THREADS_PER_BUCKET;
            CountDownLatch runningLatch = new CountDownLatch(numRunnables);
            CountDownLatch testLatch = new CountDownLatch(1);

            TestLazyBucketCreationRunnable[] runnables = new TestLazyBucketCreationRunnable[numRunnables];
            Thread[] threads = new Thread[numRunnables];
            for (int i = 0; i < numRunnables; i++)
            {
                // Integers hash to their value, which satisfies
                // NUM_THREADS_PER_BUCKET.
                Object key = i;
                runnables[i] = new TestLazyBucketCreationRunnable(key, cache, runningLatch, testLatch);
                threads[i] = new Thread(runnables[i]);
                threads[i].start();
            }

            runningLatch.await();
            testLatch.countDown();

            for (int i = 0; i < numRunnables; i++)
            {
                threads[i].join();
            }

            for (int i = 0; i < numRunnables; i++)
            {
                fail(runnables[i].ivResult);
            }
        }
    }

    private static class TestLazyBucketCreationRunnable
                    implements Runnable
    {
        private final Object ivValue;
        private final Cache ivCache;
        private final CountDownLatch ivRunningLatch;
        private final CountDownLatch ivTestLatch;
        Throwable ivResult;

        TestLazyBucketCreationRunnable(Object value, Cache cache, CountDownLatch runningLatch, CountDownLatch testLatch)
        {
            ivValue = value;
            ivCache = cache;
            ivRunningLatch = runningLatch;
            ivTestLatch = testLatch;
        }

        @Override
        public void run()
        {
            try
            {
                ivRunningLatch.countDown();
                ivTestLatch.await();

                Assert.assertNull(ivCache.find(ivValue));
                ivCache.insertUnpinned(ivValue, ivValue);
                Assert.assertSame(ivValue, ivCache.find(ivValue));
                Assert.assertSame(ivValue, ivCache.remove(ivValue, true));
                Assert.assertNull(ivCache.find(ivValue));
            } catch (Throwable t)
            {
                ivResult = t;
            }
        }
    }
}
