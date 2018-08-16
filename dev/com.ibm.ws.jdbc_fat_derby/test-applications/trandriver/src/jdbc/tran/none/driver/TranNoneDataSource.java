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
package jdbc.tran.none.driver;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

import javax.sql.DataSource;

import org.apache.derby.jdbc.EmbeddedDataSource;

/**
 *
 */
public class TranNoneDataSource implements DataSource {
    private final EmbeddedDataSource impl;
    private String serverName;

    /* Methods for wrapping Derby Embedded */
    public void setCreateDatabase(String create) {
        impl.setCreateDatabase(create);
    }

    public String getCreateDatabase() {
        return impl.getCreateDatabase();
    }

    public void setDatabaseName(String name) {
        impl.setDatabaseName(name);
    }

    public String getDatabaseName() {
        return impl.getDatabaseName();
    }

    public TranNoneDataSource() {
        impl = new EmbeddedDataSource();
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public String getServerName() {
        return this.serverName;
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return impl.getLogWriter();
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return impl.getLoginTimeout();
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return impl.getParentLogger();
    }

    @Override
    public void setLogWriter(PrintWriter arg0) throws SQLException {
        impl.setLogWriter(arg0);
    }

    @Override
    public void setLoginTimeout(int arg0) throws SQLException {
        impl.setLoginTimeout(arg0);
    }

    @Override
    public boolean isWrapperFor(Class<?> arg0) throws SQLException {
        return impl.isWrapperFor(arg0);
    }

    @Override
    public <T> T unwrap(Class<T> arg0) throws SQLException {
        return impl.unwrap(arg0);
    }

    @Override
    public Connection getConnection() throws SQLException {
        return new TranNoneConnection(impl.getConnection());
    }

    @Override
    public Connection getConnection(String arg0, String arg1) throws SQLException {
        return new TranNoneConnection(impl.getConnection(arg0, arg1));
    }

}
