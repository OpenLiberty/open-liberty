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
// Change for defect D249096 are not changed flaged as they are too numerous.

package com.ibm.ws.sib.comms.client.proxyqueue;

import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIIncorrectCallException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.sib.comms.client.ConsumerSessionProxy;
import com.ibm.ws.sib.mfp.JsMessage;
import com.ibm.ws.sib.mfp.MessageDecodeFailedException;
import com.ibm.wsspi.sib.core.AsynchConsumerCallback;
import com.ibm.wsspi.sib.core.OrderingContext;
import com.ibm.wsspi.sib.core.SITransaction;
import com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;
import com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException;
import com.ibm.wsspi.sib.core.exception.SILimitExceededException;
import com.ibm.wsspi.sib.core.exception.SINotAuthorizedException;
import com.ibm.wsspi.sib.core.exception.SISessionDroppedException;
import com.ibm.wsspi.sib.core.exception.SISessionUnavailableException;


/**
 * A proxy queue intended to support consumer sessions which
 * register an consumer callback.  The proxy queue will act as
 * a "staging area" for batches of messages being flowed as part
 * of a locked message enumeration, destined for consumer
 * sessions which register an asynchronous consumer callback.
 * @see com.ibm.ws.sib.comms.client.proxyqueue.ProxyQueue
 */
public interface AsynchConsumerProxyQueue extends ProxyQueue
{
   /**
    * Associates a consumer session with this proxy queue.  The
    * consumer session must be the session the proxy queue is being used
    * by.  This method must be called prior to any messages being delivered
    * asynchronously to a callback registered for the session.
    * <p>
    * This should not present a timing window, as even though we do not necessarily
    * know the consumer session we are being created for at the time the proxy
    * queue is created, we cannot have an asynchronous consumer associated with a
    * proxy queue until a consumer session has been created (the consumer session
    * provides the method to register asynchronous consumers).
    */
   void setConsumerSession(ConsumerSessionProxy consumerSession);

   /**
    * Returns the associated ConsumerSessionProxy
    * @return the consumer session associated with this proxy queue
    */
   ConsumerSessionProxy getConsumerSessionProxy ();                                                     //SIB0115d.comms

   /**
    * Delivers an asynchrounous exception received by the client to any
    * exception listeners that have been registered.
    * <p>
    * At any time during async processing the server may encounter an error
    * which the client cannot be directly informed about. In this event, the
    * error will be flowed back to the server as an event and calling this
    * method should allow any attached connection listeners to become aware
    * of the problem.
    * <p>
    * In the event that this event message cannot be processed, for instance
    * if it is received at a time when there is no associated consumer session,
    * then these exceptions will be queued until such a time as we can deliver
    * them.
    */
   void deliverException(Throwable exception);                                            // d172528

    /**
    * Returns the current message batch sequence number. Each message that is sent
    * asynchronously from the server contains a message batch sequence number. When
    * processing an <code>unlockAll()</code> request the sequence number is increased
    * so that a distinction can be made between old messages (prior to the unlockAll())
    * and new ones.
    *
    * @return Returns the current message batch sequence number.
    */
   short getCurrentMessageBatchSequenceNumber();

   /**
    * Performs a "receiveNoWait" against the proxy queue.  When
    * messages are present on the queue, this returns immediately
    * with a message.  When no messages are present, it must
    * check that the ME is not "hording" messages (requiring
    * a line turn around) before returning a value of null.
    *
    * @param transaction A transaction (if non-null) to receive
    * the message under.
    *
    * @return JsMessage The JsMessage (if any) retrieved.
    */
   JsMessage receiveNoWait(SITransaction transaction)    // f177889     // f187521.2.1
      throws MessageDecodeFailedException,
             SISessionUnavailableException, SISessionDroppedException,
             SIConnectionUnavailableException, SIConnectionDroppedException,
             SIResourceException, SIConnectionLostException, SILimitExceededException,
             SIErrorException,
             SINotAuthorizedException,
             SIIncorrectCallException;

   /**
    * Performs a "receive with wait" against the proxy queue.
    * This is identical to the "receiveNoWait" method in every
    * aspect except it will wait for up to a certain amount of
    * time for a message to arrive if one is not already present.
    *
    * @param timeout The amount of time to wait for a message to
    * turn up before returning null.  Zero is a special value
    * meaning "forever".
    * @param transaction The transaction (if non-null) to receive
    * the message inside.
    *
    * @return JsMessage The JsMessage (if any) retrieved.
    */
   JsMessage receiveWithWait(long timeout,
                             SITransaction transaction)  // f177889        // f187521.2.1
      throws MessageDecodeFailedException,
             SISessionUnavailableException, SISessionDroppedException,
             SIConnectionUnavailableException, SIConnectionDroppedException,
             SIResourceException, SIConnectionLostException, SILimitExceededException,
             SIErrorException,
             SINotAuthorizedException,
             SIIncorrectCallException;

   /**
    * @return The maximum size of the message batch that may be delivered
    * to an asynchronous consumer associated with this proxy queue.
    */
   int getBatchSize();                                                     // D249069

   /**
    * @return The asynchronous consumer currently registered for asynchronous
    * reciept of messages with the session associated with this proxy queue.
    * A value of null means that no consumer is registered.
    */
   AsynchConsumerCallback getAsynchConsumerCallback();                     // D249069

   /**
    * Associates the thread used to drive an asynchronous consumer callback
    * with this proxy queue.  This is done so that users of the proxy
    * queue can determine if the thread they are executing on is a
    * callback thread.
    * @param thread The thread to associate with this proxy queue.
    */
   void setAsynchConsumerThread(Thread thread);                            // D249069

   /**
    * @return The thread currently associated with this proxy queue by the
    * setAsynchConsumerThread method.
    */
   Thread getAsynchConsumerThread();                                       // D249069

   /**
    * Notifies the proxy queue that the session has started closing.
    * This would typically be used to wake up any threads that need
    * notifying of a close - for example receive with wait.
    */
   void closing();                                                         // D249069

   /**
    * Notifies the proxyqueue that the session has closed.  This
    * would typically be used to notify our peer to close the
    * core SPI session.
    * @throws SIConnectionDroppedException
    * @throws SIConnectionLostException
    * @throws SIResourceException
    * @throws SIErrorException
    */
   void closed()                                                           // D249069
   throws SIConnectionDroppedException,
          SIConnectionLostException,
          SIResourceException,
          SIErrorException;

   /**
    * Notifies the proxy queue that the session has been started.
    * This notification may be used to send the appropriate data
    * to our peer, or to start the delivery of messages to an
    * asynchronous callback for the session.
    * @throws SIConnectionDroppedException
    * @throws SIConnectionLostException
    */
   void start()                                                            // D249069
   throws SIConnectionDroppedException,
          SIConnectionLostException;

   /**
    * Notification that the session associated with this proxy queue
    * is about to stop.  This notification would typically be used
    * to stop any asynchronous consumer from receiving more messages
    *
    * @throws SISessionDroppedException
    * @throws SIConnectionDroppedException
    * @throws SISessionUnavailableException
    * @throws SIConnectionUnavailableException
    * @throws SIConnectionLostException
    * @throws SIResourceException
    * @throws SIErrorException
    */
   void stopping(boolean notifypeer)
   throws SISessionDroppedException,
          SIConnectionDroppedException,
          SISessionUnavailableException,
          SIConnectionUnavailableException,
          SIConnectionLostException,
          SIResourceException,
          SIErrorException;

   /**
    * Notification that the session associated with this proxy queue
    * has stopped.
    *
    * @param notifypeer that we are stopped
    */
   void stopped();

   /**
    * Associates an asynchronous consumer callback with this proxy
    * queue.
    * @param callback The callback to associate with this queue.
    * @param maxActiveMessages The maximum number of active messages that
    * may be delivered asynchronously to this session.
    * @param messageLockExpiry The lock expiry time for messages.
    * @param maxBatchSize The maximum size of batches of messages that
    * may be delivered to the consumer.
    * @param orderContext An optional ordering context that may be
    * @param maxSequentialFailures Only used when callback is an instance of StoppableAsynchConsumerCallback
    * used to order message delivery across sessions.
    * @param hiddenMessageDelay Only used when callback is an instance of StoppableAsynchConsumerCallback
    * used to delay messages (keep them hidden) after a rollback before redelivery.
    * @param stoppable indicates whether the callback represents a Stoppable callback or not
    * @throws SISessionDroppedException
    * @throws SIConnectionDroppedException
    * @throws SISessionUnavailableException
    * @throws SIConnectionUnavailableException
    * @throws SIErrorException
    * @throws SIIncorrectCallException
    */
   public void setAsynchCallback(AsynchConsumerCallback callback,             // D249069
                                 int maxActiveMessages,
                                 long messageLockExpiry,
                                 int maxBatchSize,
                                 OrderingContext orderContext,
                                 int maxSequentialFailures,                                             //SIB0115d.comms
                                 long hiddenMessageDelay,
                                 boolean stoppable)                                                             //472879
   throws SISessionDroppedException,
          SIConnectionDroppedException,
          SISessionUnavailableException,
          SIConnectionUnavailableException,
          SIErrorException,
          SIIncorrectCallException;

   /**
    * Instructs the proxy queue implementation to unlock all
    * locked messages for this session.  Typically this will
    * result in the proxy queue informing the server to unlock
    * all.  However special contingencies need to devised for
    * "inflight" messages which have been sent by the server
    * but have not yet arrived at the client at the point in
    * time this method is invoked.
    * @throws SISessionUnavailableException
    * @throws SISessionDroppedException
    * @throws SIConnectionUnavailableException
    * @throws SIConnectionDroppedException
    * @throws SIResourceException
    * @throws SIConnectionLostException
    * @throws SIErrorException
    */
   void unlockAll()                                                        // D249069
   throws SISessionUnavailableException,
          SISessionDroppedException,
          SIConnectionUnavailableException,
          SIConnectionDroppedException,
          SIResourceException,
          SIConnectionLostException,
          SIErrorException;

   /**
    * @return An object whos monitor must be held when invoking methods
    * on the locked message enumeration.  This is used to ensure that only
    * one of the following methods may be executed at a given time, for a
    * given session / locked message enumeration:
    * <ul><li>unlockSet</li><li>unlockAll</li><li>deleteSet</li>
    * <li>nextLocked</li><li>unlockCurrent</li><li>deleteCurrent</li>
    * <li>deleteSeen</li></ul>
    */
   Object getLMEOperationMonitor();                                        // D249069

   /**
    * Delivers a batch of messages to the asynchronous consumer
    * associated with this proxy queue.
    */
   void deliverMessages();                                                 // D249069

   /**
    * Notifies this proxy queue that messages may have become available
    * for consumption.  Typically this is only used for ordered queues -
    * where it is possible that the consumption of a batch of messages
    * associated with a particular ordering context will make available
    * messages associated with a different ordering context.
    */
   void nudge();                                                           // D249069
   
   /**
    * Informs the proxy queue of the rollback of a transaction under which
    * consumer associated with this proxy queue deleted recoverable messages.
    * Proxy queue implementation that can support the strict rollback ordering
    * concept should return all messages to the ME in this call. 
    * @throws SIErrorException 
    * @throws SIResourceException 
    * @throws SIConnectionLostException 
    * @throws SIConnectionUnavailableException 
    * @throws SISessionUnavailableException 
    * @throws SIConnectionDroppedException 
    * @throws SISessionDroppedException 
    */
   public void rollbackOccurred()  throws SISessionDroppedException, SIConnectionDroppedException, SISessionUnavailableException, SIConnectionUnavailableException, SIConnectionLostException, SIResourceException, SIErrorException;     
   
   /*
    * Return a boolean indicating whether the async consumer is started or not
    */
  boolean getStarted ();

   /*
    * Return a boolean indicating whether the async consumer is in the process of stopping
    * but is not yet fully stopped
    */
   boolean isStopping ();
}
