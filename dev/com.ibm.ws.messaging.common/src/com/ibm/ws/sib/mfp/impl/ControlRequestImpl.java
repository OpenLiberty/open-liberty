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
import com.ibm.ws.sib.mfp.control.ControlRequest;
import com.ibm.ws.sib.mfp.schema.ControlAccess;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 *  ControlRequestImpl extends ControlMessageImpl and hence JsMessageImpl,
 *  and is the implementation class for the ControlRequest interface.
 */
public class ControlRequestImpl extends ControlMessageImpl implements ControlRequest {

  private final static long serialVersionUID = 1L;

  private final static TraceComponent tc = SibTr.register(ControlRequestImpl.class, MfpConstants.MSG_GROUP, MfpConstants.MSG_BUNDLE);

  /**
   *  Constructor for a new Control Request Message.
   *
   *  This constructor should never be used except by JsMessageImpl.createNew().
   *  The method must not actually do anything.
   */
  public ControlRequestImpl() {
  }

  /**
   *  Constructor for a new Control Request Message.
   *  To be called only by the ControlMessageFactory.
   *
   *  @param flag No-op flag to distinguish different constructors.
   *
   *  @exception MessageDecodeFailedException Thrown if such a message can not be created
   */
  public ControlRequestImpl(int flag) throws MessageDecodeFailedException {
    super(flag);
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "<init>");
    setControlMessageType(ControlMessageType.REQUEST);
  }

  /**
   *  Constructor for an inbound message.
   *  (Only to be called by a superclass make method.)
   *
   *  @param inJmo The JsMsgObject representing the inbound message
   */
  public ControlRequestImpl(JsMsgObject inJmo) {
    super(inJmo);
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "<init>, inbound jmo ");
  }

  /* **************************************************************************/
  /* Get Methods                                                              */
  /* **************************************************************************/

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.mfp.control.ControlRequest#getFilter()
   */
  public final String[] getFilter() {
    List list = (List)jmo.getField(ControlAccess.BODY_REQUEST_FILTER);

    String lists[] = new String[list.size()];

    for (int i = 0; i < lists.length; i++)
      lists[i] = (String)list.get(i);

    return lists;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.mfp.control.ControlRequest#getRejectStartTick()
   */
  public final long[] getRejectStartTick() {
    List list = (List)jmo.getField(ControlAccess.BODY_REQUEST_REJECTSTARTTICK);

    long lists[] = new long[list.size()];

    for (int i = 0; i < lists.length; i++)
      lists[i] = ((Long)list.get(i)).longValue();

    return lists;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.mfp.control.ControlRequest#getGetTick()
   */
  public final long[] getGetTick() {
    List list = (List)jmo.getField(ControlAccess.BODY_REQUEST_GETTICK);

    long lists[] = new long[list.size()];

    for (int i = 0; i < lists.length; i++)
      lists[i] = ((Long)list.get(i)).longValue();

    return lists;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.mfp.control.ControlRequest#getTimeout()
   */
  public final long[] getTimeout() {
    List list = (List)jmo.getField(ControlAccess.BODY_REQUEST_TIMEOUT);

    long lists[] = new long[list.size()];

    for (int i = 0; i < lists.length; i++)
      lists[i] = ((Long)list.get(i)).longValue();

    return lists;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.mfp.control.ControlRequest#getSelectorDomain()
   */
  public int[] getSelectorDomain() {
    List list = (List)jmo.getField(ControlAccess.BODY_REQUEST_SELECTORDOMAIN);

    int lists[] = new int[list.size()];

    for (int i = 0; i < lists.length; i++)
      lists[i] = ((Integer)list.get(i)).intValue();

    return lists;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.mfp.control.ControlRequest#getControlDisciminator()
   */
  public String[] getControlDisciminator() {
    List list = (List)jmo.getField(ControlAccess.BODY_REQUEST_DISCRIMINATOR);

    String lists[] = new String[list.size()];

    for (int i = 0; i < lists.length; i++)
      lists[i] = (String)list.get(i);

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
    
    appendArray(buff, "filter", getFilter());
    
    appendArray(buff, "rejectStartTick", getRejectStartTick());
    
    appendArray(buff, "getTick", getGetTick());
    
    appendArray(buff, "timeout", getTimeout());
    
    appendArray(buff, "selectorDomain", getSelectorDomain());
    
    appendArray(buff, "controlDisciminator", getControlDisciminator());
    
  }
  
  /* **************************************************************************/
  /* Set Methods                                                              */
  /* **************************************************************************/

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.mfp.control.ControlRequest#setFilter(java.lang.String)
   */
  public final void setFilter(String[] values) {
    jmo.setField(ControlAccess.BODY_REQUEST_FILTER, values);
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.mfp.control.ControlRequest#setRejectStartTick(long[])
   */
  public final void setRejectStartTick(long[] values) {
    jmo.setField(ControlAccess.BODY_REQUEST_REJECTSTARTTICK, values);
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.mfp.control.ControlRequest#setGetTick(long[])
   */
  public final void setGetTick(long[] values) {
    jmo.setField(ControlAccess.BODY_REQUEST_GETTICK, values);

  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.mfp.control.ControlRequest#setTimeout(long[])
   */
  public final void setTimeout(long[] values) {
    jmo.setField(ControlAccess.BODY_REQUEST_TIMEOUT, values);
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.mfp.control.ControlRequest#setSelectorDomain(int)
   */
  public void setSelectorDomain(int[] values) {
    jmo.setField(ControlAccess.BODY_REQUEST_SELECTORDOMAIN, values);
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.mfp.control.ControlRequest#setControlDiscriminator(java.lang.String[])
   */
  public void setControlDiscriminator(String[] values) {
    jmo.setField(ControlAccess.BODY_REQUEST_DISCRIMINATOR, values);
  }
}
