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

import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * MessageType is a type-safe enumeration for Message types.
 */
public final class MessageType {

  private static TraceComponent tc = SibTr.register(MessageType.class, MfpConstants.MSG_GROUP, MfpConstants.MSG_BUNDLE);

  
  // ******************************** WARNING **********************************
  // To ensure inter-release compatibility, the Message Type values must never
  // be changed, and the index into the array must match the byte value of the
  // MessageType in that position.
  // If a value is no longer used, it must not be 'removed' from the array.
  // If a new value is required, it must be added to the end of the sequence,
  // or replace an 'unused' value.
  // ***************************************************************************

  /** Constant denoting a currently unknown message type            */
  public  final static MessageType UNKNOWN      = new MessageType("UNKNOWN"           , (byte)0);

  /** Constant denoting Message Type - Topology & Routing           */
  public  final static MessageType TRM          = new MessageType("TRM"               , (byte)1);

  /** Constant denoting Message Type - this value no longer used    */
  public  final static MessageType UNUSED       = new MessageType("UNUSED"            , (byte)2);

  /** Constant denoting Message Type - JMS                          */
  public  final static MessageType JMS          = new MessageType("JMS"               , (byte)3);

  /** Constant denoting Message Type - Subscription Propagation     */
  public  final static MessageType SUBSCRIPTION = new MessageType("SUBSCRIPTION"      , (byte)4);

  /** Constant denoting Message Type - SDO                          */
  public  final static MessageType SDO          = new MessageType("SDO"               , (byte)5);

  /** Constants denoting Message Types ofr various MQ Broker messages */
  public  final static MessageType BROKER_CONTROL = new MessageType("BROKER_CONTROL"  , (byte)6);

  public  final static MessageType BROKER_RESPONSE = new MessageType("BROKER_RESPONSE", (byte)7);

  public  final static MessageType BROKER_ADMIN = new MessageType("BROKER_ADMIN"      , (byte)8);

  /*  Array of defined MessageTypes - needed by getMessageType    */
  private final static MessageType[] set = {UNKNOWN
                                           ,TRM
                                           ,UNUSED
                                           ,JMS
                                           ,SUBSCRIPTION
                                           ,SDO
                                           ,BROKER_CONTROL
                                           ,BROKER_RESPONSE
                                           ,BROKER_ADMIN
                                           };

  private String name;
  private Byte   value;

  /* Private constructor - ensures the 'constants' define here are the total set. */
  private MessageType(String aName, byte aValue) {
    name  = aName;
    value = new Byte(aValue);
  }

  /**
   * Returns the corresponding MessageType for a given Byte.
   * This method should NOT be called by any code outside the MFP component.
   * It is only public so that it can be accessed by sub-packages.
   *
   * @param  aValue         The Byte for which an MessageType is required.
   *
   * @return The corresponding MessageType
   */
  public final static MessageType getMessageType(Byte aValue) {
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc,"Value = " + aValue);
    return set[aValue.intValue()];
  }

  /**
   * Returns the Byte representation of the MessageType.
   * This method should NOT be called by any code outside the MFP component.
   * It is only public so that it can be accessed by sub-packages.
   *
   * @return The Byte representation of the instance.
   */
  public final Byte toByte() {
    return value;
  }

  /**
   * Returns the name of the MessageType.
   *
   * @return The name of the instance.
   */
  public final String toString() {
    return name;
  }

}
