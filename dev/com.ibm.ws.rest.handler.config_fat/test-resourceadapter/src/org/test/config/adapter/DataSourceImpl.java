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
package org.test.config.adapter;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLInvalidAuthorizationSpecException;
import java.util.logging.Logger;

import javax.resource.ResourceException;
import javax.resource.spi.ConnectionManager;
import javax.sql.DataSource;

public class DataSourceImpl implements DataSource {
    private final ConnectionManager cm;
    private final ManagedConnectionFactoryImpl mcf;

    DataSourceImpl(ConnectionManager cm, ManagedConnectionFactoryImpl mcf) {
        this.cm = cm;
        this.mcf = mcf;
    }

    @Override
    public Connection getConnection() throws SQLException {
        return getConnection(null, null);
    }

    @Override
    public Connection getConnection(String user, String password) throws SQLException {
        ConnectionSpecImpl cri = new ConnectionSpecImpl();
        cri.interfaces = new Class<?>[] { Connection.class, DatabaseMetaData.class };
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
        return null;
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
    }

    @Override
    public <T> T unwrap(Class<T> ifc) throws SQLException {
        return isWrapperFor(ifc) ? ifc.cast(this) : null;
    }
}
