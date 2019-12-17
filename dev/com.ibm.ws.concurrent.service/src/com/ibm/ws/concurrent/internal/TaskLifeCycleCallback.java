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
package com.ibm.ws.concurrent.internal;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RejectedExecutionException;

import javax.enterprise.concurrent.AbortedException;
import javax.enterprise.concurrent.ManagedTask;
import javax.enterprise.concurrent.ManagedTaskListener;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.threading.PolicyExecutor;
import com.ibm.ws.threading.PolicyTaskCallback;
import com.ibm.ws.threading.PolicyTaskFuture;
import com.ibm.wsspi.threadcontext.ThreadContext;
import com.ibm.wsspi.threadcontext.ThreadContextDescriptor;

/**
 * Callback that uses the notifications it receives about about task life cycle
 * to apply and remove thread context and send events to a ManagedTaskListener.
 */
public class TaskLifeCycleCallback extends PolicyTaskCallback {
    private static final TraceComponent tc = Tr.register(TaskLifeCycleCallback.class);

    /**
     * Managed executor to which the task was submitted.
     */
    private final ManagedExecutorServiceImpl managedExecutor;

    /**
     * Policy executor that will run the task.
     */
    final PolicyExecutor policyExecutor;

    /**
     * Represents thread context captured from the submitting thread.
     */
    private final ThreadContextDescriptor threadContextDescriptor;

    /**
     * Construct a new task life cycle callback. A single instance can be used for multiple tasks.
     *
     * @param managedExecutor the managed executor to which the task was submitted.
     * @param threadContextDescriptor represents thread context captured from the submitting thread.
     */
    TaskLifeCycleCallback(ManagedExecutorServiceImpl managedExecutor, ThreadContextDescriptor threadContextDescriptor) {
        this.managedExecutor = managedExecutor;
        this.threadContextDescriptor = threadContextDescriptor;

        Map<String, String> execProps = threadContextDescriptor.getExecutionProperties();
        PolicyExecutor executor = Boolean.parseBoolean(execProps.get(ManagedTask.LONGRUNNING_HINT)) ? managedExecutor.longRunningPolicyExecutorRef.get() : null;
        this.policyExecutor = executor == null ? managedExecutor.policyExecutor : executor;
    }

    /**
     * Decide which policy executor will run the task.
     * Use the policy executor for the long running concurrency policy if specified and the LONGRUNNING_HINT is true.
     * Otherwise, use the policy executor the general concurrency policy.
     *
     * @param executor ignored by this implementation.
     */
    @Override
    public final PolicyExecutor getExecutor(PolicyExecutor executor) {
        return policyExecutor;
    }

    /**
     * Allows for replacing the identifier that is used in exception messages and log messages about the policy executor.
     *
     * @param policyExecutorIdentifier unique identifier for the policy executor. Some examples:
     *            concurrencyPolicy[longRunningPolicy]
     *            managedExecutorService[executor1]/longRunningPolicy[default-0]
     * @return identifier to use in messages.
     */
    @Override
    @Trivial
    public String getIdentifier(String identifier) {
        return managedExecutor.getIdentifier(identifier);
    }

    /**
     * Returns the task name.
     *
     * @param task the task.
     * @return the task name.
     */
    @Override
    @Trivial
    public final String getName(Object task) {
        Map<String, String> execProps = threadContextDescriptor.getExecutionProperties();
        String taskName = execProps == null ? null : execProps.get(ManagedTask.IDENTITY_NAME);
        return taskName == null ? task.toString() : taskName;
    }

    @Override
    @Trivial
    public final long getStartTimeout(long defaultStartTimeoutNS) {
        Map<String, String> execProps = threadContextDescriptor.getExecutionProperties();
        String value = execProps == null ? null : execProps.get("com.ibm.ws.concurrent.START_TIMEOUT_NANOS");
        try {
            long ns = value == null ? defaultStartTimeoutNS : Long.parseLong(value);
            if (ns < -1)
                throw new IllegalArgumentException("com.ibm.ws.concurrent.START_TIMEOUT_NANOS: " + value);
            return ns;
        } catch (NumberFormatException x) {
            throw new IllegalArgumentException("com.ibm.ws.concurrent.START_TIMEOUT_NANOS: " + value);
        }
    }

    @FFDCIgnore({ Error.class, RuntimeException.class }) // No need for FFDC, error is logged instead
    @Override
    public void onCancel(Object task, PolicyTaskFuture<?> future, boolean whileRunning) {
        // Tasks that are canceled while running have the taskAborted notification sent on the thread of execution instead.

        // notify listener: taskAborted (if task was canceled before it started)
        if (!whileRunning && task instanceof ManagedTask) {
            ManagedTaskListener listener = ((ManagedTask) task).getManagedTaskListener();
            if (listener != null) {
                Throwable failure = null;
                ThreadContext tranContextRestorer = managedExecutor.suspendTransaction();
                try {
                    CancellationException x = new CancellationException(Tr.formatMessage(tc, "CWWKC1110.task.canceled", getName(task), managedExecutor.name));

                    if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                        Tr.event(this, tc, "taskAborted", managedExecutor, task, x);
                    listener.taskAborted(future, managedExecutor, task, x);
                } catch (Error x) {
                    Tr.error(tc, "CWWKC1102.listener.failed", getName(task), managedExecutor.name, x);
                    failure = x;
                    throw x;
                } catch (RuntimeException x) {
                    Tr.error(tc, "CWWKC1102.listener.failed", getName(task), managedExecutor.name, x);
                    failure = x;
                    throw x;
                } finally {
                    try {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                            Tr.event(this, tc, "taskDone", managedExecutor, task, failure);
                        listener.taskDone(future, managedExecutor, task, failure);
                    } catch (Error x) {
                        Tr.error(tc, "CWWKC1102.listener.failed", getName(task), managedExecutor.name, x);
                        throw x;
                    } catch (RuntimeException x) {
                        Tr.error(tc, "CWWKC1102.listener.failed", getName(task), managedExecutor.name, x);
                        throw x;
                    } finally {
                        if (tranContextRestorer != null)
                            tranContextRestorer.taskStopping();
                    }
                }
            }
        }
    }

    @FFDCIgnore(Throwable.class) // No need for FFDC given that an error is already logged
    @Override
    public void onEnd(Object task, PolicyTaskFuture<?> future, Object startObj, boolean aborted, int pending, Throwable failure) {
        if (pending >= 0 && task instanceof ManagedTask) {
            ManagedTaskListener listener = ((ManagedTask) task).getManagedTaskListener();
            if (listener != null) {
                // notify listener: taskAborted
                try {
                    Throwable x;
                    boolean canceled = future.isCancelled();
                    if (canceled || aborted) {
                        if (failure instanceof CancellationException)
                            x = failure;
                        else if (canceled)
                            x = new CancellationException(Tr.formatMessage(tc, "CWWKC1110.task.canceled", getName(task), managedExecutor.name));
                        else if (aborted)
                            x = new AbortedException(failure);
                        else
                            x = failure;
                        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                            Tr.event(this, tc, "taskAborted", managedExecutor, task, x);
                        listener.taskAborted(future, managedExecutor, task, x);
                    }
                } catch (Throwable x) {
                    Tr.error(tc, "CWWKC1102.listener.failed", getName(task), managedExecutor.name, x);
                    if (failure == null)
                        failure = x;
                }

                // notify listener: taskDone
                try {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                        Tr.event(this, tc, "taskDone", managedExecutor, task, failure);
                    listener.taskDone(future, managedExecutor, task, failure);
                } catch (Throwable x) {
                    Tr.error(tc, "CWWKC1102.listener.failed", getName(task), managedExecutor.name, x);
                }
            }
        }

        // Restore thread context
        if (pending <= 0 && startObj != null) {
            @SuppressWarnings("unchecked")
            ArrayList<ThreadContext> contextAppliedToThread = (ArrayList<ThreadContext>) startObj;
            threadContextDescriptor.taskStopping(contextAppliedToThread);
        }
    }

    @FFDCIgnore({ Error.class, RuntimeException.class }) // No need for FFDC, error is logged instead
    @Override
    public Object onStart(Object task, PolicyTaskFuture<?> future) {
        // EE Concurrency 3.1.6.1: No task submitted to an executor can run if task's component is not started.
        // ThreadContextDescriptor.taskStarting covers this requirement for us.
        ArrayList<ThreadContext> contextAppliedToThread = threadContextDescriptor.taskStarting();

        // notify listener: taskStarting
        if (task instanceof ManagedTask) {
            ManagedTaskListener listener = ((ManagedTask) task).getManagedTaskListener();
            if (listener != null)
                try {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                        Tr.event(this, tc, "taskStarting", managedExecutor, task);
                    listener.taskStarting(future, managedExecutor, task);
                } catch (Error x) {
                    Tr.error(tc, "CWWKC1102.listener.failed", getName(task), managedExecutor.name, x);
                    threadContextDescriptor.taskStopping(contextAppliedToThread);
                    throw x;
                } catch (RuntimeException x) {
                    Tr.error(tc, "CWWKC1102.listener.failed", getName(task), managedExecutor.name, x);
                    threadContextDescriptor.taskStopping(contextAppliedToThread);
                    throw x;
                }
        }

        return contextAppliedToThread;
    }

    @Override
    public void onSubmit(Object task, PolicyTaskFuture<?> future, int invokeAnyCount) {
        // notify listener: taskSubmitted
        if (task instanceof ManagedTask) {
            ManagedTaskListener listener = ((ManagedTask) task).getManagedTaskListener();
            if (listener != null) {
                ThreadContext tranContextRestorer = managedExecutor.suspendTransaction();
                try {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                        Tr.event(this, tc, "taskSubmitted", managedExecutor, task);
                    listener.taskSubmitted(future, managedExecutor, task);
                } finally {
                    if (tranContextRestorer != null)
                        tranContextRestorer.taskStopping();
                }

                if (invokeAnyCount <= 1 && future.isCancelled())
                    if (invokeAnyCount == 1)
                        throw new RejectedExecutionException(Tr.formatMessage(tc, "CWWKC1112.all.tasks.canceled"));
                    else
                        throw new RejectedExecutionException(Tr.formatMessage(tc, "CWWKC1110.task.canceled", getName(task), managedExecutor.name));
            }
        }
    }

    @Override
    public void raiseAbortedException(Throwable x) throws ExecutionException {
        throw new AbortedException(x);
    }

    @Override
    public void resolveDeadlockOnFutureGet() throws InterruptedException {
        throw new InterruptedException(Tr.formatMessage(tc, "CWWKC1120.future.get.rejected"));
    }
}