/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
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
package com.ibm.ws.microprofile.faulttolerance20.state.impl;

import java.util.concurrent.Callable;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.microprofile.faulttolerance20.impl.MethodResult;
import com.ibm.ws.microprofile.faulttolerance20.state.SyncBulkheadState;

public class SyncBulkheadStateNullImpl implements SyncBulkheadState {

    /** {@inheritDoc} */
    @FFDCIgnore(Throwable.class)
    @Override
    public <R> MethodResult<R> run(Callable<R> callable) {
        try {
            return MethodResult.success(callable.call());
        } catch (Throwable t) {
            return MethodResult.failure(t);
        }
    }

}
