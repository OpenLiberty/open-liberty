/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.concurrent.mp.spi;

import org.eclipse.microprofile.context.ManagedExecutor;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.concurrent.mp.ManagedExecutorImpl;
import com.ibm.ws.concurrent.mp.ThreadContextImpl;
import com.ibm.ws.threading.PolicyExecutor;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

/**
 * Provides a static method to enable the ManagedExecutorBuilderImpl,
 * which is in a different bundle, to create ManagedExecutor instances.
 */
@Trivial
public class ManagedExecutorFactory {
    /**
     * Creates a new MicroProfile ManagedExecutor instance.
     *
     * @param managedExecutorName    unique name for the new ManagedExecutor instance.
     * @param threadContextName      unique name for a new ThreadContext instance to be used by the new ManagedExecutor instance.
     * @param hash                   hash code for the new instance.
     * @param policyExecutor         Executor that runs tasks under the concurrency policy for this managed executor.
     * @param config                 represents thread context propagation configuration.
     * @param tranContextProviderRef Reference to the transaction context provider.
     * @return the new instance.
     */
    public static ManagedExecutor createManagedExecutor(String managedExecutorName, String threadContextName, int hash,
                                                        PolicyExecutor policyExecutor, ThreadContextConfig config,
                                                        @SuppressWarnings("deprecation") AtomicServiceReference<com.ibm.wsspi.threadcontext.ThreadContextProvider> tranContextProviderRef) {
        ThreadContextImpl mpThreadContext = new ThreadContextImpl(threadContextName, hash, config);
        return new ManagedExecutorImpl(managedExecutorName, hash, policyExecutor, mpThreadContext, tranContextProviderRef);
    }
}
