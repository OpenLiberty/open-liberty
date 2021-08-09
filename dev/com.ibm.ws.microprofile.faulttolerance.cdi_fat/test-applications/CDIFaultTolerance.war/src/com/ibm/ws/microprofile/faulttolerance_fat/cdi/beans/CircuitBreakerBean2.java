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
package com.ibm.ws.microprofile.faulttolerance_fat.cdi.beans;

import java.time.temporal.ChronoUnit;

import javax.enterprise.context.RequestScoped;

import org.eclipse.microprofile.faulttolerance.CircuitBreaker;

import com.ibm.ws.microprofile.faulttolerance_fat.util.ConnectException;

@RequestScoped
/**
 * Set the requestVolumeThreshold to 5 - which would lead to test failure - but this bean's config
 * will be overridden in microprofile-config.properties.
 */
@CircuitBreaker(delay = 2, delayUnit = ChronoUnit.SECONDS, requestVolumeThreshold = 5, failureRatio = 1.0)
public class CircuitBreakerBean2 {

    private int executionCounterA = 0;

    // Inherit class CircuitBreaker Policy
    public String serviceA() throws ConnectException {
        executionCounterA++;

        if (executionCounterA <= 5) {
            throw new ConnectException("serviceA exception: " + executionCounterA);
        }
        return "serviceA: " + executionCounterA;
    }

    public int getExecutionCounterA() {
        return executionCounterA;
    }
}
