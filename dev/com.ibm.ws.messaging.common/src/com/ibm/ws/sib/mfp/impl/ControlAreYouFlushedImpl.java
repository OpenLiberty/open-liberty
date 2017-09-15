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
import com.ibm.ws.sib.mfp.control.ControlAreYouFlushed;
import com.ibm.ws.sib.mfp.control.ControlMessageType;
import com.ibm.ws.sib.mfp.schema.ControlAccess;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 *  ControlAreYouFlushedImpl extends ControlMessageImpl and hence JsMessageImpl,
 *  and is the implementation class for the ControlAreYouFlushed interface.
 */
public class ControlAreYouFlushedImpl extends ControlMessageImpl implements ControlAreYouFlushed {

  private final static long serialVersionUID = 1L;

  private final static TraceComponent tc = SibTr.register(ControlAreYouFlushedImpl.class, MfpConstants.MSG_GROUP, MfpConstants.MSG_BUNDLE);

  /**
   *  Constructor for a new Control Are You Flushed Get Message.
   *
   *  This constructor should never be used except by JsMessageImpl.createNew().
   *  The method must not actually do anything.
   */
  public ControlAreYouFlushedImpl() {
  }

  /**
   *  Constructor for a new Control Browse Get Message.
   *  To be called only by the ControlMessageFactory.
   *
   *  @param flag No-op flag to distinguish different constructors.
   *
   *  @exception MessageDecodeFailedException Thrown if such a message can not be created
   */
  public ControlAreYouFlushedImpl(int flag) throws MessageDecodeFailedException {
    super(flag);
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "<init>");
    setControlMessageType(ControlMessageType.AREYOUFLUSHED);
  }

  /**
   *  Constructor for an inbound message.
   *  (Only to be called by a superclass make method.)
   *
   *  @param inJmo The JsMsgObject representing the inbound message
   */
  ControlAreYouFlushedImpl(JsMsgObject inJmo) {
    super(inJmo);
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "<init>, inbound jmo ");
  }

  /* **************************************************************************/
  /* Get Methods                                                              */
  /* **************************************************************************/

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.mfp.control.ControlAreYouFlushed#getRequestID()
   */
  public final long getRequestID() {
    return jmo.getLongField(ControlAccess.BODY_AREYOUFLUSHED_REQUESTID);
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
    
  }

  /* **************************************************************************/
  /* Set Methods                                                              */
  /* **************************************************************************/

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.mfp.control.ControlAreYouFlushed#setRequestID(long)
   */
  public final void setRequestID(long value) {
    jmo.setLongField(ControlAccess.BODY_AREYOUFLUSHED_REQUESTID, value);
  }

}
