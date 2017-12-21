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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
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
import com.ibm.ws.concurrent.WSManagedExecutorService;
import com.ibm.ws.threading.PolicyExecutor;
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
 * TODO At this point, this class is just a skeleton and remains to be implemented. Methods are either rejected,
 * or provide no value above delegation back to the CompletableFuture. At first, we are just trying out
 * whether the approach is even a valid way of integrating with CompletableFuture.
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
     */
    private final WSManagedExecutorService defaultExecutor;

    /**
     * Construct a completable future with a managed executor as its default asynchronous execution facility.
     *
     * @param executor managed executor service
     * @param parent
     */
    private ManagedCompletableFuture(CompletableFuture<T> completableFuture, WSManagedExecutorService managedExecutor) {
        super();

        // For the sake of operations that rely upon CompletableFuture internals, update the state of the super class upon completion:
        completableFuture.whenComplete((result, failure) -> {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(this, tc, "whenComplete", result, failure);
            if (failure == null)
                super.complete(result);
            else
                super.completeExceptionally(failure);
        });

        this.completableFuture = completableFuture;
        this.defaultExecutor = managedExecutor;
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
                        (WSManagedExecutorService) AccessController.doPrivileged(getDefaultManagedExecutorAction));
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
     * @return completable future where the specified managed executor is the default asynchronous execution facility.
     */
    public static ManagedCompletableFuture<Void> runAsync(Runnable action, Executor executor) {
        // Reject ManagedTask so that we have the flexibility to decide later how to handle ManagedTaskListener and execution properties
        if (action instanceof ManagedTask)
            throw new IllegalArgumentException(ManagedTask.class.getName());

        WSManagedExecutorService managedExecutor = (WSManagedExecutorService) executor;
        PolicyExecutor policyExecutor = managedExecutor.getNormalPolicyExecutor(); // TODO choose based on LONGRUNNING_HINT execution property
        WSContextService contextSvc = managedExecutor.getContextService();

        @SuppressWarnings("unchecked")
        ThreadContextDescriptor contextDescriptor = contextSvc.captureThreadContext(XPROPS_SUSPEND_TRAN);
        action = contextSvc.createContextualProxy(contextDescriptor, action, Runnable.class);

        CompletableFuture<Void> completableFuture = CompletableFuture.runAsync(action, policyExecutor);
        return new ManagedCompletableFuture<Void>(completableFuture, managedExecutor);
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
     * @return completable future where the specified managed executor is the default asynchronous execution facility.
     */
    public static <U> ManagedCompletableFuture<U> supplyAsync(Supplier<U> action, Executor executor) {
        // Reject ManagedTask so that we have the flexibility to decide later how to handle ManagedTaskListener and execution properties
        if (action instanceof ManagedTask)
            throw new IllegalArgumentException(ManagedTask.class.getName());

        WSManagedExecutorService managedExecutor = (WSManagedExecutorService) executor;
        PolicyExecutor policyExecutor = managedExecutor.getNormalPolicyExecutor(); // TODO choose based on LONGRUNNING_HINT execution property
        WSContextService contextSvc = managedExecutor.getContextService();

        @SuppressWarnings("unchecked")
        ThreadContextDescriptor contextDescriptor = contextSvc.captureThreadContext(XPROPS_SUSPEND_TRAN);
        action = new ContextualSupplier<U>(contextDescriptor, action);

        CompletableFuture<U> completableFuture = CompletableFuture.supplyAsync(action, policyExecutor);
        return new ManagedCompletableFuture<U>(completableFuture, managedExecutor);
    }

    // Overrides of CompletableFuture methods

    /**
     * @see java.util.concurrent.CompletionStage#acceptEither(java.util.concurrent.CompletionStage, java.util.function.Consumer)
     */
    @Override
    public ManagedCompletableFuture<Void> acceptEither(CompletionStage<? extends T> other, Consumer<? super T> action) {
        throw new UnsupportedOperationException();
    }

    /**
     * @see java.util.concurrent.CompletionStage#acceptEitherAsync(java.util.concurrent.CompletionStage, java.util.function.Consumer)
     */
    @Override
    public ManagedCompletableFuture<Void> acceptEitherAsync(CompletionStage<? extends T> other, Consumer<? super T> action) {
        throw new UnsupportedOperationException();
    }

    /**
     * @see java.util.concurrent.CompletionStage#acceptEitherAsync(java.util.concurrent.CompletionStage, java.util.function.Consumer, java.util.concurrent.Executor)
     */
    @Override
    public ManagedCompletableFuture<Void> acceptEitherAsync(CompletionStage<? extends T> other, Consumer<? super T> action, Executor executor) {
        throw new UnsupportedOperationException();
    }

    /**
     * @see java.util.concurrent.CompletionStage#applyToEither(java.util.concurrent.CompletionStage, java.util.function.Function)
     */
    @Override
    public <U> ManagedCompletableFuture<U> applyToEither(CompletionStage<? extends T> other, Function<? super T, U> fn) {
        throw new UnsupportedOperationException();
    }

    /**
     * @see java.util.concurrent.CompletionStage#applyToEitherAsync(java.util.concurrent.CompletionStage, java.util.function.Function)
     */
    @Override
    public <U> ManagedCompletableFuture<U> applyToEitherAsync(CompletionStage<? extends T> other, Function<? super T, U> fn) {
        throw new UnsupportedOperationException();
    }

    /**
     * @see java.util.concurrent.CompletionStage#applyToEitherAsync(java.util.concurrent.CompletionStage, java.util.function.Function, java.util.concurrent.Executor)
     */
    @Override
    public <U> ManagedCompletableFuture<U> applyToEitherAsync(CompletionStage<? extends T> other, Function<? super T, U> fn, Executor executor) {
        throw new UnsupportedOperationException();
    }

    /**
     * @see java.util.concurrent.CompletableFuture#cancel(boolean)
     */
    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        // TODO also cancel from managed executor
        return completableFuture.cancel(mayInterruptIfRunning);
    }

    /**
     * @see java.util.concurrent.CompletableFuture#complete(T)
     */
    @Override
    public boolean complete(T value) {
        return completableFuture.complete(value);
    }

    /**
     * @see java.util.concurrent.CompletableFuture#completeExceptionally(java.lang.Throwable)
     */
    @Override
    public boolean completeExceptionally(Throwable x) {
        // TODO also cancel from managed executor if completed with a CancellationException
        return completableFuture.completeExceptionally(x);
    }

    /**
     * @see java.util.concurrent.CompletionStage#exceptionally(java.util.function.Function)
     */
    @Override
    public ManagedCompletableFuture<T> exceptionally(Function<Throwable, ? extends T> fn) {
        // TODO contextualize fn and other steps
        return new ManagedCompletableFuture<T>(completableFuture.exceptionally(fn), defaultExecutor);
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
    public <U> ManagedCompletableFuture<U> handle(BiFunction<? super T, Throwable, ? extends U> fn) {
        // TODO contextualize fn and other steps
        return new ManagedCompletableFuture<U>(completableFuture.handle(fn), defaultExecutor);
    }

    /**
     * @see java.util.concurrent.CompletionStage#handleAsync(java.util.function.BiFunction)
     */
    @Override
    public <U> ManagedCompletableFuture<U> handleAsync(BiFunction<? super T, Throwable, ? extends U> fn) {
        // TODO contextualize fn and other steps
        Executor policyExecutor = (Executor) defaultExecutor; // TODO get policy executor from default executor
        return new ManagedCompletableFuture<U>(completableFuture.handleAsync(fn, policyExecutor), defaultExecutor);
    }

    /**
     * @see java.util.concurrent.CompletionStage#handleAsync(java.util.function.BiFunction, java.util.concurrent.Executor)
     */
    @Override
    public <U> ManagedCompletableFuture<U> handleAsync(BiFunction<? super T, Throwable, ? extends U> fn, Executor executor) {
        // TODO contextualize fn and other steps
        Executor policyExecutor = executor; // TODO get policy executor from the supplied executor, if a managed executor
        return new ManagedCompletableFuture<U>(completableFuture.handleAsync(fn, policyExecutor), defaultExecutor);
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
     * @see java.util.concurrent.CompletableFuture#obtrudeValue(java.lang.Object)
     */
    @Override
    public void obtrudeValue(T value) {
        completableFuture.obtrudeValue(value);
    }

    /**
     * @see java.util.concurrent.CompletableFuture#obtrudeException(java.lang.Throwable)
     */
    @Override
    public void obtrudeException(Throwable x) {
        completableFuture.obtrudeException(x);
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

        WSContextService contextSvc = defaultExecutor.getContextService();

        @SuppressWarnings("unchecked")
        ThreadContextDescriptor contextDescriptor = contextSvc.captureThreadContext(XPROPS_SUSPEND_TRAN);
        action = contextSvc.createContextualProxy(contextDescriptor, action, Runnable.class);

        CompletableFuture<Void> dependentStage = completableFuture.runAfterBoth(other, action);
        return new ManagedCompletableFuture<Void>(dependentStage, defaultExecutor);
    }

    /**
     * @see java.util.concurrent.CompletionStage#runAfterBothAsync(java.util.concurrent.CompletionStage, java.lang.Runnable)
     */
    @Override
    public ManagedCompletableFuture<Void> runAfterBothAsync(CompletionStage<?> other, Runnable action) {
        if (other instanceof ManagedCompletableFuture)
            other = ((ManagedCompletableFuture<?>) other).completableFuture;

        // Reject ManagedTask so that we have the flexibility to decide later how to handle ManagedTaskListener and execution properties
        if (action instanceof ManagedTask)
            throw new IllegalArgumentException(ManagedTask.class.getName());

        PolicyExecutor policyExecutor = defaultExecutor.getNormalPolicyExecutor(); // TODO choose based on LONGRUNNING_HINT execution property
        WSContextService contextSvc = defaultExecutor.getContextService();

        @SuppressWarnings("unchecked")
        ThreadContextDescriptor contextDescriptor = contextSvc.captureThreadContext(XPROPS_SUSPEND_TRAN);
        action = contextSvc.createContextualProxy(contextDescriptor, action, Runnable.class);

        CompletableFuture<Void> dependentStage = completableFuture.runAfterBothAsync(other, action, policyExecutor);
        return new ManagedCompletableFuture<Void>(dependentStage, defaultExecutor);
    }

    /**
     * @see java.util.concurrent.CompletionStage#runAfterBothAsync(java.util.concurrent.CompletionStage, java.lang.Runnable, java.util.concurrent.Executor)
     */
    @Override
    public ManagedCompletableFuture<Void> runAfterBothAsync(CompletionStage<?> other, Runnable action, Executor executor) {
        if (other instanceof ManagedCompletableFuture)
            other = ((ManagedCompletableFuture<?>) other).completableFuture;

        CompletableFuture<Void> dependentStage;
        if (executor instanceof ManagedExecutorService) { // the only type of managed executor implementation allowed here is the built-in one
            // Reject ManagedTask so that we have the flexibility to decide later how to handle ManagedTaskListener and execution properties
            if (action instanceof ManagedTask)
                throw new IllegalArgumentException(ManagedTask.class.getName());

            WSManagedExecutorService managedExecutor = (WSManagedExecutorService) executor;
            PolicyExecutor policyExecutor = managedExecutor.getNormalPolicyExecutor(); // TODO choose based on LONGRUNNING_HINT execution property
            WSContextService contextSvc = managedExecutor.getContextService();

            @SuppressWarnings("unchecked")
            ThreadContextDescriptor contextDescriptor = contextSvc.captureThreadContext(XPROPS_SUSPEND_TRAN);
            action = contextSvc.createContextualProxy(contextDescriptor, action, Runnable.class);

            dependentStage = completableFuture.runAfterBothAsync(other, action, policyExecutor);
        } else {
            dependentStage = completableFuture.runAfterBothAsync(other, action, executor);
        }
        return new ManagedCompletableFuture<Void>(dependentStage, defaultExecutor);
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

        WSContextService contextSvc = defaultExecutor.getContextService();

        @SuppressWarnings("unchecked")
        ThreadContextDescriptor contextDescriptor = contextSvc.captureThreadContext(XPROPS_SUSPEND_TRAN);
        action = contextSvc.createContextualProxy(contextDescriptor, action, Runnable.class);

        CompletableFuture<Void> dependentStage = completableFuture.runAfterEither(other, action);
        return new ManagedCompletableFuture<Void>(dependentStage, defaultExecutor);
    }

    /**
     * @see java.util.concurrent.CompletionStage#runAfterEitherAsync(java.util.concurrent.CompletionStage, java.lang.Runnable)
     */
    @Override
    public ManagedCompletableFuture<Void> runAfterEitherAsync(CompletionStage<?> other, Runnable action) {
        if (other instanceof ManagedCompletableFuture)
            other = ((ManagedCompletableFuture<?>) other).completableFuture;

        // Reject ManagedTask so that we have the flexibility to decide later how to handle ManagedTaskListener and execution properties
        if (action instanceof ManagedTask)
            throw new IllegalArgumentException(ManagedTask.class.getName());

        PolicyExecutor policyExecutor = defaultExecutor.getNormalPolicyExecutor(); // TODO choose based on LONGRUNNING_HINT execution property
        WSContextService contextSvc = defaultExecutor.getContextService();

        @SuppressWarnings("unchecked")
        ThreadContextDescriptor contextDescriptor = contextSvc.captureThreadContext(XPROPS_SUSPEND_TRAN);
        action = contextSvc.createContextualProxy(contextDescriptor, action, Runnable.class);

        CompletableFuture<Void> dependentStage = completableFuture.runAfterEitherAsync(other, action, policyExecutor);
        return new ManagedCompletableFuture<Void>(dependentStage, defaultExecutor);
    }

    /**
     * @see java.util.concurrent.CompletionStage#runAfterEitherAsync(java.util.concurrent.CompletionStage, java.lang.Runnable, java.util.concurrent.Executor)
     */
    @Override
    public ManagedCompletableFuture<Void> runAfterEitherAsync(CompletionStage<?> other, Runnable action, Executor executor) {
        if (other instanceof ManagedCompletableFuture)
            other = ((ManagedCompletableFuture<?>) other).completableFuture;

        CompletableFuture<Void> dependentStage;
        if (executor instanceof ManagedExecutorService) { // the only type of managed executor implementation allowed here is the built-in one
            // Reject ManagedTask so that we have the flexibility to decide later how to handle ManagedTaskListener and execution properties
            if (action instanceof ManagedTask)
                throw new IllegalArgumentException(ManagedTask.class.getName());

            WSManagedExecutorService managedExecutor = (WSManagedExecutorService) executor;
            PolicyExecutor policyExecutor = managedExecutor.getNormalPolicyExecutor(); // TODO choose based on LONGRUNNING_HINT execution property
            WSContextService contextSvc = managedExecutor.getContextService();

            @SuppressWarnings("unchecked")
            ThreadContextDescriptor contextDescriptor = contextSvc.captureThreadContext(XPROPS_SUSPEND_TRAN);
            action = contextSvc.createContextualProxy(contextDescriptor, action, Runnable.class);

            dependentStage = completableFuture.runAfterEitherAsync(other, action, policyExecutor);
        } else {
            dependentStage = completableFuture.runAfterEitherAsync(other, action, executor);
        }
        return new ManagedCompletableFuture<Void>(dependentStage, defaultExecutor);
    }

    /**
     * @see java.util.concurrent.CompletionStage#thenAccept(java.util.function.Consumer)
     */
    @Override
    public ManagedCompletableFuture<Void> thenAccept(Consumer<? super T> action) {
        // TODO contextualize action and other steps
        return new ManagedCompletableFuture<Void>(completableFuture.thenAccept(action), defaultExecutor);
    }

    /**
     * @see java.util.concurrent.CompletionStage#thenAcceptAsync(java.util.function.Consumer)
     */
    @Override
    public ManagedCompletableFuture<Void> thenAcceptAsync(Consumer<? super T> action) {
        // TODO contextualize action and other steps
        Executor policyExecutor = (Executor) defaultExecutor; // TODO get policy executor from default executor
        return new ManagedCompletableFuture<Void>(completableFuture.thenAcceptAsync(action, policyExecutor), defaultExecutor);
    }

    /**
     * @see java.util.concurrent.CompletionStage#thenAcceptAsync(java.util.function.Consumer, java.util.concurrent.Executor)
     */
    @Override
    public ManagedCompletableFuture<Void> thenAcceptAsync(Consumer<? super T> action, Executor executor) {
        // TODO contextualize action and other steps
        Executor policyExecutor = executor; // TODO get policy executor from the supplied executor, if a managed executor
        return new ManagedCompletableFuture<Void>(completableFuture.thenAcceptAsync(action, policyExecutor), defaultExecutor);
    }

    /**
     * @see java.util.concurrent.CompletionStage#thenAcceptBoth(java.util.concurrent.CompletionStage, java.util.function.BiConsumer)
     */
    @Override
    public <U> ManagedCompletableFuture<Void> thenAcceptBoth(CompletionStage<? extends U> other, BiConsumer<? super T, ? super U> action) {
        throw new UnsupportedOperationException();
    }

    /**
     * @see java.util.concurrent.CompletionStage#thenAcceptBothAsync(java.util.concurrent.CompletionStage, java.util.function.BiConsumer)
     */
    @Override
    public <U> ManagedCompletableFuture<Void> thenAcceptBothAsync(CompletionStage<? extends U> other, BiConsumer<? super T, ? super U> action) {
        throw new UnsupportedOperationException();
    }

    /**
     * @see java.util.concurrent.CompletionStage#thenAcceptBothAsync(java.util.concurrent.CompletionStage, java.util.function.BiConsumer, java.util.concurrent.Executor)
     */
    @Override
    public <U> ManagedCompletableFuture<Void> thenAcceptBothAsync(CompletionStage<? extends U> other, BiConsumer<? super T, ? super U> action, Executor executor) {
        throw new UnsupportedOperationException();
    }

    /**
     * @see java.util.concurrent.CompletionStage#thenApply(java.util.function.Function)
     */
    @Override
    @SuppressWarnings("unchecked")
    public <R> ManagedCompletableFuture<R> thenApply(Function<? super T, ? extends R> action) {
        // Reject ManagedTask so that we have the flexibility to decide later how to handle ManagedTaskListener and execution properties
        if (action instanceof ManagedTask)
            throw new IllegalArgumentException(ManagedTask.class.getName());

        WSContextService contextSvc = defaultExecutor.getContextService();

        ThreadContextDescriptor contextDescriptor = contextSvc.captureThreadContext(XPROPS_SUSPEND_TRAN);
        action = contextSvc.createContextualProxy(contextDescriptor, action, Function.class);

        CompletableFuture<R> dependentStage = completableFuture.thenApply(action);
        return new ManagedCompletableFuture<R>(dependentStage, defaultExecutor);
    }

    /**
     * @see java.util.concurrent.CompletionStage#thenApplyAsync(java.util.function.Function)
     */
    @Override
    @SuppressWarnings("unchecked")
    public <R> ManagedCompletableFuture<R> thenApplyAsync(Function<? super T, ? extends R> action) {
        // Reject ManagedTask so that we have the flexibility to decide later how to handle ManagedTaskListener and execution properties
        if (action instanceof ManagedTask)
            throw new IllegalArgumentException(ManagedTask.class.getName());

        PolicyExecutor policyExecutor = defaultExecutor.getNormalPolicyExecutor(); // TODO choose based on LONGRUNNING_HINT execution property
        WSContextService contextSvc = defaultExecutor.getContextService();

        ThreadContextDescriptor contextDescriptor = contextSvc.captureThreadContext(XPROPS_SUSPEND_TRAN);
        action = contextSvc.createContextualProxy(contextDescriptor, action, Function.class);

        CompletableFuture<R> dependentStage = completableFuture.thenApplyAsync(action, policyExecutor);
        return new ManagedCompletableFuture<R>(dependentStage, defaultExecutor);
    }

    /**
     * @see java.util.concurrent.CompletionStage#thenApplyAsync(java.util.function.Function, java.util.concurrent.Executor)
     */
    @Override
    @SuppressWarnings("unchecked")
    public <R> ManagedCompletableFuture<R> thenApplyAsync(Function<? super T, ? extends R> action, Executor executor) {
        CompletableFuture<R> dependentStage;
        if (executor instanceof ManagedExecutorService) { // the only type of managed executor implementation allowed here is the built-in one
            // Reject ManagedTask so that we have the flexibility to decide later how to handle ManagedTaskListener and execution properties
            if (action instanceof ManagedTask)
                throw new IllegalArgumentException(ManagedTask.class.getName());

            WSManagedExecutorService managedExecutor = (WSManagedExecutorService) executor;
            PolicyExecutor policyExecutor = managedExecutor.getNormalPolicyExecutor(); // TODO choose based on LONGRUNNING_HINT execution property
            WSContextService contextSvc = managedExecutor.getContextService();

            ThreadContextDescriptor contextDescriptor = contextSvc.captureThreadContext(XPROPS_SUSPEND_TRAN);
            action = contextSvc.createContextualProxy(contextDescriptor, action, Function.class);

            dependentStage = completableFuture.thenApplyAsync(action, policyExecutor);
        } else {
            dependentStage = completableFuture.thenApplyAsync(action, executor);
        }
        return new ManagedCompletableFuture<R>(dependentStage, defaultExecutor);
    }

    /**
     * @see java.util.concurrent.CompletionStage#thenCombine(java.util.concurrent.CompletionStage, java.util.function.BiFunction)
     */
    @Override
    public <U, V> ManagedCompletableFuture<V> thenCombine(CompletionStage<? extends U> other, BiFunction<? super T, ? super U, ? extends V> fn) {
        throw new UnsupportedOperationException();
    }

    /**
     * @see java.util.concurrent.CompletionStage#thenCombineAsync(java.util.concurrent.CompletionStage, java.util.function.BiFunction)
     */
    @Override
    public <U, V> ManagedCompletableFuture<V> thenCombineAsync(CompletionStage<? extends U> other, BiFunction<? super T, ? super U, ? extends V> fn) {
        throw new UnsupportedOperationException();
    }

    /**
     * @see java.util.concurrent.CompletionStage#thenCombineAsync(java.util.concurrent.CompletionStage, java.util.function.BiFunction, java.util.concurrent.Executor)
     */
    @Override
    public <U, V> ManagedCompletableFuture<V> thenCombineAsync(CompletionStage<? extends U> other, BiFunction<? super T, ? super U, ? extends V> fn, Executor executor) {
        throw new UnsupportedOperationException();
    }

    /**
     * @see java.util.concurrent.CompletionStage#thenCompose(java.util.function.Function)
     */
    @Override
    public <U> ManagedCompletableFuture<U> thenCompose(Function<? super T, ? extends CompletionStage<U>> fn) {
        throw new UnsupportedOperationException();
    }

    /**
     * @see java.util.concurrent.CompletionStage#thenComposeAsync(java.util.function.Function)
     */
    @Override
    public <U> ManagedCompletableFuture<U> thenComposeAsync(Function<? super T, ? extends CompletionStage<U>> fn) {
        throw new UnsupportedOperationException();
    }

    /**
     * @see java.util.concurrent.CompletionStage#thenComposeAsync(java.util.function.Function, java.util.concurrent.Executor)
     */
    @Override
    public <U> ManagedCompletableFuture<U> thenComposeAsync(Function<? super T, ? extends CompletionStage<U>> fn, Executor executor) {
        throw new UnsupportedOperationException();
    }

    /**
     * @see java.util.concurrent.CompletionStage#thenRun(java.lang.Runnable)
     */
    @Override
    public ManagedCompletableFuture<Void> thenRun(Runnable action) {
        // Reject ManagedTask so that we have the flexibility to decide later how to handle ManagedTaskListener and execution properties
        if (action instanceof ManagedTask)
            throw new IllegalArgumentException(ManagedTask.class.getName());

        WSContextService contextSvc = defaultExecutor.getContextService();

        @SuppressWarnings("unchecked")
        ThreadContextDescriptor contextDescriptor = contextSvc.captureThreadContext(XPROPS_SUSPEND_TRAN);
        action = contextSvc.createContextualProxy(contextDescriptor, action, Runnable.class);

        CompletableFuture<Void> dependentStage = completableFuture.thenRun(action);
        return new ManagedCompletableFuture<Void>(dependentStage, defaultExecutor);
    }

    /**
     * @see java.util.concurrent.CompletionStage#thenRunAsync(java.lang.Runnable)
     */
    @Override
    public ManagedCompletableFuture<Void> thenRunAsync(Runnable action) {
        // Reject ManagedTask so that we have the flexibility to decide later how to handle ManagedTaskListener and execution properties
        if (action instanceof ManagedTask)
            throw new IllegalArgumentException(ManagedTask.class.getName());

        PolicyExecutor policyExecutor = defaultExecutor.getNormalPolicyExecutor(); // TODO choose based on LONGRUNNING_HINT execution property
        WSContextService contextSvc = defaultExecutor.getContextService();

        @SuppressWarnings("unchecked")
        ThreadContextDescriptor contextDescriptor = contextSvc.captureThreadContext(XPROPS_SUSPEND_TRAN);
        action = contextSvc.createContextualProxy(contextDescriptor, action, Runnable.class);

        CompletableFuture<Void> dependentStage = completableFuture.thenRunAsync(action, policyExecutor);
        return new ManagedCompletableFuture<Void>(dependentStage, defaultExecutor);
    }

    /**
     * @see java.util.concurrent.CompletionStage#thenRunAsync(java.lang.Runnable, java.util.concurrent.Executor)
     */
    @Override
    public ManagedCompletableFuture<Void> thenRunAsync(Runnable action, Executor executor) {
        CompletableFuture<Void> dependentStage;
        if (executor instanceof ManagedExecutorService) { // the only type of managed executor implementation allowed here is the built-in one
            // Reject ManagedTask so that we have the flexibility to decide later how to handle ManagedTaskListener and execution properties
            if (action instanceof ManagedTask)
                throw new IllegalArgumentException(ManagedTask.class.getName());

            WSManagedExecutorService managedExecutor = (WSManagedExecutorService) executor;
            PolicyExecutor policyExecutor = managedExecutor.getNormalPolicyExecutor(); // TODO choose based on LONGRUNNING_HINT execution property
            WSContextService contextSvc = managedExecutor.getContextService();

            @SuppressWarnings("unchecked")
            ThreadContextDescriptor contextDescriptor = contextSvc.captureThreadContext(XPROPS_SUSPEND_TRAN);
            action = contextSvc.createContextualProxy(contextDescriptor, action, Runnable.class);

            dependentStage = completableFuture.thenRunAsync(action, policyExecutor);
        } else {
            dependentStage = completableFuture.thenRunAsync(action, executor);
        }
        return new ManagedCompletableFuture<Void>(dependentStage, defaultExecutor);
    }

    /**
     * @see java.util.concurrent.CompletableFuture#toCompletableFuture()
     */
    @Override
    public CompletableFuture<T> toCompletableFuture() {
        return this;
    }

    // TODO override toString to include proper status (and maybe correlate to the underlying CompletableFuture)

    /**
     * @see java.util.concurrent.CompletionStage#whenComplete(java.util.function.BiConsumer)
     */
    @Override
    public ManagedCompletableFuture<T> whenComplete(BiConsumer<? super T, ? super Throwable> action) {
        // TODO contextualize action and other steps
        return new ManagedCompletableFuture<T>(completableFuture.whenComplete(action), defaultExecutor);
    }

    /**
     * @see java.util.concurrent.CompletionStage#whenCompleteAsync(java.util.function.BiConsumer)
     */
    @Override
    public ManagedCompletableFuture<T> whenCompleteAsync(BiConsumer<? super T, ? super Throwable> action) {
        // TODO contextualize action and other steps
        Executor policyExecutor = (Executor) defaultExecutor; // TODO get policy executor from default executor
        return new ManagedCompletableFuture<T>(completableFuture.whenCompleteAsync(action, policyExecutor), defaultExecutor);
    }

    /**
     * @see java.util.concurrent.CompletionStage#whenCompleteAsync(java.util.function.BiConsumer, java.util.concurrent.Executor)
     */
    @Override
    public ManagedCompletableFuture<T> whenCompleteAsync(BiConsumer<? super T, ? super Throwable> action, Executor executor) {
        // TODO contextualize action and other steps
        Executor policyExecutor = executor; // TODO get policy executor from the supplied executor, if a managed executor
        return new ManagedCompletableFuture<T>(completableFuture.whenCompleteAsync(action, policyExecutor), defaultExecutor);
    }

    // TODO move the following classes to the general context service in com.ibm.ws.context project once Java 8 can be used there

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