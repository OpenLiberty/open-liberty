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
import com.ibm.ws.sib.mfp.MessageDecodeFailedException;
import com.ibm.ws.sib.mfp.MfpConstants;
import com.ibm.ws.sib.mfp.control.ControlHighestGeneratedTick;
import com.ibm.ws.sib.mfp.control.ControlMessageType;
import com.ibm.ws.sib.mfp.schema.ControlAccess;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 *  ControlHighestGeneratedTickImpl extends ControlMessageImpl and hence JsMessageImpl,
 *  and is the implementation class for the ControlHighestGeneratedTick interface.
 */
public class ControlHighestGeneratedTickImpl extends ControlMessageImpl implements ControlHighestGeneratedTick {

  private final static long serialVersionUID = 1L;

  private final static TraceComponent tc = SibTr.register(ControlHighestGeneratedTickImpl.class, MfpConstants.MSG_GROUP, MfpConstants.MSG_BUNDLE);

  /**
   *  Constructor for a new Control Highest Generated Tick Message.
   *
   *  This constructor should never be used except by JsMessageImpl.createNew().
   *  The method must not actually do anything.
   */
  public ControlHighestGeneratedTickImpl() {
  }

  /**
   *  Constructor for a new Control Highest Generated Tick Message.
   *  To be called only by the ControlMessageFactory.
   *
   *  @param flag No-op flag to distinguish different constructors.
   *
   *  @exception MessageDecodeFailedException Thrown if such a message can not be created
   */
  public ControlHighestGeneratedTickImpl(int flag) throws MessageDecodeFailedException {
    super(flag);
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "<init>");
    setControlMessageType(ControlMessageType.HIGHESTGENERATEDTICK);
  }

  /**
   *  Constructor for an inbound message.
   *  (Only to be called by a superclass make method.)
   *
   *  @param inJmo The JsMsgObject representing the inbound message
   */
  public ControlHighestGeneratedTickImpl(JsMsgObject inJmo) {
    super(inJmo);
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "<init>, inbound jmo ");
  }

  /* **************************************************************************/
  /* Get Methods                                                              */
  /* **************************************************************************/

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.mfp.control.ControlHighestGeneratedTick#getRequestID()
   */
  public final long getRequestID() {
    return jmo.getLongField(ControlAccess.BODY_HIGHESTGENERATEDTICK_REQUESTID);
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.mfp.control.ControlHighestGeneratedTick#getTick()
   */
  public final long getTick() {
    return jmo.getLongField(ControlAccess.BODY_HIGHESTGENERATEDTICK_TICK);
  }


  /*
   * Get summary trace line for this message 
   * 
   *  Javadoc description supplied by ControlMessage interface.
   */
  public void getTraceSummaryLine(StringBuilder buff) {
    
    // Get the common fields for control messages
    super.getTraceSummaryLine(buff);
    
    buff.append(",requestID=");
    buff.append(getRequestID());
    
    buff.append(",tick=");
    buff.append(getTick());
    
  }
  
  /* **************************************************************************/
  /* Set Methods                                                              */
  /* **************************************************************************/

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.mfp.control.ControlHighestGeneratedTick#setRequestID(long)
   */
  public final void setRequestID(long value) {
    jmo.setLongField(ControlAccess.BODY_HIGHESTGENERATEDTICK_REQUESTID, value);

  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.mfp.control.ControlHighestGeneratedTick#setTick(long)
   */
  public final void setTick(long value) {
    jmo.setLongField(ControlAccess.BODY_HIGHESTGENERATEDTICK_TICK, value);
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
