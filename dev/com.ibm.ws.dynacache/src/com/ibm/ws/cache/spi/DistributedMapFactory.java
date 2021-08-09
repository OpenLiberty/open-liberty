/*******************************************************************************
 * Copyright (c) 1997, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cache.spi;

import java.util.Properties;

import com.ibm.websphere.cache.DistributedMap;
import com.ibm.wsspi.cache.DistributedObjectCacheFactory;

/**
 * This class provides internal WAS components with factory methods to
 * create/lookup instances of a DistributedMap. Each DistributedMap
 * instance can be configured independently.
 * 
 * @ibm-private-in-use
 */
public class DistributedMapFactory {

    private DistributedMapFactory() {}

    /**
     * Returns the DistributedMap instance specified by the given id. If
     * the given instance has not yet been created, then a new instance
     * is created using the default parameters.
     * 
     * @param name instance name of the DistributedMap
     * @return A DistributedMap instance
     * @see DistributedObjectCacheFactory
     * @deprecated Use DistributedObjectCacheFactory
     * @ibm-private-in-use
     */
    @Deprecated
    public static DistributedMap getMap(String name) {
        return DistributedObjectCacheFactory.getMap(name, new Properties());
    }

    /**
     * Returns the DistributedMap instance specified by the given id, using the
     * the parameters specified in properties. If the given instance has
     * not yet been created, then a new instance is created using the parameters
     * specified in the properties object.
     * <br>Properties:
     * <table role="presentation">
     * <tr><td>com.ibm.ws.cache.CacheConfig.CACHE_SIZE</td><td>integer the maximum number of cache entries</td></tr>
     * <tr><td>com.ibm.ws.cache.CacheConfig.ENABLE_DISK_OFFLOAD</td><td> boolean true to enable disk offload</td></tr>
     * <tr><td>com.ibm.ws.cache.CacheConfig.DISK_OFFLOAD_LOCATION</td><td>directory to contain offloaded cache entries</td></tr>
     * </table>
     * 
     * @param name instance name of the DistributedMap
     * @param properties
     * @return A DistributedMap instance
     * @see DistributedObjectCacheFactory
     * @deprecated Use DistributedObjectCacheFactory
     * @ibm-private-in-use
     */
    @Deprecated
    public static DistributedMap getMap(String name, Properties properties) {
        return DistributedObjectCacheFactory.getMap(name, properties);
    }
}
