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
 * ControlResetRequestAck extends the general ControlMessage 
 * interface and provides get/set methods for the fields specific to a 
 * Control Reset Request Ack Message.
 */
public interface ControlResetRequestAck extends ControlMessage {

  /* **************************************************************************/
  /* Get Methods                                                              */
  /* **************************************************************************/

  /**
   * Get the DME version for this request
   *
   * @return A long containing the DME version
   */
  public long getDMEVersion();
    
  /* **************************************************************************/
  /* Set Methods                                                              */
  /* **************************************************************************/

  /**
   * Set the DME version for this request
   * 
   * @param value A long containing the DME version
   */ 
  public void setDMEVersion(long value);
}
