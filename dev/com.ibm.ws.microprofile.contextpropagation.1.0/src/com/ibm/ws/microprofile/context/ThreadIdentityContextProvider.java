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
package com.ibm.ws.microprofile.context;

import java.util.ArrayList;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.microprofile.contextpropagation.ContextOp;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

/**
 * Partial implementation of MicroProfile Thread Identity context provider,
 * backed by Liberty's z/OS thread identity context.
 */
@Trivial
@SuppressWarnings("deprecation")
public class ThreadIdentityContextProvider extends ContainerContextProvider {
    public static final String SYNC_TO_OS_THREAD = "SyncToOSThread";

    public final AtomicServiceReference<com.ibm.wsspi.threadcontext.ThreadContextProvider> threadIdentityContextProviderRef = new AtomicServiceReference<com.ibm.wsspi.threadcontext.ThreadContextProvider>("ThreadIdentityContextProvider");

    @Override
    public void addContextSnapshot(ContextOp op, ArrayList<com.ibm.wsspi.threadcontext.ThreadContext> contextSnapshots) {
        com.ibm.wsspi.threadcontext.ThreadContext snapshot;
        com.ibm.wsspi.threadcontext.ThreadContextProvider threadIdentityProvider = threadIdentityContextProviderRef.getService();

        if (threadIdentityProvider == null)
            snapshot = new DeferredClearedContext(threadIdentityContextProviderRef);
        else if (op == ContextOp.PROPAGATED)
            snapshot = threadIdentityProvider.captureThreadContext(EMPTY_MAP, EMPTY_MAP);
        else
            snapshot = threadIdentityProvider.createDefaultThreadContext(EMPTY_MAP);
        contextSnapshots.add(snapshot);
    }

    @Override
    public final String getThreadContextType() {
        return SYNC_TO_OS_THREAD;
    }
}