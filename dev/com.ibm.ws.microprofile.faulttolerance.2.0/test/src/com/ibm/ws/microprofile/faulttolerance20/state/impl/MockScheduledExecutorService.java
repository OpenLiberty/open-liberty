/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.faulttolerance20.state.impl;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
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
 * A mock scheduled executor service for unit tests
 * <p>
 * Most methods are not implemented
 * <p>
 * {@link #getTasks()} returns a list of tasks which have been scheduled so they can be inspected and run
 */
public class MockScheduledExecutorService implements ScheduledExecutorService {

    private final List<MockScheduledTask<?>> scheduledTasks = new ArrayList<>();

    @Override
    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        MockScheduledFuture<Void> future = new MockScheduledFuture<>();
        MockScheduledTask<Void> task = new MockScheduledTask<>(command, Duration.of(unit.toNanos(delay), ChronoUnit.NANOS), future);
        scheduledTasks.add(task);
        return future;
    }

    public List<MockScheduledTask<?>> getTasks() {
        return new ArrayList<>(scheduledTasks);
    }

    static UnsupportedOperationException notImplemented() {
        return new UnsupportedOperationException("Method not implemented in test mock");
    }

    @Override
    public void shutdown() {
        throw notImplemented();
    }

    @Override
    public List<Runnable> shutdownNow() {
        throw notImplemented();
    }

    @Override
    public boolean isShutdown() {
        throw notImplemented();
    }

    @Override
    public boolean isTerminated() {
        throw notImplemented();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        throw notImplemented();
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        throw notImplemented();
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        throw notImplemented();
    }

    @Override
    public Future<?> submit(Runnable task) {
        throw notImplemented();
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        throw notImplemented();
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        throw notImplemented();
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        throw notImplemented();
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        throw notImplemented();
    }

    @Override
    public void execute(Runnable command) {
        throw notImplemented();
    }

    @Override
    public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
        throw notImplemented();
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        throw notImplemented();
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
        throw notImplemented();
    }

}
