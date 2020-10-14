/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.thread.zos.context.internal;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

import com.ibm.wsspi.threadcontext.ThreadContext;
import com.ibm.wsspi.threadcontext.ThreadContextDeserializationInfo;
import com.ibm.wsspi.threadcontext.ThreadContextProvider;

/**
 * This class provides instances of ThreadIdentityContextImpl objects, which
 * can be used to establish thread identity for asynchronous work.
 */
@Component(name = "com.ibm.ws.security.thread.zos.context.provider", configurationPolicy = ConfigurationPolicy.IGNORE)
public class ThreadIdentityContextProviderImpl implements ThreadContextProvider {
    private final List<ThreadContextProvider> prerequisites = new ArrayList<ThreadContextProvider>(2);

    /** {@inheritDoc} */
    @Override
    public ThreadContext captureThreadContext(Map<String, String> execProps, Map<String, ?> threadContextConfig) {
        return new ThreadIdentityContextImpl();
    }

    /** {@inheritDoc} */
    @Override
    public ThreadContext createDefaultThreadContext(Map<String, String> execProps) {
        return new ThreadIdentityContextImpl().setIsDefaultContext(true);
    }

    /** {@inheritDoc} */
    @Override
    public ThreadContext deserializeThreadContext(ThreadContextDeserializationInfo info, byte[] bytes) throws ClassNotFoundException, IOException {
        ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes));
        ThreadIdentityContextImpl context = null;
        try {
            context = (ThreadIdentityContextImpl) in.readObject();
        } finally {
            in.close();
        }
        return context;
    }

    /**
     * List of required contexts.
     *
     * securityContext: for propagating the caller and runAs Subjects.
     * jeeMetadataContext: for propagating the app's ComponentMetaData.
     */
    @Override
    public List<ThreadContextProvider> getPrerequisites() {
        return prerequisites;
    }

    @Reference(target = "(component.name=com.ibm.ws.javaee.metadata.context.provider)")
    protected void setJeeMetadataContextProvider(ThreadContextProvider svc) {
        prerequisites.add(svc);
    }

    protected void unsetJeeMetadataContextProvider(ThreadContextProvider svc) {
        prerequisites.remove(svc);
    }

    @Reference(target = "(component.name=com.ibm.ws.security.context.provider)")
    protected void setSecurityContextProvider(ThreadContextProvider svc) {
        prerequisites.add(svc);
    }

    protected void unsetSecurityContextProvider(ThreadContextProvider svc) {
        prerequisites.remove(svc);
    }
}
