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
import com.ibm.ws.sib.mfp.control.ControlBrowseEnd;
import com.ibm.ws.sib.mfp.control.ControlMessageType;
import com.ibm.ws.sib.mfp.schema.ControlAccess;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 *  ControlBrowseEndImpl extends ControlMessageImpl and hence JsMessageImpl,
 *  and is the implementation class for the ControlBrowseGet interface.
 */
public class ControlBrowseEndImpl extends ControlMessageImpl implements ControlBrowseEnd {

  private final static long serialVersionUID = 1L;

  private final static TraceComponent tc = SibTr.register(ControlBrowseEndImpl.class, MfpConstants.MSG_GROUP, MfpConstants.MSG_BUNDLE);

  /**
   *  Constructor for a new Control Browse End Message.
   *
   *  This constructor should never be used except by JsMessageImpl.createNew().
   *  The method must not actually do anything.
   */
  public ControlBrowseEndImpl() {
  }

  /**
   *  Constructor for a new Control Browse End Message.
   *  To be called only by the ControlMessageFactory.
   *
   *  @param flag No-op flag to distinguish different constructors.
   *
   *  @exception MessageDecodeFailedException Thrown if such a message can not be created
   */
  public ControlBrowseEndImpl(int flag) throws MessageDecodeFailedException {
    super(flag);
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "<init>");
    setControlMessageType(ControlMessageType.BROWSEEND);
  }

  /**
   *  Constructor for an inbound message.
   *  (Only to be called by a superclass make method.)
   *
   *  @param inJmo The JsMsgObject representing the inbound message
   */
  public ControlBrowseEndImpl(JsMsgObject inJmo) {
    super(inJmo);
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "<init>, inbound jmo ");
  }

  /* **************************************************************************/
  /* Get Methods                                                              */
  /* **************************************************************************/

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.mfp.control.ControlBrowseEnd#getBrowseID()
   */
  public final long getBrowseID() {
    return jmo.getLongField(ControlAccess.BODY_BROWSEEND_BROWSEID);
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.mfp.control.ControlBrowseEnd#getExceptionCode()
   */
  public final int getExceptionCode() {
    return jmo.getIntField(ControlAccess.BODY_BROWSEEND_EXCEPTIONCODE);
  }

  /*
   * Get summary trace line for this message 
   * 
   *  Javadoc description supplied by ControlMessage interface.
   */
  public void getTraceSummaryLine(StringBuilder buff) {
    
    // Get the common fields for control messages
    super.getTraceSummaryLine(buff);
    
    buff.append(",browseID=");
    buff.append(getBrowseID());
    
    buff.append(",exceptionCode=");
    buff.append(getExceptionCode());
    
  }

  /* **************************************************************************/
  /* Set Methods                                                              */
  /* **************************************************************************/

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.mfp.control.ControlBrowseEnd#setBrowseID(long)
   */
  public final void setBrowseID(long value) {
    jmo.setLongField(ControlAccess.BODY_BROWSEEND_BROWSEID, value);
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.mfp.control.ControlBrowseEnd#setExceptionCode(int)
   */
  public final void setExceptionCode(int value) {
    jmo.setIntField(ControlAccess.BODY_BROWSEEND_EXCEPTIONCODE, value);
  }

}
