/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.annocache.util;

// import java.util.Objects;

// Converted to a regular interface to remove a java8 dependency.

/**
 * Functional interface for consumers which throw exceptions.
 *
 * Modeled after {@link java.util.function.Consumer}.
 * 
 * @param <T> The type which is consumed.
 * @param <E> The type of the exception which may be thrown.
 */
// @FunctionalInterface
public interface Util_Consumer<T, E extends Exception> {
    void accept(T t) throws E;

//    default Util_Consumer<? super T, ? extends E> andThen
//        (Util_Consumer<? super T, ? extends E> after) {
//
//        Objects.requireNonNull(after);
//
//        return (T t) -> { accept(t); after.accept(t); };
//    }
}
