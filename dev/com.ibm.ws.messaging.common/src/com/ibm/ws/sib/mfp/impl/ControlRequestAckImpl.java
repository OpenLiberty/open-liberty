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
import com.ibm.ws.sib.mfp.control.ControlMessageType;
import com.ibm.ws.sib.mfp.control.ControlRequestAck;
import com.ibm.ws.sib.mfp.schema.ControlAccess;
import com.ibm.ws.sib.utils.ras.SibTr;

  /**
   *  ControlRequestAckImpl extends ControlMessageImpl and hence JsMessageImpl,
   *  and is the implementation class for the ControlRequestAck interface.
   */
public class ControlRequestAckImpl extends ControlMessageImpl implements ControlRequestAck {

  private final static long serialVersionUID = 1L;

  private final static TraceComponent tc = SibTr.register(ControlRequestAckImpl.class, MfpConstants.MSG_GROUP, MfpConstants.MSG_BUNDLE);


  /**
   *  Constructor for a new Control Request Ack Message.
   *
   *  This constructor should never be used except by JsMessageImpl.createNew().
   *  The method must not actually do anything.
   */
  public ControlRequestAckImpl() {
  }

  /**
   *  Constructor for a new Control Request Ack Message.
   *  To be called only by the ControlMessageFactory.
   *
   *  @param flag No-op flag to distinguish different constructors.
   *
   *  @exception MessageDecodeFailedException Thrown if such a message can not be created
   */
  public ControlRequestAckImpl(int flag) throws MessageDecodeFailedException {
    super(flag);
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "<init>");
    setControlMessageType(ControlMessageType.REQUESTACK);
  }

  /**
   *  Constructor for an inbound message.
   *  (Only to be called by a superclass make method.)
   *
   *  @param inJmo The JsMsgObject representing the inbound message
   */
  public ControlRequestAckImpl(JsMsgObject inJmo) {
    super(inJmo);
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "<init>, inbound jmo ");
  }

  /* **************************************************************************/
  /* Get Methods                                                              */
  /* **************************************************************************/

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.mfp.control.ControlRequestAck#getDMEVersion()
   */
  public final long getDMEVersion() {
    return jmo.getLongField(ControlAccess.BODY_REQUESTACK_DMEVERSION);
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.mfp.control.ControlRequestAck#getTick()
   */
  public final long[] getTick() {
    List list = (List)jmo.getField(ControlAccess.BODY_REQUESTACK_TICK);

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
    
    buff.append("dmeVersion=");
    buff.append(getDMEVersion());

    appendArray(buff, "tick", getTick());
    
  }
  
  /* **************************************************************************/
  /* Set Methods                                                              */
  /* **************************************************************************/

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.mfp.control.ControlRequestAck#setDMEVersion(long)
   */
  public final void setDMEVersion(long value) {
    jmo.setLongField(ControlAccess.BODY_REQUESTACK_DMEVERSION, value);
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.mfp.control.ControlRequestAck#setTick(long[])
   */
  public final void setTick(long[] values) {
    jmo.setField(ControlAccess.BODY_REQUESTACK_TICK, values);
  }

}
