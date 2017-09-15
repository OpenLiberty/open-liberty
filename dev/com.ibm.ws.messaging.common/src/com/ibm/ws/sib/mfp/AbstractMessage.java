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

import java.util.List;

import com.ibm.ws.sib.mfp.JsDestinationAddress;
import com.ibm.ws.sib.mfp.ProtocolType;
import com.ibm.ws.sib.utils.DataSlice;
import com.ibm.ws.sib.utils.SIBUuid8;
import com.ibm.ws.sib.utils.SIBUuid12;

/**
 * AbstractMessage is an interface which commons up various methods which pertain
 * to both JsMessages and ControlMessages. These methods are predominantly for
 * accessing fields, but also include encodeFast() and isControl().
 */
public interface AbstractMessage {

  /* **************************************************************************/
  /* Method for encoding                                                      */
  /* **************************************************************************/

  /**
   *  Encode the message into a List of DataSlices, the content of which
   *  will be transmitted across the wire.
   *
   *  @param conn - the CommsConnection over which this encoded message will be sent.
   *                This may be null if the message is not really being encoded for transmission.
   *
   *  @return A List of DataSlices containing the encoded message.
   *
   *  @exception MessageEncodeFailedException is thrown if the message failed to encode.
   */
  List<DataSlice> encodeFast(Object conn) throws MessageEncodeFailedException;

  /* **************************************************************************/
  /* Get Methods                                                              */
  /* **************************************************************************/

  /**
   * Method to distinguish between Control messages and other message instances.
   *
   * @return A boolean which is true if the message is an Control message.
   */
  public boolean isControlMessage();

  /**
   *  Determine whether the message has been mediated when the mediation point and
   *  queueing point are distributed.
   *
   *  @return A boolean indicating whether or not the message has been mediated.
   */
  public boolean isMediated();

  /**
   *  Get the value of the Priority field from the message header.
   *
   *  @return An Integer containing the Priority of the message.
   *          Null is returned if the field was not set.
   */
  public Integer getPriority();

  /* ------------------------------------------------------------------------ */
  /* Optional Routing Destination                                             */
  /* ------------------------------------------------------------------------ */

  /**
   *  Get the optional RoutingDestination field from the message header.
   *  The JsDeststinationAddress returned is a copy of the header field,
   *  so no updates to it affect the Message header itself.
   *
   *  @return A JsDestinationAddress representing the RoutingDestination.
   *          Null is returned if the field was not set.
   */
  public JsDestinationAddress getRoutingDestination();

  /* ------------------------------------------------------------------------ */
  /* Guaranteed Delivery information (optional for JsMessages)                */
  /* ------------------------------------------------------------------------ */

  /**
   * Get the source Messaging Engine that originated the message.
   * The Messaging Engine hosting the producing application in the case of point to point push.
   * The Messaging Engine hosting the queue in the case of Anycast, remote get.
   * The Messaging Engine hosting the publishing application in the case of publish subscribe.
   *
   * @return The source Messaging Engine that originated the message.
   *          If Guaranteed Delivery information is not included in the
   *          message, this field will not be set.
   */
  public SIBUuid8 getGuaranteedSourceMessagingEngineUUID();

  /**
   * Get the next Messaging Engine that will store the message.
   * Destination localisation Messaging Engine in the case of point to point push.
   * Consumers Messaging Engine in the case of Anycast, remote get.
   * No value set in the case of publish subscribe.
   *
   * @return The next Messaging Engine that will store the message.
   *          If Guaranteed Delivery information is not included in the
   *          message, this field will not be set.
   */
  public SIBUuid8 getGuaranteedTargetMessagingEngineUUID();

  /**
   * Get the identity of the destination definition (not localisation)
   * that the message is intended for at the target Messaging Engine.
   * The resolved queue in the case of point to point.
   * The TopicSpace in the case of publish subscribe.
   *
   * @return The identity of the destination definition.
   *          If Guaranteed Delivery information is not included in the
   *          message, this field will not be set.
   */
  public SIBUuid12 getGuaranteedTargetDestinationDefinitionUUID();

  /**
   * Get the unique StreamId used by the flush protocol to determine
   * whether a stream is active or flushed.
   *
   * @return The unique Stream Id.
   *          If Guaranteed Delivery information is not included in the
   *          message, this field will not be set.
   */
  public SIBUuid12 getGuaranteedStreamUUID();

  /**
   * Get the unique GatheringTargetUUID.
   *
   * @return The unique GatheringTarget Id.
   *          If Guaranteed Delivery information is not included in the
   *          message, this field will not be set.
   */
  public SIBUuid12 getGuaranteedGatheringTargetUUID();

  /**
   * Get the class of protocol: UnicastInput UnicastOutput PubSubInput, PubSubOutput,
   * AnyCastInput or AnyCastOutput. This is used in the target ME to determine which
   * protocol handler to give the message to.
   *
   * @return The class of protocol.
   *          If Guaranteed Delivery information is not included in the
   *          message, this field will not be set.
   */
  public ProtocolType getGuaranteedProtocolType();

  /**
   * Get the version of the guaranteed delivery protocol used by this message.
   *
   * @return The version of the protocol in use.
   */
  public byte getGuaranteedProtocolVersion();

  /* ------------------------------------------------------------------------ */
  /* Optional Guaranteed Delivery Cross-Bus information                       */
  /* ------------------------------------------------------------------------ */

  /**
   * Get the LinkName for cross-bus guaranteed delivery from the message.
   *
   * @return The LinkName for cross-bus guaranteed delivery.
   *          If Guaranteed Delivery cross-bus information is not included in the
   *          message, this field will not be set.
   */
  public String getGuaranteedCrossBusLinkName();

  /**
   * Get the SourceBusUUID for cross-bus guaranteed delivery from the message.
   *
   * @return The UUID of the Source Bus.
   *          If Guaranteed Delivery cross-bus information is not included in the
   *          message, this field will not be set.
   */
  public SIBUuid8 getGuaranteedCrossBusSourceBusUUID();


  /* ------------------------------------------------------------------------ */
  /* Allow implementing classes to contribute single line trace strings       */
  /* ------------------------------------------------------------------------ */

  /**
   * Get a one-line summary string for use in minimal ME communications trace.
   * Format is "TYPE:field1=value1,field2=value2"
   * 
   * A StringBuilder instance is passed to the method to allow concatenation
   * to an existing buffer, rather than creation of multiple strings.
   * The Appendable interface was ruled out due to having insufficient append
   * methods, and the synchronization of a StringBuffer object is not required.
   * @param buff Buffer to append to.
   */
  public void getTraceSummaryLine(StringBuilder buff);

  /* **************************************************************************/
  /* Set Methods                                                              */
  /* **************************************************************************/

  /**
   *  Set whether or not the message has been mediated when the mediation point and
   *  queueing point are distributed.
   *
   *  @param value A boolean indicating whether or not the message has been mediated.
   */
  public void setMediated(boolean value);

  /* ------------------------------------------------------------------------ */
  /* Optional Routing Destination                                             */
  /* ------------------------------------------------------------------------ */

  /**
   *  Set the optional RoutingDestination field in the message header.
   *
   *  @param value The JsDestinationAddress representing the RoutingDestination.
   */
  public void setRoutingDestination(JsDestinationAddress value);

  /* ------------------------------------------------------------------------ */
  /* Guaranteed Delivery information (optional for JsMessages)                */
  /* ------------------------------------------------------------------------ */

  /**
   * Set the source Messaging Engine that originated the message.
   * The Messaging Engine hosting the producing application in the case of point to point push.
   * The Messaging Engine hosting the queue in the case of Anycast, remote get.
   * The MEssaging Engine hosting the publishing application in the case of publish subscribe.
   *
   * @param value The source Messaging Engine that originated the message.
   */
  public void setGuaranteedSourceMessagingEngineUUID(SIBUuid8 value);

  /**
   * Set the next Messaging Engine that will store the message.
   * Destination localisation Messaging Engine in the case of point to point push.
   * Consumers Messaging Engine in the case of Anycast, remote get.
   * No value set in the case of publish subscribe.
   *
   * @param value The next Messaging Engine that will store the message.
   */
  public void setGuaranteedTargetMessagingEngineUUID(SIBUuid8 value);

  /**
   * Set the identity of the destination definition (not localisation)
   * that the message is intended for at the target Messaging Engine.
   * The resolved queue in the case of point to point.
   * The TopicSpace in the case of publish subscribe.
   *
   * @param value The identity of the destination definition.
   */
  public void setGuaranteedTargetDestinationDefinitionUUID(SIBUuid12 value);

  /**
   * Set the unique StreamId used by the flush protocol to determine
   * whether a stream is active or flushed.
   *
   * @param value The unique Stream Id.
   */
  public void setGuaranteedStreamUUID(SIBUuid12 value);

  /**
   * Set the unique GatheringTargetUUID.
   *
   * @param value The unique GatheringTarget Id.
   */
  public void setGuaranteedGatheringTargetUUID(SIBUuid12 value);

  /**
   * Set the class of protocol: UnicastInput UnicastOutput PubSubInput, PubSubOutput,
   * AnyCastInput or AnyCastOutput. This is used in the target ME to determine which
   * protocol handler to give the message to.
   *
   * @param value The class of protocol.
   */
  public void setGuaranteedProtocolType(ProtocolType value);

  /**
   * Set the version of the guaranteed delivery protocol used by this message.
   *
   * @param value The version of the protocol in use.
   */
  public void setGuaranteedProtocolVersion(byte value);

  /* ------------------------------------------------------------------------ */
  /* Optional Guaranteed Delivery Cross-Bus information                       */
  /* ------------------------------------------------------------------------ */

  /**
   * Set the LinkName for cross-bus guaranteed delivery into the message.
   *
   * @param value The LinkName for cross-bus guaranteed delivery.
   */
  public void setGuaranteedCrossBusLinkName(String value);

  /**
   * Set the SourceBusUUID for cross-bus guaranteed delivery into the message.
   *
   * @param value The UUID of the Source Bus.
   */
  public void setGuaranteedCrossBusSourceBusUUID(SIBUuid8 value);

  /* **************************************************************************/
  /* Verbose string method for debugging assistance                           */
  /* **************************************************************************/

  /**
   * Return a string containing the message content for use by other components'
   * debugging. The string will contain all parts of the message and will be
   * written by the JMF Formatter.
   *
   * @return String The message content, formatted by the JMF formatter.
   */
  public String toVerboseString();

}
