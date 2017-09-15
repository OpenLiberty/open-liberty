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

import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.threading.FutureMonitor;
import com.ibm.ws.threading.listeners.CompletionListener;

public class FutureMonitorImpl implements FutureMonitor {

    public static final class FutureMonitorInfo<T> {
        private final Future<T> _future;
        private final CompletionListener<T> _listener;

        /**
         * @param work
         * @param l
         */
        public FutureMonitorInfo(Future<T> work, CompletionListener<T> l) {
            _future = work;
            _listener = l;
        }

        public boolean test() {
            return _future.isDone();
        }

        @FFDCIgnore({ ExecutionException.class, TimeoutException.class })
        public boolean notifyListener() {
            try {
                T result = _future.get(100, TimeUnit.MILLISECONDS);
                _listener.successfulCompletion(_future, result);
            } catch (InterruptedException e) {
                return false;
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                try {
                    _listener.failedCompletion(_future, cause);
                } catch (Throwable t) {
                    // FFDC and ignore to keep the monitor thread running.
                }
            } catch (TimeoutException e) {
                return false;
            }

            return true;
        }
    }

    private final Queue<FutureMonitorInfo<?>> _monitor = new ConcurrentLinkedQueue<FutureMonitorInfo<?>>();
    private ScheduledExecutorService _executor;
    private final AtomicBoolean _threadRunning = new AtomicBoolean(false);

    private final Runnable _thread = new Runnable() {
        @Override
        public void run() {
            Iterator<FutureMonitorInfo<?>> iterator = _monitor.iterator();
            while (iterator.hasNext()) {
                FutureMonitorInfo<?> info = iterator.next();
                if (info.test() && info.notifyListener())
                    iterator.remove();
            }
            _threadRunning.set(false);

            if (_monitor.isEmpty())
                return;
            if (_threadRunning.compareAndSet(false, true))
                _executor.schedule(_thread, 100, TimeUnit.MILLISECONDS);
        }
    };

    /** {@inheritDoc} */
    @Override
    public <T> void onCompletion(Future<T> work, CompletionListener<T> l) {
        boolean queue = true;

        FutureMonitorInfo<T> info = new FutureMonitorInfo<T>(work, l);

        if (info.test()) {
            queue = !!!info.notifyListener();
        }

        if (queue && work instanceof NonCancelableResultFuture<?>) {
            ((NonCancelableResultFuture<T>) work).queueListener(l);
            queue = false;
        }

        if (queue) {
            _monitor.add(info);
            if (_threadRunning.compareAndSet(false, true)) {
                // run thread
                _executor.schedule(_thread, 100, TimeUnit.MILLISECONDS);
            }
        }
    }

    public void setExecutor(ScheduledExecutorService ses) {
        _executor = ses;
    }

    public void unsetExecutor(ScheduledExecutorService ses) {
        _executor = null;
    }

    @Override
    public <T> Future<T> createFuture(Class<T> type) {
        return new NonCancelableResultFuture<T>();
    }

    @Override
    public <T> Future<T> createFutureWithResult(T result) {
        return new ImmediateFutureImpl<T>(result);
    }

    @Override
    public <T> Future<T> createFutureWithResult(Class<T> type, Throwable t) {
        return new ImmediateFutureImpl<T>(t);
    }

    @Override
    public <T> void setResult(Future<T> future, T result) {
        if (future instanceof NonCancelableResultFuture<?>) {
            NonCancelableResultFuture<T> futureImpl = (NonCancelableResultFuture<T>) future;
            futureImpl.complete(result);
            futureImpl.callListeners();
        }
    }

    @Override
    public void setResult(Future<?> future, Throwable error) {
        if (future instanceof NonCancelableResultFuture<?>) {
            NonCancelableResultFuture<?> futureImpl = (NonCancelableResultFuture<?>) future;
            futureImpl.fail(error);
            futureImpl.callListeners();
        }

    }
}