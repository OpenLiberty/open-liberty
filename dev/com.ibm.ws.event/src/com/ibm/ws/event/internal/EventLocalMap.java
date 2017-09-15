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
package com.ibm.ws.event.internal;

/**
 * Storage map for inheritable event local values. This allows maps to be
 * chained as new Events are created that inherit values from older Events.
 * Child maps will see parent values until the child storage has modified
 * a particular key. From that point forward, that key will only see updates
 * at the child level; however, other keys will continue to see the current
 * parent values.
 * 
 * @param <K>
 * @param <V>
 */
public class EventLocalMap<K, V> {

    // TODO should keep runtime avgs of sizes to make better default tables
    // ie if we consistently have 100 values, then making a better table to
    // start instead of growing repeatedly...
    private static final int SIZE_ROW = 16; // NOTE: Code below assumes this is a
                                            // power of 2
    private static final int SIZE_TABLE = 1;
    private static final Object NO_VALUE = new Object();

    private EventLocalMap<K, V> parentMap;
    private K[] keys = null;
    private V[][] values = null;

    /**
     * Create a map with no parent.
     */
    public EventLocalMap() {
        this.parentMap = null;
    }

    /**
     * Create a map inheriting values from a parent.
     * 
     * @param parent
     */
    public EventLocalMap(EventLocalMap<K, V> parent) {
        this.parentMap = parent;
    }

    /**
     * Clear all content from this map. This will disconnect from any parent
     * map as well.
     */
    public void clear() {
        // TODO not currently used since EventImpl itself doesn't have a clear
        this.parentMap = null;
        if (null != this.values) {
            for (int i = 0; i < this.values.length; i++) {
                this.values[i] = null;
            }
            this.values = null;
        }
    }

    /**
     * Query the possible value associated with a named EventLocal. A null is
     * returned if the name does not match any stored value or if that stored
     * value is explicitly null.
     * 
     * @param name
     * @return V
     */
    public V get(String name) {
        V rc = null;
        K key = getKey(name);
        if (null != key) {
            rc = get(key);
        }
        return rc;
    }

    /**
     * Look for the key with the provided name. This returns null if no match
     * is found.
     * 
     * @param name
     * @return K
     */
    private K getKey(String name) {
        if (null != this.keys) {
            // we have locally stored values
            final K[] temp = this.keys;
            K key;
            for (int i = 0; i < temp.length; i++) {
                key = temp[i];
                if (null != key && name.equals(key.toString())) {
                    return key;
                }
            }
        }
        // if nothing found locally and we have a parent, check that
        if (null != this.parentMap) {
            return this.parentMap.getKey(name);
        }
        return null;
    }

    /**
     * Query the value for the provided key.
     * 
     * @param key
     * @return V
     */
    public V get(K key) {
        return get(key.hashCode() / SIZE_ROW, key.hashCode() % SIZE_ROW);
    }

    /**
     * Check the target row/column for a value. If nothing exists locally
     * and a parent map exists, that will be checked.
     * 
     * @param row
     * @param column
     * @return V, null if nothing exists
     */
    private V get(int row, int column) {
        if (null == this.values || row >= this.values.length || null == this.values[row] || NO_VALUE == this.values[row][column]) {
            if (null != this.parentMap) {
                return this.parentMap.get(row, column);
            }
            return null;
        }
        // something exists locally and it's not NO_VALUE, could be null
        return this.values[row][column];
    }

    /**
     * Put a key and value pair into the storage map.
     * 
     * @param key
     * @param value
     */
    public void put(K key, V value) {
        final int hash = key.hashCode();
        final int row = hash / SIZE_ROW;
        final int column = hash & (SIZE_ROW - 1); // DON'T use the % operator as we
                                                  // need the result to be
                                                  // non-negative (-1%16 is -1 for
                                                  // example)
        validateKey(hash);
        validateTable(row);
        this.values[row][column] = value;
        this.keys[hash] = key;
    }

    /**
     * Remove a key from the storage.
     * 
     * @param key
     * @return V, existing object, null if none present
     */
    public V remove(K key) {
        final int hash = key.hashCode();
        final int row = hash / SIZE_ROW;
        final int column = hash & (SIZE_ROW - 1); // DON'T use the % operator as we
                                                  // need the result to be
                                                  // non-negative (-1%16 is -1 for
                                                  // example)
        final V rc = get(row, column);
        validateKey(hash);
        validateTable(row);
        this.values[row][column] = null;
        this.keys[hash] = null;
        return rc;
    }

    /**
     * Ensure that we have space in the local key array for the target index.
     * 
     * @param index
     */
    @SuppressWarnings("unchecked")
    private void validateKey(int index) {
        final int size = (index + 1);
        if (null == this.keys) {
            // nothing has been created yet
            this.keys = (K[]) new Object[size];
        } else if (index >= this.keys.length) {
            // this row puts us beyond the current storage
            Object[] newKeys = new Object[size];
            System.arraycopy(this.keys, 0, newKeys, 0, this.keys.length);
            this.keys = (K[]) newKeys;
        }
    }

    /**
     * Validate that the storage table contains the provided row. This will
     * allocate new space if that is required.
     * 
     * @param targetRow
     */
    @SuppressWarnings("unchecked")
    private void validateTable(int targetRow) {
        // TODO pooling of the arrays?
        if (null == this.values) {
            // nothing has been created yet
            int size = (targetRow + 1);
            if (SIZE_TABLE > size) {
                size = SIZE_TABLE;
            }
            this.values = (V[][]) new Object[size][];
        } else if (targetRow >= this.values.length) {
            // this row puts us beyond the current storage
            final int size = (targetRow + 1);
            Object[][] newTable = new Object[size][];
            System.arraycopy(this.values, 0, newTable, 0, this.values.length);
            this.values = (V[][]) newTable;
        } else if (null != this.values[targetRow]) {
            // we already have this row created and are set
            return;
        }
        // need to create the new row in the table
        this.values[targetRow] = (V[]) new Object[SIZE_ROW];
        for (int i = 0; i < SIZE_ROW; i++) {
            this.values[targetRow][i] = (V) NO_VALUE;
        }
    }

}
