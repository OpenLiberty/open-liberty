/*******************************************************************************
 * Copyright (c) 2017,2023 IBM Corporation and others.
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
package jdbc.driver.proxy;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

import javax.sql.DataSource;

// DataSource that delegates to another JDBC driver.
// Prefer Derby, followed by the "mini" JDBC driver if Derby is unavailable.
// This provides a convenient way to determine if connections made by two different applications
// are being mixed when both use a single server-defined data source.
public class ProxyDataSource implements DataSource {
    private final DataSource ds;
    private final Class<? extends DataSource> dsClass;

    @SuppressWarnings("unchecked")
    public ProxyDataSource() throws Exception {
        boolean isDerby;
        Class<? extends DataSource> c;
        try {
            c = (Class<? extends DataSource>) Class.forName("org.apache.derby.jdbc.EmbeddedDataSource");
            isDerby = true;
        } catch (ClassNotFoundException x) {
            c = (Class<? extends DataSource>) Class.forName("jdbc.driver.mini.MiniDataSource");
            isDerby = false;
        }
        dsClass = c;
        ds = dsClass.newInstance();
        if (isDerby)
            dsClass.getMethod("setCreateDatabase", String.class).invoke(ds, "create");
    }

    @Override
    public Connection getConnection() throws SQLException {
        return ds.getConnection();
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return ds.getConnection(username, password);
    }

    public String getDatabaseName() throws Exception {
        return (String) dsClass.getMethod("getDatabaseName").invoke(ds);
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
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return ds.isWrapperFor(iface);
    }

    public void setDatabaseName(String databaseName) throws Exception {
        dsClass.getMethod("setDatabaseName", String.class).invoke(ds, databaseName);
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        ds.setLoginTimeout(seconds);
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        ds.setLogWriter(out);
    }

    // The otherwise unused portNumber property simulates an odd behavior of the Microsoft JDBC driver
    // where String typed accessor and setter are provided along with a second accessor that accepts
    // a primitive value, where the Liberty configuration defines the property as the primitive type.
    // A user observed this with String/boolean, but it also reproduces with String/int.
    public void setPortNumber(String portNumber) throws Exception {
        if (!"-1".equals(portNumber))
            throw new IllegalArgumentException("portNumber: " + portNumber);
    }

    public void setPortNumber(int portNumber) throws Exception {
        setPortNumber(Integer.valueOf(portNumber).toString());
    }

    public String getPortNumber() throws Exception {
        return null;
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return ds.unwrap(iface);
    }
}