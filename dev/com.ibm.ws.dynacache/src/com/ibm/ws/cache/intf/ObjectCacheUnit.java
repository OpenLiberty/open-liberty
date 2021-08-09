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
package com.ibm.ws.cache.intf;

import com.ibm.wsspi.cache.EventSource;

/**
 * This interface is used by CacheUnitImpl so that it can access some methods defined in ObjectCacheUnitImpl. 
 */
public interface ObjectCacheUnit  {

    /**
     * This is called to create object cache.
     *
     * @param reference The cache name
     */
	public Object createObjectCache(String reference);
    
    /**
     * This is called to create event source object.
     *
     * @param createAsyncEventSource boolean true - using async thread context for callback; false - using caller thread for callback
     * @param cacheName The cache name
     * @param cacheNameNonPrefix The non-prefix cache name
     * @return EventSourceIntf The event source
     */
	public EventSource createEventSource(boolean createAsyncEventSource, String cacheName);
}      
