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
package jdbc.fat.folder.driver;

import java.io.PrintWriter;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Logger;

import javax.sql.DataSource;

/**
 * A DataSource for a fake JDBC driver that is packaged in a folder
 * rather than a jar file.
 */
public class FolderDataSource implements DataSource {
    private final Properties props = new Properties();

    public String getCatalog() {
        return props.getProperty("Catalog");
    }

    @Override
    public Connection getConnection() throws SQLException {
        return getConnection(null, null);
    }

    @Override
    public Connection getConnection(String user, String password) {
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

        return (Connection) Proxy //
                        .newProxyInstance(getClass().getClassLoader(),
                                          new Class<?>[] { Connection.class },
                                          new FolderJDBCHandler(Connection.class, info));
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

    @Override
    public boolean isWrapperFor(Class<?> ifc) throws SQLException {
        return ifc.isInstance(this);
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

    @Override
    public <T> T unwrap(Class<T> ifc) throws SQLException {
        if (ifc.isInstance(this))
            return ifc.cast(this);
        else
            throw new SQLException(this + " doesn't wrap " + ifc);
    }
}
