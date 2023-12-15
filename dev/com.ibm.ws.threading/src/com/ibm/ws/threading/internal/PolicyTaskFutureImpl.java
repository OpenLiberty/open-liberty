/*******************************************************************************
 * Copyright (c) 2017,2023 IBM Corporation and others.
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
package com.ibm.ws.threading.internal;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.threading.CancellableStage;
import com.ibm.ws.threading.PolicyTaskCallback;
import com.ibm.ws.threading.PolicyTaskFuture;
import com.ibm.ws.threading.StartTimeoutException;

/**
 * Allows the policy executor to tie into internal state and other details of a Future implementation.
 * This enables us to, for example, immediately free up a queue position upon cancel.
 *
 * @param <T> type of the result.
 */
public class PolicyTaskFutureImpl<T> implements PolicyTaskFuture<T> {
    private static final TraceComponent tc = Tr.register(PolicyTaskFutureImpl.class, "concurrencyPolicy", "com.ibm.ws.threading.internal.resources.ThreadingMessages");

    /**
     * The task, if a Callable. It is wrapped with interceptors, if any.
     */
    private final Callable<T> callable;

    /**
     * Optional callback for life cycle events.
     */
    final PolicyTaskCallback callback;

    /**
     * Allows the PolicyExecutor, upon shutdownNow, to cancel CompletionStage instances corresponding to queued tasks.
     */
    CancellableStage cancellableStage;

    /**
     * The policy executor instance.
     */
    final PolicyExecutorImpl executor;

    /**
     * Latch for invokeAny futures. Otherwise null.
     */
    private final InvokeAnyLatch latch;

    /**
     * Nanosecond timestamp when this Future was created.
     */
    final long nsAcceptBegin = System.nanoTime();

    /**
     * Nanosecond timestamps for various points in the task life cycle, initialized to unset (1 less than previous timestamp).
     */
    volatile long nsAcceptEnd = nsAcceptBegin - 1, nsQueueEnd = nsAcceptBegin - 2, nsRunEnd = nsAcceptBegin - 3;

    /**
     * Nanosecond timestamp by which the task must start. A value of <code>nsAcceptBegin - 1</code> indicates startTimeout is not enabled.
     */
    final long nsStartBy;

    /**
     * Predefined result, if any, for Runnable tasks.
     */
    private final T predefinedResult;

    /**
     * The task, if a Runnable. It is wrapped with interceptors, if any.
     */
    private final Runnable runnable;

    /**
     * Represents the state of the future, and allows for waiters. Initial state is PRESUBMIT.
     * State transitions in one direction only:
     * PRESUBMIT --> ABORTED
     * PRESUBMIT --> CANCELLED
     * PRESUBMIT --> SUBMITTED --> ABORTED,
     * PRESUBMIT --> SUBMITTED --> CANCELLED,
     * PRESUBMIT --> SUBMITTED --> RUNNING --> CANCELLING --> CANCELLED,
     * PRESUBMIT --> SUBMITTED --> RUNNING --> FAILED,
     * PRESUBMIT --> SUBMITTED --> RUNNING --> SUCCESS.
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
     * Result of the task. Can be a value, an exception, or other indicator (the state distinguishes).
     * Initialized to the state field as a way of indicating a result is not set. This allows the possibility of null results.
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
     * Latch for invokeAny that supports multiple countdown. When a task fails, we count down the latch by 1.
     * When a task completes successfully, we immediately count down the latch to 0.
     */
    static class InvokeAnyLatch extends AbstractQueuedSynchronizer {
        private static final long serialVersionUID = 1L;

        /**
         * Populated with first successful result. When set to the latch instance, it means no successful result has been obtained yet.
         * Set the result prior to counting down to ensure the result is always present to any thread that awaits the countdown to 0.
         */
        private final AtomicReference<Object> result;

        /**
         * @param count count of pending tasks that have not completed or been canceled.
         */
        @Trivial
        InvokeAnyLatch(int count) {
            setState(count);
            result = new AtomicReference<Object>(this);
        }

        /**
         * Await completion of the latch and return the result of one of the futures.
         *
         * @param timeoutNS maximum number of nanoseconds to wait. -1 to wait without applying a timeout.
         * @param futures   list of futures for tasks submitted to invokeAny
         * @return result of one of the futures if any are successful before the timeout elapses.
         * @throws CancellationException      if all tasks were canceled.
         * @throws ExecutionException         if all tasks completed, at least one exceptionally or aborted, and none were successful.
         * @throws InterruptedException       if interrupted.
         * @throws RejectedExecutionException if all tasks completed, at least one aborted, and none were successful.
         * @throws TimeoutException           if the timeout elapses before all tasks complete and no task has completed successfully.
         */
        @SuppressWarnings("unchecked")
        <T> T await(long timeoutNS, List<PolicyTaskFutureImpl<T>> futures) throws ExecutionException, InterruptedException, TimeoutException {
            int countdown = getCount();
            if (countdown > 0)
                if (timeoutNS < 0)
                    acquireSharedInterruptibly(countdown);
                else if (!tryAcquireSharedNanos(countdown, timeoutNS))
                    throw new TimeoutException();

            Object result = this.result.get();
            if (result != this)
                return (T) result;
            else { // cause ExecutionException/RejectedExecutionException (preferred) or CancellationException to be raised
                boolean canceled = false;
                for (PolicyTaskFutureImpl<T> f : futures) {
                    int s = f.state.get();
                    if (s == FAILED)
                        throw new ExecutionException((Throwable) f.result.get());
                    else if (s == ABORTED) {
                        Throwable x = (Throwable) f.result.get();
                        if (f.callback != null)
                            f.callback.raiseAbortedException(x);
                        throw new RejectedExecutionException(x);
                    } else if (s == CANCELLED || s == CANCELLING)
                        canceled = true;
                }
                if (canceled)
                    throw new CancellationException();
                else
                    throw new IllegalStateException(this + ", " + futures); // should be unreachable
            }
        }

        /**
         * Count down by 1 if the current count is positive.
         *
         * @return true if counted down, otherwise false.
         */
        boolean countDown() {
            return releaseShared(1);
        }

        /**
         * Count down the latch to 0 if the specified value can be set as the result.
         *
         * @param result successful result for invokeAny
         * @return true if we set the result and counted down to 0, otherwise false.
         */
        boolean countDown(Object result) {
            return this.result.compareAndSet(this, result) && releaseShared(getState());
        }

        @Trivial
        int getCount() {
            return getState();
        }

        @Override
        @Trivial
        public String toString() {
            Object r = result.get();
            return new StringBuilder("InvokeAnyLatch@").append(Integer.toHexString(hashCode())) //
                            .append(' ').append("count:").append(getState()) //
                            .append(", result:").append((r == this ? "<empty>" : r)).toString();
        }

        @Trivial
        @Override
        protected final int tryAcquireShared(int ignored) {
            return getState() > 0 ? -1 : 1;
        }

        @Trivial
        @Override
        protected final boolean tryReleaseShared(int amount) {
            if (amount < 0)
                throw new IllegalArgumentException(Integer.toString(amount));
            int count;
            while ((count = getState()) > 0 && !compareAndSetState(count, amount > count ? 0 : count - amount));
            return count > 0 && count <= amount;
        }
    }

    /**
     * Awaitable state. Specifically, allows awaiting the transition from PRESUBMIT/SUBMITTED/RUNNING
     * to an ABORTED/CANCELLING/CANCELLED/FAILED/SUCCESS state.
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

    @FFDCIgnore(RejectedExecutionException.class)
    PolicyTaskFutureImpl(PolicyExecutorImpl executor, Callable<T> task, PolicyTaskCallback callback, long startTimeoutNS) {
        if (task == null)
            throw new NullPointerException();
        this.callable = executor.libertyThreadPool.wrap(task);
        this.callback = callback;
        this.executor = executor;
        this.latch = null;
        this.nsStartBy = nsAcceptBegin + startTimeoutNS;
        this.predefinedResult = null;
        this.runnable = null;
        this.task = task;

        if (callback != null)
            try {
                callback.onSubmit(task, this, 0);
            } catch (Error x) {
                abort(false, x);
                throw x;
            } catch (RejectedExecutionException x) { // same as RuntimeException, but ignore FFDC
                abort(false, x);
                throw x;
            } catch (RuntimeException x) {
                abort(false, x);
                throw x;
            }
    }

    @FFDCIgnore(RejectedExecutionException.class)
    PolicyTaskFutureImpl(PolicyExecutorImpl executor, Callable<T> task, PolicyTaskCallback callback, long startTimeoutNS, InvokeAnyLatch latch) {
        if (task == null)
            throw new NullPointerException();
        this.callable = executor.libertyThreadPool.wrap(task);
        this.callback = callback;
        this.executor = executor;
        this.latch = latch;
        this.nsStartBy = nsAcceptBegin + startTimeoutNS;
        this.predefinedResult = null;
        this.runnable = null;
        this.task = task;

        if (callback != null)
            try {
                callback.onSubmit(task, this, latch.getCount());
            } catch (Error x) {
                abort(false, x);
                throw x;
            } catch (RejectedExecutionException x) { // same as RuntimeException, but ignore FFDC
                abort(false, x);
                throw x;
            } catch (RuntimeException x) {
                abort(false, x);
                throw x;
            }
    }

    @Trivial
    PolicyTaskFutureImpl(PolicyExecutorImpl executor, Runnable task, CancellableStage cancellableStage, long startTimeoutNS) {
        this(executor, task, null, null, startTimeoutNS);
        this.cancellableStage = cancellableStage;
    }

    @FFDCIgnore(RejectedExecutionException.class)
    PolicyTaskFutureImpl(PolicyExecutorImpl executor, Runnable task, T predefinedResult, PolicyTaskCallback callback, long startTimeoutNS) {
        if (task == null)
            throw new NullPointerException();
        this.callable = null;
        this.callback = callback;
        this.executor = executor;
        this.latch = null;
        this.nsStartBy = nsAcceptBegin + startTimeoutNS;
        this.predefinedResult = predefinedResult;
        this.runnable = executor.libertyThreadPool.wrap(task);
        this.task = task;

        if (callback != null)
            try {
                callback.onSubmit(task, this, 0);
            } catch (Error x) {
                abort(false, x);
                throw x;
            } catch (RejectedExecutionException x) { // same as RuntimeException, but ignore FFDC
                abort(false, x);
                throw x;
            } catch (RuntimeException x) {
                abort(false, x);
                throw x;
            }
    }

    /**
     * Invoked to abort a task.
     *
     * @param removeFromQueue indicates whether we should first remove the task from the executor's queue.
     * @param cause           the cause of the abort.
     * @return true if the future transitioned to ABORTED state.
     */
    final boolean abort(boolean removeFromQueue, Throwable cause) {
        if (removeFromQueue && executor.queue.remove(this))
            executor.maxQueueSizeConstraint.release();
        if (nsAcceptEnd == nsAcceptBegin - 1) // currently unset
            nsRunEnd = nsQueueEnd = nsAcceptEnd = System.nanoTime();
        boolean aborted = result.compareAndSet(state, cause);
        if (aborted)
            try {
                state.releaseShared(ABORTED);
                if (nsQueueEnd == nsAcceptBegin - 2) // currently unset
                    nsRunEnd = nsQueueEnd = System.nanoTime();
                if (callback != null)
                    callback.onEnd(task, this, null, true, 0, cause);
            } finally {
                if (latch != null)
                    latch.countDown();
                if (cancellableStage != null) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(this, tc, "completion stage to complete exceptionally: " + cancellableStage);
                    cancellableStage.completeExceptionally(cause);
                }
            }
        else {
            // Prevent premature return from abort that would allow subsequent getState() to indicate
            // that the task is still in SUBMITTED state.
            while (state.get() < RUNNING)
                Thread.yield();
        }
        return aborted;
    }

    /**
     * Invoked to indicate the task was successfully submitted.
     *
     * @param runOnSubmitter true if accepted to run immediately on the submitter's thread. False if accepted to the queue.
     */
    @Trivial
    final void accept(boolean runOnSubmitter) {
        long time;
        nsAcceptEnd = time = System.nanoTime();
        if (runOnSubmitter)
            nsQueueEnd = time;
        state.setSubmitted();
    }

    /**
     * Await completion of the future. Completion could be successful, exceptional, or by cancellation.
     *
     * @return state of the task after awaiting completion.
     */
    @Override
    public int await() throws InterruptedException {
        int s = state.get();
        if (s == SUBMITTED || s == RUNNING && thread != Thread.currentThread()) {
            if (s == RUNNING || nsStartBy == nsAcceptBegin - 1) { // already started or no start timeout
                state.acquireSharedInterruptibly(1);
                s = state.get();
            } else { // wait for up to the start timeout, then continue waiting if task has started
                long nsGetBegin = System.nanoTime();
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(this, tc, "await start timeout for " + (nsStartBy - nsGetBegin) + "ns");
                state.tryAcquireSharedNanos(1, nsStartBy - nsGetBegin);
                s = state.get();
                if (s == SUBMITTED) { // attempt to abort the task
                    abort(true, new StartTimeoutException(getIdentifier(), getTaskName(), //
                                    System.nanoTime() - nsAcceptBegin, //
                                    nsStartBy - nsAcceptBegin));
                    s = state.get();
                }
                if (s == RUNNING) { // continue waiting
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(this, tc, "await completion");
                    state.acquireSharedInterruptibly(1);
                    s = state.get();
                }
            }
        }
        return s;
    }

    /**
     * Await completion of the future. Completion could be successful, exceptional, or by cancellation.
     *
     * @return either the state of the task after awaiting completion, or, if a TimeoutException would be raised by Future.get then the TIMEOUT constant.
     */
    @Override
    public int await(long time, TimeUnit unit) throws InterruptedException {
        int s = state.get();
        if (s == SUBMITTED || s == RUNNING && thread != Thread.currentThread()) {
            long nsGetBegin, maxWaitNS = unit.toNanos(time);
            if (s == RUNNING // already started
                || nsStartBy == nsAcceptBegin - 1 // no start timeout
                || nsStartBy - (nsGetBegin = System.nanoTime()) > maxWaitNS) { // remaining start timeout exceeds the requested max wait
                if (state.tryAcquireSharedNanos(1, maxWaitNS))
                    s = state.get();
                else
                    s = TIMEOUT;
            } else { // first wait for up to the start timeout
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(this, tc, "await start timeout for " + (nsStartBy - nsGetBegin) + "ns");
                state.tryAcquireSharedNanos(1, nsStartBy - nsGetBegin);
                s = state.get();
                if (s == SUBMITTED) { // attempt to abort the task
                    abort(true, new StartTimeoutException(getIdentifier(), getTaskName(), //
                                    System.nanoTime() - nsAcceptBegin, //
                                    nsStartBy - nsAcceptBegin));
                    s = state.get();
                }
                if (s == RUNNING) { // wait for the remainder of the timeout supplied to get
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(this, tc, "await remainder of timeout");
                    if (state.tryAcquireSharedNanos(1, maxWaitNS - (System.nanoTime() - nsGetBegin)))
                        s = state.get();
                    else
                        s = TIMEOUT;
                }
            }
        }
        return s;
    }

    @Override
    public boolean cancel(boolean interruptIfRunning) {
        if (nsStartBy != nsAcceptBegin - 1 // has a start timeout
            && state.get() < RUNNING // not started yet
            && System.nanoTime() - nsStartBy > 0) // start timeout has elapsed
            abort(true, new StartTimeoutException(getIdentifier(), getTaskName(), //
                            System.nanoTime() - nsAcceptBegin, //
                            nsStartBy - nsAcceptBegin));

        if (result.compareAndSet(state, CANCELLED))
            try {
                if (executor.queue.remove(this)) {
                    nsRunEnd = nsQueueEnd = System.nanoTime();
                    state.releaseShared(CANCELLED);
                    executor.maxQueueSizeConstraint.release();
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(this, tc, "canceled from queue");
                    if (callback != null)
                        callback.onCancel(task, this, false);
                } else if (state.get() == PRESUBMIT) {
                    nsRunEnd = nsQueueEnd = nsAcceptEnd = System.nanoTime();
                    state.releaseShared(CANCELLED);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(this, tc, "canceled during pre-submit");
                    if (callback != null)
                        callback.onCancel(task, this, false);
                } else if (interruptIfRunning) {
                    state.releaseShared(CANCELLING);
                    Thread t = thread;
                    try {
                        if (t != null) {
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                                Tr.debug(this, tc, "interrupting " + t);
                            AccessController.doPrivileged(new InterruptAction(t));
                        }
                    } finally {
                        state.releaseShared(CANCELLED);
                        if (callback != null)
                            callback.onCancel(task, this, true);
                    }
                } else {
                    state.releaseShared(CANCELLED);
                    if (callback != null)
                        callback.onCancel(task, this, true);
                }

                return true;
            } finally {
                if (latch != null)
                    latch.countDown();
                if (cancellableStage != null) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(this, tc, "completion stage to cancel: " + cancellableStage);
                    cancellableStage.cancel(false);
                }
            }
        else {
            // Prevent premature return from cancel that would allow subsequent isCanceled/isDone to return false
            while (result.get() == state)
                Thread.yield();
            return false;
        }
    }

    /**
     * Java 19+ method that immediately returns the exception with which the task completes (not including cancellation)
     * or otherwise raises IllegalStateException.
     */
    public Throwable exceptionNow() {
        Throwable cause = null;
        if (isDone()) {
            int s = state.get();
            if (s == FAILED) {
                return (Throwable) result.get();
            } else if (s == ABORTED) {
                cause = (Throwable) result.get();
            } else if (s == CANCELLED || s == CANCELLING) {
                cause = new CancellationException();
            }
        }

        IllegalStateException x = new IllegalStateException(toString());
        if (cause != null)
            x.initCause(cause);
        throw x;
    }

    @SuppressWarnings("unchecked")
    @Override
    public T get() throws InterruptedException, ExecutionException {
        switch (await()) {
            case SUCCESS:
                return (T) result.get();
            case ABORTED:
                if (callback != null)
                    callback.raiseAbortedException((Throwable) result.get());
                throw new RejectedExecutionException((Throwable) result.get());
            case FAILED:
                throw new ExecutionException((Throwable) result.get());
            case CANCELLED:
            case CANCELLING:
                throw new CancellationException();
            case RUNNING: // only possible when get() is invoked from thread of execution and therefore blocks completion
            case PRESUBMIT: // only possible when get() is invoked from onStart, which runs on the submitter's thread and therefore blocks completion
                if (callback != null)
                    callback.resolveDeadlockOnFutureGet();
                throw new InterruptedException();
            default: // should be unreachable
                throw new IllegalStateException(Integer.toString(state.get()));
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public T get(long time, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        switch (await(time, unit)) {
            case SUCCESS:
                return (T) result.get();
            case ABORTED:
                if (callback != null)
                    callback.raiseAbortedException((Throwable) result.get());
                throw new RejectedExecutionException((Throwable) result.get());
            case FAILED:
                throw new ExecutionException((Throwable) result.get());
            case CANCELLED:
            case CANCELLING:
                throw new CancellationException();
            case TIMEOUT:
                throw new TimeoutException();
            case RUNNING: // only possible when get() is invoked from thread of execution and therefore blocks completion
            case PRESUBMIT: // only possible when get() is invoked from onStart, which runs on the submitter's thread and therefore blocks completion
                if (callback != null)
                    callback.resolveDeadlockOnFutureGet();
                throw new InterruptedException();
            default: // should be unreachable
                throw new IllegalStateException(Integer.toString(state.get()));
        }
    }

    @Override
    public final long getElapsedAcceptTime(TimeUnit unit) {
        long elapsed = nsAcceptEnd - nsAcceptBegin;
        return unit.convert(elapsed >= 0 ? elapsed : System.nanoTime() - nsAcceptBegin, TimeUnit.NANOSECONDS);
    }

    @Override
    public final long getElapsedQueueTime(TimeUnit unit) {
        long begin = nsAcceptEnd;
        long elapsed = nsQueueEnd - begin;
        return unit.convert(elapsed >= 0 ? elapsed : begin - nsAcceptBegin > 0 ? System.nanoTime() - begin : 0, TimeUnit.NANOSECONDS);
    }

    @Override
    public final long getElapsedRunTime(TimeUnit unit) {
        long begin = nsQueueEnd;
        long elapsed = nsRunEnd - begin;
        if (elapsed < 0)
            elapsed = begin - nsAcceptBegin > 0 ? System.nanoTime() - begin : 0;
        else if (elapsed > 0 && state.get() == ABORTED)
            elapsed = 0; // nsQueueEnd,nsRunEnd can get out of sync when abort is attempted by multiple threads at once
        return unit.convert(elapsed, TimeUnit.NANOSECONDS);
    }

    @Trivial
    final String getIdentifier() {
        return callback == null ? executor.identifier : callback.getIdentifier(executor.identifier);
    }

    @Trivial
    final String getTaskName() {
        return callback == null ? task.toString() : callback.getName(task);
    }

    @Override
    public boolean isCancelled() {
        int s = state.get();
        return s == CANCELLED || s == CANCELLING;
    }

    @Override
    public boolean isDone() {
        int s = state.get();
        return s > RUNNING // already done
               || nsStartBy != nsAcceptBegin - 1 // has a start timeout
                  && s < RUNNING // not started yet
                  && System.nanoTime() - nsStartBy > 0 // start timeout has elapsed
                  && (abort(true, new StartTimeoutException(getIdentifier(), getTaskName(), //
                                  System.nanoTime() - nsAcceptBegin, //
                                  nsStartBy - nsAcceptBegin))
                      || state.get() > RUNNING);
    }

    /**
     * Java 19+ method that immediately returns the result or otherwise raises IllegalStateException.
     */
    public T resultNow() {
        Throwable cause = null;
        if (isDone()) {
            int s = state.get();
            if (s == SUCCESS) {
                @SuppressWarnings("unchecked")
                T successfulResult = (T) result.get();
                return successfulResult;
            } else if (s == CANCELLED || s == CANCELLING) {
                cause = new CancellationException();
            } else {
                Object failure = result.get();
                if (failure instanceof Throwable)
                    cause = (Throwable) failure;
            }
        }

        IllegalStateException x = new IllegalStateException(toString());
        if (cause != null)
            x.initCause(cause);
        throw x;
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
            nsRunEnd = System.nanoTime();
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
                aborted = state.get() == CANCELLED;
            }

            if (aborted)
                nsRunEnd = System.nanoTime();
            else {
                T t;
                if (callable == null) {
                    runnable.run();
                    t = predefinedResult;
                } else {
                    t = callable.call();
                }

                nsRunEnd = System.nanoTime();

                if (result.compareAndSet(state, t)) {
                    state.releaseShared(SUCCESS);
                    if (latch != null)
                        latch.countDown(t);
                } else if (Integer.valueOf(CANCELLED).equals(result.get())) {
                    if (trace && tc.isDebugEnabled())
                        Tr.debug(this, tc, "canceled during/after run");
                    // Prevent dirty read of state during onEnd before the canceling thread transitions the state to CANCELLING/CANCELLED
                    while (state.get() == RUNNING)
                        Thread.yield();
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

            nsRunEnd = System.nanoTime();

            if (result.compareAndSet(state, x)) {
                state.releaseShared(aborted ? ABORTED : FAILED);
                if (latch != null)
                    latch.countDown();
            }

            if (callback != null)
                callback.onEnd(task, this, callbackContext, aborted, 0, x);
        } finally {
            thread = null;
            // Prevent accidental interrupt of subsequent operations by awaiting the transition from CANCELLING to CANCELLED
            while (state.get() == CANCELLING)
                Thread.yield();
        }
    }

    /**
     * TODO Add a more efficient implementation of this method once Java 21 is the minimum level.
     * This method of java.util.concurrent.Future was added in Java 19, but has a return type that
     * makes it incompatible with prior versions of Java.
     */
    // Future.State state()

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
     * @return output of the form: PolicyTaskFuture@12345678 for org.example.MyTask@23456789 SUCCESS on MyPolicyExecutor: My Successful Task Result
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
            case CANCELLING:
                b.append("CANCELLING");
                break;
            case CANCELLED:
                b.append("CANCELLED");
                break;
            case FAILED:
                b.append("FAILED");
                break;
            case SUCCESS:
                b.append("SUCCESS");
                break;
            default:
                b.append(s); // should be unreachable
        }
        b.append(" on ").append(getIdentifier());
        if (s == SUCCESS || s == FAILED)
            b.append(": ").append(result.get());
        return b.toString();
    }
}