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

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.api.jms.ApiJmsConstants;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * Provides an implementation of the MessageListenerSetter interface.
 * Used to enable async delivery in managed containers, for use by system software.
 *
 * Must have a default constructor for use by com.ibm.wsspi.jms.JmsMessageListenerSupport
 * (implicit default constructor ok)
 */
public class MessageListenerSetter implements com.ibm.wsspi.jms.JmsMessageListenerSupport.MessageListenerSetter
{
  private static TraceComponent tc = SibTr.register(MessageListenerSetter.class, ApiJmsConstants.MSG_GROUP_INT, ApiJmsConstants.MSG_BUNDLE_INT);

  /**
   * Assign a messageListener to the message consumer.
   * @see com.ibm.wsspi.jms.JmsMessageListenerSupport.MessageListenerSetter#setMessageListener(javax.jms.MessageConsumer, javax.jms.MessageListener)
   */
  public void setMessageListener(MessageConsumer consumer, MessageListener listener) throws JMSException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "setMessageListener", new Object[]{consumer, listener});

    if (!(consumer instanceof JmsMsgConsumerImpl)) {
      // This "shouldn't happen" (tm) because the calling code switches on the class of the consumer.
      throw (JMSException) JmsErrorUtils.newThrowable(
          JMSException.class,
          "WRONG_CONSUMER_TYPE_CWSIA0088",
          new Object[] { consumer.getClass().getName() },
          null,
          "MessageListenerSetter.setMessageListener#1",
          this,
          tc);
    }

    JmsMsgConsumerImpl c = (JmsMsgConsumerImpl) consumer;

    boolean checkManaged = false;
    c._setMessageListener(listener, checkManaged);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "setMessageListener");
  }
}
