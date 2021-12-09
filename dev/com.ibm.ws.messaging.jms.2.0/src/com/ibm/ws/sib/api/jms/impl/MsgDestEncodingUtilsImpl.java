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
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Topic;

import com.ibm.websphere.sib.api.jms.ApiJmsConstants;
import com.ibm.websphere.sib.api.jms.JmsDestination;
import com.ibm.websphere.sib.api.jms.JmsQueue;
import com.ibm.websphere.sib.api.jms.JmsTopic;
import com.ibm.ws.sib.api.jms.EncodingLevel;
import com.ibm.ws.sib.api.jms.JmsDestInternals;
import com.ibm.ws.sib.api.jms.JmsInternalConstants;
import com.ibm.ws.sib.api.jms.MessageDestEncodingUtils;
import com.ibm.ws.sib.jms.util.ArrayUtil;
import com.ibm.ws.sib.jms.util.Utf8Codec;
import com.ibm.ws.sib.utils.ras.SibTr;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;

/**
 * This class provides an implementation of the MessageDestEncodingUtils
 * interface, that is used to serialize JmsDestination objects into a byte[]
 * for transmission in the message.
 *
 *******************************************************************************
 *
 * The specification for the encoded form appears to be:
 *      byte 0:  0xXY where:
 *                      X: 0 for queue
 *                         1 for topic
 *                      Y: 0 for DM = None/App
 *                         1 for DM = Pers
 *                         2 for DM = NonPers
 *
 *      byte 1:  0xXY where:
 *                      X: f for Priority=None
 *                         0-9 for real priority values
 *                      Y: 0 for TTL=none
 *                         1 for TTL set
 *
 *     If TTL is set, the next 8 bytes contain the TTL encoded in big-endian.
 *
 *     For each remaining supported 'shortName' property:
 *
 *       bytes N & N+1          :  2 character shortname of property encoded to UTF8
 *       bytes N+2 & N+3        :  short length of property value in big-endian
 *       bytes N+4 - N+3+length :  String property value encoded to UTF8
 *
 *    The short names currently supported are:  ra, tn, ts, qn, qp, pl, pb, gm
 *    See the initializePropertyMaps() method for the mappings.
 *
 *    Any properties without a 'shortName' are encoded very long-windedly as follows:
 *
 *       bytes N                     :  '*'
 *       bytes N+1 & N+2             :  short length of property name in big-endian  (=x)
 *       bytes N+3 - N+3+namelength  :  String property name encoded to UTF8
 *       bytes N+x & N+x+1           :  short length of stringified property value in big-endian
 *       bytes N+x+2 - N+x+length    :  String property value encoded to UTF8
 *
 *    A "Partial encode" omits Priority, TTL, Topic/Queue name, Topicspace & BusName
 *
 *    Any new property should be added with a 'shortName' as it is more efficient
 *    for both encoding/decoding and message size. The properties without 'shortNames'
 *    is inherited from a previous release where it was presumably forgotten about!
 *
 *******************************************************************************
 */
public class MsgDestEncodingUtilsImpl implements MessageDestEncodingUtils
{
  private static TraceComponent tc = SibTr.register(MsgDestEncodingUtilsImpl.class, ApiJmsConstants.MSG_GROUP_INT, ApiJmsConstants.MSG_BUNDLE_INT);
 
  // ******************************* CONSTANTS *********************************

  // The following constants are used for the bit-mask parts of the encoding (first two bytes).
  // Queue or Topic
  private static final byte QUEUE_TYPE = 0x00; // 0000 0000
  private static final byte TOPIC_TYPE = 0x10; // 0001 0000
  // Delivery Mode
  private static final byte DM_NOT_SET        = 0x00; // 0000 0000
  private static final byte DM_PERSISTENT     = 0x01; // 0000 0001
  private static final byte DM_NON_PERSISTENT = 0x02; // 0000 0010
  private static final byte DM_MASK           = 0x0f; // 0000 1111
  // The priority field can be obtained by taking the byte form
  // of priority (0-9) and left shifting it into the high nibble.
  private static final byte PRIORITY_NOT_SET = (byte)0xF0; // 1111 0000
  private static final byte PRIORITY_MASK    = (byte)0xF0; // 1111 0000
  // TimeToLive
  private static final byte TTL_NOT_SET    = 0x00; // 0000 0000
  private static final byte TTL_SET        = 0x01; // 0000 0001

  // The Primary Map for all the property encoding information.
  // This contains one entry per supported property & is keyed by the 'long'
  // property name.
  // This map is keyed by the 'long' property name.
  private static final Map<String,PropertyEntry> propertyMap = new HashMap<String,PropertyEntry>();

  // The Reverse Map, which maps short names to lomg names.
  private static final Map<String,String> reverseMap = new HashMap<String,String>();

  // This table maps between classes that extend JmsDestinationImpl (e.g. JmsQueueImpl
  // and JmsTopicImpl and a map of the names of properties and their default
  // values that need to omitted when objects of the said class are saved
  // in JNDI (and inserted when retrieving from JNDI if it is missing)
  private static Map<Class<? extends JmsDestinationImpl>,Map<String,Object>> defaultJNDIProperties = new HashMap<Class<? extends JmsDestinationImpl>,Map<String,Object>>();

  // ---------------------------------------------------------------------------
  // The following constants represent the short names and the case value of the
  // supported properties.
  // Note that the first character of any shortName must always have a UTF8 encoding
  // of <= x7F and that the int value must be unique.
  // Oddities:
  //    1. There is no apparent reason why PR, DM and TL have shortNames, but
  //       they did so in the previous releases.
  //    2. BN, BD, DN, DD have no shortNames because they appear to have been
  //       forgotten about on earlier releases and dropped through into the
  //       'never heard of it but does work by reflection' code. Consequently
  //       we have to continue to use the unwieldy long form of the encoding.
  private static final String PR = "pr";  private final static int PR_INT =  1;  //"priority"
  private static final String DM = "dm";  private final static int DM_INT =  2;  //"deliveryMode"
  private static final String TL = "tl";  private final static int TL_INT =  3;  //"timeToLive"
  private static final String RA = "ra";  private final static int RA_INT =  4;  //"readAhead"
  private static final String TN = "tn";  private final static int TN_INT =  5;  //"topicName"
  private static final String TS = "ts";  private final static int TS_INT =  6;  //"topicSpace"
  private static final String QN = "qn";  private final static int QN_INT =  7;  //"queueName"
  private static final String QP = "qp";  private final static int QP_INT =  8;  //"scopeToLocalQP"
  private static final String PL = "pl";  private final static int PL_INT =  9;  //"producerPreferLocal"
  private static final String PB = "pb";  private final static int PB_INT = 10;  //"producerBind"
  private static final String GM = "gm";  private final static int GM_INT = 11;  //"gatherMessages"
  private static final String BN = null;  private final static int BN_INT = 12;  //"busName"
  private static final String BD = null;  private final static int BD_INT = 13;  //"blockedDestinationCode"
  private static final String DN = null;  private final static int DN_INT = 14;  //"destName"
  private static final String DD = null;  private final static int DD_INT = 15;  //"destDiscrim"

  // ---------------------------------------------------------------------------

  // *********************** STATIC INITIALIZATION ****************************

  static {
    initializePropertyMaps();
  }

  // **************************** CONSTRUCTORS ********************************

  /**
   * Constructor to be called by the factory object.
   */
  public MsgDestEncodingUtilsImpl() throws JMSException {
  }

  // *************************** INTERFACE METHODS *****************************

  /*
   * @see com.ibm.ws.sib.api.jms.MessageDestEncodingUtils#getMessageRepresentationFromDest(com.ibm.websphere.sib.api.jms.JmsDestination)
   *
   * Returns the efficient byte[] representation of the parameter destination
   * that can be stored in the message for transmission. The boolean parameter
   * indicates whether a full (normal dest) or partial (reply dest) encoding
   * should be carried out.
   *
   * @throws JMSException if
   *    dest is null - generates FFDC
   *    getShortPropertyValue throws it - FFDCs already generated.
   *    an UnsupportedEncodingException is generated - generates FFDC.
   */
  public final byte[] getMessageRepresentationFromDest(JmsDestination dest, EncodingLevel encodingLevel) throws JMSException {

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "getMessageRepresentationFromDest", new Object[]{dest, encodingLevel});

    boolean isTopic = false;
    byte[] encodedDest = null;
    ByteArrayOutputStream baos = new ByteArrayOutputStream();        // initial size of 32 is probably OK

    if (dest == null) {
      // Error case.
      JMSException e = (JMSException)JmsErrorUtils.newThrowable(JMSException.class,
            "INTERNAL_INVALID_VALUE_CWSIA0361",
            new Object[] {"null", "JmsDestination"}, tc);
      FFDCFilter.processException(e,"MsgDestEncodingUtilsImpl","getMessageRepresentationFromDest#1", this);
      throw e;
    }

    // If the JmsDestination is a Topic, note it for later use
    if (dest instanceof Topic) {
      isTopic = true;
    }

    // This variable is used to obtain a list of all the destination's properties,
    // giving us a copy which is safe for us to mess around with.
    Map<String,Object> destProps = ((JmsDestinationImpl)dest).getCopyOfProperties();

    // Now fiddle around with our copy of the properties, so we get the set we need to encode
    manipulateProperties(destProps, isTopic, encodingLevel);

    // Encode the basic stuff - Queue/Topic, DeliveryMode, Priority & TTL
    encodeBasicProperties(baos, isTopic, destProps);

    // Encode the rest of the properties
    encodeOtherProperties(baos, destProps);

    // Extract the byte array to return
    encodedDest = baos.toByteArray();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "getMessageRepresentationFromDest", encodedDest);
    return encodedDest;
  }


  /*
   * @see com.ibm.ws.sib.api.jms.MessageDestEncodingUtils#getDestinationFromMsgRepresentation(byte[])
   *
   * Inflates the efficient byte[] representation from the message into a
   * JmsDestination object.
   *
   * Throws a JMSException if there are problems during the deserialization process, for
   *    example if the parameter is null.
   */
  public final JmsDestination getDestinationFromMsgRepresentation(byte[] msgForm) throws JMSException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "getDestinationFromMsgRepresentation");
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "msgForm: " + SibTr.formatBytes(msgForm));

    JmsDestination newDest = null;

    if (msgForm == null) {
      // Error case.
      throw (JMSException)JmsErrorUtils.newThrowable(JMSException.class,
            "INTERNAL_INVALID_VALUE_CWSIA0361",
            new Object[] {"null", "getDestinationFromMsgRepresentation(byte[])"}, tc);
    }

    // The first half-byte in the message form indicates the Destination type
    if ((msgForm[0] & TOPIC_TYPE) == TOPIC_TYPE) {
      newDest = new JmsTopicImpl();
    }
    else {
      newDest = new JmsQueueImpl();
    }

    // Decode the basic stuff (i.e. DeliveryMode, Priority & TTL) onto the new Destination
    int offset = decodeBasicProperties(newDest, msgForm);

    // Now decode the rest of the byte[] onto this new Destination
    decodeOtherProperties(newDest, msgForm, offset);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "getDestinationFromMsgRepresentation", newDest);
    return newDest;
  }

  // ******* Package access static methods called by other impl classes *******

  /**
   * getDefaultPropertyValue
   * This method returns the default value for the supplied property name. It
   * is used by the JmsDestinationImpl constructor to set up default values where
   * appropriate, and by the encoding process to check (using .equals) when a
   * property is currently set to its default value.
   *
   * Note: The properties supported are only those included in the PropertyMap.
   *       It will always return null for any other properties, such as
   *       DEST_NAME, DEST_DESCRIM, forwardRP and reverseRP.
   *
   * @param propName       The name of the property for which the default value is wanted
   *
   * @return Object        The default value for the given property.
   */
  final static Object getDefaultPropertyValue(String propName) {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "getDefaultPropertyValue", propName);

    Object defaultValue = null;

    PropertyEntry prop = propertyMap.get(propName);
    if (prop != null) {
      defaultValue = prop.getDefaultValue();
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "getDefaultPropertyValue", defaultValue);
    return defaultValue;
  }

  /**
   * getPropertyType
   * Utility method which returns the type of value required for the given property.
   * URIDestinationCreator uses this method to test if a property is known or not,
   * and assumes 'silent' failure if the property is not known.
   *
   * @param longPropertyName     The long name of the property of interest.
   *
   * @return Class               The required class for this property's value, or null
   *                             if the property is unknown.
   */
  final static Class getPropertyType(String longPropertyName) {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "getPropertyType", longPropertyName);

    Class propType = null;

    PropertyEntry prop = propertyMap.get(longPropertyName);
    if (prop != null) {
      propType = prop.getType();
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "getPropertyType", propType);
    return propType;
  }

  /**
   * convertPropertyToType
   * Convert a property from its stringified form into an Object of the appropriate
   * type. Called by URIDestinationCreator.
   *
   * @param longPropertyName     The long name of the property
   * @param stringValue          The stringified property value
   *
   * @return Object Description of returned value
   */
  final static Object convertPropertyToType(String longPropertyName, String stringValue) {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "convertPropertyToType", new Object[]{longPropertyName, stringValue});

    // If the value is null or already a String we won't have anything to do
    Object decodedObject = stringValue;

    // The only non-String types we have are Integer & Long, so deal with them
    Class requiredType = getPropertyType(longPropertyName);
    if (requiredType == Integer.class) {
      decodedObject = Integer.valueOf(stringValue);
    }
    else if (requiredType == Long.class) {
      decodedObject = Long.valueOf(stringValue);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "convertPropertyToType");
    return decodedObject;
  }

  /*
   * setDestinationProperty
   * Utility method which the MQRequestReplyUtilsImpl and URIDestinationCreater classes
   * use to sep properties onto a destination object given the name of the property and
   * the value to be set.
   *
   * FRP & RRP are ignored by this method, as they are always set separately.
   *
   * @param dest                 The JMSDestination to set the property on to
   * @param longName             The long name of the property
   * @param longValue            The value to set the property to.
   *
   * @exception JMSException     Thrown if anything goes wrong.
   */
  final static void setDestinationProperty(JmsDestination dest, String longName, Object longValue) throws JMSException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "setDestinationProperty", new Object[]{dest, longName, longValue});

    // Let's hope the property is one we know about, as that will be quick & easy
    PropertyEntry propEntry = propertyMap.get(longName);
    if (propEntry != null) {
      if (longValue != null) {
      // If the value is of the wrong type, the caller screwed up
        if (!longValue.getClass().equals(propEntry.getType())) {
          throw (JMSException)JmsErrorUtils.newThrowable(JMSException.class
                                                        ,"INTERNAL_INVALID_VALUE_CWSIA0361"
                                                        ,new Object[] {longValue, longName}
                                                        ,null
                                                        ,"MsgDestEncodingUtilsImpl.setDestinationProperty#1"
                                                        ,null
                                                        ,tc);
        }
      }
      // If everything is OK, go ahead & set it
      setProperty(dest, longName, propEntry.getIntValue(), longValue);
    }

    // We could have a bash at finding a method by reflection, but we think we have
    // already catered for all known properties (and more), so we will just barf.
    else {
      throw (JMSException)JmsErrorUtils.newThrowable(JMSException.class
                                                    ,"UNKNOWN_PROPERTY_CWSIA0363"
                                                    ,new Object[] {longName}
                                                    ,null
                                                    ,"MsgDestEncodingUtilsImpl.setDestinationProperty#2"
                                                    ,null
                                                    ,tc);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "MsgDestEncodingUtilsImpl.setDestinationProperty#2");
  }

  /*
   * getDefaultJNDIProperties
   * Returns a map of property names and values that lists the properties
   * (and their defaults) that need to suppressed from the JNDI
   * reference if their values are those default values
   * @param clazz The class for which we need the set of default jndi properties
   * @return Map<String,Object> the map of property names and defaults values
   */
  @SuppressWarnings("unchecked")
  final static Map<String,Object> getDefaultJNDIProperties(Class<? extends JmsDestinationImpl> clazz) {
    Map<String,Object> answer = defaultJNDIProperties.get(clazz);

    // Ensure we get the empty map if there are no default properties
    if (answer == null) answer = Collections.EMPTY_MAP;

    return answer;
  }

  // ************** Private Static methods  ********************

  /**
   * initializePropertyMaps
   * Initialize the Property Maps which must each contain an entry for every supported
   * Destination Property which is to be encoded or set via this class.
   */
  private static void initializePropertyMaps() {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "initializePropertyMap");

    // Add all the supported properties to the PropertyMaps:
    // PR, DM, and TL use the PhantomPropertyCoder as they are encoded/decoded
    // separately rather than by using their PropertyCoder.
    // They need to be included in the map so that methods can extract information
    // and set them.
    // Any property with a shortName which maps to null but be added with one of
    // the general coders - currently StringPropertyCoder or IntegerPropertyCoder.
    // NOTE: If SuppressIfDefaultInJNDI is not null, the LongName MUST match that defined in
    // JmsInternalConstants
    // Parameters:
    //        PropertyCoder               ( LongName                            ShortName), IntValue,  ValueType    , DefaultValue             SuppressIfDefaultInJNDI)
    addToMaps(new PhantomPropertyCoder    (JmsInternalConstants.PRIORITY             , PR),  PR_INT,   Integer.class, Integer.valueOf(Message.DEFAULT_PRIORITY) ,null);
    addToMaps(new PhantomPropertyCoder    (JmsInternalConstants.DELIVERY_MODE        , DM),  DM_INT,   String.class , ApiJmsConstants.DELIVERY_MODE_APP         ,null);
    addToMaps(new PhantomPropertyCoder    (JmsInternalConstants.TIME_TO_LIVE         , TL),  TL_INT,   Long.class   , Long.valueOf(Message.DEFAULT_TIME_TO_LIVE),null);
    addToMaps(new ReadAheadCoder          ("readAhead"                               , RA),  RA_INT,   String.class , ApiJmsConstants.READ_AHEAD_AS_CONNECTION  ,null);
    addToMaps(new ShortStringPropertyCoder("topicName"                               , TN),  TN_INT,   String.class , null                                      ,null);
    addToMaps(new ShortStringPropertyCoder("topicSpace"                              , TS),  TS_INT,   String.class , JmsTopicImpl.DEFAULT_TOPIC_SPACE          ,null);
    addToMaps(new ShortStringPropertyCoder("queueName"                               , QN),  QN_INT,   String.class , null                                      ,null);
    addToMaps(new OnOffPropertyCoder      (JmsInternalConstants.SCOPE_TO_LOCAL_QP    , QP),  QP_INT,   String.class , ApiJmsConstants.SCOPE_TO_LOCAL_QP_OFF     ,JmsQueueImpl.class);
    addToMaps(new OnOffPropertyCoder      (JmsInternalConstants.PRODUCER_PREFER_LOCAL, PL),  PL_INT,   String.class , ApiJmsConstants.PRODUCER_PREFER_LOCAL_ON  ,JmsQueueImpl.class);
    addToMaps(new OnOffPropertyCoder      (JmsInternalConstants.PRODUCER_BIND        , PB),  PB_INT,   String.class , ApiJmsConstants.PRODUCER_BIND_OFF         ,JmsQueueImpl.class);
    addToMaps(new OnOffPropertyCoder      (JmsInternalConstants.GATHER_MESSAGES      , GM),  GM_INT,   String.class , ApiJmsConstants.GATHER_MESSAGES_OFF       ,JmsQueueImpl.class);
    addToMaps(new StringPropertyCoder     (JmsInternalConstants.BUS_NAME             , BN),  BN_INT,   String.class , null                                      ,null);
    addToMaps(new IntegerPropertyCoder    (JmsInternalConstants.BLOCKED_DESTINATION  , BD),  BD_INT,   Integer.class, null                                      ,null);
    addToMaps(new StringPropertyCoder     ("destName"                                , DN),  DN_INT,   String.class , null                                      ,null);
    addToMaps(new StringPropertyCoder     ("destDiscrim"                             , DD),  DD_INT,   String.class , null                                      ,null);

    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "propertyMap" + propertyMap);
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "reverseMap" + reverseMap);
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "initializePropertyMap");
  }

  /**
   * addToMaps
   * Add an entry to the PropertyMap, mapping the longName of the property to a
   * PropertyEntry which holds everything else we need to know. Also add an entry
   * to the reverseMap to allow us to map back from the shortName (if the is one)
   * to the longName.
   *
   * @param propCoder               The coder to be used for encoding & decoding this property, or null
   * @param intValue                The int for this property, to be used in case statements
   * @param type                    The class of the values for this property
   * @param defaultVal              The default value for the property, or null
   * @param suppressIfDefaultInJNDI If non-null, the class of JMS Objects for which this property should be suppressed in JNDI
   */
  private final static void addToMaps(PropertyCoder propCoder, int intValue, Class type, Object defaultVal, Class<? extends JmsDestinationImpl> suppressIfDefaultInJNDI) {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "addToMaps", new Object[]{propCoder, intValue, type, defaultVal, suppressIfDefaultInJNDI});

    // Extract the long & short names from the PropertyCoder. (We do this, rather than
    // passing them in separately, so that the names only have to be coded once).
    String longName  = propCoder.getLongName();
    String shortName = propCoder.getShortName();

    // Create the PropertyEntry which holds all the information we could possibly want, & put it in the Primary Poperty Map
    PropertyEntry propEntry = new PropertyEntry(intValue, type, defaultVal, propCoder);
    propertyMap.put(longName, propEntry);

    // Add the appropriate reverse mapping to the Reverse Map which is used for decoding
    if (shortName != null) {
      reverseMap.put(shortName, longName);
    }
    else {
      reverseMap.put(longName, longName);
    }

    // If this property is to suppressed in JNDI, add it to defaultJNDIProperties map
    if (suppressIfDefaultInJNDI != null) {
      Map<String,Object> map = defaultJNDIProperties.get(suppressIfDefaultInJNDI);
      if (map == null) {
        map = new HashMap<String,Object>();
        defaultJNDIProperties.put(suppressIfDefaultInJNDI,map);
      }

      map.put(longName,defaultVal);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "addToMaps");
  }

  /**
   * manipulateProperties
   * Sort out the Destination properties to get the set we need to encode.
   * Some of the properties are never encoded (FRP, RRP...) and some are only
   * included for a 'fullEncode'.
   * Queue & Topic need different 'names' for the same information.
   *
   * @param destProps            A modifiable copy of the full map of JmsDestinaiton properties
   * @param isTopic              true if the Destination is a Topic, otherwise false
   * @param fullEncode           Whether a fullEncode is required.
   */
  private static void manipulateProperties(Map<String,Object> destProps, boolean isTopic, EncodingLevel encodingLevel) {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "manipulateProperties", new Object[]{destProps, isTopic, encodingLevel});

    // Remove those properties that we do not want to be encoded (ever) when the
    // destination is stored in the message. These are in general things that are
    // sensible only for the local instance of the destination object, or those that
    // are stored elsewhere in the message.
    destProps.remove(JmsInternalConstants.FORWARD_ROUTING_PATH);
    destProps.remove(JmsInternalConstants.REVERSE_ROUTING_PATH);

    if (encodingLevel != EncodingLevel.FULL)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Removing reply header properties from list");
      // Remove any properties that are only required by the full
      // encoding ('cos we're going to use this encoding for reply destinations.
      destProps.remove(JmsInternalConstants.PRIORITY         );
      destProps.remove(JmsInternalConstants.TIME_TO_LIVE     );
      destProps.remove(JmsInternalConstants.SCOPE_TO_LOCAL_QP);
      destProps.remove(JmsInternalConstants.GATHER_MESSAGES  );
    }

    if (encodingLevel == EncodingLevel.MINIMAL)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Removing dest name properties from list");
      // Remove any properties that are not required by the minimal
      // encoding, which is used for reply destinations when not processing MQ source messages
      destProps.remove(JmsInternalConstants.DEST_DISCRIM     );
      destProps.remove(JmsInternalConstants.DEST_NAME        );
      destProps.remove(JmsInternalConstants.BUS_NAME         );
    }

    // Alter the map slightly to specialise for Queue and Topic.
    if (isTopic) {
      // Remove the DestName and Discriminator properties and replace
      // them with TopicSpace and TopicName.
      Object destNameObj = destProps.remove(JmsInternalConstants.DEST_NAME);
      if (destNameObj != null) destProps.put("topicSpace", destNameObj);
      Object destDiscrimObj = destProps.remove(JmsInternalConstants.DEST_DISCRIM);
      if (destDiscrimObj != null) destProps.put("topicName", destDiscrimObj);
    }
    else {
      // Remove destName and add queueName
      Object destNameObj = destProps.remove(JmsInternalConstants.DEST_NAME);
      if (destNameObj != null) destProps.put("queueName", destNameObj);
      // We could still have a discriminator, in which case rename it to our supported name
      Object destDiscrimObj = destProps.remove(JmsInternalConstants.DEST_DISCRIM);
      if (destDiscrimObj != null) destProps.put("destDiscrim", destDiscrimObj);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "manipulateProperies", destProps);
  }

  /**
   * encodeBasicProperties
   * Encode the basic information about the JmsDestination:
   *    Queue or Topic
   *    DeliveryMode
   *    Priority
   *    TimeToLive
   *
   * @param baos                 The ByteArrayOutputStream to encode into
   * @param isTopic              true if the Destination is a Topic, otherwise false
   * @param destProps            The Map of properties to be encoded
   */
  private static void encodeBasicProperties(ByteArrayOutputStream baos, boolean isTopic, Map<String,Object> destProps) {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "encodeBasicProperties", new Object[]{baos, isTopic, destProps});

    // Examine the basic property information. Note that this uses internal
    // knowledge of how the destination properties are stored.
    String dm   = (String) destProps.remove(JmsInternalConstants.DELIVERY_MODE);
    Integer pri = (Integer)destProps.remove(JmsInternalConstants.PRIORITY);
    Long ttl    = (Long)   destProps.remove(JmsInternalConstants.TIME_TO_LIVE);

    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, " isTopic=" + isTopic + " dm: " + dm + " pri: " + pri + " ttl: " + ttl);

    // Establsh the half-byte value for Delivery Mode
    byte dmByte = DM_NOT_SET;
    if ((dm != null) && (!ApiJmsConstants.DELIVERY_MODE_APP.equals(dm))) {
      if (ApiJmsConstants.DELIVERY_MODE_PERSISTENT.equals(dm)) {
        dmByte = DM_PERSISTENT;
      }
      else if (ApiJmsConstants.DELIVERY_MODE_NONPERSISTENT.equals(dm)) {
        dmByte = DM_NON_PERSISTENT;
      }
    }

    // Populate the first byte with the Queue/Topic and DM information
    if (isTopic) {
      baos.write(TOPIC_TYPE | dmByte);
    }
    else {
      baos.write(QUEUE_TYPE | dmByte);
    }

    // Establish the half-byte value for the Priority
    byte priByte = PRIORITY_NOT_SET;
    if (pri != null) {
      priByte = (byte)(pri.byteValue() << 4);
    }

    // Populate the 2nd byte with the Priority & TTL-setness information,
    // followed by the TTL itself if there is one.
    // Note that a TTL of 0 is not equivalent to no TTL (which implies AsApplication)
    if ((ttl == null)) {
      baos.write(priByte | TTL_NOT_SET);
    }
    else {
      baos.write(priByte | TTL_SET);

      // If we do have a TTL, we write it out in big-endian, though unfortunately
      // we need an 8 byte array for ArrayUtil to write into.
      // Theoretically we could use ObjectOutStream to write directly, but we can't
      // 'guarantee' the output would be compatible with the old hand-rolled encoding.
      // At least ArrayUtil is more efficient than the old-way and we know it is compatible.
      byte[] ttlBytes = new byte[8];
      ArrayUtil.writeLong(ttlBytes, 0, ttl);
      baos.write(ttlBytes, 0, 8);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Encoded basic information: " + SibTr.formatBytes(baos.toByteArray()));
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "encodeBasicProperties");
  }


  /**
   * encodeOtherProperties
   * Encode the more interesting JmsDestination properties, which may or may not be set:
   *    Queue/Topic name
   *    TopicSpace
   *    ReadAhead
   *    Cluster properties
   *
   * @param baos                 The ByteArrayOutputStream to encode into
   * @param destProps            The Map of properties to be encoded
   *
   * @exception JMSException     Thrown if anything goes horribly wrong
   */
  private static void encodeOtherProperties(ByteArrayOutputStream baos, Map<String,Object> destProps) throws JMSException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "encodeOtherProperties", new Object[]{baos, destProps});

    // Deal with each remaining property in turn.
    Iterator<Map.Entry<String, Object>> remainingProps = destProps.entrySet().iterator();
    while (remainingProps.hasNext()) {

      // Get the name and value of the property.
      Map.Entry<String, Object> nextProp = remainingProps.next();
      String propName = nextProp.getKey();
      Object propValue = nextProp.getValue();

      // If the property value is null, or it is the default, we don't encode it.
      if (  (propValue != null)
         && (!propValue.equals(getDefaultPropertyValue(propName)))
         ) {

        // Get the property's coder to encode it....
        PropertyEntry propEntry = propertyMap.get(propName);
        if (propEntry != null) {
          propEntry.getPropertyCoder().encodeProperty(baos, propValue);
        }
        else {
          // If we have found a property we don't expect, something has gone horribly wrong
          throw (JMSException)JmsErrorUtils.newThrowable(JMSException.class
                                                        ,"UNKNOWN_PROPERTY_CWSIA0363"
                                                        ,new Object[] {propName}
                                                        ,null
                                                        ,"MsgDestEncodingUtilsImpl.encodeOtherProperties#1"
                                                        ,null
                                                        ,tc);
        }
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "encodeOtherProperties");
  }


  /**
   * decodeBasicProperties
   * Decode the basic information to set the folloing attributes, if present, on to the new JmsDestination
   *    DeliveryMode
   *    Priority
   *    TimeToLive
   *
   * @param newDest              The new JmsDestination
   * @param msgForm              A byte array containing the message form of the information
   *
   * @return                     The next offset for reading from the byte array
   */
  private static int decodeBasicProperties(JmsDestination newDest, byte[] msgForm) throws JMSException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "decodeBasicProperties", new Object[]{newDest, msgForm});

    int offset = 0;

    // The 2nd half of the first byte carries the DeliveryMode
    byte delMode = (byte)(msgForm[0] & DM_MASK);
    if (delMode == DM_PERSISTENT) {
      newDest.setDeliveryMode(ApiJmsConstants.DELIVERY_MODE_PERSISTENT);
    }
    else if (delMode == DM_NON_PERSISTENT) {
      newDest.setDeliveryMode(ApiJmsConstants.DELIVERY_MODE_NONPERSISTENT);
    }
    else {
      newDest.setDeliveryMode(ApiJmsConstants.DELIVERY_MODE_APP);
    }

    // Next we get a half-byte for priority
    byte priority = (byte)(msgForm[1] & PRIORITY_MASK);
    if (priority != PRIORITY_NOT_SET) {
      int pri = 0x0F & (priority >>> 4);
      newDest.setPriority(pri);
    }

    // Next we establish if we have a TimeToLive, & decode it if we have
    if ((msgForm[1] & TTL_SET) == TTL_SET) {
      long ttl = ArrayUtil.readLong(msgForm, 2);
      newDest.setTimeToLive(ttl);
      offset = 10;
    }
    else {
     offset = 2;
    };

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "decodeBasicProperties", offset);
    return offset;
  }

  /**
   * decodeOtherProperties
   * Decode the more interesting JmsDestination properties, which may or may not be included:
   *    Queue/Topic name
   *    TopicSpace
   *    ReadAhead
   *    Cluster properties
   *
   * @param newDest              The Destination to apply the properties to
   * @param msgForm              The byte array containing the encoded Destination values
   * @param offset               The current offset into msgForm
   *
   * @exception JMSException     Thrown if anything goes horribly wrong
   */
  private static void decodeOtherProperties(JmsDestination newDest, byte[] msgForm, int offset) throws JMSException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "decodeOtherProperties", new Object[]{newDest, msgForm, offset});

    PropertyInputStream stream = new PropertyInputStream(msgForm, offset);

    while (stream.hasMore()) {
      String shortName = stream.readShortName();
      String longName = reverseMap.get(shortName);
      if (longName != null) {
        PropertyEntry propEntry = propertyMap.get(longName);  // This can't be null, as we just got the name from the reverseMap!
        Object propValue = propEntry.getPropertyCoder().decodeProperty(stream);
        setProperty(newDest, longName, propEntry.getIntValue(), propValue);
      }
      else {
        // If there is no mapping for the short name, then the property is not known.
        // The most likely situation is that we have been sent a property for a newer release.
        //
        throw (JMSException)JmsErrorUtils.newThrowable(JMSException.class
                                                      ,"UNKNOWN_PROPERTY_CWSIA0363"
                                                      ,new Object[] {shortName}
                                                      ,null
                                                      ,"MsgDestEncodingUtilsImpl.decodeOtherProperties#1"
                                                      ,null
                                                      ,tc);
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "decodeOtherProperties");
  }

  /**
   * setProperty
   * Set the given property to the given value on the given JmsDestination.
   *
   * @param dest                 The JmsDestination whose property is to be set
   * @param longName             The name of the property to be set
   * @param propIntValue         The name of the property to be set
   * @param value                The value to set into the property
   *
   * @exception JMSException     Thrown if the case is unexpected, or the JmsDestination objects.
   */
  private static void setProperty(JmsDestination dest, String longName, int propIntValue, Object value) throws JMSException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "setProperty", new Object[]{dest, longName, propIntValue, value});

    switch (propIntValue) {
      case PR_INT:
        dest.setPriority((Integer)value);
        break;
      case DM_INT:
        dest.setDeliveryMode((String)value);
        break;
      case TL_INT:
        dest.setTimeToLive((Long)value);
        break;
      case RA_INT:
        dest.setReadAhead((String)value);
        break;
      case TN_INT:
        ((JmsTopic)dest).setTopicName((String)value);
        break;
      case TS_INT:
        ((JmsTopic)dest).setTopicSpace((String)value);
        break;
      case QN_INT:
        ((JmsQueue)dest).setQueueName((String)value);
        break;
      case QP_INT:
        ((JmsQueue)dest).setScopeToLocalQP((String)value);
        break;
      case PL_INT:
        ((JmsQueue)dest).setProducerPreferLocal((String)value);
        break;
      case PB_INT:
        ((JmsQueue)dest).setProducerBind((String)value);
        break;
      case GM_INT:
        ((JmsQueue)dest).setGatherMessages((String)value);
        break;
      case BN_INT:
        dest.setBusName((String)value);
        break;
      case BD_INT:
        ((JmsDestInternals)dest).setBlockedDestinationCode((Integer)value);
        break;
      case DN_INT:
        ((JmsDestinationImpl)dest).setDestName((String)value);
        break;
      case DD_INT:
        ((JmsDestinationImpl)dest).setDestDiscrim((String)value);
        break;
      default:
        throw (JMSException)JmsErrorUtils.newThrowable(JMSException.class
                                                      ,"UNKNOWN_PROPERTY_CWSIA0363"
                                                      ,new Object[] {longName}
                                                      ,null
                                                      ,"MsgDestEncodingUtilsImpl.setProperty#1"
                                                      ,null
                                                      ,tc);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "setProperty");
  }

////////////////////////////////////////////////////////////////////////////////
// Static inner classes
////////////////////////////////////////////////////////////////////////////////

  //-----------------------------------------------------------------------------
  //------------------------ PropertyEntry class --------------------------------
  //-----------------------------------------------------------------------------
  /**
   * PropertyEntry
   * This private class holds all the information required for encoding and
   * decoding a single property.
   */
  private static class PropertyEntry {

    private int intValue;
    private Class  type;
    private Object defaultValue;
    private PropertyCoder propCoder;
    private String debugString;

    /**
     * PropertyEntry
     * Constructor for a PropertyEntry instance.
     *
     * @param aIntValue     The int for this property, to be used in case statements
     * @param aType         The class of the values for this property
     * @param aDefault      The default value for the property, or null
     * @param aCoder        The coder to be used for encoding & decoding this property, or null
     */
    private PropertyEntry(int aIntValue, Class aType, Object aDefault, PropertyCoder aCoder) {
      intValue = aIntValue;
      type = aType;
      defaultValue = aDefault;
      propCoder = aCoder;

      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "PropertyEntry.<init> " + toString());
    }

    private final int getIntValue() {
      return intValue;
    }

    private final Class getType() {
      return type;
    }

    private final Object getDefaultValue() {
      return defaultValue;
    }

    private final PropertyCoder getPropertyCoder() {
      return propCoder;
    }

    public final String toString() {
      if (debugString == null) {
        StringBuilder sb = new StringBuilder();
        sb.append("PropertyEntry: [ ");
        sb.append(intValue);     sb.append(", ");
        sb.append(type);         sb.append(", ");
        sb.append(defaultValue); sb.append(", ");
        sb.append(propCoder);    sb.append(" ]"); // The PropertyCoder's toString includes the corresponding longName & shortName
        debugString = new String(sb);
      }
      return debugString;
    }
  }

  //-----------------------------------------------------------------------------
  //------------------------ PropertyInputStream class --------------------------
  //-----------------------------------------------------------------------------

  /**
   * PropertyInputStream
   * This class provides a stream-like wrapper around the byte array which holds the
   * encoded form of the destination. The initial offset indicates where the normally
   * encoded properties start in the array (i.e. after Pri, DM and TTL).
   * The stream is only instantiated by the enclosing class, but is used by the
   * PropertyCoder classes so the non-constructor methods need package access.
   */
  static class PropertyInputStream {
    private static TraceComponent tc = SibTr.register(PropertyInputStream.class, ApiJmsConstants.MSG_GROUP_INT, ApiJmsConstants.MSG_BUNDLE_INT);

    private byte[] bytes;
    private int offset;

    private PropertyInputStream(byte[] bytes, int offset) {
      this.bytes = bytes;
      this.offset = offset;
    }

    /**
     * hasMore
     * Returns true if reading the byte array has not yet reached the end.
     *
     * @return boolean True if there are more bytes to read, otherwise false.
     */
    boolean hasMore() {
      return (offset < bytes.length);
    }

    /**
     * readShortName
     * Reads the short property name at the current position in the stream.
     *
     * @return    String       The short property name at the current position
     * @exception JMSException Thrown if any problems are encountered during the read.
     */
    final String readShortName() throws JMSException {
      String shortName;
      // Check whether we have a really long name preceded by a *
      if (bytes[offset] == PropertyCoder.STAR) {
        offset +=1;                    // Skip the *
        shortName = readStringValue();  // The name will be preceded by a length, just like a value
      }
      // if it really is a short name, we know the length so just read it
      else {
        shortName = readString(2);
      }
      return shortName;
    }

    /**
     * readStringValue
     * Gets the stringified property value which is encoded at the current position
     * in the stream.
     *
     * @return    String       The property value encoded at the current position
     * @exception JMSException Thrown if any problems are encountered during the read.
     */
    final String readStringValue() throws JMSException {
      try {
        int length = ArrayUtil.readShort(bytes, offset);
        offset +=ArrayUtil.SHORT_SIZE;
        String value = readString(length);
        return value;
      }
      catch (ArrayIndexOutOfBoundsException e) {
        FFDCFilter.processException(e, "com.ibm.ws.sib.api.jms.PropertyInputStream", "PropertyInputStream.readStringValue#1", this,
            new Object[] {toDebugString()});
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "PropertyInputStream.readStringValue#1 " + toDebugString());
        throw (JMSException)JmsErrorUtils.newThrowable(JMSException.class
                                                      ,"INTERNAL_ERROR_CWSIA0362"
                                                      ,new Object[] {"PropertyInputStream.readStringValue#1", offset, ""}
                                                      ,e
                                                      ,null   // Not much point FFDCing it again as we have already done so
                                                      ,null
                                                      ,null);
      }
    }

    /**
     * readString
     * Reads a String of a given length from the stream.
     *
     * @param length           The length of the String to be read
     * @return String          The String of given length at the current position in the stream
     * @exception JMSException Thrown if any problems are encountered during the read.
     */
    private String readString(int length) throws JMSException {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "readString", length);
      try {
        String str = Utf8Codec.decode(bytes, offset, length);
        offset += length;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "readString", str);
        return str;
      }
      catch (StringIndexOutOfBoundsException e) {
        FFDCFilter.processException(e, "com.ibm.ws.sib.api.jms.PropertyInputStream", "PropertyInputStream.readString#1", this,
            new Object[] {toDebugString()});
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "PropertyInputStream.readString#1 " + toDebugString());
        throw (JMSException)JmsErrorUtils.newThrowable(JMSException.class
                                                      ,"INTERNAL_ERROR_CWSIA0362"
                                                      ,new Object[] {"PropertyInputStream.readString#1", offset, ""}
                                                      ,e
                                                      ,null   // Not much point FFDCing it again as we have already done so
                                                      ,null
                                                      ,null);
      }
    }

    /**
     * toDebugString
     *
     * @return  A String representation of the stream, which may be useful in debugging.
     */
    final String toDebugString() {
      StringBuilder sb = new StringBuilder();
      sb.append("offset: ");
      sb.append(offset);
      sb.append(" bytes: ");
      sb.append(SibTr.formatBytes(bytes));
      return new String(sb);
    }
  }

}
