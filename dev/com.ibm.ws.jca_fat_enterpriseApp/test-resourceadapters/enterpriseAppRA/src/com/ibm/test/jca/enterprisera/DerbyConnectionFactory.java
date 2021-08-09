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
package com.ibm.test.jca.enterprisera;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

import javax.resource.ResourceException;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ConnectionRequestInfo;
import javax.sql.DataSource;

public class DerbyConnectionFactory implements DataSource {

    private final ConnectionManager cm;
    private final DerbyManagedConnectionFactory mcf;

    public DerbyConnectionFactory(ConnectionManager cm, DerbyManagedConnectionFactory mcf) {
        this.cm = cm;
        this.mcf = mcf;
    }

    @Override
    public Connection getConnection() throws SQLException {
        return getConnection(mcf.getUserName(), mcf.getPassword());
    }

    @Override
    public Connection getConnection(String user, String password) throws SQLException {
        try {
            ConnectionRequestInfo cri = new DerbyConnectionRequestInfo(user, password);
            return ((DerbyConnection) cm.allocateConnection(mcf, cri)).init(cm);
        } catch (ResourceException x) {
            throw (SQLException) new SQLException(x.getMessage()).initCause(x);
        }
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return mcf.adapter.xaDataSource.getLoginTimeout();
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return mcf.adapter.xaDataSource.getLogWriter();
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return false;
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        throw new UnsupportedOperationException();
    }

    //@Override // Java 7
    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new UnsupportedOperationException();
    }
}