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
import com.ibm.ws.sib.mfp.control.ControlMessageType;
import com.ibm.ws.sib.mfp.control.ControlResetRequestAck;
import com.ibm.ws.sib.mfp.schema.ControlAccess;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 *  ControlResetRequestAckImpl extends ControlMessageImpl and hence JsMessageImpl,
 *  and is the implementation class for the ControlResetRequestAck interface.
 */
public class ControlResetRequestAckImpl extends ControlMessageImpl implements ControlResetRequestAck {

  private final static long serialVersionUID = 1L;

  private final static TraceComponent tc = SibTr.register(ControlResetRequestAckImpl.class, MfpConstants.MSG_GROUP, MfpConstants.MSG_BUNDLE);


  /**
   *  Constructor for a new Control Request Reset Ack Message.
   *
   *  This constructor should never be used except by JsMessageImpl.createNew().
   *  The method must not actually do anything.
   */
  public ControlResetRequestAckImpl() {
  }

  /**
   *  Constructor for a new Control Request Reset Ack Message.
   *  To be called only by the ControlMessageFactory.
   *
   *  @param flag No-op flag to distinguish different constructors.
   *
   *  @exception MessageDecodeFailedException Thrown if such a message can not be created
   */
  public ControlResetRequestAckImpl(int flag) throws MessageDecodeFailedException  {
    super(flag);
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "<init>");
    setControlMessageType(ControlMessageType.RESETREQUESTACK);
  }

  /**
   *  Constructor for an inbound message.
   *  (Only to be called by a superclass make method.)
   *
   *  @param inJmo The JsMsgObject representing the inbound message
   */
  public ControlResetRequestAckImpl(JsMsgObject inJmo) {
    super(inJmo);
  }

  /* **************************************************************************/
  /* Get Methods                                                              */
  /* **************************************************************************/

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.mfp.control.ControlResetRequestAck#getDMEVersion()
   */
  public final long getDMEVersion() {
    return jmo.getLongField(ControlAccess.BODY_RESETREQUESTACK_DMEVERSION);
  }

  /*
   * Get summary trace line for this message 
   * 
   *  Javadoc description supplied by ControlMessage interface.
   */
  public void getTraceSummaryLine(StringBuilder buff) {
    
    // Get the common fields for control messages
    super.getTraceSummaryLine(buff);

    buff.append(",dmeVersion=");
    buff.append(getDMEVersion());
    
  }
  
  /* **************************************************************************/
  /* Set Methods                                                              */
  /* **************************************************************************/

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.mfp.control.ControlResetRequestAck#setDMEVersion(long)
   */
  public final void setDMEVersion(long value) {
    jmo.setLongField(ControlAccess.BODY_RESETREQUESTACK_DMEVERSION, value);
  }

}
