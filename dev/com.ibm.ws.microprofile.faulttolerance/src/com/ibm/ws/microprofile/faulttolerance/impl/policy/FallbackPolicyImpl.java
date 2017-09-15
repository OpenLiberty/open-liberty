/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.faulttolerance.impl.policy;

import org.eclipse.microprofile.faulttolerance.ExecutionContext;
import org.eclipse.microprofile.faulttolerance.FallbackHandler;

import com.ibm.ws.microprofile.faulttolerance.spi.FallbackHandlerFactory;
import com.ibm.ws.microprofile.faulttolerance.spi.FallbackPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.FaultToleranceFunction;

public class FallbackPolicyImpl implements FallbackPolicy {

    private FaultToleranceFunction<ExecutionContext, ?> fallbackFunction;
    private Class<? extends FallbackHandler<?>> fallbackHandlerClass;
    private FallbackHandlerFactory fallbackHandlerFactory;

    /** {@inheritDoc} */
    @Override
    public FaultToleranceFunction<ExecutionContext, ?> getFallbackFunction() {
        if (this.fallbackFunction == null) {
            if (this.fallbackHandlerFactory != null && this.fallbackHandlerClass != null) {
                FallbackHandler<?> handler = this.fallbackHandlerFactory.newHandler(this.fallbackHandlerClass);
                this.fallbackFunction = (t) -> {
                    return handler.handle(t);
                };
            }
        }
        return this.fallbackFunction;
    }

    /** {@inheritDoc} */
    @Override
    public void setFallbackFunction(FaultToleranceFunction<ExecutionContext, ?> fallbackFunction) {
        this.fallbackFunction = fallbackFunction;
    }

    /** {@inheritDoc} */
    @Override
    public Class<? extends FallbackHandler<?>> getFallbackHandler() {
        return this.fallbackHandlerClass;
    }

    /** {@inheritDoc} */
    @Override
    public void setFallbackHandler(Class<? extends FallbackHandler<?>> fallbackHandlerClass, FallbackHandlerFactory fallbackHandlerFactory) {
        this.fallbackHandlerClass = fallbackHandlerClass;
        this.fallbackHandlerFactory = fallbackHandlerFactory;
    }

    /** {@inheritDoc} */
    @Override
    public FallbackHandlerFactory getFallbackHandlerFactory() {
        return this.fallbackHandlerFactory;
    }

}
