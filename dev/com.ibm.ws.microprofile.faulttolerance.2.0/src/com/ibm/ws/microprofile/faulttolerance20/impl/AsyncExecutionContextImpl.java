/*******************************************************************************
 * Copyright (c) 2018, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.faulttolerance20.impl;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import com.ibm.ws.microprofile.faulttolerance20.state.RetryState;
import com.ibm.wsspi.threadcontext.ThreadContextDescriptor;

/**
 * Stores context for one asynchronous method execution
 *
 * @param <W> the return type of the code being executed, which is also the type of the result wrapper (e.g. {@code Future<String>})
 */
public class AsyncExecutionContextImpl<W> extends SyncExecutionContextImpl {

    private W resultWrapper;
    private Callable<W> callable;
    private RetryState retryState;
    private final AtomicBoolean isCancelled = new AtomicBoolean(false);
    private boolean interruptOnCancellation;
    private Consumer<Boolean> cancelCallback;
    private ThreadContextDescriptor threadContextDescriptor;

    public AsyncExecutionContextImpl(Method method, Object[] parameters) {
        super(method, parameters);
    }

    public W getResultWrapper() {
        return resultWrapper;
    }

    public void setResultWrapper(W returnWrapper) {
        this.resultWrapper = returnWrapper;
    }

    public Callable<W> getCallable() {
        return callable;
    }

    public void setCallable(Callable<W> callable) {
        this.callable = callable;
    }

    public RetryState getRetryState() {
        return retryState;
    }

    public void setRetryState(RetryState retryState) {
        this.retryState = retryState;
    }

    public ThreadContextDescriptor getThreadContextDescriptor() {
        return threadContextDescriptor;
    }

    public void setThreadContextDescriptor(ThreadContextDescriptor threadContextDescriptor) {
        this.threadContextDescriptor = threadContextDescriptor;
    }

    public void setCancelCallback(Consumer<Boolean> cancelCallback) {
        this.cancelCallback = cancelCallback;
        if (isCancelled.get()) {
            cancelCallback.accept(interruptOnCancellation);
        }
    }

    public void cancel(boolean mayInterrupt) {
        // Note cancelledValue must be set before updating isCancelled so we don't
        // get a race condition between cancel and setCancelCallback.
        interruptOnCancellation = mayInterrupt;
        if (isCancelled.compareAndSet(false, true)) {
            cancelCallback.accept(interruptOnCancellation);
        }
    }

    public boolean isCancelled() {
        return isCancelled.get();
    }

}
