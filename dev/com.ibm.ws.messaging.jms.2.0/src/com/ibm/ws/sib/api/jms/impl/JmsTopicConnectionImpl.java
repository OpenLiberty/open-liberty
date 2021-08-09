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
import javax.jms.ServerSessionPool;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicSession;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.api.jms.ApiJmsConstants;
import com.ibm.ws.sib.api.jmsra.JmsJcaConnection;
import com.ibm.ws.sib.api.jmsra.JmsJcaSession;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.SICoreConnection;

public class JmsTopicConnectionImpl extends JmsConnectionImpl implements TopicConnection
{
  // ************************** TRACE INITIALISATION ***************************

  private static TraceComponent tc = SibTr.register(JmsTopicConnectionImpl.class, ApiJmsConstants.MSG_GROUP_EXT, ApiJmsConstants.MSG_BUNDLE_EXT);

  // ***************************** CONSTRUCTORS ********************************

  JmsTopicConnectionImpl(JmsJcaConnection jcaConnection, boolean isManaged, Map _passThruProps) throws JMSException {
    super(jcaConnection, isManaged, _passThruProps);
    // NB. The parameters for this constructor are traced in the parent.
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "JmsTopicConnectionImpl");
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "JmsTopicConnectionImpl");
  }

  // *************************** INTERFACE METHODS *****************************

  /**
   * @see javax.jms.TopicConnection#createConnectionConsumer(Topic, String,
   * ServerSessionPool, int)
   */
  public ConnectionConsumer createConnectionConsumer(Topic topic, String messageSelector, ServerSessionPool sessionPool, int maxMessages) throws JMSException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "createConnectionConsumer");
    // This is an ASF function, not implemented in Jetstream
    throw (JMSException)JmsErrorUtils.newThrowable(JMSException.class,
                                                   "UNSUPPORTED_FUNC_CWSIA0484",
                                                   new Object[] {"JmsTopicConnectionImpl.createConnectionConsumer()"},
                                                   tc
                                                  );
  }

  /**
   * @see javax.jms.TopicConnection#createTopicSession(boolean, int)
   */
  public TopicSession createTopicSession(boolean transacted, int acknowledgeMode) throws JMSException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "createTopicSession", new Object[]{transacted, acknowledgeMode});
    // createSession will actually create a topic session.
    JmsTopicSessionImpl topicSession = (JmsTopicSessionImpl)createSession(transacted, acknowledgeMode);
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "createTopicSession",  topicSession);
    return topicSession;
  }

  // ************************* IMPLEMENTATION METHODS **************************

  /**
   * This method overrides a superclass method, so that the superclass's
   * createSession() method can be inherited, but still return an object of
   * this class's type.
   */
  JmsSessionImpl instantiateSession(boolean transacted, int acknowledgeMode, SICoreConnection coreConnection, JmsJcaSession jcaSession) throws JMSException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "instantiateSession", new Object[]{transacted, acknowledgeMode, coreConnection, jcaSession});
    JmsTopicSessionImpl jmsTopicSession = new JmsTopicSessionImpl(transacted, acknowledgeMode, coreConnection, this, jcaSession);
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "instantiateSession",  jmsTopicSession);
    return jmsTopicSession;
  }
}
