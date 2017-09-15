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
package com.ibm.ws.microprofile.faulttolerance.impl.sync;

import java.util.concurrent.Callable;
import java.util.concurrent.Semaphore;

import org.eclipse.microprofile.faulttolerance.exceptions.BulkheadException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.microprofile.faulttolerance.impl.ExecutionContextImpl;
import com.ibm.ws.microprofile.faulttolerance.spi.BulkheadPolicy;

/**
 * SemaphoreTaskRunner will try to acquire a semaphore token before running. If it can not then an exception is thrown.
 */
public class SemaphoreTaskRunner<R> extends SimpleTaskRunner<R> {

    private static final TraceComponent tc = Tr.register(SemaphoreTaskRunner.class);

    private final Semaphore semaphore;

    public SemaphoreTaskRunner(BulkheadPolicy bulkheadPolicy) {
        this.semaphore = new Semaphore(bulkheadPolicy.getMaxThreads());
    }

    @Override
    public R runTask(Callable<R> callable, ExecutionContextImpl executionContext) throws Exception {
        R result = null;

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Acquiring semaphore for {0}", executionContext.getDescriptor());
        }
        boolean acquired = this.semaphore.tryAcquire();
        if (!acquired) {
            throw new BulkheadException(Tr.formatMessage(tc, "bulkhead.no.threads.CWMFT0001E", executionContext.getMethod()));
        }
        try {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Obtained semaphore for {0}", executionContext.getDescriptor());
            }
            result = super.runTask(callable, executionContext);
        } finally {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Releasing semaphore for {0}", executionContext.getDescriptor());
            }
            this.semaphore.release();
        }

        return result;
    }

}
