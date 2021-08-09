/*******************************************************************************
 * Copyright (c) 1997, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cache.web;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;

import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.servlet.cache.ExternalCacheAdapter;
import com.ibm.websphere.servlet.cache.ServletCacheRequest;
import com.ibm.ws.cache.InvalidateByTemplateEvent;
import com.ibm.ws.cache.ValueSet;
import com.ibm.ws.cache.util.ExceptionUtility;

/**
 * This class represents a group of external caches that cache the same
 * information and get the same page updates (writes and invalidations).
 */
public class ExternalCacheGroup implements Serializable {
    private static final long serialVersionUID = 1342185474L;

   private static TraceComponent tc = Tr.register(ExternalCacheGroup.class, "WebSphere Dynamic Cache", "com.ibm.ws.cache.resources.dynacache");
   public static final int SHARED = 0;
   public static final int NOT_SHARED = 1;

   private String id = null;
   private int type = SHARED;
   private Hashtable addressToBeanName = new Hashtable();
   private Hashtable addressToBean = new Hashtable();
   private ExternalCacheAdapter adapters[] = new ExternalCacheAdapter[0];

   /**
    * Constructor with parameters.
    *
    * @param id The external cache group id.
    * @param tyep Whether the external cache group is SHARED or NOT_SHARED.
    */
   public ExternalCacheGroup(String id, int type) {
      this.id = id;
      this.type = type;
   }

   /**
    * This gets the external cache group id.
    *
    * @return The id.
    */
   public String getId() {
      return id;
   }

   /**
    * This gets whether the external cache group is SHARED vs. NOT_SHARED.
    *
    * @return SHARED vs NOT_SHARED.
    */
   public int getType() {
      return type;
   }

   /**
    * This adds another ExternalCacheAdaptor to the group.
    *
    * @param address The TCP/IP address of the external cache.
    * @param beanName The bean name of the adaptor.
    */
   public void addExternalCacheAdapter(String address, String beanName) {
      ExternalCacheAdapter externalCacheAdapter = null;
      try {
         externalCacheAdapter = initializeAdapter(address, beanName.trim());
      } catch (Exception ex) {
         com.ibm.ws.ffdc.FFDCFilter.processException(ex, "com.ibm.ws.cache.servlet.ExternalCacheGroup.addExternalCacheAdapter", "92", this);
         Tr.error(tc, "dynacache.externaladaptererror", ex.getMessage());
         return;
      }
      if (tc.isDebugEnabled())
         Tr.debug(tc, "adding externalCache: " + id + " " + address + " " + beanName);
      Object temp = addressToBeanName.get(address);
      if (temp != null) {
         Tr.error(tc, "dynacache.externaldup", address);
      }
      addressToBeanName.put(address, beanName);
      addressToBean.put(address, externalCacheAdapter);
      adapters = (ExternalCacheAdapter[]) addressToBean.values().toArray(new ExternalCacheAdapter[0]);
   }
   

   /**
    * This removes an ExternalCacheAdaptor from the group.
    *
    * @param address The TCP/IP address of the external cache.
    */
   public void removeExternalCacheAdapter(String address) {
      addressToBean.remove(address);
      addressToBeanName.remove(address);
      adapters = (ExternalCacheAdapter[]) addressToBean.values().toArray(new ExternalCacheAdapter[0]);
   }

   /**
    * This method invalidates pages that are in the external caches
    * in the group.
    * It delegates the call to all ExternalCacheAdaptors.
    *
    * @param urlValueSet The ValueSet of URLs for the pages that have
    * previously been written to the external caches and need invalidation.
    */
   public void invalidatePages(ValueSet urlValueSet) {
      final String methodName="invalidatePages()";
      for (int i = 0; i < adapters.length; i++) {
          try {
              adapters[i].invalidatePages(urlValueSet.iterator());
          } catch (Exception ex) {  // 407042 add catch block
              com.ibm.ws.ffdc.FFDCFilter.processException(ex, "com.ibm.ws.cache.ExternalCacheGroup.invalidatePages", "120", null);
              if ( tc.isDebugEnabled() )
                  Tr.debug(tc, methodName+ " " + adapters[i] + " Exception: " +  ExceptionUtility.getStackTrace(ex));
          }
      }
   }

   /**
    * This method invalidates ids that are in the external caches
    * in the group.
    * It delegates the call to all ExternalCacheAdaptors.
    *
    * @param ids HashMap of ids to invalidate
    *
    */
   public void invalidateIds(HashMap ids) {
      final String methodName="invalidateIds()";
      for (int i = 0; i < adapters.length; i++) {
         try {
             adapters[i].invalidateIds(ids.keySet().iterator());
         } catch (Exception ex) {  // 407042 add catch block
             com.ibm.ws.ffdc.FFDCFilter.processException(ex, "com.ibm.ws.cache.ExternalCacheGroup.invalidateIds", "141", null);
             if ( tc.isDebugEnabled() )
                 Tr.debug(tc, methodName+ " " + adapters[i] + " Exception: " +  ExceptionUtility.getStackTrace(ex));
         }
      }
   }


   /**
    * This method clears all pages and ids that are in the external caches
    * in the group.
    * It delegates the call to all ExternalCacheAdaptors.
    *
    */   
   public void clear(InvalidateByTemplateEvent ie) {
       final String methodName="clear()";
       for (int i = 0; i < adapters.length; i++) {
          try {
              adapters[i].clear();
          } catch (Exception ex) {  // 407042 add catch block
              com.ibm.ws.ffdc.FFDCFilter.processException(ex, "com.ibm.ws.cache.ExternalCacheGroup.clear", "162", null);
              if ( tc.isDebugEnabled() )
                  Tr.debug(tc, methodName+ " " + adapters[i] + " Exception: " +  ExceptionUtility.getStackTrace(ex));
          }
       }       
   }

   /**
    * This method invalidates ids that are in the external caches
    * in the group.
    * It delegates the call to all ExternalCacheAdaptors.
    *
    * @param ids HashSet containing ValueSets of ids to invalidate
    *
    */
   public void invalidateIds(HashSet ids) {
      final String methodName="invalidateIds()";
      Iterator it = ids.iterator();
      while (it.hasNext()) {
         ValueSet v = (ValueSet) it.next();
         if (v != null && v.size() > 0) {
            for (int i = 0; i < adapters.length; i++) {
                try {
                    adapters[i].invalidateIds(v.iterator());
                } catch (Exception ex) {  // 407042 add catch block
                    com.ibm.ws.ffdc.FFDCFilter.processException(ex, "com.ibm.ws.cache.ExternalCacheGroup.invalidateIds", "188", null);
                    if ( tc.isDebugEnabled() )
                        Tr.debug(tc, methodName+ " " + adapters[i] + " Exception: " +  ExceptionUtility.getStackTrace(ex));
                }
            }
         }
      }
   }

   /**
    * This method writes pages to the external caches.
    * It delegates the call to all ExternalCacheAdaptors.
    *
    * @param externalCacheEntries The Vector of ExternalCacheEntry
    * objects for the pages that are to be cached.
    */
   public void writePages(ArrayList contentVector) {
      final String methodName="writePages()";
      for (int i = 0; i < adapters.length; i++) {
          try {
              adapters[i].writePages(contentVector.iterator());
          } catch (Exception ex) {  // 407042 add catch block
              com.ibm.ws.ffdc.FFDCFilter.processException(ex, "com.ibm.ws.cache.ExternalCacheGroup.writePages", "210", null);
              if ( tc.isDebugEnabled() )
                  Tr.debug(tc, methodName+ " " + adapters[i] + " Exception: " +  ExceptionUtility.getStackTrace(ex));
          }
      }
   }

   /**
    * This is a helper method called only by the ... method
    * It creates and initializes a single external cache adaptor.
    *
    * @param address The TCP/IP address of the external cache.
    * @param adaptorBeanName The bean name of the adaptor.
    * @return The initialized external cache adaptor.
    */
   private static ExternalCacheAdapter initializeAdapter(String address, String adapterBeanName) throws IOException, ClassNotFoundException {
	   final String methodName="initializeAdapter()";
	   try {
		   ExternalCacheAdapter externalCacheAdapter = null;
		   if (adapterBeanName.equals("com.ibm.websphere.servlet.cache.ESIInvalidatorServlet")) {
			   externalCacheAdapter = new com.ibm.websphere.servlet.cache.ESIInvalidatorServlet();    	  

		   } else if (adapterBeanName.equals("com.ibm.ws.cache.servlet.Afpa")) {
			   externalCacheAdapter = new com.ibm.ws.cache.servlet.Afpa(); 
		   } else {
			   Class extClass = ExternalCacheGroup.class.getClassLoader().loadClass(adapterBeanName);
			   externalCacheAdapter = (ExternalCacheAdapter) extClass.newInstance();
		   }
		   externalCacheAdapter.setAddress(address);
		   return externalCacheAdapter;
	   } catch (Exception ex) {
		   com.ibm.ws.ffdc.FFDCFilter.processException(ex, "com.ibm.ws.cache.ExternalCacheGroup.initializeAdapter", "233", null);
	   }
	   throw new ClassNotFoundException(adapterBeanName);
   }

   public void preInvoke(ServletCacheRequest req, HttpServletResponse resp) {
      final String methodName="preInvoke()";
      for (int i = 0; i < adapters.length; i++) {
          try {
              adapters[i].preInvoke(req, resp);
          } catch (Exception ex) {  // 407042 add catch block
              com.ibm.ws.ffdc.FFDCFilter.processException(ex, "com.ibm.ws.cache.ExternalCacheGroup.preInvoke", "244", null);
              if ( tc.isDebugEnabled() )
                  Tr.debug(tc, methodName+ " " + adapters[i] + " Exception: " +  ExceptionUtility.getStackTrace(ex));
          }
      }
   }

   public void postInvoke(ServletCacheRequest req, HttpServletResponse resp) {
      final String methodName="postInvoke()";
      for (int i = 0; i < adapters.length; i++) {
          try {
              adapters[i].postInvoke(req, resp);
          } catch (Exception ex) {  // 407042 add catch block
              com.ibm.ws.ffdc.FFDCFilter.processException(ex, "com.ibm.ws.cache.ExternalCacheGroup.postInvoke", "257", null);
              if ( tc.isDebugEnabled() )
                  Tr.debug(tc, methodName+ " " + adapters[i] + " Exception: " +  ExceptionUtility.getStackTrace(ex));
          }
      }
   }
}
