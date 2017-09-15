/*******************************************************************************
 * Copyright (c) 1998, 2014 IBM Corporation and others.
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

import com.ibm.ejs.container.util.locking.LockTable; // PK04804
import com.ibm.ejs.util.MathUtil;
import com.ibm.websphere.csi.CacheElement;
import com.ibm.websphere.csi.DiscardException;
import com.ibm.websphere.csi.DiscardStrategy;
import com.ibm.websphere.csi.EJBCache;
import com.ibm.websphere.csi.FaultException;
import com.ibm.websphere.csi.FaultStrategy;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ejbcontainer.diagnostics.IntrospectionWriter;
import com.ibm.ws.ejbcontainer.diagnostics.TrDumpWriter;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.util.cache.DiscardWithLockStrategy; // PK04804

/**
 * Implements a configurable, associative cache. Keys and the objects
 * which they identify may be any Java classes; the only requirement is
 * for key classes to implement <code>hashCode()</code> and
 * <code>equals()</code>. <p>
 * 
 * The cache is based on a hash table which implements bucket-level
 * locking to increase concurrency. The number of buckets in the hash
 * table can be configured at the time a <code>Cache</code> object is
 * constructed. The number of buckets does not change once the
 * cache has been created. <p>
 * 
 * Objects in the cache may be "pinned", preventing them from being
 * evicted from the cache. Objects should be pinned whenever in use,
 * unless special provision has been made for handling eviction. The
 * <code>find()</code>, and <code>insert()</code> operations automatically
 * pin the specified object before they return; this obviates the need for
 * external synchronization and pinning during simple operations. An object
 * may be pinned multiple times, in effect incrementing a reference
 * count on the object; it must be unpinned an equal number of times
 * before the object becomes eligible for eviction. <p>
 * 
 * Note that <code>findAndFault()</code> does not return the object in the
 * pinned state. This is an optimization that has been done for performance.
 * Any LRU data will have been updated, similar to performing an
 * <code>unpin</code>. <p>
 * 
 * <DL>
 * <DD>{@link Cache#pin} <DD>{@link Cache#unpin} <DD>{@link Cache#find} <DD>{@link Cache#findAndFault} <DD>{@link Cache#insert} </DL> <p>
 * 
 * The cache has several configurable behaviors, which allow for
 * flexibility in resource management:
 * <DL>
 * <DD>{@link FaultStrategy} - Provides a mechanism for customizing
 * behavior when a miss occurs during <code>findAndFault()</code>.
 * Allows implementation of atomic fault-in. <p>
 * 
 * 
 * <DD>{@link EvictionStrategy} - Provides a mechanism for customizing
 * how victims are selected for eviction. Allows the use of any
 * pertinent heuristic for victim selection. <p>
 * 
 * 
 * <DD>{@link DiscardStrategy} - Provides a mechanism to hook garbage
 * collection or other clean up code into the cache. Specialized
 * processing which must take place when an object is evicted from the
 * cache may be implemented here. <p>
 * </DL>
 * <p>
 * 
 * @see com.ibm.websphere.csi.FaultStrategy
 * @see EvictionStrategy
 * @see BackgroundLruEvictionStrategy
 * @see SweepLruEvictionStrategy
 * @see com.ibm.websphere.csi.DiscardStrategy
 * @see com.ibm.ejs.container.activator.Activator
 * @see com.ibm.ejs.container.WrapperManager
 **/

public final class Cache implements EJBCache {
    //
    // Construction
    //

    /**
     * Construct a <code>Cache</code> object with the specified number of
     * buckets in it's hash table. <p>
     * 
     * @param name Configured name used to identify the cache instance.
     * @param numBuckets the number of hash table buckets to use.
     * @param wrappers true if this is to be a cache of EJB Wrappers
     **/
    public Cache(String name, int numBuckets, boolean wrappers) // d195605
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "<init> (" + name + ", numBuckets = " + numBuckets +
                         ", wrappers = " + wrappers + ")");

        // For better hashing, bump the number of buckets to a prime.      d122324
        numBuckets = MathUtil.findNextPrime(numBuckets);

        this.ivName = name; // d129562
        this.numBuckets = numBuckets;
        this.wrappers = wrappers;
        buckets = new Bucket[numBuckets];

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "<init> (" + name + ", numBuckets = " + this.numBuckets + ")");
    }

    /**
     * Construct a <code>Cache</code> object with the specified number of
     * buckets in it's hash table. <p>
     * 
     * @param name Configured name used to identify the cache instance.
     * @param numBuckets the number of hash table buckets to use.
     * @param wrappers true if this is to be a cache of EJB Wrappers
     **/
    public Cache(String name, long numBuckets, boolean wrappers) // d195605
    {
        this(name, (int) numBuckets, wrappers); // d129562 d195605
    }

    //
    // Attributes
    //

    /**
     * Returns the number of objects stored in the cache. <p>
     * 
     * @return the number of objects stored in the cache.
     **/
    @Override
    public final int getSize()
    {
        return numObjects;
    }

    /**
     * Returns the number of buckets in the <code>Cache</code> hash table. <p>
     * 
     * @return the number of buckets in the <code>Cache</code> hash table.
     **/
    @Override
    public final int getNumBuckets()
    {
        return numBuckets;
    }

    /**
     * Return the name of the cache, used to identify it.
     **/
    // d129562
    @Override
    public String getName()
    {
        return ivName;
    }

    /**
     * Install a {@link FaultStrategy} for the cache, which controls
     * what actions are taken when a cache miss occurs during {@link #findAndFault findAndFault()}. <p>
     * 
     * @param strategy An instance of a <code>FaultStrategy</code>.
     *            <p>
     * @see com.ibm.websphere.csi.FaultStrategy
     * @see com.ibm.ejs.container.WrapperManager
     **/
    @Override
    public final void setFaultStrategy(FaultStrategy strategy)
    {
        faultStrategy = strategy;
    }

    /**
     * Install an {@link EvictionStrategy} for the cache, which controls
     * which objects are evicted when the cache reaches capacity. <p>
     * 
     * @param strategy An instance of an <code>EvictionStrategy</code>.
     *            <p>
     * @see EvictionStrategy
     * @see BackgroundLruEvictionStrategy
     * @see SweepLruEvictionStrategy
     **/
    public final void setEvictionStrategy(EvictionStrategy strategy)
    {
        evictionStrategy = strategy;
    }

    /**
     * Install a {@link DiscardStrategy} for the cache, which provides
     * a mechanism for cleaning up objects which are evicted from the cache.
     * <p>
     * 
     * @param strategy An instance of a <code>DiscardStrategy</code>.
     *            <p>
     * @see com.ibm.websphere.csi.DiscardStrategy
     * @see com.ibm.ejs.container.activator.Activator
     * @see com.ibm.ejs.container.WrapperManager
     **/
    @Override
    public final void setDiscardStrategy(DiscardStrategy strategy)
    {
        discardStrategy = strategy;

        // Determine if the DiscardStrategy requires locking for eviction
        // processing.                                                     PK04804
        if (strategy instanceof DiscardWithLockStrategy)
        {
            ivEvictionLocks =
                            ((DiscardWithLockStrategy) strategy).getEvictionLockTable();
        }
    }

    @Override
    public void setSweepInterval(long sweepInterval)
    {
        evictionStrategy.setSweepInterval(sweepInterval);
    }

    @Override
    public void setCachePreferredMaxSize(int maxSize)
    {
        evictionStrategy.setPreferredMaxSize(maxSize);
    }

    //
    // Operations
    //

    /**
     * Check whether or not there is an object in the cache associated with
     * the specified key. If such an object does exist it's pinned state
     * is not affected. <p>
     * 
     * @param key key to check for in the cache.
     * 
     * @return true if there is an object in the cache associated with the
     *         specified key, false otherwise.
     **/
    @Override
    public boolean contains(Object key)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "contains", key);

        Bucket bucket = getBucketForKey(key);

        Element element = null; // d173022.12
        if (bucket != null) // d739870
        {
            synchronized (bucket)
            {
                element = bucket.findByKey(key);
            } // d173022.12
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "contains", new Boolean(element != null));

        return element != null;
    }

    /**
     * Find the specified key in the cache; the object is returned
     * in the pinned state. <p>
     * 
     * @param key key for the object to locate in the cache.
     * 
     * @return the object from the cache, or null if there is no object
     *         associated with the key in the cache.
     *         <p>
     * @see Cache#pin
     * @see Cache#unpin
     **/
    @Override
    public Object find(Object key)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "find", key);

        Bucket bucket = getBucketForKey(key);
        Object object = null;

        if (bucket != null) // d739870
        {
            synchronized (bucket)
            {
                Element element = bucket.findByKey(key);

                if (element != null)
                {
                    // We found the object in the bucket; pin it so it won't be
                    // evicted and return it to the caller
                    element.pinned++;
                    object = element.object;
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "find", object);

        return object;
    }

    // d173022.12 Begins
    /**
     * Return element for the given key but do not pin the element. In addition,
     * if object is found, add adjustPinCount to the pinned state. <p>
     * 
     * Note: This method is added to support the scenario in the activation
     * strategy classes that multiple cache.unpin() are called immediately
     * after the find() operation to negate the pin operation from the
     * find() as well as other unpin requires to unwind the other pin
     * operation initiated from transaction and/or activation operation.
     * This is mainly designed for performance. Typically this will save
     * about between 75 to 428 instructions or even more depending on
     * the hashcode implementation of the key object. <p>
     * 
     * @param key key for the object to locate in the cache.
     * @param adjustPinCount additional pin adjustment count.
     * 
     * @return the object from the cache, or null if there is no object
     *         associated with the key in the cache.
     *         <p>
     * @see Cache#pin
     * @see Cache#unpin
     **/
    @Override
    public Object findDontPinNAdjustPinCount(Object key, int adjustPinCount)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "findDontPinNAdjustPinCount", key +
                                                       ", adujust pin count=" + adjustPinCount);

        Bucket bucket = getBucketForKey(key);
        Object object = null;

        if (bucket != null) // d739870
        {
            synchronized (bucket)
            {
                Element element = bucket.findByKey(key);
                if (element != null)
                {
                    // Touch the LRU flag; since an object cannot be evicted when
                    // pinned, this is the only time when we bother to set the
                    // flag
                    element.accessedSweep = numSweeps;

                    object = element.object;
                    element.pinned += adjustPinCount;
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "findDontPinNAdjustPinCount", object);

        return object;
    }

    // d173022.12 Ends

    /**
     * Find the specified key in the cache; the object is returned without
     * an additional pin. If there is no object in the cache with the
     * specified key, the <code>FaultStrategy</code>, if installed, is
     * invoked. <p>
     * 
     * If the returned object were previously pinned, it will be returned
     * in the pinned state, but an additional pin will not be obtained.
     * An <code>unpin</code> should not be performed in conjunction with
     * a call to <code>findAndFault</code>. Any LRU data maintained by
     * the <code>Cache</code> will be updated, similar to performing
     * an <code>unpin</code>. <p>
     * 
     * Note that <code>Cache</code> will hold the bucket lock when calling
     * the <code>FaultStrategy</code>. <p>
     * 
     * @param key key for the object to locate in the cache.
     * 
     * @return the object from the cache; if the object is not in the
     *         cache and the <code>FaultStrategy</code> does not create it,
     *         null is returned.
     * 
     * @exception FaultException if an Exception occurs in the FaultStrategy.
     *                <p>
     * @see Cache#pin
     * @see Cache#unpin
     **/
    @Override
    public Object findAndFault(Object key)
                    throws FaultException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "findAndFault", key);

        Bucket bucket = getOrCreateBucketForKey(key); // d739870
        Element element;
        Object object = null;

        synchronized (bucket)
        {
            element = bucket.findByKey(key);

            if (element != null)
            {
                // We found the object in the bucket; do not pin for performance,
                // update the LRU data, and return it to the caller        d140003.5
                element.accessedSweep = numSweeps;

                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    Tr.exit(tc, "findAndFault : found in cache");
                return element.object;

            } else if (faultStrategy == null)
            {
                // We could not find the object and no FaultStrategy is
                // installed.
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    Tr.exit(tc, "findAndFault : not in cache : no FaultStrategy");
                return null;
            }

            // We could not find the object in the bucket: we'll notify
            // the FaultStrategy so that it can attempt to fault the
            // object in. We'll hold the lock on the bucket while we do this

            try
            {
                // Notify the FaultStrategy and see if it is able to fault in the
                // object

                object = faultStrategy.faultOnKey(this, key);

                if (object != null)
                {
                    // The object was faulted in, and must be inserted into
                    // the cache.

                    // Insert the object.
                    element = bucket.insertByKey(key, object);

                    // Just update the LRU data - do not pin.               d140003.5
                    element.accessedSweep = numSweeps;
                }
            } catch (Exception e)
            {
                FFDCFilter.processException(e, CLASS_NAME + ".findAndFault",
                                            "417", this);
                throw new FaultException(e, key.toString()); //PK59118
            }
        }

        if (object != null) {
            synchronized (this) {
                // ACK! This is going to be a choke point
                numObjects++;
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "findAndFault : not in cache : faulted");

        return object;
    }

    /**
     * Remove the object associated with the specified key from the cache. <p>
     * 
     * In order to remove an object from the cache, the object must either
     * not be pinned or must be pinned exactly once, presumably by the
     * caller. Note, however, that there is no way to verify that the caller
     * actually owns the pinned reference. <p>
     * 
     * This relaxation is needed in order to allow for situations where
     * an object needs to be removed from the cache but remain in use. <p>
     * 
     * Also note that if an object is removed while still pinned, the final
     * call to unpin is unnecessary, and is also illegal. <p>
     * 
     * @param key the key for the object to remove from the cache.
     * @param dropRef if true, drop a reference on the object before
     *            removing from the cache.
     * 
     * @return the object which was removed from the cache.
     * 
     * @exception IllegalOperationException if the object associated with
     *                the key is currently pinned more than once.
     **/
    @Override
    public Object remove(Object key, boolean dropRef)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "remove", new Object[] { key, new Boolean(dropRef) });

        Bucket bucket = getBucketForKey(key);
        Object object = null;

        if (bucket != null) // d739870
        {
            synchronized (bucket) {
                Element element = bucket.removeByKey(key, dropRef);
                object = element != null ? element.object : null;
            }

            if (object != null) {
                synchronized (this) {
                    // ACK! This is going to be a choke point
                    numObjects--;
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "remove", object);

        return object;
    }

    /**
     * Remove the specified element from the cache. <p>
     * 
     * In order to remove an element from the cache, the element must either
     * not be pinned or must be pinned exactly once, presumably by the
     * caller. Note, however, that there is no way to verify that the caller
     * actually owns the pinned reference. <p>
     * 
     * This relaxation is needed in order to allow for situations where
     * an object needs to be removed from the cache but remain in use. <p>
     * 
     * Also note that if an element is removed while still pinned, the final
     * call to unpin is unnecessary, and is also illegal. <p>
     * 
     * @param cacheElement the element to remove from the cache.
     * @param dropRef if true, drop a reference on the object before
     *            removing from the cache.
     * 
     * @exception IllegalOperationException if the object associated with
     *                the key is currently pinned more than once.
     */
    // F61004.6
    @Override
    public void removeElement(CacheElement cacheElement, boolean dropRef)
    {
        Element element = (Element) cacheElement;

        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "remove", new Object[] { element.key, dropRef });

        Bucket bucket = element.ivBucket;
        synchronized (bucket)
        {
            bucket.removeByKey(element.key, dropRef);
        }

        synchronized (this)
        {
            // ACK! This is going to be a choke point
            numObjects--;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "remove", element.object);
    }

    /**
     * Discard the object associated with the specified key from the cache.
     * The object is removed from the cache and the cache's DiscardStrategy
     * is notified. <p>
     * 
     * In order to discard an object from the cache, the object must either
     * not be pinned or must be pinned exactly once, presumably by the
     * caller. Note, however, that there is no way to verify that the caller
     * actually owns the pinned reference. <p>
     * 
     * This relaxation is needed in order to allow for situations where
     * an object needs to be discarded from the cache but remain in use. <p>
     * 
     * Also note that if an object is discarded while still pinned, the final
     * call to unpin is unnecessary, and is also illegal. <p>
     * 
     * @param key the key for the object to remove from the cache.
     * @param dropRef if true, drop a reference on the object before
     *            removing from the cache.
     * 
     * @return the object which was discarded from the cache.
     * 
     * @exception DiscardException if an Exception is thrown from the
     *                DiscardStrategy.
     * @exception IllegalOperationException if the object associated with
     *                the key is currently pinned more than once.
     **/
    @Override
    @SuppressWarnings("null")
    public Object removeAndDiscard(java.lang.Object key, boolean dropRef)
                    throws DiscardException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "removeAndDiscard",
                     new Object[] { key, new Boolean(dropRef) });

        Bucket bucket = getOrCreateBucketForKey(key); // d739870
        Object object = null;

        try {
            synchronized (bucket)
            {
                Element element = bucket.removeByKey(key, dropRef);
                object = element != null ? element.object : null;

                if (object != null) {
                    // Inform the DiscardStrategy that we just removed an oject
                    if (discardStrategy != null) {
                        try {
                            discardStrategy.discardObject(this, element.key,
                                                          element.object);
                        } catch (Exception ex) {
                            FFDCFilter.processException(ex, CLASS_NAME + ".removeAndDiscard",
                                                        "540", this);
                            Tr.warning(tc, "EXCEPTION_THROWN_BY_DISCARD_STRATEGY_CNTR0054W"
                                       , new Object[] { element, ex }); //p111002.4
                            element = null;
                            throw new DiscardException(ex, key);
                        }
                    }
                }
            }
        } finally {

            if (object != null) {
                synchronized (this) {
                    // ACK! This is going to be a choke point
                    numObjects--;
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "removeAndDiscard", object);

        return object;
    }

    /**
     * Insert the key/object pair into the cache. It is legal to insert
     * multiple objects with the same key, though the other cache operations
     * do not differentiate between objects with the same key. The object
     * is automatically pinned before the insert operation completes. <p>
     * 
     * @param key the key of the object to insert.
     * @param object the object to insert.
     * 
     *            <p>
     * @see Cache#pin
     * @see Cache#unpin
     * @see EvictionStrategy
     * @see Cache#setEvictionStrategy
     **/
    @Override
    public CacheElement insert(Object key, Object object)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "insert", new Object[] { key, object });

        try
        {
            Bucket bucket = getOrCreateBucketForKey(key); // d739870

            Element element;
            synchronized (bucket) {
                // Insert the new object and pin it
                element = bucket.insertByKey(key, object);
                element.pinned++;
            }

            synchronized (this) {
                //ACK! This is going to be a choke point
                numObjects++;
            }

            return element;

        } finally {

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(tc, "insert");
        }
    }

    /**
     * Insert the key/object pair into the cache. It is legal to insert
     * multiple objects with the same key, though the other cache operations
     * do not differentiate between objects with the same key. The object
     * is not pinned before the insert operation completes, but its last
     * access time will be updated to indicate it is active. <p>
     * 
     * @param key the key of the object to insert.
     * @param object the object to insert.
     * 
     *            <p>
     * @see Cache#pin
     * @see Cache#unpin
     */
    // F61004.6
    @Override
    public CacheElement insertUnpinned(Object key, Object object)
    {
        boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "insertUnpinned", new Object[] { key, object });

        try
        {
            Bucket bucket = getOrCreateBucketForKey(key); // d739870

            Element element;
            synchronized (bucket)
            {
                element = bucket.insertByKey(key, object);

                // Since we're not pinning the element, we must update its LRU data
                // to prevent it from timing out immediately.
                element.accessedSweep = numSweeps;
            }

            synchronized (this)
            {
                //ACK! This is going to be a choke point
                numObjects++;
            }

            return element;

        } finally
        {
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "insertUnpinned");
        }
    }

    /**
     * Pin the object associated with the specified key; pinning an object
     * in the cache ensures that the object will not be cast-out during any
     * cache trimming operations. Objects may be pinned more than once,
     * and calls to <code>pin()</code> should be symmetric with calls to
     * <code>unpin()</code>. <p>
     * 
     * @param key key of the object to pin.
     * 
     * @exception NoSuchObjectException if there is no object in the cache
     *                associated with the specified key.
     *                <p>
     * @see Cache#pinElement
     * @see Cache#unpin
     **/
    @Override
    public void pin(Object key)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "pin", key);

        Bucket bucket = getOrCreateBucketForKey(key); // d739870

        int pinCount;
        synchronized (bucket) {
            Element element = bucket.findByKey(key);

            if (element == null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    Tr.exit(tc, "pin - throw NoSuchObjectException"); // d173022.12
                throw new NoSuchObjectException(key);
            }

            element.pinned++;
            pinCount = element.pinned;
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "pin:" + pinCount);
    }

    /**
     * Pin the element; pinning an element in the cache ensures that it
     * will not be cast-out during any cache trimming operations. Objects
     * may be pinned more than once, and calls to <code>pin()</code> should
     * be symmetric with calls to <code>unpin()</code>. <p>
     * 
     * @param cacheElement the element to pin.
     * 
     * @see #unpinElement
     */
    // F61004.6
    @Override
    public void pinElement(CacheElement cacheElement)
    {
        Element element = (Element) cacheElement;

        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "pin", element.key);

        int pinCount;
        synchronized (element.ivBucket)
        {
            element.pinned++;
            pinCount = element.pinned;
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "pin:" + pinCount);
    }

    /**
     * Mark the element corresponding to the given key as being
     * ineligible for eviction from the cache. <p>
     * 
     * @param key key of the object to mark as ineligible.
     * 
     * @exception NoSuchObjectException if there is no object in the cache
     *                associated with the specified key.
     **/
    //d465813
    @Override
    public void markElementEvictionIneligible(CacheElement cacheElement)
    {
        Element element = (Element) cacheElement;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "markEvictionIneligible", element.key);

        synchronized (element.ivBucket) {
            element.ivEvictionIneligible = true;
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "markEvictionIneligible");
    }

    /**
     * Unpin the object associated with the specified key. Objects which are
     * not pinned may be cast-out of the cache during trimming operations.
     * Calls to <code>unpin()</code> must be symmetric with calls to
     * <code>pin()</code>. <p>
     * 
     * @param key key of the object to unpin.
     * @return the number of pins remaining.
     * 
     * @exception NoSuchObjectException if there is no object in the cache
     *                associated with the specified key.
     *                <p>
     * @see Cache#pin
     * @see Cache#unpinElement
     **/
    @Override
    public int unpin(Object key)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "unpin", key);

        int remaining = 0; // LI3408
        Bucket bucket = getOrCreateBucketForKey(key); // d739870

        synchronized (bucket) {
            Element element = bucket.findByKey(key);

            if (element == null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    Tr.exit(tc, "unpin - throw NoSuchObjectException"); // d173022.12
                throw new NoSuchObjectException(key);
            }

            if (element.pinned < 1) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    Tr.exit(tc, "unpin - throw IllegalOperationException"); // d173022.12
                throw new IllegalOperationException(key, element.pinned);
            }

            element.pinned--;
            remaining = element.pinned; // LI3408

            // Touch the LRU flag; since an object cannot be evicted when
            // pinned, this is the only time when we bother to set the
            // flag
            element.accessedSweep = numSweeps; // d115046
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "unpin:" + remaining);

        return remaining; // LI3408
    }

    /**
     * Unpin the element. Elements that are not pinned may be cast-out of
     * the cache during trimming operations. Calls to <code>unpin()</code>
     * must be symmetric with calls to <code>pin()</code>. <p>
     * 
     * @param cacheElement the element to pin
     * @return the number of pins remaining.
     * @see Cache#pinElement
     */
    // F61004.6
    @Override
    public int unpinElement(CacheElement cacheElement)
    {
        Element element = (Element) cacheElement;

        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "unpin", element.key);

        int remaining = 0;
        Bucket bucket = element.ivBucket;
        synchronized (bucket)
        {
            if (element.pinned < 1)
            {
                if (isTraceOn && tc.isEntryEnabled())
                    Tr.exit(tc, "unpin - throw IllegalOperationException");
                throw new IllegalOperationException(element.key, element.pinned);
            }

            element.pinned--;
            remaining = element.pinned;

            // Touch the LRU flag; since an object cannot be evicted when
            // pinned, this is the only time when we bother to set the
            // flag
            element.accessedSweep = numSweeps;
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "unpin:" + remaining);
        return remaining;
    }

    /**
     * Updates the LRU flags() for the specified element in the Cache. <p>
     * 
     * Provides a common method for updating LRU data, so the code is
     * not duplicated throughout the Cache implementation. <p>
     * 
     * Also provides a mechanism for an Element to update its own
     * LRU data without looking itself up in the Cache. <p>
     * 
     * @param element the element in the Cache being touched/updated.
     **/
    // d195605
    public void touch(Element element)
    {
        // Touch the LRU flag(s).
        element.accessedSweep = numSweeps;
    }

    /**
     * Enumerate all of the internal {@link Element} (object holder)
     * objects in the cache. This is primarily for access to element state
     * information, such as LRU bits. Note that nothing is pinned in the
     * cache by this operation, so the contents of the enumeration are
     * a snapshot of the cache at a single point in time. Beware.
     * 
     * This method is intended for use primarily by the standard {@link Cache} collaborators {@link FaultStrategy}, {@link EvictionStrategy} and {@link DiscardStrategy}. It should
     * not be used as a general
     * mechanism for data access by <code>Cache</code> clients. <p>
     * 
     * @return an <code>Enumeration</code> of the internal {@link Element} objects in the cache.
     *         <p>
     * @see Element
     * @see CacheElementEnumerator
     **/
    @Override
    public final Enumeration<Element> enumerateElements()
    {
        return new CacheElementEnumerator(this); // d103404.2
    }

    /////////////////////////////////////////////////////////////////////////////

    //
    // Implementation
    //

    /**
     * Return the index into {@link #buckets} for the specified key.
     */
    private int getBucketIndexForKey(Object key)
    {
        return (key.hashCode() & 0x7FFFFFFF) % buckets.length;
    }

    /**
     * Returns the bucket which the specified key hashes to. <p>
     * 
     * @param key key for the object to get a bucket for.
     * 
     * @return the bucket which the specified key hashes to.
     **/
    Bucket getBucketForKey(Object key)
    {
        int index = getBucketIndexForKey(key);

        Bucket bucket = buckets[index];
        if (bucket == null)
        {
            synchronized (bucketLock)
            {
                bucket = buckets[index];
            }
        }

        return bucket;
    }

    /**
     * Returns the bucket which the specified key hashes to, or creates it if
     * it does not exist.
     * 
     * @param key key for the object to get a bucket for.
     * 
     * @return the bucket which the specified key hashes to.
     **/
    private Bucket getOrCreateBucketForKey(Object key)
    {
        int index = getBucketIndexForKey(key);

        // Double-checked locking.  Safe since buckets do not initialize any state
        // until they are synchronized by the caller.                      d739870
        Bucket bucket = buckets[index];
        if (bucket == null)
        {
            synchronized (bucketLock)
            {
                bucket = buckets[index];
                if (bucket == null)
                {
                    // If this is to be a cache of EJB Wrappers, then create Wrapper
                    // specific buckets, that hold EJSWrapperCommon objects,
                    // otherwise create generic BucketImpl.                   d195605
                    bucket = wrappers ? new WrapperBucket(this) : new BucketImpl();
                    buckets[index] = bucket;
                }
            }
        }

        return bucket;
    }

    /**
     * Evict the object associated with the specified key; does nothing
     * if there is no object associated with the key currently in the cache
     * or if the object is currently pinned. <p>
     * 
     * Returns true if the victim was successfully removed from the cache;
     * false if the victim was found but could not be removed (might be pinned)
     * or if the victim could not be found in the cache. <p>
     * 
     * The {@link DiscardStrategy} is notified of the eviction, if a
     * <code>DiscardStrategy</code> has been provided. <p>
     * 
     * @param key key of the object to evict from the cache.
     * 
     * @return true if the object was removed from the cache; false otherwise.
     **/
    protected boolean evictObject(Object key)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "evictObject", key);

        Bucket bucket = getOrCreateBucketForKey(key);
        Element element = null;

        synchronized (this) {
            numEvictionAttempts++;
        }

        try
        {
            // Hold the bucket lock while calling the discard strategy.
            // The spread of keys amongst buckets should ameliorate the
            // ill-effects of holding this lock when calling a potentially
            // long running operation (eg passivation).
            synchronized (bucket)
            {
                element = bucket.findByKey(key);

                // Give another chance to the eviction strategy to decide
                // whether it should evict an object. This time with the
                // bucket lock held, so a more consistent decision can be
                // reached
                if ((element != null) &&
                    (evictionStrategy.canBeDiscarded(element)))
                {
                    element = bucket.discardByKey(key);

                    if (element != null)
                    {
                        synchronized (this) {
                            numObjects--;
                            numEvictions++;
                        }

                        // Inform the DiscardStrategy that we just
                        // evicted an oject
                        if (discardStrategy != null) {
                            try {
                                discardStrategy.discardObject(this, element.key,
                                                              element.object);
                            } catch (Exception ex) {
                                FFDCFilter.processException(ex, CLASS_NAME + ".evictObject",
                                                            "863", this);
                                Tr.warning(tc, "EXCEPTION_THROWN_BY_DISCARD_STRATEGY_CNTR0054W"
                                           , new Object[] { element, ex }); //p111002.4
                                element = null;
                            }
                        }
                    }
                } else {
                    element = null;
                }
            }

        } catch (IllegalOperationException ex) {
            // Object is pinned and cannot be removed from the cache
            FFDCFilter.processException(ex, CLASS_NAME + ".evictObject", "878", this);
            element = null;
        } finally {

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(tc, "evictObject", new Boolean(element != null));
        }

        return element != null;
    }

    /**
     * Dump the internal state of an object to the trace stream. <p>
     * 
     * For more information, see {@link Dumpable#dump}. <p>
     **/
    public void dump()
    {
        if (dumped) {
            return;
        }

        try {
            introspect(new TrDumpWriter(tc));
        } finally {
            dumped = true;
        }
    } // dump

    /**
     * Writes the significant state data of this class, in a readable format,
     * to the specified output writer. <p>
     * 
     * @param writer output resource for the introspection data
     */
    // F86406
    public void introspect(IntrospectionWriter writer)
    {
        writer.begin("Cache : " + this);
        writer.println("Name of Cache: " + this.ivName);
        writer.println("Number of buckets: " + this.numBuckets);
        synchronized (this)
        {
            writer.println("Number of objects currently in cache: " + numObjects);
            writer.println("Number of evictions attempted (since last dump): " + numEvictionAttempts);
            writer.println("Number of evictions (since last dump): " + numEvictions);
            numEvictionAttempts = 0;
            numEvictions = 0;
        }
        writer.end();
    }

    /**
     * Reset the dump state of this <code>Cache</code>. <p>
     * 
     * For more information, see {@link Dumpable#resetDump}. <p>
     **/
    public void resetDump()
    {
        dumped = false;
    } // resetDump

    /**
     * d601399
     * Terminate this cache. Cancels and unsets eviction stategy
     */
    @Override
    public void terminate()
    {
        if (evictionStrategy != null) {
            evictionStrategy.cancel();
            evictionStrategy = null;
        }
    }

    /////////////////////////////////////////////////////////////////////////////

    //
    // Data
    //

    // Strategies
    protected FaultStrategy faultStrategy = null;
    protected EvictionStrategy evictionStrategy = null;
    protected DiscardStrategy discardStrategy = null;
    protected LockTable ivEvictionLocks = null; // PK04804

    /** Name used to identify the Cache. **/
    // d129562
    protected String ivName;

    // Buckets

    /**
     * True if this cache is being used to hold wrappers.
     */
    private final boolean wrappers;

    /**
     * The array of buckets. Buckets are lazily created using double-checked
     * locking, synchronizing on {@link #bucketLock}.
     */
    private final Bucket[] buckets;

    /**
     * The lock to use for synchronizing creation of buckets.
     */
    private final Object bucketLock = new BucketLock();

    /**
     * Marker object for the bucket lock (for javacores, heapdumps, etc.)
     */
    private static class BucketLock
    {
        // Nothing.
    }

    /**
     * The number of buckets
     */
    protected final int numBuckets;

    /** Number of objects currently held in the cache. **/
    protected int numObjects = 0;

    // Eviction statistics (93859)
    private int numEvictionAttempts = 0;
    private int numEvictions = 0;

    /**
     * Number of sweeps made over the Cache for evicting objects.
     * Set in the Cache elements when last accessed, so the Eviction Strategy
     * may use it to determine the number of sweeps since last access.
     **/
    // d115046
    protected long numSweeps = 0;

    /** True iff this container instance has dumped its internal state. **/
    protected boolean dumped = false;

    private static final TraceComponent tc =
                    Tr.register(com.ibm.ejs.util.cache.Cache.class
                                , "EJBCache"
                                , "com.ibm.ejs.container.container"); //p111002.4

    private static final String CLASS_NAME = "com.ibm.ejs.util.cache.Cache";
}
