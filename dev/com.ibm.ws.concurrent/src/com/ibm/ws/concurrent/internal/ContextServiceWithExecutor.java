/*******************************************************************************
 * Copyright (c) 2021,2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.concurrent.internal;

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

import javax.enterprise.concurrent.ContextService;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;

/**
 * A proxy ContextService that is backed by a managed executor.
 * Instances of this class are created by ManagedExecutorService.getContextService
 * to force a one-to-one association between a managed executor and a proxy context service,
 * in situations where it would otherwise be possible for multiple managed executors
 * to be associated with a single shared context service instance (for example,
 * when contextServiceRef is used rather than a nested contextService).
 */
public class ContextServiceWithExecutor implements ContextService {
    private static final TraceComponent tc = Tr.register(ContextServiceWithExecutor.class);

    /**
     * Managed executor that is associated with this instance.
     */
    private final Executor managedExecutor;

    /**
     * ContextService instance that might be shared by multiple managed executors.
     */
    private final ContextServiceImpl sharedContextSvc;

    /**
     * Constructor when used as a declarative services component.
     */
    @Trivial
    public ContextServiceWithExecutor(ContextServiceImpl sharedContextSvc, Executor managedExecutor) {
        this.managedExecutor = managedExecutor;
        this.sharedContextSvc = sharedContextSvc;
    }

    @Trivial
    public <R> Callable<R> contextualCallable(Callable<R> callable) {
        return sharedContextSvc.contextualCallable(callable);
    }

    @Trivial
    public <T, U> BiConsumer<T, U> contextualConsumer(BiConsumer<T, U> consumer) {
        return sharedContextSvc.contextualConsumer(consumer);
    }

    @Trivial
    public <T> Consumer<T> contextualConsumer(Consumer<T> consumer) {
        return sharedContextSvc.contextualConsumer(consumer);
    }

    @Trivial
    public <T, U, R> BiFunction<T, U, R> contextualFunction(BiFunction<T, U, R> function) {
        return sharedContextSvc.contextualFunction(function);
    }

    @Trivial
    public <T, R> Function<T, R> contextualFunction(Function<T, R> function) {
        return sharedContextSvc.contextualFunction(function);
    }

    @Trivial
    public Runnable contextualRunnable(Runnable runnable) {
        return sharedContextSvc.contextualRunnable(runnable);
    }

    @Trivial
    public <R> Supplier<R> contextualSupplier(Supplier<R> supplier) {
        return sharedContextSvc.contextualSupplier(supplier);
    }

    @Override
    @Trivial
    public Object createContextualProxy(Object instance, Class<?>... interfaces) {
        return sharedContextSvc.createContextualProxy(instance, interfaces);
    }

    @Override
    @Trivial
    public Object createContextualProxy(final Object instance, Map<String, String> executionProperties, Class<?>... interfaces) {
        return sharedContextSvc.createContextualProxy(instance, executionProperties, interfaces);
    }

    @Override
    @Trivial
    public <T> T createContextualProxy(T instance, Class<T> intf) {
        return sharedContextSvc.createContextualProxy(instance, intf);
    }

    @Override
    @Trivial
    public <T> T createContextualProxy(T instance, Map<String, String> executionProperties, Class<T> intf) {
        return sharedContextSvc.createContextualProxy(instance, executionProperties, intf);
    }

    @Trivial
    public Executor currentContextExecutor() {
        return sharedContextSvc.currentContextExecutor();
    }

    @Override
    @Trivial
    public Map<String, String> getExecutionProperties(Object contextualProxy) {
        return sharedContextSvc.getExecutionProperties(contextualProxy);
    }

    @Override
    @Trivial
    public final int hashCode() {
        // This instance is unique per its managed executor, not per the shared context service.
        return managedExecutor.hashCode();
    }

    public <T> CompletableFuture<T> withContextCapture(CompletableFuture<T> stage) {
        CompletableFuture<T> newCompletableFuture;

        if (ManagedCompletableFuture.JAVA8)
            newCompletableFuture = new ManagedCompletableFuture<T>(new CompletableFuture<T>(), managedExecutor, null);
        else
            newCompletableFuture = new ManagedCompletableFuture<T>(managedExecutor, null);

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

    public <T> CompletionStage<T> withContextCapture(CompletionStage<T> stage) {
        ManagedCompletionStage<T> newStage;

        if (ManagedCompletableFuture.JAVA8)
            newStage = new ManagedCompletionStage<T>(new CompletableFuture<T>(), managedExecutor, null);
        else
            newStage = new ManagedCompletionStage<T>(managedExecutor);

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
}