/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.faulttolerance.test;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.microprofile.faulttolerance.ExecutionContext;
import org.junit.Test;

import com.ibm.ws.microprofile.faulttolerance.impl.policy.TimeoutPolicyImpl;
import com.ibm.ws.microprofile.faulttolerance.spi.Executor;
import com.ibm.ws.microprofile.faulttolerance.spi.ExecutorBuilder;
import com.ibm.ws.microprofile.faulttolerance.spi.FaultToleranceProvider;
import com.ibm.ws.microprofile.faulttolerance.test.util.AsyncTestFunction;

/**
 *
 */
public class AsyncTest extends AbstractFTTest {

    private static final int TASKS = 5;
    private static final long DURATION_UNIT = 1000;
    private static final long TASK_DURATION = 4 * DURATION_UNIT;
    private static final long FUTURE_TIMEOUT = 2 * TASK_DURATION;

    @Test
    public void testAsync() throws InterruptedException, ExecutionException, TimeoutException {
        ExecutorBuilder<String, String> builder = FaultToleranceProvider.newExecutionBuilder();
        Executor<Future<String>> executor = builder.buildAsync(Future.class);

        Future<String>[] futures = new Future[TASKS];
        try {
            for (int i = 0; i < TASKS; i++) {
                String id = "testAsync" + i;
                AsyncTestFunction callable = new AsyncTestFunction(Duration.ofMillis(TASK_DURATION), id);
                ExecutionContext context = executor.newExecutionContext(id, (Method) null, id);
                Future<String> future = executor.execute(callable, context);
                assertFalse(future.isDone());
                futures[i] = future;
            }

            for (int i = 0; i < TASKS; i++) {
                String data = futures[i].get(FUTURE_TIMEOUT, TimeUnit.MILLISECONDS);
                assertEquals("testAsync" + i, data);
            }
        } finally {
            for (int i = 0; i < TASKS; i++) {
                Future<String> future = futures[i];
                if (future != null && !future.isDone()) {
                    future.cancel(true);
                }
            }
        }
    }

    @Test
    public void testAsyncCancel() throws InterruptedException, ExecutionException, TimeoutException {
        ExecutorBuilder<String, String> builder = FaultToleranceProvider.newExecutionBuilder();
        Executor<Future<String>> executor = builder.buildAsync(Future.class);

        String id = "testAsyncCancel";
        CountDownLatch runningLatch = new CountDownLatch(1);
        CountDownLatch completedLatch = new CountDownLatch(1);
        AsyncTestFunction callable = new AsyncTestFunction(Duration.ofMillis(10000), runningLatch, completedLatch, id);
        ExecutionContext context = executor.newExecutionContext(id, (Method) null, id);
        Future<String> future = executor.execute(callable, context);
        assertFalse(future.isDone());
        assertFalse(future.isCancelled());
        boolean cancelled = future.cancel(true);
        assertTrue("Future not cancelled", cancelled);
        assertTrue("Future.isDone() returned false", future.isDone());
        assertTrue("Future.isCancelled() returned false", future.isCancelled());
    }

    @Test
    public void testAsyncTimeout() throws InterruptedException {
        TimeoutPolicyImpl timeout = new TimeoutPolicyImpl();
        timeout.setTimeout(Duration.ofMillis(500));

        ExecutorBuilder<String, String> builder = FaultToleranceProvider.newExecutionBuilder();
        builder.setTimeoutPolicy(timeout);
        Executor<Future<String>> executor = builder.buildAsync(Future.class);

        String id = "testAsyncTimeout";
        ExecutionContext context = executor.newExecutionContext(id, null);

        Callable<Future<String>> nonInterruptableTask = () -> {
            long startTime = System.nanoTime();
            while (TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime) < 2000) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                } //Ignore interruption
            }
            return CompletableFuture.completedFuture("OK");
        };

        Future<String> future = executor.execute(nonInterruptableTask, context);
        assertFalse(future.isDone());

        Thread.sleep(700); // Sleep until Timeout should have fired

        assertTrue("Future did not complete quickly enough", future.isDone());
        try {
            future.get();
            fail("Future did not throw exception");
        } catch (ExecutionException e) {
            assertThat("Exception thrown by future", e.getCause(), instanceOf(org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException.class));
        }
    }

}
