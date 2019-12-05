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

import org.eclipse.microprofile.faulttolerance.ExecutionContext;
import org.eclipse.microprofile.faulttolerance.FallbackHandler;

public interface FallbackPolicy {

    public FaultToleranceFunction<ExecutionContext, ?> getFallbackFunction();

    public void setFallbackFunction(FaultToleranceFunction<ExecutionContext, ?> fallback);

    public Class<? extends FallbackHandler<?>> getFallbackHandler();

    public void setFallbackHandler(Class<? extends FallbackHandler<?>> clazz, FallbackHandlerFactory factory);

    public FallbackHandlerFactory getFallbackHandlerFactory();

    /**
     * Define the apply criteria
     *
     * @return the apply exception
     */
    public Class<? extends Throwable>[] getApplyOn();

    @SuppressWarnings("unchecked")
    public void setApplyOn(Class<? extends Throwable>... failOn);

    /**
     * Define the skip criteria
     *
     * @return the skip exception
     */
    public Class<? extends Throwable>[] getSkipOn();

    @SuppressWarnings("unchecked")
    public void setSkipOn(Class<? extends Throwable>... skipOn);

}
