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
 * ControlReject extends the general ControlMessage interface and provides
 * get/set methods for the fields specific to a Control Reject Message.
 *
 */
public interface ControlReject extends ControlMessage {

  /* **************************************************************************/
  /* Get Methods                                                              */
  /* **************************************************************************/

  /**
   * Get the Start Tick value for this request.
   *
   * @return A long[] containing the Tick values
   */
  public long[] getStartTick();

  /**
   * Get the End Tick value for this request.
   *
   * @return A long[] containing the End Tick values
   */
  public long[] getEndTick();

  /**
   * Get the recovery value for this request.
   *
   * @return a boolean true if these ticks were turned to rejected
   * state during crash recovery.
   */
  public boolean getRecovery();

  /**
   * Get the RMEUnlockCountValue for this request.
   *
   * @return A long[] which are the RMEUnlockCount's.
   *         NB: The value will always be a 0-length array if the message arrived from a pre-WAS7 ME.
   */
  public long[] getRMEUnlockCount();

  /* **************************************************************************/
  /* Set Methods                                                              */
  /* **************************************************************************/

  /**
   * Set the Start Tick values for this request.
   *
   * @param values A long[] containing the Start Tick values
   */
  public void setStartTick(long[] values);

  /**
   * Set the End Tick values for this request.
   *
   * @param values A long[] containing the End Tick values
   */
  public void setEndTick(long[] values);

  /**
   * Set the Recovery value for this request
   *
   * @param value A boolean containing the recovery value
   */
  public void setRecovery(boolean value);

  /**
   * Set the RMEUnlockCountValue for this request.
   *
   * @param value A long[] containing the RMEUnlockCountValue's
   */
  public void setRMEUnlockCount(long[] value);

}
