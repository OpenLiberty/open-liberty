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

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.mfp.jmf.JMFAddress;
import com.ibm.ws.sib.mfp.jmf.JMFMessageData;
import com.ibm.ws.sib.mfp.jmf.JMFMessageCorruptionException;
import com.ibm.ws.sib.mfp.jmf.JMFModelNotImplementedException;
import com.ibm.ws.sib.mfp.jmf.JMFPrimitiveType;
import com.ibm.ws.sib.mfp.jmf.JMFSchemaViolationException;
import com.ibm.ws.sib.mfp.jmf.JMFUninitializedAccessException;
import com.ibm.ws.sib.mfp.jmf.JmfConstants;
import com.ibm.ws.sib.mfp.jmf.JmfTr;
import com.ibm.ws.sib.mfp.MfpConstants;
import com.ibm.ws.sib.mfp.util.ArrayUtil;
import com.ibm.ws.sib.mfp.util.HexUtil;
import com.ibm.ws.sib.mfp.util.UTF8Encoder;

/**
 * This class holds the encodings and object representations for the primitive types.  It
 * also holds the mappings from representation classes to primitive types and from XSD
 * type names to primitive types.  The latter is for convenience in initializing a
 * JMFPrimitiveType object in a single call to setXSDTypeName; there is no intrinsic
 * requirement that the JMF primitive types follow the XSD primitive types otherwise.
 */

public final class JSBaseTypes {
  private static TraceComponent tc = JmfTr.register(JSBaseTypes.class, JmfConstants.MSG_GROUP, JmfConstants.MSG_BUNDLE);

  //------------------------------------------------------------------------------------
  // The definitions in this class are divided into three sections.
  //
  // 1. The Coder section defines the JSCoder objects needed to complete the JSBaseTypes
  // section.
  //
  // 2. The JSBaseTypes section defines all the base types in order of their type code,
  // giving
  //
  // - the Java class whose instances are used to represent the type "internally" (for the
  // content matcher and in the cache).  The JSCoder.decode method always produces this
  // type.
  //
  // - The Java class whose instances are used to represent the type for external APIs.
  // This is the same as the previous except for numeric and boolean types, for which the
  // internal classes are NumericValue and BooleanValue while the external ones are the
  // standard Java wrapper classes.  The JSCoder.encode method accepts either the internal
  // or the external type.
  //
  // - the length occupied by values of the type when serialized or an indication that
  // such lengths vary from instance to instance
  //
  // - the JSCoder object for the type (containing methods to serialize, deserialize, and
  // compute the serialized length).
  //
  // 3. The maps section provides hashed maps from XML Schema type names to typecodes and
  // from Java classes to type codes.
  //------------------------------------------------------------------------------------

  //------------------------------------------------------------------------------------
  // Coder section:
  //------------------------------------------------------------------------------------

  // The JSCoder for Strings (also accepts StringBuffers).
  private static JSCoder StringCoder = new JSCoder() {

    public int getEncodedLength(Object val, int indirect, JMFMessageData msg) {
      if (val == null) {
        return 4;
      }
      else {
        return 4 + UTF8Encoder.getEncodedLength((String)val);                   // SIB0112a.mfp.1
      }
    }

    public Object validate(Object val, int indirect) throws JMFSchemaViolationException {
      if (val instanceof String || val == null)
        return val;
      else
        throw new JMFSchemaViolationException(val.getClass().getName());
    }

    // encode (only) supports StringBuffer as well as String
    public int encode(byte[] frame, int offset, Object val, int indirect, JMFMessageData msg) {
      if (val == null) {
        ArrayUtil.writeInt(frame, offset, -1);
        return offset + 4;
      }
      else {
        if (val instanceof StringBuffer) {
          val = ((StringBuffer)val).toString();
        }
        int written = UTF8Encoder.encode(frame, offset + 4, (String)val);       // SIB0112a.mfp.1
        ArrayUtil.writeInt(frame, offset, written);
        return offset + 4 + written;
      }
    }

    public Object decode(byte[] frame, int offset, int indirect, JMFMessageData msg) throws JMFMessageCorruptionException {
      int len = ArrayUtil.readInt(frame, offset);
      if (len == -1)
        return null;
      JSListCoder.sanityCheck(len, frame, offset);
      offset += 4;
      String ans;
      try {
        ans = new String(frame, offset, len, "UTF8");
      } catch (UnsupportedEncodingException e) {
        FFDCFilter.processException(e, "JSBaseTypes$StringCoder.decode", "168", Integer.valueOf(offset),
            new Object[] { MfpConstants.DM_BUFFER, frame, Integer.valueOf(0), Integer.valueOf(frame.length) });
        IllegalArgumentException ex = new IllegalArgumentException();
        ex.initCause(e);
        throw ex;
      }
      return ans;
    }

    public Object copy(Object val, int indirect) {
      // Strings are immutable, so we can return the original
      return val;
    }

    public int estimateUnassembledSize(Object val) {
      if (val == null) {
        return 0;
      }
      else {
        return estSize(((String)val).length());
      }
    }

    public int estimateUnassembledSize(byte[] frame, int offset) {
      int len = ArrayUtil.readInt(frame, offset);
      if (len == -1) {
        return 0;
      }
      else {
        return estSize(len);
      }
    }

    // An unassembled String consists of a String object and a char[]. A char takes 2 bytes,
    private int estSize(int length) {
      return JSCoder.OBJECT_OVERHEAD*2 + length*2;
    }
  };

  // The JSCoder for byte[], which covers both the BINARY and IDREF typeCodes.
  private static JSCoder ByteArrayCoder = new JSCoder() {

    public int getEncodedLength(Object val, int indirect, JMFMessageData msg) {
      if (val == null)
        return 4;
      else
        return ((byte[])val).length + 4;
    }

    public Object validate(Object val, int indirect) throws JMFSchemaViolationException {
      if (val instanceof byte[] || val == null)
        return val;
      else
        throw new JMFSchemaViolationException(val.getClass().getName());
    }

    public int encode(byte[] frame, int offset, Object val, int indirect, JMFMessageData msg) {
      if (val == null) {
        ArrayUtil.writeInt(frame, offset, -1);
        return offset + 4;
      }
      byte[] bval = (byte[])val;
      ArrayUtil.writeInt(frame, offset, bval.length);
      System.arraycopy(bval, 0, frame, offset + 4, bval.length);
      return offset + 4 + bval.length;
    }

    public Object decode(byte[] frame, int offset, int indirect, JMFMessageData msg) throws JMFMessageCorruptionException {
      int len = ArrayUtil.readInt(frame, offset);
      if (len == -1)
        return null;
      JSListCoder.sanityCheck(len, frame, offset);
      byte[] bval = new byte[len];
      System.arraycopy(frame, offset + 4, bval, 0, len);
      return bval;
    }

    public Object copy(Object val, int indirect) {
      // byte arrays are not immutable but are protected by outer layers (in
      // JMFStore and sib.mfp.impl
      return val;
    }

    public int estimateUnassembledSize(Object val) {
      if (val == null) {
        return 0;
      }
      else {
        return JSCoder.OBJECT_OVERHEAD + ((byte[])val).length;
      }
    }

    public int estimateUnassembledSize(byte[] frame, int offset) {
      int len = ArrayUtil.readInt(frame, offset);
      if (len == -1) {
        return 0;
      }
      else {
        return JSCoder.OBJECT_OVERHEAD + len;
      }
    }
  };

  // The JSCoders for fixed length byte arrays
  private static JSCoder Byte8Coder = new ByteFixedCoder(8);
  private static JSCoder Byte12Coder = new ByteFixedCoder(12);

  private static class ByteFixedCoder implements JSCoder {
    ByteFixedCoder(int size) {
      this.size = size;
    }
    private final int size;

    public int getEncodedLength(Object val, int indirect, JMFMessageData msg) {
      return size + 1;
    }

    public Object validate(Object val, int indirect) throws JMFSchemaViolationException {
      if (val == null)
        return val;
      else if (val instanceof byte[])
        if (((byte[])val).length == size)
          return val;
        else
          throw new JMFSchemaViolationException("byte"+size+" size: " + ((byte[])val).length);
      else
        throw new JMFSchemaViolationException(val.getClass().getName());
    }

    public int encode(byte[] frame, int offset, Object val, int indirect, JMFMessageData msg) {
      if (val == null) {
        frame[offset] = 0;
      } else {
        frame[offset] = (byte)size;
        System.arraycopy((byte[])val, 0, frame, offset + 1, size);
      }
      return offset + 1 + size;
    }

    public Object decode(byte[] frame, int offset, int indirect, JMFMessageData msg) throws JMFMessageCorruptionException {
      int len = frame[offset];
      if (len == 0)
        return null;
      else if (len != size) {
        JMFMessageCorruptionException jmce =  new JMFMessageCorruptionException(
            "Bad length: " + len + " (0x" + HexUtil.toString(new int[] { len }) + ") at offset " + offset);
        FFDCFilter.processException(jmce, "com.ibm.ws.sib.mfp.jmf.impl.JSBaseTypes$ByteFixedCoder.decode", "272", this,
            new Object[] { MfpConstants.DM_BUFFER, frame, Integer.valueOf(0), Integer.valueOf(frame.length) });
        throw jmce;
      }
      byte[] bval = new byte[size];
      System.arraycopy(frame, offset + 1, bval, 0, size);
      return bval;
    }

    public Object copy(Object val, int indirect) {
      // byte arrays are not immutable but are protected by outer layers (in
      // JMFStore and sib.mfp.impl
      return val;
    }

    public int estimateUnassembledSize(Object val) {
      return JSCoder.OBJECT_OVERHEAD + size;
    }

    public int estimateUnassembledSize(byte[] frame, int offset) {
      return JSCoder.OBJECT_OVERHEAD + size;
    }
  }

  // The JSCoder for BigDecimal.
  private static JSCoder DecimalCoder = new JSCoder() {

    // Four byte (total) length followed by four byte scale followed by unscaledValue
    // as a byte array
    public int getEncodedLength(Object val, int indirect, JMFMessageData msg) {
      return 8 + ((BigDecimal)val).unscaledValue().bitLength() / 8 + 1;
    }

    public Object validate(Object val, int indirect) throws JMFSchemaViolationException {
      if (val instanceof BigDecimal)
        return val;
      else if (val == null)
        throw new JMFSchemaViolationException("BigDecimal==null");
      else
        throw new JMFSchemaViolationException(val.getClass().getName());
    }

    public int encode(byte[] frame, int offset, Object val, int indirect, JMFMessageData msg) {
      BigDecimal dval = (BigDecimal)val;
      byte[] unscaled = dval.unscaledValue().toByteArray();
      ArrayUtil.writeInt(frame, offset, unscaled.length + 4);
      ArrayUtil.writeInt(frame, offset + 4, dval.scale());
      System.arraycopy(unscaled, 0, frame, offset + 8, unscaled.length);
      return offset + 8 + unscaled.length;
    }

    public Object decode(byte[] frame, int offset, int indirect, JMFMessageData msg) throws JMFMessageCorruptionException {
      int len = ArrayUtil.readInt(frame, offset);
      JSListCoder.sanityCheck(len, frame, offset);
      int scale = ArrayUtil.readInt(frame, offset + 4);
      len -= 4;  // original length included the scale
      byte[] unscaled = new byte[len];
      System.arraycopy(frame, offset + 8, unscaled, 0, len);
      return new BigDecimal(new BigInteger(unscaled), scale);
    }

    public Object copy(Object val, int indirect) {
      // BigDecimal is immutable, so we can return the original
      return val;
    }

    // No idea how much size a BigDecimal takes, but we don't really care so just guess
    public int estimateUnassembledSize(Object val) {
      if (val == null) {
        return 0;
      }
      else {
        return 50;
      }
    }

    public int estimateUnassembledSize(byte[] frame, int offset) {
      int len = ArrayUtil.readInt(frame, offset);
      if (len == -1) {
        return 0;
      }
      else {
        return 50;
      }
    }
  };

  // The JSCoder for Date.  Note: we currently encode this value as a
  // millisecond time, which discards all the information in the Date object that is
  // not "point in time" information.  This is actually close to correct in terms of the
  // apparent intent of the XML Schema Datatypes standard, which does not support any
  // locale-specific annotations to accompany the representation, other than timezone.
  // While a timezone can accompany an XML date, canonical dates use UTC.  However, more
  // study/discussion may be needed to decide whether this encoding (and this
  // implementation of the encoding) is good enough.
  private static JSCoder DateCoder = new JSCoder() {

    public int getEncodedLength(Object val, int indirect, JMFMessageData msg) {
      return 8;
    }

    public Object validate(Object val, int indirect) throws JMFSchemaViolationException {
      if (val instanceof Date)
        return val;
      else if (val == null)
        throw new JMFSchemaViolationException("Date==null");
      else
        throw new JMFSchemaViolationException(val.getClass().getName());
    }

    public int encode(byte[] frame, int offset, Object val, int indirect, JMFMessageData msg) {
      if (val == null)
        throw new NullPointerException();
      ArrayUtil.writeLong(frame, offset, ((Date)val).getTime());
      return offset + 8;
    }

    public Object decode(byte[] frame, int offset, int indirect, JMFMessageData msg) {
      return new Date(ArrayUtil.readLong(frame, offset));
    }

    public Object copy(Object val, int indirect) {
      // Date is not immutable but does implement clone for an efficient copy
      return ((Date)val).clone();
    }

    // No idea how much size a Date takes, but we don't really care so just guess
    public int estimateUnassembledSize(Object val) {
      if (val == null) {
        return 0;
      }
      else {
        return 100;
      }
    }

    public int estimateUnassembledSize(byte[] frame, int offset) {
      int len = ArrayUtil.readInt(frame, offset);
      if (len == -1) {
        return 0;
      }
      else {
        return 100;
      }
    }
  };

  // The JSCoder for BigInteger.
  private static JSCoder BigIntegerCoder = new JSCoder() {

    // Four byte (total) length followed by value as two's complement byte array
    public int getEncodedLength(Object val, int indirect, JMFMessageData msg) {
      return 4 + ((BigInteger)val).bitLength() / 8 + 1;
    }

    public Object validate(Object val, int indirect) throws JMFSchemaViolationException {
      if (val instanceof BigInteger)
        return val;
      else if (val == null)
        throw new JMFSchemaViolationException("BigInteger==null");
      else
        throw new JMFSchemaViolationException(val.getClass().getName());
    }

    public int encode(byte[] frame, int offset, Object val, int indirect, JMFMessageData msg) {
      byte[] byteForm = ((BigInteger) val).toByteArray();
      ArrayUtil.writeInt(frame, offset, byteForm.length);
      System.arraycopy(byteForm, 0, frame, offset + 4, byteForm.length);
      return offset + 4 + byteForm.length;
    }

    public Object decode(byte[] frame, int offset, int indirect, JMFMessageData msg) throws JMFMessageCorruptionException {
      int len = ArrayUtil.readInt(frame, offset);
      JSListCoder.sanityCheck(len, frame, offset);
      byte[] byteForm = new byte[len];
      System.arraycopy(frame, offset + 4, byteForm, 0, len);
      return new BigInteger(byteForm);
    }

    public Object copy(Object val, int indirect) {
      // BigInteger is immutable, so we can return the original
      return val;
    }

    // No idea how much size a BigInteger takes, but we don't really care so just guess
    public int estimateUnassembledSize(Object val) {
      if (val == null) {
        return 0;
      }
      else {
        return 50;
      }
    }

    public int estimateUnassembledSize(byte[] frame, int offset) {
      int len = ArrayUtil.readInt(frame, offset);
      if (len == -1) {
        return 0;
      }
      else {
        return 50;
      }
    }
  };

  // The coder for IDREF types.
  private static JSCoder IDREFCoder = new JSCoder() {

    public int getEncodedLength(Object val, int indirect, JMFMessageData msg) throws JMFUninitializedAccessException, JMFSchemaViolationException, JMFModelNotImplementedException, JMFMessageCorruptionException {
      if (val != null)
        val = ((JMFAddress) val).getContent();
      return ByteArrayCoder.getEncodedLength(val, indirect, msg);
    }

    public int encode(byte[] frame, int offset, Object val, int indirect, JMFMessageData msg) throws JMFUninitializedAccessException, JMFSchemaViolationException, JMFModelNotImplementedException, JMFMessageCorruptionException {
      if (val != null)
        val = ((JMFAddress) val).getContent();
      return ByteArrayCoder.encode(frame, offset, val, indirect, msg);
    }

    public Object decode(byte[] frame, int offset, int indirect, JMFMessageData msg) throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFMessageCorruptionException {
      Object val = ByteArrayCoder.decode(frame, offset, indirect, msg);
      if (val != null)
        val = new JMFAddress((byte[]) val);
      return val;
    }

    public Object validate(Object val, int indirect) throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFUninitializedAccessException, JMFMessageCorruptionException {
      if (val instanceof JMFAddress || val == null)
        return val;
      else
        throw new JMFSchemaViolationException(val.getClass().getName());
    }

    public Object copy(Object val, int indirect) throws JMFSchemaViolationException {
      // Note: val is never null here.
      return new JMFAddress((JMFAddress) val);
    }

    // An IDREF is basically a byte array with a wrapper
    public int estimateUnassembledSize(Object val) {
      if (val == null) {
        return 0;
      }
      else {
        return JSCoder.OBJECT_OVERHEAD + ByteArrayCoder.estimateUnassembledSize(((JMFAddress)val).getContent());
      }
    }

    // An IDREF is basically a byte array with a wrapper
    public int estimateUnassembledSize(byte[] frame, int offset) {
      return JSCoder.OBJECT_OVERHEAD + ByteArrayCoder.estimateUnassembledSize(frame, offset);
    }
  };

  // The coder for SIMPLELIST types.
  private static JSCoder SimpleListCoder = new JSCoder() {

    public int getEncodedLength(Object val, int indirect, JMFMessageData msg)
        throws JMFUninitializedAccessException, JMFSchemaViolationException, JMFModelNotImplementedException, JMFMessageCorruptionException {
      int ans = 8; // overhead is 4 byte length plus 4 byte list size
      List lval = (List) val;
      for (Iterator iter = lval.iterator(); iter.hasNext();)
        ans += AnySimpleTypeCoder.getEncodedLength(iter.next(), indirect, msg);
      return ans;
    }

    public int encode(byte[] frame, int offset, Object val, int indirect, JMFMessageData msg) throws JMFUninitializedAccessException, JMFSchemaViolationException, JMFModelNotImplementedException, JMFMessageCorruptionException {
      List lval = (List) val;
      // Leave length to be written at the end but write size now
      ArrayUtil.writeInt(frame, offset+4, lval.size());
      int place = offset + 8;
      for (Iterator iter = lval.iterator(); iter.hasNext();)
        place = AnySimpleTypeCoder.encode(frame, place, iter.next(), indirect, msg);
      ArrayUtil.writeInt(frame, offset, place-offset-4); // length
      return place;
    }

    public Object decode(byte[] frame, int offset, int indirect, JMFMessageData msg) throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFMessageCorruptionException {
      List ans = new ArrayList();
      int size = ArrayUtil.readInt(frame, offset+4);
      int place = offset + 8;
      for (int i = 0; i < size; i++) {
        ans.add(AnySimpleTypeCoder.decode(frame, place, indirect, msg));
        place += 4 + ArrayUtil.readInt(frame, place);
      }
      return ans;
    }

    public Object validate(Object val, int indirect) throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFUninitializedAccessException, JMFMessageCorruptionException {
      if (val instanceof List)
        return val;
      else if (val == null)
        throw new JMFSchemaViolationException("List==null");
      else
        throw new JMFSchemaViolationException(val.getClass().getName());
    }

    public Object copy(Object val, int indirect) throws JMFSchemaViolationException {
      return new ArrayList((List) val);
    }

    public int estimateUnassembledSize(Object val) {
      if (val == null) {
        return 0;
      }
      else {
        int listSize = ((List)val).size();
        if (listSize > 0) {
          int entrySize = 0;
          if (((List)val).get(0) instanceof String) {
            entrySize = JSCoder.OBJECT_OVERHEAD*2 + 20*2;   // Guess that Strings average 20 characters
          }
          else {
            entrySize = JSCoder.OBJECT_OVERHEAD + 8;        // Guess that non-Strings average 8 bytes of data
          }
          return  JSCoder.OBJECT_OVERHEAD + 20 + listSize*entrySize;  // The overhead of the List + a guess for each entry
        }
        else {
          return 0;  // We'll assume that an empty list is actually the MFP singleton EMPTY_LIST
        }
      }
    }

    public int estimateUnassembledSize(byte[] frame, int offset) {
      int len = ArrayUtil.readInt(frame, offset);
      int listSize = ArrayUtil.readInt(frame, offset+4);
      int size = 0;                        // Default to 0 as it may be empty
      if (listSize > 0) {
        size = OBJECT_OVERHEAD + 20        // The overhead of the List
             + listSize*OBJECT_OVERHEAD    // An overhead for each entry
             + len*2;                      // Add double the encoded length to hopefully cover the actual data
      }
      return size;
    }

  };

  // The coder for anySimpleType, which is represented as Object of one of the recognized
  // types (including List; this results in allowing Lists to nest, although we don't
  // actually expect this facility to be exploited).
  private static JSCoder AnySimpleTypeCoder = new JSCoder() {
    private int getTypeInfo(Object val) throws JMFSchemaViolationException {
      Class lookup;
      if (val == null)
        return JMFPrimitiveType.STRING;
      else if (val instanceof List)
        return JMFPrimitiveType.SIMPLELIST;
      else
        lookup = val.getClass();
      Integer code = (Integer)classTypes.get(lookup);
      if (code == null)
        throw new JMFSchemaViolationException(val.getClass().getName());
      return code.intValue();
    }

    public int getEncodedLength(Object val, int indirect, JMFMessageData msg)
        throws JMFUninitializedAccessException, JMFSchemaViolationException, JMFModelNotImplementedException, JMFMessageCorruptionException {
      JSBaseTypes t = baseTypes[getTypeInfo(val)];
      return 5 + t.coder.getEncodedLength(val, 0, msg);
    }

    public Object validate(Object val, int indirect)
        throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFUninitializedAccessException, JMFMessageCorruptionException {
      JSBaseTypes t = baseTypes[getTypeInfo(val)];
      return t.coder.validate(val, 0);
    }

    public int encode(byte[] frame, int offset, Object val, int indirect, JMFMessageData msg)
        throws JMFUninitializedAccessException, JMFSchemaViolationException, JMFModelNotImplementedException, JMFMessageCorruptionException {
      int code = getTypeInfo(val);
      JSBaseTypes t = baseTypes[code];
      ArrayUtil.writeInt(frame, offset, 1 + t.coder.getEncodedLength(val, 0, msg));
      frame[offset + 4] = (byte)code;
      return t.coder.encode(frame, offset + 5, val, 0, msg);
    }

    public Object decode(byte[] frame, int offset, int indirect, JMFMessageData msg)
        throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFMessageCorruptionException {
      int typeCode = frame[offset + 4];
      return baseTypes[typeCode].coder.decode(frame, offset + 5, 0, msg);
    }

    public Object copy(Object val, int indirect)
        throws JMFSchemaViolationException {
      JSBaseTypes t = baseTypes[getTypeInfo(val)];
      return t.coder.copy(val, 0);
    }

    public int estimateUnassembledSize(Object val) {
      if (val == null) {
        return 0;
      }
      else {
        try {
          JSBaseTypes t = baseTypes[getTypeInfo(val)];
          return t.coder.estimateUnassembledSize(val);
        }
        catch (JMFSchemaViolationException e) {
          // No FFDC code needed
          // We don't want to throw any exception here, or even FFDC it, so just guess
          return 24;
        }
      }
    }

    public int estimateUnassembledSize(byte[] frame, int offset) {
      int typeCode = frame[offset + 4];
      return baseTypes[typeCode].coder.estimateUnassembledSize(frame, offset + 5);
    }
  };

  // The JSCoder for QNames. This stores a QName as a series of three strings.
  private static JSCoder QNameCoder = new JSCoder() {

    public int getEncodedLength(Object val, int indirect, JMFMessageData msg)
    throws JMFUninitializedAccessException, JMFSchemaViolationException,
           JMFModelNotImplementedException, JMFMessageCorruptionException
    {
      String uri = null;
      String name = null;
      String prefix = null;
      if (val != null) {
        QName value = (QName)val;
        uri = value.getNamespaceURI();
        name = value.getLocalPart();
        prefix = value.getPrefix();
      }
      int result = StringCoder.getEncodedLength(uri,    indirect, msg) +
                   StringCoder.getEncodedLength(name,   indirect, msg) +
                   StringCoder.getEncodedLength(prefix, indirect, msg);
      return result;
    }

    public Object validate(Object val, int indirect) throws JMFSchemaViolationException {
      if (val instanceof QName || val == null)
        return val;
      else
        throw new JMFSchemaViolationException(val.getClass().getName());
    }

    // encode the QName as three strings
    public int encode(byte[] frame, int offset, Object val, int indirect, JMFMessageData msg)
    throws JMFUninitializedAccessException, JMFSchemaViolationException, JMFModelNotImplementedException, JMFMessageCorruptionException
    {
      String uri = null;
      String name = null;
      String prefix = null;
      if (val != null) {
        QName value = (QName)val;
        uri = value.getNamespaceURI();
        name = value.getLocalPart();
        prefix = value.getPrefix();
      }
      offset = StringCoder.encode(frame, offset, uri, indirect, msg);
      offset = StringCoder.encode(frame, offset, name, indirect, msg);
      offset = StringCoder.encode(frame, offset, prefix, indirect, msg);
      return offset;
    }

    // decode the qname (which was stored as 3 strings)
    public Object decode(byte[] frame, int offset, int indirect, JMFMessageData msg)
    throws JMFSchemaViolationException, JMFModelNotImplementedException,
           JMFMessageCorruptionException
    {
      // The string coder can read the 3 strings, but we need to be able to tell
      // it the start offset of each one. We work that out first.
      int uriOffset = offset;
      int len = ArrayUtil.readInt(frame, uriOffset);

      int nameOffset = uriOffset + 4;
      if(len != -1) nameOffset += len;
      len = ArrayUtil.readInt(frame, nameOffset);

      int prefixOffset = nameOffset + 4;
      if(len != -1) prefixOffset += len;

      String uri = (String) StringCoder.decode(frame, uriOffset, indirect, msg);
      String name = (String) StringCoder.decode(frame, nameOffset, indirect, msg);
      String prefix = (String) StringCoder.decode(frame, prefixOffset, indirect, msg);

      QName result = new QName(uri, name, prefix);

      return result;
    }

    public Object copy(Object val, int indirect) {
      // QNames are immutable, so we can return the original
      return val;
    }

    // We don't really care about QNames so just guess the size of an item containingg 3 Strings.
    public int estimateUnassembledSize(Object val) {
      return JSCoder.OBJECT_OVERHEAD*7 + 120;
    }
    public int estimateUnassembledSize(byte[] frame, int offset) {
      return JSCoder.OBJECT_OVERHEAD*7 + 120;
    }
  };
  //------------------------------------------------------------------------------------
  // JSBaseTypes section:
  //------------------------------------------------------------------------------------

  //------------------------------------------------------------------------------------
  // Instance variables and private constructor
  //------------------------------------------------------------------------------------

  // Java class name and size (if class should be a fixed size array)
  Class javaClass;
  int javaClassSize;

  // Serialized length for this type.  A positive number means that the length is fixed to
  // that length and the value need not be interrogated.  -1 means that the length is
  // varying, and the value must be interrogated (via JSCoder.getEncodedLength) to find the
  // value.
  int length;

  // The JSCoder for this type
  JSCoder coder;

  // Constructor
  private JSBaseTypes(Class javaClass, int javaClassSize, int length, JSCoder coder) {
    this.javaClass = javaClass;
    this.javaClassSize = javaClassSize;
    this.length = length;
    this.coder = coder;
  }

  //------------------------------------------------------------------------------------
  // Table of JSBaseTypes definitions, in typeCode order
  //------------------------------------------------------------------------------------

  static JSBaseTypes[] baseTypes = { null, // typeCode==0 does not correspond to a type,
    new JSBaseTypes(Boolean.class, -1, 1, JSCoder.BOOLEAN),
    new JSBaseTypes(Byte.class, -1, 1, JSCoder.BYTE),
    new JSBaseTypes(Short.class, -1, 2, JSCoder.SHORT),
    new JSBaseTypes(Character.class, -1, 2, JSCoder.CHAR),
    new JSBaseTypes(Integer.class, -1, 4, JSCoder.INT),
    new JSBaseTypes(Long.class, -1, 8, JSCoder.LONG),
    new JSBaseTypes(Float.class, -1, 4, JSCoder.FLOAT),
    new JSBaseTypes(Double.class, -1, 8, JSCoder.DOUBLE),
    new JSBaseTypes(String.class, -1, -1, StringCoder),
    new JSBaseTypes(BigDecimal.class, -1, -1, DecimalCoder),
    new JSBaseTypes(Date.class, -1, 8, DateCoder),
    new JSBaseTypes(byte[].class, -1, -1, ByteArrayCoder),
    new JSBaseTypes(Object.class, -1, -1, AnySimpleTypeCoder),
    new JSBaseTypes(BigInteger.class, -1, -1, BigIntegerCoder),
    new JSBaseTypes(JMFAddress.class, -1, -1, IDREFCoder),
    new JSBaseTypes(List.class, -1, -1, SimpleListCoder),
    new JSBaseTypes(byte[].class, 8, 9, Byte8Coder),
    new JSBaseTypes(byte[].class, 12, 13, Byte12Coder),
    new JSBaseTypes(QName.class, -1, -1, QNameCoder)};

  //------------------------------------------------------------------------------------
  // Mapping section:
  //------------------------------------------------------------------------------------

  //------------------------------------------------------------------------------------
  // Mapping from XML Schema type names to type codes
  //------------------------------------------------------------------------------------

  static Map xsdTypeNames = new HashMap();
  static {
    xsdTypeNames.put("boolean"            , Integer.valueOf(JMFPrimitiveType.BOOLEAN));
    xsdTypeNames.put("byte"               , Integer.valueOf(JMFPrimitiveType.BYTE));
    xsdTypeNames.put("short"              , Integer.valueOf(JMFPrimitiveType.SHORT));
    xsdTypeNames.put("unsignedShort"      , Integer.valueOf(JMFPrimitiveType.CHAR));
    xsdTypeNames.put("int"                , Integer.valueOf(JMFPrimitiveType.INT));
    xsdTypeNames.put("long"               , Integer.valueOf(JMFPrimitiveType.LONG));
    xsdTypeNames.put("float"              , Integer.valueOf(JMFPrimitiveType.FLOAT));
    xsdTypeNames.put("double"             , Integer.valueOf(JMFPrimitiveType.DOUBLE));
    xsdTypeNames.put("string"             , Integer.valueOf(JMFPrimitiveType.STRING));
    xsdTypeNames.put("decimal"            , Integer.valueOf(JMFPrimitiveType.DECIMAL));
    xsdTypeNames.put("duration"           , Integer.valueOf(JMFPrimitiveType.DATETIME));
    xsdTypeNames.put("dateTime"           , Integer.valueOf(JMFPrimitiveType.DATETIME));
    xsdTypeNames.put("time"               , Integer.valueOf(JMFPrimitiveType.DATETIME));
    xsdTypeNames.put("date"               , Integer.valueOf(JMFPrimitiveType.DATETIME));
    xsdTypeNames.put("gYearMonth"         , Integer.valueOf(JMFPrimitiveType.DATETIME));
    xsdTypeNames.put("gYear"              , Integer.valueOf(JMFPrimitiveType.DATETIME));
    xsdTypeNames.put("gMonthDay"          , Integer.valueOf(JMFPrimitiveType.DATETIME));
    xsdTypeNames.put("gDay"               , Integer.valueOf(JMFPrimitiveType.DATETIME));
    xsdTypeNames.put("gMonth"             , Integer.valueOf(JMFPrimitiveType.DATETIME));
    xsdTypeNames.put("hexBinary"          , Integer.valueOf(JMFPrimitiveType.BINARY));
    xsdTypeNames.put("base64Binary"       , Integer.valueOf(JMFPrimitiveType.BINARY));
    xsdTypeNames.put("anyURI"             , Integer.valueOf(JMFPrimitiveType.STRING));
    xsdTypeNames.put("QName"              , Integer.valueOf(JMFPrimitiveType.QNAME));
    xsdTypeNames.put("NOTATION"           , Integer.valueOf(JMFPrimitiveType.STRING));
    xsdTypeNames.put("normalizedString"   , Integer.valueOf(JMFPrimitiveType.STRING));
    xsdTypeNames.put("token"              , Integer.valueOf(JMFPrimitiveType.STRING));
    xsdTypeNames.put("language"           , Integer.valueOf(JMFPrimitiveType.STRING));
    xsdTypeNames.put("NMTOKEN"            , Integer.valueOf(JMFPrimitiveType.STRING));
    xsdTypeNames.put("NMTOKENS"           , Integer.valueOf(JMFPrimitiveType.SIMPLELIST));
    xsdTypeNames.put("Name"               , Integer.valueOf(JMFPrimitiveType.STRING));
    xsdTypeNames.put("NCName"             , Integer.valueOf(JMFPrimitiveType.STRING));
    xsdTypeNames.put("ID"                 , Integer.valueOf(JMFPrimitiveType.STRING));
    xsdTypeNames.put("IDREF"              , Integer.valueOf(JMFPrimitiveType.IDREF));
    xsdTypeNames.put("IDREFS"             , Integer.valueOf(JMFPrimitiveType.SIMPLELIST));
    xsdTypeNames.put("ENTITY"             , Integer.valueOf(JMFPrimitiveType.STRING));
    xsdTypeNames.put("ENTITIES"           , Integer.valueOf(JMFPrimitiveType.SIMPLELIST));
    xsdTypeNames.put("integer"            , Integer.valueOf(JMFPrimitiveType.BIGINTEGER));
    xsdTypeNames.put("positiveInteger"    , Integer.valueOf(JMFPrimitiveType.BIGINTEGER));
    xsdTypeNames.put("nonPositiveInteger" , Integer.valueOf(JMFPrimitiveType.BIGINTEGER));
    xsdTypeNames.put("negativeInteger"    , Integer.valueOf(JMFPrimitiveType.BIGINTEGER));
    xsdTypeNames.put("nonNegativeInteger" , Integer.valueOf(JMFPrimitiveType.BIGINTEGER));
    xsdTypeNames.put("unsignedLong"       , Integer.valueOf(JMFPrimitiveType.BIGINTEGER));
    xsdTypeNames.put("unsignedInt"        , Integer.valueOf(JMFPrimitiveType.LONG));
    xsdTypeNames.put("anySimpleType"      , Integer.valueOf(JMFPrimitiveType.ANYSIMPLETYPE));
    // These are not strictly XSD type names, but we need them in the list to enable the
    // parser to process "byte8" and "byte12" data types
    xsdTypeNames.put("byte8"              , Integer.valueOf(JMFPrimitiveType.BYTE8));
    xsdTypeNames.put("byte12"             , Integer.valueOf(JMFPrimitiveType.BYTE12));
  }

  // ------------------------------------------------------------------------------------
  // Mapping from Class to type codes
  // ------------------------------------------------------------------------------------

  static HashMap classTypes = new HashMap();
  static {
    for (int i = baseTypes.length - 1; i > 0; i--)
      classTypes.put(baseTypes[i].javaClass, Integer.valueOf(i));
    // Add mappings for the truly primitive classes
    classTypes.put(boolean.class  , Integer.valueOf(JMFPrimitiveType.BOOLEAN));
    classTypes.put(byte.class     , Integer.valueOf(JMFPrimitiveType.BYTE));
    classTypes.put(short.class    , Integer.valueOf(JMFPrimitiveType.SHORT));
    classTypes.put(char.class     , Integer.valueOf(JMFPrimitiveType.CHAR));
    classTypes.put(int.class      , Integer.valueOf(JMFPrimitiveType.INT));
    classTypes.put(long.class     , Integer.valueOf(JMFPrimitiveType.LONG));
    classTypes.put(float.class    , Integer.valueOf(JMFPrimitiveType.FLOAT));
    classTypes.put(double.class   , Integer.valueOf(JMFPrimitiveType.DOUBLE));
  }
}
