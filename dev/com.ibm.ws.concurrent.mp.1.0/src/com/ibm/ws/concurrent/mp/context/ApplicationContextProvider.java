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

import org.eclipse.microprofile.concurrent.ThreadContext;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.concurrent.mp.ContextOp;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

/**
 * Partial implementation of MicroProfile Application context, backed by Liberty's
 * Classloader Context and JEE Metadata Context.
 */
@Trivial
public class ApplicationContextProvider extends ContainerContextProvider {
    public final AtomicServiceReference<com.ibm.wsspi.threadcontext.ThreadContextProvider> classloaderContextProviderRef = new AtomicServiceReference<com.ibm.wsspi.threadcontext.ThreadContextProvider>("ClassloaderContextProvider");
    public final AtomicServiceReference<com.ibm.wsspi.threadcontext.ThreadContextProvider> jeeMetadataContextProviderRef = new AtomicServiceReference<com.ibm.wsspi.threadcontext.ThreadContextProvider>("JeeMetadataContextProvider");

    @Override
    public void addContextSnapshot(ContextOp op, ArrayList<com.ibm.wsspi.threadcontext.ThreadContext> contextSnapshots) {
        com.ibm.wsspi.threadcontext.ThreadContext snapshot;

        com.ibm.wsspi.threadcontext.ThreadContextProvider classloaderProvider = classloaderContextProviderRef.getServiceWithException();
        if (op == ContextOp.PROPAGATED)
            snapshot = classloaderProvider.captureThreadContext(EMPTY_MAP, EMPTY_MAP);
        else
            snapshot = classloaderProvider.createDefaultThreadContext(EMPTY_MAP);
        contextSnapshots.add(snapshot);

        com.ibm.wsspi.threadcontext.ThreadContextProvider jeeMetadataProvider = jeeMetadataContextProviderRef.getService();
        if (op == ContextOp.PROPAGATED)
            snapshot = jeeMetadataProvider.captureThreadContext(EMPTY_MAP, EMPTY_MAP);
        else
            snapshot = jeeMetadataProvider.createDefaultThreadContext(EMPTY_MAP);
        contextSnapshots.add(snapshot);
    }

    @Override
    public final String getThreadContextType() {
        return ThreadContext.APPLICATION;
    }
}