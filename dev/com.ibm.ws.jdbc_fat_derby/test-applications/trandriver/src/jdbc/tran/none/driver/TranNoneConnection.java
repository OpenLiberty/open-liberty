/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package jdbc.tran.none.driver;

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

public class TranNoneConnection implements Connection {
    private final Connection impl;
    private int isolationLevel = Connection.TRANSACTION_NONE;

    public TranNoneConnection(Connection conn) {
        impl = conn;
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return impl.isWrapperFor(iface);
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return impl.unwrap(iface);
    }

    @Override
    public void abort(Executor arg0) throws SQLException {
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
        return impl.createStatement();
    }

    @Override
    public Statement createStatement(int arg0, int arg1) throws SQLException {
        return impl.createStatement(arg0, arg1);
    }

    @Override
    public Statement createStatement(int arg0, int arg1, int arg2) throws SQLException {
        return impl.createStatement(arg0, arg1, arg2);
    }

    @Override
    public Struct createStruct(String arg0, Object[] arg1) throws SQLException {
        return impl.createStruct(arg0, arg1);
    }

    @Override
    public boolean getAutoCommit() throws SQLException {
        return impl.getAutoCommit();
    }

    @Override
    public String getCatalog() throws SQLException {
        return impl.getCatalog();
    }

    @Override
    public Properties getClientInfo() throws SQLException {
        return impl.getClientInfo();
    }

    @Override
    public String getClientInfo(String arg0) throws SQLException {
        return impl.getClientInfo(arg0);
    }

    @Override
    public int getHoldability() throws SQLException {
        return impl.getHoldability();
    }

    @Override
    public DatabaseMetaData getMetaData() throws SQLException {
        return impl.getMetaData();
    }

    @Override
    public int getNetworkTimeout() throws SQLException {
        return impl.getNetworkTimeout();
    }

    @Override
    public String getSchema() throws SQLException {
        return impl.getSchema();
    }

    @Override
    public int getTransactionIsolation() throws SQLException {
        return isolationLevel;
    }

    @Override
    public Map<String, Class<?>> getTypeMap() throws SQLException {
        return impl.getTypeMap();
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
        return impl.nativeSQL(arg0);
    }

    @Override
    public CallableStatement prepareCall(String arg0) throws SQLException {
        return impl.prepareCall(arg0);
    }

    @Override
    public CallableStatement prepareCall(String arg0, int arg1, int arg2) throws SQLException {
        return impl.prepareCall(arg0, arg1, arg2);
    }

    @Override
    public CallableStatement prepareCall(String arg0, int arg1, int arg2, int arg3) throws SQLException {
        return impl.prepareCall(arg0, arg1, arg2, arg3);
    }

    @Override
    public PreparedStatement prepareStatement(String arg0) throws SQLException {
        return impl.prepareStatement(arg0);
    }

    @Override
    public PreparedStatement prepareStatement(String arg0, int arg1) throws SQLException {
        return impl.prepareStatement(arg0, arg1);
    }

    @Override
    public PreparedStatement prepareStatement(String arg0, int[] arg1) throws SQLException {
        return impl.prepareStatement(arg0, arg1);
    }

    @Override
    public PreparedStatement prepareStatement(String arg0, String[] arg1) throws SQLException {
        return impl.prepareStatement(arg0, arg1);
    }

    @Override
    public PreparedStatement prepareStatement(String arg0, int arg1, int arg2) throws SQLException {
        return impl.prepareStatement(arg0, arg1, arg2);
    }

    @Override
    public PreparedStatement prepareStatement(String arg0, int arg1, int arg2, int arg3) throws SQLException {
        return impl.prepareStatement(arg0, arg1, arg2, arg3);
    }

    @Override
    public void releaseSavepoint(Savepoint arg0) throws SQLException {
        impl.releaseSavepoint(arg0);
    }

    @Override
    public void rollback() throws SQLException {
        impl.rollback();
    }

    @Override
    public void rollback(Savepoint arg0) throws SQLException {
        impl.rollback(arg0);
    }

    @Override
    public void setAutoCommit(boolean arg0) throws SQLException {
        throw new SQLException("setAutoCommit called with " + arg0);
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
        switch (arg0) {
            case 0: //TRANSACTION_NONE
                throw new SQLException("setTransactionIsolation called with " + arg0);
            case 4: //TRANSACTION_REPEATABLE_READ WAS default
                throw new SQLException("setTransactionIsolation called with " + arg0);
            default:
                impl.setTransactionIsolation(arg0);
                isolationLevel = arg0;
                break;
        }
    }

    @Override
    public void setTypeMap(Map<String, Class<?>> arg0) throws SQLException {
        impl.setTypeMap(arg0);
    }

}
