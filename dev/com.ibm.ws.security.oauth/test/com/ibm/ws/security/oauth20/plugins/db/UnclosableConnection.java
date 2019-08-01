/*******************************************************************************
 * Copyright (c) 2014, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth20.plugins.db;

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

/**
 * A wrapper for java.sql.Connection class that overrides the close() method, such that
 * it will do nothing.
 * 
 * The purpose of this is so that tests that mock using in-memory derby databases, where the
 * implementation code calls the close() does not terminate the instance.
 */
public class UnclosableConnection implements Connection {
    private final Connection conn;

    public UnclosableConnection(Connection conn) {
        this.conn = conn;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.sql.Wrapper#unwrap(java.lang.Class)
     */
    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        // TODO Auto-generated method stub
        return conn.unwrap(iface);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.sql.Wrapper#isWrapperFor(java.lang.Class)
     */
    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        // TODO Auto-generated method stub
        return conn.isWrapperFor(iface);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.sql.Connection#createStatement()
     */
    @Override
    public Statement createStatement() throws SQLException {
        // TODO Auto-generated method stub
        return conn.createStatement();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.sql.Connection#prepareStatement(java.lang.String)
     */
    @Override
    public PreparedStatement prepareStatement(String sql) throws SQLException {
        // TODO Auto-generated method stub
        return conn.prepareStatement(sql);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.sql.Connection#prepareCall(java.lang.String)
     */
    @Override
    public CallableStatement prepareCall(String sql) throws SQLException {
        // TODO Auto-generated method stub
        return conn.prepareCall(sql);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.sql.Connection#nativeSQL(java.lang.String)
     */
    @Override
    public String nativeSQL(String sql) throws SQLException {
        // TODO Auto-generated method stub
        return conn.nativeSQL(sql);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.sql.Connection#setAutoCommit(boolean)
     */
    @Override
    public void setAutoCommit(boolean autoCommit) throws SQLException {
        conn.setAutoCommit(autoCommit);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.sql.Connection#getAutoCommit()
     */
    @Override
    public boolean getAutoCommit() throws SQLException {
        // TODO Auto-generated method stub
        return conn.getAutoCommit();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.sql.Connection#commit()
     */
    @Override
    public void commit() throws SQLException {
        conn.commit();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.sql.Connection#rollback()
     */
    @Override
    public void rollback() throws SQLException {
        conn.rollback();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.sql.Connection#close()
     */
    @Override
    public void close() throws SQLException {
        //Specifically do nothing, because this will close our in memory connection.
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.sql.Connection#isClosed()
     */
    @Override
    public boolean isClosed() throws SQLException {
        return conn.isClosed();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.sql.Connection#getMetaData()
     */
    @Override
    public DatabaseMetaData getMetaData() throws SQLException {
        return conn.getMetaData();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.sql.Connection#setReadOnly(boolean)
     */
    @Override
    public void setReadOnly(boolean readOnly) throws SQLException {
        conn.setReadOnly(readOnly);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.sql.Connection#isReadOnly()
     */
    @Override
    public boolean isReadOnly() throws SQLException {
        return conn.isReadOnly();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.sql.Connection#setCatalog(java.lang.String)
     */
    @Override
    public void setCatalog(String catalog) throws SQLException {
        conn.setCatalog(catalog);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.sql.Connection#getCatalog()
     */
    @Override
    public String getCatalog() throws SQLException {
        return conn.getCatalog();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.sql.Connection#setTransactionIsolation(int)
     */
    @Override
    public void setTransactionIsolation(int level) throws SQLException {
        conn.setTransactionIsolation(level);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.sql.Connection#getTransactionIsolation()
     */
    @Override
    public int getTransactionIsolation() throws SQLException {
        // TODO Auto-generated method stub
        return conn.getTransactionIsolation();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.sql.Connection#getWarnings()
     */
    @Override
    public SQLWarning getWarnings() throws SQLException {
        // TODO Auto-generated method stub
        return conn.getWarnings();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.sql.Connection#clearWarnings()
     */
    @Override
    public void clearWarnings() throws SQLException {
        // TODO Auto-generated method stub
        conn.clearWarnings();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.sql.Connection#createStatement(int, int)
     */
    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
        // TODO Auto-generated method stub
        return conn.createStatement(resultSetType, resultSetConcurrency);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.sql.Connection#prepareStatement(java.lang.String, int, int)
     */
    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        // TODO Auto-generated method stub
        return conn.prepareStatement(sql, resultSetType, resultSetConcurrency);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.sql.Connection#prepareCall(java.lang.String, int, int)
     */
    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        // TODO Auto-generated method stub
        return conn.prepareCall(sql, resultSetType, resultSetConcurrency);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.sql.Connection#getTypeMap()
     */
    @Override
    public Map<String, Class<?>> getTypeMap() throws SQLException {
        // TODO Auto-generated method stub
        return conn.getTypeMap();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.sql.Connection#setTypeMap(java.util.Map)
     */
    @Override
    public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
        // TODO Auto-generated method stub
        conn.setTypeMap(map);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.sql.Connection#setHoldability(int)
     */
    @Override
    public void setHoldability(int holdability) throws SQLException {
        // TODO Auto-generated method stub
        conn.setHoldability(holdability);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.sql.Connection#getHoldability()
     */
    @Override
    public int getHoldability() throws SQLException {
        // TODO Auto-generated method stub
        return conn.getHoldability();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.sql.Connection#setSavepoint()
     */
    @Override
    public Savepoint setSavepoint() throws SQLException {
        // TODO Auto-generated method stub
        return conn.setSavepoint();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.sql.Connection#setSavepoint(java.lang.String)
     */
    @Override
    public Savepoint setSavepoint(String name) throws SQLException {
        // TODO Auto-generated method stub
        return conn.setSavepoint(name);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.sql.Connection#rollback(java.sql.Savepoint)
     */
    @Override
    public void rollback(Savepoint savepoint) throws SQLException {
        // TODO Auto-generated method stub
        conn.rollback();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.sql.Connection#releaseSavepoint(java.sql.Savepoint)
     */
    @Override
    public void releaseSavepoint(Savepoint savepoint) throws SQLException {
        // TODO Auto-generated method stub
        conn.releaseSavepoint(savepoint);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.sql.Connection#createStatement(int, int, int)
     */
    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        // TODO Auto-generated method stub
        return conn.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.sql.Connection#prepareStatement(java.lang.String, int, int, int)
     */
    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        // TODO Auto-generated method stub
        return conn.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.sql.Connection#prepareCall(java.lang.String, int, int, int)
     */
    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        // TODO Auto-generated method stub
        return conn.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.sql.Connection#prepareStatement(java.lang.String, int)
     */
    @Override
    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
        // TODO Auto-generated method stub
        return conn.prepareStatement(sql, autoGeneratedKeys);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.sql.Connection#prepareStatement(java.lang.String, int[])
     */
    @Override
    public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
        // TODO Auto-generated method stub
        return conn.prepareStatement(sql, columnIndexes);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.sql.Connection#prepareStatement(java.lang.String, java.lang.String[])
     */
    @Override
    public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
        // TODO Auto-generated method stub
        return conn.prepareStatement(sql, columnNames);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.sql.Connection#createClob()
     */
    @Override
    public Clob createClob() throws SQLException {
        // TODO Auto-generated method stub
        return conn.createClob();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.sql.Connection#createBlob()
     */
    @Override
    public Blob createBlob() throws SQLException {
        // TODO Auto-generated method stub
        return conn.createBlob();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.sql.Connection#createNClob()
     */
    @Override
    public NClob createNClob() throws SQLException {
        // TODO Auto-generated method stub
        return conn.createNClob();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.sql.Connection#createSQLXML()
     */
    @Override
    public SQLXML createSQLXML() throws SQLException {
        // TODO Auto-generated method stub
        return conn.createSQLXML();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.sql.Connection#isValid(int)
     */
    @Override
    public boolean isValid(int timeout) throws SQLException {
        // TODO Auto-generated method stub
        return conn.isValid(timeout);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.sql.Connection#setClientInfo(java.lang.String, java.lang.String)
     */
    @Override
    public void setClientInfo(String name, String value) throws SQLClientInfoException {
        // TODO Auto-generated method stub
        conn.setClientInfo(name, value);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.sql.Connection#setClientInfo(java.util.Properties)
     */
    @Override
    public void setClientInfo(Properties properties) throws SQLClientInfoException {
        // TODO Auto-generated method stub
        conn.setClientInfo(properties);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.sql.Connection#getClientInfo(java.lang.String)
     */
    @Override
    public String getClientInfo(String name) throws SQLException {
        // TODO Auto-generated method stub
        return conn.getClientInfo(name);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.sql.Connection#getClientInfo()
     */
    @Override
    public Properties getClientInfo() throws SQLException {
        // TODO Auto-generated method stub
        return conn.getClientInfo();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.sql.Connection#createArrayOf(java.lang.String, java.lang.Object[])
     */
    @Override
    public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
        // TODO Auto-generated method stub
        return conn.createArrayOf(typeName, elements);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.sql.Connection#createStruct(java.lang.String, java.lang.Object[])
     */
    @Override
    public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
        // TODO Auto-generated method stub
        return conn.createStruct(typeName, attributes);
    }

    /** Set of Java 7 Interface methods we're implementing but has no effect, so that the compiler doesn't break **/

    public void setSchema(String schema) throws SQLException {}

    public String getSchema() throws SQLException {
        return null;
    }

    public void abort(Executor executor) throws SQLException {}

    public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {}

    public int getNetworkTimeout() throws SQLException {
        return -1;
    }
}