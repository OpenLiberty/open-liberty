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

import java.util.Map;

import com.ibm.ws.sib.utils.SIBUuid12;

/**
 * 
 */
public interface SIMPMessageHandlerControllable extends SIMPControllable
{
  /**
   * Locates the Message Processor relating to the known destination. 
   *
   * @return SIMPMessageProcessorControllable  The Message Processor object. 
   */
  SIMPMessageProcessorControllable getMessageProcessor();
  
  /**
   * Get the browsers directly addressing this message handler. 
   *
   * @return An iterator over all of the SIMPBrowserControllable objects. 
   */
  public SIMPIterator getBrowserIterator();
  /**
   * Get the consumers directly addressing this message handler. 
   *
   * @return An iterator over all of the SIMPConsumerControllable objects.
   */
  public SIMPIterator getConsumerIterator();
  /**
   * Get the producers directly addressing this message handler. 
   *
   * @return An iterator over all of the SIMPProducerControllable objects. 
   */
  public SIMPIterator getProducerIterator();  

  
  /**
   * Is this destination a local destination, defined on this bus?
   * @return true if this is local
   */
  public boolean isLocal();

  /**
   * Is this destination an alias to another destination, possibly defined
   * on this bus?
   * @return true if this is an alias
   */
  public boolean isAlias();

  /**
   * Determines if the destination is a system destination.  
   * @return true if the queue is a system destination. 
   */
  public boolean isSystem();

  /**
   * Determines if the destination is a temporary destination.  
   * @return true if the queue is a temporary destination. 
   */
  public boolean isTemporary();

  /**
   * Determines if the state of a destination.  
   * @return the state of a destination. 
   */
  public String getState();

  /**
   * Is this a foreign destination, indicating that the referenced destination
   * on the foreign bus exists?
   * @return true if this is foreign.
   */
  public boolean isForeign();

  public String getDescription();
  public Map getDestinationContext();
  public SIBUuid12 getUUID();
  
}
