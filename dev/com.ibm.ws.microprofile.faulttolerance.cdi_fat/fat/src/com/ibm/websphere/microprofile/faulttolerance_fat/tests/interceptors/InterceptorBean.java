/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.microprofile.faulttolerance_fat.tests.interceptors;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Retry;

import com.ibm.websphere.microprofile.faulttolerance_fat.tests.interceptors.EarlyInterceptor.EarlyBinding;
import com.ibm.websphere.microprofile.faulttolerance_fat.tests.interceptors.LateInterceptor.LateBinding;
import com.ibm.ws.microprofile.faulttolerance_fat.cdi.TestConstants;

/**
 * This is the bean class that has methods with bound interceptors
 */
@ApplicationScoped
public class InterceptorBean {

    @EarlyBinding
    @LateBinding
    public void serviceInterceptors(InterceptionRecorder recorder) {
    }

    @EarlyBinding
    @LateBinding
    @Asynchronous
    public Future<Void> serviceAsyncInterceptors(InterceptionRecorder recorder) {
        return CompletableFuture.completedFuture(null);
    }

    @EarlyBinding
    @LateBinding
    @Retry(maxRetries = 3, jitter = 0)
    public void serviceRetryInterceptors(InterceptionRecorder recorder, AtomicInteger calls) {
        int callCount = calls.incrementAndGet();
        if (callCount < 3) {
            throw new RuntimeException("Test Exception");
        }
    }

    @EarlyBinding
    @LateBinding
    @Bulkhead(value = 1)
    public void serviceBulkheadInterceptors(InterceptionRecorder recorder, CompletableFuture<Void> waitLatch, CompletableFuture<Void> startLatch) {
        startLatch.complete(null);
        try {
            waitLatch.get(TestConstants.TEST_TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @EarlyBinding
    @LateBinding
    @CircuitBreaker(requestVolumeThreshold = 2, failureRatio = 1.0)
    public void serviceCircuitBreakerInterceptors(InterceptionRecorder recorder, boolean throwException) {
        if (throwException) {
            throw new RuntimeException("Test exception");
        }
    }

}
