/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.persistence.internal.eclipselink.sql.delegate;

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

import com.ibm.websphere.ras.annotation.Trivial;

/**
 * An abstract Connection implementation that allows for easier extension.
 */
@Trivial
public abstract class DelegatingConnection implements Connection {
     private final Connection _del;

     public DelegatingConnection(Connection conn) {
          _del = conn;
     }

     public void abort(Executor executor) throws SQLException {

     }

     public void clearWarnings() throws SQLException {
          _del.clearWarnings();
     }

     public void close() throws SQLException {
          _del.close();
     }

     public void commit() throws SQLException {
          _del.commit();
     }

     public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
          return _del.createArrayOf(typeName, elements);
     }

     public Blob createBlob() throws SQLException {
          return _del.createBlob();
     }

     public Clob createClob() throws SQLException {
          return _del.createClob();
     }

     public NClob createNClob() throws SQLException {
          return _del.createNClob();
     }

     public SQLXML createSQLXML() throws SQLException {
          return _del.createSQLXML();
     }

     public Statement createStatement() throws SQLException {
          return _del.createStatement();
     }

     public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
          return _del.createStatement(resultSetType, resultSetConcurrency);
     }

     public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability)
          throws SQLException {
          return _del.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability);
     }

     public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
          return _del.createStruct(typeName, attributes);
     }

     public boolean getAutoCommit() throws SQLException {
          return _del.getAutoCommit();
     }

     public String getCatalog() throws SQLException {
          return _del.getCatalog();
     }

     public Properties getClientInfo() throws SQLException {
          return _del.getClientInfo();
     }

     public String getClientInfo(String name) throws SQLException {
          return _del.getClientInfo(name);
     }

     public int getHoldability() throws SQLException {
          return _del.getHoldability();
     }

     public DatabaseMetaData getMetaData() throws SQLException {
          return _del.getMetaData();
     }

     public int getNetworkTimeout() throws SQLException {
          return 0;
     }

     public String getSchema() throws SQLException {
          return null;
     }

     public int getTransactionIsolation() throws SQLException {
          return _del.getTransactionIsolation();
     }

     public Map<String, Class<?>> getTypeMap() throws SQLException {
          return _del.getTypeMap();
     }

     public SQLWarning getWarnings() throws SQLException {
          return _del.getWarnings();
     }

     public boolean isClosed() throws SQLException {
          return _del.isClosed();
     }

     public boolean isReadOnly() throws SQLException {
          return _del.isReadOnly();
     }

     public boolean isValid(int timeout) throws SQLException {
          return _del.isValid(timeout);
     }

     public boolean isWrapperFor(Class<?> iface) throws SQLException {
          return _del.isWrapperFor(iface);
     }

     public String nativeSQL(String sql) throws SQLException {
          return _del.nativeSQL(sql);
     }

     public CallableStatement prepareCall(String sql) throws SQLException {
          return _del.prepareCall(sql);
     }

     public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
          return _del.prepareCall(sql, resultSetType, resultSetConcurrency);
     }

     public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency,
          int resultSetHoldability) throws SQLException {
          return _del.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
     }

     public PreparedStatement prepareStatement(String sql) throws SQLException {
          return _del.prepareStatement(sql);
     }

     public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
          return _del.prepareStatement(sql, autoGeneratedKeys);
     }

     public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency)
          throws SQLException {
          return _del.prepareStatement(sql, resultSetType, resultSetConcurrency);
     }

     public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency,
          int resultSetHoldability) throws SQLException {
          return _del.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
     }

     public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
          return _del.prepareStatement(sql, columnIndexes);
     }

     public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
          return _del.prepareStatement(sql, columnNames);
     }

     public void releaseSavepoint(Savepoint savepoint) throws SQLException {
          _del.releaseSavepoint(savepoint);
     }

     public void rollback() throws SQLException {
          _del.rollback();
     }

     public void rollback(Savepoint savepoint) throws SQLException {
          _del.rollback(savepoint);
     }

     public void setAutoCommit(boolean autoCommit) throws SQLException {
          _del.setAutoCommit(autoCommit);
     }

     public void setCatalog(String catalog) throws SQLException {
          _del.setCatalog(catalog);
     }

     public void setClientInfo(Properties properties) throws SQLClientInfoException {
          _del.setClientInfo(properties);
     }

     public void setClientInfo(String name, String value) throws SQLClientInfoException {
          _del.setClientInfo(name, value);
     }

     public void setHoldability(int holdability) throws SQLException {
          _del.setHoldability(holdability);
     }

     public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {

     }

     public void setReadOnly(boolean readOnly) throws SQLException {
          _del.setReadOnly(readOnly);
     }

     public Savepoint setSavepoint() throws SQLException {
          return _del.setSavepoint();
     }

     public Savepoint setSavepoint(String name) throws SQLException {
          return _del.setSavepoint(name);
     }

     public void setSchema(String schema) throws SQLException {
     }

     public void setTransactionIsolation(int level) throws SQLException {
          _del.setTransactionIsolation(level);
     }

     public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
          _del.setTypeMap(map);
     }

     public <T> T unwrap(Class<T> iface) throws SQLException {
          return _del.unwrap(iface);
     }

}
