/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.threading.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import test.common.SharedOutputManager;

/**
 * Tests functionally in the ThreadPoolController
 */
public class ThreadPoolControllerTest {
    @Rule
    public SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    private final ControlledAccessBlockingQueue workQueue = new ControlledAccessBlockingQueue(5);
    ThreadPoolExecutor pool = new ThreadPoolExecutor(1, 1, 60, TimeUnit.SECONDS, workQueue);
    private CountDownLatch initialTaskLatch;

    @Before
    public void setUp() throws Exception {
        initialTaskLatch = new CountDownLatch(2);
        pool.execute(new Runnable() {

            @Override
            public void run() {
                System.out.println("Running to ensure at least one thread is created in the pool");
                initialTaskLatch.countDown();
            }
        });
        pool.execute(new Runnable() {

            @Override
            public void run() {
                System.out.println("Running to ensure at least one task is put on the queue");
                initialTaskLatch.countDown();
            }
        });
        Thread.sleep(1000);
        System.out.println("have not yet allowed access");
        workQueue.allowAccess();
        System.out.println("allowed access");
    }

    /**
     * This test guards against regression of the case where a customer sets core and max threads to
     * the same value, and then an incoming request gets put on the BlockingQueue just prior to the
     * resolveHang method checking that (1) the current task count is equal to the previous task count,
     * and (2) that the BlockingQueue is not empty. If the thread pool was previously idle and that
     * incoming request came in at just the wrong time (i.e. in time for the resolveHang method to
     * determine that the BlockingQueue is not empty, but before a thread in the pool has time to
     * work it), then we previously printed the hung threads warning message, even though the pool
     * is just fine. This test ensures that we do not print that message in this situation.
     */
    @Test
    public void testResolveHang_noFalsePositive() throws Exception {
        //outputMgr.captureStreams();
        ExecutorServiceImpl mockExecutorServiceImpl = new ExecutorServiceImpl();

        ThreadPoolController tpc = mockExecutorServiceImpl.threadPoolController;

        tpc.activate(pool);
        tpc.timer.cancel(); // we want to manually control the thread pool!  :)
        tpc.consecutiveIdleCount = 0;

        assertTrue("Pre-condition failed - initial task intended to create a single thread failed", initialTaskLatch.await(10, TimeUnit.SECONDS));

        // when the 2nd task queued to the pool releases "initialTaskLatch", the "await" call on the previous line completes and this
        // thread continues executing, but the task that released "initialTaskLatch" didn't necessarily finish (exit its run method) yet,
        // so let's wait up to 5 seconds for the completed task count to get updated
        for (int waitIntervals = 100; waitIntervals > 0 && pool.getCompletedTaskCount() != 2; waitIntervals--) {
            Thread.sleep(50);
        }
        assertEquals("Pre-condition failed - pool completed task count does not show that the 2 initial tasks have completed", 2, pool.getCompletedTaskCount());

        assertEquals("Unexpected exit from evaluateInterval()", "", tpc.evaluateInterval());
        workQueue.add(new Runnable() {

            @Override
            public void run() {
                System.out.println("Dummy task - added to the BlockingQueue just before the evaluateInterval call, " +
                                   "and not executed until after we've determined that the BlockingQueue is not empty.");
            }
        });

        System.out.println(tpc.evaluateInterval());
        tpc.consecutiveIdleCount = 0;

        // Prior to this change, this is the message we would log -- now we check that we do not log it,
        // since this is not a real hang condition.
        assertFalse("Unexpected warning logged", outputMgr.checkForMessages("unbreakableExecutorHang"));

        workQueue.allowAccess();
        assertEquals("Unexpected exit from evaluateInterval()", "", tpc.evaluateInterval());

    }
}