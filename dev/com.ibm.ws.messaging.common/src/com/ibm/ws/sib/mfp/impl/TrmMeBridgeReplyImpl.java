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
import java.util.ArrayList;
import java.util.List;

/**
 * TrmMeBridgeReplyImpl extends the general TrmFirstContactMessageImpl
 * and is the implementation class for the TrmMeBridgeReply interface.
 *
 */
public class TrmMeBridgeReplyImpl extends TrmFirstContactMessageImpl implements TrmMeBridgeReply  {

  private final static long serialVersionUID = 1L;

  private static TraceComponent tc = SibTr.register(TrmMeBridgeReplyImpl.class, MfpConstants.MSG_GROUP, MfpConstants.MSG_BUNDLE);

  /* **************************************************************************/
  /* Constructors                                                             */
  /* **************************************************************************/

  /**
   *  Constructor for a new Jetstream message.
   *
   *  @exception MessageDecodeFailedException Thrown if such a message can not be created
   */
  TrmMeBridgeReplyImpl() throws MessageDecodeFailedException {
    super();
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "<init>");

    setMessageType(TrmFirstContactMessageType.ME_BRIDGE_REPLY);

    setMagicNumber(0);
    setReplyingMeUuid(null);
    setFailureReason(new ArrayList());
  }

  /**
   *  Constructor for an inbound message.
   *  (Only to be called by TrmFirstContactMessage.makeInboundTrmMeBridgeReply
   *
   *  @param inJmo The JsMsgObject representing the inbound message.
   */
  TrmMeBridgeReplyImpl(JsMsgObject inJmo) {
    super(inJmo);
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "<init>, inbound jmo");
  }


  /* **************************************************************************/
  /* Get Methods                                                              */
  /* **************************************************************************/

  /*
   *  Get the Magic Number from the message.
   *
   *  Javadoc description supplied by TrmMeBridgeReply interface.
   */
  public final long getMagicNumber() {
    return jmo.getLongField(TrmFirstContactAccess.BODY_MEBRIDGEREPLY_MAGICNUMBER);
  }

  /*
   *  Get the Return Code from the message.
   *
   *  Javadoc description supplied by TrmMeBridgeReply interface.
   */
  public Integer getReturnCode() {
    return (Integer)jmo.getField(TrmFirstContactAccess.BODY_MEBRIDGEREPLY_RETURNCODE);
  }

  /*
   *  Get the replying ME UUID from the message.
   *
   *  Javadoc description supplied by TrmMeBridgeReply interface.
   */
  public SIBUuid8 getReplyingMeUuid() {
    byte[] b = (byte[])jmo.getField(TrmFirstContactAccess.BODY_MEBRIDGEREPLY_REPLYINGMEUUID);
    if (b != null)
      return new SIBUuid8(b);
    return null;
  }

  /*
   *  Get the failure reason from the message.
   *
   *  Javadoc description supplied by TrmMeBridgeReply interface.
   */
  public List getFailureReason() {
    // Note: Caller does not modify the returned list, so there is
    //       no need to copy it.
    return (List)jmo.getField(TrmFirstContactAccess.BODY_MEBRIDGEREPLY_FAILUREREASON);
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
    jmo.setLongField(TrmFirstContactAccess.BODY_MEBRIDGEREPLY_MAGICNUMBER, value);
  }

  /*
   *  Set the Return Code in the message.
   *
   *  Javadoc description supplied by TrmMeBridgeReply interface.
   */
  public void setReturnCode(int value) {
    jmo.setIntField(TrmFirstContactAccess.BODY_MEBRIDGEREPLY_RETURNCODE, value);
  }

  /*
   *  Set the replying ME UUID in the message.
   *
   *  Javadoc description supplied by TrmMeBridgeReply interface.
   */
  public void setReplyingMeUuid(SIBUuid8 value) {
    if (value != null)
    jmo.setField(TrmFirstContactAccess.BODY_MEBRIDGEREPLY_REPLYINGMEUUID, value.toByteArray());
    else
      jmo.setField(TrmFirstContactAccess.BODY_MEBRIDGEREPLY_REPLYINGMEUUID, null);
  }

  /*
   *  Set the failure reason in the message.
   *
   *  Javadoc description supplied by TrmMeBridgeReply interface.
   */
  public void setFailureReason(List value) {
    jmo.setField(TrmFirstContactAccess.BODY_MEBRIDGEREPLY_FAILUREREASON, value);
  }
}
