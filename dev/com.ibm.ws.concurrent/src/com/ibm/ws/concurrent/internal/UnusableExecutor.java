/*******************************************************************************
 * Copyright (c) 2018,2024 IBM Corporation and others.
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

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.BiFunction;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.concurrent.WSManagedExecutorService;
import com.ibm.ws.threading.PolicyExecutor;
import com.ibm.wsspi.threadcontext.ThreadContextDescriptor;
import com.ibm.wsspi.threadcontext.WSContextService;

/**
 * Executor that provides thread context capture/propagation only and is incapable of running tasks.
 * WSManagedExecutorService is implemented to store a context service instance
 * as a convenience to the managed completable future implementation.
 * The execute method is rejected as unsupported.
 */
@Trivial
class UnusableExecutor implements Executor, WSManagedExecutorService {
    private final WSContextService contextService;

    UnusableExecutor(WSContextService contextService) {
        this.contextService = contextService;
    }

    @Override
    @SuppressWarnings("unchecked")
    public ThreadContextDescriptor captureThreadContext(Map<String, String> props) {
        return contextService.captureThreadContext(props);
    }

    @Override
    public void execute(Runnable command) {
        throw new UnsupportedOperationException();
    }

    @Override
    public PolicyExecutor getLongRunningPolicyExecutor() {
        throw new UnsupportedOperationException();
    }

    @Override
    public PolicyExecutor getNormalPolicyExecutor() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int hashCode() {
        return contextService.hashCode(); // for easy correlation in trace with the context service that created it
    }

    @Override
    public <I, T> CompletableFuture<T> newAsyncMethod(BiFunction<I, CompletableFuture<T>, CompletionStage<T>> invoker, I invocation) {
        throw new UnsupportedOperationException();
    }

    @Override
    @Trivial
    public String toString() {
        // Both hashCode and identityHashCode are included so that we can correlate
        // output in Liberty trace, which prints toString for values and method args
        // but uses uses identityHashCode (id=...) when printing trace for a class
        return new StringBuilder(38) //
                        .append("UnusableExecutor@") //
                        .append(Integer.toHexString(hashCode())) //
                        .append("(id=") //
                        .append(Integer.toHexString(System.identityHashCode(this))) //
                        .append(')') //
                        .toString();
    }
}
