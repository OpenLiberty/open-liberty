/*******************************************************************************
 * Copyright (c) 2012, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.threading.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import junit.framework.Assert;
import junit.framework.AssertionFailedError;

import org.junit.Test;

import com.ibm.ws.threading.FutureMonitor;
import com.ibm.ws.threading.listeners.CompletionListener;

/**
 *
 */
public class FutureMonitorImplTest {

    private final FutureMonitor monitor = new FutureMonitorImpl();

    private Boolean testResult;
    private Throwable testFailure;

    @Test
    public void testEagerCompletionSuccess() {
        Future<Boolean> b = monitor.createFuture(Boolean.class);

        monitor.setResult(b, true);

        monitor.onCompletion(b, new CompletionListener<Boolean>() {

            @Override
            public void successfulCompletion(Future<Boolean> future, Boolean result) {
                testResult = result;
            }

            @Override
            public void failedCompletion(Future<Boolean> future, Throwable t) {
                Error e = new AssertionFailedError("An unexpected exception was found");
                e.initCause(t);
                throw e;
            }
        });

        assertNotNull("The Future completion listener was not notified when the future was complete", testResult);
        assertTrue("The test result was found, but did not have the expected value", testResult);
    }

    @Test
    public void testEagerCompletionFailed() {
        Future<Boolean> b = monitor.createFuture(Boolean.class);

        NullPointerException expectedFailure = new NullPointerException();

        monitor.setResult(b, expectedFailure);

        monitor.onCompletion(b, new CompletionListener<Boolean>() {

            @Override
            public void successfulCompletion(Future<Boolean> future, Boolean result) {
                Assert.fail("Unexpected claim that the notification worked: " + result);
            }

            @Override
            public void failedCompletion(Future<Boolean> future, Throwable t) {
                testFailure = t;
            }
        });

        assertEquals("The future did not fail with the expected exception", expectedFailure, testFailure);
    }

    @Test
    public void testImmediateCompletionSuccess() {
        Future<Boolean> b = monitor.createFutureWithResult(Boolean.TRUE);

        monitor.onCompletion(b, new CompletionListener<Boolean>() {

            @Override
            public void successfulCompletion(Future<Boolean> future, Boolean result) {
                testResult = result;
            }

            @Override
            public void failedCompletion(Future<Boolean> future, Throwable t) {
                Error e = new AssertionFailedError("An unexpected exception was found");
                e.initCause(t);
                throw e;
            }
        });

        assertNotNull("The Future completion listener was not notified when the future was complete", testResult);
        assertTrue("The test result was found, but did not have the expected value", testResult);
    }

    @Test
    public void testImmediateCompletionFailed() {
        NullPointerException expectedFailure = new NullPointerException();

        Future<Boolean> b = monitor.createFutureWithResult(Boolean.class, expectedFailure);

        monitor.onCompletion(b, new CompletionListener<Boolean>() {

            @Override
            public void successfulCompletion(Future<Boolean> future, Boolean result) {
                Assert.fail("Unexpected claim that the notification worked: " + result);
            }

            @Override
            public void failedCompletion(Future<Boolean> future, Throwable t) {
                testFailure = t;
            }
        });

        assertEquals("The future did not fail with the expected exception", expectedFailure, testFailure);
    }

    @Test
    public void testDelayedCompletionSuccess() {
        Future<Boolean> b = monitor.createFuture(Boolean.class);

        monitor.onCompletion(b, new CompletionListener<Boolean>() {

            @Override
            public void successfulCompletion(Future<Boolean> future, Boolean result) {
                testResult = result;
            }

            @Override
            public void failedCompletion(Future<Boolean> future, Throwable t) {
                Error e = new AssertionFailedError("An unexpected exception was found");
                e.initCause(t);
                throw e;
            }
        });

        monitor.setResult(b, true);

        assertNotNull("The Future completion listener was not notified when the future was complete", testResult);
        assertTrue("The test result was found, but did not have the expected value", testResult);

    }

    @Test
    public void testDelayedCompletionFailed() {
        Future<Boolean> b = monitor.createFuture(Boolean.class);

        monitor.onCompletion(b, new CompletionListener<Boolean>() {

            @Override
            public void successfulCompletion(Future<Boolean> future, Boolean result) {
                Assert.fail("Unexpected claim that the notification worked: " + result);
            }

            @Override
            public void failedCompletion(Future<Boolean> future, Throwable t) {
                testFailure = t;
            }
        });

        NullPointerException expectedFailure = new NullPointerException();

        monitor.setResult(b, expectedFailure);

        assertEquals("The future did not fail with the expected exception", expectedFailure, testFailure);

    }

    @Test
    public void testRedundantSuccessAndSuccess() throws Exception {
        Future<Boolean> b = monitor.createFuture(Boolean.class);
        monitor.setResult(b, true);
        monitor.setResult(b, false);
        assertTrue("The future should keep the initial result", b.get());
    }

    @Test
    public void testRedundantSuccessAndFailure() throws Exception {
        Future<Boolean> b = monitor.createFuture(Boolean.class);
        monitor.setResult(b, true);
        monitor.setResult(b, new NullPointerException());
        assertTrue("The future should keep the initial result", b.get());
    }

    @Test(expected = ExecutionException.class)
    public void testRedundantFailureAndSuccess() throws Exception {
        Future<Boolean> b = monitor.createFuture(Boolean.class);
        monitor.setResult(b, new NullPointerException());
        monitor.setResult(b, false);
        b.get();
    }

    @Test
    public void testRedundantFailureAndFailure() throws Exception {
        Future<Boolean> b = monitor.createFuture(Boolean.class);
        Exception expectedException = new NullPointerException();
        monitor.setResult(b, expectedException);
        monitor.setResult(b, new IllegalStateException());
        try {
            b.get();
            fail("The future should keep the initial exception");
        } catch (ExecutionException e) {
            assertEquals("The future should keep the initial exception", expectedException, e.getCause());
        }
    }

    @Test
    public void testImmediateRedundantSuccessAndSuccess() throws Exception {
        Future<Boolean> b = monitor.createFutureWithResult(true);
        monitor.setResult(b, false);
        assertTrue("The future should keep the immediate result", b.get());
    }

    @Test
    public void testImmediateRedundantSuccessAndFailure() throws Exception {
        Future<Boolean> b = monitor.createFutureWithResult(true);
        monitor.setResult(b, new NullPointerException());
        assertTrue("The future should keep the immediate result", b.get());
    }

    @Test(expected = ExecutionException.class)
    public void testImmediateRedundantFailureAndSuccess() throws Exception {
        Future<Boolean> b = monitor.createFutureWithResult(Boolean.class, new NullPointerException());
        monitor.setResult(b, false);
        assertTrue("The future should keep the immediate exception", b.get());
    }

    @Test
    public void testImmediateRedundantFailureAndFailure() throws Exception {
        Exception expectedException = new NullPointerException();
        Future<Boolean> b = monitor.createFutureWithResult(Boolean.class, expectedException);
        monitor.setResult(b, new IllegalStateException());
        try {
            b.get();
            fail("The future should keep the immediate exception");
        } catch (ExecutionException e) {
            assertEquals("The future should keep the immediate exception", expectedException, e.getCause());
        }
    }

}
