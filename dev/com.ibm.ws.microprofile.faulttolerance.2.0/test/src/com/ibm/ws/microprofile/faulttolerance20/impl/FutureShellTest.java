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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import org.junit.Test;

public class FutureShellTest {

    private static final String TEST_EXCEPTION_MESSAGE_PREFIX = "Test exception with value: ";

    @Test
    public void testFutureShell() throws InterruptedException, ExecutionException, TimeoutException {
        String value = "testFutureShell";
        FutureShell<String> futureShell = new FutureShell<String>();

        Future<String> completableFuture = createFuture(value);
        futureShell.setDelegate(completableFuture);
        assertEquals(value, futureShell.get(1000, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testFailedFutureShell() throws InterruptedException, TimeoutException {
        String value = "testFailedFutureShell";
        FutureShell<String> futureShell = new FutureShell<String>();

        try {
            Future<String> completableFuture = createFailedFuture(value);
            futureShell.setDelegate(completableFuture);
            futureShell.get(1000, TimeUnit.MILLISECONDS);;
            fail("No ExecutionException was thrown");
        } catch (ExecutionException e) {
            assertThat("Exception message was wrong",
                       e.getMessage(), containsString(TEST_EXCEPTION_MESSAGE_PREFIX + String.valueOf(value)));
        }
    }

    @Test
    public void testCancelBeforeSettingDelegate() throws InterruptedException, ExecutionException, TimeoutException {
        String value = "testCancelBeforeSettingDelegate";
        FutureShell<String> futureShell = new FutureShell<String>();
        futureShell.cancel(false);
        boolean canceled = false;

        try {
            Future<String> completableFuture = createFuture(value);
            futureShell.setDelegate(completableFuture);
            futureShell.get(1000, TimeUnit.MILLISECONDS);
            fail("No CancellationException was thrown");
        } catch (CancellationException e) { //expected
            canceled = true;
        }
        assertTrue(canceled);
    }

    @Test
    public void testCancelAfterSettingDelegate() throws InterruptedException, ExecutionException, TimeoutException {
        String value = "testCancelAfterSettingDelegate";
        FutureShell<String> futureShell = new FutureShell<String>();
        boolean canceled = false;

        try {
            Future<String> completableFuture = createFuture(value);
            futureShell.setDelegate(completableFuture);
            futureShell.cancel(true);
            futureShell.get(1000, TimeUnit.MILLISECONDS);
            fail("No CancellationException was thrown");
        } catch (CancellationException e) {
            canceled = true;
        }
        assertTrue(canceled);
    }

    @Test
    public void testGetBeforeSettingDeligate() throws InterruptedException, ExecutionException, TimeoutException {
        String value = "testGetBeforeSettingDeligate";
        FutureShell<String> futureShell = new FutureShell<String>();
        Future<String> delegate = createFuture(value);

        Future<String> result = Executors.newCachedThreadPool().submit(() -> {
            return futureShell.get(1000, TimeUnit.MILLISECONDS);
        });

        Thread.sleep(500);
        futureShell.setDelegate(delegate);
        assertThat("The FutureShell had the wrong result",
                   result.get(1000, TimeUnit.MILLISECONDS), containsString(value));
    }

    @Test
    public void testGetBeforeSettingFailedDeligate() throws InterruptedException, TimeoutException {
        String value = "testGetBeforeSettingFailedDeligate";
        FutureShell<String> futureShell = new FutureShell<String>();
        Future<String> result = null;
        Future<String> delegate = null;
        try {
            delegate = createFailedFuture(value);

            result = Executors.newCachedThreadPool().submit(() -> {
                return futureShell.get(1000, TimeUnit.MILLISECONDS);
            });
            Thread.sleep(500);
            futureShell.setDelegate(delegate);

            result.get(1000, TimeUnit.MILLISECONDS);
            fail("ExecutionException was not thrown");
        } catch (ExecutionException e) {
            assertThat("Exception message was wrong",
                       e.getMessage(), containsString(TEST_EXCEPTION_MESSAGE_PREFIX + String.valueOf(value)));
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

        assertTrue(callbackResults.contains(value));
    }

    @Test
    public void testCancellationCallbackWontFireWithDeligate() throws InterruptedException {
        String value = "testCancellationCallbackWontFireWithDeligate";
        final Set<String> callbackResults = new HashSet<String>();
        Consumer<Boolean> cancellationCallback = createCancellationCallback(value, callbackResults);
        FutureShell<String> futureShell = new FutureShell<String>();

        futureShell.setDelegate(createFuture(value));
        Thread.sleep(10);
        futureShell.setCancellationCallback(cancellationCallback);
        futureShell.cancel(false);

        assertFalse(callbackResults.contains(value));
    }

    @Test
    public void testIsDone() throws InterruptedException {
        FutureShell<String> cancelFutureShell = new FutureShell<String>();
        assertFalse("Is done returned true with no deligate", cancelFutureShell.isDone());

        cancelFutureShell.cancel(false);
        assertTrue("isDone returned false after cancelation", cancelFutureShell.isDone());

        FutureShell<String> futureShell = new FutureShell<String>();

        futureShell.setDelegate(createFuture("testIsDone"));
        assertFalse("Is done returned true when the deligate should still be running", futureShell.isDone());
        Thread.sleep(510);
        assertTrue("Is done returned false when the deligate should be done", futureShell.isDone());

    }

    @Test
    public void testGetWithTimeout() throws InterruptedException, ExecutionException {
        try {
            FutureShell<String> futureShell = new FutureShell<String>();
            futureShell.setDelegate(createFuture("testGetWithTimeout"));
            futureShell.get(100, TimeUnit.MILLISECONDS);
            fail("FutureShell did not throw a timeout exception");
        } catch (TimeoutException e) {
            //Expected expectation, do nothing.
        }
    }

    private Future<String> createFuture(String value) throws InterruptedException {
        CompletableFuture<String> completableFuture = new CompletableFuture<>();

        Executors.newCachedThreadPool().submit(() -> {
            Thread.sleep(500);
            completableFuture.complete(value);
            return null;
        });

        return completableFuture;
    }

    private Future<String> createFailedFuture(String value) throws InterruptedException {
        CompletableFuture<String> completableFuture = new CompletableFuture<>();

        Executors.newCachedThreadPool().submit(() -> {
            Thread.sleep(500);
            completableFuture.completeExceptionally(new IllegalStateException(TEST_EXCEPTION_MESSAGE_PREFIX + value));
            return null;
        });

        return completableFuture;
    }

    private Consumer<Boolean> createCancellationCallback(String value, final Set<String> results) {
        Consumer<Boolean> cancellationCallback = (ignoreMe) -> results.add(value);
        return cancellationCallback;
    }

}
