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
import com.ibm.ws.sib.mfp.control.ControlDeleteDurable;
import com.ibm.ws.sib.mfp.control.ControlMessageType;
import com.ibm.ws.sib.mfp.schema.ControlAccess;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 *  ControlDeleteDurableImpl extends ControlMessageImpl and hence JsMessageImpl,
 *  and is the implementation class for the ControlDeleteDurable interface.
 */
public class ControlDeleteDurableImpl extends ControlMessageImpl implements ControlDeleteDurable {

  private final static long serialVersionUID = 1L;

  private final static TraceComponent tc = SibTr.register(ControlDeleteDurableImpl.class, MfpConstants.MSG_GROUP, MfpConstants.MSG_BUNDLE);

  /**
   *  Constructor for a new Control Create Stream Message.
   *
   *  This constructor should never be used except by JsMessageImpl.createNew().
   *  The method must not actually do anything.
   */
  public ControlDeleteDurableImpl() {
  }

  /**
   *  Constructor for a new Control Create Stream Message.
   *  To be called only by the ControlMessageFactory.
   *
   *  @param flag No-op flag to distinguish different constructors.
   *
   *  @exception MessageDecodeFailedException Thrown if such a message can not be created
   */
  public ControlDeleteDurableImpl(int flag) throws MessageDecodeFailedException {
    super(flag);
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "<init>");
    setControlMessageType(ControlMessageType.DELETEDURABLE);
  }

  /**
   *  Constructor for an inbound message.
   *  (Only to be called by a superclass make method.)
   *
   *  @param inJmo The JsMsgObject representing the inbound message
   */
  public ControlDeleteDurableImpl(JsMsgObject inJmo) {
    super(inJmo);
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "<init>, inbound jmo ");
  }

  /* **************************************************************************/
  /* Get Methods                                                              */
  /* **************************************************************************/

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.mfp.control.ControlDeleteDurable#getRequestID()
   */
  public final long getRequestID() {
    return jmo.getLongField(ControlAccess.BODY_DELETEDURABLE_REQUESTID);
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.mfp.control.ControlDeleteDurable#getDurableSubName()
   */
  public String getDurableSubName() {
    return (String) jmo.getField(ControlAccess.BODY_DELETEDURABLE_SUBNAME);
  }

  /*
   *  Get the contents of the SecurityUserid field for the subscription.
   *
   *  Javadoc description supplied by ControlDeleteDurable interface.
   */
  public final String getSecurityUserid() {
    return (String)jmo.getField(ControlAccess.BODY_DELETEDURABLE_SECURITYUSERID);
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
    
    buff.append(",durableSubName=");
    buff.append(getDurableSubName());
    
    buff.append(",securityUserid=");
    buff.append(getSecurityUserid());
        
  }

  /* **************************************************************************/
  /* Set Methods                                                              */
  /* **************************************************************************/

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.mfp.control.ControlDeleteDurable#setRequestID(long)
   */
  public final void setRequestID(long value) {
    jmo.setLongField(ControlAccess.BODY_DELETEDURABLE_REQUESTID, value);
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.mfp.control.ControlDeleteDurable#setDurableSubName(String)
   */
  public void setDurableSubName(String name) {
    jmo.setField(ControlAccess.BODY_DELETEDURABLE_SUBNAME, name);
  }

  /*
   *  Set the contents of the SecurityUserid field for the subscription.
   *
   *  Javadoc description supplied by ControlDeleteDurable interface.
   */
  public final void setSecurityUserid(String value) {
    jmo.setField(ControlAccess.BODY_DELETEDURABLE_SECURITYUSERID, value);
  }

}
