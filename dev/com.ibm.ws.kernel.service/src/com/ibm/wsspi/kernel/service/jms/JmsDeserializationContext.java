/*******************************************************************************
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
 *******************************************************************************/
package com.ibm.wsspi.kernel.service.jms;

import java.util.function.Predicate;
import java.util.Optional;

/**
 * Context singleton for installing a per-thread {@link Predicate}
 * during JMS {@code ObjectMessage} deserialization.
 *
 * <p>Idiomatic usage via try-with-resources:
 * <pre>
 * try (JmsDeserializationContext.FilterHandle h =
 *         JmsDeserializationContext.setFilter(clazz -&gt; allowed(clazz))) {
 *     return objectMessage.getObject();
 * }
 * </pre>
 */
public enum JmsDeserializationContext {
    ;

    private static final ThreadLocal<Predicate<Class<?>> > THREAD_FILTER = new ThreadLocal<>();

    /**
     * A handle returned by {@link #setFilter(Predicate<Class<?>>)}.
     * Closing it removes the filter from the current thread's {@link ThreadLocal}.
     */
    public interface FilterHandle extends AutoCloseable {
        @Override
        void close();
    }

    /**
     * Installs {@code filter} as the deserialization filter for the current thread.
     *
     * <p>The filter is stored in a {@link ThreadLocal}. The returned
     * {@link FilterHandle} must be closed (via try-with-resources or an explicit
     * {@code close()} call) to remove the filter from the thread.
     * The close call must be made on the same thread that was used to set the
     * filter originally.
     *
     * @param filter the filter to install; must not be {@code null}
     * @return a handle to close when deserialization is complete
     * @throws IllegalStateException if a filter is already set on this thread
     */
    public static FilterHandle setFilter(Predicate<Class<?>>  filter) {
        if (THREAD_FILTER.get() != null) {
            throw new IllegalStateException("A deserialization filter is already set on this thread");
        }
        THREAD_FILTER.set(filter);

        return THREAD_FILTER::remove;

    }

    /**
     * Returns the deserialization filter currently active on the calling thread,
     * or {@code null} if none is set.
     *
     * @return current active filter
     */
    public static Predicate<Class<?>>  currentFilter() {
        return Optional.ofNullable(THREAD_FILTER.get()).orElse(clazz -> true);
    }
}
