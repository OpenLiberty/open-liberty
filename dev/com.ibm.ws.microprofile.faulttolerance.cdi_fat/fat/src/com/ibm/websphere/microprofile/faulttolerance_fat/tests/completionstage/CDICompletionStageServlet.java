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
package com.ibm.websphere.microprofile.faulttolerance_fat.tests.completionstage;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/CDICompletionStage")
public class CDICompletionStageServlet extends FATServlet {

    @Inject
    CDICompletionStageBean bean;

    private ArrayList<CompletableFuture<?>> latches;

    @Override
    protected void before() {
        latches = new ArrayList<>();
    }

    @Override
    protected void after() {
        for (CompletableFuture<?> latch : latches) {
            latch.complete(null);
        }
    }

    @Test
    public void testCompletionStage() throws InterruptedException {
        CompletableFuture<Void> latch = getLatch();
        CompletableFuture<String> returnValue = getLatch();

        returnValue.complete("OK"); // Return an already complete CS
        CompletionStage<String> result = bean.serviceCs(latch, returnValue);

        assertNotCompleting(result); // Should not complete until latch is completed
        latch.complete(null);
        assertResult(result, "OK");
    }

    @Test
    public void testCompletionStageLateCompletion() throws InterruptedException {
        CompletableFuture<Void> latch = getLatch();
        CompletableFuture<String> returnValue = getLatch();

        latch.complete(null); // Allow the method to return immediately
        CompletionStage<String> result = bean.serviceCs(latch, returnValue);

        assertNotCompleting(result); // method should return but result should be complete until returnValue is completed
        returnValue.complete("OK");
        assertResult(result, "OK");
    }

    @Test
    public void testCompletionStageTimeout() throws InterruptedException {
        CompletableFuture<Void> returnValue = getLatch();
        CompletionStage<Void> result = bean.serviceCsTimeout(returnValue);

        // If we don't complete the return value, we expect a timeout exception
        try {
            toCompletableFuture(result).get(2, SECONDS);
            fail("No exception thrown");
        } catch (ExecutionException e) {
            assertThat(e.getCause(), instanceOf(org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException.class));
        } catch (TimeoutException e) {
            fail("Result did not complete");
        }
    }

    @Test
    public void testCompletionStageRetry() throws InterruptedException {
        CompletionStage<Void> result = bean.serviceCsRetryAlwaysFails();
        assertCompleting(result);
        assertEquals("Number of retry attempts", 4, bean.getRetryAlwaysFailsAttemptCount());
    }

    /**
     * Get a CompletableFuture which will automatically be completed at the end of the test
     * <p>
     * Useful to ensure we don't leave anything latched in the case of an unexpected failure
     */
    private <T> CompletableFuture<T> getLatch() {
        CompletableFuture<T> latch = new CompletableFuture<>();
        latches.add(latch);
        return latch;
    }

    private <T> void assertResult(CompletionStage<T> cs, T expected) throws InterruptedException {
        try {
            T result = toCompletableFuture(cs).get(2, SECONDS);
            assertEquals(expected, result);
        } catch (TimeoutException ex) {
            fail("CompletionStage did not complete within 2 seconds");
        } catch (ExecutionException ex) {
            throw new AssertionError("Completion Stage failed with exception", ex);
        }
    }

    private void assertCompleting(CompletionStage<?> cs) throws InterruptedException {
        try {
            toCompletableFuture(cs).get(2, SECONDS);
        } catch (TimeoutException ex) {
            fail("CompletionStage did not complete within 2 seconds");
        } catch (ExecutionException ex) {
            // completion with exception is still a completion...
        }
    }

    private void assertNotCompleting(CompletionStage<?> cs) throws InterruptedException {
        try {
            toCompletableFuture(cs).get(2, SECONDS);
            fail("CompletionStage completed, exepected not to complete");
        } catch (ExecutionException e) {
            fail("CompletionStage completed exceptionally, expected not to complete");
        } catch (TimeoutException e) {
            // Expected
        }
    }

    private static <T> CompletableFuture<T> toCompletableFuture(CompletionStage<T> cs) {
        CompletableFuture<T> cf = new CompletableFuture<>();
        cs.handle((r, t) -> {
            if (t == null) {
                cf.complete(r);
            } else {
                cf.completeExceptionally(t);
            }
            return null;
        });
        return cf;
    }

}
