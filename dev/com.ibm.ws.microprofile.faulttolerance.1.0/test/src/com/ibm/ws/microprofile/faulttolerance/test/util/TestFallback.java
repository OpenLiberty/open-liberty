/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.faulttolerance.test.util;

import org.eclipse.microprofile.faulttolerance.ExecutionContext;
import org.eclipse.microprofile.faulttolerance.FallbackHandler;

import com.ibm.ws.microprofile.faulttolerance.spi.FaultToleranceFunction;

/**
 *
 */
public class TestFallback implements FaultToleranceFunction<ExecutionContext, String>, FallbackHandler<String> {

    /** {@inheritDoc} */
    @Override
    public String execute(ExecutionContext context) throws Exception {
        return "Fallback: " + context.getParameters()[0];
    }

    /** {@inheritDoc} */
    @Override
    public String handle(ExecutionContext context) {
        return "Fallback: " + context.getParameters()[0];
    }

}
