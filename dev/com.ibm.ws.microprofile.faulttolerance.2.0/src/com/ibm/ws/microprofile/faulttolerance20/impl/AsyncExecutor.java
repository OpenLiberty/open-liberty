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
package com.ibm.ws.microprofile.faulttolerance20.impl;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.microprofile.faulttolerance.ExecutionContext;
import org.eclipse.microprofile.faulttolerance.exceptions.BulkheadException;
import org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException;
import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceException;
import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.microprofile.faulttolerance.spi.BulkheadPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.CircuitBreakerPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.Executor;
import com.ibm.ws.microprofile.faulttolerance.spi.FTExecutionContext;
import com.ibm.ws.microprofile.faulttolerance.spi.FallbackPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.RetryPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.TimeoutPolicy;
import com.ibm.ws.microprofile.faulttolerance20.state.AsyncBulkheadState;
import com.ibm.ws.microprofile.faulttolerance20.state.AsyncBulkheadState.ExceptionHandler;
import com.ibm.ws.microprofile.faulttolerance20.state.AsyncBulkheadState.ExecutionReference;
import com.ibm.ws.microprofile.faulttolerance20.state.CircuitBreakerState;
import com.ibm.ws.microprofile.faulttolerance20.state.FaultToleranceStateFactory;
import com.ibm.ws.microprofile.faulttolerance20.state.RetryState;
import com.ibm.ws.microprofile.faulttolerance20.state.RetryState.RetryResult;
import com.ibm.ws.microprofile.faulttolerance20.state.TimeoutState;
import com.ibm.ws.threading.PolicyExecutorProvider;

/**
 * Abstract executor for asynchronous calls.
 * <p>
 * Asynchronous calls return their result inside a wrapper (currently either a {@link Future} or a {@link CompletionStage}).
 * <p>
 * When this executor is called, it must create and instance of the wrapper and return that immediately, while execution of the method takes place on another thread. Once the
 * execution of the method is complete, the wrapper instance must be updated with the result of the method execution.
 * <p>
 * If an internal exception occurs, this may be thrown directly from the {@link #execute(Callable, ExecutionContext)} method, or logged and propagated back to the user via the
 * result wrapper.
 *
 * @param <W> the return type of the code being executed, which is also the type of the return wrapper (e.g. {@code Future<String>})
 */
public abstract class AsyncExecutor<W> implements Executor<W> {

    private static final TraceComponent tc = Tr.register(AsyncExecutor.class);

    public AsyncExecutor(RetryPolicy retry, CircuitBreakerPolicy cbPolicy, TimeoutPolicy timeoutPolicy, FallbackPolicy fallbackPolicy, BulkheadPolicy bulkheadPolicy,
                         ScheduledExecutorService executorService, PolicyExecutorProvider policyExecutorProvider) {
        retryPolicy = retry;
        circuitBreaker = FaultToleranceStateFactory.INSTANCE.createCircuitBreakerState(cbPolicy);
        this.timeoutPolicy = timeoutPolicy;
        this.executorService = executorService;
        this.fallbackPolicy = fallbackPolicy;
        bulkhead = FaultToleranceStateFactory.INSTANCE.createAsyncBulkheadState(policyExecutorProvider, executorService, bulkheadPolicy);
    }

    private final RetryPolicy retryPolicy;
    private final CircuitBreakerState circuitBreaker;
    private final ScheduledExecutorService executorService;
    private final TimeoutPolicy timeoutPolicy;
    private final FallbackPolicy fallbackPolicy;
    private final AsyncBulkheadState bulkhead;

    @Override
    public W execute(Callable<W> callable, ExecutionContext context) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Fault tolerance asynchronous execution started for {0}", context.getMethod());
        }

        @SuppressWarnings("unchecked")
        AsyncExecutionContextImpl<W> executionContext = (AsyncExecutionContextImpl<W>) context;

        executionContext.setCallable(callable);

        W returnWrapper = createReturnWrapper(executionContext);
        executionContext.setReturnWrapper(returnWrapper);

        RetryState retryState = FaultToleranceStateFactory.INSTANCE.createRetryState(retryPolicy);
        executionContext.setRetryState(retryState);
        retryState.start();

        enqueueAttempt(executionContext);

        return returnWrapper;
    }

    /**
     * Creates a wrapper instance that will be returned to the user
     *
     * @return the wrapper instance
     */
    abstract protected W createReturnWrapper(AsyncExecutionContextImpl<W> executionContext);

    /**
     * Stores the result of the execution inside the wrapper instance
     *
     * @param executionContext the execution context
     * @param result the method result
     */
    abstract protected void commitResult(AsyncExecutionContextImpl<W> executionContext, MethodResult<W> result);

    /**
     * Enqueue an execution attempt on the bulkhead
     */
    private void enqueueAttempt(AsyncExecutionContextImpl<W> executionContext) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Method {0} enqueuing new execution attempt", executionContext.getMethod());
        }

        AsyncAttemptContextImpl<W> attemptContext = new AsyncAttemptContextImpl<>(executionContext);

        TimeoutState timeout = FaultToleranceStateFactory.INSTANCE.createTimeoutState(executorService, timeoutPolicy);
        attemptContext.setTimeoutState(timeout);

        if (!circuitBreaker.requestPermissionToExecute()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Method {0} Circuit Breaker open, not executing", executionContext.getMethod());
            }

            MethodResult<W> result = MethodResult.failure(new CircuitBreakerOpenException());
            finalizeAttempt(attemptContext, result);
            return;
        }

        timeout.start();

        ExecutionReference ref = bulkhead.submit(handleExceptions(() -> runExecutionAttempt(attemptContext), attemptContext, executionContext),
                                                 getExceptionHandler(attemptContext));

        if (!ref.wasAccepted()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Method {0} bulkhead rejected execution", executionContext.getMethod());
            }

            finalizeAttempt(attemptContext, MethodResult.failure(new BulkheadException()));
            return;
        }

        timeout.setTimeoutCallback(handleExceptions(() -> timeout(attemptContext, ref), attemptContext, executionContext));

        // Update what we should do if the user cancels the execution
        executionContext.setCancelCallback((mayInterrupt) -> {
            ref.abort(mayInterrupt);
            finalizeAttempt(attemptContext, MethodResult.failure(new CancellationException()));
        });
    }

    /**
     * Run one attempt at the execution {@link #execute(Callable, ExecutionContext)}
     */
    @FFDCIgnore(Throwable.class)
    private void runExecutionAttempt(AsyncAttemptContextImpl<W> attemptContext) {
        AsyncExecutionContextImpl<W> executionContext = attemptContext.getExecutionContext();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Method {0} running execution attempt", executionContext.getMethod());
        }

        MethodResult<W> methodResult;
        try {
            W result = executionContext.getCallable().call();
            methodResult = MethodResult.success(result);
        } catch (Throwable e) {
            methodResult = MethodResult.failure(e);
        }
        attemptContext.getTimeoutState().stop();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Method {0} attempt execution reuslt: {1}", executionContext.getMethod(), methodResult);
        }

        if (attemptContext.getTimeoutState().isTimedOut()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Method {0} attempt finished but has timed out. Result discarded.", executionContext.getMethod());
            }

            Thread.interrupted(); // Clear interrupted thread
            return; // Exit here, the timeout callback will call finalizeAttempt
        }

        finalizeAttempt(attemptContext, methodResult);
    }

    /**
     * Run if a method times out.
     */
    private void timeout(AsyncAttemptContextImpl<W> attemptContext, ExecutionReference ref) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Method {0} timed out, attempting to cancel execution attempt", attemptContext.getExecutionContext().getMethod());
        }

        ref.abort(true);

        MethodResult<W> result = MethodResult.failure(new TimeoutException());
        finalizeAttempt(attemptContext, result);
    }

    /**
     * Process the result of an attempt
     * <p>
     * This method is usually called from {@link #runExecutionAttempt(AsyncAttemptContextImpl)} at the end of the attempt or from
     * {@link #timeout(AsyncAttemptContextImpl, ExecutionReference)} in response to a timeout. In the case of an unexpected exception, it can also be called from
     * {@link #handleExceptions(Runnable, AsyncAttemptContextImpl, AsyncExecutionContextImpl)}.
     * <p>
     * To ensure that the finalize logic is only run once, this method will do nothing if called a second time for the same attempt context.
     * <p>
     * In regular execution, this method will enqueue another attempt if a retry is needed or run any fallback logic and commit the result if no retry is needed.
     * <p>
     * In the case of an internal exception, this method will commit the result directly, skipping any fallback or retry logic.
     */
    private void finalizeAttempt(AsyncAttemptContextImpl<W> attemptContext, MethodResult<W> result) {
        AsyncExecutionContextImpl<W> executionContext = attemptContext.getExecutionContext();

        try {

            if (!attemptContext.end()) {
                // This attempt has already been finalized (probably because an error occurred)
                // Whatever the reason, we don't want to run the finalization logic again
                return;
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Method {0} processing end of attempt execution. Result: {1}", executionContext.getMethod(), result);
            }

            circuitBreaker.recordResult(result);

            // Note: don't process retries or fallback for internal failures or if the user has cancelled the execution
            if (!result.isInternalFailure() && !executionContext.isCancelled()) {
                RetryResult retryResult = executionContext.getRetryState().recordResult(result);
                if (retryResult.shouldRetry()) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                        Tr.event(tc, "Method {0} retrying with delay: {1} {2}", executionContext.getMethod(), retryResult.getDelay(), retryResult.getDelayUnit());
                    }
                    if (retryResult.getDelay() > 0) {
                        executorService.schedule(handleExceptions(() -> enqueueAttempt(executionContext), null, executionContext),
                                                 retryResult.getDelay(),
                                                 retryResult.getDelayUnit());
                    } else {
                        enqueueAttempt(executionContext);
                    }
                    // We've enqueued the retry to run, exit here
                    return;
                } else {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                        Tr.event(tc, "Method {0} not retrying", executionContext.getMethod());
                    }
                }

                result = runFallback(result, executionContext);
            }

            commitResult(executionContext, result);

        } catch (Throwable t) {
            // This method is used as a general error handler, so we need some special logic in case something goes wrong while handling the error
            MethodResult<W> errorResult = MethodResult.internalFailure(new FaultToleranceException(Tr.formatMessage(tc, "internal.error.CWMFT4998E", t), t));
            commitResult(attemptContext.getExecutionContext(), errorResult);
            throw t;
        }
    }

    @SuppressWarnings("unchecked")
    private MethodResult<W> runFallback(MethodResult<W> result, AsyncExecutionContextImpl<W> executionContext) {
        // TODO: we need to make sure we're on the right thread and have the right context when we run fallback
        // I.e. on BulkheadException, we're still on calling thread, on TimeoutException we're on timer thread without context
        if (result.getFailure() == null || fallbackPolicy == null) {
            return result;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Method {0} calling fallback", executionContext.getMethod());
        }

        executionContext.setFailure(result.getFailure());
        try {
            result = MethodResult.success((W) fallbackPolicy.getFallbackFunction().execute(executionContext));
        } catch (Throwable ex) {
            result = MethodResult.failure(ex);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Method {0} fallback result: {1}", executionContext.getMethod(), result);
        }

        return result;
    }

    @Override
    public FTExecutionContext newExecutionContext(String id, Method method, Object... parameters) {
        return new AsyncExecutionContextImpl<W>(method, parameters);
    }

    @Override
    public void close() {
        bulkhead.shutdown();
    }

    /**
     * Wraps a runnable inside another runnable which ensures that any exceptions it throws are handled
     * <p>
     * Useful when we schedule a task to run but don't intend to check its result because it shouldn't throw an exception.
     * <p>
     * The strategy used to handle exceptions is as follows:
     * <ul>
     * <li>Log and FFDC the exception
     * <li>If an attemptContext was passed, call finalizeContext, reporting the exception as an internal error
     * <li>If an attemptContext was not passed, call commitResult, reporting the exception as the result
     * </ul>
     *
     * @param runnable the runnable to wrap
     * @param attemptContext the attempt context associated with the runnable, may be {@code null}
     * @param executionContext the execution context associated with the runnable
     * @return a new runnable that calls {@code runnable} and logs any exceptions thrown
     */
    private Runnable handleExceptions(Runnable runnable, AsyncAttemptContextImpl<W> attemptContext, AsyncExecutionContextImpl<W> executionContext) {
        return () -> {
            try {
                runnable.run();
            } catch (Throwable t) {
                handleException(t, attemptContext, executionContext);
            }
        };
    }

    private ExceptionHandler getExceptionHandler(AsyncAttemptContextImpl<W> attemptContext) {
        return (e) -> {
            handleException(e, attemptContext, attemptContext.getExecutionContext());
        };
    }

    private void handleException(Throwable t, AsyncAttemptContextImpl<W> attemptContext, AsyncExecutionContextImpl<W> executionContext) {
        Tr.error(tc, "internal.error.CWMFT4998E", t);
        MethodResult<W> result = MethodResult.internalFailure(new FaultToleranceException(Tr.formatMessage(tc, "internal.error.CWMFT4998E", t), t));
        if (attemptContext != null) {
            finalizeAttempt(attemptContext, result);
        } else {
            commitResult(executionContext, result);
        }
    }

}
