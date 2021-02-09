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

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.concurrent.ContextualAction;
import com.ibm.wsspi.threadcontext.ThreadContext;
import com.ibm.wsspi.threadcontext.ThreadContextDescriptor;

/**
 * Proxy for Runnable that applies thread context before running and removes it afterward.
 * If a CompletableFuture is supplied, triggers its completion upon completion of the Runnable.
 */
class ContextualRunnable implements Runnable, ContextualAction<Runnable> {
    private final Runnable action;
    private final ManagedCompletableFuture<Void> completableFuture;
    private final ThreadContextDescriptor threadContextDescriptor;

    ContextualRunnable(ThreadContextDescriptor threadContextDescriptor, Runnable action) {
        this.action = action;
        this.completableFuture = null;
        this.threadContextDescriptor = threadContextDescriptor;
    }

    ContextualRunnable(ThreadContextDescriptor threadContextDescriptor, Runnable action, ManagedCompletableFuture<Void> completableFuture) {
        this.action = action;
        this.completableFuture = completableFuture;
        this.threadContextDescriptor = threadContextDescriptor;
    }

    @Override
    @Trivial
    public Runnable getAction() {
        return action;
    }

    @Override
    @Trivial
    public ThreadContextDescriptor getContextDescriptor() {
        return threadContextDescriptor;
    }

    @Override
    public void run() {
        Throwable failure = null;
        ArrayList<ThreadContext> contextApplied = null;
        try {
            if (threadContextDescriptor != null)
                contextApplied = threadContextDescriptor.taskStarting();
            action.run();
        } catch (Error | RuntimeException x) {
            failure = x;
            throw x;
        } finally {
            try {
                if (contextApplied != null)
                    threadContextDescriptor.taskStopping(contextApplied);
            } catch (RuntimeException x) {
                failure = x;
                throw x;
            } finally {
                if (completableFuture != null)
                    if (failure == null)
                        completableFuture.super_complete(null);
                    else
                        completableFuture.super_completeExceptionally(failure);
            }
        }
    }
}
