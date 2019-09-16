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
package com.ibm.ws.microprofile.reactive.streams.test.context;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.hamcrest.Matcher;

/**
 * Class for asserting the result (or failure) of a CompletionStage
 */
public class CompletionStageResult<R> {

    private final CompletableFuture<R> resultFuture;

    public static <R> CompletionStageResult<R> from(CompletionStage<R> stage) {
        CompletableFuture<R> resultFuture = new CompletableFuture<>();
        stage.handle((r, t) -> {
            if (t == null) {
                resultFuture.complete(r);
            } else {
                resultFuture.completeExceptionally(t);
            }
            return null;
        });

        return new CompletionStageResult<>(resultFuture);
    }

    private CompletionStageResult(CompletableFuture<R> resultFuture) {
        this.resultFuture = resultFuture;
    }

    public void assertResult(Matcher<? super R> matcher) {
        try {
            R result = resultFuture.get(10, TimeUnit.SECONDS);
            assertThat(result, matcher);
        } catch (ExecutionException e) {
            throw new AssertionError("Result was exception: " + e, e);
        } catch (InterruptedException e) {
            fail("Interrupted while waiting for result");
        } catch (TimeoutException e) {
            fail("Timed out waiting for result");
        }
    }

    public void assertException(Matcher<? super Throwable> matcher) {
        try {
            R result = resultFuture.get(10, TimeUnit.SECONDS);
            fail("Result was success: " + result);
        } catch (ExecutionException e) {
            assertThat(e.getCause(), matcher);
        } catch (InterruptedException e) {
            fail("Interrupted while waiting for result");
        } catch (TimeoutException e) {
            fail("Timed out waiting for result");
        }
    }

}
