/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.restfulWS.client;

import static io.openliberty.restfulWS.client.internal.AsyncClientExecutorHelper.wrap;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.jboss.resteasy.resteasy_jaxrs.i18n.Messages;

import com.ibm.ws.threading.CompletionStageExecutor;

import io.openliberty.restfulWS.client.internal.ClientAsyncTaskWrapperComponent;

/**
 * An executor which wraps tasks using {@link ClientAsyncTaskWrapperComponent} before delegating to another executor to run them.
 * <p>
 * Used to allow wrapping of async rest client tasks
 */
public class AsyncClientExecutorService implements ExecutorService {

    private final ExecutorService delegate;

    /**
     * @param delegate the executor to delegate calls to
     */
    public AsyncClientExecutorService(ExecutorService delegate) {
        super();
        this.delegate = delegate;
    }

    private <T> Collection<? extends Callable<T>> wrapTasks(Collection<? extends Callable<T>> tasks) {
        return tasks.stream().map(ClientAsyncTaskWrapperComponent::wrap).collect(Collectors.toList());
    }

    /**
     * @param command
     * @see java.util.concurrent.Executor#execute(java.lang.Runnable)
     */
    public void execute(Runnable command) {
        delegate.execute(wrap(command));
    }

    /**
     *
     * @see java.util.concurrent.ExecutorService#shutdown()
     */
    public void shutdown() {
        delegate.shutdown();
    }

    /**
     * @return
     * @see java.util.concurrent.ExecutorService#shutdownNow()
     */
    public List<Runnable> shutdownNow() {
        return delegate.shutdownNow();
    }

    /**
     * @return
     * @see java.util.concurrent.ExecutorService#isShutdown()
     */
    public boolean isShutdown() {
        return delegate.isShutdown();
    }

    /**
     * @return
     * @see java.util.concurrent.ExecutorService#isTerminated()
     */
    public boolean isTerminated() {
        return delegate.isTerminated();
    }

    /**
     * @param timeout
     * @param unit
     * @return
     * @throws InterruptedException
     * @see java.util.concurrent.ExecutorService#awaitTermination(long, java.util.concurrent.TimeUnit)
     */
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return delegate.awaitTermination(timeout, unit);
    }

    /**
     * @param <T>
     * @param task
     * @return
     * @see java.util.concurrent.ExecutorService#submit(java.util.concurrent.Callable)
     */
    public <T> Future<T> submit(Callable<T> task) {
        return delegate.submit(wrap(task));
    }

    /**
     * @param <T>
     * @param task
     * @param result
     * @return
     * @see java.util.concurrent.ExecutorService#submit(java.lang.Runnable, java.lang.Object)
     */
    public <T> Future<T> submit(Runnable task, T result) {
        return delegate.submit(wrap(task), result);
    }

    /**
     * @param task
     * @return
     * @see java.util.concurrent.ExecutorService#submit(java.lang.Runnable)
     */
    public Future<?> submit(Runnable task) {
        return delegate.submit(wrap(task));
    }

    /**
     * @param <T>
     * @param tasks
     * @return
     * @throws InterruptedException
     * @see java.util.concurrent.ExecutorService#invokeAll(java.util.Collection)
     */
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return delegate.invokeAll(wrapTasks(tasks));
    }

    /**
     * @param <T>
     * @param tasks
     * @param timeout
     * @param unit
     * @return
     * @throws InterruptedException
     * @see java.util.concurrent.ExecutorService#invokeAll(java.util.Collection, long, java.util.concurrent.TimeUnit)
     */
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        return delegate.invokeAll(wrapTasks(tasks), timeout, unit);
    }

    /**
     * @param <T>
     * @param tasks
     * @return
     * @throws InterruptedException
     * @throws ExecutionException
     * @see java.util.concurrent.ExecutorService#invokeAny(java.util.Collection)
     */
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return delegate.invokeAny(wrapTasks(tasks));
    }

    /**
     * @param <T>
     * @param tasks
     * @param timeout
     * @param unit
     * @return
     * @throws InterruptedException
     * @throws ExecutionException
     * @throws TimeoutException
     * @see java.util.concurrent.ExecutorService#invokeAny(java.util.Collection, long, java.util.concurrent.TimeUnit)
     */
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return delegate.invokeAny(wrapTasks(tasks), timeout, unit);       
    }
  
    ExecutorService getDelegate() {
        final ExecutorService delegate = this.delegate;
        if (delegate == null) {
            throw Messages.MESSAGES.executorShutdown();
        }
        return delegate;
    }
    
    public <T> CompletableFuture<T> supplyAsync(Supplier<T> supplier) {
        ExecutorService executor = getDelegate();
        if (executor instanceof CompletionStageExecutor) {
            return ((CompletionStageExecutor) executor).supplyAsync(wrap(supplier));
        }
        // No need to wrap here as tasks will be wrapped when CompletableFuture submits them to this executor
        return CompletableFuture.supplyAsync(supplier, this);
    }

}
