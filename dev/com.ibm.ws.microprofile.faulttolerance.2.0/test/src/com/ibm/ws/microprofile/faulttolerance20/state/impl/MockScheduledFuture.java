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

import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class MockScheduledFuture<V> implements ScheduledFuture<V> {

    private boolean cancelled = false;

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        cancelled = true;
        return true;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public boolean isDone() {
        throw MockScheduledExecutorService.notImplemented();
    }

    @Override
    public V get() throws InterruptedException, ExecutionException {
        throw MockScheduledExecutorService.notImplemented();
    }

    @Override
    public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        throw MockScheduledExecutorService.notImplemented();
    }

    @Override
    public long getDelay(TimeUnit unit) {
        throw MockScheduledExecutorService.notImplemented();
    }

    @Override
    public int compareTo(Delayed o) {
        throw MockScheduledExecutorService.notImplemented();
    }
}