/*******************************************************************************
 * Copyright (c) 2004, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.jfapchannel.impl;

import java.util.NoSuchElementException;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.jfapchannel.Conversation;
import com.ibm.ws.sib.jfapchannel.JFapByteBuffer;
import com.ibm.ws.sib.jfapchannel.JFapChannelConstants;
import com.ibm.ws.sib.jfapchannel.SendListener;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.ws.util.ObjectPool;

public class TransmissionDataIterator
{
   private static final TraceComponent tc = SibTr.register(TransmissionDataIterator.class, JFapChannelConstants.MSG_GROUP, JFapChannelConstants.MSG_BUNDLE);

   static   
   {
      if (tc.isDebugEnabled()) SibTr.debug(tc, "@(#) SIB/ws/code/sib.jfapchannel.client.common.impl/src/com/ibm/ws/sib/jfapchannel/impl/TransmissionDataIterator.java, SIB.comms, WASX.SIB, uu1215.01 1.19");
   }     

   // Pool for instances of this class.
   private static ObjectPool pool = new ObjectPool("Transmission Data Iterator Pool", 100);
   
   // Transmission data object that this iterator will always return for its values.
   // By caching this object (and requiring users of the iterator not to hold on to
   // the values returned) we avoid excessive object allocations.
   private final TransmissionData xmitData = new TransmissionData(this);                     // D226242
   
   // Connection that the data returned by this iterator will be transmitted over.   
   private Connection connection;
      
   // Does this iterator have any transmissions remaining.  Determins the value returned
   // from the hasNext() method.
   private boolean transmissionsRemaining;
   
   // Layout for next piece of data returned by iterator.
   private JFapChannelConstants.TransmissionLayout layout;
   
   // Piece of data being sent.  This will become the payload of the one or more
   // transmissions returned by this iterator.
//   private WsByteBuffer[] data;
   private JFapByteBuffer buffer;
   
   // Priority for transmissions in this iterator.
   private int priority;
   
   // Are the buffers used by the transmissions in this iterator taken from pooled
   // storage?
   private boolean isPooled;
   
   // Does the data being built into one or more transmissions by this iterator represent
   // data being exchanged synchronously between peers?
   private boolean isExchange;
   
   // Segment type initially assigned to this piece of data by the person requesting the
   // data is transmitted.  This may differ from the segment type returned for an individual
   // transmission if the transmission has been segmented.
   private int segmentType;
   
   // The conversation id associated with the data being transmitted (if any)
   private int conversationId;
   
   // The request number associated with the data being transmitted (if any).  This
   // is omitted if the transmission has a primary header only.
   private int requestNumber;
   
   // The conversation associated with the data being transmitted (if any).
   private Conversation conversation;
   
   // The send listener which should be notified when all the data has been transmitted.
   // In the case of segmented messages this will be notified only when the last segment
   // has been sent.
   private SendListener sendListener;
   
   // Is this transmission intended to shut down our capacity to send?
   private boolean isTerminal;
   
   // The size of the data to be transmitted (ignoring all headers).
   private int size;

   // Number of bytes as yet untransmitted.
   private int bytesRemaining;
      
   /** 
    * Private constructor - prevents others from instantiating instances of this class.
    * Enforces use of pooling mechanism.
    */
   private TransmissionDataIterator() {}
   
   /**
    * Helper method which sets up fields in this class.
    * @param connection
    * @param buffer
    * @param priority
    * @param isPooled
    * @param isExchange
    * @param segmentType
    * @param conversationId
    * @param requestNumber
    * @param conversation
    * @param sendListener
    * @param isTerminal
    * @param size
    */
   private void setFields(Connection connection,
                          JFapByteBuffer buffer,
                          int priority,
                          boolean isPooled,
                          boolean isExchange,
                          int segmentType,
                          int conversationId,
                          int requestNumber,
                          Conversation conversation,
                          SendListener sendListener,
                          boolean isTerminal,
                          int size)
   {
      this.connection = connection;
      this.buffer = buffer;
      this.priority = priority;
      this.isPooled = isPooled;
      this.isExchange = isExchange;
      this.segmentType = segmentType;
      this.conversationId = conversationId;
      this.requestNumber = requestNumber;
      this.conversation = conversation;
      this.sendListener = sendListener;
      this.isTerminal = isTerminal;
      this.size = size;
      bytesRemaining = size;
   }
      
   /**
    * Resets the iterator so it is read for use with a new piece of user data.
    * @param connection
    * @param data
    * @param priority
    * @param isPooled
    * @param isExchange
    * @param segmentType
    * @param conversationId
    * @param requestNumber
    * @param conversation
    * @param sendListener
    * @param isUserRequest
    * @param isTerminal
    * @param size
    */                  
   private void reset(Connection connection,
                      JFapByteBuffer buffer,
                      int priority,
                      boolean isPooled,
                      boolean isExchange,
                      int segmentType,
                      int conversationId,
                      int requestNumber,
                      Conversation conversation,
                      SendListener sendListener,
                      boolean isTerminal,
                      int size)
   {
      if (tc.isEntryEnabled()) SibTr.entry(this, tc, "reset", new Object[]{connection, buffer, ""+priority, ""+isPooled, ""+isExchange, ""+segmentType, ""+conversationId, ""+requestNumber, conversation, sendListener, ""+isTerminal, ""+size});
      setFields(connection, buffer, priority, isPooled, isExchange, segmentType, conversationId, requestNumber, conversation, sendListener, isTerminal, size);
      
      int sizeIncludingHeaders = size + 
                                 JFapChannelConstants.SIZEOF_PRIMARY_HEADER +
                                 JFapChannelConstants.SIZEOF_CONVERSATION_HEADER;      
      
      transmissionsRemaining = true;
      
      if (sizeIncludingHeaders > connection.getMaxTransmissionSize())
      {
         if (tc.isDebugEnabled()) SibTr.debug(this, tc, "segmenting");
         layout = JFapChannelConstants.XMIT_SEGMENT_START;
      }
      else
         layout = JFapChannelConstants.XMIT_CONVERSATION;
      if (tc.isEntryEnabled()) SibTr.exit(this, tc, "reset");
   }

   /**
    * Returns true if this iterator contains more transmission data objects.
    * @return Returns true if this iterator contains more transmission data objects.
    */
   public boolean hasNext()
   {
      if (tc.isEntryEnabled()) SibTr.entry(this, tc, "hasNext");
      if (tc.isEntryEnabled()) SibTr.exit(this, tc, "hasNext", ""+transmissionsRemaining);
      return transmissionsRemaining;
   }

   /* (non-Javadoc)
    * @see java.util.Iterator#next()
    */
   public TransmissionData next()
   {
      if (tc.isEntryEnabled()) SibTr.entry(this, tc, "next");
      if (!transmissionsRemaining) throw new NoSuchElementException();
      
      xmitData.reset(buffer, isTerminal, connection);
      if (tc.isDebugEnabled()) SibTr.debug(this, tc, "layout="+layout);
      if (layout == JFapChannelConstants.XMIT_PRIMARY_ONLY)
      {
         int segmentLength = size + JFapChannelConstants.SIZEOF_PRIMARY_HEADER;
         xmitData.setLayoutToPrimary(segmentLength, 
                                     priority, 
                                     isPooled, 
                                     isExchange, 
                                     connection.getNextRequestNumber(), 
                                     segmentType, 
                                     sendListener);
         bytesRemaining = 0;
         layout = JFapChannelConstants.XMIT_LAYOUT_UNKNOWN;
         transmissionsRemaining = false;
      }
      else if (layout == JFapChannelConstants.XMIT_CONVERSATION)
      {
         int segmentLength = size + 
                             JFapChannelConstants.SIZEOF_PRIMARY_HEADER +
                             JFapChannelConstants.SIZEOF_CONVERSATION_HEADER;
         xmitData.setLayoutToConversation(segmentLength, 
                                          priority, 
                                          isPooled, 
                                          isExchange, 
                                          connection.getNextRequestNumber(), 
                                          segmentType,
                                          conversationId,
                                          requestNumber,
                                          conversation, 
                                          sendListener);
         bytesRemaining = 0;
         layout = JFapChannelConstants.XMIT_LAYOUT_UNKNOWN;
         transmissionsRemaining = false;
      }
      else if (layout == JFapChannelConstants.XMIT_SEGMENT_START)
      {
         int segmentLength = connection.getMaxTransmissionSize(); 
         xmitData.setLayoutToStartSegment(segmentLength,
                                          priority,
                                          isPooled,
                                          isExchange,
                                          connection.getNextRequestNumber(),
                                          segmentType,
                                          conversationId,                                          
                                          requestNumber,
                                          size);
         bytesRemaining -= 
            (connection.getMaxTransmissionSize() - (JFapChannelConstants.SIZEOF_PRIMARY_HEADER      +
                                                    JFapChannelConstants.SIZEOF_CONVERSATION_HEADER +
                                                    JFapChannelConstants.SIZEOF_SEGMENT_START_HEADER));

         int maxPayloadInNextTransmission =
            connection.getMaxTransmissionSize() - (JFapChannelConstants.SIZEOF_PRIMARY_HEADER +
                                                   JFapChannelConstants.SIZEOF_CONVERSATION_HEADER);
                                                   
         if (bytesRemaining <= maxPayloadInNextTransmission)
            layout = JFapChannelConstants.XMIT_SEGMENT_END;
         else
            layout = JFapChannelConstants.XMIT_SEGMENT_MIDDLE;                                                    
      }     
      else if (layout == JFapChannelConstants.XMIT_SEGMENT_MIDDLE)
      {
         int segmentLength = connection.getMaxTransmissionSize();
         xmitData.setLayoutToMiddleSegment(segmentLength,
                                           priority,
                                           isPooled,
                                           isExchange,
                                           connection.getNextRequestNumber(),
                                           conversationId,
                                           requestNumber);
         bytesRemaining -= 
            connection.getMaxTransmissionSize() - (JFapChannelConstants.SIZEOF_PRIMARY_HEADER      +
                                                   JFapChannelConstants.SIZEOF_CONVERSATION_HEADER);

         int maxPayloadInNextTransmission =
            connection.getMaxTransmissionSize() - (JFapChannelConstants.SIZEOF_PRIMARY_HEADER +
                                                   JFapChannelConstants.SIZEOF_CONVERSATION_HEADER);
                                                   
         if (bytesRemaining <= maxPayloadInNextTransmission)
            layout = JFapChannelConstants.XMIT_SEGMENT_END;
      }
      else if (layout == JFapChannelConstants.XMIT_SEGMENT_END)
      {
         int segmentLength = bytesRemaining +
                             JFapChannelConstants.SIZEOF_PRIMARY_HEADER +
                             JFapChannelConstants.SIZEOF_CONVERSATION_HEADER;
         xmitData.setLayoutToEndSegment(segmentLength,
                                        priority,
                                        isPooled,
                                        isExchange,
                                        connection.getNextRequestNumber(),
                                        conversationId,
                                        requestNumber,
                                        conversation,
                                        sendListener);
         bytesRemaining = 0;
         layout = JFapChannelConstants.XMIT_LAYOUT_UNKNOWN;
         transmissionsRemaining = false;
      }
      else
      {
         if (tc.isDebugEnabled()) SibTr.debug(this, tc, "invalid layout: "+layout);
      }

      if (tc.isEntryEnabled()) SibTr.exit(this, tc, "next", xmitData);
      return xmitData;
   }

   /** Allocates an instance of this class from a pool */
   protected static TransmissionDataIterator allocateFromPool(Connection connection,
                                                              JFapByteBuffer buffer,
                                                              int priority,
                                                              boolean isPooled,
                                                              boolean isExchange,
                                                              int segmentType,
                                                              int conversationId,
                                                              int requestNumber,
                                                              Conversation conversation,
                                                              SendListener sendListener,
                                                              boolean isTerminal,
                                                              int size)
   {
      if (tc.isEntryEnabled()) SibTr.entry(tc, "allocateFromPool");
      TransmissionDataIterator retValue = (TransmissionDataIterator)pool.remove();
      if (retValue == null)
         retValue = new TransmissionDataIterator();
      retValue.reset(connection, buffer, priority, isPooled, isExchange, segmentType, conversationId, requestNumber, conversation, sendListener, isTerminal, size);
      if (tc.isEntryEnabled()) SibTr.exit(tc, "allocateFromPool", retValue);
      return retValue;
   }
   

   /** Returns a previously allocated instance of this class to the pool */
   protected void release()
   {
      if (tc.isEntryEnabled()) SibTr.entry(this, tc, "release");
      if (!transmissionsRemaining)
      {
         if (tc.isDebugEnabled()) SibTr.debug(this, tc, "no more transmissions remaining - repooling");
         
         // Ensure we release the byte buffers back into the pool
         if (buffer != null)
         {
            buffer.release();
         }
         
         connection = null;
         conversation = null;
         buffer = null;
         sendListener = null;
         pool.add(this);
      }
      if (tc.isEntryEnabled()) SibTr.exit(this, tc, "release");
   }
      
   /** Returns the priority for data transmissions in this iterator */
   protected int getPriority()
   {
      if (tc.isEntryEnabled()) SibTr.entry(this, tc, "getPriority");
      if (tc.isEntryEnabled()) SibTr.exit(this, tc, "getPriority", ""+priority);
      return priority;
   }
   
   /** 
    * Sets the priority for data transmissions in this iterator.  This is
    * used when data is enqueued to the priority queue with the "lowest available"
    * option set - and we need to assigne it a hard priority
    */
   protected void setPriority(int priority)
   {
      if (tc.isEntryEnabled()) SibTr.entry(this, tc, "setPriority", ""+priority);
      this.priority = priority;
      if (tc.isEntryEnabled()) SibTr.exit(this, tc, "setPriority");      
   }
   
   /**
    * Returns the size of the payload data the user requested tranmitted.
    * @return Returns the size of the payload data the user requested tranmitted.
    */
   protected int getSize()
   {      
      if (tc.isEntryEnabled()) SibTr.entry(this, tc, "getSize");
      if (tc.isEntryEnabled()) SibTr.exit(this, tc, "getSize", ""+size);
      return size;
   }
}
