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
package com.ibm.ws.microprofile.faulttolerance.cdi;

import org.eclipse.microprofile.faulttolerance.ExecutionContext;
import org.eclipse.microprofile.faulttolerance.FallbackHandler;

import com.ibm.ws.microprofile.faulttolerance.spi.FaultToleranceFunction;

/**
 * @param <T>
 *
 */
public class FallbackFunction<R> implements FaultToleranceFunction<ExecutionContext, R> {

    private final FallbackHandler<R> fallbackHandler;

    public FallbackFunction(FallbackHandler<R> fallbackHandler) {
        this.fallbackHandler = fallbackHandler;
    }

    /** {@inheritDoc} */
    @Override
    public R execute(ExecutionContext executionContext) throws Exception {
        return this.fallbackHandler.handle(executionContext);
    }

}
