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
import com.ibm.ws.sib.mfp.control.ControlBrowseStatus;
import com.ibm.ws.sib.mfp.control.ControlMessageType;
import com.ibm.ws.sib.mfp.schema.ControlAccess;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 *  ControlBrowseStatusImpl extends ControlMessageImpl and hence JsMessageImpl,
 *  and is the implementation class for the ControlBrowseStatus interface.
 */
public class ControlBrowseStatusImpl extends ControlMessageImpl implements ControlBrowseStatus {

  private final static long serialVersionUID = 1L;

  private final static TraceComponent tc = SibTr.register(ControlBrowseStatusImpl.class, MfpConstants.MSG_GROUP, MfpConstants.MSG_BUNDLE);

  /**
   *  Constructor for a new Control Browse Status Message.
   *
   *  This constructor should never be used except by JsMessageImpl.createNew().
   *  The method must not actually do anything.
   */
  public ControlBrowseStatusImpl() {
  }

  /**
   *  Constructor for a new Control Browse Status Message.
   *  To be called only by the ControlMessageFactory.
   *
   *  @param flag No-op flag to distinguish different constructors.
   *
   *  @exception MessageDecodeFailedException Thrown if such a message can not be created
   */
  public ControlBrowseStatusImpl(int flag) throws MessageDecodeFailedException {
    super(flag);
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "<init>");
    setControlMessageType(ControlMessageType.BROWSESTATUS);
  }

  /**
   *  Constructor for an inbound message.
   *  (Only to be called by a superclass make method.)
   *
   *  @param inJmo The JsMsgObject representing the inbound message
   */
  public ControlBrowseStatusImpl(JsMsgObject inJmo) {
    super(inJmo);
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "<init>, inbound jmo ");
  }

  /* **************************************************************************/
  /* Get Methods                                                              */
  /* **************************************************************************/

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.mfp.control.ControlBrowseStatus#getBrowseID()
   */
  public final long getBrowseID() {
    return jmo.getLongField(ControlAccess.BODY_BROWSESTATUS_BROWSEID);
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.mfp.control.ControlBrowseStatus#getStatus()
   */
  public final int getStatus() {
    return jmo.getIntField(ControlAccess.BODY_BROWSESTATUS_STATUS);
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
    
    buff.append(",status=");
    buff.append(getStatus());
    
  }

  /* **************************************************************************/
  /* Set Methods                                                              */
  /* **************************************************************************/

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.mfp.control.ControlBrowseStatus#setBrowseID(long)
   */
  public final void setBrowseID(long value) {
    jmo.setLongField(ControlAccess.BODY_BROWSESTATUS_BROWSEID, value);
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.mfp.control.ControlBrowseStatus#setStatus(int)
   */
  public final void setStatus(int value) {
    jmo.setIntField(ControlAccess.BODY_BROWSESTATUS_STATUS, value);
  }

}
