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
package com.ibm.ws.sib.mfp;

import java.io.UnsupportedEncodingException;

/**
 *  JsJmsStreamMessage extends JsJmsMessage and adds the get/set methods specific
 *  to a JMS StreamMessage.
 */
public interface JsJmsStreamMessage extends JsJmsMessage {

 /** Returned by readObject if there was nothing left to read.                */
 public final static Object END_OF_STREAM = new Object();

  /* ************************************************************************ */
  /* Payload Get method                                                       */
  /* ************************************************************************ */

  /**
   *  Return the next value from the payload Stream as an Object.
   *  <p>
   *  This method is used to return in objectified format the next value from
   *  the stream payload. The value may have been written by any of the writeXxxx
   *  method calls. If a primitive was stored, the Object returned will be
   *  the corresponding object - e.g. an int will be returned as an Integer.
   *  <p>
   *  If there are no more objects to be read, the method will return
   *  JsJmsStreamMessage.END_OF_STREAM.
   *
   *  @return An object representing the next value to the payload.
   *
   *  @exception UnsupportedEncodingException is thrown if the payload is encoded
   *             in a codepage which is not supported on this system.
   */
  public Object readObject() throws UnsupportedEncodingException;

  /* ************************************************************************ */
  /* Payload Set methods                                                      */
  /* ************************************************************************ */

  /**
   *  Write a boolean value into the payload Stream.
   *
   *  @param value the boolean value to write to the payload Stream.
   *
   *  @exception UnsupportedEncodingException is thrown if the payload is encoded
   *             in a codepage which is not supported on this system.
   */
  public void writeBoolean(boolean value) throws UnsupportedEncodingException;

  /**
   *  Write a byte value into the payload Stream.
   *
   *  @param value the byte value to write to the payload Stream.
   *
   *
   *  @exception UnsupportedEncodingException is thrown if the payload is encoded
   *             in a codepage which is not supported on this system.
   */
  public void writeByte(byte value) throws UnsupportedEncodingException;

  /**
   *  Write a short value into the payload Stream.
   *
   *  @param value the short value to write to the payload Stream.
   *
   *  @exception UnsupportedEncodingException is thrown if the payload is encoded
   *             in a codepage which is not supported on this system.
   */
  public void writeShort(short value) throws UnsupportedEncodingException;

  /**
   *  Write a Unicode character value into the payload Stream.
   *
   *  @param value the Unicode character value to write to the payload Stream.
   *
   *  @exception UnsupportedEncodingException is thrown if the payload is encoded
   *             in a codepage which is not supported on this system.
   */
  public void writeChar(char value) throws UnsupportedEncodingException;

  /**
   *  Set an integer value into the payload Stream.
   *
   *  @param value the integer value to write to the payload Stream.
   *
   *  @exception UnsupportedEncodingException is thrown if the payload is encoded
   *             in a codepage which is not supported on this system.
   */
  public void writeInt(int value) throws UnsupportedEncodingException;

  /**
   *  Write a long value into the payload Stream.
   *
   *  @param value the long value to write to the payload Stream.
   *
   *  @exception UnsupportedEncodingException is thrown if the payload is encoded
   *             in a codepage which is not supported on this system.
   */
  public void writeLong(long value) throws UnsupportedEncodingException;

  /**
   *  Write a float value into the payload Stream.
   *
   *  @param value the float value to write to the payload Stream.
   *
   *  @exception UnsupportedEncodingException is thrown if the payload is encoded
   *             in a codepage which is not supported on this system.
   */
  public void writeFloat(float value) throws UnsupportedEncodingException;

  /**
   *  Write a double value into the payload Stream.
   *
   *  @param value the double value to write to the payload Stream.
   *
   *  @exception UnsupportedEncodingException is thrown if the payload is encoded
   *             in a codepage which is not supported on this system.
   */
  public void writeDouble(double value) throws UnsupportedEncodingException;

  /**
   *  Write a String value into the payload Stream.
   *
   *  @param value the String value to write to the payload Stream.
   *
   *  @exception UnsupportedEncodingException is thrown if the payload is encoded
   *             in a codepage which is not supported on this system.
   */
  public void writeString(String value) throws UnsupportedEncodingException;

  /**
   *  Write a portion of the byte array value into the payload Stream.
   *
   *  @param value the byte array value to write to the payload Stream.
   *
   *  @exception UnsupportedEncodingException is thrown if the payload is encoded
   *             in a codepage which is not supported on this system.
   */
  public void writeBytes(byte[] value) throws UnsupportedEncodingException;

  /**
   *  Write a Java object value into the payload Stream.
   *  <p>
   *  Note that this method only works for the objectified primitive
   *  object types (Integer, Double, Long ...), String's and byte arrays.
   *
   *  @param value the Java object value to write to the payload Stream.
   *
   *  @exception UnsupportedEncodingException is thrown if the payload is encoded
   *             in a codepage which is not supported on this system.
   */
  public void writeObject(Object value) throws UnsupportedEncodingException;


  /* ************************************************************************ */
  /* Miscellaneous Payload methods                                            */
  /* ************************************************************************ */

  /**
   *  Repositions the stream pointer to the beginning of the stream.
   *  This method does not set the message into 'read-only' mode as the MFP
   *  component has no knowledge of the readability/writeability of a message.
   *  The JMS API component will set and police readable/writeable state.
   */
  public void reset();

  /**
   *  Repositions the stream pointer to the position immediately before the
   *  previous read. This method allows the API component to take responsiblity
   *  for determining whether a read was for a validly typed value or not.
   *  If not, it will call the stepBack method before throwing an Exception, to
   *  meet the requirements of the JMS Specification.
   */
  public void stepBack();

  /**
   *  Return a 'User Friendly' byte array for Admin to display on a panel
   *  <p>
   *  This method is used to return a byte array in the form
   *  value|value|......|value
   *  where | is the line.separator System property.
   *
   *  @return A byte array containing a reasonably user friendly representation
   *  of the message, or null if there is no body.
   *
   *  @exception UnsupportedEncodingException is thrown if the payload is encoded
   *             in a codepage which is not supported on this system.
   */
  public byte[] getUserFriendlyBytes() throws UnsupportedEncodingException ;

}
