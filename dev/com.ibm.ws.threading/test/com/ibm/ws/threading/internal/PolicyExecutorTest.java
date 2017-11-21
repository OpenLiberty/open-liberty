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
package com.ibm.ws.threading.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.ibm.ws.threading.PolicyExecutor;
import com.ibm.ws.threading.PolicyExecutorProvider;

import test.common.SharedOutputManager;

/**
 * Unit tests for policy executor.
 */
public class PolicyExecutorTest {
    static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("*=info:ConcurrencyPolicy=all:*=all");
    @Rule
    public TestRule managerRule = outputMgr;

    // Maximum number of nanoseconds to wait for a task to finish.
    private static final long TIMEOUT_NS = TimeUnit.MINUTES.toNanos(1);

    PolicyExecutorProvider provider = new PolicyExecutorProvider();

    static class CommonTask implements Callable<Integer> {
        private final CountDownLatch beginLatch;
        private final CountDownLatch continueLatch;
        private final AtomicInteger counter;
        private final String name;

        CommonTask(String name, CountDownLatch beginLatch, CountDownLatch continueLatch, AtomicInteger counter) {
            this.beginLatch = beginLatch == null ? new CountDownLatch(1) : beginLatch;
            this.continueLatch = continueLatch;
            this.counter = counter == null ? new AtomicInteger(0) : counter;
            this.name = name;
        }

        @Override
        public Integer call() throws Exception {
            System.out.println(this + " > call " + name);
            beginLatch.countDown();
            int result = continueLatch == null || continueLatch.await(TIMEOUT_NS * 5, TimeUnit.NANOSECONDS) ? counter.incrementAndGet() : -1;
            System.out.println(this + " < call " + name + " " + result);
            return result;
        }
    }

    // Verify that tasks expedited by policy executors are executed ahead of normal tasks.
    @Test
    public void testExpedite() throws Exception {
        ExecutorServiceImpl globalExecutor = new ExecutorServiceImpl();
        Map<String, Object> globalExecutorConfig = new HashMap<String, Object>();
        globalExecutorConfig.put("coreThreads", 1);
        globalExecutorConfig.put("maxThreads", 1);
        globalExecutorConfig.put("keepAlive", TimeUnit.MINUTES.toMillis(5));
        globalExecutor.activate(globalExecutorConfig);
        globalExecutor.threadPoolController.deactivate(); // disable autonomic tuning to guarantee maxThreads stays the same

        ConcurrentHashMap<String, PolicyExecutorImpl> policyExecutors = new ConcurrentHashMap<String, PolicyExecutorImpl>();

        PolicyExecutor executor0 = new PolicyExecutorImpl(globalExecutor, "testExpedite-0", policyExecutors).expedite(0).maxConcurrency(4).maxQueueSize(3);
        PolicyExecutor executor2 = new PolicyExecutorImpl(globalExecutor, "testExpedite-2", policyExecutors).expedite(2).maxConcurrency(4).maxQueueSize(4);

        // Share a counter between all tasks to record the order in which they run
        AtomicInteger sharedCounter = new AtomicInteger(0);

        // Use up the single thread in the global pool
        CountDownLatch blockerStartedLatch = new CountDownLatch(1);
        CountDownLatch blockerLatch = new CountDownLatch(1);
        Future<Integer> future0a = executor0.submit(new CommonTask("blocker", blockerStartedLatch, blockerLatch, sharedCounter));
        assertTrue(blockerStartedLatch.await(TIMEOUT_NS, TimeUnit.NANOSECONDS));

        // Submit 3 non-expedited tasks that will be blocked from running
        Future<Integer> future0b = executor0.submit(new CommonTask("0b", null, null, sharedCounter));
        Future<Integer> future0c = executor0.submit(new CommonTask("0c", null, null, sharedCounter));
        Future<Integer> future0d = executor0.submit(new CommonTask("0d", null, null, sharedCounter));

        // Submit 4 tasks, the first 2 of which are expedited
        // Note that when the expedited requests complete, current implementation does not replace the 2 non-expedited requests with expedited ones,
        // as would have been allowed per the expedite setting. We could change this to be more precise.
        Future<Integer> future2a = executor2.submit(new CommonTask("2a", null, null, sharedCounter));
        Future<Integer> future2b = executor2.submit(new CommonTask("2b", null, null, sharedCounter));
        Future<Integer> future2c = executor2.submit(new CommonTask("2c", null, null, sharedCounter));
        Future<Integer> future2d = executor2.submit(new CommonTask("2d", null, null, sharedCounter));

        // Release the blocker, allowing all tasks to run
        blockerLatch.countDown();

        // Verify the ordering (first the blocker task which had started, then the 2 expedited tasks, then the remaining tasks in order of submit)
        assertEquals(Integer.valueOf(1), future0a.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertEquals(Integer.valueOf(2), future2a.get(TIMEOUT_NS, TimeUnit.NANOSECONDS)); // expedited
        assertEquals(Integer.valueOf(3), future2b.get(TIMEOUT_NS, TimeUnit.NANOSECONDS)); // expedited
        assertEquals(Integer.valueOf(4), future0b.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertEquals(Integer.valueOf(5), future0c.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertEquals(Integer.valueOf(6), future0d.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertEquals(Integer.valueOf(7), future2c.get(TIMEOUT_NS, TimeUnit.NANOSECONDS)); // not expedited per current impl
        assertEquals(Integer.valueOf(8), future2d.get(TIMEOUT_NS, TimeUnit.NANOSECONDS)); // not expedited per current impl

        assertEquals(Collections.emptyList(), executor0.shutdownNow());
        assertEquals(Collections.emptyList(), executor2.shutdownNow());
        globalExecutor.deactivate(0);
    }

    // Verify that expedite can be -1 (unlimited) but otherwise not negative or greater than maximum concurrency,
    // except where maximum concurrency is -1 (unlimited).
    @Test
    public void testExpediteConfiguration() {
        PolicyExecutor executor = provider.create("testExpediteConfiguration");

        try {
            executor.expedite(-2);
            fail("Should reject negative expedite.");
        } catch (IllegalArgumentException x) {
            if (!x.getMessage().contains("-2"))
                throw x;
        }

        executor.maxConcurrency(10);
        try {
            executor.expedite(12);
            fail("Should reject expedite set greater than max concurrency.");
        } catch (IllegalArgumentException x) {
            if (!x.getMessage().contains("12"))
                throw x;
        }

        executor.expedite(10);
        try {
            executor.maxConcurrency(5);
            fail("Should reject max concurrency set less than expedite.");
        } catch (IllegalArgumentException x) {
            if (!x.getMessage().contains("5"))
                throw x;
        }

        executor.maxConcurrency(Integer.MAX_VALUE);
        executor.expedite(Integer.MAX_VALUE);

        executor.expedite(-1);
        executor.maxConcurrency(-1);

        executor.expedite(0);

        executor.shutdownNow();
    }

    /**
     * Test max concurrency configuration boundaries.
     */
    @Test
    public void testMaxConcurrencyConfiguration() {
        PolicyExecutor policy = null;
        // Test max concurrency configuration cannot be 0.
        try {
            policy = provider.create("testConcurrency0");
            policy.maxConcurrency(0);
            fail("The zero invalid max concurency configuration should have generated an exception, but it did not.");
        } catch (IllegalArgumentException eae) {
            // Expected Exception.
        }

        // Test max concurrency configuration cannot be lower than the minimum value of = -1.
        try {
            policy = provider.create("testConcurrencyNegative2");
            policy.maxConcurrency(-2);
            fail("The negative two invalid max concurency configuration should have generated an exception, but it did not.");
        } catch (IllegalArgumentException eae) {
            // Expected Exception.
        }

        // Test max concurrency configuration can be -1.
        policy = provider.create("testConcurrencyNegative1");
        policy.maxConcurrency(-1);

        // Test max concurrency configuration cannot be greater than the maximum value of = Integer.MAX_VALUE.
        try {
            policy = provider.create("testConcurrencyOverMax");
            policy.maxConcurrency(Integer.MAX_VALUE + 1);
            fail("The max int plus one max concurrency configuration should have generated an exception, but it did not.");
        } catch (IllegalArgumentException eae) {
            // Expected Exception.
        }
    }

    /**
     * Test max queue configuration boundaries.
     */
    @Test
    public void testMaxQueueConfiguration() {
        PolicyExecutor policy = null;
        // Test max queue configuration cannot be 0.
        try {
            policy = provider.create("testQueue0");
            policy.maxQueueSize(0);
            fail("The zero invalid max queue configuration should have generated an exception, but it did not.");
        } catch (IllegalArgumentException eae) {
            // Expected Exception.
        }

        // Test max queue configuration cannot be lower than the minimum value of = -1.
        try {
            policy = provider.create("testQueueNegative2");
            policy.maxQueueSize(-2);
            fail("The negative two invalid max concurency configuration should have generated an exception, but it did not.");
        } catch (IllegalArgumentException eae) {
            // Expected Exception.
        }

        // Test max queue configuration can be -1.
        policy = provider.create("testQueueNegative1");
        policy.maxQueueSize(-1);

        // Test max queue configuration cannot be greater than the maximum value of = Integer.MAX_VALUE.
        try {
            policy = provider.create("testQueueOverMax");
            policy.maxQueueSize(Integer.MAX_VALUE + 1);
            fail("The max int plus one max queue configuration should have generated an exception, but it did not.");
        } catch (IllegalArgumentException eae) {
            // Expected Exception.
        }
    }

    /**
     * Test boundaries for maxWaitForEnqueue.
     */
    @Test
    public void testMaxWaitForEnqueueConfiguration() {
        try {
            fail("should not allow negative value " + provider.create("testMaxWaitForEnqueueConfiguration-negative").maxWaitForEnqueue(-1));
        } catch (IllegalArgumentException x) {
        } // pass

        provider.create("testMaxWaitForEnqueueConfiguration-zero").maxWaitForEnqueue(0);

        provider.create("testMaxWaitForEnqueueConfiguration-max").maxWaitForEnqueue(Long.MAX_VALUE);

        PolicyExecutor executor = provider.create("testMaxWaitForEnqueueConfiguration-positive").maxWaitForEnqueue(TimeUnit.SECONDS.toMillis(20));
        executor.shutdown();

        try {
            fail("should not allow change after shutdown " + executor.maxWaitForEnqueue(TimeUnit.SECONDS.toMillis(30)));
        } catch (IllegalStateException x) {
        } // pass
    }

    /**
     * Test introspector for policy executors
     */
    @Test
    public void testIntrospector() {

        PolicyExecutorImpl exec = (PolicyExecutorImpl) provider.create("testIntrospector").expedite(5).maxConcurrency(10).maxQueueSize(3).maxWaitForEnqueue(30).startTimeout(10).runIfQueueFull(true);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintWriter pw = new PrintWriter(out);
        exec.introspect(pw);
        pw.flush();
        String output = new String(out.toByteArray());

        String expectedoutput = "PolicyExecutorProvider-testIntrospector\n"
                                + "  expedite = 5\n"
                                + "  maxConcurrency = 10 (loose)\n"
                                + "  maxQueueSize = 3\n"
                                + "  maxWaitForEnqueue = 30 ms\n"
                                + "  runIfQueueFull = true\n"
                                + "  startTimeout = 10 ms\n"
                                + "  Total Enqueued to Global Executor = 0 (0 expedited)\n"
                                + "  withheldConcurrency = 0\n"
                                + "  Remaining Queue Capacity = 3\n"
                                + "  state = ACTIVE\n"
                                + "  concurrency callback = null\n"
                                + "  late start callback = null\n"
                                + "  queue capacity callback = null\n"
                                + "  Running Task Count = 0\n"
                                + "  Running Task Futures:\n"
                                + "    None\n"
                                + "  Queued Task Futures (up to first 50):\n"
                                + "    None\n\n";

        assertEquals("The policy executor introspector output did not match the expected output.", expectedoutput, output);
    }

}
