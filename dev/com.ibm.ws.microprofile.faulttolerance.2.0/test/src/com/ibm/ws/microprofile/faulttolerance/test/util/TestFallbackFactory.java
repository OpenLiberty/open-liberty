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
package com.ibm.ws.microprofile.faulttolerance.test.util;

import org.eclipse.microprofile.faulttolerance.FallbackHandler;
import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceException;

import com.ibm.ws.microprofile.faulttolerance.spi.FallbackHandlerFactory;

/**
 *
 */
public class TestFallbackFactory implements FallbackHandlerFactory {

    /** {@inheritDoc} */
    @Override
    public <R extends FallbackHandler<?>> R newHandler(Class<R> handlerClass) {
        if (handlerClass == TestFallback.class) {
            return (R) new TestFallback();
        } else {
            throw new FaultToleranceException();
        }
    }

}
