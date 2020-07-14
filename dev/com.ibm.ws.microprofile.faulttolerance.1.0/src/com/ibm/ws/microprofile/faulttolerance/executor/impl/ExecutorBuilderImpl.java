/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.faulttolerance.executor.impl;

import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;

import com.ibm.ws.microprofile.faulttolerance.executor.impl.async.AsyncOuterExecutorImpl;
import com.ibm.ws.microprofile.faulttolerance.executor.impl.sync.SynchronousExecutorImpl;
import com.ibm.ws.microprofile.faulttolerance.impl.AbstractExecutorBuilderImpl;
import com.ibm.ws.microprofile.faulttolerance.spi.Executor;
import com.ibm.ws.microprofile.faulttolerance.spi.ExecutorBuilder;
import com.ibm.ws.threading.PolicyExecutorProvider;
import com.ibm.wsspi.threadcontext.WSContextService;

public class ExecutorBuilderImpl<R> extends AbstractExecutorBuilderImpl<R> implements ExecutorBuilder<R> {

    protected final WSContextService contextService;

    public ExecutorBuilderImpl(WSContextService contextService, PolicyExecutorProvider policyExecutorProvider, ScheduledExecutorService scheduledExecutorService) {
        super(policyExecutorProvider, scheduledExecutorService);
        this.contextService = contextService;
    }

    /** {@inheritDoc} */
    @Override
    public Executor<R> build() {
        Executor<R> executor = new SynchronousExecutorImpl<R>(this.retryPolicy, this.circuitBreakerPolicy, this.timeoutPolicy, this.bulkheadPolicy, this.fallbackPolicy, this.scheduledExecutorService, this.metricRecorder);

        return executor;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <W> Executor<W> buildAsync(Class<?> asyncResultWrapperType) {
        if (asyncResultWrapperType == Future.class) {
            Executor<Future<R>> executor = new AsyncOuterExecutorImpl<R>(this.retryPolicy, this.circuitBreakerPolicy, this.timeoutPolicy, this.bulkheadPolicy, this.fallbackPolicy, this.contextService, this.policyExecutorProvider, this.scheduledExecutorService, this.metricRecorder);
            return (Executor<W>) executor;
        } else {
            throw new IllegalArgumentException("Invalid return type for async execution: " + asyncResultWrapperType);
        }
    }

}
