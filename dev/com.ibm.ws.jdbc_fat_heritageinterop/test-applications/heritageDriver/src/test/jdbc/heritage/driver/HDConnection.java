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
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.sql.ConnectionEventListener;
import javax.sql.StatementEventListener;
import javax.sql.XAConnection;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

public class HDConnection implements Connection, HeritageDBConnection, XAConnection, XAResource {
    private static final Set<String> DEFAULT_CLIENT_INFO_KEYS = Stream.of("ApplicationName", "ClientHostname", "ClientUser").collect(Collectors.toSet());
    public static final int DEFAULT_MAX_FIELD_SIZE = 225;

    /**
     * Simulates tightly coupled transaction branches by redirecting all transaction branches
     * for the same global transaction ID to use the same connection for the duration of the transaction.
     */
    private static Map<List<Byte>, HDConnection> TIGHTLY_COUPLED_TRANSACTIONS = new HashMap<List<Byte>, HDConnection>();

    /**
     * Counts the number of times that doConnectionCleanupPerCloseConnection is invoked for this connection.
     */
    public final AtomicInteger cleanupCount = new AtomicInteger();

    final HDDataSource ds;

    /**
     * Connection to Derby. Access this via the connection() helper method to allow for
     * simulation of tightly coupled transaction branches.
     */
    private final Connection derbycon;

    /**
     * Connection to Derby that simulates tightly coupled transaction branches.
     */
    private Connection derbyconForTightlyCoupledBranch;

    Set<String> clientInfoKeys = DEFAULT_CLIENT_INFO_KEYS;

    private Map<Object, Class<?>> exceptionIdentificationOverrides;
    private boolean failOnIsValid;

    private boolean restoreAutoCommitAfterTx;

    /**
     * Counts the number of times that doConnectionSetupPerGetConnection is invoked for this connection.
     */
    public final AtomicInteger setupCount = new AtomicInteger();

    /**
     * Counts the number of times that doConnectionSetupPerTransaction is invoked for this connection.
     */
    public final AtomicInteger transactionCount = new AtomicInteger(Integer.MIN_VALUE); // make it obvious if it never gets initialized to 1

    /**
     * Xid of transaction that we are pretending to participate in.
     */
    public Xid xid;

    HDConnection(HDDataSource ds, Connection con) {
        this.ds = ds;
        this.derbycon = con;
    }

    @Override
    public void abort(Executor executor) throws SQLException {
        connection().abort(executor);
    }

    @Override
    public void addConnectionEventListener(ConnectionEventListener listener) {
    }

    @Override
    public void addStatementEventListener(StatementEventListener listener) {
    }

    /**
     * Utility method to convert a global transaction ID into a list of Byte
     * in order to make it comparable.
     *
     * @param bytes global transaction ID as a byte array
     * @return global transaction ID as a list of Byte
     */
    private List<Byte> byteList(byte[] bytes) {
        List<Byte> list = new ArrayList<Byte>(bytes.length);
        for (byte b : bytes)
            list.add(b);
        return list;
    }

    @Override
    public void close() throws SQLException {
        connection().close();
    }

    @Override
    public void clearWarnings() throws SQLException {
        connection().clearWarnings();
    }

    @Override
    public void commit() throws SQLException {
        if (xid == null)
            connection().commit();
        else
            throw new SQLFeatureNotSupportedException("Connection.commit during XA transaction");
    }

    @Override
    public void commit(Xid xid, boolean onePhase) throws XAException {
        if (this.xid != xid)
            throw new XAException(XAException.XAER_PROTO);
        try {
            if (derbyconForTightlyCoupledBranch == null) {
                derbycon.commit();
            } else {
                derbyconForTightlyCoupledBranch = null;
                List<Byte> globalTxId = byteList(xid.getGlobalTransactionId());
                HDConnection c = TIGHTLY_COUPLED_TRANSACTIONS.get(globalTxId);
                if (this == c) {
                    TIGHTLY_COUPLED_TRANSACTIONS.remove(globalTxId);
                    c.derbycon.commit();
                }
                // else let the first branch that enlisted perform the commit
            }
            if (restoreAutoCommitAfterTx) {
                derbycon.setAutoCommit(true);
                restoreAutoCommitAfterTx = false;
            }
        } catch (SQLException x) {
            throw (XAException) new XAException(XAException.XAER_PROTO).initCause(x);
        } finally {
            this.xid = null;
        }
    }

    /**
     * Allows for swapping out the connection to simulate tightly coupled branches.
     *
     * @return the connection to use
     */
    private final Connection connection() {
        return derbyconForTightlyCoupledBranch == null ? derbycon : derbyconForTightlyCoupledBranch;
    }

    @Override
    public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
        return connection().createArrayOf(typeName, elements);
    }

    @Override
    public Blob createBlob() throws SQLException {
        return connection().createBlob();
    }

    @Override
    public Clob createClob() throws SQLException {
        return connection().createClob();
    }

    @Override
    public NClob createNClob() throws SQLException {
        return connection().createNClob();
    }

    @Override
    public SQLXML createSQLXML() throws SQLException {
        return connection().createSQLXML();
    }

    @Override
    public Statement createStatement() throws SQLException {
        Statement s = connection().createStatement();
        s.setMaxFieldSize(DEFAULT_MAX_FIELD_SIZE);
        return s;
    }

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
        Statement s = connection().createStatement(resultSetType, resultSetConcurrency);
        s.setMaxFieldSize(DEFAULT_MAX_FIELD_SIZE);
        return s;
    }

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        Statement s = connection().createStatement(resultSetType, resultSetConcurrency, resultSetHoldability);
        s.setMaxFieldSize(DEFAULT_MAX_FIELD_SIZE);
        return s;
    }

    @Override
    public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
        return connection().createStruct(typeName, attributes);
    }

    @Override
    public boolean getAutoCommit() throws SQLException {
        return connection().getAutoCommit();
    }

    @Override
    public void end(Xid xid, int flags) throws XAException {
        if (this.xid != xid || !(flags == XAResource.TMSUCCESS || flags == XAResource.TMFAIL))
            throw new XAException(XAException.XAER_PROTO);
    }

    @Override
    public void forget(Xid xid) throws XAException {
        if (this.xid != xid)
            throw new XAException(XAException.XAER_PROTO);
    }

    @Override
    public String getCatalog() throws SQLException {
        if (ds.supportsCatalog)
            return connection().getCatalog();
        else
            throw new SQLException("You disabled support for catalog.");
    }

    @Override
    public Properties getClientInfo() throws SQLException {
        return connection().getClientInfo();
    }

    @Override
    public String getClientInfo(String name) throws SQLException {
        return connection().getClientInfo(name);
    }

    @Override
    public Set<String> getClientInfoKeys() {
        return clientInfoKeys;
    }

    @Override
    public Connection getConnection() throws SQLException {
        return this;
    }

    @Override
    public int getHoldability() throws SQLException {
        return connection().getHoldability();
    }

    @Override
    public DatabaseMetaData getMetaData() throws SQLException {
        return connection().getMetaData();
    }

    @Override
    public int getNetworkTimeout() throws SQLException {
        if (ds.supportsNetworkTimeout)
            return connection().getNetworkTimeout();
        else
            throw new SQLException("You disabled the ability to get the network timeout.", "HDISA", 0);
    }

    @Override
    public String getSchema() throws SQLException {
        if (ds.supportsSchema)
            return connection().getSchema();
        else
            throw new SQLException("You disabled the ability to get the schema.", "HY000", 10);
    }

    @Override
    public int getTransactionIsolation() throws SQLException {
        return connection().getTransactionIsolation();
    }

    @Override
    public int getTransactionTimeout() throws XAException {
        return 0;
    }

    @Override
    public Map<String, Class<?>> getTypeMap() throws SQLException {
        if (ds.supportsTypeMap)
            return connection().getTypeMap();
        else
            throw new SQLException("You disabled support for type map.", null, -40960);
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        return connection().getWarnings();
    }

    @Override
    public XAResource getXAResource() throws SQLException {
        return this;
    }

    @Override
    public boolean isClosed() throws SQLException {
        return connection().isClosed();
    }

    @Override
    public boolean isReadOnly() throws SQLException {
        if (ds.supportsReadOnly)
            return connection().isReadOnly();
        else
            throw new SQLException("You disabled support for read only.");
    }

    @Override
    public boolean isSameRM(XAResource xaResource) throws XAException {
        return xaResource instanceof HDConnection;
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return connection().isWrapperFor(iface);
    }

    @Override
    public boolean isValid(int timeout) throws SQLException {
        if (failOnIsValid)
            throw new SQLException("Test case asked for isValid to fail, and so it is.", null, 44098);

        return connection().isValid(timeout);
    }

    @Override
    public String nativeSQL(String sql) throws SQLException {
        return connection().nativeSQL(sql);
    }

    @Override
    public int prepare(Xid xid) throws XAException {
        if (this.xid != xid)
            throw new XAException(XAException.XAER_PROTO);
        return XAResource.XA_OK;
    }

    @Override
    public CallableStatement prepareCall(String sql) throws SQLException {
        CallableStatement s = connection().prepareCall(replace(sql));
        s.setMaxFieldSize(DEFAULT_MAX_FIELD_SIZE);
        return s;
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        CallableStatement s = connection().prepareCall(replace(sql), resultSetType, resultSetConcurrency);
        s.setMaxFieldSize(DEFAULT_MAX_FIELD_SIZE);
        return s;
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        CallableStatement s = connection().prepareCall(replace(sql), resultSetType, resultSetConcurrency, resultSetHoldability);
        s.setMaxFieldSize(DEFAULT_MAX_FIELD_SIZE);
        return s;
    }

    @Override
    public PreparedStatement prepareStatement(String sql) throws SQLException {
        PreparedStatement s = connection().prepareStatement(replace(sql));
        s.setMaxFieldSize(DEFAULT_MAX_FIELD_SIZE);
        return s;
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
        PreparedStatement s = connection().prepareStatement(replace(sql), autoGeneratedKeys);
        s.setMaxFieldSize(DEFAULT_MAX_FIELD_SIZE);
        return s;
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        PreparedStatement s = connection().prepareStatement(replace(sql), resultSetType, resultSetConcurrency, resultSetHoldability);
        s.setMaxFieldSize(DEFAULT_MAX_FIELD_SIZE);
        return s;
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        PreparedStatement s = connection().prepareStatement(replace(sql), resultSetType, resultSetConcurrency);
        s.setMaxFieldSize(DEFAULT_MAX_FIELD_SIZE);
        return s;
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
        PreparedStatement s = connection().prepareStatement(replace(sql), columnIndexes);
        s.setMaxFieldSize(DEFAULT_MAX_FIELD_SIZE);
        return s;
    }

    @Override
    public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
        PreparedStatement s = connection().prepareStatement(replace(sql), columnNames);
        s.setMaxFieldSize(DEFAULT_MAX_FIELD_SIZE);
        return s;
    }

    @Override
    public Xid[] recover(int flags) throws XAException {
        return new Xid[] {};
    }

    @Override
    public void releaseSavepoint(Savepoint savepoint) throws SQLException {
        connection().releaseSavepoint(savepoint);
    }

    @Override
    public void removeConnectionEventListener(ConnectionEventListener listener) {
    }

    @Override
    public void removeStatementEventListener(StatementEventListener listener) {
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
        if (xid == null)
            connection().rollback();
        else
            throw new SQLFeatureNotSupportedException("Connection.rollback during XA transaction");
    }

    @Override
    public void rollback(Savepoint savepoint) throws SQLException {
        if (xid == null)
            connection().rollback(savepoint);
        else
            throw new SQLFeatureNotSupportedException("Connection.rollback during XA transaction");
    }

    @Override
    public void rollback(Xid xid) throws XAException {
        if (this.xid != xid)
            throw new XAException(XAException.XAER_PROTO);
        try {
            if (derbyconForTightlyCoupledBranch == null) {
                derbycon.rollback();
            } else {
                derbyconForTightlyCoupledBranch = null;
                List<Byte> globalTxId = byteList(xid.getGlobalTransactionId());
                HDConnection c = TIGHTLY_COUPLED_TRANSACTIONS.get(globalTxId);
                if (this == c) {
                    TIGHTLY_COUPLED_TRANSACTIONS.remove(globalTxId);
                    c.derbycon.rollback();
                }
                // else let the first branch that enlisted perform the rollback
            }
            if (restoreAutoCommitAfterTx) {
                derbycon.setAutoCommit(true);
                restoreAutoCommitAfterTx = false;
            }
        } catch (SQLException x) {
            throw (XAException) new XAException(XAException.XAER_PROTO).initCause(x);
        } finally {
            this.xid = null;
        }
    }

    @Override
    public void setAutoCommit(boolean autoCommit) throws SQLException {
        if (xid == null || autoCommit == false)
            connection().setAutoCommit(autoCommit);
        else
            throw new SQLFeatureNotSupportedException("Autocommit during XA transaction");
        restoreAutoCommitAfterTx = false;
    }

    @Override
    public void setCatalog(String catalog) throws SQLException {
        if (ds.supportsCatalog)
            connection().setCatalog(catalog);
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

        connection().setClientInfo(properties);
    }

    @Override
    public void setClientInfo(String name, String value) throws SQLClientInfoException {
        if (!clientInfoKeys.contains(name))
            throw new SQLClientInfoException("not supported", Collections.singletonMap(name, ClientInfoStatus.REASON_UNKNOWN_PROPERTY));
        connection().setClientInfo(name, value);
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
        connection().setHoldability(holdability);
    }

    @Override
    public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
        if (ds.supportsNetworkTimeout)
            connection().setNetworkTimeout(executor, milliseconds);
        else
            throw new SQLException("You disabled the ability to set the network timeout.");
    }

    @Override
    public void setReadOnly(boolean readOnly) throws SQLException {
        if (ds.supportsReadOnly)
            connection().setReadOnly(readOnly);
        else
            throw new SQLException("You disabled support for read only.");
    }

    @Override
    public Savepoint setSavepoint() throws SQLException {
        return connection().setSavepoint();
    }

    @Override
    public Savepoint setSavepoint(String name) throws SQLException {
        return connection().setSavepoint(name);
    }

    @Override
    public void setSchema(String schema) throws SQLException {
        if (ds.supportsSchema)
            connection().setSchema(schema);
        else
            throw new SQLException("You disabled the ability to set the schema.");
    }

    @Override
    public void setTransactionIsolation(int level) throws SQLException {
        connection().setTransactionIsolation(level);
    }

    @Override
    public boolean setTransactionTimeout(int timeout) throws XAException {
        return false;
    }

    @Override
    public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
        if (ds.supportsTypeMap)
            connection().setTypeMap(map);
        else
            throw new SQLException("You disabled support for type map.");
    }

    @Override
    public void start(Xid xid, int flags) throws XAException {
        try {
            if (getAutoCommit()) {
                setAutoCommit(false);
                restoreAutoCommitAfterTx = true;
            }

            if ((flags & LOOSELY_COUPLED_TRANSACTION_BRANCHES) == 0) {
                // Simulate tight branch coupling by redirecting all branches to a single one-phase connection
                List<Byte> globalTxId = byteList(xid.getGlobalTransactionId());
                HDConnection c = TIGHTLY_COUPLED_TRANSACTIONS.get(globalTxId);
                if (c == null)
                    TIGHTLY_COUPLED_TRANSACTIONS.put(globalTxId, c = this);
                derbyconForTightlyCoupledBranch = c.derbycon;
            }

            this.xid = xid;
        } catch (IllegalStateException | SQLException x) {
            throw (XAException) new XAException(XAException.XAER_PROTO).initCause(x);
        }
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return connection().unwrap(iface);
    }
}