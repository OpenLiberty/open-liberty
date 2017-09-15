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
import com.ibm.ws.sib.mfp.control.*;
import com.ibm.ws.sib.mfp.schema.ControlAccess;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.websphere.ras.TraceComponent;

/**
 *  ControlAckExpected extends ControlMessageImpl and hence JsMessageImpl,
 *  and is the implementation class for the ControlAckExpected interface.
 */
final class ControlAckExpectedImpl extends ControlMessageImpl implements ControlAckExpected {

  private final static long serialVersionUID = 1L;

  private static TraceComponent tc = SibTr.register(ControlAckExpectedImpl.class, MfpConstants.MSG_GROUP, MfpConstants.MSG_BUNDLE);


  /* **************************************************************************/
  /* Constructors                                                             */
  /* **************************************************************************/

  /**
   *  Constructor for a new Control AckExpected Message.
   *
   *  This constructor should never be used except by JsMessageImpl.createNew().
   *  The method must not actually do anything.
   */
  ControlAckExpectedImpl() {
  }

  /**
   *  Constructor for a new Control AckExpected Message.
   *  To be called only by the ControlMessageFactory.
   *
   *  @param flag No-op flag to distinguish different constructors.
   *
   *  @exception MessageDecodeFailedException Thrown if such a message can not be created
   */
  ControlAckExpectedImpl(int flag)  throws MessageDecodeFailedException {
    super(flag);
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "<init>");
    setControlMessageType(ControlMessageType.ACKEXPECTED);
  }


  /**
   *  Constructor for an inbound message.
   *  (Only to be called by a superclass make method.)
   *
   *  @param inJmo The JsMsgObject representing the inbound message
   */
  ControlAckExpectedImpl(JsMsgObject inJmo) {
    super(inJmo);
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "<init>, inbound jmo ");
  }


  /* **************************************************************************/
  /* Get Methods                                                              */
  /* **************************************************************************/

  /*
   *  Get the Tick value from the message.
   *
   *  Javadoc description supplied by ControlAckExpected interface.
   */
  public final long  getTick() {
    return jmo.getLongField(ControlAccess.BODY_ACKEXPECTED_TICK);
  }

  /*
   * Get summary trace line for this message 
   * 
   *  Javadoc description supplied by ControlMessage interface.
   */
  public void getTraceSummaryLine(StringBuilder buff) {
    
    // Get the common fields for control messages
    super.getTraceSummaryLine(buff);
    
    buff.append(",tick=");
    buff.append(getTick());
    
  }

  /* **************************************************************************/
  /* Set Methods                                                              */
  /* **************************************************************************/

  /*
   *  Set the Tick value in the message.
   *
   *  Javadoc description supplied by ControlAckExpected interface.
   */
  public final void setTick(long value) {
    jmo.setLongField(ControlAccess.BODY_ACKEXPECTED_TICK, value);
  }

 /**
   * Return a String representation of the message including the
   * Tick
   *
   * @return String a String representation of the message
   */
  public String toString() {
    return super.toString() + "{tick=" + this.getTick() + "}";
  }
}
