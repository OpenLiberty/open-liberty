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

import java.util.ArrayList;
import java.util.HashMap;

import com.ibm.websphere.cache.exception.DynamicCacheServiceNotStarted;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.cache.intf.CommandCache;
import com.ibm.ws.cache.intf.DCache;
import com.ibm.ws.cache.intf.ExternalInvalidation;
import com.ibm.ws.cache.intf.JSPCache;
import com.ibm.ws.cache.intf.ObjectCacheUnit;
import com.ibm.ws.cache.intf.ServletCacheUnit;
import com.ibm.ws.cache.stat.CachePerf;
import com.ibm.wsspi.cache.EventSource;

/**
 * This class provides the default implementation of a CacheUnit. It
 * parses the configuration file, registers as a remote object to
 * cooperate with other caches in the group,
 * instanciates and initializes the various
 * services provided by the cache unit (batch update daemon, the caches
 * themselves, invalidation daemon, remote services, time limit daemon,
 * etc.).
 */
public class CacheUnitImpl implements CacheUnit {
    private static TraceComponent tc = Tr.register(CacheUnitImpl.class, "WebSphere Dynamic Cache", "com.ibm.ws.cache.resources.dynacache");
    
    protected String uniqueServerNameFQ = null;
    protected CacheConfig cacheConfig = null;
    protected BatchUpdateDaemon batchUpdateDaemon = null;
    protected InvalidationAuditDaemon invalidationAuditDaemon = null;
    protected RemoteServices nullRemoteServices = new NullRemoteServices();
    protected TimeLimitDaemon timeLimitDaemon = null;
    protected boolean servicesStarted = false;
    
    /* used by the core to get to the qualified caches*/
    protected ServletCacheUnit servletCacheUnit = null; 
    protected ObjectCacheUnit objectCacheUnit = null;
    private Object serviceMonitor = new Object();

    /**
     * Constructor with configuration file parameter.
     *
     * @param fileName The file name of the configuration XML file.
     */
    public CacheUnitImpl(CacheConfig cc){
        initialize(cc);
    }

    /**
     * This is a helper method called by this CacheUnitImpl's constructor.
     * It creates all the local objects - BatchUpdateDaemon, InvalidationAuditDaemon,
     * and TimeLimitDaemon. These objects are used by all cache instances.
     *
     * @param The file name of the configuration xml file.
     */
    public void initialize(CacheConfig cc) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "initialize");
        
        cacheConfig = cc;

        if (null != cc){
        	try {
        		if (tc.isDebugEnabled())
        			Tr.debug(tc, "Initializing CacheUnit " + uniqueServerNameFQ);
        		nullRemoteServices.setCacheUnit(uniqueServerNameFQ, this);
        	} catch (Exception ex) {
        		//ex.printStackTrace();
        		com.ibm.ws.ffdc.FFDCFilter.processException(ex, "com.ibm.ws.cache.CacheUnitImpl.initialize", "120", this);
        		Tr.error(tc, "dynacache.configerror", ex.getMessage());
        		throw new IllegalStateException("Unexpected exception: " + ex.getMessage());
        	}
        }
        
        if (tc.isEntryEnabled())
            Tr.exit(tc, "initialize");
    }
    
    public RemoteServices getRemoteService(){
    	return nullRemoteServices;
    }
    
    /**
     * This implements the method in the CacheUnit interface.
     * Return an unique name to indicate cellName, nodeName and serverName.
     * 
     * @return string an unique name to indicate cellName, nodeName and serverName
     */
    public String getUniqueServerNameFQ() {
        return uniqueServerNameFQ;
    }

    /**
     * This implements the method in the CacheUnit interface.
     * It applies the updates to the local internal caches and
     * the external caches.
     * It validates timestamps to prevent race conditions.
     *
     * @param invalidateIdEvents A HashMap of invalidate by id.
     * @param invalidateTemplateEvents A HashMap of invalidate by template.
     * @param pushEntryEvents A HashMap of cache entries.
     */
    public void batchUpdate(String cacheName, HashMap invalidateIdEvents, HashMap invalidateTemplateEvents, ArrayList pushEntryEvents) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "batchUpdate():"+cacheName);

        invalidationAuditDaemon.registerInvalidations(cacheName, invalidateIdEvents.values().iterator());
        invalidationAuditDaemon.registerInvalidations(cacheName, invalidateTemplateEvents.values().iterator());

        pushEntryEvents = invalidationAuditDaemon.filterEntryList(cacheName, pushEntryEvents);

        DCache cache = ServerCache.getCache(cacheName);
        if (cache!=null) {
            cache.batchUpdate(invalidateIdEvents, invalidateTemplateEvents, pushEntryEvents);
            
            if (cache.getCacheConfig().isEnableServletSupport() == true) {
                if (servletCacheUnit != null) {
                	servletCacheUnit.invalidateExternalCaches(invalidateIdEvents, invalidateTemplateEvents);
                } else {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "batchUpdate() cannot do invalidateExternalCaches because servletCacheUnit=NULL.");
                }
            }
        }
        if (tc.isEntryEnabled())
            Tr.exit(tc, "batchUpdate()");
    }

    /**
     * This implements the method in the CacheUnit interface.
     * This is called by DRSNotificationService and DRSMessageListener.
     * A returned null indicates that the local cache should
     * execute it and return the result to the coordinating CacheUnit.
     *
     * @param cacheName The cache name
     * @param id The cache id for the entry.  The id cannot be null.
     * @param ignoreCounting true to ignore statistics counting
     * @return The entry indentified by the cache id.
     */
    public CacheEntry getEntry(String cacheName, Object id, boolean ignoreCounting ) {  
        if (tc.isEntryEnabled())
            Tr.entry(tc, "getEntry: {0}", id);

        DCache cache = ServerCache.getCache(cacheName);
        CacheEntry cacheEntry = null;
        if ( cache != null ) {
            cacheEntry = (CacheEntry) cache.getEntry(id, CachePerf.REMOTE, ignoreCounting, DCacheBase.INCREMENT_REFF_COUNT);  
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "getEntry: {0}", id);
        return cacheEntry;
    }

    /**
     * This implements the method in the CacheUnit interface.
     * This is called by DRSNotificationService and DRSMessageListener.
     *
     * @param cacheName The cache name
     * @param cacheEntry The entry to be set.
     */
    public void setEntry(String cacheName, CacheEntry cacheEntry) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "setEntry: {0}", cacheEntry.id);
        cacheEntry = invalidationAuditDaemon.filterEntry(cacheName, cacheEntry);
        if (cacheEntry != null) {
            DCache cache = ServerCache.getCache(cacheName);
            cache.setEntry(cacheEntry,CachePerf.REMOTE);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "setEntry: {0}", cacheEntry==null?"null":cacheEntry.id );
    }

    /**
     * This implements the method in the CacheUnit interface.
     * This is called by DRSRemoteService and NullRemoteServices.
     *
     * @param externalCacheFragment The external cache fragment to be relayed.
     */
    public void setExternalCacheFragment(ExternalInvalidation externalCacheFragment) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "setExternalCacheFragment: {0}", externalCacheFragment.getUri());
        externalCacheFragment = invalidationAuditDaemon.filterExternalCacheFragment(ServerCache.cache.getCacheName(), externalCacheFragment);
        if (externalCacheFragment != null) {
            batchUpdateDaemon.pushExternalCacheFragment(externalCacheFragment, ServerCache.cache);  
        }
        if (tc.isEntryEnabled())
            Tr.exit(tc, "setExternalCacheFragment: {0}", externalCacheFragment.getUri());
    }

    /**
     * This implements the method in the CacheUnit interface.
     * This is delegated to the ExternalCacheServices.  
     * It calls ServletCacheUnit to perform this operation.
     *
     * @param groupId The external cache group id.
     * @param address The IP address of the target external cache.
     * @param beanName The bean name (bean instance or class) of
     * the ExternalCacheAdaptor that can deal with the protocol of the
     * target external cache.
     */
    public void addExternalCacheAdapter(String groupId, String address, String beanName) throws DynamicCacheServiceNotStarted {
    	if (servletCacheUnit == null) {
    		throw new DynamicCacheServiceNotStarted("Servlet cache service has not been started."); 
    	}
    	servletCacheUnit.addExternalCacheAdapter(groupId, address, beanName);
    }

    /**
     * This implements the method in the CacheUnit interface.
     * This is delegated to the ExternalCacheServices.
     * It calls ServletCacheUnit to perform this operation.
     *
     * @param groupId The external cache group id.
     * @param address The IP address of the target external cache.
     */
    public void removeExternalCacheAdapter(String groupId, String address) throws DynamicCacheServiceNotStarted {
    	if (servletCacheUnit == null) {
    		throw new DynamicCacheServiceNotStarted("Servlet cache service has not been started."); 
    	}
    	servletCacheUnit.removeExternalCacheAdapter(groupId, address);
    }

    /**
	 * This is called by ServerCache to start BatchUpdateDaemon,
	 * InvalidationAuditDaemon, TimeLimitDaemon and ExternalCacheServices
	 * These services should only start once for all cache instances
	 * 
	 * @param startTLD this param is false for a third party cache provider
	 */
	public void startServices(boolean startTLD) {
		synchronized (this.serviceMonitor) { //multiple threads can call this concurrently 
			if (this.batchUpdateDaemon == null) {
	            //----------------------------------------------
	            // Initialize BatchUpdateDaemon object
	            //----------------------------------------------
	            batchUpdateDaemon = new BatchUpdateDaemon(cacheConfig.batchUpdateInterval);

	            //----------------------------------------------
	            // Initialize InvalidationAuditDaemon object
	            //----------------------------------------------
	            invalidationAuditDaemon = new InvalidationAuditDaemon(cacheConfig.timeHoldingInvalidations);
	            
	            //----------------------------------------------
	            // link invalidationAuditDaemon to BatchUpdateDaemon
	            //----------------------------------------------
	            batchUpdateDaemon.setInvalidationAuditDaemon(invalidationAuditDaemon);
	            
	            if (tc.isDebugEnabled()) {
	                Tr.debug(tc, "startServices() - starting BatchUpdateDaemon/invalidationAuditDaemon services. " +
	                		"These services should only start once for all cache instances. Settings are: " + 
	                		 " batchUpdateInterval=" + cacheConfig.batchUpdateInterval + 
	                		 " timeHoldingInvalidations=" + cacheConfig.timeHoldingInvalidations);
	            }
	            
	            //----------------------------------------------
	            // start services
	            //----------------------------------------------
	            batchUpdateDaemon.start();
	            invalidationAuditDaemon.start();
			}
			if (startTLD && this.timeLimitDaemon == null) {
	            //----------------------------------------------
	            // Initialize TimeLimitDaemon object
	            //----------------------------------------------
	            // lruToDiskTriggerTime is set to the default (5 sec) under the following conditions
	            // (1) less than 1 msec
	            // (2) larger than timeGranularityInSeconds
	            int lruToDiskTriggerTime = CacheConfig.DEFAULT_LRU_TO_DISK_TRIGGER_TIME;
	            if (cacheConfig.lruToDiskTriggerTime > cacheConfig.timeGranularityInSeconds * 1000 ||
	                cacheConfig.lruToDiskTriggerTime < CacheConfig.MIN_LRU_TO_DISK_TRIGGER_TIME) {
	                Tr.warning(tc, "DYNA0069W", new Object[] { new Integer(cacheConfig.lruToDiskTriggerTime), 
	                           "lruToDiskTriggerTime", cacheConfig.cacheName, 
	                           new Integer(CacheConfig.MIN_LRU_TO_DISK_TRIGGER_TIME),
	                           new Integer(cacheConfig.timeGranularityInSeconds * 1000), 
	                           new Integer(CacheConfig.DEFAULT_LRU_TO_DISK_TRIGGER_TIME)});
	                cacheConfig.lruToDiskTriggerTime = lruToDiskTriggerTime;
	            } else {
	                lruToDiskTriggerTime = cacheConfig.lruToDiskTriggerTime;
	            }
	            
	            if (lruToDiskTriggerTime == CacheConfig.DEFAULT_LRU_TO_DISK_TRIGGER_TIME && 
	            	(cacheConfig.lruToDiskTriggerPercent > CacheConfig.DEFAULT_LRU_TO_DISK_TRIGGER_PERCENT || 
	            	 cacheConfig.memoryCacheSizeInMB != CacheConfig.DEFAULT_DISABLE_CACHE_SIZE_MB)) {
	            	lruToDiskTriggerTime = CacheConfig.DEFAULT_LRU_TO_DISK_TRIGGER_TIME_FOR_TRIMCACHE;
	            	cacheConfig.lruToDiskTriggerTime = lruToDiskTriggerTime;
	                Tr.audit(tc, "DYNA1069I", new Object[] { new Integer(cacheConfig.lruToDiskTriggerTime)}); 
	            }
	            //----------------------------------------------
	            // Initialize TimeLimitDaemon object
	            //----------------------------------------------
	            timeLimitDaemon = new TimeLimitDaemon(cacheConfig.timeGranularityInSeconds, lruToDiskTriggerTime);
	            
	            if (tc.isDebugEnabled()) {
	                Tr.debug(tc, "startServices() - starting TimeLimitDaemon service. " +
	                		"This service should only start once for all cache instances. Settings are: " + 
	                		 " timeGranularityInSeconds=" + cacheConfig.timeGranularityInSeconds + 
	                		 " lruToDiskTriggerTime=" + cacheConfig.lruToDiskTriggerTime);
	            }
	            
	            //----------------------------------------------
	            // start service
	            //----------------------------------------------
	            timeLimitDaemon.start();
			}
		}
	}

    /**
	 * This implements the method in the CacheUnit interface. This is called to
	 * add alias ids for cache id.
	 * 
	 * @param cacheName
	 *            The cache name
	 * @param id
	 *            The cache id
	 * @param aliasArray
	 *            The array of alias ids
	 */
    public void addAlias(String cacheName, Object id, Object[] aliasArray) {
        if (id != null && aliasArray != null) {
            DCache cache = ServerCache.getCache(cacheName);
            if (cache != null) {
                try {
                    cache.addAlias(id, aliasArray, false, false);
                } catch (IllegalArgumentException e) {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "Adding alias for cache id " + id + " failure: " + e.getMessage());
                    }
                }
            }
        }
    }

    /**
     * This implements the method in the CacheUnit interface.
     * This is called to remove alias ids from cache id.
     *
     * @param cacheName The cache name
     * @param alias The alias ids 
     */
    public void removeAlias(String cacheName, Object alias) {
        if (alias != null) {
            DCache cache = ServerCache.getCache(cacheName);
            if (cache != null) {
                try {
                    cache.removeAlias(alias, false, false);
                } catch (IllegalArgumentException e) {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "Removing alias " + alias + " failure: " + e.getMessage());
                    }
                }
            }
        }
    }
    
    /**
     * This implements the method in the CacheUnit interface.
     * This is called to get BatchUpdateDaemon object.
     *
     * @return BatchUpdateDaemon object
     */
    public BatchUpdateDaemon getBatchUpdateDaemon() {
    	return this.batchUpdateDaemon;
    }

    /**
     * This implements the method in the CacheUnit interface.
     * This is called to get Command Cache object.
     *
     * @param cacheName The cache name
     * @return CommandCache object
     */
	public CommandCache getCommandCache(String cacheName) throws DynamicCacheServiceNotStarted, IllegalStateException {
    	if (servletCacheUnit == null) {
    		throw new DynamicCacheServiceNotStarted("Servlet cache service has not been started."); 
    	}
		return servletCacheUnit.getCommandCache(cacheName);
	}
	
    /**
     * This implements the method in the CacheUnit interface.
     * This is called to get JSP Cache object.
     *
     * @param cacheName The cache name
     * @return JSPCache object
     */
	public JSPCache getJSPCache(String cacheName) throws DynamicCacheServiceNotStarted, IllegalStateException {
    	if (servletCacheUnit == null) {
    		throw new DynamicCacheServiceNotStarted("Servlet cache service has not been started."); 
    	}
    	return  servletCacheUnit.getJSPCache(cacheName);
	}
	
    /**
     * This implements the method in the CacheUnit interface.
     * This is called to link ServletCacheUnit object so that it can call ServletCacheUnit methods.
     *
     * @param servletCacheUnit The servletCacheUnit object
     */
	public void setServletCacheUnit(ServletCacheUnit servletCacheUnit) {
		this.servletCacheUnit = servletCacheUnit;
	}
	

	public ServletCacheUnit getServletCacheUnit() {
		return this.servletCacheUnit;
	}
	
    /**
     * This implements the method in the CacheUnit interface.
     * This is called to link ObjectCacheUnit object so that it can call ObjectCacheUnit methods.
     *
     * @param objectCacheUnit The objectCacheUnit object
     */
	public void setObjectCacheUnit(ObjectCacheUnit objectCacheUnit) {
		this.objectCacheUnit = objectCacheUnit;
	}
	
    /**
     * This implements the method in the CacheUnit interface.
     * This is called to create object cache.
     * It calls ObjectCacheUnit to perform this operation.
     *
     * @param cacheName The cache name
     */
    public Object createObjectCache(String cacheName) throws DynamicCacheServiceNotStarted, IllegalStateException {
    	if (objectCacheUnit == null) {
    		throw new DynamicCacheServiceNotStarted("Object cache service has not been started."); 
    	}
    	return objectCacheUnit.createObjectCache(cacheName);
    }

    /**
     * This implements the method in the CacheUnit interface.
     * This is called to create event source object.
     * It calls ObjectCacheUnit to perform this operation.
     *
     * @param createAsyncEventSource boolean true - using async thread context for callback; false - using caller thread for callback
     * @param cacheName The cache name
     * @return EventSourceIntf The event source
     */
	public EventSource createEventSource(boolean createAsyncEventSource, String cacheName) throws DynamicCacheServiceNotStarted {
    	if (objectCacheUnit == null) {
    		throw new DynamicCacheServiceNotStarted("Object cache service has not been started."); 
    	}
    	return objectCacheUnit.createEventSource(createAsyncEventSource, cacheName);
	}

	/**
     * This implements the method in the CacheUnit interface.
	 * Returns TimeLimitDaemon object. Called by ServerCache.createCache(). 
	 */
	public TimeLimitDaemon getTimeLimitDaemon() {
		return timeLimitDaemon;
	}

	@Override
	public InvalidationAuditDaemon getInvalidationAuditDaemon() {
		return invalidationAuditDaemon;
	}
}
