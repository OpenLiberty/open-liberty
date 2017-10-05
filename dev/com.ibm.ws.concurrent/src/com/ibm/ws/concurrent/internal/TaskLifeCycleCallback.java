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

import java.security.AccessController;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;

import javax.enterprise.concurrent.AbortedException;
import javax.enterprise.concurrent.ManagedTask;
import javax.enterprise.concurrent.ManagedTaskListener;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.threading.PolicyTaskCallback;
import com.ibm.wsspi.threadcontext.ThreadContext;
import com.ibm.wsspi.threadcontext.ThreadContextDescriptor;
import com.ibm.wsspi.threadcontext.ThreadContextProvider;

/**
 * Callback that uses the notifications it receives about about task life cycle
 * to apply and remove thread context and send events to a ManagedTaskListener.
 * TODO always use policy executor and remove SubmittedTask, where much of this implementation is copied from,
 * if the life cycle callback is able to achieve the same behavior.
 */
public class TaskLifeCycleCallback extends PolicyTaskCallback {
    private static final TraceComponent tc = Tr.register(TaskLifeCycleCallback.class);

    /**
     * Managed executor to which the task was submitted.
     */
    private final ManagedExecutorServiceImpl managedExecutor;

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
    }

    /**
     * Returns the task name.
     *
     * @param task the task.
     * @return the task name.
     */
    @Trivial
    final String getName(Object task) {
        Map<String, String> execProps = threadContextDescriptor.getExecutionProperties();
        String taskName = execProps == null ? null : execProps.get(ManagedTask.IDENTITY_NAME);
        return taskName == null ? task.toString() : taskName;
    }

    @Override
    public void onCancel(Object task, Future<?> future, boolean timedOut, boolean whileRunning) {
        // Tasks that are canceled while running have the taskAborted notification sent on the thread of execution instead.

        // notify listener: taskAborted (if task was canceled before it started)
        if (!whileRunning && task instanceof ManagedTask) {
            ManagedTaskListener listener = ((ManagedTask) task).getManagedTaskListener();
            if (listener != null) {
                Throwable failure = null;
                ThreadContextProvider tranContextProvider = AccessController.doPrivileged(managedExecutor.tranContextProviderAccessor);
                ThreadContext suspendTranContext = tranContextProvider == null ? null : tranContextProvider.captureThreadContext(AbstractTask.XPROPS_SUSPEND_TRAN, null);
                if (suspendTranContext != null)
                    suspendTranContext.taskStarting();
                try {
                    CancellationException x = new CancellationException(Tr.formatMessage(tc, "CWWKC1110.task.canceled", getName(task), managedExecutor.name));

                    if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                        Tr.event(this, tc, "taskAborted", managedExecutor, task, x);
                    listener.taskAborted(future, managedExecutor, task, x);
                } catch (RuntimeException x) {
                    failure = x;
                    throw x;
                } catch (Error x) {
                    failure = x;
                    throw x;
                } finally {
                    try {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                            Tr.event(this, tc, "taskDone", managedExecutor, task, failure);
                        listener.taskDone(future, managedExecutor, task, failure);
                    } finally {
                        if (suspendTranContext != null)
                            suspendTranContext.taskStopping();
                    }
                }
            }
        }
    }

    @Override
    public void onEnd(Object task, Future<?> future, Object startObj, boolean aborted, int pending, Throwable failure) {
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

    @Override
    public Object onStart(Object task, Future<?> future) {
        // EE Concurrency 3.1.6.1: No task submitted to an executor can run if task's component is not started.
        // ThreadContextDescriptor.taskStarting covers this requirement for us.
        ArrayList<ThreadContext> contextAppliedToThread = threadContextDescriptor.taskStarting();

        // notify listener: taskStarting
        if (task instanceof ManagedTask) {
            ManagedTaskListener listener = ((ManagedTask) task).getManagedTaskListener();
            if (listener != null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                    Tr.event(this, tc, "taskStarting", managedExecutor, task);
                listener.taskStarting(future, managedExecutor, task);
            }
        }

        return contextAppliedToThread;
    }

    @Override
    public void onSubmit(Object task, Future<?> future) {
        // notify listener: taskSubmitted
        if (task instanceof ManagedTask) {
            ManagedTaskListener listener = ((ManagedTask) task).getManagedTaskListener();
            if (listener != null) {
                ThreadContextProvider tranContextProvider = AccessController.doPrivileged(managedExecutor.tranContextProviderAccessor);
                ThreadContext suspendTranContext = tranContextProvider == null ? null : tranContextProvider.captureThreadContext(AbstractTask.XPROPS_SUSPEND_TRAN, null);
                if (suspendTranContext != null)
                    suspendTranContext.taskStarting();
                try {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                        Tr.event(this, tc, "taskSubmitted", managedExecutor, task);
                    listener.taskSubmitted(future, managedExecutor, task);
                } finally {
                    if (suspendTranContext != null)
                        suspendTranContext.taskStopping();
                }

                if (future.isCancelled())
                    throw new RejectedExecutionException(Tr.formatMessage(tc, "CWWKC1110.task.canceled", getName(task), managedExecutor.name));
            }
        }
    }

    @Override
    public void raiseAbortedException(Throwable x) throws ExecutionException {
        throw new AbortedException(x);
    }
}