/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
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
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.kernel.service.util.JavaInfo;
import com.ibm.wsspi.threadcontext.ThreadContext;
import com.ibm.wsspi.threadcontext.ThreadContextDescriptor;
import com.ibm.wsspi.threadcontext.WSContextService;

/**
 * Extension to CompletableFuture for managed executors.
 *
 * This extension makes a managed executor and/or the Liberty thread pool be the default asynchronous
 * execution facility for a CompletableFuture. When managed executors are used, this extension ensures
 * that thread context propagation for dependent stages always follows the behavior of using the
 * context of the thread that creates the dependent stage.
 *
 * There are varying code paths for Java SE 8 and post Java SE 8 due to the fact that the CompletableFuture
 * in Java SE 8 lacks any built-in support for plugging in extensions. The code path for Java SE 8 uses
 * two instances of CompletableFuture:
 * 1) a ManagedCompletableFuture subclass itself which includes override logic and delegates to another CompletableFuture.
 * 2) a CompletableFuture instance that does the actual work.
 * Post Java SE 8, the code path uses the ManagedCompletableFuture subclass as both, relying on the
 * plugin points defaultExecutor() and newIncompleteFuture() which are invoked by the post Java SE 8 CompletableFuture.
 *
 * @param <T>
 */
public class ManagedCompletableFuture<T> extends CompletableFuture<T> {
    private static final TraceComponent tc = Tr.register(ManagedCompletableFuture.class);

    /**
     * Indicates if running on Java SE 8.
     * The Java SE 8 CompletableFuture lacks certain important methods, namely defaultExecutor and newIncompleteFuture,
     * without which it is difficult to extend Java's built-in implementation.
     */
    private static final boolean JAVA8 = JavaInfo.majorVersion() == 8;

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
     * For the Java SE 8 implementation, the real completable future to which meaningful operations are delegated.
     * For greater than Java SE 8, this value must be NULL.
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
     * Stores a futureRef value to use during construction of a ManagedCompletableFuture.
     */
    private static final ThreadLocal<AtomicReference<Future<?>>> futureRefLocal = new ThreadLocal<AtomicReference<Future<?>>>();

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

    // internal constructors

    /**
     * Construct a completable future with a managed executor as its default asynchronous execution facility.
     * Use this constructor only for Java SE 8.
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

    /**
     * Construct a completable future with a managed executor as its default asynchronous execution facility.
     *
     * @param managedExecutor managed executor service
     * @param futureRef reference to a policy executor Future that will be submitted if requested to run async. Otherwise null.
     */
    private ManagedCompletableFuture(Executor managedExecutor, AtomicReference<Future<?>> futureRef) {
        super();

        this.completableFuture = null;
        this.defaultExecutor = managedExecutor;
        this.futureRef = futureRef;
    }

    // static method equivalents for CompletableFuture

    /**
     * Replaces CompletableFuture.completedFuture(value) with an implementation that switches the
     * default asynchronous execution facility to be the default managed executor.
     *
     * @param value result of the completed future
     * @return completed completable future where the default managed executor is the default asynchronous execution facility.
     */
    public static <U> CompletableFuture<U> completedFuture(U value) {
        ManagedExecutorService defaultExecutor = AccessController.doPrivileged(getDefaultManagedExecutorAction);
        if (JAVA8) {
            return new ManagedCompletableFuture<U>(CompletableFuture.completedFuture(value), //
                            defaultExecutor, //
                            null);
        } else {
            ManagedCompletableFuture<U> completableFuture = new ManagedCompletableFuture<U>(defaultExecutor, null);
            completableFuture.super_complete(value);
            return completableFuture;
        }
    }

    /**
     * Replaces CompletableFuture.completedStage(value) with an implementation that switches the
     * default asynchronous execution facility to be the default managed executor.
     *
     * @param value result of the completed future
     * @return completed completion stage where the default managed executor is the default asynchronous execution facility.
     */
    public static <U> CompletionStage<U> completedStage(U value) {
        throw new UnsupportedOperationException(); // TODO implement when rebasing on Java 9
    }

    public static Executor delayedExecutor​(long delay, TimeUnit unit) {
        throw new UnsupportedOperationException(); // TODO implement when rebasing on Java 9
    }

    public static Executor delayedExecutor​(long delay, TimeUnit unit, Executor executor) {
        throw new UnsupportedOperationException(); // TODO implement when rebasing on Java 9
    }

    /**
     * Replaces CompletableFuture.failedFuture(Throwable) with an implementation that switches the
     * default asynchronous execution facility to be the default managed executor.
     *
     * @param x the exception.
     * @return completed completable future where the default managed executor is the default asynchronous execution facility.
     */
    public static <U> CompletableFuture<U> failedFuture(Throwable x) {
        throw new UnsupportedOperationException(); // TODO implement when rebasing on Java 9
    }

    /**
     * Replaces CompletableFuture.failedStage(Throwable) with an implementation that switches the
     * default asynchronous execution facility to be the default managed executor.
     *
     * @param x the exception.
     * @return completed completion stage where the default managed executor is the default asynchronous execution facility.
     */
    public static <U> CompletionStage<U> failedStage(Throwable x) {
        throw new UnsupportedOperationException(); // TODO implement when rebasing on Java 9
    }

    /**
     * Replaces CompletableFuture.runAsync(action) with an implementation that switches the
     * default asynchronous execution facility to be the default managed executor.
     *
     * @param action the action to run asynchronously.
     * @return completable future where the default managed executor is the default asynchronous execution facility.
     */
    @Trivial
    public static CompletableFuture<Void> runAsync(Runnable action) {
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
    @SuppressWarnings("unchecked")
    public static CompletableFuture<Void> runAsync(Runnable action, Executor executor) {
        ThreadContextDescriptor contextDescriptor;
        FutureRefExecutor futureExecutor;
        if (executor instanceof ManagedExecutorService) { // the only type of managed executor implementation allowed here is the built-in one
            // Reject ManagedTask so that we have the flexibility to decide later how to handle ManagedTaskListener and execution properties
            if (action instanceof ManagedTask)
                throw new IllegalArgumentException(ManagedTask.class.getName());

            WSManagedExecutorService managedExecutor = (WSManagedExecutorService) executor;
            contextDescriptor = managedExecutor.getContextService().captureThreadContext(XPROPS_SUSPEND_TRAN);
            futureExecutor = new FutureRefExecutor((ManagedExecutorService) executor);
        } else {
            contextDescriptor = null;
            futureExecutor = executor instanceof ExecutorService ? new FutureRefExecutor((ExecutorService) executor) : null;
        }

        if (JAVA8) {
            action = new AsyncRunnable(contextDescriptor, action, null);
            CompletableFuture<Void> completableFuture = CompletableFuture.runAsync(action, futureExecutor == null ? executor : futureExecutor);
            return new ManagedCompletableFuture<Void>(completableFuture, executor, futureExecutor);
        } else {
            ManagedCompletableFuture<Void> completableFuture = new ManagedCompletableFuture<Void>(executor, futureExecutor);
            action = new AsyncRunnable(contextDescriptor, action, completableFuture);
            (futureExecutor == null ? executor : futureExecutor).execute(action);
            return completableFuture;
        }
    }

    /**
     * Replaces CompletableFuture.supplyAsync(supplier) with an implementation that switches the
     * default asynchronous execution facility to be the default managed executor.
     *
     * @param action the supplier to invoke asynchronously.
     * @return completable future where the default managed executor is the default asynchronous execution facility.
     */
    @Trivial
    public static <U> CompletableFuture<U> supplyAsync(Supplier<U> action) {
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
    @SuppressWarnings("unchecked")
    public static <U> CompletableFuture<U> supplyAsync(Supplier<U> action, Executor executor) {
        ThreadContextDescriptor contextDescriptor;
        FutureRefExecutor futureExecutor;
        if (executor instanceof ManagedExecutorService) { // the only type of managed executor implementation allowed here is the built-in one
            // Reject ManagedTask so that we have the flexibility to decide later how to handle ManagedTaskListener and execution properties
            if (action instanceof ManagedTask)
                throw new IllegalArgumentException(ManagedTask.class.getName());

            WSManagedExecutorService managedExecutor = (WSManagedExecutorService) executor;
            contextDescriptor = managedExecutor.getContextService().captureThreadContext(XPROPS_SUSPEND_TRAN);
            futureExecutor = new FutureRefExecutor((ManagedExecutorService) executor);
        } else {
            contextDescriptor = null;
            futureExecutor = executor instanceof ExecutorService ? new FutureRefExecutor((ExecutorService) executor) : null;
        }

        if (JAVA8) {
            action = new ContextualSupplier<U>(contextDescriptor, action);
            CompletableFuture<U> completableFuture = CompletableFuture.supplyAsync(action, futureExecutor == null ? executor : futureExecutor);
            return new ManagedCompletableFuture<U>(completableFuture, executor, futureExecutor);
        } else {
            ManagedCompletableFuture<U> completableFuture = new ManagedCompletableFuture<U>(executor, futureExecutor);
            Runnable task = new AsyncSupplier<U>(contextDescriptor, action, completableFuture);
            (futureExecutor == null ? executor : futureExecutor).execute(task);
            return completableFuture;
        }
    }

    // Overrides of CompletableFuture methods

    /**
     * @see java.util.concurrent.CompletionStage#acceptEither(java.util.concurrent.CompletionStage, java.util.function.Consumer)
     */
    @Override
    public CompletableFuture<Void> acceptEither(CompletionStage<? extends T> other, Consumer<? super T> action) {
        // Reject ManagedTask so that we have the flexibility to decide later how to handle ManagedTaskListener and execution properties
        if (action instanceof ManagedTask)
            throw new IllegalArgumentException(ManagedTask.class.getName());

        WSContextService contextSvc = ((WSManagedExecutorService) defaultExecutor).getContextService();

        @SuppressWarnings("unchecked")
        ThreadContextDescriptor contextDescriptor = contextSvc.captureThreadContext(XPROPS_SUSPEND_TRAN);
        action = new ContextualConsumer<>(contextDescriptor, action);

        if (JAVA8) {
            if (other instanceof ManagedCompletableFuture)
                other = ((ManagedCompletableFuture<? extends T>) other).completableFuture;
            CompletableFuture<Void> dependentStage = completableFuture.acceptEither(other, action);
            return new ManagedCompletableFuture<Void>(dependentStage, defaultExecutor, null);
        } else {
            return super.acceptEither(other, action);
        }
    }

    /**
     * @see java.util.concurrent.CompletionStage#acceptEitherAsync(java.util.concurrent.CompletionStage, java.util.function.Consumer)
     */
    @Override
    @Trivial
    public CompletableFuture<Void> acceptEitherAsync(CompletionStage<? extends T> other, Consumer<? super T> action) {
        return acceptEitherAsync(other, action, defaultExecutor);
    }

    /**
     * @see java.util.concurrent.CompletionStage#acceptEitherAsync(java.util.concurrent.CompletionStage, java.util.function.Consumer, java.util.concurrent.Executor)
     */
    @Override
    public CompletableFuture<Void> acceptEitherAsync(CompletionStage<? extends T> other, Consumer<? super T> action, Executor executor) {
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
        } else if (executor instanceof ExecutorService) {
            futureExecutor = new FutureRefExecutor((ExecutorService) executor);
        } else {
            futureExecutor = null;
        }

        if (JAVA8) {
            if (other instanceof ManagedCompletableFuture)
                other = ((ManagedCompletableFuture<? extends T>) other).completableFuture;
            CompletableFuture<Void> dependentStage = completableFuture.acceptEitherAsync(other, action, futureExecutor == null ? executor : futureExecutor);
            return new ManagedCompletableFuture<Void>(dependentStage, defaultExecutor, futureExecutor);
        } else {
            futureRefLocal.set(futureExecutor);
            try {
                return super.acceptEitherAsync(other, action, futureExecutor == null ? executor : futureExecutor);
            } finally {
                futureRefLocal.remove();
            }
        }
    }

    /**
     * @see java.util.concurrent.CompletionStage#applyToEither(java.util.concurrent.CompletionStage, java.util.function.Function)
     */
    @Override
    public <U> CompletableFuture<U> applyToEither(CompletionStage<? extends T> other, Function<? super T, U> action) {
        // Reject ManagedTask so that we have the flexibility to decide later how to handle ManagedTaskListener and execution properties
        if (action instanceof ManagedTask)
            throw new IllegalArgumentException(ManagedTask.class.getName());

        WSContextService contextSvc = ((WSManagedExecutorService) defaultExecutor).getContextService();

        @SuppressWarnings("unchecked")
        ThreadContextDescriptor contextDescriptor = contextSvc.captureThreadContext(XPROPS_SUSPEND_TRAN);
        action = new ContextualFunction<>(contextDescriptor, action);

        if (JAVA8) {
            if (other instanceof ManagedCompletableFuture)
                other = ((ManagedCompletableFuture<? extends T>) other).completableFuture;
            CompletableFuture<U> dependentStage = completableFuture.applyToEither(other, action);
            return new ManagedCompletableFuture<U>(dependentStage, defaultExecutor, null);
        } else {
            return super.applyToEither(other, action);
        }
    }

    /**
     * @see java.util.concurrent.CompletionStage#applyToEitherAsync(java.util.concurrent.CompletionStage, java.util.function.Function)
     */
    @Override
    @Trivial
    public <U> CompletableFuture<U> applyToEitherAsync(CompletionStage<? extends T> other, Function<? super T, U> action) {
        return applyToEitherAsync(other, action, defaultExecutor);
    }

    /**
     * @see java.util.concurrent.CompletionStage#applyToEitherAsync(java.util.concurrent.CompletionStage, java.util.function.Function, java.util.concurrent.Executor)
     */
    @Override
    public <U> CompletableFuture<U> applyToEitherAsync(CompletionStage<? extends T> other, Function<? super T, U> action, Executor executor) {
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
        } else if (executor instanceof ExecutorService) {
            futureExecutor = new FutureRefExecutor((ExecutorService) executor);
        } else {
            futureExecutor = null;
        }

        if (JAVA8) {
            if (other instanceof ManagedCompletableFuture)
                other = ((ManagedCompletableFuture<? extends T>) other).completableFuture;
            CompletableFuture<U> dependentStage = completableFuture.applyToEitherAsync(other, action, futureExecutor == null ? executor : futureExecutor);
            return new ManagedCompletableFuture<U>(dependentStage, defaultExecutor, futureExecutor);
        } else {
            futureRefLocal.set(futureExecutor);
            try {
                return super.applyToEitherAsync(other, action, futureExecutor == null ? executor : futureExecutor);
            } finally {
                futureRefLocal.remove();
            }
        }
    }

    /**
     * @see java.util.concurrent.CompletableFuture#cancel(boolean)
     */
    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        boolean canceled = JAVA8 ? //
                        completableFuture.cancel(mayInterruptIfRunning) : //
                        super.cancel(mayInterruptIfRunning);

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
        boolean completedByThisMethod = JAVA8 ? //
                        completableFuture.complete(value) : //
                        super.complete(value);

        // If corresponding task has been submitted to an executor, attempt to cancel it
        if (completedByThisMethod && futureRef != null) {
            Future<?> future = futureRef.get();
            if (future != null)
                future.cancel(true);
        }

        return completedByThisMethod;
    }

    /**
     * @see java.util.concurrent.CompletableFuture#completeAsync(Supplier)
     */
    // TODO Java 9 @Override
    public CompletableFuture<T> completeAsync​(Supplier<? extends T> supplier) {
        throw new UnsupportedOperationException();
    }

    /**
     * @see java.util.concurrent.CompletableFuture#completeAsync(Supplier, Executor)
     */
    // TODO Java 9 @Override
    public CompletableFuture<T> completeAsync​(Supplier<? extends T> supplier, Executor executor) {
        throw new UnsupportedOperationException();
    }

    /**
     * @see java.util.concurrent.CompletableFuture#completeExceptionally(java.lang.Throwable)
     */
    @Override
    public boolean completeExceptionally(Throwable x) {
        boolean completedByThisMethod = JAVA8 ? //
                        completableFuture.completeExceptionally(x) : //
                        super.completeExceptionally(x);

        // If corresponding task has been submitted to an executor, attempt to cancel it
        if (completedByThisMethod && futureRef != null) {
            Future<?> future = futureRef.get();
            if (future != null)
                future.cancel(true);
        }

        return completedByThisMethod;
    }

    /**
     * @see java.util.concurrent.CompletableFuture#completeOnTimeout(Object, long, TimeUnit)
     */
    // TODO Java 9 @Override
    public CompletableFuture<T> completeOnTimeout​(T value, long timeout, TimeUnit unit) {
        throw new UnsupportedOperationException();
    }

    /**
     * @see java.util.concurrent.CompletableFuture#copy()
     */
    // TODO Java 9 @Override
    public CompletableFuture<T> copy​() {
        throw new UnsupportedOperationException();
    }

    /**
     * @see java.util.concurrent.CompletableFuture#defaultExecutor()
     */
    public Executor defaultExecutor​() {
        if (JAVA8)
            throw new UnsupportedOperationException();
        else
            return defaultExecutor;
    }

    /**
     * @see java.util.concurrent.CompletionStage#exceptionally(java.util.function.Function)
     */
    @Override
    public CompletableFuture<T> exceptionally(Function<Throwable, ? extends T> action) {
        // Reject ManagedTask so that we have the flexibility to decide later how to handle ManagedTaskListener and execution properties
        if (action instanceof ManagedTask)
            throw new IllegalArgumentException(ManagedTask.class.getName());

        WSContextService contextSvc = ((WSManagedExecutorService) defaultExecutor).getContextService();

        @SuppressWarnings("unchecked")
        ThreadContextDescriptor contextDescriptor = contextSvc.captureThreadContext(XPROPS_SUSPEND_TRAN);
        action = new ContextualFunction<>(contextDescriptor, action);

        if (JAVA8) {
            CompletableFuture<T> dependentStage = completableFuture.exceptionally(action);
            return new ManagedCompletableFuture<T>(dependentStage, defaultExecutor, null);
        } else {
            return super.exceptionally(action);
        }
    }

    /**
     * @see java.util.concurrent.CompletableFuture#get()
     */
    @Override
    public T get() throws ExecutionException, InterruptedException {
        return JAVA8 ? completableFuture.get() : //
                        super.get();
    }

    /**
     * @see java.util.concurrent.CompletableFuture#get(long, java.util.concurrent.TimeUnit)
     */
    @Override
    public T get(long timeout, TimeUnit unit) throws ExecutionException, InterruptedException, TimeoutException {
        return JAVA8 ? completableFuture.get(timeout, unit) : //
                        super.get(timeout, unit);
    }

    /**
     * @see java.util.concurrent.CompletableFuture#getNow(java.lang.Object)
     */
    @Override
    public T getNow(T valueIfAbsent) {
        return JAVA8 ? completableFuture.getNow(valueIfAbsent) : //
                        super.getNow(valueIfAbsent);
    }

    /**
     * @see java.util.concurrent.CompletableFuture#getNumberOfDependents()
     */
    @Override
    public int getNumberOfDependents() {
        if (JAVA8) {
            // Subtract out the extra dependent stage that we use internally to be notified of completion.
            int count = completableFuture.getNumberOfDependents();
            return count > 1 ? count - 1 : 0;
        } else {
            return super.getNumberOfDependents();
        }
    }

    /**
     * @see java.util.concurrent.CompletionStage#handle(java.util.function.BiFunction)
     */
    @Override
    public <R> CompletableFuture<R> handle(BiFunction<? super T, Throwable, ? extends R> action) {
        // Reject ManagedTask so that we have the flexibility to decide later how to handle ManagedTaskListener and execution properties
        if (action instanceof ManagedTask)
            throw new IllegalArgumentException(ManagedTask.class.getName());

        WSContextService contextSvc = ((WSManagedExecutorService) defaultExecutor).getContextService();

        @SuppressWarnings("unchecked")
        ThreadContextDescriptor contextDescriptor = contextSvc.captureThreadContext(XPROPS_SUSPEND_TRAN);
        action = new ContextualBiFunction<>(contextDescriptor, action);

        if (JAVA8) {
            CompletableFuture<R> dependentStage = completableFuture.handle(action);
            return new ManagedCompletableFuture<R>(dependentStage, defaultExecutor, null);
        } else {
            return super.handle(action);
        }
    }

    /**
     * @see java.util.concurrent.CompletionStage#handleAsync(java.util.function.BiFunction)
     */
    @Override
    @Trivial
    public <R> CompletableFuture<R> handleAsync(BiFunction<? super T, Throwable, ? extends R> action) {
        return handleAsync(action, defaultExecutor);
    }

    /**
     * @see java.util.concurrent.CompletionStage#handleAsync(java.util.function.BiFunction, java.util.concurrent.Executor)
     */
    @Override
    public <R> CompletableFuture<R> handleAsync(BiFunction<? super T, Throwable, ? extends R> action, Executor executor) {
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
        } else if (executor instanceof ExecutorService) {
            futureExecutor = new FutureRefExecutor((ExecutorService) executor);
        } else {
            futureExecutor = null;
        }

        if (JAVA8) {
            CompletableFuture<R> dependentStage = completableFuture.handleAsync(action, futureExecutor == null ? executor : futureExecutor);
            return new ManagedCompletableFuture<R>(dependentStage, defaultExecutor, futureExecutor);
        } else {
            futureRefLocal.set(futureExecutor);
            try {
                return super.handleAsync(action, futureExecutor == null ? executor : futureExecutor);
            } finally {
                futureRefLocal.remove();
            }
        }
    }

    /**
     * @see java.util.concurrent.CompletableFuture#isCancelled()
     */
    @Override
    public boolean isCancelled() {
        return JAVA8 ? completableFuture.isCancelled() : //
                        super.isCancelled();
    }

    /**
     * @see java.util.concurrent.CompletableFuture#isCompletedExceptionally()
     */
    @Override
    public boolean isCompletedExceptionally() {
        return JAVA8 ? completableFuture.isCompletedExceptionally() : //
                        super.isCompletedExceptionally();
    }

    /**
     * @see java.util.concurrent.CompletableFuture#isDone()
     */
    @Override
    public boolean isDone() {
        return JAVA8 ? completableFuture.isDone() : //
                        super.isDone();
    }

    /**
     * @see java.util.concurrent.CompletableFuture#join()
     */
    @Override
    public T join() {
        return JAVA8 ? completableFuture.join() : //
                        super.join();
    }

    /**
     * @see java.util.concurrent.CompletableFuture#minimalCompletionStage()
     */
    // TODO Java 9 @Override
    public CompletionStage<T> minimalCompletionStage() {
        throw new UnsupportedOperationException();
    }

    /**
     * @see java.util.concurrent.CompletableFuture#newIncompleteFuture()
     */
    // TODO Java 9 @Override
    public CompletableFuture<T> newIncompleteFuture() {
        if (JAVA8)
            return new ManagedCompletableFuture<T>(new CompletableFuture<T>(), defaultExecutor, null);
        else
            return new ManagedCompletableFuture<T>(defaultExecutor, futureRefLocal.get());
    }

    /**
     * @see java.util.concurrent.CompletableFuture#obtrudeException(java.lang.Throwable)
     */
    @Override
    public void obtrudeException(Throwable x) {
        if (JAVA8) {
            // disallow the possibility of concurrent obtrudes so as to keep the values consistent
            synchronized (completableFuture) {
                super.obtrudeException(x);
                completableFuture.obtrudeException(x);
            }
        } else {
            super.obtrudeException(x);
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
        if (JAVA8) {
            // disallow the possibility of concurrent obtrudes so as to keep the values consistent
            synchronized (completableFuture) {
                super.obtrudeValue(value);
                completableFuture.obtrudeValue(value);
            }
        } else {
            super.obtrudeValue(value);
        }

        // If corresponding task has been submitted to an executor, attempt to cancel it
        if (futureRef != null) {
            Future<?> future = futureRef.get();
            if (future != null)
                future.cancel(true);
        }
    }

    /**
     * @see java.util.concurrent.CompletableFuture#orTimeout(long, TimeUnit)
     */
    // TODO Java 9 @Override
    public CompletableFuture<T> orTimeout(long timeout, TimeUnit unit) {
        throw new UnsupportedOperationException();
    }

    /**
     * @see java.util.concurrent.CompletionStage#runAfterBoth(java.util.concurrent.CompletionStage, java.lang.Runnable)
     */
    @Override
    public CompletableFuture<Void> runAfterBoth(CompletionStage<?> other, Runnable action) {
        // Reject ManagedTask so that we have the flexibility to decide later how to handle ManagedTaskListener and execution properties
        if (action instanceof ManagedTask)
            throw new IllegalArgumentException(ManagedTask.class.getName());

        WSContextService contextSvc = ((WSManagedExecutorService) defaultExecutor).getContextService();

        @SuppressWarnings("unchecked")
        ThreadContextDescriptor contextDescriptor = contextSvc.captureThreadContext(XPROPS_SUSPEND_TRAN);
        action = contextSvc.createContextualProxy(contextDescriptor, action, Runnable.class);

        if (JAVA8) {
            if (other instanceof ManagedCompletableFuture)
                other = ((ManagedCompletableFuture<?>) other).completableFuture;
            CompletableFuture<Void> dependentStage = completableFuture.runAfterBoth(other, action);
            return new ManagedCompletableFuture<Void>(dependentStage, defaultExecutor, null);
        } else {
            return super.runAfterBoth(other, action);
        }
    }

    /**
     * @see java.util.concurrent.CompletionStage#runAfterBothAsync(java.util.concurrent.CompletionStage, java.lang.Runnable)
     */
    @Override
    @Trivial
    public CompletableFuture<Void> runAfterBothAsync(CompletionStage<?> other, Runnable action) {
        return runAfterBothAsync(other, action, defaultExecutor);
    }

    /**
     * @see java.util.concurrent.CompletionStage#runAfterBothAsync(java.util.concurrent.CompletionStage, java.lang.Runnable, java.util.concurrent.Executor)
     */
    @Override
    public CompletableFuture<Void> runAfterBothAsync(CompletionStage<?> other, Runnable action, Executor executor) {
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

        if (JAVA8) {
            if (other instanceof ManagedCompletableFuture)
                other = ((ManagedCompletableFuture<?>) other).completableFuture;
            CompletableFuture<Void> dependentStage = completableFuture.runAfterBothAsync(other, action, futureExecutor == null ? executor : futureExecutor);
            return new ManagedCompletableFuture<Void>(dependentStage, defaultExecutor, futureExecutor);
        } else {
            futureRefLocal.set(futureExecutor);
            try {
                return super.runAfterBothAsync(other, action, futureExecutor == null ? executor : futureExecutor);
            } finally {
                futureRefLocal.remove();
            }
        }
    }

    /**
     * @see java.util.concurrent.CompletionStage#runAfterEither(java.util.concurrent.CompletionStage, java.lang.Runnable)
     */
    @Override
    public CompletableFuture<Void> runAfterEither(CompletionStage<?> other, Runnable action) {
        // Reject ManagedTask so that we have the flexibility to decide later how to handle ManagedTaskListener and execution properties
        if (action instanceof ManagedTask)
            throw new IllegalArgumentException(ManagedTask.class.getName());

        WSContextService contextSvc = ((WSManagedExecutorService) defaultExecutor).getContextService();

        @SuppressWarnings("unchecked")
        ThreadContextDescriptor contextDescriptor = contextSvc.captureThreadContext(XPROPS_SUSPEND_TRAN);
        action = contextSvc.createContextualProxy(contextDescriptor, action, Runnable.class);

        if (JAVA8) {
            if (other instanceof ManagedCompletableFuture)
                other = ((ManagedCompletableFuture<?>) other).completableFuture;
            CompletableFuture<Void> dependentStage = completableFuture.runAfterEither(other, action);
            return new ManagedCompletableFuture<Void>(dependentStage, defaultExecutor, null);
        } else {
            return super.runAfterEither(other, action);
        }
    }

    /**
     * @see java.util.concurrent.CompletionStage#runAfterEitherAsync(java.util.concurrent.CompletionStage, java.lang.Runnable)
     */
    @Override
    @Trivial
    public CompletableFuture<Void> runAfterEitherAsync(CompletionStage<?> other, Runnable action) {
        return runAfterEitherAsync(other, action, defaultExecutor);
    }

    /**
     * @see java.util.concurrent.CompletionStage#runAfterEitherAsync(java.util.concurrent.CompletionStage, java.lang.Runnable, java.util.concurrent.Executor)
     */
    @Override
    public CompletableFuture<Void> runAfterEitherAsync(CompletionStage<?> other, Runnable action, Executor executor) {
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

        if (JAVA8) {
            if (other instanceof ManagedCompletableFuture)
                other = ((ManagedCompletableFuture<?>) other).completableFuture;
            CompletableFuture<Void> dependentStage = completableFuture.runAfterEitherAsync(other, action, futureExecutor == null ? executor : futureExecutor);
            return new ManagedCompletableFuture<Void>(dependentStage, defaultExecutor, futureExecutor);
        } else {
            futureRefLocal.set(futureExecutor);
            try {
                return super.runAfterEitherAsync(other, action, futureExecutor == null ? executor : futureExecutor);
            } finally {
                futureRefLocal.remove();
            }
        }
    }

    /**
     * Invokes complete on the superclass.
     *
     * @see java.util.concurrent.CompletableFuture#complete(T)
     */
    private final boolean super_complete(T value) {
        return super.complete(value);
    }

    /**
     * Invokes completeExceptionally on the superclass.
     *
     * @see java.util.concurrent.CompletableFuture#completeExceptionally(java.lang.Throwable)
     */
    private final boolean super_completeExceptionally(Throwable x) {
        return super.completeExceptionally(x);
    }

    /**
     * @see java.util.concurrent.CompletionStage#thenAccept(java.util.function.Consumer)
     */
    @Override
    public CompletableFuture<Void> thenAccept(Consumer<? super T> action) {
        // Reject ManagedTask so that we have the flexibility to decide later how to handle ManagedTaskListener and execution properties
        if (action instanceof ManagedTask)
            throw new IllegalArgumentException(ManagedTask.class.getName());

        WSContextService contextSvc = ((WSManagedExecutorService) defaultExecutor).getContextService();

        @SuppressWarnings("unchecked")
        ThreadContextDescriptor contextDescriptor = contextSvc.captureThreadContext(XPROPS_SUSPEND_TRAN);
        action = new ContextualConsumer<>(contextDescriptor, action);

        if (JAVA8) {
            CompletableFuture<Void> dependentStage = completableFuture.thenAccept(action);
            return new ManagedCompletableFuture<Void>(dependentStage, defaultExecutor, null);
        } else {
            return super.thenAccept(action);
        }
    }

    /**
     * @see java.util.concurrent.CompletionStage#thenAcceptAsync(java.util.function.Consumer)
     */
    @Override
    @Trivial
    public CompletableFuture<Void> thenAcceptAsync(Consumer<? super T> action) {
        return thenAcceptAsync(action, defaultExecutor);
    }

    /**
     * @see java.util.concurrent.CompletionStage#thenAcceptAsync(java.util.function.Consumer, java.util.concurrent.Executor)
     */
    @Override
    public CompletableFuture<Void> thenAcceptAsync(Consumer<? super T> action, Executor executor) {
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
        } else if (executor instanceof ExecutorService) {
            futureExecutor = new FutureRefExecutor((ExecutorService) executor);
        } else {
            futureExecutor = null;
        }

        if (JAVA8) {
            CompletableFuture<Void> dependentStage = completableFuture.thenAcceptAsync(action, futureExecutor == null ? executor : futureExecutor);
            return new ManagedCompletableFuture<Void>(dependentStage, defaultExecutor, futureExecutor);
        } else {
            futureRefLocal.set(futureExecutor);
            try {
                return super.thenAcceptAsync(action, futureExecutor == null ? executor : futureExecutor);
            } finally {
                futureRefLocal.remove();
            }
        }
    }

    /**
     * @see java.util.concurrent.CompletionStage#thenAcceptBoth(java.util.concurrent.CompletionStage, java.util.function.BiConsumer)
     */
    @Override
    public <U> CompletableFuture<Void> thenAcceptBoth(CompletionStage<? extends U> other, BiConsumer<? super T, ? super U> action) {
        // Reject ManagedTask so that we have the flexibility to decide later how to handle ManagedTaskListener and execution properties
        if (action instanceof ManagedTask)
            throw new IllegalArgumentException(ManagedTask.class.getName());

        WSContextService contextSvc = ((WSManagedExecutorService) defaultExecutor).getContextService();

        @SuppressWarnings("unchecked")
        ThreadContextDescriptor contextDescriptor = contextSvc.captureThreadContext(XPROPS_SUSPEND_TRAN);
        action = new ContextualBiConsumer<>(contextDescriptor, action);

        if (JAVA8) {
            if (other instanceof ManagedCompletableFuture)
                other = ((ManagedCompletableFuture<? extends U>) other).completableFuture;
            CompletableFuture<Void> dependentStage = completableFuture.thenAcceptBoth(other, action);
            return new ManagedCompletableFuture<Void>(dependentStage, defaultExecutor, null);
        } else {
            return super.thenAcceptBoth(other, action);
        }
    }

    /**
     * @see java.util.concurrent.CompletionStage#thenAcceptBothAsync(java.util.concurrent.CompletionStage, java.util.function.BiConsumer)
     */
    @Override
    @Trivial
    public <U> CompletableFuture<Void> thenAcceptBothAsync(CompletionStage<? extends U> other, BiConsumer<? super T, ? super U> action) {
        return thenAcceptBothAsync(other, action, defaultExecutor);
    }

    /**
     * @see java.util.concurrent.CompletionStage#thenAcceptBothAsync(java.util.concurrent.CompletionStage, java.util.function.BiConsumer, java.util.concurrent.Executor)
     */
    @Override
    public <U> CompletableFuture<Void> thenAcceptBothAsync(CompletionStage<? extends U> other, BiConsumer<? super T, ? super U> action, Executor executor) {
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
        } else if (executor instanceof ExecutorService) {
            futureExecutor = new FutureRefExecutor((ExecutorService) executor);
        } else {
            futureExecutor = null;
        }

        if (JAVA8) {
            if (other instanceof ManagedCompletableFuture)
                other = ((ManagedCompletableFuture<? extends U>) other).completableFuture;
            CompletableFuture<Void> dependentStage = completableFuture.thenAcceptBothAsync(other, action, futureExecutor == null ? executor : futureExecutor);
            return new ManagedCompletableFuture<Void>(dependentStage, defaultExecutor, futureExecutor);
        } else {
            futureRefLocal.set(futureExecutor);
            try {
                return super.thenAcceptBothAsync(other, action, futureExecutor == null ? executor : futureExecutor);
            } finally {
                futureRefLocal.remove();
            }
        }
    }

    /**
     * @see java.util.concurrent.CompletionStage#thenApply(java.util.function.Function)
     */
    @Override
    public <R> CompletableFuture<R> thenApply(Function<? super T, ? extends R> action) {
        // Reject ManagedTask so that we have the flexibility to decide later how to handle ManagedTaskListener and execution properties
        if (action instanceof ManagedTask)
            throw new IllegalArgumentException(ManagedTask.class.getName());

        WSContextService contextSvc = ((WSManagedExecutorService) defaultExecutor).getContextService();

        @SuppressWarnings("unchecked")
        ThreadContextDescriptor contextDescriptor = contextSvc.captureThreadContext(XPROPS_SUSPEND_TRAN);
        action = new ContextualFunction<>(contextDescriptor, action);

        if (JAVA8) {
            CompletableFuture<R> dependentStage = completableFuture.thenApply(action);
            return new ManagedCompletableFuture<R>(dependentStage, defaultExecutor, null);
        } else {
            return super.thenApply(action);
        }
    }

    /**
     * @see java.util.concurrent.CompletionStage#thenApplyAsync(java.util.function.Function)
     */
    @Override
    @Trivial
    public <R> CompletableFuture<R> thenApplyAsync(Function<? super T, ? extends R> action) {
        return thenApplyAsync(action, defaultExecutor);
    }

    /**
     * @see java.util.concurrent.CompletionStage#thenApplyAsync(java.util.function.Function, java.util.concurrent.Executor)
     */
    @Override
    public <R> CompletableFuture<R> thenApplyAsync(Function<? super T, ? extends R> action, Executor executor) {
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
        } else if (executor instanceof ExecutorService) {
            futureExecutor = new FutureRefExecutor((ExecutorService) executor);
        } else {
            futureExecutor = null;
        }

        if (JAVA8) {
            CompletableFuture<R> dependentStage = completableFuture.thenApplyAsync(action, futureExecutor == null ? executor : futureExecutor);
            return new ManagedCompletableFuture<R>(dependentStage, defaultExecutor, futureExecutor);
        } else {
            futureRefLocal.set(futureExecutor);
            try {
                return super.thenApplyAsync(action, futureExecutor == null ? executor : futureExecutor);
            } finally {
                futureRefLocal.remove();
            }
        }
    }

    /**
     * @see java.util.concurrent.CompletionStage#thenCombine(java.util.concurrent.CompletionStage, java.util.function.BiFunction)
     */
    @Override
    public <U, R> CompletableFuture<R> thenCombine(CompletionStage<? extends U> other, BiFunction<? super T, ? super U, ? extends R> action) {
        // Reject ManagedTask so that we have the flexibility to decide later how to handle ManagedTaskListener and execution properties
        if (action instanceof ManagedTask)
            throw new IllegalArgumentException(ManagedTask.class.getName());

        WSContextService contextSvc = ((WSManagedExecutorService) defaultExecutor).getContextService();

        @SuppressWarnings("unchecked")
        ThreadContextDescriptor contextDescriptor = contextSvc.captureThreadContext(XPROPS_SUSPEND_TRAN);
        action = new ContextualBiFunction<>(contextDescriptor, action);

        if (JAVA8) {
            if (other instanceof ManagedCompletableFuture)
                other = ((ManagedCompletableFuture<? extends U>) other).completableFuture;
            CompletableFuture<R> dependentStage = completableFuture.thenCombine(other, action);
            return new ManagedCompletableFuture<R>(dependentStage, defaultExecutor, null);
        } else {
            return super.thenCombine(other, action);
        }
    }

    /**
     * @see java.util.concurrent.CompletionStage#thenCombineAsync(java.util.concurrent.CompletionStage, java.util.function.BiFunction)
     */
    @Override
    @Trivial
    public <U, R> CompletableFuture<R> thenCombineAsync(CompletionStage<? extends U> other, BiFunction<? super T, ? super U, ? extends R> action) {
        return thenCombineAsync(other, action, defaultExecutor);
    }

    /**
     * @see java.util.concurrent.CompletionStage#thenCombineAsync(java.util.concurrent.CompletionStage, java.util.function.BiFunction, java.util.concurrent.Executor)
     */
    @Override
    public <U, R> CompletableFuture<R> thenCombineAsync(CompletionStage<? extends U> other, BiFunction<? super T, ? super U, ? extends R> action, Executor executor) {
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
        } else if (executor instanceof ExecutorService) {
            futureExecutor = new FutureRefExecutor((ExecutorService) executor);
        } else {
            futureExecutor = null;
        }

        if (JAVA8) {
            if (other instanceof ManagedCompletableFuture)
                other = ((ManagedCompletableFuture<? extends U>) other).completableFuture;
            CompletableFuture<R> dependentStage = completableFuture.thenCombineAsync(other, action, futureExecutor == null ? executor : futureExecutor);
            return new ManagedCompletableFuture<R>(dependentStage, defaultExecutor, futureExecutor);
        } else {
            futureRefLocal.set(futureExecutor);
            try {
                return super.thenCombineAsync(other, action, futureExecutor == null ? executor : futureExecutor);
            } finally {
                futureRefLocal.remove();
            }
        }
    }

    /**
     * @see java.util.concurrent.CompletionStage#thenCompose(java.util.function.Function)
     */
    @Override
    public <U> CompletableFuture<U> thenCompose(Function<? super T, ? extends CompletionStage<U>> action) {
        // Reject ManagedTask so that we have the flexibility to decide later how to handle ManagedTaskListener and execution properties
        if (action instanceof ManagedTask)
            throw new IllegalArgumentException(ManagedTask.class.getName());

        WSContextService contextSvc = ((WSManagedExecutorService) defaultExecutor).getContextService();

        @SuppressWarnings("unchecked")
        ThreadContextDescriptor contextDescriptor = contextSvc.captureThreadContext(XPROPS_SUSPEND_TRAN);
        action = new ContextualFunction<>(contextDescriptor, action);

        if (JAVA8) {
            CompletableFuture<U> dependentStage = completableFuture.thenCompose(action);
            return new ManagedCompletableFuture<U>(dependentStage, defaultExecutor, null);
        } else {
            return super.thenCompose(action);
        }
    }

    /**
     * @see java.util.concurrent.CompletionStage#thenComposeAsync(java.util.function.Function)
     */
    @Override
    @Trivial
    public <U> CompletableFuture<U> thenComposeAsync(Function<? super T, ? extends CompletionStage<U>> action) {
        return thenComposeAsync(action, defaultExecutor);
    }

    /**
     * @see java.util.concurrent.CompletionStage#thenComposeAsync(java.util.function.Function, java.util.concurrent.Executor)
     */
    @Override
    public <U> CompletableFuture<U> thenComposeAsync(Function<? super T, ? extends CompletionStage<U>> action, Executor executor) {
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
        } else if (executor instanceof ExecutorService) {
            futureExecutor = new FutureRefExecutor((ExecutorService) executor);
        } else {
            futureExecutor = null;
        }

        if (JAVA8) {
            CompletableFuture<U> dependentStage = completableFuture.thenComposeAsync(action, futureExecutor == null ? executor : futureExecutor);
            return new ManagedCompletableFuture<U>(dependentStage, defaultExecutor, futureExecutor);
        } else {
            futureRefLocal.set(futureExecutor);
            try {
                return super.thenComposeAsync(action, futureExecutor == null ? executor : futureExecutor);
            } finally {
                futureRefLocal.remove();
            }
        }
    }

    /**
     * @see java.util.concurrent.CompletionStage#thenRun(java.lang.Runnable)
     */
    @Override
    public CompletableFuture<Void> thenRun(Runnable action) {
        // Reject ManagedTask so that we have the flexibility to decide later how to handle ManagedTaskListener and execution properties
        if (action instanceof ManagedTask)
            throw new IllegalArgumentException(ManagedTask.class.getName());

        WSContextService contextSvc = ((WSManagedExecutorService) defaultExecutor).getContextService();

        @SuppressWarnings("unchecked")
        ThreadContextDescriptor contextDescriptor = contextSvc.captureThreadContext(XPROPS_SUSPEND_TRAN);
        action = contextSvc.createContextualProxy(contextDescriptor, action, Runnable.class);

        if (JAVA8) {
            CompletableFuture<Void> dependentStage = completableFuture.thenRun(action);
            return new ManagedCompletableFuture<Void>(dependentStage, defaultExecutor, null);
        } else {
            return super.thenRun(action);
        }
    }

    /**
     * @see java.util.concurrent.CompletionStage#thenRunAsync(java.lang.Runnable)
     */
    @Override
    @Trivial
    public CompletableFuture<Void> thenRunAsync(Runnable action) {
        return thenRunAsync(action, defaultExecutor);
    }

    /**
     * @see java.util.concurrent.CompletionStage#thenRunAsync(java.lang.Runnable, java.util.concurrent.Executor)
     */
    @Override
    public CompletableFuture<Void> thenRunAsync(Runnable action, Executor executor) {
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

        if (JAVA8) {
            CompletableFuture<Void> dependentStage = completableFuture.thenRunAsync(action, futureExecutor == null ? executor : futureExecutor);
            return new ManagedCompletableFuture<Void>(dependentStage, defaultExecutor, futureExecutor);
        } else {
            futureRefLocal.set(futureExecutor);
            try {
                return super.thenRunAsync(action, futureExecutor == null ? executor : futureExecutor);
            } finally {
                futureRefLocal.remove();
            }
        }
    }

    /**
     * @see java.util.concurrent.CompletableFuture#toCompletableFuture()
     */
    @Override
    public CompletableFuture<T> toCompletableFuture() {
        return this;
    }

    /**
     * Example output for post Java SE 8:
     * ManagedCompletableFuture@11111111[Not completed, 2 dependents] via PolicyTaskFuture@33333333 for
     * java.util.concurrent.CompletableFuture$AsyncRun@44444444 RUNNING on concurrencyPolicy[defaultConcurrencyPolicy]
     *
     * Example output for Java SE 8:
     * ManagedCompletableFuture@11111111[22222222 Not completed, 2 dependents] via PolicyTaskFuture@33333333 for
     * java.util.concurrent.CompletableFuture$AsyncRun@44444444 RUNNING on concurrencyPolicy[defaultConcurrencyPolicy]
     *
     * @see java.util.concurrent.CompletableFuture#toString()
     */
    @Override
    @Trivial
    public String toString() {
        StringBuilder s = new StringBuilder(250).append("ManagedCompletableFuture@").append(Integer.toHexString(System.identityHashCode(this))).append('[');
        if (JAVA8)
            s.append(Integer.toHexString(completableFuture.hashCode())).append(' ');
        if (JAVA8 ? completableFuture.isDone() : super.isDone())
            if (JAVA8 ? completableFuture.isCompletedExceptionally() : super.isCompletedExceptionally())
                s.append("Completed exceptionally]");
            else
                s.append("Completed normally]");
        else {
            s.append("Not completed");
            // For Java SE 8, subtract out the extra dependent stage that we use internally to be notified of completion.
            int d = JAVA8 ? completableFuture.getNumberOfDependents() - 1 : super.getNumberOfDependents();
            if (d > 0)
                s.append(", ").append(d).append(" dependents]");
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
    public CompletableFuture<T> whenComplete(BiConsumer<? super T, ? super Throwable> action) {
        // Reject ManagedTask so that we have the flexibility to decide later how to handle ManagedTaskListener and execution properties
        if (action instanceof ManagedTask)
            throw new IllegalArgumentException(ManagedTask.class.getName());

        WSContextService contextSvc = ((WSManagedExecutorService) defaultExecutor).getContextService();

        @SuppressWarnings("unchecked")
        ThreadContextDescriptor contextDescriptor = contextSvc.captureThreadContext(XPROPS_SUSPEND_TRAN);
        action = new ContextualBiConsumer<>(contextDescriptor, action);

        if (JAVA8) {
            CompletableFuture<T> dependentStage = completableFuture.whenComplete(action);
            return new ManagedCompletableFuture<T>(dependentStage, defaultExecutor, null);
        } else {
            return super.whenComplete(action);
        }
    }

    /**
     * @see java.util.concurrent.CompletionStage#whenCompleteAsync(java.util.function.BiConsumer)
     */
    @Override
    @Trivial
    public CompletableFuture<T> whenCompleteAsync(BiConsumer<? super T, ? super Throwable> action) {
        return whenCompleteAsync(action, defaultExecutor);
    }

    /**
     * @see java.util.concurrent.CompletionStage#whenCompleteAsync(java.util.function.BiConsumer, java.util.concurrent.Executor)
     */
    @Override
    public CompletableFuture<T> whenCompleteAsync(BiConsumer<? super T, ? super Throwable> action, Executor executor) {
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
        } else if (executor instanceof ExecutorService) {
            futureExecutor = new FutureRefExecutor((ExecutorService) executor);
        } else {
            futureExecutor = null;
        }

        if (JAVA8) {
            CompletableFuture<T> dependentStage = completableFuture.whenCompleteAsync(action, futureExecutor == null ? executor : futureExecutor);
            return new ManagedCompletableFuture<T>(dependentStage, defaultExecutor, futureExecutor);
        } else {
            futureRefLocal.set(futureExecutor);
            try {
                return super.whenCompleteAsync(action, futureExecutor == null ? executor : futureExecutor);
            } finally {
                futureRefLocal.remove();
            }
        }
    }

    /**
     * Proxy for Runnable that applies thread context before running and removes it afterward.
     * Triggers completion of the provided CompletableFuture upon Runnable completion.
     */
    private static class AsyncRunnable implements Runnable {
        private final Runnable action;
        private final ManagedCompletableFuture<Void> completableFuture; // Null if Java SE 8
        private final ThreadContextDescriptor threadContextDescriptor;

        private AsyncRunnable(ThreadContextDescriptor threadContextDescriptor, Runnable action, ManagedCompletableFuture<Void> completableFuture) {
            this.action = action;
            this.completableFuture = completableFuture;
            this.threadContextDescriptor = threadContextDescriptor;
        }

        @FFDCIgnore({ Error.class, RuntimeException.class })
        @Override
        public void run() {
            Throwable failure = null;
            ArrayList<ThreadContext> contextApplied = threadContextDescriptor == null ? null : threadContextDescriptor.taskStarting();
            try {
                action.run();
            } catch (Error x) {
                failure = x;
                throw x;
            } catch (RuntimeException x) {
                failure = x;
                throw x;
            } finally {
                try {
                    if (threadContextDescriptor != null)
                        threadContextDescriptor.taskStopping(contextApplied);
                } finally {
                    if (!JAVA8)
                        if (failure == null)
                            completableFuture.super_complete(null);
                        else
                            completableFuture.super_completeExceptionally(failure);
                }
            }
        }
    }

    /**
     * Proxy for Supplier that applies thread context before running and removes it afterward.
     * Triggers completion of the provided CompletableFuture upon Supplier completion.
     *
     * @param <T> type of the result that is supplied by the supplier
     */
    private static class AsyncSupplier<T> implements Runnable {
        private final Supplier<T> action;
        private final ManagedCompletableFuture<T> completableFuture;
        private final ThreadContextDescriptor threadContextDescriptor;

        private AsyncSupplier(ThreadContextDescriptor threadContextDescriptor, Supplier<T> action, ManagedCompletableFuture<T> completableFuture) {
            this.action = action;
            this.completableFuture = completableFuture;
            this.threadContextDescriptor = threadContextDescriptor;
        }

        @FFDCIgnore({ Error.class, RuntimeException.class })
        @Override
        public void run() {
            T result = null;
            Throwable failure = null;
            ArrayList<ThreadContext> contextApplied = threadContextDescriptor == null ? null : threadContextDescriptor.taskStarting();
            try {
                result = action.get();
            } catch (Error x) {
                failure = x;
                throw x;
            } catch (RuntimeException x) {
                failure = x;
                throw x;
            } finally {
                try {
                    if (threadContextDescriptor != null)
                        threadContextDescriptor.taskStopping(contextApplied);
                } finally {
                    if (failure == null)
                        completableFuture.super_complete(result);
                    else
                        completableFuture.super_completeExceptionally(failure);
                }
            }
        }
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