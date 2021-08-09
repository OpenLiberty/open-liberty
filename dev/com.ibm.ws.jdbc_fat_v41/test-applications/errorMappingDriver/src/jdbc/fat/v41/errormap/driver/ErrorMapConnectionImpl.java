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
package jdbc.fat.v41.errormap.driver;

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

public class ErrorMapConnectionImpl implements Connection, ErrorMapConnection {
    private final Connection impl;
    private final ErrorMapDataSourceImpl datasource;
    private final Set<Statement> stmtSet = new HashSet<Statement>();

    private Integer errorCode = null;
    private String sqlState = null;

    public ErrorMapConnectionImpl(ErrorMapDataSourceImpl ds, Connection conn) {
        datasource = ds;
        impl = conn;
    }

    @Override
    public boolean isWrapperFor(Class<?> arg0) throws SQLException {
        return ErrorMapConnection.class.equals(arg0) || impl.isWrapperFor(arg0);
    }

    @Override
    public <T> T unwrap(Class<T> arg0) throws SQLException {
        if (ErrorMapConnection.class.equals(ErrorMapConnection.class))
            return (T) this;
        return impl.unwrap(arg0);
    }

    @Override
    public void abort(Executor arg0) throws SQLException {
        blowupIfRequested();
        impl.abort(arg0);
    }

    @Override
    public void clearWarnings() throws SQLException {
        impl.clearWarnings();
    }

    @Override
    public void close() throws SQLException {
        impl.close();
    }

    @Override
    public void commit() throws SQLException {
        blowupIfRequested();
        impl.commit();
    }

    @Override
    public Array createArrayOf(String arg0, Object[] arg1) throws SQLException {
        return impl.createArrayOf(arg0, arg1);
    }

    @Override
    public Blob createBlob() throws SQLException {
        return impl.createBlob();
    }

    @Override
    public Clob createClob() throws SQLException {
        return impl.createClob();
    }

    @Override
    public NClob createNClob() throws SQLException {
        return impl.createNClob();
    }

    @Override
    public SQLXML createSQLXML() throws SQLException {
        return impl.createSQLXML();
    }

    @Override
    public Statement createStatement() throws SQLException {
        blowupIfRequested();
        Statement s = impl.createStatement();
        stmtSet.add(s);
        return s;
    }

    @Override
    public Statement createStatement(int arg0, int arg1) throws SQLException {
        blowupIfRequested();
        Statement s = impl.createStatement(arg0, arg1);
        stmtSet.add(s);
        return s;
    }

    @Override
    public Statement createStatement(int arg0, int arg1, int arg2) throws SQLException {
        blowupIfRequested();
        Statement s = impl.createStatement(arg0, arg1, arg2);
        stmtSet.add(s);
        return s;
    }

    @Override
    public Struct createStruct(String arg0, Object[] arg1) throws SQLException {
        return impl.createStruct(arg0, arg1);
    }

    @Override
    public boolean getAutoCommit() throws SQLException {
        blowupIfRequested();
        return impl.getAutoCommit();
    }

    @Override
    public String getCatalog() throws SQLException {
        blowupIfRequested();
        return impl.getCatalog();
    }

    @Override
    public Properties getClientInfo() throws SQLException {
        blowupIfRequested();
        return impl.getClientInfo();
    }

    @Override
    public String getClientInfo(String arg0) throws SQLException {
        blowupIfRequested();
        return impl.getClientInfo(arg0);
    }

    @Override
    public int getHoldability() throws SQLException {
        blowupIfRequested();
        return impl.getHoldability();
    }

    @Override
    public DatabaseMetaData getMetaData() throws SQLException {
        return impl.getMetaData();
    }

    @Override
    public int getNetworkTimeout() throws SQLException {
        blowupIfRequested();
        return impl.getNetworkTimeout();
    }

    @Override
    public String getSchema() throws SQLException {
        blowupIfRequested();
        return impl.getSchema();
    }

    @Override
    public int getTransactionIsolation() throws SQLException {
        blowupIfRequested();
        return impl.getTransactionIsolation();
    }

    @Override
    public Map<String, Class<?>> getTypeMap() throws SQLException {
        // Simulate a JDBC 3.0 driver which throws a SQLException with the SQLState indicating not supported
        throw new SQLException("Not Supported.", "0A000");
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        return impl.getWarnings();
    }

    @Override
    public boolean isClosed() throws SQLException {
        return impl.isClosed();
    }

    @Override
    public boolean isReadOnly() throws SQLException {
        return impl.isReadOnly();
    }

    @Override
    public boolean isValid(int arg0) throws SQLException {
        return impl.isValid(arg0);
    }

    @Override
    public String nativeSQL(String arg0) throws SQLException {
        blowupIfRequested();
        return impl.nativeSQL(arg0);
    }

    @Override
    public CallableStatement prepareCall(String arg0) throws SQLException {
        blowupIfRequested();
        CallableStatement s = impl.prepareCall(arg0);
        stmtSet.add(s);
        return s;
    }

    @Override
    public CallableStatement prepareCall(String arg0, int arg1, int arg2) throws SQLException {
        blowupIfRequested();
        CallableStatement s = impl.prepareCall(arg0, arg1, arg2);
        stmtSet.add(s);
        return s;
    }

    @Override
    public CallableStatement prepareCall(String arg0, int arg1, int arg2, int arg3) throws SQLException {
        blowupIfRequested();
        CallableStatement s = impl.prepareCall(arg0, arg1, arg2, arg3);
        stmtSet.add(s);
        return s;
    }

    @Override
    public PreparedStatement prepareStatement(String arg0) throws SQLException {
        blowupIfRequested();
        PreparedStatement s = impl.prepareStatement(arg0);
        stmtSet.add(s);
        return s;
    }

    @Override
    public PreparedStatement prepareStatement(String arg0, int arg1) throws SQLException {
        blowupIfRequested();
        PreparedStatement s = impl.prepareStatement(arg0, arg1);
        stmtSet.add(s);
        return s;
    }

    @Override
    public PreparedStatement prepareStatement(String arg0, int[] arg1) throws SQLException {
        blowupIfRequested();
        PreparedStatement s = impl.prepareStatement(arg0, arg1);
        stmtSet.add(s);
        return s;
    }

    @Override
    public PreparedStatement prepareStatement(String arg0, String[] arg1) throws SQLException {
        blowupIfRequested();
        PreparedStatement s = impl.prepareStatement(arg0, arg1);
        stmtSet.add(s);
        return s;
    }

    @Override
    public PreparedStatement prepareStatement(String arg0, int arg1, int arg2) throws SQLException {
        blowupIfRequested();
        PreparedStatement s = impl.prepareStatement(arg0, arg1, arg2);
        stmtSet.add(s);
        return s;
    }

    @Override
    public PreparedStatement prepareStatement(String arg0, int arg1, int arg2, int arg3) throws SQLException {
        blowupIfRequested();
        PreparedStatement s = impl.prepareStatement(arg0, arg1, arg2, arg3);
        stmtSet.add(s);
        return s;
    }

    @Override
    public void releaseSavepoint(Savepoint arg0) throws SQLException {
        impl.releaseSavepoint(arg0);
    }

    @Override
    public void rollback() throws SQLException {
        blowupIfRequested();
        impl.rollback();
    }

    @Override
    public void rollback(Savepoint arg0) throws SQLException {
        blowupIfRequested();
        impl.rollback(arg0);
    }

    @Override
    public void setAutoCommit(boolean arg0) throws SQLException {
        blowupIfRequested();
        impl.setAutoCommit(arg0);
    }

    @Override
    public void setCatalog(String arg0) throws SQLException {
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
        impl.setHoldability(arg0);
    }

    @Override
    public void setNetworkTimeout(Executor arg0, int arg1) throws SQLException {
        impl.setNetworkTimeout(arg0, arg1);
    }

    @Override
    public void setReadOnly(boolean arg0) throws SQLException {
        impl.setReadOnly(arg0);
    }

    @Override
    public Savepoint setSavepoint() throws SQLException {
        return impl.setSavepoint();
    }

    @Override
    public Savepoint setSavepoint(String arg0) throws SQLException {
        return impl.setSavepoint(arg0);
    }

    @Override
    public void setSchema(String arg0) throws SQLException {
        impl.setSchema(arg0);
    }

    @Override
    public void setTransactionIsolation(int arg0) throws SQLException {
        impl.setTransactionIsolation(arg0);
    }

    @Override
    public void setTypeMap(Map<String, Class<?>> arg0) throws SQLException {
        // Simulate a JDBC 3.0 driver which throws a SQLException with the SQLState indicating not supported
        throw new SQLException("Not Supported.", "0A000");
    }

    private void log(String msg) {
        System.out.println("[" + getClass().getSimpleName() + "]:     " + msg);
    }

    @Override
    public void setNextErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }

    @Override
    public void setNextSqlState(String sqlState) {
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
