/*******************************************************************************
 * Copyright (c) 2020,2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.concurrent.ext;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import javax.enterprise.concurrent.ManagedExecutorService;

import org.eclipse.microprofile.context.ManagedExecutor;
import org.eclipse.microprofile.context.ThreadContext;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.concurrent.WSManagedExecutorService;
import com.ibm.ws.concurrent.internal.ManagedExecutorServiceImpl;
import com.ibm.ws.threading.CompletionStageExecutor;
import com.ibm.ws.threading.PolicyExecutor;
import com.ibm.wsspi.resource.ResourceInfo;
import com.ibm.wsspi.threadcontext.ThreadContextDescriptor;
import com.ibm.wsspi.threadcontext.WSContextService;

/**
 * Extend this interface to intercept and replace resource reference lookups for
 * <code>managedExecutorService</code>.
 *
 * At most one extension implementation can be supplied across the entire system.
 * A feature that provides this extension point makes itself incompatible
 * with every other feature that also provides this extension point.
 * Do not implement if this restriction is unacceptable.
 */
@Trivial
public class ManagedExecutorExtension implements CompletionStageExecutor, ManagedExecutor, ManagedExecutorService, WSManagedExecutorService {
    private final WSManagedExecutorService executor;

    protected ManagedExecutorExtension(WSManagedExecutorService executor, ResourceInfo resourceInfo) {
        this.executor = executor;
    }

    @Override
    public final boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return ((ExecutorService) executor).awaitTermination(timeout, unit);
    }

    @Override
    public final ThreadContextDescriptor captureThreadContext(Map<String, String> props) {
        return executor.captureThreadContext(props);
    }

    @Override
    public final <U> CompletableFuture<U> completedFuture(U value) {
        return ((ManagedExecutor) executor).completedFuture(value);
    }

    @Override
    public final <U> CompletionStage<U> completedStage(U value) {
        return ((ManagedExecutor) executor).completedStage(value);
    }

    @Override
    public final <T> CompletableFuture<T> copy(CompletableFuture<T> future) {
        return ((ManagedExecutor) executor).copy(future);
    }

    @Override
    public final <T> CompletionStage<T> copy(CompletionStage<T> stage) {
        return ((ManagedExecutor) executor).copy(stage);
    }

    @Override
    public final void execute(Runnable command) {
        ((ExecutorService) executor).execute(command);
    }

    @Override
    public final <U> CompletableFuture<U> failedFuture(Throwable x) {
        return ((ManagedExecutor) executor).failedFuture(x);
    }

    @Override
    public final <U> CompletionStage<U> failedStage(Throwable x) {
        return ((ManagedExecutor) executor).failedStage(x);
    }

    @Deprecated // being replaced with captureThreadContext so that this method signature can change to spec
    public final WSContextService getContextService() {
        return ((ManagedExecutorServiceImpl) executor).getContextService();
    }

    @Override
    public final PolicyExecutor getLongRunningPolicyExecutor() {
        return executor.getLongRunningPolicyExecutor();
    }

    @Override
    public final PolicyExecutor getNormalPolicyExecutor() {
        return executor.getNormalPolicyExecutor();
    }

    @Override
    public final ThreadContext getThreadContext() {
        return ((ManagedExecutor) executor).getThreadContext();
    }

    @Override
    public final <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return ((ExecutorService) executor).invokeAll(tasks);
    }

    @Override
    public final <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        return ((ExecutorService) executor).invokeAll(tasks, timeout, unit);
    }

    @Override
    public final <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return ((ExecutorService) executor).invokeAny(tasks);
    }

    @Override
    public final <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return ((ExecutorService) executor).invokeAny(tasks, timeout, unit);
    }

    @Override
    public final boolean isShutdown() {
        return ((ExecutorService) executor).isShutdown();
    }

    @Override
    public final boolean isTerminated() {
        return ((ExecutorService) executor).isTerminated();
    }

    @Override
    public final <T> CompletableFuture<T> newIncompleteFuture() {
        return ((ManagedExecutor) executor).newIncompleteFuture();
    }

    @Override
    public final CompletableFuture<Void> runAsync(Runnable action) {
        return ((ManagedExecutor) executor).runAsync(action);
    }

    @Override
    public final void shutdown() {
        ((ExecutorService) executor).shutdown();
    }

    @Override
    public final List<Runnable> shutdownNow() {
        return ((ExecutorService) executor).shutdownNow();
    }

    @Override
    public final <T> Future<T> submit(Callable<T> task) {
        return ((ExecutorService) executor).submit(task);
    }

    @Override
    public final <T> Future<T> submit(Runnable task, T result) {
        return ((ExecutorService) executor).submit(task, result);
    }

    @Override
    public final Future<?> submit(Runnable task) {
        return ((ExecutorService) executor).submit(task);
    }

    @Override
    public final <T> CompletableFuture<T> supplyAsync(Supplier<T> supplier) {
        return ((ManagedExecutor) executor).supplyAsync(supplier);
    }
}
