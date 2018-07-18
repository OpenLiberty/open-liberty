/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.concurrent.rx;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * This class provides the implementation of ManagedCompletableFuture.minimalCompletionStage.
 * It is a subclass of ManagedCompletableFuture that only allows for natural completion,
 * disallowing completion by any of the various other mechanisms of CompletableFuture
 * such as cancel, complete, obtrude, timeout.
 *
 * @param <T> type of result
 */
class ManagedCompletionStage<T> extends ManagedCompletableFuture<T> {
    private static final TraceComponent tc = Tr.register(ManagedCompletionStage.class);

    /**
     * Construct a minimal completion stage that disallows completion by all other means
     * than the natural completion of the stage.
     *
     * @param executor default asynchronous execution facility for this stage
     */
    ManagedCompletionStage(Executor executor) {
        super(executor, null);
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean complete(T value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<T> completeAsync(Supplier<? extends T> supplier) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<T> completeAsync(Supplier<? extends T> supplier, Executor executor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean completeExceptionally(Throwable x) {
        throw new UnsupportedOperationException();
    }

    @Override
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
    public CompletableFuture<T> newIncompleteFuture() {
        return new ManagedCompletionStage<T>(defaultExecutor);
    }

    @Override
    public void obtrudeException(Throwable x) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void obtrudeValue(T value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<T> orTimeout(long timeout, TimeUnit unit) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<T> toCompletableFuture() {
        ManagedCompletableFuture<T> dependentStage = new ManagedCompletableFuture<T>(defaultExecutor, null);

        super.whenComplete((result, failure) -> {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(ManagedCompletionStage.this, tc, "whenComplete", result, failure);
            if (failure == null)
                dependentStage.super_complete(result);
            else
                dependentStage.super_completeExceptionally(failure);
        });

        return dependentStage;
    }
}