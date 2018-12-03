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

public class AsyncBulkheadStateNullImpl implements AsyncBulkheadState {

    private final ScheduledExecutorService executorService;

    public AsyncBulkheadStateNullImpl(ScheduledExecutorService executorService) {
        this.executorService = executorService;
    }

    @Override
    public ExecutionReference submit(Runnable runnable, ExceptionHandler exceptionHandler) {
        Future<?> future = executorService.submit(runnable);
        return new ExecutionReferenceImpl(future);
    }

    private class ExecutionReferenceImpl implements ExecutionReference {

        private final Future<?> future;

        public ExecutionReferenceImpl(Future<?> future) {
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

    @Override
    public void shutdown() {
        // Nothing to do
    }

}
