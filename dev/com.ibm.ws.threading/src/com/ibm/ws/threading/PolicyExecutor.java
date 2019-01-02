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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 * <p>Policy executors are backed by the Liberty global thread pool, but allow
 * concurrency constraints, queue attributes, and other related behaviors to be controlled independently
 * of the global thread pool.</p>
 *
 * <p>Policy executors are constructed programmatically via <code>PolicyExecutorProvider</code>,
 * or as OSGi service components backed by configuration (the <code>concurrencyPolicy</code> element).
 * </p>
 */
public interface PolicyExecutor extends ExecutorService {
    /**
     * A policy for enforcing maximum concurrency.
     */
    @Trivial
    public enum MaxPolicy {
        /**
         * Maximum concurrency is loosely enforced, in that tasks are allowed to run on the submitter's thread without counting against maximum concurrency.
         */
        loose,

        /**
         * Maximum concurrency is strictly enforced, in that tasks that run on the submitter's thread count towards maximum concurrency.
         * This policy does not allow running on the submitter's thread if already at maximum concurrency.
         */
        strict
    }

    /**
     * Attempts to cancel all tasks (both in the queue and running) where the identifier of the task submitter matches the specified identifier.
     * If PolicyTaskCallback is used when submitting a task, the identifier of the task submitter is obtained via PolicyTaskCallback.getIdentifier.
     * Otherwise, the identifier is the identifier computed by the PolicyExecutorProvider.create method when the PolicyExecutor was created.
     * Canceled tasks which have already started might still be in progress when this method returns. How the tasks responds to the interrupt/cancel
     * signal depends on the task implementation.
     *
     * @param identifier the identifier to match
     * @param interruptIfRunning indicates whether or not to allow interrupt on cancel
     * @return count of task Futures that were successfully put into the canceled state.
     */
    int cancel(String identifier, boolean interruptIfRunning);

    /**
     * Specifies a core number of tasks to aim to run concurrently
     * by expediting requests to the global thread pool. This provides no guarantee
     * that this many tasks will run concurrently. The default value is 0.
     * If the expedite value is updated while in use, the update goes into
     * effect gradually as previous expedited and non-expedited requests complete.
     *
     * @param num number of tasks to expedite.
     * @return the executor.
     * @throws IllegalArgumentException if value is negative or greater than maximum concurrency.
     * @throws IllegalStateException if the executor has been shut down.
     */
    PolicyExecutor expedite(int num);

    /**
     * Returns the unique identifier for this policy executor.
     *
     * @return the unique identifier for this policy executor.
     */
    String getIdentifier();

    /**
     * Returns the number of tasks from this PolicyExecutor currently running on the global executor.
     *
     * @return the number of running tasks
     */
    int getRunningTaskCount();

    /**
     * Submits and invokes a group of tasks with a callback per task to be invoked at various points in the task's life cycle.
     * The first task is paired with the first callback. The second task is paired with the second callback. An so forth.
     * It is okay to include the same callback at multiple positions or to include null callbacks at some positions.
     *
     * @throws ArrayIndexOutOfBoundsException if the size of the callbacks array is less than the number of tasks.
     * @see java.util.concurrent.ExecutorService#invokeAll(java.util.Collection)
     */
    <T> List<PolicyTaskFuture<T>> invokeAll(Collection<? extends Callable<T>> tasks, PolicyTaskCallback[] callbacks) throws InterruptedException;

    /**
     * Submits and invokes a group of tasks with a callback per task to be invoked at various points in the task's life cycle.
     * The first task is paired with the first callback. The second task is paired with the second callback. An so forth.
     * It is okay to include the same callback at multiple positions or to include null callbacks at some positions.
     *
     * @throws ArrayIndexOutOfBoundsException if the size of the callbacks array is less than the number of tasks.
     * @see java.util.concurrent.ExecutorService#invokeAll(java.util.Collection, long, java.util.concurrent.TimeUnit)
     */
    <T> List<PolicyTaskFuture<T>> invokeAll(Collection<? extends Callable<T>> tasks, PolicyTaskCallback[] callbacks, long timeout, TimeUnit unit) throws InterruptedException;

    /**
     * Submits and awaits successful completion of any task within a group of tasks.
     * A callback is invoked at various points in the life cycle of each task.
     * The first task is paired with the first callback. The second task is paired with the second callback. An so forth.
     *
     * @throws ArrayIndexOutOfBoundsException if the size of the callbacks array is less than the number of tasks.
     * @see java.util.concurrent.ExecutorService#invokeAny(java.util.Collection)
     */
    <T> T invokeAny(Collection<? extends Callable<T>> tasks, PolicyTaskCallback[] callbacks) throws InterruptedException, ExecutionException;

    /**
     * Submits and awaits successful completion of any task within a group of tasks, subject to a timeout.
     * A callback is invoked at various points in the life cycle of each task.
     * The first task is paired with the first callback. The second task is paired with the second callback. An so forth.
     *
     * @throws ArrayIndexOutOfBoundsException if the size of the callbacks array is less than the number of tasks.
     * @see java.util.concurrent.ExecutorService#invokeAny(java.util.Collection, long, java.util.concurrent.TimeUnit)
     */
    <T> T invokeAny(Collection<? extends Callable<T>> tasks, PolicyTaskCallback[] callbacks, long timeout,
                    TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException;

    /**
     * Specifies the maximum number of tasks that can be running
     * at any given point in time. The default is Integer.MAX_VALUE.
     * Maximum concurrency can be updated while tasks are in progress. If the maximum concurrency
     * is reduced below the number of concurrently executing tasks, the update goes into effect
     * gradually, as in-progress tasks complete rather than causing them to be canceled.
     *
     * @param max maximum concurrency.
     * @return the executor.
     * @throws IllegalArgumentException if value is not positive or -1 (which means Integer.MAX_VALUE) or less than the number to expedite.
     * @throws IllegalStateException if the executor has been shut down.
     */
    PolicyExecutor maxConcurrency(int max);

    /**
     * Indicates whether to loosely (the default) or strictly enforce maximum concurrency for tasks that run on the caller's thread.
     * Tasks can run on the caller's thread when using untimed invokeAll, or, if only invoking a single task, untimed invokeAny.
     * If runIfQueueFull is true, tasks can also run on the caller's thread when using the execute and submit methods.
     * In all of these cases, <code>maxPolicy</code> determines whether running on the caller's thread counts against
     * the maximum concurrency.
     *
     * @param policy indicates whether or not tasks that run on the invoking thread count towards maximum concurrency.
     * @return the executor.
     * @throws IllegalStateException if the executor has been shut down.
     */
    PolicyExecutor maxPolicy(MaxPolicy policy);

    /**
     * Specifies the maximum number of submitted tasks that can be queued for execution at any given point in time.
     * As tasks are started, canceled, or aborted, they are removed from the queue.
     * When the queue is at capacity and another task is submitted, the behavior is determined by the
     * <code>maxWaitForEnqueue</code> and <code>runIfQueueFull</code> attributes.
     * Applications that submit many tasks over a short period of time might want to use
     * a maximum queue size that is at least as large as the maximum concurrency.
     * The default maxQueueSize is Integer.MAX_VALUE.
     * Maximum queue size can be updated while tasks are in progress and/or queued for execution.
     * If the maximum queue size is reduced below the current number of queued tasks,
     * the update goes into effect gradually, as queued tasks execute naturally or are canceled
     * by the user, rather than automatically canceling the excess queued tasks.
     *
     * @param max capacity of the task queue.
     * @return the executor.
     * @throws IllegalArgumentException if value is not positive or -1 (which means Integer.MAX_VALUE).
     * @throws IllegalStateException if the executor has been shut down.
     */
    PolicyExecutor maxQueueSize(int max);

    /**
     * Specifies the maximum number of milliseconds to wait for enqueuing a submitted task.
     * If unable to enqueue the task within this interval, the task submission is subject to the <code>runIfQueueFull</code> policy.
     * The default value of 0 indicates to not wait at all.
     * When <code>maxWaitForEnqueue</code> is updated, the update applies only to tasks submitted
     * after that point. Submits that were already waiting continue to wait per the previously
     * configured value.
     *
     * @param ms maximum number of milliseconds to wait when attempting to enqueue a submitted task.
     * @return the executor.
     * @throws IllegalArgumentException if value is negative.
     * @throws IllegalStateException if the executor has been shut down.
     */
    PolicyExecutor maxWaitForEnqueue(long ms);

    /**
     * Returns the number of additional tasks that can be enqueued without exceeding the maximum queue size.
     *
     * @return remaining capacity in queue
     */
    int queueCapacityRemaining();

    /**
     * Registers a one-time callback to be invoked asynchronously
     * when the count of running tasks exceeds the specified maximum.
     * If a concurrency callback is already registered, this replaces
     * the previous registration.
     * To unregister an existing callback without replacing,
     * specify a null value for the callback.
     * The callback is automatically unregistered upon shutdown.
     *
     * @param max threshold for maximum concurrency beyond which the callback should be notified.
     * @param callback the callback, or null to unregister.
     * @return callback that was replaced or removed by the new registration.
     *         null if no previous callback was in place.
     * @throws IllegalStateException if the executor has been shut down.
     */
    Runnable registerConcurrencyCallback(int max, Runnable callback);

    /**
     * Registers a one-time callback to be invoked asynchronously when
     * the difference between task start time and submit time exceeds the specified maximum delay.
     * If a late start callback is already registered, this replaces
     * the previous registration.
     * To unregister an existing callback without replacing,
     * specify a null value for the callback.
     * The callback is automatically unregistered upon shutdown.
     *
     * @param maxDelay maximum delay for a task to start, beyond which the callback should be notified.
     * @param unit unit of time.
     * @param callback the callback, or null to unregister.
     * @return callback that was replaced or removed by the new registration.
     *         null if no previous callback was in place.
     * @throws IllegalArgumentException if maxDelay is greater than or equal to the
     *             maximum number of nanoseconds representable as a long value.
     * @throws IllegalStateException if the executor has been shut down.
     */
    Runnable registerLateStartCallback(long maxDelay, TimeUnit unit, Runnable callback);

    /**
     * Registers a one-time callback to be invoked asynchronously
     * when the available remaining capacity of the task queue
     * drops below the specified minimum.
     * If a queue size callback is already registered, this replaces
     * the previous registration.
     * To unregister an existing callback without replacing,
     * specify a null value for the callback.
     * The callback is automatically unregistered upon shutdown.
     *
     * @param minAvailable threshold for minimum available queue capacity
     *            below which the callback should be notified.
     * @param callback the callback, or null to unregister.
     * @return callback that was replaced or removed by the new registration.
     *         null if no previous callback was in place.
     * @throws IllegalStateException if the executor has been shut down.
     */
    Runnable registerQueueSizeCallback(int minAvailable, Runnable callback);

    /**
     * Applies when using the <code>execute</code> or <code>submit</code> methods. Indicates whether or not to run the task on the
     * caller's thread when the queue is full and the <code>maxWaitForEnqueue</code> has been exceeded.
     * The default value is false, in which case the task submission is rejected after the <code>maxWaitForEnqueue</code> elapses
     * instead of running on the caller's thread.
     *
     * @param runIfFull true to indicate that a task which cannot be queued should run on the thread from which submit or execute is invoked;
     *            false to abort the task in this case.
     * @return the executor.
     * @throws IllegalStateException if the executor has been shut down.
     */
    PolicyExecutor runIfQueueFull(boolean runIfFull);

    /**
     * Specifies a number of milliseconds, starting at task submit, after which a task should not start. The default value of -1 means no timeout.
     * The value returned by <code>PolicyTaskCallabck.getStartTimeout</code>, if any, overrides on a per-task basis.
     * Note that if both <code>maxWaitForEnqueue</code> and <code>startTimeout</code> are enabled,
     * the <code>startTimeout</code> should be configured larger than the <code>maxWaitForEnqueue</code>
     * such that remains possible to start tasks after they have waited for a queue position.
     *
     * @param ms number of milliseconds beyond which a task should not start.
     * @return the executor.
     * @throws IllegalArgumentException if value is negative (other than -1) or too large to convert to a nanosecond <code>long</code> value.
     * @throws IllegalStateException if the executor has been shut down.
     */
    PolicyExecutor startTimeout(long ms);

    /**
     * Submit a Runnable task that performs a CompletableFuture action.
     * The CompletableFuture might be unavailable when this method is invoked and can be
     * supplied at a later time by setting the value of the AtomicReference.
     * The PolicyExecutor automatically cancels the CompeletableFuture if it is
     * available at the time when the PolicyTaskFuture is canceled.
     *
     * @param completableFutureRef reference to a CompletableFuture that the PolicyExecutor should
     *            automatically cancel upon cancellation of the returned future.
     * @param task the task to run
     * @return future for the task.
     */
    PolicyTaskFuture<Void> submit(AtomicReference<Future<?>> completableFutureRef, Runnable task);

    /**
     * Submit a Callable task with a callback to be invoked at various points in the task's life cycle.
     *
     * @see java.util.concurrent.ExecutorService#submit(java.util.concurrent.Callable)
     */
    <T> PolicyTaskFuture<T> submit(Callable<T> task, PolicyTaskCallback callback);

    /**
     * Submit a Runnable task with a callback to be invoked at various points in the task's life cycle.
     *
     * @see java.util.concurrent.ExecutorService#submit(java.lang.Runnable, java.lang.Object)
     */
    <T> PolicyTaskFuture<T> submit(Runnable task, T result, PolicyTaskCallback callback);

    /**
     * Update the configuration of this instance, either initially or as a modification.
     * This method is appropriate for configuration-based OSGi service components (concurrencyPolicy).
     *
     * @param props key/value pairs containing the already-validated configuration.
     */
    void updateConfig(Map<String, Object> props);
}
