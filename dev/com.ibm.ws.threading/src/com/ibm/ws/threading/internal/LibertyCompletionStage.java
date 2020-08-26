/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.threading.internal;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

/**
 * Implementation of LibertyCompletableFuture.minimalCompletionStage.
 * This implementation is valid for Java 9+. Do not use for Java 8.
 *
 * @param <T> return type of the completion stage
 */
class LibertyCompletionStage<T> extends LibertyCompletableFuture<T> {
    LibertyCompletionStage(LibertyCompletableFuture<T> completableFuture) {
        super(completableFuture.executor);
        completableFuture.whenComplete((result, failure) -> {
            if (failure == null)
                super.complete(result);
            else
                super.completeExceptionally(failure);
        });
    }

    LibertyCompletionStage(Executor executor) {
        super(executor);
    }

    // The pattern of inheriting from CompletableFuture and rejecting most methods that aren't
    // on the CompletionStage interface appears to be used by Java, and so we do this, too,
    // attempting to reject the same set of methods.

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean complete(T value) {
        throw new UnsupportedOperationException();
    }

    public CompletableFuture<T> completeAsync(Supplier<? extends T> supplier) {
        throw new UnsupportedOperationException();
    }

    public CompletableFuture<T> completeAsync(Supplier<? extends T> supplier, Executor executor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean completeExceptionally(Throwable x) {
        throw new UnsupportedOperationException();
    }

    public CompletableFuture<T> completeOnTimeout(T value, long timeout, TimeUnit unit) {
        throw new UnsupportedOperationException();
    }

    // copy is allowed because java.util.concurrent.CompletableFuture's minimalCompletionStage allows it

    @Override
    public T get() throws ExecutionException, InterruptedException {
        throw new UnsupportedOperationException();
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws ExecutionException, InterruptedException, TimeoutException {
        throw new UnsupportedOperationException();
    }

    @Override
    public T getNow(T valueIfAbsent) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getNumberOfDependents() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isCancelled() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isCompletedExceptionally() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isDone() {
        throw new UnsupportedOperationException();
    }

    @Override
    public T join() {
        throw new UnsupportedOperationException();
    }

    // minimalCompletionStage is allowed because java.util.concurrent.CompletableFuture's minimalCompletionStage allows it

    @Override
    public CompletionStage<T> minimalCompletionStage() {
        return new LibertyCompletionStage<T>(this);
    }

    @Override
    public CompletableFuture<T> newIncompleteFuture() {
        return new LibertyCompletionStage<T>(executor);
    }

    @Override
    public void obtrudeException(Throwable x) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void obtrudeValue(T value) {
        throw new UnsupportedOperationException();
    }

    public CompletableFuture<T> orTimeout(long timeout, TimeUnit unit) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<T> toCompletableFuture() {
        LibertyCompletableFuture<T> completableFuture = new LibertyCompletableFuture<T>(executor);

        // The completable future that we are creating must complete upon completion of this stage
        super.whenComplete((result, failure) -> {
            if (failure == null)
                completableFuture.complete(result);
            else
                completableFuture.completeExceptionally(failure);
        });

        return completableFuture;
    }
}
