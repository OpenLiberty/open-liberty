/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
