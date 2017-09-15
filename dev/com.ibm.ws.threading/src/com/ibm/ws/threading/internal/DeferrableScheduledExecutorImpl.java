/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.threading.internal;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * An implementation of ScheduledExecutorService that the client acknowledges might delay execution for performance.
 */
public class DeferrableScheduledExecutorImpl implements ScheduledExecutorService {
    static final long PERIOD_MILLISECONDS = TimeUnit.MILLISECONDS.convert(15, TimeUnit.SECONDS);

    /**
     * Round up delays so that all tasks fire at approximately with approximately the same 15s period.
     */
    static long roundUpDelay(long delay, TimeUnit unit, long now) {
        if (delay < 0) {
            // Negative is treated as 0.
            delay = 0;
        }

        long target = now + unit.toMillis(delay);
        if (target < now) {
            // We can't add the delay to the current time without overflow.
            // Return the delay unaltered.
            return delay;
        }

        long remainder = target % PERIOD_MILLISECONDS;
        if (remainder == 0) {
            // Already rounded.
            return delay;
        }

        long extra = PERIOD_MILLISECONDS - remainder;

        long newDelay = delay + unit.convert(extra, TimeUnit.MILLISECONDS);
        if (newDelay < delay) {
            // We can't round up without overflow.  Return the delay unaltered.
            return delay;
        }

        return newDelay;
    }

    private static long roundUpDelay(long delay, TimeUnit unit) {
        return roundUpDelay(delay, unit, System.currentTimeMillis());
    }

    private ScheduledExecutorService executor;

    public void setExecutor(ScheduledExecutorService executor) {
        this.executor = executor;
    }

    public void unsetExecutor(ScheduledExecutorService executor) {
        // Nothing.
    }

    @Override
    public void execute(Runnable command) {
        executor.execute(command);
    }

    @Override
    public void shutdown() {
        executor.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        return executor.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return executor.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return executor.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return executor.awaitTermination(timeout, unit);
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return executor.submit(task);
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return executor.submit(task, result);
    }

    @Override
    public Future<?> submit(Runnable task) {
        return executor.submit(task);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return executor.invokeAll(tasks);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        return executor.invokeAll(tasks, timeout, unit);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return executor.invokeAny(tasks);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return executor.invokeAny(tasks, timeout, unit);
    }

    @Override
    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        return executor.schedule(command, roundUpDelay(delay, unit), unit);
    }

    @Override
    public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
        return executor.schedule(callable, roundUpDelay(delay, unit), unit);
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        return executor.scheduleAtFixedRate(command, roundUpDelay(initialDelay, unit), roundUpDelay(period, unit, 0), unit);
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
        // There's no sense in rounding the delay since the time taken to run the task will slowly cause drift anyway.
        return executor.scheduleWithFixedDelay(command, roundUpDelay(initialDelay, unit), delay, unit);
    }
}
