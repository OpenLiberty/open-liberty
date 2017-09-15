/*******************************************************************************
 * Copyright (c) 2003, 2005 IBM Corporation and others.
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

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.ws.sib.jfapchannel.JFapChannelConstants;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * A data type which allocates and maps IDs (integers) to objects.
 * Under the covers, this class uses an array to map indexes to objects.
 * The table is, currently, always grown and never shrinks.  By default
 * we grow the table rather than rescanning for free space.
 * @author prestona
 */
public class IdTable
{
	private static final TraceComponent tc = SibTr.register(IdTable.class, JFapChannelConstants.MSG_GROUP, JFapChannelConstants.MSG_BUNDLE);

   private static final TraceNLS nls = TraceNLS.getTraceNLS(JFapChannelConstants.MSG_BUNDLE);   // D226232
	
   // The table
   private Object[] table = null;

   // Maximum size for the table.
   private int maxSize = 0;  
  
   // Highest table index allocated
   private int highWaterMark = 0;
   
   // Lowest possible free index - is the starting point when scanning
   // the table looking for a free slot.
   private int lowestPossibleFree = 0;
   
   // Default initial size for the table which maps IDs to objects.
   private static final int DEFAULT_INITIAL_SIZE = 50;
   
   // Unit by which the table is grown.
   private static final int TABLE_GROWTH_INCREMENT = 50;
   
   static
   {
		if (tc.isDebugEnabled()) SibTr.debug(tc, "@(#) SIB/ws/code/sib.jfapchannel.client.common.impl/src/com/ibm/ws/sib/jfapchannel/impl/IdTable.java, SIB.comms, WASX.SIB, uu1215.01 1.12");
   }
   
   /**
    * Creates a new ID table.
    * @param maxSize Maximum number of entries permissible in the table
    */
   public IdTable(int maxSize) throws IllegalArgumentException
   {
		this(maxSize, Math.min(maxSize, DEFAULT_INITIAL_SIZE));
		if (tc.isEntryEnabled()) SibTr.entry(this, tc, "<init>");
		if (tc.isEntryEnabled()) SibTr.exit(this, tc, "<init>");
   }
 
   /**
    * Creates a new ID table.
    * @param maxSize Maximum number of entries permissible in the table
    * @param startSize Starting size for the table used to map IDs to objects.
    */
   public IdTable(int maxSize, int startSize)
   throws IllegalArgumentException
   {
		if (tc.isEntryEnabled()) SibTr.entry(this, tc, "idTable", new Object[] {""+maxSize, ""+startSize});
      if (maxSize < 1) throw new SIErrorException(nls.getFormattedMessage("IDTABLE_INTERNAL_SICJ0050", null, "IDTABLE_INTERNAL_SICJ0050"));  // D226232
      if ((startSize < 1) || (startSize > maxSize)) 
         throw new SIErrorException(nls.getFormattedMessage("IDTABLE_INTERNAL_SICJ0050", null, "IDTABLE_INTERNAL_SICJ0050"));  // D226232
         
      this.maxSize = maxSize;
      table = new Object[startSize+1];    
      lowestPossibleFree = 1;
      highWaterMark = 1;  
		if (tc.isEntryEnabled()) SibTr.exit(this, tc, "idTable");
   }
   
   
   /**
    * Adds an object to the table and returns the ID associated with the
    * object in the table.  The object will never be assigned an ID which
    * clashes with another object.
    * @param value The object to store in the table
    * @return int The ID value that has been associated with the object.
    * @throws IdTableFullException Thrown if there is no space left in the
    * table.
    */
   public synchronized int add(Object value)
   throws IdTableFullException
   {
		if (tc.isEntryEnabled()) SibTr.entry(this, tc, "add", value);
      
      if (value == null) throw new SIErrorException(nls.getFormattedMessage("IDTABLE_INTERNAL_SICJ0050", null, "IDTABLE_INTERNAL_SICJ0050"));  // D226232
      
      int id = 0;
           
      if (highWaterMark < table.length)
      {
         // Can we store the object in any free space at the end of the table?
         id = highWaterMark;
         if (table[id] != null) throw new SIErrorException(nls.getFormattedMessage("IDTABLE_INTERNAL_SICJ0050", null, "IDTABLE_INTERNAL_SICJ0050"));  // D226232
         table[id] = value;
         if (lowestPossibleFree == highWaterMark) ++lowestPossibleFree;
         ++highWaterMark;
      }
      else if (table.length < maxSize)
      {
         // Resize the table if we can
         growTable();
         id = highWaterMark;
         if (table[id] != null) throw new SIErrorException(nls.getFormattedMessage("IDTABLE_INTERNAL_SICJ0050", null, "IDTABLE_INTERNAL_SICJ0050"));  // D226232
         table[id] = value;
         if (lowestPossibleFree == highWaterMark) ++lowestPossibleFree;         
         ++highWaterMark;         
      }
      else
      {
         // Scan for free slot
         id = findFreeSlot();
         if (id == 0) throw new IdTableFullException();
      }
      
		if (tc.isEntryEnabled()) SibTr.exit(this, tc, "add", ""+id);
      return id;
   }
   
   /**
    * Removes an object from the table.
    * @param id The ID of the object to remove.  It is valid to specify
    * an ID which does not map to an object, this is effectivly a no-op.
    * @return Object The object which was removed or null if the ID did
    * not map to an object.
    * @throws IllegalArgumentException Thrown if the id specified is less than
    * one or larger than the maximum size specified when the table was
    * created.
    */
   public synchronized Object remove(int id)
   throws IllegalArgumentException
   {
		if (tc.isEntryEnabled()) SibTr.entry(this, tc, "remove", ""+id);
      
      Object returnValue = get(id);
      if (returnValue != null) table[id] = null;
            
      if (id < lowestPossibleFree) lowestPossibleFree = id;
      
      // If the ID removed was at just below the high water mark, then
      // move the high water mark down as far as we can.
      if ((id+1) == highWaterMark)
      {
         int index = id;
         while(index >= lowestPossibleFree)
         {
            if (table[index] == null) highWaterMark = index;
            --index; 
         }
      } 
      
		if (tc.isEntryEnabled()) SibTr.exit(this, tc, "remove", returnValue);
      return returnValue;
   }

   /**
    * Returns true iff the specified ID maps to an object.
    * @param id The ID to test
    * @return boolean True iff the ID maps to an object.
    * @throws IllegalArgumentException Thrown if the ID is less than one or
    * greater than the maximum size specified when the table was created.
    */
   public synchronized boolean contains(int id)
   throws IllegalArgumentException
   {
		if (tc.isEntryEnabled()) SibTr.entry(this, tc, "contains", ""+id);
      
      if ((id < 1) || (id > maxSize)) throw new SIErrorException(nls.getFormattedMessage("IDTABLE_INTERNAL_SICJ0050", null, "IDTABLE_INTERNAL_SICJ0050"));  // D226232		
      boolean returnValue = (id < table.length) && (table[id] != null); 
      
      if (tc.isEntryEnabled()) SibTr.exit(this, tc, "contains", ""+returnValue);
      return returnValue;
   }
   
   /**
    * Gets an object from the table by ID.  Returns the object associated
    * with the specified ID.  It is valid to specify an ID which does not
    * have an object associated with it.  In this case, a value of
    * null is returned.
    * @param id The ID to get the object associated with.
    * @return Object The object associated with the ID or null if no object is
    * associated with the ID.
    * @throws IllegalArgumentException Thrown if the ID is less than one or
    * greater than the maximum size for the table specified at the time the
    * table was created.
    */
   public synchronized Object get(int id)
   throws IllegalArgumentException
   {
		if (tc.isEntryEnabled()) SibTr.entry(this, tc, "get", ""+id);
      
      if ((id < 1) || (id > maxSize)) throw new SIErrorException(nls.getFormattedMessage("IDTABLE_INTERNAL_SICJ0050", null, "IDTABLE_INTERNAL_SICJ0050"));  // D226232
      Object returnValue = null;
      if (id < table.length) returnValue = table[id];
		if (tc.isEntryEnabled()) SibTr.exit(this, tc, "get", ""+returnValue); 
      return returnValue;
   }   
   
   /**
    * Helper method which increases the size of the table by a factor of
    * TABLE_GROWTH_INCREMENT until the maximum size for the table is
    * achieved.
    */
   private void growTable()
   {
		if (tc.isEntryEnabled()) SibTr.entry(this, tc, "growTable");      
      int newSize = Math.min(table.length+TABLE_GROWTH_INCREMENT, maxSize);
      if (tc.isDebugEnabled()) SibTr.debug(this, tc, "existing size="+table.length+" new size="+newSize);
      Object[] newTable = new Object[newSize+1];
      System.arraycopy(table, 0, newTable, 0, table.length);
      table = newTable;
		if (tc.isEntryEnabled()) SibTr.exit(this, tc, "growTable");
   }

   /**
    * Helper method which attempts to locate a free slot in the table.
    * The algorithm used is to scan from the lowest possible free value
    * looking for an entry with a null value.  If more than half the
    * table is scanned before a free slot is found, then the code also
    * attempts to move the high watermark back towards the beginning of
    * the table by scanning backwards looking for the last empty slot.
    * @return int A free slot to use, or zero if the table does not
    * contain a free slot.
    */   
   private int findFreeSlot()
   {
		if (tc.isEntryEnabled()) SibTr.entry(this, tc, "findFreeSlot");      
      boolean foundFreeSlot = false;
      int index = lowestPossibleFree;
      
      int largestIndex = Math.min(highWaterMark, table.length-1);
      while ((!foundFreeSlot) && (index <= largestIndex))
      {
         foundFreeSlot = (table[index] == null);
         ++index; 
      }

      int freeSlot = 0;
      if (foundFreeSlot)
      {
         freeSlot = index-1;
      
         // If we have already scanned more than half the table then
         // we might as well spend a little more time and see if we can
         // move the high water mark any lower...
         if ((index*2) > largestIndex)
         {
            boolean quit = false;
            int lowest = index;
            index = largestIndex;
            while (!quit && (index >= lowest))
            {
               if (table[index] == null) highWaterMark = index;
               else quit = true;
               --index;
            }
         }
      } 

		if (tc.isEntryEnabled()) SibTr.exit(this, tc, "findFreeSlot", ""+freeSlot);
      return freeSlot;
   }
}
