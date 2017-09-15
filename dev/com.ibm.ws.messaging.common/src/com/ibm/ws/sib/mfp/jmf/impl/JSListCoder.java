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

import java.util.Collection;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.mfp.jmf.JMFMessageData;
import com.ibm.ws.sib.mfp.jmf.JMFMessageCorruptionException;
import com.ibm.ws.sib.mfp.jmf.JMFModelNotImplementedException;
import com.ibm.ws.sib.mfp.jmf.JMFPrimitiveType;
import com.ibm.ws.sib.mfp.jmf.JMFSchemaViolationException;
import com.ibm.ws.sib.mfp.jmf.JMFUninitializedAccessException;
import com.ibm.ws.sib.mfp.jmf.JmfConstants;
import com.ibm.ws.sib.mfp.jmf.JmfTr;
import com.ibm.ws.sib.mfp.MfpConstants;
import com.ibm.ws.sib.mfp.util.HexUtil;

/**
 * The JSListCoder is a special JSCoder that is used whenever the normal (scalar) coder for
 * a JSField must be overridden because the field is dominated by a JSRepeated in the schema.
 */

public final class JSListCoder implements JSCoder {
  private static TraceComponent tc = JmfTr.register(JSListCoder.class, JmfConstants.MSG_GROUP, JmfConstants.MSG_BUNDLE);

  // Store number of indirections from this type to its element type. So, if this type is
  // "list of JSPrimitive", indirect would be zero.  If this type is "list of list of
  // JSPrimitive", indirect would be one, etc.  This field is use when an indirect
  // argument of -1 is passed in (meaning the container isn't a list and so the schema
  // should tell us statically how many levels of indirection there are).  An indirect
  // argument passed to any of the methods overrides this field (meaning that the
  // container is a list that is already taking care of some of the levels of indirection
  // and so we should ignore the schema and do what the caller tells us to do).
  int indirect;

  // Store the ultimate element type
  private JSField element;

  // Flag indicating whether the element type implies varying length list elements.  This
  // is computable from 'element' but this is done once in the constructor for efficiency.
  private boolean varying;

  // Make a new JSListCoder
  JSListCoder(int indirect, JSField element) {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.entry(this, tc, "<init>", new Object[]{indirect, element});
    this.indirect = indirect;
    this.element = element;
    if (element instanceof JSEnum)
      varying = false;
    else if (element instanceof JSPrimitive)
      varying = ((JSPrimitive)element).getLength() == -1;
    else
      varying = true;
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.exit(this, tc, "<init>", varying);
  }

  // Implement JSCoder.getEncodedLength
  public int getEncodedLength(Object val, int indirect, JMFMessageData msg)
      throws JMFUninitializedAccessException, JMFSchemaViolationException, JMFModelNotImplementedException, JMFMessageCorruptionException {
    return ((JSListImpl)val).getEncodedLength();
  }

  // Implement JSCoder.encode
  public int encode(byte[] frame, int offset, Object val, int indirect, JMFMessageData msg)
      throws JMFUninitializedAccessException, JMFSchemaViolationException, JMFModelNotImplementedException, JMFMessageCorruptionException {
    return ((JSListImpl)val).encode(frame, offset);
  }

  // Implement JSCoder.decode
  public Object decode(byte[] frame, int offset, int indirect, JMFMessageData msg) throws JMFMessageCorruptionException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.entry(this, tc, "decode", new Object[]{frame, offset, indirect, msg});
    Object result = null;
    if (indirect < 0) {
      indirect = this.indirect;
    }
    if (indirect > 0) {
      result = new JSVaryingListImpl(frame, offset, element, indirect);
    }
    else if (varying) {
      result = new JSVaryingListImpl(frame, offset, element, 0);
    }
    else {
      result = new JSFixedListImpl(frame, offset, element);
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.exit(this, tc, "decode",  result);
    return result;
  }

  // Implement JSCoder.validate
  public Object validate(Object value, int indirect) throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFUninitializedAccessException, JMFMessageCorruptionException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.entry(this, tc, "validate", new Object[]{value, indirect});
    Object result = null;

    if (indirect < 0) {
      indirect = this.indirect;
    }
    // If it already is a JMF List, just check its type and use it
    if (value instanceof JSListImpl) {
      ((JSListImpl)value).checkType(element, indirect);
      result = value;
    }
    // Otherwise, we need to create/find a JMF List to use
    else if (  (value == null)
            || (value instanceof Collection || value.getClass().isArray())
            ) {
      // If it is boxed, we always need a new JSVaryingListImpl as it needs state
      if (indirect > 0) {
        result = new JSVaryingListImpl(element, indirect, value);
      }
      // If varying==true, we need a JSVaryingListImpl
      else if (varying) {
        // If we've been passed an empty Collection, just use the appropriate singleton 'EMPTY' for now
        if (  (value == null)
           || ((value instanceof Collection) && (((Collection)value).size() == 0))
           ) {
          result = JSVaryingListImpl.EMPTY_UNBOXED_VARYINGLIST;
        }
        else {
          result = new JSVaryingListImpl(element, 0, value);
        }
      }
      // Otherwise we need a JSFixedListImpl
      else {
        // If we've been passed an empty Collection, just use the appropriate singleton 'EMPTY' for now
        if (  (value == null)
           || (value instanceof Collection) && (((Collection)value).size() == 0)
           ) {
          result = JSFixedListImpl.EMPTY_FIXEDLIST;
        }
        else {
          result = new JSFixedListImpl(element, value);
        }
      }
    }
    else {
      throw new JMFSchemaViolationException(value.getClass().getName());
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.exit(this, tc, "validate",  result);
    return result;
  }

  // Implement JSCoder.copy
  public Object copy(Object val, int indirect) throws JMFSchemaViolationException {
    return ((JSListImpl)val).getCopy();
  }

  // Implement JSCoder.estimatedUnassembledSize(Object.....
  public int estimateUnassembledSize(Object val) {
    // We'll treat it is a simple list, as we that's all we've used so far.
    return JSBaseTypes.baseTypes[JMFPrimitiveType.SIMPLELIST].coder.estimateUnassembledSize(val);
  }

  // Implement JSCoder.estimatedUnassembledSize(byte[].....
    public int estimateUnassembledSize(byte[] frame, int offset) {
    // We'll treat it is a simple list, as we that's all we've used so far.
    return JSBaseTypes.baseTypes[JMFPrimitiveType.SIMPLELIST].coder.estimateUnassembledSize(frame, offset);
  }

  /**
   * A method to sanity check a 32 bit length value read from the message prior to allocating
   * storage based on the value.  The length is not allowed to be negative or to exceed
   * the length of the remaining portion of the message's frame, assuming that the length
   * field is immediately followed by the data whose length it is.
   *
   * @param length the length value just read from the message
   * @param frame the message's frame
   * @param offset the offset in the frame at which the length value was read.  This is
   * assumed to be four bytes long and immediately followed by the data whose length is
   * supposedly given by the length parameter.
   * @exception MessageCorruptionException if the sanity check fails
   */
  public static void sanityCheck(int length, byte[] frame, int offset) throws JMFMessageCorruptionException {
    if (length < 0 || offset + 4 + length > frame.length) {
      JMFMessageCorruptionException jmce =  new JMFMessageCorruptionException(
          "Bad length: " + HexUtil.toString(new int[] { length }) + " at offset " + offset);
      FFDCFilter.processException(jmce, "com.ibm.ws.sib.mfp.jmf.impl.JSListCoder.sanityCheck", "160", Integer.valueOf(length),
          new Object[] { MfpConstants.DM_BUFFER, frame, Integer.valueOf(0), Integer.valueOf(frame.length) });
      throw jmce;
    }
  }
}
