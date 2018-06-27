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

import java.lang.reflect.Constructor;
import java.util.Map;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.managedobject.ConstructionCallback;
import com.ibm.ws.managedobject.ManagedObject;
import com.ibm.ws.managedobject.ManagedObjectContext;
import com.ibm.ws.managedobject.ManagedObjectException;
import com.ibm.ws.managedobject.ManagedObjectFactory;
import com.ibm.ws.managedobject.ManagedObjectInvocationContext;

public class ManagedObjectFactoryImpl<T> implements ManagedObjectFactory<T>, ConstructionCallback<T> {
    private Constructor<T> constructor;
    private final Class<T> managedClass;

    public ManagedObjectFactoryImpl(Class<T> klass) throws ManagedObjectException {
        this.managedClass = klass;
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
        return managedClass;
    }

    /**
     * Returns the constructor that will be used by this factory to create the managed object.
     */
    @Override
    public Constructor<T> getConstructor() {
        if (this.constructor == null) {
            try {
                this.constructor = this.managedClass.getConstructor((Class<?>[]) null);
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException(e);
            }
        }
        return this.constructor;
    }

    @Override
    public ManagedObjectContext createContext() {
        return null;
    }

    @Override
    @FFDCIgnore(Exception.class)
    public ManagedObject<T> createManagedObject() throws ManagedObjectException {
        T instance = null;
        try {
            instance = getConstructor().newInstance(new Object[0]);
        } catch (Exception e) {
            throw new ManagedObjectException(e);
        }
        ManagedObject<T> mo = createManagedObject(instance, null);
        return mo;
    }

    @Override
    @FFDCIgnore(Exception.class)
    public ManagedObject<T> createManagedObject(ManagedObjectInvocationContext<T> invocationContext) throws ManagedObjectException {
        T managedObject;
        try {
            managedObject = invocationContext.aroundConstruct(this, new Object[0], null);
        } catch (Exception e) {
            throw new ManagedObjectException(e);
        }
        return new ManagedObjectImpl<T>(managedObject);
    }

    @Override
    public T proceed(Object[] parameters, Map<String, Object> data) throws Exception {
        return getConstructor().newInstance(parameters);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.managedobject.ManagedObjectFactory#createManagedObject(java.lang.Object, com.ibm.ws.managedobject.ManagedObjectInvocationContext)
     */
    @Override
    public ManagedObject<T> createManagedObject(T existingInstance, ManagedObjectInvocationContext<T> invocationContext) {
        if (existingInstance == null) {
            throw new IllegalArgumentException("Existing instance must not be null");
        }
        return new ManagedObjectImpl<T>(existingInstance);
    }

}
