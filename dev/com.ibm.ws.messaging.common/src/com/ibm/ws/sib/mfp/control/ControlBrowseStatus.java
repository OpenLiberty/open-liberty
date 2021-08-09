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
 * ControlBrowseStatus extends the general ControlMessage interface and provides
 * get/set methods for the fields specific to a Control Browse Status Message.
 */
public interface ControlBrowseStatus extends ControlMessage  {

  /* **************************************************************************/
  /* Get Methods                                                              */
  /* **************************************************************************/

  /**
   * Get the unique id for this request
   *
   * @return A long containing the browse ID
   */
  public long getBrowseID();

  /**
   * Get the exception code for the browse end
   *
   * @return An int containing the status. 0 ALIVE, 1 CLOSE
   */
  public int getStatus();


  /* **************************************************************************/
  /* Set Methods                                                              */
  /* **************************************************************************/

  /**
   * Set the unique id for this request
   *
   * @param value A long containing the browse ID
   */
  public void setBrowseID(long value);

  /**
   * Set the exception code for the browse end
   *
   * @param value An int containing the status.  0 ALIVE, 1 CLOSE
   */
  public void setStatus(int value);

}
