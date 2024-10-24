/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.mfp.control;

/**
 * ControlNack extends the general ControlMessage interface and provides
 * get/set methods for the fields specific to a Control Nack Message.
 *
 */
public interface ControlNack extends ControlMessage {

  /* **************************************************************************/
  /* Get Methods                                                              */
  /* **************************************************************************/

  /**
   *  Get the Start Tick value from the message.
   *
   *  @return A long containing the Start Tick value.
   */
  public long getStartTick();

  /**
   *  Get the End Tick value from the message.
   *
   *  @return A long containing the End Tick value.
   */
  public long getEndTick();

  /**
   *  Get the Force value from the message.
   *
   *  @return A boolean containing the value of Force.
   */
  public boolean getForce();


  /* **************************************************************************/
  /* Set Methods                                                              */
  /* **************************************************************************/

  /**
   *  Set the Start Tick value in the message.
   *
   *  @param value A long containing the Start Tick value.
   */
  public void setStartTick(long value);

  /**
   *  Set the End Tick value in the message.
   *
   *  @param value A long containing the End Tick value.
   */
  public void setEndTick(long value);

  /**
   *  Set the Force field in the message.
   *
   *  @param value A boolean containing the value of Force.
   */
  public void setForce(boolean value);

}
