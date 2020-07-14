/*******************************************************************************
 * Copyright (c) 2018, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.faulttolerance20.impl;

import static com.ibm.ws.microprofile.faulttolerance.spi.MetricRecorder.FallbackOccurred.NO_FALLBACK;
import static com.ibm.ws.microprofile.faulttolerance.spi.MetricRecorder.FallbackOccurred.WITH_FALLBACK;

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
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.microprofile.faulttolerance.spi.BulkheadPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.CircuitBreakerPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.Executor;
import com.ibm.ws.microprofile.faulttolerance.spi.FTExecutionContext;
import com.ibm.ws.microprofile.faulttolerance.spi.FallbackPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.MetricRecorder;
import com.ibm.ws.microprofile.faulttolerance.spi.MetricRecorder.FallbackOccurred;
import com.ibm.ws.microprofile.faulttolerance.spi.RetryPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.TimeoutPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.context.ContextService;
import com.ibm.ws.microprofile.faulttolerance.spi.context.ContextSnapshot;
import com.ibm.ws.microprofile.faulttolerance.utils.FTDebug;
import com.ibm.ws.microprofile.faulttolerance20.state.AsyncBulkheadState;
import com.ibm.ws.microprofile.faulttolerance20.state.AsyncBulkheadState.BulkheadReservation;
import com.ibm.ws.microprofile.faulttolerance20.state.AsyncBulkheadState.ExceptionHandler;
import com.ibm.ws.microprofile.faulttolerance20.state.AsyncBulkheadState.ExecutionReference;
import com.ibm.ws.microprofile.faulttolerance20.state.CircuitBreakerState;
import com.ibm.ws.microprofile.faulttolerance20.state.FallbackState;
import com.ibm.ws.microprofile.faulttolerance20.state.FaultToleranceStateFactory;
import com.ibm.ws.microprofile.faulttolerance20.state.RetryState;
import com.ibm.ws.microprofile.faulttolerance20.state.RetryState.RetryResult;
import com.ibm.ws.microprofile.faulttolerance20.state.TimeoutState;

/**
 * Abstract executor for asynchronous calls.
 * <p>
 * Asynchronous calls return their result inside a wrapper (currently either a {@link Future} or a {@link CompletionStage}).
 * <p>
 * When this executor is called, it must create an instance of the wrapper and return that immediately, while execution of the method takes place on another thread. Once the
 * execution of the method is complete, the wrapper instance must be updated with the result of the method execution.
 * <p>
 * If an internal exception occurs, this may be thrown directly from the {@link #execute(Callable, ExecutionContext)} method, or logged and propagated back to the user via the
 * result wrapper.
 * <p>
 * Flow through this class:
 * <p>
 * On the calling thread:
 * <ul>
 * <li>start at {@code execute()} (called by the fault tolerance interceptor)</li>
 * <li>create a return wrapper and store it in the execution context (see createReturnWrapper)</li>
 * <li>prepare the first execution attempt to run on another thread (by submitting a task to the AsyncBulkheadState, see prepareExecutionAttempt)</li>
 * <li>return the return wrapper to the interceptor</li>
 * </ul>
 * <p>
 * Each execution attempt runs on another thread:
 * <ul>
 * <li>entry point is {@code runExecutionAttempt}</li>
 * <li>the user's method is run and the result is stored in a MethodResult</li>
 * <li>fault tolerance policies are applied based on the MethodResult (see processMethodResult and processEndOfAttempt)</li>
 * <li>if it's determined that a retry should be run, a new execution attempt is enqueued to run on another thread (see prepareExecutionAttempt)</li>
 * <li>otherwise, the MethodResult is set into the result wrapper and the execution is complete (see setResult)</li>
 * </ul>
 * <p>
 * There are also some complications that can disrupt the normal flow:
 * <ul>
 * <li>If a timeout occurs, the {@code timeout()} method is called where we try to abort the attempt (interrupting it if it's running) and do the fault tolerance handling (by
 * calling processEndOfAttempt)</li>
 * <li>If the user cancels the execution, we try to abort the current attempt and do the fault tolerance handling (calling processEndOfAttempt). We'll never retry if the user has
 * tried to cancel.</li>
 * <li>If an unexpected exception is caught, we call {@code handleException()} which will log an FFDC, return the exception as the result to the user and call processEndOfAttempt
 * to record a failure and ensure any reserved resources are released.</li>
 * </ul>
 *
 * @param <W> the return type of the code being executed, which is also the type of the result wrapper (e.g. {@code Future<String>})
 */
public abstract class AsyncExecutor<W> implements Executor<W> {

    private static final TraceComponent tc = Tr.register(AsyncExecutor.class);

    public AsyncExecutor(RetryPolicy retry, CircuitBreakerPolicy cbPolicy, TimeoutPolicy timeoutPolicy, FallbackPolicy fallbackPolicy, BulkheadPolicy bulkheadPolicy,
                         ScheduledExecutorService executorService, ContextService contextService, MetricRecorder metricRecorder) {
        retryPolicy = retry;
        circuitBreaker = FaultToleranceStateFactory.INSTANCE.createCircuitBreakerState(cbPolicy, metricRecorder);
        this.timeoutPolicy = timeoutPolicy;
        this.executorService = executorService;
        fallback = FaultToleranceStateFactory.INSTANCE.createFallbackState(fallbackPolicy);
        bulkhead = FaultToleranceStateFactory.INSTANCE.createAsyncBulkheadState(executorService, bulkheadPolicy, metricRecorder);
        this.metricRecorder = metricRecorder;
        this.contextService = contextService;
    }

    private final RetryPolicy retryPolicy;
    private final CircuitBreakerState circuitBreaker;
    private final ScheduledExecutorService executorService;
    private final TimeoutPolicy timeoutPolicy;
    private final FallbackState fallback;
    private final AsyncBulkheadState bulkhead;
    private final ContextService contextService;
    private final MetricRecorder metricRecorder;

    @Override
    public W execute(Callable<W> callable, ExecutionContext context) {
        @SuppressWarnings("unchecked")
        AsyncExecutionContextImpl<W> executionContext = (AsyncExecutionContextImpl<W>) context;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Execution {0} Fault tolerance asynchronous execution started for {1}", executionContext.getId(), context.getMethod());
        }

        executionContext.setCallable(callable);

        W resultWrapper = createEmptyResultWrapper(executionContext);
        executionContext.setResultWrapper(resultWrapper);

        executionContext.setContextSnapshot(contextService.capture());

        RetryState retryState = FaultToleranceStateFactory.INSTANCE.createRetryState(retryPolicy, metricRecorder);
        executionContext.setRetryState(retryState);
        retryState.start();

        prepareExecutionAttempt(executionContext);

        return resultWrapper;
    }

    /**
     * Creates a wrapper instance that will be returned to the user
     *
     * @return the wrapper instance
     */
    abstract protected W createEmptyResultWrapper(AsyncExecutionContextImpl<W> executionContext);

    /**
     * Stores the result of the execution inside the wrapper instance
     *
     * @param executionContext the execution context
     * @param result           the method result
     */
    abstract protected void setResult(AsyncExecutionContextImpl<W> executionContext, MethodResult<W> result);

    /**
     * Submit an execution attempt to be run
     * <p>
     * Submission is done through the bulkhead object which may either queue the execution or immediately submit it to the global execution service
     */
    private void prepareExecutionAttempt(AsyncExecutionContextImpl<W> executionContext) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Execution {0} enqueuing new execution attempt", executionContext.getId());
        }

        AsyncAttemptContextImpl<W> attemptContext = new AsyncAttemptContextImpl<>(executionContext);

        TimeoutState timeout = FaultToleranceStateFactory.INSTANCE.createTimeoutState(executorService, timeoutPolicy, metricRecorder);
        attemptContext.setTimeoutState(timeout);

        if (!circuitBreaker.requestPermissionToExecute()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Execution {0} Circuit Breaker open, not executing", executionContext.getId());
            }

            MethodResult<W> result = MethodResult.failure(new CircuitBreakerOpenException());
            processEndOfAttempt(attemptContext, result);
            return;
        }

        attemptContext.setCircuitBreakerPermittedExecution(true);

        timeout.start();

        ExecutionReference ref = bulkhead.submit((reservation) -> runExecutionAttempt(attemptContext, reservation), getExceptionHandler(attemptContext));

        if (!ref.wasAccepted()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Execution {0} bulkhead rejected execution", executionContext.getId());
            }

            processEndOfAttempt(attemptContext, MethodResult.failure(new BulkheadException()));
            return;
        }

        timeout.setTimeoutCallback(handleExceptions(() -> timeout(attemptContext, ref), attemptContext, executionContext));

        // Update what we should do if the user cancels the execution
        executionContext.setCancelCallback((mayInterrupt) -> {
            ref.abort(mayInterrupt);
            processEndOfAttempt(attemptContext, MethodResult.failure(new CancellationException()));
        });
    }

    /**
     * Run one attempt of the execution {@link #execute(Callable, ExecutionContext)}
     * <p>
     * This stage includes running the user's code
     */
    @FFDCIgnore({ Throwable.class, IllegalStateException.class })
    private void runExecutionAttempt(AsyncAttemptContextImpl<W> attemptContext, BulkheadReservation reservation) {
        AsyncExecutionContextImpl<W> executionContext = attemptContext.getExecutionContext();
        try {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Execution {0} running execution attempt", executionContext.getId());
            }

            ContextSnapshot snapshot = executionContext.getContextSnapshot();
            MethodResult<W> methodResult = null;
            try {
                methodResult = snapshot.runWithContext(() -> {
                    try {
                        // Execute the method, store the result, and catch/store any exceptions for fault tolerance
                        W result = executionContext.getCallable().call();
                        return MethodResult.success(result);
                    } catch (Throwable e) {
                        return MethodResult.failure(e);
                    }
                });
            } catch (IllegalStateException e) {
                // The application or module has gone away, we can no longer run things for this app
                // Mark this as an internal failure as we don't want any retries or further processing to occur
                // Note: If the method execution threw an IllegalStateException it would have been caught by the 'catch(Throwable e)' above
                methodResult = MethodResult.internalFailure(createAppStoppedException(e, attemptContext.getExecutionContext()));
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Execution {0} attempt result: {1}", executionContext.getId(), methodResult);
            }

            if (!methodResult.isFailure() && (methodResult.getResult() == null)) {
                String methodName = FTDebug.formatMethod(executionContext.getMethod());
                Tr.warning(tc, "asynchronous.returned.null.CWMFT0003W", methodName);
                methodResult = MethodResult.internalFailure(new NullPointerException(Tr.formatMessage(tc, "asynchronous.returned.null.CWMFT0003W", methodName)));
                // Internal Failure -> Retry and Fallback are not applied
            }

            processMethodResult(attemptContext, methodResult, reservation);

            if (attemptContext.getTimeoutState().isTimedOut()) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Execution {0} timed out, clearing interrupted flag", executionContext.getId());
                }

                Thread.interrupted(); // Clear interrupted thread
            }
        } catch (Throwable t) {
            // Handle any unexpected exceptions
            // Manual FFDC since we've ignored it for the catch above
            String sourceId = AsyncExecutor.class.getName();
            FFDCFilter.processException(t, sourceId, "runExecutionAttempt.errorBarrier", this);

            // Release bulkhead before handling exception so we don't leak permits
            reservation.release();
            handleException(t, attemptContext, executionContext);
        }
    }

    /**
     * Called to process the result of a call to the user's method
     * <p>
     * If a timeout or cancellation occurs prevents the user's method from being called, this method will not be called.
     * <p>
     * If a timeout or cancellation occurs while the user's method is running, this method will only be called when the user's method actually returns
     */
    protected void processMethodResult(AsyncAttemptContextImpl<W> attemptContext, MethodResult<W> methodResult, BulkheadReservation reservation) {
        // Release bulkhead permit
        reservation.release();

        TimeoutState timeoutState = attemptContext.getTimeoutState();
        timeoutState.stop();
        // Make sure that if we timed out, the timeout callback calls processEndOfAttempt rather than us
        if (!timeoutState.isTimedOut()) {
            processEndOfAttempt(attemptContext, methodResult);
        }
    }

    /**
     * Run if a method times out.
     */
    private void timeout(AsyncAttemptContextImpl<W> attemptContext, ExecutionReference ref) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Execution {0} timed out, attempting to cancel execution attempt", attemptContext.getExecutionContext().getId());
        }

        ref.abort(true);

        MethodResult<W> result = MethodResult.failure(new TimeoutException());
        processEndOfAttempt(attemptContext, result);
    }

    /**
     * Process the result of an attempt
     * <p>
     * This method is usually called from {@link #processMethodResult(AsyncAttemptContextImpl, MethodResult, BulkheadReservation)} at the end of the attempt or from
     * {@link #timeout(AsyncAttemptContextImpl, ExecutionReference)} in response to a timeout. In the case of an unexpected exception, it can also be called from
     * {@link #handleExceptions(Runnable, AsyncAttemptContextImpl, AsyncExecutionContextImpl)}.
     * <p>
     * To ensure that the end-of-attempt logic is only run once, this method will do nothing if called a second time for the same attempt context.
     * <p>
     * In regular execution, if a retry is needed this method will prepare another attempt, if a fallback is needed it will prepare execution of the fallback and otherwise it will
     * call {@link #processEndOfExecution(AsyncExecutionContextImpl, MethodResult)}.
     * <p>
     * In the case of an internal exception, this method will set the result directly, skipping any fallback or retry logic.
     */
    protected void processEndOfAttempt(AsyncAttemptContextImpl<W> attemptContext, MethodResult<W> result) {
        AsyncExecutionContextImpl<W> executionContext = attemptContext.getExecutionContext();

        try {

            if (!attemptContext.end()) {
                // This attempt has already been completed (probably because an error occurred)
                // Whatever the reason, we don't want to run the end-of-attempt logic again
                return;
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Execution {0} processing end of attempt execution. Result: {1}", executionContext.getId(), result);
            }

            if (attemptContext.getCircuitBreakerPermittedExecution()) {
                // Only record a circuit breaker result if it allowed us to run in the first place
                circuitBreaker.recordResult(result);
            }

            // Note: don't process retries or fallback for internal failures or if the user has cancelled the execution
            if (!result.isInternalFailure() && !executionContext.isCancelled()) {
                RetryResult retryResult = executionContext.getRetryState().recordResult(result);
                if (retryResult.shouldRetry()) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                        Tr.event(tc, "Execution {0} retrying with delay: {1} {2}", executionContext.getId(), retryResult.getDelay(), retryResult.getDelayUnit());
                    }
                    if (retryResult.getDelay() > 0) {
                        executorService.schedule(handleExceptions(() -> prepareExecutionAttempt(executionContext), null, executionContext),
                                                 retryResult.getDelay(),
                                                 retryResult.getDelayUnit());
                    } else {
                        prepareExecutionAttempt(executionContext);
                    }
                    // We've enqueued the retry to run, exit here
                    return;
                } else {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                        Tr.event(tc, "Execution {0} not retrying: {1}", executionContext.getId(), retryResult);
                    }
                }

                if (fallback.shouldApplyFallback(result)) {
                    prepareFallback(result, executionContext);
                    return;
                }
            }

            processEndOfExecution(executionContext, result, NO_FALLBACK);

        } catch (Throwable t) {
            // This method is used as a general error handler, so we need some special logic in case something goes wrong while handling the error
            MethodResult<W> errorResult = MethodResult.internalFailure(new FaultToleranceException(Tr.formatMessage(tc, "internal.error.CWMFT4998E", t), t));
            setResult(attemptContext.getExecutionContext(), errorResult);
            throw t;
        }
    }

    private void processEndOfExecution(AsyncExecutionContextImpl<W> executionContext, MethodResult<W> result, FallbackOccurred fallbackOccurred) {
        if (result.isFailure()) {
            metricRecorder.incrementInvocationFailedCount(fallbackOccurred);
        } else {
            metricRecorder.incrementInvocationSuccessCount(fallbackOccurred);
        }

        setResult(executionContext, result);
    }

    /**
     * Prepare the execution of the fallback method or handler to run on another thread
     *
     * @param result
     * @param executionContext
     */
    private void prepareFallback(MethodResult<W> result, AsyncExecutionContextImpl<W> executionContext) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Execution {0} fallback is required", executionContext.getId());
        }

        executorService.submit(() -> runFallback(result, executionContext));
    }

    @FFDCIgnore({ Throwable.class, IllegalStateException.class })
    private void runFallback(MethodResult<W> failedResult, AsyncExecutionContextImpl<W> executionContext) {
        try {

            MethodResult<W> fallbackResult = null;
            ContextSnapshot snapshot = executionContext.getContextSnapshot();

            try {
                fallbackResult = snapshot.runWithContext(() -> fallback.runFallback(failedResult, executionContext));
            } catch (IllegalStateException e) {
                fallbackResult = MethodResult.internalFailure(createAppStoppedException(e, executionContext));
            }

            processEndOfExecution(executionContext, fallbackResult, WITH_FALLBACK);

        } catch (Throwable t) {
            // Handle any unexpected exceptions
            // Manual FFDC since we've ignored it for the catch above
            String sourceId = AsyncExecutor.class.getName();
            FFDCFilter.processException(t, sourceId, "runFallback.errorBarrier", this);

            handleException(t, null, executionContext);
        }
    }

    @Override
    public FTExecutionContext newExecutionContext(String id, Method method, Object... parameters) {
        return new AsyncExecutionContextImpl<W>(id, method, parameters);
    }

    @Override
    public void close() {
        // Do nothing
    }

    /**
     * Wraps a runnable inside another runnable which ensures that any exceptions it throws are handled
     * <p>
     * Useful when we schedule a task to run but don't intend to check its result because it shouldn't throw an exception.
     * <p>
     * The strategy used to handle exceptions is as follows:
     * <ul>
     * <li>Log and FFDC the exception
     * <li>If an attemptContext was passed, call processEndOfAttempt, reporting the exception as an internal error
     * <li>If an attemptContext was not passed, call setResult, reporting the exception as the result
     * </ul>
     *
     * @param runnable         the runnable to wrap
     * @param attemptContext   the attempt context associated with the runnable, may be {@code null}
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
            processEndOfAttempt(attemptContext, result);
        } else {
            setResult(executionContext, result);
        }
    }

    protected FaultToleranceException createAppStoppedException(IllegalStateException e, AsyncExecutionContextImpl<W> context) {
        String methodString = FTDebug.formatMethod(context.getMethod());
        Tr.warning(tc, "application.shutdown.CWMFT0002W", methodString);
        return new FaultToleranceException(Tr.formatMessage(tc, "application.shutdown.CWMFT0002W", methodString), e);
    }

}
