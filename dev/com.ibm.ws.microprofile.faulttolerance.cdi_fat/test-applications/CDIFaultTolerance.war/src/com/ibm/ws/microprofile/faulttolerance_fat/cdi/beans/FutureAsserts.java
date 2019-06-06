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

import static com.ibm.ws.microprofile.faulttolerance_fat.cdi.TestConstants.NEGATIVE_TIMEOUT;
import static com.ibm.ws.microprofile.faulttolerance_fat.cdi.TestConstants.TEST_TIMEOUT;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

/**
 * Assertion methods for checking Futures
 * <p>
 * Note that these are not implemented as hamcrest matchers since these asserts require waiting for the future to return a result and the check method on a hamcrest matcher may be
 * called many times.
 */
public class FutureAsserts {

    private enum ExpectedResult {
        SUCCESS,
        FAILURE,
        INCOMPLETE,
        CANCELLED
    }

    /**
     * Assert that a future completes with a result within TEST_TIMEOUT
     * <p>
     * {@code future.get()} must return a value which is equal to {@code result}
     *
     * @param future the future to check
     * @param result the result to expect
     */
    public static <T> void assertFutureHasResult(Future<T> future, T result) {
        matchFuture(future, ExpectedResult.SUCCESS, TEST_TIMEOUT, result, null);
    }

    /**
     * Assert that a future throws a given exception within TEST_TIMEOUT
     * <p>
     * {@code future.get()} must throw an {@link ExecutionException} which wraps an exception of type {@code throwable}
     *
     * @param future    the future to check
     * @param throwable the exception type to expect
     */
    public static <T> void assertFutureThrowsException(Future<T> future, Class<? extends Throwable> throwable) {
        matchFuture(future, ExpectedResult.FAILURE, TEST_TIMEOUT, null, throwable);
    }

    /**
     * Assert that a future does not complete within NEGATIVE_TIMEOUT
     * <p>
     * {@code future.get(NEGATIVE_TIMEOUT, MILLISECONDS)} must throw a {@link TimeoutException}
     * <p>
     * Note that on a passing test, this method will always take NEGATIVE_TIMEOUT
     *
     * @param future the future to check
     */
    public static <T> void assertFutureDoesNotComplete(Future<T> future) {
        matchFuture(future, ExpectedResult.INCOMPLETE, NEGATIVE_TIMEOUT, null, null);
    }

    /**
     * Assert that a future is cancelled within TEST_TIMEOUT
     * <p>
     * {@code future.get()} must throw a {@link CancellationException}
     *
     * @param future the future to check
     */
    public static <T> void assertFutureGetsCancelled(Future<T> future) {
        matchFuture(future, ExpectedResult.CANCELLED, TEST_TIMEOUT, null, null);
    }

    private static <T> void matchFuture(Future<T> future, ExpectedResult expected, long timeout, T expectedResult, Class<? extends Throwable> exception) {
        try {
            T result = future.get(timeout, MILLISECONDS);
            if (expected != ExpectedResult.SUCCESS) {
                throw new AssertionError("Future returned a successful result: " + result);
            } else if (!isEqual(expectedResult, result)) {
                throw new AssertionError("Result returned by Future was not correct: " + result);
            }
        } catch (ExecutionException e) {
            if (expected != ExpectedResult.FAILURE) {
                throw new AssertionError("Future threw unexpected exception: " + e, e);
            } else if (!exception.isInstance(e.getCause())) {
                throw new AssertionError("Future threw wrong type of exception: " + e, e);
            }
        } catch (TimeoutException e) {
            if (expected != ExpectedResult.INCOMPLETE) {
                throw new AssertionError("Timed out waiting for result from Future");
            }
        } catch (CancellationException e) {
            if (expected != ExpectedResult.CANCELLED) {
                throw new AssertionError("Future was cancelled while waiting for result");
            }
        } catch (InterruptedException e) {
            throw new AssertionError("Interrupted while waiting for result from Future");
        }
    }

    private static <T> boolean isEqual(T a, T b) {
        if (a == null) {
            if (b == null) {
                return true;
            } else {
                return false;
            }
        } else {
            if (a.equals(b)) {
                return true;
            } else {
                return false;
            }
        }
    }
}
