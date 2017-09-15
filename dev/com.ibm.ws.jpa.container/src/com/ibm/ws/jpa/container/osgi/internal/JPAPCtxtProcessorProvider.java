/*******************************************************************************
 * Copyright (c) 2011, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jpa.container.osgi.internal;

import java.util.Collections;
import java.util.List;

import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContexts;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.ws.javaee.dd.common.JNDIEnvironmentRef;
import com.ibm.ws.javaee.dd.common.PersistenceContextRef;
import com.ibm.ws.jpa.management.JPAPCtxtAttributeAccessor;
import com.ibm.ws.jpa.management.JPAPCtxtProcessor;
import com.ibm.ws.kernel.LibertyProcess;
import com.ibm.wsspi.injectionengine.InjectionProcessor;
import com.ibm.wsspi.injectionengine.InjectionProcessorProvider;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

@Component(service = InjectionProcessorProvider.class)
public class JPAPCtxtProcessorProvider extends InjectionProcessorProvider<PersistenceContext, PersistenceContexts> {
    private static final List<Class<? extends JNDIEnvironmentRef>> REF_CLASSES =
                    Collections.<Class<? extends JNDIEnvironmentRef>> singletonList(PersistenceContextRef.class);

    private static final String REFERENCE_ATTRIBUTE_ACCESSOR = "attributeAccessor";

    private final AtomicServiceReference<JPAPCtxtAttributeAccessor> attributeAccessor = new AtomicServiceReference<JPAPCtxtAttributeAccessor>(REFERENCE_ATTRIBUTE_ACCESSOR);

    @Activate
    protected void activate(ComponentContext cc) {
        attributeAccessor.activate(cc);
    }

    @Deactivate
    protected void deactivate(ComponentContext cc) {
        attributeAccessor.activate(cc);
    }

    @Reference(name = REFERENCE_ATTRIBUTE_ACCESSOR,
               policyOption = ReferencePolicyOption.GREEDY,
               service = JPAPCtxtAttributeAccessor.class)
    protected void setAttributeAccessor(ServiceReference<JPAPCtxtAttributeAccessor> reference) {
        attributeAccessor.setReference(reference);
    }

    protected void unsetAttributeAccessor(ServiceReference<JPAPCtxtAttributeAccessor> reference) {
        attributeAccessor.unsetReference(reference);
    }

    @Reference(service = LibertyProcess.class, target = "(wlp.process.type=server)")
    protected void setLibertyProcess(ServiceReference<LibertyProcess> reference) {

    }

    protected void unsetLibertyProcess(ServiceReference<LibertyProcess> reference) {}

    /** {@inheritDoc} */
    @Override
    public InjectionProcessor<PersistenceContext, PersistenceContexts> createInjectionProcessor() {
        return new JPAPCtxtProcessor(attributeAccessor.getServiceWithException());
    }

    /** {@inheritDoc} */
    @Override
    public Class<PersistenceContext> getAnnotationClass() {
        return PersistenceContext.class;
    }

    /** {@inheritDoc} */
    @Override
    public Class<PersistenceContexts> getAnnotationsClass() {
        return PersistenceContexts.class;
    }

    /** {@inheritDoc} */
    @Override
    public List<Class<? extends JNDIEnvironmentRef>> getJNDIEnvironmentRefClasses() {
        return REF_CLASSES;
    }
}
