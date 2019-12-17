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
package com.ibm.ws.concurrent.mp.spi;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.concurrent.mp.ContextualBiConsumer;
import com.ibm.ws.concurrent.mp.ContextualBiFunction;
import com.ibm.ws.concurrent.mp.ContextualCallable;
import com.ibm.ws.concurrent.mp.ContextualConsumer;
import com.ibm.ws.concurrent.mp.ContextualExecutor;
import com.ibm.ws.concurrent.mp.ContextualFunction;
import com.ibm.ws.concurrent.mp.ContextualRunnable;
import com.ibm.ws.concurrent.mp.ContextualSupplier;
import com.ibm.ws.concurrent.mp.ManagedCompletableFuture;
import com.ibm.ws.concurrent.mp.ManagedCompletionStage;
import com.ibm.ws.concurrent.mp.UnusableExecutor;
import com.ibm.wsspi.threadcontext.ThreadContextDescriptor;
import com.ibm.wsspi.threadcontext.WSContextService;

/**
 * Provides static methods to enable ContextServiceImpl and ThreadContextImpl,
 * which are in a different bundle, to access classes in the com.ibm.ws.concurrent.mp
 * package and not require dual maintenance of code or exporting the package.
 */
public class ContextFactory {
    public static final <R> Callable<R> contextualCallable(Callable<R> callable, ThreadContextConfig config) {
        if (callable instanceof ContextualCallable)
            throw new IllegalArgumentException(ContextualCallable.class.getSimpleName());

        ThreadContextDescriptor contextDescriptor = config.captureThreadContext();
        return new ContextualCallable<R>(contextDescriptor, callable);
    }

    public static final <T, U> BiConsumer<T, U> contextualConsumer(BiConsumer<T, U> consumer, ThreadContextConfig config) {
        if (consumer instanceof ContextualBiConsumer)
            throw new IllegalArgumentException(ContextualBiConsumer.class.getSimpleName());

        ThreadContextDescriptor contextDescriptor = config.captureThreadContext();
        return new ContextualBiConsumer<T, U>(contextDescriptor, consumer);
    }

    public static final <T> Consumer<T> contextualConsumer(Consumer<T> consumer, ThreadContextConfig config) {
        if (consumer instanceof ContextualConsumer)
            throw new IllegalArgumentException(ContextualConsumer.class.getSimpleName());

        ThreadContextDescriptor contextDescriptor = config.captureThreadContext();
        return new ContextualConsumer<T>(contextDescriptor, consumer);
    }

    public static final <T, U, R> BiFunction<T, U, R> contextualFunction(BiFunction<T, U, R> function, ThreadContextConfig config) {
        if (function instanceof ContextualBiFunction)
            throw new IllegalArgumentException(ContextualBiFunction.class.getSimpleName());

        ThreadContextDescriptor contextDescriptor = config.captureThreadContext();
        return new ContextualBiFunction<T, U, R>(contextDescriptor, function);
    }

    public static final <T, R> Function<T, R> contextualFunction(Function<T, R> function, ThreadContextConfig config) {
        if (function instanceof ContextualFunction)
            throw new IllegalArgumentException(ContextualFunction.class.getSimpleName());

        ThreadContextDescriptor contextDescriptor = config.captureThreadContext();
        return new ContextualFunction<T, R>(contextDescriptor, function);
    }

    public static final Runnable contextualRunnable(Runnable runnable, ThreadContextConfig config) {
        if (runnable instanceof ContextualRunnable)
            throw new IllegalArgumentException(ContextualRunnable.class.getSimpleName());

        ThreadContextDescriptor contextDescriptor = config.captureThreadContext();
        return new ContextualRunnable(contextDescriptor, runnable);
    }

    public static final <R> Supplier<R> contextualSupplier(Supplier<R> supplier, ThreadContextConfig config) {
        if (supplier instanceof ContextualSupplier)
            throw new IllegalArgumentException(ContextualSupplier.class.getSimpleName());

        ThreadContextDescriptor contextDescriptor = config.captureThreadContext();
        return new ContextualSupplier<R>(contextDescriptor, supplier);
    }

    public static final Executor currentContextExecutor(ThreadContextConfig config) {
        ThreadContextDescriptor contextDescriptor = config.captureThreadContext();
        return new ContextualExecutor(contextDescriptor);
    }

    public static final <T> CompletableFuture<T> withContextCapture(CompletableFuture<T> stage, WSContextService contextService, Object caller, TraceComponent tc) {
        CompletableFuture<T> newCompletableFuture;

        UnusableExecutor executor = new UnusableExecutor(contextService);
        if (ManagedCompletableFuture.JAVA8)
            newCompletableFuture = new ManagedCompletableFuture<T>(new CompletableFuture<T>(), executor, null);
        else
            newCompletableFuture = new ManagedCompletableFuture<T>(executor, null);

        stage.whenComplete((result, failure) -> {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(caller, tc, "whenComplete", result, failure);
            if (failure == null)
                newCompletableFuture.complete(result);
            else
                newCompletableFuture.completeExceptionally(failure);
        });

        return newCompletableFuture;
    }

    public static final <T> CompletionStage<T> withContextCapture(CompletionStage<T> stage, WSContextService contextService, Object caller, TraceComponent tc) {
        ManagedCompletionStage<T> newStage;

        UnusableExecutor executor = new UnusableExecutor(contextService);
        if (ManagedCompletableFuture.JAVA8)
            newStage = new ManagedCompletionStage<T>(new CompletableFuture<T>(), executor, null);
        else
            newStage = new ManagedCompletionStage<T>(executor);

        stage.whenComplete((result, failure) -> {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(caller, tc, "whenComplete", result, failure);
            if (failure == null)
                newStage.super_complete(result);
            else
                newStage.super_completeExceptionally(failure);
        });

        return newStage;
    }
}
