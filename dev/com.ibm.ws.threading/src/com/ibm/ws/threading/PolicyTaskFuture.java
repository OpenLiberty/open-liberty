/*******************************************************************************
 * Copyright (c) 2017,2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.threading;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Various extensions to Future for tasks submitted to policy executors.
 *
 * @param <T> type of the result.
 */
public interface PolicyTaskFuture<T> extends Future<T> {
    // State name constants match the spelling from java.util.concurrent.Future.State

    /**
     * The await operation has timed out.
     */
    public static final int TIMEOUT = -1;

    /**
     * Task has not been submitted yet.
     */
    public static final int PRESUBMIT = 0;

    /**
     * Task was submitted but is not running yet.
     */
    public static final int SUBMITTED = 1;

    /**
     * Task is running.
     */
    public static final int RUNNING = 2;

    /**
     * Task was aborted.
     */
    public static final int ABORTED = 3;

    /**
     * Tasks has begun a cancel request.
     */
    public static final int CANCELLING = 4;

    /**
     * Task has been canceled.
     */
    public static final int CANCELLED = 5;

    /**
     * Task has completed its execution with an error.
     */
    public static final int FAILED = 6;

    /**
     * Task has completed successfully.
     */
    public static final int SUCCESS = 7;

    /**
     * Await completion of the future. Completion could be successful, exceptional, or by cancellation.
     *
     * @return <code>TIMEOUT</code> if timed out. Otherwise a constant indicating the state of the task.
     *         <code>PRESUBMIT</code> and <code>RUNNING</code> indicate an early return from this method
     *         to avoid a hang because the current thread is responsible for running or submitting the task.
     * @throws InterruptedException if interrupted while waiting.
     */
    int await() throws InterruptedException;

    /**
     * Await completion of the future. Completion could be successful, exceptional, or by cancellation.
     *
     * @param time maximum amount of time to await completion.
     * @param unit unit of time.
     * @return <code>TIMEOUT</code> if timed out. Otherwise a constant indicating the state of the task.
     *         <code>PRESUBMIT</code> and <code>RUNNING</code> indicate an early return from this method
     *         to avoid a hang because the current thread is responsible for running or submitting the task.
     * @throws InterruptedException if interrupted while waiting.
     */
    int await(long time, TimeUnit unit) throws InterruptedException;

    /**
     * Computes the estimated interval of time during which the decision is made to accept or reject the task.
     * This interval spans from the creation of the Future up until the point in time when the task is either
     * queued for execution on another thread, accepted for execution on the submitter's thread, or rejected.
     * It includes processing of callbacks such as onSubmit as well as time that elapses while awaiting a queue
     * position. A positive interval can be computed even if the task does not end up being accepted, for example,
     * if the onSubmit callback cancels it. Note that accepts for multiple tasks can overlap, especially if the
     * invokeAll/invokeAny methods are used to submit groups of tasks. The value returned is an estimate.
     * No guarantee is made that the same exact value will be returned for subsequent invocations.
     * The computed value is based on nanosecond timestamps and is therefore not valid for intervals larger than 292 years.
     *
     * @param unit time unit.
     * @return interval of time in the requested unit.
     */
    long getElapsedAcceptTime(TimeUnit unit);

    /**
     * Computes the estimated duration of time that the task spends in the queue awaiting execution.
     * A small positive interval can be returned even if the task bypasses the queue and runs on the submitter's thread.
     * The value returned is an estimate. No guarantee is made that the same exact value will be returned for subsequent invocations.
     * The computed value is based on nanosecond timestamps and is therefore not valid for intervals larger than 292 years.
     *
     * @param unit time unit.
     * @return duration of time in the requested unit.
     */
    long getElapsedQueueTime(TimeUnit unit);

    /**
     * Computes the estimated interval of time during which task execution and processing related to task execution occurs.
     * This includes the processing of the onStart callback. A positive interval can be computed even if the task does
     * end up executing, for example, if the onStart callback cancels it. The value returned is an estimate.
     * No guarantee is made that the same exact value will be returned for subsequent invocations.
     * The computed value is based on nanosecond timestamps and is therefore not valid for intervals larger than 292 years.
     *
     * @param unit time unit.
     * @return interval of time in the requested unit.
     */
    long getElapsedRunTime(TimeUnit unit);
}
