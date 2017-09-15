/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package jdbc.fat.v41.slowdriver;

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
public class TimeoutDataSourceImpl implements TimeoutDataSource, Serializable {
    private static final long serialVersionUID = -8721843807690710887L;
    private final EmbeddedDataSource40 impl;
    private int databaseDelay = 0;

    public TimeoutDataSourceImpl() {
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

    /**
     * Testing utility method to simulate a database latency.
     * NOTE: This method cannot return void because our proxy class does not allow
     * void setter methods to be invoked.
     *
     * @param milliseconds
     */
    @Override
    public boolean setLatency(int milliseconds) {
        databaseDelay = milliseconds;
        log("Set database latency to " + databaseDelay + "ms");
        return true;
    }

    /**
     * Gets the simulated database latency.
     *
     * @return
     */
    @Override
    public int getLatency() {
        return databaseDelay;
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
        return impl.isWrapperFor(arg0);
    }

    @Override
    public <T> T unwrap(Class<T> arg0) throws SQLException {
        return impl.unwrap(arg0);
    }

    @Override
    public Connection getConnection() throws SQLException {
        log("Getting a connection");
        return new TimeoutConnection(this, impl.getConnection());
    }

    @Override
    public Connection getConnection(String arg0, String arg1) throws SQLException {
        return new TimeoutConnection(this, impl.getConnection(arg0, arg1));
    }

    private void log(String msg) {
        System.out.println("[TimeoutDataSourceImpl]: " + msg);
    }
}
