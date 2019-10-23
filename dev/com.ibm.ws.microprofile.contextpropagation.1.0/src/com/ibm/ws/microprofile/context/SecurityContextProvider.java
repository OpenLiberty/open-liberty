/*******************************************************************************
 * Copyright (c) 2018,2019 IBM Corporation and others.
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

import org.eclipse.microprofile.context.ThreadContext;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.microprofile.contextpropagation.ContextOp;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

/**
 * Partial implementation of MicroProfile Security context,
 * backed by Liberty's Security context.
 */
@Trivial
@SuppressWarnings("deprecation")
public class SecurityContextProvider extends ContainerContextProvider {
    public final AtomicServiceReference<com.ibm.wsspi.threadcontext.ThreadContextProvider> securityContextProviderRef = new AtomicServiceReference<com.ibm.wsspi.threadcontext.ThreadContextProvider>("SecurityContextProvider");

    @Override
    public void addContextSnapshot(ContextOp op, ArrayList<com.ibm.wsspi.threadcontext.ThreadContext> contextSnapshots) {
        com.ibm.wsspi.threadcontext.ThreadContext snapshot;

        com.ibm.wsspi.threadcontext.ThreadContextProvider securityProvider = securityContextProviderRef.getService();
        if (securityProvider == null)
            snapshot = new DeferredClearedContext(securityContextProviderRef);
        else if (op == ContextOp.PROPAGATED)
            snapshot = securityProvider.captureThreadContext(EMPTY_MAP, EMPTY_MAP);
        else
            snapshot = securityProvider.createDefaultThreadContext(EMPTY_MAP);
        contextSnapshots.add(snapshot);
    }

    @Override
    public final String getThreadContextType() {
        return ThreadContext.SECURITY;
    }
}