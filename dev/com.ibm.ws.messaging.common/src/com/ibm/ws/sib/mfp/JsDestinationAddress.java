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
package com.ibm.ws.sib.mfp;

import java.io.Serializable;

import com.ibm.websphere.sib.SIDestinationAddress;
import com.ibm.ws.sib.utils.SIBUuid8;

/**
 * JsDestinationAddress is the internal interface for Jetstream components to
 * access an SIDestinationAddress. It allows access to all the fields.
 */
public interface JsDestinationAddress extends SIDestinationAddress, Serializable {


  /* **************************************************************************/
  /* Get methods                                                              */
  /* **************************************************************************/

  /**
   *  Determine whether the LocalOnly indicator is set in the SIDestinationAddress.
   *
   *  @return boolean true if the LocalOnly indicator is set.
   */
  public boolean isLocalOnly();

  
  /**
   *  Get the Id of the Message Engine where the Destination is localized.
   *
   *  @return SIBUuid  The Id of the Message Engine where the destination is localized
   *                   Null will be returned if no Localization is set.
   */

  public SIBUuid8 getME();


  /* **************************************************************************/
  /* Set methods                                                              */
  /* **************************************************************************/

  /**
   *  Set the Id of the Message Engine where the Destination is localized.
   *  This method should only be called by the Message Processor component.
   *
   *  @param meId    The Id of the Message Engine where the destination is localized
   *  @exception IllegalStateException if isFromMediation returns true.
   */
  public void setME(SIBUuid8 meId);


  /**
   *  Set the name of the Bus where the Destination is localized.
   *  This method should only be called by the Message Processor component.
   *
   *  @param busName  The name of the Bus where the destination is localized
   *  @exception IllegalStateException if isFromMediation returns true.
   */
  public void setBusName(String busName);


}
