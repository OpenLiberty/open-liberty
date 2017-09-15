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
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.api.jms.ApiJmsConstants;
import com.ibm.ws.sib.api.jmsra.JmsJcaConnection;
import com.ibm.ws.sib.api.jmsra.JmsJcaConnectionFactory;
import com.ibm.ws.sib.utils.ras.SibTr;

public class JmsManagedQueueConnectionFactoryImpl extends JmsManagedConnectionFactoryImpl implements QueueConnectionFactory
{
  private static final long serialVersionUID = 4066931914700154117L;

  // ************************** TRACE INITIALISATION ***************************

  private static TraceComponent tc = SibTr.register(JmsManagedQueueConnectionFactoryImpl.class, ApiJmsConstants.MSG_GROUP_EXT, ApiJmsConstants.MSG_BUNDLE_EXT);

  // ***************************** CONSTRUCTORS ********************************

  /**
   * Constructor that stores references to the associated jca connection factory.
   */
  JmsManagedQueueConnectionFactoryImpl(JmsJcaConnectionFactory jcaConnectionFactory) {
    super(jcaConnectionFactory);
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "JmsManagedQueueConnectionFactoryImpl", jcaConnectionFactory);
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "JmsManagedQueueConnectionFactoryImpl");
  }


  // *************************** INTERFACE METHODS *****************************

  /**
   * @see javax.jms.QueueConnectionFactory#createQueueConnection()
   */
  public QueueConnection createQueueConnection() throws JMSException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "createQueueConnection");
    QueueConnection queueConnection = createQueueConnection(null, null);
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "createQueueConnection",  queueConnection);
    return queueConnection;
  }

  /**
   * @see javax.jms.QueueConnectionFactory#createQueueConnection(String, String)
   */
  public QueueConnection createQueueConnection(String userName, String password) throws JMSException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "createQueueConnection", new Object[]{userName, (password == null ? "<null>" : "<non-null>")});
    // createConnection() will call this.instantiateConnection()
    QueueConnection queueConnection = (QueueConnection) createConnection(userName, password);
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "createQueueConnection",  queueConnection);
    return queueConnection;
  }


  // ************************* IMPLEMENTATION METHODS **************************

  /**
   * This overrides a superclass method, so that the superclass's
   * createConnection() method can be inherited, but still return an object of
   * this class's type.
   */
  JmsConnectionImpl instantiateConnection(JmsJcaConnection jcaConnection, Map _passThruProps) throws JMSException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "instantiateConnection", jcaConnection);
    JmsQueueConnectionImpl jmsQueueConnection = new JmsQueueConnectionImpl(jcaConnection, isManaged(), _passThruProps);
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "instantiateConnection",  jmsQueueConnection);
    return jmsQueueConnection;
  }
}
