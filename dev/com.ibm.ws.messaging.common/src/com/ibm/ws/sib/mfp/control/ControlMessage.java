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
package com.ibm.ws.sib.mfp.control;

import com.ibm.websphere.sib.Reliability;
import com.ibm.ws.sib.mfp.AbstractMessage;

/**
 * ControlMessage is the basic interface for accessing and processing any
 * Message Processor Control Messages.
 * <p>
 * All of the Control  messages are specializations of ControlMessage which is
 * a separate top level message and not an extension of SIBusMessage.
 * The ControlMessage interface provides get/set methods for the common
 * Control message fields and extends AbstractMessage which provides
 * get/set methods for the fields common to Control messages and JsMessages.
 */
public interface ControlMessage extends AbstractMessage {

  /* **************************************************************************/
  /* Get Methods                                                              */
  /* **************************************************************************/

  /**
   * Get the value of the ControlMessageType from the  message.
   *
   * @return The ControlMessageType singleton which distinguishes
   *          the type of this message.
   */
  public ControlMessageType getControlMessageType();

  /**
   *  Get the value of the Reliability field from the message header.
   *
   *  @return The Reliability instance representing the Reliability of the
   *          message (i.e. Express, Reliable or Assured).
   *          Reliability.UNKNOWN is returned if the field is not set.
   */
  public Reliability getReliability();


  /* **************************************************************************/
  /* Set Methods                                                              */
  /* **************************************************************************/

  /**
   *  Set the value of the Priority field in the message header.
   *
   *  @param value An int containing the Priority of the message.
   *
   *  @exception IllegalArgumentException The value given is outside the
   *             permitted range.
   */
  public void setPriority(int value);

  /**
   *  Set the value of the Reliability field in the message header.
   *
   *  @return The Reliability instance representing the reliability of the
   *          message (i.e. Express, Reliable or Assured).
   *
   *  @exception NullPointerException Null is not a valid Reliability.
   */
  public void setReliability(Reliability value);


}
