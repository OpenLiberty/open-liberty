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
import java.sql.Connection;
import java.sql.ConnectionBuilder;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

import javax.sql.DataSource;

public class D43DataSource extends D43CommonDataSource implements DataSource {
    final org.apache.derby.jdbc.EmbeddedDataSource ds;

    public D43DataSource() {
        ds = new org.apache.derby.jdbc.EmbeddedDataSource();
    }

    @Override
    public ConnectionBuilder createConnectionBuilder() {
        return new D43ConnectionBuilder(this);
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
    public Connection getConnection() throws SQLException {
        return (Connection) Proxy.newProxyInstance(D43Handler.class.getClassLoader(),
                                                   new Class[] { Connection.class },
                                                   new D43Handler(ds.getConnection(), null, this));
    }

    @Override
    public Connection getConnection(String user, String pwd) throws SQLException {
        return (Connection) Proxy.newProxyInstance(D43Handler.class.getClassLoader(),
                                                   new Class[] { Connection.class },
                                                   new D43Handler(ds.getConnection(user, pwd), null, this));
    }

    @Override
    public boolean isWrapperFor(Class<?> type) throws SQLException {
        return type.isInstance(this);
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

    @Override
    public <T> T unwrap(Class<T> type) throws SQLException {
        if (isWrapperFor(type))
            return type.cast(this);
        throw new SQLFeatureNotSupportedException("Not a wrapper for " + type);
    }
}