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
import javax.jms.Topic;
import javax.jms.TopicSubscriber;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.api.jms.ApiJmsConstants;
import com.ibm.wsspi.sib.core.SICoreConnection;
import com.ibm.ws.sib.utils.ras.SibTr;


public class JmsTopicSubscriberImpl extends JmsMsgConsumerImpl implements TopicSubscriber
{

  // ************************** TRACE INITIALISATION ***************************

  private static TraceComponent tc = SibTr.register(JmsTopicSubscriberImpl.class, ApiJmsConstants.MSG_GROUP_EXT, ApiJmsConstants.MSG_BUNDLE_EXT);

  // **************************** CONSTRUCTOR ********************************

  JmsTopicSubscriberImpl(SICoreConnection coreConnection, JmsSessionImpl newSession, ConsumerProperties newProps) throws JMSException {

    super(coreConnection, newSession, newProps);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "JmsTopicSubscriberImpl", new Object[]{coreConnection, newSession, newProps});
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "dest : " + newProps.getJmsDestination() + " selector : " + newProps.getSelector() + " noLocal : " + newProps.noLocal());
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "JmsTopicSubscriberImpl");
  }


  // *************************** INTERFACE METHODS *****************************

  /**
   * @see javax.jms.TopicSubscriber#getTopic()
   */
  public Topic getTopic() throws JMSException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getTopic");

    checkClosed();
    Topic topic = (Topic) getDestination();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getTopic",  (topic));
    return (topic);
  }

  /**
   * @see javax.jms.TopicSubscriber#getNoLocal()
   */
  public boolean getNoLocal() throws JMSException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getNoLocal");

    // super's getNoLocalFlag() calls checkClosed()
    boolean noLocal = super.getNoLocalFlag();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getNoLocal",  noLocal);
    return noLocal;
  }
}
