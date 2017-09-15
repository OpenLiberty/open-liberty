/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.sib.processor;

import com.ibm.websphere.sib.Reliability;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIIncorrectCallException;
import com.ibm.wsspi.sib.core.AsynchConsumerCallback;
import com.ibm.wsspi.sib.core.StoppableAsynchConsumerCallback;
import com.ibm.wsspi.sib.core.ConsumerSession;
import com.ibm.wsspi.sib.core.OrderingContext;
import com.ibm.wsspi.sib.core.SIBusMessage;
import com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException;
import com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException;
import com.ibm.wsspi.sib.core.exception.SIDurableSubscriptionNotFoundException;
import com.ibm.wsspi.sib.core.exception.SISessionDroppedException;
import com.ibm.wsspi.sib.core.exception.SISessionUnavailableException;

/**
 * Internal extended sib.core ConsumerSession interface
 */
public interface MPConsumerSession extends ConsumerSession {

  /**
   Registers a callback to which messages can be delivered asynchronously, and
   causes the ConsumerSession to transition from synchronous to asynchronous
   delivery mode. The client must implement the AsynchronousConsumerCallback
   interface, the consumeMessages method of which is called when messages are
   available for this consumer. The Core API implementation will assign up to
   maxBatchSize messages to the consumer per invocation of consumeMessages.
   registerAsynchConsumerCallback may only be called on a stopped
   ConsumerSession; otherwise, SIInvalidStateForOperationException is thrown.
   <p>
   The deliverImmediately parameter affects the behaviour of the start
   operation. If matching messages are available for this consumer at the time
   that start is called, then the AsynchConsumerSession's consumeMesasges
   method may be called prior to return from the start method itself.
   <p>
   If the ConsumerSession is already in asynchronous mode, then calling
   registerAsynchConsumerCallback deregisters the existing callback and replaces
   it with that supplied on the call.
   <p>
   The unrecoverableReliability parameter is used to override the consumerSession's
   setting. Its intended use is for Comms to use it to represent a non-transacted
   synchronous receiver.
   @param callback the object to which messages will be delivered
   asynchronously
   @param maxBatchSize the maximum number of messages that may be delivered at
   a time
   @param unrecoverableReliability the reliability level that will be
   unrecoverable for messages delivered to this asynch consumer
   @param inLine Run the callback on the caller's thread

   @throws com.ibm.wsspi.sib.core.exception.SISessionUnavailableException
   @throws com.ibm.wsspi.sib.core.exception.SISessionDroppedException
   @throws com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException
   @throws com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException
   @throws com.ibm.websphere.sib.exception.SIErrorException
   @throws com.ibm.websphere.sib.exception.SIIncorrectCallException

  */
  public void registerAsynchConsumerCallback(
      AsynchConsumerCallback callback,
      int maxActiveMessages,
      long messageLockExpiry,
      int maxBatchSize,
      Reliability unrecoverableReliability,
      boolean inLine,
      OrderingContext context)
  throws SISessionUnavailableException, SISessionDroppedException,
         SIConnectionUnavailableException, SIConnectionDroppedException,
         SIErrorException,
         SIIncorrectCallException;

  /**
   Registers a stoppable callback to which messages can be delivered asynchronously, and
   causes the ConsumerSession to transition from synchronous to asynchronous
   delivery mode. The client must implement the StoppableAsynchronousConsumerCallback
   interface, the consumeMessages method of which is called when messages are
   available for this consumer. The Core API implementation will assign up to
   maxBatchSize messages to the consumer per invocation of consumeMessages.
   registerStoppableAsynchConsumerCallback may only be called on a stopped
   ConsumerSession; otherwise, SIInvalidStateForOperationException is thrown.
   <p>
   If maxSequentialFailures occur then the session will be stopped and the consumerSessionStopped
   method of the StoppableAsynchConsumerCallback callback will be called to inform the callback
   that the session has been stopped. The session must be started to resume message delivery.
   <p>
   If the ConsumerSession is already in asynchronous mode, then calling
   registerAsynchConsumerCallback deregisters the existing callback and replaces
   it with that supplied on the call.
   <p>
   The unrecoverableReliability parameter is used to override the consumerSession's
   setting. Its intended use is for Comms to use it to represent a non-transacted
   synchronous receiver.

   @param callback the object to which messages will be delivered asynchronously
   @param maxActiveMessages
   @param messageLockExpiry
   @param maxBatchSize the maximum number of messages that may be delivered at a time
   @param unrecoverableReliability the reliability level that will be unrecoverable for messages delivered to this asynch consumer
   @param inLine Run the callback on the caller's thread
   @param context Message ordering context to be applied
   @param maxSequentialFailures number of sequential failures after which the session is stopped

   @throws com.ibm.wsspi.sib.core.exception.SISessionUnavailableException
   @throws com.ibm.wsspi.sib.core.exception.SISessionDroppedException
   @throws com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException
   @throws com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException
   @throws com.ibm.websphere.sib.exception.SIErrorException
   @throws com.ibm.websphere.sib.exception.SIIncorrectCallException

  */
  public void registerStoppableAsynchConsumerCallback(
      StoppableAsynchConsumerCallback callback,
      int maxActiveMessages,
      long messageLockExpiry,
      int maxBatchSize,
      Reliability unrecoverableReliability,
      boolean inLine,
      OrderingContext context,
      int maxSequentialFailures,
      long hiddenMessageDelay)
  throws SISessionUnavailableException, SISessionDroppedException,
         SIConnectionUnavailableException, SIConnectionDroppedException,
         SIErrorException,
         SIIncorrectCallException;

  /**
   * Returns the current message locked to the consumer, or null if it is no longer
   * available to be locked
   *
   * If SISessionUnavailableException is thrown, this indicates that the ConsumerSession
   * has been closed, and that for
   *
   * @return the current re-locked message
   *
   */
  public SIBusMessage relockMessageUnderAsynchCursor()
    throws SISessionUnavailableException;


  /**
   * Retrieve the MPSubscription object that represents the subscription (durable only)
   * that this ConsumerSession is feeding from
   *
   * This function is only available on locally homed durable subscriptions
   *
   * Performing this against a queue consumer results in a SIDurableSubscriptionNotFoundException
   */
  public MPSubscription getSubscription()
    throws SIDurableSubscriptionNotFoundException;

  /**
   * THIS IS HORRIBLE! DON'T TOUCH IT WITH A BARGE POLE
   *
   * It's only here to allow WSN to decide whether to bump the unlock count (redelivery count)
   * on it's locked messages when it closes a BifurcatedConsumerSession linked to this ConsumerSession
   * when running in an SR on zOS.
   *
   * Once we have LME.unlock(boolean redeliveryCountUnchanged) on the Core SPI (and therefore available
   * from an SR we can delete this method
   */
  public void setBifurcatedConsumerCloseRedeliveryMode(boolean bumpRedeliveryOnClose);

}

