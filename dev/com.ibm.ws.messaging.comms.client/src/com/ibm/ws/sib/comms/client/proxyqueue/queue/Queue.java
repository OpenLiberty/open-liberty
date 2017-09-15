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

package com.ibm.ws.sib.comms.client.proxyqueue.queue;

import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.sib.comms.client.proxyqueue.impl.ConversationHelper;
import com.ibm.ws.sib.comms.common.CommsByteBuffer;
import com.ibm.ws.sib.mfp.JsMessage;
import com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;

/**
 * Interface which defines methods for all types of queue.
 */
public interface Queue
{
   /**
    * Places a message onto the queue.
    *
    * @param queueData The data that makes up the message.
    * @param msgBatch The message batch number.
    */
   void put(QueueData queueData, short msgBatch);

   /**
    * Retrieves a message from the queue, or returns null if no message is
    * currently residing on the queue.  This call is only valid for
    * read ahead queues.
    *
    * @param id The session Id
    * @return JsMessage The message.
    *
    * @throws SIResourceException
    * @throws SIConnectionLostException
    * @throws SIConnectionDroppedException
    */
   JsMessage get(short id)
      throws SIResourceException, SIConnectionLostException, SIConnectionDroppedException;

   /**
    * @param msgBuffer
    * @param lastChunk
    */
   void appendToLastMessage(CommsByteBuffer msgBuffer, boolean lastChunk);

   /**
    * Delivers a batch of messages to the asynchronous consumer callback
    * @param batchSize The maximum number of messages to deliver in the batch
    * @param id The proxy id for the batch
    * @param convHelper The conversation helper to use when delivering the batch.
    */
   void deliverBatch(int batchSize, short id, ConversationHelper convHelper);

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
    */
   boolean isEmpty(short sessionId);

   /**
    * Returns true if the underlying queue is totally empty.  For read ahead, this
    * is identical to the isEmpty method.  For asynch consumer queues, this may
    * return false even if isEmpty returns true.  This is because the method will
    * only return true when there are no messages at all on the queue.
    *
    * @return boolean
    */
   boolean isQueueEmpty();

   /**
    * Blocks the caller until the queue is completely empty for the sessionId
    */
   void waitUntilEmpty(short sessionId);

   /**
    * Purge all the messages from the queue.
    *
    * @param sessionId The proxy queue id of the messages being purged.
    * Typically, this is used for ordered queues.
    */
   void purge(short sessionId);

   /**
    * @return Returns a lock to be taken when performing operations on the queue such
    *         us getting messages for delivery.
    */
   Object getConcurrentAccessLock();

   /**
    * Unlocks all messages on the proxy queue.
    */
   void unlockAll();
}
