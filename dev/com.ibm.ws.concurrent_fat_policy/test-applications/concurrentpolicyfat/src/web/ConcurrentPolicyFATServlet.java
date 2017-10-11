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
package web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Resource;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.concurrent.ManagedScheduledExecutorService;
import javax.servlet.annotation.WebServlet;
import javax.transaction.TransactionSynchronizationRegistry;
import javax.transaction.UserTransaction;

import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/ConcurrentPolicyFATServlet")
public class ConcurrentPolicyFATServlet extends FATServlet {
    // Maximum number of nanoseconds to wait for a task to finish.
    static final long TIMEOUT_NS = TimeUnit.MINUTES.toNanos(1);

    // Constants for ManagedTaskListener events, to be used as array indices
    static final int SUBMITTED = 0, ABORTED = 1, STARTING = 2, DONE = 3;

    // max: 1 + caller threads; maxQueue: 1; wait: 2m and abort
    @Resource(lookup = "java:comp/DefaultManagedExecutorService")
    private ManagedExecutorService defaultExecutor;

    // max: 2; maxQueue: 1; wait: 0 and submitter runs
    @Resource(lookup = "java:comp/DefaultManagedScheduledExecutorService")
    private ManagedScheduledExecutorService defaultScheduledExecutor;

    // max: 1; maxQueue: 1; wait: 0 and abort
    @Resource(name = "java:comp/env/concurrent/executor1ref", lookup = "concurrent/executor1")
    private ManagedExecutorService executor1;

    // max: 2 (core: 1) + caller threads; maxQueue: 2; wait: 0 and abort
    @Resource(name = "java:module/env/concurrent/executor2ref", lookup = "concurrent/scheduledExecutor2")
    private ManagedScheduledExecutorService scheduledExecutor2;

    @Resource
    private UserTransaction tran;

    @Resource(lookup = "java:comp/TransactionSynchronizationRegistry")
    private TransactionSynchronizationRegistry tranSyncRegistry;

    /**
     * Utility method to validate information recorded by TaskListener for a successful result.
     */
    private <T> void assertSuccess(ManagedExecutorService executor, Future<T> future, Object task, TaskListener listener, T... validResults) throws Exception {
        assertEquals(future, listener.future[SUBMITTED]);
        assertEquals(future, listener.future[STARTING]);
        assertEquals(future, listener.future[DONE]);

        assertEquals(executor, listener.executor[SUBMITTED]);
        assertEquals(executor, listener.executor[STARTING]);
        assertEquals(executor, listener.executor[DONE]);

        assertEquals(task, listener.task[SUBMITTED]);
        assertEquals(task, listener.task[STARTING]);
        assertEquals(task, listener.task[DONE]);

        assertNull(listener.exception[DONE]);

        if (listener.failure[SUBMITTED] != null)
            throw new Exception("Unexpected failure, see cause", listener.failure[SUBMITTED]);
        if (listener.failure[STARTING] != null)
            throw new Exception("Unexpected failure, see cause", listener.failure[STARTING]);
        if (listener.failure[DONE] != null)
            throw new Exception("Unexpected failure, see cause", listener.failure[DONE]);

        assertFalse(listener.isCancelled[SUBMITTED]);
        assertFalse(listener.isCancelled[STARTING]);
        assertFalse(listener.isCancelled[DONE]);

        assertFalse(listener.isDone[SUBMITTED]);
        assertFalse(listener.isDone[STARTING]);
        assertTrue(listener.isDone[DONE]);

        assertFalse(listener.invoked[ABORTED]);

        assertEquals(listener.result[DONE], future.get(1, TimeUnit.NANOSECONDS));

        boolean hasValidResult = false;
        for (T valid : validResults)
            hasValidResult |= valid == null && listener.result[DONE] == null || valid.equals(listener.result[DONE]);
        assertTrue("unexpected: " + listener.result[DONE], hasValidResult);
    }

    /**
     * Tests injection of managed executors and other resources used by the tests. If this doesn't work, expect many (possibly all) other tests to fail.
     */
    @Test
    public void testAllResourcesInjected() throws Exception {
        assertNotNull(defaultExecutor);
        assertNotNull(defaultScheduledExecutor);
        assertNotNull(executor1);
        assertNotNull(scheduledExecutor2);
        assertNotNull(tran);
        assertNotNull(tranSyncRegistry);
    }

    /**
     * Use untimed invokeAll to submit 3 tasks with ManagedTaskListeners that perform JNDI lookups on taskSubmitted, taskStarting, and taskDone.
     */
    @Test
    public void testInvokeAllListenersWithLookups() throws Exception {
        // With this executor, tasks can run on the invoker thread (for untimed) plus one pooled thread
        AtomicInteger counter = new AtomicInteger();
        List<CountingTask> tasks = Arrays.asList//
        (
         new CountingTask(counter, new TaskListener().doLookup("java:comp/env/concurrent/executor1ref", SUBMITTED, STARTING, DONE), null, null),
         new CountingTask(counter, new TaskListener().doLookup("java:module/env/concurrent/executor2ref", SUBMITTED, STARTING, DONE), null, null),
         new CountingTask(counter, new TaskListener().doLookup("java:comp/UserTransaction", SUBMITTED, STARTING, DONE), null, null)//
        );

        List<Future<Integer>> futures = defaultExecutor.invokeAll(tasks);
        assertEquals(3, futures.size());
        assertEquals(3, counter.get()); // all tasks ran successfully

        Future<Integer> future;
        CountingTask task;
        TaskListener listener;

        assertSuccess(defaultExecutor, futures.get(0), task = tasks.get(0), listener = (TaskListener) task.listener, 1, 2, 3);
        assertEquals(executor1, listener.resultOfLookup[SUBMITTED]);
        assertEquals(executor1, listener.resultOfLookup[STARTING]);
        assertEquals(executor1, listener.resultOfLookup[DONE]);

        assertSuccess(defaultExecutor, futures.get(1), task = tasks.get(1), listener = (TaskListener) task.listener, 1, 2, 3);
        assertEquals(scheduledExecutor2, listener.resultOfLookup[SUBMITTED]);
        assertEquals(scheduledExecutor2, listener.resultOfLookup[STARTING]);
        assertEquals(scheduledExecutor2, listener.resultOfLookup[DONE]);

        assertSuccess(defaultExecutor, futures.get(2), task = tasks.get(2), listener = (TaskListener) task.listener, 1, 2, 3);
        assertTrue("unexpected: " + listener.resultOfLookup[SUBMITTED], listener.resultOfLookup[SUBMITTED] instanceof UserTransaction);
        assertTrue("unexpected: " + listener.resultOfLookup[STARTING], listener.resultOfLookup[STARTING] instanceof UserTransaction);
        assertTrue("unexpected: " + listener.resultOfLookup[DONE], listener.resultOfLookup[DONE] instanceof UserTransaction);
    }
}
