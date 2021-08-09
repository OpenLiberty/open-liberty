/*******************************************************************************
 * Copyright (c) 2013, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.test.dynamicconfigadapter;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Map;
import java.util.logging.Logger;

import javax.resource.ResourceException;
import javax.resource.cci.MessageListener;
import javax.resource.cci.Record;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.endpoint.MessageEndpoint;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.sql.DataSource;

public class DynaCfgDataSource implements DataSource {
    private final ConnectionManager cm;
    private final DynaCfgManagedConnectionFactory mcf;

    DynaCfgDataSource(ConnectionManager cm, DynaCfgManagedConnectionFactory mcf) {
        this.cm = cm;
        this.mcf = mcf;
    }

    @Override
    public Connection getConnection() throws SQLException {
        return getConnection(null, null);
    }

    @Override
    public Connection getConnection(String user, String pwd) throws SQLException {
        try {
            ConnectionRequestInfo cri = new DynaCfgConnectionRequestInfo(user, pwd);
            return (Connection) cm.allocateConnection(mcf, cri);
        } catch (ResourceException x) {
            throw (SQLException) new SQLException(x.getMessage()).initCause(x);
        }
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return mcf.getLoginTimeout();
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return null;
    }

    //@Override // Java 7
    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return iface.isInstance(this);
    }

    @Override
    public void setLoginTimeout(final int seconds) throws SQLException {
        // abuse this method as a way for tests to trigger messages to be sent to MDBs

        Record record = new Record() {
            private static final long serialVersionUID = -43270765859960511L;

            @Override
            public Object clone() throws CloneNotSupportedException {
                throw new CloneNotSupportedException();
            }

            @Override
            public String getRecordName() {
                return "message: ignore setLoginTimeout " + seconds;
            }

            @Override
            public String getRecordShortDescription() {
                return getRecordName();
            }

            @Override
            public void setRecordName(String name) {
            }

            @Override
            public void setRecordShortDescription(String description) {
            }
        };

        for (Map.Entry<ActivationSpec, MessageEndpointFactory> entry : ((DynaCfgResourceAdapter) mcf.getResourceAdapter()).endpointFactories.entrySet()) {
            DynaCfgActivationSpec activationSpec = (DynaCfgActivationSpec) entry.getKey();
            if (seconds >= activationSpec.getMessageFilterMin() && seconds <= activationSpec.getMessageFilterMax()) {
                // send message to MDB
                MessageEndpointFactory endpointFactory = entry.getValue();
                MessageEndpoint endpoint = null;
                try {
                    endpoint = endpointFactory.createEndpoint(null);
                    ((MessageListener) endpoint).onMessage(record);
                } catch (Exception x) {
                    throw new SQLException("error on " + activationSpec + ", " + endpointFactory, x);
                } finally {
                    if (endpoint != null)
                        endpoint.release();
                }
            }
        }
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (isWrapperFor(iface))
            return (T) this;
        else
            throw new SQLFeatureNotSupportedException();
    }
}
