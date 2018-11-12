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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.eclipse.microprofile.concurrent.ThreadContext;
import org.eclipse.microprofile.concurrent.spi.ThreadContextProvider;

import com.ibm.wsspi.threadcontext.ThreadContextDescriptor;
import com.ibm.wsspi.threadcontext.WSContextService;

/**
 * Programmatically built ThreadContext instance - either via ThreadContextBuilder
 * or injected by CDI and possibly annotated by <code>@ThreadContextConfig</code>
 *
 * TODO eventually this should be merged with ContextServiceImpl such that it also
 * implements EE Concurrency ContextService. However, because the MP Concurrency spec
 * is not covering serializable thread context in its initial version, we must defer
 * this to the future. In the mean time, there will be duplicate of the MP Concurrency
 * method implementations between the two.
 */
class ThreadContextImpl implements ThreadContext, WSContextService {
    private final LinkedHashMap<ThreadContextProvider, ContextOp> configPerProvider;

    ThreadContextImpl(LinkedHashMap<ThreadContextProvider, ContextOp> configPerProvider) {
        this.configPerProvider = configPerProvider;
    }

    @Override
    public ThreadContextDescriptor captureThreadContext(Map<String, String> executionProperties,
                                                        @SuppressWarnings("unchecked") Map<String, ?>... additionalThreadContextConfig) {
        return new ThreadContextDescriptorImpl(configPerProvider);
    }

    @Override
    public <T> T createContextualProxy(ThreadContextDescriptor threadContextDescriptor, T instance, Class<T> intf) {
        throw new UnsupportedOperationException(); // not needed by ManagedCompletableFuture or ManagedExecutorServiceImpl
    }

    @Override
    public Executor currentContextExecutor() {
        ThreadContextDescriptor contextDescriptor = new ThreadContextDescriptorImpl(configPerProvider);
        return new ContextualExecutor(contextDescriptor);
    }

    @Override
    public <T> CompletableFuture<T> withContextCapture(CompletableFuture<T> stage) {
        return null; // TODO
    }

    @Override
    public <T> CompletionStage<T> withContextCapture(CompletionStage<T> stage) {
        return null; // TODO
    }

    @Override
    public <T, U> BiConsumer<T, U> withCurrentContext(BiConsumer<T, U> consumer) {
        ThreadContextDescriptor contextDescriptor = new ThreadContextDescriptorImpl(configPerProvider);
        return new ContextualBiConsumer<T, U>(contextDescriptor, consumer);
    }

    @Override
    public <T, U, R> BiFunction<T, U, R> withCurrentContext(BiFunction<T, U, R> function) {
        ThreadContextDescriptor contextDescriptor = new ThreadContextDescriptorImpl(configPerProvider);
        return new ContextualBiFunction<T, U, R>(contextDescriptor, function);
    }

    @Override
    public <R> Callable<R> withCurrentContext(Callable<R> callable) {
        ThreadContextDescriptor contextDescriptor = new ThreadContextDescriptorImpl(configPerProvider);
        return new ContextualCallable<R>(contextDescriptor, callable);
    }

    @Override
    public <T> Consumer<T> withCurrentContext(Consumer<T> consumer) {
        ThreadContextDescriptor contextDescriptor = new ThreadContextDescriptorImpl(configPerProvider);
        return new ContextualConsumer<T>(contextDescriptor, consumer);
    }

    @Override
    public <T, R> Function<T, R> withCurrentContext(Function<T, R> function) {
        ThreadContextDescriptor contextDescriptor = new ThreadContextDescriptorImpl(configPerProvider);
        return new ContextualFunction<T, R>(contextDescriptor, function);
    }

    @Override
    public Runnable withCurrentContext(Runnable runnable) {
        ThreadContextDescriptor contextDescriptor = new ThreadContextDescriptorImpl(configPerProvider);
        return new ContextualRunnable(contextDescriptor, runnable);
    }

    @Override
    public <R> Supplier<R> withCurrentContext(Supplier<R> supplier) {
        ThreadContextDescriptor contextDescriptor = new ThreadContextDescriptorImpl(configPerProvider);
        return new ContextualSupplier<R>(contextDescriptor, supplier);
    }
}