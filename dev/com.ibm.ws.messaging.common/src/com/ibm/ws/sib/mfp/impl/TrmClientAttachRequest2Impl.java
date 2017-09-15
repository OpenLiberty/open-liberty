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
import com.ibm.ws.sib.mfp.MessageDecodeFailedException;
import com.ibm.ws.sib.mfp.MfpConstants;
import com.ibm.ws.sib.mfp.schema.TrmFirstContactAccess;
import com.ibm.ws.sib.mfp.trm.TrmClientAttachRequest2;
import com.ibm.ws.sib.mfp.trm.TrmFirstContactMessageType;
import com.ibm.ws.sib.utils.SIBUuid8;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * TrmClientAttachRequest2Impl extends the general TrmFirstContactMessageImpl
 * and is the implementation class for the TrmClientAttachRequest2 interface.
 *
 */
public class TrmClientAttachRequest2Impl extends TrmFirstContactMessageImpl implements TrmClientAttachRequest2  {

  private final static long serialVersionUID = 1L;

  private static TraceComponent tc = SibTr.register(TrmClientAttachRequest2Impl.class, MfpConstants.MSG_GROUP, MfpConstants.MSG_BUNDLE);

  /* **************************************************************************/
  /* Constructors                                                             */
  /* **************************************************************************/

  /**
   *  Constructor for a new Jetstream message.
   *
   *  @exception MessageDecodeFailedException Thrown if such a message can not be created
   */
  TrmClientAttachRequest2Impl() throws MessageDecodeFailedException {
    super();
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "<init>");

    setMessageType(TrmFirstContactMessageType.CLIENT_ATTACH_REQUEST2);
  }

  /**
   *  Constructor for an inbound message.
   *  (Only to be called by TrmFirstContactMessage.makeInboundTrmClientAttachRequest
   *
   *  @param inJmo The JsMsgObject representing the inbound message.
   */
  TrmClientAttachRequest2Impl(JsMsgObject inJmo) {
    super(inJmo);
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "<init>, inbound jmo ");
  }

  /* **************************************************************************/
  /* Get Methods                                                              */
  /* **************************************************************************/

  /*
   *  Get the bus name
   *
   *  Javadoc description supplied by TrmClientAttachRequest2 interface.
   */
  public String getRequiredBusName() {
    return (String)jmo.getField(TrmFirstContactAccess.BODY_CLIENTATTACHREQUEST2_REQUIREDBUSNAME);
  }

  /*
   *  Get the credential type
   *
   *  Javadoc description supplied by TrmClientAttachRequest2 interface.
   */
  public String getCredentialType() {
    return (String)jmo.getField(TrmFirstContactAccess.BODY_CLIENTATTACHREQUEST2_CREDENTIALTYPE);
  }

  /*
   *  Get the userid
   *
   *  Javadoc description supplied by TrmClientAttachRequest2 interface.
   */
  public String getUserid() {
    return (String)jmo.getField(TrmFirstContactAccess.BODY_CLIENTATTACHREQUEST2_USERID);
  }

  /*
   *  Get the password
   *
   *  Javadoc description supplied by TrmClientAttachRequest2 interface.
   */
  public String getPassword() {
    return (String)jmo.getField(TrmFirstContactAccess.BODY_CLIENTATTACHREQUEST2_PASSWORD);
  }

  /*
   *  Get the messaging engine uuid from the message.
   *
   *  Javadoc description supplied by TrmClientAttachRequest2 interface.
   */
  public SIBUuid8 getMeUuid() {
    byte[] b = (byte[])jmo.getField(TrmFirstContactAccess.BODY_CLIENTATTACHREQUEST2_MEUUID);
    if (b != null)
      return new SIBUuid8(b);
    return null;
  }

  /* **************************************************************************/
  /* Set Methods                                                              */
  /* **************************************************************************/

  /*
   *  Set the bus name
   *
   *  Javadoc description supplied by TrmClientAttachRequest2 interface.
   */
  public void setRequiredBusName(String value) {
    jmo.setField(TrmFirstContactAccess.BODY_CLIENTATTACHREQUEST2_REQUIREDBUSNAME, value);
  }

  /*
   *  Set the credential type
   *
   *  Javadoc description supplied by TrmClientAttachRequest2 interface.
   */
  public void setCredentialType(String value) {
    jmo.setField(TrmFirstContactAccess.BODY_CLIENTATTACHREQUEST2_CREDENTIALTYPE, value);
  }

  /*
   *  Set the userid
   *
   *  Javadoc description supplied by TrmClientAttachRequest2 interface.
   */
  public void setUserid(String value) {
    jmo.setField(TrmFirstContactAccess.BODY_CLIENTATTACHREQUEST2_USERID, value);
  }

  /*
   *  Set the password
   *
   *  Javadoc description supplied by TrmClientAttachRequest2 interface.
   */
  public void setPassword(String value) {
    jmo.setField(TrmFirstContactAccess.BODY_CLIENTATTACHREQUEST2_PASSWORD, value);
  }

  /*
   *  Set the messaging engine uuid in the message.
   *
   *  Javadoc description supplied by TrmClientAttachRequest2 interface.
   */
  public void setMeUuid(SIBUuid8 value) {
    if (value != null)
      jmo.setField(TrmFirstContactAccess.BODY_CLIENTATTACHREQUEST2_MEUUID, value.toByteArray());
    else
      jmo.setField(TrmFirstContactAccess.BODY_CLIENTATTACHREQUEST2_MEUUID, null);
  }

}
