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
import java.util.Map;
import java.util.Collections;
import java.util.Enumeration;

import com.ibm.websphere.sib.SIApiConstants;
import com.ibm.ws.sib.mfp.*;
import com.ibm.ws.sib.mfp.schema.JmsMapBodyAccess;
import com.ibm.ws.sib.mfp.schema.JsPayloadAccess;
import com.ibm.ws.sib.utils.ras.SibTr;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;

/**
 *  JsJmsMapMessageImpl extends JsJmsMessageImpl and hence JsMessageImpl,
 *  and is the implementation class for the JsJmsMapMessage interface.
 */
final class JsJmsMapMessageImpl extends JsJmsMessageImpl implements JsJmsMapMessage {

  private final static long serialVersionUID = 1L;
  private final static byte[] flattenedClassName;

  private static TraceComponent tc = SibTr.register(JsJmsMapMessageImpl.class, MfpConstants.MSG_GROUP, MfpConstants.MSG_BUNDLE);

  /* Get the flattened form of the classname                   SIB0112b.mfp.2 */
  static {
    flattenedClassName = flattenClassName(JsJmsMapMessageImpl.class.getName());
  }

  /* **************************************************************************/
  /* Constructors                                                             */
  /* **************************************************************************/

  /**
   *  Constructor for a new Jetstream JMS MapMessage.
   *
   *  This constructor should never be used except by JsMessageImpl.createNew().
   *  The method must not actually do anything.
   */
  JsJmsMapMessageImpl() {
  }

  /**
   *  Constructor for a new Jetstream JMS MapMessage.
   *  To be called only by the JsJmsMessageFactory.
   *
   *  @param flag Flag to distinguish different construction reasons.
   *
   *  @exception MessageDecodeFailedException Thrown if such a message can not be created
   */
  JsJmsMapMessageImpl(int flag)  throws MessageDecodeFailedException {
    super(flag);
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "<init>");

    /* Set the JMS format & body information */
    setFormat(SIApiConstants.JMS_FORMAT_MAP);
    setBodyType(JmsBodyType.MAP);

    // We can skip this for an inbound MQ message as the MQJsMessageFactory will
    // replace the PAYLOAD_DATA with an MQJsApiEncapsulation.
    if (flag != MfpConstants.CONSTRUCTOR_INBOUND_MQ) {
      jmo.getPayloadPart().setPart(JsPayloadAccess.PAYLOAD_DATA, JmsMapBodyAccess.schema);
      getPayload().setChoiceField(JmsMapBodyAccess.BODY, JmsMapBodyAccess.IS_BODY_EMPTY);  // d327036.1
    }

    bodyMap = null;                                                   // d343628
  }

  /**
   *  Constructor for an inbound message.
   *  (Only to be called by a superclass make method.)
   *
   *  @param inJmo The JsMsgObject representing the inbound method.
   */
  JsJmsMapMessageImpl(JsMsgObject inJmo) {
    super(inJmo);
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "<init>, inbound jmo ");
  }


  /* ************************************************************************ */
  /* Payload methods                                                          */
  /* ************************************************************************ */

  private transient JsMsgMap bodyMap;

  /*
   *  Return the value with the given name as an Object.
   *  <p>
   *  This method is used to return in objectified format any value
   *  that has been stored in the payload with any of the setXxxx
   *  method calls. If a primitive was stored, the Object returned will be
   *  the corresponding object - e.g. an int will be returned as an Integer.
   *  Null is returned if no value with this name was set.
   *
   *  Javadoc description supplied by JsJmsMessage interface.
   */
  public Object getObject(String name) throws UnsupportedEncodingException {
    return getBodyMap().get(name);
  }


  /*
   * Clear the body (payload) of the message.
   */
  public void clearBody() {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "clearBody");
    getPayload().setChoiceField(JmsMapBodyAccess.BODY, JmsMapBodyAccess.IS_BODY_EMPTY);
    // Need to create a new empty map, otherwise the old values appear from the buffer d343628
    bodyMap = new JsMsgMap(null, null);                                             // d343628
    // We also need to mark it as 'changed'. It would be have been nice to just        d327036.1
    // call getBodyMap().clear(), but we can't if the CCSID isn't supported            d327036.1
    bodyMap.setChanged();                                                           // d327036.1
    clearCachedLengths();
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "clearBody");
  }


  /*
   * Provide an estimate of encoded length of the payload
   */
  int guessPayloadLength() {
    int length = 0;
    // Total guess at average size of map entry
    try {
      Map<String,Object> payload = getBodyMap();
      length = payload.size()*60;
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
   * data, which is a Map of Name Value pairs.
   *
   * @return int A guesstimate of the fluffed size of the payload data
   */
  int guessFluffedDataSize() {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "guessFluffedDataSize");

    int total = 0;

    // If we have a fluffed up in memory map ...
    if (bodyMap != null) {

       // Add the overhead for the map itself.
       total += FLUFFED_MAP_OVERHEAD;
       // Also add on a constant guess for each map entry.
       total += bodyMap.size() * FLUFFED_MAP_ENTRY_SIZE;
    }

    // Figure out a guesstimate without fluffing up anything unnecessarily
    else {

      // Add the estimate for the fluffed payload size
      // If the body's JMF message is already fluffed up & cached, ask it for the size.
      // Do NOT hold on to this JSMsgPart, as it could lose validity at any time.
      JsMsgPart part = getPayloadIfFluffed();
      if (part != null) {
        // .... get the estimate for the fluffed names (which is a list)
        // .... and add double that to cater for the values
        total += part.estimateFieldValueSize(JmsMapBodyAccess.BODY_DATA_ENTRY_NAME) * 2;
      }

      // If the JMF message hasn't been fluffed up, find the total assembled length of
      // the payload message if possible.
      else {
        // If we have a valid length, remove a bit & assume the rest is the encoded map.
        int payloadSize = jmo.getPayloadPart().getAssembledLengthIfKnown();
        if (payloadSize != -1) {
          int flatMapSize = payloadSize-FLATTENED_PAYLOAD_PART;
          // It is a number of entries each of which will consist of a name+value.
          // They are probably mostly Strings. Let's say 15 chars for each name or value.
          int numEnts = flatMapSize / 30;
          if (numEnts > 0) {
            // Now we've decided how many entries we tink we might have,
            // calculate the likely fluffed size.
            total += FLUFFED_MAP_OVERHEAD;
            total += numEnts * FLUFFED_MAP_ENTRY_SIZE;
          }
        }
        // If the payloadSize == -1, then the body message must have been fluffed up
        // but not yet cached, so we'll locate & cache it now.
        else {
          // Add the overhead for a fluffed up map
          total += FLUFFED_MAP_OVERHEAD;
          // .... get the estimate for the fluffed names (which is a list)
          // .... and add double that to cater for the values
          total += getPayload().estimateFieldValueSize(JmsMapBodyAccess.BODY_DATA_ENTRY_NAME) * 2;
        }
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "guessFluffedDataSize",  total);
    return total;
  }


  // Convenience method to get the payload as a JmsMapBodySchema
  JsMsgPart getPayload() {
    return getPayload(JmsMapBodyAccess.schema);
  }

  Map<String,Object> getBodyMap() throws UnsupportedEncodingException {
    if (bodyMap == null) {
      try {
        List<String> keys = (List<String>)getPayload().getField(JmsMapBodyAccess.BODY_DATA_ENTRY_NAME);
        List<Object> values = (List<Object>)getPayload().getField(JmsMapBodyAccess.BODY_DATA_ENTRY_VALUE);
        bodyMap = new JsMsgMap(keys, values);
      }
      catch (MFPUnsupportedEncodingRuntimeException e) {
        FFDCFilter.processException(e, "com.ibm.ws.sib.mfp.impl.JsJmsMapMessageImpl.getBodyMap", "152");
        throw (UnsupportedEncodingException)e.getCause();
      }
    }
    return bodyMap;
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
    if (bodyMap != null && bodyMap.isChanged()) {
      getPayload().setField(JmsMapBodyAccess.BODY_DATA_ENTRY_NAME, bodyMap.getKeyList());
      getPayload().setField(JmsMapBodyAccess.BODY_DATA_ENTRY_VALUE, bodyMap.getValueList());
      bodyMap.setUnChanged();                                                   // d317373.1
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "updateDataFields");
  }

  /* ************************************************************************ */
  /* Payload Set methods                                                      */
  /* ************************************************************************ */

  /*
   *  Set a boolean value with the given name, into the Map.
   *
   *  Javadoc description supplied by JsJmsMessage interface.
   */
  public void  setBoolean(String name, boolean value) throws UnsupportedEncodingException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "setBoolean", Boolean.valueOf(value));
    getBodyMap().put(name, Boolean.valueOf(value));
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "setBoolean");
  }


  /*
   *  Set a byte value with the given name, into the Map.
   *
   *
   *  Javadoc description supplied by JsJmsMessage interface.
   */
  public void  setByte(String name, byte value) throws UnsupportedEncodingException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "setByte", Byte.valueOf(value));
    getBodyMap().put(name, Byte.valueOf(value));
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "setByte");
  }


  /*
   *  Set a short value with the given name, into the Map.
   *
   *  Javadoc description supplied by JsJmsMessage interface.
   */
  public void setShort(String name, short value) throws UnsupportedEncodingException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "setShort", Short.valueOf(value));
    getBodyMap().put(name, Short.valueOf(value));
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "setShort");
  }


  /*
   *  Set a Unicode character value with the given name, into the Map.
   *
   *  Javadoc description supplied by JsJmsMessage interface.
   */
  public void setChar(String name, char value) throws UnsupportedEncodingException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "setChar", Character.valueOf(value));
    getBodyMap().put(name, Character.valueOf(value));
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "setChar");
  }


  /*
   *  Set an integer value with the given name, into the Map.
   *
   *  Javadoc description supplied by JsJmsMessage interface.
   */
  public void setInt(String name, int value) throws UnsupportedEncodingException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "setInt", Integer.valueOf(value));
    getBodyMap().put(name, Integer.valueOf(value));
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "setInt");
  }


  /*
   *  Set a long value with the given name, into the Map.
   *
   *  Javadoc description supplied by JsJmsMessage interface.
   */
  public void setLong(String name, long value) throws UnsupportedEncodingException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "setLong", Long.valueOf(value));
    getBodyMap().put(name, Long.valueOf(value));
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "setLong");
  }


  /*
   *  Set a float value with the given name, into the Map.
   *
   *  Javadoc description supplied by JsJmsMessage interface.
   */
  public void setFloat(String name, float value) throws UnsupportedEncodingException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "setFloat", new Float(value));
    getBodyMap().put(name, new Float(value));
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "setFloat");
  }


  /*
   *  Set a double value with the given name, into the Map.
   *
   *  Javadoc description supplied by JsJmsMessage interface.
   */
  public void setDouble(String name, double value) throws UnsupportedEncodingException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "setDouble", new Double(value));
    getBodyMap().put(name, new Double(value));
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "setDouble");
  }


  /*
   *  Set a String value with the given name, into the Map.
   *
   *  Javadoc description supplied by JsJmsMessage interface.
   */
  public void setString(String name, String value) throws UnsupportedEncodingException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "setString", value);
    getBodyMap().put(name, value);
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "setString");
  }


  /*
   *  Set a portion of the byte array value with the given name, into the Map.
   *
   *  Javadoc description supplied by JsJmsMessage interface.
   *
   *  As a byte array is not immutable, a copy must be stored rather than the original.
   *  However, any copying required is performed by the calling component and
   *  not the MFP component.
   */
  public void setBytes(String name, byte[] value) throws UnsupportedEncodingException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "setBytes", value);
    getBodyMap().put(name, value);
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "setBytes");
  }


  /*
   *  Set a Java object value with the given name, into the Map.
   *
   *  Javadoc description supplied by JsJmsMessage interface.
   *
   *  The only Objects permitted for the value are Strings and the primitive
   *  wrappers, both of which are immutable, plus bytes arrays. Therefore only
   *  a byte array need be copied before storing.
   *  However, any copying required is performed by the calling component and
   *  not the MFP component.
   */
  public void setObject(String name, Object value) throws UnsupportedEncodingException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "setObject", value);
    getBodyMap().put(name, value);
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "setObject");
  }


  /* ************************************************************************ */
  /* Miscellaneous Payload methods                                            */
  /* ************************************************************************ */

  /*
   *  Return an Enumeration of all the Map message's names.
   *
   *  Javadoc description supplied by JsJmsMessage interface.
   */
  public Enumeration<String> getMapNames() throws UnsupportedEncodingException {
    return Collections.enumeration(getBodyMap().keySet());
  }


  /*
   *  Check if an item exists in this MapMessage.
   */
  public boolean itemExists(String name) throws UnsupportedEncodingException {
    return getBodyMap().containsKey(name);
  }


  /*
   *  Return a 'User Friendly' byte array for Admin to display on a panel
   *  <p>
   *  This method is used to return a byte array in the form
   *  name=value|name=value|......|name=value
   *  where | is the line.separator System property.
   *
   *  Javadoc description supplied by the JsJmsMapMessage interface.
   */
  public byte[] getUserFriendlyBytes() throws UnsupportedEncodingException {

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getUserFriendlyBytes");

    byte[] result = null;

    /* Only need to do anything if there are items in the map */
    Enumeration<String> names = getMapNames();
    if (names.hasMoreElements()) {

      String name;
      Object value;
      StringBuilder buff = new StringBuilder();
      String lineSeparator = System.lineSeparator();

      /* Get the first name=value pair and append it to the buffer */
      name = names.nextElement();
      buff.append(name);
      buff.append("=");
      value = getObject(name);

      if (value != null) {
        if (!(value instanceof byte[])) {
          buff.append(value.toString());
        }
        else {
          buff.append(new String((byte[])value));
        }
      }

      /* For all subsequent name=values append a separator first */
      while (names.hasMoreElements()) {
        buff.append(lineSeparator);
        name = names.nextElement();
        buff.append(name);
        buff.append("=");
        value = getObject(name);
        if (value != null) {
          if (!(value instanceof byte[])) {
            buff.append(value.toString());
          }
          else {
            buff.append(new String((byte[])value));
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
