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
package com.ibm.ws.concurrent.mp;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;

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

    /**
     * This constructor is for Java SE 8 only.
     * Construct a minimal completion stage that disallows completion by all other means
     * than the natural completion of the stage.
     *
     * @param completedFuture completable future upon which this instance is backed.
     * @param executor default asynchronous execution facility for this stage
     * @param futureRef reference to a policy executor Future that will be submitted if requested to run async. Otherwise null.
     */
    ManagedCompletionStage(CompletableFuture<T> completableFuture, Executor executor, AtomicReference<Future<?>> futureRef) {
        super(completableFuture, executor, futureRef);
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
        if (JAVA8)
            return new ManagedCompletionStage<T>(new CompletableFuture<T>(), defaultExecutor, null);
        else
            return new ManagedCompletionStage<T>(defaultExecutor);
    }

    /**
     * This method is only for Java SE 8.
     * It is used to override the newInstance method of ManagedCompletableFuture to ensure that
     * newly created instances are ManagedCompletionStage.
     *
     * @param completableFuture underlying completable future upon which this instance is backed.
     * @param managedExecutor managed executor service
     * @param futureRef reference to a policy executor Future that will be submitted if requested to run async. Otherwise null.
     * @return a new instance of this class.
     */
    @Override
    @SuppressWarnings("hiding")
    @Trivial
    <T> CompletableFuture<T> newInstance(CompletableFuture<T> completableFuture, Executor managedExecutor, AtomicReference<Future<?>> futureRef) {
        return new ManagedCompletionStage<T>(completableFuture, managedExecutor, futureRef);
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
        ManagedCompletableFuture<T> dependentStage;
        if (JAVA8) {
            dependentStage = new ManagedCompletableFuture<T>(new CompletableFuture<T>(), defaultExecutor, null);
            // The completable future that we are creating must complete upon completion of this stage
            super.whenComplete((result, failure) -> {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(ManagedCompletionStage.this, tc, "whenComplete", result, failure);
                if (failure == null)
                    dependentStage.completableFuture.complete(result);
                else
                    dependentStage.completableFuture.completeExceptionally(failure);
            });
        } else {
            dependentStage = new ManagedCompletableFuture<T>(defaultExecutor, null);
            // The completable future that we are creating must complete upon completion of this stage
            super.whenComplete((result, failure) -> {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(ManagedCompletionStage.this, tc, "whenComplete", result, failure);
                if (failure == null)
                    dependentStage.super_complete(result);
                else
                    dependentStage.super_completeExceptionally(failure);
            });
        }

        return dependentStage;
    }
}