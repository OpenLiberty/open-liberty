package com.ibm.ws.artifact.overlay.util.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;
import java.util.function.Consumer;

/**
 * A parameterized weak identity set.  Implements a bare minimum of
 * set operations.  Synchronizes access to the store, for safe iteration. 
 */
public class WeakIdentitySet<T> {
    private final WeakHashMap<T, T> store = new WeakHashMap<>();

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
     * Add an element to this set.
     *
     * @param element The element which was added to the set.
     *
     * @return The element, if it was already present.  Otherwise, null.
     */
    public T add(T element) {
        synchronized(store) {
            return store.put(element, element);
        }
    }

    /**
     * Remove an element from the set.
     *
     * @param element The element which is to be removed.
     *
     * @return True if the element was removed.  False if the
     *     element was already not an element of the set.
     */
    public boolean remove(T element) {
        synchronized(store) {
            return (store.remove(element) != null);
        }
    }

    /**
     * Make a snapshot of the elements of this set.  Creation of
     * the snapshot is synchronized.
     *
     * @return A snapshot of the elements of the set.  The order
     *    of the elements is undefined.
     */
    public List<T> snapshot() {
        List<T> snapshot;
        synchronized (store) {
            snapshot = new ArrayList<>( store.size() );
            store.forEach( (T element, T ignored) -> snapshot.add(element) );
        }
        return snapshot;
    }

    /**
     * Apply a consumer action to the elements of this set.
     *
     * Iteration and application are both synchronized.
     *
     * @param action The action to perform on each element.
     */
    public void forEach(Consumer<T> action) {
        synchronized (store) {
            store.forEach( (T element, T ignored) -> action.accept(element) );
        }
    }
}
