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
package com.ibm.ws.microprofile.contextpropagation;

import java.util.LinkedHashMap;

import org.eclipse.microprofile.context.spi.ThreadContextProvider;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.concurrent.mp.spi.ThreadContextConfig;
import com.ibm.wsspi.threadcontext.ThreadContextDescriptor;

/**
 * Represents thread context configuration for ThreadContext and ManagedExecutor instances.
 * Configuration is represented as a Map of thread context provider to type of instruction for applying context to threads.
 * The values are either PROPAGATED or CLEARED. Contexts types that should be left on the
 * thread UNCHANGED are omitted from this map.
 */
@Trivial
class ThreadContextConfigImpl extends LinkedHashMap<ThreadContextProvider, ContextOp> implements ThreadContextConfig {
    private static final long serialVersionUID = 1L;

    /**
     * The context manager provider.
     */
    private final ContextManagerProviderImpl cmProvider;

    ThreadContextConfigImpl(ContextManagerProviderImpl cmProvider) {
        super();
        this.cmProvider = cmProvider;
    }

    @Override
    public ThreadContextDescriptor captureThreadContext() {
        return new ThreadContextDescriptorImpl(cmProvider, this);
    }
}
