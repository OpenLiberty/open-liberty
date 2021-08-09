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

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.mfp.jmf.JMFFieldDef;
import com.ibm.ws.sib.mfp.jmf.JMFMessageData;
import com.ibm.ws.sib.mfp.jmf.JMFMessageCorruptionException;
import com.ibm.ws.sib.mfp.jmf.JMFModelNotImplementedException;
import com.ibm.ws.sib.mfp.jmf.JMFSchema;
import com.ibm.ws.sib.mfp.jmf.JMFSchemaViolationException;
import com.ibm.ws.sib.mfp.jmf.JMFUninitializedAccessException;
import com.ibm.ws.sib.mfp.jmf.JmfConstants;
import com.ibm.ws.sib.mfp.jmf.JmfTr;

/**
 * The JSField class is an abstract parent for those nodes that have accessors assigned
 * to them, namely, JSPrimitive, JSEnum, JSDynamic, and JSVariant.
 */

public abstract class JSField extends JSType implements JMFFieldDef, JSCoder {
  private static TraceComponent tc = JmfTr.register(JSField.class, JmfConstants.MSG_GROUP, JmfConstants.MSG_BUNDLE);

  // The field's accessors (as a linked list).  This does not have a meaningful value
  // until a complete schema has been constructed.
  private Accessor accessor;

  // The JSCoder for use by this JSField.  For compactness, all JSField classes implement
  // JSCoder directly and set coder to this by default.  This setting is overriden in the
  // JSchema constructor to insert special coders for lists.

  private JSCoder coder;
  // Constructor: called by super() by subclasses

  protected JSField() {
    coder = this;
  }

  // Implement the schema-less getAccessor method
  public int getAccessor() {
    if (accessor == null)
      return -1;  // not yet part of a schema
    return accessor.accessor;
  }

  // Implement the general getAccessor method
  public int getAccessor(JMFSchema schema) {
    for (Accessor acc = accessor; acc != null; acc = acc.next)
      if (schema == acc.schema)
        return acc.accessor;
    return -1;
  }

  /**
   * Retrieve the coder.  This is useful when the default JSCoder is overridden and the
   * overriding JSCoder has behavior in addition to the standard JSCoder methods that the
   * caller wishes to exploit.
   */
  public JSCoder getCoder() {
    return coder;
  }

  /**
   * Retrieve the indirection count.  This is -1 if this JSField is not repeated by
   * virtue of having a JSRepeated ancestor younger than the youngest JSVariant ancestor.
   * Otherwise, 0 if it is the immediate member type of a list, 1 if it is in a list of
   * lists, etc.  This information is not available (method always returns -1) until after
   * the JSType tree has been installed in a JSchema (it is the JSchema constructor that
   * computes this information).
   */
  public int getIndirection() {
    if (coder instanceof JSListCoder)
      return ((JSListCoder)coder).indirect;
    else
      return -1;
  }

  /**
   * Set the accessor
   * @param accessor the value to set for the accessor
   * @param schema the schema for which this accessor is an accessor.  A field can
   *   have accessors for each of its enclosing box schemas as well as its outermost one.
   */
  void setAccessor(int accessor, JMFSchema schema) {
    this.accessor = new Accessor(accessor, schema, this.accessor);
  }

  // Set the coder
  void setCoder(JSCoder coder) {
    this.coder = coder;
  }

  /**
   * Get the number of bytes needed to encode a value of this type
   *
   * @param val the value whose encoded length in bytes is needed
   * @param indirect the list indirection that applies to the object or -1 if the
   * JSField's maximum list indirection (based on the number of JSRepeated nodes that
   * dominate it in the schema) is to be used: this, of course, includes the possibility
   * that it isn't a list at all.
   * @param msg the JMF Message (the top-level message, not the immediate JMFMessageData)
   * under which the decoding is occuring.
   * @return the number of bytes it will take to encode val
   */
  public int getEncodedValueLength(Object val, int indirect, JMFMessageData msg)
      throws JMFUninitializedAccessException, JMFSchemaViolationException, JMFModelNotImplementedException, JMFMessageCorruptionException {
    if (indirect == 0)
      return getEncodedLength(val, 0, msg);
    else
      return coder.getEncodedLength(val, indirect - 1, msg);
  }

  /**
   * Validate and possibly convert a value of this JSField's type to be acceptable to the
   * encodeValue method.
   *
   * @param val the value to validate
   * @param indirect the list indirection that applies to the object or -1 if the
   * JSField's maximum list indirection (based on the number of JSRepeated nodes that
   * dominate it in the schema) is to be used: this, of course, includes the possibility
   * that it isn't a list at all.
   * @return val or a conversion of val if val is valid
   * @exception SchemaViolationException if val is not valid
   */
  public Object validateValue(Object val, int indirect)
      throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFUninitializedAccessException, JMFMessageCorruptionException {
    if (indirect == 0)
      return validate(val, 0);
    else
      return coder.validate(val, indirect - 1);
  }

  /**
   * Encode a value of this JSField's type into a byte array (implemented by subclasses)
   *
   * @param frame the byte array into which the value is to be encoded
   * @param offset the position in frame where the value is to be encoded
   * @param val the value to be encoded
   * @param indirect the list indirection that applies to the object or -1 if the
   * JSField's maximum list indirection (based on the number of JSRepeated nodes that
   * dominate it in the schema) is to be used: this, of course, includes the possibility
   * that it isn't a list at all.
   * @param msg the JMFMessage (the top-level message, not the immediate JMFMessageData)
   * under which the decoding is occuring.
   * @return the index of the byte immediately following the last byte of the value
   */
  public int encodeValue(byte[] frame, int offset, Object val, int indirect, JMFMessageData msg)
      throws JMFUninitializedAccessException, JMFSchemaViolationException, JMFModelNotImplementedException, JMFMessageCorruptionException {
    if (indirect == 0)
      return encode(frame, offset, val, 0, msg);
    else
      return coder.encode(frame, offset, val, indirect - 1, msg);
  }

  /**
   * Decode a value of this JSField's type from a byte array (implemented by subclasses)
   *
   * @param frame the byte array from which the value is to be decoded
   * @param offset the position in frame where the value's bytes are to be found
   * @param indirect the list indirection that applies to the object or -1 if the
   * JSField's maximum list indirection (based on the number of JSRepeated nodes that
   * dominate it in the schema) is to be used: this, of course, includes the possibility
   * that it isn't a list at all.
   * @param msg the JMF Message (the top-level message, not the immediate JMFMessageData)
   * under which the decoding is occuring.
   * @return the decoded value
   */
  public Object decodeValue(byte[] frame, int offset, int indirect, JMFMessageData msg)
      throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFMessageCorruptionException {
    if (indirect == 0)
      return decode(frame, offset, 0, msg);
    else
      return coder.decode(frame, offset, indirect - 1, msg);
  }


  /**
   * estimateSizeOfUnassembledValue
   * Return the estimated size of the value if unassembled.
   * This size includes a guess at the heap overhead of the object(s) which
   * would be created.
   *
   * @param val the object whose unassembled length is desired
   * @param indirect the list indirection that applies to the object or -1 if the
   * JSField's maximum list indirection (based on the number of JSRepeated nodes that
   * dominate it in the schema) is to be used: this, of course, includes the possibility
   * that it isn't a list at all.
   *
   * @return int the estimated size of the unassembled object in the heap.
   */
  public int estimateSizeOfUnassembledValue(Object val, int indirect) {
    if (indirect == 0)
      return estimateUnassembledSize(val);
    else
      return coder.estimateUnassembledSize(val);
  }

  /**
   * estimateSizeOfUnassembledValue
   * Return the estimated size of the value if unassembled.
   * This size includes a guess at the heap overhead of the object(s) which
   * would be created.
   *
   * @param frame the byte array from which the object would be deserialized.
   * @param offset the byte at which to start in frame
   * @param indirect the list indirection that applies to the object or -1 if the
   * JSField's maximum list indirection (based on the number of JSRepeated nodes that
   * dominate it in the schema) is to be used: this, of course, includes the possibility
   * that it isn't a list at all.
   *
   * @return int the estimated size of the unassembled object in the heap.
   */
  public int estimateSizeOfUnassembledValue(byte[] frame, int offset, int indirect) {
    if (indirect == 0)
      return estimateUnassembledSize(frame, offset);
    else
      return coder.estimateUnassembledSize(frame, offset);
  }

  /**
   * Create a copy of the value of this JSField's type
   *
   * @param val the value to be copied
   * @param indirect the list indirection that applies to the object or -1 if the
   * JSField's maximum list indirection (based on the number of JSRepeated nodes that
   * dominate it in the schema) is to be used: this, of course, includes the possibility
   * that it isn't a list at all.
   * @return a copy of val.  This can be the original if the type for this field is immutable.
   */
  public Object copyValue(Object val, int indirect)
      throws JMFSchemaViolationException {
    if (indirect == 0)
      return copy(val, 0);
    else
      return coder.copy(val, indirect - 1);
  }

  /**
   * A utility class used to store an accessor integer / JMFSchema pair.  These pairs
   * can be chained into a linked list so that a field may have many accessors relative
   * to its enclosing JMFSchemas.  All accessors other than the one at the head of the
   * list are relative to the JMFSchemas of variant boxes, which are not normally
   * visible but can be made visible for added performance. */
  static class Accessor {
    int accessor;
    JMFSchema schema;
    Accessor next;
    Accessor(int accessor, JMFSchema schema, Accessor next) {
      this.accessor = accessor;
      this.schema = schema;
      this.next = next;
    }
  }
}
