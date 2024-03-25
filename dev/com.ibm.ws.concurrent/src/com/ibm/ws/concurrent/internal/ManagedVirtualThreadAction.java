/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
package com.ibm.ws.concurrent.internal;

import java.util.ArrayList;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.concurrent.internal.ManagedThreadFactoryService.ManagedThreadFactoryImpl;
import com.ibm.wsspi.threadcontext.ThreadContext;

/**
 * Action to run on a virtual thread from a managed thread factory.
 */
class ManagedVirtualThreadAction implements Runnable {
    private static final TraceComponent tc = Tr.register(ManagedVirtualThreadAction.class);

    /**
     * The action to run on the virtual thread.
     */
    private final Runnable action;

    /**
     * The managed thread factory that created the thread.
     */
    private final ManagedThreadFactoryImpl threadFactory;

    /**
     * Construct a new managed thread.
     *
     * @param threadFactory managed thread factory
     * @param runnable      task to run on the thread
     * @param name          name for the thread
     */
    ManagedVirtualThreadAction(ManagedThreadFactoryImpl threadFactory, Runnable action) {
        this.action = action;
        this.threadFactory = threadFactory;
    }

    /**
     * Run the action on the virtual thread.
     */
    @Override
    @Trivial
    public void run() {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "run " + action.getClass().getName());

        try {
            ArrayList<ThreadContext> contextAppliedToThread = threadFactory.threadContextDescriptor.taskStarting();
            try {
                action.run();
            } finally {
                threadFactory.threadContextDescriptor.taskStopping(contextAppliedToThread);
            }
        } catch (Error x) {
            if (trace && tc.isEntryEnabled())
                Tr.exit(this, tc, "run", x);
            throw x;
        } catch (RuntimeException x) {
            if (trace && tc.isEntryEnabled())
                Tr.exit(this, tc, "run", x);
            throw x;
        }

        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "run");
    }
}