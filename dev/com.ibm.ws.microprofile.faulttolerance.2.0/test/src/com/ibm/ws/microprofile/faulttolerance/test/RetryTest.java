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

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.microprofile.faulttolerance.ExecutionContext;
import org.junit.Test;

import com.ibm.ws.microprofile.faulttolerance.spi.Executor;
import com.ibm.ws.microprofile.faulttolerance.spi.ExecutorBuilder;
import com.ibm.ws.microprofile.faulttolerance.spi.FTExecutionContext;
import com.ibm.ws.microprofile.faulttolerance.spi.FaultToleranceProvider;
import com.ibm.ws.microprofile.faulttolerance.spi.RetryPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.TimeoutPolicy;
import com.ibm.ws.microprofile.faulttolerance.test.util.AsyncTestFunction;
import com.ibm.ws.microprofile.faulttolerance.test.util.TestFunction;

/**
 *
 */
public class RetryTest extends AbstractFTTest {

    @Test
    public void testRetry() {
        RetryPolicy retry = FaultToleranceProvider.newRetryPolicy();
        retry.setMaxRetries(3);

        ExecutorBuilder<String, String> builder = FaultToleranceProvider.newExecutionBuilder();
        builder.setRetryPolicy(retry);

        Executor<String> executor = builder.build();

        String id = "testRetry";
        TestFunction callable = new TestFunction(2, id);//first two executions will throw an exception

        FTExecutionContext context = executor.newExecutionContext(id, (Method) null, id);
        try {
            executor.execute(callable, context);
            assertEquals(3, callable.getExecutions());
        } finally {
            context.close();
        }
    }

    @Test
    public void testRetryTimeout() {
        RetryPolicy retry = FaultToleranceProvider.newRetryPolicy();
        retry.setMaxRetries(10);
        //100ms between retries
        retry.setDelay(Duration.ofMillis(100));

        ExecutorBuilder<String, String> builder = FaultToleranceProvider.newExecutionBuilder();
        builder.setRetryPolicy(retry);

        TimeoutPolicy timeout = FaultToleranceProvider.newTimeoutPolicy();
        timeout.setTimeout(Duration.ofMillis(500));
        builder.setTimeoutPolicy(timeout);

        Executor<String> executor = builder.build();

        String id = "testRetryTimeout";
        TestFunction callable = new TestFunction(8, id);//first eight executions will throw an exception
        //so will be around 900ms before the execution works
        //but the timeout should get reset on each try

        FTExecutionContext context = executor.newExecutionContext(id, (Method) null, id);
        try {
            executor.execute(callable, context);
            assertEquals(9, callable.getExecutions());
        } finally {
            context.close();
        }
    }

    @Test
    public void testAsyncRetry() throws InterruptedException, ExecutionException, TimeoutException {
        CountDownLatch latch = new CountDownLatch(3);
        RetryPolicy retry = FaultToleranceProvider.newRetryPolicy();
        retry.setMaxRetries(3);

        ExecutorBuilder<String, String> builder = FaultToleranceProvider.newExecutionBuilder();
        builder.setRetryPolicy(retry);

        Executor<Future<String>> executor = builder.buildAsync(Future.class);

        String id = "testAsyncRetry";
        AsyncTestFunction callable = new AsyncTestFunction(2, id, latch);//first two executions will throw an exception

        ExecutionContext context = executor.newExecutionContext(id, (Method) null, id);
        Future<String> future = null;
        try {
            future = executor.execute(callable, context);

            latch.await(5000, TimeUnit.MILLISECONDS);

            String result = future.get(1000, TimeUnit.MILLISECONDS);

            assertEquals(id, result);
            assertEquals(3, callable.getExecutions());
        } finally {
            if (future != null && !future.isDone()) {
                future.cancel(true);
            }
        }
    }

}
