/*
 * Copyright 2026 IBM Corporation and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.ws.kernel.service.util;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Logger;

import static java.lang.Thread.currentThread;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;

/**
 * A thread-safe holder for a lazily-initialized, effectively-final reference.
 * <p>
 * Once the value has been set (successfully or to permanent failure) it never
 * changes: every caller receives the same outcome on every call to {@link #get()}.
 * The expensive work needed to compute the value is deferred until the first call
 * to {@link #get()}; once initialization is complete, subsequent calls pay only
 * the cost of a single volatile read.
 * <p>
 * References created with {@link #of} capture initialization failures permanently:
 * every subsequent call to {@link #get()} re-throws a new {@link InitializationException}
 * wrapping the original cause.  References created with {@link #ofRetryable} reset after
 * a failure so that the next call will retry initialization.
 * <p>
 * <b>Implementation note:</b> Coordination between threads uses atomic
 * function-pointer swapping rather than locking:
 * <ol>
 *   <li>The atomic function pointer initially points to an initialization function.</li>
 *   <li>The first thread to call {@link #get()} atomically swaps it to a {@link Waiter}.</li>
 *   <li>If the swap succeeds, that thread performs initialization.</li>
 *   <li>If the swap fails, the calling thread delegates to whatever the function pointer
 *       now holds — either another {@link Waiter} (which will block until the latch is
 *       released) or an already-completed getter.</li>
 *   <li>After initialization, the pointer is swapped to a simple getter that returns
 *       the cached value (or permanently re-throws the captured error).</li>
 *   <li>The latch is released, allowing any waiting threads to proceed.</li>
 * </ol>
 *
 * @param <T> the type of the lazily initialized value
 */
public class LazyReference<T> implements Supplier<T> {
    private static final Logger LOGGER = Logger.getLogger(LazyReference.class.getName());

    /**
     * Thrown when lazy initialization fails on a reference created with {@link #of}
     * (i.e. retry is not enabled).
     * Every call to {@link #get()} after such a failure throws a fresh instance of
     * this exception, each with its own stack trace, all wrapping the same original cause.
     */
    public static class InitializationException extends RuntimeException {
        private InitializationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Thrown when a thread is interrupted while waiting for another thread to
     * complete initialization.  The calling thread's interrupt flag is restored
     * before this exception is thrown.
     */
    public static class InitializationInterruptedException extends RuntimeException {
        private InitializationInterruptedException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Thrown when the initializer calls {@link #get()} on the same
     * {@code LazyReference} instance and no user-supplied placeholder generator
     * was provided.  This is always a permanent error.
     */
    public static class RecursiveInitializationException extends IllegalStateException {
        private RecursiveInitializationException(String threadName) {
            super("Recursive initialization detected: Thread " + threadName +
                  " attempted to call get() while already initializing");
        }
    }

    // -----------------------------------------------------------------------
    // Public functional interfaces
    // -----------------------------------------------------------------------

    /**
     * Computes the lazily-initialized value.
     * <p>
     * Implementations should be free of observable side-effects beyond producing
     * the value, since the framework guarantees they are called at most once.
     *
     * @param <T> the type of the value to produce
     */
    @FunctionalInterface public interface Init<T> extends Supplier<T> {}

    /**
     * Computes the lazily-initialized value, with access to the stand-in that
     * was returned to any recursive caller during initialization.
     * <p>
     * The {@code Supplier<T>} argument returns the {@link StandIn} instance handed
     * to any recursive {@link #get()} call on the same reference, or {@code null}
     * if no recursive call occurred.  Use this to fix up back-references after the
     * real value has been constructed.
     *
     * @param <T> the type of the value to produce
     * @see StandIn
     */
    @FunctionalInterface public interface InitWith<T> extends Function<Supplier<T>, T> {}

    /**
     * Generates the stand-in value returned to recursive callers when the
     * initializer calls {@link #get()} on the same {@code LazyReference} before
     * initialization is complete.
     * <p>
     * The stand-in is generated at most once per initialization attempt; the same
     * instance is returned to every recursive caller and cleared after
     * initialization completes.
     *
     * @param <T> the type of the stand-in value
     */
    @FunctionalInterface public interface StandIn<T> extends Supplier<T> {}

    /**
     * Marker interface for the getter function to enable instanceof checks
     * in {@link #isCompleted()}.
     */
    private interface Getter<T> extends Supplier<T> {
    }

    /**
     * Installed as the function pointer while initialization is in progress.
     * Serves two roles:
     * <ul>
     *   <li>For non-initializing threads: blocks on the latch until initialization
     *       completes, then delegates to the updated function pointer.</li>
     *   <li>For the initializing thread on a recursive {@link #get()} call: returns a
     *       placeholder value generated by the user-supplied {@code genPlaceholder}
     *       supplier, or throws {@link RecursiveInitializationException} if no
     *       user-supplied placeholder generator was provided.</li>
     * </ul>
     */
    private final class Waiter implements Supplier<T> {
        public final CountDownLatch latch = new CountDownLatch(1);
        private final Thread initializingThread = currentThread();
        private T placeholder;

        /**
         * Returns the value, blocking if necessary until initialization completes.
         * <p>
         * If called by the initializing thread (a recursive {@link #get()} call),
         * returns a placeholder value via {@code genPlaceholder} rather than
         * blocking.  The placeholder is generated at most once and reused on
         * subsequent recursive calls.
         * <p>
         * After the latch is released, delegates to {@code functionPointer.get().get()},
         * which may return the initialized value or throw if initialization failed
         * permanently.
         *
         * @return the initialized value, or a placeholder for recursive calls from
         *         the initializing thread
         * @throws RecursiveInitializationException if called recursively and no
         *         user-supplied placeholder generator was provided
         * @throws InitializationInterruptedException if interrupted while waiting
         */
        @Override
        public T get() {
            final Thread currentThread = currentThread();
            // Detect recursive call from the initializing thread
            if (initializingThread == currentThread) {
                if (null == placeholder) placeholder = genPlaceholder.get();
                return placeholder;
            }

            LOGGER.fine(() -> "Thread " + currentThread.getName() + " waiting for initialization");
            try {
                latch.await();
            } catch (InterruptedException e) {
                currentThread.interrupt();
                throw new InitializationInterruptedException("Interrupted while waiting for initialization", e);
            }
            LOGGER.fine(() -> "Thread " + currentThread.getName() + " resuming after initialization");
            return functionPointer.get().get();
        }

        /**
         * Returns the placeholder generated for a recursive {@link #get()} call,
         * or {@code null} if no recursive call has occurred yet (or if the
         * user-supplied placeholder generator itself returned {@code null}).
         * <p>
         * Passed as a {@code Supplier<T>} method reference to the initializer
         * function so that the initializer can inspect the placeholder that was
         * handed back to any recursive caller.
         */
        public T getPlaceholder() {
            return placeholder;
        }

        /**
         * Nulls the placeholder field.  Called in the {@code finally} block of
         * {@link #performInitialization} so that the placeholder cannot be
         * retrieved after initialization has completed.
         */
        void clearPlaceholder() { placeholder = null; }
    }

    /**
     * Atomic reference to the current function pointer.
     * Transitions: initializationFunctionRef → Waiter → Getter (success or permanent error),
     * or initializationFunctionRef → Waiter → initializationFunctionRef (retry on failure).
     */
    private final AtomicReference<Supplier<T>> functionPointer;

    /**
     * Computes the lazily initialized value.  Receives a {@code Supplier<T>} that
     * returns any placeholder generated for a recursive {@link #get()} call (or
     * {@code null} if no recursive call has occurred).
     */
    private final Function<Supplier<T>,T> initializer;

    /**
     * Generates the placeholder value returned to the initializing thread on a
     * recursive {@link #get()} call.  Never {@code null}: when no user-supplied
     * generator is provided this field is set to {@link #throwRecursiveException},
     * which throws {@link RecursiveInitializationException} instead.
     */
    private final Supplier<T> genPlaceholder;

    /**
     * Whether to allow retry on initialization failure.
     */
    private final boolean allowRetry;

    /**
     * Stable reference to {@link #initializationFunction} used as the CAS expected
     * value, so that identity comparison reliably detects the "not yet initialized"
     * state.
     */
    private final Supplier<T> initializationFunctionRef = this::initializationFunction;

    // -----------------------------------------------------------------------
    // Static factory methods
    // -----------------------------------------------------------------------

    /**
     * Returns a new lazy reference with retry disabled and no placeholder support.
     *
     * @param <T>         the type of the lazily initialized value
     * @param initializer the supplier that will compute the value on first access
     * @return a new lazy reference
     */
    public static <T> LazyReference<T> of(Init<T> initializer) {
        return new LazyReference<>(
                (p) -> requireNonNull(initializer, "initializer must not be null").get(),
                null, false);
    }

    /**
     * Returns a new lazy reference with placeholder support and retry disabled.
     *
     * @param <T>            the type of the lazily initialized value
     * @param initializer    the {@link Init} that will compute the value on first access
     * @param genPlaceholder {@link StandIn} that generates a placeholder value when a
     *                       recursive {@link #get()} call is detected during
     *                       initialization; if {@code null}, recursive calls will
     *                       throw {@link RecursiveInitializationException}
     * @return a new lazy reference
     */
    public static <T> LazyReference<T> of(Init<T> initializer, StandIn<T> genPlaceholder) {
        return new LazyReference<>(
                (p) -> requireNonNull(initializer, "initializer must not be null").get(),
                genPlaceholder, false);
    }

    /**
     * Returns a new lazy reference with placeholder support, retry disabled, and
     * inspector access to the placeholder.
     * <p>
     * Use this factory when the initializer needs access to the placeholder
     * that was returned to any recursive caller — for example, to wire up a
     * back-reference after initialization.
     *
     * @param <T>            the type of the lazily initialized value
     * @param initializer    the {@link InitWith} that will compute the value on first
     *                       access; receives a {@code Supplier<T>} that returns any
     *                       placeholder generated for a recursive {@link #get()} call
     *                       (or {@code null} if no recursive call occurred)
     * @param genPlaceholder {@link StandIn} that generates a placeholder value when a
     *                       recursive {@link #get()} call is detected during
     *                       initialization; if {@code null}, recursive calls will
     *                       throw {@link RecursiveInitializationException}
     * @return a new lazy reference
     */
    public static <T> LazyReference<T> of(InitWith<T> initializer, StandIn<T> genPlaceholder) {
        return new LazyReference<>(initializer, genPlaceholder, false);
    }

    /**
     * Returns a new lazy reference that retries initialization on failure, with no
     * placeholder support.
     * <p>
     * Use this factory for transient failures such as a remote service that may be
     * temporarily unavailable.  After a failure the reference resets so that the
     * next call to {@link #get()} will retry.
     *
     * @param <T>         the type of the lazily initialized value
     * @param initializer the {@link Init} that will compute the value on first access
     * @return a new retryable lazy reference
     */
    public static <T> LazyReference<T> ofRetryable(Init<T> initializer) {
        return new LazyReference<>(
                (p) -> requireNonNull(initializer, "initializer must not be null").get(),
                null, true);
    }

    /**
     * Returns a new lazy reference that retries initialization on failure, with
     * placeholder support.
     *
     * @param <T>            the type of the lazily initialized value
     * @param initializer    the {@link Init} that will compute the value on first access
     * @param genPlaceholder {@link StandIn} that generates a placeholder value when a
     *                       recursive {@link #get()} call is detected during
     *                       initialization; if {@code null}, recursive calls will
     *                       throw {@link RecursiveInitializationException}
     * @return a new retryable lazy reference
     */
    public static <T> LazyReference<T> ofRetryable(Init<T> initializer, StandIn<T> genPlaceholder) {
        return new LazyReference<>(
                (p) -> requireNonNull(initializer, "initializer must not be null").get(),
                genPlaceholder, true);
    }

    /**
     * Returns a new lazy reference that retries initialization on failure, with
     * placeholder support and inspector access to the placeholder.
     * <p>
     * Use this factory when the initializer needs access to the placeholder
     * that was returned to any recursive caller — for example, to wire up a
     * back-reference after initialization.
     *
     * @param <T>            the type of the lazily initialized value
     * @param initializer    the {@link InitWith} that will compute the value on first
     *                       access; receives a {@code Supplier<T>} that returns any
     *                       placeholder generated for a recursive {@link #get()} call
     *                       (or {@code null} if no recursive call occurred)
     * @param genPlaceholder {@link StandIn} that generates a placeholder value when a
     *                       recursive {@link #get()} call is detected during
     *                       initialization; if {@code null}, recursive calls will
     *                       throw {@link RecursiveInitializationException}
     * @return a new retryable lazy reference
     */
    public static <T> LazyReference<T> ofRetryable(InitWith<T> initializer, StandIn<T> genPlaceholder) {
        return new LazyReference<>(initializer, genPlaceholder, true);
    }

    // -----------------------------------------------------------------------
    // Single private constructor — all factory methods converge here
    // -----------------------------------------------------------------------

    private LazyReference(Function<Supplier<T>,T> initializer, Supplier<T> genPlaceholder, boolean allowRetry) {
        this.initializer = requireNonNull(initializer, "initializer must not be null");
        this.genPlaceholder = ofNullable(genPlaceholder).orElse(this::throwRecursiveException);
        this.allowRetry = allowRetry;
        this.functionPointer = new AtomicReference<>(initializationFunctionRef);
    }

    private T throwRecursiveException() {
        throw new RecursiveInitializationException(currentThread().getName());
    }

    /**
     * Returns the value, initializing it on the first call.
     * <p>
     * Initialization is performed at most once across all threads.  Concurrent
     * callers that arrive before initialization completes will block until it does,
     * then receive the same outcome as the initializing thread.
     * <p>
     * If the initializer calls {@link #get()} recursively on this instance, the
     * behaviour depends on whether a placeholder generator was supplied:
     * <ul>
     *   <li>If a {@code genPlaceholder} supplier was provided, the recursive call
     *       returns the generated placeholder instead of blocking or throwing.</li>
     *   <li>If no {@code genPlaceholder} was provided, the recursive call throws
     *       {@link IllegalStateException} (caused by
     *       {@link RecursiveInitializationException}), which is then treated as a
     *       permanent initialization failure.</li>
     * </ul>
     *
     * @return the initialized value
     * @throws InitializationException            on every call if initialization failed
     *                                            permanently (i.e. reference was created
     *                                            with {@link #of}, not {@link #ofRetryable})
     * @throws InitializationInterruptedException if this thread was interrupted while
     *                                            waiting for another thread to complete
     *                                            initialization
     * @throws IllegalStateException              if recursive initialization was detected
     *                                            and no placeholder generator was provided
     */
    public T get() {
        return functionPointer.get().get();
    }

    /**
     * Attempts to become the initializing thread by atomically swapping the function
     * pointer from {@link #initializationFunctionRef} to a new {@link Waiter}.
     * <p>
     * If the CAS succeeds, this thread calls {@link #performInitialization}.
     * If the CAS fails, the function pointer has already advanced beyond the
     * initialization state, so the method delegates to whatever it now holds.
     *
     * @return the initialized value
     */
    private T initializationFunction() {
        LOGGER.fine(() -> "Thread " + currentThread().getName() + " attempting to initialize");

        // Check if we're still in initialization state before allocating Waiter
        if (functionPointer.get() != initializationFunctionRef) {
            LOGGER.fine(() -> "Thread " + currentThread().getName() + " detected initialization already in progress, reinvoking");
            return functionPointer.get().get();
        }

        Waiter waiter = new Waiter();

        boolean swapSucceeded = functionPointer.compareAndSet(initializationFunctionRef, waiter);

        if (swapSucceeded) {
            LOGGER.fine(() -> "Thread " + currentThread().getName() + " won initialization race");
            return performInitialization(waiter);
        } else {
            LOGGER.fine(() -> "Thread " + currentThread().getName() + " lost initialization race, reinvoking");
            return functionPointer.get().get();
        }
    }

    /**
     * Invokes the initializer and permanently records the outcome in the function
     * pointer, then releases the latch so that waiting threads can proceed.
     * <p>
     * On success the function pointer is set to a simple closure that returns the
     * computed value on every future call.
     * <p>
     * On failure the behaviour depends on {@code allowRetry}:
     * <ul>
     *   <li>If {@code allowRetry} is {@code true}, the function pointer is reset to
     *       {@link #initializationFunctionRef} so that the next call to {@link #get()}
     *       will retry.  The exception propagates to the caller as-is.</li>
     *   <li>If {@code allowRetry} is {@code false}, the original throwable is captured
     *       and the function pointer is set to a closure that wraps it in a new
     *       {@link InitializationException} on every future call, preserving an
     *       accurate per-caller stack trace while recording the original cause.
     *       The current caller also receives such a wrapped exception.</li>
     * </ul>
     * <p>
     * If a {@link RecursiveInitializationException} is caught it is always treated
     * as a permanent error regardless of {@code allowRetry}: the function pointer is
     * set to a closure that throws {@link IllegalStateException} on all future calls,
     * and an {@link IllegalStateException} is thrown to the current caller.
     * <p>
     * The waiter latch is always counted down in the {@code finally} block so that
     * threads blocked in {@link Waiter#get()} are never left waiting indefinitely.
     *
     * @param waiter the waiter whose latch coordinates threads blocked on initialization
     * @return the initialized value
     */
    private T performInitialization(Waiter waiter) {
        try {
            LOGGER.fine(() -> "Thread " + currentThread().getName() + " performing initialization");

            T result = initializer.apply(waiter::getPlaceholder);
            Getter<T> getter = () -> result;
            functionPointer.set(getter);

            LOGGER.fine(() -> "Thread " + currentThread().getName() + " completed initialization");
            return result;
        } catch (RecursiveInitializationException e) {
            // Recursive initialization is a programming error - set permanent error state
            LOGGER.warning(() -> "Thread " + currentThread().getName() + " detected recursive initialization");
            // Wrap in IllegalStateException to capture each caller's stack trace
            Getter<T> errorGetter = () -> {
                throw new IllegalStateException("Initialization failed (elsewhere) due to recursive call", e);
            };
            functionPointer.set(errorGetter);
            throw new IllegalStateException("Initialization failed due to recursive call", e);
        } catch (Throwable e) {
            if (allowRetry) {
                LOGGER.warning(() -> "Thread " + currentThread().getName() + " failed initialization, resetting for retry");
                functionPointer.set(initializationFunctionRef);
                throw e;
            } else {
                LOGGER.warning(() -> "Thread " + currentThread().getName() + " failed initialization, setting permanent error state");
                // Store the original cause and create new exception on each call for accurate stack traces
                Getter<T> errorGetter = () -> {
                    throw new InitializationException("Initialization failed (elsewhere) and retry is not allowed", e);
                };
                functionPointer.set(errorGetter);
                throw new InitializationException("Initialization failed and retry is not allowed", e);
            }
        } finally {
            waiter.clearPlaceholder();
            waiter.latch.countDown();
            LOGGER.fine(() -> "Thread " + currentThread().getName() + " released initialization latch");
        }
    }

    /**
     * @return {@code true} if initialization has reached a terminal state — either
     *         successful completion or permanent failure (created with {@link #of});
     *         {@code false} if initialization has not yet been attempted or failed
     *         on a reference created with {@link #ofRetryable}
     */
    public boolean isCompleted() {
        return functionPointer.get() instanceof Getter;
    }
}
