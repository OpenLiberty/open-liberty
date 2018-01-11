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
     * Allows for replacing the policy executor to which the task was submitted with another when the invokeAll/invokeAny methods are used to submit multiple tasks.
     *
     * @param executor policy executor to which the task was submitted.
     * @return the default implementation returns the policy executor to which the task was submitted, such that invokeAll/invokeAny
     *         always run tasks according the policy executor upon which the invokeAll/invokeAny operation was invoked.
     */
    public PolicyExecutor getExecutor(PolicyExecutor executor) {
        return executor;
    }

    /**
     * Allows for replacing the identifier that is used in exception messages and log messages about the policy executor.
     *
     * @param policyExecutorIdentifier unique identifier for the policy executor. Some examples:
     *            PolicyExecutorProvider-MPFaultTolerance12345
     *            concurrencyPolicy[longRunningPolicy]
     *            managedExecutorService[executor1]/longRunningPolicy[default-0]
     * @return the default implementation returns the policy executor's identifier, as supplied in the parameter to this method.
     */
    public String getIdentifier(String policyExecutorIdentifier) {
        return policyExecutorIdentifier;
    }

    /**
     * Returns the name of the task, which, for example, might be reported in exception messages or messages that are logged.
     *
     * @param task the Callable or Runnable task.
     * @return the default implementation invokes toString on the task object.
     */
    public String getName(Object task) {
        return task.toString();
    }

    /**
     * Allows for an override of the default start timeout, which is provided as a parameter.
     *
     * @param defaultStartTimeoutNS the default start timeout (in nanoseconds) that is configured on the policy executor. -1 indicates no timeout.
     * @return the default implementation returns the default start timeout in nanoseconds.
     */
    public long getStartTimeout(long defaultStartTimeoutNS) {
        return defaultStartTimeoutNS;
    }

    /**
     * Invoked when a task's future is canceled.
     * This callback is invoked synchronously on the thread that cancels the task.
     *
     * @param task the Callable or Runnable task.
     * @param future the future for the task that was canceled.
     * @param whileRunning indicates if the task was canceled while running (as opposed to while still queued for execution or just before it started).
     */
    public void onCancel(Object task, PolicyTaskFuture<?> future, boolean whileRunning) {}

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

    /**
     * Allows for raising a more meaningful exception when the Future.get is attempted from the same
     * thread that is preparing to submit or start the task. This is essentially a deadlock where the
     * task will never be able to start. The deadlock must be broken by raising an exception.
     * The default implementation does nothing, in response to which the policy executor raises a generic
     * InterruptedException without any message or cause exception.
     *
     * @throws InterruptedException if the callback provider wishes to replace the generic InterruptedException that is raised for this condition.
     */
    public void resolveDeadlockOnFutureGet() throws InterruptedException {}
}
