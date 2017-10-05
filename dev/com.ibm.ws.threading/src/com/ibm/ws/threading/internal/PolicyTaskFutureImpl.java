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
package com.ibm.ws.threading.internal;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.threading.PolicyTaskCallback;

/**
 * Allows the policy executor to tie into internal state and other details of a Future implementation.
 * This enables us to, for example, immediately free up a queue position upon cancel.
 *
 * @param <T> type of the result.
 */
public class PolicyTaskFutureImpl<T> implements Future<T> {
    private static final TraceComponent tc = Tr.register(PolicyTaskFutureImpl.class);

    // state constants
    private static final int PRESUBMIT = 0, SUBMITTED = 1, RUNNING = 2, ABORTED = 3, CANCELING = 4, CANCELED = 5, FAILED = 6, SUCCESSFUL = 7;

    /**
     * The task, if a Callable. It is wrapped with interceptors, if any.
     */
    private final Callable<T> callable;

    /**
     * Optional callback for life cycle events.
     */
    final PolicyTaskCallback callback;

    /**
     * The policy executor instance.
     */
    private final PolicyExecutorImpl executor;

    /**
     * Predefined result, if any, for Runnable tasks.
     */
    private final T predefinedResult;

    /**
     * The task, if a Runnable. It is wrapped with interceptors, if any.
     */
    private final Runnable runnable;

    /**
     * Represents the state of the future, and allows for waiters. PRESUBMIT state is SUBMITTED.
     * State transitions in one direction only:
     * PRESUBMIT --> CANCELED
     * PRESUBMIT --> SUBMITTED --> ABORTED,
     * PRESUBMIT --> SUBMITTED --> CANCELED,
     * PRESUBMIT --> SUBMITTED --> RUNNING --> CANCELING --> CANCELED,
     * PRESUBMIT --> SUBMITTED --> RUNNING --> FAILED,
     * PRESUBMIT --> SUBMITTED --> RUNNING --> SUCCESSFUL.
     * Always set the result before updating the state, so that get() operations that await state can rely on the result being available.
     */
    private final State state = new State();

    /**
     * The Callable or Runnable task. It is not wrapped with interceptors.
     */
    final Object task;

    /**
     * Thread of execution, while running.
     */
    private volatile Thread thread;

    /**
     * Tracker for invokeAny futures. Otherwise null.
     */
    private final InvokeAnyCompletionTracker tracker;

    /**
     * Result of the task. Can be a value, an exception, or other indicator (the state distinguishes).
     * PRESUBMITized to the state field as a way of indicating a result is not set. This allows the possibility of null results.
     */
    private final AtomicReference<Object> result = new AtomicReference<Object>(state);

    /**
     * Privileged action that interrupts a thread.
     */
    @Trivial
    private static class InterruptAction implements PrivilegedAction<Void> {
        private final Thread thread;

        private InterruptAction(Thread t) {
            thread = t;
        }

        @Override
        public Void run() {
            thread.interrupt();
            return null;
        }
    }

    /**
     * Tracks completion of an invokeAny operation. The invokeAny method follows a pattern of constructing an instance
     * of this tracker class from its thread of execution and later invoking completeInvokeAny at the end of the operation.
     */
    static class InvokeAnyCompletionTracker {
        /**
         * Count of pending tasks that have not completed or been canceled.
         */
        private final AtomicInteger pending;

        /**
         * Populated with first successful result. When set to the tracker instance, it means no successful result has been obtained yet.
         */
        private final AtomicReference<Object> result;

        /**
         * Thread of execution for the invokeAny operation.
         */
        private Thread thread;

        InvokeAnyCompletionTracker(int numTasks) {
            pending = new AtomicInteger(numTasks);
            result = new AtomicReference<Object>(this);
            thread = Thread.currentThread();
        }

        /**
         * Completes the processing for the invokeAny method for which this tracker was created.
         *
         * @param <T>
         *
         * @param futures futures for tasks submitted to invokeAny.
         * @return result of a task that completed successfully. If none completed successfully or exceptionally or were canceled, then returns null.
         *         The result should be used in combination with hasSuccessfulResult in order to disambiguate null values.
         * @throws CancellationException if no task completed successfully or exceptionally but at least one was canceled.
         * @throws ExecutionException if no task completed successfully but at least one completed exceptionally.
         */
        @SuppressWarnings("unchecked")
        <T> T completeInvokeAny(ArrayList<PolicyTaskFutureImpl<T>> futures) throws CancellationException, ExecutionException {
            synchronized (this) {
                thread = null;
            }

            boolean allTasksDone = pending.get() == 0;
            if (!allTasksDone)
                for (Future<?> f : futures)
                    f.cancel(true);

            Object result = this.result.get();
            if (result != this)
                return (T) result;
            else if (allTasksDone) { // cause ExecutionException (preferred) or CancellationException to be raised
                boolean canceled = false;
                for (PolicyTaskFutureImpl<?> f : futures) {
                    int s = f.state.get();
                    if (s == FAILED || s == ABORTED) {
                        Throwable x = (Throwable) f.result.get();
                        if (s == ABORTED && f.callback != null)
                            f.callback.raiseAbortedException(x);
                        throw new ExecutionException(x);
                    } else if (s == CANCELED || s == CANCELING)
                        canceled = true;
                }
                if (canceled)
                    throw new CancellationException();
            } // else allow original exception to be raised (InterruptedException or TimeoutException)
            return null;
        }

        /**
         * @return true if a successful result has been recorded, otherwise false.
         */
        boolean hasSuccessfulResult() {
            return result.get() != this;
        }

        /**
         * Interrupts the thread on which invokeAny is running so that it can complete.
         */
        private synchronized void notifyInvokeAny() {
            if (thread != null)
                try {
                    AccessController.doPrivileged(new InterruptAction(thread));
                } finally {
                    thread = null;
                }
        }
    }

    /**
     * Awaitable state. Specifically, allows awaiting the transition from PRESUBMIT/SUBMITTED/RUNNING
     * to an ABORTED/CANCELING/CANCELED/FAILED/SUCCESSFUL state.
     */
    @Trivial
    private static class State extends AbstractQueuedSynchronizer {
        private static final long serialVersionUID = 1L;

        private final int get() {
            return getState();
        }

        private boolean setRunning() {
            return compareAndSetState(SUBMITTED, RUNNING);
        }

        private boolean setSubmitted() {
            return compareAndSetState(PRESUBMIT, SUBMITTED);
        }

        @Override
        protected final int tryAcquireShared(int ignored) {
            return getState() > RUNNING ? 1 : -1;
        }

        @Override
        protected final boolean tryReleaseShared(int newState) {
            int oldState;
            while (!compareAndSetState(oldState = getState(), newState));
            return oldState <= RUNNING;
        }
    }

    PolicyTaskFutureImpl(PolicyExecutorImpl executor, Callable<T> task, PolicyTaskCallback callback) {
        if (task == null)
            throw new NullPointerException();
        this.callable = executor.globalExecutor.wrap(task);
        this.callback = callback;
        this.executor = executor;
        this.predefinedResult = null;
        this.runnable = null;
        this.task = task;
        this.tracker = null;

        if (callback != null)
            callback.onSubmit(task, this);
    }

    PolicyTaskFutureImpl(PolicyExecutorImpl executor, Callable<T> task, PolicyTaskCallback callback, InvokeAnyCompletionTracker tracker) {
        if (task == null)
            throw new NullPointerException();
        this.callable = executor.globalExecutor.wrap(task);
        this.callback = callback;
        this.executor = executor;
        this.predefinedResult = null;
        this.runnable = null;
        this.task = task;
        this.tracker = tracker;

        if (callback != null)
            callback.onSubmit(task, this);
    }

    PolicyTaskFutureImpl(PolicyExecutorImpl executor, Runnable task, T predefinedResult, PolicyTaskCallback callback) {
        if (task == null)
            throw new NullPointerException();
        this.callable = null;
        this.callback = callback;
        this.executor = executor;
        this.predefinedResult = predefinedResult;
        this.runnable = executor.globalExecutor.wrap(task);
        this.task = task;
        this.tracker = null;

        if (callback != null)
            callback.onSubmit(task, this);
    }

    /**
     * Invoked to indicate the task was successfully submitted.
     * Typically, this means the task has been successfully added to the task queue.
     * However, it can also mean the task has been accepted to run on the submitter's thread.
     */
    @Trivial
    final void accept() {
        state.setSubmitted();
    }

    /**
     * Await completion of the future. Completion could be successful, exceptional, or by cancellation.
     */
    void await() throws InterruptedException {
        if (state.get() <= RUNNING)
            state.acquireSharedInterruptibly(1);
    }

    /**
     * Await completion of the future. Completion could be successful, exceptional, or by cancellation.
     *
     * @return true if completed before the specified interval elapses, otherwise false.
     */
    boolean await(long time, TimeUnit unit) throws InterruptedException {
        return state.get() > RUNNING || state.tryAcquireSharedNanos(1, unit.toNanos(time));
    }

    @Override
    public boolean cancel(boolean interruptIfRunning) {
        if (result.compareAndSet(state, CANCELED)) {
            if (executor.queue.remove(this)) {
                state.releaseShared(CANCELED);
                executor.maxQueueSizeConstraint.release();
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(this, tc, "canceled from queue");
                if (callback != null)
                    callback.onCancel(task, this, false, false);
            } else if (state.get() == PRESUBMIT) {
                state.releaseShared(CANCELED);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(this, tc, "canceled during pre-submit");
                if (callback != null)
                    callback.onCancel(task, this, false, false);
            } else if (interruptIfRunning) {
                state.releaseShared(CANCELING);
                Thread t = thread;
                try {
                    if (t != null) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            Tr.debug(this, tc, "interrupting " + t);
                        AccessController.doPrivileged(new InterruptAction(t));
                    }
                } finally {
                    state.releaseShared(CANCELED);
                    if (callback != null)
                        callback.onCancel(task, this, false, true);
                }
            } else {
                state.releaseShared(CANCELED);
                if (callback != null)
                    callback.onCancel(task, this, false, true);
            }

            if (tracker != null && tracker.pending.decrementAndGet() == 0)
                tracker.notifyInvokeAny();

            return true;
        } else {
            // Prevent premature return from cancel that would allow subsequent isCanceled/isDone to return false
            while (result.get() == state)
                Thread.yield();
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public T get() throws InterruptedException, ExecutionException {
        int s = state.get();
        if (s == SUBMITTED || s == RUNNING && thread != Thread.currentThread()) {
            state.acquireSharedInterruptibly(1);
            s = state.get();
        }
        switch (s) {
            case SUCCESSFUL:
                return (T) result.get();
            case ABORTED:
                if (callback != null)
                    callback.raiseAbortedException((Throwable) result.get());
                // yes, we do mean to fall through here
            case FAILED:
                throw new ExecutionException((Throwable) result.get());
            case CANCELED:
            case CANCELING:
                throw new CancellationException();
            case RUNNING: // only possible when get() is invoked from thread of execution and therefore blocks completion
            case PRESUBMIT: // only possible when get() is invoke from onStart, which runs on the submitter's thread and therefore blocks completion
                throw new InterruptedException(); // TODO message
            default: // should be unreachable
                throw new IllegalStateException(Integer.toString(state.get()));
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public T get(long time, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        int s = state.get();
        if (s == SUBMITTED || s == RUNNING && thread != Thread.currentThread())
            if (state.tryAcquireSharedNanos(1, unit.toNanos(time)))
                s = state.get();
            else
                throw new TimeoutException();
        switch (s) {
            case SUCCESSFUL:
                return (T) result.get();
            case ABORTED:
                if (callback != null)
                    callback.raiseAbortedException((Throwable) result.get());
                // yes, we do mean to fall through here
            case FAILED:
                throw new ExecutionException((Throwable) result.get());
            case CANCELED:
            case CANCELING:
                throw new CancellationException();
            case RUNNING: // only possible when get() is invoked from thread of execution and therefore blocks completion
            case PRESUBMIT: // only possible when get() is invoke from onStart, which runs on the submitter's thread and therefore blocks completion
                throw new InterruptedException(); // TODO message
            default: // should be unreachable
                throw new IllegalStateException(Integer.toString(state.get()));
        }
    }

    @Override
    public boolean isCancelled() {
        int s = state.get();
        return s == CANCELED || s == CANCELING;
    }

    @Override
    public boolean isDone() {
        return state.get() > RUNNING;
    }

    /**
     * If the task hasn't already been run or canceled, run it on the current thread and record the result/failure,
     * allowing for possible interruption by the cancel method.
     */
    @FFDCIgnore(Throwable.class)
    void run() {
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        if (!state.setRunning()) {
            if (trace && tc.isDebugEnabled())
                Tr.debug(this, tc, "unable to run", state.get());
            if (callback != null)
                callback.onEnd(task, this, null, true, 0, null); // aborted, queued task will never run
            return;
        }

        boolean aborted = true;
        Object callbackContext = null;
        thread = Thread.currentThread();
        try {
            if (callback == null)
                aborted = false;
            else {
                callbackContext = callback.onStart(task, this);
                aborted = state.get() == CANCELED;
            }

            if (!aborted) {
                T t;
                if (callable == null) {
                    runnable.run();
                    t = predefinedResult;
                } else {
                    t = callable.call();
                }

                if (result.compareAndSet(state, t)) {
                    state.releaseShared(SUCCESSFUL);
                    if (tracker != null && tracker.result.compareAndSet(tracker, t))
                        tracker.notifyInvokeAny();
                }

                if (trace && tc.isDebugEnabled())
                    Tr.debug(this, tc, "run", t);
            }

            if (callback != null)
                try {
                    callback.onEnd(task, this, callbackContext, aborted, 0, null);
                } catch (Throwable x) {
                }
        } catch (Throwable x) {
            if (trace && tc.isDebugEnabled())
                Tr.debug(this, tc, "run", x);
            if (result.compareAndSet(state, x)) {
                state.releaseShared(aborted ? ABORTED : FAILED);
                if (tracker != null && tracker.pending.decrementAndGet() == 0)
                    tracker.notifyInvokeAny();
            }

            if (callback != null)
                callback.onEnd(task, this, callbackContext, aborted, 0, x);
        } finally {
            thread = null;
            // Prevent accidental interrupt of subsequent operations by awaiting the transition from CANCELING to CANCELED
            while (state.get() == CANCELING)
                Thread.yield();
        }
    }

    /**
     * @throws InterruptedException if the task failed with InterruptedException.
     */
    @Trivial
    void throwIfInterrupted() throws InterruptedException {
        int s = state.get();
        if (s == FAILED || s == ABORTED) {
            Object x = result.get();
            if (x instanceof InterruptedException) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(this, tc, "interrupted", x);
                throw (InterruptedException) x;
            }
        }
    }

    /**
     * String representation for debug purposes.
     *
     * @return output of the form: PolicyTaskFuture@12345678 for org.example.MyTask@23456789 SUCCESSFUL on MyPolicyExecutor: My Successful Task Result
     */
    @Trivial
    @Override
    public String toString() {
        StringBuilder b = new StringBuilder("PolicyTaskFuture@").append(Integer.toHexString(hashCode())).append(" for ").append(task).append(' ');
        int s = state.get();
        switch (s) {
            case PRESUBMIT:
                b.append("PRESUBMIT");
                break;
            case SUBMITTED:
                b.append("SUBMITTED");
                break;
            case RUNNING:
                b.append("RUNNING");
                break;
            case ABORTED:
                b.append("ABORTED");
                break;
            case CANCELING:
                b.append("CANCELING");
                break;
            case CANCELED:
                b.append("CANCELED");
                break;
            case FAILED:
                b.append("FAILED");
                break;
            case SUCCESSFUL:
                b.append("SUCCESSFUL");
                break;
            default:
                b.append(s); // should be unreachable
        }
        b.append(" on ").append(executor.identifier);
        if (s == SUCCESSFUL || s == FAILED)
            b.append(": ").append(result.get());
        return b.toString();
    }
}