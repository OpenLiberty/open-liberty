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
package com.ibm.ws.microprofile.faulttolerance.impl.sync;

import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.microprofile.faulttolerance.ExecutionContext;
import org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException;
import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceException;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.microprofile.faulttolerance.impl.CircuitBreakerImpl;
import com.ibm.ws.microprofile.faulttolerance.impl.ExecutionContextImpl;
import com.ibm.ws.microprofile.faulttolerance.impl.RetryImpl;
import com.ibm.ws.microprofile.faulttolerance.impl.TaskRunner;
import com.ibm.ws.microprofile.faulttolerance.impl.TimeoutImpl;
import com.ibm.ws.microprofile.faulttolerance.spi.BulkheadPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.CircuitBreakerPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.ExecutionException;
import com.ibm.ws.microprofile.faulttolerance.spi.Executor;
import com.ibm.ws.microprofile.faulttolerance.spi.FTExecutionContext;
import com.ibm.ws.microprofile.faulttolerance.spi.FallbackPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.RetryPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.TimeoutPolicy;

import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.SyncFailsafe;

/**
 *
 */
public class SynchronousExecutorImpl<R> implements Executor<R> {

    private TaskRunner<R> taskRunner;

    private TimeoutPolicy timeoutPolicy;
    private ScheduledExecutorService scheduledExecutorService;
    private CircuitBreakerImpl circuitBreaker;
    private FallbackPolicy fallbackPolicy;
    private RetryPolicy retryPolicy;

    //Standard constructor for a synchronous execution
    public SynchronousExecutorImpl(RetryPolicy retryPolicy,
                                   CircuitBreakerPolicy circuitBreakerPolicy,
                                   TimeoutPolicy timeoutPolicy,
                                   BulkheadPolicy bulkheadPolicy,
                                   FallbackPolicy fallbackPolicy,
                                   ScheduledExecutorService scheduledExecutorService) {

        this.timeoutPolicy = timeoutPolicy;
        this.scheduledExecutorService = scheduledExecutorService;

        if (circuitBreakerPolicy != null) {
            this.circuitBreaker = new CircuitBreakerImpl(circuitBreakerPolicy);
        }

        this.fallbackPolicy = fallbackPolicy;
        this.retryPolicy = retryPolicy;

        if (bulkheadPolicy == null) {
            this.taskRunner = new SimpleTaskRunner<R>();
        } else {
            this.taskRunner = new SemaphoreTaskRunner<R>(bulkheadPolicy);
        }

    }

    //internal constructor for the nested synchronous part of an asynchronous execution
    protected SynchronousExecutorImpl() {}

    @Override
    public FTExecutionContext newExecutionContext(String id_prefix, Method method, Object... params) {

        String id = id_prefix + "_" + UUID.randomUUID();
        TimeoutImpl timeout = null;
        if (this.timeoutPolicy != null && !this.timeoutPolicy.getTimeout().isZero()) {
            timeout = new TimeoutImpl(id, this.timeoutPolicy, this.scheduledExecutorService);
        }

        RetryImpl retry = new RetryImpl(this.retryPolicy);

        FTExecutionContext executionContext = new ExecutionContextImpl(id, method, params, timeout, this.circuitBreaker, this.fallbackPolicy, retry);
        return executionContext;
    }

    protected Callable<R> createTask(Callable<R> callable, ExecutionContextImpl executionContext) {
        Callable<R> task = () -> {
            R result = this.taskRunner.runTask(callable, executionContext);
            return result;
        };
        return task;
    }

    protected void preRun(ExecutionContextImpl executionContext) {
        executionContext.start();
    }

    protected boolean enableCircuitBreaker() {
        return true;
    }

    protected boolean enableFallback() {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    @FFDCIgnore({ net.jodah.failsafe.CircuitBreakerOpenException.class, net.jodah.failsafe.FailsafeException.class })
    public R execute(Callable<R> callable, ExecutionContext executionContext) {

        ExecutionContextImpl executionContextImpl = (ExecutionContextImpl) executionContext;

        SyncFailsafe<R> failsafe = Failsafe.with(executionContextImpl.getRetry());

        failsafe.onRetry((t) -> {
            executionContextImpl.onRetry();
        });

        if (executionContextImpl.getCircuitBreaker() != null && enableCircuitBreaker()) {
            failsafe = failsafe.with(executionContextImpl.getCircuitBreaker());
        }

        if (executionContextImpl.getFallbackPolicy() != null && enableFallback()) {
            @SuppressWarnings("unchecked")
            Callable<R> fallback = () -> {
                return (R) executionContextImpl.getFallbackPolicy().getFallbackFunction().execute(executionContext);
            };
            failsafe = failsafe.withFallback(fallback);
        }
        Callable<R> task = createTask(callable, executionContextImpl);

        preRun(executionContextImpl);

        R result = null;
        try {
            result = failsafe.get(task);
        } catch (net.jodah.failsafe.CircuitBreakerOpenException e) {
            // Task was not run at all, make sure we end the execution context
            executionContextImpl.close();
            throw new CircuitBreakerOpenException(e);
        } catch (net.jodah.failsafe.FailsafeException e) {
            Throwable cause = e.getCause();
            if (cause instanceof FaultToleranceException) {
                throw (FaultToleranceException) cause;
            } else {
                throw new ExecutionException(cause);
            }
        }

        return result;
    }

    @Override
    public void close() {
        // nothing to do
    }
}
