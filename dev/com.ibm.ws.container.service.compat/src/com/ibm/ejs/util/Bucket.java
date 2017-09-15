/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.util;

/**
 * Bucket is the hash table bucket abstraction for FastHashtable,
 * implementing the basic operations needed. Each bucket is essentially
 * an unsorted colleciton of Element objects. <p>
 **/
// d366845.3 add generic types support
final class Bucket<K, V> {
    /** The head/start of the list of elements in the Bucket. **/
    Element<K, V> ivHead;

    /** The number of elements currently in the Bucket. **/
    private int ivNumElements;

    /**
     * NOTE: FastHashtable lazily constructs Bucket objects using double-checked
     * locking. For this to be safe, the Bucket class must not have any
     * initialization, which includes explicit "= null" or "= 0" for member
     * variables.
     */
    Bucket() {
        // Do not add member variables that require initialization.
    }

    Element<K, V> findByKey(K key) {
        for (Element<K, V> e = ivHead; e != null; e = e.ivNext) {
            if (key.equals(e.ivKey)) {
                return e;
            }
        }

        return null;
    }

    Element<K, V> replaceByKey(K key, V object) {
        final Element<K, V> element = removeByKey(key);
        addByKey(key, object);
        return element;
    }

    void addByKey(K key, V object) {
        Element<K, V> newElement = new Element<K, V>(key, object);
        newElement.ivNext = ivHead;
        ivHead = newElement;
        ivNumElements++;
    }

    Element<K, V> removeByKey(K key) {
        Element<K, V> previous = null;

        for (Element<K, V> e = ivHead; e != null; e = e.ivNext) {
            if (key.equals(e.ivKey)) {
                if (previous == null)
                    ivHead = e.ivNext;
                else
                    previous.ivNext = e.ivNext;
                ivNumElements--;

                return e;
            }
            previous = e;
        }
        return null;
    }

    /**
     * Returns the number of elements in this Bucket / list. <p>
     * 
     * @return the number of elements in this Bucket.
     **/
    public int size() {
        return ivNumElements;
    }

    void clear() {
        ivNumElements = 0;
        ivHead = null;
    }

}
