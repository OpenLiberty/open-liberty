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

import org.eclipse.microprofile.faulttolerance.exceptions.BulkheadException;
import org.junit.Test;

import componenttest.app.FATServlet;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

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
    @Mode(TestMode.FULL)
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
        assertException(result, org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException.class);
    }

    @Test
    @Mode(TestMode.FULL)
    public void testCompletionStageBulkhead() throws InterruptedException {
        // First call will wait on latch until we complete it
        CompletableFuture<Void> returnValue1 = getLatch();
        CompletionStage<Void> result1 = bean.serviceCsBulkhead(returnValue1);

        // Second call does not wait on latch
        CompletionStage<Void> result2 = bean.serviceCsBulkhead(CompletableFuture.completedFuture(null));

        // First method call should return but result should not be complete because the returned CS is not complete
        assertNotCompleting(result1);
        assertEquals("Concurrent calls to serviceCsBulkhead", 0, bean.getConcurrentServiceCsBulkhead());

        // Second method should not be allowed to run yet as bulkhead permit is held until CS from first call completes
        assertNotCompleting(result2);

        // Third execution should fail with BulkheadException
        CompletionStage<Void> result3 = bean.serviceCsBulkhead(CompletableFuture.completedFuture(null));
        assertException(result3, BulkheadException.class);

        // If we allow the first call to complete, both the first and second calls should run and complete
        returnValue1.complete(null);
        assertResult(result1, null);
        assertResult(result2, null);
    }

    @Test
    @Mode(TestMode.FULL)
    public void testCompletionStageBulkheadTimeout() throws InterruptedException {
        // First call will wait on latch until we complete it
        CompletableFuture<Void> returnValue1 = getLatch();
        CompletionStage<Void> result1 = bean.serviceCsBulkheadTimeout(returnValue1);

        // Call will time out after 500ms
        assertException(result1, org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException.class);

        // Second call will not wait on latch
        CompletionStage<Void> result2 = bean.serviceCsBulkheadTimeout(CompletableFuture.completedFuture(null));

        // However, second call is not allowed to run as first call still holds the bulkhead, resulting in TimeoutException after 500ms
        assertException(result2, org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException.class);
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
            T result = toCompletableFuture(cs).get(10, SECONDS);
            assertEquals(expected, result);
        } catch (TimeoutException ex) {
            fail("CompletionStage did not complete within 2 seconds");
        } catch (ExecutionException ex) {
            throw new AssertionError("Completion Stage failed with exception", ex);
        }
    }

    private void assertException(CompletionStage<?> cs, Class<? extends Throwable> exceptionClazz) throws InterruptedException {
        try {
            toCompletableFuture(cs).get(10, SECONDS);
            fail("Completion stage completed successfully");
        } catch (ExecutionException ex) {
            assertThat(ex.getCause(), instanceOf(exceptionClazz));
        } catch (TimeoutException e) {
            fail("CompletionStage did not complete within 2 seconds");
        }
    }

    private void assertCompleting(CompletionStage<?> cs) throws InterruptedException {
        try {
            toCompletableFuture(cs).get(10, SECONDS);
        } catch (TimeoutException ex) {
            fail("CompletionStage did not complete within 2 seconds");
        } catch (ExecutionException ex) {
            // completion with exception is still a completion...
        }
    }

    private void assertNotCompleting(CompletionStage<?> cs) throws InterruptedException {
        try {
            toCompletableFuture(cs).get(2, SECONDS); // Shorter timeout since we expect Timeout to occur
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
