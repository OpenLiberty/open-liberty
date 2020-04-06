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
package com.ibm.ws.microprofile.faulttolerance.impl.policy;

import org.eclipse.microprofile.faulttolerance.ExecutionContext;
import org.eclipse.microprofile.faulttolerance.FallbackHandler;
import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.microprofile.faulttolerance.spi.FallbackHandlerFactory;
import com.ibm.ws.microprofile.faulttolerance.spi.FallbackPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.FaultToleranceFunction;

public class FallbackPolicyImpl implements FallbackPolicy {

    private static final TraceComponent tc = Tr.register(BulkheadPolicyImpl.class);

    private FaultToleranceFunction<ExecutionContext, ?> fallbackFunction;
    private Class<? extends FallbackHandler<?>> fallbackHandlerClass;
    private FallbackHandlerFactory fallbackHandlerFactory;
    private Class<? extends Throwable>[] applyOn;
    private Class<? extends Throwable>[] skipOn;

    @SuppressWarnings("unchecked")
    public FallbackPolicyImpl() {
        try {
            applyOn = new Class[] { Throwable.class };
            skipOn = new Class[0];
        } catch (SecurityException e) {
            throw new FaultToleranceException(Tr.formatMessage(tc, "internal.error.CWMFT4998E", e), e);
        }
    }

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

    /** {@inheritDoc} */
    @Override
    public Class<? extends Throwable>[] getApplyOn() {
        return applyOn;
    }

    /** {@inheritDoc} */
    @Override
    public void setApplyOn(Class<? extends Throwable>... applyOn) {
        this.applyOn = applyOn;

    }

    /** {@inheritDoc} */
    @Override
    public Class<? extends Throwable>[] getSkipOn() {
        return skipOn;
    }

    /** {@inheritDoc} */
    @Override
    public void setSkipOn(Class<? extends Throwable>... skipOn) {
        this.skipOn = skipOn;

    }

}
