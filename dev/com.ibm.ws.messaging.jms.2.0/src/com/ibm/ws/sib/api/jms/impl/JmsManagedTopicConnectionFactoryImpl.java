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

import javax.jms.JMSException;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.api.jms.ApiJmsConstants;
import com.ibm.ws.sib.api.jmsra.JmsJcaConnection;
import com.ibm.ws.sib.api.jmsra.JmsJcaConnectionFactory;
import com.ibm.ws.sib.utils.ras.SibTr;


public class JmsManagedTopicConnectionFactoryImpl extends JmsManagedConnectionFactoryImpl implements TopicConnectionFactory
{

  private static final long serialVersionUID = 1325516850522533785L;

  // ************************** TRACE INITIALISATION ***************************

  private static TraceComponent tc = SibTr.register(JmsManagedTopicConnectionFactoryImpl.class, ApiJmsConstants.MSG_GROUP_EXT, ApiJmsConstants.MSG_BUNDLE_EXT);


  // ***************************** CONSTRUCTORS ********************************

  /**
   * Constructor that stores references to the associated jca connection
   * factory.
   */
  JmsManagedTopicConnectionFactoryImpl(JmsJcaConnectionFactory jcaConnectionFactory) {
    super(jcaConnectionFactory);
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "JmsManagedTopicConnectionFactoryImpl", jcaConnectionFactory);
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "JmsManagedTopicConnectionFactoryImpl");
  }


  // *************************** INTERFACE METHODS *****************************

  /**
   * @see javax.jms.TopicConnectionFactory#createTopicConnection()
   */
  public TopicConnection createTopicConnection() throws JMSException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "createTopicConnection");
    TopicConnection topicConnection = createTopicConnection(null, null);
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "createTopicConnection",  topicConnection);
    return topicConnection;
  }

  /**
   * @see javax.jms.TopicConnectionFactory#createTopicConnection(String, String)
   */
  public TopicConnection createTopicConnection(String userName, String password)  throws JMSException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "createTopicConnection", new Object[]{userName, (password == null ? "<null>" : "<non-null>")});
    // createConnection() will call this.instantiateConnection()
    TopicConnection topicConnection = (TopicConnection)createConnection(userName, password);
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "createTopicConnection",  topicConnection);
    return topicConnection;
  }


  // ************************* IMPLEMENTATION METHODS **************************

  /**
   * This overrides a superclass method, so that the superclass's
   * createConnection() method can be inherited, but still return an object of
   * this class's type.
   */
  JmsConnectionImpl instantiateConnection(JmsJcaConnection jcaConnection, Map _passThruProps) throws JMSException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "instantiateConnection", jcaConnection);
    JmsTopicConnectionImpl jmsTopicConnection = new JmsTopicConnectionImpl(jcaConnection, isManaged(), _passThruProps);
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "instantiateConnection",  jmsTopicConnection);
    return jmsTopicConnection;
  }
}
