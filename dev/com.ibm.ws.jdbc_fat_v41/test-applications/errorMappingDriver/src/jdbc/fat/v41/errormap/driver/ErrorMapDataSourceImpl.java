/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package jdbc.fat.v41.errormap.driver;

import java.io.PrintWriter;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Logger;

import org.apache.derby.jdbc.EmbeddedDataSource40;

/**
 * This class is a wrapper around the EmbeddedDataSource40.
 * It allows the unit tests to override the implementation of methods.
 */
public class ErrorMapDataSourceImpl implements ErrorMapDataSource, Serializable {
    private static final long serialVersionUID = -8721843807690710887L;
    private final EmbeddedDataSource40 impl;

    private Integer errorCode = null;
    private String sqlState = null;

    public ErrorMapDataSourceImpl() {
        impl = new EmbeddedDataSource40();
    }

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

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Logger getParentLogger() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setLogWriter(PrintWriter arg0) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setLoginTimeout(int arg0) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isWrapperFor(Class<?> arg0) throws SQLException {
        return ErrorMapDataSource.class.equals(arg0) || impl.isWrapperFor(arg0);
    }

    @Override
    public <T> T unwrap(Class<T> arg0) throws SQLException {
        if (ErrorMapDataSource.class.equals(arg0))
            return (T) this;
        return impl.unwrap(arg0);
    }

    @Override
    public Connection getConnection() throws SQLException {
        log("Getting a connection with no user/pass");
        blowupIfRequested();
        return new ErrorMapConnectionImpl(this, impl.getConnection());
    }

    @Override
    public Connection getConnection(String user, String pass) throws SQLException {
        log("Getting a connection with user=" + user + " pass=" + pass);
        blowupIfRequested();
        return new ErrorMapConnectionImpl(this, impl.getConnection(user, pass));
    }

    private void log(String msg) {
        System.out.println("[ErrorMapDataSourceImpl]: " + msg);
    }

    @Override
    public void mockNextErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }

    @Override
    public void mockNextSqlState(String sqlState) {
        this.sqlState = sqlState;
    }

    private void blowupIfRequested() throws SQLException {
        if (errorCode == null && sqlState == null)
            return;

        // need to blow up with requeted sqlstate or errorCode
        String msg = "Throwing an exception requsted by the test application. errorCode=" + errorCode + " sqlState=" + sqlState;
        SQLException ex = errorCode != null ? //
                        new SQLException(msg, sqlState, errorCode) : //
                        new SQLException(msg, sqlState);
        log(msg);
        sqlState = null;
        errorCode = null;
        throw ex;
    }
}
