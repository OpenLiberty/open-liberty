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
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Resource;
import javax.enterprise.concurrent.AbortedException;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.concurrent.ManagedScheduledExecutorService;
import javax.servlet.annotation.WebServlet;
import javax.transaction.TransactionSynchronizationRegistry;
import javax.transaction.UserTransaction;

import org.junit.After;
import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/ConcurrentPolicyFATServlet")
public class ConcurrentPolicyFATServlet extends FATServlet {
    // Maximum number of nanoseconds to wait for a task to finish.
    static final long TIMEOUT_NS = TimeUnit.MINUTES.toNanos(1);

    // Constants for ManagedTaskListener events, to be used as array indices
    static final int SUBMITTED = 0, ABORTED = 1, STARTING = 2, DONE = 3;

    // Futures to cancel between tests, to reduce the chance of failures in one test method interfering with others
    private final Set<Future<?>> cancelAfterTest = Collections.newSetFromMap(new ConcurrentHashMap<Future<?>, Boolean>());

    // max: 1 + caller threads; maxQueue: 1; wait: 2m and abort
    @Resource(lookup = "java:comp/DefaultManagedExecutorService")
    private ManagedExecutorService defaultExecutor;

    // max: 4; maxQueue: 1; wait: 0 and submitter runs requiring permit
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
     * Utility method to validate information recorded by TaskListener for a task that aborts and is rejected upon submit.
     */
    private void assertRejected(ManagedExecutorService executor, Object task, TaskListener listener, Class<? extends Throwable> causeClass,
                                String causeMessagePrefix) throws Exception {
        assertTrue(listener.latch[SUBMITTED].await(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        Future<?> future = listener.future[SUBMITTED]; // Because submit is unsuccessful, the only visibility of the Future is in the ManagedTaskListener events.
        assertNotNull(future);
        assertEquals(executor, listener.executor[SUBMITTED]);
        assertEquals(task, listener.task[SUBMITTED]);
        assertFalse(listener.isCancelled[SUBMITTED]);
        assertFalse(listener.isDone[SUBMITTED]);
        if (listener.failure[SUBMITTED] != null)
            throw new Exception("Unexpected failure, see cause", listener.failure[SUBMITTED]);

        assertTrue(listener.latch[ABORTED].await(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertEquals(future, listener.future[ABORTED]);
        assertEquals(executor, listener.executor[ABORTED]);
        assertEquals(task, listener.task[ABORTED]);
        assertNotNull(listener.exception[ABORTED]);
        if (!(listener.exception[ABORTED] instanceof AbortedException))
            throw new Exception("Unexpected exception for taskAborted, see cause", listener.exception[ABORTED]);
        if (causeClass == null)
            assertNull(listener.exception[ABORTED].getCause());
        else {
            Throwable cause = listener.exception[ABORTED].getCause();
            if (cause == null || !causeClass.isAssignableFrom(cause.getClass()) ||
                causeMessagePrefix != null && (cause.getMessage() == null || !cause.getMessage().startsWith(causeMessagePrefix)))
                throw new Exception("Unexpected or missing cause of AbortedException. See chained exceptions", listener.exception[ABORTED]);
        }
        assertFalse(listener.isCancelled[ABORTED]);
        assertTrue(listener.isDone[ABORTED]);

        if (listener.doGet[ABORTED]) {
            Throwable failure = listener.failure[ABORTED];
            if (!(failure instanceof AbortedException))
                throw new Exception("Unexpected or missing failure. See chained exceptions", failure);
            Throwable cause = failure.getCause();
            if (cause == null || !causeClass.isAssignableFrom(cause.getClass()) ||
                causeMessagePrefix != null && (cause.getMessage() == null || !cause.getMessage().startsWith(causeMessagePrefix)))
                throw new Exception("Unexpected or missing cause of AbortedException. See chained exceptions", failure);
        } else {
            if (listener.failure[ABORTED] != null)
                throw new Exception("Unexpected failure, see cause", listener.failure[ABORTED]);
        }

        assertTrue(listener.latch[DONE].await(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertEquals(future, listener.future[DONE]);
        assertEquals(executor, listener.executor[DONE]);
        assertEquals(task, listener.task[DONE]);
        assertNotNull(listener.exception[DONE]);
        if (!(causeClass.isAssignableFrom(listener.exception[DONE].getClass())))
            throw new Exception("Unexpected or missing exception for taskDone. See chained exception", listener.exception[DONE]);
        assertFalse(listener.isCancelled[DONE]);
        assertTrue(listener.isDone[DONE]);

        if (listener.doGet[DONE]) {
            Throwable failure = listener.failure[DONE];
            if (!(failure instanceof AbortedException))
                throw new Exception("Unexpected or missing failure. See chained exceptions", failure);
            Throwable cause = failure.getCause();
            if (cause == null || !causeClass.isAssignableFrom(cause.getClass()) ||
                causeMessagePrefix != null && (cause.getMessage() == null || !cause.getMessage().startsWith(causeMessagePrefix)))
                throw new Exception("Unexpected or missing cause of AbortedException. See chained exceptions", failure);
        } else {
            if (listener.failure[DONE] != null)
                throw new Exception("Unexpected failure, see cause", listener.failure[DONE]);
        }

        assertFalse(listener.invoked[STARTING]);
    }

    /**
     * Utility method to validate information recorded by TaskListener for a successful result.
     */
    private <T> void assertSuccess(ManagedExecutorService executor, Future<T> future, Object task, TaskListener listener, T... validResults) throws Exception {
        assertTrue(listener.latch[SUBMITTED].await(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertEquals(future, listener.future[SUBMITTED]);
        assertEquals(executor, listener.executor[SUBMITTED]);
        assertEquals(task, listener.task[SUBMITTED]);
        assertFalse(listener.isCancelled[SUBMITTED]);
        assertFalse(listener.isDone[SUBMITTED]);
        if (listener.failure[SUBMITTED] != null)
            throw new Exception("Unexpected failure, see cause", listener.failure[SUBMITTED]);

        assertTrue(listener.latch[STARTING].await(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertEquals(future, listener.future[STARTING]);
        assertEquals(executor, listener.executor[STARTING]);
        assertEquals(task, listener.task[STARTING]);
        assertFalse(listener.isCancelled[STARTING]);
        assertFalse(listener.isDone[STARTING]);
        if (listener.failure[STARTING] != null)
            throw new Exception("Unexpected failure, see cause", listener.failure[STARTING]);

        assertTrue(listener.latch[DONE].await(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertEquals(future, listener.future[DONE]);
        assertEquals(executor, listener.executor[DONE]);
        assertEquals(task, listener.task[DONE]);
        assertNull(listener.exception[DONE]);
        assertFalse(listener.isCancelled[DONE]);
        assertTrue(listener.isDone[DONE]);
        if (listener.failure[DONE] != null)
            throw new Exception("Unexpected failure, see cause", listener.failure[DONE]);

        assertEquals(listener.result[DONE], future.get(1, TimeUnit.NANOSECONDS));

        boolean hasValidResult = false;
        for (T valid : validResults)
            hasValidResult |= valid == null && listener.result[DONE] == null || valid.equals(listener.result[DONE]);
        assertTrue("unexpected: " + listener.result[DONE], hasValidResult);

        assertFalse(listener.invoked[ABORTED]);
    }

    /**
     * Cancel futures that are still incomplete when tests methods end, in order to prevent long-running tasks
     * and queued tasks from interfering with subsequent tests.
     * Ideally, there should never be any futures left incomplete after successful execution of a test method,
     * and this should only be needed for when test methods fail and cannot easily clean up.
     */
    @After
    void cancelFuturesAfterTest() {
        int count = 0;
        for (Iterator<Future<?>> it = cancelAfterTest.iterator(); it.hasNext();) {
            Future<?> f = it.next();
            if (!f.isDone() && f.cancel(true))
                count++;
        }
        if (count > 0)
            System.out.println("Canceled " + count + " futures after previous test");
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
     * Submit a task when the queue is at capacity. Expect the submit to be aborted, with the proper exception raised and ManagedTaskListener taskAborted signal sent.
     */
    @Test
    public void testAbortWhenQueueFull() throws Exception {
        // Use up maxConcurrency of 1
        CountingTask blockerTask = new CountingTask(null, null, new CountDownLatch(1), new CountDownLatch(1));
        Future<Integer> blockerTaskFuture = executor1.submit(blockerTask);
        cancelAfterTest.add(blockerTaskFuture);
        assertTrue(blockerTask.beginLatch.await(TIMEOUT_NS, TimeUnit.NANOSECONDS));

        // Use up maxQueueSize of 1
        CountingTask queuedTask = new CountingTask(null, null, null, null); // TODO add a listener and test taskAborted on cancel from queue
        Future<Integer> queuedTaskFuture = executor1.submit(queuedTask);
        cancelAfterTest.add(queuedTaskFuture);

        // Submit task that aborts due to full queue
        CountingTask abortedTask = new CountingTask(null, new TaskListener().doLookup("java:module/env/concurrent/executor2ref", ABORTED).doGet(true, ABORTED), null, null);
        try {
            Future<Integer> abortedTaskFuture = executor1.submit(abortedTask);
            cancelAfterTest.add(abortedTaskFuture);
            fail("Unexpectedly allowed submit: " + abortedTaskFuture);
        } catch (RejectedExecutionException x) {
            if (!x.getMessage().startsWith("CWWKE1201E"))
                throw x;
        }

        assertRejected(executor1, abortedTask, (TaskListener) abortedTask.listener, RejectedExecutionException.class, "CWWKE1201E");

        assertEquals(scheduledExecutor2, ((TaskListener) abortedTask.listener).resultOfLookup[ABORTED]);

        cancelAfterTest.remove(queuedTaskFuture);
        assertTrue(queuedTaskFuture.cancel(false));

        cancelAfterTest.remove(blockerTaskFuture);
        assertTrue(blockerTaskFuture.cancel(true));
    }

    /**
     * Submit a task when the queue is at capacity. Interrupt it before maxWaitForEnqueue is exceeded. Expect the submit to be aborted,
     * with the proper exception raised and ManagedTaskListener taskAborted signal sent.
     */
    @Test
    public void testAbortWhenQueueWaitInterrupted() throws Exception {
        // Use up maxConcurrency of 1
        CountingTask blockerTask = new CountingTask(null, null, new CountDownLatch(1), new CountDownLatch(1));
        Future<Integer> blockerTaskFuture = defaultExecutor.submit(blockerTask);
        cancelAfterTest.add(blockerTaskFuture);
        assertTrue(blockerTask.beginLatch.await(TIMEOUT_NS, TimeUnit.NANOSECONDS));

        // Use up maxQueueSize of 1
        CountingTask queuedTask = new CountingTask(null, null, null, null);
        Future<Integer> queuedTaskFuture = defaultExecutor.submit(queuedTask);
        cancelAfterTest.add(queuedTaskFuture);

        // Submit task that aborts due to interrupted wait
        CountingTask abortedTask = new CountingTask(null, new TaskListener().doLookup("java:comp/env/concurrent/executor1ref", ABORTED).doGet(true, ABORTED), null, null);
        Thread.currentThread().interrupt();
        try {
            Future<Integer> abortedTaskFuture = defaultExecutor.submit(abortedTask);
            cancelAfterTest.add(abortedTaskFuture);
            fail("Unexpectedly allowed submit: " + abortedTaskFuture);
        } catch (RejectedExecutionException x) {
            if (!(x.getCause() instanceof InterruptedException))
                throw x;
        }

        assertRejected(defaultExecutor, abortedTask, (TaskListener) abortedTask.listener, InterruptedException.class, null);

        assertEquals(executor1, ((TaskListener) abortedTask.listener).resultOfLookup[ABORTED]);

        cancelAfterTest.remove(queuedTaskFuture);
        assertTrue(queuedTaskFuture.cancel(false));

        cancelAfterTest.remove(blockerTaskFuture);
        assertTrue(blockerTaskFuture.cancel(true));
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

    /**
     * When runIfQueueFull is true, it should be possible to submit maxConcurrency number of tasks, even if the maxQueueSize is less
     * than maxConcurrency because tasks that cannot be enqueued will run on the submitter's thread.
     */
    @Test
    public void testRunWhenQueueFull() throws Exception {
        CountingTask[] tasks = new CountingTask[4]; // maxConcurrency
        for (int i = 0; i < 4; i++) {
            TaskListener listener = i == 0 ? null : new TaskListener().doLookup("java:module/env/concurrent/executor2ref", STARTING, DONE);
            tasks[i] = new CountingTask(null, listener, null, null);
        }

        // Occupy the global executor with some other work to increase the chance that tasks submitted to defaultScheduledExecutor
        // will be unable to queue and run on the submitter's thread.
        CountingTask blockerTask1 = new CountingTask(null, null, new CountDownLatch(1), new CountDownLatch(1));
        CountingTask blockerTask2 = new CountingTask(null, null, new CountDownLatch(1), new CountDownLatch(1));
        Future<Integer> blockerTask1Future = scheduledExecutor2.submit(blockerTask1);
        cancelAfterTest.add(blockerTask1Future);
        Future<Integer> blockerTask2Future = scheduledExecutor2.submit(blockerTask2);
        cancelAfterTest.add(blockerTask2Future);

        // Submit as many tasks as we have maxConcurrency, even though maxQueueSize is only 1
        List<Future<Integer>> futures = new ArrayList<Future<Integer>>(4);
        for (int i = 0; i < 4; i++) {
            Future<Integer> future = defaultScheduledExecutor.submit(tasks[i]);
            cancelAfterTest.add(future);
            futures.add(future);
        }

        // Any tasks that ran on the current thread should already report being done.
        long curThreadId = Thread.currentThread().getId();
        if (((TaskListener) tasks[1].listener).threadId[STARTING] == curThreadId)
            assertTrue(futures.get(1).isDone());
        if (((TaskListener) tasks[2].listener).threadId[STARTING] == curThreadId)
            assertTrue(futures.get(2).isDone());
        if (((TaskListener) tasks[3].listener).threadId[STARTING] == curThreadId)
            assertTrue(futures.get(3).isDone());

        TaskListener listener;
        Future<Integer> future = futures.get(0);
        assertEquals(Integer.valueOf(1), future.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertTrue(future.isDone());
        assertFalse(future.isCancelled());
        cancelAfterTest.remove(future);

        assertSuccess(defaultScheduledExecutor, future = futures.get(1), tasks[1], listener = (TaskListener) tasks[1].listener, 1);
        assertEquals(scheduledExecutor2, listener.resultOfLookup[STARTING]);
        assertEquals(scheduledExecutor2, listener.resultOfLookup[DONE]);
        cancelAfterTest.remove(future);

        assertSuccess(defaultScheduledExecutor, future = futures.get(2), tasks[2], listener = (TaskListener) tasks[2].listener, 1);
        assertEquals(scheduledExecutor2, listener.resultOfLookup[STARTING]);
        assertEquals(scheduledExecutor2, listener.resultOfLookup[DONE]);
        cancelAfterTest.remove(future);

        assertSuccess(defaultScheduledExecutor, future = futures.get(3), tasks[3], listener = (TaskListener) tasks[3].listener, 1);
        assertEquals(scheduledExecutor2, listener.resultOfLookup[STARTING]);
        assertEquals(scheduledExecutor2, listener.resultOfLookup[DONE]);
        cancelAfterTest.remove(future);

        cancelAfterTest.remove(blockerTask1Future);
        assertTrue(blockerTask1Future.cancel(true));

        cancelAfterTest.remove(blockerTask2Future);
        assertTrue(blockerTask2Future.cancel(true));
    }
}
