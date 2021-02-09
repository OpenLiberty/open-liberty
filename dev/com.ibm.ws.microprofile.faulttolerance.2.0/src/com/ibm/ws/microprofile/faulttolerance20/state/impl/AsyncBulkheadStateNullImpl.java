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

import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;

import com.ibm.ws.microprofile.faulttolerance20.state.AsyncBulkheadState;

/**
 * An implementation of {@link AsyncBulkheadState} which does not implement any bulkhead logic
 * <p>
 * Although it does not implement any bulkhead logic, it will schedule any tasks passed to {@link #submit(AsyncBulkheadTask, ExceptionHandler)} to be run asynchronously.
 */

public class AsyncBulkheadStateNullImpl implements AsyncBulkheadState {

    private final ScheduledExecutorService executorService;
    private static final BulkheadReservationNullImpl NULL_RESERVATION = new BulkheadReservationNullImpl();

    public AsyncBulkheadStateNullImpl(ScheduledExecutorService executorService) {
        this.executorService = executorService;
    }

    @Override
    public ExecutionReference submit(AsyncBulkheadTask task, ExceptionHandler exceptionHandler) {
        Future<?> future = executorService.submit(() -> task.run(NULL_RESERVATION));
        return new ExecutionReferenceNullImpl(future);
    }

    private static class ExecutionReferenceNullImpl implements ExecutionReference {

        private final Future<?> future;

        public ExecutionReferenceNullImpl(Future<?> future) {
            this.future = future;
        }

        @Override
        public void abort(boolean mayInterrupt) {
            future.cancel(mayInterrupt);
        }

        @Override
        public boolean wasAccepted() {
            return true;
        }

    }

    private static class BulkheadReservationNullImpl implements BulkheadReservation {

        @Override
        public void release() {
            // Do nothing
        }

    }

}
