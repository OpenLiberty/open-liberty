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
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

import org.junit.After;
import org.junit.Test;

import com.ibm.ws.microprofile.faulttolerance.impl.policy.BulkheadPolicyImpl;
import com.ibm.ws.microprofile.faulttolerance20.state.AsyncBulkheadState.ExecutionReference;

public class AsyncBulkheadStateImplTest {

    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(10);
    private final AtomicInteger tasksStarted = new AtomicInteger(0);

    @After
    public void cleanup() {
        executor.shutdownNow();
        waitUntil(5, SECONDS, executor::isTerminated);
    }

    @Test
    public void testAsyncExecution() {
        BulkheadPolicyImpl bulkheadPolicy = new BulkheadPolicyImpl();
        bulkheadPolicy.setMaxThreads(2);
        bulkheadPolicy.setQueueSize(0);

        AsyncBulkheadStateImpl bulkheadState = new AsyncBulkheadStateImpl(executor, bulkheadPolicy);

        CompletableFuture<Void> waitingFuture = new CompletableFuture<Void>();

        WaitingTask task = new WaitingTask(waitingFuture);
        ExecutionReference ref = bulkheadState.submit(task);
        assertThat(ref.wasAccepted(), is(true));
        assertThat(task.isDone(), is(false));

        waitingFuture.complete(null);

        waitUntil(2, SECONDS, task::isDone);
    }

    @Test
    public void testTasksRejectedIfFull() {
        BulkheadPolicyImpl bulkheadPolicy = new BulkheadPolicyImpl();
        bulkheadPolicy.setMaxThreads(2);
        bulkheadPolicy.setQueueSize(0);

        AsyncBulkheadStateImpl bulkheadState = new AsyncBulkheadStateImpl(executor, bulkheadPolicy);

        CompletableFuture<Void> waitingFuture = new CompletableFuture<Void>();

        ArrayList<WaitingTask> tasks = new ArrayList<>();
        ArrayList<ExecutionReference> refs = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            WaitingTask task = new WaitingTask(waitingFuture);
            tasks.add(task);
            refs.add(bulkheadState.submit(task));
        }

        // First two should be accepted and be waiting for the waitingFuture
        for (int i = 0; i < 2; i++) {
            assertThat(refs.get(i).wasAccepted(), is(true));
            assertThat(tasks.get(i).isDone(), is(false));
        }

        // Rest should have been rejected as the bulkhead is full
        for (int i = 2; i < 10; i++) {
            assertThat(refs.get(i).wasAccepted(), is(false));
        }

        waitingFuture.complete(null);

        waitUntil(2, SECONDS, () -> tasks.stream().limit(2).allMatch(WaitingTask::isDone));
    }

    @Test
    public void testTasksQueuedIfFull() {
        BulkheadPolicyImpl bulkheadPolicy = new BulkheadPolicyImpl();
        bulkheadPolicy.setMaxThreads(2);
        bulkheadPolicy.setQueueSize(3);

        AsyncBulkheadStateImpl bulkheadState = new AsyncBulkheadStateImpl(executor, bulkheadPolicy);

        ArrayList<WaitingTask> tasks = new ArrayList<>();
        ArrayList<ExecutionReference> refs = new ArrayList<>();
        ArrayList<CompletableFuture<Void>> waitingFutures = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            CompletableFuture<Void> waitingFuture = new CompletableFuture<>();
            WaitingTask task = new WaitingTask(waitingFuture);
            waitingFutures.add(waitingFuture);
            tasks.add(task);
            refs.add(bulkheadState.submit(task));
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

    private class WaitingTask implements Runnable {
        private final Future<?> waitingFuture;
        private final AtomicBoolean isDone = new AtomicBoolean(false);

        public WaitingTask(Future<?> waitingFuture) {
            this.waitingFuture = waitingFuture;
        }

        @Override
        public void run() {
            try {
                tasksStarted.incrementAndGet();
                waitingFuture.get();
                isDone.set(true);
            } catch (InterruptedException | ExecutionException ex) {
                throw new RuntimeException(ex);
            }
        }

        public boolean isDone() {
            return isDone.get();
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

}
