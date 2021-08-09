/*******************************************************************************
 * Copyright (c) 2012, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.impl.managedobject;

import java.security.AccessController;
import java.security.PrivilegedAction;

import javax.servlet.jsp.HttpJspPage;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.ibm.ws.cdi.CDIService;
import com.ibm.ws.cdi.internal.interfaces.CDIRuntime;
import com.ibm.ws.managedobject.DefaultManagedObjectService;
import com.ibm.ws.managedobject.ManagedObjectException;
import com.ibm.ws.managedobject.ManagedObjectFactory;
import com.ibm.ws.managedobject.ManagedObjectService;
import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.wsspi.injectionengine.ReferenceContext;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

@Component(name = "com.ibm.ws.cdi.impl.managedobject.CDIManagedObjectService", service = { ManagedObjectService.class }, immediate = true, property = { "service.vendor=IBM",
                                                                                                                                                        "service.ranking:Integer=9999" })
public class CDIManagedObjectService implements ManagedObjectService {

    private final AtomicServiceReference<CDIService> cdiServiceRef = new AtomicServiceReference<CDIService>("cdiService");
    private final AtomicServiceReference<DefaultManagedObjectService> defaultMOSRef = new AtomicServiceReference<DefaultManagedObjectService>("defaultManagedObjectService");

    private CDIRuntime cdiRuntime;

    public void activate(ComponentContext cc) {
        cdiServiceRef.activate(cc);
        defaultMOSRef.activate(cc);
    }

    public void deactivate(ComponentContext cc) {
        cdiServiceRef.deactivate(cc);
        defaultMOSRef.deactivate(cc);
        cdiRuntime = null;
    }

    @Reference(name = "defaultManagedObjectService", service = DefaultManagedObjectService.class, policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MANDATORY)
    protected void setDefaultManagedObjectService(ServiceReference<DefaultManagedObjectService> ref) {
        defaultMOSRef.setReference(ref);
    }

    protected void unsetDefaultManagedObjectService(ServiceReference<DefaultManagedObjectService> ref) {
        defaultMOSRef.unsetReference(ref);
    }

    private DefaultManagedObjectService getDefaultManagedObjectService() {
        return defaultMOSRef.getServiceWithException();
    }

    @Reference(name = "cdiService", service = CDIService.class, policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MANDATORY)
    protected void setCDIService(ServiceReference<CDIService> ref) {
        cdiServiceRef.setReference(ref);
    }

    protected void unsetCDIService(ServiceReference<CDIService> ref) {
        cdiServiceRef.unsetReference(ref);
    }

    private CDIService getCDIService() {
        return AccessController.doPrivileged(new PrivilegedAction<CDIService>() {
            @Override
            public CDIService run() {
                return cdiServiceRef.getServiceWithException();
            }
        });

    }

    private CDIRuntime getCDIRuntime() {
        if (this.cdiRuntime == null) {
            this.cdiRuntime = (CDIRuntime) getCDIService();
        }
        return this.cdiRuntime;
    }

    @Override
    public <T> ManagedObjectFactory<T> createManagedObjectFactory(ModuleMetaData mmd, Class<T> klass,
                                                                  boolean requestManagingInjectionAndInterceptors) throws ManagedObjectException {
        if (!HttpJspPage.class.isAssignableFrom(klass) && isCDIEnabled(mmd)) {
            return new CDIManagedObjectFactoryImpl<T>(klass, getCDIRuntime(), requestManagingInjectionAndInterceptors);
        } else {
            return getDefaultManagedObjectService().createManagedObjectFactory(mmd, klass, requestManagingInjectionAndInterceptors);
        }
    }

    @Override
    public <T> ManagedObjectFactory<T> createEJBManagedObjectFactory(ModuleMetaData mmd, Class<T> klass, String ejbName) throws ManagedObjectException {
        ManagedObjectFactory<T> defaultEJBManagedObjectFactory = getDefaultManagedObjectService().createEJBManagedObjectFactory(mmd, klass, ejbName);
        if (isCDIEnabled(mmd)) {
            return new CDIEJBManagedObjectFactoryImpl<T>(klass, ejbName, getCDIRuntime(), defaultEJBManagedObjectFactory);
        } else {
            return defaultEJBManagedObjectFactory;
        }
    }

    @Override
    public <T> ManagedObjectFactory<T> createInterceptorManagedObjectFactory(ModuleMetaData mmd, Class<T> klass) throws ManagedObjectException {
        if (isCDIEnabled(mmd)) {
            return new CDIInterceptorManagedObjectFactoryImpl<T>(klass, getCDIRuntime());
        } else {
            return getDefaultManagedObjectService().createInterceptorManagedObjectFactory(mmd, klass);
        }
    }

    private boolean isCDIEnabled(ModuleMetaData mmd) {
        return getCDIRuntime() != null && getCDIRuntime().isModuleCDIEnabled(mmd);
    }

    /** {@inheritDoc} */
    @Override
    public <T> ManagedObjectFactory<T> createManagedObjectFactory(ModuleMetaData mmd, Class<T> klass, boolean requestManagingInjectionAndInterceptors,
                                                                  ReferenceContext referenceContext) throws ManagedObjectException {
        if (isCDIEnabled(mmd)) {
            return new CDIManagedObjectFactoryImpl<T>(klass, getCDIRuntime(), requestManagingInjectionAndInterceptors, referenceContext);
        } else {
            return getDefaultManagedObjectService().createManagedObjectFactory(mmd, klass, requestManagingInjectionAndInterceptors, referenceContext);
        }
    }
}
