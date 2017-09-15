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
package com.ibm.ws.microprofile.faulttolerance.cdi;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 *
 */
public class WrappedFuture implements Future<Object> {

    private final Future<?> future;

    public WrappedFuture(Future<?> future) {
        this.future = future;
    }

    /** {@inheritDoc} */
    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return future.cancel(mayInterruptIfRunning);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isCancelled() {
        return future.isCancelled();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isDone() {
        return future.isDone();
    }

    /** {@inheritDoc} */
    @Override
    public Object get() throws InterruptedException, ExecutionException {
        Future<?> wrapped = (Future<?>) future.get();
        return wrapped.get();
    }

    /** {@inheritDoc} */
    @Override
    public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        Future<?> wrapped = (Future<?>) future.get(timeout, unit);
        return wrapped.get(timeout, unit);
    }

}
