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
import java.util.Enumeration;

/**
 *  JsJmsMapMessage extends JsJmsMessage and adds the get/set methods specific
 *  to a JMS MapMessage.
 */
public interface JsJmsMapMessage extends JsJmsMessage {

  /* ************************************************************************ */
  /* Payload Get method                                                       */
  /* ************************************************************************ */

  /**
   *  Return the value with the given name as an Object.
   *  <p>
   *  This method is used to return in objectified format any value
   *  that has been stored in the payload with any of the setXxxx
   *  method calls. If a primitive was stored, the Object returned will be
   *  the corresponding object - e.g. an int will be returned as an Integer.
   *
   *  @param name  The name of the JMS property
   *
   *  @return An object representing the payload with the given name.
   *  Null is returned if no value with this name was set.
   *
   *  @exception UnsupportedEncodingException is thrown if the payload is encoded
   *             in a codepage which is not supported on this system.
   */
  public Object getObject(String name) throws UnsupportedEncodingException;

  /* ************************************************************************ */
  /* Payload Set methods                                                      */
  /* ************************************************************************ */

  /**
   *  Set a boolean value with the given name, into the Map.
   *
   *  @param name the name of the boolean
   *  @param value the boolean value to set in the Map.
   *
   *  @exception UnsupportedEncodingException is thrown if the payload is encoded
   *             in a codepage which is not supported on this system.
   */
  public void  setBoolean(String name, boolean value) throws UnsupportedEncodingException;

  /**
   *  Set a byte value with the given name, into the Map.
   *
   *  @param name the name of the byte
   *  @param value the byte value to set in the Map.
   *
   *  @exception UnsupportedEncodingException is thrown if the payload is encoded
   *             in a codepage which is not supported on this system.
   */
  public void  setByte(String name, byte value) throws UnsupportedEncodingException;

  /**
   *  Set a short value with the given name, into the Map.
   *
   *  @param name the name of the short
   *  @param value the short value to set in the Map.
   *
   *  @exception UnsupportedEncodingException is thrown if the payload is encoded
   *             in a codepage which is not supported on this system.
   */
  public void setShort(String name, short value) throws UnsupportedEncodingException;

  /**
   *  Set a Unicode character value with the given name, into the Map.
   *
   *  @param name the name of the Unicode character
   *  @param value the Unicode character value to set in the Map.
   *
   *  @exception UnsupportedEncodingException is thrown if the payload is encoded
   *             in a codepage which is not supported on this system.
   */
  public void setChar(String name, char value) throws UnsupportedEncodingException;

  /**
   *  Set an integer value with the given name, into the Map.
   *
   *  @param name the name of the integer
   *  @param value the integer value to set in the Map.
   *
   *  @exception UnsupportedEncodingException is thrown if the payload is encoded
   *             in a codepage which is not supported on this system.
   */
  public void setInt(String name, int value) throws UnsupportedEncodingException;

  /**
   *  Set a long value with the given name, into the Map.
   *
   *  @param name the name of the long
   *  @param value the long value to set in the Map.
   *
   *  @exception UnsupportedEncodingException is thrown if the payload is encoded
   *             in a codepage which is not supported on this system.
   */
  public void setLong(String name, long value) throws UnsupportedEncodingException;

  /**
   *  Set a float value with the given name, into the Map.
   *
   *  @param name the name of the float
   *  @param value the float value to set in the Map.
   *
   *  @exception UnsupportedEncodingException is thrown if the payload is encoded
   *             in a codepage which is not supported on this system.
   */
  public void setFloat(String name, float value) throws UnsupportedEncodingException;

  /**
   *  Set a double value with the given name, into the Map.
   *
   *  @param name the name of the double
   *  @param value the double value to set in the Map.
   *
   *  @exception UnsupportedEncodingException is thrown if the payload is encoded
   *             in a codepage which is not supported on this system.
   */
  public void setDouble(String name, double value) throws UnsupportedEncodingException;

  /**
   *  Set a String value with the given name, into the Map.
   *
   *  @param name the name of the String
   *  @param value the String value to set in the Map.
   *
   *  @exception UnsupportedEncodingException is thrown if the payload is encoded
   *             in a codepage which is not supported on this system.
   */
  public void setString(String name, String value) throws UnsupportedEncodingException;

  /**
   *  Set a portion of the byte array value with the given name, into the Map.
   *
   *  @param name the name of the byte array
   *  @param value the byte array value to set in the Map.
   *
   *  @exception UnsupportedEncodingException is thrown if the payload is encoded
   *             in a codepage which is not supported on this system.
   */
  public void setBytes(String name, byte[] value) throws UnsupportedEncodingException;

  /**
   *  Set a Java object value with the given name, into the Map.
   *  <p>
   *  Note that this method only works for the objectified primitive
   *  object types (Integer, Double, Long ...), String's and byte arrays.
   *
   *  @param name the name of the Java object
   *  @param value the Java object value to set in the Map.
   *
   *  @exception UnsupportedEncodingException is thrown if the payload is encoded
   *             in a codepage which is not supported on this system.
   */
  public void setObject(String name, Object value) throws UnsupportedEncodingException;


  /* ************************************************************************ */
  /* Miscellaneous Payload methods                                            */
  /* ************************************************************************ */

  /**
   *  Return an Enumeration of all the Map message's names.
   *
   *  @return an enumeration of all the names in this Map message.
   *
   *  @exception UnsupportedEncodingException is thrown if the payload is encoded
   *             in a codepage which is not supported on this system.
   */
  public Enumeration<String> getMapNames() throws UnsupportedEncodingException;

  /**
   *  Check if an item exists in this MapMessage.
   *
   *  @param name the name of the item to test
   *
   *  @return true if the item does exist.
   *
   *  @exception UnsupportedEncodingException is thrown if the payload is encoded
   *             in a codepage which is not supported on this system.
   */
  public boolean itemExists(String name) throws UnsupportedEncodingException;

  /**
   *  Return a 'User Friendly' byte array for Admin to display on a panel
   *  <p>
   *  This method is used to return a byte array in the form
   *  name=value|name=value|......|name=value
   *  where | is the line.separator System property.
   *
   *  @return A byte array containing a reasonably user friendly representation
   *  of the message, or null if there is no body.
   *
   *  @exception UnsupportedEncodingException is thrown if the payload is encoded
   *             in a codepage which is not supported on this system.
   */
  public byte[] getUserFriendlyBytes() throws UnsupportedEncodingException;

}
