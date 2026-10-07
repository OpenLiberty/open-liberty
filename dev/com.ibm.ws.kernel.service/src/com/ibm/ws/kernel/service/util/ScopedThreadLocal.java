/* *****************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 * *****************************************************************************/
package com.ibm.ws.kernel.service.util;

/**
 * A per-thread scoped value holder.
 *
 * <p>Typical usage with try-with-resources:
 * <pre>{@code
 * ScopedThreadLocal<MyData> context = ScopedThreadLocal.create();
 *
 * try (ScopedThreadLocal<MyData>.Scope s = context.with(myData)) {
 *     // context.get() == myData
 * }
 * // context.get() == null (restored to default)
 * }</pre>
 *
 * @param <T> the type of value held in scope
 */
public class ScopedThreadLocal<T> {

    /**
     * Returned by {@link ScopedThreadLocal#with}. Closing this object exits
     * the scope, restoring the previous value.
     * Extends {@link AutoCloseable} but {@link #close()} never throws a
     * checked exception.
     */
    public final class Scope implements AutoCloseable {
        private final Frame<T> frame;

        private Scope(Frame<T> frame) {
            this.frame = frame;
        }

        /**
         * Closes this scope, restoring the previous value for this thread.
         * Safe to call in a finally block or try-with-resources; never throws
         * a checked exception.
         *
         * @throws IllegalStateException if this scope is not the currently active
         *                               scope (indicates an out-of-order close)
         */
        @Override
        public void close() {
            Frame<T> current = top.get();
            if (current != frame) {
                throw new IllegalStateException("ScopedThreadLocal: close() called out of order — scope is not currently active");
            }
            top.set(current.prev);
        }
    }

    // -----------------------------------------------------------------------
    // Internal linked-list node
    // -----------------------------------------------------------------------

    private static final class Frame<T> {
        final T data;
        final Frame<T> prev;

        /** Regular frame. */
        Frame(T data, Frame<T> prev) {
            this.data = data;
            this.prev = prev;
        }

        /** Sentinel initial frame — prev points to itself. */
        Frame(T defaultValue) {
            this.data = defaultValue;
            this.prev = this;
        }
    }

    // -----------------------------------------------------------------------
    // ThreadLocal holding the active frame for each thread
    // -----------------------------------------------------------------------

    private final ThreadLocal<Frame<T>> top;

    private ScopedThreadLocal(T defaultValue) {
        Frame<T> initial = new Frame<>(defaultValue);
        top = ThreadLocal.withInitial(() -> initial);
    }

    /**
     * Creates a new {@code ScopedThreadLocal} whose initial default value is {@code null}.
     *
     * @param <T> the type of value held in scope
     * @return a new {@code ScopedThreadLocal}
     */
    public static <T> ScopedThreadLocal<T> create() {
        return new ScopedThreadLocal<>(null);
    }

    /**
     * Creates a new {@code ScopedThreadLocal} with the given default value.
     * {@link #get()} returns this value when outside of any active scope.
     *
     * @param <T>          the type of value held in scope
     * @param defaultValue the value returned by {@link #get()} when outside of any scope
     * @return a new {@code ScopedThreadLocal}
     */
    public static <T> ScopedThreadLocal<T> withDefault(T defaultValue) {
        return new ScopedThreadLocal<>(defaultValue);
    }

    /**
     * Enters a new scope with {@code value} bound for the current thread.
     *
     * @param value the value to bind in this scope; may be {@code null}
     * @return a {@link Scope} that, when closed, restores the previous value
     */
    public Scope with(T value) {
        Frame<T> frame = new Frame<>(value, top.get());
        top.set(frame);
        return new Scope(frame);
    }

    /**
     * Returns the value for the currently active scope on this thread, or the default
     * value if outside any scope.
     *
     * @return the current scoped value
     */
    public T get() {
        return top.get().data;
    }
}
