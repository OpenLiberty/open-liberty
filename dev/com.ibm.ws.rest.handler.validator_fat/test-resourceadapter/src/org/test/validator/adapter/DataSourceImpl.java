/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.test.validator.adapter;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLInvalidAuthorizationSpecException;
import java.util.logging.Logger;

import javax.resource.ResourceException;
import javax.resource.spi.ConnectionManager;
import javax.sql.DataSource;

import org.test.validator.adapter.ConnectionSpecImpl.ConnectionRequestInfoImpl;

public class DataSourceImpl implements DataSource {
    private final ConnectionManager cm;
    final ManagedConnectionFactoryImpl mcf;

    DataSourceImpl(ConnectionManager cm, ManagedJDBCConnectionFactoryImpl mcf) {
        this.cm = cm;
        this.mcf = mcf;
    }

    @Override
    public Connection getConnection() throws SQLException {
        return getConnection(null, null);
    }

    @Override
    public Connection getConnection(String user, String password) throws SQLException {
        ConnectionSpecImpl conSpec = new ConnectionSpecImpl();
        conSpec.setConnectionImplClass(JDBCConnectionImpl.class.getName());
        if (user != null)
            conSpec.setUserName(user);
        if (password != null)
            conSpec.setPassword(password);
        ConnectionRequestInfoImpl cri = conSpec.createConnectionRequestInfo();
        try {
            return (Connection) cm.allocateConnection(mcf, cri);
        } catch (SecurityException x) {
            throw new SQLInvalidAuthorizationSpecException(x);
        } catch (ResourceException x) {
            throw new SQLException(x);
        }
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return 0;
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return mcf.getLogWriter();
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return null;
    }

    @Override
    public boolean isWrapperFor(Class<?> ifc) throws SQLException {
        return ifc.isAssignableFrom(getClass());
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        mcf.setLogWriter(out);
    }

    @Override
    public <T> T unwrap(Class<T> ifc) throws SQLException {
        return isWrapperFor(ifc) ? ifc.cast(this) : null;
    }
}
