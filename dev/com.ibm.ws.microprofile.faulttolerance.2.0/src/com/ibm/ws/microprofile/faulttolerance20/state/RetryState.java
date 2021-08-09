/*******************************************************************************
 * Copyright (c) 2018, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.faulttolerance20.state;

import java.util.concurrent.TimeUnit;

import com.ibm.ws.microprofile.faulttolerance.spi.RetryResultCategory;
import com.ibm.ws.microprofile.faulttolerance20.impl.MethodResult;

/**
 * Implements the state and logic for a Fault Tolerance Retry
 * <p>
 * Scope: one method for one invocation
 * <p>
 * I.e. a new instance of this class should be created for each invocation
 * <p>
 * Example usage:
 *
 * <pre>
 * <code>
 * RetryState retryState = Factory.createRetryState();
 * retryState.start();
 *
 * boolean shouldRetry = true;
 * while (shouldRetry) {
 *   MethodResult<?> result = new MethodResult();
 *   try {
 *     result = MethodResult.success(codeToRun());
 *   } catch (Exception e) {
 *     result = MethodResult.failure(e);
 *   }
 *
 *   RetryResult retryResult = retryState.recordResult(result);
 *
 *   shouldRetry = retryResult.shouldRetry();
 *   if (shouldRetry) {
 *     long waitMillis = retryResult.getDelayUnit().toMillis(retryResult.getDelay());
 *     Thread.sleep(waitMillis);
 *   }
 * }
 * </code>
 * </pre>
 */
public interface RetryState {

    /**
     * Record the start of the execution
     * <p>
     * This may be used to start a timer to limit the maximum execution time
     */
    public void start();

    /**
     * Record an execution result
     * <p>
     * Returns a RetryResult which instructs the calling code whether it should retry and how long it should wait before doing so.
     *
     * @param result the execution result
     * @return the RetryResult
     */
    public RetryResult recordResult(MethodResult<?> result);

    /**
     * Message to a caller of RetryState, instructing it whether an execution attempt should be retried or not.
     */
    public interface RetryResult {

        /**
         * @return {@code true} if the execution should be retried, {@code false} if it should not be retried
         */
        public boolean shouldRetry();

        /**
         * @return the time to wait before retrying, in the time unit given by {@link #getDelayUnit()}
         */
        public long getDelay();

        /**
         * @return the TimeUnit used to express the result of {@link #getDelay()}
         */
        public TimeUnit getDelayUnit();

        /**
         * @return the RetryResultCategory for the retry attempt
         */
        public RetryResultCategory getCategory();

        /**
         * @return a string describing the result, for debugging and tracing purposes
         */
        @Override
        public String toString();
    }

}
