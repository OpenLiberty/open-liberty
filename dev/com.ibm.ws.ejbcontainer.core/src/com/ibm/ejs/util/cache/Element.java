/*******************************************************************************
 * Copyright (c) 2007, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.util.cache;

import com.ibm.websphere.csi.CacheElement;

/**
 * <code>Element</code> is a "holder" object used by <code>Cache</code> to
 * store internal data alongside each cached object.
 * 
 * <code>Element</code> is accessible outside of the cache package to
 * facilitate the development of <code>Cache</code> collaborators:
 * <code>FaultStrategy</code>, <code>EvictionStrategy</code> and
 * <code>DiscardStrategy</code>.
 * 
 * Note that it is not intended to be used for general purpose access to
 * cache data.
 * 
 */

public class Element implements CacheElement
{
    //
    // Construction
    //

    /**
     * Construct an <code>Element</code> object, holding the specified
     * key and object.
     * <p>
     * 
     * @param key The key associated with the object to be held
     * @param object The object to store in the cache
     * 
     */

    Element(Bucket bucket, Object key, Object object)
    {
        this.ivBucket = bucket;
        this.key = key;
        this.object = object;
    }

    /**
     * Construct an <code>Element</code> object, holding the specified
     * key and setting itself as the corresponding object. <p>
     * 
     * This constructor is intended for use by subclasses, which will
     * also implement the object to be cached.
     * Specifically, EJSWrapperCommon. <p>
     * 
     * @param key The key associated with this Element object
     **/
    // d195605
    public Element(Object key)
    {
        this.key = key;
        this.object = this;
    }

    @Override
    public String toString()
    {
        return "[" + key + " -> " + object + stateToString() + "]";
    }

    public String stateToString()
    {
        return " (pin=" + pinned + ", accessed=" + accessedSweep + ", eligible=" + ivEvictionIneligible + ")";
    }

    //87918.9
    //Implementation of CacheElement interface
    public final Object getObject()
    {
        return object;
    }

    public final Object getKey()
    {
        return key;
    }

    //
    // Data
    //

    /**
     * The bucket containing this element. Should be set while holding the
     * bucket lock, and should not be modified afterwards.
     */
    // F61004.6
    public Bucket ivBucket;

    /**
     * Key associated with the cached object
     */
    public final Object key;

    /**
     * Object held by the cache
     */
    public final Object object;

    /**
     * Reference count; if zero, object is not pinned and may be evicted
     */
    public int pinned = 0;

    /**
     * LRU data : the sweep count when last accessed
     **/
    // d115046
    public long accessedSweep = 0;

    /**
     * True iff this element is to be inelibible for eviction.
     * An example of when this flag may be set to true
     * is when a Stateful Session Bean has an
     * extended persistence context.
     * The default value will be "false".
     **/
    // d465813
    protected boolean ivEvictionIneligible = false;

}
