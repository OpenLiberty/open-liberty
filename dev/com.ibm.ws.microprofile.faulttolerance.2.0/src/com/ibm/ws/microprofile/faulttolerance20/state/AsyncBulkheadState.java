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
package com.ibm.ws.microprofile.faulttolerance20.state;

/**
 * Implements the state and logic for a Fault Tolerance Asynchronous Bulkhead
 * <p>
 * Scope: one method for the lifetime of the application
 * <p>
 * Example usage:
 *
 * <pre>
 * <code>
 * ExecutionReference reference = asyncBulkheadState.submit((reservation) -> {
 *   try {
 *     codeToRun();
 *   } finally {
 *     reservation.release();
 *   }
 * });
 *
 * if (!reference.wasAccepted) {
 *   throw new BulkheadException();
 * }
 * </code>
 * </pre>
 */
public interface AsyncBulkheadState {

    /**
     * Submit a runnable to be run asynchronously on the bulkhead
     * <p>
     * The caller should check the value of {@link ExecutionReference#wasAccepted()} on the returned object to see if the bulkhead accepted the execution
     * <p>
     * The exception handler will be called if an exception occurs handling the runnable after the submit method has returned.
     *
     * @param task             the task to run
     * @param exceptionHandler the exception handler
     * @return an ExecutionReference, allowing the caller to see whether the execution was accepted and to abort later if required
     */
    public ExecutionReference submit(AsyncBulkheadTask task, ExceptionHandler exceptionHandler);

    /**
     * A task that can be run by an async bulkhead
     * <p>
     * This is basically a Runnable except the run method takes a {@link BulkheadReservation} parameter
     */
    @FunctionalInterface
    public interface AsyncBulkheadTask {

        /**
         * Run the task
         * <p>
         * All implementations of this method must ensure that {@code reservation.release()} is always called (whether execution of the task is successful or fails).
         * Failure to do this will result in bulkhead permits being leaked.
         *
         * @param bulkheadReservation a callback to indicate that execution is complete and the bulkhead reservation should be released
         */
        public void run(BulkheadReservation reservation);

    }

    /**
     * A reference to an execution submitted to the bulkhead
     * <p>
     * Allows the caller to abort the execution and to check whether it was accepted by the bulkhead
     */
    public interface ExecutionReference {
        /**
         * Aborts the execution. This is equivalent to calling {@code Future.cancel(mayInterrupt)} on a task submitted to an executor.
         *
         * @param mayInterrupt whether the task should be interrupted if it is running
         */
        public void abort(boolean mayInterrupt);

        /**
         * Returns whether the runnable was accepted by the bulkhead
         *
         * @return {@code true} if the runnable was accepted, {@code false} otherwise
         */
        public boolean wasAccepted();
    }

    /**
     * A reference to the reservation on the bulkhead
     * <p>
     * Allows the task to release it's reservation on the bulkhead when its work is done, allowing another task to run
     */
    public interface BulkheadReservation {
        /**
         * Report that the execution is complete, releasing the bulkhead permit
         */
        public void release();
    }

    /**
     * A callback to handle exceptions thrown while processing the runnable
     */
    @FunctionalInterface
    public interface ExceptionHandler {

        /**
         * Handle an exception
         *
         * @param t the exception to handle
         */
        public void handle(Throwable t);

    }
}
