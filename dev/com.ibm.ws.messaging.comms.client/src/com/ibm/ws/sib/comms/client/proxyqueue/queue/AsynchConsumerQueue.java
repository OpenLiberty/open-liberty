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

package com.ibm.ws.sib.comms.client.proxyqueue.queue;

import java.util.Iterator;
import java.util.LinkedList;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.comms.CommsConstants;
import com.ibm.ws.sib.comms.client.ConsumerSessionProxy;
import com.ibm.ws.sib.comms.client.proxyqueue.AsynchConsumerProxyQueue;
import com.ibm.ws.sib.comms.client.proxyqueue.ProxyQueue;
import com.ibm.ws.sib.comms.client.proxyqueue.impl.ConversationHelper;
import com.ibm.ws.sib.comms.client.proxyqueue.impl.LockedMessageEnumerationImpl;
import com.ibm.ws.sib.comms.common.CommsByteBuffer;
import com.ibm.ws.sib.mfp.JsMessage;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;

/**
 * A queue which exhibits suitable behaviour for a asynchronous consumer
 * proxy queue.
 */
public class AsynchConsumerQueue implements Queue
{
   /** Class name for FFDC's */
   private static String CLASS_NAME = AsynchConsumerQueue.class.getName();

   /** Trace */
   private static final TraceComponent tc = SibTr.register(AsynchConsumerQueue.class,
                                                           CommsConstants.MSG_GROUP,
                                                           CommsConstants.MSG_BUNDLE);

   /** NLS handle */
   private static final TraceNLS nls = TraceNLS.getTraceNLS(CommsConstants.MSG_BUNDLE);

   /** Log class info on load */
   static
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Source info: @(#) SIB/ws/code/sib.comms.client.impl/src/com/ibm/ws/sib/comms/client/proxyqueue/queue/AsynchConsumerQueue.java, SIB.comms, WASX.SIB, uu1215.01 1.44");
   }

   /** Set to true this is an ordered queue where multiple sessions are reading from this queue */
   private boolean ordered = false;

   /** The number of batches that are ready */
   private int batchesReady = 0;

   /** The list which is used to implement the underlying queue */
   private LinkedList<QueueData> queue = new LinkedList<QueueData>();

   /** The number of batches we have received */
   private long batchesReceived = 0;                                                      // D209401

   /** The number of messages received */
   private long messagesReceived = 0;                                                     // D209401

   private Object concurrentAccessLock = new Object();

   /**
    * Creates a new asynchronous consumer proxy queue.
    * @param ordered Whether this queue is an ordered queue.
    */
   public AsynchConsumerQueue(boolean ordered)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "<init>", ""+ordered);
      this.ordered = ordered;
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "<init>");
   }

   /**
    * Places a message onto the queue.
    *
    * @param queueData
    *           The data which comprises the message to queue.
    * @param msgBatch
    *           The message batch number.
    */
   public void put(QueueData queueData, short msgBatch) // f200337
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "put", new Object[] {queueData, ""+msgBatch});

      synchronized(this)
      {
         if (!ordered && batchesReady == 1)
         {
            // A non-ordered async consumer proxy queue only caches one batch at a time. Therefore
            // if we are trying to put messages on when there is a batch already ready, we
            // messed up.
            SIErrorException e = new SIErrorException(
               nls.getFormattedMessage("ASYNC_BATCH_ALREADY_READY_SICO1031", null, null)
            );

            FFDCFilter.processException(e, CLASS_NAME + ".put",
                                        CommsConstants.ASYNCHPQ_PUT_01, this);

            throw e;
         }

         queue.addLast(queueData);

         // If the data added to the queue was not a chunked message, then we have received an
         // entire message - so update counters etc
         if (!queueData.isChunkedMessage())
         {
            notifyMessageReceived(queueData.isLastInBatch());
         }

      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Put has completed: " + this);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "put");
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
         QueueData queueData = queue.getLast();
         queueData.addSlice(msgBuffer, lastChunk);

         // If this is the last chunk, update the counters to indicate a complete message has now
         // been received
         if (lastChunk)
         {
            notifyMessageReceived(queueData.isLastInBatch());
         }
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Append has completed: " + this);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "appendToLastMessage");
   }

   /**
    * Notification that a full message has been received. Counters should be updated.
    *
    * @param lastInBatch
    */
   private void notifyMessageReceived(boolean lastInBatch)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "notifyMessageReceived", lastInBatch);

      if (lastInBatch)
      {
         batchesReceived++;
         batchesReady++;
      }

      messagesReceived++;

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "notifyMessageReceived");
   }

   /**
    * This method will check and see if the queue is 'empty' - i.e. checks to see if messages are
    * ready to be consumed from it. This method takes a sessionId parameter as it is possible that
    * multiple sessions are using this queue and the queue may appear empty to one when messages
    * are ready (not-empty) for another session.
    * <p>
    * So the checks are:
    * <ul>
    *   <li>Has the underlying queue got 0 items on it? If yes, we are empty.</li>
    *   <li>Does the next item of data on the queue belong to this session Id? If it does and the
    *       queue contains at least one batch, we are _not_ empty.
    * </ul>
    *
    * @param sessionId The session Id to check
    *
    * @return true if there are messages ready to be consumed by this session Id,
    *         otherwise false.
    *
    * @see com.ibm.ws.sib.comms.client.proxyqueue.queue.Queue#isEmpty(short)
    */
   public synchronized boolean isEmpty(short sessionId)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "isEmpty","sessionId="+sessionId);

      boolean isEmpty = true;

      // If the underlying queue has no items on it, we are undoubtably empty
      if (!isQueueEmpty())
      {
         // If not, check the next item on the queue and see if it belongs to us. If it does and
         // a complete batch is ready - nice
         QueueData data = queue.getFirst();
         if (data.getProxyQueue().getId() == sessionId && batchesReady > 0)
         {
            isEmpty = false;
         }
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "isEmpty", ""+isEmpty);
      return isEmpty;
   }

   /**
    * @return Returns true if there is anything at all on the underlying queue.  Even an
    * incomplete batch of messages.
    *
    * @see com.ibm.ws.sib.comms.client.proxyqueue.queue.Queue#isQueueEmpty()
    */
   public synchronized boolean isQueueEmpty()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "isQueueEmpty");
      boolean result = queue.isEmpty();
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "isQueueEmpty", ""+result);
      return result;
   }

   /**
    * This method will purge messages from the  queue that belong to a particular session Id.
    *
    * @param sessionId The session Id
    */
   public synchronized void purge(short sessionId)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "purge", ""+sessionId);

      // We have to travese the queue and look at all the entries for this
      int[] indexsToDelete = new int[queue.size()];
      int indexCount = 0;
      boolean completeBatchRemoved = false;

      // First build a list of all the items to delete
      for (int x = 0; x < queue.size(); x++)
      {
         QueueData data = queue.get(x);

         // Get the session id. If it matches the one passed in, remove the message
         if (data.getProxyQueue().getId() == sessionId)
         {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Deleting item: " + data);
            indexsToDelete[indexCount++] = x;
            completeBatchRemoved = data.isLastInBatch();
         }
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Removing " + indexCount + " entries");

      // Now perform the deletes in reverse order
      for (int x = (indexCount - 1); x >= 0; x--)
      {
         queue.remove(indexsToDelete[x]);
      }

      // If we removed a complete batch, decrement the counter
      if (completeBatchRemoved) batchesReady--;

      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Purge completed. Queue is now: " + this);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "purge");
   }

   /**
    * @return Returns a String summarizing ourselves
    */
   public String toString()
   {
      return "AsyncQueue@" + Integer.toHexString(hashCode()) +
             ":- CurDepth: " + queue.size() +
             ", ordered: " + ordered +
             ", messagesReceived: " + messagesReceived +                                  // D209401
             ", batchesReceived: " + batchesReceived +                                    // D209401
             ", batchesReady: " + batchesReady;
   }

   /**
    * @see com.ibm.ws.sib.comms.client.proxyqueue.queue.Queue#getConcurrentAccessLock()
    */
   public Object getConcurrentAccessLock()
   {
      return concurrentAccessLock;
   }

   /**
    * Gets a batch of messages from the queue.
    *
    * @param batchSize
    * @param convHelper
    *
    * @return Returns an array of JsMessage objects
    */
   private synchronized JsMessage[] getBatch(int batchSize, ConversationHelper convHelper)
      throws SIResourceException, SIConnectionDroppedException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getBatch",
                                           new Object[]{batchSize, convHelper});

      if (batchesReady == 0)
      {
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "No batches are ready!!");

         // We are getting a batch of messages from the queue but there are none ready. Oops.
         SIErrorException e = new SIErrorException(
            nls.getFormattedMessage("ASYNC_BATCH_NOT_READY_SICO1033", null, null)
         );

         FFDCFilter.processException(e, CLASS_NAME + ".getBatch",
                                     CommsConstants.ASYNCHPQ_GETBATCH_01, this);

         throw e;
      }

      int currentSessionId = 0;
      JsMessage[] msg = new JsMessage[batchSize];
      QueueData data = null;

      for (int i=0; i<msg.length; ++i)
      {
         data = queue.removeFirst();
         currentSessionId = data.getProxyQueue().getId();
         msg[i] = (JsMessage) data.getMessage();

         // Is this the last bit of data? If so - out we go
         if (data.isLastInBatch()) break;
      }
      batchesReady--;

      // Have a quick check and see if there is any data left on the queue. If there is, it is
      // probably for another session and we should therefore transistion that session into
      // delivery by calling batch()
      if (!isQueueEmpty())
      {
         QueueData qd = queue.get(0);
         AsynchConsumerProxyQueue pq = (AsynchConsumerProxyQueue)qd.getProxyQueue();

         if (pq.getId() != currentSessionId)
         {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Next data on the queue is for a different session:", pq);
            pq.nudge();
         }
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "getBatch", msg);
      return msg;
   }

   /**
    * This method will deliver a batch of messages to the async callback.
    *
    * @param batchSize
    * @param sessionId
    * @param convHelper
    */
   public void deliverBatch(int batchSize, short sessionId, ConversationHelper convHelper)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "deliverBatch",
                                           new Object[]
                                           {
                                             ""+batchSize,
                                             ""+sessionId,
                                             convHelper
                                           });

      QueueData queueData = queue.get(0);
      AsynchConsumerProxyQueue proxyQueue = (AsynchConsumerProxyQueue)queueData.getProxyQueue();
      proxyQueue.setAsynchConsumerThread(Thread.currentThread());
      final ConsumerSessionProxy cs = (ConsumerSessionProxy)proxyQueue.getDestinationSessionProxy();

      cs.resetCallbackThreadState();

      boolean unlockAllMessages = false;
      try
      {
         JsMessage[] msgs = getBatch(batchSize, convHelper);

         LockedMessageEnumerationImpl lockedMsgEnum =                                     // D218666.1
                  new LockedMessageEnumerationImpl(proxyQueue,
                                                   this,
                                                   msgs,
                                                   Thread.currentThread(),
                                                   proxyQueue.getLMEOperationMonitor());

         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, " ** About to call user callback **");
         proxyQueue.getAsynchConsumerCallback().consumeMessages(lockedMsgEnum);
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, " ** User callback has returned  **");

         // Check the remaining message count. If it is not 0, then the user did
         // not view all the messages and we need to unlock them.
         int remainingLockedMessages = lockedMsgEnum.getRemainingMessageCount();
         if (remainingLockedMessages != 0)
         {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "There are still " + remainingLockedMessages + " locked messages! - Unlocking them");

            lockedMsgEnum.unlockUnseen();                                                 // D218666.1
         }
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
                                     CommsConstants.ASYNCHPQ_DELIVER_01,
                                     this);

         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "exception thrown");
         if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(tc, "exception", t);

         proxyQueue.setAsynchConsumerThread(null);

         // Pass the exception to any registered exception listeners
         cs.deliverAsyncException(t);

         // We need to purge our local proxy queue, and unlock all the
         // messages in the ME to redeliver the messages.
         // However, we cannot do this before we act on any stop
         // that might have been called within the asynch consumer
         // before they threw the exception, otherwise the ME
         // will automatically send us the next batch.
         unlockAllMessages = true;
      }

      proxyQueue.setAsynchConsumerThread(null);

      concurrentAccessLock.notifyAll();

      try
      {
         if (cs.performInCallbackActions())
         {
            // Only request the next batch of messages from the messaging engine if the async consumer session
            // is started and is not in the process of stopping, and we are not
            // going to send and unlockAll (which implicitly requests the next batch)
            if (!unlockAllMessages && proxyQueue.getStarted() && !proxyQueue.isStopping()) {
              convHelper.requestNextMessageBatch();
            } else if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                SibTr.debug(this, tc, "Not requesting next message batch: " +
                            "unlockAllMessages=" + unlockAllMessages +
                            "isStarted=" + proxyQueue.getStarted() +
                            "isStopping=" + proxyQueue.isStopping());
            }
         }
      }
      catch(SIException e)
      {
         FFDCFilter.processException(e,
               CLASS_NAME + ".deliverBatch",
               CommsConstants.ASYNCHPQ_DELIVER_04,
               this);
         cs.deliverAsyncException(e);
      }

      // If we handled an exception and might need to redeliver messages,
      // then flow an unlockAll to the messaging engine to unlock all
      // messages and deliver the next batch. We perform this after
      // handling any in-callback actions, so that the messaging engine
      // knows whether or not to send us the next batch.
      // If we handled a close in-callback action (as well as
      // an exception) the unlockAll call will fail. However, there
      // is no method we can call on the consumer session to check whether
      // it is closed or not (isClosed is not properly implemented) so
      // it's safest as an APAR code change to simply accept that in
      // this unlikely circumstance we will generate an FFDC.
      if (unlockAllMessages)
      {
        try
        {
           cs.unlockAll();
        }
        catch(SIException e)
        {
           FFDCFilter.processException(e,
                 CLASS_NAME + ".deliverBatch",
                 CommsConstants.ASYNCHPQ_DELIVER_03,
                 this);
           if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "exception thrown");
           if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(tc, "exception", e);
        }
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "deliverBatch");
   }

   /** @see com.ibm.ws.sib.comms.client.proxyqueue.queue.Queue#get(short) */
   public JsMessage get(short id) throws SIResourceException, SIConnectionLostException, SIConnectionDroppedException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "get", id);
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Method not permitted for async queues");
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "get", "SIErrorException");
      // Not implemented for asynchronous queues
      throw new SIErrorException();
   }

   /** @see com.ibm.ws.sib.comms.client.proxyqueue.queue.Queue#unlockAll() */
   public void unlockAll()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "unlockAll");

      // Nothing to do here as the message batch count is calculated at the Proxy Queue level rather
      // than down here in the Queue

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "unlockAll");
   }

   /** @see com.ibm.ws.sib.comms.client.proxyqueue.queue.Queue#waitUntilEmpty() */
   public void waitUntilEmpty (final short sessionId) {
     if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "waitUntilEmpty", "sessionId="+sessionId);

     synchronized(concurrentAccessLock) {
       while (!isQueueEmptyForSessionId(sessionId)) {
         try {
           if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "waiting");
           concurrentAccessLock.wait();
         } catch (InterruptedException e) {
           // No FFDC code needed.
           if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.exception(this, tc, e);
         }
       }
     }

     if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "waitUntilEmpty");
   }

   /**
    * Checks whether there are ANY messages available for the supplied sessionId on the underlying queue.
    * Note that this provides different function from the isEmpty(short) method.
    *
    * @param sessionId
    * @return true if empty, false otherwise.
    */
   private synchronized boolean isQueueEmptyForSessionId (final short sessionId) {
     if (tc.isEntryEnabled()) SibTr.entry(this, tc, "isQueueEmptyForSessionId", "sessionId="+sessionId);

     boolean empty = true;
     final Iterator it = queue.iterator();
     while(it.hasNext()) {
       final QueueData data = (QueueData)it.next();
       final ProxyQueue pq = data.getProxyQueue();
       if (pq.getId() == sessionId) {
         if (tc.isDebugEnabled()) SibTr.debug(this, tc, "Found match: ", pq);
         empty = false;
         break;
       }
     }

     if (tc.isEntryEnabled()) SibTr.exit(this, tc, "isQueueEmptyForSessionId", Boolean.valueOf(empty));
     return empty;
   }
}
