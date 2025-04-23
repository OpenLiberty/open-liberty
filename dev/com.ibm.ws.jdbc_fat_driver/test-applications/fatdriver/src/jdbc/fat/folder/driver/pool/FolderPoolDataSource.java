/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
package jdbc.fat.folder.driver.pool;

import java.io.PrintWriter;
import java.lang.reflect.Proxy;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Logger;

import javax.sql.ConnectionPoolDataSource;
import javax.sql.PooledConnection;

import jdbc.fat.folder.driver.FolderJDBCHandler;

/**
 * A ConnectionPoolDataSource for a fake JDBC driver that is packaged in a folder
 * rather than a jar file.
 */
public class FolderPoolDataSource implements ConnectionPoolDataSource {
    private final Properties props = new Properties();

    public String getCatalog() {
        return props.getProperty("Catalog");
    }

    @Override
    public PooledConnection getPooledConnection() throws SQLException {
        return getPooledConnection(null, null);
    }

    @Override
    public PooledConnection getPooledConnection(String user, String password) {
        Properties info = (Properties) props.clone();
        info.setProperty("DatabaseProductName", "FolderDB");
        info.setProperty("DatabaseProductVersion", "1.0.0.0");
        info.setProperty("DriverName", "FolderJDBC");
        info.setProperty("DriverVersion", "1.0");
        info.setProperty("DatabaseMajorVersion", "1");
        info.setProperty("DatabaseMinorVersion", "0");
        info.setProperty("JDBCMajorVersion", "4");
        info.setProperty("JDBCMinorVersion", "1");
        if (user != null)
            info.setProperty("UserName", user);

        return (PooledConnection) Proxy //
                        .newProxyInstance(getClass().getClassLoader(),
                                          new Class<?>[] { PooledConnection.class },
                                          new FolderJDBCHandler(PooledConnection.class, info));
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return Integer.parseInt(props.getProperty("LoginTimeout", "0"));
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException();
    }

    public String getSchema() {
        return props.getProperty("Schema");
    }

    public void setCatalog(String value) {
        props.put("Catalog", value);
    }

    @Override
    public void setLoginTimeout(int seconds) {
        props.put("LoginTimeout", Integer.toString(seconds));
    }

    @Override
    public void setLogWriter(PrintWriter out) {
    }

    public void setSchema(String value) {
        props.put("Schema", value);
    }
}
