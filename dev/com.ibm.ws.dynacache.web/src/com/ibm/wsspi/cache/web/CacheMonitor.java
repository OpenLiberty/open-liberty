/*******************************************************************************
 * Copyright (c) 1997, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.cache.web;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.ibm.websphere.cache.DynamicCacheAccessor;
import com.ibm.ws.cache.DCacheBase;
import com.ibm.ws.cache.ServerCache;
import com.ibm.ws.cache.config.CacheInstance;
import com.ibm.ws.cache.intf.DCache;
import com.ibm.ws.cache.web.config.ConfigManager;
import com.ibm.wsspi.cache.Cache;
import com.ibm.wsspi.cache.ConfigEntry;

/**
 * This is the mechanism to provide CacheMonitor
 * access to the current cache instances and
 * configured cache policies.
 * @ibm-spi 
 */
public class CacheMonitor {
   
	public static final int CACHE_TYPE_JAXRPC = com.ibm.websphere.cache.CacheEntry.CACHE_TYPE_JAXRPC;
	public static final int NOT_SHARED = com.ibm.ws.cache.EntryInfo.NOT_SHARED;
	public static final int SHARED_PULL = com.ibm.ws.cache.EntryInfo.SHARED_PULL;
	public static final int SHARED_PUSH = com.ibm.ws.cache.EntryInfo.SHARED_PUSH;
	public static final int SHARED_PUSH_PULL = com.ibm.ws.cache.EntryInfo.SHARED_PUSH_PULL;
	
	public static final int HIGH = com.ibm.ws.cache.CacheConfig.HIGH;
	public static final int BALANCED = com.ibm.ws.cache.CacheConfig.BALANCED;
	public static final int LOW = com.ibm.ws.cache.CacheConfig.LOW;
	public static final int CUSTOM = com.ibm.ws.cache.CacheConfig.CUSTOM;
	
	public static final int EVICTION_RANDOM = com.ibm.ws.cache.CacheConfig.EVICTION_RANDOM;
	public static final int EVICTION_SIZE_BASED = com.ibm.ws.cache.CacheConfig.EVICTION_SIZE_BASED;
	public static final int EVICTION_NONE = com.ibm.ws.cache.CacheConfig.EVICTION_NONE;

	public static final String DISKCACHE_MORE= com.ibm.ws.cache.HTODDynacache.DISKCACHE_MORE;
	
    /**
     * This method determines if Dynamic caching (either servlet or object cache) is enabled.
     * 
     * @return true if caching is enabled, false if it is disabled.
     */
	public static boolean isCachingEnabled(){
		return DynamicCacheAccessor.isCachingEnabled();
	}
	
    /**
     * This method determines if Dynamic servlet caching is enabled.
     * 
     * @return true if caching is enabled, false if it is disabled.
     */
	public static boolean isServletCachingEnabled(){
		return ServerCache.servletCacheEnabled;
	}
	
    /**
     * This method determines if Dynamic object caching is enabled.
     * 
     * @return true if caching is enabled, false if it is disabled.
     */
	public static boolean isObjectCachingEnabled(){
		return ServerCache.objectCacheEnabled;
	}
	
    /**
     * This method returns the cache instance specified by instance name.
     * 
     * @return cache instance or NULL if instance name does not exist.
     */
	public static Cache getCache(String instanceName){
		DCache c = ServerCache.getCache(instanceName);
		if (c != null)
			return new Cache(c);
		else
			return null;
	}
	
    /**
     * This method returns a list of the configured servlet cache instance names.
     * 
     * @return a list of instance names.
     */
	public static final ArrayList getConfiguredServletCacheInstanceNames(){
    	ArrayList instanceNames = ServerCache.getServletCacheInstanceNames();
    	// Rename the jndi name for baseCache to internal baseCache name
    	// services/cache/basecache ==> baseCache
    	if (instanceNames.contains(DCacheBase.DEFAULT_BASE_JNDI_NAME)) {
    		instanceNames.remove(DCacheBase.DEFAULT_BASE_JNDI_NAME);
    		instanceNames.add(DCacheBase.DEFAULT_CACHE_NAME);
    	}
    	return instanceNames;
    }
	
    /**
     * This method returns a list of active cache instances including both servlet cache and object cache.
     * 
     * @return a list of instance names.
     */
	public static final ArrayList getCacheInstanceNames(){
    	Map map = ServerCache.getCacheInstances();
    	ArrayList<String> names = new ArrayList<String>();
        Iterator it = map.entrySet().iterator();
    	while (it.hasNext()) {
    		Map.Entry entry = (Map.Entry) it.next();
    		String instanceName = (String) entry.getKey();
    		names.add(instanceName);
        }
    	return names;
    }
	
    /**
     * This method returns a list of cache instances which are defined in cachespec.xml files.
     * 
     * @return a list of instance names.
     */
    public static final ArrayList getPolicyServletCacheInstanceNames(){
    	ArrayList<String> names = new ArrayList<String>();
    	Iterator it = ConfigManager.getInstance().getCacheInstances().iterator();
    	while (it.hasNext()){
    		CacheInstance ci = (CacheInstance)it.next();
    		names.add(ci.name);
    	}
    	return names;
    }
    
    /**
     * This method returns a list of config entries specified by the cache instance.
     * 
     * @return a list of config entries.
     */
    public static final ArrayList getConfigEntries(String instanceName){
    	ArrayList<ConfigEntry> ces = new ArrayList<ConfigEntry>();
    	List configEntries = ConfigManager.getInstance().getCacheEntries(instanceName);
    	for (int i = 0; i<configEntries.size(); i++){
    		ConfigEntry ce = new ConfigEntry((com.ibm.ws.cache.config.ConfigEntry)configEntries.get(i));
    		ces.add(ce);
    	}
    	return ces;
    }
    
    /**
     * This method returns a list of all config entries found in cachespec.xml files.
     * 
     * @return a list of config entries.
     */
    public static final ArrayList getConfigEntries(){
    	ArrayList<ConfigEntry> ces = new ArrayList<ConfigEntry>();
    	List configEntries = ConfigManager.getInstance().getEntries();
    	for (int i = 0; i<configEntries.size(); i++){
    		ConfigEntry ce = new ConfigEntry((com.ibm.ws.cache.config.ConfigEntry)configEntries.get(i));
    		ces.add(ce);
    	}
    	return ces;
    }

}