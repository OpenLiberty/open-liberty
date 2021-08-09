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

import com.ibm.ws.sib.mfp.*;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.websphere.ras.TraceComponent;

/**
 *  This class extends the abstract com.ibm.ws.sib.mfp.JsJmsMessageFactory
 *  class and provides the concrete implementations of the methods for
 *  creating JsJmsMessages.
 *  <p>
 *  The class must be public so that the abstract class static
 *  initialization can create an instance at runtime.
 *
 */
public final class JsJmsMessageFactoryImpl extends JsJmsMessageFactory{

  private static TraceComponent tc = SibTr.register(JsJmsMessageFactoryImpl.class, MfpConstants.MSG_GROUP, MfpConstants.MSG_BUNDLE);

  /**
   *  Create a new, empty null-bodied JMS Message.
   *  To be called by the API component.
   *
   *  @return The new JsJmsMessage
   *
   *  @exception MessageCreateFailedException Thrown if such a message can not be created
   */
  public JsJmsMessage createJmsMessage() throws MessageCreateFailedException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "createJmsMessage");
    JsJmsMessage msg = null;
    try {
      msg = new JsJmsMessageImpl(MfpConstants.CONSTRUCTOR_NO_OP);
    }
    catch (MessageDecodeFailedException e) {
      /* No need to FFDC this as JsMsgObject will already have done so */
      // No FFDC code needed
      throw new MessageCreateFailedException(e);
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "createJmsMessage");
    return msg;
  }

  /**
   *  Create a new, empty JMS BytesMessage.
   *  To be called by the API component.
   *
   *  @return The new JsJmsBytesMessage
   *
   *  @exception MessageCreateFailedException Thrown if such a message can not be created
   */
  public JsJmsBytesMessage createJmsBytesMessage() throws MessageCreateFailedException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "createJmsBytesMessage");
    JsJmsBytesMessage msg = null;
    try {
      msg = new JsJmsBytesMessageImpl(MfpConstants.CONSTRUCTOR_NO_OP);
    }
    catch (MessageDecodeFailedException e) {
      /* No need to FFDC this as JsMsgObject will already have done so */
      // No FFDC code needed
      throw new MessageCreateFailedException(e);
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "createJmsBytesMessage");
    return msg;
  }

  /**
   *  Create a new, empty JMS MapMessage.
   *  To be called by the API component.
   *
   *  @return The new JsJmsMapMessage
   *
   *  @exception MessageCreateFailedException Thrown if such a message can not be created
   */
  public JsJmsMapMessage createJmsMapMessage() throws MessageCreateFailedException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "createJmsMapMessage");
    JsJmsMapMessage msg = null;
    try {
      msg = new JsJmsMapMessageImpl(MfpConstants.CONSTRUCTOR_NO_OP);
    }
    catch (MessageDecodeFailedException e) {
      /* No need to FFDC this as JsMsgObject will already have done so */
      // No FFDC code needed
      throw new MessageCreateFailedException(e);
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "createJmsMapMessage");
    return msg;
  }

  /**
   *  Create a new, empty JMS ObjectMessage.
   *  To be called by the API component.
   *
   *  @return The new JsJmsObjectMessage
   *
   *  @exception MessageCreateFailedException Thrown if such a message can not be created
   */
  public JsJmsObjectMessage createJmsObjectMessage() throws MessageCreateFailedException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "createJmsObjectMessage");
    JsJmsObjectMessage msg = null;
    try {
      msg = new JsJmsObjectMessageImpl(MfpConstants.CONSTRUCTOR_NO_OP);
    }
    catch (MessageDecodeFailedException e) {
      /* No need to FFDC this as JsMsgObject will already have done so */
      // No FFDC code needed
      throw new MessageCreateFailedException(e);
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "createJmsObjectMessage");
    return msg;
  }

  /**
   *  Create a new, empty JMS StreamMessage.
   *  To be called by the API component.
   *
   *  @return The new JsJmsStreamMessage
   *
   *  @exception MessageCreateFailedException Thrown if such a message can not be created
   */
  public JsJmsStreamMessage createJmsStreamMessage() throws MessageCreateFailedException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "createJmsStreamMessage");
    JsJmsStreamMessage msg = null;
    try {
      msg = new JsJmsStreamMessageImpl(MfpConstants.CONSTRUCTOR_NO_OP);
    }
    catch (MessageDecodeFailedException e) {
      /* No need to FFDC this as JsMsgObject will already have done so */
      // No FFDC code needed
      throw new MessageCreateFailedException(e);
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "createJmsStreamMessage");
    return msg;
  }

  /**
   *  Create a new, empty JMS TextMessage.
   *  To be called by the API component.
   *
   *  @return The new JsJmsTextMessage
   *
   *  @exception MessageCreateFailedException Thrown if such a message can not be created
   */
  public JsJmsTextMessage createJmsTextMessage() throws MessageCreateFailedException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "createJmsTextMessage");
    JsJmsTextMessage msg = null;
    try {
      msg = new JsJmsTextMessageImpl(MfpConstants.CONSTRUCTOR_NO_OP);
    }
    catch (MessageDecodeFailedException e) {
      /* No need to FFDC this as JsMsgObject will already have done so */
      // No FFDC code needed
      throw new MessageCreateFailedException(e);
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "createJmsTextMessage");
    return msg;
  }


  /* **************************************************************************/
  /* Package visibility method for making the appropriate inbound message     */
  /* **************************************************************************/

  /**
   *  Create an instance of the appropriate sub-class, e.g. JsJmsTextMessage
   *  if the inbound message is actually a JMS Text Message, for the
   *  given JMO.
   *
   *  @return JsJmsMessage A JsJmsMessage of the appropriate subtype
   */
  final JsJmsMessage createInboundJmsMessage(JsMsgObject jmo, int messageType) {

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())SibTr.entry(tc, "createInboundJmsMessage " + messageType );

    JsJmsMessage jmsMessage = null;

    switch (messageType) {

      case JmsBodyType.BYTES_INT:
        jmsMessage = new JsJmsBytesMessageImpl(jmo);
        break;

      case JmsBodyType.MAP_INT:
        jmsMessage = new JsJmsMapMessageImpl(jmo);
        break;

      case JmsBodyType.OBJECT_INT:
        jmsMessage = new JsJmsObjectMessageImpl(jmo);
        break;

      case JmsBodyType.STREAM_INT:
        jmsMessage = new JsJmsStreamMessageImpl(jmo);
        break;

      case JmsBodyType.TEXT_INT:
        jmsMessage = new JsJmsTextMessageImpl(jmo);
        break;

      default:
        jmsMessage = new JsJmsMessageImpl(jmo);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())SibTr.exit(tc, "createInboundJmsMessage");
    return jmsMessage;
  }


}
