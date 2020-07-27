/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.informix.jdbcx;

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
 *
 */
public class IfxConnection implements Connection {
	Connection wrappedConn = null;
	private static int failoverValue = 0;
	private static int failoverCounter = 0;
	private static boolean failoverEnabled = false;

	private static int queryFailoverValue = 0;

	private static int queryFailoverCounter = 0;
	private static boolean queryFailoverEnabled = false;

	private static int simSQLCode = 0;

	IfxConnection(Connection realConn) {
		System.out.println("SIMHADB: construct wrapped connection using - " + realConn);
		wrappedConn = realConn;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.sql.Wrapper#unwrap(java.lang.Class)
	 */
	@Override
	public <T> T unwrap(Class<T> iface) throws SQLException {
		T theT = wrappedConn.unwrap(iface);
		return theT;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.sql.Wrapper#isWrapperFor(java.lang.Class)
	 */
	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		boolean ret = wrappedConn.isWrapperFor(iface);
		return ret;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.sql.Connection#clearWarnings()
	 */
	@Override
	public void clearWarnings() throws SQLException {
		wrappedConn.clearWarnings();

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.sql.Connection#close()
	 */
	@Override
	public void close() throws SQLException {
		wrappedConn.close();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.sql.Connection#commit()
	 */
	@Override
	public void commit() throws SQLException {
		System.out.println("SIMHADB: COMMIT");
		wrappedConn.commit();

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.sql.Connection#createStatement()
	 */
	@Override
	public Statement createStatement() throws SQLException {
		System.out.println("SIMHADB: createStatement, no param, failoverCounter: "); // +
																						// IfxStatement.failoverCounter);

		// if(IfxStatement.failoverCounter >= 1 && IfxStatement.failoverCounter
		// <= 5)
		// {
		//
		// System.out.println("SIMHADB: Feigning query failover - counter: " +
		// IfxStatement.failoverCounter);
		// IfxStatement.failoverCounter++;
		// //Connection myconn = getConnection();
		//
		// String sqlReason = "Generated internally";
		// String sqlState = "Generated reason";
		// int reasonCode = -4498; // FOR DB2 should be -4498, Oracle 17410
		// SQLException sqlex = new SQLException(sqlReason, sqlState,
		// reasonCode);
		// throw sqlex;
		// }
		Statement theS = wrappedConn.createStatement();
		IfxStatement uts = new IfxStatement(theS, this);
		System.out.println("SIMHADB: createStatement ret - " + uts);
		return uts;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.sql.Connection#createStatement(int, int)
	 */
	@Override
	public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
		System.out.println("SIMHADB: createStatement, rst: " + resultSetType + ", rsc:" + resultSetConcurrency); // +
																													// ",
																													// failoverCounter:
																													// "
																													// +
																													// IfxStatement.failoverCounter);
		// if (IfxStatement.failoverCounter >= 1 && IfxStatement.failoverCounter
		// <= 5)
		// {
		//
		// System.out.println("SIMHADB: Feigning query failover - counter: " +
		// IfxStatement.failoverCounter);
		// IfxStatement.failoverCounter++;
		// //Connection myconn = getConnection();
		//
		// String sqlReason = "Generated internally";
		// String sqlState = "Generated reason";
		// int reasonCode = -4498; // FOR DB2 should be -4498, Oracle 17410
		// SQLException sqlex = new SQLException(sqlReason, sqlState,
		// reasonCode);
		// throw sqlex;
		// }
		Statement theS = wrappedConn.createStatement(resultSetType, resultSetConcurrency);
		IfxStatement uts = new IfxStatement(theS, this);
		System.out.println("SIMHADB: createStatement ret - " + uts);
		return uts;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.sql.Connection#createStatement(int, int, int)
	 */
	@Override
	public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability)
			throws SQLException {
		System.out.println("SIMHADB: createStatement, rst: " + resultSetType + ", rsc:" + resultSetConcurrency
				+ ", rsh:" + resultSetHoldability);
		Statement theS = wrappedConn.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability);
		IfxStatement uts = new IfxStatement(theS, this);
		System.out.println("SIMHADB: createStatement ret - " + uts);
		return uts;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.sql.Connection#getAutoCommit()
	 */
	@Override
	public boolean getAutoCommit() throws SQLException {
		boolean ret = wrappedConn.getAutoCommit();
		return ret;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.sql.Connection#getCatalog()
	 */
	@Override
	public String getCatalog() throws SQLException {
		String ret = wrappedConn.getCatalog();
		return ret;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.sql.Connection#getHoldability()
	 */
	@Override
	public int getHoldability() throws SQLException {
		int ret = wrappedConn.getHoldability();
		return ret;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.sql.Connection#getMetaData()
	 */
	@Override
	public DatabaseMetaData getMetaData() throws SQLException {
		System.out.println("SIMHADB: getMetaData");
		DatabaseMetaData ret = new IfxDatabaseMetaData(); // wrappedConn.getMetaData();
		System.out.println("SIMHADB: getMetaData ret - " + ret);
		return ret;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.sql.Connection#getTransactionIsolation()
	 */
	@Override
	public int getTransactionIsolation() throws SQLException {
		int ret = wrappedConn.getTransactionIsolation();
		return ret;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.sql.Connection#getTypeMap()
	 */
	@Override
	public Map<String, Class<?>> getTypeMap() throws SQLException {
		Map ret = wrappedConn.getTypeMap();
		return ret;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.sql.Connection#getWarnings()
	 */
	@Override
	public SQLWarning getWarnings() throws SQLException {
		SQLWarning ret = wrappedConn.getWarnings();
		return ret;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.sql.Connection#isClosed()
	 */
	@Override
	public boolean isClosed() throws SQLException {
		boolean ret = wrappedConn.isClosed();
		return ret;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.sql.Connection#isReadOnly()
	 */
	@Override
	public boolean isReadOnly() throws SQLException {
		boolean ret = wrappedConn.isReadOnly();
		return ret;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.sql.Connection#nativeSQL(java.lang.String)
	 */
	@Override
	public String nativeSQL(String sql) throws SQLException {
		String ret = wrappedConn.nativeSQL(sql);
		return ret;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.sql.Connection#prepareCall(java.lang.String)
	 */
	@Override
	public CallableStatement prepareCall(String sql) throws SQLException {
		CallableStatement ret = wrappedConn.prepareCall(sql);
		return ret;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.sql.Connection#prepareCall(java.lang.String, int, int)
	 */
	@Override
	public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
		CallableStatement ret = wrappedConn.prepareCall(sql, resultSetType, resultSetConcurrency);
		return ret;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.sql.Connection#prepareCall(java.lang.String, int, int, int)
	 */
	@Override
	public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency,
			int resultSetHoldability) throws SQLException {
		CallableStatement ret = wrappedConn.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
		return ret;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.sql.Connection#prepareStatement(java.lang.String)
	 */
	@Override
	public PreparedStatement prepareStatement(String sql) throws SQLException {
		System.out.println("SIMHADB: prepareStatement - " + sql);
		PreparedStatement ret = wrappedConn.prepareStatement(sql);
		IfxPreparedStatement utps = new IfxPreparedStatement(ret);
		System.out.println("SIMHADB: prepareStatement - " + utps);
		return utps;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.sql.Connection#prepareStatement(java.lang.String, int)
	 */
	@Override
	public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
		System.out.println("SIMHADB: prepareStatement - " + sql);
		PreparedStatement ret = wrappedConn.prepareStatement(sql, autoGeneratedKeys);
		IfxPreparedStatement utps = new IfxPreparedStatement(ret);
		System.out.println("SIMHADB: prepareStatement - " + utps);
		return utps;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.sql.Connection#prepareStatement(java.lang.String, int[])
	 */
	@Override
	public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
		System.out.println("SIMHADB: prepareStatement - " + sql);
		PreparedStatement ret = wrappedConn.prepareStatement(sql, columnIndexes);
		IfxPreparedStatement utps = new IfxPreparedStatement(ret);
		System.out.println("SIMHADB: prepareStatement - " + utps);
		return utps;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.sql.Connection#prepareStatement(java.lang.String, int, int)
	 */
	@Override
	public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency)
			throws SQLException {
		System.out.println("SIMHADB: prepareStatement - " + sql);
		PreparedStatement ret = wrappedConn.prepareStatement(sql, resultSetType, resultSetConcurrency);
		IfxPreparedStatement utps = new IfxPreparedStatement(ret);
		System.out.println("SIMHADB: prepareStatement - " + utps);
		return utps;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.sql.Connection#prepareStatement(java.lang.String, int, int,
	 * int)
	 */
	@Override
	public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency,
			int resultSetHoldability) throws SQLException {
		System.out.println("SIMHADB: prepareStatement - " + sql);
		PreparedStatement ret = wrappedConn.prepareStatement(sql, resultSetType, resultSetConcurrency,
				resultSetHoldability);
		IfxPreparedStatement utps = new IfxPreparedStatement(ret);
		System.out.println("SIMHADB: prepareStatement - " + utps);
		return utps;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.sql.Connection#prepareStatement(java.lang.String,
	 * java.lang.String[])
	 */
	@Override
	public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
		System.out.println("SIMHADB: prepareStatement - " + sql);
		PreparedStatement ret = wrappedConn.prepareStatement(sql, columnNames);
		IfxPreparedStatement utps = new IfxPreparedStatement(ret);
		System.out.println("SIMHADB: prepareStatement - " + utps);
		return utps;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.sql.Connection#releaseSavepoint(java.sql.Savepoint)
	 */
	@Override
	public void releaseSavepoint(Savepoint savepoint) throws SQLException {
		wrappedConn.releaseSavepoint(savepoint);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.sql.Connection#rollback()
	 */
	@Override
	public void rollback() throws SQLException {
		wrappedConn.rollback();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.sql.Connection#rollback(java.sql.Savepoint)
	 */
	@Override
	public void rollback(Savepoint savepoint) throws SQLException {
		wrappedConn.rollback(savepoint);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.sql.Connection#setAutoCommit(boolean)
	 */
	@Override
	public void setAutoCommit(boolean autoCommit) throws SQLException {
		wrappedConn.setAutoCommit(autoCommit);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.sql.Connection#setCatalog(java.lang.String)
	 */
	@Override
	public void setCatalog(String catalog) throws SQLException {
		wrappedConn.setCatalog(catalog);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.sql.Connection#setHoldability(int)
	 */
	@Override
	public void setHoldability(int holdability) throws SQLException {
		wrappedConn.setHoldability(holdability);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.sql.Connection#setReadOnly(boolean)
	 */
	@Override
	public void setReadOnly(boolean readOnly) throws SQLException {
		wrappedConn.setReadOnly(readOnly);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.sql.Connection#setSavepoint()
	 */
	@Override
	public Savepoint setSavepoint() throws SQLException {
		Savepoint sp = wrappedConn.setSavepoint();
		return sp;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.sql.Connection#setSavepoint(java.lang.String)
	 */
	@Override
	public Savepoint setSavepoint(String name) throws SQLException {
		Savepoint sp = wrappedConn.setSavepoint(name);
		return sp;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.sql.Connection#setTransactionIsolation(int)
	 */
	@Override
	public void setTransactionIsolation(int level) throws SQLException {
		wrappedConn.setTransactionIsolation(level);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.sql.Connection#setTypeMap(java.util.Map)
	 */
	@Override
	public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
		wrappedConn.setTypeMap(map);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.sql.Connection#createClob()
	 */
	@Override
	public Clob createClob() throws SQLException {
		Clob clob = wrappedConn.createClob();
		return clob;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.sql.Connection#createBlob()
	 */
	@Override
	public Blob createBlob() throws SQLException {
		Blob blob = wrappedConn.createBlob();
		return blob;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.sql.Connection#createNClob()
	 */
	@Override
	public NClob createNClob() throws SQLException {
		NClob nclob = wrappedConn.createNClob();
		return nclob;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.sql.Connection#createSQLXML()
	 */
	@Override
	public SQLXML createSQLXML() throws SQLException {
		SQLXML sx = wrappedConn.createSQLXML();
		return sx;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.sql.Connection#isValid(int)
	 */
	@Override
	public boolean isValid(int timeout) throws SQLException {
		boolean ret = wrappedConn.isValid(timeout);
		return ret;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.sql.Connection#setClientInfo(java.lang.String,
	 * java.lang.String)
	 */
	@Override
	public void setClientInfo(String name, String value) throws SQLClientInfoException {
		wrappedConn.setClientInfo(name, value);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.sql.Connection#setClientInfo(java.util.Properties)
	 */
	@Override
	public void setClientInfo(Properties properties) throws SQLClientInfoException {
		wrappedConn.setClientInfo(properties);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.sql.Connection#getClientInfo(java.lang.String)
	 */
	@Override
	public String getClientInfo(String name) throws SQLException {
		String ret = wrappedConn.getClientInfo(name);
		return ret;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.sql.Connection#getClientInfo()
	 */
	@Override
	public Properties getClientInfo() throws SQLException {
		Properties ret = wrappedConn.getClientInfo();
		return ret;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.sql.Connection#createArrayOf(java.lang.String,
	 * java.lang.Object[])
	 */
	@Override
	public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
		Array ret = wrappedConn.createArrayOf(typeName, elements);
		return ret;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.sql.Connection#createStruct(java.lang.String,
	 * java.lang.Object[])
	 */
	@Override
	public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
		Struct ret = wrappedConn.createStruct(typeName, attributes);
		return ret;
	}

	public static void setFailoverValue(int failValue) {
		System.out.println("SIMHADB: setFailoverValue called with value - " + failValue + ", on connection");
		IfxConnection.failoverValue = failValue;
	}

	/**
	 * @return the failoverEnabled
	 */
	public static boolean isFailoverEnabled() {
		return failoverEnabled;
	}

	/**
	 * @param failoverEnabled
	 *            the failoverEnabled to set
	 */
	public static void setFailoverEnabled(boolean failoverEnabled) {
		IfxConnection.failoverEnabled = failoverEnabled;
	}

	/**
	 * @return the failoverCounter
	 */
	public static int getFailoverCounter() {
		return failoverCounter;
	}

	/**
	 * @param failoverCounter
	 *            the failoverCounter to set
	 */
	public static void setFailoverCounter(int failoverCounter) {
		IfxConnection.failoverCounter = failoverCounter;
	}

	public static void incrementFailoverCounter() {
		IfxConnection.failoverCounter++;
	}

	/**
	 * @return the failoverValue
	 */
	public static int getFailoverValue() {
		return failoverValue;
	}

	/**
	 * @return the queryFailoverValue
	 */
	public static int getQueryFailoverValue() {
		return queryFailoverValue;
	}

	/**
	 * @param queryFailoverValue
	 *            the queryFailoverValue to set
	 */
	public static void setQueryFailoverValue(int queryFailoverValue) {
		IfxConnection.queryFailoverValue = queryFailoverValue;
	}

	/**
	 * @return the queryFailoverCounter
	 */
	public static int getQueryFailoverCounter() {
		return queryFailoverCounter;
	}

	/**
	 * @param queryFailoverCounter
	 *            the queryFailoverCounter to set
	 */
	public static void setQueryFailoverCounter(int queryFailoverCounter) {
		IfxConnection.queryFailoverCounter = queryFailoverCounter;
	}

	public static void incrementQueryFailoverCounter() {
		IfxConnection.queryFailoverCounter++;
	}

	/**
	 * @return the queryFailoverEnabled
	 */
	public static boolean isQueryFailoverEnabled() {
		return queryFailoverEnabled;
	}

	/**
	 * @param queryFailoverEnabled
	 *            the queryFailoverEnabled to set
	 */
	public static void setQueryFailoverEnabled(boolean queryFailoverEnabled) {
		IfxConnection.queryFailoverEnabled = queryFailoverEnabled;
	}

	/**
	 * @return the simSQLCode
	 */
	public static int getSimSQLCode() {
		return simSQLCode;
	}

	/**
	 * @param simSQLCode
	 *            the simSQLCode to set
	 */
	public static void setSimSQLCode(int simSQLCode) {
		IfxConnection.simSQLCode = simSQLCode;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.sql.Connection#setSchema(java.lang.String)
	 */
	@Override
	public void setSchema(String schema) throws SQLException {
		// TODO Auto-generated method stub
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.sql.Connection#getSchema()
	 */
	@Override
	public String getSchema() throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.sql.Connection#abort(java.util.concurrent.Executor)
	 */
	@Override
	public void abort(Executor executor) throws SQLException {
		// TODO Auto-generated method stub
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.sql.Connection#setNetworkTimeout(java.util.concurrent.Executor,
	 * int)
	 */
	@Override
	public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.sql.Connection#getNetworkTimeout()
	 */
	@Override
	public int getNetworkTimeout() throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

}
