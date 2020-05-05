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
package com.ibm.ws.concurrent.mp.spi;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

import com.ibm.ws.concurrent.internal.ManagedCompletableFuture;

/**
 * Provides JAX-RS with access to ManagedCompletableFuture.supplyAsync(supplier, executor)
 */
@Component(configurationPolicy = ConfigurationPolicy.IGNORE, service = { CompletionStageFactory.class })
public class CompletionStageFactory {
    @Reference
    protected ExecutorService libertyGlobalThreadPool;

    /**
     * Creates a CompletableFuture which is backed by the specified executor as its default
     * asynchronous execution facility. If the executor is a ManagedExecutor, then thread context
     * is cleared/captured/propagated/restored for the supplier and all dependent stage actions
     * per the configuration of the ManagedExecutor.
     *
     * @param <T>      type of result that is returned by the supplier.
     * @param supplier operation that supplies a value.
     * @param executor an executor service. Null indicates to use the Liberty global thread pool.
     * @return CompletableFuture that is backed by the specified executor.
     */
    public <T> CompletableFuture<T> supplyAsync(Supplier<T> supplier, ExecutorService executor) {
        return ManagedCompletableFuture.supplyAsync(supplier, executor == null ? libertyGlobalThreadPool : executor);
    }
}
