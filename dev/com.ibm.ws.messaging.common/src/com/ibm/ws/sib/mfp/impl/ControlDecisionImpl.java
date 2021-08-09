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
 *  ControlDecision extends ControlMessageImpl and hence JsMessageImpl,
 *  and is the implementation class for the ControlDecision interface.
 */
final class ControlDecisionImpl extends ControlMessageImpl implements ControlDecision {

  private final static long serialVersionUID = 1L;

  private static TraceComponent tc = SibTr.register(ControlDecisionImpl.class, MfpConstants.MSG_GROUP, MfpConstants.MSG_BUNDLE);

  /* **************************************************************************/
  /* Constructors                                                             */
  /* **************************************************************************/

  /**
   *  Constructor for a new Control Decision Message.
   *
   *  This constructor should never be used except by JsMessageImpl.createNew().
   *  The method must not actually do anything.
   */
  ControlDecisionImpl() {
  }

  /**
   *  Constructor for a new Control Decision Message.
   *  To be called only by the ControlMessageFactory.
   *
   *  @param flag No-op flag to distinguish different constructors.
   *
   *  @exception MessageDecodeFailedException Thrown if such a message can not be created
   */
  ControlDecisionImpl(int flag)  throws MessageDecodeFailedException {
    super(flag);
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "<init>");
    setControlMessageType(ControlMessageType.DECISION);
  }

  /**
   *  Constructor for an inbound message.
   *  (Only to be called by a superclass make method.)
   *
   *  @param inJmo The JsMsgObject representing the inbound message
   */
  ControlDecisionImpl(JsMsgObject inJmo) {
    super(inJmo);
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "<init>, inbound jmo ");
  }


  /* **************************************************************************/
  /* Get Methods                                                              */
  /* **************************************************************************/

  /*
   *  Get the Start Tick value from the message.
   *
   *  Javadoc description supplied by ControlDecision interface.
   */
  public final long  getStartTick() {
    return jmo.getLongField(ControlAccess.BODY_DECISION_STARTTICK);
  }

  /*
   *  Get the End Tick value from the message.
   *
   *  Javadoc description supplied by ControlDecision interface.
   */
  public final long  getEndTick() {
    return jmo.getLongField(ControlAccess.BODY_DECISION_ENDTICK);
  }

  /*
   *  Get the CompletedPrefix from the message.
   *
   *  Javadoc description supplied by ControlDecision interface.
   */
  public final long  getCompletedPrefix() {
    return jmo.getLongField(ControlAccess.BODY_DECISION_COMPLETEDPREFIX);
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
    
    buff.append(",completedPrefix=");
    buff.append(getCompletedPrefix());
    
  }

  /* **************************************************************************/
  /* Set Methods                                                              */
  /* **************************************************************************/

  /*
   *  Set the Start Tick value in the message.
   *
   *  Javadoc description supplied by ControlDecision interface.
   */
  public final void setStartTick(long value) {
    jmo.setLongField(ControlAccess.BODY_DECISION_STARTTICK, value);
  }

  /*
   *  Set the End Tick value in the message.
   *
   *  Javadoc description supplied by ControlDecision interface.
   */
  public final void setEndTick(long value) {
    jmo.setLongField(ControlAccess.BODY_DECISION_ENDTICK, value);
  }

  /*
   *  Set the CompletedPrefix in the message.
   *
   *  Javadoc description supplied by ControlDecision interface.
   */
  public final void setCompletedPrefix(long value) {
    jmo.setLongField(ControlAccess.BODY_DECISION_COMPLETEDPREFIX, value);
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
