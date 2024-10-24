/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.kernel.feature.internal.util;

/**
 * Type for a lazy producer.
 */
public interface LazySupplier<T> {

    /**
     * Produce an object of the supplied type.
     *
     * This is distinguished from {@link #supply()}: Usually,
     * {@link #produce()} produces a new instance, while
     * {@link #supply()} returns either a new instance obtained
     * from {@link #produce()}, or returns a cached copy of a
     * previously supplied instance.
     *
     * @return An object of the supplied type.
     */
    T produce();

    /**
     * Supply an object of the supplied type.
     *
     * This is distinguished from {@link #produce()}: Usually,
     * {@link #produce()} produces a new instance, while
     * {@link #supply()} returns either a new instance obtained
     * from {@link #produce()}, or returns a cached copy of a
     * previously supplied instance.
     *
     * @return An object of the supplied type.
     */
    T supply();

    /**
     * Answer the previously supplied value.
     *
     * Answer null if {@link #supply()} has not yet been invoked.
     *
     * @return The previously supplied value.
     */
    T getSupplied();

}
