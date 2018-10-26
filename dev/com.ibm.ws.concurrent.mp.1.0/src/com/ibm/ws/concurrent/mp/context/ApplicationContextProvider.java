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
package com.ibm.ws.concurrent.mp.context;

import org.eclipse.microprofile.concurrent.ThreadContext;

import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

/**
 * Partial implementation of MicroProfile Application context, backed by Liberty's
 * Classloader Context and JEE Metadata Context.
 */
public class ApplicationContextProvider extends ContainerContextProvider {
    public final AtomicServiceReference<com.ibm.wsspi.threadcontext.ThreadContextProvider> classloaderContextProviderRef = new AtomicServiceReference<com.ibm.wsspi.threadcontext.ThreadContextProvider>("ClassloaderContextProvider");
    public final AtomicServiceReference<com.ibm.wsspi.threadcontext.ThreadContextProvider> jeeMetadataContextProviderRef = new AtomicServiceReference<com.ibm.wsspi.threadcontext.ThreadContextProvider>("JeeMetadataContextProvider");

    @Override
    public com.ibm.wsspi.threadcontext.ThreadContextProvider[] toContainerProviders() {
        return new com.ibm.wsspi.threadcontext.ThreadContextProvider[] { classloaderContextProviderRef.getServiceWithException(),
                                                                         jeeMetadataContextProviderRef.getServiceWithException()
        };
    }

    @Override
    public final String getThreadContextType() {
        return ThreadContext.APPLICATION;
    }
}