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
package com.ibm.ws.sib.comms.client.proxyqueue;

import com.ibm.websphere.sib.Reliability;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIIncorrectCallException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.sib.jfapchannel.Conversation;
//import com.ibm.ws.sib.multicast.MulticastReceiver;
import com.ibm.wsspi.sib.core.OrderingContext;
import com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;

/**
 * A group of proxy queues, associated with a conversation.
 * There should be a one to one mapping between instances of
 * this class and instances of the Conversation class on the
 * client.  To these ends, an instance of this object should
 * probably be held with the ClientConversationState object.
 * <p>
 * Proxy queue conversation groups exist for two reasons:
 * <ul>
 * <li>To provide a suitable place for methods which effect
 *     all the proxy queues associated with a particular
 *     conversation - e.g. close.</li>
 * <li>As a means of reducing the contention for monitors
 *     which might otherwise be required if operations like
 *     "put to a queue" were managed from a singleton class.</li>
 * </ul>  
 * @see com.ibm.ws.sib.jfapchannel.Conversation
 */
public interface ProxyQueueConversationGroup
{
   /**
    * Creates a new browser proxy queue.
    * @return BrowserProxyQueue
    */
   BrowserProxyQueue createBrowserProxyQueue()                          // F171893
      throws SIResourceException, SIIncorrectCallException;

   /**
    * Creates a proxy queue that can be used for asynchronous consumption
    * of messages - with or without ordering.
    * @param orderingContext The (optional) ordering context used for
    * ordering messages delivered to the asynchronous consumer.
    * @return The newly created proxy queue.
    * @throws SIResourceException
    * @throws SIIncorrectCallException
    */
   AsynchConsumerProxyQueue createAsynchConsumerProxyQueue(OrderingContext orderingContext)   // D249096
   throws SIResourceException, 
          SIIncorrectCallException;

   // Start D213014
   
   /**
    * Creates a new proxy queue that can be used for asynchronous or
    * ordered asynchronous consumption of messages.
    * @return The newly create proxy queue.
    * @param id The proxy queue identifier to associate with the queue.
    * @param seqNumber The sequence number to associate with the queue.
    * @param orderingContext The (optional) ordering context to associate 
    * with the queue.
    * @throws SIIncorrectCallException
    */
   AsynchConsumerProxyQueue createAsynchConsumerProxyQueue(short id,          // D249096
                                                           short seqNumber, 
                                                           OrderingContext orderingContext) 
   throws SIIncorrectCallException;
   // End D213014
   
   /**
    * Creates a new read ahead proxy queue.
    * 
    * @param unrecoverableReliabilty
    * 
    * @return ReadAheadProxyQueue
    */
   AsynchConsumerProxyQueue createReadAheadProxyQueue(Reliability unrecoverableReliabilty)  // f187521.2.1
      throws SIResourceException, SIIncorrectCallException;

   /**
    * This method is used to create a proxy queue that is used for asynchronous consumers with an
    * associated message ordering context.
    * 
    * @param context
    * 
    * @throws SIResourceException
    * @throws SIIncorrectCallException
    */
   AsynchConsumerProxyQueue createOrderedProxyQueue(OrderingContext context)
      throws SIResourceException, SIIncorrectCallException;

   // Start D213014
   /**
    * This method is used to create a proxy queue that is used for asynchronous consumers with an
    * associated message ordering context.
    * 
    * @param context
    * @param id The id to give the proxy queue.
    * @param messageBatchSequence A starting number for the message batch sequence number.
    * 
    * @throws SIResourceException
    * @throws SIIncorrectCallException
    */
   AsynchConsumerProxyQueue createOrderedProxyQueue(OrderingContext context, short id, short messageBatchSequence)
      throws SIResourceException, SIIncorrectCallException;
   // End D213014

   /**
    * Closes all proxy queues in this group.  This also
    * marks the group as "closed" preventing any more calls
    * to create proxy queues from succeeding.  This method
    * is intended to be called from the close method of a
    * SIConnection.
    * <p>
    * It is assumed that prior to closing a connection a
    * "close connection" flow will have been sent to the ME.
    * (ie. to avoid duplication, the proxy queues do not send
    * this flow themselves).
    */
   void closeNotification()
      throws SIResourceException, SIConnectionLostException,
             SIErrorException, SIConnectionDroppedException;
   
   
   void close();
   
   /**
    * Looks up a proxy queue given its identifier.
    * @param proxyQueueId
    * @return ProxyQueue The "found" proxy queue - or null if no
    * queue exists with the specified ID.
    */
   ProxyQueue find(short proxyQueueId);
   
   /**
    * Returns the conversation this group is associated with.
    * @return Conversation
    */
   Conversation getConversation() throws SIIncorrectCallException;
   
   /**
    * Unregisters this proxy queue
    * @param queue
    */
   void bury(ProxyQueue queue);                                         // d180495
   
   /**
    * Invoked to notify the group of proxy queues that the conversation backing them has 
    * closed for some reason.  This can be used to notify any synchronous operations 
    * blocked on the queue (e.g. receiveWithWait) to wakeup.
    */
   void conversationDroppedNotification();
}
