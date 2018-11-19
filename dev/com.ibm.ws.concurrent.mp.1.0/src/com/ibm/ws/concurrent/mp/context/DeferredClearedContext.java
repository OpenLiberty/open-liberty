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

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectOutputStream;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.threadcontext.ThreadContext;

/**
 * In Liberty, the container-provided context types are considered always available
 * even if the feature supplying the container internal com.ibm.wsspi.threadcontext.ThreadContextProvider
 * type isn't available. This class allows for the possibility that the feature could become
 * available at a later time, by deferring the creation of the cleared thread context snapshot
 * until the action or task is about to start.
 */
@Trivial
public class DeferredClearedContext implements com.ibm.wsspi.threadcontext.ThreadContext {
    private static final long serialVersionUID = 1L;

    public com.ibm.wsspi.threadcontext.ThreadContext clearedContextController;
    public final AtomicServiceReference<com.ibm.wsspi.threadcontext.ThreadContextProvider> contextProviderRef;

    DeferredClearedContext(AtomicServiceReference<com.ibm.wsspi.threadcontext.ThreadContextProvider> contextProviderRef) {
        this.contextProviderRef = contextProviderRef;
    }

    @Override
    public ThreadContext clone() {
        return new DeferredClearedContext(contextProviderRef);
    }

    @Override
    public void taskStarting() {
        com.ibm.wsspi.threadcontext.ThreadContextProvider provider = contextProviderRef.getService();
        if (provider != null) {
            clearedContextController = provider.createDefaultThreadContext(ContainerContextProvider.EMPTY_MAP);
            clearedContextController.taskStarting();
        }
    }

    @Override
    public void taskStopping() {
        if (clearedContextController != null) {
            clearedContextController.taskStopping();
            clearedContextController = null;
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append('@').append(Integer.toHexString(hashCode())) //
                        .append(" for ").append(contextProviderRef);
        return sb.toString();
    }

    private void writeObject(ObjectOutputStream outStream) throws IOException {
        throw new NotSerializableException();
    }
}
