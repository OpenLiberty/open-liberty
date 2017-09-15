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
public interface RemoteServices {

   /**
	* This sets this JVM's CacheUnit.
	* It is called by the CacheUnitImpl when things get started.
	*
	* @param cacheUnit The CacheUnit.
	*/
   public void setCacheUnit(String uniqueName, CacheUnit cacheUnit);

   public void setCacheName(String cacheName);

   public String getCacheName();

   public void setNotificationService(NotificationService notificationService);

   /**
	* This is called by the CacheUnitImpl class when everything gets started.
	* It gets a remote reference to all currently running CacheUnits.
	* It registers its local CacheUnit with all currently running CacheUnits.
	*/
   public void start();

   /**
	* This notifies this object of a new external cache group.
	* It is delegated from local CacheUnit when it gets the same remote call.
	*/
   public void setExternalCacheFragment(String parentFragmentId, ExternalInvalidation externalCacheFragment);

   /**
	* This allows the local Cache to set a new entry that it has created
	* in the entry's coordinating CacheUnit.
	*
	* @param cacheEntry The CacheEntry that is is being set.
	*/
   public void setEntry(CacheEntry cacheEntry);

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
   public CacheEntry getEntry(Object id);

   /**
	* This allows the BatchUpdateDaemon to send its batch update events
	* to all CacheUnits.
	*
	* @param invalidateIdEvents A Vector of invalidate by id.
	* @param invalidateTemplateEvents A Vector of invalidate by template.
	* @param pushEntryEvents A Vector of cache entries.
	*/
   public void batchUpdate(HashMap invalidateIdEvents, HashMap invalidateTemplateEvents, ArrayList pushEntryEvents, ArrayList aliasEntryEvents);  //CCC

   public boolean shouldPull(int share, Object id);

   public boolean isDRSReady();
   
   public boolean isDRSCongested();

   /**
    * This gets the current size of the pushPullTable for debug use only.
    * 
    * @return the current size of pushPullTable. 
    */
   public int getPushPullTableSize();
   
   /**
    * This gets hashcode for all the cache ids in PushPullTable for debug use only.
    * 
    * @return hashcode for all cache ids. 
    */
   public int getCacheIdsHashcodeInPushPullTable(boolean debug);  // LI4337-17
   
   /**
    * This gets all the cache ids in PushPullTable for debug use only.
    * 
    * @return array list of cache ids. 
    */
   public List getCacheIdsInPushPullTable();  // LI4337-17
   
}
