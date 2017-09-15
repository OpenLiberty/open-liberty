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

import java.util.List;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.mfp.MessageDecodeFailedException;
import com.ibm.ws.sib.mfp.MfpConstants;
import com.ibm.ws.sib.mfp.control.ControlCompleted;
import com.ibm.ws.sib.mfp.control.ControlMessageType;
import com.ibm.ws.sib.mfp.schema.ControlAccess;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 *  ControlCompletedImpl extends ControlMessageImpl and hence JsMessageImpl,
 *  and is the implementation class for the ControlCompleted interface.
 */
public class ControlCompletedImpl extends ControlMessageImpl implements ControlCompleted {

  private final static long serialVersionUID = 1L;

  private final static TraceComponent tc = SibTr.register(ControlCompletedImpl.class, MfpConstants.MSG_GROUP, MfpConstants.MSG_BUNDLE);

  /**
   *  Constructor for a new Control Completed Message.
   *
   *  This constructor should never be used except by JsMessageImpl.createNew().
   *  The method must not actually do anything.
   */
  public ControlCompletedImpl() {
  }

  /**
   *  Constructor for a new Control Completed Message.
   *  To be called only by the ControlMessageFactory.
   *
   *  @param flag No-op flag to distinguish different constructors.
   *
   *  @exception MessageDecodeFailedException Thrown if such a message can not be created
   */
  public ControlCompletedImpl(int flag) throws MessageDecodeFailedException {
    super(flag);
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "<init>");
    setControlMessageType(ControlMessageType.COMPLETED);
  }

  /**
   *  Constructor for an inbound message.
   *  (Only to be called by a superclass make method.)
   *
   *  @param inJmo The JsMsgObject representing the inbound message
   */
  public ControlCompletedImpl(JsMsgObject inJmo) {
    super(inJmo);
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "<init>, inbound jmo ");
  }

  /* **************************************************************************/
  /* Get Methods                                                              */
  /* **************************************************************************/

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.mfp.control.ControlCompleted#getStartTick()
   */
  public final long[] getStartTick() {
    List list = (List)jmo.getField(ControlAccess.BODY_COMPLETED_STARTTICK);

    long lists[] = new long[list.size()];

    for (int i = 0; i < lists.length; i++)
      lists[i] = ((Long)list.get(i)).longValue();

    return lists;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.mfp.control.ControlCompleted#getEndTick()
   */
  public final long[] getEndTick() {
    List list = (List)jmo.getField(ControlAccess.BODY_COMPLETED_ENDTICK);

    long lists[] = new long[list.size()];

    for (int i = 0; i < lists.length; i++)
      lists[i] = ((Long)list.get(i)).longValue();

    return lists;
  }

  /*
   * Get summary trace line for this message 
   * 
   *  Javadoc description supplied by ControlMessage interface.
   */
  public void getTraceSummaryLine(StringBuilder buff) {
    
    // Get the common fields for control messages
    super.getTraceSummaryLine(buff);
    
    appendArray(buff, "startTick", getStartTick());
    
    appendArray(buff, "endTick", getStartTick());
    
  }

  /* **************************************************************************/
  /* Set Methods                                                              */
  /* **************************************************************************/

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.mfp.control.ControlCompleted#setStartTick(long[])
   */
  public final void setStartTick(long[] values) {
    jmo.setField(ControlAccess.BODY_COMPLETED_STARTTICK, values);
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.mfp.control.ControlCompleted#setEndTick(long[])
   */
  public final void setEndTick(long[] values) {
    jmo.setField(ControlAccess.BODY_COMPLETED_ENDTICK, values);
  }

}
