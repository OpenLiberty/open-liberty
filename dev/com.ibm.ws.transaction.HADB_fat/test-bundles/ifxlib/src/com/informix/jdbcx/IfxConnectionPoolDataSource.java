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

import java.io.PrintWriter;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

import javax.sql.PooledConnection;

import org.apache.derby.jdbc.EmbeddedConnectionPoolDataSource40;

public class IfxConnectionPoolDataSource
		// extends IfxDataSource
		implements javax.sql.ConnectionPoolDataSource {
	EmbeddedConnectionPoolDataSource40 embedDS = null;
	private String dbName = "";
	private String createStr = "";

	// The HATABLE is configured by a servlet at runtime. When testing startup
	// we want to be able
	// to configure the table for the next restart but don't want our change to
	// be visible during
	// shutdown.
	private static boolean testingFailoverAtRuntime = true;

	// CTOR
	public IfxConnectionPoolDataSource() {
		System.out.println("SIMHADB: IfxConnectionPoolDataSource called");
		embedDS = new EmbeddedConnectionPoolDataSource40();
		System.out.println("SIMHADB: IfxConnectionPoolDataSource - " + embedDS);
	}

	@Override
	public PooledConnection getPooledConnection() throws SQLException {
		System.out.println("SIMHADB: IfxConnectionPoolDataSource.getPooledConnection");
		PooledConnection pc = embedDS.getPooledConnection();
		IfxPooledConnection utc = new IfxPooledConnection(pc);
		System.out.println("SIMHADB: getPooledConnection return " + utc);
		return utc;
	}

	@Override
	public PooledConnection getPooledConnection(String theUser, String thePassword) throws SQLException {
		System.out.println("SIMHADB: UTConnectionPoolDataSource.getPooledConnection(U,P)");
		PooledConnection pc = embedDS.getPooledConnection(theUser, thePassword);
		IfxPooledConnection utc = new IfxPooledConnection(pc);
		System.out.println("SIMHADB: getPooledConnection return " + utc);
		return utc;
	}

	@Override
	public int getLoginTimeout() throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public PrintWriter getLogWriter() throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setLoginTimeout(int seconds) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public void setLogWriter(PrintWriter out) throws SQLException {
		// TODO Auto-generated method stub

	}

	/*
	 * @Override public <T> T unwrap(Class<T> iface) throws SQLException { //
	 * TODO Auto-generated method stub return null; }
	 * 
	 * @Override public boolean isWrapperFor(Class<?> iface) throws SQLException
	 * { // TODO Auto-generated method stub return false; }
	 * 
	 * @Override public Connection getConnection() throws SQLException { // TODO
	 * Auto-generated method stub return null; }
	 * 
	 * @Override public Connection getConnection(String theUsername, String
	 * thePassword) throws SQLException { // TODO Auto-generated method stub
	 * return null; }
	 */
	public String getDatabaseName() {
		System.out.println("SIMHADB: IfxConnectionPoolDataSource.getDatabaseName: " + dbName);
		return dbName;
	}

	public void setDatabaseName(String dbName) {
		System.out.println("SIMHADB: IfxConnectionPoolDataSource.setDatabaseName: " + dbName);
		this.dbName = dbName;
		embedDS.setDatabaseName(dbName);
	}

	public String getCreateDatabase() {
		System.out.println("SIMHADB: IfxConnectionPoolDataSource.getCreateDatabase: " + createStr);
		return createStr;
	}

	public void setCreateDatabase(String create) {
		System.out.println("SIMHADB: IfxConnectionPoolDataSource.setCreateDatabase: " + create);
		this.createStr = create;
		embedDS.setCreateDatabase(create);
	}

	/**
	 * @return the startupPhase
	 */
	public static boolean isTestingFailoverAtRuntime() {
		return testingFailoverAtRuntime;
	}

	/**
	 * @param startupPhase
	 *            the startupPhase to set
	 */
	public static void setTestingFailoverAtRuntime(boolean startupPhase) {
		IfxConnectionPoolDataSource.testingFailoverAtRuntime = startupPhase;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.sql.CommonDataSource#getParentLogger()
	 */
	@Override
	public Logger getParentLogger() throws SQLFeatureNotSupportedException {
		// TODO Auto-generated method stub
		return null;
	}
}
