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
package com.ibm.ws.sib.mfp.control;

import com.ibm.ws.sib.mfp.*;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * ControlMessageType is a type-safe enumeration which indicates the type of a
 * Control Message.
 */
public final class ControlMessageType implements IntAble {

  private static TraceComponent tc = SibTr.register(ControlMessageType.class, MfpConstants.MSG_GROUP, MfpConstants.MSG_BUNDLE);

  /** Integer value of the Control Message Types                                  */
  public final static int UNKNOWN_INT                       = 0;
  public final static int ACKEXPECTED_INT                   = 1;
  public final static int SILENCE_INT                       = 2;
  public final static int ACK_INT                           = 3;
  public final static int NACK_INT                          = 4;
  public final static int PREVALUE_INT                      = 5;
  public final static int ACCEPT_INT                        = 6;
  public final static int REJECT_INT                        = 7;
  public final static int DECISION_INT                      = 8;
  public final static int REQUEST_INT                       = 9;
  public final static int REQUESTACK_INT                    = 10;
  public final static int REQUESTHIGHESTGENERATEDTICK_INT   = 11;
  public final static int HIGHESTGENERATEDTICK_INT          = 12;
  public final static int RESETREQUESTACK_INT               = 13;
  public final static int RESETREQUESTACKACK_INT            = 14;
  public final static int BROWSEGET_INT                     = 15;
  public final static int BROWSEEND_INT                     = 16;
  public final static int BROWSESTATUS_INT                  = 17;
  public final static int COMPLETED_INT                     = 18;
  public final static int DECISIONEXPECTED_INT              = 19;
  public final static int CREATESTREAM_INT                  = 20;
  public final static int AREYOUFLUSHED_INT                 = 21;
  public final static int FLUSHED_INT                       = 22;
  public final static int NOTFLUSHED_INT                    = 23;
  public final static int REQUESTFLUSH_INT                  = 24;
  public final static int REQUESTCARDINALITYINFO_INT        = 25;
  public final static int CARDINALITYINFO_INT               = 26;
  public final static int CREATEDURABLE_INT                 = 27;
  public final static int DELETEDURABLE_INT                 = 28;
  public final static int DURABLECONFIRM_INT                = 29;


  /**  Constant denoting an indeterminate DDD Message  */
  public final static ControlMessageType UNKNOWN     = new ControlMessageType("UNKNOWN"     ,(byte)UNKNOWN_INT );

  /**  Constant denoting an Ack Expected Messge */
  public final static ControlMessageType ACKEXPECTED = new ControlMessageType("ACKEXPECTED" ,(byte)ACKEXPECTED_INT   );

  /**  Constant denoting a Silence Message  */
  public final static ControlMessageType SILENCE     = new ControlMessageType("SILENCE"     ,(byte)SILENCE_INT  );

  /**  Constant denoting an Ack Message  */
  public final static ControlMessageType ACK         = new ControlMessageType("ACK"         ,(byte)ACK_INT  );

  /**  Constant denoting a Nack Message  */
  public final static ControlMessageType NACK        = new ControlMessageType("NACK"        ,(byte)NACK_INT  );

  /**  Constant denoting a Prevalue Message  */
  public final static ControlMessageType PREVALUE    = new ControlMessageType("PREVALUE"    ,(byte)PREVALUE_INT  );

  /**  Constant denoting an Accept Message  */
  public final static ControlMessageType ACCEPT      = new ControlMessageType("ACCEPT"      ,(byte)ACCEPT_INT  );

  /**  Constant denoting a Reject Message  */
  public final static ControlMessageType REJECT      = new ControlMessageType("REJECT"      ,(byte)REJECT_INT  );

  /**  Constant denoting a Decision Message  */
  public final static ControlMessageType DECISION    = new ControlMessageType("DECISION"    ,(byte)DECISION_INT  );

  /**  Constant denoting a Request Message  */
  public final static ControlMessageType REQUEST                     = new ControlMessageType("REQUEST                    ",(byte)REQUEST_INT                     );

  /**  Constant denoting a RequestAck Message  */
  public final static ControlMessageType REQUESTACK                  = new ControlMessageType("REQUESTACK                 ",(byte)REQUESTACK_INT                  );

  /**  Constant denoting a RequestHighestGeneratedTick Message  */
  public final static ControlMessageType REQUESTHIGHESTGENERATEDTICK = new ControlMessageType("REQUESTHIGHESTGENERATEDTICK",(byte)REQUESTHIGHESTGENERATEDTICK_INT );

  /**  Constant denoting a HighestGeneratedTick Message  */
  public final static ControlMessageType HIGHESTGENERATEDTICK        = new ControlMessageType("HIGHESTGENERATEDTICK       ",(byte)HIGHESTGENERATEDTICK_INT        );

  /**  Constant denoting a RequestRequestAck Message  */
  public final static ControlMessageType RESETREQUESTACK             = new ControlMessageType("RESETREQUESTACK            ",(byte)RESETREQUESTACK_INT             );

  /**  Constant denoting a RequestRequestAckAck Message  */
  public final static ControlMessageType RESETREQUESTACKACK          = new ControlMessageType("RESETREQUESTACKACK         ",(byte)RESETREQUESTACKACK_INT          );

  /**  Constant denoting a BrowseGet Message  */
  public final static ControlMessageType BROWSEGET                   = new ControlMessageType("BROWSEGET                  ",(byte)BROWSEGET_INT                   );

  /**  Constant denoting a BrowseEnd Message  */
  public final static ControlMessageType BROWSEEND                   = new ControlMessageType("BROWSEEND                  ",(byte)BROWSEEND_INT                   );

  /**  Constant denoting a BrowseStatus Message  */
  public final static ControlMessageType BROWSESTATUS                = new ControlMessageType("BROWSESTATUS               ",(byte)BROWSESTATUS_INT                );

  /**  Constant denoting a Completed Message  */
  public final static ControlMessageType COMPLETED                   = new ControlMessageType("COMPLETED                  ",(byte)COMPLETED_INT                   );

  /**  Constant denoting a DecisionExpected Message  */
  public final static ControlMessageType DECISIONEXPECTED            = new ControlMessageType("DECISIONEXPECTED           ",(byte)DECISIONEXPECTED_INT            );

  /**  Constant denoting a CreateStream Message  */
  public final static ControlMessageType CREATESTREAM                = new ControlMessageType("CREATESTREAM               ",(byte)CREATESTREAM_INT                );

  /**  Constant denoting an AreYouFlushed Message  */
  public final static ControlMessageType AREYOUFLUSHED               = new ControlMessageType("AREYOUFLUSHED              ",(byte)AREYOUFLUSHED_INT               );

  /**  Constant denoting a Flushed Message  */
  public final static ControlMessageType FLUSHED                     = new ControlMessageType("FLUSHED                    ",(byte)FLUSHED_INT                     );

  /**  Constant denoting a NotFlushed Message  */
  public final static ControlMessageType NOTFLUSHED                  = new ControlMessageType("NOTFLUSHED                 ",(byte)NOTFLUSHED_INT                  );

  /**  Constant denoting a RequestFlushed Message  */
  public final static ControlMessageType REQUESTFLUSH                = new ControlMessageType("REQUESTFLUSH               ",(byte)REQUESTFLUSH_INT                );

  /**  Constant denoting a RequestCardinalityInfo Message  */
  public final static ControlMessageType REQUESTCARDINALITYINFO      = new ControlMessageType("REQUESTCARDINALITYINFO     ",(byte)REQUESTCARDINALITYINFO_INT      );

  /**  Constant denoting a CardinalityInfo Message  */
  public final static ControlMessageType CARDINALITYINFO             = new ControlMessageType("CARDINALITYINFO            ",(byte)CARDINALITYINFO_INT             );

  /** Constant denoting a CreateDurable Message */
  public final static ControlMessageType CREATEDURABLE               = new ControlMessageType("CREATEDURABLE              ",(byte)CREATEDURABLE_INT               );

  /**  Constant denoting a DeleteDurable Message */
  public final static ControlMessageType DELETEDURABLE               = new ControlMessageType("DELETEDURABLE              ",(byte)DELETEDURABLE_INT               );

  /**  Constant denoting a DurableConfirm Message */
  public final static ControlMessageType DURABLECONFIRM              = new ControlMessageType("DURABLECONFIRM             ",(byte)DURABLECONFIRM_INT               );

  /*  Array of defined ControlMessageTypes - needed by getControlMessageType  */
  private final static ControlMessageType[] set = {UNKNOWN
                                                  ,ACKEXPECTED
                                                  ,SILENCE
                                                  ,ACK
                                                  ,NACK
                                                  ,PREVALUE
                                                  ,ACCEPT
                                                  ,REJECT
                                                  ,DECISION
                                                  ,REQUEST
                                                  ,REQUESTACK
                                                  ,REQUESTHIGHESTGENERATEDTICK
                                                  ,HIGHESTGENERATEDTICK
                                                  ,RESETREQUESTACK
                                                  ,RESETREQUESTACKACK
                                                  ,BROWSEGET
                                                  ,BROWSEEND
                                                  ,BROWSESTATUS
                                                  ,COMPLETED
                                                  ,DECISIONEXPECTED
                                                  ,CREATESTREAM
                                                  ,AREYOUFLUSHED
                                                  ,FLUSHED
                                                  ,NOTFLUSHED
                                                  ,REQUESTFLUSH
                                                  ,REQUESTCARDINALITYINFO
                                                  ,CARDINALITYINFO
                                                  ,CREATEDURABLE
                                                  ,DELETEDURABLE
                                                  ,DURABLECONFIRM
                                                  };

  private String name;
  private Byte   value;
  private int    intValue;

  /* Private constructor - ensures the 'constants' defined here are the total set. */
  private ControlMessageType(String aName, byte aValue) {
    name  = aName;
    value = Byte.valueOf(aValue);
    intValue = (int)aValue;
  }

  /**
   * Returns the corresponding ControlMessageType for a given integer.
   * This method should NOT be called by any code outside the MFP component.
   * It is only public so that it can be accessed by sub-packages.
   *
   * @param  aValue         The integer for which an ControlMessageType is required.
   *
   * @return The corresponding ControlMessageType
   */
  public final static ControlMessageType getControlMessageType(Byte aValue) {
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc,"Value = " + aValue);
    return set[aValue.intValue()];
  }

  /**
   * Returns the Byte representation of the ControlMessageType.
   * This method should NOT be called by any code outside the MFP component.
   * It is only public so that it can be accessed by sub-packages.
   *
   * @return The Byte representation of the instance.
   */
  public final Byte toByte() {
    return value;
  }

  /**
   * Returns the integer representation of the ControlMessageType.
   * This method should NOT be called by any code outside the MFP component.
   * It is only public so that it can be accessed by sub-packages.
   *
   * @return  The int representation of the instance.
   */
  public final int toInt() {
    return intValue;
  }

  /**
   * Returns the name of the ControlMessageType.
   *
   * @return  The name of the instance.
   */
  public final String toString() {
    return name;
  }

}
