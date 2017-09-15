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
 *  ControlPrevalue extends ControlMessageImpl and hence JsMessageImpl,
 *  and is the implementation class for the ControlPrevalue interface.
 */
final class ControlPrevalueImpl extends ControlMessageImpl implements ControlPrevalue {

  private final static long serialVersionUID = 1L;

  private static TraceComponent tc = SibTr.register(ControlPrevalueImpl.class, MfpConstants.MSG_GROUP, MfpConstants.MSG_BUNDLE);

  /* **************************************************************************/
  /* Constructors                                                             */
  /* **************************************************************************/

  /**
   *  Constructor for a new Control Prevalue Message.
   *
   *  This constructor should never be used except by JsMessageImpl.createNew().
   *  The method must not actually do anything.
   */
  ControlPrevalueImpl() {
  }

  /**
   *  Constructor for a new Control Prevalue Message.
   *  To be called only by the ControlMessageFactory.
   *
   *  @param flag No-op flag to distinguish different constructors.
   *
   *  @exception MessageDecodeFailedException Thrown if such a message can not be created
   */
  ControlPrevalueImpl(int flag) throws MessageDecodeFailedException {
    super(flag);
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "<init>");
    setControlMessageType(ControlMessageType.PREVALUE);
    setForce(false);
    setRequestedOnly(false);
  }

  /**
   *  Constructor for an inbound message.
   *  (Only to be called by a superclass make method.)
   *
   *  @param inJmo The JsMsgObject representing the inbound message
   */
  ControlPrevalueImpl(JsMsgObject inJmo) {
    super(inJmo);
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "<init>, inbound jmo ");
  }


  /* **************************************************************************/
  /* Get Methods                                                              */
  /* **************************************************************************/

  /*
   *  Get the Start Tick value from the message.
   *
   *  Javadoc description supplied by ControlPrevalue interface.
   */
  public final long  getStartTick() {
    return jmo.getLongField(ControlAccess.BODY_PREVALUE_STARTTICK);
  }

  /*
   *  Get the End Tick value from the message.
   *
   *  Javadoc description supplied by ControlPrevalue interface.
   */
  public final long  getEndTick() {
    return jmo.getLongField(ControlAccess.BODY_PREVALUE_ENDTICK);
  }

  /*
   *  Get the Value Tick value from the message.
   *
   *  Javadoc description supplied by ControlPrevalue interface.
   */
  public final long  getValueTick() {
    return jmo.getLongField(ControlAccess.BODY_PREVALUE_VALUETICK);
  }

  /*
   *  Get the CompletedPrefix from the message.
   *
   *  Javadoc description supplied by ControlPrevalue interface.
   */
  public final long  getCompletedPrefix() {
    return jmo.getLongField(ControlAccess.BODY_PREVALUE_COMPLETEDPREFIX);
  }

  /*
   *  Set the Force field in the message.
   *
   *  Javadoc description supplied by ControlPrevalue interface.
   */
  public final boolean getForce() {
    return jmo.getBooleanField(ControlAccess.BODY_PREVALUE_FORCE);
  }

  /*
   *  Get the RequestedOnly from the message.
   *
   *  Javadoc description supplied by ControlPrevalue interface.
   */
  public final boolean getRequestedOnly() {
    return jmo.getBooleanField(ControlAccess.BODY_PREVALUE_REQUESTEDONLY);
  }

  /*
   * Get summary trace line for this message 
   * 
   *  Javadoc description supplied by ControlMessage interface.
   */
  public void getTraceSummaryLine(StringBuilder buff) {
    
    // Get the common fields for control messages
    super.getTraceSummaryLine(buff);
    
    buff.append(",startTick=");
    buff.append(getStartTick());
    
    buff.append(",endTick=");
    buff.append(getEndTick());
    
    buff.append(",valueTick=");
    buff.append(getValueTick());
    
    buff.append(",completedPrefix=");
    buff.append(getCompletedPrefix());
    
    buff.append(",force=");
    buff.append(getForce());
    
    buff.append(",requestedOnly=");
    buff.append(getRequestedOnly());
    
  }
  
  /* **************************************************************************/
  /* Set Methods                                                              */
  /* **************************************************************************/

  /*
   *  Set the Start Tick value in the message.
   *
   *  Javadoc description supplied by ControlPrevalue interface.
   */
  public final void setStartTick(long value) {
    jmo.setLongField(ControlAccess.BODY_PREVALUE_STARTTICK, value);
  }

  /*
   *  Set the End Tick value in the message.
   *
   *  Javadoc description supplied by ControlPrevalue interface.
   */
  public final void setEndTick(long value) {
    jmo.setLongField(ControlAccess.BODY_PREVALUE_ENDTICK, value);
  }

  /*
   *  Set the Value Tick value in the message.
   *
   *  Javadoc description supplied by ControlPrevalue interface.
   */
  public final void setValueTick(long value) {
    jmo.setLongField(ControlAccess.BODY_PREVALUE_VALUETICK, value);
  }

  /*
   *  Set the CompletedPrefix in the message.
   *
   *  Javadoc description supplied by ControlPrevalue interface.
   */
  public final void setCompletedPrefix(long value) {
    jmo.setLongField(ControlAccess.BODY_PREVALUE_COMPLETEDPREFIX, value);
  }

  /*
   *  Set the Force field in the message.
   *
   *  Javadoc description supplied by ControlPrevalue interface.
   */
  public final void setForce(boolean value) {
    jmo.setBooleanField(ControlAccess.BODY_PREVALUE_FORCE, value);
  }

  /*
   *  Set the RequestedOnly in the message.
   *
   *  Javadoc description supplied by ControlPrevalue interface.
   */
  public final void setRequestedOnly(boolean value) {
    jmo.setBooleanField(ControlAccess.BODY_PREVALUE_REQUESTEDONLY, value);
  }

 /**
   * Return a String representation of the message including the
   * start and end tick values
   *
   * @return String a String representation of the message
   */
  public String toString() {
    return super.toString() + "{startTick=" + this.getStartTick() + ",endTick=" + getEndTick()+ "}";
  }
}
