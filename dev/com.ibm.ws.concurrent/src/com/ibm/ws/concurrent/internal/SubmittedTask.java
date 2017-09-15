/*******************************************************************************
 * Copyright (c) 2013, 2016 IBM Corporation and others.
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
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import javax.enterprise.concurrent.AbortedException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.wsspi.threadcontext.ThreadContext;
import com.ibm.wsspi.threadcontext.ThreadContextDescriptor;
import com.ibm.wsspi.threadcontext.ThreadContextProvider;

/**
 * Managed task/future wrapper that sends events to the ManagedTaskListener
 */
public class SubmittedTask<T> extends AbstractTask<T> implements Runnable {
    private static final TraceComponent tc = Tr.register(SubmittedTask.class);

    /**
     * Tracks the thread of execution for the scenario where there is no future (invokeAny)
     * or the future is unavailable (invokeAll).
     */
    private Thread executionThread;

    /**
     * Lock for accessing the executionThread.
     */
    private final Integer executionThreadLock = new Integer(0);

    /**
     * Failure (if any) that occurred when running the task.
     */
    private final AtomicReference<Throwable> failureRef = new AtomicReference<Throwable>();

    /**
     * Future for this managed task.
     */
    final FutureImpl future = new FutureImpl();

    /**
     * Indicates whether or not resultRef contains a result.
     */
    private volatile boolean isResultPopulated;

    /**
     * Managed (Scheduled) executor service to which the task was submitted.
     */
    private final ManagedExecutorServiceImpl managedExecSvc;

    /**
     * Result of the task.
     */
    private final AtomicReference<T> resultRef = new AtomicReference<T>();

    /**
     * Value to use as the result if the task is a runnable. If not a runnable, or no result, then null.
     */
    private final T runnableResult;

    /**
     * Current state.
     */
    final AtomicReference<State> state = new AtomicReference<State>(State.NONE);

    /**
     * Possible states. Refer to ManagedTaskListener JavaDoc.
     */
    @Trivial
    private enum State {
        NONE, SUBMITTED, STARTING, CANCELLED, ABORTED, DONE
    }

    /**
     * Interrupts the thread (if any) that is executing the task.
     */
    @Trivial
    private static class InterruptAction implements PrivilegedAction<Void> {
        private final Thread executionThread;

        private InterruptAction(Thread executionThread) {
            this.executionThread = executionThread;
        }

        /**
         * Interrupt the thread (if any) that is executing the task.
         */
        @Override
        public Void run() {
            executionThread.interrupt();
            return null;
        }
    }

    /**
     * Create a managed task, initialized with SUBMITTED state.
     * The caller is responsible for actually submitting the task to an executor of its choosing.
     *
     * @param managedExecSvc managed executor service to which the task was submitted
     * @param task the task
     * @param execProps execution properties
     * @param threadContextDescriptor previously captured thread context
     * @param result optional, predetermined result for the task. Null if none.
     */
    SubmittedTask(ManagedExecutorServiceImpl managedExecSvc, Object task, ThreadContextDescriptor threadContextDescriptor, T result) {
        super(task);
        this.managedExecSvc = managedExecSvc;
        this.runnableResult = result;
        this.threadContextDescriptor = threadContextDescriptor;

        // notify listener: taskSubmitted
        if (listener != null) {
            ThreadContextProvider tranContextProvider = AccessController.doPrivileged(managedExecSvc.tranContextProviderAccessor);
            ThreadContext suspendTranContext = tranContextProvider == null ? null : tranContextProvider.captureThreadContext(XPROPS_SUSPEND_TRAN, null);
            if (suspendTranContext != null)
                suspendTranContext.taskStarting();
            try {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                    Tr.event(this, tc, "taskSubmitted", managedExecSvc, task);
                listener.taskSubmitted(future, managedExecSvc, task);
            } finally {
                if (suspendTranContext != null)
                    suspendTranContext.taskStopping();
            }
        }

        state.compareAndSet(State.NONE, State.SUBMITTED);
    }

    /**
     * @see java.util.concurrent.Callable#call()
     */
    @FFDCIgnore(value = { Error.class, Exception.class, RuntimeException.class, Throwable.class })
    @Override
    public T call() throws Exception {
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        if (future.futureRef.get() == null)
            synchronized (executionThreadLock) {
                executionThread = Thread.currentThread();
            }

        ArrayList<ThreadContext> contextAppliedToThread = null;
        try {
            if (!state.compareAndSet(State.SUBMITTED, State.STARTING))
                throw new CancellationException(Tr.formatMessage(tc, "CWWKC1110.task.canceled", getName(), managedExecSvc.name));

            boolean aborted = true;
            try {
                // EE Concurrency 3.1.6.1: No task submitted to an executor can run if task's component is not started.
                // ThreadContextDescriptor.taskStarting covers this requirement for us.
                contextAppliedToThread = threadContextDescriptor.taskStarting();

                if (listener != null) {
                    if (trace && tc.isEventEnabled())
                        Tr.event(this, tc, "taskStarting", managedExecSvc, task);
                    listener.taskStarting(future, managedExecSvc, task);
                }

                if (State.STARTING.equals(state.get())) {
                    aborted = false;
                    @SuppressWarnings("unchecked")
                    T result = ((Callable<T>) task).call();
                    resultRef.set(result);
                    isResultPopulated = true;
                    return result;
                }
            } catch (Exception x) {
                Tr.error(tc, "CWWKC1101.task.failed", getName(), managedExecSvc.name, x);
                failureRef.compareAndSet(null, x);
                throw x;
            } catch (Error x) {
                Tr.error(tc, "CWWKC1101.task.failed", getName(), managedExecSvc.name, x);
                failureRef.compareAndSet(null, x);
                throw x;
            } finally {
                Future<T> f = future.futureRef.get();
                State newState = f != null && f.isCancelled() ? State.CANCELLED : aborted ? State.ABORTED : State.DONE;
                if ((state.compareAndSet(State.STARTING, newState) || State.CANCELLED.equals(state.get())) && listener != null)
                    try {
                        boolean canceled = State.CANCELLED.equals(state.get());
                        if (aborted || canceled) {
                            Throwable x = failureRef.get();
                            if (canceled || x == null)
                                x = new CancellationException(Tr.formatMessage(tc, "CWWKC1110.task.canceled", getName(), managedExecSvc.name));
                            else if (!(x instanceof CancellationException || x instanceof AbortedException))
                                x = new AbortedException(x);

                            if (trace && tc.isEventEnabled())
                                Tr.event(this, tc, "taskAborted", managedExecSvc, task, x);
                            listener.taskAborted(future, managedExecSvc, task, x);
                        }
                    } catch (Error x) {
                        Tr.error(tc, "CWWKC1102.listener.failed", getName(), managedExecSvc.name, x);
                        failureRef.compareAndSet(null, x);
                        throw x;
                    } catch (RuntimeException x) {
                        Tr.error(tc, "CWWKC1102.listener.failed", getName(), managedExecSvc.name, x);
                        failureRef.compareAndSet(null, x);
                        throw x;
                    } finally {
                        try {
                            if (trace && tc.isEventEnabled())
                                Tr.event(this, tc, "taskDone", managedExecSvc, task, failureRef);
                            listener.taskDone(future, managedExecSvc, task, failureRef.get());
                        } catch (Throwable x) {
                            Tr.error(tc, "CWWKC1102.listener.failed", getName(), managedExecSvc.name, x);
                        }
                    }
            }

            throw new CancellationException(Tr.formatMessage(tc, "CWWKC1110.task.canceled", getName(), managedExecSvc.name));
        } finally {
            if (contextAppliedToThread != null)
                threadContextDescriptor.taskStopping(contextAppliedToThread);

            synchronized (executionThreadLock) {
                executionThread = null;
            }
        }
    }

    /**
     * @see java.util.concurrent.RunnableFuture#run()
     */
    @FFDCIgnore(value = { Error.class, RuntimeException.class, Throwable.class })
    @Override
    public void run() {
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        if (future.futureRef.get() == null)
            synchronized (executionThreadLock) {
                executionThread = Thread.currentThread();
            }

        ArrayList<ThreadContext> contextAppliedToThread = null;
        try {
            if (!state.compareAndSet(State.SUBMITTED, State.STARTING))
                throw new CancellationException(Tr.formatMessage(tc, "CWWKC1110.task.canceled", getName(), managedExecSvc.name));

            boolean aborted = true;
            try {
                // EE Concurrency 3.1.6.1: No task submitted to an executor can run if task's component is not started.
                // ThreadContextDescriptor.taskStarting covers this requirement for us
                contextAppliedToThread = threadContextDescriptor.taskStarting();

                if (listener != null) {
                    if (trace && tc.isEventEnabled())
                        Tr.event(this, tc, "taskStarting", managedExecSvc, task);
                    listener.taskStarting(future, managedExecSvc, task);
                }

                if (State.STARTING.equals(state.get())) {
                    aborted = false;
                    ((Runnable) task).run();
                    resultRef.set(runnableResult);
                    isResultPopulated = true;
                    return;
                }
            } catch (RuntimeException x) {
                Tr.error(tc, "CWWKC1101.task.failed", getName(), managedExecSvc.name, x);
                failureRef.compareAndSet(null, x);
                throw x;
            } catch (Error x) {
                Tr.error(tc, "CWWKC1101.task.failed", getName(), managedExecSvc.name, x);
                failureRef.compareAndSet(null, x);
                throw x;
            } finally {
                Future<T> f = future.futureRef.get();
                State newState = f != null && f.isCancelled() ? State.CANCELLED : aborted ? State.ABORTED : State.DONE;
                if ((state.compareAndSet(State.STARTING, newState) || State.CANCELLED.equals(state.get())) && listener != null)
                    try {
                        boolean canceled = State.CANCELLED.equals(state.get());
                        if (aborted || canceled) {
                            Throwable x = failureRef.get();
                            if (canceled || x == null)
                                x = new CancellationException(Tr.formatMessage(tc, "CWWKC1110.task.canceled", getName(), managedExecSvc.name));
                            else if (!(x instanceof CancellationException || x instanceof AbortedException))
                                x = new AbortedException(x);

                            if (trace && tc.isEventEnabled())
                                Tr.event(this, tc, "taskAborted", managedExecSvc, task, x);
                            listener.taskAborted(future, managedExecSvc, task, x);
                        }
                    } catch (Error x) {
                        Tr.error(tc, "CWWKC1102.listener.failed", getName(), managedExecSvc.name, x);
                        failureRef.compareAndSet(null, x);
                        throw x;
                    } catch (RuntimeException x) {
                        Tr.error(tc, "CWWKC1102.listener.failed", getName(), managedExecSvc.name, x);
                        failureRef.compareAndSet(null, x);
                        throw x;
                    } finally {
                        try {
                            if (trace && tc.isEventEnabled())
                                Tr.event(this, tc, "taskDone", managedExecSvc, task, failureRef);
                            listener.taskDone(future, managedExecSvc, task, failureRef.get());
                        } catch (Throwable x) {
                            Tr.error(tc, "CWWKC1102.listener.failed", getName(), managedExecSvc.name, x);
                        }
                    }
            }

            throw new CancellationException(Tr.formatMessage(tc, "CWWKC1110.task.canceled", getName(), managedExecSvc.name));
        } finally {
            if (contextAppliedToThread != null)
                threadContextDescriptor.taskStopping(contextAppliedToThread);

            synchronized (executionThreadLock) {
                executionThread = null;
            }
        }
    }

    /**
     * Future for this scheduled task.
     *
     * Note: this class has a natural ordering that is inconsistent with equals.
     */
    @Trivial
    class FutureImpl implements Future<T> {
        /**
         * Reference to the future for this task.
         */
        private final AtomicReference<Future<T>> futureRef = new AtomicReference<Future<T>>();

        /**
         * @see java.util.concurrent.Future#cancel(boolean)
         */
        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            final boolean trace = TraceComponent.isAnyTracingEnabled();
            if (trace && tc.isEntryEnabled())
                Tr.entry(SubmittedTask.this, tc, "cancel", mayInterruptIfRunning);

            boolean canceled;
            boolean cancelStartedTask = false;
            if (!state.compareAndSet(State.NONE, State.CANCELLED)
                && !state.compareAndSet(State.SUBMITTED, State.CANCELLED)
                && !(cancelStartedTask = state.compareAndSet(State.STARTING, State.CANCELLED)))
                canceled = false;
            else
                try {
                    Future<T> future = futureRef.get();
                    if (future != null)
                        future.cancel(mayInterruptIfRunning);
                    else if (mayInterruptIfRunning) {
                        synchronized (executionThreadLock) {
                            if (executionThread != null) {
                                Thread threadToInterrupt = executionThread;
                                if (!executionThread.equals(Thread.currentThread()))
                                    executionThread = null;
                                AccessController.doPrivileged(new InterruptAction(threadToInterrupt));
                            }
                        }
                    }

                    if (!cancelStartedTask && listener != null) {
                        ThreadContextProvider tranContextProvider = AccessController.doPrivileged(managedExecSvc.tranContextProviderAccessor);
                        ThreadContext suspendTranContext = tranContextProvider == null ? null : tranContextProvider.captureThreadContext(XPROPS_SUSPEND_TRAN, null);
                        if (suspendTranContext != null)
                            suspendTranContext.taskStarting();
                        try {
                            CancellationException x = new CancellationException(Tr.formatMessage(tc, "CWWKC1110.task.canceled", getName(), managedExecSvc.name));

                            if (trace && tc.isEventEnabled())
                                Tr.event(this, tc, "taskAborted", managedExecSvc, task, x);
                            listener.taskAborted(this, managedExecSvc, task, x);
                        } catch (RuntimeException failure) {
                            failureRef.compareAndSet(null, failure);
                            throw failure;
                        } catch (Error failure) {
                            failureRef.compareAndSet(null, failure);
                            throw failure;
                        } finally {
                            try {
                                if (trace && tc.isEventEnabled())
                                    Tr.event(this, tc, "taskDone", managedExecSvc, task, failureRef);
                                listener.taskDone(this, managedExecSvc, task, failureRef.get());
                            } finally {
                                if (suspendTranContext != null)
                                    suspendTranContext.taskStopping();
                            }
                        }
                    }

                    canceled = true;
                } catch (Error x) {
                    if (trace && tc.isEntryEnabled())
                        Tr.exit(SubmittedTask.this, tc, "cancel", Utils.toString(x));
                    throw x;
                } catch (RuntimeException x) {
                    if (trace && tc.isEntryEnabled())
                        Tr.exit(SubmittedTask.this, tc, "cancel", Utils.toString(x));
                    throw x;
                }

            if (trace && tc.isEntryEnabled())
                Tr.exit(SubmittedTask.this, tc, "cancel", canceled);
            return canceled;
        }

        /**
         * @see java.util.concurrent.Future#get()
         */
        @Override
        public T get() throws InterruptedException, ExecutionException {
            final boolean trace = TraceComponent.isAnyTracingEnabled();
            if (trace && tc.isEntryEnabled())
                Tr.entry(SubmittedTask.this, tc, "get");

            Throwable x = failureRef.get();
            State s = state.get();
            if (s == State.CANCELLED)
                x = new CancellationException(Tr.formatMessage(tc, "CWWKC1110.task.canceled", getName(), managedExecSvc.name));
            else if (s == State.ABORTED && x != null)
                x = new AbortedException(x);
            else if (x == null)
                try {
                    Future<T> future = futureRef.get();
                    T result = null;
                    if (isResultPopulated)
                        result = resultRef.get();
                    else if (future != null)
                        result = future.get();
                    else
                        // We do not permit Future.get on taskSubmitted/taskStarting because the thread that invokes
                        // the listener method does not submit/start the task until the listener method returns.
                        x = new InterruptedException(Tr.formatMessage(tc, "CWWKC1120.future.get.rejected"));

                    if (x == null) {
                        if (trace && tc.isEntryEnabled())
                            Tr.exit(SubmittedTask.this, tc, "get", result);
                        return result;
                    }
                } catch (ExecutionException xx) {
                    x = state.get() == State.ABORTED ? new AbortedException(xx.getCause()) : xx;
                } catch (Throwable t) {
                    x = t;
                }
            else if (x instanceof InterruptedException) // ensure InterruptedException raised by run/call is wrapped in ExecutionException
                x = new ExecutionException(x);

            if (trace && tc.isEntryEnabled())
                Tr.exit(SubmittedTask.this, tc, "get", Utils.toString(x));
            if (x instanceof CancellationException)
                throw (CancellationException) x;
            else if (x instanceof ExecutionException)
                throw (ExecutionException) x;
            else if (x instanceof InterruptedException)
                throw (InterruptedException) x;
            else
                throw new ExecutionException(x);
        }

        /**
         * @see java.util.concurrent.Future#get(long, java.util.concurrent.TimeUnit)
         */
        @Override
        public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            final boolean trace = TraceComponent.isAnyTracingEnabled();
            if (trace && tc.isEntryEnabled())
                Tr.entry(SubmittedTask.this, tc, "get", timeout, unit);

            Throwable x = failureRef.get();
            State s = state.get();
            if (s == State.CANCELLED)
                x = new CancellationException(Tr.formatMessage(tc, "CWWKC1110.task.canceled", getName(), managedExecSvc.name));
            else if (s == State.ABORTED && x != null)
                x = new AbortedException(x);
            else if (x == null)
                try {
                    Future<T> future = futureRef.get();
                    T result = null;
                    if (isResultPopulated)
                        result = resultRef.get();
                    else if (future != null)
                        result = future.get(timeout, unit);
                    else
                        // We do not permit Future.get on taskSubmitted/taskStarting because the thread that invokes
                        // the listener method does not submit/start the task until the listener method returns.
                        x = new InterruptedException(Tr.formatMessage(tc, "CWWKC1120.future.get.rejected"));

                    if (x == null) {
                        if (trace && tc.isEntryEnabled())
                            Tr.exit(SubmittedTask.this, tc, "get", result);
                        return result;
                    }
                } catch (ExecutionException xx) {
                    x = state.get() == State.ABORTED ? new AbortedException(xx.getCause()) : xx;
                } catch (Throwable t) {
                    x = t;
                }
            else if (x instanceof InterruptedException) // ensure InterruptedException raised by run/call is wrapped in ExecutionException
                x = new ExecutionException(x);

            if (trace && tc.isEntryEnabled())
                Tr.exit(SubmittedTask.this, tc, "get", Utils.toString(x));
            if (x instanceof CancellationException)
                throw (CancellationException) x;
            else if (x instanceof ExecutionException)
                throw (ExecutionException) x;
            else if (x instanceof InterruptedException)
                throw (InterruptedException) x;
            else if (x instanceof TimeoutException)
                throw (TimeoutException) x;
            else
                throw new ExecutionException(x);
        }

        /**
         * @see java.lang.Object#hashCode(int)
         */
        @Override
        public final int hashCode() {
            return SubmittedTask.this.hashCode(); // Use the hash code of the containing class to make debug easier
        }

        /**
         * @see java.util.concurrent.Future#isCancelled()
         */
        @Override
        public boolean isCancelled() {
            switch (state.get()) {
                case CANCELLED:
                    return true;
                case DONE:
                case ABORTED:
                    return false;
                default:
                    Future<T> future = futureRef.get();
                    return future != null && future.isCancelled() && (cancel(false) || state.get() == State.CANCELLED);
            }
        }

        /**
         * @see java.util.concurrent.Future#isDone()
         */
        @Override
        public boolean isDone() {
            State s = state.get();
            return s == State.DONE || s == State.ABORTED || isCancelled();
        }

        /**
         * Initialize the reference to the future from the underlying executor service.
         *
         * @param future future from the underlying executor service.
         */
        void set(Future<T> future) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(SubmittedTask.this, tc, "set", future);

            futureRef.set(future);
        }
    }
}