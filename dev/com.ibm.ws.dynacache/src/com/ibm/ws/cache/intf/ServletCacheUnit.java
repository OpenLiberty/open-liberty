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

import java.util.HashMap;

/**
 * This interface is used by CacheUnitImpl so that it can access some methods defined in ServeltCacheUnitImpl. 
 */
public interface ServletCacheUnit {

    /**
     * This is called to get Command Cache object.
     *
     * @param cacheName The cache name
     * @return CommandCache object
     */
    public CommandCache getCommandCache(String cacheName);
    
    /**
     * This is called to get JSP Cache object.
     *
     * @param cacheName The cache name
     * @return JSPCache object
     */
    public JSPCache getJSPCache(String cacheName);
    
    /**
     * This is delegated to the ExternalCacheServices.  
     *
     * @param groupId The external cache group id.
     * @param address The IP address of the target external cache.
     * @param beanName The bean name (bean instance or class) of
     * the ExternalCacheAdaptor that can deal with the protocol of the
     * target external cache.
     */
    public void addExternalCacheAdapter(String groupId, String address, String beanName);
    
    /**
     * This is delegated to the ExternalCacheServices.
     *
     * @param groupId The external cache group id.
     * @param address The IP address of the target external cache.
     */
    public void removeExternalCacheAdapter(String groupId, String address);
    
    /**
     * It applies the updates to the external caches.
     * It validates timestamps to prevent race conditions.
     *
     * @param invalidateIdEvents A HashMap of invalidate by id.
     * @param invalidateTemplateEvents A HashMap of invalidate by template.
     */
    public void invalidateExternalCaches(HashMap invalidateIdEvents, HashMap invalidateTemplateEvents);
    
    /**
     * Drop all state for that cache and reinitialize the ServletCacheUnit
     */
    public void purgeState(String cacheName);

    /**
     * Purge state for ALL servlet caches
     */
    public void purgeState();
    
    /**
     * Additional initialization when the default cache instance is created
     */
    public void createBaseCache();
    
}      
   
  
