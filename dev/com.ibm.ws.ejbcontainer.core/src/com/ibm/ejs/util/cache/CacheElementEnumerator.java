/*******************************************************************************
 * Copyright (c) 2002, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.util.cache;

import java.util.Enumeration;
import java.util.NoSuchElementException;
import java.util.Random;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * Enumerates the {@link Element} objects contained within a {@link Cache}.
 * Traverses each bucket within the cache, and each element in those buckets.
 * Each bucket is locked as necessary during the enumeration process.
 * However, the lock is not held continuously, and objects may be
 * inserted or removed at any time. <p>
 *
 * <UL>
 * <li> Elements added to the {@link Cache} since the Enumeration was
 * created, may or may not be returned. <p>
 *
 * <li> Elements removed from the {@link Cache} since the Enumeration
 * was created, may or may not be returned. <p>
 *
 * <li> Elements in the {@link Cache} at the time the Enumeration was
 * created, will be returned unless they have been removed. They
 * may be returned even if they have been removed. <B> Adding or
 * removing elements from the {@link Cache} will not result in
 * elements being skipped by the Enumeration</B>. <p>
 * </UL>
 *
 * As each {@link Bucket} is processed, an internal copy is made of the {@link Bucket}. This allows the Enumeration to avoid skipping
 * elements or returning the same element twice due to changes in the {@link Cache}, but also allows the Enumeration to stay fairly
 * in synch with changes to the {@link Cache}. <p>
 *
 * This <code>Enumeration</code> works more like an <code>Iterator</code>,
 * but for historical reasons implements the <code>Enumeration</code>
 * interface; thus it has been called an <code>Enumerator</code>. <p>
 *
 * @see Cache
 * @see Element
 * @see Bucket
 **/

final class CacheElementEnumerator
                implements Enumeration<Element>
{
    private static final TraceComponent tc =
                    Tr.register(com.ibm.ejs.util.cache.CacheElementEnumerator.class,
                                "EJBCache", "com.ibm.ejs.container.container");

    //
    // Data
    //

    /**
     * Reference to the {@link Cache} being enumerated. The Enumeration
     * will not modify the {@link Cache}.
     **/
    private Cache ivCache = null;

    /** Number of buckets that have been enumerated. **/
    private int ivBucketCount = 0;

    /**
     * Aray index of the hash table bucket ({@link Bucket}) that is
     * currently being enumerated.
     **/
    private int ivBucketIndex = 0;

    /** Copy of the current bucket (corresponds to {@link #ivBucketIndex}). **/
    private Element[] ivBucket = null;

    /** Number of elements in the copy of the current bucket ({@link #ivBucket}). **/
    private int ivBucketSize = 0;

    /** The array index inside the current bucket. **/
    private int ivElementIndex = 0;

    /** Performance/Tuning information: Number of elements returned. **/
    private int ivElementsReturned = 0;

    /** Performance/Tuning information: Maximum bucket size encountered. **/
    private int ivMaxBucketSize = 0;

    /** Performance/Tuning information: Total lookups required for all elements. **/
    private long ivTotalLookups = 0; // d119287

    /** Performance/Tuning information: How many buckets are there of each size. **/
    private final int[] ivBucketSizeStats = new int[21]; // d119287

    //
    // Construction
    //

    /**
     * Constructs an {@link Element} enumerator for the specified {@link Cache}.
     * <p>
     *
     * The Enumeration will pick a random location in the {@link Cache} to start. <p>
     *
     * @param cache the cache to enumerate over.
     **/
    public CacheElementEnumerator(Cache cache)
    {
        ivCache = cache;

        // To gaurantee that elements are not skipped when an element is
        // removed, a copy of each bucket is made as it is accessed.
        // For performance, an array is used.  To avoid resizing the
        // array, insure it is at least big enough for 50 elements.
        // This should accomadate any minimally tuned cache.
        ivBucket = new Element[50];

        // Complete construction of the Enumeration; randomly picking
        // a bucket and initializing the internal bucket copy.
        reset();
    }

    /**
     * Resets the enumerator back to a new state, and randomly positions the
     * enumerator within the {@link Cache}. Elements previously returned will
     * be returned again (if they have not been removed). And, the
     * enumerator will probably not begin returning elements starting from
     * the same location in the {@link Cache} as when created. <p>
     *
     * This method is provided for use witin the cache package for
     * performance. It is intended for use by the constructor
     * as well as strategies which wish to cache and re-use
     * an enumeration to improve performance. <p>
     *
     * For strategies that do cache the enumeration, this method should be
     * called immediately AFTER each use, so that any resources it holds
     * (such as a copy of a Cache bucket) may be released, reducing
     * memory footprint, while not in use. <p>
     *
     * Internal data used for performance monitoring/tuning will
     * be reset. <p>
     **/
    void reset()
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "reset");
        // Pick a random bucket to start with so that the
        // EvictionStrategy doesn't play favorites.
        Random random = new Random();
        ivBucketIndex = random.nextInt(ivCache.getNumBuckets());

        if (isTraceOn && tc.isDebugEnabled())
        {
            // Staticstics about the bucket sizes are normally output when
            // the end of the enumeration is hit, but if the user never
            // hit the end before resetting, then output that info here.
            String stats = "";
            if (ivBucketCount > 0 &&
                (ivBucketCount < ivCache.getNumBuckets() ||
                ivElementIndex < ivBucketSize))
            {
                stats = ", returned = " + ivElementsReturned + " (lookup avg = " +
                        ((float) ivTotalLookups / ivElementsReturned) + // d119287
                        ", max = " + ivMaxBucketSize + ")";
            }

            Tr.debug(tc, "reset : " + ivCache.getName() + " = " + ivCache.numObjects +
                         ", index = " + ivBucketIndex + "/" + ivCache.getNumBuckets() +
                         stats);

            // Reset the Performance/Tuning/Trace data
            ivElementsReturned = 0;
            ivMaxBucketSize = 0;
            ivTotalLookups = 0; // d119287
            for (int i = 0; i < ivBucketSizeStats.length; ++i)
                // d119287
                ivBucketSizeStats[i] = 0;
        }

        // Clear out the copy made of the last bucket enumerated, so the
        // resources aren't held in memory while any 'cached' instances
        // of this class are not in use.                                   d310114
        // Changed to use ivBucket.length instead of ivBucketSize here
        // to resolve a potential problem where the "second to last"
        // bucket swept was larger than the last.   In that case the
        // we would only null out the entries from the last bucket and
        // leave the remaining entries from the previous bucket held
        // in memory.     //PM11713
        java.util.Arrays.fill(ivBucket, null);

        // Indicate that no buckets have been enumerated.
        ivBucketCount = 0;
        ivBucketSize = 0;
        ivElementIndex = 0;

        // Note: Positioning to the first non empty bucket, initializing
        // the internal copy (ivBucket) will not occur until the first use
        // of the enumerator after reset. This will actually skip the bucket
        // randomly picked above, but this is fine, because ivBucketCount
        // is initialized to 0; the bucket skipped now will be the last
        // bucket evaluated.                                               d310114
        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.exit(tc, "reset");
        }
    }

    //
    // Enumeration interface
    //

    /**
     * Tests if this enumeration contains more elements. <p>
     *
     * @return <code>true</code> if and only if this enumeration object
     *         contains at least one more element to provide;
     *         <code>false</code> otherwise.
     **/
    @Override
    public boolean hasMoreElements()
    {
        // If there are more elements in the bucket currently being
        // enumerated or if another non-empty bucket can be found, then
        // the enumeration has more elements.

        if ((ivElementIndex) < ivBucketSize)
        {
            return true;
        }
        else
        {
            return findNextBucket();
        }

    }

    /**
     * Returns the next element of this enumeration if this enumeration
     * object has at least one more element to provide. <p>
     *
     * @return the next element of this enumeration.
     * @exception NoSuchElementException if no more elements exist.
     **/
    @Override
    public Element nextElement()
                    throws NoSuchElementException
    {

        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        // If the current bucket has been exhausted, try to find
        // another non-empty bucket.
        if (ivElementIndex >= ivBucketSize)
        {
            if (!findNextBucket()) {
                // No more non-empty buckets, no more elements
                throw new NoSuchElementException();
            }
        }

        // Return the next element from the current bucket.
        // Note that we do not have to worry about concurrent
        // access to the bucket, as this is actually a copy
        // of the bucket, and not the actual bucket in
        // the Cache being enumerated.

        if (isTraceOn && tc.isDebugEnabled())
            ivElementsReturned++;

        return ivBucket[ivElementIndex++];
    }

    //
    // Extension to Enumeration interface for performance
    //

    /**
     * Returns the next element of this enumeration if this enumeration
     * object has at least one more element to provide; otherwise null. <p>
     *
     * @return the next element of this enumeration or null.
     **/
    // d532639.2
    public Object nextElemNoEx()
    {

        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        // If the current bucket has been exhausted, try to find
        // another non-empty bucket.
        if (ivElementIndex >= ivBucketSize)
        {
            if (!findNextBucket()) {
                // No more non-empty buckets, no more elements
                return null;
            }
        }

        // Return the next element from the current bucket.
        // Note that we do not have to worry about concurrent
        // access to the bucket, as this is actually a copy
        // of the bucket, and not the actual bucket in
        // the Cache being enumerated.

        if (isTraceOn && tc.isDebugEnabled())
            ivElementsReturned++;

        return ivBucket[ivElementIndex++];
    }

    //
    // Internal Methods
    //

    /**
     * Positions the Enumeration on the next non-empty bucket of the {@link Cache}'s hash table; returns <code>true</code> if such
     * a bucket is found, <code>false</code> otherwise. <p>
     *
     * @return <code>true</code> if a non-empty bucket can be found;
     *         <code>false</code> otherwise.
     **/
    private boolean findNextBucket()
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        while (ivBucketCount < ivCache.getNumBuckets())
        {
            ivBucketIndex = (ivBucketIndex + 1) % ivCache.getNumBuckets();
            Bucket bucket = ivCache.getBucketForKey(ivBucketIndex);
            ivBucketCount++;

            if (bucket == null) // d739870
            {
                // Collect Performance data if debug is enabled.
                if (isTraceOn && tc.isDebugEnabled())
                {
                    ++ivBucketSizeStats[0];
                }
                continue;
            }

            synchronized (bucket)
            {
                if (!bucket.isEmpty())
                {
                    ivBucketSize = bucket.size();

                    // Make sure the internal copy is big enough to hold
                    // the new bucket, or the copy will fail.
                    if (ivBucket.length < ivBucketSize)
                    {
                        if (isTraceOn && tc.isDebugEnabled())
                            Tr.debug(tc, ivCache.getName() +
                                         ": Expanding internal bucket from " +
                                         ivBucket.length + " to " + (ivBucketSize + 20));

                        ivBucket = new Element[ivBucketSize + 20];
                    }

                    bucket.toArray(ivBucket); // d133110
                    ivElementIndex = 0;

                    // Collect Performance data if debug is enabled.          d119287
                    if (isTraceOn && tc.isDebugEnabled())
                    {
                        if (ivBucketSize > ivMaxBucketSize)
                            ivMaxBucketSize = ivBucketSize;

                        // Keep track of how many buckets are each size.       d119287
                        if (ivBucketSize < ivBucketSizeStats.length)
                            ++ivBucketSizeStats[ivBucketSize];
                        else
                            ++ivBucketSizeStats[ivBucketSizeStats.length - 1];

                        // Add the number of lookups to find all elements.     d119287
                        for (int j = 1; j <= ivBucketSize; ++j)
                            ivTotalLookups += j;

                        if (ivBucketSize > 100)
                            Tr.debug(tc, ivCache.getName() + ": Hash = " + ivBucketIndex +
                                         ", size = " + ivBucketSize);
                    }

                    return true;
                }
                else
                {
                    // Collect Performance data if debug is enabled.          d119287
                    if (isTraceOn && tc.isDebugEnabled())
                    {
                        ++ivBucketSizeStats[0];
                    }
                }
            }
        }

        // When the end of the enumerator has been hit, dump out some
        // performance/tuning info if trace is enabled and the enumeration
        // actually enumerated through some elements.
        if (isTraceOn && tc.isDebugEnabled() &&
            ivElementsReturned > 0)
        {
            int pCapacity = (int) ((((float) ivElementsReturned) / ivBucketCount) * 100);
            Tr.debug(tc, "Empty : " + ivCache.getName() + " returned = " +
                         ivElementsReturned + ", " + pCapacity + "% capacity" +
                         " (lookup avg = " + ((float) ivTotalLookups / ivElementsReturned) +
                         ", max = " + ivMaxBucketSize + ")");

            // Dump how many buckets were each size.                        d119287
            String bucketStats = "0[" + ivBucketSizeStats[0] + "]";
            for (int i = 1; i < ivBucketSizeStats.length; ++i)
            {
                if (ivBucketSizeStats[i] > 0)
                {
                    bucketStats = bucketStats + ", " + i;
                    if (i == ivBucketSizeStats.length - 1)
                        bucketStats = bucketStats + "+";
                    bucketStats = bucketStats + "[" + ivBucketSizeStats[i] + "]";
                }
            }
            Tr.debug(tc, "Empty : " + ivCache.getName() + " size[buckets] = " + bucketStats);
        }

        return false;
    }
}
