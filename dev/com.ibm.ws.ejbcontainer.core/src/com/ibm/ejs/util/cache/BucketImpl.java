/*******************************************************************************
 * Copyright (c) 1998, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.util.cache;

import java.util.Arrays;

/**
 * BucketImpl is the hash table bucket abstraction for Cache, implementing the
 * basic operations needed. Each bucket is essentially an unsorted colleciton
 * of Element objects.
 * 
 * BucketImpl does not provide any synchronization (though the classes used
 * internally to implement it may). It is assumed that all sychronization
 * is handled externally in the Cache implementation.
 * 
 * Though BucketImpl allows multiple objects with the same key, it does not
 * distinguish between them. Operations on a key for which there are
 * multiple objects will be performed against an arbitrary instance
 * (typically the one nearest the "back" of the bucket).
 **/
final class BucketImpl
                implements Bucket // d195605
{
    /**
     * The default capacity of the bucket when it is first created.
     */
    static final int DEFAULT_CAPACITY = 10;

    /**
     * The storage for the elements contained in this bucket. The storage must
     * support {@link #add} at the end but {@link #remove} with arbitrary index.
     */
    private Element[] ivElements;

    /**
     * The lowest index in {@link #ivElements} that contains data. This value
     * must always be less than or equal to {@link #ivTailIndex}.
     */
    private int ivHeadIndex;

    /**
     * The index for adding the next element via {@link #add}. If this value is
     * set to the length of {@link #ivElements}, then either a bigger array needs
     * to be allocated, or {@link #ivHeadIndex} needs to be reset to 0 and
     * elements in the array need to be copied.
     */
    private int ivTailIndex;

    /**
     * NOTE: Cache lazily constructs buckets using double-checked locking. For
     * this to be safe, the Bucket class must not have any non-final member
     * initialization, which includes explicit "= null" or "= 0".
     */
    BucketImpl()
    {
        // Do not add non-final member variables that require initialization.
    }

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
        // Traverse the vector from the back. Given proper locking done at
        // a higher level, this will ensure that the cache gets to the same
        // element in the presence of duplicates.
        // Traversing the vector from the first element causes a problem since
        // elements are inserted at the end of the vector (for efficiency).
        // Consequently after inserting duplicate element 1.2, if another
        // operation is performed on the same key 1, we will get to 1.1 instead
        // of 1.2 (see findIndexByKey).                                    d139586
        for (int i = (size() - 1); i >= 0; --i)
        {
            Element element = (Element) get(i);
            if (element.key.equals(key)) {
                return element;
            }
        }

        return null;
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
        Element element = new Element(this, key, object);
        add(element);
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
        int i = findIndexByKey(key);
        Element element = null;

        if (i != -1) {
            element = (Element) get(i);

            if (element.pinned > 0) {
                return null;
            }
            remove(i);
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
        int i = findIndexByKey(key);
        Element element = null;

        // Check if the object is in the cache
        if (i != -1)
        {
            element = (Element) get(i);

            // Objects must either be unpinned, or pinned only once
            // (presumably by the caller)
            if ((!dropRef && element.pinned > 0) ||
                (dropRef && element.pinned > 1)) {
                throw new IllegalOperationException(key, element.pinned);
            }

            remove(i);
        }

        return element;
    }

    // --------------------------------------------------------------------------
    // Private / Internal methods
    // --------------------------------------------------------------------------

    /**
     * Return the index of the element with the specified key
     **/
    private int findIndexByKey(Object key)
    {
        // Traverse the vector from the back. Given proper locking done at
        // a higher level, this will ensure that the cache gets to the same
        // element in the presence of duplicates.
        // Traversing the vector from the first element causes a problem since
        // elements are inserted at the end of the vector (for efficiency).
        // Consequently after inserting duplicate element 1.2, if another
        // operation is performed on the same key 1, we will get to 1.1 instead
        // of 1.2
        for (int i = (size() - 1); i >= 0; --i) {
            Element element = (Element) get(i);
            if (element.key.equals(key)) {
                return i;
            }
        }

        return -1;
    }

    // --------------------------------------------------------------------------
    // List methods
    // --------------------------------------------------------------------------

    /**
     * Returns true if the bucket is empty.
     */
    public boolean isEmpty()
    {
        return ivTailIndex == ivHeadIndex;
    }

    /**
     * Returns the number of elements in the bucket.
     */
    public int size()
    {
        return ivTailIndex - ivHeadIndex;
    }

    /**
     * Copies elements from the bucket into the destination array starting at
     * index 0. The destination array must be at least large enough to hold all
     * the elements in the bucket; otherwise, the behavior is undefined.
     * 
     * @param dest the destination array
     */
    public void toArray(Element[] dest)
    {
        if (ivElements != null)
        {
            System.arraycopy(ivElements, ivHeadIndex, dest, 0, size());
        }
    }

    /**
     * Gets the element at the specified index. The index must be greater or
     * equal to 0 and less than the size; otherwise, the behavior is undefined.
     * 
     * @param listIndex the index into the bucket
     * @return the specified element
     */
    private Element get(int listIndex)
    {
        return ivElements[ivHeadIndex + listIndex];
    }

    /**
     * Adds an element to the end of the bucket.
     * 
     * @param element the element to add
     */
    private void add(Element element)
    {
        if (ivElements == null)
        {
            ivElements = new Element[DEFAULT_CAPACITY];
        }
        else if (ivTailIndex == ivElements.length)
        {
            // No more room at the tail of the array.  If we're completely out of
            // space (ivBaseIndex == 0), then we need a bigger array.  Otherwise,
            // determine if we can reset ivBaseIndex to 0 without needing to copy
            // more than half the elements.  If not, we allocate a new array.
            //
            // We choose to limit to half the array to avoid repeatedly copying
            // the array.  For example, if the array is full and ivBaseIndex == 0,
            // and we have a sequence of remove(0)/add(x), we do not want:
            //   - remove(0): ivBaseIndex++
            //   - add(x): copy 1..N to 0..N-1, ivBaseIndex=0, array[N] = x
            //   - remove(0): ivBaseIndex++
            //   - add(x): copy 1..N to 0..N-1, ivBaseIndex=0, array[N] = x
            //   - ...etc.

            int size = size();
            int halfCapacity = ivElements.length >> 1;
            if (ivHeadIndex > halfCapacity)
            {
                // Less than half of the array is full.  Rather than creating a new
                // array, just copy all the elements to the front.
                System.arraycopy(ivElements, ivHeadIndex, ivElements, 0, size);
                Arrays.fill(ivElements, ivHeadIndex, ivElements.length, null);
            }
            else
            {
                // Either we're completely out of space (ivBaseIndex == 0), or it
                // would be wasteful to continuously copy over half the elements.
                // Grow the array by half its current size.
                Element[] newElements = new Element[ivElements.length + halfCapacity];
                System.arraycopy(ivElements, ivHeadIndex, newElements, 0, size);
                ivElements = newElements;
            }

            ivHeadIndex = 0;
            ivTailIndex = size;
        }

        ivElements[ivTailIndex++] = element;
    }

    /**
     * Removes the element at the specified index. The index must be greater or
     * equal to 0 and less than the size; otherwise, the behavior is undefined.
     * 
     * @param listIndex the index of the element to remove
     */
    private void remove(int listIndex)
    {
        if (listIndex == 0)
        {
            // Trivially remove from head.
            ivElements[ivHeadIndex++] = null;
        }
        else if (listIndex == ivTailIndex - 1)
        {
            // Trivially remove from tail.
            ivElements[--ivTailIndex] = null;
        }
        else
        {
            // Determine whether shifting the head or the tail requires the lower
            // number of element copies.

            int size = size();
            int halfSize = size >> 1;
            if (listIndex < halfSize)
            {
                // The index is less than half.  Shift the elements at the head of
                // the array up one index to cover the removed element.
                System.arraycopy(ivElements, ivHeadIndex, ivElements, ivHeadIndex + 1, listIndex);
                ivElements[ivHeadIndex++] = null;
            }
            else
            {
                // The index is more than half.  Shift the elements at the tail of
                // the array down one index to cover the removed element.
                int arrayIndex = ivHeadIndex + listIndex;
                System.arraycopy(ivElements, arrayIndex + 1, ivElements, arrayIndex, size - listIndex - 1);
                ivElements[--ivTailIndex] = null;
            }
        }

        if (isEmpty())
        {
            // Reset ivHeadIndex to 0 to avoid element copies in add().
            ivHeadIndex = 0;
            ivTailIndex = 0;
        }
    }
}
