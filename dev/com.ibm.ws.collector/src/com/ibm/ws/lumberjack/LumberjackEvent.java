/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2016
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.lumberjack;

import java.util.ArrayList;

/**
 * Class that holds data relevant to Lumberjack protocol
 * Creates a list of entry objects where each entry holds a key/value pair
 * Add and get are O(1) operations, simple add and remove using the index from a pre-allocated array.
 */
public class LumberjackEvent<K, V> {

    private final ArrayList<Entry<K, V>> entryList;
    private final int initialCapcity = 3;

    public LumberjackEvent() {
        //Set the capacity to three as we wont have more than three key/value pairs.
        entryList = new ArrayList<Entry<K, V>>(initialCapcity);
    }

    public void add(Entry<K, V> entry) {
        entryList.add(entry);
    }

    public Entry<K, V> get(int index) {
        return entryList.get(index);
    }

    public int size() {
        return entryList.size();
    }

    public static class Entry<K, V> {
        private final K key;
        private final V value;

        public Entry(K key, V value) {
            this.key = key;
            this.value = value;
        }

        public K getKey() {
            return key;
        }

        public V getValue() {
            return value;
        }
    }
}
