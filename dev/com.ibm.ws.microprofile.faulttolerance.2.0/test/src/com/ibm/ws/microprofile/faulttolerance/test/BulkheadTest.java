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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.microprofile.faulttolerance.exceptions.BulkheadException;
import org.junit.Test;

import com.ibm.ws.microprofile.faulttolerance.spi.BulkheadPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.Executor;
import com.ibm.ws.microprofile.faulttolerance.spi.ExecutorBuilder;
import com.ibm.ws.microprofile.faulttolerance.spi.FaultToleranceProvider;
import com.ibm.ws.microprofile.faulttolerance.spi.RetryPolicy;
import com.ibm.ws.microprofile.faulttolerance.test.util.TestTask;

/**
 *
 */
public class BulkheadTest extends AbstractFTTest {

    @Test
    public void testBulkhead() throws InterruptedException, ExecutionException, TimeoutException {
        BulkheadPolicy bulkhead = FaultToleranceProvider.newBulkheadPolicy();
        bulkhead.setMaxThreads(2);

        ExecutorBuilder<String, String> builder = FaultToleranceProvider.newExecutionBuilder();
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

        ExecutorBuilder<String, String> builder = FaultToleranceProvider.newExecutionBuilder();
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

        ExecutorBuilder<String, String> builder = FaultToleranceProvider.newExecutionBuilder();
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
    public void testBulkheadDefaults() throws InterruptedException, ExecutionException, TimeoutException {
        BulkheadPolicy bulkhead = FaultToleranceProvider.newBulkheadPolicy();

        assertEquals(10, bulkhead.getMaxThreads());
        assertEquals(10, bulkhead.getQueueSize());
    }

}
