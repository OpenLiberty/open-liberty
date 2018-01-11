/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.concurrent.rx;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.concurrent.ManagedTask;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.concurrent.WSManagedExecutorService;
import com.ibm.wsspi.threadcontext.ThreadContext;
import com.ibm.wsspi.threadcontext.ThreadContextDescriptor;
import com.ibm.wsspi.threadcontext.WSContextService;

/**
 * Extension to CompletableFuture for managed executors.
 *
 * This is needed because the Java SE CompletableFuture does not allow having a managed executor
 * as its default asynchronous execution facility. Nor does it behave properly with managed executors
 * when explicitly specifying a managed executor when adding each dependent stage - the thread context
 * propagation is undesirably non-deterministic, depending on whether or not previous stages happened
 * to have completed or not at the point in time when the dependent stage is added.
 *
 * We would preferably provide a CompletionStage implementation of our own, except that CompletionStage
 * interface is unfortunately bound to CompletableFuture by its toCompletableFuture method.
 * Technically, implementation of this method is optional, but at the price of not being able to
 * interoperate with other CompletionStages. In practice, users will certainly expect that ability,
 * plus the other capabilities that CompletableFuture provides such as forcing/triggering completion,
 * reporting status, and obtaining results at a later point.
 *
 * Because there is neither an interface for CompletableFuture, nor is there any built-in support for
 * plugging in extensions (method implementations return CompletableFuture, not the CompletableFuture subclass)
 * this implementation attempts to work around these limitations by using two instances of CompletableFuture:
 * one for the subclass itself, which includes the override logic, which delegates to another CompletableFuture
 * instance that does the actual work.
 *
 * @param <T>
 */
public class ManagedCompletableFuture<T> extends CompletableFuture<T> {
    private static final TraceComponent tc = Tr.register(ManagedCompletableFuture.class);

    /**
     * Execution property that indicates a task should run with any previous transaction suspended.
     */
    private static Map<String, String> XPROPS_SUSPEND_TRAN = Collections.singletonMap(ManagedTask.TRANSACTION, ManagedTask.SUSPEND);

    /**
     * Privileged action that obtains the default instance of ManagedExecutorService from the service registry.
     * This is the same as looking up java:comp/DefaultManagedExecutorService, except doesn't require the jndi-1.0 feature.
     */
    private static PrivilegedAction<ManagedExecutorService> getDefaultManagedExecutorAction = () -> {
        BundleContext bc = FrameworkUtil.getBundle(ManagedCompletableFuture.class).getBundleContext();
        Collection<ServiceReference<ManagedExecutorService>> refs;
        try {
            refs = bc.getServiceReferences(ManagedExecutorService.class, "(id=DefaultManagedExecutorService)");
        } catch (InvalidSyntaxException x) {
            throw new RuntimeException(x); // should never happen
        }
        if (refs.isEmpty())
            throw new IllegalStateException("DefaultManagedExecutorService");
        return bc.getService(refs.iterator().next());
    };

    /**
     * The real completable future to which meaningful operations are delegated.
     */
    private final CompletableFuture<T> completableFuture;

    /**
     * The default asynchronous execution facility for this completable future.
     * Typically, this is a managed executor, but it is possible to use any executor.
     * (For example, the Liberty global thread pool)
     */
    private final Executor defaultExecutor;

    /**
     * Reference to the policy executor Future (if any) upon which the action runs.
     * Reference is null when the action cannot be async.
     * Value is null when an async action has not yet been submitted.
     */
    private final AtomicReference<Future<?>> futureRef;

    /**
     * Redirects the CompletableFuture implementation to use ExecutorService.submit rather than Executor.execute,
     * such that we can track the underlying Future that is used for the CompletableFuture, so that we can monitor
     * and cancel it.
     * Instances of this class are intended for one-time use, as each instance tracks a single Future.
     */
    @Trivial
    private static class FutureRefExecutor extends AtomicReference<Future<?>> implements Executor {
        private static final long serialVersionUID = 1L;

        private final ExecutorService executor;

        /**
         * Constructor when executor is not a managed executor service.
         *
         * @param executorService
         */
        private FutureRefExecutor(ExecutorService executorService) {
            executor = executorService;
        }

        /**
         * Constructor when executor is a managed executor service.
         *
         * @param managedExecutor
         */
        private FutureRefExecutor(WSManagedExecutorService managedExecutor) {
            executor = managedExecutor.getNormalPolicyExecutor(); // TODO choose based on LONGRUNNING_HINT execution property
        }

        @Override
        public void execute(Runnable command) {
            set(executor.submit(command));
        }

        @Override
        public String toString() {
            return executor.toString() + ": " + get();
        }
    }

    // Constructor equivalents for CompletableFuture

    /**
     * Construct a new incomplete ManagedCompletableFuture where the default managed executor (java:comp/DefaultManagedExecutorService)
     * is the default asynchronous execution facility.
     */
    @Trivial
    public ManagedCompletableFuture() {
        this(AccessController.doPrivileged(getDefaultManagedExecutorAction));
    }

    /**
     * Construct a new incomplete ManagedCompletableFuture where the specified executor (typically a managed executor)
     * is the default asynchronous execution facility.
     *
     * @param executor executor to become the default asynchronous execution facility.
     */
    @Trivial
    public ManagedCompletableFuture(Executor executor) {
        // The approach of creating a new CompletableFuture to fit the code works, but it ought to be possible to optimize to avoid this.
        this(new CompletableFuture<T>(), executor, null);
    }

    /**
     * Construct a completable future with a managed executor as its default asynchronous execution facility.
     *
     * @param completableFuture underlying completable future upon which this instance is backed.
     * @param managedExecutor managed executor service
     * @param futureRef reference to a policy executor Future that will be submitted if requested to run async. Otherwise null.
     */
    private ManagedCompletableFuture(CompletableFuture<T> completableFuture, Executor managedExecutor, AtomicReference<Future<?>> futureRef) {
        super();

        this.completableFuture = completableFuture;
        this.defaultExecutor = managedExecutor;
        this.futureRef = futureRef;

        // For the sake of operations that rely upon CompletableFuture internals, update the state of the super class upon completion:
        completableFuture.whenComplete((result, failure) -> {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(ManagedCompletableFuture.this, tc, "whenComplete", result, failure);
            if (failure == null)
                super.complete(result);
            else
                super.completeExceptionally(failure);
        });
    }

    // static method equivalents for CompletableFuture

    /**
     * Replaces CompletableFuture.completedFuture(value) with an implementation that switches the
     * default asynchronous execution facility to be the default managed executor.
     *
     * @param value result of the completed future
     * @return completed completable future where the default managed executor is the default asynchronous execution facility.
     */
    public static <U> ManagedCompletableFuture<U> completedFuture(U value) {
        return new ManagedCompletableFuture<U>( //
                        CompletableFuture.completedFuture(value), //
                        AccessController.doPrivileged(getDefaultManagedExecutorAction), //
                        null);
    }

    /**
     * Replaces CompletableFuture.runAsync(action) with an implementation that switches the
     * default asynchronous execution facility to be the default managed executor.
     *
     * @param action the action to run asynchronously.
     * @return completable future where the default managed executor is the default asynchronous execution facility.
     */
    public static ManagedCompletableFuture<Void> runAsync(Runnable action) {
        return runAsync(action, AccessController.doPrivileged(getDefaultManagedExecutorAction));
    }

    /**
     * Replaces CompletableFuture.runAsync(action, executor) with an implementation that switches the
     * default asynchronous execution facility to be the specified managed executor.
     *
     * @param action the action to run asynchronously.
     * @param executor the executor, typically a managed executor, that becomes the default asynchronous execution facility for the completable future.
     * @return completable future where the specified managed executor is the default asynchronous execution facility.
     */
    public static ManagedCompletableFuture<Void> runAsync(Runnable action, Executor executor) {
        FutureRefExecutor futureExecutor;
        if (executor instanceof ManagedExecutorService) { // the only type of managed executor implementation allowed here is the built-in one
            // Reject ManagedTask so that we have the flexibility to decide later how to handle ManagedTaskListener and execution properties
            if (action instanceof ManagedTask)
                throw new IllegalArgumentException(ManagedTask.class.getName());

            WSManagedExecutorService managedExecutor = (WSManagedExecutorService) executor;
            WSContextService contextSvc = managedExecutor.getContextService();

            @SuppressWarnings("unchecked")
            ThreadContextDescriptor contextDescriptor = contextSvc.captureThreadContext(XPROPS_SUSPEND_TRAN);
            action = contextSvc.createContextualProxy(contextDescriptor, action, Runnable.class);

            futureExecutor = new FutureRefExecutor(managedExecutor);
        } else if (executor instanceof ExecutorService) {
            futureExecutor = new FutureRefExecutor((ExecutorService) executor);
        } else {
            futureExecutor = null;
        }
        CompletableFuture<Void> completableFuture = CompletableFuture.runAsync(action, futureExecutor);
        return new ManagedCompletableFuture<Void>(completableFuture, executor, futureExecutor);
    }

    /**
     * Replaces CompletableFuture.supplyAsync(supplier) with an implementation that switches the
     * default asynchronous execution facility to be the default managed executor.
     *
     * @param action the supplier to invoke asynchronously.
     * @return completable future where the default managed executor is the default asynchronous execution facility.
     */
    public static <U> ManagedCompletableFuture<U> supplyAsync(Supplier<U> action) {
        return supplyAsync(action, AccessController.doPrivileged(getDefaultManagedExecutorAction));
    }

    /**
     * Replaces CompletableFuture.supplyAsync(supplier, executor) with an implementation that switches the
     * default asynchronous execution facility to be the specified managed executor.
     *
     * @param action the supplier to invoke asynchronously.
     * @param executor the executor, typically a managed executor, that becomes the default asynchronous execution facility for the completable future.
     * @return completable future where the specified managed executor is the default asynchronous execution facility.
     */
    public static <U> ManagedCompletableFuture<U> supplyAsync(Supplier<U> action, Executor executor) {
        FutureRefExecutor futureExecutor;
        if (executor instanceof ManagedExecutorService) { // the only type of managed executor implementation allowed here is the built-in one
            // Reject ManagedTask so that we have the flexibility to decide later how to handle ManagedTaskListener and execution properties
            if (action instanceof ManagedTask)
                throw new IllegalArgumentException(ManagedTask.class.getName());

            WSManagedExecutorService managedExecutor = (WSManagedExecutorService) executor;
            WSContextService contextSvc = managedExecutor.getContextService();

            @SuppressWarnings("unchecked")
            ThreadContextDescriptor contextDescriptor = contextSvc.captureThreadContext(XPROPS_SUSPEND_TRAN);
            action = new ContextualSupplier<U>(contextDescriptor, action);

            futureExecutor = new FutureRefExecutor(managedExecutor);
        } else if (executor instanceof ExecutorService) {
            futureExecutor = new FutureRefExecutor((ExecutorService) executor);
        } else {
            futureExecutor = null;
        }
        CompletableFuture<U> completableFuture = CompletableFuture.supplyAsync(action, futureExecutor);
        return new ManagedCompletableFuture<U>(completableFuture, executor, futureExecutor);
    }

    // Overrides of CompletableFuture methods

    /**
     * @see java.util.concurrent.CompletionStage#acceptEither(java.util.concurrent.CompletionStage, java.util.function.Consumer)
     */
    @Override
    public ManagedCompletableFuture<Void> acceptEither(CompletionStage<? extends T> other, Consumer<? super T> action) {
        if (other instanceof ManagedCompletableFuture)
            other = ((ManagedCompletableFuture<? extends T>) other).completableFuture;

        // Reject ManagedTask so that we have the flexibility to decide later how to handle ManagedTaskListener and execution properties
        if (action instanceof ManagedTask)
            throw new IllegalArgumentException(ManagedTask.class.getName());

        WSContextService contextSvc = ((WSManagedExecutorService) defaultExecutor).getContextService();

        @SuppressWarnings("unchecked")
        ThreadContextDescriptor contextDescriptor = contextSvc.captureThreadContext(XPROPS_SUSPEND_TRAN);
        action = new ContextualConsumer<>(contextDescriptor, action);

        CompletableFuture<Void> dependentStage = completableFuture.acceptEither(other, action);
        return new ManagedCompletableFuture<Void>(dependentStage, defaultExecutor, null);
    }

    /**
     * @see java.util.concurrent.CompletionStage#acceptEitherAsync(java.util.concurrent.CompletionStage, java.util.function.Consumer)
     */
    @Override
    @Trivial
    public ManagedCompletableFuture<Void> acceptEitherAsync(CompletionStage<? extends T> other, Consumer<? super T> action) {
        return acceptEitherAsync(other, action, defaultExecutor);
    }

    /**
     * @see java.util.concurrent.CompletionStage#acceptEitherAsync(java.util.concurrent.CompletionStage, java.util.function.Consumer, java.util.concurrent.Executor)
     */
    @Override
    public ManagedCompletableFuture<Void> acceptEitherAsync(CompletionStage<? extends T> other, Consumer<? super T> action, Executor executor) {
        if (other instanceof ManagedCompletableFuture)
            other = ((ManagedCompletableFuture<? extends T>) other).completableFuture;

        CompletableFuture<Void> dependentStage;
        FutureRefExecutor futureExecutor;
        if (executor instanceof ManagedExecutorService) { // the only type of managed executor implementation allowed here is the built-in one
            // Reject ManagedTask so that we have the flexibility to decide later how to handle ManagedTaskListener and execution properties
            if (action instanceof ManagedTask)
                throw new IllegalArgumentException(ManagedTask.class.getName());

            WSManagedExecutorService managedExecutor = (WSManagedExecutorService) executor;
            WSContextService contextSvc = managedExecutor.getContextService();

            @SuppressWarnings("unchecked")
            ThreadContextDescriptor contextDescriptor = contextSvc.captureThreadContext(XPROPS_SUSPEND_TRAN);
            action = new ContextualConsumer<>(contextDescriptor, action);

            futureExecutor = new FutureRefExecutor(managedExecutor);
            dependentStage = completableFuture.acceptEitherAsync(other, action, futureExecutor);
        } else if (executor instanceof ExecutorService) {
            futureExecutor = new FutureRefExecutor((ExecutorService) executor);
            dependentStage = completableFuture.acceptEitherAsync(other, action, futureExecutor);
        } else {
            futureExecutor = null;
            dependentStage = completableFuture.acceptEitherAsync(other, action, executor);
        }
        return new ManagedCompletableFuture<Void>(dependentStage, defaultExecutor, futureExecutor);
    }

    /**
     * @see java.util.concurrent.CompletionStage#applyToEither(java.util.concurrent.CompletionStage, java.util.function.Function)
     */
    @Override
    public <U> ManagedCompletableFuture<U> applyToEither(CompletionStage<? extends T> other, Function<? super T, U> action) {
        if (other instanceof ManagedCompletableFuture)
            other = ((ManagedCompletableFuture<? extends T>) other).completableFuture;

        // Reject ManagedTask so that we have the flexibility to decide later how to handle ManagedTaskListener and execution properties
        if (action instanceof ManagedTask)
            throw new IllegalArgumentException(ManagedTask.class.getName());

        WSContextService contextSvc = ((WSManagedExecutorService) defaultExecutor).getContextService();

        @SuppressWarnings("unchecked")
        ThreadContextDescriptor contextDescriptor = contextSvc.captureThreadContext(XPROPS_SUSPEND_TRAN);
        action = new ContextualFunction<>(contextDescriptor, action);

        CompletableFuture<U> dependentStage = completableFuture.applyToEither(other, action);
        return new ManagedCompletableFuture<U>(dependentStage, defaultExecutor, null);
    }

    /**
     * @see java.util.concurrent.CompletionStage#applyToEitherAsync(java.util.concurrent.CompletionStage, java.util.function.Function)
     */
    @Override
    @Trivial
    public <U> ManagedCompletableFuture<U> applyToEitherAsync(CompletionStage<? extends T> other, Function<? super T, U> action) {
        return applyToEitherAsync(other, action, defaultExecutor);
    }

    /**
     * @see java.util.concurrent.CompletionStage#applyToEitherAsync(java.util.concurrent.CompletionStage, java.util.function.Function, java.util.concurrent.Executor)
     */
    @Override
    public <U> ManagedCompletableFuture<U> applyToEitherAsync(CompletionStage<? extends T> other, Function<? super T, U> action, Executor executor) {
        if (other instanceof ManagedCompletableFuture)
            other = ((ManagedCompletableFuture<? extends T>) other).completableFuture;

        CompletableFuture<U> dependentStage;
        FutureRefExecutor futureExecutor;
        if (executor instanceof ManagedExecutorService) { // the only type of managed executor implementation allowed here is the built-in one
            // Reject ManagedTask so that we have the flexibility to decide later how to handle ManagedTaskListener and execution properties
            if (action instanceof ManagedTask)
                throw new IllegalArgumentException(ManagedTask.class.getName());

            WSManagedExecutorService managedExecutor = (WSManagedExecutorService) executor;
            WSContextService contextSvc = managedExecutor.getContextService();

            @SuppressWarnings("unchecked")
            ThreadContextDescriptor contextDescriptor = contextSvc.captureThreadContext(XPROPS_SUSPEND_TRAN);
            action = new ContextualFunction<>(contextDescriptor, action);

            futureExecutor = new FutureRefExecutor(managedExecutor);
            dependentStage = completableFuture.applyToEitherAsync(other, action, futureExecutor);
        } else if (executor instanceof ExecutorService) {
            futureExecutor = new FutureRefExecutor((ExecutorService) executor);
            dependentStage = completableFuture.applyToEitherAsync(other, action, futureExecutor);
        } else {
            futureExecutor = null;
            dependentStage = completableFuture.applyToEitherAsync(other, action, executor);
        }
        return new ManagedCompletableFuture<U>(dependentStage, defaultExecutor, futureExecutor);
    }

    /**
     * @see java.util.concurrent.CompletableFuture#cancel(boolean)
     */
    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        boolean canceled = completableFuture.cancel(mayInterruptIfRunning);

        // If corresponding task has been submitted to an executor, attempt to cancel it
        if (canceled && futureRef != null) {
            Future<?> future = futureRef.get();
            if (future != null)
                future.cancel(mayInterruptIfRunning);
        }

        return canceled;
    }

    /**
     * @see java.util.concurrent.CompletableFuture#complete(T)
     */
    @Override
    public boolean complete(T value) {
        boolean completedByThisMethod = completableFuture.complete(value);

        // If corresponding task has been submitted to an executor, attempt to cancel it
        if (completedByThisMethod && futureRef != null) {
            Future<?> future = futureRef.get();
            if (future != null)
                future.cancel(true);
        }

        return completedByThisMethod;
    }

    /**
     * @see java.util.concurrent.CompletableFuture#completeExceptionally(java.lang.Throwable)
     */
    @Override
    public boolean completeExceptionally(Throwable x) {
        boolean completedByThisMethod = completableFuture.completeExceptionally(x);

        // If corresponding task has been submitted to an executor, attempt to cancel it
        if (completedByThisMethod && futureRef != null) {
            Future<?> future = futureRef.get();
            if (future != null)
                future.cancel(true);
        }

        return completedByThisMethod;
    }

    /**
     * @see java.util.concurrent.CompletionStage#exceptionally(java.util.function.Function)
     */
    @Override
    public ManagedCompletableFuture<T> exceptionally(Function<Throwable, ? extends T> action) {
        // Reject ManagedTask so that we have the flexibility to decide later how to handle ManagedTaskListener and execution properties
        if (action instanceof ManagedTask)
            throw new IllegalArgumentException(ManagedTask.class.getName());

        WSContextService contextSvc = ((WSManagedExecutorService) defaultExecutor).getContextService();

        @SuppressWarnings("unchecked")
        ThreadContextDescriptor contextDescriptor = contextSvc.captureThreadContext(XPROPS_SUSPEND_TRAN);
        action = new ContextualFunction<>(contextDescriptor, action);

        CompletableFuture<T> dependentStage = completableFuture.exceptionally(action);
        return new ManagedCompletableFuture<T>(dependentStage, defaultExecutor, null);
    }

    /**
     * @see java.util.concurrent.CompletableFuture#get()
     */
    @Override
    public T get() throws ExecutionException, InterruptedException {
        return completableFuture.get();
    }

    /**
     * @see java.util.concurrent.CompletableFuture#get(long, java.util.concurrent.TimeUnit)
     */
    @Override
    public T get(long timeout, TimeUnit unit) throws ExecutionException, InterruptedException, TimeoutException {
        return completableFuture.get(timeout, unit);
    }

    /**
     * @see java.util.concurrent.CompletableFuture#getNow(java.lang.Object)
     */
    @Override
    public T getNow(T valueIfAbsent) {
        return completableFuture.getNow(valueIfAbsent);
    }

    /**
     * @see java.util.concurrent.CompletableFuture#getNumberOfDependents()
     */
    @Override
    public int getNumberOfDependents() {
        // Subtract out the extra dependent stage that we use internally to be notified of completion.
        int count = completableFuture.getNumberOfDependents();
        return count > 1 ? count - 1 : 0;
    }

    /**
     * @see java.util.concurrent.CompletionStage#handle(java.util.function.BiFunction)
     */
    @Override
    public <R> ManagedCompletableFuture<R> handle(BiFunction<? super T, Throwable, ? extends R> action) {
        // Reject ManagedTask so that we have the flexibility to decide later how to handle ManagedTaskListener and execution properties
        if (action instanceof ManagedTask)
            throw new IllegalArgumentException(ManagedTask.class.getName());

        WSContextService contextSvc = ((WSManagedExecutorService) defaultExecutor).getContextService();

        @SuppressWarnings("unchecked")
        ThreadContextDescriptor contextDescriptor = contextSvc.captureThreadContext(XPROPS_SUSPEND_TRAN);
        action = new ContextualBiFunction<>(contextDescriptor, action);

        CompletableFuture<R> dependentStage = completableFuture.handle(action);
        return new ManagedCompletableFuture<R>(dependentStage, defaultExecutor, null);
    }

    /**
     * @see java.util.concurrent.CompletionStage#handleAsync(java.util.function.BiFunction)
     */
    @Override
    @Trivial
    public <R> ManagedCompletableFuture<R> handleAsync(BiFunction<? super T, Throwable, ? extends R> action) {
        return handleAsync(action, defaultExecutor);
    }

    /**
     * @see java.util.concurrent.CompletionStage#handleAsync(java.util.function.BiFunction, java.util.concurrent.Executor)
     */
    @Override
    public <R> ManagedCompletableFuture<R> handleAsync(BiFunction<? super T, Throwable, ? extends R> action, Executor executor) {
        CompletableFuture<R> dependentStage;
        FutureRefExecutor futureExecutor;
        if (executor instanceof ManagedExecutorService) { // the only type of managed executor implementation allowed here is the built-in one
            // Reject ManagedTask so that we have the flexibility to decide later how to handle ManagedTaskListener and execution properties
            if (action instanceof ManagedTask)
                throw new IllegalArgumentException(ManagedTask.class.getName());

            WSManagedExecutorService managedExecutor = (WSManagedExecutorService) executor;
            WSContextService contextSvc = managedExecutor.getContextService();

            @SuppressWarnings("unchecked")
            ThreadContextDescriptor contextDescriptor = contextSvc.captureThreadContext(XPROPS_SUSPEND_TRAN);
            action = new ContextualBiFunction<>(contextDescriptor, action);

            futureExecutor = new FutureRefExecutor(managedExecutor);
            dependentStage = completableFuture.handleAsync(action, futureExecutor);
        } else if (executor instanceof ExecutorService) {
            futureExecutor = new FutureRefExecutor((ExecutorService) executor);
            dependentStage = completableFuture.handleAsync(action, futureExecutor);
        } else {
            futureExecutor = null;
            dependentStage = completableFuture.handleAsync(action, executor);
        }
        return new ManagedCompletableFuture<R>(dependentStage, defaultExecutor, futureExecutor);
    }

    /**
     * @see java.util.concurrent.CompletableFuture#isCancelled()
     */
    @Override
    public boolean isCancelled() {
        return completableFuture.isCancelled();
    }

    /**
     * @see java.util.concurrent.CompletableFuture#isCompletedExceptionally()
     */
    @Override
    public boolean isCompletedExceptionally() {
        return completableFuture.isCompletedExceptionally();
    }

    /**
     * @see java.util.concurrent.CompletableFuture#isDone()
     */
    @Override
    public boolean isDone() {
        return completableFuture.isDone();
    }

    /**
     * @see java.util.concurrent.CompletableFuture#join()
     */
    @Override
    public T join() {
        return completableFuture.join();
    }

    /**
     * @see java.util.concurrent.CompletableFuture#obtrudeException(java.lang.Throwable)
     */
    @Override
    public void obtrudeException(Throwable x) {
        // disallow the possibility of concurrent obtrudes so as to keep the values consistent
        synchronized (completableFuture) {
            super.obtrudeException(x);
            completableFuture.obtrudeException(x);
        }

        // If corresponding task has been submitted to an executor, attempt to cancel it
        if (futureRef != null) {
            Future<?> future = futureRef.get();
            if (future != null)
                future.cancel(true);
        }
    }

    /**
     * @see java.util.concurrent.CompletableFuture#obtrudeValue(java.lang.Object)
     */
    @Override
    public void obtrudeValue(T value) {
        // disallow the possibility of concurrent obtrudes so as to keep the values consistent
        synchronized (completableFuture) {
            super.obtrudeValue(value);
            completableFuture.obtrudeValue(value);
        }

        // If corresponding task has been submitted to an executor, attempt to cancel it
        if (futureRef != null) {
            Future<?> future = futureRef.get();
            if (future != null)
                future.cancel(true);
        }
    }

    /**
     * @see java.util.concurrent.CompletionStage#runAfterBoth(java.util.concurrent.CompletionStage, java.lang.Runnable)
     */
    @Override
    public ManagedCompletableFuture<Void> runAfterBoth(CompletionStage<?> other, Runnable action) {
        if (other instanceof ManagedCompletableFuture)
            other = ((ManagedCompletableFuture<?>) other).completableFuture;

        // Reject ManagedTask so that we have the flexibility to decide later how to handle ManagedTaskListener and execution properties
        if (action instanceof ManagedTask)
            throw new IllegalArgumentException(ManagedTask.class.getName());

        WSContextService contextSvc = ((WSManagedExecutorService) defaultExecutor).getContextService();

        @SuppressWarnings("unchecked")
        ThreadContextDescriptor contextDescriptor = contextSvc.captureThreadContext(XPROPS_SUSPEND_TRAN);
        action = contextSvc.createContextualProxy(contextDescriptor, action, Runnable.class);

        CompletableFuture<Void> dependentStage = completableFuture.runAfterBoth(other, action);
        return new ManagedCompletableFuture<Void>(dependentStage, defaultExecutor, null);
    }

    /**
     * @see java.util.concurrent.CompletionStage#runAfterBothAsync(java.util.concurrent.CompletionStage, java.lang.Runnable)
     */
    @Override
    @Trivial
    public ManagedCompletableFuture<Void> runAfterBothAsync(CompletionStage<?> other, Runnable action) {
        return runAfterBothAsync(other, action, defaultExecutor);
    }

    /**
     * @see java.util.concurrent.CompletionStage#runAfterBothAsync(java.util.concurrent.CompletionStage, java.lang.Runnable, java.util.concurrent.Executor)
     */
    @Override
    public ManagedCompletableFuture<Void> runAfterBothAsync(CompletionStage<?> other, Runnable action, Executor executor) {
        if (other instanceof ManagedCompletableFuture)
            other = ((ManagedCompletableFuture<?>) other).completableFuture;

        CompletableFuture<Void> dependentStage;
        FutureRefExecutor futureExecutor;
        if (executor instanceof ManagedExecutorService) { // the only type of managed executor implementation allowed here is the built-in one
            // Reject ManagedTask so that we have the flexibility to decide later how to handle ManagedTaskListener and execution properties
            if (action instanceof ManagedTask)
                throw new IllegalArgumentException(ManagedTask.class.getName());

            WSManagedExecutorService managedExecutor = (WSManagedExecutorService) executor;
            WSContextService contextSvc = managedExecutor.getContextService();

            @SuppressWarnings("unchecked")
            ThreadContextDescriptor contextDescriptor = contextSvc.captureThreadContext(XPROPS_SUSPEND_TRAN);
            action = contextSvc.createContextualProxy(contextDescriptor, action, Runnable.class);

            futureExecutor = new FutureRefExecutor(managedExecutor);
            dependentStage = completableFuture.runAfterBothAsync(other, action, futureExecutor);
        } else if (executor instanceof ExecutorService) {
            futureExecutor = new FutureRefExecutor((ExecutorService) executor);
            dependentStage = completableFuture.runAfterBothAsync(other, action, futureExecutor);
        } else {
            futureExecutor = null;
            dependentStage = completableFuture.runAfterBothAsync(other, action, executor);
        }
        return new ManagedCompletableFuture<Void>(dependentStage, defaultExecutor, futureExecutor);
    }

    /**
     * @see java.util.concurrent.CompletionStage#runAfterEither(java.util.concurrent.CompletionStage, java.lang.Runnable)
     */
    @Override
    public ManagedCompletableFuture<Void> runAfterEither(CompletionStage<?> other, Runnable action) {
        if (other instanceof ManagedCompletableFuture)
            other = ((ManagedCompletableFuture<?>) other).completableFuture;

        // Reject ManagedTask so that we have the flexibility to decide later how to handle ManagedTaskListener and execution properties
        if (action instanceof ManagedTask)
            throw new IllegalArgumentException(ManagedTask.class.getName());

        WSContextService contextSvc = ((WSManagedExecutorService) defaultExecutor).getContextService();

        @SuppressWarnings("unchecked")
        ThreadContextDescriptor contextDescriptor = contextSvc.captureThreadContext(XPROPS_SUSPEND_TRAN);
        action = contextSvc.createContextualProxy(contextDescriptor, action, Runnable.class);

        CompletableFuture<Void> dependentStage = completableFuture.runAfterEither(other, action);
        return new ManagedCompletableFuture<Void>(dependentStage, defaultExecutor, null);
    }

    /**
     * @see java.util.concurrent.CompletionStage#runAfterEitherAsync(java.util.concurrent.CompletionStage, java.lang.Runnable)
     */
    @Override
    @Trivial
    public ManagedCompletableFuture<Void> runAfterEitherAsync(CompletionStage<?> other, Runnable action) {
        return runAfterEitherAsync(other, action, defaultExecutor);
    }

    /**
     * @see java.util.concurrent.CompletionStage#runAfterEitherAsync(java.util.concurrent.CompletionStage, java.lang.Runnable, java.util.concurrent.Executor)
     */
    @Override
    public ManagedCompletableFuture<Void> runAfterEitherAsync(CompletionStage<?> other, Runnable action, Executor executor) {
        if (other instanceof ManagedCompletableFuture)
            other = ((ManagedCompletableFuture<?>) other).completableFuture;

        CompletableFuture<Void> dependentStage;
        FutureRefExecutor futureExecutor;
        if (executor instanceof ManagedExecutorService) { // the only type of managed executor implementation allowed here is the built-in one
            // Reject ManagedTask so that we have the flexibility to decide later how to handle ManagedTaskListener and execution properties
            if (action instanceof ManagedTask)
                throw new IllegalArgumentException(ManagedTask.class.getName());

            WSManagedExecutorService managedExecutor = (WSManagedExecutorService) executor;
            WSContextService contextSvc = managedExecutor.getContextService();

            @SuppressWarnings("unchecked")
            ThreadContextDescriptor contextDescriptor = contextSvc.captureThreadContext(XPROPS_SUSPEND_TRAN);
            action = contextSvc.createContextualProxy(contextDescriptor, action, Runnable.class);

            futureExecutor = new FutureRefExecutor(managedExecutor);
            dependentStage = completableFuture.runAfterEitherAsync(other, action, futureExecutor);
        } else if (executor instanceof ExecutorService) {
            futureExecutor = new FutureRefExecutor((ExecutorService) executor);
            dependentStage = completableFuture.runAfterEitherAsync(other, action, futureExecutor);
        } else {
            futureExecutor = null;
            dependentStage = completableFuture.runAfterEitherAsync(other, action, executor);
        }
        return new ManagedCompletableFuture<Void>(dependentStage, defaultExecutor, futureExecutor);
    }

    /**
     * @see java.util.concurrent.CompletionStage#thenAccept(java.util.function.Consumer)
     */
    @Override
    public ManagedCompletableFuture<Void> thenAccept(Consumer<? super T> action) {
        // Reject ManagedTask so that we have the flexibility to decide later how to handle ManagedTaskListener and execution properties
        if (action instanceof ManagedTask)
            throw new IllegalArgumentException(ManagedTask.class.getName());

        WSContextService contextSvc = ((WSManagedExecutorService) defaultExecutor).getContextService();

        @SuppressWarnings("unchecked")
        ThreadContextDescriptor contextDescriptor = contextSvc.captureThreadContext(XPROPS_SUSPEND_TRAN);
        action = new ContextualConsumer<>(contextDescriptor, action);

        CompletableFuture<Void> dependentStage = completableFuture.thenAccept(action);
        return new ManagedCompletableFuture<Void>(dependentStage, defaultExecutor, null);
    }

    /**
     * @see java.util.concurrent.CompletionStage#thenAcceptAsync(java.util.function.Consumer)
     */
    @Override
    @Trivial
    public ManagedCompletableFuture<Void> thenAcceptAsync(Consumer<? super T> action) {
        return thenAcceptAsync(action, defaultExecutor);
    }

    /**
     * @see java.util.concurrent.CompletionStage#thenAcceptAsync(java.util.function.Consumer, java.util.concurrent.Executor)
     */
    @Override
    public ManagedCompletableFuture<Void> thenAcceptAsync(Consumer<? super T> action, Executor executor) {
        CompletableFuture<Void> dependentStage;
        FutureRefExecutor futureExecutor;
        if (executor instanceof ManagedExecutorService) { // the only type of managed executor implementation allowed here is the built-in one
            // Reject ManagedTask so that we have the flexibility to decide later how to handle ManagedTaskListener and execution properties
            if (action instanceof ManagedTask)
                throw new IllegalArgumentException(ManagedTask.class.getName());

            WSManagedExecutorService managedExecutor = (WSManagedExecutorService) executor;
            WSContextService contextSvc = managedExecutor.getContextService();

            @SuppressWarnings("unchecked")
            ThreadContextDescriptor contextDescriptor = contextSvc.captureThreadContext(XPROPS_SUSPEND_TRAN);
            action = new ContextualConsumer<>(contextDescriptor, action);

            futureExecutor = new FutureRefExecutor(managedExecutor);
            dependentStage = completableFuture.thenAcceptAsync(action, futureExecutor);
        } else if (executor instanceof ExecutorService) {
            futureExecutor = new FutureRefExecutor((ExecutorService) executor);
            dependentStage = completableFuture.thenAcceptAsync(action, futureExecutor);
        } else {
            futureExecutor = null;
            dependentStage = completableFuture.thenAcceptAsync(action, executor);
        }
        return new ManagedCompletableFuture<Void>(dependentStage, defaultExecutor, futureExecutor);
    }

    /**
     * @see java.util.concurrent.CompletionStage#thenAcceptBoth(java.util.concurrent.CompletionStage, java.util.function.BiConsumer)
     */
    @Override
    public <U> ManagedCompletableFuture<Void> thenAcceptBoth(CompletionStage<? extends U> other, BiConsumer<? super T, ? super U> action) {
        if (other instanceof ManagedCompletableFuture)
            other = ((ManagedCompletableFuture<? extends U>) other).completableFuture;

        // Reject ManagedTask so that we have the flexibility to decide later how to handle ManagedTaskListener and execution properties
        if (action instanceof ManagedTask)
            throw new IllegalArgumentException(ManagedTask.class.getName());

        WSContextService contextSvc = ((WSManagedExecutorService) defaultExecutor).getContextService();

        @SuppressWarnings("unchecked")
        ThreadContextDescriptor contextDescriptor = contextSvc.captureThreadContext(XPROPS_SUSPEND_TRAN);
        action = new ContextualBiConsumer<>(contextDescriptor, action);

        CompletableFuture<Void> dependentStage = completableFuture.thenAcceptBoth(other, action);
        return new ManagedCompletableFuture<Void>(dependentStage, defaultExecutor, null);
    }

    /**
     * @see java.util.concurrent.CompletionStage#thenAcceptBothAsync(java.util.concurrent.CompletionStage, java.util.function.BiConsumer)
     */
    @Override
    @Trivial
    public <U> ManagedCompletableFuture<Void> thenAcceptBothAsync(CompletionStage<? extends U> other, BiConsumer<? super T, ? super U> action) {
        return thenAcceptBothAsync(other, action, defaultExecutor);
    }

    /**
     * @see java.util.concurrent.CompletionStage#thenAcceptBothAsync(java.util.concurrent.CompletionStage, java.util.function.BiConsumer, java.util.concurrent.Executor)
     */
    @Override
    public <U> ManagedCompletableFuture<Void> thenAcceptBothAsync(CompletionStage<? extends U> other, BiConsumer<? super T, ? super U> action, Executor executor) {
        if (other instanceof ManagedCompletableFuture)
            other = ((ManagedCompletableFuture<? extends U>) other).completableFuture;

        CompletableFuture<Void> dependentStage;
        FutureRefExecutor futureExecutor;
        if (executor instanceof ManagedExecutorService) { // the only type of managed executor implementation allowed here is the built-in one
            // Reject ManagedTask so that we have the flexibility to decide later how to handle ManagedTaskListener and execution properties
            if (action instanceof ManagedTask)
                throw new IllegalArgumentException(ManagedTask.class.getName());

            WSManagedExecutorService managedExecutor = (WSManagedExecutorService) executor;
            WSContextService contextSvc = managedExecutor.getContextService();

            @SuppressWarnings("unchecked")
            ThreadContextDescriptor contextDescriptor = contextSvc.captureThreadContext(XPROPS_SUSPEND_TRAN);
            action = new ContextualBiConsumer<>(contextDescriptor, action);

            futureExecutor = new FutureRefExecutor(managedExecutor);
            dependentStage = completableFuture.thenAcceptBothAsync(other, action, futureExecutor);
        } else if (executor instanceof ExecutorService) {
            futureExecutor = new FutureRefExecutor((ExecutorService) executor);
            dependentStage = completableFuture.thenAcceptBothAsync(other, action, futureExecutor);
        } else {
            futureExecutor = null;
            dependentStage = completableFuture.thenAcceptBothAsync(other, action, executor);
        }
        return new ManagedCompletableFuture<Void>(dependentStage, defaultExecutor, futureExecutor);
    }

    /**
     * @see java.util.concurrent.CompletionStage#thenApply(java.util.function.Function)
     */
    @Override
    public <R> ManagedCompletableFuture<R> thenApply(Function<? super T, ? extends R> action) {
        // Reject ManagedTask so that we have the flexibility to decide later how to handle ManagedTaskListener and execution properties
        if (action instanceof ManagedTask)
            throw new IllegalArgumentException(ManagedTask.class.getName());

        WSContextService contextSvc = ((WSManagedExecutorService) defaultExecutor).getContextService();

        @SuppressWarnings("unchecked")
        ThreadContextDescriptor contextDescriptor = contextSvc.captureThreadContext(XPROPS_SUSPEND_TRAN);
        action = new ContextualFunction<>(contextDescriptor, action);

        CompletableFuture<R> dependentStage = completableFuture.thenApply(action);
        return new ManagedCompletableFuture<R>(dependentStage, defaultExecutor, null);
    }

    /**
     * @see java.util.concurrent.CompletionStage#thenApplyAsync(java.util.function.Function)
     */
    @Override
    @Trivial
    public <R> ManagedCompletableFuture<R> thenApplyAsync(Function<? super T, ? extends R> action) {
        return thenApplyAsync(action, defaultExecutor);
    }

    /**
     * @see java.util.concurrent.CompletionStage#thenApplyAsync(java.util.function.Function, java.util.concurrent.Executor)
     */
    @Override
    public <R> ManagedCompletableFuture<R> thenApplyAsync(Function<? super T, ? extends R> action, Executor executor) {
        CompletableFuture<R> dependentStage;
        FutureRefExecutor futureExecutor;
        if (executor instanceof ManagedExecutorService) { // the only type of managed executor implementation allowed here is the built-in one
            // Reject ManagedTask so that we have the flexibility to decide later how to handle ManagedTaskListener and execution properties
            if (action instanceof ManagedTask)
                throw new IllegalArgumentException(ManagedTask.class.getName());

            WSManagedExecutorService managedExecutor = (WSManagedExecutorService) executor;
            WSContextService contextSvc = managedExecutor.getContextService();

            @SuppressWarnings("unchecked")
            ThreadContextDescriptor contextDescriptor = contextSvc.captureThreadContext(XPROPS_SUSPEND_TRAN);
            action = new ContextualFunction<>(contextDescriptor, action);

            futureExecutor = new FutureRefExecutor(managedExecutor);
            dependentStage = completableFuture.thenApplyAsync(action, futureExecutor);
        } else if (executor instanceof ExecutorService) {
            futureExecutor = new FutureRefExecutor((ExecutorService) executor);
            dependentStage = completableFuture.thenApplyAsync(action, futureExecutor);
        } else {
            futureExecutor = null;
            dependentStage = completableFuture.thenApplyAsync(action, executor);
        }
        return new ManagedCompletableFuture<R>(dependentStage, defaultExecutor, futureExecutor);
    }

    /**
     * @see java.util.concurrent.CompletionStage#thenCombine(java.util.concurrent.CompletionStage, java.util.function.BiFunction)
     */
    @Override
    public <U, R> ManagedCompletableFuture<R> thenCombine(CompletionStage<? extends U> other, BiFunction<? super T, ? super U, ? extends R> action) {
        if (other instanceof ManagedCompletableFuture)
            other = ((ManagedCompletableFuture<? extends U>) other).completableFuture;

        // Reject ManagedTask so that we have the flexibility to decide later how to handle ManagedTaskListener and execution properties
        if (action instanceof ManagedTask)
            throw new IllegalArgumentException(ManagedTask.class.getName());

        WSContextService contextSvc = ((WSManagedExecutorService) defaultExecutor).getContextService();

        @SuppressWarnings("unchecked")
        ThreadContextDescriptor contextDescriptor = contextSvc.captureThreadContext(XPROPS_SUSPEND_TRAN);
        action = new ContextualBiFunction<>(contextDescriptor, action);

        CompletableFuture<R> dependentStage = completableFuture.thenCombine(other, action);
        return new ManagedCompletableFuture<R>(dependentStage, defaultExecutor, null);
    }

    /**
     * @see java.util.concurrent.CompletionStage#thenCombineAsync(java.util.concurrent.CompletionStage, java.util.function.BiFunction)
     */
    @Override
    @Trivial
    public <U, R> ManagedCompletableFuture<R> thenCombineAsync(CompletionStage<? extends U> other, BiFunction<? super T, ? super U, ? extends R> action) {
        return thenCombineAsync(other, action, defaultExecutor);
    }

    /**
     * @see java.util.concurrent.CompletionStage#thenCombineAsync(java.util.concurrent.CompletionStage, java.util.function.BiFunction, java.util.concurrent.Executor)
     */
    @Override
    public <U, R> ManagedCompletableFuture<R> thenCombineAsync(CompletionStage<? extends U> other, BiFunction<? super T, ? super U, ? extends R> action, Executor executor) {
        if (other instanceof ManagedCompletableFuture)
            other = ((ManagedCompletableFuture<? extends U>) other).completableFuture;

        CompletableFuture<R> dependentStage;
        FutureRefExecutor futureExecutor;
        if (executor instanceof ManagedExecutorService) { // the only type of managed executor implementation allowed here is the built-in one
            // Reject ManagedTask so that we have the flexibility to decide later how to handle ManagedTaskListener and execution properties
            if (action instanceof ManagedTask)
                throw new IllegalArgumentException(ManagedTask.class.getName());

            WSManagedExecutorService managedExecutor = (WSManagedExecutorService) executor;
            WSContextService contextSvc = managedExecutor.getContextService();

            @SuppressWarnings("unchecked")
            ThreadContextDescriptor contextDescriptor = contextSvc.captureThreadContext(XPROPS_SUSPEND_TRAN);
            action = new ContextualBiFunction<>(contextDescriptor, action);

            futureExecutor = new FutureRefExecutor(managedExecutor);
            dependentStage = completableFuture.thenCombineAsync(other, action, futureExecutor);
        } else if (executor instanceof ExecutorService) {
            futureExecutor = new FutureRefExecutor((ExecutorService) executor);
            dependentStage = completableFuture.thenCombineAsync(other, action, futureExecutor);
        } else {
            futureExecutor = null;
            dependentStage = completableFuture.thenCombineAsync(other, action, executor);
        }
        return new ManagedCompletableFuture<R>(dependentStage, defaultExecutor, futureExecutor);
    }

    /**
     * @see java.util.concurrent.CompletionStage#thenCompose(java.util.function.Function)
     */
    @Override
    public <U> ManagedCompletableFuture<U> thenCompose(Function<? super T, ? extends CompletionStage<U>> action) {
        // Reject ManagedTask so that we have the flexibility to decide later how to handle ManagedTaskListener and execution properties
        if (action instanceof ManagedTask)
            throw new IllegalArgumentException(ManagedTask.class.getName());

        WSContextService contextSvc = ((WSManagedExecutorService) defaultExecutor).getContextService();

        @SuppressWarnings("unchecked")
        ThreadContextDescriptor contextDescriptor = contextSvc.captureThreadContext(XPROPS_SUSPEND_TRAN);
        action = new ContextualFunction<>(contextDescriptor, action);

        CompletableFuture<U> dependentStage = completableFuture.thenCompose(action);
        return new ManagedCompletableFuture<U>(dependentStage, defaultExecutor, null);
    }

    /**
     * @see java.util.concurrent.CompletionStage#thenComposeAsync(java.util.function.Function)
     */
    @Override
    @Trivial
    public <U> ManagedCompletableFuture<U> thenComposeAsync(Function<? super T, ? extends CompletionStage<U>> action) {
        return thenComposeAsync(action, defaultExecutor);
    }

    /**
     * @see java.util.concurrent.CompletionStage#thenComposeAsync(java.util.function.Function, java.util.concurrent.Executor)
     */
    @Override
    public <U> ManagedCompletableFuture<U> thenComposeAsync(Function<? super T, ? extends CompletionStage<U>> action, Executor executor) {
        CompletableFuture<U> dependentStage;
        FutureRefExecutor futureExecutor;
        if (executor instanceof ManagedExecutorService) { // the only type of managed executor implementation allowed here is the built-in one
            // Reject ManagedTask so that we have the flexibility to decide later how to handle ManagedTaskListener and execution properties
            if (action instanceof ManagedTask)
                throw new IllegalArgumentException(ManagedTask.class.getName());

            WSManagedExecutorService managedExecutor = (WSManagedExecutorService) executor;
            WSContextService contextSvc = managedExecutor.getContextService();

            @SuppressWarnings("unchecked")
            ThreadContextDescriptor contextDescriptor = contextSvc.captureThreadContext(XPROPS_SUSPEND_TRAN);
            action = new ContextualFunction<>(contextDescriptor, action);

            futureExecutor = new FutureRefExecutor(managedExecutor);
            dependentStage = completableFuture.thenComposeAsync(action, futureExecutor);
        } else if (executor instanceof ExecutorService) {
            futureExecutor = new FutureRefExecutor((ExecutorService) executor);
            dependentStage = completableFuture.thenComposeAsync(action, futureExecutor);
        } else {
            futureExecutor = null;
            dependentStage = completableFuture.thenComposeAsync(action, executor);
        }
        return new ManagedCompletableFuture<U>(dependentStage, defaultExecutor, futureExecutor);
    }

    /**
     * @see java.util.concurrent.CompletionStage#thenRun(java.lang.Runnable)
     */
    @Override
    public ManagedCompletableFuture<Void> thenRun(Runnable action) {
        // Reject ManagedTask so that we have the flexibility to decide later how to handle ManagedTaskListener and execution properties
        if (action instanceof ManagedTask)
            throw new IllegalArgumentException(ManagedTask.class.getName());

        WSContextService contextSvc = ((WSManagedExecutorService) defaultExecutor).getContextService();

        @SuppressWarnings("unchecked")
        ThreadContextDescriptor contextDescriptor = contextSvc.captureThreadContext(XPROPS_SUSPEND_TRAN);
        action = contextSvc.createContextualProxy(contextDescriptor, action, Runnable.class);

        CompletableFuture<Void> dependentStage = completableFuture.thenRun(action);
        return new ManagedCompletableFuture<Void>(dependentStage, defaultExecutor, null);
    }

    /**
     * @see java.util.concurrent.CompletionStage#thenRunAsync(java.lang.Runnable)
     */
    @Override
    @Trivial
    public ManagedCompletableFuture<Void> thenRunAsync(Runnable action) {
        return thenRunAsync(action, defaultExecutor);
    }

    /**
     * @see java.util.concurrent.CompletionStage#thenRunAsync(java.lang.Runnable, java.util.concurrent.Executor)
     */
    @Override
    public ManagedCompletableFuture<Void> thenRunAsync(Runnable action, Executor executor) {
        CompletableFuture<Void> dependentStage;
        FutureRefExecutor futureExecutor;
        if (executor instanceof ManagedExecutorService) { // the only type of managed executor implementation allowed here is the built-in one
            // Reject ManagedTask so that we have the flexibility to decide later how to handle ManagedTaskListener and execution properties
            if (action instanceof ManagedTask)
                throw new IllegalArgumentException(ManagedTask.class.getName());

            WSManagedExecutorService managedExecutor = (WSManagedExecutorService) executor;
            WSContextService contextSvc = managedExecutor.getContextService();

            @SuppressWarnings("unchecked")
            ThreadContextDescriptor contextDescriptor = contextSvc.captureThreadContext(XPROPS_SUSPEND_TRAN);
            action = contextSvc.createContextualProxy(contextDescriptor, action, Runnable.class);

            futureExecutor = new FutureRefExecutor(managedExecutor);
            dependentStage = completableFuture.thenRunAsync(action, futureExecutor);
        } else if (executor instanceof ExecutorService) {
            futureExecutor = new FutureRefExecutor((ExecutorService) executor);
            dependentStage = completableFuture.thenRunAsync(action, futureExecutor);
        } else {
            futureExecutor = null;
            dependentStage = completableFuture.thenRunAsync(action, executor);
        }
        return new ManagedCompletableFuture<Void>(dependentStage, defaultExecutor, futureExecutor);
    }

    /**
     * @see java.util.concurrent.CompletableFuture#toCompletableFuture()
     */
    @Override
    public CompletableFuture<T> toCompletableFuture() {
        return this;
    }

    /**
     * Example output:
     * ManagedCompletableFuture@11111111[22222222 Not completed] via PolicyTaskFuture@33333333 for
     * java.util.concurrent.CompletableFuture$AsyncRun@44444444 RUNNING on concurrencyPolicy[defaultConcurrencyPolicy]
     *
     * @see java.util.concurrent.CompletableFuture#toString()
     */
    @Override
    @Trivial
    public String toString() {
        StringBuilder s = new StringBuilder(250).append("ManagedCompletableFuture@").append(Integer.toHexString(System.identityHashCode(this))) //
                        .append('[').append(Integer.toHexString(completableFuture.hashCode()));
        if (completableFuture.isDone())
            if (completableFuture.isCompletedExceptionally())
                s.append(" Completed exceptionally]");
            else
                s.append(" Completed normally]");
        else {
            s.append(" Not completed");
            // Subtract out the extra dependent stage that we use internally to be notified of completion.
            int d = completableFuture.getNumberOfDependents();
            if (d > 1)
                s.append(", ").append(d - 1).append(" dependents]");
            else
                s.append(']');
        }
        if (futureRef != null)
            s.append(" via ").append(futureRef.get());
        return s.toString();
    }

    /**
     * @see java.util.concurrent.CompletionStage#whenComplete(java.util.function.BiConsumer)
     */
    @Override
    public ManagedCompletableFuture<T> whenComplete(BiConsumer<? super T, ? super Throwable> action) {
        // Reject ManagedTask so that we have the flexibility to decide later how to handle ManagedTaskListener and execution properties
        if (action instanceof ManagedTask)
            throw new IllegalArgumentException(ManagedTask.class.getName());

        WSContextService contextSvc = ((WSManagedExecutorService) defaultExecutor).getContextService();

        @SuppressWarnings("unchecked")
        ThreadContextDescriptor contextDescriptor = contextSvc.captureThreadContext(XPROPS_SUSPEND_TRAN);
        action = new ContextualBiConsumer<>(contextDescriptor, action);

        CompletableFuture<T> dependentStage = completableFuture.whenComplete(action);
        return new ManagedCompletableFuture<T>(dependentStage, defaultExecutor, null);
    }

    /**
     * @see java.util.concurrent.CompletionStage#whenCompleteAsync(java.util.function.BiConsumer)
     */
    @Override
    @Trivial
    public ManagedCompletableFuture<T> whenCompleteAsync(BiConsumer<? super T, ? super Throwable> action) {
        return whenCompleteAsync(action, defaultExecutor);
    }

    /**
     * @see java.util.concurrent.CompletionStage#whenCompleteAsync(java.util.function.BiConsumer, java.util.concurrent.Executor)
     */
    @Override
    public ManagedCompletableFuture<T> whenCompleteAsync(BiConsumer<? super T, ? super Throwable> action, Executor executor) {
        CompletableFuture<T> dependentStage;
        FutureRefExecutor futureExecutor;
        if (executor instanceof ManagedExecutorService) { // the only type of managed executor implementation allowed here is the built-in one
            // Reject ManagedTask so that we have the flexibility to decide later how to handle ManagedTaskListener and execution properties
            if (action instanceof ManagedTask)
                throw new IllegalArgumentException(ManagedTask.class.getName());

            WSManagedExecutorService managedExecutor = (WSManagedExecutorService) executor;
            WSContextService contextSvc = managedExecutor.getContextService();

            @SuppressWarnings("unchecked")
            ThreadContextDescriptor contextDescriptor = contextSvc.captureThreadContext(XPROPS_SUSPEND_TRAN);
            action = new ContextualBiConsumer<>(contextDescriptor, action);

            futureExecutor = new FutureRefExecutor(managedExecutor);
            dependentStage = completableFuture.whenCompleteAsync(action, futureExecutor);
        } else if (executor instanceof ExecutorService) {
            futureExecutor = new FutureRefExecutor((ExecutorService) executor);
            dependentStage = completableFuture.whenCompleteAsync(action, futureExecutor);
        } else {
            futureExecutor = null;
            dependentStage = completableFuture.whenCompleteAsync(action, executor);
        }
        return new ManagedCompletableFuture<T>(dependentStage, defaultExecutor, futureExecutor);
    }

    // TODO move the following classes to the general context service in com.ibm.ws.context project once Java 8 can be used there

    /**
     * Proxy for BiConsumer that applies thread context before running and removes it afterward
     *
     * @param <T> type of the function's parameter
     * @param <R> type of the function's result
     */
    private static class ContextualBiConsumer<T, U> implements BiConsumer<T, U> {
        private final BiConsumer<T, U> action;
        private final ThreadContextDescriptor threadContextDescriptor;

        private ContextualBiConsumer(ThreadContextDescriptor threadContextDescriptor, BiConsumer<T, U> action) {
            this.action = action;
            this.threadContextDescriptor = threadContextDescriptor;
        }

        @Override
        public void accept(T t, U u) {
            ArrayList<ThreadContext> contextApplied = threadContextDescriptor.taskStarting();
            try {
                action.accept(t, u);
            } finally {
                threadContextDescriptor.taskStopping(contextApplied);
            }
        }
    }

    /**
     * Proxy for Consumer that applies thread context before running and removes it afterward
     *
     * @param <T> type of the consumer's parameter
     */
    private static class ContextualConsumer<T> implements Consumer<T> {
        private final Consumer<T> action;
        private final ThreadContextDescriptor threadContextDescriptor;

        private ContextualConsumer(ThreadContextDescriptor threadContextDescriptor, Consumer<T> action) {
            this.action = action;
            this.threadContextDescriptor = threadContextDescriptor;
        }

        @Override
        public void accept(T t) {
            ArrayList<ThreadContext> contextApplied = threadContextDescriptor.taskStarting();
            try {
                action.accept(t);
            } finally {
                threadContextDescriptor.taskStopping(contextApplied);
            }
        }
    }

    /**
     * Proxy for BiFunction that applies thread context before running and removes it afterward
     *
     * @param <T> type of the function's first parameter
     * @param <U> type of the function's second parameter
     * @param <R> type of the function's result
     */
    private static class ContextualBiFunction<T, U, R> implements BiFunction<T, U, R> {
        private final BiFunction<T, U, R> action;
        private final ThreadContextDescriptor threadContextDescriptor;

        private ContextualBiFunction(ThreadContextDescriptor threadContextDescriptor, BiFunction<T, U, R> action) {
            this.action = action;
            this.threadContextDescriptor = threadContextDescriptor;
        }

        @Override
        public R apply(T t, U u) {
            ArrayList<ThreadContext> contextApplied = threadContextDescriptor.taskStarting();
            try {
                return action.apply(t, u);
            } finally {
                threadContextDescriptor.taskStopping(contextApplied);
            }
        }
    }

    /**
     * Proxy for Function that applies thread context before running and removes it afterward
     *
     * @param <T> type of the function's parameter
     * @param <R> type of the function's result
     */
    private static class ContextualFunction<T, R> implements Function<T, R> {
        private final Function<T, R> action;
        private final ThreadContextDescriptor threadContextDescriptor;

        private ContextualFunction(ThreadContextDescriptor threadContextDescriptor, Function<T, R> action) {
            this.action = action;
            this.threadContextDescriptor = threadContextDescriptor;
        }

        @Override
        public R apply(T t) {
            ArrayList<ThreadContext> contextApplied = threadContextDescriptor.taskStarting();
            try {
                return action.apply(t);
            } finally {
                threadContextDescriptor.taskStopping(contextApplied);
            }
        }
    }

    /**
     * Proxy for Supplier that applies thread context before running and removes it afterward
     *
     * @param <T> type of the result that is supplied by the supplier
     */
    private static class ContextualSupplier<T> implements Supplier<T> {
        private final Supplier<T> action;
        private final ThreadContextDescriptor threadContextDescriptor;

        private ContextualSupplier(ThreadContextDescriptor threadContextDescriptor, Supplier<T> action) {
            this.action = action;
            this.threadContextDescriptor = threadContextDescriptor;
        }

        @Override
        public T get() {
            ArrayList<ThreadContext> contextApplied = threadContextDescriptor.taskStarting();
            try {
                return action.get();
            } finally {
                threadContextDescriptor.taskStopping(contextApplied);
            }
        }
    }
}