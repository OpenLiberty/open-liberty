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

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;

@ApplicationScoped
public class CDICompletionStageBean {

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
