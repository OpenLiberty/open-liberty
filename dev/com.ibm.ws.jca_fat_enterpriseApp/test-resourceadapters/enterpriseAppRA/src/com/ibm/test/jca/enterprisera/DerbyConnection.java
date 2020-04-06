/*******************************************************************************
 * Copyright (c) 2013,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.test.jca.enterprisera;

import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLNonTransientConnectionException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

import javax.resource.ResourceException;
import javax.resource.spi.ConnectionEvent;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.LazyAssociatableConnectionManager;

public class DerbyConnection implements Connection {
    ConnectionManager cm;
    DerbyConnectionRequestInfo cri;
    boolean isClosed;
    DerbyManagedConnection mc;
    DerbyManagedConnectionFactory mcf;

    DerbyConnection(DerbyManagedConnection mc) {
        this.cri = mc.cri;
        this.mc = mc;
        this.mcf = mc.mcf;
    }

    DerbyConnection init(ConnectionManager cm) {
        this.cm = cm;
        return this;
    }

    private void lazyInit() throws SQLException {
        if (isClosed)
            throw new SQLNonTransientConnectionException("closed");

        try {
            if (mc == null && mcf.isDissociatable()) {
                ((LazyAssociatableConnectionManager) cm).associateConnection(this, mcf, cri);
                mc.con = mc.xacon.getConnection();
            }
        } catch (ResourceException x) {
            throw new SQLException(x);
        }
    }

    @Override
    public void close() {
        if (!isClosed) {
            isClosed = true;
            if (mc != null)
                mc.notify(ConnectionEvent.CONNECTION_CLOSED, this, null);
            mc = null;
        }
    }

    @Override
    public void clearWarnings() throws SQLException {
        lazyInit();
        mc.con.clearWarnings();
    }

    @Override
    public void commit() throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
        lazyInit();
        return mc.con.createArrayOf(typeName, elements);
    }

    @Override
    public Blob createBlob() throws SQLException {
        lazyInit();
        return mc.con.createBlob();
    }

    @Override
    public Clob createClob() throws SQLException {
        lazyInit();
        return mc.con.createClob();
    }

    @Override
    public NClob createNClob() throws SQLException {
        lazyInit();
        return mc.con.createNClob();
    }

    @Override
    public SQLXML createSQLXML() throws SQLException {
        lazyInit();
        return mc.con.createSQLXML();
    }

    @Override
    public Statement createStatement() throws SQLException {
        lazyInit();
        return mc.con.createStatement();
    }

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
        lazyInit();
        return mc.con.createStatement(resultSetType, resultSetConcurrency);
    }

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        lazyInit();
        return mc.con.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    @Override
    public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
        lazyInit();
        return mc.con.createStruct(typeName, attributes);
    }

    @Override
    public boolean getAutoCommit() throws SQLException {
        lazyInit();
        return mc.con.getAutoCommit();
    }

    @Override
    public String getCatalog() throws SQLException {
        lazyInit();
        return mc.con.getCatalog();
    }

    @Override
    public String getClientInfo(String name) throws SQLException {
        lazyInit();
        return mc.con.getClientInfo(name);
    }

    @Override
    public Properties getClientInfo() throws SQLException {
        lazyInit();
        return mc.con.getClientInfo();
    }

    @Override
    public int getHoldability() throws SQLException {
        lazyInit();
        return mc.con.getHoldability();
    }

    @Override
    public DatabaseMetaData getMetaData() throws SQLException {
        lazyInit();
        return mc.con.getMetaData();
    }

    @Override
    public int getTransactionIsolation() throws SQLException {
        lazyInit();
        return mc.con.getTransactionIsolation();
    }

    @Override
    public Map<String, Class<?>> getTypeMap() throws SQLException {
        lazyInit();
        return mc.con.getTypeMap();
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        lazyInit();
        return mc.con.getWarnings();
    }

    @Override
    public boolean isClosed() throws SQLException {
        lazyInit();
        return mc == null || mc.con.isClosed();
    }

    @Override
    public boolean isReadOnly() throws SQLException {
        lazyInit();
        return mc.con.isReadOnly();
    }

    @Override
    public boolean isValid(int timeout) throws SQLException {
        lazyInit();
        return mc.con.isValid(timeout);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        lazyInit();
        return mc.con.isWrapperFor(iface);
    }

    @Override
    public String nativeSQL(String sql) throws SQLException {
        lazyInit();
        return mc.con.nativeSQL(sql);
    }

    @Override
    public CallableStatement prepareCall(String sql) throws SQLException {
        lazyInit();
        return mc.con.prepareCall(sql);
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        lazyInit();
        return mc.con.prepareCall(sql, resultSetType, resultSetConcurrency);
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        lazyInit();
        return mc.con.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    @Override
    public PreparedStatement prepareStatement(String sql) throws SQLException {
        lazyInit();
        return mc.con.prepareStatement(sql);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
        lazyInit();
        return mc.con.prepareStatement(sql, autoGeneratedKeys);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
        lazyInit();
        return mc.con.prepareStatement(sql, columnIndexes);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        lazyInit();
        return mc.con.prepareStatement(sql, resultSetType, resultSetConcurrency);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        lazyInit();
        return mc.con.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
        lazyInit();
        return mc.con.prepareStatement(sql, columnNames);
    }

    @Override
    public void releaseSavepoint(Savepoint savepoint) throws SQLException {
        lazyInit();
        mc.con.releaseSavepoint(savepoint);
    }

    @Override
    public void rollback() throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public void rollback(Savepoint savepoint) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public void setAutoCommit(boolean autoCommit) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public void setCatalog(String catalog) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public void setHoldability(int holdability) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public void setReadOnly(boolean readOnly) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public Savepoint setSavepoint() throws SQLException {
        lazyInit();
        return mc.con.setSavepoint();
    }

    @Override
    public Savepoint setSavepoint(String name) throws SQLException {
        lazyInit();
        return mc.con.setSavepoint(name);
    }

    @Override
    public void setTransactionIsolation(int level) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public void setClientInfo(String name, String value) throws SQLClientInfoException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setClientInfo(Properties properties) throws SQLClientInfoException {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        lazyInit();
        return mc.con.unwrap(iface);
    }

    //@Override // Java 7
    @Override
    public void setSchema(String schema) {
        throw new UnsupportedOperationException();
    }

    //@Override // Java 7
    @Override
    public String getSchema() {
        throw new UnsupportedOperationException();
    }

    //@Override // Java 7
    @Override
    public void abort(Executor executor) {
        throw new UnsupportedOperationException();
    }

    //@Override // Java 7
    @Override
    public void setNetworkTimeout(Executor executor, int milliseconds) {
        throw new UnsupportedOperationException();
    }

    //@Override // Java 7
    @Override
    public int getNetworkTimeout() {
        throw new UnsupportedOperationException();
    }
}