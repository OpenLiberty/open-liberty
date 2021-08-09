/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.impl.managedobject;

import com.ibm.ws.managedobject.ManagedObject;
import com.ibm.ws.managedobject.ManagedObjectContext;
import com.ibm.ws.managedobject.ManagedObjectException;

import com.ibm.wsspi.injectionengine.InjectionTarget;
import com.ibm.wsspi.injectionengine.InjectionTargetContext;
import com.ibm.wsspi.injectionengine.ReferenceContext;

public class DummyManagedObject<T> implements ManagedObject<T> {

    private T instance = null; 

    public DummyManagedObject(T instance) {
        this.instance = instance;
    }

    @Override
    public T getObject() {        
        return instance;
    }

    @Override
    public ManagedObjectContext getContext() {        
        throw new UnsupportedOperationException();
    }

    @Override
    public <K> K getContextData(Class<K> klass) {        
        throw new UnsupportedOperationException();
    }

    @Override
    public void release() {
        //no op
    }

    @Override
    public boolean isLifecycleManaged() {     
        throw new UnsupportedOperationException();
    }

    @Override
    public String getBeanScope() {        
        throw new UnsupportedOperationException();
    }

    @Override
    public T inject(ReferenceContext referenceContext) throws ManagedObjectException {        
        throw new UnsupportedOperationException();
    }

    @Override
    public T inject(InjectionTarget[] targets, InjectionTargetContext injectionContext) throws ManagedObjectException {        
        throw new UnsupportedOperationException();
    }

}
