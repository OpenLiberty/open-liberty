/*******************************************************************************
 * Copyright (c) 2017, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.faulttolerance.spi;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;

import org.eclipse.microprofile.faulttolerance.ExecutionContext;

/**
 * Executes a callable, applying fault tolerance logic around it
 * <p>
 * Instances of this class should be obtained from an {@link ExecutorBuilder}
 *
 * @param <R> the return type of the callable
 */
public interface Executor<R> {

    /**
     * Execute a callable, applying fault tolerance logic around it
     *
     * @param callable the callable to execute
     * @param context  the execution context, which must have be obtained from a prior call to {@link #newExecutionContext(String, Method, Object...)}
     * @return the result of executing the callable, after all fault tolerance policies have been applied
     */
    public R execute(Callable<R> callable, ExecutionContext context);

    /**
     * Create a new execution context
     *
     * @param id         an identifier for the execution, only used for tracing
     * @param method     the method which is being executed by the callable, as returned by {@link ExecutionContext#getMethod()}
     * @param parameters the parameters passed to the method, as returned by {@link ExecutionContext#getParameters()}
     * @return the new execution context
     */
    public FTExecutionContext newExecutionContext(String id, Method method, Object... parameters);

    /**
     * Shuts down any thread pools created by this executor
     * <p>
     * Will be called when the executor goes out of scope
     */
    public void close();

}
