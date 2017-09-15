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
import com.ibm.ws.sib.mfp.schema.TrmAccess;
import com.ibm.ws.sib.mfp.schema.JsPayloadAccess;
import com.ibm.ws.sib.mfp.trm.*;
import com.ibm.ws.sib.utils.ras.SibTr;

import com.ibm.websphere.ras.TraceComponent;

/**
 *  TrmMessageImpl extends JsMessageImpl, and is the implementation class for
 *  the TrmMessage interface.
 *  <p>
 *  The JsMessageImpl instance contains the JsMsgObject which is the
 *  internal object which represents the API Message.
 *  The implementation classes for all the specialised TRM message types extend
 *  TrmMessageImpl, as well as implementing their specialised interface.
 */
public class TrmMessageImpl extends JsMessageImpl implements TrmMessage {

  private final static long serialVersionUID = 1L;
  private final static byte[] flattenedClassName;                               // SIB0112b.mfp.2

  private final static TraceComponent tc = SibTr.register(TrmMessageImpl.class, MfpConstants.MSG_GROUP, MfpConstants.MSG_BUNDLE);


  /* Get the flattened form of the classname                   SIB0112b.mfp.2 */
  static {
    flattenedClassName = flattenClassName(TrmMessageImpl.class.getName());
  }

  /* **************************************************************************/
  /* Constructors                                                             */
  /* **************************************************************************/

  /**
   *  Constructor for a new Jetstream TRM message.
   *
   *  This constructor should never be used explicitly.
   *  It is only to be used implicitly by the sub-classes' no-parameter constructors.
   *  The method must not actually do anything.
   */
  TrmMessageImpl() {
  }

  /**
   *  Constructor for a new Jetstream TRM message.
   *
   *  This constructor should never be used explicitly.
   *  It is only to be used implicitly by the sub-classes' constructors.
   *
   *  @param flag No-op flag to distinguish different constructors.
   *
   *  @exception MessageDecodeFailedException Thrown if such a message can not be created
   */
  TrmMessageImpl(int flag) throws MessageDecodeFailedException {
    super(flag);
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "<init>");

    /* Set the TRM message fields                                             */
    jmo.getPayloadPart().setPart(JsPayloadAccess.PAYLOAD_DATA, TrmAccess.schema);

    setJsMessageType(MessageType.TRM);
    setProducerType(ProducerType.TRM);
  }


  /**
   *  Constructor for an inbound message.
   *  (Only to be called by a superclass make method.)
   *
   *  @param inJmo The JsMsgObject representing the inbound method.
   */
  TrmMessageImpl(JsMsgObject inJmo) {
    super(inJmo);
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "<init>, inbound jmo ");
    /* Do not set any fields - they should already exist in the message       */
  }


  /* **************************************************************************/
  /* Get Methods                                                              */
  /* **************************************************************************/

  /*
   *  Get the value of the TrmMessageType from the  message.
   *
   *  Javadoc description supplied by TrmMessage interface.
   */
  public final TrmMessageType getTrmMessageType() {
    /* Get the subtype then get the corresponding TrmMessageType to return    */
    int mType = getSubtype();
    return TrmMessageType.getTrmMessageType(mType);
  }

  /*
   *  Get the Magic Number from the message.
   *
   *  Javadoc description supplied by TrmMessage interface.
   */
  public final long getMagicNumber() {
    return getPayload().getLongField(TrmAccess.MAGICNUMBER);
  }

  /*
   * Summary information for TRM messages
   */
  public void getTraceSummaryLine(StringBuilder buff) {
    super.getTraceSummaryLine(buff);
    
    buff.append(",trmMsgType=");
    buff.append(getTrmMessageType());
    
    buff.append(",magicNumber=");
    buff.append(getMagicNumber());
    
  }

  /* **************************************************************************/
  /* Set Methods                                                              */
  /* **************************************************************************/

  /*
   *  Set the Magic Number field in the message.
   *
   *  Javadoc description supplied by TrmMessage interface.
   */
  public final void setMagicNumber(long value) {
    getPayload().setLongField(TrmAccess.MAGICNUMBER, value);
  }


  /* **************************************************************************/
  /* Misc Package and Private Methods                                         */
  /* **************************************************************************/

  /* Convenience method to get the payload as a TrmSchema                     */
  JsMsgPart getPayload() {
    return getPayload(TrmAccess.schema);
  }

  /**
   * Return the name of the concrete implementation class encoded into bytes
   * using UTF8.                                              SIB0112b.mfp.2
   *
   * @return byte[] The name of the implementation class encoded into bytes.
   */
  final byte[] getFlattenedClassName() {
    return flattenedClassName;
  }

}
