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
package com.ibm.ws.microprofile.faulttolerance.test;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.microprofile.faulttolerance.impl.async.QueuedFuture;
import com.ibm.ws.microprofile.faulttolerance.test.util.TestException;

/**
 * Tests for basic functionality for QueuedFuture
 */
public class QueuedFutureTest {

    private static ExecutorService executor;

    private final List<CompletableFuture<?>> latches = new ArrayList<>();

    @BeforeClass
    public static void setupExecutor() {
        executor = Executors.newFixedThreadPool(10);
    }

    @AfterClass
    public static void cleanupExecutor() throws InterruptedException {
        executor.shutdownNow();
        executor.awaitTermination(5, TimeUnit.MINUTES);
    }

    @Test
    public void testCancellation() {
        QueuedFuture<Void> qf = new QueuedFuture<>();

        CompletableFuture<Void> latch = newLatch();

        qf.start(executor, waitOnLatch(latch), null);
        assertFalse("Queued future cancelled", qf.isCancelled());
        assertFalse("Queued future done", qf.isDone());

        assertTrue("Failed to cancel queued future", qf.cancel(true));

        assertTrue("Queued future not cancelled", qf.isCancelled());
        assertTrue("Queued future not done", qf.isDone());

        // Reading the contract for Future.cancel(), I thought this should return false.
        // However, all implementations I could find return true when canceling an already cancelled future.
        assertTrue("Cancelling already cancelled future should return true", qf.cancel(true));
    }

    @Test
    public void testGet() throws InterruptedException, ExecutionException, TimeoutException {
        QueuedFuture<String> qf = new QueuedFuture<>();

        CompletableFuture<String> latch = newLatch();

        qf.start(executor, waitOnLatch(latch), null);
        assertFalse("Queued future cancelled", qf.isCancelled());
        assertFalse("Queued future done", qf.isDone());

        latch.complete("OK");
        assertEquals("Incorrect result returned", "OK", qf.get(2, TimeUnit.MINUTES));
        assertTrue("Queued future not done", qf.isDone());
        assertFalse("Queued future cancelled", qf.isCancelled());

        assertFalse("Succeeded in cancelling already completed future", qf.cancel(true));
    }

    @Test
    public void testAbort() {
        QueuedFuture<String> qf = new QueuedFuture<>();

        CompletableFuture<String> latch = newLatch();

        qf.start(executor, waitOnLatch(latch), null);
        assertFalse("Queued future cancelled", qf.isCancelled());
        assertFalse("Queued future done", qf.isDone());

        qf.abort(new TestException());
        assertThrows(qf, TestException.class);
        assertTrue("Queued future not done", qf.isDone());
        assertFalse("Queued future cancelled", qf.isCancelled());

        assertFalse("Succeeded in cancelling aborted future", qf.cancel(true));

        // The task completing should not change the queued future result
        latch.complete("OK");
        assertTrue("Queued future not done", qf.isDone());
        assertFalse("Queued future cancelled", qf.isCancelled());
        assertFalse("Succeeded in cancelling aborted future", qf.cancel(true));
        assertThrows(qf, TestException.class);
    }

    private void assertThrows(Future<?> future, Class<? extends Exception> expectedException) {
        try {
            future.get(2, TimeUnit.MINUTES);
            fail("Future did not throw expected exception");
        } catch (ExecutionException e) {
            assertThat("Thrown exception is the wrong type", e.getCause(), instanceOf(expectedException));
        } catch (InterruptedException e) {
            throw new AssertionError("Getting future result was interrupted", e);
        } catch (CancellationException e) {
            throw new AssertionError("Future was cancelled", e);
        } catch (TimeoutException e) {
            throw new AssertionError("Timed out waiting for future to complete", e);
        }
    }

    private <T> Callable<Future<T>> waitOnLatch(Future<T> latch) {
        return () -> CompletableFuture.completedFuture(latch.get());
    }

    private <T> CompletableFuture<T> newLatch() {
        CompletableFuture<T> latch = new CompletableFuture<>();
        latches.add(latch);
        return latch;
    }

}
