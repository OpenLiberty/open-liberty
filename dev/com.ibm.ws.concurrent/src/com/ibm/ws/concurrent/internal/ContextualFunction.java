/*******************************************************************************
 * Copyright (c) 2018,2019 IBM Corporation and others.
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
import java.util.function.Function;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.concurrent.ContextualAction;
import com.ibm.wsspi.threadcontext.ThreadContext;
import com.ibm.wsspi.threadcontext.ThreadContextDescriptor;

/**
 * Proxy for Function that applies thread context before running and removes it afterward
 *
 * @param <T> type of the function's parameter
 * @param <R> type of the function's result
 */
class ContextualFunction<T, R> implements Function<T, R>, ContextualAction<Function<T, R>> {
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

    @Override
    @Trivial
    public Function<T, R> getAction() {
        return action;
    }

    @Override
    @Trivial
    public ThreadContextDescriptor getContextDescriptor() {
        return threadContextDescriptor;
    }
}
