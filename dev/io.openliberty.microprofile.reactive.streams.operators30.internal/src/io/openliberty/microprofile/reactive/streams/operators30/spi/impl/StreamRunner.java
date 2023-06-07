/*******************************************************************************
 * Copyright (c) 2018, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.reactive.streams.operators30.spi.impl;

import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;

import com.ibm.wsspi.threadcontext.ThreadContextDescriptor;
import com.ibm.wsspi.threadcontext.WSContextService;

public class StreamRunner<T> {

    private final ExecutorService executorService;
    private final PrivilegedAction<CompletionStage<T>> action;
    private final WSContextService contextService;
    private final ThreadContextDescriptor context;

    /**
     * The collection of contexts to capture under createThreadContext. Classloader,
     * JeeMetadata, and security.
     */
    @SuppressWarnings("unchecked")
    private static final Map<String, ?>[] THREAD_CONTEXT_PROVIDERS = new Map[] {
                                                                                 Collections.singletonMap(WSContextService.THREAD_CONTEXT_PROVIDER,
                                                                                                          "com.ibm.ws.classloader.context.provider"),
                                                                                 Collections.singletonMap(WSContextService.THREAD_CONTEXT_PROVIDER,
                                                                                                          "com.ibm.ws.javaee.metadata.context.provider"),
                                                                                 Collections.singletonMap(WSContextService.THREAD_CONTEXT_PROVIDER,
                                                                                                          "com.ibm.ws.security.context.provider"),
    };

    public StreamRunner(ExecutorService executor, WSContextService contextService,
                        PrivilegedAction<CompletionStage<T>> action) {
        this.executorService = executor;
        this.contextService = contextService;
        this.action = action;
        if (contextService != null) {
            this.context = contextService.captureThreadContext(null, THREAD_CONTEXT_PROVIDERS);
        } else {
            this.context = null;
        }

    }

    public StreamTask<T> startStream() {

        StreamTask<T> streamTask = new StreamTask<T>(action);

        Runnable task;
        if ((contextService != null) && (context != null)) {
            task = contextService.createContextualProxy(context, streamTask, Runnable.class);
        } else {
            task = streamTask;
        }

        executorService.execute(task);

        return streamTask;
    }

}
