/*******************************************************************************
 * Copyright (c) 1997, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cache.config;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import com.ibm.ws.cache.EntryInfo;  

public class ConfigEntry {
   private static TraceComponent tc = Tr.register(ConfigEntry.class,"WebSphere Dynamic Cache", "com.ibm.ws.cache.resources.dynacache");
   
   public String className;
   public String name;
   public String instanceName;  //MSI
   public String skipCacheAttribute;
   public HashSet allNames;
   public int sharingPolicy = 1; //not-shared
   public HashMap properties; // = new HashMap();
   public CacheId cacheIds[];
   public DependencyId dependencyIds[];
   public Invalidation invalidations[];
   
   public String appName;
   
   //implementation fields
   public int iClassName;
   public static final int SERVLET = 1;
   public static final int COMMAND = 2;
   public static final int WEB_SERVICE = 3;
   public static final int WEB_SERVICE_CLIENT = 4;
   public static final int STATIC = 5;
   public static final int PORTLET = 6;
   
   //Object array used for storing processor specific data
   //typically property data that has been parsed
   //format and size are determined by the processor
   public Object processorData[] = null;
     

   public String toString() {
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      pw.println("[CacheEntry]");
      pw.println("className     : " + className);
      pw.println("instanceName     : " + instanceName);  
      pw.println("skipCacheAttribute : " + skipCacheAttribute);
      pw.println("name          : " + name);
      pw.println("all names      : " + allNames);
      pw.println("sharingPolicy : " + sharingPolicy);
      pw.println("appName : " + appName);
      if(properties != null && properties.size() > 0) {       
         Iterator it = properties.values().iterator();
         while (it.hasNext()) {
            pw.println(((Property) it.next()).toString());
	   }
      }							
            
      for (int i = 0; cacheIds != null && i < cacheIds.length; i++) {
         pw.println("[CacheId " + i + "]");
         pw.println(cacheIds[i]);
      }
      for (int i = 0; dependencyIds != null && i < dependencyIds.length; i++) {
         pw.println("[Dependency " + i + "]");
         pw.println(dependencyIds[i]);
      }
      for (int i = 0; invalidations != null && i < invalidations.length; i++) {
         pw.println("[Invalidation " + i + "]");
         pw.println(invalidations[i]);
      }
      return sw.toString();
   }

   //produces nice ascii text
   public String fancyFormat() {
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      pw.println("[" + name + "]");
      pw.println("instanceName     : " + instanceName);   //MSI
      pw.println("skip-cache-attribute : " + skipCacheAttribute);
      pw.println("className      : " + className);
      //pw.println("name           : "+name);
      //pw.println("all names      : "+allNames);
      pw.println("sharing Policy : " + sharingPolicy + " "+                     // @A1C
                 (sharingPolicy==EntryInfo.NOT_SHARED?"NOT_SHARED":             // @A1A
                  sharingPolicy==EntryInfo.SHARED_PULL?"SHARED_PULL":           // @A1A
                  sharingPolicy==EntryInfo.SHARED_PUSH?"SHARED_PUSH":           // @A1A
                  sharingPolicy==EntryInfo.SHARED_PUSH_PULL?"SHARED_PUSH_PULL": // @A1A
                  "unknown" ) );                                                // @A1A
    if(properties != null && properties.size()>0) {   //ST-begin       
         Iterator it = properties.values().iterator();
         while (it.hasNext()) {
            pw.println(((Property) it.next()).fancyFormat(1));
	   }
      }  								//ST-end
      for (int i = 0; cacheIds != null && i < cacheIds.length; i++) {
         pw.println("[CacheId " + i + "]");
         pw.println(cacheIds[i].fancyFormat(1));
      }
      for (int i = 0; dependencyIds != null && i < dependencyIds.length; i++) {
         pw.println("[Dependency " + i + "]");
         pw.println(dependencyIds[i].fancyFormat(1));
      }
      for (int i = 0; invalidations != null && i < invalidations.length; i++) {
         pw.println("[Invalidation " + i + "]");
         pw.println(invalidations[i].fancyFormat(1));
      }
      return sw.toString();
   }

   //expects an enumeration of data ids and an enumeration of templates
   public static String getESIDependencies(Enumeration e1, Enumeration e2) {
      if ((e1 == null || !e1.hasMoreElements()))
         if ((e2 == null || !e2.hasMoreElements()))
            return null;
      StringBuffer sb = new StringBuffer("dependencies=\"");
      if (e1 != null)
         while (e1.hasMoreElements()) {
            sb.append(" ");
            sb.append((String) e1.nextElement());
         }
      if (e2 != null)
         while (e2.hasMoreElements()) {
            sb.append(" ");
            sb.append((String) e2.nextElement());
         }
      //don't append a trailing double quote, since we have to add the cache id later
      return sb.toString();
   }


   public Object clone() {
      ConfigEntry ce = new ConfigEntry();
      ce.className = className;
      ce.iClassName = iClassName;
      ce.instanceName = instanceName;  //MSI
      ce.skipCacheAttribute = skipCacheAttribute;
      ce.name = name;
      ce.allNames = allNames;
      ce.sharingPolicy = sharingPolicy;
      ce.appName = appName;
      if (properties != null) {				//ST-begin
	ce.properties = new HashMap(properties.size());
	for (Iterator i = properties.keySet().iterator(); i.hasNext();) {
            Object key = i.next();
            ce.properties.put(key, ((Property)properties.get(key)).clone());
         }

      }							     //ST-end

      if (cacheIds != null) {
         ce.cacheIds = new CacheId[cacheIds.length];
         for (int i = 0; i < cacheIds.length; i++) {
            ce.cacheIds[i] = (CacheId) cacheIds[i].clone();
         }
      }
      if (dependencyIds != null) {
         ce.dependencyIds = new DependencyId[dependencyIds.length];
         for (int i = 0; i < dependencyIds.length; i++) {
            ce.dependencyIds[i] = (DependencyId) dependencyIds[i].clone();
         }
      }
      if (invalidations != null) {
         ce.invalidations = new Invalidation[invalidations.length];
         for (int i = 0; i < invalidations.length; i++) {
            ce.invalidations[i] = (Invalidation) invalidations[i].clone();
         }
      }
      if (processorData != null) {
      	ce.processorData = new Object[processorData.length];
      	for (int i=0;i<processorData.length;i++)
      	  ce.processorData[i] = processorData[i];
      }
      return ce;
   }
}
