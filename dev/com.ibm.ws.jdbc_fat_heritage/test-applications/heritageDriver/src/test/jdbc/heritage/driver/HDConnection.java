/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.jdbc.heritage.driver;

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
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

public class HDConnection implements Connection {
    final HDDataSource ds;
    final Connection derbycon;

    HDConnection(HDDataSource ds, Connection con) {
        this.ds = ds;
        this.derbycon = con;
    }

    @Override
    public void abort(Executor executor) throws SQLException {
        derbycon.abort(executor);
    }

    @Override
    public void close() throws SQLException {
        derbycon.close();
    }

    @Override
    public void clearWarnings() throws SQLException {
        derbycon.clearWarnings();
    }

    @Override
    public void commit() throws SQLException {
        derbycon.commit();
    }

    @Override
    public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
        return derbycon.createArrayOf(typeName, elements);
    }

    @Override
    public Blob createBlob() throws SQLException {
        return derbycon.createBlob();
    }

    @Override
    public Clob createClob() throws SQLException {
        return derbycon.createClob();
    }

    @Override
    public NClob createNClob() throws SQLException {
        return derbycon.createNClob();
    }

    @Override
    public SQLXML createSQLXML() throws SQLException {
        return derbycon.createSQLXML();
    }

    @Override
    public Statement createStatement() throws SQLException {
        return derbycon.createStatement();
    }

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
        return derbycon.createStatement(resultSetType, resultSetConcurrency);
    }

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        return derbycon.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    @Override
    public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
        return derbycon.createStruct(typeName, attributes);
    }

    @Override
    public boolean getAutoCommit() throws SQLException {
        return derbycon.getAutoCommit();
    }

    @Override
    public String getCatalog() throws SQLException {
        if (ds.supportsCatalog)
            return derbycon.getCatalog();
        else
            throw new SQLException("You disabled support for catalog.");
    }

    @Override
    public Properties getClientInfo() throws SQLException {
        return derbycon.getClientInfo();
    }

    @Override
    public String getClientInfo(String name) throws SQLException {
        return derbycon.getClientInfo(name);
    }

    @Override
    public int getHoldability() throws SQLException {
        return derbycon.getHoldability();
    }

    @Override
    public DatabaseMetaData getMetaData() throws SQLException {
        return derbycon.getMetaData();
    }

    @Override
    public int getNetworkTimeout() throws SQLException {
        return derbycon.getNetworkTimeout();
    }

    @Override
    public String getSchema() throws SQLException {
        return derbycon.getSchema();
    }

    @Override
    public int getTransactionIsolation() throws SQLException {
        return derbycon.getTransactionIsolation();
    }

    @Override
    public Map<String, Class<?>> getTypeMap() throws SQLException {
        return derbycon.getTypeMap();
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        return derbycon.getWarnings();
    }

    @Override
    public boolean isClosed() throws SQLException {
        return derbycon.isClosed();
    }

    @Override
    public boolean isReadOnly() throws SQLException {
        return derbycon.isReadOnly();
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return derbycon.isWrapperFor(iface);
    }

    @Override
    public boolean isValid(int timeout) throws SQLException {
        return derbycon.isValid(timeout);
    }

    @Override
    public String nativeSQL(String sql) throws SQLException {
        return derbycon.nativeSQL(sql);
    }

    @Override
    public CallableStatement prepareCall(String sql) throws SQLException {
        return derbycon.prepareCall(sql);
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        return derbycon.prepareCall(sql, resultSetType, resultSetConcurrency);
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        return derbycon.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    @Override
    public PreparedStatement prepareStatement(String sql) throws SQLException {
        return derbycon.prepareStatement(sql);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
        return derbycon.prepareStatement(sql, autoGeneratedKeys);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        return derbycon.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        return derbycon.prepareStatement(sql, resultSetType, resultSetConcurrency);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
        return derbycon.prepareStatement(sql, columnIndexes);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
        return derbycon.prepareStatement(sql, columnNames);
    }

    @Override
    public void releaseSavepoint(Savepoint savepoint) throws SQLException {
        derbycon.releaseSavepoint(savepoint);
    }

    @Override
    public void rollback() throws SQLException {
        derbycon.rollback();
    }

    @Override
    public void rollback(Savepoint savepoint) throws SQLException {
        derbycon.rollback(savepoint);
    }

    @Override
    public void setAutoCommit(boolean autoCommit) throws SQLException {
        derbycon.setAutoCommit(autoCommit);
    }

    @Override
    public void setCatalog(String catalog) throws SQLException {
        if (ds.supportsCatalog)
            derbycon.setCatalog(catalog);
        else
            throw new SQLException("You disabled support for catalog.");
    }

    @Override
    public void setClientInfo(Properties properties) throws SQLClientInfoException {
        derbycon.setClientInfo(properties);
    }

    @Override
    public void setClientInfo(String name, String value) throws SQLClientInfoException {
        derbycon.setClientInfo(name, value);
    }

    @Override
    public void setHoldability(int holdability) throws SQLException {
        derbycon.setHoldability(holdability);
    }

    @Override
    public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
        derbycon.setNetworkTimeout(executor, milliseconds);
    }

    @Override
    public void setReadOnly(boolean readOnly) throws SQLException {
        derbycon.setReadOnly(readOnly);
    }

    @Override
    public Savepoint setSavepoint() throws SQLException {
        return derbycon.setSavepoint();
    }

    @Override
    public Savepoint setSavepoint(String name) throws SQLException {
        return derbycon.setSavepoint(name);
    }

    @Override
    public void setSchema(String schema) throws SQLException {
        derbycon.setSchema(schema);
    }

    @Override
    public void setTransactionIsolation(int level) throws SQLException {
        derbycon.setTransactionIsolation(level);
    }

    @Override
    public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
        derbycon.setTypeMap(map);
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return derbycon.unwrap(iface);
    }
}