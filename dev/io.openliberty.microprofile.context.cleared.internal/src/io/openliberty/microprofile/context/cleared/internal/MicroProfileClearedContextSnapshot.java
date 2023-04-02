/*******************************************************************************
 * Copyright (c) 2018,2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.context.cleared.internal;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.concurrent.RejectedExecutionException;

import org.eclipse.microprofile.context.spi.ThreadContextController;
import org.eclipse.microprofile.context.spi.ThreadContextSnapshot;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;

/**
 * Multi-context snapshot that clears MicroProfile context types when server config is used.
 */
@Trivial // this class does its own tracing when clearing and restoring context
public class MicroProfileClearedContextSnapshot implements com.ibm.wsspi.threadcontext.ThreadContext {
    private static final long serialVersionUID = 1L;

    private static final TraceComponent tc = Tr.register(MicroProfileClearedContextSnapshot.class);

    private final ArrayList<ThreadContextController> contextRestorers = new ArrayList<ThreadContextController>();
    private final ArrayList<ThreadContextSnapshot> contextSnapshots;

    public MicroProfileClearedContextSnapshot(ArrayList<ThreadContextSnapshot> contextSnapshots) {
        this.contextSnapshots = contextSnapshots; // shallow copy is okay here
    }

    @Override
    public com.ibm.wsspi.threadcontext.ThreadContext clone() {
        return new MicroProfileClearedContextSnapshot(contextSnapshots);
    }

    @Override
    public void taskStarting() throws RejectedExecutionException {
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        try {
            for (ThreadContextSnapshot snapshot : contextSnapshots) {
                if (trace && tc.isDebugEnabled())
                    Tr.debug(this, tc, "clearing " + snapshot);

                contextRestorers.add(snapshot.begin());
            }
        } catch (Error | RuntimeException x) {
            taskStopping();
            throw x;
        }
    }

    @Override
    public void taskStopping() {
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        Throwable failure = null;
        for (int i = contextRestorers.size() - 1; i >= 0; i--)
            try {
                ThreadContextController contextRestorer = contextRestorers.remove(i);

                if (trace && tc.isDebugEnabled())
                    Tr.debug(this, tc, "restoring " + contextRestorer);

                contextRestorer.endContext();
            } catch (Error | RuntimeException x) {
                if (failure != null)
                    failure = x;
            }

        if (failure instanceof Error)
            throw (Error) failure;
        if (failure instanceof RuntimeException)
            throw (RuntimeException) failure;
    }

    @Override
    public String toString() {
        return new StringBuilder(getClass().getSimpleName()).append('@').append(Integer.toHexString(hashCode())).toString();
    }

    private void writeObject(ObjectOutputStream outStream) throws IOException {
        outStream.putFields();
        // nothing to write because the clearing context is recreated at deserialization time
        outStream.writeFields();
    }
}