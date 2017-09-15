/*******************************************************************************
 * Copyright (c) 1997, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.cache;

import com.ibm.ws.cache.stat.CachePerf;
/**
 * An event object that provides information about the source of cache-related event.
 * InvalidationEvent objects are generated when cache entry is removed from the cache
 * based on cache id, dependency id or template. The InvalidationEvent object contains
 * six kinds of information:
 * <ul>
 * <li><i>id</i> - the id that was invalidated
 * <li><i>value</i> - the value that was invalidated
 * <li><i>causeOfInvaliation</i> - the cause of invalidation that generated this event (defined as EXPLICIT, LRU, TIMEOUT or CLEAR_ALL)
 * <li><i>sourceOfInvalidation</i> - the source of invalidation that generated this event (defined as LOCAL or REMOTE)
 * <li><i>cacheName</i> - the name of the cache being used to invalidate.
 * <li><i>timestamp</i> - the timestamp of when this event was generated
 * </ul>
 * @ibm-api 
 */
public class InvalidationEvent extends java.util.EventObject
{
    private static final long serialVersionUID = -9012240660005037807L;
    /**
	 * Define cause of invalidation for EXPLICIT
     * @ibm-api 
     */
    public final static int EXPLICIT = CachePerf.DIRECT;    // 1 // CPF-Inactivity

    /**
	 * Define cause of invalidation for Least Recently Used(LRU)
     * @ibm-api 
     */
    public final static int LRU = CachePerf.LRU;            // 2 // CPF-Inactivity

    /**
     * Define cause of invalidation for TIMEOUT
     * @ibm-api 
     */
    public final static int TIMEOUT = CachePerf.TIMEOUT;    // 3 // CPF-Inactivity

    /**
     * Define cause of invalidation for DISK_TIMEOUT
     * @ibm-api 
     */
    public final static int DISK_TIMEOUT = 4;               // see CachePerf // CPF-Inactivity

    /**
	 * Define cause of invalidation for CLEAR_ALL
     * @ibm-api 
     */
    public final static int CLEAR_ALL = 5;                  // see CachePerf // CPF-Inactivity

    /**
	 * Define cause of invalidation for INACTIVE
     */
    public final static int INACTIVE = CachePerf.INACTIVE;  // 6 // CPF-Inactivity

    /**
     * Define cause of invalidation for DISK_GARBAGE_COLLECTOR
     * @ibm-api 
     */
    public final static int DISK_GARBAGE_COLLECTOR = CachePerf.DISK_GARBAGE_COLLECTOR;  // 7 // 321649 

    /**
     * Define cause of invalidation for DISK_OVERFLOW
     */
    public final static int DISK_OVERFLOW = CachePerf.DISK_OVERFLOW;  // 8

    private int  m_causeOfInvalidation;

    /**
	 * Define source of invalidation for LOCAL (cache in memory or disk)
     * @ibm-api 
     */
    public final static int LOCAL = CachePerf.MEMORY;       // 1 // CPF-Inactivity

    /**
	 * Define source of invalidation for REMOTE
     * @ibm-api 
     */
    public final static int REMOTE = CachePerf.REMOTE;      // 2 // CPF-Inactivity

    private int  m_sourceOfInvalidation;
    private Object m_value;
    private long m_timeStamp = 0;
    public String m_cacheName;  // this cacheName will be set before firing event

    /**
     * Create a new InvalidationEvent from id, cause of invalidation and source of invalidation
     * @ibm-api 
     */
    public InvalidationEvent(Object id, Object value, int causeOfInvalidation, int sourceOfInvalidation, String cacheName) {
        super(id);
        m_value = value;
        m_causeOfInvalidation = causeOfInvalidation;
        m_sourceOfInvalidation = sourceOfInvalidation;
        m_timeStamp = System.currentTimeMillis();
        m_cacheName = cacheName;
    }

    /**
     * Gets the cache id that was invalidated. Asterisk is defined for all cache Ids.
     *
     * @return the cache id that was invalidated.
     * @ibm-api 
     */
    public Object getId() {
        return getSource();
    }

    /**
     * Gets the cache value that was invalidated. If cache id is asterisk, the value will be returned as NULL.
     * The value might be serialized in a byte array format. In this case, you must deserialize the
     * returned value.
     *
     * @return the cache value that was invalidated.
     * @ibm-api 
     */
    public Object getValue() {
        return m_value;
    }

    /**
     * Gets the cause of invalidation when this event was generated.
     * Use defined constants: EXPLICIT, LRU, TIMEOUT, DISK_TIMEOUT and CLEAR_ALL
     *
     * @return the cause of invalidation
     * @ibm-api 
     */
    public int getCauseOfInvalidation() {
        return m_causeOfInvalidation;
    }

    /**
     * Gets the source of invalidation when this event was generated.
     * Use defined constants: LOCAL and REMOTE
     *
     * @return the cause of invalidation
     * @ibm-api 
     */
    public int getSourceOfInvalidation() {
        return m_sourceOfInvalidation;
    }

    /**
     * Gets the name of the cache being used to invalidate
     *
     * @return the name of cache
     * @ibm-api 
     */
    public String getCacheName() {
        return m_cacheName;
    }

    /**
     * Gets the timestamp of when this event was generated.
     *
     * @return the timestamp
     * @ibm-api 
     */
    public long getTimeStamp() {
        return m_timeStamp;
    }
}
