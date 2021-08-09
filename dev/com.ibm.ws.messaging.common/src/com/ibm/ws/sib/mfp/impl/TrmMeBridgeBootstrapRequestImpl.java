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

import com.ibm.ws.sib.mfp.*;
import com.ibm.ws.sib.mfp.trm.*;
import com.ibm.ws.sib.mfp.schema.TrmFirstContactAccess;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * TrmMeBridgeBootstrapRequestImpl extends the general TrmFirstContactMessageImpl
 * and is the implementation class for the TrmMeBridgeBootstrapRequest interface.
 *
 */
public class TrmMeBridgeBootstrapRequestImpl extends TrmFirstContactMessageImpl implements TrmMeBridgeBootstrapRequest {

  private final static long serialVersionUID = 1L;

  private static TraceComponent tc = SibTr.register(TrmMeBridgeBootstrapRequestImpl.class, MfpConstants.MSG_GROUP, MfpConstants.MSG_BUNDLE);

  /* **************************************************************************/
  /* Constructors                                                             */
  /* **************************************************************************/

  /**
   *  Constructor for a new Jetstream message.
   *
   *  @exception MessageDecodeFailedException Thrown if such a message can not be created
   */
  TrmMeBridgeBootstrapRequestImpl() throws MessageDecodeFailedException {
    super();
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "<init>");

    setMessageType(TrmFirstContactMessageType.ME_BRIDGE_BOOTSTRAP_REQUEST);
  }

  /**
   *  Constructor for an inbound message.
   *  (Only to be called by TrmFirstContactMessage.makeInboundTrmMeBridgeBootstrapRequest
   *
   *  @param inJmo The JsMsgObject representing the inbound message.
   */
  TrmMeBridgeBootstrapRequestImpl(JsMsgObject inJmo) {
    super(inJmo);
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "<init>, inbound jmo ");
  }


  /* **************************************************************************/
  /* Get Methods                                                              */
  /* **************************************************************************/

  /*
   *  Get the required Bus name from the message.
   *
   *  Javadoc description supplied by TrmMeBridgeBootstrapRequest interface.
   */
  public String getRequiredBusName() {
    return (String)jmo.getField(TrmFirstContactAccess.BODY_MEBRIDGEBOOTSTRAPREQUEST_REQUIREDBUSNAME);
  }

  /*
   *  Get the userid
   *
   *  Javadoc description supplied by TrmMeBridgeBootstrapRequest interface.
   */
  public String getUserid () {
    return (String)jmo.getField(TrmFirstContactAccess.BODY_MEBRIDGEBOOTSTRAPREQUEST_USERID);
  }

  /*
   *  Get the password
   *
   *  Javadoc description supplied by TrmMeBridgeBootstrapRequest interface.
   */
  public String getPassword () {
    return (String)jmo.getField(TrmFirstContactAccess.BODY_MEBRIDGEBOOTSTRAPREQUEST_PASSWORD);
  }

  /*
   *  Get the requesting Bus name from the message.
   *
   *  Javadoc description supplied by TrmMeBridgeBootstrapRequest interface.
   */
  public String getRequestingBusName() {
    return (String)jmo.getField(TrmFirstContactAccess.BODY_MEBRIDGEBOOTSTRAPREQUEST_REQUESTINGBUSNAME);
  }

  /*
   *  Get the Link name from the message.
   *
   *  Javadoc description supplied by TrmMeBridgeBootstrapRequest interface.
   */
  public String getLinkName(){
    return (String)jmo.getField(TrmFirstContactAccess.BODY_MEBRIDGEBOOTSTRAPREQUEST_LINKNAME);
  }

  /*
   *  Get the required transport chain from the message.
   *
   *  Javadoc description supplied by TrmMeBridgeBootstrapRequest interface.
   */
  public String getRequiredTransportChain() {
    return (String)jmo.getField(TrmFirstContactAccess.BODY_MEBRIDGEBOOTSTRAPREQUEST_REQUIREDTRANSPORTCHAIN);
  }

  /* **************************************************************************/
  /* Set Methods                                                              */
  /* **************************************************************************/

  /*
   *  Set the required Bus name in the message.
   *
   *  Javadoc description supplied by TrmMebridgeBootstrapRequest interface.
   */
  public void setRequiredBusName(String value) {
    jmo.setField(TrmFirstContactAccess.BODY_MEBRIDGEBOOTSTRAPREQUEST_REQUIREDBUSNAME, value);
  }

  /*
   *  Set the userid
   *
   *  Javadoc description supplied by TrmMebridgeBootstrapRequest interface.
   */
  public void setUserid(String value) {
    jmo.setField(TrmFirstContactAccess.BODY_MEBRIDGEBOOTSTRAPREQUEST_USERID, value);
  }

  /*
   *  Set the password
   *
   *  Javadoc description supplied by TrmMebridgeBootstrapRequest interface.
   */
  public void setPassword(String value) {
    jmo.setField(TrmFirstContactAccess.BODY_MEBRIDGEBOOTSTRAPREQUEST_PASSWORD, value);
  }

  /*
   *  Set the requesting Bus name in the message.
   *
   *  Javadoc description supplied by TrmMebridgeBootstrapRequest interface.
   */
  public void setRequestingBusName(String value) {
    jmo.setField(TrmFirstContactAccess.BODY_MEBRIDGEBOOTSTRAPREQUEST_REQUESTINGBUSNAME, value);
  }

  /*
   *  Set the Link name in the message.
   *
   *  Javadoc description supplied by TrmMebridgeBootstrapRequest interface.
   */
  public void setLinkName(String value) {
    jmo.setField(TrmFirstContactAccess.BODY_MEBRIDGEBOOTSTRAPREQUEST_LINKNAME, value);
  }

  /*
   *  Set the required transport chain in the message.
   *
   *  Javadoc description supplied by TrmMebridgeBootstrapRequest interface.
   */
  public void setRequiredTransportChain(String value) {
    jmo.setField(TrmFirstContactAccess.BODY_MEBRIDGEBOOTSTRAPREQUEST_REQUIREDTRANSPORTCHAIN, value);
  }

}
