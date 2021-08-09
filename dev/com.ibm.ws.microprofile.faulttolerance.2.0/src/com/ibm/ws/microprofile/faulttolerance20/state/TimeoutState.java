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
 * Implements the state and logic for a Fault Tolerance Timeout
 * <p>
 * Scope: one method for one retry attempt (or for one invocation if Retry is not used)
 * <p>
 * I.e. a new instance of this class should be created for each retry attempt.
 * <p>
 * Example usage:
 *
 * <pre>
 * <code>
 * TimeoutState timeoutState = Factory.createTimeoutState();
 * Thread currentThread = Thread.currentThread();
 * timeoutState.start(() -> {currentThread.interrupt();});
 *
 * MethodResult result;
 * try {
 *   result = MethodResult.success(codeToRun());
 * } catch (Exception e) {
 *   result = MethodResult.failure(e);
 * } finally {
 *   timeoutState.stop();
 * }
 *
 * if (timeoutState.isTimedOut()) {
 *   Thread.interrupted(); // Clear interrupt flag if set
 *   result = MethodResult.failure(new TimeoutException());
 * }
 * </code>
 * </pre>
 */
public interface TimeoutState {

    /**
     * Start an execution attempt.
     * <p>
     * If {@link #setTimeoutCallback(Runnable)} is called before {@link #stop()} is called, the timeout callback will be called if the execution times out.
     */
    public void start();

    /**
     * Set the callback to call if the execution attempt times out
     * <p>
     * If the execution attempt times out before {@link #stop()} is called, {@code timeoutCallback} will be called.
     * <p>
     * {@code timeoutCallback} will not be called after {@link #stop()} has returned.
     *
     * @param timeoutCallback the callback to call if execution times out
     */
    public void setTimeoutCallback(Runnable timeoutCallback);

    /**
     * Stop an execution attempt
     * <p>
     * Calling this method stops the timeout timer.
     */
    public void stop();

    /**
     * Returns whether the execution has timed out
     * <p>
     * This should only be called after calling {@link #stop()}.
     *
     * @return {@code true} if the execution timed out, {@code false} otherwise
     */
    public boolean isTimedOut();

}