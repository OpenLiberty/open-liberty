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

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectOutputStream;

import org.eclipse.microprofile.concurrent.spi.ThreadContextController;
import org.eclipse.microprofile.concurrent.spi.ThreadContextSnapshot;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.wsspi.threadcontext.ThreadContext;

/**
 * Proxies a MicroProfile ThreadContextSnapshot as a com.ibm.wsspi.threadcontext.ThreadContext
 * which is used internally for compatibility with container-provided context types.
 */
@Trivial
public class ContextSnapshotProxy implements com.ibm.wsspi.threadcontext.ThreadContext {
    private static final long serialVersionUID = 1L;

    private ThreadContextController contextRestorer;
    private final ThreadContextSnapshot snapshot;

    ContextSnapshotProxy(ThreadContextSnapshot snapshot) {
        this.snapshot = snapshot;
    }

    @Override
    public ThreadContext clone() {
        return new ContextSnapshotProxy(snapshot);
    }

    @Override
    public void taskStarting() {
        contextRestorer = snapshot.begin();
    }

    @Override
    public void taskStopping() {
        contextRestorer.endContext();
        contextRestorer = null;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("proxy@").append(Integer.toHexString(hashCode())) //
                        .append(" for ").append(snapshot);
        if (contextRestorer != null)
            sb.append(" with ").append(contextRestorer);
        return sb.toString();
    }

    private void writeObject(ObjectOutputStream outStream) throws IOException {
        throw new NotSerializableException();
    }
}
