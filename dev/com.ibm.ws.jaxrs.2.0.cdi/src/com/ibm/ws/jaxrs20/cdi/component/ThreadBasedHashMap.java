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

import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

import com.ibm.ws.managedobject.ManagedObject;

/**
 *
 */
public class ThreadBasedHashMap extends ConcurrentHashMap<Class<?>, ManagedObject<?>> {

    private static final long serialVersionUID = 3759994379932861970L;
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
        return (tlMap.get().containsKey(key) || super.containsKey(key));
    }


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
