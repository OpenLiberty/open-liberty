/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.transaction.web;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Resource;
import javax.annotation.Resource.AuthenticationType;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import com.ibm.tx.jta.ExtendedTransactionManager;
import com.ibm.tx.jta.TransactionManagerFactory;
import com.ibm.tx.jta.ut.util.XAResourceImpl;
import com.ibm.tx.jta.ut.util.XAResourceInfoFactory;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/FailoverServlet")
public class FailoverServlet extends FATServlet {

    private enum TestType {
        STARTUP, RUNTIME, DUPLICATE_RESTART, DUPLICATE_RUNTIME, HALT, CONNECT
    };

    /**
     * Lookup string that allows character digit lookup by index value.
     * ie _digits[9] == '9' etc.
     */
    private final static String _digits = "0123456789abcdef";

    @Resource(name = "jdbc/tranlogDataSource", shareable = true, authenticationType = AuthenticationType.APPLICATION)
    DataSource ds;

    public void setupForRecoverableFailover(HttpServletRequest request, HttpServletResponse response) throws Exception {
        System.out.println("FAILOVERSERVLET: drive setupForRecoverableFailover");
        setupTestParameters(request, response, TestType.RUNTIME, -4498, 12, 1);
        System.out.println("FAILOVERSERVLET: setupForRecoverableFailover complete");
    }

    public void setupForRecoverableFailureMultipleRetries(HttpServletRequest request, HttpServletResponse response) throws Exception {
        System.out.println("FAILOVERSERVLET: drive setupForRecoverableFailureMultipleRetries");
        setupTestParameters(request, response, TestType.RUNTIME, -4498, 12, 5); // Can fail up to 5 times
        System.out.println("FAILOVERSERVLET: setupForRecoverableFailureMultipleRetries complete");
    }

    public void setupForNonRecoverableFailover(HttpServletRequest request, HttpServletResponse response) throws Exception {
        System.out.println("FAILOVERSERVLET: drive setupForNonRecoverableFailover");
        setupTestParameters(request, response, TestType.RUNTIME, -3, 12, 1);
        System.out.println("FAILOVERSERVLET: setupForNonRecoverableFailover complete");
    }

    public void setupForConnectFailover(HttpServletRequest request, HttpServletResponse response) throws Exception {
        System.out.println("FAILOVERSERVLET: drive setupForConnectFailover");
        setupTestParameters(request, response, TestType.CONNECT, 0, 0, 1);
        System.out.println("FAILOVERSERVLET: setupForConnectFailover complete");
    }

    public void setupForMultiConnectFailover(HttpServletRequest request, HttpServletResponse response) throws Exception {
        System.out.println("FAILOVERSERVLET: drive setupForConnectFailover");
        setupTestParameters(request, response, TestType.CONNECT, 0, 0, 3);
        System.out.println("FAILOVERSERVLET: setupForConnectFailover complete");
    }

    public void setupForStartupFailover(HttpServletRequest request, HttpServletResponse response) throws Exception {
        System.out.println("FAILOVERSERVLET: drive setupForStartupFailover");
        setupTestParameters(request, response, TestType.STARTUP, -4498, 999, 1);
        System.out.println("FAILOVERSERVLET: setupForStartupFailover complete");
    }

    public void setupForDuplicationRestart(HttpServletRequest request, HttpServletResponse response) throws Exception {
        System.out.println("FAILOVERSERVLET: drive setupForDuplicationRestart");
        setupTestParameters(request, response, TestType.DUPLICATE_RESTART, 0, 10, 1);
        System.out.println("FAILOVERSERVLET: setupForDuplicationRestart complete");
    }

    public void setupForDuplicationRuntime(HttpServletRequest request, HttpServletResponse response) throws Exception {
        System.out.println("FAILOVERSERVLET: drive setupForDuplicationRuntime");
        setupTestParameters(request, response, TestType.DUPLICATE_RUNTIME, 0, 10, 1);
        System.out.println("FAILOVERSERVLET: setupForDuplicationRuntime complete");
    }

    public void setupForHalt(HttpServletRequest request, HttpServletResponse response) throws Exception {
        System.out.println("FAILOVERSERVLET: drive setupForHalt");
        setupTestParameters(request, response, TestType.HALT, 0, 12, 1); // set the ioperation to the duplicate test value + 2
        System.out.println("FAILOVERSERVLET: setupForHalt complete");
    }

    private void setupTestParameters(HttpServletRequest request, HttpServletResponse response, TestType testType,
                                     int thesqlcode, int operationToFail, int numberOfFailures) throws Exception {
        System.out.println("FAILOVERSERVLET: drive setupTestParameters");

        Connection con = ds.getConnection();
        try {
            // Set up statement to use for table delete/recreate
            Statement stmt = con.createStatement();

            try {
                System.out.println("FAILOVERSERVLET: drop hatable");
                stmt.executeUpdate("drop table hatable");
            } catch (SQLException x) {
                // didn't exist
            }
            System.out.println("FAILOVERSERVLET: create hatable");
            stmt.executeUpdate(
                               "create table hatable (testtype int not null primary key, failingoperation int, numberoffailures int, simsqlcode int)");
            // was col2 varchar(20)
            System.out.println("FAILOVERSERVLET: insert row into hatable - type" + testType.ordinal()
                               + ", operationtofail: " + operationToFail + ", sqlcode: " + thesqlcode);
            stmt.executeUpdate("insert into hatable values (" + testType.ordinal() + ", " + operationToFail + ", " + numberOfFailures + ", "
                               + thesqlcode + ")"); // was -4498

            // UserTransaction Commit
            con.setAutoCommit(false);

            System.out.println("FAILOVERSERVLET: commit changes to database");
            con.commit();
        } catch (Exception ex) {
            System.out.println("FAILOVERSERVLET: caught exception in testSetup: " + ex);
        }
    }

    /**
     * Test enlistment in transactions.
     *
     * @param request
     *            HTTP request
     * @param response
     *            HTTP response
     * @throws Exception
     *             if an error occurs.
     */
    public void driveTransactions(HttpServletRequest request, HttpServletResponse response) throws Exception {
        System.out.println("FAILOVERSERVLET: driveTransactions");

        // Set the test parameters
        int batchSize = 10;
        int resources = 2;
        try {
            // Drive the transactions
            System.out.println("FAILOVERSERVLET: drive the Performance Test, resources: " + resources + ", batchSize: "
                               + batchSize);

            simulateTransactions(request, response, batchSize, resources);

        } catch (Exception e) {
            System.out.println("FAILOVERSERVLET: EXCEPTION: " + e);
            throw new Exception();
        }

    }

    public void driveSixTransactions(HttpServletRequest request, HttpServletResponse response) throws Exception {
        System.out.println("FAILOVERSERVLET: driveSixTransactions");

        // Set the test parameters
        int batchSize = 6;
        int resources = 2;
        try {
            // Drive the transactions
            System.out.println("FAILOVERSERVLET: drive the Performance Test, resources: " + resources + ", batchSize: "
                               + batchSize);

            simulateTransactions(request, response, batchSize, resources);

        } catch (Exception e) {
            System.out.println("FAILOVERSERVLET: EXCEPTION: " + e);
            throw new Exception();
        }

    }

    public void driveTransactionsWithFailure(HttpServletRequest request, HttpServletResponse response) throws Exception {
        System.out.println("FAILOVERSERVLET: driveTransactionsWithFailure");

        // Set the test parameters
        int batchSize = 10;
        int resources = 2;

        try {
            // Drive the transactions
            System.out.println("FAILOVERSERVLET: drive a batch of transactions, resources: " + resources + ", batchSize: "
                               + batchSize);

            simulateTransactions(request, response, batchSize, resources);
            System.out.println("FAILOVERSERVLET: failed to throw SystemException");
            throw new Exception();
        } catch (javax.transaction.SystemException sysex) {
            System.out.println("FAILOVERSERVLET: caught SYSTEMEXCEPTION as expected: " + sysex);
        } catch (Exception e) {
            System.out.println("FAILOVERSERVLET: unexpected EXCEPTION: " + e);
        }
    }

    private void simulateTransactions(HttpServletRequest request, HttpServletResponse response, int batchSize, int resources) throws Exception {
        final ExtendedTransactionManager tm = TransactionManagerFactory.getTransactionManager();

        System.out.println("FAILOVERSERVLET: Starting simulateTransactions");

        tm.begin();
        tm.commit();

        // final int recoveryId =
        // tm.registerResourceInfo("com.ibm.tx.test.JTMXAResourceFactory", new
        // JTMXAResourceInfo());
        final Serializable xaResInfo1 = XAResourceInfoFactory.getXAResourceInfo(0);

        final int recoveryId = tm.registerResourceInfo("(testfilter=jon)", xaResInfo1);

        /*
         * These parameters specify the key attributes of the test: the number
         * of threads to execute, the number of Resource Managers to simulate
         * and the total number of transactions to attempt to drive.
         */

        for (int i = 0; i < batchSize; i++) {
            System.out.println("FAILOVERSERVLET: Begin Transaction - " + i);
            tm.begin();

            if (resources > 0) {
                for (int j = 0; j < resources; j++) {
                    tm.enlist(new XAResourceImpl(), recoveryId);
                }
            }
            tm.commit();
        }
        System.out.println("FAILOVERSERVLET: simulateTransactions main loop has completed successfully");

    }

    public void checkForDuplicates(HttpServletRequest request,
                                   HttpServletResponse response) throws Exception {
        Connection conn = ds.getConnection();
        conn.setAutoCommit(false);
        Set<List> resultSet;
        List row;
        Statement recoveryStmt = null;
        ResultSet recoveryRS = null;

        try {
            recoveryStmt = conn.createStatement();
            String queryString = "SELECT RU_ID, RUSECTION_ID, RUSECTION_DATA_INDEX, DATA" +
                                 " FROM WAS_TRAN_LOG" +
                                 " WHERE SERVER_NAME='com.ibm.ws.transaction'" +
                                 " AND SERVICE_ID=1";
            System.out.println("Retrieve all rows from table using - " + queryString);

            recoveryRS = recoveryStmt.executeQuery(queryString);

            resultSet = new HashSet<List>();

            while (recoveryRS.next()) {
                final long ruId = recoveryRS.getLong(1);
                final long sectId = recoveryRS.getLong(2);
                final int index = recoveryRS.getInt(3);
                final byte[] data = recoveryRS.getBytes(4);
                String theBytesString = toHexString(data, 32);
                System.out.println("SQL TRANLOG: ruId: " + ruId + " sectionId: " + sectId + " item: " + index + " data: " + theBytesString);
                row = new ArrayList();
                row.add(ruId);
                row.add(sectId);
                row.add(index);
                if (resultSet.add(row)) {
                    System.out.println("SQL TRANLOG: UNIQUE row");
                } else {
                    System.out.println("SQL TRANLOG: Found DUPLICATE row");
                }
            }

            // UserTransaction Commit
            conn.setAutoCommit(false);

            System.out.println("FAILOVERSERVLET: commit changes to database");
            conn.commit();
        } catch (Exception ex) {
            System.out.println("FAILOVERSERVLET: caught exception in testSetup: " + ex);
        } finally {
            if (recoveryRS != null && !recoveryRS.isClosed())
                recoveryRS.close();
            if (recoveryStmt != null && !recoveryStmt.isClosed())
                recoveryStmt.close();
        }

    }

    public static String toHexString(byte[] byteSource, int bytes) {
        StringBuffer result = null;
        boolean truncated = false;

        if (byteSource != null) {
            if (bytes > byteSource.length) {
                // If the number of bytes to display is larger than the available number of
                // bytes, then reset the number of bytes to display to be the available
                // number of bytes.
                bytes = byteSource.length;
            } else if (bytes < byteSource.length) {
                // If we are displaying less bytes than are available then detect this
                // 'truncation' condition.
                truncated = true;
            }

            result = new StringBuffer(bytes * 2);
            for (int i = 0; i < bytes; i++) {
                result.append(_digits.charAt((byteSource[i] >> 4) & 0xf));
                result.append(_digits.charAt(byteSource[i] & 0xf));
            }

            if (truncated) {
                result.append("... (" + bytes + "/" + byteSource.length + ")");
            } else {
                result.append("(" + bytes + ")");
            }
        } else {
            result = new StringBuffer("null");
        }

        return (result.toString());
    }
}