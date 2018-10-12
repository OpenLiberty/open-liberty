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

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.wsspi.threadcontext.WSContextService;

/**
 * Super class of ContextServiceImpl to be used with Java 8 and above.
 * This class provides implementation of the MicroProfile Concurrency methods.
 * These methods can be collapsed into ContextServiceImpl once there is
 * no longer a need for OpenLiberty to support Java 7.
 */
@Trivial
public abstract class ThreadContextImpl implements ThreadContext, WSContextService {
    @Override
    public Executor currentContextExecutor() {
        return null; // TODO
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
        return null; // TODO
    }

    @Override
    public <T, U, R> BiFunction<T, U, R> withCurrentContext(BiFunction<T, U, R> function) {
        return null; // TODO
    }

    @Override
    public <R> Callable<R> withCurrentContext(Callable<R> callable) {
        return null; // TODO
    }

    @Override
    public <T> Consumer<T> withCurrentContext(Consumer<T> consumer) {
        return null; // TODO
    }

    @Override
    public <T, R> Function<T, R> withCurrentContext(Function<T, R> function) {
        return null; // TODO
    }

    @Override
    public Runnable withCurrentContext(Runnable runnable) {
        return null; // TODO
    }

    @Override
    public <R> Supplier<R> withCurrentContext(Supplier<R> supplier) {
        return null; // TODO
    }
}
