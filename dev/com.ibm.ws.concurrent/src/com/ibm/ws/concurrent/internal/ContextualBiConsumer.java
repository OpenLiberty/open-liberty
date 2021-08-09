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
import java.util.function.BiConsumer;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.concurrent.ContextualAction;
import com.ibm.wsspi.threadcontext.ThreadContext;
import com.ibm.wsspi.threadcontext.ThreadContextDescriptor;

/**
 * Proxy for BiConsumer that applies thread context before running and removes it afterward
 *
 * @param <T> type of the function's parameter
 * @param <R> type of the function's result
 */
class ContextualBiConsumer<T, U> implements BiConsumer<T, U>, ContextualAction<BiConsumer<T, U>> {
    private final BiConsumer<T, U> action;
    private final ThreadContextDescriptor threadContextDescriptor;

    ContextualBiConsumer(ThreadContextDescriptor threadContextDescriptor, BiConsumer<T, U> action) {
        this.action = action;
        this.threadContextDescriptor = threadContextDescriptor;
    }

    @Override
    public void accept(T t, U u) {
        ArrayList<ThreadContext> contextApplied = threadContextDescriptor.taskStarting();
        try {
            action.accept(t, u);
        } finally {
            threadContextDescriptor.taskStopping(contextApplied);
        }
    }

    @Override
    @Trivial
    public BiConsumer<T, U> getAction() {
        return action;
    }

    @Override
    @Trivial
    public ThreadContextDescriptor getContextDescriptor() {
        return threadContextDescriptor;
    }
}