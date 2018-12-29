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

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.threading.PolicyExecutor;
import com.ibm.ws.threading.PolicyTaskCallback;
import com.ibm.ws.threading.PolicyTaskFuture;
import com.ibm.ws.threading.StartTimeoutException;
import com.ibm.ws.threading.internal.PolicyTaskFutureImpl.InvokeAnyLatch;

/**
 * Policy executors are backed by the Liberty global thread pool,
 * but allow concurrency constraints and various queue attributes
 * to be controlled independently of the global thread pool.
 */
public class PolicyExecutorImpl implements PolicyExecutor {
    private static final TraceComponent tc = Tr.register(PolicyExecutorImpl.class, "concurrencyPolicy", "com.ibm.ws.threading.internal.resources.ThreadingMessages");

    @Trivial
    private static class Callback {
        private final Runnable runnable;
        private final long threshold;

        private Callback(long threshold, Runnable runnable) {
            this.runnable = runnable;
            this.threshold = threshold;
        }

        @Override
        public String toString() {
            return new StringBuilder("Callback@").append(Integer.toHexString(hashCode())).append(" threshold ").append(threshold).append(" for ").append(runnable).toString();
        }
    }

    private final AtomicReference<Callback> cbConcurrency = new AtomicReference<Callback>();
    private final AtomicReference<Callback> cbLateStart = new AtomicReference<Callback>();
    private final AtomicReference<Callback> cbQueueSize = new AtomicReference<Callback>();

    /**
     * Use this lock to make a consistent update to both expedite and expeditesAvailable,
     * maxConcurrency and maxConcurrencyConstraint, and to maxQueueSize and maxQueueSizeConstraint.
     */
    private final Integer configLock = new Integer(0); // new instance required to avoid sharing

    private int expedite;

    private final AtomicInteger expeditesAvailable = new AtomicInteger();

    ExecutorServiceImpl globalExecutor;

    String identifier;

    private int maxConcurrency;

    private final ReduceableSemaphore maxConcurrencyConstraint = new ReduceableSemaphore(0, false);

    private volatile MaxPolicy maxPolicy = MaxPolicy.loose;

    private int maxQueueSize;

    final ReduceableSemaphore maxQueueSizeConstraint = new ReduceableSemaphore(0, false);

    private final AtomicLong maxWaitForEnqueueNS = new AtomicLong();

    /**
     * This list is supplied to each instance that is programmatically created by PolicyExecutorProvider
     * so that each instance can manage its own membership per its life cycle.
     * The list is null if declarative services created this instance based on server configuration.
     */
    private final ConcurrentHashMap<String, PolicyExecutorImpl> policyExecutors;

    final ConcurrentLinkedQueue<PolicyTaskFutureImpl<?>> queue = new ConcurrentLinkedQueue<PolicyTaskFutureImpl<?>>();

    private volatile boolean runIfQueueFull;

    /**
     * Tasks that this policy executor is running on global executor threads. This is needed for the life cycle operations.
     */
    private final Set<PolicyTaskFutureImpl<?>> running = Collections.newSetFromMap(new ConcurrentHashMap<PolicyTaskFutureImpl<?>, Boolean>());

    /**
     * Count of tasks that this policy executor is running on global executor threads.
     */
    private final AtomicInteger runningCount = new AtomicInteger();

    /**
     * Latch that awaits the shutdown method progressing to ENQUEUE_STOPPED state.
     */
    private final CountDownLatch shutdownLatch = new CountDownLatch(1);

    /**
     * Latch that awaits the shutdownNow method progressing to TASKS_CANCELED state.
     */
    private final CountDownLatch shutdownNowLatch = new CountDownLatch(1);

    private volatile long startTimeout = -1;

    /**
     * Policy executor state, which transitions in one direction only. See constants for possible states.
     */
    private final AtomicReference<State> state = new AtomicReference<State>(State.ACTIVE);

    /**
     * Counter of tasks for which we didn't submit a GlobalPoolTask in order to honor maxConcurrency.
     * In deciding whether a GlobalPoolTask should be resubmitted, this counter can be decremented (if positive).
     */
    private final AtomicInteger withheldConcurrency = new AtomicInteger();

    @Trivial
    private static enum State {
        ACTIVE(true), // task submit/start/run all possible
        ENQUEUE_STOPPING(true), // enqueue is being disabled, submit might be possible, start/run still possible
        ENQUEUE_STOPPED(true), // task submit disallowed, start/run still possible
        TASKS_CANCELING(false), // task submit disallowed, start/run might be possible, queued and running tasks are being canceled
        TASKS_CANCELED(false), // task submit/start disallowed, waiting for all tasks to end
        TERMINATED(false); // task submit/start/run all disallowed

        boolean canStartTask;

        private State(boolean canStartTask) {
            this.canStartTask = canStartTask;
        }
    }

    /**
     * These tasks run on the global thread pool.
     * Their role is to run tasks that are queued up on the policy executor.
     */
    private class GlobalPoolTask implements QueueItem, Runnable {
        // Indicates whether or not this task should be expedited vs enqueued.
        private boolean expedite;

        @Override
        public boolean isExpedited() {
            return expedite;
        }

        @Override
        public void run() {
            boolean canRun;
            PolicyTaskFutureImpl<?> next;
            while ((next = (canRun = state.get().canStartTask) ? queue.poll() : null) != null) {
                maxQueueSizeConstraint.release();
                long nsQueueEnd = System.nanoTime();
                if (next.isCancelled()) {
                    next.nsRunEnd = next.nsQueueEnd = nsQueueEnd;
                    if (next.callback != null)
                        next.callback.onEnd(next.task, next, null, true, 0, null); // aborted, queued task will never run
                } else if (next.nsStartBy == next.nsAcceptBegin - 1 // never times out, or
                           || next.nsStartBy - nsQueueEnd > 0) { // have time remaining
                    next.nsQueueEnd = nsQueueEnd;
                    runTask(next);
                    break;
                } else { // timed out
                    next.nsRunEnd = next.nsQueueEnd = nsQueueEnd;
                    next.abort(false, new StartTimeoutException(next.getIdentifier(), next.getTaskName(), //
                                    nsQueueEnd - next.nsAcceptBegin, //
                                    next.nsStartBy - next.nsAcceptBegin));
                }
            }

            // Release permits against expedited/maxConcurrency
            if (expedite)
                expeditesAvailable.incrementAndGet();
            maxConcurrencyConstraint.release();

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(PolicyExecutorImpl.this, tc, "expedites/maxConcurrency available", expeditesAvailable, maxConcurrencyConstraint.availablePermits(), canRun);

            // Avoid reschedule if we are in a state that disallows starting tasks or if no withheld tasks remain
            if (canRun && withheldConcurrency.get() > 0 && maxConcurrencyConstraint.tryAcquire()) {
                decrementWithheldConcurrency();
                if (acquireExpedite() > 0)
                    expediteGlobal(GlobalPoolTask.this);
                else
                    enqueueGlobal(GlobalPoolTask.this);
            }
        }
    }

    /**
     * Utility class to convert a Callable to a Runnable, which is necessary for an implementation of
     * ExecutorService.shutdownNow to validly return as Runnables, which is required by the method signature,
     * a list of tasks that didn't start, where some of the tasks are Callable, not Runnable.
     */
    private static class RunnableFromCallable implements Runnable {
        private final Callable<?> callable;

        private RunnableFromCallable(Callable<?> callable) {
            this.callable = callable;
        }

        @FFDCIgnore(value = { Exception.class, RuntimeException.class })
        @Override
        public void run() {
            try {
                callable.call();
            } catch (RuntimeException x) {
                throw x;
            } catch (Exception x) {
                throw new RuntimeException(x);
            }
        }

    }

    /**
     * This constructor is used by PolicyExecutorProvider.
     *
     * @param globalExecutor the Liberty global executor, which was obtained by the PolicyExecutorProvider via declarative services.
     * @param identifier unique identifier for this instance, to be used for monitoring and problem determination.
     * @param policyExecutors list of policy executor instances created by the PolicyExecutorProvider.
     *            Each instance is responsible for adding and removing itself from the list per its life cycle.
     * @throws IllegalStateException if an instance with the specified unique identifier already exists and has not been shut down.
     * @throws NullPointerException if the specified identifier is null
     */
    public PolicyExecutorImpl(ExecutorServiceImpl globalExecutor, String identifier, ConcurrentHashMap<String, PolicyExecutorImpl> policyExecutors) {
        this.globalExecutor = globalExecutor;
        this.identifier = identifier;
        this.policyExecutors = policyExecutors;

        maxConcurrencyConstraint.release(maxConcurrency = Integer.MAX_VALUE);
        maxQueueSizeConstraint.release(maxQueueSize = Integer.MAX_VALUE);

        if (policyExecutors.putIfAbsent(this.identifier, this) != null)
            throw new IllegalStateException(this.identifier);
    }

    /**
     * Attempt to acquire a permit to expedite, which involves decrementing the available expedites.
     * Only allow decrement of a positive value, and otherwise indicate there are no available expedites.
     *
     * @return number of available expedites at the time we acquired a permit. 0 if none remains and we did not get a permit.
     */
    private int acquireExpedite() {
        int a;
        while ((a = expeditesAvailable.get()) > 0 && !expeditesAvailable.compareAndSet(a, a - 1));
        return a; // returning the value rather than true/false will enable better debug
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        // This method is optimized for the scenario where the user first invokes shutdownNow.
        // Absent that, we can progress to at least ENQUEUE_STOPPED, after which we can poll
        // for an empty queue and attempt to obtain all of the maxConcurrency permits
        // and then transition to TERMINATED state.
        final long start = System.nanoTime();

        // Progress the state at least to ENQUEUE_STOPPED (possibly TASKS_CANCELED)
        switch (state.get()) {
            case TASKS_CANCELING:
                if (!shutdownNowLatch.await(timeout, unit))
                    return false;
                break;
            case ACTIVE:
            case ENQUEUE_STOPPING:
                if (!shutdownLatch.await(timeout, unit))
                    return false;
                break;
            default:
        }

        final long pollInterval = TimeUnit.MILLISECONDS.toNanos(500);
        timeout = timeout < 0 ? 0 : unit.toNanos(timeout);
        boolean firstTime = true;

        for (long waitTime = System.nanoTime() - start, remaining; //
                        (remaining = timeout - waitTime) > 0 || firstTime; //
                        waitTime = System.nanoTime() - start) {
            if (firstTime)
                firstTime = false;
            State currentState = state.get();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(this, tc, "awaitTermination", remaining, currentState);
            switch (currentState) {
                case TERMINATED:
                    return true;
                case ENQUEUE_STOPPED:
                case TASKS_CANCELING:
                case TASKS_CANCELED:
                    // Transition to TERMINATED state if there are no tasks in the queue and we have no tasks on the global executor.
                    if (queue.isEmpty()) {
                        if (remaining > 0 ? maxConcurrencyConstraint.tryAcquire(maxConcurrency, remaining < pollInterval ? remaining : pollInterval, TimeUnit.NANOSECONDS) //
                                        : maxConcurrencyConstraint.tryAcquire(maxConcurrency)) {
                            State previous = state.getAndSet(State.TERMINATED);
                            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                                Tr.event(this, tc, "state: " + previous + " --> TERMINATED");
                            return true;
                        }
                    } else if (remaining > 0)
                        TimeUnit.NANOSECONDS.sleep(remaining < pollInterval ? remaining : pollInterval);
                    continue;
                default:
                    // unreachable
            }
        }

        return state.get() == State.TERMINATED; // one final chance, in case another thread has transitioned the state while we waited the last time
    }

    @Override
    public int cancel(String identifier, boolean interruptIfRunning) {
        int count = 0;

        // Remove and cancel all queued tasks.
        for (PolicyTaskFutureImpl<?> f = queue.poll(); f != null; f = queue.poll())
            if (f.cancel(false))
                count++;

        // Cancel tasks that are running
        for (Iterator<PolicyTaskFutureImpl<?>> it = running.iterator(); it.hasNext();)
            if (it.next().cancel(interruptIfRunning))
                count++;

        return count;
    }

    @Override
    public PolicyExecutor expedite(int num) {
        if (num == -1)
            num = Integer.MAX_VALUE;
        else if (num < 0)
            throw new IllegalArgumentException(Integer.toString(num));

        int a;
        synchronized (configLock) {
            if (num > maxConcurrency)
                throw new IllegalArgumentException("expedite: " + num + " > maxConcurrency: " + maxConcurrency);

            if (state.get() != State.ACTIVE)
                throw new IllegalStateException(Tr.formatMessage(tc, "CWWKE1203.config.update.after.shutdown", "expedite", identifier));

            a = expeditesAvailable.addAndGet(num - expedite);
            expedite = num;
        }

        // Expedite as many of the remaining tasks as the available maxConcurrency permits and increased expedites
        // will allow. We are choosing not to revoke GlobalPoolTasks that have already been enqueued as non-expedited,
        // which means we do not guarantee an increase in expedites to fully go into effect immediately.
        // Any reduction to expedites is handled gradually, as expedited GlobalPoolTasks complete.
        if (a > 0) {
            while (a-- > 0 && withheldConcurrency.get() > 0 && maxConcurrencyConstraint.tryAcquire())
                if (acquireExpedite() > 0) {
                    decrementWithheldConcurrency();
                    expediteGlobal(new GlobalPoolTask());
                } else {
                    maxConcurrencyConstraint.release();
                    break;
                }
        }

        return this;
    }

    /**
     * Decrement the counter of withheld concurrency only if positive.
     * This method should only ever be invoked if the caller is about to enqueue a task to the global executor.
     * Otherwise there is a risk of a race condition where withheldConcurrency decrements to 0 with a task still on the queue.
     */
    @Trivial
    private void decrementWithheldConcurrency() {
        int w;
        while ((w = withheldConcurrency.get()) > 0 && !withheldConcurrency.compareAndSet(w, w - 1));
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "withheld concurrency " + w + " --> " + (w == 0 ? 0 : (w - 1)));
    }

    /**
     * Attempt to add a task to the policy executor's queue, following the configured
     * behavior for waiting and rejecting vs running on the current thread if the queue is at capacity.
     * As needed, ensure that tasks are submitted to the global executor to process
     * the queued up tasks.
     *
     * @param policyTaskFuture submitted task and its Future.
     * @param wait amount of time to wait for a queue position.
     * @param runIfQueueFullOverride indicates if a task should always or may never run on the current thread
     *            if no queue positions are available. A value of null means the runIfQueueFull configuration will determine.
     *            A value of true must only be specified if the caller already has a permit or doesn't need one.
     * @return true if the task was enqueued for later execution by the global thread pool.
     *         If the task instead ran on the current thread, then returns false.
     * @throws RejectedExecutionException if the task is rejected rather than being queued.
     *             If this method runs the task on the current thread and the task raises InterruptedException,
     *             the InterruptedException is chained to the RejectedExecutionException.
     */
    @FFDCIgnore(value = { InterruptedException.class, RejectedExecutionException.class }) // these are raised directly to invoker, who decides how to handle
    private boolean enqueue(PolicyTaskFutureImpl<?> policyTaskFuture, long wait, Boolean runIfQueueFullOverride) {
        boolean enqueued;
        try {
            boolean haveQueuePermit = maxQueueSizeConstraint.tryAcquire();

            if (!haveQueuePermit && state.get() == State.ACTIVE) {
                long now = System.nanoTime(), nextTimeout = now; // means unset
                long waitEnd = wait > 0 ? now + wait : now;
                do {
                    // look for a timed-out task that can be purged to make room for this request
                    for (Iterator<PolicyTaskFutureImpl<?>> it = queue.iterator(); !haveQueuePermit && it.hasNext();) {
                        PolicyTaskFutureImpl<?> future = it.next();
                        long startBy = future.nsStartBy;
                        if (startBy != future.nsAcceptBegin - 1)
                            if (now - startBy >= 0) { // found a task in the queue that has timed out
                                if (queue.remove(future)) { // can't use iterator.remove - it doesn't tell us whether it actually removed anything
                                    future.nsRunEnd = future.nsQueueEnd = System.nanoTime();
                                    future.abort(false, new StartTimeoutException(future.getIdentifier(), future.getTaskName(), //
                                                    future.nsQueueEnd - future.nsAcceptBegin, //
                                                    future.nsStartBy - future.nsAcceptBegin));
                                    // Release and re-acquire is needed to preserve correctness of shutdown logic
                                    maxQueueSizeConstraint.release();
                                    haveQueuePermit = maxQueueSizeConstraint.tryAcquire();
                                } // else another thread removed it first, possibly to run it. Keep trying...
                            } else if (nextTimeout - startBy > 0 || nextTimeout == now)
                                nextTimeout = startBy;
                    }
                    if (!haveQueuePermit) {
                        long remain = waitEnd - System.nanoTime();
                        if (remain > 0) {
                            // wait incrementally in smallest of: { remaining time, next expected timeout, 1 second }
                            long w = nextTimeout == now || nextTimeout - now > remain ? remain : nextTimeout - now;
                            w = w < TimeUnit.SECONDS.toNanos(1) ? w : TimeUnit.SECONDS.toNanos(1);
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                                Tr.debug(this, tc, "incremental wait for enqueue of " + w + "ns");
                            haveQueuePermit = maxQueueSizeConstraint.tryAcquire(w, TimeUnit.NANOSECONDS);
                            nextTimeout = now = System.nanoTime();
                        }
                    }
                } while (!haveQueuePermit && waitEnd - now > 0);
            }

            if (haveQueuePermit) {
                Callback callback = cbQueueSize.get();
                if (callback != null
                    && maxQueueSizeConstraint.availablePermits() < callback.threshold
                    && cbQueueSize.compareAndSet(callback, null)) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(this, tc, "callback: queue capacity < " + callback.threshold, callback.runnable);
                    globalExecutor.submit(callback.runnable);
                }

                policyTaskFuture.accept(false);
                enqueued = queue.offer(policyTaskFuture);

                int w = withheldConcurrency.incrementAndGet();
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(this, tc, "withheld concurrency --> " + w);
                if (maxConcurrencyConstraint.tryAcquire()) {
                    decrementWithheldConcurrency();
                    if (acquireExpedite() > 0)
                        expediteGlobal(new GlobalPoolTask());
                    else
                        enqueueGlobal(new GlobalPoolTask());
                }

                // Check if shutdown occurred since acquiring the permit to enqueue, and if so, try to remove the queued task
                if (state.get() != State.ACTIVE && queue.remove(policyTaskFuture)) {
                    policyTaskFuture.nsRunEnd = policyTaskFuture.nsQueueEnd = System.nanoTime();
                    throw new RejectedExecutionException(Tr.formatMessage(tc, "CWWKE1202.submit.after.shutdown", policyTaskFuture.getIdentifier()));
                }
            } else if (state.get() == State.ACTIVE) {
                boolean haveConcurrencyPermit = false;
                if (policyTaskFuture.nsStartBy != policyTaskFuture.nsAcceptBegin - 1 // start timeout enabled, and
                    && System.nanoTime() - policyTaskFuture.nsStartBy >= 0) // timed out per the startTimeout
                    throw new RejectedExecutionException(new StartTimeoutException( //
                                    policyTaskFuture.getIdentifier(), policyTaskFuture.getTaskName(), //
                                    policyTaskFuture.nsQueueEnd - policyTaskFuture.nsAcceptBegin, //
                                    policyTaskFuture.nsStartBy - policyTaskFuture.nsAcceptBegin));
                else if (Boolean.TRUE.equals(runIfQueueFullOverride) ||
                         !Boolean.FALSE.equals(runIfQueueFullOverride) && runIfQueueFull
                                                                        && (maxPolicy == MaxPolicy.loose
                                                                            || (haveConcurrencyPermit = maxConcurrencyConstraint.tryAcquire())))
                    try {
                        policyTaskFuture.accept(true);
                        runTask(policyTaskFuture);
                        enqueued = false;
                    } finally {
                        if (haveConcurrencyPermit)
                            transferOrReleasePermit();
                    }
                else
                    throw new RejectedExecutionException(Tr.formatMessage(tc, "CWWKE1201.queue.full.abort", policyTaskFuture.getIdentifier(), maxQueueSize, wait));
            } else
                throw new RejectedExecutionException(Tr.formatMessage(tc, "CWWKE1202.submit.after.shutdown", policyTaskFuture.getIdentifier()));
        } catch (InterruptedException x) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(this, tc, "enqueue", x);
            policyTaskFuture.abort(false, x);
            throw new RejectedExecutionException(x);
        } catch (RejectedExecutionException x) { // redundant with RuntimeException code path, but added to allow FFDCIgnore
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(this, tc, "enqueue", x);
            policyTaskFuture.abort(false, x);
            throw x;
        } catch (RuntimeException x) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(this, tc, "enqueue", x);
            policyTaskFuture.abort(true, x);
            throw x;
        } catch (Error x) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(this, tc, "enqueue", x);
            policyTaskFuture.abort(true, x);
            throw x;
        }
        return enqueued;
    }

    /**
     * Queue a task to the global executor.
     * Prereq: maxConcurrencyConstraint permit must already be acquired to reflect the task being queued to global.
     * If unsuccessful in queuing to global, this method releases the maxConcurrencyConstraint permit.
     *
     * @param globalTask task that can execute tasks that are queued to the policy executor.
     */
    private void enqueueGlobal(GlobalPoolTask globalTask) {
        globalTask.expedite = false;
        boolean submitted = false;
        try {
            globalExecutor.executeWithoutInterceptors(globalTask);
            submitted = true;
        } finally {
            if (!submitted) {
                maxConcurrencyConstraint.release();

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(this, tc, "expedites/maxConcurrency available", expeditesAvailable, maxConcurrencyConstraint.availablePermits());
            }
        }
    }

    @Override
    public void execute(Runnable command) {
        enqueue(new PolicyTaskFutureImpl<Void>(this, command, null, null, startTimeout), maxWaitForEnqueueNS.get(), null);
    }

    /**
     * Expedite a task to the global executor.
     * Prereq: maxConcurrencyConstraint permit must already be acquired and
     * expeditesAvailable must already be decremented to reflect the task being expedited to global.
     * If unsuccessful in expediting to global, this method releases the maxConcurrencyConstraint permit
     * and increments expeditesAvailable.
     *
     * @param globalTask task that can execute tasks that are queued to the policy executor.
     */
    private void expediteGlobal(GlobalPoolTask globalTask) {
        globalTask.expedite = true;
        boolean submitted = false;
        try {
            globalExecutor.executeWithoutInterceptors(globalTask);
            submitted = true;
        } finally {
            if (!submitted) {
                int cca = expeditesAvailable.incrementAndGet();
                maxConcurrencyConstraint.release();

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(this, tc, "expedites/maxConcurrency available", cca, maxConcurrencyConstraint.availablePermits());
            }
        }
    }

    @Override
    @Trivial
    public String getIdentifier() {
        return identifier;
    }

    @Override
    public int getRunningTaskCount() {
        return runningCount.get();
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Trivial
    public final <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return (List) invokeAll(tasks, null);
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Trivial
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        return (List) invokeAll(tasks, null, timeout, unit);
    }

    // Submit and run tasks and return list of completed (possibly canceled) tasks.
    // Because this method is not timed, tasks can run on the current thread if maxPolicy=loose or a permit is available.
    @FFDCIgnore(value = { RejectedExecutionException.class })
    @Override
    public <T> List<PolicyTaskFuture<T>> invokeAll(Collection<? extends Callable<T>> tasks, PolicyTaskCallback[] callbacks) throws InterruptedException {
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        // Determine if we need a permit to run one or more of the tasks on the current thread, and if so, acquire it,
        int taskCount = tasks.size();
        boolean havePermit = false;
        boolean useCurrentThread = maxPolicy == MaxPolicy.loose || (havePermit = taskCount > 0 && maxConcurrencyConstraint.tryAcquire());

        List<PolicyTaskFutureImpl<T>> futures = new ArrayList<PolicyTaskFutureImpl<T>>(taskCount);
        try {
            // create futures in advance, which gives the callback an opportunity to reject
            int t = 0;
            for (Callable<T> task : tasks) {
                PolicyTaskCallback callback = callbacks == null ? null : callbacks[t++];
                PolicyExecutorImpl executor = callback == null ? this : (PolicyExecutorImpl) callback.getExecutor(this);
                long startTimeoutNS = callback == null ? startTimeout : callback.getStartTimeout(executor.startTimeout);
                futures.add(new PolicyTaskFutureImpl<T>(executor, task, callback, startTimeoutNS));
            }

            // enqueue tasks (except the last if we are able to run tasks on the current thread)
            int numToSubmitAsync = useCurrentThread ? taskCount - 1 : taskCount;
            t = 0;
            for (PolicyTaskFutureImpl<T> taskFuture : futures)
                if (t++ < numToSubmitAsync || taskFuture.executor != this) {
                    boolean enqueued;
                    if (useCurrentThread && taskFuture.executor == this)
                        enqueued = taskFuture.executor.enqueue(taskFuture, 0, true);
                    else
                        enqueued = taskFuture.executor.enqueue(taskFuture, taskFuture.executor.maxWaitForEnqueueNS.get(), null);

                    if (!enqueued) // must immediately return if ran on current thread and was interrupted
                        taskFuture.throwIfInterrupted();
                } else {
                    taskFuture.accept(true);
                }

            // run on current thread (by current executor) if possible
            if (useCurrentThread)
                for (t = numToSubmitAsync; t >= 0; t--) {
                    PolicyTaskFutureImpl<T> taskFuture = futures.get(t);
                    State currentState = taskFuture.executor.state.get();
                    if (taskFuture.executor == this && t == numToSubmitAsync) { // we intentionally avoided submitting the last task
                        if (currentState != State.ACTIVE)
                            throw new RejectedExecutionException(Tr.formatMessage(tc, "CWWKE1202.submit.after.shutdown", taskFuture.getIdentifier()));
                    } else if (taskFuture.executor == this && !taskFuture.isDone() && currentState.canStartTask && queue.remove(taskFuture)) {
                        maxQueueSizeConstraint.release();
                        taskFuture.nsQueueEnd = System.nanoTime();
                    } else {
                        if (trace && tc.isDebugEnabled())
                            Tr.debug(this, tc, "no longer in queue, or this executor is unable to run it", taskFuture);
                        continue; // other thread could already be processing the task, or it could be cancelled
                    }

                    if (taskFuture.nsStartBy == taskFuture.nsAcceptBegin - 1 // never times out, or
                        || taskFuture.nsStartBy - taskFuture.nsQueueEnd > 0) // have time remaining
                        runTask(taskFuture);
                    else { // timed out
                        taskFuture.nsRunEnd = taskFuture.nsQueueEnd;
                        RejectedExecutionException x = new RejectedExecutionException(new StartTimeoutException( //
                                        taskFuture.getIdentifier(), taskFuture.getTaskName(), //
                                        taskFuture.nsQueueEnd - taskFuture.nsAcceptBegin, //
                                        taskFuture.nsStartBy - taskFuture.nsAcceptBegin));
                        taskFuture.abort(false, x);
                        throw x;
                    }

                    // must immediately return if current thread is interrupted
                    taskFuture.throwIfInterrupted();
                }

            // wait for completion
            for (PolicyTaskFutureImpl<T> future : futures) {
                future.await();
                taskCount--;
            }
        } catch (RejectedExecutionException x) {
            if (trace && tc.isDebugEnabled())
                Tr.debug(this, tc, "rejected", x);
            if (x.getCause() instanceof InterruptedException)
                throw (InterruptedException) x.getCause();
            else
                throw x;
        } finally {
            if (havePermit)
                transferOrReleasePermit();

            if (taskCount != 0)
                for (Future<T> f : futures)
                    f.cancel(true);
        }

        @SuppressWarnings({ "rawtypes", "unchecked" })
        List<PolicyTaskFuture<T>> list = (List) futures;
        return list;
    }

    // Submit and run tasks within allotted interval and return list of completed (possibly canceled) tasks.
    // Because this method is timed, tasks will never run on the invoker's current thread.
    @FFDCIgnore(value = { RejectedExecutionException.class })
    @Override
    public <T> List<PolicyTaskFuture<T>> invokeAll(Collection<? extends Callable<T>> tasks, PolicyTaskCallback[] callbacks, long timeout,
                                                   TimeUnit unit) throws InterruptedException {
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        int taskCount = tasks.size();
        long stop = System.nanoTime() + unit.toNanos(timeout);
        long qWait, remaining;

        List<PolicyTaskFutureImpl<T>> futures = new ArrayList<PolicyTaskFutureImpl<T>>(taskCount);
        try {
            // create futures in advance, which gives the callback an opportunity to reject
            int t = 0;
            for (Callable<T> task : tasks) {
                PolicyTaskCallback callback = callbacks == null ? null : callbacks[t++];
                PolicyExecutorImpl executor = callback == null ? this : (PolicyExecutorImpl) callback.getExecutor(this);
                long startTimeoutNS = callback == null ? startTimeout : callback.getStartTimeout(executor.startTimeout);
                futures.add(new PolicyTaskFutureImpl<T>(executor, task, callback, startTimeoutNS));
            }

            // enqueue all tasks
            for (PolicyTaskFutureImpl<T> taskFuture : futures) {
                remaining = stop - System.nanoTime();
                if (remaining <= 0)
                    throw new RejectedExecutionException(Tr.formatMessage(tc, "CWWKE1204.unable.to.invoke", taskFuture.getIdentifier(),
                                                                          taskCount - futures.indexOf(taskFuture) + 1, taskCount, timeout, unit));
                qWait = taskFuture.executor.maxWaitForEnqueueNS.get();
                taskFuture.executor.enqueue(taskFuture,
                                            qWait < remaining ? qWait : remaining, // limit waiting to lesser of maxWaitForEnqueue and remaining time
                                            false); // never run on the current thread because it would prevent timeout
            }

            // wait for completion
            for (PolicyTaskFutureImpl<T> future : futures)
                if (future.await(stop - System.nanoTime(), TimeUnit.NANOSECONDS) > PolicyTaskFutureImpl.RUNNING)
                    taskCount--;
                else
                    break;
        } catch (RejectedExecutionException x) {
            if (trace && tc.isDebugEnabled())
                Tr.debug(this, tc, "rejected", x);
            if (x.getCause() instanceof InterruptedException)
                throw (InterruptedException) x.getCause();
            else
                throw x;
        } finally {
            if (taskCount != 0)
                for (Future<T> f : futures)
                    f.cancel(true);
        }

        @SuppressWarnings({ "rawtypes", "unchecked" })
        List<PolicyTaskFuture<T>> list = (List) futures;
        return list;
    }

    @Override
    @Trivial
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return invokeAny(tasks, null);
    }

    @Override
    @Trivial
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return invokeAny(tasks, null, timeout, unit);
    }

    // Submit a group of tasks, waiting for one to complete successfully or for all to have failed or been canceled.
    // Because this method is not timed, we allow an optimization where if only a single task is submitted it can run on the current thread.
    @Override
    @FFDCIgnore(value = { RejectedExecutionException.class })
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, PolicyTaskCallback[] callbacks) throws InterruptedException, ExecutionException {
        int taskCount = tasks.size();

        // Special case to run a single task on the current thread if we can acquire a permit, if a permit is required
        if (taskCount == 1) {
            boolean havePermit = false;
            if (maxPolicy == MaxPolicy.loose || (havePermit = maxConcurrencyConstraint.tryAcquire())) // use current thread
                try {
                    if (state.get() != State.ACTIVE)
                        throw new RejectedExecutionException(Tr.formatMessage(tc, "CWWKE1202.submit.after.shutdown", identifier));

                    // startTimeout does not apply because the task starts immediately
                    PolicyTaskFutureImpl<T> taskFuture = new PolicyTaskFutureImpl<T>(this, tasks.iterator().next(), callbacks == null ? null : callbacks[0], -1);
                    taskFuture.accept(true);
                    runTask(taskFuture);

                    // raise InterruptedException if current thread is interrupted
                    taskFuture.throwIfInterrupted();

                    return taskFuture.get();
                } finally {
                    if (havePermit)
                        transferOrReleasePermit();
                }
        } else if (taskCount == 0)
            throw new IllegalArgumentException();

        InvokeAnyLatch latch = new InvokeAnyLatch(taskCount);
        ArrayList<PolicyTaskFutureImpl<T>> futures = new ArrayList<PolicyTaskFutureImpl<T>>(taskCount);
        try {
            // create futures in advance, which gives the callback an opportunity to reject
            int t = 0;
            for (Callable<T> task : tasks) {
                PolicyTaskCallback callback = callbacks == null ? null : callbacks[t++];
                PolicyExecutorImpl executor = callback == null ? this : (PolicyExecutorImpl) callback.getExecutor(this);
                long startTimeoutNS = callback == null ? startTimeout : callback.getStartTimeout(executor.startTimeout);
                futures.add(new PolicyTaskFutureImpl<T>(executor, task, callback, startTimeoutNS, latch));
            }

            // enqueue all tasks
            for (PolicyTaskFutureImpl<T> taskFuture : futures) {
                // check if done before enqueuing more tasks
                if (latch.getCount() == 0)
                    break;

                taskFuture.executor.enqueue(taskFuture, taskFuture.executor.maxWaitForEnqueueNS.get(), false); // never run on the current thread because it would prevent timeout
            }

            // wait for completion
            return latch.await(-1, futures);
        } catch (RejectedExecutionException x) {
            if (x.getCause() instanceof InterruptedException) {
                throw (InterruptedException) x.getCause();
            } else
                throw x;
        } catch (TimeoutException x) {
            throw new RuntimeException(x); // should be unreachable with infinite timeout
        } finally {
            for (Future<?> f : futures)
                f.cancel(true);
        }
    }

    // Submit a group of tasks, waiting until the timeout is reached for one to complete successfully or for all to have failed or been canceled.
    // Because this method is timed, tasks will never run on the invoker's current thread.
    @Override
    @FFDCIgnore(value = { RejectedExecutionException.class })
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, PolicyTaskCallback[] callbacks, long timeout,
                           TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        int taskCount = tasks.size();
        long stop = System.nanoTime() + unit.toNanos(timeout);
        long qWait, remaining;

        if (taskCount == 0) // JavaDoc doesn't specify what to do in this case. Match behavior of untimed invokeAny.
            throw new IllegalArgumentException();

        InvokeAnyLatch latch = new InvokeAnyLatch(taskCount);
        ArrayList<PolicyTaskFutureImpl<T>> futures = new ArrayList<PolicyTaskFutureImpl<T>>(taskCount);
        try {
            // create futures in advance, which gives the callback an opportunity to reject
            int t = 0;
            for (Callable<T> task : tasks) {
                remaining = stop - System.nanoTime();
                PolicyTaskCallback callback = callbacks == null ? null : callbacks[t++];
                PolicyExecutorImpl executor = callback == null ? this : (PolicyExecutorImpl) callback.getExecutor(this);
                long startTimeoutNS = callback == null ? startTimeout : callback.getStartTimeout(executor.startTimeout);
                PolicyTaskFutureImpl<T> taskFuture = new PolicyTaskFutureImpl<T>(executor, task, callback, startTimeoutNS, latch);
                futures.add(taskFuture);
                if (remaining <= 0)
                    throw new RejectedExecutionException(Tr.formatMessage(tc, "CWWKE1204.unable.to.invoke", taskFuture.getIdentifier(),
                                                                          taskCount - futures.size(), taskCount, timeout, unit));
            }

            // enqueue all tasks
            for (PolicyTaskFutureImpl<T> taskFuture : futures) {
                remaining = stop - System.nanoTime();

                // check if done before enqueuing more tasks
                if (latch.getCount() == 0)
                    break;

                if (remaining <= 0)
                    throw new RejectedExecutionException(Tr.formatMessage(tc, "CWWKE1204.unable.to.invoke", taskFuture.getIdentifier(),
                                                                          taskCount - futures.size(), taskCount, timeout, unit));
                qWait = taskFuture.executor.maxWaitForEnqueueNS.get();
                taskFuture.executor.enqueue(taskFuture,
                                            qWait < remaining ? qWait : remaining, // limit waiting to lesser of maxWaitForEnqueue and remaining time
                                            false); // never run on the current thread because it would prevent timeout
            }

            // wait for completion
            remaining = stop - System.nanoTime();
            return latch.await(remaining < 0 ? 0 : remaining, futures);
        } catch (RejectedExecutionException x) {
            if (x.getCause() instanceof InterruptedException)
                throw (InterruptedException) x.getCause();
            else
                throw x;
        } finally {
            for (Future<?> f : futures)
                f.cancel(true);
        }
    }

    @Override
    public boolean isShutdown() {
        return state.get() != State.ACTIVE;
    }

    @Override
    public boolean isTerminated() {
        State currentState = state.get();
        switch (currentState) {
            case TERMINATED:
                return true;
            case ENQUEUE_STOPPED:
            case TASKS_CANCELING:
            case TASKS_CANCELED:
                // Transition to TERMINATED state if there are no tasks in the queue and we have no tasks on the global executor
                if (queue.isEmpty() && maxConcurrencyConstraint.tryAcquire(maxConcurrency)) {
                    State previous = state.getAndSet(State.TERMINATED);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                        Tr.event(this, tc, "state: " + previous + " --> TERMINATED");
                    return true;
                } else
                    return false;
            default:
                return false;
        }
    }

    @Override
    public PolicyExecutor maxConcurrency(int max) {
        if (max == -1)
            max = Integer.MAX_VALUE;
        else if (max < 1)
            throw new IllegalArgumentException(Integer.toString(max));

        synchronized (configLock) {
            if (max < expedite)
                throw new IllegalArgumentException("maxConcurrency: " + max + " < expedite: " + expedite);

            if (state.get() != State.ACTIVE)
                throw new IllegalStateException(Tr.formatMessage(tc, "CWWKE1203.config.update.after.shutdown", "maxConcurrency", identifier));

            int increase = max - maxConcurrency;
            if (increase > 0)
                maxConcurrencyConstraint.release(increase);
            else if (increase < 0)
                maxConcurrencyConstraint.reducePermits(-increase);
            maxConcurrency = max;
        }

        while (withheldConcurrency.get() > 0 && maxConcurrencyConstraint.tryAcquire()) {
            decrementWithheldConcurrency();
            enqueueGlobal(new GlobalPoolTask());
        }

        return this;
    }

    @Override
    public PolicyExecutor maxPolicy(MaxPolicy policy) {
        if (policy == null)
            throw new NullPointerException();

        if (state.get() != State.ACTIVE)
            throw new IllegalStateException(Tr.formatMessage(tc, "CWWKE1203.config.update.after.shutdown", "maxPolicy", identifier));

        maxPolicy = policy;

        return this;
    }

    @Override
    public PolicyExecutor maxQueueSize(int max) {
        if (max == -1)
            max = Integer.MAX_VALUE;
        else if (max < 1)
            throw new IllegalArgumentException(Integer.toString(max));

        int increase;
        synchronized (configLock) {
            if (state.get() != State.ACTIVE)
                throw new IllegalStateException(Tr.formatMessage(tc, "CWWKE1203.config.update.after.shutdown", "maxQueueSize", identifier));

            increase = max - maxQueueSize;
            if (increase > 0)
                maxQueueSizeConstraint.release(increase);
            else if (increase < 0)
                maxQueueSizeConstraint.reducePermits(-increase);
            maxQueueSize = max;
        }

        if (increase < 0) {
            Callback callback = cbQueueSize.get();
            if (callback != null
                && maxQueueSizeConstraint.availablePermits() < callback.threshold
                && cbQueueSize.compareAndSet(callback, null)) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(this, tc, "callback: queue capacity < " + callback.threshold, callback.runnable);
                globalExecutor.submit(callback.runnable);
            }
        }

        return this;
    }

    @Override
    public PolicyExecutor maxWaitForEnqueue(long ms) {
        if (ms < 0)
            throw new IllegalArgumentException(Long.toString(ms));

        for (long current = maxWaitForEnqueueNS.get(); current != -1; current = maxWaitForEnqueueNS.get())
            if (maxWaitForEnqueueNS.compareAndSet(current, TimeUnit.MILLISECONDS.toNanos(ms)))
                return this;

        throw new IllegalStateException(Tr.formatMessage(tc, "CWWKE1203.config.update.after.shutdown", "maxWaitForEnqueue", identifier));
    }

    @Override
    public int queueCapacityRemaining() {
        int capacity = maxQueueSizeConstraint.availablePermits();
        return capacity < 0 ? 0 : capacity;
    }

    @Override
    public Runnable registerConcurrencyCallback(int max, Runnable runnable) {
        if (state.get() != State.ACTIVE)
            throw new IllegalStateException(state.toString());

        Callback callback = new Callback(max, runnable);
        Callback previous = cbConcurrency.getAndSet(callback);
        if (runnable != null
            && runningCount.get() > max
            && cbConcurrency.compareAndSet(callback, null)) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(this, tc, "callback: concurrency > " + max, runnable);
            globalExecutor.submit(runnable);
        }
        return previous == null ? null : previous.runnable;
    }

    @Override
    public Runnable registerLateStartCallback(long maxDelay, TimeUnit unit, Runnable runnable) {
        long ns = unit.toNanos(maxDelay);
        if (ns == Long.MAX_VALUE) // overflow or max value which can never be exceeded
            throw new IllegalArgumentException(maxDelay + " " + unit);

        if (state.get() != State.ACTIVE)
            throw new IllegalStateException(state.toString());

        Callback callback = new Callback(ns, runnable);
        Callback previous = cbLateStart.getAndSet(callback);
        return previous == null ? null : previous.runnable;
    }

    @Override
    public Runnable registerQueueSizeCallback(int minAvailable, Runnable runnable) {
        if (state.get() != State.ACTIVE)
            throw new IllegalStateException(state.toString());

        Callback callback = new Callback(minAvailable, runnable);
        Callback previous = cbQueueSize.getAndSet(callback);
        if (runnable != null
            && maxQueueSizeConstraint.availablePermits() < minAvailable
            && cbQueueSize.compareAndSet(callback, null)) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(this, tc, "callback: queue capacity < " + minAvailable, runnable);
            globalExecutor.submit(runnable);
        }
        return previous == null ? null : previous.runnable;
    }

    @Override
    public PolicyExecutor runIfQueueFull(boolean runIfFull) {
        if (state.get() != State.ACTIVE)
            throw new IllegalStateException(Tr.formatMessage(tc, "CWWKE1203.config.update.after.shutdown", "runIfQueueFull", identifier));

        runIfQueueFull = runIfFull;

        return this;
    }

    /**
     * Invoked by the policy executor thread to run a task.
     *
     * @param future the future for the task.
     * @return Exception that occurred while running the task. Otherwise null.
     */
    void runTask(PolicyTaskFutureImpl<?> future) {
        running.add(future); // intentionally done before checking state to avoid missing cancels on shutdownNow
        int runCount = runningCount.incrementAndGet();
        try {
            Callback callback = cbLateStart.get();
            if (callback != null) {
                long delay = future.nsQueueEnd - future.nsAcceptBegin;
                if (delay > callback.threshold
                    && cbLateStart.compareAndSet(callback, null)) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(this, tc, "callback: late start " + delay + "ns > " + callback.threshold + "ns", callback.runnable);
                    globalExecutor.submit(callback.runnable);
                }
            }

            callback = cbConcurrency.get();
            if (callback != null
                && runCount > callback.threshold
                && cbConcurrency.compareAndSet(callback, null)) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(this, tc, "callback: concurrency > " + callback.threshold, callback.runnable);
                globalExecutor.submit(callback.runnable);
            }

            if (state.get().canStartTask) {
                future.run();
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(this, tc, "Cancel task due to policy executor state " + state);
                future.nsRunEnd = System.nanoTime();
                future.cancel(false);
                if (future.callback != null)
                    future.callback.onEnd(future.task, future, null, true, 0, null); // aborted, queued task will never run
            }
        } catch (Error x) {
            // auto FFDC
        } catch (RuntimeException x) {
            // auto FFDC
        } finally {
            runningCount.decrementAndGet();
            running.remove(future);
        }
    }

    @Override
    public void shutdown() {
        // Permanently update our configuration such that no more task submits are accepted
        if (state.compareAndSet(State.ACTIVE, State.ENQUEUE_STOPPING)) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                Tr.event(this, tc, "state: ACTIVE --> ENQUEUE_STOPPING");

            // unregister callbacks
            cbConcurrency.set(null);
            cbLateStart.set(null);
            cbQueueSize.set(null);

            maxWaitForEnqueueNS.set(-1); // make attempted task submissions fail immediately

            synchronized (configLock) {
                maxQueueSize = 0;
                maxQueueSizeConstraint.drainPermits();
                maxQueueSizeConstraint.reducePermits(Integer.MAX_VALUE);
            }

            if (state.compareAndSet(State.ENQUEUE_STOPPING, State.ENQUEUE_STOPPED))
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                    Tr.event(this, tc, "state: ENQUEUE_STOPPING --> ENQUEUE_STOPPED");

            policyExecutors.remove(identifier); // remove tracking of this instance and allow identifier to be reused

            shutdownLatch.countDown();
        } else
            while (state.get() == State.ENQUEUE_STOPPING)
                try { // Await completion of other thread that concurrently invokes shutdown.
                    shutdownLatch.await();
                } catch (InterruptedException x) {
                    throw new RuntimeException(x);
                }
    }

    @Override
    public List<Runnable> shutdownNow() {
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        shutdown();

        LinkedList<Runnable> queuedTasks = new LinkedList<Runnable>();

        if (state.compareAndSet(State.ENQUEUE_STOPPED, State.TASKS_CANCELING)) {
            if (trace && tc.isEventEnabled())
                Tr.event(this, tc, "state: ENQUEUE_STOPPED --> TASKS_CANCELING");

            // Remove and cancel all queued tasks. The maxQueueSizeConstraint should prevent queueing more,
            // apart from a timing window where a task is being scheduled during shutdown, which is
            // covered by checking the state before returning from submit.
            for (PolicyTaskFutureImpl<?> f = queue.poll(); f != null; f = queue.poll()) {
                if (f.cancel(false)) {
                    if (f.task instanceof Runnable)
                        queuedTasks.add((Runnable) f.task);
                    else
                        queuedTasks.add(new RunnableFromCallable((Callable<?>) f.task));
                }
            }

            // Cancel tasks that are running
            for (Iterator<PolicyTaskFutureImpl<?>> it = running.iterator(); it.hasNext();)
                it.next().cancel(true);

            if (state.compareAndSet(State.TASKS_CANCELING, State.TASKS_CANCELED))
                if (trace && tc.isEventEnabled())
                    Tr.event(this, tc, "state: TASKS_CANCELING --> TASKS_CANCELED");

            shutdownNowLatch.countDown();
        } else
            while (state.get() == State.TASKS_CANCELING)
                try { // Await completion of other thread that concurrently invokes shutdownNow.
                    shutdownNowLatch.await();
                } catch (InterruptedException x) {
                    throw new RuntimeException(x);
                }

        return queuedTasks;
    }

    @Override
    public PolicyExecutor startTimeout(long ms) {
        if (ms < -1 || ms > TimeUnit.NANOSECONDS.toMillis(Long.MAX_VALUE))
            throw new IllegalArgumentException(Long.toString(ms));

        if (state.get() != State.ACTIVE)
            throw new IllegalStateException(Tr.formatMessage(tc, "CWWKE1203.config.update.after.shutdown", "startTimeout", identifier));

        startTimeout = ms == -1 ? -1 /* disabled */ : TimeUnit.MILLISECONDS.toNanos(ms);

        return this;
    }

    @Override
    public PolicyTaskFuture<Void> submit(AtomicReference<Future<?>> completableFutureRef, Runnable task) {
        PolicyTaskFutureImpl<Void> policyTaskFuture = new PolicyTaskFutureImpl<Void>(this, task, completableFutureRef, startTimeout);
        enqueue(policyTaskFuture, maxWaitForEnqueueNS.get(), null);
        return policyTaskFuture;
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        PolicyTaskFutureImpl<T> policyTaskFuture = new PolicyTaskFutureImpl<T>(this, task, null, startTimeout);
        enqueue(policyTaskFuture, maxWaitForEnqueueNS.get(), null);
        return policyTaskFuture;
    }

    @Override
    public <T> PolicyTaskFuture<T> submit(Callable<T> task, PolicyTaskCallback callback) {
        long startTimeoutNS = callback == null ? startTimeout : callback.getStartTimeout(startTimeout);
        PolicyTaskFutureImpl<T> policyTaskFuture = new PolicyTaskFutureImpl<T>(this, task, callback, startTimeoutNS);
        enqueue(policyTaskFuture, maxWaitForEnqueueNS.get(), null);
        return policyTaskFuture;
    }

    @Override
    public Future<?> submit(Runnable task) {
        PolicyTaskFutureImpl<?> policyTaskFuture = new PolicyTaskFutureImpl<Void>(this, task, null, null, startTimeout);
        enqueue(policyTaskFuture, maxWaitForEnqueueNS.get(), null);
        return policyTaskFuture;
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        PolicyTaskFutureImpl<T> policyTaskFuture = new PolicyTaskFutureImpl<T>(this, task, result, null, startTimeout);
        enqueue(policyTaskFuture, maxWaitForEnqueueNS.get(), null);
        return policyTaskFuture;
    }

    @Override
    public <T> PolicyTaskFuture<T> submit(Runnable task, T result, PolicyTaskCallback callback) {
        long startTimeoutNS = callback == null ? startTimeout : callback.getStartTimeout(startTimeout);
        PolicyTaskFutureImpl<T> policyTaskFuture = new PolicyTaskFutureImpl<T>(this, task, result, callback, startTimeoutNS);
        enqueue(policyTaskFuture, maxWaitForEnqueueNS.get(), null);
        return policyTaskFuture;
    }

    /**
     * Releases a permit against maxConcurrency or transfers it to a worker task that runs on the global thread pool.
     */
    @Trivial
    private void transferOrReleasePermit() {
        maxConcurrencyConstraint.release();

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "expedites/maxConcurrency available",
                     expeditesAvailable, maxConcurrencyConstraint.availablePermits());

        // The permit might be needed to run tasks on the global executor,
        if (!queue.isEmpty() && withheldConcurrency.get() > 0 && maxConcurrencyConstraint.tryAcquire()) {
            decrementWithheldConcurrency();
            if (acquireExpedite() > 0)
                expediteGlobal(new GlobalPoolTask());
            else
                enqueueGlobal(new GlobalPoolTask());
        }
    }

    // Used by OSGi components (concurrencyPolicy) to initialize and update configuration.
    @Override
    public void updateConfig(Map<String, Object> props) {
        final long maxMS = TimeUnit.NANOSECONDS.toMillis(Long.MAX_VALUE);

        Object v;
        int u_expedite = (Integer) props.get("expedite");
        int u_max = null == (v = props.get("max")) ? Integer.MAX_VALUE : (Integer) v;
        MaxPolicy u_maxPolicy = MaxPolicy.valueOf((String) props.get("maxPolicy"));
        int u_maxQueueSize = null == (v = props.get("maxQueueSize")) ? Integer.MAX_VALUE : (Integer) v;
        long u_maxWaitForEnqueue = (Long) props.get("maxWaitForEnqueue");
        boolean u_runIfQueueFull = (Boolean) props.get("runIfQueueFull");
        long u_startTimeout = null == (v = props.get("startTimeout")) ? -1l : (Long) v;

        // Validation that cannot be performed by metatype:
        if (u_expedite > u_max)
            throw new IllegalArgumentException("expedite: " + u_expedite + " > max: " + u_max);

        if (u_maxWaitForEnqueue < 0 || u_maxWaitForEnqueue > maxMS)
            throw new IllegalArgumentException("maxWaitForEnqueue: " + u_maxWaitForEnqueue);

        if (u_startTimeout < -1 || u_startTimeout > maxMS)
            throw new IllegalArgumentException("startTimeout: " + u_startTimeout);

        for (long current = maxWaitForEnqueueNS.get(); current != -1; current = maxWaitForEnqueueNS.get())
            if (maxWaitForEnqueueNS.compareAndSet(current, TimeUnit.MILLISECONDS.toNanos(u_maxWaitForEnqueue)))
                break;

        maxPolicy = u_maxPolicy;
        runIfQueueFull = u_runIfQueueFull;
        startTimeout = u_startTimeout == -1 ? -1 : TimeUnit.MILLISECONDS.toNanos(u_startTimeout);

        int a, queueCapacityAdded;
        synchronized (configLock) {
            a = expeditesAvailable.addAndGet(u_expedite - expedite);
            expedite = u_expedite;

            int increase = u_max - maxConcurrency;
            if (increase > 0)
                maxConcurrencyConstraint.release(increase);
            else if (increase < 0)
                maxConcurrencyConstraint.reducePermits(-increase);
            maxConcurrency = u_max;

            queueCapacityAdded = u_maxQueueSize - maxQueueSize;
            if (queueCapacityAdded > 0)
                maxQueueSizeConstraint.release(queueCapacityAdded);
            else if (queueCapacityAdded < 0)
                maxQueueSizeConstraint.reducePermits(-queueCapacityAdded);
            maxQueueSize = u_maxQueueSize;
        }

        if (queueCapacityAdded < 0) {
            Callback callback = cbQueueSize.get();
            if (callback != null
                && maxQueueSizeConstraint.availablePermits() < callback.threshold
                && cbQueueSize.compareAndSet(callback, null)) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(this, tc, "callback: queue capacity < " + callback.threshold, callback.runnable);
                globalExecutor.submit(callback.runnable);
            }
        }

        // Expedite as many of the remaining tasks as the available maxConcurrency permits and increased expedites
        // will allow. We are choosing not to revoke GlobalPoolTasks that have already been enqueued as non-expedited,
        // which means we do not guarantee an increase in expedites to fully go into effect immediately.
        // Any reduction to expedites is handled gradually, as expedited GlobalPoolTasks complete.
        while (withheldConcurrency.get() > 0 && maxConcurrencyConstraint.tryAcquire()) {
            decrementWithheldConcurrency();
            if (a-- > 0 && acquireExpedite() > 0)
                expediteGlobal(new GlobalPoolTask());
            else
                enqueueGlobal(new GlobalPoolTask());
        }
    }

    public void introspect(PrintWriter out) {
        final String INDENT = "  ";
        final String DOUBLEINDENT = INDENT + INDENT;
        out.println(identifier);
        out.println(INDENT + "expedite = " + expedite);
        out.println(INDENT + "maxConcurrency = " + maxConcurrency + " (" + maxPolicy + ')');
        out.println(INDENT + "maxQueueSize = " + maxQueueSize);
        out.println(INDENT + "maxWaitForEnqueue = " + TimeUnit.NANOSECONDS.toMillis(maxWaitForEnqueueNS.get()) + " ms");
        out.println(INDENT + "runIfQueueFull = " + runIfQueueFull);
        out.println(INDENT + "startTimeout = " + (startTimeout == -1 ? "None" : TimeUnit.NANOSECONDS.toMillis(startTimeout) + " ms"));
        int numRunningThreads, numRunningPrioritizedThreads;
        synchronized (configLock) {
            numRunningThreads = maxConcurrency - maxConcurrencyConstraint.availablePermits();
            numRunningPrioritizedThreads = expedite - expeditesAvailable.get();
        }
        out.println(INDENT + "Total Enqueued to Global Executor = " + numRunningThreads + " (" + numRunningPrioritizedThreads + " expedited)");
        out.println(INDENT + "withheldConcurrency = " + withheldConcurrency.get());
        out.println(INDENT + "Remaining Queue Capacity = " + maxQueueSizeConstraint.availablePermits());
        out.println(INDENT + "state = " + state.toString());
        out.println(INDENT + "concurrency callback = " + cbConcurrency.get());
        out.println(INDENT + "late start callback = " + cbLateStart.get());
        out.println(INDENT + "queue capacity callback = " + cbQueueSize.get());
        out.println(INDENT + "Running Task Count = " + runningCount);
        out.println(INDENT + "Running Task Futures:");
        if (running.isEmpty()) {
            out.println(DOUBLEINDENT + "None");
        } else {
            for (PolicyTaskFutureImpl<?> task : running) {
                out.println(DOUBLEINDENT + task.toString());
            }
        }
        int counter = 50;
        out.println(INDENT + "Queued Task Futures (up to first " + counter + "):");
        if (queue.isEmpty()) {
            out.println(DOUBLEINDENT + "None");
        } else {
            for (PolicyTaskFutureImpl<?> task : queue) {
                if (counter-- > 0)
                    out.println(DOUBLEINDENT + task.toString());
                else
                    break;
            }
        }
        out.println();
    }
}
