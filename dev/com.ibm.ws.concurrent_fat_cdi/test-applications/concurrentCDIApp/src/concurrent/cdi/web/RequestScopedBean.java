/*******************************************************************************
 * Copyright (c) 2017,2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package concurrent.cdi.web;

import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import jakarta.enterprise.context.RequestScoped;

import prototype.enterprise.concurrent.Async;

@Async(executor = "java:module/env/concurrent/timeoutExecutorRef")
@RequestScoped
public class RequestScopedBean {
    private int number;

    /**
     * Async method that waits the specifiee latch for up to the timeout.
     */
    public CompletionStage<Boolean> await(CountDownLatch latch, long timeout, TimeUnit unit) {
        try {
            return CompletableFuture.completedFuture(latch.await(timeout, unit));
        } catch (InterruptedException x) {
            throw new CompletionException(x);
        }
    }

    /**
     * Specify conflicting Async annotations at both class and method level
     * pointing at different managed executors. Return the one that is actually used.
     */
    @Async(executor = "java:app/env/concurrent/sampleExecutorRef")
    public CompletableFuture<Executor> getExecutorOfAsyncMethods() throws Exception {
        CompletableFuture<Executor> future = Async.Result.getFuture();
        // CompletatbleFuture.defaultExecutor() is unavailable on Java 8 CompletableFuture
        Method CompletableFuture_defaultExecutor;
        try {
            CompletableFuture_defaultExecutor = CompletableFuture.class.getMethod("defaultExecutor");
            future.complete((Executor) CompletableFuture_defaultExecutor.invoke(future));
        } catch (NoSuchMethodException x) {
            future.completeExceptionally(x);
        }
        return future;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }
}
