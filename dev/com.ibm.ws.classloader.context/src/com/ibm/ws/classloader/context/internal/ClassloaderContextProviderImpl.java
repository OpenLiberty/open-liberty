/*******************************************************************************
 * Copyright (c) 2012, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.classloader.context.internal;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.List;
import java.util.Map;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;

import com.ibm.ws.classloader.context.ClassLoaderThreadContextFactory;
import com.ibm.ws.classloading.ClassLoaderIdentifierService;
import com.ibm.wsspi.threadcontext.ThreadContext;
import com.ibm.wsspi.threadcontext.ThreadContextDeserializationInfo;
import com.ibm.wsspi.threadcontext.ThreadContextProvider;

/**
 * Classloader thread context provider.
 */
@Component(name = "com.ibm.ws.classloader.context.provider", configurationPolicy = ConfigurationPolicy.IGNORE)
public class ClassloaderContextProviderImpl implements ThreadContextProvider, ClassLoaderThreadContextFactory {

    /**
     * The ClassLoaderIdentifierService.
     */
    protected ClassLoaderIdentifierService classLoaderIdentifierService;

    /**
     * Called during service activation.
     * 
     * @param context The component context.
     * @param properties The service properties.
     */
    @Activate
    protected void activate(ComponentContext context, Map<String, Object> properties) {}

    @Modified
    protected void modified(ComponentContext context) {}

    /**
     * Called during service deactivation.
     */
    @Deactivate
    protected void deactivate() {}

    /**
     * Declarative Services method for setting the classloader identifier service.
     * 
     * @param svc the service
     */
    @Reference(service = ClassLoaderIdentifierService.class, name = "classLoaderIdentifierService")
    protected void setClassLoaderIdentifierService(ClassLoaderIdentifierService svc) {
        classLoaderIdentifierService = svc;
    }

    /**
     * Declarative Services method for unsetting the classloader identifier service.
     * 
     * @param svc the service
     */
    protected void unsetClassLoaderIdentifierService(ClassLoaderIdentifierService svc) {
        classLoaderIdentifierService = null;
    }

    /** {@inheritDoc} */
    @Override
    public ThreadContext captureThreadContext(Map<String, String> execProps, Map<String, ?> threadContextConfig) {
        return new ClassloaderContextImpl(this);
    }

    /**
     * @see com.ibm.wsspi.threadcontext.ThreadContextProvider#getDefaultThreadContext()
     */
    @Override
    public ThreadContext createDefaultThreadContext(Map<String, String> execProps) {
        return new ClassloaderContextImpl(this, ClassloaderContextImpl.SYSTEM_CLASS_LOADER);
    }

    /** {@inheritDoc} */
    @Override
    public ThreadContext deserializeThreadContext(ThreadContextDeserializationInfo info, byte[] bytes) throws ClassNotFoundException, IOException {
        ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes));
        ClassloaderContextImpl context = null;
        try {
            context = (ClassloaderContextImpl) in.readObject();
        } finally {
            in.close();
        }

        if (context != null)
            context.classLoaderContextProvider = this;

        return context;
    }

    /**
     * @see com.ibm.wsspi.threadcontext.ThreadContextProvider#getPrerequisites()
     */
    @Override
    public List<ThreadContextProvider> getPrerequisites() {
        return null;
    }

    String getClassLoaderIdentifier(ClassLoader cl) {
        return classLoaderIdentifierService.getClassLoaderIdentifier(cl);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ThreadContext createThreadContext(Map<String, String> execProps, String classloaderIdentifier) {
        return new ClassloaderContextImpl(this, classloaderIdentifier);
    }
}