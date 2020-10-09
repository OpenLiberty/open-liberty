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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.microprofile.faulttolerance.ExecutionContext;
import org.eclipse.microprofile.faulttolerance.exceptions.BulkheadException;
import org.junit.Test;

import com.ibm.ws.microprofile.faulttolerance.spi.BulkheadPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.Executor;
import com.ibm.ws.microprofile.faulttolerance.spi.ExecutorBuilder;
import com.ibm.ws.microprofile.faulttolerance.spi.FaultToleranceProvider;
import com.ibm.ws.microprofile.faulttolerance.spi.RetryPolicy;
import com.ibm.ws.microprofile.faulttolerance.test.util.AsyncTestFunction;
import com.ibm.ws.microprofile.faulttolerance.test.util.TestTask;

/**
 *
 */
public class BulkheadTest extends AbstractFTTest {

    @Test
    public void testBulkhead() throws InterruptedException, ExecutionException, TimeoutException {
        BulkheadPolicy bulkhead = FaultToleranceProvider.newBulkheadPolicy();
        bulkhead.setMaxThreads(2);

        ExecutorBuilder<String> builder = FaultToleranceProvider.newExecutionBuilder();
        builder.setBulkheadPolicy(bulkhead);
        Executor<String> executor = builder.build();

        ExecutorService executorService = Executors.newFixedThreadPool(10);

        CountDownLatch executionLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(2);
        TestTask testTask1 = new TestTask(executor, Duration.ofMillis(10000), executionLatch, completionLatch, "testBulkhead1");
        TestTask testTask2 = new TestTask(executor, Duration.ofMillis(10000), executionLatch, completionLatch, "testBulkhead2");
        TestTask testTask3 = new TestTask(executor, Duration.ofMillis(10000), executionLatch, completionLatch, "testBulkhead3");
        TestTask testTask4 = new TestTask(executor, Duration.ofMillis(10000), executionLatch, completionLatch, "testBulkhead4");

        long start = System.nanoTime();
        Future<String> task1 = executorService.submit(testTask1);
        Future<String> task2 = executorService.submit(testTask2);
        System.out.println(timeDiff(start) + " - First two tasks submitted");
        Thread.sleep(1000); //allow the first two to be picked up from the queue and begin execution ... (test) queue should then be clear
        System.out.println(timeDiff(start) + " - Submitting next two");
        Future<String> task3 = executorService.submit(testTask3);
        Future<String> task4 = executorService.submit(testTask4);
        System.out.println(timeDiff(start) + " - All four submitted");
        Thread.sleep(1000); //allow the second two to be picked up and begin execution

        executionLatch.countDown(); //this releases the functions to complete
        completionLatch.await(10000, TimeUnit.MILLISECONDS); //wait for the first two to finish

        String executions1 = task1.get(100, TimeUnit.MILLISECONDS); //if we allow just over 2000ms then the first two should complete
        System.out.println(timeDiff(start) + " - task1 got");
        assertEquals("testBulkhead1", executions1);
        String executions2 = task2.get(100, TimeUnit.MILLISECONDS);
        System.out.println(timeDiff(start) + " - task2 got");
        assertEquals("testBulkhead2", executions2);

        try {
            String executions3 = task3.get(100, TimeUnit.MILLISECONDS);
            fail("Task3 should have failed: " + executions3);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            assertTrue(cause + " was not a BulkheadException", cause instanceof BulkheadException);
        }
        try {
            String executions4 = task4.get(100, TimeUnit.MILLISECONDS);
            fail("Task4 should have failed");
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            assertTrue(cause + " was not a BulkheadException", cause instanceof BulkheadException);
        }
    }

    @Test
    public void testBulkheadRetrySmall() throws InterruptedException, ExecutionException, TimeoutException {
        BulkheadPolicy bulkhead = FaultToleranceProvider.newBulkheadPolicy();
        bulkhead.setMaxThreads(2);

        RetryPolicy retryPolicy = FaultToleranceProvider.newRetryPolicy();
        retryPolicy.setRetryOn(BulkheadException.class);
        retryPolicy.setDelay(Duration.ofMillis(100));

        ExecutorBuilder<String> builder = FaultToleranceProvider.newExecutionBuilder();
        builder.setBulkheadPolicy(bulkhead);
        builder.setRetryPolicy(retryPolicy);
        Executor<String> executor = builder.build();

        ExecutorService executorService = Executors.newFixedThreadPool(10);

        CountDownLatch latch1 = new CountDownLatch(1);
        CountDownLatch latch2 = new CountDownLatch(2);
        CountDownLatch latch3 = new CountDownLatch(2);
        TestTask testTask1 = new TestTask(executor, Duration.ofMillis(100), latch1, latch2, "testBulkheadRetrySmall1");
        TestTask testTask2 = new TestTask(executor, Duration.ofMillis(100), latch1, latch2, "testBulkheadRetrySmall2");
        TestTask testTask3 = new TestTask(executor, Duration.ofMillis(100), latch1, latch3, "testBulkheadRetrySmall3");
        TestTask testTask4 = new TestTask(executor, Duration.ofMillis(100), latch1, latch3, "testBulkheadRetrySmall4");

        long start = System.nanoTime();
        Future<String> task1 = executorService.submit(testTask1);
        Future<String> task2 = executorService.submit(testTask2);
        System.out.println(timeDiff(start) + " - First two tasks submitted");
        Thread.sleep(1000); //allow the first two to be picked up from the queue and begin execution ... (test) queue should then be clear
        System.out.println(timeDiff(start) + " - Submitting next two");
        Future<String> task3 = executorService.submit(testTask3);
        Future<String> task4 = executorService.submit(testTask4);
        System.out.println(timeDiff(start) + " - All four submitted");

        latch1.countDown(); //this releases all the tasks to complete
        latch2.await(10000, TimeUnit.MILLISECONDS); //wait for the first two to finish

        String executions1 = task1.get(100, TimeUnit.MILLISECONDS);
        System.out.println(timeDiff(start) + " - task1 got");
        assertEquals("testBulkheadRetrySmall1", executions1);
        String executions2 = task2.get(100, TimeUnit.MILLISECONDS);
        System.out.println(timeDiff(start) + " - task2 got");
        assertEquals("testBulkheadRetrySmall2", executions2);

        latch3.await(10000, TimeUnit.MILLISECONDS); //wait for the second two to finish

        String executions3 = task3.get(100, TimeUnit.MILLISECONDS);
        System.out.println(timeDiff(start) + " - task3 got");
        assertEquals("testBulkheadRetrySmall3", executions3);
        String executions4 = task4.get(100, TimeUnit.MILLISECONDS);
        System.out.println(timeDiff(start) + " - task4 got");
        assertEquals("testBulkheadRetrySmall4", executions4);
    }

    @Test
    public void testBulkheadRetryOverload() throws InterruptedException, ExecutionException, TimeoutException {
        BulkheadPolicy bulkhead = FaultToleranceProvider.newBulkheadPolicy();
        bulkhead.setMaxThreads(5);

        RetryPolicy retryPolicy = FaultToleranceProvider.newRetryPolicy();
        retryPolicy.setRetryOn(BulkheadException.class);
        retryPolicy.setMaxRetries(20);
        retryPolicy.setDelay(Duration.ofMillis(500));
        retryPolicy.setJitter(Duration.ofMillis(499));
        retryPolicy.setMaxDuration(Duration.ofMillis(20000)); //maximum duration of 20seconds

        ExecutorBuilder<String> builder = FaultToleranceProvider.newExecutionBuilder();
        builder.setBulkheadPolicy(bulkhead);
        builder.setRetryPolicy(retryPolicy);
        Executor<String> executor = builder.build();

        int numberOfTasks = 20;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfTasks * 2); //this is just the test driver pool

        CountDownLatch latch2 = new CountDownLatch(numberOfTasks);

        TestTask[] testTask = new TestTask[numberOfTasks];
        for (int i = 0; i < numberOfTasks; i++) {
            testTask[i] = new TestTask(executor, Duration.ofMillis(1000), null, latch2, "testBulkheadRetryOverload" + i);
        }

        Future<String>[] task = new Future[numberOfTasks];
        long start = System.nanoTime();
        for (int i = 0; i < numberOfTasks; i++) {
            task[i] = executorService.submit(testTask[i]);
        }
        System.out.println(timeDiff(start) + " - All tasks submitted");

        latch2.await(30000, TimeUnit.MILLISECONDS); //wait for all of the tasks to finish

        for (int i = 0; i < numberOfTasks; i++) {
            String executions = task[i].get(100, TimeUnit.MILLISECONDS);
            System.out.println(timeDiff(start) + " - task" + i + " got");
            assertEquals("testBulkheadRetryOverload" + i, executions);
        }
    }

    private static String timeDiff(long relativePoint) {
        long now = System.nanoTime();
        long diff = now - relativePoint;
        double seconds = ((double) diff / (double) 1000000000);
        return "" + seconds + "s";
    }

    @Test
    public void testAsyncBulkhead() throws InterruptedException, ExecutionException, TimeoutException {
        BulkheadPolicy bulkhead = FaultToleranceProvider.newBulkheadPolicy();
        bulkhead.setMaxThreads(20);
        bulkhead.setQueueSize(20);

        ExecutorBuilder<String> builder = FaultToleranceProvider.newExecutionBuilder();
        builder.setBulkheadPolicy(bulkhead);

        Executor<Future<String>> executor = builder.buildAsync(Future.class);

        Future<String>[] futures = new Future[10];
        CountDownLatch isRunningLatch = new CountDownLatch(10);
        CountDownLatch mayCompleteLatch = new CountDownLatch(1);
        CountDownLatch completedLatch = new CountDownLatch(10);
        try {
            for (int i = 0; i < 10; i++) {
                String id = "testAsyncBulkhead" + i;
                AsyncTestFunction callable = new AsyncTestFunction(Duration.ofMillis(10000), isRunningLatch, mayCompleteLatch, completedLatch, id);
                ExecutionContext context = executor.newExecutionContext(id, (Method) null, id);
                Future<String> future = executor.execute(callable, context);
                assertFalse(future.isDone());
                futures[i] = future;
            }

            // Allow running latch to reach 0 - tasks are ready to begin execution
            isRunningLatch.await(5000, TimeUnit.MILLISECONDS);
            assertEquals("all tasks have not started", 0, isRunningLatch.getCount());

            // Check tasks have not executed to completion, as tasks should be in await phase
            for (int i = 0; i < 10; i++) {
                assertFalse(futures[i].isDone());
            }

            // Allow tasks to execute, latch should be 0
            mayCompleteLatch.countDown();

            // Ensure called tasks have not thrown exceptions
            for (int i = 0; i < 10; i++) {
                String data = futures[i].get(2300, TimeUnit.MILLISECONDS);
                assertEquals("testAsyncBulkhead" + i, data);
            }
        } finally {
            // If any task did not complete, cleanup tasks
            mayCompleteLatch.countDown();
            for (int i = 0; i < 10; i++) {
                Future<String> future = futures[i];
                if (future != null && !future.isDone()) {
                    future.cancel(true);
                }
            }
        }
    }

    @Test
    public void testAsyncBulkheadQueueFull() throws InterruptedException, ExecutionException, TimeoutException {
        // Set policy such that only two tasks may be running at a time, with two queued tasks
        BulkheadPolicy bulkhead = FaultToleranceProvider.newBulkheadPolicy();
        bulkhead.setMaxThreads(2);
        bulkhead.setQueueSize(2);

        ExecutorBuilder<String> builder = FaultToleranceProvider.newExecutionBuilder();
        builder.setBulkheadPolicy(bulkhead);

        Executor<Future<String>> executor = builder.buildAsync(Future.class);

        Future<String>[] futures = new Future[5];
        CountDownLatch isRunningLatch = new CountDownLatch(2);
        CountDownLatch mayCompleteLatch = new CountDownLatch(1);
        CountDownLatch completedLatch = new CountDownLatch(4);
        try {

            // Create and add tasks to the executor which do not exceed the bulkhead limit
            for (int i = 0; i < 4; i++) {
                String id = "testAsyncBulkheadQueueFull" + i;
                ExecutionContext context = executor.newExecutionContext(id, (Method) null, id);
                AsyncTestFunction callable = new AsyncTestFunction(Duration.ofMillis(10000), isRunningLatch, mayCompleteLatch, completedLatch, id);
                futures[i] = executor.execute(callable, context);
                System.out.println(System.currentTimeMillis() + " Test " + id + " - submitted");
            }

            // Allow running latch to reach 0 - tasks are ready to begin execution
            isRunningLatch.await(5000, TimeUnit.MILLISECONDS);
            assertEquals("all tasks have not started", 0, isRunningLatch.getCount());

            // Non of the tasks are allowed to complete as mayCompleteLatch has not count down
            // First two tasks should be running, second two should be in queue
            for (int i = 0; i < 4; i++) {
                assertFalse("task " + i + " should not be complete", futures[i].isDone());
            }

            // Create and add a final task
            String id = "testAsyncBulkheadQueueFull4";
            ExecutionContext context = executor.newExecutionContext(id, (Method) null, id);
            AsyncTestFunction callable = new AsyncTestFunction(Duration.ofMillis(10000), isRunningLatch, mayCompleteLatch, completedLatch, id);

            // Final task should be rejected from queue, and throw a Bulkhead exception
            try {
                futures[4] = executor.execute(callable, context);
                System.out.println(System.currentTimeMillis() + " Test " + id + " - submitted");
                fail("Exception not thrown");
            } catch (BulkheadException e) {
                assertTrue("Should fail with bulkhead exception ", e instanceof BulkheadException);
            }
            // Allow tasks to complete
            mayCompleteLatch.countDown();
        } finally {
            // If any task did not complete, cleanup tasks
            for (int i = 0; i < 5; i++) {
                Future<String> future = futures[i];
                if (future != null && !future.isDone()) {
                    future.cancel(true);
                }
            }
        }

    }

    @Test
    public void testAsyncBulkheadDefaults() throws InterruptedException, ExecutionException, TimeoutException {
        BulkheadPolicy bulkhead = FaultToleranceProvider.newBulkheadPolicy();

        assertEquals(10, bulkhead.getMaxThreads());
        assertEquals(10, bulkhead.getQueueSize());
    }

}
