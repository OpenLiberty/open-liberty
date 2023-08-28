/*******************************************************************************
 * Copyright (c) 2017, 2023 IBM Corporation and others.
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
package com.ibm.ws.jaxrs20.cdi.component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

import com.ibm.ws.managedobject.ManagedObject;
import com.ibm.ws.managedobject.ManagedObjectContext;
import com.ibm.ws.managedobject.ManagedObjectException;
import com.ibm.wsspi.injectionengine.InjectionTarget;
import com.ibm.wsspi.injectionengine.InjectionTargetContext;
import com.ibm.wsspi.injectionengine.ReferenceContext;

/**
 *
 */
public class ThreadBasedHashMap extends ConcurrentHashMap<Class<?>, ManagedObject<?>> {

    private static final long serialVersionUID = 3759994379932861970L;

    private static final ManagedObject<?> NULL_MANAGED_OBJECT = new ManagedObject<Void>() {

        @Override
        public Void getObject() {
            throw new UnsupportedOperationException();
        }

        @Override
        public ManagedObjectContext getContext() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Void getContextData(Class klass) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void release() {
            throw new UnsupportedOperationException();
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
        public Void inject(ReferenceContext referenceContext) throws ManagedObjectException {
            throw new UnsupportedOperationException();
        }

        @Override
        public Void inject(InjectionTarget[] targets, InjectionTargetContext injectionContext) throws ManagedObjectException {
            throw new UnsupportedOperationException();
        }
    };

    ThreadLocal<Map<Class<?>, ManagedObject<?>>> tlMap = new ThreadLocal<Map<Class<?>, ManagedObject<?>>>() {
        @Override
        protected Map<Class<?>, ManagedObject<?>> initialValue() {
            return new WeakHashMap<>();
        }
    };

    @Override
    public boolean isEmpty() {
        return (tlMap.get().isEmpty() && super.isEmpty());
    }

    @Override
    public boolean containsKey(Object key) {
        // In a ConcurrentHashMap containsKey will return false for an entry with a null value, so we will call get() instead.
        return (tlMap.get().containsKey(key) || (super.get(key) != null));
    }


    @Override
    public ManagedObject<?> get(Object key) {
        ManagedObject<?> returnObj;
        if (tlMap.get().containsKey(key)) {
            returnObj = tlMap.get().get(key);
        } else {
            returnObj = super.get(key);
            if (returnObj == NULL_MANAGED_OBJECT) {
                returnObj = null;
            }

        }
        return returnObj;

    }

    @Override
    public ManagedObject<?> put(Class<?> key, ManagedObject<?> value) {
        ManagedObject<?> prevValue;
        if (tlMap.get().containsKey(key)) {
            prevValue = tlMap.get().put(key, value);
            super.put(key, value == null ? NULL_MANAGED_OBJECT : value);
        } else {
            tlMap.get().put(key, value);
            prevValue = super.put(key, value == null ? NULL_MANAGED_OBJECT : value);
            if (prevValue == NULL_MANAGED_OBJECT) {
                prevValue = null;
            }

        }
        return prevValue;
    }

    @Override
    public ManagedObject<?> remove(Object key) {
        ManagedObject<?> prevValue;
        if (tlMap.get().containsKey(key)) {
            prevValue = tlMap.get().remove(key);
            super.remove(key);
        } else {
            prevValue = super.remove(key);
            if (prevValue == NULL_MANAGED_OBJECT) {
                prevValue = null;
            }
        }
        return prevValue;
    }

    @Override
    public Collection<ManagedObject<?>> values() {
        Collection<ManagedObject<?>> origValues = super.values();
        Collection<ManagedObject<?>> returnValues = new ArrayList<>(origValues.size());
        Iterator<ManagedObject<?>> i = origValues.iterator();
        while (i.hasNext()) {
            ManagedObject<?> value = i.next();
            if (value == NULL_MANAGED_OBJECT) {
                value = null;
            }
            returnValues.add(value);
        }
        return returnValues;
    }

    @Override
    public String toString() {
        return "ThreadBasedHashMap: (ThreadMap)" + tlMap.toString() + " (GlobalMap) " + super.toString();
    }
}
