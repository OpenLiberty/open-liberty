/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.faulttolerance20.state.impl;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

import org.junit.After;
import org.junit.Test;

import com.ibm.ws.microprofile.faulttolerance.impl.policy.BulkheadPolicyImpl;
import com.ibm.ws.microprofile.faulttolerance.utils.DummyMetricRecorder;
import com.ibm.ws.microprofile.faulttolerance20.state.AsyncBulkheadState.AsyncBulkheadTask;
import com.ibm.ws.microprofile.faulttolerance20.state.AsyncBulkheadState.BulkheadReservation;
import com.ibm.ws.microprofile.faulttolerance20.state.AsyncBulkheadState.ExceptionHandler;
import com.ibm.ws.microprofile.faulttolerance20.state.AsyncBulkheadState.ExecutionReference;

@SuppressWarnings("restriction") // Unit test accesses non-exported *PolicyImpl classes
public class AsyncBulkheadStateImplTest {

    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(10);
    private final AtomicInteger tasksStarted = new AtomicInteger(0);

    private final ArrayList<Throwable> loggedExceptions = new ArrayList<>();
    private final ExceptionHandler testExceptionHandler = loggedExceptions::add;
    private final ArrayList<CompletableFuture<?>> waitingFutures = new ArrayList<>();

    @After
    public void cleanup() {
        try {
            assertThat("Logged exceptions", loggedExceptions, is(empty()));
            executor.shutdownNow();
            for (CompletableFuture<?> f : waitingFutures) {
                f.complete(null);
            }
        } finally {
            loggedExceptions.clear();
            waitingFutures.clear();
            waitUntil(5, SECONDS, executor::isTerminated);
        }
    }

    private CompletableFuture<Void> newWaitingFuture() {
        CompletableFuture<Void> future = new CompletableFuture<Void>();
        waitingFutures.add(future);
        return future;
    }

    @Test
    public void testAsyncExecution() {
        BulkheadPolicyImpl bulkheadPolicy = new BulkheadPolicyImpl();
        bulkheadPolicy.setMaxThreads(2);
        bulkheadPolicy.setQueueSize(1);

        AsyncBulkheadStateImpl bulkheadState = new AsyncBulkheadStateImpl(executor, bulkheadPolicy, DummyMetricRecorder.get());

        CompletableFuture<Void> waitingFuture = newWaitingFuture();

        WaitingTask task = new WaitingTask(waitingFuture);
        ExecutionReference ref = bulkheadState.submit(task, testExceptionHandler);
        assertThat(ref.wasAccepted(), is(true));
        assertThat(task.isDone(), is(false));

        waitingFuture.complete(null);

        waitUntil(2, SECONDS, task::isDone);
    }

    @Test
    public void testTasksRejectedIfFull() {
        BulkheadPolicyImpl bulkheadPolicy = new BulkheadPolicyImpl();
        bulkheadPolicy.setMaxThreads(2);
        bulkheadPolicy.setQueueSize(1);

        AsyncBulkheadStateImpl bulkheadState = new AsyncBulkheadStateImpl(executor, bulkheadPolicy, DummyMetricRecorder.get());

        CompletableFuture<Void> waitingFuture = newWaitingFuture();

        ArrayList<WaitingTask> tasks = new ArrayList<>();
        ArrayList<ExecutionReference> refs = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            WaitingTask task = new WaitingTask(waitingFuture);
            tasks.add(task);
            ExecutionReference ref = bulkheadState.submit(task, testExceptionHandler);
            refs.add(ref);
        }

        // First three should be accepted and be waiting for the waitingFuture
        for (int i = 0; i < 3; i++) {
            assertThat(refs.get(i).wasAccepted(), is(true));
            assertThat(tasks.get(i).isDone(), is(false));
        }

        // Rest should have been rejected as the bulkhead is full
        for (int i = 3; i < 10; i++) {
            assertThat(refs.get(i).wasAccepted(), is(false));
        }

        waitingFuture.complete(null);

        waitUntil(2, SECONDS, () -> tasks.stream().limit(3).allMatch(WaitingTask::isDone));
    }

    @Test
    public void testTasksQueuedIfFull() {
        BulkheadPolicyImpl bulkheadPolicy = new BulkheadPolicyImpl();
        bulkheadPolicy.setMaxThreads(2);
        bulkheadPolicy.setQueueSize(3);

        AsyncBulkheadStateImpl bulkheadState = new AsyncBulkheadStateImpl(executor, bulkheadPolicy, DummyMetricRecorder.get());

        ArrayList<WaitingTask> tasks = new ArrayList<>();
        ArrayList<ExecutionReference> refs = new ArrayList<>();
        ArrayList<CompletableFuture<Void>> waitingFutures = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            CompletableFuture<Void> waitingFuture = newWaitingFuture();
            WaitingTask task = new WaitingTask(waitingFuture);
            waitingFutures.add(waitingFuture);
            tasks.add(task);
            ExecutionReference ref = bulkheadState.submit(task, testExceptionHandler);
            refs.add(ref);
        }

        // First five should be accepted and be waiting for the waitingFuture
        for (int i = 0; i < 5; i++) {
            assertThat(refs.get(i).wasAccepted(), is(true));
            assertThat(tasks.get(i).isDone(), is(false));
        }

        // Rest should have been rejected as the bulkhead is full
        for (int i = 5; i < 10; i++) {
            assertThat(refs.get(i).wasAccepted(), is(false));
        }

        // Only two of the tasks should start
        waitUntil(2, SECONDS, () -> tasksStarted.get() == 2);

        for (int i = 0; i < 3; i++) {
            // Need a final copy of i to use inside lambdas
            final int j = i;

            // Complete waiting future for task i
            waitingFutures.get(j).complete(null);

            // Check that task i now finishes
            waitUntil(2, SECONDS, () -> tasks.get(j).isDone());

            // Check that a new task is started
            waitUntil(2, SECONDS, () -> tasksStarted.get() == j + 3);
        }

        // Complete the rest of the tasks
        for (int i = 3; i < 10; i++) {
            waitingFutures.get(i).complete(null);
        }

    }

    @Test
    public void testAbortRunning() {
        BulkheadPolicyImpl bulkheadPolicy = new BulkheadPolicyImpl();
        bulkheadPolicy.setMaxThreads(2);
        bulkheadPolicy.setQueueSize(3);

        AsyncBulkheadStateImpl bulkheadState = new AsyncBulkheadStateImpl(executor, bulkheadPolicy, DummyMetricRecorder.get());

        CompletableFuture<Void> waitingFuture = newWaitingFuture();
        WaitingTask task = new WaitingTask(waitingFuture);
        ExecutionReference ref = bulkheadState.submit(task, testExceptionHandler);

        waitUntil(2, SECONDS, () -> tasksStarted.get() == 1);
        ref.abort(true);

        expectException(task.getResult(), InterruptedException.class);
    }

    @Test
    public void testAbortQueued() {
        BulkheadPolicyImpl bulkheadPolicy = new BulkheadPolicyImpl();
        bulkheadPolicy.setMaxThreads(2);
        bulkheadPolicy.setQueueSize(3);

        AsyncBulkheadStateImpl bulkheadState = new AsyncBulkheadStateImpl(executor, bulkheadPolicy, DummyMetricRecorder.get());

        ArrayList<ExecutionReference> refs = new ArrayList<>();

        CompletableFuture<Void> waitingFuture = newWaitingFuture();
        for (int i = 0; i < 5; i++) {
            ExecutionReference ref = bulkheadState.submit(new WaitingTask(waitingFuture), testExceptionHandler);
            assertThat("Execution accepted", ref.wasAccepted(), is(true));
            refs.add(ref);
        }

        ExecutionReference ref = bulkheadState.submit(new WaitingTask(waitingFuture), testExceptionHandler);
        assertThat("Execution accepted when queue full", ref.wasAccepted(), is(false));

        // Abort one of the queued tasks
        refs.get(3).abort(true);

        // Should now be space to queue one more task
        ref = bulkheadState.submit(new WaitingTask(waitingFuture), testExceptionHandler);
        assertThat("Execution accepted after queued task aborted", ref.wasAccepted(), is(true));
    }

    @Test
    public void testDelayedCompletion() throws InterruptedException, ExecutionException, TimeoutException {
        BulkheadPolicyImpl bulkheadPolicy = new BulkheadPolicyImpl();
        bulkheadPolicy.setMaxThreads(1);
        bulkheadPolicy.setQueueSize(1);

        CompletableFuture<BulkheadReservation> reservationFuture = new CompletableFuture<>();

        AsyncBulkheadTask nonCompletingTask = (r) -> {
            try {
                reservationFuture.complete(r);
            } catch (Exception e) {
            }
        };

        AsyncBulkheadStateImpl bulkheadState = new AsyncBulkheadStateImpl(executor, bulkheadPolicy, DummyMetricRecorder.get());
        bulkheadState.submit(nonCompletingTask, testExceptionHandler);

        // First task should not clear the bulkhead until release is called on the reservation, even though the method itself will complete
        BulkheadReservation reservation = reservationFuture.get(2, SECONDS);

        // Short wait to allow the first task the opportunity to complete, even though we expect it not to
        Thread.sleep(100);
        CompletableFuture<Void> waitingFuture = newWaitingFuture();

        // Next task should be queued
        ExecutionReference ref = bulkheadState.submit(new WaitingTask(waitingFuture), testExceptionHandler);
        assertThat("Task accepted", ref.wasAccepted(), is(true));

        // Next task should be rejected because first task is still incomplete
        ref = bulkheadState.submit(new WaitingTask(waitingFuture), testExceptionHandler);
        assertThat("Task accepted", ref.wasAccepted(), is(false));

        // Releasing the reservation should allow us to queue another task
        reservation.release();
        ref = bulkheadState.submit(new WaitingTask(waitingFuture), testExceptionHandler);
        assertThat("Task accepted", ref.wasAccepted(), is(true));
    }

    /**
     * Test for correct behaviour if a task is accepted by the bulkhead but is later rejected when submitted to the global executor
     */
    @Test
    public void testRejectedByExecutor() {
        BulkheadPolicyImpl bulkheadPolicy = new BulkheadPolicyImpl();
        bulkheadPolicy.setMaxThreads(1);
        bulkheadPolicy.setQueueSize(1);

        AsyncBulkheadStateImpl bulkheadState = new AsyncBulkheadStateImpl(new MockRejectingExecutor(), bulkheadPolicy, DummyMetricRecorder.get());
        CompletableFuture<?> waitingFuture = newWaitingFuture();

        // Tests two things:
        // 1) An exception is reported via the exception handler if the executorService rejects the task
        // 2) When an exception occurs, the bulkhead permit is released
        for (int i = 0; i < 10; i++) {
            WaitingTask task = new WaitingTask(waitingFuture);
            AtomicBoolean didFail = new AtomicBoolean(false);
            ExecutionReference ref = bulkheadState.submit(task, (e) -> didFail.set(true));
            assertTrue("Execution was not accepted for task " + i, ref.wasAccepted());
            assertTrue("Failure was not reported for task " + i, didFail.get());
        }

    }

    private class WaitingTask implements AsyncBulkheadTask {
        private final Future<?> waitingFuture;
        private final CompletableFuture<Void> resultFuture;
        private final AtomicBoolean isDone = new AtomicBoolean(false);

        public WaitingTask(Future<?> waitingFuture) {
            this.waitingFuture = waitingFuture;
            this.resultFuture = new CompletableFuture<Void>();
        }

        @Override
        public void run(BulkheadReservation reservation) {
            try {
                tasksStarted.incrementAndGet();
                waitingFuture.get();
                isDone.set(true);
                resultFuture.complete(null);
            } catch (InterruptedException | ExecutionException ex) {
                resultFuture.completeExceptionally(ex);
            } finally {
                reservation.release();
            }
        }

        public boolean isDone() {
            return isDone.get();
        }

        public Future<Void> getResult() {
            return resultFuture;
        }

    }

    private void waitUntil(long time, TimeUnit timeUnit, BooleanSupplier condition) {
        long timeNanos = TimeUnit.NANOSECONDS.convert(time, timeUnit);
        long startNanos = System.nanoTime();
        while (!condition.getAsBoolean()) {
            if (System.nanoTime() - startNanos > timeNanos) {
                fail("Timed out while waiting for condition to become true");
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new AssertionError("Interrupted while waiting for condition to become true", e);
            }
        }
    }

    private void expectException(Future<?> future, Class<? extends Exception> clazz) {
        try {
            future.get(2, SECONDS);
            fail("Future did not complete with an exception");
        } catch (ExecutionException e) {
            assertThat("Exception returned from future", e.getCause(), instanceOf(clazz));
        } catch (InterruptedException e) {
            fail("Interrupted while waiting for future to return an exception");
        } catch (TimeoutException e) {
            fail("Future did not return any result within two seconds");
        }
    }

}
