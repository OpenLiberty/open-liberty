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
 * TrmClientAttachRequest extends the general TrmFirstContactMessage
 * interface and provides get/set methods for all fields specific to a
 * TRM Client Attach request.
 *
 */
public interface TrmClientAttachRequest extends TrmFirstContactMessage {

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
  public String getUserid ();

  /**
   *  Get password
   *
   *  @return A String containing the password
   */
  public String getPassword ();

  /**
   *  Get the ME name from the message.
   *
   *  @return A String containing the ME name.
   */
  public String getMeName();

  /**
   *  Get the subnet name from the message.
   *
   *  @return A String containing the subnet name.
   */
  public String getSubnetName();
  
  /**
   *  Get the Security Token from the message.
   *
   *  @return A byte[] containing the security token.
   */
  public byte[] getToken();
  
  /**
   *  Get the Security Token Type from the message.
   *
   *  @return A String containing the security token type.
   */
  public String getTokenType();


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
   *  Set the ME name in the message.
   *
   *  @param value A String containing the ME name.
   */
  public void setMeName(String value);

  /**
   *  Set the subnet name in the message.
   *
   *  @param value A String containing the subnet name.
   */
  public void setSubnetName(String value);
  
  /**
   *  Set the Security Token in the message.
   *
   *  @param value A byte[] containing the security token.
   */
  public void setToken(byte[] value);
  
  /**
   *  Set the Security Token Type from the message.
   *
   *  @param A String containing the security token type.
   */
  public void setTokenType(String value);

}
