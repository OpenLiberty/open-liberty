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
package com.ibm.ws.sib.processor.impl.interfaces;

import java.util.concurrent.locks.Lock;

import com.ibm.websphere.sib.exception.SIException;
import com.ibm.ws.sib.processor.impl.ConsumerDispatcher;
import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.ws.sib.utils.SIBUuid8;

/**
 * The interface for all ConsumerPoints, local and remote
 * 
 * @author tevans
 */
public interface DispatchableConsumerPoint extends ConsumerPoint, Lock
{
  
  //These values are OR'd together, so only use x2 values
  final static int SUSPEND_FLAG_RETRY_TIMER = 1;
  final static int SUSPEND_FLAG_ACTIVE_MSGS = 2;
  final static int SUSPEND_FLAG_INITIAL_INDOUBTS = 4;
  final static int SUSPEND_FLAG_SET_ACTIVE_MSGS = 8;
  final static int SUSPEND_FLAG_MAX_HIDDEN_MSGS_REACHED = 16;
  
  /**
   * Put the message on to this consumer point. Checks should
   * have already been performed to make sure the consumer is ready
   * for a new message.
   * 
   * @param msg The message to be put
   * @param isOnItemStream true if this message being put is stored on
   * an ItemStream
   * @return true if the message was accepted by this consumer 
   */
  public boolean put(SIMPMessage msg, boolean isOnItemStream);
  
  /**
   * Delivers an exception to a ConsumerPoint. This is required
   * due to the asychronicity of CPs 
   * @param e Exception specific to the error being reported
   */
  void notifyException(Throwable e);

  /**
   * Notify ConsumerPoint with the state of Receive Allowed for the localization. 
   * @param isAllowed - New state of Receive Allowed for localization
   */
  void notifyReceiveAllowed(boolean isAllowed);
  
  /**
   * Returns true if the passed DestinationHandler matches the destination
   * handler that the consumerpoint was created against.
   * @param destinationHandlerToCompare
   * @param cd The consumerdispatcher for the base destination
   */
  boolean destinationMatches(DestinationHandler destinationHandlerToCompare, JSConsumerManager cd);
  
  /**
   * Returns the destination that the local consumer point is attached
   * through
   * @param cd The consumerdispatcher for the base destination
   */
  DestinationHandler getNamedDestination(ConsumerDispatcher cd);
  
  /**
   * Spin off a thread that checks for any stored messages.
   * This is called by a ConsumerKeyGroup to try to kick a group back
   * into life after a stopped member detaches and makes the group ready
   * again.
   */
  void checkForMessages();
  
  /**
   * Suspend this consumer from receiving messages until resume is
   * called. Used by the retry count timer to suspend a consumer until
   * the timer alarm is triggered. The retry timer is triggered after 
   * the retry count is reached and the message has nowhere to go.
   * 
   * @return boolean true if the consumer was suspended due to this call.
   *          false, if the consumer was not suspended due to this call i.e. the
   *          consumer was already suspended.
   */
  boolean suspendConsumer(int flags);
  
  /**
   * Resume this consumer to allow receive of messages 
   * Used by the retry count timer to resume a consumer when
   * the timer alarm is triggered. The retry timer is triggered after 
   * the retry count is reached and the message has nowhere to go.
   */
  void resumeConsumer(int flags);
 
  
  /**
   * Indicate whether the consumer requires exclusive access to the destination
   * for ordering purposes. If not, we should ignore indoubt msgs, i.e default = true
   * @return
   */
  boolean ignoreInitialIndoubts();
  
  /**
   * Called when the consumer gets implicitly closed by either a receive exclusive, destination
   * delete, or ME unreachable
   * @param destinationBeingDeleted
   * @param exception
   */
  public void implicitClose(SIBUuid12 deletedUuid, SIException exception, SIBUuid8 qpoint);
  
  /**
   * Indicate as to whether this consumer is suspended. A consumer is suspended
   * by using the suspendConsumer() method on this object. resumeConsumer should
   * unsuspend this consumer. Us the Suspend_Flags to determine the reason for the suspend.
   * 
   * @param suspendFlag is used to ask is the consumer suspended for this suspend flag i.e.
   *                if the consumer was suspended for Retry and the Active_Msgs flag was passed
   *                into this method then it will return false as the consumer is not suspended 
   *                for Active_Msgs. Passing a 0 value will mean return if the consumer is suspended
   *                regardless as to how it was suspended.
   * 
   * @return boolean where true means the consumer is suspended for that suspend flag.
   */
  boolean isConsumerSuspended(int suspendFlag);

  /**
   * Gets the consumerKey in use by the consumerPoint
   * @return
   */
  public ConsumableKey getConsumerKey();

  
}
