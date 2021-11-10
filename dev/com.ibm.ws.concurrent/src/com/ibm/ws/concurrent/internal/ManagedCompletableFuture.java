/*******************************************************************************
 * Copyright (c) 2017, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.concurrent.internal;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.enterprise.concurrent.ManagedTask;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.concurrent.WSManagedExecutorService;
import com.ibm.ws.kernel.service.util.JavaInfo;
import com.ibm.ws.threading.PolicyExecutor;
import com.ibm.wsspi.threadcontext.ThreadContextDescriptor;

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
 * @param <T> type of result
 */
public class ManagedCompletableFuture<T> extends CompletableFuture<T> {
    private static final TraceComponent tc = Tr.register(ManagedCompletableFuture.class);

    /**
     * Indicates if running on Java SE 8.
     * The Java SE 8 CompletableFuture lacks certain important methods, namely defaultExecutor and newIncompleteFuture,
     * without which it is difficult to extend Java's built-in implementation.
     */
    static final boolean JAVA8;

    // Java 12 methods // TODO remove usage of reflection once Liberty compiles against higher Java level
    private static final MethodHandle super_exceptionallyAsync, super_exceptionallyCompose, super_exceptionallyComposeAsync;

    static {
        int version = JavaInfo.majorVersion();
        JAVA8 = version == 8;

        if (version < 12)
            super_exceptionallyAsync = super_exceptionallyCompose = super_exceptionallyComposeAsync = null;
        else {
            MethodHandles.Lookup methods = MethodHandles.lookup();
            MethodHandle exceptionallyAsync = null, exceptionallyCompose = null, exceptionallyComposeAsync = null;

            try {
                exceptionallyAsync = methods.findSpecial(CompletableFuture.class,
                                                         "exceptionallyAsync",
                                                         MethodType.methodType(CompletableFuture.class, Function.class, Executor.class),
                                                         ManagedCompletableFuture.class);

                exceptionallyCompose = methods.findSpecial(CompletableFuture.class,
                                                           "exceptionallyCompose",
                                                           MethodType.methodType(CompletableFuture.class, Function.class),
                                                           ManagedCompletableFuture.class);

                exceptionallyComposeAsync = methods.findSpecial(CompletableFuture.class,
                                                                "exceptionallyComposeAsync",
                                                                MethodType.methodType(CompletableFuture.class, Function.class, Executor.class),
                                                                ManagedCompletableFuture.class);
            } catch (IllegalAccessException | NoSuchMethodException x) {
                throw new ExceptionInInitializerError(x);
            }

            super_exceptionallyAsync = exceptionallyAsync;
            super_exceptionallyCompose = exceptionallyCompose;
            super_exceptionallyComposeAsync = exceptionallyComposeAsync;
        }
    }

    /**
     * Execution property that indicates a task should run with any previous transaction suspended.
     */
    private static final Map<String, String> XPROPS_SUSPEND_TRAN = new TreeMap<String, String>();
    static {
        XPROPS_SUSPEND_TRAN.put("jakarta.enterprise.concurrent.TRANSACTION", "SUSPEND");
        XPROPS_SUSPEND_TRAN.put("javax.enterprise.concurrent.TRANSACTION", "SUSPEND");
    }

    /**
     * Privileged action that obtains the Liberty non-deferrable ScheduledExecutorService.
     */
    private static PrivilegedAction<ScheduledExecutorService> getScheduledExecutorAction = () -> {
        BundleContext bc = FrameworkUtil.getBundle(ManagedCompletableFuture.class).getBundleContext();
        Collection<ServiceReference<ScheduledExecutorService>> refs;
        try {
            refs = bc.getServiceReferences(ScheduledExecutorService.class, "(deferrable=false)");
        } catch (InvalidSyntaxException x) {
            throw new RuntimeException(x); // should never happen
        }
        if (refs.isEmpty())
            throw new IllegalStateException("ScheduledExecutorService");
        return bc.getService(refs.iterator().next());
    };

    /**
     * For the Java SE 8 implementation, the real completable future to which meaningful operations are delegated.
     * For greater than Java SE 8, this value must be NULL.
     */
    final CompletableFuture<T> completableFuture;

    /**
     * The default asynchronous execution facility for this completable future.
     * Typically, this is a managed executor, but it is possible to use any executor.
     * (For example, the Liberty global thread pool)
     */
    final Executor defaultExecutor;

    /**
     * Reference to the policy executor Future (if any) upon which the action runs.
     * Reference is null when the action cannot be async.
     * Value is null when an async action has not yet been submitted.
     */
    private final AtomicReference<Future<?>> futureRef;

    /**
     * Stores a futureRef value to use during construction of a ManagedCompletableFuture.
     */
    private static final ThreadLocal<FutureRefExecutor> futureRefLocal = new ThreadLocal<FutureRefExecutor>();

    /**
     * Redirects the CompletableFuture implementation to use ExecutorService.submit rather than Executor.execute,
     * such that we can track the underlying Future that is used for the CompletableFuture, so that we can monitor
     * and cancel it.
     * Instances of this class are intended for one-time use, as each instance tracks a single Future.
     */
    @Trivial
    static class FutureRefExecutor extends AtomicReference<Future<?>> implements Executor {
        private static final long serialVersionUID = 1L;

        /**
         * The reference back to the completable future allows the PolicyExecutor to complete
         * the completable future upon cancel/abort of the PolicyTaskFuture.
         * With the implementation for Java 8, this is a best-effort attempt because there is
         * a timing window where the cancel/abort could happen before completableFutureRef is
         * populated.
         * However, with the implementation for Java 9+ the timing window is eliminated,
         * such that completableFutureRef is always populated first (meaning direct reference
         * could be used instead of AtomicReference). If Java 8 support is ever dropped, this
         * can be updated.
         */
        private final CancellableStageRef cancellableStage = new CancellableStageRef();

        private final ExecutorService executor;

        private FutureRefExecutor(ExecutorService executorService) {
            if (executorService instanceof WSManagedExecutorService)
                // TODO choose based on LONGRUNNING_HINT execution property
                executor = ((WSManagedExecutorService) executorService).getNormalPolicyExecutor();
            else
                executor = executorService;
        }

        @Override
        public void execute(Runnable command) {
            Future<?> future;
            if (executor instanceof PolicyExecutor)
                future = ((PolicyExecutor) executor).submit(cancellableStage, command);
            else
                future = executor.submit(command);

            set(future);
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
    ManagedCompletableFuture(CompletableFuture<T> completableFuture, Executor managedExecutor, FutureRefExecutor futureRef) {
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

        if (futureRef != null)
            futureRef.cancellableStage.set(this);
    }

    /**
     * Construct a completable future with a managed executor as its default asynchronous execution facility.
     *
     * @param managedExecutor managed executor service
     * @param futureRef reference to a policy executor Future that will be submitted if requested to run async. Otherwise null.
     */
    ManagedCompletableFuture(Executor managedExecutor, FutureRefExecutor futureRef) {
        super();

        this.completableFuture = null;
        this.defaultExecutor = managedExecutor;
        this.futureRef = futureRef;

        if (futureRef != null)
            futureRef.cancellableStage.set(this);
    }

    // static method equivalents for CompletableFuture, plus other static methods for ManagedExecutorImpl to use

    /**
     * Because CompletableFuture.completedFuture is static, this is not a true override.
     * It will be difficult for the user to invoke this method because they would need to get the class
     * of the CompletableFuture implementation and locate the static completedFuture method on that.
     *
     * @throws UnsupportedOperationException directing the user to use the ManagedExecutor spec interface instead.
     */
    @Trivial
    public static <U> CompletableFuture<U> completedFuture(U value) {
        throw new UnsupportedOperationException(Tr.formatMessage(tc, "CWWKC1156.not.supported", "ManagedExecutor.completedFuture"));
    }

    /**
     * Provides the implementation of managedExecutor.completedFuture(value) where the target
     * executor is the default asynchronous execution facility.
     *
     * @param value result of the completed future
     * @param executor executor to become the default asynchronous execution facility for the completed future
     * @return completed completable future
     */
    @Trivial // traced by caller
    static <U> CompletableFuture<U> completedFuture(U value, Executor executor) {
        if (JAVA8) {
            return new ManagedCompletableFuture<U>(CompletableFuture.completedFuture(value), executor, null);
        } else {
            ManagedCompletableFuture<U> future = new ManagedCompletableFuture<U>(executor, null);
            future.super_complete(value);
            return future;
        }
    }

    /**
     * Because CompletableFuture.completedStage is static, this is not a true override.
     * It will be difficult for the user to invoke this method because they would need to get the class
     * of the CompletableFuture implementation and locate the static completedStage method on that.
     *
     * @throws UnsupportedOperationException directing the user to use the ManagedExecutor spec interface instead.
     */
    @Trivial
    public static <U> CompletionStage<U> completedStage(U value) {
        throw new UnsupportedOperationException(Tr.formatMessage(tc, "CWWKC1156.not.supported", "ManagedExecutor.completedStage"));
    }

    /**
     * Provides the implementation of managedExecutor.completedStage(value) where the target
     * executor is the default asynchronous execution facility.
     */
    @Trivial // traced by caller
    static <U> CompletionStage<U> completedStage(U value, Executor executor) {
        if (JAVA8) {
            return new ManagedCompletionStage<U>(CompletableFuture.completedFuture(value), executor, null);
        } else {
            ManagedCompletableFuture<U> stage = new ManagedCompletionStage<U>(executor);
            stage.super_complete(value);
            return stage;
        }
    }

    /**
     * Because CompletableFuture.delayedExecutor is static, this is not a true override.
     * It will be difficult for the user to invoke this method because they would need to get the class
     * of the CompletableFuture implementation and locate the static delayedExecutor method on that.
     *
     * @throws UnsupportedOperationException
     */
    @Trivial
    public static Executor delayedExecutor(long delay, TimeUnit unit) {
        throw new UnsupportedOperationException();
    }

    /**
     * Because CompletableFuture.delayedExecutor is static, this is not a true override.
     * It will be difficult for the user to invoke this method because they would need to get the class
     * of the CompletableFuture implementation and locate the static delayedExecutor method on that.
     *
     * @throws UnsupportedOperationException
     */
    @Trivial
    public static Executor delayedExecutor(long delay, TimeUnit unit, Executor executor) {
        throw new UnsupportedOperationException();
    }

    /**
     * Because CompletableFuture.failedFuture is static, this is not a true override.
     * It will be difficult for the user to invoke this method because they would need to get the class
     * of the CompletableFuture implementation and locate the static failedFuture method on that.
     *
     * @throws UnsupportedOperationException directing the user to use the ManagedExecutor spec interface instead.
     */
    @Trivial
    public static <U> CompletableFuture<U> failedFuture(Throwable x) {
        throw new UnsupportedOperationException(Tr.formatMessage(tc, "CWWKC1156.not.supported", "ManagedExecutor.failedFuture"));
    }

    /**
     * Provides the implementation of managedExecutor.failedFuture(value) where the target
     * executor is the default asynchronous execution facility.
     *
     * @param x the exception.
     * @param executor executor to become the default asynchronous execution facility for the completed future
     * @return completed completable future
     */
    @Trivial // traced by caller
    static <U> CompletableFuture<U> failedFuture(Throwable x, Executor executor) {
        if (JAVA8) {
            CompletableFuture<U> failedFuture = new CompletableFuture<U>();
            failedFuture.completeExceptionally(x);
            return new ManagedCompletableFuture<U>(failedFuture, executor, null);
        } else {
            ManagedCompletableFuture<U> future = new ManagedCompletableFuture<U>(executor, null);
            future.super_completeExceptionally(x);
            return future;
        }
    }

    /**
     * Because CompletableFuture.failedStage is static, this is not a true override.
     * It will be difficult for the user to invoke this method because they would need to get the class
     * of the CompletableFuture implementation and locate the static failedStage method on that.
     *
     * @throws UnsupportedOperationException directing the user to use the ManagedExecutor spec interface instead.
     */
    @Trivial
    public static <U> CompletionStage<U> failedStage(Throwable x) {
        throw new UnsupportedOperationException(Tr.formatMessage(tc, "CWWKC1156.not.supported", "ManagedExecutor.failedStage"));
    }

    /**
     * Provides the implementation of managedExecutor.failedStage(value) where the target
     * executor is the default asynchronous execution facility.
     *
     * @param x the exception.
     * @param executor executor to become the default asynchronous execution facility for the completion stage
     * @return completed completion stage
     */
    @Trivial // traced by caller
    static <U> CompletionStage<U> failedStage(Throwable x, Executor executor) {
        if (JAVA8) {
            CompletableFuture<U> failedFuture = new CompletableFuture<U>();
            failedFuture.completeExceptionally(x);
            return new ManagedCompletionStage<U>(failedFuture, executor, null);
        } else {
            ManagedCompletableFuture<U> stage = new ManagedCompletionStage<U>(executor);
            stage.super_completeExceptionally(x);
            return stage;
        }
    }

    // TODO this method is only used (reflectively) by the test case to simulate the ability that JAX-RS
    // and other components should have to construct CompletableFutures that are backed directly by the
    // Liberty executor (no managed executor service).  Consider exposing this via
    // WSExecutorService.newIncompleteFuture() SPI, or if we want to expose it more generically for
    // ANY executor, then write a new SPI for it.
    /**
     * Construct a new incomplete CompletableFuture that is backed by the specified executor.
     *
     * @param executor the default asynchronous execution facility for the new CompletableFuture.
     * @return incomplete completable future where the specified executor is the default asynchronous execution facility.
     */
    public static <T> CompletableFuture<T> newIncompleteFuture(Executor executor) {
        if (JAVA8)
            return new ManagedCompletableFuture<T>(new CompletableFuture<T>(), executor, null);
        else
            return new ManagedCompletableFuture<T>(executor, null);
    }

    /**
     * Because CompletableFuture.runAsync is static, this is not a true override.
     * It will be difficult for the user to invoke this method because they would need to get the class
     * of the CompletableFuture implementation and locate the static runAsync method on that.
     *
     * @throws UnsupportedOperationException directing the user to use the ManagedExecutor spec interface instead.
     */
    @Trivial
    public static CompletableFuture<Void> runAsync(Runnable action) {
        throw new UnsupportedOperationException(Tr.formatMessage(tc, "CWWKC1156.not.supported", "ManagedExecutor.runAsync"));
    }

    /**
     * Alternative to CompletableFuture.runAsync(action, executor) with an implementation that switches the
     * default asynchronous execution facility to be the specified managed executor.
     *
     * @param action the action to run asynchronously.
     * @param executor the executor, typically a managed executor, that becomes the default asynchronous execution facility for the completable future.
     * @return completable future where the specified managed executor is the default asynchronous execution facility.
     */
    @Trivial // traced by caller
    public static CompletableFuture<Void> runAsync(Runnable action, Executor executor) {
        rejectManagedTask(action);

        FutureRefExecutor futureExecutor = supportsAsync(executor);

        ThreadContextDescriptor contextDescriptor;
        if (action instanceof ContextualSupplier) {
            ContextualRunnable r = (ContextualRunnable) action;
            contextDescriptor = r.getContextDescriptor();
            action = r.getAction();
        } else if (executor instanceof WSManagedExecutorService) {
            contextDescriptor = ((WSManagedExecutorService) executor).captureThreadContext(XPROPS_SUSPEND_TRAN);
        } else {
            contextDescriptor = null;
        }

        if (JAVA8) {
            action = new ContextualRunnable(contextDescriptor, action);
            CompletableFuture<Void> completableFuture = CompletableFuture.runAsync(action, futureExecutor == null ? executor : futureExecutor);
            return new ManagedCompletableFuture<Void>(completableFuture, executor, futureExecutor);
        } else {
            ManagedCompletableFuture<Void> completableFuture = new ManagedCompletableFuture<Void>(executor, futureExecutor);
            action = new ContextualRunnable(contextDescriptor, action, completableFuture);
            (futureExecutor == null ? executor : futureExecutor).execute(action);
            return completableFuture;
        }
    }

    /**
     * Because CompletableFuture.supplyAsync is static, this is not a true override.
     * It will be difficult for the user to invoke this method because they would need to get the class
     * of the CompletableFuture implementation and locate the static supplyAsync method on that.
     *
     * @throws UnsupportedOperationException directing the user to use the ManagedExecutor spec interface instead.
     */
    @Trivial
    public static <U> CompletableFuture<U> supplyAsync(Supplier<U> action) {
        throw new UnsupportedOperationException(Tr.formatMessage(tc, "CWWKC1156.not.supported", "ManagedExecutor.supplyAsync"));
    }

    /**
     * Alternative to CompletableFuture.supplyAsync(supplier, executor) with an implementation that switches the
     * default asynchronous execution facility to be the specified managed executor.
     *
     * @param action the supplier to invoke asynchronously.
     * @param executor the executor, typically a managed executor, that becomes the default asynchronous execution facility for the completable future.
     * @return completable future where the specified managed executor is the default asynchronous execution facility.
     */
    @Trivial // traced by caller
    public static <U> CompletableFuture<U> supplyAsync(Supplier<U> action, Executor executor) {
        rejectManagedTask(action);

        FutureRefExecutor futureExecutor = supportsAsync(executor);

        ThreadContextDescriptor contextDescriptor;
        if (action instanceof ContextualSupplier) {
            ContextualSupplier<U> s = (ContextualSupplier<U>) action;
            contextDescriptor = s.getContextDescriptor();
            action = s.getAction();
        } else if (executor instanceof WSManagedExecutorService) {
            contextDescriptor = ((WSManagedExecutorService) executor).captureThreadContext(XPROPS_SUSPEND_TRAN);
        } else {
            contextDescriptor = null;
        }

        if (JAVA8) {
            if (contextDescriptor != null)
                action = new ContextualSupplier<U>(contextDescriptor, action);
            CompletableFuture<U> completableFuture = CompletableFuture.supplyAsync(action, futureExecutor == null ? executor : futureExecutor);
            return new ManagedCompletableFuture<U>(completableFuture, executor, futureExecutor);
        } else {
            ManagedCompletableFuture<U> completableFuture = new ManagedCompletableFuture<U>(executor, futureExecutor);
            Runnable task = new ContextualSupplierAction<U>(contextDescriptor, action, completableFuture, true);
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
        rejectManagedTask(action);

        if (!(action instanceof ContextualConsumer)) {
            ThreadContextDescriptor contextDescriptor = captureThreadContext(defaultExecutor);
            if (contextDescriptor != null)
                action = new ContextualConsumer<>(contextDescriptor, action);
        }

        if (JAVA8) {
            if (other instanceof ManagedCompletableFuture)
                other = ((ManagedCompletableFuture<? extends T>) other).completableFuture;
            CompletableFuture<Void> dependentStage = completableFuture.acceptEither(other, action);
            return newInstance(dependentStage, defaultExecutor, null);
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
        rejectManagedTask(action);

        FutureRefExecutor futureExecutor = supportsAsync(executor);

        if (!(action instanceof ContextualConsumer)) {
            ThreadContextDescriptor contextDescriptor = captureThreadContext(executor);
            if (contextDescriptor != null)
                action = new ContextualConsumer<>(contextDescriptor, action);
        }

        if (JAVA8) {
            if (other instanceof ManagedCompletableFuture)
                other = ((ManagedCompletableFuture<? extends T>) other).completableFuture;
            CompletableFuture<Void> dependentStage = completableFuture.acceptEitherAsync(other, action, futureExecutor == null ? executor : futureExecutor);
            return newInstance(dependentStage, defaultExecutor, futureExecutor);
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
        rejectManagedTask(action);

        if (!(action instanceof ContextualFunction)) {
            ThreadContextDescriptor contextDescriptor = captureThreadContext(defaultExecutor);
            if (contextDescriptor != null)
                action = new ContextualFunction<>(contextDescriptor, action);
        }

        if (JAVA8) {
            if (other instanceof ManagedCompletableFuture)
                other = ((ManagedCompletableFuture<? extends T>) other).completableFuture;
            CompletableFuture<U> dependentStage = completableFuture.applyToEither(other, action);
            return newInstance(dependentStage, defaultExecutor, null);
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
        rejectManagedTask(action);

        FutureRefExecutor futureExecutor = supportsAsync(executor);

        if (!(action instanceof ContextualFunction)) {
            ThreadContextDescriptor contextDescriptor = captureThreadContext(executor);
            if (contextDescriptor != null)
                action = new ContextualFunction<>(contextDescriptor, action);
        }

        if (JAVA8) {
            if (other instanceof ManagedCompletableFuture)
                other = ((ManagedCompletableFuture<? extends T>) other).completableFuture;
            CompletableFuture<U> dependentStage = completableFuture.applyToEitherAsync(other, action, futureExecutor == null ? executor : futureExecutor);
            return newInstance(dependentStage, defaultExecutor, futureExecutor);
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
     * Captures thread context, if possible, first based on the default asynchronous execution facility,
     * otherwise based on the specified executor. If neither of these executors are a managed executor,
     * then thread context is not captured.
     *
     * @param executor executor argument that is supplied when creating a dependent stage.
     * @return captured thread context. NULL neither the default asynchronous execution facility nor the
     *         specified executor support capturing context.
     */
    private ThreadContextDescriptor captureThreadContext(Executor executor) {
        WSManagedExecutorService managedExecutor = defaultExecutor instanceof WSManagedExecutorService //
                        ? (WSManagedExecutorService) defaultExecutor //
                        : executor != defaultExecutor && executor instanceof WSManagedExecutorService //
                                        ? (WSManagedExecutorService) executor //
                                        : null;

        if (managedExecutor == null)
            return null;

        return managedExecutor.captureThreadContext(XPROPS_SUSPEND_TRAN);
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
    @Trivial
    public CompletableFuture<T> completeAsync(Supplier<? extends T> action) {
        return completeAsync(action, defaultExecutor);
    }

    /**
     * @see java.util.concurrent.CompletableFuture#completeAsync(Supplier, Executor)
     */
    public CompletableFuture<T> completeAsync(Supplier<? extends T> action, Executor executor) {
        if (JAVA8)
            throw new UnsupportedOperationException();

        if (!super.isDone()) {
            rejectManagedTask(action);

            ThreadContextDescriptor contextDescriptor;
            if (action instanceof ContextualSupplier) {
                ContextualSupplier<? extends T> s = (ContextualSupplier<? extends T>) action;
                contextDescriptor = s.getContextDescriptor();
                action = s.getAction();
            } else {
                contextDescriptor = captureThreadContext(executor);
            }

            if (!super.isDone()) {
                @SuppressWarnings({ "rawtypes", "unchecked" })
                Runnable task = new ContextualSupplierAction(contextDescriptor, action, this, false);
                if (executor instanceof WSManagedExecutorService)
                    executor = ((WSManagedExecutorService) executor).getNormalPolicyExecutor();
                executor.execute(task);
                // The existence of completeAsync means that any number of tasks could be submitted to complete a
                // single ManagedCompletableFuture instance.  We are deciding that it is not worth it to track the
                // Futures for all of these, which means there will be no way to cancel them upon seeing that the
                // CompletableFuture has completed by some other means.  This can be revisited later if it turns
                // out to be a problem.
            }
        }

        return this;
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
    public CompletableFuture<T> completeOnTimeout(T value, long timeout, TimeUnit unit) {
        if (JAVA8)
            throw new UnsupportedOperationException();
        ScheduledExecutorService scheduledExecutor = AccessController.doPrivileged(getScheduledExecutorAction);
        if (!super.isDone())
            scheduledExecutor.schedule(new Timeout(value), timeout, unit);
        return this;
    }

    /**
     * @see java.util.concurrent.CompletableFuture#copy()
     */
    public CompletableFuture<T> copy() {
        if (JAVA8)
            throw new UnsupportedOperationException();
        else
            return super.thenApply(Function.identity());
    }

    /**
     * @see java.util.concurrent.CompletableFuture#defaultExecutor()
     */
    public Executor defaultExecutor() {
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
        rejectManagedTask(action);

        if (!(action instanceof ContextualFunction)) {
            ThreadContextDescriptor contextDescriptor = captureThreadContext(defaultExecutor);
            if (contextDescriptor != null)
                action = new ContextualFunction<>(contextDescriptor, action);
        }

        if (JAVA8) {
            CompletableFuture<T> dependentStage = completableFuture.exceptionally(action);
            return newInstance(dependentStage, defaultExecutor, null);
        } else {
            return super.exceptionally(action);
        }
    }

    @Trivial
    public CompletableFuture<T> exceptionallyAsync(Function<Throwable, ? extends T> action) {
        return exceptionallyAsync(action, defaultExecutor);
    }

    public CompletableFuture<T> exceptionallyAsync(Function<Throwable, ? extends T> action, Executor executor) {
        if (super_exceptionallyAsync == null) // unavailable prior to Java 12
            throw new UnsupportedOperationException();

        rejectManagedTask(action);

        FutureRefExecutor futureExecutor = supportsAsync(executor);

        if (!(action instanceof ContextualFunction)) {
            ThreadContextDescriptor contextDescriptor = captureThreadContext(executor);
            if (contextDescriptor != null)
                action = new ContextualFunction<>(contextDescriptor, action);
        }

        futureRefLocal.set(futureExecutor);
        try {
            Executor exec = futureExecutor == null ? executor : futureExecutor;
            return (CompletableFuture<T>) super_exceptionallyAsync.invokeExact(this, action, exec);
        } catch (Error | RuntimeException x) {
            throw x;
        } catch (Throwable x) {
            throw new RuntimeException(x);
        } finally {
            futureRefLocal.remove();
        }
    }

    public CompletableFuture<T> exceptionallyCompose(Function<Throwable, ? extends CompletionStage<T>> action) {
        if (super_exceptionallyCompose == null) // unavailable prior to Java 12
            throw new UnsupportedOperationException();

        rejectManagedTask(action);

        if (!(action instanceof ContextualFunction)) {
            ThreadContextDescriptor contextDescriptor = captureThreadContext(defaultExecutor);
            if (contextDescriptor != null)
                action = new ContextualFunction<>(contextDescriptor, action);
        }

        try {
            return (CompletableFuture<T>) super_exceptionallyCompose.invokeExact(this, action);
        } catch (Error | RuntimeException x) {
            throw x;
        } catch (Throwable x) {
            throw new RuntimeException(x);
        }
    }

    @Trivial
    public CompletableFuture<T> exceptionallyComposeAsync(Function<Throwable, ? extends CompletionStage<T>> action) {
        return exceptionallyComposeAsync(action, defaultExecutor);
    }

    public CompletableFuture<T> exceptionallyComposeAsync(Function<Throwable, ? extends CompletionStage<T>> action, Executor executor) {
        if (super_exceptionallyComposeAsync == null) // unavailable prior to Java 12
            throw new UnsupportedOperationException();

        rejectManagedTask(action);

        FutureRefExecutor futureExecutor = supportsAsync(executor);

        if (!(action instanceof ContextualFunction)) {
            ThreadContextDescriptor contextDescriptor = captureThreadContext(executor);
            if (contextDescriptor != null)
                action = new ContextualFunction<>(contextDescriptor, action);
        }

        futureRefLocal.set(futureExecutor);
        try {
            Executor exec = futureExecutor == null ? executor : futureExecutor;
            return (CompletableFuture<T>) super_exceptionallyComposeAsync.invokeExact(this, action, exec);
        } catch (Error | RuntimeException x) {
            throw x;
        } catch (Throwable x) {
            throw new RuntimeException(x);
        } finally {
            futureRefLocal.remove();
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
        rejectManagedTask(action);

        if (!(action instanceof ContextualBiFunction)) {
            ThreadContextDescriptor contextDescriptor = captureThreadContext(defaultExecutor);
            if (contextDescriptor != null)
                action = new ContextualBiFunction<>(contextDescriptor, action);
        }

        if (JAVA8) {
            CompletableFuture<R> dependentStage = completableFuture.handle(action);
            return newInstance(dependentStage, defaultExecutor, null);
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
        rejectManagedTask(action);

        FutureRefExecutor futureExecutor = supportsAsync(executor);

        if (!(action instanceof ContextualBiFunction)) {
            ThreadContextDescriptor contextDescriptor = captureThreadContext(executor);
            if (contextDescriptor != null)
                action = new ContextualBiFunction<>(contextDescriptor, action);
        }

        if (JAVA8) {
            CompletableFuture<R> dependentStage = completableFuture.handleAsync(action, futureExecutor == null ? executor : futureExecutor);
            return newInstance(dependentStage, defaultExecutor, futureExecutor);
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
    public CompletionStage<T> minimalCompletionStage() {
        if (JAVA8)
            throw new UnsupportedOperationException();
        else {
            final ManagedCompletionStage<T> minimalStage = new ManagedCompletionStage<T>(defaultExecutor);
            super.whenComplete((result, failure) -> {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(this, tc, "whenComplete", result, failure);
                if (failure == null)
                    minimalStage.super_complete(result);
                else
                    minimalStage.super_completeExceptionally(failure);
            });
            return minimalStage;
        }
    }

    /**
     * @see java.util.concurrent.CompletableFuture#newIncompleteFuture()
     */
    public <R> CompletableFuture<R> newIncompleteFuture() {
        if (JAVA8)
            return new ManagedCompletableFuture<R>(new CompletableFuture<R>(), defaultExecutor, null);
        else
            return new ManagedCompletableFuture<R>(defaultExecutor, futureRefLocal.get());
    }

    /**
     * This method is only for Java SE 8.
     * It is used to create new instances of this class (or subclasses).
     * ManagedCompletionStage overrides to ensure that an instance of that class is created instead.
     *
     * @param completableFuture underlying completable future upon which this instance is backed.
     * @param managedExecutor managed executor service
     * @param futureRef reference to a policy executor Future that will be submitted if requested to run async. Otherwise null.
     * @return a new instance of this class.
     */
    @Trivial
    <R> CompletableFuture<R> newInstance(CompletableFuture<R> completableFuture, Executor managedExecutor, FutureRefExecutor futureRef) {
        return new ManagedCompletableFuture<R>(completableFuture, managedExecutor, futureRef);
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
    public CompletableFuture<T> orTimeout(long timeout, TimeUnit unit) {
        if (JAVA8)
            throw new UnsupportedOperationException();
        ScheduledExecutorService scheduledExecutor = AccessController.doPrivileged(getScheduledExecutorAction);
        if (!super.isDone())
            scheduledExecutor.schedule(new Timeout(), timeout, unit);
        return this;
    }

    /**
     * Reject ManagedTask so that we have the flexibility to decide later how to handle ManagedTaskListener and execution properties
     */
    private static final void rejectManagedTask(Object action) {
        if (action instanceof ManagedTask)
            throw new IllegalArgumentException(ManagedTask.class.getName());
    }

    /**
     * @see java.util.concurrent.CompletionStage#runAfterBoth(java.util.concurrent.CompletionStage, java.lang.Runnable)
     */
    @Override
    public CompletableFuture<Void> runAfterBoth(CompletionStage<?> other, Runnable action) {
        rejectManagedTask(action);

        if (!(action instanceof ContextualRunnable)) {
            ThreadContextDescriptor contextDescriptor = captureThreadContext(defaultExecutor);
            if (contextDescriptor != null)
                action = new ContextualRunnable(contextDescriptor, action);
        }

        if (JAVA8) {
            if (other instanceof ManagedCompletableFuture)
                other = ((ManagedCompletableFuture<?>) other).completableFuture;
            CompletableFuture<Void> dependentStage = completableFuture.runAfterBoth(other, action);
            return newInstance(dependentStage, defaultExecutor, null);
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
        rejectManagedTask(action);

        FutureRefExecutor futureExecutor = supportsAsync(executor);

        if (!(action instanceof ContextualRunnable)) {
            ThreadContextDescriptor contextDescriptor = captureThreadContext(executor);
            if (contextDescriptor != null)
                action = new ContextualRunnable(contextDescriptor, action);
        }

        if (JAVA8) {
            if (other instanceof ManagedCompletableFuture)
                other = ((ManagedCompletableFuture<?>) other).completableFuture;
            CompletableFuture<Void> dependentStage = completableFuture.runAfterBothAsync(other, action, futureExecutor == null ? executor : futureExecutor);
            return newInstance(dependentStage, defaultExecutor, futureExecutor);
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
        rejectManagedTask(action);

        if (!(action instanceof ContextualRunnable)) {
            ThreadContextDescriptor contextDescriptor = captureThreadContext(defaultExecutor);
            if (contextDescriptor != null)
                action = new ContextualRunnable(contextDescriptor, action);
        }

        if (JAVA8) {
            if (other instanceof ManagedCompletableFuture)
                other = ((ManagedCompletableFuture<?>) other).completableFuture;
            CompletableFuture<Void> dependentStage = completableFuture.runAfterEither(other, action);
            return newInstance(dependentStage, defaultExecutor, null);
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
        rejectManagedTask(action);

        FutureRefExecutor futureExecutor = supportsAsync(executor);

        if (!(action instanceof ContextualRunnable)) {
            ThreadContextDescriptor contextDescriptor = captureThreadContext(executor);
            if (contextDescriptor != null)
                action = new ContextualRunnable(contextDescriptor, action);
        }

        if (JAVA8) {
            if (other instanceof ManagedCompletableFuture)
                other = ((ManagedCompletableFuture<?>) other).completableFuture;
            CompletableFuture<Void> dependentStage = completableFuture.runAfterEitherAsync(other, action, futureExecutor == null ? executor : futureExecutor);
            return newInstance(dependentStage, defaultExecutor, futureExecutor);
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
     * Invokes cancel on the superclass,
     * or, in the case of Java SE 8, on the CompletableFuture instance that this class proxies.
     *
     * @see java.util.concurrent.Future#cancel(boolean)
     */
    final boolean super_cancel(boolean mayInterruptIfRunning) {
        if (JAVA8)
            return completableFuture.cancel(mayInterruptIfRunning);
        else
            return super.cancel(mayInterruptIfRunning);
    }

    /**
     * Invokes complete on the superclass,
     * or, in the case of Java SE 8, on the CompletableFuture instance that this class proxies.
     *
     * @see java.util.concurrent.CompletableFuture#complete(T)
     */
    final boolean super_complete(T value) {
        if (JAVA8)
            return completableFuture.complete(value);
        else
            return super.complete(value);
    }

    /**
     * Invokes completeExceptionally on the superclass,
     * or, in the case of Java SE 8, on the CompletableFuture instance that this class proxies.
     *
     * @see java.util.concurrent.CompletableFuture#completeExceptionally(java.lang.Throwable)
     */
    final boolean super_completeExceptionally(Throwable x) {
        if (JAVA8)
            return completableFuture.completeExceptionally(x);
        else
            return super.completeExceptionally(x);
    }

    /**
     * Convenience method to validate that an executor supports running asynchronously
     * and to wrap the executor, if an ExecutorService, with FutureRefExecutor.
     * This method is named supportsAsync to make failure stacks more meaningful to users.
     *
     * @param executor executor instance supplied to *Async methods.
     * @return FutureRefExecutor if an ExecutorService is supplied. Null if a valid executor is supplied.
     * @throws UnsupportedOperation if the executor is incapable of running tasks.
     */
    @Trivial
    private final static FutureRefExecutor supportsAsync(Executor executor) {
        if (executor instanceof ExecutorService)
            return new FutureRefExecutor((ExecutorService) executor); // valid
        if (executor instanceof UnusableExecutor)
            throw new UnsupportedOperationException(); // not valid for executing tasks
        return null; // valid
    }

    /**
     * @see java.util.concurrent.CompletionStage#thenAccept(java.util.function.Consumer)
     */
    @Override
    public CompletableFuture<Void> thenAccept(Consumer<? super T> action) {
        rejectManagedTask(action);

        if (!(action instanceof ContextualConsumer)) {
            ThreadContextDescriptor contextDescriptor = captureThreadContext(defaultExecutor);
            if (contextDescriptor != null)
                action = new ContextualConsumer<>(contextDescriptor, action);
        }

        if (JAVA8) {
            CompletableFuture<Void> dependentStage = completableFuture.thenAccept(action);
            return newInstance(dependentStage, defaultExecutor, null);
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
        rejectManagedTask(action);

        FutureRefExecutor futureExecutor = supportsAsync(executor);

        if (!(action instanceof ContextualConsumer)) {
            ThreadContextDescriptor contextDescriptor = captureThreadContext(executor);
            if (contextDescriptor != null)
                action = new ContextualConsumer<>(contextDescriptor, action);
        }

        if (JAVA8) {
            CompletableFuture<Void> dependentStage = completableFuture.thenAcceptAsync(action, futureExecutor == null ? executor : futureExecutor);
            return newInstance(dependentStage, defaultExecutor, futureExecutor);
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
        rejectManagedTask(action);

        if (!(action instanceof ContextualBiConsumer)) {
            ThreadContextDescriptor contextDescriptor = captureThreadContext(defaultExecutor);
            if (contextDescriptor != null)
                action = new ContextualBiConsumer<>(contextDescriptor, action);
        }

        if (JAVA8) {
            if (other instanceof ManagedCompletableFuture)
                other = ((ManagedCompletableFuture<? extends U>) other).completableFuture;
            CompletableFuture<Void> dependentStage = completableFuture.thenAcceptBoth(other, action);
            return newInstance(dependentStage, defaultExecutor, null);
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
        rejectManagedTask(action);

        FutureRefExecutor futureExecutor = supportsAsync(executor);

        if (!(action instanceof ContextualBiConsumer)) {
            ThreadContextDescriptor contextDescriptor = captureThreadContext(executor);
            if (contextDescriptor != null)
                action = new ContextualBiConsumer<>(contextDescriptor, action);
        }

        if (JAVA8) {
            if (other instanceof ManagedCompletableFuture)
                other = ((ManagedCompletableFuture<? extends U>) other).completableFuture;
            CompletableFuture<Void> dependentStage = completableFuture.thenAcceptBothAsync(other, action, futureExecutor == null ? executor : futureExecutor);
            return newInstance(dependentStage, defaultExecutor, futureExecutor);
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
        rejectManagedTask(action);

        if (!(action instanceof ContextualFunction)) {
            ThreadContextDescriptor contextDescriptor = captureThreadContext(defaultExecutor);
            if (contextDescriptor != null)
                action = new ContextualFunction<>(contextDescriptor, action);
        }

        if (JAVA8) {
            CompletableFuture<R> dependentStage = completableFuture.thenApply(action);
            return newInstance(dependentStage, defaultExecutor, null);
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
        rejectManagedTask(action);

        FutureRefExecutor futureExecutor = supportsAsync(executor);

        if (!(action instanceof ContextualFunction)) {
            ThreadContextDescriptor contextDescriptor = captureThreadContext(executor);
            if (contextDescriptor != null)
                action = new ContextualFunction<>(contextDescriptor, action);
        }

        if (JAVA8) {
            CompletableFuture<R> dependentStage = completableFuture.thenApplyAsync(action, futureExecutor == null ? executor : futureExecutor);
            return newInstance(dependentStage, defaultExecutor, futureExecutor);
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
        rejectManagedTask(action);

        if (!(action instanceof ContextualBiFunction)) {
            ThreadContextDescriptor contextDescriptor = captureThreadContext(defaultExecutor);
            if (contextDescriptor != null)
                action = new ContextualBiFunction<>(contextDescriptor, action);
        }

        if (JAVA8) {
            if (other instanceof ManagedCompletableFuture)
                other = ((ManagedCompletableFuture<? extends U>) other).completableFuture;
            CompletableFuture<R> dependentStage = completableFuture.thenCombine(other, action);
            return newInstance(dependentStage, defaultExecutor, null);
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
        rejectManagedTask(action);

        FutureRefExecutor futureExecutor = supportsAsync(executor);

        if (!(action instanceof ContextualBiFunction)) {
            ThreadContextDescriptor contextDescriptor = captureThreadContext(executor);
            if (contextDescriptor != null)
                action = new ContextualBiFunction<>(contextDescriptor, action);
        }

        if (JAVA8) {
            if (other instanceof ManagedCompletableFuture)
                other = ((ManagedCompletableFuture<? extends U>) other).completableFuture;
            CompletableFuture<R> dependentStage = completableFuture.thenCombineAsync(other, action, futureExecutor == null ? executor : futureExecutor);
            return newInstance(dependentStage, defaultExecutor, futureExecutor);
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
        rejectManagedTask(action);

        if (!(action instanceof ContextualFunction)) {
            ThreadContextDescriptor contextDescriptor = captureThreadContext(defaultExecutor);
            if (contextDescriptor != null)
                action = new ContextualFunction<>(contextDescriptor, action);
        }

        if (JAVA8) {
            CompletableFuture<U> dependentStage = completableFuture.thenCompose(action);
            return newInstance(dependentStage, defaultExecutor, null);
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
        rejectManagedTask(action);

        FutureRefExecutor futureExecutor = supportsAsync(executor);

        if (!(action instanceof ContextualFunction)) {
            ThreadContextDescriptor contextDescriptor = captureThreadContext(executor);
            if (contextDescriptor != null)
                action = new ContextualFunction<>(contextDescriptor, action);
        }

        if (JAVA8) {
            CompletableFuture<U> dependentStage = completableFuture.thenComposeAsync(action, futureExecutor == null ? executor : futureExecutor);
            return newInstance(dependentStage, defaultExecutor, futureExecutor);
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
        rejectManagedTask(action);

        if (!(action instanceof ContextualRunnable)) {
            ThreadContextDescriptor contextDescriptor = captureThreadContext(defaultExecutor);
            if (contextDescriptor != null)
                action = new ContextualRunnable(contextDescriptor, action);
        }

        if (JAVA8) {
            CompletableFuture<Void> dependentStage = completableFuture.thenRun(action);
            return newInstance(dependentStage, defaultExecutor, null);
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
        rejectManagedTask(action);

        FutureRefExecutor futureExecutor = supportsAsync(executor);

        if (!(action instanceof ContextualRunnable)) {
            ThreadContextDescriptor contextDescriptor = captureThreadContext(executor);
            if (contextDescriptor != null)
                action = new ContextualRunnable(contextDescriptor, action);
        }

        if (JAVA8) {
            CompletableFuture<Void> dependentStage = completableFuture.thenRunAsync(action, futureExecutor == null ? executor : futureExecutor);
            return newInstance(dependentStage, defaultExecutor, futureExecutor);
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
        StringBuilder s = new StringBuilder(250).append(getClass().getSimpleName()) //
                        .append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
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
        rejectManagedTask(action);

        if (!(action instanceof ContextualBiConsumer)) {
            ThreadContextDescriptor contextDescriptor = captureThreadContext(defaultExecutor);
            if (contextDescriptor != null)
                action = new ContextualBiConsumer<>(contextDescriptor, action);
        }

        if (JAVA8) {
            CompletableFuture<T> dependentStage = completableFuture.whenComplete(action);
            return newInstance(dependentStage, defaultExecutor, null);
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
        rejectManagedTask(action);

        FutureRefExecutor futureExecutor = supportsAsync(executor);

        if (!(action instanceof ContextualBiConsumer)) {
            ThreadContextDescriptor contextDescriptor = captureThreadContext(executor);
            if (contextDescriptor != null)
                action = new ContextualBiConsumer<>(contextDescriptor, action);
        }

        if (JAVA8) {
            CompletableFuture<T> dependentStage = completableFuture.whenCompleteAsync(action, futureExecutor == null ? executor : futureExecutor);
            return newInstance(dependentStage, defaultExecutor, futureExecutor);
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
     * Task that performs completion upon reaching a timeout.
     */
    @Trivial
    private class Timeout implements Runnable {
        private final Object result;

        /**
         * Constructor to complete with a TimeoutException upon timing out.
         */
        private Timeout() {
            // To indicate exceptional completion upon timeout, choose a value that the user cannot possibly specify
            this.result = Timeout.class;
        }

        /**
         * Constructor to complete with a result upon timing out.
         */
        private Timeout(T result) {
            this.result = result;
        }

        /**
         * Invoked on an executor thread to perform the completion.
         */
        @Override
        @SuppressWarnings("unchecked")
        public void run() {
            final boolean trace = TraceComponent.isAnyTracingEnabled();
            if (trace && tc.isEntryEnabled())
                Tr.entry(ManagedCompletableFuture.this, tc, "run: complete on timeout", this);

            if (ManagedCompletableFuture.super.isDone()) {
                if (trace && tc.isEntryEnabled())
                    Tr.exit(ManagedCompletableFuture.this, tc, "run: complete on timeout - skipped because done");
            } else if (result == Timeout.class) {
                boolean completed = ManagedCompletableFuture.this.completeExceptionally(new TimeoutException());
                if (trace && tc.isEntryEnabled())
                    Tr.exit(ManagedCompletableFuture.this, tc, "run: completed exceptionally on timeout? " + completed);
            } else {
                boolean completed = ManagedCompletableFuture.this.complete((T) result);
                if (trace && tc.isEntryEnabled())
                    Tr.exit(ManagedCompletableFuture.this, tc, "run: completed on timeout? " + completed);
            }
        }
    }
}