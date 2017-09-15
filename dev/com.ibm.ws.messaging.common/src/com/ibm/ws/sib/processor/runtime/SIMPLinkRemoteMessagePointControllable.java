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

public interface SIMPLinkRemoteMessagePointControllable {

  /**
   * Get a single delivery stream set. This exists if we are sending
   * or have sent messages to a remote queue. 
   * 
   * @return The delivery stream or null if it is non existent. 
   */
  public SIMPLinkTransmitterControllable getOutboundTransmit();
  
  /**
   * Is this link transmitter for a topicpsace
   */
  public boolean isPublicationTransmitter();
  
  /**
   * Get the topicspace this publication transmitter is targetting
   * @return the name of the target is isPublicationTransmitter returns true
   * if false - return null
   */
  public String getTargetDestination();
  
}
