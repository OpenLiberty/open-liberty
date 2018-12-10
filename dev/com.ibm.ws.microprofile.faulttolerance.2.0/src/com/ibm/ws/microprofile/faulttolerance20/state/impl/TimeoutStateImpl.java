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
package com.ibm.ws.microprofile.faulttolerance20.state.impl;

import static com.ibm.ws.microprofile.faulttolerance20.state.impl.DurationUtils.asClampedNanos;

import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.ibm.ws.microprofile.faulttolerance.spi.TimeoutPolicy;
import com.ibm.ws.microprofile.faulttolerance20.state.TimeoutState;

public class TimeoutStateImpl implements TimeoutState {

    private final ScheduledExecutorService executorService;
    private final TimeoutPolicy policy;
    private TimeoutResult result = TimeoutResult.NEW;
    private Runnable timeoutCallback;
    private Future<?> timeoutFuture;

    public TimeoutStateImpl(ScheduledExecutorService executorService, TimeoutPolicy policy) {
        super();
        this.executorService = executorService;
        this.policy = policy;
    }

    /** {@inheritDoc} */
    @Override
    public void start() {
        synchronized (this) {
            if (result != TimeoutResult.NEW) {
                throw new IllegalStateException("Start called twice on the same timeout");
            }
            result = TimeoutResult.STARTED;
            if (!policy.getTimeout().isZero()) {
                timeoutFuture = executorService.schedule(this::timeout, asClampedNanos(policy.getTimeout()), TimeUnit.NANOSECONDS);
            }
        }
    }

    @Override
    public void setTimeoutCallback(Runnable timeoutCallback) {
        synchronized (this) {
            if (timeoutCallback == null) {
                throw new NullPointerException("setTimeoutCallback called with null value");
            }

            if (this.timeoutCallback != null) {
                throw new IllegalStateException("setTimeoutCallback called more than once");
            }

            this.timeoutCallback = timeoutCallback;

            // If we've already timed out, run the callback now
            if (result == TimeoutResult.TIMEDOUT && timeoutCallback != null) {
                timeoutCallback.run();
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void stop() {
        synchronized (this) {
            switch (result) {
                case NEW:
                    throw new IllegalStateException("Stop called on a timeout that was never started");
                case STARTED:
                    result = TimeoutResult.FINISHED;
                    if (timeoutFuture != null) {
                        timeoutFuture.cancel(false);
                    }
                    break;
                case FINISHED:
                    throw new IllegalStateException("Stop called twice on the same timeout");
                case TIMEDOUT:
                    // Do nothing
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean isTimedOut() {
        synchronized (this) {
            return result == TimeoutResult.TIMEDOUT;
        }
    }

    private void timeout() {
        synchronized (this) {
            switch (result) {
                case NEW:
                    throw new IllegalStateException("Timeout called on a timeout that was never started");
                case STARTED:
                    result = TimeoutResult.TIMEDOUT;
                    if (timeoutCallback != null) {
                        timeoutCallback.run();
                    }
                    break;
                case FINISHED:
                    // Do nothing
                case TIMEDOUT:
                    throw new IllegalStateException("Timeout called more than once on the same timeout");
            }
        }
    }

    private enum TimeoutResult {
        NEW, STARTED, FINISHED, TIMEDOUT
    }
}
