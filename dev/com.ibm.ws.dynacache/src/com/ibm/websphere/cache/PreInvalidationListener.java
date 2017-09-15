/*******************************************************************************
 * Copyright (c) 1997, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.cache;

import com.ibm.websphere.cache.InvalidationEvent;

/**
 * Pre-invalidation listener interface used for selectively overriding invalidation events.
 * @ibm-api
 */
public interface PreInvalidationListener extends java.util.EventListener {
	
	/**
	 * Define cause of invalidation for EXPLICIT
     * @ibm-api 
     */
	public static final int EXPLICIT = InvalidationEvent.EXPLICIT;	
	
    /**
	 * Define cause of invalidation for Least Recently Used(LRU)
     * @ibm-api 
     */
    public final static int LRU = InvalidationEvent.LRU;            

    /**
     * Define cause of invalidation for TIMEOUT
     * @ibm-api 
     */
    public final static int TIMEOUT = InvalidationEvent.TIMEOUT;    

    /**
     * Define cause of invalidation for DISK_TIMEOUT
     * @ibm-api 
     */
    public final static int DISK_TIMEOUT = InvalidationEvent.DISK_TIMEOUT;

    /**
	 * Define cause of invalidation for CLEAR_ALL
     * @ibm-api 
     */
    public final static int CLEAR_ALL = InvalidationEvent.CLEAR_ALL;

    /**
	 * Define cause of invalidation for INACTIVE
     */
    public final static int INACTIVE = InvalidationEvent.INACTIVE;

    /**
     * Define cause of invalidation for DISK_GARBAGE_COLLECTOR
     * @ibm-api 
     */
    public final static int DISK_GARBAGE_COLLECTOR = InvalidationEvent.DISK_GARBAGE_COLLECTOR; 

    /**
     * Define cause of invalidation for DISK_OVERFLOW
     */
    public final static int DISK_OVERFLOW = InvalidationEvent.DISK_OVERFLOW;

    /**
	 * Define source of invalidation for LOCAL (cache in memory or disk)
     * @ibm-api 
     */
    public final static int LOCAL = InvalidationEvent.LOCAL;

    /**
	 * Define source of invalidation for REMOTE
     * @ibm-api 
     */
    public final static int REMOTE = InvalidationEvent.REMOTE;
	
	/**
	 * Invoked prior to an invalidation event. Returned boolean will determine whether invalidation
	 * will be processed or not.
	 * 
	 * @param id The cache id
	 * @param sourceOfInvalidation The source of the invalidation, defined in com.ibm.websphere.cache.InvalidationEvent
	 * @param causeOfInvalidation The cause of the invalidation, defined in com.ibm.websphere.cache.InvalidationEvent
	 * @return boolean "true" means that the invalidation event should proceed as normal.
	 *                 "false" means that the invalidation event should be canceled.
	 * @ibm-api
	 */
	public boolean shouldInvalidate (Object id, int sourceOfInvalidation, int causeOfInvalidation);
}
