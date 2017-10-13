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
package com.ibm.ws.microprofile.faulttolerance.impl.async;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.microprofile.faulttolerance.ExecutionContext;
import org.eclipse.microprofile.faulttolerance.exceptions.BulkheadException;
import org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException;
import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.microprofile.faulttolerance.impl.CircuitBreakerImpl;
import com.ibm.ws.microprofile.faulttolerance.impl.ExecutionContextImpl;
import com.ibm.ws.microprofile.faulttolerance.impl.FTConstants;
import com.ibm.ws.microprofile.faulttolerance.impl.sync.SynchronousExecutorImpl;
import com.ibm.ws.microprofile.faulttolerance.spi.BulkheadPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.CircuitBreakerPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.ExecutionException;
import com.ibm.ws.microprofile.faulttolerance.spi.FallbackPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.RetryPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.TimeoutPolicy;
import com.ibm.ws.microprofile.faulttolerance.utils.FTDebug;
import com.ibm.ws.threading.PolicyExecutor;
import com.ibm.ws.threading.PolicyExecutorProvider;
import com.ibm.wsspi.threadcontext.ThreadContextDescriptor;
import com.ibm.wsspi.threadcontext.WSContextService;

import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.FailsafeException;

/**
 * An AsyncExecutorImpl builds on SynchronousExecutorImpl but the task which is run actually submits another task to be run asynchronously.
 *
 * Ultimately an Asynchronous execution consists of two synchronous executions, on different threads but with a shared execution context.
 */
public class AsyncOuterExecutorImpl<R> extends SynchronousExecutorImpl<Future<R>> {

    private static final TraceComponent tc = Tr.register(AsyncOuterExecutorImpl.class);

    private final AsyncInnerExecutorImpl<Future<R>> nestedExecutor;
    private final BulkheadPolicy bulkheadPolicy;
    private final WSContextService contextService;
    private final ExecutorService executorService;

    /**
     * The collection of contexts to capture under createThreadContext.
     * Classloader, JeeMetadata, and security.
     */
    @SuppressWarnings("unchecked")
    private static final Map<String, ?>[] THREAD_CONTEXT_PROVIDERS = new Map[] {
                                                                                 Collections.singletonMap(WSContextService.THREAD_CONTEXT_PROVIDER,
                                                                                                          "com.ibm.ws.classloader.context.provider"),
                                                                                 Collections.singletonMap(WSContextService.THREAD_CONTEXT_PROVIDER,
                                                                                                          "com.ibm.ws.javaee.metadata.context.provider"),
                                                                                 Collections.singletonMap(WSContextService.THREAD_CONTEXT_PROVIDER,
                                                                                                          "com.ibm.ws.security.context.provider"),
    };

    public AsyncOuterExecutorImpl(RetryPolicy retryPolicy,
                                  CircuitBreakerPolicy circuitBreakerPolicy,
                                  TimeoutPolicy timeoutPolicy,
                                  BulkheadPolicy bulkheadPolicy,
                                  FallbackPolicy fallbackPolicy,
                                  WSContextService contextService,
                                  PolicyExecutorProvider policyExecutorProvider,
                                  ScheduledExecutorService scheduledExecutorService) {

        super(retryPolicy, circuitBreakerPolicy, timeoutPolicy, bulkheadPolicy, fallbackPolicy, scheduledExecutorService);

        this.nestedExecutor = new AsyncInnerExecutorImpl<>();

        this.bulkheadPolicy = bulkheadPolicy;
        this.contextService = contextService;

        if (policyExecutorProvider != null) {
            //this is the normal case when running in Liberty
            //create a policy executor to run things asynchronously

            //TODO make the ID more human readable
            PolicyExecutor policyExecutor = policyExecutorProvider.create("FaultTolerance_" + UUID.randomUUID().toString());

            //if there is supposed to be a bulkhead then restrict the size of the policy executor
            if (this.bulkheadPolicy != null) {
                int maxThreads = bulkheadPolicy.getMaxThreads();
                int queueSize = bulkheadPolicy.getQueueSize();
                policyExecutor.maxConcurrency(maxThreads);
                policyExecutor.maxQueueSize(queueSize);
            }

            this.executorService = policyExecutor;
        } else {
            //this is really intended for unittest only, running outside of Liberty
            //create a "basic" Thread Pool to run things asynchronously
            if ("true".equalsIgnoreCase(System.getProperty(FTConstants.JSE_FLAG))) {

                //even if there is no bulkhead, we don't want an unlimited queue
                int maxThreads = Integer.MAX_VALUE;
                int queueSize = 1000;
                if (this.bulkheadPolicy != null) {
                    maxThreads = bulkheadPolicy.getMaxThreads();
                    queueSize = bulkheadPolicy.getQueueSize();
                }

                ArrayBlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(queueSize, true);
                this.executorService = new java.util.concurrent.ThreadPoolExecutor(maxThreads, maxThreads, 0l, TimeUnit.MILLISECONDS, queue, Executors.defaultThreadFactory());
            } else {
                throw new FaultToleranceException(Tr.formatMessage(tc, "internal.error.CWMFT4999E"));
            }
        }
    }

    @Override
    public Future<R> execute(Callable<Future<R>> callable, ExecutionContext executionContext) {
        ExecutionContextImpl context = (ExecutionContextImpl) executionContext;

        CircuitBreakerImpl breaker = context.getCircuitBreaker();

        // Check whether the circuit is open
        if (breaker != null && !breaker.allowsExecution()) {
            if (context.getFallbackPolicy() != null) {
                return callFallback(context);
            } else {
                throw new CircuitBreakerOpenException();
            }
        }

        return super.execute(callable, executionContext);
    }

    @Override
    protected Callable<Future<R>> createTask(Callable<Future<R>> callable, ExecutionContextImpl executionContext) {
        //this is the inner nested task that will be run synchronously
        Callable<Future<R>> innerTask = () -> {
            Future<R> future = this.nestedExecutor.execute(callable, executionContext);
            return future;
        };

        //this is the outer task which will be run synchronously but launch a new thread to to run the inner one
        Callable<Future<R>> outerTask = new Callable<Future<R>>() {

            @Override
            @FFDCIgnore({ RejectedExecutionException.class })
            public Future<R> call() {
                ThreadContextDescriptor threadContext = null;
                if (contextService != null) {
                    threadContext = contextService.captureThreadContext(new HashMap<String, String>(), THREAD_CONTEXT_PROVIDERS);
                }
                QueuedFuture<R> queuedFuture = new QueuedFuture<>(innerTask, executionContext, threadContext);

                try {
                    //begin the queuedFuture execution
                    queuedFuture.start(executorService);
                } catch (RejectedExecutionException e) {
                    //if the execution was rejected then end the execution and throw a BulkheadException
                    //TODO there might not really have been a bulkhead?? but it's pretty unlikely that the execution would
                    //be rejected otherwise!
                    executionContext.close();

                    BulkheadException bulkheadException = new BulkheadException(Tr.formatMessage(tc, "bulkhead.no.threads.CWMFT0001E",
                                                                                                 FTDebug.formatMethod(executionContext.getMethod())), e);
                    reportFailure(executionContext, bulkheadException);
                    throw bulkheadException;
                }

                return queuedFuture;
            }
        };
        return outerTask;
    }

    /**
     * Call the fallback function.
     * <p>
     * This method may only be called if there is a fallback function defined.
     *
     * @param context the execution context
     * @return the fallback result
     * @throws ExecutionException if the fallback method throws an exception
     */
    @FFDCIgnore(Throwable.class)
    private Future<R> callFallback(ExecutionContextImpl context) {
        try {
            return (Future<R>) context.getFallbackPolicy().getFallbackFunction().execute(context);
        } catch (Throwable t) {
            throw new ExecutionException(t);
        }
    }

    /**
     * Records an execution failure with the circuit breaker.
     * <p>
     * We do this by checking that the circuit breaker considers the exeception as a failure, and then running a function through failsafe which just throws that exception.
     * <p>
     * We need to do this because we want to register failures to start an async method, but not register successes, and there's no Failsafe API to manually register a failure
     * without corrupting the internal state.
     *
     * @param context the execution context
     * @param ex the exception which caused the failure
     */
    private void reportFailure(ExecutionContextImpl context, Exception ex) {
        CircuitBreakerImpl breaker = context.getCircuitBreaker();
        if (breaker != null && breaker.isFailure(null, ex)) {
            try {
                Failsafe.with(breaker).run(() -> {
                    throw ex;
                });
            } catch (FailsafeException | CircuitBreakerOpenException failsafeException) {
                // Do  nothing, all we wanted to do was register a failed execution with the circuit breaker
            }
        }
    }

    @Override
    protected boolean enableCircuitBreaker() {
        // Don't want full circuit breaker as we only want to record failures
        return false;
    }

    @Override
    protected boolean enableFallback() {
        // Since we're doing our own circuit breaking, we don't want failsafe to fallback for us
        // as that would prevent us seeing the failure. We'll do our own falling back.
        return false;
    }

    @Override
    public void close() {
        executorService.shutdown();
        super.close();
        nestedExecutor.close();
    }
}
