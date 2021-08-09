/*******************************************************************************
 * Copyright (c) 2017,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.faulttolerance.executor.impl;

import static com.ibm.ws.microprofile.faulttolerance.spi.MetricRecorder.FallbackOccurred.NO_FALLBACK;
import static com.ibm.ws.microprofile.faulttolerance.spi.MetricRecorder.FallbackOccurred.WITH_FALLBACK;
import static com.ibm.ws.microprofile.faulttolerance.spi.MetricRecorder.RetriesOccurred.NO_RETRIES;
import static com.ibm.ws.microprofile.faulttolerance.spi.MetricRecorder.RetriesOccurred.WITH_RETRIES;
import static com.ibm.ws.microprofile.faulttolerance.spi.RetryResultCategory.MAX_RETRIES_REACHED;
import static com.ibm.ws.microprofile.faulttolerance.spi.RetryResultCategory.NO_EXCEPTION;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.microprofile.faulttolerance.executor.impl.async.QueuedFuture;
import com.ibm.ws.microprofile.faulttolerance.spi.Executor;
import com.ibm.ws.microprofile.faulttolerance.spi.FTExecutionContext;
import com.ibm.ws.microprofile.faulttolerance.spi.FallbackPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.MetricRecorder;
import com.ibm.ws.microprofile.faulttolerance.spi.MetricRecorder.FallbackOccurred;
import com.ibm.ws.microprofile.faulttolerance.utils.FTDebug;

import net.jodah.failsafe.CircuitBreakerOpenException;

/**
 * Holds the data relating to a single execution of a FaultTolerance annotated method.
 * <p>
 * This class includes lots of lifecycle callback methods which are called from the Executor or from Failsafe:
 * <ul>
 * <li>{@link #start()} called right at the start before we do any processing</li>
 * <li>{@link #end()} called right after the user's method returns, before we do any processing on the returned value or exception</li>
 * <li>{@link #onRetry(Throwable)} called when we've determined that a method is going to be retried</li>
 * <li>{@link #onMainExecutionComplete(Throwable)} called when all processing apart from fallback is complete</li>
 * <li>{@link #onFullExecutionComplete(Throwable)} called when all processing including fallback is complete</li>
 * <li>{@link #onQueued()} called just before the execution task is added to the queue of an async bulkhead</li>
 * <li>{@link #onUnqueued()} called when the execution task is removed from the queue of an async bulkhead</li>
 * </ul>
 * <p>
 * Contrast this class with subclasses of {@link Executor} which hold data relating to an annotated method which is valid for all executions of that method.
 */
public class ExecutionContextImpl implements FTExecutionContext {

    private static final TraceComponent tc = Tr.register(ExecutionContextImpl.class);

    private final Method method;
    private final Object[] params;
    private final TimeoutImpl timeout;
    private final RetryImpl retry;

    private final CircuitBreakerImpl circuitBreaker;
    private final FallbackPolicy fallbackPolicy;
    private final MetricRecorder metricRecorder;

    private volatile int retries = 0;

    /**
     * The time that we started fault tolerance processing for this execution
     */
    private volatile long startTime;

    /**
     * The time that we started the current retry attempt
     */
    private volatile long attemptStartTime;

    /**
     * The time that the execution task was added to the bulkhead queue
     */
    private volatile long queueStartTime;

    /**
     * Whether or not a fallback has occurred
     */
    private volatile FallbackOccurred fallbackOccurred = NO_FALLBACK;

    /**
     * Anything thrown by the user's method - used for getFailure()
     */
    private volatile Throwable failure = null;

    private final String id;

    private volatile boolean closed = false;

    private boolean mainExecutionComplete = false;

    private QueuedFuture<?> queuedFuture = null;

    public ExecutionContextImpl(String id, Method method, Object[] params, TimeoutImpl timeout, CircuitBreakerImpl circuitBreaker, FallbackPolicy fallbackPolicy, RetryImpl retry,
                                MetricRecorder metricRecorder) {
        this.id = id;
        this.method = method;
        this.params = new Object[params.length];
        //TODO is an arraycopy really required here?
        System.arraycopy(params, 0, this.params, 0, params.length);

        this.timeout = timeout;
        this.circuitBreaker = circuitBreaker;
        this.fallbackPolicy = fallbackPolicy;
        this.retry = retry;
        this.metricRecorder = metricRecorder;

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
     * Returns any failure of the method executed or null
     *
     * @return Any Throwable thrown from the user's method or null
     *         No @Override as not in 1.0
     */
    public Throwable getFailure() {
        return failure;
    }

    public QueuedFuture<?> getQueuedFuture() {
        return queuedFuture;
    }

    public void setQueuedFuture(QueuedFuture<?> queuedFuture) {
        this.queuedFuture = queuedFuture;
    }

    /**
    *
    */
    public void start() {
        if (this.closed) {
            throw new IllegalStateException();
        }
        this.startTime = System.nanoTime();
        this.attemptStartTime = this.startTime;
        debugRelativeTime("start");
        if (timeout != null) {
            if (queuedFuture == null) {
                timeout.start(Thread.currentThread());
            } else {
                timeout.start(queuedFuture);
            }
        }
    }

    /**
    *
    */
    public void end() {
        debugRelativeTime("end");

        if (timeout != null) {
            timeout.stop();

            metricRecorder.recordTimeoutExecutionTime(System.nanoTime() - attemptStartTime);

            timeout.check();
        }
    }

    /**
     * Check if the timeout has "popped". If it has then it will throw a TimeoutException. If not then return the
     * time remaining, in nanoseconds.
     *
     * @return the time remaining on the timeout, in nanoseconds. If there is no timeout then return -1.
     */
    public long check() {
        debugRelativeTime("check");
        long remaining = -1;
        if (timeout != null) {
            remaining = timeout.check();
        }
        return remaining;
    }

    /**
     * Called when we have determined that this execution is going to be retried
     *
     * @param t the Throwable that prompted the retry
     */
    public void onRetry(Throwable t) {
        try {
            this.retries++;
            debugRelativeTime("onRetry: " + this.retries);
            metricRecorder.incrementRetriesCount();
            if (timeout != null) {
                timeout.restart();
                attemptStartTime = System.nanoTime();
            }
            onAttemptComplete(t);
        } catch (RuntimeException e) {
            // Unchecked exceptions thrown here can be swallowed by Failsafe
            // This catch ensures we at least get an FFDC
            throw e;
        }
    }

    /**
     * Called when all processing except fallback has occurred
     * <p>
     * May be called twice (before fallback and after fallback) but will only process the event the first time
     *
     * @param t the exception thrown, or {@code null} if no exception thrown
     */
    public void onMainExecutionComplete(Throwable t) {
        try {
            // May be called twice, don't do anything the second time
            if (mainExecutionComplete) {
                return;
            }
            mainExecutionComplete = true;

            this.failure = t;

            onAttemptComplete(t);

            if (t instanceof CircuitBreakerOpenException) {
                // We didn't run anything, execution context needs closing
                close();
            }

            if (retry != null) {
                if (retry.canRetryFor(null, t)) {
                    // This is a retryable failure
                    // Note in FT 1.x, we don't record the reason for stopping retrying
                    // so just assume we reached the max retries
                    metricRecorder.incrementRetryCalls(MAX_RETRIES_REACHED, WITH_RETRIES);
                } else {
                    if (retries > 0) {
                        metricRecorder.incrementRetryCalls(NO_EXCEPTION, WITH_RETRIES);
                    } else {
                        metricRecorder.incrementRetryCalls(NO_EXCEPTION, NO_RETRIES);
                    }
                }
            }
        } catch (Exception e) {
            // Unchecked exceptions thrown here can be swallowed by Failsafe
            // This catch ensures we at least get an FFDC
            throw e;
        }

    }

    /**
     * Called when all processing (including fallback) has occurred
     */
    public void onFullExecutionComplete(Throwable t) {
        if (t == null) {
            metricRecorder.incrementInvocationSuccessCount(fallbackOccurred);
        } else {
            metricRecorder.incrementInvocationFailedCount(fallbackOccurred);
        }
    }

    /**
     * Called at the end of each retry attempt
     */
    private void onAttemptComplete(Throwable t) {
        try {
            if (circuitBreaker != null) {
                if (t instanceof CircuitBreakerOpenException) {
                    metricRecorder.incrementCircuitBreakerCallsCircuitOpenCount();
                } else if (circuitBreaker.isFailure(null, t)) {
                    metricRecorder.incrementCircuitBreakerCallsFailureCount();
                } else {
                    metricRecorder.incrementCircuitBreakerCallsSuccessCount();
                }
            }

            if (t instanceof TimeoutException) {
                metricRecorder.incrementTimeoutTrueCount();
            } else {
                metricRecorder.incrementTimeoutFalseCount();
            }
        } catch (RuntimeException e) {
            // Unchecked exceptions thrown here can be swallowed by Failsafe
            // This catch ensures we at least get an FFDC
            throw e;
        }
    }

    /**
     * Called just before the fallback method or handler is run
     */
    public void onFallback() {
        fallbackOccurred = WITH_FALLBACK;
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
        // If using fallback or retry, stop the timeout and restart in synchronous mode
        if (timeout != null && (retry.getMaxRetries() != 0 || fallbackPolicy != null)) {
            timeout.runSyncOnNewThread(Thread.currentThread());
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

    /**
     * Called just before an execution task enqueued on the bulkhead
     */
    public void onQueued() {
        try {
            queueStartTime = System.nanoTime();
        } catch (RuntimeException e) {
            // Unchecked exceptions thrown here can be swallowed by Failsafe
            // This catch ensures we at least get an FFDC
            throw e;
        }
    }

    /**
     * Called when a previously queued execution task starts executing
     */
    public void onUnqueued() {
        try {
            metricRecorder.reportQueueWaitTime(System.nanoTime() - queueStartTime);
        } catch (RuntimeException e) {
            // Unchecked exceptions thrown here can be swallowed by Failsafe
            // This catch ensures we at least get an FFDC
            throw e;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void close() {
        //at the moment the only thing that might need to happen is to stop the timeout...
        //however, if execution has completed normally and as designed, the timeout will already be stopped
        //one day there might be more things that need closing
        if (this.timeout != null) {
            this.timeout.stop();
        }
        metricRecorder.recordTimeoutExecutionTime(System.nanoTime() - startTime);
        this.closed = true;
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
            FTDebug.debugRelativeTime(tc, getDescriptor(), message, this.startTime);
        }
    }

}
