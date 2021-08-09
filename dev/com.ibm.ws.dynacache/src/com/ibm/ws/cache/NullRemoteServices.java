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
import java.util.List;

import com.ibm.ws.cache.intf.ExternalInvalidation;

/**
 * This class provides the mechanisms for the CacheUnit components
 * to access other (remote) CacheUnits in the cache group.
 */
public class NullRemoteServices implements RemoteServices {

   protected boolean initialized = false;
   protected String uniqueCacheName = null;
   protected String cacheName = null;
   protected CacheUnit cacheUnit = null;
   protected NotificationService notificationService = null;
   
   /**
	* This sets this JVM's CacheUnit.
	* It is called by the CacheUnitImpl when things get started.
	*
	* @param cacheUnit The CacheUnit.
	*/
   public void setCacheUnit(String uniqueCacheName, CacheUnit cacheUnit) {
	  this.uniqueCacheName = uniqueCacheName;
	  this.cacheUnit = cacheUnit;
   }

   public void setCacheName(String cacheName) {
	   this.cacheName = cacheName;
   }

   public String getCacheName() {
	   return this.cacheName;
   }

   public void setNotificationService(NotificationService notificationService) {
	  this.notificationService = notificationService;
   }

   /**
	* This is called by the CacheUnitImpl class when everything gets started.
	* It gets a remote reference to all currently running CacheUnits.
	* It registers its local CacheUnit with all currently running CacheUnits.
	*/
   public void start() {
	  notificationService.registerCacheUnit(uniqueCacheName, cacheUnit);
	  initialized = true;
   }

   /**
	* This notifies this object of a new external cache group.
	* It is delegated from local CacheUnit when it gets the same remote call.
	*/
   public void setExternalCacheFragment(String parentFragmentId, ExternalInvalidation externalCacheFragment) {
	   this.cacheUnit.setExternalCacheFragment(externalCacheFragment);
   }

   /**
	* This allows the local Cache to set a new entry that it has created
	* in the entry's coordinating CacheUnit.
	*
	* @param cacheEntry The CacheEntry that is is being set.
	*/
   public void setEntry(CacheEntry cacheEntry) {
	  // nothing to do
   }

   /**
	* This allows the local Cache to get an entry from the entry's
	* coordinating CacheUnit.
	*
	* @param entryInfo The EntryInfo that describes the
	* entry to be obtained.
	* @return The entry value identified by this entryInfo.
	* If it is null, the calling Cache will render the entry
	* and make it available to other caches via the setEntry method.
	*/
   public CacheEntry getEntry( Object id ) {
	  return null;
   }

   /**
	* This allows the BatchUpdateDaemon to send its batch update events
	* to all CacheUnits.
	*
	* @param invalidateIdEvents A Vector of invalidate by id.
	* @param invalidateTemplateEvents A Vector of invalidate by template.
	* @param pushEntryEvents A Vector of cache entries.
	*/
   public void batchUpdate(HashMap invalidateIdEvents, HashMap invalidateTemplateEvents, ArrayList pushEntryEvents, ArrayList aliasEntryEvents) {
	  notificationService.batchUpdate(invalidateIdEvents, invalidateTemplateEvents, pushEntryEvents, aliasEntryEvents, cacheUnit);
   }

   public boolean shouldPull(int share, Object id) {
	  return false;
   }

   public boolean isDRSReady() {
       return false;
   }
   
   public boolean isDRSCongested() {
       return false;
   }

   /**
    * This gets the current size of the pushPullTable for debug use only.
    * 
    * @return the current size of pushPullTable. 
    */
   public int getPushPullTableSize() {
       return this.notificationService.getPushPullTableSize();
   }

   /**
    * This gets hashcode for all the cache ids in PushPullTable for debug use only.
    * 
    * @return hashcode for all cache ids. 
    */
   public int getCacheIdsHashcodeInPushPullTable(boolean debug) {  // LI4337-17
       return this.notificationService.getCacheIdsHashcodeInPushPullTable(debug);
   }

   /**
    * This gets all the cache ids in PushPullTable for debug use only.
    * 
    * @return array list of cache ids. 
    */
   public List getCacheIdsInPushPullTable() {  // LI4337-17
       return this.notificationService.getCacheIdsInPushPullTable();
   }

}
