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
package concurrent.mp.fat.web;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

/**
 * This class partly works around Java SE 8 not having a minimalCompletionStage method by
 * subclassing CompletableFuture to provide one that only restricts the user to CompletionStage
 * methods on the current stage. Dependent stages created from this stage go back to being
 * CompletableFuture.
 */
public class MinimalSingleCompletionStage<T> extends CompletableFuture<T> {
    MinimalSingleCompletionStage(CompletionStage<T> parent) {
        parent.whenComplete((result, failure) -> {
            if (failure == null)
                super.complete(result);
            else
                super.completeExceptionally(failure);
        });
    }

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

    public CompletableFuture<T> copy() {
        throw new UnsupportedOperationException();
    }

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
        throw new UnsupportedOperationException();
    }
}
