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
package com.ibm.websphere.sib;


/**
 * The SIDestinationAdress is the public interface which represents an SIBus
 * Destination.
 *
 * @ibm-was-base
 * @ibm-api
 */
public interface SIDestinationAddress {


  /* **************************************************************************/
  /* Get methods                                                              */
  /* **************************************************************************/

  /**
   *  Determine whether the SIDestinationAddress represents a Temporary or
   *  Permanent Destination.
   *
   *  @return boolean true if the Destination is Temporary, false if it is permanent.
   */
  public boolean isTemporary();


  /**
   *  Get the name of the Destination represented by this SIDestinationAddress.
   *
   *  @return String The name of the Destination.
   */
  public String getDestinationName();


  /**
   *  Get the Bus name for the Destination represented by this SIDestinationAddress.
   *
   *  @return String The Bus name of the SIDestinationAddress.
   */
  public String getBusName();

}
