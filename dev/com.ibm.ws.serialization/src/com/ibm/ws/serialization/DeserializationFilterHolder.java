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
package com.ibm.ws.serialization;

import java.io.ObjectInputFilter;
import java.util.concurrent.Callable;

/**
 * Holds a per-thread {@link ObjectInputFilter} to be applied by
 * {@link DeserializationObjectInputStream} when constructing a new deserialization stream.
 *
 * <p>The filter must be set on the same thread that will call
 * {@code ObjectMessage.getObject()}. It must be cleared after use — either via
 * {@link #clearFilter()} or by using the {@link #withFilter(ObjectInputFilter, Callable)}
 * convenience method, which guarantees cleanup even if the action throws.
 *
 * <p>If no filter is set on the thread, {@link DeserializationObjectInputStream} applies
 * a built-in default denylist covering known deserialization gadget-chain packages.
 *
 * <p><b>Preferred usage (framework and batch code):</b>
 * <pre>
 * byte[] payload = DeserializationFilterHolder.withFilter(
 *     ObjectInputFilter.Config.createFilter("byte[];!*"),
 *     () -&gt; (byte[]) objectMessage.getObject()
 * );
 * </pre>
 *
 * <p><b>Manual usage (when a single filter covers multiple calls):</b>
 * <pre>
 * DeserializationFilterHolder.setFilter(
 *     ObjectInputFilter.Config.createFilter("com.example.myapp.*;java.util.*;!*")
 * );
 * try {
 *     MyPayload obj = (MyPayload) objectMessage.getObject();
 * } finally {
 *     DeserializationFilterHolder.clearFilter();
 * }
 * </pre>
 *
 * <p><b>Threading note:</b> The filter is held in a {@link ThreadLocal}. It is safe for
 * concurrent use across threads because each thread has its own independent value.
 * However, the filter must be set <em>on the same thread</em> that will call
 * {@code getObject()}. In particular, a filter set before registering an async
 * {@code MessageListener} will <em>not</em> be visible on the delivery thread —
 * it must be set at the top of the {@code onMessage()} implementation instead.
 */
public final class DeserializationFilterHolder {

    private static final ThreadLocal<ObjectInputFilter> filterHolder = new ThreadLocal<>();

    private DeserializationFilterHolder() {}

    /**
     * Sets the deserialization filter for the current thread.
     * Must always be paired with a {@link #clearFilter()} call in a {@code finally} block.
     * Prefer {@link #withFilter(ObjectInputFilter, Callable)} to avoid accidental leaks.
     *
     * @param filter the filter to apply; must not be {@code null}
     */
    public static void setFilter(ObjectInputFilter filter) {
        filterHolder.set(filter);
    }

    /**
     * Returns the deserialization filter currently set for this thread,
     * or {@code null} if none has been set.
     *
     * @return the current filter, or {@code null}
     */
    public static ObjectInputFilter getFilter() {
        return filterHolder.get();
    }

    /**
     * Removes the deserialization filter from the current thread.
     * Must be called in a {@code finally} block whenever {@link #setFilter} is used directly.
     */
    public static void clearFilter() {
        filterHolder.remove();
    }

    /**
     * Sets the filter, executes {@code action}, then always clears the filter on return —
     * even if {@code action} throws. This is the preferred usage pattern as it prevents
     * filter leaks on pooled threads.
     *
     * @param <T>    the return type of the action
     * @param filter the filter to apply during the action; must not be {@code null}
     * @param action the code to execute with the filter active
     * @return the value returned by {@code action}
     * @throws Exception any exception thrown by {@code action}
     */
    public static <T> T withFilter(ObjectInputFilter filter, Callable<T> action) throws Exception {
        setFilter(filter);
        try {
            return action.call();
        } finally {
            clearFilter();
        }
    }
}
