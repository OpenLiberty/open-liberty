/*******************************************************************************
 * Copyright (c) 2020,2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.threading;

import com.ibm.ws.ffdc.FFDCFilter;

/**
 * When tasks implementing this interface are scheduled to the Liberty scheduled executor,
 * it delegates the actual running of the task to the designated policy executor at the
 * appropriate time. This can be useful if you have a PolicyExecutor to which you want
 * scheduled executor tasks to be subject to the constraints of. However, do not use in
 * combination with a policy executor that enables runIfQueueFull, because inline execution
 * of tasks would interfere with the scheduling thread.
 */
public interface ScheduledPolicyExecutorTask {
    /**
     * Returns the policy executor upon which to run this task.
     *
     * @return the policy executor upon which to run this task.
     */
    PolicyExecutor getExecutor();

    /**
     * Provides a callback to be invoked when the task fails to resubmit to
     * the designated policy executor. Typically, this will be because the
     * policy executor has been shut down, suspended, or has reached its limit
     * for maximum queue capacity.
     *
     * @param failure the error that is raised by the resubmit attempt.
     * @return error to report for the failure.
     */
    default Exception resubmitFailed(Exception failure) {
        FFDCFilter.processException(failure, getClass().getName(), "38");
        return failure;
    }
}
