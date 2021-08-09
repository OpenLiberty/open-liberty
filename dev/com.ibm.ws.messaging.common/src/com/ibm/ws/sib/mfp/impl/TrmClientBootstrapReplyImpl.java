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

import java.util.*;

import com.ibm.ws.sib.mfp.*;
import com.ibm.ws.sib.mfp.trm.*;
import com.ibm.ws.sib.mfp.schema.TrmFirstContactAccess;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * TrmClientBootstrapReplyImpl extends the general TrmFirstContactMessageImpl
 * and is the implementation class for the TrmClientBootstrapReply interface.
 *
 */
public class TrmClientBootstrapReplyImpl extends TrmFirstContactMessageImpl implements TrmClientBootstrapReply  {

  private final static long serialVersionUID = 1L;

  private static TraceComponent tc = SibTr.register(TrmClientBootstrapReplyImpl.class, MfpConstants.MSG_GROUP, MfpConstants.MSG_BUNDLE);


  /* **************************************************************************/
  /* Constructors                                                             */
  /* **************************************************************************/

  /**
   *  Constructor for a new Jetstream message.
   *
   *  @exception MessageDecodeFailedException Thrown if such a message can not be created
   */
  TrmClientBootstrapReplyImpl() throws MessageDecodeFailedException {
    super();
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "<init>");

    setMessageType(TrmFirstContactMessageType.CLIENT_BOOTSTRAP_REPLY);
    setFailureReason(new ArrayList());
  }

  /**
   *  Constructor for an inbound message.
   *  (Only to be called by TrmFirstContactMessage.makeInboundTrmClientBootstrapReply
   *
   *  @param inJmo The JsMsgObject representing the inbound message.
   */
  TrmClientBootstrapReplyImpl(JsMsgObject inJmo) {
    super(inJmo);
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "<init>, inbound jmo ");
  }


  /* **************************************************************************/
  /* Get Methods                                                              */
  /* **************************************************************************/

  /*
   *  Get the Return Code from the message.
   *
   *  Javadoc description supplied by TrmClientBootstrapReply interface.
   */
  public Integer getReturnCode() {
    return (Integer)jmo.getField(TrmFirstContactAccess.BODY_CLIENTBOOTSTRAPREPLY_RETURNCODE);
  }

  /*
   *  Get the failure reason from the message.
   *
   *  Javadoc description supplied by TrmClientBootstrapReply interface.
   */
  public List getFailureReason() {
    List fr = (List)jmo.getField(TrmFirstContactAccess.BODY_CLIENTBOOTSTRAPREPLY_FAILUREREASON);
    if (fr != null) {
      return new ArrayList(fr);
    } else {
      return null;
    }
  }

  /**
   *  Get the redirection end point data.
   *
   *  Javadoc description supplied by TrmClientBootstrapReply interface.
   */
  public byte[] getEndPointData() {
    return (byte[])jmo.getField(TrmFirstContactAccess.BODY_CLIENTBOOTSTRAPREPLY_ENDPOINTDATA);
  }

  /**
   *  Get the redirection bus name.
   *
   *  Javadoc description supplied by TrmClientBootstrapReply interface.
   */
  public String getBusName() {
    return (String)jmo.getField(TrmFirstContactAccess.BODY_CLIENTBOOTSTRAPREPLY_BUSNAME);
  }

  /**
   *  Get the redirection subnet name.
   *
   *  Javadoc description supplied by TrmClientBootstrapReply interface.
   */
  public String getSubnetName() {
    return (String)jmo.getField(TrmFirstContactAccess.BODY_CLIENTBOOTSTRAPREPLY_SUBNETNAME);
  }

  /**
   *  Get the redirection messaging engine name.
   *
   *  Javadoc description supplied by TrmClientBootstrapReply interface.
   */
  public String getMessagingEngineName() {
    return (String)jmo.getField(TrmFirstContactAccess.BODY_CLIENTBOOTSTRAPREPLY_MESSAGINGENGINENAME);
  }

  /* **************************************************************************/
  /* Set Methods                                                              */
  /* **************************************************************************/

  /*
   *  Set the Return Code in the message.
   *
   *  Javadoc description supplied by TrmClientBootstrapReply interface.
   */
  public void setReturnCode(int value) {
    jmo.setIntField(TrmFirstContactAccess.BODY_CLIENTBOOTSTRAPREPLY_RETURNCODE, value);
  }

  /*
   *  Set the failure reason in the message.
   *
   *  Javadoc description supplied by TrmClientBootstrapReply interface.
   */
  public void setFailureReason(List value) {
    List fr = new ArrayList(value);
    jmo.setField(TrmFirstContactAccess.BODY_CLIENTBOOTSTRAPREPLY_FAILUREREASON, fr);
  }

  /**
   *  Set the redirection end point data.
   *
   *  Javadoc description supplied by TrmClientBootstrapReply interface.
   */
  public void setEndPointData(byte[] value) {
    jmo.setField(TrmFirstContactAccess.BODY_CLIENTBOOTSTRAPREPLY_ENDPOINTDATA, value);
  }

  /**
   *  Set the redirection bus name.
   *
   *  Javadoc description supplied by TrmClientBootstrapReply interface.
   */
  public void setBusName(String value) {
    jmo.setField(TrmFirstContactAccess.BODY_CLIENTBOOTSTRAPREPLY_BUSNAME, value);
  }

  /**
   *  Set the redirection subnet name.
   *
   *  Javadoc description supplied by TrmClientBootstrapReply interface.
   */
  public void setSubnetName(String value) {
    jmo.setField(TrmFirstContactAccess.BODY_CLIENTBOOTSTRAPREPLY_SUBNETNAME, value);
  }

  /**
   *  Set the redirection messaging engine name.
   *
   *  Javadoc description supplied by TrmClientBootstrapReply interface.
   */
  public void setMessagingEngineName(String value) {
    jmo.setField(TrmFirstContactAccess.BODY_CLIENTBOOTSTRAPREPLY_MESSAGINGENGINENAME, value);
  }

}
