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
package com.ibm.ws.cache.config;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Iterator;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;


public class CacheId {

   protected static TraceComponent tc = Tr.register(CacheId.class,"WebSphere Dynamic Cache","com.ibm.ws.cache.resources.dynacache");

   public int           timeout;
   public int           inactivity; // CPF-Inactivity
   public int           priority;
   public String        idGenerator;
   public String        metaDataGenerator;
   public HashMap       properties;
   public Component     components[];

   // Implementation fields
   public Object        idGeneratorImpl;
   public Object        metaDataGeneratorImpl;

   //Object array used for storing processor specific data
   //typically property data that has been parsed
   //format and size are determined by the processor
   public Object processorData[] = null;


   public String toString() {
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      pw.println("timeout          : "+timeout);
      pw.println("inactivity       : "+inactivity); // CPF-Inactivity
      pw.println("priority         : "+priority);
      pw.println("idGenerator      : "+idGenerator);
      pw.println("metaDataGenerator: "+metaDataGenerator);
      pw.println("properties       : "+properties);
      for (int i=0;components != null && i<components.length;i++) {
         pw.println("Component "+i);
         pw.println(components[i]);
      }
      return sw.toString();
   }


   public String fancyFormat(int level) {
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      for (int i = level;i >0; i--) pw.print("\t");
      pw.println("timeout          : "+timeout);
      for (int i = level;i >0; i--) pw.print("\t"); // CPF-Inactivity
      pw.println("inactivity       : "+inactivity); // CPF-Inactivity
      for (int i = level;i >0; i--) pw.print("\t");
      pw.println("priority         : "+priority);
      for (int i = level;i >0; i--) pw.print("\t");
      pw.println("idGenerator      : "+idGenerator);
      for (int i = level;i >0; i--) pw.print("\t");
      pw.println("metaDataGenerator: "+metaDataGenerator);
      for (int i = level;i >0; i--) pw.print("\t");
      for (int i=0;properties != null && i<properties.size();i++) {   //ST-begin
         for (int ii = level;ii >0; ii--) pw.print("\t");
         pw.println("property "+i+":");
         Iterator it = properties.values().iterator();
         while (it.hasNext()) {
            pw.println(((Property) it.next()).fancyFormat(level+1));
	   }
      }        							      //ST-end
      for (int i=0;components != null && i<components.length;i++) {
         for (int ii = level;ii >0; ii--) pw.print("\t");
         pw.println("Component "+i+":");
         pw.println(components[i].fancyFormat(level+1));
      }
      return sw.toString();
   }


   public Object clone() {
      CacheId c =  new CacheId();
      c.timeout = timeout;
      c.inactivity = inactivity;    // CPF-Inactivity
      c.priority = priority;
      c.idGenerator = idGenerator;
      c.metaDataGenerator = metaDataGenerator;
       if (properties != null) {			  //ST-begin
	c.properties = new HashMap(properties.size());
	for (Iterator i = properties.keySet().iterator(); i.hasNext();) {
            Object key = i.next();
            c.properties.put(key, ((Property)properties.get(key)).clone());
         }

      }							     //ST-end

      if (components != null)  {
         c.components = new Component[components.length];
         for (int i = 0; i < components.length;i++) {
            c.components[i] = (Component )components[i].clone();
         }
      }
      if (processorData != null) {
      	c.processorData = new Object[processorData.length];
      	for (int i=0;i<processorData.length;i++)
      	  c.processorData[i] = processorData[i];
      }
      
      return c;
   }

}
