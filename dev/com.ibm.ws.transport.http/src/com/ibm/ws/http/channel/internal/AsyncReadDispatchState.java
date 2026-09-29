/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.http.channel.internal;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

import com.ibm.ws.http.dispatcher.internal.HttpDispatcher;
import com.ibm.ws.http.netty.NettyHttpConstants;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;

/**
 * Coordinates the channel-scoped callback and worker handoff for Netty async
 * request-body reads.
 */
public final class AsyncReadDispatchState {
    private static final AttributeKey<AsyncReadDispatchState> STATE = AttributeKey.valueOf("asyncReadDispatchState");

    private final Channel channel;
    private final Executor executor;
    private Registration armed;
    private boolean dispatched;
    private boolean failed;

    private AsyncReadDispatchState(Channel channel, Executor executor) {
        this.channel = channel;
        this.executor = executor;
    }

    public static AsyncReadDispatchState forChannel(Channel channel) {
        return forChannel(channel, null);
    }

    static AsyncReadDispatchState forChannel(Channel channel, Executor executor) {
        AsyncReadDispatchState state = channel.attr(STATE).get();
        if (state != null) {
            return state;
        }
        AsyncReadDispatchState created = new AsyncReadDispatchState(channel, executor);
        AsyncReadDispatchState existing = channel.attr(STATE).setIfAbsent(created);
        return existing == null ? created : existing;
    }

    public Registration arm(Runnable success, Runnable error) {
        synchronized (this) {
            Registration registration = new Registration(success, error);
            armed = registration;
            if (failed && !dispatched) {
                registration.signaled = true;
                registration.mode = Mode.ERROR;
                executeLocked(claimSignaledIfIdleLocked());
            }
            return registration;
        }
    }

    public void submitReady(Runnable success, Runnable error) {
        synchronized (this) {
            Registration registration = new Registration(success, error);
            registration.signaled = true;
            registration.mode = Mode.READY;
            armed = registration;
            executeLocked(claimSignaledIfIdleLocked());
        }
    }

    public void signal() {
        synchronized (this) {
            if (armed == null) {
                return;
            }
            armed.signaled = true;
            if (armed.mode != Mode.READY) {
                armed.mode = failed ? Mode.ERROR : Mode.SIGNAL;
            }
            executeLocked(claimSignaledIfIdleLocked());
        }
    }

    public void fail() {
        synchronized (this) {
            failed = true;
            if (dispatched || armed == null) {
                return;
            }
            armed.signaled = true;
            if (armed.mode != Mode.READY) {
                armed.mode = Mode.ERROR;
            }
            executeLocked(claimSignaledIfIdleLocked());
        }
    }

    public synchronized void clear(Registration expected) {
        if (armed == expected) {
            armed = null;
        }
    }

    public synchronized boolean clearIfIdle() {
        if (armed != null || dispatched || failed) {
            return false;
        }
        return true;
    }

    public synchronized boolean isDispatched() {
        return dispatched;
    }

    public synchronized boolean hasCallback() {
        return armed != null;
    }

    public synchronized boolean hasOutstandingCallback() {
        return armed != null || dispatched;
    }

    private Registration claimSignaledIfIdleLocked() {
        if (dispatched || armed == null || !armed.signaled) {
            return null;
        }
        Registration claimed = armed;
        armed = null;
        dispatched = true;
        return claimed;
    }

    private Registration finishAndTakeNextLocked() {
        if (armed != null && (armed.signaled || failed)) {
            Registration next = armed;
            armed = null;
            if (failed && next.mode != Mode.READY) {
                next.mode = Mode.ERROR;
            } else if (next.mode == null) {
                next.mode = Mode.SIGNAL;
            }
            return next;
        }
        dispatched = false;
        return null;
    }

    private boolean shouldRunError(Registration registration) {
        synchronized (this) {
            return registration.mode == Mode.ERROR
                            || registration.mode == Mode.SIGNAL
                                            && (failed || Boolean.TRUE.equals(channel.attr(NettyHttpConstants.INPUT_SHUTDOWN_PENDING).get()));
        }
    }

    private void executeLocked(Registration initial) {
        if (initial == null) {
            return;
        }
        AtomicBoolean started = new AtomicBoolean();
        try {
            executor().execute(() -> {
                started.set(true);
                run(initial);
            });
        } catch (RejectedExecutionException failure) {
            if (!started.get()) {
                restoreAfterRejectionLocked(initial);
            }
            throw failure;
        }
    }

    private Executor executor() {
        return executor == null ? HttpDispatcher.getExecutorService() : executor;
    }

    private void restoreAfterRejectionLocked(Registration rejected) {
        dispatched = false;
        rejected.signaled = true;
        if (rejected.mode == Mode.ERROR) {
            failed = true;
        }
        if (armed == null) {
            armed = rejected;
        }
    }

    private void run(Registration initial) {
        Registration current = initial;
        Throwable callbackFailure = null;
        while (current != null) {
            try {
                if (shouldRunError(current)) {
                    current.runError();
                } else {
                    current.runSuccess();
                }
            } catch (RuntimeException | Error failure) {
                if (callbackFailure == null) {
                    callbackFailure = failure;
                } else if (callbackFailure != failure) {
                    callbackFailure.addSuppressed(failure);
                }
            }
            synchronized (this) {
                current = finishAndTakeNextLocked();
            }
        }
        if (callbackFailure instanceof RuntimeException) {
            throw (RuntimeException) callbackFailure;
        }
        if (callbackFailure != null) {
            throw (Error) callbackFailure;
        }
    }

    private enum Mode {
        READY,
        SIGNAL,
        ERROR
    }

    public static final class Registration {
        private final Runnable success;
        private final Runnable error;
        private boolean signaled;
        private Mode mode;

        private Registration(Runnable success, Runnable error) {
            this.success = success;
            this.error = error;
        }

        private void runSuccess() {
            success.run();
        }

        private void runError() {
            if (error != null) {
                error.run();
            }
        }
    }
}
