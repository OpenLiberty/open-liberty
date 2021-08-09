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
 * ControlSilence extends the general ControlMessage interface and provides
 * get/set methods for the fields specific to a Control Silence Message.
 *
 */
public interface ControlSilence extends ControlMessage {

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
   *  Get the CompletedPrefix from the message.
   *
   *  @return A long containing the CompletedPrefix.
   */
  public long getCompletedPrefix();

  /**
   *  Get the Force value from the message.
   *
   *  @return A boolean containing the value of Force.
   */
  public boolean getForce();

  /**
   *  Get the RequestedOnly value from the message.
   *
   *  @return A boolean containing the value of RequestedOnly.
   */
  public boolean getRequestedOnly();


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
   *  Set the CompletedPrefix in the message.
   *
   *  @param value A long containing the CompletedPrefix.
   */
  public void setCompletedPrefix(long value);

  /**
   *  Set the Force field in the message.
   *
   *  @param value A boolean containing the value of Force.
   */
  public void setForce(boolean value);

  /**
   *  Set the RequestedOnly value in the message.
   *
   *  @param value A boolean containing the value of RequestedOnly.
   */
  public void setRequestedOnly(boolean value);


}
