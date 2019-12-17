/*******************************************************************************
 * Copyright (c) 2018,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.concurrent.mp.service;

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

import org.eclipse.microprofile.context.ThreadContext;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.concurrent.mp.spi.ContextFactory;
import com.ibm.ws.concurrent.mp.spi.ThreadContextConfig;
import com.ibm.wsspi.threadcontext.ThreadContextDescriptor;
import com.ibm.wsspi.threadcontext.WSContextService;

/**
 * Programmatically built ThreadContext instance - via ThreadContextBuilder.
 *
 * TODO eventually this should be merged with ContextServiceImpl such that it also
 * implements EE Concurrency ContextService. However, because the MP Context Propagation spec
 * is not covering serializable thread context in its initial version, we must defer
 * this to the future. In the mean time, there will be duplication of the MP Context Propagation
 * method implementations between the two.
 */
public class ThreadContextImpl implements ThreadContext, WSContextService {
    private static final TraceComponent tc = Tr.register(ThreadContextImpl.class);

    /**
     * Represents the configured context propagation settings.
     */
    private final ThreadContextConfig config;

    /**
     * Hash code for this instance.
     */
    private final int hash;

    /**
     * Unique name for this instance.
     */
    private final String name;

    /**
     * Construct a new instance to be used directly as a MicroProfile ThreadContext service or by a ManagedExecutor.
     *
     * @param name unique name for this instance.
     * @param int hash hash code for this instance.
     * @param config represents thread context propagation configuration.
     */
    public ThreadContextImpl(String name, int hash, ThreadContextConfig config) {
        this.config = config;
        this.name = name;
        this.hash = hash;
    }

    @Override
    public final ThreadContextDescriptor captureThreadContext(Map<String, String> executionProperties,
                                                              @SuppressWarnings("unchecked") Map<String, ?>... additionalThreadContextConfig) {
        return config.captureThreadContext();
    }

    @Override
    public final <R> Callable<R> contextualCallable(Callable<R> callable) {
        return ContextFactory.contextualCallable(callable, config);
    }

    @Override
    public final <T, U> BiConsumer<T, U> contextualConsumer(BiConsumer<T, U> consumer) {
        return ContextFactory.contextualConsumer(consumer, config);
    }

    @Override
    public final <T> Consumer<T> contextualConsumer(Consumer<T> consumer) {
        return ContextFactory.contextualConsumer(consumer, config);
    }

    @Override
    public final <T, U, R> BiFunction<T, U, R> contextualFunction(BiFunction<T, U, R> function) {
        return ContextFactory.contextualFunction(function, config);
    }

    @Override
    public final <T, R> Function<T, R> contextualFunction(Function<T, R> function) {
        return ContextFactory.contextualFunction(function, config);
    }

    @Override
    public final Runnable contextualRunnable(Runnable runnable) {
        return ContextFactory.contextualRunnable(runnable, config);
    }

    @Override
    public final <R> Supplier<R> contextualSupplier(Supplier<R> supplier) {
        return ContextFactory.contextualSupplier(supplier, config);
    }

    @Override
    public final <T> T createContextualProxy(ThreadContextDescriptor threadContextDescriptor, T instance, Class<T> intf) {
        throw new UnsupportedOperationException(); // not needed by ManagedCompletableFuture or ManagedExecutorServiceImpl
    }

    @Override
    public final Executor currentContextExecutor() {
        return ContextFactory.currentContextExecutor(config);
    }

    @Override
    @Trivial
    public final int hashCode() {
        return hash;
    }

    @Override
    @Trivial
    public final String toString() {
        return name;
    }

    @Override
    public final <T> CompletableFuture<T> withContextCapture(CompletableFuture<T> stage) {
        return ContextFactory.withContextCapture(stage, this, this, tc);
    }

    @Override
    public final <T> CompletionStage<T> withContextCapture(CompletionStage<T> stage) {
        return ContextFactory.withContextCapture(stage, this, this, tc);
    }
}