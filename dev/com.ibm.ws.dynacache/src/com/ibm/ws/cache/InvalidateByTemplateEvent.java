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
package com.ibm.ws.cache;

import java.io.*;

import com.ibm.ws.cache.util.ObjectSizer;

/**
 * This class represents an invalidation by template event.
 */
public class InvalidateByTemplateEvent implements InvalidationEvent, Externalizable {

    private static final long serialVersionUID = 1342185474L;;

   //------------------------------------
   // CPF-Portal Add
   //------------------------------------
   private final static long CACHE_COMMAND_NONE                    = 0x00;
   private final static long CACHE_COMMAND_CLEAR                   = 0x01;
   private final static long CACHE_COMMAND_INVALIDATE_BY_TEMPLATE  = 0x02;
   private final static long CACHE_COMMAND_INVALIDATE_BY_ID        = 0x04;
   private final static long CACHE_COMMAND_SPARE2                  = 0x08;
   private long cacheCommand = CACHE_COMMAND_NONE;
   //------------------------------------


   /**
    * The Template to be invalidated.
    */
   protected String template = null;
   protected long timeStamp = -1;
   public int source;
   // protected Cache cache = null; // 245015
   private ValueSet removedIds;

   /**
    * This is the absolute time when the event leaves
    * the local machine heading for a remote machine.
    * It is used by the receiving machine to adjust
    * timeStamp.
   */
   private long drsClock = -1;
   
   /**
    * Constructor with parameters.
    *
    * @param template The Template to be invalidated.
    */
   public InvalidateByTemplateEvent(String template, int source) { // 245015
      this.template = template;
      this.source = source;
      timeStamp = System.currentTimeMillis();
      // this.cache = cache; // 245015
      setCacheCommand_InvalidateByTemplate();
   }

   public InvalidateByTemplateEvent(String template, long timeStamp, int source) { // 245015
      this.template = template;
      this.source = source;
      this.timeStamp = timeStamp;
      // this.cache = cache; // 245015
      setCacheCommand_InvalidateByTemplate();
   }

   public ValueSet getRemovedIds() {
      return removedIds;
   }

   public void addRemovedIds(ValueSet ids) {
      if (removedIds == null) removedIds = new ValueSet(4);
      removedIds.union(ids);
   }

   //for serialization only
   public InvalidateByTemplateEvent() {
   }

   void setCacheCommand_Clear() {
       cacheCommand |= CACHE_COMMAND_CLEAR;
   }
   private void setCacheCommand_InvalidateByTemplate() {
       cacheCommand |= CACHE_COMMAND_INVALIDATE_BY_TEMPLATE;
   }
   public boolean isCacheCommand_Clear() {
       return (cacheCommand & CACHE_COMMAND_CLEAR) == CACHE_COMMAND_CLEAR;
   }
   public boolean isCacheCommand_InvalidateByTemplate() {
       return (cacheCommand & CACHE_COMMAND_INVALIDATE_BY_TEMPLATE) == CACHE_COMMAND_INVALIDATE_BY_TEMPLATE;
   }

    //------------------------------------------------------
   /**
    * This returns the Template to be invalidated.
    *
    * @return The Template.
    */
   public String getTemplate() {
      return template;
   }

   /**
    * This implements the method in the InvalidationEvent interface.
    *
    * @return The creation timestamp.
    */
   public long getTimeStamp() {
      return timeStamp;
   }

   public void writeExternal(ObjectOutput out) throws IOException {
      out.writeObject(template);
      out.writeLong(timeStamp);
      out.writeLong(drsClock);
      out.writeInt(source);
      // out.writeObject(cache.getCacheName()); // 245015
      out.writeLong(cacheCommand);
   }

   public void readExternal(ObjectInput in) throws IOException,ClassNotFoundException {
         template = (String) in.readObject();
         timeStamp = in.readLong();
         drsClock  = in.readLong();
         source = in.readInt();
         // String cacheName = (String) in.readObject(); // 245015
         // cache = ServerCache.getCache(cacheName); // 245015
         cacheCommand = in.readLong();
   }

   /**
    * @return estimate (serialized) size of InvalidateByTemplateEvent. It is called by DRS to calculate the payload.
    */     
   public long getSerializedSize() {
		long totalSize = 0;
		if (template != null) {
			totalSize += ObjectSizer.getSize(template);
		}
		//System.out.println("InvalidateByTemplateEvent.getSerializedSize(): id=" + template + " size=" + totalSize);
		return totalSize;
	}
}
