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
package com.ibm.ws.concurrent.mp.context;

import java.util.ArrayList;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.concurrent.mp.ContextOp;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

/**
 * Partial implementation of MicroProfile thread context provider,
 * backed by Liberty's z/OS WLM context.
 */
@Trivial
public class WLMContextProvider extends ContainerContextProvider {
    public static final String WORKLOAD = "Workload";

    public final AtomicServiceReference<com.ibm.wsspi.threadcontext.ThreadContextProvider> wlmContextProviderRef = new AtomicServiceReference<com.ibm.wsspi.threadcontext.ThreadContextProvider>("WLMContextProvider");

    @Override
    public void addContextSnapshot(ContextOp op, ArrayList<com.ibm.wsspi.threadcontext.ThreadContext> contextSnapshots) {
        com.ibm.wsspi.threadcontext.ThreadContext snapshot;
        com.ibm.wsspi.threadcontext.ThreadContextProvider wlmProvider = wlmContextProviderRef.getService();
        if (wlmProvider == null)
            snapshot = new DeferredClearedContext(wlmContextProviderRef);
        // TODO currently, there is no way to configure execution properties that are necessary for propagate
        //else if (op == ContextOp.PROPAGATED)
        //    snapshot = wlmProvider.captureThreadContext(...);
        else
            snapshot = wlmProvider.createDefaultThreadContext(EMPTY_MAP);

        contextSnapshots.add(snapshot);
    }

    @Override
    public final String getThreadContextType() {
        return WORKLOAD;
    }
}