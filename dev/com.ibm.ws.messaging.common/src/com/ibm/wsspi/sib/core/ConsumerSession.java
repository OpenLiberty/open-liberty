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

package com.ibm.wsspi.sib.core;

import com.ibm.websphere.sib.exception.SIIncorrectCallException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;
import com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException;
import com.ibm.wsspi.sib.core.exception.SILimitExceededException;
import com.ibm.wsspi.sib.core.exception.SINotAuthorizedException;
import com.ibm.wsspi.sib.core.exception.SISessionDroppedException;
import com.ibm.wsspi.sib.core.exception.SISessionUnavailableException;

/**
 A ConsumerSession is used to receive messages from a Destination. It can be
 regarded as representing a Destination "opened for get".
 <p>
 A ConsumerSession essentially has two states (or three if you count the closed
 state): When an AynchConsumerCallback is registered, synchronous receipt is not
 permitted (InvalidStateForOperationException is thrown); when an
 AsynchConsumerCallback is not registered, asynchronous receipt is not possible
 and the related methods on ConsumerSession throw the exception. The initial
 state of a newly created ConsumerSession is "stopped".
 <p>
 Optionally, a ConsumerSession may use a "discriminator", specified on the
 createConsumerSession call. If provided, the discriminator is matched against
 the discriminator used by Producers when sending, and only matching messages
 are returned. Normally a discriminator is specified when consuming from a
 publish/subscribe destination, indicating the set of topics that consumer is
 interested in. If a consumer to a publish/subscribe destination does not
 specify a discriminator, then it will receive all publications sent to the
 destination. If the consumer specifies the empty string, then the consumer
 will receive only messages published to the special topic at the root of the
 hierarchy; that is, it will receive only messages published with no
 discriminator. When consuming from point-to-point destinations, the
 discriminator is normally left unspecified (null or the empty string is
 passed). If a discriminator is specified, this narrows the set of messages
 received to only those published with a matching discriminator (just as if the
 discriminator had been specified as part of a selector string).
 <p>
 This class has no direct security implications. Security checks are applied at
 the time that a ConsumerSession is created, see the SICoreConnection
 class for details.
*/
public interface ConsumerSession extends AbstractConsumerSession {

  /**
   Receives a message from the destination to which the consumer is attached.
   If there is a matching message available immediately, it is returned;
   otherwise, null is returned.
   <p>
   Note that a ConsumerSession will only receive messages when it is started;
   in the stopped state, receiveNoWait always returns null. Note also that if
   receiveNoWait is invoked while an AsynchConsumerCallback is registered,
   SIIncorrectCallException is thrown.
   <p>
   The possibility of an SINotAuthorizedException being thrown by this method has
   been removed, there are no security implications with this method.

   @param tran the transaction under which the receive is to occur (may be null)

   @return a message matching the discriminator and selector

   @throws com.ibm.wsspi.sib.core.exception.SISessionUnavailableException
   @throws com.ibm.wsspi.sib.core.exception.SISessionDroppedException
   @throws com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException
   @throws com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException
   @throws com.ibm.websphere.sib.exception.SIResourceException
   @throws com.ibm.wsspi.sib.core.exception.SIConnectionLostException
   @throws com.ibm.wsspi.sib.core.exception.SILimitExceededException
   @throws com.ibm.wsspi.sib.core.exception.SINotAuthorizedException
   @throws com.ibm.websphere.sib.exception.SIIncorrectCallException
  */
  public SIBusMessage receiveNoWait(SITransaction tran)
    throws SISessionUnavailableException, SISessionDroppedException,
           SIConnectionUnavailableException, SIConnectionDroppedException,
           SIResourceException, SIConnectionLostException, SILimitExceededException,
           SINotAuthorizedException,
           SIIncorrectCallException;

  /**
   Receives a message from the destination to which the consumer is attached.
   If a matching message becomes available before the timeout milliseconds
   elapse, it is returned; otherwise, null is returned. If a timeout of 0 is
   given, the ConsumerSession waits indefinitely.
   <p>
   Note that a ConsumerSession will only receive messages when it is started; if
   the ConsumerSession is in the stopped state when the timeout expires, then
   null will be returned whether or not a matching message is available. (Note
   that the ConsumerSession always goes into the timed wait, even if it is
   stopped and the time that receiveWithWait is called; this is in case start is
   called before the timer expires, in which case an available matching message
   would be returned.)
   <p>
   If the ConsumerSession is closed during the receiveWithWait, then
   SIObjectClosedException is thrown immediately. Note also that if
   receiveNoWait is invoked while an AsynchConsumerCallback is registered,
   SIInvalidStateForOperationException is thrown.
   <p>
   The possibility of an SINotAuthorizedException being thrown by this method has
   been removed, there are no security implications with this method.

   @param tran the transaction under which the receive is to occur (may be
   null)
   @param timeout the number of milliseconds to wait for a matching message to
   become available

   @return a message matching the discriminator and selector

   @throws com.ibm.wsspi.sib.core.exception.SISessionUnavailableException
   @throws com.ibm.wsspi.sib.core.exception.SISessionDroppedException
   @throws com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException
   @throws com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException
   @throws com.ibm.websphere.sib.exception.SIResourceException
   @throws com.ibm.wsspi.sib.core.exception.SIConnectionLostException
   @throws com.ibm.wsspi.sib.core.exception.SILimitExceededException
   @throws com.ibm.wsspi.sib.core.exception.SINotAuthorizedException
   @throws com.ibm.websphere.sib.exception.SIIncorrectCallException
  */
  public SIBusMessage receiveWithWait(
      SITransaction tran,
      long timeout)
    throws SISessionUnavailableException, SISessionDroppedException,
           SIConnectionUnavailableException, SIConnectionDroppedException,
           SIResourceException, SIConnectionLostException, SILimitExceededException,
           SINotAuthorizedException,
           SIIncorrectCallException;

  /**
   Starts the ConsumerSession. If the ConsumerSession has an
   AsynchConsumerCallback registered, then messages will be delivered to it
   (before the start call returns if the deliverImmediately flag was set to true
   when the AsynchConsumerCallback was registered). If there is an outstanding
   getWithWait on the ConsumerSession, the getWithWait will return a matching
   message, if one is available.
   <p>
   If the deliverImmediately parameter has value true, and messages are
   available for this consumer at the time that start is called, then the
   AsynchConsumerSession's consumeMessages method may be called on the same
   thread prior to return from the start method itself. If the
   deliverImmediately parameter has value false, then the consumeMessages method
   will not be called at this time, whether or not there are messages available.

   @param deliverImmediately true if consumeMessages can be called before start
   returns

   @throws com.ibm.wsspi.sib.core.exception.SISessionUnavailableException
   @throws com.ibm.wsspi.sib.core.exception.SISessionDroppedException
   @throws com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException
   @throws com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException
   @throws com.ibm.websphere.sib.exception.SIResourceException
   @throws com.ibm.wsspi.sib.core.exception.SIConnectionLostException
  */
  public void start(boolean deliverImmediately)
    throws SISessionUnavailableException, SISessionDroppedException,
           SIConnectionUnavailableException, SIConnectionDroppedException,
           SIResourceException, SIConnectionLostException;

  /**
   Stops the ConsumerSession. If the ConsumerSession has an
   AsynchConsumerCallback registered, then its consumeMessages method will not
   be invoked again until the ConsumerSession is restarted (however, if it is
   currently executing consumeMessages, it can finish processing the messages
   already assigned). If there is an outstanding getWithWait on the
   ConsumerSession, then the getWithWait will return null on expiry of the
   timeout (unless the ConsumerSession is restarted before the timeout expires).

   @throws com.ibm.wsspi.sib.core.exception.SISessionUnavailableException
   @throws com.ibm.wsspi.sib.core.exception.SISessionDroppedException
   @throws com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException
   @throws com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException
   @throws com.ibm.websphere.sib.exception.SIResourceException
   @throws com.ibm.wsspi.sib.core.exception.SIConnectionLostException
  */
  public void stop()
    throws SISessionUnavailableException, SISessionDroppedException,
           SIConnectionUnavailableException, SIConnectionDroppedException,
           SIResourceException, SIConnectionLostException;

  /**
   Registers a callback to which messages can be delivered asynchronously, and
   causes the ConsumerSession to transition from synchronous to asynchronous
   delivery mode. The client must implement the AsynchronousConsumerCallback
   interface, the consumeMessages method of which is called when messages are
   available for this consumer. The Core API implementation will assign up to
   maxBatchSize messages to the consumer per invocation of consumeMessages.
   <p>
   registerAsynchConsumerCallback may only be called on a stopped
   ConsumerSession; otherwise, SIIncorrectCallException is thrown.
   <p>
   If the ConsumerSession is already in asynchronous mode, then calling
   registerAsynchConsumerCallback deregisters the existing callback and replaces
   it with that supplied on the call.
   <p>
   maxActiveMessages sets a limit on the number of messages that may be assigned
   to the consumer at any time. A message is assigned to the consumer if it is
   locked, or if the consumer has issued a delete call for the message, with a
   transaction that has not yet completed (committed or rolled back). Once this
   limit is reached, the Core SPI implementation will cease delivering messages
   to the consumer until some are unlocked or committed (deleted) or rolledback.
   <p>
   Note that undeleted messages may remain locked to the consumer even after a call to
   consumeMessages on the registered callback returns if they have been viewed
   by the callback (LockedMEssageEnumeration.nextLocked has returned the message).
   If they have not been viewed the messages will be unlocked by the system once
   consumeMessages returns without incrementing the RedeliveredCount of the message.
   <p>
   maxBatchSize determines the maximum number of messages that may be delivered
   to a consumer in a single call to AsynchConsumerCallback.consumeMessages. A value
   maxActiveMessages = 0 is interpreted to mean that the consumer places no
   limit on the number of active messages (any configured limit would still
   apply). If 0 < maxActiveMessages < maxBatchSize, then the effective maximum
   batch size will be maxActiveMessages.
   <p>
   messageLockExpiry may be used to set the number of milliseconds after which
   messages locked to the consumer may be automatically unlocked and made
   available for redelivery. A value of zero is interpreted to mean that locks
   should not expire. It is required by z/OS, because messages may be queued
   for processing by a servant, but never actually processed or deleted, due to
   failure of the servant to which they have been assigned. Because the failure
   of the servant does not cause failure of the process with the original
   ConsumerSession, the messages are not automatically unlocked, as is the case
   in a single-process architecture. The expire of a message lock is not
   considered a failed delivery; it does not affect the RedeliveredCount
   property of the message, and does not count towards the maxFailedDeliveries
   limit of the destination.
   <p>
   The extendedMessageOrderingContext parameter may be used to indicate that
   message delivery should be ordered across multiple ConsumerSessions.
   Normally (for clients other than the JMS implementation) this parameter
   should be set to null.

   @param callback the object to which messages will be delivered
   asynchronously
   @param maxActiveMessages the maximum number of unconsumed messages that may
   be assigned to this consumer. This value ONLY takes effect if the session is bifurcatable
   @param messageLockExpiry the number of milliseconds after which locked
   messages may be returned to the unassigned state and removed from the LockedMessageEnumeration
   @param maxBatchSize the maximum number of messages that may be delivered at
   a time to the callback objects consumeMessages method
   @param extendedMessageOrderingContext indicates that message delivery should
   be ordered across multiple ConsumerSessions on the same queue point or subscription (may be null)

   @throws com.ibm.wsspi.sib.core.exception.SISessionUnavailableException
   @throws com.ibm.wsspi.sib.core.exception.SISessionDroppedException
   @throws com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException
   @throws com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException
   @throws com.ibm.websphere.sib.exception.SIIncorrectCallException
  */
  public void registerAsynchConsumerCallback(
      AsynchConsumerCallback callback,
      int maxActiveMessages,
      long messageLockExpiry,
      int maxBatchSize,
      OrderingContext extendedMessageOrderingContext)
    throws SISessionUnavailableException, SISessionDroppedException,
           SIConnectionUnavailableException, SIConnectionDroppedException,
           SIIncorrectCallException;

  /**
   Deregisters the callback used to deliver messages asynchronously, and causes
   the ConsumerSession to transition from asynchronous to synchronous delivery
   mode.  deregisterAsynchConsumerCallback may only be called on a stopped
   ConsumerSession; otherwise, SIIncorrectCallException is thrown.
   This implies that a callback may not deregister itself, since a
   ConsumerSession does not actually stop until consumeMessages returns.
   Messages currently locked to this ConsumerSession will not be unlocked as
   a result of deregistering the callback.

   @throws com.ibm.wsspi.sib.core.exception.SISessionUnavailableException
   @throws com.ibm.wsspi.sib.core.exception.SISessionDroppedException
   @throws com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException
   @throws com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException
   @throws com.ibm.websphere.sib.exception.SIIncorrectCallException
  */
  public void deregisterAsynchConsumerCallback()
    throws SISessionUnavailableException, SISessionDroppedException,
           SIConnectionUnavailableException, SIConnectionDroppedException,
           SIIncorrectCallException;

  /**
   Unlocks all messages currently locked to this ConsumerSession. It should be
   noted that any invocation of unlockAll can cause messages to be delivered out
   of sequence (just as with a transaction rollback). When a message is
   unlocked, its redeliveryCount is incremented.

   @throws com.ibm.wsspi.sib.core.exception.SISessionUnavailableException
   @throws com.ibm.wsspi.sib.core.exception.SISessionDroppedException
   @throws com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException
   @throws com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException
   @throws com.ibm.websphere.sib.exception.SIResourceException
   @throws com.ibm.wsspi.sib.core.exception.SIConnectionLostException
  */
  public void unlockAll()
    throws SISessionUnavailableException, SISessionDroppedException,
           SIConnectionUnavailableException, SIConnectionDroppedException,
           SIResourceException, SIConnectionLostException;

  /**
   * Unlocks all messages currently locked to this ConsumerSession. It should be
   * noted that any invocation of unlockAll can cause messages to be delivered
   * out of sequence (just as with a transaction rollback). User has an option
   * to specify whether if the redeliveryCount has to be incremented or not on
   * unlock
   * <p>
   * There are scenarios where its possible that user might have passed false
   * but still the redelivery count might be incremented.
   * <ul>
   * <li>
   * Internally be at the client or the server if any exception occurs, SIB is
   * designed in such a way that it handles the situation by calling unlokcAll()
   * which will result in increment of the redelivery count</li>
   * <li>
   * Any messages that were not viewed by using nextLocked() in
   * consumerMessages(), as per the Core SPI specification any unseen messages
   * will be implicitly unlocked. Thus resulting in the increment of redelivery
   * count(Refer LockedMessageEnumeration.unlockunseen() )</li>
   * <li>
   * unlockAll(fasle) is not supported in the remote ME scenario(i.e client
   * application connected to ME1 and queue point is on ME2).So even though user
   * have passed false for remote ME scenario the redelivery count gets
   * incremented.This is because of the way SIB handles the remote unlock of
   * messages</li>
   * </ul>
   * 
   * @throws com.ibm.wsspi.sib.core.exception.SISessionUnavailableException
   * @throws com.ibm.wsspi.sib.core.exception.SISessionDroppedException
   * @throws com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException
   * @throws com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException
   * @throws com.ibm.websphere.sib.exception.SIResourceException
   * @throws com.ibm.wsspi.sib.core.exception.SIConnectionLostException
   */
  public void unlockAll(boolean incrementUnlockCount)
  throws SISessionUnavailableException, SISessionDroppedException,
  SIConnectionUnavailableException, SIConnectionDroppedException,
  SIResourceException, SIConnectionLostException, SIIncorrectCallException;
  
  /**
   Checks for messages, and if any are available, calls consumeMessages on a
   registered AsynchConsumerCallback, exactly once. This method may only be
   called when the ConsumerSession is in asynchronous mode (has an
   AsynchConsumerCallback registered), and is stopped. Otherwise,
   SIIncorrectCallException is thrown.
   <p>
   If the deliverImmediately parameter has value true, and messages are
   available for this consumer at the time that activateAsynchConsumer is
   called, then the AsynchConsumerSession's consumeMessages method may be called
   on the same thread prior to return from the activateAsynchConsumer method
   itself. If the deliverImmediately paramter has value false, then the
   consumeMessages method will not be called at this time, whether or not there
   are messages available.

   @param deliverImmediately true if consumeMessages can be called before start
   returns

   @throws com.ibm.wsspi.sib.core.exception.SISessionUnavailableException
   @throws com.ibm.wsspi.sib.core.exception.SISessionDroppedException
   @throws com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException
   @throws com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException
   @throws com.ibm.websphere.sib.exception.SIResourceException
   @throws com.ibm.websphere.sib.exception.SIIncorrectCallException
   @throws com.ibm.wsspi.sib.core.exception.SIConnectionLostException
  */
  public void activateAsynchConsumer(boolean deliverImmediately)
    throws SISessionUnavailableException, SISessionDroppedException,
           SIConnectionUnavailableException, SIConnectionDroppedException,
           SIResourceException, SIConnectionLostException, SIIncorrectCallException;

  /**
   This method is used to obtain an identifier for the consumer represented by
   this ConsumerSession, that may be passed to
   SICoreConnection.createBifurcatedConsumerSession. The
   BifurcatedConsumerSession interface enables messages locked to this consumer
   to be read and deleted from a different context.

   @return the ID of the consumer

   @throws com.ibm.wsspi.sib.core.exception.SISessionUnavailableException
   @throws com.ibm.wsspi.sib.core.exception.SISessionDroppedException
   @throws com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException
   @throws com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException
   @throws com.ibm.websphere.sib.exception.SIResourceException
   @throws com.ibm.wsspi.sib.core.exception.SIConnectionLostException
  */
  public long getId()
    throws SISessionUnavailableException, SISessionDroppedException,
           SIConnectionUnavailableException, SIConnectionDroppedException,
           SIResourceException, SIConnectionLostException;
  
  /**
    Registers a stoppable callback to which messages can be delivered asynchronously, and
    causes the ConsumerSession to transition from synchronous to asynchronous
    delivery mode. The client must implement the AsynchronousConsumerCallback
    interface, the consumeMessages method of which is called when messages are
    available for this consumer. The Core API implementation will assign up to
    maxBatchSize messages to the consumer per invocation of consumeMessages.
    registerAsynchConsumerCallback may only be called on a stopped
    ConsumerSession; otherwise, SIIncorrectCallException is thrown.
    <p>
    If the ConsumerSession is already in asynchronous mode, then calling
    registerAsynchConsumerCallback deregisters the existing callback and replaces
    it with that supplied on the call.
    <p>
    If this method is called against a backlevel WebSphere Application Server (prior 
    to v7) then this callback will throw a SIIncorrectCallException
    <p>
    maxActiveMessages sets a limit on the number of messages that may be assigned
    to the consumer at any time. A message is assigned to the consumer if it is
    locked, or if the consumer has issued a delete call for the message, with a
    transaction that has not yet completed (committed or rolled back). Once this
    limit is reached, the Core SPI implementation will cease delivering messages
    to the consumer until some are unlocked or committed (deleted). Note that
    maxActiveMessages is different from maxBatchSize, which determines the
    maximum number of messages that may be delivered to a consumer in a single
    call to AsynchConsumerCallback.consumeMessages. A value
    maxActiveMessages = 0 is interpreted to mean that the consumer places no
    limit on the number of active messages (any configured limit would still
    apply). If 0 < maxActiveMessages < maxBatchSize, then the effective maximum
    batch size will be maxActiveMessages.
    <p>
    messageLockExpiry may be used to set the number of milliseconds after which
    messages locked to the consumer may be automatically unlocked and made
    available for redelivery. A value of zero is interpreted to mean that locks
    should not expire. It is required by z/Vela, because messages may be queued
    for processing by a servant, but never actually processed or deleted, due to
    failure of the servant to which they have been assigned. Because the failure
    of the servant does not cause failure of the process with the original
    ConsumerSession, the messages are not automatically unlocked, as is the case
    in a single-process architecture. The expire of a message lock is not
    considered a failed delivery; it does not affect the RedeliveredCount
    property of the message, and does not count towards the maxFailedDeliveries
    limit of the destination.
    <p>
    The extendedMessageOrderingContext parameter may be used to indicate that
    message delivery should be ordered across multiple ConsumerSessions.
    Normally (for clients other than the JMS implementation) this parameter
    should be set to null.
    <p>
    The maxSequentialFailures parameter is used to indicate when the asynch
    consumer callback object will be told that the consumer session has been
    stopped. A sequential failure is one when a message has failed to be consumer 
    upto (but not including) its max retry count. The maxSequentialFailures 
    indicates how many messages that sequentially failure in this way are 
    allowed till the consumer is stopped.
    <p> If maxSequentialFailures is a positive value a hiddenMessageDelay value
    may be specified. This is the time in milliseconds that a message that has
    rolled back will stay 'hidden' for before being re-available for consumption.
    This delay is applied on each rollback of the message prior to the message
    reaching its maximum retry limit, once that limit is reached the message will be
    exceptioned or the queue will become blocked, as under normal behaviour.
  
    @param callback the object to which messages will be delivered
    asynchronously
    @param maxActiveMessages the maximum number of unconsumed messages that may
    be assigned to this consumer.  This value only takes effect if the session is bifurcatable
    @param messageLockExpiry the number of milliseconds after which locked
    messages may be returned to the unassigned state
    @param maxBatchSize the maximum number of messages that may be delivered at
    a time
    @param extendedMessageOrderingContext indicates that message delivery should
    be ordered across multiple ConsumerSessions (may be null)
    @param maxSequentialFailures indicate the threshold of sequential message
    failures before the consumer is stopped (a value of zero disables automatic stopping).
    @param hiddenMessageDelay Time (in milliseconds) that a message is hidden for before
    it is re-available to a consumer (ignored if maxSequentialFailures is zero)
  
    @throws com.ibm.wsspi.sib.core.exception.SISessionUnavailableException
    @throws com.ibm.wsspi.sib.core.exception.SISessionDroppedException
    @throws com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException
    @throws com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException
    @throws com.ibm.websphere.sib.exception.SIIncorrectCallException
   */
  public void registerStoppableAsynchConsumerCallback(
      StoppableAsynchConsumerCallback callback,
      int maxActiveMessages,
      long messageLockExpiry,
      int maxBatchSize,
      OrderingContext extendedMessageOrderingContext,
      int maxSequentialFailures,
      long hiddenMessageDelay)
    throws SISessionUnavailableException, SISessionDroppedException,
           SIConnectionUnavailableException, SIConnectionDroppedException,
           SIIncorrectCallException;
  
  /**
  Deregisters the stoppablecallback used to deliver messages asynchronously, and causes
  the ConsumerSession to transition from asynchronous to synchronous delivery
  mode.  deregisterStoppableAsynchConsumerCallback may only be called on a stopped
  ConsumerSession; otherwise, SIIncorrectCallException is thrown.
  This implies that a callback may not deregister itself, since a
  ConsumerSession does not actually stop until consumeMessages returns.
  Messages currently locked to this ConsumerSession will not be unlocked as
  a result of deregistering the callback.

  @throws com.ibm.wsspi.sib.core.exception.SISessionUnavailableException
  @throws com.ibm.wsspi.sib.core.exception.SISessionDroppedException
  @throws com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException
  @throws com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException
  @throws com.ibm.websphere.sib.exception.SIIncorrectCallException
 */
 public void deregisterStoppableAsynchConsumerCallback()
   throws SISessionUnavailableException, SISessionDroppedException,
          SIConnectionUnavailableException, SIConnectionDroppedException,
          SIIncorrectCallException;
}

