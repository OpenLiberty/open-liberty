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

import java.io.PrintWriter;
import java.util.Set;

import javax.security.auth.Subject;

import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.resource.ResourceException;
import jakarta.resource.spi.ConnectionDefinition;
import jakarta.resource.spi.ConnectionManager;
import jakarta.resource.spi.ConnectionRequestInfo;
import jakarta.resource.spi.ManagedConnection;
import jakarta.resource.spi.ManagedConnectionFactory;

@ConnectionDefinition(connection = Connection.class,
                      connectionFactory = ConnectionFactory.class,
                      connectionFactoryImpl = DummyJMSConnectionFactoryImpl.class,
                      connectionImpl = DummyJMSConnectionImpl.class)
public class DummyJMSManagedConnectionFactory implements ManagedConnectionFactory {
    private static final long serialVersionUID = 1L;

    @Override
    public Object createConnectionFactory() throws ResourceException {
        return new DummyJMSConnectionFactoryImpl();
    }

    @Override
    public Object createConnectionFactory(ConnectionManager arg0) throws ResourceException {
        return new DummyJMSConnectionFactoryImpl();
    }

    @Override
    public ManagedConnection createManagedConnection(Subject arg0, ConnectionRequestInfo arg1) throws ResourceException {
        return null;
    }

    @Override
    public PrintWriter getLogWriter() throws ResourceException {
        return null;
    }

    @Override
    public ManagedConnection matchManagedConnections(Set arg0, Subject arg1, ConnectionRequestInfo arg2) throws ResourceException {
        return null;
    }

    @Override
    public void setLogWriter(PrintWriter arg0) throws ResourceException {
    }
}
