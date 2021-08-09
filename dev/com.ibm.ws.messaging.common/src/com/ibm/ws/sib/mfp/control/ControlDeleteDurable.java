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
 * ControlDeleteDurable extends the general ControlMessage interface and provides
 * get/set methods for the fields specific to a Control Delete Durable.
 */
public interface ControlDeleteDurable extends ControlMessage {

  /* **************************************************************************/
  /* Get Methods                                                              */
  /* **************************************************************************/

  /**
   * Get the request ID for this request
   *
   * @return A long containing the request ID
   */
  public long getRequestID();

  /**
   * Get the name of the subscription to delete
   *
   * @return A subscription name of the form client##name
   */
  public String getDurableSubName();

  /**
   *  Get the contents of the SecurityUserid field for the subscription.
   *
   *  @return A String containing the SecurityUserid name.
   */
  public String getSecurityUserid();

  /* **************************************************************************/
  /* Set Methods                                                              */
  /* **************************************************************************/

  /**
   * Set the request ID for this request
   *
   * @param value A long containing the request ID
   */
  public void setRequestID(long value);

  /**
   * Set the name of the subscription to delete.
   *
   * @param name A subscription name of the form client##name
   */
  public void setDurableSubName(String name);

  /**
   *  Set the contents of the SecurityUserid field for the subscription.
   *
   *  @param value A String containing the SecurityUserid name.
   */
  public void setSecurityUserid(String value);

}
