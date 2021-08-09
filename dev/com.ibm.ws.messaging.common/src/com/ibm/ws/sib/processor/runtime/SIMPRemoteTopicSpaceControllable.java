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
public interface SIMPRemoteTopicSpaceControllable extends SIMPControllable
{
  /**
   * Locates the Topic Space relating to the attached remote subscriber. 
   *
   * @return TopicSpace  The TopicSpace object. 
   *
   */
  SIMPTopicSpaceControllable getTopicSpace();
  
  /**
   * Locates the input to the pub sub heierarchy. 
   * This exists even if we are the head of the hierarchy. 
   *
   * @deprecated because there should only be one transmit stream set to 
   * a remote topic space.
   * Use getPubSubOutboundTransmitControllable instead
   * 
   * @return SIMPPubSubtOutBoundTransmitControllable  a PubSubOutboundTransmit. 
   */
  SIMPIterator getPubSubOutboundTransmitIterator();
  

  /**
   * Returns a string for each topic that the RemoteTopicSpace has subscribed for
   * on this ME
   * @return SIMPIterator  An iterator over all of the String objects representin topic names subscribed for. 
   */
  SIMPIterator getTopicNameIterator();
  
  /**
   * @return The messaging engine name
   * @author tpm
   */
  String getMessagingEngineName();
  
  /**
   * 
   * @return The SIMPPubSubOutboundTransmitControllable controlling messages being
   * transmitted to this remote publication point
   * @author tpm
   */
  SIMPPubSubOutboundTransmitControllable getPubSubOutboundTransmitControllable();
  
  /**
   * Clear all of the remote topics that the remote topic space has 
   * subscribed for on this ME. 
   * @author tpm
   */
  public void clearTopics();
  
  /**
   * Get an iterator of the remote subscription points
   * @return An iterator with elements of type 
   * SIMPAttachedRemoteSubscriber
   * @author tpm
   */
  SIMPIterator getRemoteSubscriptions();
}
