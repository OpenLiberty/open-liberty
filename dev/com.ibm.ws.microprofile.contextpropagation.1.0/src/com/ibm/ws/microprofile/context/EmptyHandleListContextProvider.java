/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
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
 * Pseudo-context type that puts an empty HandleList onto the thread for contextual tasks.
 * This internal context type is always set by the container to be cleared.
 * Applications do not configure it.
 */
@Trivial
@SuppressWarnings("deprecation")
public class EmptyHandleListContextProvider extends ContainerContextProvider {
    public static final String EMPTY_HANDLE_LIST = "EmptyHandleList";

    public final AtomicServiceReference<com.ibm.wsspi.threadcontext.ThreadContextProvider> emptyHandleListContextProviderRef = //
                    new AtomicServiceReference<com.ibm.wsspi.threadcontext.ThreadContextProvider>("EmptyHandleListContextProvider");

    @Override
    public void addContextSnapshot(ContextOp op, ArrayList<com.ibm.wsspi.threadcontext.ThreadContext> contextSnapshots) {
        com.ibm.wsspi.threadcontext.ThreadContext snapshot;

        com.ibm.wsspi.threadcontext.ThreadContextProvider emptyHandleListProvider = emptyHandleListContextProviderRef.getService();
        if (emptyHandleListProvider == null)
            snapshot = new DeferredClearedContext(emptyHandleListContextProviderRef);
        else if (op == ContextOp.CLEARED)
            snapshot = emptyHandleListProvider.createDefaultThreadContext(EMPTY_MAP);
        else
            throw new UnsupportedOperationException(op.name());

        contextSnapshots.add(snapshot);
    }

    @Override
    public final String getThreadContextType() {
        return EMPTY_HANDLE_LIST;
    }
}