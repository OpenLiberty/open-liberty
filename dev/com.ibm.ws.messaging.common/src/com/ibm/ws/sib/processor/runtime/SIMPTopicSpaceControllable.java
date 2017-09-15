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

import com.ibm.websphere.sib.exception.SIIncorrectCallException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.sib.processor.exceptions.SIMPControllableNotFoundException;
import com.ibm.ws.sib.processor.exceptions.SIMPException;
import com.ibm.ws.sib.processor.exceptions.SIMPIncorrectCallException;
import com.ibm.ws.sib.processor.exceptions.SIMPInvalidRuntimeIDException;
import com.ibm.wsspi.sib.core.exception.SIDurableSubscriptionNotFoundException;
import com.ibm.wsspi.sib.core.exception.SIDestinationLockedException;

/**
 * The interface presented by a topicspace to perform dynamic
 * control operations.
 */
public interface SIMPTopicSpaceControllable extends SIMPMessageHandlerControllable
{
  /**
   * Get the remote durable subscriptions that consumers in this ME are attached to. 
   *
   * @return An iterator over all of the AttachedRemoteSubscriber summary objects. 
   */
  public SIMPIterator getAttachedRemoteSubscriberIterator() 
    throws SIMPException;

  /**
   * Get the local durable and non durable subscriptionsin this ME 
   * whether they have consumers attached to them or not. 
   *
   * @throws SIMPException if there is a corrupt subscription
   * @return An iterator over all of the LocalSubscription summary objects. 
   */
  public SIMPIterator getLocalSubscriptionIterator() throws SIMPException;
  
  public SIMPLocalSubscriptionControllable getLocalSubscriptionControlByID(String id)
    throws SIMPInvalidRuntimeIDException,
           SIMPControllableNotFoundException,
           SIMPException;
  
  
  /**
   * Returns a SIMPLocalSubscriptionControllable with the specified name.
   * NOTE: this is not to be confused with a subscription ID.
   * The subscription name only applies to Durable subscriptions.
   * Therefore this method will always return either a SIMPLocalSubscriptionControllable
   * for a durable subscription or will throw a SIMPControllableNotFoundException.
   * @param subscriptionName
   * @return a SIMPLocalSubscriptionControllable for the subcription.
   */
  public SIMPLocalSubscriptionControllable getLocalSubscriptionControlByName(String subscriptionName) 
    throws  SIMPException,
            SIMPControllableNotFoundException;
  
  /** Deletes a local durable subscription.
   * 
   * In the case where the subscription no longer exists a SIMPControllableNotFoundException
   * exception is thrown.
   * When the subscription isn't a durable one a SIMPIncorrectCallException exception is thrown   
   * 
   * @param id  The subscription controlable id to delete 
   */         
  public void deleteLocalSubscriptionControlByID(String id)
  throws SIMPInvalidRuntimeIDException, 
         SIMPControllableNotFoundException, 
         SIMPIncorrectCallException,
         SIMPException,
         SIDurableSubscriptionNotFoundException, 
         SIDestinationLockedException, 
         SIResourceException, 
         SIIncorrectCallException;
 

  /**
   * Get the local topic spaces in this ME. 
   *
   * @return An iterator over the LocalTopicSpace summary objects. 
   */
  public SIMPLocalTopicSpaceControllable getLocalTopicSpaceControl();

  /**
   * Locates the remote topic spaces in this ME.
   * Each remote topic space representa a child ME in the Publish Subscribe hierarchy. 
   *
   * @return An iterator over RemoteTopicSpace summary objects. 
   */
  public SIMPIterator getRemoteTopicSpaceIterator();
  public SIMPRemoteTopicSpaceControllable getRemoteTopicSpaceControlByID(String id) 
  throws SIMPControllableNotFoundException;
  
}
