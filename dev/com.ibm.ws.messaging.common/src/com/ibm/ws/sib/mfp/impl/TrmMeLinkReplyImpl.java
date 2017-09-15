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
import java.util.*;

/**
 * TrmMeLinkReplyImpl extends the general TrmFirstContactMessageImpl
 * and is the implementation class for the TrmMeLinkReply interface.
 *
 */
public class TrmMeLinkReplyImpl extends TrmFirstContactMessageImpl implements TrmMeLinkReply  {

  private final static long serialVersionUID = 1L;

  private static TraceComponent tc = SibTr.register(TrmMeLinkReplyImpl.class, MfpConstants.MSG_GROUP, MfpConstants.MSG_BUNDLE);


  /* **************************************************************************/
  /* Constructors                                                             */
  /* **************************************************************************/

  /**
   *  Constructor for a new Jetstream message.
   *
   *  @exception MessageDecodeFailedException Thrown if such a message can not be created
   */
  TrmMeLinkReplyImpl() throws MessageDecodeFailedException {
    super();
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "<init>");

    setMessageType(TrmFirstContactMessageType.ME_LINK_REPLY);

    setMagicNumber(0);
    setReplyingMeUuid(null);
    setFailureReason(new ArrayList());
  }

  /**
   *  Constructor for an inbound message.
   *  (Only to be called by TrmFirstContactMessage.makeInboundTrmMeLinkReply
   *
   *  @param inJmo The JsMsgObject representing the inbound message.
   */
  TrmMeLinkReplyImpl(JsMsgObject inJmo) {
    super(inJmo);
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "<init>, inbound jmo");
  }


  /* **************************************************************************/
  /* Get Methods                                                              */
  /* **************************************************************************/

  /*
   *  Get the Magic Number from the message.
   *
   *  Javadoc description supplied by TrmMeLinkReply interface.
   */
  public final long getMagicNumber() {
    return jmo.getLongField(TrmFirstContactAccess.BODY_MELINKREPLY_MAGICNUMBER);
  }

  /*
   *  Get the Return Code from the message.
   *
   *  Javadoc description supplied by TrmClientBootstrapRequest interface.
   */
  public Integer getReturnCode() {
    return (Integer)jmo.getField(TrmFirstContactAccess.BODY_MELINKREPLY_RETURNCODE);
  }

  /*
   *  Get the replying ME UUID from the message.
   *
   *  Javadoc description supplied by TrmClientBootstrapRequest interface.
   */
  public SIBUuid8 getReplyingMeUuid() {
    byte[] b = (byte[])jmo.getField(TrmFirstContactAccess.BODY_MELINKREPLY_REPLYINGMEUUID);
    if (b != null)
      return new SIBUuid8(b);
    return null;
  }

  /*
   *  Get the failure reason from the message.
   *
   *  Javadoc description supplied by TrmClientBootstrapRequest interface.
   */
  public List getFailureReason() {
    List fr = (List)jmo.getField(TrmFirstContactAccess.BODY_MELINKREPLY_FAILUREREASON);
    if (fr != null) {
      return new ArrayList(fr);
    } else {
      return null;
    }
  }

  /* **************************************************************************/
  /* Set Methods                                                              */
  /* **************************************************************************/

  /*
   *  Set the Magic Number field in the message.
   *
   *  Javadoc description supplied by TrmMeLinkReply interface.
   */
  public final void setMagicNumber(long value) {
    jmo.setLongField(TrmFirstContactAccess.BODY_MELINKREPLY_MAGICNUMBER, value);
  }

  /*
   *  Set the Return Code in the message.
   *
   *  Javadoc description supplied by TrmClientBootstrapRequest interface.
   */
  public void setReturnCode(int value) {
    jmo.setIntField(TrmFirstContactAccess.BODY_MELINKREPLY_RETURNCODE, value);
  }

  /*
   *  Set the replying ME UUID in the message.
   *
   *  Javadoc description supplied by TrmClientBootstrapRequest interface.
   */
  public void setReplyingMeUuid(SIBUuid8 value) {
    if (value != null)
      jmo.setField(TrmFirstContactAccess.BODY_MELINKREPLY_REPLYINGMEUUID, value.toByteArray());
    else
      jmo.setField(TrmFirstContactAccess.BODY_MELINKREPLY_REPLYINGMEUUID, null);
  }

  /*
   *  Set the failure reason in the message.
   *
   *  Javadoc description supplied by TrmClientBootstrapRequest interface.
   */
  public void setFailureReason(List value) {
    List fr = new ArrayList(value);
    jmo.setField(TrmFirstContactAccess.BODY_MELINKREPLY_FAILUREREASON, fr);
  }

}
