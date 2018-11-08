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
package com.ibm.ws.concurrent.service;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.concurrent.internal.ManagedExecutorServiceImpl;
import com.ibm.ws.threading.PolicyExecutor;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.threadcontext.ThreadContextProvider;
import com.ibm.wsspi.threadcontext.WSContextService;

/**
 * Extension point that enables MicroProfile and EE-only implementations to
 * define the OSGi service component differently.
 * This model is only needed to preserve support for Java 7.
 * Once Java 7 is no longer supported, this should all be collapsed
 * back into ManagedExecutorServiceImpl.
 */
@Trivial
public class AbstractManagedExecutorService extends ManagedExecutorServiceImpl {
    /**
     * Constructor for OSGi code path.
     */
    public AbstractManagedExecutorService() {
        super();
    }

    /**
     * Constructor for MicroProfile Concurrency (ManagedExecutorBuilder and CDI injected ManagedExecutor).
     */
    public AbstractManagedExecutorService(String name, PolicyExecutor policyExecutor, WSContextService mpThreadContext,
                                          AtomicServiceReference<ThreadContextProvider> tranContextProviderRef) {
        super(name, policyExecutor, mpThreadContext, tranContextProviderRef);
    }
}
