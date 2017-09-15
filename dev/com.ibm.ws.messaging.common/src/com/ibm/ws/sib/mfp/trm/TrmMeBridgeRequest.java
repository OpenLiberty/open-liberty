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

import com.ibm.ws.sib.utils.SIBUuid8;

/**
 * TrmMeBridgeRequest extends the general TrmFirstContactMessage
 * interface and provides get/set methods for all fields specific to a
 * TRM ME Bridge request.
 *
 */
public interface TrmMeBridgeRequest extends TrmFirstContactMessage {

  /* **************************************************************************/
  /* Get Methods                                                              */
  /* **************************************************************************/

  /**
   *  Get the Magic Number from the message.
   *
   *  @return A long containing the Magic Number.
   */
  public long getMagicNumber();

  /**
   *  Get the required Bus name from the message.
   *
   *  @return A String containing the Bus name.
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
   *  Get the required ME name from the message.
   *
   *  @return A String containing the ME name.
   */
  public String getRequiredMeName();

  /**
   *  Get the requesting Bus name from the message.
   *
   *  @return A String containing the requesting Bus name.
   */
  public String getRequestingBusName();

  /**
   *  Get the requesting ME name from the message.
   *
   *  @return A String containing the requesting ME name.
   */
  public String getRequestingMeName();

  /**
   *  Get the requesting ME UUID from the message.
   *
   *  @return The requesting ME UUID.
   */
  public SIBUuid8 getRequestingMeUuid();

  /**
   *  Get the Link name from the message.
   *
   *  @return A string containing the Link name.
   */
  public String getLinkName();

  /* **************************************************************************/
  /* Set Methods                                                              */
  /* **************************************************************************/

  /**
   *  Set the Magic Number field in the message.
   *
   *  @param value  An long containing the Magic Number.
   */
  public void setMagicNumber(long value);

  /**
   *  Set the required Bus name in the message.
   *
   *  @param value A String containing the Bus name.
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
   *  Set the required ME name in the message.
   *
   *  @param value A String containing the ME name.
   */
  public void setRequiredMeName(String value);

  /**
   *  Set the requesting Bus name in the message.
   *
   *  @param value A String containing the requesting Bus name.
   */
  public void setRequestingBusName(String value);

  /**
   *  Set the requesting ME name in the message.
   *
   *  @param value A String containing the requesting ME name.
   */
  public void setRequestingMeName(String value);

  /**
   *  Set the requesting ME UUID in the message.
   *
   *  @param value The requesting ME UUID.
   */
  public void setRequestingMeUuid(SIBUuid8 value);

  /**
   *  Set the Link name in the message.
   *
   *  @param value A String containing the Link name.
   */
  public void setLinkName(String value);

}
