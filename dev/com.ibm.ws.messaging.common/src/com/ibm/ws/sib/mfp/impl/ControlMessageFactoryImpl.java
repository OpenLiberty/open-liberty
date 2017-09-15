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

import com.ibm.ws.sib.mfp.control.ControlAccept;
import com.ibm.ws.sib.mfp.control.ControlAck;
import com.ibm.ws.sib.mfp.control.ControlAckExpected;
import com.ibm.ws.sib.mfp.control.ControlCreateDurable;
import com.ibm.ws.sib.mfp.control.ControlDecision;
import com.ibm.ws.sib.mfp.control.ControlDeleteDurable;
import com.ibm.ws.sib.mfp.control.ControlDurableConfirm;
import com.ibm.ws.sib.mfp.control.ControlMessage;
import com.ibm.ws.sib.mfp.impl.ControlMessageFactory;
import com.ibm.ws.sib.mfp.control.ControlMessageType;
import com.ibm.ws.sib.mfp.control.ControlNack;
import com.ibm.ws.sib.mfp.control.ControlPrevalue;
import com.ibm.ws.sib.mfp.control.ControlReject;
import com.ibm.ws.sib.mfp.control.ControlSilence;
import com.ibm.ws.sib.mfp.control.ControlRequest;
import com.ibm.ws.sib.mfp.control.ControlRequestAck;
import com.ibm.ws.sib.mfp.control.ControlRequestHighestGeneratedTick;
import com.ibm.ws.sib.mfp.control.ControlHighestGeneratedTick;
import com.ibm.ws.sib.mfp.control.ControlResetRequestAck;
import com.ibm.ws.sib.mfp.control.ControlResetRequestAckAck;
import com.ibm.ws.sib.mfp.control.ControlBrowseGet;
import com.ibm.ws.sib.mfp.control.ControlBrowseEnd;
import com.ibm.ws.sib.mfp.control.ControlBrowseStatus;
import com.ibm.ws.sib.mfp.control.ControlCompleted;
import com.ibm.ws.sib.mfp.control.ControlDecisionExpected;
import com.ibm.ws.sib.mfp.control.ControlCreateStream;
import com.ibm.ws.sib.mfp.control.ControlAreYouFlushed;
import com.ibm.ws.sib.mfp.control.ControlFlushed;
import com.ibm.ws.sib.mfp.control.ControlNotFlushed;
import com.ibm.ws.sib.mfp.control.ControlRequestFlush;
import com.ibm.ws.sib.mfp.control.ControlRequestCardinalityInfo;
import com.ibm.ws.sib.mfp.control.ControlCardinalityInfo;
import com.ibm.ws.sib.mfp.control.SubscriptionMessage;
import com.ibm.ws.sib.mfp.MessageCreateFailedException;
import com.ibm.ws.sib.mfp.MessageDecodeFailedException;
import com.ibm.ws.sib.mfp.MfpConstants;
import com.ibm.ws.sib.mfp.schema.ControlAccess;
import com.ibm.ws.sib.utils.DataSlice;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.websphere.ras.TraceComponent;

/**
 *  This class extends the abstract com.ibm.ws.sib.mfp.trm.ControlMessageFactory
 *  class and provides the concrete implementations of the methods for
 *  creating ControlMessages.
 *  <p>
 *  The class must be public so that the abstract class static
 *  initialization can create an instance of it at runtime.
 *
 */
public final class ControlMessageFactoryImpl extends ControlMessageFactory {

  private static TraceComponent tc = SibTr.register(ControlMessageFactoryImpl.class, MfpConstants.MSG_GROUP, MfpConstants.MSG_BUNDLE);

  // Ensure we register all the JMF schemas needed to process these messages
  static {
    SchemaManager.ensureInitialized();                                          // d380323
  }

  /**
   *  Create a new, empty Subscription Propagation message
   *
   *  @return The new SubscriptionMessage
   *
   *  @exception MessageCreateFailedException Thrown if such a message can not be created
   */
  public final SubscriptionMessage createNewSubscriptionMessage() throws MessageCreateFailedException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "createNewSubscriptionMessage");
    SubscriptionMessage msg = null;
    try {
      msg = new SubscriptionMessageImpl(MfpConstants.CONSTRUCTOR_NO_OP);
    }
    catch (MessageDecodeFailedException e) {
      /* No need to FFDC this as JsMsgObject will already have done so */
      // No FFDC code needed
      throw new MessageCreateFailedException(e);
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "createNewSubscriptionMessage");
    return msg;
  }

  /**
   *  Create a new, empty ControlAckExpected message
   *
   *  @return The new ControlAckExpected
   *
   *  @exception MessageCreateFailedException Thrown if such a message can not be created
   */
  public final ControlAckExpected createNewControlAckExpected() throws MessageCreateFailedException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "createNewControlAckExpected");
    ControlAckExpected msg = null;
    try {
      msg = new ControlAckExpectedImpl(MfpConstants.CONSTRUCTOR_NO_OP);
    }
    catch (MessageDecodeFailedException e) {
      /* No need to FFDC this as JsMsgObject will already have done so */
      // No FFDC code needed
      throw new MessageCreateFailedException(e);
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "createNewControlAckExpected");
    return msg;
  }

  /**
   *  Create a new, empty ControlSilence message
   *
   *  @return The new ControlSilence
   *
   *  @exception MessageCreateFailedException Thrown if such a message can not be created
   */
  public final ControlSilence createNewControlSilence() throws MessageCreateFailedException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "createNewControlSilence");
    ControlSilence msg = null;
    try {
      msg = new ControlSilenceImpl(MfpConstants.CONSTRUCTOR_NO_OP);
    }
    catch (MessageDecodeFailedException e) {
      /* No need to FFDC this as JsMsgObject will already have done so */
      // No FFDC code needed
      throw new MessageCreateFailedException(e);
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "createNewControlSilence");
    return msg;
  }

  /**
   *  Create a new, empty ControlAck message
   *
   *  @return The new ControlAck
   *
   *  @exception MessageCreateFailedException Thrown if such a message can not be created
   */
  public final ControlAck createNewControlAck() throws MessageCreateFailedException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "createNewControlAck");
    ControlAck msg = null;
    try {
      msg = new ControlAckImpl(MfpConstants.CONSTRUCTOR_NO_OP);
    }
    catch (MessageDecodeFailedException e) {
      /* No need to FFDC this as JsMsgObject will already have done so */
      // No FFDC code needed
      throw new MessageCreateFailedException(e);
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "createNewControlAck");
    return msg;
  }

  /**
   *  Create a new, empty ControlNack message
   *
   *  @return The new ControlNack
   *
   *  @exception MessageCreateFailedException Thrown if such a message can not be created
   */
  public final ControlNack createNewControlNack() throws MessageCreateFailedException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "createNewControlNack");
    ControlNack msg = null;
    try {
      msg = new ControlNackImpl(MfpConstants.CONSTRUCTOR_NO_OP);
    }
    catch (MessageDecodeFailedException e) {
      /* No need to FFDC this as JsMsgObject will already have done so */
      // No FFDC code needed
      throw new MessageCreateFailedException(e);
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "createNewControlNack");
    return msg;
  }

  /**
   *  Create a new, empty ControlPrevalue message
   *
   *  @return The new ControlPrevalue
   *
   *  @exception MessageCreateFailedException Thrown if such a message can not be created
   */
  public final ControlPrevalue createNewControlPrevalue() throws MessageCreateFailedException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "createNewControlPrevalue");
    ControlPrevalue msg = null;
    try {
      msg = new ControlPrevalueImpl(MfpConstants.CONSTRUCTOR_NO_OP);
    }
    catch (MessageDecodeFailedException e) {
      /* No need to FFDC this as JsMsgObject will already have done so */
      // No FFDC code needed
      throw new MessageCreateFailedException(e);
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "createNewControlPrevalue");
    return msg;
  }

  /**
   *  Create a new, empty ControlAccept message
   *
   *  @return The new ControlAccept
   *
   *  @exception MessageCreateFailedException Thrown if such a message can not be created
   */
  public final ControlAccept createNewControlAccept() throws MessageCreateFailedException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "createNewControlAccept");
    ControlAccept msg = null;
    try {
      msg = new ControlAcceptImpl(MfpConstants.CONSTRUCTOR_NO_OP);
    }
    catch (MessageDecodeFailedException e) {
      /* No need to FFDC this as JsMsgObject will already have done so */
      // No FFDC code needed
      throw new MessageCreateFailedException(e);
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "createNewControlAccept");
    return msg;
  }

  /**
   *  Create a new, empty ControlReject message
   *
   *  @return The new ControlReject
   *
   *  @exception MessageCreateFailedException Thrown if such a message can not be created
   */
  public final ControlReject createNewControlReject() throws MessageCreateFailedException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "createNewControlReject");
    ControlReject msg = null;
    try {
      msg = new ControlRejectImpl(MfpConstants.CONSTRUCTOR_NO_OP);
    }
    catch (MessageDecodeFailedException e) {
      /* No need to FFDC this as JsMsgObject will already have done so */
      // No FFDC code needed
      throw new MessageCreateFailedException(e);
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "createNewControlReject");
    return msg;
  }

  /**
   *  Create a new, empty ControlDecision message
   *
   *  @return The new ControlDecision
   *
   *  @exception MessageCreateFailedException Thrown if such a message can not be created
   */
  public final ControlDecision createNewControlDecision() throws MessageCreateFailedException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "createNewControlDecision");
    ControlDecision msg = null;
    try {
      msg = new ControlDecisionImpl(MfpConstants.CONSTRUCTOR_NO_OP);
    }
    catch (MessageDecodeFailedException e) {
      /* No need to FFDC this as JsMsgObject will already have done so */
      // No FFDC code needed
      throw new MessageCreateFailedException(e);
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "createNewControlDecision");
    return msg;
  }

  /**
   *  Create a new, empty ControlRequest Message
   *
   *  @return The new ControlRequest
   *
   *  @exception MessageCreateFailedException Thrown if such a message can not be created
   */
  public final ControlRequest createNewControlRequest() throws MessageCreateFailedException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "createNewControlRequest");
    ControlRequest msg = null;
    try {
      msg = new ControlRequestImpl(MfpConstants.CONSTRUCTOR_NO_OP);
    }
    catch (MessageDecodeFailedException e) {
      /* No need to FFDC this as JsMsgObject will already have done so */
      // No FFDC code needed
      throw new MessageCreateFailedException(e);
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "createNewControlRequest");
    return msg;
  }

  /**
   *  Create a new, empty ControlRequestAck Message
   *
   *  @return The new ControlRequestAck
   *
   *  @exception MessageCreateFailedException Thrown if such a message can not be created
   */
  public final ControlRequestAck createNewControlRequestAck() throws MessageCreateFailedException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "createNewControlRequestAck");
    ControlRequestAck msg = null;
    try {
      msg = new ControlRequestAckImpl(MfpConstants.CONSTRUCTOR_NO_OP);
    }
    catch (MessageDecodeFailedException e) {
      /* No need to FFDC this as JsMsgObject will already have done so */
      // No FFDC code needed
      throw new MessageCreateFailedException(e);
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "createNewControlRequestAck");
    return msg;
  }

  /**
   *  Create a new, empty ControlRequestHighestGeneratedTick Message
   *
   *  @return The new ControlRequestHighestGeneratedTick
   *
   *  @exception MessageCreateFailedException Thrown if such a message can not be created
   */
  public final ControlRequestHighestGeneratedTick createNewControlRequestHighestGeneratedTick() throws MessageCreateFailedException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "createNewControlRequestHighestGeneratedTick");
    ControlRequestHighestGeneratedTick msg = null;
    try {
      msg = new ControlRequestHighestGeneratedTickImpl(MfpConstants.CONSTRUCTOR_NO_OP);
    }
    catch (MessageDecodeFailedException e) {
      /* No need to FFDC this as JsMsgObject will already have done so */
      // No FFDC code needed
      throw new MessageCreateFailedException(e);
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "createNewControlRequestHighestGeneratedTick");
    return msg;
  }

  /**
   *  Create a new, empty ControlHighestGeneratedTick Message
   *
   *  @return The new ControlHighestGeneratedTick
   *
   *  @exception MessageCreateFailedException Thrown if such a message can not be created
   */
  public final ControlHighestGeneratedTick createNewControlHighestGeneratedTick() throws MessageCreateFailedException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "createNewControlHighestGeneratedTick");
    ControlHighestGeneratedTick msg = null;
    try {
      msg = new ControlHighestGeneratedTickImpl(MfpConstants.CONSTRUCTOR_NO_OP);
    }
    catch (MessageDecodeFailedException e) {
      /* No need to FFDC this as JsMsgObject will already have done so */
      // No FFDC code needed
      throw new MessageCreateFailedException(e);
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "createNewControlHighestGeneratedTick");
    return msg;
  }

  /**
   *  Create a new, empty ControlResetRequestAck Message
   *
   *  @return The new ControlResetRequestAck
   *
   *  @exception MessageCreateFailedException Thrown if such a message can not be created
   */
  public final ControlResetRequestAck createNewControlResetRequestAck() throws MessageCreateFailedException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "createNewControlResetRequestAck");
    ControlResetRequestAck msg = null;
    try {
      msg = new ControlResetRequestAckImpl(MfpConstants.CONSTRUCTOR_NO_OP);
    }
    catch (MessageDecodeFailedException e) {
      /* No need to FFDC this as JsMsgObject will already have done so */
      // No FFDC code needed
      throw new MessageCreateFailedException(e);
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "createNewControlResetRequestAck");
    return msg;
  }

  /**
   *  Create a new, empty ControlResetRequestAckAck Message
   *
   *  @return The new ControlResetRequestAckAck
   *
   *  @exception MessageCreateFailedException Thrown if such a message can not be created
   */
  public final ControlResetRequestAckAck createNewControlResetRequestAckAck() throws MessageCreateFailedException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "createNewControlResetRequestAckAck");
    ControlResetRequestAckAck msg = null;
    try {
      msg = new ControlResetRequestAckAckImpl(MfpConstants.CONSTRUCTOR_NO_OP);
    }
    catch (MessageDecodeFailedException e) {
      /* No need to FFDC this as JsMsgObject will already have done so */
      // No FFDC code needed
      throw new MessageCreateFailedException(e);
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "createNewControlResetRequestAckAck");
    return msg;
  }

  /**
   *  Create a new, empty ControlBrowseGet Message
   *
   *  @return The new ControlBrowseGet
   *
   *  @exception MessageCreateFailedException Thrown if such a message can not be created
   */
  public final ControlBrowseGet createNewControlBrowseGet() throws MessageCreateFailedException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "createNewControlBrowseGet");
    ControlBrowseGet msg = null;
    try {
      msg = new ControlBrowseGetImpl(MfpConstants.CONSTRUCTOR_NO_OP);
    }
    catch (MessageDecodeFailedException e) {
      /* No need to FFDC this as JsMsgObject will already have done so */
      // No FFDC code needed
      throw new MessageCreateFailedException(e);
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "createNewControlBrowseGet");
    return msg;
  }

  /**
   *  Create a new, empty ControlBrowseEnd Message
   *
   *  @return The new ControlBrowseEnd
   *
   *  @exception MessageCreateFailedException Thrown if such a message can not be created
   */
  public final ControlBrowseEnd createNewControlBrowseEnd() throws MessageCreateFailedException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "createNewControlBrowseEnd");
    ControlBrowseEnd msg = null;
    try {
      msg = new ControlBrowseEndImpl(MfpConstants.CONSTRUCTOR_NO_OP);
    }
    catch (MessageDecodeFailedException e) {
      /* No need to FFDC this as JsMsgObject will already have done so */
      // No FFDC code needed
      throw new MessageCreateFailedException(e);
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "createNewControlBrowseEnd");
    return msg;
  }

  /**
   *  Create a new, empty ControlBrowseStatus Message
   *
   *  @return The new ControlBrowseStatus
   *
   *  @exception MessageCreateFailedException Thrown if such a message can not be created
   */
  public final ControlBrowseStatus createNewControlBrowseStatus() throws MessageCreateFailedException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "createNewControlBrowseStatus");
    ControlBrowseStatus msg = null;
    try {
      msg = new ControlBrowseStatusImpl(MfpConstants.CONSTRUCTOR_NO_OP);
    }
    catch (MessageDecodeFailedException e) {
      /* No need to FFDC this as JsMsgObject will already have done so */
      // No FFDC code needed
      throw new MessageCreateFailedException(e);
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "createNewControlBrowseStatus");
    return msg;
  }

  /**
   *  Create a new, empty ControlCompleted Message
   *
   *  @return The new ControlCompleted
   *
   *  @exception MessageCreateFailedException Thrown if such a message can not be created
   */
  public final ControlCompleted createNewControlCompleted() throws MessageCreateFailedException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "createNewControlCompleted");
    ControlCompleted msg = null;
    try {
      msg = new ControlCompletedImpl(MfpConstants.CONSTRUCTOR_NO_OP);
    }
    catch (MessageDecodeFailedException e) {
      /* No need to FFDC this as JsMsgObject will already have done so */
      // No FFDC code needed
      throw new MessageCreateFailedException(e);
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "createNewControlCompleted");
    return msg;
  }

  /**
   *  Create a new, empty ControlDecisionExpected Message
   *
   *  @return The new ControlDecisionExpected
   *
   *  @exception MessageCreateFailedException Thrown if such a message can not be created
   */
  public final ControlDecisionExpected createNewControlDecisionExpected() throws MessageCreateFailedException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "createNewControlDecisionExpected");
    ControlDecisionExpected msg = null;
    try {
      msg = new ControlDecisionExpectedImpl(MfpConstants.CONSTRUCTOR_NO_OP);
    }
    catch (MessageDecodeFailedException e) {
      /* No need to FFDC this as JsMsgObject will already have done so */
      // No FFDC code needed
      throw new MessageCreateFailedException(e);
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "createNewControlDecisionExpected");
    return msg;
  }

  /**
   *  Create a new, empty ControlCreateStream Message
   *
   *  @return The new ControlCreateStream
   *
   *  @exception MessageCreateFailedException Thrown if such a message can not be created
   */
  public final ControlCreateStream createNewControlCreateStream() throws MessageCreateFailedException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "createNewControlCreateStream");
    ControlCreateStream msg = null;
    try {
      msg = new ControlCreateStreamImpl(MfpConstants.CONSTRUCTOR_NO_OP);
    }
    catch (MessageDecodeFailedException e) {
      /* No need to FFDC this as JsMsgObject will already have done so */
      // No FFDC code needed
      throw new MessageCreateFailedException(e);
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "createNewControlCreateStream");
    return msg;
  }

  /**
   *  Create a new, empty ControlAreYouFlushed Message
   *
   *  @return The new ControlAreYouFlushed
   *
   *  @exception MessageCreateFailedException Thrown if such a message can not be created
   */
  public final ControlAreYouFlushed createNewControlAreYouFlushed() throws MessageCreateFailedException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "createNewControlAreYouFlushed");
    ControlAreYouFlushed msg = null;
    try {
      msg = new ControlAreYouFlushedImpl(MfpConstants.CONSTRUCTOR_NO_OP);
    }
    catch (MessageDecodeFailedException e) {
      /* No need to FFDC this as JsMsgObject will already have done so */
      // No FFDC code needed
      throw new MessageCreateFailedException(e);
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "createNewControlAreYouFlushed");
    return msg;
  }

  /**
   *  Create a new, empty ControlFlushed Message
   *
   *  @return The new ControlFlushed
   *
   *  @exception MessageCreateFailedException Thrown if such a message can not be created
   */
  public final ControlFlushed createNewControlFlushed() throws MessageCreateFailedException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "createNewControlFlushed");
    ControlFlushed msg = null;
    try {
      msg = new ControlFlushedImpl(MfpConstants.CONSTRUCTOR_NO_OP);
    }
    catch (MessageDecodeFailedException e) {
      /* No need to FFDC this as JsMsgObject will already have done so */
      // No FFDC code needed
      throw new MessageCreateFailedException(e);
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "createNewControlFlushed");
    return msg;
  }

  /**
   *  Create a new, empty ControlNotFlushed Message
   *
   *  @return The new ControlNotFlushed
   *
   *  @exception MessageCreateFailedException Thrown if such a message can not be created
   */
  public final ControlNotFlushed createNewControlNotFlushed() throws MessageCreateFailedException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "createNewControlNotFlushed");
    ControlNotFlushed msg = null;
    try {
      msg = new ControlNotFlushedImpl(MfpConstants.CONSTRUCTOR_NO_OP);
    }
    catch (MessageDecodeFailedException e) {
      /* No need to FFDC this as JsMsgObject will already have done so */
      // No FFDC code needed
      throw new MessageCreateFailedException(e);
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "createNewControlNotFlushed");
    return msg;
  }

  /**
   *  Create a new, empty ControlRequestFlush Message
   *
   *  @return The new ControlRequestFlush
   *
   *  @exception MessageCreateFailedException Thrown if such a message can not be created
   */
  public final ControlRequestFlush createNewControlRequestFlush() throws MessageCreateFailedException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "createNewControlRequestFlush");
    ControlRequestFlush msg = null;
    try {
      msg = new ControlRequestFlushImpl(MfpConstants.CONSTRUCTOR_NO_OP);
    }
    catch (MessageDecodeFailedException e) {
      /* No need to FFDC this as JsMsgObject will already have done so */
      // No FFDC code needed
      throw new MessageCreateFailedException(e);
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "createNewControlRequestFlush");
    return msg;
  }

  /**
   *  Create a new, empty ControlRequestCardinalityInfo Message
   *
   *  @return The new ControlRequestCardinalityInfo
   *
   *  @exception MessageCreateFailedException Thrown if such a message can not be created
   */
  public final ControlRequestCardinalityInfo createNewControlRequestCardinalityInfo() throws MessageCreateFailedException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "createNewControlRequestCardinalityInfo");
    ControlRequestCardinalityInfo msg = null;
    try {
      msg = new ControlRequestCardinalityInfoImpl(MfpConstants.CONSTRUCTOR_NO_OP);
    }
    catch (MessageDecodeFailedException e) {
      /* No need to FFDC this as JsMsgObject will already have done so */
      // No FFDC code needed
      throw new MessageCreateFailedException(e);
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "createNewControlRequestCardinalityInfo");
    return msg;
  }

  /**
   *  Create a new, empty ControlCardinalityInfo Message
   *
   *  @return The new ControlCardinalityInfo
   *
   *  @exception MessageCreateFailedException Thrown if such a message can not be created
   */
  public final ControlCardinalityInfo createNewControlCardinalityInfo() throws MessageCreateFailedException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "createNewControlCardinalityInfo");
    ControlCardinalityInfo msg = null;
    try {
      msg = new ControlCardinalityInfoImpl(MfpConstants.CONSTRUCTOR_NO_OP);
    }
    catch (MessageDecodeFailedException e) {
      /* No need to FFDC this as JsMsgObject will already have done so */
      // No FFDC code needed
      throw new MessageCreateFailedException(e);
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "createNewControlCardinalityInfo");
    return msg;
  }

  /**
   *  Create a new, empty ControlCreateDurable Message
   *
   *  @return The new ControlCreateDurable
   *
   *  @exception MessageCreateFailedException Thrown if such a message can not be created
   */
  public final ControlCreateDurable createNewControlCreateDurable() throws MessageCreateFailedException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "createNewControlCreateDurable");
    ControlCreateDurable msg = null;
    try {
      msg = new ControlCreateDurableImpl(MfpConstants.CONSTRUCTOR_NO_OP);
    }
    catch (MessageDecodeFailedException e) {
      /* No need to FFDC this as JsMsgObject will already have done so */
      // No FFDC code needed
      throw new MessageCreateFailedException(e);
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "createNewControlCreateDurable");
    return msg;
  }

  /**
   *  Create a new, empty ControlDeleteDurable Message
   *
   *  @return The new ControlDeleteDurable
   *
   *  @exception MessageCreateFailedException Thrown if such a message can not be created
   */
  public final ControlDeleteDurable createNewControlDeleteDurable() throws MessageCreateFailedException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "createNewControlDeleteDurable");
    ControlDeleteDurable msg = null;
    try {
      msg = new ControlDeleteDurableImpl(MfpConstants.CONSTRUCTOR_NO_OP);
    }
    catch (MessageDecodeFailedException e) {
      /* No need to FFDC this as JsMsgObject will already have done so */
      // No FFDC code needed
      throw new MessageCreateFailedException(e);
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "createNewControlDeleteDurable");
    return msg;
  }


  /**
   *  Create a new, empty ControlDurableConfirm Message
   *
   *  @return The new ControlDurableConfirm
   *
   *  @exception MessageCreateFailedException Thrown if such a message can not be created
   */
  public final ControlDurableConfirm createNewControlDurableConfirm() throws MessageCreateFailedException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "createNewControlDurableConfirm");
    ControlDurableConfirm msg = null;
    try {
      msg = new ControlDurableConfirmImpl(MfpConstants.CONSTRUCTOR_NO_OP);
    }
    catch (MessageDecodeFailedException e) {
      /* No need to FFDC this as JsMsgObject will already have done so */
      // No FFDC code needed
      throw new MessageCreateFailedException(e);
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "createNewControlDurableConfirm");
    return msg;
  }


  /* **************************************************************************/
  /* Methods for creating the appropriate inbound message                     */
  /* **************************************************************************/

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
  public final ControlMessage createInboundControlMessage(List<DataSlice> slices)
                                                  throws MessageDecodeFailedException {

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())SibTr.entry(tc, "createInboundControlMessage", slices);

    ControlMessage message = null;

    // Control Messages are small & always arrive in a single slice
    if (slices.size() == 1) {
      message = createInboundControlMessage(slices.get(0).getBytes(), slices.get(0).getOffset(), slices.get(0).getLength());
    }
    // ... so if length is anything other than 1, something has gone disastrously wrong!
    else {
      MessageDecodeFailedException mdfe = new MessageDecodeFailedException("Incorrect number of data slices: " + slices.size());
      FFDCFilter.processException(mdfe, "com.ibm.ws.sib.mfp.impl.ControlMessageFactory.createInboundControlMessage", "793");
      throw mdfe;
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "createInboundControlMessage", message);
    return message;

  }

  /**
   *  Create a ControlMessage of appropriate specialization to represent an
   *  inbound message.
   *  (To be called by the Communications component.)
   *
   *  @param rawMessage  The inbound byte array containging a complete message
   *  @param offset      The offset within the byte array at which the message begins
   *  @param length      The length of the message within the byte array
   *
   *  @return An new instance of a sub-class of ControlMessage
   *
   *  @exception MessageDecodeFailedException Thrown if the inbound message could not be decoded
   */
  public final ControlMessage createInboundControlMessage(byte rawMessage[], int offset, int length)
                                                  throws MessageDecodeFailedException {

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())SibTr.entry(tc, "createInboundControlMessage", new Object[]{ rawMessage, Integer.valueOf(offset), Integer.valueOf(length)});

    JsMsgObject jmo = new JsMsgObject(ControlAccess.schema, rawMessage, offset, length);

    /* We need to return an instance of the appropriate specialisation.       */
    ControlMessage message = makeInboundControlMessage(jmo, ((Byte)jmo.getField(ControlAccess.SUBTYPE)).intValue());

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "createInboundControlMessage", message);
    return message;

  }

  /**
   *  Create an instance of the appropriate sub-class, e.g. ControlSilence if
   *  the inbound message is actually a Control Silence Message, for the
   *  given JMO. A Control Message of unknown type will
   *  be returned as a ControlMessage.
   *
   *  @param jmo         The JsMsgObject holding the inbound message.
   *  @param messageType The int subtype of the inbound message.
   *
   *  @return ControlMessage A ControlMessage of the appropriate subtype
   */
  private final ControlMessage makeInboundControlMessage(JsMsgObject jmo, int messageType) {

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())SibTr.entry(tc, "makeInboundControlMessage", Integer.valueOf(messageType) );

    ControlMessage controlMessage = null;

    /* Create an instance of the appropriate message subclass                 */
    switch (messageType) {

      case ControlMessageType.ACKEXPECTED_INT:
        controlMessage = new ControlAckExpectedImpl(jmo);
        break;

      case ControlMessageType.SILENCE_INT:
        controlMessage = new ControlSilenceImpl(jmo);
        break;

      case ControlMessageType.ACK_INT:
        controlMessage = new ControlAckImpl(jmo);
        break;

      case ControlMessageType.NACK_INT:
        controlMessage = new ControlNackImpl(jmo);
        break;

      case ControlMessageType.PREVALUE_INT:
        controlMessage = new ControlPrevalueImpl(jmo);
        break;

      case ControlMessageType.ACCEPT_INT:
        controlMessage = new ControlAcceptImpl(jmo);
        break;

      case ControlMessageType.REJECT_INT:
        controlMessage = new ControlRejectImpl(jmo);
        break;

      case ControlMessageType.DECISION_INT:
        controlMessage = new ControlDecisionImpl(jmo);
        break;

      case ControlMessageType.REQUEST_INT:
        controlMessage = new ControlRequestImpl(jmo);
        break;

      case ControlMessageType.REQUESTACK_INT:
        controlMessage = new ControlRequestAckImpl(jmo);
        break;

      case ControlMessageType.REQUESTHIGHESTGENERATEDTICK_INT:
        controlMessage = new ControlRequestHighestGeneratedTickImpl(jmo);
        break;

      case ControlMessageType.HIGHESTGENERATEDTICK_INT:
        controlMessage = new ControlHighestGeneratedTickImpl(jmo);
        break;

      case ControlMessageType.RESETREQUESTACK_INT:
        controlMessage = new ControlResetRequestAckImpl(jmo);
        break;

      case ControlMessageType.RESETREQUESTACKACK_INT:
        controlMessage = new ControlResetRequestAckAckImpl(jmo);
        break;

      case ControlMessageType.BROWSEGET_INT:
        controlMessage = new ControlBrowseGetImpl(jmo);
        break;

      case ControlMessageType.BROWSEEND_INT:
        controlMessage = new ControlBrowseEndImpl(jmo);
        break;

      case ControlMessageType.BROWSESTATUS_INT:
        controlMessage = new ControlBrowseStatusImpl(jmo);
        break;

      case ControlMessageType.COMPLETED_INT:
        controlMessage = new ControlCompletedImpl(jmo);
        break;

      case ControlMessageType.DECISIONEXPECTED_INT:
        controlMessage = new ControlDecisionExpectedImpl(jmo);
        break;

      case ControlMessageType.CREATESTREAM_INT:
        controlMessage = new ControlCreateStreamImpl(jmo);
        break;

      case ControlMessageType.AREYOUFLUSHED_INT:
        controlMessage = new ControlAreYouFlushedImpl(jmo);
        break;

      case ControlMessageType.FLUSHED_INT:
        controlMessage = new ControlFlushedImpl(jmo);
        break;

      case ControlMessageType.NOTFLUSHED_INT:
        controlMessage = new ControlNotFlushedImpl(jmo);
        break;

      case ControlMessageType.REQUESTFLUSH_INT:
        controlMessage = new ControlRequestFlushImpl(jmo);
        break;

      case ControlMessageType.REQUESTCARDINALITYINFO_INT:
        controlMessage = new ControlRequestCardinalityInfoImpl(jmo);
        break;

      case ControlMessageType.CARDINALITYINFO_INT:
        controlMessage = new ControlCardinalityInfoImpl(jmo);
        break;

      case ControlMessageType.CREATEDURABLE_INT:
        controlMessage = new ControlCreateDurableImpl(jmo);
        break;

      case ControlMessageType.DELETEDURABLE_INT:
        controlMessage = new ControlDeleteDurableImpl(jmo);
        break;

      case ControlMessageType.DURABLECONFIRM_INT:
        controlMessage = new ControlDurableConfirmImpl(jmo);
        break;

      default:
        /* This shouldn't happen at the moment but will provide some support  */
        /* for new types being added by future releases & arriving at an old  */
        /* Messaging Engine.                                                  */
        controlMessage = new ControlMessageImpl(jmo);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())SibTr.exit(tc, "makeInboundControlMessage");
    return controlMessage;
  }


}
