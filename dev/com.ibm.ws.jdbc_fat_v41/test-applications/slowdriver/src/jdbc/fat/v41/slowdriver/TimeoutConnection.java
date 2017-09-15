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
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Executor;

public class TimeoutConnection implements Connection {
    private final Connection impl;
    private final TimeoutDataSourceImpl datasource;
    private int networkTimeout = 0;
    private final Set<Statement> stmtSet = new HashSet<Statement>();

    public TimeoutConnection(TimeoutDataSourceImpl ds, Connection conn) {
        datasource = ds;
        impl = conn;
    }

    @Override
    public boolean isWrapperFor(Class<?> arg0) throws SQLException {
        simulateLatency();
        return impl.isWrapperFor(arg0);
    }

    @Override
    public <T> T unwrap(Class<T> arg0) throws SQLException {
        simulateLatency();
        return impl.unwrap(arg0);
    }

    @Override
    public void abort(Executor arg0) throws SQLException {
        simulateLatency();
        impl.abort(arg0);
    }

    @Override
    public void clearWarnings() throws SQLException {
        simulateLatency();
        impl.clearWarnings();
    }

    @Override
    public void close() throws SQLException {
        impl.close();
    }

    @Override
    public void commit() throws SQLException {
        simulateLatency();
        impl.commit();
    }

    @Override
    public Array createArrayOf(String arg0, Object[] arg1) throws SQLException {
        simulateLatency();
        return impl.createArrayOf(arg0, arg1);
    }

    @Override
    public Blob createBlob() throws SQLException {
        simulateLatency();
        return impl.createBlob();
    }

    @Override
    public Clob createClob() throws SQLException {
        simulateLatency();
        return impl.createClob();
    }

    @Override
    public NClob createNClob() throws SQLException {
        simulateLatency();
        return impl.createNClob();
    }

    @Override
    public SQLXML createSQLXML() throws SQLException {
        simulateLatency();
        return impl.createSQLXML();
    }

    @Override
    public Statement createStatement() throws SQLException {
        simulateLatency();
        Statement s = impl.createStatement();
        stmtSet.add(s);
        return s;
    }

    @Override
    public Statement createStatement(int arg0, int arg1) throws SQLException {
        Statement s = impl.createStatement(arg0, arg1);
        stmtSet.add(s);
        return s;
    }

    @Override
    public Statement createStatement(int arg0, int arg1, int arg2) throws SQLException {
        simulateLatency();
        Statement s = impl.createStatement(arg0, arg1, arg2);
        stmtSet.add(s);
        return s;
    }

    @Override
    public Struct createStruct(String arg0, Object[] arg1) throws SQLException {
        simulateLatency();
        return impl.createStruct(arg0, arg1);
    }

    @Override
    public boolean getAutoCommit() throws SQLException {
        simulateLatency();
        return impl.getAutoCommit();
    }

    @Override
    public String getCatalog() throws SQLException {
        simulateLatency();
        return impl.getCatalog();
    }

    @Override
    public Properties getClientInfo() throws SQLException {
        simulateLatency();
        return impl.getClientInfo();
    }

    @Override
    public String getClientInfo(String arg0) throws SQLException {
        simulateLatency();
        return impl.getClientInfo(arg0);
    }

    @Override
    public int getHoldability() throws SQLException {
        simulateLatency();
        return impl.getHoldability();
    }

    @Override
    public DatabaseMetaData getMetaData() throws SQLException {
        simulateLatency();
        return impl.getMetaData();
    }

    @Override
    public int getNetworkTimeout() throws SQLException {
        simulateLatency();
        if (impl.isClosed())
            throw new SQLException("Conneciton closed.");
        // Derby embedded does not support network operations, so we need
        // to manually implement a network delay mechanism.
        return this.networkTimeout;
    }

    @Override
    public String getSchema() throws SQLException {
        simulateLatency();
        return impl.getSchema();
    }

    @Override
    public int getTransactionIsolation() throws SQLException {
        simulateLatency();
        return impl.getTransactionIsolation();
    }

    @Override
    public Map<String, Class<?>> getTypeMap() throws SQLException {
        simulateLatency();
        // Simulate a JDBC 3.0 driver which throws a SQLException with the SQLState indicating not supported
        throw new SQLException("Not Supported.", "0A000");
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        simulateLatency();
        return impl.getWarnings();
    }

    @Override
    public boolean isClosed() throws SQLException {
        return impl.isClosed();
    }

    @Override
    public boolean isReadOnly() throws SQLException {
        simulateLatency();
        return impl.isReadOnly();
    }

    @Override
    public boolean isValid(int arg0) throws SQLException {
        return impl.isValid(arg0);
    }

    @Override
    public String nativeSQL(String arg0) throws SQLException {
        simulateLatency();
        return impl.nativeSQL(arg0);
    }

    @Override
    public CallableStatement prepareCall(String arg0) throws SQLException {
        simulateLatency();
        CallableStatement s = impl.prepareCall(arg0);
        stmtSet.add(s);
        return s;
    }

    @Override
    public CallableStatement prepareCall(String arg0, int arg1, int arg2) throws SQLException {
        simulateLatency();
        CallableStatement s = impl.prepareCall(arg0, arg1, arg2);
        stmtSet.add(s);
        return s;
    }

    @Override
    public CallableStatement prepareCall(String arg0, int arg1, int arg2, int arg3) throws SQLException {
        simulateLatency();
        CallableStatement s = impl.prepareCall(arg0, arg1, arg2, arg3);
        stmtSet.add(s);
        return s;
    }

    @Override
    public PreparedStatement prepareStatement(String arg0) throws SQLException {
        simulateLatency();
        PreparedStatement s = impl.prepareStatement(arg0);
        stmtSet.add(s);
        return s;
    }

    @Override
    public PreparedStatement prepareStatement(String arg0, int arg1) throws SQLException {
        simulateLatency();
        PreparedStatement s = impl.prepareStatement(arg0, arg1);
        stmtSet.add(s);
        return s;
    }

    @Override
    public PreparedStatement prepareStatement(String arg0, int[] arg1) throws SQLException {
        simulateLatency();
        PreparedStatement s = impl.prepareStatement(arg0, arg1);
        stmtSet.add(s);
        return s;
    }

    @Override
    public PreparedStatement prepareStatement(String arg0, String[] arg1) throws SQLException {
        simulateLatency();
        PreparedStatement s = impl.prepareStatement(arg0, arg1);
        stmtSet.add(s);
        return s;
    }

    @Override
    public PreparedStatement prepareStatement(String arg0, int arg1, int arg2) throws SQLException {
        simulateLatency();
        PreparedStatement s = impl.prepareStatement(arg0, arg1, arg2);
        stmtSet.add(s);
        return s;
    }

    @Override
    public PreparedStatement prepareStatement(String arg0, int arg1, int arg2, int arg3) throws SQLException {
        simulateLatency();
        PreparedStatement s = impl.prepareStatement(arg0, arg1, arg2, arg3);
        stmtSet.add(s);
        return s;
    }

    @Override
    public void releaseSavepoint(Savepoint arg0) throws SQLException {
        simulateLatency();
        impl.releaseSavepoint(arg0);
    }

    @Override
    public void rollback() throws SQLException {
        simulateLatency();
        impl.rollback();
    }

    @Override
    public void rollback(Savepoint arg0) throws SQLException {
        simulateLatency();
        impl.rollback(arg0);
    }

    @Override
    public void setAutoCommit(boolean arg0) throws SQLException {
        simulateLatency();
        impl.setAutoCommit(arg0);
    }

    @Override
    public void setCatalog(String arg0) throws SQLException {
        simulateLatency();
        impl.setCatalog(arg0);
    }

    @Override
    public void setClientInfo(Properties arg0) throws SQLClientInfoException {
        impl.setClientInfo(arg0);
    }

    @Override
    public void setClientInfo(String arg0, String arg1) throws SQLClientInfoException {
        impl.setClientInfo(arg0, arg1);
    }

    @Override
    public void setHoldability(int arg0) throws SQLException {
        simulateLatency();
        impl.setHoldability(arg0);
    }

    @Override
    public void setNetworkTimeout(Executor arg0, int arg1) throws SQLException {
        simulateLatency();

        if (impl.isClosed())
            throw new SQLException("Connection closed.");
        if (arg0 == null)
            throw new SQLException("Null Executor.");
        if (arg1 < 0)
            throw new SQLException("Negative timeout.");

        // Derby embedded does not support network operations, so we need
        // to manually implement a network delay mechanism.
        networkTimeout = arg1;
        log("Setting network timeout to: " + arg1);
    }

    @Override
    public void setReadOnly(boolean arg0) throws SQLException {
        simulateLatency();
        impl.setReadOnly(arg0);
    }

    @Override
    public Savepoint setSavepoint() throws SQLException {
        simulateLatency();
        return impl.setSavepoint();
    }

    @Override
    public Savepoint setSavepoint(String arg0) throws SQLException {
        simulateLatency();
        return impl.setSavepoint(arg0);
    }

    @Override
    public void setSchema(String arg0) throws SQLException {
        simulateLatency();
        impl.setSchema(arg0);
    }

    @Override
    public void setTransactionIsolation(int arg0) throws SQLException {
        simulateLatency();
        impl.setTransactionIsolation(arg0);
    }

    @Override
    public void setTypeMap(Map<String, Class<?>> arg0) throws SQLException {
        simulateLatency();
        // Simulate a JDBC 3.0 driver which throws a SQLException with the SQLState indicating not supported
        throw new SQLException("Not Supported.", "0A000");
    }

    private void simulateLatency() throws SQLException {
        if (networkTimeout != 0 && datasource.getLatency() >= networkTimeout) {
            log("Latency of " + datasource.getLatency() + " is greater than the allowed timeout of " + networkTimeout);
            log("Closing resources then throwing an exception...");
            for (Statement s : stmtSet)
                s.close();
            impl.close();
            throw new SQLException("Network timeout detected.  Latency:" + datasource.getLatency() + "   Timeout:" + networkTimeout);
        }
    }

    private void log(String msg) {
        System.out.println("[TimeoutConnection]:     " + msg);
    }
}
