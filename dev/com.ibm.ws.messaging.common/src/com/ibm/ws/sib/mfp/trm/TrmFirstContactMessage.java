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
package com.ibm.ws.sib.mfp.trm;

import com.ibm.ws.sib.mfp.MessageEncodeFailedException;

/**
 * TrmFirstContactMessage is the basic interface for accessing and processing any
 * Topology and Routing Manager First Contact Messages.
 * <p>
 * All of the TRM First Contact messages are specializations of
 * TRMFirstContactMessage and can be 'made' from an existing TRMFirstContactMessage
 * of the appropriate type.
 * The TRMFirstContactMessage interface provides get/set methods for the common
 * fields. It also provides the method for encoding a message
 * for transmission.
 *
 */
public interface TrmFirstContactMessage {

  /* **************************************************************************/
  /* Methods for making more specialised messages                             */
  /* **************************************************************************/

  /**
   *  Convert the existing inbound TrmFirstContactMessage into a
   *  TrmClientBootstrapRequest.
   *
   *  @return A TrmClientBootstrapRequest representing the same message.
   */
  public TrmClientBootstrapRequest makeInboundTrmClientBootstrapRequest();

  /**
   *  Convert the existing inbound TrmFirstContactMessage into a
   *  TrmClientBootstrapReply.
   *
   *  @return A TrmClientBootstrapReply representing the same message.
   */
  public TrmClientBootstrapReply makeInboundTrmClientBootstrapReply();

  /**
   *  Convert the existing inbound TrmFirstContactMessage into a
   *  TrmClientAttachRequest.
   *
   *  @return A TrmClientAttachRequest representing the same message.
   */
  public TrmClientAttachRequest makeInboundTrmClientAttachRequest();

  /**
   *  Convert the existing inbound TrmFirstContactMessage into a
   *  TrmClientAttachRequest2.
   *
   *  @return A TrmClientAttachRequest2 representing the same message.
   */
  public TrmClientAttachRequest2 makeInboundTrmClientAttachRequest2();

  /**
   *  Convert the existing inbound TrmFirstContactMessage into a
   *  TrmClientAttachReply.
   *
   *  @return A TrmClientAttachReply representing the same message.
   */
  public TrmClientAttachReply makeInboundTrmClientAttachReply();

  /**
   *  Convert the existing inbound TrmFirstContactMessage into a
   *  TrmMeConnectRequest.
   *
   *  @return A TrmMeConnectRequest representing the same message.
   */
  public TrmMeConnectRequest makeInboundTrmMeConnectRequest();

  /**
   *  Convert the existing inbound TrmFirstContactMessage into a
   *  TrmMeConnectReply.
   *
   *  @return A TrmMeConnectReply representing the same message.
   */
  public TrmMeConnectReply makeInboundTrmMeConnectReply();

  /**
   *  Convert the existing inbound TrmFirstContactMessage into a
   *  TrmMeLinkReply.
   *
   *  @return A TrmMeLinkReply representing the same message.
   */
  public TrmMeLinkRequest makeInboundTrmMeLinkRequest();

  /**
   *  Convert the existing inbound TrmFirstContactMessage into a
   *  TrmMeLinkReply.
   *
   *  @return A TrmMeLinkReply representing the same message.
   */
  public TrmMeLinkReply makeInboundTrmMeLinkReply();

  /**
   *  Convert the existing inbound TrmFirstContactMessage into a
   *  TrmMeBridgeReply.
   *
   *  @return A TrmMeBridgeReply representing the same message.
   */
  public TrmMeBridgeRequest makeInboundTrmMeBridgeRequest();

  /**
   *  Convert the existing inbound TrmFirstContactMessage into a
   *  TrmMeBridgeReply.
   *
   *  @return A TrmMeBridgeReply representing the same message.
   */
  public TrmMeBridgeReply makeInboundTrmMeBridgeReply();

  /**
   *  Convert the existing inbound TrmFirstContactMessage into a
   *  TrmMeBridgeBootstrapRequest
   *
   *  @return A TrmMeBridgeBootstrapRequest representing the same message.
   */
  public TrmMeBridgeBootstrapRequest makeInboundTrmMeBridgeBootstrapRequest();

  /**
   *  Convert the existing inbound TrmFirstContactMessage into a
   *  TrmMeBridgeBootstrapReply
   *
   *  @return A TrmMeBridgeBootstrapReply representing the same message.
   */
  public TrmMeBridgeBootstrapReply makeInboundTrmMeBridgeBootstrapReply();

  /* **************************************************************************/
  /* Methods for encoding                                                     */
  /* **************************************************************************/

  /**
   *  Encode the message into a byte array for transmission.
   *
   *  @param conn The CommsConnection over which the encoded message is to be sent.  This
   *  may be null if the message is not being encoded for transmission.
   *  @return A byte array containing the encoded message.
   *
   *  @exception MessageEncodeFailedException Thrown if the message could not be encoded
   */
  public byte[] encode(Object conn) throws MessageEncodeFailedException;

  /* **************************************************************************/
  /* Get Methods                                                              */
  /* **************************************************************************/

  /**
   *  Get the value of the TrmFirstContactMessageType from the  message.
   *
   *  @return The TrmFirstContactMessageType singleton which distinguishes
   *          the type of this message.
   */
  public TrmFirstContactMessageType getMessageType();
}
