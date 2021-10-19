/*******************************************************************************
 * Copyright (c) 2018,2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.concurrent.internal;

import java.util.Map;
import java.util.concurrent.Executor;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.concurrent.WSManagedExecutorService;
import com.ibm.ws.threading.PolicyExecutor;
import com.ibm.wsspi.threadcontext.ThreadContextDescriptor;
import com.ibm.wsspi.threadcontext.WSContextService;

/**
 * Executor that provides thread context capture/propagation only and is incapable of running tasks.
 * WSManagedExecutorService is implemented to store a context service instance
 * as a convenience to the managed completable future implementation.
 * The execute method is rejected as unsupported.
 */
@Trivial
class UnusableExecutor implements Executor, WSManagedExecutorService {
    private final WSContextService contextService;

    UnusableExecutor(WSContextService contextService) {
        this.contextService = contextService;
    }

    @Override
    @SuppressWarnings("unchecked")
    public ThreadContextDescriptor captureThreadContext(Map<String, String> props) {
        return contextService.captureThreadContext(props);
    }

    @Override
    public void execute(Runnable command) {
        throw new UnsupportedOperationException();
    }

    @Override
    public PolicyExecutor getLongRunningPolicyExecutor() {
        throw new UnsupportedOperationException();
    }

    @Override
    public PolicyExecutor getNormalPolicyExecutor() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int hashCode() {
        return contextService.hashCode(); // for easy correlation in trace with the context service that created it
    }
}
