/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
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
 *
 * @param <R> the return type of the callable
 */
public interface Executor<R> {

    public R execute(Callable<R> callable, ExecutionContext context);

    public FTExecutionContext newExecutionContext(String id, Method method, Object... parameters);

    /**
     * Shuts down any thread pools created by this executor
     * <p>
     * Will be called when the executor goes out of scope
     */
    public void close();

}
