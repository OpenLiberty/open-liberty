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
import org.eclipse.microprofile.concurrent.spi.ThreadContextProvider;

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
    /**
     * Concurrency manager that was used to obtain thread context providers. Null if created by a managed executor.
     */
    private final ConcurrencyManagerImpl concurrencyManager;

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
     * Null if created by a managed executor because it isn't needed on that path.
     */
    private final AtomicReference<ManagedExecutorImpl> managedExecutorRef;

    /**
     * Construct a new instance to be used directly as a MicroProfile ThreadContext service.
     *
     * @param concurrencyManager
     * @param configPerProvider
     */
    ThreadContextImpl(ConcurrencyManagerImpl concurrencyManager, LinkedHashMap<ThreadContextProvider, ContextOp> configPerProvider) {
        this.concurrencyManager = concurrencyManager;
        this.configPerProvider = configPerProvider;
        this.managedExecutorRef = new AtomicReference<ManagedExecutorImpl>();
    }

    /**
     * Create an instance to be used by ManagedExecutor.
     *
     * @param configPerProvider
     */
    ThreadContextImpl(LinkedHashMap<ThreadContextProvider, ContextOp> configPerProvider) {
        this.concurrencyManager = null;
        this.configPerProvider = configPerProvider;
        this.managedExecutorRef = null;
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
     * and propagating the same types as this ThreadContext service, clearing all others.
     * If possible, a cached instance is returned. If it doesn't exist yet, then an instance
     * is lazily created by this method.
     *
     * @return ManagedExecutor instance.
     */
    private ManagedExecutorImpl getManagedExecutor() {
        ManagedExecutorImpl executor = managedExecutorRef.get();

        if (executor == null) {
            StringBuilder nameBuilder = new StringBuilder("ManagedExecutor_-1_-1_");

            // Copy the propagated context types, adding all other types as cleared
            LinkedHashMap<ThreadContextProvider, ContextOp> newConfigPerProvider = new LinkedHashMap<ThreadContextProvider, ContextOp>();
            for (ThreadContextProvider provider : concurrencyManager.contextProviders) {
                ContextOp op = configPerProvider.get(provider);
                if (op == ContextOp.PROPAGATED) {
                    newConfigPerProvider.put(provider, ContextOp.PROPAGATED);
                    String contextType = provider.getThreadContextType();
                    if (contextType != null && contextType.matches("\\w*")) // one or more of a-z, A-Z, _, 0-9
                        nameBuilder.append(contextType).append("_");
                } else {
                    newConfigPerProvider.put(provider, ContextOp.CLEARED);
                }
            }
            ThreadContextImpl threadContextSvc = new ThreadContextImpl(newConfigPerProvider);

            String name = nameBuilder.append(ManagedExecutorBuilderImpl.instanceCount.incrementAndGet()).toString();

            ConcurrencyProviderImpl concurrencyProvider = concurrencyManager.concurrencyProvider;
            PolicyExecutor policyExecutor = concurrencyProvider.policyExecutorProvider.create(name);
            policyExecutor.maxConcurrency(-1).maxQueueSize(-1);
            // TODO these policy executor instances, as well as those created via ManagedExecutorBuilder are never shut down
            // and removed from PolicyExecutorProvider's list. This is a memory leak and needs to be fixed.

            executor = new ManagedExecutorImpl(name, policyExecutor, threadContextSvc, concurrencyProvider.transactionContextProvider.transactionContextProviderRef);

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
        CompletableFuture<T> completableFuture;

        ManagedExecutorImpl executor = getManagedExecutor();
        if (ManagedCompletableFuture.JAVA8)
            completableFuture = new ManagedCompletableFuture<T>(new CompletableFuture<T>(), executor, null);
        else
            completableFuture = new ManagedCompletableFuture<T>(executor, null);

        stage.whenComplete((t, x) -> {
            if (x == null)
                completableFuture.complete(t);
            else
                completableFuture.completeExceptionally(x);
        });

        return completableFuture;
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