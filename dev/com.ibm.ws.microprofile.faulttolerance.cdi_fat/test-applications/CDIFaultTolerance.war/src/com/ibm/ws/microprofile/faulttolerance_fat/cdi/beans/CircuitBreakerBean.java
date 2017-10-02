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
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;

import com.ibm.ws.microprofile.faulttolerance_fat.util.ConnectException;

@RequestScoped
public class CircuitBreakerBean {

    private int executionCounterA = 0;
    private int executionCounterB = 0;
    private int executionCounterD = 0;
    private int executionCounterE = 0;
    private int executionCounterF = 0;
    private int executionCounterG = 0;
    private int executionCounterH = 0;
    private int executionCounterI = 0;
    private int executionCounterJ = 0;
    private int executionCounterK = 0;
    private int executionCounterL = 0;

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
            System.out.println("serviceB: throw ConnectException on execution " + executionCounterB);
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

    @CircuitBreaker(requestVolumeThreshold = 3, failureRatio = 1.0)
    @Fallback(fallbackMethod = "serviceEFallback")
    public String serviceE() throws ConnectException {
        executionCounterE++;
        throw new ConnectException("serviceE: " + executionCounterE);
    }

    @CircuitBreaker(requestVolumeThreshold = 3, failureRatio = 1.0)
    @Retry(retryOn = { ConnectException.class }, maxRetries = 7)
    public String serviceF() throws ConnectException {
        executionCounterF++;
        throw new ConnectException("serviceF: " + executionCounterF);
    }

    @CircuitBreaker(requestVolumeThreshold = 3, failureRatio = 1.0)
    @Retry(retryOn = { ConnectException.class }, maxRetries = 1)
    public String serviceG() throws ConnectException {
        executionCounterG++;
        throw new ConnectException("serviceG: " + executionCounterG);
    }

    @Asynchronous
    @CircuitBreaker(requestVolumeThreshold = 3, failureRatio = 1.0)
    @Retry(retryOn = { ConnectException.class }, maxRetries = 7)
    public Future<String> serviceH() throws ConnectException {
        executionCounterH++;
        throw new ConnectException("serviceH: " + executionCounterH);
    }

    @Asynchronous
    @CircuitBreaker(requestVolumeThreshold = 3, failureRatio = 1.0)
    @Retry(retryOn = { ConnectException.class }, maxRetries = 1)
    public Future<String> serviceI() throws ConnectException {
        executionCounterI++;
        throw new ConnectException("serviceI: " + executionCounterI);
    }

    @CircuitBreaker(delay = 2, delayUnit = ChronoUnit.SECONDS, requestVolumeThreshold = 3, failureRatio = 1.0)
    public String serviceJ() throws ConnectException {
        executionCounterJ++;
        System.out.println("serviceJ: " + executionCounterJ);

        if (executionCounterJ > 2 && executionCounterJ < 6) {
            System.out.println("serviceJ: throw ConnectException on execution " + executionCounterJ);
            throw new ConnectException("serviceJ exception: " + executionCounterJ);
        }
        return "serviceJ: " + executionCounterJ;
    }

    /**
     * Set the requestVolumeThreshold to 5 - which would lead to test failure - but this bean's config
     * will be overridden in microprofile-config.properties to a value of 3.
     */
    @CircuitBreaker(delay = 2, delayUnit = ChronoUnit.SECONDS, requestVolumeThreshold = 5, failureRatio = 1.0)
    public String serviceK() throws ConnectException {
        executionCounterK++;

        if (executionCounterK <= 5) {
            throw new ConnectException("serviceK exception: " + executionCounterK);
        }
        return "serviceK: " + executionCounterK;
    }

    /**
     * Set the delay to 1 minute - which would lead to test failure - but this bean's config
     * will be overridden.
     */
    @CircuitBreaker(delay = 1, delayUnit = ChronoUnit.MINUTES, requestVolumeThreshold = 3, failureRatio = 1.0)
    public String serviceL() throws ConnectException {
        executionCounterL++;
        System.out.println("serviceL: " + executionCounterL);

        if (executionCounterL <= 3) {
            System.out.println("serviceL: throw ConnectException on execution " + executionCounterL);
            throw new ConnectException("serviceL exception: " + executionCounterL);
        }
        return "serviceL: " + executionCounterL;
    }

    public Future<String> serviceDFallback() {
        return CompletableFuture.completedFuture("serviceDFallback");
    }

    public String serviceEFallback() {
        return "serviceEFallback";
    }

    public int getExecutionCounterD() {
        return executionCounterD;
    }

    public int getExecutionCounterE() {
        return executionCounterE;
    }

    public int getExecutionCounterF() {
        return executionCounterF;
    }

    public int getExecutionCounterG() {
        return executionCounterG;
    }

    public int getExecutionCounterH() {
        return executionCounterH;
    }

    public int getExecutionCounterI() {
        return executionCounterI;
    }

    public int getExecutionCounterJ() {
        return executionCounterJ;
    }

    public int getExecutionCounterK() {
        return executionCounterK;
    }
}
