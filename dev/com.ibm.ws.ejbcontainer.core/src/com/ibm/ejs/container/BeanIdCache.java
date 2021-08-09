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
package com.ibm.ejs.container;

import java.io.Serializable; // d156807.3
import com.ibm.ejs.util.MathUtil;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ejbcontainer.diagnostics.IntrospectionWriter;

/**
 * Cache of BeanIds that have been serialized. Used to reduce the cost
 * of serializing BeanIds when creating new BeanIds. <p>
 * 
 * The BeanId servers as both the key and the object within the cache. <p>
 * 
 * The cache is based on a hash table with no locking to increase concurrency.
 * And, there is only one BeanId per hash table bucket. The number of buckets
 * in the hash table can be configured at the time a <code>BeanIdCache</code>
 * object is constructed. The number of buckets does not change once the
 * cache has been created. <p>
 * 
 * Anytime a BeanId is created that will be used as a key for a lookup in the
 * EJSWrapper Cache, the BeanIdCache should be checked first. This will avoid
 * the serialization costs of the BeanId. <p>
 * 
 * Note that serialization of Stateless Session bean ids does not need to be
 * avoided, as it is inexpensive, and since they are singleton BeanIds, the
 * BeanIds for Stateless Session beans are already cached in the Home. <p>
 * 
 * @see WrapperManager
 **/

final class BeanIdCache
{
    /**
     * Construct a <code>BeanIdCache</code> object with the specified number of
     * buckets in it's hash table. <p>
     * 
     * @param numBuckets the number of hash table buckets to use.
     **/
    public BeanIdCache(int numBuckets)
    {
        // For better hashing, bump the number of buckets to a prime.
        numBuckets = MathUtil.findNextPrime(numBuckets);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "<init> (" + numBuckets + ")");

        ivBuckets = new BeanId[numBuckets];
    }

    /**
     * Finds a BeanId that matches the specified BeanId in the cache. If found,
     * the BeanId from the cache is returned; otherwise, the BeanId specified
     * as a parameter is returned. <p>
     * 
     * @param beanId BeanId to locate in the cache.
     * 
     * @return the BeanId from the cache, or the BeanId specified as a parameter
     *         if the specified BeanId is not in the cache.
     **/
    public BeanId find(BeanId beanId)
    {
        BeanId[] buckets = this.ivBuckets;
        BeanId element = buckets[(beanId.hashValue & 0x7FFFFFFF) % buckets.length];

        if (element == null ||
            !element.equals(beanId))
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            {
                ++ivCacheFinds;
                Tr.debug(tc, "BeanId not found in BeanId Cache : " +
                             ivCacheHits + " / " + ivCacheFinds);
            }
            element = beanId;
        }
        else
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            {
                ++ivCacheFinds;
                ++ivCacheHits;
                Tr.debug(tc, "BeanId found in BeanId Cache : " +
                             ivCacheHits + " / " + ivCacheFinds);
            }
        }

        return element;
    }

    // d156807.3 Begins
    public BeanId find(EJSHome home, Serializable pkey, boolean isHome)
    {
        int hashValue = BeanId.computeHashValue(home.j2eeName, pkey, isHome);

        BeanId[] buckets = this.ivBuckets;
        BeanId element = buckets[(hashValue & 0x7FFFFFFF) % buckets.length];

        if (element == null || !element.equals(home, pkey, isHome))
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            {
                ++ivCacheFinds;
                Tr.debug(tc, "BeanId not found in BeanId Cache : " +
                             ivCacheHits + " / " + ivCacheFinds);
            }
            element = new BeanId(home, pkey, isHome);
        }
        else
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            {
                ++ivCacheFinds;
                ++ivCacheHits;
                Tr.debug(tc, "BeanId found in BeanId Cache : " +
                             ivCacheHits + " / " + ivCacheFinds);
            }
        }

        return element;
    }

    // d156807.3 Ends

    /**
     * Adds the specified BeanId into the cache. Since the cache only contains
     * one BeanId per hash table bucket, any previously added BeanId that has
     * the same hash code will be dropped from the cache. <p>
     * 
     * @param beanId the BeanId to add into the cache.
     **/
    public void add(BeanId beanId)
    {
        BeanId[] buckets = this.ivBuckets;
        buckets[(beanId.hashValue & 0x7FFFFFFF) % buckets.length] = beanId;
    }

    /**
     * Removes all of the BeanIds from the cache for the specified home.
     * 
     * @param home Home to remove all associated beanIds.
     */
    // d152323
    public void removeAll(EJSHome home)
    {
        BeanId[] buckets = this.ivBuckets;
        for (int i = 0; i < buckets.length; ++i)
        {
            BeanId element = buckets[i];
            if (element != null && element.home == home)
            {
                buckets[i] = null;
            }
        }
    }

    /**
     * Change the cache size the BeanIdCache used. Once changed, all previously
     * cached BeanId's will be dropped and over time the cache will get populated
     * once again.
     * 
     * @param cacheSize size to set the cache to
     */
    public void setSize(int cacheSize)
    {
        if (ivBuckets.length != cacheSize)
        {
            ivBuckets = new BeanId[cacheSize];
        }
    }

    /**
     * Writes the significant state data of this class, in a readable format,
     * to the specified output writer. <p>
     * 
     * @param writer output resource for the introspection data
     */
    // F86406
    public void introspect(IntrospectionWriter writer)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        {
            writer.println("BeanIdCache : size = " + ivBuckets.length +
                           ", hits/find = " + ivCacheHits + "/" + ivCacheFinds);
        }
        else
        {
            writer.println("BeanIdCache : size = " + ivBuckets.length);

        }
    }

    /** Buckets - array of BeanIds in the cache. **/
    private volatile BeanId[] ivBuckets;

    /** Number of times a find operation has been performed. **/
    private int ivCacheFinds = 0; // d215317

    /** Number of times a match was found in the cache. **/
    private int ivCacheHits = 0; // d215317

    private static final TraceComponent tc =
                    Tr.register(com.ibm.ejs.container.BeanIdCache.class,
                                "EJBContainer",
                                "com.ibm.ejs.container.container");
}
