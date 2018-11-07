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
import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

import org.eclipse.microprofile.faulttolerance.exceptions.BulkheadException;
import org.junit.Test;

import com.ibm.ws.microprofile.faulttolerance.impl.policy.BulkheadPolicyImpl;
import com.ibm.ws.microprofile.faulttolerance20.impl.MethodResult;

public class SyncBulkheadStateImplTest {

    ExecutorService executor = Executors.newFixedThreadPool(10);

    /**
     * Check that the bulkhead will reject executions if it's full
     */
    @Test
    public void testSyncBulkhead() throws InterruptedException, ExecutionException {
        BulkheadPolicyImpl bulkheadPolicy = new BulkheadPolicyImpl();
        bulkheadPolicy.setMaxThreads(2);

        SyncBulkheadStateImpl bulkheadState = new SyncBulkheadStateImpl(bulkheadPolicy);

        CompletableFuture<Void> waitingFuture = new CompletableFuture<>();

        List<Future<MethodResult<String>>> futures = new ArrayList<>();

        futures.add(submitToBulkhead(bulkheadState, waitOnFuture(waitingFuture)));
        futures.add(submitToBulkhead(bulkheadState, waitOnFuture(waitingFuture)));
        futures.add(submitToBulkhead(bulkheadState, waitOnFuture(waitingFuture)));
        futures.add(submitToBulkhead(bulkheadState, waitOnFuture(waitingFuture)));

        // Two executions should complete quickly with bulkhead exception
        waitUntil(2, SECONDS, () -> futures.stream().filter(Future::isDone).count() == 2);

        // Check that the ones that have failed failed with bulkhead exception
        for (Future<MethodResult<String>> future : futures) {
            if (future.isDone()) {
                MethodResult<String> result = future.get();
                assertThat(result.getFailure(), instanceOf(BulkheadException.class));
                assertThat(result.isFailure(), is(true));
            }
        }

        // Complete all tasks
        waitingFuture.complete(null);

        // Wait until they're all finished
        waitUntil(2, SECONDS, () -> futures.stream().allMatch(Future::isDone));

        // Get the results out
        List<MethodResult<String>> results = new ArrayList<>();
        for (Future<MethodResult<String>> future : futures) {
            results.add(future.get());
        }

        // Check that two finished without exception (the two that made it into the bulkhead)
        assertThat(results.stream().filter(not(MethodResult::isFailure)).count(), equalTo(2L));

        // Assert that the result came back
        assertThat(results.stream().filter(not(MethodResult::isFailure)).map(MethodResult::getResult).collect(toList()), everyItem(equalTo("ok")));
    }

    /**
     * Test we can run several tasks through the bulkhead
     */
    @Test
    public void testRepeatedExecution() {
        BulkheadPolicyImpl bulkheadPolicy = new BulkheadPolicyImpl();
        bulkheadPolicy.setMaxThreads(2);

        SyncBulkheadStateImpl bulkheadState = new SyncBulkheadStateImpl(bulkheadPolicy);

        for (int i = 0; i < 100; i++) {
            MethodResult<String> result = bulkheadState.run(() -> "ok");
            assertFalse(result.isFailure());
            assertThat(result.getResult(), equalTo("ok"));
        }
    }

    private <T> Future<MethodResult<T>> submitToBulkhead(SyncBulkheadStateImpl bulkheadState, Callable<T> callable) {
        return executor.submit(() -> bulkheadState.run(callable));
    }

    private Callable<String> waitOnFuture(Future<?> waitingFuture) {
        return () -> {
            waitingFuture.get();
            return "ok";
        };
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

    private <T> Predicate<T> not(Predicate<T> pred) {
        return pred.negate();
    }
}
