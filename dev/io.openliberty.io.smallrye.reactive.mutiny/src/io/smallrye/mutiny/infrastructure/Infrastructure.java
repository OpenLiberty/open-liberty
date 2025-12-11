/*******************************************************************************
 * Copyright (c) contributors to https://github.com/smallrye/smallrye-mutiny
 * Copyright (c) 2025 IBM Corporation and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 * Original version: https://github.com/smallrye/smallrye-mutiny/blob/2.7.0/implementation/src/main/java/io/smallrye/mutiny/infrastructure/Infrastructure.java
 *
 * Modified for Liberty to support SecurityManager
 * See "Liberty changes start/end" sections below for changes
 *******************************************************************************/

package io.smallrye.mutiny.infrastructure;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;
import static io.smallrye.mutiny.helpers.ParameterValidation.positive;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.LongConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.groups.MultiOverflowStrategy;
import io.smallrye.mutiny.subscription.UniSubscriber;
import io.smallrye.mutiny.tuples.Functions;

public class Infrastructure {

    private static final String DISABLE_CALLBACK_DECORATORS_PROP_NAME = "mutiny.disableCallBackDecorators";
    // Liberty changes start
    private static final boolean DISABLE_CALLBACK_DECORATORS = AccessController.doPrivileged((PrivilegedAction<Boolean>) () -> Boolean.getBoolean(DISABLE_CALLBACK_DECORATORS_PROP_NAME));
    // Liberty changes end

    static {
        ServiceLoader<ExecutorConfiguration> executorLoader = ServiceLoader.load(ExecutorConfiguration.class);
        Iterator<ExecutorConfiguration> iterator = executorLoader.iterator();
        if (iterator.hasNext()) {
            ExecutorConfiguration next = iterator.next();
            setDefaultExecutor(nonNull(next.getDefaultWorkerExecutor(), "executor"));
        } else {
            setDefaultExecutor();
        }

        reload();

        resetCanCallerThreadBeBlockedSupplier();
    }

    private static ScheduledExecutorService DEFAULT_SCHEDULER;

    private static Executor DEFAULT_EXECUTOR;
    private static UniInterceptor[] UNI_INTERCEPTORS;
    private static MultiInterceptor[] MULTI_INTERCEPTORS;
    private static CallbackDecorator[] CALLBACK_DECORATORS;
    private static UnaryOperator<CompletableFuture<?>> completableFutureWrapper;
    private static Consumer<Throwable> droppedExceptionHandler = PrintAndDumpThrowableConsumer.INSTANCE;
    private static BooleanSupplier canCallerThreadBeBlockedSupplier;
    private static OperatorLogger operatorLogger = PrintOperatorEventOperatorLogger.INSTANCE;

    private static int multiOverflowDefaultBufferSize = 128;

    private static int bufferSizeXs = 32;
    private static int bufferSizeS = 256;

    public static void reload() {
        clearInterceptors();
        reloadUniInterceptors();
        reloadMultiInterceptors();
        reloadCallbackDecorators();
        multiOverflowDefaultBufferSize = 128;
        bufferSizeXs = 32;
        bufferSizeS = 256;
    }

    /**
     * Configure or reset the executors.
     */
    public static void setDefaultExecutor() {
        ExecutorService scheduler = Executors.newCachedThreadPool();
        setDefaultExecutor(scheduler);
    }

    public static void setDefaultExecutor(Executor s) {
        if (s == DEFAULT_EXECUTOR) {
            return;
        }
        Executor existing = DEFAULT_EXECUTOR;
        if (existing instanceof ExecutorService) {
            ((ExecutorService) existing).shutdownNow();
        }
        if (DEFAULT_SCHEDULER != null) {
            DEFAULT_SCHEDULER.shutdownNow();
        }
        DEFAULT_EXECUTOR = s;
        DEFAULT_SCHEDULER = (s instanceof ScheduledExecutorService) ? (ScheduledExecutorService) s : new MutinyScheduler(s);
    }

    public static ScheduledExecutorService getDefaultWorkerPool() {
        return DEFAULT_SCHEDULER;
    }

    public static Executor getDefaultExecutor() {
        return DEFAULT_EXECUTOR;
    }

    public static <T> Uni<T> onUniCreation(Uni<T> instance) {
        Uni<T> current = instance;
        for (UniInterceptor itcp : UNI_INTERCEPTORS) {
            current = itcp.onUniCreation(current);
        }
        return current;
    }

    public static <T> Multi<T> onMultiCreation(Multi<T> instance) {
        Multi<T> current = instance;
        for (MultiInterceptor interceptor : MULTI_INTERCEPTORS) {
            current = interceptor.onMultiCreation(current);
        }
        return current;
    }

    public static <T> UniSubscriber<? super T> onUniSubscription(Uni<T> instance, UniSubscriber<? super T> subscriber) {
        UniSubscriber<? super T> current = subscriber;
        for (UniInterceptor interceptor : UNI_INTERCEPTORS) {
            current = interceptor.onSubscription(instance, current);
        }
        return current;
    }

    public static <T> Subscriber<? super T> onMultiSubscription(Flow.Publisher<? extends T> instance,
                                                                Subscriber<? super T> subscriber) {
        Subscriber<? super T> current = subscriber;
        for (MultiInterceptor itcp : MULTI_INTERCEPTORS) {
            current = itcp.onSubscription(instance, current);
        }
        return current;
    }

    public static <T> Supplier<T> decorate(Supplier<T> supplier) {
        Supplier<T> current = supplier;
        for (CallbackDecorator interceptor : CALLBACK_DECORATORS) {
            current = interceptor.decorate(current);
        }
        return current;
    }

    public static <T> Consumer<T> decorate(Consumer<T> consumer) {
        Consumer<T> current = consumer;
        for (CallbackDecorator interceptor : CALLBACK_DECORATORS) {
            current = interceptor.decorate(current);
        }
        return current;
    }

    public static LongConsumer decorate(LongConsumer consumer) {
        LongConsumer current = consumer;
        for (CallbackDecorator interceptor : CALLBACK_DECORATORS) {
            current = interceptor.decorate(current);
        }
        return current;
    }

    public static Runnable decorate(Runnable runnable) {
        Runnable current = runnable;
        for (CallbackDecorator interceptor : CALLBACK_DECORATORS) {
            current = interceptor.decorate(current);
        }
        return current;
    }

    public static <V> Callable<V> decorate(Callable<V> callable) {
        Callable<V> current = callable;
        for (CallbackDecorator interceptor : CALLBACK_DECORATORS) {
            current = interceptor.decorate(current);
        }
        return current;
    }

    public static <T1, T2> BiConsumer<T1, T2> decorate(BiConsumer<T1, T2> consumer) {
        BiConsumer<T1, T2> current = consumer;
        for (CallbackDecorator interceptor : CALLBACK_DECORATORS) {
            current = interceptor.decorate(current);
        }
        return current;
    }

    public static <I1, I2, I3, O> Functions.Function3<I1, I2, I3, O> decorate(Functions.Function3<I1, I2, I3, O> function) {
        Functions.Function3<I1, I2, I3, O> current = function;
        for (CallbackDecorator interceptor : CALLBACK_DECORATORS) {
            current = interceptor.decorate(current);
        }
        return current;
    }

    public static <I1, I2, I3, I4, O> Functions.Function4<I1, I2, I3, I4, O> decorate(
                                                                                      Functions.Function4<I1, I2, I3, I4, O> function) {
        Functions.Function4<I1, I2, I3, I4, O> current = function;
        for (CallbackDecorator interceptor : CALLBACK_DECORATORS) {
            current = interceptor.decorate(current);
        }
        return current;
    }

    public static <I1, I2, I3, I4, I5, O> Functions.Function5<I1, I2, I3, I4, I5, O> decorate(
                                                                                              Functions.Function5<I1, I2, I3, I4, I5, O> function) {
        Functions.Function5<I1, I2, I3, I4, I5, O> current = function;
        for (CallbackDecorator interceptor : CALLBACK_DECORATORS) {
            current = interceptor.decorate(current);
        }
        return current;
    }

    public static <I1, I2, I3, I4, I5, I6, O> Functions.Function6<I1, I2, I3, I4, I5, I6, O> decorate(
                                                                                                      Functions.Function6<I1, I2, I3, I4, I5, I6, O> function) {
        Functions.Function6<I1, I2, I3, I4, I5, I6, O> current = function;
        for (CallbackDecorator interceptor : CALLBACK_DECORATORS) {
            current = interceptor.decorate(current);
        }
        return current;
    }

    public static <I1, I2, I3, I4, I5, I6, I7, O> Functions.Function7<I1, I2, I3, I4, I5, I6, I7, O> decorate(
                                                                                                              Functions.Function7<I1, I2, I3, I4, I5, I6, I7, O> function) {
        Functions.Function7<I1, I2, I3, I4, I5, I6, I7, O> current = function;
        for (CallbackDecorator interceptor : CALLBACK_DECORATORS) {
            current = interceptor.decorate(current);
        }
        return current;
    }

    public static <I1, I2, I3, I4, I5, I6, I7, I8, O> Functions.Function8<I1, I2, I3, I4, I5, I6, I7, I8, O> decorate(
                                                                                                                      Functions.Function8<I1, I2, I3, I4, I5, I6, I7, I8, O> function) {
        Functions.Function8<I1, I2, I3, I4, I5, I6, I7, I8, O> current = function;
        for (CallbackDecorator interceptor : CALLBACK_DECORATORS) {
            current = interceptor.decorate(current);
        }
        return current;
    }

    public static <I1, I2, I3, I4, I5, I6, I7, I8, I9, O> Functions.Function9<I1, I2, I3, I4, I5, I6, I7, I8, I9, O> decorate(
                                                                                                                              Functions.Function9<I1, I2, I3, I4, I5, I6, I7, I8, I9, O> function) {
        Functions.Function9<I1, I2, I3, I4, I5, I6, I7, I8, I9, O> current = function;
        for (CallbackDecorator interceptor : CALLBACK_DECORATORS) {
            current = interceptor.decorate(current);
        }
        return current;
    }

    public static <I, O> Function<I, O> decorate(Function<I, O> function) {
        Function<I, O> current = function;
        for (CallbackDecorator interceptor : CALLBACK_DECORATORS) {
            current = interceptor.decorate(current);
        }
        return current;
    }

    public static <I1, I2, O> BiFunction<I1, I2, O> decorate(BiFunction<I1, I2, O> function) {
        BiFunction<I1, I2, O> current = function;
        for (CallbackDecorator interceptor : CALLBACK_DECORATORS) {
            current = interceptor.decorate(current);
        }
        return current;
    }

    public static <T> BinaryOperator<T> decorate(BinaryOperator<T> operator) {
        BinaryOperator<T> current = operator;
        for (CallbackDecorator interceptor : CALLBACK_DECORATORS) {
            current = interceptor.decorate(current);
        }
        return current;
    }

    public static <T1, T2, T3> Functions.TriConsumer<T1, T2, T3> decorate(
                                                                          Functions.TriConsumer<T1, T2, T3> consumer) {
        Functions.TriConsumer<T1, T2, T3> current = consumer;
        for (CallbackDecorator interceptor : CALLBACK_DECORATORS) {
            current = interceptor.decorate(current);
        }
        return current;
    }

    public static void setCompletableFutureWrapper(UnaryOperator<CompletableFuture<?>> wrapper) {
        completableFutureWrapper = wrapper;
    }

    @SuppressWarnings("unchecked")
    public static <T> CompletableFuture<T> wrapCompletableFuture(CompletableFuture<T> future) {
        UnaryOperator<CompletableFuture<?>> wrapper = completableFutureWrapper;
        return wrapper != null ? (CompletableFuture<T>) wrapper.apply(future) : future;
    }

    public static void handleDroppedException(Throwable throwable) {
        droppedExceptionHandler.accept(throwable);
    }

    /**
     * Defines a custom caller thread blocking check supplier.
     *
     * @param supplier the supplier, must not be {@code null} and must not throw an exception or it will also be lost.
     */
    public static void setCanCallerThreadBeBlockedSupplier(BooleanSupplier supplier) {
        nonNull(supplier, "supplier");
        canCallerThreadBeBlockedSupplier = supplier;
    }

    public static boolean canCallerThreadBeBlocked() {
        return canCallerThreadBeBlockedSupplier.getAsBoolean();
    }

    /**
     * Defines a custom dropped exception handler.
     *
     * @param handler the handler, must not be {@code null} and must not throw an exception or it will also be lost.
     */
    public static void setDroppedExceptionHandler(Consumer<Throwable> handler) {
        nonNull(handler, "handler");
        droppedExceptionHandler = handler;
    }

    public static void reloadUniInterceptors() {
        ServiceLoader<UniInterceptor> loader = ServiceLoader.load(UniInterceptor.class);
        List<UniInterceptor> interceptors = toInterceptorList(loader);
        UNI_INTERCEPTORS = interceptors.toArray(UNI_INTERCEPTORS);
    }

    public static void reloadMultiInterceptors() {
        ServiceLoader<MultiInterceptor> loader = ServiceLoader.load(MultiInterceptor.class);
        List<MultiInterceptor> interceptors = toInterceptorList(loader);
        MULTI_INTERCEPTORS = interceptors.toArray(MULTI_INTERCEPTORS);
    }

    public static void reloadCallbackDecorators() {
        if (!DISABLE_CALLBACK_DECORATORS) {
            ServiceLoader<CallbackDecorator> loader = ServiceLoader.load(CallbackDecorator.class);
            List<CallbackDecorator> interceptors = toInterceptorList(loader);
            CALLBACK_DECORATORS = interceptors.toArray(CALLBACK_DECORATORS);
        }
    }

    private static <T extends MutinyInterceptor> List<T> toInterceptorList(ServiceLoader<T> loader) {
        List<T> interceptors = new ArrayList<>();
        for (T item : loader) {
            interceptors.add(item);
        }
        interceptors.sort(MutinyInterceptorComparator.INSTANCE);
        return interceptors;
    }

    public static void clearInterceptors() {
        UNI_INTERCEPTORS = new UniInterceptor[0];
        MULTI_INTERCEPTORS = new MultiInterceptor[0];
        CALLBACK_DECORATORS = new CallbackDecorator[0];
    }

    // For testing purpose only
    public static void resetDroppedExceptionHandler() {
        droppedExceptionHandler = PrintAndDumpThrowableConsumer.INSTANCE;
    }

    // For testing purpose only
    public static void resetCanCallerThreadBeBlockedSupplier() {
        canCallerThreadBeBlockedSupplier = AlwaysTrueBooleanSupplier.INSTANCE;
    }

    private Infrastructure() {
        // Avoid direct instantiation.
    }

    public static BooleanSupplier decorate(BooleanSupplier supplier) {
        BooleanSupplier current = supplier;
        for (CallbackDecorator interceptor : CALLBACK_DECORATORS) {
            current = interceptor.decorate(current);
        }
        return current;
    }

    public static <T> Predicate<T> decorate(Predicate<T> predicate) {
        Predicate<T> current = predicate;
        for (CallbackDecorator interceptor : CALLBACK_DECORATORS) {
            current = interceptor.decorate(current);
        }
        return current;
    }

    /**
     * Log from an operator.
     * <p>
     * This method should never be called directly but only from {@link Multi#log(String)} and {@link Uni#log(String)}.
     *
     * @param identifier the event identifier
     * @param event      the event as a string
     * @param value      the value, if any or {@code null}
     * @param failure    the failure, if any or {@code null}
     */
    public static void logFromOperator(String identifier, String event, Object value, Throwable failure) {
        operatorLogger.log(identifier, event, value, failure);
    }

    /**
     * Defines operator logging behavior for {@link Multi#log(String)} and {@link Uni#log(String)}.
     *
     * @param operatorLogger the new operator logger
     */
    public static void setOperatorLogger(OperatorLogger operatorLogger) {
        Infrastructure.operatorLogger = nonNull(operatorLogger, "operatorLogger");
    }

    // For testing purpose only
    public static void resetOperatorLogger() {
        Infrastructure.operatorLogger = PrintOperatorEventOperatorLogger.INSTANCE;
    }

    /**
     * Get the default overflow buffer size fpr {@link MultiOverflowStrategy#buffer()}.
     *
     * @return the default value
     */
    public static int getMultiOverflowDefaultBufferSize() {
        return multiOverflowDefaultBufferSize;
    }

    /**
     * Sets the default overflow buffer size fpr {@link MultiOverflowStrategy#buffer()}.
     *
     * @param size the buffer size, must be strictly positive
     */
    public static void setMultiOverflowDefaultBufferSize(int size) {
        multiOverflowDefaultBufferSize = positive(size, "size");
    }

    // Liberty changes start
    private static final PrivilegedAction<String> GET_BUFFER_SIZE_XS = new PrivilegedAction<String>() {
        @Override
        public String run() {
            return System.getProperty("mutiny.buffer-size.xs");
        }
    };
    // Liberty changes end

    /**
     * Get the xs buffer size (for internal usage).
     *
     * @return the buffer size
     */
    public static int getBufferSizeXs() {
        // Liberty changes start
        String propVal;
        if (System.getSecurityManager() != null) {
            propVal = AccessController.doPrivileged(GET_BUFFER_SIZE_XS);
        } else {
            propVal = GET_BUFFER_SIZE_XS.run();
        }
        // Liberty changes end
        if (propVal != null) {
            return Math.max(8, Integer.parseInt(propVal));
        } else {
            return bufferSizeXs;
        }
    }

    /**
     * Set the xs buffer size (for internal usage).
     *
     * @param size the buffer size
     */
    public static void setBufferSizeXs(int size) {
        bufferSizeXs = positive(size, "size");
    }

    // Liberty changes start
    private static final PrivilegedAction<String> GET_BUFFER_SIZE_S = new PrivilegedAction<String>() {
        @Override
        public String run() {
            return System.getProperty("mutiny.buffer-size.s");
        }
    };
    // Liberty changes end

    /**
     * Get the s buffer size (for internal usage).
     *
     * @return the buffer size
     */
    public static int getBufferSizeS() {
        // Liberty changes start
        String propVal;
        if (System.getSecurityManager() != null) {
            propVal = AccessController.doPrivileged(GET_BUFFER_SIZE_S);
        } else {
            propVal = GET_BUFFER_SIZE_S.run();
        }
        // Liberty changes end
        if (propVal != null) {
            return Math.max(16, Integer.parseInt(propVal));
        } else {
            return bufferSizeS;
        }
    }

    /**
     * Set the xs buffer size (for internal usage).
     *
     * @param size the buffer size
     */
    public static void setBufferSizeS(int size) {
        bufferSizeS = positive(size, "size");
    }

    /**
     * An operator logger for {@link Multi#log(String)} and {@link Uni#log(String)}.
     */
    @FunctionalInterface
    public interface OperatorLogger {

        /**
         * Actual logging behavior.
         *
         * @param identifier the event identifier
         * @param event      the event as a string
         * @param value      the value, if any or {@code null}
         * @param failure    the failure, if any or {@code null}
         */
        void log(String identifier, String event, Object value, Throwable failure);
    }

    private static class MutinyInterceptorComparator implements Comparator<MutinyInterceptor> {

        private static final MutinyInterceptorComparator INSTANCE = new MutinyInterceptorComparator();

        @Override
        public int compare(MutinyInterceptor o1, MutinyInterceptor o2) {
            return Integer.compare(o1.ordinal(), o2.ordinal());
        }
    }

    private static class AlwaysTrueBooleanSupplier implements BooleanSupplier {

        private static final AlwaysTrueBooleanSupplier INSTANCE = new AlwaysTrueBooleanSupplier();

        @Override
        public boolean getAsBoolean() {
            return true;
        }
    }

    private static class PrintAndDumpThrowableConsumer implements Consumer<Throwable> {

        private static final PrintAndDumpThrowableConsumer INSTANCE = new PrintAndDumpThrowableConsumer();

        @Override
        public void accept(Throwable throwable) {
            System.err.println("[-- Mutiny had to drop the following exception --]");
            StackTraceElement element = Thread.currentThread().getStackTrace()[3];
            System.err.println("Exception received by: " + element.toString());
            throwable.printStackTrace();
            System.err.println("[------------------------------------------------]");
        }
    }

    private static class PrintOperatorEventOperatorLogger implements OperatorLogger {

        private static final PrintOperatorEventOperatorLogger INSTANCE = new PrintOperatorEventOperatorLogger();

        @Override
        public void log(String identifier, String event, Object value, Throwable failure) {
            String message = "[--> " + identifier + " | " + event;
            if (failure == null) {
                if (value != null) {
                    message = message + "(" + value + ")";
                } else {
                    message = message + "()";
                }
            } else {
                message = message + "(" + failure.getClass().getName() + "(\"" + failure.getMessage() + "\"))";
            }
            System.out.println(message);
        }
    }

}
