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
package com.ibm.ws.jaxrs2x.concurrent.component;

import java.util.concurrent.Executor;

import org.apache.cxf.message.Message;
import org.osgi.service.component.annotations.Component;

import com.ibm.ws.concurrent.WSManagedExecutorService;
import com.ibm.ws.jaxrs20.client.AsyncClientRunnableWrapper;
import com.ibm.wsspi.threadcontext.ThreadContextDescriptor;
import com.ibm.wsspi.threadcontext.WSContextService;

@Component(name = "com.ibm.ws.jaxrs2x.concurrent.component.ManagedExecutorRunnableWrapper",
           service = AsyncClientRunnableWrapper.class,
           immediate = true,
           property = { "service.vendor=IBM" })
public class ManagedExecutorRunnableWrapper implements AsyncClientRunnableWrapper {

    @Override
    public void prepare(Message message) {
        Executor executor = message.getExchange().get(Executor.class);
        if (executor instanceof WSManagedExecutorService) {
            WSContextService contextService = ((WSManagedExecutorService) executor).getContextService();
            final ThreadContextDescriptor tcd = contextService.captureThreadContext(null, null);
            message.put(ThreadContextDescriptor.class, tcd);
        }
    }

    @Override
    public Runnable wrap(Message message, Runnable runnable) {
        Runnable ret = runnable;
        Executor executor = message.getExchange().get(Executor.class);
        if (executor instanceof WSManagedExecutorService) {
            ThreadContextDescriptor tcd = message.get(ThreadContextDescriptor.class);
            if (tcd != null) {
                WSContextService contextService = ((WSManagedExecutorService) executor).getContextService();
                ret = contextService.createContextualProxy(tcd, runnable, Runnable.class);
            }

        }
        return ret;
    }
}
