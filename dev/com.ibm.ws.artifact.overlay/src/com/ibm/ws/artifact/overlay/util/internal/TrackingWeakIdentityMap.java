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
public class TrackingWeakIdentityMap<V> {
    /**
     * Record the create time of the cache.  This is used to make sense 
     * of put and get time stamps.
     */
    private final long createNano = System.nanoTime();

    public long getCreateNano() {
        return createNano;
    }

    //

    public static class TrackingData {
        public TrackingData() {
            this.putNano = System.nanoTime();
            this.getNano = this.putNano;
        }

        // We aren't synchronizing access to 'getNano':
        // That means that introspector code might retrieve the value
        // while other code is updating the value.  Use of 'volatile'
        // means the value retrieval will be atomic.  But, the actual
        // 'getValue' value isn't guaranteed to be exactly the same
        // as the value when the snapshot was obtained. Ensuring the
        // value is unchanged from when the snapshot is obtained would
        // mean duplicating the cache data objects, which doesn't seem
        // worth doing for introspection.

        public final long putNano;
        
        public long getPutNano() {
            return putNano;
        }

        public volatile long getNano;

        public long didGet() {
            return ( this.getNano = System.nanoTime() );
        }

        public long getGetNano() {
            return getNano;
        }
    }

    //

    /**
     * Storage for values and their addition times.
     * 
     * The storage is thread safe.  Local synchronization is not needed.
     */
    private final WeakIdentityMap<V, TrackingData> storage = new WeakIdentityMap<V, TrackingData>();

    /**
     * Create a snapshot of the cache.  The snapshot is completely independent
     * of the cache.  However, individual data values are not thread safe.
     *
     * @return A snapshot of the cache.
     */
    public List<? extends Map.Entry<? extends V, ? extends TrackingData>> snapshot() {
        return storage.snapshot();
    }

    /**
     * Add a value to the map.  Store tracking data for the value.
     *
     * @param value The value to add.
     *
     * @return If the value was already present, the prior tracking
     *     data of the value. Null if the value was not present in the map.
     */
    public TrackingData add(V value) {
        return storage.put( value, new TrackingData() );
    }

    /**
     * Retrieve the tracking data of a value.  Answer null if the
     * value is not contained by the map.
     *
     * @param value The value to retrieve.
     * 
     * @return The tracking data of the value.  Null if the value is
     *     not container by the map. 
     */
    public TrackingData get(V value) {
        TrackingData trackingData = storage.get(value);
        if ( trackingData != null ) {
            trackingData.didGet();
        }
        return trackingData;
    }
}
