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

import com.ibm.websphere.sib.exception.SINotPossibleInCurrentConfigurationException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.sib.msgstore.MessageStoreException;
import com.ibm.ws.sib.processor.impl.JSConsumerSet;
import com.ibm.ws.sib.utils.SIBUuid8;

public interface ConsumableKey extends JSConsumerKey {

  ConsumerPoint getConsumerPoint();

  void detach() throws SIResourceException, SINotPossibleInCurrentConfigurationException;

  void start();

  long waiting(long timeout, boolean b);

  void leaveKeyGroup();

  void stop();

  boolean isClosedDueToReceiveExclusive();

  boolean isClosedDueToDelete();

  boolean isClosedDueToLocalizationUnreachable();

  SIMPMessage getMessageLocked() throws MessageStoreException;

  /**
   * Indicates that the caller is about to attempt to lock a message. This
   * method allows the implementor to reject the attempt or track its progress.
   * 
   * Any successful call to this method wil be followed by either commitAddActiveMessage()
   * or rollbackAddActiveMessage()
   */
  boolean prepareAddActiveMessage();

  /**
   * Commits the prepare of an add, see prepareAddActiveMessage()
   */
  void commitAddActiveMessage();

  /**
   * Rolls back the prepare of an add, see prepareAddActiveMessage()
   */
  void rollbackAddActiveMessage();
  
  /**
   * Notifies the key about the removal of an active message. This method supports message 
   * concurrency where SIB is registered with XD
   * 
   * @param messageCount number of messages to remove from the count
   */
  void removeActiveMessages(int messageCount);  
  
  /**
   * Has the Consumer Set associated with this key been suspended because its 
   * concurrent active message limit has been breached. This method supports message 
   * concurrency where SIB is registered with XD
   * 
   * @return
   */
  boolean isConsumerSetSuspended();

  /**
   * Return the Consumer Set associated with this key. This method supports message 
   * concurrency where SIB is registered with XD
   * 
   * @return
   */
  JSConsumerSet getConsumerSet();
  
  /**
   * Called when the consumerpoint has been told to implicitly close
   * @param deleted
   * @param receiveExclusive
   * @param exception
   */
  public boolean close(int closeReason, SIBUuid8 qpoint);

  /**
   * Allow this consumerKey to joing the given keygroup
   * @param keyGroup
   */
  void joinKeyGroup(JSKeyGroup keyGroup) throws SIResourceException;

}
