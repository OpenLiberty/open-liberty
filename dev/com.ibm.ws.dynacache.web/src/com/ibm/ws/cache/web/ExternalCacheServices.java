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

import java.io.Serializable;
import java.util.*;

import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.servlet.cache.ServletCacheRequest;
import com.ibm.ws.cache.DCacheBase;
import com.ibm.ws.cache.DependencyTable;
import com.ibm.ws.cache.InvalidateByTemplateEvent;
import com.ibm.ws.cache.ValueSet;

/**
 * This class allows common operations to be applied to multiple cache groups.
 */
public class ExternalCacheServices implements com.ibm.ws.cache.intf.ExternalCacheServices, Serializable {
    private static final long serialVersionUID = 1342185474L;
   private static TraceComponent tc = Tr.register(ExternalCacheServices.class, "WebSphere Dynamic Cache", "com.ibm.ws.cache.resources.dynacache");

   //Hashtable with key = groupIds, value = ExternalCacheGroup
   private HashMap externalCacheGroups = new HashMap();
   private HashMap externalURLToECFTable = new HashMap();
   private DependencyTable idDependencyTable = new DependencyTable(DependencyTable.HASHTABLE, DCacheBase.DEFAULT_CACHE_SIZE / 10);   
   private DependencyTable uriDependencyTable = new DependencyTable(DependencyTable.HASHTABLE, DCacheBase.DEFAULT_CACHE_SIZE / 10);  

   public Iterator getExternalCacheGroupNames() {
      return externalCacheGroups.keySet().iterator();
   }

   /**
    * This is called by the CacheUnitImpl when things get started.
    *
    * @param externalCacheGroups The external cache groups metadata.
    */
   public void setExternalCacheGroups(HashMap externalCacheGroups) {
      this.externalCacheGroups = externalCacheGroups;
   }

   /**
    * This allows an administrator to dynamically register a new
    * ExternalCacheAdaptor with CacheUnit.
    * It is delegated from the CacheUnitImpl when it receives this call.
    *
    * @param groupId The external cache group id.
    * @param address The IP address of the target external cache.
    * @param beanName The bean name (bean instance or class) of
    * the ExternalCacheAdaptor that can deal with the protocol of the
    * target  external cache.
    */
   public void addExternalCacheAdapter(String groupId, String address, String beanName) {
      ExternalCacheGroup externalCacheGroup = (ExternalCacheGroup) externalCacheGroups.get(groupId);

      if (externalCacheGroup == null) {
         Tr.error(tc, "dynacache.externalnotfound", groupId);
         return;
      }
      externalCacheGroup.addExternalCacheAdapter(address, beanName);
   }

   /**
    * This allows an administrator to dynamically unregister a
    * ExternalCacheAdaptor with CacheUnit.
    * It is delegated from the CacheUnitImpl when it receives this call.
    *
    * @param groupId The external cache group id.
    * @param address The IP address of the target external cache.
    */
   public void removeExternalCacheAdapter(String groupId, String address) {
      ExternalCacheGroup externalCacheGroup = (ExternalCacheGroup) externalCacheGroups.get(groupId);

      if (externalCacheGroup == null) {
         return;
      }
      externalCacheGroup.removeExternalCacheAdapter(address);
   }

   /**
    * This is called by the CacheUnitImpl class when things get started.
    */
   public void start() {
      if (externalCacheGroups == null) {
         throw new IllegalStateException("externalCacheGroups must be set before start()");
      }
   }

   /**
    * This is called by the local BatchUpdateDaemon when it wakes up
    * to process invalidations and sets.
    *
    * @param invalidateIdEvents A Vector of invalidate by id.
    * @param invalidateTemplateEvents A Vector of invalidate by template.
    * @param pushECFEvents A Vector of external cache fragment events.
    */
   public void batchUpdate(HashMap invalidateIdEvents, HashMap invalidateTemplateEvents, ArrayList pushECFEvents) {
      invalidateExternalCaches(invalidateIdEvents, invalidateTemplateEvents);
      writeToExternalCaches(pushECFEvents);
   }

   /**
    * This applies invalidation to all external caches.
    * It is called internally and by the CacheUnitImpl when things start up.
    *
    * @param invalidateIdEvents A list of invalidation id events.
    * @param invalidateTemplateEvents A list of invalidation template events.
    */
   public void invalidateExternalCaches(HashMap invalidateIdEvents, HashMap invalidateTemplateEvents) {
      //Note: I could write logic to check the timestamps before
      //invalidating, but I'm not gonna bother.
      if (externalCacheGroups == null || externalCacheGroups.size() == 0 ) {
         return;
      }
      //get all ecfs affected by Events
      ValueSet fullEcfValueSet = new ValueSet(4);
      Iterator it = invalidateIdEvents.keySet().iterator();
      while (it.hasNext()) {
         //String id = (String) it.next();    //SKS-O
         Object id = it.next();    //SKS-O
         if (tc.isDebugEnabled())
            Tr.debug(tc, "ExternalCacheServices.invalidate id: " + id);
         ValueSet ecfValueSet = idDependencyTable.removeDependency(id);
         if (ecfValueSet != null) {
            fullEcfValueSet.union(ecfValueSet);
         }
      }
      it = invalidateTemplateEvents.keySet().iterator();
      HashSet removedIdsSet = null;
      while (it.hasNext()) {
         String template = (String) it.next();
         if (tc.isDebugEnabled()) 
            Tr.debug(tc, "ExternalCacheServices.invalidate template: " + template);

         InvalidateByTemplateEvent ie = (InvalidateByTemplateEvent) invalidateTemplateEvents.get(template);
         // check if it is a clear command
         if (ie.isCacheCommand_Clear()) {
             Iterator externalCacheGroupEnumeration = externalCacheGroups.values().iterator();
             while (externalCacheGroupEnumeration.hasNext()) {
                ExternalCacheGroup externalCacheGroup = (ExternalCacheGroup) externalCacheGroupEnumeration.next();
                externalCacheGroup.clear(ie);
             }
             externalURLToECFTable.clear();
             idDependencyTable.clear();
             uriDependencyTable.clear();
             return;
         }else if(ie.isCacheCommand_InvalidateByTemplate()){
             if (tc.isDebugEnabled()) 
                 Tr.warning(tc, "Ignored ExternalCacheServices.invalidate template: " + template);
         }

         //The template could be ESI/1.0+, so we'll add in all the invalidated ids
         //to be removed via invalidateIds
         ValueSet removedIds = ie.getRemovedIds();
         if (removedIds != null && removedIds.size() >0) {
            if (removedIdsSet == null) removedIdsSet = new HashSet(invalidateTemplateEvents.size());
            removedIdsSet.add(removedIds);                
         }
         
         
         ValueSet ecfValueSet = (ValueSet) uriDependencyTable.removeDependency(template);

         if (ecfValueSet != null) {
            fullEcfValueSet.union(ecfValueSet);
         }
      }

      //update internal ecf tables
      ValueSet urlValueSet = new ValueSet(10);
      Iterator ecfEnumeration = fullEcfValueSet.iterator();
      while (ecfEnumeration.hasNext()) {
         ExternalCacheFragment ecf = (ExternalCacheFragment) ecfEnumeration.next();

         String uri = ecf.getUri();
         urlValueSet.add(uri);
         externalURLToECFTable.remove(uri);

         Enumeration idEnumeration = ecf.getInvalidationIds();
         while (idEnumeration.hasMoreElements()) {
            //String id = (String) idEnumeration.nextElement();   //SKS-O
            Object id = idEnumeration.nextElement();   //SKS-O
            idDependencyTable.removeEntry(id, ecf);
         }

         Enumeration uriEnumeration = ecf.getTemplates();
         while (uriEnumeration.hasMoreElements()) {
            String template = (String) uriEnumeration.nextElement();
            uriDependencyTable.removeEntry(template, ecf);
         }
      }
      // Invalidate external caches
      boolean urlEmpty = urlValueSet.isEmpty();
      boolean idEmpty = invalidateIdEvents.isEmpty();
      boolean templateIdsEmpty = removedIdsSet == null || removedIdsSet.isEmpty();
      if (urlEmpty && idEmpty && templateIdsEmpty) {
         return;
      }

      Iterator externalCacheGroupEnumeration = externalCacheGroups.values().iterator();
      while (externalCacheGroupEnumeration.hasNext()) {
         ExternalCacheGroup externalCacheGroup = (ExternalCacheGroup) externalCacheGroupEnumeration.next();
         if (!urlEmpty)
            externalCacheGroup.invalidatePages(urlValueSet);
         if (!idEmpty)
            externalCacheGroup.invalidateIds(invalidateIdEvents);
         if (!templateIdsEmpty) {
            externalCacheGroup.invalidateIds(removedIdsSet);
         }
      }
   }

   /**
    * This is a helper method that writes a page to all external caches.
    *
    * @param pushECFEvents The new page to be written to all external caches.
    */
   private void writeToExternalCaches(ArrayList pushECFEvents) {
      // Don't bother doing the work if have no cacheGroups to publish to
      if (externalCacheGroups == null) {
         return;
      }

      // A hashtable of vectors for each externalCacheGroup
      HashMap<String, ArrayList> hashmap = new HashMap<String, ArrayList>();

      int size = pushECFEvents.size();
      for (int i = 0; i < size; i++) {
         ExternalCacheFragment externalCacheFragment = (ExternalCacheFragment) pushECFEvents.get(i);

         String cacheGroupId = externalCacheFragment.getExternalCacheGroupId();

         Object cacheGroup = externalCacheGroups.get(cacheGroupId);
         if (cacheGroup == null) {
            continue;
         }
         //only add to contentVector if content is fresher than already have
         String url = externalCacheFragment.getUri();
	 boolean newVHost = false;  //NK
         ExternalCacheFragment oldExternalCacheFragment = (ExternalCacheFragment) externalURLToECFTable.get(url);

         if ((oldExternalCacheFragment != null) && (oldExternalCacheFragment.getTimeStamp() >= externalCacheFragment.getTimeStamp())) {
	    //check if the request is from a new virtual host..
	    if(!(oldExternalCacheFragment.getHostList()).contains(externalCacheFragment.getHost()))//NK5
	      newVHost = true;		//NK5 end
            if (System.currentTimeMillis() - oldExternalCacheFragment.getTimeStamp() < 30000)
               continue;
         }
	 externalCacheFragment.addHostToList(externalCacheFragment.getHost()); //NK5

         if(!newVHost){
	     externalURLToECFTable.put(url, externalCacheFragment);
	     Enumeration idEnumeration = externalCacheFragment.getInvalidationIds();

	     while (idEnumeration.hasMoreElements()) {
		String id = (String) idEnumeration.nextElement();
		idDependencyTable.add(id, externalCacheFragment);
	     }
    
	     Enumeration uriEnumeration = externalCacheFragment.getTemplates();
	     while (uriEnumeration.hasMoreElements()) {
		String template = (String) uriEnumeration.nextElement();
		uriDependencyTable.add(template, externalCacheFragment);
	     }
	 }
         ArrayList contentVector = (ArrayList) hashmap.get(cacheGroupId);
         if (contentVector == null) {
            contentVector = new ArrayList();
            hashmap.put(cacheGroupId, contentVector);
         }
         contentVector.add(externalCacheFragment.getEntry());
      }
      // Publish pages to external caches
      for(Map.Entry<String, ArrayList> entry : hashmap.entrySet()) {
         String key = entry.getKey();
         ArrayList contentVector = entry.getValue();
         ExternalCacheGroup externalCacheGroup = (ExternalCacheGroup) externalCacheGroups.get(key);

         externalCacheGroup.writePages(contentVector);
      }
   }

   public void preInvoke(String cacheGroup, ServletCacheRequest req, HttpServletResponse resp) {
      ExternalCacheGroup externalCacheGroup = (ExternalCacheGroup) externalCacheGroups.get(cacheGroup);
      if (externalCacheGroup != null) {
         externalCacheGroup.preInvoke(req, resp);
      }

   }

   public void postInvoke(String cacheGroup, ServletCacheRequest req, HttpServletResponse resp) {
      ExternalCacheGroup externalCacheGroup = (ExternalCacheGroup) externalCacheGroups.get(cacheGroup);
      if (externalCacheGroup != null) {
         externalCacheGroup.postInvoke(req, resp);
      }

   }
}
