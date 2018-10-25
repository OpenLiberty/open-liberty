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
import java.util.function.Consumer;

import com.ibm.wsspi.threadcontext.ThreadContext;
import com.ibm.wsspi.threadcontext.ThreadContextDescriptor;

/**
 * Proxy for Consumer that applies thread context before running and removes it afterward
 *
 * @param <T> type of the consumer's parameter
 */
class ContextualConsumer<T> implements Consumer<T> {
    private final Consumer<T> action;
    private final ThreadContextDescriptor threadContextDescriptor;

    ContextualConsumer(ThreadContextDescriptor threadContextDescriptor, Consumer<T> action) {
        this.action = action;
        this.threadContextDescriptor = threadContextDescriptor;
    }

    @Override
    public void accept(T t) {
        ArrayList<ThreadContext> contextApplied = threadContextDescriptor.taskStarting();
        try {
            action.accept(t);
        } finally {
            threadContextDescriptor.taskStopping(contextApplied);
        }
    }
}
