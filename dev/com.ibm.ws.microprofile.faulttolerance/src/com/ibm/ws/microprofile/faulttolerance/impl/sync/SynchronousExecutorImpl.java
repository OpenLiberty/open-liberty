/*******************************************************************************
 * Copyright (c) 2017,2018 IBM Corporation and others.
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

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
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
import com.ibm.ws.microprofile.faulttolerance.spi.MetricRecorder;
import com.ibm.ws.microprofile.faulttolerance.spi.RetryPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.TimeoutPolicy;

import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.SyncFailsafe;
import net.jodah.failsafe.function.CheckedFunction;

/**
 *
 */
public class SynchronousExecutorImpl<R> implements Executor<R> {

    private static final TraceComponent tc = Tr.register(SynchronousExecutorImpl.class);

    private TaskRunner<R> taskRunner;

    private TimeoutPolicy timeoutPolicy;
    private ScheduledExecutorService scheduledExecutorService;
    private CircuitBreakerImpl circuitBreaker;
    private FallbackPolicy fallbackPolicy;
    private RetryPolicy retryPolicy;
    protected MetricRecorder metricRecorder;

    //Standard constructor for a synchronous execution
    public SynchronousExecutorImpl(RetryPolicy retryPolicy,
                                   CircuitBreakerPolicy circuitBreakerPolicy,
                                   TimeoutPolicy timeoutPolicy,
                                   BulkheadPolicy bulkheadPolicy,
                                   FallbackPolicy fallbackPolicy,
                                   ScheduledExecutorService scheduledExecutorService,
                                   MetricRecorder metricRecorder) {

        this.timeoutPolicy = timeoutPolicy;
        this.scheduledExecutorService = scheduledExecutorService;
        this.metricRecorder = metricRecorder;

        if (circuitBreakerPolicy != null) {
            this.circuitBreaker = new CircuitBreakerImpl(circuitBreakerPolicy);
        }

        this.fallbackPolicy = fallbackPolicy;
        this.retryPolicy = retryPolicy;

        if (bulkheadPolicy == null) {
            this.taskRunner = new SimpleTaskRunner<R>();
        } else {
            this.taskRunner = new SemaphoreTaskRunner<R>(bulkheadPolicy, metricRecorder);
        }

        if (circuitBreaker != null) {
            circuitBreaker.onOpen(metricRecorder::reportCircuitOpen);
            circuitBreaker.onHalfOpen(metricRecorder::reportCircuitHalfOpen);
            circuitBreaker.onClose(metricRecorder::reportCircuitClosed);
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

        FTExecutionContext executionContext = new ExecutionContextImpl(id, method, params, timeout, this.circuitBreaker, this.fallbackPolicy, retry, metricRecorder);
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

    /**
     * Run after execution is complete (including all fault tolerance processing)
     * <p>
     * Subclasses can override this if they require different end of execution behaviour
     *
     * @param executionContext the execution context
     * @param t the exception thrown, or {@code null} if no exception was thrown
     */
    protected void executionComplete(ExecutionContextImpl executionContext, Throwable t) {
        executionContext.onFullExecutionComplete(t);
    }

    /**
     * Configure the failsafe executor
     * <p>
     * Configure any parameters and add any required callbacks.
     * <p>
     * Subclasses can override this if they require different behaviour
     *
     * @param failsafe the failsafe executor to configure
     * @param executionContextImpl the execution context
     */
    protected void configureFailsafe(SyncFailsafe<R> failsafe, ExecutionContextImpl executionContextImpl) {
        failsafe.onRetry((t) -> {
            executionContextImpl.onRetry(t);
        });

        failsafe.onComplete((r, t) -> {
            executionContextImpl.onMainExecutionComplete(t);
        });

        if (executionContextImpl.getCircuitBreaker() != null) {
            failsafe = failsafe.with(executionContextImpl.getCircuitBreaker());
        }

        if (executionContextImpl.getFallbackPolicy() != null) {
            @SuppressWarnings("unchecked")
            CheckedFunction<Throwable, R> fallback = (t) -> {
                executionContextImpl.onMainExecutionComplete(t);
                executionContextImpl.onFallback();
                return (R) executionContextImpl.getFallbackPolicy().getFallbackFunction().execute(executionContextImpl);
            };
            failsafe = failsafe.withFallback(fallback);
        }
    }

    /** {@inheritDoc} */
    @Override
    @FFDCIgnore({ net.jodah.failsafe.CircuitBreakerOpenException.class, net.jodah.failsafe.FailsafeException.class, java.lang.Throwable.class })
    public R execute(Callable<R> callable, ExecutionContext executionContext) {

        ExecutionContextImpl executionContextImpl = (ExecutionContextImpl) executionContext;

        SyncFailsafe<R> failsafe = Failsafe.with(executionContextImpl.getRetry());

        configureFailsafe(failsafe, executionContextImpl);

        Callable<R> task = createTask(callable, executionContextImpl);

        preRun(executionContextImpl);

        R result = null;
        Throwable failure = null;
        try {
            result = failsafe.get(task);
        } catch (net.jodah.failsafe.CircuitBreakerOpenException e) {
            failure = e;
            // Task was not run at all, make sure we run end of execution processing
            executionContextImpl.onMainExecutionComplete(e);
            throw new CircuitBreakerOpenException(e);
        } catch (net.jodah.failsafe.FailsafeException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Fault tolerance execution ended in failure: {0} - {1}", executionContextImpl.getMethod(), e);
            }
            failure = e;
            Throwable cause = e.getCause();
            if (cause instanceof FaultToleranceException) {
                throw (FaultToleranceException) cause;
            } else {
                throw new ExecutionException(cause);
            }
        } catch (Throwable t) {
            failure = t;
            throw t;
        } finally {
            executionComplete(executionContextImpl, failure);
        }

        return result;
    }

    @Override
    public void close() {
        // nothing to do
    }
}
