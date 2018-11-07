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

import com.ibm.ws.microprofile.faulttolerance.impl.CircuitBreakerImpl;
import com.ibm.ws.microprofile.faulttolerance.spi.CircuitBreakerPolicy;
import com.ibm.ws.microprofile.faulttolerance20.impl.MethodResult;
import com.ibm.ws.microprofile.faulttolerance20.state.CircuitBreakerState;

public class CircuitBreakerStateImpl implements CircuitBreakerState {

    private final CircuitBreakerImpl circuitBreaker;

    public CircuitBreakerStateImpl(CircuitBreakerPolicy policy) {
        circuitBreaker = new CircuitBreakerImpl(policy);
    }

    /** {@inheritDoc} */
    @Override
    public boolean requestPermissionToExecute() {
        return circuitBreaker.allowsExecution();
    }

    /** {@inheritDoc} */
    @Override
    public void recordResult(MethodResult<?> result) {
        if (result.getFailure() == null) {
            circuitBreaker.recordResult(result.getResult());
        } else {
            circuitBreaker.recordFailure(result.getFailure());
        }
    }

}
