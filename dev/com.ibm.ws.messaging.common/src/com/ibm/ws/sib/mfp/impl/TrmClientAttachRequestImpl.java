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

import com.ibm.ws.sib.mfp.MessageDecodeFailedException;
import com.ibm.ws.sib.mfp.MfpConstants;
import com.ibm.ws.sib.mfp.schema.TrmFirstContactAccess;
import com.ibm.ws.sib.mfp.trm.TrmClientAttachRequest;
import com.ibm.ws.sib.mfp.trm.TrmFirstContactMessageType;
import com.ibm.ws.sib.utils.ras.SibTr;

import com.ibm.websphere.ras.TraceComponent;

/**
 * TrmClientAttachRequestImpl extends the general TrmFirstContactMessageImpl
 * and is the implementation class for the TrmClientAttachRequest interface.
 *
 */
public class TrmClientAttachRequestImpl extends TrmFirstContactMessageImpl implements TrmClientAttachRequest  {

  private final static long serialVersionUID = 1L;

  private static TraceComponent tc = SibTr.register(TrmClientAttachRequestImpl.class, MfpConstants.MSG_GROUP, MfpConstants.MSG_BUNDLE);

  /* **************************************************************************/
  /* Constructors                                                             */
  /* **************************************************************************/

  /**
   *  Constructor for a new Jetstream message.
   *
   *  @exception MessageDecodeFailedException Thrown if such a message can not be created
   */
  TrmClientAttachRequestImpl() throws MessageDecodeFailedException {
    super();
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "<init>");

    setMessageType(TrmFirstContactMessageType.CLIENT_ATTACH_REQUEST);
    jmo.setChoiceField(TrmFirstContactAccess.BODY_CLIENTATTACHREQUEST_CREDENTIALS, TrmFirstContactAccess.IS_BODY_CLIENTATTACHREQUEST_CREDENTIALS_EMPTY);
  }

  /**
   *  Constructor for an inbound message.
   *  (Only to be called by TrmFirstContactMessage.makeInboundTrmClientAttachRequest
   *
   *  @param inJmo The JsMsgObject representing the inbound message.
   */
  TrmClientAttachRequestImpl(JsMsgObject inJmo) {
    super(inJmo);
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "<init>, inbound jmo ");
  }


  /* **************************************************************************/
  /* Get Methods                                                              */
  /* **************************************************************************/

  /*
   *  Get the Bus name from the message.
   *
   *  Javadoc description supplied by TrmClientBootstrapRequest interface.
   */
  public String getBusName() {
    return (String)jmo.getField(TrmFirstContactAccess.BODY_CLIENTATTACHREQUEST_BUSNAME);
  }

  /*
   *  Get the credential type
   *
   *  Javadoc description supplied by TrmClientBootstrapRequest interface.
   */
  public String getCredentialType() {
    return (String)jmo.getField(TrmFirstContactAccess.BODY_CLIENTATTACHREQUEST_CREDENTIALTYPE);
  }

  /*
   *  Get the userid
   *
   *  Javadoc description supplied by TrmClientBootstrapRequest interface.
   */
  public String getUserid() {
    return (String)jmo.getField(TrmFirstContactAccess.BODY_CLIENTATTACHREQUEST_USERID);
  }

  /*
   *  Get the password
   *
   *  Javadoc description supplied by TrmClientBootstrapRequest interface.
   */
  public String getPassword() {
    return (String)jmo.getField(TrmFirstContactAccess.BODY_CLIENTATTACHREQUEST_PASSWORD);
  }

  /*
   *  Get the ME name from the message.
   *
   *  Javadoc description supplied by TrmClientBootstrapRequest interface.
   */
  public String getMeName() {
    return (String)jmo.getField(TrmFirstContactAccess.BODY_CLIENTATTACHREQUEST_MENAME);
  }

  /*
   *  Get the subnet name from the message.
   *
   *  Javadoc description supplied by TrmClientBootstrapRequest interface.
   */
  public String getSubnetName() {
    return (String)jmo.getField(TrmFirstContactAccess.BODY_CLIENTATTACHREQUEST_SUBNETNAME);
  }

  /*
   *  Get the Security Token
   *
   *  Javadoc description supplied by TrmClientBootstrapRequest interface.
   */
  public byte[] getToken() {
    return (byte[])jmo.getField(TrmFirstContactAccess.BODY_CLIENTATTACHREQUEST_CREDENTIALS_TOKENPARTS_TOKENVALUE);
  }

  /*
   *  Get the Security Token Type
   *
   *  Javadoc description supplied by TrmClientBootstrapRequest interface.
   */
  public String getTokenType() {
    return (String)jmo.getField(TrmFirstContactAccess.BODY_CLIENTATTACHREQUEST_CREDENTIALS_TOKENPARTS_TOKENTYPE);
  }

  /* **************************************************************************/
  /* Set Methods                                                              */
  /* **************************************************************************/

  /*
   *  Set the Bus name in the message.
   *
   *  Javadoc description supplied by TrmClientBootstrapRequest interface.
   */
  public void setBusName(String value) {
    jmo.setField(TrmFirstContactAccess.BODY_CLIENTATTACHREQUEST_BUSNAME, value);
  }

  /*
   *  Set the credential type
   *
   *  Javadoc description supplied by TrmClientBootstrapRequest interface.
   */
  public void setCredentialType(String value) {
    jmo.setField(TrmFirstContactAccess.BODY_CLIENTATTACHREQUEST_CREDENTIALTYPE, value);
  }

  /*
   *  Set the userid
   *
   *  Javadoc description supplied by TrmClientBootstrapRequest interface.
   */
  public void setUserid(String value) {
    jmo.setField(TrmFirstContactAccess.BODY_CLIENTATTACHREQUEST_USERID, value);
  }

  /*
   *  Set the password
   *
   *  Javadoc description supplied by TrmClientBootstrapRequest interface.
   */
  public void setPassword(String value) {
    jmo.setField(TrmFirstContactAccess.BODY_CLIENTATTACHREQUEST_PASSWORD, value);
  }

  /*
   *  Set the ME name in the message.
   *
   *  Javadoc description supplied by TrmClientBootstrapRequest interface.
   */
  public void setMeName(String value) {
    jmo.setField(TrmFirstContactAccess.BODY_CLIENTATTACHREQUEST_MENAME, value);
  }

  /*
   *  Set the subnet name in the message.
   *
   *  Javadoc description supplied by TrmClientBootstrapRequest interface.
   */
  public void setSubnetName(String value) {
    jmo.setField(TrmFirstContactAccess.BODY_CLIENTATTACHREQUEST_SUBNETNAME, value);
  }

  /*
   *  Set the Security Token
   *
   *  Javadoc description supplied by TrmClientBootstrapRequest interface.
   */
  public void setToken(byte[] value) {
    jmo.setField(TrmFirstContactAccess.BODY_CLIENTATTACHREQUEST_CREDENTIALS_TOKENPARTS_TOKENVALUE, value);
  }

  /*
   *  Set the Security Token Type
   *
   *  Javadoc description supplied by TrmClientBootstrapRequest interface.
   */
  public void setTokenType(String value) {
    jmo.setField(TrmFirstContactAccess.BODY_CLIENTATTACHREQUEST_CREDENTIALS_TOKENPARTS_TOKENTYPE, value);
  }
}
