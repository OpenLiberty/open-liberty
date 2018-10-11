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
import java.util.function.Supplier;

import org.eclipse.microprofile.concurrent.ManagedExecutor;

import com.ibm.ws.concurrent.rx.ManagedCompletableFuture;

/**
 * Super class of ManagedExecutorServiceImpl to be used with Java 8 and above.
 * This class provides implementation of the MicroProfile Concurrency methods.
 * These methods can be collapsed into ManagedExecutorServiceImpl once there is
 * no longer a need for OpenLiberty to support Java 7.
 */
public abstract class ManagedExecutorImpl implements ManagedExecutor {
    @Override
    public <U> CompletableFuture<U> completedFuture(U value) {
        return null; // TODO
    }

    @Override
    public <U> CompletionStage<U> completedStage(U value) {
        return null; // TODO
    }

    @Override
    public <U> CompletableFuture<U> failedFuture(Throwable ex) {
        return null; // TODO
    }

    @Override
    public <U> CompletionStage<U> failedStage(Throwable ex) {
        return null; // TODO
    }

    @Override
    public <U> CompletableFuture<U> newIncompleteFuture() {
        return null; // TODO
    }

    @Override
    public CompletableFuture<Void> runAsync(Runnable runnable) {
        return ManagedCompletableFuture.runAsync(runnable, this);
    }

    @Override
    public <U> CompletableFuture<U> supplyAsync(Supplier<U> supplier) {
        return ManagedCompletableFuture.supplyAsync(supplier, this);
    }
}
