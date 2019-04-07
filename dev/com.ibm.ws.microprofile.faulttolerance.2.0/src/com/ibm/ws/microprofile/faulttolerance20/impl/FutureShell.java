/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.faulttolerance20.impl;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

/**
 * A holder which delegates to a result we may not have yet
 * <p>
 * FutureShell acts as in incomplete {@link Future} until {@link #setDelegate(Future)} is called, at which point it delegates all calls to the delegate.
 * <p>
 * If FutureShell is cancelled before {@link #setDelegate(Future)} is called, it will report itself as cancelled, will call the cancellation callback if one has been set using
 * {@link #setCancellationCallback(Consumer)} and will call {@link #cancel(boolean)} on the delegate if one is
 * set at a later time.
 *
 * @param <V> The result type returned by this class's {@code get} method
 */
public class FutureShell<V> implements Future<V> {

    // Do not need to synchronize around these
    private final CompletableFuture<Future<V>> delegateHolder = new CompletableFuture<Future<V>>();
    private volatile Future<V> delegate = null;

    // Must synchronize around these
    private boolean mayInterruptWhenCancellingDelegate = false;
    private Consumer<Boolean> cancellationCallback;

    /**
     * Set a callback to be run if {@link #cancel(boolean)} is called before {@link #setDelegate(Future)} is called.
     * <p>
     * This will be run from the {@link #cancel(boolean)} method, so it should not do any waiting.
     *
     * @param cancellationCallback the callback
     */
    public synchronized void setCancellationCallback(Consumer<Boolean> cancellationCallback) {
        this.cancellationCallback = cancellationCallback;
    }

    /**
     * Set the Future to delegate calls to
     *
     * @param delegate the delegate future
     */
    public synchronized void setDelegate(Future<V> delegate) {
        if (delegateHolder.isCancelled()) {
            delegate.cancel(mayInterruptWhenCancellingDelegate);
        } else {
            this.delegate = delegate;
            delegateHolder.complete(delegate);
        }
    }

    @Override
    public synchronized boolean cancel(boolean mayInterruptIfRunning) {
        if (delegateHolder.cancel(false)) {
            mayInterruptWhenCancellingDelegate = mayInterruptIfRunning;
            if (cancellationCallback != null) {
                cancellationCallback.accept(mayInterruptIfRunning);
            }
            return true;
        }

        if (delegateHolder.isDone() && !delegateHolder.isCancelled()) {
            return delegate.cancel(mayInterruptIfRunning);
        } else {
            return false;
        }
    }

    @Override
    public boolean isCancelled() {
        if (delegateHolder.isDone()) {
            if (delegateHolder.isCancelled()) {
                return true;
            } else {
                return delegate.isCancelled();
            }
        }

        return false;
    }

    @Override
    public boolean isDone() {
        if (delegateHolder.isDone()) {
            if (delegateHolder.isCancelled()) {
                return true;
            } else {
                return delegate.isDone();
            }
        }

        return false;
    }

    @Override
    public V get() throws InterruptedException, ExecutionException {
        return delegateHolder.get().get();
    }

    @Override
    public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        long timeoutNanos = unit.toNanos(timeout);
        long startTime = System.nanoTime();

        Future<V> delegate = delegateHolder.get(timeoutNanos, TimeUnit.NANOSECONDS);

        long elapsedTime = System.nanoTime() - startTime;
        return delegate.get(timeoutNanos - elapsedTime, TimeUnit.NANOSECONDS);
    }

    @Override
    public String toString() {
        if (delegateHolder.isCancelled()) {
            return "FutureShell cancelled";
        } else if (delegateHolder.isDone()) {
            return "FutureShell delegating to " + delegate;
        } else {
            return "FutureShell incomplete";
        }
    }

}
