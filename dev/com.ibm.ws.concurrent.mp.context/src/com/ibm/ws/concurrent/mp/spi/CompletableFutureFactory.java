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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

import com.ibm.ws.concurrent.mp.ManagedCompletableFuture;

/**
 * Provides static methods to enable ManagedExecutorImpl and ManagedScheduledExecutorImpl,
 * which are in a different bundle, to access ManagedCompletableFuture static methods.
 */
public class CompletableFutureFactory {

    public static <U> CompletableFuture<U> completedFuture(U value, Executor executor) {
        return ManagedCompletableFuture.completedFuture(value, executor);
    }

    public static <U> CompletionStage<U> completedStage(U value, Executor executor) {
        return ManagedCompletableFuture.completedStage(value, executor);
    }

    public static <U> CompletableFuture<U> failedFuture(Throwable x, Executor executor) {
        return ManagedCompletableFuture.failedFuture(x, executor);
    }

    public static <U> CompletionStage<U> failedStage(Throwable x, Executor executor) {
        return ManagedCompletableFuture.failedStage(x, executor);
    }

    public static <T> CompletableFuture<T> newIncompleteFuture(Executor executor) {
        return ManagedCompletableFuture.newIncompleteFuture(executor);
    }

    public static CompletableFuture<Void> runAsync(Runnable action, Executor executor) {
        return ManagedCompletableFuture.runAsync(action, executor);
    }

    public static <U> CompletableFuture<U> supplyAsync(Supplier<U> action, Executor executor) {
        return ManagedCompletableFuture.supplyAsync(action, executor);
    }
}
