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

import java.util.ArrayList;
import java.util.function.Supplier;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.wsspi.threadcontext.ThreadContext;
import com.ibm.wsspi.threadcontext.ThreadContextDescriptor;

/**
 * Runnable that applies thread context before invoking a Supplier and removes the context afterward.
 * Triggers completion of the provided CompletableFuture upon Supplier completion.
 *
 * @param <T> type of the result that is supplied by the supplier
 */
class ContextualSupplierAction<T> implements Runnable {
    private final Supplier<T> action;
    private final ManagedCompletableFuture<T> completableFuture;
    private final boolean superComplete;
    private final ThreadContextDescriptor threadContextDescriptor;

    ContextualSupplierAction(ThreadContextDescriptor threadContextDescriptor, Supplier<T> action,
                             ManagedCompletableFuture<T> completableFuture, boolean superComplete) {
        this.action = action;
        this.completableFuture = completableFuture;
        this.superComplete = superComplete;
        this.threadContextDescriptor = threadContextDescriptor;
    }

    @FFDCIgnore({ Error.class, RuntimeException.class })
    @Override
    public void run() {
        T result = null;
        Throwable failure = null;
        ArrayList<ThreadContext> contextApplied = null;
        try {
            if (threadContextDescriptor != null)
                contextApplied = threadContextDescriptor.taskStarting();
            result = action.get();
        } catch (Error x) {
            failure = x;
            throw x;
        } catch (RuntimeException x) {
            failure = x;
            throw x;
        } finally {
            try {
                if (contextApplied != null)
                    threadContextDescriptor.taskStopping(contextApplied);
            } finally {
                if (failure == null)
                    if (superComplete)
                        completableFuture.super_complete(result);
                    else
                        completableFuture.complete(result);
                else if (superComplete)
                    completableFuture.super_completeExceptionally(failure);
                else
                    completableFuture.completeExceptionally(failure);
            }
        }
    }
}