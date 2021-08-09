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

import com.ibm.ws.sib.comms.client.DestinationSessionProxy;
import com.ibm.ws.sib.comms.client.proxyqueue.impl.ConversationHelper;
import com.ibm.ws.sib.comms.common.CommsByteBuffer;

/**
 * Base interface for all proxy queue classes.  Proxy queues
 * provide a "stageing area" for messages arriving at JS
 * clients.  They are intended to support the use of
 * ME side asynchronous messaging to reduce the number of
 * line turn arounds.  Additionally, where supported, they
 * provide read-ahead capability where a consumer can
 * have multiple messages streamed to it.
 * <p>
 * For more information about ProxyQueues, consult the
 * JetStream Client/Server Design document.
 */
public interface ProxyQueue
{
   /**
    * Returns the "proxy queue" identifier unique to this
    * proxy queue.  This is used to identify the proxy queue
    * created by the client to the ME so that it may deliver
    * messages to it.  Using this rather than a session ID
    * allows the server to assign session IDs without running
    * into timing issues or incuring additional line turn
    * arounds. 
    * 
    * @return short The "unique" identifier for this
    * proxy queue.
    */
   short getId();
     
   /**
    * Places message data onto this proxy queue.
    * Since some API calls will cause a proxy queue to be purged
    * before a message is delivered (eg. unlockAll) this method
    * accepts and stores "serialised" message data.  Conversion of
    * this data into a JsMessage is done just before de-queuing it
    * from its proxy queue. 
    * 
    * @param msgBuffer A CommsByteBuffer containing the raw data which
    * makes up a JsMessage. The position should be left at the first
    * byte of message data (i.e. the 32-bit message length) and the limit 
    * on the last.
    * @param msgBatch The message batch number the message was sent
    * in - this ensures we can correctly deal with in-flight messages
    * when issuing an "unlockAll".
    * @param lastInBatch Is this message the last message in a batch
    * of messages which make up a locked message enumeration?  This is
    * only applicable in the case of non-read ahead asynchronous
    * consumers where we must wait for a full batch of messages before
    * we can deliver them to the consumer callback.
    * @param chunk Indicates that the buffer contains a chunk of a message
    * rather than the entire JMO.
    */         
   public abstract void put(CommsByteBuffer msgBuffer,  
                            short msgBatch,
                            boolean lastInBatch,
                            boolean chunk);

   
   /**
    * @return Retrieves the destionation session proxy implementation
    * associated with this proxy queue.
    */
   DestinationSessionProxy getDestinationSessionProxy();
   
   /**
    * @return The conversation helper implementation associated with
    * this proxy queue.
    */
   ConversationHelper getConversationHelper();
   
   /**
    * Invoked to notify the proxy queue that the conversation backing it has closed for
    * some reason.  This can be used to notify any synchronous operations blocked on the
    * queue (e.g. receiveWithWait) to wakeup.
    */
   void conversationDroppedNotification();

}
