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

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.microprofile.faulttolerance.impl.async.QueuedFuture;
import com.ibm.ws.microprofile.faulttolerance.spi.FTExecutionContext;
import com.ibm.ws.microprofile.faulttolerance.spi.FallbackPolicy;

public class ExecutionContextImpl implements FTExecutionContext {

    private static final TraceComponent tc = Tr.register(ExecutionContextImpl.class);

    private final Method method;
    private final Object[] params;
    private final TimeoutImpl timeout;
    private final RetryImpl retry;

    private final CircuitBreakerImpl circuitBreaker;
    private final FallbackPolicy fallbackPolicy;

    private volatile int retries = 0;
    private volatile long startTime;

    private final String id;

    public ExecutionContextImpl(String id, Method method, Object[] params, TimeoutImpl timeout, CircuitBreakerImpl circuitBreaker, FallbackPolicy fallbackPolicy, RetryImpl retry) {
        this.id = id;
        this.method = method;
        this.params = new Object[params.length];
        //TODO is an arraycopy really required here?
        System.arraycopy(params, 0, this.params, 0, params.length);

        this.timeout = timeout;
        this.circuitBreaker = circuitBreaker;
        this.fallbackPolicy = fallbackPolicy;
        this.retry = retry;
    }

    /** {@inheritDoc} */
    @Override
    public Method getMethod() {
        return method;
    }

    /** {@inheritDoc} */
    @Override
    public Object[] getParameters() {
        return params;
    }

    /**
    *
    */
    public void start() {
        this.startTime = System.nanoTime();
        debugRelativeTime("start");
        if (timeout != null) {
            timeout.start(Thread.currentThread());
        }
    }

    public void start(QueuedFuture<?> future) {
        this.startTime = System.nanoTime();
        debugRelativeTime("start");
        if (timeout != null) {
            timeout.start(future);
        }
    }

    /**
    *
    */
    public void end() {
        debugRelativeTime("end");
        if (timeout != null) {
            timeout.stop(true);
        }
    }

    /**
    *
    */
    public long check() {
        debugRelativeTime("check");
        long remaining = -1;
        if (timeout != null) {
            remaining = timeout.check();
        }
        return remaining;
    }

    public void onRetry() {
        this.retries++;
        debugRelativeTime("onRetry: " + this.retries);
        if (timeout != null) {
            timeout.restart();
        }
    }

    public RetryImpl getRetry() {
        return retry;
    }

    public FallbackPolicy getFallbackPolicy() {
        return fallbackPolicy;
    }

    public CircuitBreakerImpl getCircuitBreaker() {
        return circuitBreaker;
    }

    /**
     * As an asynchronous execution moves from the outer part to the nested inner part, update the context's policies
     */
    public void setNested() {
        // Stop the timeout and restart in synchronous mode
        if (timeout != null) {
            timeout.restartOnNewThread(Thread.currentThread());
        }

        int retriesRemaining = this.retry.getMaxRetries() - this.retries;
        if (this.retry.getMaxDuration() != null) {
            long maxDuration = this.retry.getMaxDuration().toNanos();
            long now = System.nanoTime();
            long elapsed = now - this.startTime;

            long delay = this.retry.getDelay().toNanos();
            maxDuration = maxDuration - elapsed;

            if (maxDuration <= delay) {
                maxDuration = delay + 1;
                retriesRemaining = 0;
            }
            this.retry.withMaxDuration(maxDuration, TimeUnit.NANOSECONDS);
        }
        this.retry.withMaxRetries(retriesRemaining);
    }

    /**
     * @return
     */
    public TimeoutImpl getTimeout() {
        return this.timeout;
    }

    /** {@inheritDoc} */
    @Override
    public void close() {
        //TODO might need to do more here
        if (this.timeout != null) {
            this.timeout.stop();
        }
    }

    @Override
    @Trivial
    public String toString() {
        return getDescriptor();
    }

    @Trivial
    public String getDescriptor() {
        return "Execution Context[" + this.id + "]";
    }

    @Trivial
    private void debugRelativeTime(String message) {
        //System.out.println(getDescriptor() + " (" + FTConstants.relativeSeconds(startTime, System.nanoTime()) + "): " + message);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            FTConstants.debugRelativeTime(tc, getDescriptor(), message, this.startTime);
        }
    }

}
