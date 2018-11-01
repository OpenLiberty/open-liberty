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

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.ibm.ws.microprofile.faulttolerance.spi.TimeoutPolicy;
import com.ibm.ws.microprofile.faulttolerance20.state.TimeoutState;

public class TimeoutStateImpl implements TimeoutState {

    private final ScheduledExecutorService executorService;
    private final TimeoutPolicy policy;
    private TimeoutResult result = TimeoutResult.NEW;
    private Runnable timeoutCallback;

    public TimeoutStateImpl(ScheduledExecutorService executorService, TimeoutPolicy policy) {
        super();
        this.executorService = executorService;
        this.policy = policy;
    }

    /** {@inheritDoc} */
    @Override
    public void start(Runnable timeoutCallback) {
        synchronized (this) {
            if (result != TimeoutResult.NEW) {
                throw new IllegalStateException("Start called twice on the same timeout");
            }
            result = TimeoutResult.STARTED;
            this.timeoutCallback = timeoutCallback;
            executorService.schedule(this::timeout, policy.getTimeout().toMillis(), TimeUnit.MILLISECONDS);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void stop() {
        synchronized (this) {
            if (result == TimeoutResult.NEW) {
                throw new IllegalStateException("Stop called on a timeout that was never started");
            }

            if (result == TimeoutResult.STARTED) {
                result = TimeoutResult.FINISHED;
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
            if (result == TimeoutResult.STARTED) {
                result = TimeoutResult.TIMEDOUT;
                if (timeoutCallback != null) {
                    timeoutCallback.run();
                }
            }
        }
    }

    private enum TimeoutResult {
        NEW, STARTED, FINISHED, TIMEDOUT
    }
}
