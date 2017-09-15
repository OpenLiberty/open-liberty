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
package com.ibm.ws.managedobject.internal;

import java.lang.reflect.Constructor;
import java.util.Map;

import com.ibm.ws.managedobject.ConstructionCallback;
import com.ibm.ws.managedobject.ManagedObject;
import com.ibm.ws.managedobject.ManagedObjectContext;
import com.ibm.ws.managedobject.ManagedObjectException;
import com.ibm.ws.managedobject.ManagedObjectFactory;
import com.ibm.ws.managedobject.ManagedObjectInvocationContext;

public class ManagedObjectFactoryImpl<T> implements ManagedObjectFactory<T>, ConstructionCallback<T> {
    private final Constructor<T> constructor;

    public ManagedObjectFactoryImpl(Class<T> klass) throws ManagedObjectException {
        try {
            this.constructor = klass.getConstructor((Class<?>[]) null);
        } catch (NoSuchMethodException e) {
            throw new ManagedObjectException(e);
        }
    }

    @Override
    public boolean isManaged() {
        return false;
    }

    @Override
    public boolean managesInjectionAndInterceptors() {
        return false;
    }

    @Override
    public Class<T> getManagedObjectClass() {
        return constructor.getDeclaringClass();
    }

    /**
     * Returns the constructor that will be used by this factory to create the managed object.
     */
    @Override
    public Constructor<T> getConstructor() {
        return constructor;
    }

    @Override
    public ManagedObjectContext createContext() {
        return null;
    }

    @Override
    public ManagedObject<T> createManagedObject() throws Exception {
        return new ManagedObjectImpl<T>(constructor.newInstance(new Object[0]));
    }

    @Override
    public ManagedObject<T> createManagedObject(ManagedObjectInvocationContext<T> invocationContext) throws Exception {
        T managedObject = invocationContext.aroundConstruct(this, new Object[0], null);
        return new ManagedObjectImpl<T>(managedObject);
    }

    @Override
    public T proceed(Object[] parameters, Map<String, Object> data) throws Exception {
        return constructor.newInstance(parameters);
    }

}
