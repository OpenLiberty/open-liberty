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

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.api.jms.ApiJmsConstants;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.ConsumerSession;
import com.ibm.wsspi.sib.core.SICoreConnection;
import com.ibm.wsspi.sib.core.SICoreConnectionListener;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;

/**
 * Mapping class between the core connection listener and JMS ExceptionListener
 *
 * JmsConnectionImpl will create an instance of this class the first time setExceptionListener
 * is called, passing itself as the sole parameter to the constructor.
 *
 * When one of the methods in this class is invoked by the core, we create a JMSException
 * with an appropriate nls message, and call connection.reportException.
 * This leaves the existing code in JmsConnectionImpl to handle sequentialisation of calls,
 * and updates to the registered ExceptionListener.
 *
 * @author kingdon
 */
class ConnectionListenerImpl implements SICoreConnectionListener
{
  // ************************** TRACE INITIALISATION ***************************

  private static TraceComponent tc = SibTr.register(ConnectionListenerImpl.class, ApiJmsConstants.MSG_GROUP_INT, ApiJmsConstants.MSG_BUNDLE_INT);

  /**
   * deliver exceptions via the JmsConnectionImpl.reportException method
   */
  private JmsConnectionImpl connection;

  ConnectionListenerImpl(final JmsConnectionImpl conn)
  {
    connection = conn;
  }

  /**
   *
   * @see com.ibm.wsspi.sib.core.SICoreConnectionListener#asynchronousException(com.ibm.wsspi.sib.core.ConsumerSession, Throwable)
   */
  public void asynchronousException(ConsumerSession consumer, Throwable exception) {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "asynchronousException", new Object[]{consumer, exception});

    // Wrap in a generic exception before delivering.
    JMSException jmse = (JMSException)JmsErrorUtils.newThrowable(JMSException.class,
                                                                 "ASYNC_EXCEPTION_CWSIA0341",
                                                                 new Object[] { exception },
                                                                 exception,
                                                                 "ConnectionListenerImpl.asynchronousException#1",
                                                                 this,
                                                                 tc);
    connection.reportException(jmse);
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "asynchronousException");
  }

  /**
   * Called when ME begins shutdown.<p>
   *
   * The conn parameter is ignored. The JMS API uses a 1 to 1 mapping of ConnectionListenerImpl
   * to JMS Connection instances, so we should only ever receive calls for our own connection.
   *
   * @see com.ibm.wsspi.sib.core.SICoreConnectionListener#meQuiescing(com.ibm.wsspi.sib.core.SICoreConnection)
   */
  public void meQuiescing(SICoreConnection conn) {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "meQuiescing", conn);

    JMSException jmse = (JMSException)JmsErrorUtils.newThrowable(JMSException.class,
                                                                 "ME_QUIESCE_CWSIA0342",
                                                                 null,
                                                                 tc);
    connection.reportException(jmse);
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "meQuiescing");
  }

  /**
   * This method is called when a connection is closed due to a communications
   * failure. Examine the SICommsException to determine the nature of the failure.
   *
   * <p>The conn parameter is ignored. The JMS API uses a 1 to 1 mapping of ConnectionListenerImpl
   *  to JMS Connection instances, so we should only ever receive calls for our own connection.
   *
   * @see com.ibm.wsspi.sib.core.SICoreConnectionListener#commsFailure(com.ibm.wsspi.sib.core.SICoreConnection, com.ibm.wsspi.sib.core.SICommsException)
   */
  public void commsFailure(SICoreConnection conn, SIConnectionLostException exception) {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "commsFailure", new Object[]{conn, exception});

    JMSException jmse = (JMSException)JmsErrorUtils.newThrowable(JMSException.class,
                                                                 "COMMS_FAILURE_CWSIA0343",
                                                                 new Object[] { exception },
                                                                 exception,
                                                                 "ConnectionListenerImpl.commsFailure#1",
                                                                 this,
                                                                 tc);
    connection.reportException(jmse);
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "commsFailure");
  }

  /**
   * This method is called when a connection is closed because the Messaging
   * Engine has terminated.
   *
   * <p>The conn parameter is ignored. The JMS API uses a 1 to 1 mapping of ConnectionListenerImpl
   * to JMS Connection instances, so we should only ever receive calls for our own connection.
   *
   * @see com.ibm.wsspi.sib.core.SICoreConnectionListener#meTerminated(com.ibm.wsspi.sib.core.SICoreConnection)
   */
  public void meTerminated(SICoreConnection conn) {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "meTerminated", conn);

    JMSException jmse = (JMSException)JmsErrorUtils.newThrowable(JMSException.class,
                                                                 "ME_TERMINATED_CWSIA0344",
                                                                 null,
                                                                 tc);
    connection.reportException(jmse);
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "meTerminated");
  }
}
