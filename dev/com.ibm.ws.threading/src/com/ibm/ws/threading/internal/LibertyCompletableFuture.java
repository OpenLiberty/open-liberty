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
package com.ibm.ws.threading.internal;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;

/**
 * Completable future that is backed by an executor.
 * This implementation is valid for Java 9+. Do not use for Java 8.
 *
 * @param <T> return type of the completable future
 */
public class LibertyCompletableFuture<T> extends CompletableFuture<T> {
    final Executor executor;

    public LibertyCompletableFuture(Executor executor) {
        super();
        this.executor = executor;
    }

    public Executor defaultExecutor() {
        return executor;
    }

    public CompletionStage<T> minimalCompletionStage() {
        return new LibertyCompletionStage<T>(this);
    }

    public CompletableFuture<T> newIncompleteFuture() {
        return new LibertyCompletableFuture<T>(executor);
    }
}