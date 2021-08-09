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
 * Interface to manipulate a subscriber that is located on a remote messaging
 * engine
 */
public interface SIMPAttachedRemoteSubscriberControllable extends SIMPControllable
{
   /**
    * Locates the Topic Space relating to the attached remote subscriber. 
    *
    * @return SIMPTopicSpaceControllable  The topicSpace object.
    */
   SIMPTopicSpaceControllable getTopicSpace();

   /**
    * Locates the remote consumer receiver. 
    *
    * @return SIMPRemoteConsumerReceiverController  a remoteConsumerReceiver. 
    */
   SIMPRemoteConsumerReceiverControllable getRemoteConsumerReceiver();

   /**
    * Locates the consumers attached to the remote subscriber. 
    *
    * @return Iterator  An iterator over all of the Consumer objects. 
    */
   SIMPIterator getConsumerIterator();
   
  /**
   * Returns a string for each topic to which this consumer is subscribed.
   * @return SIMPIterator  An iterator of Strings 
   */
  SIMPIterator getTopicNameIterator();   
  
  /**
   * Clears all of the topics that this remote subscription is currently 
   * subscribed for.
   */
  void clearAllTopics();
}
