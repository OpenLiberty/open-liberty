/*******************************************************************************
 * Copyright (c) 2019, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.reactive.streams.operators30.spi.impl;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class StreamTask<T> implements Runnable {

    private final PrivilegedAction<CompletionStage<T>> action;
    private final CompletableFuture<T> wrapperCompletableFuture;
    private CompletionStage<T> innerStreamCompletionStage;
    private CompletableFuture<T> resultWrapper;

    public StreamTask(PrivilegedAction<CompletionStage<T>> action) {
        this.action = action;
        this.wrapperCompletableFuture = new CompletableFuture<>();
    }

    public CompletionStage<T> getInnerStreamCompletionStage() {
        return innerStreamCompletionStage;
    }

    public CompletableFuture<T> getWrapperCompletableFuture() {
        return wrapperCompletableFuture;
    }

    @Override
    public void run() {
        innerStreamCompletionStage = AccessController.doPrivileged(action);
        innerStreamCompletionStage.thenAccept(wrapperCompletableFuture::complete);
        innerStreamCompletionStage.exceptionally((ex) -> {
            wrapperCompletableFuture.completeExceptionally(ex);
            return null;
        });

    }

    /**
     * @return
     */
    public CompletableFuture<T> getResultWrapper() {
        return resultWrapper;
    }

}