/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.reactive.streams.operators30.spi.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import com.ibm.wsspi.kernel.service.utils.FrameworkState;
import com.ibm.wsspi.threadcontext.ThreadContext;
import com.ibm.wsspi.threadcontext.ThreadContextDescriptor;
import com.ibm.wsspi.threadcontext.WSContextService;

/**
 * Executor used for running reactive streams.
 * <p>
 * Propagates classloader, JavaEE metadata and security context
 */
@Component(immediate = true, service = RsoContextExecutor.class, configurationPolicy = ConfigurationPolicy.IGNORE)
public class RsoContextExecutor implements Executor {

    @SuppressWarnings("unchecked")
    private static final Map<String, ?>[] THREAD_CONTEXT_PROVIDERS = new Map[] {
                                                                                 Collections.singletonMap(WSContextService.THREAD_CONTEXT_PROVIDER,
                                                                                                          "com.ibm.ws.classloader.context.provider"),
                                                                                 Collections.singletonMap(WSContextService.THREAD_CONTEXT_PROVIDER,
                                                                                                          "com.ibm.ws.javaee.metadata.context.provider"),
                                                                                 Collections.singletonMap(WSContextService.THREAD_CONTEXT_PROVIDER,
                                                                                                          "com.ibm.ws.security.context.provider"),
    };

    // Filter selects the WSContextService internal singleton which is not user configurable
    @Reference(target = "(service.pid=com.ibm.ws.context.manager)")
    private WSContextService contextService;

    // Filter selects the liberty global thread pool executor service
    @Reference(target = "(component.name=com.ibm.ws.threading)")
    private ExecutorService globalExecutor;

    private static AtomicReference<RsoContextExecutor> currentInstance = new AtomicReference<>();

    @Activate
    protected void activate() {
        currentInstance.set(this);
    }

    @Deactivate
    protected void deactivate() {
        currentInstance.compareAndSet(this, null);
    }

    /** {@inheritDoc} */
    @Override
    public void execute(Runnable command) {
        ThreadContextDescriptor contextDescriptor = contextService.captureThreadContext(null, THREAD_CONTEXT_PROVIDERS);
        globalExecutor.execute(() -> {
            ArrayList<ThreadContext> oldContext = contextDescriptor.taskStarting();
            try {
                command.run();
            } finally {
                contextDescriptor.taskStopping(oldContext);
            }
        });
    }

    /**
     * Gets the current instance
     *
     * @return the executor
     * @throws IllegalStateException if an instance is not available
     */
    public static RsoContextExecutor getInstance() throws IllegalStateException {
        if (!FrameworkState.isValid()) {
            throw new IllegalStateException("Invalid OSGi Framework State");
        }

        RsoContextExecutor service = currentInstance.get();

        if (service == null) {
            throw new IllegalStateException("A executor for Reactive Streams could not be found");
        }

        return service;
    }

}
