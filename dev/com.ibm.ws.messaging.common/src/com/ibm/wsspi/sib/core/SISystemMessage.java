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
package com.ibm.wsspi.sib.core;

import com.ibm.websphere.sib.SIMessage;

/**
 * The SISystemMessage interface is the SPI interface to an SIBus message for
 * use by the Mediation Framework as well as other SIBus components.
 * Thiis interfaces allows an SIBus Core SPI user to obtain an SIBusMessage to
 * send to the Bus.
 */
public interface SISystemMessage extends SIMessage {

  /* **************************************************************************/
  /* Method for obtaining the SIBusMessage which represents the same message. */
  /* **************************************************************************/

  /**
   * Obtain the SIBusMessage which represents this message.
   *
   * @return SIBusMessage The corresponding SIBusMessage.
   */
  public SIBusMessage toSIBusMessage();


}
