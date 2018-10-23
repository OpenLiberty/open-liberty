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

import com.ibm.wsspi.threadcontext.ThreadContext;
import com.ibm.wsspi.threadcontext.ThreadContextDescriptor;

/**
 * Proxy for Supplier that applies thread context before running and removes it afterward
 *
 * @param <T> type of the result that is supplied by the supplier
 */
class ContextualSupplier<T> implements Supplier<T> {
    private final Supplier<T> action;
    private final ThreadContextDescriptor threadContextDescriptor;

    ContextualSupplier(ThreadContextDescriptor threadContextDescriptor, Supplier<T> action) {
        this.action = action;
        this.threadContextDescriptor = threadContextDescriptor;
    }

    @Override
    public T get() {
        ArrayList<ThreadContext> contextApplied = threadContextDescriptor.taskStarting();
        try {
            return action.get();
        } finally {
            threadContextDescriptor.taskStopping(contextApplied);
        }
    }
}