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
import java.util.function.Function;

import com.ibm.wsspi.threadcontext.ThreadContext;
import com.ibm.wsspi.threadcontext.ThreadContextDescriptor;

/**
 * Proxy for Function that applies thread context before running and removes it afterward
 *
 * @param <T> type of the function's parameter
 * @param <R> type of the function's result
 */
class ContextualFunction<T, R> implements Function<T, R> {
    private final Function<T, R> action;
    private final ThreadContextDescriptor threadContextDescriptor;

    ContextualFunction(ThreadContextDescriptor threadContextDescriptor, Function<T, R> action) {
        this.action = action;
        this.threadContextDescriptor = threadContextDescriptor;
    }

    @Override
    public R apply(T t) {
        ArrayList<ThreadContext> contextApplied = threadContextDescriptor.taskStarting();
        try {
            return action.apply(t);
        } finally {
            threadContextDescriptor.taskStopping(contextApplied);
        }
    }
}
