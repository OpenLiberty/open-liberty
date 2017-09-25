/*******************************************************************************
 * Copyright (c) 1997, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.http.channel.h2internal;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.ibm.ws.http.dispatcher.internal.channel.HttpDispatcherLink;

/**
 *
 */
public class H2Map implements java.util.Map<Object, Object> {

    // Wrap the Map, and Insert a local map ahead of the wrapped Map.
    // methods which use more than just the local map are:
    //     get, If the object is not in the local map, look in the wrapped Map

    Map<Object, Object> commonMap = null;
    HashMap<Object, Object> localMap = null;

    public H2Map(Map<Object, Object> x) {
        commonMap = x;
        localMap = new HashMap<Object, Object>();
    }

    @Override
    public Object get(Object key) {
        // get from local map, if not there, try the commonMap
        Object value = localMap.get(key);
        if (value == null) {
            // don't allow the backing HttpDispatcherLink to be used by this new request/stream, it needs to have it's own
            if (key.toString().equalsIgnoreCase(HttpDispatcherLink.LINK_ID)) {
                return null;
            }
            value = commonMap.get(key);
        }
        return value;
    }

    // 1. May need a method for "put" to the common map, assume not for now.
    // 2. As with the "put", may need methods that use the common map for any of these other methods below, but assume not for now

    @Override
    public Object put(Object key, Object value) {
        return localMap.put(key, value);
    }

    @Override
    public int size() {
        return localMap.size();
    }

    @Override
    public boolean isEmpty() {
        return localMap.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return localMap.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return localMap.containsValue(value);
    }

    @Override
    public Object remove(Object key) {
        return localMap.remove(key);
    }

    @Override
    public void putAll(Map m) {
        localMap.putAll(m);
    }

    @Override
    public void clear() {
        localMap.clear();
    }

    @Override
    public Set keySet() {
        return localMap.keySet();
    }

    @Override
    public Collection values() {
        return localMap.values();
    }

    @Override
    public Set entrySet() {
        return localMap.entrySet();
    }

}
