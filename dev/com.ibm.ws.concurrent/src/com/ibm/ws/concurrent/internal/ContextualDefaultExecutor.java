/*******************************************************************************
 * Copyright (c) 2020,2024 IBM Corporation and others.
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

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.BiFunction;

import org.eclipse.microprofile.context.ManagedExecutor;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.concurrent.WSManagedExecutorService;
import com.ibm.ws.threading.PolicyExecutor;
import com.ibm.wsspi.threadcontext.ThreadContextDescriptor;
import com.ibm.wsspi.threadcontext.WSContextService;

/**
 * Executor that provides thread context capture/propagation according to the specified MP ThreadContext,
 * but runs tasks on the DefaultManagedExecutorService, using its PolicyExecutor.
 * WSManagedExecutorService is implemented to store a context service instance
 * as a convenience to the managed completable future implementation.
 * This class is used for the withContextCapture methods.
 */
class ContextualDefaultExecutor implements Executor, WSManagedExecutorService {
    private final WSContextService contextService;
    private final WSManagedExecutorService defaultManagedExecutor;

    ContextualDefaultExecutor(WSContextService contextService) {
        this.contextService = contextService;
        this.defaultManagedExecutor = (WSManagedExecutorService) AccessController.doPrivileged(new PrivilegedAction<ManagedExecutor>() {
            @Override
            public ManagedExecutor run() {
                BundleContext bundleContext = FrameworkUtil.getBundle(WSManagedExecutorService.class).getBundleContext();
                Collection<ServiceReference<ManagedExecutor>> refs;
                try {
                    refs = bundleContext.getServiceReferences(ManagedExecutor.class, "(id=DefaultManagedExecutorService)");
                } catch (InvalidSyntaxException x) {
                    throw new RuntimeException(x);
                }
                if (refs.isEmpty())
                    return null;
                else
                    return bundleContext.getService(refs.iterator().next());
            }
        });
        if (defaultManagedExecutor == null)
            throw new IllegalStateException("DefaultManagedExecutorService");
    }

    @Override
    @SuppressWarnings("unchecked")
    public ThreadContextDescriptor captureThreadContext(Map<String, String> props) {
        return contextService.captureThreadContext(props);
    }

    @Override
    public void execute(Runnable command) {
        defaultManagedExecutor.getNormalPolicyExecutor().execute(command);
    }

    @Override
    @Trivial
    public PolicyExecutor getLongRunningPolicyExecutor() {
        return defaultManagedExecutor.getLongRunningPolicyExecutor();
    }

    @Override
    @Trivial
    public PolicyExecutor getNormalPolicyExecutor() {
        return defaultManagedExecutor.getNormalPolicyExecutor();
    }

    @Override
    @Trivial
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
        return new StringBuilder(47) //
                        .append("ContextualDefaultExecutor@") //
                        .append(Integer.toHexString(hashCode())) //
                        .append("(id=") //
                        .append(Integer.toHexString(System.identityHashCode(this))) //
                        .append(')') //
                        .toString();
    }
}
