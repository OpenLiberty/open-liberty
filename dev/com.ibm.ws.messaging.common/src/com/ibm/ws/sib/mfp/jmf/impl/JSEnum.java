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

package com.ibm.ws.sib.mfp.jmf.impl;

import java.util.Set;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.mfp.jmf.JMFEnumType;
import com.ibm.ws.sib.mfp.jmf.JMFMessageData;
import com.ibm.ws.sib.mfp.jmf.JMFMessageCorruptionException;
import com.ibm.ws.sib.mfp.jmf.JMFModelNotImplementedException;
import com.ibm.ws.sib.mfp.jmf.JMFSchemaViolationException;
import com.ibm.ws.sib.mfp.jmf.JMFUninitializedAccessException;
import com.ibm.ws.sib.mfp.jmf.JmfConstants;
import com.ibm.ws.sib.mfp.jmf.JmfTr;

/**
 * A representation of enumerated types.  For use in tools and API mapping layers, the
 * JSEnum can record its associated enumerators as Strings, but most of JMF ignores these
 * and they are not propagated in schema propagations.  Rather, a JSEnum is always encoded
 * as a non-negative integer less than #enumerators.
 */

public final class JSEnum extends JSField implements JMFEnumType {
  private static TraceComponent tc = JmfTr.register(JSEnum.class, JmfConstants.MSG_GROUP, JmfConstants.MSG_BUNDLE);

  // Enumerators for this JSEnum.  This field is optional and not propagated with the
  // schema.
  private String[] enumerators;

  // #Enumerators for this JSEnum.  This field is set to enumerators.length if enumerators
  // is set, but can be set without setting enumerators.
  private int enumeratorCount;

  /**
   * Default constructor
   */
  public JSEnum() {
  }

  /**
   * Provide the enumerators in the order of their assigned codes or null if the
   * enumerators are not locally known (never set, or not propagated).
   */
  public String[] getEnumerators() {
    return enumerators;
  }

  /**
   * Set the enumerators in the order of their assigned codes
   */
  public void setEnumerators(String[] val) {
    enumerators = val;
    enumeratorCount = (enumerators != null) ? enumerators.length : 0;
  }

  /**
   * Get the enumerator count
   */
  public int getEnumeratorCount() {
    return enumeratorCount;
  }

  /**
   * Set the enumerator count without setting the enumerators explicitly.  Sets
   * enumerators to null as a side-effect.
   */
  public void setEnumeratorCount(int count) {
    if (count < 0)
      throw new IllegalArgumentException("Enumerator count cannot be negative");
    enumeratorCount = count;
    enumerators = null;
  }

  // Implementations of the JSCoder interface.  This just delegates to the int coder.
  public int getEncodedLength(Object val, int indirect, JMFMessageData msg)
      throws JMFUninitializedAccessException, JMFSchemaViolationException, JMFModelNotImplementedException, JMFMessageCorruptionException {
    return JSCoder.INT.getEncodedLength(val, 0, msg);
  }

  public Object validate(Object val, int indirect)
      throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFUninitializedAccessException, JMFMessageCorruptionException {
    return JSCoder.INT.validate(val, 0);
  }

  public int encode(byte[] frame, int offset, Object val, int indirect, JMFMessageData msg)
      throws JMFUninitializedAccessException, JMFSchemaViolationException, JMFModelNotImplementedException, JMFMessageCorruptionException {
    return JSCoder.INT.encode(frame, offset, val, 0, msg);
  }

  public Object decode(byte[] frame, int offset, int indirect, JMFMessageData msg)
      throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFMessageCorruptionException {
    return JSCoder.INT.decode(frame, offset, 0, msg);
  }

  public Object copy(Object val, int indirect)
      throws JMFSchemaViolationException {
    return JSCoder.INT.copy(val, 0);
  }

  public int estimateUnassembledSize(Object val) {
    return JSCoder.INT.estimateUnassembledSize(val);
  }

  public int estimateUnassembledSize(byte[] frame, int offset) {
    return JSCoder.INT.estimateUnassembledSize(frame, offset);
  }

  // Constructor from byte array form
  JSEnum(byte[] frame, int[] limits) {
    enumeratorCount = getCount(frame, limits);
  }

  // Implement encodedTypeLength
  public int encodedTypeLength() {
    return 3;
  }

  // Implementation encodeType: note a JSEnum does not propagate its enumerator list,
  // since that information is not essential to processing the message.  But, it does
  // propagate the enumeratorCount.
  public void encodeType(byte[] frame, int[] limits) {
    setByte(frame, limits, (byte)ENUM);
    setCount(frame, limits, enumeratorCount);
  }

  // Format for printing.
  public void format(StringBuffer fmt, Set done, Set todo, int indent) {
    formatName(fmt, indent);
    fmt.append("Enum");
    if (enumerators != null) {
      fmt.append("{{");
      String delim = "";
      for (int i = 0; i < enumerators.length; i++) {
        fmt.append(delim).append(enumerators[i]);
        delim = ",";
      }
      fmt.append("}}");
    }
  }
}
