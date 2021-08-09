/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.faulttolerance20.state.impl;

import com.ibm.ws.microprofile.faulttolerance20.impl.MethodResult;
import com.ibm.ws.microprofile.faulttolerance20.impl.SyncExecutionContextImpl;
import com.ibm.ws.microprofile.faulttolerance20.state.FallbackState;

public class FallbackStateNullImpl implements FallbackState {

    /** {@inheritDoc} */
    @Override
    public boolean shouldApplyFallback(MethodResult<?> result) {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public <R> MethodResult<R> runFallback(MethodResult<R> result, SyncExecutionContextImpl executionContext) {
        throw new UnsupportedOperationException("runFallback() called when no Fallback present");
    }

}
