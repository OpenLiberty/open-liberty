/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.mfp.impl;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.mfp.MessageDecodeFailedException;
import com.ibm.ws.sib.mfp.MfpConstants;
import com.ibm.ws.sib.mfp.control.ControlMessageType;
import com.ibm.ws.sib.mfp.control.ControlRequestFlush;
import com.ibm.ws.sib.mfp.schema.ControlAccess;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 *  ControlCreateStreamImpl extends ControlMessageImpl and hence JsMessageImpl,
 *  and is the implementation class for the ControlCreateStream interface.
 */
public class ControlRequestFlushImpl extends ControlMessageImpl implements ControlRequestFlush {

  private final static long serialVersionUID = 1L;

  private final static TraceComponent tc = SibTr.register(ControlRequestFlushImpl.class, MfpConstants.MSG_GROUP, MfpConstants.MSG_BUNDLE);

  /**
   *  Constructor for a new Control Request Flush Info Message.
   *
   *  This constructor should never be used except by JsMessageImpl.createNew().
   *  The method must not actually do anything.
   */
  public ControlRequestFlushImpl() {
  }

  /**
   *  Constructor for a new Control Request Flush Info Message.
   *  To be called only by the ControlMessageFactory.
   *
   *  @param flag No-op flag to distinguish different constructors.
   *
   *  @exception MessageDecodeFailedException Thrown if such a message can not be created
   */
  public ControlRequestFlushImpl(int flag) throws MessageDecodeFailedException {
    super(flag);
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "<init>");
    setControlMessageType(ControlMessageType.REQUESTFLUSH);
  }

  /**
   *  Constructor for an inbound message.
   *  (Only to be called by a superclass make method.)
   *
   *  @param inJmo The JsMsgObject representing the inbound message
   */
  public ControlRequestFlushImpl(JsMsgObject inJmo) {
    super(inJmo);
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "<init>, inbound jmo ");
  }

  /* **************************************************************************/
  /* Get Methods                                                              */
  /* **************************************************************************/

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.mfp.control.ControlCreateStream#getRequestID()
   */
  public final long getRequestID() {
    return jmo.getLongField(ControlAccess.BODY_REQUESTFLUSH_REQUESTID);
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.mfp.control.ControlRequestFlush#getIndoubtDiscard()
   */
  public boolean getIndoubtDiscard() {
    return jmo.getBooleanField(ControlAccess.BODY_REQUESTFLUSH_INDOUBTDISCARD);
  }

  /*
   * Get summary trace line for this message 
   * 
   *  Javadoc description supplied by ControlMessage interface.
   */
  public void getTraceSummaryLine(StringBuilder buff) {
    
    // Get the common fields for control messages
    super.getTraceSummaryLine(buff);
    
    buff.append("requestID=");
    buff.append(getRequestID());
    
    buff.append("indoubtDiscard=");
    buff.append(getIndoubtDiscard());
    
  }
  
  /* **************************************************************************/
  /* Set Methods                                                              */
  /* **************************************************************************/

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.mfp.control.ControlCreateStream#setRequestID(long)
   */
  public final void setRequestID(long value) {
    jmo.setLongField(ControlAccess.BODY_REQUESTFLUSH_REQUESTID, value);
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.mfp.control.ControlRequestFlush#setIndoubtDiscard(boolean)
   */
  public void setIndoubtDiscard(boolean value) {
    jmo.setBooleanField(ControlAccess.BODY_REQUESTFLUSH_INDOUBTDISCARD, value);
  }
}
