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
package com.ibm.ws.sib.processor.runtime;

/**
 * 
 */
public interface SIMPLocalTopicSpaceControllable extends SIMPControllable
{
  /**
   * Locates the Topic Space relating to the attached remote subscriber. 
   *
   * @return SIMPTopicSpaceControllable  The TopicSpace object. 
   *
   */
  SIMPTopicSpaceControllable getTopicSpace();

  /**
   * Returns the number of messages currently on the publication point that
   * have not yet been consumed by all subscribers.
   *  
   * @return The number of unique messages on this localization.
   */
  public long getNumberOfQueuedMessages();
  
  /**
   * Returns the high messages limit property.
   *  
   * @return The destination high messages threshold for this localization.
   */
  public long getDestinationHighMsgs();
  
  /**
   * Allows the caller to find out whether this localization accepts messages
   * 
   * @return false if the localization prevents new messages being put, true
   * if further messages may be put.
   */
  public boolean isSendAllowed();

  /**
   * Allows the mbean user to set the current destination high messages threshold.
   * This value is not persisted.
   * 
   * @param arg The high messages threshold for this localization.
   */
  public void setDestinationHighMsgs(long arg);
  
  /**
   * Allows the caller to stop this localization accepting further messages
   * or not, depending on the value.
   * <p>
   * 
   * @param arg false if messages are to be prevented being put onto this 
   * localization, true if messages are to be allowed onto this localization.
   */
  public void setSendAllowed(boolean arg);

  /**
   * Get an iterator over all of the inbound receivers for this local topicspace.
   * @return an iterator containing SIMPInboundReceiverControllable objects.
   */
  public SIMPIterator getPubSubInboundReceiverIterator();
  
  /**
   * Get an iterator over all of the local subscriptions on this
   * topic space.
   * This iterator contains SIMPLocalSubscriptionControllable
   * @return
   * @author tpm
   */
  public SIMPIterator getLocalSubscriptions();
  
  

}
