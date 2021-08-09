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

package com.ibm.ws.sib.mfp.trm;

import com.ibm.ws.sib.mfp.JsMessage;

/**
 * TrmMessage is the basic interface for accessing and processing any
 * Topology Routing and Management control Messages.
 * <p>
 * All of the Trm messages are specializations of TrmMessage which is in turn
 * a specialization of JsMessage. All TrmMessages can be made from a JsMessage
 * of the appropriate type.
 */

public interface TrmMessage extends JsMessage {

  /* **************************************************************************/
  /* Get Methods                                                              */
  /* **************************************************************************/

  /**
   *  Get the value of the TrmMessageType from the  message.
   *
   *  @return The TrmMessageType singleton which distinguishes
   *          the type of this message.
   */
  public TrmMessageType getTrmMessageType();

  /**
   *  Get the Magic Number from the message.
   *
   *  @return A long containing the Magic Number.
   */
  public long getMagicNumber();

  /* **************************************************************************/
  /* Set Methods                                                              */
  /* **************************************************************************/

  /**
   *  Set the Magic Number field in the message.
   *
   *  @param value  An long containing the Magic Number.
   */
  public void setMagicNumber(long value);

}
