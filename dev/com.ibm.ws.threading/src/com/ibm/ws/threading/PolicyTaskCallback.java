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

import java.util.concurrent.ExecutionException;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 * Receives notifications at various points in the life cycle of policy executor tasks.
 */
@Trivial
public abstract class PolicyTaskCallback {
    /**
     * Invoked when a task's future is canceled.
     * This callback is invoked synchronously on the thread that cancels the task.
     *
     * @param task the Callable or Runnable task.
     * @param future the future for the task that was canceled.
     * @param timedOut indicates if the start timeout elapsed and caused cancellation.
     * @param whileRunning indicates if the task was canceled while running (as opposed to while still queued for execution or just before it started).
     */
    public void onCancel(Object task, PolicyTaskFuture<?> future, boolean timedOut, boolean whileRunning) {}

    /**
     * Invoked on the thread of execution of a task after it completes, which could be successfully, exceptionally, or due to cancellation/interrupt.
     * This callback is invoked synchronously on the task's thread of execution.
     *
     * @param task the Callable or Runnable task.
     * @param future the future for the task that has stopped running.
     * @param startObj result, if any, of previous onStart callback.
     * @param aborted indicates if the task was aborted and did not start.
     * @param pending a positive value indicates that additional work, such as completion services are pending on the task's thread of execution.
     *            The value 0 indicate that no additional work is pending. A negative value indicates that work that was previously pending is done.
     *            If positive, a subsequent onEnd callback will be sent with a negative value after the additional work ends.
     * @param failure failure, if any, that occurred while trying to run the task.
     */
    public void onEnd(Object task, PolicyTaskFuture<?> future, Object startObj, boolean aborted, int pending, Throwable failure) {}

    /**
     * Invoked before a task starts running. This callback is invoked synchronously on the task's thread of execution.
     * It is permissible to cancel the future at this point, which will prevent the task from starting.
     *
     * @param task the Callable or Runnable task.
     * @param future the future for the task that is about to start.
     * @return optional object that will be supplied to onEnd.
     */
    public Object onStart(Object task, PolicyTaskFuture<?> future) {
        return null;
    }

    /**
     * Invoked when a task is submitted to the policy executor. This is different than successful enqueue.
     * This callback is invoked synchronously on the thread that submitted the task to the executor.
     *
     * @param task the Callable or Runnable task.
     * @param future the future for the submitted task.
     * @param invokeAnyCount this value is always 0 unless invokeAny is used,
     *            in which case it is the size of the collection submitted to invokeAny excluding any that were canceled upon submit.
     */
    public void onSubmit(Object task, PolicyTaskFuture<?> future, int invokeAnyCount) {}

    /**
     * Invoked to raise an exception for an aborted task.
     * For example, a task is aborted when the onStart callback raises a runtime exception
     * or when the executor is interrupted while waiting to enqueue a task.
     * This method gives the callback the opportunity to use a different exception,
     * such as javax.enteprise.concurrent.AbortedException to distinguish aborted tasks
     * from tasks that failed during normal execution or were rejected for other reasons.
     * The default implementation of this method does nothing, in which case the policy executor raises RejectedExecutionException.
     *
     * @param x the exception
     * @throws ExecutionException
     */
    public void raiseAbortedException(Throwable x) throws ExecutionException {}
}
