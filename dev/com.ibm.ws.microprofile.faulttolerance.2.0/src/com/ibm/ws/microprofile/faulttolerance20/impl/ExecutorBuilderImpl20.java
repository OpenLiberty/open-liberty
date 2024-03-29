/*******************************************************************************
 * Copyright (c) 2018, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.faulttolerance20.impl;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;

import com.ibm.ws.microprofile.faulttolerance.impl.AbstractExecutorBuilderImpl;
import com.ibm.ws.microprofile.faulttolerance.spi.Executor;
import com.ibm.ws.threading.PolicyExecutorProvider;
import com.ibm.wsspi.threadcontext.WSContextService;

public class ExecutorBuilderImpl20<R> extends AbstractExecutorBuilderImpl<R> {

    public ExecutorBuilderImpl20(WSContextService contextService, PolicyExecutorProvider policyExecutorProvider, ScheduledExecutorService scheduledExecutorService) {
        super(contextService, policyExecutorProvider, scheduledExecutorService);
    }

    @Override
    public Executor<R> build() {
        return new SyncExecutor<>(retryPolicy, circuitBreakerPolicy, timeoutPolicy, fallbackPolicy, bulkheadPolicy, scheduledExecutorService, metricRecorder);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <W> Executor<W> buildAsync(Class<?> asyncResultWrapperType) {
        if (asyncResultWrapperType == Future.class) {
            return (Executor<W>) new AsyncFutureExecutor<Object>(retryPolicy, circuitBreakerPolicy, timeoutPolicy, fallbackPolicy, bulkheadPolicy, scheduledExecutorService, contextService, metricRecorder, asyncRequestContext);
        } else if (asyncResultWrapperType == CompletionStage.class) {
            return (Executor<W>) new AsyncCompletionStageExecutor<Object>(retryPolicy, circuitBreakerPolicy, timeoutPolicy, fallbackPolicy, bulkheadPolicy, scheduledExecutorService, contextService, metricRecorder, asyncRequestContext);
        } else {
            throw new IllegalArgumentException("Invalid return type for async execution: " + asyncResultWrapperType);
        }
    }

}
