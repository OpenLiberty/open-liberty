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

package com.ibm.ws.sib.comms.client.proxyqueue.impl;

import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIIncorrectCallException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.sib.comms.CommsConnection;
import com.ibm.wsspi.sib.core.AsynchConsumerCallback;
import com.ibm.wsspi.sib.core.OrderingContext;
import com.ibm.wsspi.sib.core.SIMessageHandle;
import com.ibm.wsspi.sib.core.SITransaction;
import com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;
import com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException;
import com.ibm.wsspi.sib.core.exception.SILimitExceededException;
import com.ibm.wsspi.sib.core.exception.SIMessageNotLockedException;
import com.ibm.wsspi.sib.core.exception.SISessionDroppedException;
import com.ibm.wsspi.sib.core.exception.SISessionUnavailableException;


/**
 * Interface which defines the conversation related operations
 * required to communicate with the ME.  By splitting this interface
 * out from the implementation, we can substitute alternative
 * implementations for testing purposes.
 */
public interface ConversationHelper
{
   /**
    * Request the session to be closed.
    */
   void closeSession()
      throws SIResourceException, SIConnectionLostException,
             SIErrorException, SIConnectionDroppedException;

   /**
    * Delete a set of messages.
    * @param msgIds The message IDs for the messages to delete.
    * @param tran   The transaction to delete the messages under.
    * @param priority The JFAP priority to send the message.
    */
   void deleteMessages(SIMessageHandle[] msgHandles, SITransaction tran, int priority)         // f174317, d178368, F219476.2
      throws SISessionUnavailableException, SISessionDroppedException,
             SIConnectionUnavailableException, SIConnectionDroppedException,
             SIResourceException, SIConnectionLostException, SILimitExceededException,
             SIIncorrectCallException, SIMessageNotLockedException,
             SIErrorException;

   /**
    * Flushes the consumer session in an attempt to dislodge
    * any messages.
    */
   void flushConsumer()
      throws SISessionUnavailableException, SISessionDroppedException,
             SIConnectionUnavailableException, SIConnectionDroppedException,
             SIResourceException, SIConnectionLostException,
             SIErrorException;

   /**
    * Request more messages (used by read ahead queues)
    * @param receivedBytes Number of bytes received.
    * @param requestedBytes Number of bytes requested.
    */
   void requestMoreMessages(int receivedBytes, int requestedBytes)
      throws SIConnectionDroppedException, SIConnectionLostException;

   /**
    * Request the session starts.
    */
   void sendStart()
      throws SIConnectionDroppedException, SIConnectionLostException;

   /**
    * Request the session stops.
    */
   void exchangeStop()
      throws SISessionUnavailableException, SISessionDroppedException,
             SIConnectionUnavailableException, SIConnectionDroppedException,
             SIResourceException, SIConnectionLostException,
             SIErrorException;

   /**
    * Sets an asycnhronous consumer callback.
    */
   void setAsynchConsumer(AsynchConsumerCallback consumer,                 // f177889
                          int maxActiveMessages,
                          long messageLockExpiry,                          // F219476.2
                          int maxBatchSize,                                // f177889  // f187521.2.1
                          OrderingContext orderContext,                    // f200337
                          int maxSequentialFailures,                       // SIB0115d.comms
                          long hiddenMessageDelay,
                          boolean stoppable)                               // 472879
      throws SISessionUnavailableException, SISessionDroppedException,
             SIConnectionUnavailableException, SIConnectionDroppedException,
             SIErrorException,
             SIIncorrectCallException;

   /**
    * Sets the session ID
    * @param sessionId
    */
   void setSessionId(short sessionId);

   /**
    * Requests an unlock all.
    * @throws SIInvalidStateForOperationException
    * @throws SIResourceException
    */
   void unlockAll()
      throws SISessionUnavailableException, SISessionDroppedException,
             SIConnectionUnavailableException, SIConnectionDroppedException,
             SIResourceException, SIConnectionLostException,
             SIErrorException;

   /**
    * Unsets an asynch consumer.
    */
   void unsetAsynchConsumer(boolean stoppable)                                                          //SIB0115d.comms
      throws SISessionUnavailableException, SISessionDroppedException,
             SIConnectionUnavailableException, SIConnectionDroppedException,
             SIErrorException,
             SIIncorrectCallException;

   /**
    * Unlocks a set of messages.
    * @param msgIds The message Ids to unlock.
    */
   void unlockSet(SIMessageHandle[] msgHandles)                            // F219476.2
      throws SIIncorrectCallException, SIMessageNotLockedException,
             SIConnectionDroppedException, SIConnectionLostException;

   /**
    * Requests another batch of messages (asynchronous queue).
    */
   void requestNextMessageBatch()
      throws SIConnectionDroppedException, SIConnectionLostException;

   /**
    * Exchanges a request for a reset of the browse cursor.
    */
   void exchangeResetBrowse()
      throws SISessionUnavailableException, SISessionDroppedException,
             SIConnectionUnavailableException, SIConnectionDroppedException,
             SIResourceException, SIConnectionLostException,
             SIErrorException;

   /**
    * @return Returns the comms connection associated with this conversation.
    */
   CommsConnection getCommsConnection();                                                  // D268606
}
