/*******************************************************************************
 * Copyright (c) 2004, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.comms.client.proxyqueue.impl;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.comms.CommsConstants;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * This class allocates ids up to the unsigned short maximum of 32,767. It keeps track of the 
 * ids that have been allocated and will attempt to be as efficient as possible in re-using ids
 * and not performing complete table scans when allocating.
 * <p>
 * To do this, it maintains a 4k byte array that is used to keep track of which ids have been 
 * allocated. Each byte of the array pertains to 8 ids that could have been allocated. When an
 * id is allocated, the bit is switched on and when it is de-allocated it is switched off.
 * <p>
 * The allocater also holds two counters. One indicates the next upwardly available id - this 
 * is incremented as ids are allocated and decremented if that id is freed. The other indicates
 * the last id that was freed so that creating then freeing in one succession will only use one
 * id.
 * 
 * @author Gareth Matthews
 */
public class ShortIdAllocator
{
   /** Class name for FFDC's */
   private static String CLASS_NAME = ShortIdAllocator.class.getName();

   /** Trace */
   private static final TraceComponent tc = SibTr.register(ShortIdAllocator.class, 
                                                           CommsConstants.MSG_GROUP, 
                                                           CommsConstants.MSG_BUNDLE);
                                                           
   /** NLS handle */
   private static final TraceNLS nls = TraceNLS.getTraceNLS(CommsConstants.MSG_BUNDLE);

   /** Log class info on load */
   static
   {
      if (tc.isDebugEnabled()) SibTr.debug(tc, "@(#)SIB/ws/code/sib.comms.client.impl/src/com/ibm/ws/sib/comms/client/proxyqueue/impl/ShortIdAllocator.java, SIB.comms, WASX.SIB, uu1215.01 1.11");
   }

   /** Constant for null id */
   private static final short NULL_ID        = -1;
   
   /** The Max id */
   private static final short MAX_SHORT_ID   = 32767;
   
   /** The maximum id we can allocate */
   private short maxId = 32767;
   
   /** Whether we are allowing zero */
   private boolean allowZero = false;
   
   /** The next upwardly available id */
   private short nextId = NULL_ID;
   
   /** The last id that was freed */
   private short lastFreeId = NULL_ID;
   
   /** Our allocation map */
   private byte[] allocMap = null;
   
   /**
    * Creates an instance.
    * @param allowZero Is zero a permissible value for an ID?
    */
   public ShortIdAllocator(boolean allowZero)
   {
      this(allowZero, MAX_SHORT_ID);
   }

   /**
    * Creates an instance with a defined max value.
    * 
    * @param allowZero Is zero a permissible value for an ID?
    * @param maxValueToAllocate The max value that can be allocated.
    */   
   public ShortIdAllocator(boolean allowZero, short maxValueToAllocate)
   {
      if (tc.isEntryEnabled()) SibTr.entry(tc, "<init>", new Object[] {""+allowZero, ""+maxValueToAllocate});
      
      // Chuck out badgers who pump in negative numbers (or large ints)
      if (maxValueToAllocate < 0)
      {
         SIErrorException e = new SIErrorException(
            nls.getFormattedMessage("INVALID_MAX_VALUE_SICO1058", null, null)
         );
         
         FFDCFilter.processException(e, CLASS_NAME + ".<init>",
                                     CommsConstants.SHORTIDALLOCATOR_INIT_01, this);
               
         throw e;
      }
      
      this.allowZero = allowZero;
      this.maxId = maxValueToAllocate;
      
      allocMap = new byte[((maxId + 1) / 8)];
      
      if (allowZero) nextId = 0;
      else nextId = 1;
      
      if (tc.isEntryEnabled()) SibTr.exit(tc, "<init>");      
   }
   
   /**
    * Allocates an ID.
    * <p>
    * At this point we need to try and allocate an id as efficiently as possible. So, first
    * check the last freed id counter. If that was not null, allocate that one and set the last
    * freed id to null.
    * <p>
    * If the next Id is not null then allocate the next id. If that id was 32,767 then set the
    * next id to null.
    * <p>
    * If the next Id is null, then we must do a scan of the allocated ids to determine if there 
    * are any free ones available. If we find one available then we should allocate it.
    * <p>
    * If we cannot find any available then there is nothing we can do and we should throw an
    * exception.
    * 
    * @return The ID allocated.
    * 
    * @throws IdAllocatorException Thrown if there are currently no free IDs.
    */
   public synchronized short allocateId() throws IdAllocatorException
   {
      if (tc.isEntryEnabled()) SibTr.entry(tc, "allocateId");

      short nextAllocatedId = NULL_ID;
      
      // First check the last freed id
      if (lastFreeId != NULL_ID)
      {
         nextAllocatedId = lastFreeId;
         lastFreeId = NULL_ID;
      }
      // Is the next Id not null?
      else if (nextId != NULL_ID)
      {
         nextAllocatedId = nextId;
         if (nextId == maxId)
         {
            nextId = NULL_ID;
         }
         else
         {
            nextId++;
         }
      }
      // Otherwise check all the possible ids for a free one
      else
      {
         for (short x = 0; x < allocMap.length; x++)
         {
            byte oneByte = allocMap[x];
            
            // Get the first byte. If all the bits are set, then don't worry about it
            if (oneByte != (byte) 0xFF)
            {
               // Otherwise check each one to find out what id is free
               if ((oneByte & 0x80) == 0)
               {
                  // Bit 1 is not set
                  nextAllocatedId = (short) (((x + 1) * 8) - 1);
              }
               else if ((oneByte & 0x40) == 0)
               {
                  // Bit 2 is not set
                  nextAllocatedId = (short) (((x + 1) * 8) - 2);
               }
               else if ((oneByte & 0x20) == 0)
               {
                  // Bit 3 is not set
                  nextAllocatedId = (short) (((x + 1) * 8) - 3);
               }
               else if ((oneByte & 0x10) == 0)
               {
                  // Bit 4 is not set
                  nextAllocatedId = (short) (((x + 1) * 8) - 4);
               }
               else if ((oneByte & 0x8) == 0)
               {
                  // Bit 5 is not set
                  nextAllocatedId = (short) (((x + 1) * 8) - 5);
               }
               else if ((oneByte & 0x4) == 0)
               {
                  // Bit 6 is not set
                  nextAllocatedId = (short) (((x + 1) * 8) - 6);
               }
               else if ((oneByte & 0x2) == 0)
               {
                  // Bit 7 is not set
                  nextAllocatedId = (short) (((x + 1) * 8) - 7);
               }
               else if ((oneByte & 0x1) == 0)
               {
                  // Bit 8 is not set
                  nextAllocatedId = (short) (((x + 1) * 8) - 8);
                  
                  // Are we allowed zero?
                  if (nextAllocatedId == 0 && !allowZero)
                  {
                     nextAllocatedId = NULL_ID;
                  }
               }
            }

            else if (nextAllocatedId != NULL_ID) 
            {
               break;
            }
         }
      }
      
      // If we found nothing, then we have no choice but to throw an exception
      if (nextAllocatedId == NULL_ID)
      {
         throw new IdAllocatorException();
      }
      else
      {
         allocate(nextAllocatedId);
      }

      if (tc.isEntryEnabled()) SibTr.exit(tc, "allocateId", ""+nextId);
      return nextAllocatedId;
   }
   
   /**
    * Releases an ID, making it available to be allocated once more.
    * @param id The ID to release.
    * @throws IdAllocatorException Thrown if ID was not allocated
    *         in the first place.
    */
   public synchronized void releaseId(short id) throws IdAllocatorException
   {
      if (tc.isEntryEnabled()) SibTr.entry(tc, "releaseId", ""+id);

      deallocate(id);
      // If we are releasing the id 1 less than the next id, set the next id to this one
      if (id == (nextId - 1))
      {
         nextId = id;
         
         // Ensure the next free id is not this as well
         if (nextId == lastFreeId)
         {
            lastFreeId = NULL_ID;
         }
      }
      // Otherwise save this id away so we can allocate it quickly
      else
      {
         lastFreeId = id;
      }

      if (tc.isEntryEnabled()) SibTr.exit(tc, "releaseId");
   }
   
   /**
    * Private method to save away in the byte array that we have allocated the id.
    * <p>
    * The byte array is managed in such away that bits representing 0-7 are in array position 0, 
    * 8-15 are in position 1 etc. Therefore, this method will do the maths and then modify the
    * array element by setting the appropriate bit on.
    * <p>
    * The bits in each element of the byte array are big-endian. I.e., in the byte representing
    * the id's 0-7, 0 results in bit 1 (00000001 / 0x01) being set, whereas 7 results in bit 8 
    * being set (10000000 / 0x80).
    * 
    * @param id. The ID to allocate.
    * 
    * @throws IdAllocatorException if the Id is already allocated (indicates an internal error)
    */
   private void allocate(short id) throws IdAllocatorException
   {
      if (tc.isEntryEnabled()) SibTr.entry(tc, "allocate", ""+id);
      
      // Find the position in the allocation map byte array and set its bit on
      int bitToSet = (id % 8) + 1;
      int allocMapPos = (int) Math.floor((float) (id) / (float) 8);
      
      byte currentMapSetting = (byte) allocMap[allocMapPos];
      
      // Set the bit on
      if (bitToSet == 1)
      {
         if ((currentMapSetting & 0x01) != 0) throw new IdAllocatorException();
         currentMapSetting |= 0x01;
      }
      else if (bitToSet == 2)
      {
         if ((currentMapSetting & 0x02) != 0) throw new IdAllocatorException();
         currentMapSetting |= 0x02;
      }
      else if (bitToSet == 3)
      {
         if ((currentMapSetting & 0x04) != 0) throw new IdAllocatorException();
         currentMapSetting |= 0x04;
      }
      else if (bitToSet == 4)
      {
         if ((currentMapSetting & 0x08) != 0) throw new IdAllocatorException();
         currentMapSetting |= 0x08;
      }
      else if (bitToSet == 5)
      {
         if ((currentMapSetting & 0x10) != 0) throw new IdAllocatorException();
         currentMapSetting |= 0x10;
      }
      else if (bitToSet == 6)
      {
         if ((currentMapSetting & 0x20) != 0) throw new IdAllocatorException();
         currentMapSetting |= 0x20;
      }
      else if (bitToSet == 7)
      {
         if ((currentMapSetting & 0x40) != 0) throw new IdAllocatorException();
         currentMapSetting |= 0x40;
      }
      else if (bitToSet == 8)
      {
         if ((currentMapSetting & 0x80) != 0) throw new IdAllocatorException();
         currentMapSetting |= 0x80;
      }
      
      allocMap[allocMapPos] = (byte) currentMapSetting;
      
      if (tc.isEntryEnabled()) SibTr.exit(tc, "allocate");
   }
   
   /**
    * Private method to save away in the byte array that we have deallocated the id.
    * <p>
    * The byte array is managed in such away that bits representing 0-7 are in array position 0, 
    * 8-15 are in position 1 etc. Therefore, this method will do the maths and then modify the
    * array element by setting the appropriate bit off.
    * <p>
    * The bits in each element of the byte array are big-endian. I.e., in the byte representing
    * the id's 0-7, 0 results in bit 1 (00000001 / 0x01) being set, whereas 7 results in bit 8 
    * being set (10000000 / 0x80).
    * 
    * @param id. The ID to deallocate.
    * 
    * @throws IdAllocatorException if the Id was not already allocated (caused by a user free'ing
    *         an Id they did not allocate).
    */
   private void deallocate(short id) throws IdAllocatorException
   {
      if (tc.isEntryEnabled()) SibTr.entry(tc, "deallocate", ""+id);
      
      // Find the position in the allocation map byte array and set its bit on
      int bitToSet = (id % 8) + 1;
      int allocMapPos = (int) Math.floor((float) (id) / (float) 8);
      
      byte currentMapSetting = (byte) allocMap[allocMapPos];
      
      // Set the bit off by inverting the current setting, setting the bit we want to turn off 
      // on the invert and then re-inverting it back
      currentMapSetting = (byte) (currentMapSetting ^ 0xFF);
      
      // Set the bit on checking to see that the bit is already set
      if (bitToSet == 1)
      {
         if ((currentMapSetting & 0x01) != 0) throw new IdAllocatorException();
         currentMapSetting |= 0x01;
      }
      else if (bitToSet == 2)
      {
         if ((currentMapSetting & 0x02) != 0) throw new IdAllocatorException();
         currentMapSetting |= 0x02;
      }
      else if (bitToSet == 3)
      {
         if ((currentMapSetting & 0x04) != 0) throw new IdAllocatorException();
         currentMapSetting |= 0x04;
      }
      else if (bitToSet == 4)
      {
         if ((currentMapSetting & 0x08) != 0) throw new IdAllocatorException();
         currentMapSetting |= 0x08;
      }
      else if (bitToSet == 5)
      {
         if ((currentMapSetting & 0x10) != 0) throw new IdAllocatorException();
         currentMapSetting |= 0x10;
      }
      else if (bitToSet == 6)
      {
         if ((currentMapSetting & 0x20) != 0) throw new IdAllocatorException();
         currentMapSetting |= 0x20;
      }
      else if (bitToSet == 7)
      {
         if ((currentMapSetting & 0x40) != 0) throw new IdAllocatorException();
         currentMapSetting |= 0x40;
      }
      else if (bitToSet == 8)
      {
         if ((currentMapSetting & 0x80) != 0) throw new IdAllocatorException();
         currentMapSetting |= 0x80;
      }
      
      // Now invert again
      allocMap[allocMapPos] = (byte) (currentMapSetting ^ 0xFF);
      
      if (tc.isEntryEnabled()) SibTr.exit(tc, "deallocate");
   }
}
