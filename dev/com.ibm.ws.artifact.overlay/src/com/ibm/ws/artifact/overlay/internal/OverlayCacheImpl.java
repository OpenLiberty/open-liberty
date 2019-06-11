/*******************************************************************************
 * Copyright (c) 2011, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.artifact.overlay.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiConsumer;

/**
 * Double layered data store: Map from {@link String} and {@link Class<?>}
 * to instances of the specified class.
 * 
 * The intent is that the data stored in association with a specific
 * data type be an instance of that data type.  The API does not enforce
 * that association.
 */
public class OverlayCacheImpl {
    public static class OverlayCacheDataImpl {
        public OverlayCacheDataImpl(Object value) {
            this.value = value;
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

        public final Object value;
        public final long putNano;
        public volatile long getNano;

        public Object getValue() {
            this.getNano = System.nanoTime();
            return value;
        }

        public long getPutNano() {
            return putNano;
        }

        public long getGetNano() {
            return getNano;
        }
    }

    /** Storage for cache values. */
    private final Map<String, Map<Class<?>, OverlayCacheDataImpl>> storage = new HashMap<>();

    /**
     * Record the create time of the cache.  This is used to make sense 
     * of put and get time stamps.
     */
    private final long createNano = System.nanoTime();

    public long getCreateNano() {
    	return createNano;
    }

    /**
     * Create a snapshot of the cache.  The snapshot is completely independent
     * of the cache.  However, individual data values are not thread safe.
     *
     * @return A snapshot of the cache.
     */
    public synchronized List<Map.Entry<String, List<Map.Entry<Class<?>, OverlayCacheDataImpl>>>> snapshot() {
        List<Map.Entry<String, List<Map.Entry<Class<?>, OverlayCacheDataImpl>>>> snapshot =
            new ArrayList<>( storage.size() );

        BiConsumer<String, Map<Class<?>, OverlayCacheDataImpl>> pathAction =
            new BiConsumer<String, Map<Class<?>, OverlayCacheDataImpl>>() {
                @Override
                public void accept(String path, Map<Class<?>, OverlayCacheDataImpl> valuesForPath) {
                    List<Map.Entry<Class<?>, OverlayCacheDataImpl>> snapshotForPath =
                        new ArrayList<>( valuesForPath.size() );

                    // Unfortunately, the entries which are retrieved from a hash mapping
                    // are not stable outside of this synchronization block.  New entries
                    // must be created to ensure the entry values are stable.

                    valuesForPath.forEach( (Class<?> dataType, OverlayCacheDataImpl data) -> {
                        snapshotForPath.add( new Map.Entry<Class<?>, OverlayCacheDataImpl>() {
                            public Class<?> getKey() {
                                return dataType;
                            }
                            public OverlayCacheDataImpl getValue() {
                                return data;
                            }
                            public OverlayCacheDataImpl setValue(OverlayCacheDataImpl value) {
                                throw new UnsupportedOperationException();
                            }
                        });
                    });

                    Map.Entry<String, List<Map.Entry<Class<?>, OverlayCacheDataImpl>>> entryForPath =
                        new Map.Entry<String, List<Map.Entry<Class<?>, OverlayCacheDataImpl>>>() {
                            public String getKey() {
                                return path;
                            }
                            public List<Map.Entry<Class<?>, OverlayCacheDataImpl>> getValue() {
                                return snapshotForPath;
                            }

                            @Override
                            public List<Entry<Class<?>, OverlayCacheDataImpl>> setValue(List<Entry<Class<?>, OverlayCacheDataImpl>> value) {
                                throw new UnsupportedOperationException();
                            }
                        };

                    snapshot.add(entryForPath);
                }
            };

        storage.forEach(pathAction);

        return snapshot;
    }

    /**
     * Map a path and a data type to a data value.  Set a put time for
     * the data.  Data previously stored is replaced.
     *
     * @param path The path to map to the data value.
     * @param dataType The data type to map to the data value.
     * @param data The data value to map.
     */
    public synchronized void addToCache(String path, Class<?> dataType, Object data) {
        Map<Class<?>, OverlayCacheDataImpl> cacheForPath = storage.get(path);
        if ( cacheForPath == null ) {
            cacheForPath = new HashMap<Class<?>, OverlayCacheDataImpl>(1);
            storage.put(path, cacheForPath);
        }
        cacheForPath.put( dataType, new OverlayCacheDataImpl(data) ); // This sets the put time stamp.
    }

    /**
     * Remove the mapping of a path and a data type.  Do nothing
     * if no mapping exists for the path and data type.
     *
     * @param path The path to remove.
     * @param dataType The data type to remove.
     */
    public synchronized void removeFromCache(String path, Class<?> dataType) {
        Map<Class<?>, OverlayCacheDataImpl> cacheForPath = storage.get(path);
        if ( cacheForPath != null ) {
            cacheForPath.remove(dataType);
            if ( cacheForPath.isEmpty() ) {
                storage.remove(path);
            }
        }
    }

    /**
     * Answer the mapping of a path and data type.  Answer null if no
     * mapping exists for the path and data type.  Update the last get
     * time of the mapped data.
     *
     * @param path The path to retrieve.
     * @param dataType The data type to retrieve.
     *
     * @return The value associated with the path and data type.
     *     Usually, an instance of the data type.  Null if no mapping exists
     *     for the path and data type.
     */
    public synchronized Object getFromCache(String path, Class<?> dataType) {
        Map<Class<?>, OverlayCacheDataImpl> cacheForPath = storage.get(path);
        if ( cacheForPath != null ) {
            OverlayCacheDataImpl cacheData = cacheForPath.get(dataType);
            if ( cacheData != null ) {
                return cacheData.getValue(); // This updates the get time stamp.
            } else {
                return null;
            }
        } else {
            return null;
        }
    }
}
