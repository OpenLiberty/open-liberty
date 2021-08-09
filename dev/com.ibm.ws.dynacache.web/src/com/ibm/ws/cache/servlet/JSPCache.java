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
package com.ibm.ws.cache.servlet;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.cache.BatchUpdateDaemon;
import com.ibm.ws.cache.DynaCacheConstants;
import com.ibm.ws.cache.EntryInfo;
import com.ibm.ws.cache.RemoteServices;

import com.ibm.ws.cache.intf.DCache;
import com.ibm.ws.cache.web.ExternalCacheFragment;
import com.ibm.ws.cache.web.ExternalCacheServices;

/**
 * This class handles caching of JSPs (and servlets).
 * Its methods either delegate to the underlying Cache
 * or does JSP-specific logic and calls the underlying Cache.
 */
public class JSPCache implements com.ibm.ws.cache.intf.JSPCache {
	private DCache cache = null;
	private RemoteServices remoteServices = null;
	private BatchUpdateDaemon batchUpdateDaemon = null;
	private ExternalCacheServices externalCacheServices = null;
	private int defaultPriority = com.ibm.ws.cache.CacheConfig.DEFAULT_PRIORITY;
    private static TraceComponent tc = Tr.register(JSPCache.class, "WebSphere Dynamic Cache", "com.ibm.ws.cache.resources.dynacache");


	/**
	 * This sets this JVM's underlying Cache.
	 * It is called by the CacheUnitImpl when things get started.
	 *
	 * @param cache The Cache.
	 */
	public void setCache(DCache cache) {
		this.cache = cache;
	}

	/**
	 * This sets this JVM's BatchUpdateDaemon.
	 * It is called by the CacheUnitImpl when things get started.
	 *
	 * @param batchUpdateDaemon The BatchUpdateDaemon.
	 */
	public void setBatchUpdateDaemon(BatchUpdateDaemon batchUpdateDaemon) {
		this.batchUpdateDaemon = batchUpdateDaemon;
	}

	/**
	 * This sets the remoteServices variable.
	 * It is called by the CacheUnitImpl when things get started.
	 *
	 * @param remoteServices The remoteServices.
	 */
	public void setRemoteServices(RemoteServices remoteServices) {
		this.remoteServices = remoteServices;
	}

	/**
	 * This sets the externalCacheServices variable.
	 * It is called by the CacheUnitImpl when things get started.
	 *
	 * @param externalCacheServices The remoteServices.
	 */
	public void setExternalCacheServices(com.ibm.ws.cache.intf.ExternalCacheServices externalCacheServices) {
		this.externalCacheServices = (com.ibm.ws.cache.web.ExternalCacheServices)externalCacheServices;
	}

	/**
	 * This changes the default priority, which is used if
	 * priority is not set on a per entry basis.
	 * A higher priority allows the entry to remain longer in the cache
	 * when it is not being used.
	 * It only affects newly entered entries.
	 * Existing entries retain current priority.
	 *
	 * @param defaultPriority The new default priority.
	 */
	public void setDefaultPriority(int defaultPriority) {
		if (defaultPriority < 0) {
			throw new IllegalArgumentException("defaultPriority must be nonnegative");
		}
		this.defaultPriority = defaultPriority;
	}

	/**
	 * This is called by the CacheUnitImpl class when everything gets started.
	 */
	public void start() {
		if ((cache == null) || (batchUpdateDaemon == null) || (remoteServices == null)) {
			throw new IllegalStateException("cache, batchUpdateDaemon, and remoteServices " + "must all be set before start()");
		}
	}

	/**
	 * This sets the ExternalCacheFragment for the specified id.
	 * It delegates this call to the corresponding method on the
	 * RemoteServices object.
	 *
	 * @param id The cache id.
	 * @param externalCacheFragment The ExternalCacheFragment.
	 */
	public void setExternalCacheFragment(String id, ExternalCacheFragment externalCacheFragment) {
		remoteServices.setExternalCacheFragment(id, externalCacheFragment);
	}

	public void setValue(EntryInfo entryInfo, FragmentComposerMemento memento, ExternalCacheFragment externalCacheFragment) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
		      Tr.entry(tc, "setValue");

		//I can't figure out a better way to piggyback this message, yet
		//ExternalCacheFragment is removed from memento in RenderCoordinator
		memento.setExternalCacheFragment(externalCacheFragment);
		if (!entryInfo.wasPrioritySet()) {
			entryInfo.setPriority(defaultPriority);
		}
		cache.setValue(entryInfo, memento, DynaCacheConstants.VBC_CACHE_NEW_CONTENT);
		//The entry will not have been pushed if entry is not to be shared
		if (externalCacheFragment != null) {
                     if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
			  Tr.debug(tc,"Adding ECF to BatchUpdateDaemon");
		      batchUpdateDaemon.pushExternalCacheFragment(externalCacheFragment,cache);
		}
		//So the ExternalCacheFragment isn't stored and copied in local cache.
		memento.setExternalCacheFragment(null);

		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                 Tr.exit(tc, "setValue");

	}
	
	/**
	 * This method is used to reset the expiration time on an "invalid cache hit." This situation
	 * occurs when a container tells Dynacache to use a cache entry that is in the "invalid" state.
	 * After this usage, its expiration time must be updated.
	 * 
	 * @param entryInfo Info about the cache entry
	 * @param memento The memento itself. Not actually changed by method, but needed as input parameter
	 */
	public void setValidatorExpirationTime (EntryInfo entryInfo, FragmentComposerMemento memento) {
		cache.setValue(entryInfo, memento, DynaCacheConstants.VBC_USE_CACHED_CONTENT);		
	}
	
	/**
	 * 
	 * @param cacheEntry The cacheEntry in question
	 * @param directive Constant in DynaCacheConstants that indicates use_new_content or use_cached_content
	 */
	public void updateStatisticsForVBC (com.ibm.websphere.cache.CacheEntry cacheEntry, boolean directive) {
		cache.updateStatisticsForVBC(cacheEntry, directive);
	}

	/**
	 * This returns the cache entry identified by the specified cache id.
	 * It returns null if not in the cache.
	 * This is delegated to the underlying Cache.
	 *
	 * @param id The cache id for the entry.  The id cannot be null.
	 * @return The entry indentified by the cache id.
	 */

	public com.ibm.websphere.cache.CacheEntry getEntry(String id) {
	    com.ibm.websphere.cache.CacheEntry ce = cache.getEntry(id);
            if(ce != null){
                if(ce.getId() == null){
                    if(TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(tc, "getEntry(String): ce.getId is null, returning null.");
                    ce = null;           
                } 
                else if(!ce.getId().equals(id)){
                    if(TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(tc, "getEntry(String): ce.getId:" + ce.getId() + " does not equal id:" + id + ", returning null.");
                    ce = null;          
                }
            }
            return ce;
	}

	/**
	 * This returns the cache entry identified by the specified cache id.
	 * It returns null if not in the cache.
	 * This is delegated to the underlying Cache.
	 *
	 * @param entryInfo The entry info for the entry.  The id inside cannot be null.
	 * @return The entry indentified by the entry info.
	 */
	public com.ibm.websphere.cache.CacheEntry getEntry(EntryInfo entryInfo) {

		com.ibm.websphere.cache.CacheEntry ce = cache.getEntry(entryInfo);
		sanityCheck(entryInfo, ce);
		return ce;
	}

	public com.ibm.websphere.cache.CacheEntry getEntry(EntryInfo ei, boolean ignoreCounting) {
		
	   com.ibm.websphere.cache.CacheEntry ce = cache.getEntry(ei, true, ignoreCounting);
	   sanityCheck(ei, ce); 	
       return ce;      		
	}

	private void sanityCheck(EntryInfo entryInfo, com.ibm.websphere.cache.CacheEntry ce) {
		if(ce != null){
		    if(ce.getId() == null){
		        if(TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
		            Tr.debug(tc, "getEntry(EntryInfo): ce.getId is null, returning null.");
		        ce = null;  
		    } 
		    else if(!ce.getId().equals(entryInfo.getId())){   
		        if(TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
		            Tr.debug(tc, "getEntry(EntryInfo): ce.getId:" + ce.getId() + 
		            		" does not equal entryInfo.getId:" + entryInfo.getId() + ", returning null.");
		        ce = null;          
		    }
		}
	}

	/**
	 * This is used by the CacheHook to determine if an entry has
	 * been either removed or invalidated while it is being rendered.
	 * This is delegated to the underlying Cache.
	 *
	 * @param id The cache id for the entry being tested.
	 * @return True if id is in cache and !removeWhenUnpinned.
	 */
	public boolean isValid(String id) {
		return cache.isValid(id);
	}

	/**
	 * This tries to find a value in the cache.
	 * If it is not there, it will try to execute it.
	 * This is delegated to the underlying Cache.
	 *
	 * @param entryInfo The entry Info id for the entry.
	 * @param askPermission True implies that execution must ask the
	 * coordinating CacheUnit for permission.
	 */
	public Object getValue(EntryInfo entryInfo, boolean askPermission) {            // @A5C
		return cache.getValue(entryInfo, askPermission);
	}

	/**
	 * This invalidates in all caches all entries dependent on the specified
	 * id.  This is delegated to the underlying Cache.
	 *
	 * @param id The cache id or data id.
	 * @param waitOnInvalidation True indicates that this method should
	 * not return until the invalidations have taken effect on all caches.
	 * False indicates that the invalidations will be queued for later
	 * batch processing.
	 */
	public void invalidateById(String id, boolean waitOnInvalidation) {
		cache.invalidateById(id, waitOnInvalidation);
	}

	/**
	 * This invalidates in all caches all entries dependent on the specified
	 * template.  This is delegated to the underlying Cache.
	 *
	 * @param template The template name.
	 * @param waitOnInvalidation True indicates that this method should
	 * not return until the invalidations have taken effect on all caches.
	 * False indicates that the invalidations will be queued for later
	 * batch processing.
	 */
	public void invalidateByTemplate(String template, boolean waitOnInvalidation) {
		cache.invalidateByTemplate(template, waitOnInvalidation);
	}

	public boolean shouldPull(int share, String id) {
		return cache.shouldPull(share, id);
	}
	
	
	public void preInvoke(String cacheGroup,CacheProxyRequest request,CacheProxyResponse response) {
     externalCacheServices.preInvoke(cacheGroup,request,response);
   }

   public void postInvoke(String cacheGroup,CacheProxyRequest request,CacheProxyResponse response) {
       externalCacheServices.postInvoke(cacheGroup,request,response);    	
   }
   
   public boolean isCascadeCachespecProperties(){
	   return cache.getCacheConfig().isCascadeCachespecProperties();
   }
   
   public boolean isAutoFlushIncludes(){
	   return cache.getCacheConfig().isAutoFlushIncludes();
   }   
   
	public boolean alwaysSetSurrogateControlHdr() {		
		return cache.getCacheConfig().alwaysSetSurrogateControlHdr();
	}
	
	public int[] getFilteredStatusCodes() {		
		return cache.getCacheConfig().getFilteredStatusCodes();
	}

	@Override
	public DCache getCache() {
		return cache; 
	}
   
}
