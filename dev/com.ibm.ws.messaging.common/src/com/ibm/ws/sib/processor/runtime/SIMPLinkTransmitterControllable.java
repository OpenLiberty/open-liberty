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

import com.ibm.ws.sib.processor.exceptions.SIMPControllableNotFoundException;

public interface SIMPLinkTransmitterControllable extends 
  SIMPPtoPOutboundTransmitControllable {
  
  /**
   * Time since the last message was sent
   * @return 
   */
  public long getTimeSinceLastMessageSent()
    throws SIMPControllableNotFoundException;
  
  /**
   * Is this destination put disabled?
   * @return
   */
  public boolean isPutInhibited();
  
  /**
   * Get a string showing the type of the link, i.e. "SIB" or "MQ"
   * 
   * @return String The link Type
   */
  public String getLinkType();
  
  /**
   * Get the Uuid of the Link
   * @return String The Unique id of the Link
   */
  public String getLinkUuid();
  
  /**
   * Get the name of the link
   * @return The Name of the link
   */
  public String getLinkName();
  
  /**
   * Get the bus name where this link is transmitting to
   * @return The bus name
   */
  public String getTargetBusName();
  
  /**
   * Get the Messaging engine uuid where this link is targetted at
   * @return The uuid of the target messaging engine of the link
   */
  public String getTargetEngineUuid();

}
