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
 * ExecutionReference reference = asyncBulkheadState.submit(() -> {codeToRun()});
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
     * @param runnable the runnable
     * @param exceptionHandler the exception handler
     * @return an ExecutionReference, allowing the caller to see whether the execution was accepted and to abort later if required
     */
    public ExecutionReference submit(Runnable runnable, ExceptionHandler exceptionHandler);

    /**
     * Shutdown the bulkhead. Any new tasks will be rejected.
     */
    public void shutdown();

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
