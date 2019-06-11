/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.artifact.overlay.util.internal;

import java.util.List;
import java.util.Map;

/**
 * Timed data store.  Associate a time when adding values to
 * the map.
 */
public class TimedWeakIdentityMap<V> {
    /**
     * Record the create time of the cache.  This is used to make sense 
     * of put and get time stamps.
     */
    private final long createNano = System.nanoTime();

    public long getCreateNano() {
    	return createNano;
    }

	/**
	 * Storage for values and their addition times.
	 * 
	 * The storage is thread safe.  Local synchronization is not needed.
	 */
    private final WeakIdentityMap<V, Long> storage = new WeakIdentityMap<V, Long>();

    /**
     * Create a snapshot of the cache.  The snapshot is completely independent
     * of the cache.  However, individual data values are not thread safe.
     *
     * @return A snapshot of the cache.
     */
    public List<? extends Map.Entry<V, Long>> snapshot() {
        return storage.snapshot();
    }

    /**
     * Add a value to the map.  Store the current time in nano-seconds
     * with the value as a record of when the value was added to the map.
     *
     * @param value The value to add.
     *
     * @return If the value was already present, the prior add time of the
     *     value. Null if the value was not present in the map.
     */
    public Long add(V value) {
        return storage.put( value, Long.valueOf( System.nanoTime() ) );
    }

    /**
     * Retrieve the addition time of a value.  Answer null if the
     * value is not contained by the map.
     *
     * @param value The value to retrieve.
     * 
     * @return The addition time of the value.  Null if the value is
     *     not container by the map. 
     */
    public Long get(V value) {
        return storage.get(value);
    }
}
