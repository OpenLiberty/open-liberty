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

import java.util.Map;

import javax.jms.ConnectionConsumer;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueSession;
import javax.jms.ServerSessionPool;
import javax.jms.Topic;
import javax.jms.TopicSession;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.api.jms.ApiJmsConstants;
import com.ibm.ws.sib.api.jmsra.JmsJcaConnection;
import com.ibm.ws.sib.api.jmsra.JmsJcaSession;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.SICoreConnection;

public class JmsQueueConnectionImpl extends JmsConnectionImpl implements QueueConnection
{
  // ************************** TRACE INITIALISATION ***************************

  private static TraceComponent tc = SibTr.register(JmsQueueConnectionImpl.class, ApiJmsConstants.MSG_GROUP_EXT, ApiJmsConstants.MSG_BUNDLE_EXT);

  // ***************************** CONSTRUCTORS ********************************

  JmsQueueConnectionImpl (JmsJcaConnection jcaConnection, boolean isManaged, Map _passThruProps) throws JMSException {
    super(jcaConnection, isManaged, _passThruProps);
    // NB. The parameters for this constructor are traced in the parent class.
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "JmsQueueConnectionImpl");
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "JmsQueueConnectionImpl");
  }


  // *************************** INTERFACE METHODS *****************************

  /**
   * @see javax.jms.Connection#createDurableConnectionConsumer(Topic, String,
   * String, ServerSessionPool, int)
   */
  public ConnectionConsumer createDurableConnectionConsumer(Topic topic, String subscriptionName, String messageSelector, ServerSessionPool sessionPool, int maxMessages) throws JMSException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "createDurableConnectionConsumer", new Object[]{topic, subscriptionName, messageSelector, sessionPool, maxMessages});
    throw (javax.jms.IllegalStateException)JmsErrorUtils.newThrowable(javax.jms.IllegalStateException.class,
                                                                      "INVALID_OP_FOR_CLASS_CWSIA0481",
                                                                      new Object[] {"createDurableConnectionConsumer", "QueueConnection"},
                                                                      tc
                                                                     );
  }

  /**
   * @see javax.jms.QueueConnection#createConnectionConsumer(Queue, String,
   * ServerSessionPool, int)
   */
  public ConnectionConsumer createConnectionConsumer(Queue queue, String messageSelector, ServerSessionPool sessionPool, int maxMessages) throws JMSException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "createConnectionConsumer", new Object[]{queue, messageSelector, sessionPool, maxMessages});
    // This is an ASF function that we do not support in Jetstream
    throw (JMSException) JmsErrorUtils.newThrowable(JMSException.class,
                                                    "UNSUPPORTED_FUNC_CWSIA0482",
                                                    new Object[] {"JmsQueueConnectionImpl.createConnectionConsumer()"},
                                                    tc
                                                   );
  }

  /**
   * @see javax.jms.QueueConnection#createQueueSession(boolean, int)
   */
  public QueueSession createQueueSession(boolean transacted, int acknowledgeMode) throws JMSException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "createQueueSession", new Object[]{transacted, acknowledgeMode});
    // createSession will actually create a queue session.
    JmsQueueSessionImpl  queueSession = (JmsQueueSessionImpl)createSession(transacted, acknowledgeMode);
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "createQueueSession",  queueSession);
    return queueSession;
  }

  /**
   * @see javax.jms.TopicConnection#createTopicSession(boolean, int)
   */
  public TopicSession createTopicSession(boolean transacted, int acknowledgeMode) throws JMSException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "createTopicSession", new Object[]{transacted, acknowledgeMode});
    // createSession will actually create a topic session.
    JmsTopicSessionImpl newTopicSession = (JmsTopicSessionImpl) createSession(transacted, acknowledgeMode);
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "createTopicSession",  newTopicSession);
    return newTopicSession;
  }

  // ************************* IMPLEMENTATION METHODS **************************

  /**
   * This method overrides a superclass method, so that the superclass's
   * createSession() method can be inherited, but still return an object of
   * this class's type.
   */
  JmsSessionImpl instantiateSession(boolean transacted, int acknowledgeMode, SICoreConnection coreConnection, JmsJcaSession jcaSession) throws JMSException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "instantiateSession", new Object[]{transacted, acknowledgeMode, coreConnection, jcaSession});
    JmsQueueSessionImpl jmsQueueSession = new JmsQueueSessionImpl(transacted, acknowledgeMode, coreConnection, this, jcaSession);
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "instantiateSession",  jmsQueueSession);
    return jmsQueueSession;
  }
}
