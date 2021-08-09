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

public interface SIMPLinkReceiverControllable extends
    SIMPInboundReceiverControllable {
  
  /**
   * Time (ms) since the last message was received
   * @return Time (ms) since the last message was received
   */
  public long getTimeSinceLastMessageReceived();
  
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
   * Get the bus name where this receiver is receiving messages from
   * @return The bus name
   */
  public String getSourceBusName();
  
  /**
   * Get the Messaging engine uuid where this receiver is receiving message from
   * @return The uuid of the source messaging engine
   */
  public String getSourceEngineUuid();
  
  /**
   * Checks if this linkReceiver is receiving message destined for a topicspace.
   * @return 
   */
  public boolean isPublicationReceiver();
  
  /**
   * If this linkreceiver is targetting :
   * a) A topicspace - The topicspace name is returned
   * b) A PtoP destination - null returned
   * c) Target is unkown (we are recovered from a pre-WAS70 ME) - SIMPConstants.UNKNOWN_TARGET_DESTINATION returned
   * @return
   */
  public String getTargetDestinationName();

}
