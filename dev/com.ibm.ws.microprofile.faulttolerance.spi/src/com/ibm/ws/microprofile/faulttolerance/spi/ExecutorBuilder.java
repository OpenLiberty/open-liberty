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
package com.ibm.ws.microprofile.faulttolerance.spi;

public interface ExecutorBuilder<R> {

    public ExecutorBuilder<R> setRetryPolicy(RetryPolicy retry);

    public ExecutorBuilder<R> setCircuitBreakerPolicy(CircuitBreakerPolicy circuitBreaker);

    public ExecutorBuilder<R> setBulkheadPolicy(BulkheadPolicy bulkhead);

    public ExecutorBuilder<R> setFallbackPolicy(FallbackPolicy fallback);

    public ExecutorBuilder<R> setTimeoutPolicy(TimeoutPolicy timeout);

    public ExecutorBuilder<R> setMetricRecorder(MetricRecorder metricRecorder);

    public Executor<R> build();

    public <W> Executor<W> buildAsync(Class<?> asyncResultWrapperType);
}
