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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.ibm.ws.sib.mfp.MessageDecodeFailedException;
import com.ibm.ws.sib.mfp.MessageType;
import com.ibm.ws.sib.mfp.MfpConstants;
import com.ibm.ws.sib.mfp.ProducerType;
import com.ibm.ws.sib.mfp.control.SubscriptionMessage;
import com.ibm.ws.sib.mfp.control.SubscriptionMessageType;
import com.ibm.ws.sib.mfp.schema.JsPayloadAccess;
import com.ibm.ws.sib.mfp.schema.SubscriptionAccess;
import com.ibm.ws.sib.utils.SIBUuid8;
import com.ibm.ws.sib.utils.ras.SibTr;

import com.ibm.websphere.ras.TraceComponent;

/**
 *  SubscriptionMessageImpl extends JsMessageImpl, and is the implementation class for
 *  the SubscriptionMessage interface.
 *  <p>
 *  The JsMessageImpl instance contains the JsMsgObject which is the
 *  internal object which represents the API Message.
 */
public class SubscriptionMessageImpl extends JsMessageImpl implements SubscriptionMessage {

  private final static long serialVersionUID = 1L;
  private final static byte[] flattenedClassName;                               // SIB0112b.mfp.2

  private final static TraceComponent tc = SibTr.register(SubscriptionMessageImpl.class, MfpConstants.MSG_GROUP, MfpConstants.MSG_BUNDLE);

  /* Get the flattened form of the classname                   SIB0112b.mfp.2 */
  static {
    flattenedClassName = flattenClassName(SubscriptionMessageImpl.class.getName());
  }

  /* **************************************************************************/
  /* Constructors                                                             */
  /* **************************************************************************/

  /**
   *  Constructor for a new Subscription Propagation control message.
   *
   *  This constructor should never be used except by JsMessageImpl.createNew().
   *  The method must not actually do anything.
   */
  SubscriptionMessageImpl() {
  }

  /**
   *  Constructor for a new Subscription Propagation control message.
   *  To be called only by the ControlMessageFactory.
   *
   *  @param flag No-op flag to distinguish different constructors.
   *
   *  @exception MessageDecodeFailedException Thrown if such a message can not be created
   */
  SubscriptionMessageImpl(int flag) throws MessageDecodeFailedException {
    super(flag);
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "<init>");

    /* Set the Subscription message fields                                    */
    jmo.getPayloadPart().setPart(JsPayloadAccess.PAYLOAD_DATA, SubscriptionAccess.schema);

    setJsMessageType(MessageType.SUBSCRIPTION);
    setProducerType(ProducerType.MP);

    /* Default all the fields                                                 */
    setSubtype(SubscriptionMessageType.UNKNOWN_INT);
    setTopics(null);
    setTopicSpaces(null);
    setTopicSpaceMappings(null);
    setMEName(null);
    setMEUUID(null);
    setBusName(null);
  }


  /**
   *  Constructor for an inbound message.
   *  (Only to be called by a superclass make method.)
   *
   *  @param inJmo The JsMsgObject representing the inbound method.
   */
  SubscriptionMessageImpl(JsMsgObject inJmo) {
    super(inJmo);
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "<init>, inbound jmo ");
    /* Do not set any fields - they should already exist in the message       */
  }


  // Convienience method to get the payload as a SubscriptionSchema
  JsMsgPart getPayload() {
    return getPayload(SubscriptionAccess.schema);
  }

  /* **************************************************************************/
  /* Get Methods                                                              */
  /* **************************************************************************/

  /*
   *  Get the value of the SubscriptionMessageType from the  message.
   *
   *  Javadoc description supplied by SubscriptionMessage interface.
   */
  public final SubscriptionMessageType getSubscriptionMessageType() {
    /* Get the subtype then get the corresponding SubscriptionMessageType to return  */
    int mType = getSubtype();
    return SubscriptionMessageType.getSubscriptionMessageType(mType);
  }

  /*
   *  Get the List of Topics from the message.
   *
   *  Javadoc description supplied by SubscriptionMessage interface.
   */
  public final List<String> getTopics() {
    return (List<String>)getPayload().getField(SubscriptionAccess.TOPICS);
  }

  /*
   *  Get the List of TopicSpaces from the message.
   *
   *  Javadoc description supplied by SubscriptionMessage interface.
   */
  public final List<String> getTopicSpaces() {
    return (List<String>)getPayload().getField(SubscriptionAccess.TOPICSPACES);
  }

  /*
   *  Get the List of TopicSpace mappings from the message.
   *
   *  Javadoc description supplied by SubscriptionMessage interface.
   */
  public final List<String> getTopicSpaceMappings() {
    return (List<String>)getPayload().getField(SubscriptionAccess.TOPICSPACEMAPPINGS);
  }

  /*
   *  Get the ME Name from the message.
   *
   *  Javadoc description supplied by SubscriptionMessage interface.
   */
  public final String getMEName() {
    return (String)getPayload().getField(SubscriptionAccess.MENAME);
  }

  /*
   *  Get the ME UUID from the message.
   *
   *  Javadoc description supplied by SubscriptionMessage interface.
   */
  public final byte[] getMEUUID() {
    return (byte[])getPayload().getField(SubscriptionAccess.MEUUID);
  }

  /*
   *  Gets the Bus name for this message
   *
   *  Javadoc description supplied by SubscriptionMessage interface.
   */
  public String getBusName() {
    return (String)getPayload().getField(SubscriptionAccess.BUSNAME);
  }
  
  /**
   * Helper method to get, or assign, a unique key to a string,
   * from an ArrayList containing the unique keys.
   * @param uniqueStrings Current set of unique strings
   * @param value Object to convert to a string, and find/assign an number for
   * @return number associated with the unique string
   */
  private int getIntKeyForString(ArrayList<String> uniqueStrings, Object value) {
    String stringValue = String.valueOf(value);
    int retval = uniqueStrings.indexOf(stringValue);
    if (retval < 0) {
      retval = uniqueStrings.size();
      uniqueStrings.add(stringValue);
    }
    return retval;
  }
  
  /*
   * Summary information for Subscription messages
   */
  public void getTraceSummaryLine(StringBuilder buff) {
    super.getTraceSummaryLine(buff);
    
    buff.append(",subMsgType=");
    buff.append(getSubscriptionMessageType());

    List topics = getTopics();
    buff.append(",topicCount=");
    buff.append(topics.size());
    
    // As we may have a lot of topics, with a lot of duplication of topic space
    // and mapping, we need to condense the topic output. The following form is chosen:
    // topics=[my/topic1|0|1,my/topic2|0|2],0=Topic.Space.1,1=Topic.Space.2,2=Topic.Space.3
    //
    // Even condensed there's a concern the output could get too large, so it's limited to around 4KB
    Iterator tIterator = topics.iterator();
    Iterator tsIterator = getTopicSpaces().iterator();
    Iterator tsmIterator = getTopicSpaceMappings().iterator();
    // We use a list, not a map, for the list of unique names. This is because
    // we expect this list to be small, and want to keep it in order without
    // the overhead of a sorted map.
    ArrayList<String> uniqueNames = new ArrayList<String>();
    
    // Iterate through the topics
    buff.append(",topics=[");
    while(tIterator.hasNext()) {
      String topic = String.valueOf(tIterator.next());
      
      // Get integer keys for the string topicSpace/topicSpaceMapping values, coping with nulls
      int tsInteger  = getIntKeyForString(uniqueNames, tsIterator.hasNext()?tsIterator.next():null);
      int tsmInteger = getIntKeyForString(uniqueNames, tsmIterator.hasNext()?tsmIterator.next():null);
      
      // Append a compressed version of the topic + mapping
      buff.append(topic);
      buff.append('|');
      buff.append(tsInteger);
      buff.append('|');
      buff.append(tsmInteger);
      
      // Break out of the loop if it looks like we're going to
      // produce a dangerously large amount of output on the line.
      if (buff.length() > 4000) {
        buff.append(",..."); // We've already printed the count
        break;
      }
      
      // Append a separator if we're going round again
      if (tIterator.hasNext()) buff.append(',');
    }
    buff.append(']');
    
    // Append the unique topic space names with their integer values
    for (int i = 0; i < uniqueNames.size(); i++) {
      buff.append(',');
      buff.append(i);
      buff.append('=');
      buff.append(uniqueNames.get(i));
    }
    
    buff.append(",meName=");
    buff.append(getMEName());
    
    buff.append(",meUuid=");
    buff.append(new SIBUuid8(getMEUUID()));
    
    buff.append(",busName=");
    buff.append(getBusName());
    
  }  

  /* **************************************************************************/
  /* Set Methods                                                              */
  /* **************************************************************************/

  /*
   *  Set the value of the SubscriptionMessageType field in the message.
   *
   *  Javadoc description supplied by SubscriptionMessage interface.
   */
  public final void setSubscriptionMessageType(SubscriptionMessageType value) {
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "setSubscriptionMessageType to " + value);
    /* Get the int value of the SubscriptionMessageType and set that into the subtype  */
    setSubtype(value.toInt());
  }

  /*
   *  Set the List of Topics in the message.
   *
   *  Javadoc description supplied by SubscriptionMessage interface.
   */
  public final void setTopics(List<String> value) {
    getPayload().setField(SubscriptionAccess.TOPICS, value);
  }

  /*
   *  Set the List of TopicSpaces in the message.
   *
   *  Javadoc description supplied by SubscriptionMessage interface.
   */
  public final void setTopicSpaces(List<String> value) {
    getPayload().setField(SubscriptionAccess.TOPICSPACES, value);
  }

  /*
   *  Set the List of TopicSpace mappings in the message.
   *
   *  Javadoc description supplied by SubscriptionMessage interface.
   */
  public final void setTopicSpaceMappings(List<String> value) {
    getPayload().setField(SubscriptionAccess.TOPICSPACEMAPPINGS, value);
  }

  /*
   *  Set the ME Name in the message.
   *
   *  Javadoc description supplied by SubscriptionMessage interface.
   */
  public final void setMEName(String value) {
    getPayload().setField(SubscriptionAccess.MENAME, value);
  }

  /*
   *  Set the byte array ME UUID in the message.
   *
   *  Javadoc description supplied by SubscriptionMessage interface.
   */
  public final void setMEUUID(byte[] value) {
    getPayload().setField(SubscriptionAccess.MEUUID, value);
  }

  /*
   *  Set the Bus name for this message
   *
   *  Javadoc description supplied by SubscriptionMessage interface.
   */
  public void setBusName(String value) {
    getPayload().setField(SubscriptionAccess.BUSNAME, value);
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
