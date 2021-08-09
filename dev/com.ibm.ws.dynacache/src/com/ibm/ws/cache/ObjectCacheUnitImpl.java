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
package com.ibm.ws.cache;

import com.ibm.websphere.cache.DistributedObjectCache;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.cache.intf.DCache;
import com.ibm.wsspi.cache.EventSource;

/**
 * This class provides the implementation of a ObjectCacheUnit for the object cache. It is called by CacheUnitImpl to
 * create DistributedMap or DistributedNioMap instances. In addition, it is used to create event source for invalidation
 * or change listeners.
 */
public class ObjectCacheUnitImpl implements com.ibm.ws.cache.intf.ObjectCacheUnit {

    private static TraceComponent tc = Tr.register(ObjectCacheUnitImpl.class, "WebSphere Dynamic Cache", "com.ibm.ws.cache.resources.dynacache");

    /**
     * Create a DistributedObjectCache from a string reference. The config for the reference must already exist.
     * 
     * @param reference
     * @return The returned object can be type cast to DistributedObjectCache and will be a DistributedMap,
     *         DistributedLockingMap or DistributedNioMap.
     */
    public Object createObjectCache(String reference) {
        final String methodName = "createCacheInstance()";

        if (tc.isEntryEnabled())
            Tr.entry(tc, methodName + " cacheName=" + reference);

        CacheConfig config = ServerCache.getCacheService().getCacheInstanceConfig(reference);

        if (config == null) {
            // DYNA1004E=DYNA1004E: WebSphere Dynamic Cache instance named {0} can not be initialized because it is not
            // configured.
            // DYNA1004E.explanation=This message indicates the named WebSphere Dynamic Cache instance can not be
            // initialized. The named instance is not avaliable.
            // DYNA1004E.useraction=Use the WebSphere Administrative Console to configure a cache instance resource
            // named {0}.
            Tr.error(tc, "DYNA1004E", new Object[] { reference });
        }

        /*
         * Sync on the WCCMConfig because multiple cache readers might have entered the createCacheInstance at the same
         * time. With this sync one reader has to wait till the other has finished creating the cache instance
         */
        DistributedObjectCache dCache = null;
        synchronized (config) { // ADDED FOR 378973
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Entered synchronized (config) for " + config.getCacheName());
            }

            dCache = config.getDistributedObjectCache();
            if (dCache == null) {
                dCache = createDistributedObjectCache(config);
                config.setDistributedObjectCache(dCache);
            }
        }
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, methodName + " cacheName in=" + reference + " out=" + reference);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, methodName + "  distributedObjectCache=" + dCache);
        return dCache;
    }

    // --------------------------------------------------------------

    /**
     * Create a DistributedObjectCache from a cacheConfig object.
     * 
     * Entry ( only call this method once per cache name ) config != null config.distributedObjectCache == null Exit
     * newly created DistributedObjectCache
     * 
     * @param config
     * @return The returned object will be a DistributedMap, DistributedLockingMap or DistributedNioMap.
     */
    private DistributedObjectCache createDistributedObjectCache(CacheConfig config) {
        final String methodName = "createDistributedObjectCache()";
        if (tc.isEntryEnabled())
            Tr.entry(tc, methodName + " cacheName=" + (config != null ? config.getCacheName() : "null"));

        DCache dCache = ServerCache.createCache(config.getCacheName(), config);
        config.setCache(dCache);
        DistributedObjectCache distributedObjectCache = null;

        if (config.isEnableNioSupport()) {
            distributedObjectCache = new DistributedNioMapImpl(dCache);
        } else {
            distributedObjectCache = new DistributedMapImpl(dCache);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, methodName + " distributedObjectCache=" + distributedObjectCache);
        return distributedObjectCache;
    }

    // --------------------------------------------------------------

    /**
     * This implements the method in the ServletCacheUnit interface. This method is used to initialize event source for
     * invalidation listener
     * 
     * @param createAsyncEventSource
     *            boolean true - using async thread context for callback; false - using caller thread for callback
     * @param cacheName
     *            The cache name
     * @return EventSourceIntf The event source
     */
    public EventSource createEventSource(boolean createAsyncEventSource, String cacheName) {

        EventSource eventSource = new DCEventSource(cacheName, createAsyncEventSource);
        if (tc.isDebugEnabled())
            Tr.debug(tc, "Using caller thread context for callback - cacheName= " + cacheName);

        return eventSource;
    }

}
