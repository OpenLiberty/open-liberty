/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth20.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

public class BoundedCommonCache<T> implements Serializable {

    private static final long serialVersionUID = -6352052283899728877L;
    private final static transient TraceComponent tc = Tr.register(BoundedCommonCache.class);

    private int capacity;
    private List<T> cache;

    public BoundedCommonCache(int maxCapacity) {
        this.cache = Collections.synchronizedList(new ArrayList<T>());
        if (maxCapacity < 0) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Cache capacity cannot be negative. Cache capacity will be set to 0.");
            }
            this.capacity = 0;
        } else {
            this.capacity = maxCapacity;
        }
    }

    public int getCapacity() {
        return capacity;
    }

    /**
     * Resets the cache capacity to the value specified. Any entries in excess
     * of the new capacity will be removed.
     * 
     * @param newCapacity
     */
    public void updateCapacity(int newCapacity) {
        if (newCapacity < 0) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Updated cache capacity cannot be negative. Cache capacity will be updated to 0.");
            }
            capacity = 0;
        } else {
            capacity = newCapacity;
        }
        int entriesToRemove = size() - capacity;
        removeExcessEntries(entriesToRemove);
    }

    public int size() {
        synchronized (cache) {
            return cache.size();
        }
    }

    public boolean contains(T key) {
        synchronized (cache) {
            return cache.contains(key);
        }
    }

    public T get(T key) {
        synchronized (cache) {
            if (cache.indexOf(key) > -1) {
                return cache.get(cache.indexOf(key));
            }
            return null;
        }
    }

    /**
     * Add an entry into the cache. If the cache is at it's maximum capacity,
     * then remove the oldest entry or entries to make room.
     * 
     * @param key
     */
    public void put(T key) {
        synchronized (cache) {
            // Remove the existing key if it already exists
            if (cache.contains(key)) {
                T existingKey = cache.get(cache.indexOf(key));
                cache.remove(existingKey);
            }
        }
        int size = size();
        if (size >= capacity) {
            // Remove excess entries if attempting to put entry in cache exceeding its capacity
            int entriesToRemove = size - capacity + 1;
            removeExcessEntries(entriesToRemove);
        }
        if (capacity > 0) {
            cache.add(key);
        }
    }

    public boolean remove(T key) {
        synchronized (cache) {
            return cache.remove(key);
        }
    }

    private void removeExcessEntries(int entriesToRemove) {
        synchronized (cache) {
            Iterator<T> it = cache.iterator();
            int i = 0;
            while (it.hasNext() && i < entriesToRemove) {
                it.next();
                it.remove();
                i++;
            }
        }
    }

}
