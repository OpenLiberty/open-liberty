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
package com.ibm.ws.microprofile.contextpropagation;

import java.util.LinkedHashMap;

import org.eclipse.microprofile.context.spi.ThreadContextProvider;

import com.ibm.ws.concurrent.mp.AbstractThreadContext;
import com.ibm.wsspi.threadcontext.ThreadContextDescriptor;

/**
 * Programmatically built ThreadContext instance - either via ThreadContextBuilder
 * or injected by CDI and possibly annotated by <code>@ThreadContextConfig</code>
 *
 * TODO eventually this should be merged with ContextServiceImpl such that it also
 * implements EE Concurrency ContextService. However, because the MP Context Propagation spec
 * is not covering serializable thread context in its initial version, we must defer
 * this to the future. In the mean time, there will be duplication of the MP Context Propagation
 * method implementations between the two.
 */
class ThreadContextImpl extends AbstractThreadContext {

    /**
     * The context manager provider.
     */
    private final ContextManagerProviderImpl cmProvider;

    /**
     * Map of thread context provider to type of instruction for applying context to threads.
     * The values are either PROPAGATED or CLEARED. Contexts types that should be left on the
     * thread UNCHANGED are omitted from this map.
     */
    private final LinkedHashMap<ThreadContextProvider, ContextOp> configPerProvider;

    /**
     * Construct a new instance to be used directly as a MicroProfile ThreadContext service or by a ManagedExecutor.
     *
     * @param name unique name for this instance.
     * @param int hash hash code for this instance.
     * @param cmProvider the context manager provider
     * @param configPerProvider
     */
    ThreadContextImpl(String name, int hash, ContextManagerProviderImpl cmProvider, LinkedHashMap<ThreadContextProvider, ContextOp> configPerProvider) {
        super(name, hash);
        this.cmProvider = cmProvider;
        this.configPerProvider = configPerProvider;
    }

    @Override
    protected ThreadContextDescriptor createThreadContextDescriptor() {
        return new ThreadContextDescriptorImpl(cmProvider, configPerProvider);
    }
}