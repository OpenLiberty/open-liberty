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
package com.ibm.ws.microprofile.faulttolerance.impl;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import com.ibm.ws.microprofile.faulttolerance.spi.CircuitBreakerPolicy;

import net.jodah.failsafe.CircuitBreaker;

/**
 *
 */
public class CircuitBreakerImpl extends CircuitBreaker {

    private final boolean async;
    private volatile boolean nested = false;

    public CircuitBreakerImpl(CircuitBreakerPolicy policy, boolean async) {
        this.async = async;

        Duration delay = policy.getDelay();
        Class<? extends Throwable>[] failOn = policy.getFailOn();
        double failureRatio = policy.getFailureRatio();
        int requestVolumeThreshold = policy.getRequestVolumeThreshold();
        int successThreshold = policy.getSuccessThreshold();

        failOn(failOn);

        if (delay.toMillis() > 0) {
            withDelay(delay.toMillis(), TimeUnit.MILLISECONDS);
        }

        int failures = (int) (failureRatio * requestVolumeThreshold);
        //TODO should failures be rounded up or down?
        int executions = requestVolumeThreshold;

        withFailureThreshold(failures, executions);
        withSuccessThreshold(successThreshold);
    }

    @Override
    public void recordSuccess() {
        if (!async || nested) {
            super.recordSuccess();
        }
    }

    public void setNested() {
        this.nested = true;
    }
}
