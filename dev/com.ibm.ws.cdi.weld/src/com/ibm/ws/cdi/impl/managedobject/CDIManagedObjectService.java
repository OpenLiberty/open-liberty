/*******************************************************************************
 * Copyright (c) 2012, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.impl.managedobject;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.servlet.jsp.HttpJspPage;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
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

@Component(name = "com.ibm.ws.cdi.impl.managedobject.CDIManagedObjectService", service = { ManagedObjectService.class }, immediate = true, property = {                                                                                                                                                         "service.ranking:Integer=9999" })
public class CDIManagedObjectService implements ManagedObjectService {

    private final DefaultManagedObjectService defaultMOS;
    private final CDIRuntime cdiRuntime;
    //CDI can get deactivated after this object is created, so we need to track that and ensure we do not send calls deeper into CDI after it is deactivated.
    private AtomicBoolean isActive = new AtomicBoolean();

    @Activate
    public CDIManagedObjectService(@Reference CDIService cdiService, @Reference DefaultManagedObjectService defaultMOS) {
        this.cdiRuntime = (CDIRuntime) cdiService;
        this.defaultMOS = defaultMOS;
        isActive.set(true);
    }

    public void deactivate(ComponentContext cc) {
        isActive.set(false);
    }

    @Override
    public <T> ManagedObjectFactory<T> createManagedObjectFactory(ModuleMetaData mmd, Class<T> klass,
                                                                  boolean requestManagingInjectionAndInterceptors) throws ManagedObjectException {
        if (!HttpJspPage.class.isAssignableFrom(klass) && isCDIEnabled(mmd)) {
            return new CDIManagedObjectFactoryImpl<T>(klass, cdiRuntime, requestManagingInjectionAndInterceptors);
        } else {
            return defaultMOS.createManagedObjectFactory(mmd, klass, requestManagingInjectionAndInterceptors);
        }
    }

    @Override
    public <T> ManagedObjectFactory<T> createEJBManagedObjectFactory(ModuleMetaData mmd, Class<T> klass, String ejbName) throws ManagedObjectException {
        ManagedObjectFactory<T> defaultEJBManagedObjectFactory = defaultMOS.createEJBManagedObjectFactory(mmd, klass, ejbName);
        if (isCDIEnabled(mmd)) {
            return new CDIEJBManagedObjectFactoryImpl<T>(klass, ejbName, cdiRuntime, defaultEJBManagedObjectFactory);
        } else {
            return defaultEJBManagedObjectFactory;
        }
    }

    @Override
    public <T> ManagedObjectFactory<T> createInterceptorManagedObjectFactory(ModuleMetaData mmd, Class<T> klass) throws ManagedObjectException {
        if (isCDIEnabled(mmd)) {
            return new CDIInterceptorManagedObjectFactoryImpl<T>(klass, cdiRuntime);
        } else {
            return defaultMOS.createInterceptorManagedObjectFactory(mmd, klass);
        }
    }

    private boolean isCDIEnabled(ModuleMetaData mmd) {
        return isActive.get() && cdiRuntime.isModuleCDIEnabled(mmd);
    }

    /** {@inheritDoc} */
    @Override
    public <T> ManagedObjectFactory<T> createManagedObjectFactory(ModuleMetaData mmd, Class<T> klass, boolean requestManagingInjectionAndInterceptors,
                                                                  ReferenceContext referenceContext) throws ManagedObjectException {
        if (isCDIEnabled(mmd)) {
            return new CDIManagedObjectFactoryImpl<T>(klass, cdiRuntime, requestManagingInjectionAndInterceptors, referenceContext);
        } else {
            return defaultMOS.createManagedObjectFactory(mmd, klass, requestManagingInjectionAndInterceptors, referenceContext);
        }
    }
}
