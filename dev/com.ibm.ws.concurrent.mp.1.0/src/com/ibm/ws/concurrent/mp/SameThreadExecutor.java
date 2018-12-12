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
package com.ibm.ws.concurrent.mp;

import java.util.concurrent.Executor;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.concurrent.WSManagedExecutorService;
import com.ibm.ws.threading.PolicyExecutor;
import com.ibm.wsspi.threadcontext.WSContextService;

/**
 * Executor that runs tasks on the same thread that invokes execute.
 * WSManagedExecutorService is implemented to store a context service instance
 * as a convenience to the managed completable future implementation.
 */
class SameThreadExecutor implements Executor, WSManagedExecutorService {
    private final WSContextService contextService;

    @Trivial
    SameThreadExecutor(WSContextService contextService) {
        this.contextService = contextService;
    }

    @Override
    public void execute(Runnable command) {
        command.run();
    }

    @Override
    @Trivial
    public WSContextService getContextService() {
        return contextService;
    }

    @Override
    @Trivial
    public PolicyExecutor getNormalPolicyExecutor() {
        throw new UnsupportedOperationException();
    }

    @Override
    @Trivial
    public int hashCode() {
        return contextService.hashCode(); // for easy correlation in trace with the context service that created it
    }
}
