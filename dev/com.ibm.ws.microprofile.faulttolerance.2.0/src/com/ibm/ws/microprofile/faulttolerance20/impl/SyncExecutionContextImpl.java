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

import com.ibm.ws.microprofile.faulttolerance.spi.FTExecutionContext;

/**
 * Stores context for one method execution
 */
public class SyncExecutionContextImpl implements FTExecutionContext {

    private final Method method;
    private final Object[] parameters;
    private Throwable failure;

    public SyncExecutionContextImpl(Method method, Object[] parameters) {
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

    @Override
    public void close() {}

}
