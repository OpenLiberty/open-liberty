/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package ra.jms.cf;

import jakarta.jms.Connection;
import jakarta.jms.ConnectionConsumer;
import jakarta.jms.ConnectionMetaData;
import jakarta.jms.Destination;
import jakarta.jms.ExceptionListener;
import jakarta.jms.JMSException;
import jakarta.jms.ServerSessionPool;
import jakarta.jms.Session;
import jakarta.jms.Topic;

public class DummyJMSConnectionImpl implements Connection {

    @Override
    public void close() throws JMSException {
    }

    @Override
    public ConnectionConsumer createConnectionConsumer(Destination arg0, String arg1, ServerSessionPool arg2, int arg3) throws JMSException {
        return null;
    }

    @Override
    public ConnectionConsumer createDurableConnectionConsumer(Topic arg0, String arg1, String arg2, ServerSessionPool arg3, int arg4) throws JMSException {
        return null;
    }

    @Override
    public Session createSession() throws JMSException {
        return null;
    }

    @Override
    public Session createSession(int arg0) throws JMSException {
        return null;
    }

    @Override
    public Session createSession(boolean arg0, int arg1) throws JMSException {
        return null;
    }

    @Override
    public ConnectionConsumer createSharedConnectionConsumer(Topic arg0, String arg1, String arg2, ServerSessionPool arg3, int arg4) throws JMSException {
        return null;
    }

    @Override
    public ConnectionConsumer createSharedDurableConnectionConsumer(Topic arg0, String arg1, String arg2, ServerSessionPool arg3, int arg4) throws JMSException {
        return null;
    }

    @Override
    public String getClientID() throws JMSException {
        return null;
    }

    @Override
    public ExceptionListener getExceptionListener() throws JMSException {
        return null;
    }

    @Override
    public ConnectionMetaData getMetaData() throws JMSException {
        return null;
    }

    @Override
    public void setClientID(String arg0) throws JMSException {
    }

    @Override
    public void setExceptionListener(ExceptionListener arg0) throws JMSException {
    }

    @Override
    public void start() throws JMSException {
    }

    @Override
    public void stop() throws JMSException {
    }

}
