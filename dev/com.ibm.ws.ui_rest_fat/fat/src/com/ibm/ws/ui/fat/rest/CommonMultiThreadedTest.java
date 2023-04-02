/*******************************************************************************
 * Copyright (c) 2013, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ui.fat.rest;

import static org.junit.Assert.assertNull;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.junit.After;
import org.junit.Before;

/**
 * Extends {@link CommonRESTTest} to provide convience methods for multi-threaded
 * testing.
 */
public class CommonMultiThreadedTest extends CommonRESTTest {
    protected int THREAD_LOOP_COUNT = 100;
    protected static volatile Throwable inThreadThrowable = null;

    /**
     * Initialize the multi-threaded test based on {@link CommonRESTTest}.
     * 
     * @param c The implementing class
     */
    protected CommonMultiThreadedTest(Class<?> c) {
        super(c);
    }

    /**
     * Define a LatchedRunnable, in which the latch is setable.
     */
    protected abstract class LatchedRunnable implements Runnable {
        protected CountDownLatch latch = null;

        void setCountDownLatch(CountDownLatch latch) {
            this.latch = latch;
        }
    }

    /**
     * Set the static thread Throwable detector to null.
     */
    @Before
    public void setUp() {
        inThreadThrowable = null;
    }

    /**
     * Set the static thread Throwable detector to null.
     */
    @Override
    @After
    public void tearDown() {
        if (inThreadThrowable != null) {
            assertNull("FAIL: inThreadThrowable should be null, but wasn't: " + inThreadThrowable.getMessage(),
                       inThreadThrowable);
        }
    }

    /**
     * Spawn all of the threads in the list and wait for them all to finish.
     * 
     * @param threads
     * @throws InterruptedException
     */
    protected void spawnThreads(final List<LatchedRunnable> threads) throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(threads.size());

        // Set the latch
        for (LatchedRunnable thread : threads) {
            thread.setCountDownLatch(latch);
        }

        // Spawn the threads
        for (Runnable thread : threads) {
            thread.run();
        }

        // Wait for all threads to finish
        latch.await();
    }

}
