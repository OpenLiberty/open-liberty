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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * <p>Policy executors are backed by the Liberty global thread pool, but allow
 * concurrency constraints and various queue attributes to be controlled independently
 * of the global thread pool.</p>
 */
public interface PolicyExecutor extends ExecutorService {
    /**
     * Specifies a core number of tasks that the policy executor should aim to run concurrently
     * by expediting requests to the global thread pool. This is different than a minimum in that
     * no guarantee is made that this many tasks will be running concurrently.
     * The default coreConcurrency is 0.
     * If core concurrency is updated while the policy executor is in use, the update goes into
     * effect gradually as previous expedited and non-expedited requests complete.
     *
     * @param core core concurrency.
     * @return the executor.
     * @throws IllegalArgumentException if value is negative or greater than maximum concurrency.
     * @throws IllegalStateException if the executor has been shut down.
     * @throws UnsupportedOperationException if invoked on a policyExecutor instance created from server configuration.
     */
    PolicyExecutor coreConcurrency(int core);

    /**
     * Submits and invokes a group of tasks with a callback per task to be invoked at various points in the task's life cycle.
     * The first task is paired with the first callback. The second task is paired with the second callback. An so forth.
     * It is okay to include the same callback at multiple positions or to include null callbacks at some positions.
     *
     * @throws ArrayIndexOutOfBoundsException if the size of the callbacks array is less than the number of tasks.
     * @see java.util.concurrent.ExecutorService#invokeAll(java.util.Collection)
     */
    <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, PolicyTaskCallback[] callbacks) throws InterruptedException;

    /**
     * Submits and invokes a group of tasks with a callback per task to be invoked at various points in the task's life cycle.
     * The first task is paired with the first callback. The second task is paired with the second callback. An so forth.
     * It is okay to include the same callback at multiple positions or to include null callbacks at some positions.
     *
     * @throws ArrayIndexOutOfBoundsException if the size of the callbacks array is less than the number of tasks.
     * @see java.util.concurrent.ExecutorService#invokeAll(java.util.Collection, long, java.util.concurrent.TimeUnit)
     */
    <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, PolicyTaskCallback[] callbacks, long timeout, TimeUnit unit) throws InterruptedException;

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
     * Specifies the maximum number of tasks from this policy executor that can be running
     * at any given point in time. The default maxConcurrency is Integer.MAX_VALUE.
     * Maximum concurrency can be updated while tasks are in progress. If the maximum concurrency
     * is reduced below the number of concurrently executing tasks, the update goes into effect
     * gradually, as in-progress tasks complete rather than causing them to be canceled.
     *
     * @param max maximum concurrency.
     * @return the executor.
     * @throws IllegalArgumentException if value is not positive or -1 (which means Integer.MAX_VALUE) or less than core concurrency.
     * @throws IllegalStateException if the executor has been shut down.
     * @throws UnsupportedOperationException if invoked on a policyExecutor instance created from server configuration.
     */
    PolicyExecutor maxConcurrency(int max);

    /**
     * Indicates whether or not to count tasks that run on the caller's thread towards maxConcurrency.
     * Tasks can run on the caller's thread when using untimed invokeAll, or, if only invoking a single task, untimed invokeAny.
     * If runIfQueueFull is true, tasks can also run on the caller's thread when using the execute and submit methods.
     * The default value is false.
     *
     * @param applyToCallerThread indicates whether or not tasks that run on the invoking thread count towards maxConcurrency.
     * @return the executor.
     * @throws IllegalStateException if the executor has been shut down.
     * @throws UnsupportedOperationException if invoked on a policyExecutor instance created from server configuration.
     */
    PolicyExecutor maxConcurrencyAppliesToCallerThread(boolean applyToCallerThread);

    /**
     * Specifies the maximum number of submitted tasks that can be queued for execution at any given point in time.
     * As tasks are started or canceled, they are removed from the queue. When the queue is
     * at capacity and another task is submitted, the policy executor waits for up to the
     * maxWaitForEnqueue for a queue position to become available, after which, if the queue
     * is still at capacity, runIfQueueFull determines whether to attempt running on the current thread
     * (if permitted by maxConcurrency) or whether to reject the task submission.
     * Applications that submit many tasks over a short period of time might want to use
     * a maximum queue size that is at least as large as the maximum concurrency.
     * The default maxQueueSize is Integer.MAX_VALUE.
     * Maximum queue size can be updated while tasks are in progress and/or queued for execution.
     * If the maximum queue size is reduced below the current number of queued tasks,
     * the update goes into effect gradually, queued tasks execute naturally or are canceled
     * by the user, rather than automatically canceling the excess queued tasks.
     *
     * @param max capacity of the task queue.
     * @return the executor.
     * @throws IllegalArgumentException if value is not positive or -1 (which means Integer.MAX_VALUE).
     * @throws IllegalStateException if the executor has been shut down.
     * @throws UnsupportedOperationException if invoked on a policyExecutor instance created from server configuration.
     */
    PolicyExecutor maxQueueSize(int max);

    /**
     * Specifies the maximum number of milliseconds to wait for enqueuing a submitted task.
     * The default value of 0 indicates to not wait at all.
     * When maxWaitForEnqueue is updated, the update applies to task submits that occur
     * after that point. Submits that were already waiting continue to wait per the previously
     * configured value.
     *
     * @param ms maximum number of milliseconds to wait when attempting to queue a submitted task.
     * @return the executor.
     * @throws IllegalArgumentException if value is negative.
     * @throws IllegalStateException if the executor has been shut down.
     * @throws UnsupportedOperationException if invoked on a policyExecutor instance created from server configuration.
     */
    PolicyExecutor maxWaitForEnqueue(long ms);

    /**
     * Applies when using the execute or submit methods. Indicates whether or not to run the task on the
     * caller's thread when the queue is full and the maxWaitForEnqueue has been exceeded.
     * The default value is false, in which case the task submission is rejected after the maxWaitForEnqueue elapses
     * instead of running on the caller's thread.
     *
     * @param runIfFull true to indicate that a task which cannot be queued should run on the thread from which submit or execute is invoked;
     *            false to abort the task in this case.
     * @return the executor.
     * @throws IllegalStateException if the executor has been shut down.
     * @throws UnsupportedOperationException if invoked on a policyExecutor instance created from server configuration.
     */
    PolicyExecutor runIfQueueFull(boolean runIfFull);

    /**
     * Submit a Callable task with a callback to be invoked at various points in the task's life cycle.
     *
     * @see java.util.concurrent.ExecutorService#submit(java.util.concurrent.Callable)
     */
    <T> Future<T> submit(Callable<T> task, PolicyTaskCallback callback);

    /**
     * Submit a Runnable task with a callback to be invoked at various points in the task's life cycle.
     *
     * @see java.util.concurrent.ExecutorService#submit(java.lang.Runnable, java.lang.Object)
     */
    <T> Future<T> submit(Runnable task, T result, PolicyTaskCallback callback);
}
