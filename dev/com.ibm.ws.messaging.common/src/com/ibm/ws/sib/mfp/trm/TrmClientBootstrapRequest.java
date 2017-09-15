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
 * TrmClientBootstrapRequest extends the general TrmFirstContactMessage
 * interface and provides get/set methods for all fields specific to a
 * TRM Client Bootstrap request.
 *
 */
public interface TrmClientBootstrapRequest extends TrmFirstContactMessage {

  /* **************************************************************************/
  /* Get Methods                                                              */
  /* **************************************************************************/

  /**
   *  Get the Bus name from the message.
   *
   *  @return A String containing the Bus name.
   */
  public String getBusName();

  /**
   *  Get credential type
   *
   *  @return A String containing the credential type
   */
  public String getCredentialType();

  /**
   *  Get userid
   *
   *  @return A String containing the userid
   */
  public String getUserid();

  /**
   *  Get password
   *
   *  @return A String containing the password
   */
  public String getPassword();

  /**
   *  Get the target group name.
   *
   *  @return A string containing the target group name.
   */
  public String getTargetGroupName();

  /**
   *  Get the target group type.
   *
   *  @return A string containing the target group type.
   */
  public String getTargetGroupType();

  /**
   *  Get the target significance.
   *
   *  @return A string containing the target significance.
   */
  public String getTargetSignificance();

  /**
   *  Get the connection proximity.
   *
   *  @return A string containing the connection proximity.
   */
  public String getConnectionProximity();

  /**
   *  Get the target transport chain.
   *
   *  @return A string containing the target transport chain.
   */
  public String getTargetTransportChain();

  /**
   * Get the bootstrap transport chain.
   *
   * @return A string containing the bootstrap transport chain.
   */
  public String getBootstrapTransportChain();

  /**
   * Get the connection mode.
   *
   * @return A string containing the connection mode.
   */
  public String getConnectionMode();                                                                        //250606.1.1

  /* **************************************************************************/
  /* Set Methods                                                              */
  /* **************************************************************************/

  /**
   *  Set the Bus name in the message.
   *
   *  @param value A String containing the Bus name.
   */
  public void setBusName(String value);

  /**
   *  Set the credential type
   *
   *  @param value A String containing the credential type
   */
  public void setCredentialType (String value);

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
   *  Set the target group name.
   *
   *  @param value A string containing the target group name.
   */
  public void setTargetGroupName(String value);

  /**
   *  Set the target group type.
   *
   *  @param value A string containing the target group type.
   */
  public void setTargetGroupType(String value);

  /**
   *  Set the target significance.
   *
   *  @param value A string containing the target significance.
   */
  public void setTargetSignificance(String value);

  /**
   *  Set the connection proximity.
   *
   *  @param value A string containing the connection proximity.
   */
  public void setConnectionProximity(String value);

  /**
   *  Set the target transport chain.
   *
   *  @param value A string containing the target transport chain.
   */
  public void setTargetTransportChain(String value);

  /**
   *  Set the bootstrap transport chain.
   *
   *  @param value A string containing the bootstrap transport chain.
   */
  public void setBootstrapTransportChain(String value);

  /**
   *  Set the connection mode.
   *
   *  @param value A string containing the connection mode.
   */
  public void setConnectionMode(String value);                                                              //250606.1.1

}
