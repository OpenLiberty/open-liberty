/*
 * Copyright The RestEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.resteasy.concurrent;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture; //Liberty change
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier; //Liberty change

import com.ibm.ws.threading.CompletionStageExecutor; //Liberty change

import io.openliberty.restfulWS.client.AsyncClientExecutorService; //Liberty change

import org.jboss.resteasy.resteasy_jaxrs.i18n.Messages;

/**
 * An {@linkplain ExecutorService executor} which wraps runnables and callables to capture the context of the current
 * thread.
 * <p>
 * If a server is {@linkplain #isManaged() managed} it's the responsibility of the user or container to manage the
 * lifecycle of the wrapped executor service.
 * </p>
 * <p>
 * <strong>Note:</strong> if the executor is consider managed, for example running in a Jakarta EE environment, the
 * following methods are effectively ignored.
 * <ul>
 * <li>{@link #shutdown()}</li>
 * <li>{@link #shutdownNow()}</li>
 * <li>{@link #isShutdown()}</li>
 * <li>{@link #isTerminated()}</li>
 * <li>{@link #awaitTermination(long, TimeUnit)}</li>
 * </ul>
 * </p>
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 * @since 5.0.0
 */
public class ContextualExecutorService implements ExecutorService {

    private final boolean managed;
    private final AtomicBoolean shutdown;
    private volatile ExecutorService delegate;

    ContextualExecutorService(final ExecutorService delegate, final boolean managed) {
        this.delegate = delegate;
        this.managed = managed;
        shutdown = new AtomicBoolean(false);
    }

    @Override
    public void shutdown() {
        if (shutdown.compareAndSet(false, true)) {
            if (isManaged()) {
                // Clear the delegate as we're done with it
                delegate = null;
            } else {
                final ExecutorService delegate = getDelegate();
                delegate.shutdown();
            }
        }
    }

    @Override
    public List<Runnable> shutdownNow() {
        if (shutdown.compareAndSet(false, true)) {
            if (isManaged()) {
                // Clear the delegate as we're done with it
                delegate = null;
            } else {
                final ExecutorService delegate = getDelegate();
                return delegate.shutdownNow();
            }
        }
        return Collections.emptyList();
    }

    @Override
    public boolean isShutdown() {
        if (isManaged()) {
            return shutdown.get();
        }
        return getDelegate().isShutdown();
    }

    @Override
    public boolean isTerminated() {
        if (isManaged()) {
            return false;
        }
        return getDelegate().isTerminated();
    }

    @Override
    public boolean awaitTermination(final long timeout, final TimeUnit unit) throws InterruptedException {
        if (isManaged()) {
            return false;
        }
        return getDelegate().awaitTermination(timeout, unit);
    }

    @Override
    public <T> Future<T> submit(final Callable<T> task) {
        return getDelegate().submit(ContextualExecutors.callable(task));
    }

    @Override
    public <T> Future<T> submit(final Runnable task, final T result) {
        return getDelegate().submit(ContextualExecutors.runnable(task), result);
    }

    @Override
    public Future<?> submit(final Runnable task) {
        return getDelegate().submit(ContextualExecutors.runnable(task));
    }

    @Override
    public <T> List<Future<T>> invokeAll(
            final Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return getDelegate().invokeAll(ContextualExecutors.callable(tasks));
    }

    @Override
    public <T> List<Future<T>> invokeAll(
            final Collection<? extends Callable<T>> tasks, final long timeout,
            final TimeUnit unit) throws InterruptedException {
        return getDelegate().invokeAll(ContextualExecutors.callable(tasks), timeout, unit);
    }

    @Override
    public <T> T invokeAny(final Collection<? extends Callable<T>> tasks)
            throws InterruptedException, ExecutionException {
        return getDelegate().invokeAny(ContextualExecutors.callable(tasks));
    }

    @Override
    public <T> T invokeAny(final Collection<? extends Callable<T>> tasks, final long timeout,
            final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return getDelegate().invokeAny(ContextualExecutors.callable(tasks), timeout, unit);
    }

    @Override
    public void execute(final Runnable command) {
        getDelegate().execute(ContextualExecutors.runnable(command));
    }

    /**
     * Indicates this executor is managed and the following methods are not executed. If the method has a return type
     * a default value is returned.
     * <ul>
     * <li>{@link #shutdown()}</li>
     * <li>{@link #shutdownNow()}</li>
     * <li>{@link #isShutdown()}</li>
     * <li>{@link #isTerminated()}</li>
     * <li>{@link #awaitTermination(long, TimeUnit)}</li>
     * </ul>
     *
     * @return {@code true} if this is a managed executor, otherwise {@code false}
     */
    public boolean isManaged() {
        return managed;
    }

    ExecutorService getDelegate() {
        final ExecutorService delegate = this.delegate;
        if (delegate == null) {
            throw Messages.MESSAGES.executorShutdown();
        }
        return delegate;
    }

  //Liberty change start
    public <T> CompletableFuture<T> supplyAsync(Supplier<T> supplier) {
        ExecutorService executor = getDelegate();
        if (executor instanceof CompletionStageExecutor)
            return ((CompletionStageExecutor) executor).supplyAsync(supplier);
        if (executor instanceof AsyncClientExecutorService) {
            return ((AsyncClientExecutorService)executor).supplyAsync(supplier);
        }
        return CompletableFuture.supplyAsync(supplier, this);
    }
  //Liberty change end
    
}
