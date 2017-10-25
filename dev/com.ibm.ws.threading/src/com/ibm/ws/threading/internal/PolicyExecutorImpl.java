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
import java.util.concurrent.locks.LockSupport;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.threading.PolicyExecutor;
import com.ibm.ws.threading.PolicyTaskCallback;
import com.ibm.ws.threading.internal.PolicyTaskFutureImpl.InvokeAnyCompletionTracker;

/**
 * Policy executors are backed by the Liberty global thread pool,
 * but allow concurrency constraints and various queue attributes
 * to be controlled independently of the global thread pool.
 */
public class PolicyExecutorImpl implements PolicyExecutor {
    private static final TraceComponent tc = Tr.register(PolicyExecutorImpl.class);

    /**
     * Use this lock to make a consistent update to both coreConcurrency and coreConcurrencyAvailable,
     * maxConcurrency and maxConcurrencyConstraint, and to maxQueueSize and maxQueueSizeConstraint.
     */
    private final Integer configLock = new Integer(0); // new instance required to avoid sharing

    private int coreConcurrency;

    private final AtomicInteger coreConcurrencyAvailable = new AtomicInteger();

    ExecutorServiceImpl globalExecutor;

    String identifier;

    private int maxConcurrency;

    private final ReduceableSemaphore maxConcurrencyConstraint = new ReduceableSemaphore(0, false);

    private int maxQueueSize;

    final ReduceableSemaphore maxQueueSizeConstraint = new ReduceableSemaphore(0, false);

    private final AtomicLong maxWaitForEnqueueNS = new AtomicLong();

    /**
     * This list is supplied to each instance that is programmatically created by PolicyExecutorProvider
     * so that each instance can manage its own membership per its life cycle.
     * The list is null if declarative services created this instance based on server configuration.
     */
    private final ConcurrentHashMap<String, PolicyExecutorImpl> providerCreated;

    final ConcurrentLinkedQueue<PolicyTaskFutureImpl<?>> queue = new ConcurrentLinkedQueue<PolicyTaskFutureImpl<?>>();

    private final AtomicReference<QueueFullAction> queueFullAction = new AtomicReference<QueueFullAction>();

    /**
     * Tasks that are running on policy executor threads.
     * This is only populated & used for policy executors that are programmatically created,
     * because it is needed only for the life cycle methods which are unavailable to
     * server-configured policy executors.
     */
    private final Set<PolicyTaskFutureImpl<?>> running = Collections.newSetFromMap(new ConcurrentHashMap<PolicyTaskFutureImpl<?>, Boolean>());

    /**
     * Latch that awaits the shutdown method progressing to ENQUEUE_STOPPED state.
     */
    private final CountDownLatch shutdownLatch = new CountDownLatch(1);

    /**
     * Latch that awaits the shutdownNow method progressing to TASKS_CANCELED state.
     */
    private final CountDownLatch shutdownNowLatch = new CountDownLatch(1);

    /**
     * Policy executor state, which transitions in one direction only. See constants for possible states.
     */
    private final AtomicReference<State> state = new AtomicReference<State>(State.ACTIVE);

    /**
     * Counter of tasks for which we didn't submit a PollingTask in order to honor maxConcurrency.
     * In deciding whether a PollingTask should be resubmitted, this counter can be decremented (if positive).
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
     * Polling tasks run on the global thread pool.
     * Their role is to run tasks that are queued up on the policy executor.
     */
    private class PollingTask implements QueueItem, Runnable {
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
            while ((next = (canRun = state.get().canStartTask) ? queue.poll() : null) != null && next.isCancelled()) {
                maxQueueSizeConstraint.release();
                if (next.callback != null)
                    next.callback.onEnd(next.task, next, null, true, 0, null); // aborted, queued task will never run
            }

            if (next != null) {
                maxQueueSizeConstraint.release();
                runTask(next);
            }

            // Release permits against core/maxConcurrency
            if (expedite)
                coreConcurrencyAvailable.incrementAndGet();
            maxConcurrencyConstraint.release();

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(PolicyExecutorImpl.this, tc, "core/maxConcurrency available",
                         coreConcurrencyAvailable, maxConcurrencyConstraint.availablePermits(), canRun);

            // Avoid reschedule if we are in a state that disallows starting tasks or if no withheld tasks remain
            if (canRun && withheldConcurrency.get() > 0 && maxConcurrencyConstraint.tryAcquire()) {
                decrementWithheldConcurrency();
                if (acquireCoreConcurrency() > 0)
                    expediteGlobal(PollingTask.this);
                else
                    enqueueGlobal(PollingTask.this);
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
     * Constructor for declarative services.
     * The majority of initialization logic should be performed in the activate method, not here.
     */
    public PolicyExecutorImpl() {
        providerCreated = null;
    }

    /**
     * This constructor is used by PolicyExecutorProvider.
     *
     * @param globalExecutor the Liberty global executor, which was obtained by the PolicyExecutorProvider via declarative services.
     * @param identifier unique identifier for this instance, to be used for monitoring and problem determination.
     *            Note: The prefix, PolicyExecutorProvider-, is prepended to the identifier.
     * @param providerCreatedInstances list of instances created by the PolicyExecutorProvider.
     *            Each instance is responsible for adding and removing itself from the list per its life cycle.
     * @throws IllegalStateException if an instance with the specified unique identifier already exists and has not been shut down.
     * @throws NullPointerException if the specified identifier is null
     */
    public PolicyExecutorImpl(ExecutorServiceImpl globalExecutor, String identifier, ConcurrentHashMap<String, PolicyExecutorImpl> providerCreatedInstances) {
        this.globalExecutor = globalExecutor;
        this.identifier = "PolicyExecutorProvider-" + identifier;
        this.providerCreated = providerCreatedInstances;

        maxConcurrencyConstraint.release(maxConcurrency = Integer.MAX_VALUE);
        maxQueueSizeConstraint.release(maxQueueSize = Integer.MAX_VALUE);

        if (providerCreated.putIfAbsent(this.identifier, this) != null)
            throw new IllegalStateException(this.identifier);
    }

    /**
     * Attempt to acquire a core concurrency permit, which involves decrementing the available core concurrency.
     * Only allow decrement of a positive value, and otherwise indicate there is no available core concurrency.
     *
     * @return amount of available core concurrency at the time we acquired it. 0 if none remains and we did not get a permit.
     */
    private int acquireCoreConcurrency() {
        int cca;
        while ((cca = coreConcurrencyAvailable.get()) > 0 && !coreConcurrencyAvailable.compareAndSet(cca, cca - 1));
        return cca; // returning the value rather than true/false will enable better debug
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        if (providerCreated == null)
            throw new UnsupportedOperationException();

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
                    // Transition to TERMINATED state if no tasks in the queue and no polling tasks on global executor.
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
    public PolicyExecutor coreConcurrency(int core) {
        if (providerCreated == null)
            throw new UnsupportedOperationException();

        if (core == -1)
            core = Integer.MAX_VALUE;
        else if (core < 0)
            throw new IllegalArgumentException(Integer.toString(core));

        int cca;
        synchronized (configLock) {
            if (core > maxConcurrency)
                throw new IllegalArgumentException(Integer.toString(core));

            if (state.get() != State.ACTIVE)
                throw new IllegalStateException(Tr.formatMessage(tc, "CWWKE1203.config.update.after.shutdown", "coreConcurrency", identifier));

            cca = coreConcurrencyAvailable.addAndGet(core - coreConcurrency);
            coreConcurrency = core;
        }

        // Expedite as many of the remaining tasks as the available maxConcurrency permits and increased coreConcurrency
        // will allow. We are choosing not to revoke PollingTasks that have already been enqueued as non-expedited,
        // which means we do not guarantee an increased coreConcurrency to fully go into effect immediately.
        // Any reduction to coreConcurrency is handled gradually, as expedited PollingTasks complete.
        if (cca > 0) {
            while (cca-- > 0 && withheldConcurrency.get() > 0 && maxConcurrencyConstraint.tryAcquire())
                if (acquireCoreConcurrency() > 0) {
                    decrementWithheldConcurrency();
                    expediteGlobal(new PollingTask());
                } else {
                    maxConcurrencyConstraint.release();
                    break;
                }
        }

        return this;
    }

    /**
     * Decrement the counter of withheld concurrency only if positive.
     * This method should only ever be invoked if the caller is about to enqueue a PollingTask to the global executor.
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
     * As needed, ensure that polling tasks are submitted to the global executor to process
     * the queued up tasks.
     *
     * @param policyTaskFuture submitted task and its Future.
     * @param wait amount of time to wait for a queue position.
     * @param callerRunsOverride indicates if a task should always or may never run on the current thread
     *            if no queue positions are available. A value of null means the queueFullAction will determine.
     * @return true if the task was enqueued for later execution by the global thread pool.
     *         If the task instead ran on the current thread, then returns false.
     * @throws RejectedExecutionException if the task is rejected rather than being queued.
     *             If this method runs the task on the current thread and the task raises InterruptedException,
     *             the InterruptedException is chained to the RejectedExecutionException.
     */
    @FFDCIgnore(value = { InterruptedException.class, RejectedExecutionException.class }) // these are raised directly to invoker, who decides how to handle
    private boolean enqueue(PolicyTaskFutureImpl<?> policyTaskFuture, long wait, Boolean callerRunsOverride) {
        boolean enqueued;
        try {
            if (wait <= 0 ? maxQueueSizeConstraint.tryAcquire() : maxQueueSizeConstraint.tryAcquire(wait, TimeUnit.NANOSECONDS)) {
                enqueued = queue.offer(policyTaskFuture);
                policyTaskFuture.accept();

                int w = withheldConcurrency.incrementAndGet();
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(this, tc, "withheld concurrency --> " + w);
                if (maxConcurrencyConstraint.tryAcquire()) {
                    decrementWithheldConcurrency();
                    if (acquireCoreConcurrency() > 0)
                        expediteGlobal(new PollingTask());
                    else
                        enqueueGlobal(new PollingTask());
                }

                // Check if shutdown occurred since acquiring the permit to enqueue, and if so, try to remove the queued task
                if (state.get() != State.ACTIVE && queue.remove(policyTaskFuture))
                    throw new RejectedExecutionException(Tr.formatMessage(tc, "CWWKE1202.submit.after.shutdown", identifier));
            } else if (state.get() == State.ACTIVE) {
                QueueFullAction action = Boolean.TRUE.equals(callerRunsOverride) ? QueueFullAction.CallerRuns : queueFullAction.get();
                if (Boolean.FALSE.equals(callerRunsOverride) && (action == QueueFullAction.CallerRuns || action == QueueFullAction.CallerRunsIfSameExecutor))
                    action = QueueFullAction.Abort;
                if (action == QueueFullAction.Abort)
                    throw new RejectedExecutionException(Tr.formatMessage(tc, "CWWKE1201.queue.full.abort", identifier, maxQueueSize, wait));
                else if (action == QueueFullAction.CallerRuns) {
                    policyTaskFuture.accept();
                    runTask(policyTaskFuture);
                    enqueued = false;
                } else
                    throw new UnsupportedOperationException("queueFullAction=" + action); // TODO DiscardOldest, CallerRunsIfSameExecutor, and null (which defaults based on maxConcurrency)
            } else
                throw new RejectedExecutionException(Tr.formatMessage(tc, "CWWKE1202.submit.after.shutdown", identifier));
        } catch (InterruptedException x) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(this, tc, "enqueue", x);
            if (policyTaskFuture.abort(x) && policyTaskFuture.callback != null)
                policyTaskFuture.callback.onEnd(policyTaskFuture.task, policyTaskFuture, null, true, 0, x);
            throw new RejectedExecutionException(x);
        } catch (RejectedExecutionException x) { // redundant with RuntimeException code path, but added to allow FFDCIgnore
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(this, tc, "enqueue", x);
            if (policyTaskFuture.abort(x) && policyTaskFuture.callback != null)
                policyTaskFuture.callback.onEnd(policyTaskFuture.task, policyTaskFuture, null, true, 0, x);
            throw x;
        } catch (RuntimeException x) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(this, tc, "enqueue", x);
            if (policyTaskFuture.abort(x) && policyTaskFuture.callback != null)
                policyTaskFuture.callback.onEnd(policyTaskFuture.task, policyTaskFuture, null, true, 0, x);
            throw x;
        } catch (Error x) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(this, tc, "enqueue", x);
            if (policyTaskFuture.abort(x) && policyTaskFuture.callback != null)
                policyTaskFuture.callback.onEnd(policyTaskFuture.task, policyTaskFuture, null, true, 0, x);
            throw x;
        }
        return enqueued;
    }

    /**
     * Queue a polling task to the global executor.
     * Prereq: maxConcurrencyConstraint permit must already be acquired to reflect the task being queued to global.
     * If unsuccessful in queuing to global, this method releases the maxConcurrencyConstraint permit.
     *
     * @param pollingTask task that can execute tasks that are queued to the policy executor.
     */
    void enqueueGlobal(PollingTask pollingTask) {
        pollingTask.expedite = false;
        boolean submitted = false;
        try {
            globalExecutor.executeWithoutInterceptors(pollingTask);
            submitted = true;
        } finally {
            if (!submitted) {
                maxConcurrencyConstraint.release();

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(this, tc, "core/maxConcurrency available", coreConcurrencyAvailable, maxConcurrencyConstraint.availablePermits());
            }
        }
    }

    @Override
    public void execute(Runnable command) {
        enqueue(new PolicyTaskFutureImpl<Void>(this, command, null, null), maxWaitForEnqueueNS.get(), null);
    }

    /**
     * Expedite a polling task to the global executor.
     * Prereq: maxConcurrencyConstraint permit must already be acquired and
     * coreConcurrencyAvailable must already be decremented to reflect the task being expedited to global.
     * If unsuccessful in expediting to global, this method releases the maxConcurrencyConstraint permit
     * and increments coreConcurrencyAvailable.
     *
     * @param pollingTask task that can execute tasks that are queued to the policy executor.
     */
    void expediteGlobal(PollingTask pollingTask) {
        pollingTask.expedite = true;
        boolean submitted = false;
        try {
            globalExecutor.executeWithoutInterceptors(pollingTask);
            submitted = true;
        } finally {
            if (!submitted) {
                int cca = coreConcurrencyAvailable.incrementAndGet();
                maxConcurrencyConstraint.release();

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(this, tc, "core/maxConcurrency available", cca, maxConcurrencyConstraint.availablePermits());
            }
        }
    }

    @Override
    @Trivial
    public final <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return invokeAll(tasks, null);
    }

    @Override
    @Trivial
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        return invokeAll(tasks, null, timeout, unit);
    }

    // Submit and run tasks and return list of completed (possibly canceled) tasks.
    // Because this method is not timed, tasks can run on the current thread if queueFullAction is CallerRuns or a permit is available.
    @FFDCIgnore(value = { RejectedExecutionException.class })
    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, PolicyTaskCallback[] callbacks) throws InterruptedException {
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        // Determine if we need a permit to run one or more of the tasks on the current thread, and if so, acquire it,
        int taskCount = tasks.size();
        boolean useCurrentThread = queueFullAction.get() == QueueFullAction.CallerRuns;
        boolean havePermit = !useCurrentThread && (useCurrentThread = taskCount > 0 && maxConcurrencyConstraint.tryAcquire());

        List<PolicyTaskFutureImpl<T>> futures = new ArrayList<PolicyTaskFutureImpl<T>>(taskCount);
        try {
            // create futures in advance, which gives the callback an opportunity to reject
            int t = 0;
            for (Callable<T> task : tasks)
                futures.add(new PolicyTaskFutureImpl<T>(this, task, callbacks == null ? null : callbacks[t++]));

            // enqueue tasks (except the last if we are able to run tasks on the current thread)
            int numToSubmitAsync = useCurrentThread ? taskCount - 1 : taskCount;
            t = 0;
            for (PolicyTaskFutureImpl<T> taskFuture : futures)
                if (t++ < numToSubmitAsync) {
                    boolean enqueued;
                    if (useCurrentThread)
                        enqueued = enqueue(taskFuture, 0, true);
                    else
                        enqueued = enqueue(taskFuture, maxWaitForEnqueueNS.get(), null);

                    if (!enqueued) // must immediately return if ran on current thread and was interrupted
                        taskFuture.throwIfInterrupted();
                } else {
                    taskFuture.accept();
                }

            // run on current thread if possible
            if (useCurrentThread)
                for (t = numToSubmitAsync; t >= 0; t--) {
                    PolicyTaskFutureImpl<T> taskFuture = futures.get(t);
                    State currentState = state.get();
                    if (t == numToSubmitAsync) { // we intentionally avoided submitting the last task
                        if (currentState != State.ACTIVE)
                            throw new RejectedExecutionException(Tr.formatMessage(tc, "CWWKE1202.submit.after.shutdown", identifier));
                    } else if (!taskFuture.isDone() && currentState.canStartTask && queue.remove(taskFuture)) {
                        maxQueueSizeConstraint.release();
                    } else {
                        if (trace && tc.isDebugEnabled())
                            Tr.debug(this, tc, "no longer in queue", taskFuture);
                        continue; // other thread could already be processing the task, or it could be cancelled
                    }

                    runTask(taskFuture);

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
            // Clear interrupted status that might be left around after task runs on this thread.
            Thread.interrupted();

            // Release the permit that we acquired in order to run tasks on this thread,
            if (havePermit) {
                maxConcurrencyConstraint.release();

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(PolicyExecutorImpl.this, tc, "core/maxConcurrency available",
                             coreConcurrencyAvailable, maxConcurrencyConstraint.availablePermits());

                // The permit might be needed to run tasks on the global executor,
                if (!queue.isEmpty() && withheldConcurrency.get() > 0 && maxConcurrencyConstraint.tryAcquire()) {
                    decrementWithheldConcurrency();
                    if (acquireCoreConcurrency() > 0)
                        expediteGlobal(new PollingTask());
                    else
                        enqueueGlobal(new PollingTask());
                }
            }

            if (taskCount != 0)
                for (Future<T> f : futures)
                    f.cancel(true);
        }

        @SuppressWarnings("unchecked")
        List<Future<T>> list = List.class.cast(futures);
        return list;
    }

    // Submit and run tasks within allotted interval and return list of completed (possibly canceled) tasks.
    // Because this method is timed, tasks will never run on the invoker's current thread.
    @FFDCIgnore(value = { RejectedExecutionException.class })
    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, PolicyTaskCallback[] callbacks, long timeout, TimeUnit unit) throws InterruptedException {
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        int taskCount = tasks.size();
        long stop = System.nanoTime() + unit.toNanos(timeout);
        long qWait, remaining;

        List<PolicyTaskFutureImpl<T>> futures = new ArrayList<PolicyTaskFutureImpl<T>>(taskCount);
        try {
            // create futures in advance, which gives the callback an opportunity to reject
            int t = 0;
            for (Callable<T> task : tasks)
                futures.add(new PolicyTaskFutureImpl<T>(this, task, callbacks == null ? null : callbacks[t++]));

            // enqueue all tasks
            for (PolicyTaskFutureImpl<T> taskFuture : futures) {
                remaining = stop - System.nanoTime();
                if (remaining <= 0)
                    throw new RejectedExecutionException(Tr.formatMessage(tc, "CWWKE1204.unable.to.invoke", identifier, taskCount - futures.indexOf(taskFuture) + 1, taskCount,
                                                                          timeout, unit));
                qWait = maxWaitForEnqueueNS.get();
                enqueue(taskFuture,
                        qWait < remaining ? qWait : remaining, // limit waiting to lesser of maxWaitForEnqueue and remaining time
                        false); // never run on the current thread because it would prevent timeout
            }

            // wait for completion
            for (PolicyTaskFutureImpl<T> future : futures)
                if (future.await(stop - System.nanoTime(), TimeUnit.NANOSECONDS))
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

        @SuppressWarnings("unchecked")
        List<Future<T>> list = List.class.cast(futures);
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
            boolean useCurrentThread = queueFullAction.get() == QueueFullAction.CallerRuns;
            boolean havePermit = !useCurrentThread && (useCurrentThread = taskCount > 0 && maxConcurrencyConstraint.tryAcquire());
            if (useCurrentThread)
                try {
                    if (state.get() != State.ACTIVE)
                        throw new RejectedExecutionException(Tr.formatMessage(tc, "CWWKE1202.submit.after.shutdown", identifier));

                    PolicyTaskFutureImpl<T> taskFuture = new PolicyTaskFutureImpl<T>(this, tasks.iterator().next(), callbacks == null ? null : callbacks[0]);
                    taskFuture.accept();
                    runTask(taskFuture);

                    // raise InterruptedException if current thread is interrupted
                    taskFuture.throwIfInterrupted();

                    return taskFuture.get();
                } finally {
                    // Clear interrupted status that might be left around after task runs on this thread.
                    Thread.interrupted();

                    // Release the permit that we acquired in order to run tasks on this thread,
                    if (havePermit) {
                        maxConcurrencyConstraint.release();

                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            Tr.debug(PolicyExecutorImpl.this, tc, "core/maxConcurrency available",
                                     coreConcurrencyAvailable, maxConcurrencyConstraint.availablePermits());

                        // The permit might be needed to run tasks on the global executor,
                        if (!queue.isEmpty() && withheldConcurrency.get() > 0 && maxConcurrencyConstraint.tryAcquire()) {
                            decrementWithheldConcurrency();
                            if (acquireCoreConcurrency() > 0)
                                expediteGlobal(new PollingTask());
                            else
                                enqueueGlobal(new PollingTask());
                        }
                    }
                }
        } else if (taskCount == 0)
            throw new IllegalArgumentException();

        InvokeAnyCompletionTracker tracker = new InvokeAnyCompletionTracker(taskCount);
        ArrayList<PolicyTaskFutureImpl<T>> futures = new ArrayList<PolicyTaskFutureImpl<T>>(taskCount);
        try {
            // create futures in advance, which gives the callback an opportunity to reject
            int t = 0;
            for (Callable<T> task : tasks)
                futures.add(new PolicyTaskFutureImpl<T>(this, task, callbacks == null ? null : callbacks[t++], tracker));

            // enqueue all tasks
            for (PolicyTaskFutureImpl<T> taskFuture : futures) {
                // check if done before enqueuing more tasks
                if (tracker.hasSuccessfulResult())
                    break;

                enqueue(taskFuture, maxWaitForEnqueueNS.get(), false); // never run on the current thread because it would prevent timeout
            }

            while (!Thread.interrupted())
                LockSupport.park(tracker);
        } catch (RejectedExecutionException x) {
            if (x.getCause() instanceof InterruptedException) {
                throw (InterruptedException) x.getCause();
            } else
                throw x;
        } finally {
            T result = tracker.completeInvokeAny(futures);
            if (result != null || tracker.hasSuccessfulResult())
                return result;
        }

        // Only reachable if invokeAny was interrupted.
        throw new InterruptedException();
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

        InvokeAnyCompletionTracker tracker = new InvokeAnyCompletionTracker(taskCount);
        ArrayList<PolicyTaskFutureImpl<T>> futures = new ArrayList<PolicyTaskFutureImpl<T>>(taskCount);
        try {
            // create futures in advance, which gives the callback an opportunity to reject
            int t = 0;
            for (Callable<T> task : tasks) {
                remaining = stop - System.nanoTime();
                futures.add(new PolicyTaskFutureImpl<T>(this, task, callbacks == null ? null : callbacks[t++], tracker));
                if (remaining <= 0)
                    throw new RejectedExecutionException(Tr.formatMessage(tc, "CWWKE1204.unable.to.invoke", identifier, taskCount - futures.size(), taskCount, timeout, unit));
            }

            // enqueue all tasks
            for (PolicyTaskFutureImpl<T> taskFuture : futures) {
                remaining = stop - System.nanoTime();

                // check if done before enqueuing more tasks
                if (tracker.hasSuccessfulResult())
                    break;

                if (remaining <= 0)
                    throw new RejectedExecutionException(Tr.formatMessage(tc, "CWWKE1204.unable.to.invoke", identifier, taskCount - futures.size(), taskCount, timeout, unit));
                qWait = maxWaitForEnqueueNS.get();
                enqueue(taskFuture,
                        qWait < remaining ? qWait : remaining, // limit waiting to lesser of maxWaitForEnqueue and remaining time
                        false); // never run on the current thread because it would prevent timeout
            }

            // wait for completion
            TimeUnit.NANOSECONDS.sleep(stop - System.nanoTime());

            throw new TimeoutException();
        } catch (RejectedExecutionException x) {
            if (x.getCause() instanceof InterruptedException) {
                throw (InterruptedException) x.getCause();
            } else
                throw x;
        } finally {
            T result = tracker.completeInvokeAny(futures);
            if (result != null || tracker.hasSuccessfulResult())
                return result;
        }
    }

    @Override
    public boolean isShutdown() {
        if (providerCreated == null)
            throw new UnsupportedOperationException();
        else
            return state.get() != State.ACTIVE;
    }

    @Override
    public boolean isTerminated() {
        if (providerCreated == null)
            throw new UnsupportedOperationException();

        State currentState = state.get();
        switch (currentState) {
            case TERMINATED:
                return true;
            case ENQUEUE_STOPPED:
            case TASKS_CANCELING:
            case TASKS_CANCELED:
                // Transition to TERMINATED state if no tasks in the queue and no polling tasks on global executor
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
        if (providerCreated == null)
            throw new UnsupportedOperationException();

        if (max == -1)
            max = Integer.MAX_VALUE;
        else if (max < 1)
            throw new IllegalArgumentException(Integer.toString(max));

        synchronized (configLock) {
            if (max < coreConcurrency)
                throw new IllegalArgumentException(Integer.toString(max));

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
            enqueueGlobal(new PollingTask());
        }

        return this;
    }

    @Override
    public PolicyExecutor maxQueueSize(int max) {
        if (providerCreated == null)
            throw new UnsupportedOperationException();

        if (max == -1)
            max = Integer.MAX_VALUE;
        else if (max < 1)
            throw new IllegalArgumentException(Integer.toString(max));

        synchronized (configLock) {
            if (state.get() != State.ACTIVE)
                throw new IllegalStateException(Tr.formatMessage(tc, "CWWKE1203.config.update.after.shutdown", "maxQueueSize", identifier));

            int increase = max - maxQueueSize;
            if (increase > 0)
                maxQueueSizeConstraint.release(increase);
            else if (increase < 0)
                maxQueueSizeConstraint.reducePermits(-increase);
            maxQueueSize = max;
        }

        return this;
    }

    @Override
    public PolicyExecutor maxWaitForEnqueue(long ms) {
        if (providerCreated == null)
            throw new UnsupportedOperationException();

        if (ms < 0)
            throw new IllegalArgumentException(Long.toString(ms));

        for (long current = maxWaitForEnqueueNS.get(); current != -1; current = maxWaitForEnqueueNS.get())
            if (maxWaitForEnqueueNS.compareAndSet(current, TimeUnit.MILLISECONDS.toNanos(ms)))
                return this;

        throw new IllegalStateException(Tr.formatMessage(tc, "CWWKE1203.config.update.after.shutdown", "maxWaitForEnqueue", identifier));
    }

    @Override
    public PolicyExecutor queueFullAction(QueueFullAction action) {
        if (providerCreated == null)
            throw new UnsupportedOperationException();

        if (state.get() != State.ACTIVE)
            throw new IllegalStateException(Tr.formatMessage(tc, "CWWKE1203.config.update.after.shutdown", "queueFullAction", identifier));

        queueFullAction.set(action);

        return this;
    }

    /**
     * Invoked by the policy executor thread to run a task.
     *
     * @param future the future for the task.
     * @return Exception that occurred while running the task. Otherwise null.
     */
    void runTask(PolicyTaskFutureImpl<?> future) {
        try {
            if (providerCreated != null) // the following code only matters when life cycle operations are permitted
                running.add(future); // intentionally done before checking state to avoid missing cancels on shutdownNow

            if (state.get().canStartTask) {
                future.run();
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(this, tc, "Cancel task due to policy executor state " + state);
                future.cancel(false);
                if (future.callback != null)
                    future.callback.onEnd(future.task, future, null, true, 0, null); // aborted, queued task will never run
            }
        } catch (Error x) {
            // auto FFDC
        } catch (RuntimeException x) {
            // auto FFDC
        } finally {
            if (providerCreated != null)
                running.remove(future);
        }
    }

    @Override
    public void shutdown() {
        if (providerCreated == null)
            throw new UnsupportedOperationException();

        // Permanently update our configuration such that no more task submits are accepted
        if (state.compareAndSet(State.ACTIVE, State.ENQUEUE_STOPPING)) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                Tr.event(this, tc, "state: ACTIVE --> ENQUEUE_STOPPING");

            maxWaitForEnqueueNS.set(-1); // make attempted task submissions fail immediately

            synchronized (configLock) {
                maxQueueSize = 0;
                maxQueueSizeConstraint.drainPermits();
                maxQueueSizeConstraint.reducePermits(Integer.MAX_VALUE);
            }

            if (state.compareAndSet(State.ENQUEUE_STOPPING, State.ENQUEUE_STOPPED))
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                    Tr.event(this, tc, "state: ENQUEUE_STOPPING --> ENQUEUE_STOPPED");

            shutdownLatch.countDown();

            providerCreated.remove(identifier); // remove tracking of this instance and allow identifier to be reused
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
    public <T> Future<T> submit(Callable<T> task) {
        PolicyTaskFutureImpl<T> policyTaskFuture = new PolicyTaskFutureImpl<T>(this, task, null);
        enqueue(policyTaskFuture, maxWaitForEnqueueNS.get(), null);
        return policyTaskFuture;
    }

    @Override
    public <T> Future<T> submit(Callable<T> task, PolicyTaskCallback callback) {
        PolicyTaskFutureImpl<T> policyTaskFuture = new PolicyTaskFutureImpl<T>(this, task, callback);
        enqueue(policyTaskFuture, maxWaitForEnqueueNS.get(), null);
        return policyTaskFuture;
    }

    @Override
    public Future<?> submit(Runnable task) {
        PolicyTaskFutureImpl<?> policyTaskFuture = new PolicyTaskFutureImpl<Void>(this, task, null, null);
        enqueue(policyTaskFuture, maxWaitForEnqueueNS.get(), null);
        return policyTaskFuture;
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        PolicyTaskFutureImpl<T> policyTaskFuture = new PolicyTaskFutureImpl<T>(this, task, result, null);
        enqueue(policyTaskFuture, maxWaitForEnqueueNS.get(), null);
        return policyTaskFuture;
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result, PolicyTaskCallback callback) {
        PolicyTaskFutureImpl<T> policyTaskFuture = new PolicyTaskFutureImpl<T>(this, task, result, callback);
        enqueue(policyTaskFuture, maxWaitForEnqueueNS.get(), null);
        return policyTaskFuture;
    }

    public void introspect(PrintWriter out) {
        final String INDENT = "  ";
        final String DOUBLEINDENT = INDENT + INDENT;
        out.println(identifier);
        out.println(INDENT + "coreConcurrency = " + coreConcurrency);
        out.println(INDENT + "maxConcurrency = " + maxConcurrency);
        out.println(INDENT + "maxQueueSize = " + maxQueueSize);
        out.println(INDENT + "maxWaitForEnqueue = " + TimeUnit.NANOSECONDS.toMillis(maxWaitForEnqueueNS.get()) + " ms");
        out.println(INDENT + "queueFullAction = " + queueFullAction.toString());
        int numRunningThreads, numRunningPrioritizedThreads;
        synchronized (configLock) {
            numRunningThreads = maxConcurrency - maxConcurrencyConstraint.availablePermits();
            numRunningPrioritizedThreads = coreConcurrency - coreConcurrencyAvailable.get();
        }
        out.println(INDENT + "Total Enqueued to Global Executor = " + numRunningThreads + " (" + numRunningPrioritizedThreads + " expedited)");
        out.println(INDENT + "withheldConcurrency = " + withheldConcurrency.get());
        out.println(INDENT + "Remaining Queue Capacity = " + maxQueueSizeConstraint.availablePermits());
        out.println(INDENT + "Created by PolicyExecutorProvider = " + (providerCreated != null));
        out.println(INDENT + "state = " + state.toString());
        out.println(INDENT + "Running Task Futures: ");
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
