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

import java.lang.reflect.Method;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.microprofile.faulttolerance.spi.FTExecutionContext;

/**
 * Stores context for one method execution
 */
public class SyncExecutionContextImpl implements FTExecutionContext {

    private final Method method;
    private final Object[] parameters;
    private final String id;
    private Throwable failure;

    /**
     * @param id         an id for the execution (used in trace messages)
     * @param method     the method being executed
     * @param parameters the parameters passed to the method
     */
    public SyncExecutionContextImpl(String id, Method method, Object[] parameters) {
        this.id = id;
        this.method = method;
        this.parameters = parameters;
    }

    @Override
    public Method getMethod() {
        return method;
    }

    @Override
    public Object[] getParameters() {
        return parameters;
    }

    public void setFailure(Throwable failure) {
        this.failure = failure;
    }

    @Override
    public Throwable getFailure() {
        return failure;
    }

    /**
     * Returns the execution ID
     * <p>
     * This is not guaranteed to be unique and should only be used for tracing.
     * <p>
     * It will be blank if no tracing is enabled
     *
     * @return the execution id, only used for tracing
     */
    @Trivial // When we're calling this method, we're tracing out the result anyway
    public String getId() {
        return id;
    }

    @Override
    public void close() {
    }

}
