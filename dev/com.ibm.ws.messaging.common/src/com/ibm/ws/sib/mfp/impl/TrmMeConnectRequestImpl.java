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
package com.ibm.ws.sib.mfp.impl;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.mfp.*;
import com.ibm.ws.sib.mfp.schema.TrmFirstContactAccess;
import com.ibm.ws.sib.mfp.trm.*;
import com.ibm.ws.sib.utils.SIBUuid8;
import com.ibm.ws.sib.utils.ras.SibTr;
import java.util.List;

/**
 * TrmMeConnectRequestImpl extends the general TrmFirstContactMessageImpl
 * and is the implementation class for the TrmMeConnectRequest interface.
 *
 */
public class TrmMeConnectRequestImpl extends TrmFirstContactMessageImpl implements TrmMeConnectRequest  {

  private final static long serialVersionUID = 1L;

  private static TraceComponent tc = SibTr.register(TrmMeConnectRequestImpl.class, MfpConstants.MSG_GROUP, MfpConstants.MSG_BUNDLE);


  /* **************************************************************************/
  /* Constructors                                                             */
  /* **************************************************************************/

  /**
   *  Constructor for a new Jetstream message.
   *
   *  @exception MessageDecodeFailedException Thrown if such a message can not be created
   */
  TrmMeConnectRequestImpl() throws MessageDecodeFailedException {
    super();
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "<init>");

    setMessageType(TrmFirstContactMessageType.ME_CONNECT_REQUEST);
    jmo.setChoiceField(TrmFirstContactAccess.BODY_MECONNECTREQUEST_CREDENTIALS, TrmFirstContactAccess.IS_BODY_MECONNECTREQUEST_CREDENTIALS_EMPTY);
    jmo.setChoiceField(TrmFirstContactAccess.BODY_MECONNECTREQUEST_CREDENTIALTYPE, TrmFirstContactAccess.IS_BODY_MECONNECTREQUEST_CREDENTIALTYPE_EMPTY);
  }

  /**
   *  Constructor for an inbound message.
   *  (Only to be called by TrmFirstContactMessage.makeInboundTrmMeConnectRequest
   *
   *  @param inJmo The JsMsgObject representing the inbound message.
   */
  TrmMeConnectRequestImpl(JsMsgObject inJmo) {
    super(inJmo);
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "<init>, inbound jmo");
  }

  /* **************************************************************************/
  /* Get Methods                                                              */
  /* **************************************************************************/

  /*
   *  Get the Magic Number from the message.
   *
   *  Javadoc description supplied by TrmMeConnectRequest interface.
   */
  public final long getMagicNumber() {
    return jmo.getLongField(TrmFirstContactAccess.BODY_MECONNECTREQUEST_MAGICNUMBER);
  }

  /*
   *  Get the required Bus name from the message.
   *
   *  Javadoc description supplied by TrmMeConnectRequest interface.
   */
  public String getRequiredBusName() {
    return (String)jmo.getField(TrmFirstContactAccess.BODY_MECONNECTREQUEST_REQUIREDBUSNAME);
  }

  /*
   *  Get userid
   *
   *  Javadoc description supplied by TrmMeConnectRequest interface.
   */
  public String getUserid() {
    return (String)jmo.getField(TrmFirstContactAccess.BODY_MECONNECTREQUEST_USERID);
  }

  /*
   *  Get password
   *
   *  Javadoc description supplied by TrmMeConnectRequest interface.
   */
  public String getPassword() {
    return (String)jmo.getField(TrmFirstContactAccess.BODY_MECONNECTREQUEST_PASSWORD);
  }

  /*
   *  Get the required ME name from the message.
   *
   *  Javadoc description supplied by TrmMeConnectRequest interface.
   */
  public String getRequiredMeName() {
    return (String)jmo.getField(TrmFirstContactAccess.BODY_MECONNECTREQUEST_REQUIREDMENAME);
  }

  /*
   *  Get the required Subnet name from the message.
   *
   *  Javadoc description supplied by TrmMeConnectRequest interface.
   */
  public String getRequiredSubnetName() {
    return (String)jmo.getField(TrmFirstContactAccess.BODY_MECONNECTREQUEST_REQUIREDSUBNETNAME);
  }

  /*
   *  Get the requesting ME name from the message.
   *
   *  Javadoc description supplied by TrmMeConnectRequest interface.
   */
  public String getRequestingMeName() {
    return (String)jmo.getField(TrmFirstContactAccess.BODY_MECONNECTREQUEST_REQUESTINGMENAME);
  }

  /*
   *  Get the requesting ME UUID from the message.
   *
   *  Javadoc description supplied by TrmMeConnectRequest interface.
   */
  public SIBUuid8 getRequestingMeUuid() {
    byte[] b = (byte[])jmo.getField(TrmFirstContactAccess.BODY_MECONNECTREQUEST_REQUESTINGMEUUID);
    if (b != null)
      return new SIBUuid8(b);
    return null;
  }

  /*
   *  Get the subnet messaging engines from the message.
   *
   *  Javadoc description supplied by TrmMeConnectRequest interface.
   */
  public List getSubnetMessagingEngines() {
    // Note: Caller does not modify the returned list, so there is
    //       no need to copy it.
    return (List)jmo.getField(TrmFirstContactAccess.BODY_MECONNECTREQUEST_SUBNETMESSAGINGENGINES);
  }

  /*
   * Get the type of the authentication credentials being provided.
   *
   *  Javadoc description supplied by TrmMeConnectRequest interface.
   */
  public String getCredentialType() {
    return (String)jmo.getField(TrmFirstContactAccess.BODY_MECONNECTREQUEST_CREDENTIALTYPE_CREDENTIALTYPE);
  }

  /*
   * Get the authentication token from the message.
   *
   *  Javadoc description supplied by TrmMeConnectRequest interface.
   */
  public byte[] getToken() {
    return (byte[])jmo.getField(TrmFirstContactAccess.BODY_MECONNECTREQUEST_CREDENTIALS_TOKENPARTS_TOKENVALUE);
  }

  /*
   * Get the type of the authentication token from the message.
   *
   *  Javadoc description supplied by TrmMeConnectRequest interface.
   */
  public String getTokenType() {
    return (String)jmo.getField(TrmFirstContactAccess.BODY_MECONNECTREQUEST_CREDENTIALS_TOKENPARTS_TOKENTYPE);
  }

  /* **************************************************************************/
  /* Set Methods                                                              */
  /* **************************************************************************/

  /*
   *  Set the Magic Number field in the message.
   *
   *  Javadoc description supplied by TrmMeConnectRequest interface.
   */
  public final void setMagicNumber(long value) {
    jmo.setLongField(TrmFirstContactAccess.BODY_MECONNECTREQUEST_MAGICNUMBER, value);
  }

  /*
   *  Set the required Bus name in the message.
   *
   *  Javadoc description supplied by TrmMeConnectRequest interface.
   */
  public void setRequiredBusName(String value) {
    jmo.setField(TrmFirstContactAccess.BODY_MECONNECTREQUEST_REQUIREDBUSNAME, value);
  }

  /*
   *  Set the userid
   *
   *  Javadoc description supplied by TrmMeConnectRequest interface.
   */
  public void setUserid(String value) {
    jmo.setField(TrmFirstContactAccess.BODY_MECONNECTREQUEST_USERID, value);
  }

  /*
   *  Set the password
   *
   *  Javadoc description supplied by TrmMeConnectRequest interface.
   */
  public void setPassword(String value) {
    jmo.setField(TrmFirstContactAccess.BODY_MECONNECTREQUEST_PASSWORD, value);
  }

  /*
   *  Set the required ME name in the message.
   *
   *  Javadoc description supplied by TrmMeConnectRequest interface.
   */
  public void setRequiredMeName(String value) {
    jmo.setField(TrmFirstContactAccess.BODY_MECONNECTREQUEST_REQUIREDMENAME, value);
  }

  /*
   *  Set the required Subnet name in the message.
   *
   *  Javadoc description supplied by TrmMeConnectRequest interface.
   */
  public void setRequiredSubnetName(String value) {
    jmo.setField(TrmFirstContactAccess.BODY_MECONNECTREQUEST_REQUIREDSUBNETNAME, value);
  }

  /*
   *  Set the requesting ME name in the message.
   *
   *  Javadoc description supplied by TrmMeConnectRequest interface.
   */
  public void setRequestingMeName(String value) {
    jmo.setField(TrmFirstContactAccess.BODY_MECONNECTREQUEST_REQUESTINGMENAME, value);
  }

  /*
   *  Set the requesting ME UUID in the message.
   *
   *  Javadoc description supplied by TrmMeConnectRequest interface.
   */
  public void setRequestingMeUuid(SIBUuid8 value) {
    if (value != null)
      jmo.setField(TrmFirstContactAccess.BODY_MECONNECTREQUEST_REQUESTINGMEUUID, value.toByteArray());
    else
      jmo.setField(TrmFirstContactAccess.BODY_MECONNECTREQUEST_REQUESTINGMEUUID, null);
  }

  /*
   *  Set the subnet messaging engines in the message.
   *
   *  Javadoc description supplied by TrmMeConnectRequest interface.
   */
  public void setSubnetMessagingEngines(List value) {
    jmo.setField(TrmFirstContactAccess.BODY_MECONNECTREQUEST_SUBNETMESSAGINGENGINES, value);
  }

  /*
   * Set the type of the authentication credentials being provided.
   *
   *  Javadoc description supplied by TrmMeConnectRequest interface.
   */
  public void setCredentialType (String value) {
    jmo.setField(TrmFirstContactAccess.BODY_MECONNECTREQUEST_CREDENTIALTYPE_CREDENTIALTYPE, value);
  }

  /*
   * Set the authentication token from the message.
   *
   *  Javadoc description supplied by TrmMeConnectRequest interface.
   */
  public void setToken(byte[] value) {
    jmo.setField(TrmFirstContactAccess.BODY_MECONNECTREQUEST_CREDENTIALS_TOKENPARTS_TOKENVALUE, value);
  }

  /*
   * Set the type of the authentication token from the message.
   *
   *  Javadoc description supplied by TrmMeConnectRequest interface.
   */
  public void setTokenType(String value) {
    jmo.setField(TrmFirstContactAccess.BODY_MECONNECTREQUEST_CREDENTIALS_TOKENPARTS_TOKENTYPE, value);
  }
}
