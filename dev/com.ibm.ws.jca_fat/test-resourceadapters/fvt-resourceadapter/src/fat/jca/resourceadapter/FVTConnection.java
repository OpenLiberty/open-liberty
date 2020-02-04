/*******************************************************************************
 * Copyright (c) 2012,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package fat.jca.resourceadapter;

import javax.jms.Connection;
import javax.jms.ConnectionConsumer;
import javax.jms.ConnectionMetaData;
import javax.jms.Destination;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.ServerSessionPool;
import javax.jms.Session;
import javax.jms.Topic;
import javax.resource.spi.ConnectionEvent;
import javax.resource.spi.ConnectionManager;

public class FVTConnection implements Connection {
    ConnectionManager cm;
    FVTManagedConnection mc;

    FVTConnection(FVTManagedConnection mc) {
        this.mc = mc;
    }

    FVTConnection init(ConnectionManager cm) {
        this.cm = cm;
        return this;
    }

    @Override
    public void close() {
        if (mc != null)
            mc.notify(ConnectionEvent.CONNECTION_CLOSED, this, null);
        mc = null;
    }

    @Override
    public ConnectionConsumer createConnectionConsumer(Destination arg0, String arg1, ServerSessionPool arg2, int arg3) throws JMSException {
        throw new UnsupportedOperationException();
    }

    @Override
    public ConnectionConsumer createDurableConnectionConsumer(Topic arg0, String arg1, String arg2, ServerSessionPool arg3, int arg4) throws JMSException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Session createSession(boolean transacted, int acknowledgeMode) throws JMSException {
        return new FVTSession(this);
    }

    @Override
    public String getClientID() throws JMSException {
        return mc.mcf.getClientID();
    }

    @Override
    public ExceptionListener getExceptionListener() throws JMSException {
        throw new UnsupportedOperationException();
    }

    @Override
    public ConnectionMetaData getMetaData() throws JMSException {
        return new FVTConnectionMetaData(mc);
    }

    @Override
    public void setClientID(String clientID) throws JMSException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setExceptionListener(ExceptionListener arg0) throws JMSException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void start() throws JMSException {
    }

    @Override
    public void stop() throws JMSException {
    }
}
