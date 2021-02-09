/*******************************************************************************
 * Copyright (c) 2018,2020 IBM Corporation and others.
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
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.microprofile.context.ThreadContext;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.microprofile.contextpropagation.ContextOp;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

/**
 * Partial implementation of MicroProfile Transaction context,
 * backed by Liberty's Transaction context.
 */
@Trivial
@SuppressWarnings("deprecation")
public class TransactionContextProvider extends ContainerContextProvider {
    /**
     * Map with execution property that instructs the transaction context to propagate transactions for serial use.
     */
    private static final Map<String, String> PROPAGATE_TX_FOR_SERIAL_USE = new TreeMap<String, String>();
    static {
        // Including both keys. It's not worth it to compute the EE spec/version here given that this class is not an OSGi service component.
        PROPAGATE_TX_FOR_SERIAL_USE.put("jakarta.enterprise.concurrent.TRANSACTION", "PROPAGATE");
        PROPAGATE_TX_FOR_SERIAL_USE.put("java.enterprise.concurrent.TRANSACTION", "PROPAGATE");
    }

    public final AtomicServiceReference<com.ibm.wsspi.threadcontext.ThreadContextProvider> transactionContextProviderRef = new AtomicServiceReference<com.ibm.wsspi.threadcontext.ThreadContextProvider>("TransactionContextProvider");

    @Override
    public void addContextSnapshot(ContextOp op, ArrayList<com.ibm.wsspi.threadcontext.ThreadContext> contextSnapshots) {
        com.ibm.wsspi.threadcontext.ThreadContext snapshot;
        com.ibm.wsspi.threadcontext.ThreadContextProvider transactionProvider = transactionContextProviderRef.getService();
        if (transactionProvider == null)
            snapshot = new DeferredClearedContext(transactionContextProviderRef);
        else if (op == ContextOp.PROPAGATED) {
            snapshot = transactionProvider.captureThreadContext(PROPAGATE_TX_FOR_SERIAL_USE, EMPTY_MAP);
        } else
            snapshot = transactionProvider.createDefaultThreadContext(EMPTY_MAP);

        contextSnapshots.add(snapshot);
    }

    @Override
    public final String getThreadContextType() {
        return ThreadContext.TRANSACTION;
    }
}