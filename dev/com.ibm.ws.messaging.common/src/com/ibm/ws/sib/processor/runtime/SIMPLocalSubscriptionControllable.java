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

import com.ibm.ws.sib.processor.exceptions.*;
import com.ibm.ws.sib.processor.exceptions.SIMPException;

/**
 * 
 */
public interface SIMPLocalSubscriptionControllable extends SIMPControllable
{
  /**
   * Locates the Topic Space relating to the attached remote subscriber. 
   *
   * @return SIMPTopicSpaceControllable  The TopicSpace object. 
   */
  public SIMPTopicSpaceControllable getTopicSpace();

  /**
    * Returns the selector.
    * @return String
    */
  public String getSelector();

  /**
   * Returns the subscriberID.
   * @return String
   */
  public String getSubscriberID();

  /**
   * Returns the array of topics.
   * 
   * @return String[]  The array of topics
   */
  public String[] getTopics();

  /**
   * Locates the consumers attached to the local subscription. 
   *
   * @return Iterator  An iterator over all of the Consumer objects. 
   */
  public SIMPIterator getConsumerIterator();

  /**
   * Locates the remote consumer xmit point. This exists of there is a remote consumer
   *  of the messages queued against this subscription. 
   *
   * @return SIMPIterator  a iterator over all the SIMPRemoteConsumerTransmitControllable 
   */
  public SIMPIterator getRemoteConsumerTransmit();

  /**
   * Locates the queued messages. 
   *
   * @return Iterator  An iterator over all of the QueuedMessage objects queued for this local subscription. 
   */
  public SIMPIterator getQueuedMessageIterator();
  public SIMPQueuedMessageControllable getQueuedMessageByID(String ID)
    throws SIMPInvalidRuntimeIDException,
           SIMPControllableNotFoundException,
           SIMPException;
  
  /**
   * Returns the number of queued messages for this subscription.
   */
  public long getNumberOfQueuedMessages();
}
