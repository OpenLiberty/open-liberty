/*******************************************************************************
 * Copyright (c) 2020, 2024 IBM Corporation and others.
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

import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.ConnectionEventListener;
import javax.sql.PooledConnection;
import javax.sql.StatementEventListener;

import com.ibm.tx.jta.ut.util.HADBTestConstants.HADBTestType;
import com.ibm.tx.jta.ut.util.HADBTestControl;

public class IfxPooledConnection implements PooledConnection {

    static {
        HADBTestControl.init(Paths.get(System.getenv("WLP_OUTPUT_DIR")).getParent().resolve(Paths.get("shared")).toString());
    }

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
            try {
                System.out.println("SIMHADB: Execute a query to see if we can find the table");
                HADBTestControl testControl = HADBTestControl.read();
                System.out.println("HADBtestControl: " + testControl);
                final HADBTestType testType = testControl.getTestType();
                System.out.println("SIMHADB: Stored column testtype is: " + testType);
                int simsqlcodeInt = testControl.getSimsqlcodeInt();
                System.out.println("SIMHADB: Stored column simsqlcode is: " + simsqlcodeInt);
                int failingOperation = testControl.getFailingOperation();
                System.out.println("SIMHADB: Stored column failingoperation is: " + failingOperation);
                int numberOfFailuresInt = testControl.getNumberOfFailuresInt();
                System.out.println("SIMHADB: Stored column numberoffailures is: " + numberOfFailuresInt);

                switch (testType) {
                    case STARTUP:
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
                            HADBTestControl.write(testType, simsqlcodeInt, 0, numberOfFailuresInt);
                            System.out.println("SIMHADB: update HATABLE with faoloverval 0");
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
                        break;

                    case RUNTIME:
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
                        break;

                    case DUPLICATE_RESTART:
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
                        break;

                    case DUPLICATE_RUNTIME:
                        IfxConnection.setDuplicationEnabled(true);
                        IfxConnection.setDuplicateCounter(0);

                        System.out.println(
                                           "SIMHADB: Test duplication at runtime, Stored column failoverval is: " + failingOperation);
                        if (failingOperation > 0)
                            IfxConnection.setFailoverValue(failingOperation);
                        System.out.println("SIMHADB: Set simsqlcode to: " + simsqlcodeInt);
                        IfxConnection.setSimSQLCode(simsqlcodeInt);
                        break;

                    case HALT:
                        IfxConnection.setHaltEnabled(true);
                        IfxConnection.setHaltCounter(0);

                        System.out.println(
                                           "SIMHADB: Test halt at runtime, Stored column failoverval is: " + failingOperation);
                        if (failingOperation > 0)
                            IfxConnection.setFailoverValue(failingOperation);
                        System.out.println("SIMHADB: Set simsqlcode to: " + simsqlcodeInt);
                        IfxConnection.setSimSQLCode(simsqlcodeInt);
                        break;

                    case CONNECT:
                        // Dependent on number of connection attempts, reset _areParametersSet
                        if (_connectAttempts < numberOfFailuresInt)
                            _areParametersSet = false;

                        if (_areParametersSet) {
                            // Last time through, need to update hatable to avoid interference with subsequent tests
                            HADBTestControl.write(HADBTestType.NONE, simsqlcodeInt, failingOperation, numberOfFailuresInt);
                        }

                        String sqlReason = "Generated internally";
                        String sqlState = "Generated reason";
                        int reasonCode = -777;

                        System.out.println("SIMHADB: sqlcode set to: " + reasonCode);
                        // if reason code is "-3" then exception is non-transient, otherwise it is transient
                        SQLException sqlex = new SQLException(sqlReason, sqlState, reasonCode);

                        throw sqlex;

                    case LEASE:
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
                        break;

                    default:
                        System.out.println("SIMHADB: unknown test type");
                        break;
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
