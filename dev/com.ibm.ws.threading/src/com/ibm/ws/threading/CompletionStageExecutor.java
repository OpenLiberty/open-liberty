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
package com.ibm.ws.threading;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * An interface to be implemented by executors to plug in an implementation to which
 * to which CompletionStageFactory delegates when the executor is supplied as a parameter.
 * This is implemented by the com.ibm.ws.concurrent bundle to handle managed executors.
 */
public interface CompletionStageExecutor {
    /**
     * Creates a CompletableFuture that is backed by this executor
     * as its default asynchronous execution facility. All dependent stages,
     * as well as dependent stages of those, and so forth, continue to be
     * backed by this executor.
     *
     * @param <T> type of result that is returned by the CompletableFuture.
     * @return the CompletableFuture.
     */
    public <T> CompletableFuture<T> newIncompleteFuture();

    /**
     * Creates a CompletableFuture that is backed by this executor
     * as its default asynchronous execution facility. All dependent stages,
     * as well as dependent stages of those, and so forth, continue to be
     * backed by this executor.
     *
     * @param action the operation.
     * @return CompletableFuture.
     */
    public CompletableFuture<Void> runAsync(Runnable action);

    /**
     * Creates a CompletableFuture that is backed by this executor
     * as its default asynchronous execution facility. All dependent stages,
     * as well as dependent stages of those, and so forth, continue to be
     * backed by this executor.
     *
     * @param <T>      type of result that is returned by the supplier.
     * @param supplier operation that supplies a value.
     * @return CompletableFuture that is backed (or partly backed if Java 8) by the Liberty global thread pool.
     */
    public <T> CompletableFuture<T> supplyAsync(Supplier<T> supplier);
}
