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

import com.ibm.ws.sib.mfp.MfpConstants;
import com.ibm.ws.sib.mfp.jmf.JMFException;
import com.ibm.ws.sib.mfp.jmf.JMFMessage;
import com.ibm.ws.sib.mfp.jmf.JMFNativePart;
import com.ibm.ws.sib.mfp.jmf.JMFSchema;
import com.ibm.ws.sib.utils.ras.SibTr;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;

/**
 * This class represents part of a complete Jetstream Message Object that is
 * described by a JMF schema.  When the complete message contains nested
 * schema defintions there will be one JsMsgPart for each schema.  Accessor
 * methods are defined to get and set data values for fields in the message
 * part - these wrapper the JMF calls to catch and log exceptions.
 */

public class JsMsgPart {
  private static TraceComponent tc = SibTr.register(JsMsgPart.class, MfpConstants.MSG_GROUP, MfpConstants.MSG_BUNDLE);

  JMFNativePart jmfPart;   // The JMF part associated with this message part

  JsMsgPart(JMFNativePart part) {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "<init>", part);
    jmfPart = part;
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "<init>");
  }

  /*
   * Getter and Setter methods for the data fields within this message part.
   * These methods will propagate any underlying JMFExceptions
   */
  void setFieldEx(int accessor, Object value) throws JMFException {
    jmfPart.setValue(accessor, value);
  }

  Object getFieldEx(int accessor) throws JMFException {
    Object result = null;
    if (jmfPart.isPresent(accessor))
      result = jmfPart.getValue(accessor);
    return result;
  }

  /*
   * Getter and Setter methods for the data fields within this message part.
   * These methods will swallow and log any JMF exceptions.
   */

  // General field access
  void setField(int accessor, Object value) {
    try {
      jmfPart.setValue(accessor, value);
    } catch (JMFException e) {
      FFDCFilter.processException(e, "setField", "87", this);
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "setField failed: " + e);
    }
  }

  Object getField(int accessor) {
    Object result = null;
    try {
      if (jmfPart.isPresent(accessor))
        result = jmfPart.getValue(accessor);
    } catch (JMFException e) {
      FFDCFilter.processException(e, "getField", "98", this);
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "getField failed: " + e);
    }
    return result;
  }

  // Convenience access to primitive type fields
  void setIntField(int accessor, int value) {
    try {
      jmfPart.setInt(accessor, value);
    } catch (JMFException e) {
      FFDCFilter.processException(e, "setIntField", "109", this);
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "setIntField failed: " + e);
    }
  }

  int getIntField(int accessor) {
    int result = 0;
    try {
      if (jmfPart.isPresent(accessor))
        result = jmfPart.getInt(accessor);
    } catch (JMFException e) {
      FFDCFilter.processException(e, "getIntField", "120", this);
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "getIntField failed: " + e);
    }
    return result;
  }

  void setLongField(int accessor, long value) {
    try {
      jmfPart.setLong(accessor, value);
    } catch (JMFException e) {
      FFDCFilter.processException(e, "setLongField", "130", this);
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "setLongField failed: " + e);
    }
  }

  long getLongField(int accessor) {
    long result = 0;
    try {
      if (jmfPart.isPresent(accessor))
        result = jmfPart.getLong(accessor);
    } catch (JMFException e) {
      FFDCFilter.processException(e, "getLongField", "141", this);
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "getLongField failed: " + e);
    }
    return result;
  }

  void setBooleanField(int accessor, boolean value) {
    try {
      jmfPart.setBoolean(accessor, value);
    } catch (JMFException e) {
      FFDCFilter.processException(e, "setBooleanField", "151", this);
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "setBooleanField failed: " + e);
    }
  }

  boolean getBooleanField(int accessor) {
    boolean result = false;
    try {
      if (jmfPart.isPresent(accessor))
        result = jmfPart.getBoolean(accessor);
    } catch (JMFException e) {
      FFDCFilter.processException(e, "getBooleanField", "162", this);
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "getBooleanField failed: " + e);
    }
    return result;
  }

  // Set a choice field to a specific variant
  void setChoiceField(int accessor, int variant) {
    try {
      jmfPart.setInt(accessor, variant);
    } catch (JMFException e) {
      FFDCFilter.processException(e, "setChoiceField", "173", this);
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "setChoiceField failed: " + e);
    }
  }

  // Get a choice field which determines which variant is set
  int getChoiceField(int accessor) {
    int result = 0;
    try {
      if (jmfPart.isPresent(accessor))
        result = jmfPart.getInt(accessor);
    } catch (JMFException e) {
      FFDCFilter.processException(e, "getChoiceField", "185", this);
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "getChoiceField failed: " + e);
    }
    return result;
  }

  // Set a dynamic field to an empty message of appropriate type
  void setPart(int accessor, JMFSchema schema) {
    try {
      jmfPart.setValue(accessor, jmfPart.newNativePart(schema));
    } catch (JMFException e) {
      FFDCFilter.processException(e, "setDynamicField", "196", this);
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "setDynamicField failed: " + e);
    }
  }

  // Obtain a nested message part
  JsMsgPart getPart(int accessor, JMFSchema schema) {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getPart", new Object[]{accessor, schema});
    JsMsgPart result = null;
    try {
      if (jmfPart.isPresent(accessor))
        result = new JsMsgPart(jmfPart.getNativePart(accessor, schema));
    } catch (JMFException e) {
      FFDCFilter.processException(e, "getPart", "208", this);
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "getPart failed: " + e);
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getPart",  result);
    return result;
  }

  // Get the estimated size of the fluffed=up value of the field
  int estimateFieldValueSize(int accessor) {
    int size = 0;
    try {
      if (jmfPart.isPresent(accessor)) {
        size = jmfPart.estimateUnassembledValueSize(accessor);
      }
    }
    catch (JMFException e) {
      FFDCFilter.processException(e, "estimateFieldValueSize", "221", this);
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "estimateFieldValueSize failed: " + e);
    }
    return size;
  }


  /**
   * getAssembledLengthIfKnown
   * Return the flattened length of the JMF Message contained by this part.
   * The originalFrame() method of JMFMessageData returns the assembled length of the
   * JMF message if it is known, i.e. if it arrived assembled and has not been updated
   * in such away to make the length invalid.
   * This length will include the length of any sub-messages.
   *
   * @return int The flattened size of the message, or -1 if it is not known.
   */
  int getAssembledLengthIfKnown() {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getAssembledLengthIfKnown");
    int flatSize = -1;
    if (jmfPart instanceof JMFMessage) {
      flatSize = ((JMFMessage)jmfPart).originalFrame();
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getAssembledLengthIfKnown",  flatSize);
    return flatSize;
  }


  /**
   * isNotEMPTYlist
   * Return false if the value of the given field is one of the singleton
   * EMPTY lists, otherwise true.
   *
   * @param accessor   The aceesor value of the field of interest.
   *
   * @return boolean True if the field is NOT a singleton JMF empty list
   */
  public boolean isNotEMPTYlist(int accessor) {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "isNotEMPTYlist", accessor);
    // The answer is true, unless an EMPTY is found.
    boolean isNotAnEMPTY = true;

    // The field can't be an EMPTY singleton unless the part is a JMFMessageData
    // as it is impossible for Encapsulations to hold an EMPTY.
    // Note the JMF call is the reverse - i.e. isEMPTY rather than isNot
    if (jmfPart instanceof JMFMessage) {
      isNotAnEMPTY = !((JMFMessage)jmfPart).isEMPTYlist(accessor);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "isNotEMPTYlist",  isNotAnEMPTY);
    return isNotAnEMPTY;
  }
}
