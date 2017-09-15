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

/**
 * TrmMeBridgeBootstrapRequest extends the general TrmFirstContactMessage
 * interface and provides get/set methods for all fields specific to a
 * TRM Me Bridge Bootstrap request.
 *
 */
public interface TrmMeBridgeBootstrapRequest extends TrmFirstContactMessage {

  /* **************************************************************************/
  /* Get Methods                                                              */
  /* **************************************************************************/

  /**
   *  Get the required Bus from the message.
   *
   *  @return A String containing the required Bus name.
   */
  public String getRequiredBusName();

  /**
   *  Get userid
   *
   *  @return A String containing the userid
   */
  public String getUserid ();

  /**
   *  Get password
   *
   *  @return A String containing the password
   */
  public String getPassword ();

  /**
   *  Get the requesting Bus from the message.
   *
   *  @return A String containing the requesting Bus name.
   */
  public String getRequestingBusName();

  /**
   *  Get the Link name from the message.
   *
   *  @return A string containing the Link name.
   */
  public String getLinkName();

  /**
   *  Get the required transport chain from the message.
   *
   *  @return A string containing the required transport chain.
   */
  public String getRequiredTransportChain();

  /* **************************************************************************/
  /* Set Methods                                                              */
  /* **************************************************************************/

  /**
   *  Set the required Bus name in the message.
   *
   *  @param value A String containing the required Bus name.
   */
  public void setRequiredBusName(String value);

  /**
   *  Set the userid
   *
   *  @param value A String containing the userid
   */
  public void setUserid (String value);

  /**
   *  Set the password
   *
   *  @param value A String containing the password
   */
  public void setPassword (String value);

  /**
   *  Set the requesting Bus name in the message.
   *
   *  @param value A String containing the requesting Bus name.
   */
  public void setRequestingBusName(String value);

  /**
   *  Set the Link name in the message.
   *
   *  @param value A String containing the Link name.
   */
  public void setLinkName(String value);

  /**
   *  Set the required transport chain in the message.
   *
   *  @param value A String containing the required transport chain.
   */
  public void setRequiredTransportChain(String value);

}
