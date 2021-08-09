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
 * TrmClientBootstrapRequestImpl extends the general TrmFirstContactMessageImpl
 * and is the implementation class for the TrmClientBootstrapRequest interface.
 *
 */
public class TrmClientBootstrapRequestImpl extends TrmFirstContactMessageImpl implements TrmClientBootstrapRequest  {

  private final static long serialVersionUID = 1L;

  private static TraceComponent tc = SibTr.register(TrmClientBootstrapRequestImpl.class, MfpConstants.MSG_GROUP, MfpConstants.MSG_BUNDLE);

  /* **************************************************************************/
  /* Constructors                                                             */
  /* **************************************************************************/

  /**
   *  Constructor for a new Jetstream message.
   *
   *  @exception MessageDecodeFailedException Thrown if such a message can not be created
   */
  TrmClientBootstrapRequestImpl() throws MessageDecodeFailedException {
    super();
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "<init>");

    setMessageType(TrmFirstContactMessageType.CLIENT_BOOTSTRAP_REQUEST);
  }

  /**
   *  Constructor for an inbound message.
   *  (Only to be called by TrmFirstContactMessage.makeInboundTrmClientBootstrapRequest
   *
   *  @param inJmo The JsMsgObject representing the inbound message.
   */
  TrmClientBootstrapRequestImpl(JsMsgObject inJmo) {
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
    return (String)jmo.getField(TrmFirstContactAccess.BODY_CLIENTBOOTSTRAPREQUEST_BUSNAME);
  }

  /*
   *  Get the credential type
   *
   *  Javadoc description supplied by TrmClientBootstrapRequest interface.
   */
  public String getCredentialType() {
    return (String)jmo.getField(TrmFirstContactAccess.BODY_CLIENTBOOTSTRAPREQUEST_CREDENTIALTYPE);
  }

  /*
   *  Get the userid
   *
   *  Javadoc description supplied by TrmClientBootstrapRequest interface.
   */
  public String getUserid() {
    return (String)jmo.getField(TrmFirstContactAccess.BODY_CLIENTBOOTSTRAPREQUEST_USERID);
  }

  /*
   *  Get the password
   *
   *  Javadoc description supplied by TrmClientBootstrapRequest interface.
   */
  public String getPassword() {
   return (String)jmo.getField(TrmFirstContactAccess.BODY_CLIENTBOOTSTRAPREQUEST_PASSWORD);
  }

  /*
   *  Get the target group name from the message.
   *
   *  Javadoc description supplied by TrmClientBootstrapRequest interface.
   */
  public String getTargetGroupName() {
    return (String)jmo.getField(TrmFirstContactAccess.BODY_CLIENTBOOTSTRAPREQUEST_TARGETGROUPNAME);
  }

  /*
   *  Get the target group type from the message.
   *
   *  Javadoc description supplied by TrmClientBootstrapRequest interface.
   */
  public String getTargetGroupType() {
    return (String)jmo.getField(TrmFirstContactAccess.BODY_CLIENTBOOTSTRAPREQUEST_TARGETGROUPTYPE);
  }

  /*
   *  Get the target significance from the message.
   *
   *  Javadoc description supplied by TrmClientBootstrapRequest interface.
   */
  public String getTargetSignificance() {
    return (String)jmo.getField(TrmFirstContactAccess.BODY_CLIENTBOOTSTRAPREQUEST_TARGETSIGNIFICANCE);
  }

  /*
   *  Get the connection proximity from the message.
   *
   *  Javadoc description supplied by TrmClientBootstrapRequest interface.
   */
  public String getConnectionProximity() {
    return (String)jmo.getField(TrmFirstContactAccess.BODY_CLIENTBOOTSTRAPREQUEST_CONNECTIONPROXIMITY);
  }

  /*
   *  Get the target transport chain from the message.
   *
   *  Javadoc description supplied by TrmClientBootstrapRequest interface.
   */
  public String getTargetTransportChain() {
    return (String)jmo.getField(TrmFirstContactAccess.BODY_CLIENTBOOTSTRAPREQUEST_TARGETTRANSPORTCHAIN);
  }

  /*
   *  Get the bootstap transport chain from the message.
   *
   *  Javadoc description supplied by TrmClientBootstrapRequest interface.
   */
  public String getBootstrapTransportChain() {
    return (String)jmo.getField(TrmFirstContactAccess.BODY_CLIENTBOOTSTRAPREQUEST_BOOTSTRAPTRANSPORTCHAIN);
  }

  /*
   *  Get the connection mode from the message.
   *
   *  Javadoc description supplied by TrmClientBootstrapRequest interface.
   */
  public String getConnectionMode() {                                                                       //250606.1.1
    return (String)jmo.getField(TrmFirstContactAccess.BODY_CLIENTBOOTSTRAPREQUEST_UPDATE1_CONNECTIONMODE);  //250606.1.1
  }                                                                                                         //250606.1.1

  /* **************************************************************************/
  /* Set Methods                                                              */
  /* **************************************************************************/

  /*
   *  Set the Bus name in the message.
   *
   *  Javadoc description supplied by TrmClientBootstrapRequest interface.
   */
  public void setBusName(String value) {
    jmo.setField(TrmFirstContactAccess.BODY_CLIENTBOOTSTRAPREQUEST_BUSNAME, value);
  }

  /*
   *  Set the credential type
   *
   *  Javadoc description supplied by TrmClientBootstrapRequest interface.
   */
  public void setCredentialType(String value) {
    jmo.setField(TrmFirstContactAccess.BODY_CLIENTBOOTSTRAPREQUEST_CREDENTIALTYPE, value);
  }

  /*
   *  Set the userid
   *
   *  Javadoc description supplied by TrmClientBootstrapRequest interface.
   */
  public void setUserid(String value) {
    jmo.setField(TrmFirstContactAccess.BODY_CLIENTBOOTSTRAPREQUEST_USERID, value);
  }

  /*
   *  Set the password
   *
   *  Javadoc description supplied by TrmClientBootstrapRequest interface.
   */
  public void setPassword(String value) {
    jmo.setField(TrmFirstContactAccess.BODY_CLIENTBOOTSTRAPREQUEST_PASSWORD, value);
  }

  /*
   *  Set the target group name in the message.
   *
   *  Javadoc description supplied by TrmClientBootstrapRequest interface.
   */
  public void setTargetGroupName(String value) {
    jmo.setField(TrmFirstContactAccess.BODY_CLIENTBOOTSTRAPREQUEST_TARGETGROUPNAME, value);
  }

  /*
   *  Set the target group type in the message.
   *
   *  Javadoc description supplied by TrmClientBootstrapRequest interface.
   */
  public void setTargetGroupType(String value) {
    jmo.setField(TrmFirstContactAccess.BODY_CLIENTBOOTSTRAPREQUEST_TARGETGROUPTYPE, value);
  }

  /*
   *  Set the target signifiance in the message.
   *
   *  Javadoc description supplied by TrmClientBootstrapRequest interface.
   */
  public void setTargetSignificance(String value) {
    jmo.setField(TrmFirstContactAccess.BODY_CLIENTBOOTSTRAPREQUEST_TARGETSIGNIFICANCE, value);
  }

  /*
   *  Set the connection proximity in the message.
   *
   *  Javadoc description supplied by TrmClientBootstrapRequest interface.
   */
  public void setConnectionProximity(String value) {
    jmo.setField(TrmFirstContactAccess.BODY_CLIENTBOOTSTRAPREQUEST_CONNECTIONPROXIMITY, value);
  }

  /*
   *  Set the target transport chain in the message.
   *
   *  Javadoc description supplied by TrmClientBootstrapRequest interface.
   */
  public void setTargetTransportChain(String value) {
    jmo.setField(TrmFirstContactAccess.BODY_CLIENTBOOTSTRAPREQUEST_TARGETTRANSPORTCHAIN, value);
  }

  /*
   *  Set the bootstrap transport chain in the message.
   *
   *  Javadoc description supplied by TrmClientBootstrapRequest interface.
   */
  public void setBootstrapTransportChain(String value) {
    jmo.setField(TrmFirstContactAccess.BODY_CLIENTBOOTSTRAPREQUEST_BOOTSTRAPTRANSPORTCHAIN, value);
  }

  /*
   *  Set the connection mode in the message.
   *
   *  Javadoc description supplied by TrmClientBootstrapRequest interface.
   */
  public void setConnectionMode(String value) {                                                             //250606.1.1
    jmo.setField(TrmFirstContactAccess.BODY_CLIENTBOOTSTRAPREQUEST_UPDATE1_CONNECTIONMODE, value);          //250606.1.1
  }                                                                                                         //250606.1.1

}
