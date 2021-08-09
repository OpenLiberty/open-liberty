/*******************************************************************************
 * Copyright (c) 2003, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

// NOTE: D181601 is not changed flagged as it modifies every line of trace and FFDC.
 
package com.ibm.ws.sib.jfapchannel.impl;

import java.util.HashMap;
import java.util.Iterator;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.ws.sib.jfapchannel.JFapChannelConstants;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * A table which tracks the conversations taking place on a connection.
 * @author prestona
 */
public class ConversationTable
{
   private static final TraceComponent tc = SibTr.register(ConversationTable.class, JFapChannelConstants.MSG_GROUP, JFapChannelConstants.MSG_BUNDLE);

	// Map of IDs to conversations.   
   private final HashMap<ImmutableKey, ConversationImpl>idToConvTable;

   // Tracks the last ID reserved in the table.  This is used as a starting point when
   // looking for a free entry to reserve. 
   private short lastAllocatedId = 0;
   
   // Special "reserved" object value used to denote entries which have been reserved. 
   private final ConversationImpl reservedObject = new ConversationImpl();
   
   private class ImmutableKey
   {
      protected int value;
      
      public ImmutableKey(int value)
      {
         this.value = value;
      }
      
      public int getValue()
      {
         return value;
      }
      
      public boolean equals(Object o)
      {
         final boolean result;
         if (o == null) result = false;
         else if (o instanceof ImmutableKey)
         {
            result = value == ((ImmutableKey)o).value;
         }
         else result = false;
         return result;
      }
      
      public int hashCode()
      {
          return value;
      }
   }
   
   // Mutable key class - used to avoid instantiating Integer
   // objects when querying the table.
   private class MutableKey extends ImmutableKey
   {
      public MutableKey()
      {
         super(0);
      }
      
      public void setValue(int value)
      {
         this.value = value;
      }
   }
   

   
   private final MutableKey mutableKey = new MutableKey();
   
   static   
   {
		if (tc.isDebugEnabled()) SibTr.debug(tc, "@(#) SIB/ws/code/sib.jfapchannel.client.common.impl/src/com/ibm/ws/sib/jfapchannel/impl/ConversationTable.java, SIB.comms, WASX.SIB, uu1215.01 1.18");
   }
  
   // begin D181601
   private void debugTraceTable()
   {
      Iterator keyIterator = idToConvTable.keySet().iterator();
      SibTr.debug(this, tc, getClass().toString()+"@"+hashCode());
      while(keyIterator.hasNext())
      {
         ImmutableKey key = (ImmutableKey)keyIterator.next(); 
         SibTr.debug(this, tc, "[key: "+key.getValue()+"] -> [value:"+idToConvTable.get(key)+"]");
      }
   } 
   // end D181601

   /**
    * Create a new conversation table.
    */
   public ConversationTable()
   {
      if (tc.isEntryEnabled()) SibTr.entry(this, tc, "<init>");
      idToConvTable = new HashMap<ImmutableKey, ConversationImpl>();
      if (tc.isEntryEnabled()) SibTr.exit(this, tc, "<init>");
   }

	/**
	 * Test to determine if a particular conversation ID is present in the table.
	 * @param id
	 * @return boolean
	 */   
   public synchronized boolean contains(int id)
   {
      if (tc.isEntryEnabled()) SibTr.entry(this, tc, "contains", ""+id);
      mutableKey.setValue(id);
      boolean returnValue = idToConvTable.containsKey(mutableKey); 
      if (tc.isEntryEnabled()) SibTr.exit(this, tc, "contains", ""+returnValue);
      return returnValue;
   }
   
   /**
    * Add a conversation to the table.  The ID it is added with is taken from the conversation.
    * @param c
    */
   // begin D181493
   public synchronized void add(ConversationImpl c)
   {
		if (tc.isEntryEnabled()) SibTr.entry(this, tc, "add", c);
      
      final int id = c.getId();
      if (contains(id))
      {
         mutableKey.setValue(id);
         if (idToConvTable.get(mutableKey) == reservedObject)
         {
            if (tc.isDebugEnabled()) SibTr.debug(this, tc, "entry present but reserved: "+id);
         }
         else
         {
            if (tc.isDebugEnabled())
            {
               SibTr.debug(this, tc, "table already contains key of "+id);
               debugTraceTable(); 
            }
            throw new SIErrorException(TraceNLS.getFormattedMessage(JFapChannelConstants.MSG_BUNDLE, "CONVERSATIONTABLE_INTERNAL_SICJ0048", null, "CONVERSATIONTABLE_INTERNAL_SICJ0048")); // D226223 
         }
      }
      idToConvTable.put(new ImmutableKey(id), c);
      if (tc.isDebugEnabled()) SibTr.debug(this, tc, "add", "[key: "+id+"] -> [value: "+c+"]");
      if (tc.isEntryEnabled()) SibTr.exit(this, tc, "add"); 
   }
   // end D181493
   
   /**
    * Retrieve a conversation from the table by ID
    * @param id
    * @return ConversationImpl
    */
   public synchronized ConversationImpl get(int id)
   {
      if (tc.isEntryEnabled()) SibTr.entry(this, tc, "get", ""+id);
      mutableKey.setValue(id);
      ConversationImpl retValue = idToConvTable.get(mutableKey);
      if (tc.isEntryEnabled()) SibTr.exit(this, tc, "get", retValue);
      return retValue;
   }
   
   /**
    * Remove a conversation from the table by ID
    * @param id
    */
   public synchronized boolean remove(int id)
   {
		if (tc.isEntryEnabled()) SibTr.entry(this, tc, "remove", ""+id);
      
      final boolean result = contains(id);
      if (result)
      {
         mutableKey.setValue(id);
         idToConvTable.remove(mutableKey);
      }
      if (tc.isEntryEnabled()) SibTr.exit(this, tc, "remove", ""+result);
      return result;
   }
   
   /**
    * Returns an iterator which iterates over the tables
    * contents.  The values returned by this iterator are
    * objects of type Conversation.
    * @return Iterator
    */
   public synchronized Iterator iterator()
   {
		if (tc.isEntryEnabled()) SibTr.entry(this, tc, "iterator");
      Iterator returnValue = idToConvTable.values().iterator();
		if (tc.isEntryEnabled()) SibTr.exit(this, tc, "iterator", returnValue);
   	return returnValue;
   }
   
   /**
    * Clears all the entries from the table.
    */
   // begin F175658
   public synchronized void clear()
   {
      if (tc.isEntryEnabled()) SibTr.entry(this, tc, "clear");
      idToConvTable.clear();
      if (tc.isEntryEnabled()) SibTr.exit(this, tc, "clear");
   }
   // end F175658
   
   // begin D181493
   /**
    * Reserves an ID in the connection table.  This provides a way of obtaining
    * the ID of an unused entry.  Once an entry is reserved, a conversation may
    * still be added to the table which uses this ID and the reserved entry may
    * be removed.  Until a reserved entry is removed (or a conversation is added
    * with the reserved entry and subsequently removed) the ID will never be
    * returned by this method. 
    * @return int The ID which was reserved.
    * @throws IdTableFullException Thrown if no free table entries exist.
    */
   public synchronized int reserveId() throws IdTableFullException
   {
      if (tc.isEntryEnabled()) SibTr.entry(this, tc, "reserveId");
      boolean found = false;
      boolean full = false;
      short id = (short)(lastAllocatedId + 1);
      while(!found && !full)
      {      
         if (id == 0) id = 1;             // Never return zero.
         full = id == (lastAllocatedId);                  
         found = !contains(id);
         if (!found)
             ++id;
      }
      if (found) 
         idToConvTable.put(new ImmutableKey(id), reservedObject);
      else 
         throw new IdTableFullException();
      lastAllocatedId = id;
      if (tc.isEntryEnabled()) SibTr.exit(this, tc, "reserveId", ""+id);
      return id;
   }
   // end D181493
}

