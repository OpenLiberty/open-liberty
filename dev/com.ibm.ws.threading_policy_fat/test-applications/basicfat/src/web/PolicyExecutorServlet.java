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
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.Vector;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionService;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Phaser;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Resource;
import javax.servlet.ServletConfig;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.threading.PolicyExecutor;
import com.ibm.ws.threading.PolicyExecutor.MaxPolicy;
import com.ibm.ws.threading.PolicyExecutorProvider;
import com.ibm.ws.threading.PolicyTaskCallback;
import com.ibm.ws.threading.PolicyTaskFuture;
import com.ibm.ws.threading.StartTimeoutException;

import componenttest.annotation.AllowedFFDC;
import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/PolicyExecutorServlet")
public class PolicyExecutorServlet extends FATServlet {
    // Maximum number of nanoseconds to wait for a task to complete
    static final long TIMEOUT_NS = TimeUnit.MINUTES.toNanos(2);

    @Resource(lookup = "test/TestPolicyExecutorProvider")
    private PolicyExecutorProvider provider;

    // Executor that can be used when tests don't want to tie up threads from the Liberty global thread pool to perform concurrent test logic
    private ExecutorService testThreads;

    @Override
    public void destroy() {
        testThreads.shutdownNow();
    }

    @Override
    public void init(ServletConfig config) {
        testThreads = Executors.newFixedThreadPool(20);
    }

    // Await termination of executors that we have never used.
    // Result should be false before shutdown/shutdownNow, and true afterwards, with 0-sized list of canceled queued tasks.
    @Test
    public void testAwaitTerminationOfUnusedExecutor() throws Exception {
        ExecutorService executor1 = provider.create("testAwaitTerminationOfUnusedExecutor-1")
                        .maxWaitForEnqueue(TimeUnit.MINUTES.toMillis(1))
                        .runIfQueueFull(false);
        assertFalse(executor1.awaitTermination(0, TimeUnit.MINUTES));
        assertFalse(executor1.isTerminated());
        assertFalse(executor1.isShutdown());
        executor1.shutdown();
        assertTrue(executor1.awaitTermination(0, TimeUnit.MINUTES));
        assertTrue(executor1.isTerminated());
        assertTrue(executor1.isShutdown());

        ExecutorService executor2 = provider.create("testAwaitTerminationOfUnusedExecutor-2")
                        .maxWaitForEnqueue(TimeUnit.MINUTES.toMillis(2))
                        .runIfQueueFull(true);
        assertFalse(executor2.awaitTermination(0, TimeUnit.HOURS));
        assertFalse(executor2.isTerminated());
        assertFalse(executor2.isShutdown());
        assertTrue(executor2.shutdownNow().isEmpty());
        assertTrue(executor2.awaitTermination(0, TimeUnit.HOURS));
        assertTrue(executor2.isTerminated());
        assertTrue(executor2.isShutdown());

        ExecutorService executor3 = provider.create("testAwaitTerminationOfUnusedExecutor-3")
                        .maxQueueSize(3)
                        .maxWaitForEnqueue(TimeUnit.MINUTES.toMillis(3));
        assertFalse(executor3.isTerminated());
        assertFalse(executor3.isShutdown());
        executor3.shutdown();
        assertTrue(executor3.isTerminated()); // checking isTerminated after shutdown should transition the state if no tasks remain
        assertTrue(executor3.isShutdown());
        assertTrue(executor3.awaitTermination(3, TimeUnit.NANOSECONDS));

        ExecutorService executor4 = provider.create("testAwaitTerminationOfUnusedExecutor-4")
                        .maxQueueSize(4)
                        .maxWaitForEnqueue(TimeUnit.DAYS.toMillis(4));
        assertFalse(executor4.isTerminated());
        assertFalse(executor4.isShutdown());
        assertTrue(executor4.shutdownNow().isEmpty());
        assertTrue(executor4.isTerminated()); // checking isTerminated after shutdownNow should transition the state if no tasks remain
        assertTrue(executor4.isShutdown());
        assertTrue(executor4.awaitTermination(4, TimeUnit.MICROSECONDS));
    }

    // Await termination of a policy executor before asking it to shut down.
    // Submit 6 tasks such that, at the time of shutdown, 2 are running, 2 are queued, and 2 are awaiting queue positions.
    // Verify that it reports successful termination after (but not before) shutdown is requested
    // and that the running and queued tasks are allowed to complete, whereas the 2 tasks awaiting queue positions are rejected.
    @Test
    public void testAwaitTerminationWhileActiveThenShutdown() throws Exception {
        PolicyExecutor executor = provider.create("testAwaitTerminationWhileActiveThenShutdown")
                        .maxConcurrency(2)
                        .maxQueueSize(2)
                        .maxWaitForEnqueue(TimeUnit.SECONDS.toMillis(1))
                        .runIfQueueFull(false);

        Future<Boolean> terminationFuture = testThreads.submit(new TerminationAwaitTask(executor, TimeUnit.MINUTES.toNanos(5)));
        assertFalse(terminationFuture.isDone());

        CountDownLatch beginLatch = new CountDownLatch(2);
        CountDownLatch continueLatch = new CountDownLatch(1);
        CountDownTask task = new CountDownTask(beginLatch, continueLatch, TimeUnit.HOURS.toNanos(1));

        Future<Boolean> future1 = executor.submit(task); // should start, decrement the beginLatch, and block on continueLatch
        Future<Boolean> future2 = executor.submit(task); // should start, decrement the beginLatch, and block on continueLatch
        assertTrue(beginLatch.await(TIMEOUT_NS, TimeUnit.MILLISECONDS));

        Future<Boolean> future3 = executor.submit(task); // should be queued
        Future<Boolean> future4 = executor.submit(task); // should be queued

        Future<Future<Boolean>> future5 = testThreads.submit(new SubmitterTask<Boolean>(executor, task)); // should wait for queue position
        Future<Future<Boolean>> future6 = testThreads.submit(new SubmitterTask<Boolean>(executor, task)); // should wait for queue position

        assertFalse(executor.isShutdown());
        assertFalse(executor.isTerminated());
        assertFalse(terminationFuture.isDone());

        executor.shutdown();

        try {
            fail("Should not be able submit new task after shutdown: " + executor.submit(new SharedIncrementTask(), "Should not be able to submit this"));
        } catch (RejectedExecutionException x) {
        } // pass

        try {
            fail("Should not be able to complete submission of task [5] after shutdown: " + future5.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        } catch (ExecutionException x) {
            if (!(x.getCause() instanceof RejectedExecutionException))
                throw x;
        }

        try {
            fail("Should not be able to complete submission of task [6] after shutdown: " + future6.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        } catch (ExecutionException x) {
            if (!(x.getCause() instanceof RejectedExecutionException))
                throw x;
        }

        assertTrue(executor.isShutdown());
        assertFalse(executor.isTerminated());
        assertFalse(terminationFuture.isDone()); // still blocked on the continueLatch

        continueLatch.countDown();

        assertTrue(terminationFuture.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertTrue(executor.isShutdown());
        assertTrue(executor.isTerminated());
        assertTrue(terminationFuture.isDone());

        assertTrue(future1.isDone());
        assertTrue(future2.isDone());
        assertTrue(future3.isDone());
        assertTrue(future4.isDone());

        assertFalse(future1.isCancelled());
        assertFalse(future2.isCancelled());
        assertFalse(future3.isCancelled());
        assertFalse(future4.isCancelled());

        assertTrue(future1.get());
        assertTrue(future2.get());
        assertTrue(future3.get());
        assertTrue(future4.get());
    }

    // Await termination of a policy executor before asking it to shut down now.
    // Submit 6 tasks such that, at the time of shutdownNow, 2 are running, 2 are queued, and 2 are awaiting queue positions.
    // Verify that it reports successful termination after (but not before) shutdownNow is requested
    // and that the running tasks are canceled and interrupted, the 2 queued tasks are canceled, and the 2 tasks awaiting queue positions are rejected.
    @Test
    public void testAwaitTerminationWhileActiveThenShutdownNow() throws Exception {
        PolicyExecutor executor = provider.create("testAwaitTerminationWhileActiveThenShutdownNow")
                        .maxConcurrency(2)
                        .maxQueueSize(2)
                        .maxWaitForEnqueue(TimeUnit.SECONDS.toMillis(1));

        Future<Boolean> terminationFuture = testThreads.submit(new TerminationAwaitTask(executor, TimeUnit.MINUTES.toNanos(6)));
        assertFalse(terminationFuture.isDone());

        CountDownLatch beginLatch = new CountDownLatch(2);
        CountDownLatch continueLatch = new CountDownLatch(1000); // this latch will never reach 0, awaits on it will be blocked until interrupted
        CountDownTask task = new CountDownTask(beginLatch, continueLatch, TimeUnit.HOURS.toNanos(2));

        Future<Boolean> future1 = executor.submit(task); // should start, decrement the beginLatch, and block on continueLatch
        Future<Boolean> future2 = executor.submit(task); // should start, decrement the beginLatch, and block on continueLatch
        assertTrue(beginLatch.await(TIMEOUT_NS, TimeUnit.MILLISECONDS));

        Future<Boolean> future3 = executor.submit(task); // should be queued
        Future<Boolean> future4 = executor.submit(task); // should be queued

        Future<Future<Boolean>> future5 = testThreads.submit(new SubmitterTask<Boolean>(executor, task)); // should wait for queue position
        Future<Future<Boolean>> future6 = testThreads.submit(new SubmitterTask<Boolean>(executor, task)); // should wait for queue position

        assertFalse(executor.isShutdown());
        assertFalse(executor.isTerminated());
        assertFalse(terminationFuture.isDone());

        List<Runnable> canceledFromQueue = executor.shutdownNow();

        try {
            fail("Task [3] should not complete successfully after shutdownNow: " + future3.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        } catch (CancellationException x) {
        } // pass

        try {
            fail("Task [4] should not complete successfully after shutdownNow: " + future4.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        } catch (CancellationException x) {
        } // pass

        try {
            fail("Should not be able to complete submission of task [5] after shutdownNow: " + future5.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        } catch (ExecutionException x) {
            if (!(x.getCause() instanceof RejectedExecutionException))
                throw x;
        }

        try {
            fail("Should not be able to complete submission of task [6] after shutdownNow: " + future6.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        } catch (ExecutionException x) {
            if (!(x.getCause() instanceof RejectedExecutionException))
                throw x;
        }

        try {
            fail("Should not be able submit new task after shutdownNow: " + executor.submit(new SharedIncrementTask(), "Should not be able to submit this"));
        } catch (RejectedExecutionException x) {
        } // pass

        assertTrue(executor.isShutdown());

        // await termination
        assertTrue(terminationFuture.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));

        assertTrue(executor.isShutdown());
        assertTrue(executor.isTerminated());
        assertTrue(terminationFuture.isDone());

        assertTrue(future1.isDone());
        assertTrue(future2.isDone());
        assertTrue(future3.isDone());
        assertTrue(future4.isDone());

        assertTrue(future1.isCancelled());
        assertTrue(future2.isCancelled());
        assertTrue(future3.isCancelled());
        assertTrue(future4.isCancelled());

        try {
            fail("Task [1] should not complete successfully after shutdownNow: " + future1.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        } catch (CancellationException x) {
        } // pass

        try {
            fail("Task [2] should not complete successfully after shutdownNow: " + future2.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        } catch (CancellationException x) {
        } // pass

        assertEquals("List of queued tasks that were canceled upon shutdownNow: " + canceledFromQueue, 2, canceledFromQueue.size());
        assertNotNull(canceledFromQueue.get(0)); // cannot directly compare with task because we submitted as Callable and it had to be converted to new Runnable instance
        assertNotNull(canceledFromQueue.get(1));
    }

    // Await termination of a policy executor before asking it to shut down and shut down now.
    // Submit 3 tasks such that, at the time of shutdown, 1 is running, 1 is queued, and 1 is awaiting a queue position. Immediately thereafter, request shutdownNow.
    // Verify that it reports successful termination after (but not before) shutdownNow is requested
    // and that the running task is canceled and interrupted, the queued task is canceled, and the task awaiting a queue positions is rejected.
    // Verify that the list of queued tasks that were cancelled by shutdownNow includes the single queued task,
    // and verify that the caller can choose to run it after the executor has shut down and terminated.
    @Test
    public void testAwaitTerminationWhileActiveThenShutdownThenShutdownNow() throws Exception {
        PolicyExecutor executor = provider.create("testAwaitTerminationWhileActiveThenShutdownThenShutdownNow")
                        .maxConcurrency(1)
                        .maxQueueSize(1)
                        .maxWaitForEnqueue(TimeUnit.SECONDS.toMillis(1));

        Future<Boolean> terminationFuture = testThreads.submit(new TerminationAwaitTask(executor, TimeUnit.MINUTES.toNanos(7)));
        assertFalse(terminationFuture.isDone());

        CountDownLatch beginLatch = new CountDownLatch(2);
        CountDownLatch continueLatch = new CountDownLatch(1);
        CountDownTask task = new CountDownTask(beginLatch, continueLatch, TimeUnit.HOURS.toNanos(3));

        Future<Boolean> future1 = executor.submit(task); // should start, decrement the beginLatch, and block on continueLatch
        Future<Boolean> future2 = null;
        while (future2 == null)
            try {
                future2 = executor.submit(task); // waits for a queue position and is either queued or rejected. In the latter case, try again.
            } catch (RejectedExecutionException x) {
                System.out.println("Rejected submit is expected depending on how fast the previous queued item can start. Just try again. Exception was: " + x);
            }

        Future<Future<Boolean>> future3 = testThreads.submit(new SubmitterTask<Boolean>(executor, task)); // should wait for queue position

        assertFalse(executor.isShutdown());
        assertFalse(executor.isTerminated());
        assertFalse(terminationFuture.isDone());

        executor.shutdown();

        List<Runnable> canceledFromQueue = executor.shutdownNow();

        assertEquals("List of queued tasks that were canceled upon shutdownNow: " + canceledFromQueue, 1, canceledFromQueue.size());

        assertTrue(executor.isShutdown());

        try {
            fail("Should not be able submit new task after shutdownNow: " + executor.submit(new SharedIncrementTask(), "Should not be able to submit this"));
        } catch (RejectedExecutionException x) {
        } // pass

        assertTrue(executor.isShutdown());

        // await termination
        assertTrue(terminationFuture.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));

        assertTrue(executor.isShutdown());
        assertTrue(executor.isTerminated());
        assertTrue(terminationFuture.isDone());

        assertTrue(future1.isDone());
        assertTrue(future2.isDone());

        assertTrue(future1.isCancelled());
        assertTrue(future2.isCancelled());

        try {
            fail("Task [1] should not complete successfully after shutdownNow: " + future1.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        } catch (CancellationException x) {
        } // pass

        try {
            fail("Task [2] should not complete successfully after shutdownNow: " + future2.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        } catch (CancellationException x) {
        } // pass

        try {
            fail("Should not be able to complete submission of task [3] after shutdownNow: " + future3.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        } catch (ExecutionException x) {
            if (!(x.getCause() instanceof RejectedExecutionException))
                throw x;
        }

        // Originally submitted Callable was converted to Runnable, so we cannot directly compare.
        // However, we can run it and verify if it invokes the callable.
        continueLatch.countDown(); // let the Callable that we are about to run do so without blocking
        // We cannot assume the beginLatch count is 1. It could be 2 if we hit a timing window where the polling task removes the first task from the queue but shutdownNow cancels it before it can start.
        long before = beginLatch.getCount();
        canceledFromQueue.iterator().next().run();
        long after = beginLatch.getCount();
        assertEquals(before - 1, after);
    }

    // Cover basic life cycle of a policy executor service: use it to run a task, shut it down, and await termination.
    @Test
    public void testBasicLifeCycle() throws Exception {
        ExecutorService executor = provider.create("testBasicLifeCycle");

        assertFalse(executor.isShutdown());
        assertFalse(executor.isTerminated());

        SharedIncrementTask task = new SharedIncrementTask();
        Future<String> future = executor.submit(task, "Successful");
        assertEquals("Successful", future.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertEquals(1, task.count());

        assertFalse(executor.isShutdown());
        assertFalse(executor.isTerminated());

        executor.shutdown();

        assertTrue(executor.isShutdown());

        List<Runnable> canceledTasks = executor.shutdownNow();
        assertEquals(canceledTasks.toString(), 0, canceledTasks.size());

        assertTrue(executor.isShutdown());

        assertTrue(executor.awaitTermination(5, TimeUnit.MINUTES));

        assertTrue(executor.isShutdown());
        assertTrue(executor.isTerminated());
    }

    // Verify that the onEnd(aborted) callback is sent for tasks that abort after they are submitted
    // but before they start to run. This test covers a task that is aborted to due to exceeding the
    // maximum queue size and a task that is aborted due to being interrupted while waiting for a
    // queue position.
    @Test
    public void testCallbacksForAbortedTasks() throws Exception {
        PolicyExecutor executor = provider.create("testCallbacksForAbortedTasks")
                        .maxConcurrency(1)
                        .maxQueueSize(1);

        CountDownLatch blocker = new CountDownLatch(1);
        CountDownLatch blockerRunning = new CountDownLatch(1);

        // Use up maxConcurrency
        CountDownTask task1 = new CountDownTask(blockerRunning, blocker, TimeUnit.MINUTES.toNanos(20));
        Future<Boolean> blockerFuture = executor.submit(task1);
        assertTrue(blockerRunning.await(TIMEOUT_NS, TimeUnit.NANOSECONDS));

        // Use up the queue position
        Runnable task2 = new SharedIncrementTask();
        Future<?> queuedFuture = executor.submit(task2);

        // abort due to queue at capacity
        Runnable task3 = new SharedIncrementTask();
        ParameterInfoCallback callback = new ParameterInfoCallback();
        try {
            fail("Submit should fail with queue at capacity. Instead: " + executor.submit(task3, "Result that should never be returned", callback));
        } catch (RejectedExecutionException x) {
            if (!x.getMessage().startsWith("CWWKE1201E"))
                throw x;
        }

        // task successfully submits
        assertFalse(callback.isCanceled[ParameterInfoCallback.SUBMIT]);
        assertFalse(callback.isDone[ParameterInfoCallback.SUBMIT]);
        assertTrue(callback.nsAccept[ParameterInfoCallback.SUBMIT] >= 0);
        assertEquals(0, callback.nsQueue[ParameterInfoCallback.SUBMIT]);
        assertEquals(0, callback.nsRun[ParameterInfoCallback.SUBMIT]);
        assertEquals(task3, callback.task[ParameterInfoCallback.SUBMIT]);

        // task does not start
        assertNull(callback.future[ParameterInfoCallback.START]);
        assertNull(callback.task[ParameterInfoCallback.START]);

        // task is not canceled
        assertNull(callback.future[ParameterInfoCallback.CANCEL]);
        assertNull(callback.task[ParameterInfoCallback.CANCEL]);

        // task is aborted
        assertEquals(task3, callback.task[ParameterInfoCallback.END]);
        assertEquals(callback.future[ParameterInfoCallback.SUBMIT], callback.future[ParameterInfoCallback.END]);
        assertFalse(callback.isCanceled[ParameterInfoCallback.END]);
        assertTrue(callback.isDone[ParameterInfoCallback.END]);
        assertTrue(callback.nsAccept[ParameterInfoCallback.END] >= callback.nsAccept[ParameterInfoCallback.SUBMIT]);
        assertEquals(0, callback.nsQueue[ParameterInfoCallback.END]);
        assertEquals(0, callback.nsRun[ParameterInfoCallback.END]);
        assertNull(callback.startContext);
        Object result = callback.result[ParameterInfoCallback.END];
        if (!(result instanceof RejectedExecutionException))
            if (result instanceof Throwable)
                throw new Exception("Unexpected failure. See cause.", (Throwable) result);
            else
                fail("result: " + result);

        // interrupt wait for enqueue
        Callable<Integer> task4 = new SharedIncrementTask();
        executor.maxWaitForEnqueue(TimeUnit.NANOSECONDS.toMillis(TIMEOUT_NS));
        Thread.currentThread().interrupt();
        try {
            executor.submit(task4, callback = new ParameterInfoCallback());
        } catch (RejectedExecutionException x) {
            if (!(x.getCause() instanceof InterruptedException))
                throw x;
        }

        // task successfully submits
        assertFalse(callback.isCanceled[ParameterInfoCallback.SUBMIT]);
        assertFalse(callback.isDone[ParameterInfoCallback.SUBMIT]);
        assertTrue(callback.nsAccept[ParameterInfoCallback.SUBMIT] >= 0);
        assertEquals(0, callback.nsQueue[ParameterInfoCallback.SUBMIT]);
        assertEquals(0, callback.nsRun[ParameterInfoCallback.SUBMIT]);
        assertEquals(task4, callback.task[ParameterInfoCallback.SUBMIT]);

        // task does not start
        assertNull(callback.future[ParameterInfoCallback.START]);
        assertNull(callback.task[ParameterInfoCallback.START]);

        // task is not canceled
        assertNull(callback.future[ParameterInfoCallback.CANCEL]);
        assertNull(callback.task[ParameterInfoCallback.CANCEL]);

        // task is aborted
        PolicyTaskFuture<?> future = callback.future[ParameterInfoCallback.SUBMIT]; // Submit was rejected, so the only access we have to the Future is from the previous callback
        assertEquals(task4, callback.task[ParameterInfoCallback.END]);
        assertEquals(future, callback.future[ParameterInfoCallback.END]);
        assertFalse(callback.isCanceled[ParameterInfoCallback.END]);
        assertTrue(callback.isDone[ParameterInfoCallback.END]);
        long nsAccept = callback.nsAccept[ParameterInfoCallback.END];
        long musAccept = TimeUnit.NANOSECONDS.toMicros(nsAccept);
        long msAccept = TimeUnit.NANOSECONDS.toMillis(nsAccept);
        assertTrue(nsAccept >= callback.nsAccept[ParameterInfoCallback.SUBMIT]);
        assertEquals(0, callback.nsQueue[ParameterInfoCallback.END]);
        assertEquals(0, callback.nsRun[ParameterInfoCallback.END]);
        assertEquals(nsAccept, future.getElapsedAcceptTime(TimeUnit.NANOSECONDS));
        assertEquals(musAccept, future.getElapsedAcceptTime(TimeUnit.MICROSECONDS));
        assertEquals(msAccept, future.getElapsedAcceptTime(TimeUnit.MILLISECONDS));
        assertNull(callback.startContext);
        result = callback.result[ParameterInfoCallback.END];
        if (!(result instanceof RejectedExecutionException) || !(((RejectedExecutionException) result).getCause() instanceof InterruptedException))
            if (result instanceof Throwable)
                throw new Exception("Unexpected failure. See cause.", (Throwable) result);
            else
                fail("result: " + result);

        List<Runnable> canceledFromQueue = executor.shutdownNow();
        assertEquals(1, canceledFromQueue.size());
        assertEquals(task2, canceledFromQueue.get(0));
        assertTrue(queuedFuture.isCancelled());
        assertTrue(blockerFuture.isCancelled());
    }

    // Verify that it is not possible to cancel tasks that are done.
    @Test
    public void testCancelAfterDone() throws Exception {
        PolicyExecutor executor = provider.create("testCancelAfterDone");

        Future<Integer> successfulFuture = executor.submit((Callable<Integer>) new SharedIncrementTask());
        assertEquals(Integer.valueOf(1), successfulFuture.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));

        assertFalse(successfulFuture.cancel(true));
        assertFalse(successfulFuture.isCancelled());
        assertEquals(Integer.valueOf(1), successfulFuture.get(0, TimeUnit.SECONDS));
        assertTrue(successfulFuture.isDone());

        Future<Integer> unsuccessfulFuture = executor.submit((Callable<Integer>) new SharedIncrementTask(null)); // intentionally cause NullPointerException
        try {
            fail("Expecting ExecutionException. Instead: " + unsuccessfulFuture.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        } catch (ExecutionException x) {
            if (!(x.getCause() instanceof NullPointerException))
                throw x;
        }

        assertFalse(unsuccessfulFuture.cancel(true));
        assertFalse(unsuccessfulFuture.isCancelled());

        try {
            fail("Still expecting ExecutionException. Instead: " + unsuccessfulFuture.get(0, TimeUnit.SECONDS));
        } catch (ExecutionException x) {
            if (!(x.getCause() instanceof NullPointerException))
                throw x;
        }

        assertTrue(unsuccessfulFuture.isDone());

        executor.shutdown();
        assertTrue(executor.awaitTermination(TIMEOUT_NS, TimeUnit.NANOSECONDS));
    }

    // Cancel tasks during the onStart callback. Verify that tasks are canceled and do not run, implying that the onStart callback is being invoked.
    @Test
    public void testCancelOnStart() throws Exception {
        PolicyExecutor executor = provider.create("testCancelOnStart");

        AtomicInteger counter = new AtomicInteger();
        CancellationCallback cancelOnStart = new CancellationCallback("onStart", false);

        // submit
        Future<Integer> future = executor.submit((Callable<Integer>) new SharedIncrementTask(counter), cancelOnStart);
        try {
            fail("Should not be able to get result of canceled future: " + future.get());
        } catch (CancellationException x) {
        } // pass
        assertTrue(future.isCancelled());
        assertTrue(future.isDone());

        // untimed invokeAll, same thread
        List<PolicyTaskFuture<Integer>> futures = executor.invokeAll(Collections.<Callable<Integer>> singleton(new SharedIncrementTask(counter)),
                                                                     new PolicyTaskCallback[] { cancelOnStart });
        assertEquals(1, futures.size());
        future = futures.get(0);
        assertTrue(future.isCancelled());
        assertTrue(future.isDone());
        try {
            fail("Should not be able to get result of canceled untimed invokeAll future: " + future.get());
        } catch (CancellationException x) {
        } // pass

        // timed invokeAll, different thread
        futures = executor.invokeAll(Collections.<Callable<Integer>> singleton(new SharedIncrementTask(counter)),
                                     new PolicyTaskCallback[] { cancelOnStart },
                                     TIMEOUT_NS, TimeUnit.NANOSECONDS);
        assertEquals(1, futures.size());
        future = futures.get(0);
        assertTrue(future.isCancelled());
        assertTrue(future.isDone());
        try {
            fail("Should not be able to get result of canceled timed invokeAll future: " + future.get());
        } catch (CancellationException x) {
        } // pass

        // untimed invokeAny(one task)
        try {
            Integer result = executor.invokeAny(Collections.<Callable<Integer>> singleton(new SharedIncrementTask(counter)),
                                                new PolicyTaskCallback[] { cancelOnStart });
            fail("untimed invokeAny task should have canceled on start. Instead result is: " + result);
        } catch (CancellationException x) {
        } // pass

        // untimed invokeAny(tasks)
        try {
            Integer result = executor.invokeAny(Arrays.asList(new SharedIncrementTask(counter), new SharedIncrementTask(counter)),
                                                new PolicyTaskCallback[] { cancelOnStart, cancelOnStart });
            fail("untimed invokeAny tasks should have canceled on start. Instead result is: " + result);
        } catch (CancellationException x) {
        } // pass

        // timed invokeAny
        try {
            Integer result = executor.invokeAny(Collections.<Callable<Integer>> singleton(new SharedIncrementTask(counter)),
                                                new PolicyTaskCallback[] { cancelOnStart },
                                                TIMEOUT_NS, TimeUnit.NANOSECONDS);
            fail("timed invokeAny task should have canceled on start. Instead result is: " + result);
        } catch (CancellationException x) {
        } // pass

        // None of the tasks should have started
        assertEquals(0, counter.get());

        List<Runnable> canceledFromQueue = executor.shutdownNow();
        assertEquals(0, canceledFromQueue.size());
    }

    // Cancel tasks during the onStart callback, where cancel interrupts the thread of execution.
    // Verify that tasks are canceled and do not run, implying that the onStart callback is being invoked.
    @Test
    public void testCancelOnStartWithInterrupt() throws Exception {
        PolicyExecutor executor = provider.create("testCancelOnStartWithInterrupt");

        AtomicInteger counter = new AtomicInteger();
        CancellationCallback cancelWithInterruptOnStart = new CancellationCallback("onStart", true);

        Future<Integer> future;

        // untimed invokeAll, same thread
        List<PolicyTaskFuture<Integer>> futures = executor.invokeAll(Collections.<Callable<Integer>> singleton(new SharedIncrementTask(counter)),
                                                                     new PolicyTaskCallback[] { cancelWithInterruptOnStart });
        assertEquals(1, futures.size());
        future = futures.get(0);
        assertTrue(future.isCancelled());
        assertTrue(future.isDone());
        try {
            fail("Should not be able to get result of canceled untimed invokeAll future: " + future.get());
        } catch (CancellationException x) {
        } // pass

        assertTrue(Thread.interrupted()); // also clears interrupted status

        // untimed invokeAny(one task), runs on same thread
        try {
            Integer result = executor.invokeAny(Collections.<Callable<Integer>> singleton(new SharedIncrementTask(counter)),
                                                new PolicyTaskCallback[] { cancelWithInterruptOnStart });
            fail("untimed invokeAny task should have canceled on start. Instead result is: " + result);
        } catch (CancellationException x) {
        } // pass

        assertTrue(Thread.interrupted()); // also clears interrupted status

        // Reduce maxConcurrency and maxQueueSize and enable tasks to run on the submitter
        executor.maxConcurrency(1).maxQueueSize(1).runIfQueueFull(true);

        // Use up the maxConcurrency permit
        CountDownLatch blockerBeginLatch = new CountDownLatch(1);
        CountDownTask blockerTask = new CountDownTask(blockerBeginLatch, new CountDownLatch(1), TIMEOUT_NS * 2);
        Future<Boolean> blockerTaskFuture = executor.submit(blockerTask);

        // Use up the single queue position
        assertTrue(blockerBeginLatch.await(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        Future<Integer> queuedTaskFuture = executor.submit(new SharedIncrementTask(counter), 1);

        // submit, run on same thread
        future = executor.submit((Callable<Integer>) new SharedIncrementTask(counter), cancelWithInterruptOnStart);
        assertTrue(Thread.interrupted());
        assertTrue(future.isCancelled());
        assertTrue(future.isDone());
        try {
            fail("Should not be able to get result of future for submit running on the current thread: " + future.get());
        } catch (CancellationException x) {
        } // pass

        // None of the tasks should have started
        assertEquals(0, counter.get());

        assertTrue(queuedTaskFuture.cancel(false));
        assertTrue(blockerTaskFuture.cancel(true));

        List<Runnable> canceledFromQueue = executor.shutdownNow();
        assertEquals(0, canceledFromQueue.size());
    }

    // Cancel tasks during the onSubmit callback. Verify that tasks are canceled and do not run, implying that the onSubmit callback is being invoked.
    @Test
    public void testCancelOnSubmit() throws Exception {
        PolicyExecutor executor = provider.create("testCancelOnSubmit");

        AtomicInteger counter = new AtomicInteger();
        CancellationCallback cancelOnSubmit = new CancellationCallback("onSubmit", false);

        // submit
        Future<Integer> future = executor.submit(new SharedIncrementTask(counter), 1, cancelOnSubmit);
        assertTrue(future.isCancelled());
        assertTrue(future.isDone());
        try {
            fail("Should not be able to get result of canceled future: " + future.get());
        } catch (CancellationException x) {
        } // pass

        // untimed invokeAll
        List<PolicyTaskFuture<Integer>> futures = executor.invokeAll(Collections.<Callable<Integer>> singleton(new SharedIncrementTask(counter)),
                                                                     new PolicyTaskCallback[] { cancelOnSubmit });
        assertEquals(1, futures.size());
        future = futures.get(0);
        assertTrue(future.isCancelled());
        assertTrue(future.isDone());
        try {
            fail("Should not be able to get result of canceled untimed invokeAll future: " + future.get());
        } catch (CancellationException x) {
        } // pass

        // timed invokeAll
        futures = executor.invokeAll(Collections.<Callable<Integer>> singleton(new SharedIncrementTask(counter)),
                                     new PolicyTaskCallback[] { cancelOnSubmit },
                                     TIMEOUT_NS, TimeUnit.NANOSECONDS);
        assertEquals(1, futures.size());
        future = futures.get(0);
        assertTrue(future.isCancelled());
        assertTrue(future.isDone());
        try {
            fail("Should not be able to get result of canceled timed invokeAll future: " + future.get());
        } catch (CancellationException x) {
        } // pass

        // untimed invokeAny(one task)
        try {
            Integer result = executor.invokeAny(Collections.<Callable<Integer>> singleton(new SharedIncrementTask(counter)),
                                                new PolicyTaskCallback[] { cancelOnSubmit });
            fail("untimed invokeAny task should have canceled on submit. Instead result is: " + result);
        } catch (CancellationException x) {
        } // pass

        // untimed invokeAny(tasks)
        try {
            Integer result = executor.invokeAny(Arrays.asList(new SharedIncrementTask(counter), new SharedIncrementTask(counter)),
                                                new PolicyTaskCallback[] { cancelOnSubmit, cancelOnSubmit });
            fail("untimed invokeAny tasks should have canceled on submit. Instead result is: " + result);
        } catch (CancellationException x) {
        } // pass

        // timed invokeAny
        try {
            Integer result = executor.invokeAny(Collections.<Callable<Integer>> singleton(new SharedIncrementTask(counter)),
                                                new PolicyTaskCallback[] { cancelOnSubmit },
                                                TIMEOUT_NS, TimeUnit.NANOSECONDS);
            fail("timed invokeAny task should have canceled on submit. Instead result is: " + result);
        } catch (CancellationException x) {
        } // pass

        // None of the tasks should have started
        assertEquals(0, counter.get());

        List<Runnable> canceledFromQueue = executor.shutdownNow();
        assertEquals(0, canceledFromQueue.size());
    }

    // When queued tasks are canceled, it should immediately free up capacity to allow tasks waiting for enqueue to be enqueued.
    @Test
    public void testCancelQueuedTasks() throws Exception {
        PolicyExecutor executor = provider.create("testCancelQueuedTasks")
                        .maxConcurrency(1)
                        .maxQueueSize(3)
                        .maxWaitForEnqueue(TimeUnit.MINUTES.toMillis(10));

        // Use up maxConcurrency
        Future<Boolean> blockerFuture = executor.submit(new CountDownTask(new CountDownLatch(1), new CountDownLatch(1), TimeUnit.MINUTES.toNanos(30)));

        // Fill the queue
        AtomicInteger counter = new AtomicInteger();
        Future<Integer> future1 = executor.submit((Callable<Integer>) new SharedIncrementTask(counter));
        Future<Integer> future2 = executor.submit((Callable<Integer>) new SharedIncrementTask(counter));
        Future<Integer> future3 = executor.submit((Callable<Integer>) new SharedIncrementTask(counter));

        // From a separate thread, submit a task that must wait for a queue position
        ParameterInfoCallback callback4 = new ParameterInfoCallback();
        Future<Future<Integer>> ff4 = testThreads.submit(new SubmitterTask<Integer>(executor, new SharedIncrementTask(counter), callback4));

        // Use the onSubmit callback to acquire the Future before it gets returned to the submitter
        callback4.latch[ParameterInfoCallback.SUBMIT].await(TIMEOUT_NS, TimeUnit.NANOSECONDS);
        PolicyTaskFuture<?> pf4 = callback4.future[ParameterInfoCallback.SUBMIT];
        long beforeGet = pf4.getElapsedAcceptTime(TimeUnit.MILLISECONDS);
        try {
            fail("Task[4] submit should remain blocked: " + ff4.get(400, TimeUnit.MILLISECONDS));
        } catch (TimeoutException x) {
        } // pass
        long afterGet = pf4.getElapsedAcceptTime(TimeUnit.MILLISECONDS);
        assertTrue(beforeGet + "ms, " + afterGet + " ms", afterGet - beforeGet > 300);

        // Verify onStart callback hasn't been invoked either due to task still waiting to enqueue
        assertNull(callback4.future[ParameterInfoCallback.START]);

        long start = System.nanoTime();
        try {
            fail("Future for task 4 should not complete because it should be waiting to enqueue. Instead: " +
                 pf4.get(TIMEOUT_NS * 2, TimeUnit.NANOSECONDS));
        } catch (InterruptedException x) {
        } // expected
        long duration = System.nanoTime() - start;
        assertTrue(duration + "ns", duration < TIMEOUT_NS);

        // Cancel a queued task
        assertTrue(future2.cancel(false));
        assertTrue(future2.isCancelled());
        assertTrue(future2.isDone());

        // Task should be queued now
        Future<Integer> future4 = ff4.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
        assertEquals(pf4, future4); // must match the future supplied to onSubmit callback

        long before = pf4.getElapsedQueueTime(TimeUnit.MILLISECONDS);

        // From separate threads, submit more tasks that must wait for queue positions
        Future<Future<Integer>> ff5 = testThreads.submit(new SubmitterTask<Integer>(executor, new SharedIncrementTask(counter)));
        Future<Future<Integer>> ff6 = testThreads.submit(new SubmitterTask<Integer>(executor, new SharedIncrementTask(counter)));

        try {
            fail("Task[5] submit should remain blocked: " + ff5.get(400, TimeUnit.MILLISECONDS));
        } catch (TimeoutException x) {
        } // pass

        try {
            fail("Task[6] submit should remain blocked: " + ff6.get(60, TimeUnit.MILLISECONDS));
        } catch (TimeoutException x) {
        } // pass

        long after = pf4.getElapsedQueueTime(TimeUnit.MILLISECONDS);
        assertTrue(before + "ms, " + after + "ms", after - before > 400);

        // Cancel 2 queued tasks
        assertTrue(future3.cancel(false));
        assertTrue(future3.isCancelled());
        assertTrue(future3.isDone());

        assertTrue(future4.cancel(false));
        assertTrue(future4.isDone());
        assertTrue(future4.isCancelled());

        long queueTime1 = pf4.getElapsedQueueTime(TimeUnit.NANOSECONDS);
        long queueTime2 = pf4.getElapsedQueueTime(TimeUnit.NANOSECONDS);
        assertEquals(queueTime1, queueTime2);
        assertEquals(0, pf4.getElapsedRunTime(TimeUnit.NANOSECONDS));

        // Both tasks should be queued now
        Future<Integer> future5 = ff5.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
        Future<Integer> future6 = ff6.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);

        // Cancel one of them
        assertTrue(future5.cancel(false));
        assertTrue(future5.isDone());
        assertTrue(future5.isCancelled());

        // Cancel the blocker task and let the two tasks remaining in the queue start and run to completion
        assertTrue(blockerFuture.cancel(true));

        int result1 = future1.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
        int result6 = future6.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);

        assertEquals(3, result1 + result6); // task increment logic could run in either order, with values of: 1,2
        assertEquals(2, counter.get());

        assertTrue(future1.isDone());
        assertTrue(future6.isDone());
        assertFalse(future1.isCancelled());
        assertFalse(future6.isCancelled());

        // Should be possible to get the result multiple times
        assertEquals(Integer.valueOf(result1), future1.get());
        assertEquals(Integer.valueOf(result6), future6.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));

        try {
            fail("get of canceled future [2] must fail: " + future2.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        } catch (CancellationException x) {
        } // pass

        try {
            fail("get of canceled future [3] must fail: " + future3.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        } catch (CancellationException x) {
        } // pass

        try {
            fail("get of canceled future [4] must fail: " + future4.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        } catch (CancellationException x) {
        } // pass

        try {
            fail("get of canceled future [5] must fail: " + future5.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        } catch (CancellationException x) {
        } // pass
    }

    // Register concurrency callbacks with a policy executor. Verify that at most one can be registered,
    // that the most recently registered replaces any previous ones, and that the concurrency callback
    // can be unregistered by supplying null. Verify that the concurrency callback is notified when the
    // number of running tasks exceeds the threshold.
    @Test
    public void testConcurrencyCallback() throws Exception {
        PolicyExecutor executor = provider.create("testConcurrencyCallback")
                        .maxConcurrency(3);

        CountDownLatch over0Latch = new CountDownLatch(1);
        Runnable callbackOver0 = new CountDownCallback(over0Latch);
        assertNull(executor.registerConcurrencyCallback(0, callbackOver0));
        assertEquals(1, over0Latch.getCount());

        // new registration replaces previous
        CountDownLatch over1Latch = new CountDownLatch(1);
        Runnable callbackOver1 = new CountDownCallback(over1Latch);
        assertEquals(callbackOver0, executor.registerConcurrencyCallback(1, callbackOver1));
        assertEquals(1, over1Latch.getCount());

        // previously registered callback is not invoked
        CountDownLatch blocker1StartedLatch = new CountDownLatch(1);
        CountDownLatch blockerContinueLatch = new CountDownLatch(1);
        CountDownTask blocker1Task = new CountDownTask(blocker1StartedLatch, blockerContinueLatch, TimeUnit.MINUTES.toNanos(9));
        Future<Boolean> blocker1Future = executor.submit(blocker1Task);
        assertTrue(blocker1StartedLatch.await(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertEquals(1, over0Latch.getCount());
        assertEquals(1, over1Latch.getCount());

        // trigger the active callback by starting another task, such that 2 are running
        CountDownLatch blocker2StartedLatch = new CountDownLatch(1);
        CountDownTask blocker2Task = new CountDownTask(blocker2StartedLatch, blockerContinueLatch, TimeUnit.MINUTES.toNanos(9));
        Future<Boolean> blocker2Future = executor.submit(blocker2Task);
        assertTrue(blocker2StartedLatch.await(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertTrue(over1Latch.await(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertEquals(1, over0Latch.getCount());

        // register a callback for a threshold that has already been reached
        assertNull(executor.registerConcurrencyCallback(0, callbackOver0));
        assertTrue(over0Latch.await(TIMEOUT_NS, TimeUnit.NANOSECONDS));

        // register another callback which hasn't been reached
        CountDownLatch over2Latch = new CountDownLatch(1);
        Runnable callbackOver2 = new CountDownCallback(over2Latch);
        assertNull(executor.registerConcurrencyCallback(2, callbackOver2));
        assertEquals(1, over2Latch.getCount());

        // trigger the active callback by starting another task, such that 3 are running
        CountDownLatch blocker3StartedLatch = new CountDownLatch(1);
        CountDownTask blocker3Task = new CountDownTask(blocker3StartedLatch, blockerContinueLatch, TimeUnit.MINUTES.toNanos(9));
        Future<Boolean> blocker3Future = executor.submit(blocker3Task);
        assertTrue(blocker3StartedLatch.await(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertTrue(over2Latch.await(TIMEOUT_NS, TimeUnit.NANOSECONDS));

        // allow all 3 blocked tasks to finish
        blockerContinueLatch.countDown();

        executor.shutdown();
        assertTrue(executor.awaitTermination(TIMEOUT_NS, TimeUnit.NANOSECONDS));

        assertTrue(blocker1Future.isDone());
        assertTrue(blocker2Future.isDone());
        assertTrue(blocker3Future.isDone());
        assertFalse(blocker1Future.isCancelled());
        assertFalse(blocker2Future.isCancelled());
        assertFalse(blocker3Future.isCancelled());

        try {
            Runnable previous = executor.registerConcurrencyCallback(4, new CountDownCallback(new CountDownLatch(1)));
            fail("Should not be able to register callback after shutdown. Result of register was: " + previous);
        } catch (IllegalStateException x) {
        } // pass
    }

    // Attempt to await termination from multiple threads at once after a shutdown.
    @Test
    public void testConcurrentAwaitTerminationAfterShutdown() throws Exception {
        final int totalAwaits = 10;
        ExecutorService executor = provider.create("testConcurrentAwaitTerminationAfterShutdown")
                        .maxConcurrency(1)
                        .maxQueueSize(10);

        // Submit one task to use up all of the threads of the executor that we will await termination of
        CountDownLatch continueLatch = new CountDownLatch(1);
        CountDownTask blockingTask = new CountDownTask(new CountDownLatch(1), continueLatch, TimeUnit.MINUTES.toNanos(20));
        Future<Boolean> blockerFuture = executor.submit(blockingTask);

        // Submit many additional tasks to be queued
        int numToQueue = 5;
        List<Future<Integer>> queuedFutures = new ArrayList<Future<Integer>>(numToQueue);
        AtomicInteger count = new AtomicInteger(0);
        for (int i = 0; i < numToQueue; i++) {
            System.out.println("Queuing task #" + i);
            queuedFutures.add(executor.submit((Callable<Integer>) new SharedIncrementTask(count)));
        }

        List<Future<Boolean>> awaitTermFutures = new ArrayList<Future<Boolean>>();
        for (int i = 0; i < totalAwaits; i++) {
            System.out.println("Submitting awaitTermination task #" + i);
            awaitTermFutures.add(testThreads.submit(new TerminationAwaitTask(executor, TimeUnit.MINUTES.toNanos(10))));
        }

        executor.shutdown();

        // Allow the single blocking task to complete, which means that it will become possible to run queued tasks.
        continueLatch.countDown();

        long start = System.nanoTime();
        long maxWait = TimeUnit.MINUTES.toNanos(5);
        for (int i = 0; i < totalAwaits; i++) {
            long remaining = maxWait - (System.nanoTime() - start);
            assertTrue("awaitTermination Future #" + i, awaitTermFutures.get(i).get(remaining, TimeUnit.NANOSECONDS));
        }

        assertTrue(blockerFuture.get()); // Initial task completed

        assertEquals(numToQueue, count.get());

        for (int i = 0; i < numToQueue; i++)
            assertTrue("previously queued Future #" + i, queuedFutures.get(i).get(0, TimeUnit.MILLISECONDS) > 0);

        try {
            executor.execute(new SharedIncrementTask(null));
            fail("Submits should not be allowed after shutdown");
        } catch (RejectedExecutionException x) {
        } // pass
    }

    // Attempt to await termination from multiple threads at once after a shutdownNow.
    @Test
    public void testConcurrentAwaitTerminationAfterShutdownNow() throws Exception {
        final int totalAwaitTermination = 6;
        final int totalAwaitEnqueue = 4;
        final int numToQueue = 2;
        PolicyExecutor executor = provider.create("testConcurrentAwaitTerminationAfterShutdownNow")
                        .expedite(0)
                        .maxConcurrency(1)
                        .maxQueueSize(numToQueue)
                        .maxWaitForEnqueue(100);

        // Submit one task to use up all of the threads of the executor that we will await termination of
        CountDownLatch continueLatch = new CountDownLatch(1);
        CountDownTask blockingTask = new CountDownTask(new CountDownLatch(1), continueLatch, TimeUnit.MINUTES.toNanos(30));
        Future<Boolean> blockerFuture = executor.submit(blockingTask);

        AtomicInteger count = new AtomicInteger(0);

        // Submit a couple of additional tasks to be queued
        List<Future<Integer>> queuedFutures = new ArrayList<Future<Integer>>(numToQueue);
        for (int i = 0; i < numToQueue; i++) {
            System.out.println("Queuing task #" + i);
            queuedFutures.add(executor.submit((Callable<Integer>) new SharedIncrementTask(count)));
        }

        // Submit tasks to wait for termination
        List<Future<Boolean>> awaitTermFutures = new ArrayList<Future<Boolean>>();
        for (int i = 0; i < totalAwaitTermination; i++) {
            System.out.println("Submitting awaitTermination task #" + i);
            awaitTermFutures.add(testThreads.submit(new TerminationAwaitTask(executor, TimeUnit.MINUTES.toNanos(10))));
        }

        // Submit several tasks to await queue positions
        List<Future<Future<Integer>>> awaitingEnqueueFutures = new ArrayList<Future<Future<Integer>>>(totalAwaitEnqueue);
        for (int i = 0; i < totalAwaitEnqueue; i++) {
            System.out.println("Submitting task #" + i + " that will wait for a queue position");
            awaitingEnqueueFutures.add(testThreads.submit(new SubmitterTask<Integer>(executor, new SharedIncrementTask(count))));
        }

        List<Runnable> tasksCanceledFromQueue = executor.shutdownNow();

        long start = System.nanoTime();
        long maxWait = TimeUnit.MINUTES.toNanos(5);
        for (int i = 0; i < totalAwaitTermination; i++) {
            long remaining = maxWait - (System.nanoTime() - start);
            assertTrue("awaitTermination Future #" + i, awaitTermFutures.get(i).get(remaining, TimeUnit.NANOSECONDS));
        }

        // Initial task should be canceled
        try {
            fail("Running task should have been canceled due to shutdownNow. Instead: " + blockerFuture.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        } catch (CancellationException x) {
        } // pass

        for (int i = 0; i < numToQueue; i++)
            try {
                fail("shutdownNow should have canceled previously queued Future #" + i + ": " + queuedFutures.get(i).get(0, TimeUnit.MILLISECONDS));
            } catch (CancellationException x) {
            } // pass

        // shutdownNow should cancel at least as many tasks as were in the queue when it was invoked.
        // There is a possibility of a task that was waiting to enqueue briefly entering the queue during this window and also being canceled,
        // which is why we check for at least as many instead of an exact match.
        assertTrue("Tasks canceled from queue by shutdownNow: " + tasksCanceledFromQueue, tasksCanceledFromQueue.size() >= numToQueue);

        // Tasks for blocked enqueue
        for (int i = 0; i < totalAwaitEnqueue; i++) {
            Future<Future<Integer>> ff = awaitingEnqueueFutures.get(i);
            try {
                System.out.println("Future for blocked enqueue #" + i);
                fail("Should not be able to submit task with full queue, even after shutdownNow: " + ff.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
            } catch (ExecutionException x) {
                if (!(x.getCause() instanceof RejectedExecutionException))
                    throw x;
            }
        }

        // None of the queued or waiting-to-enqueue tasks should even attempt to start running
        assertEquals(0, count.get());

        try {
            executor.execute(new SharedIncrementTask(null));
            fail("Submits should not be allowed after shutdownNow");
        } catch (RejectedExecutionException x) {
        } // pass
    }

    // Cancel the same queued tasks from multiple threads at the same time. Each task should only successfully cancel once,
    // and exactly one task waiting for enqueue should be allowed to enqueue for each successful cancel.
    @Test
    public void testConcurrentCancelQueuedTasks() throws Exception {
        PolicyExecutor executor = provider.create("testConcurrentCancelQueuedTasks")
                        .maxConcurrency(1)
                        .maxQueueSize(4)
                        .maxWaitForEnqueue(TimeUnit.MINUTES.toMillis(12));

        // Use up maxConcurrency
        Future<Boolean> blockerFuture = executor.submit(new CountDownTask(new CountDownLatch(1), new CountDownLatch(1), TimeUnit.MINUTES.toNanos(24)));

        // Fill the queue
        AtomicInteger counter = new AtomicInteger();
        Future<Integer> future1 = executor.submit((Callable<Integer>) new SharedIncrementTask(counter));
        Future<Integer> future2 = executor.submit((Callable<Integer>) new SharedIncrementTask(counter));
        Future<Integer> future3 = executor.submit((Callable<Integer>) new SharedIncrementTask(counter));
        Future<Integer> future4 = executor.submit((Callable<Integer>) new SharedIncrementTask(counter));

        // From separate threads, submit tasks that must wait for queue positions
        CompletionService<Future<Integer>> completionSvc = new ExecutorCompletionService<Future<Integer>>(testThreads);
        Future<Future<Integer>> ff5 = completionSvc.submit(new SubmitterTask<Integer>(executor, new SharedIncrementTask(counter)));
        Future<Future<Integer>> ff6 = completionSvc.submit(new SubmitterTask<Integer>(executor, new SharedIncrementTask(counter)));
        Future<Future<Integer>> ff7 = completionSvc.submit(new SubmitterTask<Integer>(executor, new SharedIncrementTask(counter)));

        // Have 8 threads attempt to cancel 2 of the tasks
        int numCancels = 8;
        CountDownLatch beginLatch = new CountDownLatch(numCancels);
        CountDownLatch continueLatch = new CountDownLatch(1);
        Callable<Boolean> cancellationTask1 = new CancellationTask(future1, false, beginLatch, continueLatch, TimeUnit.MINUTES.toNanos(10));
        Callable<Boolean> cancellationTask3 = new CancellationTask(future3, false, beginLatch, continueLatch, TimeUnit.MINUTES.toNanos(13));
        List<Future<Boolean>> cancellationFutures = new ArrayList<Future<Boolean>>(numCancels);
        for (int i = 0; i < 8; i++)
            cancellationFutures.add(testThreads.submit(i % 2 == 1 ? cancellationTask1 : cancellationTask3));

        // Position all of the threads so that they are about to attempt cancel
        assertTrue(beginLatch.await(TIMEOUT_NS * numCancels, TimeUnit.NANOSECONDS));

        // Let them start canceling,
        continueLatch.countDown();

        // Should be able to enqueue exactly 2 more tasks
        Future<Future<Integer>> ffA = completionSvc.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS);
        assertNotNull(ffA);
        assertTrue(ffA.isDone());
        Future<Integer> futureA = ffA.get();

        Future<Future<Integer>> ffB = completionSvc.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS);
        assertNotNull(ffB);
        assertTrue(ffB.isDone());
        Future<Integer> futureB = ffB.get();

        // At this point:
        // futures 1,3 should be canceled
        assertTrue(future1.isCancelled());
        assertTrue(future3.isCancelled());

        // future 2,4,A,B should be queued
        assertFalse(future2.isDone());
        assertFalse(future4.isDone());
        assertFalse(futureA.isDone());
        assertFalse(futureB.isDone());

        // future C (one of 5,6,7) should still be waiting for a slot in the queue.
        assertNull(completionSvc.poll());

        // Set subtraction is an inefficient way to compute the remaining future, but this is only a test case
        Set<Future<Future<Integer>>> remaining = new HashSet<Future<Future<Integer>>>(Arrays.asList(ff5, ff6, ff7));
        assertTrue(remaining.removeAll(Arrays.asList(ffA, ffB)));
        Future<Future<Integer>> ffC = remaining.iterator().next();

        try {
            fail("get of canceled future [1] must fail: " + future1.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        } catch (CancellationException x) {
        } // pass

        try {
            fail("get of canceled future [3] must fail: " + future3.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        } catch (CancellationException x) {
        } // pass

        try {
            fail(ffC + " submit should remain blocked: " + ffC.get(500, TimeUnit.MILLISECONDS));
        } catch (TimeoutException x) {
        } // pass

        // Having already verified that task C wasn't queued, cancel the queue attempt
        assertTrue(ffC.cancel(true));

        try {
            fail("get of canceled future [C] must fail: " + ffC.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        } catch (CancellationException x) {
        } // pass

        // Cancel the blocker task and let the four tasks remaining in the queue start and run to completion
        assertTrue(blockerFuture.cancel(true));

        int result2 = future2.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
        int result4 = future4.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
        int resultA = futureA.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
        int resultB = futureB.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);

        assertEquals(10, result2 + result4 + resultA + resultB); // task increment logic could run in either order, with values of: 1,2,3,4
        assertEquals(4, counter.get());

        assertTrue(future2.isDone());
        assertTrue(future4.isDone());
        assertTrue(futureA.isDone());
        assertTrue(futureB.isDone());
        assertFalse(future2.isCancelled());
        assertFalse(future4.isCancelled());
        assertFalse(futureA.isCancelled());
        assertFalse(futureB.isCancelled());
    }

    // Attempt shutdown and shutdownNow from multiple threads at once.
    @AllowedFFDC("java.lang.InterruptedException") // when shutdownNow cancels tasks that are attempting shutdown/shutdownNow
    @Test
    public void testConcurrentShutdownAndShutdownNow() throws Exception {
        final int total = 10;
        ExecutorService executor = provider.create("testConcurrentShutdownAndShutdownNow").maxConcurrency(total);
        CountDownLatch beginLatch = new CountDownLatch(total);
        CountDownLatch continueLatch = new CountDownLatch(1);

        ShutdownTask shutdownTask = new ShutdownTask(executor, false, beginLatch, continueLatch, TimeUnit.HOURS.toNanos(1));
        ShutdownTask shutdownNowTask = new ShutdownTask(executor, true, beginLatch, continueLatch, TimeUnit.HOURS.toNanos(1));
        ArrayList<Future<List<Runnable>>> futures = new ArrayList<Future<List<Runnable>>>();
        for (int i = 0; i < total; i++)
            if (i % 2 == 0) {
                System.out.println("Submitting shutdown task #" + i);
                futures.add(executor.submit(shutdownTask));
            } else {
                System.out.println("Submitting shutdownNow task #" + i);
                futures.add(executor.submit(shutdownNowTask));
            }

        Thread[] threads = new Thread[total]; // might not be in the same order as tasks were submitted

        // Position all tasks to the point where they are about to attempt a shutdown.
        beginLatch.await(TIMEOUT_NS, TimeUnit.NANOSECONDS);

        for (int i = 0; i < total; i++)
            threads[i] = shutdownTask.executionThreads.poll();

        System.out.println("Execution threads for shutdown tasks: " + Arrays.toString(threads));

        // Let all of the tasks attempt the shutdown
        continueLatch.countDown();

        for (int i = 0; i < total; i++)
            try {
                System.out.println("Attemping get for shutdown future #" + i);
                List<Runnable> canceledQueuedTasks = futures.get(i).get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
                if (i % 2 == 0)
                    assertNull(canceledQueuedTasks);
                else
                    assertEquals(0, canceledQueuedTasks.size());
                System.out.println("Successful");
            } catch (CancellationException x) { // pass because shutdownNow will cancel running tasks
                System.out.println("Task was canceled due to shutdownNow");
            }

        try {
            executor.execute(new SharedIncrementTask(null));
            fail("Submits should not be allowed after shutdown or shutdownNow");
        } catch (RejectedExecutionException x) {
        } // pass

        assertTrue(executor.isShutdown());

        // poll for termination
        for (long start = System.nanoTime(); !executor.isTerminated() && System.nanoTime() - start < TIMEOUT_NS; TimeUnit.MILLISECONDS.sleep(200));

        assertTrue(executor.isTerminated());
    }

    // Attempts submits and cancels concurrently. 3 threads submit 10 tasks each, canceling every other task submitted.
    @Test
    public void testConcurrentSubmitAndCancel() throws Exception {
        final int numThreads = 3;
        final int numIterations = 5;

        final ExecutorService executor = provider.create("testConcurrentSubmitAndCancel")
                        .maxConcurrency(4)
                        .maxQueueSize(30);

        final AtomicInteger counter = new AtomicInteger();
        final BlockingQueue<Future<Integer>> futuresToCancel = new LinkedBlockingQueue<Future<Integer>>();
        final AtomicInteger numSuccessfulCancels = new AtomicInteger();

        Callable<List<Future<Integer>>> multipleSubmitAndCancelTask = new Callable<List<Future<Integer>>>() {
            @Override
            public List<Future<Integer>> call() throws Exception {
                List<Future<Integer>> futures = new ArrayList<Future<Integer>>(numIterations * 2);
                for (int i = 0; i < numIterations; i++) {
                    Future<Integer> futureA = executor.submit((Callable<Integer>) new SharedIncrementTask(counter));
                    futuresToCancel.add(futureA);
                    futures.add(futureA);
                    Future<Integer> futureB = executor.submit((Callable<Integer>) new SharedIncrementTask(counter));
                    futures.add(futureB);

                    Future<?> futureC = futuresToCancel.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS);
                    boolean interruptIfRunning = i % 2 == 0;
                    if (futureC.cancel(interruptIfRunning))
                        numSuccessfulCancels.incrementAndGet();
                }
                return futures;
            }
        };

        List<Callable<List<Future<Integer>>>> testTasks = new ArrayList<Callable<List<Future<Integer>>>>(numThreads);
        for (int i = 0; i < numThreads; i++)
            testTasks.add(multipleSubmitAndCancelTask);

        List<Future<Integer>> allFutures = new ArrayList<Future<Integer>>();
        for (Future<List<Future<Integer>>> result : testThreads.invokeAll(testTasks, TIMEOUT_NS * numThreads * numIterations, TimeUnit.NANOSECONDS))
            allFutures.addAll(result.get());

        int numCanceled = 0;
        int numCompleted = 0;
        HashSet<Integer> resultsOfSuccessfulTasks = new HashSet<Integer>();
        for (Future<Integer> future : allFutures) {
            if (future.isCancelled()) {
                numCanceled++;
                numCompleted++;
            } else if (future.isDone()) {
                assertTrue(resultsOfSuccessfulTasks.add(future.get()));
                numCompleted++;
            } else
                try {
                    assertTrue(resultsOfSuccessfulTasks.add(future.get(TIMEOUT_NS, TimeUnit.NANOSECONDS)));
                    numCompleted++;
                } catch (CancellationException x) {
                    numCanceled++;
                }
        }

        int totalTasksSubmitted = numThreads * numIterations * 2;
        int numTasksThatStartedRunning = counter.get();

        System.out.println(totalTasksSubmitted + " tasks were submitted, of which " + numCompleted + " completed, of which " + numCanceled + " have canceled futures and "
                           + numSuccessfulCancels + " reported successful cancel.");
        System.out.println(numTasksThatStartedRunning + " tasks started running.");

        assertEquals(totalTasksSubmitted, numCompleted);
        assertEquals(numSuccessfulCancels.get(), numCanceled);

        // We requested cancellation of half of the tasks, however, some might have already completed. We can only test that no extras were canceled.
        assertTrue(numCanceled <= totalTasksSubmitted / 2);

        // Some tasks might start running and later be canceled. The number that starts running must be at least half.
        assertTrue(numTasksThatStartedRunning >= totalTasksSubmitted / 2);

        // Every task that was not successfully canceled must return a unique result (per the shared counter).
        assertEquals(totalTasksSubmitted - numSuccessfulCancels.get(), resultsOfSuccessfulTasks.size());

        // Should be nothing left in the queue for the policy executor
        List<Runnable> tasksCanceledFromQueue = executor.shutdownNow();
        assertEquals(0, tasksCanceledFromQueue.size());
    }

    // Attempt submits while concurrently shutting down. Submits should either be accepted
    // or rejected with the error that indicates the executor has been shut down.
    @Test
    public void testConcurrentSubmitAndShutdown() throws Exception {
        final int numSubmits = 10;

        PolicyExecutor executor = provider.create("testConcurrentSubmitAndShutdown").maxConcurrency(numSubmits).maxQueueSize(numSubmits);

        CountDownLatch beginLatch = new CountDownLatch(numSubmits);
        CountDownLatch continueLatch = new CountDownLatch(1);
        AtomicInteger counter = new AtomicInteger();

        List<Future<Future<Integer>>> ffs = new ArrayList<Future<Future<Integer>>>(numSubmits);
        for (int i = 0; i < numSubmits; i++)
            ffs.add(testThreads.submit(new SubmitterTask<Integer>(executor, new SharedIncrementTask(counter), beginLatch, continueLatch, TimeUnit.MINUTES.toNanos(50))));

        // Wait for all of the test threads to position themselves to start submitting to the policy executor
        beginLatch.await(TIMEOUT_NS * 5, TimeUnit.NANOSECONDS);

        // Let them start submitting tasks to the policy executor
        continueLatch.countDown();
        TimeUnit.NANOSECONDS.sleep(100);

        // Shut down the policy executor
        executor.shutdown();

        assertTrue(executor.awaitTermination(TIMEOUT_NS * 5, TimeUnit.NANOSECONDS));
        assertTrue(executor.isTerminated());

        int numAccepted = 0;
        int numRejected = 0;
        int sum = 0;

        for (Future<Future<Integer>> ff : ffs)
            try {
                Future<Integer> future = ff.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
                numAccepted++;
                assertFalse(future.isCancelled());
                assertTrue(future.isDone());
                sum += future.get();
            } catch (ExecutionException x) {
                if (x.getCause() instanceof RejectedExecutionException) {
                    numRejected++;
                    if (!x.getCause().getMessage().contains("CWWKE1202E")) // rejected-due-to-shutdown message
                        throw x;
                } else
                    throw x;
            }

        System.out.println(numAccepted + " accepted, " + numRejected + " rejected");
        assertEquals(numSubmits, numAccepted + numRejected);

        // tasks can run in any order, so it's more convenient to validate the sum of results rather than individual results
        int expectedSum = numAccepted * (numAccepted + 1) / 2;
        assertEquals(expectedSum, sum);
    }

    // Attempt submits while concurrently shutting down via shutdownNow. Submits should either be accepted
    // or rejected with the error that indicates the executor has been shut down. Tasks which are submitted
    // will either be canceled from the queue, canceled while running, or will have completed successfully.
    @Test
    public void testConcurrentSubmitAndShutdownNow() throws Exception {
        final int numSubmits = 10;

        PolicyExecutor executor = provider.create("testConcurrentSubmitAndShutdownNow").maxConcurrency(numSubmits).maxQueueSize(numSubmits);

        CountDownLatch beginLatch = new CountDownLatch(numSubmits);
        CountDownLatch continueLatch = new CountDownLatch(1);
        AtomicInteger counter = new AtomicInteger();

        List<Future<Future<Integer>>> ffs = new ArrayList<Future<Future<Integer>>>(numSubmits);
        for (int i = 0; i < numSubmits; i++)
            ffs.add(testThreads.submit(new SubmitterTask<Integer>(executor, new SharedIncrementTask(counter), beginLatch, continueLatch, TimeUnit.MINUTES.toNanos(40))));

        // Wait for all of the test threads to position themselves to start submitting to the policy executor
        beginLatch.await(TIMEOUT_NS * 5, TimeUnit.NANOSECONDS);

        // Let them start submitting tasks to the policy executor
        continueLatch.countDown();
        TimeUnit.NANOSECONDS.sleep(100);

        // Shut down the policy executor
        List<Runnable> canceledQueuedTasks = executor.shutdownNow();

        assertTrue(executor.awaitTermination(TIMEOUT_NS * 5, TimeUnit.NANOSECONDS));
        assertTrue(executor.isTerminated());

        int numAccepted = 0;
        int numAcceptedThenCanceled = 0;
        int numRejected = 0;
        int sum = 0;

        for (Future<Future<Integer>> ff : ffs)
            try {
                Future<Integer> future = ff.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
                assertTrue(future.isDone());
                numAccepted++;
                if (future.isCancelled())
                    numAcceptedThenCanceled++;
                else
                    sum += future.get();
            } catch (ExecutionException x) {
                if (x.getCause() instanceof RejectedExecutionException) {
                    numRejected++;
                    if (!x.getCause().getMessage().contains("CWWKE1202E")) // rejected-due-to-shutdown message
                        throw x;
                } else
                    throw x;
            }

        int numCanceledFromQueue = canceledQueuedTasks.size();

        System.out.println(numAccepted + " accepted, of which " + numAcceptedThenCanceled + " were canceled due to shutdownNow, with "
                           + numCanceledFromQueue + " canceled from the queue; " + numRejected + " rejected");

        assertEquals(numSubmits, numAccepted + numRejected);

        assertTrue(numCanceledFromQueue <= numAcceptedThenCanceled);

        int numSuccessful = numAccepted - numAcceptedThenCanceled;

        // tasks can run in any order and some might partially run, so we have little guarantee of the results
        int expectedSumMax = numAccepted * (numAccepted + 1) / 2;
        int expectedSumMin = numSuccessful * (numSuccessful + 1) / 2;
        assertTrue(sum >= expectedSumMin);
        assertTrue(sum <= expectedSumMax);
    }

    /**
     * Attempt concurrent updates to maxWaitForEnqueue.
     */
    @Test
    public void testConcurrentUpdateMaxWaitForEnqueue() throws Exception {
        PolicyExecutor executor = provider.create("testConcurrentUpdateMaxWaitForEnqueue")
                        .maxConcurrency(1)
                        .maxQueueSize(1)
                        .maxWaitForEnqueue(TimeUnit.HOURS.toMillis(1))
                        .runIfQueueFull(false);

        final int numConfigTasks = 6;
        CountDownLatch beginLatch = new CountDownLatch(numConfigTasks + 1);
        CountDownLatch continueLatch = new CountDownLatch(1);
        List<Future<Boolean>> futures = new ArrayList<Future<Boolean>>(numConfigTasks);
        for (int i = 0; i < numConfigTasks; i++)
            futures.add(testThreads.submit(new ConfigChangeTask(executor, beginLatch, continueLatch, TimeUnit.MINUTES.toNanos(15), "maxWaitForEnqueue", Integer.toString(i + 1))));

        // Submit a task to use up the maxConcurrency and block all other tasks from running
        CountDownLatch blockerLatch = new CountDownLatch(1);
        CountDownTask blockerTask = new CountDownTask(beginLatch, blockerLatch, TimeUnit.MINUTES.toNanos(40));
        Future<Boolean> blockerFuture = executor.submit(blockerTask);

        // wait for all to start
        assertTrue(beginLatch.await(TIMEOUT_NS * 3, TimeUnit.NANOSECONDS));

        // let them all try to update maxWaitForEnqueue at the same time
        continueLatch.countDown();

        // submit a task to fill the single queue position
        Future<Integer> queuedFuture = executor.submit(new SharedIncrementTask(), 1);

        // wait for all of the config update tasks to finish
        for (Future<Boolean> future : futures)
            assertTrue(future.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));

        // At this point, maxWaitForEnqueue could have any of various small values: 1ms, 2ms, 3ms, ...

        // Attempt to submit tasks that would exceed queue capacity and verify that rejection happens in a timely manner.
        long start = System.nanoTime();
        try {
            fail("Third task needs to be rejected when one uses up maxConcurrency and the other uses up the queue capacity: " + executor.submit(new SharedIncrementTask(), 3));
        } catch (RejectedExecutionException x) {
            long duration = System.nanoTime() - start;
            System.out.println("Submit #3 rejected after " + duration + "ns.");
            assertTrue(duration < TIMEOUT_NS);
            assertTrue(x.getMessage(), x.getMessage().startsWith("CWWKE1201E"));
        }

        start = System.nanoTime();
        try {
            fail("Fourth task needs to be rejected when one uses up maxConcurrency and the other uses up the queue capacity: " + executor.submit(new SharedIncrementTask(), 4));
        } catch (RejectedExecutionException x) {
            long duration = System.nanoTime() - start;
            System.out.println("Submit #4 rejected after " + duration + "ns.");
            assertTrue(duration < TIMEOUT_NS);
            assertTrue(x.getMessage(), x.getMessage().startsWith("CWWKE1201E"));
        }

        // Concurrently update config a few more times while trying submits
        List<Future<Boolean>> configFutures = new ArrayList<Future<Boolean>>(numConfigTasks);
        List<Future<Future<Integer>>> submitterFutures = new ArrayList<Future<Future<Integer>>>(4);
        beginLatch = new CountDownLatch(numConfigTasks + submitterFutures.size());
        continueLatch = new CountDownLatch(1);

        for (int i = 0; i < numConfigTasks; i++)
            configFutures.add(testThreads
                            .submit(new ConfigChangeTask(executor, beginLatch, continueLatch, TimeUnit.MINUTES.toNanos(20), "maxWaitForEnqueue", Integer.toString(i + 1))));

        for (int i = 0; i < submitterFutures.size(); i++)
            submitterFutures.add(testThreads.submit(new SubmitterTask<Integer>(executor, new SharedIncrementTask(), beginLatch, continueLatch, TimeUnit.MINUTES.toNanos(25))));

        // wait for all to start
        assertTrue(beginLatch.await(TIMEOUT_NS * 5, TimeUnit.NANOSECONDS));

        // let them all run at once
        start = System.nanoTime();
        continueLatch.countDown();

        // wait for all of the submitter tasks to finish - when they run, their submit attempts should all be rejected
        for (Future<Future<Integer>> ff : submitterFutures)
            try {
                System.out.println("checking submitter future " + ff);
                fail("Submits must be rejected. Unexpectedly able to obtain result: " + ff.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
            } catch (ExecutionException x) {
                if (!(x.getCause() instanceof RejectedExecutionException) || !x.getCause().getMessage().startsWith("CWWKE1201E"))
                    throw x;
            }
        long duration = System.nanoTime() - start;
        System.out.println("Submits all rejected after " + duration + "ns.");
        assertTrue(duration < TIMEOUT_NS * 4);

        // wait for all of the config update tasks to finish
        for (Future<Boolean> future : configFutures)
            assertTrue(future.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));

        // cancel the queued task an blocker task
        assertTrue(queuedFuture.cancel(false));
        assertTrue(blockerFuture.cancel(true));

        executor.shutdown();
        assertTrue(executor.isShutdown());
        assertTrue(executor.awaitTermination(TIMEOUT_NS, TimeUnit.NANOSECONDS));
    }

    /**
     * Submit a task that fails during onSubmit by raising a RuntimeException subclass. The task submission must be aborted and elapsed queue/run times must be 0.
     */
    public void testFailOnSubmit() throws Exception {
        PolicyExecutor executor = provider.create("testFailOnSubmit");

        SharedIncrementTask task = new SharedIncrementTask();
        FailingCallback callback = new FailingCallback();
        callback.failureClass[FailingCallback.SUBMIT] = IllegalArgumentException.class;
        try {
            executor.submit(task, 1, callback);
            fail("Exception during onSubmit should have prevented task from submitting");
        } catch (IllegalArgumentException x) {
        } // pass

        // onSubmit callback
        PolicyTaskFuture<?> future = callback.future[FailingCallback.SUBMIT];
        assertNotNull(future);
        assertFalse(callback.isDone[FailingCallback.SUBMIT]);
        assertFalse(callback.isCanceled[FailingCallback.SUBMIT]);
        assertEquals(task, callback.task[FailingCallback.SUBMIT]);
        long nsAccept1 = callback.nsAccept[FailingCallback.SUBMIT];
        assertTrue(nsAccept1 >= 0);
        assertEquals(0, callback.nsQueue[FailingCallback.SUBMIT]);
        assertEquals(0, callback.nsRun[FailingCallback.SUBMIT]);

        // onEnd callback
        assertEquals(future, callback.future[FailingCallback.END]);
        assertTrue(callback.isDone[FailingCallback.END]);
        assertFalse(callback.isCanceled[FailingCallback.END]);
        assertEquals(task, callback.task[FailingCallback.END]);
        long nsAccept2 = callback.nsAccept[FailingCallback.END];
        assertTrue(nsAccept1 + "ns, " + nsAccept2 + "ns", nsAccept2 >= nsAccept1);
        assertEquals(0, callback.nsQueue[FailingCallback.END]);
        assertEquals(0, callback.nsRun[FailingCallback.END]);
        Object result = callback.result[FailingCallback.END];
        if (!(result instanceof Throwable))
            fail("Unexpected result " + result);
        Throwable x = (Throwable) result;
        if (!(x instanceof RejectedExecutionException))
            throw new Exception("Unexpected exception, see cause", x);
        if (!(x.getCause() instanceof IllegalArgumentException))
            throw new Exception("Unexpected cause, see chained exceptions", x);

        assertEquals(nsAccept2, future.getElapsedAcceptTime(TimeUnit.NANOSECONDS));
        assertEquals(0, future.getElapsedQueueTime(TimeUnit.NANOSECONDS));
        assertEquals(0, future.getElapsedRunTime(TimeUnit.NANOSECONDS));

        // should not receive onStart or onCancel callback
        assertNull(callback.future[FailingCallback.START]);
        assertNull(callback.future[FailingCallback.CANCEL]);

        assertEquals(0, task.count());

        List<Runnable> tasksCanceledFromQueue = executor.shutdownNow();
        assertEquals(0, tasksCanceledFromQueue.size());
    }

    /**
     * Submit multiple tasks at once, waiting for some to complete before scheduling another group of tasks, and so forth.
     */
    @Test
    public void testGroupedSubmits() throws Exception {
        final int groupSize = 8;
        final int nextGroupOn = 6;
        final int numGroups = 5;

        ExecutorService executor = provider.create("testGroupedSubmits")
                        .maxConcurrency(4)
                        .maxQueueSize(nextGroupOn)
                        .runIfQueueFull(false);

        final CompletionService<Integer> completionSvc = new ExecutorCompletionService<Integer>(executor);

        List<Future<Future<Integer>>> submitFutures = new ArrayList<Future<Future<Integer>>>(groupSize * numGroups);

        AtomicInteger counter = new AtomicInteger();
        Phaser allTasksReady = new Phaser(groupSize);

        for (int g = 0; g < numGroups; g++) {
            // launch separate threads to submit these tasks all at once (via the allTaskReady phaser)
            for (int i = 0; i < groupSize; i++) {
                CompletionServiceTask<Integer> completionSvcTask = new CompletionServiceTask<Integer>(completionSvc, new SharedIncrementTask(counter), allTasksReady);
                submitFutures.add(testThreads.submit(completionSvcTask));
            }

            // Await successful completion of 'nextGroupOn' number of tasks before submitting more
            for (int i = 0; i < nextGroupOn; i++) {
                Future<Integer> future = completionSvc.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS);
                assertNotNull(future);
            }
        }

        List<Future<Integer>> completedFutures = new ArrayList<Future<Integer>>(groupSize * numGroups);
        int numRejected = 0;
        List<Integer> results = new ArrayList<Integer>(groupSize * numGroups);
        int sum = 0;

        // Wait for all remaining tasks to finish and compute totals
        for (Future<Future<Integer>> ff : submitFutures)
            try {
                Future<Integer> future = ff.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
                assertNotNull(completedFutures.add(future));
                int result = future.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
                assertTrue(results.add(result));
                sum += result;
            } catch (ExecutionException x) {
                if (x.getCause() instanceof RejectedExecutionException)
                    numRejected++;
                else
                    throw x;
            }

        System.out.println(completedFutures.size() + " completed successfully: " + completedFutures);
        System.out.println(numRejected + " were rejected");

        int count = counter.get();
        assertEquals(count, results.size());
        assertEquals(count * (count + 1) / 2, sum);

        int maxRejected = (groupSize - nextGroupOn) * numGroups;
        assertTrue("maximum of " + maxRejected + " tasks submits should be rejected", numRejected <= maxRejected);

        executor.shutdown();

        // poll for termination
        for (long start = System.nanoTime(); !executor.isTerminated() && System.nanoTime() - start < TIMEOUT_NS; TimeUnit.MILLISECONDS.sleep(200));

        assertTrue(executor.isTerminated());
    }

    // Tests enforcement of unique identifiers. Unique identifier cannot be reused until after the executor is shut down,
    // either by shutdown or shutdownNow.
    @Test
    public void testIdentifiers() throws Exception {
        ExecutorService executor1 = provider.create("testIdentifiers")
                        .expedite(4)
                        .maxConcurrency(20)
                        .maxQueueSize(50)
                        .maxWaitForEnqueue(TimeUnit.SECONDS.toMillis(30));

        try {
            fail("Should not be able to reuse identifier while previous instance (even if unused) still exists " + provider.create("testIdentifiers"));
        } catch (IllegalStateException x) {
        } // pass

        executor1.shutdown();

        // now we can reuse it
        ExecutorService executor2 = provider.create("testIdentifiers")
                        .maxConcurrency(1)
                        .maxQueueSize(2)
                        .maxWaitForEnqueue(0)
                        .runIfQueueFull(false);

        try {
            fail("First instance should not be usable again after identifier reused " + executor1.submit(new SharedIncrementTask(), null));
        } catch (RejectedExecutionException x) {
            if (!x.getMessage().startsWith("CWWKE1202")) // rejected submit after shutdown
                throw x;
        }

        // New instance should honor its own configuration, not the configuration of the previous instance
        CountDownLatch beginLatch = new CountDownLatch(1);
        CountDownLatch blockerLatch = new CountDownLatch(1);
        CountDownTask blockerTask = new CountDownTask(beginLatch, blockerLatch, TimeUnit.MINUTES.toNanos(20));
        Future<Boolean> blockerFuture = executor2.submit(blockerTask);

        // Wait for the blocker task to use up the maxConcurrency of 1
        assertTrue(beginLatch.await(TIMEOUT_NS, TimeUnit.NANOSECONDS));

        // Fill both queue positions
        AtomicInteger counter = new AtomicInteger();
        Future<Integer> queuedFuture1 = executor2.submit(new SharedIncrementTask(counter), 1);
        Future<Integer> queuedFuture2 = executor2.submit(new SharedIncrementTask(counter), 2);

        // Additional submits should be rejected immediately
        try {
            fail("Shoud not be able to queue another task " + executor2.submit((Callable<Integer>) new SharedIncrementTask(counter)));
        } catch (RejectedExecutionException x) {
            if (!x.getMessage().startsWith("CWWKE1201")) // rejected due to queue at capacity
                throw x;
        }

        try {
            fail("Should not be able to reuse identifier while previous instance still active " + provider.create("testIdentifiers"));
        } catch (IllegalStateException x) {
        } // pass

        // shutdownNow implies shutdown and also makes the identifier reusable
        List<Runnable> canceledFromQueue = executor2.shutdownNow();

        ExecutorService executor3 = provider.create("testIdentifiers");

        assertTrue(executor1.isShutdown());
        assertTrue(executor2.isShutdown());
        assertFalse(executor3.isShutdown());

        assertEquals(0, counter.get()); // previous queued tasks never ran

        // We can resubmit them on the new executor instance
        assertEquals(2, canceledFromQueue.size());
        Future<?> resubmitFuture1 = executor3.submit(canceledFromQueue.get(0));
        Future<?> resubmitFuture2 = executor3.submit(canceledFromQueue.get(1));

        assertNull(resubmitFuture1.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertNull(resubmitFuture2.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertEquals(2, counter.get());

        // The previous futures should still indicate canceled
        assertTrue(queuedFuture1.isCancelled());
        assertTrue(queuedFuture2.isCancelled());
        assertTrue(blockerFuture.isCancelled());

        assertFalse(resubmitFuture1.isCancelled());
        assertFalse(resubmitFuture2.isCancelled());

        try {
            fail("Should not be able to get result from canceled task 1: " + queuedFuture1.get(1, TimeUnit.SECONDS));
        } catch (CancellationException x) {
        } // pass

        try {
            fail("Should not be able to get result from canceled task 2: " + queuedFuture1.get(2, TimeUnit.SECONDS));
        } catch (CancellationException x) {
        } // pass

        // Operations such as awaitTermation are still valid on the shutdown instances, even if the identifier has been reused
        assertTrue(executor1.awaitTermination(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertTrue(executor2.awaitTermination(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertTrue(executor1.isTerminated());
        assertTrue(executor2.isTerminated());

        assertFalse(executor3.isTerminated());

        executor3.shutdown();
        executor3.shutdown(); // redundant, but not harmful
        executor3.shutdown(); // redundant, but not harmful

        assertTrue(executor3.awaitTermination(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertTrue(executor3.isTerminated());
    }

    // Attempt shutdown from multiple threads at once. Interrupt most of them and verify that the interrupted
    // shutdown operations fail rather than prematurely returning as successful prior to a successful shutdown.
    @AllowedFFDC("java.lang.InterruptedException") // when shutdown is interrupted
    @Test
    public void testInterruptShutdown() throws Exception {
        final int total = 10;
        ExecutorService executor = provider.create("testInterruptShutdown").maxConcurrency(total);
        CountDownLatch beginLatch = new CountDownLatch(total);
        CountDownLatch continueLatch = new CountDownLatch(1);

        ShutdownTask shutdownTask = new ShutdownTask(executor, false, beginLatch, continueLatch, TimeUnit.HOURS.toNanos(1));
        ArrayList<Future<List<Runnable>>> futures = new ArrayList<Future<List<Runnable>>>();
        for (int i = 0; i < total; i++) {
            System.out.println("Submitting shutdown task #" + i);
            futures.add(executor.submit(shutdownTask));
        }

        Thread[] threads = new Thread[total]; // might not be in the same order as tasks were submitted

        // Position all tasks to the point where they are about to attempt a shutdown.
        beginLatch.await(TIMEOUT_NS, TimeUnit.NANOSECONDS);

        for (int i = 0; i < total; i++)
            threads[i] = shutdownTask.executionThreads.poll();

        System.out.println("Execution threads for shutdown tasks: " + Arrays.toString(threads));

        // Let all of the tasks attempt the shutdown
        continueLatch.countDown();

        // Interrupt all but the first 2 shutdown operations
        for (int i = 2; i < total; i++)
            threads[i].interrupt();

        // Verify that all shutdown attempts either succeeded or failed as expected with a RuntimeException with cause of InterruptedException
        int interruptCount = 0;
        for (int i = 0; i < total; i++)
            try {
                System.out.println("Attemping get for shutdown future #" + i);
                List<Runnable> result = futures.get(i).get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
                assertNull(result);
                System.out.println("Successful");
            } catch (ExecutionException x) {
                System.out.println(x);
                if (x.getCause() instanceof RuntimeException && x.getCause().getCause() instanceof InterruptedException)
                    interruptCount++;
                else
                    throw x;
            }

        assertTrue("Too many tasks interrupted: " + interruptCount, interruptCount <= 8);

        try {
            executor.execute(new SharedIncrementTask(null));
            fail("Submits should not be allowed after shutdown");
        } catch (RejectedExecutionException x) {
        } // pass

        assertTrue(executor.isShutdown());

        // poll for termination
        for (long start = System.nanoTime(); !executor.isTerminated() && System.nanoTime() - start < TIMEOUT_NS; TimeUnit.MILLISECONDS.sleep(200));

        assertTrue(executor.isTerminated());
    }

    // Attempt shutdownNow from multiple threads at once. Interrupt most of them and verify that the interrupted
    // shutdownNow operations fail rather than prematurely returning as successful prior to a successful shutdown.
    @AllowedFFDC("java.lang.InterruptedException") // when shutdownNow is interrupted
    @Test
    public void testInterruptShutdownNow() throws Exception {
        final int total = 10;
        ExecutorService executor = provider.create("testInterruptShutdownNow").maxConcurrency(total);
        CountDownLatch beginLatch = new CountDownLatch(total);
        CountDownLatch continueLatch = new CountDownLatch(1);

        ShutdownTask shutdownNowTask = new ShutdownTask(executor, true, beginLatch, continueLatch, TimeUnit.HOURS.toNanos(1));
        ArrayList<Future<List<Runnable>>> futures = new ArrayList<Future<List<Runnable>>>();
        for (int i = 0; i < total; i++) {
            System.out.println("Submitting shutdownNow task #" + i);
            futures.add(executor.submit(shutdownNowTask));
        }

        Thread[] threads = new Thread[total]; // might not be in the same order as tasks were submitted

        // Position all tasks to the point where they are about to attempt a shutdown.
        beginLatch.await(TIMEOUT_NS, TimeUnit.NANOSECONDS);

        for (int i = 0; i < total; i++)
            threads[i] = shutdownNowTask.executionThreads.poll();

        System.out.println("Execution threads for shutdownNow tasks: " + Arrays.toString(threads));

        // Let all of the tasks attempt the shutdown
        continueLatch.countDown();

        // Interrupt all but the first 2 shutdown operations
        for (int i = 2; i < total; i++)
            threads[i].interrupt();

        // Verify that all shutdown attempts either succeeded or failed as expected with a RuntimeException with cause of InterruptedException
        int interruptCount = 0;
        for (int i = 0; i < total; i++)
            try {
                System.out.println("Attemping get for shutdownNow future #" + i);
                List<Runnable> canceledQueuedTasks = futures.get(i).get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
                assertEquals(0, canceledQueuedTasks.size());
                System.out.println("Successful");
            } catch (ExecutionException x) {
                System.out.println(x);
                if (x.getCause() instanceof RuntimeException && x.getCause().getCause() instanceof InterruptedException)
                    interruptCount++;
                else
                    throw x;
            } catch (CancellationException x) { // pass because shutdownNow will cancel running tasks
                System.out.println("Task was canceled due to shutdownNow");
            }

        assertTrue("Too many tasks interrupted: " + interruptCount, interruptCount <= 8);

        try {
            executor.execute(new SharedIncrementTask(null));
            fail("Submits should not be allowed after shutdownNow");
        } catch (RejectedExecutionException x) {
        } // pass

        assertTrue(executor.isShutdown());

        // poll for termination
        for (long start = System.nanoTime(); !executor.isTerminated() && System.nanoTime() - start < TIMEOUT_NS; TimeUnit.MILLISECONDS.sleep(200));

        assertTrue(executor.isTerminated());
    }

    // Interrupt an attempt to submit a task. Verify that RejectedExecutionException with chained InterruptedException is raised.
    // Also interrupt a running task which rethrows the InterruptedException and verify that the exception is raised when attempting Future.get,
    // and that a queued task that was blocked waiting for a thread is able to subsequently run.
    @Test
    public void testInterruptSubmitAndRun() throws Exception {
        ExecutorService executor = provider.create("testInterruptSubmitAndRun-submitter")
                        .maxConcurrency(1)
                        .maxQueueSize(1)
                        .maxWaitForEnqueue(TimeUnit.MINUTES.toMillis(4));
        CountDownLatch beginLatch = new CountDownLatch(1);
        CountDownLatch continueLatch = new CountDownLatch(1);
        CountDownTask task1 = new CountDownTask(beginLatch, continueLatch, TimeUnit.HOURS.toNanos(4));

        // Submit a task and wait for it to start
        PolicyTaskFuture<Boolean> future1 = (PolicyTaskFuture<Boolean>) executor.submit(task1);
        assertTrue(beginLatch.await(TIMEOUT_NS, TimeUnit.MILLISECONDS));
        long millis1 = future1.getElapsedRunTime(TimeUnit.MILLISECONDS);
        assertTrue(millis1 >= 0);

        // Submit a task which will be stuck in the queue waiting for the first task before it can get a thread to run on
        AtomicInteger count = new AtomicInteger();
        Future<?> future2 = executor.submit((Runnable) new SharedIncrementTask(count));

        // From a thread owned by the test servlet, interrupt the current thread
        CountDownLatch blocker = new CountDownLatch(1);
        Future<?> interrupterFuture = testThreads.submit(new InterrupterTask(Thread.currentThread(), blocker, 1, TimeUnit.SECONDS));

        try {
            Future<Integer> future3 = executor.submit((Callable<Integer>) new SharedIncrementTask(count));
            fail("Task submit should be interrupted while awaiting a queue position. " + future3);
        } catch (RejectedExecutionException x) {
            if (!(x.getCause() instanceof InterruptedException))
                throw x;
        }

        assertFalse(Thread.currentThread().isInterrupted()); // should have been reset when InterruptedException was raised

        assertFalse(future1.isCancelled());
        assertFalse(future2.isCancelled());
        assertFalse(future1.isDone());
        assertFalse(future2.isDone());

        long millis2 = future1.getElapsedRunTime(TimeUnit.MILLISECONDS);
        assertTrue(millis1 + "ms, " + millis2 + "ms", millis2 - millis1 >= 900);

        // Also interrupt the executing task
        Thread executionThread = task1.executionThreads.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS);
        assertNotNull(executionThread);
        executionThread.interrupt();

        // Interruption of task1 should allow the queued task to run
        assertNull(future2.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertEquals(1, count.get());
        assertFalse(future2.isCancelled());
        assertTrue(future2.isDone());

        // Task1 should be completed with exception, but not canceled
        long nanos3 = future1.getElapsedRunTime(TimeUnit.NANOSECONDS);
        assertTrue(nanos3 + "ns, " + millis2 + "ms", TimeUnit.NANOSECONDS.toMillis(nanos3) >= millis2);
        assertFalse(future1.isCancelled());
        assertTrue(future1.isDone());
        try {
            fail("Interrupted task that rethrows exception should not return result: " + future1.get(1, TimeUnit.NANOSECONDS));
        } catch (ExecutionException x) {
            if (!(x.getCause() instanceof InterruptedException))
                throw x;
        }
        assertEquals(nanos3, future1.getElapsedRunTime(TimeUnit.NANOSECONDS));

        // Wait for the task that was submitted to the test's fixed thread pool to complete, if it hasn't done so already
        assertNull(interrupterFuture.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
    }

    // Supply invalid arguments to various ExecutorService methods and verify the behavior matches the requirements of the JavaDoc.
    // Also supply some values as the top and bottom of the valid range.
    @Test
    public void testInvalidArguments() throws Exception {
        ExecutorService executor = provider.create("testInvalidArguments");

        assertFalse(executor.awaitTermination(-1, TimeUnit.MILLISECONDS));
        assertFalse(executor.awaitTermination(Long.MIN_VALUE, TimeUnit.DAYS));

        try {
            fail("Should fail with missing unit. Instead: " + executor.awaitTermination(5, null));
        } catch (NullPointerException x) {
        } // pass

        try {
            executor.execute(null);
            fail("Execute should fail with null task.");
        } catch (NullPointerException x) {
        } // pass

        try {
            fail("Should fail with null Callable. Instead: " + executor.submit((Callable<String>) null));
        } catch (NullPointerException x) {
        } // pass

        try {
            fail("Should fail with null Runnable. Instead: " + executor.submit((Runnable) null));
        } catch (NullPointerException x) {
        } // pass

        try {
            fail("Should fail with null Runnable & valid result. Instead: " + executor.submit(null, 1));
        } catch (NullPointerException x) {
        } // pass

        executor.shutdown();

        long start = System.nanoTime();
        assertTrue(executor.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS));
        long duration = start - System.nanoTime();
        assertTrue("awaitTermination took " + duration + "ns", duration < TIMEOUT_NS);
    }

    // Use invokeAll to run a group of tasks. Some might run on pooled threads. Some might run on the current thread.
    // The last task in the list will always run on the current thread (implementation detail).
    @SuppressWarnings("unchecked")
    @Test
    public void testInvokeAll() throws Exception {
        PolicyExecutor executor = provider.create("testInvokeAll")
                        .maxConcurrency(4)
                        .maxQueueSize(2);

        List<Callable<Number>> tasks = new ArrayList<Callable<Number>>();
        AtomicInteger counter = new AtomicInteger();
        for (int i = 0; i < 8; i++)
            tasks.add(Callable.class.cast(new SharedIncrementTask(counter)));
        tasks.add(Callable.class.cast(new ThreadIdTask()));

        List<Future<Number>> futures = executor.invokeAll(tasks);

        assertEquals(9, futures.size());

        int sum = 0;
        for (int i = 0; i < 8; i++) {
            Future<Number> future = futures.get(i);
            assertTrue("Task #" + i, future.isDone());
            assertFalse("Task #" + i, future.isCancelled());
            sum += future.get(0, TimeUnit.SECONDS).intValue();
        }

        assertEquals(36, sum);

        Future<Number> future = futures.get(8);
        assertTrue(future.isDone());
        assertFalse(future.isCancelled());
        assertEquals(Thread.currentThread().getId(), future.get(0, TimeUnit.NANOSECONDS));

        List<Runnable> canceledFromQueue = executor.shutdownNow();
        assertEquals(0, canceledFromQueue.size());
    }

    // Use invokeAll where maxQueueSize is constrained to 1 and runIfQueueFull is false and maxConcurrency is unlimited.
    // Normally, the queue size of 1 would risk having task submissions aborted when tasks are not pulled from the queue
    // for execution quickly enough. However, the fact that the user has specified the untimed invokeAll operation to
    // wait for all tasks to be completed means that the current thread can be used to help complete the tasks in the
    // event that they cannot be queued, because runIfQueueFull is only for submit/execute and does not apply to invokeAll/Any.
    @Test
    public void testInvokeAllAbortIgnoredWhenConcurrencyUnlimited() throws Exception {
        PolicyExecutor executor = provider.create("testInvokeAllAbortIgnoredWhenConcurrencyUnlimited")
                        .maxPolicy(MaxPolicy.loose)
                        .maxQueueSize(1)
                        .runIfQueueFull(false);

        AtomicInteger counter = new AtomicInteger();
        List<Callable<Integer>> tasks = Arrays.<Callable<Integer>> asList(new SharedIncrementTask(counter), new SharedIncrementTask(counter), new SharedIncrementTask(counter),
                                                                          new SharedIncrementTask(counter), new SharedIncrementTask(counter));

        List<Future<Integer>> futures = executor.invokeAll(tasks);

        assertEquals(5, futures.size());

        int i = 0, sum = 0;
        for (Future<Integer> future : futures) {
            assertTrue("Task #" + i, future.isDone());
            assertFalse("Task #" + i, future.isCancelled());
            sum += future.get(0, TimeUnit.SECONDS);
            i++;
        }

        assertEquals(15, sum);

        // Elapsed time for task that runs on current thread
        PolicyTaskFuture<Integer> future = (PolicyTaskFuture<Integer>) futures.get(4);
        long time;
        assertTrue((time = future.getElapsedAcceptTime(TimeUnit.NANOSECONDS)) + "ns", time >= 0);
        assertEquals(time, future.getElapsedAcceptTime(TimeUnit.NANOSECONDS)); // consistent value when repeated
        assertEquals(0, future.getElapsedQueueTime(TimeUnit.NANOSECONDS));
        assertTrue((time = future.getElapsedRunTime(TimeUnit.NANOSECONDS)) + "ns", time >= 0);
        assertEquals(time, future.getElapsedRunTime(TimeUnit.NANOSECONDS)); // consistent value when repeated

        List<Runnable> canceledFromQueue = executor.shutdownNow();
        assertEquals(0, canceledFromQueue.size());
    }

    // Attempts the timed and untimed invokeAll operations on a policy executor instance that has been shut down, expecting it to be rejected.
    public void testInvokeAllAfterShutdown() throws Exception {
        List<Callable<Integer>> oneTask = Collections.singletonList((Callable<Integer>) new SharedIncrementTask());

        PolicyExecutor executor = provider.create("testInvokeAllAfterShutdown");
        executor.shutdown();

        try {
            fail("untimed invokeAll should not be permitted after shutdown. Instead: " + executor.invokeAll(oneTask));
        } catch (RejectedExecutionException x) {
            if (!x.getMessage().startsWith("CWWKE1202"))
                throw x;
        }

        try {
            fail("timed invokeAll should not be permitted after shutdown. Instead: " + executor.invokeAll(oneTask, 1, TimeUnit.MINUTES));
        } catch (RejectedExecutionException x) {
            if (!x.getMessage().startsWith("CWWKE1202"))
                throw x;
        }

        executor = provider.create("testInvokeAllAfterShutdownNow")
                        .maxPolicy(MaxPolicy.loose)
                        .runIfQueueFull(true);
        executor.shutdownNow();

        try {
            fail("untimed invokeAll should not be permitted after shutdownNow. Instead: " + executor.invokeAll(oneTask));
        } catch (RejectedExecutionException x) {
            if (!x.getMessage().startsWith("CWWKE1202"))
                throw x;
        }

        try {
            fail("timed invokeAll should not be permitted after shutdownNow. Instead: " + executor.invokeAll(oneTask, 1, TimeUnit.MINUTES));
        } catch (RejectedExecutionException x) {
            if (!x.getMessage().startsWith("CWWKE1202"))
                throw x;
        }
    }

    // Use invokeAll where one or more tasks must run on the current thread due to the queue being full.
    // To achieve this, we constrain maxConcurrency and maxQueueSize to 1 and supply 4 tasks to invokeAll,
    // where the first task blocks until all others start. This causes maxConcurrency to be exceeded, which is only
    // possible due to the maxConcurrencyAppliesToCallerThread=false property.
    @Test
    public void testInvokeAllCallerRunsWhenQueueFull() throws Exception {
        PolicyExecutor executor = provider.create("testInvokeAllCallerRunsWhenQueueFull")
                        .maxConcurrency(1)
                        .maxPolicy(MaxPolicy.loose)
                        .maxQueueSize(1);

        CountDownLatch beginLatch = new CountDownLatch(3);
        CountDownLatch ignore = new CountDownLatch(0);

        Collection<Callable<Boolean>> tasks = new ArrayList<Callable<Boolean>>(4);
        tasks.add(new CountDownTask(ignore, beginLatch, TIMEOUT_NS * 3)); // waits for the other 3
        tasks.add(new CountDownTask(beginLatch, ignore, 0));
        tasks.add(new CountDownTask(beginLatch, ignore, 0));
        tasks.add(new CountDownTask(beginLatch, ignore, 0));

        List<Future<Boolean>> futures = executor.invokeAll(tasks);

        assertEquals(4, futures.size());

        int i = 0;
        for (Future<Boolean> future : futures) {
            assertTrue("Task #" + i, future.isDone());
            assertFalse("Task #" + i, future.isCancelled());
            assertEquals("Task #" + i, Boolean.TRUE, future.get(0, TimeUnit.MILLISECONDS));
            i++;
        }

        List<Runnable> canceledFromQueue = executor.shutdownNow();
        assertEquals(0, canceledFromQueue.size());
    }

    // Test the code path where untimed invokeAll is interrupted while it is running a task on the current thread
    // after the other tasks have been successfully submitted. Once interrupted, invokeAll must immediately cancel all of
    // its tasks which have not already completed and return.
    @Test
    public void testInvokeAllInterruptedWhileRunningTaskAfterOtherTasksSubmitted() throws Exception {
        PolicyExecutor executor = provider.create("testInvokeAllInterruptedWhileRunningTaskAfterOtherTasksSubmitted")
                        .maxConcurrency(2);

        CountDownLatch beginLatch = new CountDownLatch(2);
        CountDownLatch blocker = new CountDownLatch(1);
        long taskDurationNS = TimeUnit.MINUTES.toNanos(5);
        Collection<CountDownTask> tasks = new LinkedHashSet<CountDownTask>();
        tasks.add(new CountDownTask(beginLatch, blocker, taskDurationNS));
        tasks.add(new CountDownTask(beginLatch, blocker, taskDurationNS));

        // interrupt current thread upon seeing that both tasks have started
        Future<?> interrupterFuture = testThreads.submit(new InterrupterTask(Thread.currentThread(), beginLatch, 5, TimeUnit.MINUTES));

        long start = System.nanoTime();
        try {
            List<Future<Boolean>> futures = executor.invokeAll(tasks);
            fail("invokeAll should have been interrupted. Instead: " + futures);
        } catch (InterruptedException x) { // pass
        }

        // confirm that invokeAll completed in a timely manner after interrupt
        long duration = System.nanoTime() - start;
        assertTrue(Long.toString(duration), duration < taskDurationNS);

        interrupterFuture.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);

        List<Runnable> canceledFromQueue = executor.shutdownNow();
        assertEquals(0, canceledFromQueue.size());
    }

    // Test the code path where untimed invokeAll is interrupted while it is running a task on the current thread
    // because the queue has reached capacity. Once interrupted, invokeAll must immediately cancel all of
    // its tasks which have not already completed and return.
    @Test
    public void testInvokeAllInterruptedWhileRunningTaskThatCannotBeSubmitted() throws Exception {
        PolicyExecutor executor = provider.create("testInvokeAllInterruptedWhileRunningTaskThatCannotBeSubmitted")
                        .maxConcurrency(1)
                        .maxPolicy(MaxPolicy.loose)
                        .maxQueueSize(1);

        // Invoke 4 tasks.
        // The first will start running and block.
        // The second might be enqueued to global or run on current thread depending on how quickly the first task starts running. We will have this task block.
        // The third should either never run due to the second task blocking, or will be forced to run on current thread due to the queue having reached capacity. We will have this task block as well.
        // The fourth should never run.

        CountDownLatch beginLatch1_and_2or3 = new CountDownLatch(2); // to wait for first and third tasks to start
        CountDownLatch beginLatch4 = new CountDownLatch(1); // to ensure the fourth task never starts
        CountDownLatch blocker = new CountDownLatch(1);
        CountDownLatch unused = new CountDownLatch(0);
        long taskDurationNS = TimeUnit.MINUTES.toNanos(5);
        Collection<CountDownTask> tasks = new ArrayList<CountDownTask>();
        tasks.add(new CountDownTask(beginLatch1_and_2or3, blocker, taskDurationNS));
        tasks.add(new CountDownTask(beginLatch1_and_2or3, blocker, taskDurationNS));
        tasks.add(new CountDownTask(beginLatch1_and_2or3, blocker, taskDurationNS));
        tasks.add(new CountDownTask(beginLatch4, unused, 0));

        // interrupt current thread upon seeing that both tasks have started
        Future<?> interrupterFuture = testThreads.submit(new InterrupterTask(Thread.currentThread(), beginLatch1_and_2or3, 5, TimeUnit.MINUTES));

        long start = System.nanoTime();
        try {
            List<Future<Boolean>> futures = executor.invokeAll(tasks);
            fail("invokeAll should have been interrupted. Instead: " + futures);
        } catch (InterruptedException x) { // pass
        }

        // confirm that invokeAll completed in a timely manner after interrupt
        long duration = System.nanoTime() - start;
        assertTrue(Long.toString(duration), duration < taskDurationNS);

        assertEquals(1, beginLatch4.getCount());

        interrupterFuture.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);

        executor.shutdownNow();
    }

    // Use invokeAll where the group of tasks to invoke is a single task and where it is no tasks at all.
    // When a single task is invoked, it runs on the current thread if a permit is available, and otherwise on a separate thread.
    // However, if using CallerRuns, then it always runs on the current thread regardless of whether a permit is available.
    @Test
    public void testInvokeAllOfOneAndNone() throws Exception {
        PolicyExecutor executor = provider.create("testInvokeAllOfOneAndNone")
                        .maxConcurrency(1)
                        .maxPolicy(MaxPolicy.strict)
                        .maxQueueSize(2)
                        .maxWaitForEnqueue(TimeUnit.SECONDS.toMillis(10));

        long threadId = Thread.currentThread().getId();
        List<Future<Long>> futures;

        // Invoke group of none
        futures = executor.invokeAll(Collections.<Callable<Long>> emptySet());
        assertEquals(0, futures.size());

        // Invoke group of one, where a maxConcurrency permit is available
        futures = executor.invokeAll(Collections.singleton(new ThreadIdTask()));
        assertEquals(1, futures.size());
        assertEquals(Long.valueOf(threadId), futures.get(0).get(0, TimeUnit.MINUTES));

        // Use up the maxConcurrency permit
        CountDownLatch blockingLatch = new CountDownLatch(1);
        Future<Boolean> blockingFuture = executor.submit(new CountDownTask(new CountDownLatch(1), blockingLatch, TimeUnit.MINUTES.toNanos(15)));

        // Invoke group of one, where a maxConcurrency permit is unavailable.
        // Because no threads are available to run the task, invokeAll will be blocked, and we will need to interrupt it
        testThreads.submit(new InterrupterTask(Thread.currentThread(), blockingLatch, 1, TimeUnit.SECONDS));
        try {
            futures = executor.invokeAll(Collections.singleton(new ThreadIdTask()));
            fail("Able to invoke task despite maximum concurrency. " + futures);
        } catch (InterruptedException x) {
        } // pass

        // If using maxConcurrencyAppliesToCallerThread=false, we don't need a permit
        executor.maxPolicy(MaxPolicy.loose);
        futures = executor.invokeAll(Collections.singleton(new ThreadIdTask()));
        assertEquals(1, futures.size());
        assertEquals(Long.valueOf(threadId), futures.get(0).get(0, TimeUnit.MINUTES));

        // Release the blocker and let the blocking task finish normally
        blockingLatch.countDown();
        assertTrue(blockingFuture.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));

        List<Runnable> canceledFromQueue = executor.shutdownNow();
        assertEquals(0, canceledFromQueue.size());
    }

    // Use invokeAll where all tasks run on the current thread because there is only 1 maxConcurrency permit available.
    @Test
    public void testInvokeAllOnCurrentThread() throws Exception {
        PolicyExecutor executor = provider.create("testInvokeAllOnCurrentThread")
                        .expedite(1)
                        .maxConcurrency(1)
                        .maxPolicy(MaxPolicy.strict)
                        .maxQueueSize(10);

        long threadId = Thread.currentThread().getId();

        List<Future<Long>> futures = executor.invokeAll(Arrays.asList(new ThreadIdTask(), new ThreadIdTask(), new ThreadIdTask()));

        assertEquals(3, futures.size());

        int i = 0;
        for (Future<Long> future : futures) {
            assertTrue("Task #" + i, future.isDone());
            assertFalse("Task #" + i, future.isCancelled());
            assertEquals("Task #" + i, Long.valueOf(threadId), future.get(0, TimeUnit.MICROSECONDS));
            i++;
        }

        List<Runnable> canceledFromQueue = executor.shutdownNow();
        assertEquals(0, canceledFromQueue.size());
    }

    // Use invokeAll where all tasks run on the current thread even without a maxConcurrency permit due maxConcurrencyAppliesToCallerThread=false.
    @Test
    public void testInvokeAllOnCurrentThreadCallerRunsWithoutPermit() throws Exception {
        PolicyExecutor executor = provider.create("testInvokeAllOnCurrentThreadCallerRunsWithoutPermit")
                        .maxConcurrency(1)
                        .maxPolicy(MaxPolicy.loose)
                        .maxQueueSize(10);

        // Use up the only permit
        CountDownLatch beginLatch = new CountDownLatch(1);
        Future<Boolean> blockerFuture = executor.submit(new CountDownTask(beginLatch, new CountDownLatch(1), TimeUnit.MINUTES.toNanos(5)));
        assertTrue(beginLatch.await(TIMEOUT_NS, TimeUnit.NANOSECONDS));

        long threadId = Thread.currentThread().getId();

        List<Future<Long>> futures = executor.invokeAll(Arrays.asList(new ThreadIdTask(), new ThreadIdTask(), new ThreadIdTask()));

        assertEquals(3, futures.size());

        int i = 0;
        for (Future<Long> future : futures) {
            assertTrue("Task #" + i, future.isDone());
            assertFalse("Task #" + i, future.isCancelled());
            assertEquals("Task #" + i, Long.valueOf(threadId), future.get(0, TimeUnit.MICROSECONDS));
            i++;
        }

        assertTrue(blockerFuture.cancel(true));

        List<Runnable> canceledFromQueue = executor.shutdownNow();
        assertEquals(0, canceledFromQueue.size());
    }

    // Test invalid parameters for invokeAll. This includes a null task list, null as the only element in
    // the task list and null within a list of otherwise valid tasks.
    @Test
    public void testInvokeAllInvalidParameters() throws Exception {
        PolicyExecutor executor = provider.create("testInvokeAllInvalidParameters")
                        .maxConcurrency(2)
                        .maxWaitForEnqueue(TimeUnit.NANOSECONDS.toMillis(TIMEOUT_NS));

        // Null list of tasks
        try {
            fail("invokeAll with null list of tasks should be rejected. Instead: " +
                 executor.invokeAll(null));
        } catch (NullPointerException x) {
        } // pass

        // Null first element in list of tasks
        try {
            fail("invokeAll with null first task should be rejected. Instead: " +
                 executor.invokeAll(Collections.<Callable<Object>> singletonList(null)));
        } catch (NullPointerException x) {
        } // pass

        // Null element within list of otherwise valid tasks
        List<SharedIncrementTask> listWithOneNull = new ArrayList<SharedIncrementTask>(5);
        listWithOneNull.add(new SharedIncrementTask());
        listWithOneNull.add(new SharedIncrementTask());
        listWithOneNull.add(new SharedIncrementTask());
        listWithOneNull.add(null);
        listWithOneNull.add(new SharedIncrementTask());
        try {
            fail("invokeAll with null task in list among non-nulls should be rejected. Instead: " +
                 executor.invokeAll(listWithOneNull));
        } catch (NullPointerException x) {
        } // pass

        // Make sure we didn't run any
        for (int i = 0; i < listWithOneNull.size(); i++) {
            SharedIncrementTask task = listWithOneNull.get(i);
            if (task != null)
                assertEquals("Task #" + i, 0, task.count());
        }

        List<Runnable> canceledFromQueue = executor.shutdownNow();
        assertEquals(0, canceledFromQueue.size());
    }

    // Invoke a group of tasks where some of the tasks raise exceptions during execution.
    // This should not cause invokeAll to return prematurely and all other tasks must be allowed to continue to completion.
    @Test
    public void testInvokeAllTasksThatRaiseExceptions() throws Exception {
        PolicyExecutor executor = provider.create("testInvokeAllTasksThatRaiseExceptions")
                        .maxConcurrency(1)
                        .maxQueueSize(1);

        final CountDownLatch beginLatch = new CountDownLatch(1);
        CountDownLatch blocker = new CountDownLatch(1);
        CountDownLatch unused = new CountDownLatch(0);

        final List<Callable<Boolean>> tasks = new ArrayList<Callable<Boolean>>();
        tasks.add(new CountDownTask(beginLatch, blocker, TimeUnit.MINUTES.toNanos(5))); // run on global pool, will be interrupted by current thread once it begins
        tasks.add(new CountDownTask(unused, null, 0)); // intentionally cause NullPointerException during execution, could run on current thread or on global pool
        tasks.add(new CountDownTask(unused, null, 0)); // intentionally cause NullPointerException, will always run on current thread
        tasks.add(new Callable<Boolean>() { // interrupt the first task, so that it raises InterruptedException
            @Override
            public Boolean call() throws Exception {
                if (!beginLatch.await(TIMEOUT_NS, TimeUnit.NANOSECONDS))
                    return false;
                ((CountDownTask) tasks.get(0)).executionThreads.peek().interrupt();
                return true;
            }
        });
        tasks.add(new CountDownTask(unused, null, 0)); // intentionally cause NullPointerException, will always run on current thread

        List<Future<Boolean>> futures = executor.invokeAll(tasks);

        assertEquals(5, futures.size());

        Future<Boolean> future = futures.get(0);
        assertTrue(future.isDone());
        assertFalse(future.isCancelled());
        try {
            fail("Interrupted task should report failure. Instead: " + future.get());
        } catch (ExecutionException x) {
            if (!(x.getCause() instanceof InterruptedException))
                throw x;
        }

        future = futures.get(1);
        assertTrue(future.isDone());
        assertFalse(future.isCancelled());
        try {
            fail("Second task should report NullPointerException. Instead: " + future.get());
        } catch (ExecutionException x) {
            if (!(x.getCause() instanceof NullPointerException))
                throw x;
        }

        future = futures.get(2);
        assertTrue(future.isDone());
        assertFalse(future.isCancelled());
        try {
            fail("Third task should report NullPointerException. Instead: " + future.get());
        } catch (ExecutionException x) {
            if (!(x.getCause() instanceof NullPointerException))
                throw x;
        }

        future = futures.get(3);
        assertTrue(future.isDone());
        assertFalse(future.isCancelled());
        assertTrue(future.get());

        future = futures.get(4);
        assertTrue(future.isDone());
        assertFalse(future.isCancelled());
        try {
            fail("Fifth task should report NullPointerException. Instead: " + future.get());
        } catch (ExecutionException x) {
            if (!(x.getCause() instanceof NullPointerException))
                throw x;
        }

        List<Runnable> canceledFromQueue = executor.shutdownNow();
        assertEquals(0, canceledFromQueue.size());
    }

    // Submit and wait for groups of tasks via the timed invokeAll method.
    // Includes coverage of scenarios where core/max concurrency is insufficient to run all of the tasks at once,
    // and where the maximum queue size is insufficient to allow all of the tasks to be queued and
    // requires waiting.
    @Test
    public void testInvokeAllTimed() throws Exception {
        PolicyExecutor executor = provider.create("testInvokeAllTimed")
                        .expedite(2)
                        .maxConcurrency(3)
                        .maxPolicy(MaxPolicy.strict) // meaningless for timed invokeAll, which never runs on caller thread
                        .maxQueueSize(2)
                        .maxWaitForEnqueue(TimeUnit.NANOSECONDS.toMillis(TIMEOUT_NS));

        List<Future<Integer>> futures;
        Future<Integer> future;

        // Invoke nothing
        futures = executor.invokeAll(Collections.<Callable<Integer>> emptyList(), 20, TimeUnit.SECONDS);
        assertEquals(0, futures.size());

        // Invoke one
        SharedIncrementTask task = new SharedIncrementTask();
        futures = executor.invokeAll(Arrays.asList(task), TIMEOUT_NS, TimeUnit.NANOSECONDS);
        assertEquals(1, futures.size());
        assertNotNull(future = futures.get(0));
        assertTrue(future.isDone());
        assertFalse(future.isCancelled());
        assertEquals(Integer.valueOf(1), future.get(0, TimeUnit.SECONDS));

        // Invoke three of the same task
        int sum = 0;
        futures = executor.invokeAll(Arrays.asList(task, task, task), TIMEOUT_NS * 3, TimeUnit.NANOSECONDS);
        assertEquals(3, futures.size());
        assertNotNull(future = futures.get(0));
        assertTrue(future.isDone());
        assertFalse(future.isCancelled());
        sum += future.get(0, TimeUnit.HOURS);
        assertNotNull(future = futures.get(1));
        assertTrue(future.isDone());
        assertFalse(future.isCancelled());
        sum += future.get(0, TimeUnit.DAYS);
        assertNotNull(future = futures.get(2));
        assertTrue(future.isDone());
        assertFalse(future.isCancelled());
        sum += future.get(0, TimeUnit.MINUTES);
        assertEquals(9 /* 2 + 3 + 4 */, sum);

        // Invoke 5 different tasks with minor waiting for enqueue
        AtomicInteger counter = new AtomicInteger();
        ArrayList<SharedIncrementTask> tasks = new ArrayList<SharedIncrementTask>();
        for (int i = 0; i < 5; i++)
            tasks.add(new SharedIncrementTask(counter));
        futures = executor.invokeAll(tasks, TIMEOUT_NS, TimeUnit.NANOSECONDS);
        assertEquals(5, futures.size());
        sum = 0;
        for (int i = 0; i < 5; i++) {
            assertNotNull("Future #" + i, future = futures.get(i));
            sum += future.get(0, TimeUnit.NANOSECONDS);
            assertTrue("Future #" + i, future.isDone());
            assertFalse("Future #" + i, future.isCancelled());
        }
        assertEquals(15, sum);

        List<Runnable> canceledFromQueue = executor.shutdownNow();
        assertEquals(0, canceledFromQueue.size());
    }

    // Interrupt timed invokeAll while that tasks that it submitted are in progress.
    // All of the tasks should be canceled when invokeAll returns and finish stopping in a timely manner.
    @Test
    public void testInvokeAllTimedInterruptWaitForCompletion() throws Exception {
        PolicyExecutor executor = provider.create("testInvokeAllTimedInterruptWaitForCompletion")
                        .expedite(2);

        CountDownLatch beginLatch = new CountDownLatch(3);
        CountDownLatch blocker = new CountDownLatch(1);
        Stack<CountDownTask> tasks = new Stack<CountDownTask>();
        tasks.push(new CountDownTask(beginLatch, blocker, TimeUnit.MINUTES.toNanos(7)));
        tasks.push(new CountDownTask(beginLatch, blocker, TimeUnit.MINUTES.toNanos(8)));
        tasks.push(new CountDownTask(beginLatch, blocker, TimeUnit.MINUTES.toNanos(9)));

        // Interrupt the current thread only after all of the tasks that we will invoke have started,
        Future<?> interrupterFuture = testThreads.submit(new InterrupterTask(Thread.currentThread(), beginLatch, TIMEOUT_NS * 3, TimeUnit.NANOSECONDS));

        try {
            fail("Should have been interrupted. Instead: " + executor.invokeAll(tasks, 10, TimeUnit.MINUTES));
        } catch (InterruptedException x) {
        } // pass

        // Ensure the tasks all stop in a timely manner
        boolean stopped = false;
        for (long start = System.nanoTime(); !stopped && System.nanoTime() - start < TIMEOUT_NS; Thread.sleep(200))
            stopped = tasks.get(0).executionThreads.isEmpty() && tasks.get(1).executionThreads.isEmpty() && tasks.get(2).executionThreads.isEmpty();
        assertTrue(stopped);

        interrupterFuture.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);

        List<Runnable> canceledFromQueue = executor.shutdownNow();
        assertEquals(0, canceledFromQueue.size());
    }

    // Interrupt timed invokeAll while it is waiting for a queue position in order to submit one of the tasks.
    // All of the tasks that previously started should be canceled when invokeAll returns and finish stopping in a timely manner.
    @Test
    public void testInvokeAllTimedInterruptWaitForEnqueue() throws Exception {
        PolicyExecutor executor = provider.create("testInvokeAllTimedInterruptWaitForEnqueue")
                        .expedite(1)
                        .maxConcurrency(1)
                        .maxQueueSize(1)
                        .maxWaitForEnqueue(TimeUnit.MINUTES.toMillis(8));

        CountDownLatch beginLatch = new CountDownLatch(1); // wait for any 1 task to begin
        CountDownLatch blocker = new CountDownLatch(1);
        Vector<CountDownTask> tasks = new Vector<CountDownTask>();
        tasks.add(new CountDownTask(beginLatch, blocker, TimeUnit.MINUTES.toNanos(4)));
        tasks.add(new CountDownTask(beginLatch, blocker, TimeUnit.MINUTES.toNanos(5)));
        tasks.add(new CountDownTask(beginLatch, blocker, TimeUnit.MINUTES.toNanos(6)));

        // Interrupt the current thread only after one of the tasks that we will invoke has started,
        Future<?> interrupterFuture = testThreads.submit(new InterrupterTask(Thread.currentThread(), beginLatch, TIMEOUT_NS * 3, TimeUnit.NANOSECONDS));

        try {
            fail("Should have been interrupted. Instead: " + executor.invokeAll(tasks, 9, TimeUnit.MINUTES));
        } catch (InterruptedException x) {
        } // pass

        // Ensure the tasks all stop in a timely manner (some won't ever have started)
        boolean stopped = false;
        for (long start = System.nanoTime(); !stopped && System.nanoTime() - start < TIMEOUT_NS; Thread.sleep(200))
            stopped = tasks.get(0).executionThreads.isEmpty() && tasks.get(1).executionThreads.isEmpty() && tasks.get(2).executionThreads.isEmpty();
        assertTrue(stopped);

        interrupterFuture.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);

        List<Runnable> canceledFromQueue = executor.shutdownNow();
        assertEquals(0, canceledFromQueue.size());
    }

    // Test invalid parameters for invokeAll with timeout. This includes a null task list, null as the only element in
    // the task list, null within a list of otherwise valid tasks, null time unit, and negative timeout.
    @Test
    public void testInvokeAllTimedInvalidParameters() throws Exception {
        PolicyExecutor executor = provider.create("testInvokeAllTimedInvalidParameters")
                        .maxConcurrency(2)
                        .maxWaitForEnqueue(TimeUnit.NANOSECONDS.toMillis(TIMEOUT_NS));

        // Null list of tasks
        try {
            fail("Timed invokeAll with null list of tasks should be rejected. Instead: " +
                 executor.invokeAll(null, 5, TimeUnit.MINUTES));
        } catch (NullPointerException x) {
        } // pass

        // Null first element in list of tasks
        try {
            fail("Timed invokeAll with null first task should be rejected. Instead: " +
                 executor.invokeAll(Collections.<Callable<Object>> singletonList(null), 5, TimeUnit.MINUTES));
        } catch (NullPointerException x) {
        } // pass

        // Null element within list of otherwise valid tasks
        List<SharedIncrementTask> listWithOneNull = new ArrayList<SharedIncrementTask>(5);
        listWithOneNull.add(new SharedIncrementTask());
        listWithOneNull.add(new SharedIncrementTask());
        listWithOneNull.add(new SharedIncrementTask());
        listWithOneNull.add(null);
        listWithOneNull.add(new SharedIncrementTask());
        try {
            fail("Timed invokeAll with null task in list among non-nulls should be rejected. Instead: " +
                 executor.invokeAll(listWithOneNull, 5, TimeUnit.MINUTES));
        } catch (NullPointerException x) {
        } // pass

        // Make sure we didn't run any
        for (int i = 0; i < listWithOneNull.size(); i++) {
            SharedIncrementTask task = listWithOneNull.get(i);
            if (task != null)
                assertEquals("Task #" + i, 0, task.count());
        }

        // Null time unit
        try {
            fail("Timed invokeAll with null time unit should be rejected. Instead: " +
                 executor.invokeAll(Collections.singletonList(new SharedIncrementTask()), 6, null));
        } catch (NullPointerException x) {
        } // pass

        // Negative timeout
        try {
            fail("Timed invokeAll with negative timeout should be rejected. Instead: " +
                 executor.invokeAll(Collections.singletonList(new SharedIncrementTask()), -7, TimeUnit.SECONDS));
        } catch (RejectedExecutionException x) {
            if (!x.getMessage().startsWith("CWWKE1204"))
                throw x;
        }

        List<Runnable> canceledFromQueue = executor.shutdownNow();
        assertEquals(0, canceledFromQueue.size());
    }

    // Use timed invokeAll to submit a group of tasks. Verify that none of them run on the submitter thread.
    // This test case is added to help guard against regression to code which relies on the behavior of
    // timed invokeAll never running tasks on the submitter thread.
    // We think some customers might be relying on this behavior even though the JavaDoc/spec does not state it,
    // so do not change this behavior or alter this test without having a very good reason, and in which case
    // it should first be considered if the behavior ought to be made switchable.
    @Test
    public void testInvokeAllTimedSubmitterThreadDoesNotRunTasks() throws Exception {
        PolicyExecutor executor = provider.create("testInvokeAllTimedSubmitterThreadDoesNotRunTasks")
                        .maxConcurrency(2)
                        .maxQueueSize(4);

        long submitterThreadId = Thread.currentThread().getId();

        List<Future<Long>> futures = executor.invokeAll(Arrays.asList(new ThreadIdTask(), new ThreadIdTask(), new ThreadIdTask(), new ThreadIdTask()),
                                                        Long.MAX_VALUE,
                                                        TimeUnit.MILLISECONDS);

        for (Future<Long> future : futures)
            assertFalse(Long.valueOf(submitterThreadId).equals(future.get()));

        List<Runnable> canceledFromQueue = executor.shutdownNow();
        assertEquals(0, canceledFromQueue.size());
    }

    // Using timed invokeAll submit a group of tasks where some block, such that invokeAll times out before
    // all of the tasks complete. Verify that the blocking tasks are canceled such that invokeAll can return
    // in a timely manner, and that any non-blocking tasks completed, either successfully or exceptionally.
    @Test
    public void testInvokeAllTimedTimeoutWaitForCompletion() throws Exception {
        PolicyExecutor executor = provider.create("testInvokeAllTimedTimeoutWaitForCompletion");

        LinkedHashSet<CountDownTask> tasks = new LinkedHashSet<CountDownTask>();
        CountDownLatch beginLatch = new CountDownLatch(4);
        CountDownLatch continueLatch = new CountDownLatch(1);
        tasks.add(new CountDownTask(beginLatch, continueLatch, TimeUnit.MINUTES.toNanos(6)));
        tasks.add(new CountDownTask(beginLatch, continueLatch, 0)); // non-blocking
        tasks.add(new CountDownTask(beginLatch, continueLatch, 0)); // non-blocking
        tasks.add(new CountDownTask(beginLatch, continueLatch, TimeUnit.MINUTES.toNanos(6)));

        List<Future<Boolean>> futures = null;
        Future<Boolean> future;
        for (int retry = 0; futures == null && retry < 100; retry++)
            try {
                futures = executor.invokeAll(tasks, 1, TimeUnit.SECONDS);
            } catch (RejectedExecutionException x) {
                System.out.println("Retry submitting 4 tasks within a second.");
                x.printStackTrace(System.out);
            }

        assertNotNull("After 100 attempts, still unable to submit 4 simple tasks within a second. Aborting the test.", futures);

        assertEquals(4, futures.size());

        assertNotNull(future = futures.get(0));
        assertTrue(future.isDone());
        assertTrue(future.isCancelled());

        assertNotNull(future = futures.get(1));
        assertTrue(future.isDone());
        if (!future.isCancelled())
            assertFalse(future.get(0, TimeUnit.SECONDS));

        assertNotNull(future = futures.get(2));
        assertTrue(future.isDone());
        if (!future.isCancelled())
            assertFalse(future.get());

        assertNotNull(future = futures.get(3));
        assertTrue(future.isCancelled());
        assertTrue(future.isDone());
    }

    // Using timed invokeAll where the timeout is much longer than the maxWaitForEnqueue and maxConcurrency is less
    // than the number of tasks submitted, submit several tasks that start and block. Should be rejected waiting
    // for a queue position and all in-progress tasks should cancel before returning from invokeAll.
    // Rejection should occur even if maxConcurrencyAppliesToCallerThread is false and runIfQueueFull is true
    // because timed invokeAll disallows having the caller run tasks so that the timing can be honored.
    @Test
    public void testInvokeAllTimedTimeoutWaitForEnqueue() throws Exception {
        PolicyExecutor executor = provider.create("testInvokeAllTimedTimeoutWaitForEnqueue")
                        .maxConcurrency(1)
                        .maxPolicy(MaxPolicy.loose)
                        .maxQueueSize(1)
                        .maxWaitForEnqueue(200)
                        .runIfQueueFull(true);

        List<CountDownTask> blockingTasks = new LinkedList<CountDownTask>();
        CountDownLatch beginLatch = new CountDownLatch(3);
        CountDownLatch continueLatch = new CountDownLatch(1);
        blockingTasks.add(new CountDownTask(beginLatch, continueLatch, TimeUnit.MINUTES.toNanos(5)));
        blockingTasks.add(new CountDownTask(beginLatch, continueLatch, TimeUnit.MINUTES.toNanos(5)));
        blockingTasks.add(new CountDownTask(beginLatch, continueLatch, TimeUnit.MINUTES.toNanos(5)));

        long start = System.nanoTime();
        try {
            // Note that we allow plenty of time for the tasks to finish. However, with only 1 threads
            // able to run tasks and one queue slot to hold the waiting tasks, we should timeout
            // on the attempt to enqueue the third task (or second task if the system is slow).
            List<Future<Boolean>> futures = executor.invokeAll(blockingTasks, 20, TimeUnit.MINUTES);
            fail("Should have timed out queuing second or third task for execution. Instead: " + futures);
        } catch (RejectedExecutionException x) {
            if (!x.getMessage().startsWith("CWWKE1201E"))
                throw x;
        }

        long duration = System.nanoTime() - start;
        assertTrue("Took " + duration + "ns to timeout, which probably means maxWaitForEnqueue wasn't honored.", duration < TIMEOUT_NS);

        List<Runnable> canceledFromQueue = executor.shutdownNow();
        assertEquals(0, canceledFromQueue.size());
    }

    // Using the untimed invokeAll where maxConcurrency is less than the number of tasks submitted, submit several tasks
    // that start and block. Should be rejected waiting for a queue position and all in-progress tasks should cancel
    // before returning from invokeAll.
    @Test
    public void testInvokeAllTimeoutWaitForEnqueue() throws Exception {
        PolicyExecutor executor = provider.create("testInvokeAllTimeoutWaitForEnqueue")
                        .maxConcurrency(1)
                        .maxPolicy(MaxPolicy.strict)
                        .maxQueueSize(2)
                        .maxWaitForEnqueue(200);

        // Use up the only maxConcurrency permit to prevent invokeAll from acquiring it and running the tasks on the current thread
        CountDownLatch blockingLatch = new CountDownLatch(1);
        PolicyTaskFuture<Boolean> blockerFuture = (PolicyTaskFuture<Boolean>) executor
                        .submit(new CountDownTask(new CountDownLatch(1), blockingLatch, TimeUnit.MINUTES.toNanos(10)));
        long millis1 = blockerFuture.getElapsedRunTime(TimeUnit.MILLISECONDS);
        assertTrue(millis1 >= 0);

        AtomicInteger counter = new AtomicInteger();
        List<Callable<Integer>> blockedTasks = Arrays.<Callable<Integer>> asList(new SharedIncrementTask(counter),
                                                                                 new SharedIncrementTask(counter),
                                                                                 new SharedIncrementTask(counter));

        long start = System.nanoTime();
        try {
            // With only 1 queue slot to hold the waiting tasks, we should timeout on the attempt to enqueue the third task.
            List<Future<Integer>> futures = executor.invokeAll(blockedTasks);
            fail("Should have timed out queuing third task for execution. Instead: " + futures);
        } catch (RejectedExecutionException x) {
            if (!x.getMessage().startsWith("CWWKE1201E"))
                throw x;
        }

        long duration = System.nanoTime() - start;
        assertTrue("Took " + duration + "ns to timeout, which probably means maxWaitForEnqueue wasn't honored.", duration < TIMEOUT_NS);

        // Allow the blocking task to complete.
        blockingLatch.countDown();
        assertTrue(blockerFuture.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));

        // Elapsed run time of successful task
        long nanos2 = blockerFuture.getElapsedRunTime(TimeUnit.NANOSECONDS);
        assertTrue(millis1 + "ms, " + nanos2 + "ns", TimeUnit.NANOSECONDS.toMillis(nanos2) - millis1 > 100);
        assertEquals(nanos2, blockerFuture.getElapsedRunTime(TimeUnit.NANOSECONDS));

        // None of the invokeAll tasks should run and increment the counter because they
        // should have been canceled before any started upon rejection of invokeAll.
        assertEquals(0, counter.get());

        List<Runnable> canceledFromQueue = executor.shutdownNow();
        assertEquals(0, canceledFromQueue.size());
    }

    // Basic test of untimed invokeAny. Submit a group of tasks, of which one raises an exception,
    // another runs for a long time, and another completes successfully within a short time.
    // The result of the one that completed successfully should be returned,
    // and the duration of the invokeAny method should show that it only waited for the first successful task.
    @Test
    public void testInvokeAny1Successful() throws Exception {
        PolicyExecutor executor = provider.create("testInvokeAny1Successful");

        CountDownLatch beginLatchForBlocker = new CountDownLatch(1);
        CountDownLatch blocker = new CountDownLatch(1);
        CountDownLatch unused = new CountDownLatch(0);

        List<Callable<Boolean>> tasks = new ArrayList<Callable<Boolean>>();
        tasks.add(new CountDownTask(beginLatchForBlocker, blocker, TIMEOUT_NS * 2)); // block for longer than the timeout
        tasks.add(new CountDownTask(unused, null, 0)); // intentionally cause NullPointerException
        tasks.add(new CountDownTask(unused, beginLatchForBlocker, TIMEOUT_NS)); // should succeed

        long start = System.nanoTime();
        assertTrue(executor.invokeAny(tasks));
        long duration = System.nanoTime() - start;

        assertTrue(duration + "ns", duration < TIMEOUT_NS);

        List<Runnable> canceledFromQueue = executor.shutdownNow();
        assertEquals(0, canceledFromQueue.size());
    }

    // Submit a group of tasks via untimed invokeAny, where all fail during execution.
    // Verify that an ExecutionException is raised which chains one of the failures.
    @Test
    public void testInvokeAnyAllFail() throws Exception {
        PolicyExecutor executor = provider.create("testInvokeAnyAllFail");

        final int numTasks = 3;

        List<Callable<Integer>> tasks = new LinkedList<Callable<Integer>>();
        for (int i = 0; i < numTasks; i++)
            tasks.add(new MinFinderTaskWithInvokeAll(new int[] {}, 1, 2, executor)); // cause ArrayIndexOutOfBoundsException

        long start = System.nanoTime();
        try {
            int result = executor.invokeAny(tasks);
            fail("invokeAny should not have result when all tasks fail: " + result);
        } catch (ExecutionException x) {
            if (!(x.getCause() instanceof ArrayIndexOutOfBoundsException))
                throw x;
        }
        long duration = System.nanoTime() - start;

        assertTrue(duration + "ns", duration < TIMEOUT_NS);

        List<Runnable> canceledFromQueue = executor.shutdownNow();
        assertEquals(0, canceledFromQueue.size());
    }

    // Submit a group of tasks via untimed invokeAny, where all are able to complete successfully.
    // Verify that invokeAny returns the result of one of the successful tasks.
    @Test
    public void testInvokeAnyAllSuccessful() throws Exception {
        PolicyExecutor executor = provider.create("testInvokeAnyAllSuccessful");

        final int numTasks = 3;
        AtomicInteger counter = new AtomicInteger();

        List<Callable<Integer>> tasks = new LinkedList<Callable<Integer>>();
        for (int i = 0; i < numTasks; i++)
            tasks.add(new SharedIncrementTask(counter));

        long start = System.nanoTime();
        int result = executor.invokeAny(tasks);
        long duration = System.nanoTime() - start;

        assertTrue(Integer.toString(result), result >= 1 && result <= numTasks);
        assertTrue(duration + "ns", duration < TIMEOUT_NS);

        int count = counter.get();
        assertTrue(Integer.toString(count), count >= 1 && count <= numTasks);

        List<Runnable> canceledFromQueue = executor.shutdownNow();
        assertEquals(0, canceledFromQueue.size());
    }

    // Verify that untimed invokeAny stops trying to submit tasks after reaching the maxWaitForEnqueue.
    // It should not keep applying the maxWaitForEnqueue in attempting to submit every task in the list.
    @Test
    public void testInvokeAnyExceedMaxWaitForEnqueue() throws Exception {
        PolicyExecutor executor = provider.create("testInvokeAnyExceedMaxWaitForEnqueue")
                        .maxConcurrency(1)
                        .maxQueueSize(1)
                        .maxWaitForEnqueue(500)
                        .runIfQueueFull(true); // does not apply to invokeAny/All

        // Use up maxConcurrency
        CountDownLatch blocker = new CountDownLatch(1);
        CountDownLatch blockerBeginLatch = new CountDownLatch(1);
        ParameterInfoCallback callback = new ParameterInfoCallback();
        PolicyTaskFuture<Boolean> blockerFuture = executor.submit(new CountDownTask(blockerBeginLatch, blocker, TIMEOUT_NS), callback);

        // Somewhat unrelated testing of timing operations on PolicyTaskFuture, included in this test because it already waits for 500ms+
        assertTrue(callback.latch[ParameterInfoCallback.START].await(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        long millis1 = blockerFuture.getElapsedRunTime(TimeUnit.MILLISECONDS);
        assertTrue(millis1 >= 0);
        assertTrue(blockerBeginLatch.await(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        long millis2 = blockerFuture.getElapsedRunTime(TimeUnit.MILLISECONDS);
        assertTrue(millis2 >= millis1);
        assertTrue(blockerFuture.getElapsedAcceptTime(TimeUnit.NANOSECONDS) >= 0);
        assertTrue(blockerFuture.getElapsedQueueTime(TimeUnit.NANOSECONDS) >= 0);

        // Use up maxQueueSize
        AtomicInteger counter = new AtomicInteger();
        Runnable stuckInQueueTask = new SharedIncrementTask(counter);
        Future<?> stuckInQueueFuture = executor.submit(stuckInQueueTask, 1);

        final int numTasks = 20;
        List<Callable<Integer>> tasks = new ArrayList<Callable<Integer>>(numTasks);
        for (int i = 0; i < numTasks; i++)
            tasks.add(new SharedIncrementTask(counter));

        // Note: this test is timing sensitive and therefore involves retries to guard against failures due to delays and slowness in test infrastructure.
        // The invokeAny method ought to generally fail after a little more then 500ms. However, we are allowing up to 9 seconds,
        // which limits coverage to verifying that every single task in the list didn't await its maxWaitForEnqueue (which would total just over 10 seconds).
        // Hopefully the generous timing allowance combined with the retries will prevent invalid intermittent failures from appearing.
        // If not, the timing will need to be further adjusted.
        long start, duration = Long.MAX_VALUE;
        for (int retry = 0; duration >= TimeUnit.SECONDS.toNanos(9) && retry < 10; retry++) {
            start = System.nanoTime();
            try {
                fail("Should not be able to invoke any task when queue is blocked. Instead: " + executor.invokeAny(tasks));
            } catch (RejectedExecutionException x) {
                duration = System.nanoTime() - start;
            }
        }

        assertTrue(duration + "ns", duration < TimeUnit.SECONDS.toNanos(9));

        assertEquals(0, counter.get());

        long millis3 = blockerFuture.getElapsedRunTime(TimeUnit.MILLISECONDS);
        assertTrue(millis2 + "ms, " + millis3 + "ms", millis3 - millis2 > 400);

        List<Runnable> canceledFromQueue = executor.shutdownNow();
        assertEquals(1, canceledFromQueue.size());
        assertEquals(stuckInQueueTask, canceledFromQueue.get(0));

        assertEquals(0, counter.get());

        // After blocker task ends (it was canceled during shutdownNow, and should complete shortly after), getElapsedRunTime should stop changing
        assertTrue(callback.latch[ParameterInfoCallback.END].await(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertEquals(callback.nsRun[ParameterInfoCallback.END], blockerFuture.getElapsedRunTime(TimeUnit.NANOSECONDS));
    }

    // Submit tasks via timed and untimed invokeAny that interrupts itself in order to test appropriate handling of InterruptedException during execution.
    // ExecutionException with chained InterruptedException must be raised when running on a different thread, which is consistent with an asynchronous task failing.
    // When running on the same thread (untimed invokeAny with single task), InterruptedException should be raised directly to the caller,
    // which indicates an interruption of thread of execution of invokeAny.
    @Test
    public void testInvokeAnyInterruptRunningTask() throws Exception {
        PolicyExecutor executor = provider.create("testInvokeAnyInterruptRunningTask");

        Callable<Boolean> task = new Callable<Boolean>() {
            @Override
            public Boolean call() throws InterruptedException {
                Thread.currentThread().interrupt();
                TimeUnit.SECONDS.sleep(1); // raise InterruptedException
                return false;
            }
        };

        // timed
        try {
            fail("Task should fail during execution and timed invokeAny should raise exception. Instead: " +
                 executor.invokeAny(Collections.singleton(task), TIMEOUT_NS, TimeUnit.NANOSECONDS));
        } catch (ExecutionException x) {
            if (!(x.getCause() instanceof InterruptedException))
                throw x;
        }

        // untimed with 1 task (interrupts caller thread which it runs on)
        try {
            fail("Untimed invokeAny(1 task) should be interrupted. Instead: " +
                 executor.invokeAny(Collections.singleton(task)));
        } catch (InterruptedException x) {
        } // pass

        // untimed with 2 tasks
        try {
            fail("Task should fail during execution and untimed invokeAny(multiple tasks) should raise exception. Instead: " +
                 executor.invokeAny(Arrays.<Callable<Boolean>> asList(task, task)));
        } catch (ExecutionException x) {
            if (!(x.getCause() instanceof InterruptedException))
                throw x;
        }

        List<Runnable> canceledFromQueue = executor.shutdownNow();
        assertEquals(0, canceledFromQueue.size());
    }

    // Submit a group of tasks to untimed invokeAny, one of which raises an exception and another of which blocks.
    // Interrupt the invokeAny operation before it completes. Expect InterruptedException.
    @Test
    public void testInvokeAnyInterruptWaitForCompletion() throws Exception {
        PolicyExecutor executor = provider.create("testInvokeAnyInterruptWaitForCompletion");

        CountDownLatch beginLatch = new CountDownLatch(2);
        CountDownLatch blocker = new CountDownLatch(1);
        Collection<CountDownTask> tasks = Arrays.asList(new CountDownTask(beginLatch, null, 0), // Intentionally fail with NullPointerException
                                                        new CountDownTask(beginLatch, blocker, TIMEOUT_NS * 2)); // block

        // Interrupt the current thread after both tasks start
        Future<?> interrupterFuture = testThreads.submit(new InterrupterTask(Thread.currentThread(), beginLatch, TIMEOUT_NS * 2, TimeUnit.NANOSECONDS));

        long start = System.nanoTime();
        try {
            fail("Should have been interrupted. Instead: " + executor.invokeAny(tasks));
        } catch (InterruptedException x) { // pass
        }
        long duration = System.nanoTime() - start;

        assertTrue("Interrupt should happen promptly, instead took " + duration + "ns", duration < TIMEOUT_NS); // allow for slowness and pauses in test systems

        interrupterFuture.get();

        List<Runnable> canceledFromQueue = executor.shutdownNow();
        assertEquals(0, canceledFromQueue.size());
    }

    // Submit a group of tasks to untimed invokeAny, one of which raises an exception, another of which blocks all others from starting,
    // another of which it stuck in the queue, and another of which is blocked attempting to get a queue position.
    // Interrupt the invokeAny operation before it completes. Expect InterruptedException.
    @Test
    public void testInvokeAnyInterruptWaitForEnqueue() throws Exception {
        PolicyExecutor executor = provider.create("testInvokeAnyInterruptWaitForEnqueue")
                        .maxConcurrency(1)
                        .maxQueueSize(1)
                        .maxWaitForEnqueue(TimeUnit.NANOSECONDS.toMillis(TIMEOUT_NS * 4));

        CountDownLatch beginLatch_1_2 = new CountDownLatch(2);
        CountDownLatch blocker = new CountDownLatch(1);
        CountDownLatch unused = new CountDownLatch(0);
        Collection<CountDownTask> tasks = Arrays.asList(new CountDownTask(beginLatch_1_2, null, 0), // Intentionally fail with NullPointerException
                                                        new CountDownTask(beginLatch_1_2, blocker, TIMEOUT_NS * 2), // block
                                                        new CountDownTask(unused, blocker, TIMEOUT_NS * 2), // stuck in queue until invokeAny is interrupted
                                                        new CountDownTask(unused, blocker, TIMEOUT_NS * 2)); // blocked from entering queue until invokeAny is interrupted

        // Interrupt the current thread after the first two tasks start
        Future<?> interrupterFuture = testThreads.submit(new InterrupterTask(Thread.currentThread(), beginLatch_1_2, TIMEOUT_NS * 2, TimeUnit.NANOSECONDS));

        long start = System.nanoTime();
        try {
            fail("Should have been interrupted. Instead: " + executor.invokeAny(tasks));
        } catch (InterruptedException x) { // pass
        }
        long duration = System.nanoTime() - start;

        assertTrue("Interrupt should happen promptly, instead took " + duration + "ns", duration < TIMEOUT_NS); // allow for slowness and pauses in test systems

        interrupterFuture.get();

        List<Runnable> canceledFromQueue = executor.shutdownNow();
        assertEquals(0, canceledFromQueue.size()); // third task should have already been canceled upon invokeAny terminating with InterruptedException
    }

    // Test invalid parameters for invokeAny. This includes a null task list, an empty task list, null time unit, negative timeout,
    // null as the only element in the task list and null within a list of otherwise valid tasks.
    @Test
    public void testInvokeAnyInvalidParameters() throws Exception {
        PolicyExecutor executor = provider.create("testInvokeAnyInvalidParameters")
                        .maxConcurrency(2)
                        .maxWaitForEnqueue(TimeUnit.NANOSECONDS.toMillis(TIMEOUT_NS));

        // Null list of tasks
        try {
            fail("untimed invokeAny with null list of tasks should be rejected. Instead: " +
                 executor.invokeAny(null));
        } catch (NullPointerException x) {
        } // pass

        try {
            fail("timed invokeAny with null list of tasks should be rejected. Instead: " +
                 executor.invokeAny(null, TIMEOUT_NS, TimeUnit.NANOSECONDS));
        } catch (NullPointerException x) {
        } // pass

        // Empty task list
        try {
            fail("untimed invokeAny with empty list of tasks should be rejected. Instead: " +
                 executor.invokeAny(Collections.<Callable<Object>> emptyList()));
        } catch (IllegalArgumentException x) {
        } // pass

        try {
            fail("timed invokeAny with empty list of tasks should be rejected. Instead: " +
                 executor.invokeAny(Collections.<Callable<Object>> emptyList(), 2, TimeUnit.SECONDS));
        } catch (IllegalArgumentException x) {
        } // pass

        // Null first element in list of tasks
        try {
            fail("untimed invokeAny with null first task should be rejected. Instead: " +
                 executor.invokeAny(Collections.<Callable<Object>> singletonList(null)));
        } catch (NullPointerException x) {
        } // pass

        try {
            fail("timed invokeAny with null first task should be rejected. Instead: " +
                 executor.invokeAny(Collections.<Callable<Object>> singletonList(null), TIMEOUT_NS, TimeUnit.NANOSECONDS));
        } catch (NullPointerException x) {
        } // pass

        // Null element within list of otherwise valid tasks
        List<SharedIncrementTask> listWithOneNull = new ArrayList<SharedIncrementTask>(5);
        listWithOneNull.add(new SharedIncrementTask());
        listWithOneNull.add(new SharedIncrementTask());
        listWithOneNull.add(new SharedIncrementTask());
        listWithOneNull.add(null);
        listWithOneNull.add(new SharedIncrementTask());
        try {
            fail("untimed invokeAny with null task in list among non-nulls should be rejected. Instead: " +
                 executor.invokeAny(listWithOneNull));
        } catch (NullPointerException x) {
        } // pass

        // Make sure we didn't run any
        for (int i = 0; i < listWithOneNull.size(); i++) {
            SharedIncrementTask task = listWithOneNull.get(i);
            if (task != null)
                assertEquals("Task #" + i, 0, task.count());
        }

        try {
            fail("timed invokeAny with null task in list among non-nulls should be rejected. Instead: " +
                 executor.invokeAny(listWithOneNull, TIMEOUT_NS, TimeUnit.NANOSECONDS));
        } catch (NullPointerException x) {
        } // pass

        // Make sure we didn't run any
        for (int i = 0; i < listWithOneNull.size(); i++) {
            SharedIncrementTask task = listWithOneNull.get(i);
            if (task != null)
                assertEquals("Task #" + i, 0, task.count());
        }

        // Negative timeout
        try {
            fail("invokeAny with negative timeout should be rejected. Instead: " +
                 executor.invokeAny(Collections.<Callable<Integer>> singletonList(new SharedIncrementTask()), -9, TimeUnit.HOURS));
        } catch (RejectedExecutionException x) {
            if (!x.getMessage().startsWith("CWWKE1204"))
                throw x;
        }

        // Null time unit
        try {
            fail("invokeAny with null unit should be rejected. Instead: " +
                 executor.invokeAny(Collections.<Callable<Integer>> singletonList(new SharedIncrementTask()), 18, null));
        } catch (NullPointerException x) {
        } // pass

        List<Runnable> canceledFromQueue = executor.shutdownNow();
        assertEquals(0, canceledFromQueue.size());
    }

    // Submit a group of tasks via untimed invokeAny, where one of the tasks promptly returns a null result, and another returns a non-null result after a delay.
    // Verify that the result of invokeAny is null. This test verifies that the implementation is not limited to awaiting non-null results, and handles null properly.
    // Also verifies that the other task ends before it otherwise would have due to the implicit cancel/interrupt upon return of invokeAny.
    @Test
    public void testInvokeAnyNullSuccessfulResult() throws Exception {
        PolicyExecutor executor = provider.create("testInvokeAnyNullSuccessfulResult");

        CountDownLatch blocker = new CountDownLatch(1);
        CountDownTask task1 = new CountDownTask(new CountDownLatch(0), blocker, TIMEOUT_NS * 3);

        SharedIncrementTask task2 = new SharedIncrementTask();

        List<Callable<Boolean>> tasks = Arrays.asList(task1, Executors.callable(task2, (Boolean) null));

        assertNull(executor.invokeAny(tasks));

        // Another way to verify that task2 completed
        assertEquals(1, task2.count());

        // Verify that task1 ends prior to when it would have otherwise completed. This is due to cancel/interruption upon return from invokeAny.
        for (long start = System.nanoTime(); task1.executionThreads.peek() != null && System.nanoTime() - start < TIMEOUT_NS; Thread.sleep(100));
        assertNull(task1.executionThreads.peek());

        List<Runnable> canceledFromQueue = executor.shutdownNow();
        assertEquals(0, canceledFromQueue.size());
    }

    // Submit a single task via untimed invokeAny. If a permit is available or a permit isn't required to run tasks,
    // then it should run on the caller's thread. Otherwise, it should run on a separate thread.
    @Test
    public void testInvokeAnyOfOne() throws Exception {
        PolicyExecutor executor = provider.create("testInvokeAnyOfOne")
                        .maxConcurrency(1)
                        .maxPolicy(MaxPolicy.strict) // require a permit
                        .maxQueueSize(1);

        Set<Callable<Long>> oneTask = Collections.<Callable<Long>> singleton(new ThreadIdTask());
        Long curThreadId = Thread.currentThread().getId();

        // Permit is required and available. Task should run on current thread.
        assertEquals(curThreadId, executor.invokeAny(oneTask));

        // Use up the permit by running a task that will block until the queue capacity drops to 0 (at which point, the invokeAny task was already queued to the global pool)
        CountDownLatch beginLatch = new CountDownLatch(1);
        final CountDownLatch noQueueCapacityRemains = new CountDownLatch(1);
        Future<Boolean> blockerFuture = executor.submit(new CountDownTask(beginLatch, noQueueCapacityRemains, TIMEOUT_NS));
        assertTrue(beginLatch.await(TIMEOUT_NS, TimeUnit.NANOSECONDS));

        // Register callback to decrement the above latch once queue capacity is used up.
        assertNull(executor.registerQueueSizeCallback(1, new CountDownCallback(noQueueCapacityRemains)));

        // Permit is required and unavailable. Task should be run on global thread pool.
        assertNotSame(curThreadId, executor.invokeAny(oneTask));

        assertTrue(blockerFuture.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));

        executor.maxPolicy(MaxPolicy.loose);

        // Use up the permit
        blockerFuture = executor.submit(new CountDownTask(new CountDownLatch(0), new CountDownLatch(1), TIMEOUT_NS));

        // Permit is unavailable but not required. Task should run on current thread.
        assertEquals(curThreadId, executor.invokeAny(oneTask));

        assertTrue(blockerFuture.cancel(true));

        // invokeAny task raises a RuntimeException (subclass) on same thread
        try {
            Collection<Callable<Integer>> oneFailingTask = Collections.<Callable<Integer>> singleton(new MinFinderTaskWithInvokeAll(new int[] {}, 2, 0, null));
            fail("Unexpected result: " + executor.invokeAny(oneFailingTask));
        } catch (ExecutionException x) {
            if (!(x.getCause() instanceof ArrayIndexOutOfBoundsException))
                throw x;
        }

        // invokeAny task raises InterruptedException on same thread
        Thread.currentThread().interrupt();
        try {
            Collection<Callable<Boolean>> interruptedTask = Collections.<Callable<Boolean>> singleton(new CountDownTask(new CountDownLatch(0), new CountDownLatch(1), TIMEOUT_NS));
            fail("Unexpected result when interrupted: " + executor.invokeAny(interruptedTask));
        } catch (InterruptedException x) {
        } // pass

        // shutdownNow while running invokeAny
        CountDownLatch invokeAnyBlocker = new CountDownLatch(1);
        CountDownLatch invokeAnyTaskStarted = new CountDownLatch(1);
        Future<List<Runnable>> shutdownFuture = testThreads.submit(new ShutdownTask(executor, true, new CountDownLatch(0), invokeAnyTaskStarted, TIMEOUT_NS));

        try {
            Callable<Boolean> blockerTask = new CountDownTask(invokeAnyTaskStarted, new CountDownLatch(1), TIMEOUT_NS * 2);
            fail("Unexpected result when shutdownNow: " + executor.invokeAny(Collections.singleton(blockerTask)));
        } catch (CancellationException x) {
        } // pass

        List<Runnable> canceledFromQueue = shutdownFuture.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
        assertEquals(0, canceledFromQueue.size());
    }

    // Submit a group of tasks via untimed invokeAny. Allow the tasks to be enqueued, but blocked from starting because another thread
    // holds the only permit against maxConcurrency. Then invoke shutdownNow on the executor, all of the queued tasks should be canceled
    // without starting, allowing invokeAny to raise a CancellationException before reaching the timeout.
    @Test
    public void testInvokeAnyShutdownNowWhileEnqueued() throws Exception {
        PolicyExecutor executor = provider.create("testInvokeAnyShutdownNowWhileEnqueued")
                        .maxConcurrency(1)
                        .maxPolicy(MaxPolicy.strict)
                        .maxQueueSize(2);

        CountDownLatch blocker = new CountDownLatch(1);
        CountDownLatch blockerStarted = new CountDownLatch(1);
        Future<Boolean> blockerFuture = executor.submit(new CountDownTask(blockerStarted, blocker, TIMEOUT_NS * 2));
        assertTrue(blockerStarted.await(TIMEOUT_NS, TimeUnit.NANOSECONDS));

        AtomicInteger counter = new AtomicInteger(0);
        List<Callable<Integer>> tasks = Arrays.<Callable<Integer>> asList(new SharedIncrementTask(counter), new SharedIncrementTask(counter));

        final CountDownLatch noQueueCapacityRemains = new CountDownLatch(1);
        // Register a callback to decrement the above latch once queue capacity is used up.
        assertNull(executor.registerQueueSizeCallback(1, new CountDownCallback(noQueueCapacityRemains)));

        Future<List<Runnable>> shutdownFuture = testThreads.submit(new ShutdownTask(executor, true, new CountDownLatch(0), noQueueCapacityRemains, TIMEOUT_NS));

        int expectedCancels = 0;

        long start = System.nanoTime();
        try {
            fail("Should not succeed after shutdownNow: " + executor.invokeAny(tasks));
        } catch (CancellationException x) { // pass
            expectedCancels = 2; // ShutdownTask triggered by queue size callback runs after invokeAny's second enqueue returns, so there are 2 queued tasks to cancel
        } catch (RejectedExecutionException x) { // pass
            if (!x.getMessage().startsWith("CWWKE1202E")) // submit rejected due to shutdown
                throw x;
            expectedCancels = 1; // ShutdownTask triggered by queue size callback runs before invokeAny's second enqueue returns, so there is 1 queued task to cancel. The other was rejected.
        }
        long duration = System.nanoTime() - start;

        // invokeAny must return prematurely due to cancel by shutdownNow
        assertTrue(duration + "ns", duration < TIMEOUT_NS);

        List<Runnable> canceledFromQueue = shutdownFuture.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
        assertEquals(expectedCancels, canceledFromQueue.size());
    }

    // Submit a group of tasks via untimed invokeAny. Have all of the tasks block and then invoke shutdownNow on the executor.
    // All of the tasks should be canceled and interrupted, allowing invokeAny to raise a CancellationException before reaching the timeout.
    @Test
    public void testInvokeAnyShutdownNowWhileRunning() throws Exception {
        PolicyExecutor executor = provider.create("testInvokeAnyShutdownNowWhileRunning");

        final int numTasks = 2;
        CountDownLatch blocker = new CountDownLatch(1);
        CountDownLatch allInvokeAnyTasksBeginLatch = new CountDownLatch(numTasks);
        List<Callable<Boolean>> tasks = new ArrayList<Callable<Boolean>>(numTasks);
        for (int i = 0; i < numTasks; i++)
            tasks.add(new CountDownTask(allInvokeAnyTasksBeginLatch, blocker, TIMEOUT_NS * 2));

        Future<List<Runnable>> shutdownFuture = testThreads.submit(new ShutdownTask(executor, true, new CountDownLatch(0), allInvokeAnyTasksBeginLatch, TIMEOUT_NS));

        long start = System.nanoTime();
        try {
            fail("Should not succeed after shutdownNow: " + executor.invokeAny(tasks));
        } catch (CancellationException x) { // pass
        }
        long duration = System.nanoTime() - start;

        // invokeAny must return prematurely due to cancel/interrupt by shutdownNow
        assertTrue(duration + "ns", duration < TIMEOUT_NS);

        List<Runnable> canceledFromQueue = shutdownFuture.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
        assertEquals(0, canceledFromQueue.size());
    }

    // Basic test of timed invokeAny. Submit a group of tasks, of which one raises an exception,
    // another runs for longer than the timeout, and another completes successfully.
    // The result of the one that completed successfully should be returned,
    // and the duration of the invokeAny method should not exceed the timeout.
    @Test
    public void testInvokeAnyTimed1Successful() throws Exception {
        PolicyExecutor executor = provider.create("testInvokeAnyTimed1Successful");

        CountDownLatch beginLatchForBlocker = new CountDownLatch(1);
        CountDownLatch blocker = new CountDownLatch(1);
        CountDownLatch unused = new CountDownLatch(0);

        List<Callable<Boolean>> tasks = new ArrayList<Callable<Boolean>>();
        tasks.add(new CountDownTask(unused, null, 0)); // intentionally cause NullPointerException
        tasks.add(new CountDownTask(beginLatchForBlocker, blocker, TIMEOUT_NS * 2)); // block for longer than the timeout
        tasks.add(new CountDownTask(unused, beginLatchForBlocker, TIMEOUT_NS / 2)); // should succeed

        long start = System.nanoTime();
        assertTrue(executor.invokeAny(tasks, TIMEOUT_NS, TimeUnit.NANOSECONDS));
        long duration = System.nanoTime() - start;

        assertTrue(duration + "ns", duration < TIMEOUT_NS);

        List<Runnable> canceledFromQueue = executor.shutdownNow();
        assertEquals(0, canceledFromQueue.size());
    }

    // Submit a group of tasks to invokeAny, of which one raises an exception and
    // another runs for longer than the timeout. TimeoutException should occur.
    @Test
    public void testInvokeAnyTimed1TimesOut1Fails() throws Exception {
        PolicyExecutor executor = provider.create("testInvokeAnyTimed1TimesOut1Fails");

        CountDownLatch blocker = new CountDownLatch(1);
        CountDownLatch unused = new CountDownLatch(0);

        List<Callable<Boolean>> tasks = new ArrayList<Callable<Boolean>>();
        tasks.add(new CountDownTask(unused, null, 0)); // intentionally cause NullPointerException
        tasks.add(new CountDownTask(unused, blocker, TIMEOUT_NS * 2)); // block for longer than the timeout

        long start = System.nanoTime();
        try {
            fail("Should not return value: " + executor.invokeAny(tasks, 200, TimeUnit.MILLISECONDS));
        } catch (TimeoutException x) { // pass
        }
        long duration = System.nanoTime() - start;

        assertTrue(duration + "ns", duration < TIMEOUT_NS); // higher value used to allow for slowness and pauses in test systems

        List<Runnable> canceledFromQueue = executor.shutdownNow();
        assertEquals(0, canceledFromQueue.size());
    }

    // Submit a group of tasks to timed invokeAny, where all fail during execution.
    // Verify that an ExecutionException is raised which chains one of the failures.
    @Test
    public void testInvokeAnyTimedAllFail() throws Exception {
        PolicyExecutor executor = provider.create("testInvokeAnyTimedAllFail");

        final int numTasks = 3;

        List<Callable<Integer>> tasks = new LinkedList<Callable<Integer>>();
        for (int i = 0; i < numTasks; i++)
            tasks.add(new MinFinderTaskWithInvokeAll(new int[] {}, 2, 3, executor)); // cause ArrayIndexOutOfBoundsException

        long start = System.nanoTime();
        try {
            int result = executor.invokeAny(tasks, TIMEOUT_NS * 2, TimeUnit.NANOSECONDS);
            fail("invokeAny should not have result when all tasks fail: " + result);
        } catch (ExecutionException x) {
            if (!(x.getCause() instanceof ArrayIndexOutOfBoundsException))
                throw x;
        }
        long duration = System.nanoTime() - start;

        assertTrue(duration + "ns", duration < TIMEOUT_NS);

        List<Runnable> canceledFromQueue = executor.shutdownNow();
        assertEquals(0, canceledFromQueue.size());
    }

    // Submit a group of tasks to timed invokeAny, where all are able to complete successfully.
    // Verify that invokeAny returns the result of one of the successful tasks.
    @Test
    public void testInvokeAnyTimedAllSuccessful() throws Exception {
        PolicyExecutor executor = provider.create("testInvokeAnyTimedAllSuccessful")
                        .startTimeout(TimeUnit.NANOSECONDS.toMillis(TIMEOUT_NS));

        final int numTasks = 3;
        AtomicInteger counter = new AtomicInteger();

        List<Callable<Integer>> tasks = new LinkedList<Callable<Integer>>();
        for (int i = 0; i < numTasks; i++)
            tasks.add(new SharedIncrementTask(counter));

        long start = System.nanoTime();
        int result = executor.invokeAny(tasks, TIMEOUT_NS * 2, TimeUnit.NANOSECONDS);
        long duration = System.nanoTime() - start;

        assertTrue(Integer.toString(result), result >= 1 && result <= numTasks);
        assertTrue(duration + "ns", duration < TIMEOUT_NS);

        int count = counter.get();
        assertTrue(Integer.toString(count), count >= 1 && count <= numTasks);

        List<Runnable> canceledFromQueue = executor.shutdownNow();
        assertEquals(0, canceledFromQueue.size());
    }

    // Submit a group of tasks to timed invokeAny, where all tasks block and exceed the timeout.
    // Verify that invokeAny raises TimeoutException.
    @Test
    public void testInvokeAnyTimedAllTimeout() throws Exception {
        PolicyExecutor executor = provider.create("testInvokeAnyTimedAllTimeout");

        final int numTasks = 3;

        CountDownLatch unused = new CountDownLatch(0);
        CountDownLatch blocker = new CountDownLatch(1);
        List<Callable<Boolean>> tasks = new LinkedList<Callable<Boolean>>();
        for (int i = 0; i < numTasks; i++)
            tasks.add(new CountDownTask(unused, blocker, TIMEOUT_NS * 2));

        long start = System.nanoTime();
        try {
            boolean result = executor.invokeAny(tasks, 200, TimeUnit.MILLISECONDS);
            fail("invokeAny should time out, instead: " + result);
        } catch (TimeoutException x) { // pass
        }
        long duration = System.nanoTime() - start;

        assertTrue(duration + "ns", duration < TIMEOUT_NS); // higher value used to allow for slowness and pauses in test systems

        List<Runnable> canceledFromQueue = executor.shutdownNow();
        assertEquals(0, canceledFromQueue.size());
    }

    // Submit a group of tasks to timed invokeAny, one of which raises an exception and another of which blocks.
    // Interrupt the invokeAny operation before it completes. Expect InterruptedException.
    @Test
    public void testInvokeAnyTimedInterruptWaitForCompletion() throws Exception {
        PolicyExecutor executor = provider.create("testInvokeAnyTimedInterruptWaitForCompletion");

        CountDownLatch beginLatch = new CountDownLatch(2);
        CountDownLatch blocker = new CountDownLatch(1);
        Collection<CountDownTask> tasks = Arrays.asList(new CountDownTask(beginLatch, null, 0), // Intentionally fail with NullPointerException
                                                        new CountDownTask(beginLatch, blocker, TIMEOUT_NS * 2)); // block

        // Interrupt the current thread after both tasks start
        Future<?> interrupterFuture = testThreads.submit(new InterrupterTask(Thread.currentThread(), beginLatch, TIMEOUT_NS * 2, TimeUnit.NANOSECONDS));

        long start = System.nanoTime();
        try {
            fail("Should have been interrupted. Instead: " + executor.invokeAny(tasks, TIMEOUT_NS * 2, TimeUnit.NANOSECONDS));
        } catch (InterruptedException x) { // pass
        }
        long duration = System.nanoTime() - start;

        assertTrue("Interrupt should happen promptly, instead took " + duration + "ns", duration < TIMEOUT_NS); // allow for slowness and pauses in test systems

        interrupterFuture.get();

        List<Runnable> canceledFromQueue = executor.shutdownNow();
        assertEquals(0, canceledFromQueue.size());
    }

    // Submit a group of tasks to timed invokeAny, one of which raises an exception, another of which blocks all others from starting,
    // another of which is stuck in the queue, and another of which is blocked attempting to get a queue position.
    // Interrupt the invokeAny operation before it completes. Expect InterruptedException.
    @Test
    public void testInvokeAnyTimedInterruptWaitForEnqueue() throws Exception {
        PolicyExecutor executor = provider.create("testInvokeAnyTimedInterruptWaitForEnqueue")
                        .maxConcurrency(1)
                        .maxQueueSize(1)
                        .maxWaitForEnqueue(TimeUnit.NANOSECONDS.toMillis(TIMEOUT_NS * 4));

        CountDownLatch beginLatch_1_2 = new CountDownLatch(2);
        CountDownLatch blocker = new CountDownLatch(1);
        CountDownLatch unused = new CountDownLatch(0);
        Collection<CountDownTask> tasks = Arrays.asList(new CountDownTask(beginLatch_1_2, null, 0), // Intentionally fail with NullPointerException
                                                        new CountDownTask(beginLatch_1_2, blocker, TIMEOUT_NS * 2), // block
                                                        new CountDownTask(unused, blocker, TIMEOUT_NS * 2), // stuck in queue until invokeAny is interrupted
                                                        new CountDownTask(unused, blocker, TIMEOUT_NS * 2)); // blocked from entering queue until invokeAny is interrupted

        // Interrupt the current thread after the first two tasks start
        Future<?> interrupterFuture = testThreads.submit(new InterrupterTask(Thread.currentThread(), beginLatch_1_2, TIMEOUT_NS * 2, TimeUnit.NANOSECONDS));

        long start = System.nanoTime();
        try {
            fail("Should have been interrupted. Instead: " + executor.invokeAny(tasks, TIMEOUT_NS * 2, TimeUnit.NANOSECONDS));
        } catch (InterruptedException x) { // pass
        }
        long duration = System.nanoTime() - start;

        assertTrue("Interrupt should happen promptly, instead took " + duration + "ns", duration < TIMEOUT_NS); // allow for slowness and pauses in test systems

        interrupterFuture.get();

        List<Runnable> canceledFromQueue = executor.shutdownNow();
        assertEquals(0, canceledFromQueue.size()); // third task should have already been canceled upon invokeAny terminating with InterruptedException
    }

    // Submit a group of tasks via timed invokeAny, where one of the tasks promptly returns a null result, and another returns a non-null result after a delay.
    // Verify that the result of invokeAny is null. This test verifies that the implementation is not limited to awaiting non-null results, and handles null properly.
    // Also verifies that the other task ends before it otherwise would have due to the implicit cancel/interrupt upon return of invokeAny.
    @Test
    public void testInvokeAnyTimedNullSuccessfulResult() throws Exception {
        PolicyExecutor executor = provider.create("testInvokeAnyTimedNullSuccessfulResult");

        CountDownLatch blocker = new CountDownLatch(1);
        CountDownTask task1 = new CountDownTask(new CountDownLatch(0), blocker, TIMEOUT_NS * 3);

        SharedIncrementTask task2 = new SharedIncrementTask();

        List<Callable<Boolean>> tasks = Arrays.asList(task1, Executors.callable(task2, (Boolean) null));

        assertNull(executor.invokeAny(tasks, TIMEOUT_NS, TimeUnit.NANOSECONDS));

        // Another way to verify that task2 completed
        assertEquals(1, task2.count());

        // Verify that task1 ends prior to when it would have otherwise completed. This is due to cancel/interruption upon return from invokeAny.
        for (long start = System.nanoTime(); task1.executionThreads.peek() != null && System.nanoTime() - start < TIMEOUT_NS; Thread.sleep(100));
        assertNull(task1.executionThreads.peek());

        List<Runnable> canceledFromQueue = executor.shutdownNow();
        assertEquals(0, canceledFromQueue.size());
    }

    // Submit a group of tasks via timed invokeAny. Allow the tasks to be enqueued, but blocked from starting because another thread
    // holds the only permit against maxConcurrency. Then invoke shutdownNow on the executor, all of the queued tasks should be canceled
    // without starting, allowing invokeAny to raise a CancellationException before reaching the timeout.
    @Test
    public void testInvokeAnyTimedShutdownNowWhileEnqueued() throws Exception {
        PolicyExecutor executor = provider.create("testInvokeAnyTimedShutdownNowWhileEnqueued")
                        .maxConcurrency(1)
                        .maxQueueSize(2);

        CountDownLatch blocker = new CountDownLatch(1);
        CountDownLatch blockerStarted = new CountDownLatch(1);
        Future<Boolean> blockerFuture = executor.submit(new CountDownTask(blockerStarted, blocker, TIMEOUT_NS * 2));
        assertTrue(blockerStarted.await(TIMEOUT_NS, TimeUnit.NANOSECONDS));

        AtomicInteger counter = new AtomicInteger(0);
        List<Callable<Integer>> tasks = Arrays.<Callable<Integer>> asList(new SharedIncrementTask(counter), new SharedIncrementTask(counter));

        final CountDownLatch noQueueCapacityRemains = new CountDownLatch(1);
        // Register a callback to decrement the above latch once queue capacity is used up.
        assertNull(executor.registerQueueSizeCallback(1, new CountDownCallback(noQueueCapacityRemains)));

        Future<List<Runnable>> shutdownFuture = testThreads.submit(new ShutdownTask(executor, true, new CountDownLatch(0), noQueueCapacityRemains, TIMEOUT_NS));

        int expectedCancels = 0;

        long start = System.nanoTime();
        try {
            fail("Should not succeed after shutdownNow: " + executor.invokeAny(tasks, TIMEOUT_NS * 3, TimeUnit.NANOSECONDS));
        } catch (CancellationException x) { // pass
            expectedCancels = 2; // ShutdownTask triggered by queue size callback runs after invokeAny's second enqueue returns, so there are 2 queued tasks to cancel
        } catch (RejectedExecutionException x) { // pass
            if (!x.getMessage().startsWith("CWWKE1202E")) // submit rejected due to shutdown
                throw x;
            expectedCancels = 1; // ShutdownTask triggered by queue size callback runs before invokeAny's second enqueue returns, so there is 1 queued task to cancel. The other was rejected.
        }
        long duration = System.nanoTime() - start;

        // invokeAny must return prematurely due to cancel by shutdownNow
        assertTrue(duration + "ns", duration < TIMEOUT_NS);

        List<Runnable> canceledFromQueue = shutdownFuture.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
        assertEquals(expectedCancels, canceledFromQueue.size());
    }

    // Submit a group of tasks via timed invokeAny. Have all of the tasks block and then invoke shutdownNow on the executor.
    // All of the tasks should be canceled and interrupted, allowing invokeAny to raise a CancellationException before reaching the timeout.
    @Test
    public void testInvokeAnyTimedShutdownNowWhileRunning() throws Exception {
        PolicyExecutor executor = provider.create("testInvokeAnyTimedShutdownNowWhileRunning");

        final int numTasks = 3;
        CountDownLatch blocker = new CountDownLatch(1);
        CountDownLatch allInvokeAnyTasksBeginLatch = new CountDownLatch(numTasks);
        List<Callable<Boolean>> tasks = new ArrayList<Callable<Boolean>>(numTasks);
        for (int i = 0; i < numTasks; i++)
            tasks.add(new CountDownTask(allInvokeAnyTasksBeginLatch, blocker, TIMEOUT_NS * 2));

        Future<List<Runnable>> shutdownFuture = testThreads.submit(new ShutdownTask(executor, true, new CountDownLatch(0), allInvokeAnyTasksBeginLatch, TIMEOUT_NS));

        long start = System.nanoTime();
        try {
            fail("Should not succeed after shutdownNow: " + executor.invokeAny(tasks, TIMEOUT_NS * 3, TimeUnit.NANOSECONDS));
        } catch (CancellationException x) { // pass
        }
        long duration = System.nanoTime() - start;

        // invokeAny must return prematurely due to cancel/interrupt by shutdownNow
        assertTrue(duration + "ns", duration < TIMEOUT_NS);

        List<Runnable> canceledFromQueue = shutdownFuture.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
        assertEquals(0, canceledFromQueue.size());
    }

    // Submit a group of tasks to timed invokeAny, where there are insufficient queue positions to
    // enqueue the tasks and the timeout occurs while waiting or a queue position to become available
    // rather than while running the task.
    @Test
    public void testInvokeAnyTimedTimeoutDuringWaitForEnqueue() throws Exception {
        PolicyExecutor executor = provider.create("testInvokeAnyTimedTimeoutDuringWaitForEnqueue")
                        .maxConcurrency(1)
                        .maxQueueSize(1)
                        .maxWaitForEnqueue(TimeUnit.MINUTES.toMillis(4));

        AtomicInteger counter = new AtomicInteger();

        // Submit blocking task to use up the single maxConcurrency permit
        CountDownLatch unused = new CountDownLatch(0);
        CountDownLatch blocker = new CountDownLatch(1);
        Future<Boolean> future1 = executor.submit(new CountDownTask(unused, blocker, TimeUnit.MINUTES.toNanos(5)));

        // Submit another task to use up the single queue position
        Future<?> future2 = executor.submit((Runnable) new SharedIncrementTask(counter));

        final int numInvokeAnyTasks = 2;
        List<Callable<Integer>> tasks = new LinkedList<Callable<Integer>>();
        for (int i = 0; i < numInvokeAnyTasks; i++)
            tasks.add(new SharedIncrementTask(counter));

        long start = System.nanoTime();
        try {
            int result = executor.invokeAny(tasks, 200, TimeUnit.MILLISECONDS);
            fail("invokeAny should be rejected when unable to submit task, instead: " + result);
        } catch (RejectedExecutionException x) { // pass
        }
        long duration = System.nanoTime() - start;

        assertTrue(duration + "ns", duration < TIMEOUT_NS); // higher value used to allow for slowness and pauses in test systems

        List<Runnable> canceledFromQueue = executor.shutdownNow();
        assertEquals(1, canceledFromQueue.size());
        assertTrue(future1.isCancelled());
    }

    // Poll isTerminated until the executor terminates while concurrently awaiting termination from another thread.
    // The awaitTermination operation should recognize the state transition that is triggered by invocation of the isTerminated method.
    @Test
    public void testIsTerminatedWhileAwaitingTermination() throws Exception {
        ExecutorService executor = provider.create("testIsTerminatedWhileAwaitingTermination").maxConcurrency(3);

        // start a thread to await termination
        Future<Boolean> terminationFuture = testThreads.submit(new TerminationAwaitTask(executor, TIMEOUT_NS * 5));

        AtomicInteger counter = new AtomicInteger();
        final int numSubmitted = 15;
        List<Runnable> tasks = new ArrayList<Runnable>();
        Future<?>[] futures = new Future<?>[numSubmitted];

        for (int i = 0; i < numSubmitted; i++)
            tasks.add(new SharedIncrementTask(counter));

        for (int i = 0; i < numSubmitted; i++)
            futures[i] = executor.submit(tasks.get(i));

        List<Runnable> canceledFromQueue = executor.shutdownNow();

        // poll for termination
        for (long start = System.nanoTime(); !executor.isTerminated() && System.nanoTime() - start < TIMEOUT_NS * 4; TimeUnit.MILLISECONDS.sleep(100));

        assertTrue(executor.isTerminated());

        // awaitTermination should complete within a reasonable amount of time after isTerminated transitions the state
        assertTrue(terminationFuture.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));

        // verify that tasks canceled from the queue report that they are canceled
        for (Runnable task : canceledFromQueue) {
            int i = tasks.indexOf(task);
            assertNotSame("unknown task reported canceled from queue: " + task, -1, i);
            System.out.println("Task #" + i + " canceled from queue");

            assertTrue("task" + i, futures[i].isCancelled());
            assertTrue("task" + i, futures[i].isDone());
        }

        int numCanceled = 0;
        for (int i = 0; i < numSubmitted; i++) {
            System.out.println("Future #" + i);
            if (futures[i].isCancelled()) {
                numCanceled++;
                assertTrue(futures[i].isDone());
            }
        }

        int numCanceledFromQueue = canceledFromQueue.size();

        int count = counter.get();
        System.out.println(count + " tasks either completed successfully or were canceled during execution.");
        System.out.println(numCanceled + " tasks were canceled, either during execution or from the queue.");
        System.out.println(numCanceledFromQueue + " tasks were canceled from the queue.");

        assertTrue(count + numCanceledFromQueue <= numSubmitted);
        assertTrue(numCanceledFromQueue <= numCanceled);
    }

    // Poll isTerminated until the executor (which was previously unused) terminates while concurrently awaiting termination from another thread.
    // The awaitTermination operation should recognize the state transition that is triggered by invocation of the isTerminated method.
    @Test
    public void testIsTerminatedWhileAwaitingTerminationOfUnusedExecutor() throws Exception {
        ExecutorService executor = provider.create("testIsTerminatedWhileAwaitingTerminationOfUnusedExecutor");

        // start a thread to await termination
        Future<Boolean> terminationFuture = testThreads.submit(new TerminationAwaitTask(executor, TIMEOUT_NS * 2));

        executor.shutdown();

        // poll for termination
        for (long start = System.nanoTime(); !executor.isTerminated() && System.nanoTime() - start < TIMEOUT_NS; TimeUnit.MILLISECONDS.sleep(100));

        assertTrue(executor.isTerminated());

        // awaitTermination should complete within a reasonable amount of time after isTerminated transitions the state
        assertTrue(terminationFuture.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
    }

    // Register late start callbacks with a policy executor. Verify that at most one can be registered,
    // that the most recently registered replaces any previous ones, and that the late start callback
    // can be unregistered by supplying null. Verify that the late start callback is notified when a
    // task starts after the designated threshold.
    @Test
    public void testLateStartCallback() throws Exception {
        PolicyExecutor executor = provider.create("testLateStartCallback")
                        .maxConcurrency(1)
                        .maxQueueSize(1)
                        .maxWaitForEnqueue(TimeUnit.NANOSECONDS.toMillis(TIMEOUT_NS));

        CountDownLatch lateBy3MinutesLatch = new CountDownLatch(1);
        Runnable lateBy3MinutesCallback = new CountDownCallback(lateBy3MinutesLatch);
        assertNull(executor.registerLateStartCallback(3, TimeUnit.MINUTES, lateBy3MinutesCallback));

        // use up maxConcurrency
        CountDownLatch blocker1StartedLatch = new CountDownLatch(1);
        CountDownLatch blockerContinueLatch = new CountDownLatch(1);
        CountDownTask blocker1Task = new CountDownTask(blocker1StartedLatch, blockerContinueLatch, TimeUnit.MINUTES.toNanos(9));
        Future<Boolean> blocker1Future = executor.submit(blocker1Task);

        // queue another task
        PolicyTaskFuture<Integer> lateTaskFuture = executor.submit(new SharedIncrementTask(), 1, null);
        assertTrue(blocker1StartedLatch.await(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertEquals(1, lateBy3MinutesLatch.getCount());

        // new registration replaces previous
        CountDownLatch lateBy200MillisLatch = new CountDownLatch(1);
        Runnable lateBy200MillisCallback = new CountDownCallback(lateBy200MillisLatch);
        assertEquals(lateBy3MinutesCallback, executor.registerLateStartCallback(200, TimeUnit.MILLISECONDS, lateBy200MillisCallback));

        // wait for task to become late, and then allow it to run
        TimeUnit.MILLISECONDS.sleep(300 - lateTaskFuture.getElapsedAcceptTime(TimeUnit.MILLISECONDS) - lateTaskFuture.getElapsedQueueTime(TimeUnit.MILLISECONDS)); // extra 100ms tolerance
        blockerContinueLatch.countDown();

        assertTrue(lateBy200MillisLatch.await(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertEquals(1, lateBy3MinutesLatch.getCount());

        // register another callback which hasn't been reached
        assertNull(executor.registerLateStartCallback(3, TimeUnit.MINUTES, lateBy3MinutesCallback));
        assertEquals(1, lateBy3MinutesLatch.getCount());

        assertEquals(Integer.valueOf(1), lateTaskFuture.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));

        assertTrue(blocker1Future.isDone());
        assertFalse(blocker1Future.isCancelled());

        executor.shutdown();
        assertTrue(executor.awaitTermination(TIMEOUT_NS, TimeUnit.NANOSECONDS));

        assertEquals(1, lateBy3MinutesLatch.getCount()); // never invoked

        try {
            Runnable previous = executor.registerLateStartCallback(5, TimeUnit.SECONDS, new CountDownCallback(new CountDownLatch(1)));
            fail("Should not be able to register callback after shutdown. Result of register was: " + previous);
        } catch (IllegalStateException x) {
        } // pass
    }

    // Invoke groups of tasks on the policy executor that break themselves into multiple tasks that also
    // invoke groups of tasks on the policy executor, which in turn break themselves down and
    // invoke more groups of tasks, and so forth.
    @Test
    public void testMultipleLayersOfInvokeAll() throws Exception {
        PolicyExecutor executor = provider.create("testMultipleLayersOfInvokeAll")
                        .expedite(0) // everything could run on the current thread if it needs to
                        .maxConcurrency(3)
                        .maxQueueSize(4); // just enough to ensure we can cover a 16 element array

        int[] array1 = new int[] { 25, 85, 95, 25, 45, 15, 75, 75, 65, 35, 95, 105, 45, 35, 85, 25 };
        System.out.println("Searching for minimum of " + Arrays.toString(array1));
        assertEquals(Integer.valueOf(15), executor.invokeAll(Collections.singleton(new MinFinderTaskWithInvokeAll(array1, executor))).get(0).get(0, TimeUnit.SECONDS));

        // Force more of the tasks run on the current thread
        executor.maxConcurrency(1);
        int[] array2 = new int[] { 24, 26, 29, 27, 23, 21, 28, 26, 22, 25, 29, 21, 27, 29, 20, 23 };
        System.out.println("Searching for minimum of " + Arrays.toString(array2));
        assertEquals(Integer.valueOf(20), executor.invokeAll(Collections.singleton(new MinFinderTaskWithInvokeAll(array2, executor))).get(0).get(0, TimeUnit.SECONDS));

        List<Runnable> canceledFromQueue = executor.shutdownNow();
        assertEquals(0, canceledFromQueue.size());
    }

    // Submit a task to the policy executor that breaks itself into multiple tasks that also submit to the policy executor,
    // which in turn break themselves down and submit multiple tasks to the policy executor, and so forth.
    @Test
    public void testMultipleLayersOfSubmits() throws Exception {
        ExecutorService executor = provider.create("testMultipleLayersOfSubmits")
                        .expedite(8)
                        .maxConcurrency(8) // just enough to ensure we can cover a 16 element array
                        .maxQueueSize(3)
                        .runIfQueueFull(true);

        int[] array1 = new int[] { 2, 9, 3, 5, 1, 3, 6, 3, 8, 0, 4, 4, 10, 2, 1, 8 };
        System.out.println("Searching for minimum of " + Arrays.toString(array1));
        assertEquals(Integer.valueOf(0), executor.submit(new MinFinderTask(array1, executor)).get(TIMEOUT_NS * 5, TimeUnit.NANOSECONDS));

        int[] array2 = new int[] { 5, 20, 18, 73, 64, 102, 6, 62, 12, 31 };
        System.out.println("Searching for minimum of " + Arrays.toString(array2));
        assertEquals(Integer.valueOf(5), executor.submit(new MinFinderTask(array2, executor)).get(TIMEOUT_NS * 5, TimeUnit.NANOSECONDS));

        int[] array3 = new int[] { 80, 20, 40, 70, 30, 90, 90, 50, 10 };
        System.out.println("Searching for minimum of " + Arrays.toString(array3));
        assertEquals(Integer.valueOf(10), executor.submit(new MinFinderTask(array3, executor)).get(TIMEOUT_NS * 5, TimeUnit.NANOSECONDS));

        executor.shutdown();
        assertTrue(executor.awaitTermination(TIMEOUT_NS, TimeUnit.NANOSECONDS));
    }

    // Register queue size callbacks with a policy executor. Verify that at most one can be registered,
    // that the most recently registered replaces any previous ones, and that the queue size callback
    // can be unregistered by supplying null. Verify that the queue size callback is notified when the
    // queue size drops below the threshold.
    @Test
    public void testQueueSizeCallback() throws Exception {
        PolicyExecutor executor = provider.create("testQueueSizeCallback")
                        .maxConcurrency(1)
                        .maxQueueSize(5);

        CountDownLatch under5Latch = new CountDownLatch(1);
        Runnable callbackUnder5 = new CountDownCallback(under5Latch);
        assertNull(executor.registerQueueSizeCallback(5, callbackUnder5));
        assertEquals(1, under5Latch.getCount());

        // new registration replaces previous
        CountDownLatch under4Latch = new CountDownLatch(1);
        Runnable callbackUnder4 = new CountDownCallback(under4Latch);
        assertEquals(callbackUnder5, executor.registerQueueSizeCallback(4, callbackUnder4));
        assertEquals(1, under4Latch.getCount());

        // previously registered callback is not invoked
        CountDownLatch blockerStartedLatch = new CountDownLatch(1);
        CountDownLatch blockerContinueLatch = new CountDownLatch(1);
        CountDownTask blockerTask = new CountDownTask(blockerStartedLatch, blockerContinueLatch, TimeUnit.MINUTES.toNanos(8));
        Future<Boolean> blockerFuture = executor.submit(blockerTask);
        assertTrue(blockerStartedLatch.await(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertEquals(1, under5Latch.getCount());
        assertEquals(1, under4Latch.getCount());

        // add a task to the queue (will be stuck until blocker completes), such that capacity of 4 remains
        Future<Integer> queuedFuture1 = executor.submit((Callable<Integer>) new SharedIncrementTask());
        assertEquals(1, under4Latch.getCount());
        assertEquals(1, under5Latch.getCount());

        // trigger the active callback (under4Latch) by adding another task to the queue
        Future<Integer> queuedFuture2 = executor.submit((Callable<Integer>) new SharedIncrementTask());
        assertTrue(under4Latch.await(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertEquals(1, under5Latch.getCount());

        // register another callback
        CountDownLatch under3Latch = new CountDownLatch(1);
        Runnable callbackUnder3 = new CountDownCallback(under3Latch);
        assertNull(executor.registerQueueSizeCallback(3, callbackUnder3));
        assertEquals(1, under3Latch.getCount());

        // trigger the active callback (under3Latch) by shrinking the queue
        executor.maxQueueSize(4);
        assertTrue(under3Latch.await(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertEquals(1, under5Latch.getCount());

        // register a callback for a threshold that has already been reached
        CountDownLatch under6Latch = new CountDownLatch(1);
        Runnable callbackUnder6 = new CountDownCallback(under6Latch);
        assertNull(executor.registerQueueSizeCallback(6, callbackUnder6));
        assertTrue(under6Latch.await(TIMEOUT_NS, TimeUnit.NANOSECONDS));

        List<Runnable> canceledFromQueue = executor.shutdownNow();
        assertEquals(2, canceledFromQueue.size());
        assertTrue(blockerFuture.isCancelled());

        try {
            Runnable previous = executor.registerQueueSizeCallback(10, new CountDownCallback(new CountDownLatch(1)));
            fail("Should not be able to register callback after shutdown. Result of register was: " + previous);
        } catch (IllegalStateException x) {
        } // pass
    }

    // Use a policy executor to submit tasks that resubmit themselves to perform a recursive computation.
    // Have one of the recursive task require greater concurrency than provided by the policy executor
    // such that it hangs. Then interrupt it to free up a thread to enable completion.
    @Test
    public void testRecursiveTasks() throws Exception {
        PolicyExecutor executor = provider.create("testRecursiveTasks")
                        .expedite(8)
                        .maxConcurrency(10)
                        .maxQueueSize(10) // just enough to cover factorials of 6, 4, and 3 without getting stuck
                        .maxWaitForEnqueue(TimeUnit.NANOSECONDS.toMillis(TIMEOUT_NS * 2));
        Future<Long> f6 = executor.submit(new FactorialTask(6, executor));
        Future<Long> f4 = executor.submit(new FactorialTask(4, executor));
        Future<Long> f3 = executor.submit(new FactorialTask(3, executor));

        assertEquals(Long.valueOf(720), f6.get(TIMEOUT_NS * 6, TimeUnit.NANOSECONDS));
        assertEquals(Long.valueOf(24), f4.get(TIMEOUT_NS * 4, TimeUnit.NANOSECONDS));
        assertEquals(Long.valueOf(6), f3.get(TIMEOUT_NS * 3, TimeUnit.NANOSECONDS));

        // Decrease concurrency and submit a recursive task that will hang
        executor.expedite(3).maxConcurrency(3);
        Future<Long> f5 = executor.submit(new FactorialTask(5, executor));
        try {
            fail("Should not be able to complete recursive task with insufficient concurrency: " + f5.get(200, TimeUnit.MILLISECONDS));
        } catch (TimeoutException x) {
        } // pass

        // Interrupt the original task, which will make another thread available to complete the hung recursive invocations
        assertTrue(f5.cancel(true));

        executor.shutdown();
        assertTrue(executor.awaitTermination(TIMEOUT_NS * 4, TimeUnit.NANOSECONDS));
    }

    // Submit a recursive task that hangs the policy executor. Resolve the hang by using shutdownNow to cancel all tasks.
    @Test
    public void testRecursiveTaskThatHangsPolicyExecutorThenShutdownNow() throws Exception {
        PolicyExecutor executor = provider.create("testRecursiveTaskThatHangsPolicyExecutorThenShutdownNow")
                        .maxConcurrency(4); // intentionally not enough to run a factorial task for 6+

        FactorialTask factorial8 = new FactorialTask(8, executor);
        Future<Long> future8 = executor.submit(factorial8);

        // Poll the task to find out if it has progressed sufficiently to use up all of the concurrency
        for (long start = System.nanoTime(); factorial8.num > 4 && System.nanoTime() - start < TIMEOUT_NS; TimeUnit.MILLISECONDS.sleep(200));
        assertEquals(4, factorial8.num);

        try {
            fail("Should not be able to complete recursive task with insufficient concurrency: " + future8.get(100, TimeUnit.MILLISECONDS));
        } catch (TimeoutException x) {
        } // pass

        // Canceling the original future will release one thread, but it will become blocked on the next recursive task
        assertTrue(future8.cancel(true));

        // Wait for it to use up max concurrency
        for (long start = System.nanoTime(); factorial8.num > 3 && System.nanoTime() - start < TIMEOUT_NS; TimeUnit.MILLISECONDS.sleep(200));
        assertEquals(3, factorial8.num);

        // Additional tasks will be stuck as well
        Runnable blockedTask1 = new SharedIncrementTask();
        Future<?> blockedFuture1 = executor.submit(blockedTask1);

        Runnable blockedTask2 = new SharedIncrementTask();
        Future<?> blockedFuture2 = executor.submit(blockedTask2);

        try {
            fail("Should not be able to complete task1 with insufficient concurrency: " + blockedFuture1.get(101, TimeUnit.MILLISECONDS));
        } catch (TimeoutException x) {
        } // pass

        try {
            fail("Should not be able to complete task2 with insufficient concurrency: " + blockedFuture2.get(102, TimeUnit.MILLISECONDS));
        } catch (TimeoutException x) {
        } // pass

        // Shutting down the executor isn't enough to unblock the tasks
        executor.shutdown();

        try {
            fail("Still should not be able to complete task1 with insufficient concurrency: " + blockedFuture1.get(200, TimeUnit.MILLISECONDS));
        } catch (TimeoutException x) {
        } // pass

        List<Runnable> canceledQueuedTasks = executor.shutdownNow();
        assertEquals("Tasks canceled from the queue: " + canceledQueuedTasks, 3, canceledQueuedTasks.size());

        assertTrue(canceledQueuedTasks.remove(blockedTask1));
        assertTrue(canceledQueuedTasks.remove(blockedTask2));

        // recursively submitted task is converted to a Runnable in the shutdownNow result, so it won't directly match
        Runnable factorialRunnable = canceledQueuedTasks.get(0);
        try {
            factorialRunnable.run();
            fail("Should not be able to run FactorialTask that references a policy executor that has been shut down.");
        } catch (RejectedExecutionException x) {
            if (!x.getMessage().contains("CWWKE1202E")) // rejected-due-to-shutdown message
                throw x;
        }

        assertTrue(executor.awaitTermination(TIMEOUT_NS, TimeUnit.NANOSECONDS));
    }

    // Verify that tasks submitted via the execute and submit methods run on the caller thread when the queue is full
    // if runIfQueueFull is set to true, unless maxConcurrencyAppliesToCallerThread is true and maxConcurrency is used up.
    @Test
    public void testRunIfQueueFull() throws Exception {
        PolicyExecutor executor = provider.create("testRunIfQueueFull")
                        .maxConcurrency(1)
                        .maxPolicy(MaxPolicy.loose)
                        .maxQueueSize(1)
                        .runIfQueueFull(true);

        Long currentThreadId = Thread.currentThread().getId();

        // Use up the maxConcurrency permit
        CountDownLatch blockerBeginLatch = new CountDownLatch(1);
        CountDownTask blockerTask = new CountDownTask(blockerBeginLatch, new CountDownLatch(1), TIMEOUT_NS * 2);
        Future<Boolean> blockerTaskFuture = executor.submit(blockerTask);

        // Use up the single queue position
        assertTrue(blockerBeginLatch.await(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        Future<Integer> queuedTaskFuture = executor.submit(new SharedIncrementTask(), 1);

        // Verify that execute runs immediately on same thread
        ThreadIdTask task1 = new ThreadIdTask();
        executor.execute(task1);
        assertEquals(currentThreadId, task1.threadId.get());

        // Verify that submit(Runnable) runs immediately on same thread
        Thread.currentThread().interrupt(); // as an extra test, verify that interrupted status survives across submit & run
        ThreadIdTask task2 = new ThreadIdTask();
        Future<?> future2 = executor.submit((Runnable) task2);
        assertTrue(Thread.interrupted());
        assertTrue(future2.isDone());
        assertFalse(future2.isCancelled());
        assertEquals(currentThreadId, task2.threadId.get());
        assertNull(future2.get());

        // Verify that submit(Runnable, result) runs immediately on same thread
        ThreadIdTask task3 = new ThreadIdTask();
        Future<String> future3 = executor.submit((Runnable) task3, "3");
        assertTrue(future3.isDone());
        assertFalse(future3.isCancelled());
        assertEquals(currentThreadId, task3.threadId.get());
        assertEquals("3", future3.get(1, TimeUnit.NANOSECONDS));

        // Verify that submit(Callable) runs immediately on same thread
        ThreadIdTask task4 = new ThreadIdTask();
        Future<Long> future4 = executor.submit((Callable<Long>) task4);
        assertTrue(future4.isDone());
        assertFalse(future4.isCancelled());
        assertEquals(currentThreadId, future4.get());

        // Reconfigure so that running on the caller thread requires a permit
        executor.maxPolicy(MaxPolicy.strict);

        // Shouldn't be able to run on the current thread anymore
        try {
            executor.execute(new ThreadIdTask());
            fail("Task should have been aborted with the queue full, lacking the ability to run on the caller thread.");
        } catch (RejectedExecutionException x) {
            if (!x.getMessage().startsWith("CWWKE1201E"))
                throw x;
        }

        List<Runnable> canceledFromQueue = executor.shutdownNow();
        assertEquals(1, canceledFromQueue.size());
        assertTrue(queuedTaskFuture.isCancelled());
        assertTrue(blockerTaskFuture.isCancelled());
    }

    // Use a policy executor to submit tasks that await termination of itself.
    // Also uses the executor to submit tasks that shut itself down.
    @Test
    public void testSelfAwaitTermination() throws Exception {
        final ExecutorService executor = provider.create("testSelfAwaitTermination");

        // Submit a task to await termination of the executor
        Future<Boolean> future1 = executor.submit(new TerminationAwaitTask(executor, TimeUnit.MILLISECONDS.toNanos(50)));
        assertFalse(future1.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertTrue(future1.isDone());
        assertFalse(executor.isShutdown());
        assertFalse(executor.isTerminated());

        // Submit another task to await termination of same executor.
        CountDownLatch beginLatch = new CountDownLatch(1);
        TerminationAwaitTask awaitTerminationTask = new TerminationAwaitTask(executor, TimeUnit.MINUTES.toNanos(20), beginLatch, null, 0);
        Future<Boolean> future2 = executor.submit(awaitTerminationTask);

        // Wait for the above task to start
        beginLatch.await(TIMEOUT_NS, TimeUnit.NANOSECONDS);
        // and then encourage it to start awaiting termination
        TimeUnit.MILLISECONDS.sleep(100);

        // Run shutdown and shutdownNow from tasks submitted by the executor.
        // We must submit both tasks before either issues the shutdown because shutdown prevents subsequent submits.
        CountDownLatch shutdownLatch = new CountDownLatch(1);
        CountDownLatch shutdownNowLatch = new CountDownLatch(1);
        Future<List<Runnable>> shutdownFuture = executor.submit(new ShutdownTask(executor, false, beginLatch/* no-op */, shutdownLatch, TimeUnit.MINUTES.toNanos(10)));
        Future<List<Runnable>> shutdownNowFuture = executor.submit(new ShutdownTask(executor, true, beginLatch/* no-op */, shutdownNowLatch, TimeUnit.MINUTES.toNanos(10)));

        // let the shutdown task run
        shutdownLatch.countDown();
        assertNull(shutdownFuture.get());
        assertTrue(executor.isShutdown());

        try {
            fail("Task awaiting termination shouldn't stop when executor shuts down via shutdown: " + future2.get(100, TimeUnit.NANOSECONDS));
        } catch (TimeoutException x) {
        } // pass

        assertFalse(executor.isTerminated());

        // let the shutdownNow task run
        shutdownNowLatch.countDown();
        try {
            List<Runnable> tasksCanceledFromQueue = shutdownNowFuture.get();
            assertEquals(0, tasksCanceledFromQueue.size());
        } catch (CancellationException x) {
        } // pass if cancelled due to shutdownNow

        try {
            fail("Task awaiting termination shouldn't succeed when executor shuts down via shutdownNow: " + future2.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        } catch (CancellationException x) {
        } // pass

        Throwable x = awaitTerminationTask.errorOnAwait.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS);
        assertNotNull(x);
        if (!(x instanceof InterruptedException))
            throw new RuntimeException("Unexpected error from awaitTermination task after shutdownNow. See cause.", x);

        assertTrue(executor.awaitTermination(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertTrue(executor.isTerminated());
    }

    // Submit a task that cancels itself.
    @Test
    public void testSelfCancellation() throws Exception {
        ExecutorService executor = provider.create("testSelfCancellation");
        final LinkedBlockingQueue<Future<Void>> futures = new LinkedBlockingQueue<Future<Void>>();
        Future<Void> future = executor.submit(new Callable<Void>() {
            @Override
            public Void call() throws InterruptedException {
                Future<Void> future = futures.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS);
                assertNotNull(future);
                assertTrue(future.cancel(true));

                // Perform some operation that should be rejected due to interrupting this thread
                TimeUnit.NANOSECONDS.sleep(TIMEOUT_NS * 2);
                return null;
            }
        });

        futures.add(future);

        try {
            fail("Future for self cancelling task returned " + future.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        } catch (CancellationException x) {
        } // pass

        assertTrue(future.isCancelled());
        assertTrue(future.isDone());
    }

    // Submit tasks that attempt to get their own result while they are running. The executor should detect this and immediately raise InterruptedException
    // to prevent a hang or lengthy timeout. This also tests the onSubmit callback, which is one convenient way for a task to obtain its own Future.
    @Test
    public void testSelfGet() throws Exception {
        PolicyExecutor executor = provider.create("testSelfGet");

        // submit
        SelfGetterTask task = new SelfGetterTask();
        PolicyTaskCallback callback = task;
        long start = System.nanoTime();
        Future<Object> future = executor.submit(task, callback);
        Object result = future.get(TIMEOUT_NS * 2, TimeUnit.NANOSECONDS);
        assertTrue(result.toString(), result instanceof InterruptedException);
        long duration = System.nanoTime() - start;
        assertTrue(duration + "ns", duration < TIMEOUT_NS);

        // invokeAll
        SelfGetterTask task0 = new SelfGetterTask();
        SelfGetterTask task1 = new SelfGetterTask(TIMEOUT_NS * 2, TimeUnit.NANOSECONDS);
        PolicyTaskCallback[] callbacks = new PolicyTaskCallback[] { task0, task1 };
        start = System.nanoTime();
        List<PolicyTaskFuture<Object>> futures = executor.invokeAll(Arrays.asList(task0, task1), callbacks);
        future = futures.get(0);
        result = future.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
        assertTrue(result.toString(), result instanceof InterruptedException);
        future = futures.get(1);
        result = future.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
        assertTrue(result.toString(), result instanceof InterruptedException);

        // invokeAny
        task0 = new SelfGetterTask(TIMEOUT_NS * 2, TimeUnit.NANOSECONDS);
        task1 = new SelfGetterTask();
        callbacks = new PolicyTaskCallback[] { task0, task1 };
        start = System.nanoTime();
        result = executor.invokeAny(Arrays.asList(task0, task1), callbacks);
        duration = System.nanoTime() - start;
        assertTrue(duration + "ns", duration < TIMEOUT_NS);
        assertTrue(result.toString(), result instanceof InterruptedException);

        List<Runnable> canceledFromQueue = executor.shutdownNow();
        assertEquals(0, canceledFromQueue.size());
    }

    // Tests behavior when shutdown occurs while timed invokeAll is submitting and running tasks. Supply a group of 5 tasks to invokeAll.
    // The first task should complete successfully. The second task shuts down the executor. When the executor shuts down, the third and
    // fourth tasks might or might not have been enqueued, depending on timing. However, the fifth task will not have been enqueued yet due to a
    // maximum concurrency of 1 and maximum queue size of 2. The shutdown should prevent further enqueuing of tasks which causes the invokeAll
    // operation to be rejected.
    @SuppressWarnings("unchecked")
    @Test
    public void testShutdownDuringTimedInvokeAll() throws Exception {
        PolicyExecutor executor = provider.create("testShutdownDuringTimedInvokeAll")
                        .maxConcurrency(1)
                        .maxQueueSize(2)
                        .maxWaitForEnqueue(TimeUnit.SECONDS.toMillis(1));

        AtomicInteger completedAfterShutdown = new AtomicInteger();
        CountDownLatch unused = new CountDownLatch(0);
        List<Callable<Object>> tasks = new ArrayList<Callable<Object>>();
        tasks.add(Callable.class.cast(new SharedIncrementTask()));
        tasks.add(Callable.class.cast(new ShutdownTask(executor, false, unused, unused, 0)));
        tasks.add(Callable.class.cast(new SharedIncrementTask(completedAfterShutdown)));
        tasks.add(Callable.class.cast(new SharedIncrementTask(completedAfterShutdown)));
        tasks.add(Callable.class.cast(new SharedIncrementTask(completedAfterShutdown)));

        try {
            fail("shutdown should cause tasks submitted by invokeAll to be rejected. Instead: " +
                 executor.invokeAll(tasks, 5, TimeUnit.MINUTES));
        } catch (RejectedExecutionException x) {
        }

        assertEquals(1, SharedIncrementTask.class.cast(tasks.get(0)).count());

        assertTrue(executor.awaitTermination(TIMEOUT_NS, TimeUnit.NANOSECONDS));

        int count = completedAfterShutdown.get();
        assertTrue(Integer.toString(count), count < 3);
    }

    // Tests behavior when shutdownNow occurs while invokeAll is submitting and running tasks. Supply a group of 3 tasks to invokeAll.
    // The first task should complete successfully. The second task waits for the third to start and then shuts down the executor.
    // Relying on behavior of policy executor that runs invokeAll tasks in reverse order on the current thread if the global
    // executor hasn't been able to start them, the third (last) task should start on the current thread and block until canceled
    // by shutdownNow.
    @SuppressWarnings("unchecked")
    @Test
    public void testShutdownNowDuringInvokeAll() throws Exception {
        PolicyExecutor executor = provider.create("testShutdownNowDuringInvokeAll")
                        .maxConcurrency(2)
                        .maxPolicy(MaxPolicy.strict);

        CountDownLatch blocker = new CountDownLatch(1);
        CountDownLatch task3BeginLatch = new CountDownLatch(1);
        CountDownLatch unused = new CountDownLatch(0);
        List<Callable<Object>> tasks = new ArrayList<Callable<Object>>();
        tasks.add(Callable.class.cast(new SharedIncrementTask()));
        tasks.add(Callable.class.cast(new ShutdownTask(executor, true, unused, task3BeginLatch, TimeUnit.MINUTES.toNanos(5))));
        tasks.add(Callable.class.cast(new CountDownTask(task3BeginLatch, blocker, TimeUnit.MINUTES.toNanos(5))));

        List<Future<Object>> futures = executor.invokeAll(tasks);

        assertEquals(3, futures.size());

        Future<Object> future = futures.get(0);
        assertFalse(future.isCancelled());
        assertTrue(future.isDone());
        assertEquals(1, future.get(0, TimeUnit.SECONDS));
        assertEquals(1, SharedIncrementTask.class.cast(tasks.get(0)).count());

        future = futures.get(1);
        assertTrue(future.isCancelled());
        assertTrue(future.isDone());
        try {
            fail("ShutdownTask should have been canceled by shutdown. Instead: " + future.get(0, TimeUnit.SECONDS));
        } catch (CancellationException x) { // due to shutdown
        }

        future = futures.get(2);
        assertTrue(future.isCancelled());
        assertTrue(future.isDone());
        try {
            fail("CountDownTask should have been canceled by shutdown. Instead: " + future.get(0, TimeUnit.SECONDS));
        } catch (CancellationException x) { // due to shutdown
        }

        assertTrue(executor.awaitTermination(TIMEOUT_NS, TimeUnit.NANOSECONDS));
    }

    // Submit a task that gets queued but times out (due to startTimeout) before it can run.
    @Test
    public void testStartTimeout() throws Exception {
        PolicyExecutor executor = provider.create("testStartTimeout")
                        .maxConcurrency(1);
        // Use up maxConcurrency so that no other tasks can start
        CountDownLatch blocker = new CountDownLatch(1);
        CountDownLatch blockerStarted = new CountDownLatch(1);
        CountDownTask blockerTask = new CountDownTask(blockerStarted, blocker, TIMEOUT_NS * 2);
        PolicyTaskFuture<Boolean> blockerFuture = (PolicyTaskFuture<Boolean>) executor.submit(blockerTask);

        // startTimeout is applied after submitting the blocker so that it doesn't ever stop the blocker task from starting
        executor.startTimeout(300);

        AtomicInteger counter = new AtomicInteger();
        PolicyTaskFuture<Integer> future = (PolicyTaskFuture<Integer>) executor.submit(new SharedIncrementTask(counter), 1);

        // Wait just long enough to time out the queued task
        blockerStarted.await(TIMEOUT_NS, TimeUnit.NANOSECONDS);
        long delayMS = 300 - future.getElapsedAcceptTime(TimeUnit.MILLISECONDS) - future.getElapsedQueueTime(TimeUnit.MILLISECONDS) + 2;
        if (delayMS > 0)
            assertFalse(blocker.await(delayMS, TimeUnit.MILLISECONDS));

        // Let the blocking task finish so that the queued task can attempt to run
        blocker.countDown();

        // Task must be aborted due to start timeout
        try {
            Integer result = future.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
            fail("Task should be been aborted, instead result is: " + result);
        } catch (RejectedExecutionException x) {
            if (!(x.getCause() instanceof StartTimeoutException)
                || x.getCause().getMessage() == null
                || !x.getCause().getMessage().contains("CWWKE1205E"))
                throw x;
            // else pass - aborted due to timeout
        }

        // Task must have 0 run time, and its queue time must stop changing after abort
        long queueTimeNS = future.getElapsedQueueTime(TimeUnit.NANOSECONDS);
        assertTrue(future.isDone());
        assertFalse(future.isCancelled());
        assertEquals(0, future.getElapsedRunTime(TimeUnit.NANOSECONDS));
        boolean sameQueueTime = false;
        for (long start = System.nanoTime(); !sameQueueTime && System.nanoTime() - start < TIMEOUT_NS; Thread.sleep(200)) {
            long newQueueTimeNS = future.getElapsedQueueTime(TimeUnit.NANOSECONDS);
            sameQueueTime = queueTimeNS == newQueueTimeNS;
            queueTimeNS = newQueueTimeNS;
        }
        assertTrue(sameQueueTime);

        // Additional testing for the measured run time of the blocker task
        assertTrue(blockerFuture.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        long runTimeMS = blockerFuture.getElapsedRunTime(TimeUnit.MILLISECONDS);
        assertTrue(runTimeMS + "ms, " + delayMS + "ms", runTimeMS >= delayMS - 100); // tolerate lack of millisecond precision
        assertEquals(runTimeMS, blockerFuture.getElapsedRunTime(TimeUnit.MILLISECONDS));

        assertEquals(Collections.EMPTY_LIST, executor.shutdownNow());
    }

    // Submit tasks that get queued, but time out (due to startTimeout) while in the queue due to other
    // submits needing their queue positions.
    @Test
    public void testStartTimeoutFromQueue() throws Exception {
        PolicyExecutor executor = provider.create("testStartTimeoutFromQueue")
                        .maxConcurrency(1)
                        .maxQueueSize(3);
        // Use up maxConcurrency so that no other tasks can start
        CountDownLatch blocker = new CountDownLatch(1);
        CountDownLatch blockerStarted = new CountDownLatch(1);
        CountDownTask blockerTask = new CountDownTask(blockerStarted, blocker, TIMEOUT_NS * 2);
        PolicyTaskFuture<Boolean> blockerFuture = (PolicyTaskFuture<Boolean>) executor.submit(blockerTask);

        // Ensure that blocker task is no longer in the queue before we start to fill the queue positions
        assertTrue(blockerStarted.await(TIMEOUT_NS, TimeUnit.NANOSECONDS));

        AtomicInteger counter145 = new AtomicInteger();
        AtomicInteger counter23 = new AtomicInteger();

        // Use up the first queue position for a task that doesn't time out
        Future<Integer> future1 = executor.submit((Callable<Integer>) new SharedIncrementTask(counter145));

        // startTimeout is applied after submitting the blocker and first task,
        // so that it doesn't ever stop them from starting
        executor.startTimeout(200);

        // Submit 2 tasks that can time out
        Callable<Integer> task2 = new SharedIncrementTask(counter23);
        PolicyTaskFuture<Integer> future2 = (PolicyTaskFuture<Integer>) executor.submit(task2);
        Callable<Integer> task3 = new SharedIncrementTask(counter23);
        PolicyTaskFuture<Integer> future3 = (PolicyTaskFuture<Integer>) executor.submit(task3);

        // Let the tasks time out, but they remain in the queue
        TimeUnit.MILLISECONDS.sleep(400);

        // ensure that tasks submitted after this point do not time out
        executor.startTimeout(-1);

        // Submit 2 more tasks that should take over the queue positions of the previous 2 tasks
        Callable<Integer> task4 = new SharedIncrementTask(counter145);
        PolicyTaskFuture<Integer> future4 = (PolicyTaskFuture<Integer>) executor.submit(task4);
        Callable<Integer> task5 = new SharedIncrementTask(counter145);
        PolicyTaskFuture<Integer> future5 = (PolicyTaskFuture<Integer>) executor.submit(task5);

        // Tasks 2 and 3 must be aborted due to start timeout
        long acceptTime2 = future2.getElapsedAcceptTime(TimeUnit.NANOSECONDS);
        long acceptTime3 = future3.getElapsedAcceptTime(TimeUnit.NANOSECONDS);
        long queueTime2 = future2.getElapsedQueueTime(TimeUnit.NANOSECONDS);
        long queueTime3 = future3.getElapsedQueueTime(TimeUnit.NANOSECONDS);
        assertTrue(future2.isDone());
        assertTrue(future3.isDone());
        assertFalse(future2.isCancelled());
        assertFalse(future3.isCancelled());
        assertTrue(acceptTime2 >= 0);
        assertTrue(acceptTime3 >= 0);
        assertTrue(queueTime2 >= 0);
        assertTrue(queueTime3 >= 0);

        try {
            Integer result = future2.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
            fail("Task 2 should be been aborted, instead result is: " + result);
        } catch (RejectedExecutionException x) {
            if (!(x.getCause() instanceof StartTimeoutException)
                || x.getCause().getMessage() == null
                || !x.getCause().getMessage().contains("CWWKE1205E"))
                throw x;
            // else pass - aborted due to timeout
        }

        try {
            Integer result = future3.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
            fail("Task 3 should be been aborted, instead result is: " + result);
        } catch (RejectedExecutionException x) {
            if (!(x.getCause() instanceof StartTimeoutException)
                || x.getCause().getMessage() == null
                || !x.getCause().getMessage().contains("CWWKE1205E"))
                throw x;
            // else pass - aborted due to timeout
        }

        assertEquals(0, future2.getElapsedRunTime(TimeUnit.NANOSECONDS));
        assertEquals(0, future3.getElapsedRunTime(TimeUnit.NANOSECONDS));

        assertEquals(acceptTime2, future2.getElapsedAcceptTime(TimeUnit.NANOSECONDS));
        assertEquals(acceptTime3, future3.getElapsedAcceptTime(TimeUnit.NANOSECONDS));
        assertEquals(queueTime2, future2.getElapsedQueueTime(TimeUnit.NANOSECONDS));
        assertEquals(queueTime3, future3.getElapsedQueueTime(TimeUnit.NANOSECONDS));

        assertFalse(future1.isDone());
        assertFalse(future4.isDone());
        assertFalse(future5.isDone());

        // Let the blocking task finish so that the queued task can attempt to run
        blocker.countDown();

        // other tasks can run now, and should be successful
        assertEquals(Integer.valueOf(1), future1.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertEquals(Integer.valueOf(2), future4.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertEquals(Integer.valueOf(3), future5.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));

        assertEquals(0, counter23.get());
        assertEquals(3, counter145.get());

        assertEquals(Collections.EMPTY_LIST, executor.shutdownNow());
    }

    // Submit a task that gets queued but times out (due to startTimeout) while in the queue
    // due to another submit that is waiting for enqueue.
    @Test
    public void testStartTimeoutWhenAnotherTaskWaitsForEnqueue() throws Exception {
        PolicyExecutor executor = provider.create("testStartTimeoutWhenAnotherTaskWaitsForEnqueue")
                        .maxConcurrency(1)
                        .maxQueueSize(1)
                        .maxWaitForEnqueue(TimeUnit.NANOSECONDS.toMillis(TIMEOUT_NS));

        // Use up maxConcurrency so that no other tasks can start
        CountDownLatch blocker = new CountDownLatch(1);
        CountDownLatch unused = new CountDownLatch(0);
        CountDownTask blockerTask = new CountDownTask(unused, blocker, TIMEOUT_NS * 2);
        PolicyTaskFuture<Boolean> blockerFuture = (PolicyTaskFuture<Boolean>) executor.submit(blockerTask);

        Callable<Integer> task1 = new SharedIncrementTask();
        Callable<Integer> task2 = new SharedIncrementTask();

        // Put a task with start timeout of 400ms into the queue
        executor.startTimeout(400);
        PolicyTaskFuture<Integer> future1 = executor.submit(task1, null);

        // Disable start timeout and immediately submit another task.
        // It is only possible to submit because the previous task times out of the queue while we are waiting.
        executor.startTimeout(-1);
        PolicyTaskFuture<Integer> future2 = executor.submit(task2, null);
        long acceptTime2 = future2.getElapsedAcceptTime(TimeUnit.NANOSECONDS);
        assertTrue(acceptTime2 + "ns", acceptTime2 >= 0);

        assertTrue(future1.isDone());
        assertFalse(future1.isCancelled());
        long queueTime1 = future1.getElapsedQueueTime(TimeUnit.NANOSECONDS);
        assertTrue(queueTime1 + "ns", queueTime1 >= 0);
        assertEquals(0, future1.getElapsedRunTime(TimeUnit.NANOSECONDS));
        try {
            Integer result = future1.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
            fail("Task 1 should be been aborted, instead result is: " + result);
        } catch (RejectedExecutionException x) {
            if (!(x.getCause() instanceof StartTimeoutException)
                || x.getCause().getMessage() == null
                || !x.getCause().getMessage().contains("CWWKE1205E"))
                throw x;
            // else pass - aborted due to timeout
        }

        assertFalse(future2.isDone());

        // Allow tasks to run
        blocker.countDown();

        assertTrue(blockerFuture.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertEquals(Integer.valueOf(1), future2.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertEquals(acceptTime2, (long) future2.getElapsedAcceptTime(TimeUnit.NANOSECONDS));
        long queueTime2 = future2.getElapsedQueueTime(TimeUnit.NANOSECONDS);
        assertTrue(queueTime2 + "ns", queueTime2 >= 0);
        long runTime2 = future2.getElapsedRunTime(TimeUnit.NANOSECONDS);
        assertTrue(runTime2 + "ns", runTime2 >= 0);

        assertEquals(Collections.EMPTY_LIST, executor.shutdownNow());
    }

    //Ensure that a policy executor can be obtained from the injected provider
    @Test
    public void testGetPolicyExecutor() throws Exception {
        provider.create("testGetPolicyExecutor").maxConcurrency(2);
    }

    //Ensure that two tasks are run and the third is queued when three tasks are submitted and max concurrency is 2
    @Test
    public void testMaxConcurrencyBasic() throws Exception {
        PolicyExecutor executor = provider.create("testMaxConcurrencyBasic")
                        .maxConcurrency(2)
                        .maxQueueSize(1)
                        .maxWaitForEnqueue(TimeUnit.MINUTES.toMillis(1))
                        .runIfQueueFull(false);

        CountDownLatch beginLatch = new CountDownLatch(3);
        CountDownLatch continueLatch = new CountDownLatch(1);
        CountDownTask task = new CountDownTask(beginLatch, continueLatch, TimeUnit.HOURS.toNanos(1));

        //This task should start and block on continueLatch
        Future<Boolean> future1 = executor.submit(task);
        //This task should start and block on continueLatch
        Future<Boolean> future2 = executor.submit(task);
        //This task should be queued since we should be at max concurrency
        Future<Boolean> future3 = executor.submit(task);

        //Shorten maxWaitForEnqueue so we the test doesn't have to wait long for the timeout
        executor.maxWaitForEnqueue(200);

        try {
            //This task should be aborted since the queue should be full, triggering a RejectedExecutionException
            Future<Boolean> future4 = executor.submit(task);

            fail("The fourth task should have thrown a RejectedExecutionException when attempting to queue. Instead " + future4);

        } catch (RejectedExecutionException x) {
        } //expected

        //Let the three tasks complete
        continueLatch.countDown();

        assertTrue(future1.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertTrue(future2.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertTrue(future3.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));

        executor.shutdownNow();
    }

    //Test updating maxConcurrency:
    //The test begins with maxConcurrency of 1 and one task is submitted and runs
    //The maxConcurrency is increased to two and another task is submitted and should run
    //Submit two more tasks, one should queue and one should abort
    //Increase the maxConcurrency to 3 and queue size to 2
    //The queued task should run and then submit another task which will queue
    //Then decrease MaxConcurrency to 2 and queue size to 1
    //Allow the third submitted task to complete and submit another, which should abort since there are
    //two tasks running and one in the queue
    @Test
    public void testUpdateMaxConcurrency() throws Exception {
        PolicyExecutor executor = provider.create("testUpdateMaxConcurrency")
                        .maxConcurrency(1)
                        .maxQueueSize(1)
                        .maxWaitForEnqueue(TimeUnit.MINUTES.toMillis(1))
                        .runIfQueueFull(false);

        CountDownLatch beginLatch1 = new CountDownLatch(2);
        CountDownLatch continueLatch1 = new CountDownLatch(1);
        CountDownTask task1 = new CountDownTask(beginLatch1, continueLatch1, TimeUnit.HOURS.toNanos(1));
        CountDownLatch beginLatch2 = new CountDownLatch(1);
        CountDownLatch continueLatch2 = new CountDownLatch(1);
        CountDownTask task2 = new CountDownTask(beginLatch2, continueLatch2, TimeUnit.HOURS.toNanos(1));

        //This task should start and block on continueLatch
        Future<Boolean> future1 = executor.submit(task1);
        executor.maxConcurrency(2);
        //This task should start and block on continueLatch since maxConcurrency was just increased
        Future<Boolean> future2 = executor.submit(task1);

        //Ensure both tasks are running
        assertTrue(beginLatch1.await(TIMEOUT_NS, TimeUnit.NANOSECONDS));

        //This task should be queued since we should be at max concurrency
        Future<Boolean> future3 = executor.submit(task2);
        Future<Boolean> future4 = null;

        //Shorten maxWaitForEnqueue so we the test doesn't have to wait long for the timeout
        executor.maxWaitForEnqueue(200);

        try {
            //This task should be aborted since the queue should be full, triggering a RejectedExecutionException
            future4 = executor.submit(task1);

            fail("The fourth task should have thrown a RejectedExecutionException when attempting to queue");

        } catch (RejectedExecutionException x) {
        } //expected

        //Return maxWaitForEnqueue to a one minute timeout so it doesn't timeout on a slow machine
        executor.maxWaitForEnqueue(TimeUnit.MINUTES.toMillis(1));

        //Changing maxConcurrency to 3
        executor.maxConcurrency(3).maxQueueSize(2);

        //The queued task should run after the maxConcurrency is increased
        assertTrue(beginLatch2.await(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        Future<Boolean> future5 = executor.submit(task2);

        //Setting the maxConcurrency lower than the current number of tasks running should be allowed
        //Also set the queue size back to 1 so that the queue is full again
        executor.maxConcurrency(2).maxQueueSize(1);

        //Allow the third task to complete
        continueLatch2.countDown();

        assertTrue(future3.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));

        //Shorten maxWaitForEnqueue so the test doesn't have to wait long for the timeout
        executor.maxWaitForEnqueue(200);

        try {
            //This task should be aborted since the queue should be full and
            //there are two tasks running, triggering a RejectedExecutionException
            future4 = executor.submit(task1);

            fail("The task future4 should have thrown a RejectedExecutionException when attempting to queue");

        } catch (RejectedExecutionException x) {
        } //expected

        //Let the three tasks complete
        continueLatch1.countDown();

        assertTrue(future1.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertTrue(future2.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertTrue(future5.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));

        executor.shutdownNow();
    }

    //Test that changing the maxConcurrency of one executor does not affect a different executor
    @Test
    public void testMaxConcurrencyMultipleExecutors() throws Exception {
        PolicyExecutor executor1 = provider.create("testMaxConcurrencyMultipleExecutors-1")
                        .maxConcurrency(1)
                        .maxQueueSize(1)
                        .maxWaitForEnqueue(TimeUnit.MINUTES.toMillis(1))
                        .runIfQueueFull(false);
        PolicyExecutor executor2 = provider.create("testMaxConcurrencyMultipleExecutors-2")
                        .maxConcurrency(1)
                        .maxQueueSize(1)
                        .maxWaitForEnqueue(TimeUnit.MINUTES.toMillis(1))
                        .runIfQueueFull(false);

        CountDownLatch beginLatch = new CountDownLatch(3);
        CountDownLatch continueLatch = new CountDownLatch(1);
        CountDownTask task = new CountDownTask(beginLatch, continueLatch, TimeUnit.HOURS.toNanos(1));

        //Should run and block on continue latch
        Future<Boolean> future1 = executor1.submit(task);

        //Should run and block on continue latch
        Future<Boolean> future2 = executor2.submit(task);

        executor1.maxConcurrency(2);

        //This task should be queued since we should be at max concurrency in executor 1
        Future<Boolean> future3 = executor2.submit(task);
        Future<Boolean> future4 = null;

        //Shorten maxWaitForEnqueue so the test doesn't have to wait long for the timeout
        executor2.maxWaitForEnqueue(200);

        try {
            //This task should be aborted since the queue should be full, triggering a RejectedExecutionException
            future4 = executor2.submit(task);

            fail("The third task on executor2 should have thrown a RejectedExecutionException when attempting to queue");

        } catch (RejectedExecutionException x) {
        } //expected

        //Let the three tasks complete
        continueLatch.countDown();

        assertTrue(future1.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertTrue(future2.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertTrue(future3.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));

        executor1.shutdownNow();

        executor2.shutdownNow();
    }

    //Concurrently submit two tasks to change the maxConcurrency.  Ensure that afterward the maxConcurrency
    //is one of the two submitted values
    @Test
    public void testConcurrentUpdateMaxConcurrency() throws Exception {
        PolicyExecutor executor = provider.create("testConcurrentUpdateMaxConcurrency")
                        .maxConcurrency(2)
                        .maxWaitForEnqueue(TimeUnit.MINUTES.toMillis(1))
                        .runIfQueueFull(false)
                        .maxQueueSize(1);

        int numSubmitted = 8;

        CountDownLatch beginLatch = new CountDownLatch(numSubmitted);
        CountDownLatch continueLatch1 = new CountDownLatch(1);

        CountDownLatch continueLatch2 = new CountDownLatch(1);

        ConfigChangeTask configTask1 = new ConfigChangeTask(executor, beginLatch, continueLatch1, TIMEOUT_NS, "maxConcurrency", "1");
        ConfigChangeTask configTask2 = new ConfigChangeTask(executor, beginLatch, continueLatch1, TIMEOUT_NS, "maxConcurrency", "3");
        CountDownTask countDownTask = new CountDownTask(new CountDownLatch(0), continueLatch2, TIMEOUT_NS);

        List<Future<Boolean>> futures = new ArrayList<Future<Boolean>>();

        //Submit numSubmitted tasks, half with a value of 1 and half with a value of 3
        for (int i = 0; i < numSubmitted; i++) {
            //alternate submitting task with a value of 1 and 3
            if ((i % 2) == 0)
                futures.add(testThreads.submit(configTask1));
            else
                futures.add(testThreads.submit(configTask2));
        }

        //Wait for the tasks to begin running
        assertTrue(beginLatch.await(TIMEOUT_NS, TimeUnit.NANOSECONDS));

        //Allow the tasks to change the maxConcurrency and complete
        continueLatch1.countDown();

        for (int i = 0; i < numSubmitted; i++) {
            assertTrue(futures.get(i).get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        }

        //Now we need to check that the maxConcurrency is either 1 or 3

        //This should be the first task run
        Future<Boolean> future3 = executor.submit(countDownTask);

        //This task should queue if maxConcurrency=1, otherwise should run
        Future<Boolean> future4 = executor.submit(countDownTask);
        Future<Boolean> future5 = null;
        boolean caughtException = false;
        //Decrease to 1s so that we aren't waiting too long if maxConcurrency is 1
        //However this can't be too short that it is hit as tasks queue and run in the case that
        //maxConcurrency is 3 on a slow machine
        executor.maxWaitForEnqueue(1000);

        try {
            //This task will be aborted if maxConcurrency = 1, otherwise should run
            future5 = executor.submit(countDownTask);
        } catch (RejectedExecutionException x) {
            caughtException = true; //expected if maxConcurrency = 1
        }

        executor.maxWaitForEnqueue(TimeUnit.MINUTES.toMillis(1));
        Future<Boolean> future6 = null;
        Future<Boolean> future7 = null;
        if (caughtException == false) {
            //We should be at maxConcurrency of 3 here, so this should queue
            future6 = executor.submit(countDownTask);
            //Decrease to 200 ms so that we aren't waiting too long for timeout
            executor.maxWaitForEnqueue(200);
            try {
                //This task will be aborted if maxConcurrency = 3
                future7 = executor.submit(countDownTask);
            } catch (RejectedExecutionException x) {
                caughtException = true; //expected if maxConcurrency = 3
            }
        }

        assertTrue("maxConcurrency should be either 1 or 3", caughtException);

        //Let the submitted tasks complete
        continueLatch2.countDown();

        assertTrue(future3.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertTrue(future4.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        if (future5 != null)
            assertTrue(future5.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        if (future6 != null)
            assertTrue(future6.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));

        executor.shutdownNow();
    }

    //Test that maxConcurrency cannot be called after shutdown
    @Test
    public void testUpdateMaxConcurrencyAfterShutdown() {
        PolicyExecutor executor = provider.create("updateMaxConcurrencyAfterShutdown")
                        .maxConcurrency(2);

        executor.shutdown();
        try {
            executor.maxConcurrency(5);
            fail("Should not be allowed to change maxConcurrency after calling shutdown");
        } catch (IllegalStateException e) { //expected
        }
    }

    //Test that when maxConcurrency is increased with tasks queued that the
    //proper number of tasks are run and, more importantly, maxConcurrency is not violated
    @Test
    public void testPollingWhenMaxConcurrencyIncreased() throws Exception {
        PolicyExecutor executor = provider.create("testPollingWhenMaxConcurrencyIncreased")
                        .maxConcurrency(2)
                        .maxQueueSize(-1)
                        .maxWaitForEnqueue(TimeUnit.MINUTES.toMillis(1))
                        .runIfQueueFull(false);

        CountDownLatch beginLatch1 = new CountDownLatch(2);
        CountDownLatch continueLatch1 = new CountDownLatch(1);
        CountDownTask task1 = new CountDownTask(beginLatch1, continueLatch1, TimeUnit.HOURS.toNanos(1));
        CountDownLatch beginLatch2 = new CountDownLatch(2);
        CountDownLatch continueLatch2 = new CountDownLatch(1);
        CountDownTask task2 = new CountDownTask(beginLatch2, continueLatch2, TimeUnit.HOURS.toNanos(1));
        CountDownLatch beginLatch3 = new CountDownLatch(1);
        CountDownTask task3 = new CountDownTask(beginLatch3, continueLatch2, TimeUnit.HOURS.toNanos(1));

        //These tasks should start and block on continueLatch
        Future<Boolean> future1 = executor.submit(task1);
        Future<Boolean> future2 = executor.submit(task1);

        //Ensure both tasks are running
        assertTrue(beginLatch1.await(TIMEOUT_NS, TimeUnit.NANOSECONDS));

        //These task should be queued since we should be at max concurrency
        Future<Boolean> future3 = executor.submit(task2);
        Future<Boolean> future4 = executor.submit(task2);

        //At this point there should be two running tasks blocked on the continueLatch1 and
        //two additional tasks queued

        //Changing maxConcurrency to max int
        executor.maxConcurrency(-1);

        //The queued tasks should run after the maxConcurrency is increased
        assertTrue(beginLatch2.await(TIMEOUT_NS, TimeUnit.NANOSECONDS));

        //Allow the first group of tasks to complete
        continueLatch1.countDown();

        assertTrue(future1.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertTrue(future2.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));

        //Set the maxConcurrency back to 2
        executor.maxConcurrency(2);

        //Submit two more tasks, which will queue

        Future<Boolean> future5 = executor.submit(task3);
        Future<Boolean> future6 = executor.submit(task3);

        //change the maxConcurrency to 3- ensure that both tasks don't run since that would
        //be more than maxConcurrency
        executor.maxConcurrency(3);
        //Wait for 1 of the tasks to start
        assertTrue(beginLatch3.await(TIMEOUT_NS, TimeUnit.NANOSECONDS));

        //Now we should have three tasks running and one queued, change maxQueueSize to 1 and test that the next
        //task submitted is rejected

        executor.maxQueueSize(1).maxWaitForEnqueue(500);

        try {
            //This task should be aborted since the queue should be full, triggering a RejectedExecutionException
            executor.submit(task3);

            fail("The task should have thrown a RejectedExecutionException when attempting to queue since the queue should be full");

        } catch (RejectedExecutionException x) {
        } //expected

        //Let the rest of the tasks run
        continueLatch2.countDown();

        assertTrue(future3.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertTrue(future4.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertTrue(future5.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertTrue(future6.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));

        executor.shutdownNow();
    }

    @Test
    public void testConcurrentUpdateMaxConcurrencyAndSubmit() throws Exception {
        PolicyExecutor executor = provider.create("testConcurrentUpdateMaxConcurrencyAndSubmit")
                        .maxConcurrency(4)
                        .maxWaitForEnqueue(TimeUnit.MINUTES.toMillis(1))
                        .maxQueueSize(2);

        int numSubmitted = 6;

        //Latch for configChangeTask and submitterTask
        CountDownLatch beginLatch1 = new CountDownLatch(numSubmitted * 2);
        CountDownLatch continueLatch1 = new CountDownLatch(1);
        ConfigChangeTask configTask = new ConfigChangeTask(executor, beginLatch1, continueLatch1, TIMEOUT_NS, "maxConcurrency", "6");

        CountDownLatch continueLatch2 = new CountDownLatch(1);
        CountDownLatch beginLatch2 = new CountDownLatch(numSubmitted);
        CountDownTask countDownTask = new CountDownTask(beginLatch2, continueLatch2, TIMEOUT_NS);

        SubmitterTask<Boolean> submitTask = new SubmitterTask<Boolean>(executor, countDownTask, beginLatch1, continueLatch1, TIMEOUT_NS);

        List<Future<Boolean>> configFutures = new ArrayList<Future<Boolean>>();

        //Submit numSubmitted tasks to change maxConcurrency to 6, which will block on continueLatch2
        for (int i = 0; i < numSubmitted; i++) {
            configFutures.add(testThreads.submit(configTask));
        }

        List<Future<Future<Boolean>>> submitterFutures = new ArrayList<Future<Future<Boolean>>>();

        //Submit numSubmitted submit tasks, which will block on continueLatch2
        for (int i = 0; i < numSubmitted; i++) {
            submitterFutures.add(testThreads.submit(submitTask));
        }

        //Ensure all the submit and config tasks are running
        assertTrue(beginLatch1.await(TIMEOUT_NS, TimeUnit.NANOSECONDS));

        //Allow the configtasks and submitertasks to complete, which will submit numSubmitted countDownTasks
        //and concurrently change the maxConcurrency
        continueLatch1.countDown();

        //Ensure the configTasks have completed
        for (int i = 0; i < numSubmitted; i++) {
            assertTrue(configFutures.get(i).get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        }

        //The maxConcurrency should now be 6, so all the submitted tasks should be running
        assertTrue(beginLatch2.await(TIMEOUT_NS, TimeUnit.NANOSECONDS));

        //Now check that maxConcurrency is actually 6
        Future<Boolean> future1 = executor.submit(countDownTask);
        Future<Boolean> future2 = executor.submit(countDownTask);

        //The queue should now be full - try to add one more, it should be rejected
        executor.maxWaitForEnqueue(200);
        try {
            executor.submit(countDownTask);
            fail("MaxConcurrency should be 6");
        } catch (RejectedExecutionException x) {
        } //expected

        //Allow all the tasks to complete

        continueLatch2.countDown();

        //Ensure the submitted countdown tasks have completed
        for (int i = 0; i < numSubmitted; i++) {
            assertTrue(submitterFutures.get(i).get(TIMEOUT_NS, TimeUnit.NANOSECONDS).get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        }

        assertTrue(future1.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertTrue(future2.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));

        executor.shutdownNow();
    }

    //Test the basic functionality of MaxQueueSize
    //Submits 4 task to reach MaxQueue, submit another task and confirm it is rejected.
    //Increase Max queue size to 4, confirm 2 more task can be queued, but a third is rejected.
    //Reduce MaxQueue by 1 and confirm a newly submitted task is rejected.
    //Finish a task, so we're at MaxQueue. Confirm a submitted task is rejected.
    //Finish another task, so we're at 2/3 Queue. Submit 2 tasks, the second is rejected.
    //Finish all tasks, and confirm the correct number of tasks completed.
    @Test
    public void testMaxQueueSize() throws Exception {
        ExecutorService executor = provider.create("testMaxQueueSize")
                        .maxConcurrency(2)
                        .maxQueueSize(2)
                        .maxWaitForEnqueue(TimeUnit.MINUTES.toMillis(1))
                        .runIfQueueFull(false);

        CountDownLatch beginLatch = new CountDownLatch(2);
        CountDownLatch continueLatch = new CountDownLatch(1);
        CountDownLatch continueLatch2 = new CountDownLatch(1);
        CountDownLatch continueLatch3 = new CountDownLatch(1);

        CountDownTask task = new CountDownTask(beginLatch, continueLatch, TimeUnit.HOURS.toNanos(1));
        CountDownTask task2 = new CountDownTask(beginLatch, continueLatch2, TimeUnit.HOURS.toNanos(1));
        CountDownTask task3 = new CountDownTask(beginLatch, continueLatch3, TimeUnit.HOURS.toNanos(1));

        Future<Boolean> future1 = executor.submit(task); // should start, decrement the beginLatch, and block on continueLatch
        Future<Boolean> future2 = executor.submit(task2); // should start, decrement the beginLatch, and block on continueLatch

        assertTrue(beginLatch.await(TIMEOUT_NS, TimeUnit.NANOSECONDS));

        Future<Boolean> future3 = executor.submit(task3); // should queue
        Future<Boolean> future4 = executor.submit(task3); // should queue

        ((PolicyExecutor) executor).maxWaitForEnqueue(200);
        try {
            fail("Task should be aborted:" + executor.submit(task3)); // should abort
        } catch (RejectedExecutionException x) {
        } // pass

        ((PolicyExecutor) executor).maxWaitForEnqueue(TimeUnit.MINUTES.toMillis(1));
        ((PolicyExecutor) executor).maxQueueSize(4);

        Future<Boolean> future5 = executor.submit(task3); // should queue
        Future<Boolean> future6 = executor.submit(task3); // should queue

        ((PolicyExecutor) executor).maxWaitForEnqueue(200);
        try {
            fail("Task should be aborted:" + executor.submit(task3)); // should abort
        } catch (RejectedExecutionException x) {
        } // pass

        ((PolicyExecutor) executor).maxQueueSize(3);

        try {
            fail("Task should be aborted:" + executor.submit(task3)); // should abort
        } catch (RejectedExecutionException x) {
        } // pass

        continueLatch.countDown(); //finish one task, so we're back at a full queue size
        assertTrue(future1.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));

        try {
            fail("Task should be aborted:" + executor.submit(task3)); // should abort
        } catch (RejectedExecutionException x) {
        } // pass

        continueLatch2.countDown(); //finish one task so we're at 2/3 max queue
        assertTrue(future2.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));

        ((PolicyExecutor) executor).maxWaitForEnqueue(TimeUnit.MINUTES.toMillis(1));

        Future<Boolean> future7 = executor.submit(task3); // should queue

        ((PolicyExecutor) executor).maxWaitForEnqueue(200);
        try {
            fail("Task should be aborted:" + executor.submit(task3)); // should abort
        } catch (RejectedExecutionException x) {
        } // pass

        continueLatch3.countDown();
        assertTrue(future3.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertTrue(future4.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertTrue(future5.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertTrue(future6.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertTrue(future7.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));

        executor.shutdownNow();
    }

    //Test that changing the maxQueueSize of one executor does not affect a different executor
    @Test
    public void testMaxQueueSizeMultipleExecutors() throws Exception {
        PolicyExecutor executor1 = provider.create("testQueueSizeMultipleExecutors-1")
                        .maxConcurrency(1)
                        .maxQueueSize(1)
                        .maxWaitForEnqueue(TimeUnit.MINUTES.toMillis(1))
                        .runIfQueueFull(false);
        PolicyExecutor executor2 = provider.create("testQueueSizeMultipleExecutors-2")
                        .maxConcurrency(1)
                        .maxQueueSize(1)
                        .maxWaitForEnqueue(TimeUnit.MINUTES.toMillis(1))
                        .runIfQueueFull(false);

        CountDownLatch beginLatch = new CountDownLatch(100);
        CountDownLatch continueLatch = new CountDownLatch(1);
        CountDownTask task = new CountDownTask(beginLatch, continueLatch, TimeUnit.HOURS.toNanos(1));

        //Should run and block on continue latch
        Future<Boolean> future1 = executor1.submit(task);
        Future<Boolean> future2 = executor1.submit(task);
        Future<Boolean> future3 = executor2.submit(task);
        Future<Boolean> future4 = executor2.submit(task);

        executor1.maxQueueSize(2);

        //This task should queue

        Future<Boolean> future5 = executor1.submit(task);

        //This task should be aborted since we're at maxQueue in executor2.
        executor2.maxWaitForEnqueue(200);
        try {
            fail("Task should be aborted:" + executor2.submit(task)); // should abort
        } catch (RejectedExecutionException x) {
        } // pass

        //Let Tasks Complete
        continueLatch.countDown();

        assertTrue(future1.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertTrue(future2.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertTrue(future3.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertTrue(future4.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertTrue(future5.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));

        executor1.shutdownNow();

        executor2.shutdownNow();
    }

    //Concurrently submit two tasks to change the maxQueueSize.  Ensure that afterward the maxQueueSize
    //is one of the two submitted values
    @Test
    public void testMaxQueueSizeConcurrentUpdate() throws Exception {
        PolicyExecutor executor = provider.create("testConcurrentUpdateMaxQueueSize")
                        .maxConcurrency(2)
                        .maxWaitForEnqueue(TimeUnit.MINUTES.toMillis(1))
                        .maxQueueSize(1);

        int numSubmitted = 8;

        CountDownLatch beginLatch = new CountDownLatch(numSubmitted);
        CountDownLatch continueLatch1 = new CountDownLatch(1);

        CountDownLatch continueLatch2 = new CountDownLatch(1);

        ConfigChangeTask configTask1 = new ConfigChangeTask(executor, beginLatch, continueLatch1, TIMEOUT_NS, "maxQueueSize", "1");
        ConfigChangeTask configTask2 = new ConfigChangeTask(executor, beginLatch, continueLatch1, TIMEOUT_NS, "maxQueueSize", "3");
        CountDownTask countDownTask = new CountDownTask(new CountDownLatch(0), continueLatch2, TIMEOUT_NS);

        List<Future<Boolean>> futures = new ArrayList<Future<Boolean>>();

        //Submit numSubmitted tasks, half with a value of 1 and half with a value of 3
        for (int i = 0; i < numSubmitted; i++) {
            //alternate submitting task with a value of 1 and 3
            if ((i % 2) == 0)
                futures.add(testThreads.submit(configTask1));
            else
                futures.add(testThreads.submit(configTask2));
        }

        //Wait for the tasks to begin running
        assertTrue(beginLatch.await(TIMEOUT_NS, TimeUnit.NANOSECONDS));

        //Allow the tasks to change the maxQueueSize and complete
        continueLatch1.countDown();

        for (int i = 0; i < numSubmitted; i++) {
            assertTrue(futures.get(i).get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        }

        //Now we need to check that the maxQueueSize is either 1 or 3

        //Fill Up the concurrent tasks
        Future<Boolean> future3 = executor.submit(countDownTask);
        Future<Boolean> future4 = executor.submit(countDownTask);
        Future<Boolean> future5 = executor.submit(countDownTask); //First task should queue

        Future<Boolean> future6 = null;
        boolean caughtException = false;
        //Decrease to 1s so that we aren't waiting too long if maxQueueSize is 1
        //However this can't be too short that it is hit as tasks queue and run in the case that
        //maxQueueSize is 3 on a slow machine
        executor.maxWaitForEnqueue(1000);

        try {
            //This task will be aborted if maxQueueSize = 1, otherwise should run
            future6 = executor.submit(countDownTask);
        } catch (RejectedExecutionException x) {
            caughtException = true; //expected if maxQueueSize = 1
        }

        executor.maxWaitForEnqueue(TimeUnit.MINUTES.toMillis(1));
        Future<Boolean> future7 = null;
        Future<Boolean> future8 = null;
        if (caughtException == false) {
            //We should be at maxQueueSize of 3 here, so this should queue
            future7 = executor.submit(countDownTask);
            //Decrease to 200 ms so that we aren't waiting too long for timeout
            executor.maxWaitForEnqueue(200);
            try {
                //This task will be aborted if maxQueueSize = 3
                future8 = executor.submit(countDownTask);
            } catch (RejectedExecutionException x) {
                caughtException = true; //expected if maxQueueSize = 3
            }
        }

        assertTrue("maxQueueSize should be either 1 or 3", caughtException);

        //Let the submitted tasks complete
        continueLatch2.countDown();

        assertTrue(future3.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertTrue(future4.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertTrue(future5.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        if (future6 != null)
            assertTrue(future6.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        if (future7 != null)
            assertTrue(future7.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));

        executor.shutdownNow();
    }

    //Test that maxQueueSize cannot be called after shutdown
    @Test
    public void testMaxQueueSizeUpdateAfterShutdown() {
        PolicyExecutor executor = provider.create("updateMaxQueueSizeAfterShutdown")
                        .maxConcurrency(2)
                        .maxQueueSize(2);

        executor.shutdown();
        try {
            executor.maxQueueSize(5);
            fail("Should not be allowed to change maxQueueSize after calling shutdown");
        } catch (IllegalStateException e) { //expected
        }
    }

    //Test that when a task is waiting for Enqueue is run when Queue
    @Test
    public void testMaxQueueSizeWaitForEnqueue() throws Exception {
        PolicyExecutor executor = provider.create("testMaxQueueSizeEnqueue-1")
                        .maxConcurrency(1)
                        .maxQueueSize(1)
                        .maxWaitForEnqueue(TimeUnit.HOURS.toMillis(1))
                        .runIfQueueFull(false);

        CountDownLatch beginLatch1 = new CountDownLatch(50);
        CountDownLatch continueLatch1 = new CountDownLatch(1);
        CountDownLatch continueLatch2 = new CountDownLatch(1);

        CountDownTask task1 = new CountDownTask(beginLatch1, continueLatch1, TimeUnit.HOURS.toNanos(1));
        CountDownTask task2 = new CountDownTask(beginLatch1, continueLatch2, TimeUnit.HOURS.toNanos(1));
        SubmitterTask<Boolean> task3 = new SubmitterTask<Boolean>(executor, task2);

        //Fill up the Queue
        Future<Boolean> future1 = executor.submit(task1);
        Future<Boolean> future2 = executor.submit(task1);

        Future<Future<Boolean>> future3 = testThreads.submit(task3); //Should wait for enqueue

        executor.maxQueueSize(2);

        Future<Future<Boolean>> future4 = testThreads.submit(task3); //Should wait for enqueue

        continueLatch1.countDown(); //finish 2 tasks, queue now at 1/2
        assertTrue(future1.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertTrue(future2.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));

        Future<Boolean> future5 = executor.submit(task2); //queue full

        Future<Future<Boolean>> future6 = testThreads.submit(task3); //Should wait for enqueue

        executor.maxQueueSize(1); //reduce queue

        continueLatch2.countDown();

        assertTrue(future3.get(TIMEOUT_NS, TimeUnit.NANOSECONDS).get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertTrue(future4.get(TIMEOUT_NS, TimeUnit.NANOSECONDS).get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertTrue(future5.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertTrue(future6.get(TIMEOUT_NS, TimeUnit.NANOSECONDS).get(TIMEOUT_NS, TimeUnit.NANOSECONDS));

        executor.shutdownNow();
    }

    //Test changing maxQueueSize and maxConcurrency concurrently
    @Test
    public void testMaxQueueSizeAndMaxConcurrencyConcurrentUpdate() throws Exception {
        PolicyExecutor executor = provider.create("testMaxQueueSizeAndMaxConcurrencyConcurrentUpdate")
                        .maxConcurrency(2)
                        .maxWaitForEnqueue(TimeUnit.MINUTES.toMillis(1))
                        .maxQueueSize(2);

        int numSubmitted = 16;

        CountDownLatch beginLatch = new CountDownLatch(numSubmitted);
        CountDownLatch continueLatch1 = new CountDownLatch(1);

        CountDownLatch continueLatch2 = new CountDownLatch(1);

        ConfigChangeTask configTask1 = new ConfigChangeTask(executor, beginLatch, continueLatch1, TIMEOUT_NS, "maxQueueSize", "1");
        ConfigChangeTask configTask2 = new ConfigChangeTask(executor, beginLatch, continueLatch1, TIMEOUT_NS, "maxQueueSize", "3");
        ConfigChangeTask configTask3 = new ConfigChangeTask(executor, beginLatch, continueLatch1, TIMEOUT_NS, "maxConcurrency", "1");
        ConfigChangeTask configTask4 = new ConfigChangeTask(executor, beginLatch, continueLatch1, TIMEOUT_NS, "maxConcurrency", "3");
        CountDownTask countDownTask = new CountDownTask(new CountDownLatch(0), continueLatch2, TIMEOUT_NS);

        List<Future<Boolean>> futures = new ArrayList<Future<Boolean>>();

        //Submit numSubmitted tasks, quartered between maxQueueSize=1, maxQueueSize=3, maxConcurrency=1, and maxConcurrency=3;
        for (int i = 0; i < numSubmitted; i++) {
            switch (i % 4) {
                case 0:
                    futures.add(testThreads.submit(configTask1));
                    break;
                case 1:
                    futures.add(testThreads.submit(configTask2));
                    break;
                case 2:
                    futures.add(testThreads.submit(configTask3));
                    break;
                case 3:
                    futures.add(testThreads.submit(configTask4));
                    break;
            }
        }

        //Wait for the tasks to begin running
        assertTrue(beginLatch.await(TIMEOUT_NS, TimeUnit.NANOSECONDS));

        //Allow the tasks to change the maxQueueSize/maxConcurrency and complete
        continueLatch1.countDown();

        for (int i = 0; i < numSubmitted; i++) {
            assertTrue(futures.get(i).get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        }

        //Now we need to check that we can submit either 2 (1+1), 4(1+3,3+1), or 6(3+3) tasks

        //Fill Up the concurrent tasks
        Future<Boolean> future1 = executor.submit(countDownTask);
        Future<Boolean> future2 = executor.submit(countDownTask);

        //Possible additional futures
        Future<Boolean> future3 = null;
        Future<Boolean> future4 = null;
        Future<Boolean> future5 = null;
        Future<Boolean> future6 = null;

        boolean caughtException = false;

        executor.maxWaitForEnqueue(1000);

        try {
            //This task will be aborted if maxQueue = 1 and maxConcurrency = 1, otherwise should run
            future3 = executor.submit(countDownTask);
        } catch (RejectedExecutionException x) {
            caughtException = true;
        }

        executor.maxWaitForEnqueue(TimeUnit.MINUTES.toMillis(1));

        if (caughtException == false) {
            future4 = executor.submit(countDownTask);
            //Decrease to 200 ms so that we aren't waiting too long for timeout
            executor.maxWaitForEnqueue(200);
            try {
                //This task will be aborted if maxQueueSize + maxConcurrency = 4
                future5 = executor.submit(countDownTask);
            } catch (RejectedExecutionException x) {
                caughtException = true;
            }
        }

        executor.maxWaitForEnqueue(TimeUnit.MINUTES.toMillis(1));

        if (caughtException == false) {
            future6 = executor.submit(countDownTask);
            //Decrease to 200 ms so that we aren't waiting too long for timeout
            executor.maxWaitForEnqueue(200);
            try {
                //This task will be aborted if maxQueueSize + maxConcurrency = 6
                executor.submit(countDownTask);
                fail("Should not be able to submit 7 tasks");
            } catch (RejectedExecutionException x) {
                caughtException = true;
            }
        }

        assertTrue("maxQueueSize + maxConcurrency should be 2, 4, or 6", caughtException);

        //Let the submitted tasks complete
        continueLatch2.countDown();

        assertTrue(future1.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertTrue(future2.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        if (future3 != null)
            assertTrue(future3.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        if (future4 != null)
            assertTrue(future4.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        if (future5 != null)
            assertTrue(future5.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        if (future6 != null)
            assertTrue(future6.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        executor.shutdownNow();
    }

    @Test
    public void testConcurrentUpdateMaxQueueSizeAndSubmit() throws Exception {
        PolicyExecutor executor = provider.create("testConcurrentUpdateMaxQueueSizeAndSubmit")
                        .maxConcurrency(6)
                        .maxWaitForEnqueue(TimeUnit.MINUTES.toMillis(1))
                        .maxQueueSize(2);

        int numSubmitted = 6;

        //Latch for configChangeTask and submitterTask
        CountDownLatch beginLatch1 = new CountDownLatch(numSubmitted * 2);
        CountDownLatch continueLatch1 = new CountDownLatch(1);
        ConfigChangeTask configTask = new ConfigChangeTask(executor, beginLatch1, continueLatch1, TIMEOUT_NS, "maxQueueSize", "4");

        CountDownLatch continueLatch2 = new CountDownLatch(1);
        CountDownLatch beginLatch2 = new CountDownLatch(numSubmitted);
        CountDownTask countDownTask = new CountDownTask(beginLatch2, continueLatch2, TIMEOUT_NS);

        SubmitterTask<Boolean> submitTask = new SubmitterTask<Boolean>(executor, countDownTask, beginLatch1, continueLatch1, TIMEOUT_NS);

        List<Future<Boolean>> configFutures = new ArrayList<Future<Boolean>>();

        //Submit numSubmitted tasks to change maxQueueSize to 6, which will block on continueLatch2
        for (int i = 0; i < numSubmitted; i++) {
            configFutures.add(testThreads.submit(configTask));
        }

        List<Future<Future<Boolean>>> submitterFutures = new ArrayList<Future<Future<Boolean>>>();

        //Submit numSubmitted submit tasks, which will block on continueLatch2
        for (int i = 0; i < numSubmitted; i++) {
            submitterFutures.add(testThreads.submit(submitTask));
        }

        //Ensure all the submit and config tasks are running
        assertTrue(beginLatch1.await(TIMEOUT_NS, TimeUnit.NANOSECONDS));

        //Allow the configtasks and submitertasks to complete, which will submit numSubmitted countDownTasks
        //and concurrently change the maxQueueSize

        continueLatch1.countDown();

        //Ensure the configTasks have completed
        for (int i = 0; i < numSubmitted; i++) {
            assertTrue(configFutures.get(i).get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        }

        //The maxQueueSize should now be 4, and all the submitted tasks should be running
        assertTrue(beginLatch2.await(TIMEOUT_NS, TimeUnit.NANOSECONDS));

        //Now check that maxQueueSize is actually 4
        Future<Boolean> future1 = executor.submit(countDownTask);
        Future<Boolean> future2 = executor.submit(countDownTask);
        Future<Boolean> future3 = executor.submit(countDownTask);
        Future<Boolean> future4 = executor.submit(countDownTask);

        //The queue should now be full - try to add one more, it should be rejected
        executor.maxWaitForEnqueue(200);
        try {
            executor.submit(countDownTask);
            fail("MaxQueueSize should be 4");
        } catch (RejectedExecutionException x) {
        } //expected

        //Allow all the tasks to complete

        continueLatch2.countDown();

        //Ensure the submitted countdown tasks have completed
        for (int i = 0; i < numSubmitted; i++) {
            assertTrue(submitterFutures.get(i).get(TIMEOUT_NS, TimeUnit.NANOSECONDS).get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        }

        assertTrue(future1.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertTrue(future2.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertTrue(future3.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertTrue(future4.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));

        executor.shutdownNow();
    }

    //Test that a runnable executed on the executor is run
    @Test
    public void testExecute() throws Exception {
        ExecutorService executor = provider.create("testExecute");
        AtomicInteger counter = new AtomicInteger();
        executor.execute(new SharedIncrementTask(counter));
        Long startTime = System.nanoTime();
        while (System.nanoTime() < startTime + TIMEOUT_NS && counter.get() < 1);

        assertEquals("The executed task should have run", 1, counter.get());

        executor.shutdownNow();
    }

    //Test the getRunningTaskCount method
    @Test
    public void testGetRunningTaskCount() throws Exception {
        PolicyExecutor executor = provider.create("testgetRunningTaskCount").maxConcurrency(2);

        CountDownLatch beginLatch1 = new CountDownLatch(1);
        CountDownLatch continueLatch1 = new CountDownLatch(1);
        CountDownLatch beginLatch2 = new CountDownLatch(1);
        CountDownLatch continueLatch2 = new CountDownLatch(1);

        assertEquals(0, executor.getRunningTaskCount()); //no tasks running

        CountDownTask task1 = new CountDownTask(beginLatch1, continueLatch1, TimeUnit.HOURS.toNanos(1));
        Future<Boolean> future1 = executor.submit(task1); //task1 will begin running and block on continueLatch1
        assertTrue(beginLatch1.await(TIMEOUT_NS, TimeUnit.NANOSECONDS));

        assertEquals(1, executor.getRunningTaskCount()); //task1 is running

        CountDownTask task2 = new CountDownTask(beginLatch2, continueLatch2, TimeUnit.HOURS.toNanos(1));
        Future<Boolean> future2 = executor.submit(task2); //task2 will begin running and block on continueLatch2
        assertTrue(beginLatch2.await(TIMEOUT_NS, TimeUnit.NANOSECONDS));

        assertEquals(2, executor.getRunningTaskCount()); //task1 and task2 are running

        continueLatch1.countDown(); //allow task1 to complete
        assertTrue(future1.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        for (long start = System.nanoTime(); executor.getRunningTaskCount() != 1 && System.nanoTime() - start < TIMEOUT_NS; Thread.sleep(200));
        assertEquals(1, executor.getRunningTaskCount()); //task2 is running

        executor.shutdown();
        assertEquals(1, executor.getRunningTaskCount()); //task2 is running

        continueLatch2.countDown(); //allow task2 to complete
        assertTrue(future2.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        for (long start = System.nanoTime(); executor.getRunningTaskCount() != 0 && System.nanoTime() - start < TIMEOUT_NS; Thread.sleep(200));
        assertEquals(0, executor.getRunningTaskCount()); //no tasks running
    }

    //Test that testqueueCapacityRemaining is correct as tasks are queued, run, and the maxQueueSize value is changed.
    @Test
    public void testQueueCapacityRemaining() throws Exception {
        PolicyExecutor executor = provider.create("testqueueCapacityRemaining").maxConcurrency(1).maxQueueSize(5);

        CountDownLatch blockingBeginLatch = new CountDownLatch(1);
        CountDownLatch blockingContinueLatch = new CountDownLatch(1);
        CountDownLatch beginLatch = new CountDownLatch(1);
        CountDownLatch continueLatch = new CountDownLatch(1);

        assertEquals(5, executor.queueCapacityRemaining()); //no tasks in queue
        CountDownTask blockingTask = new CountDownTask(blockingBeginLatch, blockingContinueLatch, TimeUnit.HOURS.toNanos(1));
        Future<Boolean> blockingFuture = executor.submit(blockingTask); //blockingTask should start and block on continueLatch
        assertTrue(blockingBeginLatch.await(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertEquals(5, executor.queueCapacityRemaining()); //no tasks in queue, maxQueueSize=5

        CountDownTask task1 = new CountDownTask(beginLatch, continueLatch, TimeUnit.HOURS.toNanos(1));
        Future<Boolean> future1 = executor.submit(task1); // should queue
        assertEquals(4, executor.queueCapacityRemaining()); //task1 in queue, maxQueueSize=5

        executor.maxQueueSize(6);
        assertEquals(5, executor.queueCapacityRemaining()); //task 1 in queue, maxQueueSize=6

        executor.maxQueueSize(4);
        assertEquals(3, executor.queueCapacityRemaining()); //task1 in queue, maxQueueSize=4

        CountDownTask task2 = new CountDownTask(new CountDownLatch(1), new CountDownLatch(0), TimeUnit.HOURS.toNanos(1));
        Future<Boolean> future2 = executor.submit(task2); // should queue
        assertEquals(2, executor.queueCapacityRemaining()); //task1 and task2 in queue, maxQueueSize=4

        blockingContinueLatch.countDown(); //allow blockingTask to complete
        assertTrue(beginLatch.await(TIMEOUT_NS, TimeUnit.NANOSECONDS)); //wait for task1 to begin running

        assertEquals(3, executor.queueCapacityRemaining()); //task2 in queue, maxQueueSize=4
        continueLatch.countDown(); //allow task1 and task2 to complete

        assertTrue(blockingFuture.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertTrue(future1.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertTrue(future2.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));

        assertEquals(4, executor.queueCapacityRemaining()); //no tasks in queue, maxQueueSize=4

        executor.shutdown();
        assertEquals(0, executor.queueCapacityRemaining()); //should return 0 after shutdown
    }
}
