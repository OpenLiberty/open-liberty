/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
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
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Extension to CompletableFuture for managed executors.
 *
 * This is needed because the Java SE CompletableFuture does not allow having a managed executor
 * as its default asynchronous execution facility. Nor does it behave properly with managed executors
 * when explicitly specifying a managed executor when adding each dependent stage - the thread context
 * propagation is undesirably non-deterministic, depending on whether or not previous stages happened
 * to have completed or not at the point in time when the dependent stage is added.
 *
 * We would preferably provide a CompletionStage implementation of our own, except that CompletionStage
 * interface is unfortunately bound to CompletableFuture by its toCompletableFuture method.
 * Technically, implementation of this method is optional, but at the price of not being able to
 * interoperate with other CompletionStages. In practice, users will certainly expect that ability,
 * plus the other capabilities that CompletableFuture provides such as forcing/triggering completion,
 * reporting status, and obtaining results at a later point.
 *
 * Because there is neither an interface for CompletableFuture, nor is there any built-in support for
 * plugging in extensions (method implementations return CompletableFuture, not the CompletableFuture subclass)
 * this implementation attempts to work around these limitations by using two instances of CompletableFuture:
 * one for the subclass itself, which includes the override logic, which delegates to another CompletableFuture
 * instance that does the actual work.
 *
 * TODO At this point, this class is just a skeleton and remains to be implemented. Methods are either rejected,
 * or provide no value above delegation back to the CompletableFuture. At first, we are just trying out
 * whether the approach is even a valid way of integrating with CompletableFuture.
 *
 * @param <T>
 */
public class ManagedCompletableFuture<T> extends CompletableFuture<T> {
    private final CompletableFuture<T> completableFuture;
    private final Executor defaultExecutor;

    /**
     * Construct a completable future that with a managed executor as its default asynchronous execution facility.
     *
     * @param executor managed executor service
     * @param parent
     */
    public ManagedCompletableFuture(CompletableFuture<T> completableFuture, Executor managedExecutor) {
        super();
        this.completableFuture = completableFuture;
        this.defaultExecutor = managedExecutor;
    }

    // static method equivalents for CompletableFuture

    public static ManagedCompletableFuture<Void> runAsync(Runnable runnable, Executor executor) {
        // TODO contextualize the runnable and redirect to the policy executor of the managed executor
        Executor managedExecutor = executor;
        Executor policyExecutor = executor;
        return new ManagedCompletableFuture<Void>(CompletableFuture.runAsync(runnable, policyExecutor), managedExecutor);
    }

    public static <U> ManagedCompletableFuture<U> supplyAsync(Supplier<U> supplier, Executor executor) {
        // TODO contextualize the supplier and redirect to the policy executor of the managed executor
        Executor managedExecutor = executor;
        Executor policyExecutor = executor;
        return new ManagedCompletableFuture<U>(CompletableFuture.supplyAsync(supplier, policyExecutor), managedExecutor);
    }

    // Overrides of CompletableFuture methods

    /**
     * @see java.util.concurrent.CompletionStage#acceptEither(java.util.concurrent.CompletionStage, java.util.function.Consumer)
     */
    @Override
    public ManagedCompletableFuture<Void> acceptEither(CompletionStage<? extends T> other, Consumer<? super T> action) {
        throw new UnsupportedOperationException();
    }

    /**
     * @see java.util.concurrent.CompletionStage#acceptEitherAsync(java.util.concurrent.CompletionStage, java.util.function.Consumer)
     */
    @Override
    public ManagedCompletableFuture<Void> acceptEitherAsync(CompletionStage<? extends T> other, Consumer<? super T> action) {
        throw new UnsupportedOperationException();
    }

    /**
     * @see java.util.concurrent.CompletionStage#acceptEitherAsync(java.util.concurrent.CompletionStage, java.util.function.Consumer, java.util.concurrent.Executor)
     */
    @Override
    public ManagedCompletableFuture<Void> acceptEitherAsync(CompletionStage<? extends T> other, Consumer<? super T> action, Executor executor) {
        throw new UnsupportedOperationException();
    }

    /**
     * @see java.util.concurrent.CompletionStage#applyToEither(java.util.concurrent.CompletionStage, java.util.function.Function)
     */
    @Override
    public <U> ManagedCompletableFuture<U> applyToEither(CompletionStage<? extends T> other, Function<? super T, U> fn) {
        throw new UnsupportedOperationException();
    }

    /**
     * @see java.util.concurrent.CompletionStage#applyToEitherAsync(java.util.concurrent.CompletionStage, java.util.function.Function)
     */
    @Override
    public <U> ManagedCompletableFuture<U> applyToEitherAsync(CompletionStage<? extends T> other, Function<? super T, U> fn) {
        throw new UnsupportedOperationException();
    }

    /**
     * @see java.util.concurrent.CompletionStage#applyToEitherAsync(java.util.concurrent.CompletionStage, java.util.function.Function, java.util.concurrent.Executor)
     */
    @Override
    public <U> ManagedCompletableFuture<U> applyToEitherAsync(CompletionStage<? extends T> other, Function<? super T, U> fn, Executor executor) {
        throw new UnsupportedOperationException();
    }

    /**
     * @see java.util.concurrent.CompletableFuture#cancel(boolean)
     */
    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        // TODO also cancel from managed executor
        return completableFuture.cancel(mayInterruptIfRunning);
    }

    /**
     * @see java.util.concurrent.CompletableFuture#complete(T)
     */
    @Override
    public boolean complete(T value) {
        return completableFuture.complete(value);
    }

    /**
     * @see java.util.concurrent.CompletableFuture#completeExceptionally(java.lang.Throwable)
     */
    @Override
    public boolean completeExceptionally(Throwable x) {
        // TODO also cancel from managed executor if completed with a CancellationException
        return completableFuture.completeExceptionally(x);
    }

    /**
     * @see java.util.concurrent.CompletionStage#exceptionally(java.util.function.Function)
     */
    @Override
    public ManagedCompletableFuture<T> exceptionally(Function<Throwable, ? extends T> fn) {
        // TODO contextualize fn and other steps
        return new ManagedCompletableFuture<T>(completableFuture.exceptionally(fn), defaultExecutor);
    }

    /**
     * @see java.util.concurrent.CompletableFuture#get()
     */
    @Override
    public T get() throws ExecutionException, InterruptedException {
        return completableFuture.get();
    }

    /**
     * @see java.util.concurrent.CompletableFuture#get(long, java.util.concurrent.TimeUnit)
     */
    @Override
    public T get(long timeout, TimeUnit unit) throws ExecutionException, InterruptedException, TimeoutException {
        return completableFuture.get(timeout, unit);
    }

    /**
     * @see java.util.concurrent.CompletableFuture#getNow(java.lang.Object)
     */
    @Override
    public T getNow(T valueIfAbsent) {
        return completableFuture.getNow(valueIfAbsent);
    }

    /**
     * @see java.util.concurrent.CompletableFuture#getNumberOfDependents()
     */
    @Override
    public int getNumberOfDependents() {
        return completableFuture.getNumberOfDependents();
    }

    /**
     * @see java.util.concurrent.CompletionStage#handle(java.util.function.BiFunction)
     */
    @Override
    public <U> ManagedCompletableFuture<U> handle(BiFunction<? super T, Throwable, ? extends U> fn) {
        // TODO contextualize fn and other steps
        return new ManagedCompletableFuture<U>(completableFuture.handle(fn), defaultExecutor);
    }

    /**
     * @see java.util.concurrent.CompletionStage#handleAsync(java.util.function.BiFunction)
     */
    @Override
    public <U> ManagedCompletableFuture<U> handleAsync(BiFunction<? super T, Throwable, ? extends U> fn) {
        // TODO contextualize fn and other steps
        Executor policyExecutor = defaultExecutor; // TODO get policy executor from default executor
        return new ManagedCompletableFuture<U>(completableFuture.handleAsync(fn, policyExecutor), defaultExecutor);
    }

    /**
     * @see java.util.concurrent.CompletionStage#handleAsync(java.util.function.BiFunction, java.util.concurrent.Executor)
     */
    @Override
    public <U> ManagedCompletableFuture<U> handleAsync(BiFunction<? super T, Throwable, ? extends U> fn, Executor executor) {
        // TODO contextualize fn and other steps
        Executor policyExecutor = executor; // TODO get policy executor from the supplied executor, if a managed executor
        return new ManagedCompletableFuture<U>(completableFuture.handleAsync(fn, policyExecutor), defaultExecutor);
    }

    /**
     * @see java.util.concurrent.CompletableFuture#isCancelled()
     */
    @Override
    public boolean isCancelled() {
        return completableFuture.isCancelled();
    }

    /**
     * @see java.util.concurrent.CompletableFuture#isCompletedExceptionally()
     */
    @Override
    public boolean isCompletedExceptionally() {
        return completableFuture.isCompletedExceptionally();
    }

    /**
     * @see java.util.concurrent.CompletableFuture#isDone()
     */
    @Override
    public boolean isDone() {
        return completableFuture.isDone();
    }

    /**
     * @see java.util.concurrent.CompletableFuture#join()
     */
    @Override
    public T join() {
        return completableFuture.join();
    }

    /**
     * @see java.util.concurrent.CompletableFuture#obtrudeValue(java.lang.Object)
     */
    @Override
    public void obtrudeValue(T value) {
        completableFuture.obtrudeValue(value);
    }

    /**
     * @see java.util.concurrent.CompletableFuture#obtrudeException(java.lang.Throwable)
     */
    @Override
    public void obtrudeException(Throwable x) {
        completableFuture.obtrudeException(x);
    }

    /**
     * @see java.util.concurrent.CompletionStage#runAfterBoth(java.util.concurrent.CompletionStage, java.lang.Runnable)
     */
    @Override
    public ManagedCompletableFuture<Void> runAfterBoth(CompletionStage<?> other, Runnable action) {
        throw new UnsupportedOperationException();
    }

    /**
     * @see java.util.concurrent.CompletionStage#runAfterBothAsync(java.util.concurrent.CompletionStage, java.lang.Runnable)
     */
    @Override
    public ManagedCompletableFuture<Void> runAfterBothAsync(CompletionStage<?> other, Runnable action) {
        throw new UnsupportedOperationException();
    }

    /**
     * @see java.util.concurrent.CompletionStage#runAfterBothAsync(java.util.concurrent.CompletionStage, java.lang.Runnable, java.util.concurrent.Executor)
     */
    @Override
    public ManagedCompletableFuture<Void> runAfterBothAsync(CompletionStage<?> other, Runnable action, Executor executor) {
        throw new UnsupportedOperationException();
    }

    /**
     * @see java.util.concurrent.CompletionStage#runAfterEither(java.util.concurrent.CompletionStage, java.lang.Runnable)
     */
    @Override
    public ManagedCompletableFuture<Void> runAfterEither(CompletionStage<?> other, Runnable action) {
        throw new UnsupportedOperationException();
    }

    /**
     * @see java.util.concurrent.CompletionStage#runAfterEitherAsync(java.util.concurrent.CompletionStage, java.lang.Runnable)
     */
    @Override
    public ManagedCompletableFuture<Void> runAfterEitherAsync(CompletionStage<?> other, Runnable action) {
        throw new UnsupportedOperationException();
    }

    /**
     * @see java.util.concurrent.CompletionStage#runAfterEitherAsync(java.util.concurrent.CompletionStage, java.lang.Runnable, java.util.concurrent.Executor)
     */
    @Override
    public ManagedCompletableFuture<Void> runAfterEitherAsync(CompletionStage<?> other, Runnable action, Executor executor) {
        throw new UnsupportedOperationException();
    }

    /**
     * @see java.util.concurrent.CompletionStage#thenAccept(java.util.function.Consumer)
     */
    @Override
    public ManagedCompletableFuture<Void> thenAccept(Consumer<? super T> action) {
        // TODO contextualize action and other steps
        return new ManagedCompletableFuture<Void>(completableFuture.thenAccept(action), defaultExecutor);
    }

    /**
     * @see java.util.concurrent.CompletionStage#thenAcceptAsync(java.util.function.Consumer)
     */
    @Override
    public ManagedCompletableFuture<Void> thenAcceptAsync(Consumer<? super T> action) {
        // TODO contextualize action and other steps
        Executor policyExecutor = defaultExecutor; // TODO get policy executor from default executor
        return new ManagedCompletableFuture<Void>(completableFuture.thenAcceptAsync(action, policyExecutor), defaultExecutor);
    }

    /**
     * @see java.util.concurrent.CompletionStage#thenAcceptAsync(java.util.function.Consumer, java.util.concurrent.Executor)
     */
    @Override
    public ManagedCompletableFuture<Void> thenAcceptAsync(Consumer<? super T> action, Executor executor) {
        // TODO contextualize action and other steps
        Executor policyExecutor = executor; // TODO get policy executor from the supplied executor, if a managed executor
        return new ManagedCompletableFuture<Void>(completableFuture.thenAcceptAsync(action, policyExecutor), defaultExecutor);
    }

    /**
     * @see java.util.concurrent.CompletionStage#thenAcceptBoth(java.util.concurrent.CompletionStage, java.util.function.BiConsumer)
     */
    @Override
    public <U> ManagedCompletableFuture<Void> thenAcceptBoth(CompletionStage<? extends U> other, BiConsumer<? super T, ? super U> action) {
        throw new UnsupportedOperationException();
    }

    /**
     * @see java.util.concurrent.CompletionStage#thenAcceptBothAsync(java.util.concurrent.CompletionStage, java.util.function.BiConsumer)
     */
    @Override
    public <U> ManagedCompletableFuture<Void> thenAcceptBothAsync(CompletionStage<? extends U> other, BiConsumer<? super T, ? super U> action) {
        throw new UnsupportedOperationException();
    }

    /**
     * @see java.util.concurrent.CompletionStage#thenAcceptBothAsync(java.util.concurrent.CompletionStage, java.util.function.BiConsumer, java.util.concurrent.Executor)
     */
    @Override
    public <U> ManagedCompletableFuture<Void> thenAcceptBothAsync(CompletionStage<? extends U> other, BiConsumer<? super T, ? super U> action, Executor executor) {
        throw new UnsupportedOperationException();
    }

    /**
     * @see java.util.concurrent.CompletionStage#thenApply(java.util.function.Function)
     */
    @Override
    public <U> ManagedCompletableFuture<U> thenApply(Function<? super T, ? extends U> fn) {
        // TODO contextualize fn and other steps
        return new ManagedCompletableFuture<U>(completableFuture.thenApply(fn), defaultExecutor);
    }

    /**
     * @see java.util.concurrent.CompletionStage#thenApplyAsync(java.util.function.Function)
     */
    @Override
    public <U> ManagedCompletableFuture<U> thenApplyAsync(Function<? super T, ? extends U> fn) {
        // TODO contextualize fn and other steps
        Executor policyExecutor = defaultExecutor; // TODO get policy executor from default executor
        return new ManagedCompletableFuture<U>(completableFuture.thenApplyAsync(fn, policyExecutor), defaultExecutor);
    }

    /**
     * @see java.util.concurrent.CompletionStage#thenApplyAsync(java.util.function.Function, java.util.concurrent.Executor)
     */
    @Override
    public <U> ManagedCompletableFuture<U> thenApplyAsync(Function<? super T, ? extends U> fn, Executor executor) {
        // TODO contextualize fn and other steps
        Executor policyExecutor = executor; // TODO get policy executor from the supplied executor, if a managed executor
        return new ManagedCompletableFuture<U>(completableFuture.thenApplyAsync(fn, policyExecutor), defaultExecutor);
    }

    /**
     * @see java.util.concurrent.CompletionStage#thenCombine(java.util.concurrent.CompletionStage, java.util.function.BiFunction)
     */
    @Override
    public <U, V> ManagedCompletableFuture<V> thenCombine(CompletionStage<? extends U> other, BiFunction<? super T, ? super U, ? extends V> fn) {
        throw new UnsupportedOperationException();
    }

    /**
     * @see java.util.concurrent.CompletionStage#thenCombineAsync(java.util.concurrent.CompletionStage, java.util.function.BiFunction)
     */
    @Override
    public <U, V> ManagedCompletableFuture<V> thenCombineAsync(CompletionStage<? extends U> other, BiFunction<? super T, ? super U, ? extends V> fn) {
        throw new UnsupportedOperationException();
    }

    /**
     * @see java.util.concurrent.CompletionStage#thenCombineAsync(java.util.concurrent.CompletionStage, java.util.function.BiFunction, java.util.concurrent.Executor)
     */
    @Override
    public <U, V> ManagedCompletableFuture<V> thenCombineAsync(CompletionStage<? extends U> other, BiFunction<? super T, ? super U, ? extends V> fn, Executor executor) {
        throw new UnsupportedOperationException();
    }

    /**
     * @see java.util.concurrent.CompletionStage#thenCompose(java.util.function.Function)
     */
    @Override
    public <U> ManagedCompletableFuture<U> thenCompose(Function<? super T, ? extends CompletionStage<U>> fn) {
        throw new UnsupportedOperationException();
    }

    /**
     * @see java.util.concurrent.CompletionStage#thenComposeAsync(java.util.function.Function)
     */
    @Override
    public <U> ManagedCompletableFuture<U> thenComposeAsync(Function<? super T, ? extends CompletionStage<U>> fn) {
        throw new UnsupportedOperationException();
    }

    /**
     * @see java.util.concurrent.CompletionStage#thenComposeAsync(java.util.function.Function, java.util.concurrent.Executor)
     */
    @Override
    public <U> ManagedCompletableFuture<U> thenComposeAsync(Function<? super T, ? extends CompletionStage<U>> fn, Executor executor) {
        throw new UnsupportedOperationException();
    }

    /**
     * @see java.util.concurrent.CompletionStage#thenRun(java.lang.Runnable)
     */
    @Override
    public ManagedCompletableFuture<Void> thenRun(Runnable action) {
        // TODO contextualize action and other steps
        return new ManagedCompletableFuture<Void>(completableFuture.thenRun(action), defaultExecutor);
    }

    /**
     * @see java.util.concurrent.CompletionStage#thenRunAsync(java.lang.Runnable)
     */
    @Override
    public ManagedCompletableFuture<Void> thenRunAsync(Runnable action) {
        // TODO contextualize action and other steps
        Executor policyExecutor = defaultExecutor; // TODO get policy executor from default executor
        return new ManagedCompletableFuture<Void>(completableFuture.thenRunAsync(action, policyExecutor), defaultExecutor);
    }

    /**
     * @see java.util.concurrent.CompletionStage#thenRunAsync(java.lang.Runnable, java.util.concurrent.Executor)
     */
    @Override
    public ManagedCompletableFuture<Void> thenRunAsync(Runnable action, Executor executor) {
        // TODO contextualize action and other steps
        Executor policyExecutor = executor; // TODO get policy executor from the supplied executor, if a managed executor
        return new ManagedCompletableFuture<Void>(completableFuture.thenRunAsync(action, policyExecutor), defaultExecutor);
    }

    // TODO override toString to include proper status (and maybe correlate to the underlying CompletableFuture)

    /**
     * @see java.util.concurrent.CompletionStage#whenComplete(java.util.function.BiConsumer)
     */
    @Override
    public ManagedCompletableFuture<T> whenComplete(BiConsumer<? super T, ? super Throwable> action) {
        // TODO contextualize action and other steps
        return new ManagedCompletableFuture<T>(completableFuture.whenComplete(action), defaultExecutor);
    }

    /**
     * @see java.util.concurrent.CompletionStage#whenCompleteAsync(java.util.function.BiConsumer)
     */
    @Override
    public ManagedCompletableFuture<T> whenCompleteAsync(BiConsumer<? super T, ? super Throwable> action) {
        // TODO contextualize action and other steps
        Executor policyExecutor = defaultExecutor; // TODO get policy executor from default executor
        return new ManagedCompletableFuture<T>(completableFuture.whenCompleteAsync(action, policyExecutor), defaultExecutor);
    }

    /**
     * @see java.util.concurrent.CompletionStage#whenCompleteAsync(java.util.function.BiConsumer, java.util.concurrent.Executor)
     */
    @Override
    public ManagedCompletableFuture<T> whenCompleteAsync(BiConsumer<? super T, ? super Throwable> action, Executor executor) {
        // TODO contextualize action and other steps
        Executor policyExecutor = executor; // TODO get policy executor from the supplied executor, if a managed executor
        return new ManagedCompletableFuture<T>(completableFuture.whenCompleteAsync(action, policyExecutor), defaultExecutor);
    }
}