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
package com.ibm.websphere.microprofile.faulttolerance_fat.tests.completionstage;

import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;

@ApplicationScoped
public class CDICompletionStageBean {

    private final AtomicInteger concurrentCsBulkheadMethods = new AtomicInteger(0);

    @Asynchronous
    public <T> CompletionStage<T> serviceCs(CompletableFuture<Void> latch, CompletableFuture<T> returnValue) {
        try {
            latch.get(5, SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new RuntimeException("Error", e);
        }
        return returnValue;
    }

    @Asynchronous
    @Timeout(500)
    public CompletionStage<Void> serviceCsTimeout(CompletableFuture<Void> returnValue) {
        return returnValue;
    }

    @Asynchronous
    @Bulkhead(value = 1, waitingTaskQueue = 1)
    public CompletionStage<Void> serviceCsBulkhead(CompletableFuture<Void> returnValue) {
        try {
            concurrentCsBulkheadMethods.incrementAndGet();
            return returnValue;
        } finally {
            concurrentCsBulkheadMethods.decrementAndGet();
        }
    }

    /**
     * @return the current number of calls to serviceCsBulkhead which are currently running
     */
    public int getConcurrentServiceCsBulkhead() {
        return concurrentCsBulkheadMethods.get();
    }

    @Asynchronous
    @Bulkhead(value = 1, waitingTaskQueue = 1)
    @Timeout(500)
    public CompletionStage<Void> serviceCsBulkheadTimeout(CompletableFuture<Void> returnValue) {
        return returnValue;
    }

    private int retryAlwaysFailsAttemptCount = 0;

    @Asynchronous
    @Retry(maxRetries = 3, jitter = 0)
    public CompletionStage<Void> serviceCsRetryAlwaysFails() {
        retryAlwaysFailsAttemptCount++;
        CompletableFuture<Void> cf = new CompletableFuture<>();
        cf.completeExceptionally(new RuntimeException());
        return cf;
    }

    public int getRetryAlwaysFailsAttemptCount() {
        return retryAlwaysFailsAttemptCount;
    }

}
