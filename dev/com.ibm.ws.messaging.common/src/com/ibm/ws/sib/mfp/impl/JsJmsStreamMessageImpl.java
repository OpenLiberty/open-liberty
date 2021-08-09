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

import java.io.UnsupportedEncodingException;
import java.util.List;

import com.ibm.websphere.sib.SIApiConstants;
import com.ibm.ws.sib.mfp.*;
import com.ibm.ws.sib.mfp.schema.JmsStreamBodyAccess;
import com.ibm.ws.sib.mfp.schema.JsPayloadAccess;
import com.ibm.ws.sib.utils.ras.SibTr;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;

/**
 *  JsJmsStreamMessageImpl extends JsJmsMessageImpl and hence JsMessageImpl,
 *  and is the implementation class for the JsJmsStreamMessage interface.
 */
final class JsJmsStreamMessageImpl extends JsJmsMessageImpl implements JsJmsStreamMessage {

  private static TraceComponent tc = SibTr.register(JsJmsStreamMessageImpl.class, MfpConstants.MSG_GROUP, MfpConstants.MSG_BUNDLE);
 
  private final static long serialVersionUID = 1L;
  private final static byte[] flattenedClassName;                               // SIB0112b.mfp.2

  // The overhead of a fluffed Stream list includes the JsMsgList & its contained List.
  private final static int FLUFFED_STREAM_LIST_OVERHEAD = FLUFFED_OBJECT_OVERHEAD
                                                        + FLUFFED_REF_SIZE
                                                        + FLUFFED_JMF_LIST_SIZE;

  /* Get the flattened form of the classname                   SIB0112b.mfp.2 */
  static {
    flattenedClassName = flattenClassName(JsJmsStreamMessageImpl.class.getName());
  }

  /* **************************************************************************/
  /* Constructors                                                             */
  /* **************************************************************************/

  /**
   *  Constructor for a new Jetstream JMS StreamMessage.
   *
   *  This constructor should never be used except by JsMessageImpl.createNew().
   *  The method must not actually do anything.
   */
  JsJmsStreamMessageImpl() {
  }

  /**
   *  Constructor for a new Jetstream JMS StreamMessage.
   *  To be called only by the JsJmsMessageFactory.
   *
   *  @param flag Flag to distinguish different construction reasons.
   *
   *  @exception MessageDecodeFailedException Thrown if such a message can not be created
   */
  JsJmsStreamMessageImpl(int flag)  throws MessageDecodeFailedException {
    super(flag);
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "<init>");

    /* Set the JMS format & body information */
    setFormat(SIApiConstants.JMS_FORMAT_STREAM);
    setBodyType(JmsBodyType.STREAM);

    // We can skip this for an inbound MQ message as the MQJsMessageFactory will
    // replace the PAYLOAD_DATA with an MQJsApiEncapsulation.
    if (flag != MfpConstants.CONSTRUCTOR_INBOUND_MQ) {
      jmo.getPayloadPart().setPart(JsPayloadAccess.PAYLOAD_DATA, JmsStreamBodyAccess.schema);
      getPayload().setChoiceField(JmsStreamBodyAccess.BODY, JmsStreamBodyAccess.IS_BODY_EMPTY);
    }

    bodyList = null;
  }

  /**
   *  Constructor for an inbound message.
   *  (Only to be called by a superclass make method.)
   *
   *  @param inJmo The JsMsgObject representing the inbound method.
   */
  JsJmsStreamMessageImpl(JsMsgObject inJmo) {
    super(inJmo);
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "<init>, inbound jmo ");
  }


  /* ************************************************************************ */
  /* Payload methods                                                          */
  /* ************************************************************************ */

  private transient int streamIndex = 0;
  private transient JsMsgList bodyList = null;

  /*
   *  Return the next value from the payload Stream as an Object.
   *  <p>
   *  This method is used to return in objectified format any value
   *  that has been stored to the payload with any of the writeXxxx
   *  method calls. If a primitive was stored, the Object returned will be
   *  the corresponding object - e.g. an int will be returned as an Integer.
   *
   *  Javadoc description supplied by JsJmsMessage interface.
   */
  public Object readObject() throws UnsupportedEncodingException {
    if (streamIndex < getBodyList().size())
      return getBodyList().get(streamIndex++);
    return JsJmsStreamMessage.END_OF_STREAM;
  }


  /*
   * Clear the body (payload) of the message.
   */
  public void clearBody() {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "clearBody");
    getPayload().setChoiceField(JmsStreamBodyAccess.BODY, JmsStreamBodyAccess.IS_BODY_EMPTY);
    bodyList = new JsMsgList(null);
    reset();
    clearCachedLengths();
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "clearBody");
  }


  /*
   * Provide an estimate of encoded length of the payload
   */
  int guessPayloadLength() {
    int length = 0;
    // Total guess at average size of stream entry
    try {
      List<Object> payload = getBodyList();
      length = payload.size() * 40;
    }
    catch (UnsupportedEncodingException e) {
      // No FFDC code needed
      // hmm... how do we figure out a reasonable length
    }
    return length;
  }


  /**
   * guessFluffedDataSize
   * Return the estimated fluffed size of the payload data.
   *
   * For this class, we should return an approximation of the fluffed up payload
   * data, which is a List of items.
   *
   * @return int A guesstimate of the fluffed size of the payload data
   */
  int guessFluffedDataSize() {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "guessFluffedDataSize");

    int total = 0;

    // If we have a fluffed up in memory list ...
    if (bodyList != null) {
       // Add the overhead for the JsMsgList & its contained List
       total += FLUFFED_STREAM_LIST_OVERHEAD;
       // Guess each value has a data size of average 30, plus the Object overhead.
       // The actual size will be better for 'primitives' and worse for Strings
       total += bodyList.size() * (FLUFFED_OBJECT_OVERHEAD + 30);
    }

    // Figure out a guesstimate without fluffing up anything unnecessarily
    else {

      // Add the estimate for the fluffed payload size
      // If the body's JMF message is already fluffed up & cached, ask it for the size.
      // Do NOT hold on to this JSMsgPart, as it could lose validity at any time.
      JsMsgPart part = getPayloadIfFluffed();
      if (part != null) {
        // .... get the estimate for the list when fluffed
        total += part.estimateFieldValueSize(JmsStreamBodyAccess.BODY_DATA_VALUE);
      }

      // If the JMF message hasn't been fluffed up, find the total assembled length of
      // the payload message if possible.
      else {
        // If we have a valid length, remove a bit & assume the rest is the encoded list.
        int payloadSize = jmo.getPayloadPart().getAssembledLengthIfKnown();
        if (payloadSize != -1) {
          int flatListSize = payloadSize-FLATTENED_PAYLOAD_PART;
          // It is a number of entries which are probably mostly Strings. Let's say 15 chars each name.
          int numEnts = flatListSize / 15;
          if (numEnts > 0) {
            // Assume in UTF8, and add overhead for a Strings per entry: so 30 for chars, 56 for overhead
            // plus the overhead of a JMF Lists (at 72 each).
            total += numEnts*86 + 72;
          }
        }
        // If the payloadSize == -1, then the body message must have been fluffed up
        // but not yet cached, so we'll locate & cache it now.
        else {
          // .... get the estimate for the list when fluffed
          total += getPayload().estimateFieldValueSize(JmsStreamBodyAccess.BODY_DATA_VALUE);
        }
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "guessFluffedDataSize",  total);
    return total;
  }


  // Convenience method to get the payload as a JmsStreamBodySchema
  JsMsgPart getPayload() {
    return getPayload(JmsStreamBodyAccess.schema);
  }

  List<Object> getBodyList() throws UnsupportedEncodingException {
    if (bodyList == null) {
      try {
        List<Object> list = (List<Object>)getPayload().getField(JmsStreamBodyAccess.BODY_DATA_VALUE);
        bodyList = new JsMsgList(list);
      }
      catch (MFPUnsupportedEncodingRuntimeException e) {
        FFDCFilter.processException(e, "com.ibm.ws.sib.mfp.impl.JsJmsStreamMessageImpl.getBodyList", "148");
        throw (UnsupportedEncodingException)e.getCause();
      }
    }
    return bodyList;
  }

  /**
   *  Helper method used by the JMO to rewrite any transient data back into the
   *  underlying JMF message.
   *
   *  @param why The reason for the update
   *  @see com.ibm.ws.sib.mfp.MfpConstants
   */
  void updateDataFields(int why) {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "updateDataFields");
    super.updateDataFields(why);
    if (bodyList != null && bodyList.isChanged())
       getPayload().setField(JmsStreamBodyAccess.BODY_DATA_VALUE, bodyList.getList());
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "updateDataFields");
  }

  /* ************************************************************************ */
  /* Payload Set methods                                                      */
  /* ************************************************************************ */

  /*
   *  Write a boolean value into the payload Stream.
   *
   *  Javadoc description supplied by JsJmsMessage interface.
   */
  public void writeBoolean(boolean value) throws UnsupportedEncodingException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "writeBoolean", Boolean.valueOf(value));
    getBodyList().add(Boolean.valueOf(value));
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "writeBoolean");
  }


  /*
   *  Write a byte value into the payload Stream.
   *
   *  Javadoc description supplied by JsJmsMessage interface.
   */
  public void writeByte(byte value) throws UnsupportedEncodingException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "writeByte", Byte.valueOf(value));
    getBodyList().add(Byte.valueOf(value));
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "writeByte");
  }


  /*
   *  Write a short value into the payload Stream.
   *
   *  Javadoc description supplied by JsJmsMessage interface.
   */
  public void writeShort(short value) throws UnsupportedEncodingException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "writeShort", Short.valueOf(value));
    getBodyList().add(Short.valueOf(value));
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "writeShort");
  }


  /*
   *  Write a Unicode character value into the payload Stream.
   *
   *  Javadoc description supplied by JsJmsMessage interface.
   */
  public void writeChar(char value) throws UnsupportedEncodingException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "writeChar", Character.valueOf(value));
    getBodyList().add(Character.valueOf(value));
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "writeChar");
  }


  /*
   *  Set an integer value into the payload Stream.
   *
   *  Javadoc description supplied by JsJmsMessage interface.
   */
  public void writeInt(int value) throws UnsupportedEncodingException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "writeInt", Integer.valueOf(value));
    getBodyList().add(Integer.valueOf(value));
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "writeInt");
  }


  /*
   *  Write a long value into the payload Stream.
   *
   *  Javadoc description supplied by JsJmsMessage interface.
   */
  public void writeLong(long value) throws UnsupportedEncodingException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "writeLong", Long.valueOf(value));
    getBodyList().add(Long.valueOf(value));
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "writeLong");
  }


  /*
   *  Write a float value into the payload Stream.
   *
   *  Javadoc description supplied by JsJmsMessage interface.
   */
  public void writeFloat(float value) throws UnsupportedEncodingException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "writeFloat", new Float(value));
    getBodyList().add(new Float(value));
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "writeFloat");
  }


  /*
   *  Write a double value into the payload Stream.
   *
   *  Javadoc description supplied by JsJmsMessage interface.
   */
  public void writeDouble(double value) throws UnsupportedEncodingException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "writeDouble", new Double(value));
    getBodyList().add(new Double(value));
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "writeDouble");
  }


  /*
   *  Write a String value into the payload Stream.
   *
   *  Javadoc description supplied by JsJmsMessage interface.
   */
  public void writeString(String value) throws UnsupportedEncodingException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "writeString", value);
    getBodyList().add(value);
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "writeString");
  }


  /*
   *  Write a portion of the byte array value into the payload Stream.
   *
   */
  public void writeBytes(byte[] value) throws UnsupportedEncodingException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "writeBytes", value);
    getBodyList().add(value);
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "writeBytes");
  }


  /*
   *  Write a Java object value into the payload Stream.
   *  <p>
   *  Note that this method only works for the objectified primitive
   *  object types (Integer, Double, Long ...), String's and byte arrays.
   *
   *  Javadoc description supplied by JsJmsMessage interface.
   */
  public void writeObject(Object value) throws UnsupportedEncodingException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "writeObject", value);
    getBodyList().add(value);
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "writeObject");
  }


  /* ************************************************************************ */
  /* Miscellaneous Payload methods                                            */
  /* ************************************************************************ */

  /*
   *  Repositions the stream pointer to the beginning of the stream.
   *  This method does not set the message into 'read-only' mode as the MFP
   *  component has no knowledge of the readability/writeability of a message.
   *  The JMS API component will set and police readable/writeable state.
   *
   *  Javadoc description supplied by JsJmsMessage interface.
   */
  public void reset(){
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "reset");
    streamIndex = 0;
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "reset");
  }


  /*
   *  Repositions the stream pointer to the position immediately before the
   *  previous read. This method allows the API component to take responsiblity
   *  for determining whether a read was for a validly typed value or not.
   *  If not, it will call the stepBack method before throwing an Exception, to
   *  meet the requirements of the JMS Specification.
   *
   *  Javadoc description supplied by JsJmsMessage interface.
   */
  public void stepBack(){
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "stepBack");
    if (streamIndex > 0) streamIndex--;
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "stepBack");
  }


  /*
   *  Return a 'User Friendly' byte array for Admin to display on a panel
   *  <p>
   *  This method is used to return a byte array in the form
   *  value|value|......|value
   *  where | is the line.separator System property.
   *
   *  Javadoc description supplied by the JsJmsStreamMessage interface.
   */
  public byte[] getUserFriendlyBytes() throws UnsupportedEncodingException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getUserFriendlyBytes");

    byte[] result = null;

    /* Only need to do anything if there are items in the stream */
    if (getBodyList().size() > 0) {

      Object item;
      int index = 0;
      StringBuilder buff = new StringBuilder();
      String lineSeparator = System.lineSeparator();

      /* Get the first item and append it to the buffer */
      item = getBodyList().get(index++);
      if (item != null) {
        if (!(item instanceof byte[])) {
          buff.append(item.toString());
        }
        else {
          buff.append(new String((byte[])item));
        }
      }

      /* For all subsequent items append a separator first */
      while (index < getBodyList().size()) {
        buff.append(lineSeparator);
        item = getBodyList().get(index++);
        if (item != null) {
          if (!(item instanceof byte[])) {
            buff.append(item.toString());
          }
          else {
            buff.append(new String((byte[])item));
          }
        }
      }

      /* Now turn it into bytes */
      result = buff.toString().getBytes();
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getUserFriendlyBytes", result);
    return result;
  }


  /* **************************************************************************/
  /* Misc Package and Private Methods                                         */
  /* **************************************************************************/

  /**
   * Return the name of the concrete implementation class encoded into bytes
   * using UTF8.                                              SIB0112b.mfp.2
   *
   * @return byte[] The name of the implementation class encoded into bytes.
   */
  final byte[] getFlattenedClassName() {
    return flattenedClassName;
  }

}
