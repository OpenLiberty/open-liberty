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
package com.ibm.ws.microprofile.faulttolerance20.state.impl;

import java.util.concurrent.Callable;
import java.util.concurrent.Semaphore;

import org.eclipse.microprofile.faulttolerance.exceptions.BulkheadException;

import com.ibm.ws.microprofile.faulttolerance.spi.BulkheadPolicy;
import com.ibm.ws.microprofile.faulttolerance20.impl.MethodResult;

public class SyncBulkheadStateImpl extends SyncBulkheadStateNullImpl {

    private final Semaphore semaphore;

    public SyncBulkheadStateImpl(BulkheadPolicy policy) {
        semaphore = new Semaphore(policy.getMaxThreads());
    }

    /** {@inheritDoc} */
    @Override
    public <R> MethodResult<R> run(Callable<R> callable) {

        if (!semaphore.tryAcquire()) {
            return MethodResult.failure(new BulkheadException());
        }

        try {
            return super.run(callable);
        } finally {
            semaphore.release();
        }
    }

}
