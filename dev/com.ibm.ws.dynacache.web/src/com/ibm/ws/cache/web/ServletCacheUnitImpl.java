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
package com.ibm.ws.cache.web;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.cache.BatchUpdateDaemon;
import com.ibm.ws.cache.CacheConfig;
import com.ibm.ws.cache.DCacheBase;
import com.ibm.ws.cache.ServerCache;
import com.ibm.ws.cache.command.CommandCache;
import com.ibm.ws.cache.intf.DCache;
import com.ibm.ws.cache.servlet.JSPCache;
import com.ibm.ws.cache.web.command.SerializedPutCommandStorage;

/**
 * This class provides the implementation of a ServletCacheUnit for the servlet cache. It is called by CacheUnit to create 
 * baseCache, command cache, JSP cache, JAXRPC cache instances. In addition, it is used to create command storage policy
 * and external cache services objects.  
 */
public class ServletCacheUnitImpl implements com.ibm.ws.cache.intf.ServletCacheUnit {
	
    private static TraceComponent tc = Tr.register(ServletCacheUnitImpl.class, "WebSphere Dynamic Cache", "com.ibm.ws.cache.resources.dynacache");
    private ExternalCacheServices     externalCacheServices = null;
    private Map<String, CommandCache> commandCacheInstances = null;
    private Map<String, JSPCache>     jspCacheInstances     = null;

    
    public ServletCacheUnitImpl() throws IllegalArgumentException {        
    	this.externalCacheServices = new ExternalCacheServices();
    	this.commandCacheInstances = new HashMap<String, CommandCache>();
    	this.jspCacheInstances     = new HashMap<String, JSPCache>();
    }

    public void purgeState(){
        if (tc.isDebugEnabled())
            Tr.debug(tc, "purgeState");
    	Collection<CommandCache> commandCaches = commandCacheInstances.values();
    	for (CommandCache cc : commandCaches) {	
    		ServerCache.getCacheInstances().remove(cc.getCache());
    		cc.getCache().stop();
		}
    	commandCacheInstances.clear();

    	Collection<JSPCache> jspCaches = jspCacheInstances.values();
    	for (JSPCache jspCache : jspCaches) {
    		ServerCache.getCacheInstances().remove(jspCache.getCache());
    		jspCache.getCache().stop();
		}
    	jspCacheInstances.clear();
    	externalCacheServices = new ExternalCacheServices();
    }
    
    /**
     * This method is used to create default base cache.
     * It is called by ServerCache whena  baseCache is first created. 
     */
    public void createBaseCache() {
    	getCommandCache(DCacheBase.DEFAULT_CACHE_NAME);
    	getJSPCache(DCacheBase.DEFAULT_CACHE_NAME);
    	this.externalCacheServices.start();
        BatchUpdateDaemon batchUpdateDaemon = ServerCache.cacheUnit.getBatchUpdateDaemon();
        if (batchUpdateDaemon != null) {
        	batchUpdateDaemon.setExternalCacheServices(this.externalCacheServices);
        }
    	CacheConfig cacheConfig = ServerCache.getCacheService().getCacheInstanceConfig(DCacheBase.DEFAULT_BASE_JNDI_NAME);
    	if (null != cacheConfig){
    		HashMap externalCacheGroups = initializeExternalCacheGroups(cacheConfig.getExternalGroups());
    		this.externalCacheServices.setExternalCacheGroups(externalCacheGroups);
    	}
    }

    /**
     * This implements the method in the ServletCacheUnit interface.
     * This allows to get or create command cache from the specified cache name. 
     *
     * @param cacheName cache name
     * @return The CommandCache.
     */
    public com.ibm.ws.cache.intf.CommandCache getCommandCache(String cacheName) {
        CommandCache cacheOut = (CommandCache)commandCacheInstances.get(cacheName);
		if (!commandCacheInstances.containsKey(cacheName)){
            CacheConfig config = (CacheConfig) ServerCache.getCacheService().getCacheInstanceConfig(cacheName);
            if ( config == null ) {
                // DYNA1004E=DYNA1004E: WebSphere Dynamic Cache instance named {0} can not be initialized because it is not configured.
                // DYNA1004E.explanation=This message indicates the named WebSphere Dynamic Cache instance can not be initialized.  The named instance is not avaliable.
                // DYNA1004E.useraction=Use the WebSphere Administrative Console to configure a cache instance resource named {0}.
                Tr.error(tc, "DYNA1004E", new Object[] {cacheName});
                cacheOut = null;
            } else {
                if ( !config.isEnableServletSupport()) {
                    // DYNA1005E=DYNA1005E: WebSphere Dynamic Cache instance named {0} can not be accessed because it is the wrong type.
                    // DYNA1005E.explanation=This message indicates the named WebSphere Dynamic Cache instance can not be used.  Dynamic caching is disabled for the named instance.
                    // DYNA1005E.useraction=Use the WebSphere Administrative Console to configure a cache instance resource named {0} using the correct type.
                    Tr.error(tc, "DYNA1004E", new Object[] {cacheName});
                    cacheOut = null;
                }
                else {
                    DCache cache = ServerCache.getCache(cacheName);
                    if (cache==null) {
                        cache = ServerCache.createCache(cacheName, config);
                    }
                    cacheOut = new com.ibm.ws.cache.command.CommandCache();
                    cacheOut.setCache(cache);
                    cacheOut.setBatchUpdateDaemon(ServerCache.cacheUnit.getBatchUpdateDaemon());
                    cacheOut.setRemoteServices( cache.getRemoteServices() );
                    cacheOut.setCommandStoragePolicy(new SerializedPutCommandStorage());
                    cacheOut.start();
                }
            }
            commandCacheInstances.put(cacheName, cacheOut);
            if (cacheName.equals(DCacheBase.DEFAULT_BASE_JNDI_NAME) || cacheName.equals(DCacheBase.DEFAULT_CACHE_NAME)) {
            	ServerCache.commandCache = cacheOut;
            }
        }
    	return cacheOut;
    }

    /**
     * This implements the method in the ServletCacheUnit interface.
     * This allows to get or create JSP cache from the specified cache name. 
     *
     * @param cacheName cache name
     * @return The JSPCache.
     */
    public com.ibm.ws.cache.intf.JSPCache getJSPCache(String cacheName) {
        JSPCache cacheOut = (JSPCache)jspCacheInstances.get(cacheName);
        if (!jspCacheInstances.containsKey(cacheName)){
            CacheConfig config = (CacheConfig) ServerCache.getCacheService().getCacheInstanceConfig(cacheName);
            if ( config == null ) {
                // DYNA1004E=DYNA1004E: WebSphere Dynamic Cache instance named {0} can not be initialized because it is not configured.
                // DYNA1004E.explanation=This message indicates the named WebSphere Dynamic Cache instance can not be initialized.  The named instance is not avaliable.
               // DYNA1004E.useraction=Use the WebSphere Administrative Console to configure a cache instance resource named {0}.
                Tr.error(tc, "DYNA1004E", new Object[] {cacheName});
                cacheOut = null;
            } else {
                if ( !config.isEnableServletSupport()) {
                    // DYNA1005E=DYNA1005E: WebSphere Dynamic Cache instance named {0} can not be accessed because it is the wrong type.
                    // DYNA1005E.explanation=This message indicates the named WebSphere Dynamic Cache instance can not be used.  Dynamic caching is disabled for the named instance.
                    // DYNA1005E.useraction=Use the WebSphere Administrative Console to configure a cache instance resource named {0} using the correct type.
                    Tr.error(tc, "DYNA1004E", new Object[] {cacheName});
                    cacheOut = null;
                }
                else {
                    DCache cache = ServerCache.getCache(cacheName);
                    if (cache == null) {
                        cache = ServerCache.createCache(cacheName, config);
                    }
                    cacheOut = new com.ibm.ws.cache.servlet.JSPCache();
                    cacheOut.setCache(cache);
                    cacheOut.setBatchUpdateDaemon(ServerCache.cacheUnit.getBatchUpdateDaemon());
                    cacheOut.setRemoteServices( cache.getRemoteServices() );
                    cacheOut.setDefaultPriority( config.getJspCachePriority());
                    cacheOut.setExternalCacheServices(this.externalCacheServices);
                    cacheOut.start();
                }
            }
            jspCacheInstances.put(cacheName, cacheOut);
            if (cacheName.equals(DCacheBase.DEFAULT_BASE_JNDI_NAME) || cacheName.equals(DCacheBase.DEFAULT_CACHE_NAME)) {
            	ServerCache.jspCache = cacheOut;
            }
        }
    	return cacheOut;
    }
    
    /**
     * This implements the method in the ServletCacheUnit interface.
     * This is delegated to the ExternalCacheServices.
     *
     * @param groupId The external cache group id.
     * @param address The IP address of the target external cache.
     * @param beanName The bean name (bean instance or class) of
     * the ExternalCacheAdaptor that can deal with the protocol of the
     * target external cache.
     */
    public void addExternalCacheAdapter(String groupId, String address, String beanName) {
        this.externalCacheServices.addExternalCacheAdapter(groupId, address, beanName);
    }

    /**
     * This implements the method in the ServletCacheUnit interface.
     * This is delegated to the ExternalCacheServices.
     *
     * @param groupId The external cache group id.
     * @param address The IP address of the target external cache.
     */
    public void removeExternalCacheAdapter(String groupId, String address) {
        this.externalCacheServices.removeExternalCacheAdapter(groupId, address);
    }

    /**
     * This implements the method in the ServletCacheUnit interface.
     * This is delegated to the ExternalCacheServices.
     *
     * @param invalidateIdEvents hashmap of invalidation id events
     * @param invalidateTemplateEvents hashmap of invalidation template events
     */
    public void invalidateExternalCaches(HashMap invalidateIdEvents, HashMap invalidateTemplateEvents) {
    	this.externalCacheServices.invalidateExternalCaches(invalidateIdEvents, invalidateTemplateEvents);
    }
    
    /**
     * This is delegated to the ExternalCacheServices which is used to initialize the external cache groups
     */
    private HashMap initializeExternalCacheGroups(List groupList) {
        //for all groups in document
        HashMap<String, ExternalCacheGroup> externalCacheGroups = new HashMap<String, ExternalCacheGroup>();
        if (groupList != null) {
            Iterator li = groupList.iterator();
            while (li.hasNext()) {
                CacheConfig.ExternalCacheGroup ecg = (CacheConfig.ExternalCacheGroup) li.next();
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "ecg.name=" + ecg.name + " ecg.type=" + ecg.type);
                ExternalCacheGroup externalCacheGroup = new ExternalCacheGroup(ecg.name, ecg.type);
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "adding External Cache Group id = " + ecg.name + ", externalCacheGroup = " + externalCacheGroup);
                externalCacheGroups.put(ecg.name, externalCacheGroup);
                //for all members in group
                Iterator li2 = ecg.members.iterator();
                while (li2.hasNext()) {
                    CacheConfig.ExternalCacheGroupMember ecgm = (CacheConfig.ExternalCacheGroupMember) li2.next();
                    externalCacheGroup.addExternalCacheAdapter(ecgm.address, ecgm.beanName);
                }
                // TODO
                //remove to speed up startup performance
                //Tr.info(tc, "dynacache.joingroup", ecg.name);
            }
        }
        return externalCacheGroups;
    }

	@Override
	public void purgeState(String cacheName) {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "purgeState() cacheName="+ cacheName);
    	commandCacheInstances.remove(cacheName);
    	jspCacheInstances.remove(cacheName);
	}
}
