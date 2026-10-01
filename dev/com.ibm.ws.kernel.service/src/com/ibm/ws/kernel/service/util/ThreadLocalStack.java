/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
package com.ibm.ws.kernel.service.util;

/**
 * A per-thread stack of values.
 *
 * <p>Typical usage with try-with-resources:
 * <pre>{@code
 * ThreadLocalStack<MyData> stack = ThreadLocalStack.newStack();
 *
 * try (ThreadLocalStack.Popper p = stack.push(myData)) {
 *     // stack.get() == myData
 * }
 * // stack.get() == null (back to default)
 * }</pre>
 *
 * @param <T> the type of value held on the stack
 */
public class ThreadLocalStack<T> {

    /**
     * Returned by {@link ThreadLocalStack#push}.  Closing this object pops
     * the value that was pushed, restoring the previous stack entry.
     * Extends {@link AutoCloseable} but {@link #close()} never throws a
     * checked exception.
     */
    public final class Popper implements AutoCloseable {
        private final Frame<T> frame;

        private Popper(Frame<T> frame) {
            this.frame = frame;
        }

        /**
         * Pops the value that was pushed when this {@code Popper} was created,
         * restoring the previous stack entry.  Safe to call in a finally block
         * or try-with-resources; never throws a checked exception.
         *
         * @throws IllegalStateException if this frame is no longer the top of
         *                               the stack (indicates a push/pop mismatch)
         */
        @Override
        public void close() {
            Frame<T> current = top.get();
            if (current != frame) {
                throw new IllegalStateException("ThreadLocalStack: close() called out of order — frame is not the current top");
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
    // ThreadLocal holding the top-of-stack for each thread
    // -----------------------------------------------------------------------

    private final ThreadLocal<Frame<T>> top;

    private ThreadLocalStack(T defaultValue) {
        Frame<T> initial = new Frame<>(defaultValue);
        top = ThreadLocal.withInitial(() -> initial);
    }

    /**
     * Creates a new {@code ThreadLocalStack} whose initial frame holds the
     * given default value.  {@link #get()} returns this value when nothing
     * has been pushed.
     *
     * @param <T>          the type of value held on the stack
     * @param defaultValue the value returned by {@link #get()} before any push
     * @return a new {@code ThreadLocalStack}
     */
    public static <T> ThreadLocalStack<T> withDefault(T defaultValue) {
        return new ThreadLocalStack<>(defaultValue);
    }

    /**
     * Creates a new {@code ThreadLocalStack} whose initial frame holds
     * {@code null}.  Equivalent to {@code withDefault(null)}.
     *
     * @param <T> the type of value held on the stack
     * @return a new {@code ThreadLocalStack}
     */
    public static <T> ThreadLocalStack<T> newStack() {
        return new ThreadLocalStack<>(null);
    }

    /**
     * Pushes {@code value} onto this thread's stack.
     *
     * @param value the value to push; may be {@code null}
     * @return a {@link Popper} that, when closed, pops this value off the stack
     */
    public Popper push(T value) {
        Frame<T> frame = new Frame<>(value, top.get());
        top.set(frame);
        return new Popper(frame);
    }

    /**
     * Returns the value at the top of this thread's stack, or the default
     * value if nothing has been pushed.
     *
     * @return the current top value
     */
    public T get() {
        return top.get().data;
    }
}
