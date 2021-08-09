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
 *  TrmFirstContactMessageImpl is the implementation class for the
 *  TrmFirstContactMessage interface.
 *  <p>
 *  The TrmFirstContactMessageImpl instance extends MessageImpl which contains
 *  the JsMsgObject which is the internal object which represents a Message of any type.
 *  The implementation classes for all the specialised TRM First Contact messages extend
 *  TrmFirstContactMessageImpl, as well as implementing their specialised interface.
 *  TrmFirstContactMessage and its subclasses have no instance variables.
 *
 */
public class TrmFirstContactMessageImpl extends MessageImpl implements TrmFirstContactMessage  {

  private final static long serialVersionUID = 1L;

  private static TraceComponent tc = SibTr.register(TrmFirstContactMessageImpl.class, MfpConstants.MSG_GROUP, MfpConstants.MSG_BUNDLE);

  /* **************************************************************************/
  /* Constructors                                                             */
  /* **************************************************************************/

  /**
   *  Constructor for a new Jetstream message.
   *
   *  @exception MessageDecodeFailedException Thrown if such a message can not be created
   */
  TrmFirstContactMessageImpl() throws MessageDecodeFailedException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "<init>");

    setJmo(new JsMsgObject(TrmFirstContactAccess.schema));

    jmo.setChoiceField(TrmFirstContactAccess.BODY, TrmFirstContactAccess.IS_BODY_EMPTY);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "<init>");
  }

  /**
   *  Constructor for an inbound message.
   *
   *  @param inJmo The JsMsgObject representing the inbound message
   */
  TrmFirstContactMessageImpl(JsMsgObject inJmo) {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "<init>", inJmo);

    setJmo(inJmo);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "<init>");
  }

  /* **************************************************************************/
  /* Methods for making more specialised messages                             */
  /* **************************************************************************/

  /*
   *  Convert the existing inbound TrmFirstContactMessage into a
   *  TrmClientBootstrapRequest.
   *
   *  Javadoc description supplied by TrmFirstContactMessage interface.
   */
  public TrmClientBootstrapRequest makeInboundTrmClientBootstrapRequest() {
    return new TrmClientBootstrapRequestImpl(jmo);
  }

  /*
   *  Convert the existing inbound TrmFirstContactMessage into a
   *  TrmClientBootstrapReply.
   *
   *  Javadoc description supplied by TrmFirstContactMessage interface.
   */
  public TrmClientBootstrapReply makeInboundTrmClientBootstrapReply() {
    return new TrmClientBootstrapReplyImpl(jmo);
  }

  /*
   *  Convert the existing inbound TrmFirstContactMessage into a
   *  TrmClientAttachRequest.
   *
   *  Javadoc description supplied by TrmFirstContactMessage interface.
   */
  public TrmClientAttachRequest makeInboundTrmClientAttachRequest() {
    return new TrmClientAttachRequestImpl(jmo);
  }

  /*
   *  Convert the existing inbound TrmFirstContactMessage into a
   *  TrmClientAttachRequest2.
   *
   *  Javadoc description supplied by TrmFirstContactMessage interface.
   */
  public TrmClientAttachRequest2 makeInboundTrmClientAttachRequest2() {
    return new TrmClientAttachRequest2Impl(jmo);
  }

  /*
   *  Convert the existing inbound TrmFirstContactMessage into a
   *  TrmClientAttachReply.
   *
   *  Javadoc description supplied by TrmFirstContactMessage interface.
   */
  public TrmClientAttachReply makeInboundTrmClientAttachReply() {
    return new TrmClientAttachReplyImpl(jmo);
  }

  /*
   *  Convert the existing inbound TrmFirstContactMessage into a
   *  TrmMeConnectRequest.
   *
   *  Javadoc description supplied by TrmFirstContactMessage interface.
   */
  public TrmMeConnectRequest makeInboundTrmMeConnectRequest() {
    return new TrmMeConnectRequestImpl(jmo);
  }

  /*
   *  Convert the existing inbound TrmFirstContactMessage into a
   *  TrmMeConnectReply.
   *
   *  Javadoc description supplied by TrmFirstContactMessage interface.
   */
  public TrmMeConnectReply makeInboundTrmMeConnectReply() {
    return new TrmMeConnectReplyImpl(jmo);
  }

  /*
   *  Convert the existing inbound TrmFirstContactMessage into a
   *  TrmMeLinkReply.
   *
   *  Javadoc description supplied by TrmFirstContactMessage interface.
   */
  public TrmMeLinkRequest makeInboundTrmMeLinkRequest() {
    return new TrmMeLinkRequestImpl(jmo);
  }

  /*
   *  Convert the existing inbound TrmFirstContactMessage into a
   *  TrmMeLinkReply.
   *
   *  Javadoc description supplied by TrmFirstContactMessage interface.
   */
  public TrmMeLinkReply makeInboundTrmMeLinkReply() {
    return new TrmMeLinkReplyImpl(jmo);
  }

  /*
   *  Convert the existing inbound TrmFirstContactMessage into a
   *  TrmMeBridgeReply.
   *
   *  Javadoc description supplied by TrmFirstContactMessage interface.
   */
  public TrmMeBridgeRequest makeInboundTrmMeBridgeRequest() {
    return new TrmMeBridgeRequestImpl(jmo);
  }

  /*
   *  Convert the existing inbound TrmFirstContactMessage into a
   *  TrmMeBridgeReply.
   *
   *  Javadoc description supplied by TrmFirstContactMessage interface.
   */
  public TrmMeBridgeReply makeInboundTrmMeBridgeReply() {
    return new TrmMeBridgeReplyImpl(jmo);
  }

  /*
   *  Convert the existing inbound TrmFirstContactMessage into a
   *  TrmMeBridgeBootstrapRequest
   *
   *  Javadoc description supplied by TrmFirstContactMessage interface.
   */
  public TrmMeBridgeBootstrapRequest makeInboundTrmMeBridgeBootstrapRequest() {
    return new TrmMeBridgeBootstrapRequestImpl(jmo);
  }

  /*
   *  Convert the existing inbound TrmFirstContactMessage into a
   *  TrmMeBridgeBootstrapReply
   *
   *  Javadoc description supplied by TrmFirstContactMessage interface.
   */
  public TrmMeBridgeBootstrapReply makeInboundTrmMeBridgeBootstrapReply() {
    return new TrmMeBridgeBootstrapReplyImpl(jmo);
  }

  /* **************************************************************************/
  /* Methods for encoding                                                     */
  /* **************************************************************************/

  /*
   *  Encode the message into a byte array for transmission.
   *
   *  Javadoc description supplied by TrmFirstContactMessage interface.
   *
   *  For a simple JMF message like this only one message part, so we call
   *  encodeSinglePartMessage() which returns a single DataSlice containing
   *  the entire encoded message.
   */
  public byte[] encode(Object conn) throws MessageEncodeFailedException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "encode");
    return jmo.encodeSinglePartMessage(conn).getBytes();
  }

  /* **************************************************************************/
  /* Get Methods                                                              */
  /* **************************************************************************/

  /*
   *  Get the value of the TrmFirstContactMessageType from the  message.
   *
   *  Javadoc description supplied by TrmFirstContactMessage interface.
   */
  public final TrmFirstContactMessageType getMessageType() {
    /* Get the int value and get the corresponding TrmFirstContactMessageType to return      */
    int mType = jmo.getIntField(TrmFirstContactAccess.MESSAGETYPE);
    return TrmFirstContactMessageType.getTrmFirstContactMessageType(mType);
  }

  /* **************************************************************************/
  /* Set Methods                                                              */
  /* **************************************************************************/

  /**
   *  Set the value of the TrmFirstContactMessageType field in the message.
   *  This method is only used by message constructors.
   *
   *  @param value The TrmFirstContactMessageType instance representing the type
   *               of this message.
   */
  final void setMessageType(TrmFirstContactMessageType value) {
    jmo.setIntField(TrmFirstContactAccess.MESSAGETYPE, value.toInt());
  }
}
