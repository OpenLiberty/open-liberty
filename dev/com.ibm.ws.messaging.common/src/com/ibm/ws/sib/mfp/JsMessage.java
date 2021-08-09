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
import java.util.List;

import com.ibm.ws.sib.utils.DataSlice;
import com.ibm.ws.sib.utils.SIBUuid8;
import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.wsspi.sib.core.SIBusMessage;
import com.ibm.ws.sib.mfp.trm.TrmMessage;
import com.ibm.ws.sib.mfp.control.SubscriptionMessage;


/**
 * JsMessage is the basic interface for accessing and processing any
 * Jetstream message.
 * <p>
 * All Jetstream message types (e.g. JMS, Control, etc) are specializations of
 * JsMessage and can be 'made' from an existing JsMessage of the
 * appropriate type.
 * The JsMessage interface provides get/set methods for all the common
 * header information. It also provides methods for encoding a message
 * for transmission, and for serialization/deserialization.
 *
 */
public interface JsMessage extends SIBusMessage, AbstractMessage {

  /* **************************************************************************/
  /* Methods for making more specialised messages                             */
  /* **************************************************************************/

  /**
   *  Convert the existing inbound JsMessage into a JsJmsMessage.
   *  The JsJmsMessage returned will actually be of the appropriate
   *  sub-class - e.g. JsJmsTextMessage if the inbound message is actually
   *  a JMS TextMessage or of a form where TextMessage is the most appropriate
   *  JMS representation. A null-bodied message will be returned as a
   *  JsJmsMessage.
   *
   *  @return A JsJmsMessage, or appropriate sub-class, representing
   *   the same message.
   *
   *  @exception IncorrectMessageTypeException Thrown if the message is not a
   *  JMS message or of a type which can be represented as a JMS message.
   */
  public JsJmsMessage makeInboundJmsMessage() throws IncorrectMessageTypeException;

  /**
   *  Convert the existing inbound JsMessage into a TrmMessage.
   *  The TrmMessage returned will actually be of the appropriate
   *  sub-class - e.g. TrmRouteData if the inbound message is actually
   *  a TRM Route data Message. A TRM Message of unknown type will be
   *  returned as a TrmMessage.
   *
   *  @return A TrmMessage, or appropriate sub-class, representing
   *   the same message.
   */
  public TrmMessage makeInboundTrmMessage() throws IncorrectMessageTypeException;

  /**
   *  Convert the existing inbound JsMessage into a SubscriptionMessage.
   *
   *  @return A SubscriptionMessage, representing the same message.
   *
   *  @exception IncorrectMessageTypeException Thrown if the message is not
   *  Subscription message.
   */
  public SubscriptionMessage makeInboundSubscriptionMessage() throws IncorrectMessageTypeException;


  /* **************************************************************************/
  /* Methods for obtaining 'safe copies' of messages                          */
  /* **************************************************************************/

  /**
   *  Perform any send-time processing and return a 'safe copy' of the JsMessage.
   *  If the copy parameter is false, the 'safe copy' is the original message
   *  as no copy is actually required.
   *  <p>
   *  This method must be called by the Message processor during 'send'
   *  processing, AFTER the headers are set. The Message Processor should then
   *  use the returned message in the bus, which allows an Application to change the
   *  message it 'owns' without affectng the sent message.
   *
   *  @param copy       Indicates whether or not the message should be copied.
   *
   *  @return JsMessage The message to be 'sent' into the Messaging Engine.
   *
   *  @exception MessageCopyFailedException Thrown if the safe copy could not be made.
   */
  public JsMessage getSent(boolean copy) throws MessageCopyFailedException;

  /**
   *  Return a 'safe copy' of the JsMessage.
   *  This method must be called by the Message processor during 'receive'
   *  processing for a pub/sub message. It must be called BEFORE the headers are
   *  set for the receive. The Message Processor should then return this
   *  'copy' to the receiver, which can then make any changes without affecting
   *  the message in the bus or the copy given to any other receivers.
   *
   *  @return JsMessage Description of returned value
   *
   *  @exception MessageCopyFailedException Thrown if the safe copy could not be made.
   */
  public JsMessage getReceived() throws MessageCopyFailedException;


  /* **************************************************************************/
  /* Methods for encoding and flattenning (encodeFast is in AbstractMessage)  */
  /* **************************************************************************/

  /**
   *  Flatten the message into a List of DataSlices for persisting into
   *  the Message Store.
   *
   *  @param store The MesasgeStore that will be used for storing this message.
   *  @return A List of DataSlices containing the encoded message plus the
   *          information needed to re-establish the correct message specialisation.

   *  @exception MessageEncodeFailedException Thrown if the message could not be flattened
   */
  public List<DataSlice> flatten(Object store) throws MessageEncodeFailedException;      // SIB0112b.mfp.1

  /**
   * Get the approximate size of the message if it were to be flattened.  This
   * will be a quick and inexpensive (if perhaps somewhat inaccurate) calculation
   * of the message size.
   *
   * @return int The approximate length of the flattened message
   */
  public int getApproximateLength();

  /**
   * Get the approximate size of the message in memory.
   * This will be a will be a quick and inexpensive (but potentially very
   * inaccurate) calculation of the amount of space occupied in the heap by
   * a message which is both fully-fluffy and flattened (i.e. worst case).
   *
   * @return int The approximate size of the message in memory, when both
   *             flattened and fully decoded.
   */
  public int getInMemorySize();

 
  /**
   * Obtain a WebJsMessageEncoder for writing to the Web client.  The encoder
   * provides a method for encoding a JsMessage to a simple text format.  Only
   * JMS messages can be encoded in this way.
   *
   * @exception UnsupportedOperationException if the message is not a JMS message
   */
  public WebJsMessageEncoder getWebEncoder();

  /**
   * @return a message encode capable of encoding this message into an MQMsg2
   * (MQ Java classes message format).
   * @see com.ibm.ws.sib.mfp.JsMessage#getMQMsg2Encoder()
   
  MQMsg2Encoder getMQMsg2Encoder();
*/
  /* **************************************************************************/
  /* Methods for checking for optional data                                   */
  /* **************************************************************************/

  /**
   *  Return a boolean indicating whether the message header contains
   *  Guaranteed Delivery Remote Browse fields.
   *
   *  @return True if Guaranteed Delivery Remote Browse fields are included, otherwise
   *          false.
   */
  public boolean isGuaranteedRemoteBrowse();

  /**
   *  Return a boolean indicating whether the AuditSessionId field has been set
   *  in the message header.
   *
   *  @return True if the AuditSessionId field has been set, otherwise false.
   */
  public boolean isAuditSessionIdSet();


  /* **************************************************************************/
  /* Methods for checking whether Routing Paths are empty                     */
  /* **************************************************************************/

  /**
   *  Return a boolean indicating whether the ForwardRoutingPath is empty.
   *
   *  @return True if the ForwardRoutingPath is empty, otherwise false.
   */
  public boolean isForwardRoutingPathEmpty();

  /**
   *  Return a boolean indicating whether the ReverseRoutingPath is empty.
   *
   *  @return True if the ReverseRoutingPath is empty, otherwise false.
   */
  public boolean isReverseRoutingPathEmpty();


  /* **************************************************************************/
  /* Methods for loop detection (fingerprinting)                              */
  /* **************************************************************************/

  /**
   *  The returns the ordered list of the Fingerprints of the MEs and/or MQ Clusters
   *  the message has visited, or null if the message is not a Publish or has not
   *  visited any MEs/Clusters.
   *
   *  @return List<String> An ordered List containing the String representation of
   *          the Uuid/Clustername of each ME or MQ Cluster which the message has
   *          visited, in the order in which they were visited (assuming MQ honours ordering).
   */
  public List<String> getFingerprints();

  /**
   *  The method adds an MEUuid fingerprint to the end of the list of fingerprints.
   *
   *  @param meUuid  The MEUuid to be added as a fingerprint.
   */
  public void addFingerprint(SIBUuid8 meUuid);

  /**
   *  Clear the fingerprint list from the message.
   */
  public void clearFingerprints();


  /* **************************************************************************/
  /* Get Methods                                                              */
  /* **************************************************************************/

  /**
   *  Get the value of the Timestamp field from the message header.
   *  <p>
   *  The Timestamp is the time, in milliseconds after epoch, at which the message
   *  was originally sent.
   *
   *  @return A Long containing the Timestamp of the message.
   *          Null is returned if the field was not set.
   */
  public Long getTimestamp();

  /**
   *  Get the value of the MessageWaitTime field from the message header.
   *  <p>
   *  The MessageWaitTime is the cumulative time in, in milliseconds, that this
   *  message has been waiting in one or more MEs.
   *
   *  @return A Long containing the MessageWaitTime of the message.
   */
  public Long getMessageWaitTime();

  /**
   *  Get the value of the CurrentMEArrivalTimestamp field from the message header.
   *  <p>
   *  The CurrentMEArrivalTimestamp is the time, in milliseconds after epoch,
   *  at which either the message was originally sent (if in this ME) or when it
   *  arrived in this ME.
   *
   *  @return A Long containing the CurrentMEArrivalTimestamp of the message.
   */
  public Long getCurrentMEArrivalTimestamp();

  /**
   *  Get the value of the RedeliveredCount field from the message header.
   *
   *  @return An Integer representation of the RedeliveredCount of the message.
   */
  public Integer getRedeliveredCount();

  /**
   *  Get the value of the SystemMessageSourceUuid field in the message header.
   *
   *  @return A SIBUuid8 containing the source UUID for the message.
   */
  public SIBUuid8 getSystemMessageSourceUuid();

  /**
   *  Get the value of the SystemMessageValue field from the message header.
   *
   *  @return A long containing the identifier value of the message.
   */
  public long getSystemMessageValue();

  /**
   *  Get the contents of the Bus field from the message header.
   *
   *  @return A String containing the Bus.
   *          Null is returned if the field was not set.
   */
  public String getBus();

  /**
   *  Get the contents of the SecurityUserid field from the message header.
   *
   *  @return A String containing the SecurityUserid name.
   *          Null is returned if the field was not set.
   */
  public String getSecurityUserid();

  /**
   *  Indicate whether the message with a SecurityUserid field was sent by
   *  a system user.
   *
   *  @return A boolean indicating whether the message with the security userid
   *          was sent by a system user
   */
  public boolean isSecurityUseridSentBySystem();

  /**
   *  Get the value of the ProducerType field from the message header.
   *
   *  @return The ProducerType instance representing the Producer Type  of the
   *          message (e.g. Core, API, TRM)
   *          ProducerType.UNKNOWN is returned if the field is not set.
   */
  public ProducerType getProducerType();

  /**
   *  Get the value of the JsMessageType from the message header.
   *
   *  @return A MessageType representing the Message Type (e.g. JMS) of the message.
   */
  public MessageType getJsMessageType();

  /**
   *  Determine whether an RFH2 is allowed if encoding for MQ
   *
   *  @return A boolean indicating whether or not an RFH2 is allowed.
   */
  public boolean isMQRFH2Allowed();

  /* ------------------------------------------------------------------------ */
  /* Optional Exception information                                           */
  /* ------------------------------------------------------------------------ */

  /**
   *  Get the value of the ExceptionMessage from the message header.
   *
   *  @return A String containing the translated message formed from the
   *          Exception ReasonCode and inserts in the message.
   */
  public String getExceptionMessage();

  /* ------------------------------------------------------------------------ */
  /* Optional request metrics information                                     */
  /* -------------------------------------------------------------------------*/

  /**
   * Get the RM correlator from the message header.
   *
   * @return The RM correlator string.  Null is returned if no correlator
   *          has been set.
   */
  public String getRMCorrelator();

  /**
   * Get the ARM correlator from the message header.
   *
   * @return The ARM correlator string.  Null is returned if no correlator
   *          has been set.
   */
  public String getARMCorrelator();

  /* ------------------------------------------------------------------------ */
  /* Optional AuditSessionId                                                  */
  /* ------------------------------------------------------------------------ */

  /**
   *  Get the value of the AuditSessionId from the message header.
   *
   *  @return A String containing the AuditSessionId for this message.
   */
  public String getAuditSessionId();

  /* ------------------------------------------------------------------------ */
  /* Optional Guaranteed Delivery Value information                           */
  /* ------------------------------------------------------------------------ */

  /**
   *  Get the Guaranteed Delivery Value Start Tick value from the message.
   *
   *  @return A long containing the Guaranteed Delivery Value Start Tick value.
   *          If Guaranteed Delivery Value information is not included in the
   *          message, this field will not be set.
   */
  public long getGuaranteedValueStartTick();

  /**
   *  Get the Guaranteed Delivery Value End Tick value from the message.
   *
   *  @return A long containing the Guaranteed Delivery Value End Tick value.
   *          If Guaranteed Delivery Value information is not included in the
   *          message, this field will not be set.
   */
  public long getGuaranteedValueEndTick();

  /**
   *  Get the Guaranteed Delivery Value Value Tick value from the message.
   *
   *  @return A long containing the Guaranteed Delivery Value Value Tick value.
   *          If Guaranteed Delivery Value information is not included in the
   *          message, this field will not be set.
   */
  public long getGuaranteedValueValueTick();

  /**
   *  Get the Guaranteed Delivery Value CompletedPrefix from the message.
   *
   *  @return A long containing the Guaranteed Delivery Value LdPrefix.
   *          If Guaranteed Delivery Value information is not included in the
   *          message, this field will not be set.
   */
  public long getGuaranteedValueCompletedPrefix();

  /**
   *  Get the Guaranteed Delivery Value RequestedOnly value from the message.
   *
   *  @return A boolean containing the value of Guaranteed Delivery Value RequestedOnly.
   *          If Guaranteed Delivery Value information is not included in the
   *          message, this field will not be set.
   */
  public boolean getGuaranteedValueRequestedOnly();

  /* ------------------------------------------------------------------------ */
  /* Optional Guaranteed Remote Browse information                            */
  /* ------------------------------------------------------------------------ */

  /**
   *  Get the Guaranteed Delivery Remote Browse ID value from the message.
   *
   *  @return A long containing the Guaranteed Delivery Remote Browse ID.
   */
  public long getGuaranteedRemoteBrowseID();

  /**
   *  Get the Guaranteed Delivery Remote Browse Sequence Number from the message.
   *
   *  @return A long containing the Guaranteed Delivery Remote Browse Sequence Number.
   */
  public long getGuaranteedRemoteBrowseSequenceNumber();

  /* ------------------------------------------------------------------------ */
  /* Optional Guaranteed Remote Get information                               */
  /* ------------------------------------------------------------------------ */

  /**
   *  Get the Guaranteed Delivery Remote Get WaitTime value from the message.
   *  The time that the corresponding request waited at the DME before being satisfied.
   *  Used by the RME to estimte the round trip time.
   *
   *  @return A long containing the Guaranteed Delivery Remote Get Wait Time value.
   *          If Guaranteed Delivery Remote Get information is not included in the
   *          message, this field will not be set.
   */
  public long getGuaranteedRemoteGetWaitTime();

  /**
   *  Get the Guaranteed Delivery Remote Get Prev Tick value from the message.
   *  The previous tick contains data of the same QOS and Priority.
   *  Used to reconstruct ordering at the RME.
   *
   *  @return A long containing the Guaranteed Delivery Remote Get Prev Tick value.
   *          If Guaranteed Delivery Remote Get information is not included in the
   *          message, this field will not be set.
   */
  public long getGuaranteedRemoteGetPrevTick();

  /**
   *  Get the Guaranteed Delivery Remote Get Start Tick value from the message.
   *
   *  @return A long containing the Guaranteed Delivery Remote Get Start Tick value.
   *          If Guaranteed Delivery Remote Get information is not included in the
   *          message, this field will not be set.
   */
  public long getGuaranteedRemoteGetStartTick();

  /**
   *  Get the Guaranteed Delivery Remote Get Value Tick value from the message.
   *
   *  @return A long containing the Guaranteed Delivery Remote Get Value Tick value.
   *          If Guaranteed Delivery Remote Get information is not included in the
   *          message, this field will not be set.
   */
  public long getGuaranteedRemoteGetValueTick();


  /* **************************************************************************/
  /* Set Methods                                                              */
  /* **************************************************************************/

  /**
   *  Set the value of the Timestamp field in the message header.
   *  <p>
   *  The Timestamp is the time, in milliseconds after epoch, at which the message
   *  was originally sent.
   *
   *  @param value A long containing the Timestamp of the message.
   */
  public void setTimestamp(long value);

  /**
   *  Set the value of the MessageWaitTime field in the message header.
   *  <p>
   *  The MessageWaitTime is the cumulative time in, in milliseconds, that this
   *  message has been waiting in one or more MEs.
   *
   *  @param value A long containing the MessageWaitTime of the message.
   */
  public void setMessageWaitTime(long value);

  /**
   *  Set the value of the CurrentMEArrivalTimestamp field in the message header.
   *  <p>
   *  The CurrentMEArrivalTimestamp is the time, in milliseconds after epoch,
   *  which should be set when the message is first sent, and updated subsequently
   *  on arrival at any ME.
   *
   *  @param value A Long containing the CurrentMEArrivalTimestamp of the message.
   */
  public void setCurrentMEArrivalTimestamp(long value);

  /**
   *  Set the value of the RedeliveredCount field in the message header.
   *
   *  @param value An int representation of the RedeliveredCount of the message.
   */
  public void setRedeliveredCount(int value);

  /**
   *  Set the value of the deliveryCount transient which is used by Selectors
   *
   *  @param value The number of times the message has been delivered to a consumer.
   */
  public void setDeliveryCount(int value);

  /**
   *  Set the value of the SystemMessageSourceUuid field in the message header.
   *
   *  @param value A SIBUuid8 containing the source UUID for the message.
   */
  public void setSystemMessageSourceUuid(SIBUuid8 value);

  /**
   *  Set the value of the SystemMessageValue field in the message header.
   *
   *  @param value A long containing the identifier value for the message.
   */
  public void setSystemMessageValue(long value);

  /**
   *  Set the contents of the Bus field in the message header.
   *
   *  @param value A String containing the Bus.
   */
  public void setBus(String value);

  /**
   *  Set the contents of the SecurityUserid field in the message header.
   *  If this fiels is set, setSecurityUseridSentBySystem must be used to
   *  indicate whether or not the message was sent by a system user.
   *
   *  @param value A String containing the SecurityUserid name.
   */
  public void setSecurityUserid(String value);

  /**
   *  Set whether the message with a SecurityUserid field was sent by
   *  a system user.
   *
   *  @param value A boolean indicating whether the message with the security userid
   *               was sent by a system user
   */
  public void setSecurityUseridSentBySystem(boolean value);

  /**
   *  Set whether or not an RFH2 is allowed if encoding for MQ
   *
   *  @param value A boolean indicating whether or not an RFH2 is allowed.
   */
  public void setMQRFH2Allowed(boolean value);

  /* ------------------------------------------------------------------------ */
  /* Optional request metrics information                                     */
  /* -------------------------------------------------------------------------*/

  /**
   * Set the optional RM correlator in the message header.
   *
   * @param value The RM correlator string.
   */
  public void setRMCorrelator(String value);

  /**
   * Set the optional ARM correlator in the message header.
   *
   * @param value The ARM correlator string.
   */
  public void setARMCorrelator(String value);

  /* ------------------------------------------------------------------------ */
  /* Optional AuditSessionId                                                  */
  /* ------------------------------------------------------------------------ */

  /**
   * Set the optional AuditSessionId in the message header.
   *
   * @param value The AuditSessionId string.
   */
  public void setAuditSessionId(String value);

  /* ------------------------------------------------------------------------ */
  /* Optional Guaranteed Delivery Value information                           */
  /* ------------------------------------------------------------------------ */

  /**
   *  Set the Guaranteed Delivery Value Start Tick value in the message.
   *
   *  @param value A long containing the Guaranteed Delivery Value Start Tick value.
   */
  public void setGuaranteedValueStartTick(long value);

  /**
   *  Set the Guaranteed Delivery Value End Tick value in the message.
   *
   *  @param value A long containing the Guaranteed Delivery Value End Tick value.
   */
  public void setGuaranteedValueEndTick(long value);

  /**
   *  Set the Guaranteed Delivery Value Value Tick value in the message.
   *
   *  @param value A long containing the Guaranteed Delivery Value Value Tick value.
   */
  public void setGuaranteedValueValueTick(long value);

  /**
   *  Set the Guaranteed Delivery Value CompletedPrefix in the message.
   *
   *  @param value A long containing the Guaranteed Delivery Value CompletedPrefix.
   */
  public void setGuaranteedValueCompletedPrefix(long value);

  /**
   *  Set the Guaranteed Delivery Value RequestedOnly value in the message.
   *
   *  @param value A boolean containing the value of Guaranteed Delivery Value RequestedOnly.
   */
  public void setGuaranteedValueRequestedOnly(boolean value);

  /* ------------------------------------------------------------------------ */
  /* Optional Guaranteed Remote Browse information                            */
  /* ------------------------------------------------------------------------ */

  /**
   *  Clear the Guaranteed Delivery Remote Browse information in the message.
   */
  public void clearGuaranteedRemoteBrowse();

  /**
   *  Set the Guaranteed Delivery Remote Browse ID value in the message.
   *
   *  @param value A long containing the Guaranteed Delivery Remote Browse ID.
   */
  public void setGuaranteedRemoteBrowseID(long value);

  /**
   *  Set the Guaranteed Delivery Remote Browse Sequence Number in the message.
   *
   *  @param value A long containing the Guaranteed Delivery Remote Browse Sequence Number.
   */
  public void setGuaranteedRemoteBrowseSequenceNumber(long value);

  /* ------------------------------------------------------------------------ */
  /* Optional Guaranteed Remote Get information                               */
  /* ------------------------------------------------------------------------ */

  /**
   *  Clear the Guaranteed Delivery Remote Get information in the message.
   */
  public void clearGuaranteedRemoteGet();

  /**
   *  Set the Guaranteed Delivery Remote Get WaitTime value in the message.
   *  The time that the corresponding request waited at the DME before being satisfied.
   *  Used by the RME to estimte the round trip time.
   *
   *  @param value A long containing the Guaranteed Delivery Remote Get Wait Time value.
   */
  public void setGuaranteedRemoteGetWaitTime(long value);

  /**
   *  Set the Guaranteed Delivery Remote Get Prev Tick value in the message.
   *  The previous tick contains data of the same QOS and Priority.
   *  Used to reconstruct ordering at the RME.
   *
   *  @param value A long containing the Guaranteed Delivery Remote Get Prev Tick value.
   */
  public void setGuaranteedRemoteGetPrevTick(long value);

  /**
   *  Set the Guaranteed Delivery Remote Get Start Tick value in the message.
   *
   *  @param value A long containing the Guaranteed Delivery Remote Get Start Tick value.
   */
  public void setGuaranteedRemoteGetStartTick(long value);

  /**
   *  Set the Guaranteed Delivery Remote Get Value Tick value in the message.
   *
   *  @param value A long containing the Guaranteed Delivery Remote Get Value Tick value.
   */
  public void setGuaranteedRemoteGetValueTick(long value);

  /* ------------------------------------------------------------------------ */
  /* Optional Exception information                                           */
  /* ------------------------------------------------------------------------ */

  /**
   *  Set the value of the ExceptionReason in the message header.
   *
   *  @param value An int representing the reason the Message is being
   *               written to the Exception Destination.
   */
  public void setExceptionReason(int value);

  /**
   *  Set the value of the ExceptionInserts in the message header.
   *
   *  @param values An array containing the String 'inserts' for the Error Message
   *               identified by the Exception Reason.
   */
  public void setExceptionInserts(String[] values);

  /**
   *  Set the value of the ExceptionTimestamp in the message header.
   *
   *  @param value A long representing the time, in milliseconds after epoch,
   *               when the Message was written to the Exception Destination.
   */
  public void setExceptionTimestamp(long value);

  /**
   *  Set the value of the Exception Problem Destination in the message header.
   *
   *  @param value  A String indicating the Destination the message was intended for
   *                when a problem occurred which caused it to be routed to the
   *                Exception Destination.
   */
  public void setExceptionProblemDestination(String value);

  /**
   *  Set the value of the Exception Problem Subscription in the message header.
   *
   *  @param value  A String indicating the Subscription the message was intended for
   *                when a problem occurred which caused it to be routed to the
   *                Exception Destination.
   */
  public void setExceptionProblemSubscription(String value);

  /* **************************************************************************/
  /* Get/Set Methods for API Message meta-data fields                         */
  /* **************************************************************************/

  /**
   * Method to distinguish between API message instances and System message instances.
   * @return A boolean which is true if the message is an API message.
   */
  public boolean isApiMessage();

  /**
   *  Get the contents of the UserId field from the message header.  This is the
   *  value that a JMS application will see in the JMSXUserId property.
   *
   *  @return A string containing the UserId.  Null is returned if the field was not set.
   */
  public String getApiUserId();

  /**
   *  Get the contents of the ApiMessageId field from the message header.
   *
   *  @return A byte array containing a copy of the ApiMessageId as a binary value.
   *          Null is returned if the field was not set.
   */
  public byte[] getApiMessageIdAsBytes();

  /**
   *  Get the contents of the CorrelationId field from the message header.
   *
   *  @return A byte array containing a copy of the CorrelationId as a binary value.
   *          If the CorrelationId was stored as an arbitrary String (i.e. not
   *          of the form ID:xxxx) UTF8 encoding is used.
   *          Null is returned if the field was not set.
   */
  public byte[] getCorrelationIdAsBytes();

  /**
   *  Set the contents of the UserId field in the message header.  This is the
   *  value that a JMS application will see in the JMSXUserId property.
   *
   *  @param value A string containing the userid.
   */
  public void setApiUserId(String value);

  /**
   *  Set the contents of the ApiMessageId field in the message header.
   *  The value in the message is set to a copy of the value given.
   *
   *  @param value A byte array containing the ApiMessageId in binary.
   */
  public void setApiMessageIdAsBytes(byte[] value);

  /**
   *  Set the contents of the CorrelationId field in the message header.
   *  The value in the message is set to a copy of the value given.
   *
   *  @param value A byte array containing the CorrelationId in binary.
   */
  public void setCorrelationIdAsBytes(byte[] value);

  /* ------------------------------------------------------------------------ */
  /* ConnectionUuid                                                           */
  /* ------------------------------------------------------------------------ */

  /**
   *  Set the value of the ConnectionUuid field in the message header.
   *
   *  @param A SIBUuid12 representing the ConnectionUuid.
   */
  public void setConnectionUuid(SIBUuid12 value);

  /**
   *  Get the value of the ConnectionUuid field from the message header.
   *
   *  @return A SIBUuid12 representing the ConnectionUuid.
   *          Null is returned if the field is not set.
   */
  public SIBUuid12 getConnectionUuid();


  /* **************************************************************************/
  /* Methods to determine if the setRemainingTimeToLive() method as called    */
  /* **************************************************************************/

  /**
   * Clear a flag indicating if setRemainingTimeToLive() has been called.
   */
  public void clearWasRemainingTimeToLiveChanged();

  /**
   * Return a flag indicating that setRemainingTimeToLive() has been called
   * on this message.
   *
   * @return A boolean indicating that setRemainingTimeToLive() was invoked.
   */
  public boolean wasRemainingTimeToLiveChanged();

  /* **************************************************************************/
  /* Methods for clearing API Message properties and payload                  */
  /* **************************************************************************/

  /**
   *  Delete all the Properties from an API Message.
   */
  public void clearMessageProperties();

  /**
   *  Delete the Payload of an API Message.
   */
  public void clearMessagePayload();

  /**
   * Transcribe this message to pure JMF. If this message contains encapsulated data,
   * this method will, where possible, return a pure JMF (non-encapsulated) copy.
   * Currently only JMS messages are supported.
   *
   * @return a pure JMF copy of this message
   * @exception MessageCopyFailedException Thrown if the pure JMF copy could not be made.
   * @exception IncorrectMessageTypeException Thrown if the message was not JMS.
   * @exception UnsupportedEncodingException is thrown if the payload is encoded
   *             in a codepage which is not supported on this system.
   */
  public JsMessage transcribeToJmf() throws MessageCopyFailedException
                                          , IncorrectMessageTypeException
                                          , UnsupportedEncodingException;
}
