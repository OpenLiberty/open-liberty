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
package com.ibm.ws.concurrent.internal;

import java.util.ArrayList;
import java.util.concurrent.Executor;

import com.ibm.wsspi.threadcontext.ThreadContext;
import com.ibm.wsspi.threadcontext.ThreadContextDescriptor;

/**
 * Executor that serves as a reusable snapshot of thread context.
 */
class ContextualExecutor implements Executor {
    private final ThreadContextDescriptor threadContextDescriptor;

    ContextualExecutor(ThreadContextDescriptor threadContextDescriptor) {
        this.threadContextDescriptor = threadContextDescriptor;
    }

    @Override
    public void execute(Runnable action) {
        if (action instanceof ContextualRunnable)
            throw new IllegalArgumentException(ContextualRunnable.class.getSimpleName());

        ThreadContextDescriptor tcd = threadContextDescriptor.clone();
        ArrayList<ThreadContext> contextApplied = tcd.taskStarting();
        try {
            action.run();
        } finally {
            tcd.taskStopping(contextApplied);
        }
    }
}
