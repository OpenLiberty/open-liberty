/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.test.d43.jdbc;

import java.io.PrintWriter;
import java.lang.reflect.Proxy;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

import javax.sql.ConnectionPoolDataSource;
import javax.sql.PooledConnection;
import javax.sql.PooledConnectionBuilder;

public class D43ConnectionPoolDataSource extends D43CommonDataSource implements ConnectionPoolDataSource {
    final org.apache.derby.jdbc.EmbeddedConnectionPoolDataSource ds;

    public D43ConnectionPoolDataSource() {
        ds = new org.apache.derby.jdbc.EmbeddedConnectionPoolDataSource();
    }

    @Override
    public PooledConnectionBuilder createPooledConnectionBuilder() throws SQLException {
        return new D43PooledConnectionBuilder(this);
    }

    public String getDatabaseName() {
        return ds.getDatabaseName();
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return ds.getLoginTimeout();
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return ds.getLogWriter();
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return ds.getParentLogger();
    }

    @Override
    public PooledConnection getPooledConnection() throws SQLException {
        return (PooledConnection) Proxy.newProxyInstance(D43Handler.class.getClassLoader(),
                                                         new Class[] { PooledConnection.class },
                                                         new D43Handler(ds.getPooledConnection(), null, this));
    }

    @Override
    public PooledConnection getPooledConnection(String user, String pwd) throws SQLException {
        return (PooledConnection) Proxy.newProxyInstance(D43Handler.class.getClassLoader(),
                                                         new Class[] { PooledConnection.class },
                                                         new D43Handler(ds.getPooledConnection(user, pwd), null, this));
    }

    public void setDatabaseName(String value) {
        ds.setDatabaseName(value);
    }

    @Override
    public void setLoginTimeout(int value) throws SQLException {
        ds.setLoginTimeout(value);
    }

    @Override
    public void setLogWriter(PrintWriter value) throws SQLException {
        ds.setLogWriter(value);
    }
}