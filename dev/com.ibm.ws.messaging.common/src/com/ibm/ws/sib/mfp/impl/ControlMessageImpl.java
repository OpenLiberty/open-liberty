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
import java.util.List;

import com.ibm.websphere.sib.Reliability;
import com.ibm.ws.sib.mfp.control.ControlMessage;
import com.ibm.ws.sib.mfp.control.ControlMessageType;
import com.ibm.ws.sib.mfp.JsDestinationAddress;
import com.ibm.ws.sib.mfp.MessageDecodeFailedException;
import com.ibm.ws.sib.mfp.MessageEncodeFailedException;
import com.ibm.ws.sib.mfp.MfpConstants;
import com.ibm.ws.sib.mfp.ProtocolType;
import com.ibm.ws.sib.mfp.schema.ControlAccess;
import com.ibm.ws.sib.utils.DataSlice;
import com.ibm.ws.sib.utils.SIBUuid8;
import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.ws.sib.utils.ras.SibTr;

import com.ibm.websphere.ras.TraceComponent;

/**
 *  ControlMessageImpl extends MessageImpl, and is the implementation class for
 *  the ControlMessage interface.
 *  <p>
 *  The MessageImpl instance contains the JsMsgObject which is the
 *  internal object which represents the Message.
 */
public class ControlMessageImpl extends MessageImpl implements ControlMessage {

  private final static long serialVersionUID = 1L;

  private final static TraceComponent tc = SibTr.register(ControlMessageImpl.class, MfpConstants.MSG_GROUP, MfpConstants.MSG_BUNDLE);

  /* **************************************************************************/
  /* Constructors                                                             */
  /* **************************************************************************/

  /**
   *  Constructor for a new Control message.
   *
   *  This constructor should never be used explicitly.
   *  It is only to be used implicitly by the sub-classes' no-parameter constructors.
   *  The method must not actually do anything.
   */
  ControlMessageImpl() {
  }

  /**
   *  Constructor for a new Control message.
   *
   *  This constructor should never be used explicitly.
   *  It is only to be used implicitly by the sub-classes' constructors.
   *
   *  @param flag No-op flag to distinguish different constructors.
   *
   *  @exception MessageDecodeFailedException Thrown if such a message can not be created
   */
  ControlMessageImpl(int flag) throws MessageDecodeFailedException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "<init>");

    setJmo(new JsMsgObject(ControlAccess.schema));

    /* The flags must be primed before use, & we always want Mediated=true    */
    jmo.setField(ControlAccess.FLAGS, Byte.valueOf(MEDIATED_FLAG));

    /* Default to no optional fields being included. */
    jmo.setChoiceField(ControlAccess.ROUTINGDESTINATION, ControlAccess.IS_ROUTINGDESTINATION_EMPTY);
    jmo.setChoiceField(ControlAccess.GUARANTEEDXBUS,     ControlAccess.IS_GUARANTEEDXBUS_EMPTY);
  }


  /**
   *  Constructor for an inbound message.
   *  (Only to be called by a ControlMessageFactory method.)
   *
   *  @param inJmo The JsMsgObject representing the inbound message.
   */
  ControlMessageImpl(JsMsgObject inJmo) {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "<init>", jmo);
    setJmo(inJmo);
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "<init>");
  }

  /* **************************************************************************/
  /* Method for encoding                                                      */
  /* **************************************************************************/

  /*
   *  Flatten the message into a List of DataSlices for for transmission.
   *
   *  Javadoc description supplied by ControlMessage interface.
   */
  public final List<DataSlice> encodeFast(Object conn) throws MessageEncodeFailedException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "encodeFast");

    // ControlMessages are always single part small messages, so we encode them
    // to a single DataSlice.
    List<DataSlice> slices = new ArrayList<DataSlice>(1);
    slices.add(jmo.encodeSinglePartMessage(conn));

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "encodeFast");
    return slices;
  }


  /* **************************************************************************/
  /* Get Methods                                                              */
  /* **************************************************************************/

  /*
   * Method to distinguish between Control messages and other message instances.
   *
   *  Javadoc description supplied by the AbstractMessage interface.
   */
  public boolean isControlMessage() {
    return true;
  }

  /*
   *  Get the value of the ControlMessageType from the  message.
   *
   *  Javadoc description supplied by ControlMessage interface.
   */
  public final ControlMessageType getControlMessageType() {
    /* Get the subtype then get the corresponding ControlMessageType to return  */
    Byte mType = (Byte)jmo.getField(ControlAccess.SUBTYPE);
    return ControlMessageType.getControlMessageType(mType);
  }

  /*
   *  Get the value of the Priority field from the message header.
   *
   *  Javadoc description supplied by ControlMessage interface.
   */
  public final Integer getPriority() {
    return (Integer)jmo.getField(ControlAccess.PRIORITY);
  }

  /*
   *  Get the value of the Reliability field from the message header.
   *
   *  Javadoc description supplied by ControlMessage interface.
   */
  public final Reliability getReliability() {
    /* Get the byte value and get the corresponding Reliability instance       */
    Byte rType = (Byte)jmo.getField(ControlAccess.RELIABILITY);
    return Reliability.getReliability(rType);
  }

  /*
   *  Determine whether the message has been mediated when the mediation point and
   *  queueing point are distributed.
   *
   *  Javadoc description supplied by CommonMessageHeaders interface.
   */
  public final boolean isMediated() {
    return getFlagValue(MEDIATED_FLAG);
  }

  /* ------------------------------------------------------------------------ */
  /* Guaranteed Delivery information                                          */
  /* ------------------------------------------------------------------------ */

  /*
   * Get the source Messaging Engine that originated the message.
   *
   *  Javadoc description supplied by CommonMessageHeaders interface.
   */
  public final SIBUuid8 getGuaranteedSourceMessagingEngineUUID() {
    byte[] b = (byte[])jmo.getField(ControlAccess.SOURCEMEUUID);
    if (b != null)
      return new SIBUuid8(b);
    return null;
  }

  /*
   *  Get the next Messaging Engine that will store the message.
   *
   *  Javadoc description supplied by CommonMessageHeaders interface.
   */
  public final SIBUuid8 getGuaranteedTargetMessagingEngineUUID() {
    byte[] b = (byte[])jmo.getField(ControlAccess.TARGETMEUUID);
    if (b != null)
      return new SIBUuid8(b);
    return null;
  }

  /*
   * Get the identity of the destination definition (not localisation)
   *
   *  Javadoc description supplied by CommonMessageHeaders interface.
   */
  public final SIBUuid12 getGuaranteedTargetDestinationDefinitionUUID() {
    byte[] b = (byte[])jmo.getField(ControlAccess.TARGETDESTDEFUUID);
    if (b != null)
      return new SIBUuid12(b);
    return null;
  }

  /*
   * Get the unique StreamId used by the flush protocol to determine
   * whether a stream is active or flushed.
   *
   *  Javadoc description supplied by CommonMessageHeaders interface.
   */
  public final SIBUuid12 getGuaranteedStreamUUID() {
    byte[] b = (byte[])jmo.getField(ControlAccess.STREAMUUID);
    if (b != null)
      return new SIBUuid12(b);
    return null;
  }

  /**
   * Get the unique GatheringTargetUUID.
   *
   * @return The unique GatheringTarget Id.
   *          If Guaranteed Delivery information is not included in the
   *          message, this field will not be set.
   */
  public final SIBUuid12 getGuaranteedGatheringTargetUUID() {
    byte[] b = (byte[])jmo.getField(ControlAccess.GATHERINGTARGETUUID_VALUE);
    if (b != null)
      return new SIBUuid12(b);
    return null;
  }

  /*
   * Get the class of protocol
   *
   *  Javadoc description supplied by CommonMessageHeaders interface.
   */
  public final ProtocolType getGuaranteedProtocolType() {
    Byte value = (Byte)jmo.getField(ControlAccess.PROTOCOLTYPE);
    return ProtocolType.getProtocolType(value);
  }

  /*
   * Get the version of the guaranteed delivery protocol used by this message.
   *
   *  Javadoc description supplied by CommonMessageHeaders interface.
   */
  public byte getGuaranteedProtocolVersion() {
    Byte value = (Byte)jmo.getField(ControlAccess.PROTOCOLVERSION);
    return (value == null) ? 0 : value.byteValue();
  }

  /* ------------------------------------------------------------------------ */
  /* Optional Guaranteed Delivery Cross-Bus information                       */
  /* ------------------------------------------------------------------------ */

  /*
   * Get the LinkName for cross-bus guaranteed delivery from the message.
   *
   *  Javadoc description supplied by CommonMessageHeaders interface.
   */
  public final String getGuaranteedCrossBusLinkName() {
    return (String)jmo.getField(ControlAccess.GUARANTEEDXBUS_SET_LINKNAME);
  }

  /*
   * Get the SourceBusUUID for cross-bus guaranteed delivery from the message.
   *
   *  Javadoc description supplied by CommonMessageHeaders interface.
   */
  public final SIBUuid8 getGuaranteedCrossBusSourceBusUUID() {
    byte[] b = (byte[])jmo.getField(ControlAccess.GUARANTEEDXBUS_SET_SOURCEBUSUUID);
    if (b != null)
      return new SIBUuid8(b);
    return null;
  }

  /* ------------------------------------------------------------------------ */
  /* Optional Routing Destination                                             */
  /* ------------------------------------------------------------------------ */

  /*
   *  Get the optional RoutingDestination field from the message header.
   *  The JsDeststinationAddress returned is a copy of the header field,
   *  so no updates to it affect the Message header itself.
   *
   *  Javadoc description supplied by CommonMessageHeaders interface.
   */
  public JsDestinationAddress getRoutingDestination() {
    String name = (String)jmo.getField(ControlAccess.ROUTINGDESTINATION_VALUE_NAME);
    /* If the name is null, the RoutingDestination has never been set.        */
    if (name != null) {
      byte[] b = (byte[])jmo.getField(ControlAccess.ROUTINGDESTINATION_VALUE_MEID);
      return new JsDestinationAddressImpl(name
                                         ,false
                                         ,(b == null) ? null : new SIBUuid8(b)
                                         ,(String)jmo.getField(ControlAccess.ROUTINGDESTINATION_VALUE_BUSNAME)
                                         );
    }
    else {
      return null;
    }
  }


  /* **************************************************************************/
  /* Set Methods                                                              */
  /* **************************************************************************/

  /**
   *  Set the value of the ControlMessageType in the  message.
   *
   *  @param The ControlMessageType singleton which distinguishes
   *         the type of this message.
   */
  final void setControlMessageType(ControlMessageType value) {
    jmo.setField(ControlAccess.SUBTYPE, value.toByte());
  }

  /*
   *  Set the value of the Priority field in the message header.
   *
   *  Javadoc description supplied by CommonMessageHeaders interface.
   */
  public final void setPriority(int value) {
    jmo.setIntField(ControlAccess.PRIORITY, value);
  }

  /*
   *  Set the value of the Reliability field in the message header.
   *
   *  Javadoc description supplied by ControlMessage interface.
   */
  public final void setReliability(Reliability value) {
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "setReliability to " + value);
    /* Get the int value of the Reliability instance and set it in the field  */
    jmo.setField(ControlAccess.RELIABILITY, value.toByte());
  }

  /*
   *  Set whether or not the message has been mediated when the mediation point and
   *  queueing point are distributed.
   *
   *  Javadoc description supplied by CommonMessageHeaders interface.
   */
  public final void setMediated(boolean value) {
    setFlagValue(MEDIATED_FLAG, value);
  }

  /* ------------------------------------------------------------------------ */
  /* Guaranteed Delivery information                                          */
  /* ------------------------------------------------------------------------ */

  /*
   *  Set the source Messaging Engine that originated the message.
   *
   *  Javadoc description supplied by CommonMessageHeaders interface.
   */
  public final void setGuaranteedSourceMessagingEngineUUID(SIBUuid8 value) {
    if (value != null)
      jmo.setField(ControlAccess.SOURCEMEUUID, value.toByteArray());
    else
      jmo.setField(ControlAccess.SOURCEMEUUID, null);
  }

  /*
   *  Set the next Messaging Engine that will store the message.
   *
   *  Javadoc description supplied by CommonMessageHeaders interface.
   */
  public final void setGuaranteedTargetMessagingEngineUUID(SIBUuid8 value) {
    if (value != null)
      jmo.setField(ControlAccess.TARGETMEUUID, value.toByteArray());
    else
      jmo.setField(ControlAccess.TARGETMEUUID, null);
  }

  /*
   *  Set the identity of the destination definition (not localisation)
   *
   *  Javadoc description supplied by CommonMessageHeaders interface.
   */
  public final void setGuaranteedTargetDestinationDefinitionUUID(SIBUuid12 value) {
    if (value != null)
      jmo.setField(ControlAccess.TARGETDESTDEFUUID, value.toByteArray());
    else
      jmo.setField(ControlAccess.TARGETDESTDEFUUID, null);
  }

  /*
   *  Set the unique StreamId used by the flush protocol to determine
   *  whether a stream is active or flushed.
   *
   *  Javadoc description supplied by CommonMessageHeaders interface.
   */
  public final void setGuaranteedStreamUUID(SIBUuid12 value) {
    if (value != null)
      jmo.setField(ControlAccess.STREAMUUID, value.toByteArray());
    else
      jmo.setField(ControlAccess.STREAMUUID, null);
  }

  /**
   * Set the unique GatheringTargetUUID.
   *
   * @param value The unique GatheringTarget Id.
   */
  public final void setGuaranteedGatheringTargetUUID(SIBUuid12 value) {
    if (value != null)
      jmo.setField(ControlAccess.GATHERINGTARGETUUID_VALUE, value.toByteArray());
    else
      jmo.setField(ControlAccess.GATHERINGTARGETUUID, ControlAccess.IS_GATHERINGTARGETUUID_EMPTY);
  }

  /*
   *  Set the class of protocol
   *
   *  Javadoc description supplied by CommonMessageHeaders interface.
   */
  public final void setGuaranteedProtocolType(ProtocolType value) {
    jmo.setField(ControlAccess.PROTOCOLTYPE, value.toByte());
  }

  /*
   *  Set the version of the guaranteed delivery protocol used by this message.
   *
   *  Javadoc description supplied by CommonMessageHeaders interface.
   */
  public void setGuaranteedProtocolVersion(byte value) {
    jmo.setField(ControlAccess.PROTOCOLVERSION, Byte.valueOf(value));
  }

  /* ------------------------------------------------------------------------ */
  /* Optional Guaranteed Delivery Cross-Bus information                       */
  /* ------------------------------------------------------------------------ */

  /*
   *  Set the LinkName for cross-bus guaranteed delivery into the message.
   *
   *  Javadoc description supplied by CommonMessageHeaders interface.
   */
  public final void setGuaranteedCrossBusLinkName(String value) {
    jmo.setField(ControlAccess.GUARANTEEDXBUS_SET_LINKNAME, value);
  }

  /*
   *  Set the SourceBusUUID for cross-bus guaranteed delivery into the message.
   *
   *  Javadoc description supplied by CommonMessageHeaders interface.
   */
  public final void setGuaranteedCrossBusSourceBusUUID(SIBUuid8 value) {
    if (value != null)
      jmo.setField(ControlAccess.GUARANTEEDXBUS_SET_SOURCEBUSUUID, value.toByteArray());
    else
      jmo.setField(ControlAccess.GUARANTEEDXBUS_SET_SOURCEBUSUUID, null);
  }

  /* ------------------------------------------------------------------------ */
  /* Optional Routing Destination                                             */
  /* ------------------------------------------------------------------------ */

  /*
   *  Set the optional RoutingDestination field in the message header.
   *
   *  Javadoc description supplied by CommonMessageHeaders interface.
   */
  public void setRoutingDestination(JsDestinationAddress value) {
    if (value != null) {
      jmo.setField(ControlAccess.ROUTINGDESTINATION_VALUE_NAME, value.getDestinationName());
      if (value.getME() != null) {
        jmo.setField(ControlAccess.ROUTINGDESTINATION_VALUE_MEID, value.getME().toByteArray());
      }
      else {
        jmo.setField(ControlAccess.ROUTINGDESTINATION_VALUE_MEID, null);
      }
      jmo.setField(ControlAccess.ROUTINGDESTINATION_VALUE_BUSNAME, value.getBusName());
    }
    else {
      jmo.setChoiceField(ControlAccess.ROUTINGDESTINATION, ControlAccess.IS_ROUTINGDESTINATION_EMPTY);
    }
  }


  /* **************************************************************************/
  /* Helper methods for writing back transient data                           */
  /* **************************************************************************/

  /**
   *  Helper method used by the JMO to rewrite any transient data back into the
   *  underlying JMF message.
   *  Package level visibility as used by the JMO.
   *
   *  Currently only the flags are transient and need explicitly writing back.
   *
   *  @param why The reason for the update
   *  @see com.ibm.ws.sib.mfp.MfpConstants
   */
  void updateDataFields(int why) {
    super.updateDataFields(why);
    setFlags();
  }


  /* **************************************************************************/
  /* Package and private methods for single bit flags                         */
  /* **************************************************************************/

  /* Each flag is represented by one bit of the byte                          */
  private static final byte MEDIATED_FLAG             = (byte)0x01;

  private transient boolean gotFlags = false;
  private transient byte flags;

  /**
   * Get the boolean 'value' of one of the flags in the FLAGS field of th message.
   *
   * @param flagBit  A byte with a single bit set on, marking the position
   *                 of the required flag.
   *
   * @return boolean true if the required flag is set, otherwise false
   */
  private final boolean getFlagValue(byte flagBit) {
    if ((getFlags() & flagBit) == 0) {
      return false;
    }
    else {
      return true;
    }
  }

  /**
   * Set the boolean 'value' of one of the flags in the FLAGS field of th message.
   *
   * @param flagBit  A byte with a single bit set on, marking the position
   *                 of the required flag.
   * @param value    true if the required flag is to be set on, otherwise false
   */
  private final void setFlagValue(byte flagBit, boolean value) {
    if (value) {
      flags = (byte)(getFlags() | flagBit);
    }
    else {
      flags = (byte)(getFlags() & (~flagBit));
    }
  }

  /**
   * Get the byte value of the FLAGS field from the cached variable or directly
   * from the message.
   *
   * @return byte The value of the (possibly cached) FLAGS field.
   */
  private final byte getFlags() {
    if (!gotFlags) {
      flags = ((Byte)jmo.getField(ControlAccess.FLAGS)).byteValue();
      gotFlags = true;
    }
    return flags;
  }

  /**
   * Set the chached value of the FLAGS field back into the message.
   */
  private final void setFlags() {
    if (gotFlags) {
      jmo.setField(ControlAccess.FLAGS, Byte.valueOf(flags));
      gotFlags = false;
    }
  }
  
  /**
   * Get the common prefix for all control message summary lines
   */
  public void getTraceSummaryLine(StringBuilder buff) {
    // Need to trim the control message name, as many contain space padding
    buff.append(getControlMessageType().toString().trim());
    // Display the bit flags in hex form
    buff.append(":flags=0x");
    buff.append(Integer.toHexString(getFlags() & 0xff));
  }

  /**
   * Helper method to append a long array to a summary string method
   */
  protected static void appendArray(StringBuilder buff, String name, long[] values) {
    buff.append(',');
    buff.append(name);
    buff.append("=[");
    if (values != null) {
      for (int i = 0; i < values.length; i++) {
        if (i != 0) buff.append(',');
        buff.append(values[i]);
      }
    }
    buff.append(']');
  }
  
  /**
   * Helper method to append an integer array to a summary string method
   */
  protected static void appendArray(StringBuilder buff, String name, int[] values) {
    buff.append(',');
    buff.append(name);
    buff.append("=[");
    if (values != null) {
      for (int i = 0; i < values.length; i++) {
        if (i != 0) buff.append(',');
        buff.append(values[i]);
      }
    }
    buff.append(']');
  }
  
  /**
   * Helper method to append a string array to a summary string method
   */
  protected static void appendArray(StringBuilder buff, String name, String[] values) {
    buff.append(',');
    buff.append(name);
    buff.append("=[");
    if (values != null) {
      for (int i = 0; i < values.length; i++) {
        if (i != 0) buff.append(',');
        buff.append(values[i]);
      }
    }
    buff.append(']');
  }
  
}
