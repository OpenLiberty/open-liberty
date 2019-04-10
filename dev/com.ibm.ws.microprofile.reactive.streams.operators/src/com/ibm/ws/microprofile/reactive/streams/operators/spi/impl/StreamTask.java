/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.reactive.streams.operators.spi.impl;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class StreamTask<T> implements Runnable {

    private final PrivilegedAction<CompletionStage<T>> action;
    private final CompletableFuture<T> wrapperCompletableFuture;
    private CompletionStage<T> innerStreamCompletionStage;
    private CompletableFuture<T> resultWrapper;

    /**
     * @param action2
     */
    public <T> StreamTask(PrivilegedAction action2) {
        this.action = action2;
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