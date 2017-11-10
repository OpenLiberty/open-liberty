/*******************************************************************************
 * Copyright (c) 2012, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.impl.inject;

import javax.inject.Inject;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.ibm.ws.cdi.CDIService;
import com.ibm.ws.cdi.internal.interfaces.CDIRuntime;
import com.ibm.wsspi.injectionengine.InjectionProcessorProvider;
import com.ibm.wsspi.injectionengine.InjectionSimpleProcessor;
import com.ibm.wsspi.injectionengine.InjectionSimpleProcessorProvider;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

@Component(name = "com.ibm.ws.cdi.services.injectProcessorProvider", service = InjectionProcessorProvider.class, property = { "service.vendor=IBM" })
public class InjectProcessorProvider extends InjectionSimpleProcessorProvider<Inject> {

    private final AtomicServiceReference<CDIService> cdiServiceRef = new AtomicServiceReference<CDIService>("cdiService");

    @Override
    public InjectionSimpleProcessor<Inject> createInjectionProcessor() {
        CDIService cdiService = cdiServiceRef.getService();
        CDIRuntime cdiRuntime = (CDIRuntime) cdiService;
        return new InjectInjectionProcessor(cdiRuntime);
    }

    @Override
    public Class<Inject> getAnnotationClass() {
        return Inject.class;
    }

    public void activate(ComponentContext context) {
        cdiServiceRef.activate(context);
    }

    public void deactivate(ComponentContext context) {
        cdiServiceRef.deactivate(context);
    }

    @Reference(name = "cdiService", service = CDIService.class)
    protected void setCdiService(ServiceReference<CDIService> ref) {
        cdiServiceRef.setReference(ref);
    }

    protected void unsetCdiService(ServiceReference<CDIService> ref) {
        cdiServiceRef.unsetReference(ref);
    }

}
