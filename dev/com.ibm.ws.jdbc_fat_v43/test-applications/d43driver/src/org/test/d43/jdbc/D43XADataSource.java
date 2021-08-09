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

import javax.sql.XAConnection;
import javax.sql.XAConnectionBuilder;
import javax.sql.XADataSource;

public class D43XADataSource extends D43CommonDataSource implements XADataSource {
    final org.apache.derby.jdbc.EmbeddedXADataSource ds;

    public D43XADataSource() {
        ds = new org.apache.derby.jdbc.EmbeddedXADataSource();
    }

    @Override
    public XAConnectionBuilder createXAConnectionBuilder() throws SQLException {
        return new D43XAConnectionBuilder(this);
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
    public XAConnection getXAConnection() throws SQLException {
        return (XAConnection) Proxy.newProxyInstance(D43Handler.class.getClassLoader(),
                                                     new Class[] { XAConnection.class },
                                                     new D43Handler(ds.getXAConnection(), null, this));
    }

    @Override
    public XAConnection getXAConnection(String user, String pwd) throws SQLException {
        return (XAConnection) Proxy.newProxyInstance(D43Handler.class.getClassLoader(),
                                                     new Class[] { XAConnection.class },
                                                     new D43Handler(ds.getXAConnection(user, pwd), null, this));
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