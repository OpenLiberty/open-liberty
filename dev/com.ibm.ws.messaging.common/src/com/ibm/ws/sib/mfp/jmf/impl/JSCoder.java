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

import com.ibm.ws.sib.mfp.jmf.JMFMessageData;
import com.ibm.ws.sib.mfp.jmf.JMFMessageCorruptionException;
import com.ibm.ws.sib.mfp.jmf.JMFModelNotImplementedException;
import com.ibm.ws.sib.mfp.jmf.JMFSchemaViolationException;
import com.ibm.ws.sib.mfp.jmf.JMFUninitializedAccessException;
import com.ibm.ws.sib.mfp.util.ArrayUtil;

//import com.ibm.ws.util.PlatformHelperFactory;
//Venu Liberty change: Moved the in64bit() to SIB Utils
import com.ibm.ws.sib.utils.RuntimeInfo;

/**
 * A JSCoder is a serializer and deserializer for values of a particular kind.
 *
 * <p>The JSCoders for the eight Java primitive types are constrained to implement the
 * encoding conventions of java.io.DataOutput because fastpath code elsewhere in Jetstream
 * assumes this.  Other JSCoders are freer in their choices.  However, the current
 * encoding "rules" require that all variable length encodings <em>must</em> begin with a
 * four byte bigendian length which accurately records the length of the remainder of the
 * field, and that null values of those encodings are recorded by recording a length of -1
 * and nothing else.
 */

public interface JSCoder {

  // The overhead for a single object in the heap. The value will depend on whether we
  // are running on 32-bit or 64-bit.
  final static int OBJECT_OVERHEAD = RuntimeInfo.is64bit()? 24 : 12;

  /**
   * Get the number of bytes it will take to serialize an object of this kind.
   *
   * @param val the object whose encoded length is desired
   * @param indirect the list indirection that applies to the object or -1 if the
   * JSField's maximum list indirection (based on the number of JSRepeated nodes that
   * dominate it in the schema) is to be used: this, of course, includes the possibility
   * that it isn't a list at all.
   * @param msg the JMFMessageData (the top-level message, not the immediate JMFMessageData)
   * under which the decoding is occuring.
   * @return the length
   */
  public int getEncodedLength(Object val, int indirect, JMFMessageData msg)
    throws JMFUninitializedAccessException, JMFSchemaViolationException, JMFModelNotImplementedException, JMFMessageCorruptionException;

  /**
   * Serialize object.  The object has been previously validated and the frame is large
   * enough
   *
   * @param frame the byte array into which the object is to be serialized
   * @param offset the starting byte
   * @param val the value to serialize
   * @param indirect the list indirection that applies to the object or -1 if the
   * JSField's maximum list indirection (based on the number of JSRepeated nodes that
   * dominate it in the schema) is to be used: this, of course, includes the possibility
   * that it isn't a list at all.
   * @param msg the JMFMessageData (the top-level message, not the immediate JMFMessageData)
   * under which the decoding is occuring.
   * @return the next available byte in frame or the length of frame if all bytes are used
   * @exception NullPointerException if val is null and this type of field can't accept
   * nulls.
   */
  public int encode(byte[] frame, int offset, Object val, int indirect, JMFMessageData msg)
    throws JMFUninitializedAccessException, JMFSchemaViolationException, JMFModelNotImplementedException, JMFMessageCorruptionException;

  /**
   * Deserialize the object (uses the internal object types for numeric and boolean).
   *
   * @param frame the byte array from which the object is to be deserialized.
   * @param offset the byte at which to start in frame
   * @param indirect the list indirection that applies to the object or -1 if the
   * JSField's maximum list indirection (based on the number of JSRepeated nodes that
   * dominate it in the schema) is to be used: this, of course, includes the possibility
   * that it isn't a list at all.
   * @param msg the JMFMessageData (the top-level message, not the immediate JMFMessageData)
   * under which the decoding is occuring.
   * @return the deserialized object
   */
  public Object decode(byte[] frame, int offset, int indirect, JMFMessageData msg)
    throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFMessageCorruptionException;

  /**
   * Validate an object for a particular field and substitute a canonical representation
   * if necessary.
   *
   * @param val the object to validate and possibly replace with a canonical
   * representation.
   * @param indirect the list indirection that applies to the object or -1 if the
   * JSField's maximum list indirection (based on the number of JSRepeated nodes that
   * dominate it in the schema) is to be used: this, of course, includes the possibility
   * that it isn't a list at all.
   * @return the object or a canonical replacement
   * @exception SchemaViolationException if the object is invalid
   */
  public Object validate(Object val, int indirect)
    throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFUninitializedAccessException, JMFMessageCorruptionException;

  /**
   * Copy an object for a particular field.  This can return the original (rather than
   * a copy or clone) if the object to be copied is known to be immutable.
   *
   * @param val the object to be copied
   * @param indirect the list indirection that applies to the object or -1 if the
   * JSField's maximum list indirection (based on the number of JSRepeated nodes that
   * dominate it in the schema) is to be used: this, of course, includes the possibility
   * that it isn't a list at all.
   * @return the copied object
   * @exception SchemaViolationException if the object is invalid
   */
  public Object copy(Object val, int indirect)
    throws JMFSchemaViolationException;


  /**
   * estimateUnassembledSize
   * Return the estimated size of the value if unassembled.
   * This size includes a guess at the heap overhead of the object(s) which
   * would be created.
   *
   * @param val the object whose unassembled length is desired
   *
   * @return int the estimated size of the unassembled object in the heap.
   */
  public int estimateUnassembledSize(Object val);

  /**
   * estimateUnassembledSize
   * Return the estimated size of the value if unassembled.
   * This size includes a guess at the heap overhead of the object(s) which
   * would be created.
   *
   * @param frame the byte array from which the object would be deserialized.
   * @param offset the byte at which to start in frame
   *
   * @return int the estimates size of the unassembled object in the heap.
   */
  public int estimateUnassembledSize(byte[] frame, int offset);


  // Provide the well-known JSCoders for the primitive types.  If you're wondering why
  // these are not anonymous classes, it's because javadoc doesn't like it done that way.
  // Both javac and jikes are fine with it.  Go figure.

  public JSCoder BOOLEAN = new BooleanJSCoder();

  class BooleanJSCoder implements JSCoder {
    public int getEncodedLength(Object val, int indirect, JMFMessageData msg) {
      return ArrayUtil.BYTE_SIZE;
    }
    public Object validate(Object val, int indirect) throws JMFSchemaViolationException {
      if (val instanceof Boolean)
        return val;
      else if (val == null)
        throw new JMFSchemaViolationException("boolean==null");
      else
        throw new JMFSchemaViolationException(val.getClass().getName());
    }
    public int encode(byte[] frame, int offset, Object val, int indirect, JMFMessageData msg) {
      if (val == null)
        throw new NullPointerException();
      frame[offset] = (byte) (((Boolean)val).booleanValue() ? 1 : 0);
      return offset + ArrayUtil.BYTE_SIZE;
    }
    public Object decode(byte[] frame, int offset, int indirect, JMFMessageData msg) {
      return Boolean.valueOf(frame[offset] != 0);
    }
    public Object copy(Object val, int indirect) {
      return val;
    }
    // An unassembled Boolean value takes no space as decode returns an existing
    // Boolean object.
    public int estimateUnassembledSize(Object val) {
      return 0;
    }
    public int estimateUnassembledSize(byte[] frame, int offset) {
      return 0;
    }
  }

  public JSCoder BYTE = new ByteJSCoder();

  class ByteJSCoder implements JSCoder {
    public int getEncodedLength(Object val, int indirect, JMFMessageData msg) {
      return ArrayUtil.BYTE_SIZE;
    }
    public Object validate(Object val, int indirect) throws JMFSchemaViolationException {
      if (val instanceof Byte)
        return val;
      else if (val == null)
        throw new JMFSchemaViolationException("byte==null");
      else
        throw new JMFSchemaViolationException(val.getClass().getName());
    }
    public int encode(byte[] frame, int offset, Object val, int indirect, JMFMessageData msg) {
      if (val == null)
        throw new NullPointerException();
      frame[offset] = ((Number)val).byteValue();
      return offset + ArrayUtil.BYTE_SIZE;
    }
    public Object decode(byte[] frame, int offset, int indirect, JMFMessageData msg) {
      return Byte.valueOf(frame[offset]);
    }
    public Object copy(Object val, int indirect) {
      return val;
    }
    // An unassembled Byte value takes no space as decode returns an existing Byte object.
    public int estimateUnassembledSize(Object val) {
      return 0;
    }
    public int estimateUnassembledSize(byte[] frame, int offset) {
      return 0;
    }
  }

  public JSCoder SHORT = new ShortJSCoder();

  class ShortJSCoder implements JSCoder {
    public int getEncodedLength(Object val, int indirect, JMFMessageData msg) {
      return ArrayUtil.SHORT_SIZE;
    }
    public Object validate(Object val, int indirect) throws JMFSchemaViolationException {
      if (val instanceof Short)
        return val;
      else if (val == null)
        throw new JMFSchemaViolationException("short==null");
      else
        throw new JMFSchemaViolationException(val.getClass().getName());
    }
    public int encode(byte[] frame, int offset, Object val, int indirect, JMFMessageData msg) {
      if (val == null)
        throw new NullPointerException();
      ArrayUtil.writeShort(frame, offset, ((Number)val).shortValue());
      return offset + ArrayUtil.SHORT_SIZE;
    }
    public Object decode(byte[] frame, int offset, int indirect, JMFMessageData msg) {
      return Short.valueOf(ArrayUtil.readShort(frame, offset));
    }
    public Object copy(Object val, int indirect) {
      return val;
    }
    public int estimateUnassembledSize(Object val) {
      return OBJECT_OVERHEAD + ArrayUtil.SHORT_SIZE;
    }
    public int estimateUnassembledSize(byte[] frame, int offset) {
      return OBJECT_OVERHEAD + ArrayUtil.SHORT_SIZE;
    }
  }

  public JSCoder CHAR = new CharJSCoder();

  class CharJSCoder implements JSCoder {
    public int getEncodedLength(Object val, int indirect, JMFMessageData msg) {
      return ArrayUtil.SHORT_SIZE;
    }
    public Object validate(Object val, int indirect) throws JMFSchemaViolationException {
      if (val instanceof Character)
        return val;
      else if (val == null)
        throw new JMFSchemaViolationException("char==null");
      else
        throw new JMFSchemaViolationException(val.getClass().getName());
    }
    public int encode(byte[] frame, int offset, Object val, int indirect, JMFMessageData msg) {
      if (val == null)
        throw new NullPointerException();
      ArrayUtil.writeShort(frame, offset, (short) ((Character)val).charValue());
      return offset + ArrayUtil.SHORT_SIZE;
    }
    public Object decode(byte[] frame, int offset, int indirect, JMFMessageData msg) {
      return Character.valueOf((char)ArrayUtil.readShort(frame, offset));
    }
    public Object copy(Object val, int indirect) {
      return val;
    }
    public int estimateUnassembledSize(Object val) {
      return OBJECT_OVERHEAD + ArrayUtil.SHORT_SIZE;
    }
    public int estimateUnassembledSize(byte[] frame, int offset) {
      return OBJECT_OVERHEAD + ArrayUtil.SHORT_SIZE;
    }
  }

  public JSCoder INT = new IntJSCoder();

  class IntJSCoder implements JSCoder {
    public int getEncodedLength(Object val, int indirect, JMFMessageData msg) {
      return ArrayUtil.INT_SIZE;
    }
    public Object validate(Object val, int indirect) throws JMFSchemaViolationException {
      if (val instanceof Integer)
        return val;
      else if (val == null)
        throw new JMFSchemaViolationException("int==null");
      else
        throw new JMFSchemaViolationException(val.getClass().getName());
    }
    public int encode(byte[] frame, int offset, Object val, int indirect, JMFMessageData msg) {
      if (val == null)
        throw new NullPointerException();
      ArrayUtil.writeInt(frame, offset, ((Number)val).intValue());
      return offset + ArrayUtil.INT_SIZE;
    }
    public Object decode(byte[] frame, int offset, int indirect, JMFMessageData msg) {
      return Integer.valueOf(ArrayUtil.readInt(frame, offset));
    }
    public Object copy(Object val, int indirect) {
      return val;
    }
    public int estimateUnassembledSize(Object val) {
      return OBJECT_OVERHEAD + ArrayUtil.INT_SIZE;
    }
    public int estimateUnassembledSize(byte[] frame, int offset) {
      return OBJECT_OVERHEAD + ArrayUtil.INT_SIZE;
    }
  }

  public JSCoder LONG = new LongJSCoder();

  class LongJSCoder implements JSCoder {
    public int getEncodedLength(Object val, int indirect, JMFMessageData msg) {
      return ArrayUtil.LONG_SIZE;
    }
    public Object validate(Object val, int indirect) throws JMFSchemaViolationException {
      if (val instanceof Long)
        return val;
      else if (val == null)
        throw new JMFSchemaViolationException("long==null");
      else
        throw new JMFSchemaViolationException(val.getClass().getName());
    }
    public int encode(byte[] frame, int offset, Object val, int indirect, JMFMessageData msg) {
      if (val == null)
        throw new NullPointerException();
      ArrayUtil.writeLong(frame, offset, ((Number)val).longValue());
      return offset + ArrayUtil.LONG_SIZE;
    }
    public Object decode(byte[] frame, int offset, int indirect, JMFMessageData msg) {
      return Long.valueOf(ArrayUtil.readLong(frame, offset));
    }
    public Object copy(Object val, int indirect) {
      return val;
    }
    public int estimateUnassembledSize(Object val) {
      return OBJECT_OVERHEAD + ArrayUtil.LONG_SIZE;
    }
    public int estimateUnassembledSize(byte[] frame, int offset) {
      return OBJECT_OVERHEAD + ArrayUtil.LONG_SIZE;
    }
  }

  public JSCoder FLOAT = new FloatJSCoder();

  class FloatJSCoder implements JSCoder {
    public int getEncodedLength(Object val, int indirect, JMFMessageData msg) {
      return ArrayUtil.INT_SIZE;
    }
    public Object validate(Object val, int indirect) throws JMFSchemaViolationException {
      if (val instanceof Float)
        return val;
      else if (val == null)
        throw new JMFSchemaViolationException("float==null");
      else
        throw new JMFSchemaViolationException(val.getClass().getName());
    }
    public int encode(byte[] frame, int offset, Object val, int indirect, JMFMessageData msg) {
      if (val == null)
        throw new NullPointerException();
      ArrayUtil.writeInt(frame, offset, Float.floatToIntBits(((Number)val).floatValue()));
      return offset + ArrayUtil.INT_SIZE;
    }
    public Object decode(byte[] frame, int offset, int indirect, JMFMessageData msg) {
      return new Float(Float.intBitsToFloat(ArrayUtil.readInt(frame, offset)));
    }
    public Object copy(Object val, int indirect) {
      return val;
    }
    public int estimateUnassembledSize(Object val) {
      return OBJECT_OVERHEAD + ArrayUtil.INT_SIZE;
    }
    public int estimateUnassembledSize(byte[] frame, int offset) {
      return OBJECT_OVERHEAD + ArrayUtil.INT_SIZE;
    }
  }

  public JSCoder DOUBLE = new DoubleJSCoder();

  class DoubleJSCoder implements JSCoder {
    public int getEncodedLength(Object val, int indirect, JMFMessageData msg) {
      return ArrayUtil.LONG_SIZE;
    }
    public Object validate(Object val, int indirect) throws JMFSchemaViolationException {
      if (val instanceof Double)
        return val;
      else if (val == null)
        throw new JMFSchemaViolationException("double==null");
      else
        throw new JMFSchemaViolationException(val.getClass().getName());
    }
    public int encode(byte[] frame, int offset, Object val, int indirect, JMFMessageData msg) {
      if (val == null)
        throw new NullPointerException();
      ArrayUtil.writeLong(frame, offset, Double.doubleToLongBits(((Number)val).doubleValue()));
      return offset + ArrayUtil.LONG_SIZE;
    }
    public Object decode(byte[] frame, int offset, int indirect, JMFMessageData msg) {
      return new Double(Double.longBitsToDouble(ArrayUtil.readLong(frame, offset)));
    }
    public Object copy(Object val, int indirect) {
      return val;
    }
    public int estimateUnassembledSize(Object val) {
      return OBJECT_OVERHEAD + ArrayUtil.LONG_SIZE;
    }
    public int estimateUnassembledSize(byte[] frame, int offset) {
      return OBJECT_OVERHEAD + ArrayUtil.LONG_SIZE;
    }
  }
}
