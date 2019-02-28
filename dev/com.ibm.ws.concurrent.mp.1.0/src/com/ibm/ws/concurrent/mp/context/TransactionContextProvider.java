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
import java.util.Collections;
import java.util.Map;

import javax.enterprise.concurrent.ManagedTask;

import org.eclipse.microprofile.concurrent.ThreadContext;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.concurrent.mp.ContextOp;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

/**
 * Partial implementation of MicroProfile Transaction context,
 * backed by Liberty's Transaction context.
 */
@Trivial
@SuppressWarnings("deprecation")
public class TransactionContextProvider extends ContainerContextProvider {
    private static final TraceComponent tc = Tr.register(TransactionContextProvider.class);

    /**
     * Map with execution property that instructs the transaction context to first detect the presence of a
     * global transaction on the requesting thread. If present, a NULL is returning indicate that context
     * cannot be captured. If a global transaction is not present, then captures a cleared context.
     */
    private static final Map<String, String> SUSPEND_IF_NO_GLOBAL_TX = Collections.singletonMap(ManagedTask.TRANSACTION, "SUSPEND_IF_NO_GLOBAL_TX");

    public final AtomicServiceReference<com.ibm.wsspi.threadcontext.ThreadContextProvider> transactionContextProviderRef = new AtomicServiceReference<com.ibm.wsspi.threadcontext.ThreadContextProvider>("TransactionContextProvider");

    @Override
    public void addContextSnapshot(ContextOp op, ArrayList<com.ibm.wsspi.threadcontext.ThreadContext> contextSnapshots) {
        com.ibm.wsspi.threadcontext.ThreadContext snapshot;
        com.ibm.wsspi.threadcontext.ThreadContextProvider transactionProvider = transactionContextProviderRef.getService();
        if (transactionProvider == null)
            snapshot = new DeferredClearedContext(transactionContextProviderRef);
        else if (op == ContextOp.PROPAGATED) {
            snapshot = transactionProvider.captureThreadContext(SUSPEND_IF_NO_GLOBAL_TX, EMPTY_MAP);
            if (snapshot == null)
                throw new UnsupportedOperationException(Tr.formatMessage(tc, "CWWKC1157.cannot.propagate.tx"));
        } else
            snapshot = transactionProvider.createDefaultThreadContext(EMPTY_MAP);

        contextSnapshots.add(snapshot);
    }

    @Override
    public final String getThreadContextType() {
        return ThreadContext.TRANSACTION;
    }
}