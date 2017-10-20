/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.web.liberty;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.ibm.ws.cdi.CDIException;
import com.ibm.ws.cdi.web.impl.AbstractInitialListenerRegistration;
import com.ibm.ws.cdi.web.interfaces.CDIWebRuntime;
import com.ibm.ws.container.service.app.deploy.ModuleInfo;
import com.ibm.ws.container.service.app.deploy.extended.ExtendedModuleInfo;
import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.ws.webcontainer31.osgi.listener.PreEventListenerProvider;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.NonPersistentCache;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.webcontainer.servlet.IServletContext;

/**
 * Register WeldInitalListener on the servlet context. This listener needs to be the first listener.
 */
@Component(name = "com.ibm.ws.cdi.weldInitialListener",
                service = PreEventListenerProvider.class,
                immediate = true,
                property = { "service.vendor=IBM", "service.ranking:Integer=100" })
public class WeldInitialListenerRegistration extends AbstractInitialListenerRegistration implements PreEventListenerProvider {
    
    private final AtomicServiceReference<CDIWebRuntime> cdiWebRuntimeRef = new AtomicServiceReference<CDIWebRuntime>(
                    "cdiWebRuntime");

    protected void activate(ComponentContext context) {
        cdiWebRuntimeRef.activate(context);
    }

    protected void deactivate(ComponentContext context) {
        cdiWebRuntimeRef.deactivate(context);
    }

    protected void unsetCdiWebRuntime(ServiceReference<CDIWebRuntime> ref) {
        cdiWebRuntimeRef.unsetReference(ref);
    }
    
    @Override
    protected CDIWebRuntime getCDIWebRuntime(){
        return cdiWebRuntimeRef.getService();
    }

    @Reference(name = "cdiWebRuntime", service = CDIWebRuntime.class)
    protected void setCdiWebRuntime(ServiceReference<CDIWebRuntime> ref) {
        cdiWebRuntimeRef.setReference(ref);
    }

    private ExtendedModuleInfo getModuleInfo(Container container) throws CDIException {
        ExtendedModuleInfo moduleInfo = null;

        try {
            NonPersistentCache cache = container.adapt(NonPersistentCache.class);
            moduleInfo = (ExtendedModuleInfo) cache.getFromCache(ModuleInfo.class);
        } catch (UnableToAdaptException e) {
            throw new CDIException(e);
        }
        return moduleInfo;
    }

    @Override
    protected ModuleMetaData getModuleMetaData(IServletContext isc) {
        ModuleMetaData moduleMetaData = null;
        try {
            Container container = isc.getModuleContainer();
            ExtendedModuleInfo moduleInfo = getModuleInfo(container);
            moduleMetaData = moduleInfo.getMetaData();
        } catch (CDIException e) {
            throw new RuntimeException(e);
        }
        return moduleMetaData;
    }
}
