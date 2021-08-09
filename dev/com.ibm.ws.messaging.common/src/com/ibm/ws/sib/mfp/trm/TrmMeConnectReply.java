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
import java.util.List;

/**
 * TrmMeConnectReply extends the general TrmFirstContactMessage
 * interface and provides get/set methods for all fields specific to a
 * TRM ME Connect reply.
 *
 */
public interface TrmMeConnectReply extends TrmFirstContactMessage {

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
   *  Get the Return Code from the message.
   *
   *  @return An Integer return code.
   */
  public Integer getReturnCode();

  /**
   *  Get the replying messaging engine UUID from the message.
   *
   *  @return The replying ME UUID.
   */
  public SIBUuid8 getReplyingMeUuid();

  /**
   * Get the list of subnet messaging engines from the message.
   *
   * @return List of subnet messaging engines.  This List should
   *         be treated as read-only and not modified in any way.
   */
  public List getSubnetMessagingEngines();

  /**
   *  Get the failure reason from the message.
   *
   *  @return A List of Strings containing the failure reason, if any.
   *          If there was not a failure, null will be returned.  The
   *          List should be treated as read-only and not modified in
   *          any way.
   */
  public List getFailureReason();

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
   *  Set the Magic Number field in the message.
   *
   *  @param value  An long containing the Magic Number.
   */
  public void setMagicNumber(long value);

  /**
   *  Set the Return Code in the message.
   *
   *  @param value An int return code.
   */
  public void setReturnCode(int value);

  /**
   *  Set the replying messaging engine UUID in the message.
   *
   *  @param value The replying ME UUID.
   */
  public void setReplyingMeUuid(SIBUuid8 value);

  /**
   * Set the list of subnet messaging engines in the message.
   *
   * @param value List of subnet messaging engines
   */
  public void setSubnetMessagingEngines(List value);

  /**
   *  Set the failure reason in the message.
   *
   *  @param value A List of Strings containing the failure reason.
   */
  public void setFailureReason(List value);
  
  /**
   *  Set the Security Token in the message.
   *
   *  @param value A byte[] containing the security token.
   */
  public void setToken(byte[] value);
  
  /**
   *  Set the Security Token Type from the message.
   *
   *  @param value A String containing the security token type.
   */
  public void setTokenType(String value);
}