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

import com.ibm.ws.sib.mfp.*;
import com.ibm.ws.sib.mfp.control.*;
import com.ibm.ws.sib.mfp.schema.ControlAccess;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.websphere.ras.TraceComponent;

/**
 *  ControlAccept extends ControlMessageImpl and hence JsMessageImpl,
 *  and is the implementation class for the ControlAccept interface.
 */
final class ControlAcceptImpl extends ControlMessageImpl implements ControlAccept {

  private final static long serialVersionUID = 1L;

  private static TraceComponent tc = SibTr.register(ControlAcceptImpl.class, MfpConstants.MSG_GROUP, MfpConstants.MSG_BUNDLE);

 
  /* **************************************************************************/
  /* Constructors                                                             */
  /* **************************************************************************/

  /**
   *  Constructor for a new Control Accept Message.
   *
   *  This constructor should never be used except by JsMessageImpl.createNew().
   *  The method must not actually do anything.
   */
  ControlAcceptImpl() {
  }

  /**
   *  Constructor for a new Control Accept Message.
   *  To be called only by the ControlMessageFactory.
   *
   *  @param flag No-op flag to distinguish different constructors.
   *
   *  @exception MessageDecodeFailedException Thrown if such a message can not be created
   */
  ControlAcceptImpl(int flag)  throws MessageDecodeFailedException {
    super(flag);
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "<init>");
    setControlMessageType(ControlMessageType.ACCEPT);
  }


  /**
   *  Constructor for an inbound message.
   *  (Only to be called by a superclass make method.)
   *
   *  @param inJmo The JsMsgObject representing the inbound method.
   */
  ControlAcceptImpl(JsMsgObject inJmo) {
    super(inJmo);
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "<init>, inbound jmo ");
  }


  /* **************************************************************************/
  /* Get Methods                                                              */
  /* **************************************************************************/

  /*
   * Get the Get Tick value for this request.  Each tick represents an accepted
   * state.
   *
   *  Javadoc description supplied by ControlAccept interface.
   */
  public long[] getTick() {
    List list = (List)jmo.getField(ControlAccess.BODY_ACCEPT_TICK);

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

    appendArray(buff, "tick", getTick());
    
  }

  /* **************************************************************************/
  /* Set Methods                                                              */
  /* **************************************************************************/

  /*
   * Set the Tick value for this request.  Each tick represents an accepted
   * state.
   *
   *  Javadoc description supplied by ControlAccept interface.
   */
  public void setTick(long[] values) {
    jmo.setField(ControlAccess.BODY_ACCEPT_TICK, values);
  }


}
