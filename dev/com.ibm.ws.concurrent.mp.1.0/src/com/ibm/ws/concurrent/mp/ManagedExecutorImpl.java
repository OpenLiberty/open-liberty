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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 * Super class of ManagedExecutorServiceImpl to be used with Java 8 and above.
 * This class provides implementation of the MicroProfile Concurrency methods.
 * These methods can be collapsed into ManagedExecutorServiceImpl once there is
 * no longer a need for OpenLiberty to support Java 7.
 */
@Trivial
public abstract class ManagedExecutorImpl implements ExecutorService { // TODO switch to org.eclipse.microprofile.concurrent.ManagedExecutor
    public <U> CompletableFuture<U> completedFuture(U value) {
        return null; // TODO
    }

    public <U> CompletionStage<U> completedStage(U value) {
        return null; // TODO
    }

    public <U> CompletableFuture<U> failedFuture(Throwable ex) {
        return null; // TODO
    }

    public <U> CompletionStage<U> failedStage(Throwable ex) {
        return null; // TODO
    }

    public <U> CompletableFuture<U> newIncompleteFuture() {
        return null; // TODO
    }

    public CompletableFuture<Void> runAsync(Runnable runnable) {
        return null; // TODO ManagedCompletableFuture.runAsync(runnable, this);
    }

    public <U> CompletableFuture<U> supplyAsync(Supplier<U> supplier) {
        return null; // TODO ManagedCompletableFuture.supplyAsync(supplier, this);
    }
}
