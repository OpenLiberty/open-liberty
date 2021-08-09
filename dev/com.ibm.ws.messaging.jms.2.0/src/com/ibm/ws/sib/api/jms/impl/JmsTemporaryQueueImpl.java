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

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectOutputStream;

import javax.jms.JMSException;
import javax.jms.TemporaryQueue;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.SIDestinationAddress;
import com.ibm.websphere.sib.api.jms.ApiJmsConstants;
import com.ibm.ws.sib.api.jms.JmsTemporaryDestinationInternal;
import com.ibm.ws.sib.utils.ras.SibTr;


public class JmsTemporaryQueueImpl extends JmsQueueImpl implements TemporaryQueue, JmsTemporaryDestinationInternal
{

  private static final long serialVersionUID = 8936845633815526385L;
  // ************************** TRACE INITIALISATION ***************************

  private static TraceComponent tc = SibTr.register(JmsTemporaryQueueImpl.class, ApiJmsConstants.MSG_GROUP_EXT, ApiJmsConstants.MSG_BUNDLE_EXT);


   // **************************** STATE VARIABLES ******************************
  JmsSessionImpl session;
  boolean deleted = false;

  /**
   * This constructor is called from JmsSessionImpl.createTemporaryQueue()
   * in order to create a JmsTemporaryQueueImpl.
   * @throws JMSException - a JMSException is thrown if its not been
   *              possible to create a TemporaryQueue for any reason.
   */
  public JmsTemporaryQueueImpl(SIDestinationAddress destAddr, JmsSessionImpl sess) throws JMSException {
    super();
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "<init>", new Object[]{destAddr, sess});

    setDestName(destAddr.getDestinationName());
    setBusName(destAddr.getBusName());
    session = sess;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "<init>");
  }

  /* (non-Javadoc)
   * @see javax.jms.TemporaryQueue#delete()
   */
  public void delete() throws JMSException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "delete");

    //check to see if this has already been deleted.
    //If it has return with out doing anything
    if(!deleted) {
      session.deleteTemporaryDestination(getConsumerSIDestinationAddress());
      deleted = true;
      ((JmsConnectionImpl)session.getConnection()).removeTemporaryDestination(this);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "delete");
  }


  /**
   * The writeObject method for serialization.
   *
   * JmsDestination is declared as Serializable, which makes this class
   * Serializable. However, a TemporaryQueue/Topic should never be copied and
   * can't be used outside the scope of its session. Hence it makes no sense
   * to even attempt to serialize so we just throw java.io.Serial
   *
   * @param out The ObjectOutputStream for serialization.
   * @exception IOException A java.io.NotSerializablException is always thrown.
   */
  private void writeObject(ObjectOutputStream out) throws IOException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "writeObject", out);
    throw new NotSerializableException("TemporaryQueue");
  }

  /**
   * @see java.lang.Object#equals(Object)
   *
   * This is a very naff implementation of equals, but is what has been been the de
   * facto action for WAS 6. It is not therefore safe to change it to use any of the
   * specific fields in this class.
   */
  public boolean equals(Object other) {
    return super.equals(other);
  }

  /**
   * @see java.lang.Object#hashCode()
   *
   * This is a very naff implementation of hashCode, but is what has been been the de
   * facto action for WAS 6. It is not therefore safe to change it to use any of the
   * specific fields in this class.
   */
  public int hashCode() {
    return super.hashCode();
  }
}
