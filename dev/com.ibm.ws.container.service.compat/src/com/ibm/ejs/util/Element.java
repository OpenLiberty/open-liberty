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
 * Element is a "holder" object used by the FasthHashtable Bucket to store
 * the hashtable key alongside each cached object, and provides a 'next'
 * pointer to another Element for use as a linked list. <p>
 * 
 * The key and associated object may not be modified once the Element is
 * created, however it is the responsibility of the Bucket to maintain the
 * 'next' pointer for the linked list. <p>
 **/
//d366845.3 add generic types support
final class Element<K, V> {
    /** The key associated with the contained object. **/
    final K ivKey;

    /** The contained object, associated with key. **/
    final V ivObject;

    /** Pointer to next element in the FastHashtable Bucket. **/
    Element<K, V> ivNext;

    /**
     * Construct an <code>Element</code> object, holding the specified
     * key and object. <p>
     * 
     * @param key the key associated with the object to be held.
     * @param object the object to store in the hashtable bucket.
     **/
    Element(K key, V object) {
        this.ivKey = key;
        this.ivObject = object;
    }

    public String toString() {
        return "Element: " + ivKey.toString() + " " + ivObject.toString();
    }

}
