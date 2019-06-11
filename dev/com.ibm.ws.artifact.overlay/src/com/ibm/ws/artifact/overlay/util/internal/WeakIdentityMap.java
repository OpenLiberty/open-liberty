package com.ibm.ws.artifact.overlay.util.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.BiConsumer;

/**
 * A parameterized weak identity set.  Implements a bare minimum of
 * set operations.  Synchronizes access to the store, for safe iteration. 
 */
public class WeakIdentityMap<K, V> {
    private final WeakHashMap<K, V> store = new WeakHashMap<>();

    /**
     * Answer the size of this set.
     *
     * @return The size of this set.
     */
    public int size() {
        synchronized(store) {
            return store.size();
        }
    }

    /**
     * Answer the value mapped to a key.
     *
     * @param key The key to retrieve.
     *
     * @return The value mapped to the key.  Null
     *     if no value is mapped to the key.
     */
    public V get(K key) {
        synchronized(store) {
            return store.get(key);
        }
    }

    /**
     * Map a key to a value.
     *
     * @param key The key to add.
     * @param value The value to add.
     *
     * @return The value previously mapped to the key.  Null
     *     if there was no previous mapping.
     */
    public V put(K key, V value) {
        synchronized(store) {
            return store.put(key, value);
        }
    }

    
    /**
     * Unmap a key.
     *
     * @param key The key to remove.
     *
     * @return The value previously mapped to the key.
     *     Null if there was no prvious mapping.
     */
    public V remove(K key) {
        synchronized(store) {
            return store.remove(key);
        }
    }

    /**
     * Create a snapshot of this mapping.
     *
     * @return A snapshot this mapping.  The order
     *    of the elements is undefined.
     */
    public List<? extends Map.Entry<K, V>> snapshot() {
        List<Map.Entry<K, V>> snapshot;
        synchronized (store) {
            snapshot = new ArrayList<Map.Entry<K, V>>( store.size() );
            for ( Map.Entry<K, V> entry : store.entrySet() ) {
                snapshot.add(entry);
            }
        }
        return snapshot;
    }

    /**
     * Apply a consumer action to the mapping.
     *
     * Iteration and application are both synchronized.
     *
     * @param action The action to apply to the mapping.
     */
    public void forEach(BiConsumer<K, V> action) {
        synchronized (store) {
            store.forEach( (K key, V value) -> action.accept(key, value) );
        }
    }
}
