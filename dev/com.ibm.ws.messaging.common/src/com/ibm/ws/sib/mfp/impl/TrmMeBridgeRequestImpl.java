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

/**
 * TrmMeBridgeRequestImpl extends the general TrmFirstContactMessageImpl
 * and is the implementation class for the TrmMeBridgeRequest interface.
 *
 */
public class TrmMeBridgeRequestImpl extends TrmFirstContactMessageImpl implements TrmMeBridgeRequest  {

  private final static long serialVersionUID = 1L;

  private static TraceComponent tc = SibTr.register(TrmMeBridgeRequestImpl.class, MfpConstants.MSG_GROUP, MfpConstants.MSG_BUNDLE);

  /* **************************************************************************/
  /* Constructors                                                             */
  /* **************************************************************************/

  /**
   *  Constructor for a new Jetstream message.
   *
   *  @exception MessageDecodeFailedException Thrown if such a message can not be created
   */
  TrmMeBridgeRequestImpl() throws MessageDecodeFailedException {
    super();
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "<init>");

    setMessageType(TrmFirstContactMessageType.ME_BRIDGE_REQUEST);
  }

  /**
   *  Constructor for an inbound message.
   *  (Only to be called by TrmFirstContactMessage.makeInboundTrmMeBridgeRequest
   *
   *  @param inJmo The JsMsgObject representing the inbound message.
   */
  TrmMeBridgeRequestImpl(JsMsgObject inJmo) {
    super(inJmo);
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "<init>, inbound jmo");
  }

  /* **************************************************************************/
  /* Get Methods                                                              */
  /* **************************************************************************/

  /*
   *  Get the Magic Number from the message.
   *
   *  Javadoc description supplied by TrmMeBridgeRequest interface.
   */
  public final long getMagicNumber() {
    return jmo.getLongField(TrmFirstContactAccess.BODY_MEBRIDGEREQUEST_MAGICNUMBER);
  }

  /*
   *  Get the required Bus name from the message.
   *
   *  Javadoc description supplied by TrmMeBridgeRequest interface.
   */
  public String getRequiredBusName() {
    return (String)jmo.getField(TrmFirstContactAccess.BODY_MEBRIDGEREQUEST_REQUIREDBUSNAME);
  }

  /*
   *  Get the userid
   *
   *  Javadoc description supplied by TrmMeBridgeRequest interface.
   */
  public String getUserid () {
    return (String)jmo.getField(TrmFirstContactAccess.BODY_MEBRIDGEREQUEST_USERID);
  }

  /*
   *  Get the password
   *
   *  Javadoc description supplied by TrmMeBridgeRequest interface.
   */
  public String getPassword () {
    return (String)jmo.getField(TrmFirstContactAccess.BODY_MEBRIDGEREQUEST_PASSWORD);
  }

  /*
   *  Get the required ME name from the message.
   *
   *  Javadoc description supplied by TrmMeBridgeRequest interface.
   */
  public String getRequiredMeName() {
    return (String)jmo.getField(TrmFirstContactAccess.BODY_MEBRIDGEREQUEST_REQUIREDMENAME);
  }

  /*
   *  Get the requesting Bus name from the message.
   *
   *  Javadoc description supplied by TrmMeBridgeRequest interface.
   */
  public String getRequestingBusName() {
    return (String)jmo.getField(TrmFirstContactAccess.BODY_MEBRIDGEREQUEST_REQUESTINGBUSNAME);
  }

  /*
   *  Get the requesting ME name from the message.
   *
   *  Javadoc description supplied by TrmMeBridgeRequest interface.
   */
  public String getRequestingMeName() {
    return (String)jmo.getField(TrmFirstContactAccess.BODY_MEBRIDGEREQUEST_REQUESTINGMENAME);
  }

  /*
   *  Get the requesting ME UUID from the message.
   *
   *  Javadoc description supplied by TrmMeBridgeRequest interface.
   */
  public SIBUuid8 getRequestingMeUuid() {
    byte[] b = (byte[])jmo.getField(TrmFirstContactAccess.BODY_MEBRIDGEREQUEST_REQUESTINGMEUUID);
    if (b != null)
      return new SIBUuid8(b);
    return null;
  }

  /*
   *  Get the Link name from the message.
   *
   *  Javadoc description supplied by TrmMeBridgeRequest interface.
   */
  public String getLinkName(){
    return (String)jmo.getField(TrmFirstContactAccess.BODY_MEBRIDGEREQUEST_LINKNAME);
  }

  /* **************************************************************************/
  /* Set Methods                                                              */
  /* **************************************************************************/

  /*
   *  Set the Magic Number field in the message.
   *
   *  Javadoc description supplied by TrmMeBridgeRequest interface.
   */
  public final void setMagicNumber(long value) {
    jmo.setLongField(TrmFirstContactAccess.BODY_MEBRIDGEREQUEST_MAGICNUMBER, value);
  }

  /*
   *  Set the required Bus name in the message.
   *
   *  Javadoc description supplied by TrmMeBridgeRequest interface.
   */
  public void setRequiredBusName(String value) {
    jmo.setField(TrmFirstContactAccess.BODY_MEBRIDGEREQUEST_REQUIREDBUSNAME, value);
  }

  /*
   *  Set the userid
   *
   *  Javadoc description supplied by TrmMeBridgeRequest interface.
   */
  public void setUserid(String value) {
    jmo.setField(TrmFirstContactAccess.BODY_MEBRIDGEREQUEST_USERID, value);
  }

  /*
   *  Set the password
   *
   *  Javadoc description supplied by TrmMeBridgeRequest interface.
   */
  public void setPassword(String value) {
    jmo.setField(TrmFirstContactAccess.BODY_MEBRIDGEREQUEST_PASSWORD, value);
  }

  /*
   *  Set the required ME name in the message.
   *
   *  Javadoc description supplied by TrmMeBridgeRequest interface.
   */
  public void setRequiredMeName(String value) {
    jmo.setField(TrmFirstContactAccess.BODY_MEBRIDGEREQUEST_REQUIREDMENAME, value);
  }

  /*
   *  Set the requesting Bus name in the message.
   *
   *  Javadoc description supplied by TrmMeBridgeRequest interface.
   */
  public void setRequestingBusName(String value) {
    jmo.setField(TrmFirstContactAccess.BODY_MEBRIDGEREQUEST_REQUESTINGBUSNAME, value);
  }

  /*
   *  Set the requesting ME name in the message.
   *
   *  Javadoc description supplied by TrmMeBridgeRequest interface.
   */
  public void setRequestingMeName(String value) {
    jmo.setField(TrmFirstContactAccess.BODY_MEBRIDGEREQUEST_REQUESTINGMENAME, value);
  }

  /*
   *  Set the requesting ME UUID in the message.
   *
   *  Javadoc description supplied by TrmMeBridgeRequest interface.
   */
  public void setRequestingMeUuid(SIBUuid8 value) {
    if (value != null)
      jmo.setField(TrmFirstContactAccess.BODY_MEBRIDGEREQUEST_REQUESTINGMEUUID, value.toByteArray());
    else
      jmo.setField(TrmFirstContactAccess.BODY_MEBRIDGEREQUEST_REQUESTINGMEUUID, null);
  }

  /*
   *  Set the Link name in the message.
   *
   *  Javadoc description supplied by TrmMeBridgeRequest interface.
   */
  public void setLinkName(String value) {
    jmo.setField(TrmFirstContactAccess.BODY_MEBRIDGEREQUEST_LINKNAME, value);
  }

}
