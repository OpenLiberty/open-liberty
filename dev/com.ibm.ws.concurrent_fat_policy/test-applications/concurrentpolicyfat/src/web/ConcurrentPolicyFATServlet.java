/*******************************************************************************
 * Copyright (c) 2017,2020 IBM Corporation and others.
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
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Resource;
import javax.naming.NamingException;
import javax.servlet.annotation.WebServlet;
import javax.transaction.TransactionSynchronizationRegistry;
import javax.transaction.UserTransaction;

import org.junit.After;
import org.junit.Test;

import componenttest.annotation.ExpectedFFDC;
import componenttest.app.FATServlet;
import jakarta.enterprise.concurrent.AbortedException;
import jakarta.enterprise.concurrent.ManagedExecutorService;
import jakarta.enterprise.concurrent.ManagedScheduledExecutorService;
import jakarta.enterprise.concurrent.ManagedTask;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/ConcurrentPolicyFATServlet")
public class ConcurrentPolicyFATServlet extends FATServlet {
    // Execution property name for start timeout
    static final String START_TIMEOUT_NANOS = "com.ibm.ws.concurrent.START_TIMEOUT_NANOS";

    // Maximum number of nanoseconds to wait for a task to finish.
    static final long TIMEOUT_NS = TimeUnit.MINUTES.toNanos(1);

    // Constants for ManagedTaskListener events, to be used as array indices
    static final int SUBMITTED = 0, ABORTED = 1, STARTING = 2, DONE = 3;

    static final int MIGHT_START = -1, NOT_STARTED = 0, STARTED = 1;

    // Futures to cancel between tests, to reduce the chance of failures in one test method interfering with others
    private final Set<Future<?>> cancelAfterTest = Collections.newSetFromMap(new ConcurrentHashMap<Future<?>, Boolean>());

    // BOTH:  max: 1 + caller threads; maxQueue: 1; wait: 2m and abort
    @Resource(lookup = "java:comp/DefaultManagedExecutorService")
    private ManagedExecutorService defaultExecutor;

    // NORM:  max: 4; maxQueue: 1; wait: 0 and submitter runs requiring permit
    // LONG:  max: 1 + caller threads; maxQueue: 1; wait: 2m and abort
    @Resource(lookup = "java:comp/DefaultManagedScheduledExecutorService")
    private ManagedScheduledExecutorService defaultScheduledExecutor;

    // NORM:  max: 1; maxQueue: 1; wait: 0 and abort
    // LONG:  max: 1; maxQueue: 1; wait: 0 and abort
    @Resource(name = "java:comp/env/concurrent/executor1ref", lookup = "concurrent/executor1")
    private ManagedExecutorService executor1;

    // NORM:  max: 2 (core: 1) + caller threads; maxQueue: 2; wait: 0 and abort
    // LONG:  max: 2; maxQueue: 2; wait: 20s and abort; startTimeout: 1m
    @Resource(name = "java:app/env/concurrent/executor2ref", lookup = "concurrent/executor2")
    private ManagedExecutorService executor2;

    // NORM:  max: 2 (core: 1) + caller threads; maxQueue: 2; wait: 0 and abort
    // LONG:  max: 1 + caller threads; maxQueue: 1; wait: 2m and abort
    @Resource(name = "java:module/env/concurrent/executor2ref", lookup = "concurrent/scheduledExecutor2")
    private ManagedScheduledExecutorService scheduledExecutor2;

    @Resource
    private UserTransaction tran;

    @Resource(lookup = "java:comp/TransactionSynchronizationRegistry")
    private TransactionSynchronizationRegistry tranSyncRegistry;

    /**
     * Utility method to validate information recorded by TaskListener for a task that is canceled.
     */
    private void assertCanceled(ManagedExecutorService executor, Future<?> future, Object task, TaskListener listener, int expectStart) throws Exception {
        assertTrue(listener.latch[SUBMITTED].await(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        if (future == null)
            future = listener.future[SUBMITTED];
        else
            assertEquals(future, listener.future[SUBMITTED]);
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
        if (!(listener.exception[ABORTED] instanceof CancellationException))
            throw new Exception("Unexpected exception for taskAborted, see cause", listener.exception[ABORTED]);
        assertTrue(listener.isCancelled[ABORTED]);
        assertTrue(listener.isDone[ABORTED]);

        if (listener.doGet[ABORTED]) {
            Throwable failure = listener.failure[ABORTED];
            if (!(failure instanceof CancellationException))
                throw new Exception("Unexpected or missing failure. See chained exceptions", failure);
        } else {
            if (listener.failure[ABORTED] != null)
                throw new Exception("Unexpected failure, see cause", listener.failure[ABORTED]);
        }

        assertTrue(listener.latch[DONE].await(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertEquals(future, listener.future[DONE]);
        assertEquals(executor, listener.executor[DONE]);
        assertEquals(task, listener.task[DONE]);
        assertNull(listener.exception[DONE]);
        assertTrue(listener.isCancelled[DONE]);
        assertTrue(listener.isDone[DONE]);

        if (listener.doGet[DONE]) {
            Throwable failure = listener.failure[DONE];
            if (!(failure instanceof CancellationException))
                throw new Exception("Unexpected or missing failure. See chained exceptions", failure);
        } else {
            if (listener.failure[DONE] != null)
                throw new Exception("Unexpected failure, see cause", listener.failure[DONE]);
        }

        if (expectStart == NOT_STARTED)
            assertFalse(listener.invoked[STARTING]);
        else if (expectStart == STARTED || expectStart == MIGHT_START && listener.latch[STARTING].getCount() == 0) {
            assertTrue(listener.latch[STARTING].await(TIMEOUT_NS, TimeUnit.NANOSECONDS));
            assertEquals(future, listener.future[STARTING]);
            assertEquals(executor, listener.executor[STARTING]);
            assertEquals(task, listener.task[STARTING]);
            assertFalse(listener.isCancelled[STARTING]);
            assertFalse(listener.isDone[STARTING]);
            if (listener.failure[STARTING] != null)
                throw new Exception("Unexpected failure, see cause", listener.failure[STARTING]);
        }
    }

    /**
     * Utility method to validate information recorded by TaskListener for a task that aborts and is rejected before starting.
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
     * Utility method to reflectively invoke internal method getElapsedAcceptTime on a Future
     */
    private static long getElapsedAcceptTime(Future<?> f, TimeUnit unit) throws Exception {
        return (Long) f.getClass().getMethod("getElapsedAcceptTime", TimeUnit.class).invoke(f, unit);
    }

    /**
     * Utility method to reflectively invoke internal method getElapsedQueueTime on a Future
     */
    private static long getElapsedQueueTime(Future<?> f, TimeUnit unit) throws Exception {
        return (Long) f.getClass().getMethod("getElapsedQueueTime", TimeUnit.class).invoke(f, unit);
    }

    /**
     * Utility method to reflectively invoke internal method getElapsedRunTime on a Future
     */
    private static long getElapsedRunTime(Future<?> f, TimeUnit unit) throws Exception {
        return (Long) f.getClass().getMethod("getElapsedRunTime", TimeUnit.class).invoke(f, unit);
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
        CountingTask blockerTask1 = new CountingTask(null, null, new CountDownLatch(1), new CountDownLatch(1));
        Future<Integer> blockerTaskFuture1 = executor1.submit(blockerTask1);
        cancelAfterTest.add(blockerTaskFuture1);
        assertTrue(blockerTask1.beginLatch.await(TIMEOUT_NS, TimeUnit.NANOSECONDS));

        // Use up maxQueueSize of 1
        CountingTask queuedTask1 = new CountingTask(null, null, null, null); // TODO add a listener and test taskAborted on cancel from queue
        Future<Integer> queuedTaskFuture1 = executor1.submit(queuedTask1);
        cancelAfterTest.add(queuedTaskFuture1);

        // Submit task that aborts due to full queue
        CountingTask abortedTask1 = new CountingTask(null, new TaskListener().doLookup("java:module/env/concurrent/executor2ref", ABORTED).doGet(true, ABORTED), null, null);
        try {
            Future<Integer> abortedTaskFuture1 = executor1.submit(abortedTask1);
            cancelAfterTest.add(abortedTaskFuture1);
            fail("Unexpectedly allowed submit: " + abortedTaskFuture1);
        } catch (RejectedExecutionException x) {
            if (!x.getMessage().startsWith("CWWKE1201E"))
                throw x;
        }

        assertRejected(executor1, abortedTask1, (TaskListener) abortedTask1.listener, RejectedExecutionException.class, "CWWKE1201E");

        assertEquals(scheduledExecutor2, ((TaskListener) abortedTask1.listener).resultOfLookup[ABORTED]);

        // Even though the queue is full for normal tasks, we can still submit and run long-running tasks per the long-running policy

        // Use up long-running policy's maxConcurrency of 1
        CountingTask blockerTask2 = new CountingTask(null, null, new CountDownLatch(1), new CountDownLatch(1));
        blockerTask2.getExecutionProperties().put(ManagedTask.LONGRUNNING_HINT, Boolean.TRUE.toString());
        Future<Integer> blockerTaskFuture2 = executor1.submit(blockerTask2);
        cancelAfterTest.add(blockerTaskFuture2);
        assertTrue(blockerTask2.beginLatch.await(TIMEOUT_NS, TimeUnit.NANOSECONDS));

        // Use up long-running policy's maxQueueSize of 1
        CountingTask queuedTask2 = new CountingTask(null, null, null, null);
        queuedTask2.getExecutionProperties().put(ManagedTask.LONGRUNNING_HINT, Boolean.TRUE.toString());
        Future<Integer> queuedTaskFuture2 = executor1.submit(queuedTask2);
        cancelAfterTest.add(queuedTaskFuture2);

        // Allow the normal policy tasks to complete:
        cancelAfterTest.remove(queuedTaskFuture1);
        assertTrue(queuedTaskFuture1.cancel(false));

        cancelAfterTest.remove(blockerTaskFuture1);
        assertTrue(blockerTaskFuture1.cancel(true));

        // Submit long-running task that aborts due to full queue
        CountingTask abortedTask2 = new CountingTask(null, new TaskListener(), null, null);
        abortedTask2.getExecutionProperties().put(ManagedTask.LONGRUNNING_HINT, Boolean.TRUE.toString());
        try {
            Future<Integer> abortedTaskFuture2 = executor1.submit(abortedTask2);
            cancelAfterTest.add(abortedTaskFuture2);
            fail("Unexpectedly allowed submit: " + abortedTaskFuture2);
        } catch (RejectedExecutionException x) {
            if (!x.getMessage().startsWith("CWWKE1201E"))
                throw x;
        }

        assertRejected(executor1, abortedTask1, (TaskListener) abortedTask1.listener, RejectedExecutionException.class, "CWWKE1201E");

        // Allow the long-running policy tasks to complete:
        cancelAfterTest.remove(queuedTaskFuture2);
        assertTrue(queuedTaskFuture2.cancel(false));

        cancelAfterTest.remove(blockerTaskFuture2);
        assertTrue(blockerTaskFuture2.cancel(true));
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
        // Long running task goes to same policy executor because no longRunningPolicy[Ref] is configured
        abortedTask.getExecutionProperties().put(ManagedTask.LONGRUNNING_HINT, Boolean.TRUE.toString());
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
     * Attempt to submit 2 tasks via invokeAll where the second task's ManagedTaskListener fails during taskSubmitted
     * (force a NamingException by looking up an invalid name). Expect the first task, although submitted, to be canceled
     * when invokeAll is rejected.
     */
    @ExpectedFFDC("java.lang.RuntimeException")
    @Test
    public void testFailInvokeAllOnSecondSubmit() throws Exception {
        TaskListener listener0 = new TaskListener();
        TaskListener listener1 = new TaskListener().doLookup("concurrent/DoesNotExist", SUBMITTED).doRethrow(true, SUBMITTED);
        List<CountingTask> tasks = Arrays.asList(new CountingTask(null, listener0, null, null),
                                                 new CountingTask(null, listener1, null, null));

        try {
            List<Future<Integer>> futures = scheduledExecutor2.invokeAll(tasks, TIMEOUT_NS, TimeUnit.NANOSECONDS);
            fail("Should not be able to submit a group of tasks when one fails taskSubmitted. " + futures);
        } catch (RuntimeException x) {
            if (!RuntimeException.class.equals(x.getClass()) // exactly match what the ManagedTaskListener raises
                || !(x.getCause() instanceof NamingException))
                throw new Exception("Missing or unexpected cause or chained exception, see chained exceptions", x);
        }

        // First task submits successfully but is canceled before it starts.
        assertCanceled(scheduledExecutor2, null, tasks.get(0), listener0, NOT_STARTED);

        // Second task fails to submit and is immediately aborted.
        listener1.failure[SUBMITTED] = null; // failure on submit intentional, suppress to avoid issue during validation
        assertRejected(scheduledExecutor2, tasks.get(1), listener1, RuntimeException.class, "javax.naming.NameNotFoundException: concurrent/DoesNotExist");

        // invokeAll doesn't return, but we can access the Futures from the ManagedTaskListeners
        Future<?> future0 = listener0.future[SUBMITTED];
        Future<?> future1 = listener1.future[SUBMITTED];

        long time;

        assertTrue((time = getElapsedAcceptTime(future0, TimeUnit.NANOSECONDS)) + "ns", time >= 0);
        assertEquals(time, getElapsedAcceptTime(future0, TimeUnit.NANOSECONDS));
        assertTrue((time = getElapsedQueueTime(future0, TimeUnit.NANOSECONDS)) + "ns", time >= 0);
        assertEquals(time, getElapsedQueueTime(future0, TimeUnit.NANOSECONDS));
        assertEquals(0, getElapsedRunTime(future0, TimeUnit.NANOSECONDS));

        assertTrue((time = getElapsedAcceptTime(future1, TimeUnit.NANOSECONDS)) + "ns", time >= 0);
        assertEquals(time, getElapsedAcceptTime(future1, TimeUnit.NANOSECONDS));
        assertEquals(0, getElapsedQueueTime(future1, TimeUnit.NANOSECONDS));
        assertEquals(0, getElapsedRunTime(future1, TimeUnit.NANOSECONDS));
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
     * Submit normal and long-running tasks in a single request to timed invokeAll.
     */
    @Test
    public void testInvokeAllMixedPoliciesTimed() throws Exception {
        AtomicInteger counter = new AtomicInteger();

        CountingTask normalTask = new CountingTask(counter, null, null, null);
        normalTask.execProps.put(ManagedTask.LONGRUNNING_HINT, Boolean.FALSE.toString());

        CountingTask longTask = new CountingTask(counter, null, null, null);
        longTask.execProps.put(ManagedTask.LONGRUNNING_HINT, Boolean.TRUE.toString());

        List<Future<Integer>> futures = executor2.invokeAll(Arrays.asList(normalTask, longTask), TIMEOUT_NS, TimeUnit.NANOSECONDS);
        assertEquals(2, futures.size());
        assertTrue(futures.get(0).isDone());
        assertTrue(futures.get(1).isDone());
        assertFalse(futures.get(0).isCancelled());
        assertFalse(futures.get(1).isCancelled());
        assertEquals(2, counter.get()); // all have completed and incremented the counter
    }

    /**
     * Submit normal and long-running tasks in a single request via untimed invokeAll. Untimed invokeAll can run normal tasks on the current thread
     * in excess of max concurrency per the maxPolicy=loose configuration.
     */
    @Test
    public void testInvokeAllMixedPoliciesUntimed() throws Exception {
        // Use up normal policy's max concurrency of 2
        CountDownLatch blockerContinueLatch = new CountDownLatch(1);
        CountingTask blockerTask1 = new CountingTask(null, null, new CountDownLatch(1), blockerContinueLatch);
        CountingTask blockerTask2 = new CountingTask(null, null, new CountDownLatch(1), blockerContinueLatch);
        Future<Integer> blockerTask1Future = executor2.submit(blockerTask1);
        cancelAfterTest.add(blockerTask1Future);
        Future<Integer> blockerTask2Future = executor2.submit(blockerTask2);
        cancelAfterTest.add(blockerTask2Future);

        // Wait for at least one to start, which ensures we will have the queue capacity to perform the invokeAll
        assertTrue(blockerTask1.beginLatch.await(TIMEOUT_NS, TimeUnit.NANOSECONDS));

        AtomicInteger counter = new AtomicInteger();
        CountingTask normalTask1 = new CountingTask(counter, null, null, null);
        CountingTask normalTask2 = new CountingTask(counter, null, null, null);
        CountingTask longTask = new CountingTask(counter, null, null, null);
        longTask.execProps.put(ManagedTask.LONGRUNNING_HINT, Boolean.TRUE.toString());

        List<Future<Integer>> futures = executor2.invokeAll(Arrays.asList(normalTask1, longTask, normalTask2));
        assertEquals(3, futures.size());
        assertEquals(3, counter.get()); // all have completed and incremented the counter

        // Normal tasks were forced to run on same thread, due to max concurrency being used up
        long currentThreadId = Thread.currentThread().getId();
        assertEquals(currentThreadId, normalTask1.threadId);
        assertEquals(currentThreadId, normalTask2.threadId);

        // Allow the blockers to complete normally
        blockerContinueLatch.countDown();
        assertEquals(Integer.valueOf(1), blockerTask1Future.get());
        cancelAfterTest.remove(blockerTask1Future);
        assertEquals(Integer.valueOf(1), blockerTask2Future.get());
        cancelAfterTest.remove(blockerTask2Future);
    }

    /**
     * Submit a normal task and a long running task via invokeAny, where the normal policy has max concurrency used up.
     * The long running task must run and return its result from invokeAny.
     */
    @Test
    public void testInvokeAnyMixedPoliciesRunLongRunning() throws Exception {
        // Use up normal policy's max concurrency of 1
        CountingTask blockerTask = new CountingTask(null, null, new CountDownLatch(1), new CountDownLatch(1));
        Future<Integer> blockerTaskFuture = executor1.submit(blockerTask);
        cancelAfterTest.add(blockerTaskFuture);
        assertTrue(blockerTask.beginLatch.await(TIMEOUT_NS, TimeUnit.NANOSECONDS));

        CountingTask normalTask = new CountingTask(null, null, new CountDownLatch(1), null);
        normalTask.execProps.put(ManagedTask.LONGRUNNING_HINT, Boolean.FALSE.toString());

        CountingTask longRunningTask = new CountingTask(null, null, null, null);
        longRunningTask.execProps.put(ManagedTask.LONGRUNNING_HINT, Boolean.TRUE.toString());

        assertEquals(Integer.valueOf(1), executor1.invokeAny(Arrays.asList(normalTask, longRunningTask)));

        // long-running task completed
        assertEquals(1, longRunningTask.counter.get());

        // normal task didn't complete
        assertEquals(0, normalTask.counter.get());

        // normal task didn't start
        assertEquals(1, normalTask.beginLatch.getCount());

        blockerTask.continueLatch.countDown();
        assertEquals(Integer.valueOf(1), blockerTaskFuture.get());
        cancelAfterTest.remove(blockerTaskFuture);
    }

    /**
     * Submit a normal task and a long running task via invokeAny, where the long-running policy has max concurrency used up.
     * The normal task must run and return its result from invokeAny.
     */
    @Test
    public void testInvokeAnyMixedPoliciesRunNormal() throws Exception {
        // Use up long-running policy's max concurrency of 1
        CountingTask blockerTask = new CountingTask(null, null, new CountDownLatch(1), new CountDownLatch(1));
        blockerTask.execProps.put(ManagedTask.LONGRUNNING_HINT, Boolean.TRUE.toString());
        Future<Integer> blockerTaskFuture = executor1.submit(blockerTask);
        cancelAfterTest.add(blockerTaskFuture);
        assertTrue(blockerTask.beginLatch.await(TIMEOUT_NS, TimeUnit.NANOSECONDS));

        CountingTask longRunningTask = new CountingTask(null, null, new CountDownLatch(1), null);
        longRunningTask.execProps.put(ManagedTask.LONGRUNNING_HINT, Boolean.TRUE.toString());

        CountingTask normalTask = new CountingTask(null, null, null, null);
        normalTask.execProps.put(ManagedTask.LONGRUNNING_HINT, Boolean.FALSE.toString());

        assertEquals(Integer.valueOf(1), executor1.invokeAny(Arrays.asList(longRunningTask, normalTask), TIMEOUT_NS, TimeUnit.NANOSECONDS));

        // normal task completed
        assertEquals(1, normalTask.counter.get());

        // long-running task didn't complete
        assertEquals(0, longRunningTask.counter.get());

        // long-running task didn't start
        assertEquals(1, longRunningTask.beginLatch.getCount());

        blockerTask.continueLatch.countDown();
        assertEquals(Integer.valueOf(1), blockerTaskFuture.get());
        cancelAfterTest.remove(blockerTaskFuture);
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

    // Submit a task that exceeds its start timeout.  Verify that Future.get returns an AbortedException after the timeout elapses.
    @Test
    public void testStartTimeoutFutureGet() throws Exception {
        // Use up maxConcurrency of 1
        CountDownLatch blockerLatch = new CountDownLatch(1);
        CountDownLatch blockerStartedLatch = new CountDownLatch(1);
        CountingTask blockerTask = new CountingTask(null, null, blockerStartedLatch, blockerLatch);
        Future<Integer> blockerTaskFuture = defaultExecutor.submit(blockerTask);
        cancelAfterTest.add(blockerTaskFuture);
        assertTrue(blockerStartedLatch.await(TIMEOUT_NS, TimeUnit.NANOSECONDS));

        // Submit a task that will be unable to start
        TaskListener listener1 = new TaskListener();
        CountingTask task1 = new CountingTask(null, listener1, null, null);
        task1.getExecutionProperties().put(START_TIMEOUT_NANOS, Long.toString(TimeUnit.MILLISECONDS.toNanos(251)));
        Future<Integer> future1 = defaultExecutor.submit(task1);
        try {
            Integer result = future1.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
            fail("Task 1 should have aborted due to startTimeout. Instead, result is: " + result);
        } catch (AbortedException x) {
            if (x.getCause() == null || x.getCause().getMessage() == null || !x.getCause().getMessage().startsWith("CWWKE1205E"))
                throw x;
        }
        assertTrue(future1.isDone());
        assertFalse(future1.isCancelled());

        // Submit another task that will be unable to start
        TaskListener listener2 = new TaskListener();
        CountingTask task2 = new CountingTask(null, listener2, null, null);
        task2.getExecutionProperties().put(START_TIMEOUT_NANOS, Long.toString(TimeUnit.MILLISECONDS.toNanos(252)));
        Future<Integer> future2 = defaultExecutor.submit(task2);
        try {
            Integer result = future2.get();
            fail("Task 2 should have aborted due to startTimeout. Instead, result is: " + result);
        } catch (AbortedException x) {
            if (x.getCause() == null || x.getCause().getMessage() == null || !x.getCause().getMessage().startsWith("CWWKE1205E"))
                throw x;
        }
        assertTrue(future2.isDone());
        assertFalse(future2.isCancelled());

        blockerLatch.countDown();
        assertEquals(Integer.valueOf(1), blockerTaskFuture.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        cancelAfterTest.remove(blockerTaskFuture);
    }

    // Submit a task that exceeds its start timeout.  Verify that status methods on Future such as isDone are consistent with
    // an aborted task once the start timeout elapses.
    @Test
    public void testStartTimeoutFutureStatusMethods() throws Exception {
        // Use up maxConcurrency of 1
        CountDownLatch blockerLatch = new CountDownLatch(1);
        CountDownLatch blockerStartedLatch = new CountDownLatch(1);
        CountingTask blockerTask = new CountingTask(null, null, blockerStartedLatch, blockerLatch);
        Future<Integer> blockerTaskFuture = defaultExecutor.submit(blockerTask);
        cancelAfterTest.add(blockerTaskFuture);
        assertTrue(blockerStartedLatch.await(TIMEOUT_NS, TimeUnit.NANOSECONDS));

        // Submit a task that will timeout per its startTimeout. Verify that isDone returns true.
        TaskListener listener1 = new TaskListener();
        CountingTask task1 = new CountingTask(null, listener1, null, null);
        task1.getExecutionProperties().put(START_TIMEOUT_NANOS, Long.toString(TimeUnit.MILLISECONDS.toNanos(151)));
        Future<Integer> future1 = defaultExecutor.submit(task1);
        TimeUnit.MILLISECONDS.sleep(200); // let it time out
        assertTrue(future1.isDone());
        String s = future1.toString();
        assertTrue(s, s.contains("ABORTED"));

        // Submit another task that will timeout per its startTimeout. Verify it cannot be canceled because it has aborted.
        TaskListener listener2 = new TaskListener();
        CountingTask task2 = new CountingTask(null, listener2, null, null);
        task2.getExecutionProperties().put(START_TIMEOUT_NANOS, Long.toString(TimeUnit.MILLISECONDS.toNanos(152)));
        Future<Integer> future2 = defaultExecutor.submit(task2);
        TimeUnit.MILLISECONDS.sleep(200); // let it time out
        assertFalse(future2.cancel(true));
        s = future2.toString();
        assertTrue(s, s.contains("ABORTED"));

        blockerLatch.countDown();
        assertEquals(Integer.valueOf(1), blockerTaskFuture.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        cancelAfterTest.remove(blockerTaskFuture);
    }

    // Submit a group of tasks to timed invokeAll, where all tasks exceed the startTimeout.
    // Verify that invokeAll returns once the tasks time out, rather than after the larger timeout that was supplied to invokeAll.
    @Test
    public void testStartTimeoutInvokeAll() throws Exception {
        // Use up maxConcurrency of 1
        CountDownLatch blockerLatch = new CountDownLatch(1);
        CountDownLatch blockerStartedLatch = new CountDownLatch(1);
        CountingTask blockerTask = new CountingTask(null, null, blockerStartedLatch, blockerLatch);
        Future<Integer> blockerTaskFuture = defaultExecutor.submit(blockerTask);
        cancelAfterTest.add(blockerTaskFuture);
        assertTrue(blockerStartedLatch.await(TIMEOUT_NS, TimeUnit.NANOSECONDS));

        // 2 tasks with small startTimeout values
        TaskListener listener1 = new TaskListener();
        TaskListener listener2 = new TaskListener();
        CountingTask task1 = new CountingTask(null, listener1, null, null);
        CountingTask task2 = new CountingTask(null, listener2, null, null);
        task1.getExecutionProperties().put(START_TIMEOUT_NANOS, Long.toString(TimeUnit.MILLISECONDS.toNanos(301)));
        task2.getExecutionProperties().put(START_TIMEOUT_NANOS, Long.toString(TimeUnit.MILLISECONDS.toNanos(302)));
        List<CountingTask> tasks = Arrays.asList(task1, task2);

        // invoke tasks - should end after the start timeouts, well before the timeout supplied to invokeAll
        long start = System.nanoTime();
        List<Future<Integer>> futures = defaultExecutor.invokeAll(tasks, TIMEOUT_NS * 2, TimeUnit.NANOSECONDS);
        long duration = System.nanoTime() - start;
        assertTrue(duration + "ns", duration < TIMEOUT_NS);

        Future<Integer> future = futures.get(0);
        assertTrue(future.isDone());
        assertFalse(future.isCancelled());
        try {
            Integer result = future.get(0, TimeUnit.SECONDS);
            fail("Task 1 should have aborted due to startTimeout. Instead, result is: " + result);
        } catch (AbortedException x) {
            if (x.getCause() == null || x.getCause().getMessage() == null || !x.getCause().getMessage().startsWith("CWWKE1205E"))
                throw x;
        }

        future = futures.get(1);
        assertTrue(future.isDone());
        assertFalse(future.isCancelled());
        try {
            Integer result = future.get(0, TimeUnit.SECONDS);
            fail("Task 2 should have aborted due to startTimeout. Instead, result is: " + result);
        } catch (AbortedException x) {
            if (x.getCause() == null || x.getCause().getMessage() == null || !x.getCause().getMessage().startsWith("CWWKE1205E"))
                throw x;
        }

        blockerLatch.countDown();
        assertEquals(Integer.valueOf(1), blockerTaskFuture.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        cancelAfterTest.remove(blockerTaskFuture);
    }

    // Submit a group of tasks to timed invokeAny, where all tasks exceed the startTimeout.
    // Verify that invokeAny raises TimeoutException.
    @Test
    public void testStartTimeoutInvokeAny() throws Exception {
        // Use up maxConcurrency of 2
        final CountDownLatch blockerLatch = new CountDownLatch(1);
        CountDownLatch blockersStartedLatch = new CountDownLatch(2);
        CountingTask blockerTask1 = new CountingTask(null, null, blockersStartedLatch, blockerLatch);
        CountingTask blockerTask2 = new CountingTask(null, null, blockersStartedLatch, blockerLatch);
        Future<Integer> blockerTaskFuture1 = scheduledExecutor2.submit(blockerTask1);
        cancelAfterTest.add(blockerTaskFuture1);
        Future<Integer> blockerTaskFuture2 = scheduledExecutor2.submit(blockerTask2);
        cancelAfterTest.add(blockerTaskFuture2);
        assertTrue(blockersStartedLatch.await(TIMEOUT_NS, TimeUnit.NANOSECONDS));

        // 2 tasks with small startTimeout values
        final TaskListener listener1 = new TaskListener();
        final TaskListener listener2 = new TaskListener();
        CountingTask task1 = new CountingTask(null, listener1, null, null);
        CountingTask task2 = new CountingTask(null, listener2, null, null);
        task1.getExecutionProperties().put(START_TIMEOUT_NANOS, Long.toString(TimeUnit.MILLISECONDS.toNanos(201)));
        task2.getExecutionProperties().put(START_TIMEOUT_NANOS, Long.toString(TimeUnit.MILLISECONDS.toNanos(202)));
        List<CountingTask> tasks = Arrays.asList(task1, task2);

        // Another task blocks for queue time to exceed 250ms and then releases the blockerLatch so that the
        // invokeAny tasks can attempt to run after they have timed out.
        Future<Void> unblockerFuture = executor1.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                assertTrue(listener1.latch[SUBMITTED].await(TIMEOUT_NS, TimeUnit.NANOSECONDS));
                assertTrue(listener2.latch[SUBMITTED].await(TIMEOUT_NS, TimeUnit.NANOSECONDS));
                for (long start = System.nanoTime(); System.nanoTime() - start < TIMEOUT_NS; TimeUnit.MILLISECONDS.sleep(100))
                    if (getElapsedQueueTime(listener1.future[SUBMITTED], TimeUnit.NANOSECONDS) > 0 &&
                        getElapsedQueueTime(listener2.future[SUBMITTED], TimeUnit.NANOSECONDS) > 0) {
                        TimeUnit.MILLISECONDS.sleep(250); // more than enough for the start timeout to elapse for both tasks
                        blockerLatch.countDown();
                        return null;
                    }
                throw new Exception("Tasks not queued in reasonable amount of time");
            }
        });
        cancelAfterTest.add(unblockerFuture);

        // invokeAny - both tasks will time out before they can start
        try {
            Integer result = scheduledExecutor2.invokeAny(tasks, TIMEOUT_NS, TimeUnit.NANOSECONDS);
            fail("invokeAny should fail when all tasks abort due to start timeout, instead: " + result);
        } catch (AbortedException x) {
            if (x.getCause() == null || x.getCause().getMessage() == null || !x.getCause().getMessage().startsWith("CWWKE1205E"))
                throw x;
        }

        // ensure there were no failures
        unblockerFuture.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
        cancelAfterTest.remove(unblockerFuture);

        assertEquals(Integer.valueOf(1), blockerTaskFuture1.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        cancelAfterTest.remove(blockerTaskFuture1);

        assertEquals(Integer.valueOf(1), blockerTaskFuture2.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        cancelAfterTest.remove(blockerTaskFuture2);

        long runTime1 = getElapsedRunTime(blockerTaskFuture1, TimeUnit.MILLISECONDS);
        assertTrue(runTime1 + "ms", runTime1 >= 200); // because TimeUnit.MILLISECONDS.sleep(250) is not guaranteed to be precise
        long runTime2 = getElapsedRunTime(blockerTaskFuture2, TimeUnit.MILLISECONDS);
        assertTrue(runTime2 + "ms", runTime2 >= 200); // because TimeUnit.MILLISECONDS.sleep(250) is not guaranteed to be precise

        // ManagedTaskListener events should be consistent with an aborted task
        assertRejected(scheduledExecutor2, task1, listener1, IllegalStateException.class, "CWWKE1205E");
        assertRejected(scheduledExecutor2, task2, listener2, IllegalStateException.class, "CWWKE1205E");

        // Neither task reports any run time
        assertEquals(0, getElapsedRunTime(listener1.future[SUBMITTED], TimeUnit.NANOSECONDS));
        assertEquals(0, getElapsedRunTime(listener2.future[SUBMITTED], TimeUnit.NANOSECONDS));
    }
}
