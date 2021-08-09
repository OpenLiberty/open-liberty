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

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.QueueSender;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.api.jms.ApiJmsConstants;
import com.ibm.wsspi.sib.core.SICoreConnection;
import com.ibm.ws.sib.utils.ras.SibTr;

public class JmsQueueSenderImpl extends JmsMsgProducerImpl implements QueueSender
{
  // ************************** TRACE INITIALISATION ***************************

  private static TraceComponent tc =  SibTr.register(JmsQueueSenderImpl.class, ApiJmsConstants.MSG_GROUP_EXT, ApiJmsConstants.MSG_BUNDLE_EXT);

  // ***************************** CONSTRUCTORS ********************************

  JmsQueueSenderImpl (Destination theDest, SICoreConnection coreConnection, JmsSessionImpl newSession) throws JMSException {
    super(theDest, coreConnection, newSession);
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "JmsQueueSenderImpl", new Object[]{theDest, coreConnection, newSession});
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "JmsQueueSenderImpl");
  }

  // *************************** INTERFACE METHODS *****************************

  /**
   * @see javax.jms.QueueSender#getQueue()
   */
  public Queue getQueue() throws JMSException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getQueue");
    checkClosed();
    Queue queue = (Queue)getDestination();
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getQueue",  (queue));
    return (queue);
  }

  /**
   * @see javax.jms.MessageProducer#send(Destination, Message)
   */
  public void send(Queue queue, Message message) throws JMSException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "send", new Object[]{queue, message});
    super.send(queue, message);
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "send");
  }

  /**
   * @see javax.jms.MessageProducer#send(Destination, Message, int, int, long)
   */
  public void send(Queue queue, Message message, int deliveryMode, int priority, long timeToLive) throws JMSException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "send", new Object[]{queue, message, deliveryMode, priority, timeToLive});
    super.send(queue, message, deliveryMode, priority, timeToLive);
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "send");
  }
}
