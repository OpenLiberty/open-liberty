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
import java.util.Arrays;
import java.util.Enumeration;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.MessageFormatException;

import com.ibm.websphere.sib.api.jms.ApiJmsConstants;
import com.ibm.ws.sib.mfp.JsJmsMapMessage;
import com.ibm.ws.sib.mfp.JsJmsMessage;
import com.ibm.ws.sib.mfp.MessageCreateFailedException;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.websphere.ras.TraceComponent;


/**
 * @author matrober
 *
 * This class implements the requirements of a JMS MapMessage
 */
public class JmsMapMessageImpl extends JmsMessageImpl implements MapMessage
{

  /**
   * This svUID assigned at version 1.30
   */
  private static final long serialVersionUID = -5270211659893353729L;

  // ******************* PRIVATE STATE VARIABLES *******************

  /**
   * MFP message object representing a JMS TextMessage
   * Note: Do not initialise this to null here, otherwise it will overwrite
   *       the setup done by instantiateMessage!
   */
  private JsJmsMapMessage mapMsg;

  /**
   * This variable holds a cache of the message toString at the Message level.
   * A separate cache holds the subclass information. The cache is invalidated
   * by changing any property of the message.
   */
  private transient String cachedMapToString = null;

  // *************************** TRACE INITIALIZATION **************************
  private static TraceComponent tc = SibTr.register(JmsMapMessageImpl.class, ApiJmsConstants.MSG_GROUP_EXT, ApiJmsConstants.MSG_BUNDLE_EXT);

  // ************************ CONSTRUCTORS *************************

  /**
   * Constructor used to create a new message object.
   */
  public JmsMapMessageImpl() throws JMSException {
    // Calling the superclass no-args constructor in turn leads to the
    // instantiateMessage method being called, which we override to return
    // a map message.
    super();
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "JmsMapMessageImpl");
    messageClass = CLASS_MAP;
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "JmsMapMessageImpl");
  }


  /**
   * This constructor is used by the JmsMessage.inboundJmsInstance method (static)
   * in order to provide the inbound message path from MFP component to JMS component.
   */
  JmsMapMessageImpl(JsJmsMapMessage newMsg, JmsSessionImpl newSess) {

    // Pass this object to the parent class so that it can keep a reference.
    super(newMsg, newSess);
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "JmsMapMessageImpl", new Object[]{newMsg, newSess});

    // Store the reference we are given, and inform the parent class.
    mapMsg = newMsg;
    messageClass = CLASS_MAP;

    // Note that we do NOT initialize the defaults for inbound messages.

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "JmsMapMessageImpl");
  }


  /**
   * Construct a jetstream jms message from a (possibly non-jetstream)
   * vanilla jms message.
   */
  JmsMapMessageImpl(MapMessage mapMessage) throws JMSException {
    // copy message headers and properties.
    super(mapMessage);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "JmsMapMessageImpl", mapMessage);

    // set-up this class's state (i.e. do what this() does).
    // nothing to do.

    // copy map.
    Enumeration mapNames = mapMessage.getMapNames();
    if (mapNames != null) {
      while (mapNames.hasMoreElements()) {
        String name = (String) mapNames.nextElement();
        Object value = mapMessage.getObject(name);
        setObject(name, value);
      }
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "JmsMapMessageImpl");
  }


  // ********************* INTERFACE METHODS ***********************

  /**
   * @see javax.jms.MapMessage#getBoolean(String)
   */
  public boolean getBoolean(String name) throws JMSException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getBoolean", name);

    Object obj=null;
    try {
      obj = mapMsg.getObject(name);
    }
    catch (UnsupportedEncodingException e) {
      // No FFDC Code Needed
      throw (JMSException) JmsErrorUtils.newThrowable(
        JMSException.class,
        "EXCEPTION_RECEIVED_CWSIA0022",
        new Object[] {e, "JmsMapMessageImpl.getBoolean"},
        e, "JmsMapMessageImpl#1", this, tc);
    }

    boolean value = false;
    try {
      value = JmsMessageImpl.parseBoolean(obj, name);
    }
    catch (JMSException e) {
      // No FFDC code needed
      // d238447 FFDC review. Don't call processThrowable - this is an app' error.
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Error parsing object", e);
      throw e;
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getBoolean",  value);
    return value;
  }

  /**
   * @see javax.jms.MapMessage#getByte(String)
   */
  public byte getByte(String name) throws JMSException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getByte", name);

    Object obj=null;
    try {
      obj = mapMsg.getObject(name);
    }
    catch (UnsupportedEncodingException e) {
      // No FFDC Code Needed
      throw (JMSException) JmsErrorUtils.newThrowable(
        JMSException.class,
        "EXCEPTION_RECEIVED_CWSIA0022",
        new Object[] {e, "JmsMapMessageImpl.getByte"},
        e, "JmsMapMessageImpl#2", this, tc);
    }

    byte value = 0;
    try {
      value = JmsMessageImpl.parseByte(obj, name);
    }
    catch (JMSException e) {
      // No FFDC code needed
      // 238447 don't call processThrowable for app' errors
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Error parsing object", e);
      throw e;
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getByte",  value);
    return value;
  }

  /**
   * @see javax.jms.MapMessage#getShort(String)
   */
  public short getShort(String name) throws JMSException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getShort", name);

    Object obj=null;
    try {
      obj = mapMsg.getObject(name);
    }
    catch (UnsupportedEncodingException e) {
      // No FFDC Code Needed
      throw (JMSException) JmsErrorUtils.newThrowable(
        JMSException.class,
        "EXCEPTION_RECEIVED_CWSIA0022",
        new Object[] {e, "JmsMapMessageImpl.getShort"},
        e, "JmsMapMessageImpl#3", this, tc);
    }

    short value = 0;
    try {
      value = JmsMessageImpl.parseShort(obj, name);
    }
    catch (JMSException e) {
      // No FFDC code needed
      // 238447 Don't generate FFDCs for app' errors.
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Error parsing object", e);
      throw e;
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getShort",  value);
    return value;
  }

  /**
   * @see javax.jms.MapMessage#getChar(String)
   */
  public char getChar(String name) throws JMSException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getChar", name);

    Object obj=null;
    try {
      obj = mapMsg.getObject(name);
    }
    catch (UnsupportedEncodingException e) {
      // No FFDC Code Needed
      throw (JMSException) JmsErrorUtils.newThrowable(
          JMSException.class,
          "EXCEPTION_RECEIVED_CWSIA0022",
          new Object[] {e, "JmsMapMessageImpl.getChar"},
          e, "JmsMapMessageImpl#4", this, tc);
    }

    char value = 0;

    // This logic is not part of a Message.parseCharacter because there is no
    // equivalent Message.getCharProperty method which would share it.
    if (obj instanceof Character) {
      value = ((Character)obj).charValue();
    }
    else if (obj == null) {
      RuntimeException e = (RuntimeException)JmsErrorUtils.newThrowable(NullPointerException.class,
            "FIELD_NOT_SET_CWSIA0105", new Object[] { name }, tc);
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Field has not been set", e);
      throw e;
    }
    else {
      // Boolean can only be retrieved as boolean and String (see 3.5.4).
      JMSException e = newBadConvertException(obj, name, "Character", tc);
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Invalid convert", e);
      throw e;
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getChar",  value);
    return value;
  }

  /**
   * @see javax.jms.MapMessage#getInt(String)
   */
  public int getInt(String name) throws JMSException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getInt", name);

    Object obj=null;
    try {
      obj = mapMsg.getObject(name);
    }
    catch (UnsupportedEncodingException e) {
      // No FFDC Code Needed
      throw (JMSException) JmsErrorUtils.newThrowable(
          JMSException.class,
          "EXCEPTION_RECEIVED_CWSIA0022",
          new Object[] {e, "JmsMapMessageImpl.getInt"},
          e, "JmsMapMessageImpl#5", this, tc);
    }

    int value = 0;
    try {
      value = JmsMessageImpl.parseInt(obj, name);
    }
    catch (JMSException e) {
      // No FFDC code needed
      // 238447 Don't generate FFDCs for app' errors.
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Error parsing object", e);
      throw e;
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getInt",  value);
    return value;
  }

  /**
   * @see javax.jms.MapMessage#getLong(String)
   */
  public long getLong(String name) throws JMSException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getLong", name);

    Object obj=null;
    try {
      obj = mapMsg.getObject(name);
    }
    catch (UnsupportedEncodingException e) {
      // No FFDC Code Needed
      throw (JMSException) JmsErrorUtils.newThrowable(
          JMSException.class,
          "EXCEPTION_RECEIVED_CWSIA0022",
          new Object[] {e, "JmsMapMessageImpl.getLong"},
          e, "JmsMapMessageImpl#6", this, tc);
    }

    long value = 0;
    try {
      value = JmsMessageImpl.parseLong(obj, name);
    }
    catch (JMSException e) {
      // No FFDC code needed
      // 238447 Don't generate FFDCs for app' errors.
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Error parsing object", e);
      throw e;
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getLong",  value);
    return value;
  }

  /**
   * @see javax.jms.MapMessage#getFloat(String)
   */
  public float getFloat(String name) throws JMSException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getFloat", name);

    Object obj=null;
    try {
      obj = mapMsg.getObject(name);
    }
    catch (UnsupportedEncodingException e) {
      // No FFDC Code Needed
      throw (JMSException) JmsErrorUtils.newThrowable(
          JMSException.class,
          "EXCEPTION_RECEIVED_CWSIA0022",
          new Object[] {e, "JmsMapMessageImpl.getFloat"},
          e, "JmsMapMessageImpl#7", this, tc);
    }

    float value = 0;

    try {
      value = JmsMessageImpl.parseFloat(obj, name);
    }
    catch (JMSException e) {
      // No FFDC code needed
      // 238447 Don't generate FFDCs for app' errors.
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Error parsing object", e);
      throw e;
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getFloat",  value);
    return value;
  }

  /**
   * @see javax.jms.MapMessage#getDouble(String)
   */
  public double getDouble(String name) throws JMSException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getDouble", name);

    Object obj=null;
    try {
      obj = mapMsg.getObject(name);
    } catch (UnsupportedEncodingException e) {
      // No FFDC Code Needed
      throw (JMSException) JmsErrorUtils.newThrowable(
          JMSException.class,
          "EXCEPTION_RECEIVED_CWSIA0022",
          new Object[] {e, "JmsMapMessageImpl.getDouble"},
          e, "JmsMapMessageImpl#8", this, tc);
    }

    double value = 0;
    try {
      value = JmsMessageImpl.parseDouble(obj, name);
    }
    catch (JMSException e) {
      // No FFDC code needed
      // 238447 Don't generate FFDCs for app' errors.
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Error parsing object", e);
      throw e;
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getDouble",  value);
    return value;
  }

  /**
   * @see javax.jms.MapMessage#getString(String)
   */
  public String getString(String name) throws JMSException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getString", name);

    Object obj=null;
    try {
      obj = mapMsg.getObject(name);
    }
    catch (UnsupportedEncodingException e) {
      // No FFDC Code Needed
      throw (JMSException) JmsErrorUtils.newThrowable(
          JMSException.class,
          "EXCEPTION_RECEIVED_CWSIA0022",
          new Object[] {e, "JmsMapMessageImpl.getString"},
          e, "JmsMapMessageImpl#9", this, tc);
    }

    String value = null;

    if ((obj instanceof String) || (obj == null)) {
      value = (String)obj;
    }
    else if (obj instanceof byte[]) {
      JMSException e = newBadConvertException(obj, name, "String", tc);
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "invalid convert", e);
      throw e;
    }
    else {
      // Every type of property can be returned as a String
      value = obj.toString();
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getString",  value);
    return value;
  }

  /**
   * @see javax.jms.MapMessage#getBytes(String)
   */
  public byte[] getBytes(String name) throws JMSException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getBytes", name);

    Object obj=null;
    try {
      obj = mapMsg.getObject(name);
    }
    catch (UnsupportedEncodingException e) {
      // No FFDC Code Needed
      throw (JMSException) JmsErrorUtils.newThrowable(
          JMSException.class,
          "EXCEPTION_RECEIVED_CWSIA0022",
          new Object[] {e, "JmsMapMessageImpl.getBytes"},
          e, "JmsMapMessageImpl#10", this, tc);
    }

    byte[] value = null;
    if (obj instanceof byte[]) {
      // Take a copy to prevent an application from altering the data in the message
      byte[] objB = (byte[])obj;
      value = new byte[objB.length];
      System.arraycopy(objB, 0, value, 0, objB.length);
    }
    else if (obj == null) {
      // This property had not been set. The Javadoc for getBytes says we return null
      // if there is no item by this name. Note that this is compatible with applications
      // having set the bytes to null explicitly.
      value = null;
    }
    else {
      JMSException e = newBadConvertException(obj, name, "Byte[]", tc);
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Invalid convert", e);
      throw e;
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getBytes",  Arrays.toString(value));
    return value;
  }

  /**
   * @see javax.jms.MapMessage#getObject(String)
   */
  public Object getObject(String name) throws JMSException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getObject", name);

    Object result = null;
    Object obj = null;
    try {
      obj = mapMsg.getObject(name);
    }
    catch (UnsupportedEncodingException e) {
      // No FFDC Code Needed
      throw (JMSException) JmsErrorUtils.newThrowable(
          JMSException.class,
          "EXCEPTION_RECEIVED_CWSIA0022",
          new Object[] {e, "JmsMapMessageImpl.getObject"},
          e, "JmsMapMessageImpl#11", this, tc);
    }
    if (obj instanceof byte[]) {
      // Take a copy to prevent an application from altering the data in the message
      byte[] objB = (byte[])obj;
      byte[] value = new byte[objB.length];
      System.arraycopy(objB, 0, value, 0, objB.length);
      result = value;
    }
    else {
      result = obj;
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getObject",  result);
    return result;
  }

  /**
   * @see javax.jms.MapMessage#getMapNames()
   */
  public Enumeration getMapNames() throws JMSException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getMapNames");

    Enumeration map=null;
    try {
      map = mapMsg.getMapNames();
    } catch (UnsupportedEncodingException e) {
      // No FFDC Code Needed
      throw (JMSException) JmsErrorUtils.newThrowable(
          JMSException.class,
          "EXCEPTION_RECEIVED_CWSIA0022",
          new Object[] {e, "JmsMapMessageImpl.getMapNames"},
          e, "JmsMapMessageImpl#12", this, tc);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getMapNames",  map);
    return map;
  }

  /**
   * @see javax.jms.MapMessage#setBoolean(String, boolean)
   */
  public void setBoolean(String name, boolean value) throws JMSException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "setBoolean", new Object[]{name, value});

    checkBodyWriteable("setBoolean");
    checkPropName(name, "setBoolean");
    try {
      mapMsg.setBoolean(name, value);
    }
    catch (UnsupportedEncodingException e) {
      // No FFDC Code Needed
      throw (JMSException) JmsErrorUtils.newThrowable(
        JMSException.class,
        "EXCEPTION_RECEIVED_CWSIA0022",
        new Object[] {e, "JmsMapMessageImpl.setBoolean"},
        e, "JmsMapMessageImpl#13", this, tc);
    }
    // Invalidate the cached toString object.
    cachedMapToString = null;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "setBoolean");
  }

  /**
   * @see javax.jms.MapMessage#setByte(String, byte)
   */
  public void setByte(String name, byte value) throws JMSException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "setByte", new Object[]{name, value});

    checkBodyWriteable("setByte");
    checkPropName(name, "setByte");
    try {
      mapMsg.setByte(name, value);
    }
    catch (UnsupportedEncodingException e) {
      // No FFDC Code Needed
      throw (JMSException) JmsErrorUtils.newThrowable(
          JMSException.class,
          "EXCEPTION_RECEIVED_CWSIA0022",
          new Object[] {e, "JmsMapMessageImpl.setByte"},
          e, "JmsMapMessageImpl#14", this, tc);
    }
    // Invalidate the cached toString object.
    cachedMapToString = null;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "setByte");
  }

  /**
   * @see javax.jms.MapMessage#setBytes(String, byte[])
   */
  public void setBytes(String name, byte[] value) throws JMSException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "setBytes", new Object[]{name, value});

    int length = 0;
    if (value != null) length = value.length;
    setBytes(name, value, 0, length);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "setBytes");
  }

  /**
   * @see javax.jms.MapMessage#setBytes(String, byte[], int, int)
   */
  public void setBytes(String name, byte[] value, int start, int length) throws JMSException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "setBytes", new Object[]{name, value, start, length});

    checkBodyWriteable("setBytes");
    checkPropName(name, "setBytes");

    // carry out the deep copy & defend against nulls
    // (should be able to set a null without any problems).
    byte[] deepCopy = null;
    if (value != null) {
      deepCopy = new byte[length];
      System.arraycopy(value, start, deepCopy, 0, length);
    }

    try {
       // pass the copy of the array in to the message object.
       mapMsg.setBytes(name, deepCopy);
    }
    catch (UnsupportedEncodingException e) {
      // No FFDC Code Needed
      throw (JMSException) JmsErrorUtils.newThrowable(
          JMSException.class,
          "EXCEPTION_RECEIVED_CWSIA0022",
          new Object[] {e, "JmsMapMessageImpl.setBytes"},
          e, "JmsMapMessageImpl#15", this, tc);
    }

    // Invalidate the cached toString object.
    cachedMapToString = null;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "setBytes");
  }

  /**
   * @see javax.jms.MapMessage#setChar(String, char)
   */
  public void setChar(String name, char value) throws JMSException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "setChar", new Object[]{name, value});

    checkBodyWriteable("setChar");
    checkPropName(name, "setChar");
    try {
      mapMsg.setChar(name, value);
    }
    catch (UnsupportedEncodingException e) {
      // No FFDC Code Needed
      throw (JMSException) JmsErrorUtils.newThrowable(
          JMSException.class,
          "EXCEPTION_RECEIVED_CWSIA0022",
          new Object[] {e, "JmsMapMessageImpl.setChar"},
          e, "JmsMapMessageImpl#16", this, tc);
    }
    // Invalidate the cached toString object.
    cachedMapToString = null;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "setChar");
  }

  /**
   * @see javax.jms.MapMessage#setDouble(String, double)
   */
  public void setDouble(String name, double value) throws JMSException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "setDouble", new Object[]{name, value});

    checkBodyWriteable("setDouble");
    checkPropName(name, "setDouble");
    try {
      mapMsg.setDouble(name, value);
    }
    catch (UnsupportedEncodingException e) {
      // No FFDC Code Needed
      throw (JMSException) JmsErrorUtils.newThrowable(
          JMSException.class,
          "EXCEPTION_RECEIVED_CWSIA0022",
          new Object[] {e, "JmsMapMessageImpl.setDouble"},
          e, "JmsMapMessageImpl#17", this, tc);
    }
    // Invalidate the cached toString object.
    cachedMapToString = null;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "setDouble");
  }

  /**
   * @see javax.jms.MapMessage#setFloat(String, float)
   */
  public void setFloat(String name, float value) throws JMSException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "setFloat", new Object[]{name, value});

    checkBodyWriteable("setFloat");
    checkPropName(name, "setFloat");
    try {
      mapMsg.setFloat(name, value);
    }
    catch (UnsupportedEncodingException e) {
      // No FFDC Code Needed
      throw (JMSException) JmsErrorUtils.newThrowable(
          JMSException.class,
          "EXCEPTION_RECEIVED_CWSIA0022",
          new Object[] {e, "JmsMapMessageImpl.setFloat"},
          e, "JmsMapMessageImpl#18", this, tc);
    }
    // Invalidate the cached toString object.
    cachedMapToString = null;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "setFloat");
  }

  /**
   * @see javax.jms.MapMessage#setInt(String, int)
   */
  public void setInt(String name, int value) throws JMSException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "setInt", new Object[]{name, value});

    checkBodyWriteable("setInt");
    checkPropName(name, "setInt");
    try {
      mapMsg.setInt(name, value);
    }
    catch (UnsupportedEncodingException e) {
      // No FFDC Code Needed
      throw (JMSException) JmsErrorUtils.newThrowable(
          JMSException.class,
          "EXCEPTION_RECEIVED_CWSIA0022",
          new Object[] {e, "JmsMapMessageImpl.setInt"},
          e, "JmsMapMessageImpl#19", this, tc);
    }
    // Invalidate the cached toString object.
    cachedMapToString = null;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "setInt");
  }

  /**
   * @see javax.jms.MapMessage#setLong(String, long)
   */
  public void setLong(String name, long value) throws JMSException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "setLong", new Object[]{name, value});

    checkBodyWriteable("setLong");
    checkPropName(name, "setLong");
    try {
      mapMsg.setLong(name, value);
    } catch (UnsupportedEncodingException e) {
      // No FFDC Code Needed
      throw (JMSException) JmsErrorUtils.newThrowable(
          JMSException.class,
          "EXCEPTION_RECEIVED_CWSIA0022",
          new Object[] {e, "JmsMapMessageImpl.setLong"},
          e, "JmsMapMessageImpl#20", this, tc);
    }
    // Invalidate the cached toString object.
    cachedMapToString = null;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "setLong");
  }

  /**
   * @see javax.jms.MapMessage#setObject(String, Object)
   */
  public void setObject(String name, Object value) throws JMSException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "setObject", new Object[]{name, value});

    checkBodyWriteable("setObject");
    checkPropName(name, "setObject");

    // check that the object is of acceptable type
    if (!(  (value == null)
         || (value instanceof String)
         || (value instanceof Number)
         || (value instanceof Boolean)
         || (value instanceof Character)
         || (value instanceof byte[])
         ) ) {

      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "value is bad type: "+value.getClass().getName());
      // d238447 FFDC review. Don't generate FFDC for this case.
      throw (JMSException) JmsErrorUtils.newThrowable(
         MessageFormatException.class,
         "BAD_OBJECT_CWSIA0188",
         new Object[] { value.getClass().getName() },
         tc);
    }

    if (value instanceof byte[]) {
      // take a copy so that the application can change the original without affecting the message.
      byte[] v = (byte[])value;
      byte[] tmp = new byte[v.length];
      System.arraycopy(v, 0, tmp, 0, v.length);
      value = tmp;
    }

    try {
      mapMsg.setObject(name, value);
    }
    catch (UnsupportedEncodingException e) {
      // No FFDC Code Needed
      throw (JMSException) JmsErrorUtils.newThrowable(
          JMSException.class,
          "EXCEPTION_RECEIVED_CWSIA0022",
          new Object[] {e, "JmsMapMessageImpl.setObject"},
          e, "JmsMapMessageImpl#21", this, tc);
    }

    // Invalidate the cached toString object.
    cachedMapToString = null;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "setObject");
  }

  /**
   * @see javax.jms.MapMessage#setShort(String, short)
   */
  public void setShort(String name, short value) throws JMSException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "setShort", new Object[]{name, value});

    checkBodyWriteable("setShort");
    checkPropName(name, "setShort");
    try {
      mapMsg.setShort(name, value);
    }
    catch (UnsupportedEncodingException e) {
      // No FFDC Code Needed
      throw (JMSException) JmsErrorUtils.newThrowable(
          JMSException.class,
          "EXCEPTION_RECEIVED_CWSIA0022",
          new Object[] {e, "JmsMapMessageImpl.setShort"},
          e, "JmsMapMessageImpl#22", this, tc);
    }
    // Invalidate the cached toString object.
    cachedMapToString = null;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "setShort");
  }

  /**
   * @see javax.jms.MapMessage#setString(String, String)
   */
  public void setString(String name, String value) throws JMSException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "setString", new Object[]{name, value});

    checkBodyWriteable("setString");
    checkPropName(name, "setString");
    try {
      mapMsg.setString(name, value);
    }
    catch (UnsupportedEncodingException e) {
      // No FFDC Code Needed
      throw (JMSException) JmsErrorUtils.newThrowable(
          JMSException.class,
          "EXCEPTION_RECEIVED_CWSIA0022",
          new Object[] {e, "JmsMapMessageImpl.setString"},
          e, "JmsMapMessageImpl#23", this, tc);
    }
    // Invalidate the cached toString object.
    cachedMapToString = null;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "setString");
  }

  /**
   * @see javax.jms.MapMessage#itemExists(String)
   */
  public boolean itemExists(String name) throws JMSException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "itemExists", name);

    boolean exists = false;
    try {
      exists = mapMsg.itemExists(name);
    }
    catch (UnsupportedEncodingException e) {
      // No FFDC Code Needed
      throw (JMSException) JmsErrorUtils.newThrowable(
          JMSException.class,
          "EXCEPTION_RECEIVED_CWSIA0022",
          new Object[] {e, "JmsMapMessageImpl.itemExists"},
          e, "JmsMapMessageImpl#24", this, tc);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "itemExists",  exists);
    return exists;
  }

  /**
   * Override the vanilla message toString to add the map data. Note that
   * we keep a cache of the map information string here, which is invalidated
   * by setting any map-specific properties.
   */
  public String toString() {

    if (cachedMapToString == null) {
      // NB. This is only a cache of the map-specific parts.
      StringBuffer sb = new StringBuffer();
      try {
        Enumeration props = getMapNames();
        while (props.hasMoreElements()) {
          String nextKey = (String)props.nextElement();
          Object propVal = getObject(nextKey);
          if (propVal == null) propVal = "<null>";
          sb.append("\n"+nextKey+" = "+propVal);
        }
      }
      catch (JMSException e) {
        // No FFDC code needed
        // Ignore this error.
      }

      cachedMapToString = sb.toString();
    }

    // We append this to the string from the parent class here. Note that
    // we don't cache the whole string, because the parent values may have
    // changed and we would never know about it.
    return super.toString() + cachedMapToString;
  }

  // ******************** IMPLEMENTATION METHODS **********************

  /**
   * @see com.ibm.ws.sib.api.jms.impl.JmsMessageImpl#instantiateMessage()
   */
  protected JsJmsMessage instantiateMessage() throws Exception {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "instantiateMessage");

    // Create a new message object.
    JsJmsMapMessage newMsg = null;
    try {
      newMsg = jmfact.createJmsMapMessage();
    }
    catch(MessageCreateFailedException e) {
      // No FFDC code needed
      // 238447 Don't call processThrowable - JmsMessageImpl will use newThrowable to generate FFDC.
      throw e;
    }

    // Do any other reference storing here (for subclasses)
    mapMsg = newMsg;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "instantiateMessage",  newMsg);
    return newMsg;
  }
}
