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
package com.ibm.ws.microprofile.faulttolerance20.state.impl;

import com.ibm.ws.microprofile.faulttolerance.impl.CircuitBreakerImpl;
import com.ibm.ws.microprofile.faulttolerance.spi.CircuitBreakerPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.MetricRecorder;
import com.ibm.ws.microprofile.faulttolerance20.impl.MethodResult;
import com.ibm.ws.microprofile.faulttolerance20.state.CircuitBreakerState;

public class CircuitBreakerStateImpl implements CircuitBreakerState {

    private final CircuitBreakerImpl circuitBreaker;
    private final MetricRecorder metricRecorder;

    public CircuitBreakerStateImpl(CircuitBreakerPolicy policy, MetricRecorder metricRecorder) {
        circuitBreaker = new CircuitBreakerImpl(policy);
        this.metricRecorder = metricRecorder;
        circuitBreaker.onClose(metricRecorder::reportCircuitClosed);
        circuitBreaker.onOpen(metricRecorder::reportCircuitOpen);
        circuitBreaker.onHalfOpen(metricRecorder::reportCircuitHalfOpen);
    }

    /** {@inheritDoc} */
    @Override
    public boolean requestPermissionToExecute() {
        boolean allowsExecution = circuitBreaker.allowsExecution();
        if (!allowsExecution) {
            metricRecorder.incrementCircuitBreakerCallsCircuitOpenCount();
        }
        return allowsExecution;
    }

    /** {@inheritDoc} */
    @Override
    public void recordResult(MethodResult<?> result) {
        if (result.getFailure() == null) {
            circuitBreaker.recordResult(result.getResult());
            metricRecorder.incrementCircuitBreakerCallsSuccessCount();
        } else {
            circuitBreaker.recordFailure(result.getFailure());
            if (circuitBreaker.isFailure(null, result.getFailure())) {
                metricRecorder.incrementCircuitBreakerCallsFailureCount();
            } else {
                metricRecorder.incrementCircuitBreakerCallsSuccessCount();
            }
        }
    }

}
