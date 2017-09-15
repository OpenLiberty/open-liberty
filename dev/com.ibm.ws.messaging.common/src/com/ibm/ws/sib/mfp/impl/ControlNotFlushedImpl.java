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
import com.ibm.ws.sib.mfp.control.ControlNotFlushed;
import com.ibm.ws.sib.mfp.schema.ControlAccess;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 *  ControlNotFlushedImpl extends ControlMessageImpl and hence JsMessageImpl,
 *  and is the implementation class for the ControlNotFlushed interface.
 */
public class ControlNotFlushedImpl extends ControlMessageImpl implements ControlNotFlushed {

  private final static long serialVersionUID = 1L;

  private final static TraceComponent tc = SibTr.register(ControlNotFlushedImpl.class, MfpConstants.MSG_GROUP, MfpConstants.MSG_BUNDLE);

  /**
   *  Constructor for a new Control Not Flushed Message.
   *
   *  This constructor should never be used except by JsMessageImpl.createNew().
   *  The method must not actually do anything.
   */
  public ControlNotFlushedImpl() {
  }

  /**
   *  Constructor for a new Control Not Flushed Message.
   *  To be called only by the ControlMessageFactory.
   *
   *  @param flag No-op flag to distinguish different constructors.
   *
   *  @exception MessageDecodeFailedException Thrown if such a message can not be created
   */
  public ControlNotFlushedImpl(int flag) throws MessageDecodeFailedException {
    super(flag);
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "<init>");
    setControlMessageType(ControlMessageType.NOTFLUSHED);
  }

  /**
   *  Constructor for an inbound message.
   *  (Only to be called by a superclass make method.)
   *
   *  @param inJmo The JsMsgObject representing the inbound message
   */
  public ControlNotFlushedImpl(JsMsgObject inJmo) {
    super(inJmo);
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "<init>, inbound jmo ");
  }

  /* **************************************************************************/
  /* Get Methods                                                              */
  /* **************************************************************************/

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.mfp.control.ControlNotFlushed#getRequestID()
   */
  public final long getRequestID() {
    return jmo.getLongField(ControlAccess.BODY_NOTFLUSHED_REQUESTID);
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.mfp.control.ControlNotFlushed#getCompletedPrefixPriority()
   */
  public int[] getCompletedPrefixPriority() {
    List list = (List)jmo.getField(ControlAccess.BODY_NOTFLUSHED_COMPLETEDPRIORITY);

    int lists[] = new int[list.size()];

    for (int i = 0; i < lists.length; i++)
      lists[i] = ((Integer)list.get(i)).intValue();

    return lists;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.mfp.control.ControlNotFlushed#getCompletedPrefixQOS()
   */
  public final int[] getCompletedPrefixQOS() {
    List list = (List)jmo.getField(ControlAccess.BODY_NOTFLUSHED_COMPLETEDQOS);

    int lists[] = new int[list.size()];

    for (int i = 0; i < lists.length; i++)
      lists[i] = ((Integer)list.get(i)).intValue();

    return lists;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.mfp.control.ControlNotFlushed#getCompletedPrefixTicks()
   */
  public final long[] getCompletedPrefixTicks() {
    List list = (List)jmo.getField(ControlAccess.BODY_NOTFLUSHED_COMPLETEDPREFIX);

    long lists[] = new long[list.size()];

    for (int i = 0; i < lists.length; i++)
      lists[i] = ((Long)list.get(i)).longValue();

    return lists;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.mfp.control.ControlNotFlushed#getDuplicatePrefixPriority()
   */
  public final int[] getDuplicatePrefixPriority() {
    List list = (List)jmo.getField(ControlAccess.BODY_NOTFLUSHED_DUPLICATEPRIORITY);

    int lists[] = new int[list.size()];

    for (int i = 0; i < lists.length; i++)
      lists[i] = ((Integer)list.get(i)).intValue();

    return lists;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.mfp.control.ControlNotFlushed#getDuplicatePrefixQOS()
   */
  public final int[] getDuplicatePrefixQOS() {
    List list = (List)jmo.getField(ControlAccess.BODY_NOTFLUSHED_DUPLICATEQOS);

    int lists[] = new int[list.size()];

    for (int i = 0; i < lists.length; i++)
      lists[i] = ((Integer)list.get(i)).intValue();

    return lists;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.mfp.control.ControlNotFlushed#getDuplicatePrefixTicks()
   */
  public final long[] getDuplicatePrefixTicks() {
    List list = (List)jmo.getField(ControlAccess.BODY_NOTFLUSHED_DUPLICATEPREFIX);

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
    
    buff.append(",requestID=");
    buff.append(getRequestID());
    
    appendArray(buff, "completedPrefixPriority", getCompletedPrefixPriority());

    appendArray(buff, "completedPrefixQOS", getCompletedPrefixQOS());

    appendArray(buff, "completedPrefixTicks", getCompletedPrefixTicks());

    appendArray(buff, "duplicatePrefixPriority", getDuplicatePrefixPriority());

    appendArray(buff, "duplicatePrefixQOS", getDuplicatePrefixQOS());

    appendArray(buff, "duplicatePrefixTicks", getDuplicatePrefixTicks());
    
  }
  
  /* **************************************************************************/
  /* Set Methods                                                              */
  /* **************************************************************************/

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.mfp.control.ControlNotFlushed#setRequestID(long)
   */
  public final void setRequestID(long value) {
    jmo.setLongField(ControlAccess.BODY_NOTFLUSHED_REQUESTID, value);
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.mfp.control.ControlNotFlushed#setCompletedPrefixPriority(int[])
   */
  public final void setCompletedPrefixPriority(int[] values) {
    jmo.setField(ControlAccess.BODY_NOTFLUSHED_COMPLETEDPRIORITY, values);
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.mfp.control.ControlNotFlushed#setCompletedPrefixQOS(int[])
   */
  public final void setCompletedPrefixQOS(int[] values) {
    jmo.setField(ControlAccess.BODY_NOTFLUSHED_COMPLETEDQOS, values);
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.mfp.control.ControlNotFlushed#setCompletedPrefixTicks(long[])
   */
  public final void setCompletedPrefixTicks(long[] values) {
    jmo.setField(ControlAccess.BODY_NOTFLUSHED_COMPLETEDPREFIX, values);
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.mfp.control.ControlNotFlushed#setDuplicatePrefixPriority(int[])
   */
  public final void setDuplicatePrefixPriority(int[] values) {
    jmo.setField(ControlAccess.BODY_NOTFLUSHED_DUPLICATEPRIORITY, values);
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.mfp.control.ControlNotFlushed#setDuplicatePrefixQOS(int[])
   */
  public final void setDuplicatePrefixQOS(int[] values) {
    jmo.setField(ControlAccess.BODY_NOTFLUSHED_DUPLICATEQOS, values);
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.mfp.control.ControlNotFlushed#getDuplicatePrefixTicks(long[])
   */
  public final void setDuplicatePrefixTicks(long[] values) {
    jmo.setField(ControlAccess.BODY_NOTFLUSHED_DUPLICATEPREFIX, values);
  }

}
