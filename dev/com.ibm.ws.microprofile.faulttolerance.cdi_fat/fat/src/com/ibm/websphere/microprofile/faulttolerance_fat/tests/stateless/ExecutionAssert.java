/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.websphere.microprofile.faulttolerance_fat.tests.stateless;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.ejb.EJBException;

public class ExecutionAssert {

    private static final int COMPLETION_TIMEOUT = 5000;

    /**
     * Like {@link Runnable}, but can throw checked exceptions
     */
    public static interface ThrowingRunnable {
        public void run() throws Exception;
    }

    /**
     * Runs {@code runnable} and asserts that it throws an exception of type {@code expected}.
     *
     * @param <E>      the expected exception type
     * @param expected the expected exception type
     * @param runnable the code to run
     * @return the thrown exception
     */
    public static <E extends Exception> E assertThrows(Class<E> expected, ThrowingRunnable runnable) {
        try {
            runnable.run();
            throw new AssertionError("Exception not thrown");
        } catch (Exception e) {
            assertThat("Thrown exception is not of the correct type", e, instanceOf(expected));
            return expected.cast(e);
        }
    }

    /**
     * Runs {@code runnable} and asserts that it throws an {@link EJBException} whose cause is of type {@code expected}.
     *
     * @param <E>      the expected exception type
     * @param expected the expected exception type
     * @param runnable the code to run
     * @return the cause of the thrown {@code EJBException}
     */
    public static <E extends Exception> E assertThrowsEjbWrapped(Class<E> expected, ThrowingRunnable runnable) {
        EJBException e = assertThrows(EJBException.class, runnable);
        assertThat("Thrown exception is not of the correct type", e.getCause(), instanceOf(expected));
        return expected.cast(e.getCause());
    }

    /**
     * Runs {@code runnable} and asserts that it returns normally (i.e. doesn't throw an exception)
     *
     * @param runnable the code to run
     */
    public static void assertReturns(ThrowingRunnable runnable) {
        try {
            runnable.run();
        } catch (Exception e) {
            throw new AssertionError("Unexpected exception thrown: " + e, e);
        }
    }

    /**
     * Asserts that {@code future} completes exceptionally with an exception of type {@code expected}.
     * <p>
     * Will wait up to {@value #COMPLETION_TIMEOUT} ms for {@code future} to complete.
     *
     * @param <E>      the expected exception type
     * @param expected the expected exception type
     * @param future   the future to wait for
     * @return the exception that {@code future} completed with
     */
    public static <E extends Exception> E assertThrows(Class<E> expected, Future<?> future) {
        try {
            Object result = future.get(COMPLETION_TIMEOUT, TimeUnit.MILLISECONDS);
            throw new AssertionError("Expected exception not thrown. Result: " + result);
        } catch (ExecutionException e) {
            Throwable t = e.getCause();
            assertThat("Thrown exception has wrong type", t, instanceOf(expected));
            return expected.cast(t);
        } catch (InterruptedException e) {
            throw new AssertionError("Interrupted waiting for future", e);
        } catch (TimeoutException e) {
            throw new AssertionError("Timeout waiting for future", e);
        }
    }

    /**
     * Asserts that {@code future} completes exceptionally with an {@link EJBException} whose cause has type {@code expected}.
     * <p>
     * Will wait up to {@value #COMPLETION_TIMEOUT} ms for {@code future} to complete.
     *
     * @param <E>      the expected exception type
     * @param expected the expected exception type
     * @param future   the future to wait for
     * @return the cause of the {@code EJBException}
     */
    public static <E extends Exception> E assertThrowsEjbWrapped(Class<E> expected, Future<?> future) {
        EJBException e = assertThrows(EJBException.class, future);
        assertThat("Wrapped exception has wrong type", e.getCause(), instanceOf(expected));
        return expected.cast(e.getCause());
    }

    /**
     * Asserts that {@code future} completes successfully (i.e. without throwing an exception).
     * <p>
     * Will wait up to {@value #COMPLETION_TIMEOUT} ms for {@code future} to complete.
     *
     * @param <T>    the return type of {@code future}
     * @param future the future to wait for
     * @return the value that {@code future} completed with
     */
    public static <T> T assertCompletes(Future<T> future) {
        try {
            return future.get(COMPLETION_TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (ExecutionException e) {
            throw new AssertionError("Future completed with exception: " + e, e);
        } catch (InterruptedException e) {
            throw new AssertionError("Interrupted waiting for future", e);
        } catch (TimeoutException e) {
            throw new AssertionError("Timeout waiting for future", e);
        }
    }
}
