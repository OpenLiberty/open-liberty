/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.cdi.component;

import java.util.HashMap;
import java.util.Map;

import com.ibm.ws.managedobject.ManagedObject;

/**
 *
 */
public class ThreadBasedHashMap extends HashMap<Class<?>, ManagedObject<?>> {

    ThreadLocal<Map<Class<?>, ManagedObject<?>>> tlMap = new ThreadLocal<Map<Class<?>, ManagedObject<?>>>() {
        @Override
        protected Map<Class<?>, ManagedObject<?>> initialValue() {
            return new HashMap<>();
        }
    };

    @Override
    public ManagedObject<?> get(Object key) {
        if (tlMap.get().containsKey(key)) {
            return tlMap.get().get(key);
        }
        return super.get(key);
    }

    @Override
    public ManagedObject<?> put(Class<?> key, ManagedObject<?> value) {
        ManagedObject<?> prevValue;
        if (tlMap.get().containsKey(key)) {
            prevValue = tlMap.get().put(key, value);
            super.put(key, value);
        } else {
            tlMap.get().put(key, value);
            prevValue = super.put(key, value);
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
        }
        return prevValue;
    }

    @Override
    public String toString() {
        return "ThreadBasedHashMap: (ThreadMap)" + tlMap.toString() + " (GlobalMap) " + super.toString();
    }
}
