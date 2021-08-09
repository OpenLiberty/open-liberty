/*******************************************************************************
 * Copyright (c) 2004, 2008 IBM Corporation and others.
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
import java.util.LinkedList;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.ws.sib.jfapchannel.*;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * A table which maps request ID's to entries containing a SendListener and
 * a ReceiveListener.
 * <p>
 * The current implementation uses a hash map with the same object as both
 * value and key.  The object used (RequestIdTableEntry) has equals and
 * hashCode methods which only discriminate based on requestId.  This allows
 * us to keep a "test" instance which we can simply load with the id
 * we are interested in and not have to create too many unnecessary
 * intermediate objects.
 *
 * @author prestona
 */
public class RequestIdTable
{
	private static final TraceComponent tc = SibTr.register(RequestIdTable.class, JFapChannelConstants.MSG_GROUP, JFapChannelConstants.MSG_BUNDLE);

   private static final TraceNLS nls = TraceNLS.getTraceNLS(JFapChannelConstants.MSG_BUNDLE);   // D226223

   // The request ID table.
   private final HashMap<RequestIdTableEntry, RequestIdTableEntry> table;

   // A 'captive' request ID that is used for querying the table.
   private RequestIdTableEntry testReqIdTableEntry = null;

   static
   {
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "@(#) SIB/ws/code/sib.jfapchannel.client.common.impl/src/com/ibm/ws/sib/jfapchannel/impl/RequestIdTable.java, SIB.comms, WASX.SIB, uu1215.01 1.16");
   }

   /**
    * Represents an entry in the request ID table.
    * @author prestona
    */
   private static class RequestIdTableEntry
   {
      public int requestId;
      public ReceiveListener receiveListener;
      public SendListener sendListener;

      public RequestIdTableEntry(int id, ReceiveListener listener, SendListener sl)
      {
         if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "RequestIdTableEntry.<init>", new Object[] {""+id, listener, sl});
         requestId = id;
         receiveListener = listener;
         sendListener = sl;
         if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "RequestIdTableEntry.<init>");
      }

      public boolean equals(Object o)
      {
         if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "RequstIdTableEntry.equals", o);
         boolean value = false;
         if (o instanceof RequestIdTableEntry)
         {
            value = ((RequestIdTableEntry)o).requestId == requestId;
         }
         if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "RequstIdTableEntry.equals", ""+value);
         return value;
      }

      public int hashCode()
      {
         if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "RequstIdTableEntry.hashCode");
         if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "RequstIdTableEntry.hashCode", ""+requestId);
         return requestId;
      }

      // begin D181601
      public String toString()
      {
         return getClass()+"@"+System.identityHashCode(this)+
                " requestid:"+requestId+ " receiveListener:"+receiveListener+" sendListener";
      }
      // end D181601
   }

   /**
    * Creates a new, empty, request id table.
    */
   public RequestIdTable()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "<init>");
      table = new HashMap<RequestIdTableEntry, RequestIdTableEntry>();
      testReqIdTableEntry = new RequestIdTableEntry(0, null, null);
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "<init>");
   }

   /**
    * Adds an entry to the request ID table using the specified request ID.
    * @param requestId The request ID (must not already be in the table).
    * @param rl The receive listener to associated with the
    * request id
    * @param s The send listener to associate with the request
    */
   public synchronized void add(int requestId, ReceiveListener rl, SendListener s)
   {
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "add", new Object[] {""+requestId, rl, s});
      if (containsId(requestId))
      {
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) debugTraceTable("id ("+requestId+") already in table");
         throw new SIErrorException(nls.getFormattedMessage("REQIDTABLE_INTERNAL_SICJ0058", null, "REQIDTABLE_INTERNAL_SICJ0058")); // D226223
      }

      RequestIdTableEntry newEntry = new RequestIdTableEntry(requestId, rl, s);
      table.put(newEntry, newEntry);
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "add");
   }

   /**
    * Removes the specified request ID (and associated data) from the table.
    * The ID must already be in the table otherwise a runtime exception
    * is thrown.
    * @param requestId The request ID to remove from the table.
    */
   public synchronized void remove(int requestId)
   {
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "remove");
		
      if (!containsId(requestId))
      {
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) debugTraceTable("id ("+requestId+") not in table");
         throw new SIErrorException(nls.getFormattedMessage("REQIDTABLE_INTERNAL_SICJ0058", null, "REQIDTABLE_INTERNAL_SICJ0058")); // D226223
      }

      testReqIdTableEntry.requestId = requestId;
      table.remove(testReqIdTableEntry);

		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "remove");
   }

   /**
    * Returns the semaphore assocaited with the specified request ID.  The
    * request ID must be present in the table otherwise a runtime exception
    * is thrown.
    * @param requestId The request ID to retreive the semaphore for.
    * @return Semaphore The semaphore retrieved.
    */
   public synchronized SendListener getSendListener(int requestId)
   {
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getSendListener", ""+requestId);
		
      if (!containsId(requestId))
      {
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) debugTraceTable("id ("+requestId+") not in table");
         throw new SIErrorException(nls.getFormattedMessage("REQIDTABLE_INTERNAL_SICJ0058", null, "REQIDTABLE_INTERNAL_SICJ0058")); // D226223
      }

      testReqIdTableEntry.requestId = requestId;
      RequestIdTableEntry entry = table.get(testReqIdTableEntry);

		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getSendListener", entry.sendListener);
      return entry.sendListener;
   }

   /**
    * Returns the receive listener associated with the specified request ID.
    * The request ID must be present in the table otherwise a runtime
    * exception will be thrown.
    * @param requestId The request ID to retrieve the receive listener for.
    * @return ReceiveListener The receive listener received.
    */
   public synchronized ReceiveListener getListener(int requestId)
   {
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getListener", ""+requestId);
      if (!containsId(requestId))
      {
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) debugTraceTable("id ("+requestId+") not in table");
         throw new SIErrorException(nls.getFormattedMessage("REQIDTABLE_INTERNAL_SICJ0058", null, "REQIDTABLE_INTERNAL_SICJ0058")); // D226223
      }


      testReqIdTableEntry.requestId = requestId;
      RequestIdTableEntry entry = table.get(testReqIdTableEntry);

		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getListener", entry.receiveListener);
      return entry.receiveListener;
   }

   /**
    * Tests to see if the table contains a specified request ID.
    * @param requestId The request ID to test.
    * @return boolean Returns true if and only if the table contains the
    * request ID.
    */
   public synchronized boolean containsId(int requestId)
   {		
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "containsId", ""+requestId);
	
      testReqIdTableEntry.requestId = requestId;
		boolean result = table.containsKey(testReqIdTableEntry);
		
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "containsId", ""+result);
      return result;
   }

   /**
    * Returns an iterator which iterates over receive listeners
    * in the table.
    * @return Iterator
    */
	public synchronized Iterator receiveListenerIterator()
	{
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "receiveListenerIterator");
		
      final LinkedList<ReceiveListener>linkedList = new LinkedList<ReceiveListener>();
		final Iterator iterator = table.values().iterator();

		while(iterator.hasNext())
		{
			final RequestIdTableEntry tableEntry = (RequestIdTableEntry)iterator.next();
         if (tableEntry.receiveListener != null) linkedList.add(tableEntry.receiveListener);
		}

      final Iterator result = linkedList.iterator();
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "receiveListenerIterator", result);   	
		return result;
	}

   /**
    * Returns an iterator which iterates over the send listeners in the
    * table.
    * @return Iterator
    */
	public synchronized Iterator sendListenerIterator()
	{
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "sendListenerIterator");

		final LinkedList<SendListener> linkedList = new LinkedList<SendListener>();
		final Iterator iterator = table.values().iterator();

		while(iterator.hasNext())
		{
			final RequestIdTableEntry tableEntry = (RequestIdTableEntry)iterator.next();
         if (tableEntry.sendListener != null)
			linkedList.add(tableEntry.sendListener);
		}

      final Iterator result = linkedList.iterator();
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "sendListenerIterator", result);
		return result;
	}

   /**
    * Returns true iff the table contains at least one entry with a
    * receive listener registered for it.
    * @return boolean
    */
   // begin F174772
   public synchronized boolean hasReceiveListeners()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "hasReceiveListeners");
      boolean returnValue = false;
      if (!table.values().isEmpty())
      {
         returnValue = receiveListenerIterator().hasNext();
      }
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "hasReceiveListeners", ""+returnValue);
      return returnValue;
   }
   // end F174772

   /**
    * Returns true iff the table has at least one entry which
    * has a send listener associated with it.
    * @return boolean
    */
   // begin F174772
   public synchronized boolean hasSendListeners()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "hasSendListeners");
      boolean returnValue = false;
      if (!table.values().isEmpty())
      {
         returnValue = sendListenerIterator().hasNext();
      }
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "hasSendListeners", ""+returnValue);
      return returnValue;
   }
   // end F174772

   /**
    * Returns an iterator which iterates over the Id's in the
    * table (as Integer objects).
    * @return Iterator
    */
   public synchronized Iterator idIterator()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "idIterator");

      final LinkedList<Integer> linkedList = new LinkedList<Integer>();
      final Iterator iterator = table.values().iterator();

      while(iterator.hasNext())
      {
         final RequestIdTableEntry tableEntry = (RequestIdTableEntry)iterator.next();
         linkedList.add(Integer.valueOf(tableEntry.requestId));
      }

      final Iterator result = linkedList.iterator();
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "idIterator", result);
      return result;
   }

   /**
    * Clears out the contents of the table.
    */
   // begin F174772
   public synchronized void clear()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "clear");
      table.clear();
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "clear");
   }
   // end F174772

   // begin D181601
   private void debugTraceTable(String comment)
   {
      StringBuffer sb = new StringBuffer();
      sb.append("table:\n");
      Iterator keyIterator = table.keySet().iterator();
      if (!keyIterator.hasNext()) sb.append("   <empty>\n");
      else while(keyIterator.hasNext())
      {
         Object key = keyIterator.next();
         Object value = table.get(key);
         sb.append("   [key: "+key+"] -> [value: "+value+"]\n");
      }
      SibTr.debug(this, tc, sb.toString());
   }
   // end D181601
}
