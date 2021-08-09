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

import java.util.List;

/**
 * TrmClientBootstrapReply extends the general TrmFirstContactMessage
 * interface and provides get/set methods for all fields specific to a
 * TRM Client Bootstrap reply.
 *
 */
public interface TrmClientBootstrapReply extends TrmFirstContactMessage {

  /* **************************************************************************/
  /* Get Methods                                                              */
  /* **************************************************************************/

  /**
   *  Get the Return Code from the message.
   *
   *  @return An Integer return code.
   */
  public Integer getReturnCode();

  /**
   *  Get the failure reason from the message.
   *
   *  @return A List of Strings containing the failure reason, if any.
   *          If there was not a failure, null will be returned.
   */
  public List getFailureReason();

  /**
   *  Get the redirection end point data.
   *
   *  @return A byte[] for the end point
   */
  public byte[] getEndPointData();

  /**
   *  Get the redirection bus name.
   *
   *  @return A String bus name.
   */
  public String getBusName();

  /**
   *  Get the redirection subnet name.
   *
   *  @return A String subnet name.
   */
  public String getSubnetName();

  /**
   *  Get the redirection messaging engine name.
   *
   *  @return A String messaging engine name.
   */
  public String getMessagingEngineName();

  /* **************************************************************************/
  /* Set Methods                                                              */
  /* **************************************************************************/

  /**
   *  Set the Return Code in the message.
   *
   *  @param value An int return code.
   */
  public void setReturnCode(int value);

  /**
   *  Set the failure reason in the message.
   *
   *  @param value A List of Strings containing the failure reason.
   */
  public void setFailureReason(List value);

  /**
   *  Set the redirection end point data.
   *
   *  @param value A byte[] for the end point data.
   */
  public void setEndPointData(byte[] value);

  /**
   *  Set the redirection bus name.
   *
   *  @param value A String bus name.
   */
  public void setBusName(String value);

  /**
   *  Set the redirection subnet name.
   *
   *  @param value A String subnet name.
   */
  public void setSubnetName(String value);

  /**
   *  Set the redirection messaging engine name.
   *
   *  @param value A String messaging engine name.
   */
  public void setMessagingEngineName(String value);

}
