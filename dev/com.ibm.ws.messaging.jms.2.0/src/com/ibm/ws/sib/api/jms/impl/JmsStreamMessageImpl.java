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

import java.io.UnsupportedEncodingException;

import javax.jms.JMSException;
import javax.jms.StreamMessage;
import javax.jms.MessageEOFException;
import javax.jms.MessageFormatException;
import javax.jms.MessageNotReadableException;

import com.ibm.websphere.sib.api.jms.ApiJmsConstants;
import com.ibm.ws.sib.mfp.JsJmsMessage;
import com.ibm.ws.sib.mfp.JsJmsStreamMessage;
import com.ibm.ws.sib.mfp.MessageCreateFailedException;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * @author matrober
 * @author kingdon
 */
public class JmsStreamMessageImpl extends JmsMessageImpl implements StreamMessage {

  /**
   * assigned at version 1.20
   */
  private static final long serialVersionUID = 3222131330774615635L;

  // ******************* PRIVATE STATE VARIABLES *******************

  /**
   * MFP message object representing a JMS StreamMessage
   * Note: Do not initialise this to null here, otherwise it will overwrite
   *       the setup done by instantiateMessage!
   */
  private JsJmsStreamMessage streamMsg;

  /** flag to indicate that a byte[] has been read from the stream, but couldn't
   * be returned to the application in a single call. Further calls to readBytes
   * are required to consume the remainder of the byte[].
   */
  private boolean partReadBytesElement = false;
  /** Reference to a byte[] which has been partly returned to the application.
   */
  private byte[] bytesElement = null;
  /** offset into bytesElement indicating where to start returning next block
   * of bytes from in the next readBytes call.
   */
  private int bytesElementOffset;

  // *************************** TRACE INITIALIZATION **************************
  private static TraceComponent tc = SibTr.register(JmsStreamMessageImpl.class, ApiJmsConstants.MSG_GROUP_EXT, ApiJmsConstants.MSG_BUNDLE_EXT);


  // ************************ CONSTRUCTORS *************************

  public JmsStreamMessageImpl() throws JMSException {
    // Calling the superclass no-args constructor in turn leads to the
    // instantiateMessage method being called, which we override to return
    // a text message.
    super();
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "JmsStreamMessageImpl");

    messageClass = CLASS_STREAM;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "JmsStreamMessageImpl");
  }

  /**
   * This constructor is used by the JmsMessage.inboundJmsInstance method (static)
   * in order to provide the inbound message path from MFP component to JMS component.
   */
  JmsStreamMessageImpl(JsJmsStreamMessage newMsg, JmsSessionImpl newSess) {

    // Pass this object to the parent class so that it can keep a reference.
    super(newMsg, newSess);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "JmsStreamMessageImpl", new Object[]{newMsg, newSess});

    // Store the reference we are given, and inform the parent class.
    streamMsg = newMsg;
    messageClass = CLASS_STREAM;

    // make sure that the streamMsg cursor is reset to the beginning
    streamMsg.reset();

    // Note that we do NOT initialize the defaults for inbound messages.

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "JmsStreamMessageImpl");
  }

  /**
   * Construct a jetstream jms message from a (possibly non-jetstream)
   * vanilla jms message.
   */
  JmsStreamMessageImpl(StreamMessage streamMessage)  throws JMSException {
    // copy message headers and properties.
    super(streamMessage);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "JmsStreamMessageImpl", streamMessage);

    messageClass = CLASS_STREAM;

    // go to start of stream (unfortunately also makes body read-only)
    streamMessage.reset();

    // copy stream.
    try {
      while (true) {
        writeObject(streamMessage.readObject());
      }
    }
    catch (JMSException e) {
      // No FFDC code needed
      // d238447 FFDC review - Normal loop end condition for above copy loop, so
      //   don't call processThrowable.
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "exception caught: ", e);
      // Don't throw the exception on - this is the normal loop end condition
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "JmsStreamMessageImpl");
  }


  // *************************** INTERFACE METHODS *****************************

  /* (non-Javadoc)
   * @see javax.jms.StreamMessage#readBoolean()
   */
  public boolean readBoolean() throws JMSException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "readBoolean");

    boolean result;
    Object nextObj = getNextField("readBoolean");

    try {
      result = JmsMessageImpl.parseBoolean(nextObj, "");
    }
    catch (JMSException e) {
      // No FFDC code needed
      streamMsg.stepBack();
      // d238447 FFDC review. Don't call process throwable in this case
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "exception caught: ",e);
      throw e;
    }
    catch (RuntimeException e) {
      // No FFDC code needed
      streamMsg.stepBack();
      // d238447 FFDC review. Don't call process throwable in this case
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "exception caught: ",e);
      throw e;
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "readBoolean",  result);
    return result;
  }

  /* (non-Javadoc)
   * @see javax.jms.StreamMessage#readByte()
   */
  public byte readByte() throws JMSException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "readByte");

    byte result;
    Object nextObj = getNextField("readByte");

    try {
      result = JmsMessageImpl.parseByte(nextObj, "");
    }
    catch (RuntimeException e) {
      // No FFDC code needed
      streamMsg.stepBack();
      // d238447 FFDC review. Don't call process throwable in this case
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "exception caught: ",e);
      throw e;
    }
    catch (JMSException e) {
      // No FFDC code needed
      streamMsg.stepBack();
      // d238447 FFDC review. Don't call process throwable in this case
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "exception caught: ",e);
      throw e;
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "readByte",  result);
    return result;
  }

  /* (non-Javadoc)
   * @see javax.jms.StreamMessage#readShort()
   */
  public short readShort() throws JMSException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "readShort");

    short result;
    Object nextObj = getNextField("readShort");

    try {
      result = JmsMessageImpl.parseShort(nextObj, "");
    }
    catch (JMSException e) {
      // No FFDC code needed
      streamMsg.stepBack();
      // d238447 FFDC review. Don't call process throwable in this case
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "exception caught: ",e);
      throw e;
    }
    catch (RuntimeException e) {
      // No FFDC code needed
      streamMsg.stepBack();
      // d238447 FFDC review. Don't call process throwable in this case
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "exception caught: ",e);
      throw e;
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "readShort",  result);
    return result;
  }

  /* (non-Javadoc)
   * @see javax.jms.StreamMessage#readChar()
   */
  public char readChar() throws JMSException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "readChar");

    char result;
    Object nextObj = getNextField("readChar");

    if (nextObj == null) {
      // nulls have to be explicitly converted to NullPointerException
      // see StreamMessage javadoc for details
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "null found");
      throw (NullPointerException) JmsErrorUtils.newThrowable(NullPointerException.class, "NULL_CHAR_CWSIA0164", null, tc);
    }
    else if (nextObj instanceof Character) {
      result = ((Character) nextObj).charValue();
    }
    else {
      // move the cursor back
      streamMsg.stepBack();
      // only char can be retrieved as char
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "non-char item found");
      throw newBadConvertException(nextObj, "", "Character", tc);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "readChar",  result);
    return result;
  }

  /* (non-Javadoc)
   * @see javax.jms.StreamMessage#readInt()
   */
  public int readInt() throws JMSException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "readInt");

    int result;
    Object nextObj = getNextField("readInt");

    try {
      result = JmsMessageImpl.parseInt(nextObj, "");
    }
    catch (JMSException e) {
      // No FFDC code needed
      streamMsg.stepBack();
      // d238447 FFDC review. Don't call process throwable in this case
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "exception caught: ",e);
      throw e;
    }
    catch (RuntimeException e) {
      // No FFDC code needed
      streamMsg.stepBack();
      // d238447 FFDC review. Don't call process throwable in this case
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "exception caught: ",e);
      throw e;
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "readInt",  result);
    return result;
  }

  /* (non-Javadoc)
   * @see javax.jms.StreamMessage#readLong()
   */
  public long readLong() throws JMSException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "readLong");

    long result;
    Object nextObj = getNextField("readLong");

    try {
      result = JmsMessageImpl.parseLong(nextObj, "");
    }
    catch (JMSException e) {
      // No FFDC code needed
      streamMsg.stepBack();
      // d238447 FFDC review. Don't call process throwable in this case
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "exception caught: ",e);
      throw e;
    }
    catch (RuntimeException e) {
      // No FFDC code needed
      streamMsg.stepBack();
      // d238447 FFDC review. Don't call process throwable in this case
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "exception caught: ",e);
      throw e;
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "readLong",  result);
    return result;
  }

  /* (non-Javadoc)
   * @see javax.jms.StreamMessage#readFloat()
   */
  public float readFloat() throws JMSException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "readFloat");

    float result;
    Object nextObj = getNextField("readFloat");

    try {
      result = JmsMessageImpl.parseFloat(nextObj, "");
    }
    catch (JMSException e) {
      // No FFDC code needed
      streamMsg.stepBack();
      // d238447 FFDC review. Don't call process throwable in this case
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "exception caught: ",e);
      throw e;
    }
    catch (RuntimeException e) {
      // No FFDC code needed
      streamMsg.stepBack();
      // d238447 FFDC review. Don't call process throwable in this case
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "exception caught: ",e);
      throw e;
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "readFloat",  result);
    return result;
  }

  /* (non-Javadoc)
   * @see javax.jms.StreamMessage#readDouble()
   */
  public double readDouble() throws JMSException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "readDouble");

    double result;
    Object nextObj = getNextField("readDouble");

    try {
      result = JmsMessageImpl.parseDouble(nextObj, "");
    }
    catch (JMSException e) {
      // No FFDC code needed
      streamMsg.stepBack();
      // d238447 FFDC review. Don't call process throwable in this case
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "exception caught: ",e);
      throw e;
    }
    catch (RuntimeException e) {
      // No FFDC code needed
      streamMsg.stepBack();
      // d238447 FFDC review. Don't call process throwable in this case
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "exception caught: ",e);
      throw e;
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "readDouble",  result);
    return result;
  }

  /* (non-Javadoc)
   * @see javax.jms.StreamMessage#readString()
   */
  public String readString() throws JMSException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "readString");

    Object obj = getNextField("readString");
    String value = null;

    if ((obj instanceof String) || (obj == null)) {
      value = (String) obj;
    }
    else if (obj instanceof byte[]) {
      // move the cursor back
      streamMsg.stepBack();
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "found byte array not String");
      throw newBadConvertException(obj, "", "String", tc);
    }
    else {
      // Every other type of property can be returned as a String
      value = obj.toString();
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "readString",  value);
    return value;
  }

  /* (non-Javadoc)
   * @see javax.jms.StreamMessage#readBytes(byte[])
   */
  public int readBytes(byte[] value) throws JMSException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "readBytes", value);

    int nBytes;
    int result = 99;

    checkBodyReadable("readBytes");

    // do we need a new element?
    if (!partReadBytesElement) {

      // Get the next element from the stream
      Object obj = getNextField("readBytes");

      if (obj == null) {
        // javadoc specifies we should return -1
        result = -1;
      }
      else if (obj instanceof byte[]) {
        bytesElement = (byte[]) obj;
        bytesElementOffset = 0;
        // javadoc specifies an empty array should return 0
        if (bytesElement.length == 0) {
          result = 0;
        }
      }
      else {
        // move the cursor back
        streamMsg.stepBack();
        throw newBadConvertException(obj, "", "byte[]", tc);
      }
    }

    // If we haven't already set result to a 'proper' value to indicate null/empty....
    if (result == 99) {

      if (value == null) {
        // MQJMS didn't code explicitly for this case and would have run into a
        // null pointer exception when checking the length of the supplied buffer.
        // We'll keep the NullPointerException for compatability, but provide an
        // nls'd message.
        throw (NullPointerException) JmsErrorUtils.newThrowable(
          NullPointerException.class,
          "NULL_BUFFER_CWSIA0161",
          null,
          tc);
      }

      // how much can we return?
      int nBytesLeft = bytesElement.length - bytesElementOffset;
      if (nBytesLeft > value.length) {
        // too much to return in one go
        nBytes = value.length;
      }
      else {
        // we can fit it all in
        nBytes = nBytesLeft;
      }

      // if we return a full buffer then we must be called again (even if
      // there is no more data to return!)
      if (nBytes == value.length) {
        // Set flag to indicate more data to be returned
        partReadBytesElement = true;
      }
      else {
        partReadBytesElement = false;
      }

      if (nBytes == 0) {
        // this case arises when all the data fitted exactly into
        // the buffer provided on the previous call.
        // Spec requires us to return -1
        result = -1;
      }
      else {
        System.arraycopy(bytesElement, bytesElementOffset, value, 0, nBytes);
        // remember where to continue from on the next call
        bytesElementOffset += nBytes;
        result = nBytes;
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "readBytes",  result);
    return result;
  }

  /* (non-Javadoc)
   * @see javax.jms.StreamMessage#readObject()
   */
  public Object readObject()  throws JMSException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "readObject");

    Object result;
    Object obj = getNextField("readObject");

    // Special case for byte[].
    // We need to protect against the application modifying the contents
    // of the byte[], as this could break the lazy copy mechanism used
    // to isolate the sender and receiver(s) of messages.
    if (obj instanceof byte[]) {
      int len = ((byte[])obj).length;
      result = new byte[len];
      System.arraycopy(obj, 0, result, 0, len);
    }
    else {
      result = obj;
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "readObject",  result);
    return result;
  }


  /* (non-Javadoc)
   * @see javax.jms.StreamMessage#writeBoolean(boolean)
   */
  public void writeBoolean(boolean x) throws JMSException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "writeBoolean", x);

    checkBodyWriteable("writeBoolean");

    try {
      streamMsg.writeBoolean(x);
    }
    catch (UnsupportedEncodingException e) {
      // No FFDC Code Needed
      throw (JMSException) JmsErrorUtils.newThrowable(
          JMSException.class,
          "EXCEPTION_RECEIVED_CWSIA0022",
          new Object[] {e, "JmsStreamMessageImpl.writeBoolean"},
          e, "JmsStreamMessageImpl#1", this, tc);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "writeBoolean");
  }

  /* (non-Javadoc)
   * @see javax.jms.StreamMessage#writeByte(byte)
   */
  public void writeByte(byte x) throws JMSException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "writeByte", x);

    checkBodyWriteable("writeByte");

    try {
      streamMsg.writeByte(x);
    }
    catch (UnsupportedEncodingException e) {
      // No FFDC Code Needed
      throw (JMSException) JmsErrorUtils.newThrowable(
          JMSException.class,
          "EXCEPTION_RECEIVED_CWSIA0022",
          new Object[] {e, "JmsStreamMessageImpl.writeByte"},
          e, "JmsStreamMessageImpl#2", this, tc);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "writeByte");
  }

  /* (non-Javadoc)
   * @see javax.jms.StreamMessage#writeShort(short)
   */
  public void writeShort(short x) throws JMSException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "writeShort", x);

    checkBodyWriteable("writeShort");

    try {
      streamMsg.writeShort(x);
    }
    catch (UnsupportedEncodingException e) {
      // No FFDC Code Needed
      throw (JMSException) JmsErrorUtils.newThrowable(
          JMSException.class,
          "EXCEPTION_RECEIVED_CWSIA0022",
          new Object[] {e, "JmsStreamMessageImpl.writeShort"},
          e, "JmsStreamMessageImpl#3", this, tc);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "writeShort");
  }

  /* (non-Javadoc)
   * @see javax.jms.StreamMessage#writeChar(char)
   */
  public void writeChar(char x) throws JMSException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "writeChar", x);

    checkBodyWriteable("writeChar");

    try {
      streamMsg.writeChar(x);
    }
    catch (UnsupportedEncodingException e) {
      // No FFDC Code Needed
      throw (JMSException) JmsErrorUtils.newThrowable(
          JMSException.class,
          "EXCEPTION_RECEIVED_CWSIA0022",
          new Object[] {e, "JmsStreamMessageImpl.writeChar"},
          e, "JmsStreamMessageImpl#4", this, tc);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "writeChar");
  }

  /* (non-Javadoc)
   * @see javax.jms.StreamMessage#writeInt(int)
   */
  public void writeInt(int x) throws JMSException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "writeInt", x);

    checkBodyWriteable("writeInt");
    try {
      streamMsg.writeInt(x);
    }
    catch (UnsupportedEncodingException e) {
      // No FFDC Code Needed
      throw (JMSException) JmsErrorUtils.newThrowable(
          JMSException.class,
          "EXCEPTION_RECEIVED_CWSIA0022",
          new Object[] {e, "JmsStreamMessageImpl.writeInt"},
          e, "JmsStreamMessageImpl#5", this, tc);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "writeInt");
  }

  /* (non-Javadoc)
   * @see javax.jms.StreamMessage#writeLong(long)
   */
  public void writeLong(long x) throws JMSException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "writeLong", x);

    checkBodyWriteable("writeLong");
    try {
      streamMsg.writeLong(x);
    }
    catch (UnsupportedEncodingException e) {
      // No FFDC Code Needed
      throw (JMSException) JmsErrorUtils.newThrowable(
          JMSException.class,
          "EXCEPTION_RECEIVED_CWSIA0022",
          new Object[] {e, "JmsStreamMessageImpl.writeLong"},
          e, "JmsStreamMessageImpl#6", this, tc);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "writeLong");
  }

  /* (non-Javadoc)
   * @see javax.jms.StreamMessage#writeFloat(float)
   */
  public void writeFloat(float x) throws JMSException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "writeFloat", x);

    checkBodyWriteable("writeFloat");
    try {
      streamMsg.writeFloat(x);
    }
    catch (UnsupportedEncodingException e) {
      // No FFDC Code Needed
      throw (JMSException) JmsErrorUtils.newThrowable(
          JMSException.class,
          "EXCEPTION_RECEIVED_CWSIA0022",
          new Object[] {e, "JmsStreamMessageImpl.writeFloat"},
          e, "JmsStreamMessageImpl#7", this, tc);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "writeFloat");
  }

  /* (non-Javadoc)
   * @see javax.jms.StreamMessage#writeDouble(double)
   */
  public void writeDouble(double x) throws JMSException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "writeDouble", x);

    checkBodyWriteable("writeDouble");
    try {
      streamMsg.writeDouble(x);
    }
    catch (UnsupportedEncodingException e) {
      // No FFDC Code Needed
      throw (JMSException) JmsErrorUtils.newThrowable(
          JMSException.class,
          "EXCEPTION_RECEIVED_CWSIA0022",
          new Object[] {e, "JmsStreamMessageImpl.writeDouble"},
          e, "JmsStreamMessageImpl#8", this, tc);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "writeDouble");
  }

  /* (non-Javadoc)
   * @see javax.jms.StreamMessage#writeString(java.lang.String)
   */
  public void writeString(String x) throws JMSException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "writeString", x);

    checkBodyWriteable("writeString");
    try {
      streamMsg.writeString(x);
    }
    catch (UnsupportedEncodingException e) {
      // No FFDC Code Needed
      throw (JMSException) JmsErrorUtils.newThrowable(
          JMSException.class,
          "EXCEPTION_RECEIVED_CWSIA0022",
          new Object[] {e, "JmsStreamMessageImpl.writeString"},
          e, "JmsStreamMessageImpl#9", this, tc);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "writeString");
  }

  /* (non-Javadoc)
   * @see javax.jms.StreamMessage#writeBytes(byte[])
   */
  public void writeBytes(byte[] x) throws JMSException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "writeBytes", x);

    checkBodyWriteable("writeBytes");

    // take a copy so that the application can change the original without side effecting
    // the message body.
    if (x != null) {
      byte[] c = new byte[x.length];
      System.arraycopy(x, 0, c, 0, x.length);
      x = c;
    }

    try {
      streamMsg.writeBytes(x);
    }
    catch (UnsupportedEncodingException e) {
      // No FFDC Code Needed
      throw (JMSException) JmsErrorUtils.newThrowable(
          JMSException.class,
          "EXCEPTION_RECEIVED_CWSIA0022",
          new Object[] {e, "JmsStreamMessageImpl.writeBytes"},
          e, "JmsStreamMessageImpl#10", this, tc);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "writeBytes");
  }

  /* (non-Javadoc)
   * @see javax.jms.StreamMessage#writeBytes(byte[], int, int)
   */
  public void writeBytes(byte[] x, int offset, int len) throws JMSException {

    checkBodyWriteable("writeBytes");
    // JsJmsStreamMessage doesn't support part arrays, so we'll have to copy out
    byte[] subA = new byte[len];
    System.arraycopy(x, offset, subA, 0, len);
    try {
      streamMsg.writeBytes(subA);
    }
    catch (UnsupportedEncodingException e) {
      // No FFDC Code Needed
      throw (JMSException) JmsErrorUtils.newThrowable(
          JMSException.class,
          "EXCEPTION_RECEIVED_CWSIA0022",
          new Object[] {e, "JmsStreamMessageImpl.writeBytes"},
          e, "JmsStreamMessageImpl#11", this, tc);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "writeBytes");
  }

  /* (non-Javadoc)
   * @see javax.jms.StreamMessage#writeObject(java.lang.Object)
   */
  public void writeObject(Object x) throws JMSException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "writeObject", x);

    checkBodyWriteable("writeObject");

    // check that the object is of acceptable type
    if (!(  (x == null)
         || (x instanceof String)
         || (x instanceof Number)
         || (x instanceof Boolean)
         || (x instanceof Character)
         || (x instanceof byte[])
         )) {
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "value is bad type: "+x.getClass().getName());
      // d238447 FFDC review. App error, no FFDC required.
      throw (JMSException) JmsErrorUtils.newThrowable(
        MessageFormatException.class,
        "BAD_OBJECT_CWSIA0189",
        new Object[] { x.getClass().getName() },
        tc);
    }

    if (x instanceof byte[]) {
      // take a copy so that the application can change the original without
      // side effecting the message.
      byte[] v = (byte[])x;
      byte[] tmp = new byte[v.length];
      System.arraycopy(v, 0, tmp, 0, v.length);
      x = tmp;
    }

    try {
      streamMsg.writeObject(x);
    }
    catch (UnsupportedEncodingException e) {
      // No FFDC Code Needed
      throw (JMSException) JmsErrorUtils.newThrowable(
          JMSException.class,
          "EXCEPTION_RECEIVED_CWSIA0022",
          new Object[] {e, "JmsStreamMessageImpl.writeObject"},
          e, "JmsStreamMessageImpl#12", this, tc);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "writeObject");
  }

  /**
   * Reset the message body, making it read only and setting the stream
   * cursor to the beginning of the message.
   * After reset has been called, the message body is immutable (the only way of
   * changing it is to call clearBody and start again).
   * @see javax.jms.StreamMessage#reset()
   */
  public void reset() throws JMSException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "reset");

    // The message is now read only
    setBodyReadOnly();

    // reset the cursor within the stream
    streamMsg.reset();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "reset");
  }

  //******************** IMPLEMENTATION METHODS **********************

  /**
   * @see com.ibm.ws.sib.api.jms.impl.JmsMessageImpl#instantiateMessage()
   */
  protected JsJmsMessage instantiateMessage() throws Exception {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "instantiateMessage");

    // Create a new message object.
    JsJmsStreamMessage newMsg = null;
    try {
      newMsg = jmfact.createJmsStreamMessage();
    }
    catch(MessageCreateFailedException e) {
      // No FFDC code needed
      // d238447 FFDC review. Don't call processThrowable here.
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "exception caught: ", e);
      throw e;
    }

    // Do any other reference storing here (for subclasses)
    streamMsg = newMsg;

    // Return the new object.
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "instantiateMessage",  newMsg);
    return newMsg;
  }

  // PRIVATE methods

  /**
   * Get the next field from the stream, performing checks for
   * being readable, not being half way through a byte[] and
   * EOF.
   * @param callingMethodName The name of the method to appear in nls exceptions
   * @return the next field in the stream, as Object
   * @throws MessageEOFException if at the end of the stream
   * @throws MessageNotReadableException if the message is write only.
   * @throws MessageFormatException if a byte[] field is partly read
   */
  private Object getNextField(String callingMethodName) throws MessageEOFException, MessageNotReadableException, MessageFormatException, JMSException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getNextField", callingMethodName);

    checkBodyReadable(callingMethodName);

    if (partReadBytesElement) {
      throw (MessageFormatException) JmsErrorUtils.newThrowable(
        MessageFormatException.class,
        "INCOMPLETE_BYTE_ARRAY_CWSIA0163",
        null, tc);
    }

    Object nextObj = null;
    try {
      nextObj = streamMsg.readObject();
    }
    catch (UnsupportedEncodingException e) {
      // No FFDC Code Needed
      throw (JMSException) JmsErrorUtils.newThrowable(
          JMSException.class,
          "EXCEPTION_RECEIVED_CWSIA0022",
          new Object[] {e, "JmsStreamMessageImpl.getNextField"},
          e, "JmsStreamMessageImpl#13", this, tc);
    }

    if (nextObj == JsJmsStreamMessage.END_OF_STREAM) {
      throw (MessageEOFException) JmsErrorUtils.newThrowable(
        MessageEOFException.class,
        "END_STREAM_CWSIA0162", null, tc);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getNextField",  nextObj);
    return nextObj;
  }
}
