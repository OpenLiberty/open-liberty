/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.faulttolerance20.impl;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 * Holds the result of a method execution, whether it returned a result or throw an exception
 * <p>
 * New instances of this class should be created with {@link #success} or {@link #failure}
 *
 * @param <R> the return type of the method
 */
public class MethodResult<R> {

    private final R result;
    private final Throwable failure;

    /**
     * Create a MethodResult for a method which returned a value
     *
     * @param result the value returned by the method
     * @return the new MethodResult
     */
    public static <R> MethodResult<R> success(R result) {
        return new MethodResult<>(result, null);
    }

    /**
     * Create a MethodResult for a method which threw an exception
     *
     * @param failure the exception thrown by the method
     * @return the new MethodResult
     */
    public static <R> MethodResult<R> failure(Throwable failure) {
        return new MethodResult<>(null, failure);
    }

    @Trivial
    private MethodResult(R result, Throwable failure) {
        this.result = result;
        this.failure = failure;
    }

    /**
     * Get the value that the method returned
     *
     * @return the value that the method returned, or {@code null} if it threw an exception
     */
    @Trivial
    public R getResult() {
        return result;
    }

    /**
     * Get the exception thrown by the method
     *
     * @return the exception thrown by the method, or {@code null} if it did not throw an exception
     */
    @Trivial
    public Throwable getFailure() {
        return failure;
    }

    /**
     * Whether the method threw an exception
     *
     * @return {@code true} if the method threw an exception, {@code false} if it did not
     */
    @Trivial
    public boolean isFailure() {
        return failure != null;
    }

    @Override
    @Trivial
    public String toString() {
        if (isFailure()) {
            return "Method threw exeception: " + getFailure();
        } else {
            return "Method returned value: " + getResult();
        }
    }

}
