/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.faulttolerance_fat.cdi.beans;

import static com.ibm.ws.microprofile.faulttolerance_fat.cdi.TestConstants.TEST_TIMEOUT;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.junit.Assert.assertFalse;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import com.ibm.ws.microprofile.faulttolerance_fat.cdi.TestConstants;

public class SyntheticTask<V> implements Callable<V> {

    private final AtomicInteger count = new AtomicInteger(0);
    private final CompletableFuture<Void> startLatch = new CompletableFuture<>();
    private final CompletableFuture<Void> waitLatch = new CompletableFuture<>();
    private AssertionError error = null;
    private InterruptionAction onInterrupt = InterruptionAction.FAIL;
    private V result;

    public enum InterruptionAction {
        /** Fail the test if task is interrupted */
        FAIL,
        /** Return without waiting for complete() to be called, fail the test if not interrupted */
        RETURN,
        /** Ignore the interruption and continue waiting for complete() to be called, fail the test if not interrupted */
        IGNORE
    }

    /** {@inheritDoc} */
    @Override
    public V call() {
        startLatch.complete(null);
        count.incrementAndGet();
        boolean keepRunning = true;
        while (keepRunning) {
            keepRunning = false; // Default behaviour is to run once, unless we decide to run again
            try {
                waitLatch.get(TEST_TIMEOUT, MILLISECONDS);
                if (onInterrupt != InterruptionAction.FAIL) {
                    error = new AssertionError("Expected interruption but none occurred");
                }
            } catch (TimeoutException e) {
                error = new AssertionError("Unexpected timeout waiting for waitLatch", e);
            } catch (InterruptedException e) {
                if (onInterrupt == InterruptionAction.FAIL) {
                    error = new AssertionError("Unexpected interruption waiting for waitLatch", e);
                } else if (onInterrupt == InterruptionAction.IGNORE) {
                    keepRunning = true;
                }
            } catch (ExecutionException e) {
                error = new AssertionError("Unexpected exception waiting for waitLatch", e);
            }
        }
        return result;
    }

    public SyntheticTask<V> onInterruption(InterruptionAction action) {
        onInterrupt = action;
        return this;
    }

    public SyntheticTask<V> withResult(V result) {
        this.result = result;
        return this;
    }

    public int getCount() {
        return count.get();
    }

    public void checkErrors() {
        if (error != null) {
            throw error;
        }
    }

    public void complete() {
        waitLatch.complete(null);
    }

    public void assertStarts() {
        try {
            startLatch.get(TEST_TIMEOUT, MILLISECONDS);
        } catch (InterruptedException e) {
            throw new AssertionError("Unexpected interruption waiting for task to start", e);
        } catch (ExecutionException e) {
            throw new AssertionError("Unexpected exception waiting for task to start", e);
        } catch (TimeoutException e) {
            throw new AssertionError("Timed out waiting for task to start", e);
        }
    }

    public void assertNotStarting() {
        try {
            startLatch.get(TestConstants.NEGATIVE_TIMEOUT, MILLISECONDS);
            throw new AssertionError("Task started unexpectedly");
        } catch (InterruptedException e) {
            throw new AssertionError("Unexpected interruption waiting for task to start", e);
        } catch (ExecutionException e) {
            throw new AssertionError("Unexpected exception waiting for task to start", e);
        } catch (TimeoutException e) {
            // Expected
        }
    }

    public static void assertAllNotStarting(SyntheticTask<?>... tasks) {
        try {
            Thread.sleep(TestConstants.NEGATIVE_TIMEOUT);
        } catch (InterruptedException e) {
            throw new AssertionError("Unexpected interruption waiting for task to start", e);
        }
        for (SyntheticTask<?> task : tasks) {
            assertFalse("Task has started", task.startLatch.isDone());
        }
    }
}
