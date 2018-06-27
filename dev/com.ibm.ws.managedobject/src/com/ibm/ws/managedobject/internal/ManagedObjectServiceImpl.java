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
package com.ibm.ws.managedobject.internal;

import org.osgi.service.component.annotations.Component;

import com.ibm.ws.managedobject.DefaultManagedObjectService;
import com.ibm.ws.managedobject.ManagedObjectException;
import com.ibm.ws.managedobject.ManagedObjectFactory;
import com.ibm.ws.managedobject.ManagedObjectService;
import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.wsspi.injectionengine.ReferenceContext;

@Component(name = "com.ibm.ws.managedobject.internal.ManagedObjectServiceImpl",
           service = { ManagedObjectService.class, DefaultManagedObjectService.class },
           immediate = true,
           property = { "service.vendor=IBM", "service.ranking:Integer=-1" })
public class ManagedObjectServiceImpl implements DefaultManagedObjectService {

    @Override
    public <T> ManagedObjectFactory<T> createManagedObjectFactory(ModuleMetaData mmd, Class<T> klass, boolean requestManageInjectionAndInterceptors) throws ManagedObjectException {
        return new ManagedObjectFactoryImpl<T>(klass);
    }

    @Override
    public <T> ManagedObjectFactory<T> createManagedObjectFactory(ModuleMetaData mmd, Class<T> klass, boolean requestManageInjectionAndInterceptors,
                                                                  ReferenceContext referenceContext) throws ManagedObjectException {
        return new ManagedObjectFactoryImpl<T>(klass);
    }

    @Override
    public <T> ManagedObjectFactory<T> createEJBManagedObjectFactory(ModuleMetaData mmd, Class<T> klass, String ejbName) throws ManagedObjectException {
        //since this is a fallback adapter, the ejbName is ignored
        return createManagedObjectFactory(mmd, klass, false);
    }

    @Override
    public <T> ManagedObjectFactory<T> createInterceptorManagedObjectFactory(ModuleMetaData mmd, Class<T> klass) throws ManagedObjectException {
        return createManagedObjectFactory(mmd, klass, false);
    }
}
