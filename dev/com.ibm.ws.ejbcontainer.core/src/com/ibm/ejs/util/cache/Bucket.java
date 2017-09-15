/*******************************************************************************
 * Copyright (c) 2004, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.util.cache;

/**
 * Bucket is the interface for the hash table bucket abstraction for Cache,
 * defining the basic operations needed. Each bucket should essentially be an
 * unsorted collection of Element objects.
 * 
 * <p>Bucket should not provide any synchronization (though the classes used
 * internally to implement it may). It is assumed that all synchronization
 * is handled externally in the Cache implementation. However, Cache may lazily
 * create buckets, so Bucket implementation must not initialize any non-final
 * state in their constructor, including explicit "= null" or "= 0".
 * 
 * <p>Though Bucket allows multiple objects with the same key, it does not
 * distinguish between them. Operations on a key for which there are
 * multiple objects will be performed against an arbitrary instance
 * (typically the one nearest the "back" of the bucket).
 **/
public interface Bucket
{
    /**
     * Search the specified bucket for the specified key; return
     * the Element holding the target object, or null if there
     * is no object with the key in the bucket.
     * 
     * @param key key of the object element to be returned.
     * @return the Element holding the target object identified by the key.
     */
    public Element findByKey(Object key);

    /**
     * Insert the specified object into the bucket and associate it with
     * the specified key. Returns the Element which holds the newly-
     * inserted object.
     * 
     * @param key key of the object element to be inserted.
     * @param object the object to be inserted for the specified key.
     * @return the Element holding the target object just inserted.
     **/
    public Element insertByKey(Object key, Object object);

    /**
     * Similar to removeByKey, avoids throwing an exception when an object
     * cannot be evicted, because it is pinned, instead returns null.
     * This method is primarily for use with eviction strategies.
     * 
     * @param key key of the object element to be removed.
     * @return the Element holding the removed object.
     **/
    public Element discardByKey(Object key);

    /**
     * Remove the object associated with the specified key from the bucket.
     * Returns the Element holding the removed object.
     * 
     * @param key key of the object element to be removed.
     * @return the Element holding the removed object.
     **/
    public Element removeByKey(Object key);

    /**
     * Remove the object associated with the specified key from the bucket.
     * Returns the Element holding the removed object. Optionally drops
     * a reference on the object before removing it
     * 
     * @param key key of the object element to be removed.
     * @param dropRef true indicates a reference should be dropped
     * @return the Element holding the removed object.
     **/
    public Element removeByKey(Object key, boolean dropRef);

    /**
     * Tests if this bucket has no elements.
     * 
     * @return true if this bucket has no elements; false otherwise.
     **/
    public boolean isEmpty();

    /**
     * Returns the number of elements in this list. <p>
     * 
     * @return the number of elements in this list.
     **/
    public int size();

    /**
     * Copies elements from the bucket into the destination array starting at
     * index 0. The destination array must be at least large enough to hold all
     * the elements in the bucket; otherwise, the behavior is undefined.
     * 
     * @param dest the destination array
     */
    public void toArray(Element[] array);
}
