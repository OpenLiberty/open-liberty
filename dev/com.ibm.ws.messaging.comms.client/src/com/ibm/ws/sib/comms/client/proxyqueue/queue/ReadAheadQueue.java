/*******************************************************************************
 * Copyright (c) 2004, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.comms.client.proxyqueue.queue;

import java.util.Date;
import java.util.LinkedList;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.Reliability;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.comms.CommsConstants;
import com.ibm.ws.sib.comms.client.ConsumerSessionProxy;
import com.ibm.ws.sib.comms.client.proxyqueue.AsynchConsumerProxyQueue;
import com.ibm.ws.sib.comms.client.proxyqueue.asynch.AsynchConsumerThreadPool;
import com.ibm.ws.sib.comms.client.proxyqueue.impl.ConversationHelper;
import com.ibm.ws.sib.comms.client.proxyqueue.impl.LockedMessageEnumerationImpl;
import com.ibm.ws.sib.comms.common.CommsByteBuffer;
import com.ibm.ws.sib.comms.common.CommsUtils;
import com.ibm.ws.sib.jfapchannel.approxtime.QuickApproxTime;
import com.ibm.ws.sib.jfapchannel.framework.Framework;
import com.ibm.ws.sib.mfp.JsMessage;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.SIMessageHandle;
import com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;

/**
 * A queue suitable for use with read ahead proxy queues.
 * <p>
 * The read ahead queue is designed to cache messages in advance for a consuming client. The amount
 * of messages read is determined by the value of HIGH_QUEUE_BYTES. As messages are consumed the
 * queue will ask the server for more messages when it has given to the client more than
 * HIGH_QUEUE_BYTES - LOW_QUEUE_BYTES. LOW_QUEUE_BYTES is set as a factor of HIGH_QUEUE_BYTES.
 * <p>
 * The read ahead queue will by default keep the HIGH_QUEUE_BYTES value constant. However, if the
 * QUEUE_DEPTH_THRESHOLD_FACTOR value is set, the queue will attempt to try and ensure that when
 * new messages arrive on the queue (as a result of requesting more) the queue is still of a certain
 * depth. The value is a factor of the HIGH_QUEUE_BYTES. If the current depth of the queue in bytes
 * does not match up to that value when the message first arrives, the HIGH_QUEUE_BYTES value will
 * be adjusted to try and make that level and this will be sent to the server the next time it
 * requests messages. This optimisation will only be made if the request for messages was made in
 * the last 5 secs, to prevent the client from assuming that it is incredibly slow - when in fact
 * it is just that the messages are trickling from the server.
 * <p>
 * Configurable paramters and their possible values:
 * <ul>
 *   <li>HIGH_QUEUE_BYTES - The number of bytes to cache in advance. If the alteration algorithm
 *       is enabled then this is the number of bytes cached initially.<br>
 *       Possible values are non-zero positive integers.</li>
 *   <li>LOW_QUEUE_FACTOR - This value indicates what the low watermark is set to. It is expressed
 *       as a factor of HIGH_QUEUE_BYTES. So, for example, setting this to 0.4 indicates that when
 *       the client has been given 40% of the messages on the queue, more should be requested.<br>
 *       Possible values are anything between 0 and 1.</li>
 *   <li>HIGH_QUEUE_BYTES_THRESHOLD_FACTOR - This value specifies how full the queue should be when
 *       new requsted messages start to arrive at the client expressed as a factor of the high
 *       watermark. So, for example, setting this to 0.4 indicates that the algorithm should try and
 *       ensure that when messages start arriving at the client, the queue should be 40% full. Note
 *       that you should always set this to something less than LOW_QUEUE_FACTOR :-).
 *       Possible values are anything between 0 and 1.</li>
 *   <li>HIGH_QUEUE_BYTES_MAX - The maximum amount of bytes that the client should ever cache. This
 *       is to prevent the client trying to be too over ambitious in its message caching.
 *       Possible values are non-zero positive integers.</li>
 *   <li>HIGH_QUEUE_BYTES_ALTERATION_TIMEOUT - When reading ahead from a destination where there are
 *       lots of messages to be consumed, the time it takes from requesting more messages until when
 *       they start arriving is not very long. The alteration timeout specifies the maximum amount
 *       of time to assume that the last request directly caused messages to start arriving at the
 *       client. In the case that the destination is only trickling out messages, this timeout will
 *       guard against us trying to adjust the high watermark when the destination is not giving
 *       us messages very fast.
 *       Possible values are non-zero positive integers - time is measured in milli-seconds.</li>
 * </ul>
 *
 * @author Gareth Matthews
 */
public class ReadAheadQueue implements Queue
{
   /** Class name for FFDC's */
   private static String CLASS_NAME = ReadAheadQueue.class.getName();

   /** Trace */
   private static final TraceComponent tc = SibTr.register(ReadAheadQueue.class,
                                                           CommsConstants.MSG_GROUP,
                                                           CommsConstants.MSG_BUNDLE);

   /** Log class info on load */
   static
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Source info: @(#)SIB/ws/code/sib.comms.client.impl/src/com/ibm/ws/sib/comms/client/proxyqueue/queue/ReadAheadQueue.java, SIB.comms, WASX.SIB, uu1215.01 1.63");
   }

   /** A linked list used to implement the underlying queue */
   private final LinkedList<QueueData> queue = new LinkedList<QueueData>();

   /** The current batch number for messages we are accepting */
   private short currentBatchNumber = 0;

   /** Conversation helper used to communicate with the ME that is sending this queue messages */
   private ConversationHelper convHelper;

   /** A flag to indicate whether an async thread is servicing this queue at this time */
   private boolean serviced = false;

   /** A handle on the approximate time thread, to get the current time */
   private QuickApproxTime approxTimeThread = null;

   /** The session unrecoverable reliability */
   private Reliability unrecoverableReliability = Reliability.NONE;

   private Object concAccessMonitor = new Object();

   // *** Used for pacing ***

   /** The upper watermark for the amount of bytes read ahead */
   private int HIGH_QUEUE_BYTES =
            CommsUtils.getRuntimeIntProperty(CommsConstants.RA_HIGH_QUEUE_BYTES_KEY,
                                             CommsConstants.RA_HIGH_QUEUE_BYTES);

   /** The low queue bytes multiplier */
   private static final double LOW_QUEUE_FACTOR =
            CommsUtils.getRuntimeDoubleProperty(CommsConstants.RA_LOW_QUEUE_BYTES_FACTOR_KEY,
                                                CommsConstants.RA_LOW_QUEUE_BYTES_FACTOR);

   /** The queue depth factor - setting this to 0 means that no alterations are made */
   private static final double HIGH_QUEUE_BYTES_THRESHOLD_FACTOR =
            CommsUtils.getRuntimeDoubleProperty(CommsConstants.RA_HIGH_QUEUE_THRESH_KEY,
                                                CommsConstants.RA_HIGH_QUEUE_THRESH);

   /** The theoretical maximum that HIGH_QUEUE_BYTES can ever be */
   private static final int HIGH_QUEUE_BYTES_MAX =
            CommsUtils.getRuntimeIntProperty(CommsConstants.RA_HIGH_QUEUE_BYTES_MAX_KEY,
                                             CommsConstants.RA_HIGH_QUEUE_BYTES_MAX);
   /**
    * The amount of time in ms we assume that messages arriving are directly part of a previous
    * request.
    */
   private static final int HIGH_QUEUE_BYTES_ALTERATION_TIMEOUT =
            CommsUtils.getRuntimeIntProperty(CommsConstants.RA_HIGH_QUEUE_BYTES_TO_KEY,
                                             CommsConstants.RA_HIGH_QUEUE_BYTES_TO);

   /** The lower watermark - when we have given more messages to the client, we request more */
   private int LOW_QUEUE_BYTES = (int) (HIGH_QUEUE_BYTES * LOW_QUEUE_FACTOR);

   /** A flag to indicate whether we are pacing or not */
   private boolean trackBytes;

   /** The number of bytes we have received since the last message request */
   private int bytesReceivedSinceLastRequestForMsgs = 0;

   /** The number of bytes we have given to the user since the last request */
   private int bytesGivenToUserSinceLastRequestForMsgs = 0;

   /** The total number of requests for messages */
   private int totalRequests = 0;

   /** The total number of bytes given to the user */
   private long totalBytesGiven = 0;

   /** The number of messages received */
   private long messagesReceived = 0;

   /** The number of times we became empty after a get */
   private long goneEmptyCount = 0;

   /** A counter of the number of bytes currently on the queue at this time */
   private long currentBytesOnQueue = 0;

   /** The time that the last request for messages was made */
   private long lastRequestForMessagesTime = 0;

   /** Temporary storage for the high queue bytes when we modify it */
   private int NEW_HIGH_QUEUE_BYTES = 0;

   /** Temporary storage for the low queue bytes when we modify it */
   private int NEW_LOW_QUEUE_BYTES = 0;

   /**
    * Constructor which takes the bare essentials.
    *
    * @param id
    * @param convHelper
    */
   public ReadAheadQueue(short id, ConversationHelper convHelper)
   {
      this(id, convHelper, true, null);
   }

   /**
    * Constructor which takes an unrecoverable reliability to be used when expiring read ahead
    * messages.
    *
    * @param id The unique ID to assign to this queue.
    * @param convHelper The conversation helper to use when communicating
    * with the ME sending messages to this queue.
    * @param unrecoverableReliability
    */
   public ReadAheadQueue(short id, ConversationHelper convHelper,
                         Reliability unrecoverableReliability)
   {
      this(id, convHelper, true, unrecoverableReliability);
   }

   /**
    * Constructor.
    *
    * @param id The unique ID to assign to this queue.
    * @param convHelper The conversation helper to use when communicating
    * with the ME sending messages to this queue.
    * @param track Flag to indicate whether we should track the bytes on this queue
    * @param unrecoverableReliability
    */
   public ReadAheadQueue(short id, ConversationHelper convHelper, boolean track,
                         Reliability unrecoverableReliability)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "<init>",
                                           new Object[]
                                           {
                                             ""+id,
                                             convHelper,
                                             ""+track,
                                             unrecoverableReliability
                                           });

      this.convHelper = convHelper;
      this.trackBytes = track;

      if (unrecoverableReliability != null)
      {
         this.unrecoverableReliability = unrecoverableReliability;
      }

      if (approxTimeThread == null)
      {
         approxTimeThread = Framework.getInstance().getApproximateTimeKeeper();
         approxTimeThread.setInterval(50);
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "ReadAhead Queue Startup paramters:",
                                           new Object[]
                                           {
                                              "HIGH_QUEUE_BYTES=" + HIGH_QUEUE_BYTES,
                                              "LOW_QUEUE_BYTES=" + LOW_QUEUE_BYTES,
                                              "HIGH_QUEUE_BYTES_THRESHOLD_FACTOR=" + HIGH_QUEUE_BYTES_THRESHOLD_FACTOR,
                                              "HIGH_QUEUE_BYTES_ALTERATION_TIMEOUT=" + HIGH_QUEUE_BYTES_ALTERATION_TIMEOUT,
                                              "HIGH_QUEUE_BYTES_MAX=" + HIGH_QUEUE_BYTES_MAX
                                           });

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "<init>");
   }

   /**
    * Places a message on to the front of the proxy queue so that the next get
    * operation will consume it.
    *
    * @param queueData
    * @param msgBatch
    */
   public void putToFront(QueueData queueData, short msgBatch)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "putToFront",
                                           new Object[]{queueData, msgBatch});

      _put(queueData, msgBatch, false);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "putToFront");
   }

   /**
    * Places a message onto this queue.
    *
    * @param queueData
    * @param msgBatch
    * @param putOnEnd
    */
        private void _put(QueueData queueData, short msgBatch, boolean putOnEnd)
        {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "_put",
                                           new Object[]
                                           {
                                              queueData,
                                              ""+msgBatch,
                                              ""+putOnEnd
                                           });
      synchronized(this)
      {
         if (msgBatch == currentBatchNumber)
         {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Message is for correct batch");

            // Start D214620
            // First do a check to see if we should even bother trying to alter the HIGH_QUEUE_BYTES
            // If the alteration algorithm is switched off, or if we have already reached the max
            // then don't even bother
            if (HIGH_QUEUE_BYTES_THRESHOLD_FACTOR != 0 && HIGH_QUEUE_BYTES != HIGH_QUEUE_BYTES_MAX)
            {
               // Is this the first message back?
               if (bytesReceivedSinceLastRequestForMsgs == 0)
               {
                  // Work out how full the queue is in percent
                  float queueDepthPercent = (float) currentBytesOnQueue / (float) HIGH_QUEUE_BYTES;
                  int readableQueueDepthPercent = (int) queueDepthPercent * 100;

                  // If the queue is less full than we wanted it to be, make some adjustments
                  if (queueDepthPercent < HIGH_QUEUE_BYTES_THRESHOLD_FACTOR)
                  {
                     // Did the request come in the last 5 seconds?
                     long currentTime = System.currentTimeMillis();
                     if (currentTime < (lastRequestForMessagesTime + HIGH_QUEUE_BYTES_ALTERATION_TIMEOUT))
                     {
                        // Right to the queue is less than QUEUE_DEPTH_PERCENT_THRESHOLD% full
                        // Work out how many bytes more we needed to make it
                        // QUEUE_DEPTH_PERCENT_THRESHOLD% full and then increase the HIGH_QUEUE_BYTES
                        // by that amount
                        if (NEW_HIGH_QUEUE_BYTES == 0)
                        {
                           NEW_HIGH_QUEUE_BYTES = HIGH_QUEUE_BYTES;
                           NEW_LOW_QUEUE_BYTES = LOW_QUEUE_BYTES;
                        }

                        int old_HIGH_QUEUE_BYTES = NEW_HIGH_QUEUE_BYTES;
                        // First work out how bytes we want on the queue to be at the ideal level
                        int bytesPercent = (int) (HIGH_QUEUE_BYTES_THRESHOLD_FACTOR * NEW_HIGH_QUEUE_BYTES);
                        // And now add onto HIGH_QUEUE_BYTES how many bytes we need to make the actual
                        // queue level up to that value
                        NEW_HIGH_QUEUE_BYTES = NEW_HIGH_QUEUE_BYTES + (bytesPercent - (int) currentBytesOnQueue);
                        // Ensure the value is not too high
                        if (NEW_HIGH_QUEUE_BYTES > HIGH_QUEUE_BYTES_MAX)
                        {
                           NEW_HIGH_QUEUE_BYTES = HIGH_QUEUE_BYTES_MAX;
                        }
                        // And calculate the corresponding Low watermark
                        NEW_LOW_QUEUE_BYTES = (int) (NEW_HIGH_QUEUE_BYTES * LOW_QUEUE_FACTOR);

                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "HIGH_QUEUE_BYTES altered as Queue is " +
                                                             readableQueueDepthPercent + "% full (" +
                                                             currentBytesOnQueue + "/" +
                                                             old_HIGH_QUEUE_BYTES + ")");
                     }
                     else
                     {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Messages arrived outside the alteration timeout - no alteration made (Request made at: " + lastRequestForMessagesTime + ", now: " + currentTime);
                     }
                  }
                  else
                  {
                     if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Queue depth is fine - no need to alter (" + readableQueueDepthPercent + "% full)");
                  }
               }
            }

            // Messages are stored in reverse order in the linked list (i.e. the top of the queue
            // is the end of the list. So if we are adding onto the end of the queue, we place
            // it at the start of the list.
            synchronized(queue)
            {
            	if (putOnEnd)
            	{
            		queue.addFirst(queueData);
            	}
            	// Otheriwse add it to the end.
            	else
            	{
            		queue.addLast(queueData);
            	}
            }

            // If the data added to the queue was not a chunked message, then we have received an
            // entire message - so update counters etc
            if (!queueData.isChunkedMessage())
            {
               notifyMessageReceived(queueData);
            }
         }
         else
         {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Message is not being put as the batch number (" +
                                                 msgBatch + ") != current batch number (" + currentBatchNumber + ")");
         }
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Put complete. Queue is now: " + this);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "_put");
   }

   /**
    * This method is called when a middle or final chunk of a message has been received by the
    * proxy queue. In this case we should append the chunk to those already collected and if this
    * is the last chunk we perform the processing that would normally be done when a full message
    * is received.
    *
    * @see com.ibm.ws.sib.comms.client.proxyqueue.queue.Queue#appendToLastMessage(com.ibm.ws.sib.comms.common.CommsByteBuffer, boolean)
    */
   public void appendToLastMessage(CommsByteBuffer msgBuffer, boolean lastChunk)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "appendToLastMessage",
                                           new Object[]{msgBuffer, lastChunk});

      synchronized (this)
      {
         // Get the last queue data from the queue
    	 QueueData queueData;
     	 synchronized(queue)
    	 {
    		 queueData = queue.getFirst();
         }
         queueData.addSlice(msgBuffer, lastChunk);

         // If this is the last chunk, update the counters to indicate a complete message has now
         // been received
         if (lastChunk)
         {
            notifyMessageReceived(queueData);
         }
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Append has completed: " + this);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "appendToLastMessage");
   }

   /**
    * Notification that a full message has been received. Counters should be updated.
    *
    * @param queueData
    */
   private void notifyMessageReceived(QueueData queueData)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "notifyMessageReceived", queueData);

      long messageLength = queueData.getMessageLength();
      if (trackBytes)
      {
         bytesReceivedSinceLastRequestForMsgs += messageLength;
      }

      // Update the message arrival time
      queueData.updateArrivalTime(approxTimeThread.getApproxTime());

      messagesReceived++;
      currentBytesOnQueue += messageLength;

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "notifyMessageReceived");
   }

   /**
    * Dequeues a message from this queue and returns it.
    * @return JsMessage The message de-queued, or null if the queue is empty.
    * @see Queue#get(short)
    */
   public synchronized JsMessage get(short sessionId)
      throws SIResourceException, SIConnectionDroppedException, SIConnectionLostException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "get", sessionId);

      JsMessage retValue = null;

      QueueData rhData = removeLastIfAvailable(sessionId);
      while (rhData != null) 
      {
                 
         retValue = (JsMessage) rhData.getMessage();

         // Now we need to decide whether the message has expired or not. We do this by comparing
         // the message arrival time + message TTL and the current time. If we find the message
         // has expired we need to unlock it at the server, and discard it from our queue without
         // giving it to the caller.
         // NOTE: We want to avoid the situation where we unlock an expired message but it gets to
         // the server at the exact time it did expire. As such, we give each message an extra
         // 500ms to live. This way we absolutely guarentee that when we unlock on the server the
         // message will not be redelivered.
         boolean messageHasExpired = false;
         long msgTTL = retValue.getRemainingTimeToLive();

         if (msgTTL != -1)
         {
            long currentTime = approxTimeThread.getApproxTime();
            long msgArrivalTime = rhData.getArrivalTime();

            // Compensate the TTL by 500 ms
            msgTTL += 500;

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Arrived: " + new Date(msgArrivalTime) +
                                                     ", Now: " + new Date(currentTime) +
                                                     ", Expires: " + new Date(msgArrivalTime + msgTTL));

            if (currentTime > (msgArrivalTime + msgTTL))
            {
               if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Message has expired: " +
                                                    retValue.getMessageHandle());
               messageHasExpired = true;
            }
         }

         // Now we need to update our counters and decide whether we request more messages.
         // We do this whether the message will expire or not - as either way the message is
         // coming of the queue.
         if (trackBytes)
         {
            bytesGivenToUserSinceLastRequestForMsgs += rhData.getMessageLength();
         }

         totalBytesGiven += bytesGivenToUserSinceLastRequestForMsgs;
         currentBytesOnQueue -= rhData.getMessageLength();

         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
         {
            SibTr.debug(this, tc, "Current bytes on the queue", ""+currentBytesOnQueue);
            SibTr.debug(this, tc, "Bytes given since last request", ""+bytesGivenToUserSinceLastRequestForMsgs);
         }

         // Now check and see if we need more messags. If we have gone below the lower watermark
         // of bytes cached by us (rather - if we have given the user more than HIGH - LOW messages),
         // and if the server has stopped sending us messages (coz we have received more than the
         // high watermark) then request some more messages.
         if (trackBytes &&
             (bytesGivenToUserSinceLastRequestForMsgs >= (HIGH_QUEUE_BYTES - LOW_QUEUE_BYTES)))
         {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Need more messages: " +
                                                 bytesGivenToUserSinceLastRequestForMsgs + " >= " + (HIGH_QUEUE_BYTES - LOW_QUEUE_BYTES));

            // Are there new values waiting to be set? If so, set them now
            if (NEW_HIGH_QUEUE_BYTES != 0)
            {
               HIGH_QUEUE_BYTES = NEW_HIGH_QUEUE_BYTES;
               LOW_QUEUE_BYTES = NEW_LOW_QUEUE_BYTES;
            }

            convHelper.requestMoreMessages(bytesReceivedSinceLastRequestForMsgs, HIGH_QUEUE_BYTES);   // d172528
            bytesGivenToUserSinceLastRequestForMsgs = 0;
            bytesReceivedSinceLastRequestForMsgs = 0;
            totalRequests++;

            lastRequestForMessagesTime = System.currentTimeMillis();


            synchronized(queue)
            {
         	   if (queue.isEmpty()) goneEmptyCount++;
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Queue is now: ", this);
         }

         // If the message has not expired, break out of this loop and deliver the message
         if (!messageHasExpired)
         {
            break;
         }

         // Otherwise, unlock him if he was a recoverable message
         if (CommsUtils.isRecoverable(retValue, unrecoverableReliability))
         {
            try
            {
               convHelper.unlockSet(new SIMessageHandle[] {retValue.getMessageHandle()});
            }
            catch (SIException e)
            {
               FFDCFilter.processException(e, CLASS_NAME + ".get",
                                           CommsConstants.RHPQ_GET_01, this);

               if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Unable to unlock expired message", e);

               // Not a lot else we can do that
            }
         }

         // And null out the message so we don't accidentally return him
         retValue = null;
         rhData = removeLastIfAvailable(sessionId);
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "get", retValue);
      return retValue;
   }

   /**
    * Gets a batch of several messages from the queue.
    * @param batchSize
    * @param id
    * @return JsMessage[] The batch of messages (or null if the queue is empty)
    * @throws SIResourceException
    * @throws SIConnectionDroppedException
    * @throws SIConnectionLostException
    */
   public synchronized JsMessage[] getBatch(int batchSize, short id)
      throws SIResourceException, SIConnectionDroppedException, SIConnectionLostException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getBatch", ""+batchSize);

      int size;
      synchronized(queue)
      {   
    	  size = queue.size();
      }
      if (size > batchSize) size = batchSize;

      JsMessage[] retArray = new JsMessage[size];
      for (int i=0; i < retArray.length; ++i)
         retArray[i] = get(id);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getBatch", retArray);
      return retArray;
   }

   /**
    * For read ahead queues, identical to isEmpty.
    * @see com.ibm.ws.sib.comms.client.proxyqueue.queue.Queue#isQueueEmpty()
    */
   // begin D171917
   public boolean isQueueEmpty()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "isQueueEmpty");
      boolean isEmpty;
      synchronized(queue)
      {
    	  isEmpty = queue.isEmpty();
      }
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "isQueueEmpty", isEmpty);
      return isEmpty;
   }
   // end D171917

   /**
    * This method is called on the queue when an unlockAll() is taking place. This signals that any
    * future messages will be arriving with an incremented message batch number and as such we
    * should ignore any old messages.
    * <p>
    * At this point the queue should have been emptied by calling the purge() method to ensure that
    * the queue is cleared of unlocked messages.
    *
    * @see Queue#unlockAll()
    */
   public synchronized void unlockAll()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "unlockAll");
      if (trackBytes)                                             // f191114
         ++currentBatchNumber;
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Current batch number is: " + currentBatchNumber);
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "unlockAll");
   }

   /**
    * @return Returns some status about the queue.
    */
   public String toString()
   {
      return "ReadAheadQueue@" + Integer.toHexString(hashCode()) +
             ":- CurDepth: " + queue.size() +
             ", serviced: " + serviced +                                                  // D202977
             ", messagesReceived: " + messagesReceived +                                  // D209401
             ", totalRequests: " + totalRequests +                                        // D202977
             ", goneEmptyCount: " + goneEmptyCount +                                      // D209401
             ", bytesGivenToUserSinceLastRequest: " + bytesGivenToUserSinceLastRequestForMsgs +
             ", bytesReceivedSinceLastRequest: " + bytesReceivedSinceLastRequestForMsgs +
             ", totalBytesGiven: " + totalBytesGiven +                                    // D202977
             ", totalBytesOnQueue: " + currentBytesOnQueue;                               // D214620
   }
   // End f192215

   /** @see com.ibm.ws.sib.comms.client.proxyqueue.queue.Queue#put(com.ibm.ws.sib.comms.client.proxyqueue.queue.QueueData, short) */
   public void put(QueueData queueData, short msgBatch)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "put", new Object[]{queueData, msgBatch});
      _put(queueData, msgBatch, true);
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "put");
   }

   /**
    * @see com.ibm.ws.sib.comms.client.proxyqueue.queue.Queue#deliverBatch(int, short, ConversationHelper)
    */
   public void deliverBatch(int batchSize, short sessionId, ConversationHelper currentConvHelper)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "deliverBatch",
                                           new Object[]{""+batchSize, ""+sessionId, currentConvHelper});

      Throwable result = null;
      QueueData queueData = null;
      synchronized(queue)
      {  	  
    	  if (!queue.isEmpty())
    	  {
    		  queueData = queue.get(0);
    	  }
    	  else
    	  {
    		  SIErrorException exception = new SIErrorException("Queue is empty"); 
    	      FFDCFilter.processException(exception, 
    	    		  CLASS_NAME + ".deliverBatch", 
    	    		  CommsConstants.RHPQ_DELIVER_06, 
    	    		  this);
    	  }
      }
      if(queueData!=null)
  	  {
    	  AsynchConsumerProxyQueue proxyQueue = (AsynchConsumerProxyQueue)queueData.getProxyQueue();
    	  proxyQueue.setAsynchConsumerThread(Thread.currentThread());
    	  final ConsumerSessionProxy cs = (ConsumerSessionProxy)proxyQueue.getDestinationSessionProxy();

    	  cs.resetCallbackThreadState();

    	  try
    	  {
    		  JsMessage[] msgs = getBatch(batchSize, sessionId);

    		  LockedMessageEnumerationImpl lockedMsgEnum =                                     // D218666.1
                  new LockedMessageEnumerationImpl(proxyQueue,
                                                   this,
                                                   msgs,
                                                   Thread.currentThread(),
                                                   proxyQueue.getLMEOperationMonitor());

    		  if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, " ** About to call user callback **");
    		  proxyQueue.getAsynchConsumerCallback().consumeMessages(lockedMsgEnum);
    		  if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, " ** User callback has returned  **");

    		  // Start D218666
    		  // Check the remaining message count. If it is not 0, then the user did
    		  // not view all the messages and we need to unlock them.
    		  int remainingLockedMessages = lockedMsgEnum.getRemainingMessageCount();
    		  if (remainingLockedMessages != 0)
    		  {
    			  if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "There are still " + remainingLockedMessages + " locked messages! - Unlocking them");

    			  lockedMsgEnum.unlockUnseen();                                                 // D218666.1
    		  }

    		  // End D218666
    	  }
    	  catch(Throwable t)
    	  {
    		  // Start f200337

    		  // Naughty, naughty. A user callback threw an exception. At this point, the user has
    		  // stuffed themselves a little bit as they cannot really be sure what the state of play
    		  // is - they may have unlocked some messages, they may have deleted some and some may
    		  // still be locked.
    		  // The strategy is therefore to FFDC, pass the exception to any registered connection
    		  // listeners (through their asynchronousException() method) and then call unlock all.
    		  // Any messages that have been deleted are gone, and any messages that were not consumed
    		  // will be made available for consumption again. If the user was using express messages
    		  // then they will loose any messages in the locked message enumeration.
    		  // That'll learm 'em.

    		  FFDCFilter.processException(t,
                                     CLASS_NAME + ".deliverBatch",
                                     CommsConstants.RHPQ_DELIVER_01,
                                     this);

    		  if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "exception thrown");
    		  if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(tc, "exception", t);

    		  proxyQueue.setAsynchConsumerThread(null);

    		  cs.deliverAsyncException(result);
    		  try
    		  {
    			  cs.unlockAll();
    		  }
    		  catch(SIException e)
    		  {
    			  FFDCFilter.processException(e,
    					  CLASS_NAME + ".deliverBatch",
    					  CommsConstants.RHPQ_DELIVER_03,
    					  this);
    			  if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "exception thrown");
    			  if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(tc, "exception", e);
    		  }
    	  }

    	  proxyQueue.setAsynchConsumerThread(null);

    	  try
    	  {
    		  if (cs.performInCallbackActions())
    		  {
    			  if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Starting delivery again");
    			  AsynchConsumerThreadPool.getInstance().dispatch(proxyQueue);
    		  }
    	  }
    	  catch(SIException e)
    	  {
    		  FFDCFilter.processException(e,
    				  CLASS_NAME + ".deliverBatch",
    				  CommsConstants.RHPQ_DELIVER_04,
    				  this);
    		  cs.deliverAsyncException(e);
    	  }
      }
      else
      {	           
    	  SIErrorException exception = new SIErrorException("queueData is null");    	  
          FFDCFilter.processException(exception, 
        		  CLASS_NAME + ".deliverBatch", 
        		  CommsConstants.RHPQ_DELIVER_07, 
        		  this);    	  
      }
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "deliverBatch");
   }

   /**
    * @return Returns true if and only if the queue is empty. The queue is not empty if there is at least
    *         one complete message on the queue.
    *
    * @see Queue#isEmpty(short)
    */
   public boolean isEmpty(short sessionId)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "isEmpty", ""+sessionId);
      boolean queueEmpty = false;
      synchronized (this)
      {
    	  synchronized(queue)
    	  {
    		  queueEmpty = queue.isEmpty();
    		  if (!queueEmpty)
    		  {
    			  //System.out.println("  isEmpty(): Got has data on");
    			  // The queue is not empty. Make sure there is a complete message available
    			  QueueData data = queue.getLast();
    			  //System.out.println("  isEmpty(): First item: " + data);
    			  // If the data is not complete, the queue is still regarded as empty
    			  queueEmpty = !data.isComplete();
    			  //System.out.println("  ** isEmpty(): " + queueEmpty);
    		  }
          }
      }
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "isEmpty", ""+queueEmpty);
      return queueEmpty;
   }

   /**
    * @see com.ibm.ws.sib.comms.client.proxyqueue.queue.Queue#purge(short)
    */
   public synchronized void purge(short sessionId)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "purge", ""+sessionId);

      synchronized(queue)
      {
    	  queue.clear();
      }
      bytesGivenToUserSinceLastRequestForMsgs = 0;
      bytesReceivedSinceLastRequestForMsgs = 0;

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "purge");
   }

   /**
    * @see com.ibm.ws.sib.comms.client.proxyqueue.queue.Queue#getConcurrentAccessLock()
    */
   public Object getConcurrentAccessLock()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getConcurrentAccessLock");
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getConcurrentAccessLock", concAccessMonitor);
      return concAccessMonitor;
   }

   // Start F247845
   /**
    * Sets the trackBytes parameter. This flag is used to indicate whether this queue should keep
    * track of the bytes on the queue and request more from the server when it is running low.
    *
    * @param trackBytes
    */
   public synchronized void setTrackBytes(boolean trackBytes)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "setTrackBytes", trackBytes);
      this.trackBytes = trackBytes;
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "setTrackBytes");
   }
   // End F247845

   /** @see Queue#waitUntilEmpty() */
   public void waitUntilEmpty(final short sessionId)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "waitUntilEmpty");
      // No-op for read ahead queues.
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "waitUntilEmpty");
   }
   
   private QueueData removeLastIfAvailable(short sessionId) 
   {
	   if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "removeLastIfAvailable");
	   QueueData qData = null;
	   synchronized(this)
	   {
		   synchronized(queue)
		   {
			   if (!isEmpty(sessionId))
			   {
				   qData =  queue.removeLast();
			   }
		   }
	   }
	   if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "removeLastIfAvailable");
	   return qData;
	}
}
