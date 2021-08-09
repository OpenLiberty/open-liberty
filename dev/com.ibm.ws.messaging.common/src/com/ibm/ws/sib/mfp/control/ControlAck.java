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

/**
 * ControlAck the general ControlMessage interface and provides
 * get/set methods for the fields specific to a Control Ack Message.
 *
 */
public interface ControlAck extends ControlMessage {

  /* **************************************************************************/
  /* Get Methods                                                              */
  /* **************************************************************************/

  /**
   *  Get the Ack Prefix from the message.
   *
   *  @return A long containing the Ack Prefix.
   */
  public long getAckPrefix();


  /* **************************************************************************/
  /* Set Methods                                                              */
  /* **************************************************************************/

  /**
   *  Set the Ack Prefix in the message.
   *
   *  @param value A long containing the Ack Prefix.
   */
  public void setAckPrefix(long value);

}
