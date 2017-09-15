/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.classloading.internal.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;

import com.ibm.wsspi.classloading.ClassLoaderIdentity;

/**
 * A class to store {@link ClassLoader} objects weakly, indexed by {@link ClassLoaderIdentity}.
 */
public class CanonicalStore<K, V extends Keyed<K>> {
    // Use a concurrent map under the covers to help with thread-safety
    final ConcurrentMap<K, Ref<V>> map = new ConcurrentHashMap<K, Ref<V>>();
    // Provide a queue for references whose referents have been GC'd
    final RefQueue<V, ? extends KeyedRef<K, V>> q = new RefQueue<V, WeakKeyedRef<K, V>>();

    /**
     * Store the given {@link ClassLoader} indexed by the given {@link ClassLoaderIdentity}.
     * This method can safely be called from multiple threads concurrently, but the
     * ordering of concurrent store operations for the same key is unspecified.
     */
    public void store(K key, V loader) {
        // Clean up stale entries on every put.
        // This should avoid a slow memory leak of reference objects.
        cleanUpStaleEntries();
        map.put(key, new WeakKeyedRef<K, V>(key, loader, q));
    }

    /**
     * Retrieve the {@link ClassLoader} (if any) stored with the given {@link ClassLoaderIdentity}.
     * 
     * @return <code>null</code> if no {@link ClassLoader} has been stored or the {@link ClassLoader} has been collected.
     */
    public V retrieve(K key) {
        Ref<V> ref = map.get(key);
        return ref == null ? null : ref.get();
    }

    /**
     * Remove any mapping for the provided id
     * 
     * @param id
     */
    public boolean remove(V value) {
        // peek at what is in the map
        K key = value.getKey();
        Ref<V> ref = map.get(key);
        // only try to remove the mapping if it matches the provided class loader
        return (ref != null && ref.get() == value) ? map.remove(key, ref) : false;
    }

    /**
     * Create a value for the given key iff one has not already been stored.
     * This method is safe to be called concurrently from multiple threads.
     * It will ensure that only one thread succeeds to create the value for the given key.
     * 
     * @return the created/retrieved {@link AppClassLoader}
     */
    public V retrieveOrCreate(K key, Factory<V> factory) {
        // Clean up stale entries on every put.
        // This should avoid a slow memory leak of reference objects.
        this.cleanUpStaleEntries();
        return retrieveOrCreate(key, factory, new FutureRef<V>());
    }

    private V retrieveOrCreate(K key, Factory<V> factory, FutureRef<V> futureRef) {
        Ref<V> canonicalRef = map.putIfAbsent(key, futureRef);
        V value;
        if (canonicalRef == null) {
            // CREATOR THREAD: this thread won the race to create the canonical classloader for this ID.
            try {
                // 1) Create the classloader, 
                futureRef.result = value = factory.createInstance();
            } finally {
                // 2) Let any waiting threads see the result (or lack of one)
                futureRef.latch.countDown();
            }
            // 3) Then replace the FutureRef with a WeakRef in the map
            WeakKeyedRef<K, V> weakRef = new WeakKeyedRef<K, V>(key, value, this.q);
            this.map.replace(key, futureRef, weakRef);
        } else {
            // NON-CREATOR THREAD: some other thread already created the canonical classloader for this ID.
            value = canonicalRef.get();
            if (value == null) {
                // the loader was null: could have failed in creation or been GC'd
                // either way, recurse to try creating a new loader
                map.remove(key, canonicalRef);
                value = retrieveOrCreate(key, factory, futureRef);
            }
        }
        return value;
    }

    /** clean up stale entries */
    void cleanUpStaleEntries() {
        for (KeyedRef<K, V> ref = q.poll(); ref != null; ref = q.poll()) {
            map.remove(ref.getKey(), ref); // CONCURRENT remove() operation
        }
    }

    private static class FutureRef<V> implements Ref<V> {
        final CountDownLatch latch = new CountDownLatch(1);
        V result;

        /** Return the result, blocking until it is available. */
        @Override
        public V get() {
            // no interrupts allowed
            do {
                try {
                    latch.await();
                    return result;
                } catch (InterruptedException e) {
                }
            } while (true);
        }
    }

}
