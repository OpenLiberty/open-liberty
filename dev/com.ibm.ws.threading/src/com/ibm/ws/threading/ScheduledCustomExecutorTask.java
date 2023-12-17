/*******************************************************************************
 * Copyright (c) 2020,2023 IBM Corporation and others.
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

import java.util.concurrent.Executor;

import com.ibm.ws.ffdc.FFDCFilter;

/**
 * When tasks implementing this interface are scheduled to the Liberty scheduled executor,
 * it delegates the actual running of the task to the designated executor.
 */
public interface ScheduledCustomExecutorTask {
    /**
     * Returns a custom executor upon which to run this task.
     * You can supply an executor that runs tasks on virtual threads.
     * Or you can supply a PolicyExecutor such that tasks will be subject its constraints,
     * but do not supply a PolicyExecutor that enables runIfQueueFull, because inline execution
     * of tasks would interfere with the scheduling thread.
     * The default value of null indicates to run the task directly on the Liberty thread pool.
     *
     * @return the executor upon which to run this task.
     */
    default Executor getExecutor() {
        return null;
    }

    /**
     * Computes the next fixed-rate execution time after the specified execution time,
     * given the specified period.
     *
     * @param expectedExecutionTime nanosecond timestamp at which the task was expected to start executing.
     *                                  If delayed, the current time will be later than this expected target execution time.
     * @param period                period in nanoseconds at which the fixed-rate task should execute.
     * @return nanosecond timestamp of the next fixed-rate execution.
     */
    default long getNextFixedRateExecutionTime(long expectedExecutionTime, long period) {
        return expectedExecutionTime + period;
    }

    /**
     * Provides a callback to be invoked when the task fails to resubmit to
     * the designated executor. Typically, this will be because a PolicyExecutor
     * is used and has been shut down, suspended, or has reached its limit
     * for maximum queue capacity.
     *
     * @param failure the error that is raised by the resubmit attempt.
     * @return error to report for the failure.
     */
    default Exception resubmitFailed(Exception failure) {
        FFDCFilter.processException(failure, getClass().getName(), "59");
        return failure;
    }
}
