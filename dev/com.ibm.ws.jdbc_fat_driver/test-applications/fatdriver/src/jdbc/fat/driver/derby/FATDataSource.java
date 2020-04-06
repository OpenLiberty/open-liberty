/*******************************************************************************
 * Copyright (c) 2018,2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package jdbc.fat.driver.derby;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

import javax.sql.CommonDataSource;
import javax.sql.DataSource;

public class FATDataSource implements DataSource, FATJDBCSpecialOps {
    protected final CommonDataSource derbyds;

    public FATDataSource() throws Exception {
        derbyds = (DataSource) Class.forName("org.apache.derby.jdbc.EmbeddedDataSource").newInstance();
    }

    protected FATDataSource(CommonDataSource derbyds) {
        this.derbyds = derbyds;
    }

    @Override
    public FATVendorSpecificSomething createSomething(int someValue) {
        return new FATVendorSpecificSomething(someValue);
    }

    public boolean getAutoCreate() throws Exception {
        return "create".equals(derbyds.getClass().getMethod("getCreateDatabase").invoke(derbyds));
    }

    @Override
    public Connection getConnection() throws SQLException {
        return ((DataSource) derbyds).getConnection();
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return ((DataSource) derbyds).getConnection(username, password);
    }

    public String getDatabaseName() throws Exception {
        return (String) derbyds.getClass().getMethod("getDatabaseName").invoke(derbyds);
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return derbyds.getLoginTimeout();
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return derbyds.getLogWriter();
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return derbyds.getParentLogger();
    }

    public String getServerName() {
        return "localhost";
    }

    public void setAutoCreate(boolean value) throws Exception {
        derbyds.getClass().getMethod("setCreateDatabase", String.class).invoke(derbyds, value ? "create" : "false");
    }

    public void setDatabaseName(String value) throws Exception {
        derbyds.getClass().getMethod("setDatabaseName", String.class).invoke(derbyds, value);
    }

    @Override
    public void setLoginTimeout(int value) throws SQLException {
        derbyds.setLoginTimeout(value);
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        derbyds.setLogWriter(out);
    }

    public void setPassword(String value) throws Exception {
        derbyds.getClass().getMethod("setPassword", String.class).invoke(derbyds, value);
    }

    // @DataSourceDefinition supplies default value of 'localhost' for serverName.
    // Accept this value so that warnings are not generated in the logs.
    public void setServerName(String value) throws Exception {
        if (!"localhost".equals(value))
            throw new IllegalArgumentException(value);
    }

    public void setUser(String value) throws Exception {
        derbyds.getClass().getMethod("setUser", String.class).invoke(derbyds, value);
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (isWrapperFor(iface))
            return iface.cast(this);
        else
            throw new SQLException(this + " does not wrap " + iface);
    }

    @Override
    public int useAnything(Object anything) {
        return ((FATVendorSpecificSomething) anything).increment();
    }

    @Override
    public int useSomething(FATVendorSpecificSomething something) {
        return something.increment();
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return iface.isInterface() && iface.isAssignableFrom(getClass());
    }
}