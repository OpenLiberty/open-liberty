/*******************************************************************************
 * Copyright (c) 2011, 2026 IBM Corporation and others.
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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.ibm.websphere.ras.annotation.Trivial;

public final class ImmediateFutureImpl<T> implements Future<T> {
    private T _result;
    private ExecutionException _exception;

    public ImmediateFutureImpl(T result) {
        _result = result;
    }

    public ImmediateFutureImpl(Throwable t) {
        _exception = new ExecutionException(t);
    }

    @Trivial
    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public T get() throws ExecutionException {
        if (_exception != null) {
            throw _exception;
        } else {
            return _result;
        }
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws ExecutionException {
        return get();
    }

    @Trivial
    @Override
    public boolean isCancelled() {
        return false;
    }

    @Trivial
    @Override
    public boolean isDone() {
        return true;
    }
}