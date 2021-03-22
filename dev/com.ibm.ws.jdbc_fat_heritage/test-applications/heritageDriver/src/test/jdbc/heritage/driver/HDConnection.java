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

import java.lang.reflect.Constructor;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.ClientInfoStatus;
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
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class HDConnection implements Connection, HeritageDBConnection {
    private static final Set<String> DEFAULT_CLIENT_INFO_KEYS = Stream.of("ApplicationName", "ClientHostname", "ClientUser").collect(Collectors.toSet());
    public static final int DEFAULT_MAX_FIELD_SIZE = 225;

    /**
     * Counts the number of times that doConnectionCleanupPerCloseConnection is invoked for this connection.
     */
    public final AtomicInteger cleanupCount = new AtomicInteger();

    final HDDataSource ds;
    final Connection derbycon;
    Set<String> clientInfoKeys = DEFAULT_CLIENT_INFO_KEYS;

    private Map<Object, Class<?>> exceptionIdentificationOverrides;
    private boolean failOnIsValid;

    /**
     * Counts the number of times that doConnectionSetupPerGetConnection is invoked for this connection.
     */
    public final AtomicInteger setupCount = new AtomicInteger();

    /**
     * Counts the number of times that doConnectionSetupPerTransaction is invoked for this connection.
     */
    public final AtomicInteger transactionCount = new AtomicInteger(-1);

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
        Statement s = derbycon.createStatement();
        s.setMaxFieldSize(DEFAULT_MAX_FIELD_SIZE);
        return s;
    }

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
        Statement s = derbycon.createStatement(resultSetType, resultSetConcurrency);
        s.setMaxFieldSize(DEFAULT_MAX_FIELD_SIZE);
        return s;
    }

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        Statement s = derbycon.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability);
        s.setMaxFieldSize(DEFAULT_MAX_FIELD_SIZE);
        return s;
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
    public Set<String> getClientInfoKeys() {
        return clientInfoKeys;
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
        if (ds.supportsNetworkTimeout)
            return derbycon.getNetworkTimeout();
        else
            throw new SQLException("You disabled the ability to get the network timeout.");
    }

    @Override
    public String getSchema() throws SQLException {
        if (ds.supportsSchema)
            return derbycon.getSchema();
        else
            throw new SQLException("You disabled the ability to get the schema.");
    }

    @Override
    public int getTransactionIsolation() throws SQLException {
        return derbycon.getTransactionIsolation();
    }

    @Override
    public Map<String, Class<?>> getTypeMap() throws SQLException {
        if (ds.supportsTypeMap)
            return derbycon.getTypeMap();
        else
            throw new SQLException("You disabled support for type map.");
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
        if (ds.supportsReadOnly)
            return derbycon.isReadOnly();
        else
            throw new SQLException("You disabled support for read only.");
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return derbycon.isWrapperFor(iface);
    }

    @Override
    public boolean isValid(int timeout) throws SQLException {
        if (failOnIsValid)
            throw new SQLException("Test case asked for isValid to fail, and so it is.", null, 44098);

        return derbycon.isValid(timeout);
    }

    @Override
    public String nativeSQL(String sql) throws SQLException {
        return derbycon.nativeSQL(sql);
    }

    @Override
    public CallableStatement prepareCall(String sql) throws SQLException {
        CallableStatement s = derbycon.prepareCall(replace(sql));
        s.setMaxFieldSize(DEFAULT_MAX_FIELD_SIZE);
        return s;
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        CallableStatement s = derbycon.prepareCall(replace(sql), resultSetType, resultSetConcurrency);
        s.setMaxFieldSize(DEFAULT_MAX_FIELD_SIZE);
        return s;
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        CallableStatement s = derbycon.prepareCall(replace(sql), resultSetType, resultSetConcurrency, resultSetHoldability);
        s.setMaxFieldSize(DEFAULT_MAX_FIELD_SIZE);
        return s;
    }

    @Override
    public PreparedStatement prepareStatement(String sql) throws SQLException {
        PreparedStatement s = derbycon.prepareStatement(replace(sql));
        s.setMaxFieldSize(DEFAULT_MAX_FIELD_SIZE);
        return s;
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
        PreparedStatement s = derbycon.prepareStatement(replace(sql), autoGeneratedKeys);
        s.setMaxFieldSize(DEFAULT_MAX_FIELD_SIZE);
        return s;
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        PreparedStatement s = derbycon.prepareStatement(replace(sql), resultSetType, resultSetConcurrency, resultSetHoldability);
        s.setMaxFieldSize(DEFAULT_MAX_FIELD_SIZE);
        return s;
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        PreparedStatement s = derbycon.prepareStatement(replace(sql), resultSetType, resultSetConcurrency);
        s.setMaxFieldSize(DEFAULT_MAX_FIELD_SIZE);
        return s;
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
        PreparedStatement s = derbycon.prepareStatement(replace(sql), columnIndexes);
        s.setMaxFieldSize(DEFAULT_MAX_FIELD_SIZE);
        return s;
    }

    @Override
    public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
        PreparedStatement s = derbycon.prepareStatement(replace(sql), columnNames);
        s.setMaxFieldSize(DEFAULT_MAX_FIELD_SIZE);
        return s;
    }

    @Override
    public void releaseSavepoint(Savepoint savepoint) throws SQLException {
        derbycon.releaseSavepoint(savepoint);
    }

    /**
     * Enables the test case to intercept and replace SQL as a way of providing various
     * information back to the application for testing purposes.
     */
    private String replace(String sql) throws SQLException {
        if (sql.toUpperCase().startsWith("CALL TEST.FORCE_EXCEPTION(")) {
            String[] params = sql.substring("CALL TEST.FORCE_EXCEPTION(".length(), sql.length() - 1).split(",");
            String sqlState = params[0];
            int errorCode = params[1] == null ? 0 : Integer.parseInt(params[1]);
            try {
                throw AccessController.doPrivileged((PrivilegedExceptionAction<SQLException>) () -> {
                    Class<?> exceptionClass = params[2] == null ? SQLException.class : Class.forName(params[2]);
                    @SuppressWarnings("unchecked")
                    Constructor<SQLException> ctor = (Constructor<SQLException>) exceptionClass.getConstructor(String.class, String.class, int.class);
                    return ctor.newInstance("Test JDBC driver fails on purpose.", sqlState, errorCode);
                });
            } catch (PrivilegedActionException x) {
                throw new SQLException("Failed to create exception class", sqlState, errorCode, x);
            }
        }

        if ("CALL TEST.FORCE_EXCEPTION_ON_IS_VALID()".equalsIgnoreCase(sql))
            return "VALUES (" + (failOnIsValid = true) + ")";

        else if ("CALL TEST.GET_CLEANUP_COUNT()".equalsIgnoreCase(sql))
            return "VALUES (" + cleanupCount.get() + ")";

        else if ("CALL TEST.GET_SETUP_COUNT()".equalsIgnoreCase(sql))
            return "VALUES (" + setupCount.get() + ")";

        else if ("CALL TEST.GET_TRANSACTION_COUNT()".equalsIgnoreCase(sql))
            return "VALUES (" + transactionCount.get() + ")";

        return sql;
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
        Map<String, ClientInfoStatus> invalidEntries = clientInfoKeys.stream() //
                        .filter(key -> !properties.containsKey(key)) //
                        .collect(Collectors.toMap(key -> key, value -> ClientInfoStatus.REASON_UNKNOWN_PROPERTY));
        if (invalidEntries.size() > 0)
            throw new SQLClientInfoException("not supported", invalidEntries);

        derbycon.setClientInfo(properties);
    }

    @Override
    public void setClientInfo(String name, String value) throws SQLClientInfoException {
        if (!clientInfoKeys.contains(name))
            throw new SQLClientInfoException("not supported", Collections.singletonMap(name, ClientInfoStatus.REASON_UNKNOWN_PROPERTY));
        derbycon.setClientInfo(name, value);
    }

    @Override
    public void setClientInfoKeys(String... keys) {
        if (keys.length == 0)
            clientInfoKeys = DEFAULT_CLIENT_INFO_KEYS;
        else
            clientInfoKeys = Stream.of(keys).collect(Collectors.toSet());
    }

    public void setExceptionIdentificationOverrides(Map<Object, Class<?>> overrides) {
        exceptionIdentificationOverrides = overrides;
    }

    @Override
    public void setHoldability(int holdability) throws SQLException {
        derbycon.setHoldability(holdability);
    }

    @Override
    public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
        if (ds.supportsNetworkTimeout)
            derbycon.setNetworkTimeout(executor, milliseconds);
        else
            throw new SQLException("You disabled the ability to set the network timeout.");
    }

    @Override
    public void setReadOnly(boolean readOnly) throws SQLException {
        if (ds.supportsReadOnly)
            derbycon.setReadOnly(readOnly);
        else
            throw new SQLException("You disabled support for read only.");
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
        if (ds.supportsSchema)
            derbycon.setSchema(schema);
        else
            throw new SQLException("You disabled the ability to set the schema.");
    }

    @Override
    public void setTransactionIsolation(int level) throws SQLException {
        derbycon.setTransactionIsolation(level);
    }

    @Override
    public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
        if (ds.supportsTypeMap)
            derbycon.setTypeMap(map);
        else
            throw new SQLException("You disabled support for type map.");
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return derbycon.unwrap(iface);
    }
}