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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.ConnectionEventListener;
import javax.sql.PooledConnection;
import javax.sql.StatementEventListener;

public class IfxPooledConnection implements PooledConnection {
	PooledConnection wrappedPooledConn = null;

	// CTOR
	public IfxPooledConnection(PooledConnection pc) {
		System.out.println("SIMHADB: IfxPooledConnection called");
		wrappedPooledConn = pc;
		System.out.println("SIMHADB: IfxPooledConnection - " + wrappedPooledConn);
	}

	@Override
	public void addConnectionEventListener(ConnectionEventListener theListener) {
		System.out.println("SIMHADB: IfxPooledConnection.addConnectionEventListener");
		wrappedPooledConn.addConnectionEventListener(theListener);
	}

	@Override
	public void close() throws SQLException {
		System.out.println("SIMHADB: IfxPooledConnection.close");
		wrappedPooledConn.close();
	}

	@Override
	public Connection getConnection() throws SQLException {
		System.out.println("SIMHADB: IfxPooledConnection.getConnection");
		Connection theConn = wrappedPooledConn.getConnection();
		IfxConnection ifc = new IfxConnection(theConn);
		System.out.println("SIMHADB: IfxPooledConnection.getConnection ret - " + ifc);

		Statement stmt = theConn.createStatement();
		System.out.println("SIMHADB: IfxPooledConnection created statement - " + stmt);
		ResultSet rsBasic = null;
		try {
			System.out.println("SIMHADB: Execute a query to see if we can find the table");
			rsBasic = stmt.executeQuery("SELECT testtype, failoverval, simsqlcode" + " FROM hatable");
			if (rsBasic.next()) {
				int testTypeInt = rsBasic.getInt(1);
				System.out.println("SIMHADB: Stored column testtype is: " + testTypeInt);
				int failovervalInt = rsBasic.getInt(2);
				System.out.println("SIMHADB: Stored column failoverval is: " + failovervalInt);
				int simsqlcodeInt = rsBasic.getInt(3);
				System.out.println("SIMHADB: Stored column simsqlcode is: " + simsqlcodeInt);

				if (testTypeInt == 0) // Test Failover at startup
				{
					// We abuse the failovervalInt parameter. If it is set to
					// 999, then we will
					// not enable the failover function, so that the server can
					// safely shut
					// down. But we reset the column, so that next time (on
					// startup) failover
					// will be enabled
					if (failovervalInt == 999) {
						IfxConnectionPoolDataSource.setTestingFailoverAtRuntime(false);
						IfxConnection.setFailoverEnabled(false);
						System.out.println("SIMHADB: update HATABLE with faoloverval 0");
						stmt.executeUpdate("update hatable set failoverval = 0 where testtype = 0");
						theConn.commit();
						System.out.println("SIMHADB: HATABLE committed");
						System.out.println("SIMHADB: Set simsqlcode to: " + simsqlcodeInt);
						IfxConnection.setSimSQLCode(simsqlcodeInt);
					} else {
						if (!IfxConnectionPoolDataSource.isTestingFailoverAtRuntime()) {
							System.out.println(
									"SIMHADB: Already set to test failover at startup, we don't want to change settings");

							IfxConnection.setFailoverValue(failovervalInt);
							IfxConnection.setQueryFailoverEnabled(true);
							IfxConnection.setQueryFailoverCounter(0);

							System.out.println("SIMHADB: Set simsqlcode to: " + simsqlcodeInt);
							IfxConnection.setSimSQLCode(simsqlcodeInt);
						}
					}
				} else // Test Failover at runtime
				{
					IfxConnection.setFailoverEnabled(true);
					IfxConnection.setFailoverCounter(0);

					System.out.println(
							"SIMHADB: Test failover at runtime, Stored column failoverval is: " + failovervalInt);
					if (failovervalInt > 0)
						IfxConnection.setFailoverValue(failovervalInt);
					System.out.println("SIMHADB: Set simsqlcode to: " + simsqlcodeInt);
					IfxConnection.setSimSQLCode(simsqlcodeInt);
				}
			} else {
				System.out.println("SIMHADB: Empty result set");
				IfxConnection.setFailoverEnabled(false);
			}
		} catch (Exception e) {
			System.out.println("SIMHADB: Caught exception - " + e);
			// No table, disable failover
			IfxConnection.setFailoverEnabled(false);
		} finally {
			if (stmt != null)
				stmt.close();
			if (rsBasic != null)
				rsBasic.close();
		}

		return ifc;
	}

	@Override
	public void removeConnectionEventListener(ConnectionEventListener theListener) {
		System.out.println("SIMHADB: IfxPooledConnection.removeConnectionEventListener");
		wrappedPooledConn.removeConnectionEventListener(theListener);
	}

	@Override
	public void addStatementEventListener(StatementEventListener listener) {
		System.out.println("SIMHADB: IfxPooledConnection.addStatementEventListener");
		wrappedPooledConn.addStatementEventListener(listener);
	}

	@Override
	public void removeStatementEventListener(StatementEventListener listener) {
		System.out.println("SIMHADB: IfxPooledConnection.removeStatementEventListener");
		wrappedPooledConn.removeStatementEventListener(listener);
	}

}
