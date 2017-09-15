/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.threading.internal;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import com.ibm.ws.threading.listeners.CompletionListener;

public class NonCancelableResultFuture<T> implements Future<T> {
    /**
     * True if <code>_result</code> has been set or <code>_failure</code> is non-null.<br>
     * False otherwise.
     */
    private volatile boolean _done;
    private T _result;
    private ExecutionException _failure;
    private final Queue<CompletionListener<T>> _listeners = new ConcurrentLinkedQueue<CompletionListener<T>>();
    private final AtomicBoolean _notifiedListeners = new AtomicBoolean();

    @Override
    public String toString() {
        if (!_done) {
            return super.toString() + "[incomplete]";
        }

        Throwable failure = _failure;
        if (failure != null) {
            return super.toString() + "[fail exception=" + failure + ']';
        }

        T result = _result;
        String resultString = result == null ? null : result.getClass().getName() + '@' + Integer.toHexString(System.identityHashCode(result));
        return super.toString() + "[success result=" + resultString + ']';
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public T get() throws InterruptedException, ExecutionException {
        try {
            return get(Long.MAX_VALUE, TimeUnit.DAYS);
        } catch (TimeoutException e) {
            // I'm not sure the universe will live for Long.MAX_VALUE days so I'm ignore this exception
            throw (InterruptedException) new InterruptedException().initCause(e);
        }
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException,
                    TimeoutException {

        if (!_done) {
            synchronized (this) {
                long startTime = System.currentTimeMillis();
                long waitTime = unit.toMillis(timeout);
                //put the wait in a loop to check that it slept for long enough
                //this protects against early timeouts in cases where it
                //is awoken early (i.e. by the JVM before completion was called) 
                long elapsedWaitTime = 0L;
                do {
                    wait(waitTime - elapsedWaitTime);
                } while (!isDone() && (elapsedWaitTime = System.currentTimeMillis() - startTime) < waitTime);
            }

            if (!isDone())
                throw new TimeoutException();
        }

        if (_failure != null)
            throw _failure;

        return _result;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return _done;
    }

    public synchronized void complete(T result) {
        if (!_done) {
            _result = result;
            _done = true;
            notifyGetter();
        }
    }

    public synchronized void fail(Throwable t) {
        if (!_done) {
            _failure = new ExecutionException(t);
            _done = true;
            notifyGetter();
        }
    }

    private synchronized void notifyGetter() {
        this.notifyAll();
    }

    public void callListeners() {
        if (isDone()) {
            // Call listeners
            _notifiedListeners.compareAndSet(false, true);

            for (CompletionListener<T> l = _listeners.poll(); l != null; l = _listeners.poll()) {
                if (_failure != null) {
                    try {
                        l.failedCompletion(this, _failure.getCause());
                    } catch (Throwable t) {
                        // FFDC and ignore.
                    }
                } else {
                    l.successfulCompletion(this, _result);
                }
            }
        }
    }

    public void queueListener(CompletionListener<T> l) {
        _listeners.offer(l);
        if (_notifiedListeners.get()) {
            callListeners();
        }
    }
}