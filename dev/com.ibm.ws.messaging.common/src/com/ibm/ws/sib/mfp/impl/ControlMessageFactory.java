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

import java.util.List;

import com.ibm.ws.sib.mfp.MessageCreateFailedException;
import com.ibm.ws.sib.mfp.MessageDecodeFailedException;
import com.ibm.ws.sib.mfp.MfpConstants;
import com.ibm.ws.sib.mfp.control.ControlAccept;
import com.ibm.ws.sib.mfp.control.ControlAck;
import com.ibm.ws.sib.mfp.control.ControlAckExpected;
import com.ibm.ws.sib.mfp.control.ControlAreYouFlushed;
import com.ibm.ws.sib.mfp.control.ControlBrowseEnd;
import com.ibm.ws.sib.mfp.control.ControlBrowseGet;
import com.ibm.ws.sib.mfp.control.ControlBrowseStatus;
import com.ibm.ws.sib.mfp.control.ControlCardinalityInfo;
import com.ibm.ws.sib.mfp.control.ControlCompleted;
import com.ibm.ws.sib.mfp.control.ControlCreateDurable;
import com.ibm.ws.sib.mfp.control.ControlCreateStream;
import com.ibm.ws.sib.mfp.control.ControlDecision;
import com.ibm.ws.sib.mfp.control.ControlDecisionExpected;
import com.ibm.ws.sib.mfp.control.ControlDeleteDurable;
import com.ibm.ws.sib.mfp.control.ControlDurableConfirm;
import com.ibm.ws.sib.mfp.control.ControlFlushed;
import com.ibm.ws.sib.mfp.control.ControlHighestGeneratedTick;
import com.ibm.ws.sib.mfp.control.ControlMessage;
import com.ibm.ws.sib.mfp.control.ControlNack;
import com.ibm.ws.sib.mfp.control.ControlNotFlushed;
import com.ibm.ws.sib.mfp.control.ControlPrevalue;
import com.ibm.ws.sib.mfp.control.ControlReject;
import com.ibm.ws.sib.mfp.control.ControlRequest;
import com.ibm.ws.sib.mfp.control.ControlRequestAck;
import com.ibm.ws.sib.mfp.control.ControlRequestCardinalityInfo;
import com.ibm.ws.sib.mfp.control.ControlRequestFlush;
import com.ibm.ws.sib.mfp.control.ControlRequestHighestGeneratedTick;
import com.ibm.ws.sib.mfp.control.ControlResetRequestAck;
import com.ibm.ws.sib.mfp.control.ControlResetRequestAckAck;
import com.ibm.ws.sib.mfp.control.ControlSilence;
import com.ibm.ws.sib.mfp.control.SubscriptionMessage;
import com.ibm.ws.sib.utils.DataSlice;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;

/**
 * A singleton ControlMessageFactory is created at static initialization
 * and is subsequently used for the creation of all new and inbound CONTROL Messages
 * of any sub-type.
 */
public abstract class ControlMessageFactory {

  private final static String CONTROL_MESSAGE_FACTORY_CLASS    = "com.ibm.ws.sib.mfp.impl.ControlMessageFactoryImpl";

  private static TraceComponent tc = SibTr.register(ControlMessageFactory.class, MfpConstants.MSG_GROUP, MfpConstants.MSG_BUNDLE);

  private static ControlMessageFactory instance = null;
  private static Exception  createException = null;

  static {
  
    /* Create the singleton factory instance                                  */
    try {
      createFactoryInstance();
    }
    catch (Exception e) {
      FFDCFilter.processException(e, "com.ibm.ws.sib.mfp.impl.ControlMessageFactory.<clinit>", "68");
      createException = e;
    }
  }

  /**
   *  Get the singleton ControlMessageFactory which is to be used for
   *  creating all Message Processor Control Message instances.
   *
   *  @return The ControlMessageFactory
   *
   *  @exception Exception The method rethrows any Exception caught during
   *                       creaton of the singleton factory.
   */
  public static ControlMessageFactory getInstance() throws Exception {

    /* If instance creation failed, throw on the Exception                    */
    if (instance == null) {
      throw createException;
    }

    /* Otherwise, return the singleton                                        */
    return instance;
  }


  /**
   *  Create a ControlMessage to represent an inbound message.
   *  (To be called by the Communications component.)
   *
   *  @param slices      The List of DataSlice(s) representing the inbound message
   *
   *  @return The new ControlMessage
   *
   *  @exception MessageDecodeFailedException Thrown if the inbound message could not be decoded
   */
  public abstract ControlMessage createInboundControlMessage(List<DataSlice> slices)
                                                  throws MessageDecodeFailedException;

  /**
   *  Create a ControlMessage to represent an inbound message.
   *  (To be called by the Communications component.)
   *
   *  @param rawMessage  The inbound byte array containging a complete message
   *  @param offset      The offset within the byte array at which the message begins
   *  @param length      The length of the message within the byte array
   *
   *  @return The new ControlMessage
   *
   *  @exception MessageDecodeFailedException Thrown if the inbound message could not be decoded
   */
  public abstract ControlMessage createInboundControlMessage(byte rawMessage[], int offset, int length)
                                                  throws MessageDecodeFailedException;

  /**
   *  Create a new, empty Subscription Propagation message
   *
   *  @return The new SubscriptionMessage
   *
   *  @exception MessageCreateFailedException Thrown if such a message can not be created
   */
  public abstract SubscriptionMessage createNewSubscriptionMessage() throws MessageCreateFailedException;

  /**
   *  Create a new, empty ControlAckExpected Message
   *
   *  @return The new ControlAckExpected
   *
   *  @exception MessageCreateFailedException Thrown if such a message can not be created
   */
  public abstract ControlAckExpected createNewControlAckExpected() throws MessageCreateFailedException;

  /**
   *  Create a new, empty ControlSilence Message
   *
   *  @return The new ControlSilence
   *
   *  @exception MessageCreateFailedException Thrown if such a message can not be created
   */
  public abstract ControlSilence createNewControlSilence() throws MessageCreateFailedException;

  /**
   *  Create a new, empty ControlAck Message
   *
   *  @return The new ControlAck
   *
   *  @exception MessageCreateFailedException Thrown if such a message can not be created
   */
  public abstract ControlAck createNewControlAck() throws MessageCreateFailedException;

  /**
   *  Create a new, empty ControlNack Message
   *
   *  @return The new ControlNack
   *
   *  @exception MessageCreateFailedException Thrown if such a message can not be created
   */
  public abstract ControlNack createNewControlNack() throws MessageCreateFailedException;

  /**
   *  Create a new, empty ControlPrevalue Message
   *
   *  @return The new ControlPrevalue
   *
   *  @exception MessageCreateFailedException Thrown if such a message can not be created
   */
  public abstract ControlPrevalue createNewControlPrevalue() throws MessageCreateFailedException;

  /**
   *  Create a new, empty ControlAccept Message
   *
   *  @return The new ControlAccept
   *
   *  @exception MessageCreateFailedException Thrown if such a message can not be created
   */
  public abstract ControlAccept createNewControlAccept() throws MessageCreateFailedException;

  /**
   *  Create a new, empty ControlReject Message
   *
   *  @return The new ControlReject
   *
   *  @exception MessageCreateFailedException Thrown if such a message can not be created
   */
  public abstract ControlReject createNewControlReject() throws MessageCreateFailedException;

  /**
   *  Create a new, empty ControlDecision Message
   *
   *  @return The new ControlDecision
   *
   *  @exception MessageCreateFailedException Thrown if such a message can not be created
   */
  public abstract ControlDecision createNewControlDecision() throws MessageCreateFailedException;

  /**
   *  Create a new, empty ControlRequest Message
   *
   *  @return The new ControlRequest
   *
   *  @exception MessageCreateFailedException Thrown if such a message can not be created
   */
  public abstract ControlRequest createNewControlRequest() throws MessageCreateFailedException;

  /**
   *  Create a new, empty ControlRequestAck Message
   *
   *  @return The new ControlRequestAck
   *
   *  @exception MessageCreateFailedException Thrown if such a message can not be created
   */
  public abstract ControlRequestAck createNewControlRequestAck() throws MessageCreateFailedException;

  /**
   *  Create a new, empty ControlRequestHighestGeneratedTick Message
   *
   *  @return The new ControlRequestHighestGeneratedTick
   *
   *  @exception MessageCreateFailedException Thrown if such a message can not be created
   */
  public abstract ControlRequestHighestGeneratedTick createNewControlRequestHighestGeneratedTick() throws MessageCreateFailedException;

  /**
   *  Create a new, empty ControlHighestGeneratedTick Message
   *
   *  @return The new ControlHighestGeneratedTick
   *
   *  @exception MessageCreateFailedException Thrown if such a message can not be created
   */
  public abstract ControlHighestGeneratedTick createNewControlHighestGeneratedTick() throws MessageCreateFailedException;

  /**
   *  Create a new, empty ControlResetRequestAck Message
   *
   *  @return The new ControlResetRequestAck
   *
   *  @exception MessageCreateFailedException Thrown if such a message can not be created
   */
  public abstract ControlResetRequestAck createNewControlResetRequestAck() throws MessageCreateFailedException;

  /**
   *  Create a new, empty ControlResetRequestAckAck Message
   *
   *  @return The new ControlResetRequestAckAck
   *
   *  @exception MessageCreateFailedException Thrown if such a message can not be created
   */
  public abstract ControlResetRequestAckAck createNewControlResetRequestAckAck() throws MessageCreateFailedException;

  /**
   *  Create a new, empty ControlBrowseGet Message
   *
   *  @return The new ControlBrowseGet
   *
   *  @exception MessageCreateFailedException Thrown if such a message can not be created
   */
  public abstract ControlBrowseGet createNewControlBrowseGet() throws MessageCreateFailedException;

  /**
   *  Create a new, empty ControlBrowseEnd Message
   *
   *  @return The new ControlBrowseEnd
   *
   *  @exception MessageCreateFailedException Thrown if such a message can not be created
   */
  public abstract ControlBrowseEnd createNewControlBrowseEnd() throws MessageCreateFailedException;

  /**
   *  Create a new, empty ControlBrowseStatus Message
   *
   *  @return The new ControlBrowseStatus
   *
   *  @exception MessageCreateFailedException Thrown if such a message can not be created
   */
  public abstract ControlBrowseStatus createNewControlBrowseStatus() throws MessageCreateFailedException;

  /**
   *  Create a new, empty ControlCompleted Message
   *
   *  @return The new ControlCompleted
   *
   *  @exception MessageCreateFailedException Thrown if such a message can not be created
   */
  public abstract ControlCompleted createNewControlCompleted() throws MessageCreateFailedException;

  /**
   *  Create a new, empty ControlDecisionExpected Message
   *
   *  @return The new ControlDecisionExpected
   *
   *  @exception MessageCreateFailedException Thrown if such a message can not be created
   */
  public abstract ControlDecisionExpected createNewControlDecisionExpected() throws MessageCreateFailedException;

  /**
   *  Create a new, empty ControlCreateStream Message
   *
   *  @return The new ControlCreateStream
   *
   *  @exception MessageCreateFailedException Thrown if such a message can not be created
   */
  public abstract ControlCreateStream createNewControlCreateStream() throws MessageCreateFailedException;

  /**
   *  Create a new, empty ControlAreYouFlushed Message
   *
   *  @return The new ControlAreYouFlushed
   *
   *  @exception MessageCreateFailedException Thrown if such a message can not be created
   */
  public abstract ControlAreYouFlushed createNewControlAreYouFlushed() throws MessageCreateFailedException;

  /**
   *  Create a new, empty ControlFlushed Message
   *
   *  @return The new ControlFlushed
   *
   *  @exception MessageCreateFailedException Thrown if such a message can not be created
   */
  public abstract ControlFlushed createNewControlFlushed() throws MessageCreateFailedException;

  /**
   *  Create a new, empty ControlNotFlushed Message
   *
   *  @return The new ControlNotFlushed
   *
   *  @exception MessageCreateFailedException Thrown if such a message can not be created
   */
  public abstract ControlNotFlushed createNewControlNotFlushed() throws MessageCreateFailedException;

  /**
   *  Create a new, empty ControlRequestFlush Message
   *
   *  @return The new ControlRequestFlush
   *
   *  @exception MessageCreateFailedException Thrown if such a message can not be created
   */
  public abstract ControlRequestFlush createNewControlRequestFlush() throws MessageCreateFailedException;

  /**
   *  Create a new, empty ControlRequestCardinalityInfo Message
   *
   *  @return The new ControlRequestCardinalityInfo
   *
   *  @exception MessageCreateFailedException Thrown if such a message can not be created
   */
  public abstract ControlRequestCardinalityInfo createNewControlRequestCardinalityInfo() throws MessageCreateFailedException;

  /**
   *  Create a new, empty ControlCardinalityInfo Message
   *
   *  @return The new ControlCardinalityInfo
   *
   *  @exception MessageCreateFailedException Thrown if such a message can not be created
   */
  public abstract ControlCardinalityInfo createNewControlCardinalityInfo() throws MessageCreateFailedException;

  /**
   * Create a new, empty ControlCreateDurable Message
   *
   * @return The new ControlCreateDurable
   *
   * @throws MessageCreateFailedException Thrown if such a message can not be created
   */
  public abstract ControlCreateDurable createNewControlCreateDurable() throws MessageCreateFailedException;

  /**
   *  Create a new, empty ControlDeleteDurable Message
   *
   *  @return The new ControlDeleteDurable
   *
   *  @exception MessageCreateFailedException Thrown if such a message can not be created
   */
  public abstract ControlDeleteDurable createNewControlDeleteDurable() throws MessageCreateFailedException;

  /**
   *  Create a new, empty ControlDurableConfirm Message
   *
   *  @return The new ControlDurableConfirm
   *
   *  @exception MessageCreateFailedException Thrown if such a message can not be created
   */
  public abstract ControlDurableConfirm createNewControlDurableConfirm() throws MessageCreateFailedException;


  /**
   *  Create the singleton Factory instance.
   *
   *  @exception Exception The method rethrows any Exception caught during
   *                       creaton of the singleton factory.
   */
  private static void createFactoryInstance() throws Exception {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "createFactoryInstance");
    try {
      Class cls = Class.forName(CONTROL_MESSAGE_FACTORY_CLASS);
      instance = (ControlMessageFactory) cls.newInstance();
    }
    catch (Exception e) {
      FFDCFilter.processException(e, "com.ibm.ws.sib.mfp.ControlMessageFactory.createFactoryInstance", "112");
      SibTr.error(tc,"UNABLE_TO_CREATE_CONTROLFACTORY_CWSIF0041",e);
      throw e;
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "createFactoryInstance");
  }
}
