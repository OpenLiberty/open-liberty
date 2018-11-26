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
import java.util.function.BiFunction;

import com.ibm.wsspi.threadcontext.ThreadContext;
import com.ibm.wsspi.threadcontext.ThreadContextDescriptor;

/**
 * Proxy for BiFunction that applies thread context before running and removes it afterward
 *
 * @param <T> type of the function's first parameter
 * @param <U> type of the function's second parameter
 * @param <R> type of the function's result
 */
class ContextualBiFunction<T, U, R> implements BiFunction<T, U, R> {
    private final BiFunction<T, U, R> action;
    private final ThreadContextDescriptor threadContextDescriptor;

    ContextualBiFunction(ThreadContextDescriptor threadContextDescriptor, BiFunction<T, U, R> action) {
        this.action = action;
        this.threadContextDescriptor = threadContextDescriptor;
    }

    @Override
    public R apply(T t, U u) {
        ArrayList<ThreadContext> contextApplied = threadContextDescriptor.taskStarting();
        try {
            return action.apply(t, u);
        } finally {
            threadContextDescriptor.taskStopping(contextApplied);
        }
    }
}