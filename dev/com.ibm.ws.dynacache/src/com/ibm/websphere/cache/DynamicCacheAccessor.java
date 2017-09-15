/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.cache;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.cache.DCacheBase;
import com.ibm.ws.cache.ServerCache;

/**
 * This class provides applications with access to the Dynamic Cache,
 * allowing programmatic inspection and manipulation of WebSphere's
 * cache.
 * @ibm-api 
 */
public final class DynamicCacheAccessor {

    private static TraceComponent tc = Tr.register(DynamicCacheAccessor.class, "WebSphere Dynamic Cache", "com.ibm.ws.cache.resources.dynacache");

    /**
     * This obtains a reference to the dynamic cache.
     * 
     * @ibm-api 
     * @return Reference to the cache or null if caching is disabled
     * @see #getDistributedMap()
     * @see DistributedMap
     * @deprecated Use DistributedMap to store and manage objects
     *             in cache. DynamicCacheAccessor#getDistributedMap
     *             will return a DistributedMap for accessing
     *             base cache.
     * @ibm-api 
     */
    public static com.ibm.websphere.cache.Cache getCache() {
        if (isServletCachingEnabled())
            return (com.ibm.websphere.cache.Cache)ServerCache.cache;
        else
            return null;
    }
    
    /**
    * This determines if Dynamic caching (either servlet or object cache) is enabled.
    * @return true if caching is enabled, false if it is disabled.
    * @ibm-api 
    */
    public static boolean isCachingEnabled() {
        return (ServerCache.servletCacheEnabled || ServerCache.objectCacheEnabled); 
    }

    /**
     * This determines if Dynamic servlet caching is enabled.
     * @return true if caching is enabled, false if it is disabled.
     * @ibm-api 
     */
     public static boolean isServletCachingEnabled() {
         return ServerCache.servletCacheEnabled;
     }

     /**
      * This determines if Dynamic object caching is enabled.
      * @return true if caching is enabled, false if it is disabled.
      * @ibm-api 
      */
      public static boolean isObjectCachingEnabled() {
          return ServerCache.objectCacheEnabled;
      }

    /**
     * This method will return a DistributedMap reference to the dynamic cache.
     * 
     * @return Reference to the DistributedMap or null
     *         if caching is disabled.
     * @since v6.0
     * @ibm-api 
     * @deprecated baseCache is used for servlet caching. It should not be used
     *             as a DistributedMap. 
     */
    public static DistributedMap getDistributedMap() {
        final String methodName="getDistributedMap()";
        if (tc.isEntryEnabled()) {
            Tr.entry(tc, methodName);
        }
        DistributedMap distributedMap = null;
        Context context = null;
        if (isObjectCachingEnabled()) {
            try {
                context = new InitialContext();
                distributedMap  = (DistributedObjectCache)context.lookup(DCacheBase.DEFAULT_BASE_JNDI_NAME);
            } catch ( NamingException e ) {
                com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.websphere.cache.DynamicCacheAccessor.getDistributedMap", "99", com.ibm.websphere.cache.DynamicCacheAccessor.class);
                // No need to do anything else, since FFDC prints stack traces
            }
            finally{
                try {
                    if ( context!=null) {
                        context.close();                        
                    }
                } catch ( NamingException e ) {
                    com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.websphere.cache.DynamicCacheAccessor.getDistributedMap", "110", com.ibm.websphere.cache.DynamicCacheAccessor.class);
                }
            }
        } else {
            // DYNA1060E=DYNA1060E: WebSphere Dynamic Cache instance named {0} cannot be used because of Dynamic Object cache service has not be started.
            Tr.error(tc, "DYNA1060W", new Object[] {DCacheBase.DEFAULT_BASE_JNDI_NAME});
        }
        if (tc.isEntryEnabled()) {
            Tr.exit(tc, methodName, distributedMap);
        }
        return distributedMap;
    }

}
