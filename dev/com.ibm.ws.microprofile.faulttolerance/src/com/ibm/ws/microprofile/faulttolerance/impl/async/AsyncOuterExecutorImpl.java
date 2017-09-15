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

import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.microprofile.faulttolerance.exceptions.BulkheadException;
import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.microprofile.faulttolerance.impl.ExecutionContextImpl;
import com.ibm.ws.microprofile.faulttolerance.impl.FTConstants;
import com.ibm.ws.microprofile.faulttolerance.impl.sync.SynchronousExecutorImpl;
import com.ibm.ws.microprofile.faulttolerance.spi.BulkheadPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.CircuitBreakerPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.FallbackPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.RetryPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.TimeoutPolicy;
import com.ibm.ws.threading.PolicyExecutor;
import com.ibm.ws.threading.PolicyExecutor.QueueFullAction;
import com.ibm.ws.threading.PolicyExecutorProvider;
import com.ibm.wsspi.threadcontext.ThreadContextDescriptor;
import com.ibm.wsspi.threadcontext.WSContextService;

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
            policyExecutor.queueFullAction(QueueFullAction.Abort);

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
                    threadContext = contextService.captureThreadContext(new HashMap<String, String>());
                }
                QueuedFuture<R> queuedFuture = new QueuedFuture<>(innerTask, executionContext, threadContext);

                try {
                    //begin the queuedFuture execution
                    queuedFuture.start(executorService);
                } catch (RejectedExecutionException e) {
                    //if the execution was rejected then end the execution and throw a BulkheadException
                    //TODO there might not really have been a bulkhead?? but it's pretty unlikely that the execution would
                    //be rejected otherwise!
                    executionContext.end();
                    throw new BulkheadException(Tr.formatMessage(tc, "bulkhead.no.threads.CWMFT0001E", executionContext.getMethod()), e);
                }

                return queuedFuture;
            }
        };
        return outerTask;
    }

    @Override
    public void close() {
        executorService.shutdown();
        super.close();
        nestedExecutor.close();
    }
}
