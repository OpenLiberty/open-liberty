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
package com.ibm.ws.transaction.web;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

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
        STARTUP, RUNTIME
    };

    private static int _batchSize;
    private static int _resources;

    @Resource(name = "jdbc/tranlogDataSource", shareable = true, authenticationType = AuthenticationType.APPLICATION)
    DataSource ds;

    public void testControlSetup(HttpServletRequest request, HttpServletResponse response) throws Exception {
        System.out.println("FAILOVERSERVLET: drive testControlSetup");

        Connection con = ds.getConnection();
        try {
            // Statement used to drop table
            Statement stmt = con.createStatement();

            try {
                System.out.println("FAILOVERSERVLET: drop hatable");
                stmt.executeUpdate("drop table hatable");
            } catch (SQLException x) {
                // didn't exist
            }

            System.out.println("FAILOVERSERVLET: commit changes to database");
            con.commit();
        } catch (Exception ex) {
            System.out.println("FAILOVERSERVLET: caught exception in testSetup: " + ex);
        }
        System.out.println("FAILOVERSERVLET: testControlSetup complete");
    }

    public void testSetupKnownSqlcode(HttpServletRequest request, HttpServletResponse response) throws Exception {
        System.out.println("FAILOVERSERVLET: drive testSetupKnownSqlcode");
        testSetupWithSqlcode(request, response, TestType.RUNTIME, -4498, 12);
        System.out.println("FAILOVERSERVLET: testSetupWithSqlcode complete");
    }

    public void testSetupUnKnownSqlcode(HttpServletRequest request, HttpServletResponse response) throws Exception {
        System.out.println("FAILOVERSERVLET: drive testSetupUnKnownSqlcode");
        testSetupWithSqlcode(request, response, TestType.RUNTIME, -3, 12);
        System.out.println("FAILOVERSERVLET: testSetupUnKnownSqlcode complete");
    }

    public void testStartupSetup(HttpServletRequest request, HttpServletResponse response) throws Exception {
        System.out.println("FAILOVERSERVLET: drive testStartupSetup");
        testSetupWithSqlcode(request, response, TestType.STARTUP, -4498, 999);
        System.out.println("FAILOVERSERVLET: testStartupSetup complete");
    }

    private void testSetupWithSqlcode(HttpServletRequest request, HttpServletResponse response, TestType testType,
                                      int thesqlcode, int operationToFail) throws Exception {
        System.out.println("FAILOVERSERVLET: drive testSetupWithSqlcode");

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
                               "create table hatable (testtype int not null primary key, failoverval int, simsqlcode int)");
            // was col2 varchar(20)
            System.out.println("FAILOVERSERVLET: insert row into hatable - type" + testType.ordinal()
                               + ", operationtofail: " + operationToFail + ", sqlcode: " + thesqlcode);
            stmt.executeUpdate("insert into hatable values (" + testType.ordinal() + ", " + operationToFail + ", "
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
    public void testDriveTransactions(HttpServletRequest request, HttpServletResponse response) throws Exception {
        System.out.println("FAILOVERSERVLET: drive testDriveTransactions");

        // Get the test parameters
        _batchSize = 10;
        _resources = 2;

        try {
            // Drive the transactions
            System.out.println("FAILOVERSERVLET: drive the Performance Test, resources: " + _resources + ", batchSize: "
                               + _batchSize);

            simulateTransactions(request, response);

        } catch (Exception e) {
            System.out.println("FAILOVERSERVLET: EXCEPTION: " + e);
            throw new Exception();
        }

    }

    public void testDriveTransactionsWithFailure(HttpServletRequest request, HttpServletResponse response) throws Exception {
        System.out.println("FAILOVERSERVLET: drive testDriveTransactionsWithFailure");

        // Get the test parameters
        _batchSize = 10;
        _resources = 2;

        try {
            // Drive the transactions
            System.out.println("FAILOVERSERVLET: drive the Performance Test, resources: " + _resources + ", batchSize: "
                               + _batchSize);

            simulateTransactions(request, response);
            System.out.println("FAILOVERSERVLET: failed to throw SystemException");
            throw new Exception();
        } catch (javax.transaction.SystemException sysex) {
            System.out.println("FAILOVERSERVLET: caught SYSTEMEXCEPTION as expected: " + sysex);
        } catch (Exception e) {
            System.out.println("FAILOVERSERVLET: unexpected EXCEPTION: " + e);
            throw new Exception();
        }
    }

    private void simulateTransactions(HttpServletRequest request, HttpServletResponse response) throws Exception {
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

        for (int i = 0; i < _batchSize; i++) {
            tm.begin();

            if (_resources > 0) {
                for (int j = 0; j < _resources; j++) {
                    tm.enlist(new XAResourceImpl(), recoveryId);
                }
            }
            tm.commit();
        }
        System.out.println("FAILOVERSERVLET: simulateTransactions main test has completed successfully");

    }
}