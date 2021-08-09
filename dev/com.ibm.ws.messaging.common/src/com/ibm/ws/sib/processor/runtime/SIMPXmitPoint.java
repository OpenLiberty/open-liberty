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

import com.ibm.ws.sib.utils.SIBUuid12;

/**
 * The interface presented by a queueing point localization to perform dynamic
 * control operations.
 * <p>
 * The operations in this interface are specific to a queueing point.
 */
public interface SIMPXmitPoint extends SIMPControllable
{
  /**
   * Get the parent Message handler
   * @return The Message Handler to which this Localization belongs.
   */
  public SIMPMessageHandlerControllable getMessageHandler();

  /**
   * Returns the high messages limit property.
   *
   * @return The destination high messages threshold for this localization.
   */
  public long getDestinationHighMsgs();

  /**
   * Allows the unique id of this localization to be obtained and displayed.
   *
   * @return The unique id of this localization.
   */
  public SIBUuid12 getUUID();

  /**
   * Allows the caller to find out whether this localization accepts messages
   *
   * @return false if the localization prevents new messages being sent, true
   * if further messages may be sent.
   */
  public boolean isSendAllowed();

  /**
   * Allows the caller to stop this localization accepting further messages
   * or not, depending on the value.
   * <p>
   * This has meaning for queueing point
   *
   * @param arg true if messages are to be allowed onto this localization,
   * false if messages are to be prevented being put onto this
   * localization.
   */
  public void setSendAllowed(boolean arg);

  /**
   * Get a single delivery stream set. This exists if we are sending
   * or have sent messages to a remote queue.
   *
   * @return The delivery stream or null if it is non existent.
   */
  public SIMPPtoPOutboundTransmitControllable getPtoPOutboundTransmit();

}
