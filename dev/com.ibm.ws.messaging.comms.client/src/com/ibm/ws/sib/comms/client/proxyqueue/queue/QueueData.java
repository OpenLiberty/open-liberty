/*******************************************************************************
 * Copyright (c) 2004, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.comms.client.proxyqueue.queue;

import java.util.ArrayList;
import java.util.List;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.comms.CommsConstants;
import com.ibm.ws.sib.comms.client.proxyqueue.ProxyQueue;
import com.ibm.ws.sib.comms.common.CommsByteBuffer;
import com.ibm.ws.sib.mfp.impl.JsMessageFactory;
import com.ibm.ws.sib.utils.DataSlice;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.SIBusMessage;

/**
 * This class encapsulates the buffer that is stored on proxy queues.
 * 
 * @author prestona
 */
public class QueueData
{
   /** Class name for FFDC's */
   private static String CLASS_NAME = QueueData.class.getName();
   
   /** Trace handle */
   private static final TraceComponent tc = SibTr.register(QueueData.class, 
                                                           CommsConstants.MSG_GROUP, 
                                                           CommsConstants.MSG_BUNDLE);

   private ProxyQueue proxyQueue;
   private boolean lastInBatch;
   private CommsByteBuffer buffer;
   private long messageLength = 0;
   private boolean chunkedMessage = false;
   private boolean complete = false;
   private long arrivalTime = 0;
   private List<DataSlice> slices = new ArrayList<DataSlice>();

   /**
    * @param proxyQueue The proxy queue this data is queued on.
    * @param lastInBatch Flag to indicate that this message is the last in a batch.
    * @param chunk This flag indicates that the following buffer only contains a message slice
    *              rather than the entire message.
    * @param buffer The CommsByteBuffer of the message. The buffer should be positioned at the start
    *               of the message (i.e. the BIT64 message length followed by the JMO) or message
    *               chunk (i.e. the BIT32 chunk length followed by the chunk).
    */
   public QueueData(ProxyQueue proxyQueue, boolean lastInBatch, boolean chunk, CommsByteBuffer buffer)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "<init>", new Object[]{proxyQueue, lastInBatch, chunk, buffer});
      
      this.proxyQueue = proxyQueue;
      this.lastInBatch = lastInBatch;
      
      // If this is a non-chunked message, then simply save away the entire message
      if (!chunk)
      {
         this.buffer = buffer;
         // This is a hack - the message length is the data left in the buffer minus 8 bytes of
         // message length.
         this.messageLength = buffer.peekLong();
         complete = true;
      }
      // Otherwise save the slice
      else
      {
         slices.add(buffer.getDataSlice());
         chunkedMessage = true;
      }
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "<init>");
   }
   
   /**
    * @return Returns the message from this queue data object.
    * @throws SIResourceException if the message cannot be recreated from the buffer.
    */
   public synchronized SIBusMessage getMessage() throws SIResourceException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getMessage");
      
      SIBusMessage msg = null;
      
      // If the buffer was not null, the buffer represents an entire message
      if (buffer != null)
      {
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Recreating message from:", buffer);
         msg = buffer.getMessage(null);
      }
      // Otherwise we need to assumble the message from slices
      else
      {
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Recreating message from:", slices);
         try
         {
            msg = JsMessageFactory.getInstance().createInboundJsMessage(slices);
         }
         catch (Exception e)
         {
            FFDCFilter.processException(e, CLASS_NAME + ".getMessage",
                                        CommsConstants.QUEUEDATA_GETMESSAGE_01,
                                        new Object[] { slices, this });
            
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Unable to recreate message from slices", e);
            throw new SIResourceException(e);
         }
      }
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getMessage", msg);
      return msg;
   }
   
   /**
    * @return Returns the lastInBatch.
    */
   public boolean isLastInBatch()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "isLastInBatch");
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "isLastInBatch", lastInBatch);
      return lastInBatch;
   }
   
   /**
    * @return Returns the proxyQueue.
    */
   public ProxyQueue getProxyQueue()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getProxyQueue");
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getProxyQueue", proxyQueue);
      return proxyQueue;
   }
   
   /**
    * @return Returns the message length of the serialized message.
    */
   public synchronized long getMessageLength()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getMessageLength");
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getMessageLength", messageLength);
      return messageLength;
   }
   
   /**
    * This method will add a data slice to the list of slices for this message.
    * 
    * @param bufferContainingSlice
    * @param last
    */
   public synchronized void addSlice(CommsByteBuffer bufferContainingSlice, boolean last)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "addSlice", new Object[]{bufferContainingSlice, last});
      
      slices.add(bufferContainingSlice.getDataSlice());
      
      // If this is the last slice, calculate the message length
      if (last) 
      {
         for (DataSlice slice : slices)
         {
            messageLength += slice.getLength();
         }
         complete = true;
      }
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Message now consists of " + slices.size() + " slices");
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "addSlice");
   }
   
   /**
    * @return Returns the chunkedMessage.
    */
   public boolean isChunkedMessage()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "isChunkedMessage");
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "isChunkedMessage", chunkedMessage);
      return chunkedMessage;
   }

   /**
    * @return Returns the complete.
    */
   public synchronized boolean isComplete()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "isComplete");
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "isComplete", complete);
      return complete;
   }
   
   /**
    * Used to update the message arrival time for this data.
    * @param messageArrivalTime
    */
   public void updateArrivalTime(long messageArrivalTime)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "updateMessageArrivalTime", messageArrivalTime);
      this.arrivalTime = messageArrivalTime;
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "updateMessageArrivalTime");
   }
   
   /**
    * @return Returns the message arrival time if it has been set into the data.
    */
   public long getArrivalTime()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getMessageArrivalTime");
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getMessageArrivalTime", arrivalTime);
      return arrivalTime;
   }

   /**
    * @return Returns info about this object
    */
   public String toString()
   {
      return "QueueData@" + Integer.toHexString(System.identityHashCode(this)) + 
             " {buffer=" + buffer + ", " + 
             " slices=" + slices + ", " +
             " chunked=" + chunkedMessage + ", " + 
             " complete=" + complete + ", " + 
             " messageLength=" + messageLength + ", " +
             " lastInbatch=" + lastInBatch + ", " + 
             " arrivalTime=" + arrivalTime + ", " +
             " proxyQueue=" + proxyQueue + "}";
             
   }
}
