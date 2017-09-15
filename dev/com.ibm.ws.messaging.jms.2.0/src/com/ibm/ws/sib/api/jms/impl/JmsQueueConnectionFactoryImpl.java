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

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.api.jms.ApiJmsConstants;
import com.ibm.websphere.sib.api.jms.JmsQueueConnectionFactory;
import com.ibm.ws.sib.api.jmsra.JmsJcaConnection;
import com.ibm.ws.sib.api.jmsra.JmsJcaConnectionFactory;
import com.ibm.ws.sib.api.jmsra.JmsJcaManagedConnectionFactory;
import com.ibm.ws.sib.utils.ras.SibTr;

public class JmsQueueConnectionFactoryImpl extends JmsConnectionFactoryImpl implements JmsQueueConnectionFactory
{
  private static final long serialVersionUID = 1287034675518818522L;

  // ************************** TRACE INITIALISATION ***************************

  private static TraceComponent tc =  SibTr.register(JmsQueueConnectionFactoryImpl.class, ApiJmsConstants.MSG_GROUP_EXT, ApiJmsConstants.MSG_BUNDLE_EXT);


  // ***************************** CONSTRUCTORS ********************************

  /**
   * Constructor that stores references to the associated jca connection
   * factory and the associated jca managed connection factory by delegating
   * to the superclass constructor.
   */
  JmsQueueConnectionFactoryImpl(JmsJcaConnectionFactory jcaConnectionFactory, JmsJcaManagedConnectionFactory jcaManagedConnectionFactory) {
    super(jcaConnectionFactory, jcaManagedConnectionFactory);
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "JmsQueueConnectionFactoryImpl", new Object[]{jcaConnectionFactory, jcaManagedConnectionFactory});
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "JmsQueueConnectionFactoryImpl");
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
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "instantiateConnection", new Object[]{jcaConnection});
    JmsQueueConnectionImpl jmsQueueConnection = new JmsQueueConnectionImpl(jcaConnection, isManaged(), _passThruProps);
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "instantiateConnection",  jmsQueueConnection);
    return jmsQueueConnection;
  }
}
