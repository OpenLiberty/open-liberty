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

import java.util.HashMap;

import com.ibm.ejs.container.EJSWrapperCommon;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * WrapperBucket is a type specific implementation of the hash table bucket
 * abstraction for Cache, implementing the basic operations needed.
 * Each bucket is essentially an unsorted colleciton of Element objects,
 * which are EJSWrapperCommon objects. <p>
 * 
 * WrapperBucket does not provide any synchronization (though the classes used
 * internally to implement it may). It is assumed that all sychronization
 * is handled externally in the Cache implementation. <p>
 * 
 * Unlike BucketImpl, WrapperBucket does NOT allow multiple objects with the
 * same key. An IllegalArgumentException will be thrown if an attempt is
 * made to insert an object with a duplicate key. <p>
 **/
public final class WrapperBucket implements Bucket
{
    private static final TraceComponent tc =
                    Tr.register(com.ibm.ejs.util.cache.WrapperBucket.class,
                                "EJBCache", "com.ibm.ejs.container.container");

    private static final int DEFAULT_BUCKET_SIZE = 3; // d218859

    /** Reference to the Cache which contains this bucket instance. **/
    public final Cache ivWrapperCache;

    // For performance, the elements of the WrapperBucket will be held in a
    // HashMap. Since the elements have already been 'hashed' just to get to
    // this bucket, it might not be obvious that storing them in a HashMap
    // would be helpful.  However, since not all elements in this bucket have
    // the same hashcode... as long as the algorithm to place in a bucket is
    // different for a HashMap, they will still be distributed.  As long as
    // the 'capacity' of the HashMap remains different from that of the EJB
    // Wrapper Cache... the distribution should also be different.  Also, the
    // HashMap implementation actually just uses an Array, where each offset
    // is like a bucket... so the storage consumed by a HashMap is very
    // similar to that of an Array (like the prior implementation).       d535968

    /** Elements (EJSWrapperCommons) held by a bucket instance. **/
    private HashMap<Object, EJSWrapperCommon> ivWrappers;

    // --------------------------------------------------------------------------
    // Construction
    // --------------------------------------------------------------------------

    /**
     * Constructs an instance of WrapperBucket for use in the the specified
     * Cache. <p>
     * 
     * The purpose of the 'Cache' parameter is to allow element objects within
     * the bucket (EJSWrapperCommons) to access the Cache.touch() method. <p>
     * 
     * NOTE: Cache lazily constructs buckets using double-checked locking. For
     * this to be safe, the Bucket class must not have any non-final member
     * initialization, which includes explicit "= null" or "= 0". <p>
     * 
     * @param cache the cache that will contain the new WrapperBucket instance.
     **/
    WrapperBucket(Cache cache)
    {
        // Do not add non-final member variables that require initialization.
        ivWrapperCache = cache;
    }

    // --------------------------------------------------------------------------
    // Bucket Interface methods
    // --------------------------------------------------------------------------

    /**
     * Search the specified bucket for the specified key; return
     * the Element holding the target object, or null if there
     * is no object with the key in the bucket.
     * 
     * @param key key of the object element to be returned.
     * @return the Element holding the target object identified by the key.
     */
    public Element findByKey(Object key)
    {
        Element element = null;

        // Note : Unlike BucketImpl (EJB Cache), the WrapperBucket (Wrapper Cache)
        // does NOT support duplicate entries. Therefore, the order of the search
        // is not important and a Map may be used instead of an Array.     d535968

        if (ivWrappers != null)
        {
            element = ivWrappers.get(key);
        }

        return element;
    }

    /**
     * Insert the specified object into the bucket and associate it with
     * the specified key. Returns the Element which holds the newly-
     * inserted object.
     * 
     * @param key key of the object element to be inserted.
     * @param object the object to be inserted for the specified key.
     * @return the Element holding the target object just inserted.
     **/
    public Element insertByKey(Object key, Object object)
    {
        EJSWrapperCommon element = (EJSWrapperCommon) object;

        if (ivWrappers == null)
        {
            ivWrappers = new HashMap<Object, EJSWrapperCommon>(DEFAULT_BUCKET_SIZE);
        }

        Object duplicate = ivWrappers.put(key, element);

        if (duplicate != null)
        {
            // The wrapper cache does not support duplicates.... however, the
            // first time a home is looked up, and keyToObject faults it in,
            // deferred init will initialize it and insert it into the cache
            // before returning to faultOnKey where it will be done AGAIN!
            // In the future, it would be nice if deferred init would NOT
            // add the home to the cache, but until then, it is fine to just
            // ignore this and go on.... at least this code will only allow
            // it in the cache once.                                        d535968
            if (duplicate == element)
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "insertByKey : same object again : " + object);
            }
            else
            {
                // It is totally unacceptable to have more than one
                // EJSWrapperCommon instance per key.                        d535968
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "insertByKey : attempt to insert duplicate : " + key +
                                 " : " + object + " : " + duplicate);
                throw new IllegalArgumentException("Attempt to insert duplicate element into EJB Wrapper Cache");
            }
        }

        // Give the element (EJSWrapperCommon) access to the bucket, so it
        // may syncronize on it, and also reset the pin count to 0,
        // a non-negative, to indiate the element is now in the cache.
        element.ivBucket = this;
        element.pinned = 0;

        return element;
    }

    /**
     * Similar to removeByKey, avoids throwing an exception when an object
     * cannot be evicted, because it is pinned, instead returns null.
     * This method is primarily for use with eviction strategies.
     * 
     * @param key key of the object element to be removed.
     * @return the Element holding the removed object.
     **/
    public Element discardByKey(Object key)
    {
        Element element = (ivWrappers != null) ? ivWrappers.get(key) : null;

        if (element != null)
        {
            if (element.pinned > 0)
            {
                return null;
            }
            remove(key);
        }

        return element;
    }

    /**
     * Remove the object associated with the specified key from the bucket.
     * Returns the Element holding the removed object.
     * 
     * @param key key of the object element to be removed.
     * @return the Element holding the removed object.
     **/
    public Element removeByKey(Object key)
    {
        return removeByKey(key, false);
    }

    /**
     * Remove the object associated with the specified key from the bucket.
     * Returns the Element holding the removed object. Optionally drops
     * a reference on the object before removing it
     * 
     * @param key key of the object element to be removed.
     * @param dropRef true indicates a reference should be dropped
     * @return the Element holding the removed object.
     **/
    public Element removeByKey(Object key, boolean dropRef)
    {
        Element element = (ivWrappers != null) ? ivWrappers.get(key) : null;

        if (element != null)
        {
            // Objects must either be unpinned, or pinned only once
            // (presumably by the caller)
            if ((!dropRef && element.pinned > 0) ||
                (dropRef && element.pinned > 1)) {
                throw new IllegalOperationException(key, element.pinned);
            }
            remove(key);
        }

        return element;
    }

    /**
     * Tests if this bucket has no elements.
     * 
     * @return true if this bucket has no elements; false otherwise.
     **/
    public boolean isEmpty()
    {
        return (ivWrappers == null || ivWrappers.isEmpty());
    }

    /**
     * Returns the number of elements in this list. <p>
     * 
     * @return the number of elements in this list.
     **/
    public int size()
    {
        return (ivWrappers != null) ? ivWrappers.size() : 0;
    }

    /**
     * Returns an array containing all of the elements in this list in the
     * correct order. The runtime type of the returned array is that of the
     * specified array. If the list fits in the specified array, it is
     * returned therein. Otherwise, a new array is allocated with the runtime
     * type of the specified array and the size of this list. <p>
     * 
     * If the list fits in the specified array with room to spare (i.e., the
     * array has more elements than the list), the element in the array
     * immediately following the end of the collection is set to null. This
     * is useful in determining the length of the list only if the caller
     * knows that the list does not contain any null elements. <p>
     * 
     * @param array the array into which the elements of the list are to be
     *            stored, if it is big enough; otherwise, a new array of
     *            the same runtime type is allocated for this purpose.
     **/
    public void toArray(Element[] array)
    {
        if (ivWrappers != null)
        {
            ivWrappers.values().toArray(array);
        }
    }

    // --------------------------------------------------------------------------
    // Internal / Private  methods
    // --------------------------------------------------------------------------

    /**
     * Removes the element with the specified key from the list. <p>
     * 
     * This internal method must NOT be called unless the element is
     * known to be in the list of elements. <p>
     * 
     * @param key key of the object element to be removed.
     * @return the element that was removed from the list.
     **/
    private EJSWrapperCommon remove(Object key)
    {
        EJSWrapperCommon removed = ivWrappers.remove(key);

        // Indicate that the element (EJSWrapperCommon) is no longer in the
        // cache by setting the pin count to a negative... note that the
        // bucket reference must not be reset to null in order to allow
        // the element to synchronize on the bucket to determine if it
        // is in the cache or not.
        removed.pinned = -1; // negative is no longer in cache.

        return removed;
    }
}
