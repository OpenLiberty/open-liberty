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
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.kernel.service.util.JavaInfo;
import com.ibm.ws.threading.internal.LibertyCompletableFuture;

/**
 * Creates completion stages that are backed by the Liberty global executor or the specified executor.
 * When an executor is specified, the following precedence applies:
 *
 * <ul>
 * <li>In the case of CompletionStageExecutor (implemented by managed executors), delegates to the executor to provide a CompletableFuture.</li>
 * <li>In the case of Java 9+, implements based on the defaultExecutor/newIncompleteFuture methods of the Java CompletableFuture.</li>
 * <li>In the case of Java 8, some methods are able to run the initial stage on the Liberty global executor or specified executor.</li>
 * </ul>
 */
@Component(configurationPolicy = ConfigurationPolicy.IGNORE, service = { CompletionStageFactory.class })
public class CompletionStageFactory {
    /**
     * Indicates if running on Java SE 8.
     */
    private static final boolean JAVA8 = JavaInfo.majorVersion() == 8;

    /**
     * Use the Liberty global thread pool when the executor is unspecified.
     */
    @Reference(target = "(component.name=com.ibm.ws.threading)")
    private ExecutorService globalExecutor;

    /**
     * Creates a CompletableFuture which is backed (if Java 9+)
     * by the Liberty global thread pool.
     *
     * @param <T> type of result that is returned by the CompletableFuture.
     * @return the CompletableFuture.
     */
    @Trivial
    public <T> CompletableFuture<T> newIncompleteFuture() {
        return newIncompleteFuture(globalExecutor);
    }

    /**
     * Creates a CompletableFuture which is backed (if Java 9+ or CompletionStageExecutor is supplied)
     * by the specified executor as its default asynchronous execution facility.
     * If the com.ibm.ws.concurrent bundle is enabled and the executor is a managed executor,
     * then thread context is cleared/captured/propagated/restored for the supplier
     * and all dependent stage actions per the configuration of the managed executor.
     *
     * @param <T>      type of result that is returned by the CompletableFuture.
     * @param executor an executor. Null indicates to use the Liberty global thread pool.
     * @return the CompletableFuture.
     */
    public <T> CompletableFuture<T> newIncompleteFuture(Executor executor) {
        if (executor instanceof CompletionStageExecutor)
            return ((CompletionStageExecutor) executor).newIncompleteFuture();

        executor = executor == null ? globalExecutor : executor;
        if (JAVA8)
            return new CompletableFuture<T>(); // this method provides no additional value on Java SE 8
        else
            return new LibertyCompletableFuture<T>(executor);
    }

    /**
     * Creates a CompletableFuture which is backed (or partly backed if Java 8) by the Liberty global thread pool.
     *
     * @param action the operation.
     * @return CompletableFuture that is backed (or partly backed if Java 8) by the Liberty global thread pool.
     */
    @Trivial
    public CompletableFuture<Void> runAsync(Runnable action) {
        return runAsync(action, globalExecutor);
    }

    /**
     * Creates a CompletableFuture which is backed (or partly backed if Java 8) by the specified
     * executor as its default asynchronous execution facility.
     * If the com.ibm.ws.concurrent bundle is enabled and the executor is a managed executor,
     * then thread context is cleared/captured/propagated/restored for the runnable action
     * and all dependent stage actions per the configuration of the managed executor.
     *
     * @param action   the operation.
     * @param executor an executor. Null indicates to use the Liberty global thread pool.
     * @return CompletableFuture that is backed (or partly backed if Java 8) by the specified executor.
     */
    public CompletableFuture<Void> runAsync(Runnable action, Executor executor) {
        if (executor instanceof CompletionStageExecutor)
            return ((CompletionStageExecutor) executor).runAsync(action);

        executor = executor == null ? globalExecutor : executor;
        if (JAVA8)
            return CompletableFuture.runAsync(action, executor); // at least run the initial stage on the executor
        else {
            LibertyCompletableFuture<Void> future = new LibertyCompletableFuture<Void>(executor);
            executor.execute(new RunAsync(action, future));
            return future;
        }
    }

    private static class RunAsync implements Runnable {
        private final Runnable action;
        private final LibertyCompletableFuture<Void> future;

        RunAsync(Runnable action, LibertyCompletableFuture<Void> future) {
            this.action = action;
            this.future = future;
        }

        @FFDCIgnore(Throwable.class) // exception is from customer code path, not Liberty
        @Override
        public void run() {
            try {
                action.run();
                future.complete(null);
            } catch (Throwable x) {
                future.completeExceptionally(x);
            }
        }
    }

    /**
     * Creates a CompletableFuture which is backed (or partly backed if Java 8) by the Liberty global thread pool.
     *
     * @param <T>      type of result that is returned by the supplier.
     * @param supplier operation that supplies a value.
     * @return CompletableFuture that is backed (or partly backed if Java 8) by the Liberty global thread pool.
     */
    @Trivial
    public <T> CompletableFuture<T> supplyAsync(Supplier<T> supplier) {
        return supplyAsync(supplier, globalExecutor);
    }

    /**
     * Creates a CompletableFuture which is backed (or partly backed if Java 8) by the specified
     * executor as its default asynchronous execution facility.
     * If the com.ibm.ws.concurrent bundle is enabled and the executor is a managed executor,
     * then thread context is cleared/captured/propagated/restored for the supplier
     * and all dependent stage actions per the configuration of the managed executor.
     *
     * @param <T>      type of result that is returned by the supplier.
     * @param supplier operation that supplies a value.
     * @param executor an executor. Null indicates to use the Liberty global thread pool.
     * @return CompletableFuture that is backed (or partly backed if Java 8) by the specified executor.
     */
    public <T> CompletableFuture<T> supplyAsync(Supplier<T> supplier, Executor executor) {
        if (executor instanceof CompletionStageExecutor)
            return ((CompletionStageExecutor) executor).supplyAsync(supplier);

        executor = executor == null ? globalExecutor : executor;
        if (JAVA8)
            return CompletableFuture.supplyAsync(supplier, executor); // at least run the initial stage on the executor
        else {
            LibertyCompletableFuture<T> future = new LibertyCompletableFuture<T>(executor);
            // TODO once Java 8 is dropped, replace reflection with:
            // TODO future.completeAsync(supplier, executor)
            executor.execute(new SupplyAsync<T>(supplier, future));
            return future;
        }
    }

    private static class SupplyAsync<T> implements Runnable {
        private final Supplier<T> supplier;
        private final LibertyCompletableFuture<T> future;

        SupplyAsync(Supplier<T> supplier, LibertyCompletableFuture<T> future) {
            this.supplier = supplier;
            this.future = future;
        }

        @FFDCIgnore(Throwable.class) // exception is from customer code path, not Liberty
        @Override
        public void run() {
            try {
                T result = supplier.get();
                future.complete(result);
            } catch (Throwable x) {
                future.completeExceptionally(x);
            }
        }
    }
}
