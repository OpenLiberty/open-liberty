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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import javax.enterprise.context.RequestScoped;

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Timeout;

import com.ibm.ws.microprofile.faulttolerance_fat.util.ConnectException;

@RequestScoped
public class CircuitBreakerBean {

    private int executionCounterA = 0;
    private int executionCounterB = 0;
    private int executionCounterD = 0;

    @CircuitBreaker(delay = 1, delayUnit = ChronoUnit.SECONDS, requestVolumeThreshold = 3, failureRatio = 1.0)
    @Timeout(value = 3, unit = ChronoUnit.SECONDS)
    public String serviceA() {
        executionCounterA++;
        System.out.println("serviceA: " + executionCounterA);

        if (executionCounterA <= 3) {
            //Sleep for 10 secs to force a timeout
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                System.out.println("serviceA interrupted");
            }
        }
        return "serviceA: " + executionCounterA;
    }

    @CircuitBreaker(delay = 2, delayUnit = ChronoUnit.SECONDS, requestVolumeThreshold = 3, failureRatio = 1.0)
    public String serviceB() throws ConnectException {
        executionCounterB++;
        System.out.println("serviceB: " + executionCounterB);

        if (executionCounterB <= 3) {
            throw new ConnectException("serviceB exception: " + executionCounterB);
        }
        return "serviceB: " + executionCounterB;
    }

    @Asynchronous
    @CircuitBreaker(requestVolumeThreshold = 3, failureRatio = 1.0)
    public Future<String> serviceC() throws ConnectException {
        throw new ConnectException("serviceC");
    }

    @Asynchronous
    @CircuitBreaker(requestVolumeThreshold = 3, failureRatio = 1.0)
    @Fallback(fallbackMethod = "serviceDFallback")
    public Future<String> serviceD() throws ConnectException {
        executionCounterD++;
        throw new ConnectException("serviceD: " + executionCounterD);
    }

    public Future<String> serviceDFallback() {
        return CompletableFuture.completedFuture("serviceDFallback");
    }

    public int getExecutionCounterD() {
        return executionCounterD;
    }
}
