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
import com.ibm.ws.sib.mfp.schema.TrmFirstContactAccess;
import com.ibm.ws.sib.mfp.trm.*;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.websphere.ras.TraceComponent;

/**
 *  This class extends the abstract com.ibm.ws.sib.mfp.trm.TrmMessageFactory
 *  class and provides the concrete implementations of the methods for
 *  creating TrmMessages.
 *  <p>
 *  The class must be public so that the abstract class static
 *  initialization can create an instance of it at runtime.
 *
 */
public final class TrmMessageFactoryImpl extends TrmMessageFactory {

  private static TraceComponent tc = SibTr.register(TrmMessageFactoryImpl.class, MfpConstants.MSG_GROUP, MfpConstants.MSG_BUNDLE);

  // Ensure we register all the JMF schemas needed to process these messages
  static {
    SchemaManager.ensureInitialized();                                          // d380323
  }
 
  /****************************************************************************/
  /* First Contact Messages                                                   */
  /****************************************************************************/

  /**
   *  Create a new, empty TrmClientBootstrapRequest message
   *
   *  @return The new TrmClientBootstrapRequest.
   *
   *  @exception MessageCreateFailedException Thrown if such a message can not be created
   */
  public TrmClientBootstrapRequest createNewTrmClientBootstrapRequest() throws MessageCreateFailedException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "createNewTrmClientBootstrapRequest");
    TrmClientBootstrapRequest msg = null;
    try {
      msg = new TrmClientBootstrapRequestImpl();
    }
    catch (MessageDecodeFailedException e) {
      /* No need to FFDC this as JsMsgObject will already have done so */
      // No FFDC code needed
      throw new MessageCreateFailedException(e);
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "createNewTrmClientBootstrapRequest");
    return msg;
  }

  /**
   *  Create a new, empty TrmClientBootstrapReply message
   *
   *  @return The new TrmClientBootstrapReply.
   *
   *  @exception MessageCreateFailedException Thrown if such a message can not be created
   */
  public TrmClientBootstrapReply createNewTrmClientBootstrapReply() throws MessageCreateFailedException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "createNewTrmClientBootstrapReply");
    TrmClientBootstrapReply msg = null;
    try {
      msg = new TrmClientBootstrapReplyImpl();
    }
    catch (MessageDecodeFailedException e) {
      /* No need to FFDC this as JsMsgObject will already have done so */
      // No FFDC code needed
      throw new MessageCreateFailedException(e);
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "createNewTrmClientBootstrapReply");
    return msg;
  }

  /**
   *  Create a new, empty TrmClientAttachRequest message
   *
   *  @return The new TrmClientAttachRequest.
   *  @exception MessageCreateFailedException Thrown if such a message can not be created
   */
  public TrmClientAttachRequest createNewTrmClientAttachRequest() throws MessageCreateFailedException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "createNewTrmClientAttachRequest");
    TrmClientAttachRequest msg = null;
    try {
      msg = new TrmClientAttachRequestImpl();
    }
    catch (MessageDecodeFailedException e) {
      /* No need to FFDC this as JsMsgObject will already have done so */
      // No FFDC code needed
      throw new MessageCreateFailedException(e);
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "createNewTrmClientAttachRequest");
    return msg;
  }

  /**
   *  Create a new, empty TrmClientAttachRequest2 message
   *
   *  @return The new TrmClientAttachRequest2.
   *  @exception MessageCreateFailedException Thrown if such a message can not be created
   */
  public TrmClientAttachRequest2 createNewTrmClientAttachRequest2() throws MessageCreateFailedException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "createNewTrmClientAttachRequest2");
    TrmClientAttachRequest2 msg = null;
    try {
      msg = new TrmClientAttachRequest2Impl();
    }
    catch (MessageDecodeFailedException e) {
      /* No need to FFDC this as JsMsgObject will already have done so */
      // No FFDC code needed
      throw new MessageCreateFailedException(e);
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "createNewTrmClientAttachRequest2");
    return msg;
  }

  /**
   *  Create a new, empty TrmClientAttachReply message
   *
   *  @return The new TrmClientAttachReply.
   *
   *  @exception MessageCreateFailedException Thrown if such a message can not be created
   */
  public TrmClientAttachReply createNewTrmClientAttachReply() throws MessageCreateFailedException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "createNewTrmClientAttachReply");
    TrmClientAttachReply msg = null;
    try {
      msg = new TrmClientAttachReplyImpl();
    }
    catch (MessageDecodeFailedException e) {
      /* No need to FFDC this as JsMsgObject will already have done so */
      // No FFDC code needed
      throw new MessageCreateFailedException(e);
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "createNewTrmClientAttachReply");
    return msg;
  }

  /**
   *  Create a new, empty TrmMeConnectRequest message
   *
   *  @return The new TrmMeConnectRequest.
   *
   *  @exception MessageCreateFailedException Thrown if such a message can not be created
   */
  public TrmMeConnectRequest createNewTrmMeConnectRequest() throws MessageCreateFailedException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "createNewTrmMeConnectRequest");
    TrmMeConnectRequest msg = null;
    try {
      msg = new TrmMeConnectRequestImpl();
    }
    catch (MessageDecodeFailedException e) {
      /* No need to FFDC this as JsMsgObject will already have done so */
      // No FFDC code needed
      throw new MessageCreateFailedException(e);
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "createNewTrmMeConnectRequest");
    return msg;
  }

  /**
   *  Create a new, empty TrmMeConnectReply message
   *
   *  @return The new TrmMeConnectReply.
   *
   *  @exception MessageCreateFailedException Thrown if such a message can not be created
   */
  public TrmMeConnectReply createNewTrmMeConnectReply() throws MessageCreateFailedException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "createNewTrmMeConnectReply");
    TrmMeConnectReply msg = null;
    try {
      msg = new TrmMeConnectReplyImpl();
    }
    catch (MessageDecodeFailedException e) {
      /* No need to FFDC this as JsMsgObject will already have done so */
      // No FFDC code needed
      throw new MessageCreateFailedException(e);
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "createNewTrmMeConnectReply");
    return msg;
  }

  /**
   *  Create a new, empty TrmMeLinkRequest message
   *
   *  @return The new TrmMeLinkRequest.
   *
   *  @exception MessageCreateFailedException Thrown if such a message can not be created
   */
  public TrmMeLinkRequest createNewTrmMeLinkRequest() throws MessageCreateFailedException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "createNewTrmMeLinkRequest");
    TrmMeLinkRequest msg = null;
    try {
      msg = new TrmMeLinkRequestImpl();
    }
    catch (MessageDecodeFailedException e) {
      /* No need to FFDC this as JsMsgObject will already have done so */
      // No FFDC code needed
      throw new MessageCreateFailedException(e);
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "createNewTrmMeLinkRequest");
    return msg;
  }

  /**
   *  Create a new, empty TrmMeLinkReply message
   *
   *  @return The new TrmMeLinkReply.
   *
   *  @exception MessageCreateFailedException Thrown if such a message can not be created
   */
  public TrmMeLinkReply createNewTrmMeLinkReply() throws MessageCreateFailedException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "createNewTrmMeLinkReply");
    TrmMeLinkReply msg = null;
    try {
      msg = new TrmMeLinkReplyImpl();
    }
    catch (MessageDecodeFailedException e) {
      /* No need to FFDC this as JsMsgObject will already have done so */
      // No FFDC code needed
      throw new MessageCreateFailedException(e);
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "createNewTrmMeLinkReply");
    return msg;
  }

  /**
   *  Create a new, empty TrmMeBridgeRequest message
   *
   *  @return The new TrmMeBridgeRequest.
   *
   *  @exception MessageCreateFailedException Thrown if such a message can not be created
   */
  public TrmMeBridgeRequest createNewTrmMeBridgeRequest() throws MessageCreateFailedException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "createNewTrmMeBridgeRequest");
    TrmMeBridgeRequest msg = null;
    try {
      msg = new TrmMeBridgeRequestImpl();
    }
    catch (MessageDecodeFailedException e) {
      /* No need to FFDC this as JsMsgObject will already have done so */
      // No FFDC code needed
      throw new MessageCreateFailedException(e);
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "createNewTrmMeBridgeRequest");
    return msg;
  }

  /**
   *  Create a new, empty TrmMeBridgeReply message
   *
   *  @return The new TrmMeBridgeReply.
   *
   *  @exception MessageCreateFailedException Thrown if such a message can not be created
   */
  public TrmMeBridgeReply createNewTrmMeBridgeReply() throws MessageCreateFailedException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "createNewTrmMeBridgeReply");
    TrmMeBridgeReply msg = null;
    try {
      msg = new TrmMeBridgeReplyImpl();
    }
    catch (MessageDecodeFailedException e) {
      /* No need to FFDC this as JsMsgObject will already have done so */
      // No FFDC code needed
      throw new MessageCreateFailedException(e);
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "createNewTrmMeBridgeReply");
    return msg;
  }

  /**
   *  Create a new, empty TrmMeBridgeBootstrapRequest message
   *
   *  @return The new TrmMeBridgeBootstrapRequest.
   *
   *  @exception MessageCreateFailedException Thrown if such a message can not be created
   */
  public TrmMeBridgeBootstrapRequest createNewTrmMeBridgeBootstrapRequest() throws MessageCreateFailedException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "createNewTrmMeBridgeBootstrapRequest");
    TrmMeBridgeBootstrapRequest msg = null;
    try {
      msg = new TrmMeBridgeBootstrapRequestImpl();
    }
    catch (MessageDecodeFailedException e) {
      /* No need to FFDC this as JsMsgObject will already have done so */
      // No FFDC code needed
      throw new MessageCreateFailedException(e);
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "createNewTrmMeBridgeBootstrapRequest");
    return msg;
  }

  /**
   *  Create a new, empty TrmMeBridgeBootstrapReply message
   *
   *  @return The new TrmMeBridgeBootstrapReply.
   *
   *  @exception MessageCreateFailedException Thrown if such a message can not be created
   */
  public TrmMeBridgeBootstrapReply createNewTrmMeBridgeBootstrapReply() throws MessageCreateFailedException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "createNewTrmMeBridgeBootstrapReply");
    TrmMeBridgeBootstrapReply msg = null;
    try {
      msg = new TrmMeBridgeBootstrapReplyImpl();
    }
    catch (MessageDecodeFailedException e) {
      /* No need to FFDC this as JsMsgObject will already have done so */
      // No FFDC code needed
      throw new MessageCreateFailedException(e);
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "createNewTrmMeBridgeBootstrapReply");
    return msg;
  }

  /**
   *  Create a TrmFirstContactMessage to represent an inbound message.
   *
   *  @param rawMessage  The inbound byte array containging a complete message
   *  @param offset      The offset in the byte array at which the message begins
   *  @param length      The length of the message within the byte array
   *
   *  @return The new TrmFirstContactMessage
   *
   *  @exception MessageDecodeFailedException Thrown if the inbound message could not be decoded
   */
  public TrmFirstContactMessage createInboundTrmFirstContactMessage(byte rawMessage[], int offset, int length)
                                                                   throws MessageDecodeFailedException {

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "createInboundTrmFirstContactMessage", new Object[]{rawMessage, Integer.valueOf(offset), Integer.valueOf(length)});

    JsMsgObject jmo = new JsMsgObject(TrmFirstContactAccess.schema, rawMessage, offset, length);
    TrmFirstContactMessage message = new TrmFirstContactMessageImpl(jmo);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "createInboundTrmFirstContactMessage", message);
    return message;
  }


  /****************************************************************************/
  /* Other Message                                                            */
  /****************************************************************************/

  /**
   *  Create a TrmRouteData message
   *
   *  @return The new TrmRouteData.
   *
   *  @exception MessageCreateFailedException Thrown if such a message can not be created
   */
  public TrmRouteData createTrmRouteData() throws MessageCreateFailedException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "createTrmRouteData");
    TrmRouteData msg = null;
    try {
      msg = new TrmRouteDataImpl(MfpConstants.CONSTRUCTOR_NO_OP);
    }
    catch (MessageDecodeFailedException e) {
      /* No need to FFDC this as JsMsgObject will already have done so */
      // No FFDC code needed
      throw new MessageCreateFailedException(e);
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "createTrmRouteData");
    return msg;
  }

  /* **************************************************************************/
  /* Package visibility method for making the appropriate inbound message     */
  /* **************************************************************************/

  /**
   *  Create an instance of the appropriate sub-class, e.g. TrmRouteData if
   *  the inbound message is actually a TRM RouteData Message, for the
   *  given JMO. A TRM Message of unknown type will be returned as a TrmMessage.
   *
   *  @return TrmMessage A TrmMessage of the appropriate subtype
   */
  final TrmMessage createInboundTrmMessage(JsMsgObject jmo, int messageType) {

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())SibTr.entry(tc, "createInboundTrmMessage " + messageType );

    TrmMessage trmMessage = null;

    /* Create an instance of the appropriate message subclass                 */
    switch (messageType) {

      case TrmMessageType.ROUTE_DATA_INT:
        trmMessage = new TrmRouteDataImpl(jmo);
        break;

      default:
        trmMessage = new TrmMessageImpl(jmo);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())SibTr.exit(tc, "createInboundTrmMessage");
    return trmMessage;
  }


}
