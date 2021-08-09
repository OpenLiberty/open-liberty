/*******************************************************************************
 * Copyright (c) 2018, 2019 IBM Corporation and others.
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
import com.ibm.ws.microprofile.faulttolerance.spi.MetricRecorder;
import com.ibm.ws.microprofile.faulttolerance20.impl.MethodResult;

public class SyncBulkheadStateImpl extends SyncBulkheadStateNullImpl {

    private final Semaphore semaphore;
    private final MetricRecorder metrics;

    public SyncBulkheadStateImpl(BulkheadPolicy policy, MetricRecorder metrics) {
        final int maxThreads = policy.getMaxThreads();
        semaphore = new Semaphore(maxThreads);
        this.metrics = metrics;
        metrics.setBulkheadConcurentExecutionCountSupplier(() -> maxThreads - semaphore.availablePermits());
    }

    /** {@inheritDoc} */
    @Override
    public <R> MethodResult<R> run(Callable<R> callable) {

        if (!semaphore.tryAcquire()) {
            metrics.incrementBulkheadRejectedCount();
            return MethodResult.failure(new BulkheadException());
        }

        long startTime = System.nanoTime();
        metrics.incrementBulkeadAcceptedCount();

        try {
            return super.run(callable);
        } finally {
            semaphore.release();
            long endTime = System.nanoTime();
            metrics.recordBulkheadExecutionTime(endTime - startTime);
        }
    }

}
