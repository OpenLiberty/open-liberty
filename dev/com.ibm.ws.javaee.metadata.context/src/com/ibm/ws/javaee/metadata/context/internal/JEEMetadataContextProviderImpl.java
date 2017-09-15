/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.metadata.context.internal;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.List;
import java.util.Map;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.ibm.ws.container.service.metadata.extended.MetaDataIdentifierService;
import com.ibm.ws.javaee.metadata.context.ComponentMetaDataDecorator;
import com.ibm.ws.javaee.metadata.context.JEEMetadataThreadContextFactory;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap;
import com.ibm.wsspi.threadcontext.ThreadContext;
import com.ibm.wsspi.threadcontext.ThreadContextDeserializationInfo;
import com.ibm.wsspi.threadcontext.ThreadContextProvider;

/**
 * Java EE application component context service provider.
 */
@Component(name = "com.ibm.ws.javaee.metadata.context.provider", configurationPolicy = ConfigurationPolicy.IGNORE)
public class JEEMetadataContextProviderImpl implements ThreadContextProvider, JEEMetadataThreadContextFactory {
    final ConcurrentServiceReferenceMap<String, ComponentMetaDataDecorator> componentMetadataDecoratorRefs = new ConcurrentServiceReferenceMap<String, ComponentMetaDataDecorator>("componentMetadataDecorator");

    /**
     * The MetaDataIdentifierService.
     */
    protected MetaDataIdentifierService metadataIdentifierService;

    /**
     * Called during service activation.
     * 
     * @param context The component context.
     */
    @Activate
    protected void activate(ComponentContext context) {
        componentMetadataDecoratorRefs.activate(context);
    }

    /**
     * Called during service deactivation.
     * 
     * @param context The component context.
     */
    @Deactivate
    protected void deactivate(ComponentContext context) {
        componentMetadataDecoratorRefs.deactivate(context);
    }

    /** {@inheritDoc} */
    @Override
    public ThreadContext captureThreadContext(Map<String, String> execProps, Map<String, ?> threadContextConfig) {
        return new JEEMetadataContextImpl(this);
    }

    /** {@inheritDoc} */
    @Override
    public ThreadContext createDefaultThreadContext(Map<String, String> execProps) {
        // instance can be reused because it does not store state information about previous thread context
        return JEEMetadataContextImpl.EMPTY_CONTEXT;
    }

    /** {@inheritDoc} */
    @Override
    public ThreadContext deserializeThreadContext(ThreadContextDeserializationInfo info, byte[] bytes) throws ClassNotFoundException, IOException {
        ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes));
        JEEMetadataContextImpl context;
        try {
            context = (JEEMetadataContextImpl) in.readObject();
        } finally {
            in.close();
        }

        if (!context.beginDefaultContext) {
            context.metaDataIdentifier = info.getMetadataIdentifier();
            context.jeeMetadataContextProvider = this;
        }

        return context;
    }

    /**
     * @see com.ibm.wsspi.threadcontext.ThreadContextProvider#getPrerequisites()
     */
    @Override
    public List<ThreadContextProvider> getPrerequisites() {
        return null;
    }

    /**
     * Declarative Services method for adding a component metadata decorator.
     * 
     * @param ref reference to the service
     */
    @Reference(service = ComponentMetaDataDecorator.class, name = "componentMetadataDecorator",
               cardinality = ReferenceCardinality.MULTIPLE,
               policy = ReferencePolicy.DYNAMIC)
    protected void setComponentMetadataDecorator(ServiceReference<ComponentMetaDataDecorator> ref) {
        componentMetadataDecoratorRefs.putReference((String) ref.getProperty("component.name"), ref);
    }

    /**
     * Declarative Services method for setting the metadata identifier service.
     * 
     * @param svc the service
     */
    @Reference(service = MetaDataIdentifierService.class, name = "metadataIdentifierService")
    protected void setMetadataIdentifierService(MetaDataIdentifierService svc) {
        metadataIdentifierService = svc;
    }

    /**
     * Declarative Services method for removing a component metadata decorator.
     * 
     * @param ref reference to the service
     */
    protected void unsetComponentMetadataDecorator(ServiceReference<ComponentMetaDataDecorator> ref) {
        componentMetadataDecoratorRefs.removeReference((String) ref.getProperty("component.name"), ref);
    }

    /**
     * Declarative Services method for unsetting the metadata identifier service.
     * 
     * @param svc the service
     */
    protected void unsetMetadataIdentifierService(MetaDataIdentifierService svc) {
        metadataIdentifierService = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ThreadContext createThreadContext(Map<String, String> execProps, String metadataIdentifier) {
        return new JEEMetadataContextImpl(this, metadataIdentifier);
    }
}
