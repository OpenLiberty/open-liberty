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
package com.ibm.ws.microprofile.faulttolerance20.impl;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.emptyCollectionOf;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class FutureShellTest {

    /**
     * Time to wait for an event which is expected to occur
     */
    private static final long TIMEOUT_MS = 20000;

    /**
     * Time to wait to simulate work
     */
    private static final long RUNTIME_MS = 100;

    private static ScheduledExecutorService executor;

    @BeforeClass
    public static void setup() {
        executor = Executors.newScheduledThreadPool(10);
    }

    @AfterClass
    public static void cleanup() {
        executor.shutdown();
    }

    @Test
    public void testFutureShell() throws InterruptedException, ExecutionException, TimeoutException {
        String value = "testFutureShell";
        FutureShell<String> futureShell = new FutureShell<String>();

        CompletableFuture<String> completableFuture = new CompletableFuture<>();
        futureShell.setDelegate(completableFuture);
        executor.schedule(() -> {
            completableFuture.complete(value);
        }, RUNTIME_MS, TimeUnit.MILLISECONDS);
        assertEquals(value, futureShell.get(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testFailedFutureShell() throws InterruptedException, TimeoutException {
        FutureShell<String> futureShell = new FutureShell<String>();

        CompletableFuture<String> completableFuture = new CompletableFuture<>();
        futureShell.setDelegate(completableFuture);

        Exception exception = new IllegalStateException();
        executor.schedule(() -> {
            completableFuture.completeExceptionally(exception);
        }, RUNTIME_MS, TimeUnit.MILLISECONDS);

        try {
            futureShell.get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
            fail("No ExecutionException was thrown");
        } catch (ExecutionException e) {
            assertThat("Exception was wrong", e.getCause(), sameInstance(exception));
        }
    }

    @Test
    public void testCancelBeforeSettingDelegate() throws InterruptedException, ExecutionException, TimeoutException {
        String value = "testCancelBeforeSettingDelegate";
        FutureShell<String> futureShell = new FutureShell<String>();
        futureShell.cancel(false);
        boolean canceled = false;

        try {
            Future<String> completableFuture = CompletableFuture.completedFuture(value);
            futureShell.setDelegate(completableFuture);
            futureShell.get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
            fail("No CancellationException was thrown");
        } catch (CancellationException e) { //expected
            canceled = true;
        }
        assertTrue(canceled);
    }

    @Test
    public void testCancelAfterSettingDelegate() throws InterruptedException, ExecutionException, TimeoutException {
        FutureShell<String> futureShell = new FutureShell<String>();
        boolean canceled = false;

        Future<String> completableFuture = new CompletableFuture<>();
        futureShell.setDelegate(completableFuture);
        futureShell.cancel(true);

        try {
            futureShell.get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
            fail("No CancellationException was thrown");
        } catch (CancellationException e) {
            canceled = true;
        }
        assertTrue(canceled);
    }

    @Test
    public void testGetBeforeSettingDelegate() throws InterruptedException, ExecutionException, TimeoutException {
        String value = "testGetBeforeSettingDelegate";
        FutureShell<String> futureShell = new FutureShell<String>();
        CompletableFuture<String> delegate = new CompletableFuture<>();

        Future<String> result = executor.submit(() -> {
            return futureShell.get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        });

        Thread.sleep(RUNTIME_MS);
        futureShell.setDelegate(delegate);
        delegate.complete(value);
        assertThat("The FutureShell had the wrong result",
                   result.get(TIMEOUT_MS, TimeUnit.MILLISECONDS), containsString(value));
    }

    @Test
    public void testGetBeforeSettingFailedDelegate() throws InterruptedException, TimeoutException {
        FutureShell<String> futureShell = new FutureShell<String>();
        CompletableFuture<String> delegate = new CompletableFuture<>();
        Exception exception = new IllegalStateException();

        Future<String> result = executor.submit(() -> {
            return futureShell.get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        });
        Thread.sleep(RUNTIME_MS);
        futureShell.setDelegate(delegate);
        delegate.completeExceptionally(exception);

        try {
            result.get(1000, TimeUnit.MILLISECONDS);
            fail("ExecutionException was not thrown");
        } catch (ExecutionException e) {
            Throwable exceptionFromResult = e.getCause();
            assertThat(exceptionFromResult, instanceOf(ExecutionException.class));
            Throwable exceptionFromFutureShell = exceptionFromResult.getCause();
            assertThat("Exception was wrong", exceptionFromFutureShell, sameInstance(exception));
        }
    }

    @Test
    public void testCancellationCallbackFires() {
        String value = "testCancellationCallbackFires";
        final Set<String> callbackResults = new HashSet<String>();
        Consumer<Boolean> cancellationCallback = createCancellationCallback(value, callbackResults);
        FutureShell<String> futureShell = new FutureShell<String>();

        futureShell.setCancellationCallback(cancellationCallback);
        futureShell.cancel(false);

        assertThat(callbackResults, contains(value));
    }

    @Test
    public void testCancellationCallbackWontFireWithDelegate() throws InterruptedException {
        String value = "testCancellationCallbackWontFireWithDelegate";
        final Set<String> callbackResults = new HashSet<String>();
        Consumer<Boolean> cancellationCallback = createCancellationCallback(value, callbackResults);
        FutureShell<String> futureShell = new FutureShell<String>();
        CompletableFuture<String> delegate = new CompletableFuture<>();

        futureShell.setCancellationCallback(cancellationCallback);
        futureShell.setDelegate(delegate);
        futureShell.cancel(false);

        assertThat(callbackResults, emptyCollectionOf(String.class));

        delegate.complete(value);
        assertThat(callbackResults, emptyCollectionOf(String.class));
    }

    @Test
    public void testIsDone() throws InterruptedException {
        FutureShell<String> cancelFutureShell = new FutureShell<String>();
        assertFalse("Is done returned true with no delegate", cancelFutureShell.isDone());

        cancelFutureShell.cancel(false);
        assertTrue("isDone returned false after cancelation", cancelFutureShell.isDone());

        FutureShell<String> futureShell = new FutureShell<String>();
        CompletableFuture<String> delegate = new CompletableFuture<>();

        futureShell.setDelegate(delegate);
        assertFalse("Is done returned true when the delegate is not complete", futureShell.isDone());
        delegate.complete("testIsDone");
        assertTrue("Is done returned false when the delegate is complete", futureShell.isDone());

    }

    @Test
    public void testGetWithTimeout() throws InterruptedException, ExecutionException {
        try {
            FutureShell<String> futureShell = new FutureShell<String>();
            futureShell.setDelegate(new CompletableFuture<>());
            futureShell.get(RUNTIME_MS, TimeUnit.MILLISECONDS);
            fail("FutureShell did not throw a timeout exception");
        } catch (TimeoutException e) {
            //Expected expectation, do nothing.
        }
    }

    private Consumer<Boolean> createCancellationCallback(String value, final Set<String> results) {
        Consumer<Boolean> cancellationCallback = (ignoreMe) -> results.add(value);
        return cancellationCallback;
    }

}
