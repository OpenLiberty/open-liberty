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
package com.ibm.ws.sib.api.jms.impl;

import java.io.ByteArrayOutputStream;

import javax.jms.JMSException;

import com.ibm.websphere.sib.api.jms.ApiJmsConstants;
import com.ibm.ws.sib.jms.util.ArrayUtil;
import com.ibm.ws.sib.jms.util.UTF8Encoder;
import com.ibm.ws.sib.utils.ras.SibTr;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;

/******************************************************************************/
/*  This source file contains PropertyCoder and its concrete subclasses       */
/******************************************************************************/

/**
 * PropertyCoder
 * This class, and its concrete subclasses, are used by MsgDestEncodingUtilsImpl
 * to encode/decode JmsDestination properties to/from the compact(ish) byte array
 * encoding stored in a message.
 */
abstract class PropertyCoder {
  private static TraceComponent tc = SibTr.register(PropertyCoder.class, ApiJmsConstants.MSG_GROUP_INT, ApiJmsConstants.MSG_BUNDLE_INT);

  // Constants shared by concrete subclasses
  final static byte STAR = 0x2a;                   // The UTF8 encoding of a '*'
  final static String STAR_STRING = "*";

  final static String SHORT_ON  = "N";
  final static String SHORT_OFF = "F";

  final String longName;
  final String shortName;
  final byte[] encodedName;

  String debugString;

  /**
   * constructor
   * Initialize the PropertyCoder from the given parameters.
   * The longName is stored in case an informative Exception message needs to be
   * built. The appropriate 'encoded' form of the property name is stored so that
   * it doesn't have to be built every time the property is encoded.
   *
   * @param longName             The long name of the property
   * @param shortName            The short name of the property, if one exists
   */
  PropertyCoder(String longName, String shortName) {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "PropertyCoder.<init>", new Object[]{longName, shortName});

    this.longName = longName;
    this.shortName = shortName;

    // If there is a non-null short name, stash its encoded form away
    if (shortName != null) {
      encodedName = UTF8Encoder.encode(shortName);
    }

    // Otherwise, we have to stash an encoded version of the long name
    else {
      int len = UTF8Encoder.getEncodedLength(longName);
      encodedName = new byte[1 + 2 + len];
      encodedName[0] = STAR;
      ArrayUtil.writeShort(encodedName, 1, (short)len);
      UTF8Encoder.encode(encodedName, 3, longName);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "PropertyCoder.<init>", toString());
  }

  /**
   * getLongName
   *
   * @return String The long name for the property this PropertyCoder belongs to.
   */
  final String getLongName() {
    return longName;
  }

  /**
   * getShortName
   *
   * @return String The short name for the property this PropertyCoder belongs to,
   *                or null if it does not have a short name.
   */
  final String getShortName() {
    return shortName;
  }

  /**
   * toString
   * @return String A String containing information which may be useful for debugging,
   *                for example in trace entries. It includes the class@hashcode
   *                information provided by the superclass, so that the instance can
   *                be uniquely identified.
   */
  public String toString() {
    if (debugString == null) {
      StringBuilder sb = new StringBuilder();
      sb.append(super.toString());
      sb.append(" [ ");
      sb.append(longName);    sb.append(", ");
      sb.append(shortName);   sb.append(" ]");
      debugString = new String(sb);
    }
    return debugString;
  }

  /**
   * encodeProperty
   * Encode a property into the given ByteArrayOutputStream. The encoding
   * consists of the encoded form of the name (which is already stashed away in
   * an instance variable), the short length of the encoded value, and the actual
   * encoded value.
   *
   * @param baos      The ByteArrayOutputStream to encode the property into
   * @param value     The value of the property
   *
   * @exception JMSException Thrown if anything went wrong with the encode.
   */
  abstract void encodeProperty(ByteArrayOutputStream baos, Object value) throws JMSException;

  /**
   * decodeProperty
   * Decode a property value from the given MsgDestEncodingUtilsImpl.PropertyInputStream
   * This is not entirely symmetrical with encodeProperty as it does not decode
   * the property name; this has been done by the caller as it needs to use it to
   * find the appropriate PropertyCoder for the decoding,
   *
   * @param stream     The PropertyInputSTream to decode a property from
   *
   * @return Object    The decoded property value.
   *
   * @exception JMSException description of exception
   */
  abstract Object decodeProperty(MsgDestEncodingUtilsImpl.PropertyInputStream stream) throws JMSException;
}

//-----------------------------------------------------------------------------

/**
 * ShortStringPropertyCoder
 * This class provides the encode and decode function for any property with a
 * short name whose value is encoded as a non-shortend arbitary String.
 */
class ShortStringPropertyCoder extends PropertyCoder {
  private static TraceComponent tc = SibTr.register(ShortStringPropertyCoder.class, ApiJmsConstants.MSG_GROUP_INT, ApiJmsConstants.MSG_BUNDLE_INT);

  ShortStringPropertyCoder(String longName, String shortName) {
    super(longName, shortName);
  }

  /*
   * @see PropertyCoder#encodeProperty
   */
  void encodeProperty(ByteArrayOutputStream baos, Object value) throws JMSException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "encodeProperty", new Object[]{baos, value});

    // The value should be a non-null String
    if (value instanceof String) {

      baos.write(encodedName, 0, encodedName.length);            // Write out the encoded tname

      String strVal = (String)value;
      int len = UTF8Encoder.getEncodedLength(strVal);
      byte[] buffer = new byte[len + ArrayUtil.SHORT_SIZE];     // bytes for the value length & the value itself
      ArrayUtil.writeShort(buffer, 0, (short)len);              // Encode in the length of the String value
      UTF8Encoder.encode(buffer, ArrayUtil.SHORT_SIZE, strVal); // Encode in the String value itself

      baos.write(buffer, 0, buffer.length);                     // Write the encode value to the buffer
    }

    // If it is not a String, something has gone horribly wrong
    else {
      throw (JMSException)JmsErrorUtils.newThrowable(JMSException.class
                                                    ,"INTERNAL_ERROR_CWSIA0362"
                                                    ,new Object[] {"ShortStringPropertyCoder.encodeProperty#1", longName, value}
                                                    ,null
                                                    ,"ShortStringPropertyCoder.encodeProperty#1"
                                                    ,null
                                                    ,tc);
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "encodeProperty");
  }

  /*
   * @see PropertyCoder#decodeProperty
   */
  Object decodeProperty(MsgDestEncodingUtilsImpl.PropertyInputStream stream) throws JMSException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "decodeProperty", stream);

    String value = stream.readStringValue();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "decodeProperty", value);
    return value;
  }
}

//-----------------------------------------------------------------------------

/**
 * OnOffPropertyCoder
 * This class provides the encode and decode function for any property with a
 * short name which has wo valid values: "On" and "Off". These are shrunk to "N" and "F" for encoding.
 * It subclasses ShortStringPropertyCoder, so that the actual encoding/decoding to the streams
 * does not have to be reimplemented.
 */
class OnOffPropertyCoder extends ShortStringPropertyCoder {
  private static TraceComponent tc = SibTr.register(OnOffPropertyCoder.class, ApiJmsConstants.MSG_GROUP_INT, ApiJmsConstants.MSG_BUNDLE_INT);

  OnOffPropertyCoder(String longName, String shortName) {
    super(longName, shortName);
  }

  /*
   * @see PropertyCoder#encodeProperty
   */
  void encodeProperty(ByteArrayOutputStream baos, Object value) throws JMSException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "encodeProperty", new Object[]{baos, value});

    if (value.equals(ApiJmsConstants.ON)) {
      super.encodeProperty(baos, SHORT_ON);
    }
    else {
      super.encodeProperty(baos, SHORT_OFF);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "encodeProperty");
  }

  /*
   * @see PropertyCoder#decodeProperty
   */
  Object decodeProperty(MsgDestEncodingUtilsImpl.PropertyInputStream stream) throws JMSException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "decodeProperty", stream);

    Object value = super.decodeProperty(stream);
    if (SHORT_ON.equals(value)) {
      value = ApiJmsConstants.ON;
    }
    else {
      value = ApiJmsConstants.OFF;
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "decodeProperty", value);
    return value;
  }
}

//-----------------------------------------------------------------------------

/**
 * ReadAheadCoder
 * This class provides the encode and decode function for the ReadAhead property. It has
 * has only two valid non-default values, are shrunk to "N" and "F" for encoding.
 * It sublasses ShortStringPropertyCoder, so that the actual encoding/decoding to the streams
 * does not have to be reimplemented.
 */
class ReadAheadCoder extends ShortStringPropertyCoder {
  private static TraceComponent tc = SibTr.register(ReadAheadCoder.class, ApiJmsConstants.MSG_GROUP_INT, ApiJmsConstants.MSG_BUNDLE_INT);

  ReadAheadCoder(String longName, String shortName) {
    super(longName, shortName);
  }

  /*
   * @see PropertyCoder#encodeProperty
   */
  void encodeProperty(ByteArrayOutputStream baos, Object value) throws JMSException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "encodeProperty", new Object[]{baos, value});

    if (value.equals(ApiJmsConstants.READ_AHEAD_ON)) {
      super.encodeProperty(baos, SHORT_ON);
    }
    else {
      super.encodeProperty(baos, SHORT_OFF);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "encodeProperty");
  }

  /*
   * @see PropertyCoder#decodeProperty
   */
  Object decodeProperty(MsgDestEncodingUtilsImpl.PropertyInputStream stream) throws JMSException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "decodeProperty", stream);

    Object value = super.decodeProperty(stream);
    if (SHORT_ON.equals(value)) {
      value = ApiJmsConstants.READ_AHEAD_ON;
    }
    else {
      value = ApiJmsConstants.READ_AHEAD_OFF;
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "decodeProperty", value);
    return value;
  }
}

//-----------------------------------------------------------------------------

/**
 * StringPropertyCoder
 * This class provides the encode and decode function for any String property which doesn't
 * have a shortName.
 * The value has to be encoded as a non-shortened arbitary String, preceded by
 * an asterisk, in order to be compatible with the previous releases.
 */
class StringPropertyCoder extends ShortStringPropertyCoder {
  private static TraceComponent tc = SibTr.register(StringPropertyCoder.class, ApiJmsConstants.MSG_GROUP_INT, ApiJmsConstants.MSG_BUNDLE_INT);

  StringPropertyCoder(String longName, String shortName) {
    super(longName, shortName);
  }

  /*
   * @see PropertyCoder#encodeProperty
   */
  void encodeProperty(ByteArrayOutputStream baos, Object value) throws JMSException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "encodeProperty", new Object[]{baos, value});

    // The value should be a non-null String
    if (value instanceof String) {
      super.encodeProperty(baos, STAR_STRING + (String)value);     // The value has to have '*' preprended
    }

    // If it is not a String, something has gone horribly wrong
    else {
      throw (JMSException)JmsErrorUtils.newThrowable(JMSException.class
                                                    ,"INTERNAL_ERROR_CWSIA0362"
                                                    ,new Object[] {"StringPropertyCoder.encodeProperty#1", longName, value}
                                                    ,null
                                                    ,"StringPropertyCoder.encodeProperty#1"
                                                    ,null
                                                    ,tc);
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "encodeProperty");
  }

  /*
   * @see PropertyCoder#decodeProperty
   */
  Object decodeProperty(MsgDestEncodingUtilsImpl.PropertyInputStream stream) throws JMSException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "decodeProperty", stream);

    // Get the String value then strip the first character, which is always a '*'
    String value = (String)super.decodeProperty(stream);
    value = value.substring(1);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "decodeProperty", value);
    return value;
  }
}

//-----------------------------------------------------------------------------

/**
 * IntegerPropertyCoder
 * This class provides the encode and decode function for any Integer property
 * which doesn't have a shortName.
 * The value has to be encoded as a non-shortened arbitary String, preceded by
 * an asterisk, in order to be compatible with the previous releases.
 */
class IntegerPropertyCoder extends StringPropertyCoder {
  private static TraceComponent tc = SibTr.register(IntegerPropertyCoder.class, ApiJmsConstants.MSG_GROUP_INT, ApiJmsConstants.MSG_BUNDLE_INT);

  IntegerPropertyCoder(String longName, String shortName) {
    super(longName, shortName);
  }

  /*
   * @see PropertyCoder#encodeProperty
   */
  void encodeProperty(ByteArrayOutputStream baos, Object value) throws JMSException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "encodeProperty", new Object[]{baos, value});

    super.encodeProperty(baos, value.toString());  // Just call the superclass, with the Integer 'toString'ed

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "encodeProperty");
  }

  /*
   * @see PropertyCoder#decodeProperty
   */
  Object decodeProperty(MsgDestEncodingUtilsImpl.PropertyInputStream stream) throws JMSException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "decodeProperty", stream);

    // Get the String value in the normal way, then turn it into an Integer
    String temp = (String)super.decodeProperty(stream);
    Integer value = Integer.valueOf(temp);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "decodeProperty", value);
    return value;
  }
}

//-----------------------------------------------------------------------------

/**
 * PhantomPropertyCoder
 * This class provides an PropertyCoder which does not actually encode or decode
 * a property. It exists purely so that properties which are not encoded/decoded
 * 'normally' can have their own PropertyCoder instance.
 */
class PhantomPropertyCoder extends PropertyCoder {
  private static TraceComponent tc = SibTr.register(PhantomPropertyCoder.class, ApiJmsConstants.MSG_GROUP_INT, ApiJmsConstants.MSG_BUNDLE_INT);

  PhantomPropertyCoder(String longName, String shortName) {
    super(longName, shortName);
  }

  /*
   * @see PropertyCoder#encodeProperty
   */
  void encodeProperty(ByteArrayOutputStream baos, Object value) throws JMSException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "encodeProperty", new Object[]{baos, value});

    // The PhantomPropertyCoder does not encode/decode properties so this method should
    // never be called. If it is, throw an informative JMSException.
    throw (JMSException)JmsErrorUtils.newThrowable(JMSException.class
                                                  ,"INTERNAL_ERROR_CWSIA0362"
                                                  ,new Object[] {"PhantomPropertyCoder.encodeProperty#1", longName, value}
                                                  ,null
                                                  ,"PhantomPropertyCoder.encodeProperty#1"
                                                  ,null
                                                  ,tc);

  }

  /*
   * @see PropertyCoder#decodeProperty
   */
  Object decodeProperty(MsgDestEncodingUtilsImpl.PropertyInputStream stream) throws JMSException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "decodeProperty", stream);

    // The PhantomPropertyCoder does not encode/decode properties so this method should
    // never be called. If it is, throw an informative JMSException.
    JMSException e = (JMSException)JmsErrorUtils.newThrowable(JMSException.class
                                                             ,"INTERNAL_ERROR_CWSIA0362"
                                                             ,new Object[] {"PhantomPropertyCoder.decodeProperty#1", shortName, longName}
                                                             ,null
                                                             ,null   // FFDC it separately so we can dump the stream
                                                             ,null
                                                             ,tc);
    FFDCFilter.processException(e, "com.ibm.ws.sib.api.jms.PhantomPropertyCoder","PhantomPropertyCoder.decodeProperty#1",
      new Object[] {shortName, longName, stream.toDebugString()});
    throw e;
  }
}
