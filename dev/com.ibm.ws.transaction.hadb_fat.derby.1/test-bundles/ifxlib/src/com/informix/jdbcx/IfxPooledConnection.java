/*******************************************************************************
 * Copyright (c) 2020, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
    Connection unwrappedConnection = null;

    static boolean _areParametersSet = false;
    static int _connectAttempts = 0;

    // CTOR
    public IfxPooledConnection(PooledConnection pc) {
        System.out.println("SIMHADB: IfxPooledConnection called");
        wrappedPooledConn = pc;
        System.out.println("SIMHADB: IfxPooledConnection - " + wrappedPooledConn);
    }

    public IfxPooledConnection(Connection conn) {
        System.out.println("SIMHADB: IfxPooledConnection called with RAW connection");
        unwrappedConnection = conn;
        System.out.println("SIMHADB: IfxPooledConnection - " + unwrappedConnection);
    }

    @Override
    public void addConnectionEventListener(ConnectionEventListener theListener) {
        System.out.println("SIMHADB: IfxPooledConnection.addConnectionEventListener");
        if (wrappedPooledConn != null)
            wrappedPooledConn.addConnectionEventListener(theListener);

    }

    @Override
    public void close() throws SQLException {
        System.out.println("SIMHADB: IfxPooledConnection.close");
        if (wrappedPooledConn != null)
            wrappedPooledConn.close();
        else if (unwrappedConnection != null)
            unwrappedConnection.close();
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.sql.PooledConnection#getConnection()
     *
     * Before passing on the "real", wrapped connection to the caller, this class uses the settings in the HATABLE
     * to configure the behaviour of the connection - how and when it might fail.
     *
     * -> The HATABLE is created in the FailoverServlet in this suite. The tests in the FailoverServlet configure
     * the table to define the kind of HA failure that will be simulated.
     *
     * -> The HA failure is simulated in the IfxPreparedStatement and IfxStatement classes. It is their role to
     * drive SQLTransientExceptions or SQLExceptions at appropriate points.
     */
    @Override
    public Connection getConnection() throws SQLException {
        System.out.println("SIMHADB: IfxPooledConnection.getConnection");
        Connection theConn = null;
        if (wrappedPooledConn != null)
            theConn = wrappedPooledConn.getConnection();
        else if (unwrappedConnection != null)
            theConn = unwrappedConnection;
        IfxConnection ifc = new IfxConnection(theConn);
        System.out.println("SIMHADB: IfxPooledConnection.getConnection ifc - " + ifc + ", are parameters set? - " + _areParametersSet +
                           ", connectAttempts already - " + _connectAttempts);
        // Increment connect attempt counter
        _connectAttempts++;

        if (!_areParametersSet) {
            _areParametersSet = true;
            Statement stmt = theConn.createStatement();
            System.out.println("SIMHADB: IfxPooledConnection created statement - " + stmt);
            ResultSet rsBasic = null;
            try {
                System.out.println("SIMHADB: Execute a query to see if we can find the table");
                rsBasic = stmt.executeQuery("SELECT testtype, failingoperation, numberoffailures, simsqlcode" + " FROM hatable");
                if (rsBasic.next()) {
                    int testTypeInt = rsBasic.getInt(1);
                    System.out.println("SIMHADB: Stored column testtype is: " + testTypeInt);
                    int failingOperation = rsBasic.getInt(2);
                    System.out.println("SIMHADB: Stored column failingoperation is: " + failingOperation);
                    int numberOfFailuresInt = rsBasic.getInt(3);
                    System.out.println("SIMHADB: Stored column numberoffailures is: " + numberOfFailuresInt);
                    int simsqlcodeInt = rsBasic.getInt(4);
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
                        if (failingOperation == 999) {
                            IfxConnectionPoolDataSource.setTestingFailoverAtRuntime(false);
                            IfxConnection.setFailoverEnabled(false);
                            System.out.println("SIMHADB: update HATABLE with faoloverval 0");
                            stmt.executeUpdate("update hatable set failingoperation = 0 where testtype = 0");
                            theConn.commit();
                            System.out.println("SIMHADB: HATABLE committed");
                            System.out.println("SIMHADB: Set simsqlcode to: " + simsqlcodeInt);
                            IfxConnection.setSimSQLCode(simsqlcodeInt);
                        } else {
                            if (!IfxConnectionPoolDataSource.isTestingFailoverAtRuntime()) {
                                System.out.println(
                                                   "SIMHADB: Already set to test failover at startup, we don't want to change settings");
                            } else {
                                System.out.println("SIMHADB: Test failover at startup, make settings");
                                IfxConnection.setFailoverValue(failingOperation);
                                IfxConnection.setQueryFailoverEnabled(true);
                                IfxConnectionPoolDataSource.setTestingFailoverAtRuntime(true);
                                IfxConnection.setQueryFailoverCounter(0);

                                System.out.println("SIMHADB: Set simsqlcode to: " + simsqlcodeInt);
                                IfxConnection.setSimSQLCode(simsqlcodeInt);
                            }
                        }
                    } else if (testTypeInt == 1) // Test Failover at runtime
                    {
                        IfxConnection.setFailoverEnabled(true);
                        IfxConnection.setFailoverCounter(0);

                        System.out.println(
                                           "SIMHADB: Test failover at runtime, Stored column failoverval is: " + failingOperation);
                        if (failingOperation > 0)
                            IfxConnection.setFailoverValue(failingOperation);
                        System.out.println("SIMHADB: Set simsqlcode to: " + simsqlcodeInt);
                        IfxConnection.setSimSQLCode(simsqlcodeInt);

                        if (numberOfFailuresInt > 1) {
                            IfxConnection.setFailingRetries(numberOfFailuresInt);
                            IfxConnection.setFailingRetryCounter(0);
                        }
                    } else if (testTypeInt == 2)// Test duplication in the recovery logs
                    {
                        IfxConnection.setDuplicationEnabled(true); // Set this so that we will check in IfxPreparedStatement.eExecuteBatch()to see if we should be duplicating rows.
                        IfxConnection.setDuplicateCounter(0); // Count the number of executeBatch() calls we have made

                        // Also enable a halt
                        IfxConnection.setHaltEnabled(true);
                        System.out.println(
                                           "SIMHADB: Test duplication at runtime, Stored column failoverval is: " + failingOperation);
                        if (failingOperation > 0)
                            IfxConnection.setFailoverValue(failingOperation); //  When the duplicateCounter reaches this value we'll start collecting duplicate rows
                        System.out.println("SIMHADB: Set simsqlcode to: " + simsqlcodeInt);
                        IfxConnection.setSimSQLCode(simsqlcodeInt);
                    } else if (testTypeInt == 3)// Test duplication at runtime
                    {
                        IfxConnection.setDuplicationEnabled(true);
                        IfxConnection.setDuplicateCounter(0);

                        System.out.println(
                                           "SIMHADB: Test duplication at runtime, Stored column failoverval is: " + failingOperation);
                        if (failingOperation > 0)
                            IfxConnection.setFailoverValue(failingOperation);
                        System.out.println("SIMHADB: Set simsqlcode to: " + simsqlcodeInt);
                        IfxConnection.setSimSQLCode(simsqlcodeInt);
                    } else if (testTypeInt == 4)// Test "halt" - used as control for duplication test
                    {
                        IfxConnection.setHaltEnabled(true);
                        IfxConnection.setHaltCounter(0);

                        System.out.println(
                                           "SIMHADB: Test halt at runtime, Stored column failoverval is: " + failingOperation);
                        if (failingOperation > 0)
                            IfxConnection.setFailoverValue(failingOperation);
                        System.out.println("SIMHADB: Set simsqlcode to: " + simsqlcodeInt);
                        IfxConnection.setSimSQLCode(simsqlcodeInt);
                    } else if (testTypeInt == 5)// Special case of failure at connection, throw an exception here
                    {
                        // Dependent on number of connection attempts, reset _areParametersSet
                        if (_connectAttempts < numberOfFailuresInt)
                            _areParametersSet = false;

                        if (_areParametersSet) {
                            // Last time through, need to update hatable to avoid interference with subsequent tests
                            System.out.println("SIMHADB: update HATABLE with testtype = 99");
                            stmt.executeUpdate("update hatable set testtype = 99 where testtype = 5");
                        }

                        String sqlReason = "Generated internally";
                        String sqlState = "Generated reason";
                        int reasonCode = -777;

                        System.out.println("SIMHADB: sqlcode set to: " + reasonCode);
                        // if reason code is "-3" then exception is non-transient, otherwise it is transient
                        SQLException sqlex = new SQLException(sqlReason, sqlState, reasonCode);

                        throw sqlex;
                    } else if (testTypeInt == 6) { // Lease Log tests
                        // We abuse the failovervalInt parameter.
                        // 770 - lease update test
                        if (failingOperation == 770) {
                            IfxConnection.setTestingLeaselogUpdateFlag(true);
                        } else if (failingOperation == 771) {
                            IfxConnection.setTestingLeaselogDeleteFlag(true);
                        } else if (failingOperation == 772) {
                            IfxConnection.setTestingLeaselogClaimFlag(true);
                        } else if (failingOperation == 773) {
                            IfxConnection.setTestingLeaselogGetFlag(true);
                        }
                    } else if (testTypeInt == 7) { // Aggressive peer recovery takeover tests
                        IfxConnection.setPeerRecoveryPause(true);
                        IfxConnection.setFailingRetries(numberOfFailuresInt);
                    }
                } else {
                    System.out.println("SIMHADB: Empty result set");
                    IfxConnection.setFailoverEnabled(false);
                }
            } catch (SQLException sqle) {
                int errorCode = sqle.getErrorCode();
                System.out.println("SIMHADB: IfxPooledConnection.getConnection caught SQLException - " + sqle + " with error code: " + errorCode);

                // No table, disable failover
                IfxConnection.setFailoverEnabled(false);
                if (errorCode == -777) // rethrow
                    throw sqle;
            } catch (Exception e) {
                System.out.println("SIMHADB: IfxPooledConnection.getConnection caught General exception - " + e);
                // No table, disable failover
                IfxConnection.setFailoverEnabled(false);
            } finally {
                if (stmt != null)
                    stmt.close();
                if (rsBasic != null)
                    rsBasic.close();
            }
        }

        return ifc;
    }

    @Override
    public void removeConnectionEventListener(ConnectionEventListener theListener) {
        System.out.println("SIMHADB: IfxPooledConnection.removeConnectionEventListener");
        if (wrappedPooledConn != null)
            wrappedPooledConn.removeConnectionEventListener(theListener);
    }

    @Override
    public void addStatementEventListener(StatementEventListener listener) {
        System.out.println("SIMHADB: IfxPooledConnection.addStatementEventListener");
        if (wrappedPooledConn != null)
            wrappedPooledConn.addStatementEventListener(listener);
    }

    @Override
    public void removeStatementEventListener(StatementEventListener listener) {
        System.out.println("SIMHADB: IfxPooledConnection.removeStatementEventListener");
        if (wrappedPooledConn != null)
            wrappedPooledConn.removeStatementEventListener(listener);
    }

}
