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
 * Partial implementation of MicroProfile Transaction context,
 * backed by Liberty's Transaction context.
 */
@Trivial
public class TransactionContextProvider extends ContainerContextProvider {
    public final AtomicServiceReference<com.ibm.wsspi.threadcontext.ThreadContextProvider> transactionContextProviderRef = new AtomicServiceReference<com.ibm.wsspi.threadcontext.ThreadContextProvider>("TransactionContextProvider");

    @Override
    public void addContextSnapshot(ContextOp op, ArrayList<com.ibm.wsspi.threadcontext.ThreadContext> contextSnapshots) {
        com.ibm.wsspi.threadcontext.ThreadContext snapshot;
        com.ibm.wsspi.threadcontext.ThreadContextProvider transactionProvider = transactionContextProviderRef.getService();
        if (transactionProvider == null)
            snapshot = new DeferredClearedContext(transactionContextProviderRef);
        else if (op == ContextOp.PROPAGATED)
            // TODO it is reasonable to reject this when someone explicitly asks for transaction propagation,
            // but will it be a problem to reject this when the user specifies ALL_REMAINING ?
            throw new UnsupportedOperationException();
        else
            snapshot = transactionProvider.createDefaultThreadContext(EMPTY_MAP);

        contextSnapshots.add(snapshot);
    }

    @Override
    public final String getThreadContextType() {
        return ThreadContext.TRANSACTION;
    }
}