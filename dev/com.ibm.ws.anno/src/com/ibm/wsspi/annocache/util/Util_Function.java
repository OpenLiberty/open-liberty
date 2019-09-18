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
 * Functional interface for functions which throw exceptions.
 *
 * Modeled after {@link java.util.function.Function}.
 * 
 * @param <T> The parameter type.
 * @param <E> The type of the exception which may be thrown.
 * @param <R> The result type.
 */
// @FunctionalInterface
public interface Util_Function<T, E extends Exception, R> {
    R apply(T t) throws E;

//    default <V> Util_Function<V, E, R> compose(Util_Function<? super V, ? extends E, ? extends T> before) {
//        Objects.requireNonNull(before);
//        return (V v) -> apply( before.apply(v) );
//    }
//
//    default <V> Util_Function<T, E, V> andThen(Util_Function<? super R, ? extends E, ? extends V> after) {
//        Objects.requireNonNull(after);
//        return (T t) -> after.apply(apply(t));
//    }
//
//    static <T, E extends Exception> Util_Function<T, E, T> identity() {
//        return t -> t;
//    }
}
