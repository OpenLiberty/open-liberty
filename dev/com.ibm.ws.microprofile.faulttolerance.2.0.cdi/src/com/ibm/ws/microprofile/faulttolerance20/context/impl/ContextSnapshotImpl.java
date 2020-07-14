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
package com.ibm.ws.microprofile.faulttolerance20.context.impl;

import java.util.ArrayList;
import java.util.concurrent.Callable;

import javax.enterprise.context.ContextNotActiveException;
import javax.enterprise.context.control.RequestContextController;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.microprofile.faulttolerance.spi.context.ContextSnapshot;
import com.ibm.wsspi.threadcontext.ThreadContext;
import com.ibm.wsspi.threadcontext.ThreadContextDescriptor;

/**
 *
 */
public class ContextSnapshotImpl implements ContextSnapshot {

    private static final TraceComponent tc = Tr.register(ContextSnapshotImpl.class);

    private final ThreadContextDescriptor threadContextDescriptor;
    private final RequestContextController requestContextController;

    /**
     * @param threadContextDescriptor
     */
    public ContextSnapshotImpl(ThreadContextDescriptor threadContextDescriptor, RequestContextController requestContextController) {
        this.threadContextDescriptor = threadContextDescriptor;
        this.requestContextController = requestContextController;
    }

    @Override
    public void runWithContext(Runnable runnable) {
        ArrayList<ThreadContext> contextsApplied = threadContextDescriptor.taskStarting();
        try {
            activate(requestContextController);
            try {
                runnable.run();
            } finally {
                deactivate(requestContextController);
            }
        } finally {
            threadContextDescriptor.taskStopping(contextsApplied);
        }
    }

    @Override
    public <V> V runWithContext(Callable<V> callable) throws Exception {
        ArrayList<ThreadContext> contextsApplied = threadContextDescriptor.taskStarting();
        try {
            activate(requestContextController);
            try {
                return callable.call();
            } finally {
                deactivate(requestContextController);
            }
        } finally {
            threadContextDescriptor.taskStopping(contextsApplied);
        }
    }

    private void activate(RequestContextController requestContextController) {
        if (requestContextController != null) {
            requestContextController.activate();
        }
    }

    private void deactivate(RequestContextController requestContextController) {
        if (requestContextController != null) {
            try {
                requestContextController.deactivate();
            } catch (ContextNotActiveException e) {
                // If the application is shut down during the execution, the context may have already been deactivated
            }
        }
    }

}
