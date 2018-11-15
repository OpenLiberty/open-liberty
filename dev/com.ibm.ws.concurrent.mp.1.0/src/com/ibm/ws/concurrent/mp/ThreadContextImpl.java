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
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.eclipse.microprofile.concurrent.ThreadContext;
import org.eclipse.microprofile.concurrent.spi.ConcurrencyProvider;
import org.eclipse.microprofile.concurrent.spi.ThreadContextProvider;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.threading.PolicyExecutor;
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
    private static final TraceComponent tc = Tr.register(ThreadContextImpl.class);

    /**
     * Map of thread context provider to type of instruction for applying context to threads.
     * The values are either PROPAGATED or CLEARED. Contexts types that should be left on the
     * thread UNCHANGED are omitted from this map.
     */
    private final LinkedHashMap<ThreadContextProvider, ContextOp> configPerProvider;

    /**
     * Lazily initialized reference to a cached managed executor instance, which is
     * backed by the Liberty global thread pool without concurrency constraints,
     * propagates the type of context configured for this thread context service, and
     * clears all other types of context.
     */
    private final AtomicReference<ManagedExecutorImpl> managedExecutorRef = new AtomicReference<ManagedExecutorImpl>();

    /**
     * Construct a new instance to be used directly as a MicroProfile ThreadContext service or by a ManagedExecutor.
     *
     * @param concurrencyManager
     * @param configPerProvider
     */
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

    /**
     * Obtain a ManagedExecutor backed by the Liberty global thread pool, without constraints,
     * and propagating the same types as this ThreadContext service, clearing those which are
     * configured to be cleared.
     * If possible, a cached instance is returned. If it doesn't exist yet, then an instance
     * is lazily created by this method.
     *
     * @return ManagedExecutor instance.
     */
    private ManagedExecutorImpl getManagedExecutor() {
        ManagedExecutorImpl executor = managedExecutorRef.get();

        if (executor == null) {
            StringBuilder nameBuilder = new StringBuilder("ManagedExecutor_-1_-1_");

            // Identify the propagated context types for the name
            for (Map.Entry<ThreadContextProvider, ContextOp> entry : configPerProvider.entrySet())
                if (entry.getValue() == ContextOp.PROPAGATED) {
                    String contextType = entry.getKey().getThreadContextType();
                    if (contextType != null && contextType.matches("\\w*")) // one or more of a-z, A-Z, _, 0-9
                        nameBuilder.append(contextType).append("_");
                }

            String name = nameBuilder.append(ManagedExecutorBuilderImpl.instanceCount.incrementAndGet()).toString();

            ConcurrencyProviderImpl concurrencyProvider = (ConcurrencyProviderImpl) ConcurrencyProvider.instance();
            PolicyExecutor policyExecutor = concurrencyProvider.policyExecutorProvider.create(name);
            policyExecutor.maxConcurrency(-1).maxQueueSize(-1);
            // TODO these policy executor instances, as well as those created via ManagedExecutorBuilder are never shut down
            // and removed from PolicyExecutorProvider's list. This is a memory leak and needs to be fixed.

            executor = new ManagedExecutorImpl(name, policyExecutor, this, concurrencyProvider.transactionContextProvider.transactionContextProviderRef);

            if (!managedExecutorRef.compareAndSet(null, executor)) {
                // Another thread updated the reference first. Discard the instance we created and use the other.
                executor.shutdown();
                executor = managedExecutorRef.get();
            }
        }

        return executor;
    }

    @Override
    public <T> CompletableFuture<T> withContextCapture(CompletableFuture<T> stage) {
        CompletableFuture<T> newCompletableFuture;

        ManagedExecutorImpl executor = getManagedExecutor();
        if (ManagedCompletableFuture.JAVA8)
            newCompletableFuture = new ManagedCompletableFuture<T>(new CompletableFuture<T>(), executor, null);
        else
            newCompletableFuture = new ManagedCompletableFuture<T>(executor, null);

        stage.whenComplete((result, failure) -> {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(this, tc, "whenComplete", result, failure);
            if (failure == null)
                newCompletableFuture.complete(result);
            else
                newCompletableFuture.completeExceptionally(failure);
        });

        return newCompletableFuture;
    }

    @Override
    public <T> CompletionStage<T> withContextCapture(CompletionStage<T> stage) {
        ManagedCompletionStage<T> newStage;

        ManagedExecutorImpl executor = getManagedExecutor();
        if (ManagedCompletableFuture.JAVA8)
            newStage = new ManagedCompletionStage<T>(new CompletableFuture<T>(), executor, null);
        else
            newStage = new ManagedCompletionStage<T>(executor);

        stage.whenComplete((result, failure) -> {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(this, tc, "whenComplete", result, failure);
            if (failure == null)
                newStage.super_complete(result);
            else
                newStage.super_completeExceptionally(failure);
        });

        return newStage;
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