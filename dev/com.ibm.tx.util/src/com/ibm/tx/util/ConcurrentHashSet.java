package com.ibm.tx.util;

/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

import com.ibm.ws.kernel.service.util.CpuInfo;

/**
 * Provides a thread-safe hash set by delegating to java.util.concurrent.ConcurrentHashMap.
 * The keys of the map constitute the set. Values are ignored.
 */
public class ConcurrentHashSet<E> extends AbstractSet<E> {
    /**
     * The ConcurrentHashMap containing the set.
     */
    ConcurrentHashMap<E, byte[]> map = new ConcurrentHashMap<E, byte[]>(256, 0.75f, getNumCHBuckets());

    // Calculate number of concurrent hash buckets as a factor of
    // the number of available processors.
    public static int getNumCHBuckets() {
        // determine number of processors
        final int baseVal = CpuInfo.getAvailableProcessors().get() * 20;

        // determine next power of two
        int pow = 2;
        while (pow < baseVal)
            pow *= 2;
        return pow;
    }

    /**
     * A value to place in the map where needed.
     * The keys, not the values, of the ConcurrentHashMap constitute the set.
     */
    byte[] value = new byte[0];

    /**
     * Add an entry to the set.
     *
     * @param entry the entry.
     * @return true if added, false if already there.
     */
    @Override
    public final boolean add(E entry) {
        return map.put(entry, value) == null;
    }

    /**
     * Remove an entry from the Set.
     *
     * @param entry the entry.
     * @return true if this ConcurrentHashSet is modified, false otherwise
     */
    @Override
    public final boolean remove(Object key) {
        return map.remove(key) != null;
    }

    /**
     * Check if the set contains an entry.
     *
     * @param entry the entry.
     * @return true if the set contains the entry, false if not.
     */
    @Override
    public final boolean contains(Object entry) {
        return map.containsKey(entry);
    }

    /**
     * @return an iterator over the set.
     */
    @Override
    public final Iterator<E> iterator() {
        return map.keySet().iterator();
    }

    /**
     * @return the size of the set.
     */
    @Override
    public final int size() {
        return map.size();
    }
}